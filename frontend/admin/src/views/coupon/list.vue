<template>
  <div class="coupon-list">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="优惠券名称">
          <el-input v-model="searchForm.name" placeholder="请输入优惠券名称" clearable />
        </el-form-item>
        <el-form-item label="优惠券类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable>
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="代金券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增优惠券</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="优惠券名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ row.typeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="150">
          <template #default="{ row }">
            <span v-if="row.type === 1">满{{ row.minAmount }}减{{ row.discountValue }}</span>
            <span v-else-if="row.type === 2">{{ (row.discountValue * 10).toFixed(1) }}折</span>
            <span v-else>代金券{{ row.discountValue }}元</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="发行数量" width="100" />
        <el-table-column prop="remainCount" label="剩余数量" width="100" />
        <el-table-column label="有效期" min-width="180">
          <template #default="{ row }">
            <div v-if="row.validStartTime && row.validEndTime">
              {{ formatDate(row.validStartTime) }}<br />至<br />{{ formatDate(row.validEndTime) }}
            </div>
            <span v-else>长期有效</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === 1 ? 'info' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠券类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" @change="handleTypeChange">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="代金券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠值" prop="discountValue">
          <el-input-number 
            v-model="form.discountValue" 
            :min="0" 
            :max="form.type === 2 ? 1 : 99999"
            :step="form.type === 2 ? 0.1 : 1"
            :precision="form.type === 2 ? 1 : 2"
            placeholder="请输入优惠值"
          />
          <span class="input-tip" v-if="form.type === 1">满减金额（元）</span>
          <span class="input-tip" v-else-if="form.type === 2">折扣比例（如0.8表示8折）</span>
          <span class="input-tip" v-else>代金券金额（元）</span>
        </el-form-item>
        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number 
            v-model="form.minAmount" 
            :min="0" 
            :precision="2"
            placeholder="请输入最低消费金额"
          />
          <span class="input-tip">元（0表示无门槛）</span>
        </el-form-item>
        <el-form-item label="发行数量" prop="totalCount">
          <el-input-number 
            v-model="form.totalCount" 
            :min="1" 
            placeholder="请输入发行数量"
          />
        </el-form-item>
        <el-form-item label="有效期" prop="validTime">
          <el-date-picker
            v-model="form.validTime"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入优惠券描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="优惠券详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="优惠券名称">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentRow.typeName }}</el-descriptions-item>
        <el-descriptions-item label="优惠内容">
          <span v-if="currentRow.type === 1">满{{ currentRow.minAmount }}减{{ currentRow.discountValue }}</span>
          <span v-else-if="currentRow.type === 2">{{ (currentRow.discountValue * 10).toFixed(1) }}折</span>
          <span v-else>代金券{{ currentRow.discountValue }}元</span>
        </el-descriptions-item>
        <el-descriptions-item label="最低消费">{{ currentRow.minAmount }}元</el-descriptions-item>
        <el-descriptions-item label="发行数量">{{ currentRow.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="剩余数量">{{ currentRow.remainCount }}</el-descriptions-item>
        <el-descriptions-item label="已领取">{{ currentRow.receivedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已使用">{{ currentRow.usedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="有效期开始">{{ formatDate(currentRow.validStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="有效期结束">{{ formatDate(currentRow.validEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentRow.status === 1 ? 'success' : 'info'">
            {{ currentRow.statusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentRow.description }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getCouponList, 
  getCouponStatistics, 
  addCoupon, 
  updateCoupon, 
  deleteCoupon, 
  updateCouponStatus 
} from '@/api/coupon'

const searchForm = reactive({
  name: '',
  type: null,
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const currentRow = ref({})

const form = reactive({
  id: null,
  name: '',
  type: null,
  discountValue: 0,
  minAmount: 0,
  totalCount: 1,
  validTime: [],
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  discountValue: [{ required: true, message: '请输入优惠值', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发行数量', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  try {
    const res = await getCouponList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    
    if (res.code === 1 || res.code === 200) {
      // 处理数据，添加类型和状态名称
      tableData.value = (res.data.records || []).map(item => ({
        ...item,
        typeName: item.type === 1 ? '满减券' : item.type === 2 ? '折扣券' : '代金券',
        statusName: item.status === 1 ? '上架' : '下架'
      }))
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    type: null,
    status: null
  })
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增优惠券'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑优惠券'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    discountValue: row.discountValue,
    minAmount: row.minAmount,
    totalCount: row.totalCount,
    validTime: row.validStartTime && row.validEndTime 
      ? [row.validStartTime, row.validEndTime] 
      : [],
    description: row.description,
    status: row.status
  })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getCouponStatistics(row.id)
    if (res.code === 1 || res.code === 200) {
      currentRow.value = {
        ...res.data,
        typeName: res.data.type === 1 ? '满减券' : res.data.type === 2 ? '折扣券' : '代金券',
        statusName: res.data.status === 1 ? '上架' : '下架'
      }
      detailVisible.value = true
    }
  } catch (error) {
    console.error('加载详情失败:', error)
    ElMessage.error('加载详情失败')
  }
}

const handleToggleStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    const res = await updateCouponStatus(row.id, newStatus)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('操作成功')
      loadData()
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该优惠券吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteCoupon(row.id)
      if (res.code === 1 || res.code === 200) {
        ElMessage.success('删除成功')
        loadData()
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }).catch(() => {})
}

const handleTypeChange = () => {
  form.discountValue = 0
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const submitData = {
          ...form,
          validStartTime: form.validTime[0] || null,
          validEndTime: form.validTime[1] || null
        }
        delete submitData.validTime

        let res
        if (form.id) {
          res = await updateCoupon(submitData)
        } else {
          res = await addCoupon(submitData)
        }
        
        if (res.code === 1 || res.code === 200) {
          ElMessage.success('操作成功')
          dialogVisible.value = false
          loadData()
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error(error.response?.data?.msg || '操作失败')
      }
    }
  })
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    type: null,
    discountValue: 0,
    minAmount: 0,
    totalCount: 1,
    validTime: [],
    description: '',
    status: 1
  })
  formRef.value?.resetFields()
}

const getTypeTagType = (type) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger' }
  return types[type] || ''
}

const formatDate = (date) => {
  return date || '-'
}
</script>

<style scoped>
.coupon-list {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.input-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
