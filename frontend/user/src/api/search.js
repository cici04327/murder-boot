import request from '@/utils/request'

// 全局搜索（搜索剧本和门店）
export const globalSearch = (keyword, params) => {
  return request({
    url: '/search/global',
    method: 'get',
    params: {
      keyword,
      ...params
    }
  })
}

// 搜索剧本
export const searchScripts = (keyword, params) => {
  return request({
    url: '/script/page',
    method: 'get',
    params: {
      name: keyword,
      ...params
    }
  })
}

// 搜索门店
export const searchStores = (keyword, params) => {
  return request({
    url: '/store/page',
    method: 'get',
    params: {
      name: keyword,
      ...params
    }
  })
}

// 获取热门搜索
export const getHotSearches = () => {
  return request({
    url: '/search/hot',
    method: 'get'
  })
}

// 保存搜索历史（前端localStorage实现）
export const saveSearchHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  
  const history = getSearchHistory()
  // 移除重复项
  const newHistory = history.filter(item => item !== keyword)
  // 添加到开头
  newHistory.unshift(keyword)
  // 只保留最近10条
  const limitedHistory = newHistory.slice(0, 10)
  
  localStorage.setItem('search_history', JSON.stringify(limitedHistory))
}

// 获取搜索历史
export const getSearchHistory = () => {
  try {
    const history = localStorage.getItem('search_history')
    return history ? JSON.parse(history) : []
  } catch (error) {
    return []
  }
}

// 清空搜索历史
export const clearSearchHistory = () => {
  localStorage.removeItem('search_history')
}

// 删除单条搜索历史
export const removeSearchHistory = (keyword) => {
  const history = getSearchHistory()
  const newHistory = history.filter(item => item !== keyword)
  localStorage.setItem('search_history', JSON.stringify(newHistory))
}
