<template>
  <view class="chat-container">
    <view class="chat-main">
      <view class="chat-header">
        <view class="header-left">
          <view class="back-btn" @click="goHome">
            <text>&lt;</text>
          </view>
          <image class="header-avatar" :src="supportAvatar" mode="aspectFill" />
          <view class="header-info">
            <text class="header-name">{{ headerTitle }}</text>
            <text class="header-note">{{ headerSubtitle }}</text>
            <text :class="['header-status', { offline: socketStatus !== 'online' }]">{{ socketStatusLabel }}</text>
          </view>
        </view>
        <view class="header-actions">
          <view class="action-btn delete-action" title="Delete chat" @click="hideCurrentConversation">
            <text>Del</text>
          </view>
          <view class="video-action" title="Video call" @click="startVideoCall">
            <view class="action-btn icon-action">
              <text class="icon-video"></text>
            </view>
            <text class="video-action-label">Video</text>
          </view>
        </view>
      </view>

      <scroll-view scroll-y class="message-area" :scroll-into-view="messageScrollTarget" @click="closeChatPanels">
        <view class="message-list">
          <view class="date-divider">
            <text>Today</text>
          </view>

          <view
            v-for="message in conversation"
            :key="message.id"
            :class="['message-wrapper', isMine(message) ? 'mine' : 'theirs']"
          >
            <ChatMessageBubble
              :message="message"
              :mine="isMine(message)"
              :avatar-src="isMine(message) ? currentUserAvatar : supportAvatar"
              :call-title="videoCallTitle(message)"
              :call-room="videoCallRoom(message)"
              :call-status="videoCallStatus(message)"
              :call-status-label="videoCallStatusLabel(message)"
              :call-caption="videoCallCaption(message)"
              :can-answer-call="canAnswerVideoMessage(message)"
              :can-reject-call="canRejectVideoMessage(message)"
              :can-enter-call="canEnterVideoMessage(message)"
              @preview="previewImage"
              @play-voice="playVoice"
              @answer-call="answerVideoMessage"
              @reject-call="rejectVideoMessage"
              @enter-call="enterVideoMessage"
              @message-menu="openMessageMenu"
            />
          </view>

          <view id="msg-bottom"></view>
        </view>
      </scroll-view>

      <ComposerAttachmentPreview
        :attachment="activeAttachment"
        @retry="sendPendingAttachment"
        @clear="clearAttachment"
      />

      <view :class="['input-area', showComposerTools && 'tools-open']">
        <view v-if="replyTarget" class="reply-composer">
          <view class="reply-composer-body">
            <text class="reply-composer-label">Replying to</text>
            <text class="reply-composer-text">{{ replyTargetText }}</text>
          </view>
          <view class="reply-composer-close" @click="clearReplyTarget">×</view>
        </view>
        <view class="input-row">
          <view class="composer-tools-wrap">
            <view :class="['composer-tool-main', showComposerTools && 'is-open']" title="Attachments" @click="toggleComposerTools">
              <text></text>
              <text></text>
            </view>
          </view>
          <input v-model="draft" class="message-input" maxlength="2000" placeholder="Type a message..." @focus="closeComposerTools" @confirm="handleSend" />
          <view class="send-btn" :class="{ active: canSend }" @click="handleSend">
            <text>Send</text>
          </view>
        </view>
        <text :class="['message-limit', draft.length >= 1800 && 'warning']">{{ draft.length }}/2000</text>
        <view v-if="showComposerTools" class="composer-panel">
          <view class="composer-panel-grid">
            <view class="composer-panel-item" @click="chooseComposerTool('image')">
              <text class="composer-panel-icon image"></text>
              <text>Image</text>
            </view>
            <view class="composer-panel-item" @click="chooseComposerTool('gif')">
              <text class="composer-panel-icon gif">GIF</text>
              <text>GIF</text>
            </view>
            <view class="composer-panel-item" @click="chooseComposerTool('video')">
              <text class="composer-panel-icon video"></text>
              <text>Video</text>
            </view>
          </view>
        </view>
      </view>

      <view
        v-if="messageContextMenu"
        class="message-menu-mask"
        @click="closeMessageMenu"
        @contextmenu.prevent.stop="closeMessageMenu"
      >
        <view class="message-context-menu" :style="messageMenuStyle" @click.stop @contextmenu.prevent.stop>
          <view class="message-context-item" @click="copyContextMessage">Copy</view>
          <view v-if="canQuoteMessage(messageContextMenu.message)" class="message-context-item" @click="quoteContextMessage">Reply</view>
          <view v-if="canHideMessage(messageContextMenu.message)" class="message-context-item danger" @click="hideContextMessage">Delete</view>
        </view>
      </view>
    </view>

    <AppNav current="chat" />

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
import AppNav from '@/components/AppNav.vue'
import ChatMessageBubble from '@/components/chat/ChatMessageBubble.vue'
import ComposerAttachmentPreview from '@/components/chat/ComposerAttachmentPreview.vue'
import { useComposerAttachments, type ComposerAttachmentKind } from '@/components/chat/useComposerAttachments'
import { fetchVideoSessionBootstrap, uploadImage, uploadVideo } from '@/utils/api'
import { connectChatSocket } from '@/utils/realtime'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { uiIcons } from '@/utils/art'
import type { ChatMessage, ChatReadReceiptEvent, ChatRealtimePayload, PresenceEvent, VideoCallMessagePayload, VideoInviteEvent, VideoSessionItem, VideoSessionStatusEvent } from '@/types'

const store = useAppStore()
const supportAvatar = '/static/pwa/icons/xcard-192.png'
const draft = ref('')
const notice = ref('')
const socketTask = ref<UniApp.SocketTask | null>(null)
const readRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const socketStatus = ref<'connecting' | 'online' | 'offline'>('connecting')
const showComposerTools = ref(false)
const {
  activeAttachment,
  hasAttachment,
  isUploading: isAttachmentUploading,
  addFile,
  addPath,
  clearAttachment,
  setStatus
} = useComposerAttachments()
const messageScrollTarget = ref('msg-bottom')
const audioEnabled = ref(false)
const handledVideoInvites = new Set<string>()
const localVideoStatuses = ref<Record<string, VideoSessionItem['status']>>({})
const incomingVideoInvite = ref<VideoInviteEvent | null>(null)
const replyTarget = ref<ChatMessage['replyTo'] | null>(null)
const messageContextMenu = ref<{ message: ChatMessage; x: number; y: number } | null>(null)
const lastContextMenuPoint = ref<{ clientX: number; clientY: number; time: number } | null>(null)

const conversation = computed(() => store.state.supportMessages)
const isAgent = computed(() => store.state.currentUser?.roleCode === 'AGENT' || store.state.currentUser?.roleCode === 'ADMIN')
const activeConversation = computed(() =>
  store.state.supportConversations.find((item) => item.conversationId === store.state.supportConversationId) || null
)
const headerTitle = computed(() => isAgent.value ? (activeConversation.value?.customerUsername || 'Customer chat') : 'Dedicated Support')
const headerSubtitle = computed(() => isAgent.value ? 'tap to switch customer' : (activeConversation.value?.assignedAgent || 'assigned support'))
const heroLabel = computed(() => isAgent.value ? 'Customer conversation' : 'Dedicated support')
const heroCopy = computed(() => isAgent.value ? 'Reply to this customer here.' : 'Your support agent will reply in this chat.')
const balanceSummary = computed(() => store.state.balanceSummary)
const canSend = computed(() => Boolean((draft.value.trim() && draft.value.length <= 2000) || hasAttachment.value))
const replyTargetText = computed(() => previewMessageContent(replyTarget.value?.content || ''))
const messageMenuStyle = computed(() => {
  const menu = messageContextMenu.value
  if (!menu) return ''
  return `left:${menu.x}px;top:${menu.y}px;`
})
const socketStatusLabel = computed(() => ({
  connecting: 'connecting',
  online: 'online',
  offline: 'reconnecting'
})[socketStatus.value])
const currentUserAvatar = computed(() =>
  store.state.currentUser?.avatarUrl ? resolveMediaUrl(store.state.currentUser.avatarUrl) : uiIcons.user
)

onShow(() => {
  store.bootstrap().then(async () => {
    if (store.state.currentUser?.roleCode === 'AGENT' || store.state.currentUser?.roleCode === 'ADMIN') {
      uni.redirectTo({ url: '/pages/support-chat-v2/index' })
      return
    }
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
  detachContextMenuPointListener()
})

onMounted(() => {
  attachPasteListener()
  attachContextMenuPointListener()
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

function previewMessageContent(content: string) {
  const normalized = (content || '').trim().replace(/\s+/g, ' ')
  if (!normalized) return ''
  return normalized.length > 80 ? `${normalized.slice(0, 80)}...` : normalized
}

function copyableMessageContent(message: ChatMessage) {
  if (message.type === 'image') return '[Image]'
  if (message.type === 'gif') return '[GIF]'
  if (message.type === 'voice') return '[Voice]'
  if (message.type === 'video') return isVideoFileMessage(message) ? '[Video]' : '[Video call]'
  if (message.type === 'order') return `[Order] ${message.order?.orderNo || ''}`.trim()
  return message.content || ''
}

function clipboardMessageContent(message: ChatMessage) {
  return message.type === 'text' ? message.content || '' : copyableMessageContent(message)
}

function closeMessageMenu() {
  messageContextMenu.value = null
}

function clearReplyTarget() {
  replyTarget.value = null
}

function canQuoteMessage(message: ChatMessage) {
  return message.author !== 'system' && !message.id.startsWith('local-')
}

function canHideMessage(message: ChatMessage) {
  return message.author !== 'system' && !message.id.startsWith('local-')
}

function rememberContextMenuPoint(event: MouseEvent) {
  lastContextMenuPoint.value = {
    clientX: event.clientX,
    clientY: event.clientY,
    time: Date.now()
  }
}

function attachContextMenuPointListener() {
  // #ifdef H5
  window.addEventListener('contextmenu', rememberContextMenuPoint, true)
  // #endif
}

function detachContextMenuPointListener() {
  // #ifdef H5
  window.removeEventListener('contextmenu', rememberContextMenuPoint, true)
  // #endif
}

function resolveContextMenuPoint(point?: { clientX: number; clientY: number }) {
  const recent = lastContextMenuPoint.value
  if (recent && Date.now() - recent.time < 250) {
    return recent
  }
  return point
}

function openMessageMenu(message: ChatMessage, point?: { clientX: number; clientY: number }) {
  showComposerTools.value = false
  let x = 24
  let y = 120
  // #ifdef H5
  const menuWidth = 118
  const menuHeight = canQuoteMessage(message) ? 126 : 86
  const menuPoint = resolveContextMenuPoint(point)
  const clientX = menuPoint?.clientX || 0
  const clientY = menuPoint?.clientY || 0
  x = Math.max(12, Math.min(clientX || 24, window.innerWidth - menuWidth - 12))
  y = Math.max(12, Math.min(clientY || 120, window.innerHeight - menuHeight - 12))
  // #endif
  messageContextMenu.value = { message, x, y }
}

function copyContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message) return
  const data = clipboardMessageContent(message)
  if (!data) return
  uni.setClipboardData({
    data,
    success() {
      showNotice('Copied.')
    }
  })
}

function quoteContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message || !canQuoteMessage(message)) return
  replyTarget.value = {
    messageId: message.id,
    author: message.author,
    content: copyableMessageContent(message)
  }
}

function hideContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message || !canHideMessage(message)) return
  uni.showModal({
    title: 'Delete message',
    content: 'This only hides the message from your chat. Support and admin records are kept.',
    confirmText: 'Delete',
    confirmColor: '#d64242',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.hideRecord({
          targetType: 'MESSAGE',
          targetId: message.id,
          hiddenScope: 'SINGLE'
        })
        showNotice('Deleted from your chat.')
      } catch (error) {
        showNotice(error instanceof Error ? error.message : 'Delete failed')
      }
    }
  })
}

function hideCurrentConversation() {
  const conversationId = store.state.supportConversationId
  if (!conversationId) return
  uni.showModal({
    title: 'Delete chat',
    content: 'This only hides the chat from your account. The platform still keeps the full service record.',
    confirmText: 'Delete',
    confirmColor: '#d64242',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.hideRecord({
          targetType: 'CONVERSATION',
          targetId: conversationId,
          hiddenScope: 'CONVERSATION'
        })
        closeSocket()
        showNotice('Chat hidden from your account.')
      } catch (error) {
        showNotice(error instanceof Error ? error.message : 'Delete failed')
      }
    }
  })
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
  if (last.type === 'gif') return 'GIF'
  if (last.type === 'voice') return 'Voice'
  if (last.type === 'video') return isVideoFileMessage(last) ? 'Video' : 'Video call'
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

function videoCallCaption(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  if (!payload) return 'This video call invite cannot be opened. Refresh and try again.'
  if (status === 'active') return 'The call is active. You can re-enter the same room.'
  if (status === 'created') return 'Waiting for the other side to answer.'
  if (status === 'joining') return 'The other side is joining the call.'
  if (status === 'missed') return 'This call was missed. Start a new call if needed.'
  if (status === 'rejected') return 'This call was declined.'
  return 'This video call has ended.'
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
  if (pendingDraft && !draft.value.trim()) {
    draft.value = pendingDraft
  }
  uni.removeStorageSync('pending-support-draft')
}

async function handlePasteImage(event: ClipboardEvent) {
  const item = Array.from(event.clipboardData?.items || []).find((entry) => entry.type.startsWith('image/'))
  const file = item?.getAsFile()
  if (!file) return

  event.preventDefault()
  try {
    addFile(file)
    showNotice('Image pasted. Click Send to send it.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Unsupported image')
  }
}

async function sendText(content: string) {
  const value = content.trim()
  if (!value) return

  try {
    const replyTo = replyTarget.value || undefined
    await store.sendSupport(value, 'text', replyTo)
    startReadRefresh()
    draft.value = ''
    clearReplyTarget()
    showNotice('Message sent.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Send failed')
  }
}

function handleSend() {
  enableAudio()
  showComposerTools.value = false
  closeMessageMenu()
  if (activeAttachment.value) {
    sendPendingAttachment()
    return
  }
  const value = draft.value.trim()
  if (!value) return
  if (value.length > 2000) {
    showNotice('Text messages are limited to 2,000 characters.')
    return
  }
  sendText(value)
}

function toggleComposerTools() {
  showComposerTools.value = !showComposerTools.value
  if (showComposerTools.value) {
    nextTick(() => scrollMessagesToBottom())
  }
}

function closeComposerTools() {
  showComposerTools.value = false
}

function closeChatPanels() {
  closeMessageMenu()
  closeComposerTools()
}

function chooseComposerTool(action: 'image' | 'gif' | 'video') {
  showComposerTools.value = false
  if (action === 'image') {
    sendImage()
    return
  }
  if (action === 'gif') {
    sendGif()
    return
  }
  sendVideo()
}

async function sendPendingAttachment() {
  const attachment = activeAttachment.value
  if (!attachment || isAttachmentUploading.value) return
  setStatus(attachment.id, 'uploading')
  try {
    const source = attachment.file || attachment.url
    const asset = attachment.kind === 'video' ? await uploadVideo(source) : await uploadImage(source)
    const replyTo = replyTarget.value || undefined
    await store.sendSupport(asset.publicUrl, attachment.kind, replyTo)
    startReadRefresh()
    clearReplyTarget()
    clearAttachment(attachment.id)
    if (draft.value.trim()) {
      await sendText(draft.value)
    }
    showNotice(attachment.kind === 'gif' ? 'GIF sent.' : 'Image sent.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Send failed'
    setStatus(attachment.id, 'failed', message)
    showNotice(message)
  }
}

function useQuickQuestion(question: string) {
  draft.value = question
  sendText(question)
}

async function sendImage() {
  try {
    const filePath = await chooseImageOnce('image')
    if (!filePath) return
    addPath(filePath, 'image')
    showNotice('Image ready. Click Send to send it.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Image select failed')
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
    const filePath = await chooseImageOnce('gif')
    if (!filePath) return
    addPath(filePath, 'gif')
    showNotice('GIF ready. Click Send to send it.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'GIF select failed')
  }
}

async function sendVideo() {
  try {
    if (typeof document !== 'undefined') {
      const file = await chooseBrowserVideoOnce()
      if (file) addFile(file)
      return
    }
    const filePath = await chooseNativeVideoOnce()
    if (!filePath) return
    addPath(filePath, 'video')
    showNotice('Video ready. Click Send to send it.')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'Video select failed')
  }
}

function showTools() {
  showNotice('Use Image, GIF, Video, or the quick actions below.')
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
  const resolved = resolveMediaUrl(url)
  uni.previewImage({
    urls: [resolved],
    current: resolved
  })
}

function playVoice(url: string) {
  const audio = uni.createInnerAudioContext()
  audio.src = resolveMediaUrl(url)
  audio.play()
}

function goHome() {
  uni.redirectTo({ url: '/pages/home/index' })
}

function chooseImageOnce(kind: ComposerAttachmentKind) {
  // #ifdef H5
  return chooseBrowserImageOnce(kind)
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

function chooseBrowserImageOnce(kind: ComposerAttachmentKind) {
  return new Promise<string | null>((resolve, reject) => {
    if (typeof document === 'undefined') {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = kind === 'gif' ? 'image/gif' : 'image/*'
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

function chooseNativeVideoOnce() {
  return new Promise<string | null>((resolve, reject) => {
    uni.chooseVideo({
      sourceType: ['album', 'camera'],
      compressed: true,
      maxDuration: 60,
      success(result) {
        resolve(result.tempFilePath || null)
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function chooseBrowserVideoOnce() {
  return new Promise<File | null>((resolve, reject) => {
    if (typeof document === 'undefined') {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'video/mp4,video/webm,video/quicktime,.mov'
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)

    input.onchange = () => {
      const file = input.files?.[0] || null
      input.remove()
      resolve(file)
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

function isVideoFileMessage(message: ChatMessage) {
  return message.attachments?.some(attachment => attachment.type === 'video') || false
}
</script>

<style scoped lang="scss">
.chat-container {
  --chat-doodle-pattern: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='220' height='220' viewBox='0 0 220 220'%3E%3Cg fill='none' stroke='%23606C38' stroke-opacity='.18' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 28c10-12 27-6 27 8 0 13-17 21-27 7-4-6-4-10 0-15Z'/%3E%3Cpath d='M78 18h22v18H78zM85 18v-6M93 18v-6M82 44h14'/%3E%3Cpath d='M142 22c14 2 24 13 21 26-2 11-13 18-27 14-11-4-16-14-12-25 3-9 10-16 18-15Z'/%3E%3Cpath d='M35 92c18-5 35 4 42 21M24 110c18 12 38 15 60 8'/%3E%3Cpath d='M117 90l24 16-24 16zM151 94l12 8M151 118l12 8'/%3E%3Cpath d='M185 82c8 0 15 7 15 15s-7 15-15 15-15-7-15-15 7-15 15-15Z'/%3E%3Cpath d='M31 169c11-13 31-12 42 2M34 188c13 8 26 8 39 0'/%3E%3Cpath d='M106 162c11-9 29-7 36 5 7 13 0 27-14 29-14 2-25-6-25-18 0-7 1-12 3-16Z'/%3E%3Cpath d='M172 164h24v24h-24zM178 170h12M178 176h12M178 182h8'/%3E%3Cpath d='M62 57l11 11M73 57 62 68M198 30l8 8M206 30l-8 8M92 136l9 9M101 136l-9 9'/%3E%3C/g%3E%3C/svg%3E");
  display: flex;
  height: 100vh;
  min-height: 100vh;
  height: 100dvh;
  min-height: 100dvh;
  padding-top: constant(safe-area-inset-top);
  padding-top: env(safe-area-inset-top);
  padding-bottom: calc(118rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(118rpx + env(safe-area-inset-bottom));
  background: linear-gradient(145deg, #d8edbf 0%, #b9ddab 42%, #85bea9 100%);
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(145deg, #d8edbf 0%, #b9ddab 42%, #85bea9 100%);
  backdrop-filter: blur(10px);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(136, 153, 166, 0.22);
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
  color: #008b5c;
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

.video-action {
  width: 42px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  color: #0088cc;
  cursor: pointer;
  flex-shrink: 0;
}

.video-action:hover .action-btn {
  background: rgba(0, 136, 204, 0.16);
}

.video-action-label {
  color: #50695b;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.action-btn {
  width: 38px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 136, 204, 0.1);
  color: #0088cc;
  cursor: pointer;
}

.action-btn:hover {
  background: rgba(0, 136, 204, 0.16);
}

.delete-action {
  width: auto;
  min-width: 44px;
  padding: 0 10px;
  border-radius: 7px;
  background: rgba(214, 66, 66, 0.1);
  color: #c93636;
  font-size: 12px;
  font-weight: 900;
}

.delete-action:hover {
  background: rgba(214, 66, 66, 0.16);
}

.icon-video {
  position: relative;
  width: 21px;
  height: 15px;
  border: 2px solid currentColor;
  border-radius: 5px;
  background: transparent;
  box-sizing: border-box;
}

.icon-video::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 4px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.icon-video::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 3px;
  width: 8px;
  height: 7px;
  border-radius: 1px 4px 4px 1px;
  background: currentColor;
  clip-path: polygon(0 18%, 100% 0, 100% 100%, 0 82%);
}

.message-area {
  flex: 1;
  min-height: 0;
  background-image:
    var(--chat-doodle-pattern),
    radial-gradient(circle at 0% 8%, rgba(224, 239, 153, 0.62) 0, rgba(224, 239, 153, 0) 34%),
    linear-gradient(145deg, rgba(216, 237, 191, 0.96) 0%, rgba(185, 221, 171, 0.96) 42%, rgba(133, 190, 169, 0.96) 100%);
  background-size: 220px 220px, cover, cover;
  background-position: center;
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
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  color: #51606d;
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
  background: #ffffff;
  border-top: 1px solid rgba(136, 153, 166, 0.22);
  padding: 8px 16px 10px;
  backdrop-filter: blur(10px);
  flex-shrink: 0;
  transition: padding 0.18s ease, box-shadow 0.18s ease;
}

.input-area.tools-open {
  box-shadow: 0 -14px 32px rgba(25, 42, 62, 0.1);
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.composer-tools-wrap {
  position: relative;
  flex-shrink: 0;
}

.composer-tool-main {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(23, 33, 43, 0.06);
  color: #51606d;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.composer-tool-main:hover,
.composer-tool-main.is-open {
  background: rgba(0, 136, 204, 0.1);
  color: #0088cc;
}

.composer-tool-main:active {
  transform: scale(0.96);
}

.composer-tool-main text:first-child,
.composer-tool-main text:last-child {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 16px;
  height: 2px;
  border-radius: 2px;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.composer-tool-main text:last-child {
  transform: translate(-50%, -50%) rotate(90deg);
}

.composer-panel {
  display: block;
  min-height: 132px;
  margin: 10px -16px -10px;
  padding: 14px 24px 18px;
  background: #f7f9fb;
  border-top: 1px solid rgba(136, 153, 166, 0.18);
  box-sizing: border-box;
}

.composer-panel-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.composer-panel-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #263642;
  font-size: 12px;
  font-weight: 800;
}

.composer-panel-icon {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #f0f6f8;
  color: #0088cc;
  box-shadow: inset 0 0 0 1px rgba(0, 136, 204, 0.08);
}

.composer-panel-icon.image::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 16px;
  width: 24px;
  height: 18px;
  border: 2px solid currentColor;
  border-radius: 5px;
  box-sizing: border-box;
}

.composer-panel-icon.image::after {
  content: '';
  position: absolute;
  left: 19px;
  bottom: 16px;
  width: 18px;
  height: 11px;
  background: linear-gradient(135deg, transparent 0 42%, currentColor 43% 57%, transparent 58%);
}

.composer-panel-icon.gif {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
}

.composer-panel-icon.video::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 18px;
  width: 22px;
  height: 16px;
  border: 2px solid currentColor;
  border-radius: 5px;
  box-sizing: border-box;
}

.composer-panel-icon.video::after {
  content: '';
  position: absolute;
  right: 13px;
  top: 21px;
  border-left: 9px solid currentColor;
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
}

.reply-composer {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin-bottom: 8px;
  padding: 8px 10px;
  border-left: 3px solid #0088cc;
  border-radius: 8px;
  background: rgba(225, 244, 255, 0.94);
  box-sizing: border-box;
}

.reply-composer-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.reply-composer-label {
  color: #0071a8;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.3;
}

.reply-composer-text {
  margin-top: 2px;
  color: #344856;
  font-size: 13px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-composer-close {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  color: #42515c;
  font-size: 20px;
  line-height: 26px;
  text-align: center;
  cursor: pointer;
}

.message-menu-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: transparent;
}

.message-context-menu {
  position: fixed;
  min-width: 112px;
  padding: 6px;
  border: 1px solid rgba(38, 59, 48, 0.08);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(23, 40, 31, 0.18);
  box-sizing: border-box;
}

.message-context-item {
  height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  color: #22332a;
  font-size: 14px;
  font-weight: 700;
  line-height: 36px;
  cursor: pointer;
  box-sizing: border-box;
}

.message-context-item:hover {
  background: rgba(0, 136, 204, 0.1);
}

.message-limit {
  display: block;
  margin-top: 4px;
  padding-right: 58px;
  color: #7a8792;
  font-size: 11px;
  line-height: 16px;
  text-align: right;
}

.message-limit.warning {
  color: #c96c18;
}

.message-context-item.danger {
  color: #c93636;
}

.message-context-item.danger:hover {
  background: rgba(214, 66, 66, 0.1);
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
  flex-shrink: 0;
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
  .chat-container {
    padding-bottom: calc(118rpx + constant(safe-area-inset-bottom));
    padding-bottom: calc(118rpx + env(safe-area-inset-bottom));
  }

  .header-left {
    flex: 1;
    min-width: 0;
  }

  .chat-header {
    padding: 10px 12px;
    gap: 8px;
  }

  .header-actions {
    gap: 8px;
  }

  .header-name {
    font-size: 15px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .header-note {
    max-width: 56vw;
  }

  .message-area {
    padding: 12px 14px;
  }

  .msg-avatar {
    width: 32px;
    height: 32px;
  }

  .input-area {
    padding: 8px 10px calc(10px + env(safe-area-inset-bottom));
  }

  .input-row {
    gap: 8px;
  }

  .composer-tool-main {
    width: 36px;
    height: 36px;
  }

  .composer-panel {
    min-height: 132px;
    margin: 10px -10px calc(-10px - env(safe-area-inset-bottom));
    padding: 14px 18px calc(18px + env(safe-area-inset-bottom));
  }

  .composer-panel-icon {
    width: 50px;
    height: 50px;
  }

  .message-input {
    padding: 9px 10px;
    font-size: 14px;
  }

  .send-btn {
    padding: 9px 14px;
    font-size: 13px;
  }
}

@media (max-width: 380px) {
  .chat-container {
    padding-bottom: calc(112rpx + constant(safe-area-inset-bottom));
    padding-bottom: calc(112rpx + env(safe-area-inset-bottom));
  }
}
</style>
