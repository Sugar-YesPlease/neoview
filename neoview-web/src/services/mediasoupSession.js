import * as mediasoupClient from 'mediasoup-client';
import { SignalingSocket } from './signaling';
import { getDeviceInfo } from '../utils/device';

export class MediasoupSession {
	constructor({ token, displayName, onState }) {
		this.token = token;
		this.displayName = displayName;
		this.onState = onState;
		this.signaling = new SignalingSocket(token);
		this.device = null;
		this.sendTransport = null;
		this.recvTransport = null;
		this.micProducer = null;
		this.camProducer = null;
		this.shareProducer = null;
		this.remoteConsumers = new Map();

		this.remoteConsumerMeta = new Map();
		this.closed = false;

	}

	async join(roomId) {
		this.onState?.('connecting');
		try {
			await this.signaling.connect();
			this.signaling.connectToRoom(roomId);

			this.signaling.on('serverRequest', (request) => this.handleServerRequest(request));
			this.signaling.on('notification', (notification) => this.handleNotification(notification));
			this.signaling.on('roomUserList', (payload) => this.handleRoomUserList(payload));
			this.signaling.on('roomUserJoin', (payload) => this.handleRoomUserJoin(payload));
			this.signaling.on('roomUserLeave', (payload) => this.handleRoomUserLeave(payload));
			this.signaling.on('roomMedia', (payload) => this.handleRoomMedia(payload));
			this.signaling.on('screenShareStopped', (payload) => this.handleScreenShareStopped(payload));
			this.signaling.on('asrResult', (payload) => this.handleAsrResult(payload));
			this.signaling.on('reaction', (payload) => this.handleReaction(payload));
			this.signaling.on('close', (event) => this.handleSignalingClosed(event));
			await this.signaling.waitFor('protooOpen', 15000);

			const { routerRtpCapabilities } = await this.signaling.request('getRouterRtpCapabilities');

			this.device = new mediasoupClient.Device();
			await this.device.load({ routerRtpCapabilities });

			await this.createTransports();

			const deviceInfo = getDeviceInfo();
			await this.signaling.request('join', {
				displayName: this.displayName,
				device: deviceInfo,
				rtpCapabilities: this.device.rtpCapabilities,
			});

			this.onState?.('connected');
		} catch (error) {
			this.close({ skipSignaling: false, reason: 'join failed' });
			throw error;
		}
	}


	handleSignalingClosed(event) {
		if (this.closed) return;
		this.onState?.({
			type: 'signalingClosed',
			code: event?.code,
			reason: event?.reason,
		});
		this.close({ skipSignaling: true, reason: 'signaling closed' });
	}

	async createTransports() {

		const sendInfo = await this.signaling.request('createWebRtcTransport', {
			forceTcp: false,
			appData: { direction: 'producer' },
		});

		this.sendTransport = this.device.createSendTransport({
			id: sendInfo.transportId,
			iceParameters: sendInfo.iceParameters,
			iceCandidates: sendInfo.iceCandidates,
			dtlsParameters: sendInfo.dtlsParameters,
			iceServers: [],
		});

		this.sendTransport.on('connect', ({ dtlsParameters }, callback, errback) => {
			this.signaling
				.request('connectWebRtcTransport', {
					transportId: this.sendTransport.id,
					dtlsParameters,
				})
				.then(callback)
				.catch(errback);
		});

		this.sendTransport.on('produce', ({ kind, rtpParameters, appData }, callback, errback) => {
			const source = appData?.source || (kind === 'audio' ? 'audio' : 'video');
			this.signaling
				.request('produce', {
					transportId: this.sendTransport.id,
					kind,
					rtpParameters,
					appData: { source, ...appData },
				})
				.then(({ producerId }) => callback({ id: producerId }))
				.catch(errback);
		});



		const recvInfo = await this.signaling.request('createWebRtcTransport', {
			forceTcp: false,
			appData: { direction: 'consumer' },
		});

		this.recvTransport = this.device.createRecvTransport({
			id: recvInfo.transportId,
			iceParameters: recvInfo.iceParameters,
			iceCandidates: recvInfo.iceCandidates,
			dtlsParameters: recvInfo.dtlsParameters,
			iceServers: [],
		});

		this.recvTransport.on('connect', ({ dtlsParameters }, callback, errback) => {
			this.signaling
				.request('connectWebRtcTransport', {
					transportId: this.recvTransport.id,
					dtlsParameters,
				})
				.then(callback)
				.catch(errback);
		});
	}

	async enableMic({ stream } = {}) {
		if (this.micProducer) return stream;
		const localStream = stream || await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
		const track = localStream?.getAudioTracks?.()[0];
		if (!track) return localStream;
		this.micProducer = await this.sendTransport.produce({ track });
		return localStream;
	}

	async replaceMicTrack(track) {
		if (!this.micProducer || !track) return;
		try {
			if (typeof this.micProducer.replaceTrack === 'function') {
				await this.micProducer.replaceTrack({ track });
				return;
			}
			if (typeof this.micProducer.track === 'object') {
				this.micProducer.track = track;
			}
		} catch (error) {
			console.warn('[mediasoup] replaceMicTrack failed', error?.message || error);
		}
	}


	async disableMic() {
		if (this.micProducer) {
			this.micProducer.close();
			this.micProducer = null;
		}
	}

	async enableCam({ stream } = {}) {
		if (this.camProducer) return stream;
		const localStream = stream || await navigator.mediaDevices.getUserMedia({ audio: false, video: true });
		const track = localStream?.getVideoTracks?.()[0];
		if (!track) return localStream;
		this.camProducer = await this.sendTransport.produce({ track });
		return localStream;
	}

	async replaceCamTrack(track) {
		if (!this.camProducer || !track) return;
		try {
			if (typeof this.camProducer.replaceTrack === 'function') {
				await this.camProducer.replaceTrack({ track });
				console.log('[mediasoup] replaceCamTrack ok', track.id);
				return;
			}
			if (typeof this.camProducer.track === 'object') {
				this.camProducer.track = track;
				console.log('[mediasoup] replaceCamTrack assigned', track.id);
			}
		} catch (error) {
			console.warn('[mediasoup] replaceCamTrack failed', error?.message || error);
		}
	}


	async disableCam() {
		if (this.camProducer) {
			this.camProducer.close();
			this.camProducer = null;
		}
	}

	async enableScreenShare({ stream } = {}) {
		if (this.shareProducer) return stream;
		const localStream = stream || await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
		const track = localStream?.getVideoTracks?.()[0];
		if (!track) return localStream;
		this.shareProducer = await this.sendTransport.produce({
			track,
			appData: { source: 'screensharing' },
		});
		return localStream;
	}

	async disableScreenShare() {
		if (this.shareProducer) {
			this.shareProducer.close();
			this.shareProducer = null;
		}
	}


	async handleServerRequest(request) {

		try {
			console.log('[mediasoup] server request', request.method, request.id, request.data);
			if (request.method === 'newConsumer') {
				const data = request.data;
				console.log('[mediasoup] newConsumer 数据:', {
					consumerId: data.consumerId,
					producerId: data.producerId,
					kind: data.kind,
					peerId: data.peerId,
					appData: data.appData,
				});
				
				const consumer = await this.recvTransport.consume({
					id: data.consumerId,
					producerId: data.producerId,
					kind: data.kind,
					rtpParameters: data.rtpParameters,
					appData: data.appData,
				});

				this.remoteConsumers.set(consumer.id, consumer);

				consumer.track.enabled = true;
				const stream = new MediaStream();
				stream.addTrack(consumer.track);

				this.signaling.respond(request.id, true, {});
				
				// 提取用户信息（优先级：data.peerId > appData.peerId > producerId）
				const peerId = data.peerId || data.appData?.peerId || data.producerId;
				const displayName = data.displayName || data.appData?.displayName || peerId;
				
				console.log('[mediasoup] 提取用户信息:', { peerId, displayName });
				
				this.remoteConsumerMeta.set(consumer.id, { peerId, displayName });
				
				// 监听 track 事件（兜底处理暂停/恢复，避免远端卡帧）
				const emitPaused = () => {
					this.onState?.({
						type: 'consumerPaused',
						consumerId: consumer.id,
						peerId,
						displayName,
					});
				};
				const emitResumed = () => {
					this.onState?.({
						type: 'consumerResumed',
						consumerId: consumer.id,
						peerId,
						displayName,
					});
				};
				consumer.track.onended = () => {
					console.log('[mediasoup] track ended', consumer.id);
					emitPaused();
				};
				consumer.track.onmute = () => {
					console.log('[mediasoup] track muted', consumer.id);
					emitPaused();
				};
				consumer.track.onunmute = () => {
					console.log('[mediasoup] track unmuted', consumer.id);
					emitResumed();
				};
				
				// 传递 peerId 和 displayName 以便前端显示
				this.onState?.({ 
					type: 'consumer', 
					consumer, 
					stream,
					peerId: peerId,
					displayName: displayName,
					appData: data.appData,
				});


				return;
			}

			this.signaling.respond(request.id, false, null, '403', '未支持的请求');
		} catch (error) {
			console.error('[mediasoup] handleServerRequest failed', error);
			this.signaling.respond(request.id, false, null, '500', error.message);
		}
	}

	handleNotification(notification) {
		console.log('[mediasoup] notification', notification.method);
		
		// 处理 producer 关闭（远端用户关闭摄像头/麦克风）
		if (notification.method === 'producerClosed') {
			const { producerId } = notification.data || {};
			console.log('[mediasoup] producer closed', producerId);
			
			// 找到对应的 consumer 并关闭
			for (const [consumerId, consumer] of this.remoteConsumers.entries()) {
				if (consumer.producerId === producerId) {
					consumer.close();
					this.remoteConsumers.delete(consumerId);
					this.remoteConsumerMeta.delete(consumerId);
					
					// 通知前端移除对应的视频/音频流
					this.onState?.({
						type: 'consumerClosed',
						consumerId,
						producerId,
					});
					break;
				}
			}
		}
		
		// 处理 peer 离开（远端用户完全离开会议）
		if (notification.method === 'peerClosed') {
			const { peerId } = notification.data || {};
			console.log('[mediasoup] peer closed', peerId);
			
			// 关闭该 peer 的所有 consumer
			for (const [consumerId, consumer] of this.remoteConsumers.entries()) {
				if (consumer.appData?.peerId === peerId) {
					consumer.close();
					this.remoteConsumers.delete(consumerId);
					this.remoteConsumerMeta.delete(consumerId);
					
					this.onState?.({
						type: 'consumerClosed',
						consumerId,
						peerId,
					});
				}
			}
		}
		
		// 处理 producer 暂停/恢复（远端停止/开启视频时常见）
		if (notification.method === 'producerPaused') {
			const { producerId } = notification.data || {};
			console.log('[mediasoup] producer paused', producerId);
			for (const [consumerId, consumer] of this.remoteConsumers.entries()) {
				if (consumer.producerId === producerId) {
					consumer.pause();
					const meta = this.remoteConsumerMeta.get(consumerId) || {};
					this.onState?.({
						type: 'consumerPaused',
						consumerId,
						peerId: meta.peerId,
						displayName: meta.displayName,
					});
				}
			}
		}

		if (notification.method === 'producerResumed') {
			const { producerId } = notification.data || {};
			console.log('[mediasoup] producer resumed', producerId);
			for (const [consumerId, consumer] of this.remoteConsumers.entries()) {
				if (consumer.producerId === producerId) {
					consumer.resume();
					const meta = this.remoteConsumerMeta.get(consumerId) || {};
					this.onState?.({
						type: 'consumerResumed',
						consumerId,
						peerId: meta.peerId,
						displayName: meta.displayName,
					});
				}
			}
		}

		// 处理 consumer 暂停/恢复
		if (notification.method === 'consumerPaused') {
			const { consumerId } = notification.data || {};
			const consumer = this.remoteConsumers.get(consumerId);
			if (consumer) {
				consumer.pause();
			}
			const meta = this.remoteConsumerMeta.get(consumerId) || {};
			this.onState?.({
				type: 'consumerPaused',
				consumerId,
				peerId: meta.peerId,
				displayName: meta.displayName,
			});
		}
		
		if (notification.method === 'consumerResumed') {
			const { consumerId } = notification.data || {};
			const consumer = this.remoteConsumers.get(consumerId);
			if (consumer) {
				consumer.resume();
			}
			const meta = this.remoteConsumerMeta.get(consumerId) || {};
			this.onState?.({
				type: 'consumerResumed',
				consumerId,
				peerId: meta.peerId,
				displayName: meta.displayName,
			});
		}
	}

	handleRoomUserList(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'roomUserList',
			roomId: data.roomId,
			selfUserId: data.selfUserId,
			selfPeerId: data.selfPeerId,
			users: Array.isArray(data.users) ? data.users : [],
		});
	}

	handleRoomUserJoin(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'roomUserJoin',
			roomId: data.roomId,
			user: data.user,
		});
	}

	handleRoomUserLeave(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'roomUserLeave',
			roomId: data.roomId,
			user: data.user,
		});
	}

	handleRoomMedia(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'roomMedia',
			roomId: data.roomId,
			user: data.user,
			kind: data.kind,
			enabled: data.enabled,
		});
	}

	handleScreenShareStopped(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'screenShareStopped',
			roomId: data.roomId,
			user: data.user,
			reason: data.reason,
		});
	}

	handleAsrResult(payload) {

		const data = payload?.data || {};
		this.onState?.({
			type: 'asrResult',
			roomId: data.roomId,
			user: data.user,
			text: data.text,
			isFinal: data.isFinal,
		});
	}

	handleReaction(payload) {
		const data = payload?.data || {};
		this.onState?.({
			type: 'reaction',
			roomId: data.roomId,
			user: data.user,
			emoji: data.emoji,
			timestamp: data.timestamp,
		});
	}

	notifyRoomMedia(kind, enabled) {
		try {
			this.signaling.send({
				type: 'roomMedia',
				data: { kind, enabled },
			});
		} catch (error) {
			console.warn('[mediasoup] notifyRoomMedia failed', error?.message || error);
		}
	}

	notifyScreenShareStopped(reason = 'user-stop') {
		try {
			this.signaling.send({
				type: 'screenShareStopped',
				data: { reason },
			});
		} catch (error) {
			console.warn('[mediasoup] notifyScreenShareStopped failed', error?.message || error);
		}
	}

	sendReaction(emoji) {
		try {
			if (!emoji || !this.signaling?.isOpen?.()) return;
			this.signaling.send({
				type: 'reaction',
				data: { emoji },
			});
		} catch (error) {
			console.warn('[mediasoup] sendReaction failed', error?.message || error);
		}
	}

	close({ skipSignaling = false } = {}) {
		if (this.closed) return;
		this.closed = true;

		this.disableMic();
		this.disableCam();
		this.disableScreenShare();
		this.sendTransport?.close();

		this.recvTransport?.close();
		if (!skipSignaling) {
			this.signaling.close();
		}
		for (const consumer of this.remoteConsumers.values()) {
			try {
				consumer.close();
			} catch (error) {
				console.warn('[mediasoup] close consumer failed', error?.message || error);
			}
		}
		this.remoteConsumers.clear();
		this.remoteConsumerMeta.clear();
		this.onState?.('closed');
	}


	sendAsrStart(data) {
		try {
			console.log(407, this.signaling?.isOpen())
			if (!this.signaling?.isOpen?.()) return;
			this.signaling.send({
				type: 'asrStart',
				data,
			});
		} catch (error) {
			console.warn('[mediasoup] sendAsrStart failed', error?.message || error);
		}
	}

	sendAsrAudio(data) {
		try {
			if (!this.signaling?.isOpen?.()) return;
			this.signaling.send({
				type: 'asrAudio',
				data,
			});
		} catch (error) {
			console.warn('[mediasoup] sendAsrAudio failed', error?.message || error);
		}
	}

	sendAsrStop(data) {
		try {
			if (!this.signaling?.isOpen?.()) return;
			this.signaling.send({
				type: 'asrStop',
				data,
			});
		} catch (error) {
			console.warn('[mediasoup] sendAsrStop failed', error?.message || error);
		}
	}
}
