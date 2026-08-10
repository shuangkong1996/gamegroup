import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '@/shared/api/http'

const publicPaths = ['/login']

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/lobby'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue')
    },
    {
      path: '/lobby',
      name: 'lobby',
      component: () => import('@/pages/LobbyPage.vue')
    },
    {
      path: '/stats',
      name: 'stats',
      component: () => import('@/pages/StatsPage.vue')
    },
    {
      path: '/room/:roomId',
      name: 'room',
      component: () => import('@/pages/TablePage.vue')
    }
  ]
})

router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  if (!hasToken && !publicPaths.includes(to.path)) {
    return { path: '/login' }
  }
  if (hasToken && to.path === '/login') {
    return { path: '/lobby' }
  }
})
