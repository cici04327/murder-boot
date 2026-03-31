<template>
  <div class="group-detail-page" v-loading="loading">
    <div class="detail-container" v-if="group">
      <!-- 剧本杀风格头部 -->
      <div class="mystery-header">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="mystery-badge">🎭 神秘剧本</div>
          <h1 class="script-name">{{ group.scriptName }}</h1>
          <div class="script-meta">
            <span class="meta-badge difficulty">{{ getDifficultyText(group.difficulty) }}</span>
            <span class="meta-badge players">{{ group.playerCount }}人本</span>
            <span class="meta-badge" v-if="group.genderRequirement">{{ group.genderRequirement }}</span>
            <span class="meta-badge newbie" v-if="group.newbieWelcome">🌟 新手友好</span>
          </div>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="main-content">
        <div class="content-left">
          <!-- 剧本信息卡片 -->
          <div class="info-card mystery-card">
            <div class="card-header">
              <span class="card-icon">📋</span>
              <span class="card-title">任务情报</span>
            </div>
            <div class="card-body">
              <div class="info-row">
                <div class="info-icon">🏠</div>
                <div class="info-content">
                  <span class="info-label">集合地点</span>
                  <span class="info-value">{{ group.storeName }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-icon">⏰</div>
                <div class="info-content">
                  <span class="info-label">开局时间</span>
                  <span class="info-value highlight">{{ formatPlayTime(group.playTime) }}</span>
                </div>
              </div>
              <div class="info-row" v-if="group.expireTime">
                <div class="info-icon">⌛</div>
                <div class="info-content">
                  <span class="info-label">最晚成团</span>
                  <span class="info-value" :class="{ highlight: isDeadlinePassed }">{{ formatPlayTime(group.expireTime) }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-icon">💰</div>
                <div class="info-content">
                  <span class="info-label">入场费用</span>
                  <span class="info-value price">¥{{ group.price }}/人</span>
                </div>
              </div>
            </div>
            <div class="mission-desc" v-if="group.description">
              <div class="desc-header">📝 车主留言</div>
              <p class="desc-text">{{ group.description }}</p>
            </div>
          </div>

          <!-- 神秘玩家座位图 -->
          <div class="info-card seats-card">
            <div class="card-header">
              <span class="card-icon">🪑</span>
              <span class="card-title">神秘座位</span>
              <span class="seats-count">{{ group.currentCount }}/{{ group.needCount }}</span>
            </div>
            <div class="card-body">
              <div class="seats-grid">
                <!-- 已入座的座位，根据每个成员的joinCount展示多个座位 -->
                <template v-for="(member, mIndex) in members" :key="member.id">
                  <div 
                    v-for="n in (member.joinCount || 1)" 
                    :key="member.id + '-' + n" 
                    class="seat-item taken"
                  >
                    <div class="seat-mask">🎭</div>
                    <div class="seat-name">{{ n === 1 ? (member.anonymousName || member.nickname) : '同行玩家' }}</div>
                    <div class="seat-badge" v-if="member.isCreator && n === 1">车主</div>
                  </div>
                </template>
                <!-- 空座位 -->
                <div 
                  v-for="n in emptySeats" 
                  :key="'empty-' + n" 
                  class="seat-item empty"
                >
                  <div class="seat-mask empty-mask">❓</div>
                  <div class="seat-name">等待入座</div>
                  <div class="seat-hint">神秘玩家</div>
                </div>
              </div>
              <div class="anonymous-notice">
                <el-icon><Warning /></el-icon>
                <span>为保护隐私，所有玩家信息均已匿名处理，开局后方可揭晓真实身份</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧操作区域 -->
        <div class="content-right">
          <div class="action-card">
            <div class="status-banner" :class="getStatusClass(group.status)">
              <span class="status-icon">{{ getStatusIcon(group.status) }}</span>
              <span class="status-text">{{ getStatusText(group.status) }}</span>
            </div>
            
            <div class="progress-visual">
              <div class="progress-ring">
                <svg viewBox="0 0 100 100">
                  <circle class="progress-bg" cx="50" cy="50" r="45" />
                  <circle 
                    class="progress-bar" 
                    cx="50" cy="50" r="45" 
                    :stroke-dasharray="`${(group.currentCount / group.needCount) * 283} 283`"
                  />
                </svg>
                <div class="progress-center">
                  <span class="progress-current">{{ group.currentCount }}</span>
                  <span class="progress-divider">/</span>
                  <span class="progress-total">{{ group.needCount }}</span>
                </div>
              </div>
              <p class="progress-hint" v-if="group.needCount > group.currentCount">
                还差 <strong>{{ group.needCount - group.currentCount }}</strong> 位神秘玩家
              </p>
              <p class="progress-hint success" v-else>
                ✨ 人员已齐，准备开局！
              </p>
            </div>

            <el-button 
              type="primary" 
              size="large" 
              class="join-btn" 
              :disabled="!canJoin" 
              @click="handleJoin"
            >
              <span class="btn-icon">🚗</span>
              {{ joinBtnText }}
            </el-button>
            
            <div class="action-tips">
              <p>🔒 参团后您的身份将被匿名保护</p>
              <p>📱 开局前会收到系统通知提醒</p>
            </div>
          </div>

          <!-- 温馨提示 -->
          <div class="tips-card">
            <div class="tips-header">💡 温馨提示</div>
            <ul class="tips-list">
              <li>请准时到达，迟到可能影响游戏体验</li>
              <li>如需取消请提前通知其他玩家</li>
              <li>保持良好心态，享受推理乐趣</li>
              <li>尊重每位玩家，共创美好回忆</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { h } from 'vue'
import { ElInputNumber } from 'element-plus'
import { getGroupDetail, joinGroup } from '@/api/group'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const group = ref(null)
const members = ref([])

const isDeadlinePassed = computed(() => {
  if (!group.value?.expireTime) return false
  return new Date(group.value.expireTime).getTime() <= Date.now()
})

const canJoin = computed(() => {
  if (!group.value) return false
  if (group.value.status !== 1) return false
  if (isDeadlinePassed.value) return false
  if (group.value.currentCount >= group.value.needCount) return false
  // 匿名模式下无法判断是否已加入，依靠后端校验
  return true
})

// 计算空座位数量
const emptySeats = computed(() => {
  if (!group.value) return 0
  return Math.max(0, group.value.needCount - group.value.currentCount)
})

const joinBtnText = computed(() => {
  if (!group.value) return '加载中...'
  if (group.value.status === 0 && isDeadlinePassed.value) return '已截止'
  if (group.value.status === 2) return '已成团'
  if (group.value.status === 0) return '已取消'
  if (group.value.status === 3) return '已结束'
  if (isDeadlinePassed.value) return '已截止'
  if (group.value.currentCount >= group.value.needCount) return '已满员'
  return '立即上车'
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getGroupDetail(route.params.id)
    if (res.code === 1 || res.code === 200) {
      group.value = res.data
      members.value = res.data.members || []
    }
  } catch (error) {
    console.error('加载拼单详情失败:', error)
    ElMessage.error('加载拼单详情失败')
    router.push('/group')
  } finally {
    loading.value = false
  }
}

const joinCount = ref(1)

const handleJoin = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 计算剩余空位
  const emptyCount = (group.value?.needCount || 0) - (group.value?.currentCount || 0)

  // 弹窗让用户选人数
  joinCount.value = 1
  try {
    await ElMessageBox({
      title: '🎭 确认上车',
      message: h('div', { style: 'padding: 8px 0' }, [
        h('p', { style: 'margin-bottom: 12px; color: #c0c4cc; font-size: 13px;' },
          `当前还差 ${emptyCount} 个座位，请选择您要带几人参加：`),
        h('div', { style: 'display: flex; align-items: center; gap: 12px;' }, [
          h('span', { style: 'color: #f2f6ff;' }, '参与人数'),
          h(ElInputNumber, {
            modelValue: joinCount.value,
            min: 1,
            max: emptyCount,
            size: 'small',
            'onUpdate:modelValue': (val) => { joinCount.value = val }
          })
        ]),
        h('p', { style: 'margin-top: 12px; color: #9fb3d9; font-size: 12px;' },
          '您的身份将被匿名保护，开局后方可揭晓真实身份。')
      ]),
      confirmButtonText: '确认上车',
      cancelButtonText: '再考虑下',
      type: 'info',
      customClass: 'join-group-dialog'
    })

    const res = await joinGroup(route.params.id, joinCount.value)
    if (res.code === 1 || res.code === 200) {
      ElNotification({
        title: '🎉 拼车成功',
        message: `<div style="line-height: 1.8;">
          <p><strong>剧本：</strong>${group.value.scriptName}</p>
          <p><strong>门店：</strong>${group.value.storeName}</p>
          <p><strong>开局时间：</strong>${formatPlayTime(group.value.playTime)}</p>
          <p><strong>参与人数：</strong>${joinCount.value} 人</p>
          <p style="color: #e6a23c; margin-top: 8px;">⏰ 请在预约时间前15分钟到达门店</p>
        </div>`,
        type: 'success',
        duration: 5000,
        dangerouslyUseHTMLString: true
      })
      loadDetail()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '上车失败，请重试')
    }
  }
}

const formatPlayTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth()+1}月${d.getDate()}日 ${weekDays[d.getDay()]} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

const getStatusText = (s) => {
  if (s === 0 && isDeadlinePassed.value) return '已截止'
  return ({ 0:'已取消', 1:'召集中', 2:'已成团', 3:'已结束' }[s] || '未知')
}
const getStatusClass = (s) => ({ 0:'cancelled', 1:'active', 2:'success', 3:'ended' }[s] || '')
const getStatusIcon = (s) => ({ 0:'❌', 1:'🔥', 2:'✅', 3:'🏁' }[s] || '❓')
const getDifficultyText = (d) => ({ 1:'🌱 新手', 2:'⭐ 进阶', 3:'🔥 烧脑', 4:'💀 硬核' }[d] || '⭐ 进阶')

onMounted(() => loadDetail())
</script>

<style scoped>
.group-detail-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

/* 神秘头部 */
.mystery-header {
  position: relative;
  padding: 50px 30px;
  border-radius: 20px;
  margin-bottom: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 10% 90%, rgba(192, 57, 43, 0.4) 0%, transparent 40%),
    radial-gradient(circle at 90% 10%, rgba(192, 57, 43, 0.3) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.02) 0%, transparent 50%);
  animation: mysteryPulse 5s ease-in-out infinite;
}

@keyframes mysteryPulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

.header-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.mystery-badge {
  display: inline-block;
  background: rgba(192, 57, 43, 0.6);
  padding: 8px 20px;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.script-name {
  font-size: 32px;
  color: #fff;
  margin: 0 0 20px;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.5);
  letter-spacing: 3px;
}

.script-meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-badge {
  background: rgba(255, 255, 255, 0.15);
  padding: 6px 16px;
  border-radius: 15px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  backdrop-filter: blur(4px);
}

.meta-badge.difficulty {
  background: rgba(192, 57, 43, 0.5);
}

.meta-badge.newbie {
  background: rgba(230, 162, 60, 0.5);
}

/* 主要内容区 */
.main-content {
  display: flex;
  gap: 24px;
}

.content-left {
  flex: 1;
}

.content-right {
  width: 320px;
  flex-shrink: 0;
}

/* 信息卡片 */
.info-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 16px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.card-icon {
  font-size: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.seats-count {
  margin-left: auto;
  background: rgba(192, 57, 43, 0.6);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.card-body {
  padding: 20px;
}

/* 信息行 */
.info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px dashed rgba(192, 57, 43, 0.2);
}

.info-row:last-child {
  border-bottom: none;
}

.info-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.2) 0%, rgba(192, 57, 43, 0.1) 100%);
  border-radius: 12px;
  font-size: 22px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.info-content {
  flex: 1;
}

.info-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 4px;
}

.info-value {
  font-size: 15px;
  color: #fff;
  font-weight: 500;
}

.info-value.highlight {
  color: #ff6b6b;
  font-weight: 600;
}

.info-value.price {
  font-size: 20px;
  color: #ff6b6b;
  font-weight: bold;
}

/* 任务描述 */
.mission-desc {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.15) 0%, rgba(192, 57, 43, 0.08) 100%);
  border-radius: 12px;
  border: 1px dashed rgba(192, 57, 43, 0.3);
}

.desc-header {
  font-size: 14px;
  font-weight: 600;
  color: #ff6b6b;
  margin-bottom: 10px;
}

.desc-text {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
}

/* 座位图 */
.seats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.seat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  border-radius: 12px;
  transition: all 0.3s;
  position: relative;
}

.seat-item.taken {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}

.seat-item.empty {
  background: rgba(35, 35, 60, 0.6);
  border: 2px dashed rgba(192, 57, 43, 0.3);
}

.seat-mask {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: rgba(192, 57, 43, 0.3);
  border-radius: 50%;
  margin-bottom: 10px;
}

.seat-item.taken .seat-mask {
  animation: maskFloat 3s ease-in-out infinite;
}

@keyframes maskFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.empty-mask {
  background: rgba(192, 57, 43, 0.2);
  font-size: 24px;
}

.seat-name {
  font-size: 12px;
  color: #fff;
  text-align: center;
  line-height: 1.4;
}

.seat-item.empty .seat-name {
  color: rgba(255, 255, 255, 0.5);
}

.seat-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #16213e;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
}

.seat-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 4px;
}

.anonymous-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(230, 162, 60, 0.15);
  border-radius: 10px;
  font-size: 12px;
  color: #e6a23c;
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.anonymous-notice .el-icon {
  font-size: 16px;
}

/* 操作卡片 */
.action-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  margin-bottom: 20px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.status-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  font-size: 18px;
  font-weight: 600;
}

.status-banner.active {
  background: linear-gradient(135deg, #16213e 0%, #55141d 100%);
  color: #fff;
  animation: statusGlow 2s ease-in-out infinite;
}

@keyframes statusGlow {
  0%, 100% { box-shadow: inset 0 0 20px rgba(255, 255, 255, 0.1); }
  50% { box-shadow: inset 0 0 30px rgba(255, 255, 255, 0.2); }
}

.status-banner.success {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: #fff;
}

.status-banner.cancelled,
.status-banner.ended {
  background: rgba(60, 60, 80, 0.8);
  color: rgba(255, 255, 255, 0.5);
}

.status-icon {
  font-size: 24px;
}

/* 进度环 */
.progress-visual {
  padding: 30px 20px;
  text-align: center;
}

.progress-ring {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 20px;
}

.progress-ring svg {
  transform: rotate(-90deg);
  width: 100%;
  height: 100%;
}

.progress-bg {
  fill: none;
  stroke: rgba(192, 57, 43, 0.2);
  stroke-width: 8;
}

.progress-bar {
  fill: none;
  stroke: #ff6b6b;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

.progress-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.progress-current {
  font-size: 36px;
  font-weight: bold;
  color: #ff6b6b;
}

.progress-divider {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0 2px;
}

.progress-total {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.7);
}

.progress-hint {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

.progress-hint strong {
  color: #ff6b6b;
  font-size: 18px;
}

.progress-hint.success {
  color: #52c41a;
  font-weight: 500;
}

/* 加入按钮 */
.join-btn {
  width: calc(100% - 40px);
  margin: 0 20px 20px;
  height: 50px;
  font-size: 18px;
  background: linear-gradient(135deg, #16213e 0%, #55141d 100%);
  border: none;
  border-radius: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.join-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #a00000 0%, #d42e4a 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(192, 57, 43, 0.4);
}

.join-btn:disabled {
  background: #ccc;
}

.btn-icon {
  font-size: 20px;
}

.action-tips {
  padding: 0 20px 20px;
}

.action-tips p {
  margin: 0 0 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 提示卡片 */
.tips-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(192, 57, 43, 0.2);
}

.tips-header {
  font-size: 15px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 2;
}

/* 响应式 */
@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }
  
  .content-right {
    width: 100%;
  }
  
  .script-name {
    font-size: 24px;
  }
  
  .seats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
