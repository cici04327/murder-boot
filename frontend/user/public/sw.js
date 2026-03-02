// Service Worker for PWA
const CACHE_NAME = 'murder-mystery-v1.0.0'
const urlsToCache = [
  '/',
  '/index.html',
  '/default-avatar.jpg',
  '/default-script.jpg'
]

// 安装事件
self.addEventListener('install', (event) => {
  console.log('Service Worker 安装中...')
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('打开缓存')
        return cache.addAll(urlsToCache)
      })
      .then(() => {
        console.log('Service Worker 安装成功')
        return self.skipWaiting()
      })
      .catch((err) => {
        console.error('Service Worker 安装失败:', err)
      })
  )
})

// 激活事件
self.addEventListener('activate', (event) => {
  console.log('Service Worker 激活中...')
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('删除旧缓存:', cacheName)
            return caches.delete(cacheName)
          }
        })
      )
    }).then(() => {
      console.log('Service Worker 激活成功')
      return self.clients.claim()
    })
  )
})

// 拦截请求
self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  // 不拦截 WebSocket 连接（Vite HMR）
  if (request.url.includes('ws://') || request.url.includes('wss://')) {
    return
  }

  // 不拦截 WebSocket 升级请求
  if (request.headers.get('upgrade') === 'websocket') {
    return
  }

  // 只缓存同源请求
  if (url.origin !== location.origin) {
    return
  }

  // API 请求使用网络优先策略
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          // 克隆响应，因为响应流只能使用一次
          const responseToCache = response.clone()
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(request, responseToCache)
          })
          return response
        })
        .catch(() => {
          // 网络失败时从缓存获取
          return caches.match(request)
        })
    )
    return
  }

  // 静态资源使用缓存优先策略
  event.respondWith(
    caches.match(request)
      .then((response) => {
        if (response) {
          return response
        }
        return fetch(request).then((response) => {
          // 检查是否是有效响应
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response
          }

          const responseToCache = response.clone()
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(request, responseToCache)
          })

          return response
        })
      })
      .catch(() => {
        // 如果请求是页面导航，返回离线页面
        if (request.destination === 'document') {
          return caches.match('/index.html')
        }
      })
  )
})

// 推送通知
self.addEventListener('push', (event) => {
  console.log('收到推送通知:', event)
  
  const options = {
    body: event.data ? event.data.text() : '您有新的消息',
    icon: '/icon-192x192.png',
    badge: '/icon-72x72.png',
    vibrate: [200, 100, 200],
    data: {
      dateOfArrival: Date.now(),
      primaryKey: 1
    },
    actions: [
      {
        action: 'explore',
        title: '查看详情',
        icon: '/icon-96x96.png'
      },
      {
        action: 'close',
        title: '关闭',
        icon: '/icon-96x96.png'
      }
    ]
  }

  event.waitUntil(
    self.registration.showNotification('剧本杀平台', options)
  )
})

// 通知点击事件
self.addEventListener('notificationclick', (event) => {
  console.log('通知被点击:', event)
  event.notification.close()

  if (event.action === 'explore') {
    event.waitUntil(
      clients.openWindow('/')
    )
  }
})

// 后台同步
self.addEventListener('sync', (event) => {
  console.log('后台同步:', event)
  
  if (event.tag === 'sync-data') {
    event.waitUntil(syncData())
  }
})

// 同步数据函数
async function syncData() {
  try {
    // 这里可以实现数据同步逻辑
    console.log('开始同步数据...')
    // 例如：上传离线时保存的数据
    const cache = await caches.open(CACHE_NAME)
    // 实现具体的同步逻辑
    console.log('数据同步完成')
  } catch (error) {
    console.error('数据同步失败:', error)
  }
}

// 消息处理
self.addEventListener('message', (event) => {
  console.log('收到消息:', event.data)
  
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
  
  if (event.data && event.data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys().then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            return caches.delete(cacheName)
          })
        )
      })
    )
  }
})
