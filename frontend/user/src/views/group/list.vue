<template>
  <div class="group-list-page">
    <!-- 剧本杀风格头部 -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="header-icon">🎭</div>
        <h1>神秘车站</h1>
        <p class="header-subtitle">寻找命运的同行者，开启未知的剧本之旅</p>
        <el-button type="primary" class="create-btn" @click="router.push('/group/create')">
          <el-icon><Plus /></el-icon>
          发起组局
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <span class="filter-label">状态</span>
        <div class="filter-tags">
          <span class="filter-tag" :class="{ active: filters.status === null }" @click="setFilter('status', null)">全部</span>
          <span class="filter-tag" :class="{ active: filters.status === 1 }" @click="setFilter('status', 1)">召集中</span>
          <span class="filter-tag" :class="{ active: filters.status === 2 }" @click="setFilter('status', 2)">已成团</span>
        </div>
      </div>
      <div class="filter-row">
        <span class="filter-label">人数</span>
        <div class="filter-tags">
          <span class="filter-tag" :class="{ active: filters.playerCount === null }" @click="setFilter('playerCount', null)">全部</span>
          <span class="filter-tag" :class="{ active: filters.playerCount === 4 }" @click="setFilter('playerCount', 4)">4人本</span>
          <span class="filter-tag" :class="{ active: filters.playerCount === 5 }" @click="setFilter('playerCount', 5)">5人本</span>
          <span class="filter-tag" :class="{ active: filters.playerCount === 6 }" @click="setFilter('playerCount', 6)">6人本</span>
          <span class="filter-tag" :class="{ active: filters.playerCount === 7 }" @click="setFilter('playerCount', 7)">7人本</span>
          <span class="filter-tag" :class="{ active: filters.playerCount === 8 }" @click="setFilter('playerCount', 8)">8人本+</span>
        </div>
      </div>
      <div class="filter-row" v-if="categories.length > 0">
        <span class="filter-label">类型</span>
        <div class="filter-tags">
          <span class="filter-tag" :class="{ active: filters.categoryId === null }" @click="setFilter('categoryId', null)">全部</span>
          <span class="filter-tag" v-for="cat in categories" :key="cat.id" :class="{ active: filters.categoryId === cat.id }" @click="setFilter('categoryId', cat.id)">{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <!-- 拼单列表 -->
    <div class="group-list" v-loading="loading">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="group in groups" :key="group.id">
          <div class="group-card" @click="router.push(`/group/${group.id}`)">
            <!-- 剧本杀风格卡片头部 -->
            <div class="group-header">
              <div class="mystery-icon">🎭</div>
              <div class="group-creator">
                <span class="creator-name">{{ group.creatorName || '神秘车主' }}</span>
                <span class="create-time">{{ formatTime(group.createTime) }} 发起</span>
              </div>
              <div class="group-status" :class="getStatusClass(group.status)">
                {{ getStatusText(group.status) }}
              </div>
            </div>
            
            <div class="group-content">
              <h4 class="group-title">📜 {{ group.scriptName }}</h4>
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
              
              <!-- 座位可视化 -->
              <div class="seats-visual">
                <div class="seats-label">🪑 座位情况</div>
                <div class="seats-container">
                  <div 
                    v-for="n in group.needCount" 
                    :key="n" 
                    class="seat-item"
                    :class="{ 'seat-taken': n <= group.currentCount }"
                  >
                    <span class="seat-icon">{{ n <= group.currentCount ? '🎭' : '❓' }}</span>
                  </div>
                </div>
                <div class="seats-text">
                  <span v-if="group.needCount - group.currentCount > 0" class="spots-left">
                    还差 <strong>{{ group.needCount - group.currentCount }}</strong> 位玩家
                  </span>
                  <span v-else class="spots-full">✨ 人员已齐</span>
                </div>
              </div>
              
              <div class="group-tags">
                <el-tag size="small" type="info">{{ group.playerCount }}人本</el-tag>
                <el-tag size="small" v-if="group.genderRequirement">{{ group.genderRequirement }}</el-tag>
                <el-tag size="small" type="warning" v-if="group.newbieWelcome">🌟 新手友好</el-tag>
              </div>
            </div>
            
            <div class="group-footer">
              <span class="group-price">¥{{ group.price }}<small>/人</small></span>
              <el-button 
                size="small" 
                type="primary" 
                :disabled="group.currentCount >= group.needCount || group.status !== 1"
              >
                {{ group.currentCount >= group.needCount ? '已满员' : '🚗 上车' }}
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-if="!loading && groups.length === 0" description="暂无神秘局等待组建">
        <el-button type="primary" @click="router.push('/group/create')">发起组局</el-button>
      </el-empty>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadGroups"
          @current-change="loadGroups"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Shop, Clock } from '@element-plus/icons-vue'
import { getGroupList } from '@/api/group'
import { getScriptCategories } from '@/api/script'

const router = useRouter()

const loading = ref(false)
const groups = ref([])
const categories = ref([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filters = reactive({
  categoryId: null,
  playerCount: null,
  status: null
})

// 设置筛选条件
const setFilter = (key, value) => {
  filters[key] = value
  page.value = 1
  loadGroups()
}

// 加载拼单列表
const loadGroups = async () => {
  loading.value = true
  try {
    const res = await getGroupList({
      page: page.value,
      pageSize: pageSize.value,
      ...filters
    })
    if (res.code === 1 || res.code === 200) {
      groups.value = res.data?.records || res.data || []
      total.value = res.data?.total || groups.value.length
    }
  } catch (error) {
    console.error('加载拼单失败:', error)
    groups.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 加载分类
const loadCategories = async () => {
  try {
    const res = await getScriptCategories()
    if (res.code === 1 || res.code === 200) {
      categories.value = res.data || []
    }
  } catch (error) {
    console.error('加载分类失败')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60 / 1000)}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 60 / 60 / 1000)}小时前`
  return `${Math.floor(diff / 24 / 60 / 60 / 1000)}天前`
}

const formatPlayTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hours}:${minutes}`
}

const getStatusText = (status) => {
  const map = { 0: '已取消', 1: '拼团中', 2: '已成团', 3: '已结束' }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = { 0: 'status-cancelled', 1: 'status-active', 2: 'status-success', 3: 'status-ended' }
  return map[status] || ''
}

onMounted(() => {
  loadCategories()
  loadGroups()
})
</script>

<style scoped>
.group-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 剧本杀风格头部 */
.page-header {
  position: relative;
  text-align: center;
  padding: 60px 20px;
  border-radius: 20px;
  margin-bottom: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 0, 0, 0.2) 0%, transparent 50%);
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.header-content {
  position: relative;
  z-index: 1;
}

.header-icon {
  font-size: 48px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.page-header h1 {
  font-size: 36px;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  letter-spacing: 4px;
}

.header-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 24px;
  letter-spacing: 2px;
}

.create-btn {
  background: linear-gradient(135deg, #8B0000 0%, #c41e3a 100%);
  border: none;
  padding: 12px 32px;
  font-size: 16px;
  border-radius: 25px;
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.4);
  transition: all 0.3s;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(139, 0, 0, 0.5);
  background: linear-gradient(135deg, #a00000 0%, #d42e4a 100%);
}

/* 筛选区域 */
.filter-section {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border: 1px solid rgba(139, 0, 0, 0.2);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  min-width: 36px;
  flex-shrink: 0;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tag {
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.06);
  transition: all 0.2s;
  user-select: none;
}

.filter-tag:hover {
  border-color: rgba(139, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.9);
  background: rgba(139, 0, 0, 0.15);
}

.filter-tag.active {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.8), rgba(180, 0, 0, 0.7));
  border-color: rgba(180, 0, 0, 0.6);
  color: #fff;
  font-weight: 500;
}

.group-list {
  min-height: 400px;
}

/* 剧本杀风格卡片 */
.group-card {
  background: #16213e;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
  border: 1px solid #eee;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.group-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(139, 0, 0, 0.3);
}

.group-header {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-bottom: none;
}

.mystery-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: rgba(139, 0, 0, 0.3);
  border-radius: 50%;
  margin-right: 12px;
}

.group-creator {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.creator-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.create-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.group-status {
  padding: 5px 12px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: rgba(139, 0, 0, 0.8);
  color: #fff;
  animation: glow 2s ease-in-out infinite;
}

@keyframes glow {
  0%, 100% { box-shadow: 0 0 5px rgba(139, 0, 0, 0.5); }
  50% { box-shadow: 0 0 15px rgba(139, 0, 0, 0.8); }
}

.status-success {
  background: rgba(103, 194, 58, 0.9);
  color: #fff;
}

.status-cancelled {
  background: rgba(153, 153, 153, 0.8);
  color: #fff;
}

.status-ended {
  background: rgba(144, 147, 153, 0.8);
  color: #fff;
}

.group-content {
  padding: 16px;
}

.group-title {
  margin: 0 0 12px;
  font-size: 17px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.4;
}

.group-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.group-info .info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.group-info .info-item .el-icon {
  color: #8B0000;
}

/* 座位可视化 */
.seats-visual {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 14px;
  border: 1px dashed #ddd;
}

.seats-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}

.seats-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.seat-item {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  transition: all 0.3s;
}

.seat-item.seat-taken {
  background: linear-gradient(135deg, #8B0000 0%, #c41e3a 100%);
  box-shadow: 0 2px 8px rgba(139, 0, 0, 0.3);
}

.seat-icon {
  font-size: 16px;
}

.seats-text {
  font-size: 13px;
  text-align: center;
}

.spots-left {
  color: #e6a23c;
}

.spots-left strong {
  color: #8B0000;
  font-size: 16px;
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
  padding: 14px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(13, 27, 42, 0.95);
}

.group-price {
  font-size: 22px;
  font-weight: bold;
  color: #8B0000;
}

.group-price small {
  font-size: 13px;
  font-weight: normal;
  color: rgba(255, 255, 255, 0.6);
}

.group-footer .el-button--primary {
  background: linear-gradient(135deg, #8B0000 0%, #c41e3a 100%);
  border: none;
  border-radius: 20px;
  padding: 8px 20px;
}

.group-footer .el-button--primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #a00000 0%, #d42e4a 100%);
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .page-header {
    padding: 40px 20px;
  }
  
  .page-header h1 {
    font-size: 28px;
  }
  
  .header-subtitle {
    font-size: 14px;
  }
}
</style>
