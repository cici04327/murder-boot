import request from '@/utils/request'

// 获取门店列表（分页）
export const getStoreList = (params) => {
  return request({
    url: '/store/page',
    method: 'get',
    params
  })
}

// 高级查询门店列表（支持多条件和排序）
export const getStoreListAdvanced = (data) => {
  return request({
    url: '/store/page/advanced',
    method: 'post',
    data
  })
}

// 获取所有门店列表（不分页）
export const getAllStores = () => {
  return request({
    url: '/store/list',
    method: 'get'
  })
}

// 获取门店详情
export const getStoreDetail = (id) => {
  return request({
    url: `/store/${id}`,
    method: 'get'
  })
}

// 获取门店房间列表
export const getStoreRooms = (storeId) => {
  return request({
    url: `/store/room/list/${storeId}`,
    method: 'get'
  })
}

// 获取门店可用房间
export const getAvailableRooms = (storeId) => {
  return request({
    url: `/store/room/available/${storeId}`,
    method: 'get'
  })
}

// 获取门店评价
export const getStoreReviews = (params) => {
  return request({
    url: '/store/review/page',
    method: 'get',
    params
  })
}

// 添加门店评价
export const addStoreReview = (data) => {
  return request({
    url: '/store/review',
    method: 'post',
    data
  })
}

// 获取门店详细信息（包含房间等）
export const getStoreDetailInfo = (id) => {
  return request({
    url: `/store/detail/${id}`,
    method: 'get'
  })
}

// 获取推荐门店
export const getRecommendedStores = () => {
  return request({
    url: '/store/list/recommended',
    method: 'get'
  })
}

// 获取热门门店
export const getHotStores = () => {
  return request({
    url: '/store/list/hot',
    method: 'get'
  })
}

// 获取附近门店
export const getNearbyStores = (params) => {
  return request({
    url: '/store/nearby',
    method: 'get',
    params
  })
}
