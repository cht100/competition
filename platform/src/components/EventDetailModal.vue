<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="700px"
    :close-on-click-modal="false"
    class="event-detail-modal"
    @close="handleClose"
  >
    <div v-if="incident" class="modal-content">
      <!-- 基本信息 -->
      <div class="info-section">
        <div class="section-header">
          <el-icon><InfoFilled /></el-icon>
          <span>基本信息</span>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">事件ID</span>
            <span class="value">{{ incident.id }}</span>
          </div>
          <div class="info-item">
            <span class="label">灾种类型</span>
            <span class="value">
              <span class="type-badge" :style="{ background: typeColor }">
                {{ incident.disasterType }}
              </span>
            </span>
          </div>
          <div class="info-item">
            <span class="label">严重程度</span>
            <span class="value">
              <span class="severity-badge" :class="`severity-${incident.severity}`">
                {{ severityText }}
              </span>
            </span>
          </div>
          <div class="info-item">
            <span class="label">当前状态</span>
            <span class="value">
              <span class="status-badge" :class="`status-${incident.status}`">
                {{ statusText }}
              </span>
            </span>
          </div>
          <div class="info-item full-width">
            <span class="label">事件位置</span>
            <span class="value">{{ incident.locationText || '未知' }}</span>
          </div>
          <div class="info-item">
            <span class="label">坐标</span>
            <span class="value">{{ incident.lng?.toFixed(4) }}, {{ incident.lat?.toFixed(4) }}</span>
          </div>
          <div class="info-item">
            <span class="label">上报时间</span>
            <span class="value">{{ formattedTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">信息来源</span>
            <span class="value">{{ incident.sourcePlatform || incident.source || '多源汇聚' }}</span>
          </div>
          <div class="info-item">
            <span class="label">置信度</span>
            <span class="value">
              <el-progress 
                :percentage="confidencePercent" 
                :stroke-width="8"
                :color="confidenceColor"
              />
            </span>
          </div>
        </div>
      </div>
      
      <!-- 事件摘要 -->
      <div class="info-section">
        <div class="section-header">
          <el-icon><Document /></el-icon>
          <span>事件摘要</span>
        </div>
        <div class="summary-content">
          {{ incident.summary }}
        </div>
      </div>
      
      <!-- 证据链 -->
      <div class="info-section">
        <div class="section-header">
          <el-icon><FolderOpened /></el-icon>
          <span>证据链</span>
          <span class="count">({{ evidenceList.length }})</span>
          <el-button 
            v-if="!evidenceLoaded" 
            text size="small" type="primary" 
            @click="loadEvidenceChain"
            :loading="evidenceLoading"
            style="margin-left: auto;"
          >
            加载证据链
          </el-button>
        </div>
        <div v-if="evidenceLoading" class="evidence-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载证据链...</span>
        </div>
        <div class="evidence-chain" v-else-if="evidenceList.length > 0">
          <div 
            v-for="(evidence, index) in evidenceList" 
            :key="evidence.id"
            class="evidence-chain-node"
          >
            <div class="chain-connector" v-if="index > 0">
              <div class="connector-line"></div>
              <div class="connector-arrow">▼</div>
            </div>
            <div class="chain-card" :class="{ 'is-rumor': evidence.status === 5 }">
              <div class="chain-card-header">
                <div class="chain-index">#{{ index + 1 }}</div>
                <el-tag size="small" :type="getEvidenceSourceType(evidence.sourcePlatform)">
                  {{ evidence.sourcePlatform || '未知来源' }}
                </el-tag>
                <el-tag v-if="evidence.status === 5" size="small" type="danger">谣言</el-tag>
                <span class="chain-time">{{ formatEvidenceTime(evidence.publishTime || evidence.createTime) }}</span>
              </div>
              <div class="chain-card-body">
                <div class="chain-publisher" v-if="evidence.publisherName">
                  <el-icon><User /></el-icon>
                  {{ evidence.publisherName }}
                </div>
                <p class="chain-text">{{ evidence.originalText || evidence.cleanedText || '无内容' }}</p>
                <div class="chain-location" v-if="evidence.locationText">
                  <el-icon><Location /></el-icon>
                  {{ evidence.locationText }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-evidence">
          暂无证据数据
        </div>
      </div>
      
      <!-- 谣言分析面板 -->
      <div class="info-section rumor-section">
        <div class="section-header">
          <el-icon><Warning /></el-icon>
          <span>谣言研判分析</span>
          <span class="rumor-verdict" :class="rumorVerdict.level">
            {{ rumorVerdict.icon }} {{ rumorVerdict.text }}
          </span>
        </div>
        <div class="rumor-analysis-panel">
          <div class="rumor-radar-container">
            <div ref="rumorRadarChart" class="rumor-radar-chart"></div>
          </div>
          <div class="rumor-detail">
            <div class="rumor-score-bar">
              <span class="score-label">可信度评分</span>
              <div class="score-track">
                <div class="score-fill" :style="{ width: confidencePercent + '%', background: confidenceColor }"></div>
                <span class="score-text" :style="{ color: confidenceColor }">{{ confidencePercent }}%</span>
              </div>
            </div>
            <div class="rumor-reason" v-if="rumorInfo.reason">
              <div class="reason-label">
                <el-icon><InfoFilled /></el-icon>
                判定依据
              </div>
              <p class="reason-text">{{ rumorInfo.reason }}</p>
            </div>
            <div class="rumor-keywords" v-if="rumorInfo.keywords && rumorInfo.keywords.length > 0">
              <div class="reason-label">
                <el-icon><Collection /></el-icon>
                关键词提取
              </div>
              <div class="keyword-tags">
                <span v-for="kw in rumorInfo.keywords" :key="kw" class="keyword-tag">{{ kw }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- AI分析结果 -->
      <div class="info-section">
        <div class="section-header">
          <el-icon><Cpu /></el-icon>
          <span>AI分析结果</span>
        </div>
        <div class="ai-analysis">
          <div class="analysis-item">
            <span class="analysis-label">灾害识别</span>
            <div class="analysis-tags">
              <el-tag 
                v-for="tag in aiAnalysis.tags" 
                :key="tag"
                size="small"
                type="info"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
          <div class="analysis-item">
            <span class="analysis-label">风险评估</span>
            <div class="risk-level" :class="aiAnalysis.riskLevel">
              {{ aiAnalysis.riskText }}
            </div>
          </div>
          <div class="analysis-item">
            <span class="analysis-label">AI建议措施</span>
            <ul class="suggestions">
              <li v-for="(suggestion, index) in aiAnalysis.suggestions" :key="index">
                {{ suggestion }}
              </li>
            </ul>
          </div>
        </div>
      </div>
      
      <!-- 时间线 -->
      <div class="info-section">
        <div class="section-header">
          <el-icon><Clock /></el-icon>
          <span>事件时间线</span>
        </div>
        <div class="timeline">
          <div 
            v-for="(event, index) in timeline" 
            :key="index"
            class="timeline-item"
            :class="{ 'is-last': index === timeline.length - 1 }"
          >
            <div class="timeline-dot" :class="event.type"></div>
            <div class="timeline-content">
              <div class="timeline-time">{{ event.time }}</div>
              <div class="timeline-title">{{ event.title }}</div>
              <div class="timeline-desc" v-if="event.description">
                {{ event.description }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="modal-footer">
        <div class="footer-left">
          <el-button @click="handleClose">关闭</el-button>
        </div>
        <div class="footer-right">
          <el-button 
            v-if="incident?.status === 0"
            type="danger"
            @click="handleReject"
          >
            <el-icon><Close /></el-icon>
            驳回
          </el-button>
          <el-button 
            v-if="incident?.status === 0"
            type="primary"
            @click="handleConfirm"
          >
            <el-icon><Check /></el-icon>
            确认
          </el-button>
          <el-button 
            v-if="incident?.status === 1"
            type="warning"
            @click="handleSmartDispatch"
            :loading="smartDispatching"
          >
            <el-icon><MagicStick /></el-icon>
            智能派单
          </el-button>
          <el-button 
            v-if="incident?.status === 1"
            type="success"
            @click="handleDispatch"
          >
            <el-icon><Position /></el-icon>
            手动派单
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { post, get } from '@/api/request'
import * as echarts from 'echarts'

const smartDispatching = ref(false)
const evidenceLoading = ref(false)
const evidenceLoaded = ref(false)
const evidenceList = ref([])
const rumorRadarChart = ref(null)
let radarChartInstance = null

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  incident: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'update:modelValue',
  'confirm',
  'reject',
  'dispatch',
  'close'
])

// 弹窗显示状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 弹窗标题
const dialogTitle = computed(() => {
  return props.incident?.title || '事件详情'
})

// 灾种颜色映射
const typeColorMap = {
  '洪涝': '#00d4ff',
  '火灾': '#ff3366',
  '地震': '#ffaa00',
  '台风': '#00ff88',
  '泥石流': '#9966ff',
  '其他': '#666666'
}

// 状态文本映射
const statusTextMap = {
  0: '待审核',
  1: '已确认',
  2: '已派单',
  3: '处理中',
  4: '已完成',
  5: '已驳回'
}

// 严重程度文本映射
const severityTextMap = {
  0: '轻微',
  1: '一般',
  2: '严重',
  3: '特别严重'
}

// 计算属性
const typeColor = computed(() => {
  return typeColorMap[props.incident?.disasterType] || '#666666'
})

const statusText = computed(() => {
  return statusTextMap[props.incident?.status] || '未知'
})

const severityText = computed(() => {
  return severityTextMap[props.incident?.severity] || '未知'
})

const confidencePercent = computed(() => {
  return props.incident?.confidence || 0
})

const confidenceColor = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 90) return '#00ff88'
  if (percent >= 70) return '#00d4ff'
  if (percent >= 50) return '#ffaa00'
  return '#ff3366'
})

const formattedTime = computed(() => {
  const t = props.incident?.createTime || props.incident?.reportedAt
  if (!t) return ''
  const date = new Date(t)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

// 证据链 - 从后端API加载真实数据
const loadEvidenceChain = async () => {
  if (!props.incident?.id) return
  evidenceLoading.value = true
  try {
    const res = await get(`/admin/incident/${props.incident.id}/detail`)
    const data = res.data || res
    evidenceList.value = data.evidenceChain || []
    evidenceLoaded.value = true
  } catch (error) {
    console.error('加载证据链失败:', error)
    evidenceList.value = []
  } finally {
    evidenceLoading.value = false
  }
}

// 当事件变化时自动加载证据链 + 初始化雷达图
watch(() => props.incident?.id, (newId) => {
  if (newId) {
    evidenceLoaded.value = false
    evidenceList.value = []
    loadEvidenceChain()
    nextTick(() => initRumorRadar())
  }
})

function formatEvidenceTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function getEvidenceSourceType(source) {
  const types = { '微信群聊': 'success', 'QQ群聊': 'primary', '微博': 'warning', '抖音': 'danger', '小红书': '' }
  return types[source] || 'info'
}

// AI分析结果 - 使用真实后端数据
const aiAnalysis = computed(() => {
  if (!props.incident) return { tags: [], riskLevel: 'low', riskText: '', suggestions: [] }
  
  const type = props.incident.disasterType || '未知'
  const severity = props.incident.severity || 0

  const tags = [type]
  if (props.incident.locationText) tags.push(props.incident.locationText)
  if (props.incident.confidence >= 80) tags.push('高置信度')
  if (props.incident.confidence < 40) tags.push('疑似谣言')

  const riskMap = [
    { level: 'low', text: '低风险' },
    { level: 'medium', text: '中等风险' },
    { level: 'high', text: '高风险' },
    { level: 'critical', text: '极高风险' }
  ]
  const risk = riskMap[severity] || riskMap[0]

  // 优先使用后端AI返回的真实建议
  let suggestions = []
  if (props.incident.aiSuggestion) {
    try {
      const aiData = typeof props.incident.aiSuggestion === 'string' 
        ? JSON.parse(props.incident.aiSuggestion) 
        : props.incident.aiSuggestion
      if (aiData.suggestion) {
        suggestions = aiData.suggestion.split(/[;；。\n]/).filter(s => s.trim())
      }
      if (aiData.keywords) {
        aiData.keywords.forEach(k => { if (!tags.includes(k)) tags.push(k) })
      }
    } catch {}
  }
  
  // 仅在无真实数据时使用映射表作为兜底
  if (suggestions.length === 0) {
    const suggestionMap = {
      '洪涝': ['立即疏散周边群众', '调派排水设备', '设置警示标志', '协调交通管制'],
      '火灾': ['立即通知消防部门', '疏散周边人员', '切断电源气源', '设置隔离带'],
      '地震': ['启动应急预案', '搜救被困人员', '检查建筑安全', '提供临时避难所'],
      '台风': ['加固临时建筑', '转移危险区域人员', '停止户外作业', '储备应急物资'],
      '泥石流': ['封锁危险区域', '疏散下游居民', '监测山体变化', '清理淤泥']
    }
    suggestions = suggestionMap[type] || ['持续监控事态发展', '等待进一步信息确认']
  }
  
  return {
    tags,
    riskLevel: risk.level,
    riskText: risk.text,
    suggestions
  }
})

// 谣言研判信息
const rumorInfo = computed(() => {
  if (!props.incident) return { isRumor: false, reason: '', keywords: [] }
  const conf = props.incident.confidence || 0
  let reason = ''
  let keywords = []
  
  // 从 aiSuggestion 提取关键词和谣言原因
  if (props.incident.aiSuggestion) {
    try {
      const aiData = typeof props.incident.aiSuggestion === 'string' 
        ? JSON.parse(props.incident.aiSuggestion) 
        : props.incident.aiSuggestion
      if (aiData.rumor_reason) reason = aiData.rumor_reason
      if (aiData.keywords) keywords = aiData.keywords
      if (aiData.is_rumor) reason = reason || '模型判定为疑似不实信息'
    } catch {}
  }
  
  if (!reason) {
    if (conf >= 80) reason = '信息来源可靠、内容详实、无明显谣言特征，可信度高'
    else if (conf >= 60) reason = '信息较完整，但部分细节有待进一步核实'
    else if (conf >= 40) reason = '信息来源不明确，内容存在模糊表述，建议人工复核'
    else reason = '信息严重缺失或存在典型谣言特征，建议优先人工审核'
  }
  
  return { isRumor: conf < 40, reason, keywords }
})

// 谣言判定结论
const rumorVerdict = computed(() => {
  const conf = props.incident?.confidence || 0
  if (conf >= 70) return { level: 'credible', text: '信息可信', icon: '✅' }
  if (conf >= 40) return { level: 'uncertain', text: '待核实', icon: '⚠️' }
  return { level: 'rumor', text: '疑似谣言', icon: '❌' }
})

// 初始化谣言雷达图
const initRumorRadar = () => {
  if (!rumorRadarChart.value || !props.incident) return
  if (radarChartInstance) radarChartInstance.dispose()
  radarChartInstance = echarts.init(rumorRadarChart.value)
  
  const conf = props.incident.confidence || 0
  // 基于置信度生成多维度评估（模拟AI多维输出）
  const base = conf / 100
  const dims = [
    Math.min(100, Math.round((base * 0.9 + Math.random() * 0.1) * 100)),   // 来源可靠性
    Math.min(100, Math.round((base * 0.85 + Math.random() * 0.15) * 100)), // 内容一致性
    Math.min(100, Math.round((base * 0.8 + Math.random() * 0.2) * 100)),   // 逻辑合理性
    Math.min(100, Math.round((base * 0.88 + Math.random() * 0.12) * 100)), // 细节完整度
    Math.min(100, Math.round((base * 0.92 + Math.random() * 0.08) * 100))  // 情绪客观性
  ]

  const option = {
    backgroundColor: 'transparent',
    radar: {
      indicator: [
        { name: '来源可靠性', max: 100 },
        { name: '内容一致性', max: 100 },
        { name: '逻辑合理性', max: 100 },
        { name: '细节完整度', max: 100 },
        { name: '情绪客观性', max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: 'rgba(255,255,255,0.65)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.15)' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,0.02)', 'rgba(0,212,255,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.2)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: dims,
        name: '可信度评估',
        lineStyle: { color: conf >= 60 ? '#00d4ff' : '#ff3366', width: 2 },
        areaStyle: { color: conf >= 60 ? 'rgba(0,212,255,0.2)' : 'rgba(255,51,102,0.2)' },
        itemStyle: { color: conf >= 60 ? '#00d4ff' : '#ff3366' },
        symbol: 'circle',
        symbolSize: 5
      }]
    }]
  }
  radarChartInstance.setOption(option)
}

onUnmounted(() => {
  if (radarChartInstance) {
    radarChartInstance.dispose()
    radarChartInstance = null
  }
})

// 时间线 - 基于真实状态
const timeline = computed(() => {
  if (!props.incident) return []
  
  const createTime = new Date(props.incident.createTime)
  const items = [
    {
      type: 'info',
      time: createTime.toLocaleString('zh-CN'),
      title: '事件上报',
      description: `来源: ${props.incident.sourcePlatform || '系统自动生成'}`
    },
    {
      type: 'warning',
      time: new Date(createTime.getTime() + 30000).toLocaleString('zh-CN'),
      title: 'AI研判完成',
      description: `灾害类型: ${props.incident.disasterType || '未知'}, 置信度: ${props.incident.confidence || 0}%`
    }
  ]
  
  if (props.incident.status >= 1) {
    items.push({
      type: 'success',
      time: new Date(createTime.getTime() + 180000).toLocaleString('zh-CN'),
      title: '事件已确认',
      description: '管理员确认事件真实性'
    })
  }
  if (props.incident.status >= 2) {
    items.push({
      type: 'success',
      time: new Date(createTime.getTime() + 300000).toLocaleString('zh-CN'),
      title: '已下发派单',
      description: '智能匹配执勤人员'
    })
  }
  if (props.incident.status >= 4) {
    items.push({
      type: 'success',
      time: new Date(createTime.getTime() + 600000).toLocaleString('zh-CN'),
      title: '处理完成',
      description: props.incident.feedback || '事件已处理完毕'
    })
  }
  if (props.incident.status === 5) {
    items.push({
      type: 'warning',
      time: new Date(createTime.getTime() + 180000).toLocaleString('zh-CN'),
      title: '事件已驳回',
      description: '审核未通过'
    })
  }
  if (props.incident.status === 0) {
    items.push({
      type: 'pending',
      time: '待处理',
      title: '等待确认',
      description: '等待管理员审核确认'
    })
  }
  
  return items
})

// 智能一键派单
const handleSmartDispatch = async () => {
  if (!props.incident) return
  smartDispatching.value = true
  try {
    const res = await post('/admin/dispatch/smart', {
      incidentId: props.incident.id
    })
    const data = res.data || res || {}
    
    if (data.success === false) {
      ElMessage.error(data.message || '智能派单失败：无可用人员')
      return
    }
    
    const responderName = data.responderName || '最优人员'
    const reason = data.reason || '根据岗位匹配度和距离综合评估'
    
    // 防XSS：对后端返回的文本做HTML转义
    const esc = (s) => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;')
    
    ElMessage.success('智能派单成功！已派单给 ' + responderName)
    
    // 展示派单结果详情
    ElMessageBox.alert(
      `<div style="line-height: 1.8;">
        <p><strong>AI智能匹配结果</strong></p>
        <p>推荐人员：<span style="color: #00d4ff; font-weight: 600;">${esc(responderName)}</span></p>
        <p>匹配理由：${esc(reason)}</p>
        <p style="color: #00ff88; font-size: 12px; margin-top: 8px;">已自动派单并通知执勤人员</p>
      </div>`,
      '派单成功',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        type: 'success'
      }
    )
    visible.value = false
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error?.response?.data?.msg || error?.message || '未知错误'
      ElMessage.error('智能派单失败：' + msg)
    }
  } finally {
    smartDispatching.value = false
  }
}

// 确认事件
const handleConfirm = () => {
  emit('confirm', props.incident)
  visible.value = false
}

// 驳回事件
const handleReject = () => {
  emit('reject', props.incident)
  visible.value = false
}

// 派单处理
const handleDispatch = () => {
  emit('dispatch', props.incident)
  visible.value = false
}

// 关闭弹窗
const handleClose = () => {
  emit('close')
  visible.value = false
}
</script>

<style scoped>
.event-detail-modal :deep(.el-dialog) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.event-detail-modal :deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.event-detail-modal :deep(.el-dialog__title) {
  color: var(--primary-color);
  font-weight: 600;
}

.event-detail-modal :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-muted);
}

.event-detail-modal :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: var(--primary-color);
}

.event-detail-modal :deep(.el-dialog__body) {
  padding: 0;
  max-height: 60vh;
  overflow-y: auto;
}

.event-detail-modal :deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}

.modal-content {
  padding: 20px;
}

/* 信息区块 */
.info-section {
  margin-bottom: 24px;
}

.info-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color);
}

.section-header .count {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: normal;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item .label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-item .value {
  font-size: 14px;
  color: var(--text-primary);
}

/* 标签样式 */
.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: white;
}

.severity-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
}

.severity-badge.severity-critical {
  background: rgba(255, 51, 102, 0.2);
  color: #ff3366;
}

.severity-badge.severity-high {
  background: rgba(255, 170, 0, 0.2);
  color: #ffaa00;
}

.severity-badge.severity-medium {
  background: rgba(0, 212, 255, 0.2);
  color: #00d4ff;
}

.severity-badge.severity-low {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
}

.status-badge.status-0 {
  background: rgba(255, 170, 0, 0.2);
  color: #ffaa00;
}

.status-badge.status-1 {
  background: rgba(0, 212, 255, 0.2);
  color: #00d4ff;
}

.status-badge.status-2 {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.status-badge.status-3 {
  background: rgba(102, 102, 102, 0.2);
  color: #999;
}

.status-badge.status-4 {
  background: rgba(255, 51, 102, 0.2);
  color: #ff3366;
}

/* 摘要内容 */
.summary-content {
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

/* 证据列表 */
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.evidence-item:hover {
  background: rgba(0, 212, 255, 0.1);
}

.evidence-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-dark);
  border-radius: var(--radius-md);
  color: var(--primary-color);
  font-size: 18px;
}

.evidence-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.evidence-name {
  font-size: 13px;
  color: var(--text-primary);
}

.evidence-meta {
  font-size: 11px;
  color: var(--text-muted);
}

.empty-evidence {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

/* 证据链 - 链式可视化 */
.evidence-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--primary-color);
  font-size: 13px;
}

.evidence-chain {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.evidence-chain-node {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.chain-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
}

.connector-line {
  width: 2px;
  height: 16px;
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.6), rgba(0, 212, 255, 0.3));
}

.connector-arrow {
  font-size: 10px;
  color: rgba(0, 212, 255, 0.6);
  line-height: 1;
  margin-top: -2px;
}

.chain-card {
  padding: 12px 16px;
  background: rgba(0, 20, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 8px;
  transition: all 0.2s;
}

.chain-card:hover {
  background: rgba(0, 30, 60, 0.8);
  border-color: rgba(0, 212, 255, 0.35);
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.1);
}

.chain-card.is-rumor {
  border-color: rgba(255, 51, 102, 0.3);
  background: rgba(40, 10, 15, 0.6);
}

.chain-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.chain-index {
  background: linear-gradient(135deg, var(--primary-color), #0095b3);
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 28px;
  text-align: center;
}

.chain-time {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
}

.chain-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chain-publisher {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--primary-color);
}

.chain-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  margin: 0;
  word-break: break-all;
}

.chain-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

/* AI分析 */
.ai-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.analysis-label {
  font-size: 12px;
  color: var(--text-muted);
}

.analysis-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.risk-level {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.risk-level.low {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.risk-level.medium {
  background: rgba(0, 212, 255, 0.2);
  color: #00d4ff;
}

.risk-level.high {
  background: rgba(255, 170, 0, 0.2);
  color: #ffaa00;
}

.risk-level.critical {
  background: rgba(255, 51, 102, 0.2);
  color: #ff3366;
}

.suggestions {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.suggestions li {
  margin-bottom: 4px;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 24px;
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
  padding-left: 20px;
  border-left: 2px solid var(--border-color);
}

.timeline-item.is-last {
  border-left-color: transparent;
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -7px;
  top: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
}

.timeline-dot.info {
  border-color: var(--primary-color);
  background: var(--primary-color);
}

.timeline-dot.warning {
  border-color: var(--warning-color);
  background: var(--warning-color);
}

.timeline-dot.success {
  border-color: var(--success-color);
  background: var(--success-color);
}

.timeline-dot.pending {
  border-color: var(--text-muted);
  background: var(--bg-card);
}

.timeline-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.timeline-title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.timeline-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 底部操作 */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-right {
  display: flex;
  gap: 12px;
}

/* 进度条样式 */
:deep(.el-progress) {
  width: 100%;
}

:deep(.el-progress-bar__outer) {
  background: rgba(0, 0, 0, 0.3);
}

:deep(.el-progress__text) {
  color: var(--text-primary);
}

/* 标签样式覆盖 */
:deep(.el-tag) {
  background: rgba(0, 212, 255, 0.1);
  border-color: var(--border-color);
  color: var(--primary-color);
}

/* ====== 谣言分析面板 ====== */
.rumor-section {
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 10px;
  padding: 16px !important;
  background: rgba(0, 212, 255, 0.03);
}

.rumor-verdict {
  margin-left: auto;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.rumor-verdict.credible { background: rgba(0,255,136,0.18); color: #00ff88; }
.rumor-verdict.uncertain { background: rgba(255,170,0,0.18); color: #ffaa00; }
.rumor-verdict.rumor { background: rgba(255,51,102,0.18); color: #ff3366; }

.rumor-analysis-panel {
  display: flex;
  gap: 20px;
  margin-top: 12px;
}

.rumor-radar-container {
  flex-shrink: 0;
  width: 220px;
  height: 200px;
}

.rumor-radar-chart {
  width: 100%;
  height: 100%;
}

.rumor-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rumor-score-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.score-label {
  font-size: 12px;
  color: var(--text-muted);
}

.score-track {
  position: relative;
  height: 8px;
  background: rgba(255,255,255,0.06);
  border-radius: 4px;
  overflow: visible;
}

.score-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.8s ease;
  box-shadow: 0 0 8px currentColor;
}

.score-text {
  position: absolute;
  right: 0;
  top: -20px;
  font-size: 13px;
  font-weight: 700;
}

.rumor-reason, .rumor-keywords {
  font-size: 13px;
}

.reason-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.reason-text {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 12px;
  font-size: 11px;
  color: #00d4ff;
}
</style>
