import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000  // AI对话可能需要较长时间，增加超时时间到30秒
})

const silentErrors = [
  '/script/review/page',        // 评价列表可能为空
  '/script/favorite/',          // 收藏状态检查
  '/article/comments',          // 评论列表可能为空
  '/user/points/info',          // 积分信息页会在页面内自行重试和提示
  '/user/points/exchange-coupon' // 积分页会对兑换限制做业务提示
]

const isSilentRequest = (url = '') => silentErrors.some(path => url.includes(path))

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    
    // 添加token - 后端拦截器期望的请求头名称是 'token'
    if (userStore.token) {
      config.headers['token'] = userStore.token
    }

    // 拦截 URL 中含有 undefined 的请求，避免后端类型转换报错
    if (config.url && config.url.includes('undefined')) {
      console.warn('请求被拦截：URL 中含有 undefined，已取消请求:', config.url)
      return Promise.reject(new Error('请求参数无效（undefined）'))
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    const url = response.config?.url || ''
    
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    // 否则的话抛出错误
    if (res.code === 1 || res.code === 200) {
      return res
    } else {
      const errorMsg = res.msg || res.message || '请求失败'
      if (!isSilentRequest(url)) {
        ElMessage.error(errorMsg)
      }
      return Promise.reject(new Error(errorMsg))
    }
  },
  error => {
    // 统一记录更详细的错误信息
    const method = error.config?.method?.toUpperCase?.() || 'GET'
    const url = error.config?.url || ''
    console.error(`响应错误: ${method} ${url}`, error)

    // 静默处理的错误（不显示错误消息）
    const isSilent = isSilentRequest(url)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401: {
          const userStore = useUserStore()
          if (userStore.isLoggedIn) {
            // 已登录用户 token 过期，才提示并跳转
            if (!isSilent) {
              ElMessage.warning('登录已过期，请重新登录')
            }
            userStore.logout()
            router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
          }
          // 未登录游客访问返回401，静默处理，不提示不跳转
          break
        }
        case 403:
          if (!isSilent) {
            ElMessage.error('没有权限访问')
          }
          break
        case 404:
          // 404 错误静默处理，只在控制台显示
          console.warn('资源不存在:', url)
          break
        case 500:
          if (!isSilent) {
            ElMessage.error('服务器错误，请稍后重试')
          }
          break
        default:
          if (!isSilent) {
            const errorMsg = (data && (data.msg || data.message)) || '请求失败'
            ElMessage.error(errorMsg)
          }
      }
    } else if (error.message.includes('timeout')) {
      if (!isSilent) {
        ElMessage.error('请求超时，请检查网络')
      }
    } else if (error.code === 'ERR_NETWORK' || error.code === 'ERR_CONNECTION_REFUSED' || error.message.includes('Network Error')) {
      // 对 GET 请求做有限重试，缓解偶发现象（例如代理握手失败、服务暂时不可用）
      const config = error.config || {}
      config.__retryCount = config.__retryCount || 0
      const maxRetries = 3  // 增加重试次数
      if ((config.method || 'get').toLowerCase() === 'get' && config.__retryCount < maxRetries) {
        config.__retryCount += 1
        const delay = 500 * config.__retryCount  // 增加重试延迟
        console.warn(`网络错误，${delay}ms 后重试(${config.__retryCount}/${maxRetries}):`, config.url)
        return new Promise(resolve => setTimeout(resolve, delay)).then(() => request(config))
      }
      
      // 重试失败后的友好提示
      if (!isSilent) {
        if (error.code === 'ERR_CONNECTION_REFUSED') {
          ElMessage.error({
            message: '无法连接到服务器，请确认后端服务已启动',
            duration: 5000
          })
          console.error('后端服务连接失败，请检查：')
          console.error('1. 后端服务是否运行 (端口 8080)')
          console.error('2. 可以运行 "node check-services.js" 检查服务状态')
        } else {
          ElMessage.error('网络连接异常，请检查网络或稍后重试')
        }
      }
    } else {
      if (!isSilent) {
        ElMessage.error('请求失败，请稍后重试')
      }
    }
    
    return Promise.reject(error)
  }
)

export default request
