import { ref } from 'vue'

export const PRIVACY_VERSION = '2026-10-26.1'
const CONSENT_KEY = 'xcard-privacy-consent'
export const privacyAccepted = ref(uni.getStorageSync(CONSENT_KEY) === PRIVACY_VERSION)
let privacyRedirectPending = false

declare const plus: { os?: { name?: string }; runtime?: {
  isAgreePrivacy?: () => boolean
  agreePrivacy?: () => void
} } | undefined

export function hasPrivacyConsent() { return privacyAccepted.value }

export function ensurePrivacyRoute() {
  if (hasPrivacyConsent() || privacyRedirectPending) return
  const pages = getCurrentPages()
  let route = pages[pages.length - 1]?.route || ''
  // #ifdef H5
  route = window.location.hash.slice(1) || '/'
  // #endif
  if (route && route !== '/' && !/^\/?pages\/(login|register|admin-login|legal)\/index(?:\?|$)/.test(route)) {
    privacyRedirectPending = true
    uni.reLaunch({ url: '/pages/login/index', complete: () => { privacyRedirectPending = false } })
  }
}

export function acceptPrivacy() {
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined') plus.runtime?.agreePrivacy?.()
  // #endif
  uni.setStorageSync(CONSENT_KEY, PRIVACY_VERSION)
  privacyAccepted.value = true
}

export function syncNativePrivacyConsent() {
  // The native template runs before the Android runtime starts. Its version
  // matches this document version, so a changed policy prompts again natively.
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.os?.name === 'Android' && plus.runtime?.isAgreePrivacy) {
    if (plus.runtime.isAgreePrivacy()) acceptPrivacy()
    else {
      uni.removeStorageSync(CONSENT_KEY)
      privacyAccepted.value = false
    }
  }
  // #endif
}

export function openLegal(kind: 'privacy' | 'terms') {
  uni.navigateTo({ url: `/pages/legal/index?kind=${kind}` })
}

export function requirePrivacyConsent() {
  if (!hasPrivacyConsent()) throw new Error('Please review and agree to the Privacy Policy before continuing.')
}
