<template>
  <div class="daily-report-page">

    <!-- 顶部刷新栏 -->
    <div class="report-header">
      <div class="report-title">
        <span class="title-icon">📊</span>
        <span>门店营收日报</span>
        <el-tag size="small" type="info" style="margin-left:10px">{{ todayLabel }}</el-tag>
      </div>
      <div class="report-actions">
        <span class="report-time" v-if="lastUpdate">更新于 {{ lastUpdate }}</span>
        <el-button :icon="Refresh" @click="loadData" :loading="loading" size="small">刷新</el-button>
        <el-switch v-model="autoRefresh" size="small" active-text="自动刷新" />
      </div>
    </div>

    <div v-loading="loading">
      <!-- ── 今日核心KPI ── -->
      <div class="kpi-grid">
        <div class="kpi-card revenue">
          <div class="kpi-icon">💰</div>
          <div class="kpi-body">
            <div class="kpi-label">今日营收</div>
            <div class="kpi-value">¥{{ fmt(report.todayRevenue) }}</div>
            <div class="kpi-sub">
              昨日 ¥{{ fmt(report.yesterdayRevenue) }}
              <span :class="revenueGrowthClass">{{ growthText(report.revenueGrowth) }}</span>
            </div>
          </div>
        </div>

        <div class="kpi-card reservation">
          <div class="kpi-icon">📋</div>
          <div class="kpi-body">
            <div class="kpi-label">今日预约</div>
            <div class="kpi-value">{{ report.todayReservations ?? 0 }} <small>单</small></div>
            <div class="kpi-sub">
              昨日 {{ report.yesterdayReservations ?? 0 }} 单
              <span :class="reservationGrowthClass">{{ growthText(report.reservationGrowth) }}</span>
            </div>
          </div>
        </div>

        <div class="kpi-card avg">
          <div class="kpi-icon">🎯</div>
          <div class="kpi-body">
            <div class="kpi-label">平均客单价</div>
            <div class="kpi-value">¥{{ fmt(report.todayAvgAmount) }}</div>
            <div class="kpi-sub">今日已支付 {{ report.todayPaid ?? 0 }} 单</div>
          </div>
        </div>

        <div class="kpi-card warning">
          <div class="kpi-icon">⚠️</div>
          <div class="kpi-body">
            <div class="kpi-label">退款 / 取消</div>
            <div class="kpi-value">{{ report.todayRefund ?? 0 }} <small>/ {{ report.todayCancel ?? 0 }}</small></div>
            <div class="kpi-sub">
              <span v-if="(report.todayRefund||0)+(report.todayCancel||0)===0" style="color:#67c23a">✅ 今日无异常</span>
              <span v-else style="color:#f56c6c">今日退款/取消笔数</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 本周 / 本月汇总 ── -->
      <div class="summary-row">
        <div class="summary-card">
          <div class="sc-icon">📅</div>
          <div class="sc-label">本周营收</div>
          <div class="sc-value">¥{{ fmt(report.weekRevenue) }}</div>
          <div class="sc-sub">{{ report.weekReservations ?? 0 }} 单 · {{ report.weekPaid ?? 0 }} 已付</div>
        </div>
        <div class="summary-card">
          <div class="sc-icon">🗓️</div>
          <div class="sc-label">本月营收</div>
          <div class="sc-value">¥{{ fmt(report.monthRevenue) }}</div>
          <div class="sc-sub">{{ report.monthReservations ?? 0 }} 单 · {{ report.monthPaid ?? 0 }} 已付</div>
        </div>
        <div class="summary-card">
          <div class="sc-icon">📈</div>
          <div class="sc-label">本月转化率</div>
          <div class="sc-value">{{ monthConversionRate }}%</div>
          <div class="sc-sub">已付 / 总预约</div>
        </div>
        <div class="summary-card">
          <div class="sc-icon">💎</div>
          <div class="sc-label">本月客单价</div>
          <div class="sc-value">¥{{ monthAvgAmount }}</div>
          <div class="sc-sub">月均水平</div>
        </div>
      </div>

      <!-- ── 图表行 ── -->
      <div class="chart-row">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>📈 本周每日营收趋势</span>
              <div class="chart-legend">
                <span class="legend-item bar">■ 营收</span>
                <span class="legend-item line">— 预约数</span>
              </div>
            </div>
          </template>
          <div ref="weekChartRef" class="echarts-box"></div>
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>🕐 今日各时段预约分布</span>
              <el-tag v-if="peakHour !== null" size="small" type="warning">
                峰值 {{ peakHour }}:00
              </el-tag>
            </div>
          </template>
          <div ref="hourChartRef" class="echarts-box"></div>
        </el-card>
      </div>

      <!-- ── 今日剧本热度 TOP5 ── -->
      <el-card class="script-card" shadow="hover">
        <template #header>
          <div class="chart-header">
            <span>🔥 今日剧本热度 TOP5</span>
            <span class="total-tip" v-if="totalTodayCnt">共 {{ totalTodayCnt }} 人次预约</span>
          </div>
        </template>
        <el-empty v-if="!report.topScripts?.length" description="今日暂无预约数据" :image-size="60" />
        <el-table v-else :data="report.topScripts" size="small" style="width:100%">
          <el-table-column type="index" label="排名" width="60" align="center">
            <template #default="{ $index }">
              <span class="rank-badge">
                {{ $index === 0 ? '🥇' : $index === 1 ? '🥈' : $index === 2 ? '🥉' : $index + 1 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="scriptName" label="剧本名称" min-width="140" />
          <el-table-column prop="cnt" label="预约人次" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.cnt }} 次</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="今日营收" width="120" align="right">
            <template #default="{ row }">
              <span class="rev-text">¥{{ fmt(row.rev) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="热度占比" min-width="180">
            <template #default="{ row }">
              <div class="progress-wrap">
                <el-progress
                  :percentage="totalTodayCnt ? Math.round(row.cnt / totalTodayCnt * 100) : 0"
                  :stroke-width="8"
                  :show-text="true"
                  status="success"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getStoreDailyReport } from '@/api/schedule'
import * as echarts from 'echarts'

const loading    = ref(false)
const report     = ref({})
const lastUpdate = ref('')
const autoRefresh = ref(false)
let autoTimer = null

const loginType    = localStorage.getItem('admin-login-type') || 'admin'
const fixedStoreId = loginType === 'store' ? localStorage.getItem('admin-store-id') : null

const weekChartRef = ref(null)
const hourChartRef = ref(null)
let weekChart = null
let hourChart = null

// 今日标签（含星期）
const todayLabel = computed(() => {
  const d = new Date()
  const days = ['周日','周一','周二','周三','周四','周五','周六']
  return `${d.getMonth()+1}月${d.getDate()}日 ${days[d.getDay()]}`
})

// 计算属性
const monthConversionRate = computed(() => {
  const { monthReservations, monthPaid } = report.value
  if (!monthReservations) return 0
  return Math.round(monthPaid / monthReservations * 100)
})

const monthAvgAmount = computed(() => {
  const { monthRevenue, monthPaid } = report.value
  if (!monthPaid) return '0.00'
  return fmt(monthRevenue / monthPaid)
})

const totalTodayCnt = computed(() =>
  (report.value.topScripts || []).reduce((s, r) => s + Number(r.cnt || 0), 0)
)

const peakHour = computed(() => {
  const raw = report.value.hourlyDistribution || []
  if (!raw.length) return null
  const max = Math.max(...raw.map(r => Number(r.cnt)))
  if (max === 0) return null
  const peak = raw.find(r => Number(r.cnt) === max)
  return peak ? Number(peak.h) : null
})

// 增长色：涨绿跌红（符合常规认知）
const revenueGrowthClass = computed(() => {
  const v = Number(report.value.revenueGrowth)
  return isNaN(v) ? '' : v >= 0 ? 'growth-up' : 'growth-down'
})
const reservationGrowthClass = computed(() => {
  const v = Number(report.value.reservationGrowth)
  return isNaN(v) ? '' : v >= 0 ? 'growth-up' : 'growth-down'
})

const fmt = (v) => v != null ? Number(v).toFixed(2) : '0.00'

const growthText = (v) => {
  if (v == null) return ''
  const n = Number(v)
  return (n >= 0 ? '▲' : '▼') + Math.abs(n).toFixed(1) + '%'
}

// 本周营收趋势
const renderWeekChart = () => {
  if (!weekChartRef.value) return
  if (!weekChart) weekChart = echarts.init(weekChartRef.value)
  const dates = report.value.weekTrendDates    || []
  const revs  = (report.value.weekTrendRevenues|| []).map(Number)
  const cnts  = (report.value.weekTrendCounts  || []).map(Number)
  const weekdays = ['周日','周一','周二','周三','周四','周五','周六']
  const labels = dates.map(d => {
    const dt = new Date(d + 'T00:00:00')
    return `${d.substring(5)} ${weekdays[dt.getDay()]}`
  })
  weekChart.setOption({
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'cross' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        return `${dates[idx]}<br/>💰 营收：¥${Number(revs[idx]).toFixed(2)}<br/>📋 预约：${cnts[idx]} 单`
      }
    },
    legend: { data: ['营收(元)', '预约单数'], top: 4, textStyle: { fontSize: 12 } },
    grid: { left: 55, right: 50, top: 36, bottom: 28 },
    xAxis: {
      type: 'category', data: labels,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value', name: '营收(元)',
        nameTextStyle: { color: '#909399', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11, formatter: v => v >= 10000 ? (v/10000).toFixed(1)+'w' : v },
        splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } }
      },
      {
        type: 'value', name: '单数',
        nameTextStyle: { color: '#909399', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '营收(元)', type: 'bar', yAxisIndex: 0, data: revs, barMaxWidth: 40,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0,0,0,1,[
            {offset:0,color:'#5b8ff9'},{offset:1,color:'#a8c1fd'}
          ]),
          borderRadius: [4,4,0,0]
        },
        label: { show: true, position: 'top', formatter: p => p.value>0?`¥${Number(p.value).toFixed(0)}`:'', fontSize:11, color:'#5b8ff9' }
      },
      {
        name: '预约单数', type: 'line', yAxisIndex: 1, data: cnts, smooth: true,
        symbol: 'circle', symbolSize: 7,
        lineStyle: { color: '#5ad8a6', width: 2.5 },
        itemStyle: { color: '#5ad8a6', borderWidth: 2, borderColor: '#fff' },
        label: { show: true, position: 'top', formatter: p => p.value>0?`${p.value}单`:'', fontSize:11, color:'#5ad8a6' }
      }
    ]
  })
}

// 今日时段分布
const renderHourChart = () => {
  if (!hourChartRef.value) return
  if (!hourChart) hourChart = echarts.init(hourChartRef.value)
  const raw = report.value.hourlyDistribution || []
  const data = raw.map(r => ({ h: Number(r.h), cnt: Number(r.cnt) }))
  const maxCnt = Math.max(...data.map(x => x.cnt), 1)
  hourChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: params => `${String(params[0].axisValue).padStart(2,'0')}:00 - ${String(params[0].axisValue).padStart(2,'0')}:59<br/>预约 ${params[0].value} 单`
    },
    grid: { left: 36, right: 12, top: 20, bottom: 28 },
    xAxis: {
      type: 'category', data: data.map(d => d.h),
      axisLabel: { color: '#909399', fontSize: 11, formatter: v => `${v}时` },
      axisTick: { show: false }, axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: data.map(d => ({
        value: d.cnt,
        itemStyle: {
          color: d.cnt === maxCnt
            ? new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#ffb940'},{offset:1,color:'#ffd680'}])
            : new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#5b8ff9'},{offset:1,color:'#c6d9fd'}])
        }
      })),
      barMaxWidth: 32, itemStyle: { borderRadius: [4,4,0,0] },
      label: { show: true, position: 'top', formatter: p => p.value>0?p.value:'', fontSize:11, color:'#606266' }
    }]
  })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStoreDailyReport(fixedStoreId ? Number(fixedStoreId) : null)
    if (res.code === 1 || res.code === 200) {
      report.value = res.data || {}
      lastUpdate.value = new Date().toLocaleTimeString('zh-CN')
      await nextTick()
      renderWeekChart()
      renderHourChart()
    }
  } catch (e) {
    console.error('加载日报失败', e)
  } finally {
    loading.value = false
  }
}

const handleResize = () => { weekChart?.resize(); hourChart?.resize() }

// 自动刷新（30秒）
const startAutoRefresh = () => {
  autoTimer = setInterval(() => { if (autoRefresh.value) loadData() }, 30000)
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
  startAutoRefresh()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  clearInterval(autoTimer)
  weekChart?.dispose()
  hourChart?.dispose()
})
</script>

<style scoped>
.daily-report-page { padding: 0; }

.report-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.report-title { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 700; color: #303133; }
.title-icon { font-size: 22px; }
.report-actions { display: flex; align-items: center; gap: 10px; }
.report-time { font-size: 12px; color: #909399; }

/* KPI */
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px; }
.kpi-card {
  background: #fff; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 5px solid #409eff;
  transition: transform 0.2s, box-shadow 0.2s;
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.12); }
.kpi-card.revenue     { border-left-color: #409eff; }
.kpi-card.reservation { border-left-color: #67c23a; }
.kpi-card.avg         { border-left-color: #e6a23c; }
.kpi-card.warning     { border-left-color: #f56c6c; }
.kpi-icon  { font-size: 34px; flex-shrink: 0; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 26px; font-weight: 800; color: #303133; line-height: 1; }
.kpi-value small { font-size: 14px; font-weight: 400; }
.kpi-sub   { font-size: 12px; color: #909399; margin-top: 6px; display: flex; gap: 6px; align-items: center; }
.growth-up   { color: #67c23a; font-weight: 700; }
.growth-down { color: #f56c6c; font-weight: 700; }

/* 汇总 */
.summary-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px; }
.summary-card {
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border: 1px solid #e4e7ed; border-radius: 10px; padding: 16px 20px; text-align: center;
  transition: box-shadow 0.2s;
}
.summary-card:hover { box-shadow: 0 4px 12px rgba(64,158,255,0.12); }
.sc-icon  { font-size: 22px; margin-bottom: 4px; }
.sc-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.sc-value { font-size: 22px; font-weight: 700; color: #303133; }
.sc-sub   { font-size: 12px; color: #c0c4cc; margin-top: 4px; }

/* 图表 */
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.chart-card { border-radius: 10px; }
.echarts-box { width: 100%; height: 250px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; }
.chart-legend { display: flex; gap: 12px; font-size: 12px; color: #909399; }
.legend-item.bar  { color: #5b8ff9; }
.legend-item.line { color: #5ad8a6; }

/* 剧本热度 */
.script-card { border-radius: 10px; }
.total-tip   { font-size: 12px; color: #909399; }
.rev-text    { color: #409eff; font-weight: 600; }
.progress-wrap { min-width: 120px; }

@media (max-width: 1100px) {
  .kpi-grid, .summary-row { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .kpi-grid, .summary-row { grid-template-columns: 1fr; }
}
</style>
