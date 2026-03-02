/**
 * 搜索历史管理工具
 */

const STORAGE_KEY = 'search_history'
const MAX_HISTORY = 10 // 最多保存10条历史记录

/**
 * 获取搜索历史
 * @returns {Array<string>}
 */
export function getSearchHistory() {
  try {
    const history = localStorage.getItem(STORAGE_KEY)
    return history ? JSON.parse(history) : []
  } catch (error) {
    console.error('获取搜索历史失败:', error)
    return []
  }
}

/**
 * 添加搜索历史
 * @param {string} keyword - 搜索关键词
 */
export function addSearchHistory(keyword) {
  if (!keyword || !keyword.trim()) return
  
  try {
    let history = getSearchHistory()
    
    // 移除重复项
    history = history.filter(item => item !== keyword)
    
    // 添加到开头
    history.unshift(keyword)
    
    // 限制数量
    if (history.length > MAX_HISTORY) {
      history = history.slice(0, MAX_HISTORY)
    }
    
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history))
  } catch (error) {
    console.error('添加搜索历史失败:', error)
  }
}

/**
 * 删除单条搜索历史
 * @param {string} keyword - 要删除的关键词
 */
export function removeSearchHistory(keyword) {
  try {
    let history = getSearchHistory()
    history = history.filter(item => item !== keyword)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history))
  } catch (error) {
    console.error('删除搜索历史失败:', error)
  }
}

/**
 * 清空搜索历史
 */
export function clearSearchHistory() {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch (error) {
    console.error('清空搜索历史失败:', error)
  }
}

/**
 * 获取热门搜索（模拟数据，实际应从后端获取）
 * @returns {Array<{keyword: string, count: number, trend: string}>}
 */
export function getHotSearches() {
  return [
    { keyword: '东方快车谋杀案', count: 1285, trend: 'up' },
    { keyword: '迷雾庄园', count: 956, trend: 'up' },
    { keyword: '云中谜案', count: 842, trend: 'down' },
    { keyword: '暴雪山庄', count: 731, trend: 'up' },
    { keyword: '时光旅人', count: 625, trend: 'same' },
    { keyword: '末日余晖', count: 518, trend: 'up' },
    { keyword: '古堡迷踪', count: 467, trend: 'down' },
    { keyword: '红楼梦境', count: 392, trend: 'same' }
  ]
}

export default {
  getSearchHistory,
  addSearchHistory,
  removeSearchHistory,
  clearSearchHistory,
  getHotSearches
}

