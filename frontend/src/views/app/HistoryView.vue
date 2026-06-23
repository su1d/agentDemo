<template>
  <div class="history-page">
    <div class="page-header">
      <h1>执行历史</h1>
      <p class="subtitle">工作流运行记录和 Agent 对话日志</p>
    </div>
    <div class="empty-state">
      <p>暂无执行记录</p>
    </div>
  </div>
</template>
<script setup>
</script>
<style scoped>
.history-page { padding: 24px; max-width: 1000px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #e8e8f0; margin-bottom: 4px; }
.subtitle { font-size: 13px; color: #68687d; margin-bottom: 24px; }
.empty-state { text-align: center; padding: 80px 20px; color: #68687d; font-size: 14px; }
</style>
