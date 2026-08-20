<template>
  <div class="event-card-list">
    <div class="list-header">
      <div class="header-title">
        <el-icon><Bell /></el-icon>
        <span>事件列表</span>
        <el-badge :value="filteredIncidents.length" :max="99" class="count-badge" />
      </div>
      <el-button 
        text 
        type="primary" 
        size="small"
        @click="refreshEvents"
        :loading="loading"
      >
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>
    
    <div class="filter-bar">
      <el-select 
        v-model="filterType" 
        placeholder="灾种筛选" 
        size="small"
        clearable
        class="filter-select"
      >
        <el-option label="全部灾种" value="" />
        <el-option 
          v-for="type in disasterTypes" 
          :key="type.value" 
          :label="type.label" 
          :value="type.value"
        >
          <span class="filter-option">
            <span class="type-dot" :style="{ background: type.color }"></span>
            {{ type.label }}
          </span>
        </el-option>
      </el-select>
      
      <el-select 
        v-model="filterStatus" 
        placeholder="状态筛选" 
        size="small"
        clearable
        class="filter-select"
      >
        <el-option label="全部状态" value="" />
        <el-option label="待审核" :value="0" />
        <el-option label="已确认" :value="1" />
        <el-option label="已派单" :value="2" />
        <el-option label="处理中" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已驳回" :value="5" />
      </el-select>
    </div>
    
    <div class="search-bar">
      <el-input 
        v-model="searchKeyword"
        placeholder="搜索事件..."
        size="small"
        clearable
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <div class="card-container" ref="cardContainer">
      <TransitionGroup name="list">
        <EventCard
          v-for="incident in filteredIncidents"
          :key="incident.id"
          :incident="incident"
          @click="handleCardClick(incident)"
        />
      </TransitionGroup>
      
      <div v-if="filteredIncidents.length === 0" class="empty-state">
        <el-icon :size="48"><Document /></el-icon>
        <p>暂无事件数据</p>
        <p class="sub-text">开启模拟数据源后，事件将自动出现</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useEventStore } from '@/stores/event'
import { wsClient, MessageType } from '@/api/websocket'
import { get } from '@/api/request'
import EventCard from './EventCard.vue'

const emit = defineEmits(['select-event', 'stats-update'])

const eventStore = useEventStore()

const cardContainer = ref(null)
const loading = ref(false)
const filterType = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')
const incidents = ref([])
const confidenceThreshold = ref(60)

const disasterTypes = [
  { value: '洪涝', label: '洪涝', color: '#00d4ff' },
  { value: '火灾', label: '火灾', color: '#ff3366' },
  { value: '地震', label: '地震', color: '#ffaa00' },
  { value: '交通事故', label: '交通事故', color: '#ff6b6b' },
  { value: '燃气泄漏', label: '燃气泄漏', color: '#ffd93d' },
  { value: '其他', label: '其他', color: '#666666' }
]

const filteredIncidents = computed(() => {
  let result = incidents.value.filter(i => {
    if (i.status === 5) return false
    if ((i.confidence || 0) < confidenceThreshold.value) return false
    return true
  })
  
  if (filterType.value) {
    result = result.filter(i => i.disasterType === filterType.value)
  }
  
  if (filterStatus.value !== '' && filterStatus.value !== null) {
    result = result.filter(i => i.status === filterStatus.value)
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(i => 
      (i.summary || '').toLowerCase().includes(keyword) ||
      (i.locationText || '').toLowerCase().includes(keyword)
    )
  }
  
  result = [...result].sort((a, b) => {
    const severityOrder = { 3: 0, 2: 1, 1: 2, 0: 3 }
    const severityDiff = (severityOrder[a.severity] || 4) - (severityOrder[b.severity] || 4)
    if (severityDiff !== 0) return severityDiff
    return new Date(b.createTime) - new Date(a.createTime)
  })
  
  return result
})

const handleCardClick = (incident) => {
  emit('select-event', incident)
}

const fetchThreshold = async () => {
  try {
    const res = await get('/admin/settings/current')
    const data = res.data || res || {}
    confidenceThreshold.value = data.confidenceThreshold || 60
  } catch (error) {
    console.error('获取置信度阈值失败:', error)
  }
}

const fetchIncidents = async () => {
  loading.value = true
  try {
    const res = await get('/admin/incident/list')
    const data = res.data || res || []
    incidents.value = data
    emitStats()
  } catch (error) {
    console.error('获取事件列表失败:', error)
    incidents.value = []
  } finally {
    loading.value = false
  }
}

const emitStats = () => {
  const allIncidents = incidents.value
  const highConfidence = allIncidents.filter(i => (i.confidence || 0) >= confidenceThreshold.value && i.status !== 5).length
  const rejected = allIncidents.filter(i => i.status === 5).length
  const pending = allIncidents.filter(i => i.status === 0 && (i.confidence || 0) >= confidenceThreshold.value).length
  
  emit('stats-update', {
    total: allIncidents.length,
    highConfidence,
    rejected,
    pending
  })
}

const refreshEvents = () => {
  fetchIncidents()
}

const handleNewIncident = (data) => {
  const incident = data.payload || data
  const exists = incidents.value.find(i => i.id === incident.id)
  if (!exists) {
    incidents.value.unshift(incident)
    emitStats()
  }
}

const handleIncidentUpdate = (data) => {
  const update = data.payload || data
  const index = incidents.value.findIndex(i => i.id === update.id)
  if (index !== -1) {
    if (update.newStatus === 5) {
      incidents.value[index].status = 5
    } else {
      incidents.value[index].status = update.newStatus
    }
    emitStats()
  }
}

const handleDataReset = () => {
  incidents.value = []
  emitStats()
}

let refreshTimer = null

onMounted(() => {
  fetchThreshold()
  fetchIncidents()
  
  // Use 30-second refresh as fallback; WebSocket handles real-time updates
  refreshTimer = setInterval(fetchIncidents, 30000)
  
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.on(MessageType.DATA_RESET, handleDataReset)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.off(MessageType.DATA_RESET, handleDataReset)
})
</script>

<style scoped>
.event-card-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
}

.list-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.count-badge {
  margin-left: 4px;
}

.count-badge :deep(.el-badge__content) {
  background: var(--primary-color);
}

.filter-bar {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid var(--border-color);
}

.filter-select {
  width: 100%;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.search-bar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.search-input {
  width: 100%;
}

.card-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: var(--text-muted);
}

.empty-state .el-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.empty-state .sub-text {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.7;
}

:deep(.el-select) {
  --el-select-input-focus-border-color: var(--primary-color);
}

:deep(.el-input__wrapper) {
  background: var(--bg-darker) !important;
  border-color: var(--border-color) !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color) !important;
}

:deep(.el-input__inner) {
  color: var(--text-primary) !important;
}

:deep(.el-input__inner::placeholder) {
  color: var(--text-muted) !important;
}
</style>
