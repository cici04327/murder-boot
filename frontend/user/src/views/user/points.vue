<template>
  <div class="points-page">
    <!-- 神秘背景 -->
    <div class="mystery-bg">
      <div class="particles">
        <span v-for="n in 20" :key="n" class="particle"></span>
      </div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <span class="title-icon">🏆</span>
          <div class="title-text">
            <h1>侦探积分</h1>
            <p>收集线索点数，解锁更多推理奖励</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 积分概览 -->
    <div class="points-overview">
      <div class="overview-card">
        <!-- 主积分展示 -->
        <div class="main-points">
          <div class="points-badge">
            <div class="badge-inner">
              <span class="badge-icon">🔍</span>
              <span class="points-number">{{ currentPoints }}</span>
              <span class="points-unit">线索点</span>
            </div>
            <div class="badge-glow"></div>
          </div>
          <div class="level-info">
            <span class="level-badge" :class="levelInfo.class">
              {{ levelInfo.icon }} {{ levelInfo.name }}
            </span>
            <div class="level-progress-bar">
              <div class="progress-fill" :style="{ width: levelInfo.progress + '%' }"></div>
            </div>
            <span class="level-tip">距离下一等级还需 {{ levelInfo.nextLevelPoints }} 积分</span>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="stats-row">
          <div class="stat-card">
            <span class="stat-icon">📥</span>
            <span class="stat-value">{{ totalEarned }}</span>
            <span class="stat-label">累计获得</span>
          </div>
          <div class="stat-card">
            <span class="stat-icon">📤</span>
            <span class="stat-value">{{ totalUsed }}</span>
            <span class="stat-label">已使用</span>
          </div>
          <div class="stat-card warning">
            <span class="stat-icon">⏳</span>
            <span class="stat-value">{{ expiringSoon }}</span>
            <span class="stat-label">即将过期</span>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-actions">
          <button class="action-btn sign-btn" @click="handleSignIn" :disabled="hasSignedToday">
            <span class="btn-icon">{{ hasSignedToday ? '✅' : '📅' }}</span>
            <span class="btn-text">{{ hasSignedToday ? '今日已签到' : '每日签到' }}</span>
            <span v-if="!hasSignedToday" class="btn-reward">+10</span>
          </button>
          <button class="action-btn exchange-btn" @click="showExchangeDialog = true">
            <span class="btn-icon">🎁</span>
            <span class="btn-text">兑换奖励</span>
          </button>
        </div>
      </div>
    </div>


    <!-- 侦探任务 -->
    <div class="tasks-section">
      <div class="section-header">
        <span class="section-icon">🎯</span>
        <span class="section-title">侦探任务</span>
        <span class="section-subtitle">完成任务获取更多线索点</span>
      </div>
      <div class="tasks-grid">
        <div 
          v-for="task in tasks" 
          :key="task.id" 
          class="task-card"
          :class="{ completed: task.completed }"
        >
          <div class="task-header">
            <span class="task-icon">{{ task.emoji }}</span>
            <span v-if="task.completed" class="completed-badge">✓</span>
          </div>
          <div class="task-body">
            <h4 class="task-title">{{ task.title }}</h4>
            <p class="task-desc">{{ task.description }}</p>
          </div>
          <div class="task-footer">
            <span class="task-reward">
              <span class="reward-icon">💎</span>
              +{{ task.points }}
            </span>
            <button 
              v-if="!task.completed"
              class="task-btn"
              @click="handleTaskAction(task)"
            >
              {{ task.actionText }}
            </button>
            <span v-else class="done-text">已完成</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 兑换优惠券对话框 -->
    <el-dialog 
      v-model="showExchangeDialog" 
      title="兑换优惠券" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="exchange-content">
        <div class="current-points-tip">
          当前可用积分：<span class="highlight">{{ currentPoints }}</span>
        </div>
        <el-row :gutter="20">
          <el-col :span="12" v-for="coupon in coupons" :key="coupon.id">
            <div class="coupon-card">
              <div class="coupon-amount">
                <span class="symbol">¥</span>
                <span class="value">{{ coupon.amount }}</span>
              </div>
              <div class="coupon-info">
                <div class="coupon-title">{{ coupon.title }}</div>
                <div class="coupon-desc">{{ coupon.description }}</div>
                <div class="coupon-expire">有效期：{{ coupon.validText || `${coupon.validDays}天` }}</div>
              </div>
              <div class="coupon-footer">
                <el-tag type="warning" size="small">{{ coupon.points }} 积分</el-tag>
                <el-button 
                  type="primary" 
                  size="small"
                  :disabled="currentPoints < coupon.points"
                  @click="handleExchange(coupon)"
                >
                  立即兑换
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <!-- 线索获取规则 -->
    <div class="rules-section">
      <div class="section-header">
        <span class="section-icon">📜</span>
        <span class="section-title">线索获取指南</span>
      </div>
      <div class="rules-grid">
        <div class="rule-card">
          <span class="rule-icon">🎭</span>
          <h4>完成剧本</h4>
          <p>每次完成剧本杀可获得 <strong>100</strong> 线索点</p>
        </div>
        <div class="rule-card">
          <span class="rule-icon">⭐</span>
          <h4>发表评价</h4>
          <p>分享你的推理体验可获得 <strong>50</strong> 线索点</p>
        </div>
        <div class="rule-card">
          <span class="rule-icon">👥</span>
          <h4>邀请侦探</h4>
          <p>邀请好友加入可获得 <strong>200</strong> 线索点</p>
        </div>
        <div class="rule-card">
          <span class="rule-icon">📅</span>
          <h4>每日签到</h4>
          <p>坚持打卡可获得 <strong>10</strong> 线索点</p>
        </div>
      </div>
    </div>

    <!-- 线索记录 -->
    <div class="records-section">
      <div class="section-header">
        <span class="section-icon">📋</span>
        <span class="section-title">线索记录</span>
        <div class="record-filters">
          <button 
            v-for="filter in recordFilters" 
            :key="filter.value"
            :class="['filter-btn', { active: recordType === filter.value }]"
            @click="recordType = filter.value; loadRecords()"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <div class="records-list" v-loading="loading" element-loading-text="搜集线索中...">
        <!-- 空状态 -->
        <div v-if="records.length === 0 && !loading" class="empty-records">
          <span class="empty-icon">🔍</span>
          <p>暂无线索记录</p>
        </div>

        <!-- 记录列表 -->
        <div v-else class="record-items">
          <div 
            v-for="(record, index) in records" 
            :key="record.id" 
            class="record-item"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <div class="record-icon" :class="record.type === 1 ? 'earn' : 'use'">
              {{ record.type === 1 ? '📥' : '📤' }}
            </div>
            <div class="record-content">
              <span class="record-title">{{ record.description }}</span>
              <span class="record-time">{{ formatDateTime(record.createTime) }}</span>
            </div>
            <div class="record-points" :class="record.type === 1 ? 'positive' : 'negative'">
              {{ record.type === 1 ? '+' : '-' }}{{ record.points }}
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 回到顶部 -->
    <el-backtop :right="40" :bottom="40">
      <div class="backtop-btn">🔝</div>
    </el-backtop>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  TrophyBase, 
  CircleCheck, 
  Star, 
  User, 
  Calendar,
  ShoppingCart,
  Ticket,
  ChatDotSquare
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { 
  getUserPoints, 
  getPointsRecords, 
  signIn, 
  exchangeCoupon,
  getTasksStatus
} from '@/api/user'

const router = useRouter()

// 状态
const loading = ref(false)
const currentPoints = ref(0)
const totalEarned = ref(0)
const totalUsed = ref(0)
const expiringSoon = ref(0)

const recordType = ref('all')
const records = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 新增功能状态
const hasSignedToday = ref(false)
const showExchangeDialog = ref(false)

// 可兑换优惠券
const coupons = ref([
  {
    id: 1,
    title: '满100减10',
    description: '订单满100元可用',
    amount: 10,
    points: 100,
    validText: '长期有效'
  },
  {
    id: 2,
    title: '满200减25',
    description: '订单满200元可用',
    amount: 25,
    points: 250,
    validText: '长期有效'
  },
  {
    id: 3,
    title: '满300减40',
    description: '订单满300元可用',
    amount: 40,
    points: 400,
    validText: '长期有效'
  },
  {
    id: 4,
    title: '满500减80',
    description: '订单满500元可用',
    amount: 80,
    points: 800,
    validText: '长期有效'
  }
])

// 记录筛选选项
const recordFilters = [
  { value: 'all', label: '全部' },
  { value: 'earned', label: '获得' },
  { value: 'used', label: '使用' }
]

// 侦探等级
const levelInfo = computed(() => {
  const points = currentPoints.value
  if (points < 100) {
    return { name: '见习侦探', icon: '🔰', class: 'level-bronze', nextLevelPoints: 100 - points, progress: points }
  } else if (points < 500) {
    return { name: '初级侦探', icon: '🥉', class: 'level-silver', nextLevelPoints: 500 - points, progress: ((points - 100) / 400) * 100 }
  } else if (points < 1000) {
    return { name: '资深侦探', icon: '🥈', class: 'level-gold', nextLevelPoints: 1000 - points, progress: ((points - 500) / 500) * 100 }
  } else if (points < 5000) {
    return { name: '王牌侦探', icon: '🥇', class: 'level-platinum', nextLevelPoints: 5000 - points, progress: ((points - 1000) / 4000) * 100 }
  } else {
    return { name: '传奇侦探', icon: '👑', class: 'level-diamond', nextLevelPoints: 0, progress: 100 }
  }
})

// 侦探任务
const tasks = ref([
  {
    id: 1,
    title: '参与剧本',
    description: '预约并完成一次剧本杀',
    points: 100,
    emoji: '🎭',
    completed: false,
    actionText: '去预约'
  },
  {
    id: 2,
    title: '分享心得',
    description: '为剧本或门店撰写评价',
    points: 50,
    emoji: '✍️',
    completed: false,
    actionText: '去评价'
  },
  {
    id: 3,
    title: '侦探档案',
    description: '完善你的个人资料',
    points: 30,
    emoji: '📝',
    completed: false,
    actionText: '去完善'
  },
  {
    id: 4,
    title: '每日打卡',
    description: '坚持签到收集线索',
    points: 10,
    emoji: '📅',
    completed: false,
    actionText: '签到'
  },
  {
    id: 5,
    title: '招募同伴',
    description: '邀请好友一起推理',
    points: 200,
    emoji: '👥',
    completed: false,
    actionText: '去邀请'
  },
  {
    id: 6,
    title: '收藏剧本',
    description: '收藏5个感兴趣的剧本',
    points: 20,
    emoji: '❤️',
    completed: false,
    actionText: '去收藏'
  }
])

// 加载积分信息
const loadPoints = async () => {
  try {
    console.log('开始加载积分信息...')
    const res = await getUserPoints()
    console.log('积分信息响应:', res)
    if (res.code === 1 || res.code === 200) {
      const newPoints = res.data.currentPoints || 0
      console.log('更新积分:', currentPoints.value, '->', newPoints)
      currentPoints.value = newPoints
      totalEarned.value = res.data.totalEarned || 0
      totalUsed.value = res.data.totalUsed || 0
      expiringSoon.value = res.data.expiringSoon || 0
    } else {
      console.error('获取积分失败:', res)
    }
  } catch (error) {
    console.error('加载积分信息失败:', error)
    ElMessage.error('加载积分信息失败')
  }
}

// 分页处理函数
const handlePageChange = (newPage) => {
  console.log('积分记录页码变化:', newPage)
  currentPage.value = newPage
  loadRecords()
}

const handleSizeChange = (newSize) => {
  console.log('积分记录每页大小变化:', newSize)
  pageSize.value = newSize
  currentPage.value = 1
  loadRecords()
}

// 加载积分记录
const loadRecords = async () => {
  loading.value = true
  try {
    console.log('加载积分记录，参数:', { page: currentPage.value, pageSize: pageSize.value, type: recordType.value })
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    
    // 根据筛选类型添加参数
    if (recordType.value === 'earned') {
      params.type = 1  // 获得
    } else if (recordType.value === 'used') {
      params.type = 2  // 使用
    }
    
    const res = await getPointsRecords(params)
    if (res.code === 1 || res.code === 200) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载积分记录失败:', error)
    ElMessage.error('加载积分记录失败')
  } finally {
    loading.value = false
  }
}

// 获取记录图标
const getRecordIcon = (type) => {
  // type: 1=增加, 2=减少
  if (type === 1) {
    return CircleCheck  // 绿色勾号 - 获得积分
  } else if (type === 2) {
    return ShoppingCart  // 购物车 - 使用积分
  }
  return Star  // 默认星星
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 签到功能
const handleSignIn = async () => {
  if (hasSignedToday.value) {
    ElMessage.warning('今日已签到')
    return
  }
  
  try {
    console.log('开始签到...')
    const res = await signIn()
    console.log('签到响应:', res)
    
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('签到成功！获得10积分')
      hasSignedToday.value = true
      
      // 更新任务4的状态
      const signInTask = tasks.value.find(t => t.id === 4)
      if (signInTask) {
        signInTask.completed = true
      }
      
      // 延迟刷新，确保后端数据已更新
      console.log('延迟500ms后刷新数据...')
      setTimeout(async () => {
        await loadPoints()
        await loadRecords()
        console.log('数据刷新完成')
      }, 500)
      
    } else {
      ElMessage.error(res.msg || '签到失败')
      // 如果后端返回"今日已签到"，更新前端状态
      if (res.msg && res.msg.includes('已签到')) {
        hasSignedToday.value = true
        const signInTask = tasks.value.find(t => t.id === 4)
        if (signInTask) {
          signInTask.completed = true
        }
        // 即使已签到，也刷新一次数据
        await loadPoints()
        await loadRecords()
      }
    }
  } catch (error) {
    console.error('签到失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '签到失败'
    ElMessage.error(errorMsg)
    
    // 如果后端返回"今日已签到"，更新前端状态
    if (errorMsg.includes('已签到')) {
      hasSignedToday.value = true
      const signInTask = tasks.value.find(t => t.id === 4)
      if (signInTask) {
        signInTask.completed = true
      }
      // 即使已签到，也刷新一次数据
      await loadPoints()
      await loadRecords()
    }
  }
}


// 处理任务动作
const handleTaskAction = async (task) => {
  switch (task.id) {
    case 1: // 完成预约
      ElMessage.info('前往预约剧本')
      router.push('/script')
      break
      
    case 2: // 发表评价
      ElMessage.info('请先完成预约，然后进行评价')
      router.push('/user/reservations')
      break
      
    case 3: // 完善资料
      // 检查是否已完善资料
      await handleCompleteProfile()
      break
      
    case 4: // 每日签到
      handleSignIn()
      break
      
    case 5: // 邀请好友
      handleInviteFriend()
      break
      
    case 6: // 收藏剧本
      // 检查收藏数量
      await handleCheckFavorite()
      break
  }
}

// 处理完善资料任务
const handleCompleteProfile = async () => {
  try {
    const res = await completeProfileTask()
    
    if (res.code === 1 || res.code === 200) {
      if (res.data?.alreadyCompleted) {
        ElMessage.info('您已完成过该任务')
        const task = tasks.value.find(t => t.id === 3)
        if (task) task.completed = true
      } else if (res.data?.completed) {
        ElMessage.success('完善资料任务已完成！获得30积分')
        const task = tasks.value.find(t => t.id === 3)
        if (task) task.completed = true
        
        // 刷新数据
        setTimeout(async () => {
          await loadPoints()
          await loadRecords()
        }, 500)
      } else {
        // 资料未完善，引导用户前往完善
        ElMessage.info('请前往个人中心完善资料')
        router.push('/user/profile')
      }
    }
  } catch (error) {
    console.error('完善资料任务失败:', error)
    ElMessage.info('前往个人资料页面完善信息')
    router.push('/user/profile')
  }
}

// 检查收藏任务
const handleCheckFavorite = async () => {
  try {
    const res = await checkFavoriteTask()
    
    if (res.code === 1 || res.code === 200) {
      if (res.data?.alreadyCompleted) {
        ElMessage.info('您已完成过该任务')
        const task = tasks.value.find(t => t.id === 6)
        if (task) task.completed = true
      } else if (res.data?.completed) {
        ElMessage.success(`收藏任务已完成！获得20积分`)
        const task = tasks.value.find(t => t.id === 6)
        if (task) task.completed = true
        
        // 刷新数据
        setTimeout(async () => {
          await loadPoints()
          await loadRecords()
        }, 500)
      } else {
        // 未达到收藏条件
        const current = res.data?.currentFavorites || 0
        const required = res.data?.requiredFavorites || 5
        ElMessage.info(`您已收藏${current}个剧本，还需收藏${required - current}个`)
        router.push('/script')
      }
    }
  } catch (error) {
    console.error('检查收藏任务失败:', error)
    ElMessage.info('前往剧本页面收藏喜欢的剧本')
    router.push('/script')
  }
}

// 邀请好友功能
const handleInviteFriend = async () => {
  try {
    // 获取或生成邀请码
    const inviteRes = await getInviteInfo()
    let inviteCode = ''
    
    if (inviteRes.code === 1 || inviteRes.code === 200) {
      inviteCode = inviteRes.data?.inviteCode
      
      // 如果没有邀请码，生成一个
      if (!inviteCode) {
        const generateRes = await generateInviteCode()
        if (generateRes.code === 1 || generateRes.code === 200) {
          inviteCode = generateRes.data?.inviteCode
        }
      }
    }
    
    if (inviteCode) {
      const inviteUrl = `${window.location.origin}/register?inviteCode=${inviteCode}`
      
      // 复制邀请链接
      try {
        await navigator.clipboard.writeText(inviteUrl)
        ElMessage.success({
          message: `邀请链接已复制！每成功邀请一位好友可获得200积分`,
          duration: 5000
        })
      } catch (e) {
        // 如果复制失败，显示链接
        ElMessage.success({
          message: `您的邀请码：${inviteCode}<br>邀请链接：${inviteUrl}`,
          duration: 10000,
          dangerouslyUseHTMLString: true
        })
      }
    } else {
      ElMessage.error('获取邀请码失败，请稍后重试')
    }
  } catch (error) {
    console.error('生成邀请链接失败:', error)
    ElMessage.error('生成邀请链接失败，请稍后重试')
  }
}

// 兑换优惠券
const handleExchange = async (coupon) => {
  if (currentPoints.value < coupon.points) {
    ElMessage.warning('积分不足，无法兑换')
    return
  }
  
  try {
    const res = await exchangeCoupon(coupon.id, coupon.points)
    if (res.code === 1) {
      ElMessage.success(`成功兑换${coupon.title}！消耗${coupon.points}积分`)
      showExchangeDialog.value = false
      loadPoints()
      loadRecords()
    } else {
      ElMessage.error(res.msg || '兑换失败')
    }
  } catch (error) {
    console.error('兑换失败:', error)
    ElMessage.error(error.response?.data?.msg || '兑换失败，请重试')
  }
}

// 检查任务完成状态
const checkAllTasksStatus = async () => {
  try {
    const res = await getTasksStatus()
    console.log('任务状态响应:', res)
    
    if (res.code === 1 || res.code === 200) {
      const taskStatus = res.data || {}
      
      // 更新各任务状态
      tasks.value.forEach(task => {
        switch (task.id) {
          case 1: // 完成预约
            task.completed = taskStatus.hasReservation || false
            break
          case 2: // 发表评价
            task.completed = taskStatus.hasReview || false
            break
          case 3: // 完善资料
            task.completed = taskStatus.hasCompleteProfile || false
            break
          case 4: // 每日签到
            task.completed = taskStatus.hasSignedToday || false
            hasSignedToday.value = taskStatus.hasSignedToday || false
            break
          case 5: // 邀请好友
            task.completed = taskStatus.hasInviteFriend || false
            break
          case 6: // 收藏剧本
            task.completed = taskStatus.hasFavoriteScript || false
            break
        }
      })
      
      console.log('任务状态更新完成:', tasks.value)
    }
  } catch (error) {
    console.error('检查任务状态失败:', error)
    // 如果检查失败，尝试从记录中检查签到状态
    await checkSignInStatusFromRecords()
  }
}

// 从记录中检查签到状态（备用方法）
const checkSignInStatusFromRecords = async () => {
  try {
    const res = await getPointsRecords({ page: 1, pageSize: 10, type: 1 })
    if (res.code === 1 || res.code === 200) {
      const records = res.data.records || []
      const today = new Date().toDateString()
      
      const hasSignedIn = records.some(record => {
        const recordDate = new Date(record.createTime).toDateString()
        return recordDate === today && record.description && record.description.includes('签到')
      })
      
      hasSignedToday.value = hasSignedIn
      const signInTask = tasks.value.find(t => t.id === 4)
      if (signInTask) {
        signInTask.completed = hasSignedIn
      }
    }
  } catch (error) {
    console.error('检查签到状态失败:', error)
  }
}

// 初始化
onMounted(async () => {
  await loadPoints()
  await loadRecords()
  await checkAllTasksStatus()
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.points-page {
  min-height: calc(100vh - 64px - 100px);
  background: transparent;
  padding: 20px;
  position: relative;
  overflow-x: hidden;
}

/* ========== 神秘背景 ========== */
.mystery-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 215, 0, 0.3);
  border-radius: 50%;
  animation: particleFloat 15s infinite;
}

.particle:nth-child(1) { top: 10%; left: 20%; animation-delay: 0s; }
.particle:nth-child(2) { top: 20%; left: 80%; animation-delay: 1s; }
.particle:nth-child(3) { top: 30%; left: 40%; animation-delay: 2s; }
.particle:nth-child(4) { top: 40%; left: 60%; animation-delay: 3s; }
.particle:nth-child(5) { top: 50%; left: 10%; animation-delay: 4s; }
.particle:nth-child(6) { top: 60%; left: 90%; animation-delay: 5s; }
.particle:nth-child(7) { top: 70%; left: 30%; animation-delay: 6s; }
.particle:nth-child(8) { top: 80%; left: 70%; animation-delay: 7s; }
.particle:nth-child(9) { top: 15%; left: 50%; animation-delay: 8s; }
.particle:nth-child(10) { top: 25%; left: 15%; animation-delay: 9s; }
.particle:nth-child(11) { top: 35%; left: 85%; animation-delay: 10s; }
.particle:nth-child(12) { top: 45%; left: 25%; animation-delay: 11s; }
.particle:nth-child(13) { top: 55%; left: 75%; animation-delay: 12s; }
.particle:nth-child(14) { top: 65%; left: 5%; animation-delay: 13s; }
.particle:nth-child(15) { top: 75%; left: 95%; animation-delay: 14s; }
.particle:nth-child(16) { top: 85%; left: 45%; animation-delay: 0.5s; }
.particle:nth-child(17) { top: 5%; left: 55%; animation-delay: 1.5s; }
.particle:nth-child(18) { top: 90%; left: 35%; animation-delay: 2.5s; }
.particle:nth-child(19) { top: 12%; left: 65%; animation-delay: 3.5s; }
.particle:nth-child(20) { top: 78%; left: 12%; animation-delay: 4.5s; }

@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.3; }
  50% { transform: translateY(-30px) scale(1.5); opacity: 0.8; }
}

/* ========== 页面头部 ========== */
.page-header {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  font-size: 48px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.title-text h1 {
  margin: 0;
  font-size: 28px;
  color: #fff;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
}

.title-text p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

/* ========== 积分概览 ========== */
.points-overview {
  position: relative;
  z-index: 1;
  margin-bottom: 20px;
}

.overview-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 主积分展示 */
.main-points {
  display: flex;
  align-items: center;
  gap: 40px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.points-badge {
  position: relative;
  width: 180px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 160px;
  background: linear-gradient(135deg, #2d2d44 0%, #1a1a2e 100%);
  border-radius: 50%;
  border: 3px solid rgba(255, 215, 0, 0.5);
  position: relative;
  z-index: 1;
}

.badge-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.points-number {
  font-size: 42px;
  font-weight: bold;
  color: #ffd700;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.5);
}

.points-unit {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.badge-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 215, 0, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  animation: glow 3s infinite;
}

@keyframes glow {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.5; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.8; }
}

.level-info {
  flex: 1;
  min-width: 200px;
}

.level-badge {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
}

.level-bronze { background: linear-gradient(135deg, #cd7f32 0%, #8b4513 100%); color: #fff; }
.level-silver { background: linear-gradient(135deg, #c0c0c0 0%, #808080 100%); color: #fff; }
.level-gold { background: linear-gradient(135deg, #ffd700 0%, #daa520 100%); color: #1a1a2e; }
.level-platinum { background: linear-gradient(135deg, #e5e4e2 0%, #a0a0a0 100%); color: #1a1a2e; }
.level-diamond { background: linear-gradient(135deg, #b9f2ff 0%, #00ced1 100%); color: #1a1a2e; }

.level-progress-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  margin-bottom: 8px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #c0392b 0%, #ffd700 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.level-tip {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 统计数据 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-3px);
}

.stat-card.warning {
  border-color: rgba(255, 107, 107, 0.3);
}

.stat-icon {
  font-size: 28px;
  display: block;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  display: block;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
  display: block;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  min-width: 150px;
  padding: 14px 24px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.3s;
}

.sign-btn {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
}

.sign-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(192, 57, 43, 0.4);
}

.sign-btn:disabled {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
}

.exchange-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.exchange-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(245, 87, 108, 0.4);
}

.btn-icon {
  font-size: 20px;
}

.btn-text {
  font-size: 15px;
}

.btn-reward {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* ========== 区域通用样式 ========== */
.tasks-section,
.rules-section,
.records-section {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.section-icon {
  font-size: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
}

.section-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: auto;
}

/* ========== 侦探任务 ========== */
.tasks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.task-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.task-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  border-color: rgba(192, 57, 43, 0.5);
}

.task-card.completed {
  background: rgba(46, 213, 115, 0.1);
  border-color: rgba(46, 213, 115, 0.3);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-icon {
  font-size: 36px;
}

.completed-badge {
  background: #2ed573;
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.task-body {
  margin-bottom: 16px;
}

.task-title {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  margin: 0 0 8px 0;
}

.task-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  line-height: 1.4;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-reward {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ffd700;
  font-weight: bold;
  font-size: 14px;
}

.reward-icon {
  font-size: 16px;
}

.task-btn {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.task-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.4);
}

.done-text {
  color: #2ed573;
  font-size: 13px;
  font-weight: bold;
}

/* ========== 兑换对话框 ========== */
.exchange-content {
  padding: 10px 0;
}

.current-points-tip {
  text-align: center;
  font-size: 16px;
  margin-bottom: 25px;
  padding: 15px;
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-radius: 12px;
  color: #fff;
}

.current-points-tip .highlight {
  font-size: 32px;
  font-weight: bold;
  color: #ffd700;
  margin-left: 10px;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
}

.coupon-card {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
}

.coupon-card:hover {
  border-color: #c0392b;
  box-shadow: 0 4px 20px rgba(192, 57, 43, 0.3);
  transform: translateY(-3px);
}

.coupon-amount {
  text-align: center;
  margin-bottom: 15px;
  color: #ff6b6b;
}

.coupon-amount .symbol {
  font-size: 24px;
  font-weight: bold;
}

.coupon-amount .value {
  font-size: 48px;
  font-weight: bold;
}

.coupon-info {
  text-align: center;
  margin-bottom: 15px;
}

.coupon-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.coupon-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}

.coupon-expire {
  font-size: 12px;
  color: #909399;
}

.coupon-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

/* ========== 线索获取规则 ========== */
.rules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.rule-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
}

.rule-card:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-5px);
  border-color: rgba(192, 57, 43, 0.3);
}

.rule-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
}

.rule-card h4 {
  color: #fff;
  font-size: 16px;
  margin: 0 0 8px 0;
}

.rule-card p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  margin: 0;
  line-height: 1.5;
}

.rule-card strong {
  color: #ffd700;
  font-size: 18px;
}

/* ========== 线索记录 ========== */
.record-filters {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.filter-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.filter-btn.active {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-color: transparent;
  color: #fff;
}

.records-list {
  min-height: 300px;
}

.records-list :deep(.el-loading-mask) {
  background: rgba(26, 26, 46, 0.8);
}

.empty-records {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-records p {
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.record-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s;
  animation: recordAppear 0.3s ease-out backwards;
}

@keyframes recordAppear {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.record-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
}

.record-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 16px;
}

.record-icon.earn {
  background: rgba(46, 213, 115, 0.15);
}

.record-icon.use {
  background: rgba(255, 107, 107, 0.15);
}

.record-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-title {
  font-size: 14px;
  color: #fff;
}

.record-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.record-points {
  font-size: 20px;
  font-weight: bold;
  min-width: 80px;
  text-align: right;
}

.record-points.positive {
  color: #2ed573;
}

.record-points.negative {
  color: #ff6b6b;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 24px;
  margin-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: rgba(255, 255, 255, 0.7);
  --el-pagination-button-disabled-bg-color: transparent;
}

.pagination-wrapper :deep(.el-pager li) {
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  color: #fff;
}

.pagination-wrapper :deep(.el-pagination button) {
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
}

.pagination-wrapper :deep(.el-pagination .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

.pagination-wrapper :deep(.el-pagination .el-input__inner) {
  color: #fff;
}

/* ========== 回到顶部 ========== */
.backtop-btn {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #c0392b 0%, #7a1d1d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 15px rgba(192, 57, 43, 0.4);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .points-page {
    padding: 12px;
  }

  .page-header {
    padding: 16px;
  }

  .title-icon {
    font-size: 36px;
  }

  .title-text h1 {
    font-size: 22px;
  }

  .overview-card {
    padding: 20px;
  }

  .main-points {
    flex-direction: column;
    gap: 24px;
  }

  .points-badge {
    width: 140px;
    height: 140px;
  }

  .badge-inner {
    width: 120px;
    height: 120px;
  }

  .points-number {
    font-size: 32px;
  }

  .stats-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .quick-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }

  .tasks-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .rules-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .record-filters {
    margin-left: 0;
    width: 100%;
    justify-content: flex-start;
  }

  .section-subtitle {
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .tasks-grid,
  .rules-grid {
    grid-template-columns: 1fr;
  }

  .record-item {
    padding: 12px;
  }

  .record-points {
    font-size: 16px;
    min-width: 60px;
  }
}
</style>
