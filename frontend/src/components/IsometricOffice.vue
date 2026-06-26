<template>
  <div ref="container" class="iso-office-container">
    <div class="office-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">AI 智能体 · 台式工作站</span>
        <span class="toolbar-status" :class="connectionClass">
          <span class="status-dot"></span>
          {{ connectionText }}
        </span>
      </div>
      <div class="toolbar-right">
        <button class="tb-btn" @click="resetCamera" title="Reset View"></button>
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
const connectionClass = ref("connecting")
const connectionText = ref("Connecting...")
const autoRotate = ref(props.autoRotate)

const legendItems = [
  { color: "#4ade80", label: "Online / Working" },
  { color: "#60a5fa", label: "Speaking" },
  { color: "#fb923c", label: "Tool Calling" },
  { color: "#f87171", label: "Offline" },
  { color: "#8b7cf7", label: "Data Packet" },
]

const AGENT_COLORS = {
  orchestrator: "#6c5ce7",
  calculator: "#60a5fa",
  weather: "#4ade80",
  searcher: "#fb923c",
  summarizer: "#f472b6",
}

// Human character builder (chibi seated) - improved cuter version
function createSeatedHumanBody(color) {
  const g = new THREE.Group()
  const c = new THREE.Color(color)
  
  const skinMat = new THREE.MeshStandardMaterial({ color: 0xf5d6c6, roughness: 0.3, metalness: 0 })
  const skinDarkMat = new THREE.MeshStandardMaterial({ color: 0xe8c8b8, roughness: 0.3, metalness: 0 })
  const bodyMat = new THREE.MeshStandardMaterial({ color: c, roughness: 0.35, metalness: 0.08, emissive: c, emissiveIntensity: 0.02 })
  const darkMat = new THREE.MeshStandardMaterial({ color: 0x3a3a50, roughness: 0.5 })
  const shoeMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.7 })
  const whiteMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
  const pupilMat = new THREE.MeshStandardMaterial({ color: 0x222233 })
  const irisMat = new THREE.MeshStandardMaterial({ color: 0x5a8a5a, roughness: 0.1 })
  const mouthMat = new THREE.MeshStandardMaterial({ color: 0xd4758b })
  const hairMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.55), roughness: 0.6 })
  const hairLightMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.7), roughness: 0.6 })
  
  // ---- LEGS (seated posture) ----
  for (let s = -1; s <= 1; s += 2) {
    const thigh = new THREE.Mesh(new THREE.CylinderGeometry(0.026, 0.022, 0.08, 8), darkMat)
    thigh.position.set(s * 0.03, -0.025, 0.05); thigh.rotation.x = -0.5; g.add(thigh)
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.016, 0.07, 8), darkMat)
    shin.position.set(s * 0.03, -0.075, 0.0); shin.rotation.x = 0.3; g.add(shin)
    const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.012, 0.04), shoeMat)
    shoe.position.set(s * 0.03, -0.10, -0.01); g.add(shoe)
  }
  
  // ---- TORSO ----
  const torso = new THREE.Mesh(new THREE.CylinderGeometry(0.085, 0.07, 0.18, 10), bodyMat)
  torso.position.set(0, 0.12, 0.01); torso.castShadow = true; g.add(torso)
  
  // Collar
  const collar = new THREE.Mesh(new THREE.TorusGeometry(0.055, 0.005, 6, 12), new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.2 }))
  collar.position.set(0, 0.17, 0.01); collar.rotation.x = 0.1; collar.scale.set(1, 0.4, 0.5); g.add(collar)
  
  // ---- ARMS ----
  for (let s = -1; s <= 1; s += 2) {
    const upper = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.018, 0.08, 7), bodyMat)
    upper.position.set(s * 0.075, 0.135, -0.01); upper.rotation.z = s * 0.25; upper.rotation.x = -0.3; g.add(upper)
    const lower = new THREE.Mesh(new THREE.CylinderGeometry(0.017, 0.015, 0.065, 7), skinMat)
    lower.position.set(s * 0.088, 0.07, 0.06); lower.rotation.z = s * 0.1; lower.rotation.x = 0.6; g.add(lower)
    const hand = new THREE.Mesh(new THREE.SphereGeometry(0.016, 6, 6), skinMat)
    hand.position.set(s * 0.090, 0.04, 0.095); g.add(hand)
  }
  
  // ---- NECK ----
  const neck = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.04, 0.05, 8), skinMat)
  neck.position.set(0, 0.21, 0.01); g.add(neck)
  
  // ---- HEAD (proportionate) ----
  const headGroup = new THREE.Group()
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.055, 16, 14), skinMat)
  head.scale.set(1, 0.9, 0.85); head.castShadow = true; headGroup.add(head)
  
  // Hair - sculpted style
  const hairMain = new THREE.Mesh(new THREE.SphereGeometry(0.078, 12, 10), hairMat)
  hairMain.position.set(0, 0.06, 0.015); hairMain.scale.set(1.15, 0.7, 0.95); headGroup.add(hairMain)
  
  // Hair fringe / bangs - swept style
  for (let i = 0; i < 7; i++) {
    const bang = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.025, 0.01), i % 2 === 0 ? hairLightMat : hairMat)
    bang.position.set((i - 3) * 0.016, 0.055, -0.052); bang.rotation.x = 0.4; bang.rotation.z = (i - 3) * 0.06; headGroup.add(bang)
  }
  
  // Side hair
  for (let s = -1; s <= 1; s += 2) {
    const side = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.055, 0.02), hairLightMat)
    side.position.set(s * 0.06, 0.02, 0.01); side.rotation.z = s * 0.15; headGroup.add(side)
  }
  
  // Top volume
  const top = new THREE.Mesh(new THREE.ConeGeometry(0.03, 0.03, 6), hairMat)
  top.position.set(-0.01, 0.068, -0.01); top.rotation.x = -0.2; headGroup.add(top)
  
  // ---- FACE ----
  // Eyes
  for (let s = -1; s <= 1; s += 2) {
    const ew = new THREE.Mesh(new THREE.SphereGeometry(0.022, 10, 10), whiteMat)
    ew.position.set(s * 0.022, 0.03, -0.055); ew.scale.set(1, 1.1, 0.3); headGroup.add(ew)
    const ir = new THREE.Mesh(new THREE.SphereGeometry(0.015, 10, 10), irisMat)
    ir.position.set(s * 0.022, 0.028, -0.058); headGroup.add(ir)
    const pu = new THREE.Mesh(new THREE.SphereGeometry(0.008, 8, 8), pupilMat)
    pu.position.set(s * 0.022, 0.028, -0.06); headGroup.add(pu)
    const hl = new THREE.Mesh(new THREE.SphereGeometry(0.005, 6, 6), new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xffffff, emissiveIntensity: 0.2 }))
    hl.position.set(s * 0.026, 0.036, -0.06); headGroup.add(hl)
  }
  
  // Eyebrows
  for (let s = -1; s <= 1; s += 2) {
    const brow = new THREE.Mesh(new THREE.BoxGeometry(0.016, 0.0025, 0.003), new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.3), roughness: 0.5 }))
    brow.position.set(s * 0.022, 0.045, -0.055); headGroup.add(brow)
  }
  
  // Mouth
  const mouth = new THREE.Mesh(new THREE.BoxGeometry(0.012, 0.003, 0.003), mouthMat)
  mouth.position.set(0, 0.012, -0.058); headGroup.add(mouth)
  
  // Subtle blush
  const blushMat = new THREE.MeshStandardMaterial({ color: 0xffa0a0, transparent: true, opacity: 0.3 })
  for (let s = -1; s <= 1; s += 2) {
    const bl = new THREE.Mesh(new THREE.SphereGeometry(0.012, 8, 8), blushMat)
    bl.position.set(s * 0.028, 0.016, -0.055); bl.scale.set(1, 0.6, 0.3); headGroup.add(bl)
  }
  
  headGroup.position.set(0, 0.28, 0.015)
  g.add(headGroup)
  
  g.userData.humanType = 'seated'
  return g
}function createStandingHumanBody(color) {
  const g = new THREE.Group()
  const c = new THREE.Color(color)
  
  const skinMat = new THREE.MeshStandardMaterial({ color: 0xf5d6c6, roughness: 0.3, metalness: 0 })
  const bodyMat = new THREE.MeshStandardMaterial({ color: c, roughness: 0.35, metalness: 0.08, emissive: c, emissiveIntensity: 0.02 })
  const darkMat = new THREE.MeshStandardMaterial({ color: 0x3a3a50, roughness: 0.5 })
  const shoeMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.7 })
  const whiteMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
  const pupilMat = new THREE.MeshStandardMaterial({ color: 0x222233 })
  const irisMat = new THREE.MeshStandardMaterial({ color: 0x5a8a5a, roughness: 0.1 })
  const mouthMat = new THREE.MeshStandardMaterial({ color: 0xd4758b })
  const hairMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.55), roughness: 0.6 })
  const hairLightMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.7), roughness: 0.6 })
  
  // ---- LEGS ----
  const leftLegPivot = new THREE.Group(); leftLegPivot.name = 'leftLegPivot'
  const rightLegPivot = new THREE.Group(); rightLegPivot.name = 'rightLegPivot'
  for (let s = -1; s <= 1; s += 2) {
    const pivot = s < 0 ? leftLegPivot : rightLegPivot
    const thigh = new THREE.Mesh(new THREE.CylinderGeometry(0.026, 0.022, 0.08, 8), darkMat)
    thigh.position.set(0, -0.04, 0); pivot.add(thigh)
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.016, 0.07, 8), darkMat)
    shin.position.set(0, -0.115, 0); pivot.add(shin)
    const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.012, 0.04), shoeMat)
    shoe.position.set(0, -0.155, 0.01); pivot.add(shoe)
    pivot.position.set(s * 0.035, 0.12, 0)
  }
  g.add(leftLegPivot); g.add(rightLegPivot)
  
  // ---- TORSO ----
  const torso = new THREE.Mesh(new THREE.CylinderGeometry(0.085, 0.07, 0.18, 10), bodyMat)
  torso.position.set(0, 0.19, 0); torso.castShadow = true; g.add(torso)
  
  // Collar
  const collar = new THREE.Mesh(new THREE.TorusGeometry(0.055, 0.005, 6, 12), new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.2 }))
  collar.position.set(0, 0.26, 0); collar.rotation.x = 0.1; collar.scale.set(1, 0.4, 0.5); g.add(collar)
  
  // ---- ARMS ----
  const leftArmPivot = new THREE.Group(); leftArmPivot.name = 'leftArmPivot'
  const rightArmPivot = new THREE.Group(); rightArmPivot.name = 'rightArmPivot'
  for (let s = -1; s <= 1; s += 2) {
    const pivot = s < 0 ? leftArmPivot : rightArmPivot
    const upper = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.016, 0.075, 7), bodyMat)
    upper.position.set(0, -0.038, 0); pivot.add(upper)
    const forearm = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.013, 0.065, 7), skinMat)
    forearm.position.set(0, -0.10, 0); pivot.add(forearm)
    const hand = new THREE.Mesh(new THREE.SphereGeometry(0.015, 6, 6), skinMat)
    hand.position.set(0, -0.135, 0); pivot.add(hand)
    pivot.position.set(s * 0.08, 0.24, 0)
  }
  g.add(leftArmPivot); g.add(rightArmPivot)
  
  // ---- NECK ----
  const neck = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.04, 0.05, 8), skinMat)
  neck.position.set(0, 0.28, 0); g.add(neck)
  
  // ---- HEAD ----
  const headGroup = new THREE.Group()
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.055, 16, 14), skinMat)
  head.scale.set(1, 0.9, 0.85); head.castShadow = true; headGroup.add(head)
  
  const hairMain = new THREE.Mesh(new THREE.SphereGeometry(0.078, 12, 10), hairMat)
  hairMain.position.set(0, 0.06, 0.015); hairMain.scale.set(1.15, 0.7, 0.95); headGroup.add(hairMain)
  
  for (let i = 0; i < 7; i++) {
    const bang = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.025, 0.01), i % 2 === 0 ? hairLightMat : hairMat)
    bang.position.set((i - 3) * 0.016, 0.055, -0.052); bang.rotation.x = 0.4; bang.rotation.z = (i - 3) * 0.06; headGroup.add(bang)
  }
  
  for (let s = -1; s <= 1; s += 2) {
    const side = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.055, 0.02), hairLightMat)
    side.position.set(s * 0.06, 0.02, 0.01); side.rotation.z = s * 0.15; headGroup.add(side)
  }
  
  const top = new THREE.Mesh(new THREE.ConeGeometry(0.03, 0.03, 6), hairMat)
  top.position.set(-0.01, 0.068, -0.01); top.rotation.x = -0.2; headGroup.add(top)
  
  // Eyes
  for (let s = -1; s <= 1; s += 2) {
    const ew = new THREE.Mesh(new THREE.SphereGeometry(0.022, 10, 10), whiteMat)
    ew.position.set(s * 0.022, 0.03, -0.055); ew.scale.set(1, 1.1, 0.3); headGroup.add(ew)
    const ir = new THREE.Mesh(new THREE.SphereGeometry(0.015, 10, 10), irisMat)
    ir.position.set(s * 0.022, 0.028, -0.058); headGroup.add(ir)
    const pu = new THREE.Mesh(new THREE.SphereGeometry(0.008, 8, 8), pupilMat)
    pu.position.set(s * 0.022, 0.028, -0.06); headGroup.add(pu)
    const hl = new THREE.Mesh(new THREE.SphereGeometry(0.005, 6, 6), new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xffffff, emissiveIntensity: 0.2 }))
    hl.position.set(s * 0.026, 0.036, -0.06); headGroup.add(hl)
  }
  
  // Eyebrows
  for (let s = -1; s <= 1; s += 2) {
    const brow = new THREE.Mesh(new THREE.BoxGeometry(0.016, 0.0025, 0.003), new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.3), roughness: 0.5 }))
    brow.position.set(s * 0.022, 0.045, -0.055); headGroup.add(brow)
  }
  
  const mouth = new THREE.Mesh(new THREE.BoxGeometry(0.012, 0.003, 0.003), mouthMat)
  mouth.position.set(0, 0.012, -0.058); headGroup.add(mouth)
  
  const blushMat = new THREE.MeshStandardMaterial({ color: 0xffa0a0, transparent: true, opacity: 0.3 })
  for (let s = -1; s <= 1; s += 2) {
    const bl = new THREE.Mesh(new THREE.SphereGeometry(0.012, 8, 8), blushMat)
    bl.position.set(s * 0.028, 0.016, -0.055); bl.scale.set(1, 0.6, 0.3); headGroup.add(bl)
  }
  
  headGroup.position.set(0, 0.36, 0)
  g.add(headGroup)
  
  g.userData.humanType = 'standing'
  return g
}

function createRoleAccessory(role, color) {
  const c = new THREE.Color(color)
  const g = new THREE.Group()
  if (role === "orchestrator") {
    const headMat = new THREE.MeshStandardMaterial({ color: 0xffd700, metalness: 0.7, roughness: 0.2 })
    const band = new THREE.Mesh(new THREE.TorusGeometry(0.07, 0.006, 6, 12), headMat)
    band.position.y = 0.36; band.rotation.x = 0.2; g.add(band)
    const gem = new THREE.Mesh(new THREE.SphereGeometry(0.015, 6, 6), new THREE.MeshStandardMaterial({ color: 0x6c5ce7, emissive: 0x6c5ce7, emissiveIntensity: 0.5 }))
    gem.position.set(0, 0.38, -0.07); g.add(gem)
  }
  if (role === "calculator") {
    const gMat = new THREE.MeshStandardMaterial({ color: 0x88bbff, metalness: 0.3, roughness: 0.2, transparent: true, opacity: 0.5 })
    for (let s = -1; s <= 1; s += 2) {
      const lens = new THREE.Mesh(new THREE.TorusGeometry(0.025, 0.005, 6, 10), gMat)
      lens.position.set(s * 0.035, 0.295, -0.09); lens.rotation.y = 0.2; g.add(lens)
    }
    const bridge = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.004, 0.004), new THREE.MeshStandardMaterial({ color: 0x88aadd, metalness: 0.5 }))
    bridge.position.set(0, 0.295, -0.09); g.add(bridge)
  }
  if (role === "weather") {
    const cloudMat = new THREE.MeshStandardMaterial({ color: 0xffffff, transparent: true, opacity: 0.7 })
    for (let i = 0; i < 3; i++) {
      const cl = new THREE.Mesh(new THREE.SphereGeometry(0.018, 6, 6), cloudMat)
      cl.position.set((i - 1) * 0.022, 0.37, -0.04); g.add(cl)
    }
    const bolt = new THREE.Mesh(new THREE.ConeGeometry(0.006, 0.02, 4), new THREE.MeshStandardMaterial({ color: 0xffdd44, emissive: 0xffdd44, emissiveIntensity: 0.3 }))
    bolt.position.set(0.02, 0.35, -0.05); bolt.rotation.z = 0.2; g.add(bolt)
  }
  if (role === "searcher") {
    const mMat = new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0.6, roughness: 0.2 })
    const ring = new THREE.Mesh(new THREE.TorusGeometry(0.025, 0.006, 6, 10), mMat)
    ring.position.set(0.06, 0.28, -0.08); ring.rotation.y = 0.3; g.add(ring)
    const handle = new THREE.Mesh(new THREE.BoxGeometry(0.004, 0.03, 0.004), new THREE.MeshStandardMaterial({ color: 0x8b6b4a }))
    handle.position.set(0.06, 0.25, -0.08); g.add(handle)
  }
  if (role === "summarizer") {
    const bowMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(1.3), roughness: 0.5 })
    for (let s = -1; s <= 1; s += 2) {
      const wing = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.015, 0.008), bowMat)
      wing.position.set(s * 0.025, -0.06, 0.04); g.add(wing)
    }
    const center = new THREE.Mesh(new THREE.BoxGeometry(0.008, 0.015, 0.008), bowMat)
    center.position.set(0, -0.06, 0.04); g.add(center)
  }
  return g
}
function createDeskComputer() {
  const g = new THREE.Group()
  const techMat = new THREE.MeshStandardMaterial({ color: 0x2c2c3e, roughness: 0.15, metalness: 0.9 })
  const bezelMat = new THREE.MeshStandardMaterial({ color: 0x16161e, roughness: 0.1, metalness: 0.8 })
  const screenMatGlow = new THREE.MeshStandardMaterial({ color: 0x4488ff, emissive: 0x4488ff, emissiveIntensity: 0.25 })
  const ledMat = new THREE.MeshStandardMaterial({ color: 0x00e676, emissive: 0x00e676, emissiveIntensity: 0.8 })
  const ledDimMat = new THREE.MeshStandardMaterial({ color: 0x448aff, emissive: 0x448aff, emissiveIntensity: 0.3 })

  // Desktop monitor (screen facing AWAY - agent sat wrong way round)
  const standBase = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.008, 0.05), techMat)
  standBase.position.set(0, 0.006, -0.01); g.add(standBase)
  const standNeck = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.05, 0.015), techMat)
  standNeck.position.set(0, 0.045, -0.01); g.add(standNeck)

  const bezel = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.14, 0.015), bezelMat)
  bezel.rotation.y = Math.PI
  bezel.position.set(0, 0.12, -0.01); g.add(bezel)
  const screenInner = new THREE.Mesh(new THREE.PlaneGeometry(0.20, 0.12), screenMatGlow)
  screenInner.position.set(0, 0.12, -0.023); screenInner.rotation.y = Math.PI; g.add(screenInner)
  const codeMat = new THREE.MeshBasicMaterial({ color: 0x58a6ff, transparent: true, opacity: 0.4 })
  for (let i = 0; i < 7; i++) {
    const line = new THREE.Mesh(new THREE.PlaneGeometry(0.03 + Math.random() * 0.10, 0.003), codeMat)
    line.position.set(-0.06 + Math.random() * 0.08, 0.14 - i * 0.014, 0.008); g.add(line)
  }
  const cursorMat = new THREE.MeshBasicMaterial({ color: 0x58a6ff, transparent: true, opacity: 0.7 })
  const cursor = new THREE.Mesh(new THREE.PlaneGeometry(0.004, 0.012), cursorMat)
  cursor.position.set(0.05, 0.12, 0.008); g.add(cursor)

  // Desktop case (tower)
  const caseBody = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.10, 0.07), techMat)
  caseBody.position.set(0.20, 0.05, -0.01); g.add(caseBody)
  const frontPanel = new THREE.Mesh(new THREE.PlaneGeometry(0.058, 0.095), new THREE.MeshBasicMaterial({ color: 0x0d0d1a }))
  frontPanel.position.set(0.20, 0.05, 0.034); g.add(frontPanel)
  const powerLed = new THREE.Mesh(new THREE.CircleGeometry(0.004, 8), ledMat)
  powerLed.position.set(0.17, 0.09, 0.036); g.add(powerLed)
  const hddLed = new THREE.Mesh(new THREE.CircleGeometry(0.003, 8), ledDimMat)
  hddLed.position.set(0.185, 0.09, 0.036); g.add(hddLed)
  const pwrBtn = new THREE.Mesh(new THREE.CircleGeometry(0.006, 10), new THREE.MeshStandardMaterial({ color: 0x3a3a4e }))
  pwrBtn.position.set(0.20, 0.02, 0.036); g.add(pwrBtn)
  const dvd = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.006, 0.002), new THREE.MeshStandardMaterial({ color: 0x1a1a2e }))
  dvd.position.set(0.20, 0.07, 0.036); g.add(dvd)
  const ventMat = new THREE.MeshBasicMaterial({ color: 0x2c2c3e })
  for (let i = 0; i < 4; i++) {
    const vent = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.001, 0.002), ventMat)
    vent.position.set(0.20, 0.035 + i * 0.006, 0.036); g.add(vent)
  }
  for (let i = 0; i < 5; i++) {
    const sv = new THREE.Mesh(new THREE.BoxGeometry(0.002, 0.08, 0.002), ventMat)
    sv.position.set(0.228, 0.05, -0.015 + i * 0.008); g.add(sv)
  }

  // Keyboard
  const kbBase = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.006, 0.05), new THREE.MeshStandardMaterial({ color: 0x12121e, roughness: 0.3 }))
  kbBase.position.set(-0.05, 0.003, 0.08); g.add(kbBase)
  const keyMat = new THREE.MeshStandardMaterial({ color: 0x1e1e32, roughness: 0.4 })
  for (let r = 0; r < 4; r++) {
    for (let kc = 0; kc < 8; kc++) {
      const key = new THREE.Mesh(new THREE.BoxGeometry(0.014, 0.003, 0.009), keyMat)
      key.position.set(-0.11 + kc * 0.019, 0.005, 0.06 + r * 0.013); g.add(key)
    }
  }
  const spaceBar = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.003, 0.009), keyMat)
  spaceBar.position.set(-0.055, 0.005, 0.10); g.add(spaceBar)

  // Mouse
  const mouseBody = new THREE.Mesh(new THREE.BoxGeometry(0.018, 0.012, 0.025), new THREE.MeshStandardMaterial({ color: 0x12121e, roughness: 0.2 }))
  mouseBody.position.set(0.12, 0.004, 0.09); g.add(mouseBody)
  const mouseWheel = new THREE.Mesh(new THREE.BoxGeometry(0.004, 0.003, 0.008), new THREE.MeshStandardMaterial({ color: 0x2c2c3e }))
  mouseWheel.position.set(0.12, 0.008, 0.09); g.add(mouseWheel)

  // Cable
  const cableMat = new THREE.MeshBasicMaterial({ color: 0x1a1a2e })
  for (let i = 0; i < 3; i++) {
    const cable = new THREE.Mesh(new THREE.BoxGeometry(0.002, 0.002, 0.03), cableMat)
    cable.position.set(0.16 - i * 0.008, 0.005, 0.01 + i * 0.015); g.add(cable)
  }

  g.rotation.y = Math.PI
  g.userData.isComputer = true
  return g
}
const STATUS_COLORS = {
  idle: "#4ade80", working: "#4ade80", speaking: "#60a5fa", tool_calling: "#fb923c", offline: "#f87171",
}

let scene, camera, renderer, labelRenderer, controls
let animFrameId = null
let clock = new THREE.Clock()
let agentData = {}
let collaborLines = []
let dataPackets = []

const DESK_POSITIONS = [
  { role: "orchestrator", x: -2.2, z: -1.8, label: "Orchestrator" },
  { role: "calculator", x: 0, z: -1.8, label: "Calculator" },
  { role: "searcher", x: 2.2, z: -1.8, label: "Searcher" },
  { role: "weather", x: -1.1, z: 1.8, label: "Weather" },
  { role: "summarizer", x: 1.1, z: 1.8, label: "Summarizer" },
]

// Helper functions (sendDataPacket, clearDataPackets, etc.) - same as before
function sendDataPacket(fromRole, toRole, color, data) {
  if (color === undefined) color = "#8b7cf7"
  if (data === undefined) data = ""
  const fromPos = DESK_POSITIONS.find(p => p.role === fromRole)
  const toPos = DESK_POSITIONS.find(p => p.role === toRole)
  if (!fromPos || !toPos || !scene) return
  const start = new THREE.Vector3(fromPos.x, 0.8, fromPos.z)
  const end = new THREE.Vector3(toPos.x, 0.8, toPos.z)
  const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
  mid.y += 0.8 + Math.random() * 0.8
  const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
  const pColor = new THREE.Color(color)
  const sphere = new THREE.Mesh(new THREE.SphereGeometry(0.08, 8, 8), new THREE.MeshBasicMaterial({ color: pColor }))
  sphere.position.copy(start); scene.add(sphere)
  const glow = new THREE.Mesh(new THREE.SphereGeometry(0.14, 8, 8), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.25, blending: THREE.AdditiveBlending }))
  sphere.add(glow)
  const trail = []
  for (let i = 0; i < 5; i++) {
    const t = new THREE.Mesh(new THREE.SphereGeometry(0.03, 4, 4), new THREE.MeshBasicMaterial({ color: pColor, transparent: true, opacity: 0.3 }))
    t.visible = false; scene.add(t); trail.push(t)
  }
  dataPackets.push({ mesh: sphere, trail, curve, progress: 0, speed: 0.005 + Math.random() * 0.003, trailCount: 5, data })
}

function clearDataPackets() {
  dataPackets.forEach(p => { scene.remove(p.mesh); p.mesh.geometry.dispose(); p.mesh.material.dispose(); p.trail.forEach(t => { scene.remove(t); t.geometry.dispose(); t.material.dispose() }) })
  dataPackets = []
}

function showSpeechBubble(role, text, duration) {
  if (duration === undefined) duration = 2500
  if (!scene) return
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  const div = document.createElement("div"); div.className = "label-3d label-speech"; div.textContent = text
  const lbl = new CSS2DObject(div); lbl.position.set(desk.x, 2.2 + Math.random() * 0.3, desk.z); scene.add(lbl)
  setTimeout(function() { if (lbl.parent) scene.remove(lbl) }, duration)
}

function updateAgentStatus(role, status) {
  const d = agentData[role]
  if (!d) return
  d.currentStatus = status
  const color = STATUS_COLORS[status] || STATUS_COLORS.idle
  const targetColor = new THREE.Color(color)
  const humanGroup = d.group.children.find(function(c) { return c.userData && c.userData.humanType })
  if (humanGroup) {
    humanGroup.children.forEach(function(part) {
      if (part.material && part.material.color) {
        part.material.color.lerp(targetColor, 0.3)
        if (part.material.emissive) part.material.emissive.lerp(targetColor, 0.3)
      }
    })
  }
  d.isWorking = status === "working"
  const lbl = d.label
  if (lbl) {
    const map = { idle: "Online", working: "Working...", speaking: "Speaking", tool_calling: "Using Tools", offline: "Offline" }
    const s = lbl.element.querySelector(".label-status")
    if (s) { s.textContent = map[status] || status; s.style.color = color }
  }
}

function setConnectionStatus(online) {
  connectionClass.value = online ? "online" : "offline"
  connectionText.value = online ? "Connected" : "Offline"
}

function moveAgentTo(role, targetX, targetZ, onArrive) {
  const d = agentData[role]
  if (!d || !d.group) return
  // Swap body to standing and hide computer
  showBodyVariant(d.group, "standing")
  const comp = d.group.children.find(function(c) { return c.userData && c.userData.isComputer })
  if (comp) comp.visible = false
  d.moveAnim = { startPos: d.group.position.clone(), endPos: new THREE.Vector3(targetX, 0, targetZ), progress: 0, speed: 0.018 + Math.random() * 0.008, onArrive, role }
}

function showBodyVariant(group, variant) {
  group.children.forEach(function(ch) {
    if (ch.userData && ch.userData.bodyVariant) {
      ch.visible = ch.userData.bodyVariant === variant
    }
  })
}

function returnAgentToDesk(role) {
  const desk = DESK_POSITIONS.find(p => p.role === role)
  if (!desk) return
  moveAgentTo(role, desk.x, desk.z, function() {
    const d = agentData[role]
    if (!d || !d.group) return
    showBodyVariant(d.group, "seated")
    const comp = d.group.children.find(function(c) { return c.userData && c.userData.isComputer })
    if (comp) comp.visible = true
  })
}

function getDeskX(role) { const d = DESK_POSITIONS.find(p => p.role === role); return d ? d.x : 0 }
function getDeskZ(role) { const d = DESK_POSITIONS.find(p => p.role === role); return d ? d.z : 0 }

// Office decoration helpers
function createOfficePlant(pos, scale) {
  scale = scale || 1
  const g = new THREE.Group()
  const pot = new THREE.Mesh(new THREE.CylinderGeometry(0.08 * scale, 0.06 * scale, 0.12 * scale, 8), new THREE.MeshStandardMaterial({ color: 0x8b6b4a }))
  pot.position.y = 0.06 * scale; g.add(pot)
  const leafMat = new THREE.MeshStandardMaterial({ color: 0x4ade80, roughness: 0.9, emissive: new THREE.Color(0x2d8a4e), emissiveIntensity: 0.05 })
  for (let i = 0; i < 7; i++) {
    const leaf = new THREE.Mesh(new THREE.SphereGeometry(0.05 * scale, 6, 6), leafMat)
    const angle = (i / 7) * Math.PI * 2
    leaf.position.set(Math.cos(angle) * 0.08 * scale, 0.15 * scale + Math.random() * 0.04 * scale, Math.sin(angle) * 0.08 * scale)
    leaf.scale.set(1, 1.3 + Math.random() * 0.4, 0.8); g.add(leaf)
  }
  g.position.set(pos.x, 0, pos.z); return g
}


// ---- New: Desk decoration items ----
function createDeskOrganizer() {
  const g = new THREE.Group()
  const phMat = new THREE.MeshStandardMaterial({ color: 0x3a3a5a, roughness: 0.6 })
  const ph = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.018, 0.03, 8), phMat)
  ph.position.set(-0.22, 0.015, 0.15); g.add(ph)
  const penMat = new THREE.MeshStandardMaterial({ color: 0x4477cc, roughness: 0.3 })
  for (let pn = 0; pn < 3; pn++) {
    const pen = new THREE.Mesh(new THREE.CylinderGeometry(0.003, 0.002, 0.04, 5), penMat)
    pen.position.set(-0.22 + pn * 0.006, 0.03, 0.15); g.add(pen)
  }
  const noteMat = new THREE.MeshStandardMaterial({ color: 0xffeebb, roughness: 0.7 })
  const pad = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.004, 0.03), noteMat)
  pad.position.set(-0.28, 0.002, 0.18); g.add(pad)
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x8b6b4a, roughness: 0.4, metalness: 0.2 })
  const photoMat = new THREE.MeshStandardMaterial({ color: 0xaaccdd, roughness: 0.6 })
  const frame = new THREE.Mesh(new THREE.BoxGeometry(0.025, 0.03, 0.005), frameMat)
  frame.position.set(0.2, 0.015, 0.15); g.add(frame)
  const photo = new THREE.Mesh(new THREE.PlaneGeometry(0.02, 0.025), photoMat)
  photo.position.set(0.2, 0.015, 0.153); g.add(photo)
  const lampBaseMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.3, metalness: 0.5 })
  const lamp = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.025, 0.008, 8), lampBaseMat)
  lamp.position.set(0.3, 0.005, -0.12); g.add(lamp)
  const lampArm = new THREE.Mesh(new THREE.CylinderGeometry(0.004, 0.005, 0.06, 6), lampBaseMat)
  lampArm.position.set(0.3, 0.03, -0.12); g.add(lampArm)
  const lampHeadMat = new THREE.MeshStandardMaterial({ color: 0x3a3a5a, roughness: 0.3 })
  const lampHead = new THREE.Mesh(new THREE.ConeGeometry(0.02, 0.015, 8), lampHeadMat)
  lampHead.position.set(0.3, 0.065, -0.16); lampHead.rotation.x = 0.5; g.add(lampHead)
  const lampGlowMat = new THREE.MeshBasicMaterial({ color: 0xffeecc, transparent: true, opacity: 0.06, blending: THREE.AdditiveBlending })
  const lampGlow = new THREE.Mesh(new THREE.SphereGeometry(0.015, 6, 6), lampGlowMat)
  lampGlow.position.set(0.3, 0.06, -0.17); g.add(lampGlow)
  return g
}

function createDeskCalendar() {
  const g = new THREE.Group()
  const calMat = new THREE.MeshStandardMaterial({ color: 0xf0e8d8, roughness: 0.5 })
  const cal = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.004, 0.025), calMat)
  cal.position.set(-0.1, 0.003, -0.22); g.add(cal)
  const calStand = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.008, 0.003), new THREE.MeshStandardMaterial({ color: 0x5a4a3a, roughness: 0.4 }))
  calStand.position.set(-0.1, 0.006, -0.235); g.add(calStand)
  const gridMat = new THREE.MeshBasicMaterial({ color: 0x444444, transparent: true, opacity: 0.3 })
  for (let i = 0; i < 4; i++) {
    const line = new THREE.Mesh(new THREE.PlaneGeometry(0.003, 0.004), gridMat)
    line.position.set(-0.1 + (i - 1.5) * 0.007, 0.006, -0.22); g.add(line)
  }
  return g
}

function createDeskCoffeeCup(posOffset) {
  const g = new THREE.Group()
  const cupMat = new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.2 })
  const cup = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.012, 0.025, 8), cupMat)
  cup.position.set(posOffset[0], 0.015, posOffset[1]); g.add(cup)
  const handleMat = new THREE.MeshStandardMaterial({ color: 0xdddddd, roughness: 0.3 })
  const handle = new THREE.Mesh(new THREE.TorusGeometry(0.008, 0.003, 5, 6), handleMat)
  handle.position.set(posOffset[0] + 0.016, 0.015, posOffset[1]); handle.rotation.x = Math.PI / 2; g.add(handle)
  const coffeeMat = new THREE.MeshStandardMaterial({ color: 0x6b3a1a, roughness: 0.8 })
  const coffee = new THREE.Mesh(new THREE.CircleGeometry(0.012, 8), coffeeMat)
  coffee.position.set(posOffset[0], 0.025, posOffset[1]); coffee.rotation.x = -Math.PI / 2; g.add(coffee)
  const steamMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.08 })
  const steam = new THREE.Mesh(new THREE.SphereGeometry(0.005, 4, 4), steamMat)
  steam.position.set(posOffset[0], 0.035, posOffset[1]); g.add(steam)
  return g
}

// ---- New: Whiteboard for meeting room ----
function createWhiteboard() {
  const g = new THREE.Group()
  const wbMat = new THREE.MeshStandardMaterial({ color: 0xf0f0f0, roughness: 0.3, metalness: 0.05 })
  const wb = new THREE.Mesh(new THREE.BoxGeometry(1.0, 0.6, 0.015), wbMat)
  wb.position.set(3.2, 1.0, 4.0); g.add(wb)
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x5a5a6a, roughness: 0.4, metalness: 0.3 })
  const fr = new THREE.Mesh(new THREE.BoxGeometry(1.02, 0.62, 0.02), frameMat)
  fr.position.set(3.2, 1.0, 3.99); g.add(fr)
  const markerMat = new THREE.MeshBasicMaterial({ color: 0x4488ff, transparent: true, opacity: 0.3 })
  for (let i = 0; i < 5; i++) {
    const ln = new THREE.Mesh(new THREE.PlaneGeometry(0.08 + Math.random() * 0.15, 0.003), markerMat)
    ln.position.set(3.2 + (i - 2) * 0.14, 1.0 + (i % 3 - 1) * 0.1, 4.005); g.add(ln)
  }
  const markerMat2 = new THREE.MeshBasicMaterial({ color: 0xff6644, transparent: true, opacity: 0.25 })
  const circle = new THREE.Mesh(new THREE.RingGeometry(0.04, 0.045, 12), markerMat2)
  circle.position.set(3.55, 1.0, 4.005); circle.lookAt(3.55, 1.0, 5.0); g.add(circle)
  const trayMat = new THREE.MeshStandardMaterial({ color: 0x3a3a4a, roughness: 0.4 })
  const tray = new THREE.Mesh(new THREE.BoxGeometry(0.15, 0.005, 0.02), trayMat)
  tray.position.set(3.2, 0.685, 3.99); g.add(tray)
  const markers = [0xff4444, 0x44aaff, 0x44dd44, 0xffaa00]
  markers.forEach((mc, mi) => {
    const mk = new THREE.Mesh(new THREE.CylinderGeometry(0.003, 0.003, 0.015, 5), new THREE.MeshStandardMaterial({ color: mc, roughness: 0.4 }))
    mk.position.set(3.2 + (mi - 1.5) * 0.03, 0.692, 3.99); g.add(mk)
  })
  return g
}

// ---- New: Projection screen ----
function createProjectionScreen() {
  const g = new THREE.Group()
  const scrMat = new THREE.MeshStandardMaterial({ color: 0xe8e4dc, roughness: 0.6, metalness: 0.0, side: THREE.DoubleSide })
  const scr = new THREE.Mesh(new THREE.PlaneGeometry(0.8, 0.55), scrMat)
  scr.position.set(3.2, 1.0, 6.0); g.add(scr)
  const bMat = new THREE.MeshStandardMaterial({ color: 0x4a4a5a, roughness: 0.3, metalness: 0.2 })
  const frame = new THREE.Mesh(new THREE.BoxGeometry(0.84, 0.010, 0.02), bMat)
  frame.position.set(3.2, 0.7, 5.99); g.add(frame)
  const projMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.2, metalness: 0.6 })
  const proj = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.04, 0.08), projMat)
  proj.position.set(3.2, 2.0, 5.2); g.add(proj)
  const lensMat = new THREE.MeshStandardMaterial({ color: 0x444466, roughness: 0.1, metalness: 0.8 })
  const lens = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.015, 0.015, 8), lensMat)
  lens.position.set(3.2, 1.98, 5.3); g.add(lens)
  return g
}

// ---- New: Break area equipment ----
function createCoffeeMachine(pos) {
  const g = new THREE.Group()
  const bMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.2, metalness: 0.7 })
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.2, 0.12), bMat)
  body.position.set(pos[0], 0.1, pos[1]); g.add(body)
  const frontMat = new THREE.MeshStandardMaterial({ color: 0x1a1a2e, roughness: 0.3 })
  const front = new THREE.Mesh(new THREE.PlaneGeometry(0.10, 0.18), frontMat)
  front.position.set(pos[0], 0.1, pos[1] + 0.061); g.add(front)
  const tankMat = new THREE.MeshStandardMaterial({ color: 0x88bbee, transparent: true, opacity: 0.3, roughness: 0.1 })
  const tank = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.12, 0.04), tankMat)
  tank.position.set(pos[0], 0.14, pos[1] - 0.06); g.add(tank)
  const dripMat = new THREE.MeshStandardMaterial({ color: 0x3a3a4e, roughness: 0.3 })
  const drip = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.008, 0.04), dripMat)
  drip.position.set(pos[0], 0.06, pos[1] + 0.03); g.add(drip)
  const cupM = new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.3 })
  const cc = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.01, 0.015, 8), cupM)
  cc.position.set(pos[0], 0.03, pos[1] + 0.03); g.add(cc)
  const ledMat = new THREE.MeshStandardMaterial({ color: 0x00ff88, emissive: 0x00ff88, emissiveIntensity: 0.5 })
  const led = new THREE.Mesh(new THREE.CircleGeometry(0.004, 6), ledMat)
  led.position.set(pos[0] + 0.04, 0.16, pos[1] + 0.062); g.add(led)
  return g
}

function createWaterDispenser(pos) {
  const g = new THREE.Group()
  const bMat = new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.2 })
  const body = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.07, 0.22, 10), bMat)
  body.position.set(pos[0], 0.11, pos[1]); g.add(body)
  const topMat = new THREE.MeshStandardMaterial({ color: 0x88bbdd, transparent: true, opacity: 0.35, roughness: 0.1 })
  const top = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.055, 0.12, 10), topMat)
  top.position.set(pos[0], 0.24, pos[1]); g.add(top)
  const faucetMat = new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0.6, roughness: 0.2 })
  const faucet = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.01, 0.025), faucetMat)
  faucet.position.set(pos[0], 0.18, pos[1] + 0.055); g.add(faucet)
  const btnMat = new THREE.MeshStandardMaterial({ color: 0xff4444, roughness: 0.3 })
  const btn = new THREE.Mesh(new THREE.CircleGeometry(0.005, 6), btnMat)
  btn.position.set(pos[0] - 0.02, 0.12, pos[1] + 0.065); g.add(btn)
  const btn2 = new THREE.Mesh(new THREE.CircleGeometry(0.005, 6), new THREE.MeshStandardMaterial({ color: 0x4488ff, roughness: 0.3 }))
  btn2.position.set(pos[0] + 0.02, 0.12, pos[1] + 0.065); g.add(btn2)
  const dt = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.004, 0.03), new THREE.MeshStandardMaterial({ color: 0xaaaaaa, metalness: 0.3 }))
  dt.position.set(pos[0], 0.035, pos[1] + 0.065); g.add(dt)
  return g
}

function createMicrowave(pos) {
  const g = new THREE.Group()
  const bMat = new THREE.MeshStandardMaterial({ color: 0xcccccc, roughness: 0.2, metalness: 0.4 })
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.12, 0.16), bMat)
  body.position.set(pos[0], 0.06, pos[1]); g.add(body)
  const doorMat = new THREE.MeshStandardMaterial({ color: 0x3a3a4a, roughness: 0.2, metalness: 0.6 })
  const door = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.08, 0.005), doorMat)
  door.position.set(pos[0], 0.06, pos[1] + 0.083); g.add(door)
  const winMat = new THREE.MeshStandardMaterial({ color: 0x1a1a2e, roughness: 0.1 })
  const win = new THREE.Mesh(new THREE.PlaneGeometry(0.06, 0.04), winMat)
  win.position.set(pos[0], 0.06, pos[1] + 0.086); g.add(win)
  const cpMat = new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.3 })
  const cp = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.02, 0.002), cpMat)
  cp.position.set(pos[0] + 0.04, 0.09, pos[1] + 0.086); g.add(cp)
  const digMat = new THREE.MeshBasicMaterial({ color: 0x00ff44, transparent: true, opacity: 0.4 })
  for (let d = 0; d < 4; d++) {
    const dig = new THREE.Mesh(new THREE.PlaneGeometry(0.004, 0.008), digMat)
    dig.position.set(pos[0] + 0.035 + d * 0.007, 0.09, pos[1] + 0.088); g.add(dig)
  }
  return g
}

// ---- New: Vending machine (corner) ----
function createVendingMachine(pos) {
  const g = new THREE.Group()
  const bMat = new THREE.MeshStandardMaterial({ color: 0x446688, roughness: 0.3, metalness: 0.3 })
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.5, 0.2), bMat)
  body.position.set(pos[0], 0.3, pos[1]); g.add(body)
  const glMat = new THREE.MeshStandardMaterial({ color: 0x88bbdd, transparent: true, opacity: 0.2, roughness: 0.1 })
  const gf = new THREE.Mesh(new THREE.PlaneGeometry(0.26, 0.35), glMat)
  gf.position.set(pos[0], 0.28, pos[1] + 0.101); g.add(gf)
  const itemColors = [0xff6644, 0x44aaff, 0x44dd44, 0xffaa44, 0xff4488]
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 4; col++) {
      const im = new THREE.MeshStandardMaterial({ color: itemColors[(row + col) % itemColors.length], roughness: 0.5 })
      const item = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.04, 0.02), im)
      item.position.set(pos[0] - 0.08 + col * 0.055, 0.1 + row * 0.12, pos[1] + 0.1); g.add(item)
    }
  }
  const btnMat = new THREE.MeshStandardMaterial({ color: 0x4488ff, roughness: 0.3 })
  for (let i = 0; i < 6; i++) {
    const btn = new THREE.Mesh(new THREE.CircleGeometry(0.004, 6), btnMat)
    btn.position.set(pos[0] + 0.12, 0.1 + i * 0.06, pos[1] + 0.101); g.add(btn)
  }
  const slotMat = new THREE.MeshStandardMaterial({ color: 0x222233, metalness: 0.8, roughness: 0.2 })
  const slot = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.008, 0.003), slotMat)
  slot.position.set(pos[0] + 0.12, 0.46, pos[1] + 0.101); g.add(slot)
  return g
}

// ---- New: Bulletin board ----
function createBulletinBoard(pos) {
  const g = new THREE.Group()
  const bMat = new THREE.MeshStandardMaterial({ color: 0xd4c8b0, roughness: 0.6 })
  const board = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.35, 0.015), bMat)
  board.position.set(pos[0], pos[1], pos[2]); g.add(board)
  const lineMat = new THREE.MeshBasicMaterial({ color: 0xbbb5a0, transparent: true, opacity: 0.1 })
  for (let i = 0; i < 5; i++) {
    const hl = new THREE.Mesh(new THREE.PlaneGeometry(0.4, 0.002), lineMat)
    hl.position.set(pos[0], pos[1] - 0.12 + i * 0.06, pos[2] + 0.008); g.add(hl)
  }
  const noteColors = [0xffeecc, 0xddffcc, 0xffddcc, 0xccddff, 0xffccdd]
  for (let i = 0; i < 5; i++) {
    const nm = new THREE.MeshStandardMaterial({ color: noteColors[i], roughness: 0.7 })
    const n = new THREE.Mesh(new THREE.BoxGeometry(0.04 + Math.random() * 0.03, 0.03 + Math.random() * 0.02, 0.002), nm)
    n.position.set(pos[0] - 0.16 + (i % 3) * 0.15, pos[1] + 0.08 - Math.floor(i / 3) * 0.12, pos[2] + 0.009)
    n.rotation.z = (Math.random() - 0.5) * 0.08; g.add(n)
    const pinMat = new THREE.MeshStandardMaterial({ color: 0xff4444, metalness: 0.3 })
    const pin = new THREE.Mesh(new THREE.SphereGeometry(0.003, 4, 4), pinMat)
    pin.position.set(pos[0] - 0.16 + (i % 3) * 0.15, pos[1] + 0.08 - Math.floor(i / 3) * 0.12, pos[2] + 0.012); g.add(pin)
  }
  return { group: g, labelText: "NOTICE" }
}

// ---- New: Wall clock ----
function createWallClock(pos) {
  const g = new THREE.Group()
  const faceMat = new THREE.MeshStandardMaterial({ color: 0xf5f0e8, roughness: 0.3 })
  const face = new THREE.Mesh(new THREE.CircleGeometry(0.07, 16), faceMat)
  face.position.set(pos[0], pos[1], pos[2]); g.add(face)
  const rimMat = new THREE.MeshStandardMaterial({ color: 0x5a4a3a, roughness: 0.2, metalness: 0.3 })
  const rim = new THREE.Mesh(new THREE.TorusGeometry(0.07, 0.005, 8, 16), rimMat)
  rim.position.set(pos[0], pos[1], pos[2] - 0.001); g.add(rim)
  const markMat = new THREE.MeshBasicMaterial({ color: 0x333333 })
  for (let h = 0; h < 12; h++) {
    const a = (h / 12) * Math.PI * 2 - Math.PI / 2
    const mk = new THREE.Mesh(new THREE.PlaneGeometry(0.003, 0.008), markMat)
    mk.position.set(pos[0] + Math.cos(a) * 0.055, pos[1] + Math.sin(a) * 0.055, pos[2] - 0.002); g.add(mk)
  }
  const handMat = new THREE.MeshBasicMaterial({ color: 0x222222 })
  const minHand = new THREE.Mesh(new THREE.PlaneGeometry(0.002, 0.04), handMat)
  minHand.position.set(pos[0], pos[1] + 0.02, pos[2] - 0.002); g.add(minHand)
  const hrHand = new THREE.Mesh(new THREE.PlaneGeometry(0.002, 0.025), handMat)
  hrHand.position.set(pos[0] + 0.008, pos[1] + 0.01, pos[2] - 0.002); hrHand.rotation.z = 0.5; g.add(hrHand)
  const dotMat = new THREE.MeshStandardMaterial({ color: 0x333333, metalness: 0.5 })
  const dot = new THREE.Mesh(new THREE.SphereGeometry(0.003, 6, 6), dotMat)
  dot.position.set(pos[0], pos[1], pos[2] - 0.002); g.add(dot)
  return g
}

// ---- New: Coat rack ----
function createCoatRack(pos) {
  const g = new THREE.Group()
  const rMat = new THREE.MeshStandardMaterial({ color: 0x8b7d6b, roughness: 0.5, metalness: 0.2 })
  const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.01, 0.015, 0.25, 8), rMat)
  pole.position.set(pos[0], 0.125, pos[1]); g.add(pole)
  const baseMat = new THREE.MeshStandardMaterial({ color: 0x6b5d4f, roughness: 0.4, metalness: 0.3 })
  const base = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.06, 0.015, 8), baseMat)
  base.position.set(pos[0], 0.008, pos[1]); g.add(base)
  for (let i = 0; i < 3; i++) {
    const a = (i / 3) * Math.PI * 2
    const hook = new THREE.Mesh(new THREE.CylinderGeometry(0.003, 0.004, 0.02, 4), rMat)
    hook.position.set(pos[0] + Math.cos(a) * 0.015, 0.24, pos[1] + Math.sin(a) * 0.015)
    hook.rotation.z = Math.PI / 2 + a * 0.5; g.add(hook)
  }
  return g
}

// ---- New: Filing cabinet ----
function createFilingCabinet(pos) {
  const g = new THREE.Group()
  const cMat = new THREE.MeshStandardMaterial({ color: 0x7a7a8a, roughness: 0.3, metalness: 0.5 })
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.3, 0.14), cMat)
  body.position.set(pos[0], 0.15, pos[1]); g.add(body)
  const drawMat = new THREE.MeshStandardMaterial({ color: 0x6a6a7a, roughness: 0.3, metalness: 0.4 })
  for (let i = 0; i < 3; i++) {
    const dr = new THREE.Mesh(new THREE.BoxGeometry(0.10, 0.06, 0.003), drawMat)
    dr.position.set(pos[0], 0.05 + i * 0.08, pos[1] + 0.072); g.add(dr)
    const hdl = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.003, 0.005), new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0.6 }))
    hdl.position.set(pos[0], 0.05 + i * 0.08, pos[1] + 0.075); g.add(hdl)
  }
  return g
}

// ---- New: Desk drawer unit ----
function createDrawerUnit(pos) {
  const g = new THREE.Group()
  const cMat = new THREE.MeshStandardMaterial({ color: 0x8a7d6a, roughness: 0.4, metalness: 0.1 })
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.12, 0.08), cMat)
  body.position.set(pos[0], 0.06, pos[1]); g.add(body)
  for (let i = 0; i < 3; i++) {
    const d = new THREE.Mesh(new THREE.BoxGeometry(0.065, 0.025, 0.002), new THREE.MeshStandardMaterial({ color: 0x7a6d5a, roughness: 0.5 }))
    d.position.set(pos[0], 0.02 + i * 0.035, pos[1] + 0.041); g.add(d)
  }
  return g
}
function createDeskWithCubicle(dp) {
  const g = new THREE.Group()
  const x = dp.x, z = dp.z
  const agColor = new THREE.Color(AGENT_COLORS[dp.role] || "#6c5ce7")
  const deskMat = new THREE.MeshStandardMaterial({ color: 0xb8a88b, roughness: 0.3, metalness: 0.2, transparent: true, opacity: 0.9, emissive: new THREE.Color(0x6c5ce7), emissiveIntensity: 0.01 })
  const desk = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.035, 0.6), deskMat)
  desk.position.set(x, 0.02, z); desk.receiveShadow = true; desk.castShadow = true; g.add(desk)

  const legMat = new THREE.MeshStandardMaterial({ color: 0x6b5d4f, metalness: 0.8, roughness: 0.2 })
  for (let lx = -1; lx <= 1; lx += 2) {
    for (let lz = -1; lz <= 1; lz += 2) {
      const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.04, 6), legMat)
      leg.position.set(x + lx * 0.35, 0.02, z + lz * 0.22); g.add(leg)
    }
  }

  const pMat = new THREE.MeshStandardMaterial({ color: 0xd8d0be, roughness: 0.6, metalness: 0.0, transparent: true, opacity: 0.25, side: THREE.DoubleSide })
  const back = new THREE.Mesh(new THREE.BoxGeometry(1, 0.18, 0.015), pMat)
  back.position.set(x, 0.12, z - 0.33); g.add(back)
  for (let side = -1; side <= 1; side += 2) {
    const sp = new THREE.Mesh(new THREE.BoxGeometry(0.015, 0.18, 0.65), pMat)
    sp.position.set(x + side * 0.46, 0.12, z); g.add(sp)
  }

  const glow = new THREE.Mesh(new THREE.RingGeometry(0.25, 0.42, 24), new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.04, blending: THREE.AdditiveBlending, side: THREE.DoubleSide }))
  glow.rotation.x = -Math.PI / 2; glow.position.set(x, 0.025, z); g.add(glow)
  return g
}

function resetCamera() {
  if (!controls) return
  camera.position.set(7, 5.5, 9)
  controls.target.set(0, 0.2, 1.5)
  controls.update()
}

// Scene initialization with full office layout
function initScene() {
  const el = container.value
  if (!el) return
  const w = el.clientWidth, h = el.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf0ece4)
  scene.fog = new THREE.Fog(0xf0ece4, 14, 24)

  camera = new THREE.PerspectiveCamera(38, w / h, 0.1, 40)
  camera.position.set(7, 5.5, 9)
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1
  el.appendChild(renderer.domElement)

  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(w, h)
  labelRenderer.domElement.style.position = "absolute"
  labelRenderer.domElement.style.top = "0"
  labelRenderer.domElement.style.pointerEvents = "none"
  el.appendChild(labelRenderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 4
  controls.maxDistance = 20
  controls.maxPolarAngle = Math.PI / 2.2
  controls.target.set(0, 0.2, 1.5)

  // Lighting
  scene.add(new THREE.AmbientLight(0x303050, 0.6))
  const sun = new THREE.DirectionalLight(0xffeedd, 1.8)
  sun.position.set(6, 12, 4); sun.castShadow = true
  sun.shadow.mapSize.width = 1024; sun.shadow.mapSize.height = 1024
  const sd = 10; sun.shadow.camera.left = -sd; sun.shadow.camera.right = sd
  sun.shadow.camera.top = sd; sun.shadow.camera.bottom = -sd
  sun.shadow.camera.near = 1; sun.shadow.camera.far = 25
  scene.add(sun)

  const fill = new THREE.DirectionalLight(0x8888ff, 0.3)
  fill.position.set(-5, 4, -6); scene.add(fill)
  const warm = new THREE.DirectionalLight(0xff8844, 0.2)
  warm.position.set(8, 2, -3); scene.add(warm)
  const spot = new THREE.SpotLight(0x6c5ce7, 0.15, 10, Math.PI / 4, 0.5, 1)
  spot.position.set(0, 4, 0); scene.add(spot)

  // Floor
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(14, 12), new THREE.MeshStandardMaterial({ color: 0xd4c9b0, roughness: 0.8 }))
  floor.rotation.x = -Math.PI / 2; floor.position.set(0, -0.01, 1.5); floor.receiveShadow = true; scene.add(floor)

  // Tile grid
  const gridG = new THREE.Group()
  const tileMat = new THREE.LineBasicMaterial({ color: 0xbbb5a0, transparent: true, opacity: 0.2 })
  for (let i = -7; i <= 7; i++) {
    gridG.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(i, 0.001, -4.5), new THREE.Vector3(i, 0.001, 7.5)]), tileMat))
  }
  for (let j = -4.5; j <= 7.5; j += 1) {
    gridG.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(-7, 0.001, j), new THREE.Vector3(7, 0.001, j)]), tileMat))
  }
  gridG.position.set(0, 0.001, 1.5); scene.add(gridG)

  // Carpet zones
  const carpetMat = new THREE.MeshStandardMaterial({ color: 0xc8c0b0, roughness: 0.9, transparent: true, opacity: 0.35 })
  for (let row = -1; row <= 1; row += 2) {
    const c = new THREE.Mesh(new THREE.PlaneGeometry(2.5, 1.5), carpetMat)
    c.rotation.x = -Math.PI / 2; c.position.set(0, 0.005, row * 1.8 + 1.5); scene.add(c)
  }

  // Walls
  const wallMat = new THREE.MeshStandardMaterial({ color: 0xe8e0d0, roughness: 0.7, side: THREE.DoubleSide })
  const bw = new THREE.Mesh(new THREE.BoxGeometry(10, 2.5, 0.08), wallMat)
  bw.position.set(0, 1.25, -3.5); scene.add(bw)

  const glassMat = new THREE.MeshStandardMaterial({ color: 0xd8d0c0, roughness: 0.5, metalness: 0.1, transparent: true, opacity: 0.25, side: THREE.DoubleSide })
  const lw = new THREE.Mesh(new THREE.BoxGeometry(0.08, 2.5, 12), glassMat)
  lw.position.set(-5, 1.25, 1.5); scene.add(lw)
  const rw = new THREE.Mesh(new THREE.BoxGeometry(0.08, 2.5, 12), glassMat)
  rw.position.set(5, 1.25, 1.5); scene.add(rw)
  for (let wx of [-3.75, 3.75]) {
    const fw = new THREE.Mesh(new THREE.BoxGeometry(2.5, 2.5, 0.08), wallMat)
    fw.position.set(wx, 1.25, 6.5); scene.add(fw)
  }

  // Window glow strips
  const winGlow = new THREE.MeshBasicMaterial({ color: 0x88ccff, transparent: true, opacity: 0.08, side: THREE.DoubleSide })
  for (let wy = 0.4; wy < 2.2; wy += 0.6) {
    const wg = new THREE.Mesh(new THREE.PlaneGeometry(0.1, 0.4), winGlow)
    wg.position.set(-4.97, wy, 1.5); scene.add(wg)
    const wg2 = wg.clone(); wg2.position.x = 4.97; scene.add(wg2)
  }

  // Reception desk
  const recDesk = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.04, 0.4), new THREE.MeshStandardMaterial({ color: 0x9a8b78, roughness: 0.4, metalness: 0.3, emissive: new THREE.Color(0x6c5ce7), emissiveIntensity: 0.02 }))
  recDesk.position.set(-3.5, 0.02, 5.8); scene.add(recDesk)
  const rLeg = new THREE.MeshStandardMaterial({ color: 0x6b5d4f, metalness: 0.8 })
  for (let rx = -1; rx <= 1; rx += 2) {
    const l = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.04, 6), rLeg)
    l.position.set(-3.5 + rx * 0.35, 0.02, 5.8); scene.add(l)
  }
  const recDiv = document.createElement("div"); recDiv.textContent = "Reception"
  recDiv.style.cssText = "font-size:10px;font-weight:600;color:rgba(108,92,231,0.5);letter-spacing:1px;text-transform:uppercase"
  const recLbl = new CSS2DObject(recDiv); recLbl.position.set(-3.5, 0.2, 5.8); scene.add(recLbl)

  // Meeting room (back right)
  const mf = new THREE.Mesh(new THREE.PlaneGeometry(2.2, 1.8), new THREE.MeshStandardMaterial({ color: 0xddd4c0, transparent: true, opacity: 0.4 }))
  mf.rotation.x = -Math.PI / 2; mf.position.set(3.2, 0.005, 5.2); scene.add(mf)

  const gl = new THREE.MeshStandardMaterial({ color: 0x99badd, roughness: 0.05, metalness: 0.2, transparent: true, opacity: 0.12, side: THREE.DoubleSide })
  for (let mg of [{ p: [3.2, 1, 4.3], s: [2.2, 1.8, 0.03] }, { p: [4.2, 1, 5.2], s: [0.03, 1.8, 1.8] }, { p: [2.2, 1, 5.2], s: [0.03, 1.8, 1.8] }]) {
    const g = new THREE.Mesh(new THREE.BoxGeometry(mg.s[0], mg.s[1], mg.s[2]), gl)
    g.position.set(mg.p[0], mg.p[1], mg.p[2]); scene.add(g)
  }

  const mt = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.04, 0.8), new THREE.MeshStandardMaterial({ color: 0x8b7d6b, roughness: 0.6, metalness: 0.1 }))
  mt.position.set(3.2, 0.04, 5.2); mt.receiveShadow = true; mt.castShadow = true; scene.add(mt)

  const mcMat = new THREE.MeshStandardMaterial({ color: 0x7a6b5a })
  for (let ms = -1; ms <= 1; ms += 2) {
    for (let mt2 = -1; mt2 <= 1; mt2 += 2) {
      const ch = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.06, 0.04, 8), mcMat)
      ch.position.set(3.2 + ms * 0.5, 0.02, 5.2 + mt2 * 0.5); scene.add(ch)
    }
  }

  const mDiv = document.createElement("div"); mDiv.textContent = "Meeting Room"
  mDiv.style.cssText = "font-size:10px;font-weight:600;color:rgba(108,92,231,0.4)"
  const mLbl = new CSS2DObject(mDiv); mLbl.position.set(3.2, 0.1, 4.4); scene.add(mLbl)

  // Enhanced: Whiteboard
  const wBoard = createWhiteboard()
  scene.add(wBoard)
  // Enhanced: Projection screen and ceiling projector
  const pScreen = createProjectionScreen()
  scene.add(pScreen)


  // Break area (back left)
  const sofaMat = new THREE.MeshStandardMaterial({ color: 0x8b7d6b, roughness: 0.8 })
  const ss = new THREE.Mesh(new THREE.BoxGeometry(0.7, 0.06, 0.3), sofaMat)
  ss.position.set(-3.2, 0.05, 5.8); ss.castShadow = true; scene.add(ss)
  const sb = new THREE.Mesh(new THREE.BoxGeometry(0.7, 0.12, 0.06), sofaMat)
  sb.position.set(-3.2, 0.11, 5.95); scene.add(sb)
  const ct = new THREE.Mesh(new THREE.CylinderGeometry(0.15, 0.2, 0.04, 12), new THREE.MeshStandardMaterial({ color: 0x7a6b5a, roughness: 0.4, metalness: 0.2 }))
  ct.position.set(-3.2, 0.02, 5.3); scene.add(ct)

  // Enhanced: Coffee machine
  scene.add(createCoffeeMachine([-2.5, 5.8]))
  // Water dispenser
  scene.add(createWaterDispenser([-4.2, 5.8]))
  // Microwave
  scene.add(createMicrowave([-1.8, 5.8]))
  // Coat rack near break area
  scene.add(createCoatRack([-4.2, 4.5]))
  // Vending machine (back right corner)
  scene.add(createVendingMachine([4.5, 4.0]))

  // Bulletin board on back wall
  const bb = createBulletinBoard([3.5, 1.4, -3.0])
  scene.add(bb.group)
  // Wall clock above reception
  scene.add(createWallClock([-4.5, 2.0, 5.5]))
  ct.position.set(-3.2, 0.02, 5.3); scene.add(ct)

  // Plants
  for (let pp of [{ x: -4.5, z: -2.5 }, { x: -4.5, z: 0.5 }, { x: -4, z: 6 }, { x: 4, z: 3 }, { x: 0.5, z: -3 }]) {
    scene.add(createOfficePlant(pp, 0.8 + Math.random() * 0.3))
  }

  // Ceiling light fixtures
  const lfMat = new THREE.MeshStandardMaterial({ color: 0x6b5d4f, transparent: true, opacity: 0.3 })
  const lgMat = new THREE.MeshBasicMaterial({ color: 0xffeeaa, transparent: true, opacity: 0.12, blending: THREE.AdditiveBlending, side: THREE.DoubleSide })
  for (let fp of [[-2, -1.5], [2, -1.5], [-2, 1.5], [2, 1.5], [0, 3.5], [-3, 5.5]]) {
    const bar = new THREE.Mesh(new THREE.BoxGeometry(0.01, 1.2, 0.04), lfMat)
    bar.position.set(fp[0], 2.2, fp[1]); scene.add(bar)
    const glow = new THREE.Mesh(new THREE.PlaneGeometry(0.6, 0.02), lgMat)
    glow.position.set(fp[0], 2.18, fp[1]); scene.add(glow)
  }

  

  // Filing cabinet near workstation area
  scene.add(createFilingCabinet([-2.8, -0.5]))
  scene.add(createFilingCabinet([2.8, -0.5]))
  // Drawer units
  scene.add(createDrawerUnit([-1.5, -1.3]))
  scene.add(createDrawerUnit([1.5, -1.3]))
// Create workstations with agents
  DESK_POSITIONS.forEach(function(dp) {
    const x = dp.x, z = dp.z, agColor = new THREE.Color(AGENT_COLORS[dp.role])
    scene.add(createDeskWithCubicle(dp))

    const group = new THREE.Group()
    group.position.set(x, 0, z)
    group.userData.role = dp.role
    group.userData.floatOffset = Math.random() * Math.PI * 2
    group.userData.floatSpeed = 0.5 + Math.random() * 0.3

    // Seated body
    const seatedBody = createSeatedHumanBody(AGENT_COLORS[dp.role])
    seatedBody.userData.bodyVariant = "seated"
    seatedBody.scale.setScalar(0.65); seatedBody.position.set(0, 0.05, 0.15); group.add(seatedBody)
    // Standing body (hidden by default)
    const standingBody = createStandingHumanBody(AGENT_COLORS[dp.role])
    standingBody.userData.bodyVariant = "standing"
    standingBody.scale.setScalar(0.65); standingBody.position.set(0, 0.05, 0.15); standingBody.visible = false; group.add(standingBody)
    // Accessories
    const accessory = createRoleAccessory(dp.role, AGENT_COLORS[dp.role])
    seatedBody.add(accessory)
    const accessoryStand = createRoleAccessory(dp.role, AGENT_COLORS[dp.role])
    standingBody.add(accessoryStand)

    const compGroup = createDeskComputer()
    compGroup.scale.setScalar(0.7); compGroup.position.set(0.04, 0.04, 0.12); compGroup.userData.isComputer = true
    group.add(compGroup)

    const glowRing = new THREE.Mesh(new THREE.RingGeometry(0.06, 0.2, 20), new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.08, blending: THREE.AdditiveBlending, side: THREE.DoubleSide }))
    glowRing.rotation.x = -Math.PI / 2; glowRing.position.y = -0.14; group.add(glowRing)

    const particleGroup = new THREE.Group()
    for (let i = 0; i < 4; i++) {
      const pt = new THREE.Mesh(new THREE.SphereGeometry(0.008, 6, 6), new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0.15, blending: THREE.AdditiveBlending }))
      const a = (i / 4) * Math.PI * 2
      pt.position.set(Math.cos(a) * 0.18, Math.sin(a * 2) * 0.08, Math.sin(a) * 0.18)
      pt.userData.orbitAngle = a; pt.userData.orbitSpeed = 0.3 + Math.random() * 0.2; pt.userData.orbRadius = 0.15 + Math.random() * 0.08
      particleGroup.add(pt)
    }
    group.add(particleGroup)

    // Desk decorations on each desk
    group.add(createDeskOrganizer())
    group.add(createDeskCalendar())
    group.add(createDeskCoffeeCup([0.38, 0.22]))

    const sr = new THREE.Mesh(new THREE.RingGeometry(0.35, 0.42, 24), new THREE.MeshBasicMaterial({ color: agColor, transparent: true, opacity: 0, side: THREE.DoubleSide, blending: THREE.AdditiveBlending }))
    sr.rotation.x = -Math.PI / 2; sr.position.y = -0.02; sr.userData.isStatusRing = true; group.add(sr)
    scene.add(group)

    const d = { group, currentStatus: "idle", isWorking: false, isAtMeeting: false, homePos: new THREE.Vector3(x, 0, z) }
    agentData[dp.role] = d

    // Label
    const lblDiv = document.createElement("div"); lblDiv.className = "label-3d"
    const icon = "\u{1F9D1}\u200D\u{1F4BB}"
    lblDiv.innerHTML = '<div style="text-align:center;font-size:11px;font-weight:600;color:#2a2620;text-shadow:0 0 12px rgba(0,0,0,0.1);background:linear-gradient(135deg,rgba(240,236,228,0.9),rgba(230,222,212,0.9));padding:4px 10px;border-radius:10px;border:1px solid ' + AGENT_COLORS[dp.role] + '55;box-shadow:0 0 16px ' + AGENT_COLORS[dp.role] + '22;">' + icon + ' ' + dp.label + '<br><span class="label-status" style="font-size:9px;color:#4ade80;">\u25CF Online</span></div>'
    const lbl = new CSS2DObject(lblDiv); lbl.position.set(x, 1.25, z); scene.add(lbl); d.label = lbl
  })

  updateCollaborationLines()
}

function updateCollaborationLines(links) {
  collaborLines.forEach(function(l) { scene.remove(l); l.geometry.dispose(); l.material.dispose() })
  collaborLines = []
  const defaults = links || [{ from: "orchestrator", to: "calculator" }, { from: "orchestrator", to: "weather" }, { from: "orchestrator", to: "searcher" }, { from: "orchestrator", to: "summarizer" }]
  defaults.forEach(function(link) {
    const fp = DESK_POSITIONS.find(p => p.role === link.from)
    const tp = DESK_POSITIONS.find(p => p.role === (link.to || link))
    if (!fp || !tp) return
    const s = new THREE.Vector3(fp.x, 0.1, fp.z), e = new THREE.Vector3(tp.x, 0.1, tp.z)
    const m = new THREE.Vector3().addVectors(s, e).multiplyScalar(0.5); m.y += 0.15
    const pts = new THREE.QuadraticBezierCurve3(s, m, e).getPoints(20)
    const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints(pts), new THREE.LineBasicMaterial({ color: link.color || 0x6c5ce7, transparent: true, opacity: 0.2 }))
    line.userData = { from: link.from, to: link.to || link }; scene.add(line); collaborLines.push(line)
  })
}

function flashAgent(role, color, duration) {
  if (!color) color = "#ffffff"
  if (!duration) duration = 300
  const d = agentData[role]
  if (!d || !d.group) return
  const core = d.group.children.find(c => c.userData && c.userData.isHuman)
  if (!core) return
  const orig = core.material.color.clone(), origEm = core.material.emissive.clone()
  core.material.color.set(color); core.material.emissive.set(color)
  setTimeout(function() { if (core.material) { core.material.color.copy(orig); core.material.emissive.copy(origEm) } }, duration)
}

function highlightConnection(fromRole, toRole, intensity) {
  if (intensity === undefined) intensity = 1
  collaborLines.forEach(function(l) {
    const active = l.userData && l.userData.from === fromRole && l.userData.to === toRole
    l.material.opacity = active ? 0.9 * intensity : 0.3
    if (active) l.material.color.set("#a78bfa"); else l.material.color.set("#6c5ce7")
  })
}

// Animation loop
function animate() {
  animFrameId = requestAnimationFrame(animate)
  const delta = clock.getDelta(), time = clock.getElapsedTime()

  // Data packets
  for (let i = dataPackets.length - 1; i >= 0; i--) {
    const p = dataPackets[i]; p.progress += p.speed
    if (p.progress > 1) {
      scene.remove(p.mesh); p.mesh.geometry.dispose(); p.mesh.material.dispose()
      p.trail.forEach(function(t) { scene.remove(t); t.geometry.dispose(); t.material.dispose() })
      dataPackets.splice(i, 1); continue
    }
    const pt = p.curve.getPoint(p.progress); p.mesh.position.copy(pt)
    p.trail.forEach(function(t, ti) {
      const tp = p.progress - (ti + 1) * 0.04
      if (tp > 0) { t.position.copy(p.curve.getPoint(tp)); t.visible = true } else t.visible = false
    })
  }

  // Agents
  Object.keys(agentData).forEach(function(role) {
    const d = agentData[role], grp = d.group
    if (!grp) return
    const fo = grp.userData.floatOffset || 0
    if (!grp.userData._baseY) grp.userData._baseY = d.homePos.y
    const fy = Math.sin(time * (grp.userData.floatSpeed || 0.5) + fo) * 0.04

    // Find active body variant
    const humanBody = grp.children.find(function(c) { return c.userData && c.userData.bodyVariant && c.visible })
    if (humanBody) {
      const isStanding = humanBody.userData.bodyVariant === "standing"
      if (!isStanding) {
        humanBody.rotation.y = Math.sin(time * 0.3 + fo) * 0.02
      }
      // Running animation for standing body when agent is moving
      if (isStanding && d.moveAnim) {
        const runPhase = time * 6 + fo
        const leftLeg = humanBody.getObjectByName("leftLegPivot")
        const rightLeg = humanBody.getObjectByName("rightLegPivot")
        const leftArm = humanBody.getObjectByName("leftArmPivot")
        const rightArm = humanBody.getObjectByName("rightArmPivot")
        if (leftLeg) leftLeg.rotation.x = Math.sin(runPhase) * 0.6
        if (rightLeg) rightLeg.rotation.x = Math.sin(runPhase + Math.PI) * 0.6
        if (leftArm) leftArm.rotation.x = Math.sin(runPhase + Math.PI) * 0.5
        if (rightArm) rightArm.rotation.x = Math.sin(runPhase) * 0.5
        humanBody.rotation.x = -0.08
        const bounce = Math.abs(Math.sin(runPhase)) * 0.04
        humanBody.position.y = -0.02 + bounce
      } else if (isStanding && !d.moveAnim) {
        humanBody.rotation.x = 0
        humanBody.position.y = 0
      }
    }

    grp.children.forEach(function(ch) {
      if (ch.position && Math.abs(ch.position.y + 0.14) < 0.01 && ch.material && ch.material.blending === THREE.AdditiveBlending)
        ch.material.opacity = 0.06 + Math.sin(time * 1.2 + fo) * 0.04
      if (ch.userData && ch.userData.isStatusRing && ch.visible)
        ch.material.opacity = 0.3 + Math.sin(time * 2) * 0.2
    })

    const pg = grp.children.find(function(c) { return c.type === "Group" && c.children.length > 0 && c.children[0].userData && c.children[0].userData.orbitAngle !== undefined })
    if (pg) {
      pg.children.forEach(function(pt) {
        const a = (pt.userData.orbitAngle || 0) + time * (pt.userData.orbitSpeed || 0.3)
        const rad = pt.userData.orbRadius || 0.18
        pt.position.set(Math.cos(a) * rad, Math.sin(a * 2 + fo) * 0.08, Math.sin(a) * rad)
        pt.material.opacity = 0.1 + Math.sin(time * 1.5 + pt.userData.orbitAngle) * 0.08
      })
    }

    // Movement
    if (d.moveAnim) {
      d.moveAnim.progress += d.moveAnim.speed * delta * 60
      if (d.moveAnim.progress >= 1) {
        grp.position.copy(d.moveAnim.endPos); grp.userData._baseY = d.moveAnim.endPos.y
        const cb = d.moveAnim.onArrive; d.moveAnim = null; if (cb) cb()
      } else {
        const t = d.moveAnim.progress; grp.position.lerpVectors(d.moveAnim.startPos, d.moveAnim.endPos, t * t * (3 - 2 * t))
      }
    }

    const base = grp.userData._baseY !== undefined ? grp.userData._baseY : 0
    grp.position.y = base + fy
    if (d.isWorking && !d.moveAnim) grp.position.y = base + fy + Math.sin(time * 3 + fo) * 0.02
    // Extra bounce when moving
    if (d.moveAnim) {
      grp.position.y = base + fy + Math.abs(Math.sin(time * 6 + fo)) * 0.04
    }

    const sr = grp.children.find(function(c) { return c.userData && c.userData.isStatusRing })
    if (sr) {
      if (d.currentStatus === "speaking" || d.currentStatus === "tool_calling") {
        sr.visible = true; sr.material.opacity = 0.3 + Math.sin(time * 3) * 0.2
        sr.scale.setScalar(1 + Math.sin(time * 2.5) * 0.03)
      } else sr.visible = false
    }

    if (d.label) {
      d.label.position.x = grp.position.x; d.label.position.z = grp.position.z; d.label.position.y = grp.position.y + 0.7
    }
  })

  // Auto-rotate
  if (autoRotate.value && controls) {
    const pivot = new THREE.Vector3(0, 0.2, 1.5)
    const off = new THREE.Vector3().copy(camera.position).sub(pivot)
    const a = 0.0012 * delta * 60, c = Math.cos(a), s = Math.sin(a)
    camera.position.x = pivot.x + off.x * c - off.z * s
    camera.position.z = pivot.z + off.x * s + off.z * c
    camera.lookAt(pivot); controls.target.copy(pivot)
  }

  controls.update()
  renderer.render(scene, camera); labelRenderer.render(scene, camera)
}

function onResize() {
  if (!container.value || !renderer) return
  const w = container.value.clientWidth, h = container.value.clientHeight
  camera.aspect = w / h; camera.updateProjectionMatrix()
  renderer.setSize(w, h); labelRenderer.setSize(w, h)
}

onMounted(async function() { await nextTick(); initScene(); animate(); window.addEventListener("resize", onResize) })
onBeforeUnmount(function() {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  window.removeEventListener("resize", onResize)
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
  sendDataPacket, clearDataPackets, showSpeechBubble, updateAgentStatus, setConnectionStatus,
  moveAgentTo, returnAgentToDesk, getDeskX, getDeskZ, flashAgent, highlightConnection,
  updateCollaborationLines, resetCamera,
})
</script>

<style scoped>
.iso-office-container {
  position: relative;
  width: 100%; height: 100%;
  overflow: hidden;
  background: var(--bg-deep, #f0ece4);
}
.office-toolbar {
  position: absolute; top: 10px; left: 12px; right: 12px;
  display: flex; align-items: center; justify-content: space-between;
  z-index: 10; pointer-events: none;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-title { font-size: 13px; font-weight: 600; color: var(--text-primary, #3a3530); text-shadow: 0 0 12px rgba(0,0,0,0.5); }
.toolbar-status { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #6a6558; background: rgba(220,210,195,0.7); padding: 2px 10px 2px 8px; border-radius: 12px; border: 1px solid var(--border-color, rgba(108,92,231,0.2)); }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #6a6558; }
.toolbar-status.online .status-dot { background: #4ade80; box-shadow: 0 0 6px rgba(74,222,128,0.5); }
.toolbar-status.online { color: #4ade80; border-color: rgba(74,222,128,0.3); }
.toolbar-status.offline .status-dot { background: #f87171; }
.toolbar-status.offline { color: #f87171; border-color: rgba(248,113,113,0.3); }
.toolbar-right { display: flex; gap: 4px; pointer-events: auto; }
.tb-btn { width: 30px; height: 30px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: rgba(255,255,255,0.85); color: var(--text-secondary, #6a6558); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.tb-btn:hover { background: rgba(108,92,231,0.08); color: #6c5ce7; border-color: rgba(108,92,231,0.3); }
.tb-btn.active { background: rgba(108,92,231,0.25); color: #6c5ce7; border-color: rgba(108,92,231,0.5); }
.office-legend { position: absolute; bottom: 12px; left: 12px; display: flex; gap: 12px; z-index: 10; padding: 6px 12px; border-radius: 8px; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-color { width: 7px; height: 7px; border-radius: 50%; }
.legend-label { font-size: 10px; color: var(--text-secondary, #6a6558); }
:deep(.label-speech) { background: rgba(255,255,255,0.9); backdrop-filter: blur(12px); border: 1px solid rgba(0,0,0,0.08); border-radius: 10px; padding: 5px 12px; font-size: 12px; color: #2a2620; max-width: 200px; pointer-events: none; white-space: nowrap; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
:deep(.label-3d) { pointer-events: none; user-select: none; }
</style>