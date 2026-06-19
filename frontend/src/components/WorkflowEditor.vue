<template>
  <div class="workflow-editor" @keydown="onKeyDown" tabindex="0">
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
      <div class="toolbar-title">
        <div class="zoom-controls">
          <button class="toolbar-icon-btn" @click="zoomIn" title="放大">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
          </button>
          <span class="zoom-label">{{ Math.round(transform.scale * 100) }}%</span>
          <button class="toolbar-icon-btn" @click="zoomOut" title="缩小">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
          </button>
          <button class="toolbar-icon-btn" @click="fitView" title="适应视图">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="toolbar-hint">
          <kbd>Space</kbd>+拖拽平移 · <kbd>滚轮</kbd>缩放 · <kbd>Del</kbd>删除
        </div>
        <button class="toolbar-btn save-btn" @click="saveWorkflow"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> 保存</button>
        <button class="toolbar-btn execute-btn" @click="executeWorkflow" :disabled="!workflowId"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg> 运行</button>
      </div>
    </div>

    <div class="editor-body">
      <!-- Node Palette -->
      <div class="node-palette">
        <div class="palette-header">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          <span>节点</span>
        </div>
        <div class="palette-items">
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'start')" @dragend="onDragEnd">
            <span class="p-icon start-icon">&#9654;</span>
            <span class="p-label">开始</span>
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'task')" @dragend="onDragEnd">
            <span class="p-icon task-icon">&#9881;</span>
            <span class="p-label">任务</span>
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'condition')" @dragend="onDragEnd">
            <span class="p-icon condition-icon">&#9671;</span>
            <span class="p-label">条件</span>
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'parallel')" @dragend="onDragEnd">
            <span class="p-icon parallel-icon">&#8644;</span>
            <span class="p-label">并行</span>
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'merge')" @dragend="onDragEnd">
            <span class="p-icon merge-icon">&#10753;</span>
            <span class="p-label">合并</span>
          </div>
          <div class="palette-item" draggable="true" @dragstart="onDragStart($event, 'end')" @dragend="onDragEnd">
            <span class="p-icon end-icon">&#9679;</span>
            <span class="p-label">结束</span>
          </div>
        </div>
      </div>

      <!-- Canvas Area -->
      <div class="canvas-wrapper"
        ref="canvasWrapper"
        @wheel.prevent="onWheel"
        @mousedown="onCanvasMouseDown"
        @mousemove="onCanvasMouseMove"
        @mouseup="onCanvasMouseUp"
        @mouseleave="onCanvasMouseUp"
        @contextmenu.prevent="onContextMenu"
        @drop="onDrop"
        @dragover.prevent>
        
        <!-- Transform Group -->
        <div class="canvas-transform" :style="transformStyle">
          <!-- Grid Background -->
          <svg class="grid-layer" width="5000" height="5000" :style="{ left: '-2500px', top: '-2500px' }">
            <defs>
              <pattern id="smallGrid" width="20" height="20" patternUnits="userSpaceOnUse">
                <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(108,92,231,0.06)" stroke-width="0.5"/>
              </pattern>
              <pattern id="grid" width="100" height="100" patternUnits="userSpaceOnUse">
                <rect width="100" height="100" fill="url(#smallGrid)"/>
                <path d="M 100 0 L 0 0 0 100" fill="none" stroke="rgba(108,92,231,0.1)" stroke-width="1"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)"/>
          </svg>

          <!-- Edges SVG -->
          <svg class="edges-layer" width="5000" height="5000">
            <defs>
              <marker id="edgeArrow" markerWidth="12" markerHeight="8" refX="10" refY="4" orient="auto">
                <path d="M0,0 L12,4 L0,8 L3,4 Z" fill="rgba(108,92,231,0.4)"/>
              </marker>
              <marker id="edgeArrowSelected" markerWidth="12" markerHeight="8" refX="10" refY="4" orient="auto">
                <path d="M0,0 L12,4 L0,8 L3,4 Z" fill="#6c5ce7"/>
              </marker>
              <filter id="edgeShadow">
                <feDropShadow dx="0" dy="1" stdDeviation="2" flood-color="rgba(108,92,231,0.15)"/>
              </filter>
            </defs>
            
            <!-- Existing Edges -->
            <g v-for="edge in edges" :key="edge.id || edge._tempId" class="edge-group">
              <path :d="getEdgePath(edge)"
                :class="['edge-line', { 'edge-selected': selectedEdgeId === (edge.id || edge._tempId) }]"
                marker-end="url(#edgeArrow)"
                @click="selectEdge(edge)"
                @dblclick="deleteEdge(edge)" />
              <!-- Edge Label -->
              <foreignObject v-if="edge.label || edge.conditionExpression"
                :x="getEdgeMidpoint(edge).x - 60"
                :y="getEdgeMidpoint(edge).y - 10"
                width="120" height="24"
                class="edge-label-fo">
                <div class="edge-label" @click="selectEdge(edge)">
                  {{ edge.label || edge.conditionExpression }}
                </div>
              </foreignObject>
            </g>
            
            <!-- Drawing Edge Preview -->
            <path v-if="drawingEdge" :d="getTempEdgePath()" class="edge-preview" />
          </svg>

          <!-- Nodes -->
          <div v-for="node in nodes" :key="node.id || node._tempId"
            class="canvas-node"
            :class="['node-type-' + node.nodeType, 
              { 'node-selected': selectedIds.has(node.id || node._tempId),
                'node-dragging': draggingNodeId === (node.id || node._tempId) }]"
            :style="{ left: node.posX + 'px', top: node.posY + 'px' }"
            @mousedown.stop="onNodeMouseDown($event, node)"
            @click.stop="selectNode($event, node)"
            @dblclick.stop="editNode(node)">
            
            <!-- Connection Handles -->
            <div class="handle handle-input" @mousedown.stop="onHandleMouseDown($event, node, 'input')">
              <span class="handle-dot"></span>
            </div>
            <div class="handle handle-output" @mousedown.stop="onHandleMouseDown($event, node, 'output')">
              <span class="handle-dot"></span>
            </div>

            <div class="node-header">
              <span class="node-type-badge">{{ nodeTypeLabel(node.nodeType) }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
            <div class="node-body" v-if="node.agentRole || node.conditionExpression || node.parallelRoles">
              <span v-if="node.agentRole" class="node-tag node-tag-agent">{{ node.agentRole }}</span>
              <span v-if="node.conditionExpression" class="node-tag node-tag-condition">{{ node.conditionExpression }}</span>
              <span v-if="node.parallelRoles" class="node-tag node-tag-parallel">{{ node.parallelRoles }}</span>
            </div>
          </div>

          <!-- Selection Rubber Band -->
          <div v-if="selectionBox" class="selection-box"
            :style="{
              left: Math.min(selectionBox.x1, selectionBox.x2) + 'px',
              top: Math.min(selectionBox.y1, selectionBox.y2) + 'px',
              width: Math.abs(selectionBox.x2 - selectionBox.x1) + 'px',
              height: Math.abs(selectionBox.y2 - selectionBox.y1) + 'px'
            }"></div>

          <!-- Drop Indicator -->
          <div v-if="dropIndicator" class="drop-indicator" :style="{ left: dropIndicator.x + 'px', top: dropIndicator.y + 'px' }">
            <div class="drop-indicator-inner">+</div>
          </div>
        </div>
      </div>

      <!-- Context Menu -->
      <transition name="fade">
        <div v-if="contextMenu" class="context-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
          <div class="context-item" @click="addNodeFromMenu('start')">
            <span class="p-icon start-icon">&#9654;</span> 开始节点
          </div>
          <div class="context-item" @click="addNodeFromMenu('task')">
            <span class="p-icon task-icon">&#9881;</span> 任务节点
          </div>
          <div class="context-item" @click="addNodeFromMenu('condition')">
            <span class="p-icon condition-icon">&#9671;</span> 条件节点
          </div>
          <div class="context-divider" v-if="selectedIds.size > 0"></div>
          <div class="context-item context-item-danger" @click="deleteSelected" v-if="selectedIds.size > 0">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg> 删除选中
          </div>
        </div>
      </transition>

      <!-- Properties Panel -->
      <transition name="slide-right">
        <div v-if="selectedNode" class="node-properties">
          <div class="prop-header">
            <h3><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg> 属性</h3>
            <button class="prop-close" @click="selectedNodeId = null"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="prop-body">
            <div class="prop-field">
              <label>名称</label>
              <input v-model="selectedNode.name" @input="emitChange" placeholder="节点名称" />
            </div>
            <div class="prop-field" v-if="['task', 'start', 'end'].includes(selectedNode.nodeType)">
              <label>Agent 角色</label>
              <select v-model="selectedNode.agentRole" @change="emitChange">
                <option value="">无</option>
                <option v-for="a in agents" :key="a.role" :value="a.role">{{ a.role }}</option>
              </select>
            </div>
            <div class="prop-field" v-if="selectedNode.nodeType === 'condition'">
              <label>条件表达式</label>
              <textarea v-model="selectedNode.conditionExpression" @input="emitChange" rows="2" placeholder="如: score > 0.5"></textarea>
            </div>
            <div class="prop-field" v-if="selectedNode.nodeType === 'parallel'">
              <label>并行角色</label>
              <input v-model="selectedNode.parallelRoles" @input="emitChange" placeholder="role1, role2" />
            </div>
            <div class="prop-field" v-if="selectedNode.nodeType !== 'start' && selectedNode.nodeType !== 'end'">
              <label>提示词</label>
              <textarea v-model="selectedNode.prompt" @input="emitChange" rows="3" placeholder="节点提示词..."></textarea>
            </div>
          </div>
          <div class="prop-footer">
            <button class="delete-btn" @click="deleteSelected">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg> 删除节点
            </button>
          </div>
        </div>
      </transition>
    </div>
    
    <!-- Execution Result Modal -->
    <transition name="modal">
      <div v-if="execResult" class="modal-overlay" @click.self="execResult = null">
        <div class="modal">
          <div class="modal-header">
            <h2><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg> 执行结果</h2>
            <button class="modal-close" @click="execResult = null"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="modal-body">
            <div v-if="execResult.status" class="exec-status" :class="'status-' + execResult.status">
              {{ execResult.status === 'success' ? '✅ 执行成功' : '⚠️ 执行完成（有警告）' }}
            </div>
            <div v-for="nr in (execResult.nodeResults || [])" :key="nr.nodeId" class="exec-node-card">
              <div class="exec-node-header">
                <svg v-if="nr.status === 'success'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4ade80" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f87171" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                <span class="exec-node-name">{{ nr.nodeName }}</span>
                <span class="exec-node-type">{{ nr.nodeType }}</span>
              </div>
              <div v-if="nr.error" class="exec-error">{{ nr.error }}</div>
              <div v-if="nr.output" class="exec-output">{{ formatOutput(nr.output) }}</div>
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
  name: 'WorkflowEditor',
  data() {
    return {
      workflowId: null,
      workflowName: '',
      newWorkflowName: '',
      workflowStatus: 'draft',
      nodes: [],
      edges: [],
      selectedNodeId: null,
      selectedEdgeId: null,
      selectedIds: new Set(),
      draggingNodeId: null,
      dragOffset: { x: 0, y: 0 },
      drawingEdge: null,
      isPanning: false,
      isSelecting: false,
      spacePressed: false,
      panStart: { x: 0, y: 0 },
      canvasScrollStart: { x: 0, y: 0 },
      selectionBox: null,
      contextMenu: null,
      dropIndicator: null,
      transform: { x: 0, y: 0, scale: 1 },
      execResult: null,
      agents: [
        { role: 'requirement_agent', ready: true },
        { role: 'data_collect_agent', ready: true },
        { role: 'data_analyze_agent', ready: true },
        { role: 'visualization_agent', ready: false },
        { role: 'report_agent', ready: false },
      ],
      nodeTypeLabels: {
        start: '开始', task: '任务', condition: '条件',
        parallel: '并行', merge: '合并', end: '结束',
      },
    }
  },
  computed: {
    transformStyle() {
      return {
        transform: `translate(${this.transform.x}px, ${this.transform.y}px) scale(${this.transform.scale})`,
        transformOrigin: '0 0',
      }
    },
    selectedNode() {
      return this.nodes.find(n => (n.id || n._tempId) === this.selectedNodeId) || null
    },
  },
  mounted() {
    const wfId = this.$route?.query?.id
    if (wfId) this.loadWorkflow(wfId)
    document.addEventListener('keydown', this.onGlobalKeyDown)
    document.addEventListener('keyup', this.onGlobalKeyUp)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.onGlobalKeyDown)
    document.removeEventListener('keyup', this.onGlobalKeyUp)
  },
  methods: {
    nodeTypeLabel(t) { return this.nodeTypeLabels[t] || t },

    // ── Keyboard ──
    onGlobalKeyDown(e) {
      if (e.code === 'Space') { this.spacePressed = true; e.preventDefault() }
    },
    onGlobalKeyUp(e) {
      if (e.code === 'Space') { this.spacePressed = false }
    },
    onKeyDown(e) {
      if (e.key === 'Delete' || e.key === 'Backspace') {
        if (this.selectedNode) this.deleteSelected()
      }
      if (e.ctrlKey && e.key === 'a') {
        e.preventDefault()
        this.selectedIds = new Set(this.nodes.map(n => n.id || n._tempId))
        this.emitChange()
      }
    },

    // ── Zoom / Pan ──
    zoomIn() {
      this.transform.scale = Math.min(2, this.transform.scale + 0.1)
    },
    zoomOut() {
      this.transform.scale = Math.max(0.3, this.transform.scale - 0.1)
    },
    fitView() {
      if (this.nodes.length === 0) return
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
      for (const n of this.nodes) {
        if (n.posX < minX) minX = n.posX
        if (n.posY < minY) minY = n.posY
        if (n.posX + 160 > maxX) maxX = n.posX + 160
        if (n.posY + 60 > maxY) maxY = n.posY + 60
      }
      const wrapper = this.$refs.canvasWrapper
      const pad = 80
      const availW = wrapper.clientWidth - pad * 2
      const availH = wrapper.clientHeight - pad * 2
      const nodesW = maxX - minX
      const nodesH = maxY - minY
      const scale = Math.min(availW / (nodesW || 1), availH / (nodesH || 1), 1.5)
      this.transform.scale = Math.max(0.3, Math.min(2, scale))
      this.transform.x = pad - minX * this.transform.scale + (availW - nodesW * this.transform.scale) / 2
      this.transform.y = pad - minY * this.transform.scale + (availH - nodesH * this.transform.scale) / 2
    },
    onWheel(e) {
      const delta = e.deltaY > 0 ? -0.05 : 0.05
      const newScale = Math.max(0.3, Math.min(2, this.transform.scale + delta))
      const wrapper = this.$refs.canvasWrapper
      const rect = wrapper.getBoundingClientRect()
      const mx = e.clientX - rect.left
      const my = e.clientY - rect.top
      this.transform.x = mx - (mx - this.transform.x) * (newScale / this.transform.scale)
      this.transform.y = my - (my - this.transform.y) * (newScale / this.transform.scale)
      this.transform.scale = newScale
    },

    // ── Canvas Mouse ──
    onCanvasMouseDown(e) {
      if (e.button === 1 || this.spacePressed) {
        this.isPanning = true
        this.panStart = { x: e.clientX, y: e.clientY }
        return
      }
      // Check if we hit a node
      const target = e.target.closest('.canvas-node')
      if (target) return
      // Selection box
      this.isSelecting = true
      const pos = this.screenToCanvas(e.clientX, e.clientY)
      this.selectionBox = { x1: pos.x, y1: pos.y, x2: pos.x, y2: pos.y }
    },
    onCanvasMouseMove(e) {
      if (this.isPanning) {
        this.transform.x += (e.clientX - this.panStart.x)
        this.transform.y += (e.clientY - this.panStart.y)
        this.panStart = { x: e.clientX, y: e.clientY }
        return
      }
      if (this.isSelecting && this.selectionBox) {
        const pos = this.screenToCanvas(e.clientX, e.clientY)
        this.selectionBox.x2 = pos.x
        this.selectionBox.y2 = pos.y
      }
      if (this.draggingNodeId) {
        const node = this.nodes.find(n => (n.id || n._tempId) === this.draggingNodeId)
        if (node) {
          const newPos = this.screenToCanvas(e.clientX - this.dragOffset.x, e.clientY - this.dragOffset.y)
          // Snap to grid
          const snap = 10
          node.posX = Math.round(newPos.x / snap) * snap
          node.posY = Math.round(newPos.y / snap) * snap
          this.emitChange()
        }
      }
      if (this.drawingEdge) {
        const pos = this.screenToCanvas(e.clientX, e.clientY)
        this.drawingEdge.mouseX = pos.x
        this.drawingEdge.mouseY = pos.y
        this.emitChange()
      }
    },
    onCanvasMouseUp(e) {
      if (this.isPanning) { this.isPanning = false; return }
      if (this.isSelecting && this.selectionBox) {
        this.finishSelection()
        return
      }
      if (this.draggingNodeId) { this.draggingNodeId = null; this.emitChange(); return }
      if (this.drawingEdge) { this.finishEdgeDraw(e) }
      this.deselectAll()
    },

    // ── Selection ──
    finishSelection() {
      const s = this.selectionBox
      if (!s) return
      const x1 = Math.min(s.x1, s.x2), y1 = Math.min(s.y1, s.y2)
      const x2 = Math.max(s.x1, s.x2), y2 = Math.max(s.y1, s.y2)
      // Only consider if selection is big enough
      if (Math.abs(x2 - x1) < 5 && Math.abs(y2 - y1) < 5) {
        this.selectionBox = null
        this.isSelecting = false
        return
      }
      this.selectedIds = new Set()
      for (const n of this.nodes) {
        if (n.posX >= x1 && n.posX + 160 <= x2 && n.posY >= y1 && n.posY + 60 <= y2) {
          this.selectedIds.add(n.id || n._tempId)
        }
      }
      this.selectionBox = null
      this.isSelecting = false
      this.emitChange()
    },

    // ── Node Events ──
    onDragStart(e, type) {
      e.dataTransfer.setData('nodeType', type)
      e.dataTransfer.effectAllowed = 'copy'
    },
    onDragEnd() {
      this.dropIndicator = null
    },
    onDrop(e) {
      const type = e.dataTransfer.getData('nodeType')
      if (!type) return
      const pos = this.screenToCanvas(e.clientX, e.clientY)
      const snap = 10
      this.nodes.push({
        _tempId: 'n_' + Date.now(),
        id: null,
        nodeType: type,
        name: this.nodeTypeLabel(type) + '_' + (this.nodes.filter(n => n.nodeType === type).length + 1),
        posX: Math.round((pos.x - 80) / snap) * snap,
        posY: Math.round((pos.y - 30) / snap) * snap,
        agentRole: '',
        conditionExpression: '',
        parallelRoles: '',
        prompt: '',
      })
      this.dropIndicator = null
      this.emitChange()
    },
    addNodeFromMenu(type) {
      const pos = this.contextMenu ? this.screenToCanvas(this.contextMenu.x, this.contextMenu.y) : { x: 100, y: 100 }
      this.nodes.push({
        _tempId: 'n_' + Date.now(),
        id: null,
        nodeType: type,
        name: this.nodeTypeLabel(type) + '_' + (this.nodes.filter(n => n.nodeType === type).length + 1),
        posX: pos.x - 80,
        posY: pos.y - 30,
        agentRole: '',
        conditionExpression: '',
        parallelRoles: '',
        prompt: '',
      })
      this.contextMenu = null
      this.emitChange()
    },
    onNodeMouseDown(e, node) {
      if (this.spacePressed) return
      const id = node.id || node._tempId
      this.draggingNodeId = id
      const pos = this.screenToCanvas(e.clientX, e.clientY)
      this.dragOffset = { x: pos.x - node.posX, y: pos.y - node.posY }
      if (!e.shiftKey && !this.selectedIds.has(id)) {
        this.selectedIds = new Set([id])
        this.selectedNodeId = id
      } else if (e.shiftKey) {
        if (this.selectedIds.has(id)) this.selectedIds.delete(id)
        else this.selectedIds.add(id)
      }
    },
    selectNode(e, node) {
      if (this.spacePressed) return
      const id = node.id || node._tempId
      if (e?.shiftKey) {
        if (this.selectedIds.has(id)) this.selectedIds.delete(id)
        else this.selectedIds.add(id)
      } else {
        this.selectedIds = new Set([id])
      }
      this.selectedNodeId = id
      this.selectedEdgeId = null
    },
    selectEdge(edge) {
      this.selectedEdgeId = edge.id || edge._tempId
      this.selectedNodeId = null
    },
    deselectAll() {
      this.selectedIds = new Set()
      this.selectedNodeId = null
      this.selectedEdgeId = null
      this.contextMenu = null
    },
    editNode(node) {
      this.selectedNodeId = node.id || node._tempId
    },
    deleteSelected() {
      for (const id of this.selectedIds) {
        this.nodes = this.nodes.filter(n => (n.id || n._tempId) !== id)
        this.edges = this.edges.filter(e => e.sourceId !== id && e.targetId !== id)
      }
      this.selectedIds = new Set()
      this.selectedNodeId = null
      this.emitChange()
    },
    deleteEdge(edge) {
      const id = edge.id || edge._tempId
      this.edges = this.edges.filter(e => (e.id || e._tempId) !== id)
      this.selectedEdgeId = null
      this.emitChange()
    },

    // ── Context Menu ──
    onContextMenu(e) {
      this.contextMenu = { x: e.clientX, y: e.clientY }
    },

    // ── Handle Edge Drawing ──
    onHandleMouseDown(e, node, portType) {
      e.preventDefault(); e.stopPropagation()
      if (portType === 'output') {
        this.drawingEdge = {
          sourceId: node.id || node._tempId,
          sourcePos: { x: node.posX + 80, y: node.posY + 60 },
          mouseX: node.posX + 80,
          mouseY: node.posY + 100,
        }
      } else if (portType === 'input' && this.drawingEdge) {
        this.finishEdgeDraw(e, node)
      }
    },
    finishEdgeDraw(e, targetNode) {
      if (!targetNode) {
        const pos = this.screenToCanvas(e.clientX, e.clientY)
        for (const n of this.nodes) {
          const id = n.id || n._tempId
          if (pos.x >= n.posX && pos.x <= n.posX + 160 && pos.y >= n.posY && pos.y <= n.posY + 60 && id !== this.drawingEdge?.sourceId) {
            targetNode = n; break
          }
        }
      }
      if (targetNode) {
        const targetId = targetNode.id || targetNode._tempId
        const sourceId = this.drawingEdge.sourceId
        if (sourceId !== targetId && !this.edges.some(e => e.sourceId === sourceId && e.targetId === targetId)) {
          this.edges.push({ _tempId: 'e_' + Date.now(), id: null, sourceId, targetId, label: '', conditionExpression: '' })
        }
      }
      this.drawingEdge = null
      this.emitChange()
    },

    // ── Coordinate Helpers ──
    screenToCanvas(clientX, clientY) {
      const wrapper = this.$refs.canvasWrapper
      const rect = wrapper.getBoundingClientRect()
      return {
        x: (clientX - rect.left - this.transform.x) / this.transform.scale,
        y: (clientY - rect.top - this.transform.y) / this.transform.scale,
      }
    },

    // ── Edge Paths ──
    getEdgePath(edge) {
      const src = this.nodes.find(n => (n.id || n._tempId) === edge.sourceId)
      const tgt = this.nodes.find(n => (n.id || n._tempId) === edge.targetId)
      if (!src || !tgt) return ''
      const sx = src.posX + 80, sy = src.posY + 60
      const tx = tgt.posX + 80, ty = tgt.posY
      const cy = (sy + ty) / 2
      const dx = Math.abs(tx - sx)
      const offset = Math.max(40, dx * 0.4)
      return `M${sx},${sy} C${sx},${sy + offset} ${tx},${ty - offset} ${tx},${ty}`
    },
    getTempEdgePath() {
      if (!this.drawingEdge) return ''
      const s = this.drawingEdge.sourcePos
      const mx = this.drawingEdge.mouseX
      const my = this.drawingEdge.mouseY
      const cy = (s.y + my) / 2
      const dy = Math.abs(my - s.y)
      const offset = Math.max(40, dy * 0.4)
      return `M${s.x},${s.y} C${s.x},${s.y + offset} ${mx},${my - offset} ${mx},${my}`
    },
    getEdgeMidpoint(edge) {
      const src = this.nodes.find(n => (n.id || n._tempId) === edge.sourceId)
      const tgt = this.nodes.find(n => (n.id || n._tempId) === edge.targetId)
      if (!src || !tgt) return { x: 0, y: 0 }
      return { x: (src.posX + tgt.posX) / 2 + 80, y: (src.posY + tgt.posY) / 2 + 30 }
    },

    // ── Persistence ──
    async saveWorkflow() {
      const token = localStorage.getItem('token')
      const headers = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token }
      const body = JSON.stringify({
        name: this.workflowName || this.newWorkflowName || '未命名工作流',
        status: this.workflowStatus,
        nodes: this.nodes.map(n => ({ nodeType: n.nodeType, name: n.name, posX: n.posX, posY: n.posY, agentRole: n.agentRole, conditionExpression: n.conditionExpression, parallelRoles: n.parallelRoles, prompt: n.prompt })),
        edges: this.edges.map(e => ({ sourceId: e.sourceId, targetId: e.targetId, label: e.label, conditionExpression: e.conditionExpression })),
      })
      try {
        const method = this.workflowId ? 'PUT' : 'POST'
        const url = this.workflowId ? `${API_BASE}/workflow/${this.workflowId}` : `${API_BASE}/workflow`
        const res = await fetch(url, { method, headers, body })
        const data = await res.json()
        this.workflowId = data.id
        if (data.name) this.workflowName = data.name
      } catch (e) { console.error(e) }
    },
    async loadWorkflow(id) {
      try {
        const res = await fetch(`${API_BASE}/workflow/${id}`)
        const data = await res.json()
        this.workflowId = data.id; this.workflowName = data.name; this.workflowStatus = data.status || 'draft'
        this.nodes = data.nodes || []; this.edges = data.edges || []
      } catch (e) { console.error(e) }
    },
    updateWorkflowStatus() {},
    async executeWorkflow() {
      if (!this.workflowId) return
      const token = localStorage.getItem('token')
      try {
        const res = await fetch(`${API_BASE}/workflow/${this.workflowId}/execute`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
        this.execResult = await res.json()
      } catch (e) { console.error(e) }
    },
    formatOutput(out) { return typeof out === 'string' ? out : JSON.stringify(out, null, 2) },
    emitChange() { this.$forceUpdate() },
  },
}
</script>

<style scoped>
/* ═══ Layout ═══ */
.workflow-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  outline: none;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  gap: 12px;
}

.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-left h2 { font-size: 15px; font-weight: 700; color: var(--text-primary); }

.title-input {
  padding: 5px 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 13px;
  width: 160px;
  outline: none;
  transition: var(--transition);
}
.title-input:focus { border-color: var(--accent); box-shadow: 0 0 0 2px var(--accent-glow); }

.status-select {
  padding: 5px 8px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 11px;
  outline: none;
  cursor: pointer;
}

.toolbar-title { flex: 1; display: flex; justify-content: center; }

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--bg-secondary);
  padding: 3px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.zoom-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  min-width: 36px;
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
}

.toolbar-icon-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
}
.toolbar-icon-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }

.toolbar-right { display: flex; align-items: center; gap: 8px; }

.toolbar-hint {
  font-size: 10px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0.6;
}
.toolbar-hint kbd {
  font-family: 'JetBrains Mono', monospace;
  font-size: 9px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: 3px;
  padding: 1px 4px;
  color: var(--text-secondary);
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}
.save-btn { background: var(--accent); color: #fff; }
.save-btn:hover { background: var(--accent-light); }
.execute-btn { background: linear-gradient(135deg, #10b981, #34d399); color: #fff; }
.execute-btn:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(16,185,129,0.3); }
.execute-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ═══ Body ═══ */
.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* ═══ Node Palette ═══ */
.node-palette {
  width: 52px;
  flex-shrink: 0;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  overflow-y: auto;
}

.palette-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 9px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  opacity: 0.5;
}
.palette-header svg { width: 10px; height: 10px; }

.palette-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 4px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 8px;
  color: var(--text-muted);
  cursor: grab;
  transition: var(--transition);
  width: 38px;
}
.palette-item:hover {
  border-color: var(--accent);
  background: rgba(108,92,231,0.06);
  color: var(--accent);
}

.p-icon { font-size: 14px; }
.p-label { font-size: 7px; font-weight: 500; line-height: 1; }
.start-icon { color: var(--green); }
.task-icon { color: var(--accent); }
.condition-icon { color: var(--yellow); }
.parallel-icon { color: var(--orange); }
.merge-icon { color: #a855f7; }
.end-icon { color: var(--red); }

/* ═══ Canvas ═══ */
.canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
  cursor: grab;
}
.canvas-wrapper:active { cursor: grabbing; }

.canvas-transform {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
}

/* Grid */
.grid-layer { position: absolute; pointer-events: none; }

/* Edges */
.edges-layer {
  position: absolute;
  top: -2500px;
  left: -2500px;
  pointer-events: none;
  z-index: 1;
}
.edge-group { pointer-events: auto; }

.edge-line {
  fill: none;
  stroke: rgba(108,92,231,0.25);
  stroke-width: 2;
  cursor: pointer;
  transition: stroke 0.15s, stroke-width 0.15s;
}
.edge-line:hover { stroke: var(--accent); stroke-width: 2.5; }
.edge-selected { stroke: var(--accent) !important; stroke-width: 2.5 !important; filter: url(#edgeShadow); }

.edge-preview {
  fill: none;
  stroke: var(--accent);
  stroke-width: 2;
  stroke-dasharray: 6 3;
  opacity: 0.7;
  pointer-events: none;
}

.edge-label-fo { pointer-events: auto; overflow: visible; }
.edge-label {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 10px;
  color: var(--text-muted);
  text-align: center;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.edge-label:hover { border-color: var(--accent); color: var(--accent); }

/* ═══ Nodes ═══ */
.canvas-node {
  position: absolute;
  width: 160px;
  min-height: 50px;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  border-radius: 10px;
  cursor: move;
  z-index: 2;
  user-select: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.canvas-node:hover { border-color: rgba(108,92,231,0.35); z-index: 3; }
.node-selected {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 2px var(--accent-glow), 0 4px 16px rgba(108,92,231,0.15) !important;
  z-index: 4 !important;
}
.node-dragging { opacity: 0.85; z-index: 10 !important; }

.node-type-start { border-left: 3px solid var(--green); }
.node-type-end { border-left: 3px solid var(--red); }
.node-type-task { border-left: 3px solid var(--accent); }
.node-type-condition { border-left: 3px solid var(--yellow); }
.node-type-parallel { border-left: 3px solid var(--orange); }
.node-type-merge { border-left: 3px solid #a855f7; }

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: var(--bg-secondary);
  border-radius: 8px 8px 0 0;
}

.node-type-badge {
  font-size: 9px;
  background: var(--bg-elevated);
  color: var(--text-muted);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.node-name {
  font-size: 11px;
  color: var(--text-primary);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.node-body {
  padding: 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.node-tag {
  font-size: 9px;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: 'JetBrains Mono', monospace;
}
.node-tag-agent { color: var(--accent); background: rgba(108,92,231,0.08); }
.node-tag-condition { color: var(--yellow); background: var(--yellow-bg); }
.node-tag-parallel { color: var(--orange); background: rgba(251,146,60,0.1); }

/* ═══ Handles ═══ */
.handle {
  position: absolute;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
  cursor: crosshair;
  opacity: 0;
  transition: opacity 0.15s;
}
.canvas-node:hover .handle { opacity: 1; }

.handle-dot {
  width: 8px;
  height: 8px;
  background: var(--accent);
  border: 2px solid var(--bg-card);
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(108,92,231,0.3);
  transition: transform 0.15s;
}
.handle:hover .handle-dot { transform: scale(1.4); }

.handle-input { top: -8px; left: 50%; margin-left: -8px; }
.handle-output { bottom: -8px; left: 50%; margin-left: -8px; }

/* ═══ Selection Box ═══ */
.selection-box {
  position: absolute;
  border: 1px solid var(--accent);
  background: var(--accent-glow);
  z-index: 20;
  pointer-events: none;
}

/* ═══ Drop Indicator ═══ */
.drop-indicator {
  position: absolute;
  width: 32px;
  height: 32px;
  z-index: 20;
  pointer-events: none;
  transform: translate(-50%, -50%);
}
.drop-indicator-inner {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  box-shadow: 0 0 20px rgba(108,92,231,0.4);
}

/* ═══ Context Menu ═══ */
.context-menu {
  position: fixed;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 4px;
  min-width: 150px;
  z-index: 100;
  box-shadow: var(--shadow-elevated);
  backdrop-filter: blur(20px);
}

.context-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
}
.context-item:hover { background: rgba(108,92,231,0.08); color: var(--text-primary); }
.context-item-danger:hover { background: var(--red-bg); color: var(--red); }
.context-divider { height: 1px; background: var(--border-color); margin: 4px 0; }

/* ═══ Properties Panel ═══ */
.node-properties {
  position: fixed;
  right: 16px;
  top: 64px;
  width: 280px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}

.prop-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}
.prop-header h3 {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.prop-header h3 svg { color: var(--accent); }

.prop-close {
  width: 28px;
  height: 28px;
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
.prop-close:hover { background: var(--bg-secondary); color: var(--text-primary); }

.prop-body { padding: 12px 16px; display: flex; flex-direction: column; gap: 12px; }
.prop-field { display: flex; flex-direction: column; gap: 4px; }
.prop-field label { font-size: 11px; font-weight: 500; color: var(--text-muted); }

.prop-field input,
.prop-field select,
.prop-field textarea {
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 12px;
  outline: none;
  transition: var(--transition);
}
.prop-field input:focus,
.prop-field select:focus,
.prop-field textarea:focus { border-color: var(--accent); }
.prop-field textarea { resize: vertical; min-height: 44px; font-family: 'JetBrains Mono', monospace; font-size: 11px; }

.prop-footer { padding: 10px 16px; border-top: 1px solid var(--border-color); }

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  width: 100%;
  background: var(--red-bg);
  color: var(--red);
  border: 1px solid rgba(248,113,113,0.15);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: var(--transition);
}
.delete-btn:hover { background: rgba(248,113,113,0.2); }

/* ═══ Transitions ═══ */
.slide-right-enter-active, .slide-right-leave-active { transition: all 0.2s ease; }
.slide-right-enter-from, .slide-right-leave-to { opacity: 0; transform: translateX(20px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform 0.2s ease; }
.modal-enter-from .modal { transform: scale(0.95) translateY(10px); }
.modal-leave-to .modal { transform: scale(0.95) translateY(10px); }

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
.modal { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 14px; width: 640px; max-height: 80vh; overflow-y: auto; box-shadow: var(--shadow-elevated); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; border-bottom: 1px solid var(--border-color); }
.modal-header h2 { font-size: 16px; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }
.modal-header h2 svg { color: var(--green); }
.modal-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border-color); border-radius: 6px; background: transparent; color: var(--text-muted); cursor: pointer; transition: var(--transition); }
.modal-close:hover { background: var(--bg-secondary); color: var(--text-primary); }
.modal-body { padding: 20px 24px; }

.exec-status { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 8px; font-weight: 600; font-size: 14px; margin-bottom: 16px; }
.status-ok { background: rgba(74,222,128,0.08); color: var(--green); }
.status-success { background: rgba(74,222,128,0.08); color: var(--green); border: 1px solid rgba(74,222,128,0.15); }
.status-warn { background: var(--yellow-bg); color: var(--yellow); border: 1px solid rgba(250,204,21,0.15); }

.exec-node-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; margin-bottom: 10px; overflow: hidden; }
.exec-node-header { display: flex; gap: 8px; padding: 8px 12px; align-items: center; background: rgba(108,92,231,0.04); font-size: 13px; }
.exec-node-name { color: var(--text-primary); font-weight: 600; }
.exec-node-type { color: var(--text-muted); font-size: 11px; }
.exec-error { padding: 8px 12px; color: var(--red); font-size: 12px; background: var(--red-bg); }
.exec-output { padding: 10px 12px; font-size: 12px; color: var(--text-secondary); background: var(--bg-primary); max-height: 150px; overflow-y: auto; white-space: pre-wrap; font-family: 'JetBrains Mono', monospace; }
</style>
