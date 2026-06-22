<template>
  <div class="workflow-viewer">
    <div class="viewer-header">
      <h2>
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        工作流执行历史
      </h2>
    </div>

    <div class="workflow-list">
      <div v-for="wf in workflows" :key="wf.id" class="wf-card"
        :class="{ 'wf-active': wf.status === 'active' }"
        @click="selectWorkflow(wf.id)">
        <div class="wf-info">
          <div class="wf-name">{{ wf.name }}</div>
          <div class="wf-meta">
            <span class="wf-status" :class="'wf-status-' + wf.status">{{ statusLabel(wf.status) }}</span>
            <span v-if="wf.collaborationMode" class="wf-mode">{{ wf.collaborationMode }}</span>
            <span class="wf-date">{{ formatDate(wf.createdAt) }}</span>
          </div>
        </div>
        <svg class="wf-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
      </div>
      <div v-if="workflows.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-dasharray="4 4"><circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/></svg>
        <p>暂无工作流</p>
      </div>
    </div>

    <transition name="fade">
      <div v-if="selectedWfId" class="exec-panel">
        <div class="exec-header">
          <h3>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            {{ selectedWfName }}
          </h3>
        </div>

        <div v-if="execHistory.length === 0" class="empty-state">
          <p>暂无执行记录</p>
        </div>

        <div v-for="exec in execHistory" :key="exec.sessionId" class="exec-card">
          <div class="exec-card-header">
            <div class="exec-session-info">
              <code class="exec-session-id">{{ exec.sessionId?.substring(0, 12) }}...</code>
              <span :class="['exec-status-badge', exec.status === 'success' ? 'badge-success' : 'badge-fail']">
                {{ exec.status === 'success' ? '成功' : '失败' }}
              </span>
            </div>
            <button class="detail-btn" @click="viewExecDetail(exec)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              详情
            </button>
          </div>
          <div v-if="exec.error" class="exec-error-line">{{ exec.error }}</div>
        </div>
      </div>
    </transition>

    <!-- Detail modal -->
    <transition name="modal">
      <div v-if="detailExec" class="modal-overlay" @click.self="detailExec = null">
        <div class="modal">
          <div class="modal-header">
            <h2>执行详情</h2>
            <button class="modal-close" @click="detailExec = null">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="exec-timeline">
              <div v-for="nr in detailExec.nodeResults" :key="nr.nodeId" class="tl-item">
                <div class="tl-dot" :class="nr.status === 'success' ? 'dot-green' : 'dot-red'"></div>
                <div class="tl-content">
                  <div class="tl-header">
                    <strong>{{ nr.nodeName }}</strong>
                    <span class="tl-type">{{ nr.nodeType }}</span>
                    <span :class="nr.status === 'success' ? 'text-green' : 'text-red'">{{ nr.status }}</span>
                  </div>
                  <pre v-if="nr.output" class="tl-output">{{ formatOutput(nr.output) }}</pre>
                  <div v-if="nr.error" class="tl-error">{{ nr.error }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
const API_BASE = 'http://localhost:8080/api'

export default {
  name: 'WorkflowViewer',
  data() {
    return {
      workflows: [],
      selectedWfId: null,
      selectedWfName: '',
      execHistory: [],
      detailExec: null,
    }
  },
  mounted() { this.fetchWorkflows() },
  methods: {
    async fetchWorkflows() {
      try {
        const res = await fetch(`${API_BASE}/workflow`)
        this.workflows = await res.json()
      } catch { this.workflows = [] }
    },
    statusLabel(s) {
      const map = { draft: '草稿', active: '启用', disabled: '停用' }
      return map[s] || s
    },
    formatDate(d) {
      if (!d) return ''
      return d.substring(0, 10)
    },
    async selectWorkflow(id) {
      this.selectedWfId = id
      const wf = this.workflows.find(w => w.id === id)
      this.selectedWfName = wf?.name || ''
      try {
        const res = await fetch(`${API_BASE}/workflow/${id}/executions`)
        if (res.ok) this.execHistory = await res.json()
        else this.execHistory = []
      } catch { this.execHistory = [] }
    },
    viewExecDetail(exec) {
      this.detailExec = exec
    },
    formatOutput(output) {
      if (typeof output === 'string') return output
      return JSON.stringify(output, null, 2)
    },
  },
}
</script>

<style scoped>
.workflow-viewer {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  max-width: 900px;
  margin: 0 auto;
}

.viewer-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  letter-spacing: -0.3px;
}

.viewer-header h2 svg { color: var(--accent); }

.workflow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.wf-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  cursor: pointer;
  transition: var(--transition);
}

.wf-card:hover {
  border-color: rgba(108,92,231,0.2);
  background: var(--bg-card-hover);
}

.wf-active { border-left: 3px solid var(--green); }

.wf-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.wf-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: var(--text-muted);
  align-items: center;
}

.wf-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.wf-status-draft { background: var(--bg-secondary); color: var(--text-muted); }
.wf-status-active { background: var(--green-bg); color: var(--green); }
.wf-status-disabled { background: var(--red-bg); color: var(--red); }

.wf-mode {
  background: rgba(108,92,231,0.08);
  color: var(--accent);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
}

.wf-date { font-family: 'JetBrains Mono', monospace; font-size: 10px; }

.wf-arrow {
  color: var(--text-muted);
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 20px;
  color: var(--text-muted);
}

.empty-state svg { opacity: 0.4; }
.empty-state p { font-size: 14px; }

.exec-panel {
  animation: fadeInUp 0.3s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.exec-header {
  margin-bottom: 16px;
}

.exec-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.exec-header h3 svg { color: var(--green); }

.exec-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  margin-bottom: 10px;
  transition: var(--transition);
}

.exec-card:hover { border-color: rgba(108,92,231,0.2); }

.exec-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exec-session-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.exec-session-id {
  font-size: 12px;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}

.exec-status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.badge-success { background: var(--green-bg); color: var(--green); }
.badge-fail { background: var(--red-bg); color: var(--red); }

.detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(108,92,231,0.08);
  border: 1px solid rgba(108,92,231,0.15);
  border-radius: 6px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.detail-btn:hover {
  background: rgba(108,92,231,0.14);
}

.exec-error-line {
  margin-top: 8px;
  font-size: 12px;
  color: var(--red);
  padding: 8px 10px;
  background: var(--red-bg);
  border-radius: 6px;
}

.text-green { color: var(--green); }
.text-red { color: var(--red); }

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  backdrop-filter: blur(4px);
}

.modal {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  width: 700px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: var(--shadow-elevated);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

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

.modal-close:hover { background: var(--bg-secondary); color: var(--text-primary); }

.modal-body { padding: 20px 24px; }

.exec-timeline {
  position: relative;
  padding-left: 24px;
}

.exec-timeline::before {
  content: "";
  position: absolute;
  left: 10px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--border-color);
}

.tl-item {
  position: relative;
  margin-bottom: 16px;
}

.tl-dot {
  position: absolute;
  left: -18px;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--bg-card);
}

.dot-green { background: var(--green); box-shadow: 0 0 6px rgba(74,222,128,0.4); }
.dot-red { background: var(--red); box-shadow: 0 0 6px rgba(248,113,113,0.4); }

.tl-content {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
}

.tl-header {
  display: flex;
  gap: 10px;
  font-size: 13px;
  align-items: center;
  margin-bottom: 6px;
}

.tl-header strong { color: var(--text-primary); }

.tl-type {
  color: var(--text-muted);
  font-size: 11px;
  background: var(--bg-elevated);
  padding: 1px 6px;
  border-radius: 4px;
}

.tl-output {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-primary);
  padding: 8px 10px;
  border-radius: 4px;
  margin-top: 6px;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
}

.tl-error {
  font-size: 12px;
  color: var(--red);
  margin-top: 6px;
  padding: 6px 10px;
  background: var(--red-bg);
  border-radius: 4px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform 0.2s ease; }
.modal-enter-from .modal { transform: scale(0.95) translateY(10px); }
.modal-leave-to .modal { transform: scale(0.95) translateY(10px); }
</style>
