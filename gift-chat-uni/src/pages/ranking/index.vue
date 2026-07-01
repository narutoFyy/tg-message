<template>
  <view class="page-shell ranking-page">
    <view class="ranking-top">
      <text class="back" @click="goBack">‹</text>
      <view class="mode-tabs">
        <button :class="['mode-tab', mode === 'sales' && 'active-mode']" @click="changeMode('sales')">Sales</button>
        <button :class="['mode-tab', mode === 'invitation' && 'active-mode']" @click="changeMode('invitation')">Invitation</button>
      </view>
      <text class="help">?</text>
    </view>

    <view class="month-row">
      <text>{{ board?.month || 'Jun, 2026' }}</text>
      <text class="chevron">⌄</text>
      <text class="month-pill">Month</text>
    </view>

    <view class="podium">
      <view v-for="entry in topThree" :key="entry.rank" :class="['podium-card', `rank-${entry.rank}`]">
        <view class="avatar">{{ entry.displayName.slice(0, 1) }}</view>
        <text class="podium-name">{{ entry.displayName }}</text>
        <text class="podium-rank">{{ entry.rank }}</text>
        <text class="podium-score">{{ entry.score }}</text>
        <text class="podium-reward">{{ entry.reward }}</text>
      </view>
    </view>

    <view class="rank-list">
      <view v-for="entry in restEntries" :key="`${entry.rank}-${entry.username}`" class="rank-row">
        <text class="rank-no">{{ entry.rank }}</text>
        <view class="mini-avatar">{{ entry.displayName.slice(0, 1) }}</view>
        <text class="rank-name">{{ entry.displayName }}</text>
        <view class="rank-side">
          <text>{{ entry.score }}</text>
          <text class="rank-reward">Reward: {{ entry.reward }}</text>
        </view>
      </view>
    </view>

    <view v-if="board?.currentUser" class="my-rank">
      <text class="my-no">{{ board.currentUser.rank }}+</text>
      <view class="mini-avatar big">{{ board.currentUser.displayName.slice(0, 1) }}</view>
      <view class="my-copy">
        <text class="rank-name">{{ board.currentUser.displayName }}</text>
        <text class="rank-reward">{{ board.currentUser.score }}</text>
      </view>
      <button class="reward-button">Rewards</button>
    </view>

    <text v-if="notice" class="notice-text">{{ notice }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const mode = ref<'sales' | 'invitation'>('sales')
const notice = ref('')

const board = computed(() => store.state.ranking)
const topThree = computed(() => (board.value?.leaders || []).slice(0, 3))
const restEntries = computed(() => (board.value?.leaders || []).slice(3, 12))

onShow(loadRanking)

async function loadRanking() {
  try {
    await store.refreshRanking(mode.value)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Ranking failed'
  }
}

function changeMode(next: 'sales' | 'invitation') {
  mode.value = next
  loadRanking()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
.ranking-page {
  min-height: 100vh;
  padding-bottom: 180rpx;
  background: linear-gradient(180deg, #87efad 0%, #eafff3 35%, #ffffff 58%);
}

.ranking-top,
.month-row,
.rank-row,
.my-rank {
  display: flex;
  align-items: center;
}

.ranking-top {
  justify-content: space-between;
  padding-top: 34rpx;
}

.back {
  font-size: 66rpx;
}

.help {
  width: 58rpx;
  height: 58rpx;
  border-radius: 999rpx;
  background: #0fcf6a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 900;
}

.mode-tabs {
  display: flex;
  width: 370rpx;
  height: 76rpx;
  padding: 4rpx;
  border-radius: 999rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.86);
}

.mode-tab {
  flex: 1;
  border-radius: 999rpx;
  background: transparent;
  color: #111;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 68rpx;
}

.active-mode {
  background: #0fcf6a;
  color: #fff7cf;
}

.month-row {
  margin-top: 62rpx;
  justify-content: space-between;
  font-size: 30rpx;
  font-weight: 900;
}

.month-pill {
  margin-left: auto;
  min-width: 160rpx;
  height: 64rpx;
  border-radius: 8rpx;
  background: #0fcf6a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.podium {
  margin-top: 70rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  align-items: end;
  gap: 10rpx;
}

.podium-card {
  min-height: 280rpx;
  border-radius: 36rpx 36rpx 10rpx 10rpx;
  padding: 26rpx 10rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.rank-1 {
  min-height: 340rpx;
  background: linear-gradient(180deg, #fff1a5, #ffc44f);
  order: 2;
}

.rank-2 {
  background: linear-gradient(180deg, #dff2ff, #a9d8ff);
  order: 1;
}

.rank-3 {
  background: linear-gradient(180deg, #ffd9c4, #ff9a75);
  order: 3;
}

.avatar,
.mini-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 999rpx;
  background: #c8f5b8;
  border: 4rpx solid #43826a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
}

.podium-name,
.rank-name {
  font-size: 28rpx;
  font-weight: 900;
}

.podium-rank {
  margin-top: 12rpx;
  font-size: 50rpx;
  font-weight: 900;
}

.podium-score,
.podium-reward,
.rank-reward {
  margin-top: 8rpx;
  color: #595f67;
  font-size: 24rpx;
  font-weight: 800;
}

.rank-list {
  margin-top: 36rpx;
  padding-bottom: 150rpx;
}

.rank-row {
  min-height: 96rpx;
  gap: 24rpx;
}

.rank-no {
  width: 48rpx;
  font-size: 28rpx;
  font-weight: 900;
}

.rank-side {
  margin-left: auto;
  text-align: right;
  display: flex;
  flex-direction: column;
  font-size: 28rpx;
  font-weight: 800;
}

.my-rank {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 130rpx;
  padding: 18rpx 36rpx 30rpx;
  gap: 22rpx;
  background: #ffffff;
  box-shadow: 0 -8rpx 24rpx rgba(16, 24, 40, 0.08);
}

.my-no {
  font-size: 34rpx;
  font-weight: 900;
}

.big {
  width: 96rpx;
  height: 96rpx;
}

.my-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.reward-button {
  min-width: 210rpx;
  height: 78rpx;
  border-radius: 8rpx;
  background: #0088cc;
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.notice-text {
  display: block;
  text-align: center;
  color: #5d646d;
}
</style>
