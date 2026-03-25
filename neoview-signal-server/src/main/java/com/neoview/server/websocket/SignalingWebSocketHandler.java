package com.neoview.server.websocket;

import com.neoview.server.dto.SignalingMessage;
import com.neoview.server.model.Meeting;
import com.neoview.server.model.MeetingMember;
import com.neoview.server.model.User;
import com.neoview.server.service.AliyunNlsAsrService;
import com.neoview.server.service.MeetingService;
import com.neoview.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.util.annotation.NonNull;

/**
 * 前端信令 WebSocket 处理器：
 * 1) 校验用户身份（已通过 JWT Filter）
 * 2) 把前端信令消息转发给 Node
 * 3) Node 回包再转给前端
 * 
 * @author 椰子皮
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {
	private static final Logger logger = LoggerFactory.getLogger(SignalingWebSocketHandler.class);
	private final ObjectMapper objectMapper;
	private final NodeBridgeClient nodeBridgeClient;
	private final MeetingService meetingService;
	private final UserService userService;
	private final AliyunNlsAsrService asrService;
	private final ConcurrentMap<String, SessionContext> sessions = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ConcurrentMap<String, SessionContext>> roomSessions = new ConcurrentHashMap<>();

	@Value("${app.signaling.protoo-url:}")
	private String protooUrl;

	@Value("${app.signaling.protoo-origin:}")
	private String protooOrigin;

	@Value("${app.signaling.protoo-protocol:}")
	private String protooProtocol;

	@Value("${app.signaling.protoo-host:}")
	private String protooHost;

	@Value("${app.signaling.protoo-port:}")
	private String protooPort;

	@Value("${app.signaling.protoo-insecure:}")
	private String protooInsecure;

	/**
	 * 构造函数
	 * 注入信令处理所需的各项服务
	 * 
	 * @param objectMapper JSON序列化工具
	 * @param nodeBridgeClient Node桥接客户端
	 * @param meetingService 会议服务
	 * @param userService 用户服务
	 * @param asrService 语音识别服务
	 * @author 椰子皮
	 */
	public SignalingWebSocketHandler(ObjectMapper objectMapper,
							NodeBridgeClient nodeBridgeClient,
							MeetingService meetingService,
							UserService userService,
							AliyunNlsAsrService asrService) {
		this.objectMapper = objectMapper;
		this.nodeBridgeClient = nodeBridgeClient;
		this.meetingService = meetingService;
		this.userService = userService;
		this.asrService = asrService;
	}

	/**
	 * WebSocket连接建立回调
	 * 创建会话上下文并存入会话映射
	 * 
	 * @param session WebSocket会话
	 * @author 椰子皮
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.put(session.getId(), new SessionContext(session, objectMapper));
	}

	/**
	 * 处理文本消息
	 * 根据消息类型分发到对应的处理方法
	 * 
	 * @param session WebSocket会话
	 * @param message 文本消息
	 * @throws Exception 消息处理异常
	 * @author 椰子皮
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage message) throws Exception {
		SessionContext context = sessions.get(session.getId());
		if (context == null) {
			return;
		}

		SignalingMessage payload = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
		//logger.info("WS->Server: type={}, id={} ", payload.type, payload.id);

		switch (payload.type) {
			case "connect" -> handleConnect(session, context, payload);
			case "roomMedia" -> handleRoomMedia(context, payload);
			case "screenShareStopped" -> handleScreenShareStopped(context, payload);
			case "reaction" -> handleReaction(context, payload);
			case "asrStart" -> handleAsrStart(context, payload);
			case "asrAudio" -> handleAsrAudio(context, payload);
			case "asrStop" -> handleAsrStop(context);
			case "protooRequest", "protooNotification", "protooServerResponse" -> {
				if (context.nodeSession == null || !context.nodeSession.isReady()) {
					context.sendError("Node 连接未就绪，请先 connect");
					return;
				}
				context.nodeSession.send(payload);
			}
			default -> context.sendError("未知消息类型: " + payload.type);
		}
	}

	/**
	 * 处理连接请求
	 * 验证用户身份、加入会议、建立Node连接
	 * 
	 * @param session WebSocket会话
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @throws Exception 处理异常
	 * @author 椰子皮
	 */
	private void handleConnect(WebSocketSession session, SessionContext context, SignalingMessage payload) throws Exception {
		Authentication authentication = (Authentication) session.getPrincipal();
		if (authentication == null) {
			context.sendError("未登录");
			return;
		}

		// data 中必须包含 roomId
		if (!(payload.data instanceof Map)) {
			context.sendError("数据格式错误");
			return;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) payload.data;
		Object roomIdObj = dataMap.get("roomId");
		if (roomIdObj == null) {
			context.sendError("缺少 roomId 参数");
			return;
		}
		String roomId = roomIdObj.toString();
		Meeting meeting = meetingService.findByRoomId(roomId);
		if (meeting == null) {
			context.sendError("会议不存在");
			return;
		}

		UUID userId = UUID.fromString(authentication.getName());
		evictDuplicateUserSessions(roomId, userId.toString(), session.getId());
		String peerId = "peer-" + UUID.randomUUID();
		MeetingMember member = meetingService.joinMeeting(meeting, userId, peerId);

		User user = userService.findById(userId.toString());
		String displayName = user != null && user.getDisplayName() != null
			? user.getDisplayName()
			: peerId;

		// 建立 Node 连接并等待连接完成
		context.nodeSession = nodeBridgeClient.connect(context);
		
		// 使用异步等待机制替代忙等待
		if (!waitForNodeConnection(context)) {
			context.sendError("Node连接超时，请稍后重试");
			return;
		}

		// 记录会话信息
		context.roomId = roomId;
		context.userId = userId.toString();
		context.peerId = member.getPeerId();
		context.displayName = displayName;

		// 维护房间成员映射
		ConcurrentMap<String, SessionContext> roomMap = roomSessions.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());
		roomMap.put(session.getId(), context);

		// 先给当前用户下发完整成员列表
		List<Map<String, Object>> users = new ArrayList<>();
		for (SessionContext ctx : roomMap.values()) {
			users.add(toUserInfo(ctx));
		}

		SignalingMessage userList = new SignalingMessage();
		userList.type = "roomUserList";
		userList.data = java.util.Map.of(
			"roomId", roomId,
			"selfUserId", context.userId,
			"selfPeerId", context.peerId,
			"users", users
		);
		context.send(userList);

		// 通知房间内其他成员：有人加入
		SignalingMessage joinNotice = new SignalingMessage();
		joinNotice.type = "roomUserJoin";
		joinNotice.data = java.util.Map.of(
			"roomId", roomId,
			"user", toUserInfo(context)
		);

		for (SessionContext other : roomMap.values()) {
			if (other != context) {
				other.send(joinNotice);
			}
		}

		// 发送 connect 给 Node（桥接服务会建立 protoo 连接）
		SignalingMessage connect = new SignalingMessage();
		connect.type = "connect";
		Map<String, Object> connectData = new java.util.HashMap<>();
		connectData.put("roomId", roomId);
		connectData.put("peerId", member.getPeerId());
		if (!isBlank(protooUrl)) connectData.put("protooUrl", protooUrl);
		if (!isBlank(protooOrigin)) connectData.put("protooOrigin", protooOrigin);
		if (!isBlank(protooProtocol)) connectData.put("protooProtocol", protooProtocol);
		if (!isBlank(protooHost)) connectData.put("protooHost", protooHost);
		if (!isBlank(protooPort)) connectData.put("protooPort", protooPort);
		if (!isBlank(protooInsecure)) connectData.put("protooInsecure", protooInsecure);
		connect.data = connectData;

		try {
			context.nodeSession.send(connect);
		} catch (Exception e) {
			context.sendError("发送连接请求失败: " + e.getMessage());
		}
	}

	/**
	 * 等待Node连接建立完成
	 * 使用异步等待机制，最多等待2秒
	 * 
	 * @param context 会话上下文
	 * @return boolean 连接成功返回true，超时或失败返回false
	 * @author 椰子皮
	 */
	private boolean waitForNodeConnection(SessionContext context) {
		try {
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			
			// 启动监控线程
			Thread monitorThread = new Thread(() -> {
				int retryCount = 0;
				final int maxRetries = 20;
				final long retryDelay = 100;
				
				while (retryCount < maxRetries) {
					if (context.nodeSession != null && context.nodeSession.isReady()) {
						future.complete(true);
						return;
					}
					
					try {
						Thread.sleep(retryDelay);
						retryCount++;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						future.completeExceptionally(e);
						return;
					}
				}
				
				future.complete(false);
			});
			
			monitorThread.setDaemon(true);
			monitorThread.start();
			
			// 等待结果，最多2秒
			return future.get(2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			logger.warn("等待Node连接超时");
			return false;
		} catch (Exception e) {
			logger.error("等待Node连接时发生错误", e);
			return false;
		}
	}

	/**
	 * 转换会话上下文为用户信息Map
	 * 
	 * @param ctx 会话上下文
	 * @return Map<String, Object> 用户信息Map
	 * @author 椰子皮
	 */
	private Map<String, Object> toUserInfo(SessionContext ctx) {
		return java.util.Map.of(
			"userId", ctx.userId,
			"peerId", ctx.peerId,
			"displayName", ctx.displayName
		);
	}

	/**
	 * 驱逐同一用户的重复会话
	 * 当同一用户重复加入房间时，关闭旧的会话
	 * 
	 * @param roomId 房间ID
	 * @param userId 用户ID
	 * @param currentSessionId 当前会话ID
	 * @author 椰子皮
	 */
	private void evictDuplicateUserSessions(String roomId, String userId, String currentSessionId) {
		ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(roomId);
		if (roomMap == null || roomMap.isEmpty()) {
			return;
		}

		List<Map.Entry<String, SessionContext>> duplicates = new ArrayList<>();
		for (Map.Entry<String, SessionContext> entry : roomMap.entrySet()) {
			SessionContext existing = entry.getValue();
			if (existing == null || existing.userId == null) {
				continue;
			}
			if (entry.getKey().equals(currentSessionId)) {
				continue;
			}
			if (existing.userId.equals(userId)) {
				duplicates.add(entry);
			}
		}

		for (Map.Entry<String, SessionContext> duplicateEntry : duplicates) {
			String duplicateSessionId = duplicateEntry.getKey();
			SessionContext duplicateContext = duplicateEntry.getValue();
			roomMap.remove(duplicateSessionId);

			if (duplicateContext != null && duplicateContext.roomId != null) {
				SignalingMessage leaveNotice = new SignalingMessage();
				leaveNotice.type = "roomUserLeave";
				leaveNotice.data = java.util.Map.of(
					"roomId", duplicateContext.roomId,
					"user", toUserInfo(duplicateContext)
				);
				for (SessionContext other : roomMap.values()) {
					if (other != null && other != duplicateContext) {
						other.send(leaveNotice);
					}
				}
			}

			try {
				if (duplicateContext != null && duplicateContext.session.isOpen()) {
					duplicateContext.session.close(CloseStatus.NORMAL.withReason("same user rejoined"));
				}
			} catch (Exception e) {
				logger.warn("关闭重复会话失败: sessionId={}, userId={}, error={}", duplicateSessionId, userId, e.getMessage());
			}
		}
	}

	/**
	 * 处理房间媒体状态变更
	 * 广播用户的音视频开关状态
	 * 
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @author 椰子皮
	 */
	private void handleRoomMedia(SessionContext context, SignalingMessage payload) {
		if (context.roomId == null) {
			context.sendError("尚未加入会议");
			return;
		}
		if (!(payload.data instanceof Map)) {
			context.sendError("数据格式错误");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) payload.data;
		String kind = dataMap.getOrDefault("kind", "video").toString();
		boolean enabled = Boolean.parseBoolean(String.valueOf(dataMap.getOrDefault("enabled", true)));

		ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(context.roomId);
		if (roomMap == null) {
			return;
		}

		SignalingMessage notice = new SignalingMessage();
		notice.type = "roomMedia";
		notice.data = java.util.Map.of(
			"roomId", context.roomId,
			"user", toUserInfo(context),
			"kind", kind,
			"enabled", enabled
		);

		for (SessionContext other : roomMap.values()) {
			if (other != context) {
				other.send(notice);
			}
		}
	}

	/**
	 * 处理屏幕共享停止通知
	 * 广播屏幕共享停止事件
	 * 
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @author 椰子皮
	 */
	private void handleScreenShareStopped(SessionContext context, SignalingMessage payload) {
		if (context.roomId == null) {
			context.sendError("尚未加入会议");
			return;
		}
		String reason = "user-stop";
		if (payload.data instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dataMap = (Map<String, Object>) payload.data;
			Object reasonObj = dataMap.get("reason");
			if (reasonObj != null) {
				reason = reasonObj.toString();
			}
		}

		ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(context.roomId);
		if (roomMap == null) {
			return;
		}

		SignalingMessage notice = new SignalingMessage();
		notice.type = "screenShareStopped";
		notice.data = java.util.Map.of(
			"roomId", context.roomId,
			"user", toUserInfo(context),
			"reason", reason
		);

		for (SessionContext other : roomMap.values()) {
			if (other != context) {
				other.send(notice);
			}
		}
	}

	/**
	 * 处理表情反应
	 * 广播用户的表情反应
	 * 
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @author 椰子皮
	 */
	private void handleReaction(SessionContext context, SignalingMessage payload) {
		if (context.roomId == null) {
			context.sendError("尚未加入会议");
			return;
		}
		if (!(payload.data instanceof Map)) {
			context.sendError("数据格式错误");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) payload.data;
		String emoji = String.valueOf(dataMap.getOrDefault("emoji", "")).trim();
		if (emoji.isEmpty()) {
			return;
		}

		ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(context.roomId);
		if (roomMap == null) {
			return;
		}

		SignalingMessage notice = new SignalingMessage();
		notice.type = "reaction";
		notice.data = java.util.Map.of(
			"roomId", context.roomId,
			"user", toUserInfo(context),
			"emoji", emoji,
			"timestamp", System.currentTimeMillis()
		);

		for (SessionContext other : roomMap.values()) {
			other.send(notice);
		}
	}

	/**
	 * 处理ASR启动请求
	 * 创建语音识别会话
	 * 
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @author 椰子皮
	 */
	private void handleAsrStart(SessionContext context, SignalingMessage payload) {
		if (context.roomId == null) {
			context.sendError("尚未加入会议");
			return;
		}
		if (!(payload.data instanceof Map)) {
			context.sendError("数据格式错误");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) payload.data;
		String format = String.valueOf(dataMap.getOrDefault("format", "pcm"));
		int sampleRate = Integer.parseInt(String.valueOf(dataMap.getOrDefault("sampleRate", 16000)));

		if (context.asrSession != null) {
			logger.info("ASR会话关闭: userId={}, roomId={}", context.userId, context.roomId);
			context.asrSession.stop();
			context.asrSession = null;
		}

		logger.info("ASR会话创建: userId={}, roomId={}, format={}, sampleRate={}", 
			context.userId, context.roomId, format, sampleRate);

		AliyunNlsAsrService.AsrConfig config = new AliyunNlsAsrService.AsrConfig(format, sampleRate);
		context.asrSession = asrService.openSession(config, result -> broadcastAsrResult(context, result), error -> {
			logger.warn("ASR 连接异常: userId={}, error={}", context.userId, error.getMessage());
		});
	}

	/**
	 * 处理ASR音频数据
	 * 将音频数据发送到语音识别服务
	 * 
	 * @param context 会话上下文
	 * @param payload 信令消息
	 * @author 椰子皮
	 */
	private void handleAsrAudio(SessionContext context, SignalingMessage payload) {
		if (context.asrSession == null) {
			return;
		}
		if (!(payload.data instanceof Map)) {
			context.sendError("数据格式错误");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) payload.data;
		Object audioBase64 = dataMap.get("audioBase64");
		Object seqObj = dataMap.get("seq");
		if (audioBase64 == null) {
			return;
		}
		byte[] audioBytes;
		try {
			audioBytes = Base64.getDecoder().decode(audioBase64.toString());
		} catch (Exception ex) {
			logger.warn("ASR 音频解析失败: {}", ex.getMessage());
			return;
		}

		// 诊断日志：每50帧打印一次
		int seq = seqObj != null ? Integer.parseInt(seqObj.toString()) : -1;
		if (seq % 50 == 0) {
			// 计算音频能量
			double sum = 0;
			int sampleCount = Math.min(1000, audioBytes.length / 2);
			for (int i = 0; i < sampleCount; i++) {
				short sample = (short) ((audioBytes[i * 2] & 0xFF) | (audioBytes[i * 2 + 1] << 8));
				sum += Math.abs(sample);
			}
			double avgEnergy = sum / sampleCount / 32768.0; // 归一化到 0-1
			logger.info("ASR音频诊断: userId={}, seq={}, 字节数={}, 平均能量={}, 有声音={}", 
				context.userId, seq, audioBytes.length, String.format("%.4f", avgEnergy), avgEnergy > 0.01 ? "是" : "否");
		}

		try {
			context.asrSession.sendAudio(audioBytes);
		} catch (Exception ex) {
			logger.warn("ASR 音频发送失败: {}", ex.getMessage());
		}
	}

	/**
	 * 处理ASR停止请求
	 * 关闭语音识别会话
	 * 
	 * @param context 会话上下文
	 * @author 椰子皮
	 */
	private void handleAsrStop(SessionContext context) {
		if (context.asrSession != null) {
			context.asrSession.stop();
			context.asrSession = null;
		}
	}

	/**
	 * 广播ASR识别结果
	 * 将语音识别结果广播给房间内所有用户
	 * 
	 * @param context 会话上下文
	 * @param result ASR识别结果
	 * @author 椰子皮
	 */
	private void broadcastAsrResult(SessionContext context, AliyunNlsAsrService.AsrResult result) {
		if (context.roomId == null) {
			return;
		}
		ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(context.roomId);
		if (roomMap == null) {
			return;
		}

		logger.info("ASR result received: roomId={}, userId={}, isFinal={}, text={}",
			context.roomId, context.userId, result.isFinal(), result.text());
		SignalingMessage message = new SignalingMessage();
		message.type = "asrResult";
		message.data = java.util.Map.of(
			"roomId", context.roomId,
			"user", toUserInfo(context),
			"text", result.text(),
			"isFinal", result.isFinal()
		);

		for (SessionContext other : roomMap.values()) {
			other.send(message);
		}
	}

	/**
	 * WebSocket连接关闭回调
	 * 清理会话、通知其他用户、更新会议状态
	 * 
	 * @param session WebSocket会话
	 * @param status 关闭状态
	 * @author 椰子皮
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
		SessionContext context = sessions.remove(session.getId());
		if (context != null) {
			// 通知房间内其他成员：有人离开
			if (context.roomId != null) {
				ConcurrentMap<String, SessionContext> roomMap = roomSessions.get(context.roomId);
				if (roomMap != null) {
					roomMap.remove(session.getId());
					if (roomMap.isEmpty()) {
						roomSessions.remove(context.roomId);
					}
					SignalingMessage leaveNotice = new SignalingMessage();
					leaveNotice.type = "roomUserLeave";
					leaveNotice.data = java.util.Map.of(
						"roomId", context.roomId,
						"user", toUserInfo(context)
					);
					for (SessionContext other : roomMap.values()) {
						other.send(leaveNotice);
					}
				}
			}
			if (context.roomId != null && context.userId != null) {
				try {
					Meeting meeting = meetingService.findByRoomId(context.roomId);
					if (meeting != null) {
						meetingService.leaveMeeting(meeting, UUID.fromString(context.userId));
					}
				} catch (Exception e) {
					logger.warn("离会状态更新失败: roomId={}, userId={}, error={}", context.roomId, context.userId, e.getMessage());
				}
			}
			if (context.asrSession != null) {
				logger.info("ASR会话关闭(连接断开): userId={}, roomId={}", context.userId, context.roomId);
				context.asrSession.stop();
				context.asrSession = null;
			}
			context.close();
		}
	}

	/**
	 * 检查字符串是否为空
	 * 
	 * @param value 待检查字符串
	 * @return boolean 为空或空白返回true
	 * @author 椰子皮
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * 会话上下文：保存前端 WS 与 Node 会话
	 * 管理单个WebSocket连接的所有状态信息
	 * 
	 * @author 椰子皮
	 */
	public static class SessionContext {
		private final WebSocketSession session;
		private final ObjectMapper objectMapper;
		private NodeBridgeClient.NodeSession nodeSession;
		private AliyunNlsAsrService.AsrSession asrSession;
		private String roomId;
		private String userId;
		private String peerId;
		private String displayName;

		/**
		 * 构造函数
		 * 
		 * @param session WebSocket会话
		 * @param objectMapper JSON序列化工具
		 * @author 椰子皮
		 */
		public SessionContext(WebSocketSession session, ObjectMapper objectMapper) {
			this.session = session;
			this.objectMapper = objectMapper;
		}

		/**
		 * 发送信令消息
		 * 将消息序列化后发送给前端
		 * 
		 * @param message 信令消息
		 * @author 椰子皮
		 */
		public void send(SignalingMessage message) {
			try {
				String payload = objectMapper.writeValueAsString(message);
				session.sendMessage(new TextMessage(payload));
			} catch (Exception ignored) {
			}
		}

		/**
		 * 发送错误消息
		 * 构造错误类型的信令消息并发送
		 * 
		 * @param message 错误消息
		 * @author 椰子皮
		 */
		public void sendError(String message) {
			SignalingMessage error = new SignalingMessage();
			error.type = "error";
			error.errorReason = message;
			send(error);
		}

		/**
		 * 关闭会话
		 * 向Node发送关闭消息
		 * 
		 * @author 椰子皮
		 */
		public void close() {
			if (nodeSession != null) {
				try {
					SignalingMessage close = new SignalingMessage();
					close.type = "close";
					nodeSession.send(close);
				} catch (Exception ignored) {
				}
			}
		}
	}
}
