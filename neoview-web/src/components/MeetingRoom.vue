<template>
	<div class="meeting-room">
		<header class="meeting-header">
			<div class="header-left">
				<div class="logo">
					<svg width="24" height="24" viewBox="0 0 32 32" fill="none">
						<rect width="32" height="32" rx="8" fill="#3B82F6"/>
						<path d="M16 8L24 16L16 24L8 16L16 8Z" fill="white"/>
					</svg>
					<span class="logo-text">NeoView</span>
				</div>
				<div class="meeting-info">
					<span class="meeting-id">会议 ID: {{ roomId }}</span>
					<span class="divider">|</span>
					<span class="duration">⏱ {{ meetingDuration }}</span>
					<span class="divider">|</span>
					<span class="participants">👥 {{ participantCount }} 人</span>
				</div>
			</div>
			<!-- <div class="header-right">
				<button class="leave-btn" @click="$emit('leave')">离开会议</button>
			</div> -->
		</header>

		<div class="meeting-content">
			<div class="video-area" :class="{ 'compact-mode': screenShareActive, 'grid-mode': gridLayout.isGridMode }">
				<div v-if="screenShareNotice" class="share-notice">{{ screenShareNotice }}</div>

				<div 
					class="participant-strip" 
					:class="{ 'with-screen-share': screenShareActive }"
					ref="videoGridRef"
					:style="screenShareActive ? {} : gridLayout.style"
				>
					<!-- 屏幕共享区域 -->
					<div v-if="screenShareActive" class="screen-share-stage">
						<div class="participant-tile screen-share-tile">
							<video
								ref="shareVideoElement"
								autoplay
								playsinline
								class="share-video"
							></video>
							<div class="tile-overlay">
								<span class="participant-name">{{ activeShareDisplayName }}  正在共享屏幕</span>
							</div>
						</div>
					</div>

					<!-- 自适应网格布局（无屏幕共享时）- 按行渲染 -->
					<template v-if="!screenShareActive">
						<div class="participant-grid-container" :style="gridLayout.style">
							<div 
								v-for="(row, rowIndex) in gridLayout.rowsData" 
								:key="'row-' + rowIndex"
								class="participant-row"
								:style="gridLayout.rowStyle || {}"
							>
								<template v-for="(participantIndex, colIndex) in row" :key="'tile-' + rowIndex + '-' + colIndex">
									<!-- 空位置占位 - 显示人物轮廓图标 -->
									<div
										v-if="participantIndex === -1"
										class="participant-tile empty-tile"
										:style="{
											width: gridLayout.cardSizes?.[0]?.width + 'px' || '200px',
											height: gridLayout.cardSizes?.[0]?.height + 'px' || '150px'
										}"
									>
										<svg class="empty-person-icon" viewBox="0 0 64 64" fill="currentColor">
											<circle cx="32" cy="20" r="12"/>
											<path d="M12 56c0-11 9-20 20-20s20 9 20 20" stroke="currentColor" stroke-width="3" fill="none"/>
										</svg>
									</div>
									
									<!-- 有效参与者 -->
									<div
										v-else
										class="participant-tile"
										:style="{
											width: gridLayout.cardSizes?.[participantIndex]?.width + 'px',
											height: gridLayout.cardSizes?.[participantIndex]?.height + 'px',
											...tileStyleForDisplay(allParticipantsWithSelf[participantIndex]?.displayName || allParticipantsWithSelf[participantIndex]?.peerId || `参会者${participantIndex + 1}`)
										}"
									>
										<!-- 自己的视频 -->
										<template v-if="allParticipantsWithSelf[participantIndex]?.isSelf">
											<video
												v-show="localHasVideo"
												id="local-video-self"
												ref="localVideoElement"
												autoplay
												playsinline
												muted
												class="video-element"
											></video>
											<div v-if="!localHasVideo" class="avatar-stage">
												<div class="avatar-badge" :style="avatarStyle(displayName)">{{ avatarText(displayName) }}</div>
											</div>
											<div v-if="activeReactionFor(selfPeerId)" class="reaction-float">{{ activeReactionFor(selfPeerId) }}</div>
											<div class="tile-overlay">
												<span class="participant-name">{{ displayName }} (我)</span>
												<div class="video-controls">
													<span v-if="!micEnabled" class="control-icon muted">🎤</span>
													<span v-if="!camEnabled" class="control-icon">📹</span>
												</div>
											</div>
										</template>
										
										<!-- 远端参与者的视频 -->
										<template v-else>
											<video
												v-show="participantHasVideo(allParticipantsWithSelf[participantIndex]?.peerId)"
												:id="'remote-video-' + allParticipantsWithSelf[participantIndex]?.peerId"
												autoplay
												playsinline
												class="video-element"
											></video>
											<div v-if="!participantHasVideo(allParticipantsWithSelf[participantIndex]?.peerId)" class="avatar-stage">
												<div class="avatar-badge" :style="avatarStyle(allParticipantsWithSelf[participantIndex]?.displayName || allParticipantsWithSelf[participantIndex]?.peerId)">
													{{ avatarText(allParticipantsWithSelf[participantIndex]?.displayName || allParticipantsWithSelf[participantIndex]?.peerId || `参会者${participantIndex + 1}`) }}
												</div>
											</div>
											<div v-if="activeReactionFor(allParticipantsWithSelf[participantIndex]?.peerId)" class="reaction-float">{{ activeReactionFor(allParticipantsWithSelf[participantIndex]?.peerId) }}</div>
											<div class="tile-overlay">
												<span class="participant-name">{{ allParticipantsWithSelf[participantIndex]?.displayName || allParticipantsWithSelf[participantIndex]?.peerId || `参会者${participantIndex + 1}` }}</span>
												<span v-if="remoteVideoStateMap.get(allParticipantsWithSelf[participantIndex]?.peerId)?.paused" class="paused-badge">已停止视频</span>
											</div>
										</template>
									</div>
								</template>
							</div>
						</div>
					</template>

					<!-- 屏幕共享时的参与者列表 -->
					<div v-if="screenShareActive" class="participant-list">
					<div class="participant-tile" :style="tileStyleForDisplay(displayName)">
						<video
							v-show="localHasVideo"
							id="local-video-self"
							autoplay
							playsinline
							muted
							class="video-element"
						></video>
							<div v-if="!localHasVideo" class="avatar-stage">
								<div class="avatar-badge" :style="avatarStyle(displayName)">{{ avatarText(displayName) }}</div>
							</div>
							<div v-if="activeReactionFor(selfPeerId)" class="reaction-float">{{ activeReactionFor(selfPeerId) }}</div>
							<div class="tile-overlay">
								<span class="participant-name">{{ displayName }} (我)</span>
								<div class="video-controls">
									<span v-if="!micEnabled" class="control-icon muted">🎤</span>
									<span v-if="!camEnabled" class="control-icon">📹</span>
								</div>
							</div>
						</div>

						<div
							v-for="(participant, index) in orderedRemoteParticipants"
							:key="participant.peerId"
							class="participant-tile"
							:style="tileStyleForDisplay(participant.displayName || participant.peerId || `参会者${index + 1}`)"
						>
							<video
								v-show="participantHasVideo(participant.peerId)"
								:id="'remote-video-' + participant.peerId"
								autoplay
								playsinline
								class="video-element"
							></video>
							<div v-if="!participantHasVideo(participant.peerId)" class="avatar-stage">
								<div class="avatar-badge" :style="avatarStyle(participant.displayName || participant.peerId)">
									{{ avatarText(participant.displayName || participant.peerId || `参会者${index + 1}`) }}
								</div>
							</div>
							<div v-if="activeReactionFor(participant.peerId)" class="reaction-float">{{ activeReactionFor(participant.peerId) }}</div>
							<div class="tile-overlay">
								<span class="participant-name">{{ participant.displayName || participant.peerId || `参会者${index + 1}` }}</span>
								<span v-if="remoteVideoStateMap.get(participant.peerId)?.paused" class="paused-badge">已停止视频</span>
							</div>
						</div>
					</div>
				</div>

				<div class="audio-list">
					<audio
						v-for="remote in remoteAudios"
						:key="remote.consumerId"
						:id="'remote-audio-' + remote.consumerId"
						autoplay
					></audio>
				</div>


			</div>

			<div class="transcription-entry-row" :title="showTranscriptionPanel ? '关闭转写文字' : '打开转写文字'">
				<button class="transcription-toggle-btn" @click="toggleTranscriptionPanel">
					<svg v-if="showTranscriptionPanel" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
						<rect x="3" y="3" width="18" height="18" rx="2"/>
						<path d="M9 9l6 6M15 9l-6 6"/>
					</svg>
					<svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
						<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
						<line x1="9" y1="10" x2="15" y2="10"/>
						<line x1="9" y1="14" x2="13" y2="14"/>
					</svg>
				</button>
			</div>

			<div v-show="showTranscriptionPanel" class="transcription-bottom-panel" ref="transcriptionDisplayRef">
				<div class="transcription-panel-header">
					<span class="panel-title">语音转写</span>
					<button class="mini-btn" @click="clearTranscription">清空转写</button>
				</div>
				<div v-if="!visibleTranscriptionLines.length" class="placeholder-text">暂无转写数据</div>
				<div v-else class="transcription-list">
					<div v-for="line in visibleTranscriptionLines" :key="line.id" class="transcription-line">
						<span class="transcription-name">{{ line.displayName }}：</span>
						<span class="transcription-text">{{ line.text }}</span>
					</div>
				</div>
			</div>

			<aside v-show="false" class="minutes-hidden-structure">
				<div class="minutes-placeholder">
					<div class="placeholder-text">基于本次会议语音内容，智能生成结构化会议纪要（功能预留）</div>
					<button class="minutes-btn" disabled>生成会议纪要</button>
				</div>
			</aside>
		</div>

		<footer class="meeting-footer">
			<div class="controls-left">
				<button class="control-btn mic-btn" :class="{ active: micEnabled, danger: !micEnabled }" @click="$emit('toggle-mic')"
					:title="micEnabled ? '关闭麦克风' : '打开麦克风'"
				>
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-mic_on" v-if="micEnabled"></i>
						<i class="iconfont icon-mic_off" v-else></i>
					</span>
					<!-- <span class="btn-text">{{ micEnabled ? '麦克风开' : '麦克风关' }}</span> -->
				</button>
				<button class="control-btn cam-btn" :class="{ active: camEnabled, danger: !camEnabled }" @click="$emit('toggle-cam')"
					:title="camEnabled ? '关闭摄像头' : '打开摄像头'"
				>
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-shexiangtou" v-if="camEnabled"></i>
						<i class="iconfont icon-guanbishexiangtou" v-else></i>
					</span>
					<!-- <span class="btn-text">{{ camEnabled ? '摄像头开' : '摄像头关' }}</span> -->
				</button>
			</div>

			<div class="controls-center">
				<button class="control-btn utility-btn" :class="{ active: aiDenoiseEnabled }" @click="$emit('toggle-ai-denoise')"
					title="AI降噪"
				>
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-a-10jiangzao"></i>
					</span>
					<!-- <span class="btn-text">AI降噪</span> -->
				</button>
				<button class="control-btn utility-btn" :class="{ active: virtualAvatarEnabled }" @click="$emit('toggle-virtual-avatar')"
					title="虚拟头像"
				>
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-touxiang"></i>
					</span>
					<!-- <span class="btn-text">虚拟头像</span> -->
				</button>
				<button
					class="control-btn secondary"
					:class="{ active: screenShareActive && !screenShareDisabled }"
					:disabled="screenShareDisabled"
					@click="$emit('toggle-screen-share')"
					:title="screenShareActive ? (screenShareDisabled ? '共享中' : '停止共享') : '共享屏幕'"
				>
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-w_pingmu"></i>
					</span>
					<!-- <span class="btn-text">{{ screenShareActive ? (screenShareDisabled ? '共享中' : '停止共享') : '共享屏幕' }}</span> -->
				</button>
				<div class="reaction-wrap">
					<button class="control-btn utility-btn" :class="{ active: showReactionPicker }" @click="toggleReactionPicker"
						title="表情"
					>
						<span class="btn-icon" aria-hidden="true">
							<i class="iconfont icon-biaoqing-xue"></i>
						</span>
						<!-- <span class="btn-text">表情</span> -->
					</button>
					<div v-if="showReactionPicker" class="reaction-picker">
						<button v-for="emoji in reactionOptions" :key="emoji" class="reaction-option" @click="pickReaction(emoji)">{{ emoji }}</button>
					</div>
				</div>
			</div>

			<div class="controls-right">
				<!-- <button class="control-btn more" @click="$emit('resume-audio')">
					<span class="btn-icon" aria-hidden="true">
						<svg viewBox="0 0 24 24" class="btn-svg">
							<path d="M4 14v-4h4l5-4v12l-5-4z"></path>
							<path d="M16 9.5a4 4 0 0 1 0 5"></path>
						</svg>
					</span>
					<span class="btn-text">恢复音频</span>
				</button>-->
				<button class="control-btn leave-call-btn" @click="$emit('leave')" title="挂断">
					<span class="btn-icon" aria-hidden="true">
						<i class="iconfont icon-14guaduan-1"></i>
					</span>
					<!-- <span class="btn-text">挂断</span> -->
				</button>
			</div>
		</footer>
	</div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { calculateGridLayout } from '../utils/layoutCalculator.js';

const props = defineProps({
	roomId: { type: String, required: true },
	displayName: { type: String, required: true },
	localStream: { type: Object, default: null },
	remoteVideos: { type: Array, default: () => [] },
	remoteAudios: { type: Array, default: () => [] },
	participants: { type: Array, default: () => [] },
	selfPeerId: { type: String, default: '' },
	micEnabled: { type: Boolean, default: false },
	camEnabled: { type: Boolean, default: false },
	aiDenoiseEnabled: { type: Boolean, default: false },
	virtualAvatarEnabled: { type: Boolean, default: false },
	avatarType: { type: String, default: 'cow' },
	participantCount: { type: Number, default: 1 },
	screenShareStream: { type: Object, default: null },
	screenShareOwner: { type: Object, default: () => ({}) },
	screenShareActive: { type: Boolean, default: false },
	screenShareDisabled: { type: Boolean, default: false },
	screenShareNotice: { type: String, default: '' },
	meetingDuration: { type: String, default: '00:00:00' },
	transcriptionLines: { type: Array, default: () => [] },
	activeReactions: { type: Object, default: () => ({}) },
});

const emit = defineEmits(['toggle-mic', 'toggle-cam', 'toggle-ai-denoise', 'toggle-virtual-avatar', 'set-avatar-type', 'toggle-screen-share', 'resume-audio', 'share-stalled', 'leave', 'clear-transcription', 'send-reaction']);

const activeShareDisplayName = ref('');        //当前正在共享屏幕的人员
const localVideoElement = ref(null);
const shareVideoElement = ref(null);
const transcriptionDisplayRef = ref(null);
const showTranscriptionPanel = ref(false);
const visibleTranscriptionLines = ref([]);
const showReactionPicker = ref(false);
const reactionOptions = ['👍', '👏', '🎉', '😂', '❤️', '🔥', '😮', '🙏', '✅', '🙌', '🤔', '🥳', '💯', '💪', '👀', '⭐', '😎', '😁', '😢', '😡', '🤝', '👌', '🎯', '🚀'];
let shareMonitorTimer = null;
let lastShareTime = 0;
let shareStallCount = 0;

// 自适应网格布局相关状态
const videoGridRef = ref(null);
const gridLayout = ref({ cols: 1, rows: 1, rowConfig: [1], style: {} });
const containerSize = ref({ width: 0, height: 0 });
let resizeObserver = null;

const allRemoteParticipants = computed(() => {
	const selfPeer = props.selfPeerId;
	return props.participants.filter((p) => {
		if (!p || !p.peerId) return false;
		return selfPeer ? p.peerId !== selfPeer : true;
	});
});

const orderedRemoteParticipants = computed(() => allRemoteParticipants.value);

const visibleParticipantCount = computed(() => Math.max(1, 1 + orderedRemoteParticipants.value.length));

// 所有参与者列表（包含自己）
const allParticipantsWithSelf = computed(() => {
	const self = {
		peerId: props.selfPeerId || 'self',
		displayName: props.displayName,
		isSelf: true,
	};
	return [self, ...orderedRemoteParticipants.value];
});

// ResizeObserver 相关函数
function setupResizeObserver() {
	if (!videoGridRef.value) return;
	
	// 清理旧的 observer
	if (resizeObserver) {
		resizeObserver.disconnect();
	}

	resizeObserver = new ResizeObserver((entries) => {
		for (const entry of entries) {
			const { width, height } = entry.contentRect;
			containerSize.value = { width, height };
			
			// 重新计算布局
			updateGridLayout();
		}
	});

	resizeObserver.observe(videoGridRef.value);
}

function updateGridLayout() {
	const { width, height } = containerSize.value;
	if (width > 0 && height > 0) {
		gridLayout.value = calculateGridLayout(
			visibleParticipantCount.value,
			width,
			height
		);
	}
}

const remoteVideoStateMap = computed(() => {
	const map = new Map();
	props.remoteVideos.forEach((v) => {
		if (v.peerId) {
			map.set(v.peerId, { paused: !!v.paused, stream: v.stream });
		}
	});
	return map;
});

const localHasVideo = computed(() => {
	if (!props.camEnabled || !props.localStream) return false;
	const tracks = props.localStream.getVideoTracks?.() || [];
	return tracks.some((track) => track.readyState === 'live');
});

function hasLiveVideoTrack(stream) {
	const tracks = stream?.getVideoTracks?.() || [];
	return tracks.some((track) => track.readyState === 'live' && track.enabled !== false && track.muted !== true);
}

function participantHasVideo(peerId) {
	const state = remoteVideoStateMap.value.get(peerId);
	return !!(state && !state.paused && hasLiveVideoTrack(state.stream));
}

function normalizeName(name) {
	return String(name || '').trim();
}

function avatarText(name) {
	const normalized = normalizeName(name);
	if (!normalized) return '会议';
	const zhChars = normalized.match(/[\u4e00-\u9fa5]/gu);
	if (zhChars?.length) return zhChars.slice(0, 2).join('');
	const compact = normalized.replace(/\s+/g, '');
	const digits = compact.replace(/\D/g, '');
	if (digits) return digits.slice(0, 2);
	const enChars = compact.match(/[A-Za-z]/g);
	if (enChars?.length) return enChars.slice(0, 2).join('').toUpperCase();
	return compact.slice(0, 2).toUpperCase();
}

function hashHue(name) {
	const normalized = normalizeName(name) || 'NeoView';
	let hash = 0;
	for (let i = 0; i < normalized.length; i += 1) {
		hash = (hash * 31 + normalized.charCodeAt(i)) % 360;
	}
	return Math.abs(hash);
}

function tileStyleForDisplay(name) {
	const hue = hashHue(name);
	return {
		background: `radial-gradient(circle at 28% 20%, hsla(${hue}, 58%, 54%, 0.22), hsla(${hue}, 40%, 26%, 0.96) 70%), linear-gradient(160deg, hsl(${hue}, 36%, 30%), hsl(${hue}, 32%, 24%))`,
	};
}

function avatarStyle(name) {
	const hue = hashHue(name);
	return {
		background: `hsl(${hue}, 72%, 52%)`,
	};
}

function activeReactionFor(peerId) {
	if (!peerId) return '';
	return props.activeReactions?.[peerId] || '';
}

function toggleReactionPicker() {
	showReactionPicker.value = !showReactionPicker.value;
}

function pickReaction(emoji) {
	if (!emoji) return;
	emit('send-reaction', emoji);
	showReactionPicker.value = false;
}

function clearTranscription() {
	visibleTranscriptionLines.value = [];
	emit('clear-transcription');
}

function toggleTranscriptionPanel() {
	if (showTranscriptionPanel.value) {
		showTranscriptionPanel.value = false;
		visibleTranscriptionLines.value = [];
		return;
	}
	showTranscriptionPanel.value = true;
	visibleTranscriptionLines.value = (props.transcriptionLines || []).slice();
}

function getLocalVideoEl() {
	// 优先通过 id 查找（最可靠），兼容 ref 为数组或单元素两种情况
	const byId = document.getElementById('local-video-self');
	if (byId) return byId;
	const refVal = localVideoElement.value;
	if (!refVal) return null;
	// Vue 3 中 ref 在 v-for 内部会收集为数组
	if (Array.isArray(refVal)) return refVal[0] || null;
	return refVal;
}

function getAllLocalVideoEls() {
	// 屏幕共享模式下可能同时存在两个同 id 的元素，全部绑定
	const els = document.querySelectorAll('#local-video-self');
	if (els.length > 0) return Array.from(els);
	const refVal = localVideoElement.value;
	if (!refVal) return [];
	if (Array.isArray(refVal)) return refVal.filter(Boolean);
	return [refVal];
}

function bindLocalStream() {
	const els = getAllLocalVideoEls();
	if (!els.length) return;
	els.forEach((el) => {
		if (localHasVideo.value && props.localStream) {
			if (el.srcObject !== props.localStream) {
				el.srcObject = props.localStream;
				el.play?.().catch(() => {});
			}
		} else {
			el.srcObject = null;
		}
	});
}

function bindShareStream() {
	if (!shareVideoElement.value) return;
	if (props.screenShareStream) {
		shareVideoElement.value.srcObject = props.screenShareStream;
	} else {
		shareVideoElement.value.srcObject = null;
		shareVideoElement.value.load?.();
	}
}

function stopShareMonitor() {
	if (shareMonitorTimer) {
		clearInterval(shareMonitorTimer);
		shareMonitorTimer = null;
	}
	lastShareTime = 0;
	shareStallCount = 0;
}

function startShareMonitor() {
	stopShareMonitor();
	if (!props.screenShareActive || !shareVideoElement.value) return;
	const video = shareVideoElement.value;
	lastShareTime = video.currentTime || 0;
	shareStallCount = 0;
	shareMonitorTimer = setInterval(() => {
		if (!props.screenShareActive || !shareVideoElement.value) {
			stopShareMonitor();
			return;
		}
		const current = video.currentTime || 0;
		if (current > lastShareTime + 0.01) {
			lastShareTime = current;
			shareStallCount = 0;
			return;
		}
		shareStallCount += 1;
		if (shareStallCount >= 3) {
			stopShareMonitor();
			emit('share-stalled');
		}
	}, 1000);
}

async function bindRemoteVideos() {
	await nextTick();
	orderedRemoteParticipants.value.forEach((participant) => {
		const videoElement = document.getElementById('remote-video-' + participant.peerId);
		const state = remoteVideoStateMap.value.get(participant.peerId);
		if (!videoElement) return;
		const canRenderVideo = !!(state && !state.paused && hasLiveVideoTrack(state.stream));
		if (canRenderVideo && videoElement.srcObject !== state.stream) {
			videoElement.srcObject = state.stream;
		}
		if (!canRenderVideo) {
			videoElement.srcObject = null;
		}
	});
}

watch([() => props.localStream, () => props.camEnabled], async () => {
	await nextTick();
	bindLocalStream();
}, { immediate: true });

watch([() => props.remoteVideos, () => orderedRemoteParticipants.value], async () => {
	await bindRemoteVideos();
}, { deep: true, immediate: true });

watch(() => props.screenShareStream, async () => {
	await nextTick();
	bindShareStream();
	startShareMonitor();
}, { immediate: true });

watch(() => props.screenShareActive, async (active) => {
	if (!active) {
		stopShareMonitor();
	} else {
		startShareMonitor();
	}
	// 屏幕共享切换时 DOM 布局重新渲染，需要重新绑定本地视频
	await nextTick();
	bindLocalStream();
});

watch(() => props.remoteAudios, async (newAudios) => {
	await nextTick();
	newAudios.forEach((remote) => {
		const audioElement = document.getElementById('remote-audio-' + remote.consumerId);
		if (!audioElement || !remote.stream) return;
		if (audioElement.srcObject !== remote.stream) {
			audioElement.srcObject = remote.stream;
			audioElement.play().catch(() => {});
		}
	});
}, { deep: true, immediate: true });

watch(
	() => props.transcriptionLines,
	async () => {
		if (!showTranscriptionPanel.value) return;
		visibleTranscriptionLines.value = (props.transcriptionLines || []).slice();
		await nextTick();
		if (!transcriptionDisplayRef.value || !visibleTranscriptionLines.value.length) return;
		const container = transcriptionDisplayRef.value;
		container.scrollTop = container.scrollHeight - container.clientHeight;
	},
	{ deep: true, immediate: true }
);

watch(showTranscriptionPanel, async (show) => {
	if (!show) {
		visibleTranscriptionLines.value = [];
		return;
	}
	visibleTranscriptionLines.value = (props.transcriptionLines || []).slice();
	await nextTick();
	if (!transcriptionDisplayRef.value) return;
	const container = transcriptionDisplayRef.value;
	container.scrollTop = container.scrollHeight - container.clientHeight;
});

watch(() => props.screenShareActive, (show) => {
	if (show) {
		const index = orderedRemoteParticipants.value.findIndex((item) => item.peerId === props.screenShareOwner?.peerId);
		activeShareDisplayName.value = (index > -1 ? orderedRemoteParticipants.value[index].displayName : props.screenShareOwner?.displayName);
	}

}, { immediate: true , deep: true})

// 监听参与者数量变化，更新布局
watch(visibleParticipantCount, () => {
	updateGridLayout();
});

onMounted(() => {
	setupResizeObserver();
});

onBeforeUnmount(() => {
	stopShareMonitor();
	if (resizeObserver) {
		resizeObserver.disconnect();
		resizeObserver = null;
	}
});
</script>

<style scoped>
@import "../fonts/iconfont.css";
.meeting-room {
	display: flex;
	flex-direction: column;
	height: 100vh;
	background: #202124;
	color: #e8eaed;
	overflow: hidden;
}

.meeting-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 10px 20px;
	background: #202124;
	border-bottom: 1px solid rgba(232, 234, 237, 0.12);
	min-height: 56px;
	flex-shrink: 0;
}


.header-left {
	display: flex;
	align-items: center;
	gap: 20px;
}

.logo {
	display: flex;
	align-items: center;
	gap: 8px;
}

.logo-text {
	font-size: 18px;
	font-weight: 700;
	color: #3B82F6;
}

.meeting-info {
	display: flex;
	align-items: center;
	gap: 10px;
	font-size: 13px;
	color: #9ca3af;
}

.divider {
	color: #4b5563;
}

.leave-btn {
	padding: 8px 14px;
	border-radius: 999px;
	background: transparent;
	border: 1px solid rgba(232, 234, 237, 0.28);
	color: #e8eaed;
	font-weight: 600;
	cursor: pointer;
	transition: background-color 0.18s ease, border-color 0.18s ease;
}

.leave-btn:hover {
	background: rgba(232, 234, 237, 0.1);
	border-color: rgba(232, 234, 237, 0.44);
}


.meeting-content {
	display: flex;
	flex-direction: column;
	flex: 1;
	min-height: 0;
	position: relative;
}

.video-area {
	flex: 1;
	min-height: 0;
	padding: 20px;
	/*padding-bottom: 124px; */
	overflow: auto;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
}

/* 九宫格模式 - 无间隙 */
.video-area.grid-mode {
	padding: 0;
}

.video-area.compact-mode {
	padding: 10px;
	/* padding-bottom: 96px; */
}


.share-notice {
	position: absolute;
	top: 12px;
	left: 50%;
	transform: translateX(-50%);
	margin: 0;
	width: fit-content;
	background: rgba(32, 33, 36, 0.9);
	color: #e5e7eb;
	padding: 8px 14px;
	border-radius: 999px;
	font-size: 12px;
	border: 1px solid rgba(232, 234, 237, 0.24);
	z-index: 5;
}

.participant-strip {
	display: flex;
	flex-direction: column;
	width: 100%;
	height: 100%;
	min-height: 0;
	margin: 0 auto;
	gap: 16px;
}

/* 自适应网格容器 */
.participant-grid-container {
	width: 100%;
	height: 100%;
}

/* 每行的容器 */
.participant-row {
	display: flex;
	justify-content: center;
	align-items: center;
}

.participant-list {
	display: contents;
}

/* 屏幕共享时的布局 */
.participant-strip.with-screen-share {
	display: grid;
	grid-template-columns: minmax(0, 1fr) clamp(180px, 19vw, 280px);
	gap: 10px;
	width: min(1720px, 100%);
	height: 100%;
	min-height: 0;
	align-items: stretch;
	padding: 0;
}

.screen-share-stage {
	min-width: 0;
	min-height: 0;
	height: 100%;
}

.participant-strip.with-screen-share .participant-list {
	display: grid;
	grid-template-columns: 1fr;
	grid-auto-rows: clamp(104px, 16vh, 146px);
	gap: 8px;
	overflow-y: auto;
	padding: 1px 4px 1px 1px;
	align-content: start;
	min-height: 0;
}

.participant-tile {
	position: relative;
	min-width: 0;
	min-height: 0;
	border-radius: 16px;
	overflow: hidden;
	border: 1px solid rgba(255, 255, 255, 0.12);
	background: #2d3a44;
	box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
	transition: transform 0.2s ease, box-shadow 0.2s ease;
	flex-shrink: 0;
}

/* 九宫格空位占位 - 人物轮廓图标 */
.participant-tile.empty-tile {
	background: #2d3a44;
	border: 1px solid rgba(255, 255, 255, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
}

.participant-tile.empty-tile .empty-person-icon {
	width: 40%;
	height: 40%;
	opacity: 0.3;
	color: rgba(255, 255, 255, 0.5);
}

/* 屏幕共享时的参与者卡片样式 */
.participant-strip.with-screen-share .participant-tile:not(.screen-share-tile) {
	width: 100%;
	height: 100%;
	min-width: 0;
	max-width: none;
	aspect-ratio: auto;
}

.participant-strip.with-screen-share .participant-tile:not(.screen-share-tile) .avatar-badge {
	width: clamp(48px, 34%, 62px);
	font-size: clamp(16px, 1.7vw, 22px);
}

.participant-strip.with-screen-share .participant-list .tile-overlay {
	padding: 8px;
}

.participant-strip.with-screen-share .participant-list .participant-name {
	font-size: 12px;
	padding: 3px 8px;
}

.participant-tile:hover {
	transform: translateY(-1px);
	box-shadow: 0 10px 28px rgba(0, 0, 0, 0.34);
}

.screen-share-tile {
	width: 100%;
	height: 100%;
	aspect-ratio: auto;
	min-height: 0;
	max-width: none;
	max-height: none;
	background: #111827;
}

.video-element,
.share-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.share-video {
	object-fit: contain;
	object-position: center center;
	background: #0b1220;
}

.avatar-stage {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
}

.avatar-badge {
	width: clamp(66px, 26%, 94px);
	height: auto;
	aspect-ratio: 1 / 1;
	border-radius: 50%;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	color: #fff;
	font-size: clamp(20px, 2.2vw, 30px);
	font-weight: 700;
	line-height: 1;
	letter-spacing: 0.015em;
	font-family: 'Google Sans', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
	box-shadow: 0 8px 20px rgba(0, 0, 0, 0.28);
	text-align: center;
}

.tile-overlay {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 12px;
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 8px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.06));
}

.participant-name {
	font-size: 13px;
	font-weight: 600;
	color: #fff;
	text-shadow: 0 1px 2px rgba(0, 0, 0, 0.42);
	padding: 4px 10px;
	border-radius: 999px;
	background: rgba(17, 24, 39, 0.5);
	backdrop-filter: blur(4px);
}


.video-controls {
	display: flex;
	gap: 6px;
}

.control-icon.muted {
	color: #f87171;
}

.paused-badge {
	font-size: 12px;
	color: #f59e0b;
	background: rgba(245, 158, 11, 0.15);
	border: 1px solid rgba(245, 158, 11, 0.4);
	padding: 2px 8px;
	border-radius: 999px;
}

.transcription-entry-row {
	position: absolute;
	left: 50%;
	bottom: -14px;
	transform: translateX(-50%);
	display: flex;
	justify-content: center;
	z-index: 7;
}

.transcription-toggle-btn {
	width: 40px;
	height: 40px;
	/* padding: 0 18px; */
	border-radius: 50%;
	border: 1px solid rgba(93, 108, 132, 0.7);
	background: rgba(26, 32, 44, 0.86);
	color: #e7eefc;
	cursor: pointer;
	font-size: 14px;
	font-weight: 600;
	backdrop-filter: blur(8px);
	box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}

.transcription-toggle-btn:hover {
	background: rgba(44, 58, 82, 0.92);
}

.transcription-toggle-btn.active {
	border-color: rgba(59, 130, 246, 0.85);
	background: rgba(30, 64, 175, 0.9);
}

.transcription-bottom-panel {
	position: absolute;
	left: 50%;
	bottom: 38px;
	transform: translateX(-50%);
	width: min(860px, calc(100% - 40px));
	padding: 0;
	border: 1px solid rgba(59, 130, 246, 0.35);
	background: rgba(14, 20, 31, 0.86);
	backdrop-filter: blur(10px);
	border-radius: 16px;
	max-height: min(24vh, 210px);
	overflow-y: auto;
	overflow-x: hidden;
	box-shadow: 0 14px 36px rgba(0, 0, 0, 0.4);
	z-index: 6;
}


.transcription-panel-header {
	position: sticky;
	top: 0;
	z-index: 2;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 10px 12px 8px;
	background: rgba(14, 20, 31, 0.96);
	border-bottom: 1px solid rgba(59, 130, 246, 0.2);
}

.panel-title {
	font-size: 13px;
	font-weight: 700;
	color: #c7d2fe;
}

.transcription-list {
	display: flex;
	flex-direction: column;
	gap: 6px;
	padding: 8px 12px 10px;
}

.transcription-line {
	font-size: 12px;
	line-height: 1.35;
	color: #dbe5ff;
}

.transcription-name {
	color: #8fb3ff;
	font-weight: 700;
}

.placeholder-text {
	color: #8ea0c3;
	font-size: 12px;
	padding: 10px 12px 12px;
}

.minutes-hidden-structure {
	display: none;
}

.minutes-placeholder {
	min-height: 140px;
	background: #121b2d;
	border: 1px dashed #32466b;
	border-radius: 8px;
	padding: 12px;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	gap: 10px;
}

.minutes-btn {
	padding: 8px 12px;
	border-radius: 6px;
	border: none;
	background: #2563eb;
	color: #fff;
	font-size: 12px;
	font-weight: 600;
	opacity: 0.6;
	cursor: not-allowed;
}

.audio-list {
	display: none;
}

.reaction-float {
	position: absolute;
	top: 12px;
	right: 12px;
	font-size: 28px;
	line-height: 1;
	padding: 6px;
	border-radius: 12px;
	background: rgba(32, 33, 36, 0.38);
	backdrop-filter: blur(4px);
	animation: reaction-pop 1.8s ease forwards;
	z-index: 4;
}

@keyframes reaction-pop {
	0% {
		transform: translateY(10px) scale(0.7);
		opacity: 0;
	}
	20% {
		transform: translateY(0) scale(1);
		opacity: 1;
	}
	80% {
		opacity: 1;
	}
	100% {
		transform: translateY(-10px) scale(1);
		opacity: 0;
	}
}

.reaction-wrap {
	position: relative;
}

.reaction-picker {
	position: absolute;
	bottom: 66px;
	left: 50%;
	transform: translateX(-50%);
	display: grid;
	grid-template-columns: repeat(6, minmax(0, 1fr));
	gap: 8px;
	width: 288px;
	max-height: 220px;
	overflow-y: auto;
	padding: 10px;
	border-radius: 12px;
	background: rgba(32, 33, 36, 0.95);
	border: 1px solid rgba(232, 234, 237, 0.2);
	box-shadow: 0 10px 26px rgba(0, 0, 0, 0.35);
	z-index: 8;
}

.reaction-option {
	width: 100%;
	height: 36px;
	border: none;
	border-radius: 10px;
	background: #3c4043;
	font-size: 20px;
	cursor: pointer;
}

.reaction-option:hover {
	background: #4a4d51;
}

.meeting-footer {
	display: flex;
	justify-content: center;
	align-items: center;
	gap: 10px;
	padding: 12px 22px;
	background: #202124;
	border-top: 1px solid rgba(232, 234, 237, 0.12);
	min-height: 84px;
	flex-shrink: 0;
}

.controls-left,
.controls-center,
.controls-right {
	display: flex;
	gap: 6px;
	align-items: center;
}

.control-btn {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 4px;
	height: 50px;
	padding: 0 6px;
	background: #3c4043;
	border: 1px solid rgba(232, 234, 237, 0.14);
	border-radius: 16px;
	color: #e8eaed;
	cursor: pointer;
	min-width: 50px;
	transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s cubic-bezier(0.2, 0, 0, 1), transform 0.16s ease;
}

.control-btn:hover {
	background: #4a4d51;
	border-color: rgba(232, 234, 237, 0.3);
	transform: translateY(-1px);
}

.control-btn:active {
	transform: translateY(0);
}

.control-btn:disabled {
	opacity: 0.5;
	cursor: not-allowed;
	border-color: rgba(232, 234, 237, 0.1);
	transform: none;
}

.control-btn.active {
	background: #a8c7fa;
	border-color: #a8c7fa;
}

.control-btn.danger {
	background: #f9dedc;
	border-color: #f9dedc;
}

.control-btn.danger .iconfont {
	color: #651a16;
}

.control-btn.active .iconfont {
	color: #163d7c;
}

.control-btn.utility-btn.active {
	background: #a8c7fa;
	border-color: #a8c7fa;
	/* color: #fff; */
}

.control-btn.secondary {
	opacity: 0.96;
}

.control-btn.more {
	background: #3c4043;
}

.control-btn.leave-call-btn {
	background: #d93025;
	border-color: #d93025;
	min-width: 60px;
}

.control-btn.leave-call-btn:hover {
	background: #ea4335;
	border-color: #ea4335;
}

.btn-icon {
	line-height: 1;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 28px;
	height: 28px;
	border-radius: 50%;
	/* background: rgba(0, 0, 0, 0.18); */
}

.btn-icon .iconfont {
	font-size: 22px;
	font-weight: 600;
}

.btn-svg {
	/* width: 16px; */
	/* height: 16px; */
	stroke: currentColor;
	fill: none;
	stroke-width: 1.9;
	stroke-linecap: round;
	stroke-linejoin: round;

	width: 30px;
	height: 30px;
	/* transform: scale(1.2); */
}

.btn-text {
	font-size: 12px;
	font-weight: 500;
	line-height: 1;
}




.mini-btn {
	padding: 6px 10px;
	border-radius: 6px;
	border: 1px solid #2f3b55;
	background: #1a2336;
	color: #d8e1f5;
	font-size: 12px;
	cursor: pointer;
}

@media (max-width: 1280px) {
	.participant-strip.with-screen-share {
		grid-template-columns: minmax(0, 1fr) clamp(168px, 20vw, 250px);
	}
	.participant-strip.with-screen-share .participant-list {
		grid-auto-rows: clamp(96px, 14vh, 130px);
	}
}

@media (max-width: 900px) {
	.participant-strip.with-screen-share {
		grid-template-columns: minmax(0, 1fr) clamp(154px, 25vw, 214px);
	}
	.participant-strip.with-screen-share .participant-list {
		grid-auto-rows: clamp(88px, 12vh, 116px);
	}
	.meeting-info {
		font-size: 12px;
	}
}

@media (max-width: 640px) {
	.participant-strip.with-screen-share {
		grid-template-columns: 1fr;
		height: auto;
	}
	.participant-strip.with-screen-share .participant-list {
		grid-template-columns: repeat(2, minmax(0, 1fr));
		max-height: 34vh;
	}
	.participant-strip.with-screen-share .participant-tile:not(.screen-share-tile) {
		aspect-ratio: 10 / 16;
	}
	.meeting-header {

		padding: 10px 12px;
	}
	.meeting-footer {
		flex-direction: column;
		gap: 10px;
		align-items: stretch;
	}
	.controls-left,
	.controls-center,
	.controls-right {
		justify-content: center;
	}
	.btn-text {
		display: none;
	}
}
</style>
