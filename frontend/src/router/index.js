import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
  },
  {
    path: '/',
    component: () => import('../views/app/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/app/DashboardView.vue'),
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/app/ChatView.vue'),
      },
      {
        path: 'workflow',
        name: 'Workflow',
        component: () => import('../views/app/WorkflowView.vue'),
      },
      {
        path: 'workflow/:id',
        name: 'WorkflowEditor',
        component: () => import('../views/app/WorkflowEditorView.vue'),
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('../views/app/KnowledgeView.vue'),
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('../views/app/HistoryView.vue'),
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/app/SettingsView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
