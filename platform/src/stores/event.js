/**
 * 事件状态管理 (Pinia Store)
 * 管理系统事件、告警信息、实时数据
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get } from '../api/request'
import { wsClient } from '../api/websocket'

export const useEventStore = defineStore('event', () => {
  // 事件列表
  const events = ref([])
  // 未读事件数量
  const unreadCount = ref(0)
  // 告警列表
  const alerts = ref([])
  // 实时统计数据
  const statistics = ref({
    totalEvents: 0,
    todayEvents: 0,
    alertCount: 0,
    onlineDevices: 0
  })
  // 事件类型统计
  const eventTypeStats = ref({})
  // 加载状态
  const loading = ref(false)
  // WebSocket 连接状态
  const wsConnected = ref(wsClient.isConnected())
  // 消息流列表（用于事件流监控页面）
  const messageList = ref([])
  // 置信度阈值
  const confidenceThreshold = ref(60)

  /**
   * 初始化 WebSocket 事件监听
   */
  let listenersInitialized = false
  function initWebSocketListeners() {
    if (listenersInitialized) return
    listenersInitialized = true

    // 监听新消息（全局持久监听，不随页面切换移除）
    wsClient.on('new_message', (data) => {
      let msg = data
      if (data.payload) {
        msg = { ...data.payload, ...data }
      }
      if (msg.aiAnalysis) {
        try {
          const analysis = typeof msg.aiAnalysis === 'string' ? JSON.parse(msg.aiAnalysis) : msg.aiAnalysis
          msg.disasterType = analysis.disaster_type
          msg.infoType = analysis.info_type
          msg.severity = analysis.severity || 0
          msg.confidence = analysis.confidence || 0
        } catch (e) {
          console.error('解析AI分析结果失败:', e)
        }
      }
      addMessage(msg)
    })

    // 监听新事件（使用正确的消息类型）
    wsClient.on('new_incident', (data) => {
      addEvent(data.payload || data)
    })

    // 监听告警
    wsClient.on('alert', (data) => {
      addAlert(data)
    })

    // 监听统计数据更新
    wsClient.on('statistics_update', (data) => {
      updateStatistics(data)
    })

    // 监听连接状态
    wsClient.on('connected', () => {
      wsConnected.value = true
      console.log('WebSocket 已连接')
    })

    wsClient.on('disconnected', () => {
      wsConnected.value = false
      console.log('WebSocket 已断开')
    })

    // 监听事件状态更新
    wsClient.on('incident_update', (data) => {
      const update = data.payload || data
      const index = events.value.findIndex(e => e.id === update.id)
      if (index !== -1) {
        const updated = [...events.value]
        updated[index] = { ...updated[index], status: update.newStatus }
        events.value = updated
      }
    })

    // 监听任务状态更新
    wsClient.on('task_update', (data) => {
      // task_update 可能影响事件状态，刷新统计
      console.log('收到任务更新通知:', data)
    })

    // 监听执勤人员状态更新
    wsClient.on('responder_update', (data) => {
      console.log('收到执勤人员更新通知:', data)
    })

    // 监听数据重置
    wsClient.on('data_reset', () => {
      console.log('收到数据重置通知，清空本地状态')
      resetAllLocalState()
    })
  }

  /**
   * 获取事件列表
   * @param {object} params - 查询参数
   * @returns {Promise} 事件列表
   */
  async function fetchEvents(params = {}) {
    loading.value = true
    try {
      const response = await get('/admin/incident/list', params)
      events.value = response.data || response
      return response
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取告警列表
   * @param {object} params - 查询参数
   * @returns {Promise} 告警列表
   */
  async function fetchAlerts(params = {}) {
    loading.value = true
    try {
      const response = await get('/admin/incident/list', params)
      alerts.value = response.data || response
      return response
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取统计数据
   * @returns {Promise} 统计数据
   */
  async function fetchStatistics() {
    try {
      const response = await get('/admin/statistics/today')
      statistics.value = response.data || response
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 获取事件类型统计
   * @returns {Promise} 统计数据
   */
  async function fetchEventTypeStats() {
    try {
      const response = await get('/admin/incident/statistics')
      eventTypeStats.value = response.data || response
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 添加新事件
   * @param {object} event - 事件数据
   */
  function addEvent(event) {
    // 添加到列表开头
    events.value.unshift(event)
    // 更新统计
    statistics.value.totalEvents++
    statistics.value.todayEvents++
    // 更新未读数
    unreadCount.value++
  }

  /**
   * 添加新告警
   * @param {object} alert - 告警数据
   */
  function addAlert(alert) {
    // 添加到列表开头
    alerts.value.unshift(alert)
    // 更新统计
    statistics.value.alertCount++
  }

  /**
   * 更新统计数据
   * @param {object} data - 统计数据
   */
  function updateStatistics(data) {
    statistics.value = { ...statistics.value, ...data }
  }

  /**
   * 标记事件为已读
   * @param {string} eventId - 事件ID
   */
  function markEventAsRead(eventId) {
    const event = events.value.find(e => e.id === eventId)
    if (event && !event.read) {
      event.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  /**
   * 标记所有事件为已读
   */
  function markAllAsRead() {
    events.value.forEach(event => {
      event.read = true
    })
    unreadCount.value = 0
  }

  /**
   * 清除所有事件
   */
  function clearEvents() {
    events.value = []
    unreadCount.value = 0
  }

  /**
   * 清除所有告警
   */
  function clearAlerts() {
    alerts.value = []
    statistics.value.alertCount = 0
  }

  /**
   * 确认告警
   * @param {string} alertId - 告警ID
   */
  function acknowledgeAlert(alertId) {
    const alert = alerts.value.find(a => a.id === alertId)
    if (alert) {
      alert.acknowledged = true
      alert.acknowledgedAt = new Date().toISOString()
    }
  }

  /**
   * 添加消息到消息流列表
   * @param {object} message - 消息数据
   */
  function addMessage(message) {
    // 去重：防止 WebSocket 重复连接导致同一消息被添加两次
    if (message.id && messageList.value.some(m => m.id === message.id)) {
      return
    }
    messageList.value.unshift(message)
    if (messageList.value.length > 200) {
      messageList.value = messageList.value.slice(0, 200)
    }
  }

  /**
   * 清空消息流列表
   */
  function clearMessageList() {
    messageList.value = []
  }

  /**
   * 设置置信度阈值
   * @param {number} threshold - 阈值
   */
  function setConfidenceThreshold(threshold) {
    confidenceThreshold.value = threshold
  }

  /**
   * 重置所有本地状态（收到 data_reset WebSocket 通知时调用）
   */
  function resetAllLocalState() {
    events.value = []
    unreadCount.value = 0
    alerts.value = []
    messageList.value = []
    statistics.value = {
      totalEvents: 0,
      todayEvents: 0,
      alertCount: 0,
      onlineDevices: 0
    }
    eventTypeStats.value = {}
  }

  return {
    // 状态
    events,
    unreadCount,
    alerts,
    statistics,
    eventTypeStats,
    loading,
    wsConnected,
    messageList,
    confidenceThreshold,
    // 方法
    initWebSocketListeners,
    fetchEvents,
    fetchAlerts,
    fetchStatistics,
    fetchEventTypeStats,
    addEvent,
    addAlert,
    updateStatistics,
    markEventAsRead,
    markAllAsRead,
    clearEvents,
    clearAlerts,
    acknowledgeAlert,
    addMessage,
    clearMessageList,
    setConfidenceThreshold,
    resetAllLocalState
  }
})
