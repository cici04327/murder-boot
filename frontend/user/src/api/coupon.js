import request from '@/utils/request'

// 获取优惠券列表（分页）
export const getCouponList = (params) => {
  return request({
    url: '/coupon/page',
    method: 'get',
    params
  })
}

// 获取我的优惠券列表
export const getMyCoupons = (params) => {
  return request({
    url: '/coupon/user',
    method: 'get',
    params
  })
}

// 获取可领取的优惠券列表
export const getAvailableCoupons = () => {
  return request({
    url: '/coupon/page',
    method: 'get',
    params: { status: 1 }
  })
}

// 领取优惠券
export const receiveCoupon = (couponId) => {
  return request({
    url: '/coupon/receive',
    method: 'post',
    params: { couponId }
  })
}

// 使用优惠券
export const useCoupon = (userCouponId, orderId) => {
  return request({
    url: '/coupon/use',
    method: 'put',
    params: { userCouponId, orderId }
  })
}

// 计算优惠金额
export const calculateDiscount = (userCouponId, orderAmount) => {
  return request({
    url: '/coupon/calculate',
    method: 'get',
    params: { userCouponId, orderAmount }
  })
}

// 退还优惠券
export const refundCoupon = (orderId) => {
  return request({
    url: '/coupon/refund',
    method: 'put',
    params: { orderId }
  })
}

// 获取预约可用的优惠券
export const getAvailableCouponsForReservation = (reservationId) => {
  return request({
    url: `/coupon/available/reservation/${reservationId}`,
    method: 'get'
  })
}

// 积分兑换优惠券
export const exchangeCoupon = (couponId, points) => {
  return request({
    url: '/user/points/exchange-coupon',
    method: 'post',
    params: { couponId, points }
  })
}