package com.neoview.server.service;

import com.neoview.server.config.AliyunNlsProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 阿里云NLS语音识别服务
 * 提供实时语音识别功能，通过WebSocket与阿里云NLS服务通信
 * 
 * @author 椰子皮
 */
@Service
public class AliyunNlsAsrService {
	private static final Logger logger = LoggerFactory.getLogger(AliyunNlsAsrService.class);
	private static final String NAMESPACE = "SpeechTranscriber";
	private static final String START_NAME = "StartTranscription";
	private static final String STOP_NAME = "StopTranscription";

	private final AliyunNlsProperties properties;
	private final AliyunNlsTokenService tokenService;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	/**
	 * 构造函数
	 * 初始化阿里云NLS ASR服务
	 * 
	 * @param properties 阿里云NLS配置属性
	 * @param tokenService NLS令牌服务
	 * @param objectMapper JSON序列化工具
	 * @author 椰子皮
	 */
	public AliyunNlsAsrService(AliyunNlsProperties properties,
							AliyunNlsTokenService tokenService,
							ObjectMapper objectMapper) {
		this.properties = properties;
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	/**
	 * 打开阿里云NLS语音识别会话
	 * 创建并初始化一个新的WebSocket会话用于实时语音识别，
	 * 包含必要的认证信息和配置参数。
	 * 
	 * @param config ASR配置参数，包含音频格式和采样率
	 * @param onResult 识别结果回调函数，接收识别文本和是否为最终结果
	 * @param onError 错误处理回调函数，接收异常信息
	 * @return AsrSession 语音识别会话实例
	 * @throws IllegalStateException 当AppKey未配置时抛出
	 * @author 椰子皮
	 */
	public AsrSession openSession(AsrConfig config, Consumer<AsrResult> onResult, Consumer<Throwable> onError) {
		String token = tokenService.fetchToken();
		String wsUrl = properties.getWsUrl();
		String appKey = properties.getAppKey();
		if (appKey == null || appKey.trim().isEmpty()) {
			throw new IllegalStateException("阿里云 NLS AppKey 未配置");
		}

		NlsWebSocketSession session = new NlsWebSocketSession(wsUrl, appKey, token, config, onResult, onError);
		session.connect();
		return session;
	}

	/**
	 * ASR配置记录
	 * 包含音频格式和采样率配置
	 * 
	 * @param format 音频格式（如pcm）
	 * @param sampleRate 采样率（如16000）
	 * @author 椰子皮
	 */
	public record AsrConfig(String format, int sampleRate) {}

	/**
	 * ASR识别结果记录
	 * 包含识别文本和是否为最终结果
	 * 
	 * @param text 识别文本
	 * @param isFinal 是否为最终结果
	 * @author 椰子皮
	 */
	public record AsrResult(String text, boolean isFinal) {}

	/**
	 * ASR会话接口
	 * 定义语音识别会话的基本操作
	 * 
	 * @author 椰子皮
	 */
	public interface AsrSession {
		/**
		 * 发送音频数据
		 * 
		 * @param audioBytes 音频字节数据
		 * @author 椰子皮
		 */
		void sendAudio(byte[] audioBytes);
		
		/**
		 * 停止识别
		 * @author 椰子皮
		 */
		void stop();
		
		/**
		 * 关闭会话
		 * @author 椰子皮
		 */
		void close();
	}

	/**
	 * NLS WebSocket会话实现
	 * 实现与阿里云NLS服务的WebSocket通信
	 * 
	 * @author 椰子皮
	 */
	private class NlsWebSocketSession implements AsrSession, WebSocket.Listener {
		private final String wsUrl;
		private final String appKey;
		private final String token;
		private final AsrConfig config;
		private final Consumer<AsrResult> onResult;
		private final Consumer<Throwable> onError;
		private final Queue<byte[]> pendingAudio = new ConcurrentLinkedQueue<>();
		private final String taskId = UUID.randomUUID().toString().replace("-", "");

		private CompletableFuture<WebSocket> socketFuture;
		private volatile boolean started;
		private volatile boolean stopped;
		private long audioPacketsSent;
		private long audioBytesSent;
		private long lastAudioAtMillis;



		/**
		 * 构造函数
		 * 初始化NLS WebSocket会话
		 * 
		 * @param wsUrl WebSocket服务地址
		 * @param appKey 应用Key
		 * @param token 访问令牌
		 * @param config ASR配置
		 * @param onResult 识别结果回调
		 * @param onError 错误回调
		 * @author 椰子皮
		 */
		private NlsWebSocketSession(String wsUrl,
							String appKey,
							String token,
							AsrConfig config,
							Consumer<AsrResult> onResult,
							Consumer<Throwable> onError) {
			this.wsUrl = wsUrl;
			this.appKey = appKey;
			this.token = token;
			this.config = config;
			this.onResult = onResult;
			this.onError = onError;
		}

		/**
		 * 建立WebSocket连接
		 * 异步连接到阿里云NLS服务
		 * 
		 * @author 椰子皮
		 */
		private void connect() {
			logger.info("ASR连接创建: taskId={}, wsUrl={}", taskId, wsUrl);
			this.socketFuture = httpClient.newWebSocketBuilder()
					.connectTimeout(Duration.ofSeconds(10))
					.header("X-NLS-Token", token)
					.header("X-NLS-Appkey", appKey)
					.header("User-Agent", "neoview-signal-server")
					.buildAsync(URI.create(wsUrl), this);
		}


		/**
		 * 发送音频数据到阿里云NLS服务进行实时语音识别
		 * 将音频字节数据通过WebSocket连接发送到阿里云语音识别服务。
		 * 如果WebSocket连接尚未建立完成，则将音频数据暂存到队列中等待后续发送。
		 * 
		 * @param audioBytes 音频数据字节数组，不能为null
		 * @author 椰子皮
		 */
		@Override
		public void sendAudio(byte[] audioBytes) {
			// 检查会话是否已停止，如果已停止则直接返回
			if (stopped) {
				return;
			}
			if (!started) {
				pendingAudio.add(audioBytes);
				return;
			}
			if (socketFuture == null) {
				pendingAudio.add(audioBytes);
				return;
			}
			if (socketFuture.isCompletedExceptionally()) {
				pendingAudio.clear();
				throw new IllegalStateException("NLS WebSocket 握手失败");
			}
			// 获取WebSocket连接实例，如果连接未完成则返回null
			WebSocket socket = socketFuture.isDone() ? socketFuture.join() : null;
			// 如果WebSocket连接未建立，则将音频数据加入待发送队列
			if (socket == null) {
				pendingAudio.add(audioBytes);
				return;
			}
			// 通过WebSocket发送二进制音频数据
			sendBinary(socket, audioBytes);
		}



		/**
		 * 停止语音识别
		 * 发送停止指令并关闭WebSocket连接
		 * 
		 * @author 椰子皮
		 */
		@Override
		public void stop() {
			if (stopped) {
				return;
			}
			stopped = true;
			WebSocket socket = socketFuture != null && socketFuture.isDone() ? socketFuture.join() : null;
			if (socket != null) {
				sendStop(socket);
				socket.sendClose(WebSocket.NORMAL_CLOSURE, "stop");
			}
		}

		/**
		 * 关闭会话
		 * 调用stop方法关闭连接
		 * 
		 * @author 椰子皮
		 */
		@Override
		public void close() {
			stop();
		}

		/**
		 * WebSocket打开回调
		 * 连接建立后发送开始识别指令
		 * 
		 * @param webSocket WebSocket实例
		 * @author 椰子皮
		 */
		@Override
		public void onOpen(WebSocket webSocket) {
			logger.info("NLS WS open, taskId={}", taskId);
			WebSocket.Listener.super.onOpen(webSocket);
			webSocket.request(1);
			sendStart(webSocket);
			scheduleIdleGuard(webSocket);
		}




		/**
		 * 接收文本消息回调
		 * 处理阿里云NLS服务返回的识别结果
		 * 
		 * @param webSocket WebSocket实例
		 * @param data 文本数据
		 * @param last 是否为最后一条消息
		 * @return CompletionStage 完成阶段
		 * @author 椰子皮
		 */
		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			try {
				parseResult(data.toString());
			} catch (Exception ex) {
				logger.warn("解析 NLS 结果失败: {}", ex.getMessage());
			} finally {
				webSocket.request(1);
			}
			return WebSocket.Listener.super.onText(webSocket, data, last);
		}

		/**
		 * 接收二进制消息回调
		 * 
		 * @param webSocket WebSocket实例
		 * @param data 二进制数据
		 * @param last 是否为最后一条消息
		 * @return CompletionStage 完成阶段
		 * @author 椰子皮
		 */
		@Override
		public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
			webSocket.request(1);
			return WebSocket.Listener.super.onBinary(webSocket, data, last);
		}


		/**
		 * 错误回调
		 * 处理WebSocket连接错误
		 * 
		 * @param webSocket WebSocket实例
		 * @param error 错误对象
		 * @author 椰子皮
		 */
		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			if (error instanceof java.net.http.WebSocketHandshakeException handshake) {
				logger.warn("NLS WS handshake failed: status={}, headers={}",
					handshake.getResponse().statusCode(),
					handshake.getResponse().headers().map());
			}
			onError.accept(error);
			WebSocket.Listener.super.onError(webSocket, error);
		}


		/**
		 * 发送开始识别指令
		 * 启动语音识别会话
		 * 
		 * @param socket WebSocket实例
		 * @author 椰子皮
		 */
		private void sendStart(WebSocket socket) {
			if (started) {
				return;
			}
			try {

				Map<String, Object> header = Map.of(
						"appkey", appKey,
						"token", token,
						"namespace", NAMESPACE,
						"name", START_NAME,
						"task_id", taskId,
						"message_id", UUID.randomUUID().toString().replace("-", "")
				);

				Map<String, Object> payload = new java.util.HashMap<>();
				payload.put("format", config.format());
				payload.put("sample_rate", config.sampleRate());
				payload.put("enable_intermediate_result", true);
				payload.put("enable_punctuation_prediction", true);
				payload.put("enable_inverse_text_normalization", true);
				// 添加更多参数提高识别准确率
				payload.put("disfluency_removal", true);  // 去除语气词、重复词
				payload.put("max_sentence_silence", 1000);  // 最长句子静音时间(ms)
				payload.put("max_start_silence", 5000);  // 最长开始静音时间(ms)
				String startMsg = objectMapper.writeValueAsString(Map.of(
						"header", header,
						"payload", payload
				));
				socket.sendText(startMsg, true);
			} catch (Exception ex) {
				onError.accept(ex);
			}
		}

		/**
		 * 发送停止识别指令
		 * 终止语音识别会话
		 * 
		 * @param socket WebSocket实例
		 * @author 椰子皮
		 */
		private void sendStop(WebSocket socket) {
			try {
			Map<String, Object> header = Map.of(
						"appkey", appKey,
						"token", token,
						"namespace", NAMESPACE,
						"name", STOP_NAME,
						"task_id", taskId,
						"message_id", UUID.randomUUID().toString().replace("-", "")
			);

				String stopMsg = objectMapper.writeValueAsString(Map.of("header", header));
				socket.sendText(stopMsg, true);
			} catch (Exception ex) {
				logger.warn("发送 NLS Stop 失败: {}", ex.getMessage());
			}
		}

		/**
		 * 刷新待发送音频队列
		 * 发送所有暂存的音频数据
		 * 
		 * @param socket WebSocket实例
		 * @author 椰子皮
		 */
		private void flushPending(WebSocket socket) {
			byte[] data;
			while ((data = pendingAudio.poll()) != null) {
				sendBinary(socket, data);
			}
		}

		/**
		 * 调度空闲保护线程
		 * 定期发送静音帧保持连接活跃
		 * 
		 * @param socket WebSocket实例
		 * @author 椰子皮
		 */
		private void scheduleIdleGuard(WebSocket socket) {
			lastAudioAtMillis = System.currentTimeMillis();
			Thread guard = new Thread(() -> {
				while (!stopped) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ignored) {
						Thread.currentThread().interrupt();
						return;
					}
					if (!started) {
						continue;
					}
					long now = System.currentTimeMillis();
					if (now - lastAudioAtMillis > 10_000) {
						// 发送静音帧保活，避免空闲断开
						byte[] silence = new byte[320];
						sendBinary(socket, silence);
					}
				}
			});
			guard.setDaemon(true);
			guard.start();
		}


		/**
		 * 发送二进制音频数据
		 * 通过WebSocket发送音频数据到NLS服务
		 * 
		 * @param socket WebSocket实例
		 * @param audioBytes 音频字节数据
		 * @author 椰子皮
		 */
		private void sendBinary(WebSocket socket, byte[] audioBytes) {
			if (audioBytes == null || audioBytes.length == 0) {
				return;
			}
			audioPacketsSent++;
			audioBytesSent += audioBytes.length;
			if (audioPacketsSent == 1 || audioPacketsSent % 50 == 0) {
				logger.info("NLS audio sent: taskId={}, packets={}, bytes={}", taskId, audioPacketsSent, audioBytesSent);
			}
			lastAudioAtMillis = System.currentTimeMillis();
			socket.sendBinary(ByteBuffer.wrap(audioBytes), true);
		}



		/**
		 * 解析识别结果
		 * 处理阿里云NLS服务返回的JSON格式识别结果
		 * 
		 * @param json JSON格式字符串
		 * @throws Exception 解析异常
		 * @author 椰子皮
		 */
		private void parseResult(String json) throws Exception {
			logger.info("NLS WS 原始返回: {}", json);
			Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {});

			Object payloadObj = data.get("payload");
			Object headerObj = data.get("header");
			String name = headerObj instanceof Map ? String.valueOf(((Map<?, ?>) headerObj).get("name")) : "";
			String statusText = headerObj instanceof Map ? String.valueOf(((Map<?, ?>) headerObj).get("status_text")) : "";

			if ("TranscriptionStarted".equalsIgnoreCase(name)) {
				started = true;
				WebSocket socket = socketFuture != null && socketFuture.isDone() ? socketFuture.join() : null;
				if (socket != null) {
					flushPending(socket);
				}
			}

			if ("TaskFailed".equalsIgnoreCase(name)) {
				stopped = true;
				pendingAudio.clear();
				logger.warn("NLS task failed: {}", statusText);
			}

			String text = null;
			boolean isFinal = false;
			if (payloadObj instanceof Map payload) {

				Object sentenceObj = payload.get("sentence");
				if (sentenceObj instanceof Map sentence) {
					Object sentenceText = sentence.get("text");
					if (sentenceText != null) {
						text = sentenceText.toString();
					}
					Object finalObj = sentence.get("is_final");
					if (finalObj != null) {
						isFinal = Boolean.parseBoolean(finalObj.toString());
					}
				}
				if (text == null && payload.get("result") != null) {
					text = payload.get("result").toString();
					isFinal = "TranscriptionCompleted".equalsIgnoreCase(name) || "SentenceEnd".equalsIgnoreCase(name);
				}
			}

			if (text != null && !text.trim().isEmpty()) {
				onResult.accept(new AsrResult(text, isFinal));
			}
		}
	}
}
