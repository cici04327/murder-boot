<template>
  <div class="reviews-page">
    <div class="page-header">
      <h2>⭐ 我的评价</h2>
      <p class="sub">您发表过的所有评价记录</p>
    </div>

    <el-card class="main-card" v-loading="loading">
      <!-- 筛选 -->
      <div class="filter-bar">
        <el-button @click="fetchData" :icon="Search">刷新</el-button>
      </div>

      <!-- 列表 -->
      <div v-if="reviews.length > 0" class="review-list">
        <div class="review-item" v-for="item in reviews" :key="item.id">
          <!-- 头部：剧本+门店 -->
          <div class="item-header">
            <div class="item-title">
              <el-icon><Film /></el-icon>
              <span class="script-name">{{ item.scriptName || '未知剧本' }}</span>
              <span class="store-name" v-if="item.storeName">@ {{ item.storeName }}</span>
            </div>
            <div class="item-meta">
              <span class="create-time">{{ formatTime(item.createTime) }}</span>
            </div>
          </div>

          <!-- 评分行 -->
          <div class="rating-row">
            <span class="rating-label">综合</span>
            <el-rate :model-value="item.overallRating" disabled show-score size="small" />
            <template v-if="item.scriptRating">
              <span class="rating-label">剧本</span>
              <el-rate :model-value="item.scriptRating" disabled show-score size="small" />
            </template>
            <template v-if="item.storeRating">
              <span class="rating-label">门店</span>
              <el-rate :model-value="item.storeRating" disabled show-score size="small" />
            </template>
            <template v-if="item.dmRating">
              <span class="rating-label">DM</span>
              <el-rate :model-value="item.dmRating" disabled show-score size="small" />
            </template>
          </div>

          <!-- 评价内容 -->
          <div class="content-text">{{ item.content || '（未填写文字内容）' }}</div>

          <!-- 图片 -->
          <div class="images-row" v-if="item.images">
            <el-image
              v-for="(img, idx) in item.images.split(',')"
              :key="idx"
              :src="img"
              :preview-src-list="item.images.split(',')"
              fit="cover"
              class="review-img"
            />
          </div>

          <!-- 标签 -->
          <div class="tags-row" v-if="item.tags">
            <el-tag
              v-for="tag in item.tags.split(',')"
              :key="tag"
              size="small"
              type="info"
              class="tag-item"
            >{{ tag }}</el-tag>
          </div>


          <!-- 商家回复 -->
          <div class="reply-section" v-if="item.replyContent">
            <el-icon><ChatDotRound /></el-icon>
            <div class="reply-body">
              <span class="reply-label">商家回复：</span>
              <span class="reply-text">{{ item.replyContent }}</span>
              <span class="reply-time">{{ formatTime(item.replyTime) }}</span>
            </div>
          </div>

          <!-- 精选标识 -->
          <div class="featured-badge" v-if="item.isFeatured === 1">
            <el-tag type="warning" size="small" effect="dark">⭐ 精选评价</el-tag>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="!loading" description="您还没有发表过评价" :image-size="100">
        <el-button type="primary" @click="$router.push('/user/reservations')">去我的预约</el-button>
      </el-empty>

      <!-- 分页 -->
      <el-pagination
        v-if="total > pageSize"
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Film, Search, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const loading = ref(false)
const reviews = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const fetchData = async () => {
  if (!userStore.userId) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      userId: userStore.userId
    }
    const res = await request({ url: '/reservation/review/page', method: 'get', params })
    if (res.code === 1 || res.code === 200) {
      reviews.value = res.data.records || res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载评价失败')
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(fetchData)
</script>

<style scoped>
.reviews-page {
  --rv-text: rgba(255,255,255,0.92);
  --rv-text-sub: rgba(255,255,255,0.6);
  --rv-bg: rgba(255,255,255,0.05);
  --rv-border: rgba(255,255,255,0.10);
  max-width: 860px;
  margin: 20px auto;
  padding: 0 20px;
}

:global(.theme-light) .reviews-page,
:global(.theme-default) .reviews-page,
:global(.theme-purple) .reviews-page,
:global(.theme-warm) .reviews-page,
:global(.theme-nature) .reviews-page,
:global(.theme-aurora) .reviews-page {
  --rv-text: #1a1a2e;
  --rv-text-sub: #666;
  --rv-bg: rgba(0,0,0,0.04);
  --rv-border: rgba(0,0,0,0.09);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: var(--rv-text);
}

.sub {
  margin: 4px 0 0;
  color: var(--rv-text-sub);
  font-size: 13px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 20px;
  background: var(--rv-bg);
  border: 1px solid var(--rv-border);
  border-radius: 10px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--rv-text);
  font-size: 15px;
  font-weight: 600;
}

.store-name {
  color: var(--rv-text-sub);
  font-size: 13px;
  font-weight: 400;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.create-time {
  color: var(--rv-text-sub);
  font-size: 12px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.rating-label {
  font-size: 12px;
  color: var(--rv-text-sub);
}

.content-text {
  color: var(--rv-text);
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 10px;
}

.images-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.review-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--rv-border);
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-item {
  border-radius: 12px;
}

.audit-reason {
  margin: 10px 0;
}

.reply-section {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  background: rgba(103, 194, 58, 0.08);
  border-left: 3px solid #67c23a;
  border-radius: 0 8px 8px 0;
}

.reply-body {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
  color: var(--rv-text);
}

.reply-label {
  font-weight: 600;
  color: #67c23a;
}

.reply-time {
  color: var(--rv-text-sub);
  font-size: 12px;
  margin-left: auto;
}

.featured-badge {
  margin-top: 8px;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
  display: flex;
}
</style>
