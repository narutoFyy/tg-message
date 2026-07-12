<template>
  <view class="page-shell login-page">
    <view class="login-layout">
      <view class="brand-panel tone-market">
        <view class="brand-mark">
          <image class="brand-logo" src="/static/lottery/stone-technology-icon.png" mode="aspectFit" />
          <text class="brand-name">Xcard</text>
        </view>
        <view class="brand-copy">
          <text class="eyebrow">Gift card exchange</text>
          <text class="brand-title">Your rates, orders and support in one place.</text>
          <text class="subtitle">Sign in to continue to your account.</text>
        </view>
      </view>

      <view class="auth-card surface-card">
        <view class="tab-strip">
          <view class="tab active">
            <text>Sign in</text>
          </view>
          <view class="tab muted-tab" @click="goRegister">
            <text>Sign up</text>
          </view>
        </view>

        <view class="form-body">
          <view class="form-field">
            <text class="field-label">Country</text>
            <view class="field-input picker-field" @click="pickCountry">
              <text>{{ store.selectedCountry().name }}</text>
              <text class="picker-arrow">Change</text>
            </view>
          </view>

          <view class="form-field">
            <text class="field-label">Username, email or phone</text>
            <input v-model.trim="form.identifier" class="field-input" placeholder="Enter username, email or full phone number" />
          </view>

          <view class="form-field">
            <text class="field-label">Password</text>
            <input v-model="form.password" class="field-input" password placeholder="Enter your password" />
          </view>

          <text v-if="notice" class="form-notice">{{ notice }}</text>
          <button class="primary-button submit-button" @click="handleSubmit">Sign in</button>
          <text class="policy">By continuing, you agree to the Terms of Use and acknowledge the Privacy Policy.</text>
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
      notice.value = 'Username, email or phone is required.'
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
.login-page {
  padding-bottom: 40rpx;
  background: var(--cb-canvas);
}

.login-layout {
  width: 100%;
  max-width: 1040rpx;
  margin: 0 auto;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #cfe4fb;
}

.brand-panel {
  padding: 28rpx;
  box-sizing: border-box;
}

.brand-logo {
  width: 64rpx;
  height: 64rpx;
}

.brand-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.brand-copy {
  padding: 54rpx 0 20rpx;
}

.brand-title {
  display: block;
  max-width: 600rpx;
  margin: 16rpx 0 18rpx;
  font-size: 50rpx;
  line-height: 1.12;
  font-weight: 700;
  color: #111111;
}

.auth-card {
  width: 100%;
  overflow: hidden;
  box-shadow: 0 16rpx 42rpx rgba(34, 54, 74, 0.09);
}

.tab-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.tab {
  position: relative;
  min-height: 92rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
  border-bottom: 1rpx solid #dedfe3;
}

.tab text {
  color: #6f7178;
}

.tab.active {
  border-bottom: 4rpx solid #002fa7;
}

.tab.active text {
  color: #002fa7;
}

.muted-tab {
  background: #f7f7f8;
}

.form-body {
  padding: 32rpx 28rpx 28rpx;
}

.form-field + .form-field {
  margin-top: 24rpx;
}

.picker-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-arrow {
  color: #002fa7;
  font-size: 24rpx;
  font-weight: 700;
}

.form-notice {
  display: block;
  margin-top: 22rpx;
  color: #b42318;
  font-size: 24rpx;
}

.submit-button {
  width: 100%;
  margin-top: 28rpx;
}

.policy {
  display: block;
  margin-top: 22rpx;
  color: #6f7178;
  font-size: 22rpx;
  line-height: 1.5;
}

@media (min-width: 768px) {
  .login-page {
    display: flex;
    align-items: center;
    padding-top: 40rpx;
  }

  .login-layout {
    max-width: 1040px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 520px;
    gap: 72rpx;
    align-items: center;
  }

  .brand-panel {
    align-self: stretch;
    padding: 42rpx;
  }

  .brand-copy {
    padding-top: 100rpx;
  }
}
</style>
