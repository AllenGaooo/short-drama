import request from '@/utils/request'

// 短剧列表
export const listDramas = (params) => request.get('/api/drama/list', { params })

// 短剧详情
export const getDrama = (id) => request.get(`/api/drama/${id}`)

// 创建短剧
export const createDrama = (data) => request.post('/api/drama', data)

// 更新短剧
export const updateDrama = (id, data) => request.put(`/api/drama/${id}`, data)

// 删除短剧
export const deleteDrama = (id) => request.delete(`/api/drama/${id}`)

// 更新上架状态
export const updateDramaStatus = (id, status) =>
  request.put(`/api/drama/${id}/status`, null, { params: { status } })

// 更新完结状态
export const updateDramaCompleted = (id, completed) =>
  request.put(`/api/drama/${id}/completed`, null, { params: { completed } })

// 演员
export const listActors = (params) => request.get('/api/drama/actor/list', { params })
export const getActor = (id) => request.get(`/api/drama/actor/${id}`)
export const createActor = (data) => request.post('/api/drama/actor', data)
export const updateActor = (id, data) => request.put(`/api/drama/actor/${id}`, data)
export const deleteActor = (id) => request.delete(`/api/drama/actor/${id}`)

// 剧集
export const listEpisodes = (dramaId) => request.get(`/api/drama/episode/list/${dramaId}`)
export const getEpisode = (id) => request.get(`/api/drama/episode/${id}`)
export const createEpisode = (data) => request.post('/api/drama/episode', data)
export const updateEpisode = (id, data) => request.put(`/api/drama/episode/${id}`, data)
export const deleteEpisode = (id) => request.delete(`/api/drama/episode/${id}`)
export const updateEpisodeSort = (id, sortOrder) =>
  request.put(`/api/drama/episode/${id}/sort`, null, { params: { sortOrder } })
export const updateEpisodeFree = (id, free) =>
  request.put(`/api/drama/episode/${id}/free`, null, { params: { free } })

// 发行方
export const listPublishers = (params) => request.get('/api/drama/publisher/list', { params })
export const getPublisher = (id) => request.get(`/api/drama/publisher/${id}`)
export const createPublisher = (data) => request.post('/api/drama/publisher', data)
export const updatePublisher = (id, data) => request.put(`/api/drama/publisher/${id}`, data)
export const deletePublisher = (id) => request.delete(`/api/drama/publisher/${id}`)

// 标签
export const listTags = (parentId) =>
  request.get('/api/drama/tag', { params: parentId != null ? { parentId } : {} })
export const listAllTags = () => request.get('/api/drama/tag/all')
export const getTag = (id) => request.get(`/api/drama/tag/${id}`)
export const createTag = (data) => request.post('/api/drama/tag', data)
export const updateTag = (id, data) => request.put(`/api/drama/tag/${id}`, data)
export const deleteTag = (id) => request.delete(`/api/drama/tag/${id}`)
