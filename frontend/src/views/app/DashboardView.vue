<template>
  <div class="dashboard">
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

    <div class="dash-grid">
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { BrainCircuit, Workflow, MessageSquare, Zap, History } from '@lucide/vue'
import { useAppStore } from '../../stores/app'

const store = useAppStore()
const agentsReady = ref(0)
const agentsTotal = ref(0)
const workflowCount = ref(0)
const sessionCount = ref(0)
const backendStatus = ref('检测中')
const agentList = ref([])
const recentWorkflows = ref([])

onMounted(async () => {
  try {
    const info = await store.apiGet('/info')
    agentList.value = Object.values(info.agents || {})
    agentsTotal.value = agentList.value.length
    agentsReady.value = agentList.value.filter(a => a.ready).length
    backendStatus.value = info.status || '未知'
  } catch (e) {
    backendStatus.value = '离线'
  }
  try {
    const wfs = await store.apiGet('/workflow')
    recentWorkflows.value = (wfs || []).slice(0, 5)
    workflowCount.value = (wfs || []).length
  } catch (e) {}
})
</script>

<style scoped>
.dashboard { padding: 24px; max-width: 1200px; }
.dash-header { margin-bottom: 24px; }
.dash-header h1 { font-size: 24px; font-weight: 700; color: #e8e8f0; }
.dash-subtitle { font-size: 13px; color: #68687d; margin-top: 4px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; }
.stat-icon { flex-shrink: 0; }
.stat-icon.purple { color: #6c5ce7; }
.stat-icon.blue { color: #60a5fa; }
.stat-icon.green { color: #4ade80; }
.stat-icon.orange { color: #fb923c; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 22px; font-weight: 700; color: #e8e8f0; }
.stat-label { font-size: 12px; color: #68687d; margin-top: 2px; }
.dash-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; overflow: hidden; }
.panel-header { padding: 14px 18px; border-bottom: 1px solid rgba(108,92,231,0.15); display: flex; align-items: center; justify-content: space-between; }
.panel-header h2 { font-size: 14px; font-weight: 600; color: #e8e8f0; display: flex; align-items: center; gap: 8px; }
.panel-link { font-size: 12px; color: #6c5ce7; text-decoration: none; }
.panel-body { padding: 12px 18px; }
.agent-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid rgba(108,92,231,0.08); }
.agent-row:last-child { border-bottom: none; }
.agent-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot-on { background: #4ade80; box-shadow: 0 0 6px rgba(74,222,128,0.4); }
.dot-off { background: #f87171; }
.agent-role { flex: 1; font-size: 13px; color: #9898b0; font-family: 'JetBrains Mono', monospace; }
.text-green { color: #4ade80; font-size: 12px; }
.text-red { color: #f87171; font-size: 12px; }
.wf-row { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid rgba(108,92,231,0.08); cursor: pointer; transition: all 0.15s; }
.wf-row:hover { background: rgba(108,92,231,0.04); margin: 0 -18px; padding: 10px 18px; }
.wf-name { flex: 1; font-size: 13px; color: #e8e8f0; font-weight: 500; }
.wf-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.status-active { background: rgba(74,222,128,0.1); color: #4ade80; }
.status-draft { background: rgba(250,204,21,0.1); color: #facc15; }
.status-disabled { background: rgba(248,113,113,0.1); color: #f87171; }
.empty-hint { padding: 20px; text-align: center; color: #68687d; font-size: 13px; }
@media (max-width: 800px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .dash-grid { grid-template-columns: 1fr; } }
</style>
