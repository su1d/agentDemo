<template>
  <div class="app-container">
    <header class="app-header">
      <h1>多智能体协调系统</h1>
      <div class="header-right">
        <span class="badge" :class="statusClass">{{ statusText }}</span>
        <button class="info-btn" @click="showInfoModal = true">ℹ️</button>
      </div>
    </header>

    <div class="main-layout">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <h3>Agent 状态</h3>
          <div class="agents-list">
            <div v-for="agent in agentsList" :key="agent.role" class="agent-item">
              <div class="agent-dot" :class="agent.ready ? 'dot-green' : 'dot-red'"></div>
              <code>{{ agent.role }}</code>
              <span v-if="agent.ready" class="check-mark">✓</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>协作模式</h3>
          <div class="mode-grid">
            <button v-for="m in modes" :key="m.value"
              @click="selectMode(m.value)"
              :class="['mode-btn', mode === m.value ? 'active' : '']"
              :title="m.desc">
              <span class="mode-icon">{{ m.icon }}</span>
              <span class="mode-label">{{ m.label }}</span>
            </button>
          </div>

          <!-- Mode-specific options -->
          <div v-if="showModeOptions" class="mode-options">
            <!-- Chain / Parallel: role picker -->
            <div v-if="['orchestrate', 'parallel', 'debate', 'voting_consensus', 'brainstorm'].includes(mode)">
              <label class="option-label">选择参与 Agent：</label>
              <div class="role-checkbox-group">
                <label v-for="r in availableRoles" :key="r" class="role-checkbox">
                  <input type="checkbox" :value="r" v-model="selectedRoles" />
                  {{ r }}
                </label>
              </div>
            </div>

            <!-- Debate rounds -->
            <div v-if="mode === 'debate'">
              <label class="option-label">辩论轮数：</label>
              <input type="number" v-model.number="debateRounds" min="1" max="5" class="opt-input" />
            </div>

            <!-- Critique chain -->
            <div v-if="mode === 'critique_chain'">
              <label class="option-label">生成 Agent：
                <select v-model="critiqueGenRole" class="opt-select">
                  <option v-for="r in availableRoles" :key="r" :value="r">{{ r }}</option>
                </select>
              </label>
              <label class="option-label">批评 Agent：
                <select v-model="critiqueCritRole" class="opt-select">
                  <option v-for="r in availableRoles" :key="r" :value="r">{{ r }}</option>
                </select>
              </label>
              <label class="option-label">改进轮数：
                <input type="number" v-model.number="critiqueRounds" min="1" max="5" class="opt-input" />
              </label>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>后端状态</h3>
          <p>后端: <span :class="backendOnline ? 'text-green' : 'text-red'">{{ backendOnline ? '在线' : '离线' }}</span></p>
          <p>Agent 就绪: <span class="text-green">{{ readyCount }}/{{ totalCount }}</span></p>
        </div>
        <div class="sidebar-section">
          <h3>工作流</h3>
          <button @click="showWorkflowEditor = !showWorkflowEditor" :class="['mode-btn', showWorkflowEditor ? 'active' : '']" style="width:100%;">
            <span class="mode-icon">🔀</span>
            <span class="mode-label">工作流编辑器</span>
          </button>
        </div>
      </aside>

      <!-- Chat Area -->
      <main v-if="showWorkflowEditor" class="chat-area" style="padding:0;overflow:hidden;">
        <WorkflowEditor :workflowId="currentWorkflowId" @created="currentWorkflowId = $event" />
      </main>
      <main v-else class="chat-area">
        <div class="messages-container" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">🤖</div>
            <h2>你好！我是多智能体系统</h2>
            <p>我有 5 个专家 Agent + 7 种协作模式</p>
            <p>当前模式：<strong>{{ getModeLabel(mode) }}</strong></p>
            <p class="empty-hint">在左侧选择协作模式，然后在下方输入你的问题</p>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
            <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="bubble">
              <div class="sender">
                {{ msg.role === 'user' ? '你' : (msg.agentName || '助手') }}
                <span v-if="msg.mode" class="mode-tag">{{ getModeLabel(msg.mode) }}</span>
              </div>
              <div class="text">{{ msg.content }}</div>
              <!-- Detailed visualization for collaboration modes -->
              <div v-if="msg.detail" class="detail-section">
                <!-- Parallel mode detail -->
                <div v-if="msg.detail.mode === 'parallel'" class="collab-detail">
                  <div class="detail-title">🤖 各 Agent 回复</div>
                  <div v-for="(res, rRole) in msg.detail.individual_results" :key="rRole" class="agent-card">
                    <div class="agent-card-header">{{ res.name }} ({{ rRole }})</div>
                    <div class="agent-card-body">{{ res.reply }}</div>
                  </div>
                  <div class="summary-card">
                    <div class="summary-card-header">📋 综合汇总</div>
                    <div class="summary-card-body">{{ msg.detail.summary }}</div>
                  </div>
                </div>

                <!-- Debate mode detail -->
                <div v-if="msg.detail.mode === 'debate'" class="collab-detail">
                  <div v-for="(rnd, ri) in msg.detail.rounds" :key="ri" class="debate-round">
                    <div class="round-title">🏛️ 第 {{ rnd.round }} 轮</div>
                    <div v-for="(res, rRole) in rnd.results" :key="rRole" class="agent-card">
                      <div class="agent-card-header">{{ res.name }} ({{ rRole }})</div>
                      <div class="agent-card-body">{{ res.reply }}</div>
                    </div>
                  </div>
                  <div class="summary-card">
                    <div class="summary-card-header">🎯 最终结论</div>
                    <div class="summary-card-body">{{ msg.detail.conclusion }}</div>
                  </div>
                </div>

                <!-- Critique chain detail -->
                <div v-if="msg.detail.mode === 'critique_chain'" class="collab-detail">
                  <div v-for="(step, si) in msg.detail.history" :key="si" class="critique-step"
                    :class="step.step.includes('批评') ? 'step-critique' : (step.step.includes('改进') ? 'step-refine' : 'step-generate')">
                    <div class="step-icon">{{ step.step.includes('批评') ? '🔍' : (step.step.includes('改进') ? '🔧' : '📝') }}</div>
                    <div class="step-content">
                      <div class="step-header">{{ step.name }} ({{ step.role }}) - {{ step.step }}</div>
                      <div class="step-body">{{ step.content }}</div>
                    </div>
                  </div>
                  <div class="summary-card">
                    <div class="summary-card-header">✅ 最终答案</div>
                    <div class="summary-card-body">{{ msg.detail.final_answer }}</div>
                  </div>
                </div>

                <!-- Voting consensus detail -->
                <div v-if="msg.detail.mode === 'voting_consensus'" class="collab-detail">
                  <div class="detail-title">📊 各 Agent 方案</div>
                  <div v-for="(prop, pRole) in msg.detail.proposals" :key="pRole" class="agent-card">
                    <div class="agent-card-header">{{ prop.name }} ({{ pRole }})</div>
                    <div class="agent-card-body">{{ prop.proposal }}</div>
                  </div>
                  <div class="summary-card">
                    <div class="summary-card-header">🏆 评估与最佳方案</div>
                    <div class="summary-card-body">{{ msg.detail.evaluation }}</div>
                  </div>
                </div>

                <!-- Brainstorm detail -->
                <div v-if="msg.detail.mode === 'brainstorm'" class="collab-detail">
                  <div class="detail-title">💡 各 Agent 创意</div>
                  <div v-for="(idea, iRole) in msg.detail.ideas" :key="iRole" class="agent-card">
                    <div class="agent-card-header">{{ idea.name }} ({{ iRole }})</div>
                    <div class="agent-card-body">{{ idea.ideas }}</div>
                  </div>
                  <div class="summary-card">
                    <div class="summary-card-header">📑 整理报告</div>
                    <div class="summary-card-body">{{ msg.detail.organized_report }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="loading" class="message-row assistant">
            <div class="avatar">🤖</div>
            <div class="bubble typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <div class="input-area">
          <input
            v-model="userInput"
            @keydown.enter="sendMessage"
            placeholder="输入你的消息..."
            :disabled="loading"
            class="chat-input"
          />
          <button @click="sendMessage" :disabled="loading || !userInput.trim()" class="send-btn">
            ➤
          </button>
        </div>
      </main>
    </div>

    <!-- Info Modal -->
    <div v-if="showInfoModal" class="modal-overlay" @click.self="showInfoModal = false">
      <div class="modal">
        <div class="modal-header">
          <h2>多智能体协作系统</h2>
          <button class="modal-close" @click="showInfoModal = false">✕</button>
        </div>
        <div class="modal-body">
          <h3>协作模式说明</h3>
          <div v-for="m in modes" :key="m.value" class="mode-desc">
            <strong>{{ m.icon }} {{ m.label }}</strong>
            <p>{{ m.desc }}</p>
          </div>
          <h3>架构</h3>
          <p>Vue 3 Frontend → Java 21 Spring Boot → Python LangGraph Workers (5 agents + 1 orchestrator)</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import WorkflowEditor from './components/WorkflowEditor.vue'
import WorkflowViewer from './components/WorkflowViewer.vue'
export default {
  components: { WorkflowEditor, WorkflowViewer },
  name: 'App',
  data() {
    return {
      messages: [],
      userInput: '',
      loading: false,
      mode: 'auto',
      agentsList: [],
      availableRoles: ['assistant', 'calculator', 'weather', 'searcher', 'summarizer'],
      selectedRoles: ['calculator', 'searcher', 'summarizer'],
      debateRounds: 2,
      critiqueGenRole: 'searcher',
      critiqueCritRole: 'summarizer',
      critiqueRounds: 2,
      backendOnline: false,
      readyCount: 0,
      totalCount: 0,
      showInfoModal: false,
      showWorkflowEditor: false,
      currentWorkflowId: null,
      modes: [
        { value: 'auto', label: '自动路由', icon: '🎯', desc: '自动选择最合适的单一 Agent 处理' },
        { value: 'orchestrate', label: '链式编排', icon: '🔗', desc: '多个 Agent 依次处理，前一个输出作为下一个输入' },
        { value: 'parallel', label: '并行汇总', icon: '⚡', desc: '多个 Agent 同时处理同一问题，然后汇总' },
        { value: 'debate', label: '辩论', icon: '🏛️', desc: '多个 Agent 多轮辩论后给出结论' },
        { value: 'critique_chain', label: '批评链', icon: '🔍', desc: 'A 生成 → B 批评 → A 改进（多轮优化）' },
        { value: 'voting_consensus', label: '投票共识', icon: '📊', desc: '各 Agent 出方案，评估选最佳' },
        { value: 'brainstorm', label: '头脑风暴', icon: '💡', desc: '各 Agent 自由创意，归类整理' },
      ]
    }
  },
  computed: {
    statusText() {
      if (!this.backendOnline) return '后端离线'
      if (this.readyCount < this.totalCount) return this.readyCount + '/' + this.totalCount + ' 就绪'
      return '全部在线'
    },
    statusClass() {
      if (!this.backendOnline) return 'badge-red'
      if (this.readyCount < this.totalCount) return 'badge-yellow'
      return 'badge-green'
    },
    showModeOptions() {
      return ['orchestrate', 'parallel', 'debate', 'voting_consensus', 'brainstorm', 'critique_chain'].includes(this.mode)
    },
    API_BASE() {
      return 'http://localhost:8080/api'
    }
  },
  methods: {
    getModeLabel(value) {
      const m = this.modes.find(m => m.value === value)
      return m ? m.label : value
    },
    selectMode(value) {
      this.mode = value
      // Default role selections per mode
      if (value === 'orchestrate') this.selectedRoles = ['calculator', 'searcher', 'summarizer']
      else if (value === 'parallel') this.selectedRoles = ['calculator', 'searcher', 'weather']
      else if (value === 'debate') this.selectedRoles = ['searcher', 'calculator']
      else if (value === 'voting_consensus') this.selectedRoles = ['calculator', 'searcher', 'weather']
      else if (value === 'brainstorm') this.selectedRoles = ['assistant', 'calculator', 'searcher', 'weather']
    },
    async sendMessage() {
      const msg = this.userInput.trim()
      if (!msg || this.loading) return
      this.userInput = ''
      this.messages.push({ role: 'user', content: msg })
      this.loading = true

      try {
        const body = { message: msg, action: this.mode === 'auto' ? 'chat' : this.mode }

        if (this.mode === 'orchestrate' && this.selectedRoles.length > 0) {
          body.roles = this.selectedRoles
        }
        if (this.mode === 'parallel' && this.selectedRoles.length > 0) {
          body.roles = this.selectedRoles
        }
        if (this.mode === 'debate') {
          if (this.selectedRoles.length >= 2) body.roles = this.selectedRoles
          body.rounds = this.debateRounds
        }
        if (this.mode === 'critique_chain') {
          body.generateRole = this.critiqueGenRole
          body.critiqueRole = this.critiqueCritRole
          body.refineRounds = this.critiqueRounds
        }
        if (this.mode === 'voting_consensus' && this.selectedRoles.length > 0) {
          body.roles = this.selectedRoles
        }
        if (this.mode === 'brainstorm' && this.selectedRoles.length > 0) {
          body.roles = this.selectedRoles
        }

        const res = await fetch(this.API_BASE + '/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body)
        })
        const data = await res.json()

        let displayText = data.reply || '无响应'
        let detail = null

        // Try to parse rich response for visualization
        try {
          const parsed = JSON.parse(data.reply)
          if (parsed.mode && parsed.individual_results) {
            detail = parsed
            displayText = '✅ ' + this.getModeLabel(this.mode) + ' 完成 — 点击下方查看详情'
          } else if (parsed.mode && parsed.rounds) {
            detail = parsed
            displayText = '✅ 辩论完成 (' + parsed.rounds.length + ' 轮) — 点击下方查看详情'
          } else if (parsed.mode && parsed.history) {
            detail = parsed
            displayText = '✅ 批评链完成 (' + parsed.history.length + ' 步) — 点击下方查看详情'
          } else if (parsed.mode && parsed.proposals) {
            detail = parsed
            displayText = '✅ 投票共识完成 — 点击下方查看详情'
          } else if (parsed.mode && parsed.ideas) {
            detail = parsed
            displayText = '✅ 头脑风暴完成 — 点击下方查看详情'
          } else if (parsed.mode === 'parallel') {
            detail = parsed
            displayText = '✅ 并行汇总完成 — 点击下方查看详情'
          }
        } catch (e) {
          // Not JSON, use raw text
        }

        this.messages.push({
          role: 'assistant',
          content: displayText,
          mode: this.mode,
          detail: detail,
          agentName: this.getModeLabel(this.mode)
        })
      } catch (e) {
        this.messages.push({
          role: 'assistant',
          content: '请求失败: ' + e.message,
          agentName: '系统'
        })
      }

      this.loading = false
      this.$nextTick(() => {
        const container = this.$refs.messagesRef
        if (container) container.scrollTop = container.scrollHeight
      })
    },

    async fetchInfo() {
      try {
        const res = await fetch(this.API_BASE + '/info')
        const data = await res.json()
        if (data.agents) {
          this.agentsList = Object.entries(data.agents).map(([role, info]) => ({
            role, ready: info.ready
          }))
          this.readyCount = this.agentsList.filter(a => a.ready).length
          this.totalCount = this.agentsList.length
        }
        this.backendOnline = true
      } catch {
        this.backendOnline = false
        this.agentsList = []
      }
    }
  },
  async mounted() {
    await this.fetchInfo()
    setInterval(() => this.fetchInfo(), 10000)
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f0f1a; color: #e0e0e0; }
.app-container { display: flex; flex-direction: column; height: 100vh; }

.app-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: #1a1a2e; border-bottom: 1px solid #2a2a4a; }
.app-header h1 { font-size: 18px; font-weight: 600; color: #7c7cff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.badge-green { background: #1a3a2a; color: #4ade80; }
.badge-yellow { background: #3a3a1a; color: #facc15; }
.badge-red { background: #3a1a1a; color: #f87171; }
.info-btn { background: none; border: 1px solid #2a2a4a; border-radius: 4px; color: #a0a0b0; padding: 4px 8px; cursor: pointer; font-size: 14px; }
.info-btn:hover { background: #2a2a4a; }

.main-layout { display: flex; flex: 1; overflow: hidden; }

.sidebar { width: 280px; background: #15152a; border-right: 1px solid #2a2a4a; padding: 20px; overflow-y: auto; flex-shrink: 0; }
.sidebar-section { margin-bottom: 24px; }
.sidebar-section h3 { font-size: 13px; text-transform: uppercase; letter-spacing: 1px; color: #7c7cff; margin-bottom: 8px; }

.agents-list { display: flex; flex-direction: column; gap: 6px; }
.agent-item { display: flex; align-items: center; gap: 8px; }
.agent-dot { width: 8px; height: 8px; border-radius: 50%; }
.dot-green { background: #4ade80; box-shadow: 0 0 6px #4ade8066; }
.dot-red { background: #f87171; }
.agent-item code { font-size: 12px; color: #b0b0c0; }
.check-mark { color: #4ade80; font-size: 12px; }

.mode-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.mode-btn { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px 4px; border: 1px solid #2a2a4a; border-radius: 6px; background: #1e1e38; color: #a0a0b0; font-size: 10px; cursor: pointer; transition: all 0.15s; }
.mode-btn:hover { background: #2a2a4a; }
.mode-btn.active { background: #2a2a5a; border-color: #7c7cff; color: #7c7cff; box-shadow: 0 0 8px #7c7cff33; }
.mode-icon { font-size: 16px; }
.mode-label { font-size: 10px; }

.mode-options { margin-top: 8px; padding: 10px; background: #1a1a2e; border-radius: 6px; }
.option-label { display: block; font-size: 11px; color: #b0b0c0; margin-bottom: 4px; }
.role-checkbox-group { display: flex; flex-direction: column; gap: 3px; margin-bottom: 6px; }
.role-checkbox { font-size: 11px; color: #b0b0c0; display: flex; align-items: center; gap: 4px; cursor: pointer; }
.role-checkbox input { accent-color: #7c7cff; }
.opt-input { width: 60px; padding: 4px 6px; border: 1px solid #2a2a4a; border-radius: 4px; background: #0f0f1a; color: #e0e0e0; font-size: 12px; }
.opt-select { padding: 4px 6px; border: 1px solid #2a2a4a; border-radius: 4px; background: #0f0f1a; color: #e0e0e0; font-size: 12px; margin-left: 4px; }

.text-green { color: #4ade80; }
.text-red { color: #f87171; }
.sidebar-section p { font-size: 12px; color: #a0a0b0; line-height: 1.6; }

.chat-area { flex: 1; display: flex; flex-direction: column; background: #0f0f1a; min-width: 0; }
.messages-container { flex: 1; overflow-y: auto; padding: 24px; scroll-behavior: smooth; }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 64px; margin-bottom: 12px; }
.empty-state h2 { font-size: 20px; color: #a0a0b0; margin-bottom: 8px; }
.empty-state p { font-size: 13px; color: #606070; margin-bottom: 4px; }
.empty-hint { font-size: 12px; color: #505060; font-style: italic; }

.message-row { display: flex; gap: 12px; margin-bottom: 20px; max-width: 900px; }
.message-row.user { flex-direction: row-reverse; margin-left: auto; }
.message-row.user .bubble { background: #2a2a5a; border: 1px solid #3a3a7a; }
.message-row.user .avatar { order: -1; }

.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; background: #1e1e38; flex-shrink: 0; }
.bubble { background: #1e1e38; border: 1px solid #2a2a4a; border-radius: 12px; padding: 12px 16px; max-width: 750px; }
.sender { font-size: 11px; font-weight: 600; color: #7c7cff; margin-bottom: 4px; display: flex; align-items: center; gap: 8px; }
.mode-tag { background: #2a2a5a; color: #7c7cff; padding: 1px 8px; border-radius: 8px; font-size: 10px; font-weight: 400; }
.text { font-size: 14px; line-height: 1.6; word-break: break-word; white-space: pre-wrap; }

/* Collaboration detail styles */
.detail-section { margin-top: 12px; border-top: 1px solid #2a2a4a; padding-top: 12px; }
.detail-title { font-size: 12px; color: #7c7cff; font-weight: 600; margin-bottom: 8px; }
.agent-card { background: #15152a; border: 1px solid #25254a; border-radius: 6px; margin-bottom: 6px; overflow: hidden; }
.agent-card-header { background: #1a1a35; padding: 6px 10px; font-size: 11px; color: #b0b0ff; font-weight: 500; }
.agent-card-body { padding: 8px 10px; font-size: 12px; color: #c0c0d0; line-height: 1.5; white-space: pre-wrap; }
.summary-card { background: #1a2a1a; border: 1px solid #2a4a2a; border-radius: 6px; margin-top: 8px; overflow: hidden; }
.summary-card-header { background: #1a3a1a; padding: 6px 10px; font-size: 11px; color: #4ade80; font-weight: 500; }
.summary-card-body { padding: 8px 10px; font-size: 12px; color: #c0e0c0; line-height: 1.5; white-space: pre-wrap; }

.debate-round { margin-bottom: 8px; }
.round-title { font-size: 12px; color: #facc15; font-weight: 600; margin-bottom: 4px; }

.critique-step { display: flex; gap: 8px; margin-bottom: 6px; padding: 8px; background: #15152a; border-radius: 6px; border-left: 3px solid #2a2a4a; }
.step-generate { border-left-color: #7c7cff; }
.step-critique { border-left-color: #f87171; }
.step-refine { border-left-color: #4ade80; }
.step-icon { font-size: 16px; flex-shrink: 0; }
.step-content { flex: 1; }
.step-header { font-size: 11px; color: #b0b0c0; font-weight: 500; margin-bottom: 4px; }
.step-body { font-size: 12px; color: #c0c0d0; line-height: 1.5; white-space: pre-wrap; }

.typing-indicator { display: flex; align-items: center; gap: 6px; padding: 16px 20px; }
.typing-indicator span { width: 8px; height: 8px; border-radius: 50%; background: #7c7cff; animation: bounce 1.4s infinite; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-8px); } }

.input-area { display: flex; gap: 8px; padding: 16px 24px; background: #1a1a2e; border-top: 1px solid #2a2a4a; }
.chat-input { flex: 1; padding: 12px 16px; border: 1px solid #2a2a4a; border-radius: 8px; background: #0f0f1a; color: #e0e0e0; font-size: 14px; outline: none; }
.chat-input:focus { border-color: #7c7cff; }
.chat-input:disabled { opacity: 0.5; }
.send-btn { width: 44px; height: 44px; border: none; border-radius: 8px; background: #7c7cff; color: white; font-size: 18px; cursor: pointer; }
.send-btn:hover:not(:disabled) { background: #6a6aff; }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #1a1a2e; border: 1px solid #2a2a4a; border-radius: 12px; width: 500px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #2a2a4a; }
.modal-header h2 { font-size: 16px; color: #7c7cff; }
.modal-close { background: none; border: 1px solid #2a2a4a; color: #a0a0b0; border-radius: 4px; padding: 2px 8px; cursor: pointer; }
.modal-body { padding: 20px; }
.modal-body h3 { font-size: 14px; color: #7c7cff; margin: 16px 0 8px; }
.modal-body h3:first-child { margin-top: 0; }
.modal-body p { font-size: 13px; color: #a0a0b0; line-height: 1.5; }
.mode-desc { margin-bottom: 12px; padding: 8px; background: #15152a; border-radius: 6px; }
.mode-desc strong { font-size: 13px; color: #b0b0ff; }
.mode-desc p { font-size: 12px; color: #9090a0; margin-top: 4px; }

::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 3px; }
</style>
