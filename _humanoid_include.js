const ANIMALS = {
  orchestrator: 'human',
  calculator: 'human',
  weather: 'human',
  searcher: 'human',
  summarizer: 'human',
}

function createHumanoidAgent(color) {
  const g = new THREE.Group()
  const c = new THREE.Color(color)
  
  const bodyMat = new THREE.MeshStandardMaterial({
    color: c, roughness: 0.3, metalness: 0.15,
    emissive: c, emissiveIntensity: 0.03,
  })
  const skinMat = new THREE.MeshStandardMaterial({
    color: 0xf0d5b8, roughness: 0.6, metalness: 0.0,
  })
  const darkMat = new THREE.MeshStandardMaterial({
    color: 0x1a1a2e, roughness: 0.5,
  })
  const screenMat = new THREE.MeshStandardMaterial({
    color: 0x0a0a1a, roughness: 0.05, metalness: 0.9,
    emissive: new THREE.Color(0x6c5ce7),
    emissiveIntensity: 0.08,
  })

  // Chair
  const chairMat = new THREE.MeshStandardMaterial({ color: 0x1a1a33, roughness: 0.7, metalness: 0.1 })
  const chairBase = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.14, 0.04, 8), chairMat)
  chairBase.position.y = -0.04; g.add(chairBase)
  const chairPole = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.12, 6), darkMat)
  chairPole.position.y = 0.04; g.add(chairPole)
  const chairSeat = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.12, 0.03, 10), chairMat)
  chairSeat.position.y = 0.1; g.add(chairSeat)
  const chairBack = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.1, 0.02), chairMat)
  chairBack.position.set(0, 0.16, -0.1); g.add(chairBack)

  // Torso
  const torso = new THREE.Mesh(new THREE.CapsuleGeometry(0.06, 0.08, 6, 8), bodyMat)
  torso.position.y = 0.14; g.add(torso)

  // Head
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.05, 10, 10), skinMat)
  head.position.set(0, 0.23, 0); g.add(head)

  // Hair
  const hairMat = new THREE.MeshStandardMaterial({ color: c.clone().multiplyScalar(0.6), roughness: 0.8 })
  const hair = new THREE.Mesh(new THREE.SphereGeometry(0.048, 8, 8), hairMat)
  hair.scale.set(1.05, 0.4, 1.0); hair.position.set(0, 0.255, 0.005); g.add(hair)

  // Eyes
  const eyeMat = new THREE.MeshStandardMaterial({ color: 0x111111 })
  const eyeWhite = new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xffffff, emissiveIntensity: 0.1 })
  for (let s = -1; s <= 1; s += 2) {
    const ew = new THREE.Mesh(new THREE.SphereGeometry(0.016, 8, 8), eyeWhite)
    ew.scale.set(1, 0.7, 0.5); ew.position.set(s * 0.018, 0.24, -0.045); g.add(ew)
    const ep = new THREE.Mesh(new THREE.SphereGeometry(0.01, 6, 6), eyeMat)
    ep.position.set(s * 0.018, 0.24, -0.04); g.add(ep)
    const hl = new THREE.Mesh(new THREE.SphereGeometry(0.004, 6, 6), new THREE.MeshStandardMaterial({ color: 0xffffff }))
    hl.position.set(s * 0.021, 0.243, -0.04); g.add(hl)
  }

  // Arms resting on desk
  for (let s = -1; s <= 1; s += 2) {
    const arm = new THREE.Mesh(new THREE.CapsuleGeometry(0.018, 0.07, 6, 8), bodyMat)
    arm.position.set(s * 0.075, 0.11, 0.04); arm.rotation.z = s * 0.15; g.add(arm)
    const forearm = new THREE.Mesh(new THREE.CapsuleGeometry(0.015, 0.05, 6, 8), bodyMat)
    forearm.position.set(s * 0.075, 0.07, 0.08); forearm.rotation.x = -0.5; g.add(forearm)
    const hand = new THREE.Mesh(new THREE.SphereGeometry(0.014, 6, 6), skinMat)
    hand.position.set(s * 0.075, 0.06, 0.11); g.add(hand)
  }

  // Monitor stand + screen
  const monitorStand = new THREE.Mesh(new THREE.CylinderGeometry(0.01, 0.015, 0.06, 6), darkMat)
  monitorStand.position.set(0, -0.02, 0.14); g.add(monitorStand)
  const monitorScreen = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.09, 0.02), screenMat)
  monitorScreen.position.set(0, 0.03, 0.16); g.add(monitorScreen)
  const bezelMat = new THREE.MeshStandardMaterial({ color: 0x0a0a14, roughness: 0.3, metalness: 0.5 })
  const monitorBody = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.105, 0.025), bezelMat)
  monitorBody.position.set(0, 0.03, 0.155); g.add(monitorBody)

  // Screen glow
  const glowScreen = new THREE.Mesh(
    new THREE.PlaneGeometry(0.1, 0.07),
    new THREE.MeshBasicMaterial({ color: c, transparent: true, opacity: 0.06, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  glowScreen.position.set(0, 0.03, 0.172); g.add(glowScreen)

  // Keyboard
  const kbMat = new THREE.MeshStandardMaterial({ color: 0x111122, roughness: 0.5, metalness: 0.3 })
  const keyboard = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.01, 0.04), kbMat)
  keyboard.position.set(0, -0.025, 0.10); g.add(keyboard)

  // Desk surface
  const deskMat = new THREE.MeshStandardMaterial({
    color: 0x14142e, roughness: 0.1, metalness: 0.8,
    transparent: true, opacity: 0.75,
    emissive: c, emissiveIntensity: 0.015,
  })
  const deskSurface = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.015, 0.22), deskMat)
  deskSurface.position.y = -0.03; g.add(deskSurface)

  // Desk edge glow
  const edgeGlow = new THREE.Mesh(
    new THREE.RingGeometry(0.14, 0.16, 4),
    new THREE.MeshBasicMaterial({ color: c, transparent: true, opacity: 0.08, side: THREE.DoubleSide })
  )
  edgeGlow.rotation.x = -Math.PI / 2
  edgeGlow.position.y = -0.025; g.add(edgeGlow)

  g.children.forEach(function(ch) {
    if (ch.isMesh) { ch.userData.isCore = true; ch.userData.isHuman = true }
  })
  g.userData.agentType = 'human'
  return g
}
