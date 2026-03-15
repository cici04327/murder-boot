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
                <el-tag size="small" :type="getLevelType(row.level)">{{ row.levelName || getLevelName(row.level) }}</el-tag>
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
        <el-table-column label="预约折扣" width="110">
          <template #default="{ row }">
            <el-tag type="warning" size="small">
              {{ getLevelDiscountLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- VIP用户列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>VIP用户管理</span>
        </div>
      </template>
      <el-table :data="vipUserList" v-loading="vipUserLoading" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">{{ row.levelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="到期时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'text-warning': row.daysRemaining <= 3 && row.daysRemaining > 0 }">
              {{ row.endTime ? row.endTime.substring(0, 10) : '-' }}
            </span>
            <el-tag v-if="row.daysRemaining <= 3 && row.daysRemaining > 0" type="warning" size="small" style="margin-left:4px">
              {{ row.daysRemaining }}天后到期
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '生效中' : '已过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link @click="handleGrantCoupon(row)">补发体验券</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 补发月度体验券对话框 -->
    <el-dialog v-model="grantDialogVisible" title="手动补发月度体验券" width="440px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ grantForm.nickname }}（ID: {{ grantForm.userId }}）</span>
          <el-tag :type="getLevelType(grantForm.level)" size="small" style="margin-left:8px">{{ grantForm.levelName }}</el-tag>
        </el-form-item>
        <el-form-item label="补发月份">
          <el-date-picker
            v-model="grantForm.month"
            type="month"
            placeholder="选择补发月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="补发数量">
          <span class="grant-preview">
            {{ LEVEL_COUPON_CONFIG[grantForm.level]?.count || '-' }} 张 ×
            ¥{{ LEVEL_COUPON_CONFIG[grantForm.level]?.amount || '-' }} 元
          </span>
          <span style="color:#909399;font-size:12px;margin-left:8px">（按等级自动确定）</span>
        </el-form-item>
        <el-form-item label="补发原因">
          <el-input v-model="grantForm.reason" placeholder="请输入补发原因" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGrantSubmit" :loading="grantSubmitting">确认补发</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="会员等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择会员等级">
            <el-option label="见习侦探（9.5折）" :value="1" />
            <el-option label="银章侦探（9折）" :value="2" />
            <el-option label="金章侦探（8.5折）" :value="3" />
            <el-option label="传奇侦探（8折）" :value="4" />
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
        <el-form-item label="预约折扣">
          <el-tag type="warning">{{ { 1: '9.5折', 2: '9折', 3: '8.5折', 4: '8折' }[form.level] || '-' }}</el-tag>
          <span style="margin-left:10px;color:#909399;font-size:13px">
            折扣按会员等级自动生效，无需手动配置
          </span>
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
import { grantMonthlyCoupons } from '@/api/vip'

// 各等级月度体验券配置（与后端一致）
const LEVEL_COUPON_CONFIG = {
  1: { count: 2,  amount: 10  },
  2: { count: 5,  amount: 20  },
  3: { count: 10, amount: 50  },
  4: { count: 15, amount: 100 }
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增套餐')
const packageList = ref([])
const formRef = ref(null)

// VIP用户列表
const vipUserList = ref([])
const vipUserLoading = ref(false)

// 补发对话框
const grantDialogVisible = ref(false)
const grantSubmitting = ref(false)
const grantForm = reactive({
  userId: null,
  nickname: '',
  level: 1,
  levelName: '',
  month: '',
  reason: ''
})

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
  specialDiscount: null,
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
  const types = { 1: 'info', 2: '', 3: 'warning', 4: 'danger' }
  return types[level] || 'info'
}

const getLevelDiscountLabel = (level) => {
  const map = { 1: '9.5折', 2: '9折', 3: '8.5折', 4: '8折' }
  return map[level] || '-'
}

const getLevelName = (level) => {
  const map = { 1: '见习侦探', 2: '银章侦探', 3: '金章侦探', 4: '传奇侦探' }
  return map[level] || '未知'
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
  form.specialDiscount = null
  form.status = 1
}

const addFeature = () => {
  form.features.push('')
}

const removeFeature = (index) => {
  form.features.splice(index, 1)
}

const loadVipUsers = async () => {
  vipUserLoading.value = true
  try {
    const res = await userService.get('/admin/vip/users', {
      params: { page: 1, pageSize: 100, status: 1 }
    })
    if (res.code === 1 || res.code === 200) {
      vipUserList.value = res.data?.records || res.data || []
    }
  } catch (e) {
    console.error('加载VIP用户失败:', e)
  } finally {
    vipUserLoading.value = false
  }
}

const handleGrantCoupon = (row) => {
  // 默认补发当月
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  Object.assign(grantForm, {
    userId: row.userId,
    nickname: row.nickname || row.username || ('用户' + row.userId),
    level: row.level,
    levelName: row.levelName,
    month: `${y}-${m}`,
    reason: ''
  })
  grantDialogVisible.value = true
}

const handleGrantSubmit = async () => {
  if (!grantForm.month) {
    ElMessage.warning('请选择补发月份')
    return
  }
  if (!grantForm.reason.trim()) {
    ElMessage.warning('请填写补发原因')
    return
  }
  try {
    grantSubmitting.value = true
    const [year, month] = grantForm.month.split('-').map(Number)
    const res = await grantMonthlyCoupons(grantForm.userId, year, month, grantForm.reason)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success(`已成功为用户 ${grantForm.nickname} 补发 ${grantForm.month} 月度体验券，系统已发送通知`)
      grantDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '补发失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '补发失败，请重试')
  } finally {
    grantSubmitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadStats()
  loadVipUsers()
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

.text-warning { color: #E6A23C; font-weight: 600; }

.grant-preview {
  font-size: 15px;
  font-weight: 700;
  color: #409EFF;
}
</style>
