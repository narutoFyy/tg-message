<template>
  <view class="page-shell soft-page transactions-page">
    <view class="winner-card surface-card">
      <view class="winner-head">
        <view>
          <text class="eyebrow">Lucky winners</text>
          <text class="winner-title">Recent prize records</text>
        </view>
        <text class="winner-link" @click="goLottery">Spin</text>
      </view>
      <view class="winner-window">
        <view v-if="tickerWinners.length" class="winner-track">
          <view v-for="(winner, index) in tickerWinners" :key="`${winner.drawnAt}-${winner.prizeName}-${index}`" class="winner-row">
            <view class="winner-main">
              <text class="winner-prize">{{ winner.prizeName }}</text>
              <text class="winner-time">{{ winner.drawnAt || 'Just now' }}</text>
            </view>
            <text class="winner-name">{{ winner.displayName }}</text>
          </view>
        </view>
        <view v-else class="winner-row static-row">
          <view class="winner-main">
            <text class="winner-prize">No winners yet</text>
            <text class="winner-time">Open the wheel to start</text>
          </view>
          <text class="winner-name">Be the first</text>
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
          <button class="ghost-button action-button danger-action" @click="hideTransaction(item)">Hide</button>
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
  store.refreshLotteryWinners().catch(() => undefined)
})

const filteredTransactions = computed(() => {
  if (activeFilter.value === 'all') {
    return store.state.transactions
  }
  return store.state.transactions.filter((item) => item.status === activeFilter.value)
})

const tickerWinners = computed(() => {
  const winners = store.state.lotteryWinners
  if (!winners.length) return []
  return winners.length > 3 ? [...winners, ...winners] : [...winners, ...winners, ...winners]
})

function statusLabel(status: TransactionItem['status']) {
  return {
    pending: 'Pending',
    processing: 'Processing',
    completed: 'Completed',
    disputed: 'Disputed',
    canceled: 'Canceled'
  }[status]
}

function statusClass(status: TransactionItem['status']) {
  return {
    pending: 'warning',
    processing: 'active',
    completed: 'done',
    disputed: 'danger',
    canceled: 'danger'
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
    pending: 'Mark Pending',
    canceled: 'Canceled'
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

function goLottery() {
  uni.navigateTo({ url: '/pages/lucky-wheel/index' })
}

function hideTransaction(item: TransactionItem) {
  uni.showModal({
    title: 'Hide order',
    content: 'This only hides the order from your list. The platform and support team still keep the full record.',
    confirmText: 'Hide',
    confirmColor: '#d64242',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.hideRecord({
          targetType: 'ORDER',
          targetId: item.id,
          hiddenScope: 'ORDER'
        })
        notice.value = 'Order hidden from your list.'
      } catch (error) {
        notice.value = error instanceof Error ? error.message : 'Hide failed'
      }
    }
  })
}
</script>

<style scoped lang="scss">
.transactions-page {
  padding-bottom: 184rpx;
}

.filter-row {
  margin-top: 18rpx;
  padding: 16rpx;
  display: flex;
  gap: 12rpx;
  overflow-x: auto;
  white-space: nowrap;
}

.winner-card {
  margin-top: 0;
  padding: 24rpx;
}

.winner-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.winner-title {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 900;
  color: #17212b;
}

.winner-link {
  min-width: 108rpx;
  height: 62rpx;
  border-radius: 10rpx;
  background: #0088cc;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
}

.winner-window {
  position: relative;
  height: 250rpx;
  margin-top: 18rpx;
  overflow: hidden;
  border-radius: 10rpx;
  background: #f6fafc;
  border: 1rpx solid rgba(0, 136, 204, 0.1);
}

.winner-track {
  animation: winnerScroll 12s linear infinite;
}

.winner-row {
  min-height: 84rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  box-sizing: border-box;
  border-bottom: 1rpx solid rgba(0, 136, 204, 0.08);
}

.static-row {
  height: 250rpx;
}

.winner-main {
  min-width: 0;
  flex: 1;
}

.winner-prize {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  color: #17212b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.winner-time {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  font-weight: 700;
  color: #758391;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.winner-name {
  max-width: 280rpx;
  font-size: 24rpx;
  font-weight: 800;
  color: #53616e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes winnerScroll {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
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

.danger-action {
  color: #d64242;
  border-color: rgba(214, 66, 66, 0.22);
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
