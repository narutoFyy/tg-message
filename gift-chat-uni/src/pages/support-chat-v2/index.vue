<template>
  <view class="chat-container">
    <!-- 左侧客户列表 -->
    <view class="customer-sidebar" :class="{ 'sidebar-hidden': isMobile && showChat }">
      <!-- 顶部搜索栏 -->
      <view class="sidebar-header">
        <view class="search-box">
          <text class="search-icon">🔍</text>
          <input v-model="searchKeyword" class="search-input" placeholder="搜索客户" />
        </view>
      </view>

      <!-- 客户列表 -->
      <scroll-view scroll-y class="customer-list-scroll">
        <view
          v-for="conv in filteredConversations"
          :key="conv.conversationId"
          :class="['customer-item', { 'active': conv.conversationId === activeConversationId }]"
          @click="selectCustomer(conv)"
        >
          <!-- 头像 -->
          <view class="customer-avatar">
            <image class="avatar-img" :src="uiIcons.user" mode="aspectFit" />
            <view :class="['online-dot', { offline: !conv.online }]"></view>
          </view>

          <!-- 客户信息 -->
          <view class="customer-info">
            <view class="info-row">
              <text class="customer-name">{{ customerDisplayName(conv) }}</text>
              <text class="message-time">{{ formatTime(conv.lastMessageTime) }}</text>
            </view>
            <text v-if="conv.agentNote" class="customer-note">{{ conv.customerUsername }}</text>
            <view class="info-row">
              <text class="last-message">{{ getLastMessage(conv) }}</text>
              <view v-if="displayUnreadCount(conv) > 0" class="unread-badge">{{ displayUnreadCount(conv) }}</view>
            </view>
          </view>
        </view>

        <view v-if="filteredConversations.length === 0" class="empty-list">
          <text class="empty-text">暂无客户</text>
        </view>
      </scroll-view>

      <!-- 底部统计栏 -->
      <view class="sidebar-footer">
        <view class="stat-item">
          <text class="stat-label">客户</text>
          <text class="stat-value">{{ balanceSummary?.userCount || 0 }}</text>
        </view>
        <view class="stat-item">
          <text class="stat-label">余额</text>
          <text class="stat-value">{{ balanceSummary?.availableTotal || '0.00' }}</text>
        </view>
      </view>
    </view>

    <!-- 右侧聊天区域 -->
    <view class="chat-main" :class="{ 'chat-hidden': isMobile && !showChat }">
      <!-- 聊天顶部导航 -->
      <view class="chat-header">
        <view class="header-left">
          <view v-if="isMobile" class="back-btn" @click="backToList">
            <text>‹</text>
          </view>
          <image class="header-avatar" :src="uiIcons.user" mode="aspectFit" />
          <view class="header-info">
            <text class="header-name">{{ activeCustomer ? customerDisplayName(activeCustomer) : '选择客户' }}</text>
            <text v-if="activeCustomer?.agentNote" class="header-note">{{ activeCustomer.customerUsername }}</text>
            <text :class="['header-status', { offline: !activeCustomer?.online }]">{{ activeCustomer?.online ? '在线' : '离线' }}</text>
          </view>
        </view>
        <view class="header-actions">
          <view class="action-btn" @click="editCustomerNote">备注</view>
          <view class="action-btn" @click="toggleSound">{{ soundEnabled ? '🔔' : '🔕' }}</view>
          <view class="action-btn" @click="startVideoCall">📹</view>
          <view class="action-btn" @click="showMoreActions">⋯</view>
        </view>
      </view>

      <!-- 聊天消息区域 -->
      <scroll-view scroll-y class="message-area" :scroll-into-view="messageScrollTarget">
        <view class="message-list">
          <view class="date-divider">
            <text>今天</text>
          </view>

          <view
            v-for="msg in conversation"
            :key="msg.id"
            :class="['message-wrapper', isMine(msg) ? 'mine' : 'theirs']"
          >
            <!-- 对方消息显示头像 -->
            <ChatMessageBubble
              :message="msg"
              :mine="isMine(msg)"
              :avatar-src="uiIcons.user"
              :translation="translationFor(msg)"
              :call-title="videoCallTitle(msg)"
              :call-room="videoCallRoom(msg)"
              :call-status="videoCallStatus(msg)"
              :call-status-label="videoCallStatusLabel(msg)"
              :call-caption="videoCallCaption(msg)"
              call-answer-label="接听"
              call-reject-label="拒绝"
              call-enter-label="进入"
              :can-answer-call="canAnswerVideoMessage(msg)"
              :can-reject-call="canRejectVideoMessage(msg)"
              :can-enter-call="canEnterVideoMessage(msg)"
              @preview="previewImage"
              @retry="retryMessage"
              @answer-call="answerVideoMessage"
              @reject-call="rejectVideoMessage"
              @enter-call="enterVideoMessage"
            />
          </view>

          <view id="msg-bottom"></view>
        </view>
      </scroll-view>

      <!-- 粘贴图片预览 -->
      <ComposerAttachmentPreview
        :attachment="activeAttachment"
        @retry="sendPendingAttachment"
        @clear="clearAttachment"
      />

      <!-- 底部输入区域 -->
      <view class="input-area">
        <view class="input-toolbar">
          <view class="tool-btn" @click="sendImage">📷</view>
          <view class="tool-btn" @click="sendGif">GIF</view>
          <view v-if="isAgent" class="tool-btn" @click="sendOwnCustomerBroadcast">📢</view>
        </view>
        <view class="input-row">
          <input v-model="draft" class="message-input" placeholder="输入消息..." @confirm="handleSend" />
          <view class="send-btn" :class="{ 'active': canSend }" @click="handleSend">
            <text>发送</text>
          </view>
        </view>
      </view>
    </view>

    <view class="customer-profile-panel" :class="{ 'profile-hidden': isMobile && showChat }">
      <view class="profile-header">
        <view>
          <text class="profile-eyebrow">{{ workbenchText.customer }}</text>
          <text class="profile-title">{{ profileDisplayName }}</text>
        </view>
        <view class="profile-header-actions">
          <view class="language-toggle" @click="toggleWorkbenchLanguage">{{ workbenchLanguageLabel }}</view>
          <view v-if="profile?.customer.status" class="profile-status">{{ statusText(profile.customer.status) }}</view>
        </view>
      </view>

      <view v-if="store.state.supportCustomerProfileLoading" class="profile-empty">
        <text>{{ workbenchText.loading }}</text>
      </view>

      <scroll-view v-else-if="profile" scroll-y class="profile-scroll">
        <view class="profile-section ledger-section">
          <view class="section-head">
            <text class="section-title">{{ ledgerTitleText }}</text>
            <text class="section-count">{{ supportLedger?.summary.userCount || 0 }}</text>
          </view>
          <view class="ledger-summary-grid">
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.available }}</text>
              <text class="metric-value">{{ supportLedger?.summary.availableTotal || '0.00' }}</text>
            </view>
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.pendingBalance }}</text>
              <text class="metric-value">{{ supportLedger?.summary.pendingTotal || '0.00' }}</text>
            </view>
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.withdrawn }}</text>
              <text class="metric-value">{{ supportLedger?.summary.withdrawnTotal || '0.00' }}</text>
            </view>
          </view>
          <view v-if="store.state.supportLedgerLoading" class="mini-empty">{{ ledgerLoadingText }}</view>
          <view v-else-if="ledgerCustomers.length === 0" class="mini-empty">{{ ledgerEmptyText }}</view>
          <view
            v-for="customer in ledgerCustomers"
            :key="customer.conversationId"
            :class="['ledger-row', { active: customer.conversationId === activeConversationId }]"
            @click="selectLedgerCustomer(customer.conversationId)"
          >
            <view class="ledger-row-main">
              <text class="ledger-name">{{ customer.displayName }}</text>
              <text class="ledger-meta">{{ customer.pendingOrderCount }}/{{ customer.orderCount }} orders</text>
            </view>
            <view class="ledger-row-money">
              <text class="ledger-money">{{ customer.availableTotal }}</text>
              <text class="ledger-pending">{{ customer.pendingTotal }}</text>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.username }}</text>
            <text class="profile-value">@{{ profile.customer.username }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.agent }}</text>
            <text class="profile-value">{{ profile.customer.assignedAgent || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.phone }}</text>
            <text class="profile-value">{{ profile.customer.phone || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.email }}</text>
            <text class="profile-value">{{ profile.customer.email || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.joined }}</text>
            <text class="profile-value">{{ profile.customer.createdAt }}</text>
          </view>
        </view>

        <view class="profile-section metric-grid">
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.available }}</text>
            <text class="metric-value">{{ profile.balance.availableTotal }}</text>
          </view>
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.pendingBalance }}</text>
            <text class="metric-value">{{ profile.balance.pendingTotal }}</text>
          </view>
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.withdrawn }}</text>
            <text class="metric-value">{{ profile.balance.withdrawnTotal }}</text>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.orders }}</text>
            <text class="section-count">{{ profile.orders.length }}</text>
          </view>
          <view v-if="profile.orders.length === 0" class="mini-empty">{{ workbenchText.noOrders }}</view>
          <view v-else class="order-focus">
            <picker mode="selector" :range="orderPickerOptions" :value="selectedOrderIndex" @change="handleOrderPick">
              <view class="order-picker">
                <text class="order-picker-label">{{ orderSelectorText }}</text>
                <text class="order-picker-value">{{ selectedOrder?.orderNo }}</text>
                <text class="order-picker-arrow">⌄</text>
              </view>
            </picker>
          </view>
          <view v-if="selectedOrder" class="work-item selected-work-item">
            <view class="work-top">
              <text class="work-title">{{ selectedOrder.orderNo }}</text>
              <text :class="['work-status', selectedOrder.status]">{{ statusText(selectedOrder.status) }}</text>
            </view>
            <text class="work-line">{{ selectedOrder.cardName }} / {{ selectedOrder.faceValue }}</text>
            <text class="work-line strong">{{ selectedOrder.payoutAmount }}</text>
            <view class="work-actions">
              <button v-if="selectedOrder.status === 'pending'" class="mini-btn" @click="changeOrderStatus(selectedOrder.id, 'processing')">{{ workbenchText.process }}</button>
              <button v-if="selectedOrder.status === 'pending' || selectedOrder.status === 'processing'" class="mini-btn primary" @click="changeOrderStatus(selectedOrder.id, 'completed')">{{ workbenchText.complete }}</button>
              <button v-if="selectedOrder.status === 'pending' || selectedOrder.status === 'processing'" class="mini-btn danger" @click="changeOrderStatus(selectedOrder.id, 'disputed')">{{ workbenchText.dispute }}</button>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.withdrawals }}</text>
            <text class="section-count">{{ profile.withdrawals.length }}</text>
          </view>
          <view v-if="profile.withdrawals.length === 0" class="mini-empty">{{ workbenchText.noWithdrawals }}</view>
          <view v-for="item in profile.withdrawals.slice(0, 4)" :key="item.id" class="work-item">
            <view class="work-top">
              <text class="work-title">{{ item.requestNo }}</text>
              <text :class="['work-status', item.status]">{{ statusText(item.status) }}</text>
            </view>
            <text class="work-line">{{ item.amount }} / {{ item.country }}</text>
            <text class="work-line">{{ item.bankName }} {{ item.accountNumber }}</text>
            <view v-if="item.status === 'pending'" class="work-actions">
              <button class="mini-btn primary" @click="changeWithdrawalStatus(item.id, 'completed')">{{ workbenchText.markPaid }}</button>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.loans }}</text>
            <text class="section-count">{{ profile.loans.length }}</text>
          </view>
          <view v-if="profile.loans.length === 0" class="mini-empty">{{ workbenchText.noLoans }}</view>
          <view v-for="loan in profile.loans.slice(0, 4)" :key="loan.id" class="work-item">
            <view class="work-top">
              <text class="work-title">{{ loan.applicationNo }}</text>
              <text :class="['work-status', loan.status]">{{ statusText(loan.status) }}</text>
            </view>
            <text class="work-line">{{ loan.amount }} / {{ loan.country }}</text>
            <text class="work-line">{{ loan.purpose }}</text>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.calls }}</text>
            <text class="section-count">{{ profile.videoSessions.length }}</text>
          </view>
          <view v-if="profile.videoSessions.length === 0" class="mini-empty">{{ workbenchText.noCalls }}</view>
          <view v-for="call in profile.videoSessions.slice(0, 3)" :key="call.id" class="work-item compact">
            <view class="work-top">
              <text class="work-title">{{ call.roomId }}</text>
              <text :class="['work-status', call.status]">{{ statusText(call.status) }}</text>
            </view>
            <text class="work-line">{{ call.createdAt }}</text>
          </view>
        </view>
      </scroll-view>

      <view v-else class="profile-empty">
        <text>{{ workbenchText.selectCustomer }}</text>
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
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import ChatMessageBubble from '@/components/chat/ChatMessageBubble.vue'
import ComposerAttachmentPreview from '@/components/chat/ComposerAttachmentPreview.vue'
import { useComposerAttachments, type ComposerAttachmentKind } from '@/components/chat/useComposerAttachments'
import { createBroadcast, translateToChinese, uploadImage } from '@/utils/api'
import { connectChatSocket } from '@/utils/realtime'
import { uiIcons } from '@/utils/art'
import type { ChatMessage, PresenceEvent, SupportConversationItem, VideoCallMessagePayload, VideoInviteEvent, VideoSessionItem, VideoSessionStatusEvent } from '@/types'
import type { ChatRealtimePayload, ChatReadReceiptEvent } from '@/types'
import type { TransactionItem, WithdrawalItem } from '@/types'

const store = useAppStore()
const draft = ref('')
const searchKeyword = ref('')
const socketTask = ref<UniApp.SocketTask | null>(null)
const socketStatus = ref<'connecting' | 'online' | 'offline'>('connecting')
const presenceRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const readRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
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
const SOUND_ENABLED_KEY = 'support-chat-sound-enabled'
const WORKBENCH_LANGUAGE_KEY = 'support-workbench-language'
const audioEnabled = ref(uni.getStorageSync(SOUND_ENABLED_KEY) !== false)
const audioUnlocked = ref(false)
const workbenchLanguage = ref<'zh' | 'en'>((uni.getStorageSync(WORKBENCH_LANGUAGE_KEY) || 'zh') as 'zh' | 'en')
const showChat = ref(false)
const isMobile = ref(false)
const activeConversationId = ref('')
const handledVideoInvites = new Set<string>()
const localVideoStatuses = ref<Record<string, VideoSessionItem['status']>>({})
const incomingVideoInvite = ref<VideoInviteEvent | null>(null)
const translations = reactive<Record<string, string>>({})
const translatingIds = new Set<string>()
const pendingRouteConversationId = ref('')

const conversation = computed(() => store.state.supportMessages)
const isAgent = computed(() => store.state.currentUser?.roleCode === 'AGENT')
const balanceSummary = computed(() => store.state.balanceSummary)
const canSend = computed(() => draft.value.trim().length > 0 || hasAttachment.value)

const filteredConversations = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return store.state.supportConversations
  return store.state.supportConversations.filter(conv =>
    conv.customerUsername.toLowerCase().includes(keyword)
  )
})

const activeCustomer = computed(() =>
  store.state.supportConversations.find(c => c.conversationId === activeConversationId.value)
)
const soundEnabled = computed(() => audioEnabled.value)
const profile = computed(() => store.state.activeSupportCustomerProfile)
const supportLedger = computed(() => store.state.supportLedger)
const ledgerCustomers = computed(() => supportLedger.value?.customers.slice(0, 6) || [])
const selectedOrderId = ref('')
const selectedOrder = computed(() => {
  const orders = profile.value?.orders || []
  return orders.find(order => order.id === selectedOrderId.value) || orders[0] || null
})
const selectedOrderIndex = computed(() => {
  const orders = profile.value?.orders || []
  const index = orders.findIndex(order => order.id === selectedOrder.value?.id)
  return index >= 0 ? index : 0
})
const orderPickerOptions = computed(() =>
  (profile.value?.orders || []).map(order => `${order.orderNo} - ${statusText(order.status)}`)
)
const orderSelectorText = computed(() => workbenchLanguage.value === 'zh' ? '选择订单' : 'Select order')
const ledgerTitleText = computed(() => workbenchLanguage.value === 'zh' ? '客户账务' : 'Ledger')
const ledgerLoadingText = computed(() => workbenchLanguage.value === 'zh' ? '正在加载账务...' : 'Loading ledger...')
const ledgerEmptyText = computed(() => workbenchLanguage.value === 'zh' ? '暂无账务数据' : 'No ledger data')
const workbenchCopy = {
  zh: {
    customer: '客户',
    username: '用户名',
    agent: '客服',
    phone: '电话',
    email: '邮箱',
    joined: '加入时间',
    available: '可用余额',
    pendingBalance: '待结算',
    withdrawn: '已提现',
    orders: '订单',
    withdrawals: '提现',
    loans: '贷款',
    calls: '通话',
    noOrders: '暂无订单',
    noWithdrawals: '暂无提现',
    noLoans: '暂无贷款',
    noCalls: '暂无通话',
    process: '处理中',
    complete: '完成',
    dispute: '争议',
    markPaid: '标记已付款',
    loading: '正在加载客户资料...',
    selectCustomer: '选择客户后查看订单、余额、提现和贷款。',
    orderUpdated: '订单已更新',
    orderUpdateFailed: '订单更新失败',
    withdrawalUpdated: '提现已更新',
    withdrawalUpdateFailed: '提现更新失败',
    noCustomer: '未选择客户',
    switchTo: 'English'
  },
  en: {
    customer: 'Customer',
    username: 'Username',
    agent: 'Agent',
    phone: 'Phone',
    email: 'Email',
    joined: 'Joined',
    available: 'Available',
    pendingBalance: 'Pending',
    withdrawn: 'Withdrawn',
    orders: 'Orders',
    withdrawals: 'Withdrawals',
    loans: 'Loans',
    calls: 'Calls',
    noOrders: 'No orders',
    noWithdrawals: 'No withdrawals',
    noLoans: 'No loans',
    noCalls: 'No calls',
    process: 'Process',
    complete: 'Complete',
    dispute: 'Dispute',
    markPaid: 'Mark paid',
    loading: 'Loading customer details...',
    selectCustomer: 'Select a customer to view orders, balance, withdrawals, and loans.',
    orderUpdated: 'Order updated',
    orderUpdateFailed: 'Order update failed',
    withdrawalUpdated: 'Withdrawal updated',
    withdrawalUpdateFailed: 'Withdrawal update failed',
    noCustomer: 'No customer selected',
    switchTo: '中文'
  }
}
const statusCopy: Record<string, { zh: string; en: string }> = {
  active: { zh: '正常', en: 'Active' },
  pending: { zh: '待处理', en: 'Pending' },
  processing: { zh: '处理中', en: 'Processing' },
  completed: { zh: '已完成', en: 'Completed' },
  disputed: { zh: '争议', en: 'Disputed' },
  approved: { zh: '已通过', en: 'Approved' },
  rejected: { zh: '已拒绝', en: 'Rejected' },
  joining: { zh: '接入中', en: 'Joining' },
  ended: { zh: '已结束', en: 'Ended' },
  missed: { zh: '未接通', en: 'Missed' }
}
const workbenchText = computed(() => workbenchCopy[workbenchLanguage.value])
const workbenchLanguageLabel = computed(() => workbenchText.value.switchTo)
const profileDisplayName = computed(() => {
  if (profile.value) {
    return profile.value.customer.agentNote || profile.value.customer.username
  }
  return activeCustomer.value ? customerDisplayName(activeCustomer.value) : workbenchText.value.noCustomer
})

onLoad((query) => {
  pendingRouteConversationId.value = typeof query?.conversationId === 'string' ? query.conversationId : ''
})

onShow(() => {
  checkMobile()
  store.bootstrap().then(() => {
    applyPendingSupportDraft()
    if (store.state.supportConversations.length > 0) {
      const routeConversation = store.state.supportConversations.find(item => item.conversationId === pendingRouteConversationId.value)
      activeConversationId.value = routeConversation?.conversationId || store.state.supportConversations[0].conversationId
      pendingRouteConversationId.value = ''
      store.setActiveSupportConversation(activeConversationId.value)
      store.refreshSupportCustomerProfile(activeConversationId.value).catch(() => {})
      connectSocket()
      // 页面加载时标记第一个客户的消息为已读
      store.markSupportRead().catch(() => {})
    }
  })
})

onMounted(() => {
  attachPasteListener()
  startPresenceRefresh()
  window.addEventListener('resize', checkMobile)
})

watch(
  () => conversation.value.map((message) => `${message.id}:${message.content}`).join('|'),
  () => {
    translateVisibleIncomingMessages()
  }
)

watch(
  () => conversation.value.length,
  () => {
    scrollMessagesToBottom()
  }
)

watch(
  () => profile.value?.orders.map(order => order.id).join('|') || '',
  () => {
    const orders = profile.value?.orders || []
    if (orders.length === 0) {
      selectedOrderId.value = ''
      return
    }
    if (!orders.some(order => order.id === selectedOrderId.value)) {
      selectedOrderId.value = orders[0].id
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  stopPresenceRefresh()
  stopReadRefresh()
  closeSocket()
  detachPasteListener()
  window.removeEventListener('resize', checkMobile)
})

function checkMobile() {
  // #ifdef H5
  isMobile.value = window.innerWidth < 768
  // #endif
}

async function selectCustomer(conv: SupportConversationItem) {
  activeConversationId.value = conv.conversationId
  store.setActiveSupportConversation(conv.conversationId)
  store.clearSupportUnread(conv.conversationId)
  store.refreshSupportCustomerProfile(conv.conversationId).catch((error) => {
    console.error('Load customer profile failed:', error)
  })
  showChat.value = true
  enableAudio()
  connectSocket()

  // 标记消息为已读，清除未读角标
  try {
    await store.markSupportRead()
  } catch (error) {
    console.error('标记已读失败:', error)
    uni.showToast({ title: '标记已读失败', icon: 'none' })
  }
}

function selectLedgerCustomer(conversationId: string) {
  const conversation = store.state.supportConversations.find(item => item.conversationId === conversationId)
  if (conversation) {
    selectCustomer(conversation)
  }
}

async function changeOrderStatus(orderId: string, status: TransactionItem['status']) {
  try {
    await store.updateTransactionStatus(orderId, status)
    uni.showToast({ title: workbenchText.value.orderUpdated, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : workbenchText.value.orderUpdateFailed, icon: 'none' })
  }
}

async function changeWithdrawalStatus(withdrawalId: string, status: WithdrawalItem['status']) {
  try {
    await store.updateWithdrawalStatus(withdrawalId, status)
    uni.showToast({ title: workbenchText.value.withdrawalUpdated, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : workbenchText.value.withdrawalUpdateFailed, icon: 'none' })
  }
}

function toggleWorkbenchLanguage() {
  workbenchLanguage.value = workbenchLanguage.value === 'zh' ? 'en' : 'zh'
  uni.setStorageSync(WORKBENCH_LANGUAGE_KEY, workbenchLanguage.value)
}

function statusText(status?: string) {
  const key = (status || '').toLowerCase()
  return statusCopy[key]?.[workbenchLanguage.value] || status || '-'
}

function handleOrderPick(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  const order = profile.value?.orders[index]
  if (order) {
    selectedOrderId.value = order.id
  }
}

function backToList() {
  showChat.value = false
}

function getLastMessage(conv: SupportConversationItem) {
  const last = conv.messages[conv.messages.length - 1]
  if (!last) return '暂无消息'
  if (last.type === 'image') return '[图片]'
  if (last.type === 'gif') return '[GIF]'
  if (last.type === 'voice') return '[语音]'
  if (last.type === 'video') return '[视频通话]'
  return last.content.length > 20 ? `${last.content.slice(0, 20)}...` : last.content
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
      channelId: activeConversationId.value,
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
    created: '等待接听',
    joining: '正在加入',
    active: '通话中',
    ended: '已结束',
    missed: '未接听',
    rejected: '已拒绝'
  }[videoCallStatus(message)]
}

function videoCallCaption(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  if (!payload) return '视频通话邀请格式异常，请刷新后重试。'
  if (status === 'active') return '通话正在进行中，可继续进入同一房间。'
  if (status === 'created') return '对方尚未接听，客服和客户都可以在有效期内进入。'
  if (status === 'joining') return '对方正在加入，请稍候。'
  if (status === 'missed') return '本次呼叫未接通，可以重新发起视频通话。'
  if (status === 'rejected') return '本次呼叫已被拒绝。'
  return '本次视频通话已结束。'
}

function videoCallTitle(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const currentUsername = store.state.currentUser?.username
  if (!payload) return '视频通话邀请'
  if (payload.initiatorUsername === currentUsername) return '你发起了视频通话'
  return `${payload.initiatorUsername || '对方'} 正在邀请你视频通话`
}

function videoCallRoom(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  return `房间 ${payload?.roomId || '-'}`
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
    uni.showToast({ title: error instanceof Error ? error.message : '无法加入通话', icon: 'none' })
  }
}

async function rejectVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    const updated = await store.updateVideoSessionStatus(payload.sessionId, 'rejected')
    setLocalVideoStatus(payload.sessionId, updated.status)
    uni.showToast({ title: '已拒绝视频通话', icon: 'none' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '拒绝失败', icon: 'none' })
  }
}

async function enterVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    await openVideoSession(payload.sessionId)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '无法加入通话', icon: 'none' })
  }
}

async function openVideoSession(sessionId: string) {
  const bootstrap = await store.getVideoSessionBootstrap(sessionId)
  const encoded = encodeURIComponent(JSON.stringify(bootstrap))
  uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
}

function customerDisplayName(conv: SupportConversationItem) {
  return conv.agentNote?.trim() || conv.customerUsername
}

function displayUnreadCount(conv: SupportConversationItem) {
  return conv.conversationId === activeConversationId.value ? 0 : conv.unreadCount
}

function editCustomerNote() {
  const customer = activeCustomer.value
  if (!customer) {
    uni.showToast({ title: '请先选择客户', icon: 'none' })
    return
  }
  uni.showModal({
    title: '客户备注',
    editable: true,
    placeholderText: '输入客户备注',
    content: customer.agentNote || '',
    confirmText: '保存',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.updateSupportNote(customer.conversationId, result.content || '')
        uni.showToast({ title: '备注已保存', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : '备注保存失败', icon: 'none' })
      }
    }
  })
}

function formatTime(time?: string) {
  // 简化时间显示逻辑
  return time || ''
}

function isMine(message: ChatMessage) {
  return message.author === 'me'
}

function translationFor(message: ChatMessage) {
  if (!isAgent.value || message.author === 'me' || message.author === 'system' || message.type !== 'text') {
    return ''
  }
  return translations[message.id] || ''
}

function shouldTranslate(message: ChatMessage) {
  return isAgent.value
    && message.author !== 'me'
    && message.author !== 'system'
    && message.type === 'text'
    && !!message.content.trim()
    && !translations[message.id]
    && !translatingIds.has(message.id)
}

function translateVisibleIncomingMessages() {
  conversation.value
    .filter(shouldTranslate)
    .forEach((message) => {
      translatingIds.add(message.id)
      translateToChinese(message.content)
        .then((result) => {
          if (result.translatedText && result.translatedText.trim() !== message.content.trim()) {
            translations[message.id] = result.translatedText
          }
        })
        .catch(() => {
          // Translation is a helper; chat should stay usable if the free service is unavailable.
        })
        .finally(() => {
          translatingIds.delete(message.id)
        })
    })
}

function connectSocket() {
  closeSocket()
  const conversationId = activeConversationId.value
  if (!conversationId) return

  socketStatus.value = 'connecting'
  try {
    socketTask.value = connectChatSocket('support', conversationId, (payload) => {
      if (isReadReceipt(payload)) {
        if (payload.readerUsername !== store.state.currentUser?.username) {
          store.applySupportReadReceipt(conversationId)
        }
        return
      }

      if (isPresenceEvent(payload)) {
        store.applySupportPresence(payload.channelId, payload.online)
        refreshPresence()
        return
      }

      // 处理视频邀请
      if (isVideoInvite(payload)) {
        handleVideoInvite(payload)
        return
      }

      if (isVideoSessionStatus(payload)) {
        handleVideoSessionStatus(payload)
        return
      }

      // 处理普通消息
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
        refreshPresence()
      },
      onClose: () => {
        socketStatus.value = 'offline'
        refreshPresence()
      },
      onError: () => {
        socketStatus.value = 'offline'
        refreshPresence()
      },
      onReconnect: () => {
        store.recoverSupportMessages(conversationId).catch(() => {})
      }
    })
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    socketStatus.value = 'offline'
  }
}

function startPresenceRefresh() {
  stopPresenceRefresh()
  presenceRefreshTimer.value = setInterval(() => {
    refreshPresence()
  }, 5000)
}

function stopPresenceRefresh() {
  if (presenceRefreshTimer.value) {
    clearInterval(presenceRefreshTimer.value)
    presenceRefreshTimer.value = null
  }
}

function refreshPresence() {
  store.refreshSupport().catch(() => {})
}

function scrollMessagesToBottom() {
  nextTick(() => {
    messageScrollTarget.value = ''
    nextTick(() => {
      messageScrollTarget.value = 'msg-bottom'
    })
  })
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
      // keep current chat state if refresh fails
    }
  }, 1500)
}

function closeSocket() {
  socketTask.value?.close({})
  socketTask.value = null
}

function shouldPlayIncomingSound(message: ChatRealtimePayload, conversationId: string) {
  // 视频邀请不播放提示音
  if ('eventType' in message) return false
  return isAgent.value && message.author !== 'me' && message.author !== 'system' && conversationId
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
  const updated = store.applyVideoSessionStatus(event)
  setLocalVideoStatus(event.sessionId, updated?.status || event.status)
  if (incomingVideoInvite.value?.sessionId === event.sessionId && isTerminalVideoStatus(event.status)) {
    incomingVideoInvite.value = null
  }
}

function handleVideoInvite(invite: VideoInviteEvent) {
  if (invite.channelId !== activeConversationId.value) return
  if (handledVideoInvites.has(invite.sessionId)) return
  handledVideoInvites.add(invite.sessionId)

  const currentUsername = store.state.currentUser?.username
  if (!currentUsername || invite.initiatorUsername === currentUsername) return

  incomingVideoInvite.value = invite
}

function isTerminalVideoStatus(status: VideoSessionItem['status']) {
  return ['ended', 'missed', 'rejected'].includes(status)
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
      uni.showToast({ title: 'Call is no longer available', icon: 'none' })
      return
    }
    await openVideoSession(invite.sessionId)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Unable to join call', icon: 'none' })
  }
}

function enableAudio() {
  audioUnlocked.value = true
}

function toggleSound() {
  audioEnabled.value = !audioEnabled.value
  uni.setStorageSync(SOUND_ENABLED_KEY, audioEnabled.value)
  uni.showToast({
    title: audioEnabled.value ? '提示音已开启' : '提示音已关闭',
    icon: 'none'
  })
}

function playIncomingSound() {
  if (!audioEnabled.value || !audioUnlocked.value) return
  // #ifdef H5
  try {
    const context = new (window.AudioContext || (window as any).webkitAudioContext)()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = 880
    gain.gain.value = 0.06
    oscillator.connect(gain)
    gain.connect(context.destination)
    oscillator.start()
    oscillator.stop(context.currentTime + 0.16)
  } catch {}
  // #endif
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
  const item = Array.from(event.clipboardData?.items || []).find(entry => entry.type.startsWith('image/'))
  const file = item?.getAsFile()
  if (!file) return

  event.preventDefault()
  try {
    addFile(file)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Unsupported image', icon: 'none' })
  }
}

async function handleSend() {
  if (activeAttachment.value) {
    await sendPendingAttachment()
    return
  }
  const value = draft.value.trim()
  if (!value) return

  try {
    await store.sendSupport(value)
    draft.value = ''
    scrollMessagesToBottom()
    startReadRefresh()
  } catch (error) {
    uni.showToast({ title: 'Send failed. Tap Retry.', icon: 'none' })
  }
}

async function retryMessage(message: ChatMessage) {
  try {
    await store.retrySupportMessage(message.id)
    scrollMessagesToBottom()
    startReadRefresh()
  } catch (error) {
    uni.showToast({ title: 'Retry failed', icon: 'none' })
  }
}

async function sendPendingAttachment() {
  const attachment = activeAttachment.value
  if (!attachment || isAttachmentUploading.value) return
  setStatus(attachment.id, 'uploading')
  try {
    const asset = await uploadImage(attachment.url)
    await store.sendSupport(asset.publicUrl, attachment.kind)
    scrollMessagesToBottom()
    startReadRefresh()
    clearAttachment(attachment.id)
    if (draft.value.trim()) {
      await handleSend()
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Send failed'
    setStatus(attachment.id, 'failed', message)
    uni.showToast({ title: message, icon: 'none' })
  }
}

async function sendImage() {
  try {
    const filePath = await chooseImageOnce('image')
    if (!filePath) return
    addPath(filePath, 'image')
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '图片选择失败', icon: 'none' })
  }
}

async function sendGif() {
  try {
    const filePath = await chooseImageOnce('gif')
    if (!filePath) return
    addPath(filePath, 'gif')
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'GIF选择失败', icon: 'none' })
  }
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

async function sendOwnCustomerBroadcast() {
  const content = draft.value.trim()
  if (!content) {
    uni.showToast({ title: '请输入群发内容', icon: 'none' })
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
    uni.showToast({ title: `已群发给 ${broadcast.deliveredCount} 位客户`, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: '群发失败', icon: 'none' })
  }
}

async function startVideoCall() {
  if (!activeConversationId.value) {
    uni.showToast({ title: '请先选择客户', icon: 'none' })
    return
  }
  try {
    const bootstrap = await store.createVideoSession({
      channelType: 'support',
      channelId: activeConversationId.value
    })
    setLocalVideoStatus(bootstrap.session.id, bootstrap.session.status)
    await store.refreshSupport().catch(() => {})
    const encoded = encodeURIComponent(JSON.stringify(bootstrap))
    uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '视频通话启动失败',
      icon: 'none'
    })
  }
}

function showMoreActions() {
  if (!activeConversationId.value) {
    uni.showToast({ title: '请先选择客户', icon: 'none' })
    return
  }

  uni.showActionSheet({
    itemList: ['视频通话', '查看客户资料', '清空聊天记录'],
    success(result) {
      if (result.tapIndex === 0) {
        startVideoCall()
      } else if (result.tapIndex === 1) {
        uni.showToast({ title: '客户资料功能开发中', icon: 'none' })
      } else if (result.tapIndex === 2) {
        uni.showModal({
          title: '确认清空',
          content: '确定要清空与该客户的聊天记录吗？',
          success(res) {
            if (res.confirm) {
              uni.showToast({ title: '清空功能开发中', icon: 'none' })
            }
          }
        })
      }
    }
  })
}

function previewImage(url: string) {
  uni.previewImage({ urls: [url], current: url })
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  background: linear-gradient(180deg, #b7efb0 0%, #d7f3c3 18%, #d6ecbb 44%, #d8efc6 100%);
}

/* ============ 左侧客户列表 ============ */
.customer-sidebar {
  width: 300px;
  flex: 0 0 300px;
  min-width: 240px;
  background: rgba(255, 255, 255, 0.95);
  border-right: 1px solid rgba(90, 123, 89, 0.2);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s;
  backdrop-filter: blur(10px);
}

.customer-profile-panel {
  width: 340px;
  flex: 0 1 340px;
  min-width: 280px;
  background: rgba(255, 255, 255, 0.94);
  border-left: 1px solid rgba(90, 123, 89, 0.2);
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(10px);
}

.profile-hidden {
  display: none;
}

.profile-header {
  min-height: 82px;
  padding: 16px;
  border-bottom: 1px solid rgba(90, 123, 89, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.profile-header-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.language-toggle {
  min-width: 58px;
  height: 26px;
  padding: 0 9px;
  border-radius: 8px;
  border: 1px solid rgba(18, 201, 107, 0.25);
  background: rgba(18, 201, 107, 0.08);
  color: #0a7a44;
  font-size: 12px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.language-toggle:active {
  background: rgba(18, 201, 107, 0.18);
}

.profile-eyebrow {
  display: block;
  font-size: 11px;
  color: #6c7d6f;
  text-transform: uppercase;
  font-weight: 800;
}

.profile-title {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: #1f2d24;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.profile-status {
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(18, 201, 107, 0.12);
  color: #0a7a44;
  font-size: 12px;
  font-weight: 800;
}

.profile-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.profile-section {
  padding: 12px 0;
  border-bottom: 1px solid rgba(90, 123, 89, 0.12);
}

.profile-section:first-child {
  padding-top: 0;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  min-width: 0;
}

.profile-label,
.metric-label {
  font-size: 12px;
  color: #718075;
  flex-shrink: 0;
}

.profile-value {
  min-width: 0;
  max-width: 100%;
  font-size: 13px;
  color: #243027;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric-item {
  min-width: 0;
  padding: 10px 8px;
  border-radius: 8px;
  background: rgba(225, 245, 220, 0.72);
}

.metric-value {
  display: block;
  margin-top: 5px;
  font-size: 15px;
  color: #173321;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.ledger-section {
  padding-top: 0;
}

.ledger-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.ledger-summary-item {
  min-width: 0;
  padding: 9px 7px;
  border-radius: 8px;
  background: rgba(231, 246, 229, 0.82);
}

.ledger-row {
  margin-top: 8px;
  padding: 9px 10px;
  border: 1px solid rgba(90, 123, 89, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
}

.ledger-row.active {
  border-color: rgba(18, 201, 107, 0.35);
  background: rgba(219, 249, 215, 0.78);
}

.ledger-row-main,
.ledger-row-money {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ledger-row-money {
  align-items: flex-end;
  flex-shrink: 0;
}

.ledger-name {
  max-width: 140px;
  color: #203025;
  font-size: 13px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ledger-meta,
.ledger-pending {
  color: #718075;
  font-size: 11px;
  font-weight: 700;
}

.ledger-money {
  color: #14854d;
  font-size: 13px;
  font-weight: 900;
}

.section-head,
.work-top,
.work-actions {
  display: flex;
  align-items: center;
}

.section-head,
.work-top {
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 900;
  color: #243027;
}

.section-count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: #eef5eb;
  color: #4d6d54;
  font-size: 12px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.order-focus {
  margin-top: 10px;
}

.order-picker {
  height: 36px;
  padding: 0 10px;
  border: 1px solid rgba(90, 123, 89, 0.18);
  border-radius: 8px;
  background: rgba(238, 247, 235, 0.84);
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-picker-label {
  flex-shrink: 0;
  color: #718075;
  font-size: 12px;
  font-weight: 700;
}

.order-picker-value {
  min-width: 0;
  flex: 1;
  color: #203025;
  font-size: 13px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-picker-arrow {
  flex-shrink: 0;
  color: #607664;
  font-size: 16px;
  font-weight: 900;
}

.work-item {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid rgba(90, 123, 89, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.selected-work-item {
  background: rgba(255, 255, 255, 0.9);
}

.work-item.compact {
  padding: 8px 10px;
}

.work-title {
  min-width: 0;
  font-size: 13px;
  color: #1f2d24;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-status {
  flex-shrink: 0;
  padding: 3px 7px;
  border-radius: 8px;
  background: #eef1f2;
  color: #536269;
  font-size: 11px;
  font-weight: 800;
}

.work-status.pending,
.work-status.joining {
  background: #fff4cf;
  color: #8b6400;
}

.work-status.processing,
.work-status.active,
.work-status.approved {
  background: #dceeff;
  color: #17609a;
}

.work-status.completed,
.work-status.ended {
  background: #daf7e5;
  color: #0b7b43;
}

.work-status.disputed,
.work-status.rejected,
.work-status.missed {
  background: #ffe2e2;
  color: #a53030;
}

.work-line {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #68766c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-line.strong {
  color: #14854d;
  font-weight: 900;
}

.work-actions {
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 9px;
}

.mini-btn {
  min-width: 62px;
  height: 28px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  background: #e8eee7;
  color: #304035;
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.mini-btn.primary {
  background: #12c96b;
  color: #ffffff;
}

.mini-btn.danger {
  background: #ff6961;
  color: #ffffff;
}

.mini-empty,
.profile-empty {
  color: #7b887f;
  font-size: 13px;
}

.mini-empty {
  margin-top: 10px;
}

.profile-empty {
  padding: 18px;
  line-height: 1.5;
}

.sidebar-hidden {
  transform: translateX(-100%);
  position: absolute;
  z-index: -1;
}

.sidebar-header {
  padding: 16px 12px;
  border-bottom: 1px solid rgba(90, 123, 89, 0.15);
  background: rgba(255, 255, 255, 0.5);
}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid rgba(90, 123, 89, 0.2);
}

.search-icon {
  margin-right: 8px;
  font-size: 16px;
}

.search-input {
  flex: 1;
  font-size: 14px;
  background: transparent;
  border: none;
}

.customer-list-scroll {
  flex: 1;
  overflow-y: auto;
}

.customer-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid rgba(90, 123, 89, 0.1);
}

.customer-item:hover {
  background: rgba(183, 239, 176, 0.2);
}

.customer-item.active {
  background: rgba(183, 239, 176, 0.4);
  border-left: 3px solid #00a884;
}

.customer-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  margin-right: 12px;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #e0e0e0;
}

.online-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 12px;
  height: 12px;
  background: #07c160;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.online-dot.offline {
  background: #b8c0ba;
}

.customer-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.customer-name {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-time {
  font-size: 12px;
  color: #999999;
  flex-shrink: 0;
  margin-left: 8px;
}

.last-message {
  font-size: 13px;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.customer-note {
  display: block;
  margin-top: -2px;
  font-size: 12px;
  color: #6f8069;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  background: #ff4d4f;
  color: #ffffff;
  font-size: 12px;
  font-weight: bold;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  flex-shrink: 0;
}

.empty-list {
  padding: 60px 20px;
  text-align: center;
}

.empty-text {
  font-size: 14px;
  color: #999999;
}

.sidebar-footer {
  display: flex;
  padding: 12px 16px;
  border-top: 1px solid rgba(90, 123, 89, 0.2);
  background: rgba(183, 239, 176, 0.3);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #999999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #333333;
}

/* ============ 右侧聊天区域 ============ */
.chat-main {
  flex: 1;
  min-width: 360px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.3);
  transition: transform 0.3s;
  backdrop-filter: blur(10px);
}

.chat-hidden {
  transform: translateX(100%);
  position: absolute;
  right: 0;
  width: 100%;
  z-index: -1;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(90, 123, 89, 0.2);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 8px;
  cursor: pointer;
}

.header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  margin-right: 12px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
}

.header-status {
  font-size: 12px;
  color: #07c160;
  margin-top: 2px;
}

.header-status.offline {
  color: #8a948d;
}

.header-note {
  font-size: 12px;
  color: #6f8069;
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  border-radius: 50%;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #f0f0f0;
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
  background: rgba(0, 0, 0, 0.1);
  color: #999999;
  font-size: 12px;
  border-radius: 12px;
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

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e0e0e0;
  flex-shrink: 0;
}

.message-bubble {
  max-width: min(68%, 520px);
  min-width: 0;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
}

.message-wrapper.theirs .message-bubble {
  background: rgba(255, 255, 255, 0.92);
  border-top-left-radius: 2px;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
}

.message-wrapper.mine .message-bubble {
  background: linear-gradient(180deg, #efffd1, #f5ffd9);
  border-top-right-radius: 2px;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
}

.system-bubble {
  max-width: 80%;
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.1) !important;
  text-align: center;
  font-size: 12px;
  color: #999999;
  border-radius: 4px !important;
}

.message-text {
  font-size: 15px;
  line-height: 1.5;
  color: #333333;
}

.translation-text {
  display: block;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 13px;
  line-height: 1.45;
  color: #20805f;
}

.message-img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  display: block;
}

.video-call-card {
  min-width: 210px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.video-call-title {
  font-size: 15px;
  line-height: 1.35;
  color: #1f2d24;
  font-weight: 900;
}

.video-call-room {
  font-size: 12px;
  line-height: 1.35;
  color: #627169;
  word-break: break-all;
}

.video-call-status {
  align-self: flex-start;
  padding: 3px 8px;
  border-radius: 8px;
  background: rgba(18, 201, 107, 0.12);
  color: #0a7a44;
  font-size: 12px;
  font-weight: 800;
}

.video-call-status.rejected,
.video-call-status.missed,
.video-call-status.ended {
  background: rgba(255, 105, 97, 0.14);
  color: #a53030;
}

.video-call-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 3px;
}

.video-call-btn {
  min-width: 62px;
  height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.video-call-btn.answer,
.video-call-btn.enter {
  background: #12c96b;
}

.video-call-btn.decline {
  background: #ff6961;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  justify-content: flex-end;
}

.msg-time {
  font-size: 11px;
  color: #999999;
}

.msg-status {
  font-size: 12px;
  color: #07c160;
}

.msg-status.failed {
  color: #d92d20;
  cursor: pointer;
  font-weight: 600;
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

.image-preview-bar {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
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
  padding: 8px 16px 8px;
  backdrop-filter: blur(10px);
}

.input-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.tool-btn {
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
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
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn.active {
  background: #00a884;
  color: #ffffff;
}

.send-btn.active:hover {
  background: #008c6d;
}

/* ============ 响应式设计 ============ */
@media (max-width: 768px) {
  .customer-sidebar {
    width: 100%;
    flex-basis: 100%;
    min-width: 0;
  }

  .sidebar-hidden {
    display: none;
  }

  .chat-hidden {
    display: none;
  }

  .customer-profile-panel {
    display: none;
  }

  .message-bubble {
    max-width: 75%;
  }
}

@media (min-width: 769px) and (max-width: 1180px) {
  .customer-sidebar {
    width: 260px;
    flex-basis: 260px;
    min-width: 220px;
  }

  .customer-profile-panel {
    width: 300px;
    flex-basis: 300px;
    min-width: 260px;
  }

  .chat-main {
    min-width: 320px;
  }

  .message-area {
    padding: 14px 18px;
  }
}
</style>
