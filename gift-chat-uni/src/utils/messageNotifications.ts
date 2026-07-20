const APP_TITLE = 'Xcard'

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
