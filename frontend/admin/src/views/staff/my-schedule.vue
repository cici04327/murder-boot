<template>
  <div class="my-schedule-page">
    <div class="page-header">
      <div>
        <div class="page-title">我的场次</div>
        <div class="page-subtitle">只展示当前 DM 已分配的排期场次</div>
      </div>
      <div class="page-actions">
        <el-radio-group v-model="periodType" @change="loadSchedules">
          <el-radio-button label="today">今天</el-radio-button>
          <el-radio-button label="tomorrow">明天</el-radio-button>
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="custom">指定日期</el-radio-button>
        </el-radio-group>
        <el-button :icon="ArrowLeft" circle @click="goPrevDay" />
        <el-date-picker
          v-model="queryDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          style="width: 160px"
          :disabled="periodType !== 'custom'"
          @change="loadSchedules"
        />
        <el-button :icon="ArrowRight" circle @click="goNextDay" />
        <el-button @click="goToday">今天</el-button>
        <el-button type="primary" plain :icon="Refresh" @click="loadSchedules">刷新</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">我的场次</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.available }}</div>
        <div class="stat-label">可接待</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.full }}</div>
        <div class="stat-label">已满员</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.players }}</div>
        <div class="stat-label">预约人数</div>
      </div>
    </div>

    <el-card class="schedule-card">
      <template #header>
        <div class="card-header">
          <span>{{ headerLabel }}</span>
          <span class="card-tip">点击“查看预约”可进入本场次预约列表</span>
        </div>
      </template>

      <el-empty v-if="!loading && schedules.length === 0" :description="periodType === 'week' ? '本周暂无分配给你的场次' : '当天暂无分配给你的场次'" />

      <div v-loading="loading" class="schedule-list">
        <template v-if="periodType === 'week'">
          <div v-for="section in weekSections" :key="section.date" class="week-section">
            <div class="week-section-title">{{ section.date }}</div>
            <div v-for="item in section.items" :key="item.id" class="schedule-item">
              <div class="time-block">
                <div class="start-time">{{ formatTime(item.startTime) }}</div>
                <div class="time-separator"></div>
                <div class="end-time">{{ formatTime(item.endTime) }}</div>
              </div>

              <div class="content-block">
                <div class="content-top">
                  <div class="script-name">{{ item.scriptName || '未命名剧本' }}</div>
                  <el-tag :type="getStatusType(item.status)" size="small">
                    {{ getStatusText(item.status) }}
                  </el-tag>
                </div>

                <div class="meta-row">
                  <span>门店：{{ item.storeName || '-' }}</span>
                  <span>房间：{{ item.roomName || '-' }}</span>
                  <span>人数：{{ item.currentPlayers || 0 }}/{{ item.maxPlayers || 0 }}</span>
                </div>

                <el-progress
                  :percentage="getOccupancy(item)"
                  :show-text="false"
                  :stroke-width="6"
                  :status="getProgressStatus(item)"
                />

                <div v-if="item.remark" class="remark-row">备注：{{ item.remark }}</div>
              </div>

              <div class="action-block">
                <el-button type="primary" @click="viewReservations(item)">查看预约</el-button>
              </div>
            </div>
          </div>
        </template>
        <div v-else v-for="item in schedules" :key="item.id" class="schedule-item">
          <div class="time-block">
            <div class="start-time">{{ formatTime(item.startTime) }}</div>
            <div class="time-separator"></div>
            <div class="end-time">{{ formatTime(item.endTime) }}</div>
          </div>

          <div class="content-block">
            <div class="content-top">
              <div class="script-name">{{ item.scriptName || '未命名剧本' }}</div>
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusText(item.status) }}
              </el-tag>
            </div>

            <div class="meta-row">
              <span>门店：{{ item.storeName || '-' }}</span>
              <span>房间：{{ item.roomName || '-' }}</span>
              <span>人数：{{ item.currentPlayers || 0 }}/{{ item.maxPlayers || 0 }}</span>
            </div>

            <el-progress
              :percentage="getOccupancy(item)"
              :show-text="false"
              :stroke-width="6"
              :status="getProgressStatus(item)"
            />

            <div v-if="item.remark" class="remark-row">备注：{{ item.remark }}</div>
          </div>

          <div class="action-block">
            <el-button type="primary" @click="viewReservations(item)">查看预约</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Refresh } from '@element-plus/icons-vue'
import { getMyScheduleList, getMyScheduleRange } from '@/api/schedule'

const router = useRouter()
const loading = ref(false)
const schedules = ref([])
const storeId = Number(localStorage.getItem('admin-store-id') || 0)
const formatLocalDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const queryDate = ref(formatLocalDate(new Date()))
const periodType = ref('today')

const headerLabel = computed(() => {
  if (periodType.value === 'week') {
    return '本周场次安排'
  }
  if (periodType.value === 'tomorrow') {
    return `${queryDate.value} 明日场次`
  }
  return `${queryDate.value} 场次安排`
})

const weekSections = computed(() => {
  const grouped = new Map()
  for (const item of schedules.value) {
    const key = item.scheduleDate || queryDate.value
    if (!grouped.has(key)) {
      grouped.set(key, [])
    }
    grouped.get(key).push(item)
  }
  return Array.from(grouped.entries()).map(([date, items]) => ({
    date,
    items: items.sort((a, b) => formatTime(a.startTime).localeCompare(formatTime(b.startTime)))
  }))
})

const stats = computed(() => ({
  total: schedules.value.length,
  available: schedules.value.filter(item => Number(item.status) === 1).length,
  full: schedules.value.filter(item => Number(item.currentPlayers || 0) >= Number(item.maxPlayers || 0) && Number(item.maxPlayers || 0) > 0).length,
  players: schedules.value.reduce((sum, item) => sum + Number(item.currentPlayers || 0), 0)
}))

const formatTime = (value) => String(value || '').slice(0, 5) || '--:--'

const getStatusType = (status) => {
  const mapping = { 0: 'danger', 1: 'success', 2: 'info' }
  return mapping[status] || 'info'
}

const getStatusText = (status) => {
  const mapping = { 0: '已满员', 1: '可预约', 2: '已关闭' }
  return mapping[status] || '未知'
}

const getOccupancy = (item) => {
  const maxPlayers = Number(item.maxPlayers || 0)
  if (!maxPlayers) return 0
  return Math.min(100, Math.round((Number(item.currentPlayers || 0) / maxPlayers) * 100))
}

const getProgressStatus = (item) => {
  if (Number(item.currentPlayers || 0) >= Number(item.maxPlayers || 0) && Number(item.maxPlayers || 0) > 0) {
    return 'exception'
  }
  if (getOccupancy(item) >= 80) {
    return 'warning'
  }
  return 'success'
}

const loadSchedules = async () => {
  if (!storeId) {
    ElMessage.warning('未识别到所属门店')
    return
  }
  loading.value = true
  try {
    syncPresetDate()
    const res = periodType.value === 'week'
      ? await getMyScheduleRange(storeId, getWeekStart(queryDate.value), getWeekEnd(queryDate.value))
      : await getMyScheduleList(storeId, queryDate.value)
    schedules.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载我的场次失败')
  } finally {
    loading.value = false
  }
}

const shiftDate = (days) => {
  const current = new Date(`${queryDate.value}T00:00:00`)
  if (periodType.value === 'week') {
    current.setDate(current.getDate() + days * 7)
  } else {
    periodType.value = 'custom'
    current.setDate(current.getDate() + days)
  }
  queryDate.value = formatLocalDate(current)
  loadSchedules()
}

const goPrevDay = () => shiftDate(-1)
const goNextDay = () => shiftDate(1)

const goToday = () => {
  periodType.value = 'today'
  syncPresetDate()
  loadSchedules()
}

const viewReservations = (item) => {
  router.push({
    path: '/staff/my-reservation',
    query: {
      reservationDate: queryDate.value,
      scheduleId: String(item.id),
      scheduleName: item.scriptName || '',
      timeRange: `${formatTime(item.startTime)}-${formatTime(item.endTime)}`
    }
  })
}

onMounted(() => {
  loadSchedules()
})

function syncPresetDate() {
  const now = new Date()
  if (periodType.value === 'today') {
    queryDate.value = formatLocalDate(now)
    return
  }
  if (periodType.value === 'tomorrow') {
    now.setDate(now.getDate() + 1)
    queryDate.value = formatLocalDate(now)
  }
}

function getWeekStart(dateText) {
  const date = new Date(`${dateText}T00:00:00`)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  return formatLocalDate(date)
}

function getWeekEnd(dateText) {
  const date = new Date(`${getWeekStart(dateText)}T00:00:00`)
  date.setDate(date.getDate() + 6)
  return formatLocalDate(date)
}
</script>

<style scoped>
.my-schedule-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2d3d;
}

.page-subtitle {
  margin-top: 6px;
  color: #8492a6;
  font-size: 13px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffffff 0%, #f6f8fc 100%);
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.08);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.schedule-card {
  border-radius: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-weight: 600;
}

.card-tip {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
}

.week-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.week-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.schedule-item {
  display: flex;
  align-items: stretch;
  gap: 18px;
  padding: 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.time-block {
  width: 88px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
}

.start-time,
.end-time {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.time-separator {
  width: 2px;
  flex: 1;
  min-height: 18px;
  background: linear-gradient(180deg, #dcdfe6 0%, #c0c4cc 100%);
  margin: 8px 0;
  border-radius: 999px;
}

.content-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.script-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #606266;
  font-size: 13px;
}

.remark-row {
  color: #909399;
  font-size: 13px;
}

.action-block {
  display: flex;
  align-items: center;
}

@media (max-width: 768px) {
  .schedule-item {
    flex-direction: column;
  }

  .time-block {
    width: auto;
    flex-direction: row;
    gap: 10px;
  }

  .time-separator {
    width: 18px;
    height: 2px;
    min-height: 0;
    margin: 0;
  }

  .action-block {
    justify-content: flex-end;
  }
}
</style>
