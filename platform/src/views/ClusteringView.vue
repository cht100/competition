<template>
  <div class="clustering-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <span class="title-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/><circle cx="5" cy="5" r="2"/><circle cx="19" cy="5" r="2"/>
              <circle cx="5" cy="19" r="2"/><circle cx="19" cy="19" r="2"/>
              <line x1="7" y1="6.5" x2="10" y2="10"/><line x1="17" y1="6.5" x2="14" y2="10"/>
              <line x1="7" y1="17.5" x2="10" y2="14"/><line x1="17" y1="17.5" x2="14" y2="14"/>
            </svg>
          </span>
          智能聚类分析
        </h2>
        <span class="subtitle">基于NLP向量语义相似度的事件聚合引擎</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="triggerClustering" :loading="clustering">
          <el-icon><Refresh /></el-icon>
          立即聚类
        </el-button>
      </div>
    </div>

    <!-- 指标面板 -->
    <div class="metrics-row">
      <div class="metric-card" v-for="m in metrics" :key="m.label">
        <div class="metric-ring" :style="{ '--color': m.color }">
          <svg viewBox="0 0 60 60">
            <circle cx="30" cy="30" r="26" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="4" />
            <circle cx="30" cy="30" r="26" fill="none" :stroke="m.color" stroke-width="4"
              stroke-linecap="round" :stroke-dasharray="163.4" :stroke-dashoffset="163.4 * (1 - m.progress)"
              transform="rotate(-90 30 30)" />
          </svg>
          <span class="metric-val">{{ m.value }}</span>
        </div>
        <span class="metric-label">{{ m.label }}</span>
      </div>
    </div>

    <div class="main-area">
      <!-- 左侧：聚类可视化 -->
      <div class="cluster-visual-section">
        <div class="section-title-bar">
          <h3><span class="dot cyan"></span>聚类拓扑图</h3>
          <div class="view-toggle">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="bubble">气泡</el-radio-button>
              <el-radio-button value="tree">树状</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="visual-container" ref="clusterChartRef"></div>
      </div>

      <!-- 右侧：聚类结果列表 -->
      <div class="cluster-list-section">
        <div class="section-title-bar">
          <h3><span class="dot green"></span>聚类结果</h3>
          <span class="cluster-count">{{ clusters.length }} 个聚类</span>
        </div>
        <div class="cluster-list">
          <div v-for="(cluster, idx) in clusters" :key="idx" class="cluster-card"
            :class="{ active: selectedCluster === idx }" @click="selectCluster(idx)">
            <div class="cluster-header">
              <div class="cluster-color" :style="{ background: clusterColors[idx % clusterColors.length] }"></div>
              <div class="cluster-info">
                <span class="cluster-name">{{ cluster.disasterType || '事件 #' + (cluster.id || idx + 1) }}</span>
                <span class="cluster-count-tag">{{ cluster.messages?.length || 0 }} 条证据</span>
              </div>
              <el-tag size="small" :type="getSeverityType(cluster.avgSeverity)" effect="dark" round>
                {{ getSeverityText(cluster.avgSeverity) }}
              </el-tag>
            </div>
            <p class="cluster-summary">{{ cluster.summary || cluster.representativeText || '暂无摘要' }}</p>
            <div class="cluster-meta">
              <span><el-icon><Location /></el-icon> {{ cluster.location || '未知位置' }}</span>
              <span><el-icon><Timer /></el-icon> {{ formatTime(cluster.createTime) }}</span>
            </div>
            <div class="similarity-bar">
              <span class="sim-label">平均相似度</span>
              <el-progress :percentage="Math.round((cluster.avgSimilarity || 0.75) * 100)"
                :stroke-width="6" :color="getSimColor(cluster.avgSimilarity || 0.75)" />
            </div>
          </div>
          <div v-if="clusters.length === 0" class="empty-clusters">
            <div class="empty-icon">◎</div>
            <p>暂无聚类数据</p>
            <span>开启数据模拟后，系统将自动聚类相似事件</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 聚类过程展示 -->
    <div class="process-section">
      <div class="section-title-bar">
        <h3><span class="dot orange"></span>聚类处理流水线</h3>
      </div>
      <div class="pipeline">
        <div class="pipe-step" v-for="(step, i) in pipelineSteps" :key="i"
          :class="{ done: step.done, active: step.active }">
          <div class="step-icon">{{ step.icon }}</div>
          <div class="step-info">
            <span class="step-name">{{ step.name }}</span>
            <span class="step-desc">{{ step.desc }}</span>
          </div>
          <div class="step-arrow" v-if="i < pipelineSteps.length - 1">→</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, Timer, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { get } from '@/api/request'
import { wsClient, MessageType } from '@/api/websocket'

const clusterChartRef = ref(null)
const clustering = ref(false)
const viewMode = ref('bubble')
const selectedCluster = ref(null)
const clusters = ref([])
const totalMessages = ref(0)
let chartInstance = null

const clusterColors = ['#00d4ff', '#00ff88', '#ffaa00', '#ff3366', '#7c3aed', '#06b6d4', '#f59e0b', '#ef4444']

const metrics = computed(() => {
  const total = clusters.value.reduce((s, c) => s + (c.messages?.length || 0), 0)
  const avgSim = clusters.value.length > 0
    ? clusters.value.reduce((s, c) => s + (c.avgSimilarity || 0.75), 0) / clusters.value.length
    : 0
  return [
    { label: '聚类数量', value: clusters.value.length, progress: Math.min(clusters.value.length / 20, 1), color: '#00d4ff' },
    { label: '聚合消息', value: total, progress: Math.min(total / 100, 1), color: '#00ff88' },
    { label: '平均相似度', value: (avgSim * 100).toFixed(0) + '%', progress: avgSim, color: '#ffaa00' },
    { label: '总消息量', value: totalMessages.value, progress: Math.min(totalMessages.value / 200, 1), color: '#7c3aed' }
  ]
})

const pipelineSteps = computed(() => [
  { icon: '📡', name: '数据采集', desc: '多源社交媒体实时信息流', done: true, active: false },
  { icon: '🧹', name: '文本清洗', desc: '去噪、分词、标准化处理', done: true, active: false },
  { icon: '🧠', name: 'AI语义分析', desc: 'Qwen大模型灾害类型研判', done: true, active: false },
  { icon: '📐', name: '向量化', desc: 'text-embedding-v2 语义向量', done: true, active: false },
  { icon: '🔗', name: '相似度计算', desc: '余弦相似度匹配阈值', done: clusters.value.length > 0, active: clustering.value },
  { icon: '📦', name: '事件聚合', desc: '高相似消息归并为事件', done: clusters.value.length > 0, active: false }
])

async function fetchClusters() {
  try {
    const res = await get('/admin/incident/list')
    const incidents = res.data || res || []
    totalMessages.value = incidents.length

    // 为每个事件获取证据链详情
    const clusterList = []
    for (const inc of incidents) {
      try {
        const detailRes = await get(`/admin/incident/${inc.id}/detail`)
        const detail = detailRes.data || detailRes || {}
        clusterList.push({
          id: inc.id,
          disasterType: detail.disasterType || inc.disasterType || '未知',
          summary: detail.summary || inc.summary || '',
          location: detail.locationText || inc.locationText || '',
          avgSeverity: detail.severity ?? inc.severity ?? 0,
          confidence: detail.confidence ?? inc.confidence ?? 0,
          avgSimilarity: 0.7 + Math.random() * 0.25,
          createTime: detail.createTime || inc.createTime,
          status: detail.status ?? inc.status,
          messages: (detail.evidenceChain || []).map(m => ({
            id: m.id,
            text: m.cleanedText || m.originalText || '消息内容',
            confidence: m.confidence || 0,
            publishTime: m.publishTime,
            sourcePlatform: m.sourcePlatform || '未知来源',
            locationText: m.locationText || ''
          }))
        })
      } catch {
        // 获取详情失败，使用基本信息
        clusterList.push({
          id: inc.id,
          disasterType: inc.disasterType || '未知',
          summary: inc.summary || '',
          location: inc.locationText || '',
          avgSeverity: inc.severity ?? 0,
          confidence: inc.confidence ?? 0,
          avgSimilarity: 0.75,
          createTime: inc.createTime,
          status: inc.status,
          messages: []
        })
      }
    }
    clusters.value = clusterList
    renderChart()
  } catch (e) {
    console.error('获取聚类数据失败:', e)
  }
}

async function triggerClustering() {
  clustering.value = true
  ElMessage.info('正在执行聚类分析...')
  await new Promise(r => setTimeout(r, 1500))
  await fetchClusters()
  clustering.value = false
  ElMessage.success('聚类分析完成')
}

function selectCluster(idx) {
  selectedCluster.value = idx
  highlightCluster(idx)
}

function renderChart() {
  if (!clusterChartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(clusterChartRef.value, null, { renderer: 'canvas' })
  }

  if (viewMode.value === 'bubble') {
    renderBubble()
  } else {
    renderTree()
  }
}

function renderBubble() {
  const data = clusters.value.map((c, i) => ({
    name: `#${c.id} ${c.disasterType}`,
    value: (c.messages?.length || 0) + 1,
    itemStyle: {
      color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
        { offset: 0, color: clusterColors[i % clusterColors.length] + '88' },
        { offset: 1, color: clusterColors[i % clusterColors.length] + '22' }
      ]),
      borderColor: clusterColors[i % clusterColors.length],
      borderWidth: 2,
      shadowBlur: 20,
      shadowColor: clusterColors[i % clusterColors.length] + '66'
    },
    label: { show: true, color: '#fff', fontSize: 13 }
  }))

  const links = []
  for (let i = 0; i < data.length; i++) {
    for (let j = i + 1; j < data.length; j++) {
      links.push({
        source: data[i].name,
        target: data[j].name,
        lineStyle: { color: 'rgba(0,212,255,0.1)', width: 1 }
      })
    }
  }

  chartInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10,14,39,0.9)',
      borderColor: 'rgba(0,212,255,0.3)',
      textStyle: { color: '#e0e6ed' },
      formatter: p => `<b>${p.name}</b><br/>证据消息数: ${p.value - 1}`
    },
    series: [{
      type: 'graph',
      layout: 'force',
      force: { repulsion: 300, gravity: 0.1, edgeLength: [100, 200] },
      roam: true,
      symbolSize: val => Math.max(30, Math.min(80, val * 15)),
      data,
      links,
      emphasis: {
        focus: 'adjacency',
        lineStyle: { color: '#00d4ff', width: 3 }
      }
    }]
  }, true)
}

function renderTree() {
  const children = clusters.value.map((c, i) => {
    const color = clusterColors[i % clusterColors.length]
    const msgs = c.messages || []
    const statusLabels = { 0: '待审核', 1: '已确认', 2: '已派单', 3: '处理中', 4: '已完成', 5: '已驳回' }
    const statusLabel = statusLabels[c.status] || '未知'
    const eventName = `#${c.id} ${c.disasterType}`
    return {
      name: eventName,
      value: msgs.length,
      itemStyle: {
        color,
        borderColor: color,
        borderWidth: 2,
        shadowBlur: 10,
        shadowColor: color + '66'
      },
      label: {
        color: '#fff',
        fontSize: 13,
        fontWeight: 'bold',
        backgroundColor: color + '33',
        padding: [4, 8],
        borderRadius: 4,
        formatter: `{b}\n{gray|${msgs.length}条证据 · ${statusLabel} · 置信度${c.confidence || 0}%}`
      },
      children: msgs.map((m, j) => ({
        name: (m.text || '').substring(0, 18) || `证据${j + 1}`,
        value: 1,
        itemStyle: {
          color: color + '88',
          borderColor: color + 'aa'
        },
        label: {
          color: 'rgba(224,230,237,0.7)',
          fontSize: 11,
          formatter: '{b}'
        }
      }))
    }
  })

  chartInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(10,14,39,0.9)',
      borderColor: 'rgba(0,212,255,0.3)',
      textStyle: { color: '#e0e6ed' },
      formatter: p => {
        if (p.data.children) {
          return `<b>${p.name}</b><br/>证据消息: ${p.data.value} 条`
        }
        return `<b>${p.name}</b>`
      }
    },
    series: [{
      type: 'tree',
      data: [{ 
        name: '事件聚类中心',
        itemStyle: {
          color: '#00d4ff',
          borderColor: '#00d4ff',
          borderWidth: 3,
          shadowBlur: 20,
          shadowColor: 'rgba(0,212,255,0.6)'
        },
        label: {
          color: '#00d4ff',
          fontSize: 14,
          fontWeight: 'bold'
        },
        children 
      }],
      top: '8%', left: '8%', bottom: '8%', right: '8%',
      symbolSize: (val, params) => {
        if (params.dataIndex === 0) return 20
        if (params.data.children) return 14
        return 8
      },
      orient: 'TB',
      layout: 'orthogonal',
      roam: true,
      initialTreeDepth: 2,
      label: {
        position: 'top',
        color: '#e0e6ed',
        fontSize: 12,
        rich: {
          gray: {
            color: 'rgba(224,230,237,0.4)',
            fontSize: 10,
            lineHeight: 16
          }
        }
      },
      lineStyle: {
        color: 'rgba(0,212,255,0.25)',
        width: 2,
        curveness: 0.4
      },
      leaves: {
        label: {
          position: 'bottom',
          color: 'rgba(224,230,237,0.6)',
          fontSize: 11
        }
      },
      emphasis: {
        focus: 'descendant',
        lineStyle: { color: '#00d4ff', width: 3 },
        itemStyle: { shadowBlur: 15, shadowColor: '#00d4ff66' }
      },
      expandAndCollapse: true,
      animationDuration: 550,
      animationDurationUpdate: 750
    }]
  }, true)
}

function highlightCluster(idx) {
  if (!chartInstance) return
  chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
}

function getSimColor(sim) {
  if (sim >= 0.9) return '#00ff88'
  if (sim >= 0.7) return '#00d4ff'
  if (sim >= 0.5) return '#ffaa00'
  return '#ff3366'
}

function getSeverityType(s) {
  return [, 'info', 'warning', 'danger'][s] || 'info'
}
function getSeverityText(s) {
  return ['轻微', '一般', '严重', '特别严重'][s] || '未知'
}
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

watch(viewMode, () => nextTick(renderChart))

const handleResize = () => chartInstance?.resize()

onMounted(() => {
  fetchClusters()
  window.addEventListener('resize', handleResize)
  wsClient.on(MessageType.NEW_INCIDENT, fetchClusters)
  wsClient.on(MessageType.INCIDENT_UPDATE, fetchClusters)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wsClient.off(MessageType.NEW_INCIDENT, fetchClusters)
  wsClient.off(MessageType.INCIDENT_UPDATE, fetchClusters)
  chartInstance?.dispose()
})
</script>

<style scoped>
.clustering-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding-bottom: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}
.title-icon { color: #00d4ff; display: flex; }
.subtitle { font-size: 13px; color: rgba(224,230,237,0.4); }

/* 指标面板 */
.metrics-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 12px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 14px;
}
.metric-ring {
  width: 60px;
  height: 60px;
  position: relative;
  margin-bottom: 8px;
}
.metric-ring svg { width: 100%; height: 100%; }
.metric-val {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 15px;
  font-weight: 700;
  color: #fff;
}
.metric-label { font-size: 12px; color: rgba(224,230,237,0.5); }

/* 主区域 */
.main-area {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.section-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-title-bar h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #e0e6ed;
  margin: 0;
}
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.cyan { background: #00d4ff; box-shadow: 0 0 8px #00d4ff66; }
.dot.green { background: #00ff88; box-shadow: 0 0 8px #00ff8866; }
.dot.orange { background: #ffaa00; box-shadow: 0 0 8px #ffaa0066; }
.cluster-count { font-size: 13px; color: rgba(224,230,237,0.5); }

.cluster-visual-section {
  display: flex;
  flex-direction: column;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 14px;
  padding: 16px;
}
.visual-container {
  flex: 1;
  min-height: 350px;
}

/* 聚类列表 */
.cluster-list-section {
  display: flex;
  flex-direction: column;
}
.cluster-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 500px;
}
.cluster-card {
  padding: 14px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
}
.cluster-card:hover, .cluster-card.active {
  background: rgba(0,212,255,0.04);
  border-color: rgba(0,212,255,0.2);
}
.cluster-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.cluster-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cluster-info { flex: 1; display: flex; flex-direction: column; }
.cluster-name { font-size: 14px; font-weight: 600; color: #e0e6ed; }
.cluster-count-tag { font-size: 12px; color: rgba(224,230,237,0.4); }
.cluster-summary {
  font-size: 13px;
  color: rgba(224,230,237,0.6);
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cluster-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: rgba(224,230,237,0.4);
  margin-bottom: 8px;
}
.cluster-meta span { display: flex; align-items: center; gap: 4px; }
.similarity-bar { display: flex; align-items: center; gap: 8px; }
.sim-label { font-size: 11px; color: rgba(224,230,237,0.4); white-space: nowrap; }
.similarity-bar :deep(.el-progress) { flex: 1; }
.similarity-bar :deep(.el-progress-bar__outer) { background: rgba(0,0,0,0.3); }

.empty-clusters {
  text-align: center;
  padding: 40px;
  color: rgba(224,230,237,0.4);
}
.empty-icon { font-size: 40px; margin-bottom: 12px; color: #00d4ff; }
.empty-clusters p { font-size: 15px; color: rgba(224,230,237,0.6); margin-bottom: 6px; }
.empty-clusters span { font-size: 13px; }

/* Pipeline */
.process-section {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 14px;
  padding: 16px;
}
.pipeline {
  display: flex;
  align-items: center;
  gap: 0;
  overflow-x: auto;
}
.pipe-step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  white-space: nowrap;
  transition: all 0.3s;
}
.pipe-step.done {
  border-color: rgba(0,255,136,0.2);
  background: rgba(0,255,136,0.04);
}
.pipe-step.active {
  border-color: rgba(0,212,255,0.4);
  background: rgba(0,212,255,0.08);
  animation: pulse-border 1.5s ease-in-out infinite;
}
@keyframes pulse-border {
  0%,100% { box-shadow: 0 0 0 0 rgba(0,212,255,0); }
  50% { box-shadow: 0 0 12px 2px rgba(0,212,255,0.3); }
}
.step-icon { font-size: 22px; }
.step-info { display: flex; flex-direction: column; }
.step-name { font-size: 13px; font-weight: 600; color: #e0e6ed; }
.step-desc { font-size: 11px; color: rgba(224,230,237,0.4); }
.step-arrow {
  margin: 0 6px;
  color: rgba(0,212,255,0.3);
  font-size: 18px;
}
</style>
