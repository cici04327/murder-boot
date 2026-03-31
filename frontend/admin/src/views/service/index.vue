<template>
  <div class="service-center">
    <div class="service-layout">
      <!-- 左侧：会话列表 -->
      <div class="session-list-panel">
        <div class="panel-header">
          <span class="panel-title">客服中心</span>
          <el-badge :value="waitingCount" :hidden="waitingCount === 0" type="danger">
            <el-tag type="warning" size="small">等待中 {{ waitingCount }}</el-tag>
          </el-badge>
        </div>

        <!-- 状态筛选 -->
        <div class="status-tabs">
          <el-radio-group v-model="filterStatus" size="small" @change="loadSessions">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="0">等待接入</el-radio-button>
            <el-radio-button :value="1">进行中</el-radio-button>
            <el-radio-button :value="2">已结束</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 会话列表 -->
        <div class="session-list" v-loading="listLoading">
          <div
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
            :class="{
              active: currentSession?.id === session.id,
              waiting: session.status === 0,
              ongoing: session.status === 1,
              closed: session.status === 2
            }"
            @click="selectSession(session)"
          >
            <el-avatar :size="36" class="session-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="session-info">
              <div class="session-name">{{ session.userName || '用户' + session.userId }}</div>
              <div class="session-question">{{ session.initialQuestion || '咨询中...' }}</div>
            </div>
            <div class="session-meta">
              <el-tag
                size="small"
                :type="statusType(session.status)"
              >{{ statusLabel(session.status) }}</el-tag>
              <div class="session-time">{{ formatTime(session.createTime) }}</div>
            </div>
          </div>
          <el-empty v-if="sessions.length === 0 && !listLoading" description="暂无会话" />
        </div>

        <!-- 分页 -->
        <el-pagination
          v-if="total > pageSize"
          small
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="page"
          @current-change="loadSessions"
          style="padding: 8px; justify-content: center;"
        />
      </div>

      <!-- 右侧：聊天窗口 -->
      <div class="chat-panel">
        <template v-if="currentSession">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="chat-user-info">
              <el-avatar :size="32"><el-icon><User /></el-icon></el-avatar>
              <div>
                <div class="chat-user-name">{{ currentSession.userName || '用户' + currentSession.userId }}</div>
                <div class="chat-session-id">会话 #{{ currentSession.id }}</div>
              </div>
            </div>
            <div class="chat-actions">
              <!-- 接入按钮（等待中时显示） -->
              <el-button
                v-if="currentSession.status === 0"
                type="primary"
                size="small"
                @click="handleAccept"
                :loading="accepting"
              >
                <el-icon><Phone /></el-icon>
                接入会话
              </el-button>
              <!-- 结束按钮（进行中时显示） -->
              <el-button
                v-if="currentSession.status === 1"
                type="danger"
                size="small"
                @click="handleClose"
              >
                <el-icon><CircleClose /></el-icon>
                结束会话
              </el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="chat-messages" ref="messagesRef">
            <template v-for="(msg, index) in messages" :key="msg.id">
              <!-- 时间分隔线 -->
              <div
                v-if="index === 0 || msg.createTime - messages[index-1].createTime > 5 * 60 * 1000"
                class="time-divider"
              >{{ formatMsgTime(msg.createTime) }}</div>

              <!-- 系统消息 -->
              <div v-if="msg.senderType === 'system'" class="system-message">
                {{ msg.content }}
              </div>

              <!-- 普通消息 -->
              <div v-else :class="['message-row', msg.senderType === 'admin' ? 'right' : 'left']">
                <el-avatar :size="28" class="msg-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="msg-bubble">{{ msg.content }}</div>
              </div>
            </template>
            <div v-if="messages.length === 0" class="empty-messages">
              <el-empty description="暂无消息" :image-size="60" />
            </div>
          </div>

          <!-- 输入框（进行中才显示） -->
          <div v-if="currentSession.status === 1" class="chat-input-area">
            <!-- 快捷话语栏 -->
            <div class="quick-replies-bar">
              <div class="quick-replies-scroll">
                <el-tag
                  v-for="(phrase, idx) in quickPhrases"
                  :key="idx"
                  class="quick-phrase-tag"
                  @click="insertPhrase(phrase.text)"
                  size="small"
                  type="info"
                  effect="plain"
                  style="cursor:pointer"
                >{{ phrase.label }}</el-tag>
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click="showQuickPhraseDialog = true"
                  style="flex-shrink:0; font-size:12px;"
                >
                  <el-icon style="margin-right:2px"><Setting /></el-icon>管理
                </el-button>
              </div>
            </div>

            <!-- 输入行 -->
            <div class="chat-input">
              <el-input
                v-model="inputMsg"
                placeholder="输入回复内容... (Enter发送，Shift+Enter换行)"
                type="textarea"
                :rows="2"
                resize="none"
                @keydown.enter.exact.prevent="handleSend"
                @keydown.shift.enter.exact="() => {}"
              />
              <el-button type="primary" @click="handleSend" :disabled="!inputMsg.trim()">
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </div>

          <!-- 已结束时的评价显示 -->

          <div v-if="currentSession.status === 2 && currentSession.rating" class="session-rating">
            <span>用户评价：</span>
            <el-rate :model-value="currentSession.rating" disabled />
            <span class="rating-comment">{{ currentSession.ratingComment }}</span>
          </div>
        </template>

        <!-- 未选中会话 -->
        <div v-else class="no-session">
          <el-empty description="请选择一个会话" :image-size="80">
            <template #image>
              <el-icon style="font-size: 80px; color: #ddd;"><Service /></el-icon>
            </template>
          </el-empty>
        </div>
      </div>
    </div>
  </div>

  <!-- 快捷话语管理弹窗 -->
  <el-dialog v-model="showQuickPhraseDialog" title="快捷话语管理" width="560px" :close-on-click-modal="false">
    <div class="quick-phrase-manager">
      <!-- 新增表单 -->
      <div class="quick-phrase-add">
        <el-input v-model="newPhraseLabel" placeholder="话语标签（如：欢迎语）" style="width:140px" size="small" />
        <el-input v-model="newPhraseText" placeholder="话语内容（点击后填入输入框）" style="flex:1" size="small" />
        <el-button type="primary" size="small" @click="addPhrase" :disabled="!newPhraseLabel.trim() || !newPhraseText.trim()">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </div>

      <!-- 已有话语列表 -->
      <div class="quick-phrase-list">
        <div
          v-for="(phrase, idx) in quickPhrases"
          :key="idx"
          class="quick-phrase-item"
        >
          <div class="phrase-item-content">
            <el-input v-model="phrase.label" size="small" style="width:130px" placeholder="标签" />
            <el-input v-model="phrase.text" size="small" style="flex:1" placeholder="内容" />
          </div>
          <el-button type="danger" size="small" link @click="removePhrase(idx)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-empty v-if="quickPhrases.length === 0" description="暂无快捷话语" :image-size="48" />
      </div>
    </div>
    <template #footer>
      <el-button @click="showQuickPhraseDialog = false">取消</el-button>
      <el-button type="primary" @click="savePhrases">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Phone, CircleClose, Promotion, Service, Setting, Plus, Delete } from '@element-plus/icons-vue'
import { listSessions, acceptSession, sendAdminMessage, closeSessionByAdmin, getSessionMessages, countWaiting } from '@/api/service'

const listLoading = ref(false)
const sessions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterStatus = ref(null)
const currentSession = ref(null)
const messages = ref([])
const inputMsg = ref('')
const accepting = ref(false)
const waitingCount = ref(0)
const messagesRef = ref(null)

// 快捷话语
const STORAGE_KEY = 'admin-quick-phrases'
const defaultPhrases = [
  { label: '欢迎语', text: '您好，欢迎联系客服，请问有什么可以帮助您？😊' },
  { label: '稍等', text: '请稍等，我马上为您查询。' },
  { label: '感谢', text: '感谢您的耐心等待！' },
  { label: '无法预约', text: '非常抱歉，该场次暂时无法预约，建议您选择其他时间段。' },
  { label: '退款说明', text: '退款将在3-5个工作日内原路返回，请您耐心等待。' },
  { label: '满意结束', text: '感谢您的咨询，祝您游戏愉快！如有其他问题欢迎随时联系。👋' },
]
const loadPhrases = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : defaultPhrases
  } catch { return defaultPhrases }
}
const quickPhrases = ref(loadPhrases())
const showQuickPhraseDialog = ref(false)
const newPhraseLabel = ref('')
const newPhraseText = ref('')

const insertPhrase = (text) => {
  inputMsg.value = text
}

const addPhrase = () => {
  if (!newPhraseLabel.value.trim() || !newPhraseText.value.trim()) return
  quickPhrases.value.push({ label: newPhraseLabel.value.trim(), text: newPhraseText.value.trim() })
  newPhraseLabel.value = ''
  newPhraseText.value = ''
}

const removePhrase = (idx) => {
  quickPhrases.value.splice(idx, 1)
}

const savePhrases = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(quickPhrases.value))
  showQuickPhraseDialog.value = false
  ElMessage.success('快捷话语已保存')
}

// WebSocket
let ws = null
let wsHeartbeat = null      // 心跳定时器引用
let wsReconnectTimer = null // 重连定时器引用
let isUnmounted = false     // 组件是否已卸载

const adminUserStr = localStorage.getItem('admin-user')
const adminUserObj = adminUserStr ? JSON.parse(adminUserStr) : null
const adminId = adminUserObj?.id || adminUserObj?.userId || null

// 播放新消息提示音
const playNotifySound = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = ctx.createOscillator()
    const gainNode = ctx.createGain()
    oscillator.connect(gainNode)
    gainNode.connect(ctx.destination)
    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(880, ctx.currentTime)
    gainNode.gain.setValueAtTime(0.3, ctx.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5)
    oscillator.start(ctx.currentTime)
    oscillator.stop(ctx.currentTime + 0.5)
  } catch (e) { /* 浏览器不支持则忽略 */ }
}

const connectWs = () => {
  if (!adminId || isUnmounted) return

  // 清除旧连接，防止叠加
  if (ws && ws.readyState !== WebSocket.CLOSED) {
    ws.onclose = null // 先移除 onclose，避免触发重连
    ws.close()
  }
  // 清除旧心跳
  if (wsHeartbeat) { clearInterval(wsHeartbeat); wsHeartbeat = null }

  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${location.host}/api/ws/service?adminId=${adminId}&role=admin`
  ws = new WebSocket(wsUrl)

  ws.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_service_session') {
        // 新会话进来，刷新列表并更新等待数
        loadSessions()
        loadWaitingCount()
        playNotifySound()
        ElMessage({
          message: `📞 用户「${data.userName || '用户'}」发起了人工客服请求，请及时处理！`,
          type: 'warning',
          duration: 6000,
          showClose: true
        })
      } else if (data.type === 'service_message') {
        // 收到新消息，如果是当前会话则追加（用 == 避免数字/字符串类型不一致）
        if (currentSession.value && data.sessionId == currentSession.value.id) {
          messages.value.push({
            id: Date.now(),
            senderType: data.senderType,
            senderId: data.senderId,
            content: data.content,
            msgType: data.msgType,
            createTime: new Date(data.createTime).getTime()
          })
          scrollToBottom()
          // 用户消息才播放提示音
          if (data.senderType === 'user') playNotifySound()
        } else {
          // 其他会话有新消息，刷新列表
          loadSessions()
          if (data.senderType === 'user') playNotifySound()
        }
      } else if (data.type === 'session_closed') {
        loadSessions()
        if (currentSession.value && data.sessionId == currentSession.value.id) {
          currentSession.value.status = 2
        }
      }
    } catch (e) { /* 忽略解析错误 */ }
  }

  ws.onopen = () => {
    // 连接成功后启动心跳
    if (wsHeartbeat) clearInterval(wsHeartbeat)
    wsHeartbeat = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send('ping')
      } else {
        clearInterval(wsHeartbeat)
        wsHeartbeat = null
      }
    }, 30000)
  }

  ws.onclose = () => {
    if (wsHeartbeat) { clearInterval(wsHeartbeat); wsHeartbeat = null }
    // 组件未卸载则5秒后重连
    if (!isUnmounted) {
      wsReconnectTimer = setTimeout(connectWs, 5000)
    }
  }
}

const loadSessions = async () => {
  listLoading.value = true
  try {
    const res = await listSessions({ status: filterStatus.value, page: page.value, pageSize: pageSize.value })
    if (res.code === 1 || res.code === 200) {
      sessions.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    listLoading.value = false
  }
}

const loadWaitingCount = async () => {
  const res = await countWaiting()
  if (res.code === 1 || res.code === 200) {
    waitingCount.value = res.data || 0
  }
}

const selectSession = async (session) => {
  currentSession.value = session
  messages.value = []
  const res = await getSessionMessages(session.id)
  if (res.code === 1 || res.code === 200) {
    messages.value = (res.data || []).map(m => ({
      ...m,
      createTime: new Date(m.createTime).getTime()
    }))
  }
  scrollToBottom()
}

const handleAccept = async () => {
  accepting.value = true
  try {
    await acceptSession(currentSession.value.id)
    currentSession.value.status = 1
    ElMessage.success('已接入会话')
    loadSessions()
  } finally {
    accepting.value = false
  }
}

const handleClose = async () => {
  await ElMessageBox.confirm('确认结束此会话？', '提示', { type: 'warning' })
  await closeSessionByAdmin(currentSession.value.id)
  currentSession.value.status = 2
  ElMessage.success('会话已结束')
  loadSessions()
}

const handleSend = async () => {
  if (!inputMsg.value.trim()) return
  const content = inputMsg.value.trim()
  inputMsg.value = ''
  try {
    const res = await sendAdminMessage(currentSession.value.id, { content })
    if (res.code === 1 || res.code === 200) {
      messages.value.push({
        id: res.data?.id || Date.now(),
        senderType: 'admin',
        content,
        msgType: 'text',
        createTime: Date.now()
      })
      scrollToBottom()
    }
  } catch (e) {
    inputMsg.value = content
    ElMessage.error('发送失败，请重试')
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const statusLabel = (status) => {
  return { 0: '等待接入', 1: '进行中', 2: '已结束', 3: '已超时' }[status] || '未知'
}

const statusType = (status) => {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const formatMsgTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

let waitingCountTimer = null

onMounted(() => {
  loadSessions()
  loadWaitingCount()
  connectWs()
  // 每30秒刷新等待数
  waitingCountTimer = setInterval(loadWaitingCount, 30000)
})

onUnmounted(() => {
  isUnmounted = true
  // 清除等待数刷新定时器
  if (waitingCountTimer) { clearInterval(waitingCountTimer); waitingCountTimer = null }
  // 清除重连定时器
  if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null }
  // 清除心跳定时器
  if (wsHeartbeat) { clearInterval(wsHeartbeat); wsHeartbeat = null }
  // 关闭WebSocket
  if (ws) { ws.onclose = null; ws.close(); ws = null }
})
</script>

<style scoped>
.service-center {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.service-layout {
  display: flex;
  height: 100%;
  gap: 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* 左侧会话列表 */
.session-list-panel {
  width: 300px;
  min-width: 260px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.status-tabs {
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
  transition: background 0.2s;
}

.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; }
.session-item.waiting { border-left: 3px solid #e6a23c; }
.session-item.ongoing { border-left: 3px solid #67c23a; }
.session-item.closed { border-left: 3px solid #c0c4cc; opacity: 0.7; }

.session-info {
  flex: 1;
  min-width: 0;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-question {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.session-time {
  font-size: 11px;
  color: #c0c4cc;
}

/* 右侧聊天窗口 */
.chat-panel {
  flex: 1;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.chat-session-id {
  font-size: 12px;
  color: #909399;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-divider {
  text-align: center;
  font-size: 11px;
  color: #c0c4cc;
  position: relative;
  margin: 4px 0;
}

.system-message {
  text-align: center;
  font-size: 12px;
  color: #909399;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 6px 16px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-row.right {
  flex-direction: row-reverse;
}

.msg-bubble {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  background: #fff;
  color: #303133;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.message-row.right .msg-bubble {
  background: #409eff;
  color: #fff;
}

.chat-input-area {
  background: #fff;
  border-top: 1px solid #ebeef5;
}

/* 快捷话语栏 */
.quick-replies-bar {
  padding: 8px 16px 6px;
  border-bottom: 1px solid #f0f2f5;
}

.quick-replies-scroll {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  flex-wrap: nowrap;
  scrollbar-width: none;
}

.quick-replies-scroll::-webkit-scrollbar {
  display: none;
}

.quick-phrase-tag {
  flex-shrink: 0;
  transition: all 0.2s;
}

.quick-phrase-tag:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.chat-input {
  padding: 12px 16px 16px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input .el-textarea {
  flex: 1;
}

/* 快捷话语管理弹窗 */
.quick-phrase-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-phrase-add {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quick-phrase-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.quick-phrase-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: box-shadow 0.2s;
}

.quick-phrase-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.phrase-item-content {
  display: flex;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.session-rating {
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.rating-comment {
  color: #909399;
  font-style: italic;
}

.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-messages {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
