import request from '@/utils/request'

// 搜索短剧（白名单，无需登录）参数：keyword、page、size、sortBy
export const searchDramas = (params) => request.get('/api/search', { params })

// 搜索联想（白名单）
export const suggestSearch = (keyword) =>
  request.get('/api/search/suggest', { params: { keyword } })

// 热门搜索词（白名单）
export const getHotSearch = () => request.get('/api/search/hot')

// 手动创建索引（管理端触发全量重建）
export const createSearchIndex = () => request.post('/api/search/index')

// 搜索历史
export const addSearchHistory = (keyword) =>
  request.post('/api/search/history', null, { params: { keyword } })
export const getSearchHistory = () => request.get('/api/search/history')
export const clearSearchHistory = () => request.delete('/api/search/history')

// 热词管理
export const listHotWords = () => request.get('/api/search/hot-word')
export const createHotWord = (data) => request.post('/api/search/hot-word', data)
export const updateHotWord = (id, data) => request.put(`/api/search/hot-word/${id}`, data)
export const deleteHotWord = (id) => request.delete(`/api/search/hot-word/${id}`)
