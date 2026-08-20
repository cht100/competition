<template>
  <div class="dashboard-view">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon events">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">今日事件</p>
          <p class="stat-value">{{ statistics.todayEvents }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon alerts">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">待处理</p>
          <p class="stat-value">{{ statistics.pendingCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon devices">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">已确认</p>
          <p class="stat-value">{{ statistics.confirmedCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">总事件数</p>
          <p class="stat-value">{{ statistics.totalEvents }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon rumor">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-label">谣言</p>
          <p class="stat-value">{{ statistics.rumorCount }}</p>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <div class="content-card recent-events">
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Clock /></el-icon>
            最近事件
          </h3>
          <el-button type="primary" link size="small" @click="goToStream">
            查看事件流
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="events-list">
          <div
            v-for="event in recentEvents"
            :key="event.id"
            class="event-item"
            @click="handleEventClick(event)"
          >
            <div class="event-type" :class="getSeverityClass(event.severity)"></div>
            <div class="event-content">
              <p class="event-title">{{ event.summary || '未知事件' }}</p>
              <p class="event-time">{{ formatTime(event.createTime) }}</p>
            </div>
            <el-tag :type="getStatusTagType(event.status)" size="small">
              {{ getStatusText(event.status) }}
            </el-tag>
          </div>
          <el-empty v-if="recentEvents.length === 0" description="暂无事件，开启模拟数据源后事件将自动出现" />
        </div>
      </div>

      <div class="content-card quick-actions">
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Operation /></el-icon>
            快速操作
          </h3>
        </div>
        <div class="actions-grid">
          <div class="action-item" @click="goToStream">
            <div class="action-icon">
              <el-icon><View /></el-icon>
            </div>
            <span class="action-label">事件流监控</span>
          </div>
          <div class="action-item" @click="goToMap">
            <div class="action-icon">
              <el-icon><Location /></el-icon>
            </div>
            <span class="action-label">态势地图</span>
          </div>
          <div class="action-item" @click="goToSettings">
            <div class="action-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <span class="action-label">系统设置</span>
          </div>
          <div class="action-item" @click="goToStatistics">
            <div class="action-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <span class="action-label">数据分析</span>
          </div>
        </div>
      </div>

      <div class="content-card announcements">
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Notification /></el-icon>
            志愿者接单申请
          </h3>
          <el-badge :value="volunteerApps.length" :hidden="volunteerApps.length === 0" type="warning">
            <el-button type="primary" link size="small" @click="fetchVolunteerApps">刷新</el-button>
          </el-badge>
        </div>
        <div class="announcement-list" v-if="volunteerApps.length > 0">
          <div class="announcement-item volunteer-app" v-for="app in volunteerApps" :key="app.task?.id">
            <div class="announcement-dot" style="background: #ffaa00;"></div>
            <div class="announcement-content" style="flex: 1;">
              <p class="announcement-title">
                <strong>{{ app.responderName || '志愿者' }}</strong> 申请接受
                <el-tag size="small" type="warning" effect="plain" round>{{ app.disasterType }}</el-tag>
              </p>
              <p class="announcement-time">{{ app.incidentSummary }}</p>
            </div>
            <div class="volunteer-actions">
              <el-button type="success" size="small" @click="approveVolunteer(app.task?.id)">同意</el-button>
              <el-button type="danger" size="small" plain @click="rejectVolunteer(app.task?.id)">驳回</el-button>
            </div>
          </div>
        </div>
        <div class="announcement-list" v-else>
          <div class="announcement-item">
            <div class="announcement-dot"></div>
            <div class="announcement-content">
              <p class="announcement-title">暂无志愿者接单申请</p>
              <p class="announcement-time">志愿者主动接单后需管理员审批</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { wsClient, MessageType } from '@/api/websocket'
import { get, post } from '@/api/request'

const router = useRouter()

const statistics = ref({
  todayEvents: 0,
  pendingCount: 0,
  confirmedCount: 0,
  totalEvents: 0,
  rumorCount: 0
})

const recentEvents = ref([])
const volunteerApps = ref([])

const fetchStatistics = async () => {
  try {
    const res = await get('/admin/statistics/today')
    const data = res.data || res || {}
    statistics.value = {
      todayEvents: data.todayCount || 0,
      pendingCount: data.pendingCount || 0,
      confirmedCount: data.confirmedCount || 0,
      totalEvents: data.totalCount || 0,
      rumorCount: data.rumorTotalCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchRecentEvents = async () => {
  try {
    const res = await get('/admin/incident/list')
    const data = res.data || res || []
    // 按严重度降序、时间降序排列，取前5条
    recentEvents.value = [...data]
      .sort((a, b) => (b.severity || 0) - (a.severity || 0) || new Date(b.createTime) - new Date(a.createTime))
      .slice(0, 5)
  } catch (error) {
    console.error('获取最近事件失败:', error)
    recentEvents.value = []
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const getSeverityClass = (severity) => {
  const classes = ['info', 'warning', 'warning', 'error']
  return classes[severity] || 'info'
}

const getStatusTagType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'primary', 3: 'info', 4: 'info', 5: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待审核', 1: '已确认', 2: '已派单', 3: '处理中', 4: '已完成', 5: '已驳回' }
  return texts[status] || '未知'
}

const handleEventClick = (event) => {
  router.push('/map')
}

const goToStream = () => router.push('/stream')
const goToMap = () => router.push('/map')
const goToSettings = () => router.push('/settings')
const goToStatistics = () => router.push('/statistics')

const handleNewIncident = () => {
  fetchStatistics()
  fetchRecentEvents()
  fetchVolunteerApps()
}

const handleDataReset = () => {
  statistics.value = { todayEvents: 0, pendingCount: 0, confirmedCount: 0, totalEvents: 0, rumorCount: 0 }
  recentEvents.value = []
  volunteerApps.value = []
}

const fetchVolunteerApps = async () => {
  try {
    const res = await get('/admin/dispatch/volunteer-applications')
    volunteerApps.value = res.data || res || []
  } catch (error) {
    console.error('获取志愿者申请失败:', error)
    volunteerApps.value = []
  }
}

const approveVolunteer = async (taskId) => {
  try {
    await post(`/admin/dispatch/volunteer-applications/${taskId}/approve`)
    ElMessage.success('已同意志愿者申请')
    fetchVolunteerApps()
    fetchStatistics()
    fetchRecentEvents()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

const rejectVolunteer = async (taskId) => {
  try {
    await post(`/admin/dispatch/volunteer-applications/${taskId}/reject`)
    ElMessage.success('已驳回志愿者申请')
    fetchVolunteerApps()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  fetchStatistics()
  fetchRecentEvents()
  fetchVolunteerApps()
  
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.on(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.on(MessageType.TASK_UPDATE, handleNewIncident)
  wsClient.on(MessageType.DATA_RESET, handleDataReset)
})

onUnmounted(() => {
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.off(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.off(MessageType.TASK_UPDATE, handleNewIncident)
  wsClient.off(MessageType.DATA_RESET, handleDataReset)
})
</script>

<style scoped>
.dashboard-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);
}

.stat-card:hover {
  border-color: var(--primary-color);
  transform: translateY(-2px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
}

.stat-icon.events { background: rgba(0, 212, 255, 0.2); color: var(--primary-color); }
.stat-icon.alerts { background: rgba(255, 170, 0, 0.2); color: var(--warning-color); }
.stat-icon.devices { background: rgba(0, 255, 136, 0.2); color: var(--success-color); }
.stat-icon.total { background: rgba(102, 229, 255, 0.2); color: var(--primary-light); }
.stat-icon.rumor { background: rgba(255, 51, 102, 0.2); color: var(--danger-color); }

.stat-info { flex: 1; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; color: var(--text-primary); }

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: auto auto;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.content-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color);
  margin: 0;
}

.recent-events { grid-row: span 2; }
.events-list { flex: 1; overflow-y: auto; }

.event-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.event-item:hover {
  background: rgba(0, 212, 255, 0.1);
  transform: translateX(4px);
}

.event-type {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.event-type.info { background: var(--info-color); }
.event-type.warning { background: var(--warning-color); }
.event-type.error { background: var(--danger-color); }

.event-content { flex: 1; min-width: 0; }
.event-title { color: var(--text-primary); margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event-time { font-size: 12px; color: var(--text-muted); }

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-item:hover {
  background: rgba(0, 212, 255, 0.1);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border-radius: 10px;
  color: white;
  font-size: 20px;
}

.action-label { font-size: 13px; color: var(--text-secondary); }

.announcement-list { flex: 1; overflow-y: auto; }
.announcement-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px 0; }
.announcement-item.volunteer-app { align-items: center; }
.volunteer-actions { display: flex; gap: 8px; flex-shrink: 0; }
.announcement-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--primary-color); margin-top: 6px; flex-shrink: 0; }
.announcement-content { flex: 1; }
.announcement-title { font-size: 13px; color: var(--text-primary); margin-bottom: 4px; }
.announcement-time { font-size: 11px; color: var(--text-muted); }

:deep(.el-tag) { border-radius: 10px; }
:deep(.el-tag--danger) { background: rgba(255, 51, 102, 0.2); border-color: transparent; color: var(--danger-color); }
:deep(.el-tag--warning) { background: rgba(255, 170, 0, 0.2); border-color: transparent; color: var(--warning-color); }
:deep(.el-tag--success) { background: rgba(0, 255, 136, 0.2); border-color: transparent; color: var(--success-color); }
:deep(.el-tag--info) { background: rgba(0, 212, 255, 0.2); border-color: transparent; color: var(--primary-color); }
</style>
