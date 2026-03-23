<template>
  <div class="workbench-page" v-loading="loading">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Store Workbench</p>
        <h1>今日工作台</h1>
        <p class="hero-desc">
          聚合今天最需要处理的预约、退款、评价和通知，让门店登录后直接开始工作。
        </p>
      </div>
      <div class="hero-meta">
        <div class="hero-date">{{ todayLabel }}</div>
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </section>

    <section class="summary-grid">
      <article
        v-for="card in summaryCards"
        :key="card.key"
        class="summary-card"
        :class="card.tone"
        @click="goTo(card.route)"
      >
        <div class="card-top">
          <span class="card-title">{{ card.title }}</span>
          <el-icon class="card-icon">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <div class="card-value">{{ card.value }}</div>
        <div class="card-foot">{{ card.description }}</div>
      </article>
    </section>

    <section class="content-grid">
      <el-card class="panel quick-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>快捷入口</span>
          </div>
        </template>
        <div class="quick-actions">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="quick-action"
            type="button"
            @click="goTo(action.route)"
          >
            <el-icon class="quick-icon">
              <component :is="action.icon" />
            </el-icon>
            <div>
              <div class="quick-title">{{ action.label }}</div>
              <div class="quick-text">{{ action.text }}</div>
            </div>
          </button>
        </div>
      </el-card>

      <el-card class="panel room-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>今日房间使用情况</span>
            <span class="panel-extra">
              {{ effectiveStoreId ? `${roomUsage.length} 个房间` : '请选择具体门店后查看' }}
            </span>
          </div>
        </template>
        <div v-if="roomUsage.length" class="room-list">
          <div v-for="room in roomUsage" :key="room.roomName" class="room-item">
            <div class="room-main">
              <div>
                <div class="room-name">{{ room.roomName }}</div>
                <div class="room-meta">
                  {{ room.firstStart }} - {{ room.lastEnd }} · {{ room.totalSessions }} 场
                </div>
              </div>
              <el-tag :type="room.fullSessions > 0 ? 'danger' : 'success'" effect="light">
                满场 {{ room.fullSessions }}
              </el-tag>
            </div>
            <el-progress
              :percentage="room.utilization"
              :stroke-width="10"
              :show-text="false"
              :color="room.utilization >= 80 ? '#e67e22' : '#2d6a4f'"
            />
            <div class="room-foot">
              <span>已占用 {{ room.bookedSessions }} 场</span>
              <span>空闲 {{ room.totalSessions - room.bookedSessions }} 场</span>
            </div>
          </div>
        </div>
        <el-empty
          v-else
          :description="effectiveStoreId ? '今天暂无排期' : '请先在顶部选择具体门店'"
        />
      </el-card>
    </section>

    <section class="timeline-section">
      <el-card class="panel timeline-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>今日场次时间线</span>
            <span class="panel-extra">
              {{ effectiveStoreId ? `${scheduleTimeline.length} 场` : '请选择具体门店后查看' }}
            </span>
          </div>
        </template>
        <div v-if="scheduleTimeline.length" class="timeline-list">
          <div
            v-for="item in scheduleTimeline"
            :key="`${item.id}-${item.startTime}`"
            class="timeline-item"
          >
            <div class="timeline-time">
              <div class="time-start">{{ formatClock(item.startTime) }}</div>
              <div class="time-end">{{ formatClock(item.endTime) }}</div>
            </div>
            <div class="timeline-line">
              <span class="timeline-dot"></span>
            </div>
            <div class="timeline-content">
              <div class="timeline-head">
                <div>
                  <div class="timeline-title">{{ item.scriptName || '未命名场次' }}</div>
                  <div class="timeline-meta">
                    {{ item.roomName || '未分配房间' }} · {{ item.currentPlayers || 0 }}/{{ item.maxPlayers || 0 }} 人
                  </div>
                </div>
                <el-tag :type="getScheduleTagType(item)" effect="light">
                  {{ getScheduleLabel(item) }}
                </el-tag>
              </div>
              <div v-if="item.remark" class="timeline-remark">{{ item.remark }}</div>
            </div>
          </div>
        </div>
        <el-empty
          v-else
          :description="effectiveStoreId ? '今天暂无场次' : '请先在顶部选择具体门店'"
        />
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  ChatDotRound,
  Clock,
  DataBoard,
  Money,
  Refresh,
  Tickets
} from '@element-plus/icons-vue'
import request, { userService } from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const scheduleTimeline = ref([])
const roomUsage = ref([])

const loginType = localStorage.getItem('admin-login-type') || 'admin'
const storeAdminStoreId = localStorage.getItem('admin-store-id')
const effectiveStoreId = computed(() => {
  if (loginType === 'store') {
    return storeAdminStoreId || ''
  }
  return ''
})

const summary = reactive({
  todayReservations: 0,
  pendingArrival: 0,
  pendingRefunds: 0,
  pendingReplies: 0,
  unreadNotifications: 0
})

const today = new Date()
const todayStr = formatDate(today)
const todayLabel = computed(() =>
  today.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
)

const summaryCards = computed(() => [
  {
    key: 'todayReservations',
    title: '今天预约数',
    value: summary.todayReservations,
    description: '查看今天全部预约安排',
    route: '/reservation/list',
    icon: Calendar,
    tone: 'tone-blue'
  },
  {
    key: 'pendingArrival',
    title: '待到店',
    value: summary.pendingArrival,
    description: '已支付但尚未完成的今日预约',
    route: '/reservation/list',
    icon: Clock,
    tone: 'tone-green'
  },
  {
    key: 'pendingRefunds',
    title: '待退款',
    value: summary.pendingRefunds,
    description: '需要尽快处理的退款申请',
    route: '/reservation/refund',
    icon: Money,
    tone: 'tone-orange'
  },
  {
    key: 'pendingReplies',
    title: '待回复评价',
    value: summary.pendingReplies,
    description: '尚未回复的门店评价',
    route: '/store/review',
    icon: ChatDotRound,
    tone: 'tone-red'
  },
  {
    key: 'unreadNotifications',
    title: '未读通知',
    value: summary.unreadNotifications,
    description: '系统和门店消息待查看',
    route: '/notification/index',
    icon: Bell,
    tone: 'tone-dark'
  }
])

const quickActions = computed(() => {
  const actions = [
    {
      label: '预约列表',
      text: '处理今天订单与到店状态',
      route: '/reservation/list',
      icon: Tickets
    },
    {
      label: '退款管理',
      text: '优先处理退款申请',
      route: '/reservation/refund',
      icon: Money
    },
    {
      label: '评价回复',
      text: '及时跟进用户反馈',
      route: '/store/review',
      icon: ChatDotRound
    },
    {
      label: '排期管理',
      text: '查看并调整今日场次',
      route: '/script/schedule',
      icon: DataBoard
    }
  ]

  return loginType === 'store'
    ? actions
    : actions.filter(action => action.route !== '/script/schedule')
})

function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function isSameDay(value) {
  if (!value) return false
  return String(value).slice(0, 10) === todayStr
}

function formatClock(value) {
  if (!value) return '--:--'
  return String(value).slice(0, 5)
}

function getScheduleTagType(item) {
  if ((item.currentPlayers || 0) >= (item.maxPlayers || 0) && (item.maxPlayers || 0) > 0) {
    return 'danger'
  }
  if ((item.currentPlayers || 0) > 0) {
    return 'warning'
  }
  return item.status === 2 ? 'info' : 'success'
}

function getScheduleLabel(item) {
  if ((item.currentPlayers || 0) >= (item.maxPlayers || 0) && (item.maxPlayers || 0) > 0) {
    return '已满'
  }
  if ((item.currentPlayers || 0) > 0) {
    return '已预订'
  }
  if (item.status === 2) {
    return '已关闭'
  }
  return '可预约'
}

async function fetchAllRecords(url, baseParams = {}, maxPages = 20) {
  const pageSize = 100
  let page = 1
  let total = 0
  const records = []

  while (page <= maxPages) {
    const res = await request.get(url, {
      params: {
        ...baseParams,
        page,
        pageSize
      }
    })

    const payload = res.data || {}
    const pageRecords = payload.records || []
    total = payload.total || 0
    records.push(...pageRecords)

    if (!pageRecords.length || records.length >= total) {
      break
    }

    page += 1
  }

  return { records, total }
}

function buildRoomUsage(schedules) {
  const grouped = schedules.reduce((acc, item) => {
    const key = item.roomName || `房间-${item.roomId || 'unknown'}`
    if (!acc[key]) {
      acc[key] = {
        roomName: key,
        totalSessions: 0,
        bookedSessions: 0,
        fullSessions: 0,
        firstStart: formatClock(item.startTime),
        lastEnd: formatClock(item.endTime)
      }
    }

    const room = acc[key]
    room.totalSessions += 1
    if ((item.currentPlayers || 0) > 0) {
      room.bookedSessions += 1
    }
    if ((item.currentPlayers || 0) >= (item.maxPlayers || 0) && (item.maxPlayers || 0) > 0) {
      room.fullSessions += 1
    }
    room.firstStart = room.firstStart < formatClock(item.startTime) ? room.firstStart : formatClock(item.startTime)
    room.lastEnd = room.lastEnd > formatClock(item.endTime) ? room.lastEnd : formatClock(item.endTime)
    return acc
  }, {})

  return Object.values(grouped)
    .map(room => ({
      ...room,
      utilization: room.totalSessions ? Math.round((room.bookedSessions / room.totalSessions) * 100) : 0
    }))
    .sort((a, b) => a.roomName.localeCompare(b.roomName, 'zh-CN'))
}

async function loadSummary() {
  const requests = [
    fetchAllRecords('/reservation/page'),
    request.get('/reservation/page', { params: { page: 1, pageSize: 1, refundStatus: 1 } }),
    fetchAllRecords('/store/review/page'),
    userService.get('/admin/notification/unread-count')
  ]

  if (effectiveStoreId.value) {
    requests.push(request.get('/script/schedule/list', { params: { date: todayStr } }))
  }

  const responseList = await Promise.all(requests)
  const [{ records: reservations }, refundRes, { records: reviews }, unreadRes, scheduleRes] = responseList

  const createdTodayReservations = reservations.filter(item => isSameDay(item.createTime))
  const todayReservations = reservations.filter(item => isSameDay(item.reservationTime))
  summary.todayReservations = createdTodayReservations.length
  summary.pendingArrival = todayReservations.filter(item =>
    Number(item.payStatus) === 1
      && Number(item.status) === 2
      && Number(item.checkInStatus || 0) !== 1
  ).length
  summary.pendingRefunds = refundRes.data?.total || 0
  summary.pendingReplies = reviews.filter(item => !item.reply || !String(item.reply).trim()).length
  summary.unreadNotifications = unreadRes.data || 0

  const schedules = scheduleRes?.data || []
  scheduleTimeline.value = schedules.slice().sort((a, b) => {
    const left = `${a.scheduleDate || ''} ${a.startTime || ''}`
    const right = `${b.scheduleDate || ''} ${b.startTime || ''}`
    return left.localeCompare(right)
  })
  roomUsage.value = buildRoomUsage(scheduleTimeline.value)
}

function goTo(path) {
  router.push(path)
}

async function refreshData() {
  loading.value = true
  try {
    await loadSummary()
  } finally {
    loading.value = false
  }
}

function handleWindowFocus() {
  refreshData()
}

function handleVisibilityChange() {
  if (!document.hidden) {
    refreshData()
  }
}

onMounted(() => {
  refreshData()
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.workbench-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 214, 102, 0.35), transparent 28%),
    linear-gradient(135deg, #133c55 0%, #1f6f78 55%, #2d936c 100%);
  color: #fff;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.8;
}

.hero-card h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 620px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  min-width: 180px;
}

.hero-date {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  border: 0;
  border-radius: 20px;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  color: #fff;
  box-shadow: 0 14px 30px rgba(19, 60, 85, 0.12);
}

.summary-card:hover {
  transform: translateY(-4px);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 14px;
  opacity: 0.92;
}

.card-icon {
  font-size: 20px;
}

.card-value {
  margin-top: 18px;
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.card-foot {
  margin-top: 10px;
  font-size: 13px;
  opacity: 0.88;
  line-height: 1.6;
}

.tone-blue {
  background: linear-gradient(135deg, #20639b, #3caea3);
}

.tone-green {
  background: linear-gradient(135deg, #2d6a4f, #52b788);
}

.tone-orange {
  background: linear-gradient(135deg, #bc6c25, #dda15e);
}

.tone-red {
  background: linear-gradient(135deg, #9d0208, #dc2f02);
}

.tone-dark {
  background: linear-gradient(135deg, #283618, #606c38);
}

.content-grid {
  display: grid;
  grid-template-columns: 1.05fr 1.35fr;
  gap: 20px;
}

.panel {
  border: none;
  border-radius: 20px;
}

:deep(.panel .el-card__header) {
  border-bottom: 1px solid #eef2f4;
  padding-bottom: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 17px;
  font-weight: 600;
  color: #133c55;
}

.panel-extra {
  font-size: 13px;
  font-weight: 500;
  color: #6b7c85;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-action {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  width: 100%;
  padding: 16px;
  border: 1px solid #e7edef;
  border-radius: 16px;
  background: #fbfdfd;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.quick-action:hover {
  border-color: #2d936c;
  transform: translateY(-2px);
}

.quick-icon {
  margin-top: 2px;
  font-size: 20px;
  color: #2d936c;
}

.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #17324d;
}

.quick-text {
  margin-top: 6px;
  font-size: 13px;
  color: #667085;
  line-height: 1.6;
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.room-item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fbfb;
  border: 1px solid #edf3f3;
}

.room-main {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.room-name {
  font-size: 15px;
  font-weight: 600;
  color: #17324d;
}

.room-meta,
.room-foot {
  color: #667085;
  font-size: 13px;
}

.room-meta {
  margin-top: 4px;
}

.room-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.timeline-section {
  display: block;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 88px 20px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.timeline-time {
  padding-top: 2px;
  text-align: right;
}

.time-start {
  font-size: 15px;
  font-weight: 700;
  color: #17324d;
}

.time-end {
  margin-top: 4px;
  font-size: 12px;
  color: #667085;
}

.timeline-line {
  display: flex;
  justify-content: center;
  position: relative;
  min-height: 100%;
}

.timeline-line::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: -20px;
  left: 50%;
  width: 2px;
  background: linear-gradient(180deg, #2d936c 0%, #d8ece3 100%);
  transform: translateX(-50%);
}

.timeline-item:last-child .timeline-line::before {
  bottom: 0;
}

.timeline-dot {
  position: relative;
  z-index: 1;
  margin-top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2d936c;
  box-shadow: 0 0 0 4px rgba(45, 147, 108, 0.15);
}

.timeline-content {
  padding: 16px 18px;
  border-radius: 18px;
  background: #fbfdfd;
  border: 1px solid #ecf1f3;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.timeline-title {
  font-size: 16px;
  font-weight: 600;
  color: #17324d;
}

.timeline-meta {
  margin-top: 6px;
  font-size: 13px;
  color: #667085;
}

.timeline-remark {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #dfe7ea;
  color: #51606b;
  line-height: 1.7;
  font-size: 13px;
}

@media (max-width: 1400px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
  }

  .hero-meta {
    align-items: flex-start;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .timeline-item {
    grid-template-columns: 72px 16px minmax(0, 1fr);
    gap: 12px;
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 22px;
  }
}
</style>
