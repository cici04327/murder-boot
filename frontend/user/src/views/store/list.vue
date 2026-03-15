<template>
  <div class="store-list-container">
    <!-- 剧本杀主题头部横幅 -->
    <div class="store-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-icon">🏠</div>
        <h1>探索门店</h1>
        <p>发现身边优质剧本杀门店，开启沉浸式推理之旅</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">100+</span>
            <span class="stat-label">合作门店</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">50+</span>
            <span class="stat-label">覆盖城市</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">4.8</span>
            <span class="stat-label">平均评分</span>
          </div>
        </div>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <span class="search-title">🔎 筛选门店</span>
        <span class="store-count">共 <strong>{{ total }}</strong> 家门店</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="🔤 关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索门店名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="📍 地区">
          <el-input
            v-model="searchForm.address"
            placeholder="输入地址"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
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

    <!-- 位置提示卡片 -->
    <transition name="slide-fade">
      <div v-if="!userLocation && !locationDenied" class="location-tip-card" @click="showLocationDialog">
        <div class="tip-icon">
          <el-icon><Location /></el-icon>
        </div>
        <div class="tip-content">
          <div class="tip-title">开启位置服务</div>
          <div class="tip-desc">查看门店距离，发现附近优质门店</div>
        </div>
        <el-button type="primary" size="small" round>
          <el-icon><Location /></el-icon>
          开启
        </el-button>
      </div>
    </transition>

    <!-- 位置权限弹窗 -->
    <LocationPermissionDialog
      v-model="locationDialogVisible"
      @success="handleLocationSuccess"
      @deny="handleLocationDeny"
    />

    <div class="list-header">
      <span class="total">
        共 {{ total }} 家门店
        <el-tag v-if="userLocation" type="success" size="small" style="margin-left: 10px;">
          <el-icon><LocationInformation /></el-icon>
          距离已显示
        </el-tag>
      </span>
      <div class="sort-buttons">
        <div class="radio-group-wrapper">
          <el-radio-group v-model="searchForm.sortBy" @change="handleSortChange">
            <el-radio-button label="distance">
              <el-icon><Location /></el-icon>
              距离最近
            </el-radio-button>
            <el-radio-button label="rating">
              <el-icon><Star /></el-icon>
              评分最高
            </el-radio-button>
            <el-radio-button label="hot">
              <el-icon><TrendCharts /></el-icon>
              最热门
            </el-radio-button>
          </el-radio-group>
          <transition name="fade">
            <span v-if="searchForm.sortBy === 'distance' && searchForm.latitude" class="location-info">
              <el-icon><CircleCheck /></el-icon>
              已定位
            </span>
          </transition>
        </div>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading">
      <SkeletonStoreCard v-for="i in searchForm.pageSize" :key="i" />
    </div>
    
    <!-- 真实内容 -->
    <el-row :gutter="20" v-else>
      <el-col :xs="24" :md="12" v-for="store in stores" :key="store.id">
        <div class="store-card" @click="goToDetail(store.id)">
          <div class="store-image">
            <LazyImage :src="store.coverImage || getStoreCover(store.name)" :alt="store.name" :height="220" />
            <!-- 徽章 -->
            <div class="store-badge gold" v-if="store.rating >= 4.5">🏆 优质门店</div>
            <div class="store-badge hot" v-else-if="store.reviewCount >= 50">🔥 人气门店</div>
            <!-- 剧本数量 -->
            <div class="script-count-badge">📜 {{ store.scriptCount || 50 }}+剧本</div>
            <!-- 悬浮遮罩 -->
            <div class="image-overlay">
              <span class="overlay-btn">🏠 进入门店</span>
            </div>
          </div>
          <div class="store-info">
            <div class="store-header">
              <h3>{{ store.name }}</h3>
              <span v-if="store.distance" class="distance-tag">
                📍 {{ formatDistance(store.distance) }}
              </span>
            </div>
            <div class="store-address">
              <span class="address-icon">📍</span>
              {{ store.address }}
            </div>
            <div class="store-contact">
              <span class="contact-item" v-if="store.phone">
                <span class="contact-icon">📞</span>{{ store.phone }}
              </span>
              <span class="contact-item" v-if="store.openTime && store.closeTime">
                <span class="contact-icon">🕐</span>{{ store.openTime }} - {{ store.closeTime }}
              </span>
            </div>
            <div class="store-rating">
              <el-rate v-model="store.rating" disabled size="small" />
              <span class="rating-value">{{ store.rating?.toFixed(1) || '5.0' }}</span>
              <span class="review-count">{{ store.reviewCount || 0 }}条评价</span>
            </div>
            <!-- 门店特色 -->
            <div class="store-features">
              <span class="feature-tag">🎭 换装</span>
              <span class="feature-tag">🏰 实景</span>
              <span class="feature-tag">🎬 NPC</span>
            </div>
            <div class="store-tags" v-if="store.tags && store.tags.length">
              <span class="tag-item" v-for="tag in store.tags" :key="tag">{{ tag }}</span>
            </div>
            <div class="store-footer">
              <el-button type="danger" size="small" @click.stop="handleReserve(store)">
                🎭 立即预约
              </el-button>
              <el-button size="small" @click.stop="handleCall(store)">
                📞 电话咨询
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && stores.length === 0" description="暂无门店" />

    <el-pagination
      v-if="total > 0"
      class="pagination"
      v-model:current-page="searchForm.page"
      v-model:page-size="searchForm.pageSize"
      :total="total"
      layout="total, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreListAdvanced } from '@/api/store'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { getUserLocation as getUserLocationUtil, formatDistance as formatDistanceUtil, calculateDistance, requestLocationPermission } from '@/utils/location'
import SkeletonStoreCard from '@/components/Skeleton/SkeletonStoreCard.vue'
import LazyImage from '@/components/LazyImage.vue'
import LocationPermissionDialog from '@/components/LocationPermissionDialog.vue'
import { getStoreCover } from '@/assets/store-covers'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const locationDialogVisible = ref(false)
const locationDenied = ref(false)
const stores = ref([])
const total = ref(0)
const userLocation = ref(null) // 存储用户位置

const searchForm = reactive({
  keyword: '',
  address: '',
  sortBy: 'rating',
  latitude: null,
  longitude: null,
  page: 1,
  pageSize: 10
})

const loadStores = async () => {
  loading.value = true
  try {
    console.log('加载门店列表，参数:', { page: searchForm.page, pageSize: searchForm.pageSize })
    // 构建查询参数
    const queryData = {
      name: searchForm.keyword,
      address: searchForm.address,
      sortBy: searchForm.sortBy,
      page: searchForm.page,
      pageSize: searchForm.pageSize
    }
    
    // 如果是距离排序，添加经纬度信息
    if (searchForm.sortBy === 'distance' && searchForm.latitude && searchForm.longitude) {
      queryData.latitude = searchForm.latitude
      queryData.longitude = searchForm.longitude
    }
    
    const res = await getStoreListAdvanced(queryData)
    if (res.data) {
      if (Array.isArray(res.data)) {
        stores.value = res.data
        total.value = res.data.length
      } else {
        stores.value = res.data.records || res.data.list || []
        total.value = res.data.total || 0
      }
      
      // 将后端 images 字段（逗号分隔字符串）转换为前端所需的 coverImage
      stores.value.forEach(store => {
        if (!store.coverImage && store.images) {
          const imageList = store.images.split(',').map(img => img.trim()).filter(img => img)
          if (imageList.length > 0) {
            store.coverImage = imageList[0]
          }
        }
      })

      // 自动计算所有门店的距离
      calculateStoresDistance()
    }
  } catch (error) {
    console.error('加载门店列表失败:', error)
    ElMessage.error('加载门店列表失败，请稍后重试')
    stores.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 计算所有门店的距离
const calculateStoresDistance = () => {
  // 如果没有用户位置，不计算
  if (!userLocation.value) return
  
  // 为每个门店计算距离
  stores.value.forEach(store => {
    if (store.latitude && store.longitude) {
      // 计算距离（公里）
      const distance = calculateDistance(
        userLocation.value.latitude,
        userLocation.value.longitude,
        store.latitude,
        store.longitude
      )
      // 添加distance属性
      store.distance = distance
    }
  })
}

const handlePageChange = (newPage) => {
  console.log('门店列表页码变化:', newPage)
  searchForm.page = newPage
  loadStores()
}

const handleSizeChange = (newSize) => {
  console.log('门店列表每页大小变化:', newSize)
  searchForm.pageSize = newSize
  searchForm.page = 1
  loadStores()
}

const handleSearch = () => {
  searchForm.page = 1
  // 如果选择距离排序但没有位置信息，先获取位置
  if (searchForm.sortBy === 'distance' && !searchForm.latitude) {
    getUserLocation()
  } else {
    loadStores()
  }
}

// 处理排序方式改变
const handleSortChange = (value) => {
  searchForm.page = 1
  if (value === 'distance' && !searchForm.latitude) {
    getUserLocation()
  } else {
    loadStores()
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.address = ''
  searchForm.sortBy = 'rating'
  handleSearch()
}

const goToDetail = (id) => {
  router.push(`/store/${id}`)
}

const handleReserve = (store) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push({
    path: '/reservation/schedule',
    query: { storeId: store.id }
  })
}

const handleCall = (store) => {
  if (store.phone) {
    window.location.href = `tel:${store.phone}`
  } else {
    ElMessage.info('暂无联系电话')
  }
}

// 格式化距离显示（使用统一工具函数）
const formatDistance = (distance) => {
  return formatDistanceUtil(distance)
}

// 获取用户地理位置（使用统一工具函数）
const getUserLocation = async () => {
  loading.value = true
  ElMessage.info('正在获取您的位置...')
  
  try {
    const location = await getUserLocationUtil()
    searchForm.latitude = location.latitude
    searchForm.longitude = location.longitude
    console.log('获取位置成功:', location)
    ElMessage.success('位置获取成功')
    loadStores()
  } catch (error) {
    loading.value = false
    console.error('获取位置失败:', error)
    
    let errorMsg = '获取位置失败，将按评分排序'
    if (error.code === 1) {
      errorMsg = '您拒绝了位置权限请求，将按评分排序'
    } else if (error.code === 2) {
      errorMsg = '位置信息不可用，将按评分排序'
    } else if (error.code === 3) {
      errorMsg = '获取位置超时，将按评分排序'
    } else if (error.message && error.message.includes('不支持')) {
      errorMsg = '您的浏览器不支持地理定位，将按评分排序'
    }
    
    ElMessage.warning(errorMsg)
    // 降级到评分排序
    searchForm.sortBy = 'rating'
    loadStores()
  }
}

// 显示位置权限弹窗
const showLocationDialog = () => {
  locationDialogVisible.value = true
}

// 位置获取成功回调
const handleLocationSuccess = (location) => {
  userLocation.value = location
  searchForm.latitude = location.latitude
  searchForm.longitude = location.longitude
  locationDenied.value = false
  
  // 重新计算已加载门店的距离
  if (stores.value.length > 0) {
    calculateStoresDistance()
  }
  
  // 如果当前是按距离排序，重新加载
  if (searchForm.sortBy === 'distance') {
    loadStores()
  }
}

// 位置获取拒绝回调
const handleLocationDeny = () => {
  locationDenied.value = true
  // 标记本次会话已询问过
  sessionStorage.setItem('location_permission_asked', 'true')
}

// 初始化用户位置：仅在用户之前已主动授权过时才静默恢复缓存，不弹窗、不自动请求
const initUserLocation = () => {
  const hasGrantedInApp = localStorage.getItem('location_permission_granted') === 'true'
  const hasDeniedInApp = localStorage.getItem('location_permission_denied') === 'true'
  const hasAskedThisSession = sessionStorage.getItem('location_permission_asked') === 'true'

  // 用户明确拒绝过，隐藏提示卡片，不再打扰
  if (hasDeniedInApp || hasAskedThisSession) {
    locationDenied.value = true
    return
  }

  // 用户之前已主动授权过，尝试从缓存恢复（不弹任何弹窗）
  if (hasGrantedInApp) {
    const cached = localStorage.getItem('user_location')
    if (cached) {
      try {
        const { location, timestamp } = JSON.parse(cached)
        const age = Date.now() - timestamp
        if (age < 5 * 60 * 1000) {
          // 缓存5分钟内有效，直接使用
          userLocation.value = location
          searchForm.latitude = location.latitude
          searchForm.longitude = location.longitude
          calculateStoresDistance()
          return
        }
      } catch (e) { /* 忽略解析错误 */ }
    }
    // 缓存过期但用户曾授权过 → 显示提示卡片让用户再次点击获取
    // 不自动重新请求，避免意外弹出浏览器权限框
  }

  // 其余情况（首次访问等）：什么都不做，提示卡片已通过 v-if 显示，等用户主动点击
}

onMounted(() => {
  loadStores()
  initUserLocation()
})
</script>

<style scoped>
.store-list-container {
  padding: 20px;
  background: transparent;
  min-height: 100vh;
}

/* 头部横幅 */
.store-hero {
  position: relative;
  height: 240px;
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
  background-image: url('https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=1200&h=300&fit=crop');
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

.hero-stats {
  display: flex;
  gap: 40px;
}

.hero-stats .stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #ffd700;
}

.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
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

.store-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.store-count strong {
  color: #ff6b6b;
  font-size: 18px;
}

.search-form {
  margin: 0;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 18px 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.total {
  font-size: 14px;
  color: #fff;
}

.sort-buttons {
  display: flex;
  align-items: center;
}

.radio-group-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.sort-buttons .el-radio-button :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 5px;
}

.location-info {
  position: absolute;
  right: -90px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #67c23a;
  padding: 5px 10px;
  background: rgba(103, 194, 58, 0.2);
  border-radius: 4px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 位置提示卡片 */
.location-tip-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border: 1px solid rgba(139, 0, 0, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.location-tip-card:hover {
  border-color: #8B0000;
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.3);
  transform: translateY(-2px);
}

.location-tip-card .tip-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.location-tip-card .tip-content {
  flex: 1;
}

.location-tip-card .tip-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.location-tip-card .tip-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.location-tip-card .el-button {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  border: none;
}

.location-tip-card .el-button:hover {
  background: linear-gradient(135deg, #a01010 0%, #c01515 100%);
}

/* 位置提示样式（保留兼容） */
.location-tip {
  margin-bottom: 20px;
  border-radius: 8px;
}

/* 淡入淡出动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* 滑动淡入动画 */
.slide-fade-enter-active {
  transition: all 0.4s ease;
}

.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 门店卡片 - 优化版 */
.store-card {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  display: flex;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.store-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(139, 0, 0, 0.35);
  border-color: #8B0000;
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
}

.store-image {
  position: relative;
  width: 300px;
  height: 220px;
  flex-shrink: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.store-image :deep(.lazy-image) {
  width: 100%;
  height: 100%;
}

.store-image :deep(.lazy-image img),
.store-image :deep(img),
.store-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.4s ease;
}

.store-card:hover .store-image img {
  transform: scale(1.08);
}

/* 图片遮罩 */
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

.store-card:hover .image-overlay {
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

.store-card:hover .overlay-btn {
  transform: translateY(0);
}

/* 徽章 */
.store-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  z-index: 2;
}

.store-badge.gold {
  background: linear-gradient(135deg, #ffd700 0%, #ff8c00 100%);
  color: #1a1a2e;
}

.store-badge.hot {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: #fff;
}

.script-count-badge {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 5px 12px;
  border-radius: 15px;
  font-size: 12px;
  z-index: 2;
}

.store-info {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.store-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.store-info h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  flex: 1;
}

.store-address {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
}

.address-icon {
  font-size: 14px;
}

.store-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.contact-icon {
  font-size: 14px;
}

.distance-tag {
  padding: 4px 12px;
  background: rgba(139, 0, 0, 0.3);
  color: #ff6b6b;
  font-size: 12px;
  border-radius: 15px;
  font-weight: 500;
  white-space: nowrap;
}

.store-rating {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.rating-value {
  font-size: 16px;
  font-weight: 700;
  color: #faad14;
}

.review-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 门店特色 */
.store-features {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.feature-tag {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(139, 0, 0, 0.25);
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;
}

.tag-item {
  font-size: 12px;
  color: #ff6b6b;
  background: rgba(139, 0, 0, 0.2);
  padding: 4px 12px;
  border-radius: 12px;
}

.store-footer {
  margin-top: auto;
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid rgba(139, 0, 0, 0.2);
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .store-card {
    flex-direction: column;
  }
  
  .store-image {
    width: 100%;
    height: 200px;
  }
}
</style>
