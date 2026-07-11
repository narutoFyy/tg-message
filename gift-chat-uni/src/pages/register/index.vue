<template>
  <view class="page-shell soft-page register-page">
    <view class="page-stack">
      <view class="page-header register-hero tone-market">
        <view class="page-header-copy">
          <text class="eyebrow">New account</text>
          <text class="title">Create your Xcard account</text>
        </view>
        <text class="signin-link" @click="goLogin">Sign in</text>
      </view>

      <view class="register-form surface-card">
        <view class="form-grid">
          <view class="form-field">
            <text class="field-label">Username</text>
            <input v-model.trim="form.username" class="field-input" placeholder="Enter a unique username" />
          </view>
          <view class="form-field">
            <text class="field-label">Email</text>
            <input v-model.trim="form.email" class="field-input" placeholder="Optional email address" />
          </view>
          <view class="form-field form-field-wide">
            <text class="field-label">Phone number</text>
            <view class="phone-row">
              <picker class="country-control" :range="countryCodeLabels" :value="selectedCountryIndex" @change="handleCountryChange">
                <view class="country-picker">{{ selectedCountry.label }}</view>
              </picker>
              <input v-model.trim="form.phone" class="field-input phone-input" type="number" placeholder="Local phone number" />
            </view>
            <text class="phone-help">{{ phoneHelpText }}</text>
          </view>
          <view class="form-field">
            <text class="field-label">Password</text>
            <input v-model="form.password" class="field-input" password placeholder="At least 8 characters" />
          </view>
          <view class="form-field">
            <text class="field-label">Invite code</text>
            <input v-model.trim="form.inviteCode" class="field-input" placeholder="Optional invite code" />
          </view>
        </view>
        <text v-if="notice" class="form-notice">{{ notice }}</text>
        <button class="primary-button submit-button" @click="handleSubmit">Create account</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { safeRouteForRole } from '@/utils/routeGuard'
import { fetchCountryCodeRules } from '@/utils/api'
import type { CountryCodeRule } from '@/types'

const store = useAppStore()
const notice = ref('')
const fallbackCountryCodes: CountryCodeRule[] = [
  { countryCode: '+234', countryName: 'Nigeria', minLocalLength: 10, maxLocalLength: 10, enabled: true, sortOrder: 10 },
  { countryCode: '+91', countryName: 'India', minLocalLength: 10, maxLocalLength: 10, enabled: true, sortOrder: 20 },
  { countryCode: '+233', countryName: 'Ghana', minLocalLength: 9, maxLocalLength: 9, enabled: true, sortOrder: 30 },
  { countryCode: '+86', countryName: 'China', minLocalLength: 11, maxLocalLength: 11, enabled: true, sortOrder: 40 },
  { countryCode: '+44', countryName: 'United Kingdom', minLocalLength: 10, maxLocalLength: 10, enabled: true, sortOrder: 50 },
  { countryCode: '+1', countryName: 'United States', minLocalLength: 10, maxLocalLength: 10, enabled: true, sortOrder: 60 }
]
const countryCodes = ref<CountryCodeRule[]>(fallbackCountryCodes)
const selectedCountryIndex = ref(0)
const countryCodeLabels = computed(() => countryCodes.value.map((item) => `${item.countryCode} ${item.countryName}`))

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
  loadCountryCodes()
})

const selectedCountry = computed(() => {
  const rule = countryCodes.value[selectedCountryIndex.value] || countryCodes.value[0]
  return {
    ...rule,
    label: `${rule.countryCode} ${rule.countryName}`
  }
})
const phoneHelpText = computed(() => {
  const rule = selectedCountry.value
  const lengthText = rule.minLocalLength === rule.maxLocalLength
    ? `${rule.minLocalLength} digits`
    : `${rule.minLocalLength}-${rule.maxLocalLength} digits`
  return `${rule.countryName} phone numbers must use ${lengthText}. Do not enter ${rule.countryCode}.`
})

async function handleSubmit() {
  try {
    const username = form.username.trim()
    const email = form.email.trim()
    const phone = normalizeLocalPhone(form.phone)
    const password = form.password
    const inviteCode = form.inviteCode.trim()

    if (!username) {
      notice.value = 'Please enter a username.'
      return
    }
    if (!phone) {
      notice.value = 'Please enter a phone number.'
      return
    }
    if (!password) {
      notice.value = 'Please enter a password.'
      return
    }
    if (password.length < 8) {
      notice.value = 'Password must be at least 8 characters.'
      return
    }
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      notice.value = 'Please enter a valid email address.'
      return
    }
    const validationMessage = validateLocalPhone(phone)
    if (validationMessage) {
      notice.value = validationMessage
      return
    }
    const session = await store.register({
      username,
      email: email || undefined,
      phone: `${selectedCountry.value.countryCode}${phone}`,
      password,
      inviteCode: inviteCode || undefined
    })
    notice.value = 'Account created.'
    uni.redirectTo({ url: safeRouteForRole(session.nextRoute, session, '/pages/support/index') })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Registration failed'
  }
}

async function loadCountryCodes() {
  try {
    const rules = await fetchCountryCodeRules()
    const enabled = rules.filter((rule) => rule.enabled)
    if (enabled.length) {
      countryCodes.value = enabled
    }
  } catch {
    countryCodes.value = fallbackCountryCodes
  }
}

function handleCountryChange(event: { detail: { value: number | string } }) {
  selectedCountryIndex.value = Number(event.detail.value || 0)
}

function goLogin() {
  uni.navigateBack({ fail: () => uni.redirectTo({ url: '/pages/login/index' }) })
}

function normalizeLocalPhone(value: string) {
  const trimmed = value.trim()
  const normalizedCountry = selectedCountry.value.countryCode.replace(/[^0-9]/g, '')
  let withoutCountry = trimmed
  if (withoutCountry.startsWith('+')) {
    const digits = withoutCountry.replace(/[^0-9]/g, '')
    withoutCountry = digits.startsWith(normalizedCountry) ? digits.slice(normalizedCountry.length) : digits
  } else if (withoutCountry.startsWith('00')) {
    const digits = withoutCountry.replace(/[^0-9]/g, '')
    withoutCountry = digits.startsWith(`00${normalizedCountry}`) ? digits.slice(normalizedCountry.length + 2) : digits
  }
  return withoutCountry.replace(/[^0-9]/g, '')
}

function validateLocalPhone(phone: string) {
  const rule = selectedCountry.value
  if (phone.length < rule.minLocalLength || phone.length > rule.maxLocalLength) {
    const lengthText = rule.minLocalLength === rule.maxLocalLength
      ? `${rule.minLocalLength} digits`
      : `${rule.minLocalLength}-${rule.maxLocalLength} digits`
    return `${rule.countryName} phone numbers must use ${lengthText}.`
  }
  return ''
}
</script>

<style scoped lang="scss">
.register-page {
  padding-bottom: 48rpx;
}

.page-header-copy {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.register-hero {
  padding: 26rpx 28rpx;
  align-items: center;
  border-bottom: 0;
}

.signin-link {
  flex: 0 0 auto;
  padding: 16rpx 0;
  color: #002fa7;
  font-size: 26rpx;
  font-weight: 700;
}

.register-form {
  padding: 30rpx 28rpx;
  box-shadow: 0 12rpx 34rpx rgba(34, 54, 74, 0.07);
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24rpx;
}

.form-field,
.form-field-wide {
  min-width: 0;
}

.phone-row {
  display: grid;
  grid-template-columns: minmax(176rpx, 220rpx) minmax(0, 1fr);
  gap: 12rpx;
  align-items: stretch;
  width: 100%;
}

.country-control {
  min-width: 0;
}

.country-picker {
  height: 88rpx;
  width: 100%;
  padding: 0 16rpx;
  box-sizing: border-box;
  border-radius: 6rpx;
  background: #f7f7f8;
  border: 1rpx solid #c8c9cf;
  display: flex;
  align-items: center;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 24rpx;
  font-weight: 700;
  color: #111111;
}

.phone-input {
  flex: 1;
  min-width: 0;
}

.phone-help {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.4;
  color: #6f7178;
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

@media (min-width: 768px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-field-wide {
    grid-column: 1 / -1;
  }
}
</style>
