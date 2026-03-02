<template>
  <div class="script-list-container">
    <!-- 剧本杀主题头部 -->
    <div class="script-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-icon">📜</div>
        <h1>剧本库</h1>
        <p>探索500+精选剧本，开启你的推理之旅</p>
        <div class="hero-tags">
          <span class="hero-tag" @click="quickFilter('情感')">💕 情感本</span>
          <span class="hero-tag" @click="quickFilter('推理')">🔍 推理本</span>
          <span class="hero-tag" @click="quickFilter('恐怖')">👻 恐怖本</span>
          <span class="hero-tag" @click="quickFilter('欢乐')">😄 欢乐本</span>
          <span class="hero-tag" @click="quickFilter('机制')">⚔️ 机制本</span>
        </div>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <span class="search-title">🔎 筛选剧本</span>
        <span class="filter-count" v-if="activeFilterCount > 0">已选 {{ activeFilterCount }} 个条件</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="🔤 关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索剧本名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="🎭 分类">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="选择分类"
            clearable
            @change="handleSearch"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="⭐ 难度">
          <el-select
            v-model="searchForm.difficulty"
            placeholder="选择难度"
            clearable
            @change="handleSearch"
          >
            <el-option label="🟢 简单" :value="1" />
            <el-option label="🟡 普通" :value="2" />
            <el-option label="🟠 困难" :value="3" />
            <el-option label="🔴 硬核" :value="4" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="👥 人数">
          <el-select
            v-model="searchForm.playerCount"
            placeholder="选择人数"
            clearable
            @change="handleSearch"
          >
            <el-option label="4人本" :value="4" />
            <el-option label="5人本" :value="5" />
            <el-option label="6人本" :value="6" />
            <el-option label="7人本" :value="7" />
            <el-option label="8人+" :value="8" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="danger" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="list-header">
      <div class="list-header-left">
        <span class="total">📚 共 <strong>{{ total }}</strong> 个剧本</span>
      </div>
      <div class="list-header-right">
        <span class="sort-label">排序：</span>
        <el-radio-group v-model="searchForm.sortBy" @change="handleSearch">
          <el-radio-button label="hot">🔥 最热门</el-radio-button>
          <el-radio-button label="rating">⭐ 评分最高</el-radio-button>
          <el-radio-button label="newest">✨ 最新</el-radio-button>
          <el-radio-button label="price">💰 价格</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 骨架屏 -->
    <SkeletonScriptList v-if="loading" :count="searchForm.pageSize" />
    
    <!-- 真实内容 -->
    <el-row :gutter="20" v-else>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        v-for="script in scripts"
        :key="script.id"
      >
        <div class="script-card" @click="goToDetail(script.id)">
          <div class="script-image">
            <LazyImage
              :src="script.cover || getScriptCover(script.name, script.categoryName)"
              :alt="script.name"
              :height="200"
              @error="handleImageError"
            />
            <div class="script-tag">{{ script.categoryName }}</div>
            <div class="script-badge" v-if="script.isHot">🔥 热门</div>
            <div class="script-badge new" v-else-if="script.isNew">✨ 新本</div>
            <div class="difficulty-indicator" :class="'level-' + script.difficulty">
              {{ getDifficultyStars(script.difficulty) }}
            </div>
          </div>
          <div class="script-info">
            <h4 class="script-title">{{ script.name }}</h4>
            <div class="script-desc">{{ script.description }}</div>
            <div class="script-meta">
              <span class="meta-item difficulty" :class="'diff-' + script.difficulty">
                {{ difficultyMap[script.difficulty] || script.difficulty }}
              </span>
              <span class="meta-item players">👥 {{ script.playerCount }}人</span>
              <span class="meta-item duration">⏱️ {{ script.duration }}h</span>
            </div>
            <div class="script-rating">
              <el-rate v-model="script.rating" disabled show-score size="small" />
              <span class="play-count" v-if="script.playCount">{{ script.playCount }}人玩过</span>
            </div>
            <div class="script-footer">
              <span class="script-price">
                <small>¥</small>{{ script.price }}<small>/人</small>
              </span>
              <el-button type="danger" size="small" @click.stop="goToDetail(script.id)">
                立即预约
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && scripts.length === 0" description="暂无剧本" />

    <el-pagination
      v-if="total > 0"
      class="pagination"
      v-model:current-page="searchForm.page"
      v-model:page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getScriptList, getScriptCategories } from '@/api/script'
import { PLACEHOLDERS } from '@/assets/placeholders'
import { getScriptCover } from '@/assets/script-covers'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Filter } from '@element-plus/icons-vue'
import SkeletonScriptList from '@/components/Skeleton/SkeletonScriptList.vue'
import LazyImage from '@/components/LazyImage.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const scripts = ref([])
const categories = ref([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  categoryId: null,
  difficulty: null,
  playerCount: null,
  sortBy: 'hot',
  page: 1,
  pageSize: 12
})

// 难度映射
const difficultyMap = {
  1: '简单',
  2: '普通',
  3: '困难',
  4: '硬核'
}

// 获取难度星星
const getDifficultyStars = (level) => {
  const stars = ['⭐', '⭐⭐', '⭐⭐⭐', '⭐⭐⭐⭐']
  return stars[level - 1] || '⭐'
}

// 快速筛选
const quickFilter = (keyword) => {
  searchForm.keyword = keyword
  handleSearch()
}

// 排序映射
const sortByMap = {
  'hot': '最热门',
  'rating': '评分最高',
  'newest': '最新',
  'price': '价格'
}

const loadCategories = async () => {
  try {
    const res = await getScriptCategories()
    if (res.data) {
      categories.value = res.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadScripts = async () => {
  loading.value = true
  try {
    console.log('加载剧本列表，参数:', searchForm)
    const res = await getScriptList(searchForm)
    console.log('API返回数据结构:', res)
    
    if (res.data) {
      // 判断是否是分页对象
      if (res.data.records || res.data.list || res.data.total !== undefined) {
        // 后端已分页（PageResult格式）
        scripts.value = res.data.records || res.data.list || []
        total.value = res.data.total || 0
        
        console.log(`✅ 后端分页: 总数${total.value}, 当前页显示${scripts.value.length}条 (第${searchForm.page}页, 每页${searchForm.pageSize}条)`)
      } else if (Array.isArray(res.data)) {
        // 后端返回完整数组（未分页），前端手动分页
        const allScripts = res.data
        total.value = allScripts.length
        
        // 前端分页：计算当前页的起始和结束索引
        const start = (searchForm.page - 1) * searchForm.pageSize
        const end = start + searchForm.pageSize
        scripts.value = allScripts.slice(start, end)
        
        console.log(`✅ 前端分页: 总数${total.value}, 当前页显示${scripts.value.length}条 (第${searchForm.page}页, 每页${searchForm.pageSize}条)`)
      } else {
        console.error('❌ 未知的数据格式:', res.data)
        scripts.value = []
        total.value = 0
      }
    }
  } catch (error) {
    console.error('加载剧本列表失败:', error)
    // 使用模拟数据（48条，用于测试分页）
    const mockData = Array.from({ length: 48 }, (_, i) => ({
      id: i + 1,
      name: `剧本${i + 1}`,
      description: '这是一个精彩的剧本，充满悬念和惊喜...',
      categoryName: ['本格推理', '情感沉浸', '科幻机制', '恐怖惊悚'][i % 4],
      difficulty: (i % 4) + 1,
      playerCount: 4 + (i % 5),
      duration: 3 + (i % 3),
      price: 68 + (i * 10),
      rating: 4 + Math.random(),
      cover: ''
    }))
    
    // 前端分页模拟数据
    total.value = mockData.length
    const start = (searchForm.page - 1) * searchForm.pageSize
    const end = start + searchForm.pageSize
    scripts.value = mockData.slice(start, end)
    
    console.log(`模拟数据分页: 总数${total.value}, 当前显示${scripts.value.length}条`)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (newPage) => {
  console.log('剧本列表页码变化:', newPage)
  searchForm.page = newPage
  // 滚动到顶部，提升用户体验
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadScripts()
}

const handleSizeChange = (newSize) => {
  console.log('剧本列表每页大小变化:', newSize)
  searchForm.pageSize = newSize
  searchForm.page = 1
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadScripts()
}

const handleSearch = () => {
  searchForm.page = 1
  loadScripts()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.difficulty = null
  searchForm.playerCount = null
  searchForm.sortBy = 'hot'
  handleSearch()
}

// 清除单个筛选条件
const clearFilter = (filterName) => {
  if (filterName === 'keyword') {
    searchForm.keyword = ''
  } else if (filterName === 'categoryId') {
    searchForm.categoryId = null
  } else if (filterName === 'difficulty') {
    searchForm.difficulty = null
  } else if (filterName === 'playerCount') {
    searchForm.playerCount = null
  }
  handleSearch()
}

const goToDetail = (id) => {
  // 保存浏览历史
  const script = scripts.value.find(s => s.id === id)
  if (script) {
    saveBrowseHistory(script)
  }
  router.push(`/script/${id}`)
}

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = '/default-script.jpg'
}

// 保存浏览历史到localStorage
const saveBrowseHistory = (scriptData) => {
  try {
    // 验证数据有效性
    if (!scriptData || !scriptData.id || !scriptData.name) {
      console.warn('剧本数据无效，无法保存浏览历史:', scriptData)
      return
    }
    
    let history = []
    const savedHistory = localStorage.getItem('browseHistory')
    if (savedHistory) {
      history = JSON.parse(savedHistory)
    }
    
    // 添加新记录（去重）
    history = history.filter(item => item.id !== scriptData.id)
    
    // 处理 difficulty：如果是字符串，转换为数字
    let difficultyValue = scriptData.difficulty || 2
    if (typeof difficultyValue === 'string') {
      const diffMap = { '简单': 1, '中等': 2, '普通': 2, '困难': 3, '硬核': 4 }
      difficultyValue = diffMap[difficultyValue] || 2
    }
    
    const historyItem = {
      id: scriptData.id,
      name: scriptData.name,
      cover: scriptData.cover || '/default-script.jpg',
      categoryName: scriptData.categoryName || '未分类',
      difficulty: difficultyValue,
      playerCount: scriptData.playerCount || 6,
      rating: typeof scriptData.rating === 'number' ? scriptData.rating : 4.5,
      price: scriptData.price || 0,
      viewTime: new Date().toISOString()
    }
    
    history.unshift(historyItem)
    
    // 只保留最近30条
    history = history.slice(0, 30)
    
    localStorage.setItem('browseHistory', JSON.stringify(history))
    console.log('浏览历史已保存:', scriptData.name, historyItem)
  } catch (error) {
    console.error('保存浏览历史失败:', error)
  }
}


// 计算已选筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (searchForm.keyword) count++
  if (searchForm.categoryId) count++
  if (searchForm.difficulty) count++
  if (searchForm.playerCount) count++
  return count
})

// 判断是否有激活的筛选条件
const hasActiveFilters = computed(() => {
  const hasKeyword = !!(searchForm.keyword && searchForm.keyword.trim())
  const hasCategoryId = !!(searchForm.categoryId)
  const hasDifficulty = !!(searchForm.difficulty)
  const hasPlayerCount = !!(searchForm.playerCount)
  const hasSort = !!(searchForm.sortBy && searchForm.sortBy !== 'hot')
  
  const result = hasKeyword || hasCategoryId || hasDifficulty || hasPlayerCount || hasSort
  
  // 调试日志
  console.log('筛选条件检测:', {
    keyword: searchForm.keyword,
    categoryId: searchForm.categoryId,
    difficulty: searchForm.difficulty,
    playerCount: searchForm.playerCount,
    sortBy: searchForm.sortBy,
    hasActiveFilters: result
  })
  
  return result
})

// 获取分类名称
const getCategoryName = (categoryId) => {
  const category = categories.value.find(c => c.id === categoryId)
  return category ? category.name : ''
}

onMounted(() => {
  loadCategories()
  loadScripts()
})
</script>

<style scoped>
.script-list-container {
  padding: 20px;
  background: transparent;
  min-height: 100vh;
}

/* 头部横幅 */
.script-hero {
  position: relative;
  height: 220px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 25px;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  background-image: url('https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=1200&h=300&fit=crop');
  background-size: cover;
  background-position: center;
}

.hero-bg::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 26, 46, 0.85);
}

.hero-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  text-align: center;
  padding: 20px;
}

.hero-icon {
  font-size: 48px;
  margin-bottom: 10px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.hero-content h1 {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px 0;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
}

.hero-content p {
  font-size: 16px;
  color: rgba(255,255,255,0.8);
  margin: 0 0 20px 0;
}

.hero-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.hero-tag {
  padding: 8px 16px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.hero-tag:hover {
  background: rgba(139, 0, 0, 0.8);
  border-color: #8B0000;
  transform: translateY(-2px);
}

/* 搜索卡片 */
.search-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.search-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.filter-count {
  font-size: 12px;
  color: #ff6b6b;
  background: rgba(139, 0, 0, 0.2);
  padding: 4px 10px;
  border-radius: 12px;
}

.search-form {
  margin: 0;
}

/* 确保el-select选中后显示文字 */
.search-form :deep(.el-select) {
  width: 150px;
}

.search-form :deep(.el-select .el-input__inner),
.search-form :deep(.el-select .el-select__input) {
  color: #fff !important;
  background-color: transparent !important;
}

.search-form :deep(.el-select .el-input__wrapper),
.search-form :deep(.el-input .el-input__wrapper),
.search-form :deep(.el-select .el-input.el-input--suffix .el-input__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

/* 确保所有 Element Plus 输入框都是深色背景 */
.search-card :deep(.el-input__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

.search-card :deep(.el-input__inner) {
  color: #fff !important;
  background: transparent !important;
}

.search-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.search-card :deep(.el-select__wrapper) {
  background-color: rgba(35, 35, 60, 0.9) !important;
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

.search-card :deep(.el-select__selection) {
  color: #fff !important;
}

.search-card :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.search-card :deep(.el-select__caret) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.search-form :deep(.el-select .el-select__placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

.search-form :deep(.el-input__wrapper) {
  background-color: rgba(35, 35, 60, 0.8) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

.search-form :deep(.el-input__inner) {
  color: #fff !important;
}

.search-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.search-form :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}

.search-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

/* 列表头部 */
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.list-header-left {
  display: flex;
  align-items: center;
}

.list-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.total {
  font-size: 15px;
  color: #fff;
}

.total strong {
  color: #ff6b6b;
  font-size: 18px;
  margin: 0 3px;
}

.script-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.script-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.script-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.2);
  border-color: #8B0000;
}

/* 列表项动画 */
.list-enter-active {
  transition: all 0.5s ease;
  transition-delay: calc(var(--index) * 0.05s);
}

.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.list-leave-to {
  opacity: 0;
  transform: translateY(-30px);
}

.list-move {
  transition: transform 0.5s ease;
}

.script-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
}

.script-image :deep(.lazy-image) {
  width: 100%;
  height: 100%;
}

.script-image :deep(.lazy-image img),
.script-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  transition: transform 0.5s ease;
}

.script-card:hover .script-image :deep(img) {
  transform: scale(1.1);
}

.script-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(139, 0, 0, 0.9);
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  z-index: 2;
  backdrop-filter: blur(4px);
}

.script-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: #fff;
  padding: 5px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  z-index: 2;
}

.script-badge.new {
  background: linear-gradient(135deg, #26de81, #20bf6b);
}

.difficulty-indicator {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0,0,0,0.7);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  z-index: 2;
}

.script-info {
  padding: 18px;
}

.script-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.script-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
  min-height: 39px;
}

.script-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-item {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.8);
}

.meta-item.difficulty {
  font-weight: 600;
}

.meta-item.diff-1 {
  background: rgba(46, 125, 50, 0.3);
  color: #7ddc7a;
}

.meta-item.diff-2 {
  background: rgba(245, 124, 0, 0.3);
  color: #ffb74d;
}

.meta-item.diff-3 {
  background: rgba(230, 81, 0, 0.3);
  color: #ff9800;
}

.meta-item.diff-4 {
  background: rgba(198, 40, 40, 0.3);
  color: #ff6b6b;
}

.script-rating {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.play-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.script-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.script-price {
  font-size: 22px;
  font-weight: 700;
  color: #ff6b6b;
}

.script-price small {
  font-size: 13px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .script-hero {
    height: 180px;
  }
  
  .hero-content h1 {
    font-size: 24px;
  }
  
  .hero-tags {
    gap: 8px;
  }
  
  .hero-tag {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .list-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .list-header-right {
    width: 100%;
    overflow-x: auto;
  }
}
</style>

<!-- 非 scoped 样式，覆盖 Element Plus 下拉框和排序按钮 -->
<style>
/* 下拉框弹出层深色主题 - 全局覆盖 */
body .el-select-dropdown,
body .el-popper.el-select__popper {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.99) 0%, rgba(22, 33, 62, 0.99) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.4) !important;
  --el-bg-color-overlay: rgba(26, 26, 46, 0.99) !important;
}

body .el-select-dropdown__list {
  background: transparent !important;
}

body .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
}

body .el-select-dropdown__item:hover,
body .el-select-dropdown__item.hover {
  background: rgba(139, 0, 0, 0.25) !important;
}

body .el-select-dropdown__item.is-selected,
body .el-select-dropdown__item.selected {
  color: #ff6b6b !important;
  background: rgba(139, 0, 0, 0.2) !important;
  font-weight: bold;
}

body .el-select-dropdown__item.is-selected::after {
  border-color: #ff6b6b !important;
}

body .el-select-dropdown__empty {
  color: rgba(255, 255, 255, 0.5) !important;
}

body .el-popper.is-light,
body .el-popper.is-pure {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.99) 0%, rgba(22, 33, 62, 0.99) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.4) !important;
}

body .el-popper.is-light .el-popper__arrow::before,
body .el-popper.is-pure .el-popper__arrow::before {
  background: rgba(26, 26, 46, 0.99) !important;
  border-color: rgba(139, 0, 0, 0.4) !important;
}

/* 下拉框滚动条深色主题 */
body .el-select-dropdown__wrap {
  background: transparent !important;
}

body .el-scrollbar__bar {
  background: rgba(139, 0, 0, 0.2) !important;
}

body .el-scrollbar__thumb {
  background: rgba(139, 0, 0, 0.4) !important;
}

body .el-scrollbar__thumb:hover {
  background: rgba(139, 0, 0, 0.6) !important;
}

/* 排序 Radio Button 深色主题 */
.script-list-container .el-radio-group {
  display: flex;
  gap: 0;
}

.script-list-container .el-radio-button__inner {
  background: rgba(35, 35, 60, 0.8) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  border-color: rgba(139, 0, 0, 0.3) !important;
}

.script-list-container .el-radio-button__inner:hover {
  color: #fff !important;
  background: rgba(139, 0, 0, 0.2) !important;
}

.script-list-container .el-radio-button__original-radio:checked + .el-radio-button__inner {
  background: rgba(139, 0, 0, 0.4) !important;
  color: #ff6b6b !important;
  border-color: #8B0000 !important;
  box-shadow: -1px 0 0 0 #8B0000 !important;
}

/* 分页组件深色主题 */
.script-list-container .el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: rgba(255, 255, 255, 0.8);
  --el-pagination-button-bg-color: rgba(35, 35, 60, 0.8);
  --el-pagination-button-color: rgba(255, 255, 255, 0.8);
  --el-pagination-hover-color: #ff6b6b;
}

.script-list-container .el-pagination .el-pager li {
  background: rgba(35, 35, 60, 0.8) !important;
  color: rgba(255, 255, 255, 0.8) !important;
}

.script-list-container .el-pagination .el-pager li:hover {
  color: #ff6b6b !important;
}

.script-list-container .el-pagination .el-pager li.is-active {
  background: rgba(139, 0, 0, 0.4) !important;
  color: #ff6b6b !important;
}

.script-list-container .el-pagination .btn-prev,
.script-list-container .el-pagination .btn-next {
  background: rgba(35, 35, 60, 0.8) !important;
  color: rgba(255, 255, 255, 0.8) !important;
}

.script-list-container .el-pagination .el-pagination__total,
.script-list-container .el-pagination .el-pagination__jump {
  color: rgba(255, 255, 255, 0.8) !important;
}

.script-list-container .el-pagination .el-input__wrapper {
  background: rgba(35, 35, 60, 0.8) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
}

.script-list-container .el-pagination .el-input__inner {
  color: #fff !important;
}

/* 搜索卡片 Element Plus 覆盖 */
.script-list-container .search-card.el-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  --el-card-bg-color: transparent !important;
}

.script-list-container .search-card .el-card__body {
  background: transparent !important;
}

/* 重置按钮深色主题 */
.script-list-container .search-form .el-button:not(.el-button--danger) {
  background: rgba(35, 35, 60, 0.8) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  border-color: rgba(139, 0, 0, 0.3) !important;
}

.script-list-container .search-form .el-button:not(.el-button--danger):hover {
  background: rgba(139, 0, 0, 0.2) !important;
  border-color: #8B0000 !important;
  color: #fff !important;
}

/* 搜索表单 el-select 下拉框深色主题 */
.script-list-container .search-form .el-select .el-input__wrapper,
.script-list-container .search-form .el-select .el-input .el-input__wrapper {
  background: rgba(35, 35, 60, 0.9) !important;
  background-color: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
  --el-input-bg-color: rgba(35, 35, 60, 0.9) !important;
  --el-fill-color-blank: rgba(35, 35, 60, 0.9) !important;
}

.script-list-container .search-form .el-select .el-input__inner {
  color: #fff !important;
}

.script-list-container .search-form .el-select .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.5) !important;
}

.script-list-container .search-form .el-select .el-select__placeholder {
  color: rgba(255, 255, 255, 0.5) !important;
}

.script-list-container .search-form .el-select .el-select__caret {
  color: rgba(255, 255, 255, 0.6) !important;
}

.script-list-container .search-form .el-select .el-input.is-focus .el-input__wrapper {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}

/* el-select 选中的标签深色主题 */
.script-list-container .search-form .el-select .el-tag {
  background: rgba(139, 0, 0, 0.3) !important;
  border-color: rgba(139, 0, 0, 0.5) !important;
  color: #ff6b6b !important;
}

.script-list-container .search-form .el-select .el-tag .el-tag__close {
  color: #ff6b6b !important;
  background: transparent !important;
}

.script-list-container .search-form .el-select .el-tag .el-tag__close:hover {
  background: rgba(139, 0, 0, 0.5) !important;
  color: #fff !important;
}

/* el-input 搜索框深色主题 */
.script-list-container .search-form .el-input .el-input__wrapper {
  background: rgba(35, 35, 60, 0.9) !important;
  background-color: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) inset !important;
  --el-input-bg-color: rgba(35, 35, 60, 0.9) !important;
  --el-fill-color-blank: rgba(35, 35, 60, 0.9) !important;
}

.script-list-container .search-form .el-input .el-input__inner {
  color: #fff !important;
}

.script-list-container .search-form .el-input .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.5) !important;
}

.script-list-container .search-form .el-input .el-input__prefix .el-icon {
  color: rgba(255, 255, 255, 0.6) !important;
}

.script-list-container .search-form .el-input.is-focus .el-input__wrapper {
  box-shadow: 0 0 0 1px #8B0000 inset !important;
}
</style>
