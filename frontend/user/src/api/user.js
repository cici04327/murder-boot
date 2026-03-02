import request from '@/utils/request'

// 用户登录
export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 用户注册
export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

// 获取用户信息（通过ID）
export const getUserInfo = (id) => {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return request({
    url: '/user',
    method: 'put',
    data
  })
}

// 修改密码
export const updatePassword = (data) => {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

// 上传头像
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/upload/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// ==================== 地址管理 ====================

// 获取用户地址列表
export const getUserAddresses = (userId) => {
  return request({
    url: `/user/address/list/${userId}`,
    method: 'get'
  })
}

// 添加用户地址
export const addUserAddress = (data) => {
  return request({
    url: '/user/address',
    method: 'post',
    data
  })
}

// 更新用户地址
export const updateUserAddress = (data) => {
  return request({
    url: '/user/address',
    method: 'put',
    data
  })
}

// 删除用户地址
export const deleteUserAddress = (id) => {
  return request({
    url: `/user/address/${id}`,
    method: 'delete'
  })
}

// 设置默认地址
export const setDefaultAddress = (id, userId) => {
  return request({
    url: `/user/address/${id}/default`,
    method: 'put',
    params: { userId }
  })
}

// 获取默认地址
export const getDefaultAddress = (userId) => {
  return request({
    url: `/user/address/default/${userId}`,
    method: 'get'
  })
}

// ==================== 积分管理 ====================

// 获取当前用户积分信息（含统计）
export const getUserPoints = () => {
  return request({
    url: '/user/points/info',
    method: 'get'
  })
}

// 获取当前用户积分记录
export const getPointsRecords = (params) => {
  return request({
    url: '/user/points/records',
    method: 'get',
    params
  })
}

// 每日签到
export const signIn = () => {
  return request({
    url: '/user/points/sign-in',
    method: 'post'
  })
}

// 获取积分趋势
export const getPointsTrend = (period = 'week') => {
  return request({
    url: '/user/points/trend',
    method: 'get',
    params: { period }
  })
}

// 兑换优惠券
export const exchangeCoupon = (couponId, points) => {
  return request({
    url: '/user/points/exchange-coupon',
    method: 'post',
    params: { couponId, points }
  })
}

// 完善资料任务
export const completeProfileTask = () => {
  return request({
    url: '/user/points/task/complete-profile',
    method: 'post'
  })
}

// 收藏剧本任务（检查是否达到条件）
export const checkFavoriteTask = () => {
  return request({
    url: '/user/points/task/check-favorite',
    method: 'post'
  })
}

// 检查任务完成状态
export const checkTaskStatus = () => {
  return request({
    url: '/user/points/task/status',
    method: 'get'
  })
}

// 获取任务列表
export const getPointsTasks = () => {
  return request({
    url: '/user/points/tasks',
    method: 'get'
  })
}

// 生成邀请码
export const generateInviteCode = () => {
  return request({
    url: '/user/invite/generate',
    method: 'post'
  })
}

// 获取邀请信息
export const getInviteInfo = () => {
  return request({
    url: '/user/invite/info',
    method: 'get'
  })
}

// ==================== 消息通知 ====================

// 获取用户通知列表
export const getUserNotifications = (params) => {
  return request({
    url: '/notification/page',
    method: 'get',
    params
  })
}

// 标记通知为已读
export const markNotificationRead = (id) => {
  return request({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

// 标记所有通知为已读
export const markAllNotificationsRead = () => {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

// 删除通知
export const deleteNotification = (id) => {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}

// 清空所有通知
export const clearAllNotifications = () => {
  return request({
    url: '/notification/clear-read',
    method: 'delete'
  })
}

// 获取未读通知数量
export const getUnreadCount = () => {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

// 获取通知统计
export const getNotificationStatistics = () => {
  return request({
    url: '/notification/statistics',
    method: 'get'
  })
}

// ==================== 账号设置 ====================

// 发送手机验证码
export const sendPhoneCode = (phone) => {
  return request({
    url: '/user/phone/code',
    method: 'post',
    data: { phone }
  })
}

// 绑定/更换手机号
export const bindPhone = (data) => {
  return request({
    url: '/user/phone/bind',
    method: 'post',
    data
  })
}

// 发送邮箱验证码
export const sendEmailCode = (email) => {
  return request({
    url: '/user/email/code',
    method: 'post',
    data: { email }
  })
}

// 绑定/更换邮箱
export const bindEmail = (data) => {
  return request({
    url: '/user/email/bind',
    method: 'post',
    data
  })
}

// 实名认证
export const realNameVerify = (data) => {
  return request({
    url: '/user/real-name/verify',
    method: 'post',
    data
  })
}

// 获取登录日志
export const getLoginLogs = (params) => {
  return request({
    url: '/user/login-logs',
    method: 'get',
    params
  })
}

// 获取隐私设置
export const getPrivacySettings = () => {
  return request({
    url: '/user/settings/privacy',
    method: 'get'
  })
}

// 更新隐私设置
export const updatePrivacySettings = (data) => {
  return request({
    url: '/user/settings/privacy',
    method: 'put',
    data
  })
}

// 获取通知设置
export const getNotificationSettings = () => {
  return request({
    url: '/user/settings/notification',
    method: 'get'
  })
}

// 更新通知设置
export const updateNotificationSettings = (data) => {
  return request({
    url: '/user/settings/notification',
    method: 'put',
    data
  })
}

// 获取偏好设置
export const getPreferenceSettings = () => {
  return request({
    url: '/user/settings/preference',
    method: 'get'
  })
}

// 更新偏好设置
export const updatePreferenceSettings = (data) => {
  return request({
    url: '/user/settings/preference',
    method: 'put',
    data
  })
}

// 清空浏览历史
export const clearBrowsingHistory = () => {
  return request({
    url: '/user/history/clear',
    method: 'delete'
  })
}

// 注销账号
export const deactivateAccount = (data) => {
  return request({
    url: '/user/deactivate',
    method: 'post',
    data
  })
}

// 获取账号统计信息
export const getAccountStatistics = () => {
  return request({
    url: '/user/statistics',
    method: 'get'
  })
}

// 获取任务完成状态
export const getTasksStatus = () => {
  return request({
    url: '/user/points/tasks-status',
    method: 'get'
  })
}