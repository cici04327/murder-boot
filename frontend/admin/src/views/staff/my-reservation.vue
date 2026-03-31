<template>
  <div class="my-reservation-page">
    <div class="page-header">
      <div>
        <div class="page-title">我的预约</div>
        <div class="page-subtitle">只展示当前 DM 负责场次下的预约记录</div>
      </div>
      <div class="page-actions">
        <el-button v-if="hasScheduleFilter" @click="goBackToSchedules">返回我的场次</el-button>
      </div>
    </div>

    <el-alert
      v-if="hasScheduleFilter"
      class="schedule-alert"
      type="info"
      :closable="false"
      show-icon
    >
      <template #default>
        当前查看场次：
        <strong>{{ route.query.scheduleName || '指定场次' }}</strong>
        <span v-if="route.query.timeRange">（{{ route.query.timeRange }}）</span>
      </template>
    </el-alert>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">预约总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.pendingCheckIn }}</div>
        <div class="stat-label">待核销</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.pendingComplete }}</div>
        <div class="stat-label">待完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.completed }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <el-card>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="快捷筛选">
          <el-radio-group v-model="quickFilter" @change="applyQuickFilter">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pendingCheckIn">待核销</el-radio-button>
            <el-radio-button label="pendingComplete">待完成</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="queryForm.reservationDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="预约状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="待确认" :value="1" />
            <el-option label="已确认" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="核销状态">
          <el-select v-model="queryForm.checkInStatus" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="未核销" :value="0" />
            <el-option label="已核销" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="scriptName" label="剧本" min-width="140" />
        <el-table-column prop="roomName" label="房间" width="110" />
        <el-table-column prop="reservationTime" label="预约时间" width="170" />
        <el-table-column label="人数" width="80">
          <template #default="{ row }">
            {{ row.playerCount || 1 }}人
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getPayStatusType(row.payStatus)" size="small">
              {{ getPayStatusText(row.payStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.checkInStatus || 0) === 1 ? 'success' : 'info'" size="small">
              {{ Number(row.checkInStatus || 0) === 1 ? '已核销' : '未核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销码" width="110">
          <template #default="{ row }">
            {{ Number(row.payStatus) === 1 ? (row.checkInCode || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="canCheckIn(row)"
              type="success"
              link
              size="small"
              @click="handleCheckIn(row)"
            >
              核销
            </el-button>
            <el-button
              v-if="canComplete(row)"
              type="warning"
              link
              size="small"
              @click="handleComplete(row)"
            >
              完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="fetchData"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { hasPermissionCode } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const adminUser = JSON.parse(localStorage.getItem('admin-user') || '{}')
const permissionCodes = new Set(
  String(adminUser.permissionCodes || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
)

const queryForm = reactive({
  reservationDate: null,
  status: null,
  checkInStatus: null,
  page: 1,
  pageSize: 10
})
const quickFilter = ref('all')

const hasScheduleFilter = computed(() => Boolean(route.query.scheduleId))
const scheduleId = computed(() => {
  const value = route.query.scheduleId
  return value ? Number(value) : null
})

const stats = computed(() => ({
  total: tableData.value.length,
  pendingCheckIn: tableData.value.filter(item => Number(item.status) === 2 && Number(item.payStatus) === 1 && Number(item.checkInStatus || 0) !== 1).length,
  pendingComplete: tableData.value.filter(item => Number(item.status) === 2 && Number(item.checkInStatus || 0) === 1).length,
  completed: tableData.value.filter(item => Number(item.status) === 3).length
}))

const getStatusType = (status) => {
  const mapping = { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }
  return mapping[status] || 'info'
}

const getStatusText = (status) => {
  const mapping = { 1: '待确认', 2: '已确认', 3: '已完成', 4: '已取消' }
  return mapping[status] || '未知'
}

const getPayStatusType = (status) => {
  const mapping = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return mapping[status] || 'info'
}

const getPayStatusText = (status) => {
  const mapping = { 0: '未支付', 1: '已支付', 2: '退款中', 3: '已退款' }
  return mapping[status] || '未知'
}

const canCheckIn = (row) => Number(row.status) === 2 && Number(row.payStatus) === 1 && Number(row.checkInStatus || 0) !== 1 && hasPermissionCode('reservation:checkin')

const canComplete = (row) => Number(row.status) === 2
  && Number(row.checkInStatus || 0) === 1
  && permissionCodes.has('reservation:complete')

const normalizeCheckInCode = (value) => String(value || '').replace(/\s+/g, '').replace(/[^\d]/g, '')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/reservation/page', {
      params: {
        page: queryForm.page,
        pageSize: queryForm.pageSize,
        reservationDate: queryForm.reservationDate,
        scheduleId: scheduleId.value,
        status: queryForm.status,
        checkInStatus: queryForm.checkInStatus
      }
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载我的预约失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.status = null
  queryForm.checkInStatus = null
  queryForm.page = 1
  quickFilter.value = 'all'
  queryForm.reservationDate = route.query.reservationDate ? String(route.query.reservationDate) : null
  fetchData()
}

const handleSizeChange = (size) => {
  queryForm.pageSize = size
  queryForm.page = 1
  fetchData()
}

const handleView = (row) => {
  router.push(`/reservation/detail/${row.id}`)
}

const handleCheckIn = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入用户提供的核销码，订单号：${row.orderNo}`,
      '到店核销',
      {
        inputValue: row.checkInCode || '',
        closeOnClickModal: false,
        inputValidator: (inputValue) => {
          const code = normalizeCheckInCode(inputValue)
          if (!code) return '请输入核销码'
          if (code.length !== 6) return '核销码必须是 6 位数字'
          return true
        },
        confirmButtonText: '确认核销',
        cancelButtonText: '取消'
      }
    )

    await request({
      url: `/reservation/${row.id}/check-in`,
      method: 'put',
      params: { checkInCode: normalizeCheckInCode(value) }
    })
    ElMessage.success('核销成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '核销失败，请稍后重试')
    }
  }
}

const handleComplete = async (row) => {
  try {
    await ElMessageBox.confirm('确认将该预约标记为已完成吗？', '完成预约', {
      type: 'warning'
    })
    await request.put(`/reservation/${row.id}/complete`)
    ElMessage.success('预约已完成')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成预约失败')
    }
  }
}

const goBackToSchedules = () => {
  router.push({
    path: '/staff/my-schedule'
  })
}

const applyQuickFilter = () => {
  if (quickFilter.value === 'pendingCheckIn') {
    queryForm.status = 2
    queryForm.checkInStatus = 0
  } else if (quickFilter.value === 'pendingComplete') {
    queryForm.status = 2
    queryForm.checkInStatus = 1
  } else {
    queryForm.status = null
    queryForm.checkInStatus = null
  }
  queryForm.page = 1
  fetchData()
}

watch(
  () => route.query,
  () => {
    queryForm.page = 1
    queryForm.reservationDate = route.query.reservationDate ? String(route.query.reservationDate) : null
    fetchData()
  },
  { immediate: true }
)
</script>

<style scoped>
.my-reservation-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2d3d;
}

.page-subtitle {
  margin-top: 6px;
  color: #8492a6;
  font-size: 13px;
}

.schedule-alert {
  border-radius: 14px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffffff 0%, #f6f8fc 100%);
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.08);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.query-form {
  margin-bottom: 20px;
}
</style>
