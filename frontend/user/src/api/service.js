import request from '@/utils/request'

/** 发起转人工请求 */
export const createServiceSession = (data) => request.post('/service/session/create', data)

/** 查询当前活跃会话 */
export const getActiveSession = () => request.get('/service/session/active')

/** 用户发送消息 */
export const sendUserMessage = (sessionId, data) => request.post(`/service/session/${sessionId}/message`, data)

/** 获取会话消息列表 */
export const getSessionMessages = (sessionId) => request.get(`/service/session/${sessionId}/messages`)

/** 用户结束会话 */
export const closeSession = (sessionId) => request.post(`/service/session/${sessionId}/close?closerType=user`)

/** 用户评价会话 */
export const rateSession = (sessionId, data) => request.post(`/service/session/${sessionId}/rate`, data)
