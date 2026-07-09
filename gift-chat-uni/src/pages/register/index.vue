<template>
  <view class="page-shell soft-page">
    <view class="panel">
      <text class="eyebrow">Sign up</text>
      <view style="height: 12rpx"></view>
      <text class="title">Create your CardBrother account</text>
    </view>

    <view class="panel">
      <text class="field-label">Username</text>
      <input v-model.trim="form.username" class="field-input" placeholder="Enter a unique username" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Email</text>
      <input v-model.trim="form.email" class="field-input" placeholder="Optional email address" />
      <view style="height: 20rpx"></view>
      <text class="field-label">Phone number</text>
      <view class="phone-row">
        <picker :range="countryCodeLabels" :value="selectedCountryIndex" @change="handleCountryChange">
          <view class="country-picker">{{ selectedCountry.label }}</view>
        </picker>
        <input v-model.trim="form.phone" class="field-input phone-input" placeholder="Enter local phone number only" />
      </view>
      <text class="phone-help">{{ phoneHelpText }}</text>
      <view style="height: 20rpx"></view>
      <text class="field-label">Password</text>
      <input v-model="form.password" class="field-input" password placeholder="At least 8 characters" />
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

.phone-help {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.4;
  color: #6f7a86;
}

@media (max-width: 420px) {
  .phone-row {
    align-items: stretch;
    flex-direction: column;
  }

  .country-picker {
    justify-content: flex-start;
  }
}
</style>
