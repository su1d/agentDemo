<template>
  <div class="workflow-page">
    <WorkflowEditor />
  </div>
</template>

<script>
import WorkflowEditor from '../components/WorkflowEditor.vue'

export default {
  name: 'WorkflowView',
  components: { WorkflowEditor },
}
</script>

<style scoped>
.workflow-page { height: 100%; display: flex; flex-direction: column; background: var(--bg-primary); }
</style>
