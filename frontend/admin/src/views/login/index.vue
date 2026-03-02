<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">剧本杀管理后台</h2>
      
      <div class="login-type-switch">
        <el-radio-group v-model="loginType" size="large" @change="handleLoginTypeChange">
          <el-radio-button label="admin">
            <span>超级管理员</span>
          </el-radio-button>
          <el-radio-button label="store">
            <span>门店登录</span>
          </el-radio-button>
        </el-radio-group>
      </div>
      
      <el-form :model="loginForm" :rules="currentRules" ref="loginFormRef" class="login-form">
        <template v-if="loginType === 'admin'">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入管理员用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
        </template>
        
        <template v-else>
          <el-form-item prop="loginAccount">
            <el-input
              v-model="loginForm.loginAccount"
              placeholder="请输入门店账号"
              prefix-icon="Shop"
              size="large"
            />
          </el-form-item>
        </template>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <SliderCaptcha 
            ref="captchaRef"
            @success="handleCaptchaSuccess"
            @fail="handleCaptchaFail"
            @refresh="handleCaptchaRefresh"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            :disabled="!captchaVerified"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userService } from '@/utils/request'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

const router = useRouter()
const loginFormRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)
const captchaVerified = ref(false)
const loginType = ref('admin')

const loginForm = reactive({
  username: 'test_user',
  password: '123456',
  loginAccount: ''
})

const adminRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const storeRules = {
  loginAccount: [{ required: true, message: '请输入门店账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const currentRules = computed(() => {
  return loginType.value === 'admin' ? adminRules : storeRules
})

const handleLoginTypeChange = () => {
  if (loginFormRef.value) {
    loginFormRef.value.clearValidate()
  }
}

const handleCaptchaSuccess = () => {
  captchaVerified.value = true
  ElMessage.success('验证成功')
}

const handleCaptchaFail = () => {
  captchaVerified.value = false
  ElMessage.error('验证失败，请重试')
}

const handleCaptchaRefresh = () => {
  captchaVerified.value = false
}

const handleLogin = async () => {
  if (!captchaVerified.value) {
    ElMessage.warning('请先完成安全验证')
    return
  }
  
  try {
    await loginFormRef.value.validate()
    loading.value = true
    
    let res
    
    if (loginType.value === 'admin') {
      res = await userService.post('/user/admin/login', {
        username: loginForm.username,
        password: loginForm.password
      })
    } else {
      res = await userService.post('/store/login', {
        loginAccount: loginForm.loginAccount,
        password: loginForm.password
      })
    }
    
    if (res.code === 1 || res.code === 200) {
      localStorage.setItem('admin-token', res.data.token)
      localStorage.setItem('admin-user', JSON.stringify(res.data))
      localStorage.setItem('admin-login-type', loginType.value)
      if (loginType.value === 'store') {
        localStorage.setItem('admin-store-id', res.data.storeId)
        localStorage.setItem('admin-store-name', res.data.storeName)
      }
      
      ElMessage.success('登录成功')
      setTimeout(() => {
        router.push('/').catch(err => {
          console.error('路由跳转失败:', err)
          window.location.href = '/'
        })
      }, 100)
    } else {
      ElMessage.error(res.msg || '登录失败')
      captchaVerified.value = false
      if (captchaRef.value) {
        captchaRef.value.reset()
      }
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录异常')
    captchaVerified.value = false
    if (captchaRef.value) {
      captchaRef.value.reset()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.login-type-switch {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.login-type-switch :deep(.el-radio-group) {
  display: flex;
  width: 100%;
}

.login-type-switch :deep(.el-radio-button) {
  flex: 1;
}

.login-type-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.login-form {
  margin-top: 20px;
}
</style>
