<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-section">
        <div class="section-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          <h3>Agent 状态</h3>
        </div>
        <div class="agents-list">
          <div v-for="agent in agentsList" :key="agent.role" class="agent-item">
            <span class="agent-dot" :class="agent.ready ? 'dot-green' : 'dot-red'"></span>
            <span class="agent-role">{{ agent.role }}</span>
            <span v-if="agent.ready" class="check-mark">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
          </div>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
          <h3>协作模式</h3>
        </div>
        <div class="mode-grid">
          <button v-for="m in modes" :key="m.value"
            @click="selectMode(m.value)"
            :class="['mode-btn', mode === m.value ? 'active' : '']"
            :title="m.desc">
            <span class="mode-icon">{{ m.icon }}</span>
            <span class="mode-label">{{ m.label }}</span>
          </button>
        </div>

        <transition name="fade">
          <div v-if="showModeOptions" class="mode-options">
            <div v-if="['orchestrate', 'parallel', 'debate', 'voting_consensus', 'brainstorm'].includes(mode)">
              <label class="option-label">参与 Agent</label>
              <div class="role-checkbox-group">
                <label v-for="r in availableRoles" :key="r" class="role-tag" :class="{ selected: selectedRoles.includes(r) }">
                  <input type="checkbox" :value="r" v-model="selectedRoles" />
                  {{ r }}
                </label>
              </div>
            </div>
            <div v-if="mode === 'debate'">
              <label class="option-label">辩论轮数 <input type="number" v-model.number="debateRounds" min="1" max="5" class="opt-input" /></label>
            </div>
            <div v-if="mode === 'critique_chain'">
              <label class="option-label">生成 Agent <select v-model="critiqueGenRole" class="opt-select"><option v-for="r in availableRoles" :key="r" :value="r">{{ r }}</option></select></label>
              <label class="option-label">批评 Agent <select v-model="critiqueCritRole" class="opt-select"><option v-for="r in availableRoles" :key="r" :value="r">{{ r }}</option></select></label>
              <label class="option-label">改进轮数 <input type="number" v-model.number="critiqueRounds" min="1" max="5" class="opt-input" /></label>
            </div>
          </div>
        </transition>
      </div>

      <div class="sidebar-section">
        <div class="section-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v8"/><path d="M8 12h8"/></svg>
          <h3>后端状态</h3>
        </div>
        <div class="status-grid">
          <div class="status-item">
            <span class="status-label">后端</span>
            <span class="status-value" :class="backendOnline ? 'text-green' : 'text-red'">
              <span class="status-dot" :class="backendOnline ? 'dot-green' : 'dot-red'"></span>
              {{ backendOnline ? '在线' : '离线' }}
            </span>
          </div>
          <div class="status-item">
            <span class="status-label">Agent</span>
            <span class="status-value text-green">
              <span class="status-dot dot-green"></span>
              {{ readyCount }}/{{ totalCount }}
            </span>
          </div>
        </div>
      </div>
    </aside>

    <!-- Chat Area -->
    <main class="chat-area">
      <div class="messages-container" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-graphic">
            <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
              <rect x="10" y="10" width="60" height="60" rx="16" stroke="#6c5ce7" stroke-width="1.5" stroke-dasharray="4 4" fill="url(#grad)"/>
              <defs><linearGradient id="grad" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#6c5ce7" stop-opacity="0.08"/><stop offset="100%" stop-color="#8b7cf7" stop-opacity="0.04"/></linearGradient></defs>
              <path d="M32 40C32 32.27 38.27 26 46 26C53.73 26 60 32.27 60 40" stroke="#6c5ce7" stroke-width="2" stroke-linecap="round"/>
              <path d="M46 32V38" stroke="#6c5ce7" stroke-width="2" stroke-linecap="round"/>
              <circle cx="46" cy="44" r="2.5" fill="#6c5ce7"/>
              <path d="M28 50C28 42.27 34.27 36 42 36H50C57.73 36 64 42.27 64 50" stroke="#8b7cf7" stroke-width="1.5" stroke-linecap="round" stroke-dasharray="3 3"/>
              <circle cx="40" cy="56" r="2" fill="#4ade80" opacity="0.6"/>
              <circle cx="52" cy="56" r="2" fill="#fb923c" opacity="0.6"/>
              <circle cx="46" cy="60" r="2" fill="#facc15" opacity="0.6"/>
            </svg>
          </div>
          <h2>你好！我是多智能体系统</h2>
          <p>5 个专家 Agent · 7 种协作模式</p>
          <div class="mode-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            当前模式：<strong>{{ getModeLabel(mode) }}</strong>
          </div>
          <p class="empty-hint">在左侧选择协作模式，然后在下方输入你的问题</p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
          <div class="avatar-wrapper" :class="msg.role">
            <div class="avatar" :class="msg.role">
              <svg v-if="msg.role === 'user'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="20" height="14" x="2" y="7" rx="2"/><path d="M12 22v-5"/><path d="M9 22h6"/></svg>
            </div>
          </div>
          <div class="bubble">
            <div v-if="msg.role === 'assistant'" class="sender">
              <span class="sender-name">{{ msg.agentRole || 'System' }}</span>
              <span v-if="msg.collaborationMode" class="mode-tag">{{ msg.collaborationMode }}</span>
            </div>
            <div class="text">{{ msg.text }}</div>
            <div v-if="msg.details" class="detail-section">
              <div v-if="msg.collaborationMode === 'orchestrate'" class="summary-card">
                <div class="summary-card-header">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4M12 22v-4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M22 12h-4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
                  综合分析
                </div>
                <div class="summary-card-body">{{ msg.details.summary }}</div>
              </div>
              <div v-else-if="msg.collaborationMode === 'parallel'" class="parallel-results">
                <div v-for="(res, role) in msg.details.parallelResults" :key="role" class="agent-card">
                  <div class="agent-card-header">{{ role }}</div>
                  <div class="agent-card-body">{{ res }}</div>
                </div>
              </div>
              <div v-else-if="msg.collaborationMode === 'debate'" class="debate-results">
                <div v-for="round in msg.details.rounds" :key="round.round" class="debate-round">
                  <div class="round-title">第 {{ round.round }} 轮</div>
                  <div v-for="(res, role) in round.responses" :key="role" class="agent-card">
                    <div class="agent-card-header">{{ role }}</div>
                    <div class="agent-card-body">{{ res }}</div>
                  </div>
                </div>
              </div>
              <div v-else-if="msg.collaborationMode === 'critique_chain'" class="critique-results">
                <div v-for="(step, si) in msg.details.steps" :key="si" class="critique-step" :class="'step-' + step.type">
                  <div class="step-icon">
                    <svg v-if="step.type === 'generate'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4M12 22v-4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M22 12h-4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
                    <svg v-else-if="step.type === 'critique'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                  <div class="step-content">
                    <div class="step-header">{{ step.label }}</div>
                    <div class="step-body">{{ step.content }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="typing-indicator">
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
          <span class="typing-text">思考中...</span>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <input
            v-model="userInput"
            class="chat-input"
            placeholder="输入你的问题..."
            @keydown.enter="sendMessage"
            :disabled="loading"
          />
          <button class="send-btn" @click="sendMessage" :disabled="loading || !userInput.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 2 11 13"/><path d="M22 2 15 22 11 13 2 9z"/></svg>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>
<script>
import { apiGet, apiChat } from '../auth'

const API_BASE = 'http://localhost:8080/api'
const agentsList = [
  { role: 'orchestrator', ready: true },
  { role: 'calculator', ready: true },
  { role: 'weather', ready: true },
  { role: 'searcher', ready: false },
  { role: 'summarizer', ready: false },
]

export default {
  name: 'ChatView',
  data() {
    return {
      agentsList,
      backendOnline: false,
      messages: [],
      userInput: '',
      loading: false,
      mode: 'auto',
      selectedRoles: [],
      debateRounds: 2,
      critiqueGenRole: 'orchestrator',
      critiqueCritRole: 'weather',
      critiqueRounds: 2,
      availableRoles: agentsList.filter(a => a.ready).map(a => a.role),
      modes: [
        { value: 'orchestrate', label: '编排', icon: '🎼', desc: '分配任务给各个Agent并汇总结果' },
        { value: 'parallel', label: '并行', icon: '⚡', desc: '所有Agent同时处理同一任务' },
        { value: 'debate', label: '辩论', icon: '🗣️', desc: '多个Agent进行多轮辩论' },
        { value: 'voting_consensus', label: '投票', icon: '🗳️', desc: 'Agent投票达成共识' },
        { value: 'critique_chain', label: '批评链', icon: '⛓️', desc: '生成-批评-改进循环' },
        { value: 'brainstorm', label: '头脑风暴', icon: '🧠', desc: '自由发散创意' },
        { value: 'auto', label: '自动', icon: '🤖', desc: '自动路由到Orchestrator' },
      ],
      backendCheckInterval: null,
    }
  },
  computed: {
    showModeOptions() {
      return ['orchestrate', 'parallel', 'debate', 'voting_consensus', 'brainstorm', 'critique_chain'].includes(this.mode)
    },
    readyCount() {
      return this.agentsList.filter(a => a.ready).length
    },
    totalCount() {
      return this.agentsList.length
    },
  },
  mounted() {
    this.checkBackend()
    this.backendCheckInterval = setInterval(() => this.checkBackend(), 5000)
  },
  beforeDestroy() {
    if (this.backendCheckInterval) clearInterval(this.backendCheckInterval)
  },
  methods: {
    async checkBackend() {
      try {
        const res = await fetch(API_BASE + '/health', { signal: AbortSignal.timeout(3000) })
        this.backendOnline = res.ok
      } catch { this.backendOnline = false }
    },
    selectMode(value) {
      this.mode = value
      if (value === 'single') {
        this.selectedRoles = [this.availableRoles[0]]
      } else {
        this.selectedRoles = [...this.availableRoles]
      }
    },
    getModeLabel(value) {
      const m = this.modes.find(m => m.value === value)
      return m ? m.label : value
    },
    async sendMessage() {
      const text = this.userInput.trim()
      if (!text || this.loading) return
      this.userInput = ''
      this.messages.push({ role: 'user', text })
      this.loading = true

      try {
        const body = {
            message: text,
            action: this.mode,
            roles: this.selectedRoles,
            rounds: this.debateRounds,
            generateRole: this.critiqueGenRole,
            critiqueRole: this.critiqueCritRole,
            refineRounds: this.critiqueRounds,
          }
        const data = await apiChat(body)
        this.messages.push({
          role: 'assistant',
          text: data.reply || data.text || '',
          agentRole: data.agentRole || 'System',
          collaborationMode: this.mode,
          details: data,
        })
      } catch (e) {
        this.messages.push({ role: 'assistant', text: '❌ 请求失败：' + (e.message || '未知错误'), agentRole: 'System' })
      } finally {
        this.loading = false
        this.$nextTick(() => {
          const el = this.$refs.messagesRef
          if (el) el.scrollTop = el.scrollHeight
        })
      }
    },
  },
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* ── Sidebar ── */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 16px 12px;
  gap: 20px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-header h3 {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}

.section-header svg {
  opacity: 0.7;
}

.agents-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: var(--bg-secondary);
  border-radius: 6px;
  font-size: 12px;
}

.agent-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-green { background: var(--green); box-shadow: 0 0 6px rgba(74,222,128,0.4); }
.dot-red { background: var(--red); box-shadow: 0 0 6px rgba(248,113,113,0.4); }

.agent-role {
  flex: 1;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
}

.check-mark {
  color: var(--green);
  display: flex;
}

.mode-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.mode-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 4px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 10px;
  transition: var(--transition);
}

.mode-btn:hover {
  border-color: rgba(108,92,231,0.3);
  background: rgba(108,92,231,0.04);
}

.mode-btn.active {
  border-color: var(--accent);
  background: rgba(108,92,231,0.1);
  color: var(--accent);
}

.mode-icon { font-size: 16px; }
.mode-label { font-size: 9px; font-weight: 500; }

.mode-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.option-label {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.role-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 10px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
}

.role-tag:hover { border-color: var(--accent); }

.role-tag.selected {
  background: rgba(108,92,231,0.1);
  border-color: var(--accent);
  color: var(--accent);
}

.role-tag input { display: none; }

.opt-input, .opt-select {
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 12px;
  width: 100%;
}

.opt-input:focus, .opt-select:focus {
  border-color: var(--accent);
  outline: none;
}

.status-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  background: var(--bg-secondary);
  border-radius: 6px;
  font-size: 12px;
}

.status-label { color: var(--text-muted); }
.status-value {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.text-green { color: var(--green); }
.text-red { color: var(--red); }

/* ── Chat Area ── */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  min-height: 400px;
  gap: 12px;
}

.empty-graphic { margin-bottom: 8px; }

.empty-state h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-muted);
}

.mode-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(108,92,231,0.08);
  border: 1px solid rgba(108,92,231,0.15);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.mode-badge strong { color: var(--accent); }

.empty-hint {
  font-size: 12px !important;
  color: var(--text-muted) !important;
  font-style: italic;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  max-width: 900px;
  animation: fadeInUp 0.3s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-row.user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-row.user .bubble {
  background: rgba(108,92,231,0.1);
  border: 1px solid rgba(108,92,231,0.2);
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar.user {
  background: linear-gradient(135deg, var(--accent), #a78bfa);
  color: #fff;
}

.avatar.assistant {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  color: var(--accent);
}

.bubble {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  max-width: 750px;
  box-shadow: var(--shadow-card);
}

.sender {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sender-name { color: var(--accent); }

.mode-tag {
  background: rgba(108,92,231,0.1);
  color: var(--accent);
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 500;
}

.text {
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
  color: var(--text-primary);
}

.detail-section {
  margin-top: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.summary-card {
  background: rgba(74,222,128,0.04);
  border: 1px solid rgba(74,222,128,0.15);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.summary-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(74,222,128,0.06);
  padding: 8px 12px;
  font-size: 12px;
  color: var(--green);
  font-weight: 600;
}

.summary-card-body {
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.agent-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  overflow: hidden;
}

.agent-card-header {
  background: rgba(108,92,231,0.06);
  padding: 6px 12px;
  font-size: 11px;
  color: var(--accent);
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.agent-card-body {
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.debate-round { margin-bottom: 12px; }
.round-title {
  font-size: 12px;
  color: var(--yellow);
  font-weight: 700;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.critique-step {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  padding: 10px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--border-color);
}

.step-generate { border-left-color: var(--accent); }
.step-critique { border-left-color: var(--red); }
.step-refine { border-left-color: var(--green); }

.step-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.step-generate .step-icon { color: var(--accent); }
.step-critique .step-icon { color: var(--red); }
.step-refine .step-icon { color: var(--green); }

.step-content { flex: 1; }
.step-header { font-size: 11px; color: var(--text-muted); font-weight: 600; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.3px; }
.step-body { font-size: 13px; color: var(--text-secondary); line-height: 1.6; white-space: pre-wrap; }

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 0;
}

.typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(2) { animation-delay: 0.16s; }
.typing-dots span:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

.typing-text {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.input-area {
  padding: 16px 32px 20px;
  background: linear-gradient(to top, var(--bg-primary) 60%, transparent);
}

.input-wrapper {
  display: flex;
  gap: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 4px;
  transition: var(--transition);
}

.input-wrapper:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
}

.chat-input::placeholder { color: var(--text-muted); }
.chat-input:disabled { opacity: 0.5; }

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 8px;
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

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>



