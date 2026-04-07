import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api', // 配合 vite proxy
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  (config: any) => {
    // 如果有 token，可以在这里添加
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error: any) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      // 处理业务错误
      ElMessage.error(res.message || 'Error')
      
      // Token 失效
      if (res.code === 401 || res.code === 1003 || res.code === 1004) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data // 直接返回 data 部分
  },
  (error: any) => {
    console.error('Network Error:', error)
    if (error.response && error.response.status === 401) {
       localStorage.removeItem('token')
       router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default service
