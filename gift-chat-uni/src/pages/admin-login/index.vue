<template>
  <view class="page-shell soft-page admin-login-page">
    <view class="admin-login-card surface-card">
      <text class="eyebrow">管理员登录</text>
      <view style="height: 12rpx"></view>
      <text class="title">进入后台管理</text>
      <view style="height: 10rpx"></view>
      <text class="subtitle">使用管理员账号查看用户、客服、订单、聊天记录和平台配置。</text>
    </view>

    <view class="panel">
      <text class="field-label">管理员账号</text>
      <input v-model="form.identifier" class="field-input" placeholder="用户名 / 邮箱 / 手机号" />
      <view style="height: 22rpx"></view>

      <text class="field-label">密码</text>
      <input v-model="form.password" class="field-input" password placeholder="请输入密码" />
      <view style="height: 26rpx"></view>

      <button class="primary-button" @click="handleSubmit">进入管理员后台</button>
      <view style="height: 16rpx"></view>
      <button class="ghost-button" @click="goUserLogin">返回用户登录</button>

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
      notice.value = '请输入管理员账号。'
      return
    }
    if (!password) {
      notice.value = '请输入密码。'
      return
    }
    const session = await store.login(identifier, password)
    if (session.roleCode !== 'ADMIN') {
      await store.logout()
      notice.value = '需要管理员账号。'
      return
    }
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/admin-console/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '管理员登录失败'
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
