import request from '@/utils/request'

// 密码登录：对应后端 AdminController /api/admin/login/password
// 后端用 @RequestParam 接收 phone、password（service-admin，不走 service-user）
export function loginByPassword(phone, password) {
  return request({
    url: '/admin/login/password',
    method: 'post',
    params: { phone, password }
  })
}

// 手机号+密码注册：对应后端 AdminController /api/admin/register
export function register(phone, password) {
  return request({
    url: '/admin/register',
    method: 'post',
    params: { phone, password }
  })
}

// 获取当前管理员的分级菜单（后端 AdminController，按登录管理员userId返回菜单树）
export function getMenus() {
  return request({
    url: '/admin/menus',
    method: 'get'
  })
}
