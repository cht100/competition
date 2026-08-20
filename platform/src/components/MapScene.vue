<template>
  <div class="map-scene" ref="mapContainer">
    <div class="loading-overlay" v-if="loading">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>正在构建三维态势...</p>
      </div>
    </div>

    <div class="map-hud top-left">
      <div class="hud-label">INCIDENT TRACKER</div>
      <div class="hud-value">{{ activeCount }} <span>ACTIVE</span></div>
      <div class="hud-sub">{{ markerCount }} TOTAL</div>
    </div>

    <div class="map-legend">
      <div class="legend-title">
        <span class="legend-dot"></span>
        灾害类型
      </div>
      <div class="legend-item" v-for="item in disasterTypes" :key="item.type">
        <span class="legend-color" :style="{ backgroundColor: item.color, boxShadow: '0 0 8px ' + item.color }"></span>
        <span class="legend-label">{{ item.label }}</span>
      </div>
    </div>

    <div class="map-compass">
      <svg viewBox="0 0 80 80" width="80" height="80">
        <circle cx="40" cy="40" r="36" fill="rgba(3,8,16,0.6)" stroke="rgba(0,212,255,0.3)" stroke-width="1.5"/>
        <line x1="40" y1="6" x2="40" y2="22" stroke="rgba(0,212,255,0.6)" stroke-width="2"/>
        <line x1="40" y1="58" x2="40" y2="74" stroke="rgba(0,212,255,0.2)" stroke-width="1"/>
        <line x1="6" y1="40" x2="22" y2="40" stroke="rgba(0,212,255,0.2)" stroke-width="1"/>
        <line x1="58" y1="40" x2="74" y2="40" stroke="rgba(0,212,255,0.2)" stroke-width="1"/>
        <text x="40" y="20" text-anchor="middle" fill="#00d4ff" font-size="16" font-weight="bold">N</text>
        <text x="40" y="72" text-anchor="middle" fill="rgba(0,212,255,0.4)" font-size="10">S</text>
        <text x="12" y="44" text-anchor="middle" fill="rgba(0,212,255,0.4)" font-size="10">W</text>
        <text x="68" y="44" text-anchor="middle" fill="rgba(0,212,255,0.4)" font-size="10">E</text>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'

const props = defineProps({
  incidents: { type: Array, default: () => [] },
  lite: { type: Boolean, default: false }
})

const emit = defineEmits(['select-incident'])

const mapContainer = ref(null)
const loading = ref(true)
const markerCount = computed(() => props.incidents.length)
const activeCount = computed(() => {
  // 活跃事件：已确认(status=1)、已派单(status=2)、处理中(status=3)
  return props.incidents.filter(i => i.status >= 1 && i.status < 4).length
})

let scene, camera, renderer, controls, animationId
const eventMarkers = new Map()
let lastTime = 0
let elapsedTime = 0
let isDisposed = false
let radarScan = null
let radarAngle = 0
let groundRings = []
let hologramRing = null
let centerPulse = null
let buildingMeshes = []

const disasterTypes = [
  { type: '火灾', label: '火灾', color: '#ff3366' },
  { type: '洪涝', label: '洪涝', color: '#00d4ff' },
  { type: '地震', label: '地震', color: '#ffaa00' },
  { type: '台风', label: '台风', color: '#00ff88' },
  { type: '交通事故', label: '交通事故', color: '#ff8800' },
  { type: '燃气泄漏', label: '燃气泄漏', color: '#ff5577' },
  { type: '暴雪', label: '暴雪', color: '#88ccff' },
  { type: '泥石流', label: '泥石流', color: '#9966ff' },
  { type: '其他', label: '其他', color: '#666666' }
]

const getColorByDisasterType = (type) => {
  const item = disasterTypes.find(d => d.type === type)
  return new THREE.Color(item ? item.color : '#666666')
}

// ===================== INIT SCENE =====================
const initScene = () => {
  if (!mapContainer.value) return
  const container = mapContainer.value
  const w = container.clientWidth, h = container.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x030810)
  scene.fog = new THREE.FogExp2(0x030810, 0.002)

  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 2000)
  camera.position.set(0, 200, 260)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: !props.lite, alpha: true, powerPreference: 'high-performance' })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, props.lite ? 1 : 2))
  renderer.shadowMap.enabled = !props.lite
  if (!props.lite) {
    renderer.shadowMap.type = THREE.PCFShadowMap
  }
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2
  container.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.minDistance = 30
  controls.maxDistance = 800
  controls.maxPolarAngle = Math.PI / 2.15
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.25

  createLighting()
  createGround()
  if (!props.lite) {
    createHexGrid()
  }
  createRadarScan()
  createCenterHologram()
  createDistricts()

  // Cinematic camera entrance
  const startPos = new THREE.Vector3(0, 500, 550)
  const endPos = new THREE.Vector3(0, 200, 280)
  camera.position.copy(startPos)
  const dur = 3000, t0 = Date.now()
  const animateIn = () => {
    if (isDisposed || !camera) return
    const p = Math.min((Date.now() - t0) / dur, 1)
    const ease = 1 - Math.pow(1 - p, 4)
    camera.position.lerpVectors(startPos, endPos, ease)
    camera.lookAt(0, 0, 0)
    if (p < 1) requestAnimationFrame(animateIn)
  }
  animateIn()
  loading.value = false
}

// ===================== LIGHTING =====================
const createLighting = () => {
  scene.add(new THREE.AmbientLight(0x0a1530, 2.5))

  const dir = new THREE.DirectionalLight(0x3366aa, 1.5)
  dir.position.set(80, 120, 80)
  dir.castShadow = true
  dir.shadow.mapSize.set(2048, 2048)
  dir.shadow.camera.far = 600
  scene.add(dir)

  const colors = [0x00ff88, 0x00d4ff, 0x7c3aed, 0xff3366]
  const positions = [[-100, 60, -100], [100, 60, 100], [0, 80, 0], [-100, 50, 100]]
  colors.forEach((c, i) => {
    const pl = new THREE.PointLight(c, 0.6, 300)
    pl.position.set(...positions[i])
    scene.add(pl)
  })
}

// ===================== GROUND =====================
const createGround = () => {
  const gGeo = new THREE.PlaneGeometry(800, 800, 80, 80)
  const pos = gGeo.attributes.position
  for (let i = 0; i < pos.count; i++) {
    const x = pos.getX(i), y = pos.getY(i)
    const dist = Math.sqrt(x * x + y * y)
    pos.setZ(i, Math.sin(dist * 0.04) * 0.8 + Math.sin(x * 0.08 + y * 0.06) * 0.4)
  }
  gGeo.computeVertexNormals()

  const gMat = new THREE.MeshStandardMaterial({ color: 0x050c18, roughness: 0.95, metalness: 0.05 })
  const ground = new THREE.Mesh(gGeo, gMat)
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  ;[100, 200, 300].forEach((r) => {
    const ring = new THREE.Mesh(
      new THREE.RingGeometry(r - 0.4, r + 0.4, 128),
      new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.1, side: THREE.DoubleSide, depthWrite: false })
    )
    ring.rotation.x = -Math.PI / 2
    ring.position.y = 0.1
    scene.add(ring)
    groundRings.push(ring)
  })
}

// ===================== HEX GRID =====================
const createHexGrid = () => {
  const hexSize = 8
  const rows = 30, cols = 30
  const hexGeo = new THREE.CircleGeometry(hexSize * 0.9, 6)
  const hexMat = new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.02, side: THREE.DoubleSide, depthWrite: false })
  const edgeMat = new THREE.LineBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.05 })

  for (let row = -rows / 2; row < rows / 2; row++) {
    for (let col = -cols / 2; col < cols / 2; col++) {
      const x = col * hexSize * 1.75 + (row % 2 === 0 ? hexSize * 0.875 : 0)
      const z = row * hexSize * 1.5
      if (Math.sqrt(x * x + z * z) > 350) continue

      const hex = new THREE.Mesh(hexGeo, hexMat)
      hex.rotation.x = -Math.PI / 2
      hex.position.set(x, 0.05, z)
      scene.add(hex)

      const edges = new THREE.EdgesGeometry(hexGeo)
      const line = new THREE.LineSegments(edges, edgeMat)
      line.rotation.x = -Math.PI / 2
      line.position.set(x, 0.06, z)
      scene.add(line)
    }
  }
}

// ===================== RADAR SCAN (ADVANCED) =====================
let radarTrailMeshes = []
let radarParticles = null
let radarPulseRings = []
const createRadarScan = () => {
  const radius = 280

  // === 主扫描扇形（渐变尾迹效果，多层叠加）===
  const createSweepLayer = (sweepAngle, opacity, color) => {
    const shape = new THREE.Shape()
    shape.moveTo(0, 0)
    const segs = 48
    for (let i = 0; i <= segs; i++) {
      const a = -sweepAngle / 2 + (sweepAngle * i) / segs
      shape.lineTo(Math.cos(a) * radius, Math.sin(a) * radius)
    }
    shape.lineTo(0, 0)
    return new THREE.Mesh(
      new THREE.ShapeGeometry(shape),
      new THREE.MeshBasicMaterial({ color, transparent: true, opacity, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
  }

  // 多层叠加扇形 —— 模拟辉光渐变拖尾
  const sweepGroup = new THREE.Group()
  const allLayers = [
    { angle: Math.PI / 2.5, opacity: 0.04, color: 0x00d4ff },
    { angle: Math.PI / 3.5, opacity: 0.08, color: 0x00d4ff },
    { angle: Math.PI / 5, opacity: 0.14, color: 0x00e8ff },
    { angle: Math.PI / 8, opacity: 0.22, color: 0x00ffff },
    { angle: Math.PI / 16, opacity: 0.35, color: 0x44ffff }
  ]
  const layers = props.lite ? allLayers.filter((_, i) => i % 2 === 0) : allLayers
  layers.forEach(l => {
    const mesh = createSweepLayer(l.angle, l.opacity, l.color)
    sweepGroup.add(mesh)
  })
  sweepGroup.rotation.x = -Math.PI / 2
  sweepGroup.position.y = 0.15
  scene.add(sweepGroup)
  radarScan = sweepGroup

  // === 前沿扫描亮线 ===
  const lineGeo = new THREE.BufferGeometry()
  const lineVerts = []
  for (let i = 0; i <= 64; i++) {
    const t = i / 64
    lineVerts.push(Math.cos(0) * radius * t, Math.sin(0) * radius * t, 0)
  }
  lineGeo.setAttribute('position', new THREE.Float32BufferAttribute(lineVerts, 3))
  const radarLine = new THREE.Line(
    lineGeo,
    new THREE.LineBasicMaterial({ color: 0x00ffff, transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  radarLine.position.y = 0.01
  sweepGroup.add(radarLine)

  // === 扫描粒子特效 ===
  const particleCount = props.lite ? 30 : 120
  const pGeo = new THREE.BufferGeometry()
  const pPositions = new Float32Array(particleCount * 3)
  const pAlphas = new Float32Array(particleCount)
  for (let i = 0; i < particleCount; i++) {
    const r = Math.random() * radius * 0.95
    const a = (Math.random() - 0.5) * 0.3
    pPositions[i * 3] = Math.cos(a) * r
    pPositions[i * 3 + 1] = Math.sin(a) * r
    pPositions[i * 3 + 2] = 0
    pAlphas[i] = Math.random()
  }
  pGeo.setAttribute('position', new THREE.Float32BufferAttribute(pPositions, 3))
  radarParticles = new THREE.Points(
    pGeo,
    new THREE.PointsMaterial({ color: 0x00ffff, size: 2, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending, depthWrite: false, sizeAttenuation: true })
  )
  radarParticles.position.y = 0.02
  sweepGroup.add(radarParticles)

  // === 中心核心（多层嵌套环 + 发光球）===
  const coreGroup = new THREE.Group()
  ;[3, 5.5, 8.5].forEach((r, i) => {
    const ring = new THREE.Mesh(
      new THREE.RingGeometry(r - 0.3, r + 0.3, 48),
      new THREE.MeshBasicMaterial({ color: [0x00ffff, 0x00d4ff, 0x0088ff][i], transparent: true, opacity: [0.7, 0.45, 0.25][i], side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    ring.rotation.x = -Math.PI / 2
    coreGroup.add(ring)
  })
  const coreSphere = new THREE.Mesh(
    new THREE.SphereGeometry(2.5, 32, 32),
    new THREE.MeshBasicMaterial({ color: 0x00ffff, transparent: true, opacity: 0.85, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  coreSphere.position.y = 0.3
  coreGroup.add(coreSphere)
  coreGroup.position.y = 0.2
  scene.add(coreGroup)

  // === 外圈脉冲环（两个交替扩散的环）===
  for (let i = 0; i < 2; i++) {
    const pulse = new THREE.Mesh(
      new THREE.RingGeometry(radius * 0.4, radius * 0.4 + 1.5, 96),
      new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.3, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    pulse.rotation.x = -Math.PI / 2
    pulse.position.y = 0.12
    pulse.userData.phase = i * Math.PI
    scene.add(pulse)
    radarPulseRings.push(pulse)
  }

  // === 十字准星线 ===
  const crossMat = new THREE.LineBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.12, blending: THREE.AdditiveBlending, depthWrite: false })
  ;[0, Math.PI / 2].forEach(angle => {
    const g = new THREE.BufferGeometry()
    g.setAttribute('position', new THREE.Float32BufferAttribute([
      Math.cos(angle) * -radius, 0, Math.sin(angle) * -radius,
      Math.cos(angle) * radius, 0, Math.sin(angle) * radius
    ], 3))
    const line = new THREE.Line(g, crossMat)
    line.position.y = 0.08
    scene.add(line)
  })
}

// ===================== CENTER HOLOGRAM =====================
const createCenterHologram = () => {
  // Center pillar beam - 更亮更粗
  const beamGeo = new THREE.CylinderGeometry(1, 3, 100, 16, 1, true)
  const beamMat = new THREE.MeshBasicMaterial({
    color: 0x00d4ff, transparent: true, opacity: 0.15,
    blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
  })
  const beam = new THREE.Mesh(beamGeo, beamMat)
  beam.position.y = 50
  scene.add(beam)

  // 外层光柱
  const outerBeamGeo = new THREE.CylinderGeometry(3, 6, 100, 16, 1, true)
  const outerBeamMat = new THREE.MeshBasicMaterial({
    color: 0x00d4ff, transparent: true, opacity: 0.04,
    blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
  })
  const outerBeam = new THREE.Mesh(outerBeamGeo, outerBeamMat)
  outerBeam.position.y = 50
  scene.add(outerBeam)

  // Rotating hologram rings - 更大更亮
  const ringGroup = new THREE.Group()
  ;[8, 14, 20].forEach((r, i) => {
    const torusGeo = new THREE.TorusGeometry(r, 0.25, 12, 80)
    const torusMat = new THREE.MeshBasicMaterial({
      color: [0x00d4ff, 0x00ff88, 0x7c3aed][i],
      transparent: true, opacity: 0.6,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    const torus = new THREE.Mesh(torusGeo, torusMat)
    torus.rotation.x = Math.PI / 2 + (i * 0.35)
    torus.rotation.z = i * 1.2
    ringGroup.add(torus)
  })
  ringGroup.position.y = 55
  scene.add(ringGroup)
  hologramRing = ringGroup

  // Pulse sphere at center - 更大更亮
  const pulseGeo = new THREE.SphereGeometry(4, 32, 32)
  const pulseMat = new THREE.MeshBasicMaterial({
    color: 0x00d4ff, transparent: true, opacity: 0.8,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  centerPulse = new THREE.Mesh(pulseGeo, pulseMat)
  centerPulse.position.y = 55
  scene.add(centerPulse)

  // 底部光环
  const baseGlow = new THREE.Mesh(
    new THREE.RingGeometry(4, 8, 64),
    new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.3, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  baseGlow.rotation.x = -Math.PI / 2
  baseGlow.position.y = 0.3
  scene.add(baseGlow)
}

// ===================== DISTRICTS (BUILDINGS) =====================
const createDistricts = () => {
  const bMat = new THREE.MeshStandardMaterial({ color: 0x0a1628, roughness: 0.6, metalness: 0.4 })
  const eMat = new THREE.LineBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.2 })

  const districts = [
    { cx: -100, cz: -100, n: 25, spread: 45 },
    { cx: -100, cz: 20, n: 20, spread: 35 },
    { cx: -100, cz: 110, n: 18, spread: 30 },
    { cx: 90, cz: -90, n: 22, spread: 40 },
    { cx: 90, cz: 20, n: 18, spread: 35 },
    { cx: 90, cz: 110, n: 15, spread: 28 },
    { cx: 0, cz: -100, n: 14, spread: 22 },
    { cx: 0, cz: 100, n: 12, spread: 20 },
    { cx: -180, cz: -40, n: 10, spread: 25 },
    { cx: 180, cz: -40, n: 10, spread: 25 }
  ]

  const liteRatio = props.lite ? 0.35 : 1
  const lakeX = 120, lakeZ = 120, lakeR = 30

  districts.forEach(d => {
    const buildingCount = Math.round(d.n * liteRatio)
    const placed = []
    for (let i = 0; i < buildingCount; i++) {
      let x, z, ok = false
      for (let a = 0; a < 50; a++) {
        x = d.cx + (Math.random() - 0.5) * d.spread * 2
        z = d.cz + (Math.random() - 0.5) * d.spread * 2
        const dLake = Math.sqrt((x - lakeX) ** 2 + (z - lakeZ) ** 2)
        const tooClose = placed.some(p => Math.sqrt((x - p.x) ** 2 + (z - p.z) ** 2) < 6)
        const nearCenter = Math.sqrt(x * x + z * z) < 18
        if (dLake > lakeR && !tooClose && !nearCenter) { ok = true; break }
      }
      if (!ok) continue
      placed.push({ x, z })

      const w = 2.5 + Math.random() * 5
      const dp = 2.5 + Math.random() * 5
      const h = 10 + Math.random() * 35
      const geo = new THREE.BoxGeometry(w, h, dp)
      const mat = bMat.clone()
      mat.emissive = new THREE.Color(0x00d4ff)
      mat.emissiveIntensity = 0.03 + (h / 45) * 0.1

      const mesh = new THREE.Mesh(geo, mat)
      mesh.position.set(x, h / 2, z)
      mesh.castShadow = true
      mesh.receiveShadow = true
      scene.add(mesh)
      buildingMeshes.push(mesh)

      // Wireframe - skip in lite mode
      if (!props.lite) {
        const edgeLine = new THREE.LineSegments(new THREE.EdgesGeometry(geo), eMat.clone())
        edgeLine.position.copy(mesh.position)
        scene.add(edgeLine)
      }

      // Neon windows - skip in lite mode
      if (!props.lite) {
      const windowRows = Math.floor(h / 3)
      for (let r = 0; r < windowRows; r++) {
        const wy = r * 3 + 2
        if (wy > h - 1 || Math.random() < 0.3) continue
        const wColor = Math.random() > 0.5 ? 0x00d4ff : 0xffaa44
        const wGeo = new THREE.PlaneGeometry(w * 0.7, 0.8)
        const wMat = new THREE.MeshBasicMaterial({
          color: wColor, transparent: true, opacity: 0.15 + Math.random() * 0.25,
          blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
        })
        const wMesh = new THREE.Mesh(wGeo, wMat)
        wMesh.position.set(x, wy, z + dp / 2 + 0.05)
        scene.add(wMesh)
        const wBack = wMesh.clone()
        wBack.position.z = z - dp / 2 - 0.05
        scene.add(wBack)
      }
      } // end lite skip windows

      // Rooftop lights on tall buildings
      if (h > 25) {
        const rp = new THREE.Mesh(
          new THREE.SphereGeometry(0.5, 8, 8),
          new THREE.MeshBasicMaterial({ color: Math.random() > 0.5 ? 0xff3366 : 0x00ff88, transparent: true, opacity: 0.8 })
        )
        rp.position.set(x, h + 0.5, z)
        scene.add(rp)
      }
    }
  })

  // Lake
  const lake = new THREE.Mesh(
    new THREE.CircleGeometry(22, 64),
    new THREE.MeshStandardMaterial({ color: 0x002244, transparent: true, opacity: 0.8, roughness: 0.05, metalness: 0.9 })
  )
  lake.rotation.x = -Math.PI / 2
  lake.position.set(lakeX, 0.15, lakeZ)
  scene.add(lake)

  const rim = new THREE.Mesh(
    new THREE.RingGeometry(21, 23, 64),
    new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.12, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  rim.rotation.x = -Math.PI / 2
  rim.position.set(lakeX, 0.18, lakeZ)
  scene.add(rim)

  createRoads()
}

// ===================== ROADS =====================
const createRoads = () => {
  const rMat = new THREE.MeshStandardMaterial({ color: 0x080f1a, roughness: 0.8, metalness: 0.1 })
  const dashMat = new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.25, blending: THREE.AdditiveBlending, depthWrite: false })
  const roadW = 8
  const roads = [[[-300, 0], [300, 0]], [[0, -300], [0, 300]], [[-250, -120], [250, -120]], [[-250, 120], [250, 120]]]

  roads.forEach(([s, e]) => {
    const dx = e[0] - s[0], dz = e[1] - s[1]
    const len = Math.sqrt(dx * dx + dz * dz)
    const angle = Math.atan2(dz, dx)
    const road = new THREE.Mesh(new THREE.PlaneGeometry(len, roadW), rMat)
    road.position.set((s[0] + e[0]) / 2, 0.2, (s[1] + e[1]) / 2)
    road.rotation.x = -Math.PI / 2
    road.rotation.z = angle
    road.receiveShadow = true
    scene.add(road)

    const nd = Math.floor(len / 7)
    for (let i = 0; i < nd; i++) {
      const t = i / nd
      const dash = new THREE.Mesh(new THREE.PlaneGeometry(3, 0.4), dashMat)
      dash.position.set(s[0] + dx * t, 0.25, s[1] + dz * t)
      dash.rotation.x = -Math.PI / 2
      dash.rotation.z = angle
      scene.add(dash)
    }
  })
}

// ===================== EVENT MARKERS =====================
const geoToWorld = (lng, lat) => {
  // 中心坐标与模拟数据一致（北京区域）
  const cx = 116.4, cy = 39.92, scale = 18
  return new THREE.Vector3(
    Math.max(-280, Math.min(280, (lng - cx) * scale * 111)),
    0,
    Math.max(-280, Math.min(280, -(lat - cy) * scale * 111))
  )
}

const createMarker = (incident) => {
  const group = new THREE.Group()
  const color = getColorByDisasterType(incident.disasterType)
  const severity = incident.severity || 0
  const s = 1 + severity * 0.4

  // Ground ring
  const ring = new THREE.Mesh(
    new THREE.RingGeometry(2.5 * s, 3.2 * s, 32),
    new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.6, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  ring.rotation.x = -Math.PI / 2
  ring.position.y = 0.3
  group.add(ring)

  // Pulse ring
  const pulse = new THREE.Mesh(
    new THREE.RingGeometry(3 * s, 3.5 * s, 32),
    new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.3, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  pulse.rotation.x = -Math.PI / 2
  pulse.position.y = 0.35
  group.add(pulse)
  group.userData.pulseRing = pulse
  group.userData.pulsePhase = Math.random() * Math.PI * 2

  // Vertical beam
  const beamH = 20 + severity * 10
  const beam = new THREE.Mesh(
    new THREE.CylinderGeometry(0.15, 0.8 * s, beamH, 8, 1, true),
    new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.25, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide })
  )
  beam.position.y = beamH / 2
  group.add(beam)

  // Top sphere
  const sphere = new THREE.Mesh(
    new THREE.SphereGeometry(1.2 * s, 16, 16),
    new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.7, blending: THREE.AdditiveBlending, depthWrite: false })
  )
  sphere.position.y = beamH
  group.add(sphere)
  group.userData.topSphere = sphere
  group.userData.topBaseY = beamH

  // Danger halo for severe events
  if (severity >= 2) {
    const halo = new THREE.Mesh(
      new THREE.RingGeometry(5 * s, 8 * s, 32),
      new THREE.MeshBasicMaterial({ color: 0xff3366, transparent: true, opacity: 0.1, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    halo.rotation.x = -Math.PI / 2
    halo.position.y = 0.2
    group.add(halo)
  }

  // Confirmed event highlight (status === 1): green pulsing glow
  if (incident.status === 1) {
    const confirmRing = new THREE.Mesh(
      new THREE.RingGeometry(4 * s, 6 * s, 32),
      new THREE.MeshBasicMaterial({ color: 0x00ff88, transparent: true, opacity: 0.25, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    confirmRing.rotation.x = -Math.PI / 2
    confirmRing.position.y = 0.25
    group.add(confirmRing)
    group.userData.confirmRing = confirmRing

    // Inner glow ring
    const innerGlow = new THREE.Mesh(
      new THREE.RingGeometry(3 * s, 4.2 * s, 32),
      new THREE.MeshBasicMaterial({ color: 0x00ff88, transparent: true, opacity: 0.15, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    innerGlow.rotation.x = -Math.PI / 2
    innerGlow.position.y = 0.28
    group.add(innerGlow)
    group.userData.confirmInnerGlow = innerGlow
  }

  const pos = incident.lng && incident.lat
    ? geoToWorld(incident.lng, incident.lat)
    : new THREE.Vector3((Math.random() - 0.5) * 200, 0, (Math.random() - 0.5) * 200)
  group.position.copy(pos)

  group.userData.incident = incident
  scene.add(group)
  eventMarkers.set(incident.id, group)
  return group
}

const removeMarker = (id) => {
  const marker = eventMarkers.get(id)
  if (marker) {
    scene.remove(marker)
    marker.traverse(child => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) child.material.dispose()
    })
    eventMarkers.delete(id)
  }
}

const updateMarkers = () => {
  const currentIds = new Set(props.incidents.map(i => i.id))
  for (const [id] of eventMarkers) {
    if (!currentIds.has(id)) removeMarker(id)
  }
  for (const inc of props.incidents) {
    const existing = eventMarkers.get(inc.id)
    if (existing) {
      // Re-create marker if status changed (e.g., confirmed event needs highlight)
      if (existing.userData.incident && existing.userData.incident.status !== inc.status) {
        removeMarker(inc.id)
        createMarker(inc)
      }
    } else {
      createMarker(inc)
    }
  }
}

// ===================== CLICK HANDLING =====================
const onMouseClick = (event) => {
  if (!renderer || !camera) return
  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )
  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)

  for (const [, marker] of eventMarkers) {
    if (raycaster.intersectObjects(marker.children, true).length > 0) {
      emit('select-incident', marker.userData.incident)
      return
    }
  }
}

// ===================== ANIMATION LOOP =====================
const animate = () => {
  if (isDisposed) return
  animationId = requestAnimationFrame(animate)
  const now = performance.now() / 1000
  const delta = lastTime > 0 ? Math.min(now - lastTime, 0.1) : 0.016
  lastTime = now
  elapsedTime += delta
  const elapsed = elapsedTime
  controls.update()

  // Radar sweep - 高级多层扫描动画
  if (radarScan) {
    radarAngle += delta * 0.7
    radarScan.rotation.z = radarAngle
    // 各层呼吸脉冲
    radarScan.children.forEach((child, i) => {
      if (child.material && i < 5) {
        const baseOp = [0.04, 0.08, 0.14, 0.22, 0.35][i] || 0.1
        child.material.opacity = baseOp + Math.sin(elapsed * 1.5 + i * 0.5) * baseOp * 0.3
      }
    })
  }

  // 雷达粒子闪烁
  if (radarParticles && radarParticles.material) {
    radarParticles.material.opacity = 0.4 + Math.sin(elapsed * 3) * 0.25
    radarParticles.material.size = 1.5 + Math.sin(elapsed * 2) * 0.8
  }

  // 脉冲环扩散
  radarPulseRings.forEach(ring => {
    const p = ((elapsed * 0.3 + ring.userData.phase / (Math.PI * 2)) % 1)
    const s = 0.3 + p * 0.7
    ring.scale.set(s, s, s)
    ring.material.opacity = (1 - p) * 0.35
  })

  // Hologram rotation - 更动感
  if (hologramRing) {
    hologramRing.rotation.y += delta * 0.6
    hologramRing.children.forEach((torus, i) => {
      torus.rotation.x += delta * (0.15 + i * 0.08)
      torus.material.opacity = 0.4 + Math.sin(elapsed * 2 + i) * 0.2
    })
  }

  // Center pulse - 更强烈
  if (centerPulse) {
    const ps = 1 + Math.sin(elapsed * 2.5) * 0.4
    centerPulse.scale.set(ps, ps, ps)
    centerPulse.material.opacity = 0.5 + Math.sin(elapsed * 3) * 0.3
  }

  // Ground ring breathing - 更明显
  groundRings.forEach((ring, i) => {
    ring.material.opacity = 0.07 + Math.sin(elapsed * 0.8 + i * 1.5) * 0.04
  })

  // Event marker animations
  for (const [, marker] of eventMarkers) {
    if (marker.userData.pulseRing) {
      const phase = marker.userData.pulsePhase + elapsed * 2
      const ms = 1 + Math.sin(phase) * 0.3
      marker.userData.pulseRing.scale.set(ms, ms, 1)
      marker.userData.pulseRing.material.opacity = 0.15 + Math.sin(phase) * 0.15
    }
    if (marker.userData.topSphere) {
      marker.userData.topSphere.position.y = marker.userData.topBaseY + Math.sin(marker.userData.pulsePhase + elapsed * 1.5) * 0.5
    }
    // Confirmed event glow animation
    if (marker.userData.confirmRing) {
      const cp = marker.userData.pulsePhase + elapsed * 1.8
      const cs = 1 + Math.sin(cp) * 0.2
      marker.userData.confirmRing.scale.set(cs, cs, 1)
      marker.userData.confirmRing.material.opacity = 0.15 + Math.sin(cp) * 0.1
    }
    if (marker.userData.confirmInnerGlow) {
      const cp2 = marker.userData.pulsePhase + elapsed * 2.5
      marker.userData.confirmInnerGlow.material.opacity = 0.1 + Math.sin(cp2) * 0.08
    }
  }

  renderer.render(scene, camera)
}

// ===================== RESIZE =====================
const handleResize = () => {
  if (!mapContainer.value || !camera || !renderer) return
  const w = mapContainer.value.clientWidth, h = mapContainer.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// ===================== LIFECYCLE =====================
watch(() => props.incidents, updateMarkers, { deep: true })

onMounted(() => {
  isDisposed = false
  lastTime = 0
  elapsedTime = 0
  initScene()
  updateMarkers()
  animate()
  window.addEventListener('resize', handleResize)
  renderer?.domElement?.addEventListener('click', onMouseClick)
})

onUnmounted(() => {
  isDisposed = true
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
  for (const [id] of eventMarkers) removeMarker(id)
  if (renderer) {
    renderer.domElement?.removeEventListener('click', onMouseClick)
    renderer.dispose()
    if (mapContainer.value && renderer.domElement) {
      mapContainer.value.removeChild(renderer.domElement)
    }
  }
  controls?.dispose()
  scene = null; camera = null; renderer = null; controls = null
})
</script>

<style scoped>
.map-scene {
  width: 100%;
  height: 100%;
  position: relative;
  background: #030810;
  overflow: hidden;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(3, 8, 16, 0.95);
  z-index: 100;
}

.loading-content {
  text-align: center;
  color: #00d4ff;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(0, 212, 255, 0.15);
  border-top-color: #00d4ff;
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.map-hud {
  position: absolute;
  z-index: 50;
  padding: 12px 16px;
  background: rgba(3, 8, 16, 0.8);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 8px;
  backdrop-filter: blur(8px);
}

.map-hud.top-left {
  top: 16px;
  left: 16px;
}

.hud-label {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(0, 212, 255, 0.5);
  margin-bottom: 4px;
}

.hud-value {
  font-size: 28px;
  font-weight: 700;
  color: #00d4ff;
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.5);
}

.hud-value span {
  font-size: 11px;
  font-weight: 400;
  color: rgba(0, 212, 255, 0.5);
  margin-left: 6px;
}

.hud-sub {
  font-size: 11px;
  color: rgba(0, 212, 255, 0.4);
  margin-top: 2px;
  letter-spacing: 1px;
}

.map-legend {
  position: absolute;
  bottom: 20px;
  left: 16px;
  z-index: 50;
  padding: 12px 16px;
  background: rgba(3, 8, 16, 0.8);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 8px;
  backdrop-filter: blur(8px);
}

.legend-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
  font-weight: 600;
}

.legend-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 8px #00d4ff;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.map-compass {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 50;
  opacity: 0.7;
}
</style>
