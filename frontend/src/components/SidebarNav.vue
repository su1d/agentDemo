<template>
  <aside class="sidebar-nav">
    <div class="sidebar-header">
      <div class="sidebar-logo">
        <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="#6c5ce7" fill-opacity="0.2"/>
          <path d="M16 24C16 19.58 19.58 16 24 16C28.42 16 32 19.58 32 24" stroke="#6c5ce7" stroke-width="2.5" stroke-linecap="round"/>
          <path d="M24 20V24" stroke="#6c5ce7" stroke-width="2.5" stroke-linecap="round"/>
          <circle cx="24" cy="28" r="2" fill="#6c5ce7"/>
        </svg>
      </div>
      <div class="sidebar-title-group">
        <span class="sidebar-title">MAS</span>
        <span class="sidebar-subtitle">智能协作</span>
      </div>
    </div>

    <nav class="nav-list">
      <div v-for="item in menus" :key="item.id" class="nav-item-wrapper">
        <router-link
          v-if="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="getIcon(item.icon)"></span>
          <span class="nav-label">{{ item.name }}</span>
          <span v-if="isActive(item.path)" class="nav-active-dot"></span>
        </router-link>

        <div v-else class="nav-section">
          <div class="nav-section-title" @click="toggleSection(item.id)">
            <span class="nav-icon" v-html="getIcon(item.icon)"></span>
            <span class="nav-label">{{ item.name }}</span>
            <svg class="nav-arrow" :class="{ open: openSections[item.id] }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
          </div>
          <transition name="slide">
            <div v-if="openSections[item.id]" class="nav-children">
              <router-link
                v-for="child in item.children"
                :key="child.id"
                :to="child.path"
                class="nav-item nav-child-item"
                :class="{ active: isActive(child.path) }"
              >
                <span class="nav-icon" v-html="getIcon(child.icon)"></span>
                <span class="nav-label">{{ child.name }}</span>
              </router-link>
            </div>
          </transition>
        </div>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div class="user-info-bar">
        <div class="user-avatar">{{ username ? username.charAt(0).toUpperCase() : '?' }}</div>
        <div class="user-details">
          <span class="user-name">{{ username }}</span>
          <span class="user-status">在线</span>
        </div>
      </div>
      <button class="logout-btn" @click="logout">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        退出登录
      </button>
    </div>
  </aside>
</template>

<script>
import { apiGet } from '../auth'

export default {
  name: 'SidebarNav',
  data() {
    return {
      menus: [],
      openSections: {},
      username: localStorage.getItem('username') || '',
    }
  },
  mounted() {
    this.fetchMenus()
  },
  methods: {
    async fetchMenus() {
      try {
        const data = await apiGet('/menus')
        this.menus = data
        for (const item of data) {
          if (item.children && item.children.length > 0) {
            this.openSections[item.id] = true
          }
        }
      } catch (e) {
        console.error('Failed to load menus:', e)
      }
    },
    toggleSection(id) {
      this.openSections[id] = !this.openSections[id]
    },
    isActive(path) {
      return this.$route.path === path
    },
    getIcon(icon) {
      const map = {
        chat: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>',
        workflow: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>',
        history: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
        setting: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
        dashboard: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>',
        user: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
        document: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>',
      }
      return map[icon] || map.document
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    },
  },
}
</script>

<style scoped>
.sidebar-nav {
  width: 240px;
  min-height: 100%;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  backdrop-filter: blur(20px);
}

.sidebar-header {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(108,92,231,0.1);
  border-radius: 10px;
}

.sidebar-title-group {
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.sidebar-subtitle {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item-wrapper {
  /* spacing handled by gap */
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
  cursor: pointer;
  position: relative;
}

.nav-item:hover {
  background: rgba(108,92,231,0.06);
  color: var(--text-primary);
}

.nav-item.active {
  background: rgba(108,92,231,0.12);
  color: var(--accent);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-label {
  flex: 1;
}

.nav-active-dot {
  width: 6px;
  height: 6px;
  background: var(--accent);
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(108,92,231,0.4);
}

.nav-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.nav-section-title:hover {
  background: rgba(108,92,231,0.06);
  color: var(--text-primary);
}

.nav-arrow {
  transition: transform 0.2s ease;
  flex-shrink: 0;
  color: var(--text-muted);
}

.nav-arrow.open {
  transform: rotate(90deg);
}

.nav-children {
  margin-left: 8px;
  margin-top: 2px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-child-item {
  padding: 8px 12px;
  font-size: 13px;
  padding-left: 32px;
}

/* slide transition */
.slide-enter-active, .slide-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.slide-enter-from, .slide-leave-to {
  opacity: 0;
  max-height: 0;
}
.slide-enter-to, .slide-leave-from {
  opacity: 1;
  max-height: 200px;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-info-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--accent), #a78bfa);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-status {
  font-size: 11px;
  color: var(--green);
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.logout-btn:hover {
  background: var(--red-bg);
  border-color: rgba(248,113,113,0.3);
  color: var(--red);
}
</style>
