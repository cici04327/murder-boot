<template>
  <div class="user-feedbacks">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="header-icon">📬</div>
        <h1>我的留言</h1>
        <p class="header-subtitle">查看您提交的留言和回复</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.total }}</div>
          <div class="stat-label">全部留言</div>
        </div>
      </div>
      <div class="stat-card pending">
        <div class="stat-icon">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.pending }}</div>
          <div class="stat-label">待回复</div>
        </div>
      </div>
      <div class="stat-card replied">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.replied }}</div>
          <div class="stat-label">已回复</div>
        </div>
      </div>
    </div>

    <!-- 留言列表 -->
    <div class="feedback-list" v-loading="loading">
      <div v-if="feedbacks.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📭</div>
        <p class="empty-text">暂无留言记录</p>
        <el-button type="primary" @click="goToContact">
          <el-icon><EditPen /></el-icon>
          去留言
        </el-button>
      </div>

      <div v-else class="feedback-cards">
        <div 
          v-for="feedback in feedbacks" 
          :key="feedback.id" 
          class="feedback-card"
          :class="{ 'has-reply': feedback.status === 2 }"
        >
          <div class="card-header">
            <div class="subject-tag" :class="getSubjectClass(feedback.subject)">
              {{ feedback.subjectName }}
            </div>
            <div class="status-tag" :class="getStatusClass(feedback.status)">
              {{ feedback.statusName }}
            </div>
          </div>

          <div class="card-body">
            <div class="message-content">
              <div class="content-label">留言内容</div>
              <p class="content-text">{{ feedback.message }}</p>
            </div>

            <div class="message-meta">
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ formatTime(feedback.createTime) }}
              </span>
            </div>
          </div>

          <!-- 回复区域 -->
          <div v-if="feedback.replyContent" class="reply-section">
            <div class="reply-header">
              <span class="reply-icon">💬</span>
              <span class="reply-title">客服回复</span>
              <span class="reply-time">{{ formatTime(feedback.replyTime) }}</span>
            </div>
            <div class="reply-content">
              {{ feedback.replyContent }}
            </div>
          </div>

          <!-- 等待回复提示 -->
          <div v-else class="waiting-reply">
            <el-icon class="waiting-icon"><Loading /></el-icon>
            <span>等待客服回复中...</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { Clock, EditPen, Loading } from '@element-plus/icons-vue'
import { getUserFeedbacks } from '@/api/feedback'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const feedbacks = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statistics = reactive({
  total: 0,
  pending: 0,
  replied: 0
})

// WebSocket 连接
let ws = null

// 获取主题样式
const getSubjectClass = (subject) => {
  const classes = {
    'game': 'subject-game',
    'store': 'subject-store',
    'script': 'subject-script',
    'suggestion': 'subject-suggestion',
    'bug': 'subject-bug',
    'platform': 'subject-platform',
    'booking': 'subject-booking',
    'business': 'subject-business',
    'other': 'subject-other'
  }
  return classes[subject] || 'subject-other'
}

// 获取状态样式
const getStatusClass = (status) => {
  const classes = {
    0: 'status-pending',
    1: 'status-processing',
    2: 'status-replied',
    3: 'status-closed'
  }
  return classes[status] || 'status-pending'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  // 1分钟内
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  // 1小时内
  if (diff < 60 * 60 * 1000) {
    return Math.floor(diff / (60 * 1000)) + '分钟前'
  }
  // 24小时内
  if (diff < 24 * 60 * 60 * 1000) {
    return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
  }
  // 超过24小时显示具体日期
  return time.replace('T', ' ').substring(0, 16)
}

// 加载留言列表
const loadFeedbacks = async () => {
  loading.value = true
  try {
    const res = await getUserFeedbacks({
      page: page.value,
      pageSize: pageSize.value
    })
    
    if (res.code === 1 || res.code === 200) {
      feedbacks.value = res.data.records || []
      total.value = res.data.total || 0
      
      // 更新统计
      updateStatistics()
    }
  } catch (error) {
    console.error('加载留言列表失败:', error)
    ElMessage.error('加载留言列表失败')
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = async () => {
  try {
    // 获取所有留言统计
    const allRes = await getUserFeedbacks({ page: 1, pageSize: 1000 })
    if (allRes.code === 1 || allRes.code === 200) {
      const all = allRes.data.records || []
      statistics.total = all.length
      statistics.pending = all.filter(f => f.status === 0 || f.status === 1).length
      statistics.replied = all.filter(f => f.status === 2).length
    }
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  page.value = 1
  loadFeedbacks()
}

const handlePageChange = (val) => {
  page.value = val
  loadFeedbacks()
}

// 跳转到联系页面
const goToContact = () => {
  router.push('/contact')
}

// 初始化 WebSocket 连接
const initWebSocket = () => {
  if (!userStore.userInfo?.id) return
  
  const wsUrl = `ws://${window.location.host}/api/ws/notification?userId=${userStore.userInfo.id}`
  
  try {
    ws = new WebSocket(wsUrl)
    
    ws.onopen = () => {
      console.log('留言页面 WebSocket 已连接')
    }
    
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        
        // 处理留言回复通知
        if (data.type === 'feedback_reply') {
          ElNotification({
            title: data.title || '留言已回复',
            message: data.content,
            type: 'success',
            duration: 5000
          })
          
          // 刷新留言列表
          loadFeedbacks()
        }
      } catch (e) {
        console.error('解析 WebSocket 消息失败:', e)
      }
    }
    
    ws.onclose = () => {
      console.log('留言页面 WebSocket 已断开')
    }
    
    ws.onerror = (error) => {
      console.error('WebSocket 错误:', error)
    }
  } catch (error) {
    console.error('WebSocket 连接失败:', error)
  }
}

// 关闭 WebSocket
const closeWebSocket = () => {
  if (ws) {
    ws.close()
    ws = null
  }
}

onMounted(() => {
  loadFeedbacks()
  initWebSocket()
})

onUnmounted(() => {
  closeWebSocket()
})
</script>

<style scoped>
.user-feedbacks {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding-bottom: 40px;
}

/* 页面头部 */
.page-header {
  position: relative;
  padding: 60px 20px 40px;
  text-align: center;
  color: #fff;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="40" r="1.5" fill="rgba(255,255,255,0.08)"/><circle cx="40" cy="70" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
  opacity: 0.5;
}

.header-content {
  position: relative;
  z-index: 1;
}

.header-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 600;
}

.header-subtitle {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: 16px;
  padding: 0 20px;
  margin-top: -20px;
  margin-bottom: 24px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.stat-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-icon {
  font-size: 28px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-card.pending .stat-value {
  color: #e6a23c;
}

.stat-card.replied .stat-value {
  color: #67c23a;
}

/* 留言列表 */
.feedback-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 24px;
}

/* 留言卡片 */
.feedback-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.feedback-card.has-reply {
  border-left: 4px solid #67c23a;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.subject-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.subject-game { background: rgba(64, 158, 255, 0.16); color: #8fc7ff; border: 1px solid rgba(64, 158, 255, 0.24); }
.subject-store { background: rgba(245, 124, 0, 0.16); color: #ffb86c; border: 1px solid rgba(245, 124, 0, 0.24); }
.subject-script { background: rgba(171, 71, 188, 0.16); color: #daa9ea; border: 1px solid rgba(171, 71, 188, 0.24); }
.subject-suggestion { background: rgba(76, 175, 80, 0.16); color: #9de6a0; border: 1px solid rgba(76, 175, 80, 0.24); }
.subject-bug { background: rgba(244, 67, 54, 0.16); color: #ff9a91; border: 1px solid rgba(244, 67, 54, 0.24); }
.subject-platform { background: rgba(0, 150, 136, 0.16); color: #79dfd0; border: 1px solid rgba(0, 150, 136, 0.24); }
.subject-booking { background: rgba(233, 30, 99, 0.16); color: #ff98bb; border: 1px solid rgba(233, 30, 99, 0.24); }
.subject-business { background: rgba(255, 160, 0, 0.16); color: #ffd27b; border: 1px solid rgba(255, 160, 0, 0.24); }
.subject-other { background: rgba(144, 164, 174, 0.16); color: #d0dce2; border: 1px solid rgba(144, 164, 174, 0.24); }

.status-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending { background: rgba(255, 152, 0, 0.16); color: #ffc56a; border: 1px solid rgba(255, 152, 0, 0.24); }
.status-processing { background: rgba(64, 158, 255, 0.16); color: #8fc7ff; border: 1px solid rgba(64, 158, 255, 0.24); }
.status-replied { background: rgba(103, 194, 58, 0.16); color: #9de48b; border: 1px solid rgba(103, 194, 58, 0.24); }
.status-closed { background: rgba(144, 164, 174, 0.16); color: #d0dce2; border: 1px solid rgba(144, 164, 174, 0.24); }

.card-body {
  padding: 20px;
}

.content-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.content-text {
  margin: 0;
  color: #fff;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.message-meta {
  margin-top: 16px;
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 回复区域 */
.reply-section {
  background: linear-gradient(135deg, rgba(46, 125, 50, 0.18) 0%, rgba(28, 73, 38, 0.28) 100%);
  padding: 16px 20px;
  border-top: 1px dashed rgba(129, 199, 132, 0.35);
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.reply-icon {
  font-size: 18px;
}

.reply-title {
  font-size: 14px;
  font-weight: 600;
  color: #2e7d32;
}

.reply-time {
  margin-left: auto;
  font-size: 12px;
  color: rgba(129, 199, 132, 0.85);
}

.reply-content {
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 等待回复 */
.waiting-reply {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: rgba(245, 124, 0, 0.16);
  color: #ffbf6f;
  border-top: 1px dashed rgba(245, 124, 0, 0.28);
  font-size: 14px;
}

.waiting-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 分页 */
.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.1);
  --el-pagination-text-color: #fff;
  --el-pagination-button-color: #fff;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    flex-direction: column;
  }
  
  .page-header {
    padding: 40px 20px 30px;
  }
  
  .header-icon {
    font-size: 36px;
  }
  
  .page-header h1 {
    font-size: 22px;
  }
}
</style>
