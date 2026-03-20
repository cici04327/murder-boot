<template>
  <div class="info-page about-page">
    <!-- 面包屑导航 -->
    <InfoPageBreadcrumb />
    
    <!-- 头部横幅 - 剧本杀主题 -->
    <div class="hero-banner murder-theme">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-icon">🎭</div>
        <h1 class="hero-title animate-fade-in-up">
          探索剧本杀的无限可能
        </h1>
        <p class="hero-subtitle animate-fade-in-up delay-1">
          沉浸式推理体验 · 每一场都是独特的故事
        </p>
        <div class="hero-stats animate-fade-in-up delay-2">
          <div class="stat-item">
            <div class="stat-icon">📚</div>
            <div class="stat-number">500+</div>
            <div class="stat-label">精选剧本</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">🏠</div>
            <div class="stat-number">100+</div>
            <div class="stat-label">合作门店</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">👥</div>
            <div class="stat-number">10W+</div>
            <div class="stat-label">注册用户</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">⭐</div>
            <div class="stat-number">98%</div>
            <div class="stat-label">好评率</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 剧本杀类型介绍 -->
    <el-card class="info-card script-types-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>🎬 剧本类型</h2>
          <span class="card-subtitle">总有一款适合你的故事</span>
        </div>
      </template>
      <div class="script-types-grid">
        <div class="script-type-item" v-for="type in scriptTypes" :key="type.name">
          <div class="type-icon">{{ type.icon }}</div>
          <h4>{{ type.name }}</h4>
          <p>{{ type.desc }}</p>
          <div class="type-tags">
            <el-tag v-for="tag in type.tags" :key="tag" size="small" :type="type.tagType">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 关于我们 -->
    <el-card class="info-card animate-fade-in">
      <template #header>
        <div class="card-header">
          <h2>🏢 关于我们</h2>
        </div>
      </template>
      
      <div class="info-content">
        <section class="info-section scroll-reveal">
          <h3>🎯 平台简介</h3>
          <div class="intro-content">
            <p>
              剧本杀在线预约平台是国内领先的沉浸式推理娱乐服务平台。我们汇聚了全国优质的剧本杀门店和海量精品剧本，
              致力于为每一位剧本杀爱好者提供便捷的预约服务和极致的游戏体验。
            </p>
            <p>
              无论你是喜欢烧脑推理的硬核玩家，还是享受角色扮演的情感派，亦或是追求刺激的恐怖爱好者，
              在这里，你都能找到属于自己的那场完美剧本。
            </p>
          </div>
        </section>

        <section class="info-section scroll-reveal">
          <h3>🎭 我们的使命</h3>
          <div class="mission-cards">
            <div class="mission-item">
              <div class="mission-icon">🌟</div>
              <h4>传递快乐</h4>
              <p>让每一位玩家都能在剧本杀中收获快乐与友谊</p>
            </div>
            <div class="mission-item">
              <div class="mission-icon">🔍</div>
              <h4>推广文化</h4>
              <p>让剧本杀文化走进千家万户，成为新时代社交娱乐方式</p>
            </div>
            <div class="mission-item">
              <div class="mission-icon">🤝</div>
              <h4>连接行业</h4>
              <p>搭建玩家与门店的桥梁，推动行业健康发展</p>
            </div>
          </div>
        </section>

        <section class="info-section scroll-reveal">
          <h3>💪 核心优势</h3>
          <div class="advantage-list">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :md="6">
                <div class="advantage-item">
                  <div class="advantage-icon">📖</div>
                  <h4>海量剧本</h4>
                  <p>涵盖情感、推理、恐怖、欢乐等多种类型，新本持续更新</p>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <div class="advantage-item">
                  <div class="advantage-icon">🏆</div>
                  <h4>优质门店</h4>
                  <p>严格筛选合作门店，专业DM主持，沉浸式场景布置</p>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <div class="advantage-item">
                  <div class="advantage-icon">⚡</div>
                  <h4>便捷预约</h4>
                  <p>在线实时预约，智能推荐匹配，一键组局开车</p>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <div class="advantage-item">
                  <div class="advantage-icon">🎁</div>
                  <h4>专属福利</h4>
                  <p>新人礼包、会员折扣、积分兑换，玩得越多越划算</p>
                </div>
              </el-col>
            </el-row>
          </div>
        </section>

        <section class="info-section scroll-reveal">
          <h3>🎮 玩家须知</h3>
          <div class="player-guide">
            <div class="guide-item">
              <div class="guide-step">1</div>
              <div class="guide-content">
                <h4>选择剧本</h4>
                <p>根据人数、时长、类型筛选适合的剧本，查看评分和评价</p>
              </div>
            </div>
            <div class="guide-item">
              <div class="guide-step">2</div>
              <div class="guide-content">
                <h4>预约门店</h4>
                <p>选择心仪门店和时间档期，支持拼车组队或包场</p>
              </div>
            </div>
            <div class="guide-item">
              <div class="guide-step">3</div>
              <div class="guide-content">
                <h4>到店体验</h4>
                <p>准时到店，换装入戏，在DM带领下开启推理之旅</p>
              </div>
            </div>
            <div class="guide-item">
              <div class="guide-step">4</div>
              <div class="guide-content">
                <h4>分享评价</h4>
                <p>游戏结束后发表评价，分享体验心得，赚取积分奖励</p>
              </div>
            </div>
          </div>
        </section>

        <section class="info-section scroll-reveal">
          <h3>📅 发展历程</h3>
          <el-timeline>
            <el-timeline-item timestamp="2024年" placement="top" color="#16213e">
              <el-card class="timeline-card">
                <h4>🚀 全面升级</h4>
                <p>平台2.0版本上线，新增AI智能推荐、实时拼车、VIP会员体系</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item timestamp="2023年" placement="top" color="#16213e">
              <el-card class="timeline-card">
                <h4>📈 快速增长</h4>
                <p>合作门店突破100家，累计服务用户超过10万人次，获得行业认可</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item timestamp="2022年" placement="top" color="#16213e">
              <el-card class="timeline-card">
                <h4>🌱 初具规模</h4>
                <p>完成首轮融资，技术团队扩充，引入智能匹配算法</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item timestamp="2021年" placement="top" color="#16213e">
              <el-card class="timeline-card">
                <h4>🎬 梦想起航</h4>
                <p>平台正式成立，由一群热爱剧本杀的年轻人创建</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="info-section scroll-reveal">
          <h3>📞 联系方式</h3>
          <div class="contact-grid">
            <div class="contact-card">
              <el-icon :size="28"><Location /></el-icon>
              <div>
                <h4>公司地址</h4>
                <p>北京市朝阳区xx路xx号xx大厦xx层</p>
              </div>
            </div>
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
                <p>contact@jubensha.com</p>
              </div>
            </div>
            <div class="contact-card">
              <el-icon :size="28"><Clock /></el-icon>
              <div>
                <h4>服务时间</h4>
                <p>周一至周日 9:00-21:00</p>
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
    
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  Location,
  Phone,
  Message,
  Clock
} from '@element-plus/icons-vue'
import InfoPageBreadcrumb from '@/components/InfoPageBreadcrumb.vue'
import ShareButtons from '@/components/ShareButtons.vue'
import BackToTop from '@/components/BackToTop.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'

// 初始化滚动显示动画
useScrollReveal()

// 剧本类型数据
const scriptTypes = ref([
  {
    icon: '🔍',
    name: '硬核推理',
    desc: '烧脑逻辑推理，层层剥茧抽丝，适合推理爱好者',
    tags: ['高智商', '逻辑', '本格'],
    tagType: ''
  },
  {
    icon: '💕',
    name: '情感沉浸',
    desc: '深度角色扮演，感受悲欢离合，体验不同人生',
    tags: ['催泪', '治愈', '人物'],
    tagType: 'success'
  },
  {
    icon: '👻',
    name: '恐怖惊悚',
    desc: '沉浸式恐怖体验，胆小慎入，挑战你的心跳极限',
    tags: ['刺激', '氛围', '惊悚'],
    tagType: 'danger'
  },
  {
    icon: '😄',
    name: '欢乐搞笑',
    desc: '轻松愉快氛围，笑声不断，新手友好型剧本',
    tags: ['欢乐', '休闲', '新手'],
    tagType: 'warning'
  },
  {
    icon: '⚔️',
    name: '机制对抗',
    desc: '阵营博弈对抗，策略与运气并存的精彩对决',
    tags: ['策略', '对抗', '博弈'],
    tagType: 'info'
  },
  {
    icon: '🏰',
    name: '古风武侠',
    desc: '穿越时空体验，江湖恩怨情仇，侠骨柔情',
    tags: ['古风', '武侠', '仙侠'],
    tagType: ''
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
  background: #1a1a2e url('https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&h=400&fit=crop') center/cover;
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

/* 剧本类型卡片 */
.script-types-card {
  margin-bottom: 20px;
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

.script-types-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 20px;
}

.script-type-item {
  padding: 25px 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid rgba(192, 57, 43, 0.3);
}

.script-type-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.25);
  border-color: #16213e;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.98) 0%, rgba(30, 45, 80, 0.98) 100%);
}

.type-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.script-type-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.script-type-item p {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
}

.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  justify-content: center;
}

/* 信息卡片 */
.info-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.info-content {
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.info-section {
  margin-bottom: 50px;
  padding: 25px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 12px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.info-section:last-child {
  margin-bottom: 0;
}

.info-section h3 {
  font-size: 20px;
  color: #fff;
  margin-bottom: 25px;
  padding-bottom: 12px;
  border-bottom: 2px solid #16213e;
  display: inline-block;
}

.intro-content p {
  margin: 15px 0;
  font-size: 15px;
  text-indent: 2em;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.85);
}

/* 使命卡片 */
.mission-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.mission-item {
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.25);
}

.mission-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(192, 57, 43,0.3);
  border-color: #16213e;
}

.mission-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.mission-item h4 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #fff;
}

.mission-item p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* 优势列表 */
.advantage-item {
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  margin-bottom: 20px;
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.25);
}

.advantage-item:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(192, 57, 43,0.3);
  border-color: #16213e;
}

.advantage-icon {
  font-size: 42px;
  margin-bottom: 15px;
}

.advantage-item h4 {
  margin: 0 0 10px 0;
  font-size: 17px;
  color: #fff;
}

.advantage-item p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
}

/* 玩家指南 */
.player-guide {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.guide-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%);
  border-radius: 12px;
  transition: all 0.3s;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.guide-item:hover {
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.2);
  border-color: rgba(192, 57, 43, 0.4);
}

.guide-step {
  width: 40px;
  height: 40px;
  background: #16213e;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
}

.guide-content h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.guide-content p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* 时间线 */
.timeline-card {
  box-shadow: none;
  border: 1px solid rgba(192, 57, 43, 0.25);
  background: linear-gradient(135deg, rgba(35, 35, 60, 0.95) 0%, rgba(30, 45, 80, 0.95) 100%) !important;
}

.timeline-card h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #fff;
}

.timeline-card p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
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
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.contact-card:hover {
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.2);
  border-color: rgba(192, 57, 43, 0.4);
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
}
</style>

<!-- 非 scoped 样式，用于 scroll-reveal 动画 -->
<style>
.about-page .scroll-reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.about-page .scroll-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}
</style>
