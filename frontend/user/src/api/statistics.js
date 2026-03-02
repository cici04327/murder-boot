import request from '@/utils/request'

// 获取统计概览数据
export const getStatisticsOverview = () => {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

// 获取图表数据
export const getStatisticsCharts = (days = 7) => {
  return request({
    url: '/statistics/charts',
    method: 'get',
    params: { days }
  })
}

// 获取排行榜数据
export const getStatisticsRankings = (limit = 10) => {
  return request({
    url: '/statistics/rankings',
    method: 'get',
    params: { limit }
  })
}

// 获取实时数据
export const getStatisticsRealtime = (limit = 10) => {
  return request({
    url: '/statistics/realtime',
    method: 'get',
    params: { limit }
  })
}
