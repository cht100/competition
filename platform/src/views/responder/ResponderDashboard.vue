<template>
  <div class="responder-dashboard">
    <!-- 状态概览卡片 -->
    <div class="overview-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.gradient }">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧：当前任务 -->
      <div class="current-task-section">
        <div class="section-header">
          <h3>
            <span class="header-dot"></span>
            当前执行任务
          </h3>
        </div>
        <div v-if="currentTask" class="task-detail-card">
          <div class="task-status-bar">
            <span class="task-id">#{{ currentTask.id }}</span>
            <el-tag :type="getStatusType(currentTask.status)" effect="dark" round>
              {{ getStatusText(currentTask.status) }}
            </el-tag>
          </div>
          <h4 class="task-title">{{ currentTask.summary || currentTask.description }}</h4>
          <div class="task-meta">
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>{{ currentTask.locationText || '未知位置' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Timer /></el-icon>
              <span>{{ formatTime(currentTask.createTime) }}</span>
            </div>
            <div class="meta-item" v-if="currentTask.disasterType">
              <el-icon><Warning /></el-icon>
              <span>{{ currentTask.disasterType }}</span>
            </div>
          </div>
          <div class="task-actions">
            <el-button type="primary" @click="handleFeedback" :loading="submitting">
              <el-icon><Check /></el-icon>
              完成任务并提交反馈
            </el-button>
          </div>
        </div>
        <div v-else class="empty-task">
          <div class="empty-icon">✓</div>
          <p>当前没有执行中的任务</p>
          <span>等待管理员派单或从可接受列表中领取任务</span>
        </div>
      </div>

      <!-- 右侧：待接受任务 -->
      <div class="pending-section">
        <div class="section-header">
          <h3>
            <span class="header-dot green"></span>
            可接受的事件
          </h3>
          <el-button size="small" @click="fetchAvailableTasks" :loading="loading">刷新</el-button>
        </div>
        <div class="task-list">
          <div v-for="task in availableTasks" :key="task.id" class="pending-card" @click="showTaskDetail(task)">
            <div class="pending-header">
              <el-tag size="small" :type="getSeverityType(task.severity)" effect="dark">
                {{ getSeverityText(task.severity) }}
              </el-tag>
              <span class="pending-type">{{ task.disasterType }}</span>
            </div>
            <p class="pending-desc">{{ task.summary }}</p>
            <div class="pending-footer">
              <span class="pending-location">
                <el-icon><Location /></el-icon>
                {{ task.locationText || '位置未知' }}
              </span>
              <el-button type="primary" size="small" plain @click.stop="acceptTask(task)">
                接受任务
              </el-button>
            </div>
          </div>
          <div v-if="availableTasks.length === 0" class="empty-list">
            暂无可接受的事件
          </div>
        </div>

        <!-- 待响应的派单 -->
        <div class="section-header" style="margin-top: 20px">
          <h3>
            <span class="header-dot orange"></span>
            待响应派单
          </h3>
        </div>
        <div class="task-list">
          <div v-for="task in pendingDispatches" :key="task.id" class="dispatch-card">
            <div class="dispatch-info">
              <span class="dispatch-desc">{{ task.description }}</span>
              <span class="dispatch-time">{{ formatTime(task.createTime) }}</span>
            </div>
            <div class="dispatch-actions">
              <el-button type="success" size="small" @click="respondDispatch(task.id, true)">接受</el-button>
              <el-button type="danger" size="small" plain @click="respondDispatch(task.id, false)">拒绝</el-button>
            </div>
          </div>
          <div v-if="pendingDispatches.length === 0" class="empty-list">
            暂无待响应派单
          </div>
        </div>
      </div>
    </div>

    <!-- 反馈弹窗 -->
    <el-dialog v-model="showFeedbackDialog" title="提交处理反馈" width="500px" class="dark-dialog">
      <el-form :model="feedbackForm" label-position="top">
        <el-form-item label="处理结果">
          <el-input v-model="feedbackForm.feedback" type="textarea" :rows="4" placeholder="请详细描述处理结果..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFeedbackDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback" :loading="submitting">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Timer, Warning, Check, Document, Finished, Bell, User } from '@element-plus/icons-vue'
import { responderPortalApi } from '../../api/responderPortal'
import { wsClient, MessageType } from '../../api/websocket'

const loading = ref(false)
const submitting = ref(false)
const currentTask = ref(null)
const availableTasks = ref([])
const pendingDispatches = ref([])
const showFeedbackDialog = ref(false)
const feedbackForm = ref({ feedback: '' })

const statCards = computed(() => [
  { label: '当前任务', value: currentTask.value ? '执行中' : '空闲', icon: Document, gradient: 'linear-gradient(135deg, #00d4ff, #0088cc)' },
  { label: '可接受事件', value: availableTasks.value.length, icon: Bell, gradient: 'linear-gradient(135deg, #00ff88, #00cc6a)' },
  { label: '待响应派单', value: pendingDispatches.value.length, icon: Finished, gradient: 'linear-gradient(135deg, #ffaa00, #ff8800)' },
  { label: '在线状态', value: '执勤中', icon: User, gradient: 'linear-gradient(135deg, #7c3aed, #5b21b6)' }
])

async function fetchCurrentTask() {
  try {
    const res = await responderPortalApi.getCurrentTask()
    currentTask.value = res.code === 1 ? res.data : null
  } catch { currentTask.value = null }
}

async function fetchAvailableTasks() {
  loading.value = true
  try {
    const res = await responderPortalApi.getAvailableTasks()
    availableTasks.value = res.code === 1 ? (res.data || []) : []
  } catch { availableTasks.value = [] }
  finally { loading.value = false }
}

async function fetchPendingDispatches() {
  try {
    const res = await responderPortalApi.getPendingDispatchTasks()
    pendingDispatches.value = res.code === 1 ? (res.data || []) : []
  } catch { pendingDispatches.value = [] }
}

async function acceptTask(task) {
  try {
    await ElMessageBox.confirm(`确认接受事件 #${task.id}？`, '接受任务')
    const res = await responderPortalApi.acceptTask({ incidentId: task.id })
    if (res.code === 1) {
      // 检查返回消息是否包含"审批"关键词，说明是志愿者申请
      const msg = res.data || res.msg || ''
      if (typeof msg === 'string' && msg.includes('审批')) {
        ElMessage.success('申请已提交，等待管理员审批')
      } else {
        ElMessage.success('任务已接受')
      }
      fetchCurrentTask()
      fetchAvailableTasks()
    } else {
      ElMessage.error(res.msg || '接受失败')
    }
  } catch {}
}

async function respondDispatch(taskId, accept) {
  try {
    const res = await responderPortalApi.respondDispatch({ dispatchTaskId: taskId, accept })
    if (res.code === 1) {
      ElMessage.success(accept ? '已接受派单' : '已拒绝派单')
      fetchPendingDispatches()
      fetchCurrentTask()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {}
}

function handleFeedback() {
  feedbackForm.value.feedback = ''
  showFeedbackDialog.value = true
}

async function submitFeedback() {
  if (!feedbackForm.value.feedback.trim()) {
    ElMessage.warning('请填写处理反馈')
    return
  }
  submitting.value = true
  try {
    const res = await responderPortalApi.submitFeedback({
      incidentId: currentTask.value.id,
      feedback: feedbackForm.value.feedback
    })
    if (res.code === 1) {
      ElMessage.success('反馈提交成功')
      showFeedbackDialog.value = false
      fetchCurrentTask()
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch {} finally { submitting.value = false }
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function getStatusType(s) {
  const map = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'warning', 4: 'success', 5: '' }
  return map[s] || 'info'
}
function getStatusText(s) {
  const map = { 0: '待审核', 1: '已确认', 2: '已派单', 3: '处理中', 4: '已完成', 5: '已驳回' }
  return map[s] || '未知'
}
function getSeverityType(s) {
  return [, 'info', 'warning', 'danger'][s] || 'info'
}
function getSeverityText(s) {
  return ['轻微', '一般', '严重', '特别严重'][s] || '未知'
}
function showTaskDetail(task) {
  const esc = (s) => String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;')
  ElMessageBox.alert(
    `<div><strong>灾害类型:</strong> ${esc(task.disasterType)}<br/><strong>严重程度:</strong> ${esc(getSeverityText(task.severity))}<br/><strong>位置:</strong> ${esc(task.locationText || '未知')}<br/><strong>摘要:</strong> ${esc(task.summary)}</div>`,
    '事件详情',
    { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
  )
}

const handleTaskUpdate = () => {
  // 任务状态变化时刷新所有列表
  fetchCurrentTask()
  fetchPendingDispatches()
  fetchAvailableTasks()
}

const handleIncidentUpdate = () => {
  // 事件状态变化时刷新可接受列表
  fetchAvailableTasks()
  fetchCurrentTask()
}

const handleResponderUpdate = () => {
  fetchCurrentTask()
}

onMounted(() => {
  fetchCurrentTask()
  fetchAvailableTasks()
  fetchPendingDispatches()

  wsClient.on(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.on(MessageType.RESPONDER_UPDATE, handleResponderUpdate)
})

onUnmounted(() => {
  wsClient.off(MessageType.TASK_UPDATE, handleTaskUpdate)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleIncidentUpdate)
  wsClient.off(MessageType.RESPONDER_UPDATE, handleResponderUpdate)
})
</script>

<style scoped>
.responder-dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.5);
  margin-top: 2px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
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
.header-dot.green { background: #00ff88; }
.header-dot.orange { background: #ffaa00; }

.task-detail-card {
  padding: 24px;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 12px;
}

.task-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.task-id {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.5);
}

.task-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
  line-height: 1.5;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(224, 230, 237, 0.7);
}

.task-actions {
  display: flex;
  gap: 12px;
}

.empty-task {
  text-align: center;
  padding: 60px 20px;
  color: rgba(224, 230, 237, 0.4);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #00ff88;
}

.empty-task p {
  font-size: 16px;
  margin-bottom: 8px;
  color: rgba(224, 230, 237, 0.6);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.pending-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.pending-card:hover {
  background: rgba(0, 212, 255, 0.05);
  border-color: rgba(0, 212, 255, 0.2);
}

.pending-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.pending-type {
  font-size: 13px;
  color: rgba(224, 230, 237, 0.5);
}

.pending-desc {
  font-size: 14px;
  color: #e0e6ed;
  line-height: 1.5;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pending-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(224, 230, 237, 0.4);
}

.dispatch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: rgba(255, 170, 0, 0.05);
  border: 1px solid rgba(255, 170, 0, 0.15);
  border-radius: 10px;
}

.dispatch-desc {
  font-size: 14px;
  color: #e0e6ed;
}

.dispatch-time {
  display: block;
  font-size: 12px;
  color: rgba(224, 230, 237, 0.4);
  margin-top: 4px;
}

.dispatch-actions {
  display: flex;
  gap: 8px;
}

.empty-list {
  text-align: center;
  padding: 30px;
  color: rgba(224, 230, 237, 0.3);
  font-size: 14px;
}

.dark-dialog {
  --el-dialog-bg-color: #1a1e3e;
  --el-dialog-border-color: rgba(0, 212, 255, 0.15);
}
</style>
