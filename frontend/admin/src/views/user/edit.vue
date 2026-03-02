<template>
  <div class="edit-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span>{{ isEdit ? '编辑用户' : '新增用户' }}</span>
      </template>
    </el-page-header>
    
    <el-card style="margin-top: 20px;">
      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-width="120px"
        v-loading="loading"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名" 
            :disabled="isEdit"
            maxlength="50"
          />
          <span v-if="isEdit" style="color: #909399; font-size: 12px; margin-left: 10px;">
            用户名不可修改
          </span>
        </el-form-item>

        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码" 
            maxlength="50"
            show-password
          />
          <span v-if="isEdit" style="color: #909399; font-size: 12px; margin-left: 10px;">
            不填写则不修改密码
          </span>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input 
            v-model="form.nickname" 
            placeholder="请输入昵称" 
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input 
            v-model="form.phone" 
            placeholder="请输入手机号" 
            maxlength="11"
          />
        </el-form-item>

        <el-form-item label="头像" prop="avatar">
          <div class="avatar-upload">
            <el-avatar 
              :size="100" 
              :src="form.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
              fit="cover"
            />
            <div class="avatar-input">
              <el-input 
                v-model="form.avatar" 
                placeholder="请输入头像URL"
                clearable
              >
                <template #prepend>
                  <el-icon><Picture /></el-icon>
                </template>
              </el-input>
              <div style="color: #909399; font-size: 12px; margin-top: 5px;">
                提示: 输入图片URL或使用图床上传图片
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="会员等级" prop="memberLevel">
          <el-select v-model="form.memberLevel" placeholder="请选择会员等级">
            <el-option label="普通会员" :value="1" />
            <el-option label="银卡会员" :value="2" />
            <el-option label="金卡会员" :value="3" />
            <el-option label="钻石会员" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="积分" prop="points">
          <el-input-number 
            v-model="form.points" 
            :min="0" 
            :max="999999"
            placeholder="请输入积分"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const id = route.params.id
const isEdit = computed(() => !!id)

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  avatar: '',
  gender: 1,
  memberLevel: 1,
  points: 0,
  status: 1
})

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  memberLevel: [
    { required: true, message: '请选择会员等级', trigger: 'change' }
  ]
}

// 加载用户数据
const loadData = async () => {
  if (!isEdit.value) return
  
  loading.value = true
  try {
    const res = await request.get(`/user/${id}`)
    // 不包含密码
    form.value = {
      username: res.data.username,
      password: '',
      nickname: res.data.nickname,
      phone: res.data.phone,
      gender: res.data.gender || 1,
      memberLevel: res.data.memberLevel || 1,
      points: res.data.points || 0,
      status: res.data.status
    }
  } catch (error) {
    console.error('加载用户数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  submitting.value = true
  try {
    const submitData = { ...form.value }
    
    // 如果是编辑且密码为空，删除密码字段
    if (isEdit.value && !submitData.password) {
      delete submitData.password
    } else if (submitData.password) {
      // 密码需要MD5加密（这里简化处理，实际应该在后端加密）
      // submitData.password = md5(submitData.password)
    }

    if (isEdit.value) {
      submitData.id = id
      await request.put('/user', submitData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/user', submitData)
      ElMessage.success('创建成功')
    }
    
    router.back()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-input {
  flex: 1;
}

.edit-container {
  padding: 20px;
}
</style>
