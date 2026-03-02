<template>
  <div class="confirm-container" v-loading="loading">
    <el-result
      icon="success"
      title="预约成功"
      sub-title="您的预约已提交，请尽快完成支付"
    >
      <template #extra>
        <el-card class="reservation-info">
          <el-descriptions title="预约信息" :column="1" border>
            <el-descriptions-item label="预约编号">{{ reservation?.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="剧本名称">{{ reservation?.scriptName }}</el-descriptions-item>
            <el-descriptions-item label="门店名称">{{ reservation?.storeName }}</el-descriptions-item>
            <el-descriptions-item label="门店地址" v-if="reservation?.storeAddress">{{ reservation?.storeAddress }}</el-descriptions-item>
            <el-descriptions-item label="房间">{{ reservation?.roomName }}</el-descriptions-item>
            <el-descriptions-item label="房间容量" v-if="reservation?.roomCapacity">{{ reservation?.roomCapacity }}人</el-descriptions-item>
            <el-descriptions-item label="预约时间">{{ reservation?.reservationTime }}</el-descriptions-item>
            <el-descriptions-item label="参与人数">{{ reservation?.playerCount }}人</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ reservation?.contactName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ reservation?.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="下单时间" v-if="reservation?.createTime">{{ reservation?.createTime }}</el-descriptions-item>
            <el-descriptions-item label="预约总价">
              <span style="color: #f56c6c; font-size: 20px; font-weight: bold">
                ¥{{ reservation?.totalPrice }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(reservation?.status)">
                {{ getStatusText(reservation?.status) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          
          <div class="actions">
            <el-button type="primary" size="large" @click="handlePay">
              立即支付
            </el-button>
            <el-button size="large" @click="router.push('/user/reservations')">
              查看我的预约
            </el-button>
            <el-button size="large" @click="router.push('/home')">
              返回首页
            </el-button>
          </div>
        </el-card>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReservationDetail } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref(null)

const getStatusType = (status) => {
  const types = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'success',
    4: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待支付',
    1: '待确认',
    2: '已确认',
    3: '已完成',
    4: '已取消'
  }
  return texts[status] || '未知'
}

const loadReservation = async () => {
  loading.value = true
  try {
    const res = await getReservationDetail(route.params.id)
    if (res.data) {
      reservation.value = res.data
    }
  } catch (error) {
    console.error('加载预约信息失败:', error)
    // 模拟数据
    reservation.value = {
      id: route.params.id,
      scriptName: '迷雾庄园',
      storeName: '探案密室',
      roomName: '推理房A',
      reservationTime: '2024-01-15 14:00',
      playerCount: 6,
      contactPhone: '13800138000',
      totalPrice: 528,
      status: 0
    }
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
  padding: 40px 20px;
  max-width: 800px;
  margin: 0 auto;
}

.reservation-info {
  margin-top: 20px;
}

.actions {
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: center;
}
</style>
