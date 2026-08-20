/**
 * Vue Router 路由配置
 * 包含页面路由定义、路由守卫、权限控制
 */

import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/DashboardLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '态势总览', requiresAuth: true }
      },
      {
        path: 'stream',
        name: 'EventStream',
        component: () => import('../views/EventStreamView.vue'),
        meta: { title: '事件流监控', requiresAuth: true }
      },
      {
        path: 'clustering',
        name: 'Clustering',
        component: () => import('../views/ClusteringView.vue'),
        meta: { title: '智能聚类', requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/StatisticsView.vue'),
        meta: { title: '统计看板', requiresAuth: true }
      },
      {
        path: 'map',
        name: 'Map',
        component: () => import('../views/MapView.vue'),
        meta: { title: '地图视图', requiresAuth: true }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/SettingsView.vue'),
        meta: { title: '系统设置', requiresAuth: true }
      }
    ]
  },
  // 执勤人员端路由
  {
    path: '/responder',
    component: () => import('../views/responder/ResponderLayout.vue'),
    meta: { requiresAuth: true, role: 'responder' },
    children: [
      {
        path: '',
        redirect: '/responder/dashboard'
      },
      {
        path: 'dashboard',
        name: 'ResponderDashboard',
        component: () => import('../views/responder/ResponderDashboard.vue'),
        meta: { title: '任务中心', requiresAuth: true, role: 'responder' }
      },
      {
        path: 'tasks',
        name: 'ResponderTasks',
        component: () => import('../views/responder/ResponderTasks.vue'),
        meta: { title: '我的任务', requiresAuth: true, role: 'responder' }
      },
      {
        path: 'map',
        name: 'ResponderMap',
        component: () => import('../views/responder/ResponderMapView.vue'),
        meta: { title: '态势地图', requiresAuth: true, role: 'responder' }
      },
      {
        path: 'profile',
        name: 'ResponderProfile',
        component: () => import('../views/responder/ResponderProfile.vue'),
        meta: { title: '个人中心', requiresAuth: true, role: 'responder' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFoundView.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  }
]

// 创建路由实例
const router = createRouter({
  // 使用 HTML5 History 模式
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  // 滚动行为
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    } else {
      return { top: 0, behavior: 'smooth' }
    }
  }
})

// 全局前置守卫
router.beforeEach((to, from) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 智瞰危局` : '智瞰危局'
  
  // 获取角色对应的认证 token（支持同时登录）
  const adminToken = localStorage.getItem('admin_access_token')
  const responderToken = localStorage.getItem('responder_access_token')
  
  // 判断是否需要认证
  if (to.meta.requiresAuth) {
    if (to.meta.role === 'responder') {
      // 执勤人员路由 - 检查执勤人员token
      if (responderToken) {
        return true
      } else {
        return { path: '/login', query: { redirect: to.fullPath, role: 'responder' } }
      }
    } else {
      // 管理员路由 - 检查管理员token
      if (adminToken) {
        return true
      } else {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
  } else {
    // 登录页不自动跳转，允许用户自由选择角色登录
    return true
  }
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 可以在这里添加页面访问统计等逻辑
  console.log(`路由跳转: ${from.path} -> ${to.path}`)
})

// 导出路由实例
export default router
