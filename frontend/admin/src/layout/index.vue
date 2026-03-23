<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h2>剧本杀管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-sub-menu v-if="route.children && route.children.length > 1" :index="route.path">
            <template #title>
              <el-icon v-if="route.meta?.icon || route.children?.[0]?.meta?.icon">
                <component :is="route.meta?.icon || route.children?.[0]?.meta?.icon" />
              </el-icon>
              <span>{{ route.meta?.title || route.children?.[0]?.meta?.title }}</span>
            </template>
            <template v-for="child in route.children.filter(c => !c.meta?.parent)" :key="child.path">
              <el-sub-menu v-if="route.children.some(sc => sc.meta?.parent === child.path)" :index="route.path + '/' + child.path">
                <template #title>
                  <el-icon><component :is="child.meta.icon" /></el-icon>
                  <span>{{ child.meta.title }}</span>
                </template>
                <el-menu-item :index="route.path + '/' + child.path">
                  <el-icon><component :is="child.meta.icon" /></el-icon>
                  <span>{{ child.meta.title }}</span>
                </el-menu-item>
                <el-menu-item v-for="sub in route.children.filter(sc => sc.meta?.parent === child.path)" :key="sub.path" :index="route.path + '/' + sub.path">
                  <el-icon><component :is="sub.meta.icon || 'Plus'" /></el-icon>
                  <span>{{ sub.meta.title }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="route.path + '/' + child.path">
                <el-icon><component :is="child.meta.icon" /></el-icon>
                <span>{{ child.meta.title }}</span>
              </el-menu-item>
            </template>
          </el-sub-menu>
          <el-menu-item v-else :index="route.children && route.children.length === 1 ? (route.path === '/' ? '/' + route.children[0].path : route.path + '/' + route.children[0].path) : route.path">
            <el-icon><component :is="route.meta?.icon || route.children?.[0]?.meta?.icon" /></el-icon>
            <span>{{ route.meta?.title || route.children?.[0]?.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <NotificationBell v-if="showNotificationBell" />

          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              <span>{{ adminDisplayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import NotificationBell from '@/components/NotificationBell.vue'
import notificationWS from '@/utils/websocket'
import { playNotificationSound } from '@/utils/notification'
import { canViewNotifications, hasRoutePermission } from '@/utils/permission'

const router = useRouter()
const route = useRoute()

const adminUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('admin-user') || '{}')
  } catch (e) {
    return {}
  }
})

const loginType = computed(() => localStorage.getItem('admin-login-type') || 'admin')
const isStoreLogin = computed(() => loginType.value === 'store')
const isStaffLogin = computed(() => loginType.value === 'staff')
const isSuperAdmin = computed(() => adminUser.value?.role === 'SUPER_ADMIN' && !isStoreLogin.value && !isStaffLogin.value)

const adminDisplayName = computed(() => {
  if (isStoreLogin.value) {
    return localStorage.getItem('admin-store-name') || '门店管理员'
  }
  if (isStaffLogin.value) {
    return adminUser.value?.name || adminUser.value?.loginAccount || '门店员工'
  }
  return adminUser.value?.nickname || adminUser.value?.username || '管理员'
})

const currentStoreName = computed(() => {
  if (isStoreLogin.value) {
    return localStorage.getItem('admin-store-name') || ''
  }
  return ''
})

// ========== 客服WebSocket（全局监听新会话通知）==========
let serviceWs = null
let serviceWsHeartbeat = null

const connectServiceWs = (adminId) => {
  if (!adminId) return
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${location.host}/api/ws/service?adminId=${adminId}&role=admin`
  serviceWs = new WebSocket(wsUrl)

  serviceWs.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_service_session') {
        // 播放提示音
        playNotificationSound()
        // 显示页面内通知
        ElNotification({
          title: '📞 新客服请求',
          message: `用户「${data.userName || '用户'}」发起了人工客服请求`,
          type: 'warning',
          duration: 8000,
          onClick: () => {
            router.push('/service/index')
          }
        })
      }
    } catch (e) { /* ignore */ }
  }

  serviceWs.onclose = () => {
    // 断线后5秒重连
    setTimeout(() => {
      const ui = JSON.parse(localStorage.getItem('admin-user') || '{}')
      if (ui.id && localStorage.getItem('admin-token')) {
        connectServiceWs(ui.id)
      }
    }, 5000)
  }

  // 心跳保活
  clearInterval(serviceWsHeartbeat)
  serviceWsHeartbeat = setInterval(() => {
    if (serviceWs && serviceWs.readyState === WebSocket.OPEN) {
      serviceWs.send('ping')
    }
  }, 30000)
}

onUnmounted(() => {
  clearInterval(serviceWsHeartbeat)
  if (serviceWs) {
    serviceWs.close()
    serviceWs = null
  }
})

onMounted(() => {
  localStorage.removeItem('admin-current-store-id')
  const userInfo = JSON.parse(localStorage.getItem('admin-user') || '{}')
  if (userInfo.id && !isStaffLogin.value) {
    notificationWS.connect(userInfo.id)
    // 同时连接客服WebSocket，监听新客服会话请求
    connectServiceWs(userInfo.id)
  }
})

const currentRole = computed(() => {
  if (isStoreLogin.value) return 'store'
  if (isStaffLogin.value) return 'staff'
  return 'admin'
})

const showNotificationBell = computed(() => {
  if (isStoreLogin.value) return true
  if (isStaffLogin.value) return canViewNotifications()
  return true
})

const hasPermission = (route) => hasRoutePermission(route)

const menuRoutes = computed(() => {
  return router.options.routes
    .filter(r => r.path !== '/login')
    .filter(r => hasPermission(r))
    .map(route => {
      if (route.children && route.children.length > 0) {
        return {
          ...route,
          children: route.children.filter(child => !child.hidden && hasPermission(child))
        }
      }
      return route
    })
    .filter(route => {
      if (route.children && route.children.length === 0) return false
      return true
    })
})

const activeMenu = computed(() => (route.meta && route.meta.activeMenu) ? route.meta.activeMenu : route.path)

const currentTitle = computed(() => route.meta.title || '')

const handleCommand = (command) => {
  if (command === 'logout') {
    notificationWS.close()
    clearInterval(serviceWsHeartbeat)
    if (serviceWs) { serviceWs.close(); serviceWs = null }
    localStorage.removeItem('admin-token')
    localStorage.removeItem('admin-user')
    localStorage.removeItem('admin-current-store-id')
    localStorage.removeItem('admin-login-type')
    localStorage.removeItem('admin-store-id')
    localStorage.removeItem('admin-store-name')
    ElMessage.success('退出成功')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-menu {
  border: none;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 10px;
}

.user-dropdown span {
  margin: 0 5px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
