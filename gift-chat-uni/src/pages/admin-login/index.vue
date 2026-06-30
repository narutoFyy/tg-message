<template>
  <view class="page-shell soft-page admin-login-page">
    <view class="admin-login-card surface-card">
      <text class="eyebrow">Admin Login</text>
      <view style="height: 12rpx"></view>
      <text class="title">Back office access</text>
      <view style="height: 10rpx"></view>
      <text class="subtitle">Use an administrator account to manage rates, users, agents, and message records.</text>
    </view>

    <view class="panel">
      <text class="field-label">Admin account</text>
      <input v-model="form.identifier" class="field-input" placeholder="Username / Email / Phone" />
      <view style="height: 22rpx"></view>

      <text class="field-label">Password</text>
      <input v-model="form.password" class="field-input" password placeholder="Password" />
      <view style="height: 26rpx"></view>

      <button class="primary-button" @click="handleSubmit">Enter Admin</button>
      <view style="height: 16rpx"></view>
      <button class="ghost-button" @click="goUserLogin">User Login</button>

      <view style="height: 18rpx"></view>
      <text v-if="notice" class="muted">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { safeRouteForRole } from '@/utils/routeGuard'

const store = useAppStore()
const notice = ref('')

const form = reactive({
  identifier: '',
  password: ''
})

onShow(() => {
  form.identifier = ''
  form.password = ''
  notice.value = ''
})

async function handleSubmit() {
  try {
    const identifier = form.identifier.trim()
    const password = form.password
    if (!identifier) {
      notice.value = 'Admin account is required.'
      return
    }
    if (!password) {
      notice.value = 'Password is required.'
      return
    }
    const session = await store.login(identifier, password)
    if (session.roleCode !== 'ADMIN') {
      await store.logout()
      notice.value = 'Admin account required.'
      return
    }
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/admin-rates/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Admin login failed'
  }
}

function goUserLogin() {
  uni.redirectTo({ url: '/pages/login/index' })
}
</script>

<style scoped lang="scss">
.admin-login-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24rpx;
}

.admin-login-card {
  padding: 34rpx 30rpx;
}
</style>
