import { userService } from '@/utils/request'

/**
 * 管理端通知API
 */

/**
 * 获取管理员通知列表
 * @param {Object} params - 查询参数
 */
export function getAdminNotifications(params) {
  return userService({
    url: '/admin/notification/list',
    method: 'get',
    params
  })
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return userService({
    url: '/admin/notification/unread-count',
    method: 'get'
  })
}

/**
 * 标记通知为已读
 * @param {number} id - 通知ID
 */
export function markAsRead(id) {
  return userService({
    url: `/admin/notification/${id}/read`,
    method: 'put'
  })
}

/**
 * 标记所有通知为已读
 */
export function markAllAsRead() {
  return userService({
    url: '/admin/notification/read-all',
    method: 'put'
  })
}

/**
 * 删除通知
 * @param {number} id - 通知ID
 */
export function deleteNotification(id) {
  return userService({
    url: `/admin/notification/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除通知
 * @param {Array} ids - 通知ID列表
 */
export function batchDeleteNotifications(ids) {
  return userService({
    url: '/admin/notification/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 获取通知详情
 * @param {number} id - 通知ID
 */
export function getNotificationDetail(id) {
  return userService({
    url: `/admin/notification/${id}`,
    method: 'get'
  })
}

/**
 * 获取通知统计信息
 */
export function getNotificationStatistics() {
  return userService({
    url: '/admin/notification/statistics',
    method: 'get'
  })
}
