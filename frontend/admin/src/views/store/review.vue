<template>
  <div class="review-management">
    <el-card>
      <!-- 查询表单 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="门店">
          <el-select v-model="queryForm.storeId" placeholder="请选择门店" clearable filterable>
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="storeName" label="门店名称" width="180" />
        <el-table-column prop="userNickname" label="用户" width="120" />
        <el-table-column label="评分" width="280">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <div>
                <span style="color: #909399; font-size: 12px;">总评分：</span>
                <el-rate v-model="row.rating" disabled show-score size="small" />
              </div>
              <div v-if="row.environmentRating">
                <span style="color: #909399; font-size: 12px;">环境：</span>
                <el-rate v-model="row.environmentRating" disabled size="small" />
              </div>
              <div v-if="row.serviceRating">
                <span style="color: #909399; font-size: 12px;">服务：</span>
                <el-rate v-model="row.serviceRating" disabled size="small" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="content-cell">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="reply" label="商家回复" min-width="200">
          <template #default="{ row }">
            <div v-if="row.reply" class="reply-cell">
              <el-tag type="success" size="small">商家回复</el-tag>
              <div style="margin-top: 4px;">{{ row.reply }}</div>
            </div>
            <el-tag v-else type="info" size="small">未回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="!row.reply" 
              type="primary" 
              size="small" 
              @click="handleReply(row)"
            >
              回复
            </el-button>
            <el-button 
              v-else 
              type="success" 
              size="small" 
              @click="handleViewReply(row)"
            >
              查看回复
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="replyDialogVisible"
      title="商家回复"
      width="600px"
      @close="handleReplyDialogClose"
    >
      <el-form :model="replyForm" ref="replyFormRef" label-width="80px">
        <el-form-item label="评价内容">
          <div class="review-content">
            <div style="margin-bottom: 8px;">
              <el-rate v-model="currentReview.rating" disabled show-score />
            </div>
            <div>{{ currentReview.content }}</div>
          </div>
        </el-form-item>
        <el-form-item label="回复内容" prop="reply" :rules="[
          { required: true, message: '请输入回复内容', trigger: 'blur' }
        ]">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReply">提交回复</el-button>
      </template>
    </el-dialog>

    <!-- 查看回复对话框 -->
    <el-dialog
      v-model="viewReplyDialogVisible"
      title="查看回复"
      width="600px"
    >
      <div class="review-detail">
        <div class="review-section">
          <div class="section-title">用户评价</div>
          <div style="margin-bottom: 8px;">
            <el-rate v-model="currentReview.rating" disabled show-score />
          </div>
          <div>{{ currentReview.content }}</div>
        </div>
        <el-divider />
        <div class="reply-section">
          <div class="section-title">
            <el-tag type="success">商家回复</el-tag>
          </div>
          <div>{{ currentReview.reply }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const replyDialogVisible = ref(false)
const viewReplyDialogVisible = ref(false)
const replyFormRef = ref(null)
const storeList = ref([])
const currentReview = ref({})

const queryForm = reactive({
  storeId: null,
  page: 1,
  pageSize: 10
})

const replyForm = reactive({
  reply: ''
})

const fetchStoreList = async () => {
  try {
    const res = await request.get('/store/list')
    storeList.value = res.data
  } catch (error) {
    console.error('获取门店列表失败:', error)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: queryForm.page,
      pageSize: queryForm.pageSize
    }
    if (queryForm.storeId) {
      params.storeId = queryForm.storeId
    }
    const res = await request.get('/store/review/page', { params })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.storeId = null
  queryForm.page = 1
  fetchData()
}

const handleReply = (row) => {
  currentReview.value = row
  replyDialogVisible.value = true
}

const handleViewReply = (row) => {
  currentReview.value = row
  viewReplyDialogVisible.value = true
}

const handleSubmitReply = async () => {
  try {
    await replyFormRef.value.validate()
    
    await request.put(`/store/review/reply/${currentReview.value.id}?reply=${encodeURIComponent(replyForm.reply)}`)
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('回复失败:', error)
    if (error !== false) {
      ElMessage.error('回复失败')
    }
  }
}

const handleReplyDialogClose = () => {
  replyFormRef.value?.resetFields()
  replyForm.reply = ''
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该评价吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/store/review/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchStoreList()
  fetchData()
})
</script>

<style scoped>
.review-management {
  width: 100%;
}

.query-form {
  margin-bottom: 20px;
}

.content-cell {
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.reply-cell {
  line-height: 1.5;
}

.review-content {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
}

.review-detail {
  padding: 12px;
}

.review-section,
.reply-section {
  margin-bottom: 12px;
}

.section-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #303133;
}
</style>
