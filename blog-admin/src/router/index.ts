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
          name: '仪表盘',
          component: () => import('../views/Dashboard.vue')
        },
        {
          path: 'articles',
          name: '文章列表',
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
          name: '分类管理',
          component: () => import('../views/category/CategoryList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'tags',
          name: '标签管理',
          component: () => import('../views/tag/TagList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'series',
          name: '系列管理',
          component: () => import('../views/series/SeriesList.vue'),
          meta: { permission: 'article:edit' }
        },
        {
          path: 'comments',
          name: '评论管理',
          component: () => import('../views/comment/CommentList.vue'),
          meta: { permission: 'comment:review' }
        },
        {
          path: 'media',
          name: '媒体库',
          component: () => import('../views/media/MediaLibrary.vue'),
          meta: { permission: 'media:upload' }
        },
        {
          path: 'subscribers',
          name: '订阅者',
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
          path: 'users',
          name: '用户管理',
          component: () => import('../views/settings/UserList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'messages',
          name: '消息留言',
          component: () => import('../views/message/MessageList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'timeline',
          name: '旅程时间线',
          component: () => import('../views/settings/TimelineList.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'system-log',
          name: '系统日志',
          component: () => import('../views/settings/SystemLog.vue'),
          meta: { roles: ['SUPER_ADMIN', 'ADMIN'] }
        },
        {
          path: 'products',
          name: '数字产品',
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
    const storedRoles = JSON.parse(localStorage.getItem('roles') || '[]') as string[]
    const hasRole = requiredRoles.some(role => storedRoles.includes(role))
    if (!hasRole) {
      // Redirect to dashboard with insufficient permissions
      next('/dashboard')
      return
    }
  }

  // Check permission-based access
  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission) {
    const storedPermissions = JSON.parse(localStorage.getItem('permissions') || '[]') as string[]
    if (!storedPermissions.includes(requiredPermission)) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
