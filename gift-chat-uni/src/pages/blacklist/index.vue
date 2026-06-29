<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">Blacklist</text>
        <view style="height: 12rpx"></view>
        <text class="title">Blocked users cannot keep interacting</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">This list is currently read-only and mirrors the backend owner account.</text>
      </view>

      <view v-for="item in store.state.blacklist" :key="item.id" class="panel">
        <view class="head-row">
          <view>
            <text class="name-copy">{{ item.displayName }}</text>
            <view style="height: 8rpx"></view>
            <text class="muted">@{{ item.username }}</text>
          </view>
          <text class="status-pill paused">Blocked</text>
        </view>

        <view style="height: 16rpx"></view>
        <text class="muted">Phone: {{ item.phone }}</text>
        <view style="height: 8rpx"></view>
        <text class="muted">Blocked at: {{ item.blockedAt || 'Unknown' }}</text>
        <view style="height: 8rpx"></view>
        <text class="muted">Restrictions: no direct messages, no friend actions, and no further trade contact.</text>
        <view style="height: 16rpx"></view>
        <button class="ghost-button mini-button" @click="unblock(item.id)">Unblock</button>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const notice = ref('')

onShow(() => {
  store.bootstrap()
})

async function unblock(blacklistId: string) {
  try {
    await store.unblockUser(blacklistId)
    notice.value = 'User removed from blacklist.'
    await store.bootstrap()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Unblock failed'
  }
}
</script>

<style scoped lang="scss">
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}

.name-copy {
  font-size: 30rpx;
  font-weight: 800;
  color: #171717;
}

.mini-button {
  min-width: 160rpx;
  padding-top: 14rpx;
  padding-bottom: 14rpx;
  font-size: 24rpx;
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
