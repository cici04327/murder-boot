<template>
  <div class="info-page contact-page">
    <!-- 面包屑导航 -->
    <InfoPageBreadcrumb />
    
    <!-- 头部横幅 - 剧本杀主题 -->
    <div class="hero-banner murder-theme">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-icon">📞</div>
        <h1 class="hero-title animate-fade-in-up">
          随时为您服务
        </h1>
        <p class="hero-subtitle animate-fade-in-up delay-1">
          专业客服团队 · 7×12小时在线 · 快速响应您的每一个需求
        </p>
      </div>
    </div>
    
    <!-- 快捷联系方式 -->
    <div class="quick-contact-grid animate-fade-in">
      <div class="quick-contact-item" v-for="item in quickContacts" :key="item.title">
        <div class="contact-icon-wrapper" :style="{ background: item.bgColor }">
          <span class="contact-emoji">{{ item.icon }}</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p class="contact-value">{{ item.value }}</p>
        <p class="contact-desc">{{ item.desc }}</p>
      </div>
    </div>
    
    <el-row :gutter="20">
      <!-- 左侧：联系方式与常见问题 -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>🎯 业务合作</h2>
            </div>
          </template>
          
          <div class="cooperation-section">
            <div class="cooperation-item" v-for="coop in cooperations" :key="coop.title">
              <div class="coop-icon">{{ coop.icon }}</div>
              <div class="coop-info">
                <h4>{{ coop.title }}</h4>
                <p class="coop-desc">{{ coop.desc }}</p>
                <div class="coop-contact">
                  <span><el-icon><Message /></el-icon> {{ coop.email }}</span>
                  <span><el-icon><Phone /></el-icon> {{ coop.phone }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="info-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>❓ 常见问题</h2>
            </div>
          </template>
          
          <el-collapse accordion>
            <el-collapse-item v-for="(faq, index) in faqs" :key="index" :name="index">
              <template #title>
                <span class="faq-title">{{ faq.q }}</span>
              </template>
              <div class="faq-answer">{{ faq.a }}</div>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <el-card class="info-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>📍 门店分布</h2>
            </div>
          </template>
          
          <div class="store-regions">
            <div class="region-item" v-for="region in storeRegions" :key="region.name">
              <div class="region-header">
                <span class="region-name">{{ region.name }}</span>
                <el-tag size="small">{{ region.count }}家门店</el-tag>
              </div>
              <p class="region-cities">{{ region.cities.join('、') }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：留言表单与关注我们 -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>✉️ 在线留言</h2>
              <span class="card-subtitle">有任何问题，请留言给我们</span>
            </div>
          </template>
          
          <el-form
            ref="feedbackFormRef"
            :model="feedbackForm"
            :rules="feedbackRules"
            label-width="100px"
            class="feedback-form"
          >
            <el-form-item label="您的昵称" prop="name">
              <el-input v-model="feedbackForm.name" placeholder="请输入您的昵称或游戏ID" prefix-icon="User" />
            </el-form-item>

            <el-form-item label="联系方式" prop="phone">
              <el-input v-model="feedbackForm.phone" placeholder="手机号/微信号，方便我们联系您" prefix-icon="Phone" />
            </el-form-item>

            <el-form-item label="留言类型" prop="type">
              <el-select v-model="feedbackForm.type" placeholder="请选择留言类型" style="width: 100%">
                <el-option label="🎮 游戏咨询" value="game" />
                <el-option label="🏠 门店合作" value="store" />
                <el-option label="📝 剧本投稿" value="script" />
                <el-option label="💡 功能建议" value="suggestion" />
                <el-option label="🐛 问题反馈" value="bug" />
                <el-option label="📦 其他" value="other" />
              </el-select>
            </el-form-item>

            <el-form-item label="留言内容" prop="content">
              <el-input
                v-model="feedbackForm.content"
                type="textarea"
                :rows="5"
                placeholder="请详细描述您的问题或建议，我们会尽快回复~"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button type="danger" @click="submitFeedback" :loading="submitting" style="width: 120px;">
                提交留言
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="info-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>🎭 关注我们</h2>
              <span class="card-subtitle">获取最新剧本资讯与福利</span>
            </div>
          </template>
          
          <div class="social-section">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="qrcode-card">
                  <div class="qrcode-placeholder wechat">
                    <span>📱</span>
                  </div>
                  <h4>微信公众号</h4>
                  <p>关注获取新本推荐</p>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="qrcode-card">
                  <div class="qrcode-placeholder service">
                    <span>💬</span>
                  </div>
                  <h4>客服微信</h4>
                  <p>jubensha_service</p>
                </div>
              </el-col>
            </el-row>
            
            <div class="social-links">
              <a href="#" class="social-link weibo">
                <span>🔴</span> 官方微博
              </a>
              <a href="#" class="social-link douyin">
                <span>🎵</span> 抖音号
              </a>
              <a href="#" class="social-link bilibili">
                <span>📺</span> B站账号
              </a>
              <a href="#" class="social-link xiaohongshu">
                <span>📕</span> 小红书
              </a>
            </div>
          </div>
        </el-card>

        <el-card class="info-card tips-card animate-fade-in">
          <template #header>
            <div class="card-header">
              <h2>💡 新手小贴士</h2>
            </div>
          </template>
          
          <div class="tips-list">
            <div class="tip-item" v-for="(tip, index) in newbieTips" :key="index">
              <span class="tip-icon">{{ tip.icon }}</span>
              <span class="tip-text">{{ tip.text }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 分享按钮 -->
    <ShareButtons />
    
    <!-- 返回顶部 -->
    <BackToTop />
    
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Phone,
  Message
} from '@element-plus/icons-vue'
import InfoPageBreadcrumb from '@/components/InfoPageBreadcrumb.vue'
import ShareButtons from '@/components/ShareButtons.vue'
import BackToTop from '@/components/BackToTop.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { submitFeedback as submitFeedbackApi } from '@/api/feedback'

// 初始化
useScrollReveal()

// 快捷联系方式
const quickContacts = ref([
  {
    icon: '📞',
    title: '客服热线',
    value: '400-123-4567',
    desc: '周一至周日 9:00-21:00',
    bgColor: '#fff3e0'
  },
  {
    icon: '💬',
    title: '在线客服',
    value: '即时响应',
    desc: '点击右下角图标开始聊天',
    bgColor: '#e3f2fd'
  },
  {
    icon: '📧',
    title: '电子邮箱',
    value: 'contact@jubensha.com',
    desc: '24小时内回复',
    bgColor: '#f3e5f5'
  },
  {
    icon: '📍',
    title: '公司地址',
    value: '北京市朝阳区',
    desc: 'xx路xx号xx大厦xx层',
    bgColor: '#e8f5e9'
  }
])

// 业务合作
const cooperations = ref([
  {
    icon: '🏠',
    title: '门店入驻',
    desc: '诚邀优质剧本杀门店加入我们的平台，共享流量资源',
    email: 'store@jubensha.com',
    phone: '010-12345678'
  },
  {
    icon: '📖',
    title: '剧本合作',
    desc: '欢迎优秀剧本作者投稿，我们提供专业推广渠道',
    email: 'script@jubensha.com',
    phone: '010-12345679'
  },
  {
    icon: '🎯',
    title: '品牌推广',
    desc: '多种广告位和推广方案，精准触达目标用户群体',
    email: 'ads@jubensha.com',
    phone: '010-12345680'
  }
])

// 常见问题
const faqs = ref([
  {
    q: '如何预约剧本杀？',
    a: '在首页选择心仪的剧本或门店，选择时间和人数后即可在线预约。支持拼车组队，也可以包场。预约成功后会收到短信确认。'
  },
  {
    q: '预约后可以取消吗？',
    a: '开场前24小时可免费取消，24小时内取消会扣除部分费用。如遇特殊情况请联系客服，我们会尽量协调处理。'
  },
  {
    q: '第一次玩需要准备什么？',
    a: '不需要特别准备！到店后DM（主持人）会详细讲解规则。建议穿着舒适，保持手机静音，全身心投入角色即可。'
  },
  {
    q: '什么是城限/独家剧本？',
    a: '城限剧本指在一个城市只有少数门店有授权的稀缺剧本，独家则是该城市只有一家门店拥有。这类剧本通常品质更高。'
  },
  {
    q: '如何成为VIP会员？',
    a: '在个人中心开通VIP会员，可享受预约折扣、专属客服、优先选本等特权。首次开通还有新人礼包赠送！'
  },
  {
    q: '积分有什么用？',
    a: '积分可以兑换优惠券、周边礼品等。完成游戏、发表评价、签到、邀请好友等都可以获得积分奖励。'
  }
])

// 门店分布
const storeRegions = ref([
  { name: '华北地区', count: 35, cities: ['北京', '天津', '石家庄', '太原'] },
  { name: '华东地区', count: 42, cities: ['上海', '杭州', '南京', '苏州', '宁波'] },
  { name: '华南地区', count: 28, cities: ['广州', '深圳', '东莞', '佛山'] },
  { name: '华中地区', count: 18, cities: ['武汉', '长沙', '郑州'] },
  { name: '西南地区', count: 15, cities: ['成都', '重庆', '昆明'] }
])

// 新手贴士
const newbieTips = ref([
  { icon: '🎭', text: '新手建议从「新手友好」标签的剧本开始体验' },
  { icon: '👥', text: '组队时注意剧本人数要求，一般4-8人不等' },
  { icon: '⏰', text: '剧本时长2-6小时，请预留充足时间' },
  { icon: '🤫', text: '玩过的剧本请勿剧透，保护其他玩家体验' },
  { icon: '📝', text: '游戏后记得评价，帮助更多玩家选本' }
])

// 反馈表单
const feedbackFormRef = ref(null)
const submitting = ref(false)
const feedbackForm = reactive({
  name: '',
  phone: '',
  type: '',
  content: ''
})

const feedbackRules = {
  name: [
    { required: true, message: '请输入您的昵称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系方式', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择留言类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入留言内容', trigger: 'blur' },
    { min: 10, message: '留言内容至少10个字符', trigger: 'blur' }
  ]
}

// 提交反馈
const submitFeedback = async () => {
  if (!feedbackFormRef.value) return
  
  await feedbackFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await submitFeedbackApi({
          name: feedbackForm.name,
          contact: feedbackForm.phone,
          subject: feedbackForm.type,
          message: feedbackForm.content
        })
        
        if (res.code === 1 || res.code === 200) {
          ElMessage.success('感谢您的留言！我们会尽快与您联系~')
          resetForm()
        } else {
          ElMessage.error(res.msg || '提交失败，请稍后重试')
        }
      } catch (error) {
        console.error('提交留言失败:', error)
        ElMessage.error('提交失败，请稍后重试')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (!feedbackFormRef.value) return
  feedbackFormRef.value.resetFields()
}
</script>

<style scoped>
.info-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 头部横幅 */
.hero-banner.murder-theme {
  position: relative;
  min-height: 300px;
  background: #1a1a2e url('https://images.unsplash.com/photo-1516321497487-e288fb19713f?w=1200&h=400&fit=crop') center/cover;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 30px 0;
  padding: 50px 20px;
  border-radius: 16px;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 26, 46, 0.8);
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
}

.hero-icon {
  font-size: 56px;
  margin-bottom: 15px;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #fff;
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255,255,255,0.85);
}

/* 快捷联系网格 */
.quick-contact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.quick-contact-item {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.quick-contact-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.25);
  border-color: rgba(192, 57, 43, 0.4);
}

.contact-icon-wrapper {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  background: rgba(192, 57, 43, 0.2) !important;
}

.contact-emoji {
  font-size: 32px;
}

.quick-contact-item h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.contact-value {
  margin: 0 0 5px 0;
  font-size: 18px;
  font-weight: 600;
  color: #ff6b6b;
}

.contact-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 卡片样式 */
.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
  border: 1px solid rgba(192, 57, 43, 0.2);
  --el-card-bg-color: transparent !important;
}

.info-card.el-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
}

/* 覆盖 Element Plus 卡片内部样式 */
:deep(.info-card.el-card) {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  --el-card-bg-color: transparent !important;
}

.info-card :deep(.el-card__header) {
  background: transparent !important;
  border-bottom: 1px solid rgba(192, 57, 43, 0.2) !important;
}

.info-card :deep(.el-card__body) {
  background: transparent !important;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: #fff;
}

.card-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 业务合作 */
.cooperation-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cooperation-item {
  display: flex;
  gap: 15px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 10px;
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.cooperation-item:hover {
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.2);
  border-color: rgba(192, 57, 43, 0.4);
}

.coop-icon {
  font-size: 36px;
  flex-shrink: 0;
}

.coop-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.coop-desc {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.coop-contact {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.coop-contact span {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* FAQ */
.faq-title {
  font-weight: 500;
  color: #fff;
}

.faq-answer {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
  padding: 10px 0;
}

/* 常见问题折叠面板深度样式 */
:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 1px solid rgba(192, 57, 43, 0.2);
  border-radius: 8px;
  margin-bottom: 10px;
  padding: 0 15px;
  color: #fff;
}

:deep(.el-collapse-item__header:hover) {
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
}

:deep(.el-collapse-item__header.is-active) {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  border-bottom: none;
}

:deep(.el-collapse-item__wrap) {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border: 1px solid rgba(192, 57, 43, 0.2);
  border-top: none;
  border-radius: 0 0 8px 8px;
  margin-bottom: 10px;
}

:deep(.el-collapse-item__content) {
  padding: 15px;
  color: rgba(255, 255, 255, 0.8);
}

:deep(.el-collapse-item__arrow) {
  color: rgba(255, 255, 255, 0.7);
}

/* 门店分布 */
.store-regions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.region-item {
  padding: 15px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 8px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.region-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.region-name {
  font-weight: 600;
  color: #fff;
}

.region-cities {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 表单 */
.feedback-form {
  padding: 10px 0;
  background: transparent !important;
}

.feedback-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.85) !important;
}

.feedback-form :deep(.el-input__wrapper) {
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(192, 57, 43, 0.3) !important;
}

.feedback-form :deep(.el-input__inner) {
  color: #fff !important;
  --el-input-text-color: #fff;
}

.feedback-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.feedback-form :deep(.el-input .el-input__icon) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.feedback-form :deep(.el-textarea__inner) {
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(192, 57, 43, 0.3) !important;
  color: #fff !important;
}

.feedback-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.feedback-form :deep(.el-textarea .el-input__count) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.5) !important;
}

.feedback-form :deep(.el-select .el-input__wrapper) {
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(192, 57, 43, 0.3) !important;
}

.feedback-form :deep(.el-select .el-input__inner) {
  color: #fff !important;
}

.feedback-form :deep(.el-select .el-input__suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.feedback-form :deep(.el-input__wrapper:hover),
.feedback-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px rgba(192, 57, 43, 0.5) !important;
}

.feedback-form :deep(.el-input__wrapper.is-focus),
.feedback-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #16213e !important;
}

.feedback-form :deep(.el-button--default) {
  background: rgba(35, 35, 60, 0.9) !important;
  border-color: rgba(192, 57, 43, 0.3) !important;
  color: rgba(255, 255, 255, 0.8) !important;
}

.feedback-form :deep(.el-button--default:hover) {
  background: rgba(45, 45, 75, 0.95) !important;
  border-color: rgba(192, 57, 43, 0.5) !important;
  color: #fff !important;
}

/* 社交媒体 */
.social-section {
  padding: 10px 0;
}

.qrcode-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 10px;
  margin-bottom: 20px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.qrcode-placeholder {
  width: 120px;
  height: 120px;
  margin: 0 auto 15px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

.qrcode-placeholder.wechat {
  background: rgba(76, 175, 80, 0.2);
}

.qrcode-placeholder.service {
  background: rgba(33, 150, 243, 0.2);
}

.qrcode-card h4 {
  margin: 0 0 5px 0;
  font-size: 15px;
  color: #fff;
}

.qrcode-card p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.social-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.social-link:hover {
  background: #16213e;
  color: #fff;
  border-color: #16213e;
}

/* 新手贴士 */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 15px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 8px;
  border-left: 3px solid #f59e0b;
  border-top: 1px solid rgba(192, 57, 43, 0.15);
  border-right: 1px solid rgba(192, 57, 43, 0.15);
  border-bottom: 1px solid rgba(192, 57, 43, 0.15);
}

.tip-icon {
  font-size: 20px;
}

.tip-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

/* 动画 */
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

.animate-fade-in-up {
  animation: fadeInUp 0.6s ease-out;
}

.delay-1 {
  animation-delay: 0.15s;
  opacity: 0;
  animation-fill-mode: forwards;
}

.animate-fade-in {
  animation: fadeInUp 0.5s ease-out;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 26px;
  }
  
  .quick-contact-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .quick-contact-item {
    padding: 18px 12px;
  }
  
  .contact-value {
    font-size: 14px;
  }
  
  .cooperation-item {
    flex-direction: column;
    text-align: center;
  }
  
  .coop-contact {
    flex-direction: column;
    gap: 8px;
    align-items: center;
  }
}
</style>

<!-- 非 scoped 样式，强制覆盖 Element Plus 卡片背景 -->
<style>
.contact-page .info-card.el-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  --el-card-bg-color: transparent !important;
}

.contact-page .info-card.el-card .el-card__header {
  background: transparent !important;
  border-bottom-color: rgba(192, 57, 43, 0.2) !important;
}

.contact-page .info-card.el-card .el-card__body {
  background: transparent !important;
}

/* 强制覆盖在线留言卡片内所有子元素背景 */
.contact-page .info-card.el-card .el-card__body,
.contact-page .info-card.el-card .el-card__body > *,
.contact-page .info-card .feedback-form,
.contact-page .info-card .el-form {
  background: transparent !important;
  background-color: transparent !important;
}

/* 修复 el-select 下拉面板蓝紫色 */
.contact-page .el-select-dropdown {
  background: rgba(26, 26, 46, 0.98) !important;
  border: 1px solid rgba(192, 57, 43, 0.3) !important;
}

.contact-page .el-select-dropdown .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.85) !important;
}

.contact-page .el-select-dropdown .el-select-dropdown__item.hover,
.contact-page .el-select-dropdown .el-select-dropdown__item:hover {
  background: rgba(192, 57, 43, 0.2) !important;
}
</style>
