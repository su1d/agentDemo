<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div>
        <h1>知识库</h1>
        <p class="subtitle">管理 AI Agent 的知识来源</p>
      </div>
      <button class="create-btn glass" @click="showCreate = !showCreate">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建知识库
      </button>
    </div>

    <transition name="fade">
      <div v-if="showCreate" class="create-section glass-strong">
        <input v-model="newName" placeholder="知识库名称" class="create-input" />
        <textarea v-model="newContent" placeholder="输入知识内容..." rows="5" class="create-textarea"></textarea>
        <button class="submit-btn" @click="createKnowledgeBase">保存</button>
      </div>
    </transition>

    <div class="kb-list">
      <div v-for="kb in knowledgeBases" :key="kb.id" class="kb-card glass card-hover">
        <div class="kb-header">
          <h3>{{ kb.name }}</h3>
          <div class="kb-actions">
            <span class="kb-count">{{ kb.segmentCount || 0 }} 片段</span>
            <button class="delete-btn" @click="deleteKB(kb.id)">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>
        <p class="kb-desc">{{ kb.description }}</p>
      </div>
      <div v-if="knowledgeBases.length === 0" class="empty-state">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        <p>暂无知识库</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '../../stores/app'

const store = useAppStore()
const knowledgeBases = ref([])
const showCreate = ref(false)
const newName = ref('')
const newContent = ref('')

onMounted(async () => {
  try { knowledgeBases.value = await store.apiGet('/knowledge') } catch (e) {}
})

async function createKnowledgeBase() {
  if (!newName.value || !newContent.value) return
  try {
    const kb = await store.apiPost('/knowledge', { name: newName.value, content: newContent.value })
    knowledgeBases.value.unshift(kb)
    showCreate.value = false
    newName.value = ''
    newContent.value = ''
  } catch (e) { console.error(e) }
}

async function deleteKB(id) {
  try {
    await store.apiDelete('/knowledge/' + id)
    knowledgeBases.value = knowledgeBases.value.filter(k => k.id !== id)
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.knowledge-page { padding: 24px; }

.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }

.page-header h1 { font-size: 22px; font-weight: 700; color: var(--text-primary); }

.subtitle { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  color: var(--accent);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  border: none;
}

.create-btn:hover {
  background: rgba(108,92,231,0.12);
  border-color: var(--border-active);
}

.create-section {
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.create-input {
  padding: 10px 12px;
  background: rgba(16,16,30,0.5);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: var(--transition);
}

.create-input:focus { border-color: var(--border-active); }

.create-textarea {
  padding: 10px 12px;
  background: rgba(16,16,30,0.5);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: var(--transition);
}

.create-textarea:focus { border-color: var(--border-active); }

.submit-btn {
  padding: 9px 20px;
  background: var(--accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  align-self: flex-end;
  transition: var(--transition);
}

.submit-btn:hover { background: var(--accent-light); }

.kb-list { display: grid; gap: 10px; }

.kb-card {
  border-radius: var(--radius-md);
  padding: 18px;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kb-header h3 { font-size: 15px; font-weight: 600; color: var(--text-primary); }

.kb-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kb-count {
  font-size: 11px;
  color: var(--text-muted);
}

.delete-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xs);
  color: var(--text-muted);
  cursor: pointer;
  transition: var(--transition);
}

.delete-btn:hover {
  background: var(--red-bg);
  border-color: rgba(248,113,113,0.2);
  color: var(--red);
}

.kb-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  color: var(--text-dim);
  font-size: 14px;
}

.empty-state svg { opacity: 0.4; }

.fade-enter-active, .fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; max-height: 0; overflow: hidden; }
</style>
