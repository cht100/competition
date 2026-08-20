<template>
  <div class="responder-layout">
    <!-- 顶部导航栏 -->
    <header class="responder-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24" class="logo-icon">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="logo-text">智瞰危局 · 执勤端</span>
        </div>
      </div>
      <div class="header-center">
        <nav class="nav-tabs">
          <router-link to="/responder/dashboard" class="nav-tab" active-class="active">
            <el-icon><Monitor /></el-icon>
            <span>任务中心</span>
          </router-link>
          <router-link to="/responder/tasks" class="nav-tab" active-class="active">
            <el-icon><List /></el-icon>
            <span>我的任务</span>
          </router-link>
          <router-link to="/responder/map" class="nav-tab" active-class="active">
            <el-icon><MapLocation /></el-icon>
            <span>态势地图</span>
          </router-link>
          <router-link to="/responder/profile" class="nav-tab" active-class="active">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </router-link>
        </nav>
      </div>
      <div class="header-right">
        <div class="status-badge" :class="onlineStatus">
          <span class="status-dot"></span>
          {{ onlineStatus === 'online' ? '在线执勤' : '离线' }}
        </div>
        <el-dropdown @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="32" class="user-avatar">{{ responderName?.charAt(0) }}</el-avatar>
            <span class="user-name">{{ responderName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="switchToAdmin">切换管理端</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="responder-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, List, User, MapLocation } from '@element-plus/icons-vue'

const router = useRouter()
const responderName = ref(JSON.parse(localStorage.getItem('responder_user_info') || '{}').username || '执勤人员')
const onlineStatus = ref('online')

function handleCommand(cmd) {
  if (cmd === 'logout') {
    localStorage.removeItem('responder_access_token')
    localStorage.removeItem('responder_user_info')
    router.push('/login')
  } else if (cmd === 'switchToAdmin') {
    router.push('/dashboard')
  }
}

onMounted(() => {
  // Check responder status
})
</script>

<style scoped>
.responder-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1e3e 50%, #0d1230 100%);
  color: #e0e6ed;
}

.responder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: rgba(10, 14, 39, 0.95);
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
  backdrop-filter: blur(20px);
}

.header-left .logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 28px;
  height: 28px;
  color: #00d4ff;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #00d4ff, #00ff88);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-tabs {
  display: flex;
  gap: 4px;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: 8px;
  color: rgba(224, 230, 237, 0.6);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.nav-tab:hover {
  color: #e0e6ed;
  background: rgba(0, 212, 255, 0.08);
}

.nav-tab.active {
  color: #00d4ff;
  background: rgba(0, 212, 255, 0.12);
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge.online {
  background: rgba(0, 255, 136, 0.1);
  color: #00ff88;
  border: 1px solid rgba(0, 255, 136, 0.3);
}

.status-badge.offline {
  background: rgba(150, 150, 150, 0.1);
  color: #999;
  border: 1px solid rgba(150, 150, 150, 0.3);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-avatar {
  background: linear-gradient(135deg, #00d4ff, #7c3aed);
}

.user-name {
  font-size: 14px;
  color: #e0e6ed;
}

.responder-main {
  padding: 20px;
  min-height: calc(100vh - 60px);
}

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
