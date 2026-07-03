<template>
  <view class="page-shell soft-page games-page">
    <view class="games-hero">
      <view>
        <text class="eyebrow">Games</text>
        <text class="title">Mini game zone</text>
        <text class="subtitle">Play, earn rewards, and check back for new events.</text>
      </view>
      <view class="hero-token">
        <text>G</text>
      </view>
    </view>

    <view class="ad-slot">
      <text class="ad-label">Sponsored</text>
      <text class="ad-title">Campaign banner</text>
      <text class="ad-copy">This space is ready for a game ad, event banner, or partner promotion.</text>
    </view>

    <view class="game-grid">
      <view v-for="item in gameCards" :key="item.title" class="game-card">
        <view :class="['game-mark', item.tone]">
          <text>{{ item.initial }}</text>
        </view>
        <text class="game-title">{{ item.title }}</text>
        <text class="game-meta">{{ item.meta }}</text>
        <button class="ghost-button mini-button" disabled>{{ item.action }}</button>
      </view>
    </view>

    <AppNav current="games" />
  </view>
</template>

<script setup lang="ts">
import AppNav from '@/components/AppNav.vue'

const gameCards = [
  { title: 'Lucky Spin', meta: 'Reward draw', action: 'Soon', initial: 'S', tone: 'mint' },
  { title: 'Rate Quiz', meta: 'Daily challenge', action: 'Soon', initial: 'Q', tone: 'blue' },
  { title: 'Gift Rush', meta: 'Event game', action: 'Soon', initial: 'R', tone: 'gold' }
]
</script>

<style scoped lang="scss">
.games-page {
  padding-bottom: 190rpx;
}

.games-hero {
  min-height: 210rpx;
  border-radius: 14rpx;
  padding: 30rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.28)),
    linear-gradient(135deg, #dff6ff 0%, #ddffe9 58%, #fff2c7 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  overflow: hidden;
}

.games-hero .title,
.games-hero .subtitle {
  display: block;
}

.hero-token {
  width: 122rpx;
  height: 122rpx;
  border-radius: 50%;
  background: #15d66f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 58rpx;
  font-weight: 900;
  box-shadow: 0 18rpx 34rpx rgba(21, 214, 111, 0.24);
}

.ad-slot {
  margin-top: 24rpx;
  min-height: 180rpx;
  border-radius: 12rpx;
  padding: 28rpx;
  background: #101820;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.ad-label {
  width: fit-content;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
  font-weight: 800;
}

.ad-title {
  margin-top: 18rpx;
  font-size: 38rpx;
  font-weight: 900;
}

.ad-copy {
  margin-top: 8rpx;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.45;
}

.game-grid {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.game-card {
  min-width: 0;
  border-radius: 12rpx;
  padding: 20rpx;
  background: #ffffff;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
  box-shadow: 0 10rpx 26rpx rgba(25, 42, 62, 0.06);
}

.game-mark {
  width: 62rpx;
  height: 62rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
}

.game-mark.mint {
  color: #0b8c4d;
  background: #e9fff2;
}

.game-mark.blue {
  color: #0875b8;
  background: #e7f6ff;
}

.game-mark.gold {
  color: #9b6500;
  background: #fff5d8;
}

.game-title {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  font-weight: 900;
  color: #171717;
  line-height: 1.18;
}

.game-meta {
  display: block;
  margin-top: 8rpx;
  min-height: 42rpx;
  font-size: 22rpx;
  color: #7d8b97;
}

.mini-button {
  margin-top: 16rpx;
  min-width: 0;
  width: 100%;
  padding: 12rpx 8rpx;
  font-size: 22rpx;
}

@media (max-width: 420px) {
  .game-grid {
    grid-template-columns: 1fr;
  }

  .game-card {
    display: grid;
    grid-template-columns: 72rpx 1fr 128rpx;
    align-items: center;
    column-gap: 16rpx;
  }

  .game-title,
  .game-meta,
  .mini-button {
    margin-top: 0;
  }
}
</style>
