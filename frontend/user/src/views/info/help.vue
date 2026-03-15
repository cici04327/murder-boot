<template>
  <div class="info-page help-page">
    <!-- 面包屑导航 -->
    <InfoPageBreadcrumb />
    
    <!-- 头部横幅 - 剧本杀主题 -->
    <div class="hero-banner murder-theme">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-icon">❓</div>
        <h1 class="hero-title animate-fade-in-up">
          帮助中心
        </h1>
        <p class="hero-subtitle animate-fade-in-up delay-1">
          有问题？我们来帮您解答 · 7x15小时在线服务
        </p>
        <div class="hero-stats animate-fade-in-up delay-2">
          <div class="stat-item">
            <div class="stat-icon">📚</div>
            <div class="stat-number">50+</div>
            <div class="stat-label">常见问题</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">📁</div>
            <div class="stat-number">6</div>
            <div class="stat-label">分类模块</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">⏱️</div>
            <div class="stat-number">7x15</div>
            <div class="stat-label">小时在线</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">✅</div>
            <div class="stat-number">98%</div>
            <div class="stat-label">解决率</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 快捷服务入口 -->
    <el-card class="info-card service-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>🚀 快捷服务</h2>
          <span class="card-subtitle">一键直达常用功能</span>
        </div>
      </template>
      <div class="service-grid">
        <div class="service-item" v-for="service in quickServices" :key="service.title" @click="handleServiceClick(service)">
          <div class="service-icon">{{ service.icon }}</div>
          <h4>{{ service.title }}</h4>
          <p>{{ service.desc }}</p>
        </div>
      </div>
    </el-card>

    <!-- 常见问题 -->
    <el-card class="info-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>💡 常见问题</h2>
          <div class="search-box-small">
            <el-input
              v-model="searchQuery"
              placeholder="搜索问题..."
              prefix-icon="Search"
              clearable
              @input="handleSearch"
            />
          </div>
        </div>
      </template>
      
      <div class="info-content">
        <!-- 新手入门 -->
        <section class="info-section scroll-reveal">
          <h3>🎯 新手入门</h3>
          <div class="faq-list">
            <div class="faq-item" v-for="(item, index) in filteredFaq.newbie" :key="index">
              <div class="faq-question" @click="toggleFaq('newbie', index)">
                <span>{{ item.question }}</span>
                <el-icon :class="{ 'is-active': item.expanded }"><ArrowDown /></el-icon>
              </div>
              <div class="faq-answer" v-show="item.expanded">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 预约相关 -->
        <section class="info-section scroll-reveal">
          <h3>📅 预约相关</h3>
          <div class="faq-list">
            <div class="faq-item" v-for="(item, index) in filteredFaq.reservation" :key="index">
              <div class="faq-question" @click="toggleFaq('reservation', index)">
                <span>{{ item.question }}</span>
                <el-icon :class="{ 'is-active': item.expanded }"><ArrowDown /></el-icon>
              </div>
              <div class="faq-answer" v-show="item.expanded">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 支付退款 -->
        <section class="info-section scroll-reveal">
          <h3>💰 支付退款</h3>
          <div class="faq-list">
            <div class="faq-item" v-for="(item, index) in filteredFaq.payment" :key="index">
              <div class="faq-question" @click="toggleFaq('payment', index)">
                <span>{{ item.question }}</span>
                <el-icon :class="{ 'is-active': item.expanded }"><ArrowDown /></el-icon>
              </div>
              <div class="faq-answer" v-show="item.expanded">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 优惠券积分 -->
        <section class="info-section scroll-reveal">
          <h3>🎁 优惠券与积分</h3>
          <div class="faq-list">
            <div class="faq-item" v-for="(item, index) in filteredFaq.coupon" :key="index">
              <div class="faq-question" @click="toggleFaq('coupon', index)">
                <span>{{ item.question }}</span>
                <el-icon :class="{ 'is-active': item.expanded }"><ArrowDown /></el-icon>
              </div>
              <div class="faq-answer" v-show="item.expanded">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 账号安全 -->
        <section class="info-section scroll-reveal">
          <h3>🔐 账号安全</h3>
          <div class="faq-list">
            <div class="faq-item" v-for="(item, index) in filteredFaq.account" :key="index">
              <div class="faq-question" @click="toggleFaq('account', index)">
                <span>{{ item.question }}</span>
                <el-icon :class="{ 'is-active': item.expanded }"><ArrowDown /></el-icon>
              </div>
              <div class="faq-answer" v-show="item.expanded">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 联系方式 -->
        <section class="info-section scroll-reveal">
          <h3>📞 联系我们</h3>
          <div class="contact-grid">
            <div class="contact-card">
              <el-icon :size="28"><Phone /></el-icon>
              <div>
                <h4>客服热线</h4>
                <p>400-123-4567</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><Message /></el-icon>
              <div>
                <h4>电子邮箱</h4>
                <p>help@jubensha.com</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><Clock /></el-icon>
              <div>
                <h4>服务时间</h4>
                <p>周一至周日 9:00-24:00</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><ChatDotRound /></el-icon>
              <div>
                <h4>在线客服</h4>
                <p>点击右下角AI助手</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </el-card>
    
    <!-- 分享按钮 -->
    <ShareButtons />
    
    <!-- 返回顶部 -->
    <BackToTop />
    
    <!-- 主题切换器 -->
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Phone,
  Message,
  Clock,
  ChatDotRound,
  ArrowDown,
  Search
} from '@element-plus/icons-vue'
import InfoPageBreadcrumb from '@/components/InfoPageBreadcrumb.vue'
import ShareButtons from '@/components/ShareButtons.vue'
import BackToTop from '@/components/BackToTop.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()

// 初始化滚动显示动画
useScrollReveal()

// 搜索关键词
const searchQuery = ref('')

// 快捷服务
const quickServices = ref([
  {
    icon: '📅',
    title: '预约问题',
    desc: '预约流程、取消改期',
    action: 'reservation'
  },
  {
    icon: '💳',
    title: '支付退款',
    desc: '支付方式、退款规则',
    action: 'payment'
  },
  {
    icon: '🎫',
    title: '优惠券',
    desc: '领取使用、有效期',
    action: 'coupon'
  },
  {
    icon: '👤',
    title: '账号问题',
    desc: '注册登录、密码找回',
    action: 'account'
  },
  {
    icon: '📱',
    title: '联系客服',
    desc: '在线咨询、问题反馈',
    action: 'contact'
  },
  {
    icon: '📖',
    title: '新手指南',
    desc: '快速上手、玩法介绍',
    action: 'newbie'
  }
])

// FAQ 数据
const faqData = ref({
  newbie: [
    { question: '什么是剧本杀？', answer: '剧本杀是一种多人参与的角色扮演推理游戏。每位玩家扮演剧本中的一个角色，通过阅读剧本、搜集线索、推理讨论来完成游戏目标，通常是找出凶手或完成特定任务。', expanded: false },
    { question: '第一次玩剧本杀需要准备什么？', answer: '第一次玩不需要特别准备，只需要按时到店即可。建议穿着舒适的衣服，保持开放的心态，准备好投入角色。店家会提供所需的所有道具和剧本材料。', expanded: false },
    { question: '剧本杀一般需要多长时间？', answer: '根据剧本类型不同，时间从2小时到6小时不等。普通本一般3-4小时，情感本、硬核本可能需要5-6小时。预约时可以查看预估时长。', expanded: false },
    { question: '新手适合玩什么类型的本？', answer: '建议新手从"新手本"或"欢乐本"开始，这类剧本规则简单、节奏轻松，容易上手。等熟悉后再尝试推理本、情感本等。', expanded: false }
  ],
  reservation: [
    { question: '如何预约剧本杀？', answer: '在首页或门店页面选择想玩的剧本和门店，选择日期时间和人数，确认信息后提交预约即可。系统会发送预约确认短信。', expanded: false },
    { question: '预约后可以取消吗？', answer: '可以取消。在游戏开始前24小时取消可全额退款；24小时内取消需扣除一定手续费。具体规则以门店政策为准。', expanded: false },
    { question: '可以修改预约时间吗？', answer: '可以在"我的预约"中修改时间。建议提前24小时修改，以便门店安排。如遇特殊情况请及时联系客服。', expanded: false },
    { question: '人数不够可以拼车吗？', answer: '可以选择"拼车模式"预约，系统会帮您匹配其他玩家。您也可以创建拼车房间邀请好友。', expanded: false }
  ],
  payment: [
    { question: '支持哪些支付方式？', answer: '支持微信支付、支付宝、银行卡等多种支付方式。部分门店还支持到店付款。', expanded: false },
    { question: '什么情况可以申请退款？', answer: '以下情况可申请退款：提前24小时取消预约、门店原因导致无法游戏、游戏体验严重不符等。具体退款金额视情况而定。', expanded: false },
    { question: '退款多久到账？', answer: '退款一般在1-3个工作日内原路返回。如超过3天未收到，请联系客服查询。', expanded: false },
    { question: '支付失败怎么办？', answer: '请检查网络连接和支付账户余额。如多次失败，可尝试更换支付方式或联系客服。', expanded: false }
  ],
  coupon: [
    { question: '如何获得优惠券？', answer: '可通过以下方式获得：新用户注册礼包、参与平台活动、积分兑换、邀请好友、特定节日发放等。', expanded: false },
    { question: '优惠券如何使用？', answer: '在支付页面选择可用的优惠券即可抵扣。注意查看优惠券的使用门槛和有效期。', expanded: false },
    { question: '积分有什么用？', answer: '积分可用于兑换优惠券、抵扣现金、兑换周边礼品等。完成预约、发表评价、签到等都可获得积分。', expanded: false },
    { question: '优惠券可以叠加使用吗？', answer: '一般情况下每笔订单只能使用一张优惠券，但积分抵扣可以和优惠券同时使用。具体规则请查看优惠券说明。', expanded: false }
  ],
  account: [
    { question: '如何注册账号？', answer: '点击"注册"按钮，输入手机号获取验证码，设置密码后即可完成注册。也支持微信一键登录。', expanded: false },
    { question: '忘记密码怎么办？', answer: '在登录页面点击"忘记密码"，通过手机号验证后可重置密码。', expanded: false },
    { question: '如何修改个人信息？', answer: '进入"我的"-"设置"-"个人资料"，可修改头像、昵称、性别等信息。', expanded: false },
    { question: '账号可以注销吗？', answer: '可以在"设置"-"账号安全"-"注销账号"中申请注销。注销后账号数据将被清除且无法恢复，请谨慎操作。', expanded: false }
  ]
})

// 过滤后的FAQ（支持搜索）
const filteredFaq = computed(() => {
  const query = searchQuery.value.toLowerCase()
  if (!query) return faqData.value
  
  const result = {}
  for (const [category, items] of Object.entries(faqData.value)) {
    result[category] = items.filter(item => 
      item.question.toLowerCase().includes(query) || 
      item.answer.toLowerCase().includes(query)
    )
  }
  return result
})

// 切换FAQ展开状态
const toggleFaq = (category, index) => {
  faqData.value[category][index].expanded = !faqData.value[category][index].expanded
}

// 处理搜索
const handleSearch = () => {
  // 搜索时展开所有匹配项
  if (searchQuery.value) {
    for (const items of Object.values(faqData.value)) {
      for (const item of items) {
        if (item.question.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
            item.answer.toLowerCase().includes(searchQuery.value.toLowerCase())) {
          item.expanded = true
        }
      }
    }
  }
}

// 处理快捷服务点击
const handleServiceClick = (service) => {
  if (service.action === 'contact') {
    router.push('/contact')
  } else {
    // 滚动到对应分类
    const element = document.querySelector(`.info-section:has(h3:contains('${service.title}'))`)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' })
    }
  }
}
</script>

<style scoped>
.info-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 头部横幅 - 剧本杀主题 */
.hero-banner.murder-theme {
  position: relative;
  min-height: 420px;
  background: #1a1a2e url('https://images.unsplash.com/photo-1557804506-669a67965ba0?w=1200&h=400&fit=crop') center/cover;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 30px 0;
  padding: 60px 20px;
  border-radius: 16px;
  overflow: hidden;
}

.hero-overlay {
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
  text-align: center;
  color: #fff;
}

.hero-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 15px;
  color: #fff;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
}

.hero-subtitle {
  font-size: 18px;
  margin-bottom: 40px;
  color: rgba(255,255,255,0.9);
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
  padding: 20px 30px;
  background: rgba(255,255,255,0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.2);
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-5px);
  background: rgba(255,255,255,0.15);
}

.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #ffd700;
}

.stat-label {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin-top: 5px;
}

/* 卡片样式 */
.info-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h2 {
  margin: 0;
  font-size: 22px;
  color: #fff;
}

.card-subtitle {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.search-box-small {
  width: 250px;
}

/* 快捷服务网格 */
.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 20px;
}

.service-item {
  padding: 25px 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid rgba(139, 0, 0, 0.2);
  cursor: pointer;
}

.service-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.25);
  border-color: #8B0000;
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
}

.service-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.service-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.service-item p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

/* 内容区域 */
.info-content {
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.info-section {
  margin-bottom: 50px;
  padding: 25px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 12px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.info-section:last-child {
  margin-bottom: 0;
}

.info-section h3 {
  font-size: 20px;
  color: #fff;
  margin-bottom: 25px;
  padding-bottom: 12px;
  border-bottom: 2px solid #8B0000;
  display: inline-block;
}

/* FAQ 样式 */
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-item {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(139, 0, 0, 0.2);
  transition: all 0.3s;
}

.faq-item:hover {
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.2);
  border-color: rgba(139, 0, 0, 0.4);
}

.faq-question {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
  font-weight: 500;
  color: #fff;
  transition: all 0.3s;
}

.faq-question:hover {
  color: #ff6b6b;
}

.faq-question .el-icon {
  transition: transform 0.3s;
  color: rgba(255, 255, 255, 0.6);
}

.faq-question .el-icon.is-active {
  transform: rotate(180deg);
  color: #ff6b6b;
}

.faq-answer {
  padding: 0 20px 18px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  line-height: 1.8;
  border-top: 1px solid rgba(139, 0, 0, 0.2);
  background: rgba(26, 26, 46, 0.5);
}

.faq-answer p {
  margin: 15px 0 0;
}

/* 联系方式网格 */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.contact-card {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 10px;
  transition: all 0.3s;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.contact-card:hover {
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.2);
  border-color: rgba(139, 0, 0, 0.4);
}

.contact-card .el-icon {
  color: #ff6b6b;
  flex-shrink: 0;
  margin-top: 2px;
}

.contact-card h4 {
  margin: 0 0 5px 0;
  font-size: 15px;
  color: #fff;
}

.contact-card p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  animation: fadeInUp 0.8s ease-out;
}

.delay-1 {
  animation-delay: 0.2s;
  opacity: 0;
  animation-fill-mode: forwards;
}

.delay-2 {
  animation-delay: 0.4s;
  opacity: 0;
  animation-fill-mode: forwards;
}

.animate-fade-in {
  animation: fadeInUp 0.6s ease-out;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 28px;
  }
  
  .hero-stats {
    gap: 15px;
  }
  
  .stat-item {
    padding: 15px 20px;
  }
  
  .stat-number {
    font-size: 24px;
  }
  
  .service-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .card-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .search-box-small {
    width: 100%;
  }
}
</style>

<!-- 非 scoped 样式，用于 scroll-reveal 动画和覆盖 Element Plus -->
<style>
.help-page .scroll-reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.help-page .scroll-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

/* 覆盖 Element Plus 卡片背景 */
.help-page .info-card.el-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  --el-card-bg-color: transparent !important;
}

.help-page .info-card.el-card .el-card__header {
  background: transparent !important;
  border-bottom-color: rgba(139, 0, 0, 0.2) !important;
}

.help-page .info-card.el-card .el-card__body {
  background: transparent !important;
}

/* 覆盖搜索框样式 */
.help-page .search-box-small .el-input__wrapper {
  background: rgba(35, 35, 60, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.3) !important;
}

.help-page .search-box-small .el-input__inner {
  color: #fff !important;
}

.help-page .search-box-small .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.5) !important;
}

.help-page .search-box-small .el-input__prefix,
.help-page .search-box-small .el-input__suffix {
  color: rgba(255, 255, 255, 0.6) !important;
}

.help-page .search-box-small .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px rgba(139, 0, 0, 0.5) !important;
}

.help-page .search-box-small .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #8B0000 !important;
}
</style>
