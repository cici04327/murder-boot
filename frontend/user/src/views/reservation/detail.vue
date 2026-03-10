<template>
  <div class="reservation-detail-container" v-loading="loading">
    <el-card v-if="reservation">
      <template #header>
        <div class="header">
          <h2>预约详情</h2>
          <el-tag :type="getStatusType(reservation.status)" size="large">
            {{ getStatusText(reservation.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions title="订单信息" :column="2" border>
        <el-descriptions-item label="订单号">{{ reservation.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ reservation.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPayStatusType(reservation.payStatus)">
            {{ getPayStatusText(reservation.payStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ reservation.payTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="预约信息" :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="剧本名称" :span="2">
          <el-link type="primary" @click="goToScript">{{ reservation.scriptName || scriptInfo?.name }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="门店名称" :span="2">
          <el-link type="primary" @click="goToStore">{{ reservation.storeName || storeInfo?.name }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="门店地址" :span="2">
          {{ reservation.storeAddress || storeInfo?.address || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="房间">{{ reservation.roomName || roomInfo?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间容量">{{ reservation.roomCapacity || roomInfo?.capacity || '-' }} 人</el-descriptions-item>
        <el-descriptions-item label="预约时间" :span="2">
          <span class="time-highlight">{{ reservation.reservationTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ reservation.playerCount }} 人</el-descriptions-item>
        <el-descriptions-item label="预计时长">{{ reservation.duration || scriptInfo?.duration || 3 }} 小时</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ reservation.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="reservation.remark" label="备注" :span="2">
          {{ reservation.remark }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="到店核销" :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="核销状态">
          <el-tag :type="reservation.checkInStatus === 1 ? 'success' : 'info'">
            {{ reservation.checkInStatus === 1 ? '已核销' : '未核销' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销时间">
          {{ reservation.checkInTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="核销码" :span="2">
          <div v-if="reservation.payStatus === 1 || reservation.checkInCode" class="check-in-code">
            <span>{{ reservation.checkInCode || '-' }}</span>
            <span class="check-in-tip">到店后出示给门店工作人员进行核销</span>
          </div>
          <span v-else>支付成功后可查看核销码</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="价格信息" :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="商品原价" :span="2">
          <span :style="reservation.discountAmount > 0 ? 'color:#909399;text-decoration:line-through' : 'font-weight:bold'">
            ￥{{ Number(reservation.totalPrice || 0).toFixed(2) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item v-if="reservation.vipDiscountAmount > 0" label="VIP 折扣">
          <el-tag type="warning" size="small" style="margin-right: 6px">
            {{ getVipDiscountLabel(reservation.vipDiscount) }}
          </el-tag>
          <span style="color:#e6a23c;font-weight:bold">
            -￥{{ Number(reservation.vipDiscountAmount || 0).toFixed(2) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item v-if="reservation.discountAmount > 0 && reservation.couponId" label="优惠券抵扣">
          <span style="color:#67c23a;font-weight:bold">
            -￥{{ (Number(reservation.discountAmount || 0) - Number(reservation.vipDiscountAmount || 0)).toFixed(2) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="实付金额" :span="2">
          <span class="amount-highlight">
            ￥{{ Number(reservation.actualAmount || reservation.totalPrice || 0).toFixed(2) }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button
          v-if="reservation.status === 1 && reservation.payStatus === 0"
          type="primary"
          size="large"
          @click="handlePay"
        >
          立即支付
        </el-button>
        <el-button
          v-if="reservation.status === 3"
          type="primary"
          size="large"
          @click="handleReview"
        >
          评价订单
        </el-button>
        <el-button
          v-if="reservation.status < 3 && reservation.status !== 4 && reservation.checkInStatus !== 1"
          type="danger"
          size="large"
          @click="handleCancel"
        >
          取消预约
        </el-button>
        <el-button
          v-if="reservation.status === 2"
          size="large"
          @click="showContactDialog = true"
        >
          查看门店联系方式
        </el-button>
        <el-button size="large" @click="router.back()">返回</el-button>
      </div>

      <el-card v-if="timeline.length > 0" style="margin-top: 20px">
        <template #header>
          <span>订单进度</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in timeline"
            :key="index"
            :timestamp="item.time"
            :type="item.type"
          >
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </el-card>

    <el-dialog v-model="showCancelDialog" title="取消预约" width="400px">
      <el-form>
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="4"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCancelDialog = false">返回</el-button>
        <el-button type="primary" :loading="canceling" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showContactDialog" title="门店联系方式" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="门店名称">{{ storeInfo?.name || reservation.storeName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ storeInfo?.phone || reservation.storePhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="门店地址">{{ storeInfo?.address || reservation.storeAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">
          {{ storeInfo?.openTime || '-' }} - {{ storeInfo?.closeTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="showContactDialog = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cancelReservation, getReservationDetail } from '@/api/reservation'
import { getScriptDetail } from '@/api/script'
import { getStoreDetail } from '@/api/store'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const canceling = ref(false)
const reservation = ref(null)
const scriptInfo = ref(null)
const storeInfo = ref(null)
const roomInfo = ref(null)
const showCancelDialog = ref(false)
const showContactDialog = ref(false)
const cancelReason = ref('')

const timeline = computed(() => {
  if (!reservation.value) return []

  const items = [
    {
      time: reservation.value.createTime,
      content: '订单创建',
      type: 'primary'
    }
  ]

  if (reservation.value.payTime) {
    items.push({
      time: reservation.value.payTime,
      content: '支付成功',
      type: 'success'
    })
  }

  if (reservation.value.checkInTime) {
    items.push({
      time: reservation.value.checkInTime,
      content: '到店核销成功',
      type: 'success'
    })
  }

  if (reservation.value.status === 3) {
    items.push({
      time: reservation.value.updateTime,
      content: '预约已完成',
      type: 'success'
    })
  }

  if (reservation.value.status === 4) {
    items.push({
      time: reservation.value.updateTime,
      content: `预约已取消${reservation.value.remark ? `：${reservation.value.remark}` : ''}`,
      type: 'danger'
    })
  }

  return items
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

const getVipDiscountLabel = (discount) => {
  if (!discount) return 'VIP 折扣'
  return `会员 ${Math.round(discount * 10)} 折`
}

const loadReservation = async () => {
  loading.value = true
  try {
    const res = await getReservationDetail(route.params.id)
    reservation.value = res.data || null
    await Promise.all([loadScriptInfo(), loadStoreInfo()])
  } catch (error) {
    ElMessage.error(error.message || '加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const loadScriptInfo = async () => {
  if (!reservation.value?.scriptId) return
  try {
    const res = await getScriptDetail(reservation.value.scriptId)
    scriptInfo.value = res.data || null
  } catch (error) {
    console.error(error)
  }
}

const loadStoreInfo = async () => {
  if (!reservation.value?.storeId) return
  try {
    const res = await getStoreDetail(reservation.value.storeId)
    storeInfo.value = res.data || null
    if (res.data?.rooms) {
      roomInfo.value = res.data.rooms.find((item) => item.id === reservation.value.roomId) || null
    }
  } catch (error) {
    console.error(error)
  }
}

const handlePay = () => {
  router.push(`/payment/${reservation.value.id}`)
}

const handleCancel = () => {
  cancelReason.value = ''
  showCancelDialog.value = true
}

const confirmCancel = async () => {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }
  canceling.value = true
  try {
    await cancelReservation(reservation.value.id, cancelReason.value)
    ElMessage.success('取消成功')
    showCancelDialog.value = false
    loadReservation()
  } catch (error) {
    ElMessage.error(error.message || '取消预约失败')
  } finally {
    canceling.value = false
  }
}

const handleReview = () => {
  router.push({
    path: `/script/${reservation.value.scriptId}`,
    query: { review: true, reservationId: reservation.value.id }
  })
}

const goToScript = () => {
  if (reservation.value?.scriptId) {
    router.push(`/script/${reservation.value.scriptId}`)
  }
}

const goToStore = () => {
  if (reservation.value?.storeId) {
    router.push(`/store/${reservation.value.storeId}`)
  }
}

onMounted(() => {
  loadReservation()
})
</script>

<style scoped>
.reservation-detail-container {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header h2 {
  margin: 0;
}

.time-highlight {
  color: #f56c6c;
  font-size: 16px;
  font-weight: bold;
}

.amount-highlight {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
}

.check-in-code {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.check-in-code span:first-child {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 4px;
}

.check-in-tip {
  color: #909399;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}
</style>
