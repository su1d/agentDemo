<template>
  <div class="workflow-list">
    <div class="list-header">
      <div>
        <h1>工作流</h1>
        <p class="list-subtitle">设计和管理 AI Agent 协作流程</p>
      </div>
      <button class="create-btn" @click="createWorkflow">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建工作流
      </button>
    </div>
    <div class="workflow-grid">
      <div v-for="wf in workflows" :key="wf.id" class="wf-card" @click="openWorkflow(wf.id)">
        <div class="wf-card-header">
          <span class="wf-type-badge">{{ wf.collaborationMode || 'chain' }}</span>
          <span v-if="wf.status" class="wf-status-badge" :class="'status-' + wf.status">{{ wf.status }}</span>
        </div>
        <h3 class="wf-card-title">{{ wf.name }}</h3>
        <p class="wf-card-desc">{{ wf.description || '暂无描述' }}</p>
        <div class="wf-card-footer">
          <span class="wf-meta">创建于 {{ formatTime(wf.createdAt) }}</span>
        </div>
      </div>
    </div>
    <div v-if="workflows.length === 0" class="empty-state">
      <p>还没有工作流，点击上方按钮创建一个</p>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
const store = useAppStore()
const router = useRouter()
const workflows = ref([])
onMounted(async () => {
  try { workflows.value = await store.apiGet('/workflow') } catch (e) { console.error(e) }
})
async function createWorkflow() {
  const name = '新工作流 ' + (workflows.value.length + 1)
  try {
    const wf = await store.apiPost('/workflow', { name })
    router.push('/workflow/' + wf.id)
  } catch (e) { console.error(e) }
}
function openWorkflow(id) { router.push('/workflow/' + id) }
function formatTime(t) { if (!t) return ''; return new Date(t).toLocaleDateString('zh-CN') }
</script>
<style scoped>
.workflow-list { padding: 24px; max-width: 1200px; }
.list-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.list-header h1 { font-size: 24px; font-weight: 700; color: #e8e8f0; }
.list-subtitle { font-size: 13px; color: #68687d; margin-top: 4px; }
.create-btn { display: flex; align-items: center; gap: 6px; padding: 10px 18px; background: #6c5ce7; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.15s; }
.create-btn:hover { background: #8b7cf7; box-shadow: 0 4px 12px rgba(108,92,231,0.3); }
.workflow-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.wf-card { background: var(--bg-card); border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; padding: 18px; cursor: pointer; transition: all 0.15s; }
.wf-card:hover { border-color: #6c5ce7; box-shadow: 0 4px 16px rgba(108,92,231,0.1); }
.wf-card-header { display: flex; justify-content: space-between; margin-bottom: 10px; }
.wf-type-badge { font-size: 10px; padding: 2px 8px; border-radius: 4px; background: rgba(108,92,231,0.1); color: #6c5ce7; font-weight: 500; }
.wf-status-badge { font-size: 10px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.status-active { background: rgba(74,222,128,0.1); color: #4ade80; }
.status-draft { background: rgba(250,204,21,0.1); color: #facc15; }
.status-disabled { background: rgba(248,113,113,0.1); color: #f87171; }
.wf-card-title { font-size: 16px; font-weight: 600; color: #e8e8f0; margin-bottom: 6px; }
.wf-card-desc { font-size: 12px; color: #9898b0; line-height: 1.5; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.wf-card-footer { border-top: 1px solid rgba(108,92,231,0.08); padding-top: 10px; }
.wf-meta { font-size: 11px; color: #68687d; }
.empty-state { text-align: center; padding: 80px 20px; color: #68687d; font-size: 14px; }
</style>
