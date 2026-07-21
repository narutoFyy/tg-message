<template>
  <view class="page-shell soft-page performance-page">
    <view class="page-stack">
      <view class="page-header performance-hero tone-social">
        <view class="page-header-copy">
          <text class="eyebrow">VIP</text>
          <text class="title">Level and benefits</text>
          <text class="subtitle">Lifetime completed USD volume and available rewards.</text>
        </view>
      </view>

      <view class="tier-section">
        <view class="tier-line">
          <view><text class="tier-label">Current level</text><text class="headline">{{ tierName }}</text></view>
          <text class="tier-volume">${{ vipSummary?.points || '0' }} USD</text>
        </view>
        <view class="progress-track"><view class="progress-fill" :style="{ width: `${progress}%` }"></view></view>
        <text class="progress-copy">{{ progressText }}</text>
      </view>

      <view class="benefit-section">
        <view class="section-head"><text class="section-title">Current benefits</text><text class="section-count">{{ vipBenefits?.vipLevel || 'VIP0' }}</text></view>
        <view class="benefit-row">
          <view><text class="benefit-title">Lucky Wheel</text><text class="benefit-meta">{{ drawSchedule }}</text></view>
          <button class="ghost-button mini-action" @click="goLottery">Open</button>
        </view>
        <view class="benefit-row">
          <view><text class="benefit-title">Birthday reward</text><text class="benefit-meta">{{ birthdayText }}</text></view>
          <button v-if="vipBenefits?.birthdayEligible" class="primary-button mini-action" :disabled="claiming" @click="claimBirthday">Claim</button>
        </view>
        <view class="benefit-row">
          <view><text class="benefit-title">Support red packet</text><text class="benefit-meta">{{ supportRewardText }}</text></view>
          <button v-if="vipBenefits?.supportRedPacketEligible" class="primary-button mini-action" :disabled="claiming" @click="requestSupportReward">Request</button>
        </view>
        <view v-for="holiday in vipBenefits?.holidayRewards || []" :key="holiday.id" class="benefit-row">
          <view><text class="benefit-title">{{ holiday.holidayName }}</text><text class="benefit-meta">{{ holiday.currencyCode }} {{ holiday.rewardAmount }} / {{ holiday.holidayDate }}</text></view>
          <button v-if="holiday.claimable" class="primary-button mini-action" :disabled="claiming" @click="claimHoliday(holiday.id)">Claim</button>
          <text v-else class="benefit-state">{{ holiday.claimed ? 'Claimed' : 'Unavailable' }}</text>
        </view>
      </view>

      <view class="claim-section">
        <view class="section-head"><text class="section-title">Benefit history</text><text class="section-count">{{ recentClaims.length }}</text></view>
        <view v-for="claim in recentClaims" :key="claim.id" class="claim-row">
          <view><text class="benefit-title">{{ claimLabel(claim.benefitType) }}</text><text class="benefit-meta">{{ claim.currencyCode }} {{ claim.localAmount }} / {{ claim.requestedAt }}</text></view>
          <text :class="['status-pill', claim.status === 'approved' ? 'active' : claim.status === 'rejected' ? 'danger' : 'warning']">{{ claim.status }}</text>
        </view>
        <view v-if="!recentClaims.length" class="empty-row"><text class="muted">No benefit claims yet.</text></view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
      <view class="page-actions">
        <text class="support-link" @click="goSupport">Open support</text>
        <button class="primary-button orders-button" @click="goTransactions">View orders</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const notice = ref('')
const claiming = ref(false)

onShow(() => {
  store.bootstrap()
  store.refreshVipSummary().catch(() => undefined)
  store.refreshVipBenefits().catch(() => undefined)
})

const vipSummary = computed(() => store.state.vipSummary)
const vipBenefits = computed(() => store.state.vipBenefits)
const recentClaims = computed(() => store.state.vipBenefitClaims.slice(0, 8))
const tierName = computed(() => `${vipSummary.value?.level || 'VIP0'} / ${vipSummary.value?.levelName || 'New'}`)
const progress = computed(() => Math.max(0, Math.min(100, vipSummary.value?.progressPercent || 0)))
const progressText = computed(() => {
  const vip = vipSummary.value
  if (!vip) return 'Progress data is unavailable.'
  if (vip.maxLevel) return 'Highest level reached.'
  if (vip.level === 'VIP0') return 'Complete the first trade to reach VIP1.'
  return `$${vip.remainingPoints} USD to ${vip.nextLevel}`
})
const drawSchedule = computed(() => ({ VIP0: 'One registration chance', VIP1: 'One additional chance after the first completed trade', VIP2: 'One chance each calendar month', VIP3: 'One chance on days 1-15 and one on days 16-end', VIP4: 'One chance each Monday-Sunday week', VIP5: 'One chance each Monday-Sunday week' }[vipBenefits.value?.vipLevel || 'VIP0']))
const birthdayText = computed(() => {
  if (!vipBenefits.value?.birthDate) return 'Set your birthday in Personal information.'
  if (!vipBenefits.value.birthdayRewardDisplay) return `Saved as ${vipBenefits.value.birthDate}. Available from VIP2.`
  return `${vipBenefits.value.birthdayRewardDisplay} on ${vipBenefits.value.birthDate.slice(5)} each year`
})
const supportRewardText = computed(() => vipBenefits.value?.supportRewardDisplay ? `${vipBenefits.value.supportRewardDisplay} once per calendar month, subject to approval` : 'Available from VIP4 when configured')

async function runClaim(action: () => Promise<unknown>, success: string) {
  claiming.value = true
  notice.value = ''
  try { await action(); notice.value = success }
  catch (error) { notice.value = error instanceof Error ? error.message : 'Claim failed' }
  finally { claiming.value = false }
}
function claimBirthday() { return runClaim(() => store.claimVipBirthdayReward(), 'Birthday reward added to your wallet.') }
function requestSupportReward() { return runClaim(() => store.requestVipSupportRedPacket(), 'Request sent to support for approval.') }
function claimHoliday(id: string) { return runClaim(() => store.claimVipHolidayReward(id), 'Holiday reward added to your wallet.') }
function claimLabel(type: string) { return { birthday: 'Birthday reward', support_red_packet: 'Support red packet', holiday: 'Holiday reward' }[type] || type }
function goLottery() { uni.navigateTo({ url: '/pages/lucky-wheel/index' }) }
function goSupport() { uni.redirectTo({ url: '/pages/support/index' }) }
function goTransactions() { uni.redirectTo({ url: '/pages/transactions/index' }) }
</script>

<style scoped lang="scss">
.performance-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.performance-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.tier-section, .benefit-section, .claim-section { background: #ffffff; border: 1rpx solid #d9dde3; border-radius: 8rpx; overflow: hidden; }
.tier-section { padding: 28rpx; border-left: 5rpx solid #002fa7; }
.tier-line { display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; }
.tier-label, .headline { display: block; }
.tier-label { color: #68727d; font-size: 22rpx; }
.headline { margin-top: 8rpx; color: #101820; font-size: 34rpx; font-weight: 800; }
.tier-volume { color: #002fa7; font-size: 27rpx; font-weight: 800; text-align: right; }
.progress-track { height: 7rpx; margin-top: 24rpx; background: #e6e9ed; }
.progress-fill { height: 100%; background: #002fa7; }
.progress-copy { display: block; margin-top: 9rpx; color: #68727d; font-size: 21rpx; }
.section-head { min-height: 72rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #d9dde3; }
.section-count { color: #002fa7; font-size: 22rpx; font-weight: 800; }
.benefit-row, .claim-row { min-height: 86rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; border-bottom: 1rpx solid #e6e9ed; }
.benefit-row:last-child, .claim-row:last-child { border-bottom: 0; }
.benefit-title, .benefit-meta { display: block; }
.benefit-title { color: #101820; font-size: 25rpx; font-weight: 800; }
.benefit-meta { margin-top: 5rpx; color: #68727d; font-size: 21rpx; line-height: 1.4; }
.mini-action { flex: 0 0 auto; min-width: 130rpx; margin: 0; }
.benefit-state { color: #68727d; font-size: 21rpx; }
.status-pill.danger { color: #b42318; background: #fff0ee; }
.empty-row { padding: 26rpx 24rpx; }
.notice-text { display: block; color: #002fa7; font-size: 23rpx; text-align: center; }
.page-actions { display: flex; align-items: center; justify-content: flex-end; gap: 24rpx; }
.support-link { color: #002fa7; font-size: 24rpx; font-weight: 700; }
.orders-button { min-width: 250rpx; margin: 0; }
</style>
