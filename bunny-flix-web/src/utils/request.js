import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '',
  timeout: 15000
})

// 请求拦截器：自动携带 token
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 Result 包装
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, message, data, success }
    if (res && res.code !== undefined) {
      if (res.code === 200) {
        return res
      }
      if (res.code === 401) {
        ElMessage.error(res.message || '请先登录')
        useUserStore().logout()
        router.push('/login')
        return Promise.reject(new Error(res.message))
      }
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      useUserStore().logout()
      router.push('/login')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
