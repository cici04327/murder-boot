<template>
  <div class="statistics-container">
    <el-card class="header-card">
      <h2>
        <el-icon><DataAnalysis /></el-icon>
        数据统计
      </h2>
      <p>查看您的活动数据和统计信息</p>
    </el-card>

    <!-- 核心数据统计 -->
    <el-row :gutter="15" class="stats-overview">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-primary">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.reservationCount) }}</div>
              <div class="stat-label">总预约</div>
              <div class="stat-desc" v-if="stats.completedCount">已完成 {{ stats.completedCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-success">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.favoriteCount) }}</div>
              <div class="stat-label">剧本收藏</div>
              <div class="stat-desc">{{ stats.favoriteCount > 0 ? '继续收藏' : '暂无收藏' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-warning">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.points) }}</div>
              <div class="stat-label">当前积分</div>
              <div class="stat-desc">累计获得 {{ formatNumber(stats.totalEarned) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-danger">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><Ticket /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.couponCount) }}</div>
              <div class="stat-label">优惠券</div>
              <div class="stat-desc">{{ stats.couponCount > 0 ? '可用券' : '暂无可用' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-info">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value money">{{ formatMoney(stats.totalSpent) }}</div>
              <div class="stat-label">累计消费</div>
              <div class="stat-desc">{{ stats.reservationCount }}次预约</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card stat-purple">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon class="stat-icon" size="40"><Memo /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.reviewCount) }}</div>
              <div class="stat-label">评价数</div>
              <div class="stat-desc">{{ stats.reviewCount > 0 ? '感谢分享' : '快去评价吧' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度活动趋势 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>月度活动趋势</span>
          <el-radio-group v-model="chartPeriod" size="small">
            <el-radio-button label="month">最近一月</el-radio-button>
            <el-radio-button label="quarter">最近三月</el-radio-button>
            <el-radio-button label="year">最近一年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <!-- 详细统计表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>预约记录统计</span>
        </div>
      </template>
      
      <el-table :data="reservationStats" stripe>
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column prop="count" label="预约次数" width="120">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completed" label="已完成" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.completed }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cancelled" label="已取消" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.cancelled }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="消费金额">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 成就徽章 -->
    <el-card class="achievements-card">
      <template #header>
        <div class="card-header">
          <span>我的成就</span>
          <el-tag type="warning">{{ unlockedAchievements }}/{{ achievements.length }} 已解锁</el-tag>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" v-for="achievement in achievements" :key="achievement.id">
          <div class="achievement-item" :class="{ unlocked: achievement.unlocked }">
            <el-icon class="achievement-icon" size="50">
              <component :is="achievement.icon" />
            </el-icon>
            <div class="achievement-info">
              <h4>{{ achievement.name }}</h4>
              <p>{{ achievement.desc }}</p>
              <el-progress 
                :percentage="achievement.progress" 
                :status="achievement.unlocked ? 'success' : null"
                :stroke-width="6"
              />
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useUserStore } from '@/store/user'
import { getMyReservations } from '@/api/reservation'
import { getFavoriteScripts } from '@/api/script'
import { getMyCoupons } from '@/api/coupon'
import { getUserPoints, getPointsRecords } from '@/api/user'
// echarts 体积较大：改为进入页面后再懒加载，减少首屏包体积
let echartsLib = null
const loadEcharts = async () => {
  if (echartsLib) return echartsLib
  const mod = await import('echarts')
  // 兼容 ESM/CJS 导出
  echartsLib = mod?.default ?? mod
  return echartsLib
}
import {
  DataAnalysis,
  Tickets,
  Star,
  Coin,
  Ticket,
  User,
  Memo,
  CollectionTag,
  TrendCharts
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const chartRef = ref(null)
const chartPeriod = ref('month')
let chartInstance = null

const stats = reactive({
  reservationCount: 0,
  favoriteCount: 0,
  points: 0,
  couponCount: 0,
  totalSpent: 0,
  totalEarned: 0,
  reviewCount: 0
})

const reservationStats = ref([])

const achievements = ref([
  { id: 1, name: '初次体验', desc: '完成首次预约', icon: 'Tickets', unlocked: false, progress: 0 },
  { id: 2, name: '剧本达人', desc: '体验10个剧本', icon: 'Star', unlocked: false, progress: 0 },
  { id: 3, name: '社交达人', desc: '邀请5位好友', icon: 'User', unlocked: false, progress: 0 },
  { id: 4, name: '评论家', desc: '发表20条评价', icon: 'Memo', unlocked: false, progress: 0 },
  { id: 5, name: '收藏家', desc: '收藏50个剧本', icon: 'CollectionTag', unlocked: false, progress: 0 },
  { id: 6, name: '积分大户', desc: '累计获得1000积分', icon: 'Coin', unlocked: false, progress: 0 }
])

const unlockedAchievements = computed(() => {
  return achievements.value.filter(a => a.unlocked).length
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
  let months = 1
  if (chartPeriod.value === 'quarter') months = 3
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
    // 预约数量和详情
    const reservationRes = await getMyReservations({ page: 1, pageSize: 100 })
    if (reservationRes.code === 1 || reservationRes.code === 200) {
      stats.reservationCount = reservationRes.data.total || 0
      
      // 计算总消费金额
      const allReservations = reservationRes.data.records || []
      stats.totalSpent = allReservations
        .filter(r => r.payStatus === 1)
        .reduce((sum, r) => sum + (parseFloat(r.actualAmount) || 0), 0)
      
      // 统计已完成预约数（用于成就计算）
      const completedCount = allReservations.filter(r => r.status === 3).length
      stats.completedCount = completedCount
    }
    
    // 收藏数量
    const favoriteRes = await getFavoriteScripts({ page: 1, pageSize: 1 })
    if (favoriteRes.code === 1 || favoriteRes.code === 200) {
      stats.favoriteCount = favoriteRes.data.total || 0
    }
    
    // 优惠券数量（修复：后端total不准确，使用records.length）
    const couponRes = await getMyCoupons({ status: 1, page: 1, pageSize: 1000 })
    if (couponRes.code === 1 || couponRes.code === 200) {
      // 使用records.length获取实际优惠券数量
      stats.couponCount = couponRes.data?.records?.length || 0
      console.log('统计页面优惠券数量:', stats.couponCount)
    }
    
    // 积分（从API获取最新数据）
    try {
      const pointsRes = await getUserPoints()
      if (pointsRes.code === 1 || pointsRes.code === 200) {
        stats.points = pointsRes.data.currentPoints || 0
        stats.totalEarned = pointsRes.data.totalEarned || 0
        console.log('统计页面积分已更新:', stats.points)
      }
    } catch (error) {
      console.error('加载积分失败:', error)
      // 降级方案
      if (userStore.userInfo && userStore.userInfo.points !== undefined) {
        stats.points = userStore.userInfo.points
      }
    }
    
    // 获取评价数量（从积分记录中统计）
    try {
      const pointsRecords = await getPointsRecords({ page: 1, pageSize: 100, type: 1 })
      if (pointsRecords.code === 1 || pointsRecords.code === 200) {
        const records = pointsRecords.data.records || []
        stats.reviewCount = records.filter(r => r.description && r.description.includes('评价')).length
      }
    } catch (error) {
      console.error('加载评价数量失败:', error)
    }
    
    // 计算成就进度
    calculateAchievements()
    
    // 加载月度统计
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
    await initChart()
  } catch (error) {
    console.error('加载月度统计失败:', error)
  }
}

// 初始化图表
const initChart = async () => {
  if (!chartRef.value) return

  const echarts = await loadEcharts()

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  
  const filteredData = getFilteredStats()
  
  const option = {
    title: {
      text: '预约趋势分析',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
        color: '#303133'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['预约次数', '已完成', '已取消', '消费金额'],
      bottom: 10,
      icon: 'roundRect'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: filteredData.map(s => s.month),
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      axisLabel: {
        color: '#606266',
        formatter: (value) => {
          // 格式化日期显示
          const parts = value.split('-')
          return parts[0] + '年' + parts[1] + '月'
        }
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '次数',
        position: 'left',
        axisLine: {
          lineStyle: {
            color: '#dcdfe6'
          }
        },
        axisLabel: {
          color: '#606266'
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#ebeef5'
          }
        }
      },
      {
        type: 'value',
        name: '金额(元)',
        position: 'right',
        axisLine: {
          lineStyle: {
            color: '#dcdfe6'
          }
        },
        axisLabel: {
          color: '#606266',
          formatter: '¥{value}'
        },
        splitLine: {
          show: false
        }
      }
    ],
    series: [
      {
        name: '预约次数',
        type: 'line',
        data: filteredData.map(s => s.count),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { 
          color: '#409eff',
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(64, 158, 255, 0.3)'
            }, {
              offset: 1, color: 'rgba(64, 158, 255, 0.05)'
            }]
          }
        }
      },
      {
        name: '已完成',
        type: 'line',
        data: filteredData.map(s => s.completed),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { 
          color: '#67c23a',
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          width: 3
        }
      },
      {
        name: '已取消',
        type: 'line',
        data: filteredData.map(s => s.cancelled),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { 
          color: '#909399',
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          width: 2,
          type: 'dashed'
        }
      },
      {
        name: '消费金额',
        type: 'bar',
        yAxisIndex: 1,
        data: filteredData.map(s => s.amount.toFixed(2)),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: '#f56c6c'
            }, {
              offset: 1, color: '#fca311'
            }]
          },
          borderRadius: [5, 5, 0, 0]
        },
        barWidth: '40%'
      }
    ]
  }
  
  chartInstance.setOption(option)
}

// 计算成就进度
const calculateAchievements = () => {
  // 初次体验：完成首次预约
  if (stats.completedCount && stats.completedCount > 0) {
    achievements.value[0].progress = 100
    achievements.value[0].unlocked = true
  } else if (stats.reservationCount > 0) {
    achievements.value[0].progress = 50 // 有预约但未完成
  }
  
  // 剧本达人：体验10个剧本
  const completedCount = stats.completedCount || 0
  achievements.value[1].progress = Math.min(Math.round((completedCount / 10) * 100), 100)
  achievements.value[1].unlocked = completedCount >= 10
  
  // 社交达人：邀请5位好友（基于完成数估算）
  const socialProgress = Math.min(Math.round((completedCount / 15) * 100), 100)
  achievements.value[2].progress = socialProgress
  achievements.value[2].unlocked = completedCount >= 15
  
  // 评论家：发表20条评价
  achievements.value[3].progress = Math.min(Math.round((stats.reviewCount / 20) * 100), 100)
  achievements.value[3].unlocked = stats.reviewCount >= 20
  
  // 收藏家：收藏50个剧本
  achievements.value[4].progress = Math.min(Math.round((stats.favoriteCount / 50) * 100), 100)
  achievements.value[4].unlocked = stats.favoriteCount >= 50
  
  // 积分大户：累计获得1000积分
  const earnedPoints = stats.totalEarned || stats.points
  achievements.value[5].progress = Math.min(Math.round((earnedPoints / 1000) * 100), 100)
  achievements.value[5].unlocked = earnedPoints >= 1000
}

// 监听时间段切换
const handlePeriodChange = async () => {
  await initChart()
}

// 监听chartPeriod变化
watch(chartPeriod, async () => {
  if (chartInstance) {
    await initChart()
  }
})

let resizeHandler = null

onMounted(() => {
  loadStats()

  // 窗口大小变化时重新渲染图表
  resizeHandler = () => {
    if (chartInstance) chartInstance.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
  text-align: center;
}

.header-card h2 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 0 0 10px 0;
  font-size: 24px;
}

.header-card p {
  color: #909399;
  margin: 0;
}

/* 统计卡片 */
.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 10px;
}

.stat-icon-wrapper {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(64, 158, 255, 0.1);
  flex-shrink: 0;
}

.stat-primary .stat-icon-wrapper { background: rgba(64, 158, 255, 0.1); }
.stat-success .stat-icon-wrapper { background: rgba(103, 194, 58, 0.1); }
.stat-warning .stat-icon-wrapper { background: rgba(230, 162, 60, 0.1); }
.stat-danger .stat-icon-wrapper { background: rgba(245, 108, 108, 0.1); }
.stat-info .stat-icon-wrapper { background: rgba(144, 147, 153, 0.1); }
.stat-purple .stat-icon-wrapper { background: rgba(155, 89, 182, 0.1); }

.stat-icon {
  opacity: 0.9;
}

.stat-primary .stat-icon { color: #409eff; }
.stat-success .stat-icon { color: #67c23a; }
.stat-warning .stat-icon { color: #e6a23c; }
.stat-danger .stat-icon { color: #f56c6c; }
.stat-info .stat-icon { color: #909399; }
.stat-purple .stat-icon { color: #9b59b6; }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-value.money {
  font-size: 24px;
  color: #f56c6c;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 2px;
  font-weight: 500;
}

.stat-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

/* 图表卡片 */
.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 400px;
  width: 100%;
}

/* 表格卡片 */
.table-card {
  margin-bottom: 20px;
}

.amount {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

/* 成就卡片 */
.achievements-card {
  margin-bottom: 20px;
}

.achievement-item {
  padding: 20px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  text-align: center;
  transition: all 0.3s;
  margin-bottom: 20px;
  opacity: 0.6;
}

.achievement-item.unlocked {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4f8 100%);
  opacity: 1;
}

.achievement-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.achievement-icon {
  color: #909399;
  margin-bottom: 10px;
}

.achievement-item.unlocked .achievement-icon {
  color: #67c23a;
}

.achievement-info h4 {
  margin: 10px 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.achievement-info p {
  font-size: 14px;
  color: #909399;
  margin: 0 0 10px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .statistics-container {
    padding: 10px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .chart-container {
    height: 300px;
  }
}
</style>



