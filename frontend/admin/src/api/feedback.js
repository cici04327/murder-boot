import request from '@/utils/request'

/**
 * 分页查询留言列表
 */
export const getFeedbackList = (params) => {
  return request({
    url: '/feedback/page',
    method: 'get',
    params
  })
}

/**
 * 获取留言详情
 */
export const getFeedbackDetail = (id) => {
  return request({
    url: `/feedback/${id}`,
    method: 'get'
  })
}

/**
 * 回复留言
 */
export const replyFeedback = (id, replyContent) => {
  return request({
    url: `/feedback/${id}/reply`,
    method: 'put',
    params: { replyContent }
  })
}

/**
 * 更新留言状态
 */
export const updateFeedbackStatus = (id, status) => {
  return request({
    url: `/feedback/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除留言
 */
export const deleteFeedback = (id) => {
  return request({
    url: `/feedback/${id}`,
    method: 'delete'
  })
}
