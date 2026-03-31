import request from '@/utils/request'

export const getStoreEmployeePage = (params) => request({ url: '/store/employee/page', method: 'get', params })

export const getStoreEmployeeById = (id) => request({ url: `/store/employee/${id}`, method: 'get' })

export const addStoreEmployee = (data) => request({ url: '/store/employee', method: 'post', data })

export const updateStoreEmployee = (data) => request({ url: '/store/employee', method: 'put', data })

export const deleteStoreEmployee = (id) => request({ url: `/store/employee/${id}`, method: 'delete' })

export const updateStoreEmployeeStatus = (id, status) =>
  request({ url: `/store/employee/status/${id}`, method: 'put', params: { status } })

export const resetStoreEmployeePassword = (id, newPassword) =>
  request({ url: `/store/employee/reset-password/${id}`, method: 'put', params: { newPassword } })

export const getStoreEmployeeLogPage = (params) =>
  request({ url: '/store/employee/log/page', method: 'get', params })
