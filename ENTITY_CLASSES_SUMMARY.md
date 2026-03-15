# Java Spring Boot 项目 Entity 实体类完整列表

## 概述
项目共有 **38个 Entity 实体类**，涵盖用户、剧本、门店、预约、评价等核心业务数据模型。

---

## 1. 用户相关 Entity

### 1.1 User
用户实体
- id: Long - 主键ID
- username: String - 用户名
- password: String - 密码
- nickname: String - 昵称
- phone: String - 手机号
- avatar: String - 头像
- gender: Integer - 性别（1男，2女）
- memberLevel: Integer - 会员等级
- vipLevel: Integer - VIP等级
- vipExpireTime: LocalDateTime - VIP到期时间
- points: Integer - 积分
- status: Integer - 状态（1启用，0禁用）
- role: String - 用户角色
- storeId: Long - 管理员所属门店ID
- isDeleted: Integer - 逻辑删除
- createTime: LocalDateTime - 创建时间
- updateTime: LocalDateTime - 更新时间

### 1.2 UserAddress
用户地址
- id: Long
- userId: Long
- contactName: String - 联系人姓名
- contactPhone: String - 联系电话
- province: String - 省份
- city: String - 城市
- district: String - 区县
- detail: String - 详细地址
- isDefault: Integer - 是否默认
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 1.3 UserBrowseHistory
用户浏览历史
- id: Long
- userId: Long
- targetType: Integer - 目标类型（1剧本，2门店）
- targetId: Long - 目标ID
- browseTime: LocalDateTime - 浏览时间
- duration: Integer - 浏览时长（秒）
- isDeleted: Integer
- createTime: LocalDateTime

### 1.4 UserCoupon
用户优惠券
- id: Long
- userId: Long
- couponId: Long
- orderId: Long - 关联订单ID
- status: Integer - 状态（1未使用，2已使用，3已过期）
- receiveTime: LocalDateTime - 领取时间
- useTime: LocalDateTime - 使用时间
- expireTime: LocalDateTime - 过期时间
- createTime: LocalDateTime

### 1.5 UserNotification
用户通知记录
- id: Long
- userId: Long
- notificationId: Long
- isRead: Integer - 是否已读
- readTime: LocalDateTime - 阅读时间
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 1.6 UserPointsRecord
用户积分记录
- id: Long
- userId: Long
- points: Integer - 积分变化
- type: Integer - 类型（1增加，2减少）
- source: Integer - 积分来源
- description: String - 描述
- isDeleted: Integer
- createTime: LocalDateTime

### 1.7 UserPreference
用户偏好
- id: Long
- userId: Long
- preferenceType: String - 偏好类型
- preferenceValue: String - 偏好值
- score: BigDecimal - 偏好分数
- count: Integer - 偏好次数
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 1.8 UserSettings
用户设置
- id: Long
- userId: Long
- privacySettings: String - 隐私设置JSON
- notificationSettings: String - 通知设置JSON
- preferenceSettings: String - 偏好设置JSON
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 1.9 UserVip
用户VIP记录
- id: Long
- userId: Long
- packageId: Long - 套餐ID
- level: Integer - 会员等级
- startTime: LocalDateTime - 开始时间
- endTime: LocalDateTime - 结束时间
- originalPrice: BigDecimal - 原价
- actualPrice: BigDecimal - 实付金额
- paymentMethod: String - 支付方式
- orderNo: String - 订单号
- transactionId: String - 第三方交易号
- status: Integer - 状态（1生效中，2已过期，3已取消）
- autoRenew: Integer - 自动续费
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 2. 剧本相关 Entity

### 2.1 Script
剧本实体
- id: Long
- name: String - 剧本名称
- categoryId: Long - 分类ID
- cover: String - 封面图片URL
- images: String - 详情图片（逗号分隔）
- description: String - 简介
- type: Integer - 剧本类型
- difficulty: Integer - 难度等级
- playerCount: Integer - 游戏人数
- duration: BigDecimal - 游戏时长（小时）
- price: BigDecimal - 价格
- tags: String - 标签（逗号分隔）
- rating: BigDecimal - 评分
- isExclusive: Integer - 是否独家
- status: Integer - 状态（1上架，0下架）
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 2.2 ScriptCategory
剧本分类
- id: Long
- name: String - 分类名称
- description: String - 分类描述
- sort: Integer - 排序
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 2.3 ScriptRole
剧本角色
- id: Long
- scriptId: Long - 剧本ID
- roleName: String - 角色名称
- avatar: String - 角色头像
- characterImage: String - 角色立绘
- gender: Integer - 性别要求
- ageRange: String - 年龄要求
- description: String - 角色描述
- difficulty: Integer - 角色难度
- sort: Integer - 排序
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 2.4 ScriptSchedule
剧本排期
- id: Long
- storeId: Long - 门店ID
- scriptId: Long - 剧本ID
- roomId: Long - 房间ID
- scheduleDate: LocalDate - 排期日期
- startTime: LocalTime - 开始时间
- endTime: LocalTime - 结束时间
- maxPlayers: Integer - 最大预约人数
- currentPlayers: Integer - 当前已预约人数
- status: Integer - 状态
- dmId: Long - 主持人ID
- remark: String - 备注
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 2.5 ScriptTag
剧本标签
- id: Long
- scriptId: Long
- tagName: String - 标签名称
- tagType: Integer - 标签类型
- weight: BigDecimal - 权重
- isDeleted: Integer
- createTime: LocalDateTime

### 2.6 ScriptReview
剧本评价
- id: Long
- scriptId: Long
- userId: Long
- reservationId: Long - 预约ID
- rating: Integer - 评分
- storyRating: Integer - 剧情评分
- difficultyRating: Integer - 难度评分
- content: String - 评价内容
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 2.7 ScriptFavorite
剧本收藏
- id: Long
- userId: Long
- scriptId: Long
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 3. 门店相关 Entity

### 3.1 Store
门店实体
- id: Long
- name: String - 门店名称
- address: String - 门店地址
- phone: String - 联系电话
- images: String - 门店图片（逗号分隔）
- description: String - 门店简介
- openTime: LocalTime - 营业开始时间
- closeTime: LocalTime - 营业结束时间
- longitude: BigDecimal - 经度
- latitude: BigDecimal - 纬度
- rating: BigDecimal - 评分
- status: Integer - 状态
- loginAccount: String - 门店登录账号
- loginPassword: String - 门店登录密码
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 3.2 StoreRoom
门店房间
- id: Long
- storeId: Long - 门店ID
- name: String - 房间名称
- type: Integer - 房间类型
- capacity: Integer - 容纳人数
- images: String - 房间图片
- description: String - 房间描述
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 3.3 StoreEmployee
门店员工
- id: Long
- storeId: Long
- name: String - 员工姓名
- phone: String - 联系电话
- position: Integer - 职位
- joinDate: LocalDate - 入职日期
- salary: BigDecimal - 月薪
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 3.4 StoreReview
门店评价
- id: Long
- storeId: Long
- userId: Long
- reservationId: Long
- rating: Integer - 总评分
- environmentRating: Integer - 环境评分
- serviceRating: Integer - 服务评分
- content: String - 评价内容
- reply: String - 商家回复
- images: String - 评价图片
- isAnonymous: Integer - 是否匿名
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 3.5 Dm
DM（主持人）
- id: Long
- storeId: Long - 门店ID
- name: String - DM姓名
- avatar: String - 头像
- introduction: String - 简介
- styleTags: String - 风格标签
- rating: BigDecimal - 综合评分
- gameCount: Integer - 累计主持场次
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 4. 预约相关 Entity

### 4.1 Reservation
预约订单
- id: Long
- orderNo: String - 订单号
- userId: Long
- storeId: Long
- roomId: Long
- scriptId: Long
- groupId: Long - 拼单ID
- scheduleId: Long - 排期ID
- dmId: Long - DM ID
- playerCount: Integer - 预约人数
- reservationTime: LocalDateTime - 预约时间
- duration: BigDecimal - 游戏时长
- totalPrice: BigDecimal - 总价
- couponId: Long
- discountAmount: BigDecimal - 优惠券折扣
- vipDiscountAmount: BigDecimal - VIP折扣金额
- vipDiscount: BigDecimal - VIP折扣
- actualAmount: BigDecimal - 实付金额
- contactName: String - 联系人
- contactPhone: String - 联系电话
- remark: String - 备注
- status: Integer - 预约状态
- payStatus: Integer - 支付状态
- payTime: LocalDateTime - 支付时间
- refundReason: String - 退款原因
- refundApplyTime: LocalDateTime - 申请退款时间
- refundProcessTime: LocalDateTime - 退款处理时间
- refundStatus: Integer - 退款状态
- checkInCode: String - 签到码
- checkInStatus: Integer - 签到状态
- checkInTime: LocalDateTime - 签到时间
- adminRemark: String - 管理员备注
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 4.2 GroupOrder
拼单
- id: Long
- creatorId: Long - 发起人ID
- creatorName: String
- creatorAvatar: String
- scriptId: Long
- scriptName: String
- storeId: Long
- storeName: String
- playTime: LocalDateTime - 开车时间
- currentCount: Integer - 当前人数
- needCount: Integer - 需要总人数
- playerCount: Integer - 剧本人数
- price: BigDecimal - 每人价格
- genderRequirement: String - 性别要求
- newbieWelcome: Integer - 新手友好
- description: String - 拼单说明
- reservationId: Long - 关联预约ID
- status: Integer - 拼单状态
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 4.3 GroupMember
拼单成员
- id: Long
- groupId: Long
- userId: Long
- nickname: String
- avatar: String
- isCreator: Integer - 是否发起人
- joinCount: Integer - 参与人数
- status: Integer - 成员状态
- createTime: LocalDateTime

---

## 5. 评价相关 Entity

### 5.1 Review
综合评价
- id: Long
- reservationId: Long
- userId: Long
- storeId: Long
- scriptId: Long
- overallRating: Integer - 综合评分
- storeRating: Integer - 门店评分
- scriptRating: Integer - 剧本评分
- serviceRating: Integer - 服务评分
- dmId: Long
- dmRating: Integer - DM评分
- content: String - 评价内容
- images: String - 评价图片
- tags: String - 评价标签
- status: Integer - 状态
- isAnonymous: Integer - 是否匿名
- isFeatured: Integer - 是否精选
- replyContent: String - 商家回复
- replyTime: LocalDateTime - 回复时间
- rewardPoints: Integer - 奖励积分
- rewardCouponId: Long - 奖励优惠券ID
- auditUserId: Long - 审核人ID
- auditTime: LocalDateTime - 审核时间
- auditReason: String - 审核原因
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 6. 文章相关 Entity

### 6.1 Article
文章实体
- id: Long
- title: String - 文章标题
- summary: String - 文章摘要
- content: String - 文章内容（富文本）
- coverImage: String - 封面图片
- category: Integer - 文章分类
- categoryName: String - 分类名称
- authorId: Long - 作者ID
- authorName: String - 作者名称
- viewCount: Integer - 浏览次数
- likeCount: Integer - 点赞次数
- commentCount: Integer - 评论数
- favoriteCount: Integer - 收藏数
- isTop: Integer - 是否置顶
- isRecommended: Integer - 是否推荐
- status: Integer - 状态
- publishTime: LocalDateTime - 发布时间
- createTime: LocalDateTime
- updateTime: LocalDateTime
- createUser: Long
- updateUser: Long
- isDeleted: Integer

### 6.2 ArticleComment
文章评论
- id: Long
- articleId: Long
- userId: Long
- userName: String
- userAvatar: String
- content: String - 评论内容
- parentId: Long - 父评论ID
- replyToUserId: Long - 回复目标用户ID
- replyToUserName: String
- likeCount: Integer - 点赞数
- status: Integer - 状态
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 6.3 ArticleCommentLike
评论点赞
- id: Long
- commentId: Long
- userId: Long
- createTime: LocalDateTime

### 6.4 ArticleLike
文章点赞
- id: Long
- articleId: Long
- userId: Long
- createTime: LocalDateTime

### 6.5 ArticleFavorite
文章收藏
- id: Long
- userId: Long
- articleId: Long
- createTime: LocalDateTime

---

## 7. 优惠券相关 Entity

### 7.1 Coupon
优惠券
- id: Long
- name: String - 优惠券名称
- type: Integer - 类型（1满减，2折扣，3代金券）
- discountValue: BigDecimal - 折扣值
- minAmount: BigDecimal - 最低消费金额
- totalCount: Integer - 发行总量
- remainCount: Integer - 剩余数量
- validStartTime: LocalDateTime - 有效期开始
- validEndTime: LocalDateTime - 有效期结束
- description: String - 描述
- exchangePoints: Integer - 兑换所需积分
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 8. VIP相关 Entity

### 8.1 VipPackage
VIP套餐配置
- id: Long
- name: String - 套餐名称
- level: Integer - 会员等级
- durationType: Integer - 时长类型
- durationDays: Integer - 时长天数
- originalPrice: BigDecimal - 原价
- currentPrice: BigDecimal - 现价
- discountRate: BigDecimal - 折扣率
- pointMultiplier: BigDecimal - 积分倍率
- couponCount: Integer - 每月赠送优惠券数量
- priorityBooking: Integer - 预约优先权
- exclusiveService: Integer - 专属客服
- birthdayGift: Integer - 生日特权
- exclusiveBadge: Integer - 专属徽章
- specialDiscount: BigDecimal - 专属折扣
- description: String - 套餐描述
- features: String - 权益列表JSON
- tag: String - 标签
- sortOrder: Integer - 排序
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 9. 通知相关 Entity

### 9.1 AdminNotification
管理端通知
- id: Long
- title: String - 通知标题
- content: String - 通知内容
- type: Integer - 通知类型
- bizType: String - 业务类型
- bizId: Long - 业务ID
- targetType: Integer - 目标类型
- targetUsers: String - 目标用户ID列表
- storeId: Long - 门店ID
- priority: Integer - 优先级
- isRead: Integer - 是否已读
- readTime: LocalDateTime - 阅读时间
- sendTime: LocalDateTime - 发送时间
- status: Integer - 状态
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 9.2 SystemNotification
系统通知
- id: Long
- title: String
- content: String
- type: Integer
- bizType: String
- bizId: Long
- targetType: Integer
- targetUsers: String
- sendTime: LocalDateTime
- status: Integer
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

---

## 10. 其他 Entity

### 10.1 HotRanking
热门榜单缓存
- id: Long
- rankingType: Integer - 榜单类型
- scriptId: Long
- rank: Integer - 排名
- score: BigDecimal - 热度分数
- viewCount: Integer - 浏览量
- reserveCount: Integer - 预约量
- favoriteCount: Integer - 收藏量
- rating: BigDecimal - 评分
- updateTime: LocalDateTime

### 10.2 RecommendationLog
推荐记录
- id: Long
- userId: Long
- scriptId: Long
- recommendationType: Integer - 推荐类型
- score: BigDecimal - 推荐分数
- isClicked: Integer - 是否点击
- clickTime: LocalDateTime - 点击时间
- isReserved: Integer - 是否预约
- reserveTime: LocalDateTime - 预约时间
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 10.3 Feedback
用户留言反馈
- id: Long
- userId: Long - 用户ID（可为空）
- name: String - 留言人姓名
- contact: String - 联系方式
- subject: String - 主题类型
- message: String - 留言内容
- status: Integer - 状态
- replyContent: String - 管理员回复内容
- replyTime: LocalDateTime - 回复时间
- replyUserId: Long - 回复管理员ID
- ipAddress: String - IP地址
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 10.4 ServiceSession
客服会话
- id: Long
- userId: Long
- userName: String
- adminId: Long - 接待管理员ID
- status: Integer - 会话状态
- initialQuestion: String - 初始问题
- endTime: LocalDateTime - 会话结束时间
- rating: Integer - 用户评价分数
- ratingComment: String - 用户评价内容
- isDeleted: Integer
- createTime: LocalDateTime
- updateTime: LocalDateTime

### 10.5 ServiceMessage
客服消息
- id: Long
- sessionId: Long
- senderType: String - 发送方类型
- senderId: Long - 发送方ID
- content: String - 消息内容
- msgType: String - 消息类型
- isRead: Integer - 是否已读
- createTime: LocalDateTime
