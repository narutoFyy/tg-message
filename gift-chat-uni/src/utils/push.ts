import { registerPushDevice } from '@/utils/api'
import { hasPrivacyConsent } from '@/utils/privacy'
import { getStoredSessionUser } from '@/utils/api'
import { safeRouteForRole, supportRouteForRole } from '@/utils/routeGuard'

type PushClickPayload = {
  channelType?: 'support' | 'friend'
  channelId?: string
  route?: string
}

type PlusPushClientInfo = {
  clientid?: string
  token?: string
  id?: string
}

declare const plus: {
  os?: { name?: string }
  runtime?: { version?: string }
  device?: { model?: string }
  push?: {
    getClientInfoAsync?: (success: (info: PlusPushClientInfo) => void, fail?: (error: unknown) => void) => void
    addEventListener?: (event: 'click' | 'receive', callback: (message: { payload?: unknown }) => void) => void
  }
} | undefined

export async function registerNativePushDevice() {
  if (!hasPrivacyConsent()) return { skipped: true }
  // Native push methods can exist even when the APK has no Push module.
  // Calling those stubs opens a blocking HTML5+ missing-module dialog.
  if (import.meta.env.VITE_APP_NATIVE_PUSH_ENABLED !== 'true') {
    return { skipped: true }
  }

  if (typeof plus === 'undefined' || !plus.push?.getClientInfoAsync) {
    return { skipped: true }
  }

  const info = await new Promise<PlusPushClientInfo>((resolve, reject) => {
    plus.push?.getClientInfoAsync?.(resolve, reject)
  })
  const deviceToken = info.clientid || info.token || info.id || ''
  if (!deviceToken) {
    return { skipped: true }
  }

  const platform = (plus.os?.name || '').toLowerCase().includes('ios') ? 'ios' : 'android'
  const device = await registerPushDevice({
    platform,
    provider: 'tencent',
    deviceToken,
    deviceModel: plus.device?.model || '',
    appVersion: plus.runtime?.version || ''
  })
  installPushClickRouter()
  return { skipped: false, device }
}

export function installPushClickRouter() {
  if (typeof plus === 'undefined' || !plus.push?.addEventListener) {
    return
  }
  plus.push.addEventListener('click', (message) => {
    const payload = normalizePayload(message.payload)
    const user = getStoredSessionUser()
    if (payload.channelType === 'support' && payload.channelId) {
      const page = supportRouteForRole(user)
      uni.navigateTo({ url: `${page}?conversationId=${encodeURIComponent(payload.channelId)}` })
      return
    }
    if (payload.route) {
      uni.navigateTo({ url: safeRouteForRole(payload.route, user) })
    }
  })
}

function normalizePayload(payload: unknown): PushClickPayload {
  if (!payload) {
    return {}
  }
  if (typeof payload === 'string') {
    try {
      return JSON.parse(payload) as PushClickPayload
    } catch {
      return {}
    }
  }
  if (typeof payload === 'object') {
    return payload as PushClickPayload
  }
  return {}
}
