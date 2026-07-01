<template>
  <view class="page-shell soft-page me-page">
    <view class="me-backdrop"></view>

    <view class="status-row">
      <text class="clock">05:48</text>
      <text class="battery">31%</text>
    </view>

    <view class="me-head">
      <text class="me-title">Me</text>
      <view class="icons">
        <image class="head-icon" :src="uiIcons.share" mode="aspectFit" @click="shareInvite" />
        <image class="head-icon" :src="uiIcons.settings" mode="aspectFit" @click="goSettings" />
      </view>
    </view>

    <view class="profile-card" @click="goSettings">
      <image class="avatar-image" :src="pageArt.profileAvatar" mode="aspectFit" />
      <text class="profile-name">{{ displayName }}</text>
      <view class="invite-row" @click.stop="copyInvite">
        <text class="invite">Invite code: {{ inviteCode }}</text>
        <image class="copy-icon" :src="uiIcons.copy" mode="aspectFit" />
      </view>
    </view>

    <view class="vip-card" @click="goPerformance">
      <view class="vip-left">
        <image class="vip-badge" :src="pageArt.vipBadge" mode="aspectFit" />
        <text class="vip-label">VIP 1</text>
      </view>
      <view class="bronze-pill">
        <text>Bronze</text>
        <text class="bronze-arrow">></text>
      </view>
      <view class="vip-notch"></view>
    </view>

    <view class="quick-grid">
      <view class="quick-card wallet" @click="goWallet">
        <view>
          <text class="quick-title">Wallet</text>
          <text class="quick-value">{{ walletValue }}</text>
        </view>
        <image class="quick-art wallet-art" :src="pageArt.wallet" mode="aspectFit" />
      </view>
      <view class="quick-card support-card" @click="goSupport">
        <view>
          <text class="quick-title">Support</text>
          <text class="quick-value">{{ supportCount }}</text>
        </view>
        <image class="quick-art friend-art" :src="pageArt.friend" mode="aspectFit" />
      </view>
    </view>

    <view class="rank-card" @click="goRanking">
      <text class="rank-title">Rank ( Month )</text>
      <view class="rank-row">
        <view class="rank-col">
          <text class="muted label-muted">Sales</text>
          <text class="rank-value">{{ monthlySales }}/ No.500+</text>
        </view>
        <image class="rank-art" :src="pageArt.trophy" mode="aspectFit" />
        <view class="rank-col align-right">
          <text class="muted label-muted">Invitation</text>
          <text class="rank-value">{{ invitationCount }}/ No.100+</text>
        </view>
      </view>
      <view class="rank-ribbon"></view>
    </view>

    <view class="panel manage-panel">
      <text class="manage-title">Contact and Management</text>
      <view class="manage-grid">
        <view class="manage-item" @click="goBlacklist">
          <image class="manage-icon" :src="uiIcons.scammer" mode="aspectFit" />
          <text class="manage-label">Scammer</text>
        </view>
        <view class="manage-item" @click="goSupport">
          <image class="manage-icon" :src="uiIcons.staff" mode="aspectFit" />
          <text class="manage-label">Staff Lookup</text>
        </view>
        <view class="manage-item" @click="goLoan">
          <image class="manage-icon" :src="uiIcons.wallet" mode="aspectFit" />
          <text class="manage-label">Loan</text>
        </view>
      </view>
    </view>

    <text v-if="notice" class="notice-text">{{ notice }}</text>

    <AppNav current="me" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import { pageArt, uiIcons } from '@/utils/art'

const store = useAppStore()
const notice = ref('')

onShow(() => {
  store.bootstrap()
})

const supportCount = computed(() => store.state.supportConversations.length)
const invitationCount = computed(() => store.state.ranking?.currentUser?.score || '0')
const displayName = computed(() => store.state.currentUser?.username || 'Jay Crown')
const inviteCode = computed(() => {
  const base = (store.state.currentUser?.username || 'dhceqw').replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
  return (base + 'QW').slice(0, 6)
})

const completedPayout = computed(() =>
  store.state.transactions
    .filter((item) => item.status === 'completed')
    .reduce((sum, item) => sum + parseNgn(item.payoutAmount), 0)
)

const monthlySales = computed(() => formatNaira(completedPayout.value))
const walletValue = computed(() => formatNaira(completedPayout.value))

function parseNgn(value: string) {
  const digits = value.replace(/[^\d.]/g, '')
  return Number(digits || '0')
}

function formatNaira(value: number) {
  return `₦${value.toLocaleString('en-US')}`
}

function goBlacklist() {
  uni.navigateTo({ url: '/pages/blacklist/index' })
}

function goSupport() {
  uni.redirectTo({ url: '/pages/support/index' })
}

function goLoan() {
  uni.navigateTo({ url: '/pages/loan/index' })
}

function goWallet() {
  uni.navigateTo({ url: '/pages/wallet/index' })
}

function goPerformance() {
  uni.navigateTo({ url: '/pages/performance/index' })
}

function goRanking() {
  uni.navigateTo({ url: '/pages/ranking/index' })
}

function goSettings() {
  uni.navigateTo({ url: '/pages/settings/index' })
}

function copyInvite() {
  uni.setClipboardData({
    data: `Invite code: ${inviteCode.value}`,
    success() {
      notice.value = 'Invite code copied.'
    }
  })
}

function shareInvite() {
  const text = `Join CardBrother with my invite code: ${inviteCode.value}`
  uni.setClipboardData({
    data: text,
    success() {
      notice.value = 'Share text copied to clipboard.'
    }
  })
}
</script>

<style scoped lang="scss">
.me-page {
  position: relative;
  overflow: hidden;
}

.me-backdrop {
  position: absolute;
  inset: 0 0 auto;
  height: 620rpx;
  background: linear-gradient(180deg, #edf4f8 0%, rgba(247, 249, 251, 0.92) 48%, rgba(255, 255, 255, 0) 100%);
  pointer-events: none;
}

.status-row,
.me-head {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-row {
  font-size: 28rpx;
  font-weight: 900;
  margin-bottom: 14rpx;
}

.battery {
  color: #0088cc;
}

.me-head {
  margin-bottom: 22rpx;
}

.me-title {
  font-size: 48rpx;
  font-weight: 900;
}

.icons {
  display: flex;
  gap: 28rpx;
}

.head-icon {
  width: 50rpx;
  height: 50rpx;
}

.profile-card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 0 10rpx;
}

.avatar-image {
  width: 152rpx;
  height: 152rpx;
}

.profile-name {
  font-size: 46rpx;
  font-weight: 900;
}

.invite-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.invite {
  font-size: 28rpx;
  color: #4b4b4b;
}

.copy-icon {
  width: 30rpx;
  height: 30rpx;
}

.vip-card {
  position: relative;
  z-index: 1;
  margin-top: 22rpx;
  min-height: 122rpx;
  border-radius: 16rpx;
  padding: 0 26rpx;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.08), transparent 36%),
    linear-gradient(180deg, #17212b, #223241);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
}

.vip-card::before,
.vip-card::after {
  content: '';
  position: absolute;
  border-radius: 999rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.08);
  transform: rotate(-12deg);
}

.vip-card::before {
  width: 220rpx;
  height: 220rpx;
  right: 150rpx;
  top: -110rpx;
}

.vip-card::after {
  width: 260rpx;
  height: 260rpx;
  right: -60rpx;
  top: -80rpx;
}

.vip-left {
  display: flex;
  align-items: center;
  gap: 18rpx;
  position: relative;
  z-index: 1;
}

.vip-badge {
  width: 72rpx;
  height: 56rpx;
}

.vip-label {
  font-size: 44rpx;
  font-weight: 900;
}

.bronze-pill {
  position: relative;
  z-index: 1;
  min-width: 208rpx;
  height: 74rpx;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 34rpx;
  font-weight: 800;
}

.bronze-arrow {
  font-size: 28rpx;
}

.vip-notch {
  position: absolute;
  left: 50%;
  bottom: -16rpx;
  width: 42rpx;
  height: 32rpx;
  background: #2d315b;
  transform: translateX(-50%) rotate(45deg);
}

.quick-grid {
  position: relative;
  z-index: 1;
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.quick-card {
  min-height: 158rpx;
  border-radius: 12rpx;
  padding: 22rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
}

.wallet {
  background: #f3f6f8;
}

.support-card {
  background: rgba(0, 136, 204, 0.08);
}

.quick-title {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  margin-bottom: 14rpx;
}

.quick-value {
  font-size: 46rpx;
  font-weight: 900;
}

.quick-art {
  width: 120rpx;
  height: 120rpx;
  flex: 0 0 auto;
}

.wallet-art {
  margin-right: -8rpx;
}

.friend-art {
  width: 126rpx;
  height: 126rpx;
  margin-right: -10rpx;
}

.rank-card {
  position: relative;
  z-index: 1;
  margin-top: 22rpx;
  min-height: 258rpx;
  border-radius: 16rpx;
  padding: 26rpx 26rpx 22rpx;
  background: #ffffff;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
  overflow: hidden;
}

.rank-title {
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  margin-bottom: 18rpx;
}

.rank-row {
  display: grid;
  grid-template-columns: 1fr 220rpx 1fr;
  align-items: center;
}

.rank-col {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.align-right {
  align-items: flex-end;
}

.label-muted {
  font-size: 24rpx;
}

.rank-value {
  font-size: 34rpx;
  font-weight: 900;
  color: #171717;
}

.rank-art {
  width: 220rpx;
  height: 146rpx;
}

.rank-ribbon {
  position: absolute;
  left: -20rpx;
  right: -20rpx;
  bottom: -14rpx;
  height: 52rpx;
  border-radius: 50%;
  border-bottom: 10rpx solid #17d56f;
}

.manage-panel {
  position: relative;
  z-index: 1;
  margin-top: 26rpx;
  padding-top: 34rpx;
  padding-bottom: 38rpx;
}

.manage-title {
  font-size: 32rpx;
  font-weight: 900;
}

.manage-grid {
  display: flex;
  gap: 48rpx;
  padding-top: 34rpx;
}

.manage-item {
  width: 190rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.manage-icon {
  width: 74rpx;
  height: 74rpx;
}

.manage-label {
  font-size: 28rpx;
  font-weight: 700;
}

.notice-text {
  display: block;
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
