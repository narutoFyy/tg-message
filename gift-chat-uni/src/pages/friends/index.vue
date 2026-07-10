<template>
  <view class="page-shell soft-page redirect-page">
    <view class="redirect-panel tone-market">
      <text class="eyebrow">Support Only</text>
      <text class="title">Opening support</text>
      <text class="subtitle">User conversations are handled by your dedicated support agent.</text>
      <button class="primary-button support-button" @click="goSupport">Open support</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'

onShow(() => {
  setTimeout(() => {
    uni.redirectTo({ url: '/pages/support/index' })
  }, 150)
})

function goSupport() {
  uni.redirectTo({ url: '/pages/support/index' })
}
</script>

<style scoped lang="scss">
.redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
}

.redirect-panel {
  width: 100%;
  max-width: 620rpx;
  margin: 0 auto;
  padding: 28rpx;
  border-left: 5rpx solid var(--cb-sky-strong);
  text-align: center;
}

.redirect-panel .eyebrow,
.redirect-panel .title,
.redirect-panel .subtitle {
  display: block;
  text-align: left;
}

.redirect-panel .title { margin-top: 8rpx; }
.redirect-panel .subtitle { margin-top: 12rpx; }
.support-button { width: 100%; margin-top: 26rpx; }
</style>
