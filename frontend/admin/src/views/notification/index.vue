<template>
  <div class="notification-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Bell /></el-icon>
            通知中心
          </span>
          <div class="header-actions">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="badge-item">
              <el-tag type="danger">未读</el-tag>
            </el-badge>
            <el-button type="primary" size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
              <el-icon><Check /></el-icon>
              全部标记已读
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-radio-group v-model="onlyUnread" @change="handleFilterChange">
            <el-radio-button :label="false">全部通知</el-radio-button>
            <el-radio-button :label="true">
              仅未读
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="inline-badge" />
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-right">
          <el-button @click="loadData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <!-- 通知列表 -->
      <el-table 
        :data="notificationList" 
        v-loading="loading" 
        style="width: 100%" 
        @row-click="handleRowClick" 
        :row-class-name="getRowClassName"
        class="notification-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="notification-title">
              <span class="unread-dot" v-if="!row.isRead"></span>
              <span :class="{ 'font-bold': !row.isRead }">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300">
          <template #default="{ row }">
            <div class="content-cell">{{ truncateText(row.content, 50) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small" effect="light">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isRead" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'danger'" size="small" effect="dark">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" align="center">
          <template #default="{ row }">
            <span class="time-cell">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="success" link size="small" @click.stop="handleMarkRead(row)" v-if="!row.isRead">
              <el-icon><Check /></el-icon>
              已读
            </el-button>
            <el-button type="danger" link size="small" @click.stop="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
          background
        />
      </div>
    </el-card>

    <!-- 查看通知对话框 -->
    <el-dialog v-model="viewDialogVisible" title="通知详情" width="550px" class="detail-dialog">
      <div class="notification-detail" v-if="currentNotification">
        <div class="detail-header">
          <el-tag :type="getTypeTagType(currentNotification.type)" effect="light">
            {{ getTypeName(currentNotification.type) }}
          </el-tag>
          <el-tag :type="currentNotification.isRead ? 'info' : 'danger'" effect="dark">
            {{ currentNotification.isRead ? '已读' : '未读' }}
          </el-tag>
        </div>
        <h3 class="detail-title">{{ currentNotification.title }}</h3>
        <div class="detail-time">
          <el-icon><Clock /></el-icon>
          {{ currentNotification.createTime }}
        </div>
        <div class="detail-content">
          {{ currentNotification.content }}
        </div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleMarkRead(currentNotification)" v-if="currentNotification && !currentNotification.isRead">
          标记为已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Check, Refresh, View, Clock, Delete } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { userService } from '@/utils/request'

const loading = ref(false)
const notificationList = ref([])
const onlyUnread = ref(false)
const viewDialogVisible = ref(false)
const currentNotification = ref(null)
const stats = reactive({
  total: 0,
  unread: 0,
  today: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 未读数量
const unreadCount = ref(0)

// 获取未读数量
const loadUnreadCount = async () => {
  try {
    const res = await userService.get('/admin/notification/unread-count')
    if (res.code === 1 || res.code === 200) {
      unreadCount.value = res.data || 0
    }
  } catch (e) {
    console.error('获取未读数量失败:', e)
  }
}

// 加载通知列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      onlyUnread: onlyUnread.value || undefined
    }

    const res = await userService.get('/admin/notification/list', { params })
    if (res.code === 1 || res.code === 200) {
      notificationList.value = res.data?.records || res.data || []
      pagination.total = res.data?.total || notificationList.value.length
    }
  } catch (e) {
    console.error('加载通知失败:', e)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

// 获取类型名称
const getTypeName = (type) => {
  const names = { 
    system: '系统通知', 
    reservation: '预约通知', 
    coupon: '优惠通知',
    activity: '活动通知',
    feedback: '留言通知'
  }
  return names[type] || type || '其他'
}

// 获取类型标签颜色
const getTypeTagType = (type) => {
  const types = { 
    system: '', 
    reservation: 'warning', 
    coupon: 'danger',
    activity: 'success',
    feedback: 'info'
  }
  return types[type] || 'info'
}

// 截断文本
const truncateText = (text, maxLen) => {
  if (!text) return '-'
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

// 获取行样式类
const getRowClassName = ({ row }) => {
  return row.isRead ? 'read-row' : 'unread-row'
}

// 筛选变化
const handleFilterChange = () => {
  pagination.page = 1
  loadData()
}

// 点击行
const handleRowClick = (row) => {
  handleView(row)
}

// 查看通知详情
const handleView = async (row) => {
  currentNotification.value = row
  viewDialogVisible.value = true
  
  // 自动标记为已读（不重复调用）
  if (!row.isRead) {
    try {
      const res = await userService.put(`/admin/notification/${row.id}/read`)
      if (res.code === 1 || res.code === 200) {
        // 直接修改本地数据，不重新加载
        row.isRead = true
        if (currentNotification.value && currentNotification.value.id === row.id) {
          currentNotification.value.isRead = true
        }
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
}

// 标记单条为已读（从按钮点击）
const handleMarkRead = async (row) => {
  if (row.isRead) return // 已经是已读状态
  
  try {
    const res = await userService.put(`/admin/notification/${row.id}/read`)
    if (res.code === 1 || res.code === 200) {
      row.isRead = true
      if (currentNotification.value && currentNotification.value.id === row.id) {
        currentNotification.value.isRead = true
      }
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      ElMessage.success('已标记为已读')
    }
  } catch (e) {
    console.error('标记已读失败:', e)
    ElMessage.error('操作失败')
  }
  viewDialogVisible.value = false
}

// 全部标记为已读
const handleMarkAllRead = async () => {
  try {
    const res = await userService.put('/admin/notification/read-all')
    if (res.code === 1 || res.code === 200) {
      notificationList.value.forEach(n => n.isRead = true)
      unreadCount.value = 0
      ElMessage.success('已全部标记为已读')
    }
  } catch (e) {
    console.error('标记全部已读失败:', e)
    ElMessage.error('操作失败')
  }
}

// 删除通知
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该通知吗？', '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    const res = await userService.delete(`/admin/notification/${row.id}`)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
      loadUnreadCount()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadUnreadCount()
})
</script>

<style scoped>
.notification-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.badge-item {
  margin-right: 8px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-left {
  display: flex;
  align-items: center;
}

.filter-right {
  display: flex;
  align-items: center;
}

.inline-badge {
  margin-left: 5px;
}

/* 表格样式 */
.notification-table {
  border-radius: 8px;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: #F56C6C;
  border-radius: 50%;
  flex-shrink: 0;
}

.font-bold {
  font-weight: 600;
  color: #303133;
}

.content-cell {
  color: #606266;
  line-height: 1.5;
}

.time-cell {
  color: #909399;
  font-size: 13px;
}

:deep(.unread-row) {
  background-color: #fef0f0 !important;
  cursor: pointer;
}

:deep(.unread-row:hover td) {
  background-color: #fde2e2 !important;
}

:deep(.read-row) {
  cursor: pointer;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 详情对话框 */
.detail-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.detail-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.detail-content {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
