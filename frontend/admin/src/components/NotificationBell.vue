<template>
  <div class="notification-bell">
    <el-popover
      placement="bottom"
      :width="400"
      trigger="click"
      v-model:visible="popoverVisible"
    >
      <template #reference>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-button circle @click="handleBellClick" :class="{ 'bell-shake': hasNewNotification }">
            <el-icon :size="20"><Bell /></el-icon>
          </el-button>
        </el-badge>
      </template>

      <!-- 通知弹窗内容 -->
      <div class="notification-popover">
        <div class="popover-header">
          <span class="title">通知消息</span>
          <div class="header-actions">
            <el-tooltip :content="desktopNotificationEnabled ? '关闭桌面通知' : '开启桌面通知'">
              <el-button 
                :type="desktopNotificationEnabled ? 'primary' : ''" 
                :icon="Bell" 
                size="small" 
                circle
                @click.stop="toggleDesktopNotification"
              />
            </el-tooltip>
            <el-link type="primary" @click="goToNotificationCenter">查看全部</el-link>
          </div>
        </div>

        <el-divider style="margin: 12px 0" />

        <!-- 通知列表 -->
        <div class="notification-list" v-loading="loading">
          <div 
            v-if="notificationList.length === 0" 
            class="empty"
          >
            <el-empty description="暂无通知" :image-size="80" />
          </div>

          <div 
            v-else
            v-for="item in notificationList" 
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0 }"
            @click="handleNotificationClick(item)"
          >
            <div class="item-header">
              <el-tag :type="getTypeTag(item.type)" size="small">
                {{ item.typeName }}
              </el-tag>
              <span class="time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="item-title">{{ item.title }}</div>
            <div class="item-content">{{ item.content }}</div>
          </div>
        </div>

        <el-divider style="margin: 12px 0" />

        <!-- 底部操作 -->
        <div class="popover-footer">
          <el-button 
            size="small" 
            @click="handleMarkAllRead"
            :disabled="unreadCount === 0"
          >
            全部标记为已读
          </el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import notificationWS from '@/utils/websocket'
import { 
  requestNotificationPermission, 
  showDesktopNotification, 
  playNotificationSound,
  getNotificationPermission 
} from '@/utils/notification'
import { 
  getAdminNotifications, 
  getUnreadCount, 
  markAsRead, 
  markAllAsRead 
} from '@/api/admin-notification'

const router = useRouter()

// 响应式数据
const popoverVisible = ref(false)
const loading = ref(false)
const notificationList = ref([])
const hasNewNotification = ref(false)
const desktopNotificationEnabled = ref(false)
const unreadCount = ref(0)
let timer = null

// 获取未读数量
const getUnread = async () => {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      unreadCount.value = res.data
    }
  } catch (error) {
    console.error('获取未读数量失败', error)
  }
}

// 获取最新通知列表（只显示前5条）
const getLatestNotifications = async () => {
  loading.value = true
  try {
    console.log('开始获取管理端通知列表...')
    const res = await getAdminNotifications({
      page: 1,
      pageSize: 5,
      onlyUnread: null
    })
    console.log('管理端通知列表响应:', res)
    if (res.code === 200) {
      notificationList.value = res.data.records
      console.log('通知列表数据:', notificationList.value)
    } else {
      console.error('响应状态码不是200:', res.code, res.msg)
    }
  } catch (error) {
    console.error('获取通知列表失败', error)
    ElMessage.error('获取通知列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 点击铃铛
const handleBellClick = () => {
  console.log('点击铃铛, popoverVisible:', popoverVisible.value)
  // 每次点击都获取最新通知
  getLatestNotifications()
  // 重置新通知动画
  hasNewNotification.value = false
}

// 点击通知项
const handleNotificationClick = async (item) => {
  // 如果未读，标记为已读
  if (item.isRead === 0) {
    try {
      await markAsRead(item.id)
      item.isRead = 1
      getUnread()
    } catch (error) {
      console.error('标记已读失败', error)
    }
  }
  
  // 关闭弹窗
  popoverVisible.value = false
  
  // 根据通知类型跳转到相应页面
  switch (item.type) {
    case 1:  // 新预约
      router.push('/reservation/list')
      break
    case 2:  // 退款申请
      router.push('/reservation/refund')
      break
    case 3:  // 用户评价
      router.push('/reservation/list')
      break
    case 4:  // 系统通知
      router.push('/notification')
      break
    default:
      // 如果有bizType，按bizType跳转
      if (item.bizType === 'reservation') {
        router.push('/reservation/list')
      } else if (item.bizType === 'refund') {
        router.push('/reservation/refund')
      } else if (item.bizType === 'coupon') {
        router.push('/coupon/list')
      } else {
        router.push('/notification')
      }
  }
}

// 全部标记为已读
const handleMarkAllRead = async () => {
  try {
    const res = await markAllAsRead()
    if (res.code === 200) {
      ElMessage.success('操作成功')
      notificationList.value.forEach(item => {
        item.isRead = 1
      })
      unreadCount.value = 0
      popoverVisible.value = false
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 跳转到通知中心
const goToNotificationCenter = () => {
  popoverVisible.value = false
  router.push('/notification')
}

// 获取类型标签
const getTypeTag = (type) => {
  const typeMap = {
    1: 'success',  // 预约成功
    2: 'warning',  // 预约提醒
    3: 'danger',   // 优惠券到期
    4: 'info',     // 系统公告
    10: 'warning'  // 新预约通知（管理端专用）
  }
  return typeMap[type] || 'info'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 172800000) return '昨天'
  return time.substring(5, 10)
}

// WebSocket 消息处理器
const handleWebSocketMessage = (data) => {
  console.log('收到新通知:', data)
  
  // 更新未读数量
  unreadCount.value++
  
  // 触发铃铛动画
  hasNewNotification.value = true
  setTimeout(() => {
    hasNewNotification.value = false
  }, 3000)
  
  // 播放提示音
  playNotificationSound()
  
  // 显示页面内通知
  ElNotification({
    title: data.title,
    message: data.content,
    type: getNotificationTypeByBizType(data.type),
    duration: 5000,
    onClick: () => {
      handleNotificationClickByData(data)
    }
  })
  
  // 如果启用了桌面通知，显示桌面通知
  if (desktopNotificationEnabled.value) {
    showDesktopNotification(data.title, {
      body: data.content,
      tag: `notification-${data.id}`,
      onClick: () => {
        handleNotificationClickByData(data)
      }
    })
  }
  
  // 刷新通知列表
  if (popoverVisible.value) {
    getLatestNotifications()
  }
}

// 根据通知数据跳转
const handleNotificationClickByData = (data) => {
  switch (data.bizType) {
    case 'reservation':
      router.push('/reservation/list')
      break
    case 'refund':
      router.push('/reservation/refund')
      break
    case 'coupon':
      router.push('/coupon/list')
      break
    default:
      router.push('/notification')
  }
}

// 获取通知类型（用于页面通知）
const getNotificationTypeByBizType = (type) => {
  const typeMap = {
    1: 'success',  // 预约成功
    2: 'warning',  // 预约提醒
    3: 'warning',  // 优惠券到期
    4: 'info',     // 系统公告
    10: 'warning'  // 新预约通知（管理端专用）
  }
  return typeMap[type] || 'info'
}

// 开启/关闭桌面通知
const toggleDesktopNotification = async () => {
  if (!desktopNotificationEnabled.value) {
    const permission = await requestNotificationPermission()
    if (permission === 'granted') {
      desktopNotificationEnabled.value = true
      ElMessage.success('桌面通知已开启')
      localStorage.setItem('desktopNotificationEnabled', 'true')
    } else {
      ElMessage.warning('请在浏览器设置中允许通知权限')
    }
  } else {
    desktopNotificationEnabled.value = false
    ElMessage.info('桌面通知已关闭')
    localStorage.setItem('desktopNotificationEnabled', 'false')
  }
}

// 初始化
onMounted(() => {
  // 只有在已登录状态下才初始化通知功能
  const token = localStorage.getItem('admin-token')
  if (!token) {
    console.log('未登录，跳过通知功能初始化')
    return
  }
  
  getUnread()
  
  // 检查桌面通知权限
  const savedPermission = localStorage.getItem('desktopNotificationEnabled')
  if (savedPermission === 'true' && getNotificationPermission() === 'granted') {
    desktopNotificationEnabled.value = true
  }
  
  // WebSocket 连接由全局 layout 负责，这里只注册消息处理器即可
  notificationWS.onMessage(handleWebSocketMessage)
  
  // 每30秒刷新一次未读数量
  timer = setInterval(() => {
    getUnread()
  }, 30000)
})

// 清理定时器和消息回调（不要关闭 WebSocket 连接，避免路由切换导致频繁断开/重连）
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }

  notificationWS.offMessage(handleWebSocketMessage)
})

</script>

<style scoped>
.notification-bell {
  display: inline-block;
}

.notification-popover {
  max-height: 500px;
  overflow: hidden;
}

.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.popover-header .title {
  font-size: 16px;
  font-weight: bold;
}

.notification-list {
  max-height: 350px;
  overflow-y: auto;
}

.notification-item {
  padding: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.3s;
  margin-bottom: 8px;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.notification-item.unread:hover {
  background-color: #d9ecff;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.item-header .time {
  font-size: 12px;
  color: #909399;
}

.item-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 4px;
  color: #303133;
}

.item-content {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  padding: 20px;
  text-align: center;
}

.popover-footer {
  display: flex;
  justify-content: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 铃铛抖动动画 */
.bell-shake {
  animation: shake 0.5s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% {
    transform: rotate(0deg);
  }
  10%, 30%, 50%, 70%, 90% {
    transform: rotate(-10deg);
  }
  20%, 40%, 60%, 80% {
    transform: rotate(10deg);
  }
}
</style>
