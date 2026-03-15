import request from '@/utils/request'

// 分页查询 DM 列表
export const getDmPage = (params) => request({ url: '/dm/page', method: 'get', params })

// 查询门店 DM 列表（下拉选择用）
export const getDmList = (storeId) => request({ url: '/dm/list', method: 'get', params: { storeId } })

// 查询 DM 详情
export const getDmById = (id) => request({ url: `/dm/${id}`, method: 'get' })

// 新增 DM
export const addDm = (data) => request({ url: '/dm', method: 'post', data })

// 编辑 DM
export const updateDm = (data) => request({ url: '/dm', method: 'put', data })

// 删除 DM
export const deleteDm = (id) => request({ url: `/dm/${id}`, method: 'delete' })

// 更新状态
export const updateDmStatus = (id, status) => request({ url: '/dm/status', method: 'put', params: { id, status } })
