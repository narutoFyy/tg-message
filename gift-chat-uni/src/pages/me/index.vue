<template>
  <view class="page-shell soft-page me-page">
    <view class="me-content">
      <view class="me-head">
        <view>
          <text class="eyebrow">Account</text>
          <text class="me-title">Me</text>
        </view>
        <view class="head-actions">
          <image class="head-icon" :src="uiIcons.share" mode="aspectFit" @click="shareInvite" />
          <image class="head-icon" :src="uiIcons.settings" mode="aspectFit" @click="goSettings" />
        </view>
      </view>

      <view class="profile-section">
        <view class="avatar-wrap" @click="chooseAvatar">
          <image class="avatar-image" :src="avatarSrc" mode="aspectFill" />
          <view class="avatar-edit"><text>{{ avatarUploading ? '...' : '+' }}</text></view>
        </view>
        <view class="profile-copy">
          <text class="profile-name">{{ displayName }}</text>
          <view class="invite-row" @click.stop="copyInvite">
            <text class="invite">Invite code: {{ inviteCode }}</text>
            <image class="copy-icon" :src="uiIcons.copy" mode="aspectFit" />
          </view>
          <text class="invited-by">Invited by: {{ invitedBy }}</text>
        </view>
      </view>

      <view class="vip-row" @click="goPerformance">
        <view class="vip-copy-block">
          <view class="vip-title-row">
            <text class="vip-title">{{ vipSummary?.level || 'VIP0' }} / {{ vipSummary?.levelName || 'New member' }}</text>
            <text class="vip-points">USD {{ vipSummary?.points || '0' }} completed</text>
          </view>
          <view class="vip-progress-track">
            <view class="vip-progress-fill" :style="{ width: `${vipProgress}%` }"></view>
          </view>
          <text class="vip-progress-text">{{ vipProgressText }}</text>
        </view>
        <text class="row-action">View</text>
      </view>

      <view class="summary-strip">
        <view class="summary-item wallet-summary" @click="goWallet">
          <text class="summary-label">Wallet</text>
          <text class="summary-value">{{ walletValue }}</text>
        </view>
        <view class="summary-item invite-summary" @click="shareInvite">
          <text class="summary-label">Invited users</text>
          <text class="summary-value">{{ invitedUserCount }}</text>
        </view>
      </view>

      <view class="reward-card" @click="goLottery">
        <view>
          <text class="reward-label">Lucky Wheel</text>
          <text class="reward-value">{{ lotteryHint }}</text>
        </view>
        <image class="reward-art" src="/static/lottery/stone-technology.png" mode="aspectFit" />
      </view>

      <view class="menu-group identity-group">
        <view class="menu-row" @click="goProfileInfo">
          <view class="menu-copy">
            <text class="menu-title">Personal information</text>
            <text class="menu-subtitle">Account, phone, email and bank details</text>
          </view>
          <text class="row-action">Open</text>
        </view>
      </view>

      <view class="ranking-card" @click="goRanking">
        <text class="ranking-title">Rank (Month)</text>
        <view class="ranking-content">
          <view class="ranking-side">
            <text class="ranking-label">Sales</text>
            <text class="ranking-value">{{ salesValue }} / No.{{ salesRankLabel }}</text>
          </view>
          <image class="ranking-art" :src="pageArt.trophy" mode="aspectFit" />
          <view class="ranking-side align-right">
            <text class="ranking-label">Invitation</text>
            <text class="ranking-value">{{ invitedUserCount }} invites / No.{{ invitationRankLabel }}</text>
          </view>
        </view>
      </view>

      <view class="menu-group finance-group">
        <view class="menu-row" @click="goWallet">
          <view class="menu-copy">
            <text class="menu-title">Wallet</text>
            <text class="menu-subtitle">Payout balance and withdrawal tools</text>
          </view>
          <text class="row-action">{{ walletValue }}</text>
        </view>
        <view class="menu-row" @click="goLoan">
          <view class="menu-copy">
            <text class="menu-title">Loan applications</text>
            <text class="menu-subtitle">Apply and review existing requests</text>
          </view>
          <text class="row-action">Open</text>
        </view>
      </view>

      <view class="menu-group support-group">
        <view class="menu-row" @click="goSupport">
          <view class="menu-copy">
            <text class="menu-title">Support</text>
            <text class="menu-subtitle">Open your dedicated support conversation</text>
          </view>
          <text class="row-action">Chat</text>
        </view>
        <view class="menu-row" @click="goBlacklist">
          <view class="menu-copy">
            <text class="menu-title">Blacklist</text>
            <text class="menu-subtitle">Review blocked accounts</text>
          </view>
          <text class="row-action">Open</text>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>

    <AppNav current="me" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import { pageArt, uiIcons } from '@/utils/art'
import { fetchRanking, uploadImage } from '@/utils/api'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import type { RankingBoard } from '@/types'

const store = useAppStore()
const notice = ref('')
const avatarUploading = ref(false)
const salesRanking = ref<RankingBoard | null>(null)
const invitationRanking = ref<RankingBoard | null>(null)

onShow(() => {
  store.bootstrap()
  store.refreshBalanceSummary().catch(() => undefined)
  store.refreshCurrentAccount().catch(() => undefined)
  store.refreshVipSummary().catch(() => undefined)
  store.refreshLotteryEligibility().catch(() => undefined)
  refreshPersonalRankings()
})

const vipSummary = computed(() => store.state.vipSummary)
const vipProgress = computed(() => Math.max(0, Math.min(100, vipSummary.value?.progressPercent ?? 0)))
const vipProgressText = computed(() => {
  const vip = vipSummary.value
  if (!vip) return 'VIP progress unavailable'
  if (vip.maxLevel) return 'Highest level reached'
  if (vip.level === 'VIP0') return 'Complete your first trade to reach VIP1'
  return `USD ${vip.points} / USD ${vip.nextThreshold} to ${vip.nextLevel} / USD ${vip.remainingPoints} left`
})
const lotteryHint = computed(() => {
  const eligibility = store.state.lotteryEligibility
  if (!eligibility) return 'Check eligibility'
  return eligibility.eligible ? `${eligibility.availableChances} available` : (eligibility.nextAvailableAt || 'Not available')
})
const salesRankLabel = computed(() => rankLabel(salesRanking.value?.currentUser?.rank, '500+'))
const invitationCount = computed(() => invitationRanking.value?.currentUser?.score || '0 invites')
const invitedUserCount = computed(() => invitationCount.value.match(/\d+/)?.[0] || '0')
const invitationRankLabel = computed(() => rankLabel(invitationRanking.value?.currentUser?.rank, '100+'))
const displayName = computed(() => store.state.currentUser?.username || 'Xcard user')
const avatarSrc = computed(() => {
  const avatarUrl = store.state.currentUser?.avatarUrl
  return avatarUrl ? resolveMediaUrl(avatarUrl) : uiIcons.user
})
const inviteCode = computed(() => store.state.currentUser?.inviteCode || 'Not available')
const invitedBy = computed(() => store.state.currentUser?.invitedBy || 'Direct registration')

const completedPayout = computed(() =>
  store.state.transactions
    .filter((item) => item.status === 'completed')
    .reduce((sum, item) => sum + parseAmount(item.payoutAmount), 0)
)

const walletCurrency = computed(() => store.state.balanceSummary?.currencyCode || store.state.currentUser?.currencyCode || 'USD')
const walletValue = computed(() => `${walletCurrency.value} ${store.state.balanceSummary?.availableTotal || '0.00'}`)
const salesValue = computed(() => formatCurrency(completedPayout.value))

async function refreshPersonalRankings() {
  try {
    const [sales, invitation] = await Promise.all([fetchRanking('sales'), fetchRanking('invitation')])
    salesRanking.value = sales
    invitationRanking.value = invitation
  } catch {
    // Profile navigation remains available when ranking data is unavailable.
  }
}

function rankLabel(rank: number | undefined, fallback: string) {
  if (!rank) return fallback
  return rank >= 500 ? '500+' : `${rank}`
}

function parseAmount(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

function formatCurrency(value: number) {
  return `${walletCurrency.value} ${value.toLocaleString('en-US')}`
}

function goBlacklist() { uni.navigateTo({ url: '/pages/blacklist/index' }) }
function goSupport() { uni.redirectTo({ url: '/pages/support/index' }) }
function goLoan() { uni.navigateTo({ url: '/pages/loan/index' }) }
function goWallet() { uni.navigateTo({ url: '/pages/wallet/index' }) }
function goPerformance() { uni.navigateTo({ url: '/pages/performance/index' }) }
function goRanking() { uni.navigateTo({ url: '/pages/ranking/index' }) }
function goLottery() { uni.navigateTo({ url: '/pages/lucky-wheel/index' }) }
function goProfileInfo() { uni.navigateTo({ url: '/pages/profile-info/index' }) }
function goSettings() { uni.navigateTo({ url: '/pages/settings/index' }) }

function chooseAvatar() {
  if (avatarUploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    async success(result) {
      const tempFiles = Array.isArray(result.tempFiles) ? result.tempFiles : [result.tempFiles]
      const browserFile = tempFiles.find((item): item is File =>
        typeof File !== 'undefined' && item instanceof File
      )
      const tempFilePaths = Array.isArray(result.tempFilePaths) ? result.tempFilePaths : [result.tempFilePaths]
      const source = browserFile || tempFilePaths.find(Boolean)
      if (!source) return
      avatarUploading.value = true
      notice.value = ''
      try {
        const asset = await uploadImage(source)
        await store.updateAvatar(asset.publicUrl)
        notice.value = 'Avatar updated.'
      } catch (error) {
        notice.value = error instanceof Error ? error.message : 'Avatar update failed.'
      } finally {
        avatarUploading.value = false
      }
    }
  })
}

function copyInvite() {
  if (!store.state.currentUser?.inviteCode) {
    notice.value = 'Invite code is not available.'
    return
  }
  uni.setClipboardData({
    data: `Invite code: ${inviteCode.value}`,
    success() { notice.value = 'Invite code copied.' }
  })
}

function shareInvite() {
  if (!store.state.currentUser?.inviteCode) {
    notice.value = 'Invite code is not available.'
    return
  }
  uni.setClipboardData({
    data: `Join Xcard with my invite code: ${inviteCode.value}`,
    success() { notice.value = 'Share text copied to clipboard.' }
  })
}
</script>

<style scoped lang="scss">
.me-page {
  overflow-x: hidden;
}

.me-content {
  width: 100%;
  max-width: 1040rpx;
  margin: 0 auto;
}

.me-head {
  min-height: 82rpx;
  padding-bottom: 20rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  border-bottom: 1rpx solid #c8c9cf;
}

.me-head .eyebrow,
.me-title {
  display: block;
}

.me-title {
  margin-top: 5rpx;
  color: #111111;
  font-size: 40rpx;
  font-weight: 700;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.head-icon {
  width: 42rpx;
  height: 42rpx;
}

.profile-section {
  padding: 32rpx 4rpx 28rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar-wrap {
  position: relative;
  width: 116rpx;
  height: 116rpx;
  flex: 0 0 auto;
}

.avatar-image {
  width: 116rpx;
  height: 116rpx;
  border-radius: 50%;
  background: #efeff1;
}

.avatar-edit {
  position: absolute;
  right: -2rpx;
  bottom: -2rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  border: 3rpx solid #f7f7f8;
  background: var(--cb-lilac-strong);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23rpx;
  font-weight: 700;
}

.profile-copy {
  min-width: 0;
}

.invited-by {
  display: block;
  margin-top: 8rpx;
  color: #6e7772;
  font-size: 22rpx;
}

.profile-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111111;
  font-size: 36rpx;
  font-weight: 700;
}

.invite-row {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.invite {
  color: #6f7178;
  font-size: 24rpx;
}

.copy-icon {
  width: 26rpx;
  height: 26rpx;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.summary-item {
  min-width: 0;
  min-height: 118rpx;
  padding: 24rpx;
  box-sizing: border-box;
  border: 1rpx solid transparent;
  border-radius: 12rpx;
}

.wallet-summary {
  background: var(--cb-sky);
  border-color: #cfe4fb;
}

.invite-summary {
  background: var(--cb-mint);
  border-color: #c6ead8;
}

.summary-label,
.summary-value {
  display: block;
}

.summary-label {
  color: #6f7178;
  font-size: 20rpx;
}

.summary-value {
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111111;
  font-size: 26rpx;
  font-weight: 700;
}

.vip-row {
  margin-top: 22rpx;
  min-height: 112rpx;
  padding: 20rpx 22rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: var(--cb-navy);
  border: 1rpx solid #263b4b;
  border-radius: 12rpx;
  color: #ffffff;
  box-shadow: 0 14rpx 32rpx rgba(23, 38, 51, 0.14);
}

.vip-copy-block {
  min-width: 0;
  flex: 1;
}

.vip-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 18rpx;
}

.vip-title {
  color: #ffffff;
  font-size: 25rpx;
  font-weight: 700;
}

.vip-points,
.vip-progress-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 20rpx;
}

.vip-row .row-action {
  color: #ffffff;
}

.vip-progress-track {
  height: 6rpx;
  margin-top: 12rpx;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.2);
}

.vip-progress-fill {
  height: 100%;
  background: #f2c75c;
}

.vip-progress-text {
  display: block;
  margin-top: 7rpx;
}

.menu-group {
  margin-top: 22rpx;
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  overflow: hidden;
}

.identity-group { background: var(--cb-lilac); border-color: #ddd5fb; }
.finance-group { background: var(--cb-mint); border-color: #c6ead8; }
.support-group { background: var(--cb-coral); border-color: #f1c8c2; }

.menu-row {
  position: relative;
  min-height: 94rpx;
  padding: 18rpx 22rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border-bottom: 1rpx solid #dedfe3;
  background: rgba(255, 255, 255, 0.72);
}

.menu-row:last-child {
  border-bottom: 0;
}

.menu-row:active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 5rpx;
  background: var(--cb-lilac-strong);
}

.menu-copy {
  min-width: 0;
  flex: 1;
}

.menu-title,
.menu-subtitle {
  display: block;
}

.menu-title {
  color: #111111;
  font-size: 26rpx;
  font-weight: 700;
}

.menu-subtitle {
  margin-top: 5rpx;
  color: #6f7178;
  font-size: 21rpx;
  line-height: 1.35;
}

.row-action {
  flex: 0 0 auto;
  max-width: 210rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--cb-lilac-strong);
  font-size: 22rpx;
  font-weight: 700;
}

.reward-card {
  min-height: 126rpx;
  margin-top: 22rpx;
  padding: 22rpx 26rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  background: var(--cb-amber);
  border: 1rpx solid #f0daa4;
  border-radius: 12rpx;
}

.reward-label,
.reward-value {
  display: block;
}

.reward-label {
  color: #5f4200;
  font-size: 24rpx;
  font-weight: 700;
}

.reward-value {
  margin-top: 8rpx;
  color: #17212b;
  font-size: 30rpx;
  font-weight: 700;
}

.reward-art {
  width: 86rpx;
  height: 86rpx;
  border-radius: 8rpx;
  background: #1b130e;
  flex: 0 0 auto;
}

.ranking-card {
  margin-top: 22rpx;
  padding: 24rpx 26rpx;
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba(34, 54, 74, 0.05);
}

.ranking-title {
  display: block;
  color: #17212b;
  font-size: 26rpx;
  font-weight: 700;
}

.ranking-content {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150rpx minmax(0, 1fr);
  align-items: center;
  gap: 16rpx;
}

.ranking-side {
  min-width: 0;
}

.ranking-label,
.ranking-value {
  display: block;
}

.ranking-label {
  color: var(--cb-muted);
  font-size: 20rpx;
}

.ranking-value {
  margin-top: 7rpx;
  color: #17212b;
  font-size: 24rpx;
  font-weight: 700;
}

.ranking-art {
  width: 150rpx;
  height: 100rpx;
}

.align-right {
  text-align: right;
}

.notice-text {
  display: block;
  margin-top: 20rpx;
  color: #6f7178;
  font-size: 23rpx;
  text-align: center;
}

@media (max-width: 420px) {
  .summary-value {
    font-size: 23rpx;
  }
}

@media (min-width: 768px) {
  .me-content {
    max-width: 1280px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
  }

  .me-head,
  .profile-section,
  .vip-row,
  .summary-strip,
  .reward-card,
  .identity-group,
  .ranking-card,
  .notice-text {
    grid-column: 1 / -1;
  }

  .profile-section {
    flex-direction: column;
    justify-content: center;
    text-align: center;
  }

  .profile-copy {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .vip-row,
  .summary-strip,
  .reward-card,
  .menu-group,
  .ranking-card {
    margin-top: 0;
  }
}
</style>
