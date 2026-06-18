<template>
  <div class="workflow-viewer">
    <div class="viewer-header">
      <h2>工作流执行历史</h2>
    </div>

    <!-- Workflow list -->
    <div class="workflow-list">
      <div v-for="wf in workflows" :key="wf.id" class="wf-card"
        :class="{ 'wf-active': wf.status === 'active' }"
        @click="selectWorkflow(wf.id)">
        <div class="wf-name">{{ wf.name }}</div>
        <div class="wf-meta">
          <span class="wf-status" :class="'wf-status-' + wf.status">{{ wf.status }}</span>
          <span class="wf-mode">{{ wf.collaborationMode }}</span>
          <span class="wf-date">{{ wf.createdAt?.substring(0, 10) }}</span>
        </div>
      </div>
      <div v-if="workflows.length === 0" class="empty">暂无工作流</div>
    </div>

    <!-- Execution history for selected workflow -->
    <div v-if="selectedWfId" class="exec-history">
      <h3>{{ selectedWfName }} - 执行记录</h3>
      <div v-for="exec in execHistory" :key="exec.sessionId" class="exec-card">
        <div class="exec-header">
          <span class="exec-session">会话: {{ exec.sessionId }}</span>
          <span :class="exec.status === 'success' ? 'text-green' : 'text-red'">{{ exec.status }}</span>
        </div>
        <div v-if="exec.error" class="exec-error">{{ exec.error }}</div>
        <button class="view-detail-btn" @click="viewExecDetail(exec)">查看详情</button>
      </div>
      <div v-if="execHistory.length === 0" class="empty">暂无执行记录</div>
    </div>

    <!-- Execution detail modal -->
    <div v-if="detailExec" class="modal-overlay" @click.self="detailExec = null">
      <div class="modal">
        <div class="modal-header">
          <h2>执行详情 - {{ detailExec.sessionId }}</h2>
          <button class="modal-close" @click="detailExec = null">✕</button>
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
      detailExec: null
    }
  },
  mounted() {
    this.fetchWorkflows()
  },
  methods: {
    async fetchWorkflows() {
      try {
        const res = await fetch(`${API_BASE}/workflow`)
        this.workflows = await res.json()
      } catch { this.workflows = [] }
    },
    async selectWorkflow(id) {
      this.selectedWfId = id
      const wf = this.workflows.find(w => w.id === id)
      this.selectedWfName = wf?.name || ''
      // Fetch execution logs for this workflow
      try {
        const res = await fetch(`${API_BASE}/workflow/${id}`)
        const data = await res.json()
        // For now just show the workflow graph
        this.execHistory = []
      } catch { this.execHistory = [] }
    },
    viewExecDetail(exec) {
      this.detailExec = exec
    },
    formatOutput(output) {
      if (typeof output === 'string') return output
      return JSON.stringify(output, null, 2)
    }
  }
}
</script>

<style scoped>
.workflow-viewer { padding: 20px; height: 100%; overflow-y: auto; }
.viewer-header h2 { font-size: 18px; color: #7c7cff; margin: 0 0 16px; }
.workflow-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; }
.wf-card { background: #1a1a35; border: 1px solid #2a2a4a; border-radius: 8px; padding: 12px; cursor: pointer; }
.wf-card:hover { border-color: #5a5a8a; }
.wf-active { border-left: 3px solid #4ade80; }
.wf-name { font-size: 14px; color: #e0e0e0; font-weight: 500; margin-bottom: 4px; }
.wf-meta { display: flex; gap: 8px; font-size: 11px; color: #9090b0; }
.wf-status { padding: 1px 6px; border-radius: 4px; font-size: 10px; }
.wf-status-draft { background: #2a2a3a; color: #9090b0; }
.wf-status-active { background: #1a3a1a; color: #4ade80; }
.wf-status-disabled { background: #3a1a1a; color: #f87171; }
.exec-history h3 { font-size: 14px; color: #e0e0e0; margin: 0 0 12px; }
.exec-card { background: #15152a; border: 1px solid #2a2a4a; border-radius: 6px; padding: 10px; margin-bottom: 8px; }
.exec-header { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.exec-session { color: #a0a0b0; }
.text-green { color: #4ade80; }
.text-red { color: #f87171; }
.exec-error { font-size: 11px; color: #f87171; }
.view-detail-btn { margin-top: 6px; padding: 4px 10px; background: #2a2a5a; color: #7c7cff; border: none; border-radius: 4px; font-size: 11px; cursor: pointer; }
.empty { font-size: 13px; color: #505060; padding: 20px; text-align: center; }
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #1a1a2e; border: 1px solid #2a2a4a; border-radius: 12px; width: 700px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #2a2a4a; }
.modal-header h2 { font-size: 16px; color: #7c7cff; margin: 0; }
.modal-close { background: none; border: 1px solid #2a2a4a; color: #a0a0b0; border-radius: 4px; padding: 2px 8px; cursor: pointer; }
.modal-body { padding: 20px; }
.exec-timeline { position: relative; padding-left: 20px; }
.exec-timeline::before { content: ""; position: absolute; left: 8px; top: 0; bottom: 0; width: 2px; background: #2a2a4a; }
.tl-item { position: relative; margin-bottom: 16px; }
.tl-dot { position: absolute; left: -16px; top: 4px; width: 10px; height: 10px; border-radius: 50%; }
.dot-green { background: #4ade80; }
.dot-red { background: #f87171; }
.tl-content { background: #15152a; border: 1px solid #2a2a4a; border-radius: 6px; padding: 8px; }
.tl-header { display: flex; gap: 8px; font-size: 12px; margin-bottom: 4px; }
.tl-type { color: #9090b0; font-size: 10px; }
.tl-output { font-size: 11px; color: #c0c0d0; background: #0f0f1a; padding: 6px; border-radius: 4px; margin-top: 4px; max-height: 80px; overflow-y: auto; white-space: pre-wrap; }
.tl-error { font-size: 11px; color: #f87171; margin-top: 4px; }
h3 { font-size: 13px; color: #7c7cff; margin: 0; }
</style>
