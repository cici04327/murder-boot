<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>欢迎注册</h2>
        <p>剧本杀在线预约平台</p>
      </div>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            size="large"
            prefix-icon="Phone"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="inviteCode">
          <el-input
            v-model="registerForm.inviteCode"
            placeholder="邀请码（选填，填写可获得积分）"
            size="large"
            prefix-icon="Ticket"
          />
        </el-form-item>
        
        <!-- 滑块验证码 -->
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
            :loading="loading"
            :disabled="!captchaVerified"
            @click="handleRegister"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="router.push('/login')">立即登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const registerFormRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)
const captchaVerified = ref(false)

const registerForm = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})

// 从URL读取邀请码
onMounted(() => {
  const inviteCode = route.query.inviteCode
  if (inviteCode) {
    registerForm.inviteCode = inviteCode
    ElMessage.success('已自动填入邀请码，注册成功后您和邀请人都将获得积分奖励！')
  }
})

const validatePhone = (rule, value, callback) => {
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 验证码成功回调
const handleCaptchaSuccess = () => {
  captchaVerified.value = true
  ElMessage.success('验证成功')
}

// 验证码失败回调
const handleCaptchaFail = () => {
  captchaVerified.value = false
  ElMessage.error('验证失败，请重试')
}

// 验证码刷新回调
const handleCaptchaRefresh = () => {
  captchaVerified.value = false
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  // 检查验证码
  if (!captchaVerified.value) {
    ElMessage.warning('请先完成安全验证')
    return
  }
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const success = await userStore.register({
          username: registerForm.username,
          phone: registerForm.phone,
          password: registerForm.password,
          inviteCode: registerForm.inviteCode || undefined // 只在有值时传递
        })
        if (success) {
          if (registerForm.inviteCode) {
            ElMessage.success({
              message: '注册成功！您和邀请人都已获得积分奖励',
              duration: 3000
            })
          }
          setTimeout(() => {
            router.push('/login')
          }, 1000)
        } else {
          // 注册失败，重置验证码
          captchaVerified.value = false
          if (captchaRef.value) {
            captchaRef.value.reset()
          }
        }
      } catch (error) {
        // 注册失败，重置验证码
        captchaVerified.value = false
        if (captchaRef.value) {
          captchaRef.value.reset()
        }
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-box {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px;
}

.register-header p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.register-form {
  margin-top: 30px;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-footer span {
  margin-right: 5px;
}
</style>
