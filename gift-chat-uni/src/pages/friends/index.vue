<template>
  <view class="page-shell soft-page redirect-page">
    <view class="panel redirect-panel">
      <text class="eyebrow">Support Only</text>
      <view style="height: 12rpx"></view>
      <text class="title">Users only talk to their dedicated support agent</text>
      <view style="height: 12rpx"></view>
      <text class="subtitle">This project no longer uses user-to-user friend chat. Redirecting you to support.</text>
      <view style="height: 24rpx"></view>
      <button class="primary-button" @click="goSupport">Open Support</button>
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
  text-align: center;
}
</style>
