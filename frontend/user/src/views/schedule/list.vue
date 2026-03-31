<template>
  <div class="schedule-hall-page">
    <section class="hall-hero">
      <div class="hero-glow hero-glow-left"></div>
      <div class="hero-glow hero-glow-right"></div>
      <div class="hero-topbar">
        <button class="back-btn" type="button" @click="router.back()">
          <span class="back-btn-icon">
            <el-icon><ArrowLeft /></el-icon>
          </span>
          <span class="back-btn-text">返回</span>
        </button>
        <div class="hero-badge">📅 排期大厅</div>
      </div>
      <h1>已排期剧本杀</h1>
      <p>按日期挑当天能开的车，跨门店看不同剧本，一眼锁定你想上的场。</p>
      <div class="hero-stats">
        <div class="hero-stat">
          <span class="stat-value">{{ selectedSchedules.length }}</span>
          <span class="stat-label">当日场次</span>
        </div>
        <div class="hero-stat">
          <span class="stat-value">{{ selectedScriptCount }}</span>
          <span class="stat-label">不同剧本</span>
        </div>
        <div class="hero-stat">
          <span class="stat-value">{{ selectedStoreCount }}</span>
          <span class="stat-label">涉及门店</span>
        </div>
      </div>
    </section>

    <section class="date-section">
      <div class="section-title">
        <span class="title-icon">🗓️</span>
        <span>选择日期</span>
      </div>
      <div class="date-strip">
        <button
          v-for="item in dateTabs"
          :key="item.value"
          class="date-pill"
          :class="{ active: selectedDate === item.value, available: hasSchedules(item.value) }"
          @click="selectedDate = item.value"
        >
          <span class="date-week">{{ item.weekday }}</span>
          <span class="date-day">{{ item.day }}</span>
          <span class="date-label">{{ item.label }}</span>
          <span v-if="hasSchedules(item.value)" class="date-count">{{ getScheduleCount(item.value) }}</span>
        </button>
      </div>
    </section>

    <section class="filter-section">
      <div class="filter-header">
        <div class="section-title">
          <span class="title-icon">🎛️</span>
          <span>细筛场次</span>
        </div>
        <button v-if="hasActiveFilters" class="filter-reset" @click="clearFilters">
          重置筛选
        </button>
      </div>

      <div class="filter-group">
        <div class="filter-label">门店</div>
        <div class="filter-strip">
          <button
            class="filter-pill"
            :class="{ active: !selectedStoreId }"
            @click="selectedStoreId = ''"
          >
            全部门店
          </button>
          <button
            v-for="store in storeOptions"
            :key="store.value"
            class="filter-pill"
            :class="{ active: selectedStoreId === store.value }"
            @click="selectedStoreId = store.value"
          >
            {{ store.label }}
          </button>
        </div>
      </div>

      <div class="filter-group">
        <div class="filter-label">剧本</div>
        <div class="filter-strip">
          <button
            class="filter-pill"
            :class="{ active: !selectedScriptId }"
            @click="selectedScriptId = ''"
          >
            全部剧本
          </button>
          <button
            v-for="script in scriptOptions"
            :key="script.value"
            class="filter-pill"
            :class="{ active: selectedScriptId === script.value }"
            @click="selectedScriptId = script.value"
          >
            {{ script.label }}
          </button>
        </div>
      </div>
    </section>

    <section class="schedule-section">
      <div class="section-header">
        <div class="header-left">
          <h3>🎭 {{ selectedDateTitle }}</h3>
          <span class="header-subtitle">{{ selectedSummaryText }}</span>
        </div>
        <div class="header-right">
          <span class="summary-chip">剧本 {{ selectedScriptCount }}</span>
          <span class="summary-chip">门店 {{ selectedStoreCount }}</span>
          <span class="summary-chip danger">场次 {{ selectedSchedules.length }}</span>
        </div>
      </div>

      <div v-loading="loading" class="schedule-grid">
        <template v-if="!loading && selectedSchedules.length > 0">
          <article
            v-for="schedule in selectedSchedules"
            :key="schedule.id"
            class="schedule-card"
            @click="handleBook(schedule)"
          >
            <div class="card-cover">
              <LazyImage
                :src="schedule.cover || getScriptCover(schedule.scriptName, '')"
                :alt="schedule.scriptName"
                :height="220"
                :immediate="true"
              />
              <div class="time-badge">{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</div>
              <div class="remain-badge" :class="getRemainClass(schedule)">
                {{ getRemainText(schedule) }}
              </div>
              <div class="card-overlay">
                <span class="overlay-text">立即预约</span>
              </div>
            </div>

            <div class="card-body">
              <div class="card-top">
                <h4 class="script-name">{{ schedule.scriptName }}</h4>
                <span class="price-tag">¥{{ formatPrice(schedule.price) }}/人</span>
              </div>

              <div class="meta-row">
                <span class="meta-chip difficulty" :class="'diff-' + (schedule.difficulty || 2)">
                  {{ getDifficultyText(schedule.difficulty) }}
                </span>
                <span class="meta-chip">👥 {{ schedule.playerCount || schedule.maxPlayers || 0 }}人本</span>
                <span class="meta-chip">⏱️ {{ schedule.duration || 3 }}h</span>
              </div>

              <div class="location-block">
                <div class="location-line">
                  <span class="line-icon">🏠</span>
                  <span class="line-text">{{ schedule.storeName || '待定门店' }}</span>
                </div>
                <div class="location-line">
                  <span class="line-icon">🚪</span>
                  <span class="line-text">{{ schedule.roomName || '待定房间' }}</span>
                </div>
                <div class="location-line" :class="{ dim: !schedule.dmName }">
                  <span class="line-icon">🎭</span>
                  <span class="line-text">{{ schedule.dmName ? `DM：${schedule.dmName}` : 'DM：待分配' }}</span>
                </div>
              </div>

              <div class="progress-block">
                <div class="progress-text">
                  <span>已报 {{ schedule.currentPlayers || 0 }} / {{ schedule.maxPlayers || 0 }}</span>
                  <span>{{ getRemainHint(schedule) }}</span>
                </div>
                <el-progress
                  :percentage="getProgress(schedule)"
                  :show-text="false"
                  :stroke-width="8"
                  :color="getProgressColor(schedule)"
                />
              </div>

              <div class="card-footer">
                <span class="date-hint">{{ formatScheduleDate(schedule.scheduleDate) }}</span>
                <el-button
                  type="primary"
                  class="book-btn"
                  :disabled="isFull(schedule)"
                  @click.stop="handleBook(schedule)"
                >
                  {{ isFull(schedule) ? '该场已满' : '预约这场' }}
                </el-button>
              </div>
            </div>
          </article>
        </template>

        <div v-else-if="!loading" class="empty-state">
          <div class="empty-icon">🕯️</div>
          <h4>{{ hasActiveFilters ? '这一天没有符合筛选的场次' : '这一天暂时没有开放场次' }}</h4>
          <p>{{ hasActiveFilters ? '换个门店或剧本条件看看，同一天也许还有别的好车。' : '换个日期看看，说不定下一天就有合适的车。' }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import LazyImage from '@/components/LazyImage.vue'
import { getScheduledSessions } from '@/api/script'
import { getScriptCover } from '@/assets/script-covers'
import { useUserStore } from '@/store/user'
import { hasScheduleStarted } from '@/utils/schedule-time'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const schedules = ref([])
const selectedDate = ref('')
const selectedStoreId = ref('')
const selectedScriptId = ref('')
const scheduleNowTick = ref(Date.now())
let scheduleClockTimer = null

const toLocalDateStr = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const dateTabs = computed(() => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return Array.from({ length: 14 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() + index)
    return {
      value: toLocalDateStr(date),
      weekday: weekdays[date.getDay()],
      day: `${date.getMonth() + 1}/${date.getDate()}`,
      label: index === 0 ? '今天' : index === 1 ? '明天' : '可约'
    }
  })
})

const daySchedules = computed(() => {
  const now = scheduleNowTick.value
  return schedules.value
    .filter((item) => String(item.scheduleDate || '').slice(0, 10) === selectedDate.value)
    .filter((item) => !hasScheduleStarted(item, now))
    .sort((a, b) => {
      const aTime = String(a.startTime || '')
      const bTime = String(b.startTime || '')
      return aTime.localeCompare(bTime)
    })
})

const storeOptions = computed(() => {
  const map = new Map()
  daySchedules.value.forEach((item) => {
    if (!item.storeId || map.has(String(item.storeId))) {
      return
    }
    map.set(String(item.storeId), {
      value: String(item.storeId),
      label: item.storeName || `门店${item.storeId}`
    })
  })
  return Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
})

const scriptOptions = computed(() => {
  const map = new Map()
  daySchedules.value.forEach((item) => {
    if (!item.scriptId || map.has(String(item.scriptId))) {
      return
    }
    map.set(String(item.scriptId), {
      value: String(item.scriptId),
      label: item.scriptName || `剧本${item.scriptId}`
    })
  })
  return Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
})

const selectedSchedules = computed(() => {
  return daySchedules.value.filter((item) => {
    const matchStore = !selectedStoreId.value || String(item.storeId) === selectedStoreId.value
    const matchScript = !selectedScriptId.value || String(item.scriptId) === selectedScriptId.value
    return matchStore && matchScript
  })
})

const selectedScriptCount = computed(() => new Set(selectedSchedules.value.map((item) => item.scriptId)).size)
const selectedStoreCount = computed(() => new Set(selectedSchedules.value.map((item) => item.storeId)).size)
const hasActiveFilters = computed(() => Boolean(selectedStoreId.value || selectedScriptId.value))

const selectedDateTitle = computed(() => {
  const target = dateTabs.value.find((item) => item.value === selectedDate.value)
  if (!target) {
    return '当天场次'
  }
  return `${target.label === '可约' ? target.weekday : target.label} · ${target.day}`
})

const selectedSummaryText = computed(() => {
  const fragments = ['当天可约的剧本与门店场次']
  if (selectedStoreId.value) {
    const store = storeOptions.value.find((item) => item.value === selectedStoreId.value)
    fragments.push(`门店：${store?.label || '已选'}`)
  }
  if (selectedScriptId.value) {
    const script = scriptOptions.value.find((item) => item.value === selectedScriptId.value)
    fragments.push(`剧本：${script?.label || '已选'}`)
  }
  return fragments.join(' · ')
})

const loadSchedules = async () => {
  loading.value = true
  try {
    const res = await getScheduledSessions({ days: 14 })
    schedules.value = Array.isArray(res.data) ? res.data : []

    if (!selectedDate.value) {
      selectedDate.value = String(route.query.date || '').slice(0, 10) || dateTabs.value[0]?.value || ''
    }

    if (!selectedDate.value || !hasSchedules(selectedDate.value)) {
      const firstAvailable = dateTabs.value.find((item) => hasSchedules(item.value))
      if (firstAvailable) {
        selectedDate.value = firstAvailable.value
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '加载排期场次失败')
  } finally {
    loading.value = false
  }
}

const hasSchedules = (dateValue) => {
  const now = scheduleNowTick.value
  return schedules.value.some((item) =>
    String(item.scheduleDate || '').slice(0, 10) === dateValue && !hasScheduleStarted(item, now)
  )
}

const getScheduleCount = (dateValue) => {
  const now = scheduleNowTick.value
  return schedules.value.filter((item) =>
    String(item.scheduleDate || '').slice(0, 10) === dateValue && !hasScheduleStarted(item, now)
  ).length
}

const clearFilters = () => {
  selectedStoreId.value = ''
  selectedScriptId.value = ''
}

const formatTime = (time) => String(time || '').slice(0, 5)

const formatPrice = (price) => {
  const value = Number(price || 0)
  return Number.isFinite(value) ? value.toFixed(0) : '0'
}

const getDifficultyText = (difficulty) => {
  const map = {
    1: '简单',
    2: '普通',
    3: '困难',
    4: '硬核'
  }
  return map[difficulty] || '普通'
}

const getRemainCount = (schedule) => Math.max(0, Number(schedule.maxPlayers || 0) - Number(schedule.currentPlayers || 0))

const isFull = (schedule) => getRemainCount(schedule) <= 0

const getRemainText = (schedule) => {
  const remain = getRemainCount(schedule)
  if (remain <= 0) return '已满员'
  if (remain === 1) return '差1人成团'
  return `还差${remain}位`
}

const getRemainHint = (schedule) => {
  const remain = getRemainCount(schedule)
  if (remain <= 0) return '这场已经满了'
  if (remain <= 2) return '名额紧张'
  return '现在上车刚刚好'
}

const getRemainClass = (schedule) => {
  const remain = getRemainCount(schedule)
  if (remain <= 0) return 'full'
  if (remain <= 2) return 'few'
  return 'ok'
}

const getProgress = (schedule) => {
  const max = Number(schedule.maxPlayers || 0)
  const current = Number(schedule.currentPlayers || 0)
  if (!max) return 0
  return Math.min(100, Math.round(current / max * 100))
}

const getProgressColor = (schedule) => {
  const remain = getRemainCount(schedule)
  if (remain <= 0) return '#f56c6c'
  if (remain <= 2) return '#e6a23c'
  return '#67c23a'
}

const formatScheduleDate = (scheduleDate) => {
  const current = String(scheduleDate || '').slice(0, 10)
  const target = dateTabs.value.find((item) => item.value === current)
  return target ? `${target.weekday} ${target.day}` : current
}

const handleBook = (schedule) => {
  if (hasScheduleStarted(schedule, scheduleNowTick.value)) {
    ElMessage.warning('这场已经开场，请换一场看看')
    return
  }
  if (isFull(schedule)) {
    ElMessage.warning('这场已经满员，请换一场看看')
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  router.push({
    path: '/reservation/create',
    query: {
      scheduleId: schedule.id,
      scriptId: schedule.scriptId,
      storeId: schedule.storeId,
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

watch(
  () => route.query.date,
  (value) => {
    const nextDate = String(value || '').slice(0, 10)
    if (nextDate) {
      selectedDate.value = nextDate
    }
  },
  { immediate: true }
)

watch(
  () => storeOptions.value.map((item) => item.value).join(','),
  () => {
    if (selectedStoreId.value && !storeOptions.value.some((item) => item.value === selectedStoreId.value)) {
      selectedStoreId.value = ''
    }
  },
  { immediate: true }
)

watch(
  () => scriptOptions.value.map((item) => item.value).join(','),
  () => {
    if (selectedScriptId.value && !scriptOptions.value.some((item) => item.value === selectedScriptId.value)) {
      selectedScriptId.value = ''
    }
  },
  { immediate: true }
)

onMounted(() => {
  startScheduleClock()
  if (!selectedDate.value) {
    selectedDate.value = dateTabs.value[0]?.value || ''
  }
  loadSchedules()
})

onBeforeUnmount(() => {
  stopScheduleClock()
})
</script>

<style scoped>
.schedule-hall-page {
  min-height: 100vh;
  padding: 20px;
  color: #fff;
}

.hall-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 36px 28px 30px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 55%, rgba(15, 52, 96, 0.96) 100%);
  border: 1px solid rgba(192, 57, 43, 0.28);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
}

.hero-topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.hero-glow {
  position: absolute;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  filter: blur(24px);
  opacity: 0.22;
}

.hero-glow-left {
  left: -80px;
  bottom: -120px;
  background: rgba(192, 57, 43, 0.9);
}

.hero-glow-right {
  right: -90px;
  top: -120px;
  background: rgba(192, 57, 43, 0.9);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(8, 12, 24, 0.32);
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  transition: transform 0.22s ease, border-color 0.22s ease, background 0.22s ease, box-shadow 0.22s ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 214, 214, 0.24);
  background: rgba(192, 57, 43, 0.18);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.24);
}

.back-btn:active {
  transform: translateY(0);
}

.back-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  font-size: 14px;
}

.back-btn-text {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.hero-badge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.24);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 13px;
  color: #ffd9d9;
}

.hall-hero h1 {
  position: relative;
  z-index: 1;
  margin: 16px 0 10px;
  font-size: 36px;
  line-height: 1.1;
}

.hall-hero p {
  position: relative;
  z-index: 1;
  max-width: 760px;
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.7;
}

.hero-stats {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.hero-stat {
  min-width: 120px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.stat-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.date-section,
.filter-section,
.schedule-section {
  margin-bottom: 28px;
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  font-size: 20px;
  font-weight: 700;
}

.date-strip {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.date-strip::-webkit-scrollbar {
  height: 6px;
}

.date-strip::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
}

.date-pill {
  position: relative;
  flex-shrink: 0;
  width: 96px;
  padding: 14px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  cursor: pointer;
  transition: all 0.25s ease;
}

.date-pill:hover {
  transform: translateY(-2px);
  border-color: rgba(192, 57, 43, 0.42);
  background: rgba(255, 255, 255, 0.08);
}

.date-pill.active {
  border-color: rgba(192, 57, 43, 0.68);
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.36) 0%, rgba(196, 30, 58, 0.3) 100%);
  box-shadow: 0 10px 24px rgba(192, 57, 43, 0.18);
}

.date-pill.available::after {
  content: '';
  position: absolute;
  top: 10px;
  right: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 10px rgba(103, 194, 58, 0.6);
}

.date-week,
.date-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.date-day {
  display: block;
  margin: 8px 0 6px;
  font-size: 24px;
  font-weight: 700;
}

.date-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  margin-top: 8px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.35);
  font-size: 12px;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.filter-reset {
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: all 0.25s ease;
}

.filter-reset:hover {
  border-color: rgba(192, 57, 43, 0.46);
  background: rgba(192, 57, 43, 0.18);
  color: #fff;
}

.filter-group {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.filter-group + .filter-group {
  margin-top: 16px;
}

.filter-label {
  flex-shrink: 0;
  width: 54px;
  padding-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.62);
}

.filter-strip {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-pill {
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.76);
  cursor: pointer;
  transition: all 0.25s ease;
}

.filter-pill:hover {
  transform: translateY(-1px);
  border-color: rgba(192, 57, 43, 0.42);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.filter-pill.active {
  border-color: rgba(192, 57, 43, 0.64);
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.3) 0%, rgba(196, 30, 58, 0.26) 100%);
  color: #ffe0e0;
  box-shadow: 0 10px 22px rgba(192, 57, 43, 0.14);
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.header-left h3 {
  margin: 0 0 6px;
  font-size: 24px;
}

.header-subtitle {
  color: rgba(255, 255, 255, 0.62);
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-chip {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.74);
}

.summary-chip.danger {
  background: rgba(192, 57, 43, 0.22);
  color: #ffd6d6;
}

.schedule-grid {
  min-height: 220px;
}

.schedule-card {
  display: grid;
  grid-template-columns: 260px 1fr;
  margin-bottom: 18px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(30, 30, 55, 0.98) 0%, rgba(25, 40, 75, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.schedule-card:hover {
  transform: translateY(-4px);
  border-color: rgba(192, 57, 43, 0.5);
  box-shadow: 0 16px 32px rgba(192, 57, 43, 0.18);
}

.card-cover {
  position: relative;
  min-height: 220px;
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
}

.time-badge,
.remain-badge {
  position: absolute;
  left: 14px;
  z-index: 2;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(6px);
}

.time-badge {
  top: 14px;
  background: rgba(0, 0, 0, 0.55);
}

.remain-badge {
  bottom: 14px;
}

.remain-badge.ok {
  background: rgba(103, 194, 58, 0.2);
  color: #a8f0b0;
}

.remain-badge.few {
  background: rgba(230, 162, 60, 0.2);
  color: #ffd18b;
}

.remain-badge.full {
  background: rgba(245, 108, 108, 0.22);
  color: #ffc0c0;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 20, 34, 0.62);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.schedule-card:hover .card-overlay {
  opacity: 1;
}

.overlay-text {
  padding: 12px 22px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.88);
  font-size: 14px;
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  padding: 20px 22px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.script-name {
  margin: 0;
  font-size: 22px;
  color: #fff;
}

.price-tag {
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 700;
  color: #ff8f8f;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 16px;
}

.meta-chip {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.meta-chip.difficulty.diff-1 {
  background: rgba(46, 125, 50, 0.24);
  color: #9ce49f;
}

.meta-chip.difficulty.diff-2 {
  background: rgba(245, 124, 0, 0.24);
  color: #ffcc80;
}

.meta-chip.difficulty.diff-3 {
  background: rgba(230, 81, 0, 0.24);
  color: #ffb074;
}

.meta-chip.difficulty.diff-4 {
  background: rgba(198, 40, 40, 0.24);
  color: #ff9d9d;
}

.location-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.location-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
}

.location-line.dim {
  color: rgba(255, 255, 255, 0.42);
}

.line-icon {
  width: 18px;
  text-align: center;
}

.line-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-block {
  margin-top: 18px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.66);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: auto;
  padding-top: 18px;
}

.date-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.book-btn {
  min-width: 112px;
  border: none;
  background: linear-gradient(135deg, #16213e 0%, #55141d 100%);
}

.book-btn:hover:not(:disabled) {
  opacity: 0.92;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border-radius: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.03);
  text-align: center;
}

.empty-icon {
  font-size: 46px;
  margin-bottom: 12px;
}

.empty-state h4 {
  margin: 0 0 8px;
  font-size: 20px;
}

.empty-state p {
  margin: 0;
  color: rgba(255, 255, 255, 0.62);
}

@media (max-width: 900px) {
  .schedule-card {
    grid-template-columns: 1fr;
  }

  .card-cover {
    min-height: 200px;
  }
}

@media (max-width: 640px) {
  .schedule-hall-page {
    padding: 14px;
  }

  .hall-hero {
    padding: 32px 18px 24px;
  }

  .hero-topbar {
    align-items: flex-start;
  }

  .back-btn {
    gap: 8px;
    padding: 7px 12px 7px 9px;
  }

  .back-btn-icon {
    width: 26px;
    height: 26px;
  }

  .hall-hero h1 {
    font-size: 28px;
  }

  .hero-stats {
    gap: 10px;
  }

  .hero-stat {
    min-width: calc(50% - 10px);
  }

  .date-section,
  .filter-section,
  .schedule-section {
    padding: 18px;
  }

  .filter-header,
  .filter-group,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-label {
    width: auto;
    padding-top: 0;
  }

  .card-body {
    padding: 18px;
  }

  .card-top,
  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .book-btn {
    width: 100%;
  }
}
</style>
