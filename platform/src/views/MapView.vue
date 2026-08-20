<template>
  <div class="map-view">
    <MapScene 
      ref="mapSceneRef"
      :incidents="incidents"
      @select-incident="handleSelectIncident"
    />
    
    <EventDetailModal
      v-model="showDetailModal"
      :incident="selectedIncident"
      @confirm="handleConfirm"
      @reject="handleReject"
      @dispatch="handleDispatch"
    />
    
    <DispatchModal
      v-model:visible="showDispatchModal"
      :incident="selectedIncident"
      :responders="availableResponders"
      @submit="handleSubmitDispatch"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import MapScene from '@/components/MapScene.vue'
import EventDetailModal from '@/components/EventDetailModal.vue'
import DispatchModal from '@/components/DispatchModal.vue'
import { wsClient, MessageType } from '@/api/websocket'
import { get, post } from '@/api/request'
import { ElMessage } from 'element-plus'

const mapSceneRef = ref(null)
const selectedIncident = ref(null)
const showDetailModal = ref(false)
const showDispatchModal = ref(false)
const incidents = ref([])
const responders = ref([])

const availableResponders = computed(() => {
  return responders.value.filter(r => r.status === 1)
})

const fetchIncidents = async () => {
  try {
    const res = await get('/admin/incident/list')
    const data = res.data || res || []
    incidents.value = data.filter(i => i.status !== 5 && i.lat && i.lng)
  } catch (error) {
    console.error('获取地图事件失败:', error)
    incidents.value = []
  }
}

const fetchResponders = async () => {
  try {
    const res = await get('/admin/responder/list')
    responders.value = res.data || res || []
  } catch (error) {
    console.error('获取执勤人员失败:', error)
    responders.value = []
  }
}

const handleSelectIncident = (incident) => {
  selectedIncident.value = incident
  showDetailModal.value = true
}

const handleConfirm = async (incident) => {
  try {
    await post(`/admin/incident/confirm/${incident.id}`)
    ElMessage.success('事件已确认')
    showDetailModal.value = false
    const index = incidents.value.findIndex(i => i.id === incident.id)
    if (index !== -1) {
      const updated = [...incidents.value]
      updated[index] = { ...updated[index], status: 1 }
      incidents.value = updated
    }
  } catch (error) {
    ElMessage.error('确认失败: ' + (error.message || '未知错误'))
  }
}

const handleReject = async (incident) => {
  try {
    await post(`/admin/incident/reject/${incident.id}`, { reason: '审核驳回' })
    ElMessage.success('事件已驳回')
    showDetailModal.value = false
    incidents.value = incidents.value.filter(i => i.id !== incident.id)
  } catch (error) {
    ElMessage.error('驳回失败: ' + (error.message || '未知错误'))
  }
}

const handleDispatch = (incident) => {
  showDetailModal.value = false
  selectedIncident.value = incident
  
  if (availableResponders.value.length === 0) {
    ElMessage.warning('当前没有可用的执勤人员')
    return
  }
  
  showDispatchModal.value = true
}

const handleSubmitDispatch = async (data) => {
  try {
    await post('/admin/task/dispatch', {
      incidentId: selectedIncident.value.id,
      responderId: data.responderId,
      description: data.description,
      deadline: data.deadline,
      notes: data.notes
    })
    ElMessage.success('派单成功')
    showDispatchModal.value = false
    
    const index = incidents.value.findIndex(i => i.id === selectedIncident.value.id)
    if (index !== -1) {
      const updated = [...incidents.value]
      updated[index] = { ...updated[index], status: 2 }
      incidents.value = updated
    }
    
    fetchResponders()
  } catch (error) {
    ElMessage.error('派单失败: ' + (error.message || '未知错误'))
  }
}

const handleNewIncident = (data) => {
  const incident = data.payload || data
  if (incident.status !== 5 && incident.lat && incident.lng) {
    const exists = incidents.value.find(i => i.id === incident.id)
    if (!exists) {
      incidents.value = [...incidents.value, incident]
    }
  }
}

const handleIncidentUpdate = (data) => {
  const update = data.payload || data
  const index = incidents.value.findIndex(i => i.id === update.id)
  if (index !== -1) {
    if (update.newStatus === 5) {
      // 已驳回的事件从地图移除
      incidents.value = incidents.value.filter(i => i.id !== update.id)
    } else if (update.newStatus === 4) {
      // 已完成的事件从地图移除
      incidents.value = incidents.value.filter(i => i.id !== update.id)
    } else {
      // 更新已有事件状态 - 创建新数组触发响应式
      const updated = [...incidents.value]
      updated[index] = { ...updated[index], status: update.newStatus }
      incidents.value = updated
    }
  } else {
    // 如果是新的非驳回事件，refetch以获取完整数据
    if (update.newStatus !== 5 && update.newStatus !== 4) {
      fetchIncidents()
    }
  }
}

const handleTaskUpdate = () => {
  // 任务状态更新时刷新人员列表
  fetchResponders()
}

const handleResponderUpdate = () => {
  fetchResponders()
}

onMounted(() => {
  fetchIncidents()
  fetchResponders()
  
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.on(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.on(MessageType.RESPONDER_UPDATE, handleResponderUpdate)
})

onUnmounted(() => {
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.off(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.off(MessageType.RESPONDER_UPDATE, handleResponderUpdate)
})
</script>

<style scoped>
.map-view {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
