<template>
  <view class="page-shell soft-page">
    <view class="login-top">
      <view class="status-row">
        <text class="clock">05:47</text>
        <text class="battery">30%</text>
      </view>

      <view class="hero">
        <view class="brand-row">
          <image class="brand-logo" src="/static/cardbrother-logo.png" mode="aspectFit" />
          <view>
            <text class="hello">Hello!</text>
            <text class="brand-name">CardBrother</text>
          </view>
        </view>
        <view class="hero-visual"></view>
      </view>
    </view>

    <view class="auth-card surface-card">
      <view class="tab-strip">
        <view class="tab active">
          <text>Sign in</text>
          <view class="tab-active-line"></view>
        </view>
        <view class="tab muted-tab" @click="goRegister">
          <text>Sign up</text>
        </view>
      </view>

      <view class="form-body">
        <text class="field-label">Country</text>
        <view class="field-input picker-field" @click="pickCountry">
          <text>{{ store.selectedCountry().name }}</text>
          <text class="picker-arrow">▾</text>
        </view>
        <view style="height: 24rpx"></view>

        <text class="field-label">Email or Phone</text>
        <input v-model.trim="form.identifier" class="field-input" placeholder="Email or Phone" />
        <view style="height: 24rpx"></view>

        <text class="field-label">Password</text>
        <input v-model="form.password" class="field-input" password placeholder="Password" />

        <view class="helper-row">
          <text class="helper-link">Verification Code Login</text>
          <text class="helper-link">Forgot password?</text>
        </view>

        <button class="primary-button" @click="handleSubmit">Sign In</button>
        <text class="or-text">or</text>
        <text v-if="notice" class="muted">{{ notice }}</text>

        <view class="policy">
          <view class="policy-box">✓</view>
          <text class="muted">By continuing, you agree to CardBrother Term of Use and confirm that you have read Privacy Policy.</text>
        </view>
      </view>
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
      notice.value = 'Email or phone is required.'
      return
    }
    if (!password) {
      notice.value = 'Password is required.'
      return
    }
    const session = await store.login(identifier, password)
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/support/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Login failed'
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}

function pickCountry() {
  store.chooseCountry()
}
</script>

<style scoped lang="scss">
.login-top {
  padding: 8rpx 8rpx 24rpx;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 6rpx 22rpx;
  font-weight: 800;
  font-size: 28rpx;
}

.battery {
  color: #12c96b;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 300rpx;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.brand-logo {
  width: 92rpx;
  height: 92rpx;
}

.hello {
  display: block;
  font-size: 64rpx;
  font-weight: 900;
  color: #14cb6d;
  line-height: 1;
}

.brand-name {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #18b15f;
}

.hero-visual {
  width: 280rpx;
  height: 280rpx;
  border-radius: 120rpx 40rpx 120rpx 40rpx;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.5), rgba(0,0,0,0)),
    radial-gradient(circle at 38% 30%, rgba(255,255,255,0.8), transparent 20%),
    linear-gradient(145deg, #76f2b3, #12d46e 58%, #95ffd0);
  box-shadow: inset 0 0 0 10rpx rgba(255,255,255,0.2);
}

.auth-card {
  overflow: hidden;
}

.tab-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.tab {
  min-height: 118rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 66rpx;
  font-weight: 900;
}

.tab text {
  font-size: 66rpx;
  font-weight: 900;
  color: #18b765;
}

.muted-tab {
  background: linear-gradient(180deg, rgba(9, 202, 103, 0.55), rgba(9, 202, 103, 0.18));
}

.muted-tab text {
  color: rgba(16, 118, 69, 0.55);
}

.form-body {
  padding: 26rpx;
}

.picker-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-arrow {
  color: #212121;
  font-size: 34rpx;
}

.helper-row {
  display: flex;
  justify-content: space-between;
  margin: 22rpx 6rpx 28rpx;
}

.helper-link {
  font-size: 24rpx;
  font-weight: 700;
  color: #12c96b;
}

.or-text {
  display: block;
  text-align: center;
  font-size: 28rpx;
  color: #19c26b;
  padding: 20rpx 0;
  font-weight: 700;
}

.policy {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}

.policy-box {
  width: 44rpx;
  height: 44rpx;
  border-radius: 10rpx;
  background: #0ecf67;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}
</style>
