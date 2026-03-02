<template>
  <div class="script-schedule">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>📅 剧本排期管理</span>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增排期
          </el-button>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="query-bar">
        <el-date-picker
          v-model="queryDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadSchedules"
        />
        <el-select v-model="queryScriptId" placeholder="选择剧本" clearable @change="filterSchedules">
          <el-option 
            v-for="script in scripts" 
            :key="script.id" 
            :label="script.name" 
            :value="script.id" 
          />
        </el-select>
        <el-button type="primary" @click="loadSchedules">查询</el-button>
        <el-button @click="goToNextDay">下一天 →</el-button>
      </div>

      <!-- 排期列表 -->
      <el-table :data="filteredSchedules" v-loading="loading" stripe>
        <el-table-column prop="scheduleDate" label="日期" width="120" />
        <el-table-column prop="scriptName" label="剧本" min-width="150" />
        <el-table-column prop="roomName" label="房间" width="120" />
        <el-table-column label="时间段" width="150">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="预约情况" width="120">
          <template #default="{ row }">
            <span :class="{ 'full': row.currentPlayers >= row.maxPlayers }">
              {{ row.currentPlayers || 0 }} / {{ row.maxPlayers }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '关闭' : '开启' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑排期' : '新增排期'" 
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="剧本" prop="scriptId">
          <el-select v-model="form.scriptId" placeholder="选择剧本" style="width: 100%">
            <el-option 
              v-for="script in scripts" 
              :key="script.id" 
              :label="script.name" 
              :value="script.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="房间" prop="roomId">
          <el-select v-model="form.roomId" placeholder="选择房间" style="width: 100%">
            <el-option 
              v-for="room in rooms" 
              :key="room.id" 
              :label="room.name" 
              :value="room.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="排期日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="disablePastDates"
          />
        </el-form-item>
        
        <el-form-item label="时间段" required>
          <div class="time-range">
            <el-time-picker
              v-model="form.startTime"
              placeholder="开始时间"
              format="HH:mm"
              value-format="HH:mm:ss"
            />
            <span class="separator">至</span>
            <el-time-picker
              v-model="form.endTime"
              placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm:ss"
            />
          </div>
        </el-form-item>
        
        <el-form-item label="最大人数" prop="maxPlayers">
          <el-input-number v-model="form.maxPlayers" :min="1" :max="20" />
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量生成对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量生成排期" width="650px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="剧本" required>
          <el-select v-model="batchForm.scriptId" placeholder="选择剧本" style="width: 100%">
            <el-option v-for="script in scripts" :key="script.id" :label="script.name" :value="script.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="房间" required>
          <el-select v-model="batchForm.roomId" placeholder="选择房间" style="width: 100%">
            <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="日期范围" required>
          <el-date-picker
            v-model="batchForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disablePastDates"
          />
        </el-form-item>
        
        <el-form-item label="时间段" required>
          <div class="time-slots">
            <div v-for="(slot, index) in batchForm.timeSlots" :key="index" class="time-slot-item">
              <el-time-picker v-model="slot.start" format="HH:mm" value-format="HH:mm" placeholder="开始" />
              <span>-</span>
              <el-time-picker v-model="slot.end" format="HH:mm" value-format="HH:mm" placeholder="结束" />
              <el-button type="danger" :icon="Delete" circle size="small" @click="removeTimeSlot(index)" />
            </div>
            <el-button type="primary" size="small" @click="addTimeSlot">+ 添加时段</el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="最大人数">
          <el-input-number v-model="batchForm.maxPlayers" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

// 获取门店ID
const storeId = localStorage.getItem('admin-store-id')

// 查询条件
const queryDate = ref(new Date().toISOString().split('T')[0])
const queryScriptId = ref(null)

// 数据
const schedules = ref([])
const scripts = ref([])
const rooms = ref([])

// 过滤后的排期
const filteredSchedules = computed(() => {
  if (!queryScriptId.value) return schedules.value
  return schedules.value.filter(s => s.scriptId === queryScriptId.value)
})

// 表单
const form = reactive({
  id: null,
  storeId: storeId,
  scriptId: null,
  roomId: null,
  scheduleDate: '',
  startTime: '',
  endTime: '',
  maxPlayers: 6,
  remark: ''
})

// 批量生成表单
const batchForm = reactive({
  scriptId: null,
  roomId: null,
  dateRange: [],
  timeSlots: [{ start: '10:00', end: '14:00' }],
  maxPlayers: 6
})

// 表单验证规则
const rules = {
  scriptId: [{ required: true, message: '请选择剧本', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  maxPlayers: [{ required: true, message: '请输入最大人数', trigger: 'blur' }]
}

// 状态映射
const getStatusType = (status) => {
  const map = { 1: 'success', 0: 'warning', 2: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 1: '可预约', 0: '已满', 2: '已关闭' }
  return map[status] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time.substring(0, 5)
}

// 禁用过去的日期
const disablePastDates = (date) => {
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

// 加载排期
const loadSchedules = async () => {
  loading.value = true
  try {
    const res = await request.get('/script/schedule/list', {
      params: { storeId, date: queryDate.value }
    })
    if (res.code === 1 || res.code === 200) {
      schedules.value = res.data || []
    }
  } catch (error) {
    console.error('加载排期失败', error)
  } finally {
    loading.value = false
  }
}

// 加载剧本列表
const loadScripts = async () => {
  try {
    const res = await request.get('/script/page', { params: { storeId, page: 1, pageSize: 100 } })
    if (res.code === 1 || res.code === 200) {
      scripts.value = res.data?.records || []
    }
  } catch (error) {
    console.error('加载剧本失败', error)
  }
}

// 加载房间列表
const loadRooms = async () => {
  try {
    const res = await request.get(`/store/room/list/${storeId}`)
    if (res.code === 1 || res.code === 200) {
      rooms.value = res.data || []
    }
  } catch (error) {
    console.error('加载房间失败', error)
  }
}

// 下一天
const goToNextDay = () => {
  const date = new Date(queryDate.value)
  date.setDate(date.getDate() + 1)
  queryDate.value = date.toISOString().split('T')[0]
  loadSchedules()
}

// 显示新增对话框
const showAddDialog = () => {
  isEdit.value = false
  form.id = null
  form.scriptId = null
  form.roomId = null
  form.scheduleDate = queryDate.value
  form.startTime = ''
  form.endTime = ''
  form.maxPlayers = 6
  form.remark = ''
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    storeId: row.storeId,
    scriptId: row.scriptId,
    roomId: row.roomId,
    scheduleDate: row.scheduleDate,
    startTime: row.startTime,
    endTime: row.endTime,
    maxPlayers: row.maxPlayers,
    remark: row.remark
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (!form.startTime || !form.endTime) {
      ElMessage.error('请选择时间段')
      return
    }
    
    if (form.startTime >= form.endTime) {
      ElMessage.error('开始时间必须早于结束时间')
      return
    }
    
    submitting.value = true
    
    const data = { ...form, storeId }
    const res = isEdit.value 
      ? await request.put('/script/schedule', data)
      : await request.post('/script/schedule', data)
    
    if (res.code === 1 || res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadSchedules()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitting.value = false
  }
}

// 切换状态
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 2 : 1
  try {
    const res = await request.put('/script/schedule/status', null, {
      params: { id: row.id, status: newStatus }
    })
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('状态更新成功')
      loadSchedules()
    }
  } catch (error) {
    console.error('更新状态失败', error)
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该排期吗？', '删除确认', {
      type: 'warning'
    })
    
    const res = await request.delete(`/script/schedule/${row.id}`)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('删除成功')
      loadSchedules()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
}

// 添加时段
const addTimeSlot = () => {
  batchForm.timeSlots.push({ start: '14:00', end: '18:00' })
}

// 移除时段
const removeTimeSlot = (index) => {
  if (batchForm.timeSlots.length > 1) {
    batchForm.timeSlots.splice(index, 1)
  }
}

// 批量生成
const handleBatchGenerate = async () => {
  if (!batchForm.scriptId || !batchForm.roomId) {
    ElMessage.error('请选择剧本和房间')
    return
  }
  if (!batchForm.dateRange || batchForm.dateRange.length !== 2) {
    ElMessage.error('请选择日期范围')
    return
  }
  
  batchSubmitting.value = true
  try {
    const timeSlots = batchForm.timeSlots
      .filter(s => s.start && s.end)
      .map(s => `${s.start}:00-${s.end}:00`)
    
    const res = await request.post('/script/schedule/generate', {
      storeId,
      scriptId: batchForm.scriptId,
      roomId: batchForm.roomId,
      startDate: batchForm.dateRange[0],
      endDate: batchForm.dateRange[1],
      timeSlots,
      maxPlayers: batchForm.maxPlayers
    })
    
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('批量生成成功')
      batchDialogVisible.value = false
      loadSchedules()
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (error) {
    console.error('批量生成失败', error)
  } finally {
    batchSubmitting.value = false
  }
}

// 过滤排期
const filterSchedules = () => {
  // computed 会自动处理
}

onMounted(() => {
  loadSchedules()
  loadScripts()
  loadRooms()
})
</script>

<style scoped>
.script-schedule {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
}

.query-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.separator {
  color: #606266;
}

.full {
  color: #f56c6c;
  font-weight: bold;
}

.time-slots {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.time-slot-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
