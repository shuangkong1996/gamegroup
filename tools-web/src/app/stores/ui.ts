import { defineStore } from 'pinia'

const THEME_KEY = 'tools-web-theme'

function preferredDark() {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

export const useUiStore = defineStore('ui', {
  state: () => ({
    darkMode: false,
    sidebarOpen: false,
    searchKeyword: ''
  }),
  actions: {
    initTheme() {
      const saved = localStorage.getItem(THEME_KEY)
      this.darkMode = saved ? saved === 'dark' : preferredDark()
      this.applyTheme()
    },
    toggleTheme() {
      this.darkMode = !this.darkMode
      localStorage.setItem(THEME_KEY, this.darkMode ? 'dark' : 'light')
      this.applyTheme()
    },
    applyTheme() {
      document.documentElement.classList.toggle('dark', this.darkMode)
    },
    openSidebar() {
      this.sidebarOpen = true
    },
    closeSidebar() {
      this.sidebarOpen = false
    }
  }
})
