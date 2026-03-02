<template>
  <div class="user-notifications">
    <el-card class="page-header">
      <div class="header-content">
        <div>
          <h2>消息通知</h2>
          <p class="subtitle">查看您的所有消息通知</p>
        </div>
        <div class="header-actions">
          <el-button @click="handleMarkAllRead" :disabled="unreadCount === 0">
            <el-icon><CircleCheck /></el-icon>
            全部已读
          </el-button>
          <el-button @click="handleClearAll" type="danger" plain>
            <el-icon><Delete /></el-icon>
            清空消息
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 未读消息统计 -->
    <el-card class="unread-stats" v-if="unreadCount > 0">
      <el-alert
        :title="`您有 ${unreadCount} 条未读消息`"
        type="warning"
        :closable="false"
        show-icon
      />
    </el-card>

    <!-- 消息列表 -->
    <el-card class="notification-list-card">
      <template #header>
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="全部消息" name="all">
            <template #label>
              <span>全部消息</span>
              <el-badge v-if="unreadCount > 0" :value="unreadCount" class="tab-badge" />
            </template>
          </el-tab-pane>
          <el-tab-pane label="系统通知" name="system"></el-tab-pane>
          <el-tab-pane label="预约消息" name="reservation"></el-tab-pane>
          <el-tab-pane label="优惠活动" name="promotion"></el-tab-pane>
        </el-tabs>
      </template>

      <div v-loading="loading" class="notification-list">
        <div v-if="notifications.length === 0" class="empty-state">
          <el-empty description="暂无消息"></el-empty>
        </div>

        <div v-else class="notification-items">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ 'unread': notification.isRead === 0 }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon">
              <el-icon :size="24" :color="getNotificationColor(notification.type)">
                <component :is="getNotificationIcon(notification.type)" />
              </el-icon>
            </div>
            <div class="notification-content">
              <div class="notification-header">
                <h4 class="notification-title">{{ notification.title }}</h4>
                <el-tag v-if="notification.isRead === 0" type="danger" size="small">未读</el-tag>
              </div>
              <p class="notification-message">{{ notification.content }}</p>
              <div class="notification-footer">
                <span class="notification-time">{{ formatDateTime(notification.createTime) }}</span>
                <el-tag :type="getTypeTag(notification.type)" size="small">
                  {{ getTypeLabel(notification.type) }}
                </el-tag>
              </div>
            </div>
            <div class="notification-actions">
              <el-button
                v-if="notification.isRead === 0"
                type="primary"
                size="small"
                link
                @click.stop="handleMarkRead(notification.id)"
              >
                标记已读
              </el-button>
              <el-button
                type="danger"
                size="small"
                link
                @click.stop="handleDelete(notification.id)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <el-pagination
          v-if="total > 0"
          class="pagination"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 消息详情对话框 -->
    <el-dialog
      v-model="showDetail"
      :title="currentNotification?.title"
      width="600px"
    >
      <div class="notification-detail">
        <div class="detail-header">
          <el-tag :type="getTypeTag(currentNotification?.type)" size="small">
            {{ getTypeLabel(currentNotification?.type) }}
          </el-tag>
          <span class="detail-time">{{ formatDateTime(currentNotification?.createTime) }}</span>
        </div>
        <div class="detail-content">
          {{ currentNotification?.content }}
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button
          v-if="currentNotification?.isRead === 0"
          type="primary"
          @click="handleMarkRead(currentNotification.id, true)"
        >
          标记已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Delete,
  Bell,
  ChatDotSquare,
  ShoppingCart,
  Tickets,
  Warning,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  getUserNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
  clearAllNotifications,
  getUnreadCount
} from '@/api/user'

// 状态
const loading = ref(false)
const activeTab = ref('all')
// 后端目前不支持按 type 分类筛选，这里在前端做本地分类 + 本地分页
const allNotifications = ref([])
const notifications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)

const TAB_TYPE_MAP = {
  system: [4],
  reservation: [1, 2, 7, 8],
  promotion: [3]
}

const applyFilterAndPagination = () => {
  const list = allNotifications.value || []
  const types = TAB_TYPE_MAP[activeTab.value]
  const filtered = types ? list.filter(n => types.includes(Number(n.type))) : list

  total.value = filtered.length

  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  notifications.value = filtered.slice(start, end)
}

const showDetail = ref(false)
const currentNotification = ref(null)

// 分页处理函数
const handlePageChange = (newPage) => {
  console.log('通知列表页码变化:', newPage)
  currentPage.value = newPage
  applyFilterAndPagination()
}

const handleSizeChange = (newSize) => {
  console.log('通知列表每页大小变化:', newSize)
  pageSize.value = newSize
  currentPage.value = 1
  applyFilterAndPagination()
}

// 加载通知列表
const loadNotifications = async () => {
  loading.value = true
  try {
    console.log('加载通知列表，参数:', { page: currentPage.value, pageSize: pageSize.value, type: activeTab.value })
    // 单体后端 /notification/page 目前只支持 page/pageSize/onlyUnread，不支持 type 分类。
    // 为了让「系统通知/预约消息/优惠活动」三个 Tab 生效：这里一次性取较多记录后在前端做分类+分页。
    const params = {
      page: 1,
      pageSize: 1000
    }
    const res = await getUserNotifications(params)
    // 后端 Result 约定：code=200 表示成功（历史代码也可能用 1）
    if (res.code === 1 || res.code === 200) {
      allNotifications.value = res.data.records || []
      currentPage.value = 1
      applyFilterAndPagination()
      // 后端 PageResult 只包含 total/records，未读数单独从 /notification/unread-count 获取
      try {
        const unreadRes = await getUnreadCount()
        unreadCount.value = Number(unreadRes.data || 0)
      } catch (e) {
        // ignore
      }
    }
  } catch (error) {
    console.error('加载通知失败:', error)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  currentPage.value = 1
  applyFilterAndPagination()
}

// 点击通知
const handleNotificationClick = (notification) => {
  currentNotification.value = notification
  showDetail.value = true
  
  // 如果是未读消息，自动标记为已读
  if (notification.isRead === 0) {
    handleMarkRead(notification.id, false)
  }
}

// 标记已读
const handleMarkRead = async (id, closeDialog = false) => {
  try {
    const res = await markNotificationRead(id)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('已标记为已读')
      loadNotifications()
      if (closeDialog) {
        showDetail.value = false
      }
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 全部已读
const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有消息标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    const res = await markAllNotificationsRead()
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('已全部标记为已读')
      loadNotifications()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('全部已读失败:', error)
    }
  }
}

// 删除通知
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteNotification(id)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('删除成功')
      loadNotifications()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 清空所有消息
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有消息吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await clearAllNotifications()
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('已清空所有消息')
      loadNotifications()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  }
}

// 获取通知图标
const getNotificationIcon = (type) => {
  const iconMap = {
    'system': InfoFilled,
    'reservation': ShoppingCart,
    'promotion': Tickets,
    'warning': Warning,
    'message': ChatDotSquare
  }
  return iconMap[type] || Bell
}

// 获取通知颜色
const getNotificationColor = (type) => {
  const colorMap = {
    'system': '#409EFF',
    'reservation': '#67C23A',
    'promotion': '#E6A23C',
    'warning': '#F56C6C',
    'message': '#909399'
  }
  return colorMap[type] || '#909399'
}

// 获取类型标签样式
const getTypeTag = (type) => {
  const tagMap = {
    'system': 'info',
    'reservation': 'success',
    'promotion': 'warning',
    'warning': 'danger',
    'message': ''
  }
  return tagMap[type] || ''
}

// 获取类型标签文本
const getTypeLabel = (type) => {
  const labelMap = {
    'system': '系统通知',
    'reservation': '预约消息',
    'promotion': '优惠活动',
    'warning': '重要提醒',
    'message': '消息'
  }
  return labelMap[type] || '通知'
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于1天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 初始化
onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.user-notifications {
  padding: 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
}

.page-header :deep(.el-card__body) {
  background: transparent;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #fff;
}

.subtitle {
  margin: 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions .el-button {
  background: rgba(35, 35, 60, 0.8);
  border-color: rgba(139, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.header-actions .el-button:hover {
  background: rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  color: #fff;
}

.header-actions .el-button--danger {
  background: rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
}

.header-actions .el-button--danger:hover {
  background: #8B0000;
  border-color: #8B0000;
}

/* 未读统计 */
.unread-stats {
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
}

.unread-stats :deep(.el-card__body) {
  background: transparent;
}

.unread-stats :deep(.el-alert--warning) {
  background: rgba(139, 0, 0, 0.2);
  border-color: rgba(139, 0, 0, 0.3);
}

.unread-stats :deep(.el-alert__title) {
  color: #ff6b6b;
}

/* 消息列表 */
.notification-list-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
}

.notification-list-card :deep(.el-card__header) {
  padding: 20px 20px 0;
  background: transparent;
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.notification-list-card :deep(.el-card__body) {
  background: transparent;
}

.notification-list-card :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.7);
}

.notification-list-card :deep(.el-tabs__item.is-active) {
  color: #ff6b6b;
}

.notification-list-card :deep(.el-tabs__active-bar) {
  background-color: #8B0000;
}

.tab-badge {
  margin-left: 5px;
}

.notification-list {
  min-height: 400px;
}

.notification-items {
  margin-top: 20px;
}

.notification-item {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.5) 0%, rgba(30, 45, 80, 0.5) 100%);
  margin-bottom: 10px;
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.1);
}

.notification-item:hover {
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.8) 0%, rgba(40, 55, 90, 0.8) 100%);
  border-color: rgba(139, 0, 0, 0.3);
}

.notification-item.unread {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.15) 0%, rgba(139, 0, 0, 0.08) 100%);
  border-color: rgba(139, 0, 0, 0.3);
}

.notification-item.unread:hover {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.25) 0%, rgba(139, 0, 0, 0.15) 100%);
  border-color: rgba(139, 0, 0, 0.5);
}

.notification-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(139, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-title {
  margin: 0;
  font-size: 16px;
  color: #fff;
  font-weight: bold;
}

.notification-message {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notification-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.notification-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 15px;
}

.notification-actions .el-button {
  color: rgba(255, 255, 255, 0.7);
}

.notification-actions .el-button:hover {
  color: #ff6b6b;
}

/* 消息详情 */
.notification-detail {
  padding: 20px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.detail-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.detail-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

/* 分页 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.pagination :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: rgba(255, 255, 255, 0.7);
  --el-pagination-button-disabled-color: rgba(255, 255, 255, 0.3);
}

.pagination :deep(.el-pager li) {
  background: rgba(35, 35, 60, 0.8);
  color: rgba(255, 255, 255, 0.8);
}

.pagination :deep(.el-pager li.is-active) {
  background: #8B0000;
  color: #fff;
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  background: rgba(35, 35, 60, 0.8);
  color: rgba(255, 255, 255, 0.8);
}
</style>

<!-- 非 scoped 样式覆盖对话框 -->
<style>
.user-notifications .el-dialog {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border: 1px solid rgba(139, 0, 0, 0.3);
}

.user-notifications .el-dialog__header {
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.user-notifications .el-dialog__title {
  color: #fff;
}

.user-notifications .el-dialog__body {
  color: rgba(255, 255, 255, 0.85);
}

.user-notifications .el-dialog__footer {
  border-top: 1px solid rgba(139, 0, 0, 0.2);
}
</style>
