<template>
  <div class="reservation-detail">
    <el-page-header @back="goBack" title="返回" content="预约详情" />
    
    <el-card style="margin-top: 20px;">
      <el-descriptions title="预约信息" :column="2" border>
        <el-descriptions-item label="订单号">{{ reservation.orderNo || reservation.reservationNo }}</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <el-tag :type="getStatusType(reservation.status)">
            {{ getStatusText(reservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ reservation.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ reservation.storeName }}</el-descriptions-item>
        <el-descriptions-item label="房间">{{ reservation.roomName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="剧本">{{ reservation.scriptName }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ reservation.reservationTime }}</el-descriptions-item>
        <el-descriptions-item label="人数">{{ reservation.playerCount }} 人</el-descriptions-item>
        <el-descriptions-item label="时长">{{ reservation.duration || 3.5 }} 小时</el-descriptions-item>
        <el-descriptions-item label="总价">¥{{ reservation.totalPrice || 0 }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="reservation.payStatus === 1 ? 'success' : reservation.payStatus === 2 ? 'info' : 'warning'">
            {{ reservation.payStatus === 1 ? '已支付' : reservation.payStatus === 2 ? '已退款' : '未支付' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ reservation.payTime || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ reservation.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ reservation.remark || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions" style="margin-top: 20px;">
        <el-button type="primary" @click="handleEdit" v-if="reservation.status === 0">编辑预约</el-button>
        <el-button type="danger" @click="handleCancel" v-if="reservation.status === 0 || reservation.status === 1">取消预约</el-button>
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const reservationId = route.params.id

const reservation = ref({})

// 加载预约详情
const loadDetail = async () => {
  try {
    const res = await request.get(`/reservation/${reservationId}`)
    reservation.value = res.data || {}
  } catch (error) {
    console.error('加载预约详情失败:', error)
    ElMessage.error('加载预约详情失败')
    // 使用模拟数据
    reservation.value = {
      id: reservationId,
      orderNo: 'R202410230001',
      contactName: '张三',
      contactPhone: '13800138000',
      storeName: '推理馆·五角场店',
      scriptName: '雾都迷案',
      reservationTime: '2024-10-25 19:00:00',
      playerCount: 6,
      totalPrice: 198,
      status: 1,
      createTime: '2024-10-23 10:00:00',
      remark: '请提前准备好房间'
    }
  }
}

const goBack = () => {
  router.back()
}

const handleEdit = () => {
  router.push(`/reservation/edit/${reservationId}`)
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await request.put(`/reservation/cancel/${reservationId}`)
    ElMessage.success('预约已取消')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
      ElMessage.error('取消预约失败')
    }
  }
}

const getStatusType = (status) => {
  const types = ['warning', 'success', 'info', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待支付', '已支付', '已完成', '已取消']
  return texts[status] || '未知'
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.reservation-detail {
  padding: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
