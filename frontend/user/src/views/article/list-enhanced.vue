<template>
  <div class="article-list-container">
    <!-- 头部横幅 -->
    <div class="header-banner">
      <div class="banner-content">
        <h2>🕵️ 玩家咨询 · 资讯攻略中心</h2>
        <p class="subtitle">选本不踩雷 · DM小技巧 · 门店避坑指南 · 热门榜单速递</p>
        <div class="stats">
          <span><strong>{{ total }}</strong> 篇攻略</span>
          <span><strong>实时更新</strong> 热榜</span>
          <span><strong>玩家精选</strong> 必读</span>
        </div>
      </div>
    </div>

    <!-- 快速导航（按分类） -->
    <div class="quick-nav">
      <el-button 
        v-for="(cat, index) in categories" 
        :key="index"
        :type="queryParams.category === cat.value ? 'primary' : ''"
        @click="selectCategory(cat.value)"
        size="large"
      >
        <span class="nav-icon">{{ cat.icon }}</span>
        {{ cat.label }}
      </el-button>
    </div>

    <!-- 搜索和排序 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜：城限 / 机制 / 恐怖 / 新手 / DM / 门店..."
        clearable
        @keyup.enter="loadArticles"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button :icon="Search" @click="loadArticles">搜索</el-button>
        </template>
      </el-input>
      
      <el-select v-model="queryParams.sortBy" placeholder="排序" @change="handleSortChange" class="sort-select">
        <el-option label="最新发布" value="time" />
        <el-option label="最多浏览" value="view" />
        <el-option label="最多点赞" value="like" />
        <el-option label="最多评论" value="comment" />
      </el-select>
    </div>

    <div class="content-layout">
      <!-- 主内容：文章流 -->
      <div class="main-column">
        <div class="article-list" v-loading="loading">
          <!-- 推荐文章（置顶） -->
          <div v-if="topArticles.length > 0 && !queryParams.keyword" class="top-section">
            <h3 class="section-title">
              <el-icon><Star /></el-icon>
              置顶必读
            </h3>
            <div
              class="article-card top-article"
              v-for="article in topArticles"
              :key="'top-' + article.id"
              @click="goToDetail(article.id)"
            >
              <div class="article-cover">
                <img :src="article.coverImage" :alt="article.title" />
                <div class="article-badges">
                  <span class="badge badge-top">置顶</span>
                  <span class="badge badge-category" :style="{ background: getCategoryColor(article.category) }">
                    {{ article.categoryName }}
                  </span>
                </div>
              </div>
              <div class="article-info-wrapper">
                <h3 class="article-title">{{ article.title }}</h3>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span class="meta-item">
                    <el-icon><User /></el-icon>
                    {{ article.authorName || '匿名' }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatDate(article.publishTime) }}
                  </span>
                  <div class="meta-stats">
                    <span class="stat-item">
                      <el-icon><View /></el-icon>
                      {{ formatNumber(article.viewCount) }}
                    </span>
                    <span class="stat-item">
                      <el-icon><StarFilled /></el-icon>
                      {{ formatNumber(article.likeCount) }}
                    </span>
                    <span class="stat-item" v-if="article.commentCount > 0">
                      <el-icon><ChatDotRound /></el-icon>
                      {{ formatNumber(article.commentCount) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 普通文章 -->
          <div class="normal-section">
            <h3 class="section-title" v-if="topArticles.length > 0 && !queryParams.keyword">
              <el-icon><Document /></el-icon>
              全部攻略
            </h3>

            <div class="articles-grid">
              <div
                class="article-card"
                v-for="article in normalArticles"
                :key="article.id"
                @click="goToDetail(article.id)"
              >
                <div class="article-cover">
                  <img :src="article.coverImage" :alt="article.title" />
                  <div class="article-badges">
                    <span class="badge badge-category" :style="{ background: getCategoryColor(article.category) }">
                      {{ article.categoryName }}
                    </span>
                    <span class="badge badge-hot" v-if="article.viewCount > 2000">
                      🔥 热门
                    </span>
                  </div>
                </div>
                <div class="article-info-wrapper">
                  <h3 class="article-title">{{ article.title }}</h3>
                  <p class="article-summary">{{ article.summary }}</p>
                  <div class="article-meta">
                    <span class="meta-item">
                      <el-icon><User /></el-icon>
                      {{ article.authorName || '匿名' }}
                    </span>
                    <span class="meta-item">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(article.publishTime) }}
                    </span>
                    <div class="meta-stats">
                      <span class="stat-item">
                        <el-icon><View /></el-icon>
                        {{ formatNumber(article.viewCount) }}
                      </span>
                      <span class="stat-item">
                        <el-icon><StarFilled /></el-icon>
                        {{ formatNumber(article.likeCount) }}
                      </span>
                      <span class="stat-item" v-if="article.commentCount > 0">
                        <el-icon><ChatDotRound /></el-icon>
                        {{ formatNumber(article.commentCount) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="!loading && articleList.length === 0" description="暂无内容" />
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :total="total"
            layout="total, prev, pager, next, jumper"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- 侧边栏：像“咨询台/公告栏/热榜”一样展示关键入口 -->
      <div class="side-column">
        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="side-card-title">📌 必读推荐</div>
          </template>
          <div v-loading="sidebarLoading" class="side-list">
            <div
              v-for="item in recommendedArticles"
              :key="'rec-' + item.id"
              class="side-item"
              @click="goToDetail(item.id)"
            >
              <div class="side-item-title">{{ item.title }}</div>
              <div class="side-item-meta">
                <span>{{ item.categoryName }}</span>
                <span>·</span>
                <span>{{ formatNumber(item.viewCount) }} 浏览</span>
              </div>
            </div>
            <el-empty v-if="!sidebarLoading && recommendedArticles.length === 0" description="暂无推荐" />
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="side-card-title">🔥 热门榜</div>
          </template>
          <div v-loading="sidebarLoading" class="side-list">
            <div
              v-for="(item, idx) in hotArticles"
              :key="'hot-' + item.id"
              class="side-item"
              @click="goToDetail(item.id)"
            >
              <div class="side-item-rank" :class="{ top3: idx < 3 }">{{ idx + 1 }}</div>
              <div class="side-item-body">
                <div class="side-item-title">{{ item.title }}</div>
                <div class="side-item-meta">
                  <span>{{ item.categoryName }}</span>
                  <span>·</span>
                  <span>{{ formatNumber(item.viewCount) }} 浏览</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!sidebarLoading && hotArticles.length === 0" description="暂无榜单" />
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="side-card-title">🚪 快捷入口</div>
          </template>
          <div class="quick-entry">
            <el-button class="entry-btn" @click="router.push('/script')">去选本</el-button>
            <el-button class="entry-btn" @click="router.push('/store')">找门店</el-button>
            <el-button class="entry-btn" @click="router.push('/group')">拼车大厅</el-button>
            <el-button class="entry-btn" @click="router.push('/recommend/enhanced')">智能推荐</el-button>
            <el-button class="entry-btn" @click="router.push('/help')">新手指南</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList, getHotArticles, getRecommendedArticles } from '@/api/article'
import {
  Search, User, Calendar, View, StarFilled, ChatDotRound,
  Star, Document
} from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const articleList = ref([])
const total = ref(0)

// 侧边栏：推荐/热榜
const sidebarLoading = ref(false)
const hotArticles = ref([])
const recommendedArticles = ref([])

const categories = [
  { label: '全部', value: null, icon: '📚' },
  { label: '新手攻略', value: 1, icon: '🎓' },
  { label: '选本技巧', value: 2, icon: '🎯' },
  { label: '榜单推荐', value: 3, icon: '🏆' },
  { label: '行业动态', value: 4, icon: '📊' },
  { label: '玩家心得', value: 5, icon: '💭' }
]

const queryParams = ref({
  page: 1,
  pageSize: 9,
  category: null,
  keyword: '',
  sortBy: 'time' // 默认按最新发布排序
})

// 分离置顶和普通文章
const topArticles = computed(() => {
  return articleList.value.filter(article => article.isTop === 1)
})

const normalArticles = computed(() => {
  return articleList.value.filter(article => article.isTop !== 1)
})

const loadArticles = async () => {
  loading.value = true
  try {
    console.log('加载文章，参数:', queryParams.value)
    const res = await getArticleList(queryParams.value)
    if (res.code === 1 || res.code === 200) {
      articleList.value = res.data.records
      total.value = res.data.total
      console.log('文章加载成功，总数:', total.value, '当前页:', queryParams.value.page)
    }
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (newPage) => {
  console.log('页码变化:', newPage)
  queryParams.value.page = newPage
  loadArticles()
}

const handleSizeChange = (newSize) => {
  console.log('每页大小变化:', newSize)
  queryParams.value.pageSize = newSize
  queryParams.value.page = 1
  loadArticles()
}

const selectCategory = (category) => {
  queryParams.value.category = category
  queryParams.value.page = 1
  loadArticles()
}

const handleSortChange = () => {
  queryParams.value.page = 1
  loadArticles()
}

const loadSidebar = async () => {
  sidebarLoading.value = true
  try {
    const [hotRes, recRes] = await Promise.all([
      getHotArticles(8),
      getRecommendedArticles(6)
    ])

    if (hotRes.code === 1 || hotRes.code === 200) {
      hotArticles.value = hotRes.data || []
    }
    if (recRes.code === 1 || recRes.code === 200) {
      recommendedArticles.value = recRes.data || []
    }
  } catch (e) {
    console.error('加载侧边栏失败:', e)
  } finally {
    sidebarLoading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (24 * 60 * 60 * 1000))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num
}

const getCategoryColor = (category) => {
  const colors = {
    1: '#409eff', // 新手攻略 - 蓝色
    2: '#67c23a', // 选本技巧 - 绿色
    3: '#e6a23c', // 榜单推荐 - 橙色
    4: '#f56c6c', // 行业动态 - 红色
    5: '#909399'  // 玩家心得 - 灰色
  }
  return colors[category] || '#909399'
}

onMounted(() => {
  loadArticles()
  loadSidebar()
})
</script>

<style scoped>
.article-list-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 40px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  min-height: 100vh;
}

/* 头部横幅 - 剧本杀特色 */
.header-banner {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  padding: 60px 40px;
  border-radius: 20px;
  margin: 20px 0 30px 0;
  text-align: center;
  box-shadow: 0 10px 40px rgba(139, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.header-banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(139,0,0,0.15)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  background-size: 50px 50px;
}

.header-banner::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(ellipse at center, rgba(139, 0, 0, 0.3) 0%, transparent 70%);
  animation: pulse 8s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.6; }
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-content h2 {
  font-size: 42px;
  margin-bottom: 15px;
  font-weight: 800;
  text-shadow: 0 4px 20px rgba(139, 0, 0, 0.5);
  animation: fadeInDown 0.6s ease-out;
  letter-spacing: 3px;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 30px;
  animation: fadeIn 0.8s ease-out 0.2s both;
  letter-spacing: 2px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.stats {
  display: flex;
  justify-content: center;
  gap: 30px;
  font-size: 14px;
  animation: fadeInUp 0.8s ease-out 0.4s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stats span {
  background: rgba(139, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.5);
  padding: 15px 25px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.stats span:hover {
  background: rgba(139, 0, 0, 0.5);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.3);
}

.stats span strong {
  font-size: 28px;
  display: block;
  margin-bottom: 5px;
  font-weight: 800;
  color: #fff;
}

/* 快速导航 - 剧本杀风格 */
.quick-nav {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.quick-nav .el-button {
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  border: 2px solid rgba(139, 0, 0, 0.3);
  padding: 12px 24px;
  background: rgba(35, 35, 60, 0.8);
  color: rgba(255, 255, 255, 0.9);
}

.quick-nav .el-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(139, 0, 0, 0.3);
  border-color: #8B0000;
  color: #fff;
  background: rgba(139, 0, 0, 0.3);
}

.quick-nav .el-button--primary {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  border-color: #8B0000;
  color: #fff;
}

.quick-nav .el-button--primary:hover {
  background: linear-gradient(135deg, #a01010 0%, #b01515 100%);
  color: #fff;
}

.nav-icon {
  margin-right: 8px;
  font-size: 20px;
}

/* 两栏布局 */
.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  align-items: start;
}

.main-column {
  min-width: 0;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 90px;
  align-self: start;
}

.side-card {
  border-radius: 16px;
  border: 1px solid rgba(139, 0, 0, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
}

.side-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 16px 20px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.side-card :deep(.el-card__body) {
  background: transparent !important;
}

.side-card-title {
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  font-size: 15px;
}

.side-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}

.side-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin: 0 8px;
}

.side-item:hover {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.2) 0%, rgba(139, 0, 0, 0.1) 100%);
  transform: translateX(4px);
}

.side-item:hover .side-item-title {
  color: #ff6b6b;
}

.side-item-title {
  font-weight: 600;
  color: #fff;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 14px;
  transition: color 0.3s;
}

.side-item-meta {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.side-item-rank {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(35, 35, 60, 0.8);
  color: rgba(255, 255, 255, 0.7);
  font-weight: 800;
  font-size: 13px;
  flex: 0 0 auto;
  margin-top: 2px;
}

.side-item-rank.top3 {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.3);
}

.side-item-body {
  min-width: 0;
  flex: 1;
}

.quick-entry {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 8px;
}

.entry-btn {
  border-radius: 12px !important;
  border: 1.5px solid rgba(139, 0, 0, 0.35) !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  transition: all 0.3s !important;
  background: rgba(35, 35, 60, 0.85) !important;
  color: rgba(255, 255, 255, 0.9) !important;
  height: 38px !important;
}

.entry-btn:hover {
  border-color: #c0392b !important;
  color: #fff !important;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.4), rgba(192, 57, 43, 0.3)) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 14px rgba(139, 0, 0, 0.35) !important;
}

/* 搜索和排序 - 剧本杀风格 */
.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
  align-items: center;
}

.search-input {
  flex: 1;
}

/* 搜索框整体容器去掉默认分组边框 */
.search-input :deep(.el-input-group) {
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(139, 0, 0, 0.15);
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 0;
  box-shadow: none !important;
  background: rgba(35, 35, 60, 0.95);
  border: 1px solid rgba(139, 0, 0, 0.25);
  border-right: none;
  height: 46px;
  transition: all 0.3s;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(40, 40, 70, 0.98);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #8B0000;
  background: rgba(40, 40, 70, 0.98);
}

.search-input :deep(.el-input__inner) {
  color: #fff;
  font-size: 14px;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.search-input :deep(.el-input__prefix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

/* 搜索按钮 */
.search-input :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #8B0000 0%, #c0392b 100%) !important;
  border: none !important;
  padding: 0;
  overflow: hidden;
  border-radius: 0 12px 12px 0 !important;
  box-shadow: 2px 0 0 0 transparent;
  transition: all 0.3s;
}

.search-input :deep(.el-input-group__append:hover) {
  background: linear-gradient(135deg, #a01020 0%, #e74c3c 100%) !important;
  box-shadow: 0 0 20px rgba(192, 57, 43, 0.6);
}

.search-input :deep(.el-input-group__append .el-button) {
  background: transparent !important;
  border: none !important;
  color: #fff !important;
  font-weight: 700 !important;
  font-size: 15px !important;
  letter-spacing: 2px !important;
  padding: 0 28px !important;
  height: 46px !important;
  margin: 0 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  transition: all 0.3s;
}

.search-input :deep(.el-input-group__append .el-button:hover) {
  color: #fff !important;
  opacity: 0.95;
}

.search-input :deep(.el-input-group__append .el-button .el-icon) {
  color: #fff !important;
  margin-right: 6px;
  font-size: 16px !important;
}

/* 排序选择器 */
.sort-select {
  width: 140px;
  flex-shrink: 0;
}

.sort-select :deep(.el-input__wrapper) {
  border-radius: 14px;
  background: rgba(35, 35, 60, 0.95);
  box-shadow: none !important;
  border: 1px solid rgba(139, 0, 0, 0.25);
  height: 46px;
  transition: all 0.3s;
}

.sort-select :deep(.el-input__wrapper:hover) {
  border-color: rgba(139, 0, 0, 0.5);
}

.sort-select :deep(.el-input__inner) {
  color: #fff;
  font-size: 14px;
}

.sort-select :deep(.el-input__suffix .el-icon) {
  color: rgba(255, 255, 255, 0.5);
}

/* 排序选择器已选中值文字 */
.sort-select :deep(.el-select__selected-item) {
  color: #fff !important;
}

.sort-select :deep(.el-select__selected-item span) {
  color: #fff !important;
}

.sort-select :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

/* 文章卡片 - 剧本杀风格 */
.section-title {
  font-size: 22px;
  color: #fff;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
}

.section-title .el-icon {
  color: #ff6b6b;
}

.top-section {
  margin-bottom: 40px;
}

.article-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 24px;
  border: 2px solid rgba(139, 0, 0, 0.2);
}

.article-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(139, 0, 0, 0.3);
  border-color: #8B0000;
}

.article-card:hover .article-title {
  color: #ff6b6b;
}

.top-article {
  display: flex;
  gap: 24px;
}

.top-article .article-cover {
  width: 400px;
  height: 260px;
  flex-shrink: 0;
}

.article-cover {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: rgba(35, 35, 60, 0.9);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.1);
}

.article-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.badge-top {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
}

.badge-category {
  background: rgba(139, 0, 0, 0.9);
}

.badge-hot {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
}

.article-info-wrapper {
  padding: 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.article-title {
  font-size: 18px;
  color: #fff;
  margin-bottom: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 700;
  transition: color 0.3s;
}

.article-summary {
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(139, 0, 0, 0.2);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-stats {
  display: flex;
  gap: 16px;
  margin-left: auto;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.3s;
}

.stat-item:hover {
  color: #ff6b6b;
}

/* 网格布局 */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* 分页 - 剧本杀风格 */
.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

/* 分页数字颜色 */
.pagination-wrapper :deep(.el-pagination) {
  color: rgba(255, 255, 255, 0.7) !important;
}

.pagination-wrapper :deep(.el-pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  border-radius: 6px;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: linear-gradient(135deg, #8B0000, #c0392b) !important;
  color: #fff !important;
  font-weight: 700;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #fff !important;
  background: rgba(139, 0, 0, 0.4) !important;
}

.pagination-wrapper :deep(.el-pagination button) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  border-radius: 6px;
}

.pagination-wrapper :deep(.el-pagination button:hover) {
  color: #fff !important;
  background: rgba(139, 0, 0, 0.4) !important;
}

.pagination-wrapper :deep(.el-pagination .el-pagination__total) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.pagination-wrapper :deep(.el-pagination .el-pagination__jump) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.pagination-wrapper :deep(.el-pagination .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
}

.pagination-wrapper :deep(.el-pagination .el-input__inner) {
  color: #fff !important;
}

/* 响应式 */
@media (max-width: 1200px) {
  .articles-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-layout {
    grid-template-columns: minmax(0, 1fr) 320px;
  }
}

@media (max-width: 768px) {
  .header-banner {
    padding: 40px 20px;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .side-column {
    position: static;
    top: auto;
  }

  .quick-entry {
    grid-template-columns: 1fr;
  }

  .banner-content h2 {
    font-size: 28px;
  }

  .stats {
    flex-direction: column;
    gap: 15px;
  }

  .quick-nav {
    justify-content: flex-start;
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .filter-bar {
    flex-direction: column;
  }

  .search-input,
  .sort-select {
    max-width: 100%;
    width: 100%;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .top-article {
    flex-direction: column;
  }

  .top-article .article-cover {
    width: 100%;
    height: 200px;
  }
}
</style>
