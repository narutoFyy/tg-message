import type { ChatRealtimePayload } from '@/types'
import { buildChatSocketUrl } from '@/utils/api'

type SocketCloseOptions = Parameters<UniApp.SocketTask['close']>[0]
type ManagedSocket = Pick<UniApp.SocketTask, 'close'>

const HEARTBEAT_INTERVAL_MS = 25000
const MAX_MISSED_HEARTBEATS = 2
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
  let missedHeartbeats = 0
  let socketTask: UniApp.SocketTask | null = null
  let webSocket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  const managedSocket = {
    close(options?: SocketCloseOptions) {
      closedByUser = true
      clearReconnectTimer()
      stopHeartbeat()
      activeSockets.delete(managedSocket)
      if (webSocket) {
        const code = options?.code || 1000
        const reason = options?.reason || ''
        webSocket.close(code, reason)
        webSocket = null
      }
      if (socketTask && typeof socketTask.close === 'function') {
        socketTask.close(options || {})
        socketTask = null
      }
    }
  } as ManagedSocket

  const connect = () => {
    clearReconnectTimer()
    stopHeartbeat()
    if (useNativeWebSocket()) {
      connectNativeSocket()
      return
    }

    const task = uni.connectSocket({
      url: buildChatSocketUrl(channelType, channelId)
    })

    if (!task || typeof task !== 'object') {
      console.error('uni.connectSocket returned an invalid task')
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
      console.error('socketTask is missing required event handlers')
      options.onError?.()
      scheduleReconnect()
      return
    }

    socketTask.onOpen(() => {
      attempt = 0
      startHeartbeat()
      options.onOpen?.()
    })

    socketTask.onMessage((event: { data: string }) => {
      if (isPong(event.data)) {
        recordPong()
        return
      }
      handleMessage(event.data, onMessage)
    })

    socketTask.onClose(() => {
      socketTask = null
      stopHeartbeat()
      activeSockets.delete(managedSocket)
      options.onClose?.()
      scheduleReconnect()
    })

    socketTask.onError(() => {
      stopHeartbeat()
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
      startHeartbeat()
      options.onOpen?.()
    }

    socket.onmessage = (event) => {
      if (isPong(event.data)) {
        recordPong()
        return
      }
      handleMessage(event.data, onMessage)
    }

    socket.onclose = () => {
      if (webSocket === socket) {
        webSocket = null
      }
      stopHeartbeat()
      activeSockets.delete(managedSocket)
      options.onClose?.()
      scheduleReconnect()
    }

    socket.onerror = () => {
      options.onError?.()
      if (socket.readyState === WebSocket.CLOSED || socket.readyState === WebSocket.CLOSING) {
        stopHeartbeat()
        scheduleReconnect()
      }
    }
  }

  const startHeartbeat = () => {
    stopHeartbeat()
    missedHeartbeats = 0
    heartbeatTimer = setInterval(() => {
      sendHeartbeat()
    }, HEARTBEAT_INTERVAL_MS)
  }

  const stopHeartbeat = () => {
    missedHeartbeats = 0
    if (!heartbeatTimer) return
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }

  const sendHeartbeat = () => {
    if (closedByUser) return
    missedHeartbeats += 1
    if (missedHeartbeats > MAX_MISSED_HEARTBEATS) {
      closeForReconnect()
      return
    }

    try {
      if (webSocket && webSocket.readyState === WebSocket.OPEN) {
        webSocket.send('ping')
        return
      }

      if (socketTask && typeof socketTask.send === 'function') {
        socketTask.send({ data: 'ping' })
      }
    } catch {
      closeForReconnect()
    }
  }

  const recordPong = () => {
    missedHeartbeats = 0
  }

  const closeForReconnect = () => {
    stopHeartbeat()
    if (webSocket && webSocket.readyState !== WebSocket.CLOSED && webSocket.readyState !== WebSocket.CLOSING) {
      webSocket.close()
      return
    }

    if (socketTask && typeof socketTask.close === 'function') {
      socketTask.close({})
      return
    }

    scheduleReconnect()
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

function isPong(data: unknown) {
  return String(data).trim().toLowerCase() === 'pong'
}

function handleMessage(data: unknown, onMessage?: (message: ChatRealtimePayload) => void) {
  try {
    const payload = JSON.parse(String(data)) as ChatRealtimePayload
    onMessage?.(payload)
  } catch {
    // ignore malformed payloads
  }
}
