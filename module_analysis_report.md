# 项目模块分析报告

## 1. 用户通知模块（NotificationController/NotificationServiceImpl）

### 核心功能
- **通知发送**：支持指定用户和全体用户两种方式
- **通知存储**：分为系统通知表和用户通知映射表
- **实时推送**：WebSocket实时推送给在线用户
- **通知管理**：查询、标记已读、删除、批量操作

### 关键类

| 类 | 职责 |
|---|---|
| `NotificationController` | REST API端点（分页查询、标记读、删除等） |
| `NotificationServiceImpl` | 业务逻辑（发送、读状态管理、统计） |
| `NotificationWebSocketHandler` | WebSocket连接管理与实时推送 |

### 技术要点
- **数据模型**：SystemNotification（系统通知）+ UserNotification（用户关联）
- **WebSocket管理**：ConcurrentHashMap维护userId->Session映射，支持单播/广播
- **推送流程**：sendToUsers → 创建SystemNotification → 为每用户创建UserNotification → 实时推送WebSocket消息
- **通知类型**：预约成功(1)、预约提醒(2)、优惠券到期(3)、系统公告(4)、退款成功(5)等

### 支持的操作
```
GET  /api/notification/list          # 分页查询
GET  /api/notification/unread-count  # 未读数
PUT  /api/notification/{id}/read     # 标记已读
PUT  /api/notification/read-all      # 全标记已读
DELETE /api/notification/{id}        # 删除单条
DELETE /api/notification/batch       # 批量删除
GET  /api/notification/{id}          # 详情
GET  /api/notification/search        # 搜索
GET  /api/notification/statistics    # 统计信息
```

---

## 2. 后台管理通知模块（AdminNotificationController/AdminNotificationServiceImpl）

### 核心功能
- **后台通知发送**：面向管理员/员工的系统通知
- **权限隔离**：按门店/员工角色隔离通知数据
- **管理端WebSocket**：实时广播管理端通知

### 关键实现
- **权限控制**：resolveScopedStoreId() 根据角色（SUPER_ADMIN/STORE_ADMIN/STORE_STAFF）返回对应门店ID
- **存储**：单表AdminNotification存储所有后台通知，通过storeId和isRead字段标识
- **推送机制**：AdminNotificationWebSocketHandler.pushNotificationToAll() 广播所有在线管理端

### API端点
```
GET    /api/admin/notification/list           # 分页查询
GET    /api/admin/notification/unread-count   # 未读数
PUT    /api/admin/notification/{id}/read      # 标记已读
PUT    /api/admin/notification/read-all       # 全标记已读
DELETE /api/admin/notification/{id}           # 删除
DELETE /api/admin/notification/batch          # 批量删除
GET    /api/admin/notification/{id}           # 详情
GET    /api/admin/notification/statistics     # 统计信息
```

### 特色功能
- **容错设计**：getUnreadCount() 含fallback逻辑，异常时退回全局计数避免500错误
- **优先级支持**：AdminNotification含priority字段，支持高优先级通知优先展示

---

## 3. 客服会话模块（ServiceSessionController/ServiceSessionServiceImpl）

### 核心功能
- **会话生命周期**：创建→等待接入→进行中→已结束
- **实时双向通信**：WebSocket实时推送用户消息、管理员消息、系统消息
- **会话评价**：支持用户对客服会话的评分与评价

### 关键业务流程

**用户端流程**：
```
1. 用户发起转人工 → createSession(userId, userName, question)
2. 系统创建Session(status=0), 通知在线管理员
3. 用户可发送消息 → sendMessage(sessionId, "user", userId, content)
4. 用户结束会话 → closeSession(sessionId, "user")
5. 用户评价会话 → rateSession(sessionId, rating, comment)
```

**管理端流程**：
```
1. 管理员查看等待会话列表 → listSessions(status=0)
2. 管理员接入会话 → acceptSession(sessionId, adminId) [status变为1]
3. 管理员发送消息 → sendMessage(sessionId, "admin", adminId, content)
4. 管理员结束会话 → closeSession(sessionId, "admin") [status变为2]
```

### 数据模型
```
ServiceSession {
  id, userId, userName, initialQuestion,
  adminId, status(0=等待,1=进行,2=已结束),
  rating, ratingComment, endTime
}

ServiceMessage {
  sessionId, senderType(user/admin/system), senderId,
  content, msgType(text/image/system), isRead
}
```

### WebSocket通信
- **连接URL**：`/api/ws/service?userId=xxx&role=user` 或 `?adminId=xxx&role=admin`
- **消息类型**：
  - `new_service_session` - 新会话通知（推送给所有管理员）
  - `session_accepted` - 会话已接入（推送给用户）
  - `service_message` - 聊天消息（推送给对方）
  - `session_closed` - 会话已结束（推送给双方）

---

## 4. 文章社区模块（ArticleController/ArticleServiceImpl）

### 核心功能
- **文章管理**：发布、编辑、删除、上下架
- **内容分类**：新手攻略、选本技巧、榜单推荐、行业动态、玩家心得
- **用户交互**：点赞、收藏、评论、浏览计数
- **推荐机制**：推荐文章、热门文章

### 功能清单

| 操作 | 实现 |
|---|---|
| 分页查询 | pageQuery(page, pageSize, category, keyword, status, sortBy) |
| 详情查询 | getById(id) - 包含用户点赞状态 |
| 热门文章 | getHotArticles(limit) - 按viewCount排序 |
| 推荐文章 | getRecommendedArticles(limit) - 筛选isRecommended=1 |
| 新增文章 | add(ArticleDTO) |
| 编辑文章 | update(ArticleDTO) - 自动设置publishTime |
| 删除文章 | delete(id) |
| 增加浏览 | increaseViewCount(id) |
| 点赞 | likeArticle(id) - 幂等性保证，不重复点赞 |
| 取消点赞 | unlikeArticle(id) |
| 检查点赞状态 | isLiked(articleId, userId) |

### 排序选项
```
sortBy = "view"    # 最多浏览
sortBy = "like"    # 最多点赞
sortBy = "comment" # 最多评论
sortBy = null      # 默认最新发布
```

### 数据模型
```
Article {
  id, title, content, summary, category(1-5),
  status(0=草稿,1=发布), isTop(置顶), isRecommended,
  viewCount, likeCount, commentCount,
  publishTime, createTime, updateTime
}

ArticleLike {
  id, articleId, userId, createTime
}
```

---

## 5. 推荐与AI模块

### 5.1 推荐系统（RecommendationController/RecommendationServiceImpl）

#### 推荐算法
支持**4种推荐方式**，各占不同权重：

| 推荐方式 | 权重 | 计算方式 |
|---|---|---|
| **个性化推荐** | 40% | 基于用户历史偏好 |
| **热门推荐** | 30% | 从HotRanking榜单 |
| **新品推荐** | 30% | 按发布时间最新 |
| 去重后综合返回 | - | 合并去重，总limit限制 |

#### 多维度推荐API

```
GET /api/recommendation/personalized?limit=10
  → 返回：个性化推荐（历史40% + 热门30% + 新品30%）

GET /api/recommendation/collaborative/{scriptId}?limit=6
  → 返回：看了scriptId的用户还看了什么

GET /api/recommendation/similar/{scriptId}?limit=6
  → 返回：基于标签/分类/难度的相似剧本

GET /api/recommendation/history?limit=10
  → 返回：基于用户历史浏览的推荐

GET /api/recommendation/hot?rankingType=1&limit=10
  → 返回：热门推荐（1=今日热门，2=本周，3=本月，4=口碑榜）

GET /api/recommendation/new?limit=10
  → 返回：最新上架剧本
```

#### 推荐得分计算

**个性化匹配度**（范围0-100）：
- 分类匹配：+25%
- 类型匹配：+15%
- 难度相近：+10%
- 人数相近：+10%
- 标签匹配：+40%
- 归一化处理，剔除低分

**内容相似度**（范围0-100）：
- 标签相似（Jaccard）：40%
- 分类匹配：25%
- 类型匹配：15%
- 难度接近度：10%
- 人数接近度：10%

**热门分数**（范围0-100）：
- 评分贡献：40%（rating/5 × 40）
- 榜单排名贡献：30%（越靠前分数越高）
- 热度分数贡献：30%（score/100 × 30）

**新品分数**（范围0-100）：
- 时间新鲜度：50%（30天内衰减）
- 评分贡献：30%
- 浏览热度：20%

#### 用户行为记录
```
POST /api/recommendation/browse
  → 记录用户浏览行为（scriptId/storeId，浏览时长）
  → 自动更新用户偏好

POST /api/recommendation/click
  → 记录推荐点击行为

GET  /api/recommendation/admin/stats?days=7
  → 推荐效果统计（管理端）

POST /api/recommendation/admin/refresh-ranking?rankingType=1
  → 手动刷新热门榜单（管理端）
```

#### 关键数据表
- `UserBrowseHistory` - 用户浏览记录（targetType: 1=脚本, 2=门店）
- `UserPreference` - 用户偏好（preferenceType: category_*, type_*, difficulty_*, tag_*）
- `HotRanking` - 热门榜单缓存（rankingType: 1=今日, 2=本周, 3=本月, 4=口碑）
- `RecommendationLog` - 推荐日志（tracking推荐曝光、点击、预约）

---

### 5.2 AI客服（AIController/AIServiceImpl）

#### 核心功能
- **多AI供应商支持**：OpenAI、OpenRouter、DeepSeek、文心一言、通义千问、Mock
- **会话管理**：支持多轮对话历史
- **知识库集成**：剧本、VIP、积分、优惠券、退款等全业务知识
- **上下文感知**：根据当前页面和用户信息动态调整回答

#### API端点

```
POST /api/ai/chat
  {
    "message": "用户问题",
    "sessionId": "会话ID（可选）",
    "history": [...对话历史...],
    "context": {
      "page": "/vip",  # 当前页面
      "userInfo": {
        "nickname": "用户昵称",
        "vipLevel": 2
      }
    }
  }
  → 返回：AI回复、建议、交互动作

GET /api/ai/recommend/{type}
  → 获取AI推荐（type: script/store/activity等）

POST /api/ai/log
  → 记录对话日志（用于优化）

GET /api/ai/faq
  → 返回常见问题列表

POST /api/ai/feedback
  → 用户反馈AI回答质量
```

#### AI系统提示词包含内容

| 模块 | 关键信息 |
|---|---|
| **预约系统** | 预约流程、取消规则、时间限制 |
| **退款政策** | 退款比例（24h内全额、12-24h 80%、6-12h 50%、6h内不退） |
| **改期规则** | 24h外免费改期、12-24h需联系门店、12h内不支持 |
| **VIP体系** | 4个等级（见习、银章、金章、传奇），折扣、体验券、生日礼券 |
| **积分系统** | 签到+10、完成预约+100、评价奖励、收藏里程碑+20 |
| **优惠券体系** | VIP月度发放、积分兑换、叠加使用规则 |
| **拼团功能** | 人数不足自动发起拼团 |
| **支付方式** | 仅支持支付宝 |
| **通知体系** | 预约成功、支付成功、VIP体验券、系统公告 |
| **人工客服** | 热线400-123-4567，9:00-22:00，service@jubensha.com |
| **转人工检测** | 关键词：转人工、人工客服、转客服等 |

#### 关键特性
- **转人工检测**：检测TRANSFER_KEYWORDS列表，直接返回转接标记，不调用AI
- **多轮对话**：支持history参数（最近10轮消息）
- **上下文感知**：
  - 在/vip页面 → 优先解答VIP问题
  - 在/reservation页面 → 优先解答预约、退款、改期
  - 在/script页面 → 优先解答剧本选择、推荐
  - 在/user/points页面 → 优先解答积分问题
  - 在/user/coupons页面 → 优先解答优惠券问题
- **错误处理**：HTTP调用异常时返回"AI服务暂时不可用"
- **配置灵活**：通过application.yml配置不同AI提供商的API密钥、URL、模型、温度、token限制

---

## 6. 定时任务与实时推送支撑

### 6.1 定时任务（Task包）

#### NotificationTask - 通知相关定时任务

| 任务 | 触发时间 | 功能 |
|---|---|---|
| **reservationReminder** | 每小时（0 0 * * * ?） | 预约提醒：提醒2小时内开始的已支付预约 |
| **couponExpireReminder** | 每天9点（0 0 9 * * ?） | 优惠券到期提醒：提醒3天内到期的未使用优惠券 |
| **vipExpireReminder** | 每天10点（0 0 10 * * ?） | VIP到期提醒：提醒3天内到期的有效VIP，支持续费提示 |

**实现方式**：
```java
@Scheduled(cron = "0 0 * * * ?")
public void reservationReminder() {
  // 1. 查询status=2(已支付) 且 reservation_time在[now, now+2h]的预约
  // 2. 遍历发送通知：notificationService.sendToUsers(title, content, type=2)
  // 3. WebSocket实时推送给在线用户
}
```

#### RecommendationTask - 推荐系统定时任务

| 任务 | 触发时间 | 功能 |
|---|---|---|
| **refreshDailyHotRanking** | 每小时（0 0 * * * ?） | 刷新今日热门榜单（rankingType=1） |
| **refreshWeeklyHotRanking** | 每天1点（0 0 1 * * ?） | 刷新本周热门榜单（rankingType=2） |
| **refreshMonthlyHotRanking** | 每天2点（0 0 2 * * ?） | 刷新本月热门榜单（rankingType=3） |
| **refreshReputationRanking** | 每天3点（0 0 3 * * ?） | 刷新口碑榜（rankingType=4） |

**刷新逻辑**：
```java
// 根据rankingType，计算hot_ranking表
// 1. 今日热门：统计24h内的浏览/点赞/预约，计算热度分数
// 2. 本周热门：统计7d内的相关指标
// 3. 本月热门：统计30d内的相关指标
// 4. 口碑榜：按rating评分排序
```

---

### 6.2 WebSocket实时推送架构

#### 三个WebSocket Handler

| Handler | 连接URL | 用途 |
|---|---|---|
| **NotificationWebSocketHandler** | `/api/ws/notification?userId=xxx` | 用户通知推送 |
| **AdminNotificationWebSocketHandler** | `/api/ws/admin-notification?adminId=xxx` | 管理员通知推送 |
| **ServiceWebSocketHandler** | `/api/ws/service?userId=xxx&role=user` 或 `?adminId=xxx&role=admin` | 客服会话双向通信 |

#### 连接管理

**用户通知Handler**：
```java
// 存储结构
private static final Map<Long, WebSocketSession> USER_SESSIONS; // userId -> session

// 连接建立
afterConnectionEstablished() {
  // 从URL参数提取userId，存储session
  USER_SESSIONS.put(userId, session);
}

// 推送消息
pushNotification(userId, notification) {
  // 单播：推送给特定用户
}

pushNotificationToAll(notification) {
  // 广播：推送给所有在线用户
}
```

**管理员通知Handler**：
```java
// 存储结构
private static final Map<Long, WebSocketSession> ADMIN_SESSIONS; // adminId -> session

// 推送方式
pushNotificationToAll(notification) {
  // 广播给所有在线管理员
}
```

**客服会话Handler**：
```java
// 存储结构
private static final Map<Long, WebSocketSession> USER_SESSIONS;   // userId -> session
private static final Map<Long, WebSocketSession> ADMIN_SESSIONS;  // adminId -> session

// 推送方式
sendToUser(userId, msg)           // 单播给用户
sendToAdmin(adminId, msg)         // 单播给管理员
notifyAdmins(msg)                 // 广播给所有管理员
getOnlineAdminIds()               // 获取在线管理员ID集合
```

#### 实时推送流程图

**用户接收通知**：
```
NotificationServiceImpl.sendToUsers()
  ├─ 1. 创建SystemNotification记录
  ├─ 2. 为每个用户创建UserNotification映射
  ├─ 3. 调用pushNotificationToUser()
  └─ 4. WebSocketHandler.pushNotification(userId, msg)
       └─ 从USER_SESSIONS获取session，发送TextMessage

客户端接收：
  WebSocket.onmessage(event) {
    const notification = JSON.parse(event.data);
    // 更新UI通知计数、显示通知提示
  }
```

**客服会话消息流**：
```
ServiceSessionServiceImpl.sendMessage(sessionId, senderType, senderId, content)
  ├─ 1. 创建ServiceMessage记录
  ├─ 2. 构建WebSocket消息体
  └─ 3. 判断senderType
       ├─ user → 推送给接入管理员或所有管理员
       └─ admin → 推送给用户

WebSocketHandler.sendToUser/sendToAdmin()
  └─ 获取session，发送JSON消息
```

#### 心跳检测
```java
// 客户端发送 "ping"
handleTextMessage(session, message) {
  if ("ping".equals(message.getPayload())) {
    session.sendMessage(new TextMessage("pong"));
  }
}
```

#### 连接错误处理
```java
handleTransportError(session, exception) {
  // 判断是否是正常关闭异常（ClosedChannelException、EOFException等）
  if (isConnectionClosed(exception)) {
    log.warn("连接已断开");  // 降级处理
  } else {
    log.error("传输错误");   // 异常处理
  }
  // 从会话映射中移除
  USER_SESSIONS.entrySet().removeIf(...);
}
```

#### ObjectMapper配置
```java
// 支持LocalDateTime等时间类型序列化，避免JSON序列化失败导致500
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
```

---

## 总结架构

### 模块依赖关系
```
NotificationController ─→ NotificationServiceImpl ─→ WebSocketHandler
  ├─ SystemNotificationMapper
  ├─ UserNotificationMapper
  └─ NotificationWebSocketHandler

AdminNotificationController ─→ AdminNotificationServiceImpl ─→ AdminNotificationWebSocketHandler
  └─ AdminNotificationMapper

ServiceSessionController ─→ ServiceSessionServiceImpl ─→ ServiceWebSocketHandler
  ├─ ServiceSessionMapper
  ├─ ServiceMessageMapper
  └─ ServiceWebSocketHandler

ArticleController ─→ ArticleServiceImpl
  ├─ ArticleMapper
  └─ ArticleLikeMapper

RecommendationController ─→ RecommendationServiceImpl ─→ RecommendationTask
  ├─ ScriptMapper
  ├─ UserBrowseHistoryMapper
  ├─ UserPreferenceMapper
  ├─ HotRankingMapper
  └─ RecommendationLogMapper

AIController ─→ AIServiceImpl
  └─ RestTemplate (调用外部AI API)

NotificationTask, RecommendationTask ─→ @Scheduled定时执行
```

### 实时推送支撑要素
1. **WebSocket连接管理**：ConcurrentHashMap维护在线会话
2. **异步推送**：Service调用Handler方法异步发送
3. **错误恢复**：连接断开自动清理，心跳检测异常处理
4. **多提供商支持**：AI模块支持灵活切换AI服务商
5. **定时刷新**：榜单、提醒等定时更新，确保数据新鲜度
