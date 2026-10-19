import request from '@/utils/request.js'

/**
 * 短剧相关接口（走网关 /api/drama/**）
 */

/** 分页列表：首页、分类筛选、无限滚动加载 */
export function listDramas(params) {
  return request.get('/drama/list', { params })
}

/** 热门榜单 */
export function getRanking(params) {
  return request.get('/drama/ranking', { params })
}

/** 全部分类/标签 */
export function listTags() {
  return request.get('/drama/tag/all')
}

/** 短剧详情 */
export function getDrama(id) {
  return request.get(`/drama/${id}`)
}
