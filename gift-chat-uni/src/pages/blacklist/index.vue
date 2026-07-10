<template>
  <view class="page-shell soft-page blacklist-page">
    <view class="page-stack">
      <view class="page-header blacklist-hero tone-risk">
        <view class="page-header-copy">
          <text class="eyebrow">Privacy</text>
          <text class="title">Blacklist</text>
          <text class="subtitle">Blocked accounts cannot contact you or start new trade interactions.</text>
        </view>
        <text class="blocked-count">{{ store.state.blacklist.length }}</text>
      </view>

      <view v-if="store.state.blacklist.length" class="blacklist-section">
        <view v-for="item in store.state.blacklist" :key="item.id" class="blocked-row">
          <view class="blocked-copy">
            <view class="name-line"><text class="name-copy">{{ item.displayName }}</text><text class="status-pill paused">Blocked</text></view>
            <text class="row-meta">@{{ item.username }} / {{ item.phone }}</text>
            <text class="row-meta">Blocked {{ item.blockedAt || 'Unknown date' }}</text>
          </view>
          <button class="unblock-button" @click="unblock(item.id)">Unblock</button>
        </view>
      </view>
      <view v-else class="empty-state"><text class="section-title">No blocked accounts</text><text class="muted">Accounts you block will appear here.</text></view>
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
onShow(() => { store.bootstrap() })
async function unblock(blacklistId: string) {
  try { await store.unblockUser(blacklistId); notice.value = 'User removed from blacklist.'; await store.bootstrap() }
  catch (error) { notice.value = error instanceof Error ? error.message : 'Unblock failed' }
}
</script>

<style scoped lang="scss">
.blacklist-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.blacklist-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.blocked-count { color: var(--cb-coral-strong); font-size: 40rpx; font-weight: 700; }
.blacklist-section { background: var(--cb-coral); border: 1rpx solid #f1c8c2; border-radius: 12rpx; overflow: hidden; }
.blocked-row { min-height: 112rpx; padding: 20rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid #f1c8c2; background: rgba(255,255,255,0.62); }
.blocked-row:last-child { border-bottom: 0; }
.blocked-copy { min-width: 0; }
.name-line { display: flex; align-items: center; gap: 10rpx; }
.name-copy { color: #111111; font-size: 27rpx; font-weight: 700; }
.row-meta { display: block; margin-top: 6rpx; color: #6f7178; font-size: 21rpx; }
.unblock-button { flex: 0 0 auto; min-width: 128rpx; height: 58rpx; padding: 0 14rpx; border: 1rpx solid #c8c9cf; border-radius: 5rpx; background: #ffffff; color: #b42318; font-size: 22rpx; font-weight: 700; line-height: 56rpx; }
.unblock-button::after { border: 0; }
.empty-state { padding: 58rpx 28rpx; background: #ffffff; border: 1rpx solid #dedfe3; text-align: center; }
.empty-state .muted { display: block; margin-top: 9rpx; }
.notice-text { display: block; color: #6f7178; font-size: 23rpx; text-align: center; }
</style>
