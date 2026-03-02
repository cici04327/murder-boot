<template>
  <div class="recommend-container">
    <!-- 华丽的动画背景 -->
    <div class="animated-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="floating-particles">
        <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
    </div>

    <!-- 个性化推荐头部 - 华丽版 -->
    <div class="recommend-header">
      <div class="header-content">
        <div class="header-icon">
          <div class="icon-circle">
            <el-icon class="magic-icon"><MagicStick /></el-icon>
          </div>
        </div>
        <h1 class="header-title">
          <span class="gradient-text">为你推荐</span>
          <span class="subtitle">AI 智能推荐系统</span>
        </h1>
        <p class="header-description">
          基于你的浏览历史、偏好和行为，为你精准推荐最适合的剧本
        </p>
        <div class="header-stats">
          <div class="stat-item">
            <div class="stat-number">{{ recommendedScripts.length }}</div>
            <div class="stat-label">为你推荐</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ browseHistory.length }}</div>
            <div class="stat-label">浏览历史</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userPreferences.length }}</div>
            <div class="stat-label">偏好标签</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户偏好标签 -->
    <el-card class="preference-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><UserFilled /></el-icon> 你的偏好标签</span>
          <el-button type="primary" size="small" @click="showPreferenceDialog = true">
            编辑偏好
          </el-button>
        </div>
      </template>
      <div class="tags-container">
        <el-tag
          v-for="tag in userPreferences"
          :key="tag"
          :type="getTagType(tag)"
          size="large"
          effect="dark"
          round
        >
          {{ tag }}
        </el-tag>
        <el-tag v-if="userPreferences.length === 0" type="info" size="large">
          暂无偏好标签，系统将为你推荐热门内容
        </el-tag>
      </div>
    </el-card>

    <!-- 智能推荐理由 -->
    <div class="recommend-reason" v-if="recommendReason">
      <el-alert :title="recommendReason" type="success" :closable="false" show-icon />
    </div>

    <!-- 推荐剧本 - 华丽版 -->
    <div class="section scripts-section">
      <div class="section-header">
        <div class="section-title-group">
          <el-icon class="section-icon"><Trophy /></el-icon>
          <h2 class="section-title">为你精选的剧本</h2>
        </div>
        <el-button 
          type="primary" 
          round 
          @click="refreshRecommendations"
          :loading="scriptsLoading"
        >
          <el-icon><Refresh /></el-icon> 换一批
        </el-button>
      </div>
      
      <div class="scripts-grid" v-loading="scriptsLoading">
        <transition-group name="card-list">
          <div 
            v-for="(script, index) in recommendedScripts" 
            :key="script.id"
            class="script-card-wrapper"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="script-card glass-card" @click="goToScript(script.id)">
              <!-- 匹配度环形进度 -->
              <div class="match-ring" v-if="script.matchScore">
                <svg class="ring-svg" viewBox="0 0 100 100">
                  <circle class="ring-bg" cx="50" cy="50" r="45"></circle>
                  <circle 
                    class="ring-progress" 
                    cx="50" 
                    cy="50" 
                    r="45"
                    :style="{ strokeDashoffset: 283 - (283 * script.matchScore / 100) }"
                  ></circle>
                </svg>
                <div class="ring-text">
                  <span class="ring-number">{{ script.matchScore }}</span>
                  <span class="ring-label">%</span>
                </div>
              </div>

              <!-- 封面图片 -->
              <div class="script-image">
                <img :src="script.cover || PLACEHOLDERS.SCRIPT" alt="封面" />
                <div class="image-overlay">
                  <div class="overlay-content">
                    <el-icon class="view-icon"><View /></el-icon>
                    <span>查看详情</span>
                  </div>
                </div>
                <!-- 热门标签 -->
                <div class="hot-badge" v-if="script.isHot">
                  <el-icon><TrendCharts /></el-icon>
                  <span>热门</span>
                </div>
              </div>

              <!-- 剧本信息 -->
              <div class="script-content">
                <h3 class="script-name">{{ script.name }}</h3>
                
                <!-- 标签组 -->
                <div class="script-tags-modern">
                  <span class="tag tag-primary" v-if="script.difficulty">
                    <el-icon><StarFilled /></el-icon>
                    {{ getDifficultyText(script.difficulty) }}
                  </span>
                  <span class="tag tag-success" v-if="script.playerCount">
                    <el-icon><UserFilled /></el-icon>
                    {{ script.playerCount }}人
                  </span>
                  <span class="tag tag-warning">
                    <el-icon><CollectionTag /></el-icon>
                    {{ script.type || '其他' }}
                  </span>
                </div>

                <!-- 推荐理由 -->
                <div class="recommend-reason-box">
                  <el-icon class="reason-icon"><MagicStick /></el-icon>
                  <span class="reason-text">{{ script.recommendReason || '根据你的偏好推荐' }}</span>
                </div>

                <!-- 底部信息 -->
                <div class="script-footer-modern">
                  <div class="price-box">
                    <span class="price-label">每人</span>
                    <span class="price-value">¥{{ script.price }}</span>
                  </div>
                  <div class="rating-box">
                    <el-rate 
                      v-model="script.rating" 
                      disabled 
                      size="small"
                      :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                    />
                    <span class="rating-text">{{ script.rating || 5.0 }}</span>
                  </div>
                </div>
              </div>

              <!-- 悬浮动作按钮 -->
              <div class="card-actions">
                <el-button circle size="small" @click.stop="toggleFavorite(script)">
                  <el-icon><StarFilled v-if="script.isFavorited" /><Star v-else /></el-icon>
                </el-button>
                <el-button circle size="small" @click.stop="shareScript(script)">
                  <el-icon><Share /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </transition-group>
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-if="!scriptsLoading && recommendedScripts.length === 0"
        description="暂无推荐剧本，去浏览一些剧本建立你的偏好吧"
      >
        <el-button type="primary" @click="$router.push('/script')">浏览剧本</el-button>
      </el-empty>
    </div>

    <!-- 推荐门店 -->
    <div class="section">
      <div class="section-header">
        <h2>🏪 为你推荐的门店</h2>
      </div>
      
      <el-row :gutter="20" v-loading="storesLoading">
        <el-col :xs="24" :sm="12" :md="8" v-for="store in recommendedStores" :key="store.id">
          <el-card class="store-card" shadow="hover" @click="goToStore(store.id)">
            <div class="store-header">
              <img :src="store.logo || PLACEHOLDERS.AVATAR" alt="门店logo" class="store-logo" />
              <div class="store-info">
                <h3>{{ store.name }}</h3>
                <div class="store-location">
                  <el-icon><Location /></el-icon>
                  {{ store.address }}
                </div>
              </div>
            </div>
            <div class="store-reason">
              <el-icon><InfoFilled /></el-icon>
              {{ store.recommendReason || '附近热门门店' }}
            </div>
            <div class="store-stats">
              <div class="stat-item">
                <el-icon><Star /></el-icon>
                {{ store.rating || 4.5 }}
              </div>
              <div class="stat-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ store.reviewCount || 0 }} 评价
              </div>
              <div class="stat-item">
                <el-icon><Location /></el-icon>
                {{ store.distance || '未知' }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 浏览历史 -->
    <div class="section" v-if="browseHistory.length > 0">
      <div class="section-header">
        <h2>🕐 最近浏览</h2>
        <el-button text type="danger" @click="clearHistory">
          <el-icon><Delete /></el-icon> 清空历史
        </el-button>
      </div>
      
      <div class="history-list">
        <el-card
          v-for="item in browseHistory"
          :key="item.id"
          class="history-item"
          shadow="hover"
          @click="goToScript(item.id)"
        >
          <div class="history-content">
            <img :src="item.cover || '/default-script.jpg'" alt="封面" />
            <div class="history-info">
              <h4>{{ item.name }}</h4>
              <p class="history-time">{{ formatTime(item.viewTime) }}</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 编辑偏好对话框 -->
    <el-dialog v-model="showPreferenceDialog" title="编辑偏好标签" width="600px">
      <div class="preference-editor">
        <p>选择你感兴趣的标签，系统将为你推荐相关内容</p>
        <div class="tag-groups">
          <div class="tag-group">
            <h4>剧本类型</h4>
            <el-checkbox-group v-model="selectedPreferences.types">
              <el-checkbox label="悬疑">悬疑</el-checkbox>
              <el-checkbox label="恐怖">恐怖</el-checkbox>
              <el-checkbox label="情感">情感</el-checkbox>
              <el-checkbox label="欢乐">欢乐</el-checkbox>
              <el-checkbox label="推理">推理</el-checkbox>
              <el-checkbox label="机制">机制</el-checkbox>
              <el-checkbox label="还原">还原</el-checkbox>
              <el-checkbox label="硬核">硬核</el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="tag-group">
            <h4>难度偏好</h4>
            <el-checkbox-group v-model="selectedPreferences.difficulties">
              <el-checkbox label="简单">简单</el-checkbox>
              <el-checkbox label="普通">普通</el-checkbox>
              <el-checkbox label="困难">困难</el-checkbox>
              <el-checkbox label="极难">极难</el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="tag-group">
            <h4>人数偏好</h4>
            <el-checkbox-group v-model="selectedPreferences.playerCounts">
              <el-checkbox label="4-6人">4-6人</el-checkbox>
              <el-checkbox label="6-8人">6-8人</el-checkbox>
              <el-checkbox label="8-10人">8-10人</el-checkbox>
              <el-checkbox label="10人以上">10人以上</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPreferenceDialog = false">取消</el-button>
        <el-button type="primary" @click="savePreferences">保存偏好</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PLACEHOLDERS } from '@/assets/placeholders'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  Refresh,
  Star,
  InfoFilled,
  Location,
  ChatDotRound,
  Delete,
  MagicStick,
  Trophy,
  TrendCharts,
  StarFilled,
  View,
  CollectionTag,
  Share
} from '@element-plus/icons-vue'
import { getRecommendedScripts, getHotScripts, favoriteScript, unfavoriteScript } from '@/api/script'
import { getHotStores } from '@/api/store'
import { 
  getPersonalizedRecommendations, 
  getHistoryBasedRecommendations, 
  getHotRecommendations 
} from '@/api/recommendation'
import { useUserStore } from '@/store/user'

const router = useRouter()

// 数据状态
const scriptsLoading = ref(false)
const storesLoading = ref(false)
const userPreferences = ref([])
const recommendReason = ref('基于你的浏览历史和偏好为你推荐')
const recommendedScripts = ref([])
const recommendedStores = ref([])
const browseHistory = ref([])
const showPreferenceDialog = ref(false)

// 偏好设置
const selectedPreferences = reactive({
  types: [],
  difficulties: [],
  playerCounts: []
})

// 获取标签类型
const getTagType = (tag) => {
  const types = {
    '悬疑': 'warning',
    '恐怖': 'danger',
    '情感': 'success',
    '欢乐': 'primary',
    '推理': 'info',
    '简单': 'success',
    '普通': 'primary',
    '困难': 'warning',
    '极难': 'danger'
  }
  return types[tag] || ''
}

// 获取难度文本
const getDifficultyText = (difficulty) => {
  const map = { 1: '简单', 2: '普通', 3: '困难', 4: '极难' }
  return map[difficulty] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

// 生成粒子样式
const getParticleStyle = (index) => {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const animationDuration = Math.random() * 10 + 10
  const animationDelay = Math.random() * 5
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDuration: `${animationDuration}s`,
    animationDelay: `${animationDelay}s`
  }
}

// 跳转到剧本详情
const goToScript = (id) => {
  // 保存浏览历史
  saveBrowseHistory(id)
  router.push(`/script/${id}`)
}

// 收藏/取消收藏
const toggleFavorite = async (script) => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (script.isFavorited) {
      await unfavoriteScript(script.id)
      script.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteScript(script.id)
      script.isFavorited = true
      ElMessage.success('收藏成功！')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 分享剧本
const shareScript = (script) => {
  const shareUrl = `${window.location.origin}/script/${script.id}`
  
  if (navigator.share) {
    navigator.share({
      title: script.name,
      text: `推荐你看这个剧本：${script.name}`,
      url: shareUrl
    }).catch(() => {
      copyToClipboard(shareUrl)
    })
  } else {
    copyToClipboard(shareUrl)
  }
}

// 复制到剪贴板
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

// 保存浏览历史
const saveBrowseHistory = (scriptId) => {
  try {
    const script = recommendedScripts.value.find(s => s.id === scriptId)
    if (!script) return
    
    let history = []
    const savedHistory = localStorage.getItem('browseHistory')
    if (savedHistory) {
      history = JSON.parse(savedHistory)
    }
    
    // 添加新记录（去重）
    history = history.filter(item => item.id !== scriptId)
    history.unshift({
      id: script.id,
      name: script.name,
      cover: script.cover,
      viewTime: new Date().toISOString()
    })
    
    // 只保留最近20条
    history = history.slice(0, 20)
    
    localStorage.setItem('browseHistory', JSON.stringify(history))
  } catch (error) {
    console.error('保存浏览历史失败:', error)
  }
}

// 跳转到门店详情
const goToStore = (id) => {
  router.push(`/store/detail/${id}`)
}

// 刷新推荐
const refreshRecommendations = () => {
  loadRecommendedScripts()
  ElMessage.success('已为你刷新推荐')
}

// 清空历史
const clearHistory = () => {
  localStorage.removeItem('browseHistory')
  browseHistory.value = []
  ElMessage.success('浏览历史已清空')
}

// 保存偏好
const savePreferences = () => {
  const preferences = [
    ...selectedPreferences.types,
    ...selectedPreferences.difficulties,
    ...selectedPreferences.playerCounts
  ]
  userPreferences.value = preferences
  localStorage.setItem('userPreferences', JSON.stringify(preferences))
  localStorage.setItem('selectedPreferences', JSON.stringify(selectedPreferences))
  showPreferenceDialog.value = false
  loadRecommendedScripts()
  ElMessage.success('偏好设置已保存')
}

// 加载推荐剧本（使用后端推荐API）
const loadRecommendedScripts = async () => {
  scriptsLoading.value = true
  try {
    const userStore = useUserStore()
    let recommendedList = []
    
    // 如果用户已登录，获取个性化推荐
    if (userStore.isLoggedIn) {
      try {
        console.log('用户已登录，获取个性化推荐')
        const res = await getPersonalizedRecommendations(12)
        if (res.code === 1 && res.data && res.data.length > 0) {
          recommendedList = res.data
          console.log('个性化推荐成功，数量:', recommendedList.length)
        }
      } catch (error) {
        console.warn('个性化推荐失败，尝试历史推荐:', error)
        // 尝试基于历史的推荐
        try {
          const res = await getHistoryBasedRecommendations(12)
          if (res.code === 1 && res.data && res.data.length > 0) {
            recommendedList = res.data
            console.log('历史推荐成功，数量:', recommendedList.length)
          }
        } catch (error2) {
          console.warn('历史推荐失败:', error2)
        }
      }
    }
    
    // 如果还没有推荐结果，使用热门推荐
    if (recommendedList.length === 0) {
      console.log('使用热门推荐')
      try {
        const res = await getHotRecommendations(1, 12) // 今日热门
        if (res.code === 1 && res.data) {
          recommendedList = res.data
          console.log('热门推荐成功，数量:', recommendedList.length)
        }
      } catch (error) {
        console.error('热门推荐失败:', error)
      }
    }
    
    // 如果还是没有数据，使用备用方案
    if (recommendedList.length === 0) {
      console.log('使用备用方案，获取热门剧本列表')
      const res = await getHotScripts(12)
      if (res.code === 1 && res.data) {
        recommendedList = res.data.map(script => ({
          scriptId: script.id,
          scriptName: script.name,
          coverImage: script.cover,
          categoryName: script.categoryName,
          difficulty: script.difficulty,
          playerCount: script.playerCount,
          price: script.price,
          rating: script.rating || 4.5,
          matchScore: 75,
          reason: '热门推荐'
        }))
      }
    }
    
    console.log('最终推荐数量:', recommendedList.length)
    
    if (recommendedList.length === 0) {
      ElMessage.warning('暂无可推荐的剧本')
      return
    }
    
    // 转换为页面需要的格式
    recommendedScripts.value = recommendedList.map((item, index) => {
      // 前30%标记为热门
      const isHot = index < Math.ceil(recommendedList.length * 0.3)
      
      return {
        id: item.scriptId || item.id,
        name: item.scriptName || item.name,
        cover: item.cover || '/default-script.jpg',
        type: item.categoryName || item.type || '其他',
        difficulty: item.difficulty || 2,
        playerCount: item.playerCount || 6,
        price: item.price || 0,
        rating: item.rating || 4.5,
        matchScore: item.matchScore || 75,
        recommendReason: item.reason || '为你推荐',
        isHot: isHot,
        isFavorited: false
      }
    })
    
    console.log('推荐结果:', recommendedScripts.value.length, '个剧本')
  } catch (error) {
    console.error('加载推荐失败:', error)
    ElMessage.error('加载推荐失败，请刷新重试')
  } finally {
    scriptsLoading.value = false
  }
}

// 基于浏览历史获取推荐（协同过滤 + 内容相似度）
const getRecommendationsBasedOnHistory = async (history, allScripts) => {
  // 分析浏览历史中的剧本特征
  const historyIds = history.map(h => h.id)
  const viewedScripts = allScripts.filter(s => historyIds.includes(s.id))
  
  if (viewedScripts.length === 0) {
    return allScripts.slice(0, 12)
  }
  
  // 统计用户偏好
  const preferences = analyzeUserPreferences(viewedScripts)
  console.log('用户偏好分析:', preferences)
  
  // 计算每个剧本与用户偏好的相似度
  const scoredScripts = allScripts.map(script => ({
    ...script,
    similarityScore: calculateSimilarityScore(script, preferences, viewedScripts)
  }))
  
  // 按相似度排序
  scoredScripts.sort((a, b) => b.similarityScore - a.similarityScore)
  
  return scoredScripts.slice(0, 20) // 返回前20个最相似的
}

// 分析用户偏好
const analyzeUserPreferences = (viewedScripts) => {
  const preferences = {
    categories: {},
    difficulties: {},
    playerCounts: {},
    avgRating: 0,
    avgPrice: 0
  }
  
  viewedScripts.forEach(script => {
    // 统计类型偏好
    const category = script.categoryName || script.type || '其他'
    preferences.categories[category] = (preferences.categories[category] || 0) + 1
    
    // 统计难度偏好
    const difficulty = script.difficulty || 2
    preferences.difficulties[difficulty] = (preferences.difficulties[difficulty] || 0) + 1
    
    // 统计人数偏好
    const playerCount = script.playerCount || 6
    preferences.playerCounts[playerCount] = (preferences.playerCounts[playerCount] || 0) + 1
    
    // 累计评分和价格
    preferences.avgRating += (script.rating || 0)
    preferences.avgPrice += (script.price || 0)
  })
  
  // 计算平均值
  preferences.avgRating /= viewedScripts.length
  preferences.avgPrice /= viewedScripts.length
  
  // 找出最喜欢的类型
  preferences.favoriteCategory = Object.keys(preferences.categories).reduce((a, b) => 
    preferences.categories[a] > preferences.categories[b] ? a : b
  , '')
  
  return preferences
}

// 计算相似度得分
const calculateSimilarityScore = (script, preferences, viewedScripts) => {
  let score = 0
  
  // 1. 类型相似度（权重40%）
  const category = script.categoryName || script.type || '其他'
  if (preferences.categories[category]) {
    score += 40 * (preferences.categories[category] / viewedScripts.length)
  }
  
  // 2. 难度相似度（权重20%）
  const difficulty = script.difficulty || 2
  if (preferences.difficulties[difficulty]) {
    score += 20 * (preferences.difficulties[difficulty] / viewedScripts.length)
  }
  
  // 3. 人数相似度（权重15%）
  const playerCount = script.playerCount || 6
  if (preferences.playerCounts[playerCount]) {
    score += 15 * (preferences.playerCounts[playerCount] / viewedScripts.length)
  }
  
  // 4. 评分相似度（权重15%）
  const ratingDiff = Math.abs((script.rating || 0) - preferences.avgRating)
  score += 15 * (1 - ratingDiff / 5) // 评分差异越小分数越高
  
  // 5. 价格相似度（权重10%）
  const priceDiff = Math.abs((script.price || 0) - preferences.avgPrice)
  const maxPriceDiff = Math.max(preferences.avgPrice, 200)
  score += 10 * (1 - Math.min(priceDiff / maxPriceDiff, 1))
  
  return score
}

// 计算匹配度评分（基于浏览历史）
const calculateMatchScore = (script, history = []) => {
  let score = 50 // 基础分
  
  // 如果有浏览历史，基于历史计算
  if (history.length > 0) {
    // 获取相似度分数（来自推荐算法）
    if (script.similarityScore !== undefined) {
      // 将相似度分数(0-100)转换为匹配度(70-99)
      score = 70 + (script.similarityScore / 100) * 29
    }
  }
  
  // 根据用户偏好标签进一步调整
  if (userPreferences.value.length > 0) {
    const category = script.categoryName || script.type
    if (userPreferences.value.includes(category)) {
      score += 5
    }
    
    const difficultyMap = { 1: '简单', 2: '普通', 3: '困难', 4: '极难' }
    if (userPreferences.value.includes(difficultyMap[script.difficulty])) {
      score += 3
    }
  }
  
  // 添加一些随机性使推荐更自然
  score += Math.floor(Math.random() * 5)
  
  return Math.min(99, Math.max(70, Math.floor(score)))
}

// 获取推荐理由（基于浏览历史）
const getRecommendReason = (script, matchScore, history = []) => {
  const category = script.categoryName || script.type || '其他'
  
  // 如果有浏览历史
  if (history.length > 0) {
    if (matchScore >= 90) {
      return `与你浏览的剧本高度相似`
    } else if (matchScore >= 85) {
      return `你可能喜欢这个${category}剧本`
    } else if (matchScore >= 80) {
      return `与你最近浏览的剧本相似`
    } else if (matchScore >= 75) {
      return `根据你的浏览记录推荐`
    }
  }
  
  // 没有历史时的推荐理由
  if (script.rating >= 4.7) {
    return '高评分热门剧本'
  } else if (userPreferences.value.includes(category)) {
    return `你感兴趣的${category}类型`
  } else {
    return '为你推荐的优质剧本'
  }
}

// 加载推荐门店
const loadRecommendedStores = async () => {
  storesLoading.value = true
  try {
    const res = await getHotStores()
    if (res.code === 1 && res.data) {
      // 获取用户位置（如果允许）
      const userLocation = await getUserLocation()
      
      recommendedStores.value = res.data.map((store, index) => {
        // 计算距离
        const distance = calculateDistance(userLocation, {
          latitude: store.latitude,
          longitude: store.longitude
        })
        
        // 生成推荐理由
        const recommendReason = getStoreRecommendReason(store, distance, index)
        
        return {
          id: store.id,
          name: store.name,
          logo: store.logo || '/default-avatar.jpg',
          address: store.address || '地址未知',
          rating: store.rating || 4.5,
          reviewCount: store.reviewCount || 0,
          distance: distance,
          recommendReason: recommendReason
        }
      }).slice(0, 6)
    }
  } catch (error) {
    console.error('加载推荐门店失败:', error)
  } finally {
    storesLoading.value = false
  }
}

// 获取用户位置
const getUserLocation = () => {
  return new Promise((resolve) => {
    // 尝试从localStorage获取上次保存的位置
    const savedLocation = localStorage.getItem('userLocation')
    if (savedLocation) {
      resolve(JSON.parse(savedLocation))
      return
    }
    
    // 尝试使用浏览器定位API
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          }
          // 保存位置
          localStorage.setItem('userLocation', JSON.stringify(location))
          resolve(location)
        },
        () => {
          // 定位失败，使用默认位置（北京天安门）
          resolve({ latitude: 39.9042, longitude: 116.4074 })
        }
      )
    } else {
      // 不支持定位，使用默认位置
      resolve({ latitude: 39.9042, longitude: 116.4074 })
    }
  })
}

// 计算两点之间的距离（使用Haversine公式）
const calculateDistance = (point1, point2) => {
  if (!point1 || !point2 || !point2.latitude || !point2.longitude) {
    return '未知'
  }
  
  const R = 6371 // 地球半径（公里）
  const lat1 = point1.latitude * Math.PI / 180
  const lat2 = point2.latitude * Math.PI / 180
  const deltaLat = (point2.latitude - point1.latitude) * Math.PI / 180
  const deltaLon = (point2.longitude - point1.longitude) * Math.PI / 180
  
  const a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1) * Math.cos(lat2) *
            Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  const distance = R * c
  
  if (distance < 1) {
    return `${Math.round(distance * 1000)}m`
  } else {
    return `${distance.toFixed(1)}km`
  }
}

// 获取门店推荐理由
const getStoreRecommendReason = (store, distance, index) => {
  if (distance !== '未知' && parseFloat(distance) < 2) {
    return '距离你最近的高评分门店'
  } else if (store.rating >= 4.8) {
    return '高评分优质门店'
  } else if (index < 3) {
    return '本周热门推荐'
  } else {
    return '用户好评推荐'
  }
}

// 加载浏览历史
const loadBrowseHistory = () => {
  const history = localStorage.getItem('browseHistory')
  if (history) {
    browseHistory.value = JSON.parse(history).slice(0, 10)
  }
}

// 加载用户偏好
const loadUserPreferences = () => {
  const preferences = localStorage.getItem('userPreferences')
  const selected = localStorage.getItem('selectedPreferences')
  
  if (preferences) {
    userPreferences.value = JSON.parse(preferences)
  }
  
  if (selected) {
    const data = JSON.parse(selected)
    selectedPreferences.types = data.types || []
    selectedPreferences.difficulties = data.difficulties || []
    selectedPreferences.playerCounts = data.playerCounts || []
  }
}

// 页面加载
onMounted(() => {
  loadUserPreferences()
  loadRecommendedScripts()
  loadRecommendedStores()
  loadBrowseHistory()
})
</script>

<style scoped>
/* ===== 华丽推荐页面完整样式 ===== */

/* 容器基础样式 */
.recommend-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e1e2f 0%, #2a2a40 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* ===== 动画背景系统 ===== */
.animated-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: float 20s ease-in-out infinite;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  top: -200px;
  left: -200px;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  top: 50%;
  right: -150px;
  animation-delay: 7s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  bottom: -100px;
  left: 50%;
  animation-delay: 14s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

.floating-particles {
  position: absolute;
  width: 100%;
  height: 100%;
  display: none; /* 隐藏粒子效果 */
}

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: particle-float linear infinite;
}

@keyframes particle-float {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) rotate(360deg);
    opacity: 0;
  }
}

/* ===== 华丽头部设计 ===== */
.recommend-header {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 60px 20px;
  margin-bottom: 40px;
}

.header-content {
  max-width: 800px;
  margin: 0 auto;
}

.header-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.icon-circle {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 15px 40px rgba(102, 126, 234, 0.6);
  }
}

.magic-icon {
  font-size: 48px;
  color: white;
  animation: rotate 3s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.header-title {
  margin-bottom: 20px;
}

.gradient-text {
  display: block;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 10px;
  animation: gradient-shift 3s ease infinite;
  background-size: 200% 200%;
}

@keyframes gradient-shift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.subtitle {
  display: block;
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;
  font-weight: normal;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.header-description {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 30px;
}

.header-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 30px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  background: linear-gradient(135deg, #FFD04B, #FF9900);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 5px;
}

.stat-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.preference-card {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.recommend-reason {
  margin-bottom: 30px;
}

.section {
  margin-bottom: 50px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 24px;
  color: #303133;
}

.script-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 15px;
}

.script-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.script-image {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 12px;
}

.script-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.match-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 208, 75, 0.95);
  color: #303133;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
}

.script-info h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.script-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.script-reason {
  font-size: 13px;
  color: #67C23A;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.script-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 20px;
  color: #F56C6C;
  font-weight: bold;
}

.store-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.store-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.store-header {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.store-logo {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.store-info h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: #303133;
}

.store-location {
  font-size: 14px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 5px;
}

.store-reason {
  font-size: 13px;
  color: #67C23A;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.store-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 15px;
  border-top: 1px solid #EBEEF5;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.history-item {
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.history-content {
  display: flex;
  gap: 15px;
  align-items: center;
}

.history-content img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.history-info h4 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #303133;
}

.history-time {
  font-size: 12px;
  color: #909399;
}

.preference-editor p {
  margin-bottom: 20px;
  color: #909399;
}

.tag-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tag-group h4 {
  margin-bottom: 10px;
  color: #303133;
}

@media (max-width: 768px) {
  .recommend-header h1 {
    font-size: 28px;
  }
  
  .section-header h2 {
    font-size: 20px;
  }
  
  .history-list {
    grid-template-columns: 1fr;
  }
}
</style>
