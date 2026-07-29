import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/HomeDashboard.vue'),
        meta: { title: '首页看板' }
      },
      {
        path: 'industry',
        name: 'Industry',
        component: () => import('@/views/IndustryView.vue'),
        meta: { title: '产业链全景' }
      },
      {
        path: 'diagnosis',
        name: 'Diagnosis',
        component: () => import('@/views/DiagnosisView.vue'),
        meta: { title: '强弱诊断' }
      },
      {
        path: 'investment',
        name: 'Investment',
        component: () => import('@/views/InvestmentView.vue'),
        meta: { title: '精准招商' }
      },
      {
        path: 'enterprise',
        name: 'Enterprise',
        component: () => import('@/views/EnterpriseView.vue'),
        meta: { title: '企业详情' }
      },
      {
        path: 'local-enterprises',
        name: 'LocalEnterprises',
        component: () => import('@/views/LocalEnterprisesView.vue'),
        meta: { title: '查看本地企业' }
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/ReportsView.vue'),
        meta: { title: '招商报告' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Global navigation guard — check login
router.beforeEach((to, from, next) => {
  const loggedIn = sessionStorage.getItem('loggedIn')
  if (to.path !== '/login' && !loggedIn) {
    next('/login')
  } else if (to.path === '/login' && loggedIn) {
    next('/home')
  } else {
    next()
  }
})

export default router
