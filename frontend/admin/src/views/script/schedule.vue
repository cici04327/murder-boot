<template>
  <div class="schedule-page">

    <!-- 顶部统计概览 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon">📅</div>
        <div class="stat-body">
          <div class="stat-num">{{ todayStats.total }}</div>
          <div class="stat-label">今日场次</div>
        </div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon">✅</div>
        <div class="stat-body">
          <div class="stat-num">{{ todayStats.available }}</div>
          <div class="stat-label">可预约</div>
        </div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon">🔥</div>
        <div class="stat-body">
          <div class="stat-num">{{ todayStats.few }}</div>
          <div class="stat-label">即将满员</div>
        </div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon">🈵</div>
        <div class="stat-body">
          <div class="stat-num">{{ todayStats.full }}</div>
          <div class="stat-label">已满</div>
        </div>
      </div>
      <div class="stat-card purple">
        <div class="stat-icon">👥</div>
        <div class="stat-body">
          <div class="stat-num">{{ todayStats.players }}</div>
          <div class="stat-label">今日预约人数</div>
        </div>
      </div>
    </div>

    <!-- 主卡片 -->
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">📅 剧本排期管理</span>
          <div class="header-actions">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="day">日视图</el-radio-button>
              <el-radio-button label="week">周视图</el-radio-button>
            </el-radio-group>
            <el-button @click="batchDialogVisible = true">📆 批量生成</el-button>
            <el-button type="primary" @click="showAddDialog">
              <el-icon><Plus /></el-icon> 新增排期
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询栏 -->
      <div class="query-bar">
        <el-select
          v-if="!isStoreAdmin"
          v-model="selectedStoreId"
          placeholder="选择门店"
          style="width:180px"
          @change="handleStoreSwitch"
        >
          <el-option v-for="store in storeList" :key="store.id" :label="store.name" :value="String(store.id)" />
        </el-select>

        <div class="date-nav">
          <el-button :icon="ArrowLeft" circle size="small" @click="goPrevDay" />
          <el-date-picker
            v-model="queryDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:140px"
            @change="loadSchedules"
          />
          <el-button :icon="ArrowRight" circle size="small" @click="goToNextDay" />
          <el-button size="small" @click="goToday">今天</el-button>
        </div>

        <el-select v-model="queryScriptId" placeholder="筛选剧本" clearable style="width:160px">
          <el-option v-for="script in scripts" :key="script.id" :label="script.name" :value="script.id" />
        </el-select>

        <el-select v-model="queryStatus" placeholder="筛选状态" clearable style="width:120px">
          <el-option label="可预约" :value="1" />
          <el-option label="已满" :value="0" />
          <el-option label="已关闭" :value="2" />
        </el-select>

        <el-button type="primary" @click="loadSchedules">🔍 查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- ===== 日视图 ===== -->
      <div v-if="viewMode === 'day'">
        <!-- 时间线布局 -->
        <div v-if="filteredSchedules.length === 0 && !loading" class="empty-day">
          <el-empty description="该日暂无排期">
            <el-button type="primary" @click="showAddDialog">立即新增</el-button>
          </el-empty>
        </div>

        <div v-loading="loading" class="timeline-wrap">
          <div
            v-for="row in filteredSchedules"
            :key="row.id"
            class="timeline-card"
            :class="getCardClass(row)"
          >
            <!-- 左侧时间 -->
            <div class="tc-time">
              <div class="tc-start">{{ formatTime(row.startTime) }}</div>
              <div class="tc-line"></div>
              <div class="tc-end">{{ formatTime(row.endTime) }}</div>
            </div>

            <!-- 主体信息 -->
            <div class="tc-body">
              <div class="tc-top">
                <span class="tc-script">📜 {{ row.scriptName || '未知剧本' }}</span>
                <el-tag :type="getStatusType(row.status)" size="small" class="tc-status">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </div>
              <div class="tc-meta">
                <span>🚪 {{ row.roomName || '未分配房间' }}</span>
                <span class="tc-divider">|</span>
                <span>📅 {{ row.scheduleDate }}</span>
                <span class="tc-divider">|</span>
                <span :class="getRemainClass(row)">
                  👥 {{ row.currentPlayers || 0 }}/{{ row.maxPlayers }} 人
                  <span v-if="row.maxPlayers - (row.currentPlayers||0) <= 2 && row.maxPlayers - (row.currentPlayers||0) > 0">
                    （差{{ row.maxPlayers - (row.currentPlayers||0) }}人成团）
                  </span>
                </span>
              </div>
              <!-- 进度条 -->
              <el-progress
                :percentage="row.maxPlayers ? Math.round((row.currentPlayers || 0) / row.maxPlayers * 100) : 0"
                :status="row.currentPlayers >= row.maxPlayers ? 'exception' : row.currentPlayers >= row.maxPlayers * 0.8 ? 'warning' : 'success'"
                :stroke-width="5"
                :show-text="false"
                style="margin-top:6px"
              />
              <div v-if="row.remark" class="tc-remark">💬 {{ row.remark }}</div>
            </div>

            <!-- 右侧操作 -->
            <div class="tc-actions">
              <el-button size="small" type="primary" plain @click="handleEdit(row)">编辑</el-button>
              <el-dropdown size="small" trigger="click" @command="cmd => handleDropdown(cmd, row)">
                <el-button size="small">更多 <el-icon><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="open" :disabled="row.status === 1">开启排期</el-dropdown-item>
                    <el-dropdown-item command="close" :disabled="row.status === 2">关闭排期</el-dropdown-item>
                    <el-dropdown-item command="copy">复制到明天</el-dropdown-item>
                    <el-dropdown-item command="delete" style="color:#f56c6c">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 周视图 ===== -->
      <div v-else class="week-view" v-loading="loading">
        <div class="week-header">
          <div class="week-nav">
            <el-button :icon="ArrowLeft" circle size="small" @click="prevWeek" />
            <span class="week-label">{{ weekLabel }}</span>
            <el-button :icon="ArrowRight" circle size="small" @click="nextWeek" />
          </div>
        </div>
        <div class="week-grid">
          <div
            v-for="day in weekDays"
            :key="day.value"
            class="week-col"
            :class="{ 'week-today': day.isToday }"
          >
            <div class="week-col-header">
              <div class="wch-weekday">{{ day.weekday }}</div>
              <div class="wch-date" :class="{ 'is-today': day.isToday }">{{ day.date }}</div>
            </div>
            <div class="week-col-body">
              <div
                v-for="s in getWeekSchedules(day.value)"
                :key="s.id"
                class="week-schedule-item"
                :class="getCardClass(s)"
                @click="handleEdit(s)"
                :title="`${s.scriptName} | ${formatTime(s.startTime)}-${formatTime(s.endTime)} | ${s.currentPlayers||0}/${s.maxPlayers}人`"
              >
                <div class="wsi-time">{{ formatTime(s.startTime) }}</div>
                <div class="wsi-name">{{ s.scriptName }}</div>
                <div class="wsi-remain" :class="getRemainClass(s)">
                  {{ s.currentPlayers||0 }}/{{ s.maxPlayers }}
                </div>
              </div>
              <div v-if="getWeekSchedules(day.value).length === 0" class="week-empty">
                <el-button size="small" text @click="quickAdd(day.value)">+ 新增</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '✏️ 编辑排期' : '➕ 新增排期'"
      width="560px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="剧本" prop="scriptId">
              <el-select v-model="form.scriptId" placeholder="选择剧本" style="width:100%" filterable
                @change="handleFormScriptChange">
                <el-option v-for="s in scripts" :key="s.id" :label="s.name" :value="s.id">
                  <span>{{ s.name }}</span>
                  <span style="float:right;color:#999;font-size:12px">
                    {{ s.playerCount }}人 · {{ s.duration }}h
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间" prop="roomId">
              <el-select v-model="form.roomId" placeholder="选择房间" style="width:100%" @change="triggerConflictCheck">
                <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id">
                  <span>{{ r.name }}</span>
                  <span style="float:right;color:#999;font-size:12px">
                    容量 {{ r.capacity || '?' }} 人
                    <span v-if="r.capacity && form.scriptId">
                      <span v-if="r.capacity >= (scripts.find(s=>s.id===form.scriptId)?.playerCount||0)" style="color:#67c23a"> ✓</span>
                      <span v-else style="color:#f56c6c"> ✗</span>
                    </span>
                  </span>
                </el-option>
              </el-select>
              <div v-if="roomMatchTip" class="room-match-tip" :class="roomMatchTip.startsWith('⚠️') ? 'tip-warn' : 'tip-ok'">
                {{ roomMatchTip }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="排期日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:100%"
            :disabled-date="disablePastDates"
            @change="triggerConflictCheck"
          />
        </el-form-item>

        <el-form-item label="时间段" required>
          <div class="time-range">
            <el-time-picker v-model="form.startTime" placeholder="开始" format="HH:mm" value-format="HH:mm:ss" style="flex:1" @change="triggerConflictCheck" />
            <span class="sep">—</span>
            <el-time-picker v-model="form.endTime" placeholder="结束" format="HH:mm" value-format="HH:mm:ss" style="flex:1" @change="triggerConflictCheck" />
          </div>
          <!-- 冲突检测结果 -->
          <div v-if="conflictChecking" class="conflict-tip conflict-checking">
            <el-icon class="is-loading"><Loading /></el-icon> 检测中...
          </div>
          <div v-else-if="conflictInfo?.conflict" class="conflict-tip conflict-error">
            ⚠️ {{ conflictInfo.message }}
          </div>
          <div v-else-if="conflictInfo && !conflictInfo.conflict" class="conflict-tip conflict-ok">
            ✅ {{ conflictInfo.message }}
          </div>
          <!-- 自动生成的时间段（选剧本后出现） -->
          <div class="quick-slots" v-if="autoSlots.length > 0">
            <span class="qs-label">🤖 自动：</span>
            <el-tag
              v-for="slot in autoSlots"
              :key="slot.label"
              size="small"
              class="qs-tag"
              type="success"
              :effect="form.startTime === slot.start ? 'dark' : 'plain'"
              @click="applyQuickSlot(slot)"
            >{{ slot.label }}</el-tag>
          </div>
          <!-- 手动快捷时段 -->
          <div class="quick-slots">
            <span class="qs-label">手动：</span>
            <el-tag
              v-for="slot in quickSlots"
              :key="slot.label"
              size="small"
              class="qs-tag"
              @click="applyQuickSlot(slot)"
            >{{ slot.label }}</el-tag>
          </div>
          <div class="slot-tip" v-if="autoSlots.length > 0">
            💡 营业时间 08:00-22:00，场次间隔 30 分钟，共 {{ autoSlots.length }} 个时段
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxPlayers">
              <el-input-number v-model="form.maxPlayers" :min="1" :max="30" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="可预约" :value="1" />
                <el-option label="已关闭" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- DM 选择 -->
        <el-form-item label="主持 DM">
          <el-select
            v-model="form.dmId"
            placeholder="选择主持 DM（可不选）"
            clearable
            style="width:100%"
            :loading="dmLoading"
          >
            <el-option :value="null" label="暂不分配" />
            <el-option
              v-for="dm in dmOptions"
              :key="dm.id"
              :label="dm.name"
              :value="dm.id"
            >
              <div style="display:flex;align-items:center;gap:8px">
                <el-avatar :size="24" :src="dm.avatar">🎭</el-avatar>
                <span>{{ dm.name }}</span>
                <el-rate :model-value="Number(dm.rating)" disabled size="small" style="margin-left:auto" />
              </div>
            </el-option>
          </el-select>
          <div v-if="form.dmId && selectedDm" class="dm-selected-tip">
            🎭 {{ selectedDm.name }}
            <el-tag v-for="tag in (selectedDm.styleTagList || [])" :key="tag" size="small" style="margin-left:4px">{{ tag }}</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可填写特殊说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit"
          :disabled="conflictInfo?.conflict">
          {{ isEdit ? '保存修改' : '新增排期' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量生成对话框 -->
    <el-dialog v-model="batchDialogVisible" title="📆 批量生成排期" width="620px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px"
        description="批量生成会在所选日期范围内，为每天创建指定时段的排期，已存在的排期不会重复创建。" />

      <el-form :model="batchForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="剧本" required>
              <el-select v-model="batchForm.scriptId" placeholder="选择剧本" style="width:100%" filterable
                @change="handleBatchScriptChange">
                <el-option v-for="s in scripts" :key="s.id" :label="s.name" :value="s.id">
                  <span>{{ s.name }}</span>
                  <span style="float:right;color:#999;font-size:12px">{{ s.playerCount }}人 · {{ s.duration }}h</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间" required>
              <el-select v-model="batchForm.roomId" placeholder="选择房间" style="width:100%">
                <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id">
                  <span>{{ r.name }}</span>
                  <span style="float:right;color:#999;font-size:12px">
                    容量 {{ r.capacity || '?' }} 人
                    <span v-if="r.capacity && batchForm.scriptId">
                      <span v-if="r.capacity >= (scripts.find(s=>s.id===batchForm.scriptId)?.playerCount||0)" style="color:#67c23a"> ✓</span>
                      <span v-else style="color:#f56c6c"> ✗</span>
                    </span>
                  </span>
                </el-option>
              </el-select>
              <div v-if="batchRoomMatchTip" class="room-match-tip" :class="batchRoomMatchTip.startsWith('⚠️') ? 'tip-warn' : 'tip-ok'">
                {{ batchRoomMatchTip }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="日期范围" required>
          <el-date-picker
            v-model="batchForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:100%"
            :disabled-date="disablePastDates"
          />
        </el-form-item>

        <el-form-item label="时间段" required>
          <div class="batch-slots-header" v-if="batchForm.scriptId">
            <el-alert type="success" :closable="false" show-icon style="margin-bottom:10px">
              <template #default>
                已根据剧本时长自动生成 <strong>{{ batchForm.timeSlots.length }}</strong> 个时段（营业时间 08:00-22:00，间隔30分钟）。可手动增删调整。
              </template>
            </el-alert>
          </div>
          <div class="batch-slots">
            <div v-for="(slot, idx) in batchForm.timeSlots" :key="idx" class="batch-slot-row">
              <span class="slot-idx">第 {{ idx + 1 }} 场</span>
              <el-time-picker v-model="slot.start" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:110px" />
              <span class="sep">—</span>
              <el-time-picker v-model="slot.end" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:110px" />
              <el-button :icon="Delete" circle size="small" type="danger" plain @click="removeTimeSlot(idx)" :disabled="batchForm.timeSlots.length === 1" />
            </div>
            <el-button size="small" @click="addTimeSlot">+ 手动添加时段</el-button>
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大人数">
              <el-input-number v-model="batchForm.maxPlayers" :min="1" :max="30" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计生成">
              <span class="batch-preview">约 {{ batchPreviewCount }} 个排期</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchGenerate">
          🚀 开始生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ArrowLeft, ArrowRight, ArrowDown, Loading } from '@element-plus/icons-vue'
import {
  getScheduleList,
  addSchedule,
  updateSchedule,
  updateScheduleStatus,
  deleteSchedule,
  generateSchedules,
  checkScheduleConflict
} from '@/api/schedule'
import { getDmList } from '@/api/dm'
import request from '@/utils/request'

// =================== 角色 & 门店 ===================
const loginType = localStorage.getItem('admin-login-type') || 'admin'
const isStoreAdmin = loginType === 'store'
const fixedStoreId = localStorage.getItem('admin-store-id')
const storeList = ref([])
const selectedStoreId = ref(fixedStoreId || '')
const storeId = computed(() => isStoreAdmin ? fixedStoreId : (selectedStoreId.value || null))

const loadStoreList = async () => {
  if (isStoreAdmin) return
  try {
    const res = await request.get('/store/list')
    if (res.code === 1 || res.code === 200) {
      storeList.value = res.data || []
      if (!selectedStoreId.value && storeList.value.length > 0) {
        selectedStoreId.value = String(storeList.value[0].id)
      }
    }
  } catch (e) { console.warn('加载门店列表失败', e) }
}

// =================== DM ===================
const dmOptions = ref([])
const dmLoading = ref(false)
const selectedDm = computed(() => dmOptions.value.find(d => d.id === form.dmId) || null)

const loadDmOptions = async (sid) => {
  if (!sid) { dmOptions.value = []; return }
  dmLoading.value = true
  try {
    const res = await getDmList(sid)
    if (res.code === 1 || res.code === 200) {
      dmOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('加载 DM 列表失败', e)
  } finally {
    dmLoading.value = false
  }
}

// =================== 状态 ===================
const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const viewMode = ref('day')

// =================== 查询 ===================
const queryDate = ref(new Date().toISOString().split('T')[0])
const queryScriptId = ref(null)
const queryStatus = ref(null)
const schedules = ref([])
const weekSchedules = ref([])
const scripts = ref([])
const rooms = ref([])

const filteredSchedules = computed(() => {
  let list = schedules.value
  if (queryScriptId.value) list = list.filter(s => s.scriptId === queryScriptId.value)
  if (queryStatus.value !== null && queryStatus.value !== undefined && queryStatus.value !== '') {
    list = list.filter(s => s.status === queryStatus.value)
  }
  return list.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
})

// 今日统计
const todayStats = computed(() => {
  const list = filteredSchedules.value
  return {
    total: list.length,
    available: list.filter(s => s.status === 1 && (s.currentPlayers || 0) < s.maxPlayers).length,
    few: list.filter(s => {
      const r = s.maxPlayers - (s.currentPlayers || 0)
      return r > 0 && r <= 2
    }).length,
    full: list.filter(s => (s.currentPlayers || 0) >= s.maxPlayers).length,
    players: list.reduce((sum, s) => sum + (s.currentPlayers || 0), 0)
  }
})

const resetQuery = () => {
  queryScriptId.value = null
  queryStatus.value = null
  loadSchedules()
}

// =================== 日期导航 ===================
const goToday = () => {
  queryDate.value = new Date().toISOString().split('T')[0]
  loadSchedules()
}

const goPrevDay = () => {
  const d = new Date(queryDate.value)
  d.setDate(d.getDate() - 1)
  queryDate.value = d.toISOString().split('T')[0]
  loadSchedules()
}

const goToNextDay = () => {
  const d = new Date(queryDate.value)
  d.setDate(d.getDate() + 1)
  queryDate.value = d.toISOString().split('T')[0]
  loadSchedules()
}

// =================== 周视图 ===================
function getMonday(date) {
  const d = new Date(date)
  const day = d.getDay() || 7
  d.setDate(d.getDate() - day + 1)
  d.setHours(0, 0, 0, 0)
  return d
}

const weekStart = ref(getMonday(new Date()))

const weekDays = computed(() => {
  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const today = new Date().toISOString().split('T')[0]
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart.value)
    d.setDate(d.getDate() + i)
    const value = d.toISOString().split('T')[0]
    return { value, weekday: weekdays[i], date: `${d.getMonth() + 1}/${d.getDate()}`, isToday: value === today }
  })
})

const weekLabel = computed(() => `${weekDays.value[0].value} ~ ${weekDays.value[6].value}`)

const prevWeek = () => {
  const d = new Date(weekStart.value); d.setDate(d.getDate() - 7); weekStart.value = d; loadWeekSchedules()
}
const nextWeek = () => {
  const d = new Date(weekStart.value); d.setDate(d.getDate() + 7); weekStart.value = d; loadWeekSchedules()
}

const getWeekSchedules = (date) => {
  let list = weekSchedules.value.filter(s => s.scheduleDate === date)
  if (queryScriptId.value) list = list.filter(s => s.scriptId === queryScriptId.value)
  return list.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
}

const loadWeekSchedules = async () => {
  if (!storeId.value) return
  loading.value = true
  try {
    const results = await Promise.all(
      weekDays.value.map(day =>
        getScheduleList(storeId.value, day.value)
          .then(res => (res.code === 1 || res.code === 200) ? (res.data || []) : [])
          .catch(() => [])
      )
    )
    weekSchedules.value = results.flat()
  } finally { loading.value = false }
}

watch(viewMode, (mode) => { if (mode === 'week') loadWeekSchedules() })

// =================== 加载数据 ===================
const loadSchedules = async () => {
  if (!storeId.value) { schedules.value = []; return }
  loading.value = true
  try {
    const res = await getScheduleList(storeId.value, queryDate.value)
    if (res.code === 1 || res.code === 200) schedules.value = res.data || []
  } catch (e) { console.error('加载排期失败', e) }
  finally { loading.value = false }
}

const loadScripts = async () => {
  if (!storeId.value) return
  try {
    const res = await request.get('/script/page', { params: { page: 1, pageSize: 100 } })
    if (res.code === 1 || res.code === 200) scripts.value = res.data?.records || res.data?.list || []
  } catch (e) { console.error('加载剧本失败', e) }
}

const loadRooms = async () => {
  if (!storeId.value) return
  try {
    const res = await request.get(`/store/room/list/${storeId.value}`)
    if (res.code === 1 || res.code === 200) rooms.value = res.data || []
  } catch (e) { console.error('加载房间失败', e) }
}

const handleStoreSwitch = async () => {
  await Promise.all([loadScripts(), loadRooms()])
  await loadSchedules()
  if (viewMode.value === 'week') await loadWeekSchedules()
}

// =================== 样式辅助 ===================
const formatTime = (t) => t ? String(t).substring(0, 5) : ''
const getStatusType = (s) => ({ 1: 'success', 0: 'danger', 2: 'info' })[s] || 'info'
const getStatusText = (s) => ({ 1: '可预约', 0: '已满', 2: '已关闭' })[s] || '未知'

const getRemainClass = (row) => {
  const r = (row.maxPlayers || 0) - (row.currentPlayers || 0)
  if (r <= 0) return 'remain-full'
  if (r <= 2) return 'remain-few'
  return 'remain-ok'
}

const getCardClass = (row) => {
  if (row.status === 2) return 'tc-closed'
  if ((row.currentPlayers || 0) >= row.maxPlayers) return 'tc-full'
  const r = row.maxPlayers - (row.currentPlayers || 0)
  if (r <= 2) return 'tc-few'
  return 'tc-ok'
}

// =================== 营业时间自动切分 ===================
const BUSINESS_START = 8   // 早上 8 点
const BUSINESS_END   = 22  // 晚上 22 点（最晚结束时间）

/**
 * 根据剧本时长（小时，支持小数如 3.5）自动切分营业时间段
 * 相邻场次间隔 30 分钟（用于布场/清场）
 * 返回格式：[{ label, start, end }]
 */
const calcSlotsByDuration = (durationHours) => {
  if (!durationHours || durationHours <= 0) return []
  const slots = []
  const durationMin = Math.round(durationHours * 60)  // 转分钟
  const gapMin = 30  // 场次间隔（分钟）
  let currentMin = BUSINESS_START * 60  // 当前起始分钟（从08:00开始）

  while (currentMin + durationMin <= BUSINESS_END * 60) {
    const endMin = currentMin + durationMin
    const startStr = minsToTime(currentMin)
    const endStr   = minsToTime(endMin)
    slots.push({
      label: `${startStr.substring(0,5)}-${endStr.substring(0,5)}`,
      start: startStr,
      end:   endStr
    })
    currentMin = endMin + gapMin  // 下一场从结束后30分钟开始
  }
  return slots
}

const minsToTime = (totalMins) => {
  const h = Math.floor(totalMins / 60)
  const m = totalMins % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`
}

// 单条新增：选剧本后自动填充时间段列表
const autoSlots = ref([])   // 根据剧本时长自动生成的快捷时段
const roomMatchTip = ref('')  // 房间匹配提示
const batchRoomMatchTip = ref('')

/**
 * 根据剧本所需人数自动匹配最合适的房间
 * 策略：优先选 capacity >= playerCount 且差值最小的房间（最小富余）
 */
const autoMatchRoom = (playerCount, targetForm, tipRef) => {
  if (!playerCount || rooms.value.length === 0) return

  const available = rooms.value.filter(r => r.status === 1 || r.status == null)
  if (available.length === 0) return

  // 容量 >= 所需人数，按容量差值升序排序，取最小富余的房间
  const matched = available
    .filter(r => (r.capacity || 0) >= playerCount)
    .sort((a, b) => (a.capacity - playerCount) - (b.capacity - playerCount))

  if (matched.length > 0) {
    const best = matched[0]
    targetForm.roomId = best.id
    tipRef.value = `✅ 自动匹配：「${best.name}」容量 ${best.capacity} 人（剧本需 ${playerCount} 人）`
  } else {
    // 没有完全匹配的，选容量最大的
    const largest = [...available].sort((a, b) => (b.capacity || 0) - (a.capacity || 0))[0]
    targetForm.roomId = largest.id
    tipRef.value = `⚠️ 无容量≥${playerCount}人的房间，已选最大房间「${largest.name}」（容量 ${largest.capacity} 人）`
  }
}

const handleFormScriptChange = (scriptId) => {
  const script = scripts.value.find(s => s.id === scriptId)
  if (!script) return

  // 自动填充时间段
  if (script.duration) {
    autoSlots.value = calcSlotsByDuration(Number(script.duration))
    if (autoSlots.value.length > 0) {
      form.startTime = autoSlots.value[0].start
      form.endTime   = autoSlots.value[0].end
    }
  } else {
    autoSlots.value = []
  }

  // 自动填入最大人数
  if (script.playerCount) form.maxPlayers = script.playerCount

  // 自动匹配房间
  autoMatchRoom(script.playerCount, form, roomMatchTip)
}

// 批量生成：选剧本后自动填充所有时间段 + 匹配房间
const handleBatchScriptChange = (scriptId) => {
  const script = scripts.value.find(s => s.id === scriptId)
  if (!script) return

  if (script.duration) {
    const slots = calcSlotsByDuration(Number(script.duration))
    batchForm.timeSlots = slots.map(s => ({ start: s.start.substring(0,5), end: s.end.substring(0,5) }))
    ElMessage.success(`已根据剧本时长 ${script.duration} 小时自动生成 ${slots.length} 个时间段`)
  } else {
    batchForm.timeSlots = [{ start: '10:00', end: '14:00' }]
  }

  if (script.playerCount) batchForm.maxPlayers = script.playerCount

  // 自动匹配房间
  autoMatchRoom(script.playerCount, batchForm, batchRoomMatchTip)
}

// =================== 冲突检测 ===================
const conflictInfo = ref(null)   // null=未检测, false=无冲突, Object=有冲突
const conflictChecking = ref(false)

// 防抖冲突检测（房间/日期/时间任一变化时触发）
let conflictTimer = null
const triggerConflictCheck = () => {
  if (!form.roomId || !form.scheduleDate || !form.startTime || !form.endTime) {
    conflictInfo.value = null
    return
  }
  if (form.startTime >= form.endTime) {
    conflictInfo.value = null
    return
  }
  clearTimeout(conflictTimer)
  conflictTimer = setTimeout(async () => {
    conflictChecking.value = true
    try {
      const st = form.startTime.length === 5 ? form.startTime : form.startTime.substring(0, 5)
      const et = form.endTime.length   === 5 ? form.endTime   : form.endTime.substring(0, 5)
      const res = await checkScheduleConflict(
        storeId.value, form.roomId, form.scheduleDate, st, et,
        isEdit.value ? form.id : null
      )
      if (res.code === 1 || res.code === 200) {
        conflictInfo.value = res.data
      }
    } catch (e) {
      conflictInfo.value = null
    } finally {
      conflictChecking.value = false
    }
  }, 600)
}

// 快捷时段（手动备用）
const quickSlots = [
  { label: '08:00-12:00', start: '08:00:00', end: '12:00:00' },
  { label: '12:00-16:00', start: '12:00:00', end: '16:00:00' },
  { label: '16:00-20:00', start: '16:00:00', end: '20:00:00' },
  { label: '18:00-22:00', start: '18:00:00', end: '22:00:00' },
]

const applyQuickSlot = (slot) => {
  form.startTime = slot.start
  form.endTime = slot.end
  triggerConflictCheck()
}

// =================== 表单 ===================
const form = reactive({
  id: null, storeId: null, scriptId: null, roomId: null,
  dmId: null,
  scheduleDate: '', startTime: '', endTime: '',
  maxPlayers: 6, status: 1, remark: ''
})

const rules = {
  scriptId: [{ required: true, message: '请选择剧本', trigger: 'change' }],
  roomId:   [{ required: true, message: '请选择房间', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  maxPlayers:   [{ required: true, message: '请填写最大人数', trigger: 'blur' }]
}

const disablePastDates = (date) => date < new Date(new Date().setHours(0, 0, 0, 0))

const showAddDialog = () => {
  if (!storeId.value) { ElMessage.warning('请先选择门店'); return }
  isEdit.value = false
  Object.assign(form, { id: null, storeId: storeId.value, scriptId: null, roomId: null,
    dmId: null, scheduleDate: queryDate.value, startTime: '', endTime: '', maxPlayers: 6, status: 1, remark: '' })
  loadDmOptions(storeId.value)
  dialogVisible.value = true
}

const quickAdd = (date) => {
  if (!storeId.value) { ElMessage.warning('请先选择门店'); return }
  isEdit.value = false
  Object.assign(form, { id: null, storeId: storeId.value, scriptId: null, roomId: null,
    dmId: null, scheduleDate: date, startTime: '', endTime: '', maxPlayers: 6, status: 1, remark: '' })
  loadDmOptions(storeId.value)
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, storeId: row.storeId, scriptId: row.scriptId, roomId: row.roomId,
    dmId: row.dmId || null,
    scheduleDate: row.scheduleDate, startTime: row.startTime, endTime: row.endTime,
    maxPlayers: row.maxPlayers, status: row.status ?? 1, remark: row.remark
  })
  loadDmOptions(row.storeId)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (!form.startTime || !form.endTime) { ElMessage.error('请选择时间段'); return }
    if (form.startTime >= form.endTime) { ElMessage.error('开始时间必须早于结束时间'); return }
    submitting.value = true
    const data = { ...form, storeId: storeId.value }
    const res = isEdit.value ? await updateSchedule(data) : await addSchedule(data)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadSchedules()
      if (viewMode.value === 'week') loadWeekSchedules()
    } else { ElMessage.error(res.msg || '操作失败') }
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const resetForm = () => {
  formRef.value?.resetFields()
  conflictInfo.value = null
}

// =================== 下拉菜单 ===================
const handleDropdown = async (cmd, row) => {
  if (cmd === 'open') {
    const res = await updateScheduleStatus(row.id, 1)
    if (res.code === 1 || res.code === 200) { ElMessage.success('已开启'); loadSchedules() }
  } else if (cmd === 'close') {
    const res = await updateScheduleStatus(row.id, 2)
    if (res.code === 1 || res.code === 200) { ElMessage.success('已关闭'); loadSchedules() }
  } else if (cmd === 'copy') {
    const nextDay = new Date(row.scheduleDate)
    nextDay.setDate(nextDay.getDate() + 1)
    const nextDate = nextDay.toISOString().split('T')[0]
    const copyData = { ...row, id: undefined, scheduleDate: nextDate, currentPlayers: 0, status: 1 }
    const res = await addSchedule(copyData)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success(`已复制到 ${nextDate}`)
      loadSchedules()
      if (viewMode.value === 'week') loadWeekSchedules()
    }
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定删除【${row.scriptName}】${row.scheduleDate} ${formatTime(row.startTime)} 的排期？`,
        '删除确认', { type: 'warning' }
      )
      const res = await deleteSchedule(row.id)
      if (res.code === 1 || res.code === 200) { ElMessage.success('删除成功'); loadSchedules() }
    } catch (e) { /* cancel */ }
  }
}

// =================== 批量生成 ===================
const batchForm = reactive({
  scriptId: null, roomId: null, dateRange: [],
  timeSlots: [{ start: '10:00', end: '14:00' }],
  maxPlayers: 6
})

const batchPreviewCount = computed(() => {
  if (!batchForm.dateRange || batchForm.dateRange.length < 2) return 0
  const start = new Date(batchForm.dateRange[0])
  const end = new Date(batchForm.dateRange[1])
  const days = Math.round((end - start) / 86400000) + 1
  return days * batchForm.timeSlots.filter(s => s.start && s.end).length
})

const addTimeSlot = () => batchForm.timeSlots.push({ start: '14:00', end: '18:00' })
const removeTimeSlot = (idx) => { if (batchForm.timeSlots.length > 1) batchForm.timeSlots.splice(idx, 1) }

const handleBatchGenerate = async () => {
  if (!batchForm.scriptId || !batchForm.roomId) { ElMessage.error('请选择剧本和房间'); return }
  if (!batchForm.dateRange || batchForm.dateRange.length !== 2) { ElMessage.error('请选择日期范围'); return }
  batchSubmitting.value = true
  try {
    const timeSlots = batchForm.timeSlots.filter(s => s.start && s.end).map(s => `${s.start}:00-${s.end}:00`)
    const res = await generateSchedules({
      storeId: storeId.value, scriptId: batchForm.scriptId, roomId: batchForm.roomId,
      startDate: batchForm.dateRange[0], endDate: batchForm.dateRange[1],
      timeSlots, maxPlayers: batchForm.maxPlayers
    })
    if (res.code === 1 || res.code === 200) {
      ElMessage.success(`批量生成成功，共约 ${batchPreviewCount.value} 个排期`)
      batchDialogVisible.value = false
      loadSchedules()
      if (viewMode.value === 'week') loadWeekSchedules()
    } else { ElMessage.error(res.msg || '生成失败') }
  } catch (e) { console.error(e) }
  finally { batchSubmitting.value = false }
}

onMounted(async () => {
  await loadStoreList()
  await Promise.all([loadSchedules(), loadScripts(), loadRooms()])
})
</script>

<style scoped>
.schedule-page { padding: 0; }

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.08);
  border-left: 4px solid #409eff;
}

.stat-card.green  { border-left-color: #67c23a; }
.stat-card.orange { border-left-color: #e6a23c; }
.stat-card.red    { border-left-color: #f56c6c; }
.stat-card.purple { border-left-color: #9b59b6; }

.stat-icon { font-size: 28px; }
.stat-num  { font-size: 24px; font-weight: 700; color: #303133; line-height: 1; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }

/* 主卡片 */
.main-card { border-radius: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 17px; font-weight: 600; }
.header-actions { display: flex; gap: 10px; align-items: center; }

/* 查询栏 */
.query-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
.date-nav { display: flex; align-items: center; gap: 6px; }

/* 时间线 */
.empty-day { padding: 40px 0; }
.timeline-wrap { display: flex; flex-direction: column; gap: 10px; }

.timeline-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  border-left: 4px solid #409eff;
  transition: box-shadow 0.2s;
}
.timeline-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.tc-ok     { border-left-color: #67c23a; }
.tc-few    { border-left-color: #e6a23c; }
.tc-full   { border-left-color: #f56c6c; background: #fff5f5; }
.tc-closed { border-left-color: #dcdfe6; opacity: 0.7; }

.tc-time { display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 52px; flex-shrink: 0; }
.tc-start, .tc-end { font-size: 14px; font-weight: 700; color: #303133; }
.tc-line { width: 2px; height: 18px; background: #dcdfe6; border-radius: 1px; }

.tc-body { flex: 1; min-width: 0; }
.tc-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.tc-script { font-size: 15px; font-weight: 600; color: #303133; }
.tc-meta { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266; flex-wrap: wrap; }
.tc-divider { color: #dcdfe6; }
.tc-remark { font-size: 12px; color: #909399; margin-top: 6px; }
.tc-actions { display: flex; gap: 8px; flex-shrink: 0; align-items: flex-start; }

/* 余量颜色 */
.remain-ok   { color: #67c23a; font-weight: 600; }
.remain-few  { color: #e6a23c; font-weight: 600; }
.remain-full { color: #f56c6c; font-weight: 600; }

/* 周视图 */
.week-view { overflow-x: auto; }
.week-header { display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }
.week-nav { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 14px; font-weight: 600; color: #303133; }

.week-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; min-width: 700px; }
.week-col { border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; }
.week-col.week-today { border-color: #409eff; }

.week-col-header { background: #f5f7fa; padding: 8px; text-align: center; border-bottom: 1px solid #ebeef5; }
.week-today .week-col-header { background: #ecf5ff; }
.wch-weekday { font-size: 12px; color: #909399; }
.wch-date { font-size: 16px; font-weight: 600; color: #303133; margin-top: 2px; }
.wch-date.is-today { color: #409eff; }

.week-col-body { padding: 6px; min-height: 120px; display: flex; flex-direction: column; gap: 4px; }

.week-schedule-item {
  background: #e8f4ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  padding: 4px 6px;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.15s;
}
.week-schedule-item:hover { opacity: 0.8; transform: translateY(-1px); }
.week-schedule-item.tc-ok     { background: #f0f9eb; border-left-color: #67c23a; }
.week-schedule-item.tc-few    { background: #fdf6ec; border-left-color: #e6a23c; }
.week-schedule-item.tc-full   { background: #fef0f0; border-left-color: #f56c6c; }
.week-schedule-item.tc-closed { background: #f5f7fa; border-left-color: #dcdfe6; opacity: 0.6; }

.wsi-time { font-weight: 700; color: #303133; }
.wsi-name { color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wsi-remain { font-weight: 600; margin-top: 2px; }
.week-empty { display: flex; justify-content: center; align-items: center; flex: 1; }

/* 表单 */
.time-range { display: flex; align-items: center; gap: 8px; width: 100%; }
.sep { color: #909399; flex-shrink: 0; }

.quick-slots { display: flex; align-items: center; gap: 6px; margin-top: 8px; flex-wrap: wrap; }

.dm-selected-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.qs-label { font-size: 12px; color: #909399; }
.qs-tag { cursor: pointer; }
.qs-tag:hover { opacity: 0.8; }

.batch-slots { display: flex; flex-direction: column; gap: 8px; }
.batch-slot-row { display: flex; align-items: center; gap: 8px; }
.batch-preview { font-size: 16px; font-weight: 700; color: #409eff; }

.slot-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  padding: 4px 8px;
  background: #f4f4f5;
  border-radius: 4px;
}

.slot-idx {
  font-size: 12px;
  color: #909399;
  width: 42px;
  flex-shrink: 0;
}

.batch-slots-header { width: 100%; }

/* 冲突检测提示 */
.conflict-tip {
  font-size: 12px;
  margin-top: 6px;
  padding: 6px 10px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.5;
}
.conflict-checking { background: #f4f4f5; color: #909399; }
.conflict-error    { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; font-weight: 500; }
.conflict-ok       { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }

/* 房间匹配提示 */
.room-match-tip {
  font-size: 12px;
  margin-top: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1.5;
}
.tip-ok   { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.tip-warn { background: #fdf6ec; color: #e6a23c; border: 1px solid #faecd8; }

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .week-grid { grid-template-columns: repeat(4, 1fr); }
}
</style>
