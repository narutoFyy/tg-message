<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">Wallet</text>
        <view style="height: 12rpx"></view>
        <text class="title">Your payout overview</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">A lightweight balance view built from completed and in-flight trades.</text>
      </view>

      <view class="panel balance-panel">
        <view>
          <text class="balance-label">Available balance</text>
          <text class="balance-value">{{ availableBalance }}</text>
        </view>
        <view>
          <text class="balance-label">Pending settlement</text>
          <text class="balance-value small">{{ pendingBalance }}</text>
        </view>
      </view>

      <view class="panel">
        <text class="section-title">Recent completed payouts</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in completedTrades" :key="item.id" class="trade-row">
          <view>
            <text class="trade-name">{{ item.cardName }}</text>
            <text class="trade-meta">{{ item.counterpartyName }} · {{ item.updatedAt }}</text>
          </view>
          <text class="trade-value">{{ item.payoutAmount }}</text>
        </view>
      </view>

      <view class="panel">
        <button class="primary-button" @click="goTransactions">Open transaction records</button>
        <view style="height: 16rpx"></view>
        <button class="ghost-button" @click="goWithdraw">Withdraw</button>
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

const completedTrades = computed(() => store.state.transactions.filter((item) => item.status === 'completed'))
const pendingTrades = computed(() => store.state.transactions.filter((item) => item.status === 'pending' || item.status === 'processing'))

function parseNgn(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

function formatNaira(value: number) {
  return `₦${value.toLocaleString('en-US')}`
}

const availableBalance = computed(() =>
  formatNaira(completedTrades.value.reduce((sum, item) => sum + parseNgn(item.payoutAmount), 0))
)

const pendingBalance = computed(() =>
  formatNaira(pendingTrades.value.reduce((sum, item) => sum + parseNgn(item.payoutAmount), 0))
)

function goTransactions() {
  uni.redirectTo({ url: '/pages/transactions/index' })
}

function goWithdraw() {
  uni.navigateTo({ url: '/pages/withdraw/index' })
}
</script>

<style scoped lang="scss">
.balance-panel {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.balance-label {
  display: block;
  font-size: 24rpx;
  color: #6f7780;
}

.balance-value {
  display: block;
  margin-top: 10rpx;
  font-size: 48rpx;
  font-weight: 900;
  color: #171717;
}

.balance-value.small {
  font-size: 34rpx;
}

.trade-row {
  padding: 18rpx 0;
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.trade-name {
  display: block;
  font-size: 28rpx;
  font-weight: 800;
}

.trade-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #8b8b8b;
}

.trade-value {
  font-size: 28rpx;
  font-weight: 800;
  color: #0f9b57;
}
</style>
