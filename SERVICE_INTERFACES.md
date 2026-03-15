# Java Spring Boot 项目 Service 层接口完整列表

## 概述
项目共有 **32个 Service 接口**，涵盖用户、剧本、门店、预约、评价、通知等核心业务功能。

---

## 1. 用户相关 Service

### 1.1 UserService
用户登录、注册、个人信息管理
- register(UserRegisterDTO) - 用户注册
- login(UserLoginDTO) - 用户登录
- adminLogin(UserLoginDTO) - 管理员登录
- getUserById(Long id) - 根据ID获取用户信息
- pageQuery(Integer page, Integer pageSize, String username, String phone, Integer memberLevel) - 分页查询用户
- updatePassword(User user) - 修改密码
- addPointsForProfile(Long userId) - 完善资料奖励积分

### 1.2 UserAddressService
用户地址管理
- listByUserId(Long userId) - 查询用户地址列表
- getById(Long id) - 查询地址详情
- add(UserAddressDTO addressDTO) - 新增地址
- update(UserAddressDTO addressDTO) - 更新地址
- delete(Long id) - 删除地址
- setDefault(Long id, Long userId) - 设置默认地址
- getDefaultAddress(Long userId) - 获取默认地址

### 1.3 UserBrowseHistoryService
用户浏览记录跟踪
- pageHistory(Long userId, Integer page, Integer pageSize, Integer days, Integer targetType) - 分页查询浏览历史
- recordHistory(Long userId, Integer targetType, Long targetId, Integer duration) - 记录浏览历史
- deleteHistory(Long userId, Long historyId) - 删除单条浏览记录
- clearHistory(Long userId) - 清空用户浏览历史
- getHistoryStats(Long userId) - 获取浏览历史统计

### 1.4 UserPointsService
用户积分管理系统
- getPointsInfo(Long userId) - 获取用户积分信息（含统计）
- pageQuery(Integer page, Integer pageSize, Long userId, Integer type) - 分页查询积分记录
- getTotalPoints(Long userId) - 获取用户总积分
- addPoints(Long userId, Integer points, String reason) - 增加积分
- deductPoints(Long userId, Integer points, String reason) - 扣减积分
- signIn(Long userId) - 每日签到
- exchangeCoupon(Long userId, Long couponId, Integer points) - 积分兑换优惠券
- getTasksStatus(Long userId) - 获取任务完成状态
- rewardForFavorite(Long userId) - 收藏剧本奖励积分
- rewardForReservation(Long userId, Long reservationId) - 完成预约奖励积分
- deductForRefund(Long userId, Long reservationId) - 退款时扣除预约奖励的积分

### 1.5 UserSettingsService
用户偏好和隐私设置
- getPrivacySettings(Long userId) - 获取隐私设置
- updatePrivacySettings(Long userId, Map<String, Object> settings) - 更新隐私设置
- getNotificationSettings(Long userId) - 获取通知设置
- updateNotificationSettings(Long userId, Map<String, Object> settings) - 更新通知设置
- getPreferenceSettings(Long userId) - 获取偏好设置
- updatePreferenceSettings(Long userId, Map<String, Object> settings) - 更新偏好设置

---

## 2. 剧本相关 Service

### 2.1 ScriptService
剧本管理和查询
- pageQuery(Integer page, Integer pageSize, String name, Long categoryId, String difficulty, Integer playerCount, String sortBy, Integer type) - 分页查询剧本
- getHotScripts() - 获取热门剧本
- getRecommendedScripts() - 获取推荐剧本
- getById(Long id) - 根据ID查询剧本详情
- add(Script script) - 新增剧本
- update(Script script) - 更新剧本
- delete(Long id) - 删除剧本
- getCategories() - 查询剧本分类列表
- addCategory(ScriptCategory category) - 新增剧本分类
- updateCategory(Long id, ScriptCategory category) - 更新剧本分类
- deleteCategory(Long id) - 删除剧本分类
- getRolesByScriptId(Long scriptId) - 根据剧本ID获取角色列表

### 2.2 ScriptFavoriteService
剧本收藏功能
- favoriteScript(Long userId, Long scriptId) - 收藏剧本
- unfavoriteScript(Long userId, Long scriptId) - 取消收藏剧本
- isFavorited(Long userId, Long scriptId) - 检查是否已收藏
- getUserFavorites(Long userId, Integer page, Integer pageSize) - 获取用户收藏的剧本列表
- getFavoriteCount(Long userId) - 获取用户收藏数量

### 2.3 ScriptReviewService
剧本评价管理
- add(ScriptReviewDTO reviewDTO) - 添加剧本评价
- pageQuery(Long scriptId, Integer page, Integer pageSize) - 分页查询剧本评价
- getById(Long id) - 根据ID查询评价详情
- delete(Long id) - 删除评价
- getByReservationId(Long reservationId) - 根据预约ID查询评价

### 2.4 ScriptRoleService
剧本角色管理
- listByScriptId(Long scriptId) - 根据剧本ID查询角色列表
- getById(Long id) - 根据ID查询角色详情
- add(ScriptRole scriptRole) - 新增角色
- update(ScriptRole scriptRole) - 更新角色
- delete(Long id) - 删除角色
- addBatch(List<ScriptRole> roles) - 批量新增角色

### 2.5 ScriptScheduleService
剧本排期管理
- listByStoreAndDate(Long storeId, LocalDate scheduleDate) - 查询门店某日期的排期列表
- listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) - 查询门店日期范围内的排期
- add(ScriptSchedule schedule) - 新增排期
- batchAdd(List<ScriptSchedule> schedules) - 批量新增排期
- update(ScriptSchedule schedule) - 更新排期
- delete(Long id) - 删除排期
- getById(Long id) - 根据ID查询排期
- updateStatus(Long id, Integer status) - 更新排期状态
- generateSchedules(Long storeId, Long scriptId, Long roomId, LocalDate startDate, LocalDate endDate, List<String> timeSlots, Integer maxPlayers) - 批量生成排期
- incrementCurrentPlayers(Long scheduleId, int count) - 增加排期已预约人数
- decrementCurrentPlayers(Long scheduleId, int count) - 减少排期已预约人数
- getAvailableSchedules(Long scriptId, Long storeId, Integer days) - 查询可约场次
- checkConflict(Long storeId, Long roomId, String scheduleDate, String startTime, String endTime, Long excludeId) - 实时冲突检测

---

## 3. 门店相关 Service

### 3.1 StoreService
门店管理和查询
- pageQuery(Integer page, Integer pageSize, String name) - 分页查询门店列表
- pageQueryAdvanced(StoreQueryDTO queryDTO) - 多条件分页查询门店
- getById(Long id) - 根据ID查询门店详情
- getDetailById(Long id) - 获取门店详细信息（包含房间、员工、评价统计）
- add(Store store) - 新增门店
- update(Store store) - 更新门店
- delete(Long id) - 删除门店
- batchDelete(List<Long> ids) - 批量删除门店
- updateStatus(Long id, Integer status) - 更新门店状态
- batchUpdateStatus(List<Long> ids, Integer status) - 批量更新门店状态
- getStatistics(Long storeId) - 获取门店统计信息
- listAll() - 获取所有门店列表（不分页）
- getHotStores() - 获取热门门店
- getRecommendedStores() - 获取推荐门店
- getNearbyStores(Double latitude, Double longitude, Integer limit) - 获取附近门店（基于地理位置）
- storeLogin(StoreLoginDTO storeLoginDTO) - 门店账号登录
- updateLoginPassword(Long storeId, String oldPassword, String newPassword) - 更新门店登录密码
- updateStoreAccount(Long storeId, String loginAccount, String loginPassword) - 更新门店账号信息
- resetStorePassword(Long storeId) - 重置门店密码为默认值

### 3.2 StoreEmployeeService
门店员工管理
- pageQuery(Long storeId, Integer page, Integer pageSize) - 分页查询门店员工列表
- listByStoreId(Long storeId) - 根据门店ID查询员工列表
- getById(Long id) - 根据ID查询员工详情
- add(StoreEmployeeDTO employeeDTO) - 新增员工
- update(StoreEmployeeDTO employeeDTO) - 更新员工
- delete(Long id) - 删除员工
- batchDelete(List<Long> ids) - 批量删除员工
- updateStatus(Long id, Integer status) - 更新员工状态

### 3.3 StoreRoomService
门店房间管理
- listByStoreId(Long storeId) - 根据门店ID查询房间列表
- getById(Long id) - 根据ID查询房间详情
- add(StoreRoom room) - 新增房间
- update(StoreRoom room) - 更新房间
- delete(Long id) - 删除房间
- getAvailableRooms(Long storeId) - 查询可用房间

### 3.4 StoreReviewService
门店评价管理
- pageQuery(Long storeId, Integer page, Integer pageSize) - 分页查询门店评价列表
- getById(Long id) - 根据ID查询评价详情
- add(StoreReview review) - 新增评价
- reply(Long id, String reply) - 商家回复评价
- delete(Long id) - 删除评价

### 3.5 DmService
DM（主持人）管理
- pageQuery(Long storeId, Integer status, Integer page, Integer pageSize) - 分页查询（管理端）
- listByStoreId(Long storeId) - 查询门店下所有在职DM（排期分配/用户端展示用）
- getById(Long id) - 根据ID查询
- add(DmDTO dmDTO) - 新增
- update(DmDTO dmDTO) - 编辑
- delete(Long id) - 删除
- updateStatus(Long id, Integer status) - 更新状态（在职/离职）
- refreshRating(Long dmId) - 刷新DM评分和场次（评价提交后调用）

---

## 4. 预约和支付相关 Service

### 4.1 ReservationService
预约管理
- create(ReservationDTO reservationDTO) - 创建预约
- pageQuery(Integer page, Integer pageSize, Long userId, Integer status) - 分页查询预约
- pageQueryWithDetails(Integer page, Integer pageSize, Long userId, Long storeId, Integer status, Integer payStatus, Integer checkInStatus, Integer refundStatus, Boolean hasRefund) - 分页查询预约（带详情）
- getById(Long id) - 根据ID查询预约
- getDetailById(Long id) - 查询预约详情
- getByReservationNo(String reservationNo) - 根据预约号查询
- confirm(Long id) - 确认预约
- cancel(Long id, String reason) - 取消预约
- complete(Long id) - 完成预约
- checkIn(Long id, String checkInCode) - 签到
- getCompletableReservations() - 获取可完成的预约
- getUnpaidReservations(LocalDateTime timeoutTime) - 获取未支付预约
- pay(Long id) - 支付
- getUpcomingReservations(Integer hours) - 获取即将开始的预约
- checkRoomAvailability(Long roomId, String reservationTime, Double duration) - 检查房间可用性
- reschedule(Long id, String newReservationTime) - 重新排期

### 4.2 PaymentService
支付处理
- createPayment(Long reservationId, String paymentMethod) - 创建支付宝支付订单
- handleAlipayNotify(Map<String, String> params) - 支付宝异步通知处理
- queryPaymentStatus(Long reservationId) - 查询支付状态
- applyRefund(Long reservationId, String reason) - 申请退款（用户端）
- processRefund(Long reservationId, Integer approved, String adminRemark) - 处理退款（管理端）
- autoRefund(Long reservationId, String reason) - 系统自动退款

### 4.3 GroupOrderService
拼单管理（继承IService）
- pageQuery(Integer page, Integer pageSize, Long scriptId, Long categoryId, Integer playerCount, Integer status) - 分页查询拼单
- getHotGroups(Integer limit) - 获取热门拼单
- getDetailWithMembers(Long id) - 获取拼单详情（包含成员列表）
- createGroup(GroupOrder groupOrder, Long userId) - 创建拼单
- joinGroup(Long groupId, Long userId, Integer joinCount) - 加入拼单
- leaveGroup(Long groupId, Long userId) - 退出拼单
- cancelGroup(Long groupId, Long userId) - 取消拼单
- getMyGroups(Long userId, Integer page, Integer pageSize, Integer type) - 获取我的拼单
- getGroupMembers(Long groupId) - 获取拼单成员
- processExpiredGroups() - 处理超时未成团的拼单

---

## 5. 评价相关 Service

### 5.1 ReviewService
综合评价管理
- create(ReviewDTO reviewDTO) - 创建评价
- pageQuery(Integer page, Integer pageSize, Long storeId, Long scriptId, Long userId, Integer status) - 分页查询评价列表
- getById(Long id) - 根据ID查询评价详情
- getByReservationId(Long reservationId) - 根据预约ID查询评价
- delete(Long id) - 删除评价
- audit(Long id, Integer status, String reason) - 审核评价
- reply(Long id, String replyContent) - 回复评价
- setFeatured(Long id, Integer isFeatured) - 设置精选评价
- getStatistics(Long storeId, Long scriptId) - 获取评价统计信息

---

## 6. 文章相关 Service

### 6.1 ArticleService
文章管理和查询
- pageQuery(Integer page, Integer pageSize, Integer category, String keyword, Integer status, String sortBy) - 分页查询文章列表
- getById(Long id) - 根据ID查询文章详情
- getHotArticles(Integer limit) - 获取热门文章
- getRecommendedArticles(Integer limit) - 获取推荐文章
- add(ArticleDTO articleDTO) - 新增文章
- update(ArticleDTO articleDTO) - 更新文章
- delete(Long id) - 删除文章
- increaseViewCount(Long id) - 增加浏览次数
- likeArticle(Long id) - 点赞文章
- unlikeArticle(Long id) - 取消点赞文章
- isLiked(Long articleId, Long userId) - 检查用户是否已点赞文章

### 6.2 ArticleCommentService
文章评论管理
- getCommentsByArticleId(Long articleId) - 获取文章评论列表
- addComment(ArticleCommentDTO commentDTO) - 添加评论
- deleteComment(Long id) - 删除评论
- likeComment(Long commentId) - 点赞评论
- unlikeComment(Long commentId) - 取消点赞评论

### 6.3 ArticleFavoriteService
文章收藏
- favoriteArticle(Long articleId) - 收藏文章
- unfavoriteArticle(Long articleId) - 取消收藏
- isFavorited(Long articleId, Long userId) - 检查用户是否已收藏文章

---

## 7. 优惠券相关 Service

### 7.1 CouponService
优惠券管理
- pageQuery(Integer page, Integer pageSize, String name, Integer type, Integer status) - 分页查询优惠券列表
- getById(Long id) - 根据ID查询优惠券详情
- add(CouponDTO couponDTO) - 新增优惠券
- update(CouponDTO couponDTO) - 更新优惠券
- delete(Long id) - 删除优惠券
- updateStatus(Long id, Integer status) - 上架/下架优惠券
- receiveCoupon(Long userId, Long couponId) - 用户领取优惠券
- getUserCoupons(Long userId, Integer page, Integer pageSize, Integer status) - 查询用户的优惠券列表
- getAvailableCoupons(Long userId, BigDecimal orderAmount) - 查询用户可用的优惠券列表
- useCoupon(Long userCouponId, Long orderId) - 使用优惠券（与订单关联）
- calculateDiscount(Long userCouponId, BigDecimal orderAmount) - 计算优惠金额
- refundCoupon(Long orderId) - 退还优惠券（订单取消时）
- expireCoupons() - 批量过期优惠券（定时任务）
- getCouponStatistics(Long couponId) - 获取优惠券统计信息

---

## 8. VIP 相关 Service

### 8.1 VipService
VIP管理
- getVipPackages() - 获取VIP套餐列表
- getVipPackageById(Long id) - 根据ID获取VIP套餐详情
- purchaseVip(Long userId, Long packageId, String paymentMethod) - 购买VIP
- getUserVipInfo(Long userId) - 获取用户VIP信息
- isVip(Long userId) - 检查用户是否是VIP
- getUserVipLevel(Long userId) - 获取用户VIP等级
- getPointMultiplier(Long userId) - 获取用户积分倍率
- getVipDiscount(Long userId) - 获取用户VIP折扣
- hasPriorityBooking(Long userId) - 检查用户是否有预约优先权
- grantMonthlyCoupons(Long userId) - 发放月度体验券
- getMonthlyCouponStatus(Long userId) - 查询用户本月月度体验券发放情况
- adminGrantMonthlyCoupons(Long userId, int year, int month, String reason) - 管理员手动补发月度体验券
- renewVip(Long userId, Long packageId) - 续费VIP
- checkAndUpdateExpiredVip() - 检查并更新过期VIP
- getUserVipHistory(Long userId, Integer page, Integer pageSize) - 获取用户VIP历史记录
- createPackage(VipPackageDTO dto) - 创建VIP套餐
- updatePackage(VipPackageDTO dto) - 更新VIP套餐
- deletePackage(Long id) - 删除VIP套餐
- updatePackageStatus(Long id, Integer status) - 上下架VIP套餐
- pageQueryPackages(Integer page, Integer pageSize, Integer level, Integer status) - 分页查询VIP套餐（管理端）
- grantVip(Long userId, Integer days, Integer level, String reason) - 赠送VIP
- extendVip(Long userId, Integer days) - 延长VIP
- getVipStatistics() - 获取VIP统计数据
- pageQueryVipUsers(Integer page, Integer pageSize, Integer level, Integer status) - 分页查询VIP用户列表

---

## 9. 通知相关 Service

### 9.1 NotificationService
用户通知管理
- sendToUsers(String title, String content, Integer type, String bizType, Long bizId, Long... userIds) - 发送通知给指定用户
- sendToAll(String title, String content, Integer type, String bizType, Long bizId) - 发送通知给所有用户
- getUserNotifications(Long userId, Integer page, Integer pageSize, Boolean onlyUnread) - 分页查询用户通知列表
- markAsRead(Long userId, Long notificationId) - 标记通知为已读
- markAllAsRead(Long userId) - 标记所有通知为已读
- getUnreadCount(Long userId) - 获取未读通知数量
- deleteNotification(Long userId, Long notificationId) - 删除通知
- batchDeleteNotifications(Long userId, List<Long> notificationIds) - 批量删除通知
- batchMarkAsRead(Long userId, List<Long> notificationIds) - 批量标记通知为已读
- getNotificationDetail(Long userId, Long notificationId) - 获取通知详情
- searchNotifications(Long userId, String keyword, Integer page, Integer pageSize) - 搜索通知
- getNotificationStatistics(Long userId) - 获取通知统计信息
- clearReadNotifications(Long userId) - 清空已读通知

### 9.2 AdminNotificationService
管理端通知
- sendNotification(String title, String content, Integer type, String bizType, Long bizId, Long storeId, Integer priority) - 发送管理端通知
- getNotifications(Integer page, Integer pageSize, Long storeId, Integer type, Boolean onlyUnread) - 分页查询管理端通知列表
- markAsRead(Long notificationId) - 标记通知为已读
- markAllAsRead(Long storeId) - 标记所有通知为已读
- getUnreadCount(Long storeId) - 获取未读通知数量
- deleteNotification(Long notificationId) - 删除通知
- batchDeleteNotifications(List<Long> notificationIds) - 批量删除通知
- getNotificationDetail(Long notificationId) - 获取通知详情
- getStatistics(Long storeId) - 获取通知统计信息

---

## 10. 其他 Service

### 10.1 RecommendationService
推荐系统
- getPersonalizedRecommendations(Long userId, Integer limit) - 获取个性化推荐（综合推荐）
- getCollaborativeRecommendations(Long userId, Long scriptId, Integer limit) - 基于协同过滤的推荐
- getContentBasedRecommendations(Long scriptId, Integer limit) - 基于内容的推荐（相似剧本）
- getHistoryBasedRecommendations(Long userId, Integer limit) - 基于用户历史的推荐
- getHotRecommendations(Integer rankingType, Integer limit) - 获取热门推荐
- getNewScriptRecommendations(Integer limit) - 获取新品推荐
- recordBrowseHistory(Long userId, Integer targetType, Long targetId, Integer duration) - 记录用户浏览行为
- recordRecommendationClick(Long userId, Long scriptId) - 记录推荐点击
- recordRecommendationReserve(Long userId, Long scriptId) - 记录推荐预约转化
- updateUserPreference(Long userId, Long scriptId, Integer actionType) - 更新用户偏好
- refreshHotRanking(Integer rankingType) - 刷新热门榜单（定时任务调用）
- getRecommendationStats(Integer days) - 获取推荐效果统计

### 10.2 FeedbackService
留言反馈
- submit(FeedbackDTO feedbackDTO, Long userId, String ipAddress) - 提交留言
- pageQuery(Integer page, Integer pageSize, String subject, Integer status) - 分页查询留言列表（管理端）
- getUserFeedbacks(Long userId, Integer page, Integer pageSize) - 查询用户的留言列表
- getById(Long id) - 根据ID查询留言详情
- reply(Long id, String replyContent, Long replyUserId) - 回复留言（管理端）
- updateStatus(Long id, Integer status) - 更新留言状态（管理端）
- delete(Long id) - 删除留言（管理端）

### 10.3 AIService
AI对话和建议
- chat(String message, String sessionId, List<Map<String, Object>> history, Map<String, Object> context) - AI对话
- getRecommendation(String type) - 获取推荐
- logConversation(Map<String, Object> params) - 记录对话
- getFrequentQuestions() - 获取常见问题
- submitFeedback(Map<String, Object> params) - 提交反馈

### 10.4 ServiceSessionService
客服会话管理
- createSession(Long userId, String userName, String initialQuestion) - 用户发起转人工请求
- acceptSession(Long sessionId, Long adminId) - 管理员接入会话
- sendMessage(Long sessionId, String senderType, Long senderId, String content, String msgType) - 发送消息
- closeSession(Long sessionId, String closerType) - 结束会话
- rateSession(Long sessionId, Integer rating, String comment) - 用户提交评价
- getMessages(Long sessionId) - 获取会话消息列表
- listSessions(Integer status, Integer page, Integer pageSize) - 分页获取会话列表（管理端）
- countWaiting() - 获取等待中的会话数量
- getActiveSessionByUserId(Long userId) - 根据用户ID获取当前进行中的会话

### 10.5 StatisticsService
统计分析
- getOverview() - 获取统计概览数据
- getCharts(Integer days) - 获取图表数据
- getRankings(Integer limit) - 获取排行榜数据
- getRealtime(Integer limit) - 获取实时数据
- getOperationBoard(Integer days, Long storeId) - 获取经营看板数据
- getStoreDailyReport(Long storeId) - 门店营收日报

### 10.6 ImageService
图片处理
- toCdnUrl(String localUrl) - 将本地图片URL转换为CDN URL
- getThumbnailUrl(String imageUrl) - 获取缩略图URL
- getMediumUrl(String imageUrl) - 获取中等尺寸图片URL
- getLargeUrl(String imageUrl) - 获取大图URL
- getWebpUrl(String imageUrl) - 获取WebP格式图片URL
- getOptimizedThumbnailUrl(String imageUrl) - 获取优化后的缩略图URL
- getOptimizedMediumUrl(String imageUrl) - 获取优化后的中等尺寸图片URL
- batchToCdnUrl(String imageUrls) - 批量转换图片URL为CDN URL
- isCdnEnabled() - 检查CDN是否启用
