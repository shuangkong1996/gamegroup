import http from './http'

export interface LoginResponse {
  token: string
  userId: number
  nickname: string
  avatarUrl?: string
  isNewUser?: boolean
}

export function adminLogin(username: string, password: string) {
  return http.post<unknown, LoginResponse>('/auth/admin/login', { username, password })
}

export function validateSession() {
  return http.get('/auth/user-info')
}
