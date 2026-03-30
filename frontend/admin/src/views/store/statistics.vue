<template>
  <div class="statistics-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="32"><Shop /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalStores || 0 }}</div>
              <div class="stat-label">门店总数</div>
              <div class="stat-detail">
                <span class="success">营业: {{ statistics.openStores || 0 }}</span>
                <span class="danger">停业: {{ statistics.closedStores || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="32"><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalRooms || 0 }}</div>
              <div class="stat-label">房间总数</div>
              <div class="stat-detail">
                <span class="success">可用: {{ statistics.availableRooms || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="32"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalReviews || 0 }}</div>
              <div class="stat-label">评价总数</div>
              <div class="stat-detail">
                <span class="success">好评: {{ statistics.goodReviews || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="32"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.averageRating || 0 }}</div>
              <div class="stat-label">平均评分</div>
              <div class="stat-detail">
                <el-rate 
                  v-model="ratingValue" 
                  disabled 
                  show-score 
                  text-color="#ff9900"
                  score-template="{value}"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 预约趋势折线图 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📈 预约趋势（近{{ chartDays }}天）</span>
              <el-radio-group v-model="chartDays" size="small" @change="fetchCharts">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="14">近14天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 300px;" v-loading="chartsLoading" />
        </el-card>
      </el-col>

      <!-- 评分分布饼图 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>👥 会员等级分布</span>
          </template>
          <div ref="ratingChartRef" style="height: 300px;" v-loading="chartsLoading" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 剧本热度TOP5 + 实时数据 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 剧本热度TOP5柱状图 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>🔥 剧本热度 TOP5</span>
          </template>
          <div ref="scriptChartRef" style="height: 280px;" v-loading="rankingsLoading" />
        </el-card>
      </el-col>

      <!-- 实时数据面板 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>⚡ 实时数据</span>
              <el-button size="small" @click="fetchRealtime">刷新</el-button>
            </div>
          </template>
          <div v-loading="realtimeLoading">
            <div class="realtime-item" v-for="item in realtimeList" :key="item.id">
              <div class="realtime-left">
                <span class="realtime-name">{{ item.scriptName || '—' }}</span>
                <span class="realtime-time">
                  {{ item.storeName ? item.storeName + ' · ' : '' }}{{ formatTime(item.reservationTime) }}
                </span>
              </div>
              <el-tag size="small" type="primary">
                {{ item.userNickname || '用户' }}
              </el-tag>
            </div>
            <el-empty v-if="!realtimeLoading && realtimeList.length === 0" description="暂无实时数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 门店列表 -->
    <el-card class="store-list-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>门店详情</span>
          <el-button type="primary" @click="refreshData">刷新数据</el-button>
        </div>
      </template>

      <el-table :data="storeList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="门店名称" width="200" />
        <el-table-column prop="address" label="地址" min-width="250" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="营业时间" width="150">
          <template #default="{ row }">
            {{ row.openTime }} - {{ row.closeTime }}
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRatingType(row.rating)">
              {{ row.rating || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '营业中' : '已停业' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 门店详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="门店详细信息"
      width="900px"
    >
      <div v-loading="detailLoading" class="store-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店名称">{{ storeDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ storeDetail.phone }}</el-descriptions-item>
          <el-descriptions-item label="门店地址" :span="2">{{ storeDetail.address }}</el-descriptions-item>
          <el-descriptions-item label="营业时间">
            {{ storeDetail.openTime }} - {{ storeDetail.closeTime }}
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate v-model="detailRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="评价数量">{{ storeDetail.reviewCount || 0 }} 条</el-descriptions-item>
          <el-descriptions-item label="门店简介" :span="2">
            {{ storeDetail.description || '暂无简介' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">房间列表</el-divider>
        <el-table :data="storeDetail.rooms" style="width: 100%">
          <el-table-column prop="name" label="房间名称" width="150" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              {{ getRoomTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="容纳人数" width="100" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '可用' : '不可用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Shop, Reading, User, Star } from '@element-plus/icons-vue'
import request from '@/utils/request'
import * as echarts from 'echarts'

const isSuperAdmin = computed(() => localStorage.getItem('admin-login-type') !== 'store')

// 图表相关
const trendChartRef = ref(null)
const ratingChartRef = ref(null)
const scriptChartRef = ref(null)
let trendChart = null
let ratingChart = null
let scriptChart = null

const chartDays = ref(7)
const chartsLoading = ref(false)
const rankingsLoading = ref(false)
const realtimeLoading = ref(false)
const realtimeList = ref([])

const loading = ref(false)
const detailLoading = ref(false)
const detailDialogVisible = ref(false)
const storeList = ref([])
const storeDetail = ref({})

const statistics = reactive({
  totalStores: 0,
  openStores: 0,
  closedStores: 0,
  totalRooms: 0,
  availableRooms: 0,
  averageRating: 0,
  totalReviews: 0,
  goodReviews: 0
})

const ratingValue = computed(() => {
  return parseFloat(statistics.averageRating) || 0
})

const detailRating = computed(() => {
  return parseFloat(storeDetail.value.rating) || 0
})

const fetchStatistics = async () => {
  try {
    const params = {}
    if (!isSuperAdmin.value) {
      params.storeId = localStorage.getItem('admin-store-id')
    }
    const res = await request.get('/store/statistics', { params })
    Object.assign(statistics, res.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

const fetchStoreList = async () => {
  loading.value = true
  try {
    let res
    if (isSuperAdmin.value) {
      res = await request.get('/store/list')
      storeList.value = res.data
    } else {
      // 门店管理员只看自己的门店
      const storeId = localStorage.getItem('admin-store-id')
      res = await request.get(`/store/detail/${storeId}`)
      storeList.value = res.data ? [res.data] : []
    }
  } catch (error) {
    console.error('获取门店列表失败:', error)
    ElMessage.error('获取门店列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (id) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const res = await request.get(`/store/detail/${id}`)
    storeDetail.value = res.data
  } catch (error) {
    console.error('获取门店详情失败:', error)
    ElMessage.error('获取门店详情失败')
  } finally {
    detailLoading.value = false
  }
}

const getRatingType = (rating) => {
  if (rating >= 4.5) return 'success'
  if (rating >= 4.0) return 'warning'
  return 'danger'
}

const getRoomTypeText = (type) => {
  const map = { 1: '小房', 2: '中房', 3: '大房' }
  return map[type] || '未知'
}

// ===== 图表数据获取 =====
const fetchCharts = async () => {
  chartsLoading.value = true
  try {
    const params = { days: chartDays.value }
    if (!isSuperAdmin.value) params.storeId = localStorage.getItem('admin-store-id')
    const res = await request.get('/statistics/charts', { params })
    const data = res.data || {}
    await nextTick()
    renderTrendChart(data)
    renderRatingChart(data)
  } catch (e) {
    console.error('获取图表数据失败', e)
  } finally {
    chartsLoading.value = false
  }
}

const fetchRankings = async () => {
  rankingsLoading.value = true
  try {
    const params = { limit: 5 }
    if (!isSuperAdmin.value) params.storeId = localStorage.getItem('admin-store-id')
    const res = await request.get('/statistics/rankings', { params })
    const data = res.data || {}
    await nextTick()
    renderScriptChart(data)
  } catch (e) {
    console.error('获取排行榜数据失败', e)
  } finally {
    rankingsLoading.value = false
  }
}

const fetchRealtime = async () => {
  realtimeLoading.value = true
  try {
    const params = { limit: 8 }
    if (!isSuperAdmin.value) params.storeId = localStorage.getItem('admin-store-id')
    const res = await request.get('/statistics/realtime', { params })
    // 后端字段：recentReservations[].scriptName / storeName / reservationTime
    realtimeList.value = res.data?.recentReservations || []
  } catch (e) {
    console.error('获取实时数据失败', e)
    realtimeList.value = []
  } finally {
    realtimeLoading.value = false
  }
}

// ===== ECharts 渲染函数 =====
const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  // 后端字段：revenueDates / revenueAmounts / userGrowthDates / userGrowthCounts
  const dates = data.revenueDates || data.userGrowthDates || []
  const revenues = data.revenueAmounts || []
  const userCounts = data.userGrowthCounts || []
  trendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['营收(元)', '新增用户'], top: 0 },
    grid: { left: 50, right: 60, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 10 ? 30 : 0 } },
    yAxis: [
      { type: 'value', name: '营收(元)', position: 'left', axisLabel: { formatter: '¥{value}' } },
      { type: 'value', name: '新增用户', position: 'right' }
    ],
    series: [
      {
        name: '营收(元)', type: 'bar', data: revenues,
        itemStyle: { color: 'rgba(64,158,255,0.8)', borderRadius: [4,4,0,0] }
      },
      {
        name: '新增用户', type: 'line', smooth: true, yAxisIndex: 1, data: userCounts,
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.10)' }
      }
    ]
  })
}

const renderRatingChart = (data) => {
  if (!ratingChartRef.value) return
  if (!ratingChart) ratingChart = echarts.init(ratingChartRef.value)
  // 用会员等级分布 memberLevelDistribution 做饼图（StatisticsChartsVO 没有评分分布字段）
  const dist = data.memberLevelDistribution || {}
  const levelNames = { 0: '普通用户', 1: '见习侦探', 2: '银章侦探', 3: '金章侦探', 4: '传奇侦探' }
  const colors = ['#909399', '#409eff', '#67c23a', '#e6a23c', '#c0392b']
  const pieData = Object.entries(dist).map(([level, count], idx) => ({
    name: levelNames[level] || `等级${level}`,
    value: count,
    itemStyle: { color: colors[Number(level)] || colors[0] }
  })).filter(d => d.value > 0)
  ratingChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}条 ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['65%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: pieData
    }]
  })
}

const renderScriptChart = (data) => {
  if (!scriptChartRef.value) return
  if (!scriptChart) scriptChart = echarts.init(scriptChartRef.value)
  // 后端字段：scriptRankings[].name / bookingCount / rating
  const scripts = (data.scriptRankings || []).slice(0, 5)
  const names = scripts.map(s => s.name || '未知')
  const counts = scripts.map(s => s.bookingCount || 0)
  const ratings = scripts.map(s => Number(s.rating || 0).toFixed(1))
  const barColors = ['#c0392b','#e74c3c','#e67e22','#f39c12','#f1c40f']
  scriptChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const idx = params[0].dataIndex
        return `${names[idx]}<br/>预约数：${counts[idx]}<br/>评分：⭐${ratings[idx]}`
      }
    },
    grid: { left: 120, right: 60, top: 20, bottom: 40 },
    xAxis: { type: 'value', name: '预约次数' },
    yAxis: {
      type: 'category', data: names,
      axisLabel: { width: 100, overflow: 'truncate' }
    },
    series: [{
      type: 'bar', data: counts, barMaxWidth: 32,
      itemStyle: {
        color: (params) => barColors[params.dataIndex] || '#409eff',
        borderRadius: [0,4,4,0]
      },
      label: { show: true, position: 'right', formatter: (p) => `${p.value}次 ⭐${ratings[p.dataIndex]}` }
    }]
  })
}

// ===== 实用函数 =====
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const getReservationTagType = (status) => {
  const map = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getReservationStatusText = (status) => {
  const map = { 1: '待确认', 2: '已确认', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const refreshData = () => {
  fetchStatistics()
  fetchStoreList()
  fetchCharts()
  fetchRankings()
  fetchRealtime()
  ElMessage.success('数据已刷新')
}

// 窗口resize时自动调整图表大小
const handleResize = () => {
  trendChart?.resize()
  ratingChart?.resize()
  scriptChart?.resize()
}

onMounted(() => {
  fetchStatistics()
  fetchStoreList()
  fetchCharts()
  fetchRankings()
  fetchRealtime()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  ratingChart?.dispose()
  scriptChart?.dispose()
})
</script>

<style scoped>
.statistics-dashboard {
  width: 100%;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-detail {
  font-size: 12px;
  color: #606266;
}

.stat-detail .success {
  color: #67c23a;
  margin-right: 12px;
}

.stat-detail .danger {
  color: #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.store-detail {
  padding: 12px;
}

.realtime-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.realtime-item:last-child { border-bottom: none; }
.realtime-left { display: flex; flex-direction: column; gap: 2px; }
.realtime-name { font-size: 13px; color: #303133; font-weight: 500; }
.realtime-time { font-size: 11px; color: #909399; }
</style>
