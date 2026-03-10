<template>
  <div class="reservation-detail">
    <el-page-header content="预约详情" @back="goBack" />

    <el-card v-loading="loading" style="margin-top: 20px">
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="订单号">{{ reservation.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <el-tag :type="getStatusType(reservation.status)">
            {{ getStatusText(reservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ reservation.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ reservation.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间">{{ reservation.roomName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="剧本">{{ reservation.scriptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ reservation.reservationTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ reservation.playerCount || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="时长">{{ reservation.duration || 3 }} 小时</el-descriptions-item>
        <el-descriptions-item label="实付金额">
          {{ formatAmount(reservation.actualAmount || reservation.totalPrice) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPayStatusType(reservation.payStatus)">
            {{ getPayStatusText(reservation.payStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销状态">
          <el-tag :type="reservation.checkInStatus === 1 ? 'success' : 'info'">
            {{ reservation.checkInStatus === 1 ? '已核销' : '未核销' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销码">{{ reservation.checkInCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="核销时间">{{ reservation.checkInTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ reservation.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ reservation.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ reservation.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button v-if="canCheckIn" type="success" @click="handleCheckIn">到店核销</el-button>
        <el-button v-if="canComplete" type="warning" @click="handleComplete">完成预约</el-button>
        <el-button v-if="canCancel" type="danger" @click="handleCancel">取消预约</el-button>
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref({})

const reservationId = computed(() => route.params.id)

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

const canCheckIn = computed(() => reservation.value.status === 2 && reservation.value.payStatus === 1 && reservation.value.checkInStatus !== 1)

const canComplete = computed(() => reservation.value.status === 2 && reservation.value.checkInStatus === 1)

const canCancel = computed(() => [1, 2].includes(reservation.value.status) && reservation.value.checkInStatus !== 1)

const normalizeCheckInCode = (value) => String(value || '').replace(/\s+/g, '').replace(/[^\d]/g, '')

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/reservation/${reservationId.value}`)
    reservation.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const handleCheckIn = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入用户提供的核销码', '到店核销', {
      inputValue: reservation.value.checkInCode || '',
      closeOnClickModal: false,
      inputValidator: (inputValue) => {
        const code = normalizeCheckInCode(inputValue)
        if (!code) return '请输入核销码'
        if (code.length !== 6) return '核销码必须是 6 位数字'
        return true
      }
    })
    await request({
      url: `/reservation/${reservationId.value}/check-in`,
      method: 'put',
      params: { checkInCode: normalizeCheckInCode(value) }
    })
    ElMessage.success('核销成功')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      await ElMessageBox.alert(error.message || '核销失败，请稍后重试', '核销失败', {
        type: 'error'
      })
    }
  }
}

const handleComplete = async () => {
  try {
    await ElMessageBox.confirm('确认将该预约标记为已完成吗？', '完成预约', {
      type: 'warning'
    })
    await request.put(`/reservation/${reservationId.value}/complete`)
    ElMessage.success('预约已完成')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成预约失败')
    }
  }
}

const handleCancel = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约')
    await request({
      url: `/reservation/${reservationId.value}/cancel`,
      method: 'put',
      params: { reason: value || '' }
    })
    ElMessage.success('预约已取消')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消预约失败')
    }
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.reservation-detail {
  padding: 20px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}
</style>
