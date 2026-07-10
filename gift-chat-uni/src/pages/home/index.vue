<template>
  <view class="page-shell home-page">
    <view class="home-content">
      <view class="top-head">
        <view class="brand-inline">
          <image class="brand-logo" src="/static/cardbrother-logo.png" mode="aspectFit" />
          <view>
            <text class="brand-title">CardBrother</text>
            <text class="brand-caption">Rate desk</text>
          </view>
        </view>
        <view class="country" @click="pickCountry">
          <text>{{ store.selectedCountry().name }}</text>
          <image class="country-arrow" :src="uiIcons.chevronDown" mode="aspectFit" />
        </view>
      </view>

      <view class="desk-intro tone-market">
        <view class="intro-copy">
          <text class="eyebrow">Current market</text>
          <text class="title">Live gift card rates</text>
          <text class="subtitle">Compare active rates for {{ store.selectedCountry().name }} and start a sale.</text>
        </view>
        <view class="intro-meta">
          <text class="rate-count">{{ countryRates.length }}</text>
          <text class="rate-count-label">listed rates</text>
        </view>
      </view>

      <view class="rate-tools">
        <view class="search-field">
          <text class="search-mark"></text>
          <input v-model.trim="searchQuery" class="search-input" placeholder="Search card rates" />
          <text v-if="searchQuery" class="clear-search" @click="searchQuery = ''">Clear</text>
        </view>

        <scroll-view scroll-x class="card-tabs-scroll" :show-scrollbar="false">
          <view class="card-tabs">
            <view
              v-for="cardName in cardCategories"
              :key="cardName"
              :class="['card-tab', activeCardName === cardName && 'active-tab']"
              @click="selectCardCategory(cardName)"
            >
              <text>{{ cardName }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="rate-list">
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
            <view class="rate-price">
              <text class="rate-value">{{ rate.rate }}</text>
              <text class="rate-label">rate</text>
            </view>
            <button class="sell-link" :disabled="rate.status !== 'active'" @click="goSell(rate)">Sell</button>
          </view>
        </view>

        <view v-if="!filteredRates.length" class="empty-state">
          <text class="section-title">No matching rates</text>
          <text class="muted">Try another card name or choose a different country.</text>
        </view>
      </view>

      <view class="task-row" @click="openTaskModal">
        <view>
          <text class="task-title">Daily selling task</text>
          <text class="muted">View your current reward target</text>
        </view>
        <text class="task-action">View</text>
      </view>
    </view>

    <view v-if="showTaskModal" class="modal-mask">
      <view class="reward-modal surface-card">
        <view class="reward-heading">
          <text class="eyebrow">Daily task</text>
          <text class="section-title">Selling reward</text>
        </view>
        <view class="reward-core">
          <text class="reward-value">NGN 500</text>
          <text class="reward-text">Sell at least NGN 20,000 worth of cards to claim the daily reward.</text>
          <button class="primary-button reward-button" @click="dismissTaskModal">Got it</button>
        </view>
      </view>
      <text class="close-action" @click="dismissTaskModal">Close</text>
    </view>

    <AppNav current="home" />
  </view>
</template>

<script setup lang="ts">
import { onHide, onShow } from '@dcloudio/uni-app'
import { computed, ref, watch } from 'vue'
import AppNav from '@/components/AppNav.vue'
import { useAppStore } from '@/store/app'
import { cardLogoFor, uiIcons } from '@/utils/art'

const store = useAppStore()
const TASK_MODAL_DISMISSED_KEY = 'cardbrother-task-modal-dismissed'
const showTaskModal = ref(false)
const ALL_CARD = 'All cards'
const activeCardName = ref(ALL_CARD)
const searchQuery = ref('')
let taskModalTimer: ReturnType<typeof setTimeout> | null = null

const countryRates = computed(() =>
  store.state.rates.filter((rate) => normalizeRateRegion(rate.region) === store.state.selectedCountryCode)
)

const cardCategories = computed(() => {
  const cardNames = countryRates.value.map((rate) => rate.cardName).filter(Boolean)
  return [ALL_CARD, ...Array.from(new Set(cardNames))]
})

const filteredRates = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return countryRates.value.filter((rate) => {
    const matchesCategory = activeCardName.value === ALL_CARD || rate.cardName === activeCardName.value
    const matchesQuery = !query || rate.cardName.toLowerCase().includes(query)
    return matchesCategory && matchesQuery
  })
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

function normalizeRateRegion(region: string) {
  const normalized = region.trim().replace(/^[+\uFF0B]/, '').replace(/[\s_-]+/g, '').toUpperCase()
  const regionAliases: Record<string, string> = {
    NG: 'NG',
    NIGERIA: 'NG',
    '234': 'NG',
    '\u5c3c\u65e5\u5229\u4e9a': 'NG',
    IN: 'IN',
    INDIA: 'IN',
    '91': 'IN',
    '\u5370\u5ea6': 'IN',
    CM: 'CM',
    CAMEROON: 'CM',
    '237': 'CM',
    '\u5580\u9ea6\u9686': 'CM',
    GH: 'GH',
    GHANA: 'GH',
    '233': 'GH',
    '\u52a0\u7eb3': 'GH'
  }
  return regionAliases[normalized] || normalized
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
  min-height: 100vh;
  overflow-x: hidden;
  background: #f7f7f8;
}

.home-content {
  width: 100%;
  max-width: 1040rpx;
  margin: 0 auto;
}

.top-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 22rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.brand-inline {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.brand-logo {
  width: 64rpx;
  height: 64rpx;
  flex: 0 0 auto;
}

.brand-title,
.brand-caption {
  display: block;
}

.brand-title {
  color: #111111;
  font-size: 28rpx;
  font-weight: 700;
}

.brand-caption {
  margin-top: 2rpx;
  color: #6f7178;
  font-size: 21rpx;
}

.country {
  min-width: 0;
  max-width: 48%;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 16rpx;
  border: 1rpx solid #c8c9cf;
  border-radius: 6rpx;
  background: #ffffff;
  color: #111111;
  font-size: 24rpx;
  font-weight: 700;
}

.country text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.country-arrow {
  width: 24rpx;
  height: 24rpx;
  flex: 0 0 auto;
}

.desk-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  padding: 34rpx 30rpx;
}

.intro-copy {
  min-width: 0;
}

.intro-copy .title,
.intro-copy .subtitle {
  display: block;
}

.intro-copy .title {
  margin-top: 10rpx;
}

.intro-copy .subtitle {
  max-width: 620rpx;
  margin-top: 12rpx;
}

.intro-meta {
  flex: 0 0 auto;
  min-width: 142rpx;
  padding-left: 24rpx;
  border-left: 5rpx solid var(--cb-sky-strong);
}

.rate-count,
.rate-count-label {
  display: block;
}

.rate-count {
  color: var(--cb-sky-strong);
  font-size: 42rpx;
  line-height: 1;
  font-weight: 700;
}

.rate-count-label {
  margin-top: 8rpx;
  color: #6f7178;
  font-size: 21rpx;
}

.rate-tools {
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-bottom: 0;
  border-radius: 12rpx 12rpx 0 0;
  box-shadow: 0 10rpx 30rpx rgba(34, 54, 74, 0.05);
}

.search-field {
  height: 86rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.search-mark {
  width: 24rpx;
  height: 24rpx;
  border: 3rpx solid #777980;
  border-radius: 50%;
  box-sizing: border-box;
  position: relative;
  flex: 0 0 auto;
}

.search-mark::after {
  content: "";
  position: absolute;
  width: 10rpx;
  height: 3rpx;
  right: -8rpx;
  bottom: -4rpx;
  background: #777980;
  transform: rotate(45deg);
}

.search-input {
  min-width: 0;
  flex: 1;
  height: 86rpx;
  color: #111111;
  font-size: 27rpx;
}

.clear-search {
  color: #002fa7;
  font-size: 23rpx;
  font-weight: 700;
}

.card-tabs-scroll {
  width: 100%;
  white-space: nowrap;
}

.card-tabs {
  display: flex;
  width: max-content;
  min-width: 100%;
}

.card-tab {
  position: relative;
  flex: 0 0 auto;
  min-height: 76rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  color: #6f7178;
  font-size: 25rpx;
  font-weight: 700;
}

.card-tab.active-tab {
  color: var(--cb-sky-strong);
}

.card-tab.active-tab::after {
  content: "";
  position: absolute;
  left: 24rpx;
  right: 24rpx;
  bottom: 0;
  height: 4rpx;
  background: var(--cb-sky-strong);
}

.rate-list {
  border: 1rpx solid var(--cb-line);
  border-radius: 0 0 12rpx 12rpx;
  background: #ffffff;
}

.rate-row {
  position: relative;
  min-height: 122rpx;
  padding: 18rpx 24rpx;
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.rate-row:last-child {
  border-bottom: 0;
}

.rate-row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 18rpx;
  bottom: 18rpx;
  width: 4rpx;
  background: var(--cb-sky-strong);
  opacity: 0;
}

.rate-row:active::before {
  opacity: 1;
}

.rate-left {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-width: 0;
}

.rate-icon {
  width: 58rpx;
  height: 58rpx;
  flex: 0 0 auto;
}

.rate-copy {
  min-width: 0;
}

.rate-name-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.rate-name {
  max-width: 260rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111111;
  font-size: 27rpx;
  font-weight: 700;
}

.rate-status {
  flex: 0 0 auto;
  padding: 4rpx 8rpx;
  border-radius: 4rpx;
  font-size: 17rpx;
  font-weight: 700;
}

.rate-status.active {
  color: #137a4e;
  background: #eaf6f0;
}

.rate-status.paused {
  color: #9a5b00;
  background: #fff4df;
}

.rate-updated {
  display: block;
  margin-top: 8rpx;
  color: #777980;
  font-size: 20rpx;
}

.rate-side {
  display: flex;
  align-items: center;
  gap: 18rpx;
  flex: 0 0 auto;
}

.rate-price {
  text-align: right;
}

.rate-value,
.rate-label {
  display: block;
}

.rate-value {
  color: #111111;
  font-size: 29rpx;
  font-weight: 700;
}

.rate-label {
  margin-top: 4rpx;
  color: #777980;
  font-size: 18rpx;
  text-transform: uppercase;
}

.sell-link {
  min-width: 98rpx;
  height: 58rpx;
  padding: 0 16rpx;
  border-radius: 5rpx;
  border: 0;
  background: var(--cb-sky-strong);
  color: #ffffff;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 58rpx;
}

.sell-link::after {
  border: 0;
}

.sell-link[disabled] {
  background: #efeff1;
  color: #9a9ca3;
}

.empty-state {
  padding: 58rpx 28rpx;
  text-align: center;
}

.empty-state .muted {
  display: block;
  margin-top: 10rpx;
}

.task-row {
  margin-top: 24rpx;
  min-height: 94rpx;
  padding: 18rpx 22rpx 18rpx 26rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  border: 1rpx solid #f0daa4;
  border-left: 5rpx solid var(--cb-amber-strong);
  border-radius: 12rpx;
  background: var(--cb-amber);
}

.task-title {
  display: block;
  margin-bottom: 3rpx;
  color: #111111;
  font-size: 25rpx;
  font-weight: 700;
}

.task-action {
  color: #8a5b00;
  font-size: 24rpx;
  font-weight: 700;
}

.modal-mask {
  position: fixed;
  inset: 0;
  padding: 28rpx;
  box-sizing: border-box;
  background: rgba(17, 17, 17, 0.62);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  z-index: 40;
}

.reward-modal {
  width: min(100%, 560rpx);
  overflow: hidden;
}

.reward-heading {
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.reward-heading .eyebrow,
.reward-heading .section-title {
  display: block;
}

.reward-heading .section-title {
  margin-top: 8rpx;
}

.reward-core {
  padding: 34rpx 28rpx 28rpx;
}

.reward-value {
  display: block;
  color: #002fa7;
  font-size: 52rpx;
  font-weight: 700;
}

.reward-text {
  display: block;
  margin-top: 16rpx;
  color: #6f7178;
  font-size: 26rpx;
  line-height: 1.5;
}

.reward-button {
  width: 100%;
  margin-top: 28rpx;
}

.close-action {
  color: #ffffff;
  font-size: 25rpx;
  font-weight: 700;
}

@media (max-width: 420px) {
  .desk-intro {
    align-items: flex-start;
    flex-direction: column;
  }

  .intro-meta {
    display: flex;
    align-items: baseline;
    gap: 10rpx;
  }

  .rate-row {
    align-items: flex-start;
  }

  .rate-side {
    flex-direction: column;
    align-items: flex-end;
    gap: 10rpx;
  }
}

@media (min-width: 768px) {
  .home-content {
    max-width: 960px;
  }

  .rate-row {
    min-height: 104rpx;
  }

  .rate-name {
    max-width: 440rpx;
  }
}
</style>
