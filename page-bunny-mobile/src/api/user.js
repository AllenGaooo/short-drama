import request from '@/utils/request.js'

/**
 * 用户相关接口（走网关 /api/user/**）
 */

/** 密码登录 */
export function loginByPassword(phone, password) {
  return request.post('/user/login/password', null, { params: { phone, password } })
}

/** 验证码登录 */
export function loginByCode(phone, code) {
  return request.post('/user/login/code', null, { params: { phone, code } })
}

/** 注册 */
export function register(phone, password) {
  return request.post('/user/register', null, { params: { phone, password } })
}

/** 发送短信验证码 */
export function sendSmsCode(phone) {
  return request.post('/user/sms/send', null, { params: { phone } })
}

/** 获取当前登录用户信息 */
export function getCurrentUser() {
  return request.get('/user/me')
}
