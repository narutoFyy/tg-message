import type { ChatRealtimePayload } from '@/types'
import { buildChatSocketUrl } from '@/utils/api'

const activeSockets = new Set<UniApp.SocketTask>()

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
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  const managedSocket = {
    close(options?: Parameters<UniApp.SocketTask['close']>[0]) {
      closedByUser = true
      clearReconnectTimer()
      if (socketTask) {
        activeSockets.delete(managedSocket as UniApp.SocketTask)
        socketTask.close(options || {})
      }
    }
  } as UniApp.SocketTask

  const connect = () => {
    clearReconnectTimer()
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

    // 检查必要的方法是否存在
    if (typeof socketTask.onOpen !== 'function') {
      console.error('socketTask 缺少 onOpen 方法')
      options.onError?.()
      scheduleReconnect()
      return
    }

    socketTask.onOpen(() => {
      attempt = 0
      options.onOpen?.()
    })

    socketTask.onMessage((event: { data: string }) => {
      try {
        const payload = JSON.parse(event.data as string) as ChatRealtimePayload
        onMessage(payload)
      } catch {
        // ignore malformed payloads
      }
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

  return managedSocket
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
