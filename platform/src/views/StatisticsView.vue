<template>
  <div class="statistics-view">
    <header class="stats-header">
      <div class="header-left">
        <el-button @click="goBack" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1 class="page-title">统计看板</h1>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateChange"
        />
        <el-button type="primary" @click="exportData">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </header>

    <main class="stats-main">
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon events">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ todayEvents }}</div>
            <div class="stat-label">今日事件</div>
          </div>
          <div class="stat-trend up" v-if="todayEventsTrend > 0">
            <el-icon><Top /></el-icon>
            {{ todayEventsTrend }}%
          </div>
          <div class="stat-trend down" v-else-if="todayEventsTrend < 0">
            <el-icon><Bottom /></el-icon>
            {{ Math.abs(todayEventsTrend) }}%
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon time">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgProcessTime }}</div>
            <div class="stat-label">平均处置耗时</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon rumor">
            <el-icon><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ rejectedCount }}</div>
            <div class="stat-label">驳回数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon complete">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ completedRate }}%</div>
            <div class="stat-label">完成率</div>
          </div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">事件趋势</h3>
            <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
              <el-radio-button value="day">日</el-radio-button>
              <el-radio-button value="week">周</el-radio-button>
              <el-radio-button value="month">月</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" class="chart"></div>
        </div>
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">灾种分布</h3>
          </div>
          <div ref="typeChart" class="chart"></div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">状态分布</h3>
          </div>
          <div ref="statusChart" class="chart"></div>
        </div>
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">谣言分析</h3>
          </div>
          <div ref="rumorGaugeChart" class="chart"></div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">处置效率</h3>
          </div>
          <div ref="efficiencyChart" class="chart"></div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-container wide">
          <div class="chart-header">
            <h3 class="chart-title">执勤人员任务统计</h3>
          </div>
          <div ref="regionChart" class="chart"></div>
        </div>
      </div>

      <div class="data-table card">
        <div class="card-header">
          <h3 class="card-title">详细数据</h3>
        </div>
        <el-table :data="tableData" style="width: 100%" max-height="400" stripe :header-cell-style="{ background: 'rgba(0,51,102,0.3)', color: '#00d4ff' }" :cell-style="{ background: 'transparent', color: 'rgba(255,255,255,0.85)' }">
          <el-table-column prop="date" label="日期" min-width="100" />
          <el-table-column prop="events" label="事件数" min-width="80" />
          <el-table-column prop="completed" label="已完成" min-width="80" />
          <el-table-column prop="processing" label="处理中" min-width="80" />
          <el-table-column prop="rejected" label="驳回" min-width="80" />
          <el-table-column prop="avgTime" label="平均耗时" min-width="100" />
          <el-table-column prop="rumors" label="谣言数" min-width="80" />
          <el-table-column prop="status" label="状态" min-width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '正常' ? 'success' : 'warning'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { statisticsApi } from '@/api/statistics'
import { get } from '@/api/request'
import { wsClient, MessageType } from '@/api/websocket'

const router = useRouter()

const dateRange = ref([])
const trendPeriod = ref('day')

const trendChart = ref(null)
const typeChart = ref(null)
const statusChart = ref(null)
const efficiencyChart = ref(null)
const regionChart = ref(null)
const rumorGaugeChart = ref(null)

let trendChartInstance = null
let typeChartInstance = null
let statusChartInstance = null
let efficiencyChartInstance = null
let regionChartInstance = null
let rumorGaugeInstance = null

const todayEvents = ref(0)
const todayEventsTrend = ref(0)
const avgProcessTime = ref('0分钟')
const rumorRatio = ref(0)
const rumorRatioDisplay = ref('0.0')
const completedRate = ref(0)
const rejectedCount = ref(0)

const tableData = ref([])



const goBack = () => {
  router.push('/dashboard')
}

const handleDateChange = () => {
  loadStatistics()
  loadTrendData()
}

const exportData = async () => {
  try {
    await statisticsApi.exportReport({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
  } catch (error) {
    console.error('导出失败:', error)
  }
}

const loadStatistics = async () => {
  try {
    const res = await statisticsApi.today()
    const d = res.data || res || {}
    todayEvents.value = d.todayCount || 0
    todayEventsTrend.value = d.trend || 0
    avgProcessTime.value = d.avgProcessTime || '0分钟'
    completedRate.value = d.completedRate || 0
    rejectedCount.value = d.rejectedCount || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
  // 从后端获取谣言占比数据（基于消息级别的AI谣言检测 + 管理员驳回）
  try {
    const rumorRes = await statisticsApi.rumorRatio()
    const rumorData = rumorRes.data || rumorRes || {}
    const ratioStr = rumorData.rumorRatio || '0.0%'
    const ratio = parseFloat(ratioStr)
    rumorRatio.value = isNaN(ratio) ? 0 : ratio
    rumorRatioDisplay.value = rumorRatio.value.toFixed(1)
    // 驳回计数保持为后端返回的管理员驳回事件数（不覆盖为谣言总数）
  } catch (error) {
    console.error('加载谣言比例失败:', error)
  }
}

const loadTrendData = async () => {
  try {
    const res = await statisticsApi.eventTrend({ period: trendPeriod.value })
    const raw = res.data || res
    // 后端返回 [{date, count}, ...] 格式，转换为 {dates, values}
    let chartData = { dates: [], values: [] }
    if (Array.isArray(raw)) {
      chartData = {
        dates: raw.map(item => item.date || ''),
        values: raw.map(item => item.count || 0)
      }
    } else if (raw && raw.dates) {
      chartData = raw
    }
    initTrendChart(chartData)
  } catch (error) {
    console.error('加载趋势数据失败:', error)
    initTrendChart({ dates: [], values: [] })
  }
}

const initTrendChart = (data) => {
  if (!trendChart.value) return

  if (trendChartInstance) {
    trendChartInstance.dispose()
  }

  trendChartInstance = echarts.init(trendChart.value)

  const dates = Array.isArray(data?.dates) ? data.dates : []
  const values = Array.isArray(data?.values) ? data.values : []

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 20, 40, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)' },
      splitLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.1)' } }
    },
    series: [{
      name: '事件数',
      type: 'line',
      smooth: true,
      data: values,
      lineStyle: { color: '#00d4ff', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
          { offset: 1, color: 'rgba(0, 212, 255, 0)' }
        ])
      },
      itemStyle: { color: '#00d4ff' }
    }]
  }

  trendChartInstance.setOption(option)
}

const initTypeChart = async () => {
  if (!typeChart.value) return

  if (typeChartInstance) {
    typeChartInstance.dispose()
  }
  typeChartInstance = echarts.init(typeChart.value)

  let chartData = []
  try {
    const res = await statisticsApi.disasterTypes()
    const raw = res.data || res
    chartData = Array.isArray(raw) ? raw : []
  } catch (error) {
    chartData = []
  }
  // 如果后端API返回空，从事件列表实时统计
  if (chartData.length === 0 || (chartData.length === 1 && chartData[0].value === 0)) {
    try {
      const res = await get('/admin/incident/list')
      const incidents = res.data || res || []
      const typeCount = {}
      const typeColors = { '洪涝': '#00d4ff', '火灾': '#ff3366', '地震': '#ffaa00', '交通事故': '#06b6d4', '台风': '#00ff88', '燃气泄漏': '#f59e0b', '疫情': '#a855f7', '暴雪': '#e0e7ff', '雷电': '#fbbf24', '其他': '#64748b' }
      incidents.forEach(i => {
        const t = i.disasterType || '其他'
        typeCount[t] = (typeCount[t] || 0) + 1
      })
      chartData = Object.entries(typeCount).map(([name, value]) => ({
        value, name, itemStyle: { color: typeColors[name] || '#64748b' }
      }))
    } catch {}
  }
  if (chartData.length === 0) {
    chartData = [{ value: 0, name: '暂无数据', itemStyle: { color: '#333' } }]
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 20, 40, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#fff' }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      data: chartData,
      label: {
        color: 'rgba(255, 255, 255, 0.7)'
      },
      itemStyle: {
        borderColor: '#0a1628',
        borderWidth: 2
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 212, 255, 0.5)'
        }
      }
    }]
  }

  typeChartInstance.setOption(option)
}

const initStatusChart = async () => {
  if (!statusChart.value) return

  if (statusChartInstance) {
    statusChartInstance.dispose()
  }
  statusChartInstance = echarts.init(statusChart.value)

  // Compute status distribution from incident list
  let chartData = []
  try {
    const res = await get('/admin/incident/list')
    const incidents = res.data || res || []
    const counts = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
    incidents.forEach(i => { counts[i.status] = (counts[i.status] || 0) + 1 })
    const colors = { 0: '#ffaa00', 1: '#00d4ff', 2: '#06b6d4', 3: '#f59e0b', 4: '#00ff88', 5: '#ff3366' }
    const names = { 0: '待审核', 1: '已确认', 2: '已派单', 3: '处理中', 4: '已完成', 5: '已驳回' }
    chartData = Object.entries(counts).filter(([,v]) => v > 0).map(([k, v]) => ({
      value: v, name: names[k], itemStyle: { color: colors[k] }
    }))
    if (chartData.length === 0) {
      chartData = [{ value: 0, name: '暂无数据', itemStyle: { color: '#333' } }]
    }
  } catch (error) {
    chartData = [{ value: 0, name: '暂无数据', itemStyle: { color: '#333' } }]
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 20, 40, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#fff' }
    },
    series: [{
      type: 'pie',
      radius: '70%',
      center: ['50%', '50%'],
      data: chartData,
      label: {
        color: 'rgba(255, 255, 255, 0.7)'
      },
      itemStyle: {
        borderColor: '#0a1628',
        borderWidth: 2
      }
    }]
  }

  statusChartInstance.setOption(option)
}

const initRumorGauge = async () => {
  if (!rumorGaugeChart.value) return

  if (rumorGaugeInstance) {
    rumorGaugeInstance.dispose()
  }
  rumorGaugeInstance = echarts.init(rumorGaugeChart.value)

  let ratio = rumorRatio.value || 0
  // 从后端获取真实谣言占比：(AI谣言消息 + 管理员驳回事件) / 总消息数 × 100%
  try {
    const rumorRes = await statisticsApi.rumorRatio()
    const rumorData = rumorRes.data || rumorRes || {}
    const ratioStr = rumorData.rumorRatio || '0.0%'
    const parsed = parseFloat(ratioStr)
    ratio = isNaN(parsed) ? 0 : parsed
    rumorRatio.value = ratio
    rumorRatioDisplay.value = ratio.toFixed(1)
  } catch (e) {
    console.error('获取谣言占比失败:', e)
  }

  const option = {
    backgroundColor: 'transparent',
    series: [
      {
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        radius: '85%',
        splitNumber: 5,
        axisLine: {
          lineStyle: {
            width: 16,
            color: [
              [0.2, '#00ff88'],
              [0.5, '#00d4ff'],
              [0.75, '#ffaa00'],
              [1, '#ff3366']
            ]
          }
        },
        pointer: {
          icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
          length: '55%',
          width: 8,
          offsetCenter: [0, '-10%'],
          itemStyle: { color: 'auto' }
        },
        axisTick: { distance: -20, length: 6, lineStyle: { color: '#999', width: 1 } },
        splitLine: { distance: -24, length: 16, lineStyle: { color: '#999', width: 2 } },
        axisLabel: { color: 'rgba(255,255,255,0.5)', distance: -30, fontSize: 10 },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: 'auto',
          fontSize: 28,
          fontWeight: 700,
          offsetCenter: [0, '40%']
        },
        title: {
          offsetCenter: [0, '65%'],
          fontSize: 13,
          color: 'rgba(255,255,255,0.6)'
        },
        data: [{ value: ratio, name: '谣言占比' }]
      }
    ]
  }
  rumorGaugeInstance.setOption(option)
}

const initEfficiencyChart = async () => {
  if (!efficiencyChart.value) return

  if (efficiencyChartInstance) {
    efficiencyChartInstance.dispose()
  }
  efficiencyChartInstance = echarts.init(efficiencyChart.value)

  let chartData = { categories: [], values: [] }
  try {
    const res = await statisticsApi.efficiency()
    const raw = res.data || res || {}
    // 后端返回 hourlyEfficiency/completedRate/dispatchRate，需转换为图表格式
    const hourly = raw.hourlyEfficiency || []
    const withData = hourly.filter(h => h.count > 0)
    if (withData.length > 0) {
      chartData = {
        categories: withData.map(h => h.hour + ':00'),
        values: withData.map(h => Math.round(h.efficiency || 0))
      }
    } else {
      chartData = {
        categories: ['完成率', '派单率'],
        values: [parseFloat(raw.completedRate) || 0, parseFloat(raw.dispatchRate) || 0]
      }
    }
  } catch (error) {
    chartData = { categories: [], values: [] }
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 20, 40, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.categories || [],
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)' }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', formatter: '{value}%' },
      splitLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.1)' } }
    },
    series: [{
      type: 'bar',
      data: chartData.values || [],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#00d4ff' },
          { offset: 1, color: '#0066cc' }
        ])
      },
      barWidth: '40%'
    }]
  }

  efficiencyChartInstance.setOption(option)
}

const initRegionChart = async () => {
  if (!regionChart.value) return

  if (regionChartInstance) {
    regionChartInstance.dispose()
  }
  regionChartInstance = echarts.init(regionChart.value)

  let chartData = { categories: [], events: [], completed: [] }
  try {
    const res = await statisticsApi.responderRanking()
    const rankings = Array.isArray(res.data) ? res.data : (Array.isArray(res) ? res : [])
    chartData = {
      categories: rankings.map(r => r.name || r.responderName || '未知'),
      events: rankings.map(r => r.totalTasks || r.taskCount || 0),
      completed: rankings.map(r => r.completedCount || r.completedTasks || r.completed || 0)
    }
  } catch (error) {
    chartData = { categories: [], events: [], completed: [] }
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 20, 40, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#fff' }
    },
    legend: {
      data: ['事件总数', '已完成'],
      textStyle: { color: 'rgba(255, 255, 255, 0.7)' },
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.categories || [],
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.3)' } },
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)' },
      splitLine: { lineStyle: { color: 'rgba(0, 212, 255, 0.1)' } }
    },
    series: [
      {
        name: '事件总数',
        type: 'bar',
        data: chartData.events || [],
        itemStyle: { color: '#00d4ff' }
      },
      {
        name: '已完成',
        type: 'bar',
        data: chartData.completed || [],
        itemStyle: { color: '#00ff88' }
      }
    ]
  }

  regionChartInstance.setOption(option)
}

const handleResize = () => {
  trendChartInstance?.resize()
  typeChartInstance?.resize()
  statusChartInstance?.resize()
  rumorGaugeInstance?.resize()
  efficiencyChartInstance?.resize()
  regionChartInstance?.resize()
}

const loadTableData = async () => {
  try {
    // 同时获取事件列表和谣言统计数据
    const [incidentRes, rumorRes, todayStatsRes] = await Promise.all([
      get('/admin/incident/list'),
      statisticsApi.rumorRatio(),
      statisticsApi.today()
    ])
    const incidents = incidentRes.data || incidentRes || []
    const rumorData = rumorRes.data || rumorRes || {}
    const todayStats = todayStatsRes.data || todayStatsRes || {}

    // Group by date - 重点展示今日数据
    const grouped = {}
    const today = new Date().toLocaleDateString('zh-CN')
    incidents.forEach(inc => {
      const date = inc.createTime ? new Date(inc.createTime).toLocaleDateString('zh-CN') : today
      if (!grouped[date]) {
        grouped[date] = { date, events: 0, completed: 0, processing: 0, rejected: 0, rumors: 0 }
      }
      grouped[date].events++
      if (inc.status === 4) grouped[date].completed++
      if (inc.status === 3 || inc.status === 2) grouped[date].processing++
      // 驳回列：管理员驳回的事件
      if (inc.status === 5) grouped[date].rejected++
    })

    // 谣言数：从后端获取消息级别的AI谣言数 + 驳回事件数
    const totalRumorCount = rumorData.rumor || 0
    if (grouped[today]) {
      grouped[today].rumors = totalRumorCount
    }

    tableData.value = Object.values(grouped)
      .sort((a, b) => {
        if (a.date === today) return -1
        if (b.date === today) return 1
        return new Date(b.date) - new Date(a.date)
      })
      .slice(0, 10)
      .map(row => ({
        ...row,
        avgTime: row.date === today ? (todayStats.avgProcessTime || '-') : '-',
        status: row.processing > row.events * 0.5 ? '繁忙' : '正常'
      }))
  } catch (error) {
    console.error('加载表格数据失败:', error)
  }
}

const refreshAll = () => {
  loadStatistics()
  loadTrendData()
  initTypeChart()
  initStatusChart()
  initRumorGauge()
  initEfficiencyChart()
  initRegionChart()
  loadTableData()
}

const handleNewIncident = () => {
  refreshAll()
}

const handleDataReset = () => {
  todayEvents.value = 0
  todayEventsTrend.value = 0
  avgProcessTime.value = '0分钟'
  rumorRatio.value = 0
  completedRate.value = 0
  rejectedCount.value = 0
  tableData.value = []
  // Clear and re-init charts
  refreshAll()
}

let refreshTimer = null

onMounted(() => {
  loadStatistics()
  loadTrendData()
  initTypeChart()
  initStatusChart()
  initRumorGauge()
  initEfficiencyChart()
  initRegionChart()
  loadTableData()
  window.addEventListener('resize', handleResize)
  
  // WebSocket real-time updates
  wsClient.on(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.on(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.on(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.on(MessageType.DATA_RESET, handleDataReset)
  
  // Auto-refresh every 15 seconds
  refreshTimer = setInterval(refreshAll, 15000)
})

onUnmounted(() => {
  trendChartInstance?.dispose()
  typeChartInstance?.dispose()
  statusChartInstance?.dispose()
  rumorGaugeInstance?.dispose()
  efficiencyChartInstance?.dispose()
  regionChartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
  
  wsClient.off(MessageType.NEW_INCIDENT, handleNewIncident)
  wsClient.off(MessageType.INCIDENT_UPDATE, handleNewIncident)
  wsClient.off(MessageType.NEW_MESSAGE, handleNewIncident)
  wsClient.off(MessageType.DATA_RESET, handleDataReset)
  
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.statistics-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
}

.stats-header {
  height: 60px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);
}

.stat-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-glow);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
}

.stat-icon.events {
  background: rgba(0, 212, 255, 0.2);
  color: var(--primary-color);
}

.stat-icon.time {
  background: rgba(0, 255, 136, 0.2);
  color: var(--success-color);
}

.stat-icon.rumor {
  background: rgba(255, 51, 102, 0.2);
  color: var(--danger-color);
}

.stat-icon.complete {
  background: rgba(102, 229, 255, 0.2);
  color: var(--primary-light);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
}

.stat-trend.up {
  color: var(--danger-color);
  background: rgba(255, 51, 102, 0.1);
}

.stat-trend.down {
  color: var(--success-color);
  background: rgba(0, 255, 136, 0.1);
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.chart-container {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.chart-container.wide {
  grid-column: span 2;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.chart {
  height: 300px;
}

.data-table {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 40px;
}

.data-table :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(0, 51, 102, 0.3);
  --el-table-row-hover-bg-color: rgba(0, 212, 255, 0.08);
  --el-table-border-color: rgba(0, 212, 255, 0.1);
  --el-table-text-color: rgba(255, 255, 255, 0.85);
  --el-table-header-text-color: #00d4ff;
  --el-fill-color-lighter: transparent;
}

.data-table :deep(.el-table__inner-wrapper),
.data-table :deep(.el-table__header-wrapper),
.data-table :deep(.el-table__body-wrapper),
.data-table :deep(.el-scrollbar__wrap),
.data-table :deep(.el-table__body-wrapper .el-scrollbar__view) {
  background: transparent !important;
}

.data-table :deep(.el-table__empty-block) {
  background: transparent !important;
  min-height: 60px;
}

.data-table :deep(.el-table__empty-text) {
  color: rgba(255, 255, 255, 0.4) !important;
  line-height: 60px;
}

.data-table :deep(.el-table td.el-table__cell),
.data-table :deep(.el-table th.el-table__cell) {
  background: transparent !important;
}

.data-table :deep(.el-table th.el-table__cell) {
  background: rgba(0, 51, 102, 0.3) !important;
  color: #00d4ff !important;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}
</style>
