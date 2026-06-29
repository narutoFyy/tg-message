<template>
  <view class="page-shell soft-page home-page">
    <view class="status-row">
      <text class="clock">05:47</text>
      <text class="battery">30%</text>
    </view>

    <view class="top-head">
      <view class="brand-inline">
        <image class="brand-logo" src="/static/cardbrother-logo.png" mode="aspectFit" />
        <text class="brand-title">CardBrother</text>
      </view>
      <view class="country" @click="pickCountry">
        <text>{{ store.selectedCountry().name }}</text>
        <image class="country-arrow" :src="uiIcons.chevronDown" mode="aspectFit" />
      </view>
    </view>

    <view class="banner-card" @click="openTaskModal">
      <view class="banner-glow banner-glow-left"></view>
      <view class="banner-copy">
        <text class="banner-tag">Keep the Madness Alive</text>
        <text class="banner-title">Enhanced Rank Perks</text>
        <text class="banner-sub">₦10.43M Prize Pool: One Million Mega Grand Prize</text>
      </view>
      <image class="banner-art" :src="pageArt.homeBanner" mode="aspectFill" />
      <view class="banner-indicator"></view>
    </view>

    <view class="tab-card surface-card">
      <scroll-view scroll-x class="card-tabs-scroll" :show-scrollbar="false">
        <view class="card-tabs">
          <view
            v-for="cardName in cardCategories"
            :key="cardName"
            :class="['card-tab', activeCardName === cardName && 'active-tab']"
            @click="selectCardCategory(cardName)"
          >
            <text>{{ cardName }}</text>
            <view v-if="activeCardName === cardName" class="tab-active-line"></view>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="rate-list surface-card">
      <view v-for="rate in filteredRates" :key="rate.id" class="rate-row">
        <view class="rate-left">
          <image class="rate-icon" :src="cardLogoFor(rate.cardName)" mode="aspectFit" />
          <view class="rate-copy">
            <view class="rate-name-line">
              <text class="rate-name">{{ rate.cardName }}</text>
              <text :class="['rate-status', rate.status === 'active' ? 'active' : 'paused']">
                {{ rate.status === 'active' ? 'Active' : 'Paused' }}
              </text>
            </view>
            <text class="rate-updated">Updated {{ rate.updatedAt }}</text>
          </view>
        </view>
        <view class="rate-side">
          <text class="rate-value">{{ rate.rate }}</text>
          <button class="sell-link" @click="goSell(rate)">Sell</button>
        </view>
      </view>
    </view>

    <view v-if="showTaskModal" class="modal-mask">
      <view class="reward-modal surface-card">
        <view class="reward-top">
          <view class="reward-top-tag">
            <image class="reward-mini-logo" src="/static/cardbrother-logo.png" mode="aspectFit" />
            <text>CardBrother</text>
          </view>
          <view class="reward-gift"></view>
        </view>
        <view class="reward-core">
          <view class="coin-badge">
            <view class="coin-core"></view>
            <text>₦300</text>
          </view>
          <text class="reward-text">Daily Task: Sell at least ₦20000 worth of cards to claim the reward</text>
          <button class="primary-button reward-button" @click="dismissTaskModal">Go</button>
        </view>
      </view>
      <view class="close-ring" @click="dismissTaskModal">×</view>
    </view>

    <AppNav current="home" />
  </view>
</template>

<script setup lang="ts">
import { onHide, onShow } from '@dcloudio/uni-app'
import { computed, ref, watch } from 'vue'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import { cardLogoFor, pageArt, uiIcons } from '@/utils/art'

const store = useAppStore()
const TASK_MODAL_DISMISSED_KEY = 'cardbrother-task-modal-dismissed'
const showTaskModal = ref(false)
const ALL_CARD = 'All Card'
const activeCardName = ref(ALL_CARD)
let taskModalTimer: ReturnType<typeof setTimeout> | null = null

const cardCategories = computed(() => {
  const cardNames = countryRates.value.map((rate) => rate.cardName).filter(Boolean)
  return [ALL_CARD, ...Array.from(new Set(cardNames))]
})

const countryRates = computed(() => store.state.rates.filter((rate) => rate.region === store.state.selectedCountryCode))

const filteredRates = computed(() => {
  if (activeCardName.value === ALL_CARD) {
    return countryRates.value
  }
  return countryRates.value.filter((rate) => rate.cardName === activeCardName.value)
})

watch(cardCategories, (categories) => {
  if (!categories.includes(activeCardName.value)) {
    activeCardName.value = ALL_CARD
  }
})

onShow(() => {
  store.bootstrap()
})

onHide(() => {
  clearTaskModalTimer()
})

function pickCountry() {
  store.chooseCountry()
}

function selectCardCategory(cardName: string) {
  activeCardName.value = cardName
}

function goSell(rate: { id: string }) {
  uni.navigateTo({ url: `/pages/sell-card/index?rateId=${encodeURIComponent(rate.id)}` })
}

function openTaskModal() {
  clearTaskModalTimer()
  if (uni.getStorageSync(TASK_MODAL_DISMISSED_KEY)) {
    showTaskModal.value = false
    return
  }
  showTaskModal.value = true
}

function dismissTaskModal() {
  clearTaskModalTimer()
  showTaskModal.value = false
  uni.setStorageSync(TASK_MODAL_DISMISSED_KEY, '1')
}

function clearTaskModalTimer() {
  if (taskModalTimer) {
    clearTimeout(taskModalTimer)
    taskModalTimer = null
  }
}
</script>

<style scoped lang="scss">
.home-page {
  position: relative;
}

.status-row,
.top-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-row {
  font-size: 28rpx;
  font-weight: 900;
  margin-bottom: 16rpx;
}

.battery {
  color: #1dcf72;
}

.top-head {
  margin-bottom: 26rpx;
}

.brand-inline {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.brand-logo {
  width: 80rpx;
  height: 80rpx;
}

.brand-title,
.country {
  font-size: 38rpx;
  font-weight: 900;
}

.country {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.country-arrow {
  width: 28rpx;
  height: 28rpx;
}

.banner-card {
  position: relative;
  min-height: 190rpx;
  border-radius: 28rpx;
  padding: 26rpx 28rpx 32rpx;
  background: linear-gradient(118deg, #f55f49 0%, #fa8346 45%, #ffbf68 100%);
  overflow: hidden;
  color: #fff8f2;
}

.banner-copy {
  position: relative;
  z-index: 2;
  width: 58%;
}

.banner-glow {
  position: absolute;
  border-radius: 999rpx;
  background: rgba(255, 249, 202, 0.55);
  filter: blur(14rpx);
}

.banner-glow-left {
  width: 140rpx;
  height: 40rpx;
  left: -12rpx;
  top: 26rpx;
}

.banner-art {
  position: absolute;
  right: -10rpx;
  bottom: -2rpx;
  width: 360rpx;
  height: 168rpx;
  z-index: 1;
}

.banner-tag {
  display: inline-flex;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 251, 239, 0.94);
  color: #2d2720;
  font-size: 22rpx;
  font-weight: 800;
  box-shadow: 0 8rpx 24rpx rgba(124, 61, 26, 0.18);
}

.banner-title {
  display: block;
  margin-top: 20rpx;
  font-size: 52rpx;
  font-weight: 900;
  font-style: italic;
  line-height: 1;
  text-shadow: 0 6rpx 12rpx rgba(150, 70, 33, 0.18);
}

.banner-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
}

.banner-indicator {
  position: absolute;
  left: 50%;
  bottom: 16rpx;
  transform: translateX(-50%);
  width: 34rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.95);
  z-index: 2;
}

.tab-card {
  margin-top: 18rpx;
  padding: 18rpx 20rpx 10rpx;
  border-radius: 30rpx 30rpx 12rpx 12rpx;
}

.card-tabs-scroll {
  width: 100%;
  white-space: nowrap;
}

.card-tabs {
  display: flex;
  align-items: flex-end;
  gap: 34rpx;
  width: max-content;
  min-width: 100%;
  font-size: 28rpx;
  font-weight: 800;
}

.card-tab {
  min-height: 58rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 12rpx;
  color: #161616;
  flex: 0 0 auto;
}

.active-tab {
  color: #0ec76a;
}

.rate-list {
  margin-top: 8rpx;
  padding: 8rpx 0;
  border-radius: 10rpx 10rpx 34rpx 34rpx;
}

.rate-row {
  min-height: 126rpx;
  padding: 0 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid #eef0f2;
}

.rate-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
}

.rate-icon {
  width: 62rpx;
  height: 62rpx;
  flex: 0 0 auto;
}

.rate-copy {
  min-width: 0;
}

.rate-name-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.rate-name,
.rate-value {
  font-size: 28rpx;
  font-weight: 700;
}

.rate-name {
  max-width: 260rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rate-value {
  color: #23262a;
  flex: 0 0 auto;
}

.rate-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
  flex: 0 0 auto;
}

.sell-link {
  min-width: 112rpx;
  height: 50rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #0fd26b;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 50rpx;
}

.rate-status {
  flex: 0 0 auto;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  font-size: 18rpx;
  font-weight: 900;
}

.rate-status.active {
  color: #0a9c53;
  background: rgba(20, 216, 111, 0.12);
}

.rate-status.paused {
  color: #dd8a25;
  background: rgba(255, 184, 76, 0.2);
}

.rate-updated {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #8a949e;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.52);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
}

.reward-modal {
  width: 540rpx;
  padding: 0;
  overflow: visible;
  background: linear-gradient(180deg, #efffd8 0%, #d6ffd5 100%);
  box-shadow: 0 28rpx 70rpx rgba(0, 0, 0, 0.2);
}

.reward-top {
  position: relative;
  width: 460rpx;
  height: 118rpx;
  margin: -38rpx auto 0;
  border-radius: 30rpx;
  background: linear-gradient(180deg, #4bc384, #1e985c);
  box-shadow: 0 18rpx 38rpx rgba(23, 133, 79, 0.3);
}

.reward-top-tag {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 24rpx 28rpx;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 800;
}

.reward-mini-logo {
  width: 26rpx;
  height: 26rpx;
}

.reward-gift {
  position: absolute;
  right: 24rpx;
  top: -26rpx;
  width: 112rpx;
  height: 112rpx;
  border-radius: 26rpx;
  background: linear-gradient(180deg, #ffe384, #ffbf3b);
  transform: rotate(13deg);
  box-shadow: 0 12rpx 28rpx rgba(255, 185, 61, 0.45);
}

.reward-gift::before,
.reward-gift::after {
  content: '';
  position: absolute;
  background: #fff7d0;
}

.reward-gift::before {
  left: 43rpx;
  top: 0;
  width: 18rpx;
  height: 112rpx;
}

.reward-gift::after {
  left: 0;
  top: 40rpx;
  width: 112rpx;
  height: 18rpx;
}

.reward-core {
  padding: 34rpx 40rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28rpx;
}

.coin-badge {
  width: 230rpx;
  height: 230rpx;
  border-radius: 28rpx;
  background: linear-gradient(180deg, #fff5d7 0%, #ffe09c 100%);
  box-shadow: inset 0 0 0 12rpx rgba(255, 177, 82, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.coin-core {
  width: 82rpx;
  height: 82rpx;
  border-radius: 41rpx;
  background: radial-gradient(circle at 38% 32%, #fff5b9 0%, #f5b72a 65%, #dc8a00 100%);
  position: relative;
}

.coin-core::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 24rpx;
  height: 24rpx;
  background: #fff8ea;
  transform: translate(-50%, -50%) rotate(45deg);
}

.coin-badge text {
  font-size: 52rpx;
  font-weight: 900;
  color: #0f7257;
}

.reward-text {
  text-align: center;
  font-size: 28rpx;
  color: #137052;
  line-height: 1.5;
}

.reward-button {
  width: 100%;
  border-radius: 999rpx;
  font-size: 42rpx;
}

.close-ring {
  position: absolute;
  bottom: 132rpx;
  width: 92rpx;
  height: 92rpx;
  border-radius: 46rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.9);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 62rpx;
  line-height: 1;
}
</style>
