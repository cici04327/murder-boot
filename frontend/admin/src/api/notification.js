import { userService } from '@/utils/request'

/**
 * 通知API接口
 */

/**
 * 分页查询通知列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {boolean} params.onlyUnread - 是否只查询未读
 */
export function getNotificationPage(params) {
  return userService({
    url: '/notification/page',
    method: 'get',
    params
  })
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return userService({
    url: '/notification/unread-count',
    method: 'get'
  })
}

/**
 * 标记通知为已读
 * @param {number} id - 通知ID
 */
export function markAsRead(id) {
  return userService({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

/**
 * 标记所有通知为已读
 */
export function markAllAsRead() {
  return userService({
    url: '/notification/read-all',
    method: 'put'
  })
}

/**
 * 删除通知
 * @param {number} id - 通知ID
 */
export function deleteNotification(id) {
  return userService({
    url: `/notification/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除通知
 * @param {Array} ids - 通知ID列表
 */
export function batchDeleteNotifications(ids) {
  return userService({
    url: '/notification/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 批量标记为已读
 * @param {Array} ids - 通知ID列表
 */
export function batchMarkAsRead(ids) {
  return userService({
    url: '/notification/batch-read',
    method: 'put',
    data: ids
  })
}

/**
 * 获取通知详情
 * @param {number} id - 通知ID
 */
export function getNotificationDetail(id) {
  return userService({
    url: `/notification/${id}`,
    method: 'get'
  })
}

/**
 * 搜索通知
 * @param {Object} params - 查询参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export function searchNotifications(params) {
  return userService({
    url: '/notification/search',
    method: 'get',
    params
  })
}

/**
 * 获取通知统计信息
 */
export function getNotificationStatistics() {
  return userService({
    url: '/notification/statistics',
    method: 'get'
  })
}

/**
 * 清空已读通知
 */
export function clearReadNotifications() {
  return userService({
    url: '/notification/clear-read',
    method: 'delete'
  })
}
