import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { validateSession } from '@/shared/api/auth'
import { clearSession, hasValidSessionToken } from '@/shared/auth/session'

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
          path: 'tools/resume/login',
          name: 'resume-login',
          component: () => import('@/pages/ResumeLoginPage.vue')
        },
        {
          path: 'tools/:toolKey(resume)',
          name: 'resume',
          component: () => import('@/pages/ToolPage.vue'),
          meta: { requiresAuth: true }
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

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true
  if (!hasValidSessionToken()) {
    clearSession()
    return { name: 'resume-login', query: { redirect: to.fullPath } }
  }
  try {
    await validateSession()
    return true
  } catch {
    clearSession()
    return { name: 'resume-login', query: { redirect: to.fullPath } }
  }
})

export default router
