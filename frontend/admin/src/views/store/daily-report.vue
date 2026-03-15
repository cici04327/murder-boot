<template>
  <div class="daily-report-page">

    <!-- 顶部刷新栏 -->
    <div class="report-header">
      <div class="report-title">📊 门店营收日报</div>
      <div class="report-actions">
        <span class="report-time">更新于 {{ lastUpdate }}</span>
        <el-button :icon="Refresh" @click="loadData" :loading="loading" size="small">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <!-- ── 第一行：今日核心数据 4张卡片 ── -->
      <div class="kpi-grid">
        <div class="kpi-card revenue">
          <div class="kpi-icon">💰</div>
          <div class="kpi-body">
            <div class="kpi-label">今日营收</div>
            <div class="kpi-value">¥{{ fmt(report.todayRevenue) }}</div>
            <div class="kpi-sub">
              昨日 ¥{{ fmt(report.yesterdayRevenue) }}
              <span :class="growthClass(report.revenueGrowth)">
                {{ growthText(report.revenueGrowth) }}
              </span>
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
              <span :class="growthClass(report.reservationGrowth)">
                {{ growthText(report.reservationGrowth) }}
              </span>
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
            <div class="kpi-sub">今日退款 / 取消笔数</div>
          </div>
        </div>
      </div>

      <!-- ── 第二行：本周 / 本月汇总 ── -->
      <div class="summary-row">
        <div class="summary-card">
          <div class="sc-label">本周营收</div>
          <div class="sc-value">¥{{ fmt(report.weekRevenue) }}</div>
          <div class="sc-sub">{{ report.weekReservations ?? 0 }} 单 · {{ report.weekPaid ?? 0 }} 已付</div>
        </div>
        <div class="summary-card">
          <div class="sc-label">本月营收</div>
          <div class="sc-value">¥{{ fmt(report.monthRevenue) }}</div>
          <div class="sc-sub">{{ report.monthReservations ?? 0 }} 单 · {{ report.monthPaid ?? 0 }} 已付</div>
        </div>
        <div class="summary-card">
          <div class="sc-label">本月转化率</div>
          <div class="sc-value">
            {{ report.monthReservations ? Math.round(report.monthPaid / report.monthReservations * 100) : 0 }}%
          </div>
          <div class="sc-sub">已付 / 总预约</div>
        </div>
        <div class="summary-card">
          <div class="sc-label">本月客单价</div>
          <div class="sc-value">
            ¥{{ report.monthPaid ? fmt2(report.monthRevenue / report.monthPaid) : '0.00' }}
          </div>
          <div class="sc-sub">月均水平</div>
        </div>
      </div>

      <!-- ── 第三行：本周趋势图 + 今日各时段分布 ── -->
      <div class="chart-row">
        <!-- 本周每日营收趋势 ECharts -->
        <el-card class="chart-card" shadow="hover">
          <template #header><span>📈 本周每日营收趋势</span></template>
          <div ref="weekChartRef" class="echarts-box"></div>
        </el-card>

        <!-- 今日各小时预约分布 ECharts -->
        <el-card class="chart-card" shadow="hover">
          <template #header><span>🕐 今日各时段预约分布</span></template>
          <div ref="hourChartRef" class="echarts-box"></div>
        </el-card>
      </div>

      <!-- ── 第四行：今日剧本热度 TOP5 ── -->
      <el-card class="script-card" shadow="hover">
        <template #header><span>🔥 今日剧本热度 TOP5</span></template>
        <div v-if="!report.topScripts?.length" class="no-data" style="padding:20px">今日暂无预约数据</div>
        <el-table v-else :data="report.topScripts" size="small" style="width:100%">
          <el-table-column type="index" label="排名" width="60">
            <template #default="{ $index }">
              <span class="rank-badge" :class="['r1','r2','r3'][$index] || ''">
                {{ $index === 0 ? '🥇' : $index === 1 ? '🥈' : $index === 2 ? '🥉' : $index + 1 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="scriptName" label="剧本名称" />
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
          <el-table-column label="热度占比" width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="totalTodayCnt ? Math.round(row.cnt / totalTodayCnt * 100) : 0"
                :stroke-width="6"
                :show-text="true"
                status="success"
              />
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

// 门店身份
const loginType    = localStorage.getItem('admin-login-type') || 'admin'
const fixedStoreId = loginType === 'store' ? localStorage.getItem('admin-store-id') : null

// ECharts 实例
const weekChartRef = ref(null)
const hourChartRef = ref(null)
let weekChart = null
let hourChart = null

// ── 本周营收趋势（折线 + 柱形组合）──
const renderWeekChart = () => {
  if (!weekChartRef.value) return
  if (!weekChart) weekChart = echarts.init(weekChartRef.value)

  const dates = report.value.weekTrendDates    || []
  const revs  = (report.value.weekTrendRevenues|| []).map(Number)
  const cnts  = (report.value.weekTrendCounts  || []).map(Number)
  const labels= dates.map(d => d.substring(5)) // MM-DD

  weekChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        return `${dates[idx]}<br/>
          💰 营收：¥${Number(revs[idx]).toFixed(2)}<br/>
          📋 预约：${cnts[idx]} 单`
      }
    },
    legend: {
      data: ['营收(元)', '预约单数'],
      top: 4,
      textStyle: { fontSize: 12 }
    },
    grid: { left: 50, right: 50, top: 36, bottom: 28 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    yAxis: [
      {
        type: 'value',
        name: '营收(元)',
        nameTextStyle: { color: '#909399', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11,
          formatter: v => v >= 10000 ? (v/10000).toFixed(1)+'w' : v },
        splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '单数',
        nameTextStyle: { color: '#909399', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '营收(元)',
        type: 'bar',
        yAxisIndex: 0,
        data: revs,
        barMaxWidth: 40,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5b8ff9' },
            { offset: 1, color: '#a8c1fd' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          formatter: p => p.value > 0 ? `¥${Number(p.value).toFixed(0)}` : '',
          fontSize: 11,
          color: '#5b8ff9'
        }
      },
      {
        name: '预约单数',
        type: 'line',
        yAxisIndex: 1,
        data: cnts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { color: '#5ad8a6', width: 2.5 },
        itemStyle: { color: '#5ad8a6', borderWidth: 2, borderColor: '#fff' },
        label: {
          show: true,
          position: 'top',
          formatter: p => p.value > 0 ? `${p.value}单` : '',
          fontSize: 11,
          color: '#5ad8a6'
        }
      }
    ]
  })
}

// ── 今日各时段预约分布（渐变柱形）──
const renderHourChart = () => {
  if (!hourChartRef.value) return
  if (!hourChart) hourChart = echarts.init(hourChartRef.value)

  const raw = report.value.hourlyDistribution || []
  const data = raw.map(r => ({ h: Number(r.h), cnt: Number(r.cnt) }))
  const maxCnt = Math.max(...data.map(x => x.cnt), 1)

  hourChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: params => `${params[0].name}:00 ~ ${params[0].name}:59<br/>预约 ${params[0].value} 单`
    },
    grid: { left: 36, right: 12, top: 20, bottom: 28 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.h),
      axisLabel: { color: '#909399', fontSize: 11, formatter: v => `${v}时` },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: data.map(d => ({
        value: d.cnt,
        itemStyle: {
          color: d.cnt === maxCnt
            ? new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#ffb940' },
                { offset: 1, color: '#ffd680' }
              ])
            : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#5b8ff9' },
                { offset: 1, color: '#c6d9fd' }
              ])
        }
      })),
      barMaxWidth: 32,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: {
        show: true,
        position: 'top',
        formatter: p => p.value > 0 ? p.value : '',
        fontSize: 11,
        color: '#606266'
      }
    }]
  })
}

// 加载日报数据
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

// 响应式 resize
const handleResize = () => {
  weekChart?.resize()
  hourChart?.resize()
}

const totalTodayCnt = computed(() =>
  (report.value.topScripts || []).reduce((s, r) => s + Number(r.cnt || 0), 0)
)

// 格式化
const fmt  = (v) => v != null ? Number(v).toFixed(2) : '0.00'
const fmt2 = (v) => v != null ? Number(v).toFixed(2) : '0.00'

const growthClass = (v) => {
  if (v == null) return ''
  return Number(v) >= 0 ? 'growth-up' : 'growth-down'
}
const growthText = (v) => {
  if (v == null) return ''
  const n = Number(v)
  return (n >= 0 ? '▲' : '▼') + Math.abs(n).toFixed(1) + '%'
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  weekChart?.dispose()
  hourChart?.dispose()
})
</script>

<style scoped>
.daily-report-page { padding: 0; }

/* 顶部 */
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.report-title  { font-size: 18px; font-weight: 700; color: #303133; }
.report-actions{ display: flex; align-items: center; gap: 10px; }
.report-time   { font-size: 12px; color: #909399; }

/* KPI 4格 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}
.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-left: 5px solid #409eff;
}
.kpi-card.revenue    { border-left-color: #409eff; }
.kpi-card.reservation{ border-left-color: #67c23a; }
.kpi-card.avg        { border-left-color: #e6a23c; }
.kpi-card.warning    { border-left-color: #f56c6c; }

.kpi-icon  { font-size: 32px; flex-shrink: 0; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 26px; font-weight: 800; color: #303133; line-height: 1; }
.kpi-value small { font-size: 14px; font-weight: 400; }
.kpi-sub   { font-size: 12px; color: #909399; margin-top: 6px; display: flex; gap: 6px; align-items: center; }

.growth-up   { color: #f56c6c; font-weight: 700; }
.growth-down { color: #67c23a; font-weight: 700; }

/* 汇总行 */
.summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}
.summary-card {
  background: #f8f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 16px 20px;
  text-align: center;
}
.sc-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.sc-value { font-size: 22px; font-weight: 700; color: #303133; }
.sc-sub   { font-size: 12px; color: #c0c4cc; margin-top: 4px; }

/* 图表行 */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.chart-card  { border-radius: 10px; }
.echarts-box { width: 100%; height: 240px; }

/* 剧本热度 */
.script-card { border-radius: 10px; }
.rev-text    { color: #409eff; font-weight: 600; }

@media (max-width: 1000px) {
  .kpi-grid, .summary-row { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
