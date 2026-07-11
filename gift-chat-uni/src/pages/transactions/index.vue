<template>
  <view class="page-shell soft-page transactions-page">
    <view class="transactions-content">
      <view class="page-header orders-hero tone-risk">
        <view class="page-header-copy">
          <text class="eyebrow">Activity</text>
          <text class="title">Orders</text>
          <text class="subtitle">Track sell orders and settlement progress.</text>
        </view>
        <text class="order-count">{{ filteredTransactions.length }}</text>
      </view>

      <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
        <view class="filter-row">
          <view
            v-for="item in filters"
            :key="item.value"
            :class="['filter-tab', activeFilter === item.value && 'active-filter']"
            @click="activeFilter = item.value"
          >{{ item.label }}</view>
        </view>
      </scroll-view>

      <view v-if="filteredTransactions.length" class="transaction-list">
        <view v-for="item in filteredTransactions" :key="item.id" :class="['transaction-row', `order-${item.status}`]">
          <view class="transaction-head">
            <view class="order-heading">
              <text class="order-no">{{ item.orderNo }}</text>
              <text class="order-time">Updated {{ item.updatedAt }}</text>
            </view>
            <text :class="['status-pill', statusClass(item.status)]">{{ statusLabel(item.status) }}</text>
          </view>

          <view class="transaction-main">
            <view class="trade-card-identity">
              <image class="trade-card-logo" :src="cardLogoFor(item.cardName)" mode="aspectFit" />
              <view class="trade-summary">
                <text class="card-title">{{ item.cardName }}</text>
                <text class="trade-line">Face value {{ item.faceValue }}</text>
                <text class="payout-line">{{ item.payoutAmount }}</text>
              </view>
            </view>
            <view class="counterparty">
              <text class="meta-label">Counterparty</text>
              <text class="counterparty-name">{{ item.counterpartyName }}</text>
              <text class="counterparty-user">@{{ item.counterpartyUsername }}</text>
            </view>
          </view>

          <text v-if="item.note" class="note-copy">{{ item.note }}</text>

          <view class="action-row">
            <view class="utility-actions">
              <text class="text-action" @click="openSupportChat">Chat</text>
              <text class="text-action danger-action" @click="hideTransaction(item)">Hide</text>
            </view>
            <view class="status-actions">
              <button
                v-for="action in nextActions(item.status)"
                :key="`${item.id}-${action}`"
                :class="[action === 'disputed' ? 'ghost-button' : 'primary-button', 'action-button']"
                :disabled="loadingId === item.id"
                @click="advanceStatus(item.id, action)"
              >{{ actionLabel(action) }}</button>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-card">
        <text class="section-title">No matching orders</text>
        <text class="muted">Choose another status or create a sell order from the rate desk.</text>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>

    <AppNav current="transactions" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import { cardLogoFor } from '@/utils/art'
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
  if (activeFilter.value === 'all') return store.state.transactions
  return store.state.transactions.filter((item) => item.status === activeFilter.value)
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
    processing: 'Mark processing',
    completed: 'Mark complete',
    disputed: 'Raise dispute',
    pending: 'Mark pending',
    canceled: 'Canceled'
  }[status]
}

async function advanceStatus(transactionId: string, status: TransactionItem['status']) {
  loadingId.value = transactionId
  try {
    await store.updateTransactionStatus(transactionId, status)
    notice.value = `Order moved to ${statusLabel(status)}.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Update failed'
  } finally {
    loadingId.value = ''
  }
}

function openSupportChat() {
  uni.redirectTo({ url: '/pages/support/index' })
}

function hideTransaction(item: TransactionItem) {
  uni.showModal({
    title: 'Hide order',
    content: 'This only hides the order from your list. The platform and support team still keep the full record.',
    confirmText: 'Hide',
    confirmColor: '#b42318',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.hideRecord({ targetType: 'ORDER', targetId: item.id, hiddenScope: 'ORDER' })
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
  padding-bottom: calc(154rpx + env(safe-area-inset-bottom));
}

.transactions-content {
  width: 100%;
  max-width: 1040rpx;
  margin: 0 auto;
}

.page-header-copy .eyebrow,
.page-header-copy .title,
.page-header-copy .subtitle {
  display: block;
}

.page-header-copy .title {
  margin-top: 7rpx;
}

.page-header-copy .subtitle {
  margin-top: 8rpx;
}

.orders-hero {
  padding: 28rpx;
  align-items: center;
  border-bottom: 0;
}

.order-count {
  min-width: 64rpx;
  color: var(--cb-coral-strong);
  font-size: 42rpx;
  font-weight: 700;
  text-align: right;
}

.filter-scroll {
  width: 100%;
  margin-top: 24rpx;
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  white-space: nowrap;
}

.filter-row {
  display: flex;
  width: max-content;
  min-width: 100%;
}

.filter-tab {
  position: relative;
  min-width: 140rpx;
  height: 76rpx;
  padding: 0 22rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6f7178;
  font-size: 24rpx;
  font-weight: 700;
}

.filter-tab.active-filter {
  color: var(--cb-coral-strong);
}

.filter-tab.active-filter::after {
  content: "";
  position: absolute;
  left: 20rpx;
  right: 20rpx;
  bottom: 0;
  height: 4rpx;
  background: var(--cb-coral-strong);
}

.transaction-list {
  margin-top: 20rpx;
  border: 1rpx solid var(--cb-line);
  background: #ffffff;
  border-radius: 12rpx;
  overflow: hidden;
}

.transaction-row {
  position: relative;
  padding: 24rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.transaction-row:last-child {
  border-bottom: 0;
}

.transaction-row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 24rpx;
  bottom: 24rpx;
  width: 4rpx;
  background: var(--cb-coral-strong);
}

.transaction-row.order-processing::before { background: var(--cb-sky-strong); }
.transaction-row.order-completed::before { background: var(--cb-mint-strong); }
.transaction-row.order-disputed::before,
.transaction-row.order-canceled::before { background: var(--cb-coral-strong); }

.transaction-head,
.transaction-main,
.action-row,
.utility-actions,
.status-actions {
  display: flex;
}

.transaction-head,
.action-row {
  justify-content: space-between;
  gap: 18rpx;
}

.transaction-head {
  align-items: flex-start;
}

.order-heading {
  min-width: 0;
}

.order-no,
.order-time {
  display: block;
}

.order-no {
  color: #111111;
  font-size: 27rpx;
  font-weight: 700;
}

.order-time {
  margin-top: 6rpx;
  color: #777980;
  font-size: 21rpx;
}

.status-pill.done {
  color: #137a4e;
  background: #eaf6f0;
}

.status-pill.warning {
  color: #9a5b00;
  background: #fff4df;
}

.status-pill.danger {
  color: #b42318;
  background: #fff0ee;
}

.transaction-main {
  margin-top: 22rpx;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(190rpx, 240rpx);
  gap: 22rpx;
}

.trade-card-identity {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.trade-card-logo {
  width: 58rpx;
  height: 58rpx;
  flex: 0 0 auto;
  border: 1rpx solid #dedfe3;
  background: #f7f7f8;
}

.trade-summary {
  min-width: 0;
}

.card-title,
.trade-line,
.payout-line,
.meta-label,
.counterparty-name,
.counterparty-user {
  display: block;
}

.card-title {
  color: #111111;
  font-size: 30rpx;
  font-weight: 700;
}

.trade-line {
  margin-top: 10rpx;
  color: #6f7178;
  font-size: 23rpx;
}

.payout-line {
  margin-top: 8rpx;
  color: #137a4e;
  font-size: 29rpx;
  font-weight: 700;
}

.counterparty {
  padding-left: 20rpx;
  border-left: 1rpx solid #dedfe3;
}

.meta-label {
  color: #777980;
  font-size: 20rpx;
  text-transform: uppercase;
}

.counterparty-name {
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111111;
  font-size: 25rpx;
  font-weight: 700;
}

.counterparty-user {
  margin-top: 5rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #6f7178;
  font-size: 21rpx;
}

.note-copy {
  display: block;
  margin-top: 20rpx;
  padding: 14rpx 16rpx;
  background: #f7f7f8;
  color: #6f7178;
  font-size: 23rpx;
  line-height: 1.45;
}

.action-row {
  margin-top: 22rpx;
  padding-top: 18rpx;
  align-items: center;
  border-top: 1rpx solid #dedfe3;
}

.utility-actions,
.status-actions {
  align-items: center;
  gap: 14rpx;
}

.text-action {
  padding: 12rpx 4rpx;
  color: var(--cb-sky-strong);
  font-size: 23rpx;
  font-weight: 700;
}

.danger-action {
  color: #b42318;
}

.action-button {
  min-height: 66rpx;
  margin: 0;
  padding: 14rpx 18rpx;
  font-size: 23rpx;
}

.status-actions .primary-button {
  background: var(--cb-coral-strong);
}

.empty-card {
  margin-top: 20rpx;
  padding: 58rpx 28rpx;
  text-align: center;
  background: #ffffff;
  border: 1rpx solid #dedfe3;
}

.empty-card .muted {
  display: block;
  margin-top: 10rpx;
}

.notice-text {
  display: block;
  margin-top: 20rpx;
  color: #6f7178;
  font-size: 23rpx;
  text-align: center;
}

@media (max-width: 520px) {
  .transaction-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .counterparty {
    padding: 16rpx 0 0;
    border-left: 0;
    border-top: 1rpx solid #dedfe3;
  }

  .action-row {
    align-items: stretch;
    flex-direction: column;
  }

  .status-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-button {
    width: 100%;
  }
}

@media (min-width: 768px) {
  .transactions-content {
    max-width: 960px;
  }
}
</style>
