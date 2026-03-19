<template>
  <div class="store-detail-container" v-loading="loading">
    <!-- 数据加载失败提示 -->
    <el-empty 
      v-if="!loading && !store" 
      description="门店信息加载失败"
      :image-size="200"
    >
      <el-button type="primary" @click="loadStoreDetail">
        重新加载
      </el-button>
    </el-empty>
    
    <el-card v-if="store" class="store-main-card">
      <!-- 门店头部信息 -->
      <div class="store-header">
        <el-row :gutter="30">
          <el-col :xs="24" :md="12">
            <div class="store-images">
              <el-carousel height="400px">
                <el-carousel-item v-for="(image, index) in storeImages" :key="index">
                  <img :src="image" :alt="store.name" />
                </el-carousel-item>
              </el-carousel>
            </div>
          </el-col>
          
          <el-col :xs="24" :md="12">
            <div class="store-basic-info">
              <h1>{{ store.name }}</h1>
              
              <div class="store-rating">
                <el-rate v-model="store.rating" disabled show-score size="large" />
                <span class="review-count">({{ actualReviewCount }}条评价)</span>
              </div>
              
              <el-descriptions :column="1" class="store-desc">
                <el-descriptions-item>
                  <template #label>
                    <el-icon><Location /></el-icon>
                    地址
                  </template>
                  <div class="address-with-distance">
                    <span>{{ store.address }}</span>
                    <el-tag v-if="distance" type="info" size="small" class="distance-tag">
                      <el-icon><LocationInformation /></el-icon>
                      距离我 {{ distance }}
                    </el-tag>
                    <el-button 
                      v-if="!distance && !locationError" 
                      type="primary" 
                      text 
                      size="small"
                      @click="showLocationPermissionDialog"
                      :loading="locationLoading"
                    >
                      <el-icon><Location /></el-icon>
                      获取距离
                    </el-button>
                    <el-tooltip v-if="locationError" :content="locationError" placement="top">
                      <el-tag type="warning" size="small" @click="showLocationPermissionDialog" style="cursor: pointer;">
                        <el-icon><Warning /></el-icon>
                        点击重试
                      </el-tag>
                    </el-tooltip>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item>
                  <template #label>
                    <el-icon><Phone /></el-icon>
                    电话
                  </template>
                  {{ store.phone }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template #label>
                    <el-icon><Clock /></el-icon>
                    营业时间
                  </template>
                  <el-tag 
                    v-if="businessStatus"
                    :type="businessStatus.open ? 'success' : 'danger'"
                    size="small"
                    style="margin-right: 8px;"
                    :effect="businessStatus.urgent || businessStatus.soon ? 'dark' : 'plain'"
                  >
                    {{ businessStatus.text }}
                  </el-tag>
                  {{ store.openTime }} - {{ store.closeTime }}
                </el-descriptions-item>
              </el-descriptions>
              
              <div class="store-tags">
                <el-tag v-for="tag in store.tags" :key="tag" type="success">{{ tag }}</el-tag>
              </div>
              
              <div class="action-buttons">
                <el-button class="btn-reserve" size="large" @click="handleReserve">
                  <el-icon><Calendar /></el-icon>
                  立即预约
                </el-button>
                <el-button class="btn-call" size="large" @click="handleCall">
                  <el-icon><Phone /></el-icon>
                  电话咨询
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
    
    <!-- 门店介绍 -->
    <el-card class="detail-card store-intro-card">
      <template #header>
        <div class="card-header">
          <span>📖 门店介绍</span>
          <el-tag type="warning" size="small">
            ⭐ {{ store?.rating || 4.8 }} 分
          </el-tag>
        </div>
      </template>
      
      <!-- 门店特色标签 -->
      <div class="store-highlights" v-if="store?.highlights && store.highlights.length > 0">
        <h4 class="section-title">
          <el-icon><TrendCharts /></el-icon>
          门店特色
        </h4>
        <div class="highlight-tags">
          <el-tag 
            v-for="(highlight, index) in store.highlights" 
            :key="index"
            :type="getHighlightType(index)"
            effect="plain"
            size="large"
            class="highlight-tag"
          >
            <el-icon><Check /></el-icon>
            {{ highlight }}
          </el-tag>
        </div>
      </div>
      
      <!-- 门店设施 -->
      <div class="store-facilities" v-if="store?.facilities && store.facilities.length > 0">
        <h4 class="section-title">
          <el-icon><Medal /></el-icon>
          门店设施
        </h4>
        <el-row :gutter="15" class="facility-list">
          <el-col :xs="12" :sm="8" :md="6" v-for="(facility, index) in store.facilities" :key="index">
            <div class="facility-item">
              <el-icon :color="getFacilityColor(index)">
                <component :is="getFacilityIcon(facility)" />
              </el-icon>
              <span>{{ facility }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 门店简介 -->
      <div class="store-description-section">
        <h4 class="section-title">
          <el-icon><Document /></el-icon>
          门店简介
        </h4>
        <div class="store-description" v-html="store?.description || '这是一家优质的剧本杀门店，环境舒适，服务专业，拥有丰富的剧本资源和专业的DM团队。'"></div>
      </div>
      
      <!-- 营业信息 -->
      <div class="store-business-info">
        <h4 class="section-title">
          <el-icon><Clock /></el-icon>
          营业信息
        </h4>
        <el-row :gutter="20" class="business-info-grid">
          <el-col :xs="24" :sm="12" :md="6">
            <div class="info-card">
              <div class="info-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="info-content">
                <div class="info-label">营业时间</div>
                <div class="info-value">
                  <el-tag 
                    v-if="businessStatus"
                    :type="businessStatus.open ? 'success' : 'danger'"
                    size="small"
                    style="margin-right: 8px;"
                    :effect="businessStatus.urgent || businessStatus.soon ? 'dark' : 'plain'"
                  >
                    {{ businessStatus.text }}
                  </el-tag>
                  {{ store?.openTime || '10:00' }} - {{ store?.closeTime || '22:00' }}
                </div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="info-card">
              <div class="info-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="info-content">
                <div class="info-label">已服务</div>
                <div class="info-value">{{ store?.serviceCount || '5000+' }} 人次</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="info-card">
              <div class="info-icon">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="info-content">
                <div class="info-label">剧本数量</div>
                <div class="info-value">{{ store?.scriptCount || '200+' }} 个</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="info-card">
              <div class="info-icon">
                <el-icon><House /></el-icon>
              </div>
              <div class="info-content">
                <div class="info-label">房间数量</div>
                <div class="info-value">{{ rooms.length }} 间</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 门店环境（图片展示） -->
      <div class="store-environment" v-if="store?.environmentImages && store.environmentImages.length > 0">
        <h4 class="section-title">
          <el-icon><Picture /></el-icon>
          门店环境
        </h4>
        <el-row :gutter="15" class="environment-gallery">
          <el-col :xs="12" :sm="8" :md="6" v-for="(image, index) in store.environmentImages" :key="index">
            <div class="environment-image" @click="previewImage(image)">
              <img :src="image" :alt="`门店环境 ${index + 1}`" />
              <div class="image-overlay">
                <el-icon><ZoomIn /></el-icon>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
    
    <!-- 房间信息 -->
    <el-card class="detail-card rooms-card">
      <template #header>
        <div class="card-header">
          <span>🚪 房间信息</span>
          <el-tag v-if="availableRoomCount > 0" type="danger" size="small">
            {{ availableRoomCount }} 间可预约
          </el-tag>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="room in rooms" :key="room.id">
          <div class="room-card" :class="{ 'room-unavailable': room.status !== 1 }">
            <!-- 房间类型标签 -->
            <div class="room-type-badge" :class="`room-type-${room.type}`">
              {{ getRoomTypeName(room.type) }}
            </div>
            
            <!-- 房间状态角标 -->
            <div class="room-status-corner" :class="{ 'available': room.status === 1 }">
              <el-icon v-if="room.status === 1"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
            </div>
            
            <div class="room-header">
              <h4>{{ room.name }}</h4>
              <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small">
                {{ room.status === 1 ? '可预约' : '已占用' }}
              </el-tag>
            </div>
            
            <div class="room-info">
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>容纳人数：<strong>{{ room.capacity }}</strong> 人</span>
              </div>
              
              <div class="info-item" v-if="room.type">
                <el-icon><House /></el-icon>
                <span>房间类型：{{ getRoomTypeDesc(room.type) }}</span>
              </div>
              
              <div class="info-item" v-if="room.description">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ room.description }}</span>
              </div>
            </div>
            
            <!-- 预约按钮 -->
            <div class="room-action" v-if="room.status === 1">
              <el-button type="primary" size="small" @click.stop="handleRoomReserve(room)" plain>
                <el-icon><Calendar /></el-icon>
                预约此房间
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      
      <el-empty v-if="rooms.length === 0" description="暂无房间信息">
        <el-button type="primary" @click="handleReserve">立即预约</el-button>
      </el-empty>
    </el-card>
    
    <!-- 近期可约场次 -->
    <el-card class="detail-card schedule-card">
      <template #header>
        <div class="card-header">
          <span>🗓️ 近7天可约场次</span>
          <el-tag v-if="!loadingStoreSchedules" type="success" size="small">
            {{ storeSchedules.length > 0 ? `${storeSchedules.length} 个场次可约` : '暂无排期' }}
          </el-tag>
        </div>
      </template>
      <div v-if="loadingStoreSchedules" style="text-align:center;padding:20px;color:rgba(255,255,255,0.5)">加载中...</div>
      <div v-else-if="storeSchedules.length === 0" style="text-align:center;padding:20px;color:rgba(255,255,255,0.5)">
        🕰️ 暂无可约场次，可致电门店咨询
      </div>
      <div v-else class="store-schedule-grid">
        <div
          v-for="s in storeSchedules"
          :key="s.id"
          class="store-schedule-item"
          @click="router.push({ path: '/reservation/schedule', query: { storeId: store.value.id } })"
        >
          <div class="ss-date">{{ formatScheduleDate(s.scheduleDate) }}</div>
          <div class="ss-time">{{ formatTime(s.startTime) }}</div>
          <div class="ss-script" v-if="s.scriptName">{{ s.scriptName }}</div>
          <div class="ss-remain" :class="getRemainClass(s)">{{ getRemainText(s) }}</div>
        </div>
      </div>
      <div v-if="storeSchedules.length > 0" style="text-align:center;margin-top:16px">
        <el-button type="primary" size="small" @click="handleReserve">立即预约</el-button>
      </div>
    </el-card>

    <!-- 用户评价 -->
    <el-card class="detail-card reviews-card">
      <template #header>
        <div class="card-header">
          <span>💬 玩家评价 ({{ actualReviewCount }})</span>
          <el-button type="danger" size="small" @click="showReviewDialog = true">
            ✍️ 写评价
          </el-button>
        </div>
      </template>
      
      <div class="reviews-list">
        <!-- 显示提示：当前显示最新的评价 -->
        <div v-if="reviews.length > 0 && store && actualReviewCount > reviews.length" class="review-tip">
          <el-alert 
            type="info" 
            :closable="false"
            :show-icon="true"
          >
            <template #default>
              当前显示最新的 {{ reviews.length }} 条评价，共 {{ actualReviewCount }} 条评价
            </template>
          </el-alert>
        </div>
        
        <div class="review-item" v-for="review in reviews" :key="review.id">
          <div class="review-header">
            <el-avatar :src="review.userAvatar" :size="40" />
            <div class="review-user">
              <div class="username">{{ review.username }}</div>
              <el-rate v-model="review.rating" disabled size="small" />
            </div>
            <div class="review-date">{{ review.createTime }}</div>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <div v-if="review.images" class="review-images">
            <el-image
              v-for="(img, idx) in review.images.split(',')"
              :key="idx"
              :src="img"
              :preview-src-list="review.images.split(',')"
              :initial-index="idx"
              fit="cover"
              style="width: 80px; height: 80px; border-radius: 4px; margin-right: 8px; margin-top: 8px;"
            />
          </div>
        </div>
        
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </div>
    </el-card>
    
    <!-- 评价对话框 -->
    <el-dialog v-model="showReviewDialog" title="评价门店" width="500px">
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
        <el-form-item label="打卡晒图">
          <el-upload
            v-model:file-list="reviewForm.imageList"
            action="/api/upload/image"
            list-type="picture-card"
            :limit="6"
            accept="image/*"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多上传6张图片，每张不超过2MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview">提交</el-button>
      </template>
    </el-dialog>

    <!-- 位置权限弹窗 -->
    <LocationPermissionDialog
      v-model="locationDialogVisible"
      @success="handleLocationSuccess"
      @deny="handleLocationDeny"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStoreDetail, getStoreRooms, getStoreReviews, addStoreReview } from '@/api/store'
import { getAvailableSchedules } from '@/api/script'
import { recordBrowseHistory } from '@/api/recommendation'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { getUserLocation, getDistanceText, requestLocationPermission } from '@/utils/location'
import { getStoreCover } from '@/assets/store-covers'
import LocationPermissionDialog from '@/components/LocationPermissionDialog.vue'
import {
  Location,
  LocationInformation,
  Phone,
  Clock,
  Calendar,
  Star,
  TrendCharts,
  Check,
  Medal,
  Document,
  User,
  Reading,
  House,
  Picture,
  ZoomIn,
  SuccessFilled,
  CircleCheck,
  CircleClose,
  InfoFilled,
  Warning,
  Plus
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const store = ref(null)
const rooms = ref([])
const reviews = ref([])
const showReviewDialog = ref(false)
const distance = ref('')
const locationLoading = ref(false)
const locationError = ref('')
const locationDialogVisible = ref(false)
const browseStartTime = ref(null)
const storeSchedules = ref([])   // 门店近期可约场次
const loadingStoreSchedules = ref(false)

const reviewForm = reactive({
  rating: 5,
  content: '',
  images: [],
  imageList: []
})

// 营业状态计算
const businessStatus = computed(() => {
  if (!store.value?.openTime || !store.value?.closeTime) return null
  const now = new Date()
  const [openH, openM] = store.value.openTime.split(':').map(Number)
  const [closeH, closeM] = store.value.closeTime.split(':').map(Number)
  const nowMinutes = now.getHours() * 60 + now.getMinutes()
  const openMinutes = openH * 60 + openM
  const closeMinutes = closeH * 60 + closeM
  const isOpen = nowMinutes >= openMinutes && nowMinutes < closeMinutes
  const minutesToClose = closeMinutes - nowMinutes
  const minutesToOpen = openMinutes - nowMinutes
  if (isOpen) {
    if (minutesToClose <= 60) return { open: true, urgent: true, text: `营业中 · ${minutesToClose}分钟后闭店` }
    return { open: true, urgent: false, text: '营业中' }
  } else {
    if (minutesToOpen > 0 && minutesToOpen <= 60) return { open: false, soon: true, text: `即将开店 · ${minutesToOpen}分钟后` }
    return { open: false, soon: false, text: '已闭店' }
  }
})

// 计算可用房间数量
const availableRoomCount = computed(() => {
  return rooms.value.filter(room => room.status === 1).length
})

// 计算实际评价数量（如果后端返回的 reviewCount 不准确，使用实际加载的评价数量）
const actualReviewCount = computed(() => {
  // 如果后端的 reviewCount 大于 0，使用后端的值
  if (store.value?.reviewCount && store.value.reviewCount > 0) {
    return store.value.reviewCount
  }
  // 否则使用实际加载的评价数量
  return reviews.value.length
})

const storeImages = computed(() => {
  if (store.value?.coverImage) {
    return [store.value.coverImage]
  }
  // 使用智能匹配的门店封面
  if (store.value?.name) {
    return [getStoreCover(store.value.name)]
  }
  return ['https://dummyimage.com/800x400/cccccc/666666&text=Store']
})

// 获取默认的门店描述（通用模板）
const getDefaultDescription = (storeName = '本店') => {
  return `
    <div class="store-intro">
      <h5>🎭 关于我们</h5>
      <p><strong>${storeName}</strong>是一家专业的沉浸式剧本杀体验馆。我们致力于为每一位玩家打造难忘的推理之旅，用心呈现每一个故事，让您在虚拟与现实的交织中，体验不一样的人生。</p>
    </div>
    
    <div class="store-intro">
      <h5>🎭 专业团队</h5>
      <p>我们拥有<strong>10余名全职DM</strong>，平均从业经验<strong>3年以上</strong>，对每个剧本都有深入研究。每位DM都经过严格培训，掌握多种主持风格，无论是本格推理还是情感沉浸，都能为玩家提供极致的游戏体验。</p>
      <ul>
        <li>✨ 专业DM培训体系，持证上岗</li>
        <li>✨ 定期剧本研讨会，精进主持技巧</li>
        <li>✨ 个性化服务，根据玩家喜好推荐剧本</li>
        <li>✨ 全程跟进，及时解答疑问，调节游戏节奏</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>📚 海量剧本库</h5>
      <p>门店现有<strong>200+优质剧本</strong>，涵盖本格推理、情感沉浸、恐怖惊悚、欢乐互动、机制硬核、还原阵营等多种类型，<strong>每月更新10+新本</strong>，紧跟市场潮流。</p>
      <p><strong>热门剧本推荐：</strong></p>
      <ul>
        <li>🔍 <strong>本格推理：</strong>《东方快车谋杀案》《无人生还》《罗生门》等经典本格</li>
        <li>💔 <strong>情感沉浸：</strong>《雾中回忆》《时光代理人》《余生请多指教》</li>
        <li>👻 <strong>恐怖惊悚：</strong>《午夜出租车》《诡宅》《死亡循环》</li>
        <li>😄 <strong>欢乐互动：</strong>《饭局狼人杀》《谁是卧底》《剧本杀派对》</li>
        <li>⚙️ <strong>机制硬核：</strong>《迷雾侦探社》《密室逃脱》《犯罪现场》</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🏠 豪华环境设施</h5>
      <p>门店总面积<strong>800㎡</strong>，共设<strong>6间独立主题房间</strong>，每间房都经过专业设计师精心打造，主题风格各异，氛围感十足。</p>
      <ul>
        <li>🎬 <strong>沉浸式场景：</strong>民国风、古风、现代都市、科幻未来等多种风格</li>
        <li>🎵 <strong>专业设备：</strong>高品质音响系统、智能灯光控制、投影设备</li>
        <li>🎨 <strong>精美道具：</strong>定制化道具，还原剧本场景，增强沉浸感</li>
        <li>🌡️ <strong>舒适体验：</strong>中央空调、新风系统，四季恒温</li>
        <li>📸 <strong>拍照打卡：</strong>多个精美场景，适合拍照留念</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🎯 贴心服务</h5>
      <p>我们深知每一个细节都关乎玩家的体验，因此在服务上精益求精。</p>
      <ul>
        <li>☕ <strong>免费饮品：</strong>咖啡、茶水、果汁、零食无限量供应</li>
        <li>🛋️ <strong>舒适休息区：</strong>宽敞的等候大厅，提供舒适沙发和娱乐设施</li>
        <li>📱 <strong>高速WiFi：</strong>全馆覆盖高速无线网络</li>
        <li>🔌 <strong>充电设施：</strong>每个房间配备充电插座</li>
        <li>🎁 <strong>会员福利：</strong>积分兑换、生日优惠、专属活动</li>
        <li>📦 <strong>物品寄存：</strong>提供免费物品寄存服务</li>
        <li>🚻 <strong>卫生设施：</strong>干净整洁的卫生间，定时消毒</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🚇 交通便利</h5>
      <p><strong>地铁：</strong>地铁10号线三里屯站A出口步行5分钟即达</p>
      <p><strong>公交：</strong>113路、115路、406路、416路等多路公交直达</p>
      <p><strong>自驾：</strong>门店附近有多个停车场，停车便利（可提供停车券）</p>
      <p><strong>周边配套：</strong>三里屯商圈，美食、购物、娱乐应有尽有</p>
    </div>
    
    <div class="store-intro">
      <h5>⭐ 口碑见证</h5>
      <p>自开业以来，我们累计服务<strong>5000+人次</strong>，用户好评率<strong>98%</strong>，是北京地区评分最高的剧本杀门店之一。</p>
      <ul>
        <li>🏆 2022年度"最受欢迎剧本杀门店"</li>
        <li>🏆 2023年度"最佳服务质量奖"</li>
        <li>🏆 大众点评五星商户</li>
        <li>🏆 美团必吃榜推荐商家</li>
      </ul>
      <p><em>"环境超棒，DM专业，剧本丰富，每次来都有新体验！"</em> - 来自会员@推理狂魔</p>
      <p><em>"朋友聚会的首选，氛围好服务好，强烈推荐！"</em> - 来自会员@剧本杀爱好者</p>
    </div>
    
    <div class="store-intro">
      <h5>🎉 特色活动</h5>
      <ul>
        <li>🎭 <strong>主题活动日：</strong>每月举办主题派对，剧本联动，惊喜不断</li>
        <li>🎓 <strong>新手专场：</strong>每周末开设新手专场，DM手把手教学</li>
        <li>💰 <strong>团购优惠：</strong>3人及以上享团购价，6人车本更优惠</li>
        <li>🎂 <strong>生日专属：</strong>生日当月游戏享8折优惠，还有神秘礼物</li>
        <li>🎁 <strong>会员福利：</strong>充值送优惠券，积分兑换剧本和周边</li>
      </ul>
    </div>
    
    <div class="store-intro store-contact">
      <h5>📞 联系我们</h5>
      <p>营业时间：10:00 - 22:00（全年无休）</p>
      <p>预约电话：010-12345678</p>
      <p>客服微信：探案密室官方客服</p>
      <p>官方微信公众号：探案密室</p>
      <p>门店地址：北京市朝阳区三里屯路xx号</p>
    </div>
    
    <div class="store-tips">
      <h5>💡 温馨提示</h5>
      <ul>
        <li>📅 建议提前1-3天预约，周末及节假日请尽早预约</li>
        <li>⏰ 请提前15分钟到店，以便DM讲解规则</li>
        <li>👥 部分剧本有人数要求，拼车可联系客服</li>
        <li>🎒 游戏过程中请遵守规则，爱护道具和设施</li>
        <li>📸 拍照留念请关闭闪光灯，避免影响他人体验</li>
      </ul>
    </div>
  `
}

const loadStoreDetail = async () => {
  loading.value = true
  try {
    const res = await getStoreDetail(route.params.id)
    console.log('门店详情响应:', res)
    if (res.data) {
      store.value = res.data
      store.value.tags = store.value.tags || ['环境优雅', '服务专业', '交通便利']

      // 将后端 images 字段（逗号分隔字符串）转换为前端所需的 coverImage 和 environmentImages
      if (store.value.images) {
        const imageList = store.value.images.split(',').map(img => img.trim()).filter(img => img)
        if (imageList.length > 0) {
          store.value.coverImage = store.value.coverImage || imageList[0]
          store.value.environmentImages = store.value.environmentImages || imageList
        }
      }
      
      // 如果后端返回的description为空，使用通用的默认描述
      if (!store.value.description) {
        store.value.description = getDefaultDescription(store.value.name)
      }
      
      // 记录浏览历史
      if (userStore.isLoggedIn) {
        browseStartTime.value = Date.now()
        try {
          await recordBrowseHistory(2, store.value.id, 0) // targetType: 2=门店
        } catch (e) {
          console.error('保存门店浏览历史失败:', e)
        }
      }
    }
  } catch (error) {
    console.error('加载门店详情失败:', error)
    // 模拟数据
    store.value = {
      id: route.params.id,
      name: '探案密室·沉浸式剧本体验馆',
      address: '北京市朝阳区三里屯路19号',
      phone: '010-12345678',
      openTime: '10:00',
      closeTime: '22:00',
      rating: 4.8,
      reviewCount: 128,
      serviceCount: '5000+',
      scriptCount: '200+',
      tags: ['环境优雅', '服务专业', '交通便利'],
      
      // 门店特色
      highlights: [
        '5年老店·口碑保证',
        '专业DM团队·剧情还原度高',
        '海量剧本·更新快',
        '豪华装修·沉浸式体验',
        '地铁直达·交通便利',
        '免费饮品·舒适环境'
      ],
      
      // 门店设施
      facilities: [
        'WiFi',
        '空调',
        '饮品',
        '零食',
        '停车场',
        '独立包厢',
        '投影设备',
        '音响系统',
        '桌游区',
        '休息区',
        '卫生间',
        '充电设施'
      ],
      
      // 门店描述
      description: `
        <div class="store-intro">
          <h5>🎭 关于我们</h5>
          <p><strong>探案密室</strong>成立于2019年，是北京地区知名的沉浸式剧本杀体验馆。我们致力于为每一位玩家打造难忘的推理之旅，用心呈现每一个故事，让您在虚拟与现实的交织中，体验不一样的人生。</p>
        </div>
        
        <div class="store-intro">
          <h5>🎭 专业团队</h5>
          <p>我们拥有<strong>10余名全职DM</strong>，平均从业经验<strong>3年以上</strong>，对每个剧本都有深入研究。每位DM都经过严格培训，掌握多种主持风格，无论是本格推理还是情感沉浸，都能为玩家提供极致的游戏体验。</p>
          <ul>
            <li>✨ 专业DM培训体系，持证上岗</li>
            <li>✨ 定期剧本研讨会，精进主持技巧</li>
            <li>✨ 个性化服务，根据玩家喜好推荐剧本</li>
            <li>✨ 全程跟进，及时解答疑问，调节游戏节奏</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>📚 海量剧本库</h5>
          <p>门店现有<strong>200+优质剧本</strong>，涵盖本格推理、情感沉浸、恐怖惊悚、欢乐互动、机制硬核、还原阵营等多种类型，<strong>每月更新10+新本</strong>，紧跟市场潮流。</p>
          <p><strong>热门剧本推荐：</strong></p>
          <ul>
            <li>🔍 <strong>本格推理：</strong>《东方快车谋杀案》《无人生还》《罗生门》等经典本格</li>
            <li>💔 <strong>情感沉浸：</strong>《雾中回忆》《时光代理人》《余生请多指教》</li>
            <li>👻 <strong>恐怖惊悚：</strong>《午夜出租车》《诡宅》《死亡循环》</li>
            <li>😄 <strong>欢乐互动：</strong>《饭局狼人杀》《谁是卧底》《剧本杀派对》</li>
            <li>⚙️ <strong>机制硬核：</strong>《迷雾侦探社》《密室逃脱》《犯罪现场》</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🏠 豪华环境设施</h5>
          <p>门店总面积<strong>800㎡</strong>，共设<strong>6间独立主题房间</strong>，每间房都经过专业设计师精心打造，主题风格各异，氛围感十足。</p>
          <ul>
            <li>🎬 <strong>沉浸式场景：</strong>民国风、古风、现代都市、科幻未来等多种风格</li>
            <li>🎵 <strong>专业设备：</strong>高品质音响系统、智能灯光控制、投影设备</li>
            <li>🎨 <strong>精美道具：</strong>定制化道具，还原剧本场景，增强沉浸感</li>
            <li>🌡️ <strong>舒适体验：</strong>中央空调、新风系统，四季恒温</li>
            <li>📸 <strong>拍照打卡：</strong>多个精美场景，适合拍照留念</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🎯 贴心服务</h5>
          <p>我们深知每一个细节都关乎玩家的体验，因此在服务上精益求精。</p>
          <ul>
            <li>☕ <strong>免费饮品：</strong>咖啡、茶水、果汁、零食无限量供应</li>
            <li>🛋️ <strong>舒适休息区：</strong>宽敞的等候大厅，提供舒适沙发和娱乐设施</li>
            <li>📱 <strong>高速WiFi：</strong>全馆覆盖高速无线网络</li>
            <li>🔌 <strong>充电设施：</strong>每个房间配备充电插座</li>
            <li>🎁 <strong>会员福利：</strong>积分兑换、生日优惠、专属活动</li>
            <li>📦 <strong>物品寄存：</strong>提供免费物品寄存服务</li>
            <li>🚻 <strong>卫生设施：</strong>干净整洁的卫生间，定时消毒</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🚇 交通便利</h5>
          <p><strong>地铁：</strong>地铁10号线三里屯站A出口步行5分钟即达</p>
          <p><strong>公交：</strong>113路、115路、406路、416路等多路公交直达</p>
          <p><strong>自驾：</strong>门店附近有多个停车场，停车便利（可提供停车券）</p>
          <p><strong>周边配套：</strong>三里屯商圈，美食、购物、娱乐应有尽有</p>
        </div>
        
        <div class="store-intro">
          <h5>⭐ 口碑见证</h5>
          <p>自开业以来，我们累计服务<strong>5000+人次</strong>，用户好评率<strong>98%</strong>，是北京地区评分最高的剧本杀门店之一。</p>
          <ul>
            <li>🏆 2022年度"最受欢迎剧本杀门店"</li>
            <li>🏆 2023年度"最佳服务质量奖"</li>
            <li>🏆 大众点评五星商户</li>
            <li>🏆 美团必吃榜推荐商家</li>
          </ul>
          <p><em>"环境超棒，DM专业，剧本丰富，每次来都有新体验！"</em> - 来自会员@推理狂魔</p>
          <p><em>"朋友聚会的首选，氛围好服务好，强烈推荐！"</em> - 来自会员@剧本杀爱好者</p>
        </div>
        
        <div class="store-intro">
          <h5>🎉 特色活动</h5>
          <ul>
            <li>🎭 <strong>主题活动日：</strong>每月举办主题派对，剧本联动，惊喜不断</li>
            <li>🎓 <strong>新手专场：</strong>每周末开设新手专场，DM手把手教学</li>
            <li>💰 <strong>团购优惠：</strong>3人及以上享团购价，6人车本更优惠</li>
            <li>🎂 <strong>生日专属：</strong>生日当月游戏享8折优惠，还有神秘礼物</li>
            <li>🎁 <strong>会员福利：</strong>充值送优惠券，积分兑换剧本和周边</li>
          </ul>
        </div>
        
        <div class="store-intro store-contact">
          <h5>📞 联系我们</h5>
          <p>营业时间：10:00 - 22:00（全年无休）</p>
          <p>预约电话：010-12345678</p>
          <p>客服微信：探案密室官方客服</p>
          <p>官方微信公众号：探案密室</p>
          <p>门店地址：北京市朝阳区三里屯路xx号</p>
        </div>
        
        <div class="store-tips">
          <h5>💡 温馨提示</h5>
          <ul>
            <li>📅 建议提前1-3天预约，周末及节假日请尽早预约</li>
            <li>⏰ 请提前15分钟到店，以便DM讲解规则</li>
            <li>👥 部分剧本有人数要求，拼车可联系客服</li>
            <li>🎒 游戏过程中请遵守规则，爱护道具和设施</li>
            <li>📸 拍照留念请关闭闪光灯，避免影响他人体验</li>
          </ul>
        </div>
      `,
      
      // 环境图片
      environmentImages: [
        'https://dummyimage.com/300x200/667eea/ffffff&text=大厅',
        'https://dummyimage.com/300x200/f093fb/ffffff&text=推理房',
        'https://dummyimage.com/300x200/4facfe/ffffff&text=沉浸房',
        'https://dummyimage.com/300x200/43e97b/ffffff&text=休息区',
        'https://dummyimage.com/300x200/fa709a/ffffff&text=恐怖房',
        'https://dummyimage.com/300x200/fee140/333333&text=欢乐房'
      ],
      
      coverImage: '',
      latitude: 39.908815, // 示例坐标：北京天安门
      longitude: 116.397529
    }
  } finally {
    loading.value = false
  }
}

// 显示位置权限弹窗
const showLocationPermissionDialog = async () => {
  // 先检查权限状态
  const permissionState = await requestLocationPermission()
  
  if (permissionState === 'granted') {
    // 已授权，直接获取位置
    getMyLocation()
  } else if (permissionState === 'denied') {
    // 已拒绝，提示用户手动开启
    ElMessage.warning('位置权限已被拒绝，请在浏览器设置中手动开启')
    locationError.value = '请在浏览器设置中开启位置权限'
  } else {
    // 需要询问权限，显示弹窗
    locationDialogVisible.value = true
  }
}

// 位置获取成功回调
const handleLocationSuccess = (location) => {
  locationError.value = ''
  
  if (store.value?.latitude && store.value?.longitude) {
    const targetLocation = {
      latitude: store.value.latitude,
      longitude: store.value.longitude
    }
    
    distance.value = getDistanceText(location, targetLocation)
  }
}

// 位置获取拒绝回调
const handleLocationDeny = () => {
  locationError.value = '位置权限被拒绝'
}

// 获取用户位置并计算距离
const getMyLocation = async () => {
  if (!store.value) return
  
  locationLoading.value = true
  locationError.value = ''
  
  try {
    const userLocation = await getUserLocation(true)
    
    if (store.value.latitude && store.value.longitude) {
      const targetLocation = {
        latitude: store.value.latitude,
        longitude: store.value.longitude
      }
      
      distance.value = getDistanceText(userLocation, targetLocation)
      ElMessage.success('距离计算成功')
    } else {
      ElMessage.warning('门店位置信息不完整')
    }
  } catch (error) {
    console.error('获取位置失败:', error)
    
    if (error.code === 1) {
      locationError.value = '位置权限被拒绝，点击重试'
      ElMessage.warning('请允许浏览器访问您的位置')
    } else if (error.code === 2) {
      locationError.value = '无法获取位置信息'
      ElMessage.error('无法获取您的位置信息')
    } else if (error.code === 3) {
      locationError.value = '获取位置超时'
      ElMessage.error('获取位置超时，请重试')
    } else {
      locationError.value = '浏览器不支持地理定位'
      ElMessage.error('您的浏览器不支持地理定位功能')
    }
  } finally {
    locationLoading.value = false
  }
}

const loadRooms = async () => {
  try {
    const res = await getStoreRooms(route.params.id)
    if (res.data) {
      rooms.value = res.data
    }
  } catch (error) {
    console.error('加载房间信息失败:', error)
    // 模拟数据
    rooms.value = [
      { 
        id: 1, 
        name: '推理房A', 
        type: 1,
        capacity: 4, 
        description: '适合新手玩家，温馨舒适的小型推理房间',
        status: 1 
      },
      { 
        id: 2, 
        name: '推理房B', 
        type: 2,
        capacity: 6, 
        description: '中型房间，配备专业道具和设施',
        status: 1 
      },
      { 
        id: 3, 
        name: '沉浸房C', 
        type: 3,
        capacity: 8, 
        description: '大型沉浸式剧本房间，提供最佳游戏体验',
        status: 0 
      },
      { 
        id: 4, 
        name: '欢乐房D', 
        type: 2,
        capacity: 6, 
        description: '适合欢乐向剧本，氛围轻松',
        status: 1 
      },
      { 
        id: 5, 
        name: '机制房E', 
        type: 2,
        capacity: 7, 
        description: '配备多种机关道具，适合机制本',
        status: 1 
      },
      { 
        id: 6, 
        name: '恐怖房F', 
        type: 1,
        capacity: 5, 
        description: '专为恐怖剧本设计，音效灯光俱全',
        status: 0 
      }
    ]
  }
}

const loadReviews = async () => {
  try {
    const res = await getStoreReviews({
      storeId: route.params.id,
      page: 1,
      pageSize: 10
    })
    if (res.data) {
      const rawList = res.data.records || res.data.list || []
      reviews.value = rawList.map(item => ({
        id: item.id,
        username: item.isAnonymous === 1 ? '匿名用户' : (item.userNickname || item.userName || item.username || '神秘玩家'),
        userAvatar: item.isAnonymous === 1 ? null : (item.userAvatar || item.avatar || null),
        rating: Number(item.rating || item.overallRating || 5),
        content: item.content || '该用户未填写评价内容',
        images: item.images || '',
        createTime: item.createTime || ''
      }))
    }
  } catch (error) {
    console.error('加载评价失败:', error)
  }
}

const handleReserve = () => {
  // 跳转到场次选择页
  router.push({
    path: '/reservation/schedule',
    query: { storeId: store.value.id }
  })
}

const handleCall = () => {
  if (store.value?.phone) {
    window.location.href = `tel:${store.value.phone}`
  } else {
    ElMessage.info('暂无联系电话')
  }
}

const handleUploadSuccess = (response, file) => {
  if (response.data) {
    reviewForm.images.push(response.data)
  }
}

const handleUploadRemove = (file) => {
  const url = file.response?.data || file.url
  reviewForm.images = reviewForm.images.filter(img => img !== url)
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt2M) { ElMessage.error('图片大小不能超过2MB'); return false }
  return true
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
    await addStoreReview({
      storeId: route.params.id,
      rating: reviewForm.rating,
      content: reviewForm.content,
      images: reviewForm.images.join(',')
    })
    ElMessage.success('评价成功')
    showReviewDialog.value = false
    reviewForm.rating = 5
    reviewForm.content = ''
    reviewForm.images = []
    reviewForm.imageList = []
    // 重新加载门店信息和评价列表，更新评价数量
    await Promise.all([
      loadStoreDetail(),
      loadReviews()
    ])
  } catch (error) {
    console.error('提交评价失败:', error)
  }
}

// 获取房间类型名称
const getRoomTypeName = (type) => {
  const typeMap = {
    1: '小房',
    2: '中房',
    3: '大房'
  }
  return typeMap[type] || '普通'
}

// 获取房间类型描述
const getRoomTypeDesc = (type) => {
  const descMap = {
    1: '小型房间（2-4人）',
    2: '中型房间（5-7人）',
    3: '大型房间（8人以上）'
  }
  return descMap[type] || '标准房间'
}

// 预约指定房间 → 跳到场次选择页（带门店ID，用户选完场次再确认）
const handleRoomReserve = (room) => {
  router.push({
    path: '/reservation/schedule',
    query: { storeId: store.value.id }
  })
}

// 获取特色标签类型
const getHighlightType = (index) => {
  const types = ['success', 'primary', 'warning', 'danger', 'info']
  return types[index % types.length]
}

// 获取设施图标
const getFacilityIcon = (facility) => {
  const iconMap = {
    'WiFi': 'Wifi',
    'Wi-Fi': 'Wifi',
    '无线网络': 'Wifi',
    '空调': 'Wind',
    '暖气': 'Sunny',
    '饮品': 'CoffeeCup',
    '零食': 'Food',
    '停车': 'Van',
    '包厢': 'House',
    '投影': 'VideoCamera',
    '音响': 'Headset',
    '桌游': 'Grid',
    '休息区': 'Coffee',
    '卫生间': 'Location',
    '充电': 'Lightning'
  }
  
  // 模糊匹配
  for (const [key, icon] of Object.entries(iconMap)) {
    if (facility.includes(key)) {
      return icon
    }
  }
  
  return 'CircleCheck'
}

// 获取设施颜色
const getFacilityColor = (index) => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#36cfc9']
  return colors[index % colors.length]
}

// 预览图片
const previewImage = (image) => {
  // 可以使用 Element Plus 的图片预览组件
  ElMessage.info('点击查看大图')
  // 这里可以集成图片预览功能
}

// 页面卸载时记录完整浏览时长
onBeforeUnmount(() => {
  if (userStore.isLoggedIn && store.value && browseStartTime.value) {
    const duration = Math.floor((Date.now() - browseStartTime.value) / 1000)
    if (duration > 0) {
      recordBrowseHistory(2, store.value.id, duration).catch(e => {
        console.error('更新门店浏览时长失败:', e)
      })
    }
  }
})

// 加载门店可约场次
const loadStoreSchedules = async () => {
  if (!route.params.id) return
  loadingStoreSchedules.value = true
  try {
    const res = await getAvailableSchedules({ storeId: route.params.id, days: 7 })
    if (res.code === 200 || res.code === 1) {
      storeSchedules.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    console.error('加载门店场次失败:', e)
    storeSchedules.value = []
  } finally {
    loadingStoreSchedules.value = false
  }
}

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

const formatTime = (t) => t ? String(t).substring(0, 5) : ''

const getRemainText = (s) => {
  const r = s.maxPlayers - s.currentPlayers
  if (r <= 0) return '已满'
  if (r === 1) return '差1人成团'
  return `余 ${r} 位`
}

const getRemainClass = (s) => {
  const r = s.maxPlayers - s.currentPlayers
  if (r <= 0) return 'remain-full'
  if (r <= 2) return 'remain-few'
  return 'remain-ok'
}

onMounted(() => {
  loadStoreDetail()
  loadRooms()
  loadReviews()
  loadStoreSchedules()
})
</script>

<style scoped>
.store-detail-container {
  padding: 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  min-height: 100vh;
}

/* 主卡片区域 */
.store-main-card {
  max-width: 1200px;
  margin: 0 auto 20px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

/* 门店头部信息 */
.store-header {
  margin-bottom: 30px;
}

.store-images {
  border-radius: 12px;
  overflow: hidden;
}

.store-images img {
  width: 100%;
  height: 400px;
  object-fit: cover;
}

/* 基本信息 */
.store-basic-info h1 {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px 0;
}

.store-rating {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.store-rating .rating-value {
  font-size: 24px;
  font-weight: 700;
  color: #faad14;
}

/* Element Plus Rate 评分组件深色主题 */
.store-rating :deep(.el-rate__text) {
  color: #faad14 !important;
  font-weight: 600;
}

/* 卡片头部 el-tag 深色主题适配（暂无排期、X间可预约、X分） */
.card-header :deep(.el-tag) {
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.15) !important;
  font-weight: 600;
}

.card-header :deep(.el-tag--success) {
  background: rgba(103, 194, 58, 0.25) !important;
  border-color: rgba(103, 194, 58, 0.5) !important;
  color: #95e77d !important;
}

.card-header :deep(.el-tag--warning) {
  background: rgba(230, 162, 60, 0.25) !important;
  border-color: rgba(230, 162, 60, 0.5) !important;
  color: #f5d18a !important;
}

.card-header :deep(.el-tag--danger) {
  background: rgba(245, 108, 108, 0.25) !important;
  border-color: rgba(245, 108, 108, 0.5) !important;
  color: #f98989 !important;
}

.card-header :deep(.el-tag--info) {
  background: rgba(144, 147, 153, 0.25) !important;
  border-color: rgba(144, 147, 153, 0.5) !important;
  color: #c0c4cc !important;
}

/* el-descriptions 深色主题适配 */
.store-desc :deep(.el-descriptions__body) {
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__cell) {
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__label) {
  color: rgba(255, 255, 255, 0.6) !important;
  background: transparent !important;
  font-size: 14px;
}

.store-desc :deep(.el-descriptions__content) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  font-size: 14px;
}

/* 评价对话框深色适配 */
.store-desc :deep(.el-descriptions__table) {
  background: transparent !important;
}

/* 地址行内距离标签 */
.address-with-distance {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
}

.review-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 门店信息条目 */
.store-meta-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.8);
}

.store-meta-item .el-icon {
  color: #ff6b6b;
  font-size: 18px;
}

/* 门店标签 */
.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 20px 0;
}

.store-tags .el-tag {
  padding: 6px 16px;
  border-radius: 15px;
  font-size: 13px;
}

/* 门店特色 */
.store-features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 20px 0;
  padding: 16px;
  background: rgba(35, 35, 60, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.feature-item {
  padding: 6px 14px;
  background: rgba(139, 0, 0, 0.2);
  border-radius: 15px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(139, 0, 0, 0.3);
}

/* 操作按钮区域 */
.action-buttons {
  display: flex;
  gap: 14px;
  margin-top: 28px;
  flex-wrap: wrap;
}

/* 立即预约按钮 */
.action-buttons :deep(.btn-reserve) {
  flex: 1;
  min-width: 140px;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  border: none !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #c0392b 0%, #8B0000 60%, #6b0000 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 18px rgba(139, 0, 0, 0.5);
  transition: all 0.3s ease;
}

.action-buttons :deep(.btn-reserve:hover) {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 60%, #8B0000 100%) !important;
  box-shadow: 0 6px 24px rgba(192, 57, 43, 0.6);
  transform: translateY(-2px);
}

.action-buttons :deep(.btn-reserve:active) {
  transform: translateY(0);
  box-shadow: 0 2px 10px rgba(139, 0, 0, 0.4);
}

/* 电话咨询按钮 */
.action-buttons :deep(.btn-call) {
  flex: 1;
  min-width: 140px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1.5px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.action-buttons :deep(.btn-call:hover) {
  background: rgba(255, 255, 255, 0.16) !important;
  border-color: rgba(255, 255, 255, 0.55) !important;
  color: #fff !important;
  box-shadow: 0 4px 16px rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.action-buttons :deep(.btn-call:active) {
  transform: translateY(0);
}

/* 兼容旧的 store-actions */
.store-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.store-actions .el-button--danger {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  border: none;
  padding: 12px 28px;
  border-radius: 10px;
}

.store-actions .el-button--danger:hover {
  background: linear-gradient(135deg, #a01010 0%, #b81818 100%);
}

/* 卡片通用样式 */
.detail-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

/* 房间卡片样式 */
.rooms-card .room-item {
  padding: 20px;
  border: 1px solid rgba(139, 0, 0, 0.2);
  border-radius: 12px;
  margin-bottom: 15px;
  transition: all 0.3s;
  background: rgba(35, 35, 60, 0.8);
}

.rooms-card .room-item:hover {
  border-color: #8B0000;
  box-shadow: 0 4px 15px rgba(139,0,0,0.3);
}

.room-name {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 10px;
}

.room-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.room-price {
  font-size: 22px;
  font-weight: 700;
  color: #ff6b6b;
}

.room-price small {
  font-size: 13px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

/* 评价卡片样式 */
.reviews-card .review-item {
  padding: 20px;
  border-bottom: 1px solid rgba(139, 0, 0, 0.2);
}

.reviews-card .review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.review-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
}

.review-user-info {
  flex: 1;
}

.review-username {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.review-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.review-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
  margin-bottom: 12px;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

/* 门店场次余量 */
.store-schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.store-schedule-item {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 12px 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.store-schedule-item:hover {
  background: rgba(139,0,0,0.2);
  border-color: rgba(139,0,0,0.5);
  transform: translateY(-2px);
}

.ss-date {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 4px;
}

.ss-time {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.ss-script {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ss-remain {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  display: inline-block;
}

.remain-ok { background: rgba(103,194,58,0.2); color: #67c23a; }
.remain-few { background: rgba(230,162,60,0.2); color: #e6a23c; }
.remain-full { background: rgba(245,108,108,0.2); color: #f56c6c; }

.upload-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 8px;
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-helpful {
  font-size: 13px;
  color: #ff6b6b;
  cursor: pointer;
}

/* 空状态 */
.empty-reviews {
  text-align: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.6);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.store-basic-info h1 {
  font-size: 32px;
  margin: 0 0 20px;
  color: #fff;
}

.store-rating {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
}

.review-count {
  color: #999;
  font-size: 14px;
}

.store-desc {
  margin-bottom: 20px;
}

/* Element Plus Descriptions 组件深色主题 */
.store-desc :deep(.el-descriptions__body) {
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__table) {
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__cell) {
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__label) {
  color: rgba(255, 255, 255, 0.7) !important;
  background: transparent !important;
}

.store-desc :deep(.el-descriptions__content) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
}

.store-tags {
  margin-bottom: 25px;
}

.store-tags .el-tag {
  margin-right: 10px;
  margin-bottom: 10px;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

.action-buttons .el-button {
  flex: 1;
}

.detail-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 门店介绍卡片 */
.store-intro-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
}

/* 章节标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid rgba(139, 0, 0, 0.3);
}

.section-title .el-icon {
  font-size: 20px;
  color: #ff6b6b;
}

/* 门店特色标签 */
.store-highlights {
  margin-bottom: 30px;
}

.highlight-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.highlight-tag {
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 20px;
  cursor: default;
  transition: all 0.3s;
}

.highlight-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.highlight-tag .el-icon {
  margin-right: 4px;
}

/* 门店设施 */
.store-facilities {
  margin-bottom: 30px;
}

.facility-list {
  margin: 0;
}

.facility-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: rgba(35, 35, 60, 0.8);
  border-radius: 8px;
  margin-bottom: 10px;
  transition: all 0.3s;
  cursor: default;
  border: 1px solid rgba(139, 0, 0, 0.15);
}

.facility-item:hover {
  background: rgba(45, 45, 75, 0.9);
  transform: translateX(5px);
  border-color: rgba(139, 0, 0, 0.3);
}

.facility-item .el-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.facility-item span {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 门店简介 */
.store-description-section {
  margin-bottom: 30px;
}

.store-description {
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
}

.store-description :deep(.store-intro) {
  background: rgba(35, 35, 60, 0.8);
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  border-left: 4px solid #ff6b6b;
  transition: all 0.3s;
  border: 1px solid rgba(139, 0, 0, 0.15);
  border-left-width: 4px;
}

.store-description :deep(.store-intro:hover) {
  background: rgba(45, 45, 75, 0.9);
  box-shadow: 0 2px 12px rgba(139, 0, 0, 0.2);
  transform: translateX(5px);
}

.store-description :deep(.store-intro h5) {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.store-description :deep(.store-intro p) {
  margin: 0 0 10px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.store-description :deep(.store-intro p:last-child) {
  margin-bottom: 0;
}

.store-description :deep(.store-intro ul) {
  margin: 10px 0;
  padding-left: 0;
  list-style: none;
}

.store-description :deep(.store-intro ul li) {
  margin: 8px 0;
  padding-left: 24px;
  position: relative;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.8);
}

.store-description :deep(.store-intro ul li::before) {
  content: '•';
  position: absolute;
  left: 8px;
  color: #ff6b6b;
  font-size: 18px;
  line-height: 1.8;
}

.store-description :deep(.store-intro.store-contact) {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.4) 0%, rgba(139, 0, 0, 0.2) 100%);
  color: #fff;
  border-left-color: #ff6b6b;
}

.store-description :deep(.store-intro.store-contact h5) {
  color: #fff;
}

.store-description :deep(.store-intro.store-contact p) {
  color: rgba(255, 255, 255, 0.95);
  font-size: 15px;
}

.store-description :deep(.store-tips) {
  background: rgba(230, 162, 60, 0.15);
  border-left-color: #e6a23c;
  padding: 20px;
  border-radius: 12px;
  margin-top: 20px;
  border: 1px solid rgba(230, 162, 60, 0.3);
  border-left-width: 4px;
}

.store-description :deep(.store-tips h5) {
  color: #e6a23c;
}

.store-description :deep(.store-tips ul li::before) {
  color: #e6a23c;
}

.store-description :deep(.store-tips ul li) {
  color: rgba(255, 255, 255, 0.8);
}

.store-description p {
  margin: 0 0 15px;
}

.store-description strong {
  color: #fff;
  font-weight: 600;
}

.store-description :deep(em) {
  color: rgba(255, 255, 255, 0.7);
  font-style: italic;
  display: block;
  padding: 10px 15px;
  background: rgba(35, 35, 60, 0.6);
  border-radius: 8px;
  margin: 5px 0;
}

/* 营业信息 */
.store-business-info {
  margin-bottom: 30px;
}

.business-info-grid {
  margin: 0;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
  margin-bottom: 15px;
  transition: all 0.3s;
}

.info-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(139, 0, 0, 0.2);
  border-color: #8B0000;
}

.info-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.5) 0%, rgba(139, 0, 0, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
}

.info-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 5px;
}

.info-value {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

/* 门店环境图片 */
.store-environment {
  margin-bottom: 0;
}

.environment-gallery {
  margin: 0;
}

.environment-image {
  position: relative;
  height: 150px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 15px;
  transition: all 0.3s;
}

.environment-image:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.environment-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.environment-image:hover img {
  transform: scale(1.1);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.environment-image:hover .image-overlay {
  opacity: 1;
}

.image-overlay .el-icon {
  font-size: 32px;
  color: #fff;
}

/* 房间卡片样式 */
.room-card {
  position: relative;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 15px;
  border: 2px solid rgba(139, 0, 0, 0.25);
  transition: all 0.3s ease;
  overflow: hidden;
}

.room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.room-card.room-unavailable {
  opacity: 0.7;
  background: linear-gradient(135deg, #f5f5f5 0%, #e9e9e9 100%);
}

.room-card.room-unavailable:hover {
  transform: none;
  box-shadow: none;
}

/* 房间类型标签 */
.room-type-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: bold;
  color: #fff;
  border-radius: 0 0 12px 0;
  z-index: 2;
}

.room-type-badge.room-type-1 {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.room-type-badge.room-type-2 {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.room-type-badge.room-type-3 {
  background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%);
}

/* 房间状态角标 */
.room-status-corner {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #f5f5f5;
  color: #999;
  z-index: 2;
}

.room-status-corner.available {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 30px 0 15px;
}

.room-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.room-info {
  margin-bottom: 15px;
}

.room-info .info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  line-height: 1.6;
}

.room-info .info-item strong {
  color: #ff6b6b;
  font-size: 16px;
}

.room-info .info-item .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

/* 房间预约按钮 */
.room-action {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #e8e8e8;
}

.room-action .el-button {
  width: 100%;
}

.reviews-list {
  max-height: 600px;
  overflow-y: auto;
}

.review-tip {
  margin-bottom: 20px;
}

.review-tip :deep(.el-alert) {
  border-radius: 8px;
}

.review-item {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.review-user {
  flex: 1;
}

.username {
  font-weight: bold;
  margin-bottom: 5px;
}

.review-date {
  color: #999;
  font-size: 12px;
}

.review-content {
  color: #666;
  line-height: 1.6;
  padding-left: 55px;
}

/* 距离显示样式 */
.address-with-distance {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-with-distance > span {
  line-height: 1.6;
}

.distance-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: default;
  animation: fadeIn 0.3s ease-in;
}

.distance-tag .el-icon {
  font-size: 14px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式布局 */
@media (max-width: 768px) {
  .store-basic-info h1 {
    font-size: 24px;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
  
  .address-with-distance {
    font-size: 14px;
  }
}
</style>
