import axios from 'axios'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：带 token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理业务码
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      return Promise.reject(new Error('请先登录'))
    }
    return Promise.reject(error)
  }
)

export function getToken() {
  let token = sessionStorage.getItem('token')
  if (token) return token
  try {
    const store = sessionStorage.getItem('userStore')
    if (store) token = JSON.parse(store).token || ''
  } catch (e) {
    // 忽略
  }
  return token || ''
}

export function setToken(token) {
  sessionStorage.setItem('token', token)
}

export function removeToken() {
  sessionStorage.removeItem('token')
}

export default service
