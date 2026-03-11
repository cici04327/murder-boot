<template>
  <div class="profile-enhanced">
    <!-- 剧本杀主题装饰背景 -->
    <div class="mystery-bg">
      <div class="floating-icon icon-1">🔍</div>
      <div class="floating-icon icon-2">🗝️</div>
      <div class="floating-icon icon-3">📜</div>
      <div class="floating-icon icon-4">🎭</div>
      <div class="floating-icon icon-5">💀</div>
    </div>

    <!-- 顶部用户信息卡片 - 剧本杀主题 -->
    <el-card class="user-info-card mystery-card" shadow="always">
      <div class="user-header">
        <div class="user-avatar-section">
          <!-- 侦探等级光环 -->
          <div class="avatar-ring" :class="detectiveLevelClass">
            <el-avatar :size="100" :src="userInfo.avatar" class="user-avatar">
              <el-icon size="50"><User /></el-icon>
            </el-avatar>
            <div class="level-badge">
              <span class="level-icon">{{ detectiveLevel.icon }}</span>
            </div>
          </div>
          <el-upload
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
            :headers="uploadHeaders"
            :before-upload="beforeAvatarUpload"
            action="/api/user/upload/avatar"
            accept="image/*"
          >
            <el-button size="small" class="upload-btn mystery-btn">
              <el-icon><Camera /></el-icon>
              更换头像
            </el-button>
          </el-upload>
        </div>
        
        <div class="user-info">
          <div class="user-name-row">
            <h2>{{ userInfo.username || '神秘玩家' }}</h2>
            <el-tag v-if="userInfo.vipLevel" type="warning" effect="dark" size="large" class="vip-tag">
              <el-icon><Medal /></el-icon>
              VIP{{ userInfo.vipLevel }}
            </el-tag>
          </div>
          
          <!-- 侦探称号 -->
          <div class="detective-title">
            <span class="title-badge">{{ detectiveLevel.icon }} {{ detectiveLevel.title }}</span>
            <el-tooltip :content="'距离下一等级还需' + detectiveLevel.nextLevelExp + '经验'" placement="top">
              <div class="exp-bar">
                <div class="exp-progress" :style="{width: detectiveLevel.progress + '%'}"></div>
                <span class="exp-text">EXP {{ detectiveLevel.currentExp }}/{{ detectiveLevel.maxExp }}</span>
              </div>
            </el-tooltip>
          </div>
          
          <div class="user-meta">
            <span class="meta-item">
              <el-icon><Phone /></el-icon>
              {{ userInfo.phone || '未绑定' }}
            </span>
            <span class="meta-item">
              <el-icon><Message /></el-icon>
              {{ userInfo.email || '未绑定' }}
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              入行于 {{ formatDate(userInfo.createTime) }}
            </span>
          </div>
          
          <div class="user-actions">
            <el-button type="primary" class="mystery-btn-primary" @click="editProfileDialog = true">
              <el-icon><Edit /></el-icon>
              编辑档案
            </el-button>
            <el-button class="mystery-btn-default" @click="showPasswordDialog = true">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
            <el-button type="warning" class="mystery-btn-vip" v-if="!userInfo.vipLevel" @click="showVipDialog = true">
              <el-icon><Medal /></el-icon>
              成为VIP侦探
            </el-button>
          </div>
        </div>
      </div>
    </el-card>


    <!-- 数据统计卡片 - 剧本杀主题 -->
    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6">
        <div class="stat-card mystery-stat-card stat-cases">
          <div class="stat-decoration">🔍</div>
          <div class="stat-content">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon" size="36"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reservationCount }}</div>
              <div class="stat-label">已破案件</div>
            </div>
          </div>
          <el-button text class="stat-link" @click="$router.push('/user/reservations')">
            查看案件记录 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="stat-card mystery-stat-card stat-scripts">
          <div class="stat-decoration">📜</div>
          <div class="stat-content">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon" size="36"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.favoriteCount }}</div>
              <div class="stat-label">收藏剧本</div>
            </div>
          </div>
          <el-button text class="stat-link" @click="$router.push('/user/favorites')">
            查看剧本库 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="stat-card mystery-stat-card stat-clues">
          <div class="stat-decoration">🗝️</div>
          <div class="stat-content">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon" size="36"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.points }}</div>
              <div class="stat-label">线索积分</div>
            </div>
          </div>
          <el-button text class="stat-link" @click="$router.push('/user/points')">
            查看积分详情 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="stat-card mystery-stat-card stat-items">
          <div class="stat-decoration">🎫</div>
          <div class="stat-content">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon" size="36"><Ticket /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.couponCount }}</div>
              <div class="stat-label">道具券</div>
            </div>
          </div>
          <el-button text class="stat-link" @click="$router.push('/user/coupons')">
            查看道具背包 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 待处理案件 -->
    <el-card class="pending-section mystery-card" shadow="never" v-if="hasPendingItems">
      <template #header>
        <div class="card-header">
          <span><span class="header-icon">📋</span> 待处理案件</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8" v-if="pendingItems.unpaidCount > 0">
          <div class="pending-item pending-case pending-warning" @click="handlePendingClick('unpaid')">
            <div class="pending-case-icon">💰</div>
            <el-badge :value="pendingItems.unpaidCount" class="item-badge">
              <el-icon class="pending-icon" size="30"><Wallet /></el-icon>
            </el-badge>
            <div class="pending-info">
              <div class="pending-title">待支付报酬</div>
              <div class="pending-desc">{{ pendingItems.unpaidCount }} 个案件待付款</div>
            </div>
            <el-icon class="pending-arrow"><ArrowRight /></el-icon>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="8" v-if="pendingItems.unusedCount > 0">
          <div class="pending-item pending-case pending-success" @click="handlePendingClick('unused')">
            <div class="pending-case-icon">🎯</div>
            <el-badge :value="pendingItems.unusedCount" class="item-badge">
              <el-icon class="pending-icon" size="30"><Calendar /></el-icon>
            </el-badge>
            <div class="pending-info">
              <div class="pending-title">待执行任务</div>
              <div class="pending-desc">{{ pendingItems.unusedCount }} 个案件待开启</div>
            </div>
            <el-icon class="pending-arrow"><ArrowRight /></el-icon>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="8" v-if="pendingItems.unreadCount > 0">
          <div class="pending-item pending-case pending-primary" @click="handlePendingClick('unread')">
            <div class="pending-case-icon">💌</div>
            <el-badge :value="pendingItems.unreadCount" class="item-badge">
              <el-icon class="pending-icon" size="30"><Message /></el-icon>
            </el-badge>
            <div class="pending-info">
              <div class="pending-title">神秘信件</div>
              <div class="pending-desc">{{ pendingItems.unreadCount }} 封密函未读</div>
            </div>
            <el-icon class="pending-arrow"><ArrowRight /></el-icon>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 快捷操作 - 侦探工具箱 -->
    <el-card class="quick-actions-section mystery-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span><span class="header-icon">🧰</span> 侦探工具箱</span>
        </div>
      </template>
      
      <el-row :gutter="15">
        <el-col :xs="12" :sm="8" :md="6" v-for="action in quickActions" :key="action.name">
          <div class="quick-action-item mystery-action" @click="handleQuickAction(action.path)">
            <div class="action-icon-wrap" :style="{background: action.gradient || 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'}">
              <span class="action-emoji">{{ action.emoji }}</span>
            </div>
            <div class="action-name">{{ action.name }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 侦探成就徽章 -->
    <el-card class="achievements-section mystery-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span><span class="header-icon">🏆</span> 侦探勋章</span>
          <el-button text size="small" @click="showAllAchievements = true">
            勋章墙 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      
      <el-row :gutter="15">
        <el-col :xs="12" :sm="8" :md="6" v-for="achievement in achievements" :key="achievement.id">
          <div class="achievement-item mystery-achievement" :class="{'achievement-unlocked': achievement.unlocked}">
            <div class="achievement-badge" :class="achievement.rarity">
              <span class="badge-icon">{{ achievement.emoji }}</span>
            </div>
            <div class="achievement-name">{{ achievement.name }}</div>
            <div class="achievement-desc">{{ achievement.desc }}</div>
            <div class="achievement-rarity-tag" v-if="achievement.unlocked">
              <span :class="'rarity-' + achievement.rarity">{{ achievement.rarityText }}</span>
            </div>
            <el-progress 
              v-else 
              :percentage="achievement.progress" 
              :show-text="false"
              :stroke-width="6"
              :color="achievement.color"
              style="width: 100%"
            />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 最近案件记录 -->
    <el-card class="recent-cases-section mystery-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span><span class="header-icon">📁</span> 最近案件档案</span>
          <el-button text size="small" @click="$router.push('/user/reservations')">
            全部记录 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      
      <div class="cases-timeline" v-if="recentCases.length > 0">
        <div class="case-item" v-for="(caseItem, index) in recentCases" :key="index">
          <div class="case-date">
            <span class="date-day">{{ caseItem.day }}</span>
            <span class="date-month">{{ caseItem.month }}</span>
          </div>
          <div class="case-line">
            <div class="case-dot" :class="caseItem.status"></div>
          </div>
          <div class="case-content">
            <div class="case-title">{{ caseItem.scriptName }}</div>
            <div class="case-meta">
              <span class="case-store">🏠 {{ caseItem.storeName }}</span>
              <span class="case-role" v-if="caseItem.role">🎭 {{ caseItem.role }}</span>
            </div>
            <el-tag :type="caseItem.statusType" size="small">{{ caseItem.statusText }}</el-tag>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无案件记录，快去接受新任务吧！" :image-size="80">
        <el-button type="primary" @click="$router.push('/script')">
          <span class="btn-emoji">🔍</span> 探索剧本
        </el-button>
      </el-empty>
    </el-card>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="editProfileDialog" title="编辑个人资料" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.username" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editProfileDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- VIP开通对话框 -->
    <el-dialog v-model="showVipDialog" title="开通VIP会员" width="800px">
      <div class="vip-plans">
        <div 
          class="vip-plan" 
          v-for="plan in vipPlans" 
          :key="plan.id"
          :class="{'vip-plan-selected': selectedPlan === plan.id}"
          @click="selectedPlan = plan.id"
        >
          <el-tag v-if="plan.tag" type="danger" class="plan-tag">{{ plan.tag }}</el-tag>
          <h3>{{ plan.name }}</h3>
          <div class="plan-price">
            <span class="price">¥{{ plan.price }}</span>
            <span class="unit">/{{ plan.duration }}</span>
          </div>
          <ul class="plan-features">
            <li v-for="feature in plan.features" :key="feature">
              <el-icon color="#67C23A"><Check /></el-icon>
              {{ feature }}
            </li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="showVipDialog = false">取消</el-button>
        <el-button type="warning" @click="handlePurchaseVip" :loading="vipPurchasing">
          <el-icon><Medal /></el-icon>
          立即开通
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserInfo, getUserPoints, updateUserInfo, updatePassword, completeProfileTask } from '@/api/user'
import { getMyReservations } from '@/api/reservation'
import { getMyCoupons } from '@/api/coupon'
import { getFavoriteScripts } from '@/api/script'
import { purchaseVip, getVipPackages } from '@/api/vip'
import {
  User, Camera, Phone, Message, Calendar, Edit, Lock,
  Tickets, Star, Coin, Ticket, Bell, Wallet, ArrowRight,
  Operation, Trophy, Check, Medal,
  Grid, List, ChatLineRound, SetUp, DataAnalysis, Memo
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref({
  username: '',
  phone: '',
  email: '',
  avatar: '',
  vipLevel: 0,
  createTime: ''
})

const stats = ref({
  reservationCount: 0,
  favoriteCount: 0,
  points: 0,
  couponCount: 0
})

const pendingItems = ref({
  unpaidCount: 0,
  unusedCount: 0,
  unreadCount: 0
})

const hasPendingItems = computed(() => {
  return pendingItems.value.unpaidCount > 0 || 
         pendingItems.value.unusedCount > 0 || 
         pendingItems.value.unreadCount > 0
})

// 侦探等级系统
const detectiveLevel = ref({
  level: 1,
  title: '见习侦探',
  icon: '🔰',
  currentExp: 0,
  maxExp: 100,
  nextLevelExp: 100,
  progress: 0
})

const detectiveLevelClass = computed(() => {
  const level = detectiveLevel.value.level
  if (level >= 10) return 'level-legendary'
  if (level >= 7) return 'level-epic'
  if (level >= 5) return 'level-rare'
  if (level >= 3) return 'level-uncommon'
  return 'level-common'
})

// 侦探等级配置
const detectiveLevels = [
  { level: 1, title: '见习侦探', icon: '🔰', minExp: 0 },
  { level: 2, title: '初级侦探', icon: '🔍', minExp: 100 },
  { level: 3, title: '正式侦探', icon: '🕵️', minExp: 300 },
  { level: 4, title: '资深侦探', icon: '🎯', minExp: 600 },
  { level: 5, title: '精英侦探', icon: '⭐', minExp: 1000 },
  { level: 6, title: '高级侦探', icon: '🌟', minExp: 1500 },
  { level: 7, title: '王牌侦探', icon: '💎', minExp: 2200 },
  { level: 8, title: '传奇侦探', icon: '👑', minExp: 3000 },
  { level: 9, title: '神探', icon: '🏆', minExp: 4000 },
  { level: 10, title: '名侦探', icon: '🎭', minExp: 5500 }
]


// 最近案件记录
const recentCases = ref([])

const quickActions = ref([
  { name: '探索剧本', path: '/script', emoji: '🔍', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { name: '案件记录', path: '/user/reservations', emoji: '📋', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { name: '剧本收藏', path: '/user/favorites', emoji: '⭐', gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
  { name: '道具背包', path: '/user/coupons', emoji: '🎒', gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' },
  { name: '线索积分', path: '/user/points', emoji: '🗝️', gradient: 'linear-gradient(135deg, #96fbc4 0%, #f9f586 100%)' },
  { name: '神秘信件', path: '/user/notifications', emoji: '💌', gradient: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)' },
  { name: '我的留言', path: '/user/feedbacks', emoji: '📬', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { name: '侦探档案', path: '/user/settings', emoji: '📁', gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)' },
  { name: '破案统计', path: '/user/statistics', emoji: '📊', gradient: 'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)' }
])

const achievements = ref([
  { id: 1, name: '初出茅庐', desc: '完成首次案件', emoji: '🔰', unlocked: false, progress: 0, rarity: 'common', rarityText: '普通', color: '#909399' },
  { id: 2, name: '破案专家', desc: '成功破解10个案件', emoji: '🔍', unlocked: false, progress: 0, rarity: 'uncommon', rarityText: '稀有', color: '#67C23A' },
  { id: 3, name: '真相猎手', desc: '找出5次真凶', emoji: '🎯', unlocked: false, progress: 0, rarity: 'rare', rarityText: '精良', color: '#409EFF' },
  { id: 4, name: '完美伪装', desc: '作为凶手3次逃脱', emoji: '🎭', unlocked: false, progress: 0, rarity: 'epic', rarityText: '史诗', color: '#E6A23C' },
  { id: 5, name: '社交名探', desc: '邀请5位好友入坑', emoji: '🤝', unlocked: false, progress: 0, rarity: 'rare', rarityText: '精良', color: '#409EFF' },
  { id: 6, name: '剧评大师', desc: '发表20条精彩点评', emoji: '✍️', unlocked: false, progress: 0, rarity: 'uncommon', rarityText: '稀有', color: '#67C23A' },
  { id: 7, name: '收藏家', desc: '收藏50个剧本', emoji: '📚', unlocked: false, progress: 0, rarity: 'epic', rarityText: '史诗', color: '#E6A23C' },
  { id: 8, name: '传奇侦探', desc: '达到10级侦探等级', emoji: '👑', unlocked: false, progress: 0, rarity: 'legendary', rarityText: '传说', color: '#F56C6C' }
])

const vipPlans = ref([
  {
    id: 1,
    name: '月度会员',
    duration: '月',
    price: 29,
    features: ['专属客服', '预约优先', '积分双倍', '每月赠送优惠券']
  },
  {
    id: 2,
    name: '季度会员',
    duration: '季',
    price: 79,
    tag: '推荐',
    features: ['专属客服', '预约优先', '积分三倍', '每月赠送优惠券', '生日特权']
  },
  {
    id: 3,
    name: '年度会员',
    duration: '年',
    price: 268,
    tag: '最划算',
    features: ['专属客服', '预约优先', '积分五倍', '每月赠送优惠券', '生日特权', '专属徽章']
  }
])

const editProfileDialog = ref(false)
const showPasswordDialog = ref(false)
const showVipDialog = ref(false)
const showAllAchievements = ref(false)
const selectedPlan = ref(2)

const editForm = reactive({
  username: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    // 从 userStore 获取基本信息
    const user = userStore.userInfo
    if (user) {
      userInfo.value = {
        username: user.username || user.name || '用户',
        phone: user.phone || '',
        email: user.email || '',
        avatar: user.avatar || '',
        vipLevel: user.vipLevel || 0,
        createTime: user.createTime || ''
      }
      
      // 更新编辑表单
      editForm.username = userInfo.value.username
      editForm.phone = userInfo.value.phone
      editForm.email = userInfo.value.email
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 加载预约数量
    const reservationRes = await getMyReservations({ page: 1, pageSize: 1 })
    if (reservationRes.code === 1 || reservationRes.code === 200) {
      stats.value.reservationCount = reservationRes.data.total || 0
      
      // 计算待付款和待使用数量
      const allReservations = await getMyReservations({ page: 1, pageSize: 100 })
      if (allReservations.code === 1 || allReservations.code === 200) {
        const records = allReservations.data.records || []
        pendingItems.value.unpaidCount = records.filter(r => r.status === 0 || r.status === '待付款').length
        pendingItems.value.unusedCount = records.filter(r => r.status === 1 || r.status === '待使用').length
      }
    }
    
    // 加载收藏数量
    const favoriteRes = await getFavoriteScripts({ page: 1, pageSize: 1 })
    if (favoriteRes.code === 1 || favoriteRes.code === 200) {
      stats.value.favoriteCount = favoriteRes.data.total || (favoriteRes.data.records || []).length
    }
    
    // 加载优惠券数量（修复：后端total不准确，使用records.length）
    const couponRes = await getMyCoupons({ status: 1, page: 1, pageSize: 1000 })
    if (couponRes.code === 1 || couponRes.code === 200) {
      // 使用records.length获取实际优惠券数量
      stats.value.couponCount = couponRes.data?.records?.length || 0
      console.log('个人中心优惠券数量:', stats.value.couponCount)
    }
    
    // 加载积分（从API获取最新数据）
    try {
      const pointsRes = await getUserPoints()
      if (pointsRes.code === 1 || pointsRes.code === 200) {
        stats.value.points = pointsRes.data.currentPoints || 0
        console.log('账户中心积分已更新:', stats.value.points)
      }
    } catch (error) {
      console.error('加载积分失败:', error)
      // 如果API调用失败，尝试从 userStore 获取
      if (userStore.userInfo && userStore.userInfo.points !== undefined) {
        stats.value.points = userStore.userInfo.points
      }
    }
    
    // 计算侦探等级
    calculateDetectiveLevel()
    
    
    // 计算成就进度
    calculateAchievements()
    
    // 加载最近案件
    loadRecentCases()
    
    console.log('统计数据加载完成:', stats.value)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 计算侦探等级
const calculateDetectiveLevel = () => {
  // 经验值计算：预约数 * 50 + 收藏数 * 10 + 积分 / 10
  const totalExp = stats.value.reservationCount * 50 + stats.value.favoriteCount * 10 + Math.floor(stats.value.points / 10)
  
  let currentLevel = detectiveLevels[0]
  let nextLevel = detectiveLevels[1]
  
  for (let i = detectiveLevels.length - 1; i >= 0; i--) {
    if (totalExp >= detectiveLevels[i].minExp) {
      currentLevel = detectiveLevels[i]
      nextLevel = detectiveLevels[i + 1] || detectiveLevels[i]
      break
    }
  }
  
  const currentLevelExp = totalExp - currentLevel.minExp
  const levelExpRange = nextLevel.minExp - currentLevel.minExp
  const progress = levelExpRange > 0 ? Math.min((currentLevelExp / levelExpRange) * 100, 100) : 100
  
  detectiveLevel.value = {
    level: currentLevel.level,
    title: currentLevel.title,
    icon: currentLevel.icon,
    currentExp: totalExp,
    maxExp: nextLevel.minExp,
    nextLevelExp: nextLevel.minExp - totalExp,
    progress: Math.round(progress)
  }
}


// 加载最近案件
const loadRecentCases = async () => {
  try {
    const res = await getMyReservations({ page: 1, pageSize: 5 })
    if (res.code === 1 || res.code === 200) {
      const records = res.data.records || []
      recentCases.value = records.map(r => {
        // 使用订单创建时间
        const date = new Date(r.createTime)
        const statusMap = {
          0: { text: '待付款', type: 'warning', status: 'pending' },
          1: { text: '待开局', type: 'primary', status: 'upcoming' },
          2: { text: '已完成', type: 'success', status: 'completed' },
          3: { text: '已取消', type: 'info', status: 'cancelled' },
          4: { text: '已退款', type: 'danger', status: 'refunded' }
        }
        const statusInfo = statusMap[r.status] || statusMap[0]
        
        return {
          scriptName: r.scriptName || '神秘案件',
          storeName: r.storeName || '未知地点',
          role: r.roleName || '',
          day: date.getDate(),
          month: (date.getMonth() + 1) + '月',
          statusText: statusInfo.text,
          statusType: statusInfo.type,
          status: statusInfo.status
        }
      })
    }
  } catch (error) {
    console.error('加载最近案件失败:', error)
  }
}

// 计算成就进度
const calculateAchievements = () => {
  // 初出茅庐：完成首次案件
  if (stats.value.reservationCount > 0) {
    achievements.value[0].unlocked = true
    achievements.value[0].progress = 100
  }
  
  // 破案专家：成功破解10个案件
  const caseProgress = Math.min((stats.value.reservationCount / 10) * 100, 100)
  achievements.value[1].progress = Math.round(caseProgress)
  if (stats.value.reservationCount >= 10) {
    achievements.value[1].unlocked = true
  }
  
  // 真相猎手：找出5次真凶（基于预约数估算）
  const truthProgress = Math.min((stats.value.reservationCount / 5) * 100, 100)
  achievements.value[2].progress = Math.round(truthProgress)
  if (stats.value.reservationCount >= 5) {
    achievements.value[2].unlocked = true
  }
  
  // 完美伪装：作为凶手3次逃脱
  const disguiseProgress = Math.min((Math.floor(stats.value.reservationCount * 0.15) / 3) * 100, 100)
  achievements.value[3].progress = Math.round(disguiseProgress)
  if (stats.value.reservationCount >= 20) {
    achievements.value[3].unlocked = true
  }
  
  // 社交名探：邀请5位好友入坑
  const socialProgress = Math.min((stats.value.reservationCount / 15) * 100, 100)
  achievements.value[4].progress = Math.round(socialProgress)
  if (stats.value.reservationCount >= 15) {
    achievements.value[4].unlocked = true
  }
  
  // 剧评大师：发表20条精彩点评
  const reviewProgress = Math.min((stats.value.favoriteCount / 5) * 100, 100)
  achievements.value[5].progress = Math.round(reviewProgress)
  if (stats.value.favoriteCount >= 5) {
    achievements.value[5].unlocked = true
  }
  
  // 收藏家：收藏50个剧本
  const collectProgress = Math.min((stats.value.favoriteCount / 50) * 100, 100)
  achievements.value[6].progress = Math.round(collectProgress)
  if (stats.value.favoriteCount >= 50) {
    achievements.value[6].unlocked = true
  }
  
  // 传奇侦探：达到10级侦探等级
  const legendProgress = Math.min((detectiveLevel.value.level / 10) * 100, 100)
  achievements.value[7].progress = Math.round(legendProgress)
  if (detectiveLevel.value.level >= 10) {
    achievements.value[7].unlocked = true
  }
}

// 上传头像需要带的 headers（包含 token）
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { 'token': token } : {}
})

// 上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
    return false
  }
  return true
}

// 处理头像上传成功
const handleAvatarSuccess = (response) => {
  if (response.code === 1 || response.code === 200) {
    userInfo.value.avatar = response.data.url || response.data
    // 更新 userStore
    if (userStore.userInfo) {
      userStore.userInfo.avatar = userInfo.value.avatar
    }
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 处理头像上传失败
const handleAvatarError = (error) => {
  console.error('头像上传失败:', error)
  ElMessage.error('头像上传失败，请稍后重试')
}

// 处理快捷操作点击
const handleQuickAction = (path) => {
  router.push(path)
}

// 处理待处理事项点击
const handlePendingClick = (type) => {
  switch(type) {
    case 'unpaid':
      router.push('/user/reservations?status=unpaid')
      break
    case 'unused':
      router.push('/user/reservations?status=unused')
      break
    case 'unread':
      router.push('/user/notifications')
      break
  }
}

// 更新个人资料
const handleUpdateProfile = async () => {
  // 验证表单
  if (!editForm.username || !editForm.username.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (editForm.phone && !/^1[3-9]\d{9}$/.test(editForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (editForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  try {
    // 调用后端 PUT /api/user 更新资料
    const res = await updateUserInfo({
      username: editForm.username.trim(),
      phone: editForm.phone,
      email: editForm.email
    })
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '更新失败')
      return
    }

    // 更新本地数据 & Store
    userInfo.value.username = editForm.username.trim()
    userInfo.value.phone = editForm.phone
    userInfo.value.email = editForm.email
    if (userStore.userInfo) {
      userStore.userInfo.username = editForm.username.trim()
      userStore.userInfo.phone = editForm.phone
      userStore.userInfo.email = editForm.email
    }

    ElMessage.success('资料更新成功')
    editProfileDialog.value = false

    // 触发完善资料积分任务（首次完善赠30积分）
    try {
      const taskRes = await completeProfileTask()
      if ((taskRes.code === 1 || taskRes.code === 200) && taskRes.data?.completed && !taskRes.data?.alreadyCompleted) {
        ElMessage.success('🎉 恭喜完成「完善资料」任务，获得 30 积分！')
      }
    } catch (_) { /* 积分任务失败不影响主流程 */ }
  } catch (error) {
    console.error('更新资料失败:', error)
    ElMessage.error(error?.msg || '更新失败，请稍后重试')
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword) { ElMessage.warning('请输入原密码'); return }
  if (!passwordForm.newPassword) { ElMessage.warning('请输入新密码'); return }
  if (passwordForm.newPassword.length < 6) { ElMessage.warning('新密码长度至少6位'); return }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) { ElMessage.error('两次密码输入不一致'); return }
  if (passwordForm.oldPassword === passwordForm.newPassword) { ElMessage.warning('新密码不能与原密码相同'); return }

  try {
    const res = await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '修改失败，请检查原密码是否正确')
      return
    }

    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    showPasswordDialog.value = false

    // 退出登录，跳转到登录页
    setTimeout(async () => {
      await userStore.logout?.()
      router.push('/login')
    }, 1500)
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error?.msg || '修改失败，请稍后重试')
  }
}

// 购买VIP
const vipPurchasing = ref(false)
const handlePurchaseVip = async () => {
  const plan = vipPlans.value.find(p => p.id === selectedPlan.value)
  if (!plan) { ElMessage.error('请选择套餐'); return }

  try {
    await ElMessageBox.confirm(
      `确定要开通「${plan.name}」吗？费用为 ¥${plan.price}`,
      '确认开通',
      { confirmButtonText: '立即开通', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return /* 用户取消 */ }

  vipPurchasing.value = true
  try {
    // 调用后端 POST /api/vip/purchase
    const res = await purchaseVip({ packageId: plan.id })
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '开通失败，请稍后重试')
      return
    }

    ElMessage.success(`🎉 恭喜！「${plan.name}」开通成功！`)

    // 从后端返回的最新用户信息中读取VIP等级，兜底用 plan.id
    const newVipLevel = res.data?.vipLevel ?? plan.id
    userInfo.value.vipLevel = newVipLevel
    if (userStore.userInfo) {
      userStore.userInfo.vipLevel = newVipLevel
    }

    showVipDialog.value = false
    // 刷新统计数据
    await loadUserInfo()
  } catch (error) {
    console.error('开通VIP失败:', error)
    ElMessage.error(error?.msg || '开通失败，请稍后重试')
  } finally {
    vipPurchasing.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  loadUserInfo()
  loadStats()
})
</script>

<script>
export default {
  name: 'ProfileEnhanced'
}
</script>

<style scoped>
.profile-enhanced {
  padding: 20px;
  background: transparent;
  min-height: calc(100vh - 64px - 100px); /* 减去header和footer高度 */
  position: relative;
  overflow: hidden;
}

/* 神秘背景装饰 - 性能优化版 */
.mystery-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.floating-icon {
  position: absolute;
  font-size: 40px;
  opacity: 0.08;
  will-change: transform;
  animation: float 20s linear infinite;
  transform: translateZ(0); /* 启用GPU加速 */
}

.icon-1 { top: 10%; left: 5%; animation-delay: 0s; }
.icon-2 { top: 30%; right: 10%; animation-delay: 4s; }
.icon-3 { bottom: 20%; left: 15%; animation-delay: 8s; }
.icon-4 { top: 60%; right: 5%; animation-delay: 12s; }
.icon-5 { bottom: 10%; right: 20%; animation-delay: 16s; }

@keyframes float {
  0%, 100% { transform: translateY(0) translateZ(0); }
  50% { transform: translateY(-15px) translateZ(0); }
}

/* 神秘主题卡片 */
.mystery-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(102, 126, 234, 0.2) !important;
  border-radius: 16px !important;
  position: relative;
  z-index: 1;
}

.header-icon {
  margin-right: 8px;
  font-size: 20px;
}

/* 用户信息卡片 */
.user-info-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%) !important;
  color: #fff;
}

.user-header {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.user-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

/* 侦探等级光环 */
.avatar-ring {
  position: relative;
  padding: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.avatar-ring.level-common { background: linear-gradient(135deg, #909399 0%, #606266 100%); }
.avatar-ring.level-uncommon { background: linear-gradient(135deg, #67C23A 0%, #409EFF 100%); }
.avatar-ring.level-rare { background: linear-gradient(135deg, #409EFF 0%, #764ba2 100%); }
.avatar-ring.level-epic { background: linear-gradient(135deg, #E6A23C 0%, #F56C6C 100%); }
.avatar-ring.level-legendary { 
  background: linear-gradient(135deg, #FFD700 0%, #FF6B6B 50%, #9B59B6 100%);
  box-shadow: 0 0 25px rgba(255, 215, 0, 0.5);
}

.level-badge {
  position: absolute;
  bottom: -5px;
  right: -5px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  border: 2px solid #fff;
}

.level-icon {
  font-size: 16px;
}

.user-avatar {
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  border: 3px solid rgba(255,255,255,0.3);
}

.upload-btn, .mystery-btn {
  font-size: 12px;
  background: rgba(255,255,255,0.1) !important;
  border-color: rgba(255,255,255,0.3) !important;
  color: #fff !important;
}

.upload-btn:hover, .mystery-btn:hover {
  background: rgba(255,255,255,0.2) !important;
}

.user-info {
  flex: 1;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.user-name-row h2 {
  margin: 0;
  font-size: 28px;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.vip-tag {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%) !important;
  border: none !important;
}

/* 侦探称号 */
.detective-title {
  margin-bottom: 15px;
}

.title-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  border-radius: 20px;
  font-size: 14px;
  color: #a0d2ff;
  border: 1px solid rgba(102, 126, 234, 0.5);
  margin-bottom: 8px;
}

.exp-bar {
  width: 200px;
  height: 8px;
  background: rgba(255,255,255,0.1);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.exp-progress {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.exp-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  color: rgba(255,255,255,0.8);
  white-space: nowrap;
}

.user-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: rgba(255,255,255,0.7);
  font-size: 14px;
}

.user-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.mystery-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
}

.mystery-btn-default {
  background: rgba(255,255,255,0.1) !important;
  border-color: rgba(255,255,255,0.3) !important;
  color: #fff !important;
}

.mystery-btn-vip {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%) !important;
  border: none !important;
  color: #1a1a2e !important;
}

/* 统计卡片 */
.stats-section {
  margin-bottom: 20px;
}

/* 统计卡片 */
.stat-card {
  background: linear-gradient(135deg, rgba(40, 40, 70, 0.9) 0%, rgba(30, 30, 55, 0.9) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 30px rgba(102, 126, 234, 0.25);
}

.stat-card-original {
  cursor: pointer;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  border-radius: 16px !important;
  overflow: hidden;
  position: relative;
  will-change: transform;
}

.mystery-stat-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%) !important;
  border: 1px solid rgba(102, 126, 234, 0.2) !important;
}

.stat-decoration {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 30px;
  opacity: 0.15;
}

/* 统计卡片悬浮效果已在上方定义 */

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px 0;
}

.stat-icon-wrap {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-cases .stat-icon-wrap { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-scripts .stat-icon-wrap { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-clues .stat-icon-wrap { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.stat-items .stat-icon-wrap { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); }

.stat-icon {
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 5px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.stat-decoration {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 24px;
  opacity: 0.3;
}

.stat-cases .stat-value { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; background-clip: text; }
.stat-scripts .stat-value { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); -webkit-background-clip: text; background-clip: text; }
.stat-clues .stat-value { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); -webkit-background-clip: text; background-clip: text; }
.stat-items .stat-value { background: linear-gradient(135deg, #56ab2f 0%, #a8e6cf 100%); -webkit-background-clip: text; background-clip: text; }

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.stat-link {
  width: 100%;
  margin-top: 10px;
  border-top: 1px dashed rgba(255, 255, 255, 0.2);
  padding-top: 10px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-link:hover {
  color: #FFD700;
}

/* 待处理案件 */
.pending-section {
  margin-bottom: 20px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(40, 40, 70, 0.9) 0%, rgba(30, 30, 55, 0.9) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  margin-bottom: 15px;
  position: relative;
  overflow: hidden;
  will-change: transform;
}

.pending-case::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.pending-case-icon {
  position: absolute;
  top: 10px;
  right: 15px;
  font-size: 24px;
  opacity: 0.2;
}

.pending-item:hover {
  transform: translateX(5px) translateZ(0);
  box-shadow: 0 6px 15px rgba(0,0,0,0.08);
}

.pending-icon {
  flex-shrink: 0;
}

.pending-info {
  flex: 1;
}

.pending-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #fff;
}

.pending-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.pending-arrow {
  color: rgba(255, 255, 255, 0.6);
}

.pending-arrow {
  opacity: 0;
  transition: all 0.3s;
  transform: translateX(-10px);
}

.pending-item:hover .pending-arrow {
  opacity: 1;
  transform: translateX(0);
}

.pending-warning::before { background: linear-gradient(180deg, #E6A23C 0%, #F56C6C 100%); }
.pending-success::before { background: linear-gradient(180deg, #67C23A 0%, #409EFF 100%); }
.pending-primary::before { background: linear-gradient(180deg, #409EFF 0%, #764ba2 100%); }

/* 侦探工具箱 */
.quick-actions-section {
  margin-bottom: 20px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 10px;
  background: linear-gradient(135deg, rgba(40, 40, 70, 0.9) 0%, rgba(30, 30, 55, 0.9) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  margin-bottom: 15px;
  will-change: transform;
}

.mystery-action:hover {
  transform: translateY(-3px) translateZ(0);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

.action-icon-wrap {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mystery-action:hover .action-icon-wrap {
  /* 移除缩放动画减少重绘 */
}

.action-emoji {
  font-size: 28px;
}

.action-name {
  font-size: 14px;
  color: #fff;
  text-align: center;
}

/* 侦探勋章 */
.achievements-section {
  margin-bottom: 20px;
}

.achievement-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 15px;
  background: linear-gradient(135deg, rgba(40, 40, 70, 0.7) 0%, rgba(30, 30, 55, 0.7) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  text-align: center;
  margin-bottom: 15px;
  position: relative;
  overflow: hidden;
}

.mystery-achievement {
  opacity: 0.7;
}

.mystery-achievement.achievement-unlocked {
  opacity: 1;
  background: linear-gradient(135deg, rgba(50, 50, 80, 0.95) 0%, rgba(40, 40, 65, 0.95) 100%);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
}

.achievement-badge {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  position: relative;
  background: linear-gradient(135deg, #e0e0e0 0%, #c0c0c0 100%);
}

.achievement-unlocked .achievement-badge {
  box-shadow: 0 0 15px rgba(255, 255, 255, 0.4);
}

.achievement-badge.common { background: linear-gradient(135deg, #909399 0%, #606266 100%); }
.achievement-badge.uncommon { background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%); }
.achievement-badge.rare { background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%); }
.achievement-badge.epic { background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%); }
.achievement-badge.legendary { 
  background: linear-gradient(135deg, #FFD700 0%, #FF6B6B 50%, #9B59B6 100%);
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.4);
}


.badge-icon {
  font-size: 32px;
}

.achievement-name {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 4px;
  color: #fff;
}

.achievement-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.achievement-rarity-tag {
  margin-top: 5px;
}

.rarity-common { color: #909399; font-size: 12px; }
.rarity-uncommon { color: #67C23A; font-size: 12px; font-weight: bold; }
.rarity-rare { color: #409EFF; font-size: 12px; font-weight: bold; }
.rarity-epic { color: #E6A23C; font-size: 12px; font-weight: bold; }
.rarity-legendary { 
  background: linear-gradient(90deg, #FFD700, #FF6B6B, #9B59B6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 12px; 
  font-weight: bold;
}

/* 最近案件档案 */
.recent-cases-section {
  margin-bottom: 20px;
}

.cases-timeline {
  position: relative;
  padding-left: 10px;
}

.case-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  position: relative;
}

.case-date {
  width: 50px;
  text-align: center;
  flex-shrink: 0;
}

.date-day {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  line-height: 1;
}

.date-month {
  display: block;
  font-size: 12px;
  color: rgba(255,255,255,0.7);
}

.case-line {
  width: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.case-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #909399;
  position: relative;
  z-index: 1;
}

.case-dot.completed { background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%); }
.case-dot.upcoming { background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%); }
.case-dot.pending { background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%); }
.case-dot.cancelled { background: #909399; }

.case-line::after {
  content: '';
  flex: 1;
  width: 2px;
  background: rgba(255,255,255,0.2);
  margin-top: 5px;
}

.case-item:last-child .case-line::after {
  display: none;
}

.case-content {
  flex: 1;
  background: linear-gradient(135deg, rgba(40, 40, 70, 0.9) 0%, rgba(30, 30, 55, 0.9) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  padding: 15px;
  border-radius: 12px;
  transition: transform 0.2s ease-out, box-shadow 0.2s ease-out;
  will-change: transform;
}

.case-content:hover {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  transform: translateX(3px) translateZ(0);
}

.case-title {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
}

.case-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.btn-emoji {
  margin-right: 5px;
}

/* VIP套餐 */
.vip-plans {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
}

.vip-plan {
  position: relative;
  padding: 30px 20px;
  border: 2px solid #eee;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.vip-plan:hover {
  border-color: #E6A23C;
  box-shadow: 0 8px 16px rgba(230, 162, 60, 0.2);
}

.vip-plan-selected {
  border-color: #E6A23C;
  background: #FFF9E6;
}

.plan-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.vip-plan h3 {
  margin: 0 0 15px;
  font-size: 20px;
  text-align: center;
}

.plan-price {
  text-align: center;
  margin-bottom: 20px;
}

.plan-price .price {
  font-size: 36px;
  font-weight: bold;
  color: #E6A23C;
}

.plan-price .unit {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.plan-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

/* 响应式 */
@media (max-width: 992px) {
  .role-stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .profile-enhanced {
    padding: 15px;
  }
  
  .mystery-bg .floating-icon {
    font-size: 30px;
  }
  
  .user-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .user-name-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .user-name-row h2 {
    font-size: 22px;
  }
  
  .detective-title {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  
  .exp-bar {
    width: 160px;
  }
  
  .user-meta {
    flex-direction: column;
    gap: 10px;
    justify-content: center;
  }
  
  .user-actions {
    justify-content: center;
  }
  
  .role-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .role-stat-item {
    padding: 12px 8px;
  }
  
  .role-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
  
  .role-info .role-count {
    font-size: 18px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .stat-icon-wrap {
    width: 50px;
    height: 50px;
  }
  
  .stat-icon {
    font-size: 24px !important;
  }
  
  .action-icon-wrap {
    width: 50px;
    height: 50px;
    border-radius: 12px;
  }
  
  .action-emoji {
    font-size: 22px;
  }
  
  .achievement-badge {
    width: 55px;
    height: 55px;
  }
  
  .badge-icon {
    font-size: 24px;
  }
  
  .case-date {
    width: 40px;
  }
  
  .date-day {
    font-size: 18px;
  }
  
  .case-content {
    padding: 12px;
  }
  
  .case-meta {
    flex-direction: column;
    gap: 5px;
  }
  
  .vip-plans {
    grid-template-columns: 1fr;
  }
  
  .pending-item {
    padding: 15px;
  }
  
  .pending-title {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .role-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .role-stat-item:nth-child(5) {
    grid-column: span 2;
  }
  
  .stat-content {
    padding: 15px 0;
  }
  
  .stat-value {
    font-size: 20px;
  }
}
</style>
