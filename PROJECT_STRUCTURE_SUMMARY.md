# 剧本杀预约与门店管理系统 - Spring Boot 单体应用 核心结构摘要

## 📋 项目概览

**项目名称**: Murder Boot (剧本杀预约与门店管理系统)  
**架构类型**: Spring Boot 3.2.5 单体应用  
**Java版本**: Java 17  
**打包方式**: JAR  
**主要用途**: 剧本杀游戏的预约、门店管理、用户社区、支付等综合平台

---

## 🏗️ 项目核心技术栈

### 后端框架
- **Spring Boot 3.2.5** - Web框架
- **MyBatis Plus 3.5.3** - ORM框架
- **MySQL 8.3.0** - 数据库驱动
- **Druid 1.2.20** - 数据库连接池
- **Redis** - 缓存和会话存储

### 认证与安全
- **JWT (JJWT 0.12.5)** - Token认证
- **Spring Security** - 权限控制

### API文档与接口
- **Knife4j 4.4.0** - API文档生成
- **OpenAPI 3** - API规范
- **Swagger** - API展示

### 其他关键依赖
- **Lombok 1.18.30** - 代码简化
- **FastJSON2 2.0.47** - JSON序列化
- **Hutool 5.8.25** - 工具类库
- **Alipay SDK 4.38.157** - 支付宝支付集成
- **MinIO 8.5.7** - 对象存储
- **Aliyun OSS 3.17.4** - 阿里云对象存储

---

## 📁 项目目录结构

```
src/main/java/com/murder/
├── MurderBootApplication.java          # 启动类
├── controller/                          # 控制层 (32个)
├── service/                             # 业务层
│   ├── *.java                          # 33个服务接口
│   └── impl/                           # 33个服务实现
├── entity/                              # 实体层 (35个实体)
├── mapper/                              # 数据访问层 (36个Mapper)
├── dto/                                 # 数据传输对象 (17个)
├── vo/                                  # 视图对象 (27个)
├── common/                              # 通用模块
│   ├── config/                         # 配置类 (9个)
│   ├── constant/                       # 常量定义 (4个)
│   ├── context/                        # 上下文工具
│   ├── exception/                      # 异常处理
│   ├── handler/                        # 请求/异常处理器
│   ├── properties/                     # 配置属性
│   ├── result/                         # 统一返回结果
│   └── utils/                          # 工具类
├── interceptor/                         # 拦截器 (2个)
├── task/                                # 定时任务 (5个)
└── websocket/                           # WebSocket处理 (3个)

src/main/resources/
├── application.yml                      # 主配置
├── application-dev.yml                  # 开发配置
├── application-prod.yml                 # 生产配置
├── ai-config.yml                        # AI配置
├── logback-spring.xml                   # 日志配置
└── static/                              # 静态资源
```

---

## 🔧 核心模块详解

### 1. 控制层 (Controller) - 32个接口

#### 用户相关
- **UserController** - 用户登录、注册、个人资料、地址管理
- **AuthController** - Token验证、刷新、健康检查
- **UserPointsController** - 积分查询和管理

#### 预约与支付
- **ReservationController** - 预约创建、查询、取消、核销、改期
- **PaymentController** - 支付处理
- **ReviewController** - 评价管理

#### 门店管理
- **StoreController** - 门店信息、列表查询、统计
- **StoreEmployeeController** - 员工管理
- **StoreRoomController** - 房间管理
- **StoreReviewController** - 门店评价

#### 剧本管理
- **ScriptController** - 剧本CRUD、分类、角色、搜索
- **ScriptFavoriteController** - 收藏功能
- **ScriptReviewController** - 剧本评价
- **ScriptRoleController** - 角色管理
- **ScriptScheduleController** - 排期管理

#### 内容与社区
- **ArticleController** - 文章发布、评论
- **FeedbackController** - 用户反馈
- **AIController** - AI客服对话、推荐、FAQ

#### 营销与通知
- **CouponController** - 优惠券管理、领取、使用
- **VipController** - VIP购买、查询
- **VipManageController** - VIP管理
- **NotificationController** - 用户通知
- **AdminNotificationController** - 管理端通知
- **GroupOrderController** - 拼团订单
- **DmController** - DM (主持人) 管理
- **ServiceSessionController** - 客服会话
- **StatisticsController** - 数据统计
- **RecommendationController** - 推荐系统
- **FileUploadController** - 文件上传

---

## 📊 数据模型 (Entity) - 35个实体

### 用户相关
- **User** - 用户基本信息、会员等级、VIP等级
- **UserAddress** - 用户地址
- **UserVip** - 用户VIP会员记录
- **UserCoupon** - 用户优惠券
- **UserPointsRecord** - 积分记录
- **UserBrowseHistory** - 浏览历史
- **UserSettings** - 用户设置
- **UserPreference** - 用户偏好
- **UserNotification** - 用户通知

### 门店相关
- **Store** - 门店基本信息（名称、地址、营业时间、评分等）
- **StoreRoom** - 门店房间（类型、容量、状态）
- **StoreEmployee** - 员工信息（职位、薪资、入职日期）
- **StoreReview** - 门店评价
- **Dm** - DM（主持人）信息，含风格标签、评分

### 预约相关
- **Reservation** - 预约订单（核心业务表）
  - 包含订单号、用户、门店、房间、剧本
  - 价格、优惠券、VIP折扣
  - 支付状态、退款、核销

### 剧本相关
- **Script** - 剧本基本信息（名称、分类、难度、人数、价格）
- **ScriptCategory** - 剧本分类
- **ScriptRole** - 角色列表
- **ScriptTag** - 剧本标签
- **ScriptSchedule** - 剧本排期（日期、时段、人数、DM分配）
- **ScriptFavorite** - 剧本收藏
- **ScriptReview** - 剧本评价

### 营销相关
- **Coupon** - 优惠券配置
- **VipPackage** - VIP套餐
- **Review** - 综合评价（预约后的多维度评价）
- **HotRanking** - 热门排行

### 内容相关
- **Article** - 文章（攻略、榜单、动态等）
- **ArticleComment** - 文章评论
- **ArticleFavorite** - 文章收藏
- **ArticleCommentLike** - 评论点赞
- **ArticleLike** - 文章点赞

### 服务相关
- **ServiceSession** - 客服会话
- **ServiceMessage** - 客服消息
- **SystemNotification** - 系统通知
- **AdminNotification** - 管理员通知
- **GroupMember** - 拼团成员
- **GroupOrder** - 拼团订单
- **RecommendationLog** - 推荐日志
- **Feedback** - 用户反馈

---

## 🔌 服务层 (Service) - 33个服务

### 核心业务服务
- **ReservationService** - 预约管理（创建、取消、改期、核销、支付）
- **PaymentService** - 支付处理
- **StoreService** - 门店管理
- **ScriptService** - 剧本管理
- **UserService** - 用户管理

### 营销服务
- **CouponService** - 优惠券
- **VipService** - VIP会员
- **RecommendationService** - 推荐系统

### 社区服务
- **ArticleService** - 文章管理
- **ArticleCommentService** - 文章评论
- **ArticleFavoriteService** - 收藏

### 门店模块
- **StoreEmployeeService** - 员工管理
- **StoreRoomService** - 房间管理
- **StoreReviewService** - 门店评价
- **ScriptScheduleService** - 排期管理
- **ScriptFavoriteService** - 剧本收藏
- **ScriptReviewService** - 剧本评价
- **ScriptRoleService** - 角色管理

### 用户服务
- **UserAddressService** - 地址管理
- **UserBrowseHistoryService** - 浏览历史
- **UserPointsService** - 积分管理
- **UserSettingsService** - 用户设置

### 通知与通信
- **NotificationService** - 通知管理
- **AdminNotificationService** - 管理员通知
- **ServiceSessionService** - 客服会话
- **AIService** - AI客服

### 其他服务
- **StatisticsService** - 统计分析
- **ImageService** - 图片处理
- **GroupOrderService** - 拼团管理
- **DmService** - DM管理
- **ReviewService** - 评价管理
- **FeedbackService** - 反馈管理

---

## 📝 DTO和VO (数据传输对象)

### DTO (17个) - 请求数据
- ReservationDTO, RescheduleDTO
- UserLoginDTO, UserRegisterDTO, UserUpdateDTO
- UserAddressDTO
- CouponDTO
- VipPackageDTO
- StoreLoginDTO, StoreQueryDTO
- ArticleDTO, ArticleCommentDTO
- ReviewDTO, ScriptReviewDTO
- DmDTO
- FeedbackDTO

### VO (27个) - 响应数据
- ReservationVO, UserLoginVO, StoreLoginVO
- StoreDetailVO, StoreVO, StoreStatisticsVO, StoreEmployeeVO, StoreReviewVO
- ArticleVO, ArticleCommentVO
- CouponVO, UserCouponVO
- VipPackageVO
- ReviewVO, ReviewStatisticsVO, ScriptReviewVO
- StatisticsOverviewVO, StatisticsChartsVO, StatisticsRankingVO, StatisticsRealtimeVO
- NotificationVO, AdminNotificationVO
- DmVO
- FeedbackVO
- RecommendationVO
- BrowseHistoryVO, UserPointsRecordVO

---

## 🔐 认证与安全

### JWT认证
- **JwtProperties** - JWT配置（分为用户、管理员、通用三套密钥）
- **JwtTokenInterceptor** - JWT拦截器
  - 智能密钥选择：通过X-Client-Type请求头
  - 权限验证：区分SUPER_ADMIN、STORE_ADMIN、USER
  - 线程上下文存储：userId、role、storeId

### 权限控制
- 基于角色的访问控制 (RBAC)
- 用户端/管理端分离认证
- 预约编号访问权限控制

---

## ⏰ 定时任务 (Task) - 5个

1. **ReservationTask** - 预约自动处理
   - 检查待支付预约
   - 自动完成已过期预约
   - 生成核销码

2. **CouponExpireTask** - 优惠券过期处理
   - 批量标记过期优惠券

3. **NotificationTask** - 通知提醒
   - 预约即将开始提醒
   - 支付超时提醒

4. **GroupOrderTask** - 拼团处理
   - 自动成团/取消

5. **RecommendationTask** - 推荐系统
   - 定期刷新推荐数据

---

## 🔌 WebSocket实时通信 - 3个处理器

1. **NotificationWebSocketHandler** - 用户通知推送
   - 连接管理
   - 实时消息推送
   - 心跳检测

2. **AdminNotificationWebSocketHandler** - 管理端通知
   - 后台消息推送

3. **ServiceWebSocketHandler** - 客服实时通信
   - 客服-用户实时对话

---

## 🔧 配置层 (Config) - 9个

1. **MybatisPlusConfig** - MyBatis Plus配置
2. **RedisConfig** - Redis配置（缓存、会话）
3. **WebMvcConfig** - Web MVC配置
   - 拦截器注册
   - 静态资源映射
   - CORS配置
4. **WebSocketConfig** - WebSocket配置
5. **RestTemplateConfig** - HTTP客户端
6. **JacksonConfig** - JSON序列化
7. **AlipayConfig** - 支付宝配置
8. **CdnConfig** - CDN配置
9. **MyBatisFixConfig** - MyBatis修复配置

---

## 📚 Mapper层 - 36个

实现数据库CRUD操作和复杂查询：
UserMapper, StoreMapper, ScriptMapper, ReservationMapper, ...等

所有Mapper使用MyBatis Plus的BaseMapper接口，支持：
- 基础CRUD
- 分页查询
- 条件查询
- 批量操作

---

## 📱 API端点总览

### 认证接口 (/api/auth)
```
POST   /api/auth/verify          - 验证Token
POST   /api/auth/refresh         - 刷新Token
GET    /api/auth/health          - 健康检查
```

### 用户接口 (/api/user)
```
POST   /api/user/login           - 登录
POST   /api/user/register        - 注册
GET    /api/user/{id}            - 获取用户信息
POST   /api/user/admin/login     - 管理员登录
```

### 预约接口 (/api/reservation)
```
POST   /api/reservation          - 创建预约
GET    /api/reservation/page     - 分页查询
GET    /api/reservation/{id}     - 获取预约详情
PUT    /api/reservation/{id}/confirm    - 确认预约
PUT    /api/reservation/{id}/cancel     - 取消预约
PUT    /api/reservation/{id}/check-in   - 到店核销
PUT    /api/reservation/{id}/pay        - 支付
PUT    /api/reservation/{id}/reschedule - 改期
```

### 门店接口 (/api/store)
```
GET    /api/store/page           - 门店列表
GET    /api/store/{id}/detail    - 门店详情
POST   /api/store/login          - 门店登录
GET    /api/store/statistics     - 统计数据
```

### 剧本接口 (/api/script)
```
GET    /api/script/page          - 剧本列表
GET    /api/script/{id}          - 剧本详情
GET    /api/script/list/hot      - 热门剧本
GET    /api/script/list/recommended - 推荐剧本
GET    /api/script/category      - 分类列表
GET    /api/script/{id}/roles    - 角色列表
```

### 优惠券接口 (/api/coupon)
```
GET    /api/coupon/page          - 优惠券列表
POST   /api/coupon/receive       - 领取优惠券
GET    /api/coupon/user          - 用户优惠券
```

### VIP接口 (/api/vip)
```
GET    /api/vip/packages         - VIP套餐列表
POST   /api/vip/purchase         - 购买VIP
GET    /api/vip/info             - VIP信息
```

### AI客服接口 (/api/ai)
```
POST   /api/ai/chat              - AI对话
GET    /api/ai/recommend/{type}  - AI推荐
GET    /api/ai/faq               - 常见问题
```

### WebSocket接口
```
WS     /api/ws/notification      - 通知推送
WS     /api/ws/admin-notification - 管理端通知
WS     /api/ws/service           - 客服通信
```

---

## ⚙️ 配置文件

### application.yml - 主配置
```yaml
server:
  port: 8080
  servlet.context-path: /

spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver: mysql 8.3.0
    url: jdbc:mysql://localhost:3306/murder
  data.redis:
    host: localhost
    port: 6379
    database: 1

mybatis-plus:
  type-aliases-package: com.murder.entity
  global-config.db-config.logic-delete-field: deleted

murder.jwt:
  user-secret-key: murder-user-secret-key-2024-secure-key-for-jwt
  user-ttl: 7200000 (2小时)
  admin-secret-key: murder-admin-secret-key-2024-secure-key-for-jwt
  admin-ttl: 7200000

ai:
  provider: openrouter
  model: deepseek/deepseek-r1-0528:free
  api-url: https://openrouter.ai/api/v1/chat/completions

alipay:
  app-id: your-app-id
  gateway-url: https://openapi.alipay.com/gateway.do

cdn:
  enabled: false
  domain: https://cdn.example.com
```

---

## 🎯 核心业务流程

### 1. 用户预约流程
```
用户注册/登录 → 浏览剧本 → 选择门店&房间
→ 查看排期 → 创建预约 → 输入优惠券/VIP → 支付
→ 生成核销码 → 到店核销 → 评价 → 积分奖励
```

### 2. 门店管理流程
```
门店登录 → 管理房间/员工 → 设置排期
→ 查看预约 → 核销管理 → 查看评价
→ 统计数据 → 营收分析
```

### 3. 营销运营流程
```
创建优惠券/VIP套餐 → 发放/推广
→ 用户领取/购买 → 预约时使用
→ 积分奖励 → 用户留存
```

---

## 🗄️ 数据库设计特点

1. **逻辑删除** - 所有表都有is_deleted字段，支持软删除
2. **时间审计** - 所有表都有create_time和update_time
3. **用户隔离** - 基于userId、storeId的数据隔离
4. **交易安全** - 预约表包含支付、退款、核销全流程字段
5. **多维评价** - 支持综合评价、门店评价、剧本评价、DM评价
6. **灵活营销** - 支持优惠券、VIP、积分等多种营销方式

---

## 📈 整体业务功能

### 核心功能
✅ 用户注册登录、个人资料管理  
✅ 剧本查询、搜索、分类、热门推荐  
✅ 门店信息查询、附近门店、热门门店  
✅ 预约创建、改期、取消、退款  
✅ 支付宝支付集成  
✅ 到店核销管理  
✅ 多维度评价系统  

### 营销功能
✅ 优惠券管理与使用  
✅ VIP会员体系（4个等级+多个套餐）  
✅ 积分系统  
✅ 拼团功能  
✅ 推荐算法  

### 社区功能
✅ 文章发布、评论、收藏  
✅ 用户反馈  
✅ 通知系统  

### AI功能
✅ AI客服对话  
✅ 常见问题库  
✅ AI推荐  

### 管理功能
✅ 门店管理员后台  
✅ 超级管理员后台  
✅ 数据统计与分析  
✅ 员工管理  
✅ 房间管理  
✅ 排期管理  

---

## 🚀 启动与运行

### 启动类
```
com.murder.MurderBootApplication
```

### 启动日志输出
```
剧本杀预约与门店管理系统启动成功！
本地访问: http://localhost:8080/
API文档: http://localhost:8080/doc.html
Druid监控: http://localhost:8080/druid
```

### 依赖启动
- MySQL 8.0+
- Redis
- 支付宝沙箱账号（可选）

---

## 📊 项目规模统计

| 分类 | 数量 | 说明 |
|------|------|------|
| Controller | 32 | 控制层接口 |
| Service | 33 | 业务逻辑 |
| Service Impl | 33 | 服务实现 |
| Entity | 35 | 数据模型 |
| Mapper | 36 | 数据访问 |
| DTO | 17 | 请求数据结构 |
| VO | 27 | 响应数据结构 |
| Config | 9 | 配置类 |
| Task | 5 | 定时任务 |
| WebSocket Handler | 3 | 实时通信 |
| **总计** | **230+** | **代码文件** |

---

## 🔒 安全特性

1. **JWT认证** - Token-based无状态认证
2. **角色权限** - SUPER_ADMIN、STORE_ADMIN、USER三层权限
3. **数据隔离** - 基于userId和storeId的数据隔离
4. **CORS支持** - 跨域资源共享配置
5. **请求拦截** - 全局异常处理和验证

---

## 📦 前后端集成

### 后端API文档
- Knife4j/Swagger UI: `/doc.html`
- OpenAPI 3 规范

### 前端应用（Separate）
- 用户端: `frontend/user/` (Vue 3 + Vite)
- 管理端: `frontend/admin/` (Vue 3 + Vite)
- 采用独立前后端分离架构

---

## 🎓 总结

这是一个**功能完整、架构清晰**的Spring Boot单体应用，整合了原有的微服务功能。系统包含：
- **完整的电商预约流程**（用户、商品、订单、支付、评价）
- **灵活的营销体系**（优惠券、VIP、积分、推荐）
- **实时通信能力**（WebSocket、通知、客服）
- **数据分析功能**（统计、排行、热度）
- **AI增强功能**（智能客服、推荐算法）

采用规范的分层架构、丰富的业务模型、现代化的技术栈，可支持数百万级用户规模的平台运营。

