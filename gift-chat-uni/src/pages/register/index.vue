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
import { fallbackCountryCodeRules } from '@/data/supported-countries'

const store = useAppStore()
const notice = ref('')
const countryCodes = ref<CountryCodeRule[]>(fallbackCountryCodeRules)
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
      countryCode: selectedCountry.value.code,
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
      const selectedIndex = enabled.findIndex((rule) => rule.code === store.state.selectedCountryCode)
      selectedCountryIndex.value = selectedIndex >= 0 ? selectedIndex : 0
    }
  } catch {
    countryCodes.value = fallbackCountryCodeRules
  }
}

function handleCountryChange(event: { detail: { value: number | string } }) {
  selectedCountryIndex.value = Number(event.detail.value || 0)
  store.setCountry(selectedCountry.value.code)
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
  --register-gold: #dfc285;
  --register-text: #f2f2f0;
  --register-muted: #a3a5a9;
  min-height: 100vh;
  min-height: 100dvh;
  padding: max(64px, env(safe-area-inset-top)) max(24px, env(safe-area-inset-right)) max(48px, env(safe-area-inset-bottom)) max(24px, env(safe-area-inset-left));
  background: #101112;
  color: var(--register-text);
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  letter-spacing: 0;
  box-sizing: border-box;
}

.register-page .page-stack {
  width: 100%;
  max-width: 660px;
  gap: 0;
}

.register-hero {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 0 0 32px;
  border: 0;
  border-bottom: 1px solid #343536;
  border-radius: 0;
  background: transparent;
}

.register-hero::before {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 56px;
  height: 2px;
  background: var(--register-gold);
}

.page-header-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-header-copy::before {
  content: "";
  display: block;
  width: 48px;
  height: 48px;
  margin-bottom: 22px;
  background: url('/static/lottery/stone-technology-icon.png') center / contain no-repeat;
}

.register-page .eyebrow {
  color: var(--register-gold);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0;
  text-transform: uppercase;
}

.register-page .title {
  max-width: 430px;
  margin: 0;
  color: var(--register-text);
  font-size: 32px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.signin-link {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 2px;
  color: var(--register-gold);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 5px;
  text-decoration-color: #746441;
}

.register-form {
  padding: 32px 0 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
}

.form-field,
.form-field-wide {
  min-width: 0;
}

.register-page .field-label {
  display: block;
  margin-bottom: 10px;
  color: #d7d8db;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
}

.register-page .field-input,
.country-picker {
  width: 100%;
  height: 52px;
  min-height: 52px;
  padding: 0 16px;
  box-sizing: border-box;
  border: 1px solid #3e4043;
  border-radius: 6px;
  background: #1b1d20;
  color: var(--register-text);
  font-size: 15px;
  font-weight: 400;
  line-height: normal;
  transition: border-color 160ms ease, box-shadow 160ms ease, background-color 160ms ease;
}

.register-page .field-input:focus-within,
.country-control:focus-within .country-picker {
  border-color: var(--register-gold);
  background: #202225;
  box-shadow: 0 0 0 3px rgb(223 194 133 / 12%);
}

.register-page :deep(.uni-input-placeholder),
.register-page :deep(input::placeholder) {
  color: var(--register-muted);
  opacity: 1;
}

.register-page :deep(input) {
  caret-color: var(--register-gold);
}

.phone-row {
  display: grid;
  grid-template-columns: minmax(140px, 190px) minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  width: 100%;
}

.country-control {
  min-width: 0;
}

.country-picker {
  display: flex;
  align-items: center;
  padding-right: 32px;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}

.country-picker::after {
  content: "";
  position: absolute;
  right: 14px;
  top: 20px;
  width: 6px;
  height: 6px;
  border-right: 1px solid var(--register-gold);
  border-bottom: 1px solid var(--register-gold);
  transform: rotate(45deg);
}

.phone-input {
  flex: 1;
  min-width: 0;
}

.phone-help {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--register-muted);
}

.form-notice {
  display: block;
  margin-top: 24px;
  padding: 12px 14px;
  border-left: 2px solid #ef9ca6;
  background: #2c1f24;
  color: #ffc3cc;
  font-size: 13px;
  line-height: 1.5;
}

.register-page .submit-button {
  width: 100%;
  min-height: 52px;
  margin-top: 32px;
  padding: 15px 24px;
  border: 1px solid var(--register-gold);
  border-radius: 6px;
  background: var(--register-gold);
  color: #191711;
  font-size: 15px;
  font-weight: 700;
  line-height: 20px;
  box-shadow: none;
  cursor: pointer;
  transition: background-color 160ms ease, border-color 160ms ease;
}

.register-page .submit-button:focus-visible {
  outline: 2px solid var(--register-text);
  outline-offset: 4px;
}

.register-page .submit-button:active {
  background: #c9ab6d;
  border-color: #c9ab6d;
}

@media (hover: hover) {
  .register-page .submit-button:hover {
    background: #ecd5a7;
    border-color: #ecd5a7;
  }

  .signin-link:hover {
    color: #f3ddb0;
    text-decoration-color: currentColor;
  }
}

@media (min-width: 768px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 24px;
  }

  .form-field-wide {
    grid-column: 1 / -1;
  }
}

@media (max-width: 480px) {
  .register-page {
    padding-top: max(32px, env(safe-area-inset-top));
    padding-right: max(20px, env(safe-area-inset-right));
    padding-left: max(20px, env(safe-area-inset-left));
  }

  .register-hero {
    gap: 16px;
    padding-bottom: 24px;
  }

  .page-header-copy::before {
    width: 40px;
    height: 40px;
    margin-bottom: 10px;
  }

  .register-page .title {
    font-size: 26px;
  }

  .register-form {
    padding-top: 24px;
  }

  .form-grid {
    gap: 20px;
  }

  .phone-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }

  .register-page .field-input {
    font-size: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-page .field-input,
  .country-picker,
  .register-page .submit-button {
    transition: none;
  }
}
</style>
