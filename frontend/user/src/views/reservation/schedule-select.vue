<template>
  <div class="schedule-select-page">
    <!-- 顶部信息 -->
    <div class="page-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <el-button class="back-btn" text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div class="header-icon">🎭</div>
        <h1>选择场次</h1>
        <p class="header-subtitle">请选择您想要参与的剧本场次</p>
      </div>
    </div>

    <!-- 剧本信息卡片 -->
    <div class="script-info-card" v-if="scriptInfo">
      <div class="script-cover-wrap">
        <img :src="scriptInfo.cover || defaultCover" class="script-cover" />
      </div>
      <div class="script-meta">
        <h2 class="script-name">{{ scriptInfo.name }}</h2>
        <div class="script-tags">
          <el-tag size="small" type="warning">👥 {{ scriptInfo.playerCount }} 人本</el-tag>
          <el-tag size="small" type="info" v-if="scriptInfo.duration">⏱️ {{ scriptInfo.duration }} 小时</el-tag>
          <el-tag size="small" type="danger" v-if="scriptInfo.difficulty">
            {{ ['', '★☆☆', '★★☆', '★★★', '★★★★'][scriptInfo.difficulty] || '未知难度' }}
          </el-tag>
          <el-tag size="small" type="success">¥{{ scriptInfo.price }}/人</el-tag>
        </div>
      </div>
    </div>

    <!-- 门店筛选（剧本详情进来时，可选门店） -->
    <div class="filter-bar" v-if="!fixedStoreId">
      <span class="filter-label">选择门店：</span>
      <div class="store-tabs">
        <div
          v-for="store in storeList"
          :key="store.id"
          class="store-tab"
          :class="{ active: selectedStoreId === store.id }"
          @click="selectStore(store.id)"
        >
          🏠 {{ store.name }}
        </div>
      </div>
    </div>
    <div class="store-fixed-bar" v-else-if="storeInfo">
      <span class="store-fixed-name">🏠 {{ storeInfo.name }}</span>
      <span class="store-fixed-addr">📍 {{ storeInfo.address }}</span>
    </div>

    <!-- 日期选择 -->
    <div class="date-bar">
      <div
        v-for="day in dateList"
        :key="day.value"
        class="date-tab"
        :class="{ active: selectedDate === day.value }"
        @click="selectDate(day.value)"
      >
        <div class="date-weekday">{{ day.weekday }}</div>
        <div class="date-num">{{ day.date }}</div>
        <div class="date-label">{{ day.label }}</div>
      </div>
    </div>

    <!-- 场次列表 -->
    <div class="schedule-list-section">
      <div v-if="loading" class="loading-wrap">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载场次中...</span>
      </div>

      <div v-else-if="!selectedStoreId && !fixedStoreId" class="empty-tip">
        <span>👆 请先选择门店</span>
      </div>

      <div v-else-if="visibleSchedules.length === 0" class="empty-tip">
        <el-empty description="今日暂无可约场次" :image-size="80">
          <el-button @click="selectDate(getNextAvailableDate())">查看其他日期</el-button>
        </el-empty>
      </div>

      <div v-else class="schedule-cards">
        <div
          v-for="schedule in visibleSchedules"
          :key="schedule.id"
          class="schedule-card"
          :class="{
            'schedule-full': schedule.currentPlayers >= schedule.maxPlayers,
            'schedule-few': remainCount(schedule) <= 2 && remainCount(schedule) > 0
          }"
          @click="handleSelectSchedule(schedule)"
        >
          <!-- 时间 -->
          <div class="sc-time">
            <span class="sc-start">{{ formatTime(schedule.startTime) }}</span>
            <span class="sc-sep">—</span>
            <span class="sc-end">{{ formatTime(schedule.endTime) }}</span>
          </div>

          <!-- 剧本名（门店视角可能多剧本） -->
          <div class="sc-script" v-if="schedule.scriptName && !scriptInfo">
            📜 {{ schedule.scriptName }}
          </div>

          <!-- 房间 -->
          <div class="sc-room" v-if="schedule.roomName">
            🚪 {{ schedule.roomName }}
          </div>

          <!-- DM 信息 -->
          <div class="sc-dm" v-if="schedule.dmName">
            <el-avatar :size="24" :src="schedule.dmAvatar" style="vertical-align:middle;margin-right:6px">
              🎭
            </el-avatar>
            <span>DM：{{ schedule.dmName }}</span>
          </div>
          <div class="sc-dm sc-dm-empty" v-else>
            🎭 DM：待分配
          </div>

          <!-- 余量 -->
          <div class="sc-remain-wrap">
            <el-progress
              :percentage="Math.round((schedule.currentPlayers || 0) / schedule.maxPlayers * 100)"
              :status="schedule.currentPlayers >= schedule.maxPlayers ? 'exception' : remainCount(schedule) <= 2 ? 'warning' : 'success'"
              :stroke-width="6"
              :show-text="false"
              style="flex:1"
            />
            <div class="sc-remain" :class="getRemainClass(schedule)">
              {{ getRemainText(schedule) }}
            </div>
          </div>

          <!-- 价格 -->
          <div class="sc-price">¥{{ scriptInfo?.price || schedule.price || '—' }}/人</div>

          <!-- 操作按钮 -->
          <el-button
            type="primary"
            size="small"
            class="sc-btn"
            :disabled="schedule.currentPlayers >= schedule.maxPlayers"
            @click.stop="handleSelectSchedule(schedule)"
          >
            {{ schedule.currentPlayers >= schedule.maxPlayers ? '已满' : '选择此场次' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { getAvailableSchedules, getScriptDetail } from '@/api/script'
import { getStoreDetail, getStoreList } from '@/api/store'
import { useUserStore } from '@/store/user'
import { hasScheduleStarted } from '@/utils/schedule-time'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultCover = '/default-script.jpg'

// URL 参数
const scriptId = route.query.scriptId ? parseInt(route.query.scriptId) : null
const fixedStoreId = route.query.storeId ? parseInt(route.query.storeId) : null

// 数据
const scriptInfo = ref(null)
const storeInfo = ref(null)
const storeList = ref([])
const selectedStoreId = ref(fixedStoreId || null)
const schedules = ref([])
const loading = ref(false)
const scheduleNowTick = ref(Date.now())
let scheduleClockTimer = null

// 获取本地日期字符串（避免 toISOString() 因 UTC 时区导致日期偏移）
const toLocalDateStr = (d) => {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 生成未来14天日期列表
const dateList = computed(() => {
  const list = []
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  for (let i = 0; i < 14; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const value = toLocalDateStr(d)
    list.push({
      value,
      weekday: weekdays[d.getDay()],
      date: `${d.getMonth() + 1}/${d.getDate()}`,
      label: i === 0 ? '今天' : i === 1 ? '明天' : ''
    })
  }
  return list
})

const selectedDate = ref(dateList.value[0].value)

// 加载剧本信息
const loadScriptInfo = async () => {
  if (!scriptId) return
  try {
    const res = await getScriptDetail(scriptId)
    if (res.code === 1 || res.code === 200) {
      scriptInfo.value = res.data
    }
  } catch (e) {
    console.warn('加载剧本信息失败', e)
  }
}

// 加载门店信息（固定门店时）
const loadStoreInfo = async () => {
  if (!fixedStoreId) return
  try {
    const res = await getStoreDetail(fixedStoreId)
    if (res.code === 1 || res.code === 200) {
      storeInfo.value = res.data
    }
  } catch (e) {
    console.warn('加载门店信息失败', e)
  }
}

// 加载可选门店列表（有 scriptId 但无固定 storeId 时）
const loadStoreList = async () => {
  if (fixedStoreId) return
  try {
    const res = await getStoreList({ page: 1, pageSize: 50 })
    if (res.code === 1 || res.code === 200) {
      storeList.value = res.data?.records || res.data?.list || []
      // 如果有 scriptId，优先找有该剧本排期的门店；否则默认选第一个
      if (storeList.value.length > 0 && !selectedStoreId.value) {
        selectedStoreId.value = storeList.value[0].id
      }
    }
  } catch (e) {
    console.warn('加载门店列表失败', e)
  }
}

// 加载当天可约场次
const loadSchedules = async () => {
  if (!selectedStoreId.value && !fixedStoreId) return
  loading.value = true
  schedules.value = []
  try {
    const params = {
      storeId: selectedStoreId.value || fixedStoreId,
      days: 14  // 扩大查询天数，避免今天没排期时显示空
    }
    if (scriptId) params.scriptId = scriptId

    const res = await getAvailableSchedules(params)
    if (res.code === 1 || res.code === 200) {
      const all = Array.isArray(res.data) ? res.data : []
      // scheduleDate 统一取前10位，兼容不同格式
      schedules.value = all.filter(s => {
        const date = s.scheduleDate ? String(s.scheduleDate).substring(0, 10) : ''
        return date === selectedDate.value
      })
    }
  } catch (e) {
    console.error('加载场次失败', e)
  } finally {
    loading.value = false
  }
}

const selectStore = (id) => {
  selectedStoreId.value = id
  loadSchedules()
}

const selectDate = (date) => {
  selectedDate.value = date
  loadSchedules()
}

const getNextAvailableDate = () => {
  const idx = dateList.value.findIndex(d => d.value === selectedDate.value)
  return dateList.value[Math.min(idx + 1, dateList.value.length - 1)].value
}

const visibleSchedules = computed(() => {
  const now = scheduleNowTick.value
  return schedules.value.filter((schedule) => !hasScheduleStarted(schedule, now))
})

// 余量计算
const remainCount = (s) => (s.maxPlayers || 0) - (s.currentPlayers || 0)

const getRemainText = (s) => {
  const r = remainCount(s)
  if (r <= 0) return '已满'
  if (r === 1) return '差1人成团'
  return `余 ${r} 位`
}

const getRemainClass = (s) => {
  const r = remainCount(s)
  if (r <= 0) return 'remain-full'
  if (r <= 2) return 'remain-few'
  return 'remain-ok'
}

const formatTime = (t) => t ? String(t).substring(0, 5) : ''

// 选择场次 → 跳转预约确认页
const handleSelectSchedule = (schedule) => {
  if (hasScheduleStarted(schedule, scheduleNowTick.value)) {
    ElMessage.warning('该场次已开场，请选择其他场次')
    return
  }
  if (schedule.currentPlayers >= schedule.maxPlayers) {
    ElMessage.warning('该场次已满，请选择其他场次')
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // 跳转到预约创建页，携带 scheduleId 和相关信息
  router.push({
    path: '/reservation/create',
    query: {
      scheduleId: schedule.id,
      scriptId: scriptId || schedule.scriptId,
      storeId: selectedStoreId.value || fixedStoreId,
      roomId: schedule.roomId
    }
  })
}

const startScheduleClock = () => {
  if (scheduleClockTimer) {
    return
  }
  scheduleClockTimer = setInterval(() => {
    scheduleNowTick.value = Date.now()
  }, 30000)
}

const stopScheduleClock = () => {
  if (!scheduleClockTimer) {
    return
  }
  clearInterval(scheduleClockTimer)
  scheduleClockTimer = null
}

onMounted(async () => {
  startScheduleClock()
  // 串行：先并行加载剧本信息和门店列表，确保 selectedStoreId 赋值完成后再查场次
  await Promise.all([
    loadScriptInfo(),
    loadStoreInfo(),
    loadStoreList()
  ])
  // 此时 selectedStoreId 已赋值，可以安全加载场次
  await loadSchedules()
})

onBeforeUnmount(() => {
  stopScheduleClock()
})
</script>

<style scoped>
.schedule-select-page {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 40px;
  min-height: 100vh;
}

/* 顶部 */
.page-header {
  position: relative;
  text-align: center;
  padding: 36px 20px 28px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%);
  border-radius: 0 0 20px 20px;
  margin-bottom: 16px;
}

.header-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(192, 57, 43,0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(192, 57, 43,0.2) 0%, transparent 50%);
  border-radius: inherit;
}

.header-content { position: relative; z-index: 1; }

.back-btn {
  position: absolute;
  top: 16px;
  left: 16px;
  color: rgba(255,255,255,0.8) !important;
}

.header-icon { font-size: 40px; margin-bottom: 10px; }

.page-header h1 {
  font-size: 26px;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.header-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  margin: 0;
}

/* 剧本信息卡片 */
.script-info-card {
  display: flex;
  gap: 16px;
  align-items: center;
  background: linear-gradient(135deg, rgba(26,26,46,0.98) 0%, rgba(22,33,62,0.98) 100%);
  border: 1px solid rgba(192, 57, 43,0.2);
  border-radius: 14px;
  padding: 16px;
  margin: 0 16px 16px;
}

.script-cover {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

.script-name {
  font-size: 18px;
  color: #fff;
  margin: 0 0 10px;
}

.script-tags { display: flex; flex-wrap: wrap; gap: 6px; }

/* 门店筛选 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  flex-shrink: 0;
}

.store-tabs { display: flex; flex-wrap: wrap; gap: 8px; }

.store-tab {
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15);
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.store-tab.active,
.store-tab:hover {
  background: rgba(192, 57, 43,0.3);
  border-color: rgba(192, 57, 43,0.6);
  color: #fff;
}

.store-fixed-bar {
  padding: 0 16px 12px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.store-fixed-name {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.store-fixed-addr {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}

/* 日期选择 */
.date-bar {
  display: flex;
  gap: 8px;
  padding: 0 16px 16px;
  overflow-x: auto;
}

.date-bar::-webkit-scrollbar { display: none; }

.date-tab {
  flex-shrink: 0;
  width: 64px;
  padding: 10px 0;
  text-align: center;
  border-radius: 12px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  cursor: pointer;
  transition: all 0.2s;
}

.date-tab.active {
  background: linear-gradient(135deg, rgba(192, 57, 43,0.5) 0%, rgba(196,30,58,0.5) 100%);
  border-color: rgba(192, 57, 43,0.7);
}

.date-tab:hover:not(.active) {
  background: rgba(255,255,255,0.1);
}

.date-weekday {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 4px;
}

.date-num {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 2px;
}

.date-label {
  font-size: 10px;
  color: #ff6b6b;
  min-height: 14px;
}

/* 场次列表 */
.schedule-list-section {
  padding: 0 16px;
}

.loading-wrap, .empty-tip {
  text-align: center;
  padding: 40px 0;
  color: rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
}

.loading-icon {
  font-size: 24px;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.schedule-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-card {
  background: linear-gradient(135deg, rgba(30,30,55,0.98) 0%, rgba(25,40,75,0.98) 100%);
  border: 1px solid rgba(192, 57, 43,0.2);
  border-radius: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-card:hover:not(.schedule-full) {
  border-color: rgba(192, 57, 43,0.5);
  box-shadow: 0 4px 20px rgba(192, 57, 43,0.2);
  transform: translateY(-2px);
}

.schedule-card.schedule-full {
  opacity: 0.55;
  cursor: not-allowed;
}

.schedule-card.schedule-few {
  border-color: rgba(230,162,60,0.4);
}

.sc-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sc-start, .sc-end {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.sc-sep {
  color: rgba(255,255,255,0.4);
  font-size: 16px;
}

.sc-script, .sc-room {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}

.sc-dm {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: rgba(255,255,255,0.75);
}

.sc-dm-empty {
  color: rgba(255,255,255,0.35);
}

.sc-remain-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sc-remain {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
  flex-shrink: 0;
}

.remain-ok { background: rgba(103,194,58,0.15); color: #67c23a; }
.remain-few { background: rgba(230,162,60,0.15); color: #e6a23c; }
.remain-full { background: rgba(245,108,108,0.15); color: #f56c6c; }

.sc-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
}

.sc-btn {
  align-self: flex-end;
  min-width: 110px;
}

@media (max-width: 480px) {
  .script-info-card { flex-direction: column; align-items: flex-start; }
  .sc-start, .sc-end { font-size: 20px; }
}
</style>
