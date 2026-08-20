<template>
  <div class="event-stream-view">
    <header class="stream-header">
      <div class="header-left">
        <div class="header-indicator">
          <span class="indicator-dot"></span>
          <span class="indicator-ring"></span>
        </div>
        <h1 class="page-title">事件流监控</h1>
        <span class="page-subtitle">REAL-TIME EVENT STREAM</span>
      </div>
      <div class="header-right">
        <div class="stream-status" :class="{ active: isStreaming }">
          <el-icon><Connection /></el-icon>
          <span>{{ isStreaming ? '实时接收中' : '已暂停' }}</span>
        </div>
        <el-tag :type="wsConnected ? 'success' : 'danger'">
          WebSocket: {{ wsConnected ? '已连接' : '未连接' }}
        </el-tag>
        <el-button 
          :type="isStreaming ? 'danger' : 'primary'" 
          @click="toggleStream"
        >
          <el-icon><VideoPlay v-if="!isStreaming" /><VideoPause v-else /></el-icon>
          {{ isStreaming ? '暂停接收' : '开始接收' }}
        </el-button>
        <el-button @click="clearMessages">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
        <el-button type="warning" @click="handleBatchDispatch" :loading="batchDispatching">
          <el-icon><Promotion /></el-icon>
          一键智能派单
        </el-button>
      </div>
    </header>

    <main class="stream-main">
      <div class="stream-container">
        <div class="message-stream" ref="messageStreamRef">
          <div class="stream-empty" v-if="messageList.length === 0">
            <el-icon :size="48"><ChatLineRound /></el-icon>
            <p>暂无消息流</p>
            <p class="hint">请在系统设置中开启模拟数据源</p>
          </div>
          <TransitionGroup name="message" tag="div" class="message-list" v-else>
            <div 
              v-for="msg in messageList" 
              :key="msg.id" 
              class="message-item"
              :class="{
                'high-confidence': msg.confidence >= 70,
                'low-confidence': msg.confidence < 50 && msg.confidence > 0,
                'is-rumor': msg.infoType === '谣言' || msg.confidence <= 30
              }"
            >
              <!-- 谣言醒目横幅 -->
              <div class="rumor-banner" v-if="msg.infoType === '谣言' || msg.confidence <= 30">
                <span class="rumor-icon">⚠</span>
                <span class="rumor-label">疑似谣言</span>
                <span class="rumor-hint">AI研判低可信度信息</span>
              </div>
              <!-- 高可信标记 -->
              <div class="credible-banner" v-else-if="msg.confidence >= 80">
                <span class="credible-icon">✓</span>
                <span class="credible-label">高可信度</span>
              </div>
              <div class="message-header">
                <div class="source-info">
                  <el-tag size="small" :type="getSourceType(msg.sourcePlatform)">
                    {{ msg.sourcePlatform || '未知来源' }}
                  </el-tag>
                  <span class="publisher">{{ msg.publisherName || '匿名用户' }}</span>
                </div>
                <span class="time">{{ formatTime(msg.publishTime) }}</span>
              </div>
              
              <div class="message-content">
                <p class="original-text">{{ msg.originalText }}</p>
                <p class="cleaned-text" v-if="msg.cleanedText && msg.cleanedText !== msg.originalText">
                  <el-icon><Edit /></el-icon>
                  清洗后：{{ msg.cleanedText }}
                </p>
              </div>

              <div class="message-meta">
                <div class="location" v-if="msg.locationText">
                  <el-icon><Location /></el-icon>
                  {{ msg.locationText }}
                </div>
              </div>

              <div class="ai-analysis" v-if="msg.aiAnalysis">
                <div class="analysis-header">
                  <el-icon><Cpu /></el-icon>
                  <span>AI研判结果</span>
                </div>
                <div class="analysis-content">
                  <div class="analysis-item">
                    <span class="label">灾害类型：</span>
                    <el-tag size="small" type="warning">{{ msg.disasterType || '未知' }}</el-tag>
                  </div>
                  <div class="analysis-item">
                    <span class="label">信息类型：</span>
                    <el-tag size="small" :type="msg.infoType === '谣言' ? 'danger' : 'info'">
                      {{ msg.infoType || '未知' }}
                    </el-tag>
                  </div>
                  <div class="analysis-item">
                    <span class="label">严重程度：</span>
                    <el-rate 
                      :model-value="msg.severity + 1" 
                      :max="4" 
                      disabled 
                      show-score 
                      text-color="#ff9900"
                    />
                  </div>
                  <div class="analysis-item">
                    <span class="label">置信度：</span>
                    <el-progress 
                      :percentage="msg.confidence || 0" 
                      :color="getConfidenceColor(msg.confidence)"
                      :stroke-width="10"
                    />
                  </div>
                </div>
              </div>

              <div class="message-footer">
                <div class="status-info">
                  <el-tag 
                    :type="msg.status === 3 ? 'success' : 'info'" 
                    size="small"
                  >
                    {{ getStatusText(msg.status) }}
                  </el-tag>
                  <el-tag 
                    v-if="msg.incidentId" 
                    type="success" 
                    size="small"
                  >
                    已聚合到事件 #{{ msg.incidentId }}
                  </el-tag>
                  <el-tag 
                    v-if="msg.infoType === '谣言' || msg.confidence <= 30" 
                    type="danger" 
                    size="small"
                    effect="dark"
                  >
                    谣言标记
                  </el-tag>
                </div>
              </div>
            </div>
          </TransitionGroup>
        </div>

        <aside class="stream-sidebar">
          <div class="sidebar-section">
            <h3 class="section-title">实时统计</h3>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-value">{{ stats.totalMessages }}</span>
                <span class="stat-label">消息总数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ stats.highConfidence }}</span>
                <span class="stat-label">高置信度</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ stats.rumors }}</span>
                <span class="stat-label">谣言识别</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ stats.rejected }}</span>
                <span class="stat-label">已驳回</span>
              </div>
            </div>
          </div>

          <div class="sidebar-section">
            <h3 class="section-title">置信度阈值</h3>
            <el-slider 
              v-model="confidenceThreshold" 
              :min="0" 
              :max="100" 
              :marks="{ 0: '0%', 50: '50%', 100: '100%' }"
              :format-tooltip="val => val + '%'"
              @change="updateThreshold"
            />
            <p class="threshold-hint">
              置信度 ≥ {{ confidenceThreshold }}% 的消息将自动生成事件
            </p>
          </div>

          <div class="sidebar-section">
            <h3 class="section-title">最新事件</h3>
            <div class="incident-list">
              <div 
                v-for="incident in recentIncidents" 
                :key="incident.id" 
                class="incident-item"
                @click="viewIncident(incident)"
              >
                <div class="incident-type">
                  <el-tag size="small" :type="getSeverityType(incident.severity)">
                    {{ incident.disasterType }}
                  </el-tag>
                </div>
                <div class="incident-summary">{{ incident.summary }}</div>
                <div class="incident-meta">
                  <span class="confidence">
                    置信度: {{ incident.confidence }}%
                  </span>
                  <span class="time">{{ formatTime(incident.createTime) }}</span>
                </div>
              </div>
              <div v-if="recentIncidents.length === 0" class="empty-hint">
                暂无事件
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { wsClient, MessageType } from '@/api/websocket'
import { settingsApi } from '@/api/settings'
import { get, post } from '@/api/request'
import { useEventStore } from '@/stores/event'

const router = useRouter()
const eventStore = useEventStore()

const messageStreamRef = ref(null)
const messageList = computed(() => eventStore.messageList)
const recentIncidents = ref([])
const allIncidents = ref([])
const isStreaming = ref(false)
const wsConnected = computed(() => wsClient.isConnected())
const confidenceThreshold = computed({
  get: () => eventStore.confidenceThreshold,
  set: (val) => eventStore.setConfidenceThreshold(val)
})

const stats = computed(() => {
  const totalMessages = messageList.value.length
  const highConfidence = messageList.value.filter(m => (m.confidence || 0) >= confidenceThreshold.value).length
  const rumors = messageList.value.filter(m => (m.confidence || 0) <= 30).length
  const rejected = allIncidents.value.filter(i => i.status === 5).length
  return { totalMessages, highConfidence, rumors, rejected }
})

const goBack = () => {
  router.push('/dashboard')
}

const toggleStream = async () => {
  try {
    // 始终先从后端获取真实状态，避免前后端状态不同步
    try {
      const statusRes = await settingsApi.getSettings()
      const statusData = statusRes.data || statusRes
      isStreaming.value = statusData.dataSourceEnabled || false
    } catch {}
    
    const newState = !isStreaming.value
    await settingsApi.toggleDataSource(newState)
    isStreaming.value = newState
    ElMessage.success(newState ? '已开始接收消息流' : '已暂停接收')
  } catch (error) {
    // 出错时重新同步状态
    try {
      const statusRes = await settingsApi.getSettings()
      const statusData = statusRes.data || statusRes
      isStreaming.value = statusData.dataSourceEnabled || false
    } catch {}
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

const clearMessages = () => {
  eventStore.clearMessageList()
  recentIncidents.value = []
  allIncidents.value = []
  ElMessage.success('已清空消息')
}

const batchDispatching = ref(false)
const handleBatchDispatch = async () => {
  batchDispatching.value = true
  try {
    const res = await post('/admin/dispatch/batch-smart')
    const data = res.data || res || {}
    const total = data.total || 0
    const success = data.success || 0
    const fail = data.fail || 0
    if (total === 0) {
      ElMessage.warning('当前没有已确认待派单的事件')
    } else {
      ElMessage.success(`批量派单完成：共${total}个事件，成功${success}个，失败${fail}个`)
    }
    fetchRecentIncidents()
  } catch (error) {
    ElMessage.error('批量派单失败：' + (error.message || '未知错误'))
  } finally {
    batchDispatching.value = false
  }
}

const updateThreshold = async (val) => {
  try {
    await settingsApi.updateThreshold(val)
    ElMessage.success(`置信度阈值已更新为 ${val}%`)
  } catch (error) {
    console.error('更新阈值失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const getSourceType = (source) => {
  const types = {
    '微信群聊': 'success',
    'QQ群聊': 'primary',
    '微博': 'warning'
  }
  return types[source] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待清洗',
    1: '已清洗',
    2: '已研判',
    3: '已聚合'
  }
  return texts[status] || '未知'
}

const getConfidenceColor = (confidence) => {
  if (confidence >= 80) return '#00ff88'
  if (confidence >= 60) return '#00d4ff'
  if (confidence >= 40) return '#ffaa00'
  return '#ff3366'
}

const getSeverityType = (severity) => {
  const types = ['info', 'warning', 'danger', 'danger']
  return types[severity] || 'info'
}

const viewIncident = (incident) => {
  ElMessage.info(`查看事件详情: ${incident.summary}`)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageStreamRef.value) {
      messageStreamRef.value.scrollTop = messageStreamRef.value.scrollHeight
    }
  })
}

const handleNewIncident = (data) => {
  const incident = data.payload || data
  allIncidents.value.unshift(incident)
  if (incident.status !== 5) {
    recentIncidents.value.unshift(incident)
    if (recentIncidents.value.length > 10) {
      recentIncidents.value = recentIncidents.value.slice(0, 10)
    }
  }
}

const handleIncidentUpdate = (data) => {
  const update = data.payload || data
  const index = allIncidents.value.findIndex(i => i.id === update.id)
  if (index !== -1) {
    allIncidents.value[index].status = update.newStatus
  }
}

const handleDataReset = () => {
  // 后端重置数据时同步前端状态
  isStreaming.value = false
  recentIncidents.value = []
  allIncidents.value = []
  ElMessage.info('数据已重置，模拟推送已停止')
}

const fetchRecentIncidents = async () => {
  try {
    const res = await get('/admin/incident/list')
    const incidents = res.data || res || []
    allIncidents.value = incidents
    recentIncidents.value = incidents.filter(i => i.status !== 5).slice(0, 10)
  } catch (error) {
    console.error('获取事件列表失败:', error)
  }
}

const checkStreamStatus = async () => {
  try {
    const res = await settingsApi.getSettings()
    const data = res.data || res
    isStreaming.value = data.dataSourceEnabled || false
    eventStore.setConfidenceThreshold(data.confidenceThreshold || 60)
  } catch (error) {
    console.error('获取设置失败:', error)
  }
}

onMounted(() => {
  // NEW_MESSAGE 由全局 event store 持久监听，无需在此注册
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.on(MessageType.DATA_RESET, handleDataReset)
  
  checkStreamStatus()
  fetchRecentIncidents()
})

onUnmounted(() => {
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.off(MessageType.DATA_RESET, handleDataReset)
})
</script>

<style scoped>
.event-stream-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
}

.stream-header {
  height: 60px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-indicator {
  position: relative;
  width: 12px;
  height: 12px;
}

.indicator-dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 8px #00d4ff;
}

.indicator-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(0, 212, 255, 0.4);
  animation: indicator-pulse 2s ease-in-out infinite;
}

@keyframes indicator-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.3; }
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.page-subtitle {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(0, 212, 255, 0.4);
  font-family: monospace;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stream-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(255, 68, 68, 0.2);
  color: #ff4444;
  font-size: 14px;
}

.stream-status.active {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.stream-main {
  flex: 1;
  overflow: hidden;
}

.stream-container {
  display: flex;
  height: 100%;
}

.message-stream {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.stream-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-muted);
}

.stream-empty .hint {
  font-size: 12px;
  margin-top: 8px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.message-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.message-item:hover {
  border-color: var(--primary-color);
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.1);
}

.message-item.high-confidence {
  border-left: 3px solid var(--success-color);
}

.message-item.low-confidence {
  border-left: 3px solid var(--danger-color);
}

.message-item.is-rumor {
  background: rgba(255, 51, 102, 0.08);
  border-left: 3px solid #ff3366;
  box-shadow: inset 0 0 20px rgba(255, 51, 102, 0.06);
  animation: rumor-pulse 3s ease-in-out infinite;
}

@keyframes rumor-pulse {
  0%, 100% { box-shadow: inset 0 0 20px rgba(255, 51, 102, 0.06); }
  50% { box-shadow: inset 0 0 30px rgba(255, 51, 102, 0.12); }
}

.rumor-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin-bottom: 12px;
  background: rgba(255, 51, 102, 0.12);
  border: 1px solid rgba(255, 51, 102, 0.25);
  border-radius: 6px;
}

.rumor-icon {
  font-size: 16px;
  animation: rumor-blink 1.5s ease-in-out infinite;
}

@keyframes rumor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.rumor-label {
  font-size: 13px;
  font-weight: 700;
  color: #ff3366;
  letter-spacing: 1px;
}

.rumor-hint {
  font-size: 11px;
  color: rgba(255, 51, 102, 0.6);
  margin-left: auto;
}

.credible-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  margin-bottom: 10px;
  background: rgba(0, 255, 136, 0.08);
  border: 1px solid rgba(0, 255, 136, 0.15);
  border-radius: 6px;
}

.credible-icon {
  color: #00ff88;
  font-weight: bold;
}

.credible-label {
  font-size: 12px;
  color: #00ff88;
  font-weight: 600;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.source-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.publisher {
  color: var(--text-secondary);
  font-size: 13px;
}

.time {
  color: var(--text-muted);
  font-size: 12px;
}

.message-content {
  margin-bottom: 12px;
}

.original-text {
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.cleaned-text {
  color: var(--text-secondary);
  font-size: 13px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 6px;
}

.message-meta {
  margin-bottom: 12px;
}

.location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--primary-color);
  font-size: 13px;
}

.ai-analysis {
  background: rgba(0, 51, 102, 0.3);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary-color);
  font-size: 14px;
  margin-bottom: 12px;
}

.analysis-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.analysis-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.analysis-item .label {
  color: var(--text-secondary);
  font-size: 13px;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-info {
  display: flex;
  gap: 8px;
}

.stream-sidebar {
  width: 320px;
  background: var(--bg-card);
  border-left: 1px solid var(--border-color);
  padding: 20px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  background: rgba(0, 51, 102, 0.3);
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  transition: all 0.3s ease;
}

.stat-item:hover {
  border-color: rgba(0, 212, 255, 0.3);
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.1);
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.4);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.threshold-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 12px;
  text-align: center;
}

.incident-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.incident-item {
  background: rgba(0, 51, 102, 0.3);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.incident-item:hover {
  background: rgba(0, 51, 102, 0.5);
}

.incident-type {
  margin-bottom: 8px;
}

.incident-summary {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.incident-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.empty-hint {
  text-align: center;
  color: var(--text-muted);
  padding: 20px;
}

.message-enter-active,
.message-leave-active {
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.message-enter-from {
  opacity: 0;
  transform: translateY(-30px) scale(0.97);
  filter: blur(4px);
}

.message-leave-to {
  opacity: 0;
  transform: translateX(30px) scale(0.97);
  filter: blur(4px);
}

.message-move {
  transition: transform 0.4s ease;
}
</style>
