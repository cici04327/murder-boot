<template>
  <div class="login-container">
    <div class="floating-elements">
      <span class="float-icon icon-1">🎭</span>
      <span class="float-icon icon-2">🔍</span>
      <span class="float-icon icon-3">🎪</span>
      <span class="float-icon icon-4">🗝️</span>
      <span class="float-icon icon-5">🕯️</span>
      <span class="float-icon icon-6">🃏</span>
      <span class="float-icon icon-7">🎬</span>
      <span class="float-icon icon-8">🧩</span>
    </div>

    <div class="corner-decor top-left"></div>
    <div class="corner-decor top-right"></div>
    <div class="corner-decor bottom-left"></div>
    <div class="corner-decor bottom-right"></div>

    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">🎭</div>
        <h2>欢迎登录</h2>
        <p>剧本杀管理后台</p>
        <div class="header-decoration">
          <span class="deco-line"></span>
          <span class="deco-diamond">◆</span>
          <span class="deco-line"></span>
        </div>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="currentRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
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
        <span>登录身份：</span>
        <el-link
          :type="loginType === 'admin' ? 'primary' : 'info'"
          :underline="false"
          @click="switchLoginType('admin')"
        >
          超级管理员
        </el-link>
        <span class="footer-divider">|</span>
        <el-link
          :type="loginType === 'store' ? 'primary' : 'info'"
          :underline="false"
          @click="switchLoginType('store')"
        >
          门店登录
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
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
  username: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const storeRules = {
  loginAccount: [{ required: true, message: '请输入门店账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const currentRules = computed(() => (loginType.value === 'admin' ? adminRules : storeRules))

const handleLoginTypeChange = () => {
  if (loginFormRef.value) {
    loginFormRef.value.clearValidate()
  }
  captchaVerified.value = false
  if (captchaRef.value) {
    captchaRef.value.reset()
  }
}

const switchLoginType = (type) => {
  if (loginType.value === type) return
  loginType.value = type
  handleLoginTypeChange()
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
  if (!loginFormRef.value) return

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
      return
    }

    ElMessage.error(res.msg || '登录失败')
    captchaVerified.value = false
    if (captchaRef.value) {
      captchaRef.value.reset()
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(255, 215, 0, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 69, 19, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(75, 0, 130, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse at 30% 70%, rgba(47, 79, 79, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 30%, rgba(25, 25, 112, 0.25) 0%, transparent 50%),
    linear-gradient(180deg, #0d0d0d 0%, #1a1a2e 25%, #16213e 50%, #1a1a2e 75%, #0d0d0d 100%);
  background-attachment: fixed;
}

.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(circle at 25% 25%, rgba(255, 215, 0, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(139, 0, 0, 0.05) 0%, transparent 50%);
  animation: mysteryGlow 15s ease-in-out infinite;
  pointer-events: none;
}

@keyframes mysteryGlow {
  0%,
  100% {
    transform: rotate(0deg) scale(1);
    opacity: 0.5;
  }

  50% {
    transform: rotate(180deg) scale(1.1);
    opacity: 0.8;
  }
}

.login-container::after {
  content: '';
  position: absolute;
  inset: 0;
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
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 3.5rem;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5));
  animation: logoGlow 3s ease-in-out infinite;
}

@keyframes logoGlow {
  0%,
  100% {
    filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.5));
    transform: scale(1);
  }

  50% {
    filter: drop-shadow(0 0 25px rgba(255, 215, 0, 0.8));
    transform: scale(1.05);
  }
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
  0%,
  100% {
    opacity: 0.6;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.3);
  }
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

.login-footer .footer-divider {
  margin: 0 8px;
  color: rgba(255, 255, 255, 0.3);
}

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

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #8b4513 0%, #a0522d 50%, #8b4513 100%);
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #ffd700;
  font-weight: 700;
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

:deep(.el-link--info) {
  color: rgba(255, 255, 255, 0.58);
}

:deep(.el-link--info:hover) {
  color: rgba(255, 255, 255, 0.82);
}

.floating-elements {
  position: absolute;
  inset: 0;
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

.icon-1 {
  top: 10%;
  left: 5%;
  animation-delay: 0s;
  font-size: 2.5rem;
}

.icon-2 {
  top: 20%;
  right: 8%;
  animation-delay: 1s;
  font-size: 1.8rem;
}

.icon-3 {
  top: 45%;
  left: 3%;
  animation-delay: 2s;
  font-size: 2rem;
}

.icon-4 {
  top: 60%;
  right: 5%;
  animation-delay: 0.5s;
  font-size: 2.2rem;
}

.icon-5 {
  top: 75%;
  left: 8%;
  animation-delay: 1.5s;
  font-size: 1.6rem;
}

.icon-6 {
  top: 85%;
  right: 10%;
  animation-delay: 2.5s;
  font-size: 2rem;
}

.icon-7 {
  top: 30%;
  left: 10%;
  animation-delay: 3s;
  font-size: 1.5rem;
}

.icon-8 {
  top: 50%;
  right: 3%;
  animation-delay: 3.5s;
  font-size: 1.8rem;
}

@keyframes floatUpDown {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }

  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

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

  .login-box {
    padding: 28px 22px;
  }
}
</style>
