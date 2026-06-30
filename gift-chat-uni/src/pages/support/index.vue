<template>
  <view class="chat-container">
    <view class="chat-main">
      <view class="chat-header">
        <view class="header-left">
          <view class="back-btn" @click="goHome">
            <text>&lt;</text>
          </view>
          <image class="header-avatar" :src="uiIcons.user" mode="aspectFit" />
          <view class="header-info">
            <text class="header-name">{{ headerTitle }}</text>
            <text class="header-note">{{ headerSubtitle }}</text>
            <text :class="['header-status', { offline: socketStatus !== 'online' }]">{{ socketStatusLabel }}</text>
          </view>
        </view>
        <view class="header-actions">
          <view class="action-btn text-action" @click="startVideoCall">Video</view>
        </view>
      </view>

      <scroll-view scroll-y class="message-area" :scroll-into-view="messageScrollTarget">
        <view class="message-list">
          <view class="date-divider">
            <text>Today</text>
          </view>

          <view
            v-for="message in conversation"
            :key="message.id"
            :class="['message-wrapper', isMine(message) ? 'mine' : 'theirs']"
          >
            <image v-if="!isMine(message) && message.author !== 'system'" class="msg-avatar" :src="uiIcons.user" mode="aspectFit" />

            <view :class="['message-bubble', message.author === 'system' ? 'system-bubble' : '']">
              <image
                v-if="message.type === 'image'"
                class="message-img"
                :src="message.content"
                mode="aspectFill"
                @click="previewImage(message.content)"
              />
              <view v-else-if="message.type === 'voice'" class="voice-chip" @click="playVoice(message.content)">
                <text>Voice message</text>
              </view>
              <view v-else-if="message.type === 'video'" class="video-call-card">
                <text class="video-call-title">{{ videoCallTitle(message) }}</text>
                <text class="video-call-room">{{ videoCallRoom(message) }}</text>
                <text :class="['video-call-status', videoCallStatus(message)]">{{ videoCallStatusLabel(message) }}</text>
                <view class="video-call-actions">
                  <button v-if="canAnswerVideoMessage(message)" class="video-call-btn answer" @click.stop="answerVideoMessage(message)">Answer</button>
                  <button v-if="canRejectVideoMessage(message)" class="video-call-btn decline" @click.stop="rejectVideoMessage(message)">Decline</button>
                  <button v-if="canEnterVideoMessage(message)" class="video-call-btn enter" @click.stop="enterVideoMessage(message)">Enter call</button>
                </view>
              </view>
              <text v-else class="message-text">{{ message.content }}</text>

              <view v-if="message.author !== 'system'" class="message-meta">
                <text class="msg-time">{{ message.createdAt }}</text>
                <text v-if="isMine(message)" class="msg-status">{{ message.readState === 'read' ? '✓✓' : '✓' }}</text>
              </view>
            </view>

            <image v-if="isMine(message)" class="msg-avatar" :src="uiIcons.user" mode="aspectFit" />
          </view>

          <view id="msg-bottom"></view>
        </view>
      </scroll-view>

      <view v-if="pendingImageUrl" class="image-preview-bar">
        <image class="preview-thumb" :src="pendingImageUrl" mode="aspectFill" />
        <text class="preview-text">Image ready. Send it when you are ready.</text>
        <view class="preview-close" @click="clearPendingImage">x</view>
      </view>

      <view class="input-area">
        <view class="input-toolbar">
          <view class="tool-btn" @click="sendImage">Image</view>
          <view class="tool-btn" @click="sendGif">GIF</view>
          <view class="tool-btn" @click="startVideoCall">Video</view>
        </view>
        <view class="input-row">
          <input v-model="draft" class="message-input" placeholder="Type a message..." @confirm="handleSend" />
          <view class="send-btn" :class="{ active: canSend }" @click="handleSend">
            <text>Send</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="incomingVideoInvite" class="incoming-call-mask">
      <view class="incoming-call-dialog">
        <text class="incoming-call-title">Video call</text>
        <text class="incoming-call-copy">{{ incomingVideoInvite.initiatorUsername }} is calling you.</text>
        <view class="incoming-call-actions">
          <button class="incoming-call-btn decline" @click="declineIncomingVideo">Decline</button>
          <button class="incoming-call-btn answer" @click="answerIncomingVideo">Answer</button>
        </view>
      </view>
    </view>

    <text v-if="notice" class="notice-text">{{ notice }}</text>
  </view>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { createBroadcast, fetchVideoSessionBootstrap, uploadImage } from '@/utils/api'
import { connectChatSocket } from '@/utils/realtime'
import { uiIcons } from '@/utils/art'
import type { ChatMessage, ChatReadReceiptEvent, ChatRealtimePayload, PresenceEvent, VideoCallMessagePayload, VideoInviteEvent, VideoSessionItem, VideoSessionStatusEvent } from '@/types'

const store = useAppStore()
const draft = ref('')
const notice = ref('')
const socketTask = ref<UniApp.SocketTask | null>(null)
const readRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const socketStatus = ref<'connecting' | 'online' | 'offline'>('connecting')
const pendingImageUrl = ref('')
const messageScrollTarget = ref('msg-bottom')
const audioEnabled = ref(false)
const handledVideoInvites = new Set<string>()
const localVideoStatuses = ref<Record<string, VideoSessionItem['status']>>({})
const incomingVideoInvite = ref<VideoInviteEvent | null>(null)

const conversation = computed(() => store.state.supportMessages)
const isAgent = computed(() => store.state.currentUser?.roleCode === 'AGENT')
const activeConversation = computed(() =>
  store.state.supportConversations.find((item) => item.conversationId === store.state.supportConversationId) || null
)
const headerTitle = computed(() => isAgent.value ? (activeConversation.value?.customerUsername || 'Customer chat') : 'Dedicated Support')
const headerSubtitle = computed(() => isAgent.value ? 'tap to switch customer' : (activeConversation.value?.assignedAgent || 'assigned support'))
const heroLabel = computed(() => isAgent.value ? 'Customer conversation' : 'Dedicated support')
const heroCopy = computed(() => isAgent.value ? 'Reply to this customer here.' : 'Your support agent will reply in this chat.')
const balanceSummary = computed(() => store.state.balanceSummary)
const canSend = computed(() => Boolean(draft.value.trim() || pendingImageUrl.value))
const socketStatusLabel = computed(() => ({
  connecting: 'connecting',
  online: 'online',
  offline: 'reconnecting'
})[socketStatus.value])

onShow(() => {
  store.bootstrap().then(async () => {
    applyPendingSupportDraft()
    connectSocket()
    await store.markSupportRead()
  }).catch(() => {
    applyPendingSupportDraft()
    connectSocket()
  })
})

onUnmounted(() => {
  closeSocket()
  stopReadRefresh()
  detachPasteListener()
})

onMounted(() => {
  attachPasteListener()
})

watch(
  () => store.state.supportConversationId,
  async () => {
    await connectSocket()
    await store.markSupportRead().catch(() => {})
  }
)

watch(
  () => conversation.value.length,
  () => {
    scrollMessagesToBottom()
  }
)

function isMine(message: ChatMessage) {
  return message.author === 'me'
}

function isReadReceipt(payload: ChatRealtimePayload): payload is ChatReadReceiptEvent {
  return 'eventType' in payload && payload.eventType === 'read'
}

function isVideoInvite(payload: ChatRealtimePayload): payload is VideoInviteEvent {
  return 'eventType' in payload && payload.eventType === 'video_invite'
}

function isVideoSessionStatus(payload: ChatRealtimePayload): payload is VideoSessionStatusEvent {
  return 'eventType' in payload && payload.eventType === 'video_session_status'
}

function isPresenceEvent(payload: ChatRealtimePayload): payload is PresenceEvent {
  return 'eventType' in payload && payload.eventType === 'presence'
}

function handleVideoSessionStatus(event: VideoSessionStatusEvent) {
  if (event.channelId !== store.state.supportConversationId) return
  const updated = store.applyVideoSessionStatus(event)
  setLocalVideoStatus(event.sessionId, updated?.status || event.status)
  if (incomingVideoInvite.value?.sessionId === event.sessionId && isTerminalVideoStatus(event.status)) {
    incomingVideoInvite.value = null
  }
}

function isTerminalVideoStatus(status: VideoSessionItem['status']) {
  return ['ended', 'missed', 'rejected'].includes(status)
}

function connectSocket() {
  closeSocket()
  try {
    const conversationId = store.state.supportConversationId
    socketStatus.value = 'connecting'
    socketTask.value = connectChatSocket('support', conversationId, (payload) => {
      if (isReadReceipt(payload)) {
        if (payload.readerUsername !== store.state.currentUser?.username) {
          store.applySupportReadReceipt(conversationId)
        }
        return
      }

      if (isPresenceEvent(payload)) {
        store.applySupportPresence(payload.channelId, payload.online)
        return
      }

      if (isVideoInvite(payload)) {
        handleVideoInvite(payload)
        return
      }

      if (isVideoSessionStatus(payload)) {
        handleVideoSessionStatus(payload)
        return
      }

      if (shouldPlayIncomingSound(payload, conversationId)) {
        playIncomingSound()
      }
      store.pushSupportRealtime(payload, conversationId)
      scrollMessagesToBottom()
      store.markSupportRead().catch(() => {})
    }, {
      onOpen: () => {
        socketStatus.value = 'online'
        store.recoverSupportMessages(conversationId).catch(() => {})
      },
      onClose: () => {
        socketStatus.value = 'offline'
      },
      onError: () => {
        socketStatus.value = 'offline'
      },
      onReconnect: () => {
        socketStatus.value = 'connecting'
        store.recoverSupportMessages(conversationId).catch(() => {})
      }
    })
  } catch {
    socketStatus.value = 'offline'
  }
}

function shouldPlayIncomingSound(message: ChatMessage, conversationId: string) {
  return isAgent.value
    && message.author !== 'me'
    && message.author !== 'system'
    && conversationId
}

function closeSocket() {
  socketTask.value?.close({})
  socketTask.value = null
  socketStatus.value = 'offline'
}

function handleVideoInvite(invite: VideoInviteEvent) {
  if (invite.channelId !== store.state.supportConversationId) return
  if (handledVideoInvites.has(invite.sessionId)) return
  handledVideoInvites.add(invite.sessionId)

  const currentUsername = store.state.currentUser?.username
  if (!currentUsername || invite.initiatorUsername === currentUsername) return

  incomingVideoInvite.value = invite
}

async function declineIncomingVideo() {
  const invite = incomingVideoInvite.value
  if (!invite) return
  incomingVideoInvite.value = null
  await store.updateVideoSessionStatus(invite.sessionId, 'rejected').catch(() => {})
  setLocalVideoStatus(invite.sessionId, 'rejected')
}

async function answerIncomingVideo() {
  const invite = incomingVideoInvite.value
  if (!invite) return
  incomingVideoInvite.value = null
  try {
    const session = await store.updateVideoSessionStatus(invite.sessionId, 'joining')
    setLocalVideoStatus(invite.sessionId, session.status)
    if (isTerminalVideoStatus(session.status)) {
      showNotice('Call is no longer available')
      return
    }
    await openVideoSession(invite.sessionId)
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Unable to join call')
  }
}

function stopReadRefresh() {
  if (!readRefreshTimer.value) return
  clearInterval(readRefreshTimer.value)
  readRefreshTimer.value = null
}

function startReadRefresh() {
  stopReadRefresh()
  const startedAt = Date.now()
  readRefreshTimer.value = setInterval(async () => {
    if (Date.now() - startedAt > 12000) {
      stopReadRefresh()
      return
    }

    try {
      await store.refreshSupport()
      const hasPendingOwnMessage = conversation.value.some((message) => message.author === 'me' && message.readState === 'sent')
      if (!hasPendingOwnMessage) {
        stopReadRefresh()
      }
    } catch {
      // keep the existing local message state if a refresh fails
    }
  }, 1500)
}

function scrollMessagesToBottom() {
  nextTick(() => {
    messageScrollTarget.value = ''
    nextTick(() => {
      messageScrollTarget.value = 'msg-bottom'
    })
  })
}

function openConversationPicker() {
  if (!isAgent.value) return
  const items = store.state.supportConversations.map((item) => item.customerUsername)
  if (!items.length) return
  uni.showActionSheet({
    itemList: items,
    success(result) {
      const next = store.state.supportConversations[result.tapIndex]
      if (next) {
        store.setActiveSupportConversation(next.conversationId)
      }
    }
  })
}

function selectConversation(conversationId: string) {
  enableAudio()
  store.setActiveSupportConversation(conversationId)
}

function lastMessageText(item: { messages: ChatMessage[] }) {
  const last = item.messages[item.messages.length - 1]
  if (!last) return 'No messages'
  if (last.type === 'image') return 'Image'
  if (last.type === 'voice') return 'Voice'
  if (last.type === 'video') return 'Video call'
  return last.content.length > 28 ? `${last.content.slice(0, 28)}...` : last.content
}

function parseVideoCallMessage(message: ChatMessage) {
  if (message.type !== 'video') return null
  try {
    const payload = JSON.parse(message.content) as Partial<VideoCallMessagePayload>
    if (payload.kind !== 'video_call' || !payload.sessionId || !payload.roomId) {
      return null
    }
    return payload as VideoCallMessagePayload
  } catch {
    const match = message.content.match(/Room\s+([A-Za-z0-9_-]+)/i)
    if (!match) return null
    return {
      kind: 'video_call',
      sessionId: '',
      roomId: match[1],
      channelType: 'support',
      channelId: store.state.supportConversationId,
      initiatorUsername: '',
      receiverUsername: ''
    } as VideoCallMessagePayload
  }
}

function videoCallSession(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return null
  return store.state.videoSessions.find((session) => session.id === payload.sessionId) || null
}

function videoCallStatus(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (payload?.sessionId && localVideoStatuses.value[payload.sessionId]) {
    return localVideoStatuses.value[payload.sessionId]
  }
  return videoCallSession(message)?.status || 'created'
}

function videoCallStatusLabel(message: ChatMessage) {
  return {
    created: 'Waiting for answer',
    joining: 'Joining',
    active: 'In call',
    ended: 'Ended',
    missed: 'Missed',
    rejected: 'Declined'
  }[videoCallStatus(message)]
}

function videoCallTitle(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const currentUsername = store.state.currentUser?.username
  if (!payload) return 'Video call request'
  if (payload.initiatorUsername === currentUsername) return 'You started a video call'
  return `${payload.initiatorUsername || 'Support'} is calling`
}

function videoCallRoom(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  return `Room ${payload?.roomId || '-'}`
}

function canAnswerVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  return Boolean(payload?.sessionId)
    && payload?.initiatorUsername !== store.state.currentUser?.username
    && (status === 'created' || status === 'joining')
}

function canRejectVideoMessage(message: ChatMessage) {
  return canAnswerVideoMessage(message)
}

function canEnterVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  return Boolean(payload?.sessionId) && ['created', 'joining', 'active'].includes(status)
}

function setLocalVideoStatus(sessionId: string, status: VideoSessionItem['status']) {
  localVideoStatuses.value = {
    ...localVideoStatuses.value,
    [sessionId]: status
  }
}

async function answerVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    setLocalVideoStatus(payload.sessionId, 'joining')
    await store.updateVideoSessionStatus(payload.sessionId, 'joining')
    await openVideoSession(payload.sessionId)
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Unable to join call')
  }
}

async function rejectVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    const updated = await store.updateVideoSessionStatus(payload.sessionId, 'rejected')
    setLocalVideoStatus(payload.sessionId, updated.status)
    showNotice('Video call declined.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Unable to decline call')
  }
}

async function enterVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    await openVideoSession(payload.sessionId)
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Unable to join call')
  }
}

async function openVideoSession(sessionId: string) {
  const bootstrap = await fetchVideoSessionBootstrap(sessionId)
  const encoded = encodeURIComponent(JSON.stringify(bootstrap))
  uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
}

function attachPasteListener() {
  // #ifdef H5
  document.addEventListener('paste', handlePasteImage)
  // #endif
}

function detachPasteListener() {
  // #ifdef H5
  document.removeEventListener('paste', handlePasteImage)
  // #endif
}

function applyPendingSupportDraft() {
  const pendingDraft = uni.getStorageSync('pending-support-draft') as string | undefined
  if (!pendingDraft) return
  if (!draft.value.trim()) {
    draft.value = pendingDraft
  }
  uni.removeStorageSync('pending-support-draft')
}

async function handlePasteImage(event: ClipboardEvent) {
  const item = Array.from(event.clipboardData?.items || []).find((entry) => entry.type.startsWith('image/'))
  const file = item?.getAsFile()
  if (!file) return

  event.preventDefault()
  clearPendingImage()
  pendingImageUrl.value = URL.createObjectURL(file)
  showNotice('Image pasted. Click Send to send it.')
}

async function sendText(content: string) {
  const value = content.trim()
  if (!value) return

  try {
    await store.sendSupport(value)
    startReadRefresh()
    draft.value = ''
    showNotice('Message sent.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Send failed')
  }
}

function handleSend() {
  enableAudio()
  if (pendingImageUrl.value) {
    sendPendingImage()
    return
  }
  const value = draft.value.trim()
  if (!value) return
  sendText(value)
}

async function sendPendingImage() {
  const imageUrl = pendingImageUrl.value
  if (!imageUrl) return
  try {
    const asset = await uploadImage(imageUrl)
    await store.sendSupport(asset.publicUrl, 'image')
    startReadRefresh()
    clearPendingImage()
    if (draft.value.trim()) {
      await sendText(draft.value)
    }
    showNotice('Image sent.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Image send failed')
  }
}

function clearPendingImage() {
  if (pendingImageUrl.value) {
    URL.revokeObjectURL(pendingImageUrl.value)
  }
  pendingImageUrl.value = ''
}

function useQuickQuestion(question: string) {
  draft.value = question
  sendText(question)
}

async function sendImage() {
  try {
    const filePath = await chooseImageOnce()
    if (!filePath) return
    const asset = await uploadImage(filePath)
    await store.sendSupport(asset.publicUrl, 'image')
    showNotice('Image sent.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Image send failed')
  }
}

function enableAudio() {
  audioEnabled.value = true
}

function playIncomingSound() {
  if (!audioEnabled.value) return
  // #ifdef H5
  try {
    const AudioContextCtor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AudioContextCtor) return
    const context = new AudioContextCtor()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = 880
    gain.gain.value = 0.06
    oscillator.connect(gain)
    gain.connect(context.destination)
    oscillator.start()
    oscillator.stop(context.currentTime + 0.16)
  } catch {
    // Browser audio can be blocked until the user interacts with the page.
  }
  // #endif
}

function sendVoice() {
  showNotice('Voice recording is not supported in this H5 build yet.')
}

async function sendGif() {
  try {
    await store.sendSupport('[GIF] smile', 'gif')
    showNotice('GIF sent.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'GIF send failed')
  }
}

function showTools() {
  showNotice('Use Image, GIF, Video, or the quick actions below.')
}

async function sendOwnCustomerBroadcast() {
  if (!isAgent.value) return
  const content = draft.value.trim()
  if (!content) {
    showNotice('Type a broadcast message first.')
    return
  }

  try {
    const broadcast = await createBroadcast({
      scope: 'own',
      content,
      messageType: 'text'
    })
    draft.value = ''
    await store.refreshSupport().catch(() => {})
    showNotice(`Broadcast sent to ${broadcast.deliveredCount} customer${broadcast.deliveredCount === 1 ? '' : 's'}.`)
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Broadcast failed')
  }
}

async function startVideoCall() {
  if (!store.state.supportConversationId) return
  try {
    const bootstrap = await store.createVideoSession({
      channelType: 'support',
      channelId: store.state.supportConversationId
    })
    setLocalVideoStatus(bootstrap.session.id, bootstrap.session.status)
    await store.refreshSupport().catch(() => {})
    const encoded = encodeURIComponent(JSON.stringify(bootstrap))
    uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Video call failed')
  }
}

function previewImage(url: string) {
  uni.previewImage({
    urls: [url],
    current: url
  })
}

function playVoice(url: string) {
  const audio = uni.createInnerAudioContext()
  audio.src = url
  audio.play()
}

function goHome() {
  uni.redirectTo({ url: '/pages/home/index' })
}

function chooseImageOnce() {
  // #ifdef H5
  return chooseBrowserImageOnce()
  // #endif
  return new Promise<string | null>((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      success(result) {
        resolve(result.tempFilePaths?.[0] || null)
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function chooseBrowserImageOnce() {
  return new Promise<string | null>((resolve, reject) => {
    if (typeof document === 'undefined') {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/*'
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)

    input.onchange = () => {
      const file = input.files?.[0]
      const filePath = file ? URL.createObjectURL(file) : null
      input.remove()
      resolve(filePath)
    }
    input.onerror = (event) => {
      input.remove()
      reject(event)
    }
    input.click()
  })
}

function showNotice(message: string) {
  notice.value = message
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 1800
  })
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: 100vh;
  background: linear-gradient(180deg, #b7efb0 0%, #d7f3c3 18%, #d6ecbb 44%, #d8efc6 100%);
  overflow: hidden;
  width: 100%;
}

.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(90, 123, 89, 0.2);
  backdrop-filter: blur(10px);
  gap: 12px;
}

.header-left {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #333333;
  cursor: pointer;
  flex-shrink: 0;
}

.header-avatar,
.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  flex-shrink: 0;
}

.msg-avatar {
  width: 36px;
  height: 36px;
}

.header-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.header-name {
  font-size: 17px;
  font-weight: 800;
  color: #243329;
  line-height: 1.2;
}

.header-note {
  max-width: 48vw;
  margin-top: 2px;
  font-size: 12px;
  color: #687b68;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-status {
  margin-top: 2px;
  font-size: 12px;
  color: #00a884;
}

.header-status.offline {
  color: #8b9489;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.action-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(0, 168, 132, 0.12);
  color: #00795f;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.message-area {
  flex: 1;
  min-height: 0;
  background: transparent;
  padding: 16px 22px;
  box-sizing: border-box;
  overflow-y: auto;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-divider {
  text-align: center;
  margin: 12px 0;
}

.date-divider text {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #7d8b79;
  font-size: 12px;
  font-weight: 700;
}

.message-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-width: 0;
}

.message-wrapper.mine {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(68%, 520px);
  min-width: 0;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
}

.message-wrapper.theirs .message-bubble {
  background: rgba(255, 255, 255, 0.92);
  border-top-left-radius: 2px;
}

.message-wrapper.mine .message-bubble {
  background: linear-gradient(180deg, #efffd1, #f5ffd9);
  border-top-right-radius: 2px;
}

.system-bubble {
  max-width: 80%;
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.1) !important;
  text-align: center;
  font-size: 12px;
  color: #777777;
  border-radius: 4px !important;
  box-shadow: none;
}

.message-text {
  font-size: 15px;
  line-height: 1.5;
  color: #333333;
}

.message-img {
  width: 220px;
  max-width: 58vw;
  height: 160px;
  border-radius: 4px;
  display: block;
  object-fit: cover;
}

.voice-chip {
  min-width: 140px;
  color: #243329;
  font-size: 14px;
  font-weight: 700;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  justify-content: flex-end;
}

.msg-time,
.msg-status {
  font-size: 11px;
  color: #999999;
}

.msg-status {
  color: #07c160;
}

.video-call-card {
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.video-call-title {
  font-size: 15px;
  font-weight: 800;
  color: #243329;
}

.video-call-room {
  font-size: 12px;
  color: #6f8069;
}

.video-call-status {
  width: fit-content;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(0, 168, 132, 0.12);
  color: #00795f;
  font-size: 12px;
  font-weight: 800;
}

.video-call-status.declined,
.video-call-status.ended,
.video-call-status.expired {
  background: rgba(255, 94, 87, 0.12);
  color: #e04841;
}

.video-call-status.ringing {
  background: rgba(255, 196, 77, 0.18);
  color: #a06600;
}

.video-call-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.video-call-btn {
  min-width: 76px;
  height: 32px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  line-height: 32px;
}

.video-call-btn.answer,
.video-call-btn.enter {
  background: #00a884;
}

.video-call-btn.decline {
  background: #ff5e57;
}

.image-preview-bar {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid rgba(90, 123, 89, 0.2);
  gap: 12px;
}

.preview-thumb {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  object-fit: cover;
}

.preview-text {
  flex: 1;
  font-size: 14px;
  color: #666666;
}

.preview-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  border-radius: 50%;
  background: #f0f0f0;
}

.input-area {
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid rgba(90, 123, 89, 0.2);
  padding: 8px 16px 10px;
  backdrop-filter: blur(10px);
}

.input-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.tool-btn {
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 4px;
  color: #355140;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.tool-btn:hover {
  background: #f0f0f0;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.message-input {
  flex: 1;
  min-width: 0;
  padding: 10px 14px;
  border: 1px solid rgba(90, 123, 89, 0.25);
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
}

.send-btn {
  padding: 10px 24px;
  background: rgba(0, 168, 132, 0.2);
  color: #6f8069;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn.active {
  background: #00a884;
  color: #ffffff;
}

.incoming-call-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 28, 23, 0.48);
  box-sizing: border-box;
}

.incoming-call-dialog {
  width: min(340px, 100%);
  padding: 20px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 46px rgba(20, 36, 29, 0.24);
  box-sizing: border-box;
}

.incoming-call-title,
.incoming-call-copy {
  display: block;
}

.incoming-call-title {
  font-size: 18px;
  font-weight: 800;
  color: #1f3328;
}

.incoming-call-copy {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.45;
  color: #53645b;
  word-break: break-word;
}

.incoming-call-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.incoming-call-btn {
  flex: 1;
  min-width: 0;
  height: 40px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  line-height: 40px;
}

.incoming-call-btn.answer {
  background: #00a884;
}

.incoming-call-btn.decline {
  background: #ff5e57;
}

.notice-text {
  position: fixed;
  left: 50%;
  bottom: 92px;
  transform: translateX(-50%);
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(36, 51, 41, 0.86);
  color: #ffffff;
  font-size: 12px;
  z-index: 20;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 10px 12px;
  }

  .header-note {
    max-width: 42vw;
  }

  .message-area {
    padding: 12px 14px;
  }

  .message-bubble {
    max-width: 76%;
  }

  .msg-avatar {
    width: 32px;
    height: 32px;
  }

  .input-area {
    padding: 8px 12px 10px;
  }

  .send-btn {
    padding: 10px 18px;
  }
}
</style>

