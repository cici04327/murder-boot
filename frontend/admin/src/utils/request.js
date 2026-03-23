import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  // 添加重试配置
  retryDelay: 1000,
  retry: 3
})

// 用户服务专用实例（用于通知等用户相关功能）
// Spring Boot 单体应用中所有服务统一使用 /api 前缀
export const userService = axios.create({
  baseURL: '/api',
  timeout: 10000,
  // 添加重试配置
  retryDelay: 1000,
  retry: 3
})

// 预约服务专用实例
// Spring Boot 单体应用中所有服务统一使用 /api 前缀
export const reservationService = axios.create({
  baseURL: '/api',
  timeout: 10000,
  // 添加重试配置
  retryDelay: 1000,
  retry: 3
})

const clearAdminAuth = () => {
  localStorage.removeItem('admin-token')
  localStorage.removeItem('admin-login-type')
  localStorage.removeItem('admin-store-id')
  localStorage.removeItem('admin-current-store-id')
}

const redirectToAdminLogin = () => {
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

const handleAdminUnauthorized = (message = '登录已过期，请重新登录') => {
  ElMessage.error(message)
  clearAdminAuth()
  redirectToAdminLogin()
}

// 统一处理管理端请求：token + client-type + storeId
const applyAdminRequestMeta = (config) => {
  const token = localStorage.getItem('admin-token')
  if (token) {
    // Spring Boot 统一使用 token 请求头
    config.headers['token'] = token
  }

  // 标记管理端请求（用于后端识别 admin 客户端 + 选择管理员 JWT 密钥）
  config.headers['X-Client-Type'] = 'admin'
  
  // 获取登录类型和门店ID
  const loginType = localStorage.getItem('admin-login-type')
  const storeAdminStoreId = localStorage.getItem('admin-store-id')
  
  // 门店登录：在请求头中携带门店ID，用于后端权限控制
  if ((loginType === 'store' || loginType === 'staff') && storeAdminStoreId) {
    config.headers['X-Store-Id'] = storeAdminStoreId
  }

  // 处理 storeId 查询参数
  const method = (config.method || 'get').toLowerCase()
  const url = String(config.url || '')
  const allowStoreId = url.startsWith('/reservation') || url.startsWith('/admin/notification') || 
                       url.startsWith('/script') || url.startsWith('/store')

  if (method === 'get' && allowStoreId) {
    // 门店管理员：强制使用自己门店的ID
    if ((loginType === 'store' || loginType === 'staff') && storeAdminStoreId) {
      config.params = config.params || {}
      config.params.storeId = Number(storeAdminStoreId)
    }
  }
  
  // POST/PUT 请求也附加 storeId（门店管理员）
  if ((method === 'post' || method === 'put') && (loginType === 'store' || loginType === 'staff') && storeAdminStoreId) {
    if (config.data && typeof config.data === 'object' && !Array.isArray(config.data)) {
      config.data.storeId = Number(storeAdminStoreId)
    }
  }

  return config
}

// 请求拦截器
service.interceptors.request.use(
  config => {
    return applyAdminRequestMeta(config)
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 用户服务请求拦截器
userService.interceptors.request.use(
  config => {
    return applyAdminRequestMeta(config)
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 成功的状态码：1 或 200
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      
      // 401: 未授权，跳转登录页
      if (res.code === 401) {
        handleAdminUnauthorized(res.msg || '登录已过期，请重新登录')
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    
    return res
  },
  async error => {
    console.error('响应错误:', error)
    
    const config = error.config
    
    // 如果是网络错误且配置了重试
    if (error.code === 'ERR_NETWORK' && config && config.retry) {
      config.__retryCount = config.__retryCount || 0
      
      if (config.__retryCount < config.retry) {
        config.__retryCount++
        console.log(`请求失败，正在重试 (${config.__retryCount}/${config.retry})...`)
        
        // 延迟后重试
        await new Promise(resolve => setTimeout(resolve, config.retryDelay || 1000))
        return service.request(config)
      }
    }
    
    // 只在所有重试都失败后才显示错误消息
    if (error.response?.status === 401) {
      handleAdminUnauthorized()
    } else if (error.response?.status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接失败，请检查网络或稍后重试')
    } else if (!error.config?.__retryCount) {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

// 用户服务响应拦截器
userService.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      
      if (res.code === 401) {
        handleAdminUnauthorized(res.msg || '登录已过期，请重新登录')
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    
    return res
  },
  async error => {
    console.error('响应错误:', error)
    
    const config = error.config
    
    // 如果是网络错误且配置了重试
    if (error.code === 'ERR_NETWORK' && config && config.retry) {
      config.__retryCount = config.__retryCount || 0
      
      if (config.__retryCount < config.retry) {
        config.__retryCount++
        console.log(`用户服务请求失败，正在重试 (${config.__retryCount}/${config.retry})...`)
        
        // 延迟后重试
        await new Promise(resolve => setTimeout(resolve, config.retryDelay || 1000))
        return userService.request(config)
      }
    }
    
    // 只在所有重试都失败后才显示错误消息
    if (error.response?.status === 401) {
      handleAdminUnauthorized()
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接失败，请检查网络或稍后重试')
    } else if (!error.config?.__retryCount) {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

// 预约服务请求拦截器
reservationService.interceptors.request.use(
  config => {
    return applyAdminRequestMeta(config)
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 预约服务响应拦截器
reservationService.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== 1 && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      
      if (res.code === 401) {
        handleAdminUnauthorized(res.msg || '登录已过期，请重新登录')
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    
    return res
  },
  async error => {
    console.error('响应错误:', error)
    
    const config = error.config
    
    // 如果是网络错误且配置了重试
    if (error.code === 'ERR_NETWORK' && config && config.retry) {
      config.__retryCount = config.__retryCount || 0
      
      if (config.__retryCount < config.retry) {
        config.__retryCount++
        console.log(`预约服务请求失败，正在重试 (${config.__retryCount}/${config.retry})...`)
        
        // 延迟后重试
        await new Promise(resolve => setTimeout(resolve, config.retryDelay || 1000))
        return reservationService.request(config)
      }
    }
    
    // 只在所有重试都失败后才显示错误消息
    if (error.response?.status === 401) {
      handleAdminUnauthorized()
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接失败，请检查网络或稍后重试')
    } else if (!error.config?.__retryCount) {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

export default service
