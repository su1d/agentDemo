<template>
  <div class="dashboard">
    <div class="dash-layout">
      <!-- Left: 3D Office Scene -->
      <div class="office-panel">
        <IsometricOffice
          ref="officeRef"
          :agents="agentList"
          :collaborations="activeCollaborations"
        />
      </div>

      <!-- Right: Stats & Info -->
      <div class="info-panel">
        <header class="dash-header">
          <h1>仪表盘</h1>
          <p class="dash-subtitle">AgentForge 系统概览</p>
        </header>

        <div class="stats-grid">
          <div class="stat-card">
            <BrainCircuit :size="22" class="stat-icon purple" />
            <div class="stat-info">
              <span class="stat-value">{{ agentsReady }}/{{ agentsTotal }}</span>
              <span class="stat-label">Agent 在线</span>
            </div>
          </div>
          <div class="stat-card">
            <Workflow :size="22" class="stat-icon blue" />
            <div class="stat-info">
              <span class="stat-value">{{ workflowCount }}</span>
              <span class="stat-label">工作流</span>
            </div>
          </div>
          <div class="stat-card">
            <MessageSquare :size="22" class="stat-icon green" />
            <div class="stat-info">
              <span class="stat-value">{{ sessionCount }}</span>
              <span class="stat-label">对话次数</span>
            </div>
          </div>
          <div class="stat-card">
            <Zap :size="22" class="stat-icon orange" />
            <div class="stat-info">
              <span class="stat-value">{{ backendStatus }}</span>
              <span class="stat-label">后端状态</span>
            </div>
          </div>
        </div>

        <div class="panels-row">
          <div class="panel panel-agents">
            <div class="panel-header">
              <h2><BrainCircuit :size="16" /> Agent 状态</h2>
            </div>
            <div class="panel-body">
              <div v-for="a in agentList" :key="a.role" class="agent-row">
                <span class="agent-dot" :class="a.ready ? 'dot-on' : 'dot-off'"></span>
                <span class="agent-role">{{ a.role }}</span>
                <span class="agent-status" :class="a.ready ? 'text-green' : 'text-red'">{{ a.ready ? '在线' : '离线' }}</span>
              </div>
            </div>
          </div>

          <div class="panel panel-recent">
            <div class="panel-header">
              <h2><History :size="16" /> 最近工作流</h2>
              <router-link to="/workflow" class="panel-link">查看全部</router-link>
            </div>
            <div class="panel-body">
              <div v-if="recentWorkflows.length === 0" class="empty-hint">暂无工作流</div>
              <div v-for="wf in recentWorkflows" :key="wf.id" class="wf-row" @click="$router.push('/workflow/' + wf.id)">
                <span class="wf-name">{{ wf.name }}</span>
                <span class="wf-status" :class="'status-' + (wf.status || 'draft')">{{ wf.status || 'draft' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { BrainCircuit, Workflow, MessageSquare, Zap, History } from '@lucide/vue'
import { useAppStore } from '../../stores/app'
import IsometricOffice from '../../components/IsometricOffice.vue'

const store = useAppStore()
const officeRef = ref(null)
const agentsReady = ref(0)
const agentsTotal = ref(0)
const workflowCount = ref(0)
const sessionCount = ref(0)
const backendStatus = ref('检测中')
const agentList = ref([])
const recentWorkflows = ref([])
const activeCollaborations = ref([])
const connectionOnline = ref(false)

let pollInterval = null

onMounted(async () => {
  await loadData()
  // Poll every 5 seconds for real-time updates
  pollInterval = setInterval(loadData, 5000)
})

onBeforeUnmount(() => {
  if (pollInterval) clearInterval(pollInterval)
})

async function loadData() {
  try {
    const info = await store.apiGet('/info')
    const agents = Object.values(info.agents || {})
    agentList.value = agents
    agentsTotal.value = agents.length
    agentsReady.value = agents.filter(a => a.ready).length
    backendStatus.value = info.status || '未知'
    connectionOnline.value = true

    // Update 3D scene with agent statuses
    if (officeRef.value) {
      officeRef.value.setConnectionStatus(true)
      agents.forEach(a => {
        const status = a.ready ? 'idle' : 'offline'
        officeRef.value.updateAgentStatus(a.role, status)
      })
    }
  } catch (e) {
    backendStatus.value = '离线'
    connectionOnline.value = false
    if (officeRef.value) {
      officeRef.value.setConnectionStatus(false)
    }
  }

  try {
    const wfs = await store.apiGet('/workflow')
    recentWorkflows.value = (wfs || []).slice(0, 5)
    workflowCount.value = (wfs || []).length
  } catch (e) {}

  // Simulate collaboration links between agents for demo visualization
  // In production, these would come from the backend
  updateCollaborationDemo()
}

function updateCollaborationDemo() {
  // Create dynamic collaboration links based on which agents are online
  const onlineAgents = agentList.value.filter(a => a.ready).map(a => a.role)
  const links = []
  if (onlineAgents.length >= 2) {
    for (let i = 0; i < onlineAgents.length - 1; i++) {
      links.push({
        from: onlineAgents[i],
        to: onlineAgents[i + 1],
        color: '#6c5ce7'
      })
    }
    // Also connect last to orchestrator if available
    if (onlineAgents.includes('orchestrator')) {
      links.push({
        from: onlineAgents[onlineAgents.length - 1],
        to: 'orchestrator',
        color: '#8b7cf7'
      })
    }
  }
  activeCollaborations.value = links
  if (officeRef.value) {
    officeRef.value.updateCollaborationLines(links)
  }
}
</script>

<style scoped>
.dashboard {
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
}

.dash-layout {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: calc(100vh - 84px);
}

/* 3D Office takes the left ~60% */
.office-panel {
  flex: 3;
  min-width: 0;
  border-radius: 12px;
  overflow: hidden;
  background: #0d0d1a;
  border: 1px solid rgba(108,92,231,0.12);
}

/* Info panel takes the right ~40% */
.info-panel {
  flex: 2;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding-right: 4px;
}

.dash-header { margin-bottom: 4px; }
.dash-header h1 { font-size: 22px; font-weight: 700; color: #e8e8f0; }
.dash-subtitle { font-size: 12px; color: #68687d; margin-top: 2px; }

.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat-card { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.stat-icon { flex-shrink: 0; }
.stat-icon.purple { color: #6c5ce7; }
.stat-icon.blue { color: #60a5fa; }
.stat-icon.green { color: #4ade80; }
.stat-icon.orange { color: #fb923c; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 20px; font-weight: 700; color: #e8e8f0; }
.stat-label { font-size: 11px; color: #68687d; margin-top: 1px; }

.panels-row { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.panel { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; overflow: hidden; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid rgba(108,92,231,0.15); display: flex; align-items: center; justify-content: space-between; }
.panel-header h2 { font-size: 13px; font-weight: 600; color: #e8e8f0; display: flex; align-items: center; gap: 6px; }
.panel-link { font-size: 11px; color: #6c5ce7; text-decoration: none; }
.panel-body { padding: 8px 16px; }
.agent-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid rgba(108,92,231,0.08); }
.agent-row:last-child { border-bottom: none; }
.agent-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dot-on { background: #4ade80; box-shadow: 0 0 5px rgba(74,222,128,0.4); }
.dot-off { background: #f87171; }
.agent-role { flex: 1; font-size: 12px; color: #9898b0; font-family: 'JetBrains Mono', monospace; }
.text-green { color: #4ade80; font-size: 11px; }
.text-red { color: #f87171; font-size: 11px; }
.wf-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid rgba(108,92,231,0.08); cursor: pointer; transition: all 0.15s; }
.wf-row:hover { background: rgba(108,92,231,0.04); margin: 0 -16px; padding: 8px 16px; }
.wf-name { flex: 1; font-size: 12px; color: #e8e8f0; font-weight: 500; }
.wf-status { font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 500; }
.status-active { background: rgba(74,222,128,0.1); color: #4ade80; }
.status-draft { background: rgba(250,204,21,0.1); color: #facc15; }
.status-disabled { background: rgba(248,113,113,0.1); color: #f87171; }
.empty-hint { padding: 16px; text-align: center; color: #68687d; font-size: 12px; }

@media (max-width: 900px) {
  .dash-layout { flex-direction: column; }
  .office-panel { flex: none; height: 50vh; }
}
</style>

