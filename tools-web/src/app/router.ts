import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/pages/HomePage.vue')
        },
        {
          path: 'category/:categoryKey',
          name: 'category',
          component: () => import('@/pages/CategoryPage.vue')
        },
        {
          path: 'tools/:toolKey',
          name: 'tool',
          component: () => import('@/pages/ToolPage.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/NotFoundPage.vue')
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
