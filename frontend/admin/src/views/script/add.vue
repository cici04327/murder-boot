<template>
  <div class="script-add">
    <el-card>
      <template #header>{{ isEdit ? '编辑剧本' : '新增剧本' }}</template>
      <el-form label-width="100px" :model="form" :rules="rules" ref="formRef" @submit.prevent>
        <el-form-item label="剧本名称">
          <el-input v-model="form.name" placeholder="请输入剧本名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="封面图片">
          <div style="display: flex; gap: 10px; align-items: flex-start;">
            <el-upload
              class="cover-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploading">
                <el-icon v-if="!uploading"><Upload /></el-icon>
                {{ uploading ? '上传中...' : '上传封面' }}
              </el-button>
            </el-upload>
            <el-input v-model="form.cover" placeholder="或手动输入图片URL" style="width: 400px" />
          </div>
          <div v-if="form.cover" style="margin-top: 10px;">
            <el-image :src="form.cover" style="width: 100px; height: 133px; border-radius: 4px;" fit="cover">
              <template #error>
                <div style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5;">
                  <span style="color: #999; font-size: 12px;">加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
          <div style="margin-top: 5px; color: #999; font-size: 12px;">
            建议尺寸：宽高比 3:4，如 300x400px
          </div>
        </el-form-item>
        
        <el-form-item label="标签">
          <el-input 
            v-model="form.tags" 
            placeholder="多个标签用逗号分隔，如：烧脑,推理,新手友好" 
            style="width: 500px"
          />
          <div style="margin-top: 5px; color: #999; font-size: 12px;">
            示例标签：烧脑、推理、情感、欢乐、机制、还原、新手友好、硬核、沉浸
          </div>
        </el-form-item>
        
        <el-form-item label="评分">
          <el-rate v-model="form.rating" :max="5" show-score />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">用于展示剧本质量评价</span>
        </el-form-item>
        
        <el-form-item label="独家剧本">
          <el-switch v-model="form.isExclusive" :active-value="1" :inactive-value="0" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">开启后将在列表中显示"独家"标识</span>
        </el-form-item>
        
        <el-form-item label="玩家人数">
          <el-input-number v-model="form.playerCount" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="时长(小时)">
          <el-input-number v-model="form.duration" :min="1" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" placeholder="请输入简介" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const submitting = ref(false)
const uploading = ref(false)
const categoryOptions = ref([])
const formRef = ref(null)
const isEdit = ref(false)
const scriptId = ref(null)

// 上传配置
const uploadUrl = '/api/admin/file/upload/script-cover'
const uploadHeaders = {
  token: localStorage.getItem('admin-token') || '',
  'X-Client-Type': 'admin'
}

const form = reactive({
  name: '',
  categoryId: null,
  cover: '',
  tags: '',
  rating: 0,
  isExclusive: 0,
  playerCount: 6,
  duration: 3.5,
  difficulty: 3,
  price: 168,
  description: ''
})

const loadCategories = async () => {
  try {
    const res = await request.get('/script/category')
    categoryOptions.value = res.data || []
  } catch (e) {
    categoryOptions.value = []
  }
}

const loadScriptDetail = async (id) => {
  try {
    const res = await request.get(`/script/${id}`)
    const data = res.data
    form.name = data.name
    form.categoryId = data.categoryId
    form.cover = data.cover || ''
    form.tags = data.tags || ''
    form.rating = data.rating || 0
    form.isExclusive = data.isExclusive || 0
    form.playerCount = data.playerCount
    form.duration = data.duration
    form.difficulty = data.difficulty
    form.price = data.price
    form.description = data.description
  } catch (e) {
    ElMessage.error('加载剧本详情失败')
    router.back()
  }
}

const rules = {
  name: [{ required: true, message: '请输入剧本名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  playerCount: [
    { required: true, message: '请输入人数', trigger: 'change' },
    { validator: (_, v, cb) => (v >= 1 && v <= 12) ? cb() : cb(new Error('人数范围1-12')), trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请输入时长', trigger: 'change' },
    { validator: (_, v, cb) => (v > 0) ? cb() : cb(new Error('时长需大于0')), trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'change' },
    { validator: (_, v, cb) => (v >= 0) ? cb() : cb(new Error('价格需不小于0')), trigger: 'change' }
  ]
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  uploading.value = true
  return true
}

// 上传成功回调
const handleCoverSuccess = (response) => {
  uploading.value = false
  console.log('上传响应:', response)
  if (response.code === 200) {
    form.cover = response.data
    console.log('封面已更新为:', form.cover)
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    console.log('提交前 form.cover:', form.cover)
    
    if (isEdit.value) {
      // 将 ID 添加到 form 对象中
      const updateData = { ...form, id: scriptId.value }
      console.log('提交的数据:', updateData)
      await request.put('/script', updateData)
      ElMessage.success('剧本更新成功')
    } else {
      await request.post('/script', form)
      ElMessage.success('剧本新增成功')
    }
    
    router.push('/script/list')
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  
  // 检查是否为编辑模式
  const query = router.currentRoute.value.query
  if (query.id) {
    isEdit.value = true
    scriptId.value = parseInt(query.id)
    await loadScriptDetail(scriptId.value)
  }
})
</script>

<style scoped>
.script-add { width: 100%; }
</style>
