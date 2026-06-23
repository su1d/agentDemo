<template>
  <div class="chat-layout">
    <aside class="chat-sidebar glass-strong">
      <div class="sidebar-section">
        <div class="section-title">Agent 状态</div>
        <div class="agent-list">
          <div v-for="a in agents" :key="a.role" class="agent-item glass">
            <span class="agent-avatar-sm" :style="{ background: agentColors[a.role] || '#6c5ce7' }">
              {{ a.role.charAt(0).toUpperCase() }}
            </span>
            <div class="agent-sm-meta">
              <span class="agent-sm-name">{{ a.role }}</span>
            </div>
            <span class="dot" :class="a.ready ? 'dot-on' : 'dot-off'"></span>
          </div>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-title">协作模式</div>
        <div class="mode-grid">
          <button
            v-for="m in modes"
            :key="m.value"
            @click="mode = m.value"
            :class="['mode-btn glass', mode === m.value ? 'active' : '']"
          >
            <span class="mode-label">{{ m.label }}</span>
          </button>
        </div>
      </div>

      <div class="sidebar-section sidebar-status">
        <div class="section-title">后端状态</div>
        <div class="status-row glass">
          <span class="dot" :class="backendOnline ? 'dot-on' : 'dot-off'"></span>
          <span>{{ backendOnline ? '已连接' : '离线' }}</span>
          <span class="status-ping" :class="backendOnline ? 'ping-ok' : ''"></span>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h2>智能对话</h2>
          <p>选择协作模式，输入你的问题，AI Agent 团队将为你协作完成</p>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
          <div class="bubble" :class="msg.role === 'assistant' ? 'glass-strong' : ''">
            <div class="sender" v-if="msg.role === 'assistant'">
              <span class="sender-avatar" :style="{ background: agentColors[msg.agentRole] || '#6c5ce7' }">{{ msg.agentRole ? msg.agentRole.charAt(0).toUpperCase() : 'S' }}</span>
              <span class="sender-name">{{ msg.agentRole || 'System' }}</span>
              <span v-if="msg.collaborationMode" class="mode-tag">{{ msg.collaborationMode }}</span>
            </div>
            <div class="text">{{ msg.text }}</div>
          </div>
        </div>

        <div v-if="loading" class="typing">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper glass-strong">
          <input
            v-model="userInput"
            class="input-field"
            placeholder="输入你的问题..."
            @keydown.enter="sendMessage"
            :disabled="loading"
          />
          <button class="send-btn" @click="sendMessage" :disabled="loading || !userInput.trim()">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 2 11 13"/>
              <path d="M22 2 15 22 11 13 2 9z"/>
            </svg>
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

const agentColors = {
  orchestrator: '#6c5ce7',
  calculator: '#60a5fa',
  weather: '#4ade80',
  searcher: '#fb923c',
  summarizer: '#f472b6'
}

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
.chat-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.chat-sidebar {
  width: 210px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 12px;
  overflow-y: auto;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-status {
  margin-top: auto;
}

.section-title {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
}

.agent-avatar-sm {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.agent-sm-meta {
  flex: 1;
}

.agent-sm-name {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 500;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-on { background: var(--green); box-shadow: 0 0 4px rgba(74,222,128,0.4); }
.dot-off { background: var(--red); }

.mode-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.mode-btn {
  padding: 6px 4px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  text-align: center;
  border: none;
}

.mode-btn:hover {
  border-color: var(--border-light);
  color: var(--text-primary);
}

.mode-btn.active {
  border-color: var(--border-active);
  background: rgba(108,92,231,0.12);
  color: var(--accent);
}

.mode-label {
  font-weight: 600;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 8px 10px;
  border-radius: var(--radius-sm);
}

.status-ping {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--red);
}

.status-ping.ping-ok {
  background: var(--green);
  animation: ping-pulse 2s ease infinite;
}

@keyframes ping-pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  min-height: 300px;
  gap: 12px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: rgba(108,92,231,0.08);
  margin-bottom: 4px;
}

.welcome h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.welcome p {
  font-size: 13px;
  color: var(--text-muted);
  max-width: 360px;
  line-height: 1.6;
}

.msg-row {
  margin-bottom: 16px;
  max-width: 780px;
  animation: fadeIn 0.2s ease;
}

.msg-row.user {
  margin-left: auto;
}

.msg-row.user .bubble {
  background: rgba(108,92,231,0.1);
  border: 1px solid rgba(108,92,231,0.15);
  border-radius: 14px 14px 4px 14px;
}

.bubble {
  border-radius: 14px 14px 14px 4px;
  padding: 12px 16px;
}

.sender {
  font-size: 11px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sender-avatar {
  width: 20px;
  height: 20px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
  color: #fff;
}

.sender-name {
  color: var(--accent);
  font-weight: 600;
  font-size: 12px;
}

.mode-tag {
  background: rgba(108,92,231,0.1);
  color: var(--accent-light);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 500;
}

.text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.typing {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: rgba(20,20,42,0.5);
  border-radius: 12px;
  width: fit-content;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce 1.4s infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.16s; }
.typing-dot:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%,60%,100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.input-area {
  padding: 12px 24px 16px;
  background: linear-gradient(to top, var(--bg-deep) 60%, transparent);
}

.input-wrapper {
  display: flex;
  gap: 8px;
  border-radius: var(--radius-md);
  padding: 4px;
  transition: var(--transition);
}

.input-wrapper:focus-within {
  border-color: var(--border-active);
  box-shadow: 0 0 0 3px rgba(108,92,231,0.08);
}

.input-field {
  flex: 1;
  padding: 11px 14px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
}

.input-field::placeholder {
  color: var(--text-dim);
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: var(--accent-light);
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
