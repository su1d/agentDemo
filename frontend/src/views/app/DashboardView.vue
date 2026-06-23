<template>
  <div class="dashboard">
    <div class="dash-layout">
      <!-- Left: 3D Office Scene -->
      <div class="office-panel">
        <IsometricOffice
          ref="officeRef"
          :agents="agentList"
          :collaborations="activeCollaborations"
        />
      </div>

      <!-- Right: Stats & Info -->
      <div class="info-panel">
        <header class="dash-header">
          <h1>仪表盘</h1>
          <p class="dash-subtitle">AgentForge 数字孪生办公室</p>
        </header>

        <div class="stats-grid">
          <div class="stat-card">
            <BrainCircuit :size="22" class="stat-icon purple" />
            <div class="stat-info">
              <span class="stat-value">{{ agentsReady }}/{{ agentsTotal }}</span>
              <span class="stat-label">Agent 在线</span>
            </div>
          </div>
          <div class="stat-card">
            <Zap :size="22" class="stat-icon orange" />
            <div class="stat-info">
              <span class="stat-value">{{ backendOnline ? '已连接' : '离线' }}</span>
              <span class="stat-label">后端状态</span>
            </div>
          </div>
        </div>

        <div class="panels-row">
          <div class="panel panel-agents">
            <div class="panel-header">
              <h2><BrainCircuit :size="16" /> Agent 状态</h2>
            </div>
            <div class="panel-body">
              <div v-for="a in agentList" :key="a.role" class="agent-row">
                <span class="agent-dot" :class="a.ready ? 'dot-on' : 'dot-off'"></span>
                <span class="agent-role">{{ roleMap[a.role] || a.role }}</span>
                <span class="agent-status" :class="a.ready ? 'text-green' : 'text-red'">{{ a.ready ? '在线' : '离线' }}</span>
              </div>
            </div>
          </div>

          <div class="panel panel-events">
            <div class="panel-header">
              <h2><Zap :size="16" /> 实时动态</h2>
            </div>
            <div class="panel-body">
              <div v-for="(evt, i) in events" :key="i" class="event-row">
                <span class="event-dot" :style="{ background: eventColor(evt.type) }"></span>
                <span class="event-text">{{ evt.text }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue"
import { BrainCircuit, Zap } from "@lucide/vue"
import { useAppStore } from "../../stores/app"
import IsometricOffice from "../../components/IsometricOffice.vue"

const store = useAppStore()
const officeRef = ref(null)
const agentsReady = ref(5)
const agentsTotal = ref(5)
const backendOnline = ref(true)
const agentList = ref([
  { role: "orchestrator", ready: true },
  { role: "calculator", ready: true },
  { role: "weather", ready: true },
  { role: "searcher", ready: true },
  { role: "summarizer", ready: true },
])
const events = ref([])

const roleMap = {
  orchestrator: "协调器",
  calculator: "计算器",
  weather: "天气",
  searcher: "搜索器",
  summarizer: "摘要器"
}

const scenarios = [
  {
    title: "天气查询",
    steps: [
      { action: "working", role: "orchestrator", duration: 1000, speech: "收到天气查询请求" },
      { action: "packet", from: "orchestrator", to: "weather", duration: 800, data: "query:weather" },
      { action: "working", role: "weather", duration: 1500, speech: "????????..." },
      { action: "speaking", role: "weather", duration: 600, speech: "数据已获取" },
      { action: "packet", from: "weather", to: "orchestrator", duration: 800, data: "result:weather" },
      { action: "working", role: "orchestrator", duration: 800, speech: "正在处理" },
      { action: "speaking", role: "orchestrator", duration: 1500, speech: "今日气温22-28°C 多云转晴" },
    ]
  },
  {
    title: "天气查询",
    steps: [
      { action: "working", role: "orchestrator", duration: 800, speech: "??????" },
      { action: "packet", from: "orchestrator", to: "calculator", duration: 600, data: "calc:complex_math" },
      { action: "working", role: "calculator", duration: 1800, speech: "??????..." },
      { action: "speaking", role: "calculator", duration: 600, speech: "???? ?????" },
      { action: "packet", from: "calculator", to: "orchestrator", duration: 600, data: "result:calc" },
      { action: "working", role: "orchestrator", duration: 600, speech: "??????" },
    ]
  },
  {
    title: "???????",
    steps: [
      { action: "working", role: "orchestrator", duration: 500, speech: "??????" },
      { action: "packet", from: "orchestrator", to: "searcher", duration: 800, data: "search:query" },
      { action: "working", role: "searcher", duration: 2000, speech: "??????..." },
      { action: "packet", from: "searcher", to: "orchestrator", duration: 700, data: "result:docs" },
      { action: "packet", from: "orchestrator", to: "summarizer", duration: 600, data: "summarize" },
      { action: "working", role: "summarizer", duration: 1800, speech: "????????..." },
      { action: "packet", from: "summarizer", to: "orchestrator", duration: 600, data: "result:summary" },
      { action: "speaking", role: "orchestrator", duration: 1500, speech: "完成！5个文档已汇总" },
    ]
  },
  {
    title: "?Agent????",
    steps: [
      { action: "speaking", role: "orchestrator", duration: 500, speech: "??????" },
      { action: "meeting", roles: ["calculator", "weather", "searcher"], duration: 3000 },
      { action: "working", role: "orchestrator", duration: 800, speech: "??????" },
      { action: "speaking", role: "calculator", duration: 1000, speech: "?????????" },
      { action: "speaking", role: "weather", duration: 800, speech: "???????" },
      { action: "speaking", role: "searcher", duration: 700, speech: "???????" },
      { action: "return", roles: ["calculator", "weather", "searcher"], duration: 2000 },
      { action: "speaking", role: "orchestrator", duration: 1000, speech: "会议结束，继续工作" },
    ]
  },
]

let activeScenario = null
let scenarioTimeout = null
let currentStepIndex = 0
let sceneReady = false
let simInterval = null
let scenarioIndex = -1

async function executeScenario(scenario) {
  if (!officeRef.value) return
  activeScenario = scenario
  currentStepIndex = 0

  const msg = "开始执行: " + scenario.title
  events.value = [{ type: "collab", text: msg }].concat(events.value).slice(0, 12)

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
    const tm = { speaking: "speak", packet: "collab", working: "collab", meeting: "collab", collab: "collab", return: "collab" }
    events.value = [{ type: tm[step.action] || "collab", text: step.speech }].concat(events.value).slice(0, 12)
  }

  switch (step.action) {
    case "working":
      office.updateAgentStatus(step.role, "working")
      office.showSpeechBubble(step.role, step.speech || "???...")
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
          if (office) office.sendAgentToMeeting(r, function() { office.showSpeechBubble(r, "?????", 1000) })
        }, i * 400)
      })
      await delay(step.duration)
      break

    case "return":
      step.roles.forEach(function(r, i) {
        setTimeout(function() {
          if (office) office.returnAgentFromMeeting(r); office.showSpeechBubble(r, "回到工位", 1000)
        }, i * 300)
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
  agentList.value.forEach(function(a) { officeRef.value.updateAgentStatus(a.role, "idle") })
  updateCollaborations()
  sceneReady = true
}

function updateCollaborations() {
  if (!officeRef.value) return
  const online = agentList.value.filter(function(a) { return a.ready }).map(function(a) { return a.role })
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

  // Start first scenario
  scenarioIndex = 0
  await executeScenario(scenarios[0])

  // Schedule more scenarios
  simInterval = setInterval(function() {
    if (!activeScenario && sceneReady && officeRef.value) {
      scenarioIndex = (scenarioIndex + 1) % scenarios.length
      executeScenario(scenarios[scenarioIndex])
    }
  }, 4000)
})

onBeforeUnmount(function() {
  if (simInterval) clearInterval(simInterval)
  if (scenarioTimeout) clearTimeout(scenarioTimeout)
})

function eventColor(type) {
  const m = { collab: "#6c5ce7", speak: "#60a5fa", tool: "#fb923c" }
  return m[type] || "#68687d"
}
</script>

<style scoped>
.dashboard { padding: 16px; height: 100%; box-sizing: border-box; }
.dash-layout { display: flex; gap: 16px; height: 100%; min-height: calc(100vh - 84px); }
.office-panel { flex: 3; min-width: 0; border-radius: 12px; overflow: hidden; background: #0d0d1a; border: 1px solid rgba(108,92,231,0.12); }
.info-panel { flex: 2; min-width: 0; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; padding-right: 4px; }
.dash-header { margin-bottom: 2px; }
.dash-header h1 { font-size: 22px; font-weight: 700; color: #e8e8f0; }
.dash-subtitle { font-size: 12px; color: #68687d; margin-top: 2px; }
.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat-card { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.stat-icon { flex-shrink: 0; }
.stat-icon.purple { color: #6c5ce7; }
.stat-icon.orange { color: #fb923c; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 20px; font-weight: 700; color: #e8e8f0; }
.stat-label { font-size: 11px; color: #68687d; margin-top: 1px; }
.panels-row { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.panel { background: #141428; border: 1px solid rgba(108,92,231,0.15); border-radius: 10px; overflow: hidden; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid rgba(108,92,231,0.15); display: flex; align-items: center; justify-content: space-between; }
.panel-header h2 { font-size: 13px; font-weight: 600; color: #e8e8f0; display: flex; align-items: center; gap: 6px; }
.panel-body { padding: 8px 16px; }
.agent-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid rgba(108,92,231,0.08); }
.agent-row:last-child { border-bottom: none; }
.agent-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dot-on { background: #4ade80; box-shadow: 0 0 5px rgba(74,222,128,0.4); }
.dot-off { background: #f87171; }
.agent-role { flex: 1; font-size: 12px; color: #9898b0; font-weight: 500; }
.text-green { color: #4ade80; font-size: 11px; }
.text-red { color: #f87171; font-size: 11px; }

.event-row { display: flex; align-items: flex-start; gap: 8px; padding: 5px 0; border-bottom: 1px solid rgba(108,92,231,0.06); font-size: 11px; }
.event-row:last-child { border-bottom: none; }
.event-dot { width: 5px; height: 5px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; }
.event-text { color: #9898b0; line-height: 1.4; }

@media (max-width: 900px) { .dash-layout { flex-direction: column; } .office-panel { flex: none; height: 50vh; } }
</style>
