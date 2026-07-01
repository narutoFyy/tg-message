<template>
  <view class="page-shell login-page">
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
          <text class="picker-arrow">v</text>
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
          <view class="policy-box">OK</view>
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
  position: relative;
  z-index: 1;
  width: min(100%, 520px);
  margin: 0 auto;
  padding: 8rpx 8rpx 24rpx;
}

.login-page {
  position: relative;
  min-height: 100vh;
  box-sizing: border-box;
  background-image:
    linear-gradient(180deg, rgba(5, 14, 23, 0.08), rgba(5, 14, 23, 0.36)),
    url('/static/login-bg.png');
  background-size: cover;
  background-position: center;
  overflow: hidden;
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
  color: rgba(255, 255, 255, 0.92);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 214rpx;
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
  font-size: 52rpx;
  font-weight: 900;
  color: #ffffff;
  line-height: 1;
  text-shadow: 0 4rpx 18rpx rgba(0, 0, 0, 0.26);
}

.brand-name {
  display: block;
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.82);
}

.hero-visual {
  width: 156rpx;
  height: 116rpx;
  border-radius: 16rpx;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.16) 1rpx, transparent 1rpx),
    linear-gradient(180deg, rgba(255, 255, 255, 0.16) 1rpx, transparent 1rpx),
    rgba(255, 255, 255, 0.1);
  background-size: 34rpx 34rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 16rpx 34rpx rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(12rpx);
}

.auth-card {
  position: relative;
  z-index: 1;
  width: min(100%, 520px);
  margin: 0 auto;
  overflow: hidden;
  background: rgba(8, 19, 30, 0.72);
  border-color: rgba(255, 255, 255, 0.16);
  box-shadow: 0 28rpx 76rpx rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(24rpx);
}

.tab-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.tab {
  min-height: 92rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 30rpx;
  font-weight: 900;
}

.tab text {
  font-size: 34rpx;
  font-weight: 900;
  color: #ffffff;
}

.muted-tab {
  background: rgba(255, 255, 255, 0.08);
}

.muted-tab text {
  color: rgba(255, 255, 255, 0.56);
}

.form-body {
  padding: 26rpx;
}

.field-label {
  color: rgba(255, 255, 255, 0.74);
}

.field-input {
  background: rgba(255, 255, 255, 0.94);
  border-color: rgba(255, 255, 255, 0.28);
}

.picker-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-arrow {
  color: #7d8b97;
  font-size: 24rpx;
  font-weight: 900;
}

.helper-row {
  display: flex;
  justify-content: space-between;
  margin: 22rpx 6rpx 28rpx;
}

.helper-link {
  font-size: 24rpx;
  font-weight: 700;
  color: #55c7ff;
}

.or-text {
  display: block;
  text-align: center;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.56);
  padding: 20rpx 0;
  font-weight: 700;
}

.policy {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}

.policy .muted {
  color: rgba(255, 255, 255, 0.58);
}

.policy-box {
  width: 48rpx;
  height: 44rpx;
  border-radius: 8rpx;
  background: #0088cc;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
}

@media (min-width: 900px) {
  .login-page {
    padding-top: 34px;
  }

  .login-top,
  .auth-card {
    width: 488px;
  }
}
</style>
