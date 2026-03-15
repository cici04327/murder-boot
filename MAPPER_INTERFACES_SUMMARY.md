# Java Spring Boot 项目 Mapper 接口完整列表

## 概述
项目共有 **40个 Mapper 接口**，全部继承自 MyBatis-Plus 的 `BaseMapper<T>`，提供基础的CRUD操作。部分Mapper增强了自定义的SQL查询方法。

---

## 1. 用户相关 Mapper

### 1.1 UserMapper
用户数据访问层
```java
public interface UserMapper extends BaseMapper<User>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 1.2 UserAddressMapper
用户地址数据访问层
```java
public interface UserAddressMapper extends BaseMapper<UserAddress>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 1.3 UserBrowseHistoryMapper
用户浏览历史数据访问层
```java
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory>
```
**自定义方法：**
- `getRecentBrowseScriptIds(Long userId, Integer limit)` - 获取用户最近浏览的剧本ID列表
- `getUserBrowseCategoryStats(Long userId)` - 获取用户浏览过的剧本分类统计
- `getUserBrowseTypeStats(Long userId)` - 获取用户浏览过的剧本类型统计
- `cleanExpiredRecords(LocalDateTime expireTime)` - 清理过期的浏览记录（超过90天）

### 1.4 UserCouponMapper
用户优惠券数据访问层
```java
public interface UserCouponMapper extends BaseMapper<UserCoupon>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 1.5 UserNotificationMapper
用户通知记录数据访问层
```java
public interface UserNotificationMapper extends BaseMapper<UserNotification>
```
**自定义方法：**
- `countUnreadByUserId(Long userId)` - 查询用户未读通知数量

### 1.6 UserPointsRecordMapper
用户积分记录数据访问层
```java
public interface UserPointsRecordMapper extends BaseMapper<UserPointsRecord>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 1.7 UserPreferenceMapper
用户偏好数据访问层
```java
public interface UserPreferenceMapper extends BaseMapper<UserPreference>
```
**自定义方法：**
- `getUserTopPreferences(Long userId, Integer limit)` - 获取用户的偏好列表（按分数排序）
- `getUserPreferencesByType(Long userId, String typePrefix)` - 获取用户特定类型的偏好
- `incrementPreference(Long userId, String preferenceType, String preferenceValue, Double score)` - 增加偏好计数和分数（INSERT ON DUPLICATE KEY UPDATE）

### 1.8 UserSettingsMapper
用户设置数据访问层
```java
public interface UserSettingsMapper extends BaseMapper<UserSettings>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 1.9 UserVipMapper
用户VIP数据访问层
```java
public interface UserVipMapper extends BaseMapper<UserVip>
```
**自定义方法：**
- `getCurrentVip(Long userId)` - 获取用户当前有效的VIP记录

---

## 2. 剧本相关 Mapper

### 2.1 ScriptMapper
剧本数据访问层
```java
public interface ScriptMapper extends BaseMapper<Script>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 2.2 ScriptCategoryMapper
剧本分类数据访问层
```java
public interface ScriptCategoryMapper extends BaseMapper<ScriptCategory>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 2.3 ScriptRoleMapper
剧本角色数据访问层
```java
public interface ScriptRoleMapper extends BaseMapper<ScriptRole>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 2.4 ScriptScheduleMapper
剧本排期数据访问层
```java
public interface ScriptScheduleMapper extends BaseMapper<ScriptSchedule>
```
**自定义方法：**
- `listByStoreAndDate(Long storeId, LocalDate scheduleDate)` - 查询门店某日期的排期列表（带剧本名和房间名）
- `listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate)` - 查询门店日期范围内的排期

### 2.5 ScriptTagMapper
剧本标签数据访问层
```java
public interface ScriptTagMapper extends BaseMapper<ScriptTag>
```
**自定义方法：**
- `batchGetScriptTagsRaw(List<Long> scriptIds)` - 批量获取多个剧本的标签（返回原始数据）
- `batchGetScriptTags(List<Long> scriptIds)` - 批量获取多个剧本的标签（返回 scriptId -> tagNames 的 Map）
- `getScriptTags(Long scriptId)` - 获取剧本的所有标签
- `getSimilarScriptIdsByTags(List<String> tags, Long excludeScriptId, Integer limit)` - 根据标签获取相似剧本ID列表
- `batchInsert(List<ScriptTag> tags)` - 批量插入标签

### 2.6 ScriptReviewMapper
剧本评价数据访问层
```java
public interface ScriptReviewMapper extends BaseMapper<ScriptReview>
```
**自定义方法：**
- `calculateAverageRating(Long scriptId)` - 计算剧本平均评分
- `countByScriptId(Long scriptId)` - 统计剧本评价数量

### 2.7 ScriptFavoriteMapper
剧本收藏数据访问层
```java
public interface ScriptFavoriteMapper extends BaseMapper<ScriptFavorite>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 3. 门店相关 Mapper

### 3.1 StoreMapper
门店数据访问层
```java
public interface StoreMapper extends BaseMapper<Store>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 3.2 StoreRoomMapper
门店房间数据访问层
```java
public interface StoreRoomMapper extends BaseMapper<StoreRoom>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 3.3 StoreEmployeeMapper
门店员工数据访问层
```java
public interface StoreEmployeeMapper extends BaseMapper<StoreEmployee>
```
**自定义方法：**
- `countByStoreId(Long storeId)` - 统计门店员工数量
- `countActiveByStoreId(Long storeId)` - 统计在职员工数量

### 3.4 StoreReviewMapper
门店评价数据访问层
```java
public interface StoreReviewMapper extends BaseMapper<StoreReview>
```
**自定义方法：**
- `countByStoreId(Long storeId)` - 统计门店评价数量
- `getAverageRating(Long storeId)` - 计算门店平均评分
- `countGoodReviews()` - 统计好评数量（评分>=4为好评）
- `countGoodReviewsByStoreId(Long storeId)` - 统计指定门店的好评数量

### 3.5 DmMapper
DM（主持人）数据访问层
```java
public interface DmMapper extends BaseMapper<Dm>
```
**自定义方法：**
- `updateRatingAndGameCount(Long dmId, BigDecimal rating, Integer gameCount)` - 更新DM评分和场次

---

## 4. 预约相关 Mapper

### 4.1 ReservationMapper
预约订单数据访问层
```java
public interface ReservationMapper extends BaseMapper<Reservation>
```
**自定义方法：**
- `selectUpcomingReservations(LocalDateTime startTime, LocalDateTime endTime)` - 查询即将开始的预约（指定时间范围内）
- `countConflictingReservations(Long roomId, LocalDateTime startTime, LocalDateTime endTime)` - 检查房间在指定时间段内是否有冲突的预约

### 4.2 GroupOrderMapper
拼单数据访问层
```java
public interface GroupOrderMapper extends BaseMapper<GroupOrder>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 4.3 GroupMemberMapper
拼单成员数据访问层
```java
public interface GroupMemberMapper extends BaseMapper<GroupMember>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 5. 评价相关 Mapper

### 5.1 ReviewMapper
综合评价数据访问层
```java
public interface ReviewMapper extends BaseMapper<Review>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 6. 文章相关 Mapper

### 6.1 ArticleMapper
文章数据访问层
```java
public interface ArticleMapper extends BaseMapper<Article>
```
**自定义方法：**
- `increaseViewCount(Long id)` - 增加浏览次数
- `increaseLikeCount(Long id)` - 增加点赞次数
- `decreaseLikeCount(Long id)` - 减少点赞次数
- `increaseCommentCount(Long id)` - 增加评论数
- `decreaseCommentCount(Long id)` - 减少评论数
- `increaseFavoriteCount(Long id)` - 增加收藏数
- `decreaseFavoriteCount(Long id)` - 减少收藏数

### 6.2 ArticleCommentMapper
文章评论数据访问层
```java
public interface ArticleCommentMapper extends BaseMapper<ArticleComment>
```
**自定义方法：**
- `increaseLikeCount(Long id)` - 增加评论点赞数
- `decreaseLikeCount(Long id)` - 减少评论点赞数

### 6.3 ArticleCommentLikeMapper
评论点赞数据访问层
```java
public interface ArticleCommentLikeMapper extends BaseMapper<ArticleCommentLike>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 6.4 ArticleLikeMapper
文章点赞数据访问层
```java
public interface ArticleLikeMapper extends BaseMapper<ArticleLike>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 6.5 ArticleFavoriteMapper
文章收藏数据访问层
```java
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 7. 优惠券相关 Mapper

### 7.1 CouponMapper
优惠券数据访问层
```java
public interface CouponMapper extends BaseMapper<Coupon>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 8. VIP相关 Mapper

### 8.1 VipPackageMapper
VIP套餐数据访问层
```java
public interface VipPackageMapper extends BaseMapper<VipPackage>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 9. 通知相关 Mapper

### 9.1 AdminNotificationMapper
管理端通知数据访问层
```java
public interface AdminNotificationMapper extends BaseMapper<AdminNotification>
```
**自定义方法：**
- `countUnread()` - 统计未读通知数量
- `countUnreadByStoreId(Long storeId)` - 统计指定门店未读通知数量

### 9.2 SystemNotificationMapper
系统通知数据访问层
```java
public interface SystemNotificationMapper extends BaseMapper<SystemNotification>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

---

## 10. 其他 Mapper

### 10.1 HotRankingMapper
热门榜单数据访问层
```java
public interface HotRankingMapper extends BaseMapper<HotRanking>
```
**自定义方法：**
- `getHotRankingList(Integer rankingType, Integer limit)` - 获取指定类型的热门榜单
- `clearRankingByType(Integer rankingType)` - 清空指定类型的榜单

### 10.2 RecommendationLogMapper
推荐记录数据访问层
```java
public interface RecommendationLogMapper extends BaseMapper<RecommendationLog>
```
**自定义方法：**
- `updateClickStatus(Long userId, Long scriptId)` - 更新点击状态
- `updateReserveStatus(Long userId, Long scriptId)` - 更新预约状态
- `getRecommendationStats(String startTime)` - 获取推荐转化率统计
- `getRecentUnclickedScriptIds(Long userId, Integer limit)` - 获取用户最近未点击的推荐剧本ID

### 10.3 FeedbackMapper
用户留言反馈数据访问层
```java
public interface FeedbackMapper extends BaseMapper<Feedback>
```
- 继承BaseMapper的所有CRUD方法
- 无额外自定义方法

### 10.4 ServiceSessionMapper
客服会话数据访问层
```java
public interface ServiceSessionMapper extends BaseMapper<ServiceSession>
```
**自定义方法：**
- `countWaiting()` - 统计等待接入的会话数
- `pageWithUserInfo(Page<ServiceSession> page, Integer status)` - 分页查询会话列表（带用户信息）

### 10.5 ServiceMessageMapper
客服消息数据访问层
```java
public interface ServiceMessageMapper extends BaseMapper<ServiceMessage>
```
**自定义方法：**
- `listBySessionId(Long sessionId)` - 获取会话的所有消息（按时间排序）
- `markAsRead(Long sessionId, String senderType)` - 将指定会话的未读消息标为已读

---

## BaseMapper 继承的标准CRUD方法

所有Mapper都继承自 `BaseMapper<T>`，提供以下标准方法：

**查询方法：**
- `selectById(Serializable id)` - 根据ID查询
- `selectByMap(Map<String, Object> columnMap)` - 根据条件Map查询
- `selectOne(Wrapper<T> queryWrapper)` - 查询一条记录
- `selectCount(Wrapper<T> queryWrapper)` - 统计记录数
- `selectList(Wrapper<T> queryWrapper)` - 查询列表
- `selectPage(Page<T> page, Wrapper<T> queryWrapper)` - 分页查询

**新增方法：**
- `insert(T entity)` - 新增单条
- `insertBatch(Collection<T> entityList)` - 批量新增（通常返回插入行数）

**更新方法：**
- `updateById(T entity)` - 根据ID更新
- `update(T entity, Wrapper<T> updateWrapper)` - 条件更新

**删除方法：**
- `deleteById(Serializable id)` - 根据ID删除
- `deleteByMap(Map<String, Object> columnMap)` - 根据条件Map删除
- `delete(Wrapper<T> queryWrapper)` - 条件删除
- `deleteBatchIds(Collection<? extends Serializable> idList)` - 批量删除

---

## 总结统计

| 类别 | Mapper数量 | 有自定义方法的Mapper | 无自定义方法的Mapper |
|------|----------|-------------------|------------------|
| 用户相关 | 9 | 4 | 5 |
| 剧本相关 | 7 | 4 | 3 |
| 门店相关 | 5 | 2 | 3 |
| 预约相关 | 3 | 1 | 2 |
| 评价相关 | 1 | 0 | 1 |
| 文章相关 | 5 | 3 | 2 |
| 优惠券相关 | 1 | 0 | 1 |
| VIP相关 | 1 | 0 | 1 |
| 通知相关 | 2 | 1 | 1 |
| 其他 | 5 | 4 | 1 |
| **总计** | **40** | **19** | **21** |
