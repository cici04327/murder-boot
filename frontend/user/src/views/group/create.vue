<template>
  <div class="create-group-page">
    <el-card class="create-card">
      <template #header>
        <div class="card-header">
          <span>🤝 发起拼单</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="create-form">
        <el-form-item label="选择剧本" prop="scriptId">
          <el-select v-model="form.scriptId" placeholder="请选择剧本" filterable @change="handleScriptChange">
            <el-option v-for="script in scripts" :key="script.id" :label="script.name" :value="script.id">
              <span>{{ script.name }}</span>
              <span style="color: #999; margin-left: 8px;">{{ script.playerCount }}人 | ¥{{ script.price }}/人</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="选择门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" filterable>
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="开车时间" prop="playTime">
          <el-date-picker v-model="form.playTime" type="datetime" placeholder="选择开车时间" :disabled-date="disabledDate" />
        </el-form-item>

        <el-form-item label="需要人数" prop="needCount">
          <el-input-number v-model="form.needCount" :min="2" :max="12" />
          <span class="form-tip">（不含自己）</span>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :max="999" />
          <span class="form-tip">元/人</span>
        </el-form-item>

        <el-form-item label="性别要求">
          <el-radio-group v-model="form.genderRequirement">
            <el-radio label="男女不限">男女不限</el-radio>
            <el-radio label="仅限男生">仅限男生</el-radio>
            <el-radio label="仅限女生">仅限女生</el-radio>
            <el-radio label="自定义">自定义</el-radio>
          </el-radio-group>
          <el-input v-if="form.genderRequirement === '自定义'" v-model="form.customGender" placeholder="如：需2男3女" style="width: 150px; margin-left: 10px;" />
        </el-form-item>

        <el-form-item label="新手友好">
          <el-switch v-model="form.newbieWelcome" />
          <span class="form-tip">开启后更容易吸引新手玩家</span>
        </el-form-item>

        <el-form-item label="拼单说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可以写一些备注，如：不要太认真，开心最重要~" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">发起拼单</el-button>
          <el-button size="large" @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createGroup } from '@/api/group'
import { getScriptList } from '@/api/script'
import { getStoreList } from '@/api/store'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const scripts = ref([])
const stores = ref([])

const form = reactive({
  scriptId: null,
  storeId: null,
  playTime: null,
  needCount: 5,
  price: 128,
  genderRequirement: '男女不限',
  customGender: '',
  newbieWelcome: true,
  description: ''
})

const rules = {
  scriptId: [{ required: true, message: '请选择剧本', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  playTime: [{ required: true, message: '请选择开车时间', trigger: 'change' }],
  needCount: [{ required: true, message: '请输入需要人数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const disabledDate = (time) => time.getTime() < Date.now() - 8.64e7

const handleScriptChange = (scriptId) => {
  const script = scripts.value.find(s => s.id === scriptId)
  if (script) {
    form.needCount = script.playerCount - 1
    form.price = script.price
  }
}

const loadScripts = async () => {
  try {
    const res = await getScriptList({ page: 1, pageSize: 100 })
    scripts.value = res.data?.records || res.data || []
  } catch { scripts.value = [] }
}

const loadStores = async () => {
  try {
    const res = await getStoreList({ page: 1, pageSize: 100 })
    stores.value = res.data?.records || res.data || []
  } catch { stores.value = [] }
}

const handleSubmit = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      // 获取选中的剧本信息
      const selectedScript = scripts.value.find(s => s.id === form.scriptId)
      const selectedStore = stores.value.find(s => s.id === form.storeId)
      
      // 格式化时间为 yyyy-MM-dd HH:mm
      const playTimeFormatted = form.playTime ? 
        new Date(form.playTime).toLocaleString('sv-SE', { 
          year: 'numeric', month: '2-digit', day: '2-digit', 
          hour: '2-digit', minute: '2-digit' 
        }).replace('T', ' ').substring(0, 16) : null
      
      const data = {
        scriptId: form.scriptId,
        scriptName: selectedScript?.name || '',
        storeId: form.storeId,
        storeName: selectedStore?.name || '',
        playTime: playTimeFormatted,
        needCount: form.needCount,
        playerCount: selectedScript?.playerCount || form.needCount,
        price: form.price,
        genderRequirement: form.genderRequirement === '自定义' ? form.customGender : form.genderRequirement,
        newbieWelcome: form.newbieWelcome ? 1 : 0,  // 转换布尔值为整数
        description: form.description
      }
      const res = await createGroup(data)
      if (res.code === 1 || res.code === 200) {
        ElMessage.success('🎉 拼单发起成功！')
        router.push('/group')
      }
    } catch (error) {
      console.error('发起拼单失败:', error)
      ElMessage.error('发起失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadScripts()
  loadStores()
})
</script>

<style scoped>
.create-group-page { max-width: 700px; margin: 20px auto; padding: 0 20px; }
.create-card { border-radius: 16px; }
.card-header { font-size: 20px; font-weight: 600; }
.create-form { padding: 20px 0; }
.create-form .el-select { width: 100%; }
.form-tip { margin-left: 10px; color: #999; font-size: 13px; }
.el-button--primary { background: #8B0000; border-color: #8B0000; }
.el-button--primary:hover { background: rgba(139, 0, 0, 0.9); }
</style>
