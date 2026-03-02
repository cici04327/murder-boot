<template>
  <div class="payment-container" v-loading="loading">
    <el-card>
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
          <el-radio label="wechat">
            <div class="method-item">
              <el-icon :size="24" color="#07c160"><Wallet /></el-icon>
              <span>微信支付</span>
            </div>
          </el-radio>
          <el-radio label="mock">
            <div class="method-item">
              <el-icon :size="24" color="#f56c6c"><Money /></el-icon>
              <span>模拟支付（测试）</span>
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
          @click="handlePay"
        >
          确认支付 ¥{{ finalPrice }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReservationDetail, createPayment, queryPaymentStatus } from '@/api/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const reservation = ref(null)
const paymentMethod = ref('mock')

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
      reservationTime: '2024-01-15 14:00',
      playerCount: 6,
      totalPrice: 528
    }
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  paying.value = true
  try {
    const res = await createPayment(route.params.id, paymentMethod.value)
    
    if (res.code === 1 || res.code === 200) {
      const payResult = res.data
      
      // 模拟支付直接成功
      if (payResult === 'MOCK_PAY_SUCCESS' || payResult === 'ALIPAY_MOCK_SUCCESS') {
        ElMessage.success('支付成功')
        router.push({
          path: '/payment/result',
          query: {
            success: true,
            reservationId: route.params.id
          }
        })
      } 
      // 真实支付宝支付：显示支付表单
      else if (payResult && payResult.includes('<form')) {
        // 创建临时div显示支付表单并自动提交
        const div = document.createElement('div')
        div.innerHTML = payResult
        document.body.appendChild(div)
        // 自动提交表单跳转到支付宝
        setTimeout(() => {
          document.forms[document.forms.length - 1].submit()
        }, 100)
      } 
      // 其他情况
      else {
        ElMessage.success('支付成功')
        router.push({
          path: '/payment/result',
          query: {
            success: true,
            reservationId: route.params.id
          }
        })
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

.order-info {
  margin-bottom: 20px;
}

.price-detail {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 16px;
}

.price-item.total {
  font-size: 20px;
  font-weight: bold;
}

.discount {
  color: #f56c6c;
}

.price {
  color: #f56c6c;
  font-size: 24px;
}

.payment-method-card {
  margin-bottom: 20px;
}

.payment-methods {
  width: 100%;
}

.payment-methods .el-radio {
  display: block;
  margin-bottom: 15px;
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  transition: all 0.3s;
}

.payment-methods .el-radio:hover {
  border-color: #409eff;
  background: #f0f9ff;
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
</style>
