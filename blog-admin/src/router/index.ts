import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue')
        },
        {
          path: 'articles',
          name: 'ArticleList',
          component: () => import('../views/article/ArticleList.vue'),
          meta: { permission: 'article:view' }
        },
        {
          path: 'articles/create',
          name: 'ArticleCreate',
          component: () => import('../views/article/ArticleEditor.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'articles/edit/:id',
          name: 'ArticleEdit',
          component: () => import('../views/article/ArticleEditor.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'categories',
          name: 'CategoryList',
          component: () => import('../views/category/CategoryList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'tags',
          name: 'TagList',
          component: () => import('../views/tag/TagList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'series',
          name: 'SeriesList',
          component: () => import('../views/series/SeriesList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'comments',
          name: 'CommentList',
          component: () => import('../views/comment/CommentList.vue'),
          meta: { permission: 'comment:review' }
        },
        {
          path: 'media',
          name: 'MediaLibrary',
          component: () => import('../views/media/MediaLibrary.vue'),
          meta: { permission: 'media:upload' }
        },
        {
          path: 'subscribers',
          name: 'SubscriberList',
          component: () => import('../views/subscriber/SubscriberList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN', 'EDITOR'] }
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('../views/settings/SiteSettings.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'backup',
          name: 'Backup',
          component: () => import('../views/settings/BackupRestore.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'newsletter',
          name: 'Newsletter',
          component: () => import('../views/newsletter/NewsletterList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN', 'EDITOR'] }
        },
        {
          path: 'seo',
          name: 'SeoSettings',
          component: () => import('../views/settings/SeoSettings.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'menus',
          name: 'MenuSettings',
          component: () => import('../views/settings/MenuSettings.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'users',
          name: 'UserList',
          component: () => import('../views/settings/UserList.vue'),
          meta: { roles: ['SUPER_ADMIN'] }
        },
        {
          path: 'system-log',
          name: 'SystemLog',
          component: () => import('../views/settings/SystemLog.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'products',
          name: 'ProductList',
          component: () => import('../views/product/ProductList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        }
      ]
    }
  ]
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  // Public routes
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  // Not logged in
  if (!token) {
    next('/login')
    return
  }

  // Check role-based access
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    // We rely on the store — if roles aren't loaded yet, AdminLayout will handle it
    // For now, allow navigation; AdminLayout's onMounted will verify
  }

  next()
})

export default router
