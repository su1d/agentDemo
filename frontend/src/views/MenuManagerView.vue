<template>
  <div class="menu-manager">
    <div class="page-header">
      <div class="header-left">
        <h2>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          菜单管理
        </h2>
        <span class="menu-count">{{ menus.length }} 个菜单</span>
      </div>
      <button class="btn-primary" @click="openCreateForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建菜单
      </button>
    </div>

    <div class="menu-tree">
      <div v-for="item in menus" :key="item.id" class="menu-tree-item">
        <div class="menu-row">
          <div class="menu-info">
            <span class="menu-icon" v-html="getIcon(item.icon)"></span>
            <span class="menu-name">{{ item.name }}</span>
            <code v-if="item.path" class="menu-path">{{ item.path }}</code>
            <span v-else class="menu-path group-label">分组</span>
          </div>
          <div class="menu-actions">
            <button class="btn-icon" @click="editMenu(item)" title="编辑">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
            <button class="btn-icon btn-icon-danger" @click="deleteMenu(item.id)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
            </button>
          </div>
        </div>
        <transition name="slide">
          <div v-if="item.children && item.children.length" class="menu-children">
            <div v-for="child in item.children" :key="child.id" class="menu-row menu-row-child">
              <div class="menu-info">
                <span class="menu-icon" v-html="getIcon(child.icon)"></span>
                <span class="menu-name">{{ child.name }}</span>
                <code class="menu-path">{{ child.path }}</code>
              </div>
              <div class="menu-actions">
                <button class="btn-icon" @click="editMenu(child)" title="编辑">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button class="btn-icon btn-icon-danger" @click="deleteMenu(child.id)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                </button>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- Modal -->
    <transition name="modal">
      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal-card">
          <div class="modal-header">
            <h3>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              {{ editingMenu ? '编辑菜单' : '新建菜单' }}
            </h3>
            <button class="modal-close" @click="showForm = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>菜单名称</label>
              <input v-model="form.name" class="form-input" placeholder="例如：智能对话" />
            </div>
            <div class="form-group">
              <label>路由路径</label>
              <input v-model="form.path" class="form-input" placeholder="例如：/chat（留空为分组）" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>图标标识</label>
                <input v-model="form.icon" class="form-input" placeholder="chat / workflow / history" />
              </div>
              <div class="form-group">
                <label>排序</label>
                <input v-model.number="form.sortOrder" type="number" class="form-input" />
              </div>
            </div>
            <div class="form-group">
              <label>父菜单</label>
              <select v-model="form.parentId" class="form-input">
                <option :value="null">顶级菜单</option>
                <option v-for="item in menus" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="showForm = false">取消</button>
            <button class="btn-primary" @click="saveMenu">保存</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { apiGet } from '../auth'

const API_BASE = 'http://localhost:8080/api'

export default {
  name: 'MenuManagerView',
  data() {
    return {
      menus: [],
      showForm: false,
      editingMenu: null,
      form: { name: '', path: '', icon: '', parentId: null, sortOrder: 0 },
    }
  },
  mounted() { this.loadMenus() },
  methods: {
    async loadMenus() {
      try {
        const data = await apiGet('/menus')
        this.menus = data
      } catch (e) { console.error(e) }
    },
    openCreateForm() {
      this.editingMenu = null
      this.form = { name: '', path: '', icon: '', parentId: null, sortOrder: 0 }
      this.showForm = true
    },
    editMenu(item) {
      this.editingMenu = item
      this.form = {
        name: item.name,
        path: item.path || '',
        icon: item.icon || '',
        parentId: item.parentId || null,
        sortOrder: item.sortOrder || 0,
      }
      this.showForm = true
    },
    async saveMenu() {
      const token = localStorage.getItem('token')
      const headers = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token }
      const body = JSON.stringify(this.form)
      try {
        if (this.editingMenu) {
          await fetch(API_BASE + '/menus/' + this.editingMenu.id, { method: 'PUT', headers, body })
        } else {
          await fetch(API_BASE + '/menus', { method: 'POST', headers, body })
        }
        this.showForm = false
        this.loadMenus()
      } catch (e) { console.error(e) }
    },
    async deleteMenu(id) {
      if (!confirm('确定删除此菜单？')) return
      const token = localStorage.getItem('token')
      try {
        await fetch(API_BASE + '/menus/' + id, { method: 'DELETE', headers: { Authorization: 'Bearer ' + token } })
        this.loadMenus()
      } catch (e) { console.error(e) }
    },
    getIcon(icon) {
      const map = {
        chat: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>',
        workflow: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>',
        history: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
        setting: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
      }
      return map[icon] || map.chat
    },
  },
}
</script>

<style scoped>
.menu-manager {
  padding: 28px 32px;
  max-width: 860px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.3px;
}

.header-left h2 svg { color: var(--accent); }

.menu-count {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  padding: 3px 10px;
  background: var(--bg-secondary);
  border-radius: 12px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border: none;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--accent), var(--accent-light));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(108,92,231,0.3);
}

.menu-tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.menu-row:hover {
  border-color: rgba(108,92,231,0.2);
  background: var(--bg-card-hover);
}

.menu-row-child {
  margin-bottom: 4px;
}

.menu-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.menu-icon {
  display: flex;
  align-items: center;
  color: var(--accent);
  width: 20px;
  justify-content: center;
}

.menu-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.menu-path {
  font-size: 12px;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: 4px;
}

.group-label {
  color: var(--yellow) !important;
  font-size: 10px !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.menu-actions {
  display: flex;
  gap: 4px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: var(--transition);
}

.btn-icon:hover {
  background: var(--bg-secondary);
  border-color: var(--border-color);
  color: var(--text-primary);
}

.btn-icon-danger:hover {
  background: var(--red-bg);
  border-color: rgba(248,113,113,0.2);
  color: var(--red);
}

.menu-children {
  margin-left: 28px;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

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
  max-height: 500px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(4px);
}

.modal-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  width: 460px;
  max-width: 92vw;
  box-shadow: var(--shadow-elevated);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-header h3 svg { color: var(--accent); }

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: var(--transition);
}

.modal-close:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: var(--transition);
}

.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.btn-cancel {
  padding: 9px 18px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-cancel:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
.modal-enter-active .modal-card, .modal-leave-active .modal-card {
  transition: transform 0.2s ease;
}
.modal-enter-from .modal-card { transform: scale(0.95) translateY(10px); }
.modal-leave-to .modal-card { transform: scale(0.95) translateY(10px); }
</style>
