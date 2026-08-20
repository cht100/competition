/**
 * 用户状态管理 (Pinia Store)
 * 管理用户登录状态、用户信息、认证 token
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { post, get } from '../api/request'
import { wsClient } from '../api/websocket'

export const useUserStore = defineStore('user', () => {
  // 根据当前URL判断角色上下文
  function detectRole() {
    return window.location.pathname.startsWith('/responder') ? 'responder' : 'admin'
  }
  const initRole = detectRole()

  // 状态 - 使用角色前缀的localStorage键
  const token = ref(localStorage.getItem(`${initRole}_access_token`) || '')
  const refreshToken = ref(localStorage.getItem(`${initRole}_refresh_token`) || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(`${initRole}_user_info`) || 'null'))
  const role = ref(initRole)
  const isLoggedIn = computed(() => {
    const r = detectRole()
    return !!localStorage.getItem(`${r}_access_token`)
  })

  /**
   * 用户登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   * @param {string} loginRole - 登录角色 admin/responder
   * @returns {Promise} 登录结果
   */
  async function login(username, password, loginRole = 'admin') {
    try {
      const endpoint = loginRole === 'responder' ? '/responder/login' : '/admin/login'
      const result = await post(endpoint, { username, password })
      
      // 后端返回 Result 结构: { code, msg, data }
      if (result.code !== 1) {
        throw new Error(result.msg || '登录失败')
      }
      
      const response = result.data
      
      // 保存认证信息
      token.value = response.token
      role.value = loginRole
      userInfo.value = { id: response.id, username: response.userName, role: loginRole }
      
      // 持久化到 localStorage（使用角色前缀，支持同时登录）
      localStorage.setItem(`${loginRole}_access_token`, response.token)
      localStorage.setItem(`${loginRole}_user_info`, JSON.stringify(userInfo.value))
      
      // 建立 WebSocket 连接
      wsClient.connect(response.token)
      
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 用户登出
   */
  async function logout() {
    try {
      // 登出：仅清理本地状态（后端无 /auth/logout 端点）
    } catch (error) {
      // ignore
    } finally {
      // 清除当前角色的状态
      const currentRole = role.value || detectRole()
      token.value = ''
      refreshToken.value = ''
      userInfo.value = null
      
      // 清除当前角色的 localStorage
      localStorage.removeItem(`${currentRole}_access_token`)
      localStorage.removeItem(`${currentRole}_refresh_token`)
      localStorage.removeItem(`${currentRole}_user_info`)
      
      // 仅当另一个角色也未登录时才断开 WebSocket
      const otherRole = currentRole === 'admin' ? 'responder' : 'admin'
      if (!localStorage.getItem(`${otherRole}_access_token`)) {
        wsClient.disconnect()
      }
    }
  }

  /**
   * 刷新 token
   * @returns {Promise} 刷新结果
   */
  async function refreshAccessToken() {
    // 后端未实现 /auth/refresh 端点，直接触发重新登录
    logout()
    throw new Error('Token已过期，请重新登录')
  }

  /**
   * 获取用户信息
   * @returns {Promise} 用户信息
   */
  async function fetchUserInfo() {
    // 后端未实现 /user/info 端点，从 localStorage 恢复
    const currentRole = role.value || detectRole()
    const stored = localStorage.getItem(`${currentRole}_user_info`)
    if (stored) {
      try { userInfo.value = JSON.parse(stored) } catch {}
    }
    return userInfo.value
  }

  /**
   * 更新用户信息
   * @param {object} data - 更新数据
   * @returns {Promise} 更新结果
   */
  async function updateUserInfo(data) {
    try {
      const response = await post('/user/update', data)
      userInfo.value = { ...userInfo.value, ...response }
      localStorage.setItem('user_info', JSON.stringify(userInfo.value))
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 修改密码
   * @param {string} oldPassword - 旧密码
   * @param {string} newPassword - 新密码
   * @returns {Promise} 修改结果
   */
  async function changePassword(oldPassword, newPassword) {
    try {
      const response = await post('/user/change-password', {
        old_password: oldPassword,
        new_password: newPassword
      })
      return response
    } catch (error) {
      throw error
    }
  }

  /**
   * 检查登录状态
   * @returns {boolean} 是否已登录
   */
  function checkAuth() {
    return !!token.value && !!userInfo.value
  }

  return {
    // 状态
    token,
    refreshToken,
    userInfo,
    role,
    isLoggedIn,
    // 方法
    login,
    logout,
    refreshAccessToken,
    fetchUserInfo,
    updateUserInfo,
    changePassword,
    checkAuth
  }
})
