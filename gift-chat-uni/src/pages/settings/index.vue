<template>
  <view class="page-shell soft-page settings-page">
    <view class="page-stack">
      <view class="page-header settings-hero tone-social">
        <view class="page-header-copy">
          <text class="eyebrow">Preferences</text>
          <text class="title">Settings</text>
          <text class="subtitle">Account details and support tools.</text>
        </view>
      </view>

      <view class="settings-group account-group">
        <view class="group-head"><text class="section-title">Account</text></view>
        <view class="info-row"><text class="info-label">Username</text><text class="value-copy">{{ store.state.currentUser?.username || 'Not signed in' }}</text></view>
        <view class="info-row"><text class="info-label">Email</text><text class="value-copy">{{ store.state.currentUser?.email || 'Not set' }}</text></view>
        <view class="info-row"><text class="info-label">Phone</text><text class="value-copy">{{ store.state.currentUser?.phone || 'Not set' }}</text></view>
        <view class="info-row"><text class="info-label">Role</text><text class="value-copy">{{ store.state.currentUser?.roleCode || 'Not signed in' }}</text></view>
      </view>

      <view class="settings-group action-group">
        <view class="action-row" @click="copyInvite"><text>Copy invite code</text><text class="action-value">{{ inviteCode }}</text></view>
        <view class="action-row" @click="goSupport"><text>Support</text><text class="action-value">Open chat</text></view>
        <view v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="action-row" @click="goAdminRates"><text>Rate admin</text><text class="action-value">Open</text></view>
        <view v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="action-row" @click="goAdminConsole"><text>Admin console</text><text class="action-value">Open</text></view>
      </view>

      <button class="logout-button" @click="logout">Log out</button>
      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const notice = ref('')
const inviteCode = computed(() => store.state.currentUser?.inviteCode || 'Unavailable')

function copyInvite() {
  if (!store.state.currentUser?.inviteCode) { notice.value = 'Invite code is not available.'; return }
  uni.setClipboardData({ data: `Invite code: ${inviteCode.value}`, success() { notice.value = 'Invite code copied.' } })
}
function goSupport() { uni.redirectTo({ url: '/pages/support/index' }) }
function goAdminRates() { uni.navigateTo({ url: '/pages/admin-rates/index' }) }
function goAdminConsole() { uni.navigateTo({ url: '/pages/admin-console/index' }) }
async function logout() { await store.logout(); uni.redirectTo({ url: '/pages/login/index' }) }
</script>

<style scoped lang="scss">
.settings-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.settings-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.settings-group { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.account-group { background: var(--cb-sky); border-color: #cfe4fb; }
.action-group { background: var(--cb-mint); border-color: #c6ead8; }
.group-head { min-height: 72rpx; padding: 0 24rpx; display: flex; align-items: center; border-bottom: 1rpx solid #dedfe3; }
.info-row, .action-row { min-height: 76rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 22rpx; border-bottom: 1rpx solid #dedfe3; }
.info-row:last-child, .action-row:last-child { border-bottom: 0; }
.info-label { flex: 0 0 auto; color: #6f7178; font-size: 24rpx; }
.value-copy { min-width: 0; color: #111111; font-size: 25rpx; font-weight: 700; text-align: right; overflow-wrap: anywhere; }
.action-row { color: #111111; font-size: 25rpx; font-weight: 700; }
.action-row:active { border-left: 5rpx solid var(--cb-mint-strong); }
.action-value { max-width: 50%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #0f7f49; font-size: 22rpx; }
.logout-button { width: 100%; min-height: 82rpx; border: 1rpx solid #dedfe3; border-radius: 5rpx; background: #ffffff; color: #b42318; font-size: 26rpx; font-weight: 700; }
.logout-button::after { border: 0; }
.notice-text { display: block; color: #6f7178; font-size: 23rpx; text-align: center; }
</style>
