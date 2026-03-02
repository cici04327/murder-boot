import request from '@/utils/request'

// 创建预约
export const createReservation = (data) => {
  return request({
    url: '/reservation',
    method: 'post',
    data
  })
}

// 获取预约详情
export const getReservationDetail = (id) => {
  return request({
    url: `/reservation/${id}`,
    method: 'get'
  })
}

// 根据预约编号获取预约详情
export const getReservationByNo = (reservationNo) => {
  return request({
    url: `/reservation/no/${reservationNo}`,
    method: 'get'
  })
}

// 获取我的预约列表（分页）
export const getMyReservations = (params) => {
  return request({
    url: '/reservation/page',
    method: 'get',
    params
  })
}

// 取消预约
export const cancelReservation = (id, reason) => {
  return request({
    url: `/reservation/${id}/cancel`,
    method: 'put',
    params: { reason }
  })
}

// 确认预约
export const confirmReservation = (id) => {
  return request({
    url: `/reservation/${id}/confirm`,
    method: 'put'
  })
}

// 支付预约
export const payReservation = (id) => {
  return request({
    url: `/reservation/${id}/pay`,
    method: 'put'
  })
}

// 创建支付订单
export const createPayment = (reservationId, paymentMethod) => {
  return request({
    url: '/reservation/payment/create',
    method: 'post',
    params: { reservationId, paymentMethod }
  })
}

// 查询支付状态
export const queryPaymentStatus = (reservationId) => {
  return request({
    url: `/reservation/payment/status/${reservationId}`,
    method: 'get'
  })
}

// 完成预约
export const completeReservation = (id) => {
  return request({
    url: `/reservation/${id}/complete`,
    method: 'put'
  })
}

// 获取即将开始的预约
export const getUpcomingReservations = (hours = 2) => {
  return request({
    url: '/reservation/upcoming',
    method: 'get',
    params: { hours }
  })
}

// 检查房间可用性
export const checkRoomAvailability = (params) => {
  return request({
    url: '/reservation/check-availability',
    method: 'get',
    params
  })
}

// 修改预约
export const updateReservation = (id, data) => {
  return request({
    url: `/reservation/${id}`,
    method: 'put',
    data
  })
}
