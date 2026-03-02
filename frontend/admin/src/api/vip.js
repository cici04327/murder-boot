import { userService } from '@/utils/request'

/**
 * VIP管理API - 管理端
 * 注意：userService 使用 /user-api 前缀，网关会去掉 /user-api 并转发到用户服务
 * 所以这里的 URL 不需要 /user 前缀
 */

// 获取所有VIP套餐（包括下架的）
export const getAllPackages = (params) => {
  return userService({
    url: '/admin/vip/packages',
    method: 'get',
    params
  })
}

// 创建VIP套餐
export const createPackage = (data) => {
  return userService({
    url: '/admin/vip/packages',
    method: 'post',
    data
  })
}

// 更新VIP套餐
export const updatePackage = (data) => {
  return userService({
    url: '/admin/vip/packages',
    method: 'put',
    data
  })
}

// 删除VIP套餐
export const deletePackage = (id) => {
  return userService({
    url: `/admin/vip/packages/${id}`,
    method: 'delete'
  })
}

// 上架/下架套餐
export const updatePackageStatus = (id, status) => {
  return userService({
    url: `/admin/vip/packages/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 获取VIP用户列表
export const getVipUsers = (params) => {
  return userService({
    url: '/admin/vip/users',
    method: 'get',
    params
  })
}

// 赠送VIP
export const grantVip = (userId, days, level, reason) => {
  return userService({
    url: '/admin/vip/grant',
    method: 'post',
    params: { userId, days, level, reason }
  })
}

// 延长VIP
export const extendVip = (userId, days) => {
  return userService({
    url: '/admin/vip/extend',
    method: 'post',
    params: { userId, days }
  })
}

// VIP统计数据
export const getVipStatistics = () => {
  return userService({
    url: '/admin/vip/statistics',
    method: 'get'
  })
}

