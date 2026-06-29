<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">Performance</text>
        <view style="height: 12rpx"></view>
        <text class="title">VIP and monthly progress</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">A practical summary behind the VIP card and rank banner on the Me page.</text>
      </view>

      <view class="panel">
        <text class="section-title">Current tier</text>
        <view style="height: 16rpx"></view>
        <text class="headline">VIP 1 · Bronze</text>
        <view style="height: 10rpx"></view>
        <text class="muted">Keep settling trades and inviting new verified users to unlock the next rank band.</text>
      </view>

      <view class="panel stats-grid">
        <view>
          <text class="stat-label">Monthly sales</text>
          <text class="stat-value">{{ monthlySales }}</text>
        </view>
        <view>
          <text class="stat-label">Support sessions</text>
          <text class="stat-value">{{ outgoingInvites }}</text>
        </view>
        <view>
          <text class="stat-label">Completed trades</text>
          <text class="stat-value">{{ completedTrades }}</text>
        </view>
        <view>
          <text class="stat-label">Open trades</text>
          <text class="stat-value">{{ openTrades }}</text>
        </view>
      </view>

      <view class="panel actions-panel">
        <button class="ghost-button" @click="goSupport">Open support</button>
        <button class="primary-button" @click="goTransactions">Open transactions</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()

onShow(() => {
  store.bootstrap()
})

function parseNgn(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

function formatNaira(value: number) {
  return `₦${value.toLocaleString('en-US')}`
}

const monthlySales = computed(() =>
  formatNaira(
    store.state.transactions
      .filter((item) => item.status === 'completed')
      .reduce((sum, item) => sum + parseNgn(item.payoutAmount), 0)
  )
)

const outgoingInvites = computed(() => store.state.supportConversations.length)
const completedTrades = computed(() => store.state.transactions.filter((item) => item.status === 'completed').length)
const openTrades = computed(() => store.state.transactions.filter((item) => item.status === 'pending' || item.status === 'processing').length)

function goSupport() {
  uni.redirectTo({ url: '/pages/support/index' })
}

function goTransactions() {
  uni.redirectTo({ url: '/pages/transactions/index' })
}
</script>

<style scoped lang="scss">
.headline {
  display: block;
  font-size: 42rpx;
  font-weight: 900;
  color: #181818;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: #6f7780;
}

.stat-value {
  display: block;
  margin-top: 10rpx;
  font-size: 38rpx;
  font-weight: 900;
}

.actions-panel {
  display: flex;
  gap: 16rpx;
}

.actions-panel button {
  flex: 1;
}
</style>
