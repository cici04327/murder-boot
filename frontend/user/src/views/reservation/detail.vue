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
      
      <!-- 订单信息 -->
      <el-descriptions title="订单信息" :column="2" border>
        <el-descriptions-item label="订单编号">{{ reservation.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ reservation.createTime }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(reservation.status)">
            {{ getStatusText(reservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPayStatusType(reservation.payStatus)">
            {{ getPayStatusText(reservation.payStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间" v-if="reservation.payTime">
          {{ reservation.payTime }}
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 预约信息 -->
      <el-descriptions title="预约信息" :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="剧本名称" :span="2">
          <el-link type="primary" @click="goToScript">{{ reservation?.scriptName || scriptInfo?.name }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="门店名称" :span="2">
          <el-link type="primary" @click="goToStore">{{ reservation?.storeName || storeInfo?.name }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="门店地址" :span="2">
          {{ reservation?.storeAddress || storeInfo?.address }}
        </el-descriptions-item>
        <el-descriptions-item label="房间">{{ reservation?.roomName || roomInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="房间容量">{{ reservation?.roomCapacity || roomInfo?.capacity }}人</el-descriptions-item>
        <el-descriptions-item label="预约时间" :span="2">
          <span style="color: #f56c6c; font-weight: bold; font-size: 16px">
            {{ reservation.reservationTime }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ reservation.playerCount }}人</el-descriptions-item>
        <el-descriptions-item label="预计时长">{{ scriptInfo?.duration }}小时</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ reservation.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2" v-if="reservation.remark">
          {{ reservation.remark }}
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 价格信息 -->
      <el-descriptions title="价格信息" :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="商品总价">
          ¥{{ reservation.totalPrice }}
        </el-descriptions-item>
        <el-descriptions-item label="优惠金额" v-if="reservation.discountAmount">
          <span style="color: #f56c6c">-¥{{ reservation.discountAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="实付金额" :span="2">
          <span style="color: #f56c6c; font-weight: bold; font-size: 20px">
            ¥{{ reservation.actualAmount || reservation.totalPrice }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 操作按钮 -->
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
          v-if="reservation.status < 3 && reservation.status !== 4"
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
          查看联系方式
        </el-button>
        <el-button size="large" @click="router.back()">
          返回
        </el-button>
      </div>
      
      <!-- 时间轴 -->
      <el-card style="margin-top: 20px" v-if="timeline.length > 0">
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
    
    <!-- 取消预约对话框 -->
    <el-dialog v-model="showCancelDialog" title="取消预约" width="400px">
      <el-form>
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="4"
            placeholder="请输入取消原因（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCancelDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCancel" :loading="canceling">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 联系方式对话框 -->
    <el-dialog v-model="showContactDialog" title="门店联系方式" width="400px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="门店名称">{{ storeInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ storeInfo?.phone }}</el-descriptions-item>
        <el-descriptions-item label="门店地址">{{ storeInfo?.address }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ storeInfo?.openTime }} - {{ storeInfo?.closeTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="showContactDialog = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReservationDetail, cancelReservation } from '@/api/reservation'
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
  
  if (reservation.value.status === 2) {
    items.push({
      time: reservation.value.updateTime,
      content: '预约已确认',
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
      content: `预约已取消${reservation.value.remark ? '：' + reservation.value.remark : ''}`,
      type: 'danger'
    })
  }
  
  return items
})

const getStatusType = (status) => {
  const types = {
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    1: '待确认',
    2: '已确认',
    3: '已完成',
    4: '已取消'
  }
  return texts[status] || '未知'
}

const getPayStatusType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return types[status] || 'info'
}

const getPayStatusText = (status) => {
  const texts = {
    0: '未支付',
    1: '已支付',
    2: '已退款'
  }
  return texts[status] || '未知'
}

const loadReservation = async () => {
  loading.value = true
  try {
    const res = await getReservationDetail(route.params.id)
    if (res.data) {
      reservation.value = res.data
      // 加载关联信息
      loadScriptInfo()
      loadStoreInfo()
    }
  } catch (error) {
    console.error('加载预约详情失败:', error)
    ElMessage.error('加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const loadScriptInfo = async () => {
  if (!reservation.value?.scriptId) return
  try {
    const res = await getScriptDetail(reservation.value.scriptId)
    if (res.data) {
      scriptInfo.value = res.data
    }
  } catch (error) {
    console.error('加载剧本信息失败:', error)
  }
}

const loadStoreInfo = async () => {
  if (!reservation.value?.storeId) return
  try {
    const res = await getStoreDetail(reservation.value.storeId)
    if (res.data) {
      storeInfo.value = res.data
      // 从门店信息中获取房间信息
      if (res.data.rooms) {
        roomInfo.value = res.data.rooms.find(r => r.id === reservation.value.roomId)
      }
    }
  } catch (error) {
    console.error('加载门店信息失败:', error)
  }
}

const handlePay = () => {
  router.push(`/payment/${reservation.value.id}`)
}

const handleCancel = () => {
  showCancelDialog.value = true
  cancelReason.value = ''
}

const confirmCancel = async () => {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }
  
  canceling.value = true
  try {
    const res = await cancelReservation(reservation.value.id, cancelReason.value)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('取消成功')
      showCancelDialog.value = false
      loadReservation()
    }
  } catch (error) {
    console.error('取消预约失败:', error)
    ElMessage.error('取消预约失败')
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
  if (scriptInfo.value?.id) {
    router.push(`/script/${scriptInfo.value.id}`)
  }
}

const goToStore = () => {
  if (storeInfo.value?.id) {
    router.push(`/store/${storeInfo.value.id}`)
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
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.actions {
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: center;
  padding: 20px 0;
  border-top: 1px solid #eee;
}
</style>
