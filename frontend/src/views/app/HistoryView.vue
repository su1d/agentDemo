<template>
  <div class="history-page">
    <div class="page-header">
      <div>
        <h1>执行历史</h1>
        <p class="subtitle">工作流运行记录和 Agent 对话日志</p>
      </div>
    </div>
    <div class="empty-state glass">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      <p>暂无执行记录</p>
    </div>
  </div>
</template>
<script setup>
</script>
<style scoped>
.history-page { padding: 24px; }

.page-header { margin-bottom: 24px; }

.page-header h1 { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }

.subtitle { font-size: 13px; color: var(--text-muted); }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  border-radius: var(--radius-md);
  color: var(--text-dim);
  font-size: 14px;
}

.empty-state svg { opacity: 0.4; }
</style>
