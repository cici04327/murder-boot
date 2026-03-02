<template>
  <div class="article-detail-container" v-loading="loading">
    <!-- 返回按钮 -->
    <div class="back-button-wrapper">
      <el-button @click="goBack" :icon="ArrowLeft" circle size="large" class="back-button" />
    </div>

    <!-- 文章头部 -->
    <div class="article-header" v-if="article">
      <div class="header-background">
        <div class="header-overlay"></div>
        <img v-if="article.coverImage" :src="article.coverImage" class="header-bg-image" />
      </div>
      
      <div class="header-content">
        <div class="category-badge-wrapper">
          <span class="category-badge" :style="{ background: getCategoryColor(article.category) }">
            {{ getCategoryIcon(article.category) }} {{ article.categoryName }}
          </span>
          <span class="badge-top" v-if="article.isTop === 1">📌 置顶</span>
        </div>
        
        <h1 class="article-title-main">{{ article.title }}</h1>
        
        <div class="article-meta-bar">
          <div class="meta-left">
            <span class="meta-item author-item">
              <el-avatar :size="32" class="author-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="author-name">{{ article.authorName || '匿名作者' }}</span>
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(article.publishTime) }}
            </span>
          </div>
          
          <div class="meta-right">
            <span class="stat-badge">
              <el-icon><View /></el-icon>
              {{ formatNumber(article.viewCount) }}
            </span>
            <span class="stat-badge">
              <el-icon><StarFilled /></el-icon>
              {{ formatNumber(article.likeCount) }}
            </span>
            <span class="stat-badge">
              <el-icon><ChatDotRound /></el-icon>
              {{ formatNumber(article.commentCount || 0) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 文章主体 -->
    <div class="article-main" v-if="article">
      <!-- 文章摘要 -->
      <div class="article-summary-box" v-if="article.summary">
        <div class="summary-icon">💡</div>
        <p class="summary-text">{{ article.summary }}</p>
      </div>

      <!-- 文章内容 -->
      <div class="article-content">
        <div v-html="article.content"></div>
      </div>

      <!-- 文章标签 -->
      <div class="article-tags" v-if="article.categoryName">
        <el-tag type="primary" size="large" effect="plain">
          # {{ article.categoryName }}
        </el-tag>
      </div>
    </div>

    <!-- 文章操作栏 -->
    <div class="article-actions-bar" v-if="article">
      <div class="actions-container">
        <el-button 
          size="large"
          round
          :icon="liked ? StarFilled : Star" 
          @click="handleLike" 
          :loading="liking"
          :class="['action-button', 'like-button', { 'is-liked': liked }]"
        >
          <span class="action-text">{{ liked ? '已点赞' : '点赞' }}</span>
          <span class="action-count">{{ formatNumber(article.likeCount) }}</span>
        </el-button>
        
        <el-button 
          size="large"
          round
          :icon="Share" 
          @click="handleShare"
          class="action-button share-button"
        >
          分享文章
        </el-button>
      </div>
    </div>

    <!-- 评论区域 -->
    <div class="comment-section" v-if="article">
      <div class="comment-header">
        <h3>
          <el-icon><ChatDotRound /></el-icon>
          评论 ({{ comments.length }})
        </h3>
      </div>

      <!-- 发表评论 -->
      <div class="comment-editor">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="4"
          placeholder="说说你的看法..."
          maxlength="500"
          show-word-limit
        />
        <div class="comment-actions">
          <el-button type="primary" @click="submitComment" :loading="commenting">
            发表评论
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comment-list" v-loading="loadingComments">
        <div v-if="comments.length === 0" class="empty-comments">
          <el-empty description="暂无评论，快来发表第一条评论吧！" />
        </div>
        <div v-else>
          <div 
            class="comment-item" 
            v-for="comment in comments" 
            :key="comment.id"
          >
            <div class="comment-avatar">
              <el-avatar :size="40" :src="comment.userAvatar">
                {{ comment.userName ? comment.userName[0] : 'U' }}
              </el-avatar>
            </div>
            <div class="comment-main">
              <div class="comment-user">
                <span class="user-name">{{ comment.userName || '匿名用户' }}</span>
                <span class="comment-time">{{ formatCommentTime(comment.createTime) }}</span>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-footer">
                <el-button 
                  text 
                  size="small" 
                  :icon="comment.userLiked ? StarFilled : Star"
                  @click="likeComment(comment)"
                  :class="{ 'liked': comment.userLiked }"
                >
                  {{ comment.likeCount || 0 }}
                </el-button>
                <el-button 
                  text 
                  size="small" 
                  :icon="ChatLineRound"
                  @click="replyComment(comment)"
                >
                  回复
                </el-button>
              </div>

              <!-- 回复输入框 -->
              <div class="reply-editor" v-if="replyingTo === comment.id">
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="3"
                  :placeholder="`回复 @${comment.userName}...`"
                  maxlength="300"
                  show-word-limit
                />
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button size="small" type="primary" @click="submitReply(comment)" :loading="replying">
                    回复
                  </el-button>
                </div>
              </div>

              <!-- 回复列表 -->
              <div class="reply-list" v-if="comment.replies && comment.replies.length > 0">
                <div 
                  class="reply-item" 
                  v-for="reply in comment.replies" 
                  :key="reply.id"
                >
                  <div class="reply-avatar">
                    <el-avatar :size="30" :src="reply.userAvatar">
                      {{ reply.userName ? reply.userName[0] : 'U' }}
                    </el-avatar>
                  </div>
                  <div class="reply-main">
                    <div class="reply-user">
                      <span class="user-name">{{ reply.userName || '匿名用户' }}</span>
                      <span class="reply-to" v-if="reply.replyToUserName">
                        回复 @{{ reply.replyToUserName }}
                      </span>
                      <span class="reply-time">{{ formatCommentTime(reply.createTime) }}</span>
                    </div>
                    <div class="reply-content">{{ reply.content }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  getArticleDetail, 
  likeArticle, 
  unlikeArticle,
  getArticleComments,
  addArticleComment,
  likeArticleComment,
  unlikeArticleComment
} from '@/api/article'
import { ElMessage } from 'element-plus'
import { 
  User, Calendar, View, StarFilled, Star, Share, 
  ChatDotRound, ChatLineRound, ArrowLeft 
} from '@element-plus/icons-vue'

// 确保组件在路由上下文中
const instance = getCurrentInstance()
let router, route

try {
  router = useRouter()
  route = useRoute()
} catch (error) {
  console.error('Router injection failed:', error)
  // 如果在非路由上下文中，尝试从实例获取
  if (instance) {
    router = instance.appContext.config.globalProperties.$router
    route = instance.appContext.config.globalProperties.$route
  }
}

const loading = ref(false)
const liking = ref(false)
const commenting = ref(false)
const replying = ref(false)
const loadingComments = ref(false)

const article = ref(null)
const liked = ref(false)

const commentContent = ref('')
const replyContent = ref('')
const replyingTo = ref(null)
const comments = ref([])

const loadArticle = async () => {
  loading.value = true
  try {
    const res = await getArticleDetail(route.params.id)
    if (res.code === 1 || res.code === 200) {
      article.value = res.data
      // 加载评论
      loadComments()
    }
  } catch (error) {
    console.error('加载文章详情失败:', error)
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  loadingComments.value = true
  try {
    const res = await getArticleComments(route.params.id)
    if (res.code === 1 || res.code === 200) {
      comments.value = res.data || []
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loadingComments.value = false
  }
}

const handleLike = async () => {
  liking.value = true
  try {
    if (!liked.value) {
      await likeArticle(route.params.id)
      article.value.likeCount++
      liked.value = true
      ElMessage.success('点赞成功')
    } else {
      await unlikeArticle(route.params.id)
      article.value.likeCount--
      liked.value = false
      ElMessage.success('取消点赞')
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    liking.value = false
  }
}

const handleShare = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commenting.value = true
  try {
    await addArticleComment(route.params.id, {
      content: commentContent.value
    })
    
    commentContent.value = ''
    ElMessage.success('评论发表成功')
    
    // 重新加载评论列表和文章信息
    await Promise.all([loadComments(), loadArticle()])
  } catch (error) {
    console.error('发表评论失败:', error)
    ElMessage.error(error.response?.data?.msg || '发表失败')
  } finally {
    commenting.value = false
  }
}

const replyComment = (comment) => {
  replyingTo.value = comment.id
  replyContent.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

const submitReply = async (comment) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  replying.value = true
  try {
    await addArticleComment(route.params.id, {
      content: replyContent.value,
      parentId: comment.id,
      replyToUserId: comment.userId
    })
    
    cancelReply()
    ElMessage.success('回复成功')
    
    // 重新加载评论列表
    await loadComments()
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error(error.response?.data?.msg || '回复失败')
  } finally {
    replying.value = false
  }
}

const likeComment = async (comment) => {
  try {
    if (!comment.userLiked) {
      await likeArticleComment(comment.id)
      comment.likeCount = (comment.likeCount || 0) + 1
      comment.userLiked = true
    } else {
      await unlikeArticleComment(comment.id)
      comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
      comment.userLiked = false
    }
  } catch (error) {
    console.error('点赞评论失败:', error)
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const goBack = () => {
  router.push('/article')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const formatCommentTime = (dateStr) => {
  if (!dateStr) return ''
  
  const now = new Date()
  const date = new Date(dateStr)
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

const formatNumber = (num) => {
  if (!num) return 0
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

const getCategoryColor = (category) => {
  const colors = {
    1: '#409eff',
    2: '#67c23a',
    3: '#e6a23c',
    4: '#f56c6c',
    5: '#909399'
  }
  return colors[category] || '#909399'
}

const getCategoryIcon = (category) => {
  const icons = {
    1: '🎓',
    2: '🎯',
    3: '🏆',
    4: '📊',
    5: '💭'
  }
  return icons[category] || '📚'
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  min-height: 100vh;
}

/* 返回按钮 - 剧本杀风格 */
.back-button-wrapper {
  position: fixed;
  top: 80px;
  left: 20px;
  z-index: 100;
}

.back-button {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
  border: none;
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.back-button:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.4);
  background: linear-gradient(135deg, #a01010 0%, #b01515 100%);
}

/* 文章头部 - 剧本杀风格 */
.article-header {
  position: relative;
  height: 420px;
  margin-bottom: 40px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}

.header-background {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.header-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(8px);
  transform: scale(1.1);
  opacity: 0.3;
}

.header-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(26, 26, 46, 0.7) 0%, rgba(22, 33, 62, 0.9) 100%);
}

.header-overlay::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(139,0,0,0.1)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  background-size: 50px 50px;
}

.header-content {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  color: #fff;
}

.category-badge-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeInDown 0.6s ease-out;
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

.category-badge {
  padding: 10px 24px;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: rgba(139, 0, 0, 0.9);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.4);
}

.badge-top {
  padding: 10px 24px;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: #fff;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

.article-title-main {
  font-size: 40px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 30px;
  line-height: 1.4;
  text-align: center;
  text-shadow: 0 4px 20px rgba(139, 0, 0, 0.5);
  animation: fadeIn 0.8s ease-out 0.2s both;
  max-width: 900px;
  letter-spacing: 1px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.article-meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 30px;
  width: 100%;
  max-width: 900px;
  padding: 20px 30px;
  background: rgba(139, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.4);
  backdrop-filter: blur(20px);
  border-radius: 50px;
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

.meta-left,
.meta-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
}

.author-item {
  background: rgba(139, 0, 0, 0.4);
  padding: 5px 15px 5px 5px;
  border-radius: 25px;
}

.author-avatar {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
}

.author-name {
  font-weight: 600;
}

.stat-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 15px;
  background: rgba(139, 0, 0, 0.4);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  transition: all 0.3s;
}

.stat-badge:hover {
  background: rgba(139, 0, 0, 0.6);
  transform: translateY(-2px);
}

/* 文章主体 */
.article-main {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

/* 文章摘要盒子 - 剧本杀风格 */
.article-summary-box {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 28px 32px;
  border-radius: 20px;
  margin-bottom: 30px;
  display: flex;
  gap: 20px;
  align-items: flex-start;
  box-shadow: 0 8px 30px rgba(139, 0, 0, 0.2);
  animation: fadeInUp 0.6s ease-out;
  border: 1px solid rgba(139, 0, 0, 0.3);
}

.summary-icon {
  font-size: 36px;
  flex-shrink: 0;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.summary-text {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  line-height: 1.9;
  margin: 0;
  font-weight: 400;
}

/* 文章内容 - 卡片式排版 */
.article-content {
  background: transparent;
  padding: 0;
  font-size: 16px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 30px;
}

/* 段落卡片 */
.article-content :deep(p) {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  padding: 24px 28px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  text-indent: 2em;
  text-align: justify;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(139, 0, 0, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.article-content :deep(p:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(139, 0, 0, 0.25);
  border-color: rgba(139, 0, 0, 0.3);
}

/* 二级标题卡片 */
.article-content :deep(h2) {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  padding: 20px 24px;
  margin: 28px 0 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 15px rgba(26, 26, 46, 0.3);
}

.article-content :deep(h2::before) {
  content: '📖';
}

/* 三级标题卡片 */
.article-content :deep(h3) {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  padding: 16px 22px;
  margin: 24px 0 16px;
  border-radius: 10px;
  border-left: 4px solid #8B0000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

/* 四级标题 */
.article-content :deep(h4) {
  background: rgba(139, 0, 0, 0.15);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  padding: 14px 20px;
  margin: 20px 0 14px;
  border-radius: 8px;
  border-left: 3px solid rgba(139, 0, 0, 0.6);
}

/* 强调文字 */
.article-content :deep(strong) {
  color: #ff6b6b;
  font-weight: 600;
}

/* 斜体文字 */
.article-content :deep(em) {
  font-style: italic;
  color: rgba(255, 255, 255, 0.7);
}

/* 列表卡片 */
.article-content :deep(ul),
.article-content :deep(ol) {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  padding: 24px 28px 24px 48px;
  margin: 16px 0;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.article-content :deep(li) {
  margin-bottom: 12px;
  line-height: 1.8;
}

.article-content :deep(li:last-child) {
  margin-bottom: 0;
}

.article-content :deep(ul li::marker) {
  color: #8B0000;
}

.article-content :deep(ol li::marker) {
  color: #8B0000;
  font-weight: 600;
}

/* 引用块卡片 */
.article-content :deep(blockquote) {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.08) 0%, rgba(139, 0, 0, 0.03) 100%);
  padding: 24px 28px;
  margin: 16px 0;
  border-radius: 12px;
  border-left: 4px solid #8B0000;
  font-style: italic;
  color: #555;
}

.article-content :deep(blockquote p) {
  background: transparent;
  padding: 0;
  margin: 0;
  box-shadow: none;
  text-indent: 0;
}

.article-content :deep(blockquote p:hover) {
  transform: none;
  box-shadow: none;
}

/* 图片卡片 */
.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  margin: 16px 0;
  display: block;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

/* 分隔线 */
.article-content :deep(hr) {
  border: none;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(139, 0, 0, 0.2), transparent);
  margin: 28px 0;
}

/* 表格卡片 */
.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.article-content :deep(th) {
  background: rgba(139, 0, 0, 0.3);
  color: #fff;
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
}

.article-content :deep(td) {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.article-content :deep(tr:last-child td) {
  border-bottom: none;
}

.article-content :deep(tr:nth-child(even)) {
  background: rgba(139, 0, 0, 0.1);
}

/* 链接 */
.article-content :deep(a) {
  color: #8B0000;
  text-decoration: none;
  border-bottom: 1px solid rgba(139, 0, 0, 0.3);
  transition: all 0.2s ease;
}

.article-content :deep(a:hover) {
  color: #a01010;
  border-bottom-color: #a01010;
}

/* 代码 */
.article-content :deep(code) {
  background: rgba(139, 0, 0, 0.08);
  color: #8B0000;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', monospace;
  font-size: 0.9em;
}

.article-content :deep(pre) {
  background: #1a1a2e;
  color: #e0e0e0;
  padding: 20px 24px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 16px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.article-content :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .article-content :deep(p),
  .article-content :deep(ul),
  .article-content :deep(ol),
  .article-content :deep(blockquote) {
    padding: 20px 22px;
  }
  
  .article-content :deep(h2) {
    font-size: 18px;
    padding: 16px 20px;
  }
  
  .article-content :deep(h3) {
    font-size: 16px;
    padding: 14px 18px;
  }
}

/* 文章标签 */
.article-tags {
  margin-top: 30px;
  padding-top: 25px;
  border-top: 2px dashed rgba(139, 0, 0, 0.3);
}

.article-tags .el-tag {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.2) 0%, rgba(139, 0, 0, 0.1) 100%);
  border-color: rgba(139, 0, 0, 0.4);
  color: #ff6b6b;
}

/* 文章操作栏 - 剧本杀风格 */
.article-actions-bar {
  max-width: 900px;
  margin: 0 auto 40px;
  padding: 0 20px;
}

.actions-container {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  padding: 28px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
  display: flex;
  justify-content: center;
  gap: 20px;
  align-items: center;
}

.action-button {
  font-size: 16px;
  font-weight: 600;
  padding: 14px 32px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  border-radius: 25px;
}

.action-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.2);
}

.like-button {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
  border: none;
}

.like-button:hover {
  background: linear-gradient(135deg, #a01010 0%, #b01515 100%);
}

.like-button.is-liked {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  animation: pulse 0.6s ease-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.action-text {
  margin-right: 8px;
}

.action-count {
  background: rgba(255, 255, 255, 0.3);
  padding: 4px 14px;
  border-radius: 15px;
  font-size: 14px;
}

.share-button {
  background: rgba(35, 35, 60, 0.8);
  color: #ff6b6b;
  border-color: rgba(139, 0, 0, 0.5);
}

.share-button:hover {
  background: #8B0000;
  color: #fff;
  border-color: #8B0000;
}

/* 评论区样式 - 剧本杀风格 */
.comment-section {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

.comment-header {
  margin-bottom: 25px;
}

.comment-header h3 {
  font-size: 22px;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 28px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(139, 0, 0, 0.2);
}

.comment-editor {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  padding: 28px;
  border-radius: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 2px solid rgba(139, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.comment-editor:focus-within {
  border-color: #8B0000;
  box-shadow: 0 8px 30px rgba(139, 0, 0, 0.3);
}

.comment-editor :deep(.el-textarea__inner) {
  background: rgba(35, 35, 60, 0.9);
  border-color: rgba(139, 0, 0, 0.3);
  color: #fff;
}

.comment-editor :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

.comment-editor :deep(.el-input__count) {
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
}

.comment-actions {
  margin-top: 15px;
  text-align: right;
}

.comment-actions .el-button--primary {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  border: none;
  border-radius: 20px;
  padding: 10px 28px;
}

.comment-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #a01010 0%, #b01515 100%);
}

.comment-list {
  min-height: 200px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  padding: 28px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.empty-comments {
  padding: 60px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.comment-item {
  display: flex;
  gap: 16px;
  padding: 24px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 16px;
  border: 2px solid rgba(139, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.comment-item:hover {
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
  border-color: #8B0000;
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.2);
  transform: translateX(5px);
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-avatar .el-avatar {
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.15);
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.user-name {
  font-weight: 600;
  color: #fff;
  font-size: 15px;
}

.comment-time,
.reply-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.comment-content {
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.7;
  margin-bottom: 12px;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  gap: 16px;
}

.comment-footer .el-button {
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s;
}

.comment-footer .el-button:hover {
  color: #ff6b6b;
}

.comment-footer .liked {
  color: #ff6b6b;
}

/* 回复样式 */
.reply-editor {
  margin-top: 16px;
  padding: 16px;
  background: rgba(26, 26, 46, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.reply-editor :deep(.el-textarea__inner) {
  background: rgba(35, 35, 60, 0.9);
  border-color: rgba(139, 0, 0, 0.3);
  color: #fff;
}

.reply-editor :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

.reply-actions {
  margin-top: 12px;
  text-align: right;
}

.reply-actions .el-button--primary {
  background: #8B0000;
  border-color: #8B0000;
}

.reply-actions .el-button--default {
  background: rgba(35, 35, 60, 0.8);
  border-color: rgba(139, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.8);
}

.reply-list {
  margin-top: 16px;
  padding: 16px;
  background: rgba(26, 26, 46, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.15);
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(139, 0, 0, 0.15);
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-avatar .el-avatar {
  background: linear-gradient(135deg, #666 0%, #888 100%);
}

.reply-main {
  flex: 1;
  min-width: 0;
}

.reply-user {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.reply-to {
  color: #ff6b6b;
}

.reply-content {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

/* 响应式 */
@media (max-width: 768px) {
  .article-header {
    height: 350px;
  }

  .article-title-main {
    font-size: 26px;
  }

  .article-meta-bar {
    flex-direction: column;
    gap: 15px;
    padding: 15px 20px;
    border-radius: 20px;
  }

  .meta-left,
  .meta-right {
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
  }

  .article-content {
    padding: 24px;
    font-size: 15px;
  }

  .article-content :deep(h2) {
    font-size: 22px;
  }

  .article-content :deep(h3) {
    font-size: 18px;
  }

  .actions-container {
    flex-direction: column;
    gap: 12px;
  }

  .action-button {
    width: 100%;
  }

  .comment-item {
    flex-direction: column;
    gap: 12px;
  }

  .comment-header h3 {
    font-size: 18px;
    padding: 16px 20px;
  }
}
</style>
