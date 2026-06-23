# encoding: utf-8
import sys
sys.stdout.reconfigure(encoding="utf-8")

# Read the ORIGINAL backup
with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue.bak", "rb") as f:
    iso_raw = f.read()

# The original is GB18030 encoded
try:
    text = iso_raw.decode("gb18030")
except:
    text = iso_raw.decode("utf-8", errors="replace")

print("Original length: %d" % len(text))

# Step 1: Add dataPackets variable after particleSystem
text = text.replace(
    "let collaborLines = []\nlet particleSystem = null",
    "let collaborLines = []\nlet particleSystem = null\n\nlet dataPackets = []"
)

# Step 2: Insert new functions after MEETING_ROOM_POS
new_funcs_code = """
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
"""

text = text.replace(
    "const MEETING_ROOM_POS = { x: 0, z: 5.5 }\n\nfunction updateAgentStatus",
    "const MEETING_ROOM_POS = { x: 0, z: 5.5 }\n" + new_funcs_code + "\nfunction updateAgentStatus"
)

print("Step 2 done")

# Step 3: Update updateCollaborationLines with active link support
old_collab = (
    "function updateCollaborationLines(links) {"
    "\n  collaborLines.forEach(l => { scene.remove(l); l.geometry.dispose(); l.material.dispose() })"
    "\n  collaborLines = []"
    "\n  if (particleSystem) { scene.remove(particleSystem); particleSystem.geometry.dispose(); particleSystem.material.dispose(); particleSystem = null }"
    "\n  if (!links || links.length === 0) return"
    "\n  const positions = []"
    "\n  links.forEach(link => {"
    "\n    const fromPos = DESK_POSITIONS.find(p => p.role === link.from)"
    "\n    const toPos = DESK_POSITIONS.find(p => p.role === link.to)"
    "\n    if (!fromPos || !toPos) return"
    "\n    const curve = new THREE.LineCurve3(new THREE.Vector3(fromPos.x, 0.3, fromPos.z), new THREE.Vector3(toPos.x, 0.3, toPos.z))"
    "\n    const pts = curve.getPoints(20)"
    "\n    const geometry = new THREE.BufferGeometry().setFromPoints(pts)"
    "\n    const material = new THREE.LineDashedMaterial({ color: link.color || '#6c5ce7', dashSize: 0.15, gapSize: 0.1, transparent: true, opacity: 0.6 })"
    "\n    const line = new THREE.Line(geometry, material)"
    "\n    line.computeLineDistances(); scene.add(line); collaborLines.push(line)"
    "\n    pts.forEach(p => positions.push(p.x, p.y, p.z))"
    "\n  })"
    "\n  if (positions.length > 0) {"
    "\n    const particleGeo = new THREE.BufferGeometry()"
    "\n    particleGeo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))"
    "\n    particleSystem = new THREE.Points(particleGeo, new THREE.PointsMaterial({ color: '#8b7cf7', size: 0.08, transparent: true, opacity: 0.7, blending: THREE.AdditiveBlending }))"
    "\n    scene.add(particleSystem)"
    "\n  }"
    "\n}"
    "\n"
)

new_collab = (
    "function updateCollaborationLines(links) {"
    "\n  collaborLines.forEach(l => { scene.remove(l); l.geometry.dispose(); l.material.dispose() })"
    "\n  collaborLines = []"
    "\n  if (particleSystem) { scene.remove(particleSystem); particleSystem.geometry.dispose(); particleSystem.material.dispose(); particleSystem = null }"
    "\n  if (!links || links.length === 0) return"
    "\n  const positions = []"
    "\n  links.forEach(link => {"
    "\n    const color = link.active ? '#a78bfa' : (link.color || '#6c5ce7')"
    "\n    const opacity = link.active ? 0.9 : 0.4"
    "\n    const fromPos = DESK_POSITIONS.find(p => p.role === link.from)"
    "\n    const toPos = DESK_POSITIONS.find(p => p.role === link.to)"
    "\n    if (!fromPos || !toPos) return"
    "\n    const curve = new THREE.LineCurve3(new THREE.Vector3(fromPos.x, 0.3, fromPos.z), new THREE.Vector3(toPos.x, 0.3, toPos.z))"
    "\n    const pts = curve.getPoints(20)"
    "\n    const geometry = new THREE.BufferGeometry().setFromPoints(pts)"
    "\n    const material = new THREE.LineDashedMaterial({ color, dashSize: 0.15, gapSize: 0.1, transparent: true, opacity })"
    "\n    const line = new THREE.Line(geometry, material)"
    "\n    line.userData = { from: link.from, to: link.to }"
    "\n    line.computeLineDistances(); scene.add(line); collaborLines.push(line)"
    "\n    if (link.active) {"
    "\n      pts.forEach(p => positions.push(p.x, p.y, p.z))"
    "\n      for (let i = 0; i < 5; i++) {"
    "\n        const pt = curve.getPoint(0.2 + i * 0.15)"
    "\n        positions.push(pt.x, pt.y + Math.sin(i) * 0.02, pt.z)"
    "\n      }"
    "\n    }"
    "\n  })"
    "\n  if (positions.length > 12) {"
    "\n    const particleGeo = new THREE.BufferGeometry()"
    "\n    particleGeo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))"
    "\n    particleSystem = new THREE.Points(particleGeo, new THREE.PointsMaterial({ color: '#a78bfa', size: 0.09, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending }))"
    "\n    scene.add(particleSystem)"
    "\n    particleSystem.userData.active = true"
    "\n  }"
    "\n}"
    "\n"
)

if old_collab in text:
    text = text.replace(old_collab, new_collab)
    print("Step 3: collab function replaced")
else:
    print("Step 3 WARNING: old collab function not found!")
    # Try to find the function differently
    idx = text.find("function updateCollaborationLines(links) {")
    if idx >= 0:
        print("Found at position %d" % idx)
        end_idx = text.find("\nfunction resetCamera()", idx)
        if end_idx >= 0:
            old = text[idx:end_idx]
            print("Old collab length: %d" % len(old))
            text = text[:idx] + new_collab + text[end_idx:]
            print("Step 3: replaced by position")
        else:
            print("Cannot find end boundary")

# Step 4: Update animate function - replace the agent loop to include meeting animation
old_loop = "Object.values(agentMeshes).forEach(m => { if (m.userData.isWorking) m.position.y = 0.25 + Math.sin(t * 3 + (m.userData.role || '').length) * 0.03 })"

new_loop = (
    "Object.values(agentMeshes).forEach(m => {"
    "\n    if (m.userData.isWorking) m.position.y = 0.25 + Math.sin(t * 3 + (m.userData.role || '').length) * 0.03"
    "\n    if (m.userData.meetingAnim) {"
    "\n      const a = m.userData.meetingAnim"
    "\n      a.progress += a.speed"
    "\n      if (a.progress >= 1) {"
    "\n        m.position.copy(a.endPos)"
    "\n        if (a.onArrive) a.onArrive()"
    "\n        delete m.userData.meetingAnim"
    "\n      } else {"
    "\n        m.position.lerpVectors(a.startPos, a.endPos, a.progress)"
    "\n        m.position.y += Math.sin(t * 4) * 0.02"
    "\n      }"
    "\n    }"
    "\n  })"
)

text = text.replace(old_loop, new_loop)
print("Step 4: animation loop updated")

# Step 5: Add data packet animation after particle system line in animate
old_particle = "if (particleSystem) { const pos = particleSystem.geometry.attributes.position.array; for (let i = 1; i < pos.length; i += 3) pos[i] += Math.sin(t * 0.5 + i) * 0.001; particleSystem.geometry.attributes.position.needsUpdate = true }"

new_particle_and_packets = (
    "if (particleSystem) { const pos = particleSystem.geometry.attributes.position.array; for (let i = 1; i < pos.length; i += 3) pos[i] += Math.sin(t * 0.5 + i) * 0.001; particleSystem.geometry.attributes.position.needsUpdate = true }"
    "\n  // Animate data packets"
    "\n  for (let i = dataPackets.length - 1; i >= 0; i--) {"
    "\n    const p = dataPackets[i]"
    "\n    p.progress += p.speed"
    "\n    if (p.progress >= 1) {"
    "\n      scene.remove(p.mesh); p.mesh.geometry.dispose(); p.mesh.material.dispose()"
    "\n      p.trail.forEach(t => { scene.remove(t); t.geometry.dispose(); t.material.dispose() })"
    "\n      dataPackets.splice(i, 1); continue"
    "\n    }"
    "\n    const pos = p.curve.getPoint(p.progress)"
    "\n    p.mesh.position.copy(pos)"
    "\n    const pulse = 1 + Math.sin(t * 12 + i * 2) * 0.25"
    "\n    p.mesh.scale.setScalar(pulse)"
    "\n    for (let j = 0; j < p.trailCount; j++) {"
    "\n      const tp = Math.max(0, p.progress - (j + 1) * 0.05)"
    "\n      if (tp > 0) { const tpos = p.curve.getPoint(tp); p.trail[j].position.copy(tpos); p.trail[j].visible = true; p.trail[j].material.opacity = 0.25 - j * 0.035 }"
    "\n      else { p.trail[j].visible = false }"
    "\n    }"
    "\n  }"
)

text = text.replace(old_particle, new_particle_and_packets)
print("Step 5: data packet animation added")

# Step 6: Update defineExpose
old_expose = 'defineExpose({ updateAgentStatus, setConnectionStatus, updateCollaborationLines, resetCamera })'
new_expose = (
    'defineExpose({\n'
    '  updateAgentStatus, setConnectionStatus, updateCollaborationLines, resetCamera,\n'
    '  sendDataPacket, clearDataPackets,\n'
    '  sendAgentToMeeting, returnAgentFromMeeting,\n'
    '  showSpeechBubble,\n'
    '  highlightConnection, flashAgent,\n'
    '})'
)
text = text.replace(old_expose, new_expose)
print("Step 6: defineExpose updated")

# Step 7: Add speech bubble CSS
old_css_end = '.label-meeting span { font-size: 11px; font-weight: 600; color: #8b7cf7; }\n</style>'
new_css_end = (
    '.label-meeting span { font-size: 11px; font-weight: 600; color: #8b7cf7; }\n\n'
    '.label-speech {\n'
    '  background: rgba(13,13,26,0.85); border-color: rgba(167,139,250,0.35);\n'
    '  font-size: 10px; font-weight: 500; color: #c4b5fd; max-width: 140px;\n'
    '  white-space: nowrap; text-overflow: ellipsis; overflow: hidden;\n'
    '  backdrop-filter: blur(8px); box-shadow: 0 0 12px rgba(167,139,250,0.15);\n'
    '  animation: speechFadeIn 0.3s ease-out;\n'
    '}\n'
    '@keyframes speechFadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }\n'
    '</style>'
)
text = text.replace(old_css_end, new_css_end)
print("Step 7: CSS added")

# Write the result as UTF-8 with BOM
output = b'\xef\xbb\xbf' + text.encode('utf-8')
with open("D:\\pj\\frontend\\src\\components\\IsometricOffice.vue", "wb") as f:
    f.write(output)

print("IsometricOffice.vue written successfully!")
print("Final length: %d" % len(text))
