<template>
  <div class="user-coupons">
    <!-- 剧本杀主题背景装饰 -->
    <div class="mystery-bg">
      <div class="floating-icon icon-1">🎫</div>
      <div class="floating-icon icon-2">🎁</div>
      <div class="floating-icon icon-3">💎</div>
    </div>

    <el-card class="page-header mystery-header">
      <div class="header-content">
        <div>
          <h2>
            <span class="header-emoji">🎒</span>
            道具背包
          </h2>
          <p class="subtitle">管理您的探案道具，享受更多优惠</p>
        </div>
        <el-button type="primary" size="large" class="claim-btn" @click="showAvailableCoupons = true">
          <span class="btn-emoji">🎁</span>
          领取道具
        </el-button>
      </div>
    </el-card>

    <!-- 统计信息 - 剧本杀主题 -->
    <el-row :gutter="15" class="stats-row">
      <el-col :xs="24" :sm="8" :md="8">
        <el-card shadow="hover" class="stats-card mystery-stat-card stats-primary">
          <div class="stat-decoration">📦</div>
          <div class="stats-content">
            <div class="stats-icon-wrapper">
              <span class="stat-emoji">🎫</span>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ formatNumber(statistics.total) }}</div>
              <div class="stats-label">全部道具</div>
              <div class="stats-desc">累计获得</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="8">
        <el-card shadow="hover" class="stats-card mystery-stat-card stats-success">
          <div class="stat-decoration">✨</div>
          <div class="stats-content">
            <div class="stats-icon-wrapper">
              <span class="stat-emoji">🗝️</span>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ formatNumber(statistics.available) }}</div>
              <div class="stats-label">可使用</div>
              <div class="stats-desc">{{ statistics.available > 0 ? '立即使用' : '暂无道具' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="8">
        <el-card shadow="hover" class="stats-card mystery-stat-card stats-danger">
          <div class="stat-decoration">⏰</div>
          <div class="stats-content">
            <div class="stats-icon-wrapper">
              <span class="stat-emoji">⚠️</span>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ formatNumber(statistics.expiring) }}</div>
              <div class="stats-label">即将失效</div>
              <div class="stats-desc">{{ statistics.expiring > 0 ? '7天内失效' : '无失效风险' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 优惠券列表 - 剧本杀主题 -->
    <el-card class="coupon-list-card mystery-card">
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="coupon-tabs mystery-tabs">
            <el-tab-pane name="unused">
              <template #label>
                <span class="tab-label">
                  <span class="tab-emoji">🗝️</span>
                  可使用
                  <el-badge v-if="statistics.available > 0" :value="statistics.available" class="tab-badge" />
                </span>
              </template>
            </el-tab-pane>
            <el-tab-pane name="used">
              <template #label>
                <span class="tab-label">
                  <span class="tab-emoji">✅</span>
                  已使用
                </span>
              </template>
            </el-tab-pane>
            <el-tab-pane name="expired">
              <template #label>
                <span class="tab-label">
                  <span class="tab-emoji">💨</span>
                  已失效
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>

      <div v-loading="loading" class="coupon-list">
        <EmptyState
          v-if="coupons.length === 0"
          type="no-coupon"
          @action="showAvailableCoupons = true"
        />

        <el-row :gutter="15" v-else>
          <el-col :xs="24" :sm="24" :md="12" :lg="12" v-for="coupon in coupons" :key="coupon.id">
            <div class="coupon-item mystery-coupon" :class="getCouponClass(coupon)">
              <!-- 优惠券左侧 - 金额显示 -->
              <div class="coupon-left">
                <div class="coupon-icon">{{ getCouponEmoji(coupon.type) }}</div>
                <div class="coupon-amount">
                  <span class="currency">¥</span>
                  <span class="value">{{ coupon.discountValue }}</span>
                </div>
                <div class="coupon-condition">满{{ coupon.minAmount }}可用</div>
                <div class="coupon-type-badge">
                  {{ getCouponTypeName(coupon.type) }}
                </div>
              </div>

              <!-- 优惠券右侧 - 详细信息 -->
              <div class="coupon-right">
                <div class="coupon-info">
                  <h3 class="coupon-name">
                    {{ coupon.couponName || coupon.name }}
                    <el-tag v-if="isExpiringSoon(coupon)" type="warning" size="small" effect="dark" class="expiring-tag">
                      ⏰ 即将失效
                    </el-tag>
                  </h3>
                  <p class="coupon-desc">{{ coupon.description || '可用于所有剧本预约' }}</p>
                  <div class="coupon-meta">
                    <div class="meta-item">
                      <span class="meta-emoji">📅</span>
                      <span>有效期至 {{ formatDate(coupon.expireTime) }}</span>
                    </div>
                    <div class="meta-item" v-if="coupon.status === 2 && coupon.usedTime">
                      <span class="meta-emoji">✅</span>
                      <span>使用于 {{ formatDate(coupon.usedTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="coupon-actions">
                  <el-tag v-if="coupon.status === 2" type="info" size="large" class="status-tag used-tag">
                    ✅ 已使用
                  </el-tag>
                  <el-tag v-else-if="coupon.status === 3" type="danger" size="large" class="status-tag expired-tag">
                    💨 已失效
                  </el-tag>
                  <el-button v-else type="primary" size="default" class="use-btn" @click="useCouponNow(coupon)">
                    🎯 立即使用
                  </el-button>
                </div>
              </div>

              <!-- 装饰性元素 -->
              <div class="coupon-decoration">
                <div class="circle circle-top"></div>
                <div class="circle circle-bottom"></div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 可领取优惠券对话框 -->
    <el-dialog
      v-model="showAvailableCoupons"
      title="🎁 领取优惠券"
      width="900px"
      :close-on-click-modal="false"
      class="coupon-dialog"
    >
      <div v-loading="availableLoading" class="available-coupons">
        <div v-if="availableCoupons.length === 0" class="empty-state">
          <el-empty description="暂无可领取的优惠券">
            <template #image>
              <el-icon :size="100" color="#909399"><Ticket /></el-icon>
            </template>
          </el-empty>
        </div>
        <div v-else class="available-list">
          <div v-for="coupon in availableCoupons" :key="coupon.id" class="available-coupon-item">
            <!-- 优惠券左侧 - 金额显示 -->
            <div class="coupon-left">
              <div class="coupon-amount">
                <span class="currency">¥</span>
                <span class="value">{{ coupon.discountValue }}</span>
              </div>
              <div class="coupon-condition">满{{ coupon.minAmount }}元</div>
              <div class="coupon-type-badge">
                {{ getCouponTypeName(coupon.type) }}
              </div>
            </div>

            <!-- 优惠券右侧 - 详细信息 -->
            <div class="coupon-right">
              <div class="coupon-info">
                <h3 class="coupon-name">
                  {{ coupon.name }}
                  <el-tag v-if="coupon.remainCount <= 10 && coupon.remainCount > 0" type="danger" size="small" effect="dark">
                    仅剩{{ coupon.remainCount }}张
                  </el-tag>
                </h3>
                <p class="coupon-desc">{{ coupon.description || '可用于所有剧本预约' }}</p>
                <div class="coupon-meta">
                  <div class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ formatDate(coupon.validStartTime) }} 至 {{ formatDate(coupon.validEndTime) }}</span>
                  </div>
                  <div class="meta-item">
                    <el-icon><Tickets /></el-icon>
                    <span>剩余 {{ coupon.remainCount }} / {{ coupon.totalCount }}</span>
                  </div>
                  <div class="meta-item" v-if="coupon.exchangePoints && coupon.exchangePoints > 0">
                    <el-icon><StarFilled /></el-icon>
                    <span>需要 {{ coupon.exchangePoints }} 积分</span>
                  </div>
                </div>
              </div>
              <div class="coupon-actions">
                <el-button 
                  :type="coupon.exchangePoints > 0 ? 'warning' : 'primary'"
                  size="large"
                  :disabled="coupon.remainCount <= 0"
                  :loading="receivingCouponId === coupon.id"
                  @click="handleReceiveCoupon(coupon)"
                >
                  <el-icon v-if="coupon.remainCount > 0"><Plus /></el-icon>
                  {{ coupon.remainCount > 0 ? (coupon.exchangePoints > 0 ? `${coupon.exchangePoints}积分兑换` : '免费领取') : '已抢光' }}
                </el-button>
              </div>
            </div>

            <!-- 装饰性元素 -->
            <div class="coupon-decoration">
              <div class="circle circle-top"></div>
              <div class="circle circle-bottom"></div>
            </div>

            <!-- 进度条 -->
            <div class="stock-progress">
              <el-progress 
                :percentage="getStockPercentage(coupon)" 
                :color="getProgressColor(coupon)"
                :show-text="false"
                :stroke-width="3"
              />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Tickets, 
  CircleCheck, 
  Clock, 
  Plus, 
  Ticket,
  Select,
  CircleClose,
  Calendar,
  StarFilled
} from '@element-plus/icons-vue'
import { getMyCoupons, getAvailableCoupons, receiveCoupon } from '@/api/coupon'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()

// 状态
const loading = ref(false)
const activeTab = ref('unused')
const coupons = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 可领取优惠券
const showAvailableCoupons = ref(false)
const availableLoading = ref(false)
const availableCoupons = ref([])
const receivingCouponId = ref(null)

// 统计信息
const statistics = reactive({
  total: 0,
  available: 0,
  expiring: 0
})

// 格式化数字
const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 获取优惠券类型名称
const getCouponTypeName = (type) => {
  const typeMap = {
    1: '满减券',
    2: '折扣券',
    3: '代金券'
  }
  return typeMap[type] || '优惠券'
}

// 获取优惠券对应的emoji图标
const getCouponEmoji = (type) => {
  const emojiMap = {
    1: '🎫', // 满减券
    2: '💎', // 折扣券
    3: '🎁'  // 代金券
  }
  return emojiMap[type] || '🎫'
}

// 获取优惠券样式类
const getCouponClass = (coupon) => {
  const classes = []
  if (coupon.status === 3) classes.push('expired')
  if (coupon.status === 2) classes.push('used')
  if (isExpiringSoon(coupon)) classes.push('expiring-soon')
  return classes.join(' ')
}

// 判断是否即将过期（7天内）
const isExpiringSoon = (coupon) => {
  if (!coupon.expireTime || coupon.status !== 1) return false
  const now = new Date()
  const expireTime = new Date(coupon.expireTime)
  const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
  return expireTime <= sevenDaysLater && expireTime > now
}

// 获取库存百分比
const getStockPercentage = (coupon) => {
  if (!coupon.totalCount || coupon.totalCount === 0) return 0
  return Math.round((coupon.remainCount / coupon.totalCount) * 100)
}

// 获取进度条颜色
const getProgressColor = (coupon) => {
  const percentage = getStockPercentage(coupon)
  if (percentage > 50) return '#67c23a'
  if (percentage > 20) return '#e6a23c'
  return '#f56c6c'
}

// 分页处理函数
const handlePageChange = (newPage) => {
  console.log('优惠券列表页码变化:', newPage)
  currentPage.value = newPage
  loadCoupons()
}

const handleSizeChange = (newSize) => {
  console.log('优惠券列表每页大小变化:', newSize)
  pageSize.value = newSize
  currentPage.value = 1
  loadCoupons()
}

// 加载优惠券列表
const loadCoupons = async () => {
  loading.value = true
  try {
    const statusValue = activeTab.value === 'unused' ? 1 : activeTab.value === 'used' ? 2 : 3
    console.log('===== 加载优惠券列表 =====')
    console.log('当前标签:', activeTab.value)
    console.log('状态值:', statusValue)
    console.log('页码:', currentPage.value)
    console.log('每页数量:', pageSize.value)
    
    const params = {
      status: statusValue,
      page: currentPage.value,
      pageSize: pageSize.value
    }
    
    const res = await getMyCoupons(params)
    console.log('API响应:', res)
    console.log('响应码:', res.code)
    console.log('数据:', res.data)
    
    if (res.code === 1 || res.code === 200) {
      const records = res.data.records || []
      const totalCount = res.data.total || 0
      
      console.log('优惠券记录数:', records.length)
      console.log('总数:', totalCount)
      console.log('优惠券列表:', records)
      
      coupons.value = records
      total.value = totalCount
      
      // 更新统计信息
      updateStatistics()
    } else {
      console.error('API返回错误码:', res.code, '错误信息:', res.msg)
      ElMessage.error(res.msg || '加载优惠券失败')
    }
  } catch (error) {
    console.error('加载优惠券失败，错误详情:', error)
    ElMessage.error('加载优惠券失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 更新统计信息
const updateStatistics = async () => {
  try {
    console.log('===== 更新统计信息 =====')
    
    // 获取所有状态的优惠券数量（使用较大的pageSize以获取所有记录）
    const [unusedRes, usedRes, expiredRes] = await Promise.all([
      getMyCoupons({ status: 1, page: 1, pageSize: 1000 }),
      getMyCoupons({ status: 2, page: 1, pageSize: 1000 }),
      getMyCoupons({ status: 3, page: 1, pageSize: 1000 })
    ])
    
    console.log('未使用响应:', unusedRes)
    console.log('已使用响应:', usedRes)
    console.log('已过期响应:', expiredRes)
    
    // 修复：后端total字段有问题，使用records.length获取实际数量
    const unusedCount = unusedRes.data?.records?.length || 0
    const usedCount = usedRes.data?.records?.length || 0
    const expiredCount = expiredRes.data?.records?.length || 0
    
    console.log('未使用数量:', unusedCount)
    console.log('已使用数量:', usedCount)
    console.log('已过期数量:', expiredCount)
    
    statistics.available = unusedCount
    statistics.total = unusedCount + usedCount + expiredCount
    
    console.log('统计信息 - 可用:', statistics.available, '总数:', statistics.total)
    
    // 计算即将过期的优惠券（7天内）- 需要获取所有未使用的优惠券
    if (statistics.available > 0) {
      console.log('获取所有未使用优惠券，数量:', statistics.available)
      const allUnusedRes = await getMyCoupons({ status: 1, page: 1, pageSize: statistics.available })
      console.log('所有未使用优惠券响应:', allUnusedRes)
      
      const now = new Date()
      const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
      
      const allUnusedCoupons = allUnusedRes.data?.records || []
      console.log('未使用优惠券列表:', allUnusedCoupons)
      
      statistics.expiring = allUnusedCoupons.filter(c => {
        if (!c.expireTime) return false
        const expireTime = new Date(c.expireTime)
        return expireTime <= sevenDaysLater && expireTime > now
      }).length
      
      console.log('即将过期数量:', statistics.expiring)
    } else {
      statistics.expiring = 0
      console.log('没有未使用的优惠券')
    }
  } catch (error) {
    console.error('更新统计信息失败，错误详情:', error)
  }
}

// 加载可领取的优惠券
const loadAvailableCoupons = async () => {
  availableLoading.value = true
  try {
    const res = await getAvailableCoupons()
    console.log('可领取优惠券API响应:', res)
    console.log('响应码:', res.code)
    console.log('数据:', res.data)
    
    if (res.code === 1 || res.code === 200) {
      // 如果返回的是分页结构，取records，否则直接用data
      if (res.data && res.data.records) {
        availableCoupons.value = res.data.records || []
        console.log('可领取优惠券数量:', res.data.records.length)
      } else {
        availableCoupons.value = res.data || []
        console.log('可领取优惠券数量:', res.data?.length || 0)
      }
    } else {
      console.error('API返回错误码:', res.code)
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载可领取优惠券失败:', error)
    ElMessage.error('加载可领取优惠券失败')
  } finally {
    availableLoading.value = false
  }
}

// 领取优惠券
const handleReceiveCoupon = async (coupon) => {
  // 如果需要积分兑换，先确认
  if (coupon.exchangePoints && coupon.exchangePoints > 0) {
    const userPoints = userStore.userInfo?.points || 0
    
    if (userPoints < coupon.exchangePoints) {
      ElMessage.warning(`积分不足！当前积分：${userPoints}，需要：${coupon.exchangePoints}`)
      return
    }
    
    try {
      await ElMessageBox.confirm(
        `兑换此优惠券需要消耗 ${coupon.exchangePoints} 积分，当前积分：${userPoints}，确认兑换吗？`,
        '积分兑换确认',
        {
          confirmButtonText: '确认兑换',
          cancelButtonText: '取消',
          type: 'warning',
          customClass: 'exchange-confirm-box'
        }
      )
    } catch {
      return // 用户取消
    }
  }
  
  receivingCouponId.value = coupon.id
  try {
    const res = await receiveCoupon(coupon.id)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success({
        message: coupon.exchangePoints > 0 
          ? `🎉 兑换成功！消耗 ${coupon.exchangePoints} 积分` 
          : '🎉 领取成功！已添加到您的优惠券',
        duration: 3000
      })
      // 刷新用户信息以更新积分
      if (coupon.exchangePoints > 0) {
        userStore.fetchUserInfo()
      }
      loadAvailableCoupons()
      loadCoupons()
      updateStatistics()
    } else {
      ElMessage.error(res.msg || '领取失败，请重试')
    }
  } catch (error) {
    console.error('领取优惠券失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '领取失败，请重试'
    ElMessage.error(errorMsg)
  } finally {
    receivingCouponId.value = null
  }
}

// 立即使用优惠券
const useCouponNow = (coupon) => {
  ElMessageBox.confirm(
    '使用优惠券需要创建预约，是否前往预约页面？',
    '提示',
    {
      confirmButtonText: '前往预约',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    console.log('跳转到预约页面，优惠券ID:', coupon.id)
    // 使用命名路由跳转
    router.push({ 
      name: 'CreateReservation',
      query: { couponId: coupon.id }
    }).then(() => {
      console.log('跳转成功')
    }).catch(err => {
      console.error('跳转失败:', err)
      // 如果命名路由失败，尝试使用路径
      router.push('/script/list')
    })
  }).catch(() => {
    console.log('用户取消跳转')
  })
}

// 切换标签
const handleTabChange = () => {
  currentPage.value = 1
  loadCoupons()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 初始化
onMounted(() => {
  loadCoupons()
})

// 页面激活时刷新数据（从其他页面返回时）
onActivated(() => {
  console.log('优惠券页面被激活，刷新数据')
  loadCoupons()
})

// 监听对话框状态
watch(showAvailableCoupons, (newVal) => {
  if (newVal) {
    loadAvailableCoupons()
  }
})
</script>

<style scoped>
.user-coupons {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: transparent;
  min-height: calc(100vh - 64px - 100px);
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

.icon-1 { top: 15%; left: 8%; animation-delay: 0s; }
.icon-2 { top: 40%; right: 12%; animation-delay: 5s; }
.icon-3 { bottom: 25%; left: 20%; animation-delay: 10s; }

@keyframes float {
  0%, 100% { transform: translateY(0) translateZ(0); }
  50% { transform: translateY(-15px) translateZ(0); }
}

/* 页面头部 - 剧本杀主题 */
.page-header {
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 16px !important;
  position: relative;
  z-index: 1;
}

.page-header :deep(.el-card__body) {
  padding: 30px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.page-header h2 {
  margin: 0 0 10px 0;
  font-size: 28px;
  color: white;
  display: flex;
  align-items: center;
  gap: 12px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.header-emoji {
  font-size: 32px;
}

.subtitle {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.claim-btn {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%) !important;
  border: none !important;
  font-size: 16px;
  padding: 12px 24px;
  height: auto;
}

.btn-emoji {
  margin-right: 6px;
  font-size: 18px;
}

/* 统计卡片 - 剧本杀主题 */
.stats-row {
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.stats-card {
  cursor: pointer;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  border: none;
  will-change: transform;
}

.mystery-stat-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border-radius: 16px !important;
  border: 1px solid rgba(192, 57, 43, 0.2) !important;
  position: relative;
  overflow: hidden;
}

.stat-decoration {
  position: absolute;
  top: 10px;
  right: 15px;
  font-size: 28px;
  opacity: 0.15;
}

.stats-card:hover {
  transform: translateY(-3px) translateZ(0);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
}

.stats-content {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 10px;
}

.stats-icon-wrapper {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  flex-shrink: 0;
}

.stat-emoji {
  font-size: 32px;
}

.stats-primary .stats-icon-wrapper {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
}

.stats-success .stats-icon-wrapper {
  background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
}

.stats-danger .stats-icon-wrapper {
  background: linear-gradient(135deg, #F56C6C 0%, #f78989 100%);
}

.stats-icon {
  opacity: 0.9;
}

.stats-primary .stats-icon {
  color: #409eff;
}

.stats-success .stats-icon {
  color: #67c23a;
}

.stats-danger .stats-icon {
  color: #f56c6c;
}

.stats-info {
  flex: 1;
  min-width: 0;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stats-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 2px;
  font-weight: 500;
}

.stats-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

/* 优惠券列表卡片 - 剧本杀主题 */
.coupon-list-card {
  position: relative;
  z-index: 1;
}

.mystery-card {
  background: rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(10px);
  border-radius: 16px !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.coupon-list-card :deep(.el-card__header) {
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px 16px 0 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.coupon-tabs {
  flex: 1;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
}

.tab-emoji {
  font-size: 16px;
}

.tab-badge {
  margin-left: 5px;
}

.coupon-list {
  min-height: 400px;
  padding: 10px 0;
}

/* 优惠券卡片 - 剧本杀主题 */
.coupon-item {
  display: flex;
  border-radius: 16px;
  overflow: visible;
  margin-bottom: 15px;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.3);
  position: relative;
  will-change: transform;
}

.mystery-coupon {
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.coupon-item:hover {
  transform: translateY(-3px) translateZ(0);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.4);
}

.coupon-item.expired,
.coupon-item.used {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
  opacity: 0.7;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.coupon-item.expired:hover,
.coupon-item.used:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.coupon-item.expiring-soon {
  background: linear-gradient(135deg, #E6A23C 0%, #F56C6C 100%);
  border: 2px solid rgba(255, 215, 0, 0.5);
}

/* 优惠券左侧 - 金额区域 */
.coupon-left {
  width: 140px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px 15px;
  color: white;
  position: relative;
  background: rgba(255, 255, 255, 0.1);
}

.coupon-icon {
  font-size: 28px;
  margin-bottom: 5px;
}

.coupon-amount {
  margin-bottom: 6px;
  text-align: center;
}

.coupon-amount .currency {
  font-size: 16px;
  font-weight: 500;
  opacity: 0.9;
}

.coupon-amount .value {
  font-size: 40px;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.coupon-condition {
  font-size: 13px;
  opacity: 0.95;
  margin-bottom: 8px;
  font-weight: 500;
}

.coupon-type-badge {
  font-size: 11px;
  padding: 3px 10px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  font-weight: 500;
  backdrop-filter: blur(5px);
}

/* 优惠券右侧 - 信息区域 */
.coupon-right {
  flex: 1;
  display: flex;
  padding: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.coupon-info {
  flex: 1;
  min-width: 0;
}

.coupon-name {
  margin: 0 0 8px 0;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.coupon-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
}

.coupon-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.meta-item .el-icon {
  font-size: 14px;
}

.coupon-actions {
  margin-left: 20px;
  flex-shrink: 0;
}

/* 装饰性元素 */
.coupon-decoration {
  position: absolute;
  left: 140px;
  top: 0;
  bottom: 0;
  width: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  width: 20px;
  height: 20px;
  background: rgba(26, 26, 46, 0.9);
  border-radius: 50%;
  left: -10px;
}

.circle-top {
  top: -10px;
}

.circle-bottom {
  bottom: -10px;
}

/* 可领取优惠券对话框 */
.coupon-dialog :deep(.el-dialog) {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(192, 57, 43, 0.3) !important;
  --el-dialog-bg-color: transparent !important;
}

.coupon-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.6) 0%, rgba(192, 57, 43, 0.4) 100%) !important;
  padding: 25px 30px;
  border-bottom: 1px solid rgba(192, 57, 43, 0.3);
}

.coupon-dialog :deep(.el-dialog__title) {
  color: white !important;
  font-size: 20px;
  font-weight: 600;
}

.coupon-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white !important;
  font-size: 20px;
}

.coupon-dialog :deep(.el-dialog__body) {
  padding: 20px;
  background: transparent !important;
}

.available-coupons {
  max-height: 600px;
  overflow-y: auto;
}

.available-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.available-coupon-item {
  display: flex;
  border-radius: 12px;
  overflow: visible;
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.5) 0%, rgba(192, 57, 43, 0.3) 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  position: relative;
  transition: all 0.3s ease;
  border: 1px solid rgba(192, 57, 43, 0.3);
}

.available-coupon-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(192, 57, 43, 0.4);
  border-color: #16213e;
}

.available-coupon-item .coupon-right {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
}

/* 库存进度条 */
.stock-progress {
  position: absolute;
  bottom: 0;
  left: 140px;
  right: 0;
  background: rgba(35, 35, 60, 0.9);
}

.stock-progress :deep(.el-progress) {
  margin: 0;
}

.stock-progress :deep(.el-progress-bar__outer) {
  border-radius: 0;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
  text-align: center;
}

/* 分页 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-coupons {
    padding: 10px;
  }

  .page-header :deep(.el-card__body) {
    padding: 20px;
  }

  .header-content {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .page-header h2 {
    font-size: 22px;
  }

  .stats-content {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }

  .stats-icon-wrapper {
    width: 50px;
    height: 50px;
  }

  .stats-icon {
    font-size: 32px !important;
  }

  .stats-value {
    font-size: 24px;
  }

  .coupon-item {
    flex-direction: column;
  }

  .coupon-left {
    width: 100%;
    padding: 20px;
    flex-direction: row;
    justify-content: space-around;
  }

  .coupon-right {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .coupon-actions {
    margin-left: 0;
    width: 100%;
  }

  .coupon-actions .el-button {
    width: 100%;
  }

  .coupon-decoration {
    display: none;
  }

  .available-coupon-item {
    flex-direction: column;
  }

  .available-coupon-item .coupon-left {
    width: 100%;
  }

  .stock-progress {
    left: 0;
  }
}

@media (max-width: 992px) {
  .card-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
}
</style>

<!-- 非 scoped 样式，覆盖 append-to-body 的 Element Plus 对话框 -->
<style>
/* 使用更精确的选择器来覆盖 Element Plus 对话框 */
.el-overlay-dialog .coupon-dialog.el-dialog,
.coupon-dialog.el-dialog {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(192, 57, 43, 0.3) !important;
  --el-dialog-bg-color: transparent !important;
}

.el-overlay-dialog .coupon-dialog .el-dialog__header,
.coupon-dialog .el-dialog__header,
.coupon-dialog.el-dialog .el-dialog__header {
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.6) 0%, rgba(192, 57, 43, 0.4) 100%) !important;
  padding: 25px 30px !important;
  border-bottom: 1px solid rgba(192, 57, 43, 0.3) !important;
}

.el-overlay-dialog .coupon-dialog .el-dialog__title,
.coupon-dialog .el-dialog__title,
.coupon-dialog.el-dialog .el-dialog__title {
  color: white !important;
  font-size: 20px !important;
  font-weight: 600 !important;
}

.el-overlay-dialog .coupon-dialog .el-dialog__headerbtn .el-dialog__close,
.coupon-dialog .el-dialog__headerbtn .el-dialog__close,
.coupon-dialog.el-dialog .el-dialog__headerbtn .el-dialog__close {
  color: white !important;
  font-size: 20px !important;
}

.el-overlay-dialog .coupon-dialog .el-dialog__body,
.coupon-dialog .el-dialog__body,
.coupon-dialog.el-dialog .el-dialog__body {
  padding: 20px !important;
  background: transparent !important;
}

.el-overlay {
  background-color: rgba(0, 0, 0, 0.6) !important;
}
</style>
