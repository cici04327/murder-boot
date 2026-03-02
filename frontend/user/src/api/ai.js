import request from '@/utils/request'

/**
 * AI客服API
 */

/**
 * 发送消息给AI
 * @param {Object} params
 * @param {String} params.message - 用户消息
 * @param {Array} params.history - 对话历史
 * @param {String} params.sessionId - 会话ID
 */
export const sendMessage = (params) => {
  return request.post('/ai/chat', params)
}

/**
 * 获取AI推荐
 * @param {String} type - 推荐类型（script/store等）
 */
export const getAIRecommendation = (type) => {
  return request.get(`/ai/recommend/${type}`)
}

/**
 * 记录客服对话
 * @param {Object} params
 */
export const logConversation = (params) => {
  return request.post('/ai/log', params)
}

/**
 * 获取常见问题
 */
export const getFrequentQuestions = () => {
  return request.get('/ai/faq')
}

/**
 * 反馈AI回答质量
 * @param {Object} params
 * @param {String} params.messageId - 消息ID
 * @param {Number} params.rating - 评分(1-5)
 * @param {String} params.comment - 评论
 */
export const submitFeedback = (params) => {
  return request.post('/ai/feedback', params)
}
