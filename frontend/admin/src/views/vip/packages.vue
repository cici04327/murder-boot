<template>
  <div class="vip-packages-container">
    <!-- 顶部统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF;">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalVipUsers || 0 }}</div>
              <div class="stat-label">VIP会员总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A;">
              <el-icon :size="28"><ShoppingBag /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalPackages || 0 }}</div>
              <div class="stat-label">套餐总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C;">
              <el-icon :size="28"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.monthlyRevenue || 0 }}</div>
              <div class="stat-label">本月收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #F56C6C;">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.growthRate || 0 }}%</div>
              <div class="stat-label">增长率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 套餐列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>VIP套餐管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增套餐
          </el-button>
        </div>
      </template>

      <el-table :data="packageList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="套餐名称" width="150">
          <template #default="{ row }">
            <div class="package-info">
              <div class="package-name">{{ row.name }}</div>
              <div class="package-level">
                <el-tag size="small" :type="getLevelType(row.level)">{{ row.levelName }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="currentPrice" label="价格" width="120">
          <template #default="{ row }">
            <div class="price-info">
              <div class="current-price">¥{{ row.currentPrice }}</div>
              <div class="original-price" v-if="row.originalPrice">¥{{ row.originalPrice }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="durationText" label="有效期" width="100">
          <template #default="{ row }">
            {{ row.durationText || (row.durationDays + '天') }}
          </template>
        </el-table-column>
        <el-table-column prop="features" label="权益" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="(f, i) in (row.features || []).slice(0, 3)" :key="i" size="small" style="margin-right: 5px;">
              {{ f }}
            </el-tag>
            <span v-if="(row.features || []).length > 3">...</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="会员等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择会员等级">
            <el-option label="银卡会员" :value="1" />
            <el-option label="金卡会员" :value="2" />
            <el-option label="钻石会员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐价格" prop="currentPrice">
          <el-input-number v-model="form.currentPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="有效期(天)" prop="durationDays">
          <el-input-number v-model="form.durationDays" :min="1" />
        </el-form-item>
        <el-form-item label="积分倍率">
          <el-input-number v-model="form.pointMultiplier" :min="1" :max="10" :precision="1" />
        </el-form-item>
        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入套餐描述" />
        </el-form-item>
        <el-form-item label="套餐权益" prop="features">
          <div class="features-editor">
            <div v-for="(f, i) in form.features" :key="i" class="feature-item">
              <el-input v-model="form.features[i]" style="width: 80%;" />
              <el-button type="danger" link @click="removeFeature(i)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addFeature">+ 添加权益</el-button>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ShoppingBag, Money, TrendCharts, Plus } from '@element-plus/icons-vue'
import { userService } from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增套餐')
const packageList = ref([])
const formRef = ref(null)

const stats = reactive({
  totalVipUsers: 0,
  totalPackages: 0,
  monthlyRevenue: 0,
  growthRate: 0
})

const form = reactive({
  id: null,
  name: '',
  level: 1,
  currentPrice: 0,
  originalPrice: 0,
  durationDays: 30,
  durationType: 2, // 1-月 2-年 3-永久
  features: [],
  description: '',
  pointMultiplier: 1.0,
  couponCount: 0,
  priorityBooking: false,
  exclusiveService: false,
  birthdayGift: false,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择会员等级', trigger: 'change' }],
  currentPrice: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }],
  durationDays: [{ required: true, message: '请输入有效期', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await userService.get('/admin/vip/packages', {
      params: { page: 1, pageSize: 100 }
    })
    if (res.code === 1 || res.code === 200) {
      packageList.value = res.data?.records || res.data || []
      stats.totalPackages = packageList.value.length
    }
  } catch (e) {
    console.error('加载套餐失败:', e)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await userService.get('/admin/vip/statistics')
    if (res.code === 1 || res.code === 200) {
      Object.assign(stats, res.data)
    }
  } catch (e) {
    console.error('加载统计失败:', e)
  }
}

const getLevelType = (level) => {
  const types = { 1: '', 2: 'warning', 3: 'danger' }
  return types[level] || ''
}

const handleAdd = () => {
  dialogTitle.value = '新增套餐'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑套餐'
  Object.assign(form, { ...row, features: [...(row.features || [])] })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该套餐吗?', '提示', { type: 'warning' })
    await userService.delete(`/admin/vip/packages/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row) => {
  try {
    await userService.put(`/admin/vip/packages/${row.id}/status`, null, {
      params: { status: row.status }
    })
    ElMessage.success('状态更新成功')
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (form.id) {
      await userService.put('/admin/vip/packages', form)
    } else {
      await userService.post('/admin/vip/packages', form)
    }
    
    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.level = 1
  form.currentPrice = 0
  form.originalPrice = 0
  form.durationDays = 30
  form.durationType = 2
  form.features = []
  form.description = ''
  form.pointMultiplier = 1.0
  form.couponCount = 0
  form.priorityBooking = false
  form.exclusiveService = false
  form.birthdayGift = false
  form.status = 1
}

const addFeature = () => {
  form.features.push('')
}

const removeFeature = (index) => {
  form.features.splice(index, 1)
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.vip-packages-container {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.package-info .package-name {
  font-weight: bold;
  margin-bottom: 4px;
}

.price-info .current-price {
  font-size: 16px;
  font-weight: bold;
  color: #F56C6C;
}

.price-info .original-price {
  font-size: 12px;
  color: #909399;
  text-decoration: line-through;
}

.features-editor {
  width: 100%;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
</style>
