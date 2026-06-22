<template>
  <div class="history-page">
    <WorkflowViewer />
  </div>
</template>

<script>
import WorkflowViewer from '../components/WorkflowViewer.vue'

export default {
  name: 'ExecHistoryView',
  components: { WorkflowViewer },
}
</script>

<style scoped>
.history-page { height: 100%; padding: 24px; overflow-y: auto; background: var(--bg-primary); }
</style>
