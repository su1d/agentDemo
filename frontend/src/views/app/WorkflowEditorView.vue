<template>
  <div class="workflow-editor-page">
    <div class="editor-toolbar">
      <button class="back-btn" @click="$router.push('/workflow')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
        返回
      </button>
      <div class="toolbar-info">
        <input v-model="workflowName" class="title-input" placeholder="工作流名称" />
        <select v-model="workflowStatus" class="status-select" @change="saveWorkflow">
          <option value="draft">草稿</option>
          <option value="active">启用</option>
          <option value="disabled">停用</option>
        </select>
      </div>
      <div class="toolbar-actions">
        <button class="t-btn save-btn" @click="saveWorkflow">保存</button>
        <button class="t-btn exec-btn" @click="executeWorkflow" :disabled="!workflowId">运行</button>
      </div>
    </div>

    <div class="flow-container" ref="flowContainer">
      <WorkflowFlowEditor ref="flowEditor" :nodes="nodes" :edges="edges" @update:nodes="nodes = $event" @update:edges="edges = $event" />
    </div>

    <!-- Node Config Panel -->
    <transition name="slide">
      <div v-if="selectedNode" class="config-panel">
        <div class="config-header">
          <h3>节点配置</h3>
          <button class="close-btn" @click="selectedNode = null"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
        </div>
        <div class="config-body">
          <div class="field">
            <label>名称</label>
            <input v-model="selectedNode.data.label" placeholder="节点名称" />
          </div>
          <div class="field">
            <label>类型</label>
            <input :value="selectedNode.type" disabled />
          </div>
          <div class="field" v-if="selectedNode.type === 'task'">
            <label>Agent 角色</label>
            <select v-model="selectedNode.data.agentRole">
              <option value="">无</option>
              <option v-for="a in agents" :key="a" :value="a">{{ a }}</option>
            </select>
          </div>
          <div class="field" v-if="selectedNode.type === 'task'">
            <label>提示词模板</label>
            <textarea v-model="selectedNode.data.prompt" rows="3" placeholder="使用 {{var}} 引用变量"></textarea>
          </div>
          <div class="field" v-if="selectedNode.type === 'condition'">
            <label>条件表达式</label>
            <input v-model="selectedNode.data.condition" placeholder="如: result.success === true" />
          </div>
          <div class="field" v-if="selectedNode.type === 'parallel'">
            <label>并行角色（逗号分隔）</label>
            <input v-model="selectedNode.data.parallelRoles" placeholder="calculator,searcher" />
          </div>
        </div>
      </div>
    </transition>

    <!-- Execution Result Modal -->
    <transition name="modal">
      <div v-if="execResult" class="modal-overlay" @click.self="execResult = null">
        <div class="modal">
          <div class="modal-header">
            <h2>执行结果</h2>
            <button class="close-btn" @click="execResult = null"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="modal-body">
            <pre class="result-json">{{ JSON.stringify(execResult, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '../../stores/app'
import WorkflowFlowEditor from '../../components/flow/WorkflowFlowEditor.vue'

const route = useRoute()
const store = useAppStore()

const workflowId = ref(null)
const workflowName = ref('')
const workflowStatus = ref('draft')
const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const execResult = ref(null)

const agents = ['calculator', 'weather', 'searcher', 'summarizer', 'requirement_agent', 'data_collect_agent', 'data_analyze_agent', 'visualization_agent', 'report_agent']

onMounted(async () => {
  const id = route.params.id
  if (id) await loadWorkflow(id)
})

watch(nodes, () => { updateSelection() }, { deep: true })

function updateSelection() {
  // Keep selectedNode in sync with panel data changes
}

async function loadWorkflow(id) {
  try {
    const graph = await store.apiGet('/workflow/' + id)
    workflowId.value = graph.workflow.id
    workflowName.value = graph.workflow.name
    workflowStatus.value = graph.workflow.status || 'draft'
    nodes.value = (graph.nodes || []).map(n => ({
      id: String(n.id),
      type: n.nodeType === 'task' ? 'task' : n.nodeType === 'start' ? 'start' : n.nodeType,
      position: { x: n.posX || 0, y: n.posY || 0 },
      data: { label: n.name, agentRole: n.agentRole, prompt: n.inputTemplate, condition: n.conditionExpression, parallelRoles: n.parallelRoles }
    }))
    edges.value = (graph.edges || []).map(e => ({
      id: String(e.id),
      source: String(e.sourceNodeId),
      target: String(e.targetNodeId),
      label: e.label || '',
    }))
  } catch (e) { console.error(e) }
}

function addNode(type) {
  const labels = { start: '开始', task: '新任务', condition: '条件判断', parallel: '并行分支', merge: '合并节点', end: '结束' }
  const id = 'n_' + Date.now()
  const pos = { x: 100 + Math.random() * 200, y: 100 + Math.random() * 200 }
  nodes.value = [...nodes.value, { id, type, position: pos, data: { label: labels[type] || type } }]
}
async function saveWorkflow() {
  const body = {
    name: workflowName.value || '未命名工作流',
    status: workflowStatus.value,
    nodes: nodes.value.map(n => ({
      nodeType: n.type,
      name: n.data.label,
      posX: n.position.x,
      posY: n.position.y,
      agentRole: n.data.agentRole,
      conditionExpression: n.data.condition,
      parallelRoles: n.data.parallelRoles,
      inputTemplate: n.data.prompt,
    })),
    edges: edges.value.map(e => ({
      sourceNodeId: parseInt(e.source),
      targetNodeId: parseInt(e.target),
      label: e.label,
    })),
  }
  try {
    const result = workflowId.value
      ? await store.apiPut('/workflow/' + workflowId.value, body)
      : await store.apiPost('/workflow', body)
    if (result.id) workflowId.value = result.id
  } catch (e) { console.error(e) }
}

async function executeWorkflow() {
  if (!workflowId.value) return
  await saveWorkflow()
  try {
    execResult.value = await store.apiPost('/workflow/' + workflowId.value + '/execute', { variables: { message: '' } })
  } catch (e) {
    execResult.value = { error: e.message }
  }
}
</script>

<style scoped>
.workflow-editor-page { display: flex; flex-direction: column; height: 100%; position: relative; }
.editor-toolbar { display: flex; align-items: center; gap: 12px; padding: 8px 16px; background: #141428; border-bottom: 1px solid rgba(108,92,231,0.15); flex-shrink: 0; }
.back-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: transparent; color: #9898b0; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.15s; }
.back-btn:hover { background: rgba(108,92,231,0.06); color: #e8e8f0; }
.toolbar-info { flex: 1; display: flex; align-items: center; gap: 8px; }
.title-input { padding: 6px 10px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #e8e8f0; font-size: 14px; font-weight: 600; width: 200px; outline: none; }
.title-input:focus { border-color: #6c5ce7; }
.status-select { padding: 6px 8px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #9898b0; font-size: 12px; cursor: pointer; outline: none; }
.toolbar-actions { display: flex; gap: 8px; }
.t-btn { padding: 8px 18px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s; }
.save-btn { background: #6c5ce7; color: #fff; }
.save-btn:hover { background: #8b7cf7; }
.exec-btn { background: linear-gradient(135deg, #10b981, #34d399); color: #fff; }
.exec-btn:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(16,185,129,0.3); }
.exec-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.node-palette {
  position: absolute; left: 12px; top: 12px; z-index: 10;
  background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px;
  padding: 10px; display: flex; flex-direction: column; gap: 6px; min-width: 80px;
}
.palette-title { font-size: 10px; font-weight: 600; color: #68687d; text-transform: uppercase; letter-spacing: 0.3px; text-align: center; margin-bottom: 2px; }
.palette-items { display: flex; flex-direction: column; gap: 4px; }
.palette-item {
  padding: 6px 10px; border-radius: 5px; font-size: 11px; font-weight: 500;
  cursor: pointer; transition: all 0.15s; text-align: center;
  border: 1px solid rgba(108,92,231,0.15); background: #141428; color: #9898b0;
}
.palette-item:hover { border-color: #6c5ce7; color: #6c5ce7; background: rgba(108,92,231,0.06); }
.palette-item.start { border-left: 3px solid #4ade80; }
.palette-item.task { border-left: 3px solid #6c5ce7; }
.palette-item.condition { border-left: 3px solid #facc15; }
.palette-item.parallel { border-left: 3px solid #fb923c; }
.palette-item.merge { border-left: 3px solid #a855f7; }
.palette-item.end { border-left: 3px solid #f87171; }
.flow-container { flex: 1; position: relative; overflow: hidden; }
.config-panel { position: fixed; right: 16px; top: 64px; width: 280px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; z-index: 100; box-shadow: 0 8px 32px rgba(0,0,0,0.3); overflow: hidden; }
.config-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid rgba(108,92,231,0.15); }
.config-header h3 { font-size: 14px; font-weight: 700; color: #e8e8f0; }
.close-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; background: transparent; color: #68687d; cursor: pointer; transition: all 0.15s; }
.close-btn:hover { background: rgba(248,113,113,0.1); color: #f87171; }
.config-body { padding: 12px 16px; display: flex; flex-direction: column; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 500; color: #68687d; }
.field input, .field select, .field textarea { padding: 6px 8px; background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #e8e8f0; font-size: 12px; outline: none; }
.field input:focus, .field select:focus, .field textarea:focus { border-color: #6c5ce7; }
.field textarea { resize: vertical; min-height: 44px; font-family: 'JetBrains Mono', monospace; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 200; backdrop-filter: blur(4px); }
.modal { background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 14px; width: 640px; max-height: 80vh; overflow-y: auto; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; border-bottom: 1px solid rgba(108,92,231,0.15); }
.modal-header h2 { font-size: 16px; font-weight: 700; color: #e8e8f0; }
.modal-body { padding: 20px 24px; }
.result-json { font-size: 12px; color: #9898b0; background: #0d0d1a; padding: 16px; border-radius: 8px; overflow-x: auto; white-space: pre-wrap; font-family: 'JetBrains Mono', monospace; }
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(20px); }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
