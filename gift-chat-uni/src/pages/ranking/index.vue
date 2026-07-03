<template>
  <view class="page-shell ranking-page">
    <view class="ranking-top">
      <button class="back-button" @click="goBack">&lt;</button>
      <view class="mode-tabs">
        <button :class="['mode-tab', mode === 'sales' && 'active-mode']" @click="changeMode('sales')">Sales</button>
        <button :class="['mode-tab', mode === 'invitation' && 'active-mode']" @click="changeMode('invitation')">Invitation</button>
      </view>
      <button class="help" @click="showRules">?</button>
    </view>

    <view class="month-row">
      <view>
        <text class="month-label">{{ board?.month || currentMonthLabel }}</text>
        <text class="board-subtitle">{{ modeLabel }} leaderboard</text>
      </view>
      <text class="month-pill">Month</text>
    </view>

    <view v-if="loading" class="state-card">
      <view class="loading-dot"></view>
      <text>Loading ranking...</text>
    </view>

    <view v-else-if="notice" class="state-card">
      <text>{{ notice }}</text>
      <button class="retry-button" @click="loadRanking">Retry</button>
    </view>

    <view v-else-if="!topThree.length && !restEntries.length" class="state-card">
      <text>No ranking data for this month.</text>
    </view>

    <template v-else>
      <view class="podium">
        <view v-for="entry in topThree" :key="entry.rank" :class="['podium-card', `rank-${entry.rank}`]">
          <view class="avatar">
            <image v-if="avatarSrc(entry)" class="avatar-img" :src="avatarSrc(entry)" mode="aspectFill" />
            <text v-else>{{ entryInitial(entry) }}</text>
          </view>
          <text class="podium-name">{{ entry.displayName }}</text>
          <text class="podium-rank">No.{{ entry.rank }}</text>
          <text class="podium-score">{{ entry.score }}</text>
          <text class="podium-reward">{{ entry.reward }}</text>
        </view>
      </view>

      <view class="rank-list">
        <view v-for="entry in restEntries" :key="`${entry.rank}-${entry.username}`" class="rank-row">
          <text class="rank-no">{{ entry.rank }}</text>
          <view class="mini-avatar">
            <image v-if="avatarSrc(entry)" class="avatar-img" :src="avatarSrc(entry)" mode="aspectFill" />
            <text v-else>{{ entryInitial(entry) }}</text>
          </view>
          <text class="rank-name">{{ entry.displayName }}</text>
          <view class="rank-side">
            <text>{{ entry.score }}</text>
            <text class="rank-reward">Reward: {{ entry.reward }}</text>
          </view>
        </view>
      </view>
    </template>

    <view v-if="board?.currentUser" class="my-rank">
      <text class="my-no">{{ myRankLabel }}</text>
      <view class="mini-avatar big">
        <image v-if="avatarSrc(board.currentUser)" class="avatar-img" :src="avatarSrc(board.currentUser)" mode="aspectFill" />
        <text v-else>{{ entryInitial(board.currentUser) }}</text>
      </view>
      <view class="my-copy">
        <text class="rank-name">{{ board.currentUser.displayName }}</text>
        <text class="rank-reward">{{ board.currentUser.score }}</text>
      </view>
      <button class="reward-button" @click="showRules">Rewards</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import type { RankingEntry } from '@/types'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const store = useAppStore()
const mode = ref<'sales' | 'invitation'>('sales')
const notice = ref('')
const loading = ref(false)

const board = computed(() => store.state.ranking)
const topThree = computed(() => (board.value?.leaders || []).slice(0, 3))
const restEntries = computed(() => (board.value?.leaders || []).slice(3, 12))
const modeLabel = computed(() => (mode.value === 'sales' ? 'Sales' : 'Invitation'))
const myRankLabel = computed(() => {
  const rank = board.value?.currentUser?.rank || 0
  return rank >= 500 ? '500+' : `${rank}`
})
const currentMonthLabel = computed(() =>
  new Date().toLocaleDateString('en-US', {
    month: 'short',
    year: 'numeric'
  })
)

onShow(loadRanking)

async function loadRanking() {
  notice.value = ''
  loading.value = true
  try {
    await store.refreshRanking(mode.value)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Ranking failed'
  } finally {
    loading.value = false
  }
}

function changeMode(next: 'sales' | 'invitation') {
  if (mode.value === next) return
  mode.value = next
  loadRanking()
}

function avatarSrc(entry: RankingEntry) {
  return entry.avatarUrl ? resolveMediaUrl(entry.avatarUrl) : ''
}

function entryInitial(entry: RankingEntry) {
  return (entry.displayName || entry.username || '?').slice(0, 1).toUpperCase()
}

function showRules() {
  uni.showToast({
    title: mode.value === 'sales' ? 'Ranked by completed sales' : 'Ranked by invites',
    icon: 'none'
  })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
.ranking-page {
  min-height: 100vh;
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
  background:
    radial-gradient(circle at 18% 8%, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0) 26%),
    linear-gradient(180deg, #8ce6ab 0%, #effaf4 39%, #ffffff 66%);
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

.back-button,
.help {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 62rpx;
  font-size: 34rpx;
  font-weight: 900;
}

.back-button {
  background: rgba(255, 255, 255, 0.7);
  color: #1f2d35;
}

.help {
  background: #0fcf6a;
  color: #ffffff;
}

.mode-tabs {
  display: flex;
  width: 370rpx;
  height: 76rpx;
  padding: 4rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.5);
  border: 2rpx solid rgba(255, 255, 255, 0.86);
}

.mode-tab {
  flex: 1;
  border-radius: 999rpx;
  background: transparent;
  color: #183024;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 68rpx;
}

.active-mode {
  background: #0fcf6a;
  color: #ffffff;
}

.month-row {
  margin-top: 52rpx;
  justify-content: space-between;
}

.month-label,
.board-subtitle {
  display: block;
}

.month-label {
  font-size: 36rpx;
  font-weight: 900;
  color: #182a32;
}

.board-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  font-weight: 800;
  color: #5f716a;
}

.month-pill {
  min-width: 160rpx;
  height: 64rpx;
  border-radius: 8rpx;
  background: #0fcf6a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 900;
}

.state-card {
  margin-top: 70rpx;
  min-height: 210rpx;
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.78);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  color: #52615b;
  font-size: 28rpx;
  font-weight: 800;
}

.loading-dot {
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  border: 6rpx solid rgba(15, 207, 106, 0.25);
  border-top-color: #0fcf6a;
}

.retry-button {
  width: 180rpx;
  height: 64rpx;
  border-radius: 8rpx;
  background: #0088cc;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 64rpx;
}

.podium {
  margin-top: 62rpx;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
  gap: 10rpx;
}

.podium-card {
  min-height: 280rpx;
  border-radius: 8rpx;
  padding: 26rpx 10rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 26rpx rgba(35, 57, 46, 0.1);
  overflow: hidden;
}

.rank-1 {
  min-height: 342rpx;
  background: linear-gradient(180deg, #fff2a7, #ffc34d);
  order: 2;
}

.rank-2 {
  background: linear-gradient(180deg, #e4f4ff, #a8d8ff);
  order: 1;
}

.rank-3 {
  background: linear-gradient(180deg, #ffdcc9, #ff9d76);
  order: 3;
}

.avatar,
.mini-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #e8f3ed;
  border: 4rpx solid rgba(67, 130, 106, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
  color: #193c2d;
  overflow: hidden;
  flex: 0 0 auto;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.podium-name,
.rank-name {
  font-size: 28rpx;
  font-weight: 900;
  min-width: 0;
}

.podium-name {
  width: 100%;
  margin-top: 14rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.podium-rank {
  margin-top: 12rpx;
  font-size: 42rpx;
  font-weight: 900;
}

.podium-score,
.podium-reward,
.rank-reward {
  margin-top: 8rpx;
  color: #596660;
  font-size: 24rpx;
  font-weight: 800;
}

.podium-score,
.podium-reward {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-list {
  margin-top: 36rpx;
  padding-bottom: 34rpx;
}

.rank-row {
  min-height: 104rpx;
  gap: 20rpx;
  padding: 12rpx 4rpx;
  border-bottom: 1rpx solid rgba(132, 151, 143, 0.16);
}

.rank-no {
  width: 48rpx;
  font-size: 28rpx;
  font-weight: 900;
  color: #27352f;
}

.rank-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1d2d25;
}

.rank-side {
  min-width: 184rpx;
  margin-left: auto;
  text-align: right;
  display: flex;
  flex-direction: column;
  font-size: 28rpx;
  font-weight: 800;
  color: #1c3127;
}

.my-rank {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 130rpx;
  padding: 18rpx 36rpx calc(30rpx + env(safe-area-inset-bottom));
  gap: 22rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(16, 24, 40, 0.08);
}

.my-no {
  width: 76rpx;
  font-size: 34rpx;
  font-weight: 900;
  color: #0e8f55;
}

.big {
  width: 96rpx;
  height: 96rpx;
}

.my-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.reward-button {
  min-width: 190rpx;
  height: 78rpx;
  border-radius: 8rpx;
  background: #0088cc;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 78rpx;
}
</style>
