<template>
  <div class="payment-result-container">
    <el-result
      :icon="isSuccess ? 'success' : 'error'"
      :title="isSuccess ? '支付成功' : '支付失败'"
      :sub-title="isSuccess ? '您的订单已支付成功，请按时前往门店体验' : '支付遇到问题，请重试或联系客服'"
    >
      <template #extra>
        <el-space direction="vertical" :size="20">
          <el-button type="primary" size="large" @click="goToDetail" v-if="isSuccess">
            查看订单详情
          </el-button>
          <el-button size="large" @click="goToReservations" v-if="isSuccess">
            我的预约
          </el-button>
          <el-button type="primary" size="large" @click="retry" v-else>
            重新支付
          </el-button>
          <el-button size="large" @click="router.push('/home')">
            返回首页
          </el-button>
        </el-space>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isSuccess = computed(() => route.query.success === 'true')
const reservationId = computed(() => route.query.reservationId)

const goToDetail = () => {
  router.push(`/user/reservations`)
}

const goToReservations = () => {
  router.push('/user/reservations')
}

const retry = () => {
  if (reservationId.value) {
    router.push(`/payment/${reservationId.value}`)
  } else {
    router.push('/user/reservations')
  }
}
</script>

<style scoped>
.payment-result-container {
  padding: 60px 20px;
}
</style>
