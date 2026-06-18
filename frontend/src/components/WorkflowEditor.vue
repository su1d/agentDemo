<template>
  <div class="workflow-editor">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <h2 v-if="workflowName">{{ workflowName }}</h2>
        <input v-else v-model="newWorkflowName" placeholder="工作流名称" class="title-input" />
        <select v-model="workflowStatus" class="status-select" @change="updateWorkflowStatus">
          <option value="draft">草稿</option>
          <option value="active">启用</option>
          <option value="disabled">停用</option>
        </select>
      </div>
      <div class="toolbar-right">
        <button class="toolbar-btn save-btn" @click="saveWorkflow">💾 保存</button>
        <button class="toolbar-btn execute-btn" @click="executeWorkflow" :disabled="!workflowId">▶ 运行</button>
      </div>
    </div>

    <div class="editor-layout">
      <!-- Node palette -->
      <div class="node-palette">
        <h3>节点类型</h3>
        <div class="palette-items">
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'start')">
            <span class="node-icon start-icon">▶</span> 开始
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'task')">
            <span class="node-icon task-icon">⚙</span> 任务
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'condition')">
            <span class="node-icon condition-icon">◇</span> 条件
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'parallel')">
            <span class="node-icon parallel-icon">⇄</span> 并行
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'merge')">
            <span class="node-icon merge-icon">⤵</span> 合并
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'end')">
            <span class="node-icon end-icon">■</span> 结束
          </div>
        </div>
        <hr />
        <h3>Agent 角色</h3>
        <div class="agent-list">
          <div v-for="a in agents" :key="a.role" class="agent-info">
            <span class="agent-dot" :class="a.ready ? 'dot-green' : 'dot-red'"></span>
            <code>{{ a.role }}</code>
          </div>
        </div>
      </div>

      <!-- Canvas -->
      <div class="canvas-container" ref="canvasRef"
        @drop="onDrop" @dragover.prevent
        @click.self="deselectAll">
        <svg class="edges-layer" :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }">
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#4a4a6a" />
            </marker>
            <marker id="arrowhead-selected" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#7c7cff" />
            </marker>
          </defs>
          <path v-for="edge in edges" :key="edge.id || edge._tempId"
            :d="getEdgePath(edge)"
            :class="['edge-line', { 'edge-selected': selectedEdgeId === edge.id }]"
            marker-end="url(#arrowhead)"
            @click="selectEdge(edge.id)"
            @dblclick="deleteEdge(edge.id)" />
          <!-- Edge labels -->
          <text v-for="edge in edges" :key="'lbl-' + (edge.id || edge._tempId)"
            :x="getEdgeMidpoint(edge).x" :y="getEdgeMidpoint(edge).y"
            text-anchor="middle" fill="#9090b0" font-size="10"
            @click="selectEdge(edge.id)">
            {{ edge.label || edge.conditionExpression || '' }}
          </text>
        </svg>

        <!-- Nodes -->
        <div v-for="node in nodes" :key="node.id || node._tempId"
          class="canvas-node"
          :class="[
            'node-type-' + node.nodeType,
            { 'node-selected': selectedNodeId === node.id,
              'node-dragging': draggingNodeId === node.id }
          ]"
          :style="{
            left: node.posX + 'px',
            top: node.posY + 'px'
          }"
          @mousedown.stop="startDrag($event, node)"
          @click.stop="selectNode(node)"
          @dblclick.stop="editNode(node)">
          <div class="node-header">
            <span class="node-type-badge">{{ nodeTypeLabel(node.nodeType) }}</span>
            <span class="node-name">{{ node.name }}</span>
          </div>
          <div class="node-body">
            <span v-if="node.agentRole" class="node-role">{{ node.agentRole }}</span>
            <span v-if="node.conditionExpression" class="node-condition">{{ node.conditionExpression }}</span>
            <span v-if="node.parallelRoles" class="node-parallel">{{ node.parallelRoles }}</span>
          </div>
          <!-- Input/Output handles -->
          <div class="handle handle-input" @mousedown.stop @mouseup="startEdgeConnect(node.id, 'input')"></div>
          <div class="handle handle-output" @mousedown.stop @mouseup="startEdgeConnect(node.id, 'output')"></div>
        </div>
      </div>
    </div>

    <!-- Node property editor -->
    <div v-if="selectedNode" class="node-properties">
      <h3>节点属性</h3>
      <label>名称: <input v-model="selectedNode.name" @change="updateNode" /></label>
      <label>类型: <select v-model="selectedNode.nodeType" @change="updateNode" :disabled="isSystemNodeType(selectedNode.nodeType)">
        <option value="start">开始</option>
        <option value="task">任务</option>
        <option value="condition">条件</option>
        <option value="parallel">并行</option>
        <option value="merge">合并</option>
        <option value="end">结束</option>
      </select></label>
      <label v-if="selectedNode.nodeType === 'task'">Agent 角色:
        <select v-model="selectedNode.agentRole" @change="updateNode">
          <option v-for="a in agents" :key="a.role" :value="a.role">{{ a.role }}</option>
        </select>
      </label>
      <label v-if="selectedNode.nodeType === 'condition'">条件表达式:
        <input v-model="selectedNode.conditionExpression" placeholder="success / contains:xxx / equals:key:val" @change="updateNode" />
      </label>
      <label v-if="selectedNode.nodeType === 'parallel'">并行角色 (逗号分隔):
        <input v-model="selectedNode.parallelRoles" placeholder="calculator,searcher,summarizer" @change="updateNode" />
      </label>
      <label><input type="checkbox" v-model="showInputTemplate" /> 输入模板</label>
      <textarea v-if="showInputTemplate" v-model="selectedNode.inputTemplate" rows="3" @change="updateNode" placeholder="支持 {{变量}} 替换"></textarea>
      <label>超时(秒): <input type="number" v-model.number="selectedNode.timeoutSeconds" min="5" @change="updateNode" /></label>
      <label>重试次数: <input type="number" v-model.number="selectedNode.retryCount" min="0" @change="updateNode" /></label>
      <button class="delete-btn" @click="deleteSelectedNode">删除节点</button>
    </div>

    <!-- Execution result modal -->
    <div v-if="execResult" class="modal-overlay" @click.self="execResult = null">
      <div class="modal exec-modal">
        <div class="modal-header">
          <h2>工作流执行结果</h2>
          <button class="modal-close" @click="execResult = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="exec-status" :class="execResult.status === 'success' ? 'status-ok' : 'status-warn'">
            状态: {{ execResult.status === 'success' ? '成功' : execResult.status }}
          </div>
          <h4>节点执行详情</h4>
          <div v-for="nr in execResult.nodeResults" :key="nr.nodeId" class="exec-node-card">
            <div class="exec-node-header">
              <span class="exec-node-name">{{ nr.nodeName }}</span>
              <span class="exec-node-type">{{ nr.nodeType }}</span>
              <span :class="['exec-node-status', nr.status === 'success' ? 'text-green' : 'text-red']">{{ nr.status }}</span>
            </div>
            <div v-if="nr.error" class="exec-error">{{ nr.error }}</div>
            <pre v-if="nr.output" class="exec-output">{{ formatOutput(nr.output) }}</pre>
          </div>
          <h4>最终上下文</h4>
          <pre class="exec-context">{{ JSON.stringify(execResult.context, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const API_BASE = 'http://localhost:8080/api'

export default {
  name: 'WorkflowEditor',
  props: {
    workflowId: { type: Number, default: null }
  },
  data() {
    return {
      nodes: [],
      edges: [],
      agents: [],
      selectedNodeId: null,
      selectedEdgeId: null,
      draggingNodeId: null,
      dragOffset: { x: 0, y: 0 },
      canvasWidth: 2000,
      canvasHeight: 1200,
      workflowName: '',
      newWorkflowName: '',
      workflowStatus: 'draft',
      showInputTemplate: false,
      execResult: null,
      edgeConnectSource: null,
      nodeCounter: 0
    }
  },
  computed: {
    selectedNode() {
      return this.nodes.find(n => n.id === this.selectedNodeId) || null
    }
  },
  mounted() {
    this.fetchAgents()
    if (this.workflowId) {
      this.loadWorkflow(this.workflowId)
    }
  },
  watch: {
    workflowId(val) {
      if (val) this.loadWorkflow(val)
    }
  },
  methods: {
    async fetchAgents() {
      try {
        const res = await fetch(`${API_BASE}/agents`)
        const data = await res.json()
        this.agents = Object.entries(data).map(([role, info]) => ({ role, ...info }))
      } catch { this.agents = [] }
    },
    async loadWorkflow(id) {
      try {
        const res = await fetch(`${API_BASE}/workflow/${id}`)
        const data = await res.json()
        this.workflowName = data.workflow?.name || ''
        this.workflowStatus = data.workflow?.status || 'draft'
        this.nodes = (data.nodes || []).map(n => ({ ...n }))
        this.edges = (data.edges || []).map(e => ({ ...e }))
        // Adjust canvas
        for (const n of this.nodes) {
          if ((n.posX || 0) + 200 > this.canvasWidth) this.canvasWidth = n.posX + 400
          if ((n.posY || 0) + 150 > this.canvasHeight) this.canvasHeight = n.posY + 300
        }
      } catch (e) {
        alert('加载工作流失败: ' + e.message)
      }
    },
    async saveWorkflow() {
      try {
        // Create workflow if no ID
        let wfId = this.workflowId
        if (!wfId) {
          const name = this.newWorkflowName || '未命名工作流'
          const res = await fetch(`${API_BASE}/workflow`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description: '通过可视化编辑器创建' })
          })
          const wf = await res.json()
          wfId = wf.id
          this.workflowId = wfId
          this.workflowName = name
          this.$emit('created', wfId)
        }

        // Save nodes
        for (const node of this.nodes) {
          if (node._tempId) {
            const res = await fetch(`${API_BASE}/workflow/${wfId}/nodes`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: node.name,
                nodeType: node.nodeType,
                agentRole: node.agentRole,
                posX: node.posX,
                posY: node.posY,
                inputTemplate: node.inputTemplate,
                conditionExpression: node.conditionExpression,
                parallelRoles: node.parallelRoles,
                timeoutSeconds: node.timeoutSeconds,
                retryCount: node.retryCount
              })
            })
            const saved = await res.json()
            node.id = saved.id
            delete node._tempId
          } else {
            await fetch(`${API_BASE}/workflow/nodes/${node.id}`, {
              method: 'PUT',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: node.name,
                nodeType: node.nodeType,
                agentRole: node.agentRole,
                posX: node.posX,
                posY: node.posY,
                conditionExpression: node.conditionExpression,
                parallelRoles: node.parallelRoles
              })
            })
          }
        }

        // Save edges
        for (const edge of this.edges) {
          if (edge._tempId) {
            await fetch(`${API_BASE}/workflow/${wfId}/edges`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                sourceNodeId: edge.sourceNodeId,
                targetNodeId: edge.targetNodeId,
                conditionExpression: edge.conditionExpression || null,
                label: edge.label || null
              })
            })
            delete edge._tempId
          }
        }

        alert('保存成功!')
      } catch (e) {
        alert('保存失败: ' + e.message)
      }
    },
    async executeWorkflow() {
      if (!this.workflowId) return
      try {
        const msg = prompt('请输入消息内容:', 'Hello, show me the weather in Beijing')
        const res = await fetch(`${API_BASE}/workflow/${this.workflowId}/execute`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ variables: { message: msg || '' } })
        })
        const result = await res.json()
        this.execResult = result
      } catch (e) {
        alert('执行失败: ' + e.message)
      }
    },
    async updateWorkflowStatus() {
      if (!this.workflowId) return
      try {
        await fetch(`${API_BASE}/workflow/${this.workflowId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: this.workflowStatus })
        })
      } catch {}
    },
    onDragStart(e, type) {
      e.dataTransfer.setData('nodeType', type)
      e.dataTransfer.effectAllowed = 'copy'
    },
    onDrop(e) {
      const type = e.dataTransfer.getData('nodeType')
      if (!type) return
      const rect = this.$refs.canvasRef.getBoundingClientRect()
      const posX = e.clientX - rect.left - 75
      const posY = e.clientY - rect.top - 30
      this.nodeCounter++
      const names = { start: '开始', task: '任务', condition: '条件', parallel: '并行', merge: '合并', end: '结束' }
      this.nodes.push({
        _tempId: 'new_' + this.nodeCounter,
        name: names[type] || type,
        nodeType: type,
        agentRole: type === 'task' ? 'assistant' : null,
        posX: Math.max(0, posX),
        posY: Math.max(0, posY),
        inputTemplate: '',
        conditionExpression: null,
        parallelRoles: type === 'parallel' ? 'calculator,searcher,summarizer' : null,
        timeoutSeconds: 60,
        retryCount: 0,
        workflowId: this.workflowId
      })
    },
    startDrag(e, node) {
      if (this.edgeConnectSource) return
      this.draggingNodeId = node.id
      this.dragOffset = { x: e.clientX - node.posX, y: e.clientY - node.posY }
      const onMove = (ev) => {
        if (this.draggingNodeId !== node.id) return
        node.posX = Math.max(0, ev.clientX - this.dragOffset.x)
        node.posY = Math.max(0, ev.clientY - this.dragOffset.y)
      }
      const onUp = () => {
        this.draggingNodeId = null
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
        if (this.workflowId) this.saveWorkflow()
      }
      document.addEventListener('mousemove', onMove)
      document.addEventListener('mouseup', onUp)
    },
    startEdgeConnect(nodeId, type) {
      if (type === 'output') {
        this.edgeConnectSource = nodeId
        document.addEventListener('mouseup', () => {
          this.edgeConnectSource = null
        }, { once: true })
      }
    },
    selectNode(node) {
      this.selectedNodeId = node.id
      this.selectedEdgeId = null
      this.showInputTemplate = !!node.inputTemplate
    },
    editNode(node) {
      this.selectNode(node)
    },
    selectEdge(id) {
      this.selectedEdgeId = id
      this.selectedNodeId = null
    },
    deselectAll() {
      this.selectedNodeId = null
      this.selectedEdgeId = null
    },
    updateNode() {
      // Auto save handled by save button
    },
    isSystemNodeType(type) {
      return ['start', 'end'].includes(type)
    },
    async deleteSelectedNode() {
      if (!this.selectedNode) return
      const nodeId = this.selectedNode.id
      this.nodes = this.nodes.filter(n => n.id !== nodeId)
      this.edges = this.edges.filter(e => e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId)
      this.selectedNodeId = null
      if (!nodeId.toString().startsWith('new_')) {
        try { await fetch(`${API_BASE}/workflow/nodes/${nodeId}`, { method: 'DELETE' }) } catch {}
      }
    },
    async deleteEdge(id) {
      this.edges = this.edges.filter(e => e.id !== id && e._tempId !== id)
      if (!id.toString().startsWith('new_')) {
        try { await fetch(`${API_BASE}/workflow/edges/${id}`, { method: 'DELETE' }) } catch {}
      }
    },
    nodeTypeLabel(type) {
      const labels = { start: '开始', end: '结束', task: '任务', condition: '条件', parallel: '并行', merge: '合并' }
      return labels[type] || type
    },
    getEdgePath(edge) {
      const src = this.nodes.find(n => n.id === edge.sourceNodeId)
      const tgt = this.nodes.find(n => n.id === edge.targetNodeId)
      if (!src || !tgt) return ''
      const x1 = src.posX + 140, y1 = src.posY + 40
      const x2 = tgt.posX, y2 = tgt.posY + 20
      const cx = (x1 + x2) / 2
      return `M ${x1} ${y1} C ${cx} ${y1}, ${cx} ${y2}, ${x2} ${y2}`
    },
    getEdgeMidpoint(edge) {
      const src = this.nodes.find(n => n.id === edge.sourceNodeId)
      const tgt = this.nodes.find(n => n.id === edge.targetNodeId)
      if (!src || !tgt) return { x: 0, y: 0 }
      return { x: (src.posX + tgt.posX) / 2 + 70, y: (src.posY + tgt.posY) / 2 + 10 }
    },
    formatOutput(output) {
      if (typeof output === 'string') return output
      return JSON.stringify(output, null, 2)
    }
  }
}
</script>

<style scoped>
.workflow-editor { display: flex; flex-direction: column; height: 100%; }
.editor-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 8px 16px; background: #1a1a2e; border-bottom: 1px solid #2a2a4a; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.toolbar-left h2 { font-size: 16px; color: #e0e0e0; margin: 0; }
.title-input { padding: 6px 10px; border: 1px solid #2a2a4a; border-radius: 4px; background: #0f0f1a; color: #e0e0e0; font-size: 14px; width: 200px; }
.status-select { padding: 4px 8px; border: 1px solid #2a2a4a; border-radius: 4px; background: #0f0f1a; color: #e0e0e0; }
.toolbar-right { display: flex; gap: 8px; }
.toolbar-btn { padding: 6px 14px; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.save-btn { background: #2a2a5a; color: #7c7cff; }
.execute-btn { background: #1a4a1a; color: #4ade80; }
.toolbar-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.editor-layout { display: flex; flex: 1; overflow: hidden; }
.node-palette { width: 160px; background: #15152a; border-right: 1px solid #2a2a4a; padding: 12px; overflow-y: auto; }
.node-palette h3 { font-size: 11px; color: #7c7cff; text-transform: uppercase; margin: 0 0 8px; }
.palette-item { display: flex; align-items: center; gap: 6px; padding: 6px 8px; margin-bottom: 4px; background: #1a1a35; border: 1px solid #2a2a4a; border-radius: 4px; cursor: grab; font-size: 12px; color: #c0c0d0; }
.palette-item:hover { border-color: #7c7cff; }
.node-icon { font-size: 14px; width: 20px; text-align: center; }
.start-icon { color: #4ade80; }
.task-icon { color: #7c7cff; }
.condition-icon { color: #facc15; }
.parallel-icon { color: #f97316; }
.merge-icon { color: #a855f7; }
.end-icon { color: #f87171; }
.agent-list { display: flex; flex-direction: column; gap: 4px; }
.agent-info { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #a0a0b0; }
.agent-dot { width: 6px; height: 6px; border-radius: 50%; }
.dot-green { background: #4ade80; }
.dot-red { background: #f87171; }
.canvas-container { flex: 1; position: relative; overflow: auto; background: #0a0a18; background-image: radial-gradient(circle, #1a1a2e 1px, transparent 1px); background-size: 20px 20px; min-height: 600px; }
.edges-layer { position: absolute; top: 0; left: 0; pointer-events: none; z-index: 1; }
.edges-layer text { pointer-events: auto; cursor: pointer; }
.edge-line { fill: none; stroke: #4a4a6a; stroke-width: 2; pointer-events: stroke; cursor: pointer; }
.edge-line:hover { stroke: #7c7cff; stroke-width: 3; }
.edge-selected { stroke: #7c7cff; stroke-width: 3; }
.canvas-node { position: absolute; width: 160px; min-height: 60px; background: #1a1a35; border: 2px solid #2a2a4a; border-radius: 8px; cursor: move; z-index: 2; user-select: none; }
.canvas-node:hover { border-color: #5a5a8a; }
.node-selected { border-color: #7c7cff !important; box-shadow: 0 0 12px rgba(124,124,255,0.3); }
.node-dragging { opacity: 0.8; }
.node-type-start { border-left: 4px solid #4ade80; }
.node-type-end { border-left: 4px solid #f87171; }
.node-type-task { border-left: 4px solid #7c7cff; }
.node-type-condition { border-left: 4px solid #facc15; }
.node-type-parallel { border-left: 4px solid #f97316; }
.node-type-merge { border-left: 4px solid #a855f7; }
.node-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 8px; background: #15152a; border-radius: 6px 6px 0 0; }
.node-type-badge { font-size: 9px; background: #2a2a4a; color: #9090b0; padding: 1px 6px; border-radius: 4px; }
.node-name { font-size: 11px; color: #e0e0e0; font-weight: 500; }
.node-body { padding: 6px 8px; }
.node-role { font-size: 10px; color: #7c7cff; background: #1e1e3a; padding: 1px 6px; border-radius: 4px; }
.node-condition { font-size: 10px; color: #facc15; }
.node-parallel { font-size: 10px; color: #f97316; }
.handle { position: absolute; width: 12px; height: 12px; background: #2a2a4a; border: 2px solid #4a4a6a; border-radius: 50%; z-index: 3; }
.handle:hover { background: #7c7cff; }
.handle-input { top: -6px; left: 50%; margin-left: -6px; cursor: crosshair; }
.handle-output { bottom: -6px; left: 50%; margin-left: -6px; cursor: crosshair; }
.node-properties { position: fixed; right: 16px; top: 80px; width: 280px; background: #1a1a2e; border: 1px solid #2a2a4a; border-radius: 8px; padding: 12px; z-index: 10; display: flex; flex-direction: column; gap: 8px; }
.node-properties h3 { font-size: 13px; color: #7c7cff; margin: 0; }
.node-properties label { font-size: 11px; color: #a0a0b0; display: flex; flex-direction: column; gap: 2px; }
.node-properties input, .node-properties select, .node-properties textarea { padding: 4px 6px; border: 1px solid #2a2a4a; border-radius: 4px; background: #0f0f1a; color: #e0e0e0; font-size: 11px; }
.delete-btn { background: #3a1a1a; color: #f87171; border: 1px solid #5a2a2a; padding: 6px; border-radius: 4px; cursor: pointer; font-size: 11px; margin-top: 4px; }
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #1a1a2e; border: 1px solid #2a2a4a; border-radius: 12px; width: 600px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #2a2a4a; }
.modal-header h2 { font-size: 16px; color: #7c7cff; margin: 0; }
.modal-close { background: none; border: 1px solid #2a2a4a; color: #a0a0b0; border-radius: 4px; padding: 2px 8px; cursor: pointer; }
.modal-body { padding: 20px; }
.exec-status { padding: 8px; border-radius: 6px; font-weight: 600; font-size: 14px; margin-bottom: 12px; }
.status-ok { background: #1a3a1a; color: #4ade80; }
.status-warn { background: #3a3a1a; color: #facc15; }
.exec-node-card { background: #15152a; border: 1px solid #2a2a4a; border-radius: 6px; margin-bottom: 8px; padding: 8px; }
.exec-node-header { display: flex; gap: 8px; font-size: 12px; align-items: center; }
.exec-node-name { color: #e0e0e0; font-weight: 500; }
.exec-node-type { color: #9090b0; font-size: 10px; }
.exec-node-status { font-weight: 500; }
.text-green { color: #4ade80; }
.text-red { color: #f87171; }
.exec-error { color: #f87171; font-size: 11px; margin-top: 4px; }
.exec-output { font-size: 11px; color: #c0c0d0; background: #0f0f1a; padding: 6px; border-radius: 4px; margin-top: 4px; max-height: 100px; overflow-y: auto; white-space: pre-wrap; }
.exec-context { font-size: 11px; color: #9090b0; background: #0f0f1a; padding: 6px; border-radius: 4px; max-height: 150px; overflow-y: auto; }
h4 { font-size: 13px; color: #7c7cff; margin: 8px 0; }
hr { border: none; border-top: 1px solid #2a2a4a; margin: 8px 0; }
</style>
