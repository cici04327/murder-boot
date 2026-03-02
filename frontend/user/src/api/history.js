import request from '@/utils/request'

/**
 * 浏览历史相关API
 */

/**
 * 分页获取浏览历史
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页大小
 * @param {number} params.days - 天数筛选（1=今天，7=本周，30=本月）
 * @param {number} params.targetType - 目标类型（1=剧本，2=门店）
 */
export function getBrowseHistory(params) {
  return request({
    url: '/user/browse-history',
    method: 'get',
    params
  })
}

/**
 * 记录浏览历史
 * @param {number} targetType - 目标类型（1=剧本，2=门店）
 * @param {number} targetId - 目标ID
 * @param {number} duration - 浏览时长（秒）
 */
export function recordBrowseHistory(targetType, targetId, duration = 0) {
  return request({
    url: '/user/browse-history',
    method: 'post',
    params: { targetType, targetId, duration }
  })
}

/**
 * 删除单条浏览记录
 * @param {number} id - 记录ID
 */
export function deleteBrowseHistory(id) {
  return request({
    url: `/user/browse-history/${id}`,
    method: 'delete'
  })
}

/**
 * 清空所有浏览历史
 */
export function clearBrowseHistory() {
  return request({
    url: '/user/browse-history/clear',
    method: 'delete'
  })
}

/**
 * 获取浏览历史统计
 */
export function getBrowseHistoryStats() {
  return request({
    url: '/user/browse-history/stats',
    method: 'get'
  })
}

/**
 * 浏览历史本地存储工具（备用方案）
 */
const LOCAL_STORAGE_KEY = 'browse_history'
const MAX_LOCAL_HISTORY = 100

export const localHistoryUtils = {
  /**
   * 获取本地浏览历史
   */
  getLocalHistory() {
    try {
      const history = localStorage.getItem(LOCAL_STORAGE_KEY)
      return history ? JSON.parse(history) : []
    } catch (e) {
      console.error('获取本地浏览历史失败:', e)
      return []
    }
  },

  /**
   * 添加本地浏览记录
   */
  addLocalHistory(item) {
    try {
      let history = this.getLocalHistory()
      
      // 移除重复项
      history = history.filter(h => 
        !(h.targetType === item.targetType && h.targetId === item.targetId)
      )
      
      // 添加到开头
      history.unshift({
        ...item,
        id: Date.now(),
        browseTime: new Date().toISOString().replace('T', ' ').slice(0, 19)
      })
      
      // 限制数量
      if (history.length > MAX_LOCAL_HISTORY) {
        history = history.slice(0, MAX_LOCAL_HISTORY)
      }
      
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(history))
    } catch (e) {
      console.error('保存本地浏览历史失败:', e)
    }
  },

  /**
   * 删除本地浏览记录
   */
  removeLocalHistory(id) {
    try {
      let history = this.getLocalHistory()
      history = history.filter(h => h.id !== id)
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(history))
    } catch (e) {
      console.error('删除本地浏览历史失败:', e)
    }
  },

  /**
   * 清空本地浏览历史
   */
  clearLocalHistory() {
    try {
      localStorage.removeItem(LOCAL_STORAGE_KEY)
    } catch (e) {
      console.error('清空本地浏览历史失败:', e)
    }
  }
}
