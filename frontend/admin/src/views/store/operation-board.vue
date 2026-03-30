<template>
  <div class="operation-board">

    <!-- 顶部筛选栏 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <div class="board-title">
          <span class="title-icon">📊</span>
          <span>经营看板</span>
        </div>
        <div class="filter-controls">
          <el-select v-if="!isStoreAdmin" v-model="selectedStoreId" placeholder="全部门店"
            clearable style="width:180px" @change="loadData">
            <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="days" style="width:120px" @change="loadData">
            <el-option label="近7天"  :value="7"  />
            <el-option label="近30天" :value="30" />
            <el-option label="近60天" :value="60" />
            <el-option label="近90天" :value="90" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="loadData">
            <el-icon><Refresh /></el-icon>&nbsp;刷新
          </el-button>
          <span class="update-time" v-if="generatedAt">更新于 {{ formatTime(generatedAt) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 核心指标卡片 -->
    <div class="metric-cards" v-loading="loading">
      <div class="metric-card" v-for="m in metricCards" :key="m.key"
        :style="{ borderTopColor: m.color }">
        <div class="mc-header">
          <span class="mc-icon">{{ m.icon }}</span>
          <span class="mc-label">{{ m.label }}</span>
        </div>
        <div class="mc-value" :style="{ color: m.color }">{{ m.value }}</div>
        <div class="mc-sub">{{ m.sub }}</div>
        <el-progress
          v-if="m.progress !== undefined"
          :percentage="Math.min(Number(m.progress) || 0, 100)"
          :color="m.color"
          :stroke-width="5"
          :show-text="false"
          style="margin-top:8px"
        />
      </div>
    </div>

    <!-- 中部：趋势图 + 热度榜 -->
    <div class="chart-row">
      <el-card class="trend-card">
        <template #header>
          <div class="chart-header">
            <span>📈 预约趋势（近{{ days }}天）</span>
            <el-radio-group v-model="trendType" size="small" @change="renderTrendChart">
              <el-radio-button value="count">预约量</el-radio-button>
              <el-radio-button value="revenue">营收</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-box"></div>
      </el-card>

      <el-card class="rank-card">
        <template #header><span>🏆 剧本热度榜 TOP8</span></template>
        <div v-if="data.scriptHotRank?.length" class="script-rank-list">
          <div v-for="(item, idx) in data.scriptHotRank" :key="idx" class="rank-item">
            <div class="rank-num" :class="['top1','top2','top3'][idx] || 'topN'">{{ idx + 1 }}</div>
            <div class="rank-name" :title="item.name">{{ item.name || '未知剧本' }}</div>
            <div class="rank-bar-wrap">
              <div class="rank-bar" :style="{ width: getRankBarWidth(item.cnt) + '%', background: rankBarColor(idx) }"></div>
            </div>
            <div class="rank-cnt">{{ item.cnt }}次</div>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </el-card>
    </div>

    <!-- 下部：漏斗 + 利用率 + 用户分层 -->
    <div class="bottom-row">

      <!-- 转化漏斗 -->
      <el-card class="bottom-card">
        <template #header><span>🔽 预约转化漏斗</span></template>
        <div class="funnel-list">
          <div class="funnel-item" v-for="f in funnelItems" :key="f.label">
            <div class="fi-label">{{ f.icon }} {{ f.label }}</div>
            <div class="fi-bar-wrap">
              <div class="fi-bar" :class="f.cls" :style="{ width: Math.max(f.pct, 4) + '%' }">
                <span class="fi-num" v-if="f.num > 0">{{ f.num }}</span>
              </div>
            </div>
            <div class="fi-rate">{{ f.rate }}</div>
          </div>
        </div>
        <!-- 漏斗分析提示 -->
        <div class="funnel-tip" v-if="data.totalReservations > 0">
          <el-tag size="small" :type="conversionOk ? 'success' : 'warning'">
            {{ conversionOk ? '✅ 转化良好' : '⚠️ 转化待优化' }}
          </el-tag>
          <span class="tip-text">完成率 {{ completionRate }}%</span>
        </div>
      </el-card>

      <!-- 房间利用率 -->
      <el-card class="bottom-card">
        <template #header><span>🚪 房间利用率</span></template>
        <div class="util-center">
          <el-progress
            type="dashboard"
            :percentage="data.roomUtilizationRate || 0"
            :color="getUtilColor(data.roomUtilizationRate)"
            :stroke-width="14"
            :width="150"
          >
            <template #default="{ percentage }">
              <div class="util-label">
                <div class="util-pct">{{ percentage }}%</div>
                <div class="util-sub">利用率</div>
              </div>
            </template>
          </el-progress>
          <div class="util-stats">
            <div class="us-item">
              <span class="us-num">{{ data.scheduleTotal || 0 }}</span>
              <span class="us-label">总排期</span>
            </div>
            <div class="us-item">
              <span class="us-num" style="color:#67c23a">{{ data.scheduleUsed || 0 }}</span>
              <span class="us-label">有预约</span>
            </div>
            <div class="us-item">
              <span class="us-num" style="color:#909399">{{ (data.scheduleTotal||0) - (data.scheduleUsed||0) }}</span>
              <span class="us-label">空场次</span>
            </div>
          </div>
          <div class="util-tip" :class="getUtilTipClass(data.roomUtilizationRate)">
            {{ getUtilTip(data.roomUtilizationRate) }}
          </div>
        </div>
      </el-card>

      <!-- 用户分层 -->
      <el-card class="bottom-card">
        <template #header><span>👥 用户分层分析</span></template>
        <div ref="userChartRef" class="user-chart-box"></div>
        <div class="user-stats">
          <div class="user-stat-item">
            <span class="usi-dot" style="background:#409eff"></span>
            <span>新客户</span>
            <strong>{{ data.newCustomers || 0 }} 人</strong>
          </div>
          <div class="user-stat-item">
            <span class="usi-dot" style="background:#67c23a"></span>
            <span>回头客</span>
            <strong>{{ data.returningCustomers || 0 }} 人</strong>
          </div>
          <div class="user-stat-item">
            <span class="usi-dot" style="background:#9b59b6"></span>
            <span>复购率</span>
            <strong style="color:#9b59b6">{{ data.repurchaseRate || 0 }}%</strong>
          </div>
        </div>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const loginType      = localStorage.getItem('admin-login-type') || 'admin'
const isStoreAdmin   = loginType === 'store'
const fixedStoreId   = localStorage.getItem('admin-store-id')
const storeList      = ref([])
const selectedStoreId = ref(fixedStoreId ? Number(fixedStoreId) : null)

const days        = ref(30)
const loading     = ref(false)
const data        = ref({})
const generatedAt = ref('')
const trendType   = ref('count')

const trendChartRef = ref(null)
const userChartRef  = ref(null)
let trendChart = null
let userChart  = null

// 加载门店列表
const loadStoreList = async () => {
  if (isStoreAdmin) return
  try {
    const res = await request.get('/store/list')
    if (res.code === 1 || res.code === 200) storeList.value = res.data || []
  } catch (e) { console.warn('加载门店列表失败', e) }
}

// 加载看板数据
const loadData = async () => {
  loading.value = true
  try {
    const params = { days: days.value }
    if (selectedStoreId.value) params.storeId = selectedStoreId.value
    else if (isStoreAdmin && fixedStoreId) params.storeId = fixedStoreId
    const res = await request.get('/statistics/operation-board', { params })
    if (res.code === 1 || res.code === 200) {
      data.value = res.data || {}
      generatedAt.value = res.data?.generatedAt || ''
      await nextTick()
      renderTrendChart()
      renderUserChart()
    }
  } catch (e) { console.error('加载经营看板失败', e) }
  finally { loading.value = false }
}

// 核心指标卡片
const metricCards = computed(() => [
  {
    key: 'conversion', icon: '🎯', label: '预约转化率',
    value: `${data.value.conversionRate ?? 0}%`,
    sub: `${data.value.paidReservations ?? 0} / ${data.value.totalReservations ?? 0} 笔`,
    color: '#409eff', progress: data.value.conversionRate ?? 0
  },
  {
    key: 'repurchase', icon: '🔁', label: '复购率',
    value: `${data.value.repurchaseRate ?? 0}%`,
    sub: `${data.value.repurchaseUsers ?? 0} / ${data.value.totalPayingUsers ?? 0} 人`,
    color: '#9b59b6', progress: data.value.repurchaseRate ?? 0
  },
  {
    key: 'roomUtil', icon: '🚪', label: '房间利用率',
    value: `${data.value.roomUtilizationRate ?? 0}%`,
    sub: `${data.value.scheduleUsed ?? 0} / ${data.value.scheduleTotal ?? 0} 场次`,
    color: '#67c23a', progress: data.value.roomUtilizationRate ?? 0
  },
  {
    key: 'cancel', icon: '❌', label: '取消率',
    value: `${data.value.cancelRate ?? 0}%`,
    sub: `取消 ${data.value.cancelReservations ?? 0} 笔`,
    color: '#e6a23c', progress: data.value.cancelRate ?? 0
  },
  {
    key: 'refund', icon: '💸', label: '退款率',
    value: `${data.value.refundRate ?? 0}%`,
    sub: `退款 ${data.value.refundCount ?? 0} 笔`,
    color: '#f56c6c', progress: data.value.refundRate ?? 0
  },
  {
    key: 'avg', icon: '💰', label: '平均客单价',
    value: `¥${data.value.avgOrderAmount ?? 0}`,
    sub: `总营收 ¥${data.value.totalRevenue ?? 0}`,
    color: '#e6a23c'
  }
])

// 转化漏斗
const funnelItems = computed(() => {
  const total     = data.value.totalReservations || 0
  const paid      = data.value.paidReservations || 0
  const cancel    = data.value.cancelReservations || 0
  const refund    = data.value.refundCount || 0
  const completed = Math.max(0, paid - refund)
  const pct = (n) => total ? Math.round(n / total * 100) : 0
  return [
    { icon: '📋', label: '创建预约', num: total,           pct: 100,         rate: '100%',            cls: 'bar-blue'   },
    { icon: '💳', label: '完成支付', num: paid,            pct: pct(paid),   rate: `${pct(paid)}%`,   cls: 'bar-green'  },
    { icon: '✅', label: '成功完成', num: completed,       pct: pct(completed), rate: `${pct(completed)}%`, cls: 'bar-purple' },
    { icon: '↩️', label: '取消/退款', num: cancel + refund, pct: pct(cancel+refund), rate: `${pct(cancel+refund)}%`, cls: 'bar-red' },
  ]
})

const completionRate = computed(() => {
  const total = data.value.totalReservations || 0
  const paid = data.value.paidReservations || 0
  const refund = data.value.refundCount || 0
  const completed = Math.max(0, paid - refund)
  return total ? Math.round(completed / total * 100) : 0
})
const conversionOk = computed(() => (data.value.conversionRate || 0) >= 50)

// 热度榜
const getRankBarWidth = (cnt) => {
  const max = data.value.scriptHotRank?.[0]?.cnt || 1
  return Math.round((cnt / max) * 100)
}
const rankBarColor = (idx) => {
  const colors = ['linear-gradient(90deg,#FFD700,#FFE87C)', 'linear-gradient(90deg,#C0C0C0,#E8E8E8)', 'linear-gradient(90deg,#CD7F32,#E8A87C)']
  return colors[idx] || 'linear-gradient(90deg,#409eff,#67c23a)'
}

// 利用率
const getUtilColor = (pct) => pct >= 80 ? '#67c23a' : pct >= 50 ? '#e6a23c' : '#f56c6c'
const getUtilTipClass = (pct) => pct >= 80 ? 'tip-good' : pct >= 50 ? 'tip-warn' : 'tip-bad'
const getUtilTip = (pct) => {
  if (pct >= 80) return '✅ 利用率良好，场次安排合理'
  if (pct >= 50) return '⚠️ 利用率中等，可适当增加营销'
  return '❌ 利用率偏低，建议优化排期或加强推广'
}

const formatTime = (t) => t ? t.substring(0, 16).replace('T', ' ') : ''

// 趋势图
const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const dates  = data.value.trendDates || []
  const values = trendType.value === 'count' ? (data.value.trendCounts || []) : (data.value.trendRevenues || [])
  const name   = trendType.value === 'count' ? '预约量' : '营收(元)'
  const color  = trendType.value === 'count' ? '#409eff' : '#e6a23c'
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>${name}：${trendType.value === 'revenue' ? '¥' : ''}${p.value}${trendType.value === 'count' ? ' 单' : ''}`
      }
    },
    grid: { left: 55, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category', data: dates,
      axisLabel: { fontSize: 11, color: '#909399' },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value', name,
      nameTextStyle: { color: '#909399', fontSize: 11 },
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } }
    },
    series: [{
      name, type: 'line', smooth: true, data: values,
      symbol: 'circle', symbolSize: 6,
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: color + '50' }, { offset: 1, color: color + '05' }] } },
      lineStyle: { color, width: 2.5 },
      itemStyle: { color, borderColor: '#fff', borderWidth: 2 }
    }]
  }, true)
}

// 用户分层饼图
const renderUserChart = () => {
  if (!userChartRef.value) return
  if (!userChart) userChart = echarts.init(userChartRef.value)
  const newC = data.value.newCustomers || 0
  const retC = data.value.returningCustomers || 0
  userChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '42%'],
      data: [
        { name: '新客户', value: newC, itemStyle: { color: '#409eff' } },
        { name: '回头客', value: retC, itemStyle: { color: '#67c23a' } },
      ],
      label: {
        show: newC + retC > 0,
        formatter: '{b}\n{d}%',
        fontSize: 12
      },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  }, true)
}

// resize
const handleResize = () => { trendChart?.resize(); userChart?.resize() }

onMounted(async () => {
  await loadStoreList()
  await loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  userChart?.dispose()
})
</script>

<style scoped>
.operation-board { display: flex; flex-direction: column; gap: 16px; }

/* 筛选栏 */
.filter-card { border-radius: 10px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.board-title { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 700; color: #303133; }
.title-icon  { font-size: 22px; }
.filter-controls { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.update-time { font-size: 12px; color: #909399; }

/* 指标卡片 */
.metric-cards { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.metric-card {
  background: #fff; border-radius: 10px; padding: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.08);
  border-top: 3px solid #ebeef5;
  transition: transform 0.2s, box-shadow 0.2s;
}
.metric-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
.mc-header { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.mc-icon   { font-size: 18px; }
.mc-label  { font-size: 12px; color: #909399; }
.mc-value  { font-size: 24px; font-weight: 700; line-height: 1.2; }
.mc-sub    { font-size: 12px; color: #909399; margin-top: 4px; }

/* 图表行 */
.chart-row { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.trend-card, .rank-card { border-radius: 10px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; }
.chart-box { width: 100%; height: 270px; }

/* 热度榜 */
.script-rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank-item { display: flex; align-items: center; gap: 8px; }
.rank-num {
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0;
  background: #f0f0f0; color: #606266;
}
.rank-num.top1 { background: #FFD700; color: #fff; }
.rank-num.top2 { background: #C0C0C0; color: #fff; }
.rank-num.top3 { background: #CD7F32; color: #fff; }
.rank-name { width: 90px; font-size: 13px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex-shrink: 0; }
.rank-bar-wrap { flex: 1; background: #f5f7fa; border-radius: 4px; height: 10px; overflow: hidden; }
.rank-bar { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.rank-cnt { font-size: 12px; color: #909399; flex-shrink: 0; width: 36px; text-align: right; }

/* 底部 */
.bottom-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.bottom-card { border-radius: 10px; }

/* 漏斗 */
.funnel-list { display: flex; flex-direction: column; gap: 12px; }
.funnel-item { display: flex; align-items: center; gap: 8px; }
.fi-label { font-size: 13px; color: #606266; width: 72px; flex-shrink: 0; }
.fi-bar-wrap { flex: 1; background: #f5f7fa; border-radius: 6px; height: 26px; overflow: hidden; }
.fi-bar { height: 100%; border-radius: 6px; display: flex; align-items: center; padding-left: 8px; transition: width 0.7s ease; min-width: 4%; }
.fi-num { font-size: 12px; font-weight: 600; color: #fff; }
.fi-rate { font-size: 12px; color: #909399; width: 44px; text-align: right; flex-shrink: 0; }
.bar-blue   { background: linear-gradient(90deg,#409eff,#66b1ff); }
.bar-green  { background: linear-gradient(90deg,#67c23a,#95d475); }
.bar-purple { background: linear-gradient(90deg,#9b59b6,#c39bd3); }
.bar-red    { background: linear-gradient(90deg,#f56c6c,#fab6b6); }
.funnel-tip { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.tip-text   { font-size: 12px; color: #909399; }

/* 利用率 */
.util-center { display: flex; flex-direction: column; align-items: center; gap: 14px; }
.util-label  { text-align: center; }
.util-pct    { font-size: 26px; font-weight: 700; color: #303133; }
.util-sub    { font-size: 12px; color: #909399; }
.util-stats  { display: flex; gap: 20px; }
.us-item     { text-align: center; }
.us-num      { display: block; font-size: 20px; font-weight: 700; }
.us-label    { font-size: 12px; color: #909399; }
.util-tip    { font-size: 13px; padding: 6px 14px; border-radius: 8px; text-align: center; }
.tip-good { background: #f0f9eb; color: #67c23a; }
.tip-warn { background: #fdf6ec; color: #e6a23c; }
.tip-bad  { background: #fef0f0; color: #f56c6c; }

/* 用户分层 */
.user-chart-box { width: 100%; height: 200px; }
.user-stats { display: flex; justify-content: space-around; margin-top: 8px; }
.user-stat-item { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.usi-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }

@media (max-width: 1400px) {
  .metric-cards { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1100px) {
  .metric-cards { grid-template-columns: repeat(2, 1fr); }
  .chart-row    { grid-template-columns: 1fr; }
  .bottom-row   { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .metric-cards { grid-template-columns: 1fr; }
}
</style>
