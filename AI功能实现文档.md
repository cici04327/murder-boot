# 🤖 AI功能实现文档

> 项目：剧本杀预约与门店管理系统  
> 更新时间：2026-03-30

---

## 一、AI功能总览

| 功能模块 | 入口 | 技术方案 | 是否依赖AI Key |
|---------|------|---------|--------------|
| AI智能客服 | 用户端右下角浮窗 | DeepSeek + 知识库RAG | ✅ 是 |
| AI智能分配DM | 管理端→剧本管理→AI智能分配DM | 规则引擎 + DeepSeek总结 | 可选 |
| AI增强推荐 | 用户端→推荐页→AI推荐Tab | 规则引擎 + DeepSeek重排序 | 可选 |
| AI用户画像 | 用户端→推荐页→AI推荐Tab顶部卡片 | 行为分析 + DeepSeek生成描述 | 可选 |

> **说明**：标注"可选"的功能在没有配置AI Key时会自动降级为规则引擎结果，不影响正常使用。

---

## 二、AI客服

### 2.1 功能描述

用户在平台任意页面可通过右下角浮动按钮打开AI客服对话框，支持：
- 预约咨询、退款政策、VIP权益等问题解答
- 剧本推荐
- 门店信息查询
- 转人工客服触发词检测

### 2.2 技术实现

```
用户发送消息
    ↓
前端 AICustomerService.vue / AICustomerServiceEnhanced.vue
    ↓
POST /api/ai/chat { message, sessionId, history }
    ↓
AIController → AIService
    ↓
① 知识库检索（KnowledgeBase表，按category分类）
    ↓
② 构建 System Prompt（包含平台信息、用户上下文、知识库内容）
    ↓
③ 调用 DeepSeek API（deepseek-chat模型）
    ↓
④ 返回回复 + 建议问题 + 快捷操作按钮
```

### 2.3 核心代码位置

| 文件 | 说明 |
|------|------|
| `src/main/java/com/murder/service/impl/AIServiceImpl.java` | 核心实现，包含DeepSeek调用、知识库检索、System Prompt构建 |
| `src/main/java/com/murder/controller/AIController.java` | REST接口 `/api/ai/**` |
| `src/main/java/com/murder/entity/KnowledgeBase.java` | 知识库实体 |
| `frontend/user/src/components/AICustomerService.vue` | 用户端客服组件 |
| `frontend/user/src/components/AICustomerServiceEnhanced.vue` | 增强版客服组件 |

### 2.4 知识库分类（9大类）

| category | 内容 |
|---------|------|
| reservation | 预约流程、状态、核销、改期 |
| refund | 退款规则与时限 |
| vip | 等级权益、购买续费 |
| points | 积分获取、兑换规则 |
| coupon | 优惠券使用说明 |
| payment | 支付方式与流程 |
| group | 拼单功能说明 |
| dm | 主持人相关说明 |
| store | 门店房间信息 |

### 2.5 System Prompt 设计

AI被设定为"小剧"——专业剧本杀客服助手，具备：
- 平台基本信息（VIP等级、积分规则、预约流程）
- 用户个人上下文（当前VIP等级、积分余额、优惠券数量）
- 知识库检索结果（相关FAQ）
- 转人工触发词检测（"人工"、"投诉"等关键词）

---

## 三、AI智能分配DM

### 3.1 功能描述

管理端门店管理员/总部管理员可通过「剧本管理 → AI智能分配DM」页面，为指定日期范围内的排期自动推荐最合适的DM主持人。

**核心能力：**
- 时间冲突检测（同一DM不能同时主持两场）
- 风格标签匹配（DM的styleTags与剧本tags的Jaccard相似度）
- 负载均衡（优先推荐本期场次少的DM）
- 评分加权（高评分DM优先）
- DeepSeek生成整体分析报告

### 3.2 技术实现

```
管理员点击「AI智能推荐」
    ↓
POST /api/ai/schedule/dm/suggest { storeId, startDate, endDate, onlyUnassigned }
    ↓
AiScheduleController → AiScheduleService
    ↓
① 查询日期范围内排期（可筛选仅未分配的）
② 查询门店所有在职DM（status=1）
③ 统计各DM本期已排场次（用于负载均衡）
    ↓
④ 规则打分（满分100分）：
   - 基础分：50分
   - 风格标签匹配：最高+30分（每匹配一个标签+10分）
   - DM评分加成：rating/5 × 15分（最高+15分）
   - 负载均衡：已排场次越少得分越高（最高+10分，每场-2分）
   - 时间冲突：直接排除
    ↓
⑤ 生成推荐结果（TOP3候选DM + 匹配理由）
    ↓
⑥ 调用DeepSeek生成整体分配总结报告
    ↓
返回 AiDmSuggestVO（含每场推荐DM、匹配分、推荐理由）
    ↓
管理员可逐条确认或一键接受全部
    ↓
POST /api/ai/schedule/dm/confirm → 批量更新script_schedule表的dm_id字段
```

### 3.3 核心代码位置

| 文件 | 说明 |
|------|------|
| `src/main/java/com/murder/service/impl/AiScheduleServiceImpl.java` | 规则引擎 + AI调用核心实现 |
| `src/main/java/com/murder/controller/AiScheduleController.java` | REST接口 `/api/ai/schedule/**` |
| `src/main/java/com/murder/entity/AiScheduleTask.java` | 任务记录实体 |
| `src/main/java/com/murder/vo/AiDmSuggestVO.java` | 返回结果VO |
| `src/main/java/com/murder/task/ScheduleAutoTask.java` | 定时任务（每周一凌晨3点自动分配） |
| `frontend/admin/src/views/script/ai-schedule.vue` | 管理端分配页面 |
| `frontend/admin/src/api/ai-schedule.js` | 前端API封装（60s超时） |

### 3.4 定时任务

```java
// 每周一凌晨3点自动为下周排期分配DM
@Scheduled(cron = "0 0 3 * * MON")
public void autoAssignDmForNextWeek()
```

### 3.5 降级策略

- **有AI Key**：调用DeepSeek生成中文分析报告（推荐质量、负载情况、优化建议）
- **无AI Key**：返回默认文字总结，规则引擎结果不受影响

---

## 四、AI增强推荐

### 4.1 功能描述

用户端推荐页（`/recommend`）的默认「🤖 AI推荐」Tab，在原有规则引擎推荐基础上增加：

1. **AI重排序**：把规则引擎的推荐候选集（最多20个）交给DeepSeek，根据用户偏好重新排序，返回最优的N个
2. **AI推荐理由**：为每个推荐剧本生成一句15字以内的个性化推荐理由
3. **降级保障**：AI调用失败时自动使用规则生成的推荐理由

### 4.2 技术实现

```
用户访问推荐页
    ↓
GET /api/recommendation/ai-enhanced?limit=12
    ↓
RecommendationController → RecommendationService
    ↓
① 调用原有规则引擎获取候选集（limit×2，最多20个）
   - 40%历史偏好推荐
   - 30%热门推荐
   - 30%新品推荐
    ↓
② 构建用户偏好描述（从UserPreference表提取tag/category偏好）
    ↓
③ 调用DeepSeek重排序：
   - 输入：候选剧本列表 + 用户偏好描述
   - 输出：JSON数组 [{ rank, index, reason }]
   - 解析后按rank重新排序
    ↓
④ 每个推荐结果附上aiReason字段（个性化推荐理由）
    ↓
返回 List<RecommendationVO>（含aiReason、aiRank字段）
```

### 4.3 核心Prompt设计

```
你是剧本杀推荐助手。根据用户偏好从候选剧本选出最适合的N个重新排序，
为每个生成15字以内的个性化推荐理由。
用户偏好：{偏好标签、偏好分类、互动次数}
候选剧本：{编号、名称、分类、评分、标签}
只返回JSON数组：[{"rank":1,"index":1,"reason":"推荐理由"}...]
```

### 4.4 降级推荐理由规则

| 推荐类型 | 降级理由 |
|---------|---------|
| 协同过滤(1) | 和你口味相似的玩家都爱这个 |
| 内容推荐(2) | 与你喜欢的剧本风格相近 |
| 热门推荐(3) | 近期最多玩家选择 |
| 历史推荐(4) | 根据你的游玩历史推荐 |
| 高分剧本 | 口碑极佳，强烈推荐 |
| 有标签 | 符合你的{标签}偏好 |

### 4.5 核心代码位置

| 文件 | 说明 |
|------|------|
| `src/main/java/com/murder/service/impl/RecommendationServiceImpl.java` | AI重排序核心实现（aiReRankAndAddReasons方法） |
| `src/main/java/com/murder/controller/RecommendationController.java` | 新增 `/recommendation/ai-enhanced` 接口 |
| `src/main/java/com/murder/vo/RecommendationVO.java` | 新增aiReason、aiRank字段 |
| `frontend/user/src/views/recommend/enhanced.vue` | AI推荐Tab UI（含用户画像卡片） |
| `frontend/user/src/api/recommendation.js` | 新增getAiEnhancedRecommendations()（60s超时） |

---

## 五、AI用户画像

### 5.1 功能描述

在AI推荐Tab顶部展示用户的「玩家画像」卡片，包含：
- 偏好标签（从行为数据提取）
- AI生成的个性化画像描述（2-3句口语化描述）

### 5.2 技术实现

```
页面加载时并行调用
    ↓
GET /api/recommendation/ai-profile
    ↓
① 从UserPreference表查询用户偏好数据
   - preferenceType = "tag_*" → 提取偏好标签
   - preferenceType = "category_*" → 提取偏好分类
   - count字段 → 统计互动总次数
    ↓
② 构建行为描述文本
    ↓
③ 调用DeepSeek生成玩家画像描述
   Prompt: "根据以下数据，用2-3句话生成有趣的玩家画像，要口语化有个性"
    ↓
返回 Map { userId, preferenceTags, behaviorSummary, aiProfile, generatedAt }
```

### 5.3 UserPreference 数据结构

```
preferenceType = "tag_推理"      preferenceValue = "推理"   count=5  score=2.5
preferenceType = "category_恐怖"  preferenceValue = "恐怖"   count=3  score=1.5
preferenceType = "difficulty_3"  preferenceValue = "3"      count=2  score=1.0
```

### 5.4 核心代码位置

| 文件 | 说明 |
|------|------|
| `src/main/java/com/murder/service/impl/RecommendationServiceImpl.java` | getAiUserProfile()、buildUserPreferenceDesc()、callAiForUserProfile() |
| `src/main/java/com/murder/controller/RecommendationController.java` | `/recommendation/ai-profile` 接口 |
| `frontend/user/src/views/recommend/enhanced.vue` | 画像卡片UI（深色渐变背景 + 呼吸动效） |

---

## 六、AI配置

### 6.1 配置文件

```yaml
# application.yml
ai:
  provider: deepseek
  api-key: sk-xxxxxxxxxxxxxxxx      # DeepSeek API Key
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat              # 使用的模型
  temperature: 0.7                  # 回复创意度（0-1）
  max-tokens: 1000                  # 单次最大token数
```

### 6.2 支持切换的AI服务商

| provider值 | 说明 |
|-----------|------|
| deepseek | DeepSeek（当前使用，性价比高） |
| openai | OpenAI GPT系列 |
| openrouter | OpenRouter（聚合多模型） |
| wenxin | 百度文心一言 |
| tongyi | 阿里通义千问 |
| mock | 本地模拟（测试用） |

### 6.3 DeepSeek API申请

官网：https://platform.deepseek.com/api_keys

免费额度充足，适合开发测试。

---

## 七、各功能超时配置

| 功能 | 后端超时 | 前端超时 | 说明 |
|------|---------|---------|------|
| AI客服 | RestTemplate默认 | 30s | 对话响应较快 |
| AI DM分配 | RestTemplate默认 | 60s | 需处理多个排期 |
| AI增强推荐 | RestTemplate默认 | 60s | 需重排序多个剧本 |
| AI用户画像 | RestTemplate默认 | 60s | 画像生成较慢 |

---

## 八、功能对比：有无AI Key

| 功能 | 有AI Key | 无AI Key |
|------|---------|---------|
| AI客服 | DeepSeek智能回复 | ❌ 无法使用 |
| DM分配总结 | AI生成中文分析报告 | 默认文字总结 |
| 推荐重排序 | DeepSeek重新排序 | 规则引擎原顺序 |
| 推荐理由 | 个性化AI生成理由 | 规则模板理由 |
| 用户画像 | AI生成有趣描述 | 标签拼接文字 |
