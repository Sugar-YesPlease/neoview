// mediasoup 信令桥接服务：
// Spring Boot <--(WebSocket JSON)--> 本 Node 服务 <--(protoo WebSocket)--> mediasoup-demo server
import WebSocket, { WebSocketServer } from 'ws';
import protooClient from 'protoo-client';
import qs from 'qs';

// 监听端口与目标 mediasoup protoo 地址配置（通过环境变量配置）
const LISTEN_PORT = Number(process.env.LISTEN_PORT || 7000);
const PROTOO_PROTOCOL = process.env.PROTOO_PROTOCOL || 'wss';
const PROTOO_HOST = process.env.PROTOO_HOST || 'localhost';
const PROTOO_PORT = Number(process.env.PROTOO_PORT || 4443);
const PROTOO_INSECURE = String(process.env.PROTOO_INSECURE || 'false') === 'true';
const PROTOO_ORIGIN = process.env.PROTOO_ORIGIN || '';

if (PROTOO_INSECURE) {
	// 允许自签名证书（仅建议在测试环境使用）
	process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}


// 根据 roomId/peerId 拼接 protoo 的 WebSocket 地址
function buildProtooUrl({
	roomId,
	peerId,
	consumerReplicas,
	usePipeTransports,
	protocol = PROTOO_PROTOCOL,
	host = PROTOO_HOST,
	port = PROTOO_PORT,
}) {
	const query = qs.stringify({
		roomId,
		peerId,
		consumerReplicas,
		usePipeTransports,
	});

	return `${protocol}://${host}:${port}/?${query}`;
}

function buildProtooOrigin({ protocol, host, port, origin }) {
	if (origin) return origin;
	const scheme = protocol === 'wss' ? 'https' : 'http';
	return `${scheme}://${host}:${port}`;
}

function toBoolean(value, fallback) {
	if (value === undefined || value === null || value === '') return fallback;
	return String(value) === 'true';
}


// 安全发送：只在 WS 处于 OPEN 状态时发送
function safeSend(ws, payload) {
	if (ws.readyState === WebSocket.OPEN) {
		ws.send(JSON.stringify(payload));
	}
}

// 把 error 对象序列化成 JSON，便于回传给 Spring Boot
function serializeError(error) {
	return {
		name: error?.name,
		message: error?.message || String(error),
		stack: error?.stack,
	};
}

function logInfo(message, meta) {
	// eslint-disable-next-line no-console
	console.log(`[bridge] ${message}`, meta || '');
}

function logError(message, meta) {
	// eslint-disable-next-line no-console
	console.error(`[bridge] ${message}`, meta || '');
}

function withTimeout(promise, timeoutMs = 15000, label = 'request') {
	return new Promise((resolve, reject) => {
		const timer = setTimeout(() => {
			reject(new Error(`${label} timeout`));
		}, timeoutMs);
		promise
			.then(result => {
				clearTimeout(timer);
				resolve(result);
			})
			.catch(error => {
				clearTimeout(timer);
				reject(error);
			});
	});
}


// 每一个 Spring Boot 的 WebSocket 连接，对应一个 BridgeSession
class BridgeSession {

	constructor(ws) {
		this.ws = ws;
		// 与 mediasoup-demo protoo 连接的 Peer
		this.protoo = null;
		// 记录来自 mediasoup-demo 的 request，需要 Spring Boot 回应 accept/reject
		this.pendingServerRequests = new Map();
		this.lastProtoo = null;
	}

	registerPending(requestId, accept, reject, timeoutMs = 15000) {
		let timer = null;
		if (timeoutMs > 0) {
			timer = setTimeout(() => {
				this.pendingServerRequests.delete(requestId);
				try {
					reject(408, 'serverRequest timeout');
				} catch (error) {
					logError('reject pending timeout failed', serializeError(error));
				}
			}, timeoutMs);
		}
		this.pendingServerRequests.set(requestId, { accept, reject, timer });
	}

	clearPending(reason = 'bridge closed') {
		for (const [requestId, pending] of this.pendingServerRequests.entries()) {
			if (pending?.timer) {
				clearTimeout(pending.timer);
			}
			try {
				pending.reject(500, reason);
			} catch (error) {
				logError('reject pending failed', serializeError(error));
			}
			this.pendingServerRequests.delete(requestId);
		}
	}



	// 建立到 mediasoup-demo 的 protoo 连接
	connect(params = {}) {
		if (this.protoo) return;

		const {
			protooUrl,
			protooOrigin,
			protooProtocol,
			protooHost,
			protooPort,
			protooInsecure,
			roomId,
			peerId,
			consumerReplicas,
			usePipeTransports,
		} = params;

		if (!protooUrl && (!roomId || !peerId)) {
			throw new Error('roomId and peerId are required when protooUrl is not provided');
		}

		const protocol = protooProtocol || PROTOO_PROTOCOL;
		const host = protooHost || PROTOO_HOST;
		const port = Number(protooPort || PROTOO_PORT);
		const origin = buildProtooOrigin({
			protocol,
			host,
			port,
			origin: protooOrigin || PROTOO_ORIGIN,
		});
		const insecure = toBoolean(protooInsecure, PROTOO_INSECURE);

		const url = protooUrl || buildProtooUrl({
			roomId,
			peerId,
			consumerReplicas,
			usePipeTransports,
			protocol,
			host,
			port,
		});

		this.lastProtoo = { url, origin, protocol, host, port, insecure };
		logInfo('connecting to protoo', this.lastProtoo);

		// 建立 protoo WebSocket 连接
		const transport = new protooClient.WebSocketTransport(url, {
			origin,
			rejectUnauthorized: !insecure,
		});

		const rawWs = transport?._ws;
		if (rawWs?.on) {
			rawWs.on('error', (error) => {
				logError('protoo ws error', serializeError(error));
			});
			rawWs.on('close', (code, reason) => {
				logError('protoo ws close', { code, reason: reason?.toString?.() });
			});
			rawWs.on('unexpected-response', (_req, res) => {

				logError('protoo ws unexpected response', {
					statusCode: res?.statusCode,
					statusMessage: res?.statusMessage,
					headers: res?.headers,
				});
			});
		}


		this.protoo = new protooClient.Peer(transport);

		// 把 protoo 的连接状态回传给 Spring Boot
		this.protoo.on('open', () => {
			logInfo('protoo open');
			safeSend(this.ws, { type: 'protooOpen' });
		});
		this.protoo.on('failed', () => {
			logError('protoo failed', this.lastProtoo || {});
			this.clearPending('protoo failed');
			safeSend(this.ws, { type: 'protooFailed' });
		});
		this.protoo.on('disconnected', () => {
			logError('protoo disconnected', this.lastProtoo || {});
			this.clearPending('protoo disconnected');
			safeSend(this.ws, { type: 'protooDisconnected' });
		});

		this.protoo.on('close', () => {
			logInfo('protoo closed');
			this.clearPending('protoo closed');
			safeSend(this.ws, { type: 'protooClose' });
		});



		// mediasoup-demo 主动发起的 request（比如 newConsumer）
		this.protoo.on('request', (request, accept, reject) => {
			const requestId = String(request.id ?? `${Date.now()}-${Math.random()}`);

			this.registerPending(requestId, accept, reject, 15000);

			safeSend(this.ws, {
				type: 'protooServerRequest',
				id: requestId,
				method: request.method,
				data: request.data,
			});
		});



		// mediasoup-demo 主动通知（无需回应）
		this.protoo.on('notification', (notification) => {
			safeSend(this.ws, {
				type: 'protooNotification',
				method: notification.method,
				data: notification.data,
			});
		});
	}

	// 关闭到 mediasoup-demo 的连接
	close() {
		if (this.protoo) {
			this.protoo.close();
			this.protoo = null;
		}
		this.clearPending('bridge closed');
	}


	// Spring Boot 对 mediasoup-demo 的 request 给出 accept/reject
	handleServerResponse(message) {
		const responseId = String(message.id);
		logInfo('handleServerResponse', { id: responseId, ok: message.ok });
		const pending = this.pendingServerRequests.get(responseId);


		if (!pending) {
			logError('no pending request for response', {
				id: responseId,
				pendingKeys: Array.from(this.pendingServerRequests.keys()),
			});
			return;
		}



		if (pending?.timer) {
			clearTimeout(pending.timer);
		}
		this.pendingServerRequests.delete(message.id);

		if (message.ok) {

			pending.accept(message.data);
		} else {
			const errorCode = message.errorCode || 500;
			const errorReason = message.errorReason || 'Request rejected by Spring Boot';
			pending.reject(errorCode, errorReason);
		}
	}
}

// WebSocket 服务：Spring Boot 连到这里
const wss = new WebSocketServer({ port: LISTEN_PORT });

wss.on('connection', (ws) => {
	logInfo('spring boot connected');
	const session = new BridgeSession(ws);

	ws.on('message', async (raw) => {
		let message;


		try {
			message = JSON.parse(raw.toString());
		} catch (error) {
			safeSend(ws, {
				type: 'error',
				message: 'Invalid JSON',
				details: serializeError(error),
			});
			return;
		}

		try {
			logInfo('node received', { type: message.type, id: message.id });
			switch (message.type) {

				// Spring Boot 请求：建立到 mediasoup 的连接
				case 'connect': {
					session.connect(message.data || {});
					safeSend(ws, { type: 'connectAck', ok: true });
					break;
				}
				// Spring Boot 请求：向 mediasoup 发送 protoo request
				case 'protooRequest': {
					if (!session.protoo) {
						throw new Error('protoo is not connected');
					}

					try {
						const response = await withTimeout(
							session.protoo.request(message.method, message.data || {}),
							15000,
							'protooRequest'
						);

						safeSend(ws, {
							type: 'protooResponse',
							id: message.id,
							ok: true,
							data: response,
						});
					} catch (error) {
						logError('protoo request failed', serializeError(error));
						safeSend(ws, {
							type: 'protooResponse',
							id: message.id,
							ok: false,
							errorReason: error?.message || 'protoo request failed',
						});
					}
					break;
				}

				// Spring Boot 请求：向 mediasoup 发送 protoo notification
				case 'protooNotification': {
					if (!session.protoo) {
						throw new Error('protoo is not connected');
					}

					session.protoo.notify(message.method, message.data || {});
					break;
				}
				// Spring Boot 回应：处理 mediasoup 过来的 request
				case 'protooServerResponse': {
					session.handleServerResponse(message);
					break;
				}
				// Spring Boot 通知：关闭连接
				case 'close': {
					session.close();
					break;
				}
				default: {
					safeSend(ws, {
						type: 'error',
						message: `Unknown message type: ${message.type}`,
					});
				}
			}
		} catch (error) {
			safeSend(ws, {
				type: 'error',
				message: error?.message || 'Unhandled error',
				details: serializeError(error),
				requestId: message?.id,
			});
		}
	});

	ws.on('close', () => {
		logInfo('spring boot disconnected');
		session.close();
	});
});

logInfo(`mediasoup bridge listening on ws://0.0.0.0:${LISTEN_PORT}`);


