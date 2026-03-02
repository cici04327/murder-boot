import request from '@/utils/request'

// 获取优惠券列表（分页）
export const getCouponList = (params) => {
  return request({
    url: '/coupon/page',
    method: 'get',
    params
  })
}

// 获取优惠券详情
export const getCouponDetail = (id) => {
  return request({
    url: `/coupon/${id}`,
    method: 'get'
  })
}

// 获取优惠券统计信息
export const getCouponStatistics = (id) => {
  return request({
    url: `/coupon/${id}/statistics`,
    method: 'get'
  })
}

// 新增优惠券
export const addCoupon = (data) => {
  return request({
    url: '/coupon',
    method: 'post',
    data
  })
}

// 更新优惠券
export const updateCoupon = (data) => {
  return request({
    url: '/coupon',
    method: 'put',
    data
  })
}

// 删除优惠券
export const deleteCoupon = (id) => {
  return request({
    url: `/coupon/${id}`,
    method: 'delete'
  })
}

// 更新优惠券状态
export const updateCouponStatus = (id, status) => {
  return request({
    url: `/coupon/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 批量过期优惠券
export const expireCoupons = () => {
  return request({
    url: '/coupon/expire',
    method: 'post'
  })
}
