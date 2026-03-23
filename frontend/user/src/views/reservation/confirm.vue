<template>
  <div class="confirm-container" v-loading="loading">
    <el-result
      icon="success"
      title="预约提交成功"
      :sub-title="reservation?.groupId ? '人数不足已自动发起拼团，请尽快支付并分享拼团给朋友' : '请尽快完成支付，到店时向门店出示核销码'"
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

          <div class="group-panel" v-if="reservation?.groupId">
            <div class="group-panel-title">拼团已自动发起</div>
            <div class="group-panel-desc">
              当前预约人数不足完整开团，系统已经为该场次生成拼团。你可以前往拼团页查看进度，并把链接分享给好友一起上车。
            </div>
            <el-button type="warning" plain @click="handleViewGroup">查看拼团</el-button>
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

const handleViewGroup = () => {
  if (!reservation.value?.groupId) return
  router.push(`/group/${reservation.value.groupId}`)
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
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(160deg, rgba(11, 20, 38, 0.94), rgba(15, 27, 48, 0.9));
  box-shadow: 0 24px 48px rgba(3, 8, 20, 0.32);
}

.amount-highlight {
  color: #ff8f8f;
  font-size: 22px;
  font-weight: bold;
}

.check-in-panel {
  margin-top: 20px;
  padding: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(27, 41, 66, 0.94), rgba(18, 29, 49, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}

.check-in-title {
  color: #9fb3d9;
  font-size: 14px;
}

.check-in-code {
  margin-top: 12px;
  color: #f4f7ff;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 6px;
}

.check-in-desc {
  margin-top: 12px;
  color: #9fb3d9;
  font-size: 13px;
}

.group-panel {
  margin-top: 20px;
  padding: 20px 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(31, 45, 73, 0.94), rgba(20, 31, 52, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
}

.group-panel-title {
  color: #f1f5ff;
  font-size: 16px;
  font-weight: 700;
}

.group-panel-desc {
  margin: 10px 0 16px;
  color: #a8b7d4;
  font-size: 13px;
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}

.confirm-container :deep(.el-result) {
  padding: 36px 28px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(7, 15, 30, 0.94), rgba(10, 20, 37, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 30px 60px rgba(2, 8, 18, 0.35);
}

.confirm-container :deep(.el-result__title p) {
  color: #f2f6ff;
}

.confirm-container :deep(.el-result__subtitle p) {
  color: #9fb3d9;
}

.confirm-container :deep(.el-card__body) {
  background: transparent;
}

.confirm-container :deep(.el-descriptions) {
  --el-descriptions-table-border: rgba(255, 255, 255, 0.08);
}

.confirm-container :deep(.el-descriptions__title) {
  color: #f2f6ff;
}

.confirm-container :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  background: rgba(18, 30, 51, 0.92);
  color: #9fb3d9;
}

.confirm-container :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  background: rgba(11, 20, 38, 0.88);
  color: #f2f6ff;
}

.confirm-container :deep(.el-button:not(.el-button--primary):not(.is-plain)) {
  background: rgba(18, 30, 51, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
  color: #dbe5fb;
}

.confirm-container :deep(.el-button.is-plain) {
  background: rgba(18, 30, 51, 0.4);
  border-color: rgba(255, 255, 255, 0.12);
  color: #f2f6ff;
}
</style>
