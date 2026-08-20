/**
 * WebSocket 连接封装
 * 支持自动重连、心跳检测、消息队列、多种消息类型
 */

import { ElMessage } from 'element-plus'

// WebSocket 配置
const WS_CONFIG = {
  // WebSocket 服务器地址
  url: import.meta.env.VITE_WS_URL || '/ws/message',
  // 重连间隔（毫秒）
  reconnectInterval: 3000,
  // 最大重连次数
  maxReconnectAttempts: 5,
  // 心跳间隔（毫秒）
  heartbeatInterval: 30000,
  // 心跳消息
  heartbeatMessage: JSON.stringify({ type: 'ping' })
}

function resolveWsUrl(rawUrl) {
  if (rawUrl.startsWith('ws://') || rawUrl.startsWith('wss://')) {
    return rawUrl
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const path = rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`
  return `${protocol}//${window.location.host}${path}`
}

/**
 * WebSocket 消息类型枚举
 * 定义系统支持的所有消息类型
 */
export const MessageType = {
  // 心跳消息
  PING: 'ping',
  PONG: 'pong',
  // 新消息推送 - 原始消息入库后推送
  NEW_MESSAGE: 'new_message',
  // 新事件创建 - 消息聚类后创建新事件
  NEW_INCIDENT: 'new_incident',
  // 事件状态更新 - 事件状态变化时推送
  INCIDENT_UPDATE: 'incident_update',
  // 人员状态变化 - 执勤人员状态变化
  RESPONDER_UPDATE: 'responder_update',
  // 任务状态更新 - 派单任务状态变化
  TASK_UPDATE: 'task_update',
  // 系统通知 - 系统级通知消息
  SYSTEM_NOTICE: 'system_notice',
  // 告警消息 - 高严重度事件告警
  ALERT: 'alert',
  // 数据重置 - 清空所有本地数据
  DATA_RESET: 'data_reset'
}

/**
 * WebSocket 客户端类
 * 提供完整的 WebSocket 连接管理功能
 */
class WebSocketClient {
  constructor() {
    // WebSocket 实例
    this.ws = null
    // 连接状态
    this.connected = false
    // 重连计数器
    this.reconnectAttempts = 0
    // 心跳定时器
    this.heartbeatTimer = null
    // 消息队列（连接断开时暂存消息）
    this.messageQueue = []
    // 事件监听器
    this.listeners = new Map()
    // 手动关闭标志
    this.manualClose = false
  }

  /**
   * 建立 WebSocket 连接
   * @param {string} token - JWT 认证 token
   * @returns {Promise} 连接结果
   */
  connect(token) {
    return new Promise((resolve, reject) => {
      try {
        // 关闭已有连接，防止重复连接导致消息重复推送
        if (this.ws) {
          this.manualClose = true
          try { this.ws.close() } catch (e) { /* ignore */ }
          this.ws = null
          this.connected = false
          this.stopHeartbeat()
        }

        // 构建带 token 的 WebSocket URL
        const wsBaseUrl = resolveWsUrl(WS_CONFIG.url)
        const wsUrl = `${wsBaseUrl}?token=${token}`
        
        // 创建 WebSocket 实例
        this.ws = new WebSocket(wsUrl)
        this.manualClose = false

        // 连接成功
        this.ws.onopen = () => {
          console.log('WebSocket 连接成功')
          this.connected = true
          this.reconnectAttempts = 0
          
          // 启动心跳检测
          this.startHeartbeat()
          
          // 发送队列中的消息
          this.flushMessageQueue()
          
          // 触发连接事件
          this.emit('connected')
          
          resolve()
        }

        // 接收消息
        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            
            // 处理心跳响应
            if (data.type === MessageType.PONG) {
              return
            }
            
            // 根据消息类型分发处理
            this.dispatchMessage(data)
          } catch (error) {
            console.error('解析 WebSocket 消息失败:', error)
          }
        }

        // 连接关闭
        this.ws.onclose = (event) => {
          console.log('WebSocket 连接关闭:', event.code, event.reason)
          this.connected = false
          
          // 停止心跳
          this.stopHeartbeat()
          
          // 触发断开事件
          this.emit('disconnected', { code: event.code, reason: event.reason })
          
          // 非手动关闭时尝试重连
          if (!this.manualClose) {
            this.reconnect(token)
          }
        }

        // 连接错误
        this.ws.onerror = (error) => {
          console.error('WebSocket 错误:', error)
          this.emit('error', error)
          reject(error)
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  /**
   * 消息分发处理
   * 根据消息类型触发对应的事件监听器
   * @param {object} data - 消息数据
   */
  dispatchMessage(data) {
    // 触发通用消息事件
    this.emit('message', data)
    
    // 根据消息类型触发特定事件
    if (data.type) {
      switch (data.type) {
        case MessageType.NEW_MESSAGE:
          // 新消息推送
          this.emit(MessageType.NEW_MESSAGE, data.payload || data)
          this.emit('newMessage', data.payload || data)
          break
          
        case MessageType.NEW_INCIDENT:
          // 新事件创建
          this.emit(MessageType.NEW_INCIDENT, data.payload || data)
          this.emit('newIncident', data.payload || data)
          // 显示通知
          this.showIncidentNotification(data.payload || data)
          break
          
        case MessageType.INCIDENT_UPDATE:
          // 事件状态更新
          this.emit(MessageType.INCIDENT_UPDATE, data.payload || data)
          this.emit('incidentUpdate', data.payload || data)
          break
          
        case MessageType.RESPONDER_UPDATE:
          // 人员状态变化
          this.emit(MessageType.RESPONDER_UPDATE, data.payload || data)
          this.emit('responderUpdate', data.payload || data)
          break
          
        case MessageType.TASK_UPDATE:
          // 任务状态更新
          this.emit(MessageType.TASK_UPDATE, data.payload || data)
          this.emit('taskUpdate', data.payload || data)
          break
          
        case MessageType.SYSTEM_NOTICE:
          // 系统通知
          this.emit(MessageType.SYSTEM_NOTICE, data.payload || data)
          this.emit('systemNotice', data.payload || data)
          if (data.message) {
            ElMessage.info(data.message)
          }
          break
          
        case MessageType.ALERT:
          // 告警消息
          this.emit(MessageType.ALERT, data.payload || data)
          this.emit('alert', data.payload || data)
          this.showAlertNotification(data.payload || data)
          break

        case MessageType.DATA_RESET:
          // 数据重置通知
          this.emit(MessageType.DATA_RESET, data.payload || data)
          this.emit('dataReset', data.payload || data)
          break
          
        default:
          // 其他消息类型
          this.emit(data.type, data)
      }
    }
  }

  /**
   * 显示事件通知
   * @param {object} incident - 事件数据
   */
  showIncidentNotification(incident) {
    if (!incident) return
    
    const title = incident.summary || incident.disasterType || '新事件'
    const severity = incident.severity || 0
    const severityText = ['轻微', '一般', '严重', '特别严重'][severity] || '未知'
    
    // 高严重度事件使用警告样式
    if (severity >= 2) {
      ElMessage.warning(`【${severityText}】${title}`)
    } else {
      ElMessage.info(`新事件：${title}`)
    }
  }

  /**
   * 显示告警通知
   * @param {object} alert - 告警数据
   */
  showAlertNotification(alert) {
    if (!alert) return
    
    const message = alert.message || alert.summary || '系统告警'
    ElMessage.error(`告警：${message}`)
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.manualClose = true
    this.stopHeartbeat()
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    this.connected = false
  }

  /**
   * 重新连接
   * @param {string} token - JWT 认证 token
   */
  reconnect(token) {
    if (this.reconnectAttempts >= WS_CONFIG.maxReconnectAttempts) {
      console.error('WebSocket 重连次数已达上限')
      ElMessage.error('实时连接断开，请刷新页面重试')
      return
    }

    this.reconnectAttempts++
    console.log(`尝试第 ${this.reconnectAttempts} 次重连...`)

    setTimeout(() => {
      this.connect(token).catch((error) => {
        console.error('重连失败:', error)
      })
    }, WS_CONFIG.reconnectInterval)
  }

  /**
   * 发送消息
   * @param {object} data - 消息数据
   * @returns {boolean} 发送结果
   */
  send(data) {
    const message = typeof data === 'string' ? data : JSON.stringify(data)

    if (this.connected && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(message)
      return true
    } else {
      // 连接断开时，将消息加入队列
      console.warn('WebSocket 未连接，消息已加入队列')
      this.messageQueue.push(message)
      return false
    }
  }

  /**
   * 启动心跳检测
   */
  startHeartbeat() {
    this.stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      if (this.connected && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(WS_CONFIG.heartbeatMessage)
      }
    }, WS_CONFIG.heartbeatInterval)
  }

  /**
   * 停止心跳检测
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 发送队列中的消息
   */
  flushMessageQueue() {
    while (this.messageQueue.length > 0 && this.connected) {
      const message = this.messageQueue.shift()
      this.ws.send(message)
    }
  }

  /**
   * 添加事件监听器
   * @param {string} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  /**
   * 移除事件监听器
   * @param {string} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    if (!this.listeners.has(event)) return
    
    const callbacks = this.listeners.get(event)
    const index = callbacks.indexOf(callback)
    
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  /**
   * 移除所有事件监听器
   * @param {string} event - 事件名称（可选，不传则清除所有）
   */
  removeAllListeners(event) {
    if (event) {
      this.listeners.delete(event)
    } else {
      this.listeners.clear()
    }
  }

  /**
   * 触发事件
   * @param {string} event - 事件名称
   * @param {any} data - 事件数据
   */
  emit(event, data) {
    if (!this.listeners.has(event)) return
    
    this.listeners.get(event).forEach((callback) => {
      try {
        callback(data)
      } catch (error) {
        console.error(`事件处理错误 [${event}]:`, error)
      }
    })
  }

  /**
   * 获取连接状态
   * @returns {boolean} 连接状态
   */
  isConnected() {
    return this.connected && this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 导出单例实例
export const wsClient = new WebSocketClient()

// 导出类，供需要多实例的场景使用
export default WebSocketClient
