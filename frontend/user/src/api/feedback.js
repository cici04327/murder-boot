import request from '@/utils/request'

/**
 * 提交留言
 * @param {Object} data - 留言数据 { name, contact, subject, message }
 */
export const submitFeedback = (data) => {
  return request({
    url: '/feedback/submit',
    method: 'post',
    data
  })
}

/**
 * 获取用户的留言列表
 * @param {Object} params - 分页参数 { page, pageSize }
 */
export const getUserFeedbacks = (params) => {
  return request({
    url: '/feedback/user',
    method: 'get',
    params
  })
}

/**
 * 获取留言详情
 * @param {Number} id - 留言ID
 */
export const getFeedbackDetail = (id) => {
  return request({
    url: `/feedback/${id}`,
    method: 'get'
  })
}
