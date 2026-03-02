/**
 * 浏览器桌面通知工具
 */

/**
 * 请求通知权限
 */
export function requestNotificationPermission() {
  if (!('Notification' in window)) {
    console.warn('浏览器不支持桌面通知')
    return Promise.resolve('denied')
  }

  if (Notification.permission === 'granted') {
    return Promise.resolve('granted')
  }

  if (Notification.permission !== 'denied') {
    return Notification.requestPermission()
  }

  return Promise.resolve('denied')
}

/**
 * 显示桌面通知
 * @param {string} title - 标题
 * @param {object} options - 选项
 */
export function showDesktopNotification(title, options = {}) {
  if (!('Notification' in window)) {
    console.warn('浏览器不支持桌面通知')
    return null
  }

  if (Notification.permission !== 'granted') {
    console.warn('未授予通知权限')
    return null
  }

  try {
    const notification = new Notification(title, {
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      ...options
    })

    // 点击通知时的处理
    notification.onclick = function(event) {
      event.preventDefault()
      window.focus()
      notification.close()
      
      // 如果有自定义点击处理器，执行它
      if (options.onClick) {
        options.onClick()
      }
    }

    // 3秒后自动关闭
    setTimeout(() => {
      notification.close()
    }, 5000)

    return notification
  } catch (error) {
    console.error('显示桌面通知失败:', error)
    return null
  }
}

/**
 * 播放通知提示音
 */
export function playNotificationSound() {
  try {
    // 创建音频上下文
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)

    // 设置音调
    oscillator.frequency.value = 800
    oscillator.type = 'sine'

    // 设置音量渐变
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5)

    // 播放
    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + 0.5)
  } catch (error) {
    console.error('播放提示音失败:', error)
  }
}

/**
 * 检查通知权限状态
 */
export function getNotificationPermission() {
  if (!('Notification' in window)) {
    return 'unsupported'
  }
  return Notification.permission
}
