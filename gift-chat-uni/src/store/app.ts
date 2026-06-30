import { reactive } from 'vue'
import type {
  BroadcastItem,
  BalanceSummary,
  ChatMessage,
  CountryOption,
  FriendProfile,
  FriendRequest,
  LoanApplicationItem,
  RankingBoard,
  RateItem,
  SearchFriendResult,
  SessionUser,
  SellOrderPayload,
  SupportConversationItem,
  SupportCustomerProfile,
  SupportLedgerReport,
  TransactionItem,
  VideoSessionBootstrap,
  VideoSessionStatusEvent,
  VideoSessionItem,
  WithdrawalItem
} from '@/types'
import {
  acceptFriendRequest,
  addBlacklist,
  markFriendConversationRead,
  markSupportConversationRead,
  updateSupportConversationNote,
  removeBlacklist,
  removeFriend,
  createBroadcast as createBroadcastRequest,
  createFriendRequest,
  createLoanApplication as createLoanApplicationRequest,
  createRate as createRateRequest,
  createSellOrder as createSellOrderRequest,
  createVideoSession as createVideoSessionRequest,
  deleteRate as deleteRateRequest,
  createTransaction as createTransactionRequest,
  createWithdrawal as createWithdrawalRequest,
  fetchBlacklist,
  fetchBalanceSummary,
  fetchBroadcasts,
  fetchFriendRequests,
  fetchFriends,
  fetchLoans,
  fetchRanking,
  fetchRates,
  fetchSupportCustomerProfile,
  fetchSupportLedger,
  fetchSupportMessages,
  fetchTransactions,
  fetchVideoSessionBootstrap,
  fetchVideoSessions,
  fetchWithdrawals,
  getStoredSessionUser,
  loginWithPassword,
  logoutSession,
  registerAccount,
  rejectFriendRequest,
  searchFriends,
  setSessionToken,
  setStoredSessionUser,
  sendDirectMessage,
  sendSupportMessage,
  syncFriendMessages,
  syncSupportMessages,
  updateLoanStatus as updateLoanStatusRequest,
  updateRateStatus as updateRateStatusRequest,
  updateRate as updateRateRequest,
  updateTransactionStatus as updateTransactionStatusRequest,
  updateVideoSessionStatus as updateVideoSessionStatusRequest,
  updateWithdrawalStatus as updateWithdrawalStatusRequest
} from '@/utils/api'
import { closeAllChatSockets } from '@/utils/realtime'
import { safeRouteForRole } from '@/utils/routeGuard'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const state = reactive({
  currentUser: getInitialUser(),
  countries: [
    { code: 'NG', name: 'Nigeria' },
    { code: 'IN', name: 'India' },
    { code: 'CM', name: 'Cameroon' },
    { code: 'GH', name: 'Ghana' }
  ] as CountryOption[],
  selectedCountryCode: 'NG',
  rates: [] as RateItem[],
  friends: [] as FriendProfile[],
  friendRequests: [] as FriendRequest[],
  friendSearchResults: [] as SearchFriendResult[],
  blacklist: [] as FriendProfile[],
  supportMessages: [] as ChatMessage[],
  supportUnreadCount: 0,
  supportConversations: [] as SupportConversationItem[],
  transactions: [] as TransactionItem[],
  withdrawals: [] as WithdrawalItem[],
  loans: [] as LoanApplicationItem[],
  broadcasts: [] as BroadcastItem[],
  balanceSummary: null as BalanceSummary | null,
  activeSupportCustomerProfile: null as SupportCustomerProfile | null,
  supportCustomerProfileLoading: false,
  supportCustomerProfileCache: {} as Record<string, SupportCustomerProfile>,
  supportLedger: null as SupportLedgerReport | null,
  supportLedgerLoading: false,
  videoSessions: [] as VideoSessionItem[],
  ranking: null as RankingBoard | null,
  supportConversationId: 'support-1',
  activeFriendUsername: ''
})

const SUPPORT_READ_CACHE_KEY = 'support-read-message-cache'
const SUPPORT_MESSAGE_READ_STATE_KEY = 'support-message-read-state-cache'
const CHAT_CURSOR_CACHE_KEY = 'chat-message-cursor-cache'

type ChannelType = 'friend' | 'support'

function cursorKey(channelType: ChannelType, channelId: string) {
  return `${channelType}:${channelId}`
}

function getChatCursorCache() {
  return (uni.getStorageSync(CHAT_CURSOR_CACHE_KEY) || {}) as Record<string, number>
}

function getChatCursor(channelType: ChannelType, channelId: string) {
  return getChatCursorCache()[cursorKey(channelType, channelId)] || 0
}

function setChatCursor(channelType: ChannelType, channelId: string, seq?: number) {
  if (!channelId || !seq || seq <= 0) return
  const cache = getChatCursorCache()
  const key = cursorKey(channelType, channelId)
  cache[key] = Math.max(cache[key] || 0, seq)
  uni.setStorageSync(CHAT_CURSOR_CACHE_KEY, cache)
}

function rememberMessageCursor(channelType: ChannelType, channelId: string, message: ChatMessage) {
  setChatCursor(channelType, channelId, message.serverSeq)
}

function latestMessageSeq(messages: ChatMessage[]) {
  return messages.reduce((latest, message) => Math.max(latest, message.serverSeq || 0), 0)
}

function rememberSupportConversationCursors(conversations: SupportConversationItem[]) {
  conversations.forEach((conversation) => {
    setChatCursor('support', conversation.conversationId, latestMessageSeq(conversation.messages))
  })
}

function rememberFriendConversationCursors(friends: FriendProfile[]) {
  friends.forEach((friend) => {
    setChatCursor('friend', friend.id, latestMessageSeq(friend.messages))
  })
}

function applyReadSeq(messages: ChatMessage[], readSeq: number) {
  if (!readSeq || readSeq <= 0) return
  messages.forEach((message) => {
    if (message.author === 'me' && (message.serverSeq || 0) > 0 && (message.serverSeq || 0) <= readSeq) {
      message.readState = 'read'
    }
  })
}

function normalizeMediaMessageContent(message: ChatMessage) {
  return message.type === 'image' || message.type === 'gif' || message.type === 'voice'
    ? resolveMediaUrl(message.content)
    : message.content
}

function normalizeChatMessage(message: ChatMessage) {
  return {
    ...message,
    content: normalizeMediaMessageContent(message),
    attachments: message.attachments?.map((attachment) => ({
      ...attachment,
      url: resolveMediaUrl(attachment.url),
      thumbnailUrl: attachment.thumbnailUrl ? resolveMediaUrl(attachment.thumbnailUrl) : attachment.thumbnailUrl
    }))
  } as ChatMessage
}

function normalizeChatMessages(messages: ChatMessage[]) {
  return messages.map(normalizeChatMessage)
}

function normalizeSupportConversation(conversation: SupportConversationItem) {
  return {
    ...conversation,
    messages: normalizeChatMessages(conversation.messages)
  }
}

function normalizeFriendProfile(friend: FriendProfile) {
  return {
    ...friend,
    messages: normalizeChatMessages(friend.messages)
  }
}

function getInitialUser() {
  const storedUser = getStoredSessionUser()
  if (storedUser) {
    return storedUser
  }

  return null as SessionUser | null
}

function mergeUniqueMessage(messages: ChatMessage[], incomingMessage: ChatMessage) {
  const incoming = normalizeChatMessage(incomingMessage)
  if (messages.some((message) => message.id === incoming.id)) {
    return
  }
  const pendingIndex = messages.findIndex((message) =>
    message.id.startsWith('local-')
    && message.author === incoming.author
    && message.author === 'me'
    && (
      (!!message.clientMessageId && message.clientMessageId === incoming.clientMessageId)
      || (message.type === incoming.type && message.content === incoming.content)
    )
    && message.readState === 'sending'
  )
  if (pendingIndex >= 0) {
    messages.splice(pendingIndex, 1, incoming)
    return
  }
  messages.push(incoming)
}

function replaceMessage(messages: ChatMessage[], localId: string, message: ChatMessage) {
  const normalizedMessage = normalizeChatMessage(message)
  const localIndex = messages.findIndex((item) => item.id === localId)
  if (localIndex >= 0) {
    messages.splice(localIndex, 1, normalizedMessage)
    return
  }
  mergeUniqueMessage(messages, normalizedMessage)
}

function markMessageFailed(messages: ChatMessage[], localId: string) {
  const message = messages.find((item) => item.id === localId)
  if (message) {
    message.readState = 'failed'
  }
}

function makeClientMessageId() {
  return `cm-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function makeLocalMessage(content: string, type: ChatMessage['type'], clientMessageId = makeClientMessageId()) {
  return normalizeChatMessage({
    id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    author: 'me',
    content,
    type,
    createdAt: 'Sending',
    readState: 'sending',
    clientMessageId,
    deliveryStatus: 'pending'
  } as ChatMessage)
}

function getSupportMessageReadStateCache() {
  return (uni.getStorageSync(SUPPORT_MESSAGE_READ_STATE_KEY) || {}) as Record<string, string[]>
}

function setSupportMessageReadStateCache(conversationId: string, messageIds: string[]) {
  if (!conversationId || messageIds.length === 0) return
  const cache = getSupportMessageReadStateCache()
  cache[conversationId] = Array.from(new Set([...(cache[conversationId] || []), ...messageIds]))
  uni.setStorageSync(SUPPORT_MESSAGE_READ_STATE_KEY, cache)
}

function rememberReadSupportMessages(conversationId: string) {
  const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
  const ids = [
    ...(conversation?.messages || []),
    ...(conversationId === state.supportConversationId ? state.supportMessages : [])
  ]
    .filter((message) => message.author === 'me')
    .map((message) => message.id)
  setSupportMessageReadStateCache(conversationId, ids)
}

function applySupportMessageReadStateCache(conversations: SupportConversationItem[]) {
  const cache = getSupportMessageReadStateCache()
  conversations.forEach((conversation) => {
    const readIds = new Set(cache[conversation.conversationId] || [])
    if (readIds.size === 0) return
    conversation.messages.forEach((message) => {
      if (message.author === 'me' && readIds.has(message.id)) {
        message.readState = 'read'
      }
    })
  })
  return conversations
}

function normalizeOwnSupportMessage(message: ChatMessage) {
  return {
    ...message,
    author: 'me' as const
  }
}

function totalSupportUnread() {
  return state.supportConversations.reduce((total, conversation) => total + conversation.unreadCount, 0)
}

function lastSupportMessageId(conversation: SupportConversationItem) {
  return conversation.messages[conversation.messages.length - 1]?.id || ''
}

function getSupportReadCache() {
  return (uni.getStorageSync(SUPPORT_READ_CACHE_KEY) || {}) as Record<string, string>
}

function setSupportReadCache(conversationId: string, messageId: string) {
  if (!conversationId || !messageId) return
  const cache = getSupportReadCache()
  cache[conversationId] = messageId
  uni.setStorageSync(SUPPORT_READ_CACHE_KEY, cache)
}

function applySupportReadCache(conversations: SupportConversationItem[]) {
  const cache = getSupportReadCache()
  conversations.forEach((conversation) => {
    const lastMessageId = lastSupportMessageId(conversation)
    if (lastMessageId && cache[conversation.conversationId] === lastMessageId) {
      conversation.unreadCount = 0
    }
  })
  return applySupportMessageReadStateCache(conversations)
}

function syncActiveSupportConversation() {
  const active = state.supportConversations.find((conversation) => conversation.conversationId === state.supportConversationId)
  if (!active) {
    return
  }
  state.supportMessages = active.messages
  state.supportUnreadCount = totalSupportUnread()
}

function upsertSupportConversation(conversation: SupportConversationItem) {
  const normalizedConversation = normalizeSupportConversation(conversation)
  applySupportReadCache([normalizedConversation])
  const index = state.supportConversations.findIndex((item) => item.conversationId === normalizedConversation.conversationId)
  if (index >= 0) {
    state.supportConversations.splice(index, 1, normalizedConversation)
  } else {
    state.supportConversations.unshift(normalizedConversation)
  }
  syncActiveSupportConversation()
}

function clearSupportUnread(conversationId = state.supportConversationId) {
  const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
  if (conversation) {
    conversation.unreadCount = 0
    setSupportReadCache(conversationId, lastSupportMessageId(conversation))
  }
  state.supportUnreadCount = totalSupportUnread()
}

export function useAppStore() {
  function selectedCountry() {
    return state.countries.find((country) => country.code === state.selectedCountryCode) || state.countries[0]
  }

  async function bootstrap() {
    try {
      const previousActiveFriend = state.activeFriendUsername
      const previousSupportConversationId = state.supportConversationId
      const [rates, friends, blacklist, support, transactions, friendRequests, withdrawals, loans, videoSessions] = await Promise.all([
        fetchRates(),
        fetchFriends(),
        fetchBlacklist(),
        fetchSupportMessages(),
        fetchTransactions(),
        fetchFriendRequests(),
        fetchWithdrawals(),
        fetchLoans(),
        fetchVideoSessions()
      ])

      const normalizedFriends = friends.map(normalizeFriendProfile)
      const normalizedSupport = support.map(normalizeSupportConversation)
      state.rates = rates
      rememberFriendConversationCursors(normalizedFriends)
      state.friends = normalizedFriends
      state.blacklist = blacklist
      state.supportConversations = applySupportReadCache(normalizedSupport)
      rememberSupportConversationCursors(state.supportConversations)
      state.transactions = transactions
      state.withdrawals = withdrawals
      state.loans = loans
      state.videoSessions = videoSessions
      state.friendRequests = friendRequests
      refreshBalanceSummary().catch(() => {})
      refreshSupportLedger().catch(() => {})
      state.supportConversationId = normalizedSupport.some((conversation) => conversation.conversationId === previousSupportConversationId)
        ? previousSupportConversationId
        : normalizedSupport[0]?.conversationId || state.supportConversationId
      syncActiveSupportConversation()
      state.activeFriendUsername = normalizedFriends.some((friend) => friend.username === previousActiveFriend)
        ? previousActiveFriend
        : normalizedFriends[0]?.username || ''
    } catch (error) {
      throw error instanceof Error ? error : new Error('Bootstrap failed')
    }
  }

  async function refreshRates() {
    const rates = await fetchRates()
    state.rates = rates
    return rates
  }

  async function refreshSupport() {
    const support = await fetchSupportMessages()
    const normalizedSupport = support.map(normalizeSupportConversation)
    const previousSupportConversationId = state.supportConversationId
    state.supportConversations = applySupportReadCache(normalizedSupport)
    rememberSupportConversationCursors(state.supportConversations)
    state.supportConversationId = normalizedSupport.some((conversation) => conversation.conversationId === previousSupportConversationId)
      ? previousSupportConversationId
      : normalizedSupport[0]?.conversationId || state.supportConversationId
    syncActiveSupportConversation()
    return normalizedSupport
  }

  function setCountry(code: string) {
    state.selectedCountryCode = code
  }

  function chooseCountry() {
    uni.showActionSheet({
      itemList: state.countries.map((country) => country.name),
      success(result) {
        const country = state.countries[result.tapIndex]
        if (country) {
          state.selectedCountryCode = country.code
        }
      }
    })
  }

  async function login(identifier: string, password: string) {
    const session = await loginWithPassword(identifier, password)
    state.currentUser = session
    setSessionToken(session.accessToken)
    setStoredSessionUser(session)
    return session
  }

  async function register(payload: {
    username: string
    email?: string
    phone?: string
    password: string
  }) {
    const session = await registerAccount(payload)
    state.currentUser = session
    setSessionToken(session.accessToken)
    setStoredSessionUser(session)
    return session
  }

  async function logout() {
    try {
      await logoutSession()
    } catch {
      // Local logout should still complete if the network request fails.
    } finally {
      closeAllChatSockets()
      state.currentUser = null
      state.friendRequests = []
      state.friendSearchResults = []
      state.supportLedger = null
      state.activeSupportCustomerProfile = null
      state.supportCustomerProfileCache = {}
      setSessionToken(undefined)
      setStoredSessionUser(null)
      uni.removeStorageSync(SUPPORT_MESSAGE_READ_STATE_KEY)
      uni.removeStorageSync(CHAT_CURSOR_CACHE_KEY)
    }
  }

  async function addRate(payload: { cardName: string; region: string; rate: string }) {
    const rate = await createRateRequest(payload)
    state.rates.unshift(rate)
    return rate
  }

  async function updateRateStatus(rateId: string, status: RateItem['status']) {
    const updated = await updateRateStatusRequest(rateId, status)
    const index = state.rates.findIndex((item) => item.id === rateId)
    if (index >= 0) {
      state.rates.splice(index, 1, updated)
    }
    return updated
  }

  async function updateRate(rateId: string, payload: { cardName: string; region: string; rate: string }) {
    const updated = await updateRateRequest(rateId, payload)
    const index = state.rates.findIndex((item) => item.id === rateId)
    if (index >= 0) {
      state.rates.splice(index, 1, updated)
    }
    return updated
  }

  async function deleteRate(rateId: string) {
    const deleted = await deleteRateRequest(rateId)
    state.rates = state.rates.filter((item) => item.id !== rateId)
    return deleted
  }

  async function queryFriendSearch(keyword: string) {
    if (!keyword.trim()) {
      state.friendSearchResults = []
      return []
    }

    const results = await searchFriends(keyword.trim())
    state.friendSearchResults = results
    return results
  }

  async function sendFriendRequest(username: string) {
    const request = await createFriendRequest(username)
    state.friendRequests.unshift(request)
    await bootstrap()
    return request
  }

  async function respondFriendRequest(friendshipId: string, action: 'accept' | 'reject') {
    const request = action === 'accept'
      ? await acceptFriendRequest(friendshipId)
      : await rejectFriendRequest(friendshipId)
    state.friendRequests = state.friendRequests.filter((item) => item.friendshipId !== friendshipId)
    await bootstrap()
    return request
  }

  function appendSupportMessage(content: string, type: ChatMessage['type'] = 'text', clientMessageId?: string) {
    const fallback = makeLocalMessage(content, type, clientMessageId)
    mergeUniqueMessage(state.supportMessages, fallback)
    const active = state.supportConversations.find((conversation) => conversation.conversationId === state.supportConversationId)
    if (active) {
      mergeUniqueMessage(active.messages, fallback)
    }
    return fallback
  }

  function pushSupportRealtime(message: ChatMessage, conversationId = state.supportConversationId) {
    rememberMessageCursor('support', conversationId, message)
    const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
    if (conversation) {
      mergeUniqueMessage(conversation.messages, message)
      if (conversationId !== state.supportConversationId) {
        conversation.unreadCount += 1
      } else {
        conversation.unreadCount = 0
      }
    }
    if (conversationId === state.supportConversationId) {
      mergeUniqueMessage(state.supportMessages, message)
    }
    state.supportUnreadCount = totalSupportUnread()
  }

  function applySupportReadReceipt(conversationId = state.supportConversationId) {
    const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
    if (conversation) {
      conversation.messages.forEach((message) => {
        if (message.author === 'me') {
          message.readState = 'read'
        }
      })
    }
    if (conversationId === state.supportConversationId) {
      state.supportMessages.forEach((message) => {
        if (message.author === 'me') {
          message.readState = 'read'
        }
      })
    }
    rememberReadSupportMessages(conversationId)
  }

  function applySupportPresence(conversationId: string, online: boolean) {
    const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
    if (conversation) {
      conversation.online = online
    }
    if (state.activeSupportCustomerProfile?.conversationId === conversationId) {
      state.activeSupportCustomerProfile.customer.online = online
    }
  }

  async function sendSupport(content: string, messageType: ChatMessage['type'] = 'text') {
    const conversationId = state.supportConversationId
    const local = appendSupportMessage(content, messageType)
    const active = state.supportConversations.find((conversation) => conversation.conversationId === state.supportConversationId)
    try {
      const sent = await sendSupportMessage(conversationId, {
        content,
        messageType,
        clientMessageId: local.clientMessageId
      })
      const message = normalizeOwnSupportMessage(sent)
      rememberMessageCursor('support', conversationId, message)
      replaceMessage(state.supportMessages, local.id, message)
      if (active) {
        replaceMessage(active.messages, local.id, message)
      }
      return message
    } catch (error) {
      markMessageFailed(state.supportMessages, local.id)
      if (active) {
        markMessageFailed(active.messages, local.id)
      }
      throw error
    }
  }

  async function retrySupportMessage(messageId: string) {
    const message = state.supportMessages.find((item) => item.id === messageId)
    if (!message || message.author !== 'me' || message.readState !== 'failed') {
      return null
    }
    state.supportMessages = state.supportMessages.filter((item) => item.id !== messageId)
    const active = state.supportConversations.find((conversation) => conversation.conversationId === state.supportConversationId)
    if (active) {
      active.messages = active.messages.filter((item) => item.id !== messageId)
    }
    const clientMessageId = message.clientMessageId || makeClientMessageId()
    const local = appendSupportMessage(message.content, message.type, clientMessageId)
    try {
      const sent = await sendSupportMessage(state.supportConversationId, {
        content: message.content,
        messageType: message.type,
        clientMessageId
      })
      const canonical = normalizeOwnSupportMessage(sent)
      rememberMessageCursor('support', state.supportConversationId, canonical)
      replaceMessage(state.supportMessages, local.id, canonical)
      if (active) {
        replaceMessage(active.messages, local.id, canonical)
      }
      return canonical
    } catch (error) {
      markMessageFailed(state.supportMessages, local.id)
      if (active) {
        markMessageFailed(active.messages, local.id)
      }
      throw error
    }
  }

  async function recoverSupportMessages(conversationId = state.supportConversationId) {
    const conversation = state.supportConversations.find((item) => item.conversationId === conversationId)
    const cachedSeq = getChatCursor('support', conversationId)
    const localSeq = latestMessageSeq((conversation?.messages || []).filter((message) => !message.id.startsWith('local-')))
    const sync = await syncSupportMessages(conversationId, Math.max(cachedSeq, localSeq))
    const normalizedMessages = sync.messages.map(normalizeChatMessage)
    normalizedMessages.forEach((message) => {
      pushSupportRealtime(message, conversationId)
    })
    setChatCursor('support', conversationId, sync.latestSeq)
    if (conversation) {
      applyReadSeq(conversation.messages, sync.readSeq)
      conversation.unreadCount = conversationId === state.supportConversationId ? 0 : sync.unreadCount
    }
    if (conversationId === state.supportConversationId) {
      applyReadSeq(state.supportMessages, sync.readSeq)
    }
    if (sync.messages.length > 0 && conversationId === state.supportConversationId) {
      await markSupportRead().catch(() => {})
    }
    state.supportUnreadCount = totalSupportUnread()
    return normalizedMessages
  }

  async function markSupportRead() {
    const conversationId = state.supportConversationId
    clearSupportUnread(conversationId)
    const updated = await markSupportConversationRead(conversationId)
    upsertSupportConversation(updated)
    return updated
  }

  async function updateSupportNote(conversationId: string, note: string) {
    const updated = await updateSupportConversationNote(conversationId, note)
    upsertSupportConversation(updated)
    refreshSupportLedger().catch(() => {})
    return updated
  }

  function setActiveFriend(username: string) {
    state.activeFriendUsername = username
  }

  function setActiveSupportConversation(conversationId: string) {
    state.supportConversationId = conversationId
    syncActiveSupportConversation()
    state.activeSupportCustomerProfile = state.supportCustomerProfileCache[conversationId] || null
  }

  async function refreshSupportCustomerProfile(conversationId = state.supportConversationId, force = false) {
    if (!conversationId) {
      state.activeSupportCustomerProfile = null
      return null
    }
    if (!force && state.supportCustomerProfileCache[conversationId]) {
      state.activeSupportCustomerProfile = state.supportCustomerProfileCache[conversationId]
      return state.activeSupportCustomerProfile
    }
    state.supportCustomerProfileLoading = true
    try {
      const profile = await fetchSupportCustomerProfile(conversationId)
      state.supportCustomerProfileCache[conversationId] = profile
      if (state.supportConversationId === conversationId) {
        state.activeSupportCustomerProfile = profile
      }
      return profile
    } finally {
      state.supportCustomerProfileLoading = false
    }
  }

  async function refreshSupportLedger() {
    const role = state.currentUser?.roleCode
    if (role !== 'AGENT' && role !== 'ADMIN') {
      state.supportLedger = null
      return null
    }
    state.supportLedgerLoading = true
    try {
      const ledger = await fetchSupportLedger()
      state.supportLedger = ledger
      return ledger
    } finally {
      state.supportLedgerLoading = false
    }
  }

  function pushFriendRealtime(friendshipId: string, message: ChatMessage) {
    rememberMessageCursor('friend', friendshipId, message)
    const friend = state.friends.find((item) => item.id === friendshipId)
    if (friend) {
      mergeUniqueMessage(friend.messages, message)
    }
  }

  function applyFriendReadReceipt(friendshipId: string) {
    const friend = state.friends.find((item) => item.id === friendshipId)
    if (!friend) return

    friend.messages.forEach((message) => {
      if (message.author === 'me' && message.readState === 'sent') {
        message.readState = 'read'
      }
    })
  }

  async function sendFriendMessage(friendshipId: string, content: string, messageType: ChatMessage['type'] = 'text') {
    const message = normalizeChatMessage(await sendDirectMessage(friendshipId, {
      content,
      messageType,
      clientMessageId: makeClientMessageId()
    }))
    rememberMessageCursor('friend', friendshipId, message)
    const friend = state.friends.find((item) => item.id === friendshipId)
    if (friend) {
      mergeUniqueMessage(friend.messages, message)
    }
    return message
  }

  async function recoverFriendMessages(friendshipId: string) {
    const friend = state.friends.find((item) => item.id === friendshipId)
    const cachedSeq = getChatCursor('friend', friendshipId)
    const localSeq = latestMessageSeq((friend?.messages || []).filter((message) => !message.id.startsWith('local-')))
    const sync = await syncFriendMessages(friendshipId, Math.max(cachedSeq, localSeq))
    const normalizedMessages = sync.messages.map(normalizeChatMessage)
    normalizedMessages.forEach((message) => {
      pushFriendRealtime(friendshipId, message)
    })
    setChatCursor('friend', friendshipId, sync.latestSeq)
    if (friend) {
      applyReadSeq(friend.messages, sync.readSeq)
      friend.unreadCount = sync.unreadCount
    }
    return normalizedMessages
  }

  async function markFriendRead(friendshipId: string) {
    const updated = await markFriendConversationRead(friendshipId)
    const index = state.friends.findIndex((friend) => friend.id === friendshipId)
    if (index >= 0) {
      state.friends.splice(index, 1, updated)
    }
    return updated
  }

  async function removeFriendship(friendshipId: string) {
    const removed = await removeFriend(friendshipId)
    state.friends = state.friends.filter((friend) => friend.id !== friendshipId)
    if (state.activeFriendUsername === removed.username) {
      state.activeFriendUsername = state.friends[0]?.username || ''
    }
    return removed
  }

  async function createTransaction(payload: {
    counterpartyUsername: string
    friendshipId?: string
    cardName: string
    faceValue: string
    payoutAmount: string
    note?: string
    voucherImageUrl?: string
  }) {
    const transaction = await createTransactionRequest(payload)
    state.transactions.unshift(transaction)
    return transaction
  }

  async function createSellOrder(payload: SellOrderPayload) {
    const transaction = await createSellOrderRequest(payload)
    state.transactions.unshift(transaction)
    await bootstrap()
    return transaction
  }

  async function createWithdrawal(payload: {
    amount: string
    country: string
    accountName: string
    bankName: string
    accountNumber: string
    contact?: string
    note?: string
    sendChatMessage?: boolean
  }) {
    const withdrawal = await createWithdrawalRequest(payload)
    state.withdrawals.unshift(withdrawal)
    await bootstrap()
    return withdrawal
  }

  async function updateWithdrawalStatus(withdrawalId: string, status: WithdrawalItem['status']) {
    const updated = await updateWithdrawalStatusRequest(withdrawalId, status)
    const index = state.withdrawals.findIndex((item) => item.id === withdrawalId)
    if (index >= 0) {
      state.withdrawals.splice(index, 1, updated)
    }
    refreshSupportCustomerProfile(state.supportConversationId, true).catch(() => {})
    refreshSupportLedger().catch(() => {})
    return updated
  }

  async function createLoanApplication(payload: {
    amount: string
    country: string
    purpose: string
    contact?: string
    repaymentPlan?: string
    sendChatMessage?: boolean
  }) {
    const loan = await createLoanApplicationRequest(payload)
    state.loans.unshift(loan)
    await bootstrap()
    return loan
  }

  async function updateLoanStatus(loanId: string, status: LoanApplicationItem['status'], reviewNote?: string) {
    const updated = await updateLoanStatusRequest(loanId, status, reviewNote)
    const index = state.loans.findIndex((item) => item.id === loanId)
    if (index >= 0) {
      state.loans.splice(index, 1, updated)
    }
    return updated
  }

  async function refreshBroadcasts() {
    const broadcasts = await fetchBroadcasts()
    state.broadcasts = broadcasts
    return broadcasts
  }

  async function refreshBalanceSummary() {
    const summary = await fetchBalanceSummary()
    state.balanceSummary = summary
    return summary
  }

  async function createBroadcast(payload: {
    scope: 'own' | 'all'
    content: string
    messageType: BroadcastItem['messageType']
  }) {
    const broadcast = await createBroadcastRequest(payload)
    state.broadcasts.unshift(broadcast)
    await bootstrap()
    return broadcast
  }

  async function createVideoSession(payload: {
    channelType: 'support' | 'friend'
    channelId: string
  }) {
    const bootstrap = await createVideoSessionRequest(payload)
    state.videoSessions.unshift(bootstrap.session)
    return bootstrap
  }

  async function getVideoSessionBootstrap(sessionId: string) {
    return fetchVideoSessionBootstrap(sessionId)
  }

  async function updateVideoSessionStatus(sessionId: string, status: VideoSessionItem['status']) {
    const updated = await updateVideoSessionStatusRequest(sessionId, status)
    upsertVideoSession(updated)
    return updated
  }

  function applyVideoSessionStatus(event: VideoSessionStatusEvent) {
    const index = state.videoSessions.findIndex((item) => item.id === event.sessionId)
    if (index < 0) {
      return null
    }
    const updated: VideoSessionItem = {
      ...state.videoSessions[index],
      status: event.status,
      startedAt: event.startedAt,
      endedAt: event.endedAt,
      updatedAt: event.updatedAt
    }
    upsertVideoSession(updated)
    return updated
  }

  function upsertVideoSession(updated: VideoSessionItem) {
    const index = state.videoSessions.findIndex((item) => item.id === updated.id)
    if (index >= 0) {
      state.videoSessions.splice(index, 1, updated)
    } else {
      state.videoSessions.unshift(updated)
    }
  }

  async function refreshRanking(mode: 'sales' | 'invitation' = 'sales', month?: string) {
    const ranking = await fetchRanking(mode, month)
    state.ranking = ranking
    return ranking
  }

  async function updateTransactionStatus(transactionId: string, status: TransactionItem['status']) {
    const updated = await updateTransactionStatusRequest(transactionId, status)
    const index = state.transactions.findIndex((item) => item.id === transactionId)
    if (index >= 0) {
      state.transactions.splice(index, 1, updated)
    }
    refreshSupportCustomerProfile(state.supportConversationId, true).catch(() => {})
    refreshSupportLedger().catch(() => {})
    return updated
  }

  async function blockUser(username: string, reason?: string) {
    const blocked = await addBlacklist(username, reason)
    state.blacklist.unshift(blocked)
    state.friends = state.friends.filter((friend) => friend.username !== username)
    return blocked
  }

  async function unblockUser(blacklistId: string) {
    const restored = await removeBlacklist(blacklistId)
    state.blacklist = state.blacklist.filter((item) => item.id !== blacklistId)
    return restored
  }

  function go(url: string) {
    uni.navigateTo({ url: safeRouteForRole(url, state.currentUser) })
  }

  return {
    state,
    selectedCountry,
    bootstrap,
    refreshRates,
    refreshSupport,
    login,
    register,
    logout,
    addRate,
    updateRateStatus,
    updateRate,
    deleteRate,
    queryFriendSearch,
    sendFriendRequest,
    respondFriendRequest,
    appendSupportMessage,
    pushSupportRealtime,
    applySupportReadReceipt,
    applySupportPresence,
    sendSupport,
    retrySupportMessage,
    recoverSupportMessages,
    markSupportRead,
    updateSupportNote,
    refreshSupportCustomerProfile,
    refreshSupportLedger,
    clearSupportUnread,
    setActiveSupportConversation,
    setCountry,
    chooseCountry,
    setActiveFriend,
    pushFriendRealtime,
    applyFriendReadReceipt,
    sendFriendMessage,
    recoverFriendMessages,
    markFriendRead,
    removeFriendship,
    createTransaction,
    createSellOrder,
    updateTransactionStatus,
    createWithdrawal,
    updateWithdrawalStatus,
    createLoanApplication,
    updateLoanStatus,
    refreshBroadcasts,
    refreshBalanceSummary,
    createBroadcast,
    createVideoSession,
    getVideoSessionBootstrap,
    updateVideoSessionStatus,
    applyVideoSessionStatus,
    refreshRanking,
    blockUser,
    unblockUser,
    go
  }
}
