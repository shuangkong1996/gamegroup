export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  userId: number
  nickname: string
  avatarUrl: string
  isNewUser: boolean
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl: string
}
