<template>
  <div class="login-container">
    <!-- 粒子流背景 -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    
    <!-- 网格背景 -->
    <div class="grid-background">
      <div class="scanline"></div>
    </div>
    
    <!-- 登录卡片 -->
    <div class="login-card" :class="{ 'login-success': loginSuccess }">
      <!-- 系统标题 -->
      <div class="login-header">
        <div class="logo-container">
          <div class="logo-ring"></div>
          <div class="logo-core"></div>
        </div>
        <h1 class="system-title">智瞰危局</h1>
        <p class="system-subtitle">INTELLIGENT CRISIS OBSERVATORY</p>
      </div>
      
      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <!-- 角色切换 -->
        <div class="role-switch">
          <div 
            class="role-option" 
            :class="{ active: loginForm.role === 'admin' }"
            @click="loginForm.role = 'admin'"
          >
            <el-icon><Monitor /></el-icon>
            <span>管理员</span>
          </div>
          <div 
            class="role-option" 
            :class="{ active: loginForm.role === 'responder' }"
            @click="loginForm.role = 'responder'"
          >
            <el-icon><User /></el-icon>
            <span>执勤人员</span>
          </div>
        </div>

        <!-- 用户名输入 -->
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        
        <!-- 密码输入 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <!-- 记住密码 -->
        <div class="login-options">
          <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>
        
        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 已登录角色快速入口 -->
      <div class="active-sessions" v-if="hasAdminSession || hasResponderSession">
        <div class="sessions-divider">
          <span>已登录角色 — 快速进入</span>
        </div>
        <div class="session-buttons">
          <div
            v-if="hasAdminSession"
            class="session-btn admin-btn"
            @click="goToAdmin"
          >
            <el-icon><Monitor /></el-icon>
            <span>管理员控制台</span>
            <span class="session-user">{{ adminUserName }}</span>
          </div>
          <div
            v-if="hasResponderSession"
            class="session-btn responder-btn"
            @click="goToResponder"
          >
            <el-icon><User /></el-icon>
            <span>执勤人员端</span>
            <span class="session-user">{{ responderUserName }}</span>
          </div>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="login-footer">
        <p>© 2026 智瞰危局 v2.0</p>
      </div>
    </div>
    
    <!-- 登录成功过渡动画 -->
    <transition name="success-fade">
      <div v-if="loginSuccess" class="success-overlay">
        <div class="success-content">
          <div class="success-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
          <p class="success-text">登录成功</p>
          <p class="success-subtext">正在进入系统...</p>
        </div>
      </div>
    </transition>
    
    <!-- 角落装饰 -->
    <div class="corner-decoration top-left"></div>
    <div class="corner-decoration top-right"></div>
    <div class="corner-decoration bottom-left"></div>
    <div class="corner-decoration bottom-right"></div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

// 路由
const router = useRouter()
const route = useRoute()

// 用户状态
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref(null)

// 加载状态
const loading = ref(false)

// 登录成功状态
const loginSuccess = ref(false)

// 记住密码
const rememberMe = ref(false)

// 检测已登录的角色会话
const hasAdminSession = computed(() => !!localStorage.getItem('admin_access_token'))
const hasResponderSession = computed(() => !!localStorage.getItem('responder_access_token'))
const adminUserName = computed(() => {
  try { return JSON.parse(localStorage.getItem('admin_user_info') || '{}').username || '管理员' } catch { return '管理员' }
})
const responderUserName = computed(() => {
  try { return JSON.parse(localStorage.getItem('responder_user_info') || '{}').username || '执勤人员' } catch { return '执勤人员' }
})
const goToAdmin = () => router.push('/dashboard')
const goToResponder = () => router.push('/responder/dashboard')

// 根据URL参数预设角色
const initRole = route.query.role === 'responder' ? 'responder' : 'admin'

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  role: initRole
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6-30 个字符', trigger: 'blur' }
  ]
}

// 粒子画布引用
const particleCanvas = ref(null)

// 粒子系统
let particles = []
let animationId = null
let ctx = null

// 粒子类
class Particle {
  constructor(canvas) {
    this.canvas = canvas
    this.reset()
  }

  reset() {
    this.x = Math.random() * this.canvas.width
    this.y = Math.random() * this.canvas.height
    this.vx = (Math.random() - 0.5) * 0.5
    this.vy = (Math.random() - 0.5) * 0.5
    this.radius = Math.random() * 2 + 1
    this.opacity = Math.random() * 0.5 + 0.2
  }

  update() {
    this.x += this.vx
    this.y += this.vy

    if (this.x < 0 || this.x > this.canvas.width) this.vx *= -1
    if (this.y < 0 || this.y > this.canvas.height) this.vy *= -1
  }

  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(0, 212, 255, ${this.opacity})`
    ctx.fill()
  }
}

// 初始化粒子系统
const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return

  ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  // 创建粒子
  const particleCount = Math.floor((canvas.width * canvas.height) / 10000)
  particles = []
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle(canvas))
  }

  // 开始动画
  animate()
}

// 动画循环
const animate = () => {
  if (!ctx) return

  ctx.clearRect(0, 0, particleCanvas.value.width, particleCanvas.value.height)

  // 更新和绘制粒子
  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  // 绘制粒子之间的连线
  particles.forEach((p1, i) => {
    particles.slice(i + 1).forEach(p2 => {
      const dx = p1.x - p2.x
      const dy = p1.y - p2.y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 150) {
        ctx.beginPath()
        ctx.moveTo(p1.x, p1.y)
        ctx.lineTo(p2.x, p2.y)
        ctx.strokeStyle = `rgba(0, 212, 255, ${0.1 * (1 - distance / 150)})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    })
  })

  animationId = requestAnimationFrame(animate)
}

// 窗口大小改变处理
const handleResize = () => {
  const canvas = particleCanvas.value
  if (!canvas) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    // 表单验证
    await loginFormRef.value.validate()

    loading.value = true

    // 调用登录接口
    await userStore.login(loginForm.username, loginForm.password, loginForm.role)

    // 显示成功状态
    loginSuccess.value = true

    // 延迟跳转（根据角色跳转到不同页面）
    setTimeout(() => {
      if (loginForm.role === 'responder') {
        router.push('/responder/dashboard')
      } else {
        const redirect = route.query.redirect || '/dashboard'
        router.push(redirect)
      }
    }, 1500)
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// 组件挂载
onMounted(() => {
  initParticles()
  window.addEventListener('resize', handleResize)
})

// 组件卸载
onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0a0e17 0%, #0d1a2d 50%, #0a0e17 100%);
}

/* 粒子画布 */
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* 网格背景 */
.grid-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    linear-gradient(rgba(0, 212, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 2;
}

.grid-background .scanline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100px;
  background: linear-gradient(
    180deg,
    transparent,
    rgba(0, 212, 255, 0.1),
    transparent
  );
  animation: scanline 4s linear infinite;
}

@keyframes scanline {
  0% {
    transform: translateY(-100px);
  }
  100% {
    transform: translateY(100vh);
  }
}

/* 登录卡片 */
.login-card {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 420px;
  padding: 40px;
  background: rgba(10, 20, 40, 0.9);
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 16px;
  box-shadow: 
    0 0 40px rgba(0, 212, 255, 0.2),
    inset 0 0 60px rgba(0, 212, 255, 0.05);
  z-index: 10;
  backdrop-filter: blur(10px);
  transition: all 0.5s ease;
}

.login-card.login-success {
  transform: translate(-50%, -50%) scale(0.95);
  opacity: 0;
}

/* 登录头部 */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
}

.logo-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 2px solid rgba(0, 212, 255, 0.5);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.logo-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #00d4ff, #0066cc);
  border-radius: 50%;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.5);
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

.system-title {
  font-size: 28px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 8px;
  letter-spacing: 4px;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

.system-subtitle {
  font-size: 12px;
  color: rgba(0, 212, 255, 0.7);
  letter-spacing: 2px;
}

/* 登录表单 */
.login-form {
  margin-top: 20px;
}

/* 角色切换 */
.role-switch {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.role-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.role-option:hover {
  border-color: rgba(0, 212, 255, 0.5);
  color: rgba(255, 255, 255, 0.7);
}

.role-option.active {
  border-color: #00d4ff;
  background: rgba(0, 212, 255, 0.1);
  color: #00d4ff;
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.2);
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(0, 20, 40, 0.6) !important;
  border: 1px solid rgba(0, 212, 255, 0.3) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  transition: all 0.3s ease !important;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 212, 255, 0.5) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.3) !important;
}

.login-form :deep(.el-input__inner) {
  color: #ffffff !important;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

.login-form :deep(.el-input__prefix) {
  color: rgba(0, 212, 255, 0.7) !important;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.7);
}

.login-options :deep(.el-checkbox__inner) {
  background: transparent;
  border-color: rgba(0, 212, 255, 0.5);
}

.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #00d4ff;
  border-color: #00d4ff;
}

.forgot-password {
  color: rgba(0, 212, 255, 0.7);
  font-size: 13px;
  transition: color 0.3s ease;
}

.forgot-password:hover {
  color: #00d4ff;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #00d4ff, #0066cc) !important;
  border: none !important;
  border-radius: 8px !important;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.4);
  transition: all 0.3s ease !important;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 30px rgba(0, 212, 255, 0.6) !important;
}

/* 登录底部 */
.active-sessions {
  margin-top: 20px;
}

.sessions-divider {
  text-align: center;
  margin-bottom: 12px;
  position: relative;
}

.sessions-divider::before,
.sessions-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: rgba(0, 212, 255, 0.2);
}

.sessions-divider::before { left: 0; }
.sessions-divider::after { right: 0; }

.sessions-divider span {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  padding: 0 10px;
}

.session-buttons {
  display: flex;
  gap: 10px;
}

.session-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
  border: 1px solid rgba(0, 212, 255, 0.2);
  background: rgba(0, 212, 255, 0.05);
}

.session-btn:hover {
  border-color: rgba(0, 212, 255, 0.6);
  background: rgba(0, 212, 255, 0.1);
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.2);
}

.session-btn .el-icon {
  font-size: 20px;
  color: #00d4ff;
}

.session-btn span:nth-child(2) {
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.session-user {
  color: rgba(0, 212, 255, 0.7);
  font-size: 11px;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 212, 255, 0.2);
}

.login-footer p {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

/* 成功过渡动画 */
.success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(10, 14, 23, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.success-content {
  text-align: center;
}

.success-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border: 3px solid #00ff88;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: success-pulse 0.5s ease-out;
}

.success-icon svg {
  width: 40px;
  height: 40px;
  color: #00ff88;
  stroke-dasharray: 50;
  stroke-dashoffset: 50;
  animation: checkmark 0.5s ease-out 0.2s forwards;
}

@keyframes success-pulse {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes checkmark {
  to {
    stroke-dashoffset: 0;
  }
}

.success-text {
  font-size: 24px;
  font-weight: 600;
  color: #00ff88;
  margin-bottom: 8px;
}

.success-subtext {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.success-fade-enter-active {
  animation: fadeIn 0.3s ease-out;
}

.success-fade-leave-active {
  animation: fadeIn 0.3s ease-out reverse;
}

/* 角落装饰 */
.corner-decoration {
  position: absolute;
  width: 100px;
  height: 100px;
  z-index: 5;
}

.corner-decoration::before,
.corner-decoration::after {
  content: '';
  position: absolute;
  background: rgba(0, 212, 255, 0.3);
}

.corner-decoration.top-left {
  top: 20px;
  left: 20px;
}

.corner-decoration.top-left::before {
  top: 0;
  left: 0;
  width: 60px;
  height: 2px;
}

.corner-decoration.top-left::after {
  top: 0;
  left: 0;
  width: 2px;
  height: 60px;
}

.corner-decoration.top-right {
  top: 20px;
  right: 20px;
}

.corner-decoration.top-right::before {
  top: 0;
  right: 0;
  width: 60px;
  height: 2px;
}

.corner-decoration.top-right::after {
  top: 0;
  right: 0;
  width: 2px;
  height: 60px;
}

.corner-decoration.bottom-left {
  bottom: 20px;
  left: 20px;
}

.corner-decoration.bottom-left::before {
  bottom: 0;
  left: 0;
  width: 60px;
  height: 2px;
}

.corner-decoration.bottom-left::after {
  bottom: 0;
  left: 0;
  width: 2px;
  height: 60px;
}

.corner-decoration.bottom-right {
  bottom: 20px;
  right: 20px;
}

.corner-decoration.bottom-right::before {
  bottom: 0;
  right: 0;
  width: 60px;
  height: 2px;
}

.corner-decoration.bottom-right::after {
  bottom: 0;
  right: 0;
  width: 2px;
  height: 60px;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }

  .system-title {
    font-size: 22px;
  }
}
</style>
