/**
 * recordingService.js
 * 屏幕录制服务
 *
 * 功能：
 * - 录制整个屏幕画面（getDisplayMedia，支持选择整个屏幕/应用窗口/标签页）
 * - 同时采集系统声音（displayMedia audio: true）和麦克风声音（可选）
 * - 使用 MediaRecorder API 编码为 WebM/MP4 格式
 * - 录制完成后自动触发下载保存到本地
 * - 提供录制时长、状态等实时信息
 *
 * 适用场景：教育机构授课录制、课堂回放、会议记录等
 */

/**
 * 检测浏览器支持的最优录制格式
 * @returns {string} MIME type 字符串
 */
export function detectBestMimeType() {
  const candidates = [
    'video/webm; codecs=vp9,opus',
    'video/webm; codecs=vp8,opus',
    'video/webm; codecs=h264,opus',
    'video/webm',
    'video/mp4; codecs=h264,aac',
    'video/mp4',
  ];
  for (const mime of candidates) {
    if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(mime)) {
      return mime;
    }
  }
  return '';
}

/**
 * 格式化秒数为 HH:MM:SS 字符串
 * @param {number} totalSeconds
 * @returns {string}
 */
function formatDuration(totalSeconds) {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [h, m, s].map((v) => String(v).padStart(2, '0')).join(':');
}

/**
 * 从 MIME 类型推断文件扩展名
 * @param {string} mimeType
 * @returns {string}
 */
function mimeToExtension(mimeType) {
  if (mimeType.startsWith('video/mp4')) return 'mp4';
  return 'webm';
}

/**
 * RecordingService —— 屏幕录制服务类
 *
 * 使用示例：
 *   const svc = new RecordingService();
 *   await svc.start({ includeMic: true });
 *   // ...一段时间后...
 *   svc.stop(); // 停止并触发下载
 *
 * 事件回调：
 *   onStatusChange(status)   status: 'idle' | 'requesting' | 'recording' | 'stopping' | 'saving'
 *   onDurationUpdate(str)    每秒更新一次时长字符串，如 "00:01:23"
 *   onError(errorMsg)        录制过程中发生错误时触发
 *   onSaved(filename)        文件下载完毕后触发
 */
export class RecordingService {
  constructor() {
    /** @type {'idle' | 'requesting' | 'recording' | 'stopping' | 'saving'} */
    this.status = 'idle';

    /** @type {MediaStream | null} 屏幕（+系统声音）流 */
    this._screenStream = null;

    /** @type {MediaStream | null} 麦克风流 */
    this._micStream = null;

    /** @type {MediaStream | null} 合并后用于录制的最终流 */
    this._recordStream = null;

    /** @type {MediaRecorder | null} */
    this._recorder = null;

    /** @type {Blob[]} 录制数据块 */
    this._chunks = [];

    /** @type {number} 录制开始时间戳（ms） */
    this._startTime = 0;

    /** @type {number | null} 计时器 ID */
    this._durationTimer = null;

    /** @type {number} 当前录制时长（秒） */
    this._durationSeconds = 0;

    /** @type {string} 当前使用的 MIME 类型 */
    this.mimeType = '';

    // --- 回调 ---
    /** @type {((status: string) => void) | null} */
    this.onStatusChange = null;
    /** @type {((durationStr: string) => void) | null} */
    this.onDurationUpdate = null;
    /** @type {((errorMsg: string) => void) | null} */
    this.onError = null;
    /** @type {((filename: string) => void) | null} */
    this.onSaved = null;
  }

  // ─────────────────────────────────────────────
  //  Public API
  // ─────────────────────────────────────────────

  /**
   * 开始录制
   *
   * @param {object} options
   * @param {boolean} [options.includeMic=false]         是否同时录制麦克风
   * @param {MediaStream | null} [options.existingMicStream=null]
   *   传入已有的麦克风 MediaStream（如项目中的 micStream），
   *   若不传则会重新申请 getUserMedia
   * @param {number} [options.videoBitsPerSecond=3000000] 视频码率（bps），默认 3Mbps
   * @param {number} [options.audioBitsPerSecond=128000]  音频码率（bps），默认 128kbps
   *
   * @returns {Promise<void>}
   */
  async start({
    includeMic = false,
    existingMicStream = null,
    videoBitsPerSecond = 3_000_000,
    audioBitsPerSecond = 128_000,
  } = {}) {
    if (this.status !== 'idle') {
      console.warn('[Recording] 已在录制中，忽略重复启动请求');
      return;
    }

    this._setStatus('requesting');
    this._chunks = [];
    this._durationSeconds = 0;

    try {
      // ① 请求屏幕捕获（含系统声音）
      console.log('[Recording] 请求屏幕捕获权限...');
      this._screenStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          // 尝试请求高分辨率（用户仍可在系统弹窗中选择实际分辨率）
          width: { ideal: 1920 },
          height: { ideal: 1080 },
          frameRate: { ideal: 30, max: 60 },
          cursor: 'always',       // 录制鼠标光标
          displaySurface: 'monitor', // 默认建议选整个屏幕
        },
        audio: {
          // 请求系统声音（Windows/macOS 共享选项中勾选后可获取）
          // Chrome 支持；Firefox/Safari 对系统声音支持有限
          echoCancellation: false,
          noiseSuppression: false,
          sampleRate: 48000,
        },
      });
      console.log('[Recording] 屏幕流已获取，tracks:', this._screenStream.getTracks().map((t) => `${t.kind}:${t.label}`));

      // ② 监听用户在系统层面停止共享（点击浏览器"停止共享"按钮）
      const videoTrack = this._screenStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.addEventListener('ended', () => {
          console.log('[Recording] 屏幕共享被用户停止（系统按钮）');
          this.stop();
        }, { once: true });
      }

      // ③ 收集音频轨道（系统声音 + 可选麦克风）
      const audioTracks = [];

      // 系统声音轨道
      const systemAudioTracks = this._screenStream.getAudioTracks();
      if (systemAudioTracks.length > 0) {
        console.log('[Recording] 已获取系统声音轨道，数量:', systemAudioTracks.length);
        audioTracks.push(...systemAudioTracks);
      } else {
        console.warn('[Recording] 未获取到系统声音轨道（用户未勾选"共享音频"，或浏览器/系统不支持）');
      }

      // 麦克风轨道
      if (includeMic) {
        try {
          if (existingMicStream) {
            // 优先复用已有的麦克风流（避免重复弹权限）
            const micTracks = existingMicStream.getAudioTracks();
            if (micTracks.length > 0) {
              // 克隆轨道，避免影响原始流
              micTracks.forEach((t) => audioTracks.push(t.clone()));
              console.log('[Recording] 复用现有麦克风轨道');
            }
          } else {
            this._micStream = await navigator.mediaDevices.getUserMedia({
              audio: {
                echoCancellation: true,
                noiseSuppression: true,
                sampleRate: 48000,
              },
              video: false,
            });
            this._micStream.getAudioTracks().forEach((t) => audioTracks.push(t));
            console.log('[Recording] 新建麦克风轨道');
          }
        } catch (micErr) {
          console.warn('[Recording] 麦克风获取失败（继续录制，无麦克风音频）:', micErr?.message || micErr);
        }
      }

      // ④ 合并视频轨道 + 所有音频轨道 → 最终录制流
      if (audioTracks.length > 1) {
        // 多路音频需要通过 AudioContext 混合
        this._recordStream = await this._mixAudioTracks(
          this._screenStream.getVideoTracks(),
          audioTracks,
        );
      } else {
        // 单路音频或无音频直接合并
        const allTracks = [
          ...this._screenStream.getVideoTracks(),
          ...audioTracks,
        ];
        this._recordStream = new MediaStream(allTracks);
      }

      console.log('[Recording] 最终录制流 tracks:', this._recordStream.getTracks().map((t) => `${t.kind}:${t.label || t.id.slice(0, 8)}`));

      // ⑤ 创建 MediaRecorder
      this.mimeType = detectBestMimeType();
      const recorderOptions = { videoBitsPerSecond, audioBitsPerSecond };
      if (this.mimeType) {
        recorderOptions.mimeType = this.mimeType;
      }

      this._recorder = new MediaRecorder(this._recordStream, recorderOptions);
      console.log('[Recording] MediaRecorder 已创建，mimeType:', this._recorder.mimeType);

      this._recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          this._chunks.push(event.data);
        }
      };

      this._recorder.onstop = () => {
        console.log('[Recording] MediaRecorder stopped，chunks:', this._chunks.length);
        this._saveFile();
      };

      this._recorder.onerror = (event) => {
        const msg = event?.error?.message || '录制发生未知错误';
        console.error('[Recording] MediaRecorder error:', msg);
        this._emitError(msg);
        this._cleanup();
      };

      // ⑥ 启动录制（每 1 秒触发一次 dataavailable，防止内存过大）
      this._recorder.start(1000);
      this._startTime = Date.now();
      this._startDurationTimer();
      this._setStatus('recording');
      console.log('[Recording] 录制已开始');

    } catch (err) {
      console.error('[Recording] 启动录制失败:', err?.message || err);
      this._cleanup();
      // 用户取消权限授予时不报错，其他错误才通知
      if (err?.name !== 'NotAllowedError' && err?.name !== 'AbortError') {
        this._emitError('启动录制失败：' + (err?.message || err));
      } else {
        this._setStatus('idle');
      }
    }
  }

  /**
   * 停止录制并保存文件
   */
  stop() {
    if (this.status !== 'recording') {
      console.warn('[Recording] 当前不在录制状态，忽略 stop()');
      return;
    }
    console.log('[Recording] 停止录制...');
    this._setStatus('stopping');
    this._stopDurationTimer();

    try {
      if (this._recorder && this._recorder.state !== 'inactive') {
        this._recorder.stop(); // 触发 onstop → _saveFile
      } else {
        this._saveFile();
      }
    } catch (err) {
      console.error('[Recording] 停止录制时出错:', err?.message || err);
      this._saveFile();
    }
  }

  /**
   * 强制取消录制（不保存）
   */
  cancel() {
    console.log('[Recording] 取消录制');
    this._stopDurationTimer();
    if (this._recorder && this._recorder.state !== 'inactive') {
      try {
        // 移除 onstop 回调，避免触发保存
        this._recorder.onstop = null;
        this._recorder.stop();
      } catch (_) { /* ignore */ }
    }
    this._chunks = [];
    this._cleanup();
  }

  /**
   * 当前是否正在录制
   * @returns {boolean}
   */
  get isRecording() {
    return this.status === 'recording';
  }

  /**
   * 获取当前录制时长字符串（HH:MM:SS）
   * @returns {string}
   */
  get durationString() {
    return formatDuration(this._durationSeconds);
  }

  // ─────────────────────────────────────────────
  //  Private helpers
  // ─────────────────────────────────────────────

  /**
   * 使用 AudioContext 混合多路音频轨道
   * @param {MediaStreamTrack[]} videoTracks
   * @param {MediaStreamTrack[]} audioTracks
   * @returns {Promise<MediaStream>}
   */
  async _mixAudioTracks(videoTracks, audioTracks) {
    const audioCtx = new AudioContext({ sampleRate: 48000 });
    const destination = audioCtx.createMediaStreamDestination();

    for (const track of audioTracks) {
      try {
        const src = audioCtx.createMediaStreamSource(new MediaStream([track]));
        src.connect(destination);
      } catch (err) {
        console.warn('[Recording] 混合音频轨道失败，跳过:', err?.message || err);
      }
    }

    // 保存 AudioContext 以便后续关闭
    this._audioCtx = audioCtx;

    const mixedAudioTracks = destination.stream.getAudioTracks();
    return new MediaStream([...videoTracks, ...mixedAudioTracks]);
  }

  /**
   * 保存录制文件到本地
   */
  _saveFile() {
    this._setStatus('saving');
    console.log('[Recording] 开始保存文件，总大小:', this._chunks.reduce((s, c) => s + c.size, 0), 'bytes');

    try {
      const finalMime = this._recorder?.mimeType || this.mimeType || 'video/webm';
      const ext = mimeToExtension(finalMime);
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      const filename = `recording-${timestamp}.${ext}`;

      const blob = new Blob(this._chunks, { type: finalMime });
      console.log('[Recording] Blob 大小:', blob.size, 'bytes，文件名:', filename);

      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      anchor.style.display = 'none';
      document.body.appendChild(anchor);
      anchor.click();

      // 延迟释放 URL，确保下载触发
      setTimeout(() => {
        URL.revokeObjectURL(url);
        document.body.removeChild(anchor);
        console.log('[Recording] 文件下载已触发:', filename);
      }, 1000);

      if (typeof this.onSaved === 'function') {
        this.onSaved(filename);
      }
    } catch (err) {
      console.error('[Recording] 保存文件失败:', err?.message || err);
      this._emitError('保存录制文件失败：' + (err?.message || err));
    } finally {
      this._cleanup();
    }
  }

  /**
   * 启动录制时长计时器
   */
  _startDurationTimer() {
    this._stopDurationTimer();
    this._durationTimer = setInterval(() => {
      this._durationSeconds = Math.floor((Date.now() - this._startTime) / 1000);
      if (typeof this.onDurationUpdate === 'function') {
        this.onDurationUpdate(formatDuration(this._durationSeconds));
      }
    }, 1000);
  }

  /**
   * 停止录制时长计时器
   */
  _stopDurationTimer() {
    if (this._durationTimer !== null) {
      clearInterval(this._durationTimer);
      this._durationTimer = null;
    }
  }

  /**
   * 清理所有资源
   */
  _cleanup() {
    this._stopDurationTimer();

    // 停止屏幕流所有轨道
    if (this._screenStream) {
      this._screenStream.getTracks().forEach((t) => t.stop());
      this._screenStream = null;
    }

    // 停止麦克风流（仅停止本服务自己创建的）
    if (this._micStream) {
      this._micStream.getTracks().forEach((t) => t.stop());
      this._micStream = null;
    }

    // 关闭混音 AudioContext
    if (this._audioCtx) {
      this._audioCtx.close().catch(() => {});
      this._audioCtx = null;
    }

    this._recordStream = null;
    this._recorder = null;
    this._chunks = [];

    this._setStatus('idle');
    console.log('[Recording] 资源已清理');
  }

  /**
   * 更新状态并触发回调
   * @param {string} newStatus
   */
  _setStatus(newStatus) {
    this.status = newStatus;
    if (typeof this.onStatusChange === 'function') {
      this.onStatusChange(newStatus);
    }
  }

  /**
   * 触发错误回调
   * @param {string} msg
   */
  _emitError(msg) {
    if (typeof this.onError === 'function') {
      this.onError(msg);
    }
  }
}
