import { WS_URL } from '../config';

export class SignalingSocket {
	constructor(token) {
		this.token = token;
		this.socket = null;
		this.pending = new Map();
		this.listeners = new Map();
		this.seq = 0;
		this.started = false;
		this.closed = false;

	}

	connect() {
		return new Promise((resolve, reject) => {
			const url = this.token ? `${WS_URL}?token=${encodeURIComponent(this.token)}` : WS_URL;
			this.socket = new WebSocket(url);

		this.socket.onopen = () => resolve();
		this.socket.onerror = () => reject(new Error('WebSocket 连接失败'));
		this.socket.onmessage = (event) => this.handleMessage(event);
		this.socket.onclose = (event) => {
			this.rejectAllPending(`WebSocket closed: ${event?.code || ''} ${event?.reason || ''}`.trim());
			this.emit('close', event);
		};

		});
	}


	send(payload) {
		if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
			throw new Error('WebSocket 未连接');
		}
		this.socket.send(JSON.stringify(payload));
	}

	connectToRoom(roomId) {
		this.send({ type: 'connect', data: { roomId } });
	}

	request(method, data, timeoutMs = 15000) {
		const id = crypto.randomUUID();
		return new Promise((resolve, reject) => {
			let timer = null;
			try {
				this.send({ type: 'protooRequest', id, method, data });
			} catch (error) {
				reject(error);
				return;
			}
			if (timeoutMs > 0) {
				timer = setTimeout(() => {
					this.pending.delete(id);
					reject(new Error(`${method} 请求超时`));
				}, timeoutMs);
			}
			this.pending.set(id, { resolve, reject, timer });
		});
	}


	notify(method, data) {
		this.send({ type: 'protooNotification', method, data });
	}

	respond(id, ok, data, errorCode, errorReason) {
		console.log('[signaling] respond', { id, ok, errorReason });
		this.send({
			type: 'protooServerResponse',
			id,
			ok,
			data,
			errorCode,
			errorReason,
		});
	}

	isOpen() {
		return this.socket && this.socket.readyState === WebSocket.OPEN;
	}


	on(event, handler) {
		this.listeners.set(event, handler);
	}

	waitFor(event, timeoutMs = 10000) {
		return new Promise((resolve, reject) => {
			const timer = setTimeout(() => {
				this.listeners.delete(event);
				reject(new Error(`${event} 超时`));
			}, timeoutMs);

			this.on(event, (payload) => {
				clearTimeout(timer);
				resolve(payload);
			});
		});
	}

	emit(event, payload) {
		const handler = this.listeners.get(event);
		if (handler) {
			handler(payload);
		}
	}


	handleMessage(event) {
		const message = JSON.parse(event.data);
		console.log('[signaling] message', message.type, message);


		if (message.type === 'protooResponse') {
			const pending = this.pending.get(message.id);
			if (pending) {
				this.pending.delete(message.id);
				if (pending.timer) {
					clearTimeout(pending.timer);
				}
				if (message.ok) {
					pending.resolve(message.data);
				} else {
					pending.reject(new Error(message.errorReason || '请求失败'));
				}
			}
			return;
		}


		if (message.type === 'protooServerRequest') {
			this.emit('serverRequest', message);
			return;
		}

		if (message.type === 'protooServerNotification' || message.type === 'protooNotification') {
			this.emit('notification', message);
			return;
		}

		this.emit(message.type, message);
	}

	rejectAllPending(reason = '连接已关闭') {
		for (const [id, pending] of this.pending.entries()) {
			if (pending?.timer) {
				clearTimeout(pending.timer);
			}
			pending.reject(new Error(reason));
			this.pending.delete(id);
		}
	}

	close() {
		if (this.socket) {
			this.closed = true;
			this.socket.close();
			this.socket = null;
		}
		this.rejectAllPending('连接已关闭');
	}


	process(roomId, userId, peerId, stream, displayName) {
		// let mediaStream = navigator.mediaDevices.getUserMedia({ audio: true });
        // let audioContext = new AudioContext();

		// const sourceNode = audioContext.createMediaStreamSource(mediaStream);
        // const processor = audioContext.createScriptProcessor(1024, 1, 1);

		// sourceNode.connect(processor);
        // processor.connect(audioContext.destination);

		// processor.onaudioprocess = (e) => {
		// 	const pcmData = e.inputBuffer.getChannelData(0);
		// 	const buffer = new Int16Array(pcmData.length);

		// 	// 转换为 16 位 PCM
		// 	for (let i = 0; i < pcmData.length; i++) {
		// 		buffer[i] = Math.max(-32768, Math.min(32767, pcmData[i] * 32768));
		// 	}

		// 	if (websocket.readyState === WebSocket.OPEN) {
		// 		this.send(buffer.buffer);
		// 	}
		// };


		try {
			this.started = false;
			let audioContext = new AudioContext({ sampleRate: 16000 });
			let source = audioContext.createMediaStreamSource(stream);
			let processor = audioContext.createScriptProcessor(4096, 1, 1);

			processor.onaudioprocess = (event) => {
				const inputBuffer = event.inputBuffer.getChannelData(0);
				const pcmData = downsampleBuffer(inputBuffer, event.inputBuffer.sampleRate, 16000);
				if (!pcmData || pcmData.length === 0) return;

				if (!this.started) {
					this.started = true;
					this.send({
						roomId,
						userId,
						peerId,
						displayName,
						format: 'pcm',
						sampleRate: 16000,
					});
				}

				const base64 = encodePCMToBase64(pcmData);
				this.send({
					roomId,
					seq: this.seq++,
					audioBase64: base64,
				});
			};

			source.connect(processor);
			processor.connect(audioContext.destination);
		} catch (error) {
			console.error(185, error);
		}
		
	}
}
