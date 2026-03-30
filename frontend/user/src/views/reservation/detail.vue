<template>
  <div class="reservation-detail-container" v-loading="loading">
    <el-card v-if="reservation" class="detail-card">
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
        <!-- DM 信息 -->
        <el-descriptions-item label="主持 DM" :span="2">
          <div v-if="reservation.dmId" class="dm-info-row">
            <el-avatar :size="36" :src="reservation.dmAvatar" style="margin-right:10px;flex-shrink:0">🎭</el-avatar>
            <div class="dm-info-content">
              <span class="dm-info-name">{{ reservation.dmName }}</span>
              <el-rate :model-value="Number(reservation.dmRating || 0)" disabled show-score size="small" style="margin-left:8px" />
              <div class="dm-info-tags" v-if="reservation.dmStyleTags">
                <el-tag
                  v-for="tag in reservation.dmStyleTags.split(',')"
                  :key="tag"
                  size="small"
                  style="margin:2px"
                >{{ tag }}</el-tag>
              </div>
            </div>
          </div>
          <span v-else class="muted-text">暂未分配主持 DM</span>
        </el-descriptions-item>
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
          <el-tag :type="isCheckedIn ? 'success' : 'info'">
            {{ isCheckedIn ? '已核销' : '未核销' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销时间">
          {{ reservation.checkInTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="核销码" :span="2">
          <div v-if="reservation.payStatus === 1" class="check-in-code-wrapper">
            <div class="check-in-code">
              <span>{{ reservation.checkInCode || '-' }}</span>
              <span class="check-in-tip">到店后出示给门店工作人员进行核销</span>
            </div>
            <div class="qrcode-container">
              <canvas ref="qrcodeCanvas"></canvas>
              <span class="qrcode-label">扫码核销</span>
            </div>
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

      <div v-if="reservation.status === 1 && reservation.payStatus === 0" class="countdown-container">
        <div :class="['countdown', { 'countdown-blink': countdownTime.split(':')[0] === '00' && parseInt(countdownTime.split(':')[1]) < 5 }]">
          ⏳ 请在 {{ countdownTime }} 内完成支付，超时将自动取消
        </div>
      </div>

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
          v-if="reservation.status < 3 && reservation.status !== 4 && !isCheckedIn"
          type="warning"
          size="large"
          @click="handleReschedule"
        >
          申请改期
        </el-button>
        <el-button
          v-if="reservation.status < 3 && reservation.status !== 4 && !isCheckedIn"
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

      <!-- 我的评价区块 -->
      <el-card v-if="reservation.status === 3 && myReview" class="review-block-card" style="margin-top: 20px">
        <template #header>
          <span>⭐ 我的评价</span>
        </template>
        <div class="review-block">
          <div class="review-ratings">
            <span class="r-label">综合</span>
            <el-rate :model-value="myReview.overallRating" disabled show-score size="small" />
            <template v-if="myReview.scriptRating">
              <span class="r-label">剧本</span>
              <el-rate :model-value="myReview.scriptRating" disabled show-score size="small" />
            </template>
            <template v-if="myReview.storeRating">
              <span class="r-label">门店</span>
              <el-rate :model-value="myReview.storeRating" disabled show-score size="small" />
            </template>
            <template v-if="myReview.dmRating">
              <span class="r-label">DM</span>
              <el-rate :model-value="myReview.dmRating" disabled show-score size="small" />
            </template>
          </div>
          <p class="review-content-text">{{ myReview.content || '（未填写文字内容）' }}</p>
          <div class="review-imgs" v-if="myReview.images">
            <el-image
              v-for="(img, idx) in myReview.images.split(',')"
              :key="idx" :src="img"
              :preview-src-list="myReview.images.split(',')"
              fit="cover" class="review-thumb"
            />
          </div>
          <div class="review-tags" v-if="myReview.tags">
            <el-tag v-for="tag in myReview.tags.split(',')" :key="tag" size="small" type="info" style="margin:2px">{{ tag }}</el-tag>
          </div>
          <div class="review-time">发表于 {{ myReview.createTime }}</div>
          <!-- 商家回复 -->
          <div class="review-reply" v-if="myReview.replyContent">
            <span class="reply-label">商家回复：</span>
            <span class="reply-text">{{ myReview.replyContent }}</span>
          </div>
        </div>
      </el-card>

      <el-card v-if="timeline.length > 0" class="timeline-card" style="margin-top: 20px">
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

    <!-- 改期弹窗 -->
    <el-dialog v-model="showRescheduleDialog" title="申请改期" width="460px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="当前预约时间">
          <span style="color: #909399;">{{ reservation?.reservationTime }}</span>
        </el-form-item>

        <!-- 改期规则说明 -->
        <el-form-item label=" ">
          <div class="reschedule-rules">
            <div class="rule-title">📋 改期规则</div>
            <div class="rule-item" :class="{ 'rule-warn': !canReschedule }">
              <span class="rule-icon">⏰</span>
              <span>最晚改期时间：开局前 <strong>24小时</strong>
                <span v-if="!canReschedule" style="color:#f56c6c;margin-left:6px">（已超时，无法改期）</span>
                <span v-else style="color:#67c23a;margin-left:6px">（截止 {{ rescheduleDeadline }}）</span>
              </span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">💰</span>
              <span>手续费：<strong>免费</strong></span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">🔁</span>
              <span>每笔订单最多可改期 <strong>1次</strong></span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">📌</span>
              <span>改期不影响支付状态，新时间须晚于当前时间</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item v-if="canReschedule" label="新预约时间" required>
          <el-date-picker
            v-model="newReservationTime"
            type="datetime"
            placeholder="请选择新的预约时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm"
            :disabled-date="(date) => date < new Date()"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-if="!canReschedule" label=" ">
          <el-alert
            title="无法改期"
            type="error"
            :closable="false"
            description="距开局不足24小时，已超过最晚改期时间。如有特殊情况请直接联系门店。"
            show-icon
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRescheduleDialog = false">关闭</el-button>
        <el-button v-if="canReschedule" type="primary" :loading="rescheduling" @click="confirmReschedule">确认改期</el-button>
      </template>
    </el-dialog>

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
import { computed, onMounted, onActivated, ref, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import QRCode from 'qrcode'
import { cancelReservation, getReservationDetail, rescheduleReservation } from '@/api/reservation'
import { getScriptDetail } from '@/api/script'
import { getStoreDetail } from '@/api/store'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const canceling = ref(false)
const reservation = ref(null)
const myReview = ref(null)
const scriptInfo = ref(null)
const storeInfo = ref(null)
const roomInfo = ref(null)
const showCancelDialog = ref(false)
const showContactDialog = ref(false)
const showRescheduleDialog = ref(false)
const cancelReason = ref('')
const rescheduling = ref(false)
const newReservationTime = ref('')

// Countdown timer states
const countdownTime = ref('00:00')
const isCountdownExpired = ref(false)
const countdownTimerId = ref(null)

// QR code canvas ref
const qrcodeCanvas = ref(null)
const isCheckedIn = computed(() => {
  if (!reservation.value) return false
  return Number(reservation.value.checkInStatus || 0) === 1
    || !!reservation.value.checkInTime
    || Number(reservation.value.status) === 3
})

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
    // 已完成且已评价，加载评价内容
    if (reservation.value?.status === 3 && reservation.value?.hasReviewed === 1) {
      loadMyReview(reservation.value.id)
    }
  } catch (error) {
    ElMessage.error(error.message || '加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const refreshReservationIfVisible = () => {
  if (document.visibilityState === 'visible' && route.params.id) {
    loadReservation()
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

const loadMyReview = async (reservationId) => {
  try {
    const res = await request({ url: `/reservation/review/reservation/${reservationId}`, method: 'get' })
    if ((res.code === 1 || res.code === 200) && res.data) {
      myReview.value = res.data
    }
  } catch (e) {
    console.warn('加载评价失败', e)
  }
}

const handleReview = () => {
  router.push(`/reservation/review/${reservation.value.id}`)
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

// 计算是否可以改期（距开局24小时前）
const canReschedule = computed(() => {
  if (!reservation.value?.reservationTime) return false
  const reservationDate = new Date(reservation.value.reservationTime)
  const deadline = new Date(reservationDate.getTime() - 24 * 60 * 60 * 1000)
  return new Date() < deadline
})

// 计算最晚改期截止时间（开局前24小时）
const rescheduleDeadline = computed(() => {
  if (!reservation.value?.reservationTime) return ''
  const reservationDate = new Date(reservation.value.reservationTime)
  const deadline = new Date(reservationDate.getTime() - 24 * 60 * 60 * 1000)
  const y = deadline.getFullYear()
  const mo = String(deadline.getMonth() + 1).padStart(2, '0')
  const d = String(deadline.getDate()).padStart(2, '0')
  const h = String(deadline.getHours()).padStart(2, '0')
  const mi = String(deadline.getMinutes()).padStart(2, '0')
  return `${y}-${mo}-${d} ${h}:${mi}`
})

const handleReschedule = () => {
  newReservationTime.value = reservation.value.reservationTime || ''
  showRescheduleDialog.value = true
}

const confirmReschedule = async () => {
  if (!newReservationTime.value) {
    ElMessage.warning('请选择新的预约时间')
    return
  }
  // 格式化为 yyyy-MM-dd HH:mm:ss
  const formatted = newReservationTime.value.replace('T', ' ') + ':00'
  rescheduling.value = true
  try {
    await rescheduleReservation(reservation.value.id, formatted)
    ElMessage.success('改期成功')
    showRescheduleDialog.value = false
    loadReservation()
  } catch (error) {
    ElMessage.error(error.message || '改期失败')
  } finally {
    rescheduling.value = false
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

// Function to parse datetime string and calculate countdown
const startCountdown = () => {
  if (!reservation.value || reservation.value.status !== 1 || reservation.value.payStatus !== 0) {
    return
  }

  // Clear existing timer
  if (countdownTimerId.value) {
    clearInterval(countdownTimerId.value)
  }

  const updateCountdown = () => {
    const createTime = new Date(reservation.value.createTime)
    const deadlineTime = new Date(createTime.getTime() + 30 * 60 * 1000) // 30 minutes later
    const now = new Date()
    const remainingMs = deadlineTime.getTime() - now.getTime()

    if (remainingMs <= 0) {
      // Countdown expired
      countdownTime.value = '00:00'
      isCountdownExpired.value = true
      clearInterval(countdownTimerId.value)
      ElMessage.error('订单已超时，请重新预约')
      setTimeout(() => {
        router.go(0) // Refresh the page
      }, 2000)
      return
    }

    // Calculate minutes and seconds
    const totalSeconds = Math.floor(remainingMs / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60

    countdownTime.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }

  updateCountdown()
  countdownTimerId.value = setInterval(updateCountdown, 1000)
}

// Function to generate QR code
const generateQRCode = async () => {
  if (!reservation.value?.checkInCode) return

  // 等待 DOM 渲染（v-if 控制显示，需要 nextTick 后 canvas 才存在）
  await nextTick()

  if (!qrcodeCanvas.value) {
    console.warn('qrcodeCanvas ref 未找到，稍后重试')
    setTimeout(generateQRCode, 300)
    return
  }

  try {
    const options = {
      width: 150,
      margin: 2,
      color: {
        dark: '#ffffff',   // 前景色（白色）
        light: '#1a1a2e'   // 背景色（深色）
      }
    }
    await QRCode.toCanvas(qrcodeCanvas.value, reservation.value.checkInCode, options)
  } catch (error) {
    console.error('生成二维码失败:', error)
  }
}

// Watch for reservation changes to update countdown and QR code
watch(
  () => reservation.value,
  (newVal) => {
    if (newVal) {
      if (newVal.status === 1 && newVal.payStatus === 0) {
        startCountdown()
      }
      if (newVal.checkInCode) {
        generateQRCode()
      }
    }
  },
  { deep: true }
)

// Watch for check-in code changes
watch(
  () => reservation.value?.checkInCode,
  (newCode) => {
    if (newCode) {
      generateQRCode()
    }
  }
)

onMounted(() => {
  loadReservation()
  window.addEventListener('focus', refreshReservationIfVisible)
  document.addEventListener('visibilitychange', refreshReservationIfVisible)
})

onActivated(() => {
  refreshReservationIfVisible()
})

onBeforeUnmount(() => {
  if (countdownTimerId.value) {
    clearInterval(countdownTimerId.value)
  }
  window.removeEventListener('focus', refreshReservationIfVisible)
  document.removeEventListener('visibilitychange', refreshReservationIfVisible)
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
  color: #f2f6ff;
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

/* DM 信息展示 */
.dm-info-row {
  display: flex;
  align-items: flex-start;
}

.dm-info-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.dm-info-name {
  font-size: 15px;
  font-weight: 600;
  color: #f2f6ff;
}

.dm-info-tags {
  width: 100%;
  margin-top: 4px;
}

.countdown-container {
  margin-top: 20px;
  margin-bottom: 20px;
  text-align: center;
}

.countdown {
  color: #f56c6c;
  font-size: 16px;
  font-weight: bold;
  padding: 12px 16px;
  background-color: rgba(245, 108, 108, 0.1);
  border-radius: 4px;
  display: inline-block;
}

.countdown-blink {
  animation: blink 0.6s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.check-in-code-wrapper {
  display: flex;
  gap: 40px;
  align-items: flex-start;
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
  color: #9fb3d9;
  font-size: 13px;
}

.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qrcode-container canvas {
  background-color: #1a1a2e;
  border-radius: 4px;
  padding: 4px;
}

.qrcode-label {
  color: #9fb3d9;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* 改期规则样式 */
.reschedule-rules {
  background: linear-gradient(135deg, rgba(17, 27, 47, 0.95), rgba(12, 19, 35, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px 16px;
  width: 100%;
}

.rule-title {
  font-size: 13px;
  font-weight: 600;
  color: #f2f6ff;
  margin-bottom: 10px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #b8c7e2;
  margin-bottom: 6px;
  line-height: 1.5;
}

.rule-item:last-child {
  margin-bottom: 0;
}

.rule-item strong {
  color: #f2f6ff;
}

.rule-icon {
  flex-shrink: 0;
  width: 18px;
  text-align: center;
}

.rule-warn {
  color: #f56c6c;
}

.muted-text {
  color: rgba(255, 255, 255, 0.56);
}

.detail-card,
/* 我的评价区块样式 */
.review-block { padding: 4px 0; }
.review-ratings {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.r-label {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}
.review-content-text {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
  line-height: 1.7;
  margin: 0 0 10px;
}
.review-imgs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.review-thumb {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid rgba(255,255,255,0.12);
}
.review-time {
  font-size: 12px;
  color: rgba(255,255,255,0.45);
  margin-top: 8px;
}
.review-reply {
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(103,194,58,0.08);
  border-left: 3px solid #67c23a;
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  color: rgba(255,255,255,0.85);
}
.reply-label { color: #67c23a; font-weight: 600; margin-right: 6px; }
.review-tags { margin-bottom: 8px; }

.timeline-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(7, 15, 30, 0.94), rgba(10, 20, 37, 0.9));
  box-shadow: 0 30px 60px rgba(2, 8, 18, 0.35);
}

.reservation-detail-container :deep(.el-card) {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(7, 15, 30, 0.94), rgba(10, 20, 37, 0.9));
  box-shadow: 0 30px 60px rgba(2, 8, 18, 0.35);
}

.reservation-detail-container :deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 25, 44, 0.72);
}

.reservation-detail-container :deep(.el-card__body) {
  background: transparent;
}

.reservation-detail-container :deep(.el-descriptions) {
  --el-descriptions-table-border: rgba(255, 255, 255, 0.08);
}

.reservation-detail-container :deep(.el-descriptions__title) {
  color: #f2f6ff;
}

.reservation-detail-container :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  background: rgba(18, 30, 51, 0.92);
  color: #9fb3d9;
}

.reservation-detail-container :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  background: rgba(11, 20, 38, 0.88);
  color: #f2f6ff;
}

.reservation-detail-container :deep(.el-link) {
  color: #ff9b88;
}

.reservation-detail-container :deep(.el-link:hover) {
  color: #ffb1a2;
}

.reservation-detail-container :deep(.el-rate__text) {
  color: #d8e4fb;
}

.reservation-detail-container :deep(.el-timeline-item__timestamp) {
  color: #9fb3d9;
}

.reservation-detail-container :deep(.el-timeline-item__content) {
  color: #f2f6ff;
}

.reservation-detail-container :deep(.el-dialog) {
  background: linear-gradient(180deg, rgba(7, 15, 30, 0.97), rgba(10, 20, 37, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 30px 60px rgba(2, 8, 18, 0.42);
}

.reservation-detail-container :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 25, 44, 0.72);
}

.reservation-detail-container :deep(.el-dialog__title) {
  color: #f2f6ff;
}

.reservation-detail-container :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(255, 255, 255, 0.7);
}

.reservation-detail-container :deep(.el-dialog__headerbtn .el-dialog__close:hover) {
  color: #fff;
}

.reservation-detail-container :deep(.el-dialog__body),
.reservation-detail-container :deep(.el-dialog__footer) {
  background: transparent;
}

.reservation-detail-container :deep(.el-form-item__label) {
  color: #dbe5fb;
}

.reservation-detail-container :deep(.el-alert) {
  background: rgba(245, 108, 108, 0.12);
  border: 1px solid rgba(245, 108, 108, 0.2);
}

.reservation-detail-container :deep(.el-alert__title),
.reservation-detail-container :deep(.el-alert__description) {
  color: #f8d5d5;
}

.reservation-detail-container :deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.el-button--warning)) {
  background: rgba(18, 30, 51, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
  color: #dbe5fb;
}
</style>
