<template>
  <div class="feedback-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon total">
              <el-icon :size="24"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">留言总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon pending">
              <el-icon :size="24"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon replied">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.replied }}</div>
              <div class="stat-label">已回复</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon today">
              <el-icon :size="24"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.today }}</div>
              <div class="stat-label">今日新增</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主内容卡片 -->
    <el-card class="main-card" shadow="hover">
      <!-- 筛选工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-radio-group v-model="filterStatus" @change="handleFilterChange" size="default">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="0">
              <el-badge :value="stats.pending" :hidden="stats.pending === 0" class="status-badge">
                待处理
              </el-badge>
            </el-radio-button>
            <el-radio-button label="2">已回复</el-radio-button>
          </el-radio-group>
          
          <el-select v-model="filterSubject" placeholder="留言类型" clearable style="width: 140px; margin-left: 16px;" @change="handleFilterChange">
            <el-option label="意见建议" value="suggestion" />
            <el-option label="问题反馈" value="problem" />
            <el-option label="投诉举报" value="complaint" />
            <el-option label="其他" value="other" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索姓名、联系方式或内容" 
            style="width: 280px;" 
            clearable 
            @clear="loadData"
            @keyup.enter="loadData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="loadData">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
          <el-button type="primary" plain @click="loadData" style="margin-left: 12px;">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <!-- 留言列表 -->
      <el-table :data="feedbackList" v-loading="loading" style="width: 100%" row-key="id" class="feedback-table">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="expand-section">
                <div class="expand-label">留言内容：</div>
                <div class="expand-text">{{ row.message }}</div>
              </div>
              <div class="expand-section" v-if="row.replyContent">
                <div class="expand-label reply-label">
                  <el-icon><ChatLineSquare /></el-icon>
                  回复内容：
                </div>
                <div class="expand-text reply-text">{{ row.replyContent }}</div>
                <div class="reply-time">回复时间：{{ row.replyTime }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="用户信息" width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" class="user-avatar">
                {{ (row.name || '匿').charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.name || '匿名用户' }}</div>
                <div class="user-contact">{{ row.contact || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getSubjectType(row.subject)" size="small" effect="light">
              {{ getSubjectName(row.subject) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="留言内容" min-width="280">
          <template #default="{ row }">
            <div class="message-cell">
              <div class="message-preview">{{ truncateText(row.message, 60) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : (row.status === 1 ? 'primary' : 'warning')" effect="dark" size="small">
              {{ row.status === 2 ? '已回复' : (row.status === 1 ? '处理中' : '待处理') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" align="center">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="success" link size="small" @click="handleReply(row)" v-if="row.status !== 2">
              <el-icon><ChatLineSquare /></el-icon>
              回复
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          background
        />
      </div>
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="留言详情" width="650px" class="detail-dialog">
      <div class="detail-content" v-if="currentFeedback">
        <!-- 用户信息 -->
        <div class="detail-header">
          <el-avatar :size="48" class="detail-avatar">
            {{ (currentFeedback.name || '匿').charAt(0) }}
          </el-avatar>
          <div class="detail-user">
            <div class="detail-name">{{ currentFeedback.name || '匿名用户' }}</div>
            <div class="detail-meta">
              <span v-if="currentFeedback.contact">
                <el-icon><Phone /></el-icon>
                {{ currentFeedback.contact }}
              </span>
              <span>
                <el-icon><Clock /></el-icon>
                {{ currentFeedback.createTime }}
              </span>
            </div>
          </div>
          <div class="detail-tags">
            <el-tag :type="getSubjectType(currentFeedback.subject)" effect="light">
              {{ getSubjectName(currentFeedback.subject) }}
            </el-tag>
            <el-tag :type="currentFeedback.status === 2 ? 'success' : (currentFeedback.status === 1 ? 'primary' : 'warning')" effect="dark">
              {{ currentFeedback.status === 2 ? '已回复' : (currentFeedback.status === 1 ? '处理中' : '待处理') }}
            </el-tag>
          </div>
        </div>
        
        <!-- 留言内容 -->
        <div class="detail-section">
          <div class="section-title">
            <el-icon><ChatDotRound /></el-icon>
            留言内容
          </div>
          <div class="section-content message-box">
            {{ currentFeedback.message }}
          </div>
        </div>
        
        <!-- 回复内容 -->
        <div class="detail-section" v-if="currentFeedback.replyContent">
          <div class="section-title reply-title">
            <el-icon><ChatLineSquare /></el-icon>
            回复内容
          </div>
          <div class="section-content reply-box">
            {{ currentFeedback.replyContent }}
            <div class="reply-meta">回复时间：{{ currentFeedback.replyTime }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleReply(currentFeedback)" v-if="currentFeedback && currentFeedback.status !== 2">
          <el-icon><ChatLineSquare /></el-icon>
          回复此留言
        </el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="回复留言" width="550px" class="reply-dialog">
      <div class="reply-content" v-if="currentFeedback">
        <div class="original-message">
          <div class="original-header">
            <el-avatar :size="32">{{ (currentFeedback.name || '匿').charAt(0) }}</el-avatar>
            <span class="original-name">{{ currentFeedback.name || '匿名用户' }}</span>
            <span class="original-time">{{ currentFeedback.createTime }}</span>
          </div>
          <div class="original-text">{{ currentFeedback.message }}</div>
        </div>
        <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules">
          <el-form-item prop="replyContent">
            <el-input 
              v-model="replyForm.replyContent" 
              type="textarea" 
              :rows="5" 
              placeholder="请输入回复内容..." 
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReplySubmit" :loading="replying">
          <el-icon><Promotion /></el-icon>
          发送回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ChatDotRound, Clock, CircleCheck, Calendar, Search, Refresh,
  View, ChatLineSquare, Delete, Phone, Promotion
} from '@element-plus/icons-vue'
import { userService } from '@/utils/request'

const loading = ref(false)
const replying = ref(false)
const feedbackList = ref([])
const filterStatus = ref('')
const filterSubject = ref('')
const searchKeyword = ref('')
const viewDialogVisible = ref(false)
const replyDialogVisible = ref(false)
const currentFeedback = ref(null)
const replyFormRef = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = reactive({
  total: 0,
  pending: 0,
  replied: 0,
  today: 0
})

const replyForm = reactive({
  replyContent: ''
})

const replyRules = {
  replyContent: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterStatus.value !== '') {
      params.status = parseInt(filterStatus.value)
    }
    if (filterSubject.value) {
      params.subject = filterSubject.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    const res = await userService.get('/feedback/page', { params })
    if (res.code === 1 || res.code === 200) {
      feedbackList.value = res.data?.records || []
      pagination.total = res.data?.total || 0
      
      // 计算统计数据
      stats.total = pagination.total
      stats.pending = feedbackList.value.filter(f => f.status === 0).length
      stats.replied = feedbackList.value.filter(f => f.status === 2).length
    }
  } catch (e) {
    console.error('加载留言失败:', e)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  // 统计数据从列表数据中计算，无需单独接口
}

const getSubjectName = (subject) => {
  const names = { 
    suggestion: '意见建议', 
    problem: '问题反馈', 
    complaint: '投诉举报', 
    other: '其他' 
  }
  return names[subject] || subject || '其他'
}

const getSubjectType = (subject) => {
  const types = { 
    suggestion: 'success', 
    problem: 'warning', 
    complaint: 'danger', 
    other: 'info' 
  }
  return types[subject] || 'info'
}

const truncateText = (text, maxLen) => {
  if (!text) return '-'
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const formatTime = (time) => {
  if (!time) return '-'
  return time
}

const handleFilterChange = () => {
  pagination.page = 1
  loadData()
}

const handleView = (row) => {
  currentFeedback.value = row
  viewDialogVisible.value = true
}

const handleReply = (row) => {
  currentFeedback.value = row
  replyForm.replyContent = ''
  viewDialogVisible.value = false
  replyDialogVisible.value = true
}

const handleReplySubmit = async () => {
  try {
    await replyFormRef.value.validate()
    replying.value = true
    
    const res = await userService.put(`/feedback/${currentFeedback.value.id}/reply`, null, {
      params: { replyContent: replyForm.replyContent }
    })
    
    if (res.code === 1 || res.code === 200) {
      // 更新本地数据状态
      const feedback = feedbackList.value.find(f => f.id === currentFeedback.value.id)
      if (feedback) {
        feedback.status = 2  // 已回复状态是2
        feedback.replyContent = replyForm.replyContent
        feedback.replyTime = new Date().toLocaleString()
      }
      
      // 更新统计
      stats.pending = Math.max(0, stats.pending - 1)
      stats.replied = stats.replied + 1
      
      ElMessage.success('回复成功')
      replyDialogVisible.value = false
      
      // 重新加载数据确保同步
      loadData()
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('回复失败:', e)
      ElMessage.error('回复失败')
    }
  } finally {
    replying.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该留言吗？删除后无法恢复。', '确认删除', { 
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await userService.delete(`/feedback/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.feedback-container {
  padding: 0;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon.total { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.stat-icon.pending { background: linear-gradient(135deg, #E6A23C, #f0c78a); }
.stat-icon.replied { background: linear-gradient(135deg, #67C23A, #95d475); }
.stat-icon.today { background: linear-gradient(135deg, #909399, #b1b3b8); }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 6px;
}

/* 主卡片 */
.main-card {
  border-radius: 8px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.status-badge :deep(.el-badge__content) {
  top: -4px;
  right: -4px;
}

/* 表格样式 */
.feedback-table {
  border-radius: 8px;
  overflow: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  font-weight: 600;
}

.user-info {
  line-height: 1.4;
}

.user-name {
  font-weight: 500;
  color: #303133;
}

.user-contact {
  font-size: 12px;
  color: #909399;
}

.message-cell {
  line-height: 1.5;
}

.message-preview {
  color: #606266;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #909399;
  font-size: 13px;
}

/* 展开内容 */
.expand-content {
  padding: 16px 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin: 8px 0;
}

.expand-section {
  margin-bottom: 16px;
}

.expand-section:last-child {
  margin-bottom: 0;
}

.expand-label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.expand-label.reply-label {
  color: #67C23A;
}

.expand-text {
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

.expand-text.reply-text {
  color: #67C23A;
  background-color: #f0f9eb;
  padding: 12px;
  border-radius: 6px;
  border-left: 3px solid #67C23A;
}

.reply-time {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  text-align: right;
}

/* 分页 */
.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 详情对话框 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.detail-avatar {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  font-weight: 600;
}

.detail-user {
  flex: 1;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.detail-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title.reply-title {
  color: #67C23A;
}

.section-content {
  line-height: 1.8;
  color: #303133;
}

.message-box {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
}

.reply-box {
  background-color: #f0f9eb;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #67C23A;
  color: #67C23A;
  white-space: pre-wrap;
  word-break: break-all;
}

.reply-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 10px;
  text-align: right;
}

/* 回复对话框 */
.original-message {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.original-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.original-name {
  font-weight: 500;
  color: #303133;
}

.original-time {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.original-text {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
