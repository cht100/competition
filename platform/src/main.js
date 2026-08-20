/**
 * Vue 应用入口文件
 * 配置 Vue 插件、路由、状态管理、UI 组件库
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Element Plus 组件库
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// Element Plus 图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局样式
import './styles/global.css'

// 路由配置
import router from './router'

// 根组件
import App from './App.vue'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 注册路由
app.use(router)

// 注册 Element Plus
app.use(ElementPlus, {
  locale: zhCn,
})

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('全局错误:', err)
  console.error('错误信息:', info)
}

// 挂载应用
app.mount('#app')
