<template>
  <div class="info-page agreement-page">
    <!-- 面包屑导航 -->
    <InfoPageBreadcrumb />
    
    <!-- 头部横幅 - 剧本杀主题 -->
    <div class="hero-banner murder-theme">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-icon">📜</div>
        <h1 class="hero-title animate-fade-in-up">
          用户服务协议
        </h1>
        <p class="hero-subtitle animate-fade-in-up delay-1">
          请仔细阅读以下条款，使用我们的服务即表示您同意本协议
        </p>
        <div class="hero-stats animate-fade-in-up delay-2">
          <div class="stat-item">
            <div class="stat-icon">📅</div>
            <div class="stat-number">2024.11</div>
            <div class="stat-label">更新日期</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">📄</div>
            <div class="stat-number">V2.0</div>
            <div class="stat-label">版本号</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">📖</div>
            <div class="stat-number">12</div>
            <div class="stat-label">主要条款</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">⏱️</div>
            <div class="stat-number">10</div>
            <div class="stat-label">分钟阅读</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 协议概览 -->
    <el-card class="info-card overview-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>📋 协议概览</h2>
          <span class="card-subtitle">核心条款快速了解</span>
        </div>
      </template>
      <div class="overview-grid">
        <div class="overview-item" v-for="item in overviewItems" :key="item.title">
          <div class="overview-icon">{{ item.icon }}</div>
          <h4>{{ item.title }}</h4>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </el-card>

    <!-- 协议正文 -->
    <el-card class="info-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>📝 协议详情</h2>
        </div>
      </template>
      
      <div class="info-content">
        <!-- 重要提示 -->
        <el-alert
          title="重要提示"
          type="warning"
          :closable="false"
          class="notice-alert scroll-reveal"
        >
          <p>在使用本平台服务前，请您务必仔细阅读并充分理解本协议的全部内容，特别是<strong>加粗部分</strong>。如您不同意本协议的任何内容，请勿注册或使用本平台服务。</p>
        </el-alert>

        <!-- 协议范围 -->
        <section class="info-section scroll-reveal">
          <h3>📌 一、协议的范围</h3>
          <div class="section-content">
            <p>1.1 本协议是您（以下称"用户"）与剧本杀在线预约平台（以下称"本平台"）之间关于用户使用本平台服务所订立的协议。</p>
            <p>1.2 本协议内容包括协议正文及本平台已经发布或将来可能发布的各类规则。所有规则为本协议不可分割的一部分，与协议正文具有同等法律效力。</p>
          </div>
        </section>

        <!-- 账号注册 -->
        <section class="info-section scroll-reveal">
          <h3>👤 二、账号注册与使用</h3>
          <div class="section-content">
            <p>2.1 用户在使用本平台服务前需要注册一个账号。账号应当使用手机号码绑定注册。</p>
            <p>2.2 用户应当提供真实、准确、完整的注册信息，并及时更新。如因提供虚假信息造成的一切后果，由用户自行承担。</p>
            <p class="highlight">2.3 <strong>用户应当妥善保管账号及密码，不得将账号转让、出售或出借给他人使用。因用户保管不善导致账号被盗用的，用户应自行承担责任。</strong></p>
            <p>2.4 用户不得注册多个账号进行恶意操作或牟取不正当利益。</p>
          </div>
        </section>

        <!-- 服务内容 -->
        <section class="info-section scroll-reveal">
          <h3>🎯 三、服务内容</h3>
          <div class="section-content">
            <p>3.1 本平台提供剧本杀相关的在线预约、信息展示、评价分享等服务。</p>
            <p>3.2 本平台有权根据实际情况随时调整所提供的服务内容，并通过网站公告等方式通知用户。</p>
            <p class="highlight">3.3 <strong>本平台仅作为预约平台，实际服务由各合作门店提供。因门店服务质量产生的纠纷，用户应与门店协商解决。</strong></p>
          </div>
        </section>

        <!-- 用户行为规范 -->
        <section class="info-section scroll-reveal">
          <h3>⚠️ 四、用户行为规范</h3>
          <div class="section-content">
            <p>4.1 用户在使用本平台服务时，应当遵守国家法律法规，不得利用本平台从事违法违规活动。</p>
            <p>4.2 用户不得发布任何违法、虚假、侵权、骚扰、诽谤他人的信息。</p>
            <p class="highlight">4.3 <strong>用户不得恶意刷单、刷评价、恶意差评或进行其他不正当竞争行为。</strong></p>
            <p>4.4 用户不得利用技术手段干扰本平台的正常运营。</p>
          </div>
        </section>

        <!-- 预约与支付 -->
        <section class="info-section scroll-reveal">
          <h3>💰 五、预约与支付</h3>
          <div class="section-content">
            <p>5.1 用户通过本平台预约时，应如实填写预约信息，并按时到店消费。</p>
            <p class="highlight">5.2 <strong>预约成功后，如需取消，应在规定时间内操作。逾期取消可能产生违约金。具体规则以各门店政策为准。</strong></p>
            <p>5.3 用户应使用合法的支付方式完成支付，不得使用他人账户或盗用他人银行卡信息。</p>
            <p>5.4 因支付过程中用户操作不当或第三方支付平台原因导致的支付失败，本平台不承担责任。</p>
          </div>
        </section>

        <!-- 优惠券与积分 -->
        <section class="info-section scroll-reveal">
          <h3>🎁 六、优惠券与积分</h3>
          <div class="section-content">
            <p>6.1 本平台可能向用户发放优惠券、积分等优惠。</p>
            <p>6.2 优惠券和积分的获取、使用规则以平台规定为准，不得转让、买卖。</p>
            <p>6.3 优惠券和积分均有有效期，过期自动失效。</p>
          </div>
        </section>

        <!-- 知识产权 -->
        <section class="info-section scroll-reveal">
          <h3>©️ 七、知识产权</h3>
          <div class="section-content">
            <p>7.1 本平台的所有内容，包括但不限于文字、图片、标识、界面设计、软件等，均属于本平台或相关权利人所有。</p>
            <p class="highlight">7.2 <strong>未经本平台书面许可，用户不得擅自复制、传播、修改本平台的任何内容。</strong></p>
            <p>7.3 用户在本平台发布的内容，应确保拥有相应权利，如侵犯他人知识产权，用户应自行承担法律责任。</p>
          </div>
        </section>

        <!-- 隐私保护 -->
        <section class="info-section scroll-reveal">
          <h3>🔐 八、隐私保护</h3>
          <div class="section-content">
            <p>8.1 本平台重视用户隐私保护，将按照《隐私政策》的规定收集、使用、存储用户信息。</p>
            <p class="highlight">8.2 <strong>本平台承诺不会向第三方出售或出租用户个人信息，但法律法规另有规定的除外。</strong></p>
            <p>8.3 用户理解并同意，为提供更好的服务，本平台可能需要收集用户的位置信息、联系方式等。</p>
          </div>
        </section>

        <!-- 免责声明 -->
        <section class="info-section scroll-reveal">
          <h3>⛔ 九、免责声明</h3>
          <div class="section-content">
            <p>9.1 本平台对服务的及时性、安全性、准确性不作任何保证。</p>
            <p class="highlight">9.2 <strong>因不可抗力（包括但不限于自然灾害、政府行为、黑客攻击等）导致的服务中断，本平台不承担责任。</strong></p>
            <p>9.3 用户因使用本平台服务与第三方产生纠纷的，应自行解决，本平台不承担责任。</p>
            <p>9.4 本平台对用户上传或发布的内容不承担审查义务，如发现违法违规内容，有权随时删除。</p>
          </div>
        </section>

        <!-- 协议变更 -->
        <section class="info-section scroll-reveal">
          <h3>📝 十、协议的变更</h3>
          <div class="section-content">
            <p>10.1 本平台有权根据需要修改本协议，修改后的协议将在平台公布。</p>
            <p class="highlight">10.2 <strong>如用户不同意修改后的协议，应立即停止使用本平台服务。继续使用则视为接受修改后的协议。</strong></p>
          </div>
        </section>

        <!-- 法律适用 -->
        <section class="info-section scroll-reveal">
          <h3>⚖️ 十一、法律适用与争议解决</h3>
          <div class="section-content">
            <p>11.1 本协议的订立、执行和解释及争议的解决均适用中华人民共和国法律。</p>
            <p>11.2 如双方就本协议内容或其执行发生任何争议，应友好协商解决；协商不成的，任何一方均可向本平台所在地人民法院提起诉讼。</p>
          </div>
        </section>

        <!-- 其他 -->
        <section class="info-section scroll-reveal">
          <h3>📎 十二、其他</h3>
          <div class="section-content">
            <p>12.1 本协议的标题仅为方便阅读而设，不影响本协议的解释。</p>
            <p>12.2 如本协议的任何条款被认定为无效，不影响其他条款的效力。</p>
            <p>12.3 本平台对本协议拥有最终解释权。</p>
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
                <p>legal@jubensha.com</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><Clock /></el-icon>
              <div>
                <h4>服务时间</h4>
                <p>周一至周日 9:00-21:00</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><Location /></el-icon>
              <div>
                <h4>公司地址</h4>
                <p>北京市朝阳区xx路xx号</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 底部提示 -->
        <div class="agreement-footer scroll-reveal">
          <el-alert type="success" :closable="false">
            <p>如您对本协议有任何疑问，请联系我们的客服团队。感谢您选择剧本杀在线预约平台！</p>
          </el-alert>
        </div>
      </div>
    </el-card>
    
    <!-- 分享按钮 -->
    <ShareButtons />
    
    <!-- 返回顶部 -->
    <BackToTop />
    
    <!-- 主题切换器 -->
    <ThemeSwitcher />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  Phone,
  Message,
  Clock,
  Location
} from '@element-plus/icons-vue'
import InfoPageBreadcrumb from '@/components/InfoPageBreadcrumb.vue'
import ShareButtons from '@/components/ShareButtons.vue'
import BackToTop from '@/components/BackToTop.vue'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { useTheme } from '@/composables/useTheme'

// 初始化滚动显示动画
useScrollReveal()

// 初始化主题
const { loadTheme } = useTheme()
loadTheme()

// 协议概览
const overviewItems = ref([
  {
    icon: '🔐',
    title: '隐私保护',
    desc: '严格保护您的个人信息安全'
  },
  {
    icon: '💳',
    title: '支付保障',
    desc: '安全支付，退款有保障'
  },
  {
    icon: '⏰',
    title: '服务承诺',
    desc: '7x15小时客服在线服务'
  },
  {
    icon: '📋',
    title: '权益清晰',
    desc: '用户权益条款清晰明确'
  },
  {
    icon: '🛡️',
    title: '账号安全',
    desc: '多重保护，账号更安全'
  },
  {
    icon: '⚖️',
    title: '公平公正',
    desc: '合规运营，公平交易'
  }
])
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
  background: #1a1a2e url('https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=1200&h=400&fit=crop') center/cover;
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
  background: rgba(26, 26, 46, 0.88);
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

/* 协议概览网格 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 20px;
}

.overview-item {
  padding: 25px 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid rgba(139, 0, 0, 0.2);
}

.overview-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(139, 0, 0, 0.25);
  border-color: #8B0000;
  background: linear-gradient(135deg, rgba(45, 45, 75, 0.98) 0%, rgba(40, 55, 90, 0.98) 100%);
}

.overview-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.overview-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.overview-item p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

/* 内容区域 */
.info-content {
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.notice-alert {
  margin-bottom: 30px;
}

.notice-alert p {
  margin: 0;
  line-height: 1.8;
}

.info-section {
  margin-bottom: 30px;
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
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #8B0000;
  display: inline-block;
}

.section-content {
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.9) 0%, rgba(30, 45, 80, 0.9) 100%);
  padding: 20px;
  border-radius: 10px;
  border: 1px solid rgba(139, 0, 0, 0.15);
}

.section-content p {
  margin: 12px 0;
  font-size: 15px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.85);
}

.section-content p.highlight {
  background: rgba(139, 0, 0, 0.2);
  padding: 12px 15px;
  border-radius: 8px;
  border-left: 4px solid #ff6b6b;
}

.section-content p.highlight strong {
  color: #ff6b6b;
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

/* 底部 */
.agreement-footer {
  margin-top: 30px;
  padding-top: 20px;
}

.agreement-footer p {
  margin: 0;
  line-height: 1.8;
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
  
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

<!-- 非 scoped 样式，用于 scroll-reveal 动画和覆盖 Element Plus -->
<style>
.agreement-page .scroll-reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.agreement-page .scroll-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

/* 覆盖 Element Plus 卡片背景 */
.agreement-page .info-card.el-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  --el-card-bg-color: transparent !important;
}

.agreement-page .info-card.el-card .el-card__header {
  background: transparent !important;
  border-bottom-color: rgba(139, 0, 0, 0.2) !important;
}

.agreement-page .info-card.el-card .el-card__body {
  background: transparent !important;
}

/* 覆盖 Element Plus Alert 样式 */
.agreement-page .el-alert--warning {
  background: rgba(139, 0, 0, 0.15) !important;
  border-color: rgba(139, 0, 0, 0.3) !important;
}

.agreement-page .el-alert--warning .el-alert__title {
  color: #ff6b6b !important;
}

.agreement-page .el-alert--warning .el-alert__description,
.agreement-page .el-alert--warning p {
  color: rgba(255, 255, 255, 0.85) !important;
}

.agreement-page .el-alert--warning .el-alert__description strong {
  color: #ff6b6b !important;
}

.agreement-page .el-alert--success {
  background: rgba(76, 175, 80, 0.15) !important;
  border-color: rgba(76, 175, 80, 0.3) !important;
}

.agreement-page .el-alert--success .el-alert__title {
  color: #81c784 !important;
}

.agreement-page .el-alert--success .el-alert__description,
.agreement-page .el-alert--success p {
  color: rgba(255, 255, 255, 0.85) !important;
}
</style>
