<template>
  <div class="chat-layout">
    <!-- Left Sidebar: Agent Status & Mode Selection -->
    <aside class="chat-sidebar">
      <div class="sidebar-section">
        <div class="section-title">Agent 状态</div>
        <div class="agent-list">
          <div v-for="a in agents" :key="a.role" class="agent-item">
            <span class="dot" :class="a.ready ? 'dot-on' : 'dot-off'"></span>
            <span class="agent-name">{{ a.role }}</span>
            <span class="agent-status" :class="a.ready ? 'text-green' : 'text-red'">{{ a.ready ? '在线' : '离线' }}</span>
          </div>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-title">协作模式</div>
        <div class="mode-grid">
          <button v-for="m in modes" :key="m.value" @click="mode = m.value" :class="['mode-btn', mode === m.value ? 'active' : '']">
            <span class="mode-label">{{ m.label }}</span>
          </button>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-title">后端状态</div>
        <div class="status-row">
          <span class="dot" :class="backendOnline ? 'dot-on' : 'dot-off'"></span>
          <span>{{ backendOnline ? '在线' : '离线' }}</span>
        </div>
      </div>
    </aside>

    <!-- Main Chat Area -->
    <main class="chat-main">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <h2>智能对话</h2>
          <p>选择协作模式，输入你的问题，AI Agent 团队将为你协作完成</p>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
          <div class="bubble">
            <div class="sender" v-if="msg.role === 'assistant'">
              <span class="sender-name">{{ msg.agentRole || 'System' }}</span>
              <span v-if="msg.collaborationMode" class="mode-tag">{{ msg.collaborationMode }}</span>
            </div>
            <div class="text">{{ msg.text }}</div>
          </div>
        </div>

        <div v-if="loading" class="typing">
          <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <input v-model="userInput" class="input-field" placeholder="输入你的问题..." @keydown.enter="sendMessage" :disabled="loading" />
          <button class="send-btn" @click="sendMessage" :disabled="loading || !userInput.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 2 11 13"/><path d="M22 2 15 22 11 13 2 9z"/></svg>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useAppStore } from '../../stores/app'

const store = useAppStore()
const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const mode = ref('auto')
const backendOnline = ref(false)
const messagesRef = ref(null)

const agents = [
  { role: 'orchestrator', ready: true },
  { role: 'calculator', ready: true },
  { role: 'weather', ready: true },
  { role: 'searcher', ready: false },
  { role: 'summarizer', ready: false },
]

const modes = [
  { value: 'auto', label: '自动' },
  { value: 'orchestrate', label: '编排' },
  { value: 'parallel', label: '并行' },
  { value: 'debate', label: '辩论' },
  { value: 'critique_chain', label: '批评链' },
  { value: 'voting_consensus', label: '投票' },
  { value: 'brainstorm', label: '头脑风暴' },
]

let interval = null

onMounted(() => {
  checkBackend()
  interval = setInterval(checkBackend, 5000)
})

onBeforeUnmount(() => {
  if (interval) clearInterval(interval)
})

async function checkBackend() {
  try {
    const res = await fetch('http://localhost:8080/api/health', { signal: AbortSignal.timeout(3000) })
    backendOnline.value = res.ok
  } catch {
    backendOnline.value = false
  }
}

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text || loading.value) return
  userInput.value = ''
  messages.value.push({ role: 'user', text })
  loading.value = true

  try {
    const data = await store.apiPost('/chat', {
      message: text,
      action: mode.value,
      roles: [],
    })
    messages.value.push({
      role: 'assistant',
      text: data.reply || data.text || '(empty response)',
      agentRole: data.agentRole || 'System',
      collaborationMode: mode.value,
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', text: '请求失败: ' + (e.message || '未知错误'), agentRole: 'System' })
  } finally {
    loading.value = false
    await nextTick()
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-layout { display: flex; height: 100%; overflow: hidden; }

/* Sidebar */
.chat-sidebar { width: 200px; flex-shrink: 0; background: #141428; border-right: 1px solid rgba(108,92,231,0.15); display: flex; flex-direction: column; gap: 16px; padding: 16px 12px; overflow-y: auto; }
.sidebar-section { display: flex; flex-direction: column; gap: 8px; }
.section-title { font-size: 10px; font-weight: 600; color: #68687d; text-transform: uppercase; letter-spacing: 0.5px; }
.agent-list { display: flex; flex-direction: column; gap: 4px; }
.agent-item { display: flex; align-items: center; gap: 6px; padding: 4px 6px; background: #1a1a30; border-radius: 5px; font-size: 11px; }
.dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.dot-on { background: #4ade80; box-shadow: 0 0 4px rgba(74,222,128,0.4); }
.dot-off { background: #f87171; }
.agent-name { flex: 1; color: #9898b0; font-family: 'JetBrains Mono', monospace; font-size: 10px; }
.agent-status { font-size: 10px; font-weight: 500; }
.text-green { color: #4ade80; }
.text-red { color: #f87171; }

.mode-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.mode-btn { padding: 5px 4px; border: 1px solid rgba(108,92,231,0.15); border-radius: 5px; background: #1a1a30; color: #9898b0; font-size: 10px; cursor: pointer; transition: all 0.15s; }
.mode-btn:hover { border-color: rgba(108,92,231,0.3); }
.mode-btn.active { border-color: #6c5ce7; background: rgba(108,92,231,0.1); color: #6c5ce7; }
.mode-label { font-weight: 500; }

.status-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #9898b0; }

/* Chat Main */
.chat-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.messages { flex: 1; overflow-y: auto; padding: 20px 24px; }
.welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; height: 100%; min-height: 300px; gap: 8px; }
.welcome h2 { font-size: 20px; font-weight: 700; color: #e8e8f0; }
.welcome p { font-size: 13px; color: #68687d; max-width: 360px; }

.msg-row { margin-bottom: 16px; max-width: 780px; }
.msg-row.user { margin-left: auto; }
.msg-row.user .bubble { background: rgba(108,92,231,0.1); border: 1px solid rgba(108,92,231,0.2); }
.bubble { background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; padding: 12px 16px; }
.sender { font-size: 11px; margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
.sender-name { color: #6c5ce7; font-weight: 600; }
.mode-tag { background: rgba(108,92,231,0.1); color: #6c5ce7; padding: 1px 6px; border-radius: 8px; font-size: 9px; }
.text { font-size: 14px; line-height: 1.6; color: #e8e8f0; white-space: pre-wrap; word-break: break-word; }

.typing { display: flex; gap: 4px; padding: 12px 0; }
.typing-dot { width: 6px; height: 6px; border-radius: 50%; background: #6c5ce7; animation: bounce 1.4s infinite; }
.typing-dot:nth-child(2) { animation-delay: 0.16s; }
.typing-dot:nth-child(3) { animation-delay: 0.32s; }
@keyframes bounce { 0%,60%,100% { transform: translateY(0); } 30% { transform: translateY(-6px); } }

.input-area { padding: 12px 24px 16px; background: linear-gradient(to top, #0d0d1a 60%, transparent); }
.input-wrapper { display: flex; gap: 8px; background: #1a1a30; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; padding: 4px; transition: all 0.15s; }
.input-wrapper:focus-within { border-color: #6c5ce7; box-shadow: 0 0 0 2px rgba(108,92,231,0.1); }
.input-field { flex: 1; padding: 10px 12px; border: none; background: transparent; color: #e8e8f0; font-size: 14px; outline: none; }
.input-field::placeholder { color: #68687d; }
.send-btn { width: 40px; height: 40px; border: none; border-radius: 8px; background: #6c5ce7; color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.15s; flex-shrink: 0; }
.send-btn:hover:not(:disabled) { background: #8b7cf7; transform: scale(1.05); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>