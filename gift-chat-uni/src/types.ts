export interface RateItem {
  id: string
  cardName: string
  region: string
  rate: string
  status: 'active' | 'paused'
  updatedAt: string
}

export interface ChatMessage {
  id: string
  author: 'me' | 'support' | 'friend' | 'system'
  content: string
  type: 'text' | 'image' | 'voice' | 'gif' | 'link' | 'video'
  createdAt: string
  readState?: 'none' | 'sent' | 'read'
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

export interface PresenceEvent {
  eventType: 'presence'
  channelType: 'friend' | 'support'
  channelId: string
  userId: string
  online: boolean
}

export type ChatRealtimePayload = ChatMessage | ChatReadReceiptEvent | VideoInviteEvent | PresenceEvent

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
  status: 'pending' | 'processing' | 'completed' | 'disputed'
  counterpartyName: string
  counterpartyUsername: string
  friendshipId: string
  note: string
  voucherImageUrl: string
  createdAt: string
  updatedAt: string
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
}

export interface WithdrawalItem {
  id: string
  requestNo: string
  ownerUsername: string
  amount: string
  country: string
  accountName: string
  bankName: string
  accountNumber: string
  contact: string
  note: string
  status: 'pending' | 'completed'
  assignedAgent: string
  createdAt: string
  updatedAt: string
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
  messageType: 'text' | 'image' | 'voice' | 'gif' | 'link'
  content: string
  deliveredCount: number
  createdAt: string
}

export interface BalanceSummary {
  scope: 'self' | 'own' | 'all'
  availableTotal: string
  pendingTotal: string
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
  createdAt: string
}

export interface AgentItem {
  id: string
  username: string
  email: string
  phone: string
  status: string
  assignedConversationCount: number
}

export interface SupportConversationItem {
  conversationId: string
  customerUsername: string
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
  email: string
  phone: string
  status: string
  agentNote: string
  online: boolean
  assignedAgent: string
  createdAt: string
  updatedAt: string
}

export interface SupportCustomerProfile {
  conversationId: string
  customer: SupportCustomerInfo
  balance: CustomerBalanceSummary
  orders: TransactionItem[]
  withdrawals: WithdrawalItem[]
  loans: LoanApplicationItem[]
  videoSessions: VideoSessionItem[]
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
  roleCode?: 'USER' | 'AGENT' | 'ADMIN'
  accessToken?: string
  expiresAt?: string
  nextRoute?: string
}

export interface CountryOption {
  code: string
  name: string
}
