/**
 * AI服务工具类
 * 支持多种AI服务提供商
 */

import { sendMessage } from '@/api/ai'

class AIService {
  constructor() {
    this.sessionId = this.generateSessionId()
    this.conversationHistory = []
    this.maxHistoryLength = 10 // 保留最近10轮对话
  }

  /**
   * 生成会话ID
   */
  generateSessionId() {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  /**
   * 发送消息并获取AI回复
   * @param {String} message - 用户消息
   * @param {Object} context - 上下文信息
   */
  async chat(message, context = {}) {
    // 优先检测转人工意图，直接触发转接而不走 AI
    const transferKeywords = ['转人工', '人工客服', '转客服', '人工服务', '真人客服']
    if (transferKeywords.some(kw => message.includes(kw))) {
      return {
        success: true,
        message: '好的，马上为您转接人工客服，请稍等...',
        suggestions: [],
        actions: [{ type: 'transfer', label: '点击转接人工客服' }],
        fallback: false,
        triggerTransfer: true  // 前端收到此标记直接调用 transferToHuman
      }
    }

    try {
      // 添加到历史记录
      this.conversationHistory.push({
        role: 'user',
        content: message,
        timestamp: Date.now()
      })

      // 限制历史记录长度
      if (this.conversationHistory.length > this.maxHistoryLength * 2) {
        this.conversationHistory = this.conversationHistory.slice(-this.maxHistoryLength * 2)
      }

      // 调用后端API
      const response = await sendMessage({
        message,
        history: this.conversationHistory,
        sessionId: this.sessionId,
        context: {
          ...context,
          userInfo: this.getUserInfo()
        }
      })

      // 兼容多种响应格式
      const isSuccess = response.code === 200 || response.code === 1
      const responseData = response.data || response
      const aiResponse = responseData.reply || responseData.message || responseData.content

      if (isSuccess && aiResponse) {
        // 添加AI回复到历史
        this.conversationHistory.push({
          role: 'assistant',
          content: aiResponse,
          timestamp: Date.now()
        })

        return {
          success: true,
          message: aiResponse,
          suggestions: responseData.suggestions || [],
          actions: responseData.actions || [],
          fallback: false,
          triggerTransfer: responseData.triggerTransfer || false
        }
      } else {
        console.warn('AI API返回无效响应，使用本地知识库:', response)
        return this.getFallbackResponse(message)
      }
    } catch (error) {
      console.error('AI服务调用失败:', error)
      return this.getFallbackResponse(message)
    }
  }

  /**
   * 获取用户信息（用于个性化回复）
   */
  getUserInfo() {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        return {
          id: user.id,
          nickname: user.nickname,
          vipLevel: user.vipLevel
        }
      } catch (e) {
        return null
      }
    }
    return null
  }

  /**
   * 降级方案：本地知识库匹配
   */
  getFallbackResponse(message) {
    const knowledgeBase = this.getLocalKnowledgeBase()
    
    for (let [keywords, response] of Object.entries(knowledgeBase)) {
      const keywordList = keywords.split('|')
      if (keywordList.some(kw => message.includes(kw))) {
        return {
          success: true,
          message: response.answer,
          suggestions: response.suggestions || [],
          fallback: true
        }
      }
    }

    // 没有匹配到，返回默认回复
    return {
      success: true,
      message: `抱歉，我暂时无法理解您的问题。您可以：
        
1. 📞 拨打客服热线：400-123-4567
2. 💬 查看帮助中心获取更多信息
3. 📧 发送邮件：service@jubensha.com

请尝试换个方式描述您的问题，或直接联系人工客服。`,
      suggestions: [
        '如何预约剧本？',
        '支持哪些支付方式？',
        '如何申请退款？',
        '联系人工客服'
      ],
      fallback: true
    }
  }

  /**
   * 本地知识库（作为降级方案）
   */
  getLocalKnowledgeBase() {
    return {
      '预约|怎么预约|如何预约': {
        answer: `📋 **预约剧本流程：**

1. 浏览剧本列表，选择喜欢的剧本
2. 点击"立即预约"按钮
3. 选择门店、日期和时间
4. 填写联系信息和参与人数
5. 选择优惠券并完成支付

💡 **温馨提示：**
• 建议提前3-7天预约，避免档期紧张
• 热门剧本和周末档期更需提前预约
• 确认人数后再预约，避免临时改动`,
        suggestions: ['查看热门剧本', '附近的门店', '优惠券使用']
      },
      '退款|取消|退订': {
        answer: `💰 **退款政策：**

• 预约前24小时以上取消：全额退款 ✅
• 预约前12-24小时取消：退款80% ⚠️
• 预约前6-12小时取消：退款50% ⚠️
• 预约前6小时内取消：不予退款 ❌

📝 **申请退款步骤：**
1. 进入"个人中心-我的预约"
2. 找到对应订单，点击"申请退款"
3. 选择退款原因并提交
4. 等待审核（1-3个工作日）
5. 审核通过后1-7个工作日到账`,
        suggestions: ['查看我的预约', '联系客服', '退款进度查询']
      },
      '支付|付款|支付方式': {
        answer: `💳 **支持的支付方式：**

• 💰 支付宝支付
• 🎁 优惠券抵扣
• ⭐ 积分兑换

🔒 **安全保障：**
所有支付均采用SSL加密传输，安全可靠！`,
        suggestions: ['优惠券在哪里', '如何使用积分', '支付失败怎么办']
      },
      '人工|客服|联系|转人工': {
        answer: `📞 **联系我们：**

• ☎️ 客服热线：400-123-4567
• ⏰ 服务时间：9:00-22:00（全年无休）
• 📧 邮箱：service@jubensha.com
• 💬 微信公众号：剧本杀预约平台

💡 非紧急问题可以先问我哦~`,
        suggestions: ['转接人工客服', '查看营业时间', '获取联系方式']
      },
      '热门|推荐|好玩': {
        answer: `🔥 **热门剧本推荐：**

• 🎭 **情感本**：《年轮》《云边有个小卖部》- 适合沉浸体验
• 🔍 **硬核本**：《东方快车谋杀案》《无人生还》- 适合推理爱好者
• 😄 **欢乐本**：《奇妙物语》- 适合新手和聚会
• 😱 **恐怖本**：《死者之书》- 适合喜欢刺激的玩家

💡 可以根据人数、时长、风格筛选剧本！`,
        suggestions: ['查看全部剧本', '新手推荐', '按人数筛选']
      },
      '优惠券|优惠|折扣': {
        answer: `🎫 **优惠券使用说明：**

• 💰 支付时自动展示可用优惠券
• 📋 每笔订单限用一张优惠券
• ⏰ 注意查看优惠券有效期

🎁 **获取优惠券的方式：**
• 新用户注册礼包
• 每日签到奖励
• 积分兑换
• 参与平台活动`,
        suggestions: ['我的优惠券', '如何获取优惠券', '积分兑换']
      },
      '积分|点数|兑换': {
        answer: `⭐ **积分用途：**

• 💰 抵扣现金（100积分=1元）
• 🎫 兑换优惠券
• 🎁 兑换精美周边

📈 **获取积分的方式：**
• 完成预约并游戏 +50积分
• 发表评价 +20积分
• 每日签到 +5积分
• 邀请好友注册 +100积分`,
        suggestions: ['我的积分', '积分商城', '签到领积分']
      },
      'VIP|会员|特权': {
        answer: `👑 **VIP会员特权：**

• 💰 专属折扣（最高8折）
• 🎫 每月专属优惠券
• ⭐ 积分加成（最高2倍）
• 🎯 优先预约热门剧本
• 🎁 生日专属礼包`,
        suggestions: ['开通VIP', '会员等级', 'VIP价格']
      },
      '拼车|组队|凑人': {
        answer: `👥 **拼车组队功能：**

• 🎯 人数不够？发起拼车让其他玩家加入
• 👀 浏览已有的拼车房间
• 💬 房间内可以聊天交流
• ✅ 人齐后一起支付确认

💡 热门剧本拼车成功率更高！`,
        suggestions: ['发起拼车', '拼车大厅', '如何加入拼车']
      },
      '门店|店铺|附近': {
        answer: `🏪 **查找门店方式：**

• 📍 首页点击"附近门店"自动定位
• 🔍 搜索门店名称或地区
• 🗺️ 地图模式查看周边门店

💡 **选店建议：**
• 查看门店评分和评价
• 关注门店的剧本库存
• 对比不同门店价格`,
        suggestions: ['附近门店', '搜索门店', '门店排行']
      },
      '你好|您好|hi|hello': {
        answer: `您好！👋 我是AI客服小剧，很高兴为您服务！🎭

**我可以帮您：**
• 📅 了解预约流程
• 💰 咨询退款政策
• 🎫 使用优惠券
• 🏪 查找门店信息
• 👥 了解拼车组队

请问有什么可以帮您的呢？😊`,
        suggestions: ['如何预约', '退款政策', '优惠券使用']
      },
      '谢谢|感谢|多谢': {
        answer: `不客气！很高兴能帮到您！😊

如果还有其他问题，随时可以问我哦～

🎭 **祝您游戏愉快！**`,
        suggestions: []
      },
      '怎么玩|如何玩|玩法|规则|新手|第一次': {
        answer: `🎭 **剧本杀玩法介绍：**

1. 📖 **领取剧本**：到店后每人获得专属角色剧本
2. 🔍 **阅读剧情**：了解自己角色的背景和任务
3. 💬 **自由交流**：与其他玩家交流、搜证、推理
4. 🗳️ **投票指凶**：在规定时间内找出凶手
5. 🎬 **复盘揭秘**：DM主持揭晓真相

💡 **新手建议：**
• 选择「欢乐本」或「新手本」入门
• 认真阅读剧本，记住关键线索
• 大胆发言，积极参与互动
• 全程约3-5小时，建议留足时间`,
        suggestions: ['推荐新手剧本', '如何预约', '拼车组队']
      },
      '团体|团建|多人|包场|公司|聚会|生日': {
        answer: `🎉 **团体预约服务：**

• 👥 **人数范围**：6-20人均可接待
• 🎭 **包场服务**：10人以上可申请包场
• 💰 **团体优惠**：10人以上享9折，20人以上享8.5折
• 🎂 **生日特权**：寿星免费参与，赠送专属道具

📝 **预约方式：**
1. 在预约页面选择人数
2. 备注「团体预约」
3. 客服会主动联系确认

📞 也可直接致电：**400-123-4567**`,
        suggestions: ['立即预约', '查看适合团建的剧本', '联系客服']
      },
      '投诉|评价|差评|不满意|纠纷|举报': {
        answer: `📝 **评价与投诉渠道：**

**发表评价：**
• 游戏结束后在「我的预约」中评价
• 评价可获得 **+20积分** 奖励

**意见反馈：**
• 首页底部「意见反馈」提交
• 反馈后1个工作日内回复

**投诉处理：**
• 📞 客服热线：400-123-4567
• 📧 邮箱：complaint@jubensha.com
• ⏰ 处理时效：3个工作日内

我们重视每一条反馈，感谢您的监督！`,
        suggestions: ['我的预约', '联系客服', '意见反馈']
      }
    }
  }

  /**
   * 清除对话历史
   */
  clearHistory() {
    this.conversationHistory = []
    this.sessionId = this.generateSessionId()
  }

  /**
   * 获取对话历史
   */
  getHistory() {
    return this.conversationHistory
  }
}

// 导出单例
export default new AIService()
