<template>
  <view class="page-shell ranking-page">
    <view class="ranking-content">
      <view class="ranking-top tone-reward">
        <button class="icon-button" @click="goBack">&lt;</button>
        <view class="page-heading"><text class="eyebrow">{{ board?.month || currentMonthLabel }}</text><text class="title">{{ modeLabel }} ranking</text></view>
        <button class="icon-button help-button" @click="showRules">?</button>
      </view>

      <view class="mode-tabs">
        <button :class="['mode-tab', mode === 'sales' && 'active-mode']" @click="changeMode('sales')">Sales</button>
        <button :class="['mode-tab', mode === 'invitation' && 'active-mode']" @click="changeMode('invitation')">Invitations</button>
      </view>

      <view v-if="loading" class="state-card"><view class="loading-dot"></view><text>Loading ranking...</text></view>
      <view v-else-if="notice" class="state-card"><text>{{ notice }}</text><button class="primary-button retry-button" @click="loadRanking">Retry</button></view>
      <view v-else-if="!topThree.length && !restEntries.length" class="state-card"><text>No ranking data for this month.</text></view>

      <template v-else>
        <view class="podium">
          <view v-for="entry in topThree" :key="entry.rank" :class="['podium-row', entry.rank === 1 && 'first-place']">
            <text class="podium-rank">{{ entry.rank }}</text>
            <view class="avatar"><image v-if="avatarSrc(entry)" class="avatar-img" :src="avatarSrc(entry)" mode="aspectFill" /><text v-else>{{ entryInitial(entry) }}</text></view>
            <view class="podium-copy"><text class="podium-name">{{ entry.displayName }}</text><text class="podium-score">{{ entry.score }}</text></view>
            <text class="podium-reward">{{ entry.reward }}</text>
          </view>
        </view>

        <view v-if="board?.currentUser" class="my-rank">
          <view><text class="my-label">Your position</text><text class="my-number">{{ myRankLabel }}</text></view>
          <view class="my-copy"><text class="rank-name">{{ board.currentUser.displayName }}</text><text class="rank-score">{{ board.currentUser.score }}</text></view>
          <text class="rules-link" @click="showRules">Rules</text>
        </view>

        <view v-if="restEntries.length" class="rank-list">
          <view class="list-head"><text class="section-title">Leaderboard</text><text class="muted">Top 12</text></view>
          <view v-for="entry in restEntries" :key="`${entry.rank}-${entry.username}`" class="rank-row">
            <text class="rank-no">{{ entry.rank }}</text>
            <view class="mini-avatar"><image v-if="avatarSrc(entry)" class="avatar-img" :src="avatarSrc(entry)" mode="aspectFill" /><text v-else>{{ entryInitial(entry) }}</text></view>
            <text class="rank-name">{{ entry.displayName }}</text>
            <view class="rank-side"><text class="rank-score">{{ entry.score }}</text><text class="rank-reward">{{ entry.reward }}</text></view>
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import type { RankingEntry } from '@/types'
import { resolveMediaUrl } from '@/utils/mediaUrl'
const store = useAppStore(); const mode = ref<'sales' | 'invitation'>('sales'); const notice = ref(''); const loading = ref(false)
const board = computed(() => store.state.ranking); const topThree = computed(() => (board.value?.leaders || []).slice(0, 3)); const restEntries = computed(() => (board.value?.leaders || []).slice(3, 12)); const modeLabel = computed(() => mode.value === 'sales' ? 'Sales' : 'Invitation')
const myRankLabel = computed(() => { const rank = board.value?.currentUser?.rank || 0; return rank >= 500 ? '500+' : `${rank}` })
const currentMonthLabel = computed(() => new Date().toLocaleDateString('en-US', { month: 'short', year: 'numeric' }))
onShow(loadRanking)
async function loadRanking() { notice.value = ''; loading.value = true; try { await store.refreshRanking(mode.value) } catch (error) { notice.value = error instanceof Error ? error.message : 'Ranking failed' } finally { loading.value = false } }
function changeMode(next: 'sales' | 'invitation') { if (mode.value === next) return; mode.value = next; loadRanking() }
function avatarSrc(entry: RankingEntry) { return entry.avatarUrl ? resolveMediaUrl(entry.avatarUrl) : '' }
function entryInitial(entry: RankingEntry) { return (entry.displayName || entry.username || '?').slice(0, 1).toUpperCase() }
function showRules() { uni.showToast({ title: mode.value === 'sales' ? 'Ranked by completed sales' : 'Ranked by verified invites', icon: 'none' }) }
function goBack() { uni.navigateBack() }
</script>

<style scoped lang="scss">
.ranking-page { min-height: 100vh; padding-bottom: 48rpx; background: #f7f7f8; }
.ranking-content { width: 100%; max-width: 1040rpx; margin: 0 auto; }
.ranking-top { display: grid; grid-template-columns: 62rpx minmax(0, 1fr) 62rpx; align-items: center; gap: 18rpx; padding: 22rpx 24rpx; border-bottom: 0; border-radius: 12rpx; }
.page-heading { text-align: center; }
.page-heading .eyebrow, .page-heading .title { display: block; }
.page-heading .title { margin-top: 5rpx; font-size: 34rpx; }
.icon-button { width: 62rpx; height: 62rpx; padding: 0; border: 1rpx solid #c8c9cf; border-radius: 50%; background: #ffffff; color: #111111; font-size: 30rpx; font-weight: 700; line-height: 60rpx; }
.icon-button::after, .mode-tab::after { border: 0; }
.help-button { color: #002fa7; }
.mode-tabs { height: 76rpx; margin-top: 24rpx; padding: 4rpx; box-sizing: border-box; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; }
.mode-tab { height: 66rpx; border-radius: 4rpx; background: transparent; color: #6f7178; font-size: 25rpx; font-weight: 700; line-height: 66rpx; }
.mode-tab.active-mode { background: var(--cb-amber-strong); color: #382600; }
.state-card { min-height: 210rpx; margin-top: 24rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 18rpx; background: #ffffff; border: 1rpx solid #dedfe3; color: #6f7178; font-size: 25rpx; }
.loading-dot { width: 32rpx; height: 32rpx; border: 5rpx solid #dedfe3; border-top-color: #002fa7; border-radius: 50%; }
.retry-button { min-width: 180rpx; margin: 0; }
.podium { margin-top: 24rpx; background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.podium-row { position: relative; min-height: 104rpx; padding: 16rpx 22rpx; box-sizing: border-box; display: grid; grid-template-columns: 48rpx 68rpx minmax(0, 1fr) minmax(120rpx, auto); align-items: center; gap: 16rpx; border-bottom: 1rpx solid #dedfe3; }
.podium-row:last-child { border-bottom: 0; }
.podium-row:nth-child(1) { background: var(--cb-amber); }
.podium-row:nth-child(2) { background: var(--cb-sky); }
.podium-row:nth-child(3) { background: var(--cb-coral); }
.podium-row.first-place::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 5rpx; background: var(--cb-amber-strong); }
.podium-rank { color: #8a5b00; font-size: 32rpx; font-weight: 700; }
.avatar, .mini-avatar { width: 60rpx; height: 60rpx; overflow: hidden; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #eef2ff; color: #002fa7; font-size: 24rpx; font-weight: 700; flex: 0 0 auto; }
.avatar-img { width: 100%; height: 100%; }
.podium-copy { min-width: 0; }
.podium-name, .podium-score { display: block; }
.podium-name, .rank-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #111111; font-size: 25rpx; font-weight: 700; }
.podium-score, .rank-score { margin-top: 5rpx; color: #6f7178; font-size: 21rpx; }
.podium-reward { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #137a4e; font-size: 22rpx; font-weight: 700; text-align: right; }
.my-rank { min-height: 96rpx; margin-top: 20rpx; padding: 18rpx 22rpx; box-sizing: border-box; display: grid; grid-template-columns: 120rpx minmax(0, 1fr) auto; align-items: center; gap: 18rpx; background: var(--cb-mint); border: 1rpx solid #c6ead8; border-left: 5rpx solid var(--cb-mint-strong); border-radius: 12rpx; }
.my-label, .my-number { display: block; }
.my-label { color: #6f7178; font-size: 19rpx; }
.my-number { margin-top: 4rpx; color: #002fa7; font-size: 30rpx; font-weight: 700; }
.my-copy { min-width: 0; }
.my-copy .rank-name, .my-copy .rank-score { display: block; }
.rules-link { color: #002fa7; font-size: 23rpx; font-weight: 700; }
.rank-list { margin-top: 20rpx; background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.list-head { min-height: 72rpx; padding: 0 22rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #dedfe3; }
.rank-row { min-height: 88rpx; padding: 13rpx 22rpx; box-sizing: border-box; display: grid; grid-template-columns: 44rpx 60rpx minmax(0, 1fr) minmax(130rpx, auto); align-items: center; gap: 14rpx; border-bottom: 1rpx solid #dedfe3; }
.rank-row:last-child { border-bottom: 0; }
.rank-no { color: #6f7178; font-size: 24rpx; font-weight: 700; }
.rank-side { min-width: 0; text-align: right; }
.rank-reward { display: block; margin-top: 4rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #137a4e; font-size: 19rpx; }
@media (max-width: 420px) { .podium-row { grid-template-columns: 38rpx 58rpx minmax(0, 1fr); } .podium-reward { grid-column: 3; text-align: left; } .rank-row { grid-template-columns: 38rpx 52rpx minmax(0, 1fr); } .rank-side { grid-column: 3; text-align: left; } }
@media (min-width: 768px) { .ranking-content { max-width: 960px; } }
</style>
