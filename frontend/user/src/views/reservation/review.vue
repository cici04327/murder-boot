<template>
  <div class="review-page">
    <el-card class="review-card">
      <template #header>
        <div class="card-header">
          <span>订单评价</span>
        </div>
      </template>

      <!-- 订单信息 -->
      <div class="order-info" v-if="reservation">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预约编号">{{ reservation.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ formatTime(reservation.reservationTime) }}</el-descriptions-item>
          <el-descriptions-item label="剧本名称">{{ reservation.scriptName }}</el-descriptions-item>
          <el-descriptions-item label="门店名称">{{ reservation.storeName }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 评价Tab分区 -->
      <el-tabs v-model="activeReviewTab" class="review-tabs">
        <!-- 剧本评价 Tab -->
        <el-tab-pane label="剧本评价" name="script">
          <div class="tab-content">
            <div class="review-section-title">
              <el-icon><Film /></el-icon>
              <span>对剧本「{{ reservation?.scriptName }}」的评价</span>
            </div>
            
            <el-form
              ref="scriptFormRef"
              :model="scriptReviewForm"
              :rules="scriptRules"
              label-width="100px"
              class="review-form"
            >
              <!-- 剧本评分 -->
              <el-form-item label="剧本评分" prop="rating">
                <el-rate v-model="scriptReviewForm.rating" :max="5" show-text />
              </el-form-item>

              <!-- 剧情评分 -->
              <el-form-item label="剧情评分" prop="plotRating">
                <el-rate v-model="scriptReviewForm.plotRating" :max="5" />
                <span class="rating-desc">故事情节、逻辑性</span>
              </el-form-item>

              <!-- 难度评分 -->
              <el-form-item label="难度评分" prop="difficultyRating">
                <el-rate v-model="scriptReviewForm.difficultyRating" :max="5" />
                <span class="rating-desc">推理难度、谜题设计</span>
              </el-form-item>

              <!-- 沉浸感评分 -->
              <el-form-item label="沉浸感" prop="immersionRating">
                <el-rate v-model="scriptReviewForm.immersionRating" :max="5" />
                <span class="rating-desc">代入感、氛围感</span>
              </el-form-item>

              <!-- 评价内容 -->
              <el-form-item label="评价内容" prop="content">
                <el-input
                  v-model="scriptReviewForm.content"
                  type="textarea"
                  :rows="4"
                  placeholder="分享你对这个剧本的体验感受~"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <!-- 剧本评价标签 -->
              <el-form-item label="评价标签">
                <el-checkbox-group v-model="scriptReviewForm.tags">
                  <el-checkbox-button v-for="tag in scriptTags" :key="tag" :label="tag">
                    {{ tag }}
                  </el-checkbox-button>
                </el-checkbox-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- DM 评价 Tab -->
        <el-tab-pane label="DM 评价" name="dm">
          <div class="tab-content">
            <div class="review-section-title">
              <el-icon><Avatar /></el-icon>
              <span>对主持人（DM）的评价</span>
            </div>
            <div v-if="!dmInfo" class="dm-no-info">
              <el-empty description="本次预约暂无 DM 信息，可跳过此项" :image-size="60" />
            </div>
            <template v-else>
              <!-- DM 信息展示 -->
              <div class="dm-profile">
                <el-avatar :size="56" :src="dmInfo.avatar">🎭</el-avatar>
                <div class="dm-meta">
                  <div class="dm-name">{{ dmInfo.name }}</div>
                  <div class="dm-tags">
                    <el-tag v-for="tag in (dmInfo.styleTagList || [])" :key="tag" size="small" style="margin:2px">{{ tag }}</el-tag>
                  </div>
                  <el-rate :model-value="Number(dmInfo.rating)" disabled show-score size="small" />
                </div>
              </div>
            </template>
            <el-form label-width="100px" class="review-form" style="margin-top:20px">
              <el-form-item label="DM 评分">
                <el-rate v-model="dmReviewForm.rating" :max="5" show-text
                  :texts="['很差', '较差', '一般', '不错', '超棒']" />
              </el-form-item>
              <el-form-item label="评价内容">
                <el-input
                  v-model="dmReviewForm.content"
                  type="textarea"
                  :rows="3"
                  placeholder="分享你对本次 DM 主持体验的感受～"
                  maxlength="300"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="评价标签">
                <el-checkbox-group v-model="dmReviewForm.tags">
                  <el-checkbox-button v-for="tag in dmTags" :key="tag" :label="tag">{{ tag }}</el-checkbox-button>
                </el-checkbox-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 门店评价 Tab -->
        <el-tab-pane label="门店评价" name="store">
          <div class="tab-content">
            <div class="review-section-title">
              <el-icon><Shop /></el-icon>
              <span>对门店「{{ reservation?.storeName }}」的评价</span>
            </div>
            
            <el-form
              ref="storeFormRef"
              :model="storeReviewForm"
              :rules="storeRules"
              label-width="100px"
              class="review-form"
            >
              <!-- 门店评分 -->
              <el-form-item label="门店评分" prop="rating">
                <el-rate v-model="storeReviewForm.rating" :max="5" show-text />
              </el-form-item>

              <!-- 环境评分 -->
              <el-form-item label="环境评分" prop="environmentRating">
                <el-rate v-model="storeReviewForm.environmentRating" :max="5" />
                <span class="rating-desc">装修、卫生、氛围</span>
              </el-form-item>

              <!-- 服务评分 -->
              <el-form-item label="服务评分" prop="serviceRating">
                <el-rate v-model="storeReviewForm.serviceRating" :max="5" />
                <span class="rating-desc">DM服务、响应速度</span>
              </el-form-item>

              <!-- 位置评分 -->
              <el-form-item label="位置评分" prop="locationRating">
                <el-rate v-model="storeReviewForm.locationRating" :max="5" />
                <span class="rating-desc">交通便利、停车方便</span>
              </el-form-item>

              <!-- 评价内容 -->
              <el-form-item label="评价内容" prop="content">
                <el-input
                  v-model="storeReviewForm.content"
                  type="textarea"
                  :rows="4"
                  placeholder="分享你对这家门店的体验感受~"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <!-- 门店评价标签 -->
              <el-form-item label="评价标签">
                <el-checkbox-group v-model="storeReviewForm.tags">
                  <el-checkbox-button v-for="tag in storeTags" :key="tag" :label="tag">
                    {{ tag }}
                  </el-checkbox-button>
                </el-checkbox-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 上传图片（通用） -->
      <div class="upload-section">
        <h4>上传图片（可选）</h4>
        <el-upload
          v-model:file-list="imageList"
          action="#"
          list-type="picture-card"
          :auto-upload="false"
          :limit="5"
          accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">最多上传5张图片，上传图片可获得额外积分</div>
      </div>

      <!-- 是否匿名 -->
      <div class="anonymous-section">
        <el-switch v-model="form.isAnonymous" :active-value="1" :inactive-value="0" />
        <span class="rating-desc">匿名评价（不显示用户名）</span>
      </div>

      <!-- 积分提示 -->
      <el-alert
        title="积分奖励说明"
        type="info"
        :closable="false"
        class="points-tip"
      >
        <div>剧本评价积分：30分 | 门店评价积分：30分</div>
        <div>上传图片：+10分 | 详细评价（超过50字）：+10分</div>
        <div>完成两项评价最高可获得：80积分</div>
      </el-alert>

      <!-- 提交按钮 -->
      <div class="submit-section">
        <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
          提交评价
        </el-button>
        <el-button @click="handleCancel" size="large">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Film, Shop, Avatar } from '@element-plus/icons-vue'
import { getReservationDetail } from '@/api/reservation'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const scriptFormRef = ref(null)
const storeFormRef = ref(null)
const submitting = ref(false)
const reservation = ref(null)
const imageList = ref([])
const activeReviewTab = ref('script')

// 剧本评价标签
const scriptTags = [
  '剧情精彩',
  '烧脑推理',
  '沉浸感强',
  '适合新手',
  '适合进阶',
  '氛围绝佳',
  '反转惊人',
  '值得二刷'
]

// 门店评价标签
const storeTags = [
  '环境优雅',
  '服务周到',
  '性价比高',
  'DM专业',
  '交通便利',
  '设施完善',
  '卫生干净',
  '氛围好'
]

// 剧本评价表单
const scriptReviewForm = reactive({
  rating: 5,
  plotRating: 5,
  difficultyRating: 5,
  immersionRating: 5,
  content: '',
  tags: []
})

// 门店评价表单
const storeReviewForm = reactive({
  rating: 5,
  environmentRating: 5,
  serviceRating: 5,
  locationRating: 5,
  content: '',
  tags: []
})

// DM 信息
const dmInfo = ref(null)

// DM 评价标签
const dmTags = ['专业投入', '引导到位', '氛围烘托好', '反应灵活', '值得再约', '幽默风趣', '沉浸感强']

// DM 评价表单
const dmReviewForm = reactive({
  rating: 5,
  content: '',
  tags: []
})

// 通用表单（匿名等）
const form = reactive({
  reservationId: null,
  storeId: null,
  scriptId: null,
  dmId: null,
  isAnonymous: 0
})

// 剧本评价规则
const scriptRules = {
  rating: [
    { required: true, message: '请选择剧本评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    { min: 10, message: '评价内容至少10个字', trigger: 'blur' }
  ]
}

// 门店评价规则
const storeRules = {
  rating: [
    { required: true, message: '请选择门店评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    { min: 10, message: '评价内容至少10个字', trigger: 'blur' }
  ]
}

// 加载预约详情
const loadReservation = async () => {
  try {
    const res = await getReservationDetail(route.params.id)
    if (res.code === 1 || res.code === 200) {
      reservation.value = res.data
      form.reservationId = res.data.id
      form.storeId = res.data.storeId
      form.scriptId = res.data.scriptId

      // 尝试加载排期关联的 DM 信息
      if (res.data.scheduleId) {
        try {
          const scheduleRes = await request({ url: `/api/script/schedule/${res.data.scheduleId}`, method: 'get' })
          const schedule = scheduleRes.data
          if (schedule?.dmId) {
            form.dmId = schedule.dmId
            const dmRes = await request({ url: `/api/dm/${schedule.dmId}`, method: 'get' })
            dmInfo.value = dmRes.data
          }
        } catch (e) {
          console.warn('加载 DM 信息失败', e)
        }
      }
      
      // 检查订单状态
      if (res.data.status !== 3) {
        ElMessage.warning('只有已完成的订单才能评价')
        router.back()
      }
    }
  } catch (error) {
    console.error('加载预约详情失败:', error)
    ElMessage.error('加载订单信息失败')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 提交评价
const handleSubmit = async () => {
  // 验证两个表单
  let scriptValid = true
  let storeValid = true
  
  if (scriptFormRef.value) {
    await scriptFormRef.value.validate((valid) => {
      scriptValid = valid
    }).catch(() => { scriptValid = false })
  }
  
  if (storeFormRef.value) {
    await storeFormRef.value.validate((valid) => {
      storeValid = valid
    }).catch(() => { storeValid = false })
  }
  
  // 至少需要填写一项评价
  if (!scriptReviewForm.content && !storeReviewForm.content) {
    ElMessage.warning('请至少填写一项评价内容')
    return
  }
  
  submitting.value = true
  try {
    // 处理图片
    const images = imageList.value
      .filter(item => item.url || item.raw)
      .map(item => item.url || URL.createObjectURL(item.raw))
      .join(',')
    
    // 构建提交数据
    const reviewData = {
      reservationId: form.reservationId,
      storeId: form.storeId,
      scriptId: form.scriptId,
      dmId: form.dmId || null,
      dmRating: dmReviewForm.rating || null,
      isAnonymous: form.isAnonymous,
      images,
      // 剧本评价
      scriptRating: scriptReviewForm.rating,
      scriptPlotRating: scriptReviewForm.plotRating,
      scriptDifficultyRating: scriptReviewForm.difficultyRating,
      scriptImmersionRating: scriptReviewForm.immersionRating,
      scriptContent: scriptReviewForm.content,
      scriptTags: scriptReviewForm.tags.join(','),
      // 门店评价
      storeRating: storeReviewForm.rating,
      storeEnvironmentRating: storeReviewForm.environmentRating,
      storeServiceRating: storeReviewForm.serviceRating,
      storeLocationRating: storeReviewForm.locationRating,
      storeContent: storeReviewForm.content,
      storeTags: storeReviewForm.tags.join(',')
    }
    
    const res = await request.post('/reservation/review', reviewData)
    
    if (res.code === 1 || res.code === 200) {
      ElMessage.success({
        message: `评价成功！获得 ${calculatePoints()} 积分奖励`,
        duration: 3000
      })
      setTimeout(() => {
        router.push('/user/reservations')
      }, 1500)
    }
  } catch (error) {
    console.error('提交评价失败:', error)
    ElMessage.error(error.response?.data?.msg || '提交评价失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 计算积分
const calculatePoints = () => {
  let points = 50 // 基础积分
  
  if (imageList.value.length > 0) {
    points += 10 // 上传图片
  }
  
  if (form.content && form.content.length > 50) {
    points += 10 // 详细评价
  }
  
  if (form.storeRating && form.scriptRating && form.serviceRating) {
    points += 5 // 完成所有评分
  }
  
  return points
}

// 取消
const handleCancel = () => {
  router.back()
}

onMounted(() => {
  if (route.params.id) {
    loadReservation()
  } else {
    ElMessage.error('缺少订单ID')
    router.back()
  }
})
</script>

<style scoped>
.review-page {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.review-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.order-info {
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

/* Tab样式 */
.review-tabs {
  margin-top: 20px;
}

.review-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.review-tabs :deep(.el-tabs__item.is-active) {
  color: #16213e;
}

.review-tabs :deep(.el-tabs__active-bar) {
  background-color: #16213e;
}

.tab-content {
  padding: 20px 0;
}

/* 评价区域标题 */
.review-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 8px;
  border-left: 4px solid #16213e;
}

.review-section-title .el-icon {
  color: #16213e;
  font-size: 20px;
}

/* DM 信息卡片 */
.dm-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 10px;
  margin-bottom: 16px;
}

.dm-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}

.dm-tags {
  margin-bottom: 6px;
}

.dm-no-info {
  padding: 20px 0;
}

.review-form {
  margin-top: 20px;
}

.rating-desc {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}

/* 上传图片区域 */
.upload-section {
  margin-top: 30px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.upload-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

.upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

/* 匿名评价区域 */
.anonymous-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
}

/* 提交按钮区域 */
.submit-section {
  margin-top: 30px;
  text-align: center;
}

.submit-section .el-button {
  min-width: 120px;
  margin: 0 10px;
}

.submit-section .el-button--primary {
  background-color: #16213e;
  border-color: #16213e;
}

.submit-section .el-button--primary:hover {
  background-color: #a00000;
  border-color: #a00000;
}

.points-tip {
  margin: 20px 0;
}

.points-tip div {
  line-height: 24px;
}

:deep(.el-rate) {
  height: 20px;
}

:deep(.el-checkbox-button) {
  margin-right: 10px;
  margin-bottom: 10px;
}

:deep(.el-checkbox-button__inner) {
  border-radius: 16px;
  padding: 6px 12px;
}

:deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  background-color: #16213e;
  border-color: #16213e;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 100px;
  height: 100px;
}

:deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
  line-height: 100px;
}

/* 响应式 */
@media (max-width: 768px) {
  .review-page {
    padding: 0 10px;
  }
  
  .order-info :deep(.el-descriptions) {
    --el-descriptions-item-bordered-label-background: #fafafa;
  }
  
  .review-section-title {
    font-size: 14px;
  }
}
</style>

