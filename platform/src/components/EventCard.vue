<template>
  <div 
    class="event-card"
    :class="[`severity-${incident.severity}`, `status-${incident.status}`]"
    @click="handleClick"
  >
    <div class="card-header">
      <div class="disaster-type" :style="{ borderColor: typeColor }">
        <el-icon :style="{ color: typeColor }">
          <component :is="typeIcon" />
        </el-icon>
        <span :style="{ color: typeColor }">{{ incident.disasterType || '未知' }}</span>
      </div>
      <div class="status-tag" :class="`status-${incident.status}`">
        {{ statusText }}
      </div>
    </div>
    
    <div class="card-body">
      <h4 class="event-title">{{ incident.summary || '无摘要' }}</h4>
      <p class="event-summary">{{ incident.summary || '' }}</p>
    </div>
    
    <div class="card-info">
      <div class="info-row">
        <el-icon><Location /></el-icon>
        <span class="info-text">{{ incident.locationText || '未知位置' }}</span>
      </div>
      <div class="info-row">
        <el-icon><Clock /></el-icon>
        <span class="info-text">{{ formattedTime }}</span>
      </div>
    </div>
    
    <div class="card-footer">
      <div class="meta-info">
        <div class="confidence">
          <span class="label">置信度</span>
          <el-progress 
            :percentage="confidencePercent" 
            :stroke-width="4"
            :color="confidenceColor"
            :show-text="false"
          />
          <span class="value">{{ confidencePercent }}%</span>
        </div>
      </div>
      
      <div class="severity-tag" :class="`severity-${incident.severity}`">
        {{ severityText }}
      </div>
    </div>
    
    <div class="hover-indicator"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  incident: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

const typeIconMap = {
  '洪涝': 'Cloudy',
  '火灾': 'Sunny',
  '地震': 'TrendCharts',
  '台风': 'WindPower',
  '泥石流': 'Mountain',
  '交通事故': 'Van',
  '燃气泄漏': 'Warning',
  '其他': 'Warning'
}

const typeColorMap = {
  '洪涝': '#00d4ff',
  '火灾': '#ff3366',
  '地震': '#ffaa00',
  '台风': '#00ff88',
  '泥石流': '#9966ff',
  '交通事故': '#ff6b6b',
  '燃气泄漏': '#ffd93d',
  '其他': '#666666'
}

const statusTextMap = {
  0: '待审核',
  1: '已确认',
  2: '已派单',
  3: '处理中',
  4: '已完成',
  5: '已驳回'
}

const severityTextMap = {
  0: '轻微',
  1: '一般',
  2: '严重',
  3: '特别严重'
}

const typeIcon = computed(() => typeIconMap[props.incident.disasterType] || 'Warning')
const typeColor = computed(() => typeColorMap[props.incident.disasterType] || '#666666')
const statusText = computed(() => statusTextMap[props.incident.status] || '未知')
const severityText = computed(() => severityTextMap[props.incident.severity] || '未知')

const confidencePercent = computed(() => {
  return props.incident.confidence || 0
})

const confidenceColor = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return '#00ff88'
  if (percent >= 60) return '#00d4ff'
  if (percent >= 40) return '#ffaa00'
  return '#ff3366'
})

const formattedTime = computed(() => {
  if (!props.incident.createTime) return ''
  
  const date = new Date(props.incident.createTime)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return `${minutes}分钟前`
  }
  
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }
  
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const handleClick = () => {
  emit('click', props.incident)
}
</script>

<style scoped>
.event-card {
  position: relative;
  padding: 14px;
  margin-bottom: 10px;
  background: rgba(10, 20, 40, 0.6);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  overflow: hidden;
}

.event-card:hover {
  background: rgba(0, 212, 255, 0.05);
  border-color: var(--primary-color);
  transform: translateX(4px);
}

.event-card:hover .hover-indicator {
  opacity: 1;
}

.event-card.severity-3 {
  border-left: 3px solid #ff3366;
}

.event-card.severity-2 {
  border-left: 3px solid #ffaa00;
}

.event-card.severity-1 {
  border-left: 3px solid #00d4ff;
}

.event-card.severity-0 {
  border-left: 3px solid #00ff88;
}

.hover-indicator {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--primary-color);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.disaster-type {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.disaster-type .el-icon {
  font-size: 14px;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.status-tag.status-0 {
  background: rgba(255, 170, 0, 0.2);
  color: #ffaa00;
}

.status-tag.status-1 {
  background: rgba(0, 212, 255, 0.2);
  color: #00d4ff;
}

.status-tag.status-2 {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.status-tag.status-3 {
  background: rgba(102, 204, 255, 0.2);
  color: #66ccff;
}

.status-tag.status-4 {
  background: rgba(102, 102, 102, 0.2);
  color: #999;
}

.status-tag.status-5 {
  background: rgba(255, 51, 102, 0.2);
  color: #ff3366;
}

.card-body {
  margin-bottom: 10px;
}

.event-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-summary {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  margin-bottom: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row .el-icon {
  font-size: 12px;
  color: var(--text-muted);
}

.info-text {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.confidence {
  display: flex;
  align-items: center;
  gap: 6px;
}

.confidence .label {
  font-size: 10px;
  color: var(--text-muted);
}

.confidence .el-progress {
  width: 50px;
}

.confidence .value {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-primary);
}

.severity-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 500;
}

.severity-tag.severity-3 {
  background: rgba(255, 51, 102, 0.2);
  color: #ff3366;
}

.severity-tag.severity-2 {
  background: rgba(255, 170, 0, 0.2);
  color: #ffaa00;
}

.severity-tag.severity-1 {
  background: rgba(0, 212, 255, 0.2);
  color: #00d4ff;
}

.severity-tag.severity-0 {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

:deep(.el-progress-bar__outer) {
  background: rgba(0, 0, 0, 0.3);
}

:deep(.el-progress-bar__inner) {
  border-radius: 2px;
}
</style>
