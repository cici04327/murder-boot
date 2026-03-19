<template>
  <div class="reservation-detail">
    <el-page-header content="预约详情" @back="goBack" />

    <el-card v-loading="loading" style="margin-top: 20px">
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="订单号">{{ reservation.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <el-tag :type="getStatusType(reservation.status)">
            {{ getStatusText(reservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ reservation.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ reservation.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间">{{ reservation.roomName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="剧本">{{ reservation.scriptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主持 DM" :span="2">
          <div v-if="reservation.dmId" class="dm-info-row">
            <el-avatar :size="36" :src="reservation.dmAvatar" style="margin-right:10px;flex-shrink:0">🎭</el-avatar>
            <div class="dm-info-content">
              <div class="dm-info-name">{{ reservation.dmName || '未命名 DM' }}</div>
              <div class="dm-info-meta" v-if="reservation.dmStyleTags">
                <el-tag
                  v-for="tag in reservation.dmStyleTags.split(',')"
                  :key="tag"
                  size="small"
                  style="margin-right:6px;margin-top:6px"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
          <span v-else style="color:#909399">暂未分配主持 DM</span>
        </el-descriptions-item>
        <el-descriptions-item label="分配规则" :span="2">
          <span :class="reservation.dmAssignable ? 'dm-tip-ok' : 'dm-tip-warn'">
            {{ reservation.dmAssignHint || '当前场次支持在管理端分配 DM' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ reservation.reservationTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ reservation.playerCount || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="时长">{{ reservation.duration || 3 }} 小时</el-descriptions-item>
        <el-descriptions-item label="实付金额">
          {{ formatAmount(reservation.actualAmount || reservation.totalPrice) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPayStatusType(reservation.payStatus)">
            {{ getPayStatusText(reservation.payStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销状态">
          <el-tag :type="reservation.checkInStatus === 1 ? 'success' : 'info'">
            {{ reservation.checkInStatus === 1 ? '已核销' : '未核销' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="核销码">
          {{ reservation.payStatus === 1 ? (reservation.checkInCode || '-') : '支付成功后生成' }}
        </el-descriptions-item>
        <el-descriptions-item label="核销时间">{{ reservation.checkInTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ reservation.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ reservation.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ reservation.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button
          v-if="showDmAction"
          type="primary"
          :disabled="!reservation.dmAssignable"
          @click="openDmDialog"
        >
          {{ reservation.dmId ? '改派 DM' : '分配 DM' }}
        </el-button>
        <el-button v-if="canCheckIn" type="success" @click="handleCheckIn">到店核销</el-button>
        <el-button v-if="canComplete" type="warning" @click="handleComplete">完成预约</el-button>
        <el-button v-if="canCancel" type="danger" @click="handleCancel">取消预约</el-button>
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </el-card>

    <el-dialog
      v-model="showDmDialog"
      :title="reservation.dmId ? '改派主持 DM' : '分配主持 DM'"
      width="420px"
    >
      <el-form label-width="90px">
        <el-form-item label="当前场次">
          <div>{{ reservation.scriptName || '-' }} / {{ reservation.reservationTime || '-' }}</div>
        </el-form-item>
        <el-form-item label="分配说明">
          <div :class="reservation.dmAssignable ? 'dm-tip-ok' : 'dm-tip-warn'">
            {{ reservation.dmAssignHint || '-' }}
          </div>
        </el-form-item>
        <el-form-item label="选择 DM">
          <el-select
            v-model="selectedDmId"
            placeholder="请选择主持 DM"
            style="width:100%"
            :loading="dmLoading"
            filterable
          >
            <el-option
              v-for="dm in dmOptions"
              :key="dm.id"
              :label="dm.name"
              :value="dm.id"
            >
              <div style="display:flex;align-items:center;gap:8px">
                <el-avatar :size="24" :src="dm.avatar">🎭</el-avatar>
                <span>{{ dm.name }}</span>
                <span style="margin-left:auto;color:#909399;font-size:12px">
                  {{ (dm.styleTagList || []).slice(0, 2).join(' / ') || '暂无风格标签' }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDmDialog = false">取消</el-button>
        <el-button type="primary" :loading="assigningDm" @click="handleAssignDm">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref({})
const showDmDialog = ref(false)
const dmOptions = ref([])
const dmLoading = ref(false)
const assigningDm = ref(false)
const selectedDmId = ref(null)

const reservationId = computed(() => route.params.id)

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

const formatAmount = (amount) => `￥${Number(amount || 0).toFixed(2)}`

const canCheckIn = computed(() => reservation.value.status === 2 && reservation.value.payStatus === 1 && reservation.value.checkInStatus !== 1)

const canComplete = computed(() => reservation.value.status === 2 && reservation.value.checkInStatus === 1)

const canCancel = computed(() => [1, 2].includes(reservation.value.status) && reservation.value.checkInStatus !== 1)

const showDmAction = computed(() => [1, 2].includes(reservation.value.status))

const normalizeCheckInCode = (value) => String(value || '').replace(/\s+/g, '').replace(/[^\d]/g, '')

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/reservation/${reservationId.value}`)
    reservation.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const loadDmOptions = async () => {
  if (!reservation.value.storeId) {
    dmOptions.value = []
    return
  }
  dmLoading.value = true
  try {
    const res = await request.get('/dm/list', {
      params: { storeId: reservation.value.storeId }
    })
    dmOptions.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载 DM 列表失败')
    dmOptions.value = []
  } finally {
    dmLoading.value = false
  }
}

const openDmDialog = async () => {
  if (!reservation.value.dmAssignable) {
    ElMessage.warning(reservation.value.dmAssignHint || '当前暂不能分配 DM')
    return
  }
  selectedDmId.value = reservation.value.dmId || null
  await loadDmOptions()
  if (dmOptions.value.length === 0) {
    ElMessage.warning('当前门店暂无可用 DM')
    return
  }
  showDmDialog.value = true
}

const handleAssignDm = async () => {
  if (!selectedDmId.value) {
    ElMessage.warning('请选择要分配的 DM')
    return
  }
  assigningDm.value = true
  try {
    await request({
      url: `/reservation/${reservationId.value}/assign-dm`,
      method: 'put',
      params: { dmId: selectedDmId.value }
    })
    ElMessage.success('DM 分配成功')
    showDmDialog.value = false
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || 'DM 分配失败')
  } finally {
    assigningDm.value = false
  }
}

const handleCheckIn = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入用户提供的核销码', '到店核销', {
      inputValue: reservation.value.checkInCode || '',
      closeOnClickModal: false,
      inputValidator: (inputValue) => {
        const code = normalizeCheckInCode(inputValue)
        if (!code) return '请输入核销码'
        if (code.length !== 6) return '核销码必须是 6 位数字'
        return true
      }
    })
    await request({
      url: `/reservation/${reservationId.value}/check-in`,
      method: 'put',
      params: { checkInCode: normalizeCheckInCode(value) }
    })
    ElMessage.success('核销成功')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      await ElMessageBox.alert(error.message || '核销失败，请稍后重试', '核销失败', {
        type: 'error'
      })
    }
  }
}

const handleComplete = async () => {
  try {
    await ElMessageBox.confirm('确认将该预约标记为已完成吗？', '完成预约', {
      type: 'warning'
    })
    await request.put(`/reservation/${reservationId.value}/complete`)
    ElMessage.success('预约已完成')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成预约失败')
    }
  }
}

const handleCancel = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约')
    await request({
      url: `/reservation/${reservationId.value}/cancel`,
      method: 'put',
      params: { reason: value || '' }
    })
    ElMessage.success('预约已取消')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消预约失败')
    }
  }
}

const goBack = () => {
  router.back()
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
  gap: 12px;
  margin-top: 20px;
}

.dm-info-row {
  display: flex;
  align-items: flex-start;
}

.dm-info-content {
  display: flex;
  flex-direction: column;
}

.dm-info-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.dm-info-meta {
  display: flex;
  flex-wrap: wrap;
  margin-top: 2px;
}

.dm-tip-ok {
  color: #67c23a;
}

.dm-tip-warn {
  color: #e6a23c;
}
</style>
