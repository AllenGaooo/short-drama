import axios from 'axios'

// 基础路径走 vite 代理的 /api（网关 8080）。Agent 接口无需登录。
const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 响应拦截：统一处理业务码
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => Promise.reject(error)
)

export default service
