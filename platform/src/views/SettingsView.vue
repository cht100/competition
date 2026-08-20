<template>
  <div class="settings-container">
    <main class="settings-main">
      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">演示控制</h3>
        </div>
        <div class="demo-controls">
          <div class="demo-control-row">
            <div class="demo-info">
              <p class="setting-name">一键清除数据</p>
              <p class="setting-desc">清空所有事件、派单、聚类等处理数据，将消息恢复到初始状态</p>
            </div>
            <el-button 
              type="danger" 
              :loading="resetLoading" 
              @click="handleResetData"
            >
              <el-icon><Delete /></el-icon>
              清除所有数据
            </el-button>
          </div>
          <div class="demo-control-row">
            <div class="demo-info">
              <p class="setting-name">模拟数据推送</p>
              <p class="setting-desc">{{ dataSourceEnabled ? '正在推送模拟消息中...' : '开启后将自动推送模拟消息并实时处理' }}</p>
            </div>
            <el-switch v-model="dataSourceEnabled" @change="toggleDataSource" />
            <span class="status" :class="{ active: dataSourceEnabled }">
              {{ dataSourceEnabled ? '运行中' : '已停止' }}
            </span>
          </div>
        </div>
        <div class="demo-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>演示流程：清除数据 → 开启模拟推送 → 查看实时处理效果 → 停止推送 → 再次清除</span>
        </div>
      </div>

      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">数据源控制</h3>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <p class="setting-name">推送间隔（秒）</p>
            <p class="setting-desc">模拟消息的推送频率</p>
          </div>
          <el-input-number 
            v-model="pushInterval" 
            :min="1" 
            :max="60" 
            :disabled="!dataSourceEnabled"
            @change="updateInterval" 
          />
        </div>
      </div>

      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">阈值配置</h3>
        </div>
        <div class="setting-item slider-item">
          <div class="setting-info">
            <p class="setting-name">置信度阈值</p>
            <p class="setting-desc">低于此值的事件将进入待复核池</p>
          </div>
          <el-slider 
            v-model="confidenceThreshold" 
            :min="0" 
            :max="100" 
            show-input 
            @change="updateThreshold" 
          />
        </div>
        <div class="setting-item slider-item">
          <div class="setting-info">
            <p class="setting-name">聚类相似度阈值</p>
            <p class="setting-desc">超过此值的消息将合并到现有事件</p>
          </div>
          <el-slider 
            v-model="similarityThreshold" 
            :min="0" 
            :max="100" 
            :format-tooltip="formatPercent" 
            show-input 
            @change="updateSimilarity"
          />
        </div>
      </div>

      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">通知设置</h3>
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <p class="setting-name">新事件通知</p>
            <p class="setting-desc">当有新事件创建时发送通知</p>
          </div>
          <el-switch v-model="newEventNotify" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <p class="setting-name">高严重度事件提醒</p>
            <p class="setting-desc">严重程度为"严重"及以上时特别提醒</p>
          </div>
          <el-switch v-model="highSeverityAlert" />
        </div>
      </div>

      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">系统状态</h3>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">后端服务</span>
            <span class="value" :class="getStatusClass(backendStatus)">{{ backendStatus }}</span>
          </div>
          <div class="info-item">
            <span class="label">模型服务</span>
            <span class="value" :class="getStatusClass(modelStatus)">{{ modelStatus }}</span>
          </div>
          <div class="info-item">
            <span class="label">WebSocket</span>
            <span class="value" :class="getStatusClass(wsStatus)">{{ wsStatus }}</span>
          </div>
          <div class="info-item">
            <span class="label">数据库</span>
            <span class="value" :class="getStatusClass(dbStatus)">{{ dbStatus }}</span>
          </div>
        </div>
        <div class="refresh-btn">
          <el-button type="primary" plain @click="checkSystemStatus">
            <el-icon><Refresh /></el-icon>
            刷新状态
          </el-button>
        </div>
      </div>

      <div class="settings-section card">
        <div class="section-header">
          <h3 class="section-title">关于系统</h3>
        </div>
        <div class="about-info">
          <p><strong>系统名称：</strong>智瞰危局</p>
          <p><strong>版本号：</strong>v2.0.0</p>
          <p><strong>开发团队：</strong>智瞰危局开发组</p>
          <p><strong>更新日期：</strong>2026-03-30</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { settingsApi } from '@/api/settings'
import { useEventStore } from '@/stores/event'

const router = useRouter()
const eventStore = useEventStore()

const dataSourceEnabled = ref(false)
const pushInterval = ref(5)
const confidenceThreshold = ref(60)
const similarityThreshold = ref(75)
const newEventNotify = ref(true)
const highSeverityAlert = ref(true)
const backendStatus = ref('检测中')
const modelStatus = ref('检测中')
const wsStatus = ref('检测中')
const dbStatus = ref('检测中')
const resetLoading = ref(false)

let statusCheckTimer = null

const formatPercent = (val) => val + '%'

const goBack = () => {
  router.push('/dashboard')
}

const handleResetData = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将清空所有事件、派单、聚类等处理数据，将消息恢复到初始未处理状态。确认清除？',
      '确认清除所有数据',
      { confirmButtonText: '确认清除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  
  resetLoading.value = true
  try {
    await settingsApi.resetData()
    dataSourceEnabled.value = false
    // 清空前端本地状态
    eventStore.clearEvents()
    eventStore.clearAlerts()
    eventStore.clearMessageList()
    ElMessage.success('所有演示数据已清除，可以开始新一轮演示')
  } catch (error) {
    ElMessage.error('数据清除失败: ' + (error.message || '未知错误'))
  } finally {
    resetLoading.value = false
  }
}

const toggleDataSource = async (val) => {
  try {
    await settingsApi.toggleDataSource(val)
    ElMessage.success(val ? '数据源已启动' : '数据源已停止')
  } catch (error) {
    ElMessage.error('操作失败')
    dataSourceEnabled.value = !val
  }
}

const updateInterval = async (val) => {
  try {
    await settingsApi.updateInterval(val)
    ElMessage.success('推送间隔已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const updateThreshold = async (val) => {
  try {
    await settingsApi.updateThreshold(val)
    ElMessage.success(`置信度阈值已更新为 ${val}%`)
  } catch (error) {
    console.error('更新阈值失败:', error)
  }
}

const updateSimilarity = async (val) => {
  try {
    await settingsApi.updateSimilarity(val)
    ElMessage.success(`相似度阈值已更新为 ${val}%`)
  } catch (error) {
    console.error('更新相似度阈值失败:', error)
  }
}

const checkSystemStatus = async () => {
  try {
    const res = await settingsApi.checkStatus()
    const data = res.data || res
    backendStatus.value = data.backend ? '正常' : '异常'
    modelStatus.value = data.model ? '正常' : '异常'
    wsStatus.value = data.websocket ? '已连接' : '未连接'
    dbStatus.value = data.database ? '正常' : '异常'
  } catch (error) {
    backendStatus.value = '异常'
    modelStatus.value = '异常'
    wsStatus.value = '未连接'
    dbStatus.value = '异常'
  }
}

const getStatusClass = (status) => {
  if (status === '正常' || status === '已连接') return 'success'
  if (status === '异常' || status === '未连接') return 'error'
  return 'warning'
}

onMounted(() => {
  fetchSettings()
  checkSystemStatus()
  statusCheckTimer = setInterval(checkSystemStatus, 30000)
})

const fetchSettings = async () => {
  try {
    const res = await settingsApi.getSettings()
    const data = res.data || res || {}
    dataSourceEnabled.value = data.dataSourceEnabled || false
    pushInterval.value = data.pushInterval || 5
    confidenceThreshold.value = data.confidenceThreshold || 60
    similarityThreshold.value = data.similarityThreshold || 75
  } catch (error) {
    console.error('获取设置失败:', error)
  }
}

onUnmounted(() => {
  if (statusCheckTimer) {
    clearInterval(statusCheckTimer)
  }
})
</script>

<style scoped>
.settings-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
}

.settings-header {
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

.settings-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.settings-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
}

.section-header {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
  gap: 16px;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item.slider-item {
  flex-direction: column;
  align-items: flex-start;
}

.setting-item.slider-item .setting-info {
  margin-bottom: 12px;
}

.setting-item.slider-item .el-slider {
  width: 100%;
}

.setting-info {
  flex: 1;
}

.setting-name {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.setting-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.status {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  background: rgba(255, 68, 68, 0.2);
  color: #ff4444;
}

.status.active {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(0, 51, 102, 0.3);
  border-radius: 8px;
}

.info-item .label {
  color: var(--text-secondary);
  font-size: 14px;
}

.info-item .value {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.info-item .value.success {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.info-item .value.error {
  background: rgba(255, 68, 68, 0.2);
  color: #ff4444;
}

.info-item .value.warning {
  background: rgba(255, 204, 0, 0.2);
  color: #ffcc00;
}

.refresh-btn {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.about-info {
  line-height: 2;
}

.about-info p {
  color: var(--text-secondary);
}

.about-info strong {
  color: var(--text-primary);
}

.demo-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.demo-control-row {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(0, 51, 102, 0.2);
  border-radius: 8px;
  gap: 16px;
}

.demo-info {
  flex: 1;
}

.demo-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(0, 200, 255, 0.08);
  border: 1px solid rgba(0, 200, 255, 0.2);
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.demo-tip .el-icon {
  color: var(--primary-color);
  font-size: 16px;
  flex-shrink: 0;
}
</style>
