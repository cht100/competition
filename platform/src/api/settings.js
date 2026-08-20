/**
 * 系统设置 API
 */
import { get, post } from './request'

export const settingsApi = {
  /** 切换模拟数据源 */
  toggleDataSource(enabled) {
    return post('/admin/settings/simulate/toggle', { enabled })
  },

  /** 更新推送间隔 */
  updateInterval(interval) {
    return post('/admin/settings/simulate/interval', { interval })
  },

  /** 更新置信度阈值 */
  updateThreshold(threshold) {
    return post('/admin/settings/threshold', { threshold })
  },

  /** 更新相似度阈值 */
  updateSimilarity(threshold) {
    return post('/admin/settings/similarity', { threshold })
  },

  /** 检查系统状态 */
  checkStatus() {
    return get('/admin/settings/status')
  },

  /** 获取当前设置 */
  getSettings() {
    return get('/admin/settings/current')
  },

  /** 启动模拟 */
  startSimulate() {
    return post('/admin/simulate/start')
  },

  /** 停止模拟 */
  stopSimulate() {
    return post('/admin/simulate/stop')
  },

  /** 一键重置所有演示数据 */
  resetData() {
    return post('/admin/settings/reset-data')
  }
}

export default settingsApi
