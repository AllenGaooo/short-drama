import request from '@/utils/request'

// 密码登录
export const loginByPassword = (phone, password) =>
  request.post('/api/user/login/password', null, { params: { phone, password } })

// 验证码登录
export const loginByCode = (phone, code) =>
  request.post('/api/user/login/code', null, { params: { phone, code } })

// 发送验证码
export const sendSmsCode = (phone) =>
  request.post('/api/user/sms/send', null, { params: { phone } })

// 注册
export const register = (phone, password) =>
  request.post('/api/user/register', null, { params: { phone, password } })

// 当前用户信息
export const getMe = () => request.get('/api/user/me')

// 用户详情
export const getUserById = (userId) => request.get(`/api/user/${userId}`)

// 更新资料
export const updateProfile = (data) => request.put('/api/user/profile', data)

// 修改密码
export const changePassword = (oldPassword, newPassword) =>
  request.put('/api/user/password', null, { params: { oldPassword, newPassword } })

// 用户列表（无列表接口时用 /me 兜底）
export const getUserList = (userId) => request.get(`/api/user/${userId}`)

// 关注
export const follow = (followUserId) => request.post(`/api/user/follow/${followUserId}`)
export const unfollow = (followUserId) => request.delete(`/api/user/follow/${followUserId}`)
export const checkFollow = (followUserId) => request.get(`/api/user/follow/check/${followUserId}`)
