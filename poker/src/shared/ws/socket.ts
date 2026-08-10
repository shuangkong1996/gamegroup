import { TOKEN_KEY } from '@/shared/api/http'

export interface WsMessage<T = unknown> {
  type: string
  data: T
}

type Handler = (data: unknown) => void

/**
 * WebSocket 单例封装：自动心跳、断线重连（指数退避）、消息分发。
 */
class PokerSocket {
  private ws: WebSocket | null = null
  private handlers = new Map<string, Set<Handler>>()
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private attempts = 0
  private roomId: number | null = null
  private manuallyClosed = false

  connect(roomId: number) {
    this.roomId = roomId
    this.manuallyClosed = false
    this.attempts = 0
    this.open()
  }

  private open() {
    const token = localStorage.getItem(TOKEN_KEY) || ''
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${protocol}://${location.host}/api/ws/poker?token=${encodeURIComponent(token)}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.attempts = 0
      this.startHeartbeat()
      this.send({ type: 'JOIN', roomId: this.roomId })
    }

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data) as WsMessage
        this.dispatch(msg.type, msg.data)
      } catch {
        // 忽略无法解析的消息
      }
    }

    this.ws.onclose = () => {
      this.stopHeartbeat()
      if (!this.manuallyClosed) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = () => {
      // onclose 会随后触发
    }
  }

  on<T>(type: string, handler: (data: T) => void) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, new Set())
    }
    this.handlers.get(type)!.add(handler as Handler)
  }

  off<T>(type: string, handler: (data: T) => void) {
    this.handlers.get(type)?.delete(handler as Handler)
  }

  send(msg: object) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg))
    }
  }

  disconnect() {
    this.manuallyClosed = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }

  get connected() {
    return this.ws?.readyState === WebSocket.OPEN
  }

  private dispatch(type: string, data: unknown) {
    this.handlers.get(type)?.forEach((handler) => handler(data))
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'HEARTBEAT' })
      }
    }, 15000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return
    const delay = Math.min(1000 * 2 ** this.attempts, 15000)
    this.attempts++
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      if (!this.manuallyClosed && this.roomId != null) {
        this.open()
      }
    }, delay)
  }
}

export const pokerSocket = new PokerSocket()
