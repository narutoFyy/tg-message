<template>
  <view class="tabbar">
    <view :class="['tab-item', 'nav-home', current === 'home' && 'active']" @click="go('/pages/home/index')">
      <image class="tab-icon-image" :src="current === 'home' ? navIcons.home.active : navIcons.home.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'home' && 'active']">Home</text>
    </view>
    <view :class="['tab-item', 'nav-chat', current === 'chat' && 'active']" @click="go('/pages/support/index')">
      <view class="tab-icon-wrap">
        <image class="tab-icon-image" :src="current === 'chat' ? navIcons.chat.active : navIcons.chat.inactive" mode="aspectFit" />
        <view v-if="store.state.supportUnreadCount > 0" class="tab-unread-badge" aria-label="Unread support messages" />
      </view>
      <text :class="['tab-label', current === 'chat' && 'active']">Chat</text>
    </view>
    <view :class="['tab-item', 'nav-games', current === 'games' && 'active']" @click="go('/pages/games/index')">
      <image class="tab-icon-image" :src="current === 'games' ? navIcons.games.active : navIcons.games.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'games' && 'active']">Games</text>
    </view>
    <view :class="['tab-item', 'nav-orders', current === 'transactions' && 'active']" @click="go('/pages/transactions/index')">
      <image class="tab-icon-image" :src="current === 'transactions' ? navIcons.swap.active : navIcons.swap.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'transactions' && 'active']">Orders</text>
    </view>
    <view :class="['tab-item', 'nav-me', current === 'me' && 'active']" @click="go('/pages/me/index')">
      <image class="tab-icon-image" :src="current === 'me' ? navIcons.me.active : navIcons.me.inactive" mode="aspectFit" />
      <text :class="['tab-label', current === 'me' && 'active']">Me</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useAppStore } from '@/store/app'
import { getStoredSessionUser } from '@/utils/api'
import { navIcons } from '@/utils/art'
import { setAppUnreadBadge } from '@/utils/messageNotifications'
import { safeRouteForRole } from '@/utils/routeGuard'

defineProps<{
  current?: 'home' | 'chat' | 'games' | 'transactions' | 'me'
}>()

const store = useAppStore()

watch(
  () => store.state.supportUnreadCount,
  (count) => setAppUnreadBadge(count),
  { immediate: true }
)

function go(url: string) {
  uni.redirectTo({ url: safeRouteForRole(url, getStoredSessionUser()) })
}
</script>

<style scoped lang="scss">
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(118rpx + constant(safe-area-inset-bottom));
  height: calc(118rpx + env(safe-area-inset-bottom));
  padding: 0 0 constant(safe-area-inset-bottom);
  padding: 0 0 env(safe-area-inset-bottom);
  box-sizing: border-box;
  display: flex;
  align-items: stretch;
  z-index: 30;
  background: #ffffff;
  border-top: 1rpx solid #dedfe3;
}

.tab-item {
  position: relative;
  flex: 1 1 20%;
  width: 20%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7rpx;
  padding-top: 15rpx;
  box-sizing: border-box;
}

.tab-item.active::before {
  content: "";
  position: absolute;
  top: -1rpx;
  left: 18%;
  right: 18%;
  height: 4rpx;
  background: #2f80ed;
}

.nav-chat.active::before { background: #20b86a; }
.nav-games.active::before { background: #e3a72f; }
.nav-orders.active::before { background: #df6658; }
.nav-me.active::before { background: #6f5bd3; }

.tab-icon-image {
  width: 42rpx;
  height: 42rpx;
}

.tab-icon-wrap {
  position: relative;
  width: 42rpx;
  height: 42rpx;
}

.tab-unread-badge {
  position: absolute;
  top: -13rpx;
  right: -20rpx;
  width: 18rpx;
  height: 18rpx;
  box-sizing: border-box;
  border: 3rpx solid #ffffff;
  border-radius: 50%;
  background: #e5484d;
}

.tab-label {
  font-size: 24rpx;
  color: #777980;
  font-weight: 600;
}

.nav-home .tab-label.active { color: #2f80ed; }
.nav-chat .tab-label.active { color: #168653; }
.nav-games .tab-label.active { color: #9a6500; }
.nav-orders .tab-label.active { color: #bd3f35; }
.nav-me .tab-label.active { color: #6f5bd3; }

.tab-item:active .tab-label { color: #17212b; }

@media (max-width: 380px) {
  .tabbar {
    height: calc(112rpx + constant(safe-area-inset-bottom));
    height: calc(112rpx + env(safe-area-inset-bottom));
  }

  .tab-icon-image {
    width: 38rpx;
    height: 38rpx;
  }

  .tab-label {
    font-size: 22rpx;
  }
}
</style>
