<template>
  <div class="statistics-container">

    <!-- 顶部英雄区 -->
    <div class="hero-section">
      <div class="hero-bg-particles">
        <span v-for="i in 12" :key="i" class="particle" :style="getParticleStyle(i)"></span>
      </div>
      <div class="hero-content">
        <div class="hero-avatar">
          <img :src="userStore.userInfo?.avatar || '/default-avatar.jpg'" alt="avatar" />
          <div class="avatar-ring"></div>
        </div>
        <div class="hero-text">
          <h1>{{ userStore.userInfo?.nickname || '探案者' }} 的推理档案</h1>
          <p>🔍 共参与 <strong>{{ stats.reservationCount }}</strong> 次剧本杀 · 🏆 已完成 <strong>{{ stats.completedCount || 0 }}</strong> 场 · ⭐ 好评率 <strong>{{ completionRate }}%</strong></p>
        </div>
        <div class="hero-level">
          <div class="level-badge">
            <span class="level-icon">{{ playerLevel.icon }}</span>
            <span class="level-name">{{ playerLevel.name }}</span>
          </div>
          <div class="level-progress-wrap">
            <el-progress :percentage="playerLevel.progress" :show-text="false" :stroke-width="6"
              style="width: 120px;" color="linear-gradient(90deg,#667eea,#f093fb)" />
            <span class="level-tip">距下一级 {{ playerLevel.remaining }} 场</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 六宫格核心数据 -->
    <div class="stats-grid">
      <div class="stat-tile" v-for="tile in statTiles" :key="tile.key"
        :style="{ '--tile-color': tile.color, '--tile-glow': tile.glow }">
        <div class="tile-icon">{{ tile.emoji }}</div>
        <div class="tile-value">{{ tile.value }}</div>
        <div class="tile-label">{{ tile.label }}</div>
        <div class="tile-sub">{{ tile.sub }}</div>
        <div class="tile-shine"></div>
      </div>
    </div>

    <!-- 图表 + 分布 两列布局 -->
    <div class="chart-row">
      <!-- 趋势图 -->
      <div class="dark-card chart-card">
        <div class="dark-card-header">
          <span class="card-title">📈 预约趋势</span>
          <div class="period-tabs">
            <button v-for="p in periods" :key="p.value"
              :class="['period-btn', { active: chartPeriod === p.value }]"
              @click="chartPeriod = p.value">{{ p.label }}</button>
          </div>
        </div>
        <div ref="chartRef" class="chart-container"></div>
      </div>

      <!-- 状态分布饼图 -->
      <div class="dark-card pie-card">
        <div class="dark-card-header">
          <span class="card-title">🎯 预约状态分布</span>
        </div>
        <div ref="pieRef" class="pie-container"></div>
        <div class="pie-legend">
          <div class="legend-item" v-for="item in pieData" :key="item.name">
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <span class="legend-name">{{ item.name }}</span>
            <span class="legend-val">{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 月度明细表 -->
    <div class="dark-card table-card">
      <div class="dark-card-header">
        <span class="card-title">📋 月度明细</span>
        <span class="card-sub">最近 {{ reservationStats.length }} 个月数据</span>
      </div>
      <div class="custom-table">
        <div class="table-head">
          <span>月份</span>
          <span>预约次数</span>
          <span>已完成</span>
          <span>已取消</span>
          <span>消费金额</span>
          <span>完成率</span>
        </div>
        <div class="table-body">
          <div class="table-row" v-for="row in reservationStats" :key="row.month"
            :class="{ 'has-data': row.count > 0 }">
            <span class="month-col">{{ row.month }}</span>
            <span><span class="badge badge-blue">{{ row.count }}</span></span>
            <span><span class="badge badge-green">{{ row.completed }}</span></span>
            <span><span class="badge badge-gray">{{ row.cancelled }}</span></span>
            <span class="amount-col">¥{{ Number(row.amount).toFixed(2) }}</span>
            <span>
              <div class="mini-progress">
                <div class="mini-bar" :style="{ width: row.count > 0 ? (row.completed/row.count*100)+'%' : '0%' }"></div>
              </div>
              <span class="mini-pct">{{ row.count > 0 ? Math.round(row.completed/row.count*100) : 0 }}%</span>
            </span>
          </div>
          <div class="table-empty" v-if="reservationStats.length === 0">暂无数据</div>
        </div>
      </div>
    </div>

    <!-- 成就墙 -->
    <div class="dark-card achievements-card">
      <div class="dark-card-header">
        <span class="card-title">🏆 成就解锁</span>
        <span class="achievement-progress-text">{{ unlockedAchievements }}/{{ achievements.length }} 已解锁</span>
      </div>
      <div class="achievements-grid">
        <div class="achievement-tile" v-for="a in achievements" :key="a.id"
          :class="{ unlocked: a.unlocked }">
          <div class="achievement-glow" v-if="a.unlocked"></div>
          <div class="achievement-emoji">{{ a.emoji }}</div>
          <div class="achievement-name">{{ a.name }}</div>
          <div class="achievement-desc">{{ a.desc }}</div>
          <div class="achievement-bar">
            <div class="achievement-fill" :style="{ width: a.progress + '%',
              background: a.unlocked ? 'linear-gradient(90deg,#67c23a,#95d475)' : 'linear-gradient(90deg,#667eea,#764ba2)' }">
            </div>
          </div>
          <div class="achievement-pct">{{ a.progress }}%</div>
          <div class="lock-icon" v-if="!a.unlocked">🔒</div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useUserStore } from '@/store/user'
import { getMyReservations } from '@/api/reservation'
import { getFavoriteScripts } from '@/api/script'
import { getMyCoupons } from '@/api/coupon'
import { getUserPoints, getPointsRecords } from '@/api/user'

let echartsLib = null
const loadEcharts = async () => {
  if (echartsLib) return echartsLib
  const mod = await import('echarts')
  echartsLib = mod?.default ?? mod
  return echartsLib
}

const userStore = useUserStore()
const chartRef = ref(null)
const pieRef = ref(null)
const chartPeriod = ref('year')
let chartInstance = null
let pieInstance = null

const periods = [
  { label: '近3月', value: 'quarter' },
  { label: '近半年', value: 'half' },
  { label: '近一年', value: 'year' },
]

const stats = reactive({
  reservationCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  favoriteCount: 0,
  points: 0,
  couponCount: 0,
  totalSpent: 0,
  totalEarned: 0,
  reviewCount: 0
})

const reservationStats = ref([])

// 完成率
const completionRate = computed(() => {
  if (!stats.reservationCount) return 0
  return Math.round((stats.completedCount / stats.reservationCount) * 100)
})

// 玩家等级
const playerLevel = computed(() => {
  const c = stats.completedCount || 0
  const levels = [
    { min: 0,  max: 1,  icon: '🌱', name: '新手探案者', next: 1 },
    { min: 1,  max: 5,  icon: '🔍', name: '初级侦探',   next: 5 },
    { min: 5,  max: 15, icon: '🕵️', name: '资深推理师', next: 15 },
    { min: 15, max: 30, icon: '🎭', name: '剧本达人',   next: 30 },
    { min: 30, max: 50, icon: '🏆', name: '推理王者',   next: 50 },
    { min: 50, max: 999,icon: '👑', name: '传说侦探',   next: 999 },
  ]
  const lv = levels.find(l => c >= l.min && c < l.max) || levels[levels.length - 1]
  const progress = lv.next === 999 ? 100 : Math.round(((c - lv.min) / (lv.next - lv.min)) * 100)
  return { ...lv, progress, remaining: Math.max(0, lv.next - c) }
})

// 六宫格数据
const statTiles = computed(() => [
  { key: 'reservation', emoji: '🎭', label: '总预约', value: stats.reservationCount, sub: `已完成 ${stats.completedCount}`, color: '#667eea', glow: 'rgba(102,126,234,0.3)' },
  { key: 'favorite',    emoji: '⭐', label: '剧本收藏', value: stats.favoriteCount, sub: stats.favoriteCount > 0 ? '继续探索' : '去收藏吧', color: '#f6d365', glow: 'rgba(246,211,101,0.3)' },
  { key: 'points',      emoji: '💎', label: '当前积分', value: stats.points, sub: `累计 ${stats.totalEarned}`, color: '#a18cd1', glow: 'rgba(161,140,209,0.3)' },
  { key: 'coupon',      emoji: '🎫', label: '可用优惠券', value: stats.couponCount, sub: stats.couponCount > 0 ? '快去使用' : '暂无可用', color: '#f093fb', glow: 'rgba(240,147,251,0.3)' },
  { key: 'spent',       emoji: '💰', label: '累计消费', value: formatMoney(stats.totalSpent), sub: `${stats.reservationCount} 次预约`, color: '#f56c6c', glow: 'rgba(245,108,108,0.3)' },
  { key: 'review',      emoji: '✍️', label: '我的评价', value: stats.reviewCount, sub: stats.reviewCount > 0 ? '感谢分享' : '快去评价', color: '#43e97b', glow: 'rgba(67,233,123,0.3)' },
])

// 饼图数据
const pieData = computed(() => [
  { name: '已完成', value: stats.completedCount, color: '#67c23a' },
  { name: '已取消', value: stats.cancelledCount, color: '#909399' },
  { name: '待处理', value: Math.max(0, stats.reservationCount - stats.completedCount - stats.cancelledCount), color: '#667eea' },
])

const achievements = ref([
  { id: 1, name: '初次体验', desc: '完成首次预约', emoji: '🎯', unlocked: false, progress: 0 },
  { id: 2, name: '剧本达人', desc: '体验10个剧本', emoji: '📚', unlocked: false, progress: 0 },
  { id: 3, name: '老玩家',   desc: '完成15场游戏', emoji: '🎮', unlocked: false, progress: 0 },
  { id: 4, name: '评论家',   desc: '发表20条评价', emoji: '✍️', unlocked: false, progress: 0 },
  { id: 5, name: '收藏家',   desc: '收藏50个剧本', emoji: '⭐', unlocked: false, progress: 0 },
  { id: 6, name: '积分大户', desc: '累计获得1000积分', emoji: '💎', unlocked: false, progress: 0 },
])

const unlockedAchievements = computed(() => achievements.value.filter(a => a.unlocked).length)

// 粒子样式
const getParticleStyle = (i) => {
  const size = Math.random() * 4 + 2
  return {
    width: size + 'px', height: size + 'px',
    left: (i * 8.5) + '%',
    top: (Math.sin(i) * 30 + 50) + '%',
    animationDelay: (i * 0.3) + 's',
    animationDuration: (Math.random() * 3 + 3) + 's',
  }
}

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

// 格式化金额
const formatMoney = (amount) => {
  if (amount === undefined || amount === null) return '¥0.00'
  if (amount >= 10000) {
    return '¥' + (amount / 10000).toFixed(2) + 'w'
  }
  if (amount >= 1000) {
    return '¥' + (amount / 1000).toFixed(1) + 'k'
  }
  return '¥' + amount.toFixed(2)
}

// 根据时间段过滤数据
const getFilteredStats = () => {
  let months = 12
  if (chartPeriod.value === 'quarter') months = 3
  if (chartPeriod.value === 'half') months = 6
  if (chartPeriod.value === 'year') months = 12
  const cutoffDate = new Date()
  cutoffDate.setMonth(cutoffDate.getMonth() - months)
  return reservationStats.value.filter(stat => {
    const statDate = new Date(stat.month + '-01')
    return statDate >= cutoffDate
  })
}

// 加载统计数据
const loadStats = async () => {
  try {
    const [reservationRes, favoriteRes, couponRes] = await Promise.allSettled([
      getMyReservations({ page: 1, pageSize: 100 }),
      getFavoriteScripts({ page: 1, pageSize: 1 }),
      getMyCoupons({ status: 1, page: 1, pageSize: 1000 }),
    ])

    if (reservationRes.status === 'fulfilled') {
      const res = reservationRes.value
      if (res.code === 1 || res.code === 200) {
        stats.reservationCount = res.data.total || 0
        const all = res.data.records || []
        stats.completedCount = all.filter(r => r.status === 3).length
        stats.cancelledCount = all.filter(r => r.status === 4).length
        stats.totalSpent = all.filter(r => r.payStatus === 1)
          .reduce((sum, r) => sum + (parseFloat(r.actualAmount) || 0), 0)
      }
    }

    if (favoriteRes.status === 'fulfilled') {
      const res = favoriteRes.value
      if (res.code === 1 || res.code === 200) stats.favoriteCount = res.data.total || 0
    }

    if (couponRes.status === 'fulfilled') {
      const res = couponRes.value
      if (res.code === 1 || res.code === 200) stats.couponCount = res.data?.records?.length || 0
    }

    try {
      const pointsRes = await getUserPoints()
      if (pointsRes.code === 1 || pointsRes.code === 200) {
        stats.points = pointsRes.data.currentPoints || 0
        stats.totalEarned = pointsRes.data.totalEarned || 0
      }
    } catch {
      stats.points = userStore.userInfo?.points || 0
    }

    try {
      const pointsRecords = await getPointsRecords({ page: 1, pageSize: 100, type: 1 })
      if (pointsRecords.code === 1 || pointsRecords.code === 200) {
        const records = pointsRecords.data.records || []
        stats.reviewCount = records.filter(r => r.description?.includes('评价')).length
      }
    } catch {}

    calculateAchievements()
    await loadMonthlyStats()
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载月度统计
const loadMonthlyStats = async () => {
  try {
    // 获取所有预约数据
    const reservationRes = await getMyReservations({ page: 1, pageSize: 100 })
    if (reservationRes.code === 1 || reservationRes.code === 200) {
      const allReservations = reservationRes.data.records || []
      
      // 按月份分组统计
      const monthlyMap = {}
      allReservations.forEach(reservation => {
        if (!reservation.createTime) return
        
        // 提取年月
        const month = reservation.createTime.substring(0, 7) // "2024-11"
        
        if (!monthlyMap[month]) {
          monthlyMap[month] = {
            month,
            count: 0,
            completed: 0,
            cancelled: 0,
            amount: 0
          }
        }
        
        monthlyMap[month].count++
        
        if (reservation.status === 3) {
          monthlyMap[month].completed++
        } else if (reservation.status === 4) {
          monthlyMap[month].cancelled++
        }
        
        if (reservation.payStatus === 1 && reservation.actualAmount) {
          monthlyMap[month].amount += parseFloat(reservation.actualAmount) || 0
        }
      })
      
      // 转换为数组并排序
      let monthlyData = Object.values(monthlyMap)
        .sort((a, b) => b.month.localeCompare(a.month))
        .slice(0, 12) // 保留最近12个月数据
        .reverse() // 时间从早到晚
      
      // 如果数据不足12个月，补充空白月份
      if (monthlyData.length < 12) {
        const now = new Date()
        const filledData = []
        
        for (let i = 11; i >= 0; i--) {
          const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
          const monthStr = date.toISOString().substring(0, 7)
          
          const existingData = monthlyData.find(d => d.month === monthStr)
          if (existingData) {
            filledData.push(existingData)
          } else {
            filledData.push({
              month: monthStr,
              count: 0,
              completed: 0,
              cancelled: 0,
              amount: 0
            })
          }
        }
        
        monthlyData = filledData
      }
      
      reservationStats.value = monthlyData
    }
    
    // 初始化图表
    await nextTick()
    await Promise.all([initChart(), initPieChart()])
  } catch (error) {
    console.error('加载月度统计失败:', error)
  }
}

// 初始化趋势图
const initChart = async () => {
  if (!chartRef.value) return
  const echarts = await loadEcharts()
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const fd = getFilteredStats()
  chartInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,20,50,0.95)',
      borderColor: 'rgba(102,126,234,0.4)',
      borderWidth: 1,
      textStyle: { color: 'rgba(255,255,255,0.9)', fontSize: 13 },
      axisPointer: { type: 'cross', label: { backgroundColor: '#667eea' } }
    },
    legend: {
      data: ['预约次数', '已完成', '已取消'],
      bottom: 0, icon: 'circle',
      textStyle: { color: 'rgba(255,255,255,0.6)', fontSize: 12 }
    },
    grid: { left: '2%', right: '2%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: fd.map(s => s.month.slice(5) + '月'),
      axisLine: { lineStyle: { color: 'rgba(102,126,234,0.2)' } },
      axisLabel: { color: 'rgba(255,255,255,0.45)', fontSize: 12 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(102,126,234,0.2)' } },
      axisLabel: { color: 'rgba(255,255,255,0.45)' },
      splitLine: { lineStyle: { type: 'dashed', color: 'rgba(102,126,234,0.08)' } }
    },
    series: [
      {
        name: '预约次数', type: 'line', data: fd.map(s => s.count),
        smooth: true, symbolSize: 7,
        itemStyle: { color: '#667eea' },
        lineStyle: { width: 3, color: '#667eea' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(102,126,234,0.35)' }, { offset: 1, color: 'rgba(102,126,234,0.02)' }] } }
      },
      {
        name: '已完成', type: 'line', data: fd.map(s => s.completed),
        smooth: true, symbolSize: 7,
        itemStyle: { color: '#43e97b' },
        lineStyle: { width: 2, color: '#43e97b' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(67,233,123,0.2)' }, { offset: 1, color: 'rgba(67,233,123,0.01)' }] } }
      },
      {
        name: '已取消', type: 'bar', data: fd.map(s => s.cancelled),
        itemStyle: { color: 'rgba(144,147,153,0.4)', borderRadius: [4, 4, 0, 0] },
        barWidth: '30%'
      }
    ]
  })
}

// 初始化饼图
const initPieChart = async () => {
  if (!pieRef.value) return
  const echarts = await loadEcharts()
  if (!pieInstance) pieInstance = echarts.init(pieRef.value)
  const total = stats.reservationCount || 1
  pieInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15,20,50,0.95)',
      borderColor: 'rgba(102,126,234,0.4)',
      textStyle: { color: '#fff' },
      formatter: '{b}: {c} ({d}%)'
    },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: 'rgba(15,20,50,0.6)', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' } },
      data: [
        { name: '已完成', value: stats.completedCount, itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 1, colorStops: [{ offset: 0, color: '#43e97b' }, { offset: 1, color: '#38f9d7' }] } } },
        { name: '已取消', value: stats.cancelledCount, itemStyle: { color: '#606266' } },
        { name: '待处理', value: Math.max(0, total - stats.completedCount - stats.cancelledCount), itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 1, colorStops: [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }] } } },
      ]
    }]
  })
}

// 计算成就进度
const calculateAchievements = () => {
  const c = stats.completedCount || 0
  achievements.value[0].progress = c > 0 ? 100 : (stats.reservationCount > 0 ? 50 : 0)
  achievements.value[0].unlocked = c > 0
  achievements.value[1].progress = Math.min(Math.round((c / 10) * 100), 100)
  achievements.value[1].unlocked = c >= 10
  achievements.value[2].progress = Math.min(Math.round((c / 15) * 100), 100)
  achievements.value[2].unlocked = c >= 15
  achievements.value[3].progress = Math.min(Math.round((stats.reviewCount / 20) * 100), 100)
  achievements.value[3].unlocked = stats.reviewCount >= 20
  achievements.value[4].progress = Math.min(Math.round((stats.favoriteCount / 50) * 100), 100)
  achievements.value[4].unlocked = stats.favoriteCount >= 50
  const earned = stats.totalEarned || stats.points
  achievements.value[5].progress = Math.min(Math.round((earned / 1000) * 100), 100)
  achievements.value[5].unlocked = earned >= 1000
}

watch(chartPeriod, async () => { await initChart() })

let resizeHandler = null
onMounted(() => {
  loadStats()
  resizeHandler = () => {
    chartInstance?.resize()
    pieInstance?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})
onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  chartInstance?.dispose()
  pieInstance?.dispose()
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: transparent;
}

/* ===== 英雄区 ===== */
.hero-section {
  position: relative;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  border-radius: 20px;
  padding: 32px 36px;
  margin-bottom: 24px;
  overflow: hidden;
  border: 1px solid rgba(102,126,234,0.25);
}

.hero-bg-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(102,126,234,0.6);
  animation: float linear infinite;
}

@keyframes float {
  0%   { transform: translateY(0) scale(1); opacity: 0.7; }
  50%  { transform: translateY(-20px) scale(1.2); opacity: 0.3; }
  100% { transform: translateY(0) scale(1); opacity: 0.7; }
}

.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.hero-avatar {
  position: relative;
  flex-shrink: 0;
}

.hero-avatar img {
  width: 80px; height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(102,126,234,0.6);
  display: block;
}

.avatar-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: linear-gradient(135deg,#667eea,#f093fb) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: destination-out;
  mask-composite: exclude;
  animation: spin 4s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.hero-text { flex: 1; min-width: 200px; }
.hero-text h1 {
  font-size: 22px; font-weight: 700;
  color: #fff; margin: 0 0 8px;
  text-shadow: 0 0 20px rgba(102,126,234,0.5);
}
.hero-text p { color: rgba(255,255,255,0.6); margin: 0; font-size: 14px; line-height: 1.8; }
.hero-text strong { color: #f093fb; font-size: 18px; }

.hero-level { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.level-badge {
  display: flex; align-items: center; gap: 8px;
  background: rgba(102,126,234,0.15);
  border: 1px solid rgba(102,126,234,0.3);
  border-radius: 20px; padding: 6px 16px;
}
.level-icon { font-size: 20px; }
.level-name { color: #fff; font-weight: 600; font-size: 14px; }
.level-progress-wrap { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.level-tip { font-size: 11px; color: rgba(255,255,255,0.45); }

/* ===== 六宫格 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 1200px) { .stats-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  { .stats-grid { grid-template-columns: repeat(2, 1fr); } }

.stat-tile {
  position: relative;
  background: linear-gradient(135deg, rgba(26,26,46,0.98), rgba(22,33,62,0.98));
  border: 1px solid rgba(var(--tile-color, 102,126,234), 0.2);
  border-radius: 16px;
  padding: 20px 16px;
  text-align: center;
  overflow: hidden;
  cursor: default;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

.stat-tile:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0,0,0,0.5), 0 0 20px var(--tile-glow);
  border-color: var(--tile-color);
}

.stat-tile::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; height: 3px;
  background: var(--tile-color);
  opacity: 0.8;
}

.tile-shine {
  position: absolute;
  top: -50%; left: -50%;
  width: 200%; height: 200%;
  background: radial-gradient(circle at 50% 50%, rgba(255,255,255,0.03), transparent 60%);
  pointer-events: none;
}

.tile-icon { font-size: 32px; margin-bottom: 8px; filter: drop-shadow(0 0 8px var(--tile-glow)); }
.tile-value { font-size: 24px; font-weight: 700; color: #fff; line-height: 1.2; margin-bottom: 4px; }
.tile-label { font-size: 12px; color: rgba(255,255,255,0.5); letter-spacing: 1px; margin-bottom: 4px; }
.tile-sub { font-size: 11px; color: rgba(255,255,255,0.3); }

/* ===== 图表行 ===== */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
  margin-bottom: 24px;
}
@media (max-width: 1024px) { .chart-row { grid-template-columns: 1fr; } }

/* ===== 通用深色卡片 ===== */
.dark-card {
  background: linear-gradient(135deg, rgba(26,26,46,0.98), rgba(22,33,62,0.98));
  border: 1px solid rgba(102,126,234,0.2);
  border-radius: 16px;
  overflow: hidden;
}

.dark-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(102,126,234,0.15);
}

.card-title { font-size: 16px; font-weight: 600; color: #fff; }
.card-sub { font-size: 12px; color: rgba(255,255,255,0.4); }

/* 趋势图 */
.chart-card { padding-bottom: 8px; }
.chart-container { height: 320px; width: 100%; padding: 12px 8px 0; box-sizing: border-box; }

/* 时间段按钮 */
.period-tabs { display: flex; gap: 4px; }
.period-btn {
  padding: 4px 12px; border-radius: 20px; font-size: 12px;
  border: 1px solid rgba(102,126,234,0.3);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.6);
  cursor: pointer; transition: all 0.2s;
}
.period-btn:hover { background: rgba(102,126,234,0.2); color: #fff; }
.period-btn.active {
  background: linear-gradient(135deg,#667eea,#764ba2);
  border-color: #667eea; color: #fff;
}

/* 饼图 */
.pie-card { display: flex; flex-direction: column; }
.pie-container { height: 200px; flex-shrink: 0; }
.pie-legend { padding: 8px 20px 20px; display: flex; flex-direction: column; gap: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-name { flex: 1; font-size: 13px; color: rgba(255,255,255,0.7); }
.legend-val { font-size: 14px; font-weight: 600; color: #fff; }

/* ===== 表格 ===== */
.table-card { margin-bottom: 24px; }
.custom-table { overflow-x: auto; }
.table-head, .table-row {
  display: grid;
  grid-template-columns: 100px repeat(5, 1fr);
  padding: 12px 20px;
  align-items: center;
  gap: 8px;
}
.table-head {
  background: rgba(102,126,234,0.08);
  font-size: 12px; color: rgba(255,255,255,0.5);
  font-weight: 600; letter-spacing: 1px;
  border-bottom: 1px solid rgba(102,126,234,0.1);
}
.table-row {
  border-bottom: 1px solid rgba(102,126,234,0.06);
  font-size: 13px; color: rgba(255,255,255,0.75);
  transition: background 0.2s;
}
.table-row:hover { background: rgba(102,126,234,0.06); }
.table-row.has-data { color: rgba(255,255,255,0.9); }
.table-row:last-child { border-bottom: none; }
.table-empty { padding: 40px; text-align: center; color: rgba(255,255,255,0.3); font-size: 14px; }
.month-col { color: rgba(255,255,255,0.5); font-size: 12px; }
.amount-col { color: #f56c6c; font-weight: 600; }

.badge {
  display: inline-block; padding: 2px 10px;
  border-radius: 10px; font-size: 12px; font-weight: 600;
}
.badge-blue  { background: rgba(102,126,234,0.2); color: #a5b4fc; border: 1px solid rgba(102,126,234,0.3); }
.badge-green { background: rgba(67,233,123,0.15); color: #6ee7b7; border: 1px solid rgba(67,233,123,0.3); }
.badge-gray  { background: rgba(144,147,153,0.15); color: #9ca3af; border: 1px solid rgba(144,147,153,0.3); }

.mini-progress {
  height: 4px; background: rgba(255,255,255,0.08);
  border-radius: 2px; overflow: hidden; margin-bottom: 3px;
}
.mini-bar {
  height: 100%; border-radius: 2px;
  background: linear-gradient(90deg,#667eea,#43e97b);
  transition: width 0.6s ease;
}
.mini-pct { font-size: 11px; color: rgba(255,255,255,0.4); }

/* ===== 成就墙 ===== */
.achievements-card { margin-bottom: 24px; }
.achievement-progress-text { font-size: 13px; color: rgba(255,255,255,0.5); }

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  padding: 20px;
}
@media (max-width: 1200px) { .achievements-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  { .achievements-grid { grid-template-columns: repeat(2, 1fr); } }

.achievement-tile {
  position: relative;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(102,126,234,0.12);
  border-radius: 14px; padding: 20px 12px;
  text-align: center; transition: all 0.3s;
  opacity: 0.5; overflow: hidden;
}
.achievement-tile:hover { transform: translateY(-4px); opacity: 0.8; }
.achievement-tile.unlocked {
  opacity: 1;
  border-color: rgba(67,233,123,0.4);
  background: linear-gradient(135deg, rgba(67,233,123,0.08), rgba(26,26,46,0.98));
  box-shadow: 0 0 20px rgba(67,233,123,0.1);
}
.achievement-tile.unlocked:hover { box-shadow: 0 8px 30px rgba(67,233,123,0.2); }

.achievement-glow {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 50% 0%, rgba(67,233,123,0.15), transparent 70%);
  pointer-events: none;
}

.achievement-emoji { font-size: 36px; margin-bottom: 10px; filter: grayscale(1); }
.achievement-tile.unlocked .achievement-emoji { filter: none; filter: drop-shadow(0 0 8px rgba(67,233,123,0.5)); }

.achievement-name { font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.7); margin-bottom: 4px; }
.achievement-tile.unlocked .achievement-name { color: #fff; }
.achievement-desc { font-size: 11px; color: rgba(255,255,255,0.35); margin-bottom: 12px; line-height: 1.5; }

.achievement-bar {
  height: 4px; background: rgba(255,255,255,0.08);
  border-radius: 2px; overflow: hidden; margin-bottom: 4px;
}
.achievement-fill { height: 100%; border-radius: 2px; transition: width 0.8s ease; }
.achievement-pct { font-size: 11px; color: rgba(255,255,255,0.35); }

.lock-icon {
  position: absolute; top: 8px; right: 10px;
  font-size: 12px; opacity: 0.4;
}

@media (max-width: 768px) {
  .statistics-container { padding: 12px; }
  .hero-section { padding: 20px; }
  .hero-text h1 { font-size: 18px; }
  .chart-container { height: 240px; }
}

</style>



