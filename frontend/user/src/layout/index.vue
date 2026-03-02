<template>
  <div class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">🎭</span>
          <span>剧本杀</span>
        </div>
        
        <!-- 导航链接 -->
        <nav class="header-nav">
          <a href="/" @click.prevent="router.push('/home')" :class="{ active: activeMenu === '/home' }">首页</a>
          <a href="/script" @click.prevent="router.push('/script')" :class="{ active: activeMenu.startsWith('/script') }">剧本列表</a>
          <a href="/store" @click.prevent="router.push('/store')" :class="{ active: activeMenu.startsWith('/store') }">门店列表</a>
          <a href="/about" @click.prevent="router.push('/about')" :class="{ active: activeMenu === '/about' }">关于我们</a>
          <a href="/contact" @click.prevent="router.push('/contact')" :class="{ active: activeMenu === '/contact' }">联系我们</a>
        </nav>
        
        <!-- 全局搜索 -->
        <GlobalSearch />
        
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <!-- 消息通知 -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button circle @click="router.push('/user/notifications')">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            
            <!-- 用户菜单 -->
            <el-dropdown @command="handleUserCommand">
              <div class="user-info">
                <el-avatar :src="userStore.avatar" :size="36" />
                <span class="username">{{ userStore.nickname }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="vip">
                    <el-icon><Medal /></el-icon>
                    VIP会员
                  </el-dropdown-item>
                  <el-dropdown-item command="reservations">
                    <el-icon><Calendar /></el-icon>
                    我的预约
                  </el-dropdown-item>
                  <el-dropdown-item command="coupons">
                    <el-icon><Ticket /></el-icon>
                    我的优惠券
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon>
                    我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item command="history">
                    <el-icon><Clock /></el-icon>
                    浏览历史
                  </el-dropdown-item>
                  <el-dropdown-item command="points">
                    <el-icon><Star /></el-icon>
                    我的积分
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          
          <template v-else>
            <a href="/login" @click.prevent="router.push('/login')" class="btn-login">登录</a>
            <a href="/register" @click.prevent="router.push('/register')" class="btn-register">注册</a>
          </template>
        </div>
      </div>
    </el-header>
    
    <!-- 主体内容 -->
    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
    
    <!-- 底部 -->
    <el-footer class="footer">
      <div class="footer-content">
        <div class="footer-links">
          <a @click="router.push('/about')">关于我们</a>
          <span class="divider">|</span>
          <a @click="router.push('/contact')">联系我们</a>
          <span class="divider">|</span>
          <a @click="router.push('/help')">帮助中心</a>
          <span class="divider">|</span>
          <a @click="router.push('/agreement')">用户协议</a>
        </div>
        <div class="copyright">
          © 2024 剧本杀在线预约平台. All Rights Reserved.
        </div>
      </div>
    </el-footer>
    
    <!-- AI客服 -->
    <AICustomerService />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox, ElNotification } from 'element-plus'
import { Location, Bell, User, Calendar, Ticket, Star, SwitchButton, Medal, Clock } from '@element-plus/icons-vue'
import GlobalSearch from '@/components/GlobalSearch.vue'
import AICustomerService from '@/components/AICustomerService.vue'
import { getUnreadCount } from '@/api/user'
import notificationWS from '@/utils/websocket'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const unreadCount = ref(0)

const refreshUnreadCount = async () => {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await getUnreadCount()
    // request 拦截器保证成功时返回 res
    unreadCount.value = Number(res.data || 0)
  } catch (e) {
    // 读取失败不打断 UI
    console.warn('获取未读通知数量失败:', e)
  }
}

const getNotificationRoute = (data) => {
  // wsMessage 字段：bizType/bizId/type/title/content...
  const bizType = data?.bizType
  const bizId = data?.bizId

  // 预约相关：跳转预约详情
  if (bizType === 'reservation' && bizId) {
    return `/reservation/detail/${bizId}`
  }

  // 其他类型：统一跳到通知列表
  return '/user/notifications'
}

const handleWsMessage = (data) => {
  // 显示更完整：title + content，并支持点击跳转
  if (data?.title || data?.content) {
    const routePath = getNotificationRoute(data)
    ElNotification({
      title: data?.title || '新通知',
      message: data?.content || '',
      type: 'success',
      duration: 6000,
      position: 'top-right',
      onClick: () => {
        router.push(routePath)
      }
    })
  }

  refreshUnreadCount()
}

const connectWsIfNeeded = () => {
  const uid = userStore.userId
  if (!userStore.isLoggedIn || !uid) {
    notificationWS.close()
    return
  }
  // 不要频繁 clear/重绑 handler，否则 watch 触发时会导致重复 connect 引起断线
  notificationWS.onMessage(handleWsMessage)
  notificationWS.connect(uid)
}

onMounted(() => {
  refreshUnreadCount()
  connectWsIfNeeded()
})

watch(
  () => [userStore.isLoggedIn, userStore.userId],
  () => {
    refreshUnreadCount()
    connectWsIfNeeded()
  }
)

// 注意：layout 在路由切换时可能会被卸载重建，
// 这里不要主动 close WebSocket，否则会出现“频繁断开/重连”。
// WebSocket 只应在用户登出时关闭（watch 已处理）。
onBeforeUnmount(() => {
  notificationWS.offMessage(handleWsMessage)
})

const activeMenu = computed(() => {
  return route.path
})

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'vip':
      router.push('/vip')
      break
    case 'reservations':
      router.push('/user/reservations')
      break
    case 'coupons':
      router.push('/user/coupons')
      break
    case 'favorites':
      router.push('/user/favorites')
      break
    case 'history':
      router.push('/user/history')
      break
    case 'points':
      router.push('/user/points')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
        router.push('/home')
      }).catch(() => {})
      break
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  background-attachment: fixed;
}

.header {
  background: rgba(26, 26, 26, 0.95); /* 深黑色背景 - 与0.html一致 */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
  border-bottom: none;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  color: #FFD700; /* 金色 - 与0.html一致 */
  cursor: pointer;
  user-select: none;
  transition: color 0.3s;
}

.logo:hover {
  color: #FFC107;
}

/* Logo图标 */
.logo-icon {
  font-size: 24px;
}

/* 导航链接 - 与0.html一致 */
.header-nav {
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
  margin: 0 40px;
}

.header-nav a {
  color: white;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
  white-space: nowrap;
}

.header-nav a:hover {
  color: #FFD700;
}

.header-nav a.active {
  color: #FFD700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 200px;
  flex-shrink: 0;
  justify-content: flex-end;
}

/* 登录按钮 - 金色边框 */
.btn-login {
  padding: 8px 16px;
  border: 1px solid #FFD700;
  color: #FFD700;
  border-radius: 4px;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-login:hover {
  background: rgba(255, 215, 0, 0.1);
}

/* 注册按钮 - 金色背景 */
.btn-register {
  padding: 8px 16px;
  background: #FFD700;
  color: #1A1A1A;
  border-radius: 4px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-register:hover {
  background: rgba(255, 215, 0, 0.9);
}

/* 图标颜色 */
.header-right :deep(.el-icon) {
  color: white;
}

.header-right :deep(.el-button) {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.header-right :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.notification-badge {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  font-size: 14px;
  color: white;
}

.main-content {
  flex: 1;
  padding: 20px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.footer {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #fff;
  padding: 30px 0;
  text-align: center;
  border-top: 1px solid rgba(139, 0, 0, 0.3);
  box-shadow: 0 -2px 20px rgba(0, 0, 0, 0.3);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-links {
  margin-bottom: 15px;
}

.footer-links a {
  color: #fff;
  text-decoration: none;
  margin: 0 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.footer-links a:hover {
  color: #ff6b6b;
  text-decoration: underline;
}

.divider {
  margin: 0 5px;
  opacity: 0.5;
}

.copyright {
  font-size: 14px;
  opacity: 0.7;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<!-- 非 scoped 样式，覆盖 Element Plus 下拉菜单的默认白色背景 -->
<style>
/* 下拉菜单深色主题 */
.el-dropdown-menu {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.3) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4) !important;
}

.el-dropdown-menu__item {
  color: rgba(255, 255, 255, 0.9) !important;
}

.el-dropdown-menu__item:hover,
.el-dropdown-menu__item:focus {
  background: rgba(139, 0, 0, 0.3) !important;
  color: #fff !important;
}

.el-dropdown-menu__item--divided {
  border-top-color: rgba(139, 0, 0, 0.2) !important;
}

.el-dropdown-menu__item .el-icon {
  color: rgba(255, 255, 255, 0.7) !important;
}

.el-dropdown-menu__item:hover .el-icon,
.el-dropdown-menu__item:focus .el-icon {
  color: #fff !important;
}

/* 下拉菜单箭头 */
.el-popper.is-light .el-popper__arrow::before {
  background: rgba(26, 26, 46, 0.98) !important;
  border-color: rgba(139, 0, 0, 0.3) !important;
}
</style>
