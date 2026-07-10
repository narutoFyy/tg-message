<template>
  <view class="page-shell soft-page wallet-page">
    <view class="page-stack">
      <view class="page-header wallet-hero tone-finance">
        <view class="page-header-copy">
          <text class="eyebrow">Wallet</text>
          <text class="title">Payout balance</text>
          <text class="subtitle">Completed and in-flight order settlements.</text>
        </view>
        <text class="orders-link" @click="goTransactions">Orders</text>
      </view>

      <view class="balance-section tone-finance">
        <view class="balance-primary">
          <text class="balance-label">Available balance</text>
          <text class="balance-value">{{ availableBalance }}</text>
        </view>
        <view class="balance-secondary">
          <text class="balance-label">Pending settlement</text>
          <text class="pending-value">{{ pendingBalance }}</text>
        </view>
      </view>

      <view v-if="registrationBonus" class="content-section bonus-section">
        <view class="section-head">
          <text class="section-title">Registration bonus</text>
          <text :class="['status-chip', registrationBonus.status]">{{ registrationBonus.status }}</text>
        </view>
        <view class="bonus-row">
          <view>
            <text class="trade-name">{{ registrationBonus.bonusAmount }} {{ registrationBonus.currencyCode || '' }}</text>
            <text class="trade-meta">{{ registrationBonus.countryCode || 'Unknown country' }} / {{ registrationBonus.createdAt }}</text>
            <text class="trade-meta">{{ registrationBonus.reason }}</text>
          </view>
        </view>
      </view>

      <view class="content-section">
        <view class="section-head">
          <text class="section-title">Recent payouts</text>
          <text class="section-count">{{ completedTrades.length }}</text>
        </view>
        <view v-if="completedTrades.length" class="trade-list">
          <view v-for="item in completedTrades" :key="item.id" class="trade-row">
            <view class="trade-copy">
              <text class="trade-name">{{ item.cardName }}</text>
              <text class="trade-meta">{{ item.counterpartyName }} / {{ item.updatedAt }}</text>
            </view>
            <text class="trade-value">{{ item.payoutAmount }}</text>
          </view>
        </view>
        <view v-else class="empty-row">
          <text class="muted">Completed payouts will appear here.</text>
        </view>
      </view>

      <button class="primary-button withdraw-button" @click="goWithdraw">Withdraw funds</button>
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
const registrationBonus = computed(() => store.state.registrationBonusRecord)

function parseNgn(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

function formatNaira(value: number) {
  return `NGN ${value.toLocaleString('en-US')}`
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
.wallet-page {
  padding-bottom: 48rpx;
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

.wallet-hero {
  padding: 28rpx;
  align-items: center;
  border-bottom: 0;
}

.orders-link {
  padding: 14rpx 0;
  color: #002fa7;
  font-size: 25rpx;
  font-weight: 700;
}

.balance-section {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  border-left: 5rpx solid var(--cb-mint-strong);
}

.balance-primary,
.balance-secondary {
  padding: 28rpx;
}

.balance-secondary {
  border-left: 1rpx solid #dedfe3;
}

.balance-label,
.balance-value,
.pending-value {
  display: block;
}

.balance-label {
  color: #6f7178;
  font-size: 22rpx;
}

.balance-value {
  margin-top: 12rpx;
  color: #0f7f49;
  font-size: 44rpx;
  font-weight: 700;
}

.pending-value {
  margin-top: 15rpx;
  color: #111111;
  font-size: 30rpx;
  font-weight: 700;
}

.content-section {
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  overflow: hidden;
}

.bonus-section {
  background: var(--cb-amber);
  border-color: #f0daa4;
}

.section-head {
  min-height: 78rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.section-count {
  color: #6f7178;
  font-size: 23rpx;
}

.bonus-row,
.trade-row,
.empty-row {
  padding: 20rpx 24rpx;
}

.trade-row {
  min-height: 82rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.trade-row:last-child {
  border-bottom: 0;
}

.trade-copy {
  min-width: 0;
}

.trade-name,
.trade-meta {
  display: block;
}

.trade-name {
  color: #111111;
  font-size: 26rpx;
  font-weight: 700;
}

.trade-meta {
  margin-top: 6rpx;
  color: #777980;
  font-size: 21rpx;
  line-height: 1.4;
}

.trade-value {
  flex: 0 0 auto;
  color: #137a4e;
  font-size: 26rpx;
  font-weight: 700;
}

.status-chip {
  padding: 7rpx 12rpx;
  border-radius: 4rpx;
  background: #efeff1;
  color: #6f7178;
  font-size: 20rpx;
  font-weight: 700;
  text-transform: capitalize;
}

.status-chip.available {
  color: #137a4e;
  background: #eaf6f0;
}

.status-chip.skipped {
  color: #9a5b00;
  background: #fff4df;
}

.withdraw-button {
  width: 100%;
  background: var(--cb-mint-strong);
}

@media (max-width: 420px) {
  .balance-section {
    grid-template-columns: minmax(0, 1fr);
  }

  .balance-secondary {
    border-top: 1rpx solid #dedfe3;
    border-left: 0;
  }
}
</style>
