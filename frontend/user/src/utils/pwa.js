// PWA工具函数

/**
 * 注册Service Worker
 */
export function registerServiceWorker() {
  // 开发环境不注册 Service Worker，避免频繁刷新
  if (import.meta.env.DEV) {
    console.log('开发环境：已禁用 Service Worker')
    return
  }
  
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker
        .register('/sw.js')
        .then((registration) => {
          console.log('Service Worker 注册成功:', registration.scope)
          
          // 检查更新
          registration.addEventListener('updatefound', () => {
            const newWorker = registration.installing
            console.log('发现新的 Service Worker')
            
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                console.log('新版本可用，请刷新页面')
                showUpdateNotification()
              }
            })
          })
        })
        .catch((error) => {
          console.error('Service Worker 注册失败:', error)
        })
    })
  }
}

/**
 * 显示更新通知
 */
function showUpdateNotification() {
  if (window.ElMessage) {
    window.ElMessage({
      message: '发现新版本，点击刷新以获取最新内容',
      type: 'info',
      duration: 0,
      showClose: true
    })
  }
}

/**
 * 检查是否支持PWA
 */
export function isPWASupported() {
  return 'serviceWorker' in navigator && 'PushManager' in window
}

/**
 * 检查是否已安装PWA
 */
export function isPWAInstalled() {
  return window.matchMedia('(display-mode: standalone)').matches ||
         window.navigator.standalone === true
}

/**
 * 显示安装提示
 */
export function showInstallPrompt() {
  // 开发环境不显示安装提示
  if (import.meta.env.DEV) {
    return
  }
  
  let deferredPrompt = null

  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault()
    deferredPrompt = e
    showCustomInstallPrompt(deferredPrompt)
  })

  window.addEventListener('appinstalled', () => {
    console.log('PWA 已安装')
    if (window.ElMessage) {
      window.ElMessage.success('应用已成功安装到桌面')
    }
  })
}

/**
 * 显示自定义安装提示
 */
function showCustomInstallPrompt(deferredPrompt) {
  if (window.ElMessageBox) {
    window.ElMessageBox.confirm(
      '将应用添加到主屏幕，获得更好的使用体验',
      '安装应用',
      {
        confirmButtonText: '安装',
        cancelButtonText: '稍后',
        type: 'info'
      }
    ).then(() => {
      if (deferredPrompt) {
        deferredPrompt.prompt()
      }
    }).catch(() => {
      console.log('用户取消了安装')
    })
  }
}

/**
 * 请求通知权限
 */
export async function requestNotificationPermission() {
  if (!('Notification' in window)) {
    return false
  }

  if (Notification.permission === 'granted') {
    return true
  }

  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission()
    return permission === 'granted'
  }

  return false
}

/**
 * 显示通知
 */
export function showNotification(title, options = {}) {
  if (Notification.permission === 'granted') {
    const notification = new Notification(title, {
      icon: '/icon-192x192.png',
      badge: '/icon-72x72.png',
      ...options
    })

    notification.onclick = () => {
      window.focus()
      notification.close()
    }

    return notification
  }
}
