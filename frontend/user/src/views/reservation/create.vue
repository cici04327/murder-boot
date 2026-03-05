<template>
  <div class="reservation-create-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="header-icon">🎟️</div>
        <h1>预约剧本</h1>
        <p class="header-subtitle">开启一场神秘的推理之旅</p>
      </div>
    </div>

    <!-- 步骤指示器 -->
    <div class="steps-container">
      <div class="step-item" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <div class="step-icon">📜</div>
        <div class="step-label">选择剧本</div>
      </div>
      <div class="step-line" :class="{ active: currentStep > 1 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <div class="step-icon">🏠</div>
        <div class="step-label">选择门店</div>
      </div>
      <div class="step-line" :class="{ active: currentStep > 2 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
        <div class="step-icon">⏰</div>
        <div class="step-label">预约时间</div>
      </div>
      <div class="step-line" :class="{ active: currentStep > 3 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 4 }">
        <div class="step-icon">✅</div>
        <div class="step-label">确认预约</div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧表单 -->
      <div class="form-section">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="reservation-form"
        >
          <!-- 剧本选择卡片 -->
          <div class="form-card">
            <div class="card-header">
              <span class="card-icon">📜</span>
              <span class="card-title">选择剧本</span>
            </div>
            <div class="card-body">
              <el-form-item prop="scriptId">
                <el-select
                  v-model="form.scriptId"
                  placeholder="请选择想要体验的剧本"
                  filterable
                  size="large"
                  @change="handleScriptChange"
                  class="full-width"
                >
                  <el-option
                    v-for="script in scripts"
                    :key="script.id"
                    :label="script.name"
                    :value="script.id"
                  >
                    <div class="script-option">
                      <span class="script-name">{{ script.name }}</span>
                      <span class="script-meta">
                        <span class="player-tag">👥 {{ script.playerCount }}人</span>
                        <span class="price-tag">¥{{ script.price }}/人</span>
                      </span>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
              
              <!-- 选中剧本信息展示 -->
              <div class="selected-script" v-if="selectedScript">
                <div class="script-preview">
                  <div class="script-info">
                    <h4>{{ selectedScript.name }}</h4>
                    <div class="script-tags">
                      <span class="tag">👥 {{ selectedScript.playerCount }}人本</span>
                      <span class="tag" v-if="selectedScript.duration">⏱️ {{ selectedScript.duration }}小时</span>
                      <span class="tag price">¥{{ selectedScript.price }}/人</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 门店选择卡片 -->
          <div class="form-card">
            <div class="card-header">
              <span class="card-icon">🏠</span>
              <span class="card-title">选择门店</span>
            </div>
            <div class="card-body">
              <el-form-item prop="storeId">
                <el-select
                  v-model="form.storeId"
                  placeholder="请选择门店"
                  filterable
                  size="large"
                  @change="handleStoreChange"
                  class="full-width"
                >
                  <el-option
                    v-for="store in stores"
                    :key="store.id"
                    :label="store.name"
                    :value="store.id"
                  >
                    <div class="store-option">
                      <div class="store-name">{{ store.name }}</div>
                      <div class="store-address">📍 {{ store.address }}</div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
              
              <!-- 房间选择 -->
              <el-form-item prop="roomId" v-if="rooms.length > 0">
                <div class="room-label">选择房间</div>
                <div class="room-grid">
                  <div 
                    v-for="room in availableRooms" 
                    :key="room.id"
                    class="room-card"
                    :class="{ selected: form.roomId === room.id }"
                    @click="form.roomId = room.id"
                  >
                    <div class="room-icon">🚪</div>
                    <div class="room-name">{{ room.name }}</div>
                    <div class="room-capacity">可容纳 {{ room.capacity }} 人</div>
                    <div class="room-check" v-if="form.roomId === room.id">✓</div>
                  </div>
                </div>
              </el-form-item>
            </div>
          </div>

          <!-- 时间选择卡片 -->
          <div class="form-card">
            <div class="card-header">
              <span class="card-icon">⏰</span>
              <span class="card-title">预约时间</span>
            </div>
            <div class="card-body">
              <div class="time-picker-group">
                <el-form-item prop="reservationDate" class="date-picker">
                  <template #label><span class="form-label">选择日期</span></template>
                  <el-date-picker
                    v-model="form.reservationDate"
                    type="date"
                    placeholder="选择日期"
                    :disabled-date="disabledDate"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    size="large"
                    class="full-width"
                  />
                </el-form-item>
                
                <el-form-item prop="reservationTime" class="time-picker">
                  <template #label><span class="form-label">选择时段</span></template>
                  <el-time-select
                    v-model="form.reservationTime"
                    start="09:00"
                    step="00:30"
                    end="21:00"
                    placeholder="选择时间"
                    size="large"
                    class="full-width"
                  />
                </el-form-item>
              </div>
              
              <!-- 时间段显示提示 -->
              <div class="time-range-display" v-if="form.reservationTime && selectedScript?.duration">
                <div class="time-range-card">
                  <span class="time-range-icon">🕐</span>
                  <div class="time-range-content">
                    <div class="time-range-label">您选择的时间段</div>
                    <div class="time-range-value">
                      {{ form.reservationTime }} - {{ endTime }}
                      <span class="duration-tag">（共{{ selectedScript.duration }}小时）</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 人数与联系方式卡片 -->
          <div class="form-card">
            <div class="card-header">
              <span class="card-icon">👥</span>
              <span class="card-title">人数与联系方式</span>
            </div>
            <div class="card-body">
              <el-form-item prop="playerCount">
                <template #label><span class="form-label">参与人数</span></template>
                <div class="player-count-wrapper">
                  <el-input-number
                    v-model="form.playerCount"
                    :min="1"
                    :max="selectedScript?.playerCount || 12"
                    size="large"
                  />
                  <span class="player-hint" v-if="selectedScript">
                    （该剧本需要 <strong>{{ selectedScript.playerCount }}</strong> 人）
                  </span>
                </div>
                
                <!-- 人数状态提示 -->
                <div class="player-status" v-if="selectedScript">
                  <div class="status-card warning" v-if="form.playerCount < selectedScript.playerCount">
                    <div class="status-icon">🎭</div>
                    <div class="status-content">
                      <div class="status-title">人数不足，将自动发起拼单</div>
                      <div class="status-desc">
                        还差 <strong>{{ selectedScript.playerCount - form.playerCount }}</strong> 人，
                        提交后系统将自动在拼单大厅发起组队，等待其他玩家加入
                      </div>
                    </div>
                  </div>
                  <div class="status-card success" v-else>
                    <div class="status-icon">✅</div>
                    <div class="status-content">
                      <div class="status-title">人数已满足要求</div>
                      <div class="status-desc">可直接预约，无需等待其他玩家</div>
                    </div>
                  </div>
                </div>
              </el-form-item>
              
              <div class="contact-group">
                <el-form-item prop="contactName" class="contact-item">
                  <template #label><span class="form-label">联系人</span></template>
                  <el-input 
                    v-model="form.contactName" 
                    placeholder="请输入联系人姓名" 
                    size="large"
                    prefix-icon="User"
                  />
                </el-form-item>
                
                <el-form-item prop="contactPhone" class="contact-item">
                  <template #label><span class="form-label">联系电话</span></template>
                  <el-input 
                    v-model="form.contactPhone" 
                    placeholder="请输入联系电话" 
                    size="large"
                    prefix-icon="Phone"
                  />
                </el-form-item>
              </div>
            </div>
          </div>

          <!-- 优惠券与备注卡片 -->
          <div class="form-card" v-if="availableCoupons.length > 0 || true">
            <div class="card-header">
              <span class="card-icon">🎁</span>
              <span class="card-title">优惠与备注</span>
            </div>
            <div class="card-body">
              <el-form-item v-if="availableCoupons.length > 0">
                <template #label><span class="form-label">选择优惠券</span></template>
                <el-select 
                  v-model="form.userCouponId" 
                  placeholder="选择优惠券（可不选）" 
                  clearable 
                  @change="handleCouponChange"
                  size="large"
                  class="full-width"
                >
                  <el-option
                    v-for="coupon in availableCoupons"
                    :key="coupon.id"
                    :label="`${coupon.couponName}`"
                    :value="coupon.id"
                  >
                    <div class="coupon-option">
                      <span class="coupon-name">🎫 {{ coupon.couponName }}</span>
                      <span class="coupon-value">
                        {{ coupon.type === 1 ? `减¥${coupon.discountValue}` : coupon.type === 2 ? `${coupon.discountValue * 10}折` : `抵用¥${coupon.discountValue}` }}
                      </span>
                    </div>
                  </el-option>
                </el-select>
                <div class="coupon-hint" v-if="selectedCoupon && selectedCoupon.minAmount">
                  满¥{{ selectedCoupon.minAmount }}可用
                </div>
              </el-form-item>
              
              <el-form-item>
                <template #label><span class="form-label">备注信息</span></template>
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="有什么特殊需求可以在这里说明，如：需要停车位、有新手玩家等"
                  size="large"
                />
              </el-form-item>
            </div>
          </div>
        </el-form>
      </div>

      <!-- 右侧订单摘要 -->
      <div class="summary-section">
        <div class="summary-card sticky">
          <div class="summary-header">
            <span class="summary-icon">📋</span>
            <span class="summary-title">订单摘要</span>
          </div>
          
          <div class="summary-body">
            <!-- 剧本信息 -->
            <div class="summary-item" v-if="selectedScript">
              <div class="item-label">剧本</div>
              <div class="item-value">{{ selectedScript.name }}</div>
            </div>
            
            <!-- 门店信息 -->
            <div class="summary-item" v-if="form.storeId">
              <div class="item-label">门店</div>
              <div class="item-value">{{ stores.find(s => s.id === form.storeId)?.name }}</div>
            </div>
            
            <!-- 房间信息 -->
            <div class="summary-item" v-if="form.roomId">
              <div class="item-label">房间</div>
              <div class="item-value">{{ rooms.find(r => r.id === form.roomId)?.name }}</div>
            </div>
            
            <!-- 时间信息 -->
            <div class="summary-item" v-if="form.reservationDate && form.reservationTime">
              <div class="item-label">时间</div>
              <div class="item-value highlight">
                {{ form.reservationDate }} {{ timeRangeText }}
                <div class="duration-hint" v-if="selectedScript?.duration">（{{ selectedScript.duration }}小时）</div>
              </div>
            </div>
            
            <!-- 人数信息 -->
            <div class="summary-item">
              <div class="item-label">人数</div>
              <div class="item-value">
                {{ form.playerCount }}人
                <span class="need-group" v-if="needGroup">（需拼单）</span>
              </div>
            </div>
            
            <div class="summary-divider"></div>
            
            <!-- 价格明细 -->
            <div class="price-detail">
              <div class="price-row">
                <span>单价</span>
                <span>¥{{ selectedScript?.price || 0 }}/人</span>
              </div>
              <div class="price-row">
                <span>人数</span>
                <span>× {{ form.playerCount }}</span>
              </div>
              <div class="price-row">
                <span>小计</span>
                <span :style="vipDiscountAmount > 0 ? 'color:#909399;text-decoration:line-through' : ''">
                  ¥{{ totalPrice }}
                </span>
              </div>
              <div class="price-row vip-discount" v-if="vipDiscountRate">
                <span>
                  <el-tag size="small" type="warning" style="margin-right:4px">💎{{ vipLevelName }}</el-tag>
                  {{ Math.round(vipDiscountRate * 10) }}折
                </span>
                <span style="color:#e6a23c;font-weight:bold">-¥{{ vipDiscountAmount }}</span>
              </div>
              <div class="price-row discount" v-if="couponDiscountAmount > 0">
                <span>🎫 优惠券</span>
                <span style="color:#67c23a;font-weight:bold">-¥{{ couponDiscountAmount }}</span>
              </div>
            </div>
            
            <div class="total-price">
              <span class="total-label">应付金额</span>
              <span class="total-value">¥{{ finalPrice }}</span>
            </div>
            <div class="vip-saved-tip" v-if="vipDiscountAmount > 0">
              💎 VIP专属优惠已为您节省 ¥{{ vipDiscountAmount }}
            </div>
          </div>
          
          <div class="summary-actions">
            <el-button 
              type="primary" 
              size="large" 
              class="submit-btn"
              @click="handleSubmit" 
              :loading="submitting"
            >
              <span class="btn-icon">🎟️</span>
              {{ needGroup ? '提交预约并发起拼单' : '立即预约' }}
            </el-button>
            <el-button size="large" class="cancel-btn" @click="router.back()">
              返回
            </el-button>
          </div>
          
          <div class="summary-tips">
            <p>💡 预约成功后请准时到店</p>
            <p>🔒 您的个人信息将被安全保护</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScriptList } from '@/api/script'
import { getStoreList, getStoreRooms } from '@/api/store'
import { createReservation, checkRoomAvailability } from '@/api/reservation'
import { createGroup } from '@/api/group'
import { getMyCoupons } from '@/api/coupon'
import { getUserVipInfo } from '@/api/vip'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 计算当前步骤
const currentStep = computed(() => {
  if (!form.scriptId) return 1
  if (!form.storeId || !form.roomId) return 2
  if (!form.reservationDate || !form.reservationTime) return 3
  return 4
})

const formRef = ref(null)
const submitting = ref(false)
const checkingAvailability = ref(false)
const scripts = ref([])
const stores = ref([])
const rooms = ref([])
const availableCoupons = ref([])
const selectedCoupon = ref(null)

// VIP折扣相关
const vipDiscountRate = ref(null)   // 折扣率，如 0.9 表示9折
const vipLevelName = ref('')        // 等级名称，如"银章侦探"
const vipDiscountMap = { 1: 0.95, 2: 0.90, 3: 0.85, 4: 0.80 }
const vipLevelNameMap = { 1: '见习侦探', 2: '银章侦探', 3: '金章侦探', 4: '传奇侦探' }

const loadVipInfo = async () => {
  try {
    const res = await getUserVipInfo()
    if ((res.code === 1 || res.code === 200) && res.data?.isVip) {
      const level = res.data.level
      vipDiscountRate.value = vipDiscountMap[level] || null
      vipLevelName.value = vipLevelNameMap[level] || ''
    } else {
      vipDiscountRate.value = null
      vipLevelName.value = ''
    }
  } catch (e) {
    vipDiscountRate.value = null
  }
}

const form = reactive({
  scriptId: null,
  storeId: null,
  roomId: null,
  reservationDate: '',
  reservationTime: '',
  playerCount: 1,
  contactPhone: '',
  contactName: '',
  remark: '',
  userCouponId: null
})

const rules = {
  scriptId: [{ required: true, message: '请选择剧本', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  reservationDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  reservationTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  playerCount: [{ required: true, message: '请输入人数', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 监听时间变化，自动检查可用性
watch([() => form.roomId, () => form.reservationDate, () => form.reservationTime], () => {
  if (form.roomId && form.reservationDate && form.reservationTime) {
    checkAvailability()
  }
})

const selectedScript = computed(() => {
  return scripts.value.find(s => s.id === form.scriptId)
})

// 计算结束时间（根据开始时间和剧本时长）
const calculateEndTime = (startTime, durationHours) => {
  if (!startTime || !durationHours) return ''
  
  const [hours, minutes] = startTime.split(':').map(Number)
  const totalMinutes = hours * 60 + minutes + durationHours * 60
  const endHours = Math.floor(totalMinutes / 60)
  const endMinutes = totalMinutes % 60
  
  // 格式化为 HH:mm
  return `${String(endHours).padStart(2, '0')}:${String(endMinutes).padStart(2, '0')}`
}

// 计算完整时间段显示文本
const timeRangeText = computed(() => {
  if (!form.reservationTime || !selectedScript.value?.duration) {
    return form.reservationTime || ''
  }
  
  const endTime = calculateEndTime(form.reservationTime, selectedScript.value.duration)
  return `${form.reservationTime} - ${endTime}`
})

// 计算结束时间
const endTime = computed(() => {
  if (!form.reservationTime || !selectedScript.value?.duration) {
    return ''
  }
  return calculateEndTime(form.reservationTime, selectedScript.value.duration)
})

const availableRooms = computed(() => {
  return rooms.value.filter(r => r.status === 1)
})

// 原始总价（剧本单价 × 人数）
const totalPrice = computed(() => {
  if (!selectedScript.value) return 0
  return (selectedScript.value.price * form.playerCount).toFixed(2)
})

// VIP折扣金额
const vipDiscountAmount = computed(() => {
  if (!vipDiscountRate.value || !selectedScript.value) return 0
  const total = parseFloat(totalPrice.value)
  return (total * (1 - vipDiscountRate.value)).toFixed(2)
})

// VIP折后价（优惠券在此基础上再折扣）
const priceAfterVip = computed(() => {
  const total = parseFloat(totalPrice.value)
  const vipDiscount = parseFloat(vipDiscountAmount.value)
  return Math.max(total - vipDiscount, 0)
})

// 优惠券折扣金额（基于VIP折后价计算）
const couponDiscountAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  const base = priceAfterVip.value
  if (selectedCoupon.value.type === 1) {
    // 满减券
    if (base >= parseFloat(selectedCoupon.value.minAmount || 0)) {
      return parseFloat(selectedCoupon.value.discountValue)
    }
  } else if (selectedCoupon.value.type === 2) {
    // 折扣券
    return (base * (1 - parseFloat(selectedCoupon.value.discountValue))).toFixed(2)
  } else if (selectedCoupon.value.type === 3) {
    // 代金券
    return parseFloat(selectedCoupon.value.discountValue)
  }
  return 0
})

// 合计优惠（VIP + 优惠券）
const discountAmount = computed(() => {
  return (parseFloat(vipDiscountAmount.value) + parseFloat(couponDiscountAmount.value)).toFixed(2)
})

// 最终实付金额
const finalPrice = computed(() => {
  const total = parseFloat(totalPrice.value)
  const discount = parseFloat(discountAmount.value)
  return Math.max(total - discount, 0).toFixed(2)
})

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

const loadScripts = async () => {
  try {
    const res = await getScriptList({ page: 1, pageSize: 100 })
    if (res.data) {
      scripts.value = res.data.records || res.data.list || res.data
    }
  } catch (error) {
    console.error('加载剧本失败:', error)
    // 模拟数据
    scripts.value = [
      { id: 1, name: '迷雾庄园', price: 88, playerCount: 6 },
      { id: 2, name: '时光旅人', price: 68, playerCount: 5 }
    ]
  }
}

const loadStores = async () => {
  try {
    const res = await getStoreList({ page: 1, pageSize: 100 })
    if (res.data) {
      stores.value = res.data.records || res.data.list || res.data
    }
  } catch (error) {
    console.error('加载门店失败:', error)
    // 模拟数据
    stores.value = [
      { id: 1, name: '探案密室', address: '北京市朝阳区xxx' },
      { id: 2, name: '时空剧本馆', address: '北京市海淀区xxx' }
    ]
  }
}

const handleScriptChange = (scriptId) => {
  const script = scripts.value.find(s => s.id === scriptId)
  if (script) {
    // 不再强制设置为剧本人数，保持用户当前选择或默认1人
    if (form.playerCount > script.playerCount) {
      form.playerCount = script.playerCount
    }
  }
  // 加载可用优惠券
  loadAvailableCoupons()
}

const handleCouponChange = (couponId) => {
  selectedCoupon.value = availableCoupons.value.find(c => c.id === couponId)
}

const handleStoreChange = async (storeId) => {
  form.roomId = null
  rooms.value = []
  
  try {
    const res = await getStoreRooms(storeId)
    if (res.data) {
      rooms.value = res.data
    }
  } catch (error) {
    console.error('加载房间失败:', error)
    // 模拟数据
    rooms.value = [
      { id: 1, name: '推理房A', capacity: 6, status: 1 },
      { id: 2, name: '推理房B', capacity: 8, status: 1 }
    ]
  }
}

const loadAvailableCoupons = async () => {
  if (!userStore.userInfo?.id) return
  
  try {
    // 获取用户的未使用优惠券
    const res = await getMyCoupons({ status: 1, page: 1, pageSize: 100 })
    if (res.code === 1 || res.code === 200) {
      availableCoupons.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载优惠券失败:', error)
  }
}

const checkAvailability = async () => {
  if (!form.roomId || !form.reservationDate || !form.reservationTime) {
    return true
  }
  
  checkingAvailability.value = true
  try {
    const res = await checkRoomAvailability({
      roomId: form.roomId,
      reservationTime: `${form.reservationDate} ${form.reservationTime}`,
      duration: selectedScript.value?.duration || 3
    })
    
    if (res.data === false || res.code === 0) {
      ElMessage.warning('该时段房间已被预约，请选择其他时间')
      return false
    }
    return true
  } catch (error) {
    console.error('检查房间可用性失败:', error)
    return true // 检查失败时允许继续，由后端再次验证
  } finally {
    checkingAvailability.value = false
  }
}

// 判断是否需要拼单
const needGroup = computed(() => {
  if (!selectedScript.value) return false
  return form.playerCount < selectedScript.value.playerCount
})

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 检查房间可用性
      const isAvailable = await checkAvailability()
      if (!isAvailable) {
        return
      }
      
      submitting.value = true
      try {
        // 计算总价格和实际价格
        const totalAmount = parseFloat(totalPrice.value)
        const actualAmount = parseFloat(finalPrice.value)
        
        // 构建预约数据
        const reservationData = {
          storeId: form.storeId,
          roomId: form.roomId,
          scriptId: form.scriptId,
          playerCount: form.playerCount,
          reservationTime: `${form.reservationDate} ${form.reservationTime}`,
          duration: selectedScript.value?.duration || 3.0,
          totalPrice: totalAmount,          // 原价
          discountAmount: parseFloat(discountAmount.value),   // 合计优惠
          vipDiscountAmount: parseFloat(vipDiscountAmount.value), // VIP折扣金额
          vipDiscount: vipDiscountRate.value,                 // VIP折扣率
          actualAmount: parseFloat(finalPrice.value),         // 实付金额
          contactName: form.contactName,
          contactPhone: form.contactPhone,
          remark: form.remark,
          userId: userStore.userInfo?.id,
          userCouponId: form.userCouponId || null
        }
        console.log('预约提交数据:', reservationData)
        
        // 如果人数不足，需要发起拼单
        if (needGroup.value) {
          // 先创建预约
          const resReservation = await createReservation(reservationData)
          if (resReservation.code === 1 || resReservation.code === 200) {
            // 自动发起拼单
            const selectedStore = stores.value.find(s => s.id === form.storeId)
            const groupData = {
              scriptId: form.scriptId,
              scriptName: selectedScript.value.name,
              storeId: form.storeId,
              storeName: selectedStore?.name || '',
              playTime: `${form.reservationDate} ${form.reservationTime}`,
              currentCount: form.playerCount,
              needCount: selectedScript.value.playerCount,
              playerCount: selectedScript.value.playerCount,
              price: selectedScript.value.price,
              genderRequirement: '男女不限',
              newbieWelcome: true,
              description: form.remark || '欢迎小伙伴一起来玩！',
              reservationId: resReservation.data.id || resReservation.data
            }
            
            try {
              const resGroup = await createGroup(groupData)
              if (resGroup.code === 1 || resGroup.code === 200) {
                ElMessage.success({
                  message: `预约成功！已自动发起拼单，还差${selectedScript.value.playerCount - form.playerCount}人`,
                  duration: 4000
                })
                router.push(`/group/${resGroup.data.id || resGroup.data}`)
              } else {
                // 拼单创建失败，但预约成功
                ElMessage.warning('预约成功，但拼单发起失败，您可以手动发起拼单')
                router.push(`/reservation/confirm/${resReservation.data.id || resReservation.data}`)
              }
            } catch (groupError) {
              console.error('发起拼单失败:', groupError)
              ElMessage.warning('预约成功，但拼单发起失败，您可以手动发起拼单')
              router.push(`/reservation/confirm/${resReservation.data.id || resReservation.data}`)
            }
          }
        } else {
          // 人数足够，直接预约
          const res = await createReservation(reservationData)
          if (res.code === 1 || res.code === 200) {
            ElMessage.success('预约成功！')
            router.push(`/reservation/confirm/${res.data.id || res.data}`)
          }
        }
      } catch (error) {
        console.error('创建预约失败:', error)
        ElMessage.error(error.response?.data?.msg || '创建预约失败，请重试')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 监听房间列表变化，自动选择预约的房间
watch(rooms, (newRooms) => {
  // 如果URL中有roomId参数，自动选中该房间
  if (route.query.roomId && newRooms.length > 0) {
    const roomId = parseInt(route.query.roomId)
    const targetRoom = newRooms.find(r => r.id === roomId)
    
    if (targetRoom) {
      form.roomId = roomId
      
      // 如果房间不可用，给出提示
      if (targetRoom.status !== 1) {
        ElMessage.warning(`房间"${targetRoom.name}"当前不可预约，请选择其他房间`)
      } else {
        const roomName = route.query.roomName || targetRoom.name
        ElMessage.success(`已自动选择房间：${roomName}`)
      }
    }
  }
})

onMounted(async () => {
  // 先加载剧本、门店列表、优惠券和VIP信息
  await Promise.all([loadScripts(), loadStores(), loadAvailableCoupons(), loadVipInfo()])
  
  // 从URL参数中获取预选值
  if (route.query.scriptId) {
    form.scriptId = parseInt(route.query.scriptId)
    handleScriptChange(form.scriptId)
  }
  
  // 如果有门店参数，先加载门店的房间列表
  if (route.query.storeId) {
    form.storeId = parseInt(route.query.storeId)
    await handleStoreChange(form.storeId)
    // 房间列表加载后，watch会自动处理roomId的选择
  }
  
  // 预填联系信息
  if (userStore.userInfo) {
    form.contactName = userStore.userInfo.name || userStore.userInfo.username
    form.contactPhone = userStore.userInfo.phone || ''
  }
  
  // 如果同时有门店和房间信息，显示提示
  if (route.query.storeId && route.query.roomId) {
    const storeName = stores.value.find(s => s.id === parseInt(route.query.storeId))?.name
    if (storeName) {
      console.log(`从门店"${storeName}"跳转，正在加载房间信息...`)
    }
  }
})
</script>

<style scoped>
.reservation-create-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

/* 页面头部 */
.page-header {
  position: relative;
  text-align: center;
  padding: 50px 20px;
  border-radius: 20px;
  margin-bottom: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 0, 0, 0.2) 0%, transparent 50%);
}

.header-content {
  position: relative;
  z-index: 1;
}

.header-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.page-header h1 {
  font-size: 32px;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  letter-spacing: 3px;
}

.header-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
  letter-spacing: 2px;
}

/* 步骤指示器 */
.steps-container {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  padding: 24px 40px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: all 0.3s;
}

.step-item.active {
  opacity: 1;
}

.step-item.completed .step-icon {
  background: #52c41a;
}

.step-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: rgba(60, 60, 80, 0.8);
  border-radius: 50%;
  transition: all 0.3s;
}

.step-item.active .step-icon {
  background: linear-gradient(135deg, #8B0000 0%, #c41e3a 100%);
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.3);
}

.step-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.step-line {
  width: 60px;
  height: 3px;
  background: rgba(60, 60, 80, 0.8);
  margin: 0 12px 24px;
  border-radius: 2px;
  transition: all 0.3s;
}

.step-line.active {
  background: linear-gradient(90deg, #8B0000, #c41e3a);
}

/* 主要内容区 */
.main-content {
  display: flex;
  gap: 24px;
}

.form-section {
  flex: 1;
}

.summary-section {
  width: 360px;
  flex-shrink: 0;
}

/* 表单卡片 */
.form-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 16px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.card-icon {
  font-size: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.card-body {
  padding: 20px;
  background: transparent;
}

.full-width {
  width: 100%;
}

.form-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

/* 剧本选项样式 */
.script-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 4px 0;
}

.script-name {
  font-weight: 500;
}

.script-meta {
  display: flex;
  gap: 12px;
}

.player-tag {
  color: #666;
  font-size: 13px;
}

.price-tag {
  color: #8B0000;
  font-weight: 600;
}

/* 选中剧本展示 */
.selected-script {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.15) 0%, rgba(139, 0, 0, 0.08) 100%);
  border-radius: 12px;
  border: 1px dashed rgba(139, 0, 0, 0.3);
}

.script-preview {
  display: flex;
  gap: 16px;
}

.script-info h4 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #fff;
}

.script-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.script-tags .tag {
  padding: 4px 12px;
  background: rgba(139, 0, 0, 0.25);
  border-radius: 15px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.script-tags .tag.price {
  background: rgba(139, 0, 0, 0.35);
  color: #ff6b6b;
  font-weight: 600;
}

/* 门店选项样式 */
.store-option {
  padding: 4px 0;
}

.store-name {
  font-weight: 500;
  margin-bottom: 2px;
}

.store-address {
  font-size: 12px;
  color: #999;
}

/* 房间选择 */
.room-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 12px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.room-card {
  position: relative;
  padding: 16px;
  background: rgba(35, 35, 60, 0.8);
  border: 2px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.room-card:hover {
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(45, 45, 75, 0.9);
}

.room-card.selected {
  border-color: #8B0000;
  background: rgba(139, 0, 0, 0.2);
}

.room-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.room-name {
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.room-capacity {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.room-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background: #8B0000;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

/* 时间选择 */
.time-picker-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* 时间段显示 */
.time-range-display {
  margin-top: 16px;
}

.time-range-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.08), rgba(139, 0, 0, 0.03));
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
}

.time-range-icon {
  font-size: 28px;
}

.time-range-content {
  flex: 1;
}

.time-range-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 4px;
}

.time-range-value {
  font-size: 18px;
  font-weight: 600;
  color: #ff6b6b;
}

.time-range-value .duration-tag {
  font-size: 14px;
  font-weight: normal;
  color: rgba(255, 255, 255, 0.7);
}

/* 摘要中的时长提示 */
.duration-hint {
  font-size: 12px;
  color: #999;
  font-weight: normal;
  margin-top: 2px;
}

/* 人数与联系方式 */
.player-count-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.player-hint {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.player-hint strong {
  color: #ff6b6b;
}

.player-status {
  margin-top: 16px;
}

.status-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
}

.status-card.warning {
  background: rgba(230, 162, 60, 0.15);
  border: 1px solid rgba(230, 162, 60, 0.4);
}

.status-card.success {
  background: rgba(82, 196, 26, 0.15);
  border: 1px solid rgba(82, 196, 26, 0.4);
}

.status-icon {
  font-size: 24px;
}

.status-title {
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
}

.status-desc strong {
  color: #ff6b6b;
}

.contact-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 16px;
}

/* 优惠券选项 */
.coupon-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.coupon-name {
  font-weight: 500;
}

.coupon-value {
  color: #f56c6c;
  font-weight: 600;
}

.coupon-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

/* 订单摘要 */
.summary-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.summary-card.sticky {
  position: sticky;
  top: 20px;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.summary-icon {
  font-size: 20px;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
}

.summary-body {
  padding: 20px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(139, 0, 0, 0.2);
}

.item-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.item-value {
  color: #fff;
  font-weight: 500;
}

.item-value.highlight {
  color: #ff6b6b;
}

.need-group {
  font-size: 12px;
  color: #e6a23c;
  font-weight: normal;
}

.summary-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(139, 0, 0, 0.3), transparent);
  margin: 16px 0;
}

.price-detail {
  margin-bottom: 16px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.price-row.discount span:last-child {
  color: #7ddc7a;
}

.price-row.vip-discount {
  background: rgba(230, 162, 60, 0.1);
  border-radius: 6px;
  padding: 6px 8px;
  margin: 4px 0;
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.total-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.2) 0%, rgba(139, 0, 0, 0.15) 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.total-label {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.total-value {
  font-size: 28px;
  font-weight: bold;
  color: #ff6b6b;
}

.vip-saved-tip {
  text-align: center;
  font-size: 13px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 16px;
}

.summary-actions {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  background: linear-gradient(135deg, #8B0000 0%, #c41e3a 100%);
  border: none;
  border-radius: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #a00000 0%, #d42e4a 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(139, 0, 0, 0.4);
}

.cancel-btn {
  width: 100%;
  border-radius: 25px;
}

.btn-icon {
  font-size: 18px;
}

.summary-tips {
  padding: 16px 20px;
  background: rgba(26, 26, 46, 0.5);
  border-top: 1px solid rgba(139, 0, 0, 0.2);
}

.summary-tips p {
  margin: 0 0 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.summary-tips p:last-child {
  margin-bottom: 0;
}

/* 响应式 */
@media (max-width: 992px) {
  .main-content {
    flex-direction: column;
  }
  
  .summary-section {
    width: 100%;
  }
  
  .summary-card.sticky {
    position: static;
  }
  
  .steps-container {
    padding: 16px;
    overflow-x: auto;
  }
  
  .step-line {
    width: 30px;
  }
}

@media (max-width: 768px) {
  .time-picker-group,
  .contact-group {
    grid-template-columns: 1fr;
  }
  
  .page-header h1 {
    font-size: 24px;
  }
  
  .room-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Element Plus 输入框深色主题 */
:deep(.el-input__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.5) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
  background-color: transparent !important;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-input__prefix .el-icon),
:deep(.el-input__suffix .el-icon) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* Element Plus 选择器深色主题 */
:deep(.el-select__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.5) inset !important;
}

:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}

:deep(.el-select__placeholder),
:deep(.el-select__selected-item) {
  color: #fff !important;
}

:deep(.el-select__caret) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* Element Plus 日期选择器深色主题 */
:deep(.el-date-editor.el-input__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
}

/* Element Plus 表单标签深色主题 */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* Element Plus 文本域深色主题 */
:deep(.el-textarea__inner) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
  color: #fff !important;
}

:deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.5) inset !important;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}

:deep(.el-input__count) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.5) !important;
}

/* Element Plus 数字输入框深色主题 */
:deep(.el-input-number) {
  background-color: transparent !important;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  border-color: rgba(139, 0, 0, 0.3) !important;
  color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: #ff6b6b !important;
}
</style>
