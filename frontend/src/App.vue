/* src/App.vue - 多智能体协调系统 Chat UI */
<template>
  <div class="app-container">
    <header class="app-header">
      <h1>多智能体协调系统</h1>
      <span class="badge" :class="statusClass">{{ statusText }}</span>
    </header>

    <div class="main-layout">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <h3>关于</h3>
          <p>基于 LangGraph、Java 21 和 Vue 3 的多智能体协调系统。</p>
        </div>
        <div class="sidebar-section">
          <h3>Agent 状态</h3>
          <div v-if="agentsList.length > 0" class="agents-list">
            <div v-for="agent in agentsList" :key="agent.role" class="agent-item">
              <div class="agent-dot" :class="agent.ready ? 'dot-green' : 'dot-red'"></div>
              <code>{{ agent.role }}</code>
            </div>
          </div>
          <p v-else class="loading-text">加载中...</p>
        </div>
        <div class="sidebar-section">
          <h3>操作模式</h3>
          <div class="mode-selector">
            <button @click="mode='auto'" :class="['mode-btn', mode==='auto'?'active':'']">自动路由</button>
            <button @click="mode='orchestrate'" :class="['mode-btn', mode==='orchestrate'?'active':'']">链式编排</button>
          </div>
          <div v-if="mode==='orchestrate'" class="role-selector">
            <label v-for="r in availableRoles" :key="r" class="role-checkbox">
              <input type="checkbox" :value="r" v-model="orchestrateRoles" />
              {{ r }}
            </label>
          </div>
        </div>
        <div class="sidebar-section">
          <h3>后端状态</h3>
          <p>后端: <span :class="backendOnline ? 'text-green' : 'text-red'">{{ backendOnline ? '在线' : '离线' }}</span></p>
          <p>Agent 就绪: <span class="text-green">{{ readyCount }}/{{ totalCount }}</span></p>
        </div>
      </aside>

      <!-- Chat Area -->
      <main class="chat-area">
        <div class="messages-container" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">🤖</div>
            <h2>你好！我是多智能体系统</h2>
            <p>我有 5 个专家 Agent：通用助手、计算专家、天气专家、搜索专家、总结专家。</p>
            <p>当前模式：<strong>{{ mode === 'auto' ? '自动路由' : '链式编排' }}</strong></p>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
            <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="bubble">
              <div class="sender">{{ msg.role === 'user' ? '你' : (msg.agentName || '助手') }}</div>
              <div class="text">{{ msg.content }}</div>
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
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      messages: [],
      userInput: '',
      loading: false,
      mode: 'auto',
      agentsList: [],
      availableRoles: ['assistant', 'calculator', 'weather', 'searcher', 'summarizer'],
      orchestrateRoles: ['calculator', 'summarizer'],
      backendOnline: false,
      readyCount: 0,
      totalCount: 0,
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
    API_BASE() {
      return 'http://localhost:8080/api'
    }
  },
  methods: {
    async sendMessage() {
      const msg = this.userInput.trim()
      if (!msg || this.loading) return
      this.userInput = ''
      this.messages.push({ role: 'user', content: msg })
      this.loading = true

      try {
        let url = this.API_BASE + '/chat'
        let body = { message: msg }

        if (this.mode === 'orchestrate' && this.orchestrateRoles.length > 0) {
          url = this.API_BASE + '/chat/orchestrate'
          body.roles = this.orchestrateRoles
          this.messages.push({ role: 'assistant', content: '🔗 链式编排: [' + this.orchestrateRoles.join(' → ') + ']', agentName: '系统' })
        }

        const res = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body)
        })
        const data = await res.json()
        this.messages.push({ role: 'assistant', content: data.reply || '错误: ' + (data.error || '未知错误') })
      } catch (e) {
        this.messages.push({ role: 'assistant', content: '错误: 无法连接到后端。' })
      } finally {
        this.loading = false
        this.$nextTick(() => {
          const el = this.$refs.messagesRef
          if (el) el.scrollTop = el.scrollHeight
        })
      }
    },
    async fetchInfo() {
      try {
        const res = await fetch(this.API_BASE + '/agents')
        if (res.ok) {
          const data = await res.json()
          this.agentsList = Object.entries(data).map(([role, info]) => ({
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
.badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.badge-green { background: #1a3a2a; color: #4ade80; }
.badge-yellow { background: #3a3a1a; color: #facc15; }
.badge-red { background: #3a1a1a; color: #f87171; }

.main-layout { display: flex; flex: 1; overflow: hidden; }

.sidebar { width: 260px; background: #15152a; border-right: 1px solid #2a2a4a; padding: 20px; overflow-y: auto; flex-shrink: 0; }
.sidebar-section { margin-bottom: 24px; }
.sidebar-section h3 { font-size: 13px; text-transform: uppercase; letter-spacing: 1px; color: #7c7cff; margin-bottom: 8px; }
.sidebar-section p { font-size: 13px; color: #a0a0b0; line-height: 1.5; }

.agents-list { display: flex; flex-direction: column; gap: 6px; }
.agent-item { display: flex; align-items: center; gap: 8px; }
.agent-dot { width: 8px; height: 8px; border-radius: 50%; }
.dot-green { background: #4ade80; }
.dot-red { background: #f87171; }
.agent-item code { font-size: 12px; color: #b0b0c0; }

.mode-selector { display: flex; gap: 6px; margin-bottom: 8px; }
.mode-btn { flex: 1; padding: 6px; border: 1px solid #2a2a4a; border-radius: 4px; background: #1e1e38; color: #a0a0b0; font-size: 12px; cursor: pointer; }
.mode-btn.active { background: #2a2a5a; border-color: #7c7cff; color: #7c7cff; }
.role-selector { display: flex; flex-direction: column; gap: 4px; padding: 8px; background: #1a1a2e; border-radius: 4px; }
.role-checkbox { font-size: 12px; color: #b0b0c0; display: flex; align-items: center; gap: 6px; }

.loading-text { color: #666; }
.text-green { color: #4ade80; }
.text-red { color: #f87171; }

.chat-area { flex: 1; display: flex; flex-direction: column; background: #0f0f1a; min-width: 0; }
.messages-container { flex: 1; overflow-y: auto; padding: 24px; scroll-behavior: smooth; }

.empty-state { text-align: center; padding: 80px 20px; color: #666; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-state h2 { font-size: 20px; color: #a0a0b0; margin-bottom: 8px; }
.empty-state p { font-size: 14px; color: #606070; }

.message-row { display: flex; gap: 12px; margin-bottom: 20px; max-width: 800px; }
.message-row.user { flex-direction: row-reverse; margin-left: auto; }
.message-row.user .bubble { background: #2a2a5a; border: 1px solid #3a3a7a; }
.message-row.user .avatar { order: -1; }

.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; background: #1e1e38; flex-shrink: 0; }
.bubble { background: #1e1e38; border: 1px solid #2a2a4a; border-radius: 12px; padding: 12px 16px; max-width: 600px; }
.sender { font-size: 11px; font-weight: 600; color: #7c7cff; margin-bottom: 4px; }
.text { font-size: 14px; line-height: 1.6; word-break: break-word; white-space: pre-wrap; }

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

::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 3px; }
</style>
