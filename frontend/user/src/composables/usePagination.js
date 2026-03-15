import { ref, reactive } from 'vue'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '@/utils/constants'

/**
 * 通用分页逻辑 composable
 * 
 * @param {Function} fetchFn - 数据加载函数，接收 pagination 对象，返回 Promise<{ list, total }>
 * @param {Object} options - 配置项
 * @param {number} options.pageSize - 每页条数，默认 12
 * @param {boolean} options.autoLoad - 是否在 mount 时自动加载，默认 true
 * 
 * @example
 * const { list, total, loading, pagination, handlePageChange, handleSizeChange, refresh } = usePagination(
 *   async (params) => {
 *     const res = await getScriptList({ ...searchForm, ...params })
 *     return { list: res.data.records, total: res.data.total }
 *   }
 * )
 */
export function usePagination(fetchFn, options = {}) {
  const { pageSize = DEFAULT_PAGE_SIZE, autoLoad = true } = options

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)

  const pagination = reactive({
    page: DEFAULT_PAGE,
    pageSize
  })

  const load = async () => {
    loading.value = true
    try {
      const result = await fetchFn({ page: pagination.page, pageSize: pagination.pageSize })
      if (result) {
        // 支持 { list, total } 或 { records, total } 两种格式
        list.value = result.list || result.records || []
        total.value = result.total || 0
      }
    } catch (error) {
      console.error('分页数据加载失败:', error)
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const handlePageChange = (newPage) => {
    pagination.page = newPage
    window.scrollTo({ top: 0, behavior: 'smooth' })
    load()
  }

  const handleSizeChange = (newSize) => {
    pagination.pageSize = newSize
    pagination.page = DEFAULT_PAGE
    window.scrollTo({ top: 0, behavior: 'smooth' })
    load()
  }

  // 重置到第一页并重新加载（用于搜索/筛选触发）
  const refresh = () => {
    pagination.page = DEFAULT_PAGE
    load()
  }

  if (autoLoad) {
    // 延迟加载，确保组件已挂载（配合 onMounted 使用）
    // 调用方应在 onMounted 中调用 load() 或 refresh()
  }

  return {
    loading,
    list,
    total,
    pagination,
    load,
    refresh,
    handlePageChange,
    handleSizeChange
  }
}
