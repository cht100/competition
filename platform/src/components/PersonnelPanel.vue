<template>
  <div class="personnel-panel">
    <div class="panel-header">
      <h3 class="panel-title">执勤人员</h3>
      <div class="status-summary">
        <span class="status-item idle">
          <span class="status-dot"></span>
          空闲: {{ idleCount }}
        </span>
        <span class="status-item busy">
          <span class="status-dot"></span>
          执行中: {{ busyCount }}
        </span>
        <span class="status-item total">
          总数: {{ totalCount }}
        </span>
      </div>
    </div>

    <div class="personnel-list" v-loading="loading">
      <div
        v-for="responder in responders"
        :key="responder.id"
        class="personnel-item"
        :class="getStatusClass(responder.status)"
      >
        <div class="avatar">
          <el-avatar :size="40">{{ responder.name.charAt(0) }}</el-avatar>
          <span class="status-indicator"></span>
        </div>
        <div class="info">
          <div class="name">{{ responder.name }}</div>
          <div class="position">{{ responder.position }}</div>
          <div class="current-task" v-if="responder.currentTask">
            <el-icon><Location /></el-icon>
            {{ responder.currentTask }}
          </div>
        </div>
        <div class="actions">
          <el-button
            v-if="responder.status === 1"
            type="primary"
            size="small"
            @click="openDispatchModal(responder)"
          >
            派单
          </el-button>
          <el-tag
            v-else-if="responder.status === 2 || responder.status === 3"
            type="warning"
            size="small"
          >
            {{ getStatusText(responder.status) }}
          </el-tag>
          <el-tag
            v-else-if="responder.status === 0"
            type="info"
            size="small"
          >
            离线
          </el-tag>
        </div>
      </div>

      <el-empty v-if="responders.length === 0 && !loading" description="暂无执勤人员" />
    </div>

    <DispatchModal
      v-model:visible="dispatchModalVisible"
      :responder="selectedResponder"
      :incidents="pendingIncidents"
      @submit="handleDispatch"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { responderApi } from '@/api/responder'
import { incidentApi } from '@/api/incident'
import DispatchModal from './DispatchModal.vue'

const loading = ref(false)
const responders = ref([])
const pendingIncidents = ref([])
const dispatchModalVisible = ref(false)
const selectedResponder = ref(null)

const idleCount = computed(() => {
  return responders.value.filter(r => r.status === 1).length
})

const busyCount = computed(() => {
  return responders.value.filter(r => r.status >= 2).length
})

const totalCount = computed(() => {
  return responders.value.length
})

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-offline',
    1: 'status-idle',
    2: 'status-dispatched',
    3: 'status-executing'
  }
  return classMap[status] || 'status-offline'
}

const getStatusText = (status) => {
  const textMap = {
    0: '离线',
    1: '空闲',
    2: '已派单',
    3: '执行中'
  }
  return textMap[status] || '未知'
}

const openDispatchModal = (responder) => {
  selectedResponder.value = responder
  dispatchModalVisible.value = true
}

const handleDispatch = async (data) => {
  try {
    await responderApi.dispatch({
      responderId: selectedResponder.value.id,
      ...data
    })
    ElMessage.success('派单成功')
    dispatchModalVisible.value = false
    loadResponders()
  } catch (error) {
    console.error('派单失败:', error)
  }
}

const loadResponders = async () => {
  loading.value = true
  try {
    const res = await responderApi.list()
    responders.value = res.data || res || []
  } catch (error) {
    console.error('加载人员列表失败:', error)
    responders.value = []
  } finally {
    loading.value = false
  }
}

const loadPendingIncidents = async () => {
  try {
    const res = await incidentApi.list()
    const all = res.data || res || []
    pendingIncidents.value = all.filter(i => i.status === 1)
  } catch (error) {
    console.error('加载待处理事件失败:', error)
    pendingIncidents.value = []
  }
}

let refreshTimer = null

const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    loadResponders()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadResponders()
  loadPendingIncidents()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.personnel-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.status-summary {
  display: flex;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-item .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-item.idle .status-dot {
  background: var(--success-color);
  box-shadow: 0 0 6px var(--success-color);
}

.status-item.busy .status-dot {
  background: var(--warning-color);
  box-shadow: 0 0 6px var(--warning-color);
}

.personnel-list {
  flex: 1;
  overflow-y: auto;
}

.personnel-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(0, 20, 40, 0.5);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
}

.personnel-item:hover {
  border-color: var(--primary-color);
  background: rgba(0, 212, 255, 0.05);
}

.personnel-item.status-idle {
  border-left: 3px solid var(--success-color);
}

.personnel-item.status-dispatched,
.personnel-item.status-executing {
  border-left: 3px solid var(--warning-color);
}

.personnel-item.status-offline {
  border-left: 3px solid var(--text-muted);
  opacity: 0.6;
}

.avatar {
  position: relative;
  flex-shrink: 0;
}

.status-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--bg-card);
}

.status-idle .status-indicator {
  background: var(--success-color);
  box-shadow: 0 0 6px var(--success-color);
}

.status-dispatched .status-indicator,
.status-executing .status-indicator {
  background: var(--warning-color);
  box-shadow: 0 0 6px var(--warning-color);
  animation: pulse 2s ease-in-out infinite;
}

.status-offline .status-indicator {
  background: var(--text-muted);
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.position {
  font-size: 12px;
  color: var(--text-muted);
}

.current-task {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--warning-color);
}

.actions {
  flex-shrink: 0;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
