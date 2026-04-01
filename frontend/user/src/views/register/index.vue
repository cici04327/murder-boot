<template>
  <div class="register-container">
    <!-- 漂浮装饰元素 -->
    <div class="floating-elements">
      <span class="float-icon icon-1">🎭</span>
      <span class="float-icon icon-2">🔮</span>
      <span class="float-icon icon-3">📜</span>
      <span class="float-icon icon-4">🗝️</span>
      <span class="float-icon icon-5">🕯️</span>
      <span class="float-icon icon-6">⚔️</span>
      <span class="float-icon icon-7">🎪</span>
      <span class="float-icon icon-8">💀</span>
    </div>
    <!-- 角落装饰 -->
    <div class="corner-decor top-left"></div>
    <div class="corner-decor top-right"></div>
    <div class="corner-decor bottom-left"></div>
    <div class="corner-decor bottom-right"></div>

    <div class="register-box">
      <div class="register-header">
        <div class="logo-icon">🎭</div>
        <h2>欢迎注册</h2>
        <p>剧本杀在线预约平台</p>
        <div class="header-decoration">
          <span class="deco-line"></span>
          <span class="deco-diamond">◆</span>
          <span class="deco-line"></span>
        </div>
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
/* ===== 容器背景（与登录页一致） ===== */
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(255, 215, 0, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 69, 19, 0.10) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(75, 0, 130, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse at 30% 70%, rgba(47, 79, 79, 0.30) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 30%, rgba(25, 25, 112, 0.25) 0%, transparent 50%),
    linear-gradient(180deg, #0d0d0d 0%, #1a1a2e 25%, #16213e 50%, #1a1a2e 75%, #0d0d0d 100%);
  background-attachment: fixed;
}

.register-container::before {
  content: '';
  position: absolute;
  top: -50%; left: -50%;
  width: 200%; height: 200%;
  background:
    radial-gradient(circle at 25% 25%, rgba(255, 215, 0, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(192, 57, 43, 0.05) 0%, transparent 50%);
  animation: mysteryGlow 15s ease-in-out infinite;
  pointer-events: none;
}

.register-container::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.15) 1px, transparent 1px),
    radial-gradient(circle, rgba(255, 215, 0, 0.10) 1px, transparent 1px);
  background-size: 50px 50px, 80px 80px;
  background-position: 0 0, 25px 25px;
  animation: dustFloat 20s linear infinite;
  pointer-events: none;
  opacity: 0.3;
}

@keyframes mysteryGlow {
  0%, 100% { transform: rotate(0deg) scale(1); opacity: 0.5; }
  50%       { transform: rotate(180deg) scale(1.1); opacity: 0.8; }
}
@keyframes dustFloat {
  0%   { transform: translateY(0); }
  100% { transform: translateY(-50px); }
}

/* ===== 卡片 ===== */
.register-box {
  background: rgba(30, 30, 40, 0.85);
  border-radius: 16px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.5),
    0 0 60px rgba(139, 69, 19, 0.10),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  padding: 40px;
  width: 100%;
  max-width: 450px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 215, 0, 0.10);
}

.register-box::before {
  content: '';
  position: absolute;
  top: 10px; left: 10px; right: 10px; bottom: 10px;
  border: 1px solid rgba(255, 215, 0, 0.15);
  border-radius: 12px;
  pointer-events: none;
}

/* ===== 头部 ===== */
.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  font-size: 3.5rem;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5));
  animation: logoGlow 3s ease-in-out infinite;
}

@keyframes logoGlow {
  0%, 100% { filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5)); transform: scale(1); }
  50%       { filter: drop-shadow(0 0 25px rgba(255, 215, 0, 0.8)); transform: scale(1.05); }
}

.register-header h2 {
  font-size: 28px;
  color: #ffd700;
  margin: 0 0 10px;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
  letter-spacing: 2px;
}

.register-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  letter-spacing: 1px;
}

.header-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 15px;
  gap: 10px;
}

.deco-line {
  width: 50px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 215, 0, 0.5), transparent);
}

.deco-diamond {
  color: rgba(255, 215, 0, 0.6);
  font-size: 8px;
  animation: diamondPulse 2s ease-in-out infinite;
}

@keyframes diamondPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50%       { opacity: 1;   transform: scale(1.3); }
}

/* ===== 表单 ===== */
.register-form { margin-top: 30px; }

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}
.register-footer span { margin-right: 5px; }

/* ===== 输入框 ===== */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 215, 0, 0.2);
  box-shadow: none;
}
:deep(.el-input__wrapper:hover)   { border-color: rgba(255, 215, 0, 0.4); }
:deep(.el-input__wrapper.is-focus) {
  border-color: #ffd700;
  box-shadow: 0 0 10px rgba(255, 215, 0, 0.2);
}
:deep(.el-input__inner)           { color: #fff; }
:deep(.el-input__inner::placeholder) { color: rgba(255, 255, 255, 0.4); }
:deep(.el-input__prefix)          { color: rgba(255, 215, 0, 0.6); }

/* ===== 按钮 ===== */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #8b4513 0%, #a0522d 50%, #8b4513 100%);
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #ffd700;
  font-weight: bold;
  letter-spacing: 2px;
  transition: all 0.3s ease;
}
:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #a0522d 0%, #cd853f 50%, #a0522d 100%);
  border-color: #ffd700;
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
  transform: translateY(-2px);
}
:deep(.el-button--primary:disabled) {
  background: rgba(139, 69, 19, 0.3);
  border-color: rgba(255, 215, 0, 0.1);
  color: rgba(255, 215, 0, 0.4);
}
:deep(.el-link--primary)       { color: #ffd700; }
:deep(.el-link--primary:hover) { color: #ffed4a; }

/* ===== 漂浮图标 ===== */
.floating-elements {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.float-icon {
  position: absolute;
  font-size: 2rem;
  opacity: 0.15;
  filter: drop-shadow(0 0 10px rgba(255, 215, 0, 0.3));
  animation: floatUpDown 6s ease-in-out infinite;
}
.icon-1 { top: 10%; left: 5%;   animation-delay: 0s;   font-size: 2.5rem; }
.icon-2 { top: 20%; right: 8%;  animation-delay: 1s;   font-size: 1.8rem; }
.icon-3 { top: 45%; left: 3%;   animation-delay: 2s;   font-size: 2rem;   }
.icon-4 { top: 60%; right: 5%;  animation-delay: 0.5s; font-size: 2.2rem; }
.icon-5 { top: 75%; left: 8%;   animation-delay: 1.5s; font-size: 1.6rem; }
.icon-6 { top: 85%; right: 10%; animation-delay: 2.5s; font-size: 2rem;   }
.icon-7 { top: 30%; left: 10%;  animation-delay: 3s;   font-size: 1.5rem; }
.icon-8 { top: 50%; right: 3%;  animation-delay: 3.5s; font-size: 1.8rem; }

@keyframes floatUpDown {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%       { transform: translateY(-20px) rotate(5deg); }
}

/* ===== 角落装饰 ===== */
.corner-decor {
  position: absolute;
  width: 100px; height: 100px;
  pointer-events: none;
  z-index: 0;
}
.corner-decor::before,
.corner-decor::after {
  content: '';
  position: absolute;
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.3), transparent);
}
.corner-decor.top-left    { top: 20px; left: 20px; }
.corner-decor.top-left::before  { width: 60px; height: 2px; top: 0; left: 0; }
.corner-decor.top-left::after   { width: 2px; height: 60px; top: 0; left: 0; background: linear-gradient(180deg, rgba(255,215,0,0.3), transparent); }

.corner-decor.top-right   { top: 20px; right: 20px; }
.corner-decor.top-right::before { width: 60px; height: 2px; top: 0; right: 0; background: linear-gradient(270deg, rgba(255,215,0,0.3), transparent); }
.corner-decor.top-right::after  { width: 2px; height: 60px; top: 0; right: 0; background: linear-gradient(180deg, rgba(255,215,0,0.3), transparent); }

.corner-decor.bottom-left { bottom: 20px; left: 20px; }
.corner-decor.bottom-left::before  { width: 60px; height: 2px; bottom: 0; left: 0; }
.corner-decor.bottom-left::after   { width: 2px; height: 60px; bottom: 0; left: 0; background: linear-gradient(0deg, rgba(255,215,0,0.3), transparent); }

.corner-decor.bottom-right { bottom: 20px; right: 20px; }
.corner-decor.bottom-right::before { width: 60px; height: 2px; bottom: 0; right: 0; background: linear-gradient(270deg, rgba(255,215,0,0.3), transparent); }
.corner-decor.bottom-right::after  { width: 2px; height: 60px; bottom: 0; right: 0; background: linear-gradient(0deg, rgba(255,215,0,0.3), transparent); }

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  .float-icon   { font-size: 1.5rem; }
  .corner-decor { display: none; }
  .logo-icon    { font-size: 2.5rem; }
}
</style>
