<template>
  <div class="dashboard">
    <div class="dash-layout">
      <div class="office-panel glass-strong">
        <IsometricOffice
          ref="officeRef"
          :agents="agentList"
          :collaborations="activeCollaborations"
        />
      </div>

      <div class="info-panel">
        <header class="dash-header">
          <h1>仪表盘</h1>
          <p class="dash-subtitle">数字孪生办公室 · 实时状态</p>
          <div class="dash-badges">
            <span class="badge badge-online" v-if="backendOnline">
              <span class="badge-dot"></span> 系统在线
            </span>
            <span class="badge badge-off" v-else>
              <span class="badge-dot"></span> 离线
            </span>
          </div>
        </header>

        <div class="stats-grid">
          <div class="stat-card glass card-hover">
            <div class="stat-icon-wrap purple">
              <BrainCircuit :size="20" />
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ agentsReady }}/{{ agentsTotal }}</span>
              <span class="stat-label">Agent 在线</span>
            </div>
            <div class="stat-trend up">100%</div>
          </div>
          <div class="stat-card glass card-hover">
            <div class="stat-icon-wrap orange">
              <Zap :size="20" />
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ backendOnline ? '已连接' : '离线' }}</span>
              <span class="stat-label">后端状态</span>
            </div>
            <div class="stat-trend" :class="backendOnline ? 'up' : 'down'">{{ backendOnline ? '正常' : '异常' }}</div>
          </div>
          <div class="stat-card glass card-hover">
            <div class="stat-icon-wrap green">
              <Activity :size="20" />
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ totalRuns }}</span>
              <span class="stat-label">今日执行</span>
            </div>
            <div class="stat-trend up">+{{ scenarioIndex }}</div>
          </div>
          <div class="stat-card glass card-hover">
            <div class="stat-icon-wrap blue">
              <Clock :size="20" />
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ uptime }}</span>
              <span class="stat-label">运行时长</span>
            </div>
            <div class="stat-trend up">正常</div>
          </div>
        </div>

        <div class="panels-row">
          <div class="panel glass card-hover">
            <div class="panel-header">
              <h2><BrainCircuit :size="14" /> Agent 状态</h2>
              <span class="panel-count">{{ agents.filter(a => a.ready).length }}/{{ agents.length }}</span>
            </div>
            <div class="panel-body">
              <div v-for="a in agents" :key="a.role" class="agent-row">
                <span class="agent-avatar" :style="{ background: agentColors[a.role] || '#6c5ce7' }">
                  {{ roleMap[a.role] ? roleMap[a.role].charAt(0) : '?' }}
                </span>
                <div class="agent-meta">
                  <span class="agent-role">{{ roleMap[a.role] || a.role }}</span>
                  <span class="agent-role-en">{{ a.role }}</span>
                </div>
                <span class="agent-dot" :class="a.ready ? 'dot-on' : 'dot-off'"></span>
                <span class="agent-status" :class="a.ready ? 'text-green' : 'text-red'">{{ a.ready ? '在线' : '离线' }}</span>
              </div>
            </div>
          </div>

          <div class="panel glass card-hover">
            <div class="panel-header">
              <h2><Zap :size="14" /> 实时动态</h2>
              <span class="panel-count red-dot"></span>
            </div>
            <div class="panel-body events-body">
              <div v-for="(evt, i) in events" :key="i" class="event-row">
                <span class="event-dot" :style="{ background: eventColor(evt.type) }"></span>
                <span class="event-text">{{ evt.text }}</span>
              </div>
              <div v-if="events.length === 0" class="event-empty">等待活动...</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from "vue"
import { BrainCircuit, Zap, Activity, Clock } from "@lucide/vue"
import { useAppStore } from "../../stores/app"
import IsometricOffice from "../../components/IsometricOffice.vue"

const store = useAppStore()
const officeRef = ref(null)
const agentsReady = ref(5)
const agentsTotal = ref(5)
const backendOnline = ref(true)
const totalRuns = ref(0)
const uptime = ref('00:00:00')
const startTime = ref(Date.now())

const agents = ref([
  { role: "orchestrator", ready: true },
  { role: "calculator", ready: true },
  { role: "weather", ready: true },
  { role: "searcher", ready: true },
  { role: "summarizer", ready: true },
])

const agentList = computed(() => agents.value)

const activeCollaborations = ref([])

const events = ref([])

const roleMap = {
  orchestrator: "协调器",
  calculator: "计算器",
  weather: "天气",
  searcher: "搜索器",
  summarizer: "摘要器"
}

const agentColors = {
  orchestrator: "#6c5ce7",
  calculator: "#60a5fa",
  weather: "#4ade80",
  searcher: "#fb923c",
  summarizer: "#f472b6"
}

const scenarios = [
  {
    title: "天气查询",
    steps: [
      { action: "working", role: "orchestrator", duration: 1000, speech: "收到天气查询请求" },
      { action: "packet", from: "orchestrator", to: "weather", duration: 800, data: "query:weather" },
      { action: "working", role: "weather", duration: 1500, speech: "正在查询气象数据..." },
      { action: "speaking", role: "weather", duration: 600, speech: "数据已获取" },
      { action: "packet", from: "weather", to: "orchestrator", duration: 800, data: "result:weather" },
      { action: "working", role: "orchestrator", duration: 800, speech: "正在处理" },
      { action: "speaking", role: "orchestrator", duration: 1500, speech: "今日气温22-28°C 多云转晴" },
    ]
  },
  {
    title: "计算任务",
    steps: [
      { action: "working", role: "orchestrator", duration: 800, speech: "收到计算请求" },
      { action: "packet", from: "orchestrator", to: "calculator", duration: 600, data: "calc:complex_math" },
      { action: "working", role: "calculator", duration: 1800, speech: "正在计算复杂公式..." },
      { action: "speaking", role: "calculator", duration: 600, speech: "结果已计算完成" },
      { action: "packet", from: "calculator", to: "orchestrator", duration: 600, data: "result:calc" },
      { action: "working", role: "orchestrator", duration: 600, speech: "接收计算结果" },
    ]
  },
  {
    title: "文档检索",
    steps: [
      { action: "working", role: "orchestrator", duration: 500, speech: "开始文档搜索" },
      { action: "packet", from: "orchestrator", to: "searcher", duration: 800, data: "search:query" },
      { action: "working", role: "searcher", duration: 2000, speech: "正在搜索相关文档..." },
      { action: "packet", from: "searcher", to: "orchestrator", duration: 700, data: "result:docs" },
      { action: "packet", from: "orchestrator", to: "summarizer", duration: 600, data: "summarize" },
      { action: "working", role: "summarizer", duration: 1800, speech: "正在生成文档摘要..." },
      { action: "packet", from: "summarizer", to: "orchestrator", duration: 600, data: "result:summary" },
      { action: "speaking", role: "orchestrator", duration: 1500, speech: "完成！5个文档已汇总" },
    ]
  },
  {
    title: "Agent串门互动",
    steps: [
      { action: "speaking", role: "orchestrator", duration: 400, speech: "calculator 来我工位一下" },
      { action: "visit", from: "calculator", to: "orchestrator", duration: 2000, speech: "收到，这就来！" },
      { action: "speaking", role: "calculator", duration: 1200, speech: "数据报表已生成，请您过目" },
      { action: "speaking", role: "orchestrator", duration: 800, speech: "好的，我再看看" },
      { action: "return_to_desk", roles: ["calculator"], duration: 1500 },
      { action: "speaking", role: "weather", duration: 300, speech: "咦？calculator 去找谁了？我也去看看" },
      { action: "visit", from: "weather", to: "orchestrator", duration: 1800, speech: "有什么需要帮忙的吗" },
      { action: "speaking", role: "orchestrator", duration: 600, speech: "你们来得正好，一起讨论" },
      { action: "return_to_desk", roles: ["weather"], duration: 1300, speech: "好的，回工位继续" },
    ]
  },
]

let activeScenario = null
let scenarioTimeout = null
let currentStepIndex = 0
let sceneReady = false
let simInterval = null
let scenarioIndex = 0
let uptimeInterval = null

async function executeScenario(scenario) {
  if (!officeRef.value) return
  activeScenario = scenario
  currentStepIndex = 0
  totalRuns.value++

  const msg = "开始: " + scenario.title
  events.value = [{ type: "collab", text: msg }].concat(events.value).slice(0, 20)

  await executeNextStep()
}

async function executeNextStep() {
  if (!activeScenario || !officeRef.value) return

  if (currentStepIndex >= activeScenario.steps.length) {
    activeScenario = null
    return
  }

  const step = activeScenario.steps[currentStepIndex]
  const office = officeRef.value

  if (step.speech) {
    const tm = { speaking: "speak", packet: "collab", working: "collab", meeting: "collab", collab: "collab", return: "collab", visit: "collab", return_to_desk: "collab" }
    events.value = [{ type: tm[step.action] || "collab", text: step.speech }].concat(events.value).slice(0, 20)
  }

  switch (step.action) {
    case "working":
      office.updateAgentStatus(step.role, "working")
      office.showSpeechBubble(step.role, step.speech || "工作中...")
      office.flashAgent(step.role)
      await delay(step.duration)
      office.updateAgentStatus(step.role, "idle")
      break

    case "speaking":
      office.updateAgentStatus(step.role, "speaking")
      office.showSpeechBubble(step.role, step.speech || "")
      await delay(step.duration)
      office.updateAgentStatus(step.role, "idle")
      break

    case "packet":
      office.sendDataPacket(step.from, step.to, undefined, step.data || "")
      office.flashAgent(step.from, "#8b7cf7", 200)
      await delay(step.duration)
      office.flashAgent(step.to, "#8b7cf7", 200)
      office.showSpeechBubble(step.to, "收到 " + (step.data || "数据"), 1200)
      break

    case "collab":
      office.highlightConnection(step.from, step.to, 1)
      office.sendDataPacket(step.from, step.to, "#a78bfa", step.data || "")
      await delay(step.duration)
      office.highlightConnection(step.from, step.to, 0)
      break

    case "meeting":
      step.roles.forEach(function(r, i) {
        setTimeout(function() {
          if (office) office.sendAgentToMeeting(r, function() { office.showSpeechBubble(r, "已到达会议室", 1000) })
        }, i * 400)
      })
      await delay(step.duration)
      break

    case "return":
      step.roles.forEach(function(r, i) {
        setTimeout(function() {
          if (office) office.returnAgentFromMeeting(r)
        }, i * 300)
      })
      await delay(step.duration)
      break

    case "visit":
      office.updateAgentStatus(step.from, "working")
      office.showSpeechBubble(step.from, step.speech || "去找 " + step.to, 1500)
      // Look up the target desk position
      office.moveAgentTo(step.from, office.getDeskX(step.to), office.getDeskZ(step.to), function() {
        office.showSpeechBubble(step.from, "到了！", 1200)
        office.updateAgentStatus(step.from, "idle")
      })
      await delay(step.duration)
      break

    case "return_to_desk":
      step.roles.forEach(function(r, i) {
        setTimeout(function() {
          if (office) {
            office.returnAgentToDesk(r)
            if (step.speech) office.showSpeechBubble(r, step.speech, 1200)
          }
        }, i * 400)
      })
      await delay(step.duration)
      break
  }

  currentStepIndex++
  scenarioTimeout = setTimeout(executeNextStep, 500)
}

function delay(ms) { return new Promise(function(resolve) { setTimeout(resolve, ms) }) }

function initDemoAgents() {
  if (!officeRef.value) return
  officeRef.value.setConnectionStatus(true)
  agents.value.forEach(function(a) { officeRef.value.updateAgentStatus(a.role, "idle") })
  updateCollaborations()
  sceneReady = true
}

function updateCollaborations() {
  if (!officeRef.value) return
  const online = agents.value.filter(function(a) { return a.ready }).map(function(a) { return a.role })
  const links = []
  if (online.length >= 2) {
    online.forEach(function(r) {
      if (r !== "orchestrator") {
        links.push({ from: r, to: "orchestrator", active: false, color: "#6c5ce7" })
        links.push({ from: "orchestrator", to: r, active: false, color: "#6c5ce7" })
      }
    })
  }
  officeRef.value.updateCollaborationLines(links)
}

onMounted(async function() {
  await delay(500)
  initDemoAgents()
  await delay(2000)
  updateCollaborations()

  await executeScenario(scenarios[0])

  simInterval = setInterval(function() {
    if (!activeScenario && sceneReady && officeRef.value) {
      scenarioIndex = (scenarioIndex + 1) % scenarios.length
      executeScenario(scenarios[scenarioIndex])
    }
  }, 4000)

  uptimeInterval = setInterval(function() {
    const elapsed = Math.floor((Date.now() - startTime.value) / 1000)
    const h = String(Math.floor(elapsed / 3600)).padStart(2, '0')
    const m = String(Math.floor((elapsed % 3600) / 60)).padStart(2, '0')
    const s = String(elapsed % 60).padStart(2, '0')
    uptime.value = `${h}:${m}:${s}`
  }, 1000)
})

onBeforeUnmount(function() {
  if (simInterval) clearInterval(simInterval)
  if (scenarioTimeout) clearTimeout(scenarioTimeout)
  if (uptimeInterval) clearInterval(uptimeInterval)
})

function eventColor(type) {
  const m = { collab: "#6c5ce7", speak: "#60a5fa", tool: "#fb923c" }
  return m[type] || "#68688a"
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.dash-layout {
  display: flex;
  gap: 20px;
  height: 100%;
  min-height: calc(100vh - 80px);
}

.office-panel {
  flex: 3;
  min-width: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
}

.info-panel {
  flex: 2;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding-right: 4px;
}

.dash-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.dash-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.dash-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  width: 100%;
  margin-top: -4px;
}

.dash-badges {
  margin-left: auto;
}

.badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}

.badge-online {
  background: var(--green-bg);
  color: var(--green);
  border: 1px solid rgba(74,222,128,0.2);
}

.badge-off {
  background: var(--red-bg);
  color: var(--red);
  border: 1px solid rgba(248,113,113,0.2);
}

.badge-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-card {
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap.purple { background: rgba(108,92,231,0.12); color: #6c5ce7; }
.stat-icon-wrap.orange { background: rgba(251,146,60,0.12); color: #fb923c; }
.stat-icon-wrap.green { background: rgba(74,222,128,0.12); color: #4ade80; }
.stat-icon-wrap.blue { background: rgba(96,165,250,0.12); color: #60a5fa; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 1px;
}

.stat-trend {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.stat-trend.up { background: var(--green-bg); color: var(--green); }
.stat-trend.down { background: var(--red-bg); color: var(--red); }

.panels-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.panel {
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h2 {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.panel-count {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.red-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--red);
  animation: pulseGlow 2s ease infinite;
}

.panel-body {
  padding: 8px 16px;
  overflow-y: auto;
  flex: 1;
}

.events-body {
  max-height: 180px;
}

.agent-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(108,92,231,0.06);
}

.agent-row:last-child {
  border-bottom: none;
}

.agent-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.agent-meta {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.agent-role {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
}

.agent-role-en {
  font-size: 10px;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}

.agent-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-on { background: var(--green); box-shadow: 0 0 6px rgba(74,222,128,0.5); }
.dot-off { background: var(--red); }

.text-green { color: var(--green); font-size: 11px; }
.text-red { color: var(--red); font-size: 11px; }

.event-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid rgba(108,92,231,0.04);
  font-size: 11px;
}

.event-row:last-child { border-bottom: none; }

.event-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.event-text {
  color: var(--text-secondary);
  line-height: 1.4;
}

.event-empty {
  text-align: center;
  padding: 20px;
  color: var(--text-dim);
  font-size: 12px;
}

@keyframes pulseGlow {
  0%, 100% { opacity: 0.5; box-shadow: 0 0 4px rgba(248,113,113,0.3); }
  50% { opacity: 1; box-shadow: 0 0 8px rgba(248,113,113,0.6); }
}

@media (max-width: 900px) {
  .dash-layout { flex-direction: column; }
  .office-panel { flex: none; height: 50vh; }
}
</style>

