import type {
  AdminDirectConversationItem,
  AdminUserItem,
  BalanceSummary,
  AgentItem,
  BankAccountItem,
  BankAccountRiskMatch,
  BroadcastItem,
  ChatMessage,
  ChatMessageSync,
  CountryCodeRule,
  CurrencyExchangeRateItem,
  FriendProfile,
  FriendRequest,
  HiddenRecordItem,
  LoanApplicationItem,
  LotteryDrawResult,
  LotteryEligibility,
  LotteryFulfillmentItem,
  LotteryPrizeItem,
  LotteryRecordItem,
  LotteryWinnerItem,
  NotificationItem,
  PushDeviceItem,
  ReferralRewardConfigItem,
  ReferralRewardItem,
  RegistrationBonusConfigItem,
  RegistrationBonusRecordItem,
  RankingBoard,
  RateItem,
  SearchFriendResult,
  SessionUser,
  SellOrderPayload,
  SupportConversationItem,
  SupportCustomerProfile,
  SupportCustomerSearchResult,
  SupportLedgerReport,
  SupportMessageSearchResult,
  TransactionItem,
  CompletedTransactionFeedItem,
  UploadAsset,
  VideoSessionBootstrap,
  VideoSessionItem,
  VipSummary,
  WithdrawalItem
} from '@/types'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || '/ws'
const SESSION_TOKEN_KEY = 'gift-chat-access-token'
const SESSION_USER_KEY = 'gift-chat-session-user'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

function getAccessToken() {
  return uni.getStorageSync(SESSION_TOKEN_KEY) as string | undefined
}

function normalizeApiBase(url: string) {
  return url.replace(/\/+$/, '')
}

function normalizeWsBase(url: string) {
  return url.replace(/\/+$/, '')
}

function resolveWsBase(url: string) {
  const normalized = normalizeWsBase(url)
  if (/^wss?:\/\//i.test(normalized)) {
    return normalized
  }

  // #ifdef H5
  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const path = normalized.startsWith('/') ? normalized : `/${normalized}`
    return `${protocol}://${window.location.host}${path}`
  }
  // #endif

  return normalized
}

function readErrorMessage(body: unknown, fallback: string) {
  if (body && typeof body === 'object' && 'message' in body) {
    return String((body as { message?: unknown }).message || fallback)
  }

  if (typeof body === 'string' && body.trim()) {
    return body
  }

  return fallback
}

function request<T>(url: string, method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET', data?: Record<string, unknown>) {
  const token = getAccessToken()

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${normalizeApiBase(API_BASE_URL)}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success(response) {
        const body = response.data as ApiEnvelope<T> | { message?: string } | string
        if (response.statusCode && response.statusCode >= 400) {
          reject(new Error(readErrorMessage(body, 'Request failed')))
          return
        }
        resolve((body as ApiEnvelope<T>).data)
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

export function setSessionToken(token?: string) {
  if (token) {
    uni.setStorageSync(SESSION_TOKEN_KEY, token)
    return
  }

  uni.removeStorageSync(SESSION_TOKEN_KEY)
}

export function getStoredSessionUser() {
  return uni.getStorageSync(SESSION_USER_KEY) as SessionUser | undefined
}

export function setStoredSessionUser(user?: SessionUser | null) {
  if (user) {
    uni.setStorageSync(SESSION_USER_KEY, user)
    return
  }

  uni.removeStorageSync(SESSION_USER_KEY)
}

export function buildChatSocketUrl(channelType: 'friend' | 'support', channelId: string) {
  return `${resolveWsBase(WS_BASE_URL)}/chat/${channelType}/${channelId}`
}

export function buildChatSocketAuthMessage() {
  const token = getAccessToken()
  if (!token) {
    throw new Error('Missing access token')
  }
  return JSON.stringify({ type: 'auth', token })
}

export function fetchRates() {
  return request<RateItem[]>('/rates').then((rates) => rates.map(normalizeRateItem))
}

export function fetchFriends() {
  return request<FriendProfile[]>('/friends')
}

export function fetchFriendRequests() {
  return request<FriendRequest[]>('/friends/requests')
}

export function searchFriends(username: string) {
  return request<SearchFriendResult[]>(`/friends/search?username=${encodeURIComponent(username)}`)
}

export function createFriendRequest(username: string) {
  return request<FriendRequest>('/friends/requests', 'POST', { username })
}

export function acceptFriendRequest(friendshipId: string) {
  return request<FriendRequest>(`/friends/requests/${friendshipId}/accept`, 'POST')
}

export function rejectFriendRequest(friendshipId: string) {
  return request<FriendRequest>(`/friends/requests/${friendshipId}/reject`, 'POST')
}

export function removeFriend(friendshipId: string) {
  return request<FriendProfile>(`/friends/${friendshipId}`, 'DELETE')
}

export function fetchBlacklist() {
  return request<FriendProfile[]>('/blacklist')
}

export function addBlacklist(username: string, reason?: string) {
  return request<FriendProfile>('/blacklist', 'POST', { username, reason })
}

export function removeBlacklist(blacklistId: string) {
  return request<FriendProfile>(`/blacklist/${blacklistId}`, 'DELETE')
}

export function fetchSupportMessages() {
  return request<SupportConversationItem[]>('/support/conversations')
}

export function markSupportConversationRead(conversationId: string) {
  return request<SupportConversationItem>(`/support/conversations/${conversationId}/read`, 'POST')
}

export function fetchSupportMessagesAfter(conversationId: string, afterId?: string) {
  const query = afterId ? `?afterId=${encodeURIComponent(afterId)}` : ''
  return request<ChatMessage[]>(`/support/conversations/${conversationId}/messages${query}`)
}

export function syncSupportMessages(conversationId: string, sinceSeq = 0) {
  const query = `?sinceSeq=${encodeURIComponent(String(Math.max(0, sinceSeq || 0)))}`
  return request<ChatMessageSync>(`/support/conversations/${conversationId}/messages/sync${query}`)
}

export function fetchSupportCustomerProfile(conversationId: string) {
  return request<SupportCustomerProfile>(`/support/conversations/${conversationId}/customer-profile`)
}

export function fetchSupportLedger() {
  return request<SupportLedgerReport>('/support/ledger')
}

export function updateSupportConversationNote(conversationId: string, note: string) {
  return request<SupportConversationItem>(`/support/conversations/${conversationId}/note`, 'POST', { note })
}

export function fetchTransactions() {
  return request<TransactionItem[]>('/transactions')
}

export function fetchRecentCompletedTransactions() {
  return request<CompletedTransactionFeedItem[]>('/transactions/recent-completed')
}

export function createSellOrder(payload: SellOrderPayload) {
  return request<TransactionItem>('/transactions/sell-orders', 'POST', { ...payload })
}

export function fetchWithdrawals() {
  return request<WithdrawalItem[]>('/withdrawals')
}

export function createWithdrawal(payload: {
  amount: string
  country: string
  accountName: string
  bankName: string
  accountNumber: string
  contact?: string
  note?: string
  sendChatMessage?: boolean
}) {
  return request<WithdrawalItem>('/withdrawals', 'POST', payload)
}

export function fetchCountryCodeRules() {
  return request<CountryCodeRule[]>('/country-codes')
}

export function fetchMyBankAccount() {
  return request<BankAccountItem | null>('/bank-accounts/me')
}

export function bindBankAccount(payload: {
  country: string
  accountName: string
  bankName: string
  accountNumber: string
}) {
  return request<BankAccountItem>('/bank-accounts', 'POST', payload)
}

export function replaceBankAccount(payload: {
  country: string
  accountName: string
  bankName: string
  accountNumber: string
}) {
  return request<BankAccountItem>('/bank-accounts/me', 'PUT', payload)
}

export function requestLotteryWithdrawal(recordId: string, bankAccount?: {
  country: string
  accountName: string
  bankName: string
  accountNumber: string
}) {
  return request<WithdrawalItem>(
    `/lottery/records/${recordId}/withdrawal-request`,
    'POST',
    bankAccount ? { bankAccount } : undefined
  )
}

export function fetchMyLotteryRecords() {
  return request<LotteryRecordItem[]>('/lottery/records/me')
}

export function fetchLotteryFulfillments() {
  return request<LotteryFulfillmentItem[]>('/lottery-fulfillments')
}

export function createLotteryFulfillment(recordId: string, payload: {
  recipientName: string
  phone: string
  country: string
  addressLine: string
}) {
  return request<LotteryFulfillmentItem>(`/lottery/records/${recordId}/fulfillment-order`, 'POST', payload)
}

export function updateLotteryFulfillmentStatus(orderId: string, status: LotteryFulfillmentItem['status']) {
  return request<LotteryFulfillmentItem>(`/lottery-fulfillments/${orderId}/status`, 'POST', { status })
}

export function updateWithdrawalStatus(withdrawalId: string, status: WithdrawalItem['status']) {
  return request<WithdrawalItem>(`/withdrawals/${withdrawalId}/status`, 'POST', { status })
}

export function fetchRanking(mode: 'sales' | 'invitation', month?: string) {
  const query = new URLSearchParams({ mode })
  if (month) query.set('month', month)
  return request<RankingBoard>(`/rankings?${query.toString()}`)
}

export function fetchNotifications() {
  return request<NotificationItem[]>('/notifications')
}

export function registerPushDevice(payload: {
  platform: string
  provider: string
  deviceToken: string
  deviceModel?: string
  appVersion?: string
}) {
  return request<PushDeviceItem>('/push/devices', 'POST', payload)
}

export function disablePushDevice(deviceId: string) {
  return request<PushDeviceItem>(`/push/devices/${deviceId}`, 'DELETE')
}

export function fetchBroadcasts() {
  return request<BroadcastItem[]>('/broadcasts')
}

export function fetchBalanceSummary() {
  return request<BalanceSummary>('/balances/summary')
}

export function createBroadcast(payload: {
  scope: 'own' | 'all'
  content: string
  messageType: BroadcastItem['messageType']
  mediaUrl?: string
  countryCodes?: string[]
  keyword?: string
  targetConversationIds?: string[]
}) {
  return request<BroadcastItem>('/broadcasts', 'POST', payload)
}

export function translateToChinese(text: string) {
  return request<{ originalText: string; translatedText: string; source: string }>('/translations/zh', 'POST', { text })
}

export function fetchLoans() {
  return request<LoanApplicationItem[]>('/loans')
}

export function createLoanApplication(payload: {
  amount: string
  country: string
  purpose: string
  contact?: string
  repaymentPlan?: string
  sendChatMessage?: boolean
}) {
  return request<LoanApplicationItem>('/loans', 'POST', payload)
}

export function updateLoanStatus(loanId: string, status: LoanApplicationItem['status'], reviewNote?: string) {
  return request<LoanApplicationItem>(`/loans/${loanId}/status`, 'POST', { status, reviewNote })
}

export function fetchVideoSessions() {
  return request<VideoSessionItem[]>('/video-sessions')
}

export function createVideoSession(payload: {
  channelType: 'support' | 'friend'
  channelId: string
}) {
  return request<VideoSessionBootstrap>('/video-sessions', 'POST', payload)
}

export function fetchVideoSessionBootstrap(sessionId: string) {
  return request<VideoSessionBootstrap>(`/video-sessions/${sessionId}/bootstrap`)
}

export function updateVideoSessionStatus(sessionId: string, status: VideoSessionItem['status']) {
  return request<VideoSessionItem>(`/video-sessions/${sessionId}/status`, 'POST', { status })
}

export function createTransaction(payload: {
  counterpartyUsername: string
  friendshipId?: string
  cardName: string
  faceValue: string
  payoutAmount: string
  note?: string
  voucherImageUrl?: string
}) {
  return request<TransactionItem>('/transactions', 'POST', payload)
}

export function loginWithPassword(identifier: string, password: string, countryCode: string) {
  return request<SessionUser>('/auth/login', 'POST', {
    identifier,
    password,
    countryCode
  })
}

export function registerAccount(payload: {
  countryCode: string
  username: string
  email?: string
  phone?: string
  password: string
  inviteCode?: string
}) {
  return request<SessionUser>('/auth/register', 'POST', payload)
}

export function logoutSession() {
  return request<boolean>('/auth/logout', 'POST')
}

export function fetchCurrentAccount() {
  return request<SessionUser>('/account/me')
}

export function updateAccountAvatar(avatarUrl: string) {
  return request<SessionUser>('/account/avatar', 'POST', { avatarUrl })
}

export function createRate(payload: {
  cardName: string
  cardCode?: string | null
  region: string
  rate: string
  localPayoutPerUsd?: number
}) {
  return request<RateItem>('/admin/rates', 'POST', payload).then(normalizeRateItem)
}

export function updateRateStatus(rateId: string, status: RateItem['status']) {
  return request<RateItem>(`/admin/rates/${rateId}/status`, 'POST', { status }).then(normalizeRateItem)
}

export function updateRate(rateId: string, payload: {
  cardName: string
  cardCode?: string | null
  region: string
  rate: string
  localPayoutPerUsd?: number
}) {
  return request<RateItem>(`/admin/rates/${rateId}`, 'POST', payload).then(normalizeRateItem)
}

export function deleteRate(rateId: string) {
  return request<RateItem>(`/admin/rates/${rateId}`, 'DELETE').then(normalizeRateItem)
}

function normalizeRateItem(rate: RateItem) {
  return {
    ...rate,
    rate: normalizeRateText(rate.displayRate || rate.rate)
  }
}

export function fetchMyCurrencyExchangeRate() {
  return request<CurrencyExchangeRateItem>('/currency-rates/me')
}

export function fetchAdminCurrencyExchangeRates() {
  return request<CurrencyExchangeRateItem[]>('/admin/currency-rates')
}

export function updateAdminCurrencyExchangeRate(payload: {
  countryCode: string
  localCurrencyPerUsd: number
  enabled: boolean
  note?: string
}) {
  return request<CurrencyExchangeRateItem>('/admin/currency-rates', 'POST', payload)
}

function normalizeRateText(value: string) {
  return value
    .replace(/\$1\s*=\s*/g, () => '$1 ≈ ')
    .replace(/1\s+USD\s*=\s*/gi, () => '1 USD ≈ ')
    .replace(/\s+\?\s+\?/g, ' ≈ ₦')
    .replace(/\s+\u00e2\u0089\u0088\s+\u00e2\u0082\u00a6/g, ' ≈ ₦')
}

export function fetchAdminUsers() {
  return request<AdminUserItem[]>('/admin/users')
}

export function fetchAgents() {
  return request<AgentItem[]>('/admin/agents')
}

export function createAgent(payload: { username: string; email?: string; phone?: string; password: string }) {
  return request<AgentItem>('/admin/agents', 'POST', payload)
}

export function updateAgentStatus(agentId: string, status: string) {
  return request<AgentItem>(`/admin/agents/${agentId}/status`, 'POST', { status })
}

export function updateAgentWelcomeMessage(agentId: string, payload: { content: string; enabled: boolean }) {
  return request<AgentItem>(`/admin/agents/${agentId}/welcome-message`, 'POST', payload)
}

export function fetchAdminSupportConversations() {
  return request<SupportConversationItem[]>('/admin/support/conversations')
}

export function fetchAdminDirectConversations(username?: string) {
  const query = username?.trim() ? `?username=${encodeURIComponent(username.trim())}` : ''
  return request<AdminDirectConversationItem[]>(`/admin/direct/conversations${query}`)
}

export function assignSupportConversation(conversationId: string, agentUsername: string) {
  return request<SupportConversationItem>(`/admin/support/conversations/${conversationId}/assign`, 'POST', { agentUsername })
}

export function fetchReferralRewardConfig() {
  return request<ReferralRewardConfigItem>('/admin/referral-rewards/config')
}

export function updateReferralRewardConfig(payload: {
  registrationCashbackEnabled: boolean
  registrationCashbackAmount: string
  tradeRebateEnabled: boolean
  tradeRebatePercent: string
}) {
  return request<ReferralRewardConfigItem>('/admin/referral-rewards/config', 'POST', payload)
}

export function fetchReferralRewards() {
  return request<ReferralRewardItem[]>('/admin/referral-rewards')
}

export function fetchRegistrationBonusConfigs() {
  return request<RegistrationBonusConfigItem[]>('/admin/registration-bonuses/config')
}

export function updateRegistrationBonusConfig(payload: {
  countryCode: string
  countryName: string
  currencyCode: string
  bonusAmount: string
  enabled: boolean
  note?: string
}) {
  return request<RegistrationBonusConfigItem>('/admin/registration-bonuses/config', 'POST', payload)
}

export function fetchRegistrationBonusRecords() {
  return request<RegistrationBonusRecordItem[]>('/admin/registration-bonuses')
}

export function fetchAdminBankAccountRisks() {
  return request<BankAccountRiskMatch[]>('/admin/bank-account-risks')
}

export function fetchAdminBankAccounts() {
  return request<BankAccountItem[]>('/admin/bank-accounts')
}

export function fetchMyRegistrationBonus() {
  return request<RegistrationBonusRecordItem | null>('/account/registration-bonus')
}

export function fetchVipSummary() {
  return request<VipSummary>('/vip/me')
}

export function fetchLotteryEligibility() {
  return request<LotteryEligibility>('/lottery/eligibility')
}

export function spinLottery() {
  return request<LotteryDrawResult>('/lottery/spin', 'POST')
}

export function fetchLotteryWinners() {
  return request<LotteryWinnerItem[]>('/lottery/winners')
}

export function fetchLotteryPrizes() {
  return request<LotteryPrizeItem[]>('/lottery/prizes')
}

export function hideAccountRecord(payload: {
  targetType: 'ORDER' | 'MESSAGE' | 'CONVERSATION' | 'order' | 'message' | 'conversation' | string
  targetId: string
  hiddenScope?: string
}) {
  return request<HiddenRecordItem>('/account/hidden-records', 'POST', payload)
}

export function fetchHiddenRecords() {
  return request<HiddenRecordItem[]>('/account/hidden-records')
}

export function searchSupportCustomers(keyword: string) {
  return request<SupportCustomerSearchResult[]>(`/support/search/customers?keyword=${encodeURIComponent(keyword)}`)
}

export function searchSupportMessages(keyword: string) {
  return request<SupportMessageSearchResult[]>(`/support/search/messages?keyword=${encodeURIComponent(keyword)}`)
}

export function fetchAdminLotteryRecords() {
  return request<LotteryRecordItem[]>('/admin/lottery/records')
}

export function updateAdminLotteryRecordStatus(recordId: string, status: string) {
  return request<LotteryRecordItem>(`/admin/lottery/records/${recordId}/status`, 'POST', { status })
}

export function resetAdminLotteryEligibility(userId: string, reason: string) {
  return request<boolean>(`/admin/lottery/users/${userId}/reset`, 'POST', { reason })
}

export function sendSupportMessage(conversationId: string, payload: { content: string; messageType: string; clientMessageId?: string; replyTo?: ChatMessage['replyTo'] }) {
  return request<ChatMessage>(
    `/support/conversations/${conversationId}/messages`,
    'POST',
    payload
  )
}

export function sendDirectMessage(friendshipId: string, payload: { content: string; messageType: string; clientMessageId?: string }) {
  return request<ChatMessage>(
    `/friends/${friendshipId}/messages`,
    'POST',
    payload
  )
}

export function fetchFriendMessagesAfter(friendshipId: string, afterId?: string) {
  const query = afterId ? `?afterId=${encodeURIComponent(afterId)}` : ''
  return request<ChatMessage[]>(`/friends/${friendshipId}/messages${query}`)
}

export function syncFriendMessages(friendshipId: string, sinceSeq = 0) {
  const query = `?sinceSeq=${encodeURIComponent(String(Math.max(0, sinceSeq || 0)))}`
  return request<ChatMessageSync>(`/friends/${friendshipId}/messages/sync${query}`)
}

export function markFriendConversationRead(friendshipId: string) {
  return request<FriendProfile>(`/friends/${friendshipId}/read`, 'POST')
}

export function updateTransactionStatus(transactionId: string, status: TransactionItem['status']) {
  return request<TransactionItem>(`/transactions/${transactionId}/status`, 'POST', { status })
}

export function cancelTransaction(transactionId: string, payload: { reason: string; note?: string; notifyCustomer?: boolean }) {
  return request<TransactionItem>(`/transactions/${transactionId}/cancel`, 'POST', payload)
}

export function uploadImage(filePath: string) {
  return uploadFile('/uploads/images', filePath)
}

export function uploadVoice(filePath: string) {
  return uploadFile('/uploads/voices', filePath)
}

export function uploadVideo(filePath: string) {
  return uploadFile('/uploads/videos', filePath)
}

function uploadFile(url: string, filePath: string) {
  const token = getAccessToken()

  return new Promise<UploadAsset>((resolve, reject) => {
    uni.uploadFile({
      url: `${normalizeApiBase(API_BASE_URL)}${url}`,
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success(response) {
        try {
          const body = JSON.parse(response.data) as ApiEnvelope<UploadAsset> | { message?: string }
          if (response.statusCode >= 400) {
            reject(new Error(readErrorMessage(body, 'Upload failed')))
            return
          }
          resolve(normalizeUploadAsset((body as ApiEnvelope<UploadAsset>).data))
        } catch (error) {
          reject(error)
        }
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function normalizeUploadAsset(asset: UploadAsset) {
  return {
    ...asset,
    publicUrl: resolveMediaUrl(asset.publicUrl)
  }
}
