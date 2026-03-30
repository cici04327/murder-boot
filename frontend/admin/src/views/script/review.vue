<template>
  <div class="review-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>综合评价管理</span>
          <el-tag type="info" size="small">来源：用户预约完成后提交的评价</el-tag>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="门店" v-if="!isStoreAdmin">
          <el-select v-model="queryForm.storeId" placeholder="全部门店" clearable style="width:180px">
            <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="剧本">
          <el-select v-model="queryForm.scriptId" placeholder="全部剧本" clearable filterable style="width:180px">
            <el-option v-for="s in scriptList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width:120px">
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" style="width:100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="storeName" label="门店" width="140" show-overflow-tooltip />
        <el-table-column prop="scriptName" label="剧本" width="130" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="用户" width="100">
          <template #default="{ row }">
            <span>{{ row.isAnonymous ? '匿名用户' : row.userNickname }}</span>
          </template>
        </el-table-column>
        <el-table-column label="综合评分" width="200">
          <template #default="{ row }">
            <div style="display:flex;flex-direction:column;gap:4px">
              <div>
                <span style="color:#909399;font-size:12px">综合：</span>
                <el-rate :model-value="row.overallRating" disabled size="small" show-score />
              </div>
              <div v-if="row.scriptRating">
                <span style="color:#909399;font-size:12px">剧本：</span>
                <el-rate :model-value="row.scriptRating" disabled size="small" />
              </div>
              <div v-if="row.dmRating">
                <span style="color:#909399;font-size:12px">DM：</span>
                <el-rate :model-value="row.dmRating" disabled size="small" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===2?'success':row.status===3?'danger':'warning'" size="small">
              {{ row.status===2?'已通过':row.status===3?'已拒绝':'待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.replyContent?'success':'info'" size="small">
              {{ row.replyContent ? '已回复' : '未回复' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="155" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="!row.replyContent" size="small" type="primary" @click="handleReply(row)">回复</el-button>
            <el-button v-if="row.status===1" size="small" type="success" @click="handleAudit(row, 2)">通过</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchData"
        style="margin-top:20px;justify-content:flex-end"
      />
    </el-card>

    <!-- 查看详情 -->
    <el-dialog v-model="detailVisible" title="评价详情" width="650px">
      <div v-if="currentReview">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店">{{ currentReview.storeName }}</el-descriptions-item>
          <el-descriptions-item label="剧本">{{ currentReview.scriptName }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentReview.isAnonymous ? '匿名用户' : currentReview.userNickname }}</el-descriptions-item>
          <el-descriptions-item label="预约ID">{{ currentReview.reservationId }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">
            <el-rate :model-value="currentReview.overallRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="剧本评分" v-if="currentReview.scriptRating">
            <el-rate :model-value="currentReview.scriptRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="门店评分" v-if="currentReview.storeRating">
            <el-rate :model-value="currentReview.storeRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="DM评分" v-if="currentReview.dmRating">
            <el-rate :model-value="currentReview.dmRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="评价内容" :span="2">{{ currentReview.content }}</el-descriptions-item>
          <el-descriptions-item label="标签" v-if="currentReview.tags">{{ currentReview.tags }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentReview.status===2?'success':currentReview.status===3?'danger':'warning'" size="small">
              {{ currentReview.status===2?'已通过':currentReview.status===3?'已拒绝':'待审核' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商家回复" :span="2" v-if="currentReview.replyContent">
            {{ currentReview.replyContent }}
          </el-descriptions-item>
          <el-descriptions-item label="评价时间">{{ currentReview.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible=false">关闭</el-button>
        <el-button v-if="currentReview&&!currentReview.replyContent" type="primary" @click="detailVisible=false;handleReply(currentReview)">去回复</el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="500px">
      <div v-if="currentReview" style="margin-bottom:12px;padding:12px;background:#f5f7fa;border-radius:6px">
        <el-rate :model-value="currentReview.overallRating" disabled show-score size="small" />
        <div style="margin-top:6px;color:#606266">{{ currentReview.content }}</div>
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="replyVisible=false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleSubmitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading   = ref(false)
const replying  = ref(false)
const tableData = ref([])
const total     = ref(0)
const storeList = ref([])
const scriptList = ref([])
const detailVisible = ref(false)
const replyVisible  = ref(false)
const currentReview = ref(null)
const replyContent  = ref('')

const loginType    = localStorage.getItem('admin-login-type') || 'admin'
const isStoreAdmin = loginType === 'store'
const fixedStoreId = isStoreAdmin ? Number(localStorage.getItem('admin-store-id')) : null

const queryForm = reactive({
  page: 1, pageSize: 10,
  storeId: fixedStoreId || null,
  scriptId: null, status: null
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = Object.fromEntries(Object.entries(queryForm).filter(([,v]) => v !== null && v !== undefined))
    const res = await request.get('/reservation/review/page', { params })
    console.log('[review] res:', res)
    tableData.value = res.data?.records || []
    total.value     = res.data?.total || 0
  } catch (e) {
    console.error('获取评价失败', e)
    ElMessage.error('获取数据失败')
  } finally { loading.value = false }
}

const fetchStoreList = async () => {
  if (isStoreAdmin) return
  try {
    const res = await request.get('/store/list')
    storeList.value = res?.data || []
  } catch {}
}

const fetchScriptList = async () => {
  try {
    const res = await request.get('/script/page', { params: { page: 1, pageSize: 200 } })
    scriptList.value = res.data?.records || []
  } catch {}
}

const handleQuery = () => { queryForm.page = 1; fetchData() }
const handleSizeChange = (s) => { queryForm.pageSize = s; queryForm.page = 1; fetchData() }
const handleReset = () => {
  queryForm.storeId = fixedStoreId || null
  queryForm.scriptId = null; queryForm.status = null; queryForm.page = 1
  fetchData()
}

const handleView = (row) => { currentReview.value = row; detailVisible.value = true }

const handleReply = (row) => { currentReview.value = row; replyContent.value = ''; replyVisible.value = true }

const handleSubmitReply = async () => {
  if (!replyContent.value.trim()) return ElMessage.warning('请输入回复内容')
  replying.value = true
  try {
    await request.put(`/reservation/review/${currentReview.value.id}/reply`, null, { params: { replyContent: replyContent.value } })
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchData()
  } catch { ElMessage.error('回复失败') } finally { replying.value = false }
}

const handleAudit = async (row, status) => {
  try {
    await request.put(`/reservation/review/${row.id}/audit`, null, { params: { status } })
    ElMessage.success('审核成功')
    fetchData()
  } catch { ElMessage.error('审核失败') }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该评价吗？', '提示', { type: 'warning' })
  try {
    await request.delete(`/reservation/review/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => { fetchStoreList(); fetchScriptList(); fetchData() })
</script>

<style scoped>
.review-management { width: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.query-form { margin-bottom: 20px; }
</style>
