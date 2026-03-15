# Service、Entity、Mapper 对应关系映射表

## 概述
本文档展示了 Service 接口、Entity 实体类和 Mapper 接口之间的对应关系，便于快速定位代码。

---

## 用户相关模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **UserService** | User | UserMapper | 用户注册、登录、个人信息 |
| **UserAddressService** | UserAddress | UserAddressMapper | 用户地址簿管理 |
| **UserBrowseHistoryService** | UserBrowseHistory | UserBrowseHistoryMapper | 浏览历史跟踪 |
| **UserPointsService** | UserPointsRecord | UserPointsRecordMapper | 积分系统管理 |
| **UserSettingsService** | UserSettings | UserSettingsMapper | 用户隐私和偏好设置 |
| **VipService** | UserVip, VipPackage | UserVipMapper, VipPackageMapper | VIP会员管理 |

---

## 剧本相关模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **ScriptService** | Script, ScriptCategory | ScriptMapper, ScriptCategoryMapper | 剧本和分类管理 |
| **ScriptRoleService** | ScriptRole | ScriptRoleMapper | 剧本角色配置 |
| **ScriptScheduleService** | ScriptSchedule | ScriptScheduleMapper | 排期和时间段管理 |
| **ScriptFavoriteService** | ScriptFavorite | ScriptFavoriteMapper | 剧本收藏功能 |
| **ScriptReviewService** | ScriptReview | ScriptReviewMapper | 剧本评价管理 |

---

## 门店相关模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **StoreService** | Store | StoreMapper | 门店信息和登录 |
| **StoreRoomService** | StoreRoom | StoreRoomMapper | 房间管理 |
| **StoreEmployeeService** | StoreEmployee | StoreEmployeeMapper | 员工管理 |
| **StoreReviewService** | StoreReview | StoreReviewMapper | 门店评价 |
| **DmService** | Dm | DmMapper | DM主持人管理 |

---

## 预约和支付模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **ReservationService** | Reservation | ReservationMapper | 预约管理 |
| **PaymentService** | Reservation | ReservationMapper | 支付和退款 |
| **GroupOrderService** | GroupOrder, GroupMember | GroupOrderMapper, GroupMemberMapper | 拼单功能 |

---

## 评价相关模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **ReviewService** | Review | ReviewMapper | 综合评价管理 |

---

## 文章相关模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **ArticleService** | Article | ArticleMapper | 文章发布和管理 |
| **ArticleCommentService** | ArticleComment | ArticleCommentMapper | 文章评论 |
| **ArticleFavoriteService** | ArticleFavorite | ArticleFavoriteMapper | 文章收藏 |

---

## 优惠和VIP模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **CouponService** | Coupon, UserCoupon | CouponMapper, UserCouponMapper | 优惠券管理 |
| **VipService** | VipPackage, UserVip | VipPackageMapper, UserVipMapper | VIP套餐和用户VIP |

---

## 通知模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **NotificationService** | UserNotification | UserNotificationMapper | 用户通知 |
| **AdminNotificationService** | AdminNotification | AdminNotificationMapper | 管理端通知 |

---

## 辅助功能模块

| Service | 主要Entity | 相关Mapper | 功能描述 |
|---------|-----------|----------|--------|
| **RecommendationService** | RecommendationLog, HotRanking, UserPreference | RecommendationLogMapper, HotRankingMapper, UserPreferenceMapper | 推荐系统 |
| **StatisticsService** | 多个Entity | 多个Mapper | 数据分析 |
| **FeedbackService** | Feedback | FeedbackMapper | 用户反馈 |
| **AIService** | ServiceSession, ServiceMessage | ServiceSessionMapper, ServiceMessageMapper | AI对话 |
| **ServiceSessionService** | ServiceSession, ServiceMessage | ServiceSessionMapper, ServiceMessageMapper | 客服会话 |
| **ImageService** | 无Entity | 无Mapper | 图片处理 |

---

## Entity到Service的反向映射

### User
- **Service：** UserService
- **相关操作：** 注册、登录、查询、更新

### UserAddress
- **Service：** UserAddressService
- **相关操作：** 增删改查、设置默认

### UserBrowseHistory
- **Service：** UserBrowseHistoryService
- **相关操作：** 记录、查询、删除、清空

### UserCoupon
- **Service：** CouponService
- **相关操作：** 领取、使用、查询

### UserNotification
- **Service：** NotificationService
- **相关操作：** 查询、标记已读、删除

### UserPointsRecord
- **Service：** UserPointsService
- **相关操作：** 增加、扣减、查询

### UserPreference
- **Service：** RecommendationService
- **相关操作：** 更新、查询

### UserSettings
- **Service：** UserSettingsService
- **相关操作：** 获取、更新隐私/通知/偏好设置

### UserVip
- **Service：** VipService
- **相关操作：** 购买、续费、查询、赠送

### Script
- **Service：** ScriptService, RecommendationService, ArticleService
- **相关操作：** 增删改查、获取热门、获取推荐

### ScriptCategory
- **Service：** ScriptService
- **相关操作：** 增删改查

### ScriptRole
- **Service：** ScriptRoleService
- **相关操作：** 增删改查、批量操作

### ScriptSchedule
- **Service：** ScriptScheduleService
- **相关操作：** 增删改查、生成、冲突检测、人数管理

### ScriptTag
- **Service：** RecommendationService
- **相关操作：** 标签管理、相似度计算

### ScriptReview
- **Service：** ScriptReviewService
- **相关操作：** 增删改查、统计评分

### ScriptFavorite
- **Service：** ScriptFavoriteService
- **相关操作：** 收藏、取消、检查、查询列表

### Store
- **Service：** StoreService
- **相关操作：** 增删改查、登录、统计

### StoreRoom
- **Service：** StoreRoomService
- **相关操作：** 增删改查、可用房间查询

### StoreEmployee
- **Service：** StoreEmployeeService
- **相关操作：** 增删改查、状态更新、统计

### StoreReview
- **Service：** StoreReviewService
- **相关操作：** 增删改查、回复、统计

### Dm
- **Service：** DmService
- **相关操作：** 增删改查、状态更新、评分刷新

### Reservation
- **Service：** ReservationService, PaymentService
- **相关操作：** 创建、查询、确认、取消、支付、签到、完成

### GroupOrder
- **Service：** GroupOrderService
- **相关操作：** 创建、查询、加入、退出、取消、处理超时

### GroupMember
- **Service：** GroupOrderService
- **相关操作：** 成员管理

### Review
- **Service：** ReviewService
- **相关操作：** 创建、查询、审核、回复、精选设置

### Article
- **Service：** ArticleService
- **相关操作：** 发布、查询、点赞、浏览计数

### ArticleComment
- **Service：** ArticleCommentService
- **相关操作：** 发表、删除、点赞

### ArticleFavorite
- **Service：** ArticleFavoriteService
- **相关操作：** 收藏、取消

### Coupon
- **Service：** CouponService
- **相关操作：** 增删改查、状态更新、过期处理

### VipPackage
- **Service：** VipService
- **相关操作：** 增删改查、状态更新、统计

### AdminNotification
- **Service：** AdminNotificationService
- **相关操作：** 发送、查询、标记已读、删除

### SystemNotification
- **Service：** NotificationService
- **相关操作：** 发送、统计

### HotRanking
- **Service：** RecommendationService
- **相关操作：** 刷新、查询榜单

### RecommendationLog
- **Service：** RecommendationService
- **相关操作：** 记录、更新点击/预约状态、统计

### Feedback
- **Service：** FeedbackService
- **相关操作：** 提交、查询、回复、删除

### ServiceSession
- **Service：** ServiceSessionService
- **相关操作：** 创建、接入、关闭、评价

### ServiceMessage
- **Service：** ServiceSessionService
- **相关操作：** 发送、查询、标记已读

---

## 关键数据流向

### 预约流程中涉及的Service和Entity
```
User → Reservation → ScriptSchedule → Script
     ↓
   Dm
     ↓
   GroupOrder (可选)
     ↓
   Coupon/UserCoupon (可选)
     ↓
   UserVip (可选)
     ↓
   Review
     ↓
   UserPointsRecord
```

### 推荐系统中涉及的Entity
```
User → UserBrowseHistory → Script
     ↓
   UserPreference
     ↓
   RecommendationLog
     ↓
   HotRanking
```

### 评价审核流程中涉及的Entity
```
User → Reservation → Review
                   ↓
                ScriptReview/StoreReview
                   ↓
                UserPointsRecord (奖励)
                   ↓
                Coupon (奖励)
```

---

## Service间的依赖关系

```
UserService
  ↓
  ├─→ UserAddressService (用户地址)
  ├─→ UserPointsService (用户积分)
  ├─→ UserBrowseHistoryService (浏览历史)
  └─→ UserSettingsService (用户设置)

ReservationService
  ├─→ ScriptScheduleService (排期管理)
  ├─→ PaymentService (支付处理)
  ├─→ CouponService (优惠券)
  ├─→ VipService (VIP折扣)
  ├─→ UserPointsService (奖励积分)
  └─→ GroupOrderService (可选，拼单)

ReviewService
  ├─→ UserPointsService (评价奖励)
  ├─→ CouponService (评价奖励优惠券)
  └─→ DmService (DM评分更新)

RecommendationService
  ├─→ ScriptService (剧本信息)
  ├─→ UserBrowseHistoryService (浏览历史)
  ├─→ UserPreferenceMapper (用户偏好)
  └─→ HotRankingMapper (热门榜单)

StoreService
  ├─→ StoreRoomService (门店房间)
  ├─→ StoreEmployeeService (门店员工)
  ├─→ DmService (DM管理)
  └─→ StoreReviewService (评价统计)
```
