/**
 * WebSocket 通知客户端（用户端）
 */
class NotificationWebSocket {
  constructor() {
    this.ws = null
    this.currentUserId = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.reconnectCount = 0
    this.maxReconnectCount = 5
    this.reconnectInterval = 3000
    this.heartbeatInterval = 30000
    this.messageHandlers = []
    this.isManualClose = false
  }

  connect(userId) {
    if (!userId) {
      console.warn('userId 为空，无法建立 WebSocket 连接')
      return
    }

    // 已经是同一个用户并且连接还活着：不重复 close/connect，避免频繁断线
    if (this.ws && this.currentUserId === userId && this.ws.readyState === WebSocket.OPEN) {
      return
    }

    this.isManualClose = false

    // userId 变化或旧连接异常时才关闭重建
    if (this.ws) {
      this.close()
    }

    this.currentUserId = userId

    const wsUrl = this.getWebSocketUrl(userId)
    console.log('WebSocket 连接中...', wsUrl)

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('✅ WebSocket 连接成功')
        this.reconnectCount = 0
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        if (event.data === 'pong') return

        try {
          const data = JSON.parse(event.data)
          this.messageHandlers.forEach(handler => {
            try {
              handler(data)
            } catch (e) {
              console.error('消息处理器执行失败:', e)
            }
          })
        } catch (e) {
          console.error('解析 WebSocket 消息失败:', e, event.data)
        }
      }

      this.ws.onclose = () => {
        console.log('❌ WebSocket 连接关闭')
        this.stopHeartbeat()
        if (!this.isManualClose) {
          this.reconnect(userId)
        }
      }

      this.ws.onerror = (error) => {
        console.error('❌ WebSocket 连接错误:', error)
      }
    } catch (e) {
      console.error('WebSocket 初始化失败:', e)
      this.reconnect(userId)
    }

  }

  getWebSocketUrl(userId) {
    // 用户端开发环境端口为 3001（Vite 代理 /api 到 8001）
    // 这里为了保持一致，也通过 3001 走代理（proxy 需要 ws: true）
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname

    if (import.meta.env.DEV) {
      return `${protocol}//${host}:3001/api/ws/notification?userId=${userId}`
    }

    return `${protocol}//${host}/api/ws/notification?userId=${userId}`
  }

  reconnect(userId) {
    if (this.isManualClose) return

    if (this.reconnectCount >= this.maxReconnectCount) {
      console.error('❌ WebSocket 重连失败，已达到最大重连次数')
      return
    }

    this.reconnectCount++
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => this.connect(userId), this.reconnectInterval)
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, this.heartbeatInterval)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  close() {
    this.isManualClose = true
    this.stopHeartbeat()
    this.currentUserId = null

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  onMessage(handler) {
    if (typeof handler === 'function') {
      this.messageHandlers.push(handler)
    }
  }

  offMessage(handler) {
    const idx = this.messageHandlers.indexOf(handler)
    if (idx >= 0) this.messageHandlers.splice(idx, 1)
  }

  clearMessageHandlers() {
    this.messageHandlers = []
  }
}

const notificationWS = new NotificationWebSocket()
export default notificationWS
