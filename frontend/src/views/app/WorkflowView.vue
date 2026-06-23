<template>
  <div class="workflow-page">
    <WorkflowListView />
  </div>
</template>
<script setup>
import WorkflowListView from '../../components/WorkflowListView.vue'
</script>
<style scoped>
.workflow-page { height: 100%; display: flex; flex-direction: column; background: #0d0d1a; }
</style>
