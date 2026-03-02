import request from '@/utils/request'

// 获取拼单列表（分页）
export const getGroupList = (params) => {
  return request({
    url: '/group/page',
    method: 'get',
    params
  })
}

// 获取热门拼单
export const getHotGroups = (limit = 10) => {
  return request({
    url: '/group/hot',
    method: 'get',
    params: { limit }
  })
}

// 获取拼单详情
export const getGroupDetail = (id) => {
  return request({
    url: `/group/${id}`,
    method: 'get'
  })
}

// 创建拼单
export const createGroup = (data) => {
  return request({
    url: '/group',
    method: 'post',
    data
  })
}

// 加入拼单
export const joinGroup = (id) => {
  return request({
    url: `/group/${id}/join`,
    method: 'post'
  })
}

// 退出拼单
export const leaveGroup = (id) => {
  return request({
    url: `/group/${id}/leave`,
    method: 'post'
  })
}

// 取消拼单（仅发起人）
export const cancelGroup = (id) => {
  return request({
    url: `/group/${id}/cancel`,
    method: 'post'
  })
}

// 获取我的拼单
export const getMyGroups = (params) => {
  return request({
    url: '/group/my',
    method: 'get',
    params
  })
}
