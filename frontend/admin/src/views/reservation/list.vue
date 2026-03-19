<template>
  <div class="reservation-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="预约状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="核销状态">
          <el-select v-model="queryForm.checkInStatus" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="未核销" :value="0" />
            <el-option label="已核销" :value="1" />
          </el-select>
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
        <el-form-item label="退款申请">
          <el-select v-model="queryForm.hasRefund" clearable placeholder="全部" style="width: 140px">
            <el-option label="有退款申请" :value="true" />
            <el-option label="无退款申请" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增预约</el-button>
          <el-button type="warning" :loading="exporting" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="storeName" label="门店" min-width="140" />
        <el-table-column prop="roomName" label="房间" width="110" />
        <el-table-column prop="scriptName" label="剧本" min-width="140" />
        <el-table-column label="主持DM" width="130">
          <template #default="{ row }">
            <el-tag :type="row.dmName ? 'success' : 'info'" size="small">
              {{ row.dmName || '待分配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reservationTime" label="预约时间" width="170" />
        <el-table-column label="实付金额" width="100">
          <template #default="{ row }">
            {{ formatAmount(row.actualAmount || row.totalPrice) }}
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
        <el-table-column label="核销状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.checkInStatus === 1 ? '已核销' : '未核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销码" width="120">
          <template #default="{ row }">
            {{ row.payStatus === 1 ? (row.checkInCode || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="核销时间" width="170">
          <template #default="{ row }">
            {{ row.checkInTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="canCheckIn(row)"
              type="success"
              link
              size="small"
              @click="handleCheckIn(row)"
            >
              到店核销
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
            <el-button
              v-if="canCancel(row)"
              type="danger"
              link
              size="small"
              @click="handleCancel(row)"
            >
              取消
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)

const statusOptions = [
  { label: '待确认', value: 1 },
  { label: '已确认', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const queryForm = reactive({
  reservationDate: null,
  status: null,
  checkInStatus: null,
  hasRefund: null,
  page: 1,
  pageSize: 10
})

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 1: '待确认', 2: '已确认', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const getPayStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

const getPayStatusText = (status) => {
  const map = { 0: '未支付', 1: '已支付', 2: '退款中', 3: '已退款' }
  return map[status] || '未知'
}

const formatAmount = (amount) => `￥${Number(amount || 0).toFixed(2)}`

const canCheckIn = (row) => row.status === 2 && row.payStatus === 1 && row.checkInStatus !== 1

const canComplete = (row) => row.status === 2 && row.checkInStatus === 1

const canCancel = (row) => [1, 2].includes(row.status) && row.checkInStatus !== 1

const normalizeCheckInCode = (value) => String(value || '').replace(/\s+/g, '').replace(/[^\d]/g, '')

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: queryForm.page,
      pageSize: queryForm.pageSize,
      reservationDate: queryForm.reservationDate,
      status: queryForm.status,
      checkInStatus: queryForm.checkInStatus,
      hasRefund: queryForm.hasRefund
    }
    const res = await request.get('/reservation/page', { params })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.reservationDate = null
  queryForm.status = null
  queryForm.checkInStatus = null
  queryForm.hasRefund = null
  queryForm.page = 1
  fetchData()
}

const handleSizeChange = (size) => {
  queryForm.pageSize = size
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  router.push('/reservation/add')
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
      await ElMessageBox.alert(error.message || '核销失败，请稍后重试', '核销失败', {
        type: 'error'
      })
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

const handleCancel = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', {
      inputValue: '',
      confirmButtonText: '确认取消',
      cancelButtonText: '返回'
    })
    await request({
      url: `/reservation/${row.id}/cancel`,
      method: 'put',
      params: { reason: value || '' }
    })
    ElMessage.success('预约已取消')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消预约失败')
    }
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await request.get('/reservation/page', {
      params: {
        page: 1,
        pageSize: 10000,
        reservationDate: queryForm.reservationDate,
        status: queryForm.status,
        checkInStatus: queryForm.checkInStatus,
        hasRefund: queryForm.hasRefund
      }
    })
    const rows = res.data?.records || []

    if (!rows.length) {
      ElMessage.warning('暂无可导出数据')
      return
    }

    const exportData = rows.map((item) => ({
      订单号: item.orderNo,
      联系人: item.contactName || '',
      联系电话: item.contactPhone || '',
      门店: item.storeName || '',
      房间: item.roomName || '',
      剧本: item.scriptName || '',
      预约时间: item.reservationTime || '',
      实付金额: Number(item.actualAmount || item.totalPrice || 0).toFixed(2),
      支付状态: getPayStatusText(item.payStatus),
      预约状态: getStatusText(item.status),
      核销状态: Number(item.checkInStatus || 0) === 1 ? '已核销' : '未核销',
      核销码: item.payStatus === 1 ? (item.checkInCode || '') : '',
      核销时间: item.checkInTime || ''
    }))

    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '预约列表')
    const buffer = XLSX.write(workbook, { type: 'array', bookType: 'xlsx' })
    saveAs(new Blob([buffer], { type: 'application/octet-stream' }), `预约列表_${Date.now()}.xlsx`)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.reservation-list {
  width: 100%;
}

.query-form {
  margin-bottom: 20px;
}
</style>
