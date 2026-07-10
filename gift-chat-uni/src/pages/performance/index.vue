<template>
  <view class="page-shell soft-page performance-page">
    <view class="page-stack">
      <view class="page-header performance-hero tone-social">
        <view class="page-header-copy">
          <text class="eyebrow">Performance</text>
          <text class="title">VIP and monthly progress</text>
          <text class="subtitle">Settlement activity and account progress.</text>
        </view>
      </view>

      <view class="tier-section tone-vip">
        <view class="tier-line">
          <view><text class="tier-label">Current tier</text><text class="headline">{{ tierName }}</text></view>
          <text class="tier-points">{{ vipSummary?.points || 0 }} pts</text>
        </view>
        <view class="progress-track"><view class="progress-fill" :style="{ width: `${progress}%` }"></view></view>
        <text class="progress-copy">{{ progressText }}</text>
      </view>

      <view class="stats-section">
        <view class="stat-row"><text class="stat-label">Monthly sales</text><text class="stat-value">{{ monthlySales }}</text></view>
        <view class="stat-row"><text class="stat-label">Support conversations</text><text class="stat-value">{{ supportConversations }}</text></view>
        <view class="stat-row"><text class="stat-label">Completed orders</text><text class="stat-value">{{ completedTrades }}</text></view>
        <view class="stat-row"><text class="stat-label">Open orders</text><text class="stat-value">{{ openTrades }}</text></view>
      </view>

      <view class="page-actions">
        <text class="support-link" @click="goSupport">Open support</text>
        <button class="primary-button orders-button" @click="goTransactions">View orders</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()
onShow(() => { store.bootstrap(); store.refreshVipSummary().catch(() => undefined) })

const vipSummary = computed(() => store.state.vipSummary)
const tierName = computed(() => `${vipSummary.value?.level || 'VIP1'} / ${vipSummary.value?.levelName || 'Bronze'}`)
const progress = computed(() => Math.max(0, Math.min(100, vipSummary.value?.progressPercent || 0)))
const progressText = computed(() => {
  const vip = vipSummary.value
  if (!vip) return 'Progress data is unavailable.'
  if (vip.maxLevel) return 'Highest level reached.'
  return `${vip.remainingPoints} points to ${vip.nextLevel}`
})
function parseNgn(value: string) { return Number(value.replace(/[^\d.]/g, '') || '0') }
function formatNaira(value: number) { return `NGN ${value.toLocaleString('en-US')}` }
const monthlySales = computed(() => formatNaira(store.state.transactions.filter((item) => item.status === 'completed').reduce((sum, item) => sum + parseNgn(item.payoutAmount), 0)))
const supportConversations = computed(() => store.state.supportConversations.length)
const completedTrades = computed(() => store.state.transactions.filter((item) => item.status === 'completed').length)
const openTrades = computed(() => store.state.transactions.filter((item) => item.status === 'pending' || item.status === 'processing').length)
function goSupport() { uni.redirectTo({ url: '/pages/support/index' }) }
function goTransactions() { uni.redirectTo({ url: '/pages/transactions/index' }) }
</script>

<style scoped lang="scss">
.performance-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.performance-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.tier-section { padding: 28rpx; border-left: 5rpx solid #f2c75c; }
.tier-line { display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; }
.tier-label, .headline { display: block; }
.tier-label { color: rgba(255,255,255,0.68); font-size: 22rpx; }
.headline { margin-top: 8rpx; color: #ffffff; font-size: 34rpx; font-weight: 700; }
.tier-points { color: #f2c75c; font-size: 27rpx; font-weight: 700; }
.progress-track { height: 7rpx; margin-top: 24rpx; overflow: hidden; background: rgba(255,255,255,0.2); }
.progress-fill { height: 100%; background: #f2c75c; }
.progress-copy { display: block; margin-top: 9rpx; color: rgba(255,255,255,0.7); font-size: 21rpx; }
.stats-section { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.stat-row:nth-child(1) { background: var(--cb-sky); }
.stat-row:nth-child(2) { background: var(--cb-mint); }
.stat-row:nth-child(3) { background: var(--cb-amber); }
.stat-row:nth-child(4) { background: var(--cb-coral); }
.stat-row { min-height: 78rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid #dedfe3; }
.stat-row:last-child { border-bottom: 0; }
.stat-label { color: #6f7178; font-size: 24rpx; }
.stat-value { color: #111111; font-size: 27rpx; font-weight: 700; text-align: right; }
.page-actions { display: flex; align-items: center; justify-content: flex-end; gap: 24rpx; }
.support-link { color: var(--cb-lilac-strong); font-size: 24rpx; font-weight: 700; }
.orders-button { min-width: 250rpx; margin: 0; }
</style>
