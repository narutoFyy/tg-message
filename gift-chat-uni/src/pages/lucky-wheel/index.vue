<template>
  <view class="page-shell soft-page lucky-page">
    <view class="page-header lucky-head tone-reward">
      <view class="page-header-copy"><text class="eyebrow">Games</text><text class="title">Lucky Wheel</text><text class="subtitle">{{ eligibility?.vipLevel || 'VIP1' }} account draw</text></view>
      <text :class="['chance-status', eligibility?.eligible && 'available']">{{ eligibility?.eligible ? 'Available' : 'Locked' }}</text>
    </view>

    <view class="draw-layout">
      <view class="wheel-section">
        <view class="wheel-stage">
          <view class="pointer"></view>
          <view class="wheel" :style="{ transform: `rotate(${rotation}deg)` }">
            <view v-for="index in wheelPrizes.length" :key="`spoke-${index}`" class="wheel-spoke" :style="{ transform: `rotate(${index * (360 / wheelPrizes.length)}deg)` }"></view>
            <view v-for="(prize, index) in wheelPrizes" :key="`${prize}-${index}`" :class="['wheel-label', wheelPrizeImage(prize) && 'has-image']" :style="labelStyle(index, prize)">
              <image v-if="wheelPrizeImage(prize)" class="wheel-prize-thumb" :src="wheelPrizeImage(prize)" mode="aspectFit" />
              <text>{{ prize }}</text>
            </view>
          </view>
          <view class="wheel-center"><image src="/static/lottery/stone-technology-icon.png" mode="aspectFill" /></view>
        </view>
        <button class="primary-button spin-button" :disabled="spinning || !eligibility?.eligible" @click="handleSpin">{{ spinning ? 'Drawing...' : 'Start draw' }}</button>
        <text class="spin-hint">{{ spinHint }}</text>
      </view>

      <view class="draw-sidebar">
        <view class="status-section"><text class="section-title">Draw status</text><text class="status-copy">{{ eligibilityText }}</text></view>
        <view v-if="lastPrize" class="result-section">
          <text class="eyebrow">Result saved</text><text class="result-prize">{{ lastPrize }}</text><text class="muted">This prize is now recorded on your account.</text>
          <button class="primary-button withdrawal-button" @click="goBindBankAccount">Set up withdrawal</button>
        </view>
        <view class="prize-section">
          <view class="section-head"><text class="section-title">Featured prizes</text><text class="section-count">{{ featuredPrizes.length }}</text></view>
          <view class="prize-list">
            <view v-for="prize in featuredPrizes" :key="prize.name" class="prize-row"><image class="prize-image" :src="prize.image" mode="aspectFit" /><view><text class="prize-name">{{ prize.name }}</text><text class="prize-note">{{ prize.note }}</text></view></view>
          </view>
        </view>
        <view class="winner-section">
          <view class="section-head"><text class="section-title">Recent winners</text><text class="section-count">{{ recentWinners.length }}</text></view>
          <view v-for="winner in recentWinners" :key="`${winner.displayName}-${winner.prizeName}-${winner.drawnAt}`" class="winner-row"><view><text class="winner-prize">{{ winner.prizeName }}</text><text class="winner-time">{{ winner.drawnAt || 'Recent draw' }}</text></view><text class="winner-name">{{ winner.displayName }}</text></view>
          <view v-if="!recentWinners.length" class="empty-winners"><text class="muted">No recent winners.</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
const store = useAppStore(); const spinning = ref(false); const rotation = ref(0); const lastPrize = ref(''); const lastRecordId = ref('')
const featuredPrizes = [
  { name: 'iPhone 17', note: 'Phone prize', image: '/static/lottery/iphone.jpg' },
  { name: 'iPad', note: 'Tablet prize', image: '/static/lottery/ipad.jpg' },
  { name: 'Computer', note: 'Laptop prize', image: '/static/lottery/diannao.jpg' }
]
const wheelPrizes = ['NGN 500', 'iPad', 'NGN 800', 'Computer', 'NGN 500', 'NGN 1,000', 'NGN 2,000', 'NGN 3,000', 'NGN 5,000', 'iPhone 17']
const spinDurationMs = 4000
onShow(() => { store.refreshLotteryEligibility().catch(() => undefined); store.refreshLotteryWinners().catch(() => undefined) })
const eligibility = computed(() => store.state.lotteryEligibility)
const recentWinners = computed(() => store.state.lotteryWinners.slice(0, 6))
const eligibilityText = computed(() => { if (!eligibility.value) return 'Checking your draw chance.'; if (eligibility.value.eligible) return 'You have a draw chance available.'; return eligibility.value.message || 'No draw chance available.' })
const spinHint = computed(() => { if (!eligibility.value) return ''; if (eligibility.value.eligible) return 'The server determines the final prize.'; return eligibility.value.nextAvailableAt ? `Next: ${eligibility.value.nextAvailableAt}` : 'Upgrade VIP or wait for reset.' })
function wheelPrizeImage(prize: string) { return featuredPrizes.find((item) => item.name === prize)?.image || '' }
function labelStyle(index: number, prize: string) { const angle = index * (360 / wheelPrizes.length) + 18; const distance = wheelPrizeImage(prize) ? 176 : 185; return `transform: rotate(${angle}deg) translateY(-${distance}rpx) rotate(-${angle}deg);` }
function prizeKey(prize: string) {
  const cashValue = prize.replace(/[^\d]/g, '')
  return cashValue ? `cash-${cashValue}` : prize.trim().toLowerCase()
}
function targetRotationForPrize(prizeName: string) {
  const targetIndex = wheelPrizes.findIndex((prize) => prizeKey(prize) === prizeKey(prizeName)); if (targetIndex < 0) return rotation.value + 1800
  const sliceAngle = 360 / wheelPrizes.length; const targetCenterAngle = targetIndex * sliceAngle + sliceAngle / 2; const targetBaseRotation = -targetCenterAngle
  const minRotation = rotation.value + 5 * 360; const maxRotation = rotation.value + 15 * 360; const kMin = Math.ceil((minRotation - targetBaseRotation) / 360); const kMax = Math.floor((maxRotation - targetBaseRotation) / 360)
  const k = kMin + Math.floor(Math.random() * (kMax - kMin + 1)); const jitter = (Math.random() - 0.5) * sliceAngle * 0.3
  return targetBaseRotation + k * 360 + jitter
}
function waitForSpin() { return new Promise<void>((resolve) => { setTimeout(resolve, spinDurationMs) }) }
async function handleSpin() {
  if (spinning.value || !eligibility.value?.eligible) return
  spinning.value = true; lastPrize.value = ''; lastRecordId.value = ''
  try { const result = await store.spinLottery(); rotation.value = targetRotationForPrize(result.prize.name); await waitForSpin(); lastPrize.value = result.prize.name; lastRecordId.value = result.recordId }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : 'Draw failed', icon: 'none' }) }
  finally { spinning.value = false }
}
function goBindBankAccount() { if (lastRecordId.value) uni.navigateTo({ url: `/pages/withdraw/index?lotteryRecordId=${encodeURIComponent(lastRecordId.value)}` }) }
</script>

<style scoped lang="scss">
.lucky-page { min-height: 100vh; padding-bottom: 48rpx; max-width: 1280px; margin: 0 auto; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.lucky-head { padding: 28rpx; align-items: center; border-bottom: 0; }
.chance-status { flex: 0 0 auto; padding: 8rpx 14rpx; border-radius: 4rpx; background: #efeff1; color: #6f7178; font-size: 22rpx; font-weight: 700; }
.chance-status.available { color: #137a4e; background: #eaf6f0; }
.draw-layout { margin-top: 26rpx; display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 22rpx; align-items: start; }
.wheel-section { min-height: 650rpx; padding: 28rpx; box-sizing: border-box; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--cb-sky); border: 1rpx solid #cfe4fb; border-radius: 12rpx; }
.wheel-stage { width: min(620rpx, 380px); aspect-ratio: 1; position: relative; display: flex; align-items: center; justify-content: center; }
.pointer { position: absolute; top: -3rpx; left: 50%; width: 0; height: 0; transform: translateX(-50%); border-left: 18rpx solid transparent; border-right: 18rpx solid transparent; border-top: 48rpx solid #002fa7; z-index: 4; }
.wheel { position: relative; width: 100%; height: 100%; overflow: hidden; border: 7rpx solid #ffffff; border-radius: 50%; background: conic-gradient(var(--cb-amber) 0 36deg, var(--cb-mint) 36deg 72deg, var(--cb-sky) 72deg 108deg, var(--cb-coral) 108deg 144deg, var(--cb-lilac) 144deg 180deg, #ffe8b8 180deg 216deg, #caf2de 216deg 252deg, #d7eaff 252deg 288deg, #ffd8d2 288deg 324deg, #e2dafc 324deg 360deg); box-sizing: border-box; box-shadow: 0 18rpx 46rpx rgba(34, 54, 74, 0.14); transition: transform 4s cubic-bezier(0.16, 0.72, 0.14, 1); }
.wheel-spoke { position: absolute; left: 50%; top: 50%; width: 2rpx; height: 50%; background: #c8c9cf; transform-origin: 50% 0; }
.wheel-spoke:nth-child(odd) { background: #002fa7; }
.wheel-label { position: absolute; left: 50%; top: 50%; width: 108rpx; margin-left: -54rpx; margin-top: -18rpx; display: flex; flex-direction: column; align-items: center; gap: 3rpx; color: #111111; font-size: 18rpx; font-weight: 700; text-align: center; }
.wheel-label.has-image { margin-top: -38rpx; font-size: 17rpx; }
.wheel-prize-thumb { width: 66rpx; height: 44rpx; background: #ffffff; border: 1rpx solid #dedfe3; }
.wheel-center { position: absolute; width: 112rpx; height: 112rpx; overflow: hidden; border: 5rpx solid #002fa7; border-radius: 50%; background: #ffffff; z-index: 3; }
.wheel-center image { width: 100%; height: 100%; }
.spin-button { width: min(100%, 360rpx); margin-top: 26rpx; background: var(--cb-amber-strong); color: #382600; }
.spin-hint { display: block; min-height: 32rpx; margin-top: 12rpx; color: #6f7178; font-size: 22rpx; text-align: center; }
.draw-sidebar { display: flex; flex-direction: column; gap: 18rpx; }
.status-section, .result-section { padding: 22rpx 24rpx; background: var(--cb-mint); border-left: 5rpx solid var(--cb-mint-strong); border-radius: 12rpx; }
.status-copy { display: block; margin-top: 9rpx; color: #6f7178; font-size: 24rpx; line-height: 1.4; }
.result-section { background: var(--cb-amber); border-top: 1rpx solid #f0daa4; border-right: 1rpx solid #f0daa4; border-bottom: 1rpx solid #f0daa4; }
.result-prize { display: block; margin: 9rpx 0; color: #002fa7; font-size: 38rpx; font-weight: 700; }
.withdrawal-button { width: 100%; margin-top: 20rpx; }
.prize-section, .winner-section { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.prize-section { background: var(--cb-sky); border-color: #cfe4fb; }
.winner-section { background: var(--cb-lilac); border-color: #ddd5fb; }
.section-head { min-height: 72rpx; padding: 0 20rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #dedfe3; }
.section-count { color: #6f7178; font-size: 21rpx; }
.prize-row { min-height: 92rpx; padding: 12rpx 18rpx; box-sizing: border-box; display: grid; grid-template-columns: 94rpx minmax(0, 1fr); align-items: center; gap: 14rpx; border-bottom: 1rpx solid #dedfe3; }
.prize-row:last-child { border-bottom: 0; }
.prize-image { width: 94rpx; height: 66rpx; background: #f7f7f8; }
.prize-name, .prize-note { display: block; }
.prize-name { color: #111111; font-size: 25rpx; font-weight: 700; }
.prize-note { margin-top: 5rpx; color: #6f7178; font-size: 20rpx; }
.winner-row { min-height: 76rpx; padding: 12rpx 18rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 14rpx; border-bottom: 1rpx solid #dedfe3; }
.winner-row:last-child { border-bottom: 0; }
.winner-prize, .winner-time { display: block; }
.winner-prize { color: #111111; font-size: 23rpx; font-weight: 700; }
.winner-time { margin-top: 4rpx; color: #777980; font-size: 18rpx; }
.winner-name { max-width: 190rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #002fa7; font-size: 21rpx; font-weight: 700; }
.empty-winners { padding: 24rpx 18rpx; }
@media (max-width: 900px) { .draw-layout { grid-template-columns: minmax(0, 1fr); } .wheel-stage { width: min(620rpx, calc(100vw - 96rpx)); } }
@media (max-width: 420px) { .wheel-section { min-height: 570rpx; padding: 20rpx 12rpx; } .wheel-stage { width: min(560rpx, calc(100vw - 76rpx)); } .wheel-label { transform-origin: center; font-size: 18rpx; } }
</style>
