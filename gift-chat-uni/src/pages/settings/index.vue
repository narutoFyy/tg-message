<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">Settings</text>
        <view style="height: 12rpx"></view>
        <text class="title">Account and app tools</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">This page backs the top-right settings action on Me.</text>
      </view>

      <view class="panel">
        <text class="section-title">Account</text>
        <view style="height: 18rpx"></view>
        <view class="info-row">
          <text class="muted">Username</text>
          <text class="value-copy">{{ store.state.currentUser?.username || 'Not signed in' }}</text>
        </view>
        <view class="info-row">
          <text class="muted">Email</text>
          <text class="value-copy">{{ store.state.currentUser?.email || 'Not set' }}</text>
        </view>
        <view class="info-row">
          <text class="muted">Phone</text>
          <text class="value-copy">{{ store.state.currentUser?.phone || 'Not set' }}</text>
        </view>
        <view class="info-row">
          <text class="muted">Role</text>
          <text class="value-copy">{{ store.state.currentUser?.roleCode || 'Not signed in' }}</text>
        </view>
      </view>

      <view class="panel actions-panel">
        <button class="ghost-button" @click="copyInvite">Copy Invite</button>
        <button class="ghost-button" @click="goSupport">Open Support</button>
        <button v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="ghost-button" @click="goAdminRates">Rate Admin</button>
        <button v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="ghost-button" @click="goAdminConsole">Admin Console</button>
        <button class="primary-button danger-button" @click="logout">Log Out</button>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const notice = ref('')

const inviteCode = computed(() => {
  const base = (store.state.currentUser?.username || 'dhceqw').replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
  return (base + 'QW').slice(0, 6)
})

function copyInvite() {
  uni.setClipboardData({
    data: `Invite code: ${inviteCode.value}`,
    success() {
      notice.value = 'Invite code copied.'
    }
  })
}

function goSupport() {
  uni.redirectTo({ url: '/pages/support/index' })
}

function goAdminRates() {
  uni.navigateTo({ url: '/pages/admin-rates/index' })
}

function goAdminConsole() {
  uni.navigateTo({ url: '/pages/admin-console/index' })
}

async function logout() {
  await store.logout()
  uni.redirectTo({ url: '/pages/login/index' })
}
</script>

<style scoped lang="scss">
.info-row {
  padding: 14rpx 0;
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.value-copy {
  font-size: 28rpx;
  font-weight: 700;
  color: #181818;
  text-align: right;
}

.actions-panel {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.danger-button {
  background: linear-gradient(180deg, #ff8070, #ef5945);
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
