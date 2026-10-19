import request from '@/utils/request'

// ===== 优惠券（独立 CouponController，需显式传 userId）=====

// 领取优惠券
export const receiveCoupon = (templateId, userId) =>
  request.post(`/api/marketing/coupon/receive/${templateId}`, null, { params: { userId } })

// 我的优惠券（分页）
export const getUserCoupons = (userId, status, page, size) =>
  request.get('/api/marketing/coupon', { params: { userId, status, page, size } })

// 使用优惠券
export const useCoupon = (couponId, userId, orderAmount) =>
  request.post(`/api/marketing/coupon/use/${couponId}`, null, { params: { userId, orderAmount } })

// ===== 券模板（CouponTemplateController）=====

// 可用券模板（分页）
export const getAvailableTemplates = (page, size) =>
  request.get('/api/marketing/coupon/template/available', { params: { page, size } })

// 券模板分页
export const listTemplates = (page, size) =>
  request.get('/api/marketing/coupon/template', { params: { page, size } })

export const getTemplate = (id) => request.get(`/api/marketing/coupon/template/${id}`)
export const createCouponTemplate = (data) => request.post('/api/marketing/coupon/template', data)
export const updateCouponTemplate = (id, data) =>
  request.put(`/api/marketing/coupon/template/${id}`, data)
export const deleteCouponTemplate = (id) =>
  request.delete(`/api/marketing/coupon/template/${id}`)
export const enableCouponTemplate = (id) =>
  request.put(`/api/marketing/coupon/template/${id}/enable`)
export const disableCouponTemplate = (id) =>
  request.put(`/api/marketing/coupon/template/${id}/disable`)

// ===== 签到（CheckinController）=====

// 签到
export const checkin = (userId) =>
  request.post('/api/marketing/checkin', null, { params: { userId } })

// 签到日历
export const checkinCalendar = (userId, month) =>
  request.get('/api/marketing/checkin/calendar', { params: { userId, month } })

// 补签
export const makeupCheckin = (userId, date) =>
  request.post('/api/marketing/checkin/makeup', null, { params: { userId, date } })

// ===== 任务（TaskController）=====

// 我的任务
export const listUserTasks = (userId) =>
  request.get('/api/marketing/task', { params: { userId } })

// 领取任务奖励
export const claimTask = (taskId, userId) =>
  request.post(`/api/marketing/task/claim/${taskId}`, null, { params: { userId } })

// 创建任务
export const createTask = (data) => request.post('/api/marketing/task', data)
