/**
 * 消息相关 API
 */
import { get } from './request'

export const messageApi = {
  /** 获取消息详情 */
  getById(id) {
    return get(`/admin/message/${id}`)
  },

  /** 获取消息列表 */
  list() {
    return get('/admin/message/list')
  },

  /** 获取最近消息 */
  listRecent(limit = 50) {
    return get('/admin/message/recent', { limit })
  }
}

export default messageApi
