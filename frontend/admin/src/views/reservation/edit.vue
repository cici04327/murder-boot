<template>
  <div class="reservation-edit">
    <el-card>
      <template #header>编辑预约</template>
      <el-form label-width="100px" :model="form" :rules="rules" ref="formRef" @submit.prevent>
        
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="门店" prop="storeId">
          <el-select 
            v-model="form.storeId" 
            placeholder="请选择门店" 
            filterable 
            clearable 
            style="width: 260px"
            @change="handleStoreChange"
          >
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="房间" prop="roomId">
          <el-select 
            v-model="form.roomId" 
            placeholder="请先选择门店" 
            filterable 
            clearable 
            style="width: 260px"
            :disabled="!form.storeId"
          >
            <el-option 
              v-for="r in roomOptions" 
              :key="r.id" 
              :label="r.name"
              :value="r.id"
            >
              <span>{{ r.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">容量: {{ r.capacity }}人</span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="剧本" prop="scriptId">
          <el-select v-model="form.scriptId" placeholder="请选择剧本" filterable clearable style="width: 260px">
            <el-option v-for="s in scriptOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间" prop="reservationTime">
          <el-date-picker 
            v-model="form.reservationTime" 
            type="datetime" 
            placeholder="请选择时间" 
            format="YYYY-MM-DD HH:mm:ss" 
            value-format="YYYY-MM-DD HH:mm:ss" 
          />
        </el-form-item>
        <el-form-item label="人数" prop="playerCount">
          <el-input-number v-model="form.playerCount" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="时长(小时)" prop="duration">
          <el-input-number v-model="form.duration" :min="1" :max="8" :step="0.5" />
        </el-form-item>
        <el-form-item label="支付状态" prop="payStatus">
          <el-radio-group v-model="form.payStatus">
            <el-radio :value="0">未支付</el-radio>
            <el-radio :value="1">已支付</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="备注信息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const reservationId = route.params.id

const formRef = ref(null)
const form = reactive({
  contactName: '',
  contactPhone: '',
  storeId: null,
  roomId: null,
  scriptId: null,
  reservationTime: '',
  playerCount: 6,
  duration: 3.5,
  payStatus: 0,
  remark: ''
})
const submitting = ref(false)
const storeOptions = ref([])
const scriptOptions = ref([])
const roomOptions = ref([])

const loadOptions = async () => {
  try {
    const [storesRes, scriptRes] = await Promise.all([
      request.get('/store/list'),
      request.get('/script/page', { params: { page: 1, pageSize: 100 } })
    ])
    storeOptions.value = storesRes.data || []
    scriptOptions.value = (scriptRes.data?.records) || []
  } catch (e) {
    storeOptions.value = []
    scriptOptions.value = []
  }
}

const loadReservation = async () => {
  try {
    const res = await request.get(`/reservation/${reservationId}`)
    const data = res.data
    form.contactName = data.contactName
    form.contactPhone = data.contactPhone
    form.storeId = data.storeId
    form.roomId = data.roomId
    form.scriptId = data.scriptId
    form.reservationTime = data.reservationTime
    form.playerCount = data.playerCount
    form.duration = data.duration || 3.5
    form.payStatus = data.payStatus || 0
    form.remark = data.remark
    
    // 加载对应门店的房间列表
    if (form.storeId) {
      await handleStoreChange(form.storeId)
    }
  } catch (error) {
    console.error('加载预约信息失败:', error)
    ElMessage.error('加载预约信息失败')
  }
}

const handleStoreChange = async (storeId) => {
  const currentRoomId = form.roomId
  roomOptions.value = []
  
  if (!storeId) {
    form.roomId = null
    return
  }
  
  try {
    const res = await request.get(`/store/room/list/${storeId}`)
    roomOptions.value = res.data || []
    
    // 如果原来有选中的房间，保持选中状态
    if (currentRoomId && roomOptions.value.some(r => r.id === currentRoomId)) {
      form.roomId = currentRoomId
    } else {
      form.roomId = null
    }
  } catch (e) {
    console.error('加载房间失败:', e)
    ElMessage.error('加载房间列表失败')
  }
}

const phoneRule = /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/
const rules = {
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: (_, v, cb) => phoneRule.test(v) ? cb() : cb(new Error('请输入合法的手机号或座机号')), trigger: 'blur' }
  ],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  scriptId: [{ required: true, message: '请选择剧本', trigger: 'change' }],
  reservationTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  playerCount: [
    { required: true, message: '请输入人数', trigger: 'change' },
    { validator: (_, v, cb) => (v >= 1 && v <= 12) ? cb() : cb(new Error('人数范围1-12')), trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请输入时长', trigger: 'change' },
    { validator: (_, v, cb) => (v >= 1 && v <= 8) ? cb() : cb(new Error('时长范围1-8小时')), trigger: 'change' }
  ]
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 将来时间校验
    if (form.reservationTime && new Date(form.reservationTime).getTime() < Date.now()) {
      ElMessage.error('预约时间须晚于当前时间')
      return
    }
    
    submitting.value = true
    await request.put(`/reservation/${reservationId}`, form)
    ElMessage.success('更新成功')
    router.push('/reservation/list')
  } catch (e) {
    // 错误提示由拦截器处理
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadReservation()
})
</script>

<style scoped>
.reservation-edit { 
  width: 100%; 
  padding: 20px;
}
</style>
