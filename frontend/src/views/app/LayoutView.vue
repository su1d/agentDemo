<template>
  <div class="app-shell">
    <!-- Top Navigation Bar -->
    <header class="topbar">
      <div class="topbar-left">
        <div class="brand" @click="$router.push('/dashboard')">
          <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="currentColor" fill-opacity="0.15"/>
            <path d="M16 24C16 19.58 19.58 16 24 16c4.42 0 8 3.58 8 8" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
            <path d="M24 20v4" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
            <circle cx="24" cy="28" r="2" fill="currentColor"/>
          </svg>
          <span class="brand-name">AgentForge</span>
        </div>
      </div>

      <nav class="topbar-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item" :class="{ active: isActive(item.path) }">
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="topbar-right">
        <div class="user-badge">
          <div class="user-avatar">{{ store.username.charAt(0).toUpperCase() }}</div>
          <span class="user-name">{{ store.username }}</span>
        </div>
        <button class="logout-btn" @click="handleLogout" title="退出登录">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '../../stores/app'
import { MessageSquare, Workflow, Database, History, LayoutDashboard, Settings } from '@lucide/vue'

const store = useAppStore()
const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/dashboard', label: '仪表盘', icon: LayoutDashboard },
  { path: '/chat', label: '对话', icon: MessageSquare },
  { path: '/workflow', label: '工作流', icon: Workflow },
  { path: '/knowledge', label: '知识库', icon: Database },
  { path: '/history', label: '历史', icon: History },
  { path: '/settings', label: '设置', icon: Settings },
]

function isActive(path) {
  if (path === '/workflow') return route.path.startsWith('/workflow')
  return route.path === path
}
function handleLogout() { store.logout(); router.push('/login') }
</script>

<style scoped>
.app-shell { display: flex; flex-direction: column; height: 100vh; overflow: hidden; background: #0d0d1a; }

.topbar {
  display: flex; align-items: center; gap: 8px;
  height: 52px; padding: 0 16px;
  background: #141428;
  border-bottom: 1px solid rgba(108,92,231,0.15);
  flex-shrink: 0;
}

.topbar-left { display: flex; align-items: center; }
.brand { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #6c5ce7; margin-right: 24px; }
.brand-name { font-size: 16px; font-weight: 800; color: #e8e8f0; letter-spacing: -0.3px; }

.topbar-nav { display: flex; align-items: center; gap: 2px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px; border-radius: 6px;
  color: #9898b0; text-decoration: none;
  font-size: 13px; font-weight: 500;
  transition: all 0.15s;
}
.nav-item:hover { background: rgba(108,92,231,0.06); color: #e8e8f0; }
.nav-item.active { background: rgba(108,92,231,0.12); color: #6c5ce7; }

.topbar-right { display: flex; align-items: center; gap: 8px; }
.user-badge { display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: #1a1a30; border-radius: 6px; }
.user-avatar { width: 24px; height: 24px; border-radius: 5px; background: linear-gradient(135deg, #6c5ce7, #a78bfa); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; }
.user-name { font-size: 12px; color: #e8e8f0; }

.logout-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; background: transparent; color: #68687d; cursor: pointer; transition: all 0.15s; }
.logout-btn:hover { background: rgba(248,113,113,0.1); border-color: rgba(248,113,113,0.3); color: #f87171; }

.main-content { flex: 1; overflow-y: auto; }
</style>