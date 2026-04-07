import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/blog'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/admin/Login.vue')
  },
  {
    path: '/blog',
    component: () => import('@/views/blog/Layout.vue'),
    children: [
      {
        path: '',
        name: 'BlogHome',
        component: () => import('@/views/blog/Home.vue')
      },
      {
        path: 'article/:id',
        name: 'BlogArticle',
        component: () => import('@/views/blog/Article.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    children: [
      {
        path: 'articles',
        name: 'AdminArticles',
        component: () => import('@/views/admin/ArticleList.vue')
      },
      {
        path: 'publish',
        name: 'AdminPublish',
        component: () => import('@/views/admin/Publish.vue')
      },
      {
        path: 'ai-video',
        name: 'AiVideo',
        component: () => import('@/views/admin/AiVideo.vue')
      },
      {
        path: 'ledger',
        name: 'AdminLedger',
        component: () => import('@/views/admin/Ledger.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path.startsWith('/admin')) {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
