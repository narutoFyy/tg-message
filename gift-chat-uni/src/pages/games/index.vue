<template>
  <view class="page-shell soft-page games-page">
    <view class="games-content">
      <view class="page-header games-hero tone-reward">
        <view class="page-header-copy"><text class="eyebrow">Games</text><text class="title">Reward activities</text><text class="subtitle">Available activities linked to your CardBrother account.</text></view>
      </view>
      <view class="activity-row" @click="handleGameCard(gameCard)">
        <image class="activity-image" src="/static/lottery/stone-technology.png" mode="aspectFit" />
        <view class="activity-copy"><text class="activity-title">Lucky Wheel</text><text class="activity-meta">Use an eligible draw chance to win a listed prize.</text><text class="activity-status">Available activity</text></view>
        <button class="primary-button play-button">Play</button>
      </view>
      <view class="activity-note"><text class="section-title">Activity availability</text><text class="muted">Eligibility and prize results are determined by your account and the server at draw time.</text></view>
    </view>
    <AppNav current="games" />
  </view>
</template>

<script setup lang="ts">
import AppNav from '@/components/AppNav.vue'
const gameCard = { route: '/pages/lucky-wheel/index' }
function handleGameCard(item: typeof gameCard) { if (item.route) uni.navigateTo({ url: item.route }) }
</script>

<style scoped lang="scss">
.games-page { padding-bottom: calc(154rpx + env(safe-area-inset-bottom)); }
.games-content { width: 100%; max-width: 1040rpx; margin: 0 auto; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.games-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.activity-row { min-height: 168rpx; margin-top: 28rpx; padding: 22rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; gap: 22rpx; background: var(--cb-amber); border: 1rpx solid #f0daa4; border-left: 5rpx solid var(--cb-amber-strong); border-radius: 12rpx; }
.activity-image { width: 112rpx; height: 112rpx; flex: 0 0 auto; background: #f7f7f8; }
.activity-copy { min-width: 0; flex: 1; }
.activity-title, .activity-meta, .activity-status { display: block; }
.activity-title { color: #111111; font-size: 31rpx; font-weight: 700; }
.activity-meta { margin-top: 8rpx; color: #6f7178; font-size: 23rpx; line-height: 1.4; }
.activity-status { margin-top: 9rpx; color: #137a4e; font-size: 21rpx; font-weight: 700; }
.play-button { min-width: 138rpx; margin: 0; background: var(--cb-amber-strong); color: #382600; }
.activity-note { margin-top: 24rpx; padding: 22rpx 24rpx; background: var(--cb-lilac); border: 1rpx solid #ddd5fb; border-left: 5rpx solid var(--cb-lilac-strong); border-radius: 12rpx; }
.activity-note .muted { display: block; margin-top: 8rpx; }
@media (max-width: 420px) { .activity-row { align-items: flex-start; flex-wrap: wrap; } .activity-copy { width: calc(100% - 138rpx); } .play-button { width: 100%; } }
@media (min-width: 768px) { .games-content { max-width: 960px; } }
</style>
