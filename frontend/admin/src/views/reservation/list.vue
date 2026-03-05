<template>
  <div class="reservation-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="用户名">
          <el-input v-model="queryForm.userName" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款筛选">
          <el-select v-model="queryForm.hasRefund" placeholder="请选择" clearable>
            <el-option label="有退款申请" :value="true" />
            <el-option label="无退款申请" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增预约</el-button>
          <el-button type="warning" @click="handleExport" :loading="exporting">导出Excel</el-button>
        </el-form-item>
      </el-form>

      <el-table 
        :data="tableData" 
        style="width: 100%" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="orderNo" label="订单号" width="120" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column prop="storeName" label="门店" width="130" />
        <el-table-column prop="roomName" label="房间" width="100" />
        <el-table-column prop="scriptName" label="剧本" width="130" />
        <el-table-column prop="reservationTime" label="预约时间" width="160" />
        <el-table-column prop="playerCount" label="人数" width="70" />
        <el-table-column prop="duration" label="时长(h)" width="80" />
        <el-table-column prop="totalPrice" label="原价" width="80">
          <template #default="{ row }">
            <span :style="row.discountAmount > 0 ? 'color:#c0c4cc;text-decoration:line-through;font-size:12px' : ''">
              ¥{{ row.totalPrice }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="实付/折扣" width="130">
          <template #default="{ row }">
            <div style="line-height:1.6">
              <span style="color:#f56c6c;font-weight:bold">¥{{ Number(row.actualAmount || row.totalPrice).toFixed(2) }}</span>
              <div v-if="row.vipDiscountAmount > 0" style="margin-top:2px">
                <el-tag type="warning" size="small">💎 {{ getVipDiscountLabel(row.vipDiscount) }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付" width="90">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : row.payStatus === 2 ? 'info' : 'warning'" size="small">
              {{ row.payStatus === 1 ? '已支付' : row.payStatus === 2 ? '已退款' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="success" link size="small" @click="handleEdit(row)" v-if="row.status === 0">编辑</el-button>
            <el-button type="info" link size="small" @click="handleComplete(row)" v-if="row.status === 1">完成</el-button>
            <el-button type="danger" link size="small" @click="handleCancel(row)" v-if="row.status === 0 || row.status === 1">取消</el-button>
            <el-button type="warning" link size="small" @click="handleDelete(row)" v-if="row.status === 3">删除</el-button>
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
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedRows = ref([])

const queryForm = reactive({
  userName: '',
  status: null,
  hasRefund: null,
  page: 1,
  pageSize: 10
})

const getVipDiscountLabel = (discount) => {
  if (!discount) return 'VIP折扣'
  const fold = Math.round(discount * 10)
  return `${fold}折`
}

const getStatusType = (status) => {
  const types = ['warning', 'success', 'info', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待支付', '已支付', '已完成', '已取消']
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    console.log('加载预约列表，参数:', queryForm)
    const res = await request.get('/reservation/page', { params: queryForm })
    const list = res.data?.records || []
    total.value = res.data?.total || 0
    tableData.value = list.map(it => ({
      id: it.id,
      orderNo: it.orderNo || it.reservationNo || it.id,
      userName: it.userName || it.contactName || '-',
      storeName: it.storeName || '-',
      roomName: it.roomName || '-',
      scriptName: it.scriptName || '-',
      reservationTime: it.reservationTime || it.createTime || '-',
      playerCount: it.playerCount || it.people || 0,
      duration: it.duration || 3.5,
      totalPrice: it.totalPrice || it.price || 0,
      actualAmount: it.actualAmount,
      discountAmount: it.discountAmount || 0,
      vipDiscountAmount: it.vipDiscountAmount || 0,
      vipDiscount: it.vipDiscount || null,
      couponId: it.couponId || null,
      payStatus: it.payStatus ?? 0,
      status: it.status ?? 0
    }))
  } catch (e) {
    ElMessage.error('加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (newPage) => {
  console.log('预约列表页码变化:', newPage)
  queryForm.page = newPage
  fetchData()
}

const handleSizeChange = (newSize) => {
  console.log('预约列表每页大小变化:', newSize)
  queryForm.pageSize = newSize
  queryForm.page = 1
  fetchData()
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.userName = ''
  queryForm.status = null
  queryForm.hasRefund = null
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  router.push('/reservation/add')
}

const handleView = (row) => {
  router.push(`/reservation/detail/${row.id}`)
}

const handleEdit = (row) => {
  router.push(`/reservation/edit/${row.id}`)
}

const handleComplete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要完成该预约吗？完成后将为用户增加100积分。', '提示', {
      type: 'success',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    loading.value = true
    await request.put(`/reservation/complete/${row.id}`)
    ElMessage.success('预约已完成，用户获得100积分')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成预约失败:', error)
      ElMessage.error('完成预约失败')
    }
  } finally {
    loading.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    loading.value = true
    await request.put(`/reservation/cancel/${row.id}`)
    ElMessage.success('预约已取消')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
      ElMessage.error('取消预约失败')
    }
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该预约记录吗？此操作不可恢复！', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    loading.value = true
    await request.delete(`/reservation/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleExport = async () => {
  exporting.value = true
  try {
    // 获取所有数据（不分页）
    const res = await request.get('/reservation/page', {
      params: {
        page: 1,
        pageSize: 10000,
        userName: queryForm.userName || undefined,
        status: queryForm.status
      }
    })

    const data = res.data?.records || []
    
    if (data.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }

    // 转换数据格式
    const exportData = data.map(item => ({
      '订单号': item.orderNo || item.reservationNo || item.id,
      '联系人': item.contactName || item.userName || '-',
      '联系电话': item.contactPhone || '-',
      '门店': item.storeName || '-',
      '房间': item.roomName || '-',
      '剧本': item.scriptName || '-',
      '预约时间': item.reservationTime || '-',
      '人数': item.playerCount || item.people || 0,
      '时长(小时)': item.duration || 3.5,
      '原价': item.totalPrice || item.price || 0,
      'VIP折扣': item.vipDiscountAmount > 0
        ? `${Math.round(item.vipDiscount * 10)}折(-¥${Number(item.vipDiscountAmount).toFixed(2)})`
        : '无',
      '优惠券折扣': item.couponId && item.discountAmount > 0
        ? `-¥${(Number(item.discountAmount) - Number(item.vipDiscountAmount || 0)).toFixed(2)}`
        : '无',
      '实付金额': Number(item.actualAmount || item.totalPrice || 0).toFixed(2),
      '支付状态': item.payStatus === 1 ? '已支付' : item.payStatus === 2 ? '已退款' : '未支付',
      '预约状态': getStatusText(item.status),
      '创建时间': item.createTime || '-'
    }))

    // 创建工作簿
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '预约列表')

    // 设置列宽
    const colWidths = [
      { wch: 15 }, // 订单号
      { wch: 10 }, // 联系人
      { wch: 15 }, // 联系电话
      { wch: 20 }, // 门店
      { wch: 12 }, // 房间
      { wch: 20 }, // 剧本
      { wch: 20 }, // 预约时间
      { wch: 8 },  // 人数
      { wch: 10 }, // 时长
      { wch: 10 }, // 原价
      { wch: 16 }, // VIP折扣
      { wch: 14 }, // 优惠券折扣
      { wch: 12 }, // 实付金额
      { wch: 10 }, // 支付状态
      { wch: 10 }, // 预约状态
      { wch: 20 }  // 创建时间
    ]
    worksheet['!cols'] = colWidths

    // 生成Excel文件
    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/octet-stream' })
    
    // 下载文件
    const fileName = `预约列表_${new Date().getTime()}.xlsx`
    saveAs(blob, fileName)
    
    ElMessage.success(`成功导出 ${data.length} 条数据`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  // 检查URL参数中是否有hasRefund
  if (route.query.hasRefund !== undefined) {
    queryForm.hasRefund = route.query.hasRefund === 'true' || route.query.hasRefund === true
  }
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
