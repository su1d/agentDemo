<template>
  <div ref="container" class="iso-office-container">
    <div class="office-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">数字孪生办公室</span>
        <span class="toolbar-status" :class="connectionClass">
          <span class="status-dot"></span>
          {{ connectionText }}
        </span>
      </div>
      <div class="toolbar-right">
        <button class="tb-btn" @click="resetCamera" title="重置视角">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        </button>
        <button class="tb-btn" @click="autoRotate = !autoRotate" :class="{ active: autoRotate }" title="自动旋转">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
        </button>
      </div>
    </div>
    <div class="office-legend glass">
      <div v-for="l in legendItems" :key="l.label" class="legend-item">
        <span class="legend-color" :style="{ background: l.color }"></span>
        <span class="legend-label">{{ l.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'

const props = defineProps({
  agents: { type: Array, default: () => [] },
  collaborations: { type: Array, default: () => [] },
  autoRotate: { type: Boolean, default: true },
})

const container = ref(null)
const connectionClass = ref('connecting')
const connectionText = ref('连接中...')
const autoRotate = ref(props.autoRotate)

const legendItems = [
  { color: '#4ade80', label: '在线 / 工作中' },
  { color: '#60a5fa', label: '发言中' },
  { color: '#fb923c', label: '工具调用' },
  { color: '#f87171', label: '离线' },
  { color: '#8b7cf7', label: '数据包' },
]

const AGENT_COLORS = {
  orchestrator: '#6c5ce7',
  calculator: '#60a5fa',
  weather: '#4ade80',
  searcher: '#fb923c',
  summarizer: '#f472b6',
}

const STATUS_COLORS = {
  idle: '#4ade80',
  working: '#4ade80',
  speaking: '#60a5fa',
  tool_calling: '#fb923c',
  offline: '#f87171',
}

let scene, camera, renderer, labelRenderer, controls
let animFrameId = null
let clock = new THREE.Clock()
let agentData = {}  // { role: { group, label, ring, thrusterGlow, orbitRing, homePos, currentStatus } }
let collaborLines = []
let dataPackets = []

const DESK_POSITIONS = [
  { role: 'orchestrator', x: 0, z: 0, label: 'Orchestrator' },
  { role: 'calculator', x: -3, z: 2.5, label: 'Calculator' },
  { role: 'weather', x: 3, z: 2.5, label: 'Weather' },
  { role: 'searcher', x: -3, z: -2.5, label: 'Searcher' },
  { role: 'summarizer', x: 3, z: -2.5, label: 'Summarizer' },
]

// ---- exposed API ----

function sendDataPacket(fromRole, toRole, color, data) {
  if (color === undefined) color = '#8b7cf7'
  if (data === undefined) data = ''
  const fromPos = DESK_POSITIONS.find(p => p.role === fromRole)
  const toPos = DESK_POSITIONS.find(p => p.role === toRole)
  if (!fromPos || !toPos || !scene) return
  const start = new THREE.Vector3(fromPos.x, 0.8, fromPos.z)
  const end = new THREE.Vector3(toPos.x, 0.8, toPos.z)
  const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
  mid.y += 0.8 + Math.random() * 0.8
  const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
  const pColor = new THREE.Color(color)
  const sphere = new THREE.Mesh(new THREE.SphereGeometry(0.08, 8, 8), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 1 }))
  sphere.position.copy(start); scene.add(sphere)
  const glow = new THREE.Mesh(new THREE.SphereGeometry(0.14, 8, 8), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.25, blending: THREE.AdditiveBlending }))
  sphere.add(glow)
  const trailCount = 5; const trail = []
  for (let i = 0; i < trailCount; i++) {
    const t = new THREE.Mesh(new THREE.SphereGeometry(0.03, 4, 4), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.3 }))
    t.visible = false; scene.add(t); trail.push(t)
  }
  dataPackets.push({ mesh: sphere, trail, curve, progress: 0, speed: 0.005 + Math.random() * 0.003, trailCount, data })
}

function clearDataPackets() {
  dataPackets.forEach(p => { scene.remove(p.mesh); p.mesh.geometry.dispose(); p.mesh.material.dispose(); p.trail.forEach(t => { scene.remove(t); t.geometry.dispose(); t.material.dispose() }) })
  dataPackets = []
}

function sendAgentToMeeting(role, onArrive) {
  const d = agentData[role]
  if (!d || !d.group) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  d.meetingAnim = {
    startPos: new THREE.Vector3(desk.x, 0.6, desk.z),
    endPos: new THREE.Vector3(0, 0.6, 5.5),
    progress: 0, speed: 0.008 + Math.random() * 0.004, onArrive
  }
  d.isAtMeeting = true
}

function returnAgentFromMeeting(role) {
  const d = agentData[role]
  if (!d || !d.group) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  d.meetingAnim = {
    startPos: new THREE.Vector3(0, 0.6, 5.5),
    endPos: new THREE.Vector3(desk.x, 0.6, desk.z),
    progress: 0, speed: 0.006, onArrive: null
  }
  d.isAtMeeting = false
}

function showSpeechBubble(role, text, duration) {
  if (duration === undefined) duration = 2500
  if (!scene) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  const div = document.createElement('div'); div.className = 'label-3d label-speech'; div.textContent = text
  const lbl = new CSS2DObject(div); lbl.position.set(desk.x, 2.2 + Math.random() * 0.3, desk.z); scene.add(lbl)
  setTimeout(function() { if (lbl.parent) scene.remove(lbl) }, duration)
}

function highlightConnection(fromRole, toRole, intensity) {
  if (intensity === undefined) intensity = 1.0
  collaborLines.forEach(function(l) {
    const active = l.userData && l.userData.from === fromRole && l.userData.to === toRole
    l.material.opacity = active ? 0.9 * intensity : 0.3
    if (active) l.material.color.set('#a78bfa'); else l.material.color.set('#6c5ce7')
  })
}

function flashAgent(role, color, duration) {
  if (!color) color = '#ffffff'
  if (!duration) duration = 300
  const d = agentData[role]
  if (!d || !d.group) return
  const core = d.group.children.find(c => c.userData.isCore)
  if (!core) return
  const orig = core.material.color.clone()
  const origEm = core.material.emissive.clone()
  core.material.color.set(color)
  core.material.emissive.set(color)
  setTimeout(function() { if (core.material) { core.material.color.copy(orig); core.material.emissive.copy(origEm) } }, duration)
}

function updateAgentStatus(role, status) {
  const d = agentData[role]
  if (!d) return
  d.currentStatus = status
  const color = STATUS_COLORS[status] || STATUS_COLORS.idle
  const targetColor = new THREE.Color(color)

  const core = d.group.children.find(c => c.userData.isCore)
  if (core) {
    core.material.color.lerp(targetColor, 0.3)
    core.material.emissive.lerp(targetColor, 0.3)
  }
  const thruster = d.group.children.find(c => c.userData.isThruster)
  if (thruster) {
    thruster.material.color.lerp(targetColor, 0.3)
  }

  if (d.ring) {
    d.ring.visible = status === 'speaking' || status === 'tool_calling'
    if (d.ring.visible) { d.ring.scale.setScalar(1); d.ring.material.opacity = 0.6 }
  }

  if (status === 'working') {
    d.isWorking = true
  } else {
    d.isWorking = false
  }

  const lbl = d.label
  if (lbl) {
    const statusMap = { idle: '在线', working: '工作中...', speaking: '发言中', tool_calling: '调用工具', offline: '离线' }
    const s = lbl.element.querySelector('.label-status')
    if (s) { s.textContent = statusMap[status] || status; s.style.color = color }
  }
}

function setConnectionStatus(online) {
  connectionClass.value = online ? 'online' : 'offline'
  connectionText.value = online ? '已连接' : '离线'
}

// ---- agent movement between desks ----
function moveAgentTo(role, targetX, targetZ, onArrive) {
  const d = agentData[role]
  if (!d || !d.group) return
  const start = d.group.position.clone()
  d.moveAnim = {
    startPos: start,
    endPos: new THREE.Vector3(targetX, 0.6, targetZ),
    progress: 0, speed: 0.018 + Math.random() * 0.008,
    onArrive, role
  }
}

function returnAgentToDesk(role) {
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  moveAgentTo(role, desk.x, desk.z)
}

function getDeskX(role) {
  const desk = DESK_POSITIONS.find(p => p.role === role)
  return desk ? desk.x : 0
}

function getDeskZ(role) {
  const desk = DESK_POSITIONS.find(p => p.role === role)
  return desk ? desk.z : 0
}

// ---- scene init ----
function initScene() {
  const el = container.value
  if (!el) return

  const w = el.clientWidth
  const h = el.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x080816)
  scene.fog = new THREE.Fog(0x080816, 15, 25)

  camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 40)
  camera.position.set(8, 6, 10)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2
  el.appendChild(renderer.domElement)

  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(w, h)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0'
  labelRenderer.domElement.style.pointerEvents = 'none'
  el.appendChild(labelRenderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 4
  controls.maxDistance = 22
  controls.maxPolarAngle = Math.PI / 2.2
  controls.target.set(0, 0.5, 0)
  controls.update()

  // Lights
  const ambient = new THREE.AmbientLight(0x2a2a4a, 0.5)
  scene.add(ambient)

  const dirLight = new THREE.DirectionalLight(0xffeedd, 1.5)
  dirLight.position.set(10, 14, 8)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.width = 1024
  dirLight.shadow.mapSize.height = 1024
  const d = 12
  dirLight.shadow.camera.left = -d
  dirLight.shadow.camera.right = d
  dirLight.shadow.camera.top = d
  dirLight.shadow.camera.bottom = -d
  dirLight.shadow.camera.near = 1
  dirLight.shadow.camera.far = 25
  scene.add(dirLight)

  const fillLight = new THREE.DirectionalLight(0x8888ff, 0.3)
  fillLight.position.set(-5, 4, -6)
  scene.add(fillLight)

  // Floor - refined
  const floorGeo = new THREE.PlaneGeometry(16, 16)
  const floorMat = new THREE.MeshStandardMaterial({
    color: 0x0e0e20,
    roughness: 0.6,
    metalness: 0.05,
    transparent: true,
    opacity: 0.9,
  })
  const floor = new THREE.Mesh(floorGeo, floorMat)
  floor.rotation.x = -Math.PI / 2
  floor.position.y = -0.01
  floor.receiveShadow = true
  scene.add(floor)

  // Grid - subtle
  const grid = new THREE.GridHelper(16, 16, 0x6c5ce7, 0x2a2a5a)
  grid.position.y = 0.01
  grid.material.transparent = true
  grid.material.opacity = 0.12
  scene.add(grid)

  // Walls
  const wallMat = new THREE.MeshStandardMaterial({
    color: 0x0e0e20,
    roughness: 0.8,
    metalness: 0.0,
    transparent: true,
    opacity: 0.3,
    side: THREE.DoubleSide,
  })
  const wallPositions = [
    { x: 0, z: -6.5, ry: 0 },
    { x: 0, z: 6.5, ry: 0 },
    { x: -6.5, z: 0, ry: Math.PI / 2 },
    { x: 6.5, z: 0, ry: Math.PI / 2 },
  ]
  wallPositions.forEach(function(wp) {
    const wm = new THREE.Mesh(new THREE.BoxGeometry(13, 3, 0.05), wallMat)
    wm.position.set(wp.x, 1.5, wp.z)
    wm.rotation.y = wp.ry
    scene.add(wm)
  })

  // Meeting room
  const tableMat = new THREE.MeshStandardMaterial({ color: 0x1a1a3a, roughness: 0.5, metalness: 0.3 })
  const table = new THREE.Mesh(new THREE.CylinderGeometry(1.0, 1.2, 0.15, 24), tableMat)
  table.position.set(0, 0.075, 5.5)
  table.receiveShadow = true
  table.castShadow = true
  scene.add(table)

  const meetingRing = new THREE.Mesh(
    new THREE.RingGeometry(1.4, 1.7, 32),
    new THREE.MeshBasicMaterial({ color: 0x6c5ce7, transparent: true, opacity: 0.1, side: THREE.DoubleSide })
  )
  meetingRing.rotation.x = -Math.PI / 2
  meetingRing.position.set(0, 0.01, 5.5)
  scene.add(meetingRing)

  // Label style for meeting room
  const labelDiv = document.createElement('div')
  labelDiv.textContent = '会议室'
  labelDiv.style.cssText = 'font-size:11px;font-weight:600;color:rgba(108,92,231,0.5)'
  const meetingLabel = new CSS2DObject(labelDiv)
  meetingLabel.position.set(0, 0.01, 6.2)
  scene.add(meetingLabel)

  // ---- Create desks and agents ----
  DESK_POSITIONS.forEach(function(dp) {
    // Desk - thin glass
    const deskMat = new THREE.MeshStandardMaterial({
      color: 0x14142e,
      roughness: 0.15,
      metalness: 0.7,
      transparent: true,
      opacity: 0.8,
      emissive: new THREE.Color(0x6c5ce7),
      emissiveIntensity: 0.02,
    })
    const desk = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.04, 0.8), deskMat)
    desk.position.set(dp.x, 0.02, dp.z)
    desk.receiveShadow = true
    desk.castShadow = true
    scene.add(desk)

    // Desk border glow
    const borderMat = new THREE.MeshBasicMaterial({
      color: 0x6c5ce7, transparent: true, opacity: 0.08, side: THREE.DoubleSide
    })
    const border = new THREE.Mesh(new THREE.RingGeometry(0.55, 0.6, 4), borderMat)
    border.rotation.x = -Math.PI / 2
    border.position.set(dp.x, 0.03, dp.z)
    scene.add(border)

    // Desk surface glow ring (subtle)
    const surfaceGlow = new THREE.Mesh(
      new THREE.RingGeometry(0.3, 0.5, 24),
      new THREE.MeshBasicMaterial({ color: AGENT_COLORS[dp.role], transparent: true, opacity: 0.04, side: THREE.DoubleSide, blending: THREE.AdditiveBlending })
    )
    surfaceGlow.rotation.x = -Math.PI / 2
    surfaceGlow.position.set(dp.x, 0.025, dp.z)
    scene.add(surfaceGlow)

    // ---- Agent: sleek floating drone ----
    const agColor = new THREE.Color(AGENT_COLORS[dp.role] || '#6c5ce7')
    const group = new THREE.Group()
    group.position.set(dp.x, 0.6, dp.z)
    group.userData.role = dp.role
    group.userData.floatOffset = Math.random() * Math.PI * 2
    group.userData.floatSpeed = 0.5 + Math.random() * 0.3

    // Core body - octahedron (crystal/drone look)
    const coreMat = new THREE.MeshStandardMaterial({
      color: agColor,
      roughness: 0.1,
      metalness: 0.6,
      emissive: agColor,
      emissiveIntensity: 0.15,
    })
    const core = new THREE.Mesh(new THREE.OctahedronGeometry(0.18, 0), coreMat)
    core.position.y = 0
    core.castShadow = true
    core.userData.isCore = true
    group.add(core)

    // Inner core glow (small sphere inside)
    const innerGlow = new THREE.Mesh(
      new THREE.SphereGeometry(0.08, 8, 8),
      new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.3, blending: THREE.AdditiveBlending })
    )
    innerGlow.position.y = 0
    group.add(innerGlow)

    // Orbit ring around the drone
    const orbitRingGeo = new THREE.TorusGeometry(0.3, 0.012, 8, 24)
    const orbitRingMat = new THREE.MeshBasicMaterial({
      color: agColor,
      transparent: true,
      opacity: 0.25,
      blending: THREE.AdditiveBlending,
    })
    const orbitRing = new THREE.Mesh(orbitRingGeo, orbitRingMat)
    orbitRing.rotation.x = Math.PI / 3
    orbitRing.rotation.z = 0.3
    orbitRing.userData.isOrbitRing = true
    group.add(orbitRing)

    // Second orbit ring (tilted different direction)
    const orbitRing2 = new THREE.Mesh(
      new THREE.TorusGeometry(0.35, 0.008, 8, 24),
      new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.15, blending: THREE.AdditiveBlending })
    )
    orbitRing2.rotation.x = -Math.PI / 4
    orbitRing2.rotation.y = 0.5
    orbitRing2.userData.isOrbitRing = true
    group.add(orbitRing2)

    // Bottom thruster glow
    const thrusterMat = new THREE.MeshBasicMaterial({
      color: agColor,
      transparent: true,
      opacity: 0.4,
      blending: THREE.AdditiveBlending,
    })
    const thruster = new THREE.Mesh(new THREE.ConeGeometry(0.06, 0.1, 6), thrusterMat)
    thruster.position.y = -0.2
    thruster.rotation.x = Math.PI
    thruster.userData.isThruster = true
    group.add(thruster)

    // Small thruster glow particles (3 tiny spheres)
    for (let i = 0; i < 3; i++) {
      const tp = new THREE.Mesh(
        new THREE.SphereGeometry(0.015, 4, 4),
        new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.2, blending: THREE.AdditiveBlending })
      )
      tp.position.set((i - 1) * 0.04, -0.28 + Math.random() * 0.02, 0)
      tp.userData.isThrusterParticle = true
      tp.userData.particleIndex = i
      group.add(tp)
    }

    scene.add(group)

    // Save agent data
    const d = {
      group,
      currentStatus: 'idle',
      isWorking: false,
      isAtMeeting: false,
      homePos: new THREE.Vector3(dp.x, 0.6, dp.z),
    }
    agentData[dp.role] = d

    // Status ring (pulsing)
    const sRing = new THREE.Mesh(
      new THREE.RingGeometry(0.3, 0.36, 24),
      new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0, side: THREE.DoubleSide, blending: THREE.AdditiveBlending })
    )
    sRing.rotation.x = -Math.PI / 2
    sRing.position.set(dp.x, 0.02, dp.z)
    scene.add(sRing)
    d.ring = sRing

    // CSS2D label - modern card
    const labelDiv = document.createElement('div')
    labelDiv.className = 'label-3d'
    labelDiv.innerHTML = '<div style="text-align:center;font-size:11px;font-weight:600;color:#e8e8f0;text-shadow:0 0 12px rgba(0,0,0,0.9);background:linear-gradient(135deg,rgba(12,12,28,0.85),rgba(24,18,40,0.85));padding:5px 12px;border-radius:10px;border:1px solid ' + AGENT_COLORS[dp.role] + '55;box-shadow:0 0 16px ' + AGENT_COLORS[dp.role] + '22;">' + dp.label + '<br><span class="label-status" style="font-size:9px;color:#4ade80;">● 在线</span></div>'
    const lbl = new CSS2DObject(labelDiv)
    lbl.position.set(dp.x, 1.3, dp.z)
    scene.add(lbl)
    d.label = lbl
  })

  updateCollaborationLines()
}

function updateCollaborationLines(links) {
  collaborLines.forEach(function(l) { scene.remove(l); l.geometry.dispose(); l.material.dispose() })
  collaborLines = []

  const defaultLinks = links || [
    { from: 'orchestrator', to: 'calculator' },
    { from: 'orchestrator', to: 'weather' },
    { from: 'orchestrator', to: 'searcher' },
    { from: 'orchestrator', to: 'summarizer' },
  ]

  defaultLinks.forEach(function(link) {
    const fromPos = DESK_POSITIONS.find(p => p.role === link.from)
    const toPos = DESK_POSITIONS.find(p => p.role === (link.to || link))
    if (!fromPos || !toPos) return

    const start = new THREE.Vector3(fromPos.x, 0.1, fromPos.z)
    const end = new THREE.Vector3(toPos.x, 0.1, toPos.z)
    const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
    mid.y += 0.15

    const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
    const points = curve.getPoints(20)
    const geo = new THREE.BufferGeometry().setFromPoints(points)
    const mat = new THREE.LineBasicMaterial({
      color: link.color || 0x6c5ce7,
      transparent: true,
      opacity: 0.25,
    })
    const line = new THREE.Line(geo, mat)
    line.userData = { from: link.from, to: link.to || link }
    scene.add(line)
    collaborLines.push(line)
  })
}

function resetCamera() {
  if (!controls) return
  camera.position.set(8, 6, 10)
  controls.target.set(0, 0.5, 0)
  controls.update()
}

// ---- animation loop ----
function animate() {
  animFrameId = requestAnimationFrame(animate)
  const delta = clock.getDelta()
  const time = clock.getElapsedTime()

  // Animate data packets
  for (let i = dataPackets.length - 1; i >= 0; i--) {
    const p = dataPackets[i]
    p.progress += p.speed
    if (p.progress > 1) {
      scene.remove(p.mesh)
      p.mesh.geometry.dispose()
      p.mesh.material.dispose()
      p.trail.forEach(function(t) { scene.remove(t); t.geometry.dispose(); t.material.dispose() })
      dataPackets.splice(i, 1)
      continue
    }
    const point = p.curve.getPoint(p.progress)
    p.mesh.position.copy(point)

    p.trail.forEach(function(t, ti) {
      const tp = p.progress - (ti + 1) * 0.04
      if (tp > 0) {
        const tp2 = p.curve.getPoint(tp)
        t.position.copy(tp2)
        t.visible = true
      } else {
        t.visible = false
      }
    })
  }

  // Animate agents
  Object.keys(agentData).forEach(function(role) {
    const d = agentData[role]
    const grp = d.group
    if (!grp) return

    // --- Floating animation ---
    const floatOffset = grp.userData.floatOffset || 0
    const floatSpeed = grp.userData.floatSpeed || 0.5
    const baseY = grp.userData._baseY || 0.6

    if (!grp.userData._baseY) {
      grp.userData._baseY = d.homePos.y
    }

    // Gentle float up/down
    const floatY = Math.sin(time * floatSpeed + floatOffset) * 0.06

    // Core rotation (slow spin)
    const core = grp.children.find(c => c.userData.isCore)
    if (core) {
      core.rotation.x = Math.sin(time * 0.3 + floatOffset) * 0.1
      core.rotation.y += delta * 0.5
    }

    // Orbit ring rotation
    grp.children.forEach(function(child) {
      if (child.userData.isOrbitRing) {
        child.rotation.y += delta * 0.8
        child.rotation.z += delta * 0.3
      }
      if (child.userData.isThrusterParticle) {
        const idx = child.userData.particleIndex || 0
        child.position.y = -0.28 - Math.sin(time * 2 + idx * 1.5) * 0.03
        child.material.opacity = 0.15 + Math.sin(time * 3 + idx * 2) * 0.1
      }
    })

    // Thruster pulse
    const thruster = grp.children.find(c => c.userData.isThruster)
    if (thruster) {
      thruster.scale.setScalar(1 + Math.sin(time * 2 + floatOffset) * 0.15)
      thruster.material.opacity = 0.3 + Math.sin(time * 2 + floatOffset) * 0.15
    }

    // --- Movement between desks ---
    if (d.moveAnim) {
      d.moveAnim.progress += d.moveAnim.speed * delta * 60
      if (d.moveAnim.progress >= 1) {
        grp.position.copy(d.moveAnim.endPos)
        // Restore floating base
        grp.userData._baseY = d.moveAnim.endPos.y
        const arrived = d.moveAnim.onArrive
        d.moveAnim = null
        if (arrived) arrived()
      } else {
        const t = d.moveAnim.progress
        const smooth = t * t * (3 - 2 * t)
        grp.position.lerpVectors(d.moveAnim.startPos, d.moveAnim.endPos, smooth)
        grp.userData._baseY = grp.position.y
      }
    }

    // --- Meeting animation ---
    if (d.meetingAnim) {
      d.meetingAnim.progress += d.meetingAnim.speed * delta * 60
      if (d.meetingAnim.progress >= 1) {
        grp.position.copy(d.meetingAnim.endPos)
        grp.userData._baseY = d.meetingAnim.endPos.y
        const mArrived = d.meetingAnim.onArrive
        d.meetingAnim = null
        if (mArrived) mArrived()
      } else {
        const t = d.meetingAnim.progress
        const smooth = t * t * (3 - 2 * t)
        grp.position.lerpVectors(d.meetingAnim.startPos, d.meetingAnim.endPos, smooth)
        grp.userData._baseY = grp.position.y
      }
    }

    // Apply floating to Y
    const base = grp.userData._baseY !== undefined ? grp.userData._baseY : 0.6
    grp.position.y = base + floatY

    // --- Working animation: drift toward target briefly ---
    if (d.isWorking && !d.moveAnim && !d.meetingAnim) {
      // Slight extra bob when working
      grp.position.y = base + floatY + Math.sin(time * 3 + floatOffset) * 0.02
    }

    // --- Pulse ring ---
    if (d.ring) {
      if (d.ring.visible) {
        const pulse = 0.5 + Math.sin(time * 2) * 0.3
        d.ring.material.opacity = pulse * 0.6
        d.ring.scale.setScalar(1 + Math.sin(time * 2) * 0.05)
      }
    }

    // Update label position to follow drone
    if (d.label) {
      d.label.position.x = grp.position.x
      d.label.position.z = grp.position.z
      d.label.position.y = grp.position.y + 0.7
    }
  })

  // Auto-rotate
  if (autoRotate.value && controls) {
    const pivot = new THREE.Vector3(0, 0.5, 0)
    const offset = new THREE.Vector3().copy(camera.position).sub(pivot)
    const angle = 0.0015 * delta * 60
    const cos = Math.cos(angle)
    const sin = Math.sin(angle)
    camera.position.x = pivot.x + offset.x * cos - offset.z * sin
    camera.position.z = pivot.z + offset.x * sin + offset.z * cos
    camera.lookAt(pivot)
    controls.target.copy(pivot)
    controls.update()
  }

  controls.update()
  renderer.render(scene, camera)
  labelRenderer.render(scene, camera)
}

function onResize() {
  if (!container.value || !renderer || !labelRenderer) return
  const w = container.value.clientWidth
  const h = container.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  labelRenderer.setSize(w, h)
}

onMounted(async function() {
  await nextTick()
  initScene()
  animate()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(function() {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', onResize)
  if (renderer) { renderer.dispose(); if (renderer.domElement && renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement) }
  if (labelRenderer) { if (labelRenderer.domElement && labelRenderer.domElement.parentNode) labelRenderer.domElement.parentNode.removeChild(labelRenderer.domElement) }
  scene && scene.traverse(function(obj) {
    if (obj.geometry) obj.geometry.dispose()
    if (obj.material) {
      if (Array.isArray(obj.material)) obj.material.forEach(function(m) { m.dispose() })
      else obj.material.dispose()
    }
  })
})

defineExpose({
  sendDataPacket,
  clearDataPackets,
  sendAgentToMeeting,
  returnAgentFromMeeting,
  showSpeechBubble,
  highlightConnection,
  flashAgent,
  updateAgentStatus,
  setConnectionStatus,
  updateCollaborationLines,
  resetCamera,
  moveAgentTo,
  returnAgentToDesk,
  getDeskX,
  getDeskZ,
})
</script>

<style scoped>
.iso-office-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--bg-deep, #080816);
}

.office-toolbar {
  position: absolute;
  top: 10px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  pointer-events: none;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #e8e8f0);
  text-shadow: 0 0 12px rgba(0,0,0,0.5);
}

.toolbar-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #68687d;
  background: rgba(13,13,26,0.7);
  padding: 2px 10px 2px 8px;
  border-radius: 12px;
  border: 1px solid var(--border-color, rgba(108,92,231,0.2));
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #68687d;
}

.toolbar-status.online .status-dot {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74,222,128,0.5);
}

.toolbar-status.online {
  color: #4ade80;
  border-color: rgba(74,222,128,0.3);
}

.toolbar-status.offline .status-dot {
  background: #f87171;
}

.toolbar-status.offline {
  color: #f87171;
  border-color: rgba(248,113,113,0.3);
}

.toolbar-right {
  display: flex;
  gap: 4px;
  pointer-events: auto;
}

.tb-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid rgba(108,92,231,0.2);
  background: rgba(13,13,26,0.75);
  color: var(--text-secondary, #9898b0);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tb-btn:hover {
  background: rgba(108,92,231,0.2);
  color: #e8e8f0;
  border-color: rgba(108,92,231,0.3);
}

.tb-btn.active {
  background: rgba(108,92,231,0.25);
  color: #6c5ce7;
  border-color: rgba(108,92,231,0.5);
}

.office-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  gap: 12px;
  z-index: 10;
  padding: 6px 12px;
  border-radius: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-color {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.legend-label {
  font-size: 10px;
  color: var(--text-secondary, #9898b0);
}

:deep(.label-speech) {
  background: rgba(20,20,42,0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(108,92,231,0.2);
  border-radius: 10px;
  padding: 5px 12px;
  font-size: 12px;
  color: #e8e8f0;
  max-width: 200px;
  pointer-events: none;
  white-space: nowrap;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
}

:deep(.label-3d) {
  pointer-events: none;
  user-select: none;
}
</style>



