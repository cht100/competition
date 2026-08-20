<template>
  <div class="dashboard-layout">
    <!-- 顶部状态栏 -->
    <header class="top-bar">
      <div class="logo-section">
        <div class="logo-icon">
          <el-icon :size="24"><Monitor /></el-icon>
        </div>
        <span class="logo-text">智瞰危局</span>
      </div>
      
      <div class="status-info">
        <div class="status-item">
          <el-icon><Calendar /></el-icon>
          <span>{{ currentDate }}</span>
        </div>
        <div class="status-divider"></div>
        <div class="status-item highlight">
          <el-icon><Bell /></el-icon>
          <span>今日事件: <em>{{ todayCount }}</em></span>
        </div>
        <div class="status-item warning">
          <el-icon><Warning /></el-icon>
          <span>待处理: <em>{{ pendingCount }}</em></span>
        </div>
        <div class="status-item success">
          <el-icon><Connection /></el-icon>
          <span>WebSocket: 
            <span :class="wsConnected ? 'online' : 'offline'">
              {{ wsConnected ? '已连接' : '未连接' }}
            </span>
          </span>
        </div>
      </div>
      
      <div class="user-info">
        <el-dropdown @command="handleUserCommand">
          <div class="user-avatar">
            <el-avatar :size="36" icon="User" />
            <span class="username">{{ username }}</span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>
                系统设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    
    <div class="main-content">
      <!-- 左侧菜单 -->
      <aside class="side-menu">
        <div class="menu-header">
          <span>导航菜单</span>
        </div>
        <el-menu 
          :default-active="activeMenu" 
          router
          class="nav-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <span>态势总览</span>
          </el-menu-item>
          <el-menu-item index="/stream">
            <el-icon><ChatLineRound /></el-icon>
            <span>事件流监控</span>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计看板</span>
          </el-menu-item>
          <el-menu-item index="/map">
            <el-icon><Location /></el-icon>
            <span>地图视图</span>
          </el-menu-item>
          <el-menu-item index="/clustering">
            <el-icon><Connection /></el-icon>
            <span>事件聚类</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
        
        <!-- 系统状态面板 -->
        <div class="system-status">
          <div class="status-header">系统状态</div>
          <div class="status-list">
            <div class="status-row">
              <span class="label">CPU</span>
              <el-progress 
                :percentage="systemStatus.cpu" 
                :stroke-width="6"
                :color="getProgressColor(systemStatus.cpu)"
              />
            </div>
            <div class="status-row">
              <span class="label">内存</span>
              <el-progress 
                :percentage="systemStatus.memory" 
                :stroke-width="6"
                :color="getProgressColor(systemStatus.memory)"
              />
            </div>
            <div class="status-row">
              <span class="label">磁盘</span>
              <el-progress 
                :percentage="systemStatus.disk" 
                :stroke-width="6"
                :color="getProgressColor(systemStatus.disk)"
              />
            </div>
          </div>
        </div>
      </aside>
      
      <!-- 主内容区 -->
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
      
      <!-- 右侧事件卡片区 -->
      <aside class="event-panel">
        <EventCardList @select-event="handleSelectEvent" @stats-update="handleStatsUpdate" />
      </aside>
    </div>
    
    <!-- 事件详情弹窗 -->
    <EventDetailModal 
      v-model="showDetailModal"
      :incident="selectedIncident"
      @confirm="handleConfirm"
      @reject="handleReject"
      @dispatch="handleDispatchEvent"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useEventStore } from '@/stores/event'
import { wsClient, MessageType } from '@/api/websocket'
import { get,post } from '@/api/request'
import EventCardList from '@/components/EventCardList.vue'
import EventDetailModal from '@/components/EventDetailModal.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const eventStore = useEventStore()

const activeMenu = computed(() => route.path)

const username = computed(() => userStore.userInfo?.username || '管理员')

const wsConnected = computed(() => eventStore.wsConnected)

const todayCount = ref(0)
const pendingCount = ref(0)
const currentDate = ref('')

const systemStatus = ref({
  cpu: 35,
  memory: 62,
  disk: 48
})

const selectedIncident = ref(null)
const showDetailModal = ref(false)

const updateDate = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    weekday: 'short'
  }
  currentDate.value = now.toLocaleDateString('zh-CN', options)
}

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#00ff88'
  if (percentage < 80) return '#ffaa00'
  return '#ff3366'
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      logout()
      break
  }
}

const logout = async () => {
  await userStore.logout()
  router.push('/login')
}

const handleSelectEvent = (incident) => {
  selectedIncident.value = incident
  showDetailModal.value = true
}

const handleConfirm = async (incident) => {
  try {
    await post(`/admin/incident/confirm/${incident.id}`)
    ElMessage.success('事件已确认')
    showDetailModal.value = false
  } catch (error) {
    ElMessage.error('确认失败: ' + (error.message || '未知错误'))
  }
}

const handleReject = async (incident) => {
  try {
    await post(`/admin/incident/reject/${incident.id}`, { reason: '审核驳回' })
    ElMessage.success('事件已驳回')
    showDetailModal.value = false
  } catch (error) {
    ElMessage.error('驳回失败: ' + (error.message || '未知错误'))
  }
}

const handleDispatchEvent = (incident) => {
  console.log('派单:', incident)
}

const handleStatsUpdate = () => {
  // Stats are managed by fetchStatistics() from the API — avoid overriding with filtered card list data
  fetchStatistics()
}

const fetchStatistics = async () => {
  try {
    const res = await get('/admin/statistics/today')
    const data = res.data || res || {}
    todayCount.value = data.todayCount || data.todayEvents || 0
    pendingCount.value = data.pendingCount || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const handleNewIncident = () => {
  fetchStatistics()
}

const handleDataReset = () => {
  todayCount.value = 0
  pendingCount.value = 0
  fetchStatistics()
}

let dateTimer = null
let statusTimer = null

onMounted(() => {
  updateDate()
  dateTimer = setInterval(updateDate, 60000)
  
  statusTimer = setInterval(() => {
    systemStatus.value = {
      cpu: Math.min(95, Math.max(15, systemStatus.value.cpu + (Math.random() - 0.5) * 5)),
      memory: Math.min(90, Math.max(40, systemStatus.value.memory + (Math.random() - 0.5) * 3)),
      disk: Math.min(80, Math.max(30, systemStatus.value.disk + (Math.random() - 0.5) * 1))
    }
  }, 10000)
  
  fetchStatistics()
  
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.on(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.on(MessageType.DATA_RESET, handleDataReset)
})

onUnmounted(() => {
  if (dateTimer) clearInterval(dateTimer)
  if (statusTimer) clearInterval(statusTimer)
  
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.off(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.off(MessageType.DATA_RESET, handleDataReset)
})
</script>

<style scoped>
.dashboard-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
  overflow: hidden;
}

/* 顶部状态栏 */
.top-bar {
  height: 60px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(180deg, rgba(0, 20, 40, 0.95) 0%, rgba(10, 14, 23, 0.95) 100%);
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.3);
  z-index: 100;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border-radius: 10px;
  color: white;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.4);
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-color);
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
  letter-spacing: 2px;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.status-item .el-icon {
  font-size: 16px;
}

.status-item.highlight {
  color: var(--primary-color);
}

.status-item.highlight em {
  font-style: normal;
  font-weight: 600;
  color: var(--primary-light);
}

.status-item.warning {
  color: var(--warning-color);
}

.status-item.warning em {
  font-style: normal;
  font-weight: 600;
}

.status-item.success .online {
  color: var(--success-color);
}

.status-item.success .offline {
  color: var(--danger-color);
}

.status-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-avatar:hover {
  background: var(--bg-card-hover);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
}

.dropdown-icon {
  font-size: 12px;
  color: var(--text-muted);
  transition: transform var(--transition-fast);
}

.user-avatar:hover .dropdown-icon {
  transform: rotate(180deg);
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧菜单 */
.side-menu {
  width: 220px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.menu-header {
  padding: 16px 20px;
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid var(--border-color);
}

.nav-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 8px 0;
}

.nav-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.nav-menu .el-menu-item:hover {
  background: var(--bg-card-hover);
  color: var(--primary-color);
}

.nav-menu .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.2), transparent);
  color: var(--primary-color);
  border-left: 3px solid var(--primary-color);
}

.nav-menu .el-menu-item .el-icon {
  font-size: 18px;
  margin-right: 8px;
}

/* 系统状态面板 */
.system-status {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.status-header {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-row .label {
  width: 36px;
  font-size: 12px;
  color: var(--text-secondary);
}

.status-row .el-progress {
  flex: 1;
}

/* 主内容区 */
.content-area {
  flex: 1;
  padding: 20px;
  overflow: hidden;
  background: var(--bg-dark);
}

/* 右侧事件面板 */
.event-panel {
  width: 320px;
  background: var(--bg-card);
  border-left: 1px solid var(--border-color);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Element Plus 下拉菜单样式覆盖 */
:deep(.el-dropdown-menu) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-card);
}

:deep(.el-dropdown-menu__item) {
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: var(--bg-card-hover);
  color: var(--primary-color);
}

/* 进度条样式覆盖 */
:deep(.el-progress-bar__outer) {
  background: rgba(0, 0, 0, 0.3);
}

:deep(.el-progress-bar__inner) {
  border-radius: 3px;
}

:deep(.el-progress__text) {
  color: var(--text-muted);
  font-size: 12px !important;
}
</style>
