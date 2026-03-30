<template>
  <div class="payment-container" v-loading="loading">
    <el-result
      v-if="loadError"
      icon="error"
      title="订单加载失败"
      :sub-title="loadErrorMessage"
      class="payment-error-result"
    >
      <template #extra>
        <el-space>
          <el-button type="primary" @click="loadReservation">重新加载</el-button>
          <el-button @click="router.push('/user/reservations')">返回我的预约</el-button>
        </el-space>
      </template>
    </el-result>

    <el-card v-else-if="reservation">
      <template #header>
        <h2>订单支付</h2>
      </template>
      
      <el-descriptions :column="1" border class="order-info">
        <el-descriptions-item label="订单编号">{{ reservation?.id }}</el-descriptions-item>
        <el-descriptions-item label="剧本名称">{{ reservation?.scriptName }}</el-descriptions-item>
        <el-descriptions-item label="门店名称">{{ reservation?.storeName }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ reservation?.reservationTime }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ reservation?.playerCount }}人</el-descriptions-item>
      </el-descriptions>
      
      <!-- 价格明细 -->
      <div class="price-detail">
        <div class="price-item">
          <span>商品总价：</span>
          <span>¥{{ reservation?.originalPrice || reservation?.totalPrice }}</span>
        </div>
        <div class="price-item" v-if="couponDiscount > 0">
          <span>优惠券抵扣：</span>
          <span class="discount">-¥{{ couponDiscount }}</span>
        </div>
        <el-divider />
        <div class="price-item total">
          <span>实付金额：</span>
          <span class="price">¥{{ finalPrice }}</span>
        </div>
      </div>
      
      <!-- 倒计时 -->
      <div class="countdown-wrap" v-if="!countdownExpired">
        <el-icon><Clock /></el-icon>
        <span :class="['countdown-text', countdownSeconds <= 300 ? 'warn' : '']">
          剩余支付时间：{{ countdownDisplay }}
        </span>
      </div>
      <el-alert
        v-else
        title="支付超时，订单已自动取消"
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom:16px"
      />

      <!-- 支付方式 -->
      <el-card class="payment-method-card">
        <template #header>
          <span>支付方式</span>
        </template>
        <el-radio-group v-model="paymentMethod" class="payment-methods">
          <el-radio label="alipay">
            <div class="method-item">
              <el-icon :size="24" color="#1677ff"><CreditCard /></el-icon>
              <span>支付宝</span>
            </div>
          </el-radio>
        </el-radio-group>
      </el-card>
      
      <div class="action-buttons">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button
          type="primary"
          size="large"
          :loading="paying"
          :disabled="countdownExpired"
          @click="handlePay"
        >
          确认支付 ¥{{ finalPrice }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CreditCard } from '@element-plus/icons-vue'
import { getReservationDetail, createPayment } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const reservation = ref(null)
const loadError = ref(false)
const loadErrorMessage = ref('')
const paymentMethod = ref('alipay')

// 倒计时
const countdownSeconds = ref(1800) // 30分钟
const countdownExpired = ref(false)
let countdownTimer = null

const countdownDisplay = computed(() => {
  const s = Math.max(0, countdownSeconds.value)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const startCountdown = (createTime) => {
  if (!createTime) return
  const created = new Date(createTime).getTime()
  const deadline = created + 30 * 60 * 1000
  const update = () => {
    const remaining = Math.floor((deadline - Date.now()) / 1000)
    if (remaining <= 0) {
      countdownSeconds.value = 0
      countdownExpired.value = true
      clearInterval(countdownTimer)
      setTimeout(() => router.push('/user/reservations'), 3000)
    } else {
      countdownSeconds.value = remaining
    }
  }
  update()
  countdownTimer = setInterval(update, 1000)
}

onUnmounted(() => { clearInterval(countdownTimer) })

// 优惠券折扣金额
const couponDiscount = computed(() => {
  if (!reservation.value) return 0
  // 从预约信息中获取优惠券折扣
  return parseFloat(reservation.value?.couponDiscount || reservation.value?.discountAmount || 0)
})

// 实付金额
const finalPrice = computed(() => {
  if (!reservation.value) return '0.00'
  // 如果后端已经计算好了实付金额，直接使用
  if (reservation.value.actualPrice !== undefined && reservation.value.actualPrice !== null) {
    return parseFloat(reservation.value.actualPrice).toFixed(2)
  }
  // 否则用总价减去优惠券折扣
  const total = parseFloat(reservation.value?.totalPrice || 0)
  const discount = couponDiscount.value
  return Math.max(0, total - discount).toFixed(2)
})

const loadReservation = async () => {
  if (!route.params.id) {
    ElMessage.error('预约ID无效')
    router.push('/user/reservations')
    return
  }
  loading.value = true
  loadError.value = false
  loadErrorMessage.value = ''
  reservation.value = null
  try {
    const res = await getReservationDetail(route.params.id)
    if ((res.code === 1 || res.code === 200) && res.data) {
      reservation.value = res.data
      startCountdown(res.data?.createTime)
    } else {
      throw new Error(res.msg || '未获取到预约信息')
    }
  } catch (error) {
    console.error('加载预约信息失败:', error)
    loadError.value = true
    loadErrorMessage.value = error?.message || '当前订单无法加载，可能已失效、无权限访问或登录状态已过期'
    ElMessage.error(loadErrorMessage.value)
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  // 防重复提交：正在支付中则直接返回
  if (paying.value) return
  if (!reservation.value) {
    ElMessage.warning('订单信息加载失败，暂时无法发起支付')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认使用【支付宝】支付 ¥${finalPrice.value}？`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '再想想',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
  } catch {
    // 用户取消，不做任何处理
    return
  }

  paying.value = true
  try {
    const res = await createPayment(route.params.id, 'alipay')
    
    if (res.code === 1 || res.code === 200) {
      const payResult = res.data

      if (payResult && payResult.includes('<form')) {
        const div = document.createElement('div')
        div.innerHTML = payResult
        document.body.appendChild(div)
        setTimeout(() => {
          document.forms[document.forms.length - 1].submit()
        }, 100)
      } else {
        throw new Error('未获取到有效的支付宝支付表单')
      }
    }
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error(error.response?.data?.msg || '支付失败，请重试')
  } finally {
    paying.value = false
  }
}

onMounted(() => {
  loadReservation()
})
</script>

<style scoped>
.payment-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 20px;
}

.payment-error-result {
  padding: 60px 20px;
}

.order-info {
  margin-bottom: 20px;
}

.price-detail {
  background: linear-gradient(160deg, rgba(11, 20, 38, 0.94), rgba(15, 27, 48, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 20px;
  border-radius: 18px;
  margin-bottom: 20px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 16px;
  color: #dbe5fb;
}

.price-item.total {
  font-size: 20px;
  font-weight: bold;
}

.discount {
  color: #ff8f8f;
}

.price {
  color: #ff8f8f;
  font-size: 24px;
}

.countdown-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: rgba(255,255,255,0.05);
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.08);
}
.countdown-text { font-size: 15px; color: #dbe5fb; }
.countdown-text.warn { color: #e6a23c; font-weight: bold; }

.payment-method-card {
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(160deg, rgba(11, 20, 38, 0.94), rgba(15, 27, 48, 0.9));
}

.payment-methods {
  width: 100%;
}

.payment-methods :deep(.el-radio) {
  display: block;
  margin-bottom: 15px;
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  transition: all 0.3s;
  background: rgba(15, 25, 44, 0.88);
}

.payment-methods :deep(.el-radio:hover) {
  border-color: rgba(255, 255, 255, 0.16);
  background: rgba(20, 33, 58, 0.94);
}

.payment-methods :deep(.el-radio__label) {
  color: #e8eefc;
}

.payment-methods :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #f4f7ff;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
}

.payment-container :deep(.el-card) {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(7, 15, 30, 0.94), rgba(10, 20, 37, 0.9));
  box-shadow: 0 30px 60px rgba(2, 8, 18, 0.35);
}

.payment-container :deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 25, 44, 0.72);
}

.payment-container :deep(.el-card__header h2),
.payment-container :deep(.el-card__header span) {
  color: #f2f6ff;
}

.payment-container :deep(.el-card__body) {
  background: transparent;
}

.payment-container :deep(.el-descriptions) {
  --el-descriptions-table-border: rgba(255, 255, 255, 0.08);
}

.payment-container :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  background: rgba(18, 30, 51, 0.92);
  color: #9fb3d9;
}

.payment-container :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  background: rgba(11, 20, 38, 0.88);
  color: #f2f6ff;
}

.payment-container :deep(.el-divider) {
  border-color: rgba(255, 255, 255, 0.08);
}

.payment-container :deep(.el-button:not(.el-button--primary)) {
  background: rgba(18, 30, 51, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
  color: #dbe5fb;
}
</style>
