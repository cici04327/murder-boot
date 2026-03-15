<template>
  <div class="dm-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item v-if="!isStoreAdmin" label="门店">
          <el-select v-model="searchForm.storeId" placeholder="全部门店" clearable style="width:180px" @change="fetchData">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:120px" @change="fetchData">
            <el-option label="在职" :value="1" />
            <el-option label="离职" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openDialog()">新增 DM</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" border stripe row-key="id">
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.avatar" :icon="UserFilled" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="DM 姓名" min-width="100" />
        <el-table-column prop="storeName" label="所属门店" min-width="120" />
        <el-table-column label="风格标签" min-width="160">
          <template #default="{ row }">
            <el-tag
              v-for="tag in (row.styleTagList || [])"
              :key="tag"
              size="small"
              style="margin:2px"
            >{{ tag }}</el-tag>
            <span v-if="!row.styleTagList?.length" class="text-gray">暂无</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100" align="center">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.rating)" disabled show-score size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="gameCount" label="主持场次" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >{{ row.status === 1 ? '离职' : '复职' }}</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑 DM' : '新增 DM'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item v-if="!isStoreAdmin" label="所属门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width:100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="DM 姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入 DM 姓名或艺名" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="form.avatar" placeholder="可填入头像图片地址" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="3"
            placeholder="请输入 DM 个人简介"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="风格标签">
          <el-checkbox-group v-model="selectedTags">
            <el-checkbox v-for="tag in tagOptions" :key="tag" :label="tag">{{ tag }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">在职</el-radio>
            <el-radio :label="0">离职</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, UserFilled } from '@element-plus/icons-vue'
import { getDmPage, addDm, updateDm, deleteDm, updateDmStatus } from '@/api/dm'
import request from '@/utils/request'

// ------------------------------------------------------------------ 数据
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const storeOptions = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const tagOptions = ['推理型', '情感型', '恐怖型', '欢乐型', '机制型', '古风型', '全能型']

// 门店管理员身份
const loginType = localStorage.getItem('admin-login-type') || 'admin'
const isStoreAdmin = loginType === 'store'
const fixedStoreId = isStoreAdmin ? Number(localStorage.getItem('admin-store-id')) : null

const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ storeId: fixedStoreId || null, status: null })

const form = reactive({
  storeId: null,
  name: '',
  avatar: '',
  introduction: '',
  styleTags: '',
  status: 1
})
const selectedTags = ref([])

const rules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  name: [{ required: true, message: '请输入 DM 姓名', trigger: 'blur' }]
}

// ------------------------------------------------------------------ 查询
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDmPage({
      storeId: fixedStoreId || searchForm.storeId,
      status: searchForm.status,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取 DM 列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStores = async () => {
  try {
    const res = await request({ url: '/store/list', method: 'get', params: { page: 1, pageSize: 200 } })
    storeOptions.value = res.data?.records || res.data?.list || []
  } catch (e) {
    // 静默
  }
}

const resetSearch = () => {
  searchForm.storeId = null
  searchForm.status = null
  pagination.page = 1
  fetchData()
}

// ------------------------------------------------------------------ 对话框
const openDialog = (row = null) => {
  editingId.value = row?.id || null
  if (row) {
    Object.assign(form, {
      storeId: row.storeId,
      name: row.name,
      avatar: row.avatar || '',
      introduction: row.introduction || '',
      status: row.status
    })
    selectedTags.value = row.styleTagList || []
  } else {
    // 门店管理员新增时自动填充门店ID
    Object.assign(form, { storeId: fixedStoreId || null, name: '', avatar: '', introduction: '', status: 1 })
    selectedTags.value = []
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    form.styleTags = selectedTags.value.join(',')
    const payload = editingId.value
      ? { ...form, id: editingId.value }
      : { ...form }
    editingId.value ? await updateDm(payload) : await addDm(payload)
    ElMessage.success(editingId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// ------------------------------------------------------------------ 状态 / 删除
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 0 ? '离职' : '复职'
  try {
    await ElMessageBox.confirm(`确认将 ${row.name} 设为${label}？`, '提示', { type: 'warning' })
    await updateDmStatus(row.id, newStatus)
    ElMessage.success(`已设为${label}`)
    fetchData()
  } catch (e) { /* 取消 */ }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除 DM【${row.name}】？`, '提示', { type: 'warning' })
    await deleteDm(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* 取消 */ }
}

// ------------------------------------------------------------------ 初始化
onMounted(() => {
  fetchStores()
  fetchData()
})
</script>

<style scoped>
.dm-manage { padding: 16px; }
.search-card { margin-bottom: 0; }
.text-gray { color: #999; font-size: 12px; }
</style>
