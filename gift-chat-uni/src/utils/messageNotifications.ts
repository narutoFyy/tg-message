const APP_TITLE = 'Xcard'
const GENERIC_MESSAGE_TEXT = '您收到了一条消息'

let audioContext: AudioContext | null = null
let audioUnlocked = false
let audioUnlockInstalled = false

declare const plus: {
  runtime?: {
    setBadgeNumber?: (count: number) => void
  }
  push?: {
    createMessage?: (content: string, title: string, options?: Record<string, unknown>) => void
  }
} | undefined

type BrowserBadgeNavigator = Navigator & {
  setAppBadge?: (count?: number) => Promise<void>
  clearAppBadge?: () => Promise<void>
}

export function setAppUnreadBadge(count: number) {
  const normalized = Math.max(0, Math.floor(Number(count) || 0))

  if (typeof document !== 'undefined') {
    document.title = normalized > 0 ? `(${normalized > 99 ? '99+' : normalized}) ${APP_TITLE}` : APP_TITLE
    const badgeNavigator = navigator as BrowserBadgeNavigator
    const update = normalized > 0
      ? badgeNavigator.setAppBadge?.(normalized)
      : badgeNavigator.clearAppBadge?.()
    update?.catch(() => {})
  }

  if (typeof plus !== 'undefined') {
    plus.runtime?.setBadgeNumber?.(normalized)
  }
}

export async function requestMessageNotificationPermission() {
  // #ifdef H5
  if (typeof Notification === 'undefined') return 'unsupported'
  if (Notification.permission === 'default') return Notification.requestPermission()
  return Notification.permission
  // #endif

  return 'unsupported'
}

export function installMessageAudioUnlock() {
  // #ifdef H5
  if (audioUnlockInstalled || typeof window === 'undefined') return
  audioUnlockInstalled = true
  const unlock = () => {
    audioUnlocked = true
    try {
      const AudioContextCtor = window.AudioContext
        || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
      if (AudioContextCtor && !audioContext) {
        audioContext = new AudioContextCtor()
      }
      audioContext?.resume().catch(() => {})
    } catch {
      // The unread indicator remains available when audio is unsupported.
    }
  }
  window.addEventListener('pointerdown', unlock, { passive: true })
  window.addEventListener('touchstart', unlock, { passive: true })
  window.addEventListener('keydown', unlock)
  // #endif
}

export function playMessageNotificationSound() {
  // #ifdef H5
  if (!audioUnlocked || typeof window === 'undefined') return false
  try {
    const AudioContextCtor = window.AudioContext
      || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AudioContextCtor) return false
    audioContext ||= new AudioContextCtor()
    const oscillator = audioContext.createOscillator()
    const gain = audioContext.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = 880
    gain.gain.value = 0.06
    oscillator.connect(gain)
    gain.connect(audioContext.destination)
    oscillator.start()
    oscillator.stop(audioContext.currentTime + 0.16)
    return true
  } catch {
    return false
  }
  // #endif

  return false
}

export function notifyIncomingCustomerMessage(conversationId: string) {
  const payload = JSON.stringify({
    channelType: 'support',
    channelId: conversationId,
    route: '/pages/support/index'
  })

  if (typeof plus !== 'undefined' && plus.push?.createMessage) {
    plus.push.createMessage(GENERIC_MESSAGE_TEXT, APP_TITLE, { payload })
    return
  }

  // #ifdef H5
  if (typeof document !== 'undefined' && document.hidden && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
    const notification = new Notification(APP_TITLE, {
      body: GENERIC_MESSAGE_TEXT,
      icon: '/static/pwa/icons/xcard-192.png',
      tag: `customer-support-${conversationId}`
    })
    notification.onclick = () => {
      window.focus()
      window.location.hash = `/pages/support/index?conversationId=${encodeURIComponent(conversationId)}`
      notification.close()
    }
    return
  }
  // #endif

  uni.showToast({
    title: GENERIC_MESSAGE_TEXT,
    icon: 'none',
    duration: 3000
  })
}

export function notifyIncomingSupportMessage(customerName: string, preview: string, conversationId: string) {
  const title = customerName || 'New customer message'
  const body = preview || 'You have a new support message.'
  const payload = JSON.stringify({
    channelType: 'support',
    channelId: conversationId,
    route: '/pages/support-chat-v2/index'
  })

  if (typeof plus !== 'undefined' && plus.push?.createMessage) {
    plus.push.createMessage(body, title, { payload })
    return
  }

  // #ifdef H5
  if (typeof document !== 'undefined' && document.hidden && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
    const notification = new Notification(title, {
      body,
      icon: '/static/pwa/icons/xcard-192.png',
      tag: `support-${conversationId}`
    })
    notification.onclick = () => {
      window.focus()
      window.location.hash = `/pages/support-chat-v2/index?conversationId=${encodeURIComponent(conversationId)}`
      notification.close()
    }
    return
  }
  // #endif

  uni.showToast({
    title: `${title}: ${body}`.slice(0, 80),
    icon: 'none',
    duration: 3000
  })
}
