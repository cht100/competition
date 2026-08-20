<template>
  <div class="app-container">
    <!-- 路由视图 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { useEventStore } from './stores/event'
import { wsClient } from './api/websocket'

const router = useRouter()
const userStore = useUserStore()
const eventStore = useEventStore()

onMounted(async () => {
  // 使用任意可用的token建立WebSocket连接（支持同时登录）
  const adminToken = localStorage.getItem('admin_access_token')
  const responderToken = localStorage.getItem('responder_access_token')
  const connectToken = adminToken || responderToken
  
  if (connectToken) {
    eventStore.initWebSocketListeners()
    try {
      await wsClient.connect(connectToken)
      console.log('WebSocket 重新连接成功')
    } catch (error) {
      console.error('WebSocket 连接失败:', error)
    }
  }
})

onUnmounted(() => {
  wsClient.removeAllListeners()
})
</script>

<style>
/* 页面切换过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
