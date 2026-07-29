export type GiftCardFaceCurrency = 'USD' | 'EUR' | 'GBP' | 'AUD'

export interface RateItem {
  id: string
  cardName: string
  cardCode?: string | null
  region: string
  rate: string
  currencyCode?: string
  localPayoutPerUsd?: string
  displayRate?: string
  quotes: Partial<Record<GiftCardFaceCurrency, string>>
  imageUrl: string
  status: 'active' | 'paused'
  updatedAt: string
}

export interface ChatMessage {
  id: string
  author: 'me' | 'support' | 'friend' | 'system'
  content: string
  type: 'text' | 'image' | 'voice' | 'gif' | 'link' | 'video' | 'order'
  createdAt: string
  readState?: 'none' | 'sending' | 'sent' | 'read' | 'failed'
  clientMessageId?: string
  serverSeq?: number
  deliveryStatus?: 'pending' | 'delivered' | 'failed'
  deliveredAt?: string
  failedReason?: string
  attachments?: MessageAttachment[]
  replyTo?: ChatMessageReply | null
  order?: ChatOrderItem | null
}

export interface ChatOrderItem {
  id: string
  orderNo: string
  cardName: string
  faceValue: string
  estimatedLocalAmount: string
  finalLocalAmount: string
  payoutAmount: string
  currencyCode: string
  status: TransactionItem['status']
  voucherImageUrl: string
  manualVipPoints: string
  settlementReason: string
  settledBy: string
  settledAt: string
}

export interface ChatMessageReply {
  messageId: string
  author?: ChatMessage['author'] | string
  content: string
}

export interface MessageAttachment {
  id: string
  type: 'image' | 'gif' | 'video' | 'voice' | 'file' | 'call'
  url: string
  thumbnailUrl?: string
  mimeType?: string
  originalName?: string
  sizeBytes?: number
  width?: number
  height?: number
  durationMs?: number
  status?: string
}

export interface ChatMessageSync {
  messages: ChatMessage[]
  latestSeq: number
  readSeq: number
  unreadCount: number
}

export interface VideoCallMessagePayload {
  kind: 'video_call'
  sessionId: string
  roomId: string
  channelType: 'friend' | 'support'
  channelId: string
  initiatorUsername: string
  receiverUsername: string
}

export interface ChatReadReceiptEvent {
  eventType: 'read'
  channelType: 'friend' | 'support'
  channelId: string
  readerUserId: string
  readerUsername: string
  readAt: string
}

export interface VideoInviteEvent {
  eventType: 'video_invite'
  channelType: 'friend' | 'support'
  channelId: string
  sessionId: string
  roomId: string
  initiatorUsername: string
  receiverUsername: string
  createdAt: string
}

export interface VideoSessionStatusEvent {
  eventType: 'video_session_status'
  channelType: 'friend' | 'support'
  channelId: string
  sessionId: string
  roomId: string
  status: VideoSessionItem['status']
  startedAt: string
  endedAt: string
  updatedAt: string
}

export interface PresenceEvent {
  eventType: 'presence'
  channelType: 'friend' | 'support'
  channelId: string
  userId: string
  online: boolean
}

export type ChatRealtimePayload = ChatMessage | ChatReadReceiptEvent | VideoInviteEvent | VideoSessionStatusEvent | PresenceEvent

export interface FriendProfile {
  id: string
  username: string
  displayName: string
  phone: string
  status: 'online' | 'offline' | 'blocked'
  tags: string[]
  messages: ChatMessage[]
  unreadCount: number
  blockedAt?: string
}

export interface FriendRequest {
  friendshipId: string
  username: string
  displayName: string
  direction: 'incoming' | 'outgoing'
  status: 'pending' | 'accepted' | 'rejected'
  createdAt: string
}

export interface SearchFriendResult {
  username: string
  displayName: string
  status: 'searchable' | 'accepted' | 'pending_incoming' | 'pending_outgoing' | 'blocked'
}

export interface TransactionItem {
  id: string
  orderNo: string
  cardName: string
  faceValue: string
  payoutAmount: string
  baseAmountUsd?: string
  localAmount?: string
  estimatedLocalAmount?: string
  finalLocalAmount?: string
  currencyCode?: string
  businessRate?: string
  faceCurrencyCode?: string
  faceValueAmount?: string
  quantity?: number
  faceToUsdRate?: string
  status: 'pending' | 'processing' | 'completed' | 'disputed' | 'canceled'
  counterpartyName: string
  counterpartyUsername: string
  friendshipId: string
  note: string
  voucherImageUrl: string
  cancelReason: string
  cancelNote: string
  canceledBy: string
  canceledAt: string
  manualVipPoints?: string
  settlementReason?: string
  settledBy?: string
  settledAt?: string
  createdAt: string
  updatedAt: string
}

export interface CompletedTransactionFeedItem {
  displayName: string
  cardName: string
  payoutAmount: string
  completedAt: string
}

export interface VipSummary {
  level: 'VIP0' | 'VIP1' | 'VIP2' | 'VIP3' | 'VIP4' | 'VIP5' | string
  levelName: string
  points: string
  nextLevel: string
  nextThreshold: string
  remainingPoints: string
  progressPercent: number
  maxLevel: boolean
}

export interface LotteryPrizeItem {
  id: string
  name: string
  prizeType: 'cash' | 'physical' | string
  baseAmountUsd: string
  localAmount: string
  currencyCode: string
  displayAmount: string
  exchangeRate: string
  weight: number
  imageUrl: string
  enabled: boolean
  sortOrder: number
}

export interface LotteryEligibility {
  vipLevel: string
  eligible: boolean
  periodType: 'once' | 'week' | 'day' | string
  periodKey: string
  periodDrawCount: number
  availableChances: number
  nextAvailableAt: string
  message: string
}

export interface VipHolidayRewardItem {
  id: string
  countryCode: string
  holidayCode: string
  holidayName: string
  holidayDate: string
  rewardAmount: string
  currencyCode: string
  enabled: boolean
  claimable: boolean
  claimed: boolean
  updatedAt: string
  updatedBy: string
}

export interface VipBenefitSummary {
  vipLevel: string
  birthDate: string
  birthdayLocked: boolean
  birthdayEligible: boolean
  birthdayRewardNgn: string
  birthdayRewardDisplay: string
  supportRedPacketEligible: boolean
  supportRewardDisplay: string
  holidayRewards: VipHolidayRewardItem[]
}

export interface VipBenefitClaimItem {
  id: string
  username: string
  benefitType: 'birthday' | 'support_red_packet' | 'holiday' | string
  periodKey: string
  vipLevel: string
  status: 'pending' | 'approved' | 'rejected' | string
  baseAmountUsd: string
  localAmount: string
  currencyCode: string
  requestedAt: string
  reviewedBy: string
  reviewedAt: string
  reviewNote: string
}

export interface VipBenefitConfigItem {
  vip4SupportAmountNgn: string
  vip5SupportAmountNgn: string
  supportRewardEnabled: boolean
  updatedAt: string
  updatedBy: string
}

export interface LotteryDrawResult {
  eligibility: LotteryEligibility
  prize: LotteryPrizeItem
  recordId: string
  drawnAt: string
}

export interface CountryCodeRule {
  code: string
  countryCode: string
  countryName: string
  currencyCode: string
  currencySymbol: string
  minLocalLength: number
  maxLocalLength: number
  enabled: boolean
  sortOrder: number
}

export interface LotteryWinnerItem {
  displayName: string
  prizeName: string
  drawnAt: string
  displayOnly: boolean
}

export interface LotteryRecordItem {
  id: string
  username: string
  vipLevel: string
  prizeName: string
  prizeType: string
  baseAmountUsd: string
  localAmount: string
  currencyCode: string
  displayAmount: string
  exchangeRate: string
  periodType: string
  periodKey: string
  fulfillmentStatus: string
  processedBy: string
  processedAt: string
  drawnAt: string
}

export interface HiddenRecordItem {
  id: string
  targetType: 'order' | 'message' | 'conversation' | string
  targetId: string
  hiddenScope: string
  createdAt: string
  restoredAt: string
}

export interface SellOrderPayload {
  cardName: string
  cardCountry: string
  settlementCountry: string
  faceValue: number
  quantity: number
  rate: string
  settlementAmount: string
  cardType: string
  speed: string
  cardData?: string
  note?: string
  voucherImageUrl?: string
  sendChatMessage?: boolean
  clientRequestId?: string
}

export interface WithdrawalItem {
  id: string
  requestNo: string
  sourceType: 'wallet' | 'lottery_cash'
  ownerUsername: string
  lotteryRecordId: string
  prizeName: string
  prizeType: string
  amount: string
  currencyCode: string
  country: string
  accountName: string
  bankName: string
  accountNumber: string
  contact: string
  note: string
  status: 'pending' | 'completed' | 'rejected'
  assignedAgent: string
  createdAt: string
  updatedAt: string
}

export interface LotteryFulfillmentItem {
  id: string
  orderNo: string
  ownerUsername: string
  lotteryRecordId: string
  prizeName: string
  prizeType: string
  recipientName: string
  phone: string
  country: string
  addressLine: string
  status: 'pending' | 'completed'
  assignedAgent: string
  createdAt: string
  updatedAt: string
}

export interface BankAccountItem {
  id: string
  ownerUsername: string
  country: string
  accountName: string
  bankName: string
  accountNumber: string
  maskedAccountNumber: string
  status: string
  createdAt: string
}

export interface LoanApplicationItem {
  id: string
  applicationNo: string
  ownerUsername: string
  amount: string
  country: string
  purpose: string
  contact: string
  repaymentPlan: string
  status: 'pending' | 'approved' | 'rejected'
  reviewNote: string
  assignedAgent: string
  reviewer: string
  createdAt: string
  updatedAt: string
}

export interface BroadcastItem {
  id: string
  senderUsername: string
  senderRole: 'agent' | 'admin'
  scope: 'own' | 'all'
  messageType: 'text' | 'image' | 'video' | 'voice' | 'gif' | 'link'
  content: string
  mediaUrl: string
  deliveredCount: number
  countryCodes: string
  searchKeyword: string
  targetMode: 'filter' | 'explicit' | string
  targetUsernames: string
  createdAt: string
}

export interface BalanceSummary {
  scope: 'self' | 'own' | 'all'
  currencyCode: string
  availableTotal: string
  pendingTotal: string
  pendingWithdrawalTotal: string
  withdrawnTotal: string
  userCount: number
}

export interface SupportLedgerCustomer {
  conversationId: string
  customerUsername: string
  displayName: string
  assignedAgent: string
  availableTotal: string
  pendingTotal: string
  pendingWithdrawalTotal: string
  withdrawnTotal: string
  orderCount: number
  pendingOrderCount: number
  withdrawalCount: number
  updatedAt: string
}

export interface SupportLedgerReport {
  summary: BalanceSummary
  customers: SupportLedgerCustomer[]
}

export interface CustomerBalanceSummary {
  availableTotal: string
  pendingTotal: string
  pendingWithdrawalTotal: string
  withdrawnTotal: string
}

export interface VideoSessionItem {
  id: string
  roomId: string
  channelType: 'support' | 'friend'
  channelId: string
  initiatorUsername: string
  receiverUsername: string
  vendor: string
  status: 'created' | 'joining' | 'active' | 'ended' | 'missed' | 'rejected'
  startedAt: string
  endedAt: string
  createdAt: string
  updatedAt: string
}

export interface VideoSessionBootstrap {
  session: VideoSessionItem
  sdkAppId: string
  userId: string
  userSig: string
  sdkConfigured: boolean
  vendor: string
  note: string
}

export interface RankingEntry {
  rank: number
  username: string
  displayName: string
  avatarUrl: string
  score: string
  reward: string
  currentUser: boolean
}

export interface RankingBoard {
  mode: 'sales' | 'invitation'
  month: string
  leaders: RankingEntry[]
  currentUser: RankingEntry
}

export interface NotificationItem {
  id: string
  eventType: string
  title: string
  body: string
  targetType: string
  targetId: string
  read: boolean
  createdAt: string
}

export interface PushDeviceItem {
  id: string
  platform: 'ios' | 'android' | string
  provider: 'tencent' | 'unipush' | string
  deviceModel: string
  appVersion: string
  enabled: boolean
  lastSeenAt: string
}

export interface UploadAsset {
  id: string
  originalName: string
  mimeType: string
  publicUrl: string
  sizeBytes: number
  createdAt: string
}

export interface AdminUserItem {
  id: string
  username: string
  email: string
  phone: string
  role: 'USER' | 'AGENT' | 'ADMIN'
  status: string
  blacklisted: boolean
  vipLevel: string
  vipPoints: string
  createdAt: string
}

export interface AgentItem {
  id: string
  username: string
  email: string
  phone: string
  status: string
  assignedConversationCount: number
  welcomeMessage: string
  welcomeMessageEnabled: boolean
  welcomeMessageUpdatedAt: string
  welcomeMessageUpdatedBy: string
}

export interface SupportConversationItem {
  conversationId: string
  customerUsername: string
  customerAvatarUrl?: string
  customerPhone?: string
  phoneCountryCode?: string
  vipLevel?: string
  vipPoints?: string
  assignmentStatus: string
  assignedAgent: string
  agentNote: string
  messages: ChatMessage[]
  unreadCount: number
  lastMessageTime?: string
  online?: boolean
}

export interface SupportCustomerInfo {
  id: string
  username: string
  avatarUrl?: string
  email: string
  phone: string
  phoneCountryCode: string
  status: string
  agentNote: string
  online: boolean
  assignedAgent: string
  createdAt: string
  updatedAt: string
}

export interface SupportCustomerSearchResult {
  conversationId: string
  customerUsername: string
  displayName: string
  phone: string
  phoneCountryCode: string
  email: string
  vipLevel: string
  vipPoints: string
  unreadCount: number
  lastMessageTime: string
  online: boolean
}

export interface SupportMessageSearchResult {
  conversationId: string
  messageId: string
  customerUsername: string
  displayName: string
  phoneCountryCode: string
  senderRole: string
  snippet: string
  createdAt: string
}

export interface RegistrationBonusRecordItem {
  id: string
  username: string
  phone: string
  countryCode: string
  countryName: string
  currencyCode: string
  bonusAmount: string
  status: string
  reason: string
  createdAt: string
}

export interface BankAccountRiskMatch {
  riskLevel: 'high' | 'medium' | 'low' | string
  reason: string
  username: string
  displayName: string
  phoneCountryCode: string
  assignedAgent: string
  bankName: string
  accountName: string
  accountNumber: string
  submittedAt: string
  fullAccess: boolean
}

export interface SupportCustomerProfile {
  conversationId: string
  customer: SupportCustomerInfo
  balance: CustomerBalanceSummary
  orders: TransactionItem[]
  withdrawals: WithdrawalItem[]
  lotteryFulfillments: LotteryFulfillmentItem[]
  loans: LoanApplicationItem[]
  videoSessions: VideoSessionItem[]
  registrationBonus?: RegistrationBonusRecordItem | null
  bankAccountRiskMatches: BankAccountRiskMatch[]
}

export interface AdminDirectConversationItem {
  friendshipId: string
  requesterUsername: string
  addresseeUsername: string
  status: string
  messages: ChatMessage[]
}

export interface SessionUser {
  username: string
  email?: string
  phone?: string
  avatarUrl?: string
  inviteCode?: string
  countryCode?: string
  countryName?: string
  currencyCode?: string
  currencySymbol?: string
  roleCode?: 'USER' | 'AGENT' | 'ADMIN'
  accessToken?: string
  expiresAt?: string
  nextRoute?: string
}

export interface ReferralRewardConfigItem {
  registrationCashbackEnabled: boolean
  registrationCashbackAmount: string
  tradeRebateEnabled: boolean
  tradeRebatePercent: string
  updatedAt: string
  updatedBy: string
}

export interface ReferralRewardItem {
  id: string
  referrerUsername: string
  referredUsername: string
  tradeOrderNo: string
  rewardType: 'registration' | 'trade_rebate' | string
  amount: string
  ratePercent: string
  status: string
  createdAt: string
}

export interface RegistrationBonusConfigItem {
  id: string
  countryCode: string
  countryName: string
  currencyCode: string
  bonusAmount: string
  enabled: boolean
  note: string
  updatedAt: string
  updatedBy: string
}

export interface CountryOption {
  code: string
  name: string
}

export interface CurrencyExchangeRateItem {
  id: string
  countryCode: string
  countryName: string
  currencyCode: string
  currencySymbol: string
  localCurrencyPerUsd: string
  displayRate: string
  enabled: boolean
  note: string
  updatedAt: string
  updatedBy: string
}
