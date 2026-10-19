import axios from 'axios'

// 创建 axios 实例，基础路径走 vite 代理的 /api
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动带上 token
service.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('userStore')
      ? JSON.parse(sessionStorage.getItem('userStore')).token
      : ''
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
    // 后端 Result 约定：code === 200 成功（Result.success 通常 code=200）
    if (res.code && res.code !== 200) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    return Promise.reject(error)
  }
)

export default service
