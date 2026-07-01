<template>
  <view class="tabbar surface-card">
    <view class="tab-item" @click="go('/pages/home/index')">
      <image class="tab-icon-image" :src="current === 'home' ? navIcons.home.active : navIcons.home.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'home' && 'active']">Home</text>
    </view>
    <view class="tab-item" @click="go('/pages/support/index')">
      <image class="tab-icon-image" :src="current === 'chat' ? navIcons.chat.active : navIcons.chat.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'chat' && 'active']">Chat</text>
    </view>
    <view class="tab-item" @click="go('/pages/transactions/index')">
      <image class="tab-icon-image" :src="current === 'transactions' ? navIcons.swap.active : navIcons.swap.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'transactions' && 'active']">Orders</text>
    </view>
    <view class="tab-item" @click="go('/pages/me/index')">
      <image class="tab-icon-image" :src="current === 'me' ? navIcons.me.active : navIcons.me.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'me' && 'active']">Me</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { getStoredSessionUser } from '@/utils/api'
import { navIcons } from '@/utils/art'
import { safeRouteForRole } from '@/utils/routeGuard'

defineProps<{
  current?: 'home' | 'chat' | 'transactions' | 'me'
}>()

function go(url: string) {
  uni.redirectTo({ url: safeRouteForRole(url, getStoredSessionUser()) })
}
</script>

<style scoped lang="scss">
.tabbar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 20rpx;
  height: 112rpx;
  padding: 12rpx 10rpx 14rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  z-index: 30;
  backdrop-filter: blur(18rpx);
}

.tab-item {
  width: 140rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.tab-icon-image {
  width: 42rpx;
  height: 42rpx;
}

.tab-label {
  font-size: 24rpx;
  color: #7d8b97;
  font-weight: 700;
}

.tab-label.active,
.tab-item:active .tab-label {
  color: #0088cc;
}
</style>
