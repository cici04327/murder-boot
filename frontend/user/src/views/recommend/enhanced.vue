<template>
  <div class="recommend-page">
    <!-- 神秘背景 -->
    <div class="mystery-bg">
      <div class="particles">
        <span v-for="n in 15" :key="n" class="particle"></span>
      </div>
      <div class="floating-icons">
        <span class="float-icon" v-for="n in 6" :key="'icon'+n">🔍</span>
      </div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <span class="title-icon">🎯</span>
          <div class="title-text">
            <h1>为你推荐</h1>
            <p>基于你的推理偏好，精选最适合你的剧本</p>
          </div>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ totalScripts }}</span>
            <span class="stat-label">精选剧本</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userPreference }}</span>
            <span class="stat-label">匹配度</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐分类Tab -->
    <div class="category-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-name">{{ tab.name }}</span>
        <span v-if="tab.count" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">

      <!-- AI增强推荐 -->
      <div v-show="activeTab === 'ai-enhanced'" class="tab-content">
        <!-- AI用户画像卡片 -->
        <div class="ai-profile-card" v-if="aiProfile || loadingProfile">
          <div class="ai-profile-header">
            <span class="ai-badge">🤖 AI</span>
            <span class="ai-profile-title">你的玩家画像</span>
            <span class="ai-profile-tags" v-if="aiProfile?.preferenceTags?.length">
              <el-tag v-for="tag in (aiProfile.preferenceTags||[]).slice(0,5)" :key="tag"
                size="small" type="info" style="margin:2px">{{ tag }}</el-tag>
            </span>
          </div>
          <div class="ai-profile-desc" v-if="loadingProfile">
            <span class="ai-typing">AI正在分析你的游戏偏好...</span>
          </div>
          <div class="ai-profile-desc" v-else-if="aiProfile?.aiProfile">
            {{ aiProfile.aiProfile }}
          </div>
        </div>

        <!-- AI推荐列表 -->
        <div class="section-intro ai">
          <span class="intro-icon">🤖</span>
          <div class="intro-text">
            <h3>AI智能推荐</h3>
            <p>结合你的行为偏好，经AI重排序+个性化推荐理由，精准为你匹配</p>
          </div>
        </div>
        <div v-loading="loading.aiEnhanced" class="script-grid" element-loading-text="AI正在为你分析推荐...">
          <div v-for="(script, index) in recommendations.aiEnhanced" :key="script.id"
            class="script-card" @click="goToScript(script.id)">
            <div class="card-rank" v-if="script.aiRank">{{ script.aiRank }}</div>
            <div class="card-cover">
              <img :src="script.cover" :alt="script.name" loading="lazy" />
              <div class="card-overlay">
                <span class="overlay-btn">查看详情</span>
              </div>
            </div>
            <div class="card-info">
              <h3 class="card-name">{{ script.name }}</h3>
              <div class="card-meta">
                <span class="meta-category">{{ script.categoryName }}</span>
                <span class="meta-difficulty" :class="getDifficultyClass(script.difficulty)">
                  {{ getDifficultyText(script.difficulty) }}
                </span>
              </div>
              <div class="card-rating">
                <el-rate :model-value="Number(script.rating)" disabled size="small" />
                <span class="rating-value">{{ script.rating }}</span>
              </div>
              <!-- AI推荐理由 -->
              <div class="ai-reason-tag" v-if="script.aiReason">
                <span class="ai-icon-small">🤖</span>
                <span>{{ script.aiReason }}</span>
              </div>
              <div v-else-if="script.recommendReason" class="reason-tag">
                {{ script.recommendReason }}
              </div>
              <div class="card-footer">
                <span class="card-price">¥{{ script.price }}</span>
                <span class="card-players">👥 {{ script.playerCount }}人</span>
              </div>
            </div>
          </div>
          <div v-if="!loading.aiEnhanced && recommendations.aiEnhanced.length === 0" class="empty-tip">
            暂无AI推荐数据，请先浏览一些剧本
          </div>
        </div>
      </div>

      <!-- 个性推荐 -->
      <div v-show="activeTab === 'personalized'" class="tab-content">
        <div class="section-intro">
          <span class="intro-icon">🎭</span>
          <div class="intro-text">
            <h3>专属推荐</h3>
            <p>根据你的游戏历史和偏好，AI智能匹配最适合你的剧本</p>
          </div>
        </div>
        <div v-loading="loading.personalized" class="script-grid" element-loading-text="正在分析你的偏好...">
          <div 
            v-for="(script, index) in recommendations.personalized" 
            :key="script.id"
            class="script-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="handleScriptClick(script)"
          >
            <div class="card-rank" v-if="index < 3">
              <span class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
            </div>
            <div class="card-cover">
              <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
              <div class="cover-overlay">
                <span class="play-icon">🔍</span>
                <span>查看详情</span>
              </div>
              <div class="match-score" v-if="script.recommendScore">
                <span class="score-icon">💯</span>
                <span>{{ script.recommendScore }}%匹配</span>
              </div>
              <div class="recommend-reason" v-if="script.recommendReason">
                {{ script.recommendReason }}
              </div>
            </div>
            <div class="card-body">
              <h4 class="card-title">{{ script.name }}</h4>
              <div class="card-tags">
                <span class="tag category">{{ script.categoryName || '剧本杀' }}</span>
                <span class="tag players">👥 {{ script.playerCount }}人</span>
                <span class="tag difficulty" :class="getDifficultyClass(script.difficulty)">
                  {{ getDifficultyText(script.difficulty) }}
                </span>
              </div>
              <div class="card-footer">
                <div class="rating">
                  <span class="stars">★</span>
                  <span class="score">{{ (script.rating || 4.5).toFixed(1) }}</span>
                </div>
                <div class="price">¥{{ script.price }}<span>/人</span></div>
              </div>
            </div>
          </div>
          <div v-if="!loading.personalized && recommendations.personalized.length === 0" class="empty-state">
            <span class="empty-icon">🔮</span>
            <p>暂无个性化推荐，多玩几局后系统会更了解你</p>
          </div>
        </div>
      </div>

      <!-- 今日热门 -->
      <div v-show="activeTab === 'hot-daily'" class="tab-content">
        <div class="section-intro hot">
          <span class="intro-icon">🔥</span>
          <div class="intro-text">
            <h3>今日热门</h3>
            <p>24小时内最受欢迎的剧本，大家都在玩</p>
          </div>
        </div>
        <div v-loading="loading.hotDaily" class="script-grid" element-loading-text="加载热门剧本...">
          <div 
            v-for="(script, index) in recommendations.hotDaily" 
            :key="script.id"
            class="script-card hot-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="handleScriptClick(script)"
          >
            <div class="card-rank hot">
              <span class="rank-badge" :class="'rank-' + Math.min(index + 1, 3)">{{ index + 1 }}</span>
              <span class="hot-fire" v-if="index < 3">🔥</span>
            </div>
            <div class="card-cover">
              <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
              <div class="cover-overlay">
                <span class="play-icon">🔍</span>
                <span>查看详情</span>
              </div>
              <div class="hot-badge">HOT</div>
            </div>
            <div class="card-body">
              <h4 class="card-title">{{ script.name }}</h4>
              <div class="card-tags">
                <span class="tag category">{{ script.categoryName || '剧本杀' }}</span>
                <span class="tag players">👥 {{ script.playerCount }}人</span>
              </div>
              <div class="card-footer">
                <div class="rating">
                  <span class="stars">★</span>
                  <span class="score">{{ (script.rating || 4.5).toFixed(1) }}</span>
                </div>
                <div class="price">¥{{ script.price }}<span>/人</span></div>
              </div>
            </div>
          </div>
          <div v-if="!loading.hotDaily && recommendations.hotDaily.length === 0" class="empty-state">
            <span class="empty-icon">🔥</span>
            <p>暂无热门数据</p>
          </div>
        </div>
      </div>

      <!-- 本周热门 -->
      <div v-show="activeTab === 'hot-weekly'" class="tab-content">
        <div class="section-intro weekly">
          <span class="intro-icon">📈</span>
          <div class="intro-text">
            <h3>本周热门</h3>
            <p>本周最受关注的剧本排行榜</p>
          </div>
        </div>
        <div v-loading="loading.hotWeekly" class="script-grid" element-loading-text="加载周榜数据...">
          <div 
            v-for="(script, index) in recommendations.hotWeekly" 
            :key="script.id"
            class="script-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="handleScriptClick(script)"
          >
            <div class="card-rank">
              <span class="rank-badge" :class="'rank-' + Math.min(index + 1, 3)">{{ index + 1 }}</span>
            </div>
            <div class="card-cover">
              <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
              <div class="cover-overlay">
                <span class="play-icon">🔍</span>
                <span>查看详情</span>
              </div>
            </div>
            <div class="card-body">
              <h4 class="card-title">{{ script.name }}</h4>
              <div class="card-tags">
                <span class="tag category">{{ script.categoryName || '剧本杀' }}</span>
                <span class="tag players">👥 {{ script.playerCount }}人</span>
              </div>
              <div class="card-footer">
                <div class="rating">
                  <span class="stars">★</span>
                  <span class="score">{{ (script.rating || 4.5).toFixed(1) }}</span>
                </div>
                <div class="price">¥{{ script.price }}<span>/人</span></div>
              </div>
            </div>
          </div>
          <div v-if="!loading.hotWeekly && recommendations.hotWeekly.length === 0" class="empty-state">
            <span class="empty-icon">📈</span>
            <p>暂无周榜数据</p>
          </div>
        </div>
      </div>

      <!-- 口碑榜 -->
      <div v-show="activeTab === 'reputation'" class="tab-content">
        <div class="section-intro reputation">
          <span class="intro-icon">⭐</span>
          <div class="intro-text">
            <h3>口碑之选</h3>
            <p>玩家好评如潮，质量有保障的优质剧本</p>
          </div>
        </div>
        <div v-loading="loading.reputation" class="script-grid" element-loading-text="加载口碑榜...">
          <div 
            v-for="(script, index) in recommendations.reputation" 
            :key="script.id"
            class="script-card reputation-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="handleScriptClick(script)"
          >
            <div class="card-rank">
              <span class="rank-badge gold">{{ index + 1 }}</span>
            </div>
            <div class="card-cover">
              <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
              <div class="cover-overlay">
                <span class="play-icon">🔍</span>
                <span>查看详情</span>
              </div>
              <div class="quality-badge">⭐ 口碑佳作</div>
            </div>
            <div class="card-body">
              <h4 class="card-title">{{ script.name }}</h4>
              <div class="card-tags">
                <span class="tag category">{{ script.categoryName || '剧本杀' }}</span>
                <span class="tag players">👥 {{ script.playerCount }}人</span>
              </div>
              <div class="card-footer">
                <div class="rating highlight">
                  <span class="stars">★</span>
                  <span class="score">{{ (script.rating || 4.8).toFixed(1) }}</span>
                </div>
                <div class="price">¥{{ script.price }}<span>/人</span></div>
              </div>
            </div>
          </div>
          <div v-if="!loading.reputation && recommendations.reputation.length === 0" class="empty-state">
            <span class="empty-icon">⭐</span>
            <p>暂无口碑数据</p>
          </div>
        </div>
      </div>

      <!-- 新品上架 -->
      <div v-show="activeTab === 'new'" class="tab-content">
        <div class="section-intro new">
          <span class="intro-icon">✨</span>
          <div class="intro-text">
            <h3>新品速递</h3>
            <p>最新上架的剧本，抢先体验</p>
          </div>
        </div>
        <div v-loading="loading.new" class="script-grid" element-loading-text="加载新品...">
          <div 
            v-for="(script, index) in recommendations.new" 
            :key="script.id"
            class="script-card new-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="handleScriptClick(script)"
          >
            <div class="new-badge">NEW</div>
            <div class="card-cover">
              <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
              <div class="cover-overlay">
                <span class="play-icon">🔍</span>
                <span>查看详情</span>
              </div>
            </div>
            <div class="card-body">
              <h4 class="card-title">{{ script.name }}</h4>
              <div class="card-tags">
                <span class="tag category">{{ script.categoryName || '剧本杀' }}</span>
                <span class="tag players">👥 {{ script.playerCount }}人</span>
              </div>
              <div class="card-footer">
                <div class="rating">
                  <span class="stars">★</span>
                  <span class="score">{{ (script.rating || 4.5).toFixed(1) }}</span>
                </div>
                <div class="price">¥{{ script.price }}<span>/人</span></div>
              </div>
            </div>
          </div>
          <div v-if="!loading.new && recommendations.new.length === 0" class="empty-state">
            <span class="empty-icon">✨</span>
            <p>暂无新品</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 猜你喜欢 -->
    <div v-if="recommendations.history.length > 0" class="history-section">
      <div class="section-header">
        <span class="section-icon">💡</span>
        <h2>猜你喜欢</h2>
        <span class="section-desc">根据你的浏览历史推荐</span>
      </div>
      <div class="history-grid">
        <div 
          v-for="script in recommendations.history" 
          :key="script.id"
          class="history-card"
          @click="handleScriptClick(script)"
        >
          <img :src="script.cover || '/default-script.jpg'" :alt="script.name" />
          <div class="history-info">
            <h5>{{ script.name }}</h5>
            <span class="history-price">¥{{ script.price }}</span>
          </div>
        </div>
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
import { ElMessage } from 'element-plus'
import {
  getPersonalizedRecommendations,
  getHotRecommendations,
  getNewScriptRecommendations,
  getHistoryBasedRecommendations,
  recordRecommendationClick,
  getAiEnhancedRecommendations,
  getAiUserProfile
} from '@/api/recommendation'
import { getScriptList } from '@/api/script'

const router = useRouter()
const tabs = ref([
  { key: 'ai-enhanced', name: 'AI推荐', icon: '🤖', count: 0 },
  { key: 'personalized', name: '专属推荐', icon: '🎯', count: 0 },
  { key: 'hot-daily', name: '今日热门', icon: '🔥', count: 0 },
  { key: 'hot-weekly', name: '本周热门', icon: '📈', count: 0 },
  { key: 'reputation', name: '口碑榜', icon: '⭐', count: 0 },
  { key: 'new', name: '新品上架', icon: '✨', count: 0 }
])
const activeTab = ref('ai-enhanced')

const recommendations = reactive({
  aiEnhanced: [],
  personalized: [],
  hotDaily: [],
  hotWeekly: [],
  reputation: [],
  new: [],
  history: []
})

const loading = reactive({
  aiEnhanced: false,
  personalized: false,
  hotDaily: false,
  hotWeekly: false,
  reputation: false,
  new: false,
  history: false
})

// AI用户画像
const aiProfile = ref(null)
const loadingProfile = ref(false)

// 加载AI增强推荐
const loadAiEnhanced = async () => {
  loading.aiEnhanced = true
  try {
    const res = await getAiEnhancedRecommendations(12)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.aiEnhanced = res.data.map(item => ({
        id: item.id,
        name: item.name,
        cover: item.cover || '/default-script.jpg',
        categoryName: item.categoryName || '剧本杀',
        difficulty: item.difficulty || 2,
        playerCount: item.playerCount || 6,
        price: item.price || 0,
        rating: item.rating || 4.5,
        tags: item.tags || [],
        recommendReason: item.aiReason || item.recommendReason || 'AI智能推荐',
        aiReason: item.aiReason,
        aiRank: item.aiRank,
        recommendScore: item.recommendScore
      }))
      tabs.value[0].count = recommendations.aiEnhanced.length
    } else {
      await loadFallbackData('personalized')
      recommendations.aiEnhanced = recommendations.personalized
      tabs.value[0].count = recommendations.aiEnhanced.length
    }
  } catch (e) {
    console.warn('AI推荐加载失败，使用备用', e)
    await loadFallbackData('personalized')
    recommendations.aiEnhanced = recommendations.personalized
  } finally {
    loading.aiEnhanced = false
  }
}

// 加载AI用户画像
const loadAiProfile = async () => {
  loadingProfile.value = true
  try {
    const res = await getAiUserProfile()
    if ((res.code === 1 || res.code === 200) && res.data) {
      aiProfile.value = res.data
    }
  } catch (e) {
    console.warn('AI画像加载失败', e)
  } finally {
    loadingProfile.value = false
  }
}

// 统计数据
const totalScripts = computed(() => {
  return recommendations.personalized.length + 
         recommendations.hotDaily.length + 
         recommendations.hotWeekly.length + 
         recommendations.reputation.length + 
         recommendations.new.length
})

const userPreference = computed(() => {
  if (recommendations.personalized.length > 0) {
    const avgScore = recommendations.personalized.reduce((sum, s) => sum + (s.recommendScore || 80), 0) / recommendations.personalized.length
    return Math.round(avgScore) + '%'
  }
  return '计算中...'
})

// 难度文本
const getDifficultyText = (difficulty) => {
  const map = { 1: '新手', 2: '进阶', 3: '烧脑', 4: '硬核' }
  return map[difficulty] || '未知'
}

// 难度样式
const getDifficultyClass = (difficulty) => {
  const map = { 1: 'easy', 2: 'medium', 3: 'hard', 4: 'expert' }
  return map[difficulty] || ''
}

// 格式化推荐数据
const formatRecommendationData = (data) => {
  return (data || []).map(item => ({
    id: item.scriptId || item.id,
    name: item.scriptName || item.name,
    cover: item.cover || '/default-script.jpg',
    categoryName: item.categoryName || item.type || '剧本杀',
    difficulty: item.difficulty || 2,
    playerCount: item.playerCount || item.minPlayerCount || 6,
    price: item.price || 0,
    rating: item.rating || item.averageRating || 4.5,
    tags: item.tags || [],
    recommendReason: item.recommendReason || item.reason || '',
    recommendScore: item.recommendScore || item.matchScore || null,
    isHot: item.isHot || false,
    isNew: item.isNew || false
  }))
}

// 加载备用数据 - 直接从剧本列表获取
const loadFallbackData = async (type, orderBy = 'rating') => {
  try {
    const params = { page: 1, pageSize: 20 }
    // 根据类型设置不同的排序
    if (type === 'new') {
      params.orderBy = 'createTime'
      params.orderDir = 'desc'
    } else if (type === 'reputation') {
      params.orderBy = 'rating'
      params.orderDir = 'desc'
    }
    
    const res = await getScriptList(params)
    let records = []
    if (res.code === 1 && res.data) {
      records = res.data.records || res.data || []
    }
    
    if (records.length > 0) {
      const formattedData = records.map((item, index) => ({
        id: item.id,
        name: item.name,
        cover: item.cover || '/default-script.jpg',
        categoryName: item.categoryName || '剧本杀',
        difficulty: item.difficulty || 2,
        playerCount: item.playerCount || 6,
        price: item.price || 0,
        rating: item.rating || 4.5,
        tags: item.tags || [],
        recommendReason: getReasonByType(type),
        recommendScore: type === 'personalized' ? Math.floor(85 + Math.random() * 15) : null
      }))
      
      recommendations[type] = formattedData
      
      // 更新对应Tab的数量
      const tabIndex = tabs.value.findIndex(t => t.key === type || 
        (t.key === 'hot-daily' && type === 'hotDaily') ||
        (t.key === 'hot-weekly' && type === 'hotWeekly'))
      if (tabIndex !== -1) {
        tabs.value[tabIndex].count = formattedData.length
      }
    }
  } catch (err) {
    console.error('加载备用数据失败:', err)
  }
}

// 根据类型获取推荐理由
const getReasonByType = (type) => {
  const reasons = {
    personalized: '猜你喜欢',
    hotDaily: '今日热门',
    hotWeekly: '本周热门',
    reputation: '口碑佳作',
    new: '新品上架',
    history: '根据浏览历史'
  }
  return reasons[type] || '精选推荐'
}

// 加载个性化推荐
const loadPersonalized = async () => {
  loading.personalized = true
  try {
    const res = await getPersonalizedRecommendations(20)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.personalized = formatRecommendationData(res.data)
      tabs.value[0].count = recommendations.personalized.length
    } else {
      console.log('个性化推荐API无数据，使用备用方案')
      await loadFallbackData('personalized')
    }
  } catch (error) {
    console.error('加载个性化推荐失败:', error)
    await loadFallbackData('personalized')
  } finally {
    loading.personalized = false
  }
}

// 加载今日热门
const loadHotDaily = async () => {
  loading.hotDaily = true
  try {
    const res = await getHotRecommendations(1, 20)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.hotDaily = formatRecommendationData(res.data)
      tabs.value[1].count = recommendations.hotDaily.length
    } else {
      console.log('今日热门API无数据，使用备用方案')
      await loadFallbackData('hotDaily')
    }
  } catch (error) {
    console.error('加载今日热门失败:', error)
    await loadFallbackData('hotDaily')
  } finally {
    loading.hotDaily = false
  }
}

// 加载本周热门
const loadHotWeekly = async () => {
  loading.hotWeekly = true
  try {
    const res = await getHotRecommendations(2, 20)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.hotWeekly = formatRecommendationData(res.data)
      tabs.value[2].count = recommendations.hotWeekly.length
    } else {
      console.log('本周热门API无数据，使用备用方案')
      await loadFallbackData('hotWeekly')
    }
  } catch (error) {
    console.error('加载本周热门失败:', error)
    await loadFallbackData('hotWeekly')
  } finally {
    loading.hotWeekly = false
  }
}

// 加载口碑榜
const loadReputation = async () => {
  loading.reputation = true
  try {
    const res = await getHotRecommendations(4, 20)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.reputation = formatRecommendationData(res.data)
      tabs.value[3].count = recommendations.reputation.length
    } else {
      console.log('口碑榜API无数据，使用备用方案')
      await loadFallbackData('reputation')
    }
  } catch (error) {
    console.error('加载口碑榜失败:', error)
    await loadFallbackData('reputation')
  } finally {
    loading.reputation = false
  }
}

// 加载新品推荐
const loadNew = async () => {
  loading.new = true
  try {
    const res = await getNewScriptRecommendations(20)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.new = formatRecommendationData(res.data)
      tabs.value[4].count = recommendations.new.length
    } else {
      console.log('新品推荐API无数据，使用备用方案')
      await loadFallbackData('new')
    }
  } catch (error) {
    console.error('加载新品推荐失败:', error)
    await loadFallbackData('new')
  } finally {
    loading.new = false
  }
}

// 加载历史推荐（猜你喜欢）
const loadHistory = async () => {
  loading.history = true
  try {
    const res = await getHistoryBasedRecommendations(12)
    if ((res.code === 1 || res.code === 200) && res.data?.length > 0) {
      recommendations.history = formatRecommendationData(res.data)
    } else {
      // 历史推荐使用备用数据
      await loadFallbackData('history')
    }
  } catch (error) {
    console.error('加载历史推荐失败:', error)
    // 历史推荐失败时静默处理
  } finally {
    loading.history = false
  }
}

// 切换Tab
const switchTab = (tabKey) => {
  activeTab.value = tabKey
}

// 剧本点击
const handleScriptClick = async (script) => {
  try {
    await recordRecommendationClick(script.id)
  } catch (error) {
    console.error('记录点击失败', error)
  }
  router.push(`/script/${script.id}`)
}

// goToScript跳转
const goToScript = (id) => { router.push(`/script/${id}`) }

// 初始化
onMounted(async () => {
  // AI推荐和用户画像优先加载（并行）
  loadAiEnhanced()
  loadAiProfile()
  // 其他Tab数据后台加载
  Promise.all([
    loadPersonalized(),
    loadHotDaily(),
    loadHotWeekly(),
    loadReputation(),
    loadNew(),
    loadHistory()
  ])
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.recommend-page {
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
.particle:nth-child(10) { top: 25%; left: 15%; animation-delay: 9s; }
.particle:nth-child(11) { top: 85%; left: 45%; animation-delay: 10s; }
.particle:nth-child(12) { top: 5%; left: 55%; animation-delay: 11s; }
.particle:nth-child(13) { top: 75%; left: 25%; animation-delay: 12s; }
.particle:nth-child(14) { top: 45%; left: 85%; animation-delay: 13s; }
.particle:nth-child(15) { top: 95%; left: 5%; animation-delay: 14s; }

@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.3; }
  50% { transform: translateY(-30px) scale(1.5); opacity: 0.8; }
}

.floating-icons {
  position: absolute;
  width: 100%;
  height: 100%;
}

.float-icon {
  position: absolute;
  font-size: 24px;
  opacity: 0.1;
  animation: floatIcon 20s infinite ease-in-out;
}

.float-icon:nth-child(1) { top: 15%; left: 8%; animation-delay: 0s; }
.float-icon:nth-child(2) { top: 25%; left: 85%; animation-delay: -4s; }
.float-icon:nth-child(3) { top: 55%; left: 12%; animation-delay: -8s; }
.float-icon:nth-child(4) { top: 65%; left: 78%; animation-delay: -12s; }
.float-icon:nth-child(5) { top: 35%; left: 45%; animation-delay: -16s; }
.float-icon:nth-child(6) { top: 85%; left: 32%; animation-delay: -20s; }

@keyframes floatIcon {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(15deg); }
}

/* ========== 页面头部 ========== */
.page-header {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 24px;
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
  font-size: 52px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.title-text h1 {
  margin: 0;
  font-size: 32px;
  color: #fff;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
}

.title-text p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
}

.header-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #ffd700;
  text-shadow: 0 0 15px rgba(255, 215, 0, 0.4);
}

.stat-label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

/* ========== Tab切换 ========== */
.category-tabs {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.category-tabs::-webkit-scrollbar {
  height: 4px;
}

.category-tabs::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.category-tabs::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 30px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.tab-btn.active {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 20px rgba(192, 57, 43, 0.4);
}

.tab-icon {
  font-size: 18px;
}

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* ========== 内容区域 ========== */
.content-area {
  position: relative;
  z-index: 1;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ========== 区域介绍 ========== */
.section-intro {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(192, 57, 43, 0.1);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.section-intro.hot {
  background: rgba(255, 107, 107, 0.1);
  border-color: rgba(255, 107, 107, 0.2);
}

.section-intro.weekly {
  background: rgba(46, 213, 115, 0.1);
  border-color: rgba(46, 213, 115, 0.2);
}

.section-intro.reputation {
  background: rgba(255, 215, 0, 0.1);
  border-color: rgba(255, 215, 0, 0.2);
}

.section-intro.new {
  background: rgba(155, 89, 182, 0.1);
  border-color: rgba(155, 89, 182, 0.2);
}

.intro-icon {
  font-size: 40px;
}

.intro-text h3 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #fff;
}

.intro-text p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* ========== 剧本网格 ========== */
.script-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  min-height: 300px;
}

.script-grid :deep(.el-loading-mask) {
  background: rgba(26, 26, 46, 0.8);
}

/* ========== 剧本卡片 ========== */
.script-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
  position: relative;
  animation: cardAppear 0.5s ease-out backwards;
}

@keyframes cardAppear {
  from { opacity: 0; transform: translateY(30px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.script-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(192, 57, 43, 0.2);
  border-color: rgba(192, 57, 43, 0.3);
}

/* 排名徽章 */
.card-rank {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 4px;
}

.rank-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
}

.rank-badge.rank-1 { background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%); color: #1a1a2e; }
.rank-badge.rank-2 { background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%); color: #1a1a2e; }
.rank-badge.rank-3 { background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%); color: #fff; }
.rank-badge.gold { background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%); color: #1a1a2e; }

.hot-fire {
  font-size: 16px;
  animation: fireFlicker 0.5s infinite alternate;
}

@keyframes fireFlicker {
  from { transform: scale(1); }
  to { transform: scale(1.2); }
}

/* 卡片封面 */
.card-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
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
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, transparent 50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
  font-size: 14px;
}

.script-card:hover .cover-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 32px;
}

/* 匹配度分数 */
.match-score {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-icon {
  font-size: 14px;
}

/* 推荐原因 */
.recommend-reason {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #ffd700;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

/* 热门徽章 */
.hot-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

/* 新品徽章 */
.new-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

/* 口碑徽章 */
.quality-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%);
  color: #1a1a2e;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

/* 卡片内容 */
.card-body {
  padding: 16px;
}

.card-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.tag.category {
  background: rgba(192, 57, 43, 0.2);
  color: #a0b4ff;
}

.tag.players {
  background: rgba(46, 213, 115, 0.2);
  color: #7bed9f;
}

.tag.difficulty {
  background: rgba(255, 255, 255, 0.1);
}

.tag.difficulty.easy { color: #2ed573; }
.tag.difficulty.medium { color: #ffa502; }
.tag.difficulty.hard { color: #ff6b6b; }
.tag.difficulty.expert { color: #e056fd; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating .stars {
  color: #ffd700;
  font-size: 16px;
}

.rating .score {
  color: #ffd700;
  font-weight: bold;
  font-size: 15px;
}

.rating.highlight .score {
  font-size: 18px;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
}

.price {
  font-size: 20px;
  font-weight: bold;
  color: #ff6b6b;
}

.price span {
  font-size: 12px;
  font-weight: normal;
  color: rgba(255, 255, 255, 0.5);
}

/* ========== 空状态 ========== */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* ========== 猜你喜欢 ========== */
.history-section {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 24px;
  margin-top: 30px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-icon {
  font-size: 28px;
}

.section-header h2 {
  margin: 0;
  font-size: 22px;
  color: #fff;
}

.section-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: auto;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.history-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.history-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

.history-card img {
  width: 100%;
  height: 100px;
  object-fit: cover;
}

.history-info {
  padding: 12px;
}

.history-info h5 {
  margin: 0 0 6px;
  font-size: 13px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-price {
  font-size: 14px;
  color: #ff6b6b;
  font-weight: bold;
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
  .recommend-page {
    padding: 12px;
  }

  .page-header {
    padding: 20px;
  }

  .header-content {
    flex-direction: column;
    text-align: center;
  }

  .title-section {
    flex-direction: column;
    gap: 12px;
  }

  .title-icon {
    font-size: 40px;
  }

  .title-text h1 {
    font-size: 24px;
  }

  .header-stats {
    width: 100%;
    justify-content: center;
  }

  .category-tabs {
    gap: 8px;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 13px;
  }

  .script-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .card-cover {
    height: 140px;
  }

  .card-body {
    padding: 12px;
  }

  .card-title {
    font-size: 14px;
  }

  .section-intro {
    flex-direction: column;
    text-align: center;
    gap: 12px;
    padding: 16px;
  }

  .history-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .script-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .card-cover {
    height: 120px;
  }

  .history-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
/* ===== AI推荐样式 ===== */
.ai-profile-card {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  border: 1px solid rgba(99, 179, 237, 0.3);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.ai-profile-card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(99,179,237,0.05) 0%, transparent 60%);
  animation: aiGlow 4s ease-in-out infinite;
}
@keyframes aiGlow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}
.ai-profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.ai-badge {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}
.ai-profile-title {
  color: #e2e8f0;
  font-size: 15px;
  font-weight: 600;
}
.ai-profile-desc {
  color: #a0aec0;
  font-size: 14px;
  line-height: 1.8;
  position: relative;
  z-index: 1;
}
.ai-typing {
  color: #63b3ed;
  animation: blink 1s ease-in-out infinite;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0.4} }

.section-intro.ai {
  background: linear-gradient(135deg, rgba(102,126,234,0.15), rgba(118,75,162,0.15));
  border-left-color: #667eea;
}

/* AI推荐理由标签 */
.ai-reason-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, rgba(102,126,234,0.15), rgba(118,75,162,0.15));
  border: 1px solid rgba(102,126,234,0.3);
  border-radius: 20px;
  padding: 3px 10px;
  font-size: 11px;
  color: #667eea;
  margin: 6px 0;
  width: fit-content;
  max-width: 100%;
}
.ai-icon-small { font-size: 11px; }

/* AI排名角标 */
.card-rank {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 26px;
  height: 26px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(102,126,234,0.5);
}

.empty-tip {
  grid-column: 1 / -1;
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
}
</style>
