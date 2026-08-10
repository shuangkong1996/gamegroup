import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, fetchUserInfo } from '@/shared/api/auth'
import { TOKEN_KEY } from '@/shared/api/http'
import type { UserInfo } from '@/shared/api/types'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null as UserInfo | null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    async login(username: string, password: string) {
      const res = await loginApi(username, password)
      this.token = res.token
      localStorage.setItem(TOKEN_KEY, res.token)
      this.userInfo = {
        id: res.userId,
        username,
        nickname: res.nickname || username,
        avatarUrl: res.avatarUrl || ''
      }
    },

    async registerAndLogin(username: string, password: string) {
      await registerApi(username, password)
      await this.login(username, password)
    },

    async loadUserInfo() {
      if (!this.token) return
      try {
        this.userInfo = await fetchUserInfo()
      } catch {
        // token 失效由拦截器统一处理
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
