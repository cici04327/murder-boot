import request from '@/utils/request'

/** 获取会话列表 */
export const listSessions = (params) => request.get('/service/admin/sessions', { params })

/** 接入会话 */
export const acceptSession = (sessionId) => request.post(`/service/admin/session/${sessionId}/accept`)

/** 管理员发送消息 */
export const sendAdminMessage = (sessionId, data) => request.post(`/service/admin/session/${sessionId}/message`, data)

/** 管理员结束会话 */
export const closeSessionByAdmin = (sessionId) => request.post(`/service/admin/session/${sessionId}/close`)

/** 获取会话消息列表 */
export const getSessionMessages = (sessionId) => request.get(`/service/session/${sessionId}/messages`)

/** 获取等待中的会话数量 */
export const countWaiting = () => request.get('/service/admin/waiting-count')
