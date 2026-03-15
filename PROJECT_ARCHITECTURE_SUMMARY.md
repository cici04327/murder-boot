# Java Spring Boot 项目完整架构总结

## 项目概述

这是一个杀人游戏（谋杀推理游戏）预约和管理平台的Java Spring Boot后端项目。该项目采用标准的三层架构模式（Service - Mapper - Entity），使用MyBatis-Plus作为ORM框架，提供了完整的业务功能。

---

## 核心架构层次

### 1. Service 层（业务逻辑层）
- **总数：32个 Service 接口**
- **特点：**
  - 每个Service接口对应一个或多个Entity
  - 使用接口+实现类的分离模式
  - 实现类使用@Service注解
  - 通过注入Mapper层进行数据访问

### 2. Entity 层（数据模型层）
- **总数：38个 Entity 类**
- **特点：**
  - 使用Lombok简化代码（@Data、@Builder等）
  - 使用MyBatis-Plus注解（@TableName、@TableId、@TableField等）
  - 支持逻辑删除（@TableLogic）
  - 自动填充时间字段（@TableField(fill = FieldFill.INSERT_UPDATE)）

### 3. Mapper 层（数据访问层）
- **总数：40个 Mapper 接口**
- **特点：**
  - 全部继承自BaseMapper<T>
  - 提供CRUD基础操作
  - 部分Mapper增强了自定义SQL查询（@Select、@Update、@Delete等）
  - 使用MyBatis注解或XML配置SQL

---

## 业务模块分类

### 模块一：用户管理（10个Service）

#### 核心功能：
1. **UserService** - 用户注册登录、个人信息管理
2. **UserAddressService** - 用户地址簿管理
3. **UserBrowseHistoryService** - 用户浏览历史跟踪
4. **UserPointsService** - 积分系统（奖励、消费、兑换）
5. **UserSettingsService** - 用户隐私和偏好设置
6. **UserVipMapper** - VIP用户数据访问

#### 关键特性：
- 支持三种用户角色：普通用户(USER)、总部管理员(SUPER_ADMIN)、门店管理员(STORE_ADMIN)
- 完整的积分系统：签到、消费奖励、任务完成
- VIP会员等级管理
- 个性化用户偏好跟踪

---

### 模块二：剧本管理（7个Service）

#### 核心功能：
1. **ScriptService** - 剧本信息和分类管理
2. **ScriptRoleService** - 剧本角色配置
3. **ScriptScheduleService** - 剧本排期和时间段管理
4. **ScriptFavoriteService** - 用户剧本收藏
5. **ScriptReviewService** - 剧本评价管理

#### 关键特性：
- 多维剧本分类（类型、难度、人数等）
- 灵活的排期系统：支持日期范围、时间段、最大人数配置
- 冲突检测：防止时间段重叠预约
- 动态库存管理：实时跟踪排期人数

---

### 模块三：门店管理（6个Service）

#### 核心功能：
1. **StoreService** - 门店基础信息和登录
2. **StoreRoomService** - 门店房间管理
3. **StoreEmployeeService** - 门店员工管理
4. **StoreReviewService** - 门店评价和回复
5. **DmService** - DM（主持人）管理和评分

#### 关键特性：
- 支持地理位置查询（经纬度）
- 门店账号登录（独立于用户系统）
- DM管理：评分、主持场次跟踪、离职管理
- 营业时间配置
- 房间容纳人数管理

---

### 模块四：预约和支付（3个Service）

#### 核心功能：
1. **ReservationService** - 预约创建、取消、确认、完成
2. **PaymentService** - 支付宝支付集成、退款处理
3. **GroupOrderService** - 拼单功能（团购预约）

#### 关键特性：
- 完整的预约生命周期：创建→确认→支付→签到→完成→评价
- 支付状态跟踪：待支付、已支付、退款中、已退款
- 优惠券和VIP折扣自动计算
- 拼单功能：允许多用户组队预约同一剧本
- 超时处理：自动处理未成团拼单
- 签到码验证

---

### 模块五：评价系统（2个Service）

#### 核心功能：
1. **ReviewService** - 综合评价（门店、剧本、DM、服务）
2. **ScriptReviewService** - 纯剧本评价

#### 关键特性：
- 多维度评分：综合、门店、剧本、服务、DM
- 评价内容审核：待审核→已通过/已拒绝
- 商家回复机制
- 评价奖励：积分和优惠券
- 精选评价功能
- 匿名评价选项

---

### 模块六：文章内容（3个Service）

#### 核心功能：
1. **ArticleService** - 文章发布、编辑、分类
2. **ArticleCommentService** - 文章评论和点赞
3. **ArticleFavoriteService** - 文章收藏

#### 关键特性：
- 文章分类：新手攻略、选本技巧、榜单推荐、行业动态、玩家心得
- 评论回复和嵌套
- 浏览、点赞、评论、收藏四大指标跟踪
- 文章置顶和推荐

---

### 模块七：优惠和VIP（2个Service）

#### 核心功能：
1. **CouponService** - 优惠券生成、领取、使用、过期
2. **VipService** - VIP套餐购买、续费、权益管理

#### 关键特性：
- 多种优惠券类型：满减、折扣、代金券
- 优惠券有效期管理
- 积分兑换优惠券
- VIP等级：普通、银卡、金卡、钻石
- VIP权益：积分倍率、月度体验券、预约优先权、生日特权、专属客服
- VIP自动续费配置

---

### 模块八：通知系统（2个Service）

#### 核心功能：
1. **NotificationService** - 用户通知管理
2. **AdminNotificationService** - 管理端通知管理

#### 关键特性：
- 推送给指定用户或全体用户
- 通知类型分类
- 未读统计
- 通知搜索
- 统计信息（按类型、状态等）

---

### 模块九：辅助功能（5个Service）

#### 核心功能：
1. **RecommendationService** - 个性化推荐引擎
   - 协同过滤推荐
   - 基于内容的推荐
   - 基于历史的推荐
   - 热门推荐
   - 转化率追踪

2. **StatisticsService** - 数据分析和报表
   - 统计概览
   - 图表数据
   - 排行榜
   - 实时数据
   - 经营看板

3. **FeedbackService** - 用户反馈和建议
   - 留言收集
   - 管理员回复

4. **AIService** - AI对话和建议
   - 智能对话
   - 推荐建议
   - 常见问题库

5. **ServiceSessionService** - 客服转人工
   - 会话管理
   - 消息记录
   - 评价反馈

6. **ImageService** - 图片处理和CDN
   - CDN URL转换
   - 缩略图生成
   - WebP格式转换
   - 图片优化

---

## 数据库设计亮点

### 1. 时间戳管理
```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```
- 自动填充创建和更新时间

### 2. 逻辑删除
```java
@TableLogic
private Integer isDeleted;
```
- 数据软删除，便于数据恢复和审计

### 3. 关联关系
- **一对多：** Store ↔ StoreRoom, StoreEmployee, ScriptSchedule
- **多对多：** User ↔ Script (ScriptFavorite), User ↔ Article (ArticleFavorite)
- **一对多：** Script ↔ ScriptRole, ScriptSchedule, ScriptReview
- **一对一：** Reservation ↔ Review, UserVip ↔ VipPackage

### 4. 业务数据的冗余存储
- ScriptSchedule: 存储scriptName, roomName, dmName, dmAvatar（非数据库字段）
- GroupOrder: 存储creatorName, creatorAvatar, scriptName, storeName
- 目的：查询性能优化，减少关联查询

---

## Service 接口命名规范

### CRUD 操作
- `add()` / `create()` - 新增
- `update()` - 更新
- `delete()` / `remove()` - 删除
- `getById()` - 单条查询
- `list*()` - 列表查询
- `page*()` - 分页查询

### 业务操作
- `receive*()` / `claim*()` - 领取（优惠券、礼物等）
- `favorite()` / `unfavorite()` - 收藏/取消收藏
- `like()` / `unlike()` - 点赞/取消点赞
- `publish()` - 发布
- `audit()` / `verify()` - 审核
- `reply()` - 回复
- `confirm()` / `cancel()` - 确认/取消
- `checkIn()` - 签到
- `complete()` - 完成
- `refresh*()` - 刷新/重算
- `record*()` - 记录

### 查询统计
- `get*Stats()` - 获取统计信息
- `get*Statistics()` - 获取统计数据
- `count*()` - 统计数量
- `search*()` - 搜索

---

## Mapper 自定义方法统计

**有自定义SQL的Mapper（19个）：**
- UserBrowseHistoryMapper：4个自定义方法
- UserNotificationMapper：1个
- UserPreferenceMapper：3个
- UserVipMapper：1个
- ScriptScheduleMapper：2个
- ScriptTagMapper：5个
- ScriptReviewMapper：2个
- StoreEmployeeMapper：2个
- StoreReviewMapper：4个
- DmMapper：1个
- ReservationMapper：2个
- ArticleMapper：7个
- ArticleCommentMapper：2个
- AdminNotificationMapper：2个
- HotRankingMapper：2个
- RecommendationLogMapper：4个
- ServiceSessionMapper：2个
- ServiceMessageMapper：2个

**纯CRUD的Mapper（21个）：**
- 仅使用BaseMapper提供的标准方法

---

## 技术栈总结

| 技术 | 用途 |
|------|------|
| Spring Boot | 后端框架 |
| MyBatis-Plus | ORM框架 |
| Lombok | 代码简化 |
| Java 8+ | 编程语言 |
| LocalDateTime | 日期时间 |
| BigDecimal | 精确数值计算 |

---

## 关键业务流程

### 1. 预约流程
```
用户登录 → 查看剧本和排期 → 创建预约 → 确认预约 → 支付 
→ 签到 → 完成 → 评价 → 积分奖励
```

### 2. 拼单流程
```
发起人创建拼单 → 其他用户加入 → 人数达到要求自动成团 
→ 关联预约 → 支付 → 游戏 → 评价
```

### 3. VIP购买流程
```
用户选择VIP套餐 → 支付 → 获得VIP权益（积分倍率、月度优惠券等）
→ 自动续费或手动续费
```

### 4. 推荐系统流程
```
记录用户浏览 → 记录偏好 → 生成推荐列表 → 记录推荐点击
→ 记录转化（预约）→ 优化推荐算法
```

### 5. 评价审核流程
```
用户提交评价 → 待审核状态 → 管理员审核 
→ 通过/拒绝 → 商家回复 → 用户可见
```

---

## 扩展性设计

### 1. Service 层的易扩展性
- 接口+实现类分离
- 便于添加新的业务逻辑
- 易于进行事务管理和AOP拦截

### 2. Mapper 层的易扩展性
- BaseMapper 提供标准CRUD
- 支持自定义SQL注解
- 支持XML配置复杂查询

### 3. Entity 层的易扩展性
- Lombok 减少代码量
- MyBatis-Plus 注解灵活
- 支持逻辑删除和自动填充

### 4. 数据库设计的易扩展性
- 冗余字段预留
- 灵活的字符串存储（逗号分隔的标签）
- JSON字段支持（用户设置）

---

## 安全和规范建议

### 1. 数据校验
- Service 层应进行入参校验
- 使用 Spring Validation 框架
- 业务规则校验（如优惠券有效期）

### 2. 并发控制
- 库存扣减使用数据库行级锁
- 支付操作使用乐观锁或悲观锁
- 排期人数更新使用原子操作

### 3. 日志和审计
- 重要操作记录日志
- 支付操作记录完整流水
- 评价和退款操作记录原因

### 4. 权限控制
- 区分用户角色（USER、STORE_ADMIN、SUPER_ADMIN）
- 门店管理员仅能看自己门店的数据
- 总部管理员可以看全部数据

---

## 性能优化建议

### 1. 数据库优化
- 为频繁查询字段建立索引
- 使用分区表处理大数据量
- 定期清理过期浏览历史

### 2. 缓存策略
- 缓存热门剧本和门店信息
- 缓存用户VIP信息
- 缓存榜单数据

### 3. 查询优化
- 使用关联查询代替N+1查询
- 预加载相关数据
- 分页限制结果集大小

### 4. 异步处理
- 推送通知异步处理
- 积分计算异步更新
- 推荐算法异步刷新
