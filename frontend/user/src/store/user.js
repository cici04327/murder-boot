import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username)
  const avatar = computed(() => userInfo.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
  
  // 登录
  const login = async (loginForm) => {
    try {
      const res = await loginApi(loginForm)
      if (res.code === 1 || res.code === 200) {
        const { token: newToken, ...user } = res.data
        token.value = newToken
        userInfo.value = user
        
        // 保存到本地存储
        localStorage.setItem('token', newToken)
        localStorage.setItem('userInfo', JSON.stringify(user))
        
        ElMessage.success('登录成功')
        return true
      }
      return false
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }
  
  // 注册
  const register = async (registerForm) => {
    try {
      const res = await registerApi(registerForm)
      if (res.code === 1 || res.code === 200) {
        ElMessage.success('注册成功，请登录')
        return true
      }
      return false
    } catch (error) {
      console.error('注册失败:', error)
      return false
    }
  }
  
  // 获取用户信息
  const loadUserInfo = async () => {
    if (!userId.value) {
      console.warn('用户ID不存在，无法加载用户信息')
      return
    }
    try {
      const res = await getUserInfo(userId.value)
      if (res.code === 1 || res.code === 200) {
        userInfo.value = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }
  
  // 从本地存储加载用户信息
  const loadUserFromStorage = () => {
    const savedToken = localStorage.getItem('token')
    const savedUserInfo = localStorage.getItem('userInfo')
    
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  }
  
  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
  }
  
  // 更新用户信息
  const updateUserInfo = (newInfo) => {
    userInfo.value = { ...userInfo.value, ...newInfo }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    userId,
    username,
    nickname,
    avatar,
    login,
    register,
    logout,
    loadUserInfo,
    loadUserFromStorage,
    updateUserInfo
  }
})
