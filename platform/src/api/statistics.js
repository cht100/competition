/**
 * 统计数据 API
 */
import { get } from './request'

export const statisticsApi = {
  /** 获取今日统计 */
  today() {
    return get('/admin/statistics/today')
  },

  /** 获取事件趋势 */
  eventTrend(params = {}) {
    return get('/admin/statistics/trend', params)
  },

  /** 获取灾种分布统计 */
  disasterTypes() {
    return get('/admin/statistics/disaster-types')
  },

  /** 获取处置效率 */
  efficiency() {
    return get('/admin/statistics/efficiency')
  },

  /** 获取谣言占比 */
  rumorRatio() {
    return get('/admin/statistics/rumor-ratio')
  },

  /** 获取执勤人员排名 */
  responderRanking() {
    return get('/admin/statistics/responder-ranking')
  }
}

export default statisticsApi
