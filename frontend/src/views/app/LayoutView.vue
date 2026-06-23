<template>
  <div class="app-shell">
    <header class="topbar glass-strong">
      <div class="topbar-left">
        <div class="brand" @click="$router.push('/dashboard')">
          <div class="brand-icon">
            <svg width="24" height="24" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="currentColor" fill-opacity="0.12"/>
              <path d="M16 24C16 19.58 19.58 16 24 16c4.42 0 8 3.58 8 8" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <path d="M24 20v4" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <circle cx="24" cy="28" r="2" fill="currentColor"/>
            </svg>
          </div>
          <span class="brand-name">AgentForge</span>
        </div>
        <div class="brand-divider"></div>
      </div>

      <nav class="topbar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="15" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="topbar-right">
        <div class="user-badge glass">
          <div class="user-avatar">{{ store.username.charAt(0).toUpperCase() }}</div>
          <span class="user-name">{{ store.username }}</span>
        </div>
        <button class="logout-btn" @click="handleLogout" title="退出登录">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </header>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
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
async function handleLogout() {
  await store.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-deep);
}

.topbar {
  display: flex;
  align-items: center;
  height: 54px;
  padding: 0 16px;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--accent);
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(108,92,231,0.1);
  color: var(--accent);
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.brand-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
}

.topbar-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  padding-left: 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: var(--transition);
  position: relative;
}

.nav-item:hover {
  background: rgba(108,92,231,0.06);
  color: var(--text-primary);
}

.nav-item.active {
  background: rgba(108,92,231,0.1);
  color: var(--accent);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  border-radius: 1px;
  background: var(--accent);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  border-radius: 8px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: linear-gradient(135deg, var(--accent), var(--pink));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.logout-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: var(--transition);
}

.logout-btn:hover {
  background: var(--red-bg);
  border-color: rgba(248,113,113,0.25);
  color: var(--red);
}

.main-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
}
</style>
