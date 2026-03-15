import request from '@/utils/request'

/**
 * 剧本排期管理 API
 * 对应后端 ScriptScheduleController，路径前缀 /script/schedule
 */

// 实时冲突检测
export const checkScheduleConflict = (storeId, roomId, scheduleDate, startTime, endTime, excludeId) => {
  return request.get('/script/schedule/check-conflict', {
    params: { storeId, roomId, scheduleDate, startTime, endTime, excludeId }
  })
}

// 门店营收日报
export const getStoreDailyReport = (storeId) => {
  return request.get('/statistics/store-daily-report', { params: { storeId } })
}

// 查询门店某日期排期列表（含剧本名、房间名）
export const getScheduleList = (storeId, date) => {
  return request({
    url: '/script/schedule/list',
    method: 'get',
    params: { storeId, date }
  })
}

// 查询门店日期范围排期列表
export const getScheduleRange = (storeId, startDate, endDate) => {
  return request({
    url: '/script/schedule/range',
    method: 'get',
    params: { storeId, startDate, endDate }
  })
}

// 新增单条排期
export const addSchedule = (data) => {
  return request({
    url: '/script/schedule',
    method: 'post',
    data
  })
}

// 批量新增排期
export const batchAddSchedule = (list) => {
  return request({
    url: '/script/schedule/batch',
    method: 'post',
    data: list
  })
}

// 批量生成排期（按日期范围 × 时间段模板）
// data: { storeId, scriptId, roomId, startDate, endDate, timeSlots: ['10:00:00-14:00:00'], maxPlayers }
export const generateSchedules = (data) => {
  return request({
    url: '/script/schedule/generate',
    method: 'post',
    data
  })
}

// 更新排期
export const updateSchedule = (data) => {
  return request({
    url: '/script/schedule',
    method: 'put',
    data
  })
}

// 更新排期状态（1=开启 0=关闭/已满 2=暂停）
export const updateScheduleStatus = (id, status) => {
  return request({
    url: '/script/schedule/status',
    method: 'put',
    params: { id, status }
  })
}

// 删除排期（逻辑删除）
export const deleteSchedule = (id) => {
  return request({
    url: `/script/schedule/${id}`,
    method: 'delete'
  })
}

// 查询排期详情
export const getScheduleById = (id) => {
  return request({
    url: `/script/schedule/${id}`,
    method: 'get'
  })
}

// 查询可约场次（用户端/管理端均可用）
// params: { scriptId?, storeId?, days? }
export const getAvailableSchedules = (params) => {
  return request({
    url: '/script/schedule/available',
    method: 'get',
    params
  })
}
