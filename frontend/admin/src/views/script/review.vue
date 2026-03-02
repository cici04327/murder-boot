<template>
  <div class="script-review-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>剧本评价管理</span>
        </div>
      </template>

      <!-- 查询表单 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="剧本">
          <el-select v-model="queryForm.scriptId" placeholder="请选择剧本" clearable filterable style="width: 250px">
            <el-option
              v-for="script in scriptList"
              :key="script.id"
              :label="script.name"
              :value="script.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="scriptName" label="剧本名称" width="180" />
        <el-table-column prop="userNickname" label="用户" width="120" />
        <el-table-column label="评分" width="280">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <div>
                <span style="color: #909399; font-size: 12px;">总评分：</span>
                <el-rate v-model="row.rating" disabled show-score size="small" />
              </div>
              <div v-if="row.storyRating">
                <span style="color: #909399; font-size: 12px;">剧情：</span>
                <el-rate v-model="row.storyRating" disabled size="small" />
              </div>
              <div v-if="row.difficultyRating">
                <span style="color: #909399; font-size: 12px;">难度：</span>
                <el-rate v-model="row.difficultyRating" disabled size="small" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="250">
          <template #default="{ row }">
            <div class="content-cell">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
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

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="评价详情"
      width="600px"
    >
      <div v-if="currentReview" class="review-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="剧本名称">
            {{ currentReview.scriptName }}
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ currentReview.userNickname }}
          </el-descriptions-item>
          <el-descriptions-item label="总评分">
            <el-rate v-model="currentReview.rating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="剧情评分" v-if="currentReview.storyRating">
            <el-rate v-model="currentReview.storyRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="难度评分" v-if="currentReview.difficultyRating">
            <el-rate v-model="currentReview.difficultyRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="评价内容">
            {{ currentReview.content }}
          </el-descriptions-item>
          <el-descriptions-item label="评价时间">
            {{ currentReview.createTime }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 数据
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const scriptList = ref([])

const queryForm = reactive({
  page: 1,
  pageSize: 10,
  scriptId: null
})

// 详情
const detailDialogVisible = ref(false)
const currentReview = ref(null)

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    const response = await request.get('/script/review/page', {
      params: queryForm
    })
    tableData.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchScripts = async () => {
  try {
    const response = await request.get('/script/page', {
      params: { page: 1, pageSize: 100 }
    })
    if (response && response.data && response.data.records) {
      scriptList.value = response.data.records
    }
  } catch (error) {
    console.error('获取剧本列表失败', error)
  }
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.scriptId = null
  queryForm.page = 1
  fetchData()
}

const handleView = (row) => {
  currentReview.value = row
  detailDialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该评价吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/script/review/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchData()
  fetchScripts()
})
</script>

<style scoped>
.script-review-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  margin-bottom: 20px;
}

.content-cell {
  line-height: 1.6;
  color: #606266;
}

.review-detail {
  padding: 10px 0;
}
</style>
