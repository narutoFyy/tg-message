<template>
  <view class="page-shell soft-page">
    <view class="panel">
      <text class="eyebrow">Sign up</text>
      <view style="height: 12rpx"></view>
      <text class="title">Create account for CardBrother</text>
    </view>

    <view class="panel">
      <text class="field-label">Username</text>
      <input v-model.trim="form.username" class="field-input" placeholder="Unique username" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Email</text>
      <input v-model.trim="form.email" class="field-input" placeholder="Email address" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Phone</text>
      <input v-model.trim="form.phone" class="field-input" placeholder="Phone number" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Password</text>
      <input v-model="form.password" class="field-input" password placeholder="Password" />
      <view style="height: 24rpx"></view>
      <button class="primary-button" @click="handleSubmit">Create account</button>
      <view style="height: 16rpx"></view>
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
  username: '',
  email: '',
  phone: '',
  password: ''
})

onShow(() => {
  form.username = ''
  form.email = ''
  form.phone = ''
  form.password = ''
  notice.value = ''
})

async function handleSubmit() {
  try {
    const username = form.username.trim()
    const email = form.email.trim()
    const phone = form.phone.trim()
    const password = form.password

    if (!username) {
      notice.value = 'Username is required.'
      return
    }
    if (!email && !phone) {
      notice.value = 'Email or phone is required.'
      return
    }
    if (!password) {
      notice.value = 'Password is required.'
      return
    }
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      notice.value = 'Email format is invalid.'
      return
    }
    const session = await store.register({
      username,
      email: email || undefined,
      phone: phone || undefined,
      password
    })
    notice.value = 'Registration complete.'
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/support/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Registration failed'
  }
}
</script>
