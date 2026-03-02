<template>
  <div class="room-management">
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
        <el-form-item label="房间类型">
          <el-select v-model="queryForm.type" placeholder="请选择类型" clearable>
            <el-option label="小房" :value="1" />
            <el-option label="中房" :value="2" />
            <el-option label="大房" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="可用" :value="1" />
            <el-option label="不可用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增房间</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="房间名称" width="150" />
        <el-table-column prop="type" label="房间类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoomTypeColor(row.type)">
              {{ getRoomTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容纳人数" width="100" align="center">
          <template #default="{ row }">
            {{ row.capacity }} 人
          </template>
        </el-table-column>
        <el-table-column prop="description" label="房间描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '可用' : '不可用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleChangeStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
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
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="请选择门店" filterable>
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="房间类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择房间类型">
            <el-option label="小房" :value="1" />
            <el-option label="中房" :value="2" />
            <el-option label="大房" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number
            v-model="formData.capacity"
            :min="1"
            :max="50"
            :step="1"
            placeholder="请输入容纳人数"
          />
        </el-form-item>
        <el-form-item label="房间描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入房间描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">可用</el-radio>
            <el-radio :label="0">不可用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
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
const dialogVisible = ref(false)
const dialogTitle = ref('新增房间')
const formRef = ref(null)
const storeList = ref([])

const queryForm = reactive({
  storeId: null,
  type: null,
  status: null,
  page: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  storeId: null,
  name: '',
  type: null,
  capacity: null,
  description: '',
  status: 1
})

const formRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  name: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择房间类型', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }]
}

const getRoomTypeText = (type) => {
  const map = { 1: '小房', 2: '中房', 3: '大房' }
  return map[type] || '未知'
}

const getRoomTypeColor = (type) => {
  const map = { 1: '', 2: 'success', 3: 'warning' }
  return map[type] || ''
}

const fetchStoreList = async () => {
  try {
    const res = await request.get('/store/list')
    storeList.value = res.data
  } catch (error) {
    console.error('获取门店列表失败:', error)
  }
}

const fetchData = async () => {
  if (!queryForm.storeId) {
    ElMessage.warning('请先选择门店')
    return
  }
  
  loading.value = true
  try {
    const res = await request.get(`/store/room/list/${queryForm.storeId}`)
    let data = res.data || []
    
    // 前端过滤
    if (queryForm.type !== null) {
      data = data.filter(item => item.type === queryForm.type)
    }
    if (queryForm.status !== null) {
      data = data.filter(item => item.status === queryForm.status)
    }
    
    tableData.value = data
    total.value = data.length
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  fetchData()
}

const handleReset = () => {
  queryForm.type = null
  queryForm.status = null
  if (queryForm.storeId) {
    fetchData()
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增房间'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑房间'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleChangeStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '停用'
  
  try {
    await ElMessageBox.confirm(`确定要${actionText}该房间吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.put('/store/room', {
      ...row,
      status: newStatus
    })
    ElMessage.success(`${actionText}成功`)
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该房间吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/store/room/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (formData.id) {
      await request.put('/store/room', formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/store/room', formData)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
    if (error !== false) {
      ElMessage.error('提交失败')
    }
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    storeId: null,
    name: '',
    type: null,
    capacity: null,
    description: '',
    status: 1
  })
}

onMounted(() => {
  fetchStoreList()
})
</script>

<style scoped>
.room-management {
  width: 100%;
}

.query-form {
  margin-bottom: 20px;
}
</style>
