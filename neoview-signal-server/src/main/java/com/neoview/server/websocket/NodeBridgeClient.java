package com.neoview.server.websocket;

import com.neoview.server.dto.SignalingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Node 桥接客户端：由 Spring Boot 主动连接 Node
 * 负责在Spring Boot和Node.js信令服务器之间建立WebSocket桥接
 * 
 * @author 椰子皮
 */
@Component
public class NodeBridgeClient {
	private static final Logger logger = LoggerFactory.getLogger(NodeBridgeClient.class);
	private final String nodeUrl;
	private final long requestTimeout;
	private final ObjectMapper objectMapper;

	/**
	 * 初始化Node桥接客户端
	 * 
	 * @param nodeUrl Node服务器WebSocket地址
	 * @param requestTimeout 请求超时时间（毫秒）
	 * @param objectMapper JSON序列化工具
	 * @author 椰子皮
	 */
	public NodeBridgeClient(@Value("${app.signaling.node-url}") String nodeUrl,
						@Value("${app.signaling.request-timeout}") long requestTimeout,
						ObjectMapper objectMapper) {
		this.nodeUrl = nodeUrl;
		this.requestTimeout = requestTimeout;
		this.objectMapper = objectMapper;
	}

	/**
	 * 建立与Node服务器的WebSocket连接
	 * 
	 * @param context 前端会话上下文，用于发送错误信息
	 * @return NodeSession Node会话实例
	 * @throws Exception 连接建立过程中的异常
	 * @author 椰子皮
	 */
    public NodeSession connect(SignalingWebSocketHandler.SessionContext context) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        NodeSession session = new NodeSession(context, objectMapper, requestTimeout);

        // 1. 构建WebSocket请求头（空头部，可按需添加自定义头）
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        // 如需添加自定义头示例：
        // headers.add("Authorization", "Bearer token");

        // 2. 转换URL为URI对象
        URI nodeUri = URI.create(nodeUrl);

        // 3. 使用Spring 6.0推荐的execute方法（非弃用），替代doHandshake
        CompletableFuture<WebSocketSession> future = client.execute(session, headers, nodeUri);

        // 4. 适配回调逻辑到CompletableFuture（替代原ListenableFutureCallback）
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // 连接失败处理
                logger.error("Node连接失败: {}", ex.getMessage(), ex);
                context.sendError("Node 连接失败: " + ex.getMessage());
            } else {
                // 连接成功，绑定WebSocketSession
                logger.info("Node连接成功建立");
                session.attach(result);
            }
        });

        return session;
    }

	/**
	 * Node 会话：一个前端 WS 对应一个 Node 连接
	 * 负责管理与Node.js服务器的WebSocket连接
	 * 
	 * @author 椰子皮
	 */
	public static class NodeSession extends TextWebSocketHandler {
		private final SignalingWebSocketHandler.SessionContext context;
		private final ObjectMapper objectMapper;
		private final long requestTimeout;
		private WebSocketSession session;

		// 用于跟踪请求回包
		private final Map<String, Long> pending = new ConcurrentHashMap<>();

		/**
		 * 初始化Node会话
		 * 
		 * @param context 前端会话上下文
		 * @param objectMapper JSON序列化工具
		 * @param requestTimeout 请求超时时间（毫秒）
		 * @author 椰子皮
		 */
		public NodeSession(SignalingWebSocketHandler.SessionContext context,
						ObjectMapper objectMapper,
						long requestTimeout) {
			this.context = context;
			this.objectMapper = objectMapper;
			this.requestTimeout = requestTimeout;
		}

		/**
		 * 绑定WebSocket会话
		 * 
		 * @param session WebSocket会话实例
		 * @author 椰子皮
		 */
		public void attach(WebSocketSession session) {
			this.session = session;
		}

		/**
		 * 检查Node连接是否就绪
		 * 
		 * @return true表示连接可用，false表示未连接或已断开
		 * @author 椰子皮
		 */
		public boolean isReady() {
			return session != null && session.isOpen();
		}

		/**
		 * 向Node服务器发送信令消息
		 * 
		 * @param message 信令消息对象
		 * @throws Exception 当连接未就绪或发送失败时抛出异常
		 * @author 椰子皮
		 */
		public void send(SignalingMessage message) throws Exception {
			if (session == null || !session.isOpen()) {
				throw new IllegalStateException("Node 连接未就绪");
			}
			logger.info("Server->Node: type={}, id={}", message.type, message.id);
			String payload = objectMapper.writeValueAsString(message);
			session.sendMessage(new TextMessage(payload));

			if (message.id != null) {
				pending.put(message.id, System.currentTimeMillis());
			}
		}

		/**
		 * 处理来自Node服务器的文本消息
		 * 
		 * @param session WebSocket会话
		 * @param message 接收到的文本消息
		 * @throws Exception 消息处理过程中的异常
		 * @author 椰子皮
		 */
		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			SignalingMessage payload = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
			logger.info("Node->Server: type={}, id={}", payload.type, payload.id);
			// 直接转发给前端
			context.send(payload);

			if (payload.id != null) {
				pending.remove(payload.id);
			}
		}

		/**
		 * 处理WebSocket连接关闭事件
		 * 
		 * @param session WebSocket会话
		 * @param status 连接关闭状态
		 * @author 椰子皮
		 */
		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			context.sendError("Node 连接已关闭");
		}

		/**
		 * 清理超时的请求记录
		 * 移除超过超时时间的待处理请求
		 * 
		 * @author 椰子皮
		 */
		public void cleanupTimeout() {
			long now = System.currentTimeMillis();
			pending.entrySet().removeIf(entry -> now - entry.getValue() > requestTimeout);
		}
	}
}
