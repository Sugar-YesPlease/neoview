	export class AsrStreamer {
	constructor({ sendStart, sendAudio, sendStop, onError }) {
		this.sendStart = sendStart;
		this.sendAudio = sendAudio;
		this.sendStop = sendStop;
		this.onError = onError;
		this.audioContext = null;
		this.processor = null;
		this.source = null;
		this.running = false;
		this.started = false;
		this.seq = 0;
		this.meta = null;
		this.lastSendTime = 0;  // 添加时间戳追踪
	}

	async start({ stream, roomId, userId, peerId, displayName }) {
		if (this.running || !stream) return;
		this.running = true;
		this.started = false;
		this.seq = 0;
		this.meta = { roomId, userId, peerId, displayName };

		// 诊断：检查输入流
		console.log('[ASR诊断] start() 被调用');
		console.log('[ASR诊断] stream:', stream);
		console.log('[ASR诊断] stream.getAudioTracks():', stream?.getAudioTracks?.());
		const track = stream?.getAudioTracks?.()?.[0];
		if (track) {
			console.log('[ASR诊断] 音频轨道:', {
				id: track.id,
				label: track.label,
				enabled: track.enabled,
				muted: track.muted,
				readyState: track.readyState,
				settings: track.getSettings?.()
			});
		} else {
			console.error('[ASR诊断] 没有找到音频轨道！');
		}

		try {
			this.audioContext = new AudioContext({ sampleRate: 16000 });
			this.source = this.audioContext.createMediaStreamSource(stream);
			this.processor = this.audioContext.createScriptProcessor(4096, 1, 1);

			this.processor.onaudioprocess = (event) => {
				if (!this.running) return;
				const inputBuffer = event.inputBuffer.getChannelData(0);
				const pcmData = downsampleBuffer(inputBuffer, event.inputBuffer.sampleRate, 16000);
				if (!pcmData || pcmData.length === 0) return;

				// 诊断日志：计算音频能量
				const rms = Math.sqrt(pcmData.reduce((sum, v) => sum + v * v, 0) / pcmData.length);
				const maxAmplitude = Math.max(...pcmData.map(Math.abs));
				
				if (this.seq % 20 === 0) {
					console.log('[ASR诊断] seq:', this.seq, 
						'采样率:', event.inputBuffer.sampleRate,
						'buffer长度:', pcmData.length,
						'RMS能量:', rms.toFixed(4),
						'最大振幅:', maxAmplitude.toFixed(4),
						'有声音:', rms > 0.01 ? '✅' : '❌');
				}

				if (!this.started) {
					this.started = true;
					console.log('[ASR诊断] 发送 asrStart, format=pcm, sampleRate=16000');
					this.sendStart?.({
						roomId,
						userId,
						peerId,
						displayName,
						format: 'pcm',
						sampleRate: 16000,
					});
				}

				const base64 = encodePCMToBase64(pcmData);
				// 只在有声音时打印日志
				if (rms > 0.01) {
					console.log('[ASR] 发送音频 seq:', this.seq, 'RMS:', rms.toFixed(4));
				}
				this.sendAudio?.({
					roomId,
					seq: this.seq++,
					audioBase64: base64,
				});
			};

			this.source.connect(this.processor);
			this.processor.connect(this.audioContext.destination);
		} catch (error) {
			this.onError?.(error);
			this.stop();
		}
	}

	stop() {
		if (!this.running) return;
		console.log('[ASR] 停止 ASR，发送 stop 信号');
		this.running = false;
		this.started = false;
		this.seq = 0;  // 重置序列号
		
		// 发送 stop 信号给后端
		try {
			this.sendStop?.(this.meta || {});
		} catch (error) {
			console.warn('[ASR] 发送 stop 信号失败:', error);
		}

		try {
			if (this.processor) {
				this.processor.disconnect();
				this.processor.onaudioprocess = null;
			}
			if (this.source) {
				this.source.disconnect();
			}
			if (this.audioContext) {
				this.audioContext.close();
			}
		} catch (error) {
			this.onError?.(error);
		}

		this.processor = null;
		this.source = null;
		this.audioContext = null;
		this.meta = null;
		this.lastSendTime = 0;
		console.log('[ASR] ASR 已完全停止');
	}
}

function downsampleBuffer(buffer, sampleRate, outSampleRate) {
	if (outSampleRate === sampleRate) {
		return buffer;
	}
	if (outSampleRate > sampleRate) {
		return buffer;
	}
	const sampleRateRatio = sampleRate / outSampleRate;
	const newLength = Math.round(buffer.length / sampleRateRatio);
	const result = new Float32Array(newLength);
	let offsetResult = 0;
	let offsetBuffer = 0;
	while (offsetResult < result.length) {
		const nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio);
		let accum = 0;
		let count = 0;
		for (let i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
			accum += buffer[i];
			count++;
		}
		result[offsetResult] = accum / count;
		offsetResult++;
		offsetBuffer = nextOffsetBuffer;
	}
	return result;
}

function encodePCMToBase64(float32Buffer) {
	const buffer = new ArrayBuffer(float32Buffer.length * 2);
	const view = new DataView(buffer);
	for (let i = 0; i < float32Buffer.length; i++) {
		let s = Math.max(-1, Math.min(1, float32Buffer[i]));
		view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
	}
	const uint8 = new Uint8Array(buffer);
	let binary = '';
	const chunkSize = 0x8000;
	for (let i = 0; i < uint8.length; i += chunkSize) {
		binary += String.fromCharCode.apply(null, uint8.subarray(i, i + chunkSize));
	}
	return btoa(binary);
}
