<template>
  <div class="script-detail-container" v-loading="loading">
    <!-- 沉浸式头部横幅 -->
    <section class="script-hero-banner" v-if="script">
      <div class="hero-bg" :style="{ backgroundImage: `url(${script.cover || PLACEHOLDERS.SCRIPT})` }"></div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <!-- 面包屑导航 -->
        <nav class="breadcrumb-hero">
          <router-link to="/" class="breadcrumb-link">🏠 首页</router-link>
          <span class="sep">/</span>
          <router-link to="/script" class="breadcrumb-link">📚 剧本库</router-link>
          <span class="sep">/</span>
          <span class="current">{{ script?.name }}</span>
        </nav>
        
        <div class="hero-main">
          <div class="hero-tags">
            <span class="hero-tag category">{{ script.categoryName }}</span>
            <span class="hero-tag difficulty" :class="'diff-' + script.difficulty">
              {{ getDifficultyText(script.difficulty) }}
            </span>
            <span class="hero-tag" v-if="script.isHot">🔥 热门</span>
            <span class="hero-tag" v-if="script.isNew">✨ 新本</span>
          </div>
          <h1 class="hero-title">{{ script.name }}</h1>
          <div class="hero-meta">
            <span class="meta-item">👥 {{ script.playerCount }}人本</span>
            <span class="meta-item">⏱️ {{ script.duration }}小时</span>
            <span class="meta-item">
              <el-rate v-model="script.rating" disabled size="small" />
              <span class="rating-text">{{ script.rating?.toFixed(1) }}</span>
            </span>
            <span class="meta-item">📝 {{ actualReviewCount }}人评价</span>
          </div>
          <p class="hero-desc">{{ script.description?.substring(0, 100) }}...</p>
        </div>
      </div>
    </section>

    <!-- 剧本基本信息 -->
    <section class="script-info-section" v-if="script">
      <div class="container">
        <div class="script-grid">
          <!-- 左侧：剧本封面 -->
          <div class="script-cover-col">
            <div class="cover-image-wrapper">
              <img :src="script.cover || PLACEHOLDERS.SCRIPT" :alt="script.name" class="cover-image">
            </div>
            
            <!-- 价格和预约按钮 -->
            <div class="price-booking-card">
              <div class="card-header-tag">💰 价格信息</div>
              <div class="price-row">
                <span class="price-label">基础价格</span>
                <span class="price-amount">
                  <small>¥</small>{{ script.price }}<small>/人</small>
                </span>
              </div>
              
              <div class="price-extra-info">
                <div class="extra-row">
                  <span>⏱️ 游戏时长</span>
                  <span>约{{ script.duration }}小时</span>
                </div>
                <div class="extra-row">
                  <span>👥 适合人数</span>
                  <span>{{ script.playerCount }}人</span>
                </div>
              </div>
              
              <div class="action-buttons">
                <a href="#" class="booking-button" @click.prevent="handleReserve">
                  <span class="btn-icon">🎭</span>
                  立即预约
                </a>
                <a href="#" class="collect-button" :class="{ 'is-collected': isFavorited }" @click.prevent="handleCollect">
                  <span class="btn-icon">{{ isFavorited ? '❤️' : '🤍' }}</span>
                  {{ isFavorited ? '已收藏' : '收藏剧本' }}
                </a>
              </div>
              
              <div class="guarantee-info">
                <span class="guarantee-item">✅ 随时可退</span>
                <span class="guarantee-item">✅ 真实评价</span>
              </div>
            </div>
            
            <!-- 可约场次余量模块 -->
            <div class="schedule-availability-card">
              <div class="schedule-card-header">
                <span class="schedule-title">📅 近7天可约场次</span>
                <span class="schedule-hint" v-if="!loadingSchedules">
                  {{ availableSchedules.length > 0 ? `共 ${availableSchedules.length} 个场次` : '暂无排期' }}
                </span>
              </div>
              <div v-if="loadingSchedules" class="schedule-loading">加载中...</div>
              <div v-else-if="availableSchedules.length === 0" class="schedule-empty">
                <span>🕰️ 暂无可约场次，可联系门店咨询</span>
              </div>
              <div v-else class="schedule-list">
                <div
                  v-for="s in availableSchedules.slice(0, 6)"
                  :key="s.id"
                  class="schedule-item"
                  @click="handleReserve"
                >
                  <div class="schedule-date">{{ formatScheduleDate(s.scheduleDate) }}</div>
                  <div class="schedule-time">{{ formatTime(s.startTime) }}</div>
                  <div class="schedule-remain" :class="getRemainClass(s)">{{ getRemainText(s) }}</div>
                </div>
              </div>
              <div v-if="availableSchedules.length > 6" class="schedule-more" @click="handleReserve">
                查看更多场次 →
              </div>
            </div>
          </div>
          
          <!-- 右侧：剧本详情 -->
          <div class="script-detail-col">
            <!-- 快速信息栏 -->
            <div class="quick-info-bar">
              <div class="quick-item">
                <span class="quick-icon">🎭</span>
                <span class="quick-label">类型</span>
                <span class="quick-value">{{ script.categoryName }}</span>
              </div>
              <div class="quick-item">
                <span class="quick-icon">⭐</span>
                <span class="quick-label">难度</span>
                <span class="quick-value" :class="'diff-text-' + script.difficulty">{{ getDifficultyText(script.difficulty) }}</span>
              </div>
              <div class="quick-item">
                <span class="quick-icon">👥</span>
                <span class="quick-label">人数</span>
                <span class="quick-value">{{ script.playerCount }}人</span>
              </div>
              <div class="quick-item">
                <span class="quick-icon">⏱️</span>
                <span class="quick-label">时长</span>
                <span class="quick-value">{{ script.duration }}h</span>
              </div>
            </div>
            
            <!-- 基本信息和体验信息 -->
            <div class="info-columns">
              <div class="info-column">
                <h3 class="column-title">📋 基本信息</h3>
                <ul class="info-items">
                  <li class="info-item">
                    <span class="item-label">剧本类型</span>
                    <span class="item-value tag-style">{{ script.categoryName }}</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">玩家人数</span>
                    <span class="item-value">{{ script.playerCount }}人本</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">游戏时长</span>
                    <span class="item-value">约{{ script.duration }}小时</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">难度等级</span>
                    <span class="item-value" :class="'diff-value-' + script.difficulty">
                      {{ getDifficultyStars(script.difficulty) }} {{ getDifficultyText(script.difficulty) }}
                    </span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">发行方式</span>
                    <span class="item-value">📦 盒装本</span>
                  </li>
                </ul>
              </div>
              <div class="info-column experience-column">
                <h3 class="column-title">🎪 体验信息</h3>
                <ul class="info-items">
                  <li class="info-item">
                    <span class="item-label">换装体验</span>
                    <span class="item-value success">👗 提供</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">实景布置</span>
                    <span class="item-value success">🏰 有</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">NPC演绎</span>
                    <span class="item-value success">🎭 有</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">适合人群</span>
                    <span class="item-value">🎯 新手/进阶</span>
                  </li>
                  <li class="info-item">
                    <span class="item-label">复盘难度</span>
                    <span class="item-value">🧩 中等</span>
                  </li>
                </ul>
              </div>
            </div>
            
            <!-- 剧情介绍 -->
            <div class="story-section">
              <h3 class="story-title">📖 剧情介绍</h3>
              <div class="story-card">
                <div class="story-decoration">🔮</div>
                <div class="story-content" v-html="script.description || '暂无简介'"></div>
                <div class="story-warning">
                  <span class="warning-icon">⚠️</span>
                  <span>以上为剧情梗概，更多精彩内容请到店体验，请勿剧透！</span>
                </div>
              </div>
            </div>
            
            <!-- 角色介绍（在右侧底部） -->
            <div class="roles-inline-section" v-if="roles.length > 0">
              <h3 class="story-title">角色介绍</h3>
              <div class="roles-grid-inline">
                <div class="role-card-small" v-for="role in roles" :key="role.id">
                  <img :src="role.avatar || PLACEHOLDERS.AVATAR" :alt="role.roleName" class="role-avatar-small">
                  <div class="role-info-small">
                    <h4 class="role-name-small">{{ role.roleName }}</h4>
                    <p class="role-meta-small">{{ getGenderText(role.gender) }} | {{ role.age || '??岁' }} | {{ role.occupation || '职业未知' }}</p>
                    <p class="role-desc-small">{{ role.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <!-- 玩家评价 -->
    <section class="player-reviews-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-heading">💬 玩家评价</h2>
          <div class="rating-summary" v-if="script">
            <span class="big-rating">{{ script.rating?.toFixed(1) || '5.0' }}</span>
            <div class="rating-detail">
              <el-rate v-model="script.rating" disabled size="small" />
              <span class="review-count">{{ actualReviewCount }}条评价</span>
            </div>
          </div>
        </div>
        
        <div class="reviews-grid-three">
          <div class="review-item" v-for="review in reviews.slice(0, 3)" :key="review.id">
            <div class="review-quote">"</div>
            <div class="review-top">
              <img :src="review.userAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" 
                   :alt="review.username" class="reviewer-avatar">
              <div class="reviewer-info">
                <h4 class="reviewer-name">{{ review.username }}</h4>
                <el-rate v-model="review.rating" disabled size="small" />
              </div>
            </div>
            <p class="review-content">{{ review.content }}</p>
            <div class="review-footer">
              <span class="review-date">🕐 {{ review.createTime }}</span>
              <span class="review-helpful">👍 有帮助</span>
            </div>
          </div>
          
          <div v-if="reviews.length === 0" class="no-reviews-placeholder">
            <div class="empty-icon">📝</div>
            <p>暂无评价，快来成为第一个评价的玩家吧！</p>
          </div>
        </div>
        
        <!-- 查看更多评价按钮 -->
        <div class="more-reviews-btn" v-if="reviews.length > 0">
          <button class="btn-more" @click="showAllReviewsDialog = true">
            查看全部 {{ actualReviewCount }} 条评价 →
          </button>
        </div>
      </div>
    </section>
    
    <!-- 相关剧本推荐 -->
    <section class="related-scripts-section" v-if="recommendedScripts.length > 0">
      <div class="container">
        <div class="section-header">
          <h2 class="section-heading">🎭 猜你喜欢</h2>
          <router-link to="/script" class="view-more-link">
            查看更多剧本 →
          </router-link>
        </div>
        
        <div class="scripts-grid-three">
          <div class="related-script-card" v-for="(relScript, index) in recommendedScripts.slice(0, 3)" :key="relScript.id" @click="goToScript(relScript.id)">
            <!-- 推荐序号徽章 -->
            <div class="recommend-badge">
              <span class="badge-icon">{{ index === 0 ? '🥇' : index === 1 ? '🥈' : '🥉' }}</span>
            </div>
            
            <!-- 图片区域 -->
            <div class="related-script-image-wrapper">
              <img :src="relScript.cover || PLACEHOLDERS.SCRIPT" :alt="relScript.name" class="related-script-image">
              <div class="image-overlay">
                <span class="play-btn">▶ 预约体验</span>
              </div>
              <!-- 难度指示器 -->
              <div class="difficulty-badge" :class="'diff-' + relScript.difficulty">
                {{ getDifficultyStars(relScript.difficulty) }}
              </div>
            </div>
            
            <div class="related-script-info">
              <div class="related-script-header">
                <span class="related-script-tag">
                  <span class="tag-icon">🎬</span> {{ relScript.categoryName }}
                </span>
                <span class="related-script-meta">
                  <span class="meta-icon">👥</span> {{ relScript.playerCount }}人
                  <span class="meta-sep">|</span>
                  <span class="meta-icon">⏱️</span> {{ relScript.duration || 4 }}h
                </span>
              </div>
              
              <h3 class="related-script-title">{{ relScript.name }}</h3>
              
              <p class="related-script-desc">{{ relScript.description || '探索未知的故事，解开谜团，揭示真相...' }}</p>
              
              <!-- 评分 -->
              <div class="related-script-rating">
                <el-rate v-model="relScript.rating" disabled size="small" />
                <span class="rating-value">{{ relScript.rating?.toFixed(1) || '5.0' }}</span>
              </div>
              
              <div class="related-script-footer">
                <div class="price-wrapper">
                  <span class="price-label">价格</span>
                  <span class="related-script-price">
                    <small>¥</small>{{ relScript.price }}<small>/人</small>
                  </span>
                </div>
                <a href="#" class="related-script-btn" @click.stop.prevent="goToScript(relScript.id)">
                  <span>🎭</span> 立即预约
                </a>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 更多推荐提示 -->
        <div class="more-scripts-tip">
          <span class="tip-icon">💡</span>
          <span>根据您的浏览记录和偏好为您推荐，<router-link to="/script">发现更多精彩剧本</router-link></span>
        </div>
      </div>
    </section>
    
    <!-- 查看全部评价对话框 -->
    <el-dialog v-model="showAllReviewsDialog" title="全部评价" width="700px" class="all-reviews-dialog" :append-to-body="false">
      <div class="all-reviews-list">
        <div class="review-item" v-for="review in reviews" :key="review.id" style="margin-bottom: 16px;">
          <div class="review-top">
            <img :src="review.userAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
                 :alt="review.username" class="reviewer-avatar">
            <div class="reviewer-info">
              <h4 class="reviewer-name">{{ review.username }}</h4>
              <el-rate v-model="review.rating" disabled size="small" />
            </div>
            <span class="review-date" style="margin-left: auto; font-size: 12px; color: rgba(255,255,255,0.5);">
              🕐 {{ review.createTime }}
            </span>
          </div>
          <p class="review-content" style="margin-top: 12px;">{{ review.content }}</p>
          <!-- 评价图片 -->
          <div v-if="review.images" style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px;">
            <el-image
              v-for="(img, idx) in review.images.split(',')"
              :key="idx"
              :src="img.trim()"
              :preview-src-list="review.images.split(',').map(i => i.trim())"
              :initial-index="idx"
              fit="cover"
              style="width: 80px; height: 80px; border-radius: 6px; cursor: pointer;"
              preview-teleported
            />
          </div>
        </div>
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </div>
      <template #footer>
        <el-button type="primary" @click="showAllReviewsDialog = false; showReviewDialog = true">
          ✍️ 我要评价
        </el-button>
        <el-button @click="showAllReviewsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 评价对话框 -->
    <el-dialog v-model="showReviewDialog" title="评价剧本" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入评价内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScriptDetail, getScriptRoles, getScriptReviews, addScriptReview, getScriptCategories, favoriteScript, unfavoriteScript, checkScriptFavoriteStatus, getRecommendedScripts, getAvailableSchedules } from '@/api/script'
import request from '@/utils/request'
import { checkFavoriteTask } from '@/api/user'
import { recordBrowseHistory } from '@/api/recommendation'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Calendar, Star, HomeFilled, EditPen, ArrowRight, User, ChatDotRound } from '@element-plus/icons-vue'
import { PLACEHOLDERS } from '@/assets/placeholders'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const script = ref(null)
const roles = ref([])
const reviews = ref([])
const showReviewDialog = ref(false)
const showAllReviewsDialog = ref(false)
const categories = ref([])
const isFavorited = ref(false)
const browseStartTime = ref(null) // 记录浏览开始时间
const recommendedScripts = ref([]) // 相关推荐剧本
const availableSchedules = ref([]) // 可约场次列表
const loadingSchedules = ref(false)

const reviewForm = reactive({
  rating: 5,
  content: ''
})

// 计算实际评价数量（如果后端返回的 reviewCount 不准确，使用实际加载的评价数量）
const actualReviewCount = computed(() => {
  // 如果后端的 reviewCount 大于 0，使用后端的值
  if (script.value?.reviewCount && script.value.reviewCount > 0) {
    return script.value.reviewCount
  }
  // 否则使用实际加载的评价数量
  return reviews.value.length
})

// 难度映射
const difficultyMap = {
  1: '简单',
  2: '普通',
  3: '困难',
  4: '硬核'
}

// 获取难度文本
const getDifficultyStars = (difficulty) => {
  const stars = ['⭐', '⭐⭐', '⭐⭐⭐', '⭐⭐⭐⭐']
  return stars[difficulty - 1] || '⭐'
}

const getDifficultyText = (difficulty) => {
  return difficultyMap[difficulty] || difficulty
}

// 获取性别文本
const getGenderText = (gender) => {
  const genderMap = {
    1: '男',
    2: '女',
    3: '不限'
  }
  return genderMap[gender] || '不限'
}

// 获取性别标签类型
const getGenderType = (gender) => {
  if (gender === 1) return 'primary'
  if (gender === 2) return 'danger'
  return 'info'
}

const loadScriptDetail = async () => {
  loading.value = true
  try {
    const res = await getScriptDetail(route.params.id)
    if (res.code === 200 && res.data) {
      script.value = res.data
      // 补充缺失的字段
      if (!script.value.publishDate) {
        script.value.publishDate = script.value.createTime ? script.value.createTime.split('T')[0] : ''
      }
      if (!script.value.reviewCount) {
        script.value.reviewCount = 0
      }
      // 获取分类名称
      if (script.value.categoryId && categories.value.length > 0) {
        const category = categories.value.find(c => c.id === script.value.categoryId)
        script.value.categoryName = category ? category.name : '未分类'
      }
      
      // 保存浏览历史
      saveBrowseHistory(script.value)
    } else {
      ElMessage.error(res.msg || '加载剧本详情失败')
      router.push('/script')
    }
  } catch (error) {
    console.error('加载剧本详情失败:', error)
    ElMessage.error('请求失败，请稍后重试')
    router.push('/script')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await getScriptRoles(route.params.id)
    if (res.data) {
      roles.value = res.data
    }
  } catch (error) {
    console.error('加载角色信息失败:', error)
    // 角色信息接口暂未实现，不显示角色卡片
    roles.value = []
  }
}

const loadReviews = async () => {
  try {
    const scriptId = route.params.id
    console.log('正在加载剧本评价, scriptId:', scriptId)
    
    // 使用 reservation/review API 获取评价（订单评价中包含剧本评价）
    const res = await request.get('/reservation/review/page', {
      params: {
        scriptId: scriptId,
        page: 1,
        pageSize: 20
        // 不传status，获取所有评价
      }
    })
    console.log('评价API返回:', res)
    
    if (res.code === 200 || res.code === 1) {
      // 兼容多种返回格式
      let reviewList = []
      if (res.data) {
        if (res.data.records && Array.isArray(res.data.records)) {
          reviewList = res.data.records
        } else if (res.data.list && Array.isArray(res.data.list)) {
          reviewList = res.data.list
        } else if (Array.isArray(res.data)) {
          reviewList = res.data
        }
      }
      
      // 格式化评价数据
      reviews.value = reviewList.map(item => ({
        id: item.id,
        username: item.isAnonymous === 1 ? '匿名用户' : (item.userName || item.username || item.nickname || '神秘玩家'),
        userAvatar: item.userAvatar || item.avatar || '/default-avatar.jpg',
        rating: Number(item.scriptRating || item.overallRating || item.rating || 5),
        content: item.content || item.scriptContent || '该用户未填写评价内容',
        images: item.images || '',
        createTime: item.createTime || item.createdAt || ''
      }))
      console.log('格式化后的评价:', reviews.value)
      
      console.log('加载到的评价数量:', reviews.value.length, reviews.value)
    } else {
      console.log('评价API返回code不是1或200:', res.code)
      reviews.value = []
    }
  } catch (error) {
    console.error('加载评价失败:', error)
    reviews.value = []
  }
}

const handleCollect = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (!isFavorited.value) {
      const res = await favoriteScript(route.params.id)
      isFavorited.value = true
      ElMessage.success(res.data || '收藏成功！')
      
      // 检查收藏任务
      try {
        const taskRes = await checkFavoriteTask()
        if (taskRes.code === 1 || taskRes.code === 200) {
          if (taskRes.data?.completed && !taskRes.data?.alreadyCompleted) {
            ElMessage.success('恭喜！完成收藏任务，获得20积分')
          } else if (taskRes.data?.currentFavorites) {
            const current = taskRes.data.currentFavorites
            const required = taskRes.data.requiredFavorites || 5
            if (current < required) {
              ElMessage.info(`已收藏${current}个剧本，还需${required - current}个即可获得20积分`)
            }
          }
        }
      } catch (err) {
        console.error('检查收藏任务失败:', err)
      }
    } else {
      await unfavoriteScript(route.params.id)
      isFavorited.value = false
      ElMessage.success('取消收藏')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    const errorMsg = error.response?.data?.msg || error.msg || '操作失败'
    if (String(errorMsg).includes('已收藏该剧本')) {
      isFavorited.value = true
      ElMessage.success('已收藏该剧本')
      return
    }
    ElMessage.error(errorMsg)
  }
}

// 保存浏览历史到localStorage和后端数据库
const saveBrowseHistory = async (scriptData) => {
  try {
    // 验证数据有效性
    if (!scriptData || !scriptData.id || !scriptData.name) {
      console.warn('剧本数据无效，无法保存浏览历史:', scriptData)
      return
    }
    
    // 1. 保存到localStorage（前端展示用）
    let history = []
    const savedHistory = localStorage.getItem('browseHistory')
    if (savedHistory) {
      history = JSON.parse(savedHistory)
    }
    
    // 添加新记录（去重）
    history = history.filter(item => item.id !== scriptData.id)
    
    const historyItem = {
      id: scriptData.id,
      name: scriptData.name,
      cover: scriptData.cover || '/default-script.jpg',
      categoryName: scriptData.categoryName || '未分类',
      difficulty: scriptData.difficulty || 2,
      playerCount: scriptData.playerCount || 6,
      rating: scriptData.rating || 4.5,
      price: scriptData.price || 0,
      viewTime: new Date().toISOString()
    }
    
    history.unshift(historyItem)
    
    // 只保留最近30条
    history = history.slice(0, 30)
    
    localStorage.setItem('browseHistory', JSON.stringify(history))
    console.log('浏览历史已保存到localStorage:', scriptData.name)
    
    // 2. 保存到后端数据库（推荐系统用）
    if (userStore.isLoggedIn) {
      // 记录浏览开始时间
      browseStartTime.value = Date.now()
      
      // 立即记录浏览行为（时长为0）
      try {
        await recordBrowseHistory(1, scriptData.id, 0) // targetType: 1=剧本
        console.log('浏览历史已保存到数据库:', scriptData.name)
      } catch (error) {
        console.error('保存浏览历史到数据库失败:', error)
      }
    }
  } catch (error) {
    console.error('保存浏览历史失败:', error)
  }
}

const handleSubmitReview = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  
  try {
    await addScriptReview({
      scriptId: route.params.id,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success('评价成功')
    showReviewDialog.value = false
    reviewForm.rating = 5
    reviewForm.content = ''
    // 重新加载剧本信息和评价列表，更新评价数量
    await Promise.all([
      loadScriptDetail(),
      loadReviews()
    ])
  } catch (error) {
    console.error('提交评价失败:', error)
  }
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

// 立即预约
const handleReserve = () => {
  // 跳转到场次选择页，而不是直接进预约创建页
  router.push({
    path: '/reservation/schedule',
    query: { scriptId: route.params.id }
  })
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  if (!userStore.isLoggedIn) {
    isFavorited.value = false
    return
  }
  
  try {
    const res = await checkScriptFavoriteStatus(route.params.id)
    if (res.code === 1 || res.code === 200) {
      isFavorited.value = res.data === true
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
    isFavorited.value = false
  }
}

// 页面卸载时记录完整的浏览时长
onBeforeUnmount(() => {
  if (userStore.isLoggedIn && script.value && browseStartTime.value) {
    const duration = Math.floor((Date.now() - browseStartTime.value) / 1000) // 转换为秒
    if (duration > 0) {
      // 更新浏览时长
      recordBrowseHistory(1, script.value.id, duration).catch(error => {
        console.error('更新浏览时长失败:', error)
      })
      console.log(`页面关闭，浏览时长: ${duration}秒`)
    }
  }
})

// 跳转到剧本详情
const goToScript = async (scriptId) => {
  // 更新路由
  await router.push(`/script/${scriptId}`)
  
  // 滚动到顶部
  window.scrollTo(0, 0)
  
  // 重新加载所有数据
  loading.value = true
  try {
    await loadCategories()
    await loadScriptDetail()
    await Promise.all([
      loadRoles(),
      loadReviews(),
      checkFavoriteStatus(),
      loadRecommendedScripts()
    ])
  } finally {
    loading.value = false
  }
}

// 加载推荐剧本
const loadRecommendedScripts = async () => {
  try {
    const res = await getRecommendedScripts(3)
    if (res.code === 200 || res.code === 1) {
      recommendedScripts.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载推荐剧本失败:', error)
    recommendedScripts.value = []
  }
}

// 加载可约场次（含余量）
const loadAvailableSchedules = async () => {
  if (!route.params.id) return
  loadingSchedules.value = true
  try {
    const res = await getAvailableSchedules({ scriptId: route.params.id, days: 7 })
    if (res.code === 200 || res.code === 1) {
      availableSchedules.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载可约场次失败:', error)
    availableSchedules.value = []
  } finally {
    loadingSchedules.value = false
  }
}

// 格式化场次日期（yyyy-MM-dd -> 今天/明天/MM月dd日）
const formatScheduleDate = (dateStr) => {
  if (!dateStr) return ''
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(today.getDate() + 1)
  const d = new Date(dateStr)
  if (d.toDateString() === today.toDateString()) return '今天'
  if (d.toDateString() === tomorrow.toDateString()) return '明天'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// 格式化时间 LocalTime -> HH:mm
const formatTime = (t) => {
  if (!t) return ''
  return String(t).substring(0, 5)
}

// 余量文字
const getRemainText = (schedule) => {
  const remain = schedule.maxPlayers - schedule.currentPlayers
  if (remain <= 0) return '已满'
  if (remain === 1) return '差1人成团'
  return `余 ${remain} 位`
}

// 余量样式
const getRemainClass = (schedule) => {
  const remain = schedule.maxPlayers - schedule.currentPlayers
  if (remain <= 0) return 'remain-full'
  if (remain <= 2) return 'remain-few'
  return 'remain-ok'
}

onMounted(async () => {
  // 先加载分类，再加载剧本详情
  await loadCategories()
  await loadScriptDetail()
  
  // 并行加载其他数据，确保评价及时显示
  await Promise.all([
    loadRoles(),
    loadReviews(),
    checkFavoriteStatus(),
    loadRecommendedScripts(),
    loadAvailableSchedules()
  ])
})
</script>

<style scoped>
/* 全局容器 - 按照0.html风格 */
.script-detail-container {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  min-height: 100vh;
  font-family: 'Inter', system-ui, sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

/* 沉浸式头部横幅 */
.script-hero-banner {
  position: relative;
  min-height: 320px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover;
  background-position: center;
  filter: blur(8px);
  transform: scale(1.1);
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.92) 0%, rgba(15, 52, 96, 0.88) 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  color: #fff;
}

.breadcrumb-hero {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  font-size: 14px;
}

.breadcrumb-hero .breadcrumb-link {
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  transition: color 0.3s;
}

.breadcrumb-hero .breadcrumb-link:hover {
  color: #fff;
}

.breadcrumb-hero .sep {
  color: rgba(255,255,255,0.4);
}

.breadcrumb-hero .current {
  color: rgba(255,255,255,0.9);
}

.hero-main {
  max-width: 800px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.hero-tag {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(4px);
}

.hero-tag.category {
  background: rgba(192, 57, 43, 0.8);
}

.hero-tag.difficulty.diff-1 { background: rgba(46, 125, 50, 0.8); }
.hero-tag.difficulty.diff-2 { background: rgba(245, 124, 0, 0.8); }
.hero-tag.difficulty.diff-3 { background: rgba(230, 81, 0, 0.8); }
.hero-tag.difficulty.diff-4 { background: rgba(198, 40, 40, 0.8); }

.hero-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 16px 0;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.hero-meta .meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: rgba(255,255,255,0.9);
}

.hero-meta .rating-text {
  font-weight: 600;
  color: #ffd700;
  margin-left: 6px;
}

.hero-meta :deep(.el-rate__icon) {
  font-size: 16px;
}

.hero-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.75);
  line-height: 1.6;
  margin: 0;
}

/* 剧本信息区 - 0.html布局 */
.script-info-section {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  padding: 48px 0;
}

.script-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 32px;
}

/* 左侧封面列 */
.script-cover-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.cover-image-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.cover-image {
  width: 100%;
  height: auto;
  display: block;
}

/* 价格预约卡片 - 优化版 */
.price-booking-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 2px solid rgba(192, 57, 43, 0.3);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.card-header-tag {
  font-size: 14px;
  font-weight: 600;
  color: #ff6b6b;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(192, 57, 43, 0.3);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.price-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.price-amount {
  font-size: 32px;
  font-weight: 700;
  color: #ff6b6b;
}

.price-amount small {
  font-size: 16px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

.price-extra-info {
  margin-bottom: 20px;
  background: rgba(192, 57, 43, 0.15);
  border-radius: 10px;
  padding: 12px;
}

.extra-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  border-bottom: 1px solid rgba(192, 57, 43, 0.15);
}

.extra-row:last-child {
  border-bottom: none;
}

.extra-price {
  color: #ff9800;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.booking-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  color: white;
  text-align: center;
  padding: 14px 24px;
  border-radius: 10px;
  text-decoration: none;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.3);
}

.booking-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.4);
}

.btn-icon {
  font-size: 18px;
}

.collect-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  background: rgba(35, 35, 60, 0.8);
  color: #ff6b6b;
  border: 2px solid rgba(192, 57, 43, 0.5);
  text-align: center;
  padding: 12px 24px;
  border-radius: 10px;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s;
}

.collect-button:hover {
  background: rgba(192, 57, 43, 0.2);
  border-color: #16213e;
}

.collect-button.is-collected {
  background: rgba(192, 57, 43, 0.3);
  border-color: #ff6b6b;
  color: #ff6b6b;
}

/* 可约场次余量卡片 */
.schedule-availability-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 2px solid rgba(192, 57, 43, 0.3);
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.schedule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.schedule-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.schedule-hint {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.schedule-loading, .schedule-empty {
  text-align: center;
  color: rgba(255,255,255,0.5);
  font-size: 13px;
  padding: 12px 0;
}

.schedule-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.schedule-item {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 10px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.schedule-item:hover {
  background: rgba(192, 57, 43,0.25);
  border-color: rgba(192, 57, 43,0.5);
  transform: translateY(-1px);
}

.schedule-date {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 4px;
}

.schedule-time {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 5px;
}

.schedule-remain {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 8px;
  display: inline-block;
}

.remain-ok { background: rgba(103,194,58,0.2); color: #67c23a; }
.remain-few { background: rgba(230,162,60,0.2); color: #e6a23c; }
.remain-full { background: rgba(245,108,108,0.2); color: #f56c6c; }

.schedule-more {
  text-align: center;
  margin-top: 10px;
  font-size: 13px;
  color: #ff6b6b;
  cursor: pointer;
}

.schedule-more:hover { text-decoration: underline; }

.guarantee-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid rgba(192, 57, 43, 0.2);
}

.guarantee-item {
  font-size: 12px;
  color: #7ddc7a;
}

/* 快速信息栏 */
.quick-info-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  border-radius: 12px;
}

.quick-item {
  text-align: center;
  color: #fff;
}

.quick-icon {
  font-size: 24px;
  display: block;
  margin-bottom: 6px;
}

.quick-label {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  display: block;
  margin-bottom: 4px;
}

.quick-value {
  font-size: 15px;
  font-weight: 600;
}

.quick-value.diff-text-1 { color: #52c41a; }
.quick-value.diff-text-2 { color: #faad14; }
.quick-value.diff-text-3 { color: #fa8c16; }
.quick-value.diff-text-4 { color: #f5222d; }

/* 难度值颜色 */
.diff-value-1 { color: #52c41a; }
.diff-value-2 { color: #faad14; }
.diff-value-3 { color: #fa8c16; }
.diff-value-4 { color: #f5222d; }

.item-value.tag-style {
  background: rgba(192, 57, 43, 0.1);
  color: #16213e;
  padding: 2px 10px;
  border-radius: 12px;
}

.item-value.success {
  color: #52c41a;
}

.experience-column {
  background: linear-gradient(135deg, rgba(35, 60, 35, 0.95) 0%, rgba(30, 50, 40, 0.95) 100%);
  border: 1px solid rgba(82, 196, 26, 0.3);
}

/* 右侧详情列 */
.script-detail-col {
  padding-left: 0;
}

.script-name {
  font-size: clamp(1.8rem, 3vw, 2.5rem);
  font-weight: 700;
  color: #1A1A1A;
  margin: 0 0 20px;
  line-height: 1.2;
}

/* 标签 - 0.html风格 */
.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.script-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.tag-primary {
  background: rgba(192, 57, 43, 0.1);
  color: #16213e;
}

.tag-secondary {
  background: rgba(44, 14, 55, 0.1);
  color: #2C0E37;
}

.tag-accent {
  background: rgba(255, 215, 0, 0.1);
  color: #e6a23c;
}

.tag-gray {
  background: #f0f0f0;
  color: #666;
}

/* 评分 - 0.html风格 */
.rating-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.rating-wrapper :deep(.el-rate) {
  display: flex;
  align-items: center;
}

.rating-wrapper :deep(.el-rate__text) {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-left: 8px;
}

.rating-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

/* 信息列 - 0.html双列布局 */
.info-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.info-column {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.column-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 16px;
}

.info-items {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(192, 57, 43, 0.15);
}

.info-item:last-child {
  border-bottom: none;
}

.item-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.item-value {
  color: #fff;
  font-weight: 500;
  font-size: 14px;
}

/* 剧情介绍 */
.story-section {
  margin-top: 32px;
  margin-bottom: 32px;
}

.story-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 16px;
}

.story-card {
  position: relative;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 1px solid rgba(192, 57, 43, 0.2);
  border-radius: 16px;
  padding: 28px;
  overflow: hidden;
}

.story-decoration {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 48px;
  opacity: 0.15;
}

.story-content {
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
  text-indent: 2em;
  margin-bottom: 20px;
}

.story-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(230, 162, 60, 0.15);
  border: 1px solid rgba(230, 162, 60, 0.4);
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;
}

.warning-icon {
  font-size: 16px;
}

/* 区块头部 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-heading {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.rating-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.big-rating {
  font-size: 36px;
  font-weight: 700;
  color: #faad14;
}

.rating-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 评价卡片优化 */
.review-item {
  position: relative;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.review-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.25);
  border-color: #16213e;
}

.review-quote {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 48px;
  color: rgba(255, 107, 107, 0.2);
  font-family: serif;
  line-height: 1;
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(192, 57, 43, 0.2);
}

.review-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.review-helpful {
  font-size: 12px;
  color: #ff6b6b;
  cursor: pointer;
  transition: color 0.3s;
}

.review-helpful:hover {
  color: #ff8a8a;
}

.no-reviews-placeholder {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 角色介绍（在右侧底部） - 0.html风格 */
.roles-inline-section {
  margin-top: 32px;
}

.roles-grid-inline {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.role-card-small {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 1px solid rgba(192, 57, 43, 0.2);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
}

.role-card-small:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(192, 57, 43, 0.25);
  border-color: rgba(192, 57, 43, 0.4);
}

.role-avatar-small {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: contain;
  background-color: #f5f5f5;
}

.role-info-small {
  padding: 12px;
}

.role-name-small {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #fff;
}

.role-meta-small {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 8px;
}

.role-desc-small {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 玩家评价区 - 0.html风格 */
.player-reviews-section {
  background: linear-gradient(180deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 22, 40, 0.98) 100%);
  padding: 48px 0;
}

.roles-section {
  background: linear-gradient(180deg, rgba(22, 22, 40, 0.98) 0%, rgba(26, 26, 46, 0.98) 100%);
  padding: 48px 0;
}

.related-scripts-section {
  background: linear-gradient(180deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 22, 40, 0.98) 100%);
  padding: 48px 0;
}

.reviews-grid-three {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.review-item {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.review-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
}

.review-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.reviewer-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.reviewer-info {
  flex: 1;
}

.reviewer-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #fff;
}

.reviewer-info :deep(.el-rate) {
  height: 20px;
}

.review-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
  margin: 0 0 12px;
}

.review-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.no-reviews-placeholder {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.more-reviews-btn {
  text-align: center;
}

.btn-more {
  padding: 10px 32px;
  border: 1px solid #ff6b6b;
  background: transparent;
  color: #ff6b6b;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-more:hover {
  background: rgba(192, 57, 43, 0.2);
  border-color: #16213e;
}

.btn-more :deep(.el-icon) {
  font-size: 16px;
}

/* 相关剧本推荐 - 优化版 */
.related-scripts-section {
  background: linear-gradient(180deg, rgba(22, 22, 40, 0.98) 0%, rgba(26, 26, 46, 0.98) 100%);
  padding: 60px 0;
}

.view-more-link {
  color: #ff6b6b;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.view-more-link:hover {
  color: #ff8a8a;
  text-decoration: underline;
}

.scripts-grid-three {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
  margin-bottom: 32px;
}

.related-script-card {
  position: relative;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.related-script-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 20px 40px rgba(192, 57, 43, 0.3);
  border-color: #16213e;
}

.recommend-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  background: rgba(255,255,255,0.95);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.badge-icon {
  font-size: 20px;
}

.related-script-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.related-script-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.related-script-card:hover .related-script-image {
  transform: scale(1.1);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 26, 46, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.related-script-card:hover .image-overlay {
  opacity: 1;
}

.play-btn {
  padding: 12px 24px;
  background: rgba(192, 57, 43, 0.9);
  color: white;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  transform: translateY(10px);
  transition: transform 0.3s;
}

.related-script-card:hover .play-btn {
  transform: translateY(0);
}

.difficulty-badge {
  position: absolute;
  bottom: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(4px);
}

.difficulty-badge.diff-1 { color: #52c41a; }
.difficulty-badge.diff-2 { color: #faad14; }
.difficulty-badge.diff-3 { color: #fa8c16; }
.difficulty-badge.diff-4 { color: #f5222d; }

.related-script-info {
  padding: 20px;
}

.related-script-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.related-script-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.1) 0%, rgba(192, 57, 43, 0.05) 100%);
  color: #16213e;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 500;
}

.tag-icon {
  font-size: 12px;
}

.related-script-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.meta-icon {
  font-size: 12px;
}

.meta-sep {
  color: #ddd;
  margin: 0 2px;
}

.related-script-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 10px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.related-script-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.related-script-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.rating-value {
  font-size: 14px;
  font-weight: 600;
  color: #faad14;
}

.related-script-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(192, 57, 43, 0.2);
}

.price-wrapper {
  display: flex;
  flex-direction: column;
}

.price-wrapper .price-label {
  font-size: 11px;
  color: #999;
  margin-bottom: 2px;
}

.related-script-price {
  font-size: 22px;
  font-weight: 700;
  color: #ff6b6b;
}

.related-script-price small {
  font-size: 12px;
  font-weight: 400;
  color: #999;
}

.related-script-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  color: white;
  border-radius: 20px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(192, 57, 43, 0.3);
}

.related-script-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(192, 57, 43, 0.4);
}

/* 更多推荐提示 */
.more-scripts-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 24px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 12px;
  font-size: 14px;
  color: #ad8b00;
}

.more-scripts-tip .tip-icon {
  font-size: 18px;
}

.more-scripts-tip a {
  color: #16213e;
  font-weight: 500;
  text-decoration: underline;
}

/* 全部评价弹窗深色主题 */
:deep(.all-reviews-dialog) {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%) !important;
  border: 1px solid rgba(192, 57, 43, 0.4) !important;
  border-radius: 20px !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.7) !important;

  .el-dialog__header {
    background: transparent !important;
    border-bottom: 1px solid rgba(192, 57, 43, 0.2);

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: bold;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: rgba(255, 255, 255, 0.6) !important;
      &:hover { color: #fff !important; }
    }
  }

  .el-dialog__body {
    background: transparent !important;
    max-height: 60vh;
    overflow-y: auto;
  }

  .el-dialog__footer {
    background: transparent !important;
    border-top: 1px solid rgba(192, 57, 43, 0.2);

    .el-button--default {
      background: rgba(255, 255, 255, 0.08) !important;
      border-color: rgba(255, 255, 255, 0.2) !important;
      color: rgba(255, 255, 255, 0.8) !important;
      &:hover {
        background: rgba(255, 255, 255, 0.15) !important;
        color: #fff !important;
      }
    }
  }
}

/* 响应式设计 - 0.html风格 */
@media (max-width: 1024px) {
  .script-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  
  .roles-grid-inline,
  .reviews-grid-three,
  .scripts-grid-three {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .script-info-section,
  .roles-section,
  .player-reviews-section,
  .related-scripts-section {
    padding: 32px 0;
  }
  
  .section-heading {
    font-size: 1.5rem;
    margin-bottom: 24px;
  }
  
  .script-name {
    font-size: 1.8rem;
  }
  
  .info-columns {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .roles-grid-inline {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .reviews-grid-three,
  .scripts-grid-three {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .price-booking-card {
    margin-top: 20px;
  }
}
</style>
