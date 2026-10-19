import request from '@/utils/request'

// 个性化推荐
export const getRecommendations = (page, size) =>
  request.get('/api/recommend', { params: { page, size } })

// 热门推荐（白名单，无需登录）
export const getHotRecommend = (page, size) =>
  request.get('/api/recommend/hot', { params: { page, size } })
