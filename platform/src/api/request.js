/**
 * Axios HTTP 请求封装
 * 支持 JWT 认证、请求拦截、响应拦截、错误处理
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
  // 基础 URL，从环境变量获取，默认为本地开发服务器地址
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  // 请求超时时间（毫秒）
  timeout: 30000,
  // 请求头配置
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取角色对应的 JWT token（支持同时登录）
    const adminToken = localStorage.getItem('admin_access_token')
    const responderToken = localStorage.getItem('responder_access_token')
    
    // 根据请求路径设置对应的token头
    if (adminToken) {
      config.headers.token = adminToken
    }
    if (responderToken) {
      config.headers['responder-token'] = responderToken
    }
    
    return config
  },
  (error) => {
    // 请求错误处理
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 成功响应，直接返回数据
    return response.data
  },
  (error) => {
    // 错误响应处理
    const { response } = error
    
    if (response) {
      // 根据状态码处理不同错误
      switch (response.status) {
        case 401:
          // 未授权，token 过期或无效
          ElMessage.error('登录已过期，请重新登录')
          // 清除当前角色的认证信息
          const isResponderPath = window.location.pathname.startsWith('/responder')
          const rolePrefix = isResponderPath ? 'responder' : 'admin'
          localStorage.removeItem(`${rolePrefix}_access_token`)
          localStorage.removeItem(`${rolePrefix}_refresh_token`)
          localStorage.removeItem(`${rolePrefix}_user_info`)
          // 跳转到登录页（执勤人员携带 role 参数）
          window.location.href = isResponderPath ? '/login?role=responder' : '/login'
          break
        case 403:
          // 禁止访问
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          // 资源不存在
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          // 服务器错误
          ElMessage.error('服务器内部错误，请稍后重试')
          break
        default:
          // 其他错误
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      // 网络错误或请求被取消
      if (error.message.includes('timeout')) {
        ElMessage.error('请求超时，请检查网络连接')
      } else if (error.message.includes('Network Error')) {
        ElMessage.error('网络错误，请检查网络连接')
      } else {
        ElMessage.error('请求失败: ' + error.message)
      }
    }
    
    return Promise.reject(error)
  }
)

/**
 * GET 请求
 * @param {string} url - 请求地址
 * @param {object} params - 查询参数
 * @param {object} config - 额外配置
 * @returns {Promise} 请求结果
 */
export const get = (url, params = {}, config = {}) => {
  return request.get(url, { params, ...config })
}

/**
 * POST 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @param {object} config - 额外配置
 * @returns {Promise} 请求结果
 */
export const post = (url, data = {}, config = {}) => {
  return request.post(url, data, config)
}

/**
 * PUT 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @param {object} config - 额外配置
 * @returns {Promise} 请求结果
 */
export const put = (url, data = {}, config = {}) => {
  return request.put(url, data, config)
}

/**
 * DELETE 请求
 * @param {string} url - 请求地址
 * @param {object} params - 查询参数
 * @param {object} config - 额外配置
 * @returns {Promise} 请求结果
 */
export const del = (url, params = {}, config = {}) => {
  return request.delete(url, { params, ...config })
}

/**
 * 文件上传
 * @param {string} url - 上传地址
 * @param {File} file - 文件对象
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise} 上传结果
 */
export const upload = (url, file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return request.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

// 导出 axios 实例，供特殊场景使用
export default request
