<template>
  <div class="login-container">
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
    
    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">🎭</div>
        <h2>欢迎登录</h2>
        <p>剧本杀在线预约平台</p>
        <div class="header-decoration">
          <span class="deco-line"></span>
          <span class="deco-diamond">◆</span>
          <span class="deco-line"></span>
        </div>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名/手机号"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
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
            @click="handleLogin"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)
const captchaVerified = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
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

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  // 检查验证码
  if (!captchaVerified.value) {
    ElMessage.warning('请先完成安全验证')
    return
  }
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const success = await userStore.login(loginForm)
        if (success) {
          ElMessage.success('登录成功')
          // 登录成功，跳转到之前的页面或首页
          const redirect = route.query.redirect || localStorage.getItem('user-default-page') || '/home'
          router.push(redirect)
        } else {
          // 登录失败，重置验证码
          captchaVerified.value = false
          if (captchaRef.value) {
            captchaRef.value.reset()
          }
        }
      } catch (error) {
        // 登录失败，重置验证码
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
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  /* 剧本杀主题深色神秘背景 */
  background: 
    /* 顶层：微光粒子效果 */
    radial-gradient(ellipse at 20% 80%, rgba(255, 215, 0, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 69, 19, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(75, 0, 130, 0.15) 0%, transparent 60%),
    /* 中层：神秘烟雾效果 */
    radial-gradient(ellipse at 30% 70%, rgba(47, 79, 79, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 30%, rgba(25, 25, 112, 0.25) 0%, transparent 50%),
    /* 底层：深色渐变基底 */
    linear-gradient(180deg, 
      #0d0d0d 0%, 
      #1a1a2e 25%, 
      #16213e 50%, 
      #1a1a2e 75%, 
      #0d0d0d 100%);
  background-attachment: fixed;
}

/* 神秘光晕动画效果 */
.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: 
    radial-gradient(circle at 25% 25%, rgba(255, 215, 0, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(192, 57, 43, 0.05) 0%, transparent 50%);
  animation: mysteryGlow 15s ease-in-out infinite;
  pointer-events: none;
}

@keyframes mysteryGlow {
  0%, 100% {
    transform: rotate(0deg) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: rotate(180deg) scale(1.1);
    opacity: 0.8;
  }
}

/* 漂浮尘埃效果 */
.login-container::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(circle, rgba(255, 255, 255, 0.15) 1px, transparent 1px),
    radial-gradient(circle, rgba(255, 215, 0, 0.1) 1px, transparent 1px);
  background-size: 50px 50px, 80px 80px;
  background-position: 0 0, 25px 25px;
  animation: dustFloat 20s linear infinite;
  pointer-events: none;
  opacity: 0.3;
}

@keyframes dustFloat {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50px);
  }
}

.login-box {
  background: rgba(30, 30, 40, 0.85);
  border-radius: 16px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.5),
    0 0 60px rgba(139, 69, 19, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  padding: 40px;
  width: 100%;
  max-width: 450px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 215, 0, 0.1);
}

/* 登录框复古边框装饰 */
.login-box::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  bottom: 10px;
  border: 1px solid rgba(255, 215, 0, 0.15);
  border-radius: 12px;
  pointer-events: none;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 28px;
  color: #ffd700;
  margin: 0 0 10px;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
  letter-spacing: 2px;
}

.login-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  letter-spacing: 1px;
}

.login-form {
  margin-top: 30px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.login-footer span {
  margin-right: 5px;
}

/* 输入框深色主题适配 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 215, 0, 0.2);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 215, 0, 0.4);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #ffd700;
  box-shadow: 0 0 10px rgba(255, 215, 0, 0.2);
}

:deep(.el-input__inner) {
  color: #fff;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.el-input__prefix) {
  color: rgba(255, 215, 0, 0.6);
}

/* 按钮剧本杀主题 */
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

:deep(.el-link--primary) {
  color: #ffd700;
}

:deep(.el-link--primary:hover) {
  color: #ffed4a;
}

/* ========== 剧本杀装饰元素 ========== */

/* 漂浮图标容器 */
.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

/* 漂浮图标基础样式 */
.float-icon {
  position: absolute;
  font-size: 2rem;
  opacity: 0.15;
  filter: drop-shadow(0 0 10px rgba(255, 215, 0, 0.3));
  animation: floatUpDown 6s ease-in-out infinite;
}

.icon-1 { top: 10%; left: 5%; animation-delay: 0s; font-size: 2.5rem; }
.icon-2 { top: 20%; right: 8%; animation-delay: 1s; font-size: 1.8rem; }
.icon-3 { top: 45%; left: 3%; animation-delay: 2s; font-size: 2rem; }
.icon-4 { top: 60%; right: 5%; animation-delay: 0.5s; font-size: 2.2rem; }
.icon-5 { top: 75%; left: 8%; animation-delay: 1.5s; font-size: 1.6rem; }
.icon-6 { top: 85%; right: 10%; animation-delay: 2.5s; font-size: 2rem; }
.icon-7 { top: 30%; left: 10%; animation-delay: 3s; font-size: 1.5rem; }
.icon-8 { top: 50%; right: 3%; animation-delay: 3.5s; font-size: 1.8rem; }

@keyframes floatUpDown {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/* 角落装饰 */
.corner-decor {
  position: absolute;
  width: 100px;
  height: 100px;
  pointer-events: none;
  z-index: 0;
}

.corner-decor::before,
.corner-decor::after {
  content: '';
  position: absolute;
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.3), transparent);
}

.corner-decor.top-left {
  top: 20px;
  left: 20px;
}
.corner-decor.top-left::before {
  width: 60px;
  height: 2px;
  top: 0;
  left: 0;
}
.corner-decor.top-left::after {
  width: 2px;
  height: 60px;
  top: 0;
  left: 0;
  background: linear-gradient(180deg, rgba(255, 215, 0, 0.3), transparent);
}

.corner-decor.top-right {
  top: 20px;
  right: 20px;
}
.corner-decor.top-right::before {
  width: 60px;
  height: 2px;
  top: 0;
  right: 0;
  background: linear-gradient(270deg, rgba(255, 215, 0, 0.3), transparent);
}
.corner-decor.top-right::after {
  width: 2px;
  height: 60px;
  top: 0;
  right: 0;
  background: linear-gradient(180deg, rgba(255, 215, 0, 0.3), transparent);
}

.corner-decor.bottom-left {
  bottom: 20px;
  left: 20px;
}
.corner-decor.bottom-left::before {
  width: 60px;
  height: 2px;
  bottom: 0;
  left: 0;
}
.corner-decor.bottom-left::after {
  width: 2px;
  height: 60px;
  bottom: 0;
  left: 0;
  background: linear-gradient(0deg, rgba(255, 215, 0, 0.3), transparent);
}

.corner-decor.bottom-right {
  bottom: 20px;
  right: 20px;
}
.corner-decor.bottom-right::before {
  width: 60px;
  height: 2px;
  bottom: 0;
  right: 0;
  background: linear-gradient(270deg, rgba(255, 215, 0, 0.3), transparent);
}
.corner-decor.bottom-right::after {
  width: 2px;
  height: 60px;
  bottom: 0;
  right: 0;
  background: linear-gradient(0deg, rgba(255, 215, 0, 0.3), transparent);
}

/* Logo图标 */
.logo-icon {
  font-size: 3.5rem;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5));
  animation: logoGlow 3s ease-in-out infinite;
}

@keyframes logoGlow {
  0%, 100% {
    filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 25px rgba(255, 215, 0, 0.8));
    transform: scale(1.05);
  }
}

/* 标题装饰线 */
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
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.3);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .float-icon {
    font-size: 1.5rem;
  }
  
  .corner-decor {
    display: none;
  }
  
  .logo-icon {
    font-size: 2.5rem;
  }
}
</style>
