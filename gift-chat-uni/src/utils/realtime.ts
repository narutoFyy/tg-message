import type { ChatRealtimePayload } from '@/types'
import { buildChatSocketUrl } from '@/utils/api'

type SocketCloseOptions = Parameters<UniApp.SocketTask['close']>[0]
type ManagedSocket = Pick<UniApp.SocketTask, 'close'>

const activeSockets = new Set<ManagedSocket>()

export function connectChatSocket(
  channelType: 'friend' | 'support',
  channelId: string,
  onMessage: (message: ChatRealtimePayload) => void,
  options: {
    onOpen?: () => void
    onClose?: () => void
    onError?: () => void
    onReconnect?: (attempt: number) => void
  } = {}
) {
  let closedByUser = false
  let attempt = 0
  let socketTask: UniApp.SocketTask | null = null
  let webSocket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  const managedSocket = {
    close(options?: SocketCloseOptions) {
      closedByUser = true
      clearReconnectTimer()
      activeSockets.delete(managedSocket)
      if (webSocket) {
        const code = options?.code || 1000
        const reason = options?.reason || ''
        webSocket.close(code, reason)
        webSocket = null
      }
      if (socketTask && typeof socketTask.close === 'function') {
        socketTask.close(options || {})
      }
    }
  } as ManagedSocket

  const connect = () => {
    clearReconnectTimer()
    if (useNativeWebSocket()) {
      connectNativeSocket()
      return
    }

    const task = uni.connectSocket({
      url: buildChatSocketUrl(channelType, channelId)
    })

    // 检查返回的任务对象是否有效
    if (!task || typeof task !== 'object') {
      console.error('uni.connectSocket 返回了无效的对象')
      options.onError?.()
      scheduleReconnect()
      return
    }

    socketTask = task as unknown as UniApp.SocketTask
    activeSockets.add(managedSocket)

    if (
      typeof socketTask.onOpen !== 'function'
      || typeof socketTask.onMessage !== 'function'
      || typeof socketTask.onClose !== 'function'
      || typeof socketTask.onError !== 'function'
    ) {
      console.error('socketTask 缺少必要的事件方法')
      options.onError?.()
      scheduleReconnect()
      return
    }

    socketTask.onOpen(() => {
      attempt = 0
      options.onOpen?.()
    })

    socketTask.onMessage((event: { data: string }) => {
      handleMessage(event.data, onMessage)
    })

    socketTask.onClose(() => {
      activeSockets.delete(managedSocket)
      options.onClose?.()
      scheduleReconnect()
    })

    socketTask.onError(() => {
      activeSockets.delete(managedSocket)
      options.onError?.()
      scheduleReconnect()
    })
  }

  const connectNativeSocket = () => {
    activeSockets.add(managedSocket)
    const socket = new WebSocket(buildChatSocketUrl(channelType, channelId))
    webSocket = socket

    socket.onopen = () => {
      attempt = 0
      options.onOpen?.()
    }

    socket.onmessage = (event) => {
      handleMessage(event.data, onMessage)
    }

    socket.onclose = () => {
      if (webSocket === socket) {
        webSocket = null
      }
      activeSockets.delete(managedSocket)
      options.onClose?.()
      scheduleReconnect()
    }

    socket.onerror = () => {
      options.onError?.()
      if (socket.readyState === WebSocket.CLOSED || socket.readyState === WebSocket.CLOSING) {
        scheduleReconnect()
      }
    }
  }

  const scheduleReconnect = () => {
    if (closedByUser) return
    if (reconnectTimer) return
    attempt += 1
    const delay = Math.min(1000 * attempt, 5000)
    options.onReconnect?.(attempt)
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      if (!closedByUser) {
        connect()
      }
    }, delay)
  }

  const clearReconnectTimer = () => {
    if (!reconnectTimer) return
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  connect()

  return managedSocket as UniApp.SocketTask
}

export function closeAllChatSockets() {
  Array.from(activeSockets).forEach((socket) => {
    try {
      socket.close({})
    } catch {
      // ignore close failures during logout
    } finally {
      activeSockets.delete(socket)
    }
  })
}

function useNativeWebSocket() {
  return typeof window !== 'undefined' && typeof window.WebSocket === 'function'
}

function handleMessage(data: unknown, onMessage?: (message: ChatRealtimePayload) => void) {
  try {
    const payload = JSON.parse(String(data)) as ChatRealtimePayload
    onMessage?.(payload)
  } catch {
    // ignore malformed payloads
  }
}
