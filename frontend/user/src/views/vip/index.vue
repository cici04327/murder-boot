<template>
  <div class="vip-container">
    <!-- 剧本杀主题背景装饰 -->
    <div class="mystery-bg">
      <div class="floating-icon icon-1">👑</div>
      <div class="floating-icon icon-2">💎</div>
      <div class="floating-icon icon-3">🏆</div>
      <div class="floating-icon icon-4">⭐</div>
    </div>

    <!-- 顶部横幅 - 剧本杀主题 -->
    <div class="vip-banner mystery-banner">
      <div class="banner-content">
        <div class="banner-left">
          <div class="vip-icon mystery-icon">
            <span class="icon-emoji">👑</span>
          </div>
          <div class="banner-text">
            <h1>🎭 侦探俱乐部</h1>
            <p>解锁专属特权，成为传奇侦探</p>
          </div>
        </div>
        <div class="banner-right" v-if="userVipInfo">
          <div class="current-vip" v-if="userVipInfo.level > 0">
            <!-- VIP等级徽章 -->
            <div class="vip-badge-large" :class="`level-${userVipInfo.level}`">
              <el-icon class="badge-icon"><Medal /></el-icon>
              <div class="badge-text">
                <div class="level-name">{{ getLevelName(userVipInfo.level) }}</div>
                <div class="level-desc">{{ getLevelDescription(userVipInfo.level) }}</div>
              </div>
            </div>
            
            <!-- 有效期信息 -->
            <div class="vip-expire-card">
              <div class="expire-item">
                <el-icon class="expire-icon"><Calendar /></el-icon>
                <div class="expire-content">
                  <div class="expire-label">有效期至</div>
                  <div class="expire-date">{{ formatDate(userVipInfo.expireTime) }}</div>
                </div>
              </div>
              <div class="days-remaining" v-if="userVipInfo.daysLeft > 0" :class="{ 'warning': userVipInfo.daysLeft <= 7 }">
                <el-icon><Clock /></el-icon>
                <span>剩余 {{ userVipInfo.daysLeft }} 天</span>
              </div>
              <div class="expired-badge" v-else>
                <el-icon><CircleCheck /></el-icon>
                <span>已过期</span>
              </div>
            </div>

            <!-- 专属权益 -->
            <div class="vip-benefits-mini">
              <div class="benefits-title">专属权益</div>
              <div class="benefits-tags">
                <div class="benefit-tag" v-for="benefit in getVipBenefits(userVipInfo.level)" :key="benefit">
                  <el-icon><Check /></el-icon>
                  <span>{{ benefit }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="non-vip" v-else>
            <div class="non-vip-icon">
              <el-icon><Medal /></el-icon>
            </div>
            <p class="main-text">您还不是VIP会员</p>
            <p class="sub-text">立即开通，享受专属权益</p>
            <el-button type="primary" size="large" @click="scrollToPackages">
              立即开通VIP
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- VIP权益展示 - 剧本杀主题 -->
    <div class="benefits-section mystery-section">
      <h2 class="section-title">
        <span class="title-emoji">🎁</span>
        侦探专属特权
      </h2>
      <div class="benefits-grid">
        <div class="benefit-card mystery-benefit" v-for="benefit in benefits" :key="benefit.icon">
          <div class="benefit-icon">
            <span class="benefit-emoji">{{ benefit.emoji }}</span>
          </div>
          <h3>{{ benefit.title }}</h3>
          <p>{{ benefit.description }}</p>
        </div>
      </div>
    </div>

    <!-- 权益对比表 -->
    <div class="compare-section mystery-section">
      <h2 class="section-title">
        <span class="title-emoji">📊</span>
        等级权益对比
      </h2>
      <div class="compare-table-wrap">
        <table class="compare-table">
          <thead>
            <tr>
              <th class="feature-col">权益项目</th>
              <th v-for="lv in levelList" :key="lv.level" :class="`level-${lv.level}-col`">
                <div class="th-level-badge" :class="`level-${lv.level}`">
                  {{ lv.emoji }} {{ lv.name }}
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in compareRows" :key="row.key">
              <td class="feature-col">
                <span class="row-icon">{{ row.icon }}</span>{{ row.label }}
              </td>
              <td v-for="lv in levelList" :key="lv.level" class="value-col">
                <span v-if="row.values[lv.level] === true" class="check-yes">✓</span>
                <span v-else-if="row.values[lv.level] === false" class="check-no">—</span>
                <span v-else class="value-text">{{ row.values[lv.level] }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 生日券 & 月度体验券感知模块 -->
    <div class="coupon-perks-section mystery-section">
      <h2 class="section-title">
        <span class="title-emoji">🎟️</span>
        专属券福利
      </h2>
      <div class="coupon-perks-grid">
        <div class="coupon-perk-card">
          <div class="cp-icon">🎂</div>
          <div class="cp-body">
            <div class="cp-title">生日专享券</div>
            <div class="cp-desc">生日当月自动发放，无需领取</div>
            <div class="cp-detail">
              <span class="cp-level l1">见习 ¥30</span>
              <span class="cp-level l2">银章 ¥80</span>
              <span class="cp-level l3">金章 ¥150</span>
              <span class="cp-level l4">传奇 ¥200</span>
            </div>
            <div class="cp-tip">📌 每月最后一天到期，请及时使用</div>
          </div>
        </div>
        <div class="coupon-perk-card" :class="{ 'highlight-card': monthlyCouponStatus?.alreadyGranted }">
          <div class="cp-icon">🎫</div>
          <div class="cp-body">
            <div class="cp-title">月度体验券</div>
            <div class="cp-desc">每月1日自动发放到账户</div>
            <div class="cp-detail">
              <span class="cp-level l1">见习 2张×¥10</span>
              <span class="cp-level l2">银章 5张×¥20</span>
              <span class="cp-level l3">金章 10张×¥50</span>
              <span class="cp-level l4">传奇 15张×¥100</span>
            </div>

            <!-- 当前用户本月发放状态（仅VIP用户显示） -->
            <div class="cp-grant-status" v-if="monthlyCouponStatus?.isVip">
              <div class="grant-row" v-if="monthlyCouponStatus.alreadyGranted">
                <span class="grant-done">✅ 本月已发放</span>
                <span class="grant-count">{{ monthlyCouponStatus.grantedCount }} 张 × ¥{{ monthlyCouponStatus.couponAmount }}</span>
              </div>
              <div class="grant-row" v-else>
                <span class="grant-pending">⏳ 本月待发放</span>
                <span class="grant-count">{{ monthlyCouponStatus.totalCount }} 张 × ¥{{ monthlyCouponStatus.couponAmount }}</span>
              </div>
              <div class="countdown-row" v-if="countdownText">
                <span class="countdown-label">🕐 下次发放倒计时：</span>
                <span class="countdown-value">{{ countdownText }}</span>
              </div>
            </div>

            <div class="cp-tip">📌 当月最后一天到期，可叠加折扣使用</div>
          </div>
        </div>
        <div class="coupon-perk-card">
          <div class="cp-icon">💎</div>
          <div class="cp-body">
            <div class="cp-title">专属折扣</div>
            <div class="cp-desc">所有剧本预约享受会员价</div>
            <div class="cp-detail">
              <span class="cp-level l1">见习 9.5折</span>
              <span class="cp-level l2">银章 9折</span>
              <span class="cp-level l3">金章 8.5折</span>
              <span class="cp-level l4">传奇 8折</span>
            </div>
            <div class="cp-tip">📌 结算时自动抵扣，无需手动选择</div>
          </div>
        </div>
        <div class="coupon-perk-card">
          <div class="cp-icon">🚀</div>
          <div class="cp-body">
            <div class="cp-title">优先拼单特权</div>
            <div class="cp-desc">热门剧本优先匹配拼单队友</div>
            <div class="cp-detail">
              <span class="cp-level l1">—</span>
              <span class="cp-level l2">银章优先</span>
              <span class="cp-level l3">金章优先</span>
              <span class="cp-level l4">传奇置顶</span>
            </div>
            <div class="cp-tip">📌 拼单大厅展示位置优先级更高</div>
          </div>
        </div>
      </div>
    </div>

    <!-- VIP限定榜单入口 -->
    <div class="vip-exclusive-section mystery-section" v-if="userVipInfo && userVipInfo.level >= 2">
      <h2 class="section-title">
        <span class="title-emoji">👑</span>
        VIP限定特权
      </h2>
      <div class="exclusive-grid">
        <div class="exclusive-card" @click="router.push('/recommend')">
          <div class="ex-icon">🏆</div>
          <div class="ex-title">VIP专属榜单</div>
          <div class="ex-desc">口碑榜·新品榜·稀缺本</div>
          <el-tag type="warning" size="small">仅VIP可见</el-tag>
        </div>
        <div class="exclusive-card" @click="router.push('/reservation/schedule')">
          <div class="ex-icon">⚡</div>
          <div class="ex-title">优先预约通道</div>
          <div class="ex-desc">VIP用户专属预约时段</div>
          <el-tag type="warning" size="small">提前24h开抢</el-tag>
        </div>
        <div class="exclusive-card" @click="router.push('/user/coupons')">
          <div class="ex-icon">🎟️</div>
          <div class="ex-title">我的专属券包</div>
          <div class="ex-desc">查看已发放的月度券和生日券</div>
          <el-tag type="success" size="small">立即查看</el-tag>
        </div>
      </div>
    </div>

    <!-- VIP套餐选择 - 剧本杀主题 -->
    <div class="packages-section mystery-section">
      <h2 class="section-title">
        <span class="title-emoji">🎫</span>
        选择会员等级
      </h2>
      
      <el-row :gutter="20" class="packages-row">
        <el-col :xs="24" :sm="12" :lg="6" v-for="pkg in packages" :key="pkg.id">
          <div 
            class="package-card" 
            :class="{ 
              'recommended': pkg.tag === '推荐',
              'best-value': pkg.tag === '最划算',
              'selected': selectedPackage?.id === pkg.id 
            }"
            @click="selectPackage(pkg)"
          >
            <!-- 标签 -->
            <div class="package-tag" v-if="pkg.tag">{{ pkg.tag }}</div>
            
            <!-- 等级徽章 -->
            <div class="package-level" :class="`level-${pkg.level}`">
              <el-icon><Medal /></el-icon>
              {{ getLevelName(pkg.level) }}
            </div>

            <!-- 套餐名称 -->
            <h3 class="package-name">{{ pkg.name }}</h3>

            <!-- 预约折扣专属标签 -->
            <div class="booking-discount-tag" v-if="pkg.specialDiscount">
              <span class="discount-icon">💎</span>
              <span>预约享 <strong>{{ Math.round(pkg.specialDiscount * 10) }}折</strong> 优惠</span>
            </div>

            <!-- 价格 -->
            <div class="package-price">
              <div class="current-price">
                <span class="currency">¥</span>
                <span class="amount">{{ pkg.currentPrice }}</span>
              </div>
              <div class="original-price" v-if="pkg.originalPrice > pkg.currentPrice">
                原价 ¥{{ pkg.originalPrice }}
              </div>
              <div class="discount-badge" v-if="pkg.discountRate < 1">
                {{ Math.floor(pkg.discountRate * 10) }}折
              </div>
            </div>

            <!-- 时长/有效期 -->
            <div class="package-duration-info">
              <el-icon class="clock-icon"><Clock /></el-icon>
              <span class="duration-text">{{ getDurationText(pkg.durationDays) }}</span>
            </div>

            <!-- 核心权益 -->
            <div class="package-features">
              <div class="feature-item" v-for="(feature, idx) in pkg.features.slice(0, 3)" :key="idx">
                <el-icon class="check-icon"><Check /></el-icon>
                <span>{{ feature }}</span>
              </div>
            </div>

            <!-- 选择按钮 -->
            <el-button 
              type="primary" 
              class="select-btn"
              :class="{ 'selected': selectedPackage?.id === pkg.id }"
              @click.stop="selectPackage(pkg)"
            >
              {{ selectedPackage?.id === pkg.id ? '已选择' : '立即开通' }}
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 购买确认 -->
    <div class="purchase-section" v-if="selectedPackage">
      <div class="purchase-summary">
        <div class="summary-left">
          <h3>已选择：{{ selectedPackage.name }}</h3>
          <p class="package-info">
            <el-icon><Clock /></el-icon>
            <span>有效期：{{ getDurationText(selectedPackage.durationDays) }}</span>
            <span class="divider">|</span>
            <el-icon><Medal /></el-icon>
            <span>{{ getLevelName(selectedPackage.level) }}</span>
          </p>
        </div>
        <div class="summary-right">
          <div class="total-price">
            <span class="label">应付金额：</span>
            <span class="price">¥{{ selectedPackage.currentPrice }}</span>
          </div>
          <el-button 
            type="primary" 
            size="large" 
            @click="showPaymentDialog = true"
            :loading="purchasing"
          >
            立即支付
          </el-button>
        </div>
      </div>
    </div>

    <!-- 支付方式对话框 -->
    <el-dialog
      v-model="showPaymentDialog"
      title="选择支付方式"
      width="500px"
      :close-on-click-modal="false"
      class="vip-payment-dialog"
      :append-to-body="false"
    >
      <div class="payment-dialog">
        <div class="payment-info">
          <div class="info-row">
            <span class="label">套餐：</span>
            <span class="value">{{ selectedPackage?.name }}</span>
          </div>
          <div class="info-row">
            <span class="label">时长：</span>
            <span class="value">{{ selectedPackage?.durationDays }}天</span>
          </div>
          <div class="info-row total">
            <span class="label">应付金额：</span>
            <span class="value price">¥{{ selectedPackage?.currentPrice }}</span>
          </div>
        </div>

        <div class="payment-methods">
          <div 
            class="payment-method"
            :class="{ 'active': paymentMethod === 'ALIPAY' }"
            @click="paymentMethod = 'ALIPAY'"
          >
            <img src="https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*xPQ2RYsKY7QAAAAAAAAAAAAAARQnAQ" alt="支付宝">
            <span>支付宝支付</span>
            <el-icon v-if="paymentMethod === 'ALIPAY'" class="check"><CircleCheck /></el-icon>
          </div>
          <div 
            class="payment-method"
            :class="{ 'active': paymentMethod === 'WECHAT' }"
            @click="paymentMethod = 'WECHAT'"
          >
            <img src="https://res.wx.qq.com/a/wx_fed/assets/res/NTI4MWU5.ico" alt="微信">
            <span>微信支付</span>
            <el-icon v-if="paymentMethod === 'WECHAT'" class="check"><CircleCheck /></el-icon>
          </div>
          <div 
            class="payment-method"
            :class="{ 'active': paymentMethod === 'POINTS' }"
            @click="paymentMethod = 'POINTS'"
            v-if="userInfo && userInfo.points >= selectedPackage?.currentPrice * 100"
          >
            <el-icon :size="32"><StarFilled /></el-icon>
            <span>积分支付</span>
            <span class="points-info">（需{{ selectedPackage?.currentPrice * 100 }}积分）</span>
            <el-icon v-if="paymentMethod === 'POINTS'" class="check"><CircleCheck /></el-icon>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showPaymentDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handlePurchase"
          :loading="purchasing"
          :disabled="!paymentMethod"
        >
          确认支付
        </el-button>
      </template>
    </el-dialog>

    <!-- 加载状态 -->
    <el-skeleton :rows="6" animated v-if="loading" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Medal, Present, ShoppingBag, Check, CircleCheck, StarFilled,
  TrendCharts, ChatLineRound, Calendar, Trophy, Picture, Discount, Clock
} from '@element-plus/icons-vue'
import { getVipPackages, getUserVipInfo, purchaseVip, getMonthlyCouponStatus } from '@/api/vip'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// 数据
const loading = ref(true)
const packages = ref([])
const userVipInfo = ref(null)
const selectedPackage = ref(null)
const showPaymentDialog = ref(false)
const paymentMethod = ref('')
const purchasing = ref(false)

// 月度体验券状态
const monthlyCouponStatus = ref(null)
// 倒计时显示文本
const countdownText = ref('')
let countdownTimer = null

// 启动倒计时
const startCountdown = (secondsUntilNext) => {
  if (countdownTimer) clearInterval(countdownTimer)
  let remaining = secondsUntilNext

  const format = (s) => {
    const d = Math.floor(s / 86400)
    const h = Math.floor((s % 86400) / 3600)
    const m = Math.floor((s % 3600) / 60)
    const sec = s % 60
    if (d > 0) return `${d}天${h}小时${m}分`
    if (h > 0) return `${h}小时${m}分${sec}秒`
    return `${m}分${sec}秒`
  }

  countdownText.value = format(remaining)
  countdownTimer = setInterval(() => {
    remaining = Math.max(0, remaining - 1)
    countdownText.value = format(remaining)
    if (remaining <= 0) clearInterval(countdownTimer)
  }, 1000)
}

// 等级列表
const levelList = [
  { level: 1, name: '见习侦探', emoji: '🔰' },
  { level: 2, name: '银章侦探', emoji: '🥈' },
  { level: 3, name: '金章侦探', emoji: '🥇' },
  { level: 4, name: '传奇侦探', emoji: '👑' }
]

// 权益对比表数据
const compareRows = [
  { key: 'discount',    icon: '💎', label: '预约折扣',   values: { 1: '9.5折', 2: '9折', 3: '8.5折', 4: '8折' } },
  { key: 'monthly',     icon: '🎫', label: '月度体验券', values: { 1: '¥20×1', 2: '¥50×2', 3: '¥100×5', 4: '¥150×10' } },
  { key: 'birthday',    icon: '🎂', label: '生日专享券', values: { 1: '¥30', 2: '¥80', 3: '¥150', 4: '¥200' } },
  { key: 'points',      icon: '🔍', label: '积分加成',   values: { 1: '1.5倍', 2: '2倍', 3: '3倍', 4: '5倍' } },
  { key: 'priority',    icon: '🚀', label: '优先拼单',   values: { 1: false, 2: true, 3: true, 4: true } },
  { key: 'ranking',     icon: '🏆', label: 'VIP专属榜单', values: { 1: false, 2: true, 3: true, 4: true } },
  { key: 'earlybook',   icon: '⚡', label: '提前24h预约', values: { 1: false, 2: false, 3: true, 4: true } },
  { key: 'service',     icon: '🕵️', label: 'VIP专属客服', values: { 1: false, 2: false, 3: true, 4: true } },
  { key: 'badge',       icon: '👑', label: '专属徽章',   values: { 1: false, 2: false, 3: false, 4: true } },
]

// 会员权益 - 剧本杀主题
const benefits = [
  {
    icon: 'TrendCharts',
    emoji: '🔍',
    title: '线索加成',
    description: '消费享受2-5倍积分加成'
  },
  {
    icon: 'Discount',
    emoji: '💎',
    title: '专属折扣',
    description: '预约剧本享受专属折扣：见习9.5折、银章9折、金章8.5折、传奇8折'
  },
  {
    icon: 'Calendar',
    emoji: '🎯',
    title: '优先预约',
    description: '优先预约热门时段和剧本'
  },
  {
    icon: 'ChatLineRound',
    emoji: '🕵️',
    title: '专属助手',
    description: '7x24小时VIP客服支持'
  },
  {
    icon: 'Gift',
    emoji: '🎁',
    title: '每月礼包',
    description: '每月赠送道具券和惊喜礼包'
  },
  {
    icon: 'Trophy',
    emoji: '👑',
    title: '尊贵徽章',
    description: '专属侦探徽章和身份标识'
  },
  {
    icon: 'Picture',
    emoji: '🎂',
    title: '生日特权',
    description: '生日月享受额外惊喜礼包'
  },
  {
    icon: 'Medal',
    emoji: '🎭',
    title: '专属剧本',
    description: '优先体验会员专属剧本'
  }
]

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    const [packagesRes, vipInfoRes, couponStatusRes] = await Promise.all([
      getVipPackages(),
      getUserVipInfo(),
      getMonthlyCouponStatus()
    ])
    
    if (packagesRes.code === 200 || packagesRes.code === 1) {
      packages.value = packagesRes.data || []
    }
    
    if (vipInfoRes.code === 200 || vipInfoRes.code === 1) {
      userVipInfo.value = vipInfoRes.data
    }

    if (couponStatusRes.code === 200 || couponStatusRes.code === 1) {
      monthlyCouponStatus.value = couponStatusRes.data
      if (couponStatusRes.data?.secondsUntilNext > 0) {
        startCountdown(couponStatusRes.data.secondsUntilNext)
      }
    }
  } catch (error) {
    console.error('加载VIP数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

// 选择套餐
const selectPackage = (pkg) => {
  selectedPackage.value = pkg
  // 滚动到底部支付区域
  setTimeout(() => {
    document.querySelector('.purchase-section')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 100)
}

// 处理购买
const handlePurchase = async () => {
  if (!paymentMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }

  try {
    purchasing.value = true
    const res = await purchaseVip(selectedPackage.value.id, paymentMethod.value)
    
    if (res.code === 200 || res.code === 1) {
      ElMessage.success('开通成功！')
      showPaymentDialog.value = false
      // 刷新用户信息
      await userStore.loadUserInfo()
      // 重新加载VIP信息
      await loadData()
      // 清空选择
      selectedPackage.value = null
      paymentMethod.value = ''
    } else {
      ElMessage.error(res.msg || '开通失败')
    }
  } catch (error) {
    console.error('购买失败:', error)
    ElMessage.error(error.message || '购买失败，请重试')
  } finally {
    purchasing.value = false
  }
}

// 获取等级名称 - 剧本杀主题
const getLevelName = (level) => {
  const names = {
    1: '见习侦探',
    2: '银章侦探',
    3: '金章侦探',
    4: '传奇侦探'
  }
  return names[level] || '侦探'
}

// 获取等级emoji
const getLevelEmoji = (level) => {
  const emojis = {
    1: '🔰',
    2: '🥈',
    3: '🥇',
    4: '👑'
  }
  return emojis[level] || '🎭'
}

// 格式化有效期天数
const getDurationText = (days) => {
  if (!days) return ''
  
  if (days === 30) return '1个月'
  if (days === 90) return '3个月'
  if (days === 180) return '6个月'
  if (days === 365) return '1年'
  
  // 其他天数按月/年显示
  if (days >= 365) {
    const years = Math.floor(days / 365)
    const months = Math.floor((days % 365) / 30)
    if (months > 0) {
      return `${years}年${months}个月`
    }
    return `${years}年`
  } else if (days >= 30) {
    const months = Math.floor(days / 30)
    return `${months}个月`
  }
  return `${days}天`
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 获取等级描述 - 剧本杀主题
const getLevelDescription = (level) => {
  const descriptions = {
    1: '初入江湖，崭露头角',
    2: '声名鹊起，屡破奇案',
    3: '名震四方，探案高手',
    4: '传奇神探，无案不破'
  }
  return descriptions[level] || ''
}

// 根据VIP等级获取权益列表 - 剧本杀主题
const getVipBenefits = (level) => {
  const benefitsMap = {
    1: ['🔍 线索翻倍', '🎂 生日特权', '🕵️ 专属助手', '💎 预约9.5折'],
    2: ['🔍 线索2倍', '🎂 生日特权', '🕵️ 专属助手', '🎯 优先预约', '🎫 每月2张券', '💎 预约9折'],
    3: ['🔍 线索3倍', '🎂 生日特权', '🕵️ 专属助手', '🎯 优先预约', '🎫 每月5张券', '💎 预约8.5折'],
    4: ['🔍 线索5倍', '🎂 生日特权', '🕵️ 专属助手', '🎯 优先预约', '🎫 每月10张券', '💎 预约8折', '👑 传奇徽章']
  }
  return benefitsMap[level] || []
}

// 滚动到套餐选择区域
const scrollToPackages = () => {
  const packagesSection = document.querySelector('.packages-section')
  if (packagesSection) {
    packagesSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(() => {
  loadData()
})
</script>


<style scoped lang="scss">
.vip-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px);
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
}

// 神秘背景装饰
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
  font-size: 40px;
  opacity: 0.08;
  will-change: transform;
  animation: float 20s linear infinite;
  transform: translateZ(0);
}

.icon-1 { top: 10%; left: 8%; animation-delay: 0s; }
.icon-2 { top: 35%; right: 10%; animation-delay: 5s; }
.icon-3 { bottom: 30%; left: 15%; animation-delay: 10s; }
.icon-4 { bottom: 15%; right: 20%; animation-delay: 15s; }

@keyframes float {
  0%, 100% { transform: translateY(0) translateZ(0); }
  50% { transform: translateY(-15px) translateZ(0); }
}

// 顶部横幅 - 剧本杀主题
.vip-banner {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.1);

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -10%;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 70%);
    border-radius: 50%;
  }

  .banner-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
    z-index: 1;

    .banner-left {
      display: flex;
      align-items: center;
      gap: 24px;

      .vip-icon {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 20px;
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 1px solid rgba(255, 255, 255, 0.2);
      }

      .icon-emoji {
        font-size: 50px;
      }

      .banner-text {
        color: #fff;

        h1 {
          font-size: 32px;
          font-weight: bold;
          margin: 0 0 8px 0;
          text-shadow: 0 2px 10px rgba(0,0,0,0.3);
          background: linear-gradient(90deg, #FFD700, #FFA500);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }

        p {
          font-size: 16px;
          opacity: 0.9;
          margin: 0;
          color: rgba(255, 255, 255, 0.9);
        }
      }
    }

    .banner-right {
      .current-vip {
        background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
        border: 1px solid rgba(102, 126, 234, 0.2);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-width: 350px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);

        .vip-badge-large {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 16px 24px;
          border-radius: 16px;
          color: #fff;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

          &.level-1 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          }

          &.level-2 {
            background: linear-gradient(135deg, #C0C0C0, #E8E8E8);
            color: #333;
          }

          &.level-3 {
            background: linear-gradient(135deg, #FFD700, #FFA500);
          }

          &.level-4 {
            background: linear-gradient(135deg, #B24FD8, #FF6B9D);
          }

          .badge-icon {
            font-size: 32px;
          }

          .badge-text {
            .level-name {
              font-size: 20px;
              font-weight: bold;
              margin-bottom: 4px;
            }

            .level-desc {
              font-size: 13px;
              opacity: 0.9;
            }
          }
        }

        .vip-expire-card {
          background: linear-gradient(135deg, rgba(40, 40, 70, 0.95) 0%, rgba(30, 30, 60, 0.95) 100%);
          border: 2px solid rgba(102, 126, 234, 0.3);
          border-radius: 12px;
          padding: 16px;

          .expire-item {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 12px;

            .expire-icon {
              font-size: 24px;
              color: #667eea;
            }

            .expire-content {
              .expire-label {
                font-size: 13px;
                color: rgba(255, 255, 255, 0.7);
                margin-bottom: 4px;
              }

              .expire-date {
                font-size: 16px;
                font-weight: bold;
                color: #fff;
              }
            }
          }

          .days-remaining {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 8px 12px;
            background: linear-gradient(135deg, rgba(46, 125, 50, 0.3) 0%, rgba(76, 175, 80, 0.3) 100%);
            border-radius: 8px;
            color: #81c784;
            font-weight: 600;
            font-size: 14px;

            &.warning {
              background: linear-gradient(135deg, rgba(230, 81, 0, 0.3) 0%, rgba(255, 152, 0, 0.3) 100%);
              color: #ffb74d;
            }

            .el-icon {
              font-size: 16px;
            }
          }

          .expired-badge {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 8px 12px;
            background: linear-gradient(135deg, rgba(198, 40, 40, 0.3) 0%, rgba(244, 67, 54, 0.3) 100%);
            border-radius: 8px;
            color: #ef9a9a;
            font-weight: 600;
            font-size: 14px;

            .el-icon {
              font-size: 16px;
            }
          }
        }

        .vip-benefits-mini {
          background: linear-gradient(135deg, rgba(40, 40, 70, 0.95) 0%, rgba(30, 30, 60, 0.95) 100%);
          border: 2px solid rgba(102, 126, 234, 0.3);
          border-radius: 12px;
          padding: 16px;

          .benefits-title {
            font-size: 14px;
            font-weight: bold;
            color: #a0b0ff;
            margin-bottom: 12px;
          }

          .benefits-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;

            .benefit-tag {
              display: flex;
              align-items: center;
              gap: 4px;
              padding: 6px 12px;
              background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
              border-radius: 20px;
              font-size: 13px;
              color: #a0b0ff;
              border: 1px solid rgba(102, 126, 234, 0.3);

              .el-icon {
                font-size: 14px;
              }
            }
          }
        }
      }

      .non-vip {
        background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
        border: 1px solid rgba(102, 126, 234, 0.2);
        backdrop-filter: blur(10px);
        text-align: center;
        padding: 30px 20px;
        border-radius: 20px;
        min-width: 350px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);

        .non-vip-icon {
          font-size: 48px;
          color: rgba(255, 255, 255, 0.5);
          margin-bottom: 16px;

          .el-icon {
            font-size: 48px;
          }
        }

        .main-text {
          margin: 0 0 8px 0;
          font-size: 18px;
          font-weight: bold;
          color: #fff;
        }

        .sub-text {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.7);
          margin: 0 0 20px 0;
        }

        .el-button {
          width: 100%;
        }
      }
    }
  }
}

// 权益展示 - 剧本杀主题
.benefits-section {
  margin-bottom: 60px;
  position: relative;
  z-index: 1;
}

.mystery-section {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 30px;
}

.section-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;

  .title-emoji {
    font-size: 32px;
  }

  .el-icon {
    color: #667eea;
  }
}

.benefits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.benefit-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 16px;
  padding: 30px 24px;
  text-align: center;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  border: 2px solid rgba(102, 126, 234, 0.25);
  will-change: transform;

  &:hover {
    transform: translateY(-5px) translateZ(0);
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
    border-color: rgba(102, 126, 234, 0.5);
  }

  .benefit-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 70px;
    height: 70px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    color: #fff;
    margin-bottom: 20px;
  }

  .benefit-emoji {
    font-size: 36px;
  }

  h3 {
    font-size: 18px;
    font-weight: bold;
    color: #fff;
    margin: 0 0 12px 0;
  }

  p {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.6;
    margin: 0;
  }
}

// 套餐卡片 - 剧本杀主题
.packages-section {
  margin-bottom: 100px;
  position: relative;
  z-index: 1;
}

.packages-row {
  margin-top: 30px;
}

.package-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 20px;
  padding: 32px 24px;
  cursor: pointer;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  border: 3px solid rgba(102, 126, 234, 0.25);
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  will-change: transform;

  &:hover {
    transform: translateY(-8px) translateZ(0);
    box-shadow: 0 15px 40px rgba(102, 126, 234, 0.3);
  }

  &.recommended {
    border-color: #667eea;
    background: linear-gradient(135deg, rgba(40, 40, 80, 0.98) 0%, rgba(30, 30, 70, 0.98) 100%);

    .package-tag {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }

  &.best-value {
    border-color: #FFD700;
    background: linear-gradient(135deg, rgba(50, 45, 30, 0.98) 0%, rgba(40, 35, 25, 0.98) 100%);

    .package-tag {
      background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
      color: #1a1a2e;
    }
  }

  &.selected {
    border-color: #67C23A;
    box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.15);
  }

  .package-tag {
    position: absolute;
    top: 16px;
    right: 16px;
    color: #fff;
    padding: 6px 16px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: bold;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .package-level {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    border-radius: 20px;
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 16px;

    &.level-2 {
      background: linear-gradient(135deg, #C0C0C0, #E8E8E8);
      color: #333;
    }

    &.level-3 {
      background: linear-gradient(135deg, #FFD700, #FFA500);
      color: #fff;
    }

    &.level-4 {
      background: linear-gradient(135deg, #B24FD8, #FF6B9D);
      color: #fff;
    }
  }

  .package-name {
    font-size: 24px;
    font-weight: bold;
    color: #fff;
    margin: 0 0 8px 0;
  }

  .booking-discount-tag {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.2) 0%, rgba(255, 200, 0, 0.15) 100%);
    border: 1px solid rgba(230, 162, 60, 0.5);
    border-radius: 8px;
    padding: 6px 12px;
    font-size: 13px;
    color: #e6a23c;
    margin-bottom: 12px;

    strong {
      font-size: 15px;
      color: #ffd700;
    }

    .discount-icon {
      font-size: 16px;
    }
  }

  .package-duration {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.7);
    margin-bottom: 24px;
  }

  .package-price {
    margin-bottom: 24px;
    position: relative;

    .current-price {
      display: flex;
      align-items: baseline;
      gap: 4px;
      color: #667eea;

      .currency {
        font-size: 24px;
        font-weight: bold;
      }

      .amount {
        font-size: 48px;
        font-weight: bold;
        line-height: 1;
      }
    }

    .original-price {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.5);
      text-decoration: line-through;
      margin-top: 8px;
    }

    .discount-badge {
      position: absolute;
      top: 0;
      right: 0;
      background: #FF4D4F;
      color: #fff;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 14px;
      font-weight: bold;
    }
  }

  .package-duration-info {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 20px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
    border-radius: 10px;
    margin: 20px 0;
    border: 2px solid rgba(102, 126, 234, 0.3);

    .clock-icon {
      font-size: 18px;
      color: #a0b0ff;
    }

    .duration-text {
      font-size: 16px;
      font-weight: 600;
      color: #a0b0ff;
    }
  }

  .package-features {
    flex: 1;
    margin-bottom: 24px;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);

      .check-icon {
        color: #67C23A;
        flex-shrink: 0;
      }
    }
  }

  .select-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: bold;
    border-radius: 12px;

    &.selected {
      background: #67C23A;
      border-color: #67C23A;
    }
  }
}

// 购买区域
.purchase-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-top: 1px solid rgba(102, 126, 234, 0.3);
  box-shadow: 0 -5px 20px rgba(0,0,0,0.3);
  z-index: 100;
  padding: 20px;

  .purchase-summary {
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .summary-left {
      h3 {
        font-size: 20px;
        font-weight: bold;
        color: #fff;
        margin: 0 0 4px 0;
      }

      p {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.7);
        margin: 0;
      }

      .package-info {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 8px;
        font-size: 14px;
        color: rgba(255, 255, 255, 0.8);

        .el-icon {
          font-size: 16px;
          color: #a0b0ff;
        }

        .divider {
          margin: 0 8px;
          color: rgba(255, 255, 255, 0.3);
        }
      }
    }

    .summary-right {
      display: flex;
      align-items: center;
      gap: 24px;

      .total-price {
        text-align: right;

        .label {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.7);
          display: block;
          margin-bottom: 4px;
        }

        .price {
          font-size: 32px;
          font-weight: bold;
          color: #FF4D4F;
        }
      }

      .el-button {
        padding: 12px 48px;
        font-size: 18px;
        height: auto;
      }
    }
  }
}

// 支付对话框弹窗样式 - append-to-body=false 后可用 :deep() 生效
:deep(.vip-payment-dialog) {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%) !important;
  border: 1px solid rgba(102, 126, 234, 0.4) !important;
  border-radius: 20px !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.7) !important;

  .el-dialog__header {
    background: transparent !important;
    border-bottom: 1px solid rgba(102, 126, 234, 0.2);
    padding: 20px 24px;

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: bold;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: rgba(255, 255, 255, 0.6) !important;
      &:hover {
        color: #fff !important;
      }
    }
  }

  .el-dialog__body {
    background: transparent !important;
    padding: 24px;
  }

  .el-dialog__footer {
    background: transparent !important;
    border-top: 1px solid rgba(102, 126, 234, 0.2);
    padding: 16px 24px;

    .el-button--default {
      background: rgba(255, 255, 255, 0.08) !important;
      border-color: rgba(255, 255, 255, 0.2) !important;
      color: rgba(255, 255, 255, 0.8) !important;

      &:hover {
        background: rgba(255, 255, 255, 0.15) !important;
        border-color: rgba(255, 255, 255, 0.4) !important;
        color: #fff !important;
      }
    }
  }
}

// 支付对话框内容
.payment-dialog {
  .payment-info {
    background: linear-gradient(135deg, rgba(40, 40, 70, 0.95) 0%, rgba(30, 30, 60, 0.95) 100%);
    border: 1px solid rgba(102, 126, 234, 0.3);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;

    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }

      &.total {
        padding-top: 12px;
        border-top: 1px dashed rgba(255, 255, 255, 0.2);
        margin-top: 12px;

        .value.price {
          font-size: 24px;
          font-weight: bold;
          color: #FF4D4F;
        }
      }

      .label {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.7);
      }

      .value {
        font-size: 16px;
        color: #fff;
        font-weight: 500;
      }
    }
  }

  .payment-methods {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .payment-method {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border: 2px solid rgba(102, 126, 234, 0.3);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.3s ease;
      position: relative;
      background: linear-gradient(135deg, rgba(40, 40, 70, 0.95) 0%, rgba(30, 30, 60, 0.95) 100%);

      &:hover {
        border-color: #667eea;
        background: linear-gradient(135deg, rgba(50, 50, 90, 0.95) 0%, rgba(40, 40, 80, 0.95) 100%);
      }

      &.active {
        border-color: #667eea;
        background: linear-gradient(135deg, rgba(50, 50, 90, 0.95) 0%, rgba(40, 40, 80, 0.95) 100%);

        .check {
          display: block;
        }
      }

      img {
        width: 32px;
        height: 32px;
        object-fit: contain;
      }

      .el-icon {
        font-size: 32px;
        color: #a0b0ff;
      }

      span {
        font-size: 16px;
        color: #fff;
        font-weight: 500;

        &.points-info {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.6);
          font-weight: normal;
        }
      }

      .check {
        position: absolute;
        right: 16px;
        color: #67C23A;
        font-size: 24px;
        display: none;
      }
    }
  }
}

// 权益对比表
.compare-section { margin-bottom: 30px; }

.compare-table-wrap { overflow-x: auto; }

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;

  th, td {
    padding: 12px 16px;
    text-align: center;
    border-bottom: 1px solid rgba(255,255,255,0.08);
  }

  .feature-col {
    text-align: left;
    color: rgba(255,255,255,0.8);
    white-space: nowrap;
    font-weight: 500;
    min-width: 130px;
  }

  .row-icon { margin-right: 6px; }

  th { background: rgba(255,255,255,0.05); }

  tr:hover td { background: rgba(102,126,234,0.08); }
}

.th-level-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 13px;
  display: inline-block;

  &.level-1 { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  &.level-2 { background: linear-gradient(135deg, #C0C0C0, #E8E8E8); color: #333; }
  &.level-3 { background: linear-gradient(135deg, #FFD700, #FFA500); color: #fff; }
  &.level-4 { background: linear-gradient(135deg, #B24FD8, #FF6B9D); color: #fff; }
}

.check-yes { color: #67c23a; font-size: 18px; font-weight: 700; }
.check-no { color: rgba(255,255,255,0.25); font-size: 18px; }
.value-text { color: #fff; font-weight: 600; }

// 专属券福利
.coupon-perks-section { margin-bottom: 30px; }

// 月度体验券发放状态
.cp-grant-status {
  margin: 10px 0 8px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.grant-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.grant-done { color: #67c23a; font-weight: 600; }
.grant-pending { color: #e6a23c; font-weight: 600; }
.grant-count { color: #fff; font-weight: 700; }

.countdown-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  border-top: 1px dashed rgba(255,255,255,0.15);
  padding-top: 6px;
}
.countdown-label { color: rgba(255,255,255,0.6); }
.countdown-value {
  color: #a0b0ff;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.highlight-card {
  border-color: rgba(103, 194, 58, 0.5) !important;
  box-shadow: 0 0 12px rgba(103, 194, 58, 0.15);
}

.coupon-perks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.coupon-perk-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  gap: 16px;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(102,126,234,0.4);
    background: rgba(102,126,234,0.08);
  }
}

.cp-icon { font-size: 36px; flex-shrink: 0; }

.cp-title {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.cp-desc {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 10px;
}

.cp-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.cp-level {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;

  &.l1 { background: rgba(102,126,234,0.2); color: #a0b0ff; }
  &.l2 { background: rgba(192,192,192,0.2); color: #e0e0e0; }
  &.l3 { background: rgba(255,215,0,0.2); color: #ffd700; }
  &.l4 { background: rgba(178,79,216,0.2); color: #da9fef; }
}

.cp-tip {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
}

// VIP限定入口
.vip-exclusive-section { margin-bottom: 30px; }

.exclusive-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.exclusive-card {
  background: linear-gradient(135deg, rgba(40,40,80,0.95) 0%, rgba(30,30,70,0.95) 100%);
  border: 2px solid rgba(255,215,0,0.25);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;

  &:hover {
    border-color: rgba(255,215,0,0.6);
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(255,215,0,0.15);
  }
}

.ex-icon { font-size: 40px; }
.ex-title { font-size: 16px; font-weight: 700; color: #fff; }
.ex-desc { font-size: 13px; color: rgba(255,255,255,0.6); }

// 响应式
@media (max-width: 768px) {
  .vip-banner {
    padding: 24px;

    .banner-content {
      flex-direction: column;
      gap: 24px;

      .banner-left {
        .vip-icon {
          padding: 16px;

          .el-icon {
            font-size: 40px;
          }
        }

        .banner-text h1 {
          font-size: 24px;
        }
      }

      .banner-right {
        width: 100%;

        .current-vip, .non-vip {
          min-width: auto;
        }
      }
    }
  }

  .benefits-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }

  .purchase-section {
    .purchase-summary {
      flex-direction: column;
      gap: 16px;

      .summary-left, .summary-right {
        width: 100%;
        text-align: center;
      }

      .summary-right {
        flex-direction: column;
        gap: 12px;

        .el-button {
          width: 100%;
        }
      }
    }
  }
}
</style>

