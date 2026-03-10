<template>
  <div class="reservations-container">
    <!-- 剧本杀主题背景装饰 -->
    <div class="mystery-bg">
      <div class="floating-icon icon-1">📋</div>
      <div class="floating-icon icon-2">🎭</div>
      <div class="floating-icon icon-3">🔍</div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <span class="header-emoji">📁</span>
          <div class="header-text">
            <h1>案件档案</h1>
            <p>查看您的所有预约记录</p>
          </div>
        </div>
        <el-button type="primary" class="create-btn" @click="router.push('/reservation/create')">
          <span class="btn-emoji">🎯</span>
          接受新任务
        </el-button>
      </div>
    </div>

    <el-card class="main-card mystery-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="mystery-tabs">
        <el-tab-pane name="all">
          <template #label>
            <span class="tab-label">📋 全部案件</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="0">
          <template #label>
            <span class="tab-label">💰 待支付</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="1">
          <template #label>
            <span class="tab-label">⏳ 待确认</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="2">
          <template #label>
            <span class="tab-label">✅ 已确认</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="3">
          <template #label>
            <span class="tab-label">🏆 已完成</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="4">
          <template #label>
            <span class="tab-label">❌ 已取消</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="reservations-list" v-loading="loading">
        <div class="reservation-item mystery-item" :class="getItemClass(item)" v-for="item in reservations" :key="item.id">
          <div class="item-header">
            <div class="order-info">
              <span class="case-icon">{{ getStatusEmoji(item) }}</span>
              <span class="order-no">案件编号：#{{ item.id }}</span>
            </div>
            <div class="header-tags">
              <el-tag :type="getStatusType(item)" class="status-tag" effect="dark">
                {{ getStatusText(item) }}
              </el-tag>
              <el-tag
                :type="item.checkInStatus === 1 ? 'success' : 'info'"
                class="status-tag"
                effect="dark"
              >
                {{ item.checkInStatus === 1 ? '已核销' : '未核销' }}
              </el-tag>
            </div>
          </div>

          <el-row :gutter="20" class="item-content">
            <el-col :span="16">
              <div class="info-row">
                <span class="label">🎭 剧本：</span>
                <span class="value script-name">{{ item.scriptName }}</span>
              </div>
              <div class="info-row">
                <span class="label">🏠 场馆：</span>
                <span class="value">{{ item.storeName }}</span>
              </div>
              <div class="info-row">
                <span class="label">🚪 房间：</span>
                <span class="value">{{ item.roomName }}</span>
              </div>
              <div class="info-row">
                <span class="label">📅 时间：</span>
                <span class="value">{{ item.reservationTime }}</span>
              </div>
              <div class="info-row">
                <span class="label">👥 人数：</span>
                <span class="value">{{ item.playerCount }}位侦探</span>
              </div>
              <div class="info-row" v-if="item.payStatus === 1 || item.checkInCode">
                <span class="label">🔐 核销码：</span>
                <span class="value">{{ item.checkInCode || '-' }}</span>
              </div>
            </el-col>

            <el-col :span="8" class="price-actions">
              <div class="price">
                <span class="label">报酬：</span>
                <span class="value">¥{{ Number(item.actualAmount || item.totalPrice || 0).toFixed(2) }}</span>
              </div>
              <div class="actions">
                <el-button
                  v-if="item.payStatus === 0"
                  type="primary"
                  size="small"
                  class="action-btn pay-btn"
                  @click="handlePay(item)"
                >
                  💰 支付报酬
                </el-button>
                <el-button
                  v-if="canRefund(item)"
                  type="warning"
                  size="small"
                  class="action-btn"
                  @click="handleRefund(item)"
                >
                  🔄 申请退款
                </el-button>
                <el-tag
                  v-if="item.refundStatus === 1"
                  type="warning"
                  size="small"
                  effect="dark"
                  class="status-mini-tag"
                >
                  ⏳ 退款审核中
                </el-tag>
                <el-tag
                  v-if="item.refundStatus === 2"
                  type="success"
                  size="small"
                  effect="dark"
                  class="status-mini-tag"
                >
                  ✅ 已退款
                </el-tag>
                <el-button
                  v-if="item.status === 3 && item.hasReviewed === 0"
                  type="primary"
                  size="small"
                  class="action-btn review-btn"
                  @click="handleReview(item)"
                >
                  ⭐ 撰写评价
                </el-button>
                <el-tag
                  v-if="item.status === 3 && item.hasReviewed === 1"
                  type="success"
                  size="small"
                  effect="dark"
                  class="status-mini-tag"
                >
                  ✅ 已评价
                </el-tag>
                <el-button
                  v-if="item.payStatus === 0 && getDerivedStatus(item) < 2"
                  size="small"
                  class="action-btn cancel-btn"
                  @click="handleCancel(item)"
                >
                  ❌ 放弃任务
                </el-button>
                <el-button size="small" class="action-btn detail-btn" @click="handleViewDetail(item)">
                  🔍 查看详情
                </el-button>
              </div>
            </el-col>
          </el-row>
        </div>

        <EmptyState
          v-if="!loading && reservations.length === 0"
          type="no-reservation"
          @action="$router.push('/reservation/create')"
        />
      </div>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 取消预约对话框 -->
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
        <el-button @click="showCancelDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCancel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { ElMessage } from 'element-plus'
import { getMyReservations, cancelReservation } from '@/api/reservation'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('all')
const reservations = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const showCancelDialog = ref(false)
const cancelReason = ref('')
const currentCancelItem = ref(null)

const getDerivedStatus = (item) => {
  if (item.payStatus === 0) return 0
  return item.status ?? 0
}

const getStatusType = (item) => {
  const status = getDerivedStatus(item)
  const types = {
    0: 'warning',
    1: 'info',
    2: 'success',
    3: 'success',
    4: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (item) => {
  const status = getDerivedStatus(item)
  const texts = {
    0: '待支付',
    1: '待确认',
    2: '已确认',
    3: '已破案',
    4: '已取消'
  }
  return texts[status] || '未知'
}

const getStatusEmoji = (item) => {
  const status = getDerivedStatus(item)
  const emojis = {
    0: '💰',
    1: '⏳',
    2: '✅',
    3: '🏆',
    4: '❌'
  }
  return emojis[status] || '📋'
}

const getItemClass = (item) => {
  const classes = []
  const status = getDerivedStatus(item)
  if (status === 3) classes.push('completed')
  if (status === 4) classes.push('cancelled')
  if (status === 0) classes.push('unpaid')
  return classes.join(' ')
}

const canRefund = (item) => {
  return item.payStatus === 1
    && item.status < 3
    && item.checkInStatus !== 1
    && (!item.refundStatus || item.refundStatus === 0 || item.refundStatus === 3)
}

const handlePageChange = (newPage) => {
  page.value = newPage
  loadReservations()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  page.value = 1
  loadReservations()
}

const loadReservations = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }

    if (activeTab.value === '0') {
      params.payStatus = 0
    } else if (activeTab.value !== 'all') {
      params.status = parseInt(activeTab.value, 10)
    }

    const res = await getMyReservations(params)
    if (res.data) {
      if (Array.isArray(res.data)) {
        reservations.value = res.data
        total.value = res.data.length
      } else {
        reservations.value = res.data.records || res.data.list || []
        total.value = res.data.total || 0
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  loadReservations()
}

const handlePay = (item) => {
  router.push(`/payment/${item.id}`)
}

const handleCancel = (item) => {
  currentCancelItem.value = item
  showCancelDialog.value = true
}

const confirmCancel = async () => {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }

  try {
    await cancelReservation(currentCancelItem.value.id, cancelReason.value)
    ElMessage.success('取消成功')
    showCancelDialog.value = false
    cancelReason.value = ''
    loadReservations()
  } catch (error) {
    ElMessage.error(error.message || '取消预约失败')
  }
}

const handleReview = (item) => {
  router.push(`/reservation/review/${item.id}`)
}

const handleRefund = (item) => {
  router.push({
    path: '/reservation/refund',
    query: { id: item.id }
  })
}

const handleViewDetail = (item) => {
  router.push(`/reservation/detail/${item.id}`)
}

onMounted(() => {
  loadReservations()
})

onActivated(() => {
  loadReservations()
})
</script>

<style scoped>
.reservations-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 64px - 100px);
  background: transparent;
  position: relative;
}

/* 神秘背景装饰 */
.mystery-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.floating-icon {
  position: absolute;
  font-size: 35px;
  opacity: 0.08;
  will-change: transform;
  animation: float 20s linear infinite;
  transform: translateZ(0);
}

.icon-1 { top: 15%; left: 10%; animation-delay: 0s; }
.icon-2 { top: 45%; right: 8%; animation-delay: 7s; }
.icon-3 { bottom: 20%; left: 20%; animation-delay: 14s; }

@keyframes float {
  0%, 100% { transform: translateY(0) translateZ(0); }
  50% { transform: translateY(-15px) translateZ(0); }
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-emoji {
  font-size: 48px;
}

.header-text h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #fff;
  background: linear-gradient(90deg, #FFD700, #FFA500);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.create-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  font-size: 16px;
  padding: 12px 24px;
  height: auto;
}

.btn-emoji {
  margin-right: 8px;
  font-size: 18px;
}

/* 主卡片 */
.main-card {
  position: relative;
  z-index: 1;
}

.mystery-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border-radius: 16px !important;
  border: 1px solid rgba(102, 126, 234, 0.2) !important;
}

/* 标签页 */
.mystery-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.mystery-tabs :deep(.el-tabs__item:hover) {
  color: #fff;
}

.mystery-tabs :deep(.el-tabs__item.is-active) {
  color: #FFD700;
}

.mystery-tabs :deep(.el-tabs__active-bar) {
  background-color: #FFD700;
}

.mystery-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.1);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 预约列表 */
.reservations-list {
  min-height: 400px;
  padding: 10px 0;
}

/* 预约项卡片 */
.reservation-item {
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 15px;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  backdrop-filter: blur(10px);
  will-change: transform;
}

.reservation-item:hover {
  transform: translateY(-3px) translateZ(0);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.25);
  border-color: rgba(102, 126, 234, 0.5);
  background: linear-gradient(135deg, rgba(30, 30, 55, 0.98) 0%, rgba(26, 38, 72, 0.98) 100%);
}

.reservation-item.completed {
  border-color: rgba(103, 194, 58, 0.5);
  background: linear-gradient(135deg, rgba(20, 40, 30, 0.95) 0%, rgba(25, 50, 35, 0.95) 100%);
}

.reservation-item.cancelled {
  border-color: rgba(144, 147, 153, 0.5);
  background: linear-gradient(135deg, rgba(40, 40, 45, 0.95) 0%, rgba(50, 50, 55, 0.95) 100%);
  opacity: 0.8;
}

.reservation-item.unpaid {
  border-color: rgba(230, 162, 60, 0.5);
  background: linear-gradient(135deg, rgba(40, 35, 25, 0.95) 0%, rgba(50, 40, 30, 0.95) 100%);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 2px dashed rgba(255, 255, 255, 0.15);
}

.order-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.case-icon {
  font-size: 24px;
}

.order-no {
  font-weight: bold;
  color: #fff;
  font-size: 15px;
}

.header-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 20px;
}

.item-content {
  margin-top: 15px;
}

.info-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.info-row .label {
  color: rgba(255, 255, 255, 0.7);
  min-width: 100px;
  font-size: 14px;
}

.info-row .value {
  color: #fff;
  font-size: 14px;
}

.info-row .value.script-name {
  font-weight: bold;
  color: #fff;
  font-size: 16px;
}

.price-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}

.price {
  font-size: 16px;
  margin-bottom: 15px;
  text-align: right;
}

.price .label {
  color: rgba(255, 255, 255, 0.7);
}

.price .value {
  color: #F56C6C;
  font-weight: bold;
  font-size: 28px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.action-btn {
  width: 100%;
  border-radius: 8px !important;
  font-size: 13px;
}

.pay-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
}

.review-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
  border: none !important;
}

.detail-btn {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%) !important;
  border: 1px solid rgba(102, 126, 234, 0.3) !important;
  color: #1a1a2e !important;
}

.cancel-btn {
  background: transparent !important;
  border: 1px solid #dcdfe6 !important;
  color: #909399 !important;
}

.status-mini-tag {
  width: 100%;
  text-align: center;
  justify-content: center;
  border-radius: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .header-left {
    flex-direction: column;
  }

  .header-emoji {
    font-size: 40px;
  }

  .header-text h1 {
    font-size: 22px;
  }

  .create-btn {
    width: 100%;
  }

  .item-content .el-col {
    margin-bottom: 15px;
  }

  .price-actions {
    align-items: stretch;
  }

  .price {
    text-align: center;
  }

  .header-tags {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
