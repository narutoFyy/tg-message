import {
  disableWebPushSubscription,
  fetchWebPushConfiguration,
  registerWebPushSubscription
} from '@/utils/api'

export interface WebPushState {
  supported: boolean
  serverEnabled: boolean
  subscribed: boolean
  permission: NotificationPermission | 'unsupported'
  requiresHomeScreen: boolean
}

const SERVICE_WORKER_URL = '/static/pwa/service-worker.js'

export async function readWebPushState(): Promise<WebPushState> {
  const supported = browserSupportsWebPush()
  const configuration = await fetchWebPushConfiguration()
  if (!supported) {
    return {
      supported: false,
      serverEnabled: configuration.enabled,
      subscribed: false,
      permission: 'unsupported',
      requiresHomeScreen: isIosBrowser() && !isStandaloneDisplay()
    }
  }

  const registration = await findServiceWorkerRegistration()
  const subscription = registration ? await registration.pushManager.getSubscription() : null
  return {
    supported: true,
    serverEnabled: configuration.enabled && !!configuration.publicKey,
    subscribed: !!subscription,
    permission: Notification.permission,
    requiresHomeScreen: false
  }
}

export async function enableWebPushNotifications(): Promise<WebPushState> {
  if (!browserSupportsWebPush()) {
    throw new Error(isIosBrowser()
      ? 'Install Xcard on the Home Screen, then open it there to enable notifications.'
      : 'Push notifications are not supported by this browser.')
  }

  const configuration = await fetchWebPushConfiguration()
  if (!configuration.enabled || !configuration.publicKey) {
    throw new Error('Message notifications are not available yet.')
  }

  const permission = await Notification.requestPermission()
  if (permission !== 'granted') {
    throw new Error(permission === 'denied'
      ? 'Notifications are blocked in browser settings.'
      : 'Notification permission was not granted.')
  }

  const registration = await ensureServiceWorkerRegistration()
  let subscription = await registration.pushManager.getSubscription()
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(configuration.publicKey) as BufferSource
    })
  }

  const serialized = subscription.toJSON()
  if (!serialized.endpoint || !serialized.keys?.p256dh || !serialized.keys.auth) {
    await subscription.unsubscribe()
    throw new Error('The browser returned an incomplete push subscription.')
  }

  try {
    await registerWebPushSubscription({
      endpoint: serialized.endpoint,
      keys: {
        p256dh: serialized.keys.p256dh,
        auth: serialized.keys.auth
      },
      userAgent: typeof navigator === 'undefined' ? '' : navigator.userAgent
    })
  } catch (error) {
    await subscription.unsubscribe()
    throw error
  }

  return readWebPushState()
}

export async function disableWebPushNotifications(): Promise<WebPushState> {
  if (!browserSupportsWebPush()) {
    return readWebPushState()
  }
  const registration = await findServiceWorkerRegistration()
  const subscription = registration ? await registration.pushManager.getSubscription() : null
  let apiError: unknown
  if (subscription) {
    try {
      await disableWebPushSubscription(subscription.endpoint)
    } catch (error) {
      apiError = error
    }
    await subscription.unsubscribe()
  }
  if (apiError) throw apiError
  return readWebPushState()
}

function browserSupportsWebPush() {
  return typeof window !== 'undefined'
    && typeof navigator !== 'undefined'
    && 'serviceWorker' in navigator
    && 'PushManager' in window
    && 'Notification' in window
}

async function findServiceWorkerRegistration() {
  if (!browserSupportsWebPush()) return undefined
  return navigator.serviceWorker.getRegistration('/')
}

async function ensureServiceWorkerRegistration() {
  const existing = await navigator.serviceWorker.getRegistration('/')
  if (existing) return existing
  return navigator.serviceWorker.register(SERVICE_WORKER_URL, { scope: '/' })
}

function isIosBrowser() {
  if (typeof navigator === 'undefined') return false
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
}

function isStandaloneDisplay() {
  if (typeof window === 'undefined') return false
  const iosNavigator = navigator as Navigator & { standalone?: boolean }
  return window.matchMedia('(display-mode: standalone)').matches || iosNavigator.standalone === true
}

function urlBase64ToUint8Array(value: string) {
  const padding = '='.repeat((4 - (value.length % 4)) % 4)
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/')
  const decoded = window.atob(base64)
  return Uint8Array.from(decoded, character => character.charCodeAt(0))
}
