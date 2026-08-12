import { useAppStore } from '@/store/app'
import type { ChatMessage, ChatReadReceiptEvent, ChatRealtimePayload, PresenceEvent } from '@/types'
import {
  installMessageAudioUnlock,
  notifyIncomingCustomerMessage,
  playMessageNotificationSound,
  setAppUnreadBadge
} from '@/utils/messageNotifications'
import { connectChatSocket } from '@/utils/realtime'

const NOTIFICATION_THROTTLE_MS = 1500

let socketTask: UniApp.SocketTask | null = null
let socketConversationId = ''
let suspended = false
let initialized = false
let lastNotificationAt = 0

export function initializeGlobalSupportNotifications() {
  if (!initialized) {
    initialized = true
    installMessageAudioUnlock()
  }
  syncGlobalSupportNotifications()
}

export function setGlobalSupportNotificationsSuspended(nextSuspended: boolean) {
  suspended = nextSuspended
  syncGlobalSupportNotifications()
}

export function syncGlobalSupportNotifications() {
  const store = useAppStore()
  const user = store.state.currentUser
  const conversationId = store.state.supportConversationId
  const conversationReady = store.state.supportConversations.some((item) => item.conversationId === conversationId)

  if (suspended || user?.roleCode !== 'USER' || !user.accessToken || !conversationId || !conversationReady) {
    closeGlobalSocket()
    return
  }
  if (socketTask && socketConversationId === conversationId) return

  closeGlobalSocket()
  socketConversationId = conversationId
  socketTask = connectChatSocket('support', conversationId, (payload) => {
    handlePayload(payload, conversationId)
  }, {
    onOpen: () => {
      store.refreshSupport()
        .then(() => setAppUnreadBadge(store.state.supportUnreadCount))
        .catch(() => {})
    },
    onReconnect: () => {
      store.refreshSupport()
        .then(() => setAppUnreadBadge(store.state.supportUnreadCount))
        .catch(() => {})
    }
  })
}

export function stopGlobalSupportNotifications() {
  suspended = false
  closeGlobalSocket()
  setAppUnreadBadge(0)
}

function handlePayload(payload: ChatRealtimePayload, conversationId: string) {
  const store = useAppStore()
  if (isReadReceipt(payload)) {
    if (payload.readerUsername === store.state.currentUser?.username) {
      store.clearSupportUnread(conversationId)
      setAppUnreadBadge(store.state.supportUnreadCount)
    } else {
      store.applySupportReadReceipt(conversationId)
    }
    return
  }
  if (isPresenceEvent(payload)) {
    store.applySupportPresence(payload.channelId, payload.online)
    return
  }
  if (isEventPayload(payload)) return

  const message = payload as ChatMessage
  if (message.author === 'me' || message.author === 'system') return
  const inserted = store.pushSupportRealtime(message, conversationId, { markIncomingUnread: true })
  if (!inserted) return

  setAppUnreadBadge(store.state.supportUnreadCount)
  const now = Date.now()
  if (now - lastNotificationAt < NOTIFICATION_THROTTLE_MS) return
  lastNotificationAt = now
  playMessageNotificationSound()
  notifyIncomingCustomerMessage(conversationId)
}

function closeGlobalSocket() {
  socketTask?.close({})
  socketTask = null
  socketConversationId = ''
}

function isEventPayload(payload: ChatRealtimePayload) {
  return 'eventType' in payload
}

function isReadReceipt(payload: ChatRealtimePayload): payload is ChatReadReceiptEvent {
  return 'eventType' in payload && payload.eventType === 'read'
}

function isPresenceEvent(payload: ChatRealtimePayload): payload is PresenceEvent {
  return 'eventType' in payload && payload.eventType === 'presence'
}
