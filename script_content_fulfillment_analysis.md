# 剧本与内容履约模块分析

## 1️⃣ 剧本内容体系

### 1.1 核心实体关系
```
Script (剧本)
├── ScriptCategory (分类)
├── ScriptRole (角色)
├── ScriptSchedule (排期)
├── ScriptReview (剧本评价)
└── ScriptFavorite (收藏)
```

### 1.2 剧本基础属性
| 属性 | 说明 | 备注 |
|------|------|------|
| id | 剧本ID | 主键自增 |
| name | 剧本名称 | 支持模糊搜索 |
| categoryId | 分类ID | 用于分类筛选 |
| cover | 封面图片 | 支持多张详情图片 |
| type | 剧本类型 | 1:恐怖 2:推理 3:情感 4:欢乐 5:机制 6:还原 |
| difficulty | 难度等级 | 1:简单 2:普通 3:困难 4:硬核 |
| playerCount | 游戏人数 | 固定人数设定 |
| duration | 游戏时长 | BigDecimal 小时数 |
| price | 价格 | 每人单价 |
| rating | 评分 | 基于ScriptReview自动计算平均分 |
| status | 状态 | 1:上架 0:下架 |
| isExclusive | 独家标记 | 1:是 0:否 |

### 1.3 剧本角色系统
**ScriptRole 实体字段：**
- scriptId | 关联剧本
- roleName | 角色名称（支持多个角色）
- avatar | 角色头像
- characterImage | 角色立绘图片
- gender | 性别要求 (1:男 2:女 3:不限)
- ageRange | 年龄范围建议
- description | 角色描述/剧情背景
- difficulty | 角色难度 (1:简单 2:普通 3:困难)
- sort | 排序字段 (按顺序展示角色)

**特点：**
- 按sort字段排序展示
- 支持多角色批量新增
- 角色与性别/年龄要求关联

### 1.4 分类体系
**ScriptCategory 特点：**
- 用于剧本归类
- 支持高级筛选功能
- 与GroupOrder的categoryId关联（便于拼单分类筛选）

### 1.5 搜索与筛选
**支持的搜索维度：**
- 关键词搜索 (keyword/name)
- 分类筛选 (categoryId)
- 难度筛选 (difficulty: 简单/普通/困难/硬核)
- 人数筛选 (playerCount: 8人及以上合并处理)
- 排序方式 (sortBy):
  - "hot" | 按热度排序 (rating降序 + createTime降序)
  - "rating" | 按评分排序 (rating降序 + createTime降序)
  - "newest" | 按最新排序 (createTime降序)
  - "price" | 按价格排序

---

## 2️⃣ 排期与DM/门店关系

### 2.1 ScriptSchedule 排期核心字段
| 字段 | 类型 | 说明 |
|------|------|------|
| storeId | Long | 门店ID - 排期属于哪个门店 |
| scriptId | Long | 剧本ID - 该排期对应的剧本 |
| roomId | Long | 房间ID - 在哪个房间进行 |
| scheduleDate | LocalDate | 排期日期 (yyyy-MM-dd) |
| startTime | LocalTime | 开始时间 (HH:mm:ss) |
| endTime | LocalTime | 结束时间 (HH:mm:ss) |
| maxPlayers | Integer | 最大预约人数 |
| currentPlayers | Integer | 当前已预约人数 |
| status | Integer | 1:可预约 0:已满 2:已关闭 |
| dmId | Long | **DM主持人ID** (可为空-暂未分配) |

### 2.2 DM与排期的关系
**DM角色定位：**
- DM (Dungeon Master) = 主持人/游戏机制师
- 一个DM可管理多场排期
- 一个排期可能暂时没有分配DM

**DM信息填充流程：**
```java
// ScriptScheduleServiceImpl.fillDmInfo()
1. 提取所有dmId (不为空、去重)
2. 批量查询Dm实体 (避免N+1查询)
3. 填充dmName + dmAvatar 到排期对象
```

**DM权限边界：**
- DM可查询 `/api/script/schedule/mine` (我的排期)
  - 通过 `applyDmScheduleScope(list, true)` 过滤
  - 只展示当前DM负责的排期

### 2.3 排期与门店/房间关系
**关联信息填充：**
```
ScriptSchedule
├── storeId → Store (门店名称、地址)
├── roomId → StoreRoom (房间名称)
├── scriptId → Script (剧本名称、价格、评分、难度、时长、人数)
└── dmId → Dm (DM名称、头像)
```

**批量填充优化：**
- fillScheduleDisplayInfo() 方法
- 按storeId/roomId/scriptId分别批量查询
- 避免循环查询导致性能问题

### 2.4 排期冲突检测
**检测维度：**
```
同门店 + 同房间 + 同日期 + 时段重叠 = 冲突
```

**检测API：**
- `GET /api/script/schedule/check-conflict`
- 参数：storeId, roomId, scheduleDate, startTime, endTime, excludeId(编辑时排除自身)
- 返回：{conflict: true/false, conflictSchedules: [...]}

**应用场景：**
- 新增排期前端验证
- 编辑排期时排除自身ID

### 2.5 排期状态管理
| 状态值 | 含义 | 触发条件 |
|--------|------|---------|
| 1 | 可预约 | 初始状态、currentPlayers < maxPlayers |
| 0 | 已满 | currentPlayers == maxPlayers (自动转换) |
| 2 | 已关闭 | 主动关闭或过期 |

### 2.6 排期查询类型
**门店端查询：**
- `/list` - 查询门店某日期的所有排期
- `/range` - 查询门店日期范围的所有排期

**DM端查询：**
- `/mine` - 我的排期 (单日期)
- `/mine/range` - 我的排期 (日期范围)

**用户端查询：**
- `/available` - 查询可约场次 (status=1 + 未满员 + 7天内)
- 支持scriptId/storeId/days参数

---

## 3️⃣ 收藏与评价

### 3.1 收藏系统 (ScriptFavorite)
**实体字段：**
- userId | 用户ID
- scriptId | 剧本ID
- createTime | 收藏时间
- updateTime | 更新时间

**收藏功能：**
| 接口 | 说明 | 权限 |
|------|------|------|
| POST `/api/script/favorite/{scriptId}` | 收藏剧本 | 需登录 |
| DELETE `/api/script/favorite/{scriptId}` | 取消收藏 | 需登录 |
| GET `/api/script/favorite/{scriptId}/status` | 检查收藏状态 | 可选登录 |
| GET `/api/script/favorite/list` | 获取我的收藏列表 | 需登录 |
| GET `/api/script/favorite/count` | 获取收藏总数 | 需登录 |

**收藏积分奖励机制：**
```java
- 每收藏5个剧本奖励20积分
- 收藏时判断：favoriteCount % 5 == 0
- 例：收藏第5个、第10个、第15个时获得积分
- 提示用户：已收藏X个 / 再收藏Y个可获得积分
```

**缓存策略：**
- 缓存键：`script:favorite:userId_scriptId`
- 缓存键：`script:favorite:list`
- 收藏/取消时清除所有相关缓存

### 3.2 剧本评价 (ScriptReview)
**实体字段：**
| 字段 | 说明 |
|------|------|
| scriptId | 关联剧本 |
| userId | 评价用户 |
| reservationId | 关联预约 (用于溯源) |
| rating | 总体评分 (1-5星) |
| storyRating | 剧情评分 |
| difficultyRating | 难度评分 |
| content | 评价内容 |

**评价流程：**
```
ScriptReviewDTO (前端输入)
→ ScriptReviewServiceImpl.add()
→ 1. 存储评价到script_review表
  2. 通过reviewMapper.calculateAverageRating()计算平均分
  3. 更新script表的rating字段 (2位小数四舍五入)
```

**API接口：**
| 接口 | 说明 |
|------|------|
| POST `/api/script/review` | 添加剧本评价 |
| GET `/api/script/review/page` | 分页查询评价列表 |
| GET `/api/script/review/{id}` | 查询评价详情 |
| DELETE `/api/script/review/{id}` | 删除评价 |

---

## 4️⃣ 订单评价与剧本评价区别

### 4.1 两个独立评价体系

#### **Review (订单综合评价)**
**关联对象：**
- reservationId | 预约订单 (一预约一评价)
- storeId | 门店
- scriptId | 剧本
- userId | 评价用户
- dmId | 主持人DM

**评分维度：**
| 评分字段 | 含义 | 权重 |
|---------|------|------|
| overallRating | 综合评分 | 根据下述计算 |
| storeRating | 门店评分 | 1-5星 |
| scriptRating | 剧本评分 | 1-5星 |
| serviceRating | 服务评分 | 1-5星 |
| dmRating | DM评分 | 1-5星 (可选) |

**综合评分计算逻辑：**
```java
// ReviewServiceImpl.create()
if (review.getOverallRating() == null) {
    int ratingSum = 0;
    int ratingCount = 0;
    if (storeRating != null) { ratingSum += storeRating; ratingCount++; }
    if (scriptRating != null) { ratingSum += scriptRating; ratingCount++; }
    if (serviceRating != null) { ratingSum += serviceRating; ratingCount++; }
    overallRating = ratingCount > 0 ? 
        Math.round((float) ratingSum / ratingCount) : 5;
}
```

**附加属性：**
- content | 评价内容
- images | 评价图片 (逗号分隔)
- tags | 评价标签 (逗号分隔)
- isAnonymous | 是否匿名 (1/0)
- isFeatured | 是否精选 (1/0)

#### **ScriptReview (剧本单维评价)**
**关联对象：**
- scriptId | 剧本
- userId | 评价用户
- reservationId | 关联预约 (用于溯源，非强制)

**评分维度：**
| 评分字段 | 含义 |
|---------|------|
| rating | 总体评分 (1-5星) |
| storyRating | 剧情评分 |
| difficultyRating | 难度评分 |

**作用：**
- 单独评价剧本质量
- 自动计算并更新script表的rating字段
- 用户可能在不评价门店/服务的情况下单独评价剧本

### 4.2 两个体系的关联性
```
一次预约体验(Reservation)
│
├─ Review (综合评价) ← 一预约一评价
│  ├─ storeRating (门店服务评价)
│  ├─ scriptRating (剧本体验评价)
│  ├─ serviceRating (服务态度评价)
│  └─ dmRating (DM主持评价)
│
└─ ScriptReview (剧本专项评价) ← 可选/独立
   ├─ rating (总体评分)
   ├─ storyRating (剧情评分)
   └─ difficultyRating (难度评分)
```

### 4.3 积分奖励机制对比

#### **Review 的积分奖励：**
```java
int rewardPoints = 50; // 基础积分

// 上传图片：+10积分
if (images != null && !images.isEmpty()) {
    rewardPoints += 10;
}

// 内容详细 (>50字)：+10积分
if (content != null && content.length() > 50) {
    rewardPoints += 10;
}

// 三项都评分：+5积分
if (storeRating != null && scriptRating != null && serviceRating != null) {
    rewardPoints += 5;
}

// 总计最高 65积分
```

#### **ScriptReview 的积分奖励：**
- 暂无显式积分奖励机制
- 可能与ScriptFavorite共享积分体系

#### **ScriptFavorite 的积分奖励：**
```
每收藏5个剧本 → 20积分
```

### 4.4 审核与发布流程

**Review 审核流程：**
```
1. 创建时默认状态 = 2 (已通过，直接显示)
2. 支持后续审核接口 /audit
   - status: 1待审核 2已通过 3已拒绝
   - auditReason: 审核原因
   - auditUserId: 审核人ID
   - auditTime: 审核时间
3. 支持商家回复
   - replyContent: 回复内容
   - replyTime: 回复时间
```

**ScriptReview 审核流程：**
- 暂无审核机制 (直接存储)
- 无后续状态管理

### 4.5 评价展示与统计

**ReviewController 提供统计接口：**
```
GET /api/reservation/review/statistics
参数：storeId (可选), scriptId (可选)
返回：ReviewStatisticsVO
```

**统计内容推测：**
- 各评分星级分布
- 平均评分
- 评价总数
- 各评分维度的平均值

---

## 5️⃣ 拼单机制

### 5.1 拼单核心实体
```
GroupOrder (拼单主表)
└── GroupMember[] (拼单成员表)
```

### 5.2 GroupOrder 关键字段
| 字段 | 类型 | 说明 |
|------|------|------|
| creatorId | Long | 发起人用户ID |
| creatorName | String | 发起人昵称 |
| creatorAvatar | String | 发起人头像 |
| scriptId | Long | 剧本ID |
| scriptName | String | 剧本名称 (快照) |
| storeId | Long | 门店ID |
| storeName | String | 门店名称 (快照) |
| playTime | LocalDateTime | 开车时间 |
| currentCount | Integer | **当前人数** (包括发起人) |
| needCount | Integer | **需要总人数** |
| playerCount | Integer | 剧本人数 (快照) |
| price | BigDecimal | 每人价格 (快照) |
| genderRequirement | String | 性别要求 |
| newbieWelcome | Integer | 新手友好 (0/1) |
| description | String | 拼单说明 |
| reservationId | Long | **关联预约ID** |
| status | Integer | 1:拼团中 2:已成团 3:已结束 0:已取消 |

### 5.3 GroupMember 成员管理
| 字段 | 说明 |
|------|------|
| groupId | 拼单ID |
| userId | 用户ID |
| nickname | 用户昵称 |
| avatar | 用户头像 |
| isCreator | 是否发起人 (0/1) |
| joinCount | 参与人数 (该用户带的人数) |
| status | 状态 (0:已退出 1:已加入) |

**特点：**
- 一个成员可代表多人 (joinCount > 1)
- 支持团队参与拼单

### 5.4 拼单生命周期
```
创建 → 拼团中 → 成团 → 结束
  ↓
  └─→ 24小时未满 → 过期/取消
```

**状态转换流程：**
```java
// 创建时
status = 1 (拼团中)
currentCount = 1 (发起人)

// 加人过程中
currentCount += joinCount

// 成团条件
if (currentCount >= needCount) {
    status = 2 (已成团)
    // 触发 checkAndUpdateGroupStatus()
}

// 过期条件
if (status == 1 && createTime < now - 24小时) {
    status = 0 (已取消)
    // 或状态为 (已过期)
}
```

**成团提前期限：**
```java
private static final int GROUP_FORMATION_LEAD_HOURS = 2;
// playTime 需要距离现在至少2小时
```

### 5.5 拼单操作API
| 接口 | 说明 | 权限 |
|------|------|------|
| GET `/api/group/page` | 分页查询拼单 | 可选登录 |
| GET `/api/group/hot` | 获取热门拼单 | 可选登录 |
| GET `/api/group/{id}` | 获取拼单详情 | 可选登录 |
| POST `/api/group` | 创建拼单 | 需登录 |
| POST `/api/group/{id}/join` | 加入拼单 | 需登录 |
| POST `/api/group/{id}/leave` | 退出拼单 | 需登录 |
| POST `/api/group/{id}/cancel` | 取消拼单 | 需登录+发起人 |
| GET `/api/group/my` | 我的拼单 | 需登录 |

### 5.6 拼单筛选维度
| 参数 | 说明 | 备注 |
|------|------|------|
| scriptId | 剧本ID | 指定剧本的拼单 |
| categoryId | 分类ID | 按分类筛选 |
| playerCount | 人数 | 按人数筛选 |
| status | 状态 | 1:拼团中 2:已成团 其他:已结束 |

**拼团中(status=1)的动态条件：**
```java
// 只显示拼团中且未过期的拼单
status == 1 && 
createTime > now - 24小时 &&
(playTime == null || playTime > now + 2小时)
```

### 5.7 匿名性设计
**用户端数据脱敏：**
```java
// GroupOrderServiceImpl.getDetailWithMembers()
1. 发起人信息隐藏：creatorName = "神秘车主", creatorAvatar = null
2. 成员匿名展示：
   - 使用预定义角色名：["神秘侦探", "暗夜行者", ...]
   - 显示格式：🎭 角色名 (发起人) / 🎭 角色名
   - 保留joinCount和isCreator标识
```

**特殊处理：**
- 成员数据返回 `anonymousMembers` 数组
- 仅保留：id, isCreator, joinCount, createTime, anonymousName, avatar(null), nickname

### 5.8 自动拼单机制
**函数：**
```java
createOrAttachAutoGroup(GroupOrder, Reservation, userId)
```

**流程：**
1. 检查是否存在可复用的拼单 (findReusableAutoGroup)
2. 如果存在：附加预约到现有拼单 (attachReservationToExistingGroup)
3. 如果不存在：创建新拼单 (createGroup)

**自动拼单描述生成：**
```java
buildAutoGroupDescription(currentCount, needCount)
// 格式：已有X人，需要Y人
```

### 5.9 与预约的关联
**关联字段：**
- reservationId | GroupOrder.reservationId

**绑定流程：**
```java
bindCreatorReservation(GroupOrder)
// 发起人的预约与拼单关联
// 自动拼单时也绑定预约
```

**用处：**
- 拼单成团时生成预约
- 评价预约时追溯所属拼单
- 预约完成时更新拼单状态

### 5.10 热度排序
```
ORDER BY createTime DESC
// 最新创建的拼单优先显示
```

---

## 📊 模块交互关系图

```
用户端
  ├─ 剧本浏览
  │  └─ Script (分页查询/热门/推荐)
  │     ├─ ScriptCategory (分类筛选)
  │     ├─ ScriptRole (角色展示)
  │     └─ ScriptSchedule (可约场次)
  │
  ├─ 收藏与评价
  │  ├─ ScriptFavorite (收藏机制) → 积分奖励
  │  ├─ ScriptReview (剧本评价) → 更新Script.rating
  │  └─ Review (订单综合评价) → 多维评分+积分奖励
  │
  └─ 拼单
     └─ GroupOrder
        ├─ GroupMember[] (成员管理)
        ├─ Script (快照存储)
        ├─ Store (快照存储)
        └─ Reservation (可选关联)

DM端
  └─ ScriptSchedule.mine (我的排期)
     ├─ 按日期范围查询
     └─ 权限验证通过dmId

门店端
  └─ ScriptSchedule (门店排期管理)
     ├─ 排期生成/编辑/删除
     ├─ 冲突检测
     └─ 成员/房间管理
```

---

## 🔑 关键设计要点

### 1. **快照存储**
- GroupOrder存储scriptName, scriptId, price, playerCount
- 保证拼单历史数据的准确性（剧本价格改动不影响已拼单）

### 2. **级联删除与逻辑删除**
- Script/ScriptRole/ScriptFavorite/ScriptReview 使用逻辑删除
- 保留历史数据可追溯性

### 3. **积分奖励多元化**
- 收藏积分：每5个20积分
- 评价积分：50-65积分可变
- 不同行为鼓励用户参与

### 4. **缓存策略**
- ScriptFavorite使用缓存：`script:favorite:userId_scriptId`
- 避免频繁查询数据库

### 5. **权限隔离**
- DM只能查看自己的排期
- 用户只能删除自己的评价
- 拼单支持匿名参与

### 6. **批量查询优化**
- fillScheduleDisplayInfo() 避免N+1查询
- 按ID分组批量查询相关实体

### 7. **状态机管理**
- ScriptSchedule: 可预约/已满/已关闭
- GroupOrder: 拼团中/已成团/已结束/已取消
- Review: 待审核/已通过/已拒绝
