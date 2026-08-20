/**
 * 执勤人员/派单相关 API
 */
import { get, post, put } from './request'

export const responderApi = {
  /** 获取人员列表 */
  list() {
    return get('/admin/responder/list')
  },

  /** 获取空闲人员列表 */
  listIdle() {
    return get('/admin/responder/idle')
  },

  /** 更新人员状态 */
  updateStatus(id, status) {
    return put(`/admin/responder/status/${id}`, null, { params: { status } })
  },

  /** 获取各状态人员数量 */
  countByStatus() {
    return get('/admin/responder/count')
  },

  /** 智能派单 */
  smartDispatch(data) {
    return post('/admin/dispatch/smart', data)
  },

  /** 查询事件的派单任务 */
  getDispatchTasks(incidentId) {
    return get(`/admin/dispatch/tasks/incident/${incidentId}`)
  },

  /** 取消派单 */
  cancelDispatch(taskId) {
    return post(`/admin/dispatch/tasks/${taskId}/cancel`)
  },

  /** 重新派单 */
  redispatch(taskId) {
    return post(`/admin/dispatch/tasks/${taskId}/redispatch`)
  }
}

export default responderApi
