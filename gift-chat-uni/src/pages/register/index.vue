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
      <view class="phone-row">
        <picker :range="countryCodeLabels" :value="selectedCountryIndex" @change="handleCountryChange">
          <view class="country-picker">{{ selectedCountry.label }}</view>
        </picker>
        <input v-model.trim="form.phone" class="field-input phone-input" placeholder="Local phone number" />
      </view>
      <view style="height: 20rpx"></view>
      <text class="field-label">Password</text>
      <input v-model="form.password" class="field-input" password placeholder="Password" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Invite code</text>
      <input v-model.trim="form.inviteCode" class="field-input" placeholder="Optional invite code" />
      <view style="height: 24rpx"></view>
      <button class="primary-button" @click="handleSubmit">Create account</button>
      <view style="height: 16rpx"></view>
      <text v-if="notice" class="muted">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { safeRouteForRole } from '@/utils/routeGuard'

const store = useAppStore()
const notice = ref('')
const countryCodes = [
  { code: '+234', label: '+234 Nigeria' },
  { code: '+91', label: '+91 India' },
  { code: '+233', label: '+233 Ghana' },
  { code: '+86', label: '+86 China' },
  { code: '+44', label: '+44 United Kingdom' },
  { code: '+1', label: '+1 United States' }
]
const selectedCountryIndex = ref(0)
const countryCodeLabels = countryCodes.map((item) => item.label)

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  inviteCode: ''
})

onShow(() => {
  form.username = ''
  form.email = ''
  form.phone = ''
  form.password = ''
  form.inviteCode = ''
  notice.value = ''
  selectedCountryIndex.value = 0
})

const selectedCountry = computed(() => countryCodes[selectedCountryIndex.value] || countryCodes[0])

async function handleSubmit() {
  try {
    const username = form.username.trim()
    const email = form.email.trim()
    const phone = normalizeLocalPhone(form.phone)
    const password = form.password
    const inviteCode = form.inviteCode.trim()

    if (!username) {
      notice.value = 'Username is required.'
      return
    }
    if (!phone) {
      notice.value = 'Phone is required.'
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
      phone: phone ? `${selectedCountry.value.code}${phone}` : undefined,
      password,
      inviteCode: inviteCode || undefined
    })
    notice.value = 'Registration complete.'
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/support/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Registration failed'
  }
}

function handleCountryChange(event: { detail: { value: number | string } }) {
  selectedCountryIndex.value = Number(event.detail.value || 0)
}

function normalizeLocalPhone(value: string) {
  const trimmed = value.trim()
  const withoutCountry = trimmed.startsWith('+') ? trimmed.replace(/^\+\d{1,4}\s*/, '') : trimmed
  return withoutCountry.replace(/[^0-9]/g, '')
}
</script>

<style scoped lang="scss">
.phone-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.country-picker {
  height: 92rpx;
  min-width: 220rpx;
  padding: 0 18rpx;
  border-radius: 12rpx;
  background: #f2f5f7;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 800;
  color: #20262d;
}

.phone-input {
  flex: 1;
  min-width: 0;
}
</style>
