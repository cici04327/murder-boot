<template>
  <div class="favorites-page">
    <!-- 神秘背景装饰 -->
    <div class="mystery-bg">
      <div class="fog fog-1"></div>
      <div class="fog fog-2"></div>
      <div class="floating-icons">
        <span class="icon-float" v-for="n in 6" :key="n">🔍</span>
      </div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <div class="title-icon">📜</div>
          <div class="title-text">
            <h1>我的剧本收藏</h1>
            <p class="subtitle">
              <span class="clue-icon">🔎</span>
              已收集 <span class="highlight">{{ total }}</span> 个神秘剧本，等待你来揭开真相
            </p>
          </div>
        </div>
        
        <div class="header-actions">
          <!-- 视图切换 -->
          <div class="view-switcher">
            <button 
              :class="['switch-btn', { active: viewMode === 'card' }]"
              @click="viewMode = 'card'"
            >
              <span class="btn-icon">🎴</span>
              <span>卡片</span>
            </button>
            <button 
              :class="['switch-btn', { active: viewMode === 'list' }]"
              @click="viewMode = 'list'"
            >
              <span class="btn-icon">📋</span>
              <span>列表</span>
            </button>
          </div>

          <button class="action-btn refresh-btn" @click="handleRefresh" :disabled="loading">
            <span class="btn-icon" :class="{ 'spinning': loading }">🔄</span>
            <span>刷新线索</span>
          </button>

          <button class="action-btn filter-btn" @click="showFilterPanel = !showFilterPanel">
            <span class="btn-icon">🎯</span>
            <span>筛选条件</span>
            <span v-if="activeFiltersCount > 0" class="filter-badge">{{ activeFiltersCount }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选面板 -->
    <transition name="slide-fade">
      <div class="filter-panel" v-show="showFilterPanel">
        <div class="panel-header">
          <span class="panel-icon">🎭</span>
          <span>剧本筛选器</span>
        </div>
        <div class="panel-body">
          <div class="filter-grid">
            <!-- 剧本类型 -->
            <div class="filter-item">
              <label><span class="label-icon">📁</span> 剧本类型</label>
              <el-select v-model="filters.category" placeholder="选择类型" clearable>
                <el-option label="全部类型" value="" />
                <el-option 
                  v-for="cat in categories" 
                  :key="cat.id" 
                  :label="cat.icon + ' ' + cat.name" 
                  :value="cat.id" 
                />
              </el-select>
            </div>

            <!-- 玩家人数 -->
            <div class="filter-item">
              <label><span class="label-icon">👥</span> 玩家人数</label>
              <el-select v-model="filters.playerCount" placeholder="选择人数" clearable>
                <el-option label="全部人数" value="" />
                <el-option label="4-6人 小型本" value="4-6" />
                <el-option label="6-8人 中型本" value="6-8" />
                <el-option label="8-10人 大型本" value="8-10" />
                <el-option label="10人以上 超大本" value="10+" />
              </el-select>
            </div>

            <!-- 难度等级 -->
            <div class="filter-item">
              <label><span class="label-icon">⚡</span> 推理难度</label>
              <el-select v-model="filters.difficulty" placeholder="选择难度" clearable>
                <el-option label="全部难度" value="" />
                <el-option label="🌟 新手入门" value="简单" />
                <el-option label="🌟🌟 进阶推理" value="中等" />
                <el-option label="🌟🌟🌟 烧脑挑战" value="困难" />
              </el-select>
            </div>

            <!-- 价格区间 -->
            <div class="filter-item price-filter">
              <label><span class="label-icon">💰</span> 价格区间</label>
              <el-slider 
                v-model="filters.priceRange" 
                range 
                :min="0" 
                :max="500" 
                :step="10"
                :format-tooltip="(val) => `¥${val}`"
              />
              <div class="price-labels">
                <span>¥{{ filters.priceRange[0] }}</span>
                <span>¥{{ filters.priceRange[1] }}</span>
              </div>
            </div>

            <!-- 最低评分 -->
            <div class="filter-item">
              <label><span class="label-icon">⭐</span> 最低评分</label>
              <el-rate v-model="filters.minRating" show-text :texts="['较差', '一般', '不错', '推荐', '必玩']" />
            </div>

            <!-- 收藏时间 -->
            <div class="filter-item">
              <label><span class="label-icon">📅</span> 收藏时间</label>
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </div>
          </div>

          <div class="filter-actions">
            <button class="btn-apply" @click="applyFilters">
              <span>🔍</span> 开始搜索
            </button>
            <button class="btn-reset" @click="resetFilters">
              <span>🔄</span> 重置条件
            </button>
            <button class="btn-save" @click="saveFilters">
              <span>💾</span> 保存偏好
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 活跃筛选标签 -->
    <div class="active-filters" v-if="activeFiltersCount > 0">
      <span class="filters-label">🏷️ 当前筛选：</span>
      <div class="filter-tags">
        <span 
          v-for="(tag, index) in activeFilterTags" 
          :key="index"
          class="filter-tag"
        >
          {{ tag.label }}: {{ tag.value }}
          <button class="tag-close" @click="removeFilter(tag.key)">×</button>
        </span>
      </div>
      <button class="clear-all" @click="resetFilters">清空全部</button>
    </div>

    <!-- 排序栏 -->
    <div class="sort-bar">
      <span class="sort-label">📊 排序方式：</span>
      <div class="sort-options">
        <button 
          v-for="option in sortOptions" 
          :key="option.value"
          :class="['sort-btn', { active: sortBy === option.value }]"
          @click="changeSortBy(option.value)"
        >
          <span>{{ option.icon }}</span>
          <span>{{ option.label }}</span>
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area" v-loading="loading" element-loading-text="正在搜集线索...">
      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="card-grid">
        <div 
          v-for="(script, index) in scriptList" 
          :key="script.id"
          class="script-card"
          :style="{ animationDelay: `${index * 0.1}s` }"
        >
          <!-- 卡片封面 -->
          <div class="card-cover" @click="goToDetail(script.id)">
            <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
            <div class="cover-overlay">
              <div class="overlay-content">
                <span class="view-icon">🔍</span>
                <span>查看详情</span>
              </div>
            </div>
            
            <!-- 角标 -->
            <div class="card-badges">
              <span v-if="script.isHot" class="badge hot">🔥 热门</span>
              <span v-if="script.isNew" class="badge new">✨ 新品</span>
            </div>

            <!-- 类型标识 -->
            <div class="category-ribbon">
              {{ getCategoryIcon(script.categoryName) }} {{ script.categoryName || '未分类' }}
            </div>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <h3 class="card-title" @click="goToDetail(script.id)">
              {{ script.name }}
            </h3>

            <!-- 剧本属性 -->
            <div class="script-attrs">
              <span class="attr">
                <span class="attr-icon">👥</span>
                {{ script.playerCount }}人
              </span>
              <span class="attr">
                <span class="attr-icon">⏱️</span>
                {{ script.duration }}小时
              </span>
              <span class="attr" :class="getDifficultyClass(script.difficulty)">
                <span class="attr-icon">⚡</span>
                {{ script.difficulty || '未知' }}
              </span>
            </div>

            <!-- 评分 -->
            <div class="card-rating">
              <div class="stars">
                <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= Math.round(script.rating || 0) }">★</span>
              </div>
              <span class="rating-score">{{ (script.rating || 0).toFixed(1) }}</span>
              <span class="review-count">({{ script.reviewCount || 0 }}条评价)</span>
            </div>

            <!-- 价格和操作 -->
            <div class="card-footer">
              <div class="price-section">
                <span class="price">¥{{ script.price }}</span>
                <span class="price-unit">/人</span>
              </div>
              <div class="action-buttons">
                <button class="btn-reserve" @click="handleReserve(script.id)">
                  🎫 立即预约
                </button>
                <button class="btn-unfavorite" @click="handleUnfavorite(script.id)">
                  💔
                </button>
              </div>
            </div>

            <!-- 收藏时间 -->
            <div class="card-meta">
              <span class="meta-icon">📌</span>
              收藏于 {{ formatDate(script.favoriteTime) }}
            </div>
          </div>

          <!-- 神秘装饰 -->
          <div class="card-decoration">
            <div class="corner top-left"></div>
            <div class="corner top-right"></div>
            <div class="corner bottom-left"></div>
            <div class="corner bottom-right"></div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <div class="list-header">
          <span class="col-cover">封面</span>
          <span class="col-name">剧本名称</span>
          <span class="col-info">信息</span>
          <span class="col-rating">评分</span>
          <span class="col-price">价格</span>
          <span class="col-time">收藏时间</span>
          <span class="col-actions">操作</span>
        </div>
        <div 
          v-for="(script, index) in scriptList" 
          :key="script.id"
          class="list-item"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="col-cover">
            <img :src="script.cover || '/default-script.jpg'" @click="goToDetail(script.id)" />
          </div>
          <div class="col-name" @click="goToDetail(script.id)">
            <span class="name">{{ script.name }}</span>
            <span class="category">{{ getCategoryIcon(script.categoryName) }} {{ script.categoryName }}</span>
          </div>
          <div class="col-info">
            <span>👥 {{ script.playerCount }}人</span>
            <span>⏱️ {{ script.duration }}h</span>
          </div>
          <div class="col-rating">
            <span class="stars-mini">★</span>
            <span>{{ (script.rating || 0).toFixed(1) }}</span>
          </div>
          <div class="col-price">¥{{ script.price }}/人</div>
          <div class="col-time">{{ formatDate(script.favoriteTime) }}</div>
          <div class="col-actions">
            <button class="btn-sm reserve" @click="handleReserve(script.id)">🎫 预约</button>
            <button class="btn-sm remove" @click="handleUnfavorite(script.id)">💔 取消</button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="scriptList.length === 0 && !loading" class="empty-state">
        <div class="empty-illustration">
          <div class="magnifier">🔍</div>
          <div class="question-marks">
            <span>❓</span>
            <span>❓</span>
            <span>❓</span>
          </div>
        </div>
        <h3>线索库空空如也...</h3>
        <p>还没有收藏任何剧本，快去发现精彩的推理故事吧！</p>
        <button class="btn-explore" @click="$router.push('/script')">
          🎭 探索剧本世界
        </button>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
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
import { getFavoriteScripts, unfavoriteScript } from '@/api/script'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const scriptList = ref([])
const total = ref(0)
const viewMode = ref('card')
const sortBy = ref('latest')
const showFilterPanel = ref(false)

// 剧本分类（带图标）
const categories = ref([
  { id: 1, name: '推理', icon: '🔍' },
  { id: 2, name: '恐怖', icon: '👻' },
  { id: 3, name: '情感', icon: '💕' },
  { id: 4, name: '欢乐', icon: '😄' },
  { id: 5, name: '机制', icon: '⚙️' },
  { id: 6, name: '古风', icon: '🏯' },
  { id: 7, name: '悬疑', icon: '🎭' },
])

// 排序选项
const sortOptions = [
  { value: 'latest', label: '最新收藏', icon: '🕐' },
  { value: 'rating', label: '评分最高', icon: '⭐' },
  { value: 'price-asc', label: '价格升序', icon: '💰' },
  { value: 'price-desc', label: '价格降序', icon: '💎' },
  { value: 'popular', label: '最受欢迎', icon: '🔥' },
]

const queryParams = reactive({
  page: 1,
  pageSize: 12
})

const filters = reactive({
  category: '',
  playerCount: '',
  difficulty: '',
  priceRange: [0, 500],
  minRating: 0,
  dateRange: null
})

// 获取分类图标
const getCategoryIcon = (categoryName) => {
  const cat = categories.value.find(c => c.name === categoryName)
  return cat?.icon || '📜'
}

// 获取难度样式类
const getDifficultyClass = (difficulty) => {
  const map = { '简单': 'easy', '中等': 'medium', '困难': 'hard' }
  return map[difficulty] || ''
}

// 活跃的筛选条件数量
const activeFiltersCount = computed(() => {
  let count = 0
  if (filters.category) count++
  if (filters.playerCount) count++
  if (filters.difficulty) count++
  if (filters.minRating > 0) count++
  if (filters.dateRange && filters.dateRange.length === 2) count++
  if (filters.priceRange[0] > 0 || filters.priceRange[1] < 500) count++
  return count
})

// 活跃的筛选标签
const activeFilterTags = computed(() => {
  const tags = []
  if (filters.category) {
    const cat = categories.value.find(c => c.id === filters.category)
    tags.push({ key: 'category', label: '类型', value: cat?.name || filters.category })
  }
  if (filters.playerCount) {
    tags.push({ key: 'playerCount', label: '人数', value: filters.playerCount })
  }
  if (filters.difficulty) {
    tags.push({ key: 'difficulty', label: '难度', value: filters.difficulty })
  }
  if (filters.minRating > 0) {
    tags.push({ key: 'minRating', label: '评分', value: `≥${filters.minRating}星` })
  }
  if (filters.priceRange[0] > 0 || filters.priceRange[1] < 500) {
    tags.push({ 
      key: 'priceRange', 
      label: '价格', 
      value: `¥${filters.priceRange[0]}-¥${filters.priceRange[1]}` 
    })
  }
  return tags
})

// 加载收藏列表
const loadFavorites = async () => {
  loading.value = true
  try {
    const params = {
      page: queryParams.page,
      pageSize: queryParams.pageSize
    }
    
    const res = await getFavoriteScripts(params)
    
    if (res.code === 1 || res.code === 200) {
      if (res.data) {
        let list = res.data.records || []
        
        // 前端筛选
        if (filters.category) {
          list = list.filter(s => s.categoryId === filters.category)
        }
        if (filters.playerCount) {
          const range = filters.playerCount.split('-')
          if (range.length === 2) {
            const min = parseInt(range[0])
            const max = range[1] === '+' ? 999 : parseInt(range[1])
            list = list.filter(s => s.playerCount >= min && s.playerCount <= max)
          }
        }
        if (filters.difficulty) {
          list = list.filter(s => s.difficulty === filters.difficulty)
        }
        if (filters.minRating > 0) {
          list = list.filter(s => (s.rating || 0) >= filters.minRating)
        }
        if (filters.priceRange[0] > 0 || filters.priceRange[1] < 500) {
          list = list.filter(s => s.price >= filters.priceRange[0] && s.price <= filters.priceRange[1])
        }
        
        // 前端排序
        if (sortBy.value === 'rating') {
          list.sort((a, b) => (b.rating || 0) - (a.rating || 0))
        } else if (sortBy.value === 'price-asc') {
          list.sort((a, b) => a.price - b.price)
        } else if (sortBy.value === 'price-desc') {
          list.sort((a, b) => b.price - a.price)
        }
        
        scriptList.value = list
        // 如果有前端筛选条件，用筛选后的数量；否则用后端返回的总数
        const hasFilter = filters.category || filters.playerCount || filters.difficulty ||
          filters.minRating > 0 || (filters.priceRange[0] > 0 || filters.priceRange[1] < 500)
        total.value = hasFilter ? list.length : (res.data.total || list.length)
      } else {
        scriptList.value = []
        total.value = 0
      }
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 切换排序
const changeSortBy = (value) => {
  sortBy.value = value
  loadFavorites()
}

// 应用筛选
const applyFilters = () => {
  queryParams.page = 1
  loadFavorites()
  showFilterPanel.value = false
  ElMessage.success('筛选条件已应用')
}

// 重置筛选
const resetFilters = () => {
  filters.category = ''
  filters.playerCount = ''
  filters.difficulty = ''
  filters.priceRange = [0, 500]
  filters.minRating = 0
  filters.dateRange = null
  queryParams.page = 1
  loadFavorites()
}

// 移除单个筛选条件
const removeFilter = (key) => {
  if (key === 'priceRange') {
    filters[key] = [0, 500]
  } else if (key === 'minRating') {
    filters[key] = 0
  } else {
    filters[key] = ''
  }
  applyFilters()
}

// 保存筛选条件
const saveFilters = () => {
  localStorage.setItem('favorites_filters', JSON.stringify(filters))
  ElMessage.success('筛选偏好已保存')
}

// 加载保存的筛选条件
const loadSavedFilters = () => {
  const saved = localStorage.getItem('favorites_filters')
  if (saved) {
    try {
      const savedFilters = JSON.parse(saved)
      Object.assign(filters, savedFilters)
    } catch (e) {
      console.error('加载保存的筛选条件失败:', e)
    }
  }
}

// 刷新
const handleRefresh = () => {
  loadFavorites()
  ElMessage.success('线索已更新')
}

// 跳转到详情页
const goToDetail = (id) => {
  router.push(`/script/${id}`)
}

// 预约剧本
const handleReserve = (scriptId) => {
  router.push({
    path: '/reservation/schedule',
    query: { scriptId }
  })
}

// 取消收藏
const handleUnfavorite = async (scriptId) => {
  try {
    await ElMessageBox.confirm('确定要将这个剧本从收藏中移除吗？', '移除收藏', {
      confirmButtonText: '确定移除',
      cancelButtonText: '再想想',
      type: 'warning'
    })
    
    await unfavoriteScript(scriptId)
    ElMessage.success('已从收藏中移除')
    loadFavorites()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 分页变化
const handlePageChange = (newPage) => {
  queryParams.page = newPage
  loadFavorites()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleSizeChange = (newSize) => {
  queryParams.pageSize = newSize
  queryParams.page = 1
  loadFavorites()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadSavedFilters()
  loadFavorites()
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.favorites-page {
  min-height: calc(100vh - 64px - 100px);
  background: transparent;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* ========== 神秘背景装饰 ========== */
.mystery-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.fog {
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at center, rgba(255,255,255,0.03) 0%, transparent 70%);
  animation: fogMove 20s infinite linear;
}

.fog-1 { top: -50%; left: -50%; }
.fog-2 { top: -30%; left: -30%; animation-delay: -10s; animation-direction: reverse; }

@keyframes fogMove {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(50px, 30px) rotate(360deg); }
}

.floating-icons {
  position: absolute;
  width: 100%;
  height: 100%;
}

.icon-float {
  position: absolute;
  font-size: 24px;
  opacity: 0.1;
  animation: floatIcon 15s infinite ease-in-out;
}

.icon-float:nth-child(1) { top: 10%; left: 10%; animation-delay: 0s; }
.icon-float:nth-child(2) { top: 20%; left: 80%; animation-delay: -3s; }
.icon-float:nth-child(3) { top: 60%; left: 15%; animation-delay: -6s; }
.icon-float:nth-child(4) { top: 70%; left: 70%; animation-delay: -9s; }
.icon-float:nth-child(5) { top: 40%; left: 50%; animation-delay: -12s; }
.icon-float:nth-child(6) { top: 85%; left: 30%; animation-delay: -15s; }

@keyframes floatIcon {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
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
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
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

.subtitle {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.clue-icon {
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.highlight {
  color: #ffd700;
  font-weight: bold;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 视图切换 */
.view-switcher {
  display: flex;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 4px;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 14px;
}

.switch-btn:hover {
  color: #fff;
}

.switch-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

/* 操作按钮 */
.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
  font-size: 14px;
  position: relative;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 16px;
}

.btn-icon.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.filter-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ff4757;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

/* ========== 筛选面板 ========== */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.filter-panel {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.panel-header {
  padding: 16px 24px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 16px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-icon {
  font-size: 20px;
}

.panel-body {
  padding: 24px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.label-icon {
  font-size: 16px;
}

.filter-item :deep(.el-select),
.filter-item :deep(.el-date-picker) {
  width: 100%;
}

.filter-item :deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: none !important;
}

.filter-item :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.4);
}

.filter-item :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(102, 126, 234, 0.8) !important;
}

.filter-item :deep(.el-input__inner) {
  color: #fff !important;
}

/* 已选中的值文字颜色 */
.filter-item :deep(.el-select__selected-item) {
  color: #fff !important;
}

.filter-item :deep(.el-select__selected-item span) {
  color: #fff !important;
}

/* placeholder 颜色 */
.filter-item :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

/* 下拉箭头颜色 */
.filter-item :deep(.el-select__suffix .el-icon) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 清除按钮颜色 */
.filter-item :deep(.el-select__clear) {
  color: rgba(255, 255, 255, 0.6) !important;
  background: transparent !important;
}

.filter-item :deep(.el-select__clear:hover) {
  color: #fff !important;
}

.price-filter .price-labels {
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  margin-top: 4px;
}

.filter-item :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.1);
}

.filter-item :deep(.el-slider__bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.filter-item :deep(.el-rate__icon) {
  font-size: 20px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-actions button {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-apply {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-apply:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-reset {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.btn-reset:hover {
  background: rgba(255, 255, 255, 0.2);
}

.btn-save {
  background: transparent;
  border: 1px solid rgba(255, 215, 0, 0.5);
  color: #ffd700;
}

.btn-save:hover {
  background: rgba(255, 215, 0, 0.1);
}

/* ========== 活跃筛选标签 ========== */
.active-filters {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.05);
  padding: 12px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filters-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  color: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  transition: all 0.2s;
}

.tag-close:hover {
  background: #ff4757;
}

.clear-all {
  background: transparent;
  border: none;
  color: #ff4757;
  cursor: pointer;
  font-size: 13px;
  margin-left: auto;
}

.clear-all:hover {
  text-decoration: underline;
}

/* ========== 排序栏 ========== */
.sort-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.05);
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.sort-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.sort-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s;
  font-size: 13px;
}

.sort-btn:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.sort-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: #fff;
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

.content-area :deep(.el-loading-text) {
  color: #ffd700;
}

/* ========== 卡片网格 ========== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

/* ========== 剧本卡片 ========== */
.script-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  position: relative;
  animation: cardAppear 0.5s ease-out backwards;
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.script-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

/* 卡片装饰角 */
.card-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: rgba(255, 215, 0, 0.3);
  border-style: solid;
  border-width: 0;
  transition: all 0.3s;
}

.script-card:hover .corner {
  border-color: rgba(255, 215, 0, 0.6);
}

.corner.top-left { top: 8px; left: 8px; border-top-width: 2px; border-left-width: 2px; }
.corner.top-right { top: 8px; right: 8px; border-top-width: 2px; border-right-width: 2px; }
.corner.bottom-left { bottom: 8px; left: 8px; border-bottom-width: 2px; border-left-width: 2px; }
.corner.bottom-right { bottom: 8px; right: 8px; border-bottom-width: 2px; border-right-width: 2px; }

/* 卡片封面 */
.card-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.script-card:hover .card-cover img {
  transform: scale(1.1);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 50%, rgba(0,0,0,0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.script-card:hover .cover-overlay {
  opacity: 1;
}

.overlay-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 14px;
}

.view-icon {
  font-size: 32px;
  animation: pulse 1.5s infinite;
}

/* 角标 */
.card-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 6px;
}

.badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.badge.hot {
  background: linear-gradient(135deg, #ff4757 0%, #ff6b81 100%);
  color: #fff;
}

.badge.new {
  background: linear-gradient(135deg, #2ed573 0%, #7bed9f 100%);
  color: #fff;
}

/* 分类飘带 */
.category-ribbon {
  position: absolute;
  bottom: 0;
  right: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.9) 0%, rgba(118, 75, 162, 0.9) 100%);
  color: #fff;
  padding: 6px 16px;
  font-size: 12px;
  border-top-left-radius: 12px;
}

/* 卡片内容 */
.card-body {
  padding: 16px;
}

.card-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #fff;
  cursor: pointer;
  transition: color 0.3s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title:hover {
  color: #ffd700;
}

/* 剧本属性 */
.script-attrs {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.attr {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 10px;
  border-radius: 4px;
}

.attr-icon {
  font-size: 14px;
}

.attr.easy { color: #2ed573; }
.attr.medium { color: #ffa502; }
.attr.hard { color: #ff4757; }

/* 评分 */
.card-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  color: rgba(255, 255, 255, 0.2);
  font-size: 14px;
  transition: color 0.3s;
}

.star.filled {
  color: #ffd700;
  text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
}

.rating-score {
  color: #ffd700;
  font-weight: bold;
  font-size: 14px;
}

.review-count {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 12px;
}

.price-section {
  display: flex;
  align-items: baseline;
}

.price {
  color: #ff6b6b;
  font-size: 22px;
  font-weight: bold;
  text-shadow: 0 0 10px rgba(255, 107, 107, 0.3);
}

.price-unit {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  margin-left: 2px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-reserve {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.btn-reserve:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-unfavorite {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-unfavorite:hover {
  background: rgba(255, 71, 87, 0.2);
  border-color: #ff4757;
}

/* 卡片元信息 */
.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.meta-icon {
  font-size: 14px;
}

/* ========== 列表视图 ========== */
.list-view {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.list-header {
  display: grid;
  grid-template-columns: 100px 2fr 1fr 80px 100px 120px 160px;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  font-weight: 500;
}

.list-item {
  display: grid;
  grid-template-columns: 100px 2fr 1fr 80px 100px 120px 160px;
  gap: 16px;
  padding: 16px 20px;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s;
  animation: listItemAppear 0.3s ease-out backwards;
}

@keyframes listItemAppear {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.list-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.list-item:last-child {
  border-bottom: none;
}

.col-cover img {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  transition: transform 0.3s;
}

.col-cover img:hover {
  transform: scale(1.05);
}

.col-name {
  cursor: pointer;
}

.col-name .name {
  color: #fff;
  font-size: 14px;
  display: block;
  margin-bottom: 4px;
  transition: color 0.3s;
}

.col-name:hover .name {
  color: #ffd700;
}

.col-name .category {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.col-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.col-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ffd700;
  font-size: 14px;
}

.stars-mini {
  font-size: 16px;
}

.col-price {
  color: #ff6b6b;
  font-weight: bold;
  font-size: 14px;
}

.col-time {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.col-actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.btn-sm.reserve {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-sm.reserve:hover {
  transform: translateY(-2px);
}

.btn-sm.remove {
  background: rgba(255, 71, 87, 0.2);
  color: #ff4757;
  border: 1px solid rgba(255, 71, 87, 0.3);
}

.btn-sm.remove:hover {
  background: rgba(255, 71, 87, 0.3);
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-illustration {
  position: relative;
  width: 150px;
  height: 150px;
  margin: 0 auto 30px;
}

.magnifier {
  font-size: 80px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.question-marks {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
}

.question-marks span {
  font-size: 24px;
  opacity: 0;
  animation: questionAppear 2s infinite;
}

.question-marks span:nth-child(1) { animation-delay: 0s; }
.question-marks span:nth-child(2) { animation-delay: 0.3s; }
.question-marks span:nth-child(3) { animation-delay: 0.6s; }

@keyframes questionAppear {
  0%, 100% { opacity: 0; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-20px); }
}

.empty-state h3 {
  color: #fff;
  font-size: 20px;
  margin: 0 0 12px;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 30px;
}

.btn-explore {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  padding: 14px 32px;
  border-radius: 30px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-explore:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: rgba(255, 255, 255, 0.7);
  --el-pagination-button-disabled-bg-color: transparent;
}

.pagination-wrapper :deep(.el-pager li) {
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* ========== 回到顶部 ========== */
.backtop-btn {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .list-header,
  .list-item {
    grid-template-columns: 80px 1.5fr 1fr 80px 140px;
  }
  
  .col-time,
  .col-info {
    display: none;
  }
}

@media (max-width: 768px) {
  .favorites-page {
    padding: 12px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  
  .title-text h1 {
    font-size: 22px;
  }
  
  .card-grid {
    grid-template-columns: 1fr;
  }
  
  .list-view {
    display: none;
  }
  
  .sort-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .sort-options {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
