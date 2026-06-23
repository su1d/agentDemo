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
    <div class="office-legend">
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
let agentMeshes = {}
let agentLabels = {}
let agentRings = {}
let collaborLines = []
let particleSystem = null

let dataPackets = []

const DESK_POSITIONS = [
  { role: 'orchestrator', x: 0, z: 0, label: 'Orchestrator' },
  { role: 'calculator', x: -3, z: 2.5, label: 'Calculator' },
  { role: 'weather', x: 3, z: 2.5, label: 'Weather' },
  { role: 'searcher', x: -3, z: -2.5, label: 'Searcher' },
  { role: 'summarizer', x: 3, z: -2.5, label: 'Summarizer' },
]

const MEETING_ROOM_POS = { x: 0, z: 5.5 }

function sendDataPacket(fromRole, toRole, color, data) {
  if (color === undefined) color = '#8b7cf7'
  if (data === undefined) data = ''
  const fromPos = DESK_POSITIONS.find(p => p.role === fromRole)
  const toPos = DESK_POSITIONS.find(p => p.role === toRole)
  if (!fromPos || !toPos || !scene) return
  const start = new THREE.Vector3(fromPos.x, 0.8, fromPos.z)
  const end = new THREE.Vector3(toPos.x, 0.8, toPos.z)
  const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
  mid.y += 0.6 + Math.random() * 0.6
  const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
  const pColor = new THREE.Color(color)
  const sphere = new THREE.Mesh(new THREE.SphereGeometry(0.09, 8, 8), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 1 }))
  sphere.position.copy(start); scene.add(sphere)
  const glow = new THREE.Mesh(new THREE.SphereGeometry(0.16, 8, 8), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.2, blending: THREE.AdditiveBlending }))
  sphere.add(glow)
  const trailCount = 6; const trail = []
  for (let i = 0; i < trailCount; i++) {
    const t = new THREE.Mesh(new THREE.SphereGeometry(0.035, 4, 4), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.3 }))
    t.visible = false; scene.add(t); trail.push(t)
  }
  dataPackets.push({ mesh: sphere, trail, curve, progress: 0, speed: 0.006 + Math.random() * 0.003, trailCount, data })
}

function clearDataPackets() {
  dataPackets.forEach(p => { scene.remove(p.mesh); p.mesh.geometry.dispose(); p.mesh.material.dispose(); p.trail.forEach(t => { scene.remove(t); t.geometry.dispose(); t.material.dispose() }) })
  dataPackets = []
}

function sendAgentToMeeting(role, onArrive) {
  if (!scene) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return; const mesh = agentMeshes[role]
  if (!mesh) return
  mesh.userData.meetingAnim = { startPos: new THREE.Vector3(desk.x, 0.25, desk.z), endPos: new THREE.Vector3(0, 0.25, 5.5), progress: 0, speed: 0.008 + Math.random() * 0.004, onArrive }
  mesh.userData.isAtMeeting = true
}

function returnAgentFromMeeting(role) {
  if (!scene) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return; const mesh = agentMeshes[role]
  if (!mesh) return
  mesh.userData.meetingAnim = { startPos: new THREE.Vector3(0, 0.25, 5.5), endPos: new THREE.Vector3(desk.x, 0.25, desk.z), progress: 0, speed: 0.006, onArrive: null }
  mesh.userData.isAtMeeting = false
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
  const mesh = agentMeshes[role]
  if (!mesh) return
  const orig = mesh.material.color.clone(); const origEm = mesh.material.emissive.clone()
  mesh.material.color.set(color); mesh.material.emissive.set(color)
  setTimeout(function() { if (mesh.material) { mesh.material.color.copy(orig); mesh.material.emissive.copy(origEm) } }, duration)
}

function updateAgentStatus(role, status, detail) {
  if (!scene) return
  const mesh = agentMeshes[role]
  if (!mesh) return
  const color = STATUS_COLORS[status] || STATUS_COLORS.idle
  const targetColor = new THREE.Color(color)
  mesh.material.color.lerp(targetColor, 0.3)

  const ring = agentRings[role]
  if (ring) {
    ring.visible = status === 'speaking' || status === 'tool_calling'
    if (ring.visible) { ring.scale.setScalar(1); ring.material.opacity = 0.6 }
  }

  if (status === 'working') mesh.userData.isWorking = true
  else { mesh.userData.isWorking = false; mesh.position.y = 0.25 }

  const lbl = agentLabels[role]
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

function initScene() {
  const el = container.value
  if (!el) return

  const w = el.clientWidth
  const h = el.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0d0d1a)
  scene.fog = new THREE.Fog(0x0d0d1a, 15, 25)

  camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 40)
  camera.position.set(8, 6, 10)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
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
  const ambient = new THREE.AmbientLight(0x2a2a4a, 0.6)
  scene.add(ambient)

  const dirLight = new THREE.DirectionalLight(0xffeedd, 1.8)
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

  const fillLight = new THREE.DirectionalLight(0x8888ff, 0.4)
  fillLight.position.set(-5, 4, -6)
  scene.add(fillLight)

  // Floor
  const floorGeo = new THREE.PlaneGeometry(16, 16)
  const floorMat = new THREE.MeshStandardMaterial({
    color: 0x181830,
    roughness: 0.7,
    metalness: 0.1,
    transparent: true,
    opacity: 0.95,
  })
  const floor = new THREE.Mesh(floorGeo, floorMat)
  floor.rotation.x = -Math.PI / 2
  floor.position.y = -0.01
  floor.receiveShadow = true
  scene.add(floor)

  // Grid
  const grid = new THREE.GridHelper(16, 16, 0x6c5ce7, 0x2a2a5a)
  grid.position.y = 0.01
  grid.material.transparent = true
  grid.material.opacity = 0.15
  scene.add(grid)

  // Walls (decorative)
  const wallMat = new THREE.MeshStandardMaterial({
    color: 0x141428,
    roughness: 0.8,
    metalness: 0.0,
    transparent: true,
    opacity: 0.4,
    side: THREE.DoubleSide,
  })

  const wallPositions = [
    { x: 0, z: -6.5, ry: 0 },
    { x: 0, z: 6.5, ry: 0 },
    { x: -6.5, z: 0, ry: Math.PI / 2 },
    { x: 6.5, z: 0, ry: Math.PI / 2 },
  ]
  wallPositions.forEach(function(wp) {
    const wg = new THREE.BoxGeometry(13, 3, 0.05)
    const wm = new THREE.Mesh(wg, wallMat)
    wm.position.set(wp.x, 1.5, wp.z)
    wm.rotation.y = wp.ry
    scene.add(wm)
  })

  // Meeting room table
  const tableMat = new THREE.MeshStandardMaterial({
    color: 0x1e1e40,
    roughness: 0.6,
    metalness: 0.2,
  })
  const table = new THREE.Mesh(new THREE.CylinderGeometry(1.0, 1.2, 0.2, 24), tableMat)
  table.position.set(0, 0.1, 5.5)
  table.receiveShadow = true
  table.castShadow = true
  scene.add(table)

  // Meeting room highlight ring
  const ringGeo = new THREE.RingGeometry(1.4, 1.6, 32)
  const ringMat = new THREE.MeshBasicMaterial({
    color: 0x6c5ce7,
    transparent: true,
    opacity: 0.15,
    side: THREE.DoubleSide,
  })
  const meetingRing = new THREE.Mesh(ringGeo, ringMat)
  meetingRing.rotation.x = -Math.PI / 2
  meetingRing.position.set(0, 0.01, 5.5)
  scene.add(meetingRing)

  // Create desks and agents
  DESK_POSITIONS.forEach(function(dp) {
    // Desk
    const deskMat = new THREE.MeshStandardMaterial({
      color: 0x1a1a35,
      roughness: 0.5,
      metalness: 0.3,
    })
    const desk = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.08, 0.9), deskMat)
    desk.position.set(dp.x, 0.04, dp.z)
    desk.receiveShadow = true
    desk.castShadow = true
    scene.add(desk)

    // Desk legs
    const legMat = new THREE.MeshStandardMaterial({ color: 0x2a2a5a, metalness: 0.5, roughness: 0.4 })
    const legPos = [[-0.6, -0.35], [0.6, -0.35], [-0.6, 0.35], [0.6, 0.35]]
    legPos.forEach(function(lp) {
      const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.3, 6), legMat)
      leg.position.set(dp.x + lp[0], -0.11, dp.z + lp[1])
      scene.add(leg)
    })

    // Agent character
    const agColor = new THREE.Color(AGENT_COLORS[dp.role] || '#6c5ce7')
    const group = new THREE.Group()
    group.position.set(dp.x, 0.25, dp.z)
    group.userData.role = dp.role

    // Body (cylinder)
    const bodyMat = new THREE.MeshStandardMaterial({ color: agColor, roughness: 0.3, metalness: 0.4, emissive: agColor, emissiveIntensity: 0.05 })
    const body = new THREE.Mesh(new THREE.CylinderGeometry(0.25, 0.3, 0.4, 12), bodyMat)
    body.position.y = 0.2
    body.castShadow = true
    group.add(body)

    // Head
    const headMat = new THREE.MeshStandardMaterial({ color: agColor, roughness: 0.2, metalness: 0.3, emissive: agColor, emissiveIntensity: 0.1 })
    const head = new THREE.Mesh(new THREE.SphereGeometry(0.18, 12, 10), headMat)
    head.position.y = 0.5
    head.castShadow = true
    group.add(head)

    // Eyes
    const eyeMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
    const pupilMat = new THREE.MeshBasicMaterial({ color: 0x000000 })
    ;[-0.07, 0.07].forEach(function(ex) {
      const eye = new THREE.Mesh(new THREE.SphereGeometry(0.04, 8, 8), eyeMat)
      eye.position.set(ex, 0.52, -0.16)
      group.add(eye)
      const pupil = new THREE.Mesh(new THREE.SphereGeometry(0.02, 8, 8), pupilMat)
      pupil.position.set(ex, 0.52, -0.19)
      group.add(pupil)
    })

    scene.add(group)
    agentMeshes[dp.role] = group

    // Status ring
    const sRing = new THREE.Mesh(
      new THREE.RingGeometry(0.35, 0.42, 24),
      new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0, side: THREE.DoubleSide })
    )
    sRing.rotation.x = -Math.PI / 2
    sRing.position.set(dp.x, 0.01, dp.z)
    scene.add(sRing)
    agentRings[dp.role] = sRing

    // CSS2D label
    const labelDiv = document.createElement('div')
    labelDiv.className = 'label-3d'
    labelDiv.innerHTML = '<div style="text-align:center;font-size:11px;font-weight:600;color:#e8e8f0;text-shadow:0 0 8px rgba(0,0,0,0.8);background:rgba(13,13,26,0.7);padding:2px 8px;border-radius:6px;border:1px solid ' + AGENT_COLORS[dp.role] + '44;">' + dp.label + '<br><span class="label-status" style="font-size:9px;color:#4ade80;">在线</span></div>'
    const lbl = new CSS2DObject(labelDiv)
    lbl.position.set(dp.x, 1.0, dp.z)
    scene.add(lbl)
    agentLabels[dp.role] = lbl
  })

  // Initial collaboration lines
  updateCollaborationLines()
}

function updateCollaborationLines(links) {
  // Remove existing lines
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

    const start = new THREE.Vector3(fromPos.x, 0.15, fromPos.z)
    const end = new THREE.Vector3(toPos.x, 0.15, toPos.z)

    const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
    mid.y += 0.2

    const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
    const points = curve.getPoints(20)
    const geo = new THREE.BufferGeometry().setFromPoints(points)
    const mat = new THREE.LineBasicMaterial({
      color: link.color || 0x6c5ce7,
      transparent: true,
      opacity: 0.3,
      linewidth: 1,
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

function animate() {
  animFrameId = requestAnimationFrame(animate)
  const delta = clock.getDelta()

  // Animate data packets
  dataPackets.forEach(function(p, idx) {
    p.progress += p.speed
    if (p.progress > 1) {
      scene.remove(p.mesh)
      p.mesh.geometry.dispose()
      p.mesh.material.dispose()
      p.trail.forEach(function(t) { scene.remove(t); t.geometry.dispose(); t.material.dispose() })
      dataPackets.splice(idx, 1)
      return
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
  })

  // Animate agent working bounce
  Object.keys(agentMeshes).forEach(function(role) {
    const m = agentMeshes[role]
    if (m.userData.isWorking && !m.userData.isAtMeeting) {
      m.position.y = 0.25 + Math.sin(Date.now() * 0.005) * 0.04
    }
  })

  // Animate meeting movement
  Object.keys(agentMeshes).forEach(function(role) {
    const m = agentMeshes[role]
    const anim = m.userData.meetingAnim
    if (anim) {
      anim.progress += anim.speed * delta * 60
      if (anim.progress >= 1) {
        m.position.copy(anim.endPos)
        m.userData.meetingAnim = null
        if (anim.onArrive) anim.onArrive()
      } else {
        const t = anim.progress
        const smooth = t * t * (3 - 2 * t)
        m.position.lerpVectors(anim.startPos, anim.endPos, smooth)
      }
    }
  })

  // Auto-rotate
  if (autoRotate.value && controls) {
    const pivot = new THREE.Vector3(0, 0.5, 0)
    const offset = new THREE.Vector3().copy(camera.position).sub(pivot)
    const angle = 0.0015 * delta * 60
    const cos = Math.cos(angle)
    const sin = Math.sin(angle)
    const x = offset.x * cos - offset.z * sin
    const z = offset.x * sin + offset.z * cos
    camera.position.x = pivot.x + x
    camera.position.z = pivot.z + z
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
  // Clean up geometries/materials
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
})
</script>

<style scoped>
.iso-office-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #0d0d1a;
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
  color: #e8e8f0;
  text-shadow: 0 0 12px rgba(0,0,0,0.6);
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
  border: 1px solid rgba(108,92,231,0.2);
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
  color: #9898b0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tb-btn:hover {
  background: rgba(108,92,231,0.2);
  color: #e8e8f0;
  border-color: rgba(108,92,231,0.4);
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
  background: rgba(13,13,26,0.75);
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(108,92,231,0.15);
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
  color: #9898b0;
}
</style>