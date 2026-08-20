<template>
  <div class="responder-profile">
    <div class="profile-card">
      <div class="avatar-section">
        <div class="avatar-ring">
          <div class="avatar">
            <span>{{ responderInfo?.name?.charAt(0) || '?' }}</span>
          </div>
        </div>
        <h2 class="name">{{ responderInfo?.name || '加载中...' }}</h2>
        <div class="online-status" :class="{ online: isOnline }">
          <span class="dot"></span>
          {{ isOnline ? '在线执勤中' : '离线' }}
        </div>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <label>联系电话</label>
          <span>{{ responderInfo?.phone || '-' }}</span>
        </div>
        <div class="info-item">
          <label>擅长领域</label>
          <span>{{ responderInfo?.position || '-' }}</span>
        </div>
        <div class="info-item">
          <label>当前位置</label>
          <span>{{ locationText }}</span>
        </div>
        <div class="info-item">
          <label>账号ID</label>
          <span>{{ responderInfo?.userId || '-' }}</span>
        </div>
      </div>

      <div class="action-section">
        <el-button :type="isOnline ? 'danger' : 'success'" size="large" @click="toggleOnline" :loading="toggling">
          {{ isOnline ? '下线' : '上线执勤' }}
        </el-button>
      </div>
    </div>

    <!-- 执勤统计 -->
    <div class="stats-card">
      <h3>
        <span class="header-dot"></span>
        执勤统计
      </h3>
      <div class="stats-grid">
        <div class="stat-item" v-for="s in stats" :key="s.label">
          <div class="stat-number" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-desc">{{ s.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { responderPortalApi } from '../../api/responderPortal'

const responderInfo = ref(null)
const isOnline = ref(false)
const toggling = ref(false)

const locationText = computed(() => {
  const r = responderInfo.value
  if (!r || (r.lat == null && r.lng == null)) return '未设置'
  return `${Number(r.lat)?.toFixed(4)}, ${Number(r.lng)?.toFixed(4)}`
})

const stats = computed(() => [
  { label: '累计接受任务', value: responderInfo.value?.totalTasks || 0, color: '#00d4ff' },
  { label: '成功完成', value: responderInfo.value?.completedTasks || 0, color: '#00ff88' },
  { label: '平均响应(分)', value: responderInfo.value?.avgResponseTime || '-', color: '#ffaa00' },
  { label: '评价得分', value: responderInfo.value?.rating || '-', color: '#7c3aed' }
])

async function fetchInfo() {
  try {
    const res = await responderPortalApi.getCurrentInfo()
    if (res.code === 1 && res.data) {
      responderInfo.value = res.data
      isOnline.value = res.data.status === 1
    }
  } catch {}
}

async function toggleOnline() {
  toggling.value = true
  try {
    const api = isOnline.value ? responderPortalApi.goOffline : responderPortalApi.goOnline
    const res = await api()
    if (res.code === 1) {
      isOnline.value = !isOnline.value
      ElMessage.success(isOnline.value ? '已上线执勤' : '已离线')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {} finally { toggling.value = false }
}

onMounted(fetchInfo)
</script>

<style scoped>
.responder-profile {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card, .stats-card {
  padding: 32px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 28px;
}

.avatar-ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: conic-gradient(#00d4ff, #00ff88, #7c3aed, #00d4ff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  animation: spin 8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #0f1335;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: #00d4ff;
}

.name {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: rgba(224, 230, 237, 0.5);
}

.online-status.online {
  color: #00ff88;
}

.online-status .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(224, 230, 237, 0.3);
}

.online-status.online .dot {
  background: #00ff88;
  box-shadow: 0 0 8px #00ff88;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 28px;
}

.info-item {
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.info-item label {
  display: block;
  font-size: 12px;
  color: rgba(224, 230, 237, 0.4);
  margin-bottom: 6px;
}

.info-item span {
  font-size: 15px;
  color: #e0e6ed;
  font-weight: 500;
}

.action-section {
  display: flex;
  justify-content: center;
}

.stats-card h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 20px;
}

.header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00d4ff;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 10px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
}

.stat-desc {
  font-size: 12px;
  color: rgba(224, 230, 237, 0.5);
  margin-top: 4px;
}
</style>
