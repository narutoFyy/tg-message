<template>
  <view class="page-shell soft-page lucky-page">
    <view class="draw-layout">
      <view class="wheel-panel">
        <view class="wheel-panel-head">
          <view>
            <text class="eyebrow">Lucky Wheel</text>
            <text class="title hero-title">{{ eligibility?.vipLevel || 'VIP1' }} draw</text>
          </view>
          <text :class="['chance-pill', eligibility?.eligible && 'available']">{{ eligibility?.eligible ? 'Available' : 'Locked' }}</text>
        </view>

        <view class="wheel-stage">
          <view class="pointer"></view>
          <view class="wheel" :style="{ transform: `rotate(${rotation}deg)` }">
            <view
              v-for="(prize, index) in wheelPrizes"
              :key="`${prize}-${index}`"
              :class="['wheel-label', wheelPrizeImage(prize) && 'has-image']"
              :style="labelStyle(index, prize)"
            >
              <image v-if="wheelPrizeImage(prize)" class="wheel-prize-thumb" :src="wheelPrizeImage(prize)" mode="aspectFit" />
              <text>{{ prize }}</text>
            </view>
          </view>
          <view class="wheel-center">
            <image src="/static/lottery/gsmz.jpg" mode="aspectFit" />
          </view>
        </view>

        <button class="primary-button spin-button" :disabled="spinning || !eligibility?.eligible" @click="handleSpin">
          {{ spinning ? 'Spinning...' : 'Spin' }}
        </button>
        <text class="spin-hint">{{ spinHint }}</text>
      </view>

      <view class="side-panel">
        <view class="chance-card">
          <view>
            <text class="eyebrow">Draw status</text>
            <text class="side-title">{{ eligibilityText }}</text>
          </view>
          <image class="brand-logo" src="/static/lottery/gsmz.jpg" mode="aspectFit" />
        </view>

        <view class="prize-panel">
          <view class="panel-head">
            <text class="section-title">Prize display</text>
            <text class="muted">Physical prizes shown below</text>
          </view>
          <view class="prize-list">
            <view v-for="prize in featuredPrizes" :key="prize.name" class="prize-card">
              <image class="prize-image" :src="prize.image" mode="aspectFit" />
              <view class="prize-copy">
                <text class="prize-name">{{ prize.name }}</text>
                <text class="prize-note">{{ prize.note }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="lastPrize" class="result-panel">
          <text class="section-title">Congratulations</text>
          <text class="result-prize">{{ lastPrize }}</text>
          <text class="muted">Your prize record has been saved.</text>
        </view>

        <view class="winner-panel">
          <text class="section-title">Recent winners</text>
          <view class="winner-window">
            <view class="winner-track">
              <view v-for="(winner, index) in tickerWinners" :key="`${winner.displayName}-${winner.prizeName}-${index}`" class="winner-row">
                <view>
                  <text class="winner-prize">{{ winner.prizeName }}</text>
                  <text class="winner-time">{{ winner.drawnAt || 'Recent draw' }}</text>
                </view>
                <text class="winner-name">{{ winner.displayName }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const spinning = ref(false)
const rotation = ref(0)
const lastPrize = ref('')
const featuredPrizes = [
  { name: 'iPhone 17', note: 'Phone prize', image: '/static/lottery/iphone.jpg' },
  { name: 'iPad', note: 'Tablet prize', image: '/static/lottery/ipad.jpg' },
  { name: 'Computer', note: 'Laptop prize', image: '/static/lottery/diannao.jpg' }
]
const wheelPrizes = ['₦1000', 'iPad', '₦2000', 'Computer', '₦3000', '₦5000', '₦8000', '₦10000', '₦15000', 'iPhone 17']
const drawablePrizeNames = ['₦1000', '₦2000', '₦3000', '₦5000']
const spinDurationMs = 4000

onShow(() => {
  store.refreshLotteryEligibility().catch(() => undefined)
  store.refreshLotteryWinners().catch(() => undefined)
})

const eligibility = computed(() => store.state.lotteryEligibility)
const eligibilityText = computed(() => {
  if (!eligibility.value) return 'Checking your draw chance.'
  if (eligibility.value.eligible) return 'You have a draw chance available.'
  return eligibility.value.message || 'No draw chance available.'
})
const spinHint = computed(() => {
  if (!eligibility.value) return ''
  if (eligibility.value.eligible) return 'Good luck!'
  return eligibility.value.nextAvailableAt ? `Next: ${eligibility.value.nextAvailableAt}` : 'Upgrade VIP or wait for reset.'
})
const tickerWinners = computed(() => {
  const winners = store.state.lotteryWinners
  return winners.length ? [...winners, ...winners] : []
})

function wheelPrizeImage(prize: string) {
  return featuredPrizes.find((item) => item.name === prize)?.image || ''
}

function labelStyle(index: number, prize: string) {
  const angle = index * (360 / wheelPrizes.length) + 18
  const distance = wheelPrizeImage(prize) ? 166 : 154
  return `transform: rotate(${angle}deg) translateY(-${distance}rpx) rotate(-${angle}deg);`
}

function pickDrawablePrize() {
  return drawablePrizeNames[Math.floor(Math.random() * drawablePrizeNames.length)]
}

function targetRotationForPrize(prizeName: string) {
  const targetIndex = wheelPrizes.findIndex((prize) => prize === prizeName)
  if (targetIndex < 0) return rotation.value + 1800
  const sliceAngle = 360 / wheelPrizes.length
  const targetCenterAngle = targetIndex * sliceAngle + sliceAngle / 2
  const targetBaseRotation = -targetCenterAngle
  const minRotation = rotation.value + 5 * 360
  const maxRotation = rotation.value + 15 * 360
  const kMin = Math.ceil((minRotation - targetBaseRotation) / 360)
  const kMax = Math.floor((maxRotation - targetBaseRotation) / 360)
  const k = kMin + Math.floor(Math.random() * (kMax - kMin + 1))
  const jitter = (Math.random() - 0.5) * sliceAngle * 0.3
  return targetBaseRotation + k * 360 + jitter
}

function waitForSpin() {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, spinDurationMs)
  })
}

async function handleSpin() {
  if (spinning.value || !eligibility.value?.eligible) return
  spinning.value = true
  lastPrize.value = ''
  const targetPrize = pickDrawablePrize()
  rotation.value = targetRotationForPrize(targetPrize)
  try {
    await waitForSpin()
    const result = await store.spinLottery(targetPrize)
    lastPrize.value = result.prize.name
    spinning.value = false
  } catch (error) {
    spinning.value = false
    uni.showToast({ title: error instanceof Error ? error.message : 'Spin failed', icon: 'none' })
  }
}
</script>

<style scoped lang="scss">
.lucky-page {
  min-height: 100vh;
  padding-bottom: 60rpx;
  max-width: 1280px;
  margin: 0 auto;
  box-sizing: border-box;
}

.draw-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 20rpx;
  align-items: start;
}

.wheel-panel,
.side-panel {
  position: relative;
  z-index: 1;
}

.wheel-panel {
  min-height: 620rpx;
  padding: 26rpx;
  border-radius: 8rpx;
  background:
    radial-gradient(circle at 50% 42%, rgba(0, 136, 204, 0.08), transparent 38%),
    #ffffff;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.wheel-panel-head {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 16rpx;
}

.hero-title {
  display: block;
  margin-top: 8rpx;
  font-size: 40rpx;
  line-height: 1.15;
}

.chance-pill {
  min-width: 132rpx;
  height: 54rpx;
  padding: 0 18rpx;
  border-radius: 8rpx;
  background: #eff3f6;
  color: #697583;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 54rpx;
  text-align: center;
  box-sizing: border-box;
}

.chance-pill.available {
  background: rgba(20, 216, 111, 0.14);
  color: #0a9c53;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.chance-card {
  min-height: 138rpx;
  padding: 22rpx;
  border-radius: 8rpx;
  background: #1e242b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  box-sizing: border-box;
}

.side-title {
  display: block;
  margin-top: 10rpx;
  font-size: 30rpx;
  line-height: 1.35;
  font-weight: 900;
}

.brand-logo {
  width: 116rpx;
  height: 116rpx;
  border-radius: 8rpx;
  background: #fff;
  flex: 0 0 auto;
}

.wheel-stage {
  width: min(660rpx, 390px);
  aspect-ratio: 1;
  margin: 4rpx auto 24rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pointer {
  position: absolute;
  top: -10rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 30rpx solid transparent;
  border-right: 30rpx solid transparent;
  border-top: 70rpx solid #e5b93f;
  z-index: 4;
}

.wheel {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: conic-gradient(#ff6b6b 0 36deg, #ff69b4 36deg 72deg, #4ecdc4 72deg 108deg, #8b6d3d 108deg 144deg, #ffe66d 144deg 180deg, #95e1d3 180deg 216deg, #f38181 216deg 252deg, #aa96da 252deg 288deg, #fcbad3 288deg 324deg, #ffd700 324deg 360deg);
  border: 8rpx solid #fff;
  box-shadow: 0 22rpx 54rpx rgba(24, 31, 38, 0.24);
  position: relative;
  transition: transform 4s cubic-bezier(0.16, 0.72, 0.14, 1);
}

.wheel-label {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 130rpx;
  margin-left: -65rpx;
  margin-top: -22rpx;
  text-align: center;
  font-size: 23rpx;
  font-weight: 900;
  color: #202020;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
}

.wheel-label.has-image {
  width: 138rpx;
  margin-left: -69rpx;
  margin-top: -42rpx;
  font-size: 19rpx;
  line-height: 1.05;
}

.wheel-prize-thumb {
  width: 74rpx;
  height: 50rpx;
  border-radius: 6rpx;
  background: rgba(255, 255, 255, 0.82);
}

.wheel-center {
  position: absolute;
  width: 142rpx;
  height: 142rpx;
  border-radius: 50%;
  background: #fff;
  border: 6rpx solid #e5b93f;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
}

.wheel-center image {
  width: 110rpx;
  height: 110rpx;
}

.spin-button {
  width: 360rpx;
  margin: 0 auto;
}

.spin-hint {
  display: block;
  text-align: center;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #6a717a;
  min-height: 34rpx;
}

.prize-panel {
  padding: 20rpx;
  border-radius: 8rpx;
  background: #fff;
  border: 1rpx solid rgba(136, 153, 166, 0.14);
}

.panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.prize-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12rpx;
}

.prize-card {
  min-height: 102rpx;
  padding: 12rpx;
  border-radius: 8rpx;
  background: #f7fafb;
  border: 1rpx solid rgba(136, 153, 166, 0.12);
  display: grid;
  grid-template-columns: 112rpx minmax(0, 1fr);
  align-items: center;
  gap: 14rpx;
  box-sizing: border-box;
}

.prize-image {
  width: 112rpx;
  height: 78rpx;
  border-radius: 6rpx;
  background: #fff;
}

.prize-copy {
  min-width: 0;
}

.prize-name {
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  color: #20262d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prize-note {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #697583;
}

.result-panel,
.winner-panel {
  padding: 20rpx;
  border-radius: 8rpx;
  background: #fff;
  border: 1rpx solid rgba(136, 153, 166, 0.14);
}

.result-prize {
  display: block;
  margin: 12rpx 0;
  font-size: 48rpx;
  font-weight: 900;
  color: #d74848;
}

.winner-window {
  height: 318rpx;
  margin-top: 16rpx;
  overflow: hidden;
  border-radius: 8rpx;
  background: #f7fafb;
}

.winner-track {
  animation: winnerScroll 18s linear infinite;
}

.winner-row {
  min-height: 86rpx;
  padding: 0 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid rgba(136, 153, 166, 0.14);
  box-sizing: border-box;
}

.winner-prize {
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  color: #d69822;
}

.winner-time {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #7b8792;
}

.winner-name {
  max-width: 260rpx;
  font-size: 24rpx;
  font-weight: 800;
  color: #4c5661;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes winnerScroll {
  from { transform: translateY(0); }
  to { transform: translateY(-50%); }
}

@media (max-width: 900px) {
  .lucky-page {
    max-width: none;
  }

  .draw-layout {
    grid-template-columns: 1fr;
  }

  .wheel-panel {
    min-height: 680rpx;
  }

  .wheel-stage {
    width: min(700rpx, calc(100vw - 76rpx));
  }

  .winner-window {
    height: 310rpx;
  }

  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .prize-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .prize-card {
    grid-template-columns: 1fr;
    min-height: 210rpx;
    align-content: start;
  }

  .prize-image {
    width: 100%;
    height: 116rpx;
  }

  .prize-name,
  .prize-note {
    text-align: center;
  }
}
</style>
