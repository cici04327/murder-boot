<template>
  <div class="home-container">
    <!-- 骨架屏 -->
    <SkeletonHome v-if="pageLoading" />
    
    <!-- 真实内容 -->
    <div v-else class="home-content">
    <!-- 英雄Banner区域 - 剧本杀主题 -->
    <section class="hero-banner">
      <!-- 装饰元素 -->
      <div class="hero-decorations">
        <span class="deco-item deco-1">🎭</span>
        <span class="deco-item deco-2">🔍</span>
        <span class="deco-item deco-3">📜</span>
        <span class="deco-item deco-4">🗝️</span>
      </div>
      
      <el-carousel height="80vh" class="banner" :interval="5000" arrow="always">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="hero-gradient"></div>
            <div class="banner-content">
              <!-- 剧本杀主题标签 -->
              <div class="hero-badge">
                <span class="badge-icon">🎭</span>
                <span>沉浸式剧本杀体验</span>
              </div>
              
              <h1 class="hero-title">{{ item.title }}</h1>
              <p class="hero-description">{{ item.description }}</p>
              
              <div class="hero-buttons">
                <el-button class="hero-btn-primary" size="large" @click="router.push(item.link)">
                  <span class="btn-icon">🎮</span>
                  {{ item.buttonText }}
                  <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
                <el-button class="hero-btn-secondary" size="large" @click="router.push('/script')">
                  <span class="btn-icon">📚</span>
                  浏览剧本
                  <el-icon class="el-icon--right"><Reading /></el-icon>
                </el-button>
              </div>
              
            </div>
            <!-- 滚动提示（放在 banner-item 底部，独立于 banner-content） -->
            <div class="scroll-indicator">
              <span class="scroll-text">向下滑动探索更多</span>
              <span class="scroll-arrow">↓</span>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 快速入口 - 剧本杀主题 -->
    <div class="quick-entry">
      <div class="quick-entry-title">
        <span class="title-icon">🚀</span>
        <span>快速开始</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="6" v-for="entry in quickEntries" :key="entry.id">
          <div class="entry-card" :class="'entry-' + entry.id" @click="router.push(entry.path)">
            <div class="entry-icon-wrapper">
              <span class="entry-emoji">{{ entry.emoji }}</span>
            </div>
            <div class="entry-title">{{ entry.title }}</div>
            <div class="entry-desc">{{ entry.description }}</div>
            <div class="entry-arrow">→</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 已排期剧本杀 -->
    <div class="section scheduled-section">
      <div class="section-header">
        <div class="header-left">
          <h3>📅 已排期剧本杀</h3>
          <span class="header-subtitle">按日期挑当天能开的车，跨门店快速上车</span>
        </div>
        <el-link type="primary" @click="router.push('/schedule/list')">
          查看排期大厅 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-link>
      </div>

      <el-row :gutter="20" v-loading="scheduledSessionsLoading">
        <el-col :xs="24" :sm="12" :md="6" v-for="session in visibleScheduledSessions" :key="session.id">
          <div class="scheduled-card" @click="handleOpenScheduledSession(session)">
            <div class="scheduled-cover">
              <LazyImage
                :src="session.cover || getScriptCover(session.scriptName, '')"
                :alt="session.scriptName"
                :height="190"
                :immediate="true"
                @error="handleImageError"
              />
              <div class="scheduled-date">{{ formatScheduleCardDate(session.scheduleDate) }}</div>
              <div class="scheduled-time">{{ formatScheduleCardTime(session) }}</div>
              <div class="scheduled-remain" :class="getScheduledRemainClass(session)">
                {{ getScheduledRemainText(session) }}
              </div>
            </div>

            <div class="scheduled-body">
              <h4>{{ session.scriptName }}</h4>
              <div class="scheduled-meta">
                <span class="meta-badge difficulty" :class="'diff-' + (session.difficulty || 2)">
                  {{ getDifficultyText(session.difficulty) }}
                </span>
                <span class="meta-badge">👥 {{ session.playerCount || session.maxPlayers }}人本</span>
                <span class="meta-badge">⏱️ {{ session.duration || 3 }}h</span>
              </div>

              <div class="scheduled-info-line">🏠 {{ session.storeName || '待定门店' }}</div>
              <div class="scheduled-info-line">🚪 {{ session.roomName || '待定房间' }}</div>

              <div class="scheduled-footer">
                <span class="scheduled-price">¥{{ Number(session.price || 0).toFixed(0) }}/人</span>
                <span class="scheduled-action">查看场次 →</span>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="24" v-if="!scheduledSessionsLoading && visibleScheduledSessions.length === 0">
          <div class="empty-groups">
            <el-empty description="最近暂无开放场次">
              <el-button type="primary" @click="router.push('/schedule/list')">去排期大厅看看</el-button>
            </el-empty>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 热门剧本 - 自动播放 -->
    <div class="section">
      <div class="section-header">
        <div class="header-left">
          <h3>🔥 热门剧本</h3>
          <span class="header-subtitle">精选高评分剧本</span>
        </div>
        <div class="header-right">
          <el-button 
            :icon="autoPlayPaused ? 'VideoPlay' : 'VideoPause'" 
            circle 
            size="small"
            @click="toggleAutoPlay"
            :title="autoPlayPaused ? '开始自动播放' : '暂停自动播放'"
          />
          <el-link type="primary" @click="router.push('/script')">
            查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-link>
        </div>
      </div>
      <div 
        class="scroll-container auto-scroll" 
        ref="hotScriptsContainer"
        v-loading="hotScriptsLoading"
        @mouseenter="pauseAutoPlay"
        @mouseleave="resumeAutoPlay"
      >
        <div class="scroll-wrapper" :style="{ transform: `translateX(-${scrollOffset}px)` }">
          <!-- 原始剧本列表 -->
          <div class="script-card-horizontal" v-for="(script, index) in hotScripts" :key="'original-' + script.id" @click="router.push(`/script/${script.id}`)">
            <div class="script-image">
              <LazyImage
                :src="script.cover || getScriptCover(script.name, script.categoryName)"
                :alt="script.name"
                :height="200"
                :immediate="true"
                @error="handleImageError"
              />
              <div class="script-tag">🎭 {{ script.categoryName }}</div>
              <div class="hot-badge">🔥 HOT</div>
              <div class="script-rank" v-if="index < 3">TOP{{ index + 1 }}</div>
              <div class="difficulty-stars">{{ getDifficultyStars(script.difficulty) }}</div>
              <div class="image-overlay">
                <span class="overlay-btn">▶ 立即预约</span>
              </div>
            </div>
            <div class="script-info">
              <h4>{{ script.name }}</h4>
              <div class="script-meta">
                <span class="meta-badge difficulty" :class="'diff-' + script.difficulty">{{ getDifficultyText(script.difficulty) }}</span>
                <span class="meta-badge">👥 {{ script.playerCount }}人</span>
                <span class="meta-badge">⏱️ {{ script.duration }}h</span>
              </div>
              <div class="script-rating">
                <el-rate v-model="script.rating" disabled show-score size="small" text-color="#ff9900" />
              </div>
              <div class="script-footer">
                <div class="script-price">
                  <small>¥</small><span class="price-value">{{ script.price }}</span><small>/人</small>
                </div>
                <span class="book-hint">点击预约 →</span>
              </div>
            </div>
          </div>
          <!-- 克隆的剧本列表（用于无缝循环） -->
          <div class="script-card-horizontal" v-for="(script, index) in hotScripts" :key="'clone-' + script.id" @click="router.push(`/script/${script.id}`)">
            <div class="script-image">
              <LazyImage
                :src="script.cover || getScriptCover(script.name, script.categoryName)"
                :alt="script.name"
                :height="200"
                :immediate="true"
                @error="handleImageError"
              />
              <div class="script-tag">🎭 {{ script.categoryName }}</div>
              <div class="hot-badge">🔥 HOT</div>
              <div class="script-rank" v-if="index < 3">TOP{{ index + 1 }}</div>
              <div class="difficulty-stars">{{ getDifficultyStars(script.difficulty) }}</div>
              <div class="image-overlay">
                <span class="overlay-btn">▶ 立即预约</span>
              </div>
            </div>
            <div class="script-info">
              <h4>{{ script.name }}</h4>
              <div class="script-meta">
                <span class="meta-badge difficulty" :class="'diff-' + script.difficulty">{{ getDifficultyText(script.difficulty) }}</span>
                <span class="meta-badge">👥 {{ script.playerCount }}人</span>
                <span class="meta-badge">⏱️ {{ script.duration }}h</span>
              </div>
              <div class="script-rating">
                <el-rate v-model="script.rating" disabled show-score size="small" text-color="#ff9900" />
              </div>
              <div class="script-footer">
                <div class="script-price">
                  <small>¥</small><span class="price-value">{{ script.price }}</span><small>/人</small>
                </div>
                <span class="book-hint">点击预约 →</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门拼单 -->
    <div class="section group-section">
      <div class="section-header">
        <div class="header-left">
          <h3>🤝 热门拼单</h3>
          <span class="header-subtitle">找搭子，一起玩剧本杀</span>
        </div>
        <el-link type="primary" @click="router.push('/group')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-link>
      </div>
      <el-row :gutter="20" v-loading="groupsLoading">
        <el-col :xs="24" :sm="12" :md="6" v-for="group in hotGroups" :key="group.id">
          <div class="group-card" @click="router.push(`/group/${group.id}`)">
            <div class="group-header">
              <div class="group-avatar mystery-avatar">
                <span class="avatar-mask">🎭</span>
                <span class="creator-badge">车主</span>
              </div>
              <div class="group-creator">
                <span class="creator-name">{{ group.creatorAnonymousName || '神秘车主' }}</span>
                <span class="create-time">{{ formatGroupTime(group.createTime) }}</span>
              </div>
              <div class="group-status" :class="getGroupStatusClass(group.status)">
                {{ getGroupStatusText(group.status) }}
              </div>
            </div>
            <div class="group-content">
              <h4 class="group-title">{{ group.scriptName }}</h4>
              <div class="group-info">
                <span class="info-item">
                  <el-icon><Shop /></el-icon>
                  {{ group.storeName }}
                </span>
                <span class="info-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatPlayTime(group.playTime) }}
                </span>
              </div>
              <div class="group-progress">
                <div class="progress-text">
                  <span>已报名 <strong>{{ group.currentCount }}</strong> / {{ group.needCount }} 人</span>
                  <span class="spots-left" v-if="group.needCount - group.currentCount > 0">
                    还差 {{ group.needCount - group.currentCount }} 人
                  </span>
                  <span class="spots-full" v-else>已满员</span>
                </div>
                <el-progress 
                  :percentage="(group.currentCount / group.needCount) * 100" 
                  :show-text="false"
                  :color="group.currentCount >= group.needCount ? '#67c23a' : '#8B0000'"
                />
              </div>
              <div class="group-tags">
                <el-tag size="small" type="info">{{ group.playerCount }}人本</el-tag>
                <el-tag size="small" v-if="group.genderRequirement">{{ group.genderRequirement }}</el-tag>
                <el-tag size="small" type="warning" v-if="group.newbieWelcome">新手友好</el-tag>
              </div>
            </div>
            <div class="group-footer">
              <span class="group-price">¥{{ group.price }}/人</span>
              <el-button 
                size="small" 
                type="primary" 
                :disabled="group.currentCount >= group.needCount || group.status !== 1"
                @click.stop="handleJoinGroup(group)"
              >
                {{ group.currentCount >= group.needCount ? '已满员' : '立即参团' }}
              </el-button>
            </div>
          </div>
        </el-col>
        <el-col :span="24" v-if="!groupsLoading && hotGroups.length === 0">
          <div class="empty-groups">
            <el-empty description="暂无拼单，快来发起第一个吧！">
              <el-button type="primary" @click="router.push('/schedule/list')">发起拼单</el-button>
            </el-empty>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 个性化推荐 -->
    <div class="section recommendation-section">
      <div class="section-header">
        <div class="header-left">
          <h3>💎 为你推荐</h3>
          <span class="header-subtitle">根据你的喜好精选</span>
        </div>
        <el-link type="primary" @click="router.push('/recommend/enhanced')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-link>
      </div>
      <el-row :gutter="20" v-loading="recommendedScriptsLoading">
        <el-col :xs="24" :sm="12" :md="6" v-for="(script, index) in recommendedScripts" :key="script.id">
          <div class="script-card recommend-card" @click="router.push(`/script/${script.id}`)">
            <div class="script-image">
              <LazyImage
                :src="script.cover || getScriptCover(script.name, script.categoryName)"
                :alt="script.name"
                :height="220"
                :immediate="true"
                @error="handleImageError"
              />
              <div class="script-tag">🎭 {{ script.categoryName }}</div>
              <div class="recommend-badge">💎 推荐</div>
              <div class="difficulty-stars">{{ getDifficultyStars(script.difficulty) }}</div>
              <div class="image-overlay">
                <span class="overlay-btn">▶ 立即预约</span>
              </div>
            </div>
            <div class="script-info">
              <h4>{{ script.name }}</h4>
              <div class="script-meta">
                <span class="meta-badge difficulty" :class="'diff-' + script.difficulty">{{ getDifficultyText(script.difficulty) }}</span>
                <span class="meta-badge">👥 {{ script.playerCount }}人</span>
                <span class="meta-badge">⏱️ {{ script.duration }}h</span>
              </div>
              <div class="script-rating">
                <el-rate v-model="script.rating" disabled show-score size="small" />
                <span class="rating-text">{{ script.rating?.toFixed(1) }}</span>
              </div>
              <div class="script-footer">
                <div class="script-price">
                  <small>¥</small><span class="price-value">{{ script.price }}</span><small>/人</small>
                </div>
                <span class="book-hint">点击预约 →</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 推荐门店 -->
    <div class="section store-section">
      <div class="section-header">
        <div class="header-left">
          <h3>🏪 推荐门店</h3>
          <span class="header-subtitle">优质商家精选</span>
        </div>
        <el-link type="primary" @click="router.push('/store')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-link>
      </div>
      <el-row :gutter="20" v-loading="hotStoresLoading">
        <el-col :xs="24" :sm="12" :md="6" v-for="(store, index) in hotStores.slice(0, 4)" :key="store.id">
          <div class="store-card" @click="router.push(`/store/${store.id}`)">
            <div class="store-image">
              <LazyImage :src="store.coverImage || PLACEHOLDERS.STORE" :alt="store.name" :height="180" :immediate="true" />
              <div class="store-badge gold" v-if="store.rating >= 4.5">🏆 优质门店</div>
              <div class="store-badge new" v-else-if="index === 0">✨ 人气推荐</div>
              <div class="store-script-count">📜 {{ store.scriptCount || 50 }}+剧本</div>
              <div class="image-overlay">
                <span class="overlay-btn">🏠 进入门店</span>
              </div>
            </div>
            <div class="store-info">
              <div class="store-header">
                <h4>{{ store.name }}</h4>
                <span class="store-distance" v-if="store.distanceText">{{ store.distanceText }}</span>
              </div>
              <div class="store-address">
                <span class="address-icon">📍</span>
                {{ store.address }}
              </div>
              <div class="store-rating">
                <el-rate v-model="store.rating" disabled size="small" />
                <span class="rating-value">{{ store.rating?.toFixed(1) }}</span>
                <span class="review-count">{{ store.reviewCount }}条评价</span>
              </div>
              <div class="store-features">
                <span class="feature-item" v-if="store.hasParking">🅿️ 停车</span>
                <span class="feature-item">🎭 换装</span>
                <span class="feature-item">🏰 实景</span>
              </div>
              <div class="store-footer">
                <div class="store-tags">
                  <span class="store-tag" v-for="tag in (store.tags || ['情感', '推理']).slice(0, 2)" :key="tag">{{ tag }}</span>
                </div>
                <span class="visit-hint">查看详情 →</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 最新资讯/攻略 -->
    <div class="section news-section">
      <div class="section-header">
        <div class="header-left">
          <h3>📰 最新资讯</h3>
          <span class="header-subtitle">剧本攻略 · 行业动态</span>
        </div>
        <el-link type="primary" @click="router.push('/article')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-link>
      </div>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6" v-for="(news, index) in newsList" :key="news.id">
          <div class="news-card" @click="handleNewsClick(news)">
            <div class="news-image">
              <LazyImage :src="news.image" :alt="news.title" :height="150" :immediate="true" />
              <div class="news-category">
                <span class="category-icon">{{ getCategoryIcon(news.category) }}</span>
                {{ news.category }}
              </div>
              <div class="news-read-time">📖 {{ Math.ceil(news.views / 200) }}分钟阅读</div>
              <div class="image-overlay">
                <span class="overlay-btn">📖 阅读全文</span>
              </div>
            </div>
            <div class="news-content">
              <h4>{{ news.title }}</h4>
              <p class="news-summary">{{ news.summary }}</p>
              <div class="news-meta">
                <span class="news-date">🗓️ {{ news.date }}</span>
                <span class="news-views">
                  👁️ {{ news.views }}
                </span>
              </div>
              <div class="news-footer">
                <span class="read-more">阅读全文 →</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import SkeletonHome from '@/components/Skeleton/SkeletonHome.vue'
import LazyImage from '@/components/LazyImage.vue'
import { useRouter } from 'vue-router'
import { Reading, ArrowRight } from '@element-plus/icons-vue'
import { getHotScripts, getRecommendedScripts, getScheduledSessions } from '@/api/script'
import { PLACEHOLDERS } from '@/assets/placeholders'
import { getScriptCover } from '@/assets/script-covers'
import { getHotStores } from '@/api/store'
import { getRecommendedArticles } from '@/api/article'
import { getHotGroups } from '@/api/group'
import { Shop, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { hasScheduleStarted } from '@/utils/schedule-time'
import { getCachedUserLocation, calculateDistance, formatDistance } from '@/utils/location'

const router = useRouter()
const userStore = useUserStore()

// 拼单相关
const hotGroups = ref([])
const groupsLoading = ref(false)

// 自动播放相关
const hotScriptsContainer = ref(null)
const scrollOffset = ref(0)
const autoPlayTimer = ref(null)
const autoPlayPaused = ref(false)
const scrollSpeed = 1 // 每帧滚动的像素数
const cardWidth = 300 // 卡片宽度（280px + 20px gap）

const banners = ref([
  {
    id: 1,
    title: '探索精彩剧本世界',
    description: '海量优质剧本，总有一款适合你',
    image: PLACEHOLDERS.POSTER1,
    link: '/script',
    buttonText: '立即探索',
    badge: '热门推荐'
  },
  {
    id: 2,
    title: '寻找附近门店',
    description: '优质门店，舒适环境，专业服务',
    image: PLACEHOLDERS.POSTER2,
    link: '/store',
    buttonText: '查看门店',
    badge: '精选商家'
  },
  {
    id: 3,
    title: '在线预约，快速便捷',
    description: '随时随地预约，享受优惠',
    image: PLACEHOLDERS.POSTER3,
    link: '/script',
    buttonText: '立即预约',
    badge: '限时优惠'
  },
  {
    id: 4,
    title: '新剧本上线',
    description: '《时空裂痕》震撼来袭，立即体验',
    image: PLACEHOLDERS.POSTER4,
    link: '/script',
    buttonText: '查看详情',
    badge: '新品首发'
  }
])

const quickEntries = ref([
  {
    id: 1,
    title: '剧本大厅',
    description: '500+精选剧本等你来',
    emoji: '📜',
    color: '#667eea',
    path: '/script'
  },
  {
    id: 2,
    title: '门店探索',
    description: '发现身边优质门店',
    emoji: '🏠',
    color: '#52c41a',
    path: '/store'
  },
  {
    id: 3,
    title: '排期场次',
    description: '当天开车场次一眼看全',
    emoji: '📅',
    color: '#faad14',
    path: '/schedule/list'
  },
  {
    id: 4,
    title: '组队拼车',
    description: '找搭子一起玩',
    emoji: '👥',
    color: '#f5222d',
    path: '/group'
  }
])

// 页面整体加载状态
const pageLoading = ref(true)

const hotScripts = ref([])
const hotScriptsLoading = ref(false)
const scheduledSessions = ref([])
const scheduledSessionsLoading = ref(false)
const scheduleNowTick = ref(Date.now())
const scheduleClockTimer = ref(null)
const recommendedScripts = ref([])
const recommendedScriptsLoading = ref(false)
const hotStores = ref([])
const hotStoresLoading = ref(false)

const newsList = ref([
  {
    id: 1,
    title: '剧本杀新手入门指南',
    summary: '从零开始，带你了解剧本杀的魅力',
    category: '新手攻略',
    image: 'https://dummyimage.com/300x200/667eea/ffffff&text=新手指南',
    date: '2024-01-15',
    views: 1234
  },
  {
    id: 2,
    title: '如何选择适合自己的剧本',
    summary: '根据难度、类型、人数选择最适合的剧本',
    category: '选本技巧',
    image: 'https://dummyimage.com/300x200/f093fb/ffffff&text=选本技巧',
    date: '2024-01-14',
    views: 856
  },
  {
    id: 3,
    title: '2024年度热门剧本排行榜',
    summary: '盘点本年度最受欢迎的十大剧本',
    category: '榜单推荐',
    image: 'https://dummyimage.com/300x200/4facfe/ffffff&text=热门榜单',
    date: '2024-01-13',
    views: 2341
  },
  {
    id: 4,
    title: '剧本杀行业发展趋势分析',
    summary: '探讨剧本杀行业的未来发展方向',
    category: '行业动态',
    image: 'https://dummyimage.com/300x200/43e97b/ffffff&text=行业动态',
    date: '2024-01-12',
    views: 678
  }
])

// 难度映射
const difficultyMap = {
  1: '简单',
  2: '普通',
  3: '困难',
  4: '硬核'
}

// 获取难度文本
const getDifficultyText = (difficulty) => {
  return difficultyMap[difficulty] || difficulty
}

// 获取难度星星
const getDifficultyStars = (difficulty) => {
  const stars = ['⭐', '⭐⭐', '⭐⭐⭐', '⭐⭐⭐⭐']
  return stars[difficulty - 1] || '⭐'
}

// 获取资讯分类图标
const getCategoryIcon = (category) => {
  const icons = {
    '新手攻略': '📚',
    '选本技巧': '🎯',
    '榜单推荐': '🏆',
    '行业动态': '📈',
    '玩家分享': '💬',
    '活动公告': '📢'
  }
  return icons[category] || '📰'
}

// 图片加载失败处理（LazyImage 组件不传递 event 对象）
const handleImageError = () => {
  // LazyImage 组件内部已经处理了错误显示，这里不需要做任何操作
  console.log('图片加载失败，已使用默认图片')
}

const loadHotScripts = async () => {
  hotScriptsLoading.value = true
  try {
    const res = await getHotScripts()
    if (res.data) {
      hotScripts.value = res.data.slice(0, 8)
    }
  } catch (error) {
    console.error('加载热门剧本失败:', error)
    // 使用模拟数据（带精美封面图片）
    hotScripts.value = [
      {
        id: 1,
        name: '迷雾庄园',
        categoryName: '本格推理',
        difficulty: 2,
        playerCount: 6,
        duration: 4,
        price: 88,
        rating: 4.5,
        coverImage: 'https://images.unsplash.com/photo-1582719508461-905c673771fd?q=80&w=1200&auto=format&fit=crop' // 神秘庄园
      },
      {
        id: 2,
        name: '时光旅人',
        categoryName: '情感沉浸',
        difficulty: 1,
        playerCount: 5,
        duration: 3,
        price: 68,
        rating: 4.8,
        coverImage: 'https://images.unsplash.com/photo-1501139083538-0139583c060f?q=80&w=1200&auto=format&fit=crop' // 怀旧时钟
      },
      {
        id: 3,
        name: '末日余晖',
        categoryName: '科幻机制',
        difficulty: 3,
        playerCount: 7,
        duration: 5,
        price: 108,
        rating: 4.6,
        coverImage: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200&auto=format&fit=crop' // 地球太空
      },
      {
        id: 4,
        name: '古堡迷踪',
        categoryName: '恐怖惊悚',
        difficulty: 2,
        playerCount: 6,
        duration: 4,
        price: 98,
        rating: 4.7,
        coverImage: 'https://images.unsplash.com/photo-1518562923427-c0ff595c8072?q=80&w=1200&auto=format&fit=crop' // 恐怖古堡
      },
      {
        id: 5,
        name: '星际穿越',
        categoryName: '科幻机制',
        difficulty: 3,
        playerCount: 6,
        duration: 5,
        price: 118,
        rating: 4.9,
        coverImage: 'https://images.unsplash.com/photo-1446776653964-20c1d3a81b06?q=80&w=1200&auto=format&fit=crop' // 星空宇宙
      },
      {
        id: 6,
        name: '云中谜案',
        categoryName: '本格推理',
        difficulty: 2,
        playerCount: 7,
        duration: 4,
        price: 98,
        rating: 4.6,
        coverImage: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1200&auto=format&fit=crop' // 中式古典建筑
      },
      {
        id: 7,
        name: '时间囚徒',
        categoryName: '情感沉浸',
        difficulty: 2,
        playerCount: 5,
        duration: 3,
        price: 78,
        rating: 4.7,
        coverImage: 'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?q=80&w=1200&auto=format&fit=crop' // 沙漏与时间
      },
      {
        id: 8,
        name: '幽灵旅馆',
        categoryName: '恐怖惊悚',
        difficulty: 3,
        playerCount: 6,
        duration: 4,
        price: 108,
        rating: 4.8,
        coverImage: 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1200&auto=format&fit=crop' // 废弃酒店
      }
    ]
  } finally {
    hotScriptsLoading.value = false
  }
}

const loadScheduledSessions = async () => {
  scheduledSessionsLoading.value = true
  try {
    const res = await getScheduledSessions({ days: 7 })
    const list = Array.isArray(res.data) ? res.data : []
    scheduledSessions.value = list
  } catch (error) {
    console.error('加载已排期场次失败:', error)
    scheduledSessions.value = []
  } finally {
    scheduledSessionsLoading.value = false
  }
}

const visibleScheduledSessions = computed(() => {
  const now = scheduleNowTick.value
  return scheduledSessions.value
    .filter((session) => !hasScheduleStarted(session, now))
    .slice(0, 4)
})

const loadRecommendedScripts = async () => {
  recommendedScriptsLoading.value = true
  try {
    const res = await getRecommendedScripts()
    if (res.data) {
      recommendedScripts.value = res.data.slice(0, 4)
    }
  } catch (error) {
    // 静默失败，使用模拟数据（带精美封面图片）
    console.log('使用默认推荐剧本数据')
    recommendedScripts.value = [
      {
        id: 11,
        name: '红楼梦境',
        categoryName: '情感沉浸',
        difficulty: 2,
        playerCount: 6,
        duration: 4,
        price: 88,
        rating: 4.7,
        coverImage: 'https://images.unsplash.com/photo-1528127269322-539801943592?q=80&w=1200&auto=format&fit=crop' // 中国古典园林
      },
      {
        id: 12,
        name: '深海秘境',
        categoryName: '冒险探索',
        difficulty: 2,
        playerCount: 5,
        duration: 3,
        price: 78,
        rating: 4.5,
        coverImage: 'https://images.unsplash.com/photo-1559827260-dc66d52bef19?q=80&w=1200&auto=format&fit=crop' // 深海潜水
      },
      {
        id: 13,
        name: '黑暗骑士',
        categoryName: '机制对抗',
        difficulty: 3,
        playerCount: 7,
        duration: 5,
        price: 98,
        rating: 4.6,
        coverImage: 'https://images.unsplash.com/photo-1599423300746-b62533397364?q=80&w=1200&auto=format&fit=crop' // 中世纪盔甲
      },
      {
        id: 14,
        name: '魔法学院',
        categoryName: '欢乐互动',
        difficulty: 1,
        playerCount: 6,
        duration: 3,
        price: 68,
        rating: 4.8,
        coverImage: 'https://images.unsplash.com/photo-1519791883288-dc8bd696e667?q=80&w=1200&auto=format&fit=crop' // 图书馆魔法书
      }
    ]
  } finally {
    recommendedScriptsLoading.value = false
  }
}

const loadHotStores = async () => {
  hotStoresLoading.value = true
  try {
    const res = await getHotStores()
    if (res.data) {
      const userLocation = getCachedUserLocation()
      hotStores.value = res.data.slice(0, 6).map(store => {
        if (!store.coverImage && store.images) {
          const imageList = store.images.split(',').map(img => img.trim()).filter(img => img)
          if (imageList.length > 0) store.coverImage = imageList[0]
        }

        if (userLocation && store.latitude && store.longitude) {
          const distanceKm = calculateDistance(
            userLocation.latitude,
            userLocation.longitude,
            Number(store.latitude),
            Number(store.longitude)
          )
          store.distance = distanceKm
          store.distanceText = formatDistance(distanceKm)
        } else {
          store.distance = null
          store.distanceText = ''
        }

        return store
      })
    }
  } catch (error) {
    console.error('加载推荐门店失败:', error)
    // 使用模拟数据
    hotStores.value = [
      {
        id: 1,
        name: '探案密室',
        address: '北京市朝阳区xxx街道xxx号',
        rating: 4.8,
        reviewCount: 128,
        tags: ['环境优雅', '服务专业', '交通便利'],
        coverImage: 'https://images.unsplash.com/photo-1593642532454-e138e28a63f4?q=80&w=1200&auto=format&fit=crop'
      },
      {
        id: 2,
        name: '时空剧本馆',
        address: '北京市海淀区xxx街道xxx号',
        rating: 4.6,
        reviewCount: 96,
        tags: ['剧本丰富', '价格实惠', '氛围好'],
        coverImage: 'https://images.unsplash.com/photo-1481277542470-605612bd2d61?q=80&w=1200&auto=format&fit=crop'
      },
      {
        id: 3,
        name: '沉浸式体验馆',
        address: '北京市东城区xxx街道xxx号',
        rating: 4.9,
        reviewCount: 156,
        tags: ['装修精美', 'DM专业', '停车方便'],
        coverImage: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=1200&auto=format&fit=crop'
      },
      {
        id: 4,
        name: '推理殿堂',
        address: '北京市西城区xxx街道xxx号',
        rating: 4.7,
        reviewCount: 142,
        tags: ['书香氛围', '推理专场', '咖啡厅'],
        coverImage: 'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?q=80&w=1200&auto=format&fit=crop'
      },
      {
        id: 5,
        name: '剧本杀公馆',
        address: '北京市丰台区xxx街道xxx号',
        rating: 4.9,
        reviewCount: 189,
        tags: ['欧式风格', '高端品质', 'VIP包间'],
        coverImage: 'https://images.unsplash.com/photo-1556909212-d5b604d0c90d?q=80&w=1200&auto=format&fit=crop'
      },
      {
        id: 6,
        name: '谜境体验店',
        address: '北京市石景山区xxx街道xxx号',
        rating: 4.5,
        reviewCount: 117,
        tags: ['包间齐全', '服务周到', '停车方便'],
        coverImage: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=1200&auto=format&fit=crop'
      }
    ]
  } finally {
    hotStoresLoading.value = false
  }
}

// 加载资讯列表
const loadNews = async () => {
  try {
    const res = await getRecommendedArticles(4)
    if (res.code === 1 || res.code === 200) {
      newsList.value = res.data.map(article => ({
        id: article.id,
        title: article.title,
        summary: article.summary,
        category: article.categoryName,
        image: article.coverImage || PLACEHOLDERS.ARTICLE,
        date: article.publishTime ? new Date(article.publishTime).toLocaleDateString('zh-CN') : '',
        views: article.viewCount
      }))
    }
  } catch (error) {
    console.log('使用默认资讯数据')
  }
}

// 处理资讯点击
const handleNewsClick = (news) => {
  router.push(`/article/${news.id}`)
}

// 加载热门拼单
const loadHotGroups = async () => {
  groupsLoading.value = true
  try {
    const res = await getHotGroups(4)
    if (res.code === 1 || res.code === 200) {
      hotGroups.value = res.data || []
    }
  } catch (error) {
    console.error('加载拼单失败:', error)
    hotGroups.value = []
  } finally {
    groupsLoading.value = false
  }
}

// 格式化拼单时间
const formatGroupTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60 / 1000)}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 60 / 60 / 1000)}小时前`
  return `${Math.floor(diff / 24 / 60 / 60 / 1000)}天前`
}

// 格式化开车时间
const formatPlayTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hours}:${minutes}`
}

// 获取拼单状态文本
const getGroupStatusText = (status) => {
  const statusMap = {
    0: '已取消',
    1: '拼团中',
    2: '已成团',
    3: '已结束'
  }
  return statusMap[status] || '未知'
}

// 获取拼单状态样式
const getGroupStatusClass = (status) => {
  const classMap = {
    0: 'status-cancelled',
    1: 'status-active',
    2: 'status-success',
    3: 'status-ended'
  }
  return classMap[status] || ''
}

const getScheduledRemainCount = (session) => {
  return Math.max(0, Number(session.maxPlayers || 0) - Number(session.currentPlayers || 0))
}

const getScheduledRemainText = (session) => {
  const remain = getScheduledRemainCount(session)
  if (remain <= 0) return '已满员'
  if (remain === 1) return '差1人成团'
  return `余${remain}位`
}

const getScheduledRemainClass = (session) => {
  const remain = getScheduledRemainCount(session)
  if (remain <= 0) return 'full'
  if (remain <= 2) return 'few'
  return 'ok'
}

const formatScheduleCardDate = (scheduleDate) => {
  const value = String(scheduleDate || '').slice(0, 10)
  if (!value) return '待定日期'
  const [year, month, day] = value.split('-')
  return `${month}/${day}`
}

const formatScheduleCardTime = (session) => {
  const startTime = String(session.startTime || '').slice(0, 5)
  const endTime = String(session.endTime || '').slice(0, 5)
  return `${startTime}${endTime ? ` - ${endTime}` : ''}`
}

const handleOpenScheduledSession = (session) => {
  router.push({
    path: '/schedule/list',
    query: {
      date: String(session.scheduleDate || '').slice(0, 10)
    }
  })
}

const startScheduleClock = () => {
  if (scheduleClockTimer.value) {
    return
  }
  scheduleClockTimer.value = setInterval(() => {
    scheduleNowTick.value = Date.now()
  }, 30000)
}

const stopScheduleClock = () => {
  if (!scheduleClockTimer.value) {
    return
  }
  clearInterval(scheduleClockTimer.value)
  scheduleClockTimer.value = null
}

// 加入拼单
const handleJoinGroup = (group) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push(`/group/${group.id}`)
}

// 自动播放函数
const startAutoPlay = () => {
  if (autoPlayTimer.value) return
  
  autoPlayTimer.value = setInterval(() => {
    if (autoPlayPaused.value) return
    
    scrollOffset.value += scrollSpeed
    
    // 计算一半内容的宽度（因为我们复制了一份）
    const halfWidth = hotScripts.value.length * cardWidth
    
    // 当滚动到一半时，重置到起点（无缝循环）
    if (scrollOffset.value >= halfWidth) {
      scrollOffset.value = 0
    }
  }, 16) // 约60fps
}

const stopAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value)
    autoPlayTimer.value = null
  }
}

const toggleAutoPlay = () => {
  autoPlayPaused.value = !autoPlayPaused.value
  if (autoPlayPaused.value) {
    stopAutoPlay()
  } else {
    startAutoPlay()
  }
}

const pauseAutoPlay = () => {
  if (!autoPlayPaused.value) {
    stopAutoPlay()
  }
}

const resumeAutoPlay = () => {
  if (!autoPlayPaused.value) {
    startAutoPlay()
  }
}

onMounted(async () => {
  startScheduleClock()
  try {
    // 并行加载所有数据
    await Promise.all([
      loadScheduledSessions(),
      loadHotScripts(),
      loadHotGroups(),
      loadRecommendedScripts(),
      loadHotStores(),
      loadNews()
    ])
    
    // 数据加载完成，关闭骨架屏
    pageLoading.value = false
    
    // 启动自动播放
    setTimeout(() => {
      if (hotScripts.value.length > 0) {
        startAutoPlay()
      }
    }, 500)
  } catch (error) {
    console.error('页面数据加载失败:', error)
    // 即使加载失败也关闭骨架屏
    pageLoading.value = false
  }
})

onBeforeUnmount(() => {
  stopAutoPlay()
  stopScheduleClock()
})
</script>

<style scoped>
.home-container {
  padding: 0;
  background: transparent; /* 使用全局深色背景 */
}

/* 英雄Banner样式 - 剧本杀主题 */
.hero-banner {
  position: relative;
  min-height: 600px;
}

/* 装饰元素 */
.hero-decorations {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 3;
  overflow: hidden;
}

.deco-item {
  position: absolute;
  font-size: 48px;
  opacity: 0.15;
  animation: float-deco 6s ease-in-out infinite;
}

.deco-1 { top: 15%; left: 5%; animation-delay: 0s; }
.deco-2 { top: 25%; right: 8%; animation-delay: 1.5s; }
.deco-3 { bottom: 30%; left: 10%; animation-delay: 3s; }
.deco-4 { bottom: 20%; right: 5%; animation-delay: 4.5s; }

@keyframes float-deco {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.banner {
  margin-bottom: 0;
  border-radius: 0;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.banner-item {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(26, 26, 26, 0.8), rgba(139, 0, 0, 0.6));
  z-index: 1;
}

.banner-content {
  position: relative;
  text-align: center;
  color: #fff;
  z-index: 2;
  max-width: 800px;
  padding: 0 20px;
}

.hero-title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  margin-bottom: 24px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
}

.hero-description {
  font-size: clamp(1rem, 3vw, 1.5rem);
  margin-bottom: 32px;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.5);
  opacity: 0.95;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.hero-buttons {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-btn-primary {
  padding: 12px 32px;
  background: #8B0000;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 500;
  transition: all 0.3s;
}

.hero-btn-primary:hover {
  background: rgba(139, 0, 0, 0.9);
  transform: translateY(-2px);
}

.hero-btn-secondary {
  padding: 12px 32px;
  background: transparent;
  color: #FFD700;
  border: 1px solid #FFD700;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 500;
  transition: all 0.3s;
}

.hero-btn-secondary:hover {
  background: rgba(255, 215, 0, 0.1);
  transform: translateY(-2px);
}

/* Hero Badge */
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: rgba(139, 0, 0, 0.8);
  border-radius: 25px;
  margin-bottom: 20px;
  font-size: 14px;
  backdrop-filter: blur(4px);
}

.badge-icon {
  font-size: 18px;
}

/* 特色标签 */
.hero-features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 15px;
  margin-bottom: 30px;
}

.feature-tag {
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  font-size: 14px;
  backdrop-filter: blur(4px);
  transition: all 0.3s;
}

.feature-tag:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.btn-icon {
  margin-right: 6px;
}

.scroll-indicator {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.8);
  z-index: 3;
  pointer-events: none;
}

.scroll-text {
  font-size: 12px;
}

.scroll-arrow {
  font-size: 20px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

/* 快速入口 */
/* 快速入口 - 剧本杀主题 */
.quick-entry {
  margin: 0 auto 60px;
  max-width: 1200px;
  padding: 0 20px;
}

.quick-entry-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 25px;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.title-icon {
  font-size: 24px;
}

.entry-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  height: 100%;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.entry-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(139, 0, 0, 0.03) 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.entry-card:hover::before {
  opacity: 1;
}

.entry-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.entry-icon-wrapper {
  width: 70px;
  height: 70px;
  margin: 0 auto 15px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  transition: all 0.3s;
}

.entry-card:hover .entry-icon-wrapper {
  transform: scale(1.1) rotate(5deg);
  background: rgba(139, 0, 0, 0.3);
}

.entry-emoji {
  font-size: 36px;
}

.entry-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #fff;
}

.entry-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
}

.entry-arrow {
  position: absolute;
  bottom: 15px;
  right: 20px;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.3s;
}

.entry-card:hover .entry-arrow {
  color: #8B0000;
  transform: translateX(5px);
}

/* 入口卡片统一使用深色风格 */
/* ==================== 剧本杀风格：四个卡片统一深色主题 ==================== */
/* 目标：不要白底；偏悬疑/档案/线索卡质感；不使用渐变 */

.entry-1,
.entry-2,
.entry-3,
.entry-4 {
  background: #111827;            /* 深墨蓝黑 */
  border-color: rgba(139, 0, 0, 0.35);
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.22);
}

/* 禁用卡片原本的渐变遮罩层 */
.entry-1::before,
.entry-2::before,
.entry-3::before,
.entry-4::before {
  background: none;
}

.entry-1 .entry-icon-wrapper,
.entry-2 .entry-icon-wrapper,
.entry-3 .entry-icon-wrapper,
.entry-4 .entry-icon-wrapper {
  background: rgba(139, 0, 0, 0.92); /* 酒红圆徽章 */
  box-shadow: 0 10px 22px rgba(139, 0, 0, 0.25);
}

.entry-1 .entry-title,
.entry-2 .entry-title,
.entry-3 .entry-title,
.entry-4 .entry-title {
  color: #f9fafb;
  letter-spacing: 0.5px;
}

.entry-1 .entry-desc,
.entry-2 .entry-desc,
.entry-3 .entry-desc,
.entry-4 .entry-desc {
  color: rgba(249, 250, 251, 0.72);
}

.entry-1 .entry-arrow,
.entry-2 .entry-arrow,
.entry-3 .entry-arrow,
.entry-4 .entry-arrow {
  color: rgba(249, 250, 251, 0.35);
}

.entry-1:hover,
.entry-2:hover,
.entry-3:hover,
.entry-4:hover {
  background: #0b1220; /* 更深一点，营造"阴影" */
  border-color: rgba(139, 0, 0, 0.75);
  box-shadow: 0 16px 40px rgba(17, 24, 39, 0.35);
}

.entry-1:hover .entry-arrow,
.entry-2:hover .entry-arrow,
.entry-3:hover .entry-arrow,
.entry-4:hover .entry-arrow {
  color: #8B0000;
}

/* 章节通用样式 */
.section {
  margin-bottom: 60px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  padding: 0 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.section-header h3 {
  font-size: 26px;
  color: #fff;
  margin: 0;
  font-weight: 700;
}

.header-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.scheduled-section {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 40px 20px;
  border-radius: 16px;
  margin-bottom: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.scheduled-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s ease;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.28);
}

.scheduled-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.scheduled-cover {
  position: relative;
  height: 190px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.scheduled-cover :deep(img) {
  transition: transform 0.5s ease;
}

.scheduled-card:hover .scheduled-cover :deep(img) {
  transform: scale(1.12);
}

.scheduled-date,
.scheduled-time,
.scheduled-remain {
  position: absolute;
  left: 12px;
  z-index: 2;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(5px);
}

.scheduled-date {
  top: 12px;
  background: rgba(139, 0, 0, 0.9);
  color: #fff;
  font-weight: 600;
}

.scheduled-time {
  top: 48px;
  background: rgba(0, 0, 0, 0.55);
  color: rgba(255, 255, 255, 0.92);
}

.scheduled-remain {
  bottom: 12px;
  font-weight: 600;
}

.scheduled-remain.ok {
  background: rgba(103, 194, 58, 0.2);
  color: #9be7a2;
}

.scheduled-remain.few {
  background: rgba(230, 162, 60, 0.2);
  color: #ffd18b;
}

.scheduled-remain.full {
  background: rgba(245, 108, 108, 0.2);
  color: #ffc2c2;
}

.scheduled-body {
  padding: 18px;
}

.scheduled-body h4 {
  margin: 0 0 12px;
  font-size: 18px;
  color: #fff;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduled-meta {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scheduled-info-line {
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.76);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduled-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  margin-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.scheduled-price {
  font-size: 20px;
  font-weight: 700;
  color: #ff8f8f;
}

.scheduled-action {
  font-size: 12px;
  color: #8B0000;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.3s;
}

.scheduled-card:hover .scheduled-action {
  opacity: 1;
}

/* 横向滚动容器 */
.scroll-container {
  position: relative;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 10px;
}

.scroll-container::-webkit-scrollbar {
  height: 8px;
}

.scroll-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.scroll-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 自动播放容器 */
.scroll-container.auto-scroll {
  overflow: hidden;
}

.scroll-wrapper {
  display: flex;
  gap: 20px;
  padding-bottom: 5px;
  transition: transform 0.3s linear;
}

.auto-scroll .scroll-wrapper {
  transition: none;
}

/* 右侧控制区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 横向剧本卡片 - 优化版 */
.script-card-horizontal {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  width: 280px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.script-card-horizontal:hover {
  transform: translateY(-12px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

/* 普通剧本卡片 - 优化版 */
.script-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.script-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.script-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.script-card .script-image {
  height: 220px;
}

.script-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.script-card:hover .script-image img,
.script-card-horizontal:hover .script-image img {
  transform: scale(1.15);
}

/* 图片遮罩层 */
.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 26, 46, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.script-card:hover .image-overlay,
.script-card-horizontal:hover .image-overlay,
.store-card:hover .image-overlay,
.news-card:hover .image-overlay {
  opacity: 1;
}

.overlay-btn {
  padding: 12px 24px;
  background: rgba(139, 0, 0, 0.9);
  color: white;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  transform: translateY(10px);
  transition: transform 0.3s;
}

.script-card:hover .overlay-btn,
.script-card-horizontal:hover .overlay-btn,
.store-card:hover .overlay-btn,
.news-card:hover .overlay-btn {
  transform: translateY(0);
}

.script-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(139, 0, 0, 0.9);
  backdrop-filter: blur(5px);
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.hot-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: #fff;
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 11px;
  font-weight: bold;
  animation: pulse 2s infinite;
}

.recommend-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 11px;
  font-weight: bold;
}

.script-rank {
  position: absolute;
  top: 50px;
  left: 12px;
  background: linear-gradient(135deg, #ffd700 0%, #ff8c00 100%);
  color: #1a1a2e;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: bold;
}

.difficulty-stars {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.7);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.script-info {
  padding: 18px;
}

.script-info h4 {
  margin: 0 0 12px;
  font-size: 17px;
  color: #fff;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.script-meta {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-badge {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.meta-badge.difficulty {
  font-weight: 600;
}

.meta-badge.difficulty.diff-1 {
  background: #e8f5e9;
  color: #2e7d32;
}

.meta-badge.difficulty.diff-2 {
  background: #fff8e1;
  color: #f57c00;
}

.meta-badge.difficulty.diff-3 {
  background: #fff3e0;
  color: #e65100;
}

.meta-badge.difficulty.diff-4 {
  background: #ffebee;
  color: #c62828;
}

.script-rating {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-text {
  font-size: 14px;
  font-weight: 600;
  color: #faad14;
}

.script-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.script-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff6b6b;
}

.script-price small {
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

.price-value {
  font-size: 24px;
}

.book-hint {
  font-size: 12px;
  color: #8B0000;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.3s;
}

.script-card:hover .book-hint,
.script-card-horizontal:hover .book-hint {
  opacity: 1;
}

/* 推荐区域特殊样式 */
.recommendation-section {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  padding: 40px 20px;
  border-radius: 16px;
  margin-bottom: 60px;
}

/* 门店卡片 - 优化版 */
.store-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.store-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.store-image {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.store-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.store-card:hover .store-image img {
  transform: scale(1.15);
}

.store-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: bold;
}

.store-badge.gold {
  background: linear-gradient(135deg, #ffd700 0%, #ff8c00 100%);
  color: #1a1a2e;
}

.store-badge.new {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.store-script-count {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 5px 12px;
  border-radius: 15px;
  font-size: 12px;
}

.store-info {
  padding: 18px;
}

.store-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.store-info h4 {
  margin: 0;
  font-size: 17px;
  color: #fff;
  font-weight: 700;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store-distance {
  font-size: 12px;
  color: #52c41a;
  background: rgba(82, 196, 26, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 10px;
}

.store-address {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
}

.store-rating {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.review-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.store-tags .el-tag {
  margin: 0;
}

/* 门店新增样式 */
.address-icon {
  font-size: 14px;
}

.store-rating .rating-value {
  font-size: 15px;
  font-weight: 700;
  color: #faad14;
}

.store-features {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.store-features .feature-item {
  font-size: 12px;
  color: #52c41a;
  background: rgba(82, 196, 26, 0.2);
  padding: 3px 8px;
  border-radius: 10px;
}

.store-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.store-tag {
  font-size: 11px;
  color: #8B0000;
  background: rgba(139, 0, 0, 0.08);
  padding: 3px 10px;
  border-radius: 10px;
}

.visit-hint {
  font-size: 12px;
  color: #8B0000;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.3s;
}

.store-card:hover .visit-hint {
  opacity: 1;
}

/* 拼单区域 */
.group-section {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 40px 20px;
  border-radius: 16px;
  margin-bottom: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.group-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.group-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(139, 0, 0, 0.25);
  border-color: rgba(139, 0, 0, 0.3);
  background: rgba(255, 255, 255, 0.12);
}

.group-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
  position: relative;
}

.group-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 神秘面具头像样式 */
.group-avatar.mystery-avatar {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.group-avatar .avatar-mask {
  font-size: 22px;
  animation: maskFloat 3s ease-in-out infinite;
}

@keyframes maskFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.group-avatar .creator-badge {
  position: absolute;
  bottom: -4px;
  right: -8px;
  background: #8B0000;
  color: #fff;
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 6px;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.group-creator {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.creator-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.create-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.group-status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: rgba(139, 0, 0, 0.3);
  color: #ff6b6b;
}

.status-success {
  background: rgba(103, 194, 58, 0.2);
  color: #7ddc7a;
}

.status-cancelled {
  background: rgba(153, 153, 153, 0.2);
  color: rgba(255, 255, 255, 0.6);
}

.status-ended {
  background: rgba(144, 147, 153, 0.2);
  color: rgba(255, 255, 255, 0.5);
}

.group-content {
  padding: 16px;
}

.group-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.group-info .info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.group-info .info-item .el-icon {
  color: #ff6b6b;
}

.group-progress {
  margin-bottom: 12px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.progress-text strong {
  color: #ff6b6b;
  font-size: 16px;
}

.spots-left {
  color: #e6a23c;
  font-weight: 500;
}

.spots-full {
  color: #67c23a;
  font-weight: 500;
}

.group-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.group-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.group-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff6b6b;
}

.group-footer .el-button--primary {
  background: #8B0000;
  border-color: #8B0000;
}

.group-footer .el-button--primary:hover:not(:disabled) {
  background: rgba(139, 0, 0, 0.9);
}

.empty-groups {
  padding: 40px 0;
  text-align: center;
}

.empty-groups .el-button {
  background: #8B0000;
  border-color: #8B0000;
}

/* 资讯区域 */
.news-section {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 40px 20px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 资讯卡片 - 优化版 */
.news-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.news-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 35px rgba(139, 0, 0, 0.3);
  border-color: rgba(139, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.news-image {
  position: relative;
  height: 150px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.news-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.news-card:hover .news-image img {
  transform: scale(1.15);
}

.news-category {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(139, 0, 0, 0.9);
  color: #fff;
  padding: 5px 12px;
  border-radius: 15px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.category-icon {
  font-size: 12px;
}

.news-read-time {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
}

.news-content {
  padding: 16px;
}

.news-content h4 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #fff;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-summary {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 12px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.news-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}

.news-views {
  display: flex;
  align-items: center;
  gap: 4px;
}

.news-footer {
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.read-more {
  font-size: 13px;
  color: #8B0000;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.3s;
}

.news-card:hover .read-more {
  opacity: 1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .banner {
    height: 350px;
    margin-bottom: 30px;
  }
  
  .banner-content h2 {
    font-size: 32px;
  }
  
  .banner-content p {
    font-size: 16px;
  }
  
  .section-header h3 {
    font-size: 22px;
  }
  
  .script-card-horizontal {
    width: 240px;
  }
  
  .quick-entry {
    margin-bottom: 40px;
  }
  
  .entry-card {
    padding: 25px 15px;
    margin-bottom: 15px;
  }
}
</style>
