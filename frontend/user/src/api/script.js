import request from '@/utils/request'

// 获取剧本列表
export const getScriptList = (params) => {
  // 清理空参数，避免发送 null 或 undefined
  const cleanParams = {}
  Object.keys(params).forEach(key => {
    if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
      cleanParams[key] = params[key]
    }
  })
  
  return request({
    url: '/script/page',
    method: 'get',
    params: cleanParams
  })
}

// 获取剧本详情
export const getScriptDetail = (id) => {
  return request({
    url: `/script/${id}`,
    method: 'get'
  })
}

// 获取剧本分类
export const getScriptCategories = () => {
  return request({
    url: '/script/category',
    method: 'get'
  })
}

// 获取剧本角色
export const getScriptRoles = (scriptId) => {
  return request({
    url: `/script/${scriptId}/roles`,
    method: 'get'
  })
}

// 添加剧本评价
export const addScriptReview = (data) => {
  return request({
    url: '/script/review',
    method: 'post',
    data
  })
}

// 获取剧本评价列表
export const getScriptReviews = (params) => {
  return request({
    url: '/script/review/page',
    method: 'get',
    params
  })
}

// ==================== 收藏功能 ====================

// 收藏剧本
export const favoriteScript = (scriptId) => {
  return request({
    url: `/script/favorite/${scriptId}`,
    method: 'post'
  })
}

// 取消收藏剧本
export const unfavoriteScript = (scriptId) => {
  return request({
    url: `/script/favorite/${scriptId}`,
    method: 'delete'
  })
}

// 检查收藏状态
export const checkScriptFavoriteStatus = (scriptId) => {
  return request({
    url: `/script/favorite/${scriptId}/status`,
    method: 'get'
  })
}

// 获取收藏的剧本列表
export const getFavoriteScripts = (params) => {
  return request({
    url: '/script/favorite/list',
    method: 'get',
    params
  })
}

// 获取收藏数量
export const getFavoriteCount = () => {
  return request({
    url: '/script/favorite/count',
    method: 'get'
  })
}

// ==================== 首页推荐 ====================

// 获取热门剧本
export const getHotScripts = (limit = 8) => {
  return request({
    url: '/script/list/hot',
    method: 'get',
    params: { limit }
  })
}

// 获取推荐剧本
export const getRecommendedScripts = (limit = 4) => {
  return request({
    url: '/script/list/recommended',
    method: 'get',
    params: { limit }
  })
}
