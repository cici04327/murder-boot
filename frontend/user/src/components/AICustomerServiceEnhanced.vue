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
          <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
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
                  <el-button
                    size="small"
                    @click="submitFeedback(msg.id, 5, '很有帮助')"
                    :disabled="msg.feedbackSubmitted"
                  >
                    👍 有帮助
                  </el-button>
                  <el-button
                    size="small"
                    @click="submitFeedback(msg.id, 1, '没有帮助')"
                    :disabled="msg.feedbackSubmitted"
                  >
                    👎 没帮助
                  </el-button>
                </div>
                
                <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
              </div>

              <el-avatar v-if="msg.type === 'user'" :size="32" class="message-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
          </div>

          <!-- 正在输入提示 -->
          <div v-if="isTyping" class="typing-indicator">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <span style="margin-left: 8px;">AI正在思考...</span>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div v-if="messages.length === 0" class="quick-questions">
          <div class="quick-title">常见问题</div>
          <div class="quick-buttons">
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

        <!-- 输入框 -->
        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            placeholder="输入您的问题..."
            @keyup.enter="sendMessage"
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
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Service, Close, Delete, User, Promotion } from '@element-plus/icons-vue'
import aiService from '@/utils/aiService'
import { submitFeedback as submitFeedbackAPI } from '@/api/ai'

const router = useRouter()

const isOpen = ref(false)
const messages = ref([])
const inputMessage = ref('')
const isTyping = ref(false)
const unreadCount = ref(0)
const messagesContainer = ref(null)

// 快捷问题
const quickQuestions = ref([
  { id: 1, label: '如何预约剧本？', question: '如何预约剧本？' },
  { id: 2, label: '支付方式', question: '支持哪些支付方式？' },
  { id: 3, label: '退款政策', question: '如何申请退款？' },
  { id: 4, label: '热门推荐', question: '有什么热门剧本推荐？' }
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

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息
  addUserMessage(userMessage)

  // 显示正在输入
  isTyping.value = true

  try {
    // 调用AI服务（已内置降级处理）
    const response = await aiService.chat(userMessage, {
      page: router.currentRoute.value.path
    })

    // 模拟打字延迟
    await new Promise(resolve => setTimeout(resolve, 800))

    // 添加AI回复
    addBotMessage(
      response.message,
      response.suggestions || [],
      response.actions || [],
      !response.fallback // 非降级响应才显示反馈按钮
    )

  } catch (error) {
    console.error('AI对话失败:', error)
    // 即使出现意外错误，也提供友好的回复
    addBotMessage(
      `<div class="kb-answer"><p>🤔 抱歉，我遇到了一点小问题。</p><p><strong>您可以尝试：</strong></p><ul><li>💡 点击上方常见问题</li><li>🔄 重新描述您的问题</li><li>📞 联系人工客服：<strong>400-123-4567</strong></li></ul></div>`,
      ['如何预约剧本？', '如何申请退款？', '联系人工客服'],
      [],
      false
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
const addBotMessage = (content, suggestions = [], actions = [], showFeedback = false) => {
  const messageId = Date.now()
  messages.value.push({
    id: messageId,
    type: 'bot',
    content: formatContent(content),
    suggestions,
    actions,
    showFeedback,
    feedbackSubmitted: false,
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
    ElMessage.info('正在为您转接人工客服...')
    // TODO: 实现人工客服转接
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

// 保存对话历史
const saveChatHistory = () => {
  if (messages.value.length > 0) {
    localStorage.setItem('ai_chat_history', JSON.stringify({
      messages: messages.value,
      timestamp: Date.now()
    }))
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

// 生命周期
onMounted(() => {
  restoreChatHistory()
})

onUnmounted(() => {
  saveChatHistory()
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
  width: 380px;
  height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
