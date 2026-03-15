/**
 * WebSocket 通知客户端
 */
class NotificationWebSocket {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.reconnectCount = 0
    this.maxReconnectCount = 5
    this.reconnectInterval = 3000 // 3秒重连一次
    this.heartbeatInterval = 30000 // 30秒心跳一次
    this.messageHandlers = []
    this.isManualClose = false
  }

  /**
   * 连接 WebSocket
   */
  connect(userId) {
    if (!userId) {
      console.warn('userId 为空，无法建立 WebSocket 连接')
      return
    }

    this.isManualClose = false
    
    // 如果已经连接，先关闭
    if (this.ws) {
      this.close()
    }

    try {
      // 获取 WebSocket URL
      const wsUrl = this.getWebSocketUrl(userId)
      console.log('WebSocket 连接中...', wsUrl)
      
      this.ws = new WebSocket(wsUrl)

      // 连接建立
      this.ws.onopen = () => {
        console.log('✅ WebSocket 连接成功')
        this.reconnectCount = 0
        this.startHeartbeat()
      }

      // 收到消息
      this.ws.onmessage = (event) => {
        // 处理心跳响应
        if (event.data === 'pong') {
          return
        }
        
        try {
          const data = JSON.parse(event.data)
          console.log('📨 收到通知:', data)
          
          // 调用所有消息处理器
          this.messageHandlers.forEach(handler => {
            try {
              handler(data)
            } catch (error) {
              console.error('消息处理器执行失败:', error)
            }
          })
        } catch (error) {
          console.error('解析 WebSocket 消息失败:', error, event.data)
        }
      }

      // 连接关闭
      this.ws.onclose = () => {
        console.log('❌ WebSocket 连接关闭')
        this.stopHeartbeat()
        
        // 如果不是手动关闭，尝试重连
        if (!this.isManualClose) {
          this.reconnect(userId)
        }
      }

      // 连接错误
      this.ws.onerror = (error) => {
        console.error('❌ WebSocket 连接错误:', error)
      }
    } catch (error) {
      console.error('WebSocket 初始化失败:', error)
      this.reconnect(userId)
    }
  }

  /**
   * 获取 WebSocket URL
   */
  getWebSocketUrl(userId) {
    // 使用当前页面的 host（含端口），开发环境下 Vite 代理会将 /api/ws 转发到后端
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    return `${protocol}//${host}/api/ws/admin-notification?adminId=${userId}`
  }

  /**
   * 重连
   */
  reconnect(userId) {
    if (this.isManualClose) {
      return
    }

    if (this.reconnectCount >= this.maxReconnectCount) {
      console.error('❌ WebSocket 重连失败，已达到最大重连次数')
      return
    }

    this.reconnectCount++
    console.log(`🔄 WebSocket 重连中... (${this.reconnectCount}/${this.maxReconnectCount})`)

    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      this.connect(userId)
    }, this.reconnectInterval)
  }

  /**
   * 启动心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, this.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 关闭连接
   */
  close() {
    this.isManualClose = true
    this.stopHeartbeat()
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 添加消息处理器
   */
  onMessage(handler) {
    if (typeof handler === 'function') {
      this.messageHandlers.push(handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(handler) {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
    }
  }

  /**
   * 清空所有消息处理器
   */
  clearMessageHandlers() {
    this.messageHandlers = []
  }
}

// 创建单例
const notificationWS = new NotificationWebSocket()

export default notificationWS
