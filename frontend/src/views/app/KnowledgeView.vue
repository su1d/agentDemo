<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div>
        <h1>知识库</h1>
        <p class="subtitle">管理 AI Agent 的知识来源</p>
      </div>
      <button class="create-btn" @click="showCreate = !showCreate">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建知识库
      </button>
    </div>

    <transition name="fade">
      <div v-if="showCreate" class="create-section">
        <input v-model="newName" placeholder="知识库名称" class="create-input" />
        <textarea v-model="newContent" placeholder="输入知识内容..." rows="6" class="create-textarea"></textarea>
        <button class="submit-btn" @click="createKnowledgeBase">保存</button>
      </div>
    </transition>

    <div class="kb-list">
      <div v-for="kb in knowledgeBases" :key="kb.id" class="kb-card">
        <div class="kb-header">
          <h3>{{ kb.name }}</h3>
          <button class="delete-btn" @click="deleteKB(kb.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
        <p class="kb-desc">{{ kb.description }}</p>
        <div class="kb-meta">片段数: {{ kb.segmentCount || 0 }}</div>
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
.knowledge-page { padding: 24px; max-width: 1000px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #e8e8f0; }
.subtitle { font-size: 13px; color: #68687d; margin-top: 4px; }
.create-btn { display: flex; align-items: center; gap: 6px; padding: 10px 18px; background: #6c5ce7; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.15s; }
.create-btn:hover { background: #8b7cf7; }
.create-section { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; padding: 20px; margin-bottom: 20px; display: flex; flex-direction: column; gap: 12px; }
.create-input { padding: 10px 12px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #e8e8f0; font-size: 14px; outline: none; }
.create-input:focus { border-color: #6c5ce7; }
.create-textarea { padding: 10px 12px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #e8e8f0; font-size: 13px; outline: none; resize: vertical; font-family: inherit; }
.create-textarea:focus { border-color: #6c5ce7; }
.submit-btn { padding: 10px 20px; background: #6c5ce7; color: #fff; border: none; border-radius: 6px; font-size: 14px; font-weight: 600; cursor: pointer; align-self: flex-end; }
.submit-btn:hover { background: #8b7cf7; }
.kb-list { display: grid; gap: 12px; }
.kb-card { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 12px; padding: 18px; }
.kb-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.kb-header h3 { font-size: 16px; font-weight: 600; color: #e8e8f0; }
.delete-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: transparent; border: 1px solid rgba(108,92,231,0.15); border-radius: 6px; color: #68687d; cursor: pointer; transition: all 0.15s; }
.delete-btn:hover { background: rgba(248,113,113,0.1); color: #f87171; }
.kb-desc { font-size: 13px; color: #9898b0; line-height: 1.5; margin-bottom: 8px; }
.kb-meta { font-size: 11px; color: #68687d; }
.fade-enter-active, .fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; max-height: 0; overflow: hidden; }
</style>
