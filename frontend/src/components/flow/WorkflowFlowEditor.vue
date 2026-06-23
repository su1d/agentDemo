<template>
  <div class="vue-flow-wrapper">
    <VueFlow v-model:nodes="flowNodes" v-model:edges="flowEdges" :default-viewport="{ x: 200, y: 200, zoom: 1 }" fit-view-on-init>
      <Background :gap="20" size="1" />
      <Controls position="bottom-right" />
      <MiniMap position="bottom-left" />

      <template #node-start="props">
        <div class="custom-node node-start"><div class="node-badge">开始</div><div class="node-label">{{ props.data.label }}</div></div>
      </template>
      <template #node-task="props">
        <div class="custom-node node-task"><div class="node-badge">任务</div><div class="node-label">{{ props.data.label }}</div><div v-if="props.data.agentRole" class="node-tag">{{ props.data.agentRole }}</div></div>
      </template>
      <template #node-condition="props">
        <div class="custom-node node-condition"><div class="node-badge">条件</div><div class="node-label">{{ props.data.label }}</div></div>
      </template>
      <template #node-parallel="props">
        <div class="custom-node node-parallel"><div class="node-badge">并行</div><div class="node-label">{{ props.data.label }}</div></div>
      </template>
      <template #node-end="props">
        <div class="custom-node node-end"><div class="node-badge">结束</div><div class="node-label">{{ props.data.label }}</div></div>
      </template>
      <template #node-merge="props">
        <div class="custom-node node-merge"><div class="node-badge">合并</div><div class="node-label">{{ props.data.label }}</div></div>
      </template>
    </VueFlow>
  </div>
</template>
<script setup>
import { ref, watch, markRaw } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:nodes', 'update:edges', 'node-select'])

const flowNodes = ref(props.nodes.map(n => ({ ...n, type: n.type || 'task' })))
const flowEdges = ref(props.edges)

watch(() => props.nodes, (val) => { flowNodes.value = val.map(n => ({ ...n, type: n.type || 'task' })) }, { deep: true })
watch(() => props.edges, (val) => { flowEdges.value = val }, { deep: true })

function onNodeClick({ node }) { emit('node-select', node) }
function onPaneClick() { emit('node-select', null) }
function onConnect(connection) {
  const newEdge = { id: 'e_' + Date.now(), source: connection.source, target: connection.target, label: '' }
  flowEdges.value = [...flowEdges.value, newEdge]
  emit('update:edges', flowEdges.value)
}
</script>
<style scoped>
.vue-flow-wrapper { width: 100%; height: 100%; }
.custom-node { padding: 10px 14px; border-radius: 8px; min-width: 140px; background: #1a1a30; border: 2px solid rgba(108,92,231,0.2); color: #e8e8f0; font-size: 12px; }
.node-start { border-color: #4ade80; }
.node-task { border-color: #6c5ce7; }
.node-condition { border-color: #facc15; }
.node-parallel { border-color: #fb923c; }
.node-merge { border-color: #a855f7; }
.node-end { border-color: #f87171; }
.node-badge { font-size: 9px; font-weight: 600; text-transform: uppercase; opacity: 0.7; margin-bottom: 4px; }
.node-label { font-size: 13px; font-weight: 600; }
.node-tag { font-size: 10px; margin-top: 4px; padding: 2px 6px; border-radius: 3px; background: rgba(108,92,231,0.1); color: #8b7cf7; display: inline-block; }
</style>