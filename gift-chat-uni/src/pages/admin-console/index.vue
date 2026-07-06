<template>
  <view class="page-shell soft-page admin-page">
    <view v-if="isAdminReady" class="page-stack">
      <view class="panel">
        <text class="eyebrow">Admin Console</text>
        <view style="height: 12rpx"></view>
        <text class="title">Users, support agents, and conversations</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">Review platform users, create support accounts, and manually assign support sessions.</text>
      </view>

      <view class="panel tab-row">
        <button :class="['ghost-button', activeTab === 'users' && 'active-tab']" @click="activeTab = 'users'">Users</button>
        <button :class="['ghost-button', activeTab === 'agents' && 'active-tab']" @click="activeTab = 'agents'">Agents</button>
        <button :class="['ghost-button', activeTab === 'support' && 'active-tab']" @click="activeTab = 'support'">Support</button>
        <button :class="['ghost-button', activeTab === 'direct' && 'active-tab']" @click="activeTab = 'direct'">Direct</button>
        <button :class="['ghost-button', activeTab === 'broadcast' && 'active-tab']" @click="activeTab = 'broadcast'">Broadcast</button>
        <button :class="['ghost-button', activeTab === 'orders' && 'active-tab']" @click="activeTab = 'orders'">Orders</button>
        <button :class="['ghost-button', activeTab === 'withdrawals' && 'active-tab']" @click="activeTab = 'withdrawals'">Withdraw</button>
        <button :class="['ghost-button', activeTab === 'risks' && 'active-tab']" @click="activeTab = 'risks'">Risk</button>
        <button :class="['ghost-button', activeTab === 'loans' && 'active-tab']" @click="activeTab = 'loans'">Loans</button>
        <button :class="['ghost-button', activeTab === 'rewards' && 'active-tab']" @click="activeTab = 'rewards'">Rewards</button>
        <button :class="['ghost-button', activeTab === 'notifications' && 'active-tab']" @click="activeTab = 'notifications'">Alerts</button>
      </view>

      <view class="panel admin-balance-panel">
        <view>
          <text class="section-title">Platform balance</text>
          <text class="row-meta">All active users: {{ store.state.balanceSummary?.userCount || 0 }}</text>
        </view>
        <view class="balance-grid">
          <view>
            <text class="row-meta">Available</text>
            <text class="balance-number">{{ store.state.balanceSummary?.availableTotal || '0.00' }}</text>
          </view>
          <view>
            <text class="row-meta">Pending</text>
            <text class="balance-number">{{ store.state.balanceSummary?.pendingTotal || '0.00' }}</text>
          </view>
          <view>
            <text class="row-meta">Withdrawn</text>
            <text class="balance-number">{{ store.state.balanceSummary?.withdrawnTotal || '0.00' }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'users'" class="panel">
        <text class="section-title">User management</text>
        <view style="height: 18rpx"></view>
        <view v-for="user in users" :key="user.id" class="list-row">
          <view>
            <text class="row-title">{{ user.username }}</text>
            <text class="row-meta">{{ user.phone || 'No phone' }} / {{ user.email || 'No email' }}</text>
            <text class="row-meta">{{ user.role }} / {{ user.status }} / {{ user.createdAt }}</text>
          </view>
          <text :class="['status-pill', user.blacklisted ? 'paused' : 'active']">
            {{ user.blacklisted ? 'Blacklisted' : 'Clear' }}
          </text>
        </view>
      </view>

      <view v-if="activeTab === 'agents'" class="panel">
        <text class="section-title">Create support agent</text>
        <view style="height: 18rpx"></view>
        <input v-model="agentForm.username" class="field-input" placeholder="Agent username" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.email" class="field-input" placeholder="Email" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.phone" class="field-input" placeholder="Phone" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.password" class="field-input" password placeholder="Password" />
        <view style="height: 18rpx"></view>
        <button class="primary-button" @click="submitAgent">Create Agent</button>
      </view>

      <view v-if="activeTab === 'agents'" class="panel">
        <text class="section-title">Support agents</text>
        <view style="height: 18rpx"></view>
        <view v-for="agent in agents" :key="agent.id" class="list-row">
          <view>
            <text class="row-title">{{ agent.username }}</text>
            <text class="row-meta">{{ agent.assignedConversationCount }} assigned conversations</text>
            <text class="row-meta">{{ agent.phone || 'No phone' }} / {{ agent.email || 'No email' }}</text>
          </view>
          <button class="ghost-button mini-button" @click="toggleAgent(agent.id, agent.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')">
            {{ agent.status === 'ACTIVE' ? 'Disable' : 'Enable' }}
          </button>
        </view>
      </view>

      <view v-if="activeTab === 'support'" class="panel">
        <text class="section-title">Support conversations</text>
        <view style="height: 18rpx"></view>
        <view v-for="conversation in conversations" :key="conversation.conversationId" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ conversation.customerUsername }}</text>
              <text class="row-meta">{{ conversation.assignmentStatus }} / {{ conversation.assignedAgent || 'Unassigned' }}</text>
            </view>
            <text class="status-pill active">{{ conversation.messages.length }} msgs</text>
          </view>
          <view class="assign-row">
            <input v-model="assignDrafts[conversation.conversationId]" class="field-input assign-input" placeholder="Agent username" />
            <button class="primary-button mini-button" @click="assignConversation(conversation.conversationId)">Assign</button>
          </view>
          <view v-if="conversation.messages[0]" class="last-message">
            {{ conversation.messages[conversation.messages.length - 1].content }}
          </view>
          <view v-if="conversation.messages.length" class="admin-message-list">
            <view
              v-for="message in conversation.messages"
              :key="message.id"
              class="admin-message"
            >
              <text class="message-meta">{{ message.author }} · {{ message.createdAt }} · {{ message.type }}</text>
              <text class="message-body">{{ message.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'direct'" class="panel">
        <text class="section-title">Direct message records</text>
        <view style="height: 18rpx"></view>
        <view class="assign-row">
          <input v-model="directSearch" class="field-input assign-input" placeholder="Filter by username" />
          <button class="primary-button mini-button" @click="refreshDirectConversations">Search</button>
        </view>
        <view style="height: 18rpx"></view>
        <view v-for="conversation in directConversations" :key="conversation.friendshipId" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ conversation.requesterUsername }} / {{ conversation.addresseeUsername }}</text>
              <text class="row-meta">{{ conversation.status }} / {{ conversation.messages.length }} messages</text>
            </view>
          </view>
          <view v-if="conversation.messages.length" class="admin-message-list">
            <view
              v-for="message in conversation.messages"
              :key="message.id"
              class="admin-message"
            >
              <text class="message-meta">{{ message.author }} · {{ message.createdAt }} · {{ message.type }}</text>
              <text class="message-body">{{ message.content }}</text>
            </view>
          </view>
          <text v-else class="row-meta">No direct messages yet.</text>
        </view>
      </view>

      <view v-if="activeTab === 'broadcast'" class="panel">
        <text class="section-title">Admin broadcast</text>
        <view style="height: 18rpx"></view>
        <view class="assign-row">
          <input v-model="broadcastForm.content" class="field-input assign-input" placeholder="Message all users..." />
          <button class="primary-button mini-button" @click="submitBroadcast">Send</button>
        </view>
        <view style="height: 14rpx"></view>
        <view class="broadcast-type-row">
          <button
            v-for="type in broadcastTypes"
            :key="type"
            :class="['ghost-button', 'mini-button', broadcastForm.messageType === type && 'active-soft']"
            @click="broadcastForm.messageType = type"
          >
            {{ type }}
          </button>
        </view>
        <view style="height: 14rpx"></view>
        <input v-model="broadcastForm.keyword" class="field-input assign-input" placeholder="Filter by note, username, phone, or bank account" />
        <view class="broadcast-type-row">
          <button
            v-for="code in adminBroadcastCountryOptions"
            :key="code"
            :class="['ghost-button', 'mini-button', broadcastForm.countryCodes.includes(code) && 'active-soft']"
            @click="toggleAdminBroadcastCountry(code)"
          >
            {{ code }}
          </button>
        </view>
      </view>

      <view v-if="activeTab === 'broadcast'" class="panel">
        <text class="section-title">Broadcast records</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in broadcasts" :key="item.id" class="list-row">
          <view>
            <text class="row-title">{{ item.senderUsername }} / {{ item.scope }}</text>
            <text class="row-meta">{{ item.messageType }} / delivered {{ item.deliveredCount }} / {{ item.createdAt }}</text>
            <text class="row-meta">filters: {{ item.countryCodes || 'all countries' }} / {{ item.searchKeyword || 'no keyword' }} / {{ item.targetMode }}</text>
            <text v-if="item.targetUsernames" class="row-meta">targets: {{ item.targetUsernames }}</text>
            <text class="row-meta">{{ item.content }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'orders'" class="panel">
        <text class="section-title">Sell and trade orders</text>
        <view style="height: 18rpx"></view>
        <view v-for="order in transactions" :key="order.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ order.orderNo }} / {{ order.cardName }}</text>
              <text class="row-meta">{{ order.faceValue }} / {{ order.payoutAmount }} / {{ order.counterpartyUsername }}</text>
              <text class="row-meta">{{ order.note }}</text>
            </view>
            <text :class="['status-pill', order.status === 'completed' ? 'active' : 'warning']">{{ order.status }}</text>
          </view>
          <view class="broadcast-type-row">
            <button
              v-for="status in transactionStatuses(order.status)"
              :key="`${order.id}-${status}`"
              class="ghost-button mini-button"
              @click="updateOrderStatus(order.id, status)"
            >
              {{ status }}
            </button>
            <button
              v-if="order.status === 'pending' || order.status === 'processing'"
              class="ghost-button mini-button danger-action"
              @click="cancelOrder(order.id)"
            >
              Cancel bad card
            </button>
          </view>
          <text v-if="order.status === 'canceled'" class="row-meta">Canceled: {{ order.cancelReason }} {{ order.cancelNote }}</text>
        </view>
      </view>

      <view v-if="activeTab === 'withdrawals'" class="panel">
        <text class="section-title">Withdrawal requests</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in withdrawals" :key="item.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ item.requestNo }} / {{ item.amount }}</text>
              <text class="row-meta">{{ item.ownerUsername || item.accountName }} / {{ item.bankName }} / {{ item.accountNumber }}</text>
              <text class="row-meta">{{ item.assignedAgent }} / {{ item.createdAt }}</text>
            </view>
            <text :class="['status-pill', item.status === 'completed' ? 'active' : 'warning']">{{ item.status }}</text>
          </view>
          <button
            v-if="item.status !== 'completed'"
            class="ghost-button mini-button"
            @click="completeWithdrawal(item.id)"
          >
            Mark Completed
          </button>
        </view>
      </view>

      <view v-if="activeTab === 'risks'" class="panel">
        <text class="section-title">Duplicate bank account risk</text>
        <view style="height: 18rpx"></view>
        <view v-for="risk in bankAccountRisks" :key="`${risk.username}-${risk.reason}-${risk.accountNumber}-${risk.submittedAt}`" class="list-row">
          <view>
            <text class="row-title">{{ risk.username }} / {{ risk.riskLevel }}</text>
            <text class="row-meta">{{ risk.reason }} / {{ risk.bankName }} / {{ risk.accountName }}</text>
            <text class="row-meta">{{ risk.accountNumber }} / {{ risk.phoneCountryCode || '-' }} / {{ risk.assignedAgent || '-' }}</text>
            <text class="row-meta">{{ risk.submittedAt }}</text>
          </view>
          <text :class="['status-pill', risk.riskLevel === 'high' ? 'danger' : 'warning']">{{ risk.riskLevel }}</text>
        </view>
        <text v-if="bankAccountRisks.length === 0" class="row-meta">No duplicate bank account risk found.</text>
      </view>

      <view v-if="activeTab === 'loans'" class="panel">
        <text class="section-title">Loan applications</text>
        <view style="height: 18rpx"></view>
        <view v-for="loan in loans" :key="loan.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ loan.applicationNo }} / {{ loan.ownerUsername }}</text>
              <text class="row-meta">{{ loan.amount }} / {{ loan.country }} / {{ loan.assignedAgent }}</text>
              <text class="row-meta">{{ loan.purpose }}</text>
              <text v-if="loan.reviewNote" class="row-meta">{{ loan.reviewNote }}</text>
            </view>
            <text :class="['status-pill', loan.status === 'approved' ? 'active' : loan.status === 'rejected' ? 'danger' : 'warning']">
              {{ loan.status }}
            </text>
          </view>
          <input v-model="loanReviewDrafts[loan.id]" class="field-input assign-input" placeholder="Review note" />
          <view class="broadcast-type-row">
            <button class="ghost-button mini-button" @click="reviewLoan(loan.id, 'approved')">Approve</button>
            <button class="ghost-button mini-button danger-action" @click="reviewLoan(loan.id, 'rejected')">Reject</button>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Referral rewards</text>
        <view style="height: 18rpx"></view>
        <view class="reward-setting-row">
          <view>
            <text class="row-title">Registration cashback</text>
            <text class="row-meta">Fixed reward paid to the inviter after a new user registers.</text>
          </view>
          <switch :checked="rewardForm.registrationCashbackEnabled" @change="setRegistrationRewardEnabled" />
        </view>
        <input v-model="rewardForm.registrationCashbackAmount" class="field-input" type="digit" placeholder="Registration cashback amount" />
        <view style="height: 18rpx"></view>
        <view class="reward-setting-row">
          <view>
            <text class="row-title">Trade rebate</text>
            <text class="row-meta">Percent reward paid to the inviter when the new user's order is completed.</text>
          </view>
          <switch :checked="rewardForm.tradeRebateEnabled" @change="setTradeRewardEnabled" />
        </view>
        <input v-model="rewardForm.tradeRebatePercent" class="field-input" type="digit" placeholder="Trade rebate percent" />
        <view style="height: 18rpx"></view>
        <button class="primary-button" @click="saveRewardConfig">Save Reward Rules</button>
        <view style="height: 12rpx"></view>
        <text class="row-meta">Updated by {{ referralRewardConfig?.updatedBy || '-' }} / {{ referralRewardConfig?.updatedAt || '-' }}</text>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Reward records</text>
        <view style="height: 18rpx"></view>
        <view v-for="reward in referralRewards" :key="reward.id" class="list-row">
          <view>
            <text class="row-title">{{ reward.referrerUsername }} earned {{ reward.amount }}</text>
            <text class="row-meta">{{ reward.rewardType }} / invited {{ reward.referredUsername }} / {{ reward.status }}</text>
            <text class="row-meta">{{ reward.tradeOrderNo || 'Registration reward' }} / {{ reward.createdAt }}</text>
          </view>
          <text :class="['status-pill', reward.status === 'available' ? 'active' : 'warning']">
            {{ reward.ratePercent ? reward.ratePercent + '%' : 'fixed' }}
          </text>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Registration bonus by phone country code</text>
        <view style="height: 18rpx"></view>
        <view v-for="config in registrationBonusConfigs" :key="config.countryCode" class="bonus-config-card">
          <view class="reward-setting-row">
            <view>
              <text class="row-title">{{ config.countryCode }} / {{ registrationBonusDrafts[config.countryCode]?.countryName || config.countryName }}</text>
              <text class="row-meta">Paid once when a new account registers with this phone prefix.</text>
            </view>
            <switch
              :checked="registrationBonusDrafts[config.countryCode]?.enabled"
              @change="setBonusEnabled(config.countryCode, $event)"
            />
          </view>
          <view class="bonus-config-grid">
            <input v-model="registrationBonusDrafts[config.countryCode].countryName" class="field-input" placeholder="Country name" />
            <input v-model="registrationBonusDrafts[config.countryCode].currencyCode" class="field-input" placeholder="Currency" />
            <input v-model="registrationBonusDrafts[config.countryCode].bonusAmount" class="field-input" type="digit" placeholder="Bonus amount" />
          </view>
          <input v-model="registrationBonusDrafts[config.countryCode].note" class="field-input" placeholder="Internal note" />
          <view style="height: 12rpx"></view>
          <button class="primary-button mini-button" @click="saveRegistrationBonusConfig(config)">Save {{ config.countryCode }}</button>
          <text class="row-meta">Updated by {{ config.updatedBy || '-' }} / {{ config.updatedAt || '-' }}</text>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Registration bonus records</text>
        <view style="height: 18rpx"></view>
        <view v-for="record in registrationBonusRecords" :key="record.id" class="list-row">
          <view>
            <text class="row-title">{{ record.username }} received {{ record.bonusAmount }} {{ record.currencyCode }}</text>
            <text class="row-meta">{{ record.countryCode }} / {{ record.phone || 'No phone' }} / {{ record.status }}</text>
            <text class="row-meta">{{ record.reason }} / {{ record.createdAt }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'notifications'" class="panel">
        <text class="section-title">Admin notifications</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in notifications" :key="item.id" class="list-row">
          <view>
            <text class="row-title">{{ item.title }}</text>
            <text class="row-meta">{{ item.eventType }} / {{ item.targetType }} / {{ item.createdAt }}</text>
            <text class="row-meta">{{ item.body }}</text>
          </view>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import type {
  AdminDirectConversationItem,
  AdminUserItem,
  AgentItem,
  BankAccountRiskMatch,
  BroadcastItem,
  LoanApplicationItem,
  NotificationItem,
  ReferralRewardConfigItem,
  ReferralRewardItem,
  RegistrationBonusConfigItem,
  RegistrationBonusRecordItem,
  SupportConversationItem,
  TransactionItem,
  WithdrawalItem
} from '@/types'
import {
  assignSupportConversation,
  cancelTransaction,
  createBroadcast,
  createAgent,
  fetchAdminDirectConversations,
  fetchAdminBankAccountRisks,
  fetchAdminSupportConversations,
  fetchAdminUsers,
  fetchAgents,
  fetchBroadcasts,
  fetchLoans,
  fetchNotifications,
  fetchReferralRewardConfig,
  fetchReferralRewards,
  fetchRegistrationBonusConfigs,
  fetchRegistrationBonusRecords,
  fetchTransactions,
  fetchWithdrawals,
  updateAgentStatus,
  updateLoanStatus,
  updateReferralRewardConfig,
  updateRegistrationBonusConfig,
  updateTransactionStatus,
  updateWithdrawalStatus
} from '@/utils/api'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const isAdminReady = ref(false)
const activeTab = ref<'users' | 'agents' | 'support' | 'direct' | 'broadcast' | 'orders' | 'withdrawals' | 'risks' | 'loans' | 'rewards' | 'notifications'>('users')
const notice = ref('')
const users = ref<AdminUserItem[]>([])
const agents = ref<AgentItem[]>([])
const conversations = ref<SupportConversationItem[]>([])
const directConversations = ref<AdminDirectConversationItem[]>([])
const broadcasts = ref<BroadcastItem[]>([])
const transactions = ref<TransactionItem[]>([])
const withdrawals = ref<WithdrawalItem[]>([])
const bankAccountRisks = ref<BankAccountRiskMatch[]>([])
const loans = ref<LoanApplicationItem[]>([])
const referralRewardConfig = ref<ReferralRewardConfigItem | null>(null)
const referralRewards = ref<ReferralRewardItem[]>([])
const registrationBonusConfigs = ref<RegistrationBonusConfigItem[]>([])
const registrationBonusRecords = ref<RegistrationBonusRecordItem[]>([])
const notifications = ref<NotificationItem[]>([])
const directSearch = ref('')
const assignDrafts = reactive<Record<string, string>>({})
const loanReviewDrafts = reactive<Record<string, string>>({})
const registrationBonusDrafts = reactive<Record<string, {
  countryName: string
  currencyCode: string
  bonusAmount: string
  enabled: boolean
  note: string
}>>({})
const broadcastTypes = ['text', 'image', 'voice', 'gif', 'link'] as const
const adminBroadcastCountryOptions = ['+234', '+91', '+233']

const broadcastForm = reactive({
  content: '',
  messageType: 'text' as BroadcastItem['messageType'],
  keyword: '',
  countryCodes: [] as string[]
})

const agentForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: ''
})

const rewardForm = reactive({
  registrationCashbackEnabled: true,
  registrationCashbackAmount: '1.00',
  tradeRebateEnabled: true,
  tradeRebatePercent: '5'
})

onShow(() => {
  if (requireAdmin()) {
    refreshAll()
  }
})

function requireAdmin() {
  if (store.state.currentUser?.roleCode === 'ADMIN') {
    isAdminReady.value = true
    return true
  }
  isAdminReady.value = false
  notice.value = 'Admin account required.'
  uni.redirectTo({ url: '/pages/admin-login/index' })
  return false
}

async function refreshAll() {
  try {
    const [
      nextUsers,
      nextAgents,
      nextConversations,
      nextBroadcasts,
      nextTransactions,
      nextWithdrawals,
      nextBankAccountRisks,
      nextLoans,
      nextRewardConfig,
      nextRewards,
      nextRegistrationBonusConfigs,
      nextRegistrationBonusRecords,
      nextNotifications
    ] = await Promise.all([
      fetchAdminUsers(),
      fetchAgents(),
      fetchAdminSupportConversations(),
      fetchBroadcasts(),
      fetchTransactions(),
      fetchWithdrawals(),
      fetchAdminBankAccountRisks(),
      fetchLoans(),
      fetchReferralRewardConfig(),
      fetchReferralRewards(),
      fetchRegistrationBonusConfigs(),
      fetchRegistrationBonusRecords(),
      fetchNotifications()
    ])
    users.value = nextUsers
    agents.value = nextAgents
    conversations.value = nextConversations
    broadcasts.value = nextBroadcasts
    transactions.value = nextTransactions
    withdrawals.value = nextWithdrawals
    bankAccountRisks.value = nextBankAccountRisks
    loans.value = nextLoans
    referralRewardConfig.value = nextRewardConfig
    referralRewards.value = nextRewards
    registrationBonusConfigs.value = nextRegistrationBonusConfigs
    registrationBonusRecords.value = nextRegistrationBonusRecords
    applyRewardConfig(nextRewardConfig)
    applyRegistrationBonusConfigs(nextRegistrationBonusConfigs)
    notifications.value = nextNotifications
    await store.refreshBalanceSummary().catch(() => {})
    nextConversations.forEach((conversation) => {
      assignDrafts[conversation.conversationId] = conversation.assignedAgent || ''
    })
    nextLoans.forEach((loan) => {
      loanReviewDrafts[loan.id] = loan.reviewNote || ''
    })
    if (!directConversations.value.length) {
      directConversations.value = await fetchAdminDirectConversations()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Admin data failed'
  }
}

function applyRewardConfig(config: ReferralRewardConfigItem) {
  rewardForm.registrationCashbackEnabled = config.registrationCashbackEnabled
  rewardForm.registrationCashbackAmount = config.registrationCashbackAmount
  rewardForm.tradeRebateEnabled = config.tradeRebateEnabled
  rewardForm.tradeRebatePercent = config.tradeRebatePercent
}

function applyRegistrationBonusConfigs(configs: RegistrationBonusConfigItem[]) {
  configs.forEach((config) => {
    registrationBonusDrafts[config.countryCode] = {
      countryName: config.countryName,
      currencyCode: config.currencyCode,
      bonusAmount: config.bonusAmount,
      enabled: config.enabled,
      note: config.note
    }
  })
}

function setRegistrationRewardEnabled(event: Event) {
  rewardForm.registrationCashbackEnabled = switchValue(event)
}

function setTradeRewardEnabled(event: Event) {
  rewardForm.tradeRebateEnabled = switchValue(event)
}

function setBonusEnabled(countryCode: string, event: Event) {
  const draft = registrationBonusDrafts[countryCode]
  if (draft) {
    draft.enabled = switchValue(event)
  }
}

function switchValue(event: Event) {
  return Boolean((event as unknown as { detail?: { value?: boolean } }).detail?.value)
}

async function saveRewardConfig() {
  try {
    const updated = await updateReferralRewardConfig({
      registrationCashbackEnabled: rewardForm.registrationCashbackEnabled,
      registrationCashbackAmount: rewardForm.registrationCashbackAmount,
      tradeRebateEnabled: rewardForm.tradeRebateEnabled,
      tradeRebatePercent: rewardForm.tradeRebatePercent
    })
    referralRewardConfig.value = updated
    applyRewardConfig(updated)
    notice.value = 'Reward rules saved.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Reward rules failed'
  }
}

async function saveRegistrationBonusConfig(config: RegistrationBonusConfigItem) {
  try {
    const draft = registrationBonusDrafts[config.countryCode]
    if (!draft) return
    const updated = await updateRegistrationBonusConfig({
      countryCode: config.countryCode,
      countryName: draft.countryName,
      currencyCode: draft.currencyCode,
      bonusAmount: draft.bonusAmount,
      enabled: draft.enabled,
      note: draft.note
    })
    const index = registrationBonusConfigs.value.findIndex(item => item.countryCode === updated.countryCode)
    if (index >= 0) {
      registrationBonusConfigs.value.splice(index, 1, updated)
    }
    applyRegistrationBonusConfigs(registrationBonusConfigs.value)
    notice.value = `${config.countryCode} registration bonus saved.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Registration bonus save failed'
  }
}

async function submitBroadcast() {
  try {
    const content = broadcastForm.content.trim()
    if (!content) return
    const confirmed = await confirmAdminBroadcast(content)
    if (!confirmed) return
    await createBroadcast({
      scope: 'all',
      content,
      messageType: broadcastForm.messageType,
      countryCodes: broadcastForm.countryCodes,
      keyword: broadcastForm.keyword.trim()
    })
    broadcastForm.content = ''
    broadcastForm.keyword = ''
    notice.value = 'Broadcast sent.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Broadcast failed'
  }
}

function confirmAdminBroadcast(content: string) {
  const countryText = broadcastForm.countryCodes.length ? broadcastForm.countryCodes.join(', ') : 'all countries'
  const keywordText = broadcastForm.keyword.trim() || 'no keyword'
  const countText = `${adminBroadcastTargetCount.value} estimated targets`
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: 'Confirm broadcast',
      content: `Countries: ${countryText}\nKeyword: ${keywordText}\nTargets: ${countText}\nMessage: ${content}`,
      confirmText: 'Send',
      cancelText: 'Cancel',
      success(result) {
        resolve(Boolean(result.confirm))
      },
      fail() {
        resolve(false)
      }
    })
  })
}

const adminBroadcastTargetCount = computed(() => {
  const keyword = broadcastForm.keyword.trim().toLowerCase()
  const countries = broadcastForm.countryCodes
  return users.value.filter((user) => {
    if (user.role !== 'USER' || user.status !== 'ACTIVE') return false
    const country = resolveAdminUserCountry(user.phone)
    if (countries.length > 0 && !countries.includes(country)) return false
    if (!keyword) return true
    return [user.username, user.email, user.phone, country]
      .some(value => (value || '').toLowerCase().includes(keyword))
  }).length
})

function resolveAdminUserCountry(phone: string) {
  const normalized = `+${(phone || '').replace(/^00/, '').replace(/[^0-9]/g, '')}`
  return adminBroadcastCountryOptions
    .slice()
    .sort((left, right) => right.length - left.length)
    .find(code => normalized.startsWith(code)) || ''
}

function toggleAdminBroadcastCountry(code: string) {
  const index = broadcastForm.countryCodes.indexOf(code)
  if (index >= 0) {
    broadcastForm.countryCodes.splice(index, 1)
    return
  }
  broadcastForm.countryCodes.push(code)
}

function transactionStatuses(status: TransactionItem['status']) {
  if (status === 'pending') return ['processing', 'disputed'] as TransactionItem['status'][]
  if (status === 'processing') return ['completed', 'disputed'] as TransactionItem['status'][]
  return [] as TransactionItem['status'][]
}

async function updateOrderStatus(orderId: string, status: TransactionItem['status']) {
  try {
    await updateTransactionStatus(orderId, status)
    notice.value = `Order moved to ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Order update failed'
  }
}

async function cancelOrder(orderId: string) {
  try {
    await cancelTransaction(orderId, {
      reason: 'Bad card',
      notifyCustomer: true
    })
    notice.value = 'Order canceled.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Order cancellation failed'
  }
}

async function completeWithdrawal(withdrawalId: string) {
  try {
    await updateWithdrawalStatus(withdrawalId, 'completed')
    notice.value = 'Withdrawal completed.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Withdrawal update failed'
  }
}

async function reviewLoan(loanId: string, status: LoanApplicationItem['status']) {
  try {
    await updateLoanStatus(loanId, status, loanReviewDrafts[loanId] || undefined)
    notice.value = `Loan ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Loan review failed'
  }
}

async function refreshDirectConversations() {
  try {
    directConversations.value = await fetchAdminDirectConversations(directSearch.value)
    notice.value = 'Direct records refreshed.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Direct records failed'
  }
}

async function submitAgent() {
  try {
    await createAgent({
      username: agentForm.username,
      email: agentForm.email || undefined,
      phone: agentForm.phone || undefined,
      password: agentForm.password
    })
    notice.value = 'Agent created.'
    agentForm.username = ''
    agentForm.email = ''
    agentForm.phone = ''
    agentForm.password = ''
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Create agent failed'
  }
}

async function toggleAgent(agentId: string, status: string) {
  try {
    await updateAgentStatus(agentId, status)
    notice.value = `Agent moved to ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Agent status failed'
  }
}

async function assignConversation(conversationId: string) {
  try {
    const agentUsername = assignDrafts[conversationId]?.trim()
    if (!agentUsername) return
    await assignSupportConversation(conversationId, agentUsername)
    notice.value = 'Conversation assigned.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Assignment failed'
  }
}
</script>

<style scoped lang="scss">
.admin-page {
  padding-bottom: 80rpx;
}

.tab-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
}

.tab-row button {
  padding: 18rpx 10rpx;
  font-size: 24rpx;
}

.active-tab {
  color: #ffffff;
  background: #13d66f;
}

.active-soft {
  color: #0d9b56;
  border-color: #13d66f;
  background: #effff5;
}

.list-row {
  padding: 18rpx 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.compact-row {
  border-bottom: none;
}

.row-title {
  display: block;
  font-size: 29rpx;
  font-weight: 800;
  color: #171717;
}

.row-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #818891;
}

.mini-button {
  min-width: 150rpx;
  padding: 14rpx 18rpx;
  font-size: 23rpx;
}

.conversation-card {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef1f3;
}

.assign-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.assign-input {
  height: 74rpx;
  font-size: 25rpx;
}

.last-message {
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: #f6f8fa;
  color: #5f6872;
  font-size: 23rpx;
  line-height: 1.45;
}

.admin-message-list {
  margin-top: 14rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.admin-message {
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #f6f8fa;
}

.message-meta {
  display: block;
  font-size: 21rpx;
  color: #87919c;
}

.message-body {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #22272d;
  line-height: 1.45;
  word-break: break-word;
}

.broadcast-type-row {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.admin-balance-panel {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
}

.balance-number {
  display: block;
  margin-top: 6rpx;
  font-size: 32rpx;
  font-weight: 900;
  color: #101820;
  line-height: 1.15;
  word-break: break-word;
}

.reward-setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 14rpx;
}

.bonus-config-card {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef1f3;
}

.bonus-config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.status-pill.danger,
.danger-action {
  color: #d64242;
  border-color: #f0b1ab;
  background: rgba(244, 91, 91, 0.14);
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}

@media (max-width: 760px) {
  .bonus-config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
