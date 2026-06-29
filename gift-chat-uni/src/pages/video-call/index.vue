<template>
  <view class="video-page">
    <view id="remote-video-view" class="remote-video">
      <view v-if="!hasRemoteVideo" class="fallback-backdrop"></view>
    </view>

    <view class="glass-layer"></view>

    <view class="top-status">
      <text class="status-time">{{ roomLabel }}</text>
      <view class="status-icons">
        <view class="signal-bars"><text></text><text></text><text></text></view>
        <view class="wifi-mark"></view>
        <view class="battery-mark"><text></text></view>
      </view>
    </view>

    <view class="top-actions">
      <view class="top-icon stack-icon">
        <text></text>
        <text></text>
      </view>
      <view class="top-icon plus-icon">+</view>
    </view>

    <view class="caller-card">
      <image class="caller-avatar" :src="uiIcons.user" mode="aspectFit" />
      <text class="caller-name">{{ peerLabel }}</text>
      <view class="calling-dots">
        <text></text>
        <text></text>
        <text></text>
      </view>
      <text class="caller-status">{{ statusLabel }}</text>
      <text v-if="callHint" class="notice-text">{{ callHint }}</text>
      <text v-if="notice" class="device-notice">{{ notice }}</text>
      <button v-if="canRetryLocalDevices" class="device-retry-button" @click="retryLocalDevices">
        {{ deviceRetryLabel }}
      </button>
      <view class="remote-state-row">
        <text :class="['remote-state-pill', remoteUserJoined && 'is-on']">{{ remoteUserJoined ? 'Peer joined' : 'Waiting peer' }}</text>
        <text :class="['remote-state-pill', remoteAudioAvailable && 'is-on']">{{ remoteAudioAvailable ? 'Peer mic on' : 'Peer mic off' }}</text>
        <text :class="['remote-state-pill', remoteVideoAvailable && 'is-on']">{{ remoteVideoAvailable ? 'Peer camera on' : 'Peer camera off' }}</text>
      </view>
    </view>

    <view id="local-video-view" class="local-video">
      <text v-if="!localVideoStarted" class="local-placeholder">Local</text>
    </view>

    <view class="call-chat">
      <input v-model="chatDraft" class="call-chat-input" placeholder="Message..." @confirm="sendCallChat" />
      <button :class="['call-chat-send', canSendChat && 'is-ready']" @click="sendCallChat">Send</button>
      <text v-if="chatNotice" class="call-chat-notice">{{ chatNotice }}</text>
    </view>

    <view class="control-panel">
      <view class="primary-controls">
        <view class="control-item">
          <button :class="['round-control', mutedAudio && 'is-off']" @click="toggleAudio">
            <text class="icon-mic"></text>
          </button>
          <text class="control-label">{{ micLabel }}</text>
        </view>
        <view class="control-item">
          <button class="round-control" @click="toggleSpeaker">
            <text class="icon-speaker"></text>
          </button>
          <text class="control-label">{{ speakerLabel }}</text>
        </view>
        <view class="control-item">
          <button :class="['round-control', mutedVideo && 'is-off']" @click="toggleVideo">
            <text class="icon-camera"></text>
          </button>
          <text class="control-label">{{ cameraLabel }}</text>
        </view>
      </view>

      <view class="secondary-controls">
        <button class="soft-control">
          <text class="icon-card"></text>
        </button>
        <button class="hangup-control" @click="hangup">
          <text class="icon-phone"></text>
        </button>
        <button class="soft-control" @click="toggleVideo">
          <text class="icon-switch"></text>
        </button>
      </view>
    </view>

    <view class="home-indicator"></view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import TRTC from 'trtc-sdk-v5'
import type { VideoSessionBootstrap, VideoSessionItem } from '@/types'
import { useAppStore } from '@/store/app'
import { uiIcons } from '@/utils/art'

const store = useAppStore()
const bootstrap = ref<VideoSessionBootstrap | null>(null)
const roomId = ref('')
const sessionId = ref('')
const notice = ref('')
const chatDraft = ref('')
const chatSending = ref(false)
const chatNotice = ref('')
const localVideoStarted = ref(false)
const localAudioStarted = ref(false)
const localDeviceFailed = ref(false)
const hasRemoteVideo = ref(false)
const remoteUserJoined = ref(false)
const remoteAudioAvailable = ref(false)
const remoteVideoAvailable = ref(false)
const mutedAudio = ref(false)
const mutedVideo = ref(false)
const speakerEnabled = ref(true)
const trtc = ref<TRTC | null>(null)
const joined = ref(false)

const peerLabel = computed(() => {
  const session = bootstrap.value?.session
  if (!session) return 'Preparing call'
  const currentUsername = store.state.currentUser?.username
  return session.initiatorUsername === currentUsername ? session.receiverUsername : session.initiatorUsername
})

const roomLabel = computed(() => bootstrap.value?.vendor?.toUpperCase() || 'TRTC')

const statusLabel = computed(() => {
  const status = bootstrap.value?.session.status || 'created'
  return {
    created: 'Connecting',
    joining: 'Joining room',
    active: 'In call',
    ended: 'Call ended',
    missed: 'Missed call',
    rejected: 'Call rejected'
  }[status as VideoSessionItem['status']]
})

const remotePlaceholder = computed(() => {
  if (!bootstrap.value?.sdkConfigured) return bootstrap.value?.note || 'TRTC is not configured'
  if (!joined.value) return 'Preparing video'
  if (!remoteUserJoined.value) return 'Waiting for the other side to answer'
  if (!remoteVideoAvailable.value) return 'Connected. Waiting for the other side camera'
  return ''
})

const callHint = computed(() => {
  if (hasRemoteVideo.value) return ''
  return remotePlaceholder.value
})

const micLabel = computed(() => mutedAudio.value ? 'Mic off' : 'Mic on')
const cameraLabel = computed(() => mutedVideo.value ? 'Camera off' : 'Camera on')
const speakerLabel = computed(() => speakerEnabled.value ? 'Speaker on' : 'Speaker off')
const canSendChat = computed(() => Boolean(chatDraft.value.trim()) && !chatSending.value)
const canRetryLocalDevices = computed(() => Boolean(trtc.value) && localDeviceFailed.value)
const deviceRetryLabel = computed(() => {
  if (mutedAudio.value && mutedVideo.value) return '重新开启麦克风和摄像头'
  if (mutedAudio.value) return '重新开启麦克风'
  return '重新开启摄像头'
})

onLoad(async (options) => {
  try {
    await store.bootstrap().catch(() => {})
    if (options?.bootstrap) {
      bootstrap.value = JSON.parse(decodeURIComponent(options.bootstrap)) as VideoSessionBootstrap
    } else if (options?.sessionId) {
      bootstrap.value = await store.getVideoSessionBootstrap(String(options.sessionId))
    }
    roomId.value = bootstrap.value?.session.roomId || ''
    sessionId.value = bootstrap.value?.session.id || ''
    await nextTick()
    await joinRoom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Video session failed to load'
  }
})

onUnload(() => {
  cleanup()
})

async function joinRoom() {
  const currentBootstrap = bootstrap.value as VideoSessionBootstrap | null
  if (!currentBootstrap) return
  if (!currentBootstrap.sdkConfigured || !currentBootstrap.userSig) {
    notice.value = currentBootstrap.note || 'TRTC SDKAppID or UserSig is missing'
    return
  }

  const environmentNotice = await getEnvironmentNotice()
  if (environmentNotice) {
    notice.value = environmentNotice
    return
  }

  const client = TRTC.create()
  trtc.value = client
  bindTrtcEvents(client)
  await store.updateVideoSessionStatus(sessionId.value, 'joining')

  await client.enterRoom({
    sdkAppId: Number(currentBootstrap.sdkAppId),
    userId: currentBootstrap.userId,
    userSig: currentBootstrap.userSig,
    strRoomId: currentBootstrap.session.roomId,
    scene: TRTC.TYPE.SCENE_RTC,
    autoReceiveAudio: true,
    autoReceiveVideo: false
  })
  joined.value = true

  await startLocalDevices(client)

  const session = await store.updateVideoSessionStatus(sessionId.value, 'active')
  bootstrap.value = { ...currentBootstrap, session }
  if (!notice.value) {
    notice.value = ''
  }
}

function bindTrtcEvents(client: TRTC) {
  client.on(TRTC.EVENT.REMOTE_USER_ENTER, () => {
    remoteUserJoined.value = true
  })

  client.on(TRTC.EVENT.REMOTE_VIDEO_AVAILABLE, async ({ userId, streamType }) => {
    remoteUserJoined.value = true
    remoteVideoAvailable.value = true
    try {
      await client.startRemoteVideo({ userId, streamType, view: 'remote-video-view' })
      hasRemoteVideo.value = true
      notice.value = ''
    } catch (error) {
      notice.value = error instanceof Error ? error.message : 'Remote video failed'
    }
  })

  client.on(TRTC.EVENT.REMOTE_VIDEO_UNAVAILABLE, () => {
    remoteVideoAvailable.value = false
    hasRemoteVideo.value = false
  })

  client.on(TRTC.EVENT.REMOTE_AUDIO_AVAILABLE, () => {
    remoteUserJoined.value = true
    remoteAudioAvailable.value = true
  })

  client.on(TRTC.EVENT.REMOTE_AUDIO_UNAVAILABLE, () => {
    remoteAudioAvailable.value = false
  })

  client.on(TRTC.EVENT.REMOTE_USER_EXIT, () => {
    remoteUserJoined.value = false
    remoteAudioAvailable.value = false
    remoteVideoAvailable.value = false
    hasRemoteVideo.value = false
    notice.value = 'The other side left the call'
  })

  client.on(TRTC.EVENT.ERROR, (error) => {
    notice.value = error?.message || 'TRTC error'
  })
}

async function startLocalDevices(client: TRTC) {
  const failures: string[] = []
  localDeviceFailed.value = false
  try {
    await client.startLocalVideo({ view: 'local-video-view' })
    localVideoStarted.value = true
    mutedVideo.value = false
  } catch (error) {
    mutedVideo.value = true
    failures.push(toDeviceMessage(error, 'camera'))
  }

  try {
    await client.startLocalAudio()
    localAudioStarted.value = true
    mutedAudio.value = false
  } catch (error) {
    mutedAudio.value = true
    failures.push(toDeviceMessage(error, 'microphone'))
  }

  if (failures.length) {
    localDeviceFailed.value = true
    notice.value = failures.join(' ')
  }
}

function toDeviceMessage(error: unknown, device: 'camera' | 'microphone') {
  const raw = error instanceof Error ? error.message : String(error || '')
  const deviceName = device === 'camera' ? 'Camera' : 'Microphone'
  if (!isSecureOrigin()) {
    return '当前页面不是安全连接，浏览器会禁止摄像头和麦克风。请使用 HTTPS 域名访问。'
  }
  if (/no camera|NotFoundError|not found/i.test(raw)) {
    return `${deviceName} was not found. Check the device connection.`
  }
  if (/permission|NotAllowedError|denied/i.test(raw)) {
    return `${deviceName} permission is blocked. Allow access in the browser address bar.`
  }
  if (/NotReadableError|TrackStartError|Could not start|already in use|device in use/i.test(raw)) {
    return `${deviceName} is already in use by another app or browser tab. Close it and retry.`
  }
  return `${deviceName} failed to start. Check browser permissions and retry.`
}

async function getEnvironmentNotice() {
  if (!isSecureOrigin()) {
    return '当前访问地址不是 HTTPS，浏览器会禁止网页调用摄像头和麦克风。请用 HTTPS 域名重新打开。'
  }

  try {
    const checkResult = await TRTC.isSupported()
    if (checkResult?.result) return ''
    return buildUnsupportedMessage(checkResult?.detail)
  } catch {
    return '当前浏览器环境检测失败，请使用最新版 Chrome、Edge、Safari 或 Firefox 重试。'
  }
}

function isSecureOrigin() {
  if (typeof window === 'undefined') return true
  const hostname = window.location.hostname
  return window.isSecureContext || ['localhost', '127.0.0.1', '::1'].includes(hostname)
}

function buildUnsupportedMessage(detail: Record<string, boolean> = {}) {
  if (detail.isMediaDevicesSupported === false) {
    return '当前浏览器不能获取摄像头/麦克风。请确认使用 HTTPS，并换最新版 Chrome、Edge、Safari 或 Firefox。'
  }
  if (detail.isWebRTCSupported === false) {
    return '当前浏览器不支持 WebRTC，无法进行网页视频通话。请换最新版 Chrome、Edge、Safari 或 Firefox。'
  }
  if (detail.isBrowserSupported === false) {
    return '当前浏览器暂不支持 TRTC 网页通话。请换最新版 Chrome、Edge、Safari 或 Firefox。'
  }
  if (detail.isH264EncodeSupported === false || detail.isVp8EncodeSupported === false) {
    return '当前浏览器不支持发布视频所需的编码能力。请更新浏览器或更换设备重试。'
  }
  if (detail.isH264DecodeSupported === false || detail.isVp8DecodeSupported === false) {
    return '当前浏览器不支持播放对方视频所需的解码能力。请更新浏览器或更换设备重试。'
  }
  return '当前浏览器环境不支持 TRTC 网页通话。请确认 HTTPS、浏览器版本和摄像头/麦克风权限。'
}

async function retryLocalDevices() {
  if (!trtc.value) return
  notice.value = ''
  await startLocalDevices(trtc.value)
}
async function toggleAudio() {
  if (!trtc.value) return
  try {
    if (mutedAudio.value) {
      await trtc.value.startLocalAudio()
      localAudioStarted.value = true
      mutedAudio.value = false
    } else {
      await trtc.value.stopLocalAudio()
      localAudioStarted.value = false
      mutedAudio.value = true
    }
    localDeviceFailed.value = mutedAudio.value || mutedVideo.value
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Microphone toggle failed'
  }
}

async function toggleVideo() {
  if (!trtc.value) return
  try {
    if (mutedVideo.value) {
      await trtc.value.startLocalVideo({ view: 'local-video-view' })
      localVideoStarted.value = true
      mutedVideo.value = false
    } else {
      await trtc.value.stopLocalVideo()
      localVideoStarted.value = false
      mutedVideo.value = true
    }
    localDeviceFailed.value = mutedAudio.value || mutedVideo.value
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Camera toggle failed'
  }
}

function toggleSpeaker() {
  speakerEnabled.value = !speakerEnabled.value
}

async function sendCallChat() {
  const content = chatDraft.value.trim()
  const session = bootstrap.value?.session
  if (!content || !session || chatSending.value) return

  chatSending.value = true
  chatNotice.value = ''
  try {
    if (session.channelType === 'support') {
      store.setActiveSupportConversation(session.channelId)
      await store.sendSupport(content)
    } else {
      await store.sendFriendMessage(session.channelId, content)
    }
    chatDraft.value = ''
    chatNotice.value = 'Sent'
    setTimeout(() => {
      if (chatNotice.value === 'Sent') chatNotice.value = ''
    }, 1600)
  } catch (error) {
    chatNotice.value = error instanceof Error ? error.message : 'Message failed'
  } finally {
    chatSending.value = false
  }
}

async function hangup() {
  await cleanup(true)
  uni.navigateBack()
}

async function cleanup(updateStatus = false) {
  try {
    if (trtc.value) {
      await trtc.value.stopRemoteVideo({ userId: '*' }).catch(() => {})
      await trtc.value.stopLocalVideo().catch(() => {})
      await trtc.value.stopLocalAudio().catch(() => {})
      if (joined.value) {
        await trtc.value.exitRoom().catch(() => {})
      }
      trtc.value.destroy()
      trtc.value = null
    }
    joined.value = false
    localVideoStarted.value = false
    localAudioStarted.value = false
    localDeviceFailed.value = false
    hasRemoteVideo.value = false
    remoteUserJoined.value = false
    remoteAudioAvailable.value = false
    remoteVideoAvailable.value = false
    if (updateStatus && sessionId.value) {
      const session = await store.updateVideoSessionStatus(sessionId.value, 'ended')
      if (bootstrap.value) {
        bootstrap.value = { ...bootstrap.value, session }
      }
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Hang up failed'
  }
}
</script>

<style scoped lang="scss">
.video-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #1b1111;
  color: #ffffff;
  box-sizing: border-box;
}

.remote-video {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #1d1414;
}

.fallback-backdrop {
  position: absolute;
  inset: -5%;
  background:
    radial-gradient(circle at 36% 18%, rgba(218, 156, 143, 0.82), transparent 28%),
    radial-gradient(circle at 66% 14%, rgba(108, 45, 41, 0.8), transparent 30%),
    radial-gradient(circle at 34% 64%, rgba(180, 95, 84, 0.88), transparent 35%),
    linear-gradient(180deg, #2b1717 0%, #83504a 48%, #251313 100%);
  filter: blur(20px);
  transform: scale(1.08);
}

.glass-layer {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.2) 0%, rgba(0, 0, 0, 0.02) 34%, rgba(0, 0, 0, 0.42) 100%);
  pointer-events: none;
}

.top-status {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 42rpx 64rpx 0;
}

.status-time {
  font-size: 38rpx;
  font-weight: 800;
  line-height: 1;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.28);
}

.status-icons {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.signal-bars {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  height: 24rpx;
}

.signal-bars text {
  width: 7rpx;
  border-radius: 4rpx;
  background: #ffffff;
}

.signal-bars text:nth-child(1) { height: 10rpx; }
.signal-bars text:nth-child(2) { height: 17rpx; }
.signal-bars text:nth-child(3) { height: 24rpx; }

.wifi-mark {
  width: 28rpx;
  height: 20rpx;
  border: 6rpx solid #ffffff;
  border-bottom: 0;
  border-left-color: transparent;
  border-right-color: transparent;
  border-radius: 30rpx 30rpx 0 0;
}

.battery-mark {
  position: relative;
  width: 48rpx;
  height: 24rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.9);
  border-radius: 7rpx;
  box-sizing: border-box;
}

.battery-mark::after {
  content: '';
  position: absolute;
  right: -8rpx;
  top: 5rpx;
  width: 4rpx;
  height: 10rpx;
  border-radius: 2rpx;
  background: rgba(255, 255, 255, 0.9);
}

.battery-mark text {
  display: block;
  width: 28rpx;
  height: 14rpx;
  margin: 1rpx;
  border-radius: 3rpx;
  background: rgba(255, 255, 255, 0.92);
}

.top-actions {
  position: relative;
  z-index: 2;
  margin-top: 76rpx;
  padding: 0 52rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 70rpx;
  font-weight: 200;
  line-height: 1;
  text-shadow: 0 3rpx 10rpx rgba(0, 0, 0, 0.24);
}

.stack-icon {
  position: relative;
}

.stack-icon text {
  position: absolute;
  width: 34rpx;
  height: 34rpx;
  border: 5rpx solid #ffffff;
  border-radius: 6rpx;
  box-sizing: border-box;
}

.stack-icon text:first-child {
  left: 5rpx;
  top: 8rpx;
}

.stack-icon text:last-child {
  right: 5rpx;
  bottom: 8rpx;
}

.caller-card {
  position: relative;
  z-index: 2;
  margin-top: 138rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.caller-avatar {
  width: 136rpx;
  height: 136rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.78);
  overflow: hidden;
  box-shadow: 0 14rpx 34rpx rgba(0, 0, 0, 0.24);
}

.caller-name {
  margin-top: 32rpx;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.2;
  text-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.35);
}

.calling-dots {
  margin-top: 34rpx;
  display: flex;
  gap: 18rpx;
}

.calling-dots text {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  animation: dotPulse 1.25s infinite ease-in-out;
}

.calling-dots text:nth-child(2) { animation-delay: 0.16s; }
.calling-dots text:nth-child(3) { animation-delay: 0.32s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.45; transform: scale(0.86); }
  40% { opacity: 1; transform: scale(1); }
}

.caller-status {
  margin-top: 18rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.24);
}

.notice-text {
  max-width: 72vw;
  margin-top: 14rpx;
  font-size: 22rpx;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.78);
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.28);
}

.device-notice {
  max-width: 72vw;
  margin-top: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.24);
  color: rgba(255, 255, 255, 0.78);
  font-size: 20rpx;
  line-height: 1.35;
}

.device-retry-button {
  min-width: 260rpx;
  max-width: 72vw;
  height: 56rpx;
  margin-top: 12rpx;
  padding: 0 22rpx;
  border: 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  color: #2e1616;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 56rpx;
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.18);
}

.remote-state-row {
  margin-top: 18rpx;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10rpx;
}

.remote-state-pill {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.22);
  color: rgba(255, 255, 255, 0.72);
  font-size: 19rpx;
  font-weight: 700;
  line-height: 1.2;
}

.remote-state-pill.is-on {
  background: rgba(255, 255, 255, 0.9);
  color: #2e1616;
}

.local-video {
  position: absolute;
  z-index: 3;
  right: 26rpx;
  top: 216rpx;
  width: 140rpx;
  height: 196rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.66);
  border-radius: 18rpx;
  overflow: hidden;
  background: rgba(30, 20, 20, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 14rpx 34rpx rgba(0, 0, 0, 0.22);
}

.local-placeholder {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.72);
}

.call-chat {
  position: absolute;
  z-index: 4;
  left: 48rpx;
  right: 48rpx;
  bottom: 364rpx;
  min-height: 66rpx;
  display: grid;
  grid-template-columns: 1fr 108rpx;
  align-items: center;
  gap: 10rpx;
}

.call-chat-input {
  min-width: 0;
  height: 66rpx;
  padding: 0 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.22);
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.28);
  color: #ffffff;
  font-size: 24rpx;
  box-sizing: border-box;
  backdrop-filter: blur(14rpx);
}

.call-chat-send {
  height: 66rpx;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.74);
  font-size: 22rpx;
  font-weight: 800;
  line-height: 66rpx;
}

.call-chat-send.is-ready {
  background: rgba(255, 255, 255, 0.94);
  color: #2e1616;
}

.call-chat-notice {
  position: absolute;
  left: 18rpx;
  top: -34rpx;
  color: rgba(255, 255, 255, 0.8);
  font-size: 20rpx;
}

.control-panel {
  position: absolute;
  z-index: 4;
  left: 0;
  right: 0;
  bottom: 46rpx;
  padding: 0 64rpx;
}

.primary-controls,
.secondary-controls {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  align-items: center;
  justify-items: center;
}

.primary-controls {
  gap: 34rpx;
}

.secondary-controls {
  margin-top: 54rpx;
  gap: 46rpx;
}

.control-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.round-control,
.soft-control,
.hangup-control {
  width: 124rpx;
  height: 124rpx;
  border: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-sizing: border-box;
}

.round-control {
  background: rgba(255, 255, 255, 0.96);
  color: #050505;
}

.round-control.is-off {
  background: rgba(255, 255, 255, 0.32);
  color: rgba(255, 255, 255, 0.82);
}

.control-label {
  margin-top: 18rpx;
  font-size: 25rpx;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  text-shadow: 0 3rpx 10rpx rgba(0, 0, 0, 0.35);
}

.soft-control {
  background: rgba(28, 12, 12, 0.68);
  color: #ffffff;
}

.hangup-control {
  background: #ff4f4f;
  color: #ffffff;
}

.home-indicator {
  position: absolute;
  z-index: 5;
  left: 50%;
  bottom: 14rpx;
  width: 270rpx;
  height: 9rpx;
  transform: translateX(-50%);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.94);
}

.icon-mic,
.icon-speaker,
.icon-camera,
.icon-card,
.icon-phone,
.icon-switch {
  position: relative;
  display: block;
  width: 58rpx;
  height: 58rpx;
  box-sizing: border-box;
}

.icon-mic::before {
  content: '';
  position: absolute;
  left: 19rpx;
  top: 4rpx;
  width: 20rpx;
  height: 34rpx;
  border-radius: 12rpx;
  background: currentColor;
}

.icon-mic::after {
  content: '';
  position: absolute;
  left: 12rpx;
  top: 28rpx;
  width: 34rpx;
  height: 24rpx;
  border: 6rpx solid currentColor;
  border-top: 0;
  border-radius: 0 0 18rpx 18rpx;
}

.icon-speaker::before {
  content: '';
  position: absolute;
  left: 4rpx;
  top: 18rpx;
  width: 22rpx;
  height: 24rpx;
  background: currentColor;
  border-radius: 4rpx;
}

.icon-speaker::after {
  content: '';
  position: absolute;
  right: 3rpx;
  top: 12rpx;
  width: 22rpx;
  height: 34rpx;
  border: 6rpx solid currentColor;
  border-left: 0;
  border-radius: 0 28rpx 28rpx 0;
}

.icon-camera::before {
  content: '';
  position: absolute;
  left: 3rpx;
  top: 16rpx;
  width: 38rpx;
  height: 28rpx;
  border-radius: 5rpx;
  background: currentColor;
}

.icon-camera::after {
  content: '';
  position: absolute;
  right: 4rpx;
  top: 20rpx;
  width: 0;
  height: 0;
  border-top: 10rpx solid transparent;
  border-bottom: 10rpx solid transparent;
  border-left: 16rpx solid currentColor;
}

.icon-card {
  border: 6rpx solid currentColor;
  border-radius: 8rpx;
  transform: rotate(-5deg);
}

.icon-card::before {
  content: '';
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  border: 5rpx solid currentColor;
}

.icon-card::after {
  content: '';
  position: absolute;
  left: 9rpx;
  right: 9rpx;
  bottom: 9rpx;
  height: 18rpx;
  border-radius: 16rpx 16rpx 0 0;
  border: 5rpx solid currentColor;
  border-bottom: 0;
}

.icon-phone::before {
  content: '';
  position: absolute;
  left: 8rpx;
  top: 22rpx;
  width: 42rpx;
  height: 20rpx;
  border-radius: 22rpx 22rpx 8rpx 8rpx;
  border: 0;
  background: currentColor;
  transform: rotate(180deg);
}

.icon-phone::after {
  content: '';
  position: absolute;
  left: 20rpx;
  top: 26rpx;
  width: 18rpx;
  height: 16rpx;
  border-radius: 0 0 6rpx 6rpx;
  background: #ff4f4f;
}

.icon-switch::before,
.icon-switch::after {
  content: '';
  position: absolute;
  width: 34rpx;
  height: 34rpx;
  border: 6rpx solid currentColor;
  border-left-color: transparent;
  border-radius: 50%;
}

.icon-switch::before {
  left: 7rpx;
  top: 4rpx;
  transform: rotate(-30deg);
}

.icon-switch::after {
  right: 7rpx;
  bottom: 4rpx;
  transform: rotate(150deg);
}

@media (min-width: 760px) {
  .video-page {
    max-width: 430px;
    min-height: 860px;
    height: 100vh;
    margin: 0 auto;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08), 0 24px 80px rgba(0, 0, 0, 0.3);
  }
}
</style>

