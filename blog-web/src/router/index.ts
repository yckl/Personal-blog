import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/BlogLayout.vue'),
      children: [
        { path: '', name: 'Home', component: () => import('../views/Home.vue') },
        { path: 'articles', name: 'ArticleList', component: () => import('../views/ArticleList.vue') },
        { path: 'article/:slug', name: 'ArticleDetail', component: () => import('../views/ArticleDetail.vue') },
        { path: 'category/:slug', name: 'CategoryPage', component: () => import('../views/CategoryPage.vue') },
        { path: 'tag/:slug', name: 'TagPage', component: () => import('../views/TagPage.vue') },
        { path: 'series/:slug', name: 'SeriesDetail', component: () => import('../views/SeriesPage.vue') },
        { path: 'search', name: 'SearchPage', component: () => import('../views/SearchPage.vue') },
        { path: 'archive', name: 'ArchivePage', component: () => import('../views/ArchivePage.vue') },
        { path: 'about', name: 'AboutPage', component: () => import('../views/AboutPage.vue') },
        { path: 'contact', name: 'ContactPage', component: () => import('../views/ContactPage.vue') },
        { path: 'products', name: 'ProductsPage', component: () => import('../views/ProductsPage.vue') },
        { path: 'subscribe/confirm', name: 'SubscribeConfirm', component: () => import('../views/SubscribeConfirm.vue') },
        { path: 'unsubscribe', name: 'Unsubscribe', component: () => import('../views/UnsubscribePage.vue') },
        { path: 'member', component: () => import('../layouts/UserCenterLayout.vue'),
          children: [
            { path: '', redirect: '/member/profile' },
            { path: 'profile', name: 'UserProfile', component: () => import('../views/member/UserProfile.vue') },
            { path: 'plans', name: 'UserPlans', component: () => import('../views/member/UserPlans.vue') },
            { path: 'orders', name: 'UserOrders', component: () => import('../views/member/UserOrders.vue') },
            { path: 'assets', name: 'UserAssets', component: () => import('../views/member/UserAssets.vue') },
          ]
        },
        { path: ':pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFoundPage.vue') },
      ]
    }
  ]
})

export default router
