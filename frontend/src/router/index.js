import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import WorkflowView from '../views/WorkflowView.vue'
import ExecHistoryView from '../views/ExecHistoryView.vue'
import LoginView from '../views/LoginView.vue'
import MenuManagerView from '../views/MenuManagerView.vue'

const routes = [
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/', redirect: '/chat' },
  { path: '/chat', name: 'Chat', component: ChatView, meta: { requiresAuth: true } },
  { path: '/workflow', name: 'Workflow', component: WorkflowView, meta: { requiresAuth: true } },
  { path: '/history', name: 'History', component: ExecHistoryView, meta: { requiresAuth: true } },
  { path: '/menu-manager', name: 'MenuManager', component: MenuManagerView, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
