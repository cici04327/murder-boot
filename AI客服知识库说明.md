# AI 客服知识库说明文档

> 本文档说明「剧本杀预约与门店管理平台」AI 客服的整体架构、知识库组成、使用方式及维护指南。

---

## 一、AI 客服整体架构

本平台 AI 客服采用 **「DeepSeek 大模型 + 前端本地兜底」混合模式**，分为三层知识体系：

```
用户发消息
    ↓
前端 AICustomerService.vue
    ↓
POST /api/ai/chat（携带消息 + 历史 + 用户信息 + 当前页面）
    ↓
后端 AIServiceImpl.buildSystemPrompt()
  → 拼装平台业务知识 + 用户上下文
    ↓
DeepSeek 官方 API（deepseek-chat 模型）
  → 返回真实 AI 回答
    ↓
如果 DeepSeek 调用失败
    ↓
前端 knowledgeBase 关键词匹配（兜底）
  → 返回固定问答内容
```

---

## 二、知识库组成（三层）

### 第一层：后端系统提示词（核心知识库）

**文件位置：**
```
src/main/java/com/murder/service/impl/AIServiceImpl.java
方法：buildSystemPrompt(Map<String, Object> context)
```

**作用：** 每次用户发消息时，将平台所有业务知识作为 `system` 角色的提示词发给 DeepSeek，让 AI 在回答前先"学习"本平台的真实业务。

**包含内容：**

| 板块 | 说明 |
|---|---|
| 身份定位 | AI 是本平台专属客服，不能虚构平台不存在的功能 |
| 平台角色体系 | 用户 / 系统管理员 / 门店管理员 / 门店员工（MANAGER/DM/CLERK）及权限 |
| 预约完整流程 | 9步预约流程 + 4种预约状态说明 |
| 退款规则 | 24h以上全退 / 12-24h退80% / 6-12h退50% / 6h内不退 / 已核销不退 |
| 改期规则 | 24h以上免费改一次 / 12h内不支持 |
| VIP体系 | 4个等级的折扣/月度券数量面额/生日礼券金额 |
| 积分体系 | 签到+10 / 游戏+100 / 收藏+20 / VIP倍率 / 兑换规则 |
| 优惠券 | 三种类型 / 获取方式 / 使用与叠加规则 |
| 拼团 | 发起/加入/自动拼团逻辑 |
| 支付方式 | 仅支持支付宝 |
| 通知体系 | 4类通知场景 |
| 人工客服 | 热线/邮件/服务时间/转人工方式 |
| 动态页面感知 | 根据用户当前所在页面调整回答重点 |
| 用户信息 | 用户昵称 + VIP等级（由前端实时传入） |
| 回答原则 | 7条规则，包含"不虚构功能"、"严格按平台数字"等 |

---

### 第二层：前端本地知识库（兜底）

**文件位置：**
```
frontend/user/src/components/AICustomerService.vue
对象：const knowledgeBase = { ... }
函数：getKnowledgeResponse(message)
```

**作用：** 当 DeepSeek API 调用失败时，通过关键词匹配返回固定答案，保证用户不会看到报错。

**工作方式：**
- 左边是正则关键词，比如 `预约|怎么预约|如何预约`
- 右边是对应的回答内容（HTML 格式）+ 快捷操作按钮

**已收录的 20 个问答类目：**

| 关键词 | 回答内容 |
|---|---|
| 预约/怎么预约/如何预约 | 5步预约流程 + 跳转按钮 |
| 支付/付款/支付方式 | 仅支付宝 + VIP/优惠券叠加说明 |
| 退款/退钱/取消预约 | 退款规则（24h/12-24h/6-12h/6h内）+ 流程 |
| 优惠券/优惠/折扣 | 使用规则 + 获取方式 |
| 积分/点数/兑换 | 积分获取和兑换规则 |
| 密码/登录/注册 | 找回密码步骤 |
| 门店/附近/地址 | 查找门店方式 |
| 拼车/组队/凑人 | 拼团功能说明 |
| 新手/第一次/怎么玩 | 剧本杀入门指南 |
| 人工/转人工/联系客服 | 热线/邮件 + 转接按钮 |
| VIP/会员/开通/特权 | 4个等级详细介绍 |
| 热门/推荐/好玩/排行 | 热门剧本推荐引导 |
| 修改时间/改期 | 改期规则 + 步骤 |
| 人数不够/凑人 | 拼团解决方案 |
| 迟到/晚到 | 迟到处理说明 |
| 评价/评论/打分 | 评价说明 + 积分奖励 |
| 收藏/喜欢/关注 | 收藏功能说明 |
| 手机号/换号 | 修改手机号流程 |
| 通知/消息/提醒 | 通知类型说明 |
| 退款到账/多久到账 | 支付宝1-3工作日 |
| 你好/您好/hi | 欢迎语 |
| 谢谢/感谢 | 感谢回复 |
| （默认兜底） | 未识别时的引导回复 |

---

### 第三层：对话历史上下文（动态知识）

**文件位置：**
```
frontend/user/src/components/AICustomerService.vue
函数：buildHistoryPayload()
```

**作用：** 每次发消息时，把最近 10 轮对话历史和用户信息一起传给后端，让 DeepSeek 具备连续对话能力。

**传给后端的数据结构：**
```json
{
  "message": "用户这句话",
  "sessionId": "当前会话ID",
  "history": [
    { "role": "user", "content": "上一句用户问的话" },
    { "role": "assistant", "content": "AI上一次的回答" }
  ],
  "context": {
    "page": "/vip",
    "userInfo": {
      "nickname": "用户昵称",
      "vipLevel": 2
    }
  }
}
```

---

## 三、AI 配置说明

**文件位置：**
```
src/main/resources/application.yml
```

**当前配置：**
```yaml
ai:
  provider: deepseek              # 使用 DeepSeek 官方 API
  api-key: sk-xxxx                # DeepSeek 官方 API Key
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat            # 推荐使用 deepseek-chat（稳定、快速）
  temperature: 0.7                # 回答随机性（0~1，越低越保守）
  max-tokens: 1000                # 单次最大回复 token 数
```

**可选模型：**
| 模型名 | 特点 | 推荐场景 |
|---|---|---|
| `deepseek-chat` | 通用对话，速度快，稳定 | 客服场景（推荐） |
| `deepseek-reasoner` | 推理能力强，速度较慢 | 复杂问题分析 |

---

## 四、VIP 等级对照表（供知识库维护参考）

| 等级 | 名称 | 折扣 | 月度体验券 | 生日礼券 |
|---|---|---|---|---|
| Lv.1 | 见习侦探 | 9.5折 | 2张 × 10元 | 30元 |
| Lv.2 | 银章侦探 | 9折 | 5张 × 20元 | 80元 |
| Lv.3 | 金章侦探 | 8.5折 | 10张 × 50元 | 150元 |
| Lv.4 | 传奇侦探 | 8折 | 15张 × 100元 | 200元 |

---

## 五、退款规则对照表（供知识库维护参考）

| 取消时间 | 退款比例 |
|---|---|
| 游戏开始前 24小时以上 | 全额退款 ✅ |
| 游戏开始前 12~24小时 | 退款 80% ⚠️ |
| 游戏开始前 6~12小时 | 退款 50% ⚠️ |
| 游戏开始前 6小时内 | 不退款 ❌ |
| 已到店核销后 | 不退款 ❌ |

退款到账：支付宝原路退回，1-3个工作日到账。

---

## 六、积分规则对照表（供知识库维护参考）

| 行为 | 积分 |
|---|---|
| 每日签到 | +10 积分 |
| 完成预约游戏 | +100 积分 |
| 收藏剧本达到里程碑 | +20 积分/次 |
| VIP 倍率加成 | 按等级配置 |

积分用途：兑换优惠券（每种券每天限兑一次）。

---

## 七、如何维护和修改知识库

### 修改 AI 的业务知识（最常改的地方）

**文件：** `src/main/java/com/murder/service/impl/AIServiceImpl.java`  
**方法：** `buildSystemPrompt()`

直接在对应板块修改文字即可。修改后需要重启后端服务。

**示例：** 如果你要修改退款规则，找到这段：
```java
prompt.append("- 游戏开始前 24小时以上：全额退款 ✅\n");
prompt.append("- 游戏开始前 12~24小时：退款80% ⚠️\n");
```
改成你最新的规则，保存后重启即可。

---

### 修改前端兜底问答

**文件：** `frontend/user/src/components/AICustomerService.vue`  
**对象：** `const knowledgeBase = { ... }`

**新增一个问答：**
```js
const knowledgeBase = {
  // 已有的 ...
  
  // 新增示例：处理"活动"关键词
  '活动|优惠活动|限时': {
    answer: `<div class="kb-answer"><p>您可以在首页查看最新活动信息 🎉</p></div>`,
    actions: [{ label: '🏠 去首页', route: '/', type: 'primary' }],
    showFeedback: true
  },
}
```

修改后需要重新构建前端：
```bash
npm --prefix frontend/user run build
```

---

### 修改 AI 回答风格

在 `buildSystemPrompt()` 末尾找到"回答原则"这段，修改对应条目即可。

**示例：** 如果你希望 AI 回答更简短：
```java
prompt.append("2. 回答尽量控制在100字以内，简洁为主\n");
```

---

### 修改 DeepSeek 模型或参数

**文件：** `src/main/resources/application.yml`

```yaml
ai:
  model: deepseek-chat     # 改成 deepseek-reasoner 可提升推理能力
  temperature: 0.7         # 调低（如0.3）让回答更保守稳定
  max-tokens: 1000         # 调高让 AI 可以回答更长的内容
```

---

## 八、VIP 状态自动刷新机制

为确保 AI 客服能实时获取用户最新 VIP 等级，平台实现了三处自动刷新：

| 触发场景 | 位置 |
|---|---|
| 用户打开任意页面时 | `layout/index.vue` → `onMounted` |
| 用户登录后 | `layout/index.vue` → `watch(isLoggedIn)` |
| 用户进入 VIP 页面时 | `vip/index.vue` → `loadData()` |
| 用户支付完成回跳时 | `vip/index.vue` → `handlePayResult()` → `loadData()` |

VIP 等级会实时写入 `userStore`，AI 客服每次发消息时都会从 store 读取最新值带给后端。

---

## 九、人工客服转接

当用户说以下关键词时，系统会自动发起人工客服转接：

```
转人工 / 人工客服 / 转客服 / 人工服务 / 真人客服 / 联系客服 / 转接人工
```

人工客服基于 WebSocket 实现实时消息通道，转接后 AI 客服会自动退出对话模式，切换为人工模式。

---

## 十、注意事项

1. **AI Key 安全**：`application.yml` 中的 `api-key` 不要提交到公开代码仓库，生产环境建议改成环境变量读取。
2. **DeepSeek API 配额**：注意 DeepSeek 官方 API 的调用频率和 Token 配额限制。
3. **前后端知识库一致性**：修改业务规则时，需要同时更新后端 `buildSystemPrompt()` 和前端 `knowledgeBase`，避免两处说法不一致。
4. **兜底逻辑保留**：建议始终保留前端本地知识库作为 DeepSeek 失败时的兜底，确保用户体验不中断。
