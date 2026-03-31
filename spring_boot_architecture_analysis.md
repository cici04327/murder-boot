# Spring Boot 后端架构分析：剧本杀预约与门店管理系统

## 🏗️ 包结构总览

```
com.murder
├── common/          # 公共基础设施
│   ├── config/      # 框架配置
│   ├── constant/    # 常量定义
│   ├── context/     # 线程上下文
│   ├── exception/   # 异常体系
│   ├── handler/     # 全局处理器
│   ├── properties/  # 配置属性
│   ├── result/      # 统一返回结果
│   └── utils/       # 工具类
├── controller/      # 控制层（REST API）
├── dto/             # 请求数据传输对象
├── entity/          # JPA 数据库映射实体
├── mapper/          # MyBatis Plus 数据访问层
├── service/         # 业务逻辑层
├── task/            # 定时任务
├── websocket/       # WebSocket 处理器
├── interceptor/     # HTTP 拦截器
└── vo/              # 视图对象（响应数据）
```

---

## 🎯 核心业务域模块划分

### 1. **预约管理域**（Reservation）
- **核心实体**: `Reservation` (预约订单)、`ScriptSchedule`（剧本排期）
- **关键业务流**:
  - 创建预约 → 支付 → 入场确认 → 完成游戏 → 评价
  - 支持退款、改期、DM分配
  - 自动完成、支付超时检测
- **服务**: `ReservationService`、`ScriptScheduleService`
- **定时任务**: `ReservationTask` (自动完成、支付超时处理)

### 2. **用户域**（User）
- **核心实体**: `User`、`UserVip`、`UserAddress`、`UserCoupon`、`UserPointsRecord`
- **关键功能**:
  - 注册/登录（支持积分奖励）
  - VIP会员管理（分级：普通/银卡/金卡/钻石）
  - 积分系统、优惠券管理
  - 账户安全（实名认证、手机/邮箱验证）
  - 浏览历史、个人设置
- **服务**: `UserService`、`UserAccountService`、`UserPointsService`、`VipService`

### 3. **门店与房间域**（Store）
- **核心实体**: `Store`、`StoreRoom`、`StoreEmployee`
- **关键功能**:
  - 门店信息管理（地址、营业时间、评分）
  - 房间库存与可用性检查
  - 员工账户管理（支持门店级RBAC）
  - 门店统计（营收、订单、评分）
  - 门店登录账号体系
- **服务**: `StoreService`、`StoreRoomService`、`StoreEmployeeService`

### 4. **剧本内容域**（Script）
- **核心实体**: `Script`、`ScriptCategory`、`ScriptRole`、`ScriptTag`
- **关键功能**:
  - 剧本元数据管理（难度、人数、类型、时长、价格）
  - 分类和标签体系
  - 角色信息管理
  - 热门/推荐算法
- **服务**: `ScriptService`、`ScriptRoleService`

### 5. **评价与反馈域**（Review & Feedback）
- **核心实体**: `Review`、`ScriptReview`、`StoreReview`、`Feedback`
- **关键业务流**:
  - 游戏完成后评价
  - 门店评分聚合
  - 用户反馈收集
- **服务**: `ReviewService`、`ScriptReviewService`、`StoreReviewService`、`FeedbackService`

### 6. **优惠券与VIP域**（Coupon & VIP）
- **核心实体**: `Coupon`、`UserCoupon`、`VipPackage`、`UserVip`
- **关键功能**:
  - 优惠券发放、使用、过期管理
  - VIP套餐定义与购买
  - VIP等级权益管理
  - 自动过期检测
- **定时任务**: `CouponExpireTask`

### 7. **通知与消息域**（Notification）
- **核心实体**: `UserNotification`、`AdminNotification`、`SystemNotification`
- **传输方式**:
  - REST API（查询、标记已读）
  - **WebSocket** 实时推送（`NotificationWebSocketHandler`）
- **服务**: `NotificationService`、`AdminNotificationService`
- **定时任务**: `NotificationTask`

### 8. **内容与社区域**（Article & Group）
- **核心实体**: `Article`、`ArticleComment`、`ArticleLike`、`GroupOrder`、`GroupMember`
- **关键功能**:
  - 文章发布、评论、点赞
  - 团购订单管理
- **服务**: `ArticleService`、`ArticleCommentService`、`GroupOrderService`

### 9. **AI客服与数据分析域**（AI & Statistics）
- **服务**: `AIService`、`StatisticsService`
- **关键功能**:
  - AI对话服务
  - 实时统计、趋势分析、报表生成
  - 热点排名、推荐日志

### 10. **直接消息与门店服务**（DM & Service）
- **核心实体**: `Dm`、`ServiceSession`、`ServiceMessage`
- **功能**: DM调度、客服会话管理
- **WebSocket**: `ServiceWebSocketHandler`

---

## 📊 典型数据实体关系

```
用户预约核心链路：
User → Reservation ← ScriptSchedule
         ↓
      StoreRoom, Script
         ↓
      ScriptRole（角色信息）
         ↓
      Payment → UserCoupon（优惠券）、UserVip（VIP折扣）
         ↓
      Review（评价） + Feedback（反馈）

其他关键关联：
User ←→ UserVip (VIP等级管理)
User ←→ UserCoupon (优惠券关联)
User ←→ UserPointsRecord (积分记录)
User ←→ UserBrowseHistory (浏览历史)
Store ←→ StoreEmployee (员工与权限)
Store ←→ StoreRoom (房间库存)
Script ←→ ScriptRole (角色定义)
Script ←→ ScriptSchedule (排期与库存)
Script ←→ ScriptFavorite (收藏)
```

---

## 🔄 控制器 → 服务层组织方式

### 分层调用模式

```
HTTP Request (Rest Controller)
    ↓
    └─ @PostMapping/@GetMapping 处理请求参数
       ├─ 从 BaseContext 获取当前用户ID/角色
       ├─ 参数验证与转换 (DTO → Entity)
       └─ 委派到 Service 层
           ↓
       Service Interface (服务接口)
           ↓
       ServiceImpl (实现类)
           ├─ 复杂业务逻辑
           ├─ 多实体协调
           ├─ 事务管理 (@Transactional)
           └─ 调用 Mapper 层
               ↓
           Mapper (MyBatis Plus)
               └─ 数据库 CRUD
                   ↓
               返回 Entity → VO (转为响应对象)
                   ↓
           Result<T> (统一返回结构)
```

### 主要控制器及其服务依赖

| 控制器 | 核心服务 | 主要操作 |
|--------|---------|--------|
| `ReservationController` | `ReservationService` | 创建/查询/支付/入场/退款/改期 |
| `UserController` | `UserService`, `UserAccountService` | 注册/登录/资料更新/验证 |
| `StoreController` | `StoreService`, `StoreRoomService` | 门店查询/详情/统计 |
| `ScriptController` | `ScriptService`, `ScriptScheduleService` | 剧本列表/详情/排期 |
| `PaymentController` | `PaymentService`, `CouponService` | 支付/退款/优惠券扣除 |
| `NotificationController` | `NotificationService` | 通知查询/标记已读 |
| `StatisticsController` | `StatisticsService` | 数据分析/报表 |

---

## 🔐 请求处理流程

### 1. **JWT 鉴权拦截**
- **文件**: `JwtTokenInterceptor` (src/main/java/com/murder/interceptor/)
- **机制**:
  - 从请求头 `Authorization` 获取 JWT Token
  - 区分**可选认证**（公开API）与**强制认证**（个人/管理接口）
  - 解析 Token → 设置 `BaseContext` (当前用户ID、角色、权限)
  - 支持用户、门店、DM多角色

### 2. **全局异常处理**
- **文件**: `GlobalExceptionHandler`
- **处理范围**:
  - 参数类型不匹配 → 400
  - 业务异常 (`BaseException`) → 500
  - 权限异常 → 403
  - SQL约束违反 → 数据重复提示
  - 数据完整性异常 → 缺字段提示
  - 客户端主动断开 → 499
  - 运行时异常 → 详细上下文日志

### 3. **统一返回结果**
```java
Result<T> {
    code: 200/500/400/403/404  // 状态码
    msg: String                 // 返回消息
    data: T                      // 泛型数据对象
}
```

---

## ⏰ 定时任务系统

**文件**: `src/main/java/com/murder/task/`

| 任务类 | 触发频率 | 功能 |
|--------|---------|------|
| `ReservationTask` | 定时（秒级） | 自动完成预约、支付超时处理、VIP过期续费 |
| `CouponExpireTask` | 定时 | 优惠券过期标记 |
| `GroupOrderTask` | 定时 | 团购订单自动完成 |
| `NotificationTask` | 定时 | 通知推送 |
| `RecommendationTask` | 定时 | 推荐日志清理、推荐算法计算 |

**启用方式**: `@EnableScheduling` + `@Scheduled` 注解

---

## 🔌 WebSocket 实时通信

**文件**: `src/main/java/com/murder/websocket/`

### 三大 WebSocket 处理器

1. **`NotificationWebSocketHandler`** 
   - 用户通知实时推送
   - 存储 `userId → WebSocketSession` 映射
   - 支持单播/广播推送

2. **`ServiceWebSocketHandler`**
   - 客服会话实时消息
   - 用户与客服双向通信

3. **`AdminNotificationWebSocketHandler`**
   - 管理员通知实时推送
   - 后台消息下发

**机制**:
- 连接时从 URL 参数提取 `userId`
- 心跳检测 (ping/pong)
- 断开连接时自动移除会话
- JSON 序列化支持 LocalDateTime 等时间类型

---

## 🔑 关键配置与工具

### 核心配置
- **`MybatisPlusConfig`**: 分页插件、最大页限 1000
- **`WebSocketConfig`**: WebSocket 端点注册
- **`WebMvcConfig`**: 拦截器注册、CORS 配置
- **`JwtProperties`**: JWT 密钥、过期时间
- **`RedisConfig`**: 缓存配置

### 上下文管理
- **`BaseContext`**: ThreadLocal 存储当前用户上下文
  - userId、role、storeId、employeeId、staffRole、dmId、权限码

### 数据库映射
- **`MyMetaObjectHandler`**: 自动填充 createTime、updateTime
- **逻辑删除**: `@TableLogic` 注解标记的 `isDeleted` 字段

---

## 📈 重要文件清单

### 最关键的文件
1. **启动类**: `MurderBootApplication.java`
2. **核心服务**: 
   - `ReservationService` / `ReservationServiceImpl`
   - `UserService` / `UserServiceImpl`
   - `StoreService` / `StoreServiceImpl`
   - `ScriptService` / `ScriptServiceImpl`
3. **数据实体**:
   - `Reservation`, `User`, `Store`, `Script`, `UserCoupon`, `UserVip`
4. **控制层**: `ReservationController`, `UserController`, `StoreController`
5. **基础设施**:
   - `JwtTokenInterceptor` (认证)
   - `GlobalExceptionHandler` (异常)
   - `BaseContext` (线程上下文)
   - `Result<T>` (统一返回)
6. **WebSocket**: `NotificationWebSocketHandler`, `ServiceWebSocketHandler`
7. **定时任务**: `ReservationTask`, `CouponExpireTask`

---

## 💡 架构特点总结

| 特点 | 实现方式 |
|------|---------|
| **分层架构** | Controller → Service → Mapper → Entity → DB |
| **数据交互** | DTO (请求) → Entity (存储) → VO (响应) |
| **权限控制** | JWT + 拦截器 + BaseContext 上下文 |
| **事务管理** | `@Transactional` + MyBatis Plus 原生支持 |
| **缓存** | Redis (通过 RedisConfig) |
| **异常处理** | 全局处理器 + 自定义异常体系 |
| **分页查询** | MyBatis Plus Pagination Plugin |
| **实时通信** | WebSocket + JSON 序列化 |
| **后台任务** | Spring Scheduling (定时任务框架) |
| **API文档** | Swagger/Knife4j (application.yml 配置) |
| **数据库监控** | Druid (SQL 监控) |

---

## 🎪 业务核心流程示例

### 预约完整流程
```
1. 用户查询 (StoreController.list)
     ↓
2. 选择门店、房间、剧本 → 创建预约 (ReservationController.create)
     ↓ ReservationService.create
     ├─ 检查房间可用性
     ├─ 同步 ScriptSchedule.currentPlayers
     ├─ 计算 VIP 折扣、优惠券扣除
     └─ 创建 Reservation 记录
     ↓
3. 支付 (PaymentController) → UserCoupon 标记已使用
     ↓
4. 定时任务检测超时未支付 → 自动取消
     ↓
5. 用户入场 (ReservationController.checkIn)
     ↓
6. 定时任务自动完成 (ReservationTask.completeReservations)
     ↓
7. 用户评价 (ReviewController) → Review + ScriptReview + StoreReview
     ↓
8. 积分奖励、推荐日志记录
```

### 通知推送流程
```
系统事件触发 (预约状态变更、消息下发)
    ↓
NotificationService.create (生成通知记录)
    ↓
WebSocket 检查 USER_SESSIONS 中是否在线
    ↓
在线: NotificationWebSocketHandler.pushNotification (实时推送)
离线: 存储数据库，用户登录后查询
```

---

## 📋 完整服务列表 (33 个核心服务)

业务服务: `ReservationService`, `UserService`, `StoreService`, `ScriptService`, `PaymentService`, `ReviewService`, `CouponService`, `VipService`, `NotificationService`, `AdminNotificationService`, `ArticleService`, `GroupOrderService`, `FeedbackService`, `AIService`, `StatisticsService`, `DmService`, `ServiceSessionService`, `RecommendationService`

基础服务: `ImageService`, `UserAccountService`, `UserPointsService`, `UserAddressService`, `UserSettingsService`, `UserBrowseHistoryService`, `StoreEmployeeService`, `StoreReviewService`, `StoreRoomService`, `ScriptScheduleService`, `ScriptFavoriteService`, `ScriptReviewService`, `ScriptRoleService`, `ArticleCommentService`, `ArticleFavoriteService`, `StoreEmployeeOperationLogService`

---

**系统类型**: Spring Boot 3.x + MyBatis Plus + MySQL + Redis + WebSocket  
**部署方式**: 单体应用 (可 Docker 容器化)  
**主要特性**: 预约管理、用户管理、门店运营、内容管理、实时通知、数据分析
