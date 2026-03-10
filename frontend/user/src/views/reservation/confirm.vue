<template>
  <div class="confirm-container" v-loading="loading">
    <el-result
      icon="success"
      title="预约提交成功"
      sub-title="请尽快完成支付，到店时向门店出示核销码"
    >
      <template #extra>
        <el-card class="reservation-info">
          <el-descriptions title="预约信息" :column="1" border>
            <el-descriptions-item label="预约编号">{{ reservation?.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="剧本名称">{{ reservation?.scriptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="门店名称">{{ reservation?.storeName || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="reservation?.storeAddress" label="门店地址">
              {{ reservation.storeAddress }}
            </el-descriptions-item>
            <el-descriptions-item label="房间">{{ reservation?.roomName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预约时间">{{ reservation?.reservationTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参与人数">{{ reservation?.playerCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ reservation?.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ reservation?.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(reservation?.status)">
                {{ getStatusText(reservation?.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="amount-highlight">
                ￥{{ Number(reservation?.actualAmount || reservation?.totalPrice || 0).toFixed(2) }}
              </span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="check-in-panel">
            <div class="check-in-title">到店核销码</div>
            <div class="check-in-code">{{ reservation?.checkInCode || '-' }}</div>
            <div class="check-in-desc">
              {{ reservation?.payStatus === 1 ? '支付成功后可直接向门店出示此核销码' : '该核销码已生成，支付成功后即可使用' }}
            </div>
          </div>

          <div class="actions">
            <el-button type="primary" size="large" @click="handlePay">立即支付</el-button>
            <el-button size="large" @click="router.push('/user/reservations')">查看我的预约</el-button>
            <el-button size="large" @click="router.push('/home')">返回首页</el-button>
          </div>
        </el-card>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReservationDetail } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref(null)

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 1: '待确认', 2: '已确认', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const loadReservation = async () => {
  loading.value = true
  try {
    const res = await getReservationDetail(route.params.id)
    reservation.value = res.data || null
  } finally {
    loading.value = false
  }
}

const handlePay = () => {
  router.push(`/payment/${route.params.id}`)
}

onMounted(() => {
  loadReservation()
})
</script>

<style scoped>
.confirm-container {
  max-width: 820px;
  margin: 0 auto;
  padding: 40px 20px;
}

.reservation-info {
  margin-top: 20px;
}

.amount-highlight {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
}

.check-in-panel {
  margin-top: 20px;
  padding: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff4e6 0%, #fffaf2 100%);
  border: 1px solid #f7d8a8;
  text-align: center;
}

.check-in-title {
  color: #8c6a2a;
  font-size: 14px;
}

.check-in-code {
  margin-top: 12px;
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 6px;
}

.check-in-desc {
  margin-top: 12px;
  color: #8c6a2a;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}
</style>
