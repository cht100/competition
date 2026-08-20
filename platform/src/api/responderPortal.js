/**
 * 执勤人员端 API（志愿者/执勤人员自己使用的接口）
 */
import { get, post, put } from './request'

export const responderPortalApi = {
  /** 执勤人员登录 */
  login(data) {
    return post('/responder/login', data)
  },

  /** 获取当前执勤人员信息 */
  getCurrentInfo() {
    return get('/responder/info')
  },

  /** 更新个人信息 */
  updateSelfInfo(data) {
    return put('/responder/info', data)
  },

  /** 上线 */
  goOnline() {
    return post('/responder/online')
  },

  /** 下线 */
  goOffline() {
    return post('/responder/offline')
  },

  /** 获取可接受的事件 */
  getAvailableTasks() {
    return get('/responder/incidents/available')
  },

  /** 获取当前处理的事件 */
  getCurrentTask() {
    return get('/responder/incidents/current')
  },

  /** 接受任务 */
  acceptTask(data) {
    return post('/responder/incidents/accept', data)
  },

  /** 提交反馈 */
  submitFeedback(data) {
    return post('/responder/incidents/feedback', data)
  },

  /** 获取待响应的派单任务 */
  getPendingDispatchTasks() {
    return get('/responder/dispatch/tasks/pending')
  },

  /** 获取所有派单任务 */
  getDispatchTasks() {
    return get('/responder/dispatch/tasks')
  },

  /** 响应派单（接受/拒绝） */
  respondDispatch(data) {
    return post('/responder/dispatch/respond', data)
  },

  /** 获取执勤人员关联的所有事件（地图用） */
  getMyIncidents() {
    return get('/responder/incidents/my-incidents')
  }
}

export default responderPortalApi
