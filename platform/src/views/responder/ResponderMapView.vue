<template>
  <div class="responder-map-view">
    <MapScene
      :incidents="incidents"
      :lite="true"
      @select-incident="handleSelectIncident"
    />

    <!-- 任务信息面板 -->
    <div class="task-info-panel" v-if="currentTask">
      <div class="panel-header">
        <span class="header-dot"></span>
        当前任务
      </div>
      <div class="panel-body">
        <div class="task-summary">{{ currentTask.summary }}</div>
        <div class="task-meta">
          <span class="meta-tag" :style="{ color: getTypeColor(currentTask.disasterType) }">
            {{ currentTask.disasterType }}
          </span>
          <span class="meta-severity" :class="'severity-' + currentTask.severity">
            {{ ['轻微','一般','严重','特别严重'][currentTask.severity] }}
          </span>
        </div>
        <div class="task-location" v-if="currentTask.locationText">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
          {{ currentTask.locationText }}
        </div>
      </div>
    </div>

    <!-- 无任务提示 -->
    <div class="no-task-panel" v-else-if="!loading">
      <div class="panel-header">
        <span class="header-dot"></span>
        态势地图
      </div>
      <div class="panel-body">
        <p>暂无进行中的任务</p>
      </div>
    </div>

    <!-- 选中事件信息 -->
    <div class="selected-panel" v-if="selectedIncident">
      <div class="panel-header">
        <span class="header-dot green"></span>
        事件详情
        <span class="close-btn" @click="selectedIncident = null">&times;</span>
      </div>
      <div class="panel-body">
        <div class="task-summary">{{ selectedIncident.summary }}</div>
        <div class="task-meta">
          <span class="meta-tag" :style="{ color: getTypeColor(selectedIncident.disasterType) }">
            {{ selectedIncident.disasterType }}
          </span>
          <span class="meta-severity" :class="'severity-' + selectedIncident.severity">
            {{ ['轻微','一般','严重','特别严重'][selectedIncident.severity] }}
          </span>
        </div>
        <div class="task-location" v-if="selectedIncident.locationText">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
          {{ selectedIncident.locationText }}
        </div>
        <div class="task-status">
          状态：{{ ['待审核','已确认','已派单','处理中','已完成','已驳回'][selectedIncident.status] }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import MapScene from '@/components/MapScene.vue'
import { responderPortalApi } from '@/api/responderPortal'
import { wsClient, MessageType } from '@/api/websocket'

const incidents = ref([])
const currentTask = ref(null)
const selectedIncident = ref(null)
const loading = ref(true)

const typeColors = {
  '洪涝': '#00d4ff', '火灾': '#ff3366', '地震': '#ffaa00',
  '台风': '#00ff88', '泥石流': '#9966ff'
}

const getTypeColor = (type) => typeColors[type] || '#666'

const fetchData = async () => {
  loading.value = true
  try {
    // 获取当前正在处理的事件
    const currentRes = await responderPortalApi.getCurrentTask()
    if (currentRes.code === 1 && currentRes.data) {
      currentTask.value = currentRes.data
    }

    // 获取关联的所有事件（用于地图标记）
    const myRes = await responderPortalApi.getMyIncidents()
    if (myRes.code === 1 && Array.isArray(myRes.data)) {
      incidents.value = myRes.data.filter(i => i.lat && i.lng)
    }

    // 如果有当前任务但不在列表中，加入
    if (currentTask.value && currentTask.value.lat && currentTask.value.lng) {
      const exists = incidents.value.find(i => i.id === currentTask.value.id)
      if (!exists) {
        incidents.value.push(currentTask.value)
      }
    }
  } catch (e) {
    console.error('获取任务数据失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSelectIncident = (incident) => {
  selectedIncident.value = incident
}

const handleTaskUpdate = () => {
  fetchData()
}

const handleIncidentUpdate = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
  wsClient.on(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
})

onUnmounted(() => {
  wsClient.off(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
})
</script>

<style scoped>
.responder-map-view {
  width: 100%;
  height: calc(100vh - 100px);
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.task-info-panel, .no-task-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 50;
  width: 300px;
  background: rgba(3, 8, 16, 0.88);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.selected-panel {
  position: absolute;
  bottom: 20px;
  right: 16px;
  z-index: 50;
  width: 300px;
  background: rgba(3, 8, 16, 0.88);
  border: 1px solid rgba(0, 255, 136, 0.2);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #e0e6ed;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 8px #00d4ff;
}

.header-dot.green {
  background: #00ff88;
  box-shadow: 0 0 8px #00ff88;
}

.close-btn {
  margin-left: auto;
  cursor: pointer;
  color: rgba(224, 230, 237, 0.4);
  font-size: 18px;
}

.close-btn:hover {
  color: #ff3366;
}

.panel-body {
  padding: 14px 16px;
}

.task-summary {
  font-size: 14px;
  color: #e0e6ed;
  line-height: 1.5;
  margin-bottom: 10px;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.meta-tag {
  font-size: 12px;
  font-weight: 600;
}

.meta-severity {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(224, 230, 237, 0.6);
}

.meta-severity.severity-2 {
  background: rgba(255, 170, 0, 0.15);
  color: #ffaa00;
}

.meta-severity.severity-3 {
  background: rgba(255, 51, 102, 0.15);
  color: #ff3366;
}

.task-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(224, 230, 237, 0.5);
  margin-bottom: 6px;
}

.task-status {
  font-size: 12px;
  color: rgba(0, 212, 255, 0.7);
  margin-top: 6px;
}

.no-task-panel .panel-body p {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.4);
  margin: 0;
}
</style>
