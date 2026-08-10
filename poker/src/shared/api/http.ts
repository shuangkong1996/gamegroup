import axios, { AxiosError } from 'axios'
import type { ApiResponse } from './types'

export const TOKEN_KEY = 'poker_token'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body.code !== 200) {
      if (body.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      redirectToLogin()
    }
    const message =
      (error.response?.data as ApiResponse | undefined)?.message ||
      error.message ||
      '网络异常'
    return Promise.reject(new Error(message))
  }
)

let redirecting = false

function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  localStorage.removeItem(TOKEN_KEY)
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get<ApiResponse<T>>(url, { params })
  return res.data.data
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.post<ApiResponse<T>>(url, data)
  return res.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.put<ApiResponse<T>>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await http.delete<ApiResponse<T>>(url)
  return res.data.data
}
