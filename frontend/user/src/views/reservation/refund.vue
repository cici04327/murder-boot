<template>
  <div class="refund-container">
    <el-card class="refund-card">
      <template #header>
        <div class="card-header">
          <span>申请退款</span>
        </div>
      </template>

      <el-form :model="refundForm" :rules="rules" ref="refundFormRef" label-width="100px">
        <el-form-item label="订单编号">
          <el-input v-model="reservation.orderNo" disabled />
        </el-form-item>

        <el-form-item label="预约时间">
          <el-input v-model="reservation.reservationTime" disabled />
        </el-form-item>

        <el-form-item label="支付金额">
          <el-input v-model="reservation.actualAmount" disabled>
            <template #suffix>元</template>
          </el-input>
        </el-form-item>

        <el-form-item label="退款原因" prop="reason">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入退款原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitRefund" :loading="loading">
            提交退款申请
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        title="退款说明"
        type="info"
        :closable="false"
        style="margin-top: 20px"
      >
        <ul style="margin: 10px 0; padding-left: 20px;">
          <li>退款申请提交后，将由管理员审核</li>
          <li>审核通过后，退款金额将原路退回</li>
          <li>如果您通过完成此预约获得了积分，退款成功后将扣除相应积分</li>
          <li>预约已完成的订单无法申请退款</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const refundFormRef = ref(null)
const loading = ref(false)
const reservation = ref({})

const refundForm = reactive({
  reason: ''
})

const rules = {
  reason: [
    { required: true, message: '请输入退款原因', trigger: 'blur' },
    { min: 10, message: '退款原因至少10个字符', trigger: 'blur' }
  ]
}

// 获取预约详情
const getReservationDetail = async () => {
  try {
    const reservationId = route.query.id
    if (!reservationId) {
      ElMessage.error('缺少预约ID')
      goBack()
      return
    }

    const res = await request.get(`/reservation/${reservationId}`)
    console.log('获取预约详情响应:', res)
    
    if (res.code === 1 || res.code === 200) {
      reservation.value = res.data

      // 检查是否可以申请退款
      if (reservation.value.payStatus !== 1) {
        ElMessage.warning('该订单无法申请退款')
        goBack()
        return
      }

      if (reservation.value.status === 3) {
        ElMessage.warning('预约已完成，无法申请退款')
        goBack()
        return
      }
    } else {
      ElMessage.error(res.msg || '获取预约信息失败')
      goBack()
    }
  } catch (error) {
    console.error('获取预约详情失败:', error)
    ElMessage.error('获取预约信息失败: ' + (error.message || '网络错误'))
    goBack()
  }
}

// 提交退款申请
const submitRefund = async () => {
  if (!refundFormRef.value) return

  await refundFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          '确认要申请退款吗？退款申请提交后将由管理员审核。',
          '确认退款',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        loading.value = true

        const res = await request.post('/reservation/payment/refund/apply', null, {
          params: {
            reservationId: route.query.id,
            reason: refundForm.reason
          }
        })

        console.log('提交退款响应:', res)

        if (res.code === 1 || res.code === 200) {
          ElMessage.success('退款申请已提交，请等待管理员审核')
          setTimeout(() => {
            router.push('/user/reservations')
          }, 1500)
        } else {
          ElMessage.error(res.msg || '提交失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('提交退款申请失败:', error)
          ElMessage.error('提交失败，请稍后重试')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  getReservationDetail()
})
</script>

<style scoped>
.refund-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.refund-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>
