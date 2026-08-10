import { get, post } from './http'
import type { LoginResponse, UserInfo } from './types'

export function register(username: string, password: string) {
  return post<void>('/auth/register', { username, password })
}

export function login(username: string, password: string) {
  return post<LoginResponse>('/auth/admin/login', { username, password })
}

export function fetchUserInfo() {
  return get<UserInfo>('/auth/user-info')
}
