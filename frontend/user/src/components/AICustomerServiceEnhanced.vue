<template>
  <div class="ai-customer-service">
    <!-- 悬浮球 -->
    <transition name="bounce">
      <div 
        v-if="!isOpen" 
        class="service-bubble" 
        @click="toggleChat"
        :class="{ 'has-unread': unreadCount > 0 }"
      >
        <el-icon class="service-icon"><Service /></el-icon>
        <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</div>
        <div class="bubble-tip">AI客服</div>
      </div>
    </transition>

    <!-- 聊天窗口 -->
    <transition name="slide-up">
      <div v-if="isOpen" class="chat-window">

        <!-- ========== 人工客服模式 ========== -->
        <template v-if="isHumanMode">
          <div class="chat-header">
            <div class="header-left">
              <el-button text @click="backToAI" style="color:white; padding: 0;">
                <el-icon><ArrowDown style="transform: rotate(90deg)" /></el-icon>
              </el-button>
              <div class="header-info">
                <div class="header-title">人工客服</div>
                <div class="header-status">
                  <span class="status-dot" :style="{ background: humanConnected ? '#67c23a' : '#e6a23c' }"></span>
                  <span>{{ humanConnected ? '客服已接入' : '等待客服接入...' }}</span>
                </div>
              </div>
            </div>
            <div class="header-right">
              <el-button text @click="endHumanSession" title="结束会话" style="color:white;">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 人工消息列表 -->
          <div class="chat-messages" ref="humanMessagesRef">
            <template v-for="(msg, index) in humanMessages" :key="msg.id">
              <div v-if="msg.senderType === 'system'" class="human-system-msg">
                {{ msg.content }}
              </div>
              <div v-else :class="['message', msg.senderType === 'user' ? 'user' : 'bot']">
                <el-avatar v-if="msg.senderType !== 'user'" :size="32" class="message-avatar">
                  <el-icon><Service /></el-icon>
                </el-avatar>
                <div class="message-content">
                  <div class="message-bubble">{{ msg.content }}</div>
                </div>
                <el-avatar v-if="msg.senderType === 'user'" :size="32" class="message-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
            </template>
            <div v-if="humanMessages.length === 0" style="text-align:center; color:#c0c4cc; padding: 40px 0;">
              等待客服接入中...
            </div>
          </div>

          <!-- 人工输入框 -->
          <div class="chat-input">
            <el-input
              v-model="humanInput"
              placeholder="输入消息..."
              @keydown.enter.prevent="sendHumanMessage"
              :disabled="!humanConnected"
            >
              <template #suffix>
                <el-button text :icon="Promotion" @click="sendHumanMessage" :disabled="!humanInput.trim() || !humanConnected">
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="chat-footer">
            <el-text size="small" type="info">人工客服服务时间 9:00-22:00</el-text>
          </div>
        </template>

        <!-- ========== AI 模式 ========== -->
        <template v-else>
        <!-- 头部 -->
        <div class="chat-header">
          <div class="header-left">
            <el-avatar :size="32" src="/default-avatar.jpg">
              <el-icon><Service /></el-icon>
            </el-avatar>
            <div class="header-info">
              <div class="header-title">AI客服小剧</div>
              <div class="header-status">
                <span class="status-dot"></span>
                <span>在线</span>
              </div>
            </div>
          </div>
          <div class="header-right">
            <el-button text @click="clearChat" title="清空对话">
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button text @click="toggleChat" title="关闭">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer">
          <template v-for="(msg, index) in messages" :key="msg.id">
            <!-- 时间分隔线：与上一条消息间隔超过5分钟才显示 -->
            <div
              v-if="index === 0 || msg.timestamp - messages[index - 1].timestamp > 5 * 60 * 1000"
              class="time-divider"
            >
              {{ formatTime(msg.timestamp) }}
            </div>

            <div class="message-wrapper">
              <div :class="['message', msg.type]">
                <el-avatar v-if="msg.type === 'bot'" :size="32" class="message-avatar">
                  <el-icon><Service /></el-icon>
                </el-avatar>
                
                <div class="message-content">
                  <div class="message-bubble" v-html="msg.content"></div>
                  
                  <!-- 快捷建议 -->
                  <div v-if="msg.suggestions && msg.suggestions.length > 0" class="message-suggestions">
                    <el-tag
                      v-for="(suggestion, i) in msg.suggestions"
                      :key="i"
                      size="small"
                      type="info"
                      effect="plain"
                      @click="askQuestion(suggestion)"
                      style="cursor: pointer; margin: 4px;"
                    >
                      {{ suggestion }}
                    </el-tag>
                  </div>
                  
                  <!-- 操作按钮 -->
                  <div v-if="msg.actions && msg.actions.length > 0" class="message-actions">
                    <el-button
                      v-for="(action, i) in msg.actions"
                      :key="i"
                      size="small"
                      type="primary"
                      @click="handleAction(action)"
                    >
                      {{ action.label }}
                    </el-button>
                  </div>
                  
                  <!-- 满意度评价 -->
                  <div v-if="msg.showFeedback" class="message-feedback">
                    <span style="margin-right: 8px;">这个回答有帮助吗？</span>
                    <el-button size="small" @click="submitFeedback(msg.id, 5, '很有帮助')" :disabled="msg.feedbackSubmitted">
                      👍 有帮助
                    </el-button>
                    <el-button size="small" @click="submitFeedback(msg.id, 1, '没有帮助')" :disabled="msg.feedbackSubmitted">
                      👎 没帮助
                    </el-button>
                  </div>

                  <!-- 发送失败重试 -->
                  <div v-if="msg.failed" class="message-retry">
                    <span class="retry-hint">⚠️ 发送失败</span>
                    <el-button size="small" text type="danger" @click="retryMessage(msg)">重试</el-button>
                  </div>

                  <!-- 复制按钮（仅bot消息显示） -->
                  <div v-if="msg.type === 'bot'" class="message-copy" @click="copyMessage(msg)">
                    <el-icon><CopyDocument /></el-icon>
                  </div>
                </div>

                <el-avatar v-if="msg.type === 'user'" :size="32" class="message-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
            </div>
          </template>

          <!-- 正在输入提示 -->
          <div v-if="isTyping" class="typing-indicator">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <span style="margin-left: 8px;">AI正在思考...</span>
          </div>
        </div>

        <!-- 快捷问题：始终显示，有消息时折叠为小标题 -->
        <div class="quick-questions" :class="{ 'collapsed': messages.length > 0 }">
          <div class="quick-title" @click="toggleQuickQuestions">
            常见问题
            <el-icon class="quick-toggle"><ArrowUp v-if="quickExpanded || messages.length === 0" /><ArrowDown v-else /></el-icon>
          </div>
          <div v-show="quickExpanded || messages.length === 0" class="quick-buttons">
            <el-button
              v-for="q in quickQuestions"
              :key="q.id"
              size="small"
              @click="askQuestion(q.question)"
            >
              {{ q.label }}
            </el-button>
          </div>
        </div>

        <!-- 转人工按钮 -->
        <div class="transfer-bar">
          <el-button size="small" text @click="transferToHuman" :loading="transferring">
            <el-icon><Phone /></el-icon>
            转接人工客服
          </el-button>
        </div>

        <!-- 输入框 -->
        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            placeholder="输入您的问题..."
            @keydown.enter.prevent="handleEnter"
            @compositionstart="isComposing = true"
            @compositionend="isComposing = false"
            :disabled="isTyping"
          >
            <template #suffix>
              <el-button
                text
                :icon="Promotion"
                @click="sendMessage"
                :loading="isTyping"
                :disabled="!inputMessage.trim()"
              >
                发送
              </el-button>
            </template>
          </el-input>
        </div>

        <!-- 底部信息 -->
        <div class="chat-footer">
          <el-text size="small" type="info">
            AI客服24小时在线为您服务
          </el-text>
        </div>
        </template>
        <!-- ========== AI 模式结束 ========== -->

      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Service, Close, Delete, User, Promotion, CopyDocument, ArrowUp, ArrowDown, Phone } from '@element-plus/icons-vue'
import aiService from '@/utils/aiService'
import { submitFeedback as submitFeedbackAPI } from '@/api/ai'
import { createServiceSession, sendUserMessage, getSessionMessages, closeSession } from '@/api/service'

const router = useRouter()

const isOpen = ref(false)
const messages = ref([])
const inputMessage = ref('')
const isTyping = ref(false)
const unreadCount = ref(0)
const messagesContainer = ref(null)
const isComposing = ref(false)       // 中文输入法组合中标志
const quickExpanded = ref(false)     // 有消息时快捷问题是否展开

// 人工客服相关
const isHumanMode = ref(false)       // 是否在人工客服模式
const humanSessionId = ref(null)     // 当前人工会话ID
const humanMessages = ref([])        // 人工会话消息列表
const humanInput = ref('')           // 人工会话输入框
const transferring = ref(false)      // 转接中
const humanConnected = ref(false)    // 人工客服是否已接入
let serviceWs = null                 // 客服专用 WebSocket

// 快捷问题
const quickQuestions = ref([
  { id: 1, label: '如何预约剧本？', question: '如何预约剧本？' },
  { id: 2, label: '支付方式', question: '支持哪些支付方式？' },
  { id: 3, label: '退款政策', question: '如何申请退款？' },
  { id: 4, label: '热门推荐', question: '有什么热门剧本推荐？' },
  { id: 5, label: '游戏玩法', question: '剧本杀怎么玩？' },
  { id: 6, label: '团体预约', question: '支持团体预约吗？' }
])

// 打开/关闭聊天
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    unreadCount.value = 0
    
    // 首次打开显示欢迎消息
    if (messages.value.length === 0) {
      addBotMessage(getGreeting() + ' 我是AI智能客服小剧，很高兴为您服务！😊\n\n我可以帮您了解预约流程、退款政策、推荐剧本等。请问有什么可以帮您的吗？')
    }
    
    nextTick(() => {
      scrollToBottom()
    })
  }
}

// 获取问候语
const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好！'
  if (hour < 18) return '下午好！'
  return '晚上好！'
}

// 中文输入法兼容：仅在非组合输入时发送
const handleEnter = () => {
  if (!isComposing.value) {
    sendMessage()
  }
}

// 展开/收起快捷问题
const toggleQuickQuestions = () => {
  if (messages.value.length > 0) {
    quickExpanded.value = !quickExpanded.value
  }
}

// 复制消息内容
const copyMessage = async (msg) => {
  const text = msg.content.replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success({ message: '已复制', duration: 1500 })
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 重试失败的消息
const retryMessage = async (failedMsg) => {
  // 找到该失败消息的原始用户输入（失败消息前一条）
  const idx = messages.value.findIndex(m => m.id === failedMsg.id)
  const userMsg = idx > 0 ? messages.value[idx - 1] : null
  if (!userMsg) return

  // 移除失败的bot消息
  messages.value.splice(idx, 1)
  failedMsg.failed = false

  isTyping.value = true
  try {
    const response = await Promise.race([
      aiService.chat(userMsg.content, { page: router.currentRoute.value.path }),
      new Promise((_, reject) => setTimeout(() => reject(new Error('请求超时')), 8000))
    ])
    await new Promise(resolve => setTimeout(resolve, 500))
    addBotMessage(response.message, response.suggestions || [], response.actions || [], !response.fallback)
  } catch (error) {
    addBotMessage('', [], [], false, true) // failed = true
  } finally {
    isTyping.value = false
    scrollToBottom()
  }
}

// 发送消息（含超时控制）
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  addUserMessage(userMessage)
  isTyping.value = true

  try {
    const response = await Promise.race([
      aiService.chat(userMessage, { page: router.currentRoute.value.path }),
      new Promise((_, reject) => setTimeout(() => reject(new Error('请求超时')), 8000))
    ])
    await new Promise(resolve => setTimeout(resolve, 500))
    addBotMessage(response.message, response.suggestions || [], response.actions || [], !response.fallback)
    // 检测到转人工意图，自动触发转接
    if (response.triggerTransfer) {
      setTimeout(() => transferToHuman(), 800)
    }
  } catch (error) {
    console.error('AI对话失败:', error)
    const isTimeout = error.message === '请求超时'
    // 标记最后一条用户消息发送失败，显示重试按钮
    addBotMessage(
      isTimeout
        ? `<div class="kb-answer"><p>⏱️ 响应超时，请稍后重试。</p><p>📞 人工客服：<strong>400-123-4567</strong></p></div>`
        : `<div class="kb-answer"><p>🤔 抱歉，我遇到了一点小问题。</p><p>📞 人工客服：<strong>400-123-4567</strong></p></div>`,
      ['如何预约剧本？', '如何申请退款？', '联系人工客服'],
      [],
      false,
      true // failed = true，显示重试按钮
    )
  } finally {
    isTyping.value = false
    scrollToBottom()
  }
}

// 快捷提问
const askQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 添加用户消息
const addUserMessage = (content) => {
  messages.value.push({
    id: Date.now(),
    type: 'user',
    content,
    timestamp: Date.now()
  })
  scrollToBottom()
}

// 添加机器人消息
const addBotMessage = (content, suggestions = [], actions = [], showFeedback = false, failed = false) => {
  const messageId = Date.now()
  messages.value.push({
    id: messageId,
    type: 'bot',
    content: formatContent(content),
    suggestions,
    actions,
    showFeedback,
    feedbackSubmitted: false,
    failed,
    timestamp: Date.now()
  })
  
  // 如果窗口未打开，增加未读计数
  if (!isOpen.value) {
    unreadCount.value++
  }
  
  scrollToBottom()
  return messageId
}

// 格式化消息内容
const formatContent = (content) => {
  // 将换行符转换为<br>
  content = content.replace(/\n/g, '<br>')
  
  // 将**文本**转换为粗体
  content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  
  // 将电话号码高亮
  content = content.replace(/(\d{3}-\d{3}-\d{4})/g, '<span style="color: #409eff; font-weight: bold;">$1</span>')
  
  return content
}

// 处理操作按钮
const handleAction = (action) => {
  if (action.route) {
    router.push(action.route)
    toggleChat()
  } else if (action.type === 'transfer') {
    transferToHuman()
  }
}

// 提交反馈
const submitFeedback = async (messageId, rating, comment) => {
  try {
    await submitFeedbackAPI({
      messageId,
      rating,
      comment
    })
    
    // 标记已提交反馈
    const message = messages.value.find(m => m.id === messageId)
    if (message) {
      message.feedbackSubmitted = true
    }
    
    ElMessage.success('感谢您的反馈！')
  } catch (error) {
    console.error('提交反馈失败:', error)
  }
}

// 清空对话
const clearChat = () => {
  messages.value = []
  aiService.clearHistory()
  addBotMessage('对话已清空。有什么可以帮您的吗？😊')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 保存对话历史（FIFO，最多200条，防止 localStorage 超限）
const saveChatHistory = () => {
  if (messages.value.length === 0) return
  try {
    let msgs = messages.value
    if (msgs.length > 200) {
      msgs = msgs.slice(-200)
    }
    const data = JSON.stringify({ messages: msgs, timestamp: Date.now() })
    localStorage.setItem('ai_chat_history', data)
  } catch (e) {
    // QuotaExceededError：清空历史重新保存
    if (e.name === 'QuotaExceededError') {
      try {
        const msgs = messages.value.slice(-50)
        localStorage.setItem('ai_chat_history', JSON.stringify({ messages: msgs, timestamp: Date.now() }))
      } catch { /* 忽略 */ }
    }
  }
}

// 恢复对话历史
const restoreChatHistory = () => {
  try {
    const history = localStorage.getItem('ai_chat_history')
    if (history) {
      const data = JSON.parse(history)
      // 只恢复24小时内的对话
      if (Date.now() - data.timestamp < 24 * 60 * 60 * 1000) {
        messages.value = data.messages
      }
    }
  } catch (error) {
    console.error('恢复对话历史失败:', error)
  }
}

// ==================== 人工客服相关 ====================

const transferToHuman = async () => {
  const userInfo = JSON.parse(localStorage.getItem('user-info') || '{}')
  if (!userInfo.id) {
    ElMessage.warning('请先登录后再使用人工客服服务')
    return
  }
  transferring.value = true
  try {
    const lastUserMsg = [...messages.value].reverse().find(m => m.type === 'user')
    const question = lastUserMsg?.content?.replace(/<[^>]+>/g, '') || ''
    const res = await createServiceSession({
      userName: userInfo.nickname || userInfo.name || '用户',
      question
    })
    if (res.code === 1 || res.code === 200) {
      humanSessionId.value = res.data
      isHumanMode.value = true
      humanMessages.value = []
      humanConnected.value = false
      await loadHumanMessages()
      connectServiceWs(userInfo.id)
    }
  } catch (e) {
    ElMessage.error('转接失败，请稍后重试')
  } finally {
    transferring.value = false
  }
}

const loadHumanMessages = async () => {
  if (!humanSessionId.value) return
  const res = await getSessionMessages(humanSessionId.value)
  if (res.code === 1 || res.code === 200) {
    humanMessages.value = (res.data || []).map(m => ({
      ...m, createTime: new Date(m.createTime).getTime()
    }))
    scrollHumanToBottom()
  }
}

const humanMessagesRef = ref(null)

const connectServiceWs = (userId) => {
  if (serviceWs) serviceWs.close()
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  serviceWs = new WebSocket(`${protocol}//${location.host}/api/ws/service?userId=${userId}&role=user`)
  serviceWs.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'session_accepted') {
        humanConnected.value = true
        humanMessages.value.push({ id: Date.now(), senderType: 'system', content: '客服已接入，请开始咨询。', createTime: Date.now() })
        scrollHumanToBottom()
      } else if (data.type === 'service_message' && data.senderType === 'admin') {
        humanMessages.value.push({ id: Date.now(), senderType: 'admin', content: data.content, createTime: Date.now() })
        scrollHumanToBottom()
      } else if (data.type === 'session_closed') {
        humanConnected.value = false
        humanMessages.value.push({ id: Date.now(), senderType: 'system', content: '会话已结束，感谢您的咨询。', createTime: Date.now() })
        scrollHumanToBottom()
      }
    } catch (e) { /* 忽略 */ }
  }
  serviceWs.onclose = () => {
    if (isHumanMode.value) setTimeout(() => connectServiceWs(userId), 5000)
  }
  const hb = setInterval(() => {
    if (serviceWs?.readyState === WebSocket.OPEN) serviceWs.send('ping')
    else clearInterval(hb)
  }, 30000)
}

const sendHumanMessage = async () => {
  if (!humanInput.value.trim() || !humanSessionId.value) return
  const content = humanInput.value.trim()
  humanInput.value = ''
  humanMessages.value.push({ id: Date.now(), senderType: 'user', content, createTime: Date.now() })
  scrollHumanToBottom()
  try {
    await sendUserMessage(humanSessionId.value, { content })
  } catch (e) {
    ElMessage.error('发送失败，请重试')
  }
}

const endHumanSession = async () => {
  if (!humanSessionId.value) return
  await closeSession(humanSessionId.value)
  isHumanMode.value = false
  humanSessionId.value = null
  humanConnected.value = false
  if (serviceWs) { serviceWs.close(); serviceWs = null }
  ElMessage.success('会话已结束')
}

const backToAI = () => { isHumanMode.value = false }

const scrollHumanToBottom = () => {
  nextTick(() => {
    if (humanMessagesRef.value) humanMessagesRef.value.scrollTop = humanMessagesRef.value.scrollHeight
  })
}

// 生命周期
onMounted(() => {
  restoreChatHistory()
})

onUnmounted(() => {
  saveChatHistory()
  if (serviceWs) serviceWs.close()
})
</script>

<style scoped>
.ai-customer-service {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 9999;
}

.service-bubble {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  position: relative;
}

.service-bubble:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.service-bubble.has-unread {
  animation: shake 0.5s ease-in-out infinite;
}

.service-icon {
  font-size: 28px;
  color: white;
}

.bubble-tip {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}

.service-bubble:hover .bubble-tip {
  opacity: 1;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #f56c6c;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.chat-window {
  width: min(380px, 92vw);
  height: min(600px, 85vh);
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 480px) {
  .chat-window {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
    position: fixed;
    bottom: 0;
    right: 0;
  }
  .ai-customer-service {
    bottom: 0;
    right: 0;
  }
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-weight: bold;
  font-size: 16px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  opacity: 0.9;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #67c23a;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.header-right {
  display: flex;
  gap: 8px;
}

.header-right .el-button {
  color: white;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.message-wrapper {
  margin-bottom: 16px;
}

.message {
  display: flex;
  gap: 12px;
  animation: messageSlide 0.3s ease-out;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message.bot .message-bubble {
  background: white;
  border: 1px solid #e4e7ed;
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-suggestions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.message-feedback {
  margin-top: 8px;
  padding: 8px;
  background: #f0f2f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.message-time {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  width: fit-content;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  margin-right: 4px;
  animation: typingBounce 1.4s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.quick-questions {
  padding: 16px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

.quick-title {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 12px;
}

.quick-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-input {
  padding: 16px;
  background: white;
  border-top: 1px solid #e4e7ed;
}

.chat-footer {
  padding: 8px 16px;
  text-align: center;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

/* 动画 */
@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-10deg); }
  75% { transform: rotate(10deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes messageSlide {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

/* 过渡动画 */
.bounce-enter-active {
  animation: bounce 0.5s;
}

.bounce-leave-active {
  animation: bounce 0.5s reverse;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 时间分隔线 */
.time-divider {
  text-align: center;
  font-size: 11px;
  color: #c0c4cc;
  margin: 8px 0;
  position: relative;
}

.time-divider::before,
.time-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: #e4e7ed;
}

.time-divider::before { left: 0; }
.time-divider::after { right: 0; }

/* 复制按钮 */
.message-copy {
  display: none;
  cursor: pointer;
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
  padding: 2px 4px;
  border-radius: 4px;
  transition: color 0.2s;
}

.message-wrapper:hover .message-copy {
  display: flex;
  align-items: center;
  gap: 4px;
}

.message-copy:hover {
  color: #667eea;
}

/* 重试按钮 */
.message-retry {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.retry-hint {
  font-size: 12px;
  color: #f56c6c;
}

/* 快捷问题折叠状态 */
.quick-questions.collapsed {
  padding: 8px 16px;
}

.quick-questions.collapsed .quick-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  margin-bottom: 0;
  user-select: none;
}

.quick-questions.collapsed .quick-title:hover {
  color: #667eea;
}

.quick-toggle {
  font-size: 12px;
  transition: transform 0.2s;
}

.quick-questions.collapsed .quick-buttons {
  padding-top: 8px;
}

/* 转人工按钮栏 */
.transfer-bar {
  padding: 4px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

.transfer-bar .el-button {
  color: #909399;
  font-size: 12px;
}

.transfer-bar .el-button:hover {
  color: #667eea;
}

/* 人工客服系统消息 */
.human-system-msg {
  text-align: center;
  font-size: 12px;
  color: #909399;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 6px 16px;
  margin: 4px auto;
  max-width: 80%;
}

/* 知识库回答样式 */
:deep(.kb-answer) {
  line-height: 1.8;
}

:deep(.kb-answer p) {
  margin: 8px 0;
}

:deep(.kb-answer strong) {
  color: #667eea;
}

:deep(.kb-answer ol),
:deep(.kb-answer ul) {
  margin: 8px 0;
  padding-left: 20px;
}

:deep(.kb-answer li) {
  margin: 4px 0;
}
</style>
