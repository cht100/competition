/**
 * 事件相关 API - 与后端 IncidentController 对接
 */
import { get, post } from './request'

export const incidentApi = {
  /** 获取事件列表 */
  list(params = {}) {
    if (params.status !== undefined && params.status !== '') {
      return get(`/admin/incident/list/status/${params.status}`)
    }
    if (params.type) {
      return get(`/admin/incident/list/type/${params.type}`)
    }
    return get('/admin/incident/list')
  },

  /** 获取事件基本信息 */
  getById(id) {
    return get(`/admin/incident/${id}`)
  },

  /** 获取事件详情（含证据链） */
  detail(id) {
    return get(`/admin/incident/${id}/detail`)
  },

  /** 确认事件 */
  confirm(id) {
    return post(`/admin/incident/confirm/${id}`)
  },

  /** 驳回事件 */
  reject(id, reason = '') {
    return post(`/admin/incident/reject/${id}`, { reason })
  },

  /** 获取事件统计 */
  getStatistics() {
    return get('/admin/incident/statistics')
  }
}

export default incidentApi
