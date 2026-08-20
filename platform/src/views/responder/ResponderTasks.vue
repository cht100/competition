<template>
  <div class="responder-tasks">
    <div class="section-header">
      <h3>
        <span class="header-dot"></span>
        历史任务记录
      </h3>
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="任务状态" clearable size="small" style="width: 130px">
          <el-option label="全部" :value="null" />
          <el-option label="待接受" :value="0" />
          <el-option label="已接受" :value="1" />
          <el-option label="处理中" :value="5" />
          <el-option label="已完成" :value="6" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button size="small" @click="fetchTasks" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="tasks-table-wrapper">
      <el-table :data="filteredTasks" style="width: 100%" :header-cell-style="{ background: 'rgba(0,212,255,0.05)', color: '#e0e6ed' }"
        :row-style="{ background: 'transparent', color: '#c0c6cd' }" :cell-style="{ borderColor: 'rgba(255,255,255,0.04)' }">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="事件摘要" min-width="200">
          <template #default="{ row }">
            <span class="event-summary">{{ row.summary || row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="灾害类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="disasterTagType(row.disasterType)">
              {{ row.disasterType || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="严重等级" width="100">
          <template #default="{ row }">
            <span class="severity" :class="'level-' + row.severity">
              {{ getSeverityText(row.severity) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="dark" size="small" round>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="反馈" min-width="150">
          <template #default="{ row }">
            <span v-if="row.feedback" class="feedback-text">{{ row.feedback }}</span>
            <span v-else class="no-feedback">暂无反馈</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 任务统计 -->
    <div class="task-stats">
      <div class="stat-box">
        <span class="stat-num cyan">{{ tasks.length }}</span>
        <span class="stat-desc">总任务数</span>
      </div>
      <div class="stat-box">
        <span class="stat-num green">{{ tasks.filter(t => t.status === 6).length }}</span>
        <span class="stat-desc">已完成</span>
      </div>
      <div class="stat-box">
        <span class="stat-num orange">{{ tasks.filter(t => t.status >= 1 && t.status <= 5).length }}</span>
        <span class="stat-desc">进行中</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { responderPortalApi } from '../../api/responderPortal'
import { wsClient, MessageType } from '../../api/websocket'

const loading = ref(false)
const tasks = ref([])
const statusFilter = ref('')

const filteredTasks = computed(() => {
  if (statusFilter.value === '' || statusFilter.value === null) return tasks.value
  return tasks.value.filter(t => t.status === statusFilter.value)
})

async function fetchTasks() {
  loading.value = true
  try {
    const res = await responderPortalApi.getDispatchTasks()
    tasks.value = res.code === 1 ? (res.data || []) : []
  } catch { tasks.value = [] }
  finally { loading.value = false }
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}
function getStatusType(s) {
  return { 0: 'info', 1: 'primary', 2: 'danger', 3: 'warning', 4: 'warning', 5: 'warning', 6: 'success', 7: 'danger', 8: 'info' }[s] || 'info'
}
function getStatusText(s) {
  return { 0: '待接受', 1: '已接受', 2: '已拒绝', 3: '已出发', 4: '已到达', 5: '处理中', 6: '已完成', 7: '需增援', 8: '待审批' }[s] || '未知'
}
function getSeverityText(s) {
  return ['轻微', '一般', '严重', '特别严重'][s] || '未知'
}
function disasterTagType(type) {
  const map = { '地震': 'danger', '洪水': 'primary', '台风': 'warning', '火灾': 'danger', '泥石流': 'warning' }
  return map[type] || 'info'
}

const handleTaskUpdate = () => {
  fetchTasks()
}

onMounted(() => {
  fetchTasks()
  wsClient.on(MessageType.TASK_UPDATE, handleTaskUpdate)
})

onUnmounted(() => {
  wsClient.off(MessageType.TASK_UPDATE, handleTaskUpdate)
})
</script>

<style scoped>
.responder-tasks {
  max-width: 1400px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
}

.header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00d4ff;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tasks-table-wrapper {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.tasks-table-wrapper :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-border-color: rgba(255, 255, 255, 0.04);
  --el-table-text-color: #c0c6cd;
  --el-table-header-text-color: #e0e6ed;
}

.event-summary {
  color: #e0e6ed;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.severity {
  font-weight: 600;
  font-size: 13px;
}
.severity.level-0 { color: #67c23a; }
.severity.level-1 { color: #e6a23c; }
.severity.level-2 { color: #f56c6c; }
.severity.level-3 { color: #ff4444; }

.feedback-text {
  color: rgba(224, 230, 237, 0.7);
  font-size: 13px;
}
.no-feedback {
  color: rgba(224, 230, 237, 0.3);
  font-size: 13px;
}
.time-text {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.5);
}

.task-stats {
  display: flex;
  gap: 16px;
}

.stat-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
}
.stat-num.cyan { color: #00d4ff; }
.stat-num.green { color: #00ff88; }
.stat-num.orange { color: #ffaa00; }

.stat-desc {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.5);
  margin-top: 4px;
}
</style>
