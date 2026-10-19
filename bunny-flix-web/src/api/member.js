import request from '@/utils/request'

// VIP 状态（userId 从登录态 UserContext 获取，无需传参）
export const getVipStatus = () => request.get('/api/member/vip/status')

// VIP 套餐
export const getVipPlans = () => request.get('/api/member/vip/plans')

// 购买 VIP
export const purchaseVip = (planId) =>
  request.post('/api/member/vip/purchase', { planId })

// 充值（创建充值订单）
export const createRechargeOrder = (amount) =>
  request.post('/api/member/recharge/order', { amount })

// 支付回调
export const payCallback = (orderNo, success) =>
  request.post('/api/member/recharge/callback', { orderNo, success })

// 充值记录
export const getRechargeOrders = (page, size) =>
  request.get('/api/member/recharge/orders', { params: { page, size } })

// 消费记录
export const getConsumeRecords = (page, size, type) =>
  request.get('/api/member/consume/records', { params: { page, size, type } })

// 钱包
export const getWallet = () => request.get('/api/member/wallet')
export const getBalance = () => request.get('/api/member/wallet/balance')
