<template>
	<div class="app">
		<!-- 登录前：显示登录/注册界面 -->
		<div v-if="!token" class="auth-container">
			<!-- 飞船飞行背景层 -->
			<div class="warp-bg">
				<!-- 星云背景层 -->
				<div class="nebula-layer">
					<div class="nebula nebula-1"></div>
					<div class="nebula nebula-2"></div>
					<div class="nebula nebula-3"></div>
				</div>
				<!-- 银河光带 -->
				<div class="milky-way"></div>
				<!-- 3D 透视容器 -->
				<div class="warp-container">
					<!-- 远处星星（慢速） -->
					<div class="warp-stars-layer layer-far">
						<div v-for="n in 80" :key="'far-'+n" class="warp-star star-far" :style="getWarpStarStyle(n, 'far')"></div>
					</div>
					<!-- 中间星星（中速） -->
					<div class="warp-stars-layer layer-mid">
						<div v-for="n in 50" :key="'mid-'+n" class="warp-star star-mid" :style="getWarpStarStyle(n, 'mid')"></div>
					</div>
					<!-- 近处星星（快速） -->
					<div class="warp-stars-layer layer-near">
						<div v-for="n in 30" :key="'near-'+n" class="warp-star star-near" :style="getWarpStarStyle(n, 'near')"></div>
					</div>
				</div>
				<!-- 速度线效果 -->
				<div class="speed-lines">
					<div v-for="n in 20" :key="'line-'+n" class="speed-line" :style="getSpeedLineStyle(n)"></div>
				</div>
				<!-- 中心光晕（模拟飞船前方） -->
				<div class="warp-core"></div>
			</div>

			<div class="auth-box">
				<div class="auth-header">
					<div class="logo">
						<!-- 科技未来风格 Logo -->
					<svg width="56" height="56" viewBox="0 0 64 64" fill="none" class="logo-svg">
						<defs>
							<linearGradient id="orbitGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
								<stop offset="0%" stop-color="#06B6D4" />
								<stop offset="100%" stop-color="#3B82F6" />
							</linearGradient>
							<linearGradient id="orbitGrad2" x1="0%" y1="0%" x2="100%" y2="100%">
								<stop offset="0%" stop-color="#8B5CF6" />
								<stop offset="100%" stop-color="#EC4899" />
							</linearGradient>
							<linearGradient id="orbitGrad3" x1="0%" y1="0%" x2="100%" y2="100%">
								<stop offset="0%" stop-color="#22D3EE" />
								<stop offset="100%" stop-color="#A78BFA" />
							</linearGradient>
							<radialGradient id="coreGrad" cx="50%" cy="50%" r="50%">
								<stop offset="0%" stop-color="#FFFFFF" />
								<stop offset="50%" stop-color="#06B6D4" />
								<stop offset="100%" stop-color="#3B82F6" />
							</radialGradient>
							<filter id="orbitGlow" x="-50%" y="-50%" width="200%" height="200%">
								<feGaussianBlur stdDeviation="2" result="blur"/>
								<feMerge>
									<feMergeNode in="blur"/>
									<feMergeNode in="SourceGraphic"/>
								</feMerge>
							</filter>
						</defs>
						
						<!-- 外轨道环 -->
						<ellipse cx="32" cy="32" rx="28" ry="10" stroke="url(#orbitGrad1)" stroke-width="1" fill="none" opacity="0.6" filter="url(#orbitGlow)">
							<animateTransform attributeName="transform" type="rotate" from="0 32 32" to="360 32 32" dur="12s" repeatCount="indefinite" />
						</ellipse>
						
						<!-- 中轨道环 -->
						<ellipse cx="32" cy="32" rx="22" ry="14" stroke="url(#orbitGrad2)" stroke-width="1.5" fill="none" opacity="0.7" filter="url(#orbitGlow)">
							<animateTransform attributeName="transform" type="rotate" from="120 32 32" to="480 32 32" dur="8s" repeatCount="indefinite" />
						</ellipse>
						
						<!-- 内轨道环 -->
						<ellipse cx="32" cy="32" rx="16" ry="8" stroke="url(#orbitGrad3)" stroke-width="2" fill="none" opacity="0.8" filter="url(#orbitGlow)">
							<animateTransform attributeName="transform" type="rotate" from="240 32 32" to="600 32 32" dur="6s" repeatCount="indefinite" />
						</ellipse>
						
						<!-- 轨道卫星点 -->
						<circle r="2" fill="#06B6D4" filter="url(#orbitGlow)">
							<animateMotion dur="12s" repeatCount="indefinite" path="M 32,22 A 28,10 0 1,1 31.9,22" />
						</circle>
						<circle r="2.5" fill="#8B5CF6" filter="url(#orbitGlow)">
							<animateMotion dur="8s" repeatCount="indefinite" path="M 32,18 A 22,14 0 1,1 31.9,18" />
						</circle>
						<circle r="3" fill="#22D3EE" filter="url(#orbitGlow)">
							<animateMotion dur="6s" repeatCount="indefinite" path="M 32,24 A 16,8 0 1,1 31.9,24" />
						</circle>
						
						<!-- 中心核心 -->
						<circle cx="32" cy="32" r="8" fill="url(#coreGrad)" filter="url(#orbitGlow)">
							<animate attributeName="r" values="8;9;8" dur="2s" repeatCount="indefinite" />
							<animate attributeName="opacity" values="0.9;1;0.9" dur="2s" repeatCount="indefinite" />
						</circle>
						
						<!-- 核心光晕 -->
						<circle cx="32" cy="32" r="12" stroke="url(#orbitGrad1)" stroke-width="0.5" fill="none" opacity="0.4">
							<animate attributeName="r" values="12;14;12" dur="2s" repeatCount="indefinite" />
							<animate attributeName="opacity" values="0.4;0.2;0.4" dur="2s" repeatCount="indefinite" />
						</circle>
					</svg>
					<span class="logo-text">NEOVIEW</span>
				</div>
				<p class="tagline">新一代智能视频会议平台</p>
				</div>

				<div class="tab-row">
					<div class="tab-indicator" :style="{ transform: authTab === 'login' ? 'translateX(0)' : 'translateX(100%)' }"></div>
					<button :class="{ active: authTab === 'login' }" @click="authTab = 'login'">登录</button>
					<button :class="{ active: authTab === 'register' }" @click="authTab = 'register'">注册</button>
				</div>

				<form v-if="authTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
					<div class="input-group">
						<svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
							<circle cx="12" cy="7" r="4"></circle>
						</svg>
						<input v-model="authForm.username" placeholder="用户名" />
					</div>
					<div class="input-group">
						<svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
							<path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
						</svg>
						<input v-model="authForm.password" type="password" placeholder="密码" />
					</div>
					<button type="submit" class="primary-btn" :class="{ loading: isLoggingIn }">
						<span class="btn-text">登录</span>
						<span class="btn-loader"></span>
					</button>
				</form>

				<form v-else @submit.prevent="handleRegister" class="auth-form">
					<div class="input-group">
						<svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
							<circle cx="12" cy="7" r="4"></circle>
						</svg>
						<input v-model="authForm.username" placeholder="用户名" />
					</div>
					<div class="input-group">
						<svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
							<circle cx="12" cy="7" r="4"></circle>
						</svg>
						<input v-model="authForm.displayName" placeholder="显示名" />
					</div>
					<div class="input-group">
						<svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
							<rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
							<path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
						</svg>
						<input v-model="authForm.password" type="password" placeholder="密码" />
					</div>
					<button type="submit" class="primary-btn" :class="{ loading: isRegistering }">
						<span class="btn-text">注册</span>
						<span class="btn-loader"></span>
					</button>
				</form>

				<div v-if="statusText && !token" class="status-message" :class="{ error: statusText.includes('失败') || statusText.includes('错误') }">
					{{ statusText }}
				</div>
			</div>
		</div>

		<!-- 登录后未加入会议：显示会议大厅 -->
		<div v-else-if="!callActive" class="lobby-container">
			<header class="lobby-header">
				<div class="logo">
					<svg width="28" height="28" viewBox="0 0 32 32" fill="none">
						<rect width="32" height="32" rx="8" fill="#3B82F6"/>
						<path d="M16 8L24 16L16 24L8 16L16 8Z" fill="white"/>
					</svg>
					<span class="logo-text">NeoView</span>
				</div>
				<div class="user-info">
					<span class="username">{{ displayName }}</span>
					<button class="ghost-btn" @click="logout">退出登录</button>
				</div>
			</header>

			<main class="lobby-main">
				<h1>欢迎使用 NeoView</h1>
				<p class="subtitle">创建或加入会议，开启高效协作之旅</p>

				<div class="meeting-cards">
					<div class="meeting-card">
						<div class="card-icon">📹</div>
						<h3>创建会议</h3>
						<input v-model="meetingTitle" placeholder="输入会议标题" />
						<button @click="handleCreateMeeting" class="primary-btn">立即创建</button>
					</div>

					<div class="meeting-card">
						<div class="card-icon">🚪</div>
						<h3>加入会议</h3>
						<input v-model="joinRoomId" placeholder="输入会议ID" />
						<button
							@click="handleJoinMeeting"
							class="primary-btn"
							:disabled="isJoiningMeeting"
						>
							{{ isJoiningMeeting ? '加入中...' : '加入会议' }}
						</button>
					</div>
				</div>

				<div v-if="statusText" class="status-message">{{ statusText }}</div>
			</main>
		</div>

		<!-- 已加入会议：显示会议室界面 -->
		<MeetingRoom
			v-else
			:room-id="currentRoomId"
			:display-name="displayName"
			:local-stream="localStream"
			:remote-videos="remoteVideos"
			:remote-audios="remoteAudios"
			:participants="meetingParticipants"
			:self-peer-id="selfPeerId"
			:mic-enabled="micEnabled"
			:cam-enabled="camEnabled"
			:ai-denoise-enabled="aiDenoiseEnabled"
			:virtual-avatar-enabled="virtualAvatarEnabled"
			:avatar-type="avatarType"
			:participant-count="participantCount"
			:screen-share-stream="screenShareStream"
			:screen-share-owner="screenShareOwner"
			:screen-share-active="screenShareActive"
			:screen-share-disabled="screenShareDisabled"
			:screen-share-notice="screenShareNotice"

			:meeting-duration="meetingDuration"
			@share-stalled="handleShareStalled"


			:transcription-lines="transcriptionLines"
			:active-reactions="activeReactions"
			@toggle-mic="toggleMic"
			@toggle-cam="toggleCam"
			@toggle-ai-denoise="toggleAiDenoise"
			@toggle-virtual-avatar="toggleVirtualAvatar"
			@set-avatar-type="setAvatarType"
			@toggle-screen-share="toggleScreenShare"
			@resume-audio="resumeRemoteAudio"

			@leave="leaveCall"
			@clear-transcription="clearTranscriptionLines"
			@send-reaction="sendReaction"


		/>


	</div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import MeetingRoom from './components/MeetingRoom.vue';
import { login, register, createMeeting } from './services/api';
import { MediasoupSession } from './services/mediasoupSession';
import { AsrStreamer } from './services/asrStreamer';
import rnnoiseWasmUrl from '@jitsi/rnnoise-wasm/dist/rnnoise.wasm?url';




const authTab = ref('login');
const authForm = ref({ username: '', password: '', displayName: '' });
const isLoggingIn = ref(false);
const isRegistering = ref(false);

	// 飞船飞行星星样式生成 - 螺旋隧道效果
function getWarpStarStyle(n, layer) {
	// 螺旋参数：角度和半径
	const goldenAngle = 137.5; // 黄金角度，自然螺旋
	const angle = (n * goldenAngle + Math.random() * 30) * (Math.PI / 180);
	const armOffset = (Math.floor(n / 50) * 120) * (Math.PI / 180); // 3条旋臂
	const finalAngle = angle + armOffset;
	
	// 根据层级设置不同的起始半径、速度和大小（大幅增大尺寸）
	const config = {
		far: { startRadius: 5, endRadius: 80, duration: 3, size: 4, delay: Math.random() * 3 },
		mid: { startRadius: 3, endRadius: 90, duration: 2, size: 7, delay: Math.random() * 2 },
		near: { startRadius: 1, endRadius: 100, duration: 1.2, size: 10, delay: Math.random() * 1.2 }
	};
	
	const cfg = config[layer];
	
	return {
		'--start-radius': `${cfg.startRadius}%`,
		'--end-radius': `${cfg.endRadius}%`,
		'--angle': `${finalAngle}rad`,
		'--spiral-rotate': `${Math.random() * 360}deg`,
		width: `${cfg.size}px`,
		height: `${cfg.size}px`,
		animationDelay: `${cfg.delay}s`,
		animationDuration: `${cfg.duration}s`,
		'--end-scale': layer === 'near' ? '4' : layer === 'mid' ? '3' : '2'
	};
}

// 速度线样式生成
function getSpeedLineStyle(n) {
	const angle = Math.random() * 360;
	const length = Math.random() * 150 + 100;
	const delay = Math.random() * 2;
	const duration = Math.random() * 0.8 + 0.5;
	
	return {
		width: `${length}px`,
		animationDelay: `${delay}s`,
		animationDuration: `${duration}s`,
		'--angle': `${angle}deg`,
		'--line-opacity': `${Math.random() * 0.4 + 0.2}`
	};
}
const token = ref(sessionStorage.getItem('token') || '');
const displayName = ref(sessionStorage.getItem('displayName') || '');

const meetingTitle = ref('');
const joinRoomId = ref('');
const currentRoomId = ref('');
const statusText = ref('');
const callActive = ref(false);
const isJoiningMeeting = ref(false);
const micEnabled = ref(false);
const camEnabled = ref(false);
const aiDenoiseEnabled = ref(false);
const virtualAvatarEnabled = ref(false);
const avatarType = ref('cow');
const localStream = ref(null);
const screenShareStream = ref(null);
const screenShareOwner = ref({ peerId: '', displayName: '' });
const screenShareActive = ref(false);
const screenShareDisabled = ref(false);
const screenShareNotice = ref('');
let screenShareNoticeTimer = null;


const remoteVideos = ref([]);
const remoteAudios = ref([]);

const meetingParticipants = ref([]);
const selfUserId = ref('');
const selfPeerId = ref('');
const mediaStateByPeer = ref({});
const meetingStartTime = ref(0);
const meetingDuration = ref('00:00:00');
let session = null;
let durationTimer = null;

let asrStreamer = null;
const micStream = ref(null);
const transcriptionLines = ref([]);
const activeReactions = ref({});
const reactionTimers = new Map();
let rawMicStream = null;
let rawCamStream = null;
let rawScreenShareStream = null;
let screenShareConsumerId = '';
let screenShareProducerId = '';
let denoiseController = null;


let avatarController = null;
let faceMeshLoaderPromise = null;
let faceMeshBaseUrl = '';
let rnnoiseModulePromise = null;





// 计算参会人数（优先使用房间成员列表）
const participantCount = computed(() => {
	if (meetingParticipants.value.length > 0) {
		return meetingParticipants.value.length;
	}
	const uniquePeers = new Set();
	remoteVideos.value.forEach(v => {
		if (v.peerId) uniquePeers.add(v.peerId);
	});
	remoteAudios.value.forEach(a => {
		if (a.peerId) uniquePeers.add(a.peerId);
	});
	return uniquePeers.size + 1; // +1 是本地用户
});

const statusMap = {
	connecting: '正在连接会议...',
	connected: '已连接到会议',
	closed: '已断开连接',
};

function updateStatus(state) {
	console.log(153, state);
	if (typeof state === 'string') {
		statusText.value = statusMap[state] || state;
		return;
	}

	if (state?.type === 'signalingClosed') {
		console.warn('[App] 信令连接已断开', state);
		statusText.value = '信令连接已断开，请重新加入';
		leaveCall({ keepStatusText: true });
		return;
	}

	if (state?.type === 'roomUserList') {

		console.log('[App] 收到房间成员列表:', state.users);
		selfUserId.value = state.selfUserId || '';
		selfPeerId.value = state.selfPeerId || '';
		meetingParticipants.value = normalizeParticipants(state.users || []);
		return;
	}

	if (state?.type === 'roomUserJoin') {
		const user = state.user;
		console.log('[App] 新成员加入:', user);
		meetingParticipants.value = upsertParticipant(meetingParticipants.value, user);
		// 播放用户加入音效（仅当新加入者不是自己时播放）
		if (user?.peerId && selfPeerId.value && String(user.peerId) !== String(selfPeerId.value)) {
			playJoinSound();
		}
		return;
	}

	if (state?.type === 'roomUserLeave') {
		const user = state.user;
		console.log('[App] 成员离开:', user);
		if (!user || !user.peerId) return;
		meetingParticipants.value = meetingParticipants.value.filter(p => p.peerId !== String(user.peerId));
		if (screenShareOwner.value?.peerId && screenShareOwner.value.peerId === String(user.peerId)) {
			console.log('[Share] 共享发起人离开，清理共享');
			clearScreenShareState({ showNotice: true, noticeText: '共享已结束：共享者已离开' });
		}

		return;
	}

	if (state?.type === 'screenShareStopped') {
		const user = state.user || {};
		const peerId = user.peerId || state.peerId || '';
		const reason = state.reason || state?.data?.reason || '';
		console.log('[Share] 收到停止共享通知:', { peerId, reason });
		if (!screenShareActive.value) return;
		if (!peerId || !screenShareOwner.value?.peerId || screenShareOwner.value.peerId === String(peerId)) {
			clearScreenShareState({ showNotice: true, noticeText: '共享已结束' });
		}
		return;
	}


	if (state?.type === 'consumer') {

		const { consumer, stream, peerId, displayName, appData } = state;
		const source = appData?.source || (consumer.kind === 'audio' ? 'audio' : 'video');
		console.log('[App] 收到新的 consumer:', {
			kind: consumer.kind,
			consumerId: consumer.id,
			producerId: consumer.producerId,
			peerId: peerId || 'unknown',
			displayName: displayName || 'unknown',
			source,
		});

		if (consumer.kind === 'audio') {
			// 检查是否已存在该 consumer
			const existingIndex = remoteAudios.value.findIndex(a => a.consumerId === consumer.id);
			if (existingIndex === -1) {
				remoteAudios.value.push({ 
					id: consumer.id, 
					stream, 
					peerId, 
					displayName,
					consumerId: consumer.id,
					producerId: consumer.producerId
				});
				console.log('[App] 添加音频流，当前音频数量:', remoteAudios.value.length);
			} else {
				console.warn('[App] 音频流已存在，跳过添加');
			}
			return;
		}

		if (source === 'screensharing') {
			const shareTrack = stream?.getVideoTracks?.()?.[0] || null;
			
			// 检查 track 是否有效：如果 track 已经 ended，直接忽略
			if (!shareTrack || shareTrack.readyState === 'ended') {
				console.warn('[Share] 收到已失效的屏幕共享 consumer，忽略');
				return;
			}
			
			if (screenShareActive.value && screenShareOwner.value?.peerId === selfPeerId.value) {
				console.warn('[Share] 检测到他人共享，停止本地共享');
				stopScreenShare({ silent: true });
			}
			
			// 设置共享信息
			screenShareOwner.value = {
				peerId: peerId || '',
				displayName: displayName || peerId || '参会者',
			};
			screenShareConsumerId = consumer.id;
			screenShareProducerId = consumer.producerId;
			screenShareDisabled.value = !!selfPeerId.value && screenShareOwner.value.peerId !== selfPeerId.value;
			
			// 【关键】先激活 screenShareActive，确保 MeetingRoom 中的 video 元素已渲染
			screenShareActive.value = true;
			
			// 使用 setTimeout 确保 DOM 已更新，再设置 stream
			// 这样 MeetingRoom 的 watch 触发时，video 元素已存在，能正确绑定 srcObject
			setTimeout(() => {
				screenShareStream.value = stream;
				console.log('[Share] 远端共享 stream 已绑定');
			}, 0);
			
			console.log('[Share] 远端共享已激活:', {
				consumerId: consumer.id,
				producerId: consumer.producerId,
				peerId,
			});

			// 监听 track ended 事件
			if (shareTrack?.addEventListener) {
				shareTrack.addEventListener('ended', () => {
					if (screenShareConsumerId !== consumer.id) return;
					console.log('[Share] 远端共享轨道结束，清理共享');
					clearScreenShareState({ showNotice: true, noticeText: '共享已结束' });
				}, { once: true });
			}
			return;
		}

		// 同一用户重新开视频时，先移除旧的视频流
		if (peerId) {
			const before = remoteVideos.value.length;
			remoteVideos.value = remoteVideos.value.filter(v => v.peerId !== peerId);
			if (before !== remoteVideos.value.length) {
				console.log('[App] 替换同一用户旧视频流:', peerId);
			}
		}

		// 检查是否已存在该 consumer
		const existingIndex = remoteVideos.value.findIndex(v => v.consumerId === consumer.id);
		if (existingIndex === -1) {
			remoteVideos.value.push({ 
				id: consumer.id, 
				stream, 
				peerId, 
				displayName,
				consumerId: consumer.id,
				producerId: consumer.producerId,
				paused: !!mediaStateByPeer.value?.[peerId]?.videoPaused,
			});
			console.log('[App] 添加视频流，当前视频数量:', remoteVideos.value.length);
		} else {
			console.warn('[App] 视频流已存在，跳过添加');
		}
	}


	// 处理 peer 离开或 producer 关闭
	if (state?.type === 'consumerClosed') {
		const { consumerId, producerId } = state;

		console.log('[App] 关闭 consumer:', consumerId, 'producer:', producerId || '');
		
		if ((screenShareConsumerId && consumerId === screenShareConsumerId)
			|| (screenShareProducerId && producerId && producerId === screenShareProducerId)) {
			console.log('[Share] 共享屏幕已结束');
			clearScreenShareState({ showNotice: true, noticeText: '共享已结束' });
			return;
		}



		const videosBefore = remoteVideos.value.length;
		const audiosBefore = remoteAudios.value.length;
		
		remoteVideos.value = remoteVideos.value.filter(v => v.consumerId !== consumerId);
		remoteAudios.value = remoteAudios.value.filter(a => a.consumerId !== consumerId);
		
		console.log('[App] 清理后视频数量:', videosBefore, '->', remoteVideos.value.length);
		console.log('[App] 清理后音频数量:', audiosBefore, '->', remoteAudios.value.length);
	}


	if (state?.type === 'consumerPaused') {
		const { consumerId, peerId } = state;
		console.log('[App] consumer 暂停:', consumerId);
		// 屏幕共享 consumer 暂停时不立即清理，等待恢复或真正的关闭通知
		if (screenShareConsumerId && consumerId === screenShareConsumerId) {
			console.log('[Share] 远端共享 consumer 暂停，等待恢复');
			// 不清理状态，只标记暂停，等待 consumerResumed 或 consumerClosed
			return;
		}

		remoteVideos.value = remoteVideos.value.map(v => {
			if (v.consumerId === consumerId || (peerId && v.peerId === peerId)) {
				return { ...v, paused: true };
			}
			return v;
		});
	}


	if (state?.type === 'consumerResumed') {
		const { consumerId, peerId } = state;
		console.log('[App] consumer 恢复:', consumerId);
		remoteVideos.value = remoteVideos.value.map(v => {
			if (v.consumerId === consumerId || (peerId && v.peerId === peerId)) {
				return { ...v, paused: false };
			}
			return v;
		});
	}

	if (state?.type === 'roomMedia') {
		const { user, kind, enabled } = state;
		if (!user || !user.peerId) return;
		if (kind !== 'video') return;
		console.log('[App] roomMedia:', user.peerId, enabled);
		mediaStateByPeer.value = {
			...mediaStateByPeer.value,
			[user.peerId]: {
				...(mediaStateByPeer.value[user.peerId] || {}),
				videoPaused: !enabled,
			},
		};
		remoteVideos.value = remoteVideos.value.map(v => {
			if (v.peerId === user.peerId) {
				return { ...v, paused: !enabled };
			}
			return v;
		});
	}

	if (state?.type === 'reaction') {
		const user = state.user || {};
		const peerId = user.peerId || '';
		const emoji = state.emoji || '';
		if (!peerId || !emoji) return;
		activeReactions.value = {
			...activeReactions.value,
			[peerId]: emoji,
		};
		const oldTimer = reactionTimers.get(peerId);
		if (oldTimer) clearTimeout(oldTimer);
		const timer = setTimeout(() => {
			const next = { ...activeReactions.value };
			delete next[peerId];
			activeReactions.value = next;
			reactionTimers.delete(peerId);
		}, 1800);
		reactionTimers.set(peerId, timer);
		return;
	}

	if (state?.type === 'asrResult') {
		const user = state.user || {};
		const text = state.text || '';
		const isFinal = !!state.isFinal;
		updateTranscriptionLines({
			userId: user.userId || user.peerId || 'unknown',
			displayName: user.displayName || user.userId || user.peerId || '参会者',
			text,
			isFinal,
		});
		return;
	}
}

function normalizeParticipants(users) {
	const mapByUser = new Map();
	const mapByPeer = new Map();
	users.forEach((user) => {
		if (!user) return;
		const peerId = user.peerId ? String(user.peerId) : '';
		if (!peerId) return;
		const normalized = {
			userId: user.userId ? String(user.userId) : '',
			peerId,
			displayName: user.displayName || peerId,
		};
		if (normalized.userId) {
			mapByUser.set(normalized.userId, normalized);
			return;
		}
		mapByPeer.set(peerId, normalized);
	});
	return [...mapByUser.values(), ...mapByPeer.values()];
}

function upsertParticipant(list, user) {
	if (!user) return list;
	const peerId = user.peerId ? String(user.peerId) : '';
	if (!peerId) return list;
	const userId = user.userId ? String(user.userId) : '';
	const displayName = user.displayName || peerId;
	let next = list.slice();
	if (userId) {
		next = next.filter(p => p.userId !== userId || p.peerId === peerId);
	}
	const index = next.findIndex(p => p.peerId === peerId);
	if (index >= 0) {
		next[index] = { ...next[index], userId: userId || next[index].userId, displayName };
		return next;
	}
	next.push({ userId, peerId, displayName });
	return next;
}

function playJoinSound() {
	try {
		const audio = new Audio('/sounds/join.ogg');
		audio.volume = 0.5;
		audio.play().catch((error) => {
			console.warn('[App] 播放加入音效失败:', error?.message || error);
		});
	} catch (error) {
		console.warn('[App] 创建音效实例失败:', error?.message || error);
	}
}

function updateTranscriptionLines({ userId, displayName, text, isFinal }) {
	if (!text) return;
	const lines = transcriptionLines.value.slice();
	const existingIndex = lines.findIndex(line => line.userId === userId && !line.isFinal);
	if (existingIndex >= 0) {
		lines[existingIndex] = {
			...lines[existingIndex],
			text,
			isFinal,
			updatedAt: Date.now(),
		};
	} else {
		lines.push({
			id: crypto.randomUUID(),
			userId,
			displayName,
			text,
			isFinal,
			createdAt: Date.now(),
		});
	}

	if (lines.length > 200) {
		lines.splice(0, lines.length - 200);
	}
	transcriptionLines.value = lines;
}

function clearTranscriptionLines() {
	transcriptionLines.value = [];
}


async function loadRnnoiseModule() {
	if (rnnoiseModulePromise) return rnnoiseModulePromise;
	rnnoiseModulePromise = import('@jitsi/rnnoise-wasm')
		.then(async (mod) => {
			console.log('[AI Denoise] RNNoise 模块已导入', mod);
			console.log('[AI Denoise] RNNoise WASM URL:', rnnoiseWasmUrl);
			
			const moduleOptions = {
				locateFile: (path) => {
					if (path.endsWith('.wasm')) {
						return rnnoiseWasmUrl;
					}
					return path;
				},
			};
			
			// @jitsi/rnnoise-wasm 导出的是 { createRNNWasmModule, createRNNWasmModuleSync }
			// 优先使用异步版本
			if (mod.createRNNWasmModule) {
				console.log('[AI Denoise] 使用 createRNNWasmModule (异步)');
				const Module = await mod.createRNNWasmModule(moduleOptions);
				console.log('[AI Denoise] RNNoise WASM 模块异步初始化完成');
				return Module;
			}
			
			// 尝试同步版本
			if (mod.createRNNWasmModuleSync) {
				console.log('[AI Denoise] 使用 createRNNWasmModuleSync (同步)');
				const Module = mod.createRNNWasmModuleSync(moduleOptions);
				console.log('[AI Denoise] RNNoise WASM 模块同步初始化完成');
				return Module;
			}
			
			throw new Error('无法找到 createRNNWasmModule 或 createRNNWasmModuleSync');
		})
		.catch((error) => {
			console.error('[AI Denoise] RNNoise 模块加载失败:', error);
			rnnoiseModulePromise = null;
			throw error;
		});
	return rnnoiseModulePromise;
}

function createRnnoiseProcessor(moduleInstance) {
	if (!moduleInstance) {
		console.error('[AI Denoise] 模块实例为空');
		return null;
	}
	console.log('[AI Denoise] 初始化 RNNoise 处理器，模块类型:', typeof moduleInstance);
	console.log('[AI Denoise] 模块属性:', Object.keys(moduleInstance).slice(0, 20));
	
	// 优先使用 cwrap；如果不存在则使用 Emscripten 导出的 _rnnoise_* 函数
	try {
		const create = typeof moduleInstance.cwrap === 'function'
			? moduleInstance.cwrap('rnnoise_create', 'number', [])
			: moduleInstance._rnnoise_create;
		const destroy = typeof moduleInstance.cwrap === 'function'
			? moduleInstance.cwrap('rnnoise_destroy', 'void', ['number'])
			: moduleInstance._rnnoise_destroy;
		const process = typeof moduleInstance.cwrap === 'function'
			? moduleInstance.cwrap('rnnoise_process_frame', 'number', ['number', 'number', 'number'])
			: moduleInstance._rnnoise_process_frame;
		
		if (typeof create !== 'function' || typeof destroy !== 'function' || typeof process !== 'function') {
			console.error('[AI Denoise] ❌ RNNoise 导出函数不可用', {
				createType: typeof create,
				destroyType: typeof destroy,
				processType: typeof process,
			});
			return null;
		}
		
		const frameSize = 480;

		console.log('[AI Denoise] 创建 RNNoise 状态...');
		const statePtr = create();
		console.log('[AI Denoise] 状态指针:', statePtr);
		
		// 分配内存：输入/输出都是 float32（4字节）
		const inputPtr = moduleInstance._malloc(frameSize * 4);
		const outputPtr = moduleInstance._malloc(frameSize * 4);
		const inputHeapIndex = inputPtr >> 2;  // HEAPF32 使用 4 字节索引
		const outputHeapIndex = outputPtr >> 2; // HEAPF32 使用 4 字节索引
		
		console.log('[AI Denoise] ✅ RNNoise 初始化完成 - State:', statePtr, 'Input:', inputPtr, 'Output:', outputPtr);

		const processFrame = (frame) => {
			// RNNoise 期望 float PCM，幅度范围与 int16 等价（约 ±32768）
			for (let i = 0; i < frameSize; i++) {
				const sample = frame[i] || 0;
				moduleInstance.HEAPF32[inputHeapIndex + i] = sample * 32768;
			}
			
			// 调用 RNNoise 处理函数（签名: state, out, in）
			process(statePtr, outputPtr, inputPtr);
			
			// 读取 Float32 输出并缩放回 [-1, 1]
			const output = new Float32Array(frameSize);
			for (let i = 0; i < frameSize; i++) {
				const value = moduleInstance.HEAPF32[outputHeapIndex + i] / 32768;
				output[i] = Math.max(-1, Math.min(1, value));
			}
			
			return output;
		};



		const destroyProcessor = () => {
			try {
				console.log('[AI Denoise] 销毁 RNNoise 处理器');
				destroy(statePtr);
				moduleInstance._free(inputPtr);
				moduleInstance._free(outputPtr);
			} catch (error) {
				console.warn('[AI Denoise] RNNoise 释放失败', error?.message || error);
			}
		};

		return { processFrame, destroy: destroyProcessor };
	} catch (error) {
		console.error('[AI Denoise] ❌ 创建处理器时出错:', error);
		return null;
	}
}


async function createDenoiseStream(inputStream) {
	if (!inputStream) return null;
	console.log('[AI Denoise] 开始创建降噪流');
	
	// 克隆输入流以避免轨道冲突
	const clonedStream = inputStream.clone();
	const clonedTrack = clonedStream.getAudioTracks()[0];
	console.log('[AI Denoise] 克隆的音频轨道:', {
		id: clonedTrack?.id,
		enabled: clonedTrack?.enabled,
		muted: clonedTrack?.muted,
		readyState: clonedTrack?.readyState
	});
	
	// 先加载 RNNoise，再创建 AudioContext
	let rnnoiseProcessor = null;
	try {
		console.log('[AI Denoise] 开始加载 RNNoise 模块...');
		const rnnoiseModule = await loadRnnoiseModule();
		console.log('[AI Denoise] RNNoise 模块加载成功，开始创建处理器...');
		rnnoiseProcessor = createRnnoiseProcessor(rnnoiseModule);
		if (!rnnoiseProcessor?.processFrame) {
			console.error('[AI Denoise] ❌ RNNoise 处理器创建失败 - processFrame 不存在');
			return null;
		} else {
			console.log('[AI Denoise] ✅ RNNoise 处理器已就绪');
		}
	} catch (error) {
		console.error('[AI Denoise] ❌ RNNoise 加载失败', error?.message || error, error);
		return null;
	}
	
	// RNNoise 加载成功后再创建 AudioContext
	const audioContext = new AudioContext({ sampleRate: 48000 });
	await audioContext.resume();
	console.log('[AI Denoise] AudioContext 已创建，采样率:', audioContext.sampleRate);
	
	const source = audioContext.createMediaStreamSource(clonedStream);
	const processor = audioContext.createScriptProcessor(4096, 1, 1);
	const destination = audioContext.createMediaStreamDestination();

	const frameSize = 480;
	const inputBuffer = new Float32Array(frameSize);
	let inputOffset = 0;
	let frameCount = 0;
	let logInterval = 100; // 每处理100帧打印一次日志

	processor.onaudioprocess = (event) => {
		const input = event.inputBuffer.getChannelData(0);
		const output = event.outputBuffer.getChannelData(0);
		
		// 首次回调日志
		if (frameCount === 0) {
			const inputRMS = Math.sqrt(input.reduce((sum, v) => sum + v * v, 0) / input.length);
			console.log(`[AI Denoise] 首次音频回调 - buffer大小: ${input.length}, 输入RMS: ${inputRMS.toFixed(6)}`);
		}
		
		// rnnoiseProcessor 在这个函数创建时就已经初始化好了
		if (!rnnoiseProcessor?.processFrame) {
			console.error('[AI Denoise] ❌ 音频回调中处理器不可用');
			output.set(input);
			return;
		}

		let inputIndex = 0;
		let outputIndex = 0;

		while (inputIndex < input.length) {
			const remaining = frameSize - inputOffset;
			const toCopy = Math.min(remaining, input.length - inputIndex);
			
			for (let i = 0; i < toCopy; i++) {
				inputBuffer[inputOffset + i] = input[inputIndex + i];
			}
			
			inputIndex += toCopy;
			inputOffset += toCopy;

			if (inputOffset === frameSize) {
				try {
					const processed = rnnoiseProcessor.processFrame(inputBuffer);
					const toCopyOut = Math.min(frameSize, output.length - outputIndex);
					
					for (let i = 0; i < toCopyOut; i++) {
						const value = processed[i];
						if (Number.isFinite(value)) {
							output[outputIndex + i] = value;
						} else {
							output[outputIndex + i] = 0;
						}
					}
					
					outputIndex += toCopyOut;
					frameCount++;
					
					// 定期打印调试信息
					if (frameCount % logInterval === 0) {
						const inputRMS = Math.sqrt(inputBuffer.reduce((sum, v) => sum + v * v, 0) / frameSize);
						const outputRMS = Math.sqrt(processed.reduce((sum, v) => sum + v * v, 0) / frameSize);
						console.log(`[AI Denoise] 帧 ${frameCount}: 输入RMS=${inputRMS.toFixed(6)}, 输出RMS=${outputRMS.toFixed(6)}, 降噪率=${((1 - outputRMS / (inputRMS || 1)) * 100).toFixed(1)}%`);
					}
				} catch (error) {
					console.warn('[AI Denoise] 处理帧失败', error);
					for (let i = 0; i < frameSize && outputIndex < output.length; i++) {
						output[outputIndex++] = inputBuffer[i] || 0;
					}
				}
				inputOffset = 0;
			}
		}

		// 填充剩余输出
		while (outputIndex < output.length) {
			output[outputIndex++] = 0;
		}
	};

	source.connect(processor);
	processor.connect(destination);
	
	console.log('[AI Denoise] 降噪流创建完成');

	return {
		stream: destination.stream,
		clonedStream,
		close: async () => {
			try {
				console.log('[AI Denoise] 开始关闭降噪流');
				rnnoiseProcessor?.destroy?.();
				processor.disconnect();
				processor.onaudioprocess = null;
				source.disconnect();
				clonedStream.getTracks().forEach(track => track.stop());
				await audioContext.close();
				console.log('[AI Denoise] 降噪流已关闭');
			} catch (error) {
				console.warn('[AI Denoise] 关闭失败', error?.message || error);
			}
		},
	};
}

async function enableDenoiseForActiveMic() {
	console.log('[AI Denoise] 开始启用降噪');
	if (!rawMicStream || !session) {
		console.warn('[AI Denoise] 缺少原始音频流或会话');
		return;
	}
	
	const rawTracks = rawMicStream.getAudioTracks();
	console.log('[AI Denoise] 原始音频轨道状态:', rawTracks.map(t => ({ 
		id: t.id, 
		enabled: t.enabled, 
		muted: t.muted, 
		readyState: t.readyState 
	})));
	
	if (!denoiseController) {
		console.log('[AI Denoise] 创建降噪控制器');
		denoiseController = await createDenoiseStream(rawMicStream);
	}
	const denoisedStream = denoiseController?.stream;
	if (!denoisedStream) {
		console.warn('[AI Denoise] 降噪流创建失败');
		return;
	}
	
	const denoisedTrack = denoisedStream.getAudioTracks?.()?.[0];
	console.log('[AI Denoise] 降噪轨道状态:', {
		id: denoisedTrack?.id,
		enabled: denoisedTrack?.enabled,
		muted: denoisedTrack?.muted,
		readyState: denoisedTrack?.readyState
	});
	
	await session.replaceMicTrack?.(denoisedTrack);
	micStream.value = denoisedStream;
	console.log('[AI Denoise] 降噪已启用');
	restartAsr();
}

async function disableDenoiseForActiveMic() {
	console.log('[AI Denoise] 开始关闭降噪');
	if (!session) {
		console.warn('[AI Denoise] 缺少会话');
		return;
	}
	
	let rawTrack = rawMicStream?.getAudioTracks?.()[0];
	console.log('[AI Denoise] 原始轨道状态:', {
		exists: !!rawTrack,
		id: rawTrack?.id,
		readyState: rawTrack?.readyState
	});
	
	if (!rawTrack || rawTrack.readyState === 'ended') {
		console.log('[AI Denoise] 原始轨道已结束，重新获取麦克风');
		const refreshedStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
		rawMicStream = refreshedStream;
		rawTrack = refreshedStream.getAudioTracks?.()[0];
		console.log('[AI Denoise] 已获取新轨道:', rawTrack?.id);
	}
	
	if (!rawTrack) {
		console.warn('[AI Denoise] 无法获取原始轨道');
		return;
	}
	
	await session.replaceMicTrack?.(rawTrack);
	micStream.value = rawMicStream;
	cleanupDenoise();
	console.log('[AI Denoise] 降噪已关闭');
	restartAsr();
}


async function toggleAiDenoise() {
	aiDenoiseEnabled.value = !aiDenoiseEnabled.value;
	if (!micEnabled.value) return;
	if (aiDenoiseEnabled.value) {
		await enableDenoiseForActiveMic();
		return;
	}
	await disableDenoiseForActiveMic();
}

function cleanupDenoise() {
	if (denoiseController?.close) {
		denoiseController.close();
	}
	denoiseController = null;
}

function cleanupRawMicStream() {
	if (rawMicStream) {
		rawMicStream.getTracks().forEach(track => track.stop());
	}
	rawMicStream = null;
}

function cleanupRawCamStream() {
	if (rawCamStream) {
		rawCamStream.getTracks().forEach(track => track.stop());
	}
	rawCamStream = null;
}


function loadFaceMeshLib() {
	if (window.FaceMesh) return Promise.resolve({ FaceMesh: window.FaceMesh, baseUrl: faceMeshBaseUrl || '' });
	if (faceMeshLoaderPromise) return faceMeshLoaderPromise;
	const baseUrls = [
		'https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh',
		'https://unpkg.com/@mediapipe/face_mesh',
		'https://cdn.staticfile.org/mediapipe/face_mesh',
	];
	faceMeshLoaderPromise = new Promise(async (resolve, reject) => {
		for (const baseUrl of baseUrls) {
			try {
				await new Promise((scriptResolve, scriptReject) => {
					const script = document.createElement('script');
					script.src = `${baseUrl}/face_mesh.js`;
					script.crossOrigin = 'anonymous';
					script.onload = () => scriptResolve();
					script.onerror = (error) => scriptReject(error);
					document.head.appendChild(script);
				});
				if (window.FaceMesh) {
					faceMeshBaseUrl = baseUrl;
					console.log('[Avatar] FaceMesh 脚本加载成功:', baseUrl);
					resolve({ FaceMesh: window.FaceMesh, baseUrl });
					return;
				}
			} catch (error) {
				console.warn('[Avatar] FaceMesh 脚本加载失败:', baseUrl, error?.message || error);
			}
		}
		reject(new Error('FaceMesh 脚本加载失败，请检查网络或配置本地资源'));
	});
	return faceMeshLoaderPromise;
}


function getPoint(landmarks, index, width, height) {
	const point = landmarks[index];
	if (!point) return null;
	return { x: point.x * width, y: point.y * height };
}

function distance(a, b) {
	if (!a || !b) return 0;
	const dx = a.x - b.x;
	const dy = a.y - b.y;
	return Math.hypot(dx, dy);
}

function clamp(value, min, max) {
	return Math.max(min, Math.min(max, value));
}

function getEyeOpenRatio(landmarks, width, height, indices) {
	const left = getPoint(landmarks, indices.left, width, height);
	const right = getPoint(landmarks, indices.right, width, height);
	const top = getPoint(landmarks, indices.top, width, height);
	const bottom = getPoint(landmarks, indices.bottom, width, height);
	const eyeWidth = distance(left, right) || 1;
	const eyeHeight = distance(top, bottom);
	const ratio = eyeHeight / eyeWidth;
	const normalized = (ratio - 0.12) / 0.18;
	return clamp(normalized, 0, 1);
}

function getMouthOpenRatio(landmarks, width, height) {
	const upper = getPoint(landmarks, 13, width, height);
	const lower = getPoint(landmarks, 14, width, height);
	const left = getPoint(landmarks, 78, width, height);
	const right = getPoint(landmarks, 308, width, height);
	const mouthWidth = distance(left, right) || 1;
	const mouthHeight = distance(upper, lower);
	const ratio = mouthHeight / mouthWidth;
	const normalized = (ratio - 0.02) / 0.25;
	return clamp(normalized, 0, 1);
}

function getFaceBounds(landmarks, width, height) {
	let minX = 1;
	let minY = 1;
	let maxX = 0;
	let maxY = 0;
	for (const p of landmarks) {
		minX = Math.min(minX, p.x);
		minY = Math.min(minY, p.y);
		maxX = Math.max(maxX, p.x);
		maxY = Math.max(maxY, p.y);
	}
	return {
		minX: minX * width,
		minY: minY * height,
		maxX: maxX * width,
		maxY: maxY * height,
	};
}

function drawCowAvatar(ctx, { eyeOpen, mouthOpen, headW, headH }) {

	const faceColor = '#fff6e6';
	const outline = '#1f2937';
	ctx.lineWidth = Math.max(2, headW * 0.04);
	ctx.strokeStyle = outline;
	ctx.fillStyle = faceColor;
	ctx.beginPath();
	ctx.ellipse(0, 0, headW / 2, headH / 2, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.stroke();

	ctx.fillStyle = '#111827';
	ctx.beginPath();
	ctx.ellipse(-headW * 0.18, -headH * 0.1, headW * 0.07, headH * 0.08, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.beginPath();
	ctx.ellipse(headW * 0.2, headH * 0.05, headW * 0.09, headH * 0.08, 0, 0, Math.PI * 2);
	ctx.fill();

	ctx.fillStyle = '#fcd34d';
	ctx.strokeStyle = outline;
	ctx.lineWidth = Math.max(1.5, headW * 0.02);
	ctx.beginPath();
	ctx.ellipse(-headW * 0.26, -headH * 0.55, headW * 0.09, headH * 0.12, -0.4, 0, Math.PI * 2);
	ctx.fill();
	ctx.stroke();
	ctx.beginPath();
	ctx.ellipse(headW * 0.26, -headH * 0.55, headW * 0.09, headH * 0.12, 0.4, 0, Math.PI * 2);
	ctx.fill();
	ctx.stroke();

	ctx.fillStyle = '#111827';
	const eyeY = -headH * 0.1;
	const eyeXOffset = headW * 0.18;
	const eyeW = headW * 0.12;
	const eyeH = headH * 0.12 * Math.max(0.15, eyeOpen);
	ctx.beginPath();
	ctx.ellipse(-eyeXOffset, eyeY, eyeW / 2, eyeH / 2, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.beginPath();
	ctx.ellipse(eyeXOffset, eyeY, eyeW / 2, eyeH / 2, 0, 0, Math.PI * 2);
	ctx.fill();

	ctx.strokeStyle = '#111827';
	ctx.lineWidth = Math.max(2, headW * 0.02);
	ctx.beginPath();
	ctx.ellipse(0, headH * 0.18, headW * 0.16, headH * (0.04 + 0.12 * mouthOpen), 0, 0, Math.PI * 2);
	ctx.stroke();
}

function drawPigAvatar(ctx, { eyeOpen, mouthOpen, headW, headH }) {

	const faceColor = '#ffd7d7';
	const outline = '#d97777';
	ctx.lineWidth = Math.max(2, headW * 0.04);
	ctx.strokeStyle = outline;
	ctx.fillStyle = faceColor;
	ctx.beginPath();
	ctx.ellipse(0, 0, headW / 2, headH / 2, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.stroke();

	ctx.fillStyle = '#ffb3b3';
	ctx.beginPath();
	ctx.ellipse(-headW * 0.32, -headH * 0.35, headW * 0.1, headH * 0.18, -0.6, 0, Math.PI * 2);
	ctx.fill();
	ctx.beginPath();
	ctx.ellipse(headW * 0.32, -headH * 0.35, headW * 0.1, headH * 0.18, 0.6, 0, Math.PI * 2);
	ctx.fill();

	ctx.fillStyle = '#2f2f2f';
	const eyeY = -headH * 0.1;
	const eyeXOffset = headW * 0.18;
	const eyeW = headW * 0.1;
	const eyeH = headH * 0.1 * Math.max(0.15, eyeOpen);
	ctx.beginPath();
	ctx.ellipse(-eyeXOffset, eyeY, eyeW / 2, eyeH / 2, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.beginPath();
	ctx.ellipse(eyeXOffset, eyeY, eyeW / 2, eyeH / 2, 0, 0, Math.PI * 2);
	ctx.fill();

	ctx.fillStyle = '#ff9aaa';
	ctx.strokeStyle = '#d46b7a';
	ctx.lineWidth = Math.max(2, headW * 0.02);
	ctx.beginPath();
	ctx.ellipse(0, headH * 0.16, headW * 0.18, headH * 0.12, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.stroke();
	ctx.fillStyle = '#b45353';
	ctx.beginPath();
	ctx.ellipse(-headW * 0.06, headH * 0.16, headW * 0.035, headH * 0.05, 0, 0, Math.PI * 2);
	ctx.fill();
	ctx.beginPath();
	ctx.ellipse(headW * 0.06, headH * 0.16, headW * 0.035, headH * 0.05, 0, 0, Math.PI * 2);
	ctx.fill();

	ctx.strokeStyle = '#b45353';
	ctx.lineWidth = Math.max(2, headW * 0.02);
	ctx.beginPath();
	ctx.ellipse(0, headH * 0.32, headW * 0.16, headH * (0.03 + 0.1 * mouthOpen), 0, 0, Math.PI * 2);
	ctx.stroke();
}

function drawAvatarOverlay(ctx, landmarks, width, height, type) {
	if (!landmarks || !landmarks.length) {
		console.warn('[Avatar] landmarks 为空，跳过绘制');
		return;
	}
	const bounds = getFaceBounds(landmarks, width, height);

	const faceW = (bounds.maxX - bounds.minX) || 1;
	const faceH = (bounds.maxY - bounds.minY) || 1;
	const headW = faceW * 2.2;
	const headH = faceH * 2.4;


	const leftEye = getPoint(landmarks, 33, width, height);
	const rightEye = getPoint(landmarks, 263, width, height);
	const roll = Math.atan2((rightEye?.y || 0) - (leftEye?.y || 0), (rightEye?.x || 0) - (leftEye?.x || 0));

	const eyeOpenLeft = getEyeOpenRatio(landmarks, width, height, { left: 33, right: 133, top: 159, bottom: 145 });
	const eyeOpenRight = getEyeOpenRatio(landmarks, width, height, { left: 362, right: 263, top: 386, bottom: 374 });
	const eyeOpen = (eyeOpenLeft + eyeOpenRight) / 2;
	const mouthOpen = getMouthOpenRatio(landmarks, width, height);

	const nose = getPoint(landmarks, 1, width, height);
	const centerX = (bounds.minX + bounds.maxX) / 2;
	const centerY = (bounds.minY + bounds.maxY) / 2;
	const headX = centerX + ((nose?.x || centerX) - centerX) * 0.6;
	const headY = centerY + ((nose?.y || centerY) - centerY) * 0.6;

	ctx.save();
	ctx.translate(headX, headY);
	ctx.rotate(roll);
	if (type === 'pig') {
		drawPigAvatar(ctx, { width, height, eyeOpen, mouthOpen, headW, headH });
	} else {
		drawCowAvatar(ctx, { width, height, eyeOpen, mouthOpen, headW, headH });
	}
	ctx.restore();
}

async function createAvatarController({ baseStream, avatarType = 'cow' } = {}) {
	if (!baseStream) return null;
	const baseTrack = baseStream.getVideoTracks?.()[0] || null;
	const trackSettings = baseTrack?.getSettings?.() || {};
	let width = trackSettings.width || 640;
	let height = trackSettings.height || 480;

	let video = null;
	let canvas = null;
	let ctx = null;
	let detectCanvas = null;
	let detectCtx = null;
	let renderStream = null;
	let renderTrack = null;


	if (!baseTrack) {
		throw new Error('未找到可用的视频轨道');
	}
	console.log('[Avatar] 原始轨道状态:', {
		id: baseTrack.id,
		enabled: baseTrack.enabled,
		muted: baseTrack.muted,
		readyState: baseTrack.readyState,
	});


	try {
		video = document.createElement('video');
		video.autoplay = true;
		video.muted = true;
		video.playsInline = true;
		video.style.cssText = 'position:fixed;left:8px;bottom:8px;width:1px;height:1px;opacity:0.01;pointer-events:none;z-index:9999;';
		renderTrack = baseTrack.clone();
		renderStream = new MediaStream([renderTrack]);
		video.srcObject = renderStream;
		document.body.appendChild(video);


		await new Promise((resolve, reject) => {
			let done = false;
			const timeout = setTimeout(() => {
				if (done) return;
				done = true;
				reject(new Error('视频加载超时'));
			}, 5000);
			video.onloadedmetadata = () => {
				if (done) return;
				done = true;
				clearTimeout(timeout);
				resolve();
			};
			video.onerror = (error) => {
				if (done) return;
				done = true;
				clearTimeout(timeout);
				reject(error || new Error('视频元数据加载失败'));
			};
		});

		await video.play().catch((error) => {
			console.error('[Avatar] 视频播放失败:', error?.message || error);
			throw error;
		});

		await new Promise((resolve, reject) => {
			let settled = false;
			const timeout = setTimeout(() => {
				if (settled) return;
				settled = true;
				reject(new Error('视频首帧等待超时'));
			}, 3000);
			const onReady = () => {
				if (settled) return;
				settled = true;
				clearTimeout(timeout);
				resolve();
			};
			if (typeof video.requestVideoFrameCallback === 'function') {
				video.requestVideoFrameCallback(() => onReady());
				return;
			}
			const timer = setInterval(() => {
				if (video.currentTime > 0) {
					clearInterval(timer);
					onReady();
				}
			}, 50);
		});

		width = video.videoWidth || width;
		height = video.videoHeight || height;
		console.log('[Avatar] 最终视频尺寸:', width, height, 'currentTime:', video.currentTime);


		canvas = document.createElement('canvas');
		canvas.width = width;
		canvas.height = height;
		canvas.style.cssText = 'position:fixed;left:-10000px;top:-10000px;width:1px;height:1px;opacity:0;pointer-events:none;';
		document.body.appendChild(canvas);
		ctx = canvas.getContext('2d');
		if (!ctx) {
			throw new Error('Canvas context 不可用');
		}

		detectCanvas = document.createElement('canvas');
		detectCanvas.width = width;
		detectCanvas.height = height;
		detectCanvas.style.cssText = 'position:fixed;left:-10000px;top:-10000px;width:1px;height:1px;opacity:0;pointer-events:none;';
		document.body.appendChild(detectCanvas);
		detectCtx = detectCanvas.getContext('2d');
		if (!detectCtx) {
			throw new Error('Detect canvas context 不可用');
		}



	} catch (error) {
		console.warn('[Avatar] 视频流初始化失败', error?.message || error);
		try {
			renderTrack?.stop?.();
			video?.pause?.();
			if (video) {
				video.srcObject = null;
				video.remove();
			}
			detectCanvas?.remove?.();
			canvas?.remove?.();
		} catch (cleanupError) {
			console.warn('[Avatar] 清理视频失败', cleanupError?.message || cleanupError);
		}

		throw error;

	}

	const { FaceMesh, baseUrl } = await loadFaceMeshLib();
	const locateBase = baseUrl || faceMeshBaseUrl || 'https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh';
	console.log('[Avatar] FaceMesh 资源地址:', locateBase);
	const faceMesh = new FaceMesh({
		locateFile: (file) => {
			// 强制使用非 SIMD 版本，避免部分浏览器/环境报错
			const resolved = file.includes('simd_wasm_bin')
				? file.replace('simd_wasm_bin', 'wasm_bin')
				: file;
			if (resolved !== file) {
				console.warn('[Avatar] 使用非 SIMD 模型:', resolved);
			}
			return `${locateBase}/${resolved}`;
		},
	});

	faceMesh.setOptions({
		maxNumFaces: 1,
		modelComplexity: 0,
		refineLandmarks: true,
		minDetectionConfidence: 0.1,
		minTrackingConfidence: 0.1,
		selfieMode: false,
	});



	let lastLandmarks = null;
	let lastLandmarkTime = 0;
	let loggedLandmarks = false;
	let lastNoLandmarkLogTime = 0;
	let smoothedLandmarks = null;

	faceMesh.onResults((results) => {
		const landmarks = results.multiFaceLandmarks?.[0] || null;
		if (landmarks) {
			lastLandmarks = landmarks;
			lastLandmarkTime = performance.now();
			if (!smoothedLandmarks || smoothedLandmarks.length !== landmarks.length) {
				smoothedLandmarks = landmarks.map((p) => ({ x: p.x, y: p.y, z: p.z }));
			} else {
				const alpha = 0.8;
				for (let i = 0; i < landmarks.length; i += 1) {
					const src = landmarks[i];
					const dst = smoothedLandmarks[i];
					dst.x = dst.x * alpha + src.x * (1 - alpha);
					dst.y = dst.y * alpha + src.y * (1 - alpha);
					dst.z = dst.z * alpha + src.z * (1 - alpha);
				}
			}
			if (!loggedLandmarks) {
				console.log('[Avatar] 检测到人脸 landmarks，数量:', landmarks.length);
				loggedLandmarks = true;
			}
		} else {
			const now = performance.now();
			if (now - lastNoLandmarkLogTime > 2000) {
				console.warn('[Avatar] 当前帧无人脸 landmarks');
				lastNoLandmarkLogTime = now;
			}
		}

	});

	const stream = canvas.captureStream(30);
	let animationId = null;
	let intervalId = null;
	let isProcessing = false;

	let currentAvatarType = avatarType;
	let noLandmarkFrames = 0;
	let loggedNoLandmarks = false;
	let loggedFirstDraw = false;

	const renderFrame = async () => {

		if (video.readyState < 2) {
			return;
		}
		if (video.videoWidth && video.videoHeight && (width !== video.videoWidth || height !== video.videoHeight)) {
			width = video.videoWidth;
			height = video.videoHeight;
			canvas.width = width;
			canvas.height = height;
			if (detectCanvas) {
				detectCanvas.width = width;
				detectCanvas.height = height;
			}
		}


		try {
			ctx.drawImage(video, 0, 0, width, height);
		} catch (error) {
			ctx.fillStyle = '#0f172a';
			ctx.fillRect(0, 0, width, height);
			ctx.fillStyle = '#f87171';
			ctx.font = '18px sans-serif';
			ctx.fillText('Video draw failed', 20, 32);
		}

		if (detectCtx) {
			try {
				detectCtx.drawImage(video, 0, 0, width, height);
			} catch (error) {
				console.warn('[Avatar] Detect draw failed', error?.message || error);
			}
		}

		if (!loggedFirstDraw) {

			try {
				const pixel = ctx.getImageData(0, 0, 1, 1).data;
				console.log('[Avatar] 首帧像素:', Array.from(pixel), 'videoTime:', video.currentTime);
				loggedFirstDraw = true;
			} catch (error) {
				console.warn('[Avatar] 读取像素失败', error?.message || error);
				loggedFirstDraw = true;
			}
		}


		ctx.save();
		ctx.globalAlpha = 0.6;
		ctx.fillStyle = '#22c55e';
		ctx.fillRect(8, 8, 8, 8);
		ctx.restore();

		const now = performance.now();
		const renderLandmarks = smoothedLandmarks || lastLandmarks;
		if (renderLandmarks && now - lastLandmarkTime < 3000) {
			noLandmarkFrames = 0;
			drawAvatarOverlay(ctx, renderLandmarks, width, height, currentAvatarType);
		} else {

			noLandmarkFrames += 1;
			if (!loggedNoLandmarks && noLandmarkFrames > 90) {
				console.warn('[Avatar] 未检测到人脸，请确认 FaceMesh 资源加载或摄像头可用');
				loggedNoLandmarks = true;
			}
		}

		if (isProcessing) return;
		isProcessing = true;
		try {
			await faceMesh.send({ image: detectCanvas || video });
		} catch (error) {

			console.warn('[Avatar] FaceMesh 推理失败', error?.message || error);
			if (!loggedNoLandmarks) {
				console.warn('[Avatar] 可能是 SIMD 不兼容或模型资源受限，已尝试非 SIMD 模型');
				loggedNoLandmarks = true;
			}
		} finally {
			isProcessing = false;
		}

	};

	const useVideoFrameCallback = typeof video.requestVideoFrameCallback === 'function';
	const scheduleNextFrame = () => {
		if (useVideoFrameCallback) {
			video.requestVideoFrameCallback(() => {
				renderFrame();
				scheduleNextFrame();
			});
			return;
		}
		animationId = requestAnimationFrame(() => {
			renderFrame();
			scheduleNextFrame();
		});
	};

	scheduleNextFrame();
	intervalId = setInterval(() => {
		renderFrame();
	}, 33);

	const close = async () => {
		if (animationId) cancelAnimationFrame(animationId);
		if (intervalId) clearInterval(intervalId);
		stream.getTracks().forEach(track => track.stop());

		try {
			faceMesh.close();
		} catch (error) {
			console.warn('[Avatar] FaceMesh 关闭失败', error?.message || error);
		}
		try {
			renderTrack?.stop?.();
			video.pause();
			video.srcObject = null;
			video.remove();
		} catch (error) {
			console.warn('[Avatar] 清理视频元素失败', error?.message || error);
		}

		try {
			canvas?.remove?.();
		} catch (error) {
			console.warn('[Avatar] 清理画布失败', error?.message || error);
		}
		try {
			detectCanvas?.remove?.();
		} catch (error) {
			console.warn('[Avatar] 清理检测画布失败', error?.message || error);
		}

	};

	const setAvatarType = (type) => {
		currentAvatarType = type || 'cow';
	};

	return { stream, video, faceMesh, canvas, stop: close, close, setAvatarType };
}



async function enableVirtualAvatar() {
	if (!session) return false;
	try {
		if (!rawCamStream) {
			rawCamStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
		}
		if (!avatarController) {
			avatarController = await createAvatarController({
				baseStream: rawCamStream,
				avatarType: avatarType.value,
			});
		} else {
			avatarController.setAvatarType?.(avatarType.value);
		}

		const avatarStream = avatarController?.stream;
		const avatarTrack = avatarStream?.getVideoTracks?.()[0];
		if (!avatarTrack) {
			console.warn('[Avatar] 虚拟头像轨道不可用');
			return false;
		}

		console.log('[Avatar] 轨道准备:', {
			camEnabled: camEnabled.value,
			hasCamProducer: !!session?.camProducer,
			avatarTrackId: avatarTrack?.id,
		});
		if (!camEnabled.value) {
			console.log('[Avatar] 摄像头未开启，使用虚拟头像轨道创建视频');
			await session.enableCam({ stream: avatarStream });
			camEnabled.value = true;
			session?.notifyRoomMedia?.('video', true);
		} else if (session?.camProducer) {
			await session.replaceCamTrack?.(avatarTrack);
		}

		updateLocalStream(avatarStream, { keepVideoTrack: true });
		console.log('[Avatar] 虚拟头像已启用');

		return true;
	} catch (error) {
		console.warn('[Avatar] 启用失败', error?.message || error);
		console.warn('[Avatar] FaceMesh 加载失败时可尝试启用本地资源或更换网络');
		return false;
	}
}


async function disableVirtualAvatar({ restoreCam = true } = {}) {

	if (!session) return;
	try {
		if (restoreCam) {
			const camTrack = rawCamStream?.getVideoTracks?.()[0];
			if (camTrack) {
				if (!session.camProducer) {
					await session.enableCam({ stream: rawCamStream });
				} else {
					await session.replaceCamTrack?.(camTrack);
				}
				updateLocalStream(rawCamStream);
				console.log('[Avatar] 已恢复摄像头画面');
			} else {
				removeLocalTrack('video');
				await session.disableCam();
				camEnabled.value = false;
				session?.notifyRoomMedia?.('video', false);
			}
		}
	} finally {
		await avatarController?.close?.();
		avatarController = null;
	}
}

async function toggleVirtualAvatar() {
	if (!session) return;
	virtualAvatarEnabled.value = !virtualAvatarEnabled.value;

	const ensureRawCamTrack = async () => {
		let track = rawCamStream?.getVideoTracks?.()[0] || null;
		if (!track || track.readyState === 'ended') {
			console.warn('[Avatar] 原始轨道不可用，重新获取摄像头');
			const refreshedStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
			rawCamStream = refreshedStream;
			track = refreshedStream.getVideoTracks?.()[0] || null;
		}
		return track;
	};

	if (virtualAvatarEnabled.value) {
		const rawTrack = await ensureRawCamTrack();
		if (!rawTrack) {
			console.warn('[Avatar] 摄像头未启用/无原始流');
			virtualAvatarEnabled.value = false;
			return;
		}
		if (avatarController?.stop) {
			await avatarController.stop();
			avatarController = null;
		}
		avatarController = await createAvatarController({
			baseStream: rawCamStream,
			avatarType: avatarType.value,
		});
		if (!avatarController?.stream) {
			console.warn('[Avatar] 虚拟头像流创建失败');
			virtualAvatarEnabled.value = false;
			return;
		}
		const avatarTrack = avatarController.stream.getVideoTracks()[0];
		console.log('[Avatar] 头像轨道状态:', {
			id: avatarTrack?.id,
			enabled: avatarTrack?.enabled,
			muted: avatarTrack?.muted,
			readyState: avatarTrack?.readyState,
		});

		if (!camEnabled.value || !session.camProducer) {
			await session.enableCam({ stream: avatarController.stream });
			camEnabled.value = true;
			session?.notifyRoomMedia?.('video', true);
		} else {
			await session.replaceCamTrack(avatarTrack);
		}
		updateLocalStream(avatarController.stream, { keepVideoTrack: true });
		console.log('[Avatar] 虚拟头像轨道已替换');
		return;
	}

	if (avatarController?.stop) {
		await avatarController.stop();
		avatarController = null;
	}
	const rawTrack = await ensureRawCamTrack();
	if (!rawTrack) {
		console.warn('[Avatar] 无法恢复原始摄像头轨道');
		return;
	}
	if (!session.camProducer) {
		await session.enableCam({ stream: rawCamStream });
		camEnabled.value = true;
		session?.notifyRoomMedia?.('video', true);
	} else {
		await session.replaceCamTrack(rawTrack);
	}
	updateLocalStream(rawCamStream);
	console.log('[Avatar] 已恢复原始摄像头轨道');
}



function setAvatarType(type) {
	avatarType.value = type || 'cow';
	console.log('[Avatar] 切换形象:', avatarType.value);
	if (virtualAvatarEnabled.value) {
		avatarController?.setAvatarType?.(avatarType.value);
	}
}


function cleanupAvatarController() {

	if (avatarController?.close) {
		avatarController.close();
	}
	avatarController = null;
	virtualAvatarEnabled.value = false;
}

function showScreenShareNotice(message) {
	if (!message) return;
	screenShareNotice.value = message;
	if (screenShareNoticeTimer) {
		clearTimeout(screenShareNoticeTimer);
	}
	screenShareNoticeTimer = setTimeout(() => {
		screenShareNotice.value = '';
		screenShareNoticeTimer = null;
	}, 3000);
}

function handleShareStalled() {
	if (!screenShareActive.value) return;
	if (screenShareOwner.value?.peerId && screenShareOwner.value.peerId === selfPeerId.value) return;
	console.warn('[Share] 共享画面长时间无帧，执行清理');
	clearScreenShareState({ showNotice: true, noticeText: '共享已结束（连接中断）' });
}

function clearScreenShareState(options = {}) {

	const { showNotice = false, noticeText = '共享已结束' } = options;
	console.log('[Share] 清理共享状态');
	screenShareStream.value = null;
	screenShareOwner.value = { peerId: '', displayName: '' };
	screenShareActive.value = false;
	screenShareDisabled.value = false;
	screenShareConsumerId = '';
	screenShareProducerId = '';
	if (showNotice) {
		showScreenShareNotice(noticeText);
	}
}



async function startScreenShare() {
	if (!session) return;
	if (screenShareActive.value && screenShareOwner.value?.peerId && screenShareOwner.value.peerId !== selfPeerId.value) {
		console.warn('[Share] 其他用户正在共享，无法发起共享');
		screenShareDisabled.value = true;
		return;
	}

	try {
		const shareStream = await session.enableScreenShare();
		const track = shareStream?.getVideoTracks?.()[0];
		if (!track) {
			console.warn('[Share] 获取屏幕共享轨道失败');
			return;
		}
		rawScreenShareStream = shareStream;
		track.onended = () => {
			console.log('[Share] 屏幕共享已结束');
			stopScreenShare();
		};
		screenShareStream.value = shareStream;
		screenShareOwner.value = { peerId: selfPeerId.value || '', displayName: `${displayName.value || '我'} (我)` };
		screenShareActive.value = true;
		screenShareDisabled.value = false;
	} catch (error) {
		console.warn('[Share] 屏幕共享启动失败', error?.message || error);
	}
}

async function stopScreenShare({ silent = false } = {}) {
	if (!session) return;
	const shouldNotify = screenShareOwner.value?.peerId && screenShareOwner.value.peerId === selfPeerId.value;
	try {
		await session.disableScreenShare();
		if (rawScreenShareStream) {
			rawScreenShareStream.getTracks().forEach(track => track.stop());
		}
		if (shouldNotify) {
			session.notifyScreenShareStopped?.('user-stop');
		}
	} catch (error) {
		console.warn('[Share] 停止共享失败', error?.message || error);
	} finally {
		rawScreenShareStream = null;
		clearScreenShareState();
		if (!silent) {
			console.log('[Share] 已停止共享');
		}
	}
}


async function toggleScreenShare() {
	if (!session) return;
	if (screenShareActive.value && screenShareOwner.value?.peerId === selfPeerId.value) {
		await stopScreenShare();
		return;
	}
	await startScreenShare();
}

function restartAsr() {

	stopAsr();
	maybeStartAsr();
}



async function handleRegister() {
	isRegistering.value = true;
	const { username, displayName, password } = authForm.value;

	// 用户名校验：必须是手机号（中国大陆手机号，11位数字，以1开头）
	const phoneRegex = /^1[3-9]\d{9}$/;
	if (!phoneRegex.test(username)) {
		statusText.value = '注册失败：用户名必须是有效的手机号';
		isRegistering.value = false;
		return;
	}

	// 显示名校验：不超过5位，且必须是汉字或英文
	if (!displayName || displayName.length > 5) {
		statusText.value = '注册失败：显示名不能超过5位';
		isRegistering.value = false;
		return;
	}
	// 汉字或英文（允许汉字、英文字母，不允许混合数字或特殊字符）
	const displayNameRegex = /^[\u4e00-\u9fa5a-zA-Z]+$/;
	if (!displayNameRegex.test(displayName)) {
		statusText.value = '注册失败：显示名只能是汉字或英文';
		isRegistering.value = false;
		return;
	}

	// 密码校验
	if (!password || password.length < 6) {
		statusText.value = '注册失败：密码至少6位';
		isRegistering.value = false;
		return;
	}

	try {
		const data = await register(authForm.value);
		setAuth(data);
	} catch (error) {
		statusText.value = '注册失败：' + error.message;
	} finally {
		isRegistering.value = false;
	}
}

async function handleLogin() {
	isLoggingIn.value = true;
	try {
		const data = await login(authForm.value);
		setAuth(data);
	} catch (error) {
		statusText.value = '登录失败：' + error.message;
	} finally {
		isLoggingIn.value = false;
	}
}

function setAuth(data) {
	token.value = data.token;
	displayName.value = data.displayName;
	sessionStorage.setItem('token', data.token);
	sessionStorage.setItem('displayName', data.displayName);
	statusText.value = '登录成功！';
	setTimeout(() => { statusText.value = ''; }, 2000);
}

function logout() {
	// 先停止 ASR，再清空引用
	if (asrStreamer) {
		asrStreamer.stop();
		asrStreamer = null;
	}
	if (session) {
		session.close();
	}
	cleanupDenoise();
	cleanupRawMicStream();
	cleanupRawCamStream();
	cleanupAvatarController();
	stopScreenShare({ silent: true });
	localStorage.clear();


	token.value = '';
	displayName.value = '';
	callActive.value = false;
	aiDenoiseEnabled.value = false;
	statusText.value = '';
	micStream.value = null;


	transcriptionLines.value = [];
	activeReactions.value = {};
	reactionTimers.forEach((timer) => clearTimeout(timer));
	reactionTimers.clear();
}


async function handleCreateMeeting() {
	if (!token.value) {
		statusText.value = '请先登录';
		return;
	}
	if (!meetingTitle.value) {
		statusText.value = '请输入会议标题';
		return;
	}
	try {
		const meeting = await createMeeting({ title: meetingTitle.value });
		joinRoomId.value = meeting.roomId;
		statusText.value = `会议已创建，ID: ${meeting.roomId}`;
	} catch (error) {
		statusText.value = '创建会议失败：' + error.message;
	}
}

async function handleJoinMeeting() {
	if (isJoiningMeeting.value || callActive.value) return;
	if (!joinRoomId.value) {
		statusText.value = '请输入会议ID';
		return;
	}
	if (!token.value) {
		statusText.value = '请先登录';
		return;
	}

	isJoiningMeeting.value = true;
	try {
		console.log('[App] 开始加入会议:', joinRoomId.value);
		initAsrStreamer();
		
		session = new MediasoupSession({
			token: token.value,
			displayName: displayName.value,
			onState: updateStatus,
		});

		await session.join(joinRoomId.value);
		currentRoomId.value = joinRoomId.value;
		callActive.value = true;
		statusText.value = '';
		
		// 启动会议时长计时器
		meetingStartTime.value = Date.now();
		startDurationTimer();
		
		console.log('[App] 成功加入会议，准备自动开启麦克风（摄像头默认关闭）');
		
		// 自动开启麦克风（延迟500ms确保传输通道已建立），摄像头保持默认关闭
		setTimeout(async () => {
			try {
				console.log('[App] 自动开启麦克风');
				const micStreamValue = await openMicWithOptionalDenoise();
				if (micStreamValue) {
					updateLocalStream(micStreamValue);
				}
				micEnabled.value = true;
				maybeStartAsr();
				console.log('[App] 麦克风已自动开启，摄像头保持关闭');
			} catch (error) {
				console.error('[App] 自动开启麦克风失败:', error);
				statusText.value = '提示：请手动开启麦克风';
			}
		}, 500);
		
	} catch (error) {
		statusText.value = '加入会议失败：' + error.message;
		console.error('[App] Join meeting error:', error);
		leaveCall({ keepStatusText: true });
	} finally {
		isJoiningMeeting.value = false;
	}

}

function startDurationTimer() {
	if (durationTimer) clearInterval(durationTimer);
	durationTimer = setInterval(() => {
		const elapsed = Math.floor((Date.now() - meetingStartTime.value) / 1000);
		const hours = Math.floor(elapsed / 3600);
		const minutes = Math.floor((elapsed % 3600) / 60);
		const seconds = elapsed % 60;
		meetingDuration.value = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
	}, 1000);
}

async function openMicWithOptionalDenoise() {
	if (!session) return null;
	const stream = await session.enableMic();
	if (!stream) return null;
	rawMicStream = stream;

	if (!aiDenoiseEnabled.value) {
		micStream.value = stream;
		return stream;
	}

	if (!denoiseController) {
		denoiseController = await createDenoiseStream(stream);
	}
	const denoisedStream = denoiseController?.stream;
	const denoisedTrack = denoisedStream?.getAudioTracks?.()[0];
	if (denoisedTrack) {
		await session.replaceMicTrack?.(denoisedTrack);
		micStream.value = denoisedStream;
	} else {
		micStream.value = stream;
	}
	return stream;
}


async function toggleMic() {

	if (!session) return;
	try {
		if (!micEnabled.value) {
			console.log('[App] 开启麦克风');
			const stream = await openMicWithOptionalDenoise();
			if (stream) {
				updateLocalStream(stream);
			}
			micEnabled.value = true;
			maybeStartAsr();
		} else {
			console.log('[App] 关闭麦克风');
			stopAsr();
			await session.disableMic();
			micEnabled.value = false;
			micStream.value = null;
			cleanupDenoise();
			cleanupRawMicStream();
		}
	} catch (error) {

		console.error('[App] 切换麦克风失败:', error);
		statusText.value = '麦克风操作失败：' + error.message;
	}
}

async function toggleCam() {
	if (!session) return;
	try {
		if (!camEnabled.value) {
			console.log('[App] 开启摄像头');
			const stream = await session.enableCam();
			rawCamStream = stream || rawCamStream;
			updateLocalStream(stream);
			session?.notifyRoomMedia?.('video', true);
			camEnabled.value = true;
		} else {
			console.log('[App] 关闭摄像头');
			if (virtualAvatarEnabled.value) {
				await disableVirtualAvatar({ restoreCam: false });
				virtualAvatarEnabled.value = false;
			}
			await session.disableCam();
			session?.notifyRoomMedia?.('video', false);
			removeLocalTrack('video');
			if (rawCamStream) {
				// 仅停止视频轨道，避免误停已合并到本地流里的音频轨道
				rawCamStream.getVideoTracks().forEach(track => track.stop());
				rawCamStream = null;
			}
			camEnabled.value = false;


		}

	} catch (error) {
		console.error('[App] 切换摄像头失败:', error);
		statusText.value = '摄像头操作失败：' + error.message;
	}
}

function updateLocalStream(newStream, options = {}) {
	if (!newStream) return;
	const { keepVideoTrack = false } = options;
	
	// 如果已有本地流，合并新的 track
	if (localStream.value) {
		const existingTracks = localStream.value.getTracks();
		const newTracks = newStream.getTracks();
		
		let hasChanges = false;
		const rawTrack = rawMicStream?.getAudioTracks?.()[0];
		// 收集需要保留的 tracks
		const tracksToKeep = [];
		
		existingTracks.forEach(oldTrack => {
			const sameKindNewTrack = newTracks.find(t => t.kind === oldTrack.kind);
			if (sameKindNewTrack && oldTrack !== sameKindNewTrack) {
				// 有同类型的新 track，移除旧的
				if (oldTrack.kind === 'video' && keepVideoTrack) {
					tracksToKeep.push(oldTrack);
				} else if (!(oldTrack.kind === 'audio' && rawTrack && oldTrack === rawTrack)) {
					oldTrack.stop();
					hasChanges = true;
				} else {
					tracksToKeep.push(oldTrack);
				}
			} else if (!sameKindNewTrack) {
				// 没有同类型的新 track，保留旧的
				tracksToKeep.push(oldTrack);
			} else {
				// 同一个 track，保留
				tracksToKeep.push(oldTrack);
			}
		});
		
		// 添加新的 tracks
		newTracks.forEach(newTrack => {
			if (!tracksToKeep.includes(newTrack)) {
				tracksToKeep.push(newTrack);
				hasChanges = true;
			}
		});
		
		// 【关键修复】创建新的 MediaStream 触发 Vue 响应式更新
		if (hasChanges || newTracks.some(t => t.kind === 'video')) {
			const combinedStream = new MediaStream(tracksToKeep);
			localStream.value = combinedStream;
			console.log('[App] 更新本地流（新实例），tracks:', tracksToKeep.map(t => t.kind));
		}
	} else {
		// 首次创建
		localStream.value = newStream;
		console.log('[App] 创建本地流，tracks:', localStream.value.getTracks().map(t => t.kind));
	}
}


function removeLocalTrack(kind) {
	if (!localStream.value) return;
	const tracks = localStream.value.getTracks();
	tracks.forEach((track) => {
		if (track.kind === kind) {
			localStream.value.removeTrack(track);
			track.stop();
		}
	});
	console.log('[App] 移除本地 track:', kind, '剩余:', localStream.value.getTracks().map(t => t.kind));
}

function resumeRemoteAudio() {
	const audios = document.querySelectorAll('audio');
	audios.forEach((audio) => {
		if (audio.srcObject) {
			audio.play().catch(() => {});
		}
	});
}

function sendReaction(emoji) {
	if (!emoji || !session) return;
	session.sendReaction?.(emoji);
}

function leaveCall(options = {}) {
	const { keepStatusText = false } = options;

	console.log('[App] 离开会议');
	
	// 先停止 ASR，再清空引用
	if (asrStreamer) {
		asrStreamer.stop();
		asrStreamer = null;
	}
	
	cleanupDenoise();
	cleanupRawMicStream();
	cleanupRawCamStream();
	cleanupAvatarController();
	stopScreenShare({ silent: true });
	if (session) {




		session.close();
		session = null;
	}
	if (durationTimer) {
		clearInterval(durationTimer);
		durationTimer = null;
	}
	
	// 停止本地流
	if (localStream.value) {
		localStream.value.getTracks().forEach(track => track.stop());
		localStream.value = null;
	}
	
	callActive.value = false;
	micEnabled.value = false;
	camEnabled.value = false;
	aiDenoiseEnabled.value = false;
	remoteVideos.value = [];

	remoteAudios.value = [];
	meetingParticipants.value = [];
	selfUserId.value = '';
	selfPeerId.value = '';
	mediaStateByPeer.value = {};
	currentRoomId.value = '';
	meetingDuration.value = '00:00:00';
	if (!keepStatusText) {
		statusText.value = '';
	}
	micStream.value = null;

	transcriptionLines.value = [];
	activeReactions.value = {};
	reactionTimers.forEach((timer) => clearTimeout(timer));
	reactionTimers.clear();

	meetingTitle.value = '';
	joinRoomId.value = '';
	
	console.log('[App] 会议状态已重置');
}

// 监听浏览器关闭事件，确保清理资源
function handleBeforeUnload() {
	if (session) {
		session.close();
	}
}

function initAsrStreamer() {
	asrStreamer = new AsrStreamer({
		sendStart: (data) => session?.sendAsrStart?.(data),
		sendAudio: (data) => session?.sendAsrAudio?.(data),
		sendStop: (data) => session?.sendAsrStop?.(data),
		onError: (error) => console.warn('[App] ASR error:', error?.message || error),
	});
}

function maybeStartAsr() {
	console.log(589, asrStreamer, micEnabled.value, micStream.value, currentRoomId.value, selfUserId.value, selfPeerId.value)
	if (!asrStreamer) return;
	if (!micEnabled.value) return;
	if (!micStream.value) return;
	if (!currentRoomId.value) return;
	if (!selfUserId.value && !selfPeerId.value) return;
	asrStreamer.start({
		stream: micStream.value,
		roomId: currentRoomId.value,
		userId: selfUserId.value,
		peerId: selfPeerId.value,
		displayName: displayName.value,
	});
}

function stopAsr() {
	asrStreamer?.stop();
}

onMounted(() => {
	window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
	window.removeEventListener('beforeunload', handleBeforeUnload);
	if (durationTimer) clearInterval(durationTimer);
	reactionTimers.forEach((timer) => clearTimeout(timer));
	reactionTimers.clear();
	if (session) session.close();
});
</script>
