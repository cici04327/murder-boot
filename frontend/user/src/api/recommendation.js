import request from '@/utils/request'

/**
 * 推荐系统API
 */

// 获取个性化推荐
export function getPersonalizedRecommendations(limit = 10) {
  return request({
    url: '/recommendation/personalized',
    method: 'get',
    params: { limit }
  })
}

// 获取协同过滤推荐（看了这个还看了...）
export function getCollaborativeRecommendations(scriptId, limit = 6) {
  return request({
    url: `/recommendation/collaborative/${scriptId}`,
    method: 'get',
    params: { limit }
  })
}

// 获取相似剧本推荐
export function getContentBasedRecommendations(scriptId, limit = 6) {
  return request({
    url: `/recommendation/similar/${scriptId}`,
    method: 'get',
    params: { limit }
  })
}

// 获取基于历史的推荐
export function getHistoryBasedRecommendations(limit = 10) {
  return request({
    url: '/recommendation/history',
    method: 'get',
    params: { limit }
  })
}

// 获取热门推荐
export function getHotRecommendations(rankingType = 1, limit = 10) {
  return request({
    url: '/recommendation/hot',
    method: 'get',
    params: { rankingType, limit }
  })
}

// 获取新品推荐
export function getNewScriptRecommendations(limit = 10) {
  return request({
    url: '/recommendation/new',
    method: 'get',
    params: { limit }
  })
}

// 记录浏览历史
export function recordBrowseHistory(targetType, targetId, duration = 0) {
  return request({
    url: '/recommendation/browse',
    method: 'post',
    params: { targetType, targetId, duration }
  })
}

// 记录推荐点击
export function recordRecommendationClick(scriptId) {
  return request({
    url: '/recommendation/click',
    method: 'post',
    params: { scriptId }
  })
}

// 获取推荐统计（管理端）
export function getRecommendationStats(days = 7) {
  return request({
    url: '/recommendation/admin/stats',
    method: 'get',
    params: { days }
  })
}

// 刷新热门榜单（管理端）
export function refreshHotRanking(rankingType) {
  return request({
    url: '/recommendation/admin/refresh-ranking',
    method: 'post',
    params: { rankingType }
  })
}

// AI增强推荐（规则引擎 + AI重排序 + 推荐理由）
export function getAiEnhancedRecommendations(limit = 10) {
  return request({
    url: '/recommendation/ai-enhanced',
    method: 'get',
    params: { limit },
    timeout: 60000  // AI调用可能较慢，设置60秒超时
  })
}

// AI用户画像分析
export function getAiUserProfile() {
  return request({
    url: '/recommendation/ai-profile',
    method: 'get',
    timeout: 60000
  })
}
