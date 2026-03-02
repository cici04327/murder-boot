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
        <div class="bubble-glow"></div>
        <el-icon class="service-icon"><Service /></el-icon>
        <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</div>
        <div class="bubble-tip">🎭 AI客服</div>
      </div>
    </transition>

    <!-- 聊天窗口 -->
    <transition name="slide-up">
      <div v-if="isOpen" class="chat-window" :class="{ 'fullscreen': isFullscreen }">
        <!-- 头部 -->
        <div class="chat-header">
          <div class="header-left">
            <div class="avatar-wrapper">
              <div class="avatar-glow"></div>
              <el-avatar :size="42" class="bot-avatar">
                <span>🎭</span>
              </el-avatar>
              <div class="online-dot"></div>
            </div>
            <div class="header-info">
              <div class="header-title">AI客服小剧</div>
              <div class="header-status">
                <span class="status-dot"></span>
                {{ connectionStatus }}
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip content="历史记录" placement="bottom">
              <el-button text circle @click="showHistoryPanel = !showHistoryPanel">
                <el-icon><Clock /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="清空记录" placement="bottom">
              <el-button text circle @click="clearHistory">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
              <el-button text circle @click="toggleFullscreen">
                <el-icon><FullScreen v-if="!isFullscreen" /><Close v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="最小化" placement="bottom">
              <el-button text circle @click="minimizeChat">
                <el-icon><Minus /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="关闭" placement="bottom">
              <el-button text circle @click="closeChat">
                <el-icon><Close /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 历史会话面板 -->
        <transition name="slide-down">
          <div v-if="showHistoryPanel" class="history-panel">
            <div class="history-header">
              <span>📚 历史会话</span>
              <el-button text size="small" @click="clearAllHistory" type="danger">
                清空全部
              </el-button>
            </div>
            <div class="history-list" v-if="chatSessions.length > 0">
              <div 
                v-for="session in chatSessions" 
                :key="session.id"
                class="history-item"
                :class="{ active: session.id === currentSessionId }"
                @click="loadSession(session.id)"
              >
                <div class="history-item-title">{{ session.title || '新会话' }}</div>
                <div class="history-item-time">{{ formatSessionTime(session.timestamp) }}</div>
                <el-button 
                  text 
                  size="small" 
                  class="history-item-delete"
                  @click.stop="deleteSession(session.id)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="history-empty" v-else>
              暂无历史会话
            </div>
            <el-button class="new-session-btn" @click="startNewSession" type="primary" size="small">
              + 开始新会话
            </el-button>
          </div>
        </transition>

        <!-- 快捷标签 -->
        <div class="quick-tags">
          <div 
            class="quick-tag" 
            v-for="tag in quickTags" 
            :key="tag.id"
            @click="askQuestion(tag.question)"
          >
            <span class="tag-icon">{{ tag.icon }}</span>
            <span class="tag-text">{{ tag.label }}</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
          <!-- 加载更多 -->
          <div v-if="hasMoreMessages" class="load-more" @click="loadMoreMessages">
            <el-icon class="is-loading" v-if="loadingMore"><Loading /></el-icon>
            <span v-else>加载更多消息</span>
          </div>

          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            class="message-item"
            :class="[msg.type, { 'highlight': msg.highlight }]"
          >
            <!-- AI消息 -->
            <template v-if="msg.type === 'ai'">
              <div class="message-avatar">
                <el-avatar :size="32" class="ai-avatar">🎭</el-avatar>
              </div>
              <div class="message-content">
                <div class="message-bubble" v-html="msg.content"></div>
                
                <!-- 相关建议标签 -->
                <div class="message-suggestions" v-if="msg.suggestions && msg.suggestions.length">
                  <span class="suggestions-label">相关问题：</span>
                  <el-tag 
                    v-for="(suggestion, idx) in msg.suggestions" 
                    :key="idx"
                    size="small"
                    type="info"
                    effect="plain"
                    @click="askQuestion(suggestion)"
                    class="suggestion-tag"
                  >
                    {{ suggestion }}
                  </el-tag>
                </div>
                
                <div class="message-meta">
                  <span class="message-time">{{ msg.time }}</span>
                  
                  <!-- 消息操作 -->
                  <div class="message-tools">
                    <el-tooltip content="复制" placement="top">
                      <el-button text size="small" @click="copyMessage(msg)">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="朗读" placement="top">
                      <el-button text size="small" @click="speakMessage(msg)" :class="{ speaking: speakingMsgId === msg.id }">
                        <el-icon><Microphone /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                  
                  <!-- 满意度反馈 -->
                  <div class="message-feedback" v-if="msg.showFeedback && !msg.feedbackGiven">
                    <span class="feedback-label">有帮助吗？</span>
                    <el-button text size="small" @click="giveFeedback(msg.id, true, 5)">
                      <el-icon><Select /></el-icon> 有用
                    </el-button>
                    <el-button text size="small" @click="giveFeedback(msg.id, false, 1)">
                      <el-icon><CloseBold /></el-icon> 没用
                    </el-button>
                  </div>
                  <div class="feedback-thanks" v-if="msg.feedbackGiven">
                    ✓ 感谢反馈
                  </div>
                </div>
                
                <!-- 快捷操作按钮 -->
                <div class="message-actions" v-if="msg.actions && msg.actions.length">
                  <el-button 
                    v-for="action in msg.actions" 
                    :key="action.label"
                    size="small"
                    :type="action.type || 'default'"
                    @click="handleAction(action)"
                  >
                    {{ action.label }}
                  </el-button>
                </div>
              </div>
            </template>

            <!-- 用户消息 -->
            <template v-else>
              <div class="message-content">
                <div class="message-bubble">{{ msg.content }}</div>
                <div class="message-meta">
                  <span class="message-time">{{ msg.time }}</span>
                  <el-icon v-if="msg.status === 'sending'" class="is-loading"><Loading /></el-icon>
                  <el-icon v-else-if="msg.status === 'sent'" style="color: #67c23a;"><Select /></el-icon>
                  <el-icon v-else-if="msg.status === 'failed'" style="color: #f56c6c;" @click="resendMessage(msg)"><RefreshRight /></el-icon>
                </div>
              </div>
            </template>
          </div>

          <!-- AI正在输入 -->
          <div v-if="isTyping" class="message-item ai typing-indicator">
            <div class="message-avatar">
              <el-avatar :size="32" class="ai-avatar">🎭</el-avatar>
            </div>
            <div class="message-content">
              <div class="typing-bubble">
                <div class="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <span class="typing-text">正在思考中...</span>
              </div>
            </div>
          </div>
          
          <!-- 滚动到底部按钮 -->
          <transition name="fade">
            <div v-if="showScrollBottom" class="scroll-bottom-btn" @click="scrollToBottom">
              <el-icon><ArrowDown /></el-icon>
              <span v-if="newMessageCount > 0">{{ newMessageCount }}条新消息</span>
            </div>
          </transition>
        </div>

        <!-- 快捷问题 -->
        <div class="quick-questions" v-if="showQuickQuestions">
          <div class="quick-questions-header">
            <span>💡 猜你想问</span>
            <el-button text size="small" @click="showQuickQuestions = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="quick-questions-tabs">
            <el-radio-group v-model="questionCategory" size="small">
              <el-radio-button label="hot">🔥 热门</el-radio-button>
              <el-radio-button label="reservation">📅 预约</el-radio-button>
              <el-radio-button label="payment">💰 支付</el-radio-button>
              <el-radio-button label="account">👤 账户</el-radio-button>
            </el-radio-group>
          </div>
          <div class="quick-questions-list">
            <div 
              class="quick-question-item"
              v-for="q in filteredQuickQuestions" 
              :key="q.id"
              @click="askQuestion(q.question)"
            >
              {{ q.label }}
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <div class="input-tools">
            <el-tooltip content="常见问题" placement="top">
              <el-button text circle @click="showQuickQuestions = !showQuickQuestions" :class="{ active: showQuickQuestions }">
                <el-icon><QuestionFilled /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="表情" placement="top">
              <el-popover placement="top" :width="280" trigger="click">
                <template #reference>
                  <el-button text circle>
                    <el-icon><Sunny /></el-icon>
                  </el-button>
                </template>
                <div class="emoji-picker">
                  <span 
                    v-for="emoji in emojis" 
                    :key="emoji" 
                    class="emoji-item"
                    @click="insertEmoji(emoji)"
                  >
                    {{ emoji }}
                  </span>
                </div>
              </el-popover>
            </el-tooltip>
          </div>
          <el-input
            ref="inputRef"
            v-model="inputMessage"
            placeholder="输入您的问题..."
            @keyup.enter="sendMessage"
            @input="handleInputChange"
            :disabled="isTyping"
            maxlength="500"
            show-word-limit
          />
          <el-button 
            type="primary" 
            :icon="Promotion"
            @click="sendMessage"
            :disabled="!inputMessage.trim() || isTyping"
            :loading="isTyping"
            class="send-btn"
          >
            发送
          </el-button>
        </div>

        <!-- 底部提示 -->
        <div class="chat-footer">
          <el-text type="info" size="small">
            🎭 AI客服24小时在线 · 如需人工服务请说"转人工"
          </el-text>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Service, Delete, Minus, Close, QuestionFilled, 
  Promotion, Select, CloseBold, Clock, FullScreen,
  CopyDocument, Microphone, Loading, RefreshRight, 
  ArrowDown, Sunny
} from '@element-plus/icons-vue'
import { submitFeedback as submitFeedbackAPI } from '@/api/ai'

const router = useRouter()
const isOpen = ref(false)
const messages = ref([])
const inputMessage = ref('')
const isTyping = ref(false)
const unreadCount = ref(0)
const messagesContainer = ref(null)
const inputRef = ref(null)
const showQuickQuestions = ref(false)
const showHistoryPanel = ref(false)
const isFullscreen = ref(false)
const connectionStatus = ref('在线服务中')
const questionCategory = ref('hot')
const speakingMsgId = ref(null)
const showScrollBottom = ref(false)
const newMessageCount = ref(0)
const hasMoreMessages = ref(false)
const loadingMore = ref(false)
const chatSessions = ref([])
const currentSessionId = ref(null)

// 表情列表
const emojis = ref([
  '😊', '😄', '🤔', '👍', '👎', '❤️', '🎭', '🎬', '🎮', '🎯',
  '💰', '🎫', '📅', '⏰', '📞', '💬', '✅', '❌', '⭐', '🔥',
  '👏', '🙏', '💪', '🤝', '👀', '😅', '😂', '🥺', '😭', '🤗'
])

// 快捷标签
const quickTags = ref([
  { id: 1, icon: '📅', label: '预约', question: '如何预约剧本杀？' },
  { id: 2, icon: '💰', label: '退款', question: '如何申请退款？' },
  { id: 3, icon: '🎫', label: '优惠', question: '有什么优惠活动？' },
  { id: 4, icon: '🏪', label: '门店', question: '如何查找附近的门店？' },
  { id: 5, icon: '👥', label: '拼车', question: '可以拼车组队吗？' },
  { id: 6, icon: '👤', label: '人工', question: '转人工客服' }
])

// 快捷问题列表（按分类）
const quickQuestions = ref([
  // 热门问题
  { id: 1, label: '🎮 如何预约剧本杀？', question: '如何预约剧本杀？', category: 'hot' },
  { id: 2, label: '🔥 有什么热门剧本推荐？', question: '有什么热门剧本推荐？', category: 'hot' },
  { id: 3, label: '💳 支持哪些支付方式？', question: '支持哪些支付方式？', category: 'hot' },
  { id: 4, label: '🔄 如何申请退款？', question: '如何申请退款？', category: 'hot' },
  // 预约相关
  { id: 5, label: '📅 预约后可以改时间吗？', question: '预约后可以修改时间吗？', category: 'reservation' },
  { id: 6, label: '👥 人数不够怎么办？', question: '人数不够可以预约吗？', category: 'reservation' },
  { id: 7, label: '⏰ 迟到了怎么办？', question: '预约后迟到了怎么办？', category: 'reservation' },
  { id: 8, label: '🚫 可以取消预约吗？', question: '如何取消预约？', category: 'reservation' },
  // 支付相关
  { id: 9, label: '🎁 优惠券怎么使用？', question: '优惠券怎么使用？', category: 'payment' },
  { id: 10, label: '⭐ 积分有什么用？', question: '积分有什么用？', category: 'payment' },
  { id: 11, label: '💰 退款多久到账？', question: '退款多久能到账？', category: 'payment' },
  { id: 12, label: '👑 VIP有什么特权？', question: 'VIP会员有什么特权？', category: 'payment' },
  // 账户相关
  { id: 13, label: '🔐 忘记密码怎么办？', question: '忘记密码怎么办？', category: 'account' },
  { id: 14, label: '📱 如何修改手机号？', question: '如何修改绑定的手机号？', category: 'account' },
  { id: 15, label: '🖼️ 如何修改头像？', question: '如何修改头像和昵称？', category: 'account' },
  { id: 16, label: '🔔 如何关闭通知？', question: '如何关闭消息通知？', category: 'account' }
])

// 过滤后的快捷问题
const filteredQuickQuestions = computed(() => {
  return quickQuestions.value.filter(q => q.category === questionCategory.value)
})

// 知识库
const knowledgeBase = {
  '预约|怎么预约|如何预约|订票|下单': {
    answer: `<div class="kb-answer"><p><strong>📋 预约剧本杀流程：</strong></p><ol><li>🔍 浏览剧本列表，选择喜欢的剧本</li><li>🏪 选择门店和游戏时间</li><li>👥 填写参与人数和联系方式</li><li>🎫 选择优惠券（如有）</li><li>💳 确认支付完成预约</li></ol><p><strong>💡 小贴士：</strong></p><ul><li>热门剧本建议提前3-7天预约</li><li>周末档期更紧张，尽早预约</li><li>可以先加入拼车，等人齐了再付款</li></ul></div>`,
    actions: [{ label: '🔍 浏览剧本', route: '/script', type: 'primary' }, { label: '🏪 查看门店', route: '/store' }],
    showFeedback: true
  },
  '支付|付款|怎么付|支付方式': {
    answer: `<div class="kb-answer"><p><strong>💳 支持的支付方式：</strong></p><ul><li>✅ 微信支付（推荐）</li><li>✅ 支付宝</li><li>✅ 银行卡支付</li><li>✅ 余额支付</li></ul><p><strong>🎁 省钱小技巧：</strong></p><ul><li>使用优惠券可享折扣</li><li>积分可抵扣部分金额</li><li>关注平台活动获取红包</li></ul></div>`,
    showFeedback: true
  },
  '退款|退钱|取消预约|不去了': {
    answer: `<div class="kb-answer"><p><strong>🔄 退款规则：</strong></p><ul><li>✅ 游戏开始前 <strong>24小时以上</strong>：全额退款</li><li>⚠️ 游戏开始前 <strong>12-24小时</strong>：退款80%</li><li>⚠️ 游戏开始前 <strong>6-12小时</strong>：退款50%</li><li>❌ 游戏开始前 <strong>6小时内</strong>：不支持退款</li></ul><p><strong>📝 退款流程：</strong></p><ol><li>进入"我的预约"</li><li>找到要取消的订单</li><li>点击"申请退款"</li><li>填写退款原因</li><li>等待审核（1-3个工作日）</li></ol></div>`,
    actions: [{ label: '📋 我的预约', route: '/user/reservations', type: 'primary' }],
    showFeedback: true
  },
  '优惠券|优惠|折扣|红包|券': {
    answer: `<div class="kb-answer"><p><strong>🎫 优惠券使用说明：</strong></p><ul><li>💰 支付时自动展示可用优惠券</li><li>📋 每笔订单限用一张优惠券</li><li>⏰ 注意查看优惠券有效期</li><li>💵 部分券有最低消费门槛</li></ul><p><strong>🎁 获取优惠券的方式：</strong></p><ul><li>🆕 新用户注册礼包</li><li>📅 每日签到奖励</li><li>⭐ 积分兑换</li><li>🎉 参与平台活动</li><li>👥 邀请好友奖励</li></ul></div>`,
    actions: [{ label: '🎫 我的优惠券', route: '/user/coupons', type: 'primary' }],
    showFeedback: true
  },
  '积分|点数|奖励|兑换': {
    answer: `<div class="kb-answer"><p><strong>⭐ 积分用途：</strong></p><ul><li>💰 抵扣现金（100积分=1元）</li><li>🎫 兑换优惠券</li><li>🎁 兑换精美周边</li><li>👑 提升会员等级</li></ul><p><strong>📈 获取积分的方式：</strong></p><ul><li>🎮 完成预约并游戏 +50积分</li><li>⭐ 发表评价 +20积分</li><li>📅 每日签到 +5积分</li><li>👥 邀请好友注册 +100积分</li></ul></div>`,
    actions: [{ label: '⭐ 我的积分', route: '/user/points', type: 'primary' }],
    showFeedback: true
  },
  '密码|登录|注册|账号|忘记': {
    answer: `<div class="kb-answer"><p><strong>🔐 账号常见问题：</strong></p><p><strong>忘记密码？</strong></p><ol><li>点击登录页"忘记密码"</li><li>输入注册手机号</li><li>获取验证码并验证</li><li>设置新密码</li></ol><p><strong>如何注册？</strong></p><ul><li>📱 手机号+验证码快速注册</li><li>💬 微信一键授权登录</li></ul></div>`,
    actions: [{ label: '🔐 账号设置', route: '/user/settings', type: 'primary' }],
    showFeedback: true
  },
  '门店|店铺|附近|地址|位置': {
    answer: `<div class="kb-answer"><p><strong>🏪 查找门店方式：</strong></p><ul><li>📍 首页点击"附近门店"自动定位</li><li>🔍 搜索门店名称或地区</li><li>🗺️ 地图模式查看周边门店</li></ul><p><strong>💡 选店建议：</strong></p><ul><li>⭐ 查看门店评分和评价</li><li>🎮 关注门店的剧本库存</li><li>💰 对比不同门店价格</li></ul></div>`,
    actions: [{ label: '🏪 浏览门店', route: '/store', type: 'primary' }],
    showFeedback: true
  },
  '拼车|组队|凑人|一起玩|找人': {
    answer: `<div class="kb-answer"><p><strong>👥 拼车组队功能：</strong></p><ul><li>🎯 人数不够？发起拼车让其他玩家加入</li><li>👀 浏览已有的拼车房间</li><li>💬 房间内可以聊天交流</li><li>✅ 人齐后一起支付确认</li></ul></div>`,
    actions: [{ label: '👥 拼车大厅', route: '/group', type: 'primary' }],
    showFeedback: true
  },
  '新手|第一次|入门|怎么玩|规则': {
    answer: `<div class="kb-answer"><p><strong>🎭 剧本杀入门指南：</strong></p><p>剧本杀是一种角色扮演推理游戏，每位玩家扮演一个角色，通过阅读剧本、搜集线索、推理讨论来完成游戏目标。</p><p><strong>🆕 新手建议：</strong></p><ul><li>📖 选择"新手本"或"欢乐本"开始</li><li>⏱️ 预留3-4小时游戏时间</li><li>👔 穿着舒适的衣服</li><li>🎭 大胆代入角色，享受过程</li></ul></div>`,
    actions: [{ label: '📖 新手剧本', route: '/script?type=newbie', type: 'primary' }],
    showFeedback: true
  },
  '人工|客服|联系|咨询|电话|转人工': {
    answer: `<div class="kb-answer"><p><strong>📞 联系我们：</strong></p><ul><li>☎️ 客服热线：<strong style="color: #8B0000;">400-123-4567</strong></li><li>⏰ 服务时间：9:00-22:00（全年无休）</li><li>📧 邮箱：service@jubensha.com</li><li>💬 微信公众号：剧本杀预约平台</li></ul><p><strong>💡 温馨提示：</strong></p><ul><li>非紧急问题可以先问我哦~</li><li>请准备好订单号以便快速处理</li></ul></div>`,
    actions: [{ label: '💬 意见反馈', route: '/contact', type: 'primary' }],
    showFeedback: true
  },
  'VIP|会员|开通|特权': {
    answer: `<div class="kb-answer"><p><strong>👑 VIP会员特权：</strong></p><ul><li>💰 专属折扣（最高8折）</li><li>🎫 每月专属优惠券</li><li>⭐ 积分加成（最高2倍）</li><li>🎯 优先预约热门剧本</li><li>🎁 生日专属礼包</li></ul></div>`,
    actions: [{ label: '👑 开通VIP', route: '/vip', type: 'primary' }],
    showFeedback: true
  },
  '热门|推荐|好玩|精品|排行': {
    answer: `<div class="kb-answer"><p><strong>🔥 热门剧本推荐：</strong></p><ul><li>🎭 <strong>情感本</strong>：《年轮》《云使》- 适合喜欢沉浸体验的玩家</li><li>🔍 <strong>硬核本</strong>：《古木吟》《孤城》- 适合推理爱好者</li><li>😄 <strong>欢乐本</strong>：《奇妙物语》《我们的夜晚》- 适合新手和聚会</li><li>😱 <strong>恐怖本</strong>：《死者之书》《暗夜古堡》- 适合喜欢刺激的玩家</li></ul><p><strong>💡 温馨提示：</strong>可以根据人数、时长、风格筛选剧本哦！</p></div>`,
    actions: [{ label: '🔥 查看热门', route: '/script?sort=hot', type: 'primary' }, { label: '📖 全部剧本', route: '/script' }],
    showFeedback: true
  },
  '修改时间|改时间|改期|延期|换时间': {
    answer: `<div class="kb-answer"><p><strong>📅 修改预约时间说明：</strong></p><ul><li>✅ 游戏开始前 <strong>24小时以上</strong>：可免费修改1次</li><li>⚠️ 游戏开始前 <strong>12-24小时</strong>：需支付手续费</li><li>❌ 游戏开始前 <strong>12小时内</strong>：不支持修改</li></ul><p><strong>📝 修改步骤：</strong></p><ol><li>进入"我的预约"</li><li>选择需要修改的订单</li><li>点击"修改时间"</li><li>选择新的日期和时段</li></ol></div>`,
    actions: [{ label: '📋 我的预约', route: '/user/reservations', type: 'primary' }],
    showFeedback: true
  },
  '人数不够|凑人|少一个|差人': {
    answer: `<div class="kb-answer"><p><strong>👥 人数不够的解决方案：</strong></p><ul><li>🎯 <strong>发起拼车</strong>：创建拼车房间，等待其他玩家加入</li><li>👀 <strong>加入拼车</strong>：浏览已有的拼车房间并加入</li><li>💬 <strong>联系门店</strong>：门店可能有其他散客可以拼场</li></ul><p><strong>💡 小贴士：</strong></p><ul><li>热门剧本拼车成功率更高</li><li>提前2-3天发起拼车效果更好</li></ul></div>`,
    actions: [{ label: '👥 发起拼车', route: '/group/create', type: 'primary' }, { label: '👀 拼车大厅', route: '/group' }],
    showFeedback: true
  },
  '迟到|晚到|赶不上|堵车': {
    answer: `<div class="kb-answer"><p><strong>⏰ 迟到处理说明：</strong></p><ul><li>⚠️ 迟到 <strong>15分钟内</strong>：可正常参与游戏</li><li>⚠️ 迟到 <strong>15-30分钟</strong>：需联系门店确认是否可加入</li><li>❌ 迟到 <strong>30分钟以上</strong>：可能无法参与，不予退款</li></ul><p><strong>💡 建议：</strong></p><ul><li>请预留充足的到达时间</li><li>如预计迟到请提前电话联系门店</li><li>导航建议提前查看门店具体位置</li></ul></div>`,
    showFeedback: true
  },
  '评价|评论|打分|点评': {
    answer: `<div class="kb-answer"><p><strong>⭐ 评价说明：</strong></p><ul><li>📝 游戏结束后可对剧本和门店进行评价</li><li>⭐ 发表评价可获得 <strong>20积分</strong> 奖励</li><li>📸 上传照片额外获得 <strong>10积分</strong></li></ul><p><strong>📋 评价内容：</strong></p><ul><li>剧本质量评分</li><li>DM主持水平</li><li>门店环境和服务</li><li>文字评价（可选）</li></ul></div>`,
    actions: [{ label: '📝 待评价订单', route: '/user/reservations?status=completed', type: 'primary' }],
    showFeedback: true
  },
  '收藏|喜欢|关注|感兴趣': {
    answer: `<div class="kb-answer"><p><strong>❤️ 收藏功能说明：</strong></p><ul><li>📖 可以收藏喜欢的剧本，方便下次查找</li><li>🏪 可以关注常去的门店，接收优惠通知</li><li>📋 收藏的内容可在"我的收藏"中查看</li></ul><p><strong>💡 小贴士：</strong></p><ul><li>收藏的剧本有优惠时会收到提醒</li><li>关注的门店发布新本时会通知您</li></ul></div>`,
    actions: [{ label: '❤️ 我的收藏', route: '/user/favorites', type: 'primary' }],
    showFeedback: true
  },
  '手机号|换号|修改手机|绑定': {
    answer: `<div class="kb-answer"><p><strong>📱 修改手机号流程：</strong></p><ol><li>进入"个人中心" > "账号设置"</li><li>点击"修改手机号"</li><li>验证原手机号（接收验证码）</li><li>输入新手机号并验证</li><li>完成修改</li></ol><p><strong>⚠️ 注意事项：</strong></p><ul><li>每30天只能修改1次手机号</li><li>修改后原手机号将解绑</li></ul></div>`,
    actions: [{ label: '⚙️ 账号设置', route: '/user/settings', type: 'primary' }],
    showFeedback: true
  },
  '头像|昵称|个人信息|资料': {
    answer: `<div class="kb-answer"><p><strong>👤 修改个人信息：</strong></p><ol><li>进入"个人中心"</li><li>点击头像或昵称区域</li><li>选择要修改的内容</li><li>保存更改</li></ol><p><strong>📝 可修改内容：</strong></p><ul><li>🖼️ 头像（支持拍照或相册选择）</li><li>✏️ 昵称</li><li>🎂 生日（设置后可获得生日礼包）</li><li>📍 常住城市</li></ul></div>`,
    actions: [{ label: '👤 个人中心', route: '/user/profile', type: 'primary' }],
    showFeedback: true
  },
  '通知|消息|提醒|推送': {
    answer: `<div class="kb-answer"><p><strong>🔔 通知设置说明：</strong></p><ul><li>📅 <strong>预约提醒</strong>：游戏开始前1天/2小时提醒</li><li>🎫 <strong>优惠通知</strong>：优惠券到期、新优惠活动</li><li>👥 <strong>拼车消息</strong>：拼车房间状态变化</li><li>📢 <strong>系统公告</strong>：平台重要通知</li></ul><p><strong>⚙️ 自定义设置：</strong></p><p>可在"设置-通知管理"中开关各类通知</p></div>`,
    actions: [{ label: '🔔 通知设置', route: '/user/settings/notification', type: 'primary' }],
    showFeedback: true
  },
  '退款到账|多久到账|到账时间': {
    answer: `<div class="kb-answer"><p><strong>💰 退款到账时间：</strong></p><ul><li>💳 <strong>微信/支付宝</strong>：1-3个工作日</li><li>🏦 <strong>银行卡</strong>：3-7个工作日</li><li>💵 <strong>余额支付</strong>：即时到账</li></ul><p><strong>📋 查看退款进度：</strong></p><ol><li>进入"我的预约"</li><li>找到对应订单</li><li>点击查看退款详情</li></ol><p><strong>⚠️ 注意：</strong>退款审核通过后开始计算到账时间</p></div>`,
    actions: [{ label: '📋 我的订单', route: '/user/reservations', type: 'primary' }],
    showFeedback: true
  },
  '你好|您好|hi|hello|在吗': {
    answer: `<div class="kb-answer"><p>您好！👋 我是AI客服小剧，很高兴为您服务！🎭</p><p><strong>我可以帮您：</strong></p><ul><li>📅 了解预约流程</li><li>💰 咨询退款政策</li><li>🎫 使用优惠券</li><li>🏪 查找门店信息</li><li>👥 了解拼车组队</li></ul><p>请问有什么可以帮您的呢？😊</p></div>`,
    showFeedback: false
  },
  '谢谢|感谢|多谢|太棒了': {
    answer: `<div class="kb-answer"><p>不客气！很高兴能帮到您！😊</p><p>如果还有其他问题，随时可以问我哦～</p><p><strong>🎭 祝您游戏愉快！</strong></p></div>`,
    showFeedback: false
  },
  'default': {
    answer: `<div class="kb-answer"><p>🤔 抱歉，我没有完全理解您的问题。</p><p><strong>您可以尝试：</strong></p><ul><li>💡 点击下方快捷问题</li><li>🔄 换个方式描述问题</li><li>📞 联系人工客服：<strong>400-123-4567</strong></li></ul></div>`,
    suggestions: ['如何预约剧本杀？', '如何申请退款？', '联系人工客服'],
    showFeedback: false
  }
}

// 打开聊天
const toggleChat = () => {
  isOpen.value = true
  unreadCount.value = 0
  if (messages.value.length === 0) {
    setTimeout(() => {
      const greeting = getGreeting()
      addAIMessage(`${greeting} 我是AI客服小剧，很高兴为您服务！🎭<br><br>我可以帮您解答预约、退款、优惠券等问题。请问有什么可以帮您的？`, [], true)
    }, 300)
  }
}

const closeChat = () => { isOpen.value = false }
const minimizeChat = () => { isOpen.value = false }

const clearHistory = () => {
  ElMessageBox.confirm('确定要清空聊天记录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    messages.value = []
    ElMessage.success('已清空聊天记录')
  }).catch(() => {})
}

const askQuestion = (question) => {
  inputMessage.value = question
  showQuickQuestions.value = false
  sendMessage()
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return
  const userMessage = inputMessage.value.trim()
  addUserMessage(userMessage)
  inputMessage.value = ''
  showQuickQuestions.value = false
  isTyping.value = true

  try {
    const { sendMessage: sendAIMessage } = await import('@/api/ai')
    const response = await sendAIMessage({
      message: userMessage,
      context: { page: router.currentRoute.value.path }
    })
    await delay(800)
    if (response.data && response.data.reply) {
      addAIMessage(response.data.reply.replace(/\n/g, '<br>'), [], true)
    } else {
      throw new Error('Invalid response')
    }
  } catch (error) {
    console.error('AI API调用失败:', error)
    await delay(600)
    const { answer, actions, showFeedback } = getKnowledgeResponse(userMessage)
    addAIMessage(answer, actions || [], showFeedback)
  } finally {
    isTyping.value = false
  }
}

const getKnowledgeResponse = (question) => {
  for (const [keywords, response] of Object.entries(knowledgeBase)) {
    if (keywords === 'default') continue
    if (keywords.split('|').some(k => question.toLowerCase().includes(k.toLowerCase()))) {
      return response
    }
  }
  return knowledgeBase.default
}

const addUserMessage = (content) => {
  const msg = { 
    id: Date.now(), 
    type: 'user', 
    content, 
    time: getCurrentTime(),
    status: 'sending'
  }
  messages.value.push(msg)
  scrollToBottom()
  // 模拟发送成功
  setTimeout(() => { msg.status = 'sent' }, 300)
  return msg
}

const addAIMessage = (content, actions = [], showFeedback = false, suggestions = []) => {
  const msg = { 
    id: Date.now(), 
    type: 'ai', 
    content, 
    actions, 
    suggestions,
    showFeedback, 
    feedbackGiven: false, 
    time: getCurrentTime() 
  }
  messages.value.push(msg)
  if (!isOpen.value) unreadCount.value++
  scrollToBottom()
  saveCurrentSession()
  return msg
}

// 重发消息
const resendMessage = (msg) => {
  const content = msg.content
  const index = messages.value.findIndex(m => m.id === msg.id)
  if (index > -1) {
    messages.value.splice(index, 1)
  }
  inputMessage.value = content
  sendMessage()
}

// 给反馈
const giveFeedback = async (msgId, helpful, rating = 3) => {
  const msg = messages.value.find(m => m.id === msgId)
  if (msg) {
    msg.feedbackGiven = true
    try {
      await submitFeedbackAPI({ messageId: String(msgId), rating, comment: helpful ? '有帮助' : '没帮助' })
    } catch (e) {
      console.error('提交反馈失败:', e)
    }
    ElMessage.success(helpful ? '感谢您的认可！' : '感谢反馈，我们会继续改进！')
  }
}

const handleAction = (action) => {
  if (action.route) {
    router.push(action.route)
    closeChat()
  } else if (action.type === 'copy') {
    copyToClipboard(action.content)
  }
}

// 复制消息
const copyMessage = (msg) => {
  const text = msg.content.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
  copyToClipboard(text)
}

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 朗读消息
let speechSynthesis = null
const speakMessage = (msg) => {
  if (speakingMsgId.value === msg.id) {
    // 停止朗读
    window.speechSynthesis?.cancel()
    speakingMsgId.value = null
    return
  }
  
  const text = msg.content.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1
    utterance.onend = () => { speakingMsgId.value = null }
    utterance.onerror = () => { speakingMsgId.value = null }
    speakingMsgId.value = msg.id
    window.speechSynthesis.speak(utterance)
  } else {
    ElMessage.warning('您的浏览器不支持语音朗读')
  }
}

// 插入表情
const insertEmoji = (emoji) => {
  inputMessage.value += emoji
  inputRef.value?.focus()
}

// 输入变化处理
const handleInputChange = () => {
  // 可以添加输入建议等功能
}

// 滚动处理
const handleScroll = () => {
  if (!messagesContainer.value) return
  const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
  showScrollBottom.value = scrollHeight - scrollTop - clientHeight > 100
}

// 全屏切换
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

// 加载更多消息
const loadMoreMessages = async () => {
  if (loadingMore.value) return
  loadingMore.value = true
  // 模拟加载
  await delay(500)
  loadingMore.value = false
  hasMoreMessages.value = false
}

// ========== 会话历史管理 ==========
const STORAGE_KEY = 'ai_chat_sessions'

const generateSessionId = () => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

const loadSessions = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      chatSessions.value = JSON.parse(data)
    }
  } catch (e) {
    console.error('加载会话历史失败:', e)
  }
}

const saveSessions = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chatSessions.value))
  } catch (e) {
    console.error('保存会话历史失败:', e)
  }
}

const saveCurrentSession = () => {
  if (!currentSessionId.value || messages.value.length === 0) return
  
  const existingIndex = chatSessions.value.findIndex(s => s.id === currentSessionId.value)
  const sessionData = {
    id: currentSessionId.value,
    title: getSessionTitle(),
    messages: messages.value.slice(-50), // 最多保存50条
    timestamp: Date.now()
  }
  
  if (existingIndex > -1) {
    chatSessions.value[existingIndex] = sessionData
  } else {
    chatSessions.value.unshift(sessionData)
  }
  
  // 最多保存10个会话
  if (chatSessions.value.length > 10) {
    chatSessions.value = chatSessions.value.slice(0, 10)
  }
  
  saveSessions()
}

const getSessionTitle = () => {
  const userMsgs = messages.value.filter(m => m.type === 'user')
  if (userMsgs.length > 0) {
    const firstMsg = userMsgs[0].content
    return firstMsg.length > 20 ? firstMsg.substring(0, 20) + '...' : firstMsg
  }
  return '新会话'
}

const loadSession = (sessionId) => {
  const session = chatSessions.value.find(s => s.id === sessionId)
  if (session) {
    messages.value = [...session.messages]
    currentSessionId.value = sessionId
    showHistoryPanel.value = false
    scrollToBottom()
  }
}

const deleteSession = (sessionId) => {
  chatSessions.value = chatSessions.value.filter(s => s.id !== sessionId)
  saveSessions()
  if (currentSessionId.value === sessionId) {
    startNewSession()
  }
}

const startNewSession = () => {
  saveCurrentSession()
  messages.value = []
  currentSessionId.value = generateSessionId()
  showHistoryPanel.value = false
  
  // 显示欢迎消息
  setTimeout(() => {
    const greeting = getGreeting()
    addAIMessage(`${greeting} 我是AI客服小剧，很高兴为您服务！🎭<br><br>我可以帮您解答预约、退款、优惠券等问题。请问有什么可以帮您的？`, [], true)
  }, 300)
}

const clearAllHistory = () => {
  ElMessageBox.confirm('确定要清空所有历史会话吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    chatSessions.value = []
    saveSessions()
    ElMessage.success('已清空所有历史会话')
  }).catch(() => {})
}

const formatSessionTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// ========== 工具方法 ==========
const getGreeting = () => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，'
  if (h < 9) return '早上好！'
  if (h < 12) return '上午好！'
  if (h < 14) return '中午好！'
  if (h < 18) return '下午好！'
  if (h < 22) return '晚上好！'
  return '夜深了，'
}

const getCurrentTime = () => {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      newMessageCount.value = 0
      showScrollBottom.value = false
    }
  })
}

// ========== 生命周期 ==========
onMounted(() => {
  // 加载历史会话
  loadSessions()
  
  // 初始化会话ID
  if (!currentSessionId.value) {
    currentSessionId.value = generateSessionId()
  }
  
  // 延迟显示未读提示
  setTimeout(() => {
    if (!isOpen.value && messages.value.length === 0) {
      unreadCount.value = 1
    }
  }, 5000)
})

onUnmounted(() => {
  // 保存当前会话
  saveCurrentSession()
  // 停止语音
  window.speechSynthesis?.cancel()
})
</script>

<style scoped>
.ai-customer-service {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 9999;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 悬浮球 - 剧本杀主题 */
.service-bubble {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #1a1a2e 0%, #8B0000 50%, #1a1a2e 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  border: 2px solid rgba(212, 175, 55, 0.4);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 20px rgba(139, 0, 0, 0.5), 0 0 40px rgba(139, 0, 0, 0.2);
}

.bubble-glow {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid rgba(139, 0, 0, 0.4);
  animation: pulse-ring 2s ease-out infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.3); opacity: 0; }
}

.service-bubble:hover {
  transform: scale(1.1) rotate(10deg);
  box-shadow: 0 8px 30px rgba(139, 0, 0, 0.6), 0 0 60px rgba(139, 0, 0, 0.3);
}

.service-bubble.has-unread {
  animation: shake 0.5s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
}

.service-icon {
  font-size: 28px;
  color: #ffd700;
  filter: drop-shadow(0 0 8px rgba(255, 215, 0, 0.5));
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: linear-gradient(135deg, #ff4757, #ff6b81);
  color: white;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  border: 2px solid #1a1a2e;
}

.bubble-tip {
  position: absolute;
  bottom: -35px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(26, 26, 46, 0.95);
  color: #ffd700;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  transition: all 0.3s;
  border: 1px solid rgba(139, 0, 0, 0.5);
}

.service-bubble:hover .bubble-tip {
  opacity: 1;
  bottom: -40px;
}

/* 聊天窗口 */
.chat-window {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 400px;
  height: 600px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 50px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(139, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部 */
.chat-header {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 2px solid #8B0000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-wrapper {
  position: relative;
}

.avatar-glow {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8B0000, #ffd700);
  animation: avatar-glow 2s ease-in-out infinite;
  opacity: 0.6;
}

@keyframes avatar-glow {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.05); }
}

.bot-avatar {
  background: linear-gradient(135deg, #8B0000, #a01010);
  font-size: 24px;
  position: relative;
  z-index: 1;
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background: #52c41a;
  border-radius: 50%;
  border: 2px solid #1a1a2e;
  z-index: 2;
}

.header-info {
  color: #fff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffd700;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #52c41a;
  border-radius: 50%;
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.header-actions {
  display: flex;
  gap: 4px;
}

.header-actions .el-button {
  color: rgba(255, 255, 255, 0.7);
}

.header-actions .el-button:hover {
  color: #ffd700;
  background: rgba(255, 255, 255, 0.1);
}

/* 快捷标签 */
.quick-tags {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  overflow-x: auto;
}

.quick-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s;
  font-size: 13px;
}

.quick-tag:hover {
  border-color: #8B0000;
  color: #8B0000;
  background: #fff5f5;
}

.tag-icon {
  font-size: 14px;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: linear-gradient(180deg, #fafafa 0%, #fff 100%);
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(139, 0, 0, 0.2);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(139, 0, 0, 0.4);
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  background: linear-gradient(135deg, #8B0000, #a01010);
  font-size: 18px;
}

.message-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 14px;
}

.ai .message-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #eee;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user .message-bubble {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-feedback {
  display: flex;
  align-items: center;
  gap: 4px;
}

.feedback-label {
  font-size: 12px;
  color: #999;
}

.message-feedback .el-button {
  font-size: 12px;
  padding: 2px 6px;
}

.feedback-thanks {
  font-size: 12px;
  color: #52c41a;
}

.message-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.message-actions .el-button {
  border-radius: 16px;
  font-size: 12px;
}

/* AI输入动画 */
.typing-indicator {
  opacity: 0.9;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  background: #8B0000;
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.typing-text {
  font-size: 13px;
  color: #666;
}

/* 快捷问题面板 */
.quick-questions {
  background: #fff;
  border-top: 1px solid #eee;
  max-height: 200px;
  overflow-y: auto;
}

.quick-questions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fafafa;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  border-bottom: 1px solid #eee;
}

.quick-questions-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 12px;
}

.quick-question-item {
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.quick-question-item:hover {
  background: #fff5f5;
  color: #8B0000;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.1);
}

/* 输入区域 */
.chat-input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eee;
}

.input-tools {
  display: flex;
  gap: 4px;
}

.input-tools .el-button {
  color: #666;
}

.input-tools .el-button:hover {
  color: #8B0000;
}

.chat-input .el-input {
  flex: 1;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  padding: 8px 16px;
}

.chat-input :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(139, 0, 0, 0.2);
}

.send-btn {
  border-radius: 20px;
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  border: none;
}

.send-btn:hover {
  background: linear-gradient(135deg, #a01010 0%, #8B0000 100%);
}

/* 底部提示 */
.chat-footer {
  padding: 8px 16px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #eee;
}

/* 知识库回答样式 */
:deep(.kb-answer) {
  line-height: 1.8;
}

:deep(.kb-answer p) {
  margin: 8px 0;
}

:deep(.kb-answer strong) {
  color: #8B0000;
}

:deep(.kb-answer ol),
:deep(.kb-answer ul) {
  margin: 8px 0;
  padding-left: 20px;
}

:deep(.kb-answer li) {
  margin: 4px 0;
}

/* 过渡动画 */
.bounce-enter-active {
  animation: bounce-in 0.5s;
}

.bounce-leave-active {
  animation: bounce-in 0.3s reverse;
}

@keyframes bounce-in {
  0% { transform: scale(0); opacity: 0; }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 480px) {
  .ai-customer-service {
    bottom: 20px;
    right: 20px;
  }

  .service-bubble {
    width: 54px;
    height: 54px;
  }

  .service-icon {
    font-size: 24px;
  }

  .chat-window {
    width: calc(100vw - 20px);
    height: calc(100vh - 100px);
    right: 10px;
    bottom: 10px;
    border-radius: 16px;
  }

  .quick-questions-list {
    grid-template-columns: 1fr;
  }
}

/* ========== 新增功能样式 ========== */

/* 全屏模式 */
.chat-window.fullscreen {
  width: 100vw !important;
  height: 100vh !important;
  right: 0 !important;
  bottom: 0 !important;
  border-radius: 0 !important;
}

/* 历史会话面板 */
.history-panel {
  background: #fff;
  border-bottom: 1px solid #eee;
  max-height: 250px;
  overflow-y: auto;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
  font-size: 13px;
  font-weight: 500;
}

.history-list {
  padding: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.history-item:hover {
  background: #f5f5f5;
}

.history-item.active {
  background: #fff5f5;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.history-item-title {
  flex: 1;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-time {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
}

.history-item-delete {
  opacity: 0;
  margin-left: 8px;
}

.history-item:hover .history-item-delete {
  opacity: 1;
}

.history-empty {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.new-session-btn {
  width: calc(100% - 16px);
  margin: 8px;
}

/* 消息工具按钮 */
.message-tools {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-tools {
  opacity: 1;
}

.message-tools .el-button {
  padding: 2px 6px;
  font-size: 12px;
}

.message-tools .el-button.speaking {
  color: #8B0000;
  animation: pulse 1s infinite;
}

/* 消息建议标签 */
.message-suggestions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.suggestions-label {
  font-size: 12px;
  color: #999;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-tag:hover {
  background: #fff5f5;
  border-color: #8B0000;
  color: #8B0000;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 10px;
  color: #666;
  font-size: 12px;
  cursor: pointer;
}

.load-more:hover {
  color: #8B0000;
}

/* 滚动到底部按钮 */
.scroll-bottom-btn {
  position: sticky;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 10px rgba(139, 0, 0, 0.3);
  z-index: 10;
  width: fit-content;
  margin: 0 auto;
}

.scroll-bottom-btn:hover {
  transform: translateX(-50%) scale(1.05);
}

/* 快捷问题分类标签 */
.quick-questions-tabs {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.quick-questions-tabs :deep(.el-radio-button__inner) {
  padding: 6px 12px;
  font-size: 12px;
}

/* 表情选择器 */
.emoji-picker {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
}

.emoji-item {
  font-size: 20px;
  padding: 4px;
  text-align: center;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.emoji-item:hover {
  background: #f5f5f5;
  transform: scale(1.2);
}

/* 输入工具按钮激活状态 */
.input-tools .el-button.active {
  color: #8B0000;
  background: rgba(139, 0, 0, 0.1);
}

/* 消息高亮 */
.message-item.highlight .message-bubble {
  animation: highlight-pulse 1s ease-out;
}

@keyframes highlight-pulse {
  0% { box-shadow: 0 0 0 0 rgba(139, 0, 0, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(139, 0, 0, 0); }
  100% { box-shadow: 0 0 0 0 rgba(139, 0, 0, 0); }
}

/* 滑动动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-10px);
  opacity: 0;
  max-height: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  max-height: 250px;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 消息状态图标 */
.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.message-meta .el-icon {
  font-size: 14px;
}

/* 用户消息右对齐元数据 */
.user .message-meta {
  justify-content: flex-end;
}
</style>
