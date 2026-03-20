<template>
  <div class="history-page">
    <!-- 神秘背景 -->
    <div class="mystery-bg">
      <div class="particles">
        <span v-for="n in 12" :key="n" class="particle"></span>
      </div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <span class="title-icon">🕐</span>
          <div class="title-text">
            <h1>浏览历史</h1>
            <p>追溯你的推理足迹，发现曾经心动的剧本</p>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="handleClearAll" :disabled="historyList.length === 0">
            <span class="btn-icon">🗑️</span>
            <span>清空历史</span>
          </button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-bar" v-if="stats.totalCount > 0">
        <div class="stat-item">
          <span class="stat-icon">📊</span>
          <span class="stat-label">总浏览</span>
          <span class="stat-value">{{ stats.totalCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">🎭</span>
          <span class="stat-label">剧本</span>
          <span class="stat-value">{{ stats.scriptCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">🏠</span>
          <span class="stat-label">门店</span>
          <span class="stat-value">{{ stats.storeCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">📅</span>
          <span class="stat-label">今日</span>
          <span class="stat-value">{{ stats.todayCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">📆</span>
          <span class="stat-label">本周</span>
          <span class="stat-value">{{ stats.weekCount }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <!-- 时间筛选 -->
      <div class="time-filter">
        <button 
          v-for="filter in timeFilters" 
          :key="filter.value"
          :class="['filter-btn', { active: currentFilter === filter.value }]"
          @click="changeFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <!-- 类型筛选 -->
      <div class="type-filter">
        <button 
          v-for="typeItem in typeFilters" 
          :key="typeItem.value"
          :class="['type-btn', { active: currentType === typeItem.value }]"
          @click="changeType(typeItem.value)"
        >
          <span class="type-icon">{{ typeItem.icon }}</span>
          {{ typeItem.label }}
        </button>
      </div>
    </div>

    <!-- 历史列表 -->
    <div class="content-area" v-loading="loading" element-loading-text="加载历史记录...">
      <!-- 按日期分组 -->
      <div v-for="(group, date) in groupedHistory" :key="date" class="history-group">
        <div class="group-header">
          <span class="date-icon">📅</span>
          <span class="date-text">{{ formatDateHeader(date) }}</span>
          <span class="count-badge">{{ group.length }} 条记录</span>
        </div>
        <div class="history-list">
          <div 
            v-for="(item, index) in group" 
            :key="item.id"
            class="history-item"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <div class="item-cover" @click="goToDetail(item)">
              <img :src="item.cover || '/default-script.jpg'" :alt="item.name" />
              <div class="cover-overlay">
                <span class="view-icon">🔍</span>
              </div>
              <span class="item-type" :class="getTypeClass(item.targetType)">
                {{ getTypeName(item.targetType) }}
              </span>
            </div>
            <div class="item-content">
              <h4 class="item-title" @click="goToDetail(item)">{{ item.name }}</h4>
              <div class="item-meta">
                <span v-if="item.categoryName" class="meta-tag">{{ item.categoryName }}</span>
                <span v-if="item.playerCount" class="meta-info">👥 {{ item.playerCount }}人</span>
                <span v-if="item.price" class="meta-price">¥{{ item.price }}</span>
              </div>
              <div class="item-time">
                <span class="time-icon">🕐</span>
                {{ formatTime(getBrowseTime(item)) }}
              </div>
            </div>
            <div class="item-actions">
              <button class="btn-detail" @click="goToDetail(item)">
                查看详情
              </button>
              <button class="btn-delete" @click="handleDelete(item)">
                <span>🗑️</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && historyList.length === 0" class="empty-state">
        <div class="empty-illustration">
          <span class="empty-icon">🔍</span>
          <div class="empty-particles">
            <span>✨</span>
            <span>✨</span>
            <span>✨</span>
          </div>
        </div>
        <h3>还没有浏览记录</h3>
        <p>快去探索精彩的剧本世界吧！</p>
        <button class="btn-explore" @click="$router.push('/script')">
          🎭 探索剧本
        </button>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && historyList.length > 0" class="load-more">
        <button class="btn-load-more" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

    <!-- 回到顶部 -->
    <el-backtop :right="40" :bottom="40">
      <div class="backtop-btn">🔝</div>
    </el-backtop>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getBrowseHistory, 
  deleteBrowseHistory, 
  clearBrowseHistory, 
  getBrowseHistoryStats,
  localHistoryUtils 
} from '@/api/history'

const router = useRouter()
const loading = ref(false)
const loadingMore = ref(false)
const historyList = ref([])
const currentFilter = ref('all')
const currentType = ref(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 统计数据
const stats = reactive({
  totalCount: 0,
  scriptCount: 0,
  storeCount: 0,
  todayCount: 0,
  weekCount: 0
})

const timeFilters = [
  { value: 'all', label: '全部' },
  { value: 'today', label: '今天' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' }
]

const typeFilters = [
  { value: null, label: '全部', icon: '📋' },
  { value: 1, label: '剧本', icon: '🎭' },
  { value: 2, label: '门店', icon: '🏠' }
]

// 是否还有更多数据
const hasMore = computed(() => {
  return historyList.value.length < total.value
})

// 按日期分组
const groupedHistory = computed(() => {
  const groups = {}
  historyList.value.forEach(item => {
    // 兼容 browseTime 和 browseTimeStr 两种格式
    const timeStr = item.browseTimeStr || item.browseTime
    const date = timeStr ? timeStr.split(' ')[0] : '未知日期'
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(item)
  })
  return groups
})

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getBrowseHistoryStats()
    if (res.code === 1 || res.code === 200) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取浏览历史
const loadHistory = async (reset = true) => {
  if (reset) {
    loading.value = true
    page.value = 1
    historyList.value = []
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }

    // 根据时间筛选添加参数
    if (currentFilter.value === 'today') {
      params.days = 1
    } else if (currentFilter.value === 'week') {
      params.days = 7
    } else if (currentFilter.value === 'month') {
      params.days = 30
    }

    // 类型筛选
    if (currentType.value !== null) {
      params.targetType = currentType.value
    }

    const res = await getBrowseHistory(params)

    if (res.code === 1 || res.code === 200) {
      const records = res.data?.records || res.data || []
      if (reset) {
        historyList.value = records
      } else {
        historyList.value.push(...records)
      }
      total.value = res.data?.total || records.length
    }
  } catch (error) {
    console.error('加载浏览历史失败:', error)
    // 如果API不存在，使用本地存储的历史记录
    loadLocalHistory()
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 从本地存储加载历史（备用方案）
const loadLocalHistory = () => {
  try {
    const history = localHistoryUtils.getLocalHistory()
    if (history.length > 0) {
      // 根据筛选条件过滤
      let filtered = history
      
      // 时间筛选
      if (currentFilter.value !== 'all') {
        const now = new Date()
        let startTime
        if (currentFilter.value === 'today') {
          startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate())
        } else if (currentFilter.value === 'week') {
          startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
        } else if (currentFilter.value === 'month') {
          startTime = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
        }
        if (startTime) {
          filtered = filtered.filter(item => new Date(item.browseTime) >= startTime)
        }
      }
      
      // 类型筛选
      if (currentType.value !== null) {
        filtered = filtered.filter(item => item.targetType === currentType.value)
      }
      
      historyList.value = filtered
      total.value = filtered.length
    }
  } catch (e) {
    console.error('加载本地历史失败:', e)
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  loadHistory(false)
}

// 切换时间筛选
const changeFilter = (filter) => {
  currentFilter.value = filter
  loadHistory(true)
}

// 切换类型筛选
const changeType = (type) => {
  currentType.value = type
  loadHistory(true)
}

// 删除单条记录
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除这条浏览记录吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调用API删除
    try {
      await deleteBrowseHistory(item.id)
    } catch (e) {
      // API可能不存在，继续执行本地删除
    }

    // 从列表中移除
    const index = historyList.value.findIndex(h => h.id === item.id)
    if (index !== -1) {
      historyList.value.splice(index, 1)
      total.value--
    }

    // 同时从本地存储移除
    localHistoryUtils.removeLocalHistory(item.id)

    // 更新统计
    if (stats.totalCount > 0) {
      stats.totalCount--
      if (item.targetType === 1 && stats.scriptCount > 0) {
        stats.scriptCount--
      } else if (item.targetType === 2 && stats.storeCount > 0) {
        stats.storeCount--
      }
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 清空所有历史
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有浏览历史吗？此操作不可恢复。', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调用API清空
    try {
      await clearBrowseHistory()
    } catch (e) {
      // API可能不存在，继续执行本地清空
    }

    // 清空列表
    historyList.value = []
    total.value = 0

    // 清空本地存储
    localHistoryUtils.clearLocalHistory()

    // 重置统计
    Object.assign(stats, {
      totalCount: 0,
      scriptCount: 0,
      storeCount: 0,
      todayCount: 0,
      weekCount: 0
    })

    ElMessage.success('已清空浏览历史')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  }
}

// 跳转到详情页
const goToDetail = (item) => {
  if (item.targetType === 1 || item.targetType === 'script') {
    router.push(`/script/${item.targetId || item.id}`)
  } else if (item.targetType === 2 || item.targetType === 'store') {
    router.push(`/store/${item.targetId || item.id}`)
  } else {
    router.push(`/script/${item.targetId || item.id}`)
  }
}

// 获取类型名称
const getTypeName = (type) => {
  const names = {
    1: '剧本',
    2: '门店',
    'script': '剧本',
    'store': '门店'
  }
  return names[type] || '剧本'
}

// 获取类型样式类
const getTypeClass = (type) => {
  if (type === 1 || type === 'script') return 'type-script'
  if (type === 2 || type === 'store') return 'type-store'
  return 'type-script'
}

// 格式化日期头
const formatDateHeader = (dateStr) => {
  const today = new Date().toISOString().split('T')[0]
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0]
  
  if (dateStr === today) return '今天'
  if (dateStr === yesterday) return '昨天'
  
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  // 兼容 browseTimeStr 格式
  const date = new Date(timeStr.replace(' ', 'T'))
  if (isNaN(date.getTime())) return ''
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 获取浏览时间（兼容不同字段）
const getBrowseTime = (item) => {
  return item.browseTimeStr || item.browseTime || ''
}

onMounted(() => {
  loadHistory()
  loadStats()
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.history-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 20px;
  position: relative;
  overflow-x: hidden;
}

/* ========== 神秘背景 ========== */
.mystery-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 215, 0, 0.3);
  border-radius: 50%;
  animation: particleFloat 15s infinite;
}

.particle:nth-child(1) { top: 10%; left: 20%; animation-delay: 0s; }
.particle:nth-child(2) { top: 20%; left: 80%; animation-delay: 1s; }
.particle:nth-child(3) { top: 30%; left: 40%; animation-delay: 2s; }
.particle:nth-child(4) { top: 40%; left: 60%; animation-delay: 3s; }
.particle:nth-child(5) { top: 50%; left: 10%; animation-delay: 4s; }
.particle:nth-child(6) { top: 60%; left: 90%; animation-delay: 5s; }
.particle:nth-child(7) { top: 70%; left: 30%; animation-delay: 6s; }
.particle:nth-child(8) { top: 80%; left: 70%; animation-delay: 7s; }
.particle:nth-child(9) { top: 15%; left: 50%; animation-delay: 8s; }
.particle:nth-child(10) { top: 85%; left: 25%; animation-delay: 9s; }
.particle:nth-child(11) { top: 45%; left: 85%; animation-delay: 10s; }
.particle:nth-child(12) { top: 75%; left: 15%; animation-delay: 11s; }

@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.3; }
  50% { transform: translateY(-30px) scale(1.5); opacity: 0.8; }
}

/* ========== 页面头部 ========== */
.page-header {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  font-size: 48px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.title-text h1 {
  margin: 0;
  font-size: 28px;
  color: #fff;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
}

.title-text p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid rgba(255, 71, 87, 0.5);
  background: rgba(255, 71, 87, 0.1);
  color: #ff6b6b;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
  font-size: 14px;
}

.action-btn:hover:not(:disabled) {
  background: rgba(255, 71, 87, 0.2);
  border-color: #ff6b6b;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 统计栏 ========== */
.stats-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s;
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 16px;
}

.stat-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.stat-value {
  color: #ffd700;
  font-weight: bold;
  font-size: 15px;
}

/* ========== 筛选栏 ========== */
.filter-bar {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

/* ========== 时间筛选 ========== */
.time-filter {
  display: flex;
  gap: 12px;
}

.filter-btn {
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.filter-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.filter-btn.active {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-color: transparent;
  color: #fff;
}

/* ========== 类型筛选 ========== */
.type-filter {
  display: flex;
  gap: 10px;
}

.type-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
}

.type-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.type-btn.active {
  background: linear-gradient(135deg, #2ed573 0%, #17c964 100%);
  border-color: transparent;
  color: #fff;
}

.type-icon {
  font-size: 14px;
}

/* ========== 内容区域 ========== */
.content-area {
  position: relative;
  z-index: 1;
  min-height: 400px;
}

.content-area :deep(.el-loading-mask) {
  background: rgba(26, 26, 46, 0.8);
}

/* ========== 历史分组 ========== */
.history-group {
  margin-bottom: 24px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.date-icon {
  font-size: 20px;
}

.date-text {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.count-badge {
  background: rgba(192, 57, 43, 0.2);
  color: #a0b4ff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

/* ========== 历史列表 ========== */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
  animation: itemAppear 0.3s ease-out backwards;
}

@keyframes itemAppear {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(192, 57, 43, 0.3);
}

/* 封面 */
.item-cover {
  position: relative;
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.history-item:hover .item-cover img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.item-cover:hover .cover-overlay {
  opacity: 1;
}

.view-icon {
  font-size: 24px;
}

.item-type {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

.type-script {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
}

.type-store {
  background: linear-gradient(135deg, #2ed573 0%, #17c964 100%);
  color: #fff;
}

/* 内容 */
.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #fff;
  cursor: pointer;
  transition: color 0.3s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-title:hover {
  color: #ffd700;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.meta-tag {
  background: rgba(192, 57, 43, 0.2);
  color: #a0b4ff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.meta-info {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.meta-price {
  color: #ff6b6b;
  font-weight: bold;
  font-size: 14px;
}

.item-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

.time-icon {
  font-size: 14px;
}

/* 操作按钮 */
.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-detail {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-detail:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.4);
}

.btn-delete {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  width: 36px;
  height: 36px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.btn-delete:hover {
  background: rgba(255, 71, 87, 0.2);
  border-color: rgba(255, 71, 87, 0.5);
  color: #ff6b6b;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-illustration {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 64px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.empty-particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.empty-particles span {
  position: absolute;
  font-size: 16px;
  opacity: 0;
  animation: sparkle 2s infinite;
}

.empty-particles span:nth-child(1) { top: 10%; left: 20%; animation-delay: 0s; }
.empty-particles span:nth-child(2) { top: 30%; right: 10%; animation-delay: 0.5s; }
.empty-particles span:nth-child(3) { bottom: 20%; left: 30%; animation-delay: 1s; }

@keyframes sparkle {
  0%, 100% { opacity: 0; transform: scale(0.5); }
  50% { opacity: 1; transform: scale(1); }
}

.empty-state h3 {
  color: #fff;
  font-size: 20px;
  margin: 0 0 12px;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 24px;
}

.btn-explore {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
  border: none;
  padding: 12px 32px;
  border-radius: 24px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-explore:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.4);
}

/* ========== 加载更多 ========== */
.load-more {
  text-align: center;
  padding: 24px;
}

.btn-load-more {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 12px 48px;
  border-radius: 24px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-load-more:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.btn-load-more:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 回到顶部 ========== */
.backtop-btn {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.4);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .history-page {
    padding: 12px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .title-icon {
    font-size: 36px;
  }

  .title-text h1 {
    font-size: 22px;
  }

  .stats-bar {
    gap: 10px;
  }

  .stat-item {
    padding: 6px 12px;
    font-size: 12px;
  }

  .stat-value {
    font-size: 13px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .time-filter {
    overflow-x: auto;
    padding-bottom: 8px;
    width: 100%;
  }

  .type-filter {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .filter-btn,
  .type-btn {
    white-space: nowrap;
  }

  .history-item {
    flex-direction: column;
    align-items: stretch;
  }

  .item-cover {
    width: 100%;
    height: 150px;
  }

  .item-actions {
    justify-content: space-between;
  }

  .btn-detail {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .stats-bar {
    justify-content: space-between;
  }

  .stat-item {
    flex: 1;
    min-width: calc(33% - 8px);
    justify-content: center;
  }

  .stat-label {
    display: none;
  }
}
</style>
