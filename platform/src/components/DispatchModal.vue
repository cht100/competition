<template>
  <el-dialog
    v-model="dialogVisible"
    title="派单处理"
    width="550px"
    :close-on-click-modal="false"
    class="dispatch-modal"
  >
    <div class="dispatch-form">
      <div class="incident-info" v-if="incident">
        <div class="info-header">
          <el-icon><Warning /></el-icon>
          <span>事件信息</span>
        </div>
        <div class="info-content">
          <div class="info-row">
            <span class="label">事件ID:</span>
            <span class="value">#{{ incident.id }}</span>
          </div>
          <div class="info-row">
            <span class="label">灾害类型:</span>
            <el-tag size="small" type="warning">{{ incident.disasterType || '未知' }}</el-tag>
          </div>
          <div class="info-row">
            <span class="label">严重程度:</span>
            <el-tag size="small" :type="getSeverityType(incident.severity)">
              {{ getSeverityText(incident.severity) }}
            </el-tag>
          </div>
          <div class="info-row full">
            <span class="label">事件摘要:</span>
            <span class="value summary">{{ incident.summary }}</span>
          </div>
        </div>
      </div>

      <div class="responder-section">
        <div class="section-header">
          <span>选择执勤人员</span>
          <span class="available-count">
            可用: {{ availableCount }} / {{ responders.length }}
          </span>
        </div>
        
        <div class="responder-list" v-loading="loading">
          <div
            v-for="responder in responders"
            :key="responder.id"
            class="responder-item"
            :class="{ 
              selected: formData.responderId === responder.id,
              disabled: responder.status !== 1
            }"
            @click="selectResponder(responder)"
          >
            <div class="responder-avatar">
              <el-avatar :size="40">{{ responder.name?.charAt(0) || '?' }}</el-avatar>
              <span class="status-dot" :class="getStatusClass(responder.status)"></span>
            </div>
            <div class="responder-info">
              <div class="responder-name">{{ responder.name }}</div>
              <div class="responder-position">{{ responder.position }}</div>
              <div class="responder-phone" v-if="responder.phone">{{ responder.phone }}</div>
            </div>
            <div class="responder-status">
              <el-tag 
                :type="getStatusTagType(responder.status)" 
                size="small"
              >
                {{ getStatusText(responder.status) }}
              </el-tag>
            </div>
          </div>
          
          <el-empty v-if="responders.length === 0" description="暂无执勤人员" />
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="form-content"
      >
        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="formData.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item label="注意事项" prop="notes">
          <el-input
            v-model="formData.notes"
            type="textarea"
            :rows="2"
            placeholder="请输入注意事项（选填）"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleSubmit" 
          :loading="submitting"
          :disabled="!formData.responderId"
        >
          确认派单
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  incident: {
    type: Object,
    default: null
  },
  responders: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const availableCount = computed(() => {
  return props.responders.filter(r => r.status === 1).length
})

const formData = ref({
  responderId: null,
  description: '',
  deadline: null,
  notes: ''
})

const rules = {
  description: [
    { required: true, message: '请输入任务描述', trigger: 'blur' },
    { min: 5, message: '任务描述至少5个字符', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' }
  ]
}

const getSeverityType = (severity) => {
  const types = ['info', 'warning', 'danger', 'danger']
  return types[severity] || 'info'
}

const getSeverityText = (severity) => {
  const texts = ['轻微', '一般', '严重', '特别严重']
  return texts[severity] || '未知'
}

const getStatusClass = (status) => {
  const classes = {
    0: 'offline',
    1: 'idle',
    2: 'busy',
    3: 'busy'
  }
  return classes[status] || 'offline'
}

const getStatusTagType = (status) => {
  const types = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '离线',
    1: '空闲',
    2: '已派单',
    3: '执行中'
  }
  return texts[status] || '未知'
}

const disabledDate = (date) => {
  return date.getTime() < Date.now() - 86400000
}

const selectResponder = (responder) => {
  if (responder.status !== 1) {
    ElMessage.warning('该人员当前不可用')
    return
  }
  formData.value.responderId = responder.id
}

const resetForm = () => {
  formData.value = {
    responderId: null,
    description: '',
    deadline: null,
    notes: ''
  }
  formRef.value?.resetFields()
}

const handleCancel = () => {
  resetForm()
  dialogVisible.value = false
}

const handleSubmit = async () => {
  if (!formData.value.responderId) {
    ElMessage.warning('请选择执勤人员')
    return
  }

  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    const submitData = {
      responderId: formData.value.responderId,
      description: formData.value.description,
      deadline: formData.value.deadline,
      notes: formData.value.notes
    }

    emit('submit', submitData)
    resetForm()
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val && props.incident) {
    formData.value.description = props.incident.summary || ''
  }
  if (!val) {
    resetForm()
  }
})
</script>

<style scoped>
.dispatch-modal :deep(.el-dialog) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.dispatch-modal :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-color);
  padding: 16px 20px;
}

.dispatch-modal :deep(.el-dialog__title) {
  color: var(--primary-color);
  font-weight: 600;
}

.dispatch-modal :deep(.el-dialog__body) {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.dispatch-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.incident-info {
  background: rgba(255, 170, 0, 0.1);
  border: 1px solid rgba(255, 170, 0, 0.3);
  border-radius: var(--radius-md);
  padding: 12px;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ffaa00;
  font-weight: 600;
  margin-bottom: 12px;
}

.info-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-row.full {
  grid-column: span 2;
}

.info-row .label {
  color: var(--text-muted);
  font-size: 13px;
}

.info-row .value {
  color: var(--text-primary);
  font-size: 13px;
}

.info-row .value.summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.responder-section {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(0, 51, 102, 0.3);
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
}

.available-count {
  font-size: 12px;
  color: var(--success-color);
  font-weight: normal;
}

.responder-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
}

.responder-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  margin-bottom: 8px;
  background: rgba(0, 20, 40, 0.5);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.responder-item:hover:not(.disabled) {
  border-color: var(--primary-color);
  background: rgba(0, 212, 255, 0.1);
}

.responder-item.selected {
  border-color: var(--success-color);
  background: rgba(0, 255, 136, 0.1);
}

.responder-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.responder-avatar {
  position: relative;
}

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--bg-card);
}

.status-dot.idle {
  background: var(--success-color);
  box-shadow: 0 0 6px var(--success-color);
}

.status-dot.busy {
  background: var(--warning-color);
}

.status-dot.offline {
  background: var(--text-muted);
}

.responder-info {
  flex: 1;
}

.responder-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.responder-position {
  font-size: 12px;
  color: var(--text-muted);
}

.responder-phone {
  font-size: 11px;
  color: var(--text-muted);
}

.form-content {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: var(--text-primary);
  background: rgba(0, 20, 40, 0.5);
}
</style>
