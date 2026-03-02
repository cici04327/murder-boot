import request from '@/utils/request'

/**
 * VIP会员API
 * 注意：后端VipController路径是 /vip，所以这里直接用 /vip
 */

// 获取所有可用的VIP套餐
export const getVipPackages = () => {
  return request({
    url: '/vip/packages',
    method: 'get'
  })
}

// 获取用户VIP信息
export const getUserVipInfo = () => {
  return request({
    url: '/vip/info',
    method: 'get'
  })
}

// 开通VIP会员
export const purchaseVip = (packageId, paymentMethod) => {
  return request({
    url: '/vip/purchase',
    method: 'post',
    params: {
      packageId,
      paymentMethod
    }
  })
}

// 获取VIP权益详情
export const getVipBenefits = (level) => {
  return request({
    url: '/vip/benefits',
    method: 'get',
    params: { level }
  })
}

// 获取用户VIP历史记录
export const getVipHistory = (params) => {
  return request({
    url: '/vip/history',
    method: 'get',
    params
  })
}

// 计算VIP价格（含优惠）
export const calculateVipPrice = (packageId, couponId) => {
  return request({
    url: '/vip/calculate-price',
    method: 'get',
    params: { packageId, couponId }
  })
}

