<template>
  <view class="page-shell soft-page transactions-page">
    <view class="status-row">
      <text class="clock">05:48</text>
      <text class="battery">31%</text>
    </view>

    <view class="hero-card surface-card">
      <view class="hero-copy">
        <text class="eyebrow">Orders</text>
        <text class="title hero-title">Order records and status flow</text>
        <text class="subtitle">Track sell orders and trade orders, update milestones, and jump back into the related chat.</text>
      </view>
      <view class="hero-stats">
        <view class="stat-pill">
          <text class="stat-label">Open</text>
          <text class="stat-value">{{ activeCount }}</text>
        </view>
        <view class="stat-pill mint">
          <text class="stat-label">Done</text>
          <text class="stat-value">{{ completedCount }}</text>
        </view>
      </view>
    </view>

    <view class="filter-row surface-card">
      <view
        v-for="item in filters"
        :key="item.value"
        :class="['filter-pill', activeFilter === item.value && 'active-filter']"
        @click="activeFilter = item.value"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="filteredTransactions.length" class="transaction-list">
      <view v-for="item in filteredTransactions" :key="item.id" class="panel transaction-card">
        <view class="transaction-head">
          <view>
            <text class="order-no">{{ item.orderNo }}</text>
            <text class="order-time">Updated {{ item.updatedAt }}</text>
          </view>
          <text :class="['status-pill', statusClass(item.status)]">{{ statusLabel(item.status) }}</text>
        </view>

        <view class="transaction-main">
          <view>
            <text class="card-title">{{ item.cardName }}</text>
            <text class="trade-line">Face value {{ item.faceValue }}</text>
            <text class="trade-line payout-line">{{ item.payoutAmount }}</text>
          </view>
          <view class="counterparty-card">
            <text class="counterparty-label">Counterparty</text>
            <text class="counterparty-name">{{ item.counterpartyName }}</text>
            <text class="counterparty-user">@{{ item.counterpartyUsername }}</text>
          </view>
        </view>

        <text class="note-copy">{{ item.note }}</text>

        <view class="action-row">
          <button class="ghost-button action-button" @click="openSupportChat">Chat</button>
          <button
            v-for="action in nextActions(item.status)"
            :key="`${item.id}-${action}`"
            class="primary-button action-button"
            :disabled="loadingId === item.id"
            @click="advanceStatus(item.id, action)"
          >
            {{ actionLabel(action) }}
          </button>
        </view>
      </view>
    </view>

    <view v-else class="panel empty-card">
      <text class="title" style="font-size: 34rpx;">No matching trades</text>
      <view style="height: 10rpx"></view>
      <text class="muted">Try another status filter or start with your active sell orders.</text>
    </view>

    <text v-if="notice" class="notice-text">{{ notice }}</text>

    <AppNav current="transactions" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import type { TransactionItem } from '@/types'

const store = useAppStore()
const notice = ref('')
const loadingId = ref('')
const activeFilter = ref<'all' | TransactionItem['status']>('all')

const filters = [
  { label: 'All', value: 'all' as const },
  { label: 'Pending', value: 'pending' as const },
  { label: 'Processing', value: 'processing' as const },
  { label: 'Completed', value: 'completed' as const },
  { label: 'Disputed', value: 'disputed' as const }
]

onShow(() => {
  store.bootstrap()
})

const filteredTransactions = computed(() => {
  if (activeFilter.value === 'all') {
    return store.state.transactions
  }
  return store.state.transactions.filter((item) => item.status === activeFilter.value)
})

const activeCount = computed(() =>
  store.state.transactions.filter((item) => item.status === 'pending' || item.status === 'processing').length
)

const completedCount = computed(() =>
  store.state.transactions.filter((item) => item.status === 'completed').length
)

function statusLabel(status: TransactionItem['status']) {
  return {
    pending: 'Pending',
    processing: 'Processing',
    completed: 'Completed',
    disputed: 'Disputed'
  }[status]
}

function statusClass(status: TransactionItem['status']) {
  return {
    pending: 'warning',
    processing: 'active',
    completed: 'done',
    disputed: 'danger'
  }[status]
}

function nextActions(status: TransactionItem['status']) {
  if (status === 'pending') return ['processing', 'disputed'] as TransactionItem['status'][]
  if (status === 'processing') return ['completed', 'disputed'] as TransactionItem['status'][]
  return []
}

function actionLabel(status: TransactionItem['status']) {
  return {
    processing: 'Mark Processing',
    completed: 'Mark Complete',
    disputed: 'Raise Dispute',
    pending: 'Mark Pending'
  }[status]
}

async function advanceStatus(transactionId: string, status: TransactionItem['status']) {
  loadingId.value = transactionId
  try {
    await store.updateTransactionStatus(transactionId, status)
    notice.value = `Trade moved to ${statusLabel(status)}.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Update failed'
  } finally {
    loadingId.value = ''
  }
}

function openSupportChat() {
  uni.redirectTo({ url: '/pages/support/index' })
}
</script>

<style scoped lang="scss">
.transactions-page {
  padding-bottom: 184rpx;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 28rpx;
  font-weight: 900;
  margin-bottom: 18rpx;
}

.battery {
  color: #0088cc;
}

.hero-card {
  padding: 26rpx;
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.hero-copy {
  flex: 1;
}

.hero-title {
  margin-top: 10rpx;
  font-size: 38rpx;
}

.hero-stats {
  width: 164rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.stat-pill {
  min-height: 118rpx;
  border-radius: 12rpx;
  padding: 18rpx;
  background: #f3f6f8;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-pill.mint {
  background: rgba(0, 136, 204, 0.08);
}

.stat-label {
  font-size: 24rpx;
  color: #6e6e6e;
}

.stat-value {
  margin-top: 8rpx;
  font-size: 44rpx;
  font-weight: 900;
  color: #181818;
}

.filter-row {
  margin-top: 18rpx;
  padding: 16rpx;
  display: flex;
  gap: 12rpx;
  overflow-x: auto;
  white-space: nowrap;
}

.filter-pill {
  min-width: 136rpx;
  height: 70rpx;
  padding: 0 20rpx;
  border-radius: 10rpx;
  background: #f2f5f7;
  color: #6a717a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.active-filter {
  background: #0088cc;
  color: #ffffff;
}

.transaction-list {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.transaction-card {
  padding: 24rpx;
}

.transaction-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.order-no {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  color: #171717;
}

.order-time {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #8a8a8a;
}

.status-pill.done {
  color: #0a9c53;
  background: rgba(20, 216, 111, 0.12);
}

.status-pill.warning {
  color: #dd8a25;
  background: rgba(255, 184, 76, 0.2);
}

.status-pill.danger {
  color: #d64242;
  background: rgba(244, 91, 91, 0.14);
}

.transaction-main {
  margin-top: 22rpx;
  display: grid;
  grid-template-columns: 1fr 220rpx;
  gap: 18rpx;
}

.card-title {
  display: block;
  font-size: 34rpx;
  font-weight: 900;
}

.trade-line {
  display: block;
  margin-top: 12rpx;
  font-size: 25rpx;
  color: #626a73;
}

.payout-line {
  font-size: 30rpx;
  color: #0f9b57;
  font-weight: 800;
}

.counterparty-card {
  border-radius: 22rpx;
  background: #f7fafb;
  padding: 18rpx;
}

.counterparty-label {
  display: block;
  font-size: 22rpx;
  color: #8a8a8a;
}

.counterparty-name {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  font-weight: 800;
  color: #171717;
}

.counterparty-user {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6e7680;
}

.note-copy {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #626a73;
}

.action-row {
  margin-top: 20rpx;
  display: flex;
  gap: 14rpx;
  flex-wrap: wrap;
}

.action-button {
  min-width: 200rpx;
  padding-top: 20rpx;
  padding-bottom: 20rpx;
  font-size: 26rpx;
}

.empty-card {
  margin-top: 18rpx;
  text-align: center;
  padding-top: 48rpx;
  padding-bottom: 48rpx;
}

.notice-text {
  display: block;
  text-align: center;
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
