<template>
  <div class="knowledge-page">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-label">知识条目</div>
          <div class="stat-value">{{ stats.totalCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-label">启用条目</div>
          <div class="stat-value success">{{ stats.activeCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-label">FAQ条目</div>
          <div class="stat-value warning">{{ stats.faqCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-label">累计命中</div>
          <div class="stat-value primary">{{ stats.totalHitCount || 0 }}</div>
          <div class="stat-sub">今日 {{ stats.todayHitCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="标题/内容/关键词/FAQ问题" clearable style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增条目
          </el-button>
          <el-button type="warning" @click="handleReload" :loading="reloading">
            <el-icon><Refresh /></el-icon> 刷新缓存
          </el-button>
          <el-button type="info" @click="searchDialogVisible = true">
            <el-icon><Search /></el-icon> 检索测试
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="analysis-row">
      <el-col :xs="24" :md="12">
        <el-card class="analysis-card">
          <template #header><div class="card-title">分类分布</div></template>
          <div v-if="stats.categoryStats?.length" class="analysis-list">
            <div v-for="item in stats.categoryStats" :key="item.category" class="analysis-item">
              <div>
                <el-tag size="small" :type="categoryTagType(item.category)">{{ categoryLabel(item.category) }}</el-tag>
              </div>
              <div class="analysis-meta">共 {{ item.count }} 条 / 启用 {{ item.activeCount }} 条</div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="analysis-card">
          <template #header><div class="card-title">热门问题</div></template>
          <div v-if="stats.recentQueries?.length" class="analysis-list">
            <div v-for="item in stats.recentQueries" :key="item.query" class="analysis-item">
              <div class="query-text">{{ item.query }}</div>
              <div class="analysis-meta">命中 {{ item.count }} 次</div>
            </div>
          </div>
          <el-empty v-else description="暂无命中记录" />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <el-table :data="tableData" border v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)">
              {{ categoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="faqQuestion" label="FAQ问题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.faqQuestion || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="150" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column label="FAQ" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isFaq === 1 ? 'warning' : 'info'" size="small">{{ row.isFaq === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitCount" label="命中次数" width="100" />
        <el-table-column prop="lastHitTime" label="最后命中" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.lastHitTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'info' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识条目' : '新增知识条目'" width="720px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="95px">
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="知识条目标题，作为检索关键词之一" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="知识内容，将注入到AI提示词中" />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔，匹配权重最高" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="FAQ展示">
              <el-switch v-model="faqEnabled" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="FAQ问题" prop="faqQuestion">
              <el-input v-model="form.faqQuestion" :disabled="!faqEnabled" placeholder="用于用户端快捷问题展示" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="searchDialogVisible" title="RAG 检索测试" width="700px">
      <el-form :inline="true">
        <el-form-item label="测试问题">
          <el-input v-model="testQuery" placeholder="输入用户问题" style="width:300px" clearable />
        </el-form-item>
        <el-form-item label="Top K">
          <el-input-number v-model="testTopK" :min="1" :max="10" style="width:80px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="searching">检索</el-button>
        </el-form-item>
      </el-form>
      <div v-if="searchResults.length > 0" class="search-results">
        <div v-for="(item, idx) in searchResults" :key="item.id" class="result-item">
          <div class="result-header">
            <span class="result-index">#{{ idx + 1 }}</span>
            <el-tag size="small" :type="categoryTagType(item.category)">{{ categoryLabel(item.category) }}</el-tag>
            <span class="result-title">{{ item.title }}</span>
            <el-tag v-if="item.isFaq === 1" size="small" type="warning">FAQ</el-tag>
            <el-tag size="small" type="info" style="margin-left:auto">优先级 {{ item.priority }}</el-tag>
          </div>
          <div class="result-content">{{ item.content }}</div>
          <div class="result-keywords" v-if="item.keywords"><el-icon><Key /></el-icon> {{ item.keywords }}</div>
        </div>
      </div>
      <el-empty v-else-if="searched && searchResults.length === 0" description="未检索到相关知识条目" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Key } from '@element-plus/icons-vue'
import {
  getKnowledgeList,
  getKnowledgeStats,
  addKnowledge,
  updateKnowledge,
  deleteKnowledge,
  reloadKnowledge,
  searchKnowledge
} from '@/api/knowledge'

const categories = [
  { value: 'reservation', label: '预约', tagType: 'primary' },
  { value: 'refund', label: '退款', tagType: 'danger' },
  { value: 'vip', label: 'VIP', tagType: 'warning' },
  { value: 'coupon', label: '优惠券', tagType: 'success' },
  { value: 'points', label: '积分', tagType: 'success' },
  { value: 'group', label: '拼团', tagType: 'primary' },
  { value: 'payment', label: '支付', tagType: 'danger' },
  { value: 'dm', label: 'DM主持', tagType: 'warning' },
  { value: 'store', label: '门店', tagType: 'info' },
  { value: 'system', label: '系统', tagType: 'info' }
]

const categoryLabel = (val) => categories.find(c => c.value === val)?.label || val
const categoryTagType = (val) => categories.find(c => c.value === val)?.tagType || 'info'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const stats = ref({})
const searchForm = reactive({ category: null, keyword: '', status: null })

const loadStats = async () => {
  const res = await getKnowledgeStats()
  stats.value = res.data || {}
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList({
      page: page.value,
      pageSize: pageSize.value,
      ...searchForm
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(searchForm, { category: null, keyword: '', status: null })
  page.value = 1
  loadData()
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  category: '',
  title: '',
  content: '',
  keywords: '',
  priority: 0,
  status: 1,
  isFaq: 0,
  faqQuestion: ''
})
const faqEnabled = computed({
  get: () => form.isFaq === 1,
  set: (val) => {
    form.isFaq = val ? 1 : 0
    if (!val) form.faqQuestion = ''
  }
})
const rules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    category: '',
    title: '',
    content: '',
    keywords: '',
    priority: 0,
    status: 1,
    isFaq: 0,
    faqQuestion: ''
  })
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    category: row.category,
    title: row.title,
    content: row.content,
    keywords: row.keywords,
    priority: row.priority ?? 0,
    status: row.status ?? 1,
    isFaq: row.isFaq ?? 0,
    faqQuestion: row.faqQuestion || ''
  })
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...form, isFaq: form.isFaq === 1 ? 1 : 0 }
      const res = isEdit.value ? await updateKnowledge(payload) : await addKnowledge(payload)
      if (res.code === 1 || res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
        dialogVisible.value = false
        await Promise.all([loadData(), loadStats()])
      }
    } catch (e) {
      ElMessage.error('操作失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    const res = await updateKnowledge({ ...row, status: newStatus })
    if (res.code === 1 || res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      loadStats()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示', { type: 'warning' }).then(async () => {
    const res = await deleteKnowledge(row.id)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('删除成功')
      await Promise.all([loadData(), loadStats()])
    }
  }).catch(() => {})
}

const reloading = ref(false)
const handleReload = async () => {
  reloading.value = true
  try {
    const res = await reloadKnowledge()
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('知识库缓存已刷新，AI将使用最新数据')
      await Promise.all([loadData(), loadStats()])
    }
  } finally {
    reloading.value = false
  }
}

const searchDialogVisible = ref(false)
const testQuery = ref('')
const testTopK = ref(5)
const searching = ref(false)
const searched = ref(false)
const searchResults = ref([])

const handleSearch = async () => {
  if (!testQuery.value.trim()) return ElMessage.warning('请输入测试问题')
  searching.value = true
  searched.value = false
  try {
    const res = await searchKnowledge({ query: testQuery.value, topK: testTopK.value })
    if (res.code === 1 || res.code === 200) {
      searchResults.value = res.data || []
      searched.value = true
    }
  } finally {
    searching.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadStats()])
})
</script>

<style scoped>
.knowledge-page { padding: 20px; }
.stats-row, .analysis-row { margin-bottom: 16px; }
.search-card, .table-card { margin-bottom: 16px; }
.stat-card { min-height: 108px; }
.stat-label { font-size: 13px; color: #909399; }
.stat-value { font-size: 30px; font-weight: 700; color: #303133; margin-top: 8px; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.primary { color: #409eff; }
.stat-sub { font-size: 12px; color: #909399; margin-top: 4px; }
.analysis-card { min-height: 220px; }
.card-title { font-weight: 600; }
.analysis-list { display: flex; flex-direction: column; gap: 12px; }
.analysis-item { display: flex; justify-content: space-between; align-items: center; gap: 12px; border-bottom: 1px solid #f0f2f5; padding-bottom: 8px; }
.analysis-meta { color: #606266; font-size: 13px; text-align: right; }
.query-text { color: #303133; font-size: 14px; }
.search-results { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.result-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px 16px; background: #fafafa; }
.result-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.result-index { font-weight: 700; color: #409eff; font-size: 14px; min-width: 24px; }
.result-title { font-weight: 600; color: #303133; font-size: 14px; }
.result-content { font-size: 13px; color: #606266; line-height: 1.6; margin-bottom: 6px; white-space: pre-wrap; }
.result-keywords { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 4px; }
</style>
