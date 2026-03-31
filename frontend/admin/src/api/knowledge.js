import request from '@/utils/request'

/** 获取知识库列表 */
export const getKnowledgeList = (params) => request.get('/admin/knowledge/list', { params })

/** 获取知识库统计 */
export const getKnowledgeStats = () => request.get('/admin/knowledge/stats')

/** 新增知识条目 */
export const addKnowledge = (data) => request.post('/admin/knowledge', data)

/** 更新知识条目 */
export const updateKnowledge = (data) => request.put('/admin/knowledge', data)

/** 删除知识条目 */
export const deleteKnowledge = (id) => request.delete(`/admin/knowledge/${id}`)

/** 刷新知识库缓存 */
export const reloadKnowledge = () => request.post('/admin/knowledge/reload')

/** 测试RAG检索 */
export const searchKnowledge = (params) => request.get('/admin/knowledge/search', { params })
