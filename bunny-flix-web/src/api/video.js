import request from '@/utils/request'

// 视频上传（绑定剧集）
export const uploadVideo = (episodeId, fileUrl) =>
  request.post('/api/video/upload', null, { params: { episodeId, fileUrl } })

// 获取播放地址
export const getPlayUrl = (episodeId, userId) =>
  request.get(`/api/video/play/${episodeId}`, { params: userId ? { userId } : {} })

// 视频信息
export const getVideoInfo = (videoId) => request.get(`/api/video/${videoId}`)

// 清晰度列表
export const getResolution = (episodeId) => request.get(`/api/video/resolution/${episodeId}`)

// 播放进度
export const saveProgress = (data) => request.post('/api/video/progress', null, { params: data })
export const getProgress = (episodeId, userId) =>
  request.get(`/api/video/progress/${episodeId}`, { params: { userId } })

// 观看历史
export const listHistory = (userId, page, pageSize) =>
  request.get('/api/video/history/list', { params: { userId, page, pageSize } })
export const deleteHistory = (id) => request.delete(`/api/video/history/${id}`)
