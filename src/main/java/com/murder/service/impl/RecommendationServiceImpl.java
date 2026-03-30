package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.*;
import com.murder.mapper.*;
import com.murder.service.RecommendationService;
import com.murder.vo.RecommendationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private ScriptMapper scriptMapper;

    @Autowired
    private UserBrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private RecommendationLogMapper recommendationLogMapper;

    @Autowired
    private ScriptTagMapper scriptTagMapper;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Autowired
    private HotRankingMapper hotRankingMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ScriptFavoriteMapper scriptFavoriteMapper;

    @Autowired
    private ScriptCategoryMapper scriptCategoryMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Override
    public List<RecommendationVO> getPersonalizedRecommendations(Long userId, Integer limit) {
        log.info("生成用户 {} 的个性化推荐，数量：{}", userId, limit);
        
        // 如果用户ID为空（未登录），返回热门推荐
        if (userId == null) {
            log.warn("用户未登录，返回热门推荐");
            return getHotRecommendations(1, limit);
        }
        
        List<RecommendationVO> recommendations = new ArrayList<>();
        
        // 1. 基于用户历史的推荐（占40%）
        int historyCount = (int) (limit * 0.4);
        List<RecommendationVO> historyBased = getHistoryBasedRecommendations(userId, historyCount);
        log.info("历史推荐数量: {}", historyBased.size());
        recommendations.addAll(historyBased);
        
        // 2. 热门推荐（占30%）
        int hotCount = (int) (limit * 0.3);
        List<RecommendationVO> hotBased = getHotRecommendations(1, hotCount);
        log.info("热门推荐数量: {}", hotBased.size());
        recommendations.addAll(hotBased);
        
        // 3. 新品推荐（占30%）
        int newCount = limit - recommendations.size();
        List<RecommendationVO> newBased = getNewScriptRecommendations(newCount);
        log.info("新品推荐数量: {}", newBased.size());
        recommendations.addAll(newBased);
        
        // 去重并记录推荐日志
        List<RecommendationVO> uniqueRecommendations = removeDuplicates(recommendations);
        log.info("去重后推荐数量: {}", uniqueRecommendations.size());
        
        if (userId != null) {
            saveRecommendationLogs(userId, uniqueRecommendations);
        }
        
        return uniqueRecommendations.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<RecommendationVO> getCollaborativeRecommendations(Long userId, Long scriptId, Integer limit) {
        log.info("为用户 {} 生成基于剧本 {} 的协同过滤推荐", userId, scriptId);
        
        // 获取浏览过当前剧本的其他用户
        List<Long> similarUserIds = getSimilarUsers(scriptId, 50);
        
        if (similarUserIds.isEmpty()) {
            return getContentBasedRecommendations(scriptId, limit);
        }
        
        // 获取这些用户浏览过的其他剧本，并统计频次
        Map<Long, Integer> scriptViewCounts = new HashMap<>();
        for (Long similarUserId : similarUserIds) {
            List<Long> browsedScripts = browseHistoryMapper.getRecentBrowseScriptIds(similarUserId, 20);
            for (Long browsedScriptId : browsedScripts) {
                if (!browsedScriptId.equals(scriptId)) {
                    scriptViewCounts.put(browsedScriptId, scriptViewCounts.getOrDefault(browsedScriptId, 0) + 1);
                }
            }
        }
        
        if (scriptViewCounts.isEmpty()) {
            return getContentBasedRecommendations(scriptId, limit);
        }
        
        // 计算最大浏览次数，用于归一化
        int maxViewCount = scriptViewCounts.values().stream().max(Integer::compareTo).orElse(1);
        
        // 按协同过滤分数排序并获取推荐剧本
        List<Long> recommendedScriptIds = scriptViewCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // 构建推荐结果，使用真实的协同过滤分数
        return buildCollaborativeRecommendationVOs(recommendedScriptIds, userId, scriptViewCounts, maxViewCount);
    }

    @Override
    public List<RecommendationVO> getContentBasedRecommendations(Long scriptId, Integer limit) {
        log.info("生成基于剧本 {} 的内容推荐", scriptId);
        
        // 获取当前剧本信息
        Script currentScript = scriptMapper.selectById(scriptId);
        if (currentScript == null) {
            return Collections.emptyList();
        }
        
        // 获取当前剧本的标签
        List<String> tags = scriptTagMapper.getScriptTags(scriptId);
        
        List<RecommendationVO> recommendations = new ArrayList<>();
        Set<Long> addedScriptIds = new HashSet<>();
        addedScriptIds.add(scriptId); // 排除当前剧本
        
        // 1. 基于标签的推荐（使用真实的内容相似度分数）
        if (!tags.isEmpty()) {
            List<Long> similarByTags = scriptTagMapper.getSimilarScriptIdsByTags(tags, scriptId, limit * 2);
            if (!similarByTags.isEmpty()) {
                List<Script> similarScripts = scriptMapper.selectBatchIds(similarByTags);
                List<RecommendationVO> tagBasedVOs = buildContentBasedRecommendationVOs(currentScript, similarScripts, null);
                for (RecommendationVO vo : tagBasedVOs) {
                    if (!addedScriptIds.contains(vo.getId()) && recommendations.size() < limit) {
                        vo.setRecommendReason("相似风格");
                        recommendations.add(vo);
                        addedScriptIds.add(vo.getId());
                    }
                }
            }
        }
        
        // 2. 同分类推荐（使用真实的内容相似度分数）
        if (recommendations.size() < limit && currentScript.getCategoryId() != null) {
            LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Script::getCategoryId, currentScript.getCategoryId())
                   .notIn(Script::getId, addedScriptIds)
                   .eq(Script::getStatus, 1)
                   .eq(Script::getIsDeleted, 0)
                   .orderByDesc(Script::getRating)
                   .last("LIMIT " + (limit - recommendations.size()));
            
            List<Script> similarScripts = scriptMapper.selectList(wrapper);
            List<RecommendationVO> categoryBasedVOs = buildContentBasedRecommendationVOs(currentScript, similarScripts, null);
            for (RecommendationVO vo : categoryBasedVOs) {
                if (!addedScriptIds.contains(vo.getId()) && recommendations.size() < limit) {
                    vo.setRecommendReason("同类型剧本");
                    recommendations.add(vo);
                    addedScriptIds.add(vo.getId());
                }
            }
        }
        
        // 按相似度分数降序排序
        recommendations.sort((a, b) -> b.getRecommendScore().compareTo(a.getRecommendScore()));
        
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<RecommendationVO> getHistoryBasedRecommendations(Long userId, Integer limit) {
        log.info("为用户 {} 生成基于历史的推荐", userId);
        
        // 如果用户ID为空，返回热门推荐
        if (userId == null) {
            log.warn("用户ID为空，返回热门推荐");
            return getHotRecommendations(1, limit);
        }
        
        // 获取用户偏好
        List<UserPreference> preferences = null;
        try {
            preferences = userPreferenceMapper.getUserTopPreferences(userId, 10);
        } catch (Exception e) {
            log.error("获取用户偏好失败: {}", e.getMessage());
            preferences = new ArrayList<>();
        }
        
        if (preferences == null || preferences.isEmpty()) {
            log.info("用户 {} 没有偏好数据，返回热门推荐", userId);
            return getHotRecommendations(1, limit);
        }
        
        log.info("用户 {} 有 {} 个偏好", userId, preferences.size());
        
        // 根据用户偏好查询剧本
        List<RecommendationVO> recommendations = new ArrayList<>();
        
        for (UserPreference preference : preferences) {
            if (recommendations.size() >= limit) break;
            
            try {
                LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Script::getStatus, 1)
                       .eq(Script::getIsDeleted, 0)
                       .orderByDesc(Script::getRating);
                
                // 根据偏好类型添加条件
                if (preference.getPreferenceType().startsWith("category_")) {
                    Long categoryId = Long.parseLong(preference.getPreferenceValue());
                    wrapper.eq(Script::getCategoryId, categoryId);
                } else if (preference.getPreferenceType().startsWith("type_")) {
                    Integer type = Integer.parseInt(preference.getPreferenceValue());
                    wrapper.eq(Script::getType, type);
                } else if (preference.getPreferenceType().startsWith("difficulty_")) {
                    Integer difficulty = Integer.parseInt(preference.getPreferenceValue());
                    wrapper.eq(Script::getDifficulty, difficulty);
                }
                
                wrapper.last("LIMIT " + (limit - recommendations.size()));
                List<Script> scripts = scriptMapper.selectList(wrapper);
                log.info("根据偏好类型 {} 找到 {} 个剧本", preference.getPreferenceType(), scripts.size());
                recommendations.addAll(convertToRecommendationVOs(scripts, userId, 4, "根据您的喜好"));
            } catch (Exception e) {
                log.error("处理偏好 {} 时出错: {}", preference.getPreferenceType(), e.getMessage());
            }
        }
        
        // 如果推荐数量不够，补充热门推荐
        if (recommendations.size() < limit) {
            log.info("历史推荐数量不足，补充热门推荐");
            int needCount = limit - recommendations.size();
            List<RecommendationVO> hotRecommendations = getHotRecommendations(1, needCount);
            recommendations.addAll(hotRecommendations);
        }
        
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<RecommendationVO> getHotRecommendations(Integer rankingType, Integer limit) {
        log.info("获取热门推荐，类型：{}", rankingType);
        
        List<HotRanking> rankings = hotRankingMapper.getHotRankingList(rankingType, limit);
        if (rankings.isEmpty()) {
            // 如果榜单为空，从数据库实时查询
            return getRealTimeHotRecommendations(limit);
        }
        
        // 构建榜单映射，用于计算热门分数
        Map<Long, HotRanking> rankingMap = rankings.stream()
                .collect(Collectors.toMap(HotRanking::getScriptId, r -> r, (a, b) -> a));
        
        List<Long> scriptIds = rankings.stream()
                .map(HotRanking::getScriptId)
                .collect(Collectors.toList());
        
        // 使用真实的热门分数计算
        return buildHotRecommendationVOs(scriptIds, rankingMap);
    }

    @Override
    public List<RecommendationVO> getNewScriptRecommendations(Integer limit) {
        log.info("获取新品推荐，数量：{}", limit);
        
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getStatus, 1)
               .eq(Script::getIsDeleted, 0)
               .orderByDesc(Script::getCreateTime)
               .last("LIMIT " + limit);
        
        List<Script> scripts = scriptMapper.selectList(wrapper);
        
        // 使用真实的新品分数计算
        return buildNewScriptRecommendationVOs(scripts);
    }

    @Override
    public void recordBrowseHistory(Long userId, Integer targetType, Long targetId, Integer duration) {
        // 检查是否已存在相同的浏览记录（同一用户、同一目标、5分钟内）
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        LambdaQueryWrapper<UserBrowseHistory> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(UserBrowseHistory::getUserId, userId)
                   .eq(UserBrowseHistory::getTargetType, targetType)
                   .eq(UserBrowseHistory::getTargetId, targetId)
                   .eq(UserBrowseHistory::getIsDeleted, 0)
                   .ge(UserBrowseHistory::getBrowseTime, fiveMinutesAgo);

        UserBrowseHistory existingHistory = browseHistoryMapper.selectOne(existWrapper);
        
        if (existingHistory != null) {
            // 更新浏览时间和时长
            existingHistory.setBrowseTime(LocalDateTime.now());
            if (duration != null && duration > 0) {
                existingHistory.setDuration(existingHistory.getDuration() + duration);
            }
            browseHistoryMapper.updateById(existingHistory);
        } else {
            // 创建新记录
            UserBrowseHistory history = UserBrowseHistory.builder()
                    .userId(userId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .browseTime(LocalDateTime.now())
                    .duration(duration != null ? duration : 0)
                    .build();
            
            browseHistoryMapper.insert(history);
        }
        
        // 异步更新用户偏好
        if (targetType == 1) { // 剧本
            updateUserPreference(userId, targetId, 1);
        }
    }

    @Override
    public void recordRecommendationClick(Long userId, Long scriptId) {
        recommendationLogMapper.updateClickStatus(userId, scriptId);
    }

    @Override
    public void recordRecommendationReserve(Long userId, Long scriptId) {
        recommendationLogMapper.updateReserveStatus(userId, scriptId);
    }

    @Override
    public void updateUserPreference(Long userId, Long scriptId, Integer actionType) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) return;

        // 根据行为类型计算权重分数（含时间衰减，行为发生在当前时刻）
        double score = calculateActionScore(actionType, LocalDateTime.now());

        // 更新分类偏好
        if (script.getCategoryId() != null) {
            userPreferenceMapper.incrementPreference(userId, "category_" + script.getCategoryId(),
                    String.valueOf(script.getCategoryId()), score);
        }

        // 更新类型偏好
        if (script.getType() != null) {
            userPreferenceMapper.incrementPreference(userId, "type_" + script.getType(),
                    String.valueOf(script.getType()), score);
        }

        // 更新难度偏好
        if (script.getDifficulty() != null) {
            userPreferenceMapper.incrementPreference(userId, "difficulty_" + script.getDifficulty(),
                    String.valueOf(script.getDifficulty()), score);
        }

        // 更新标签偏好（批量获取）
        List<String> tags = scriptTagMapper.getScriptTags(scriptId);
        for (String tag : tags) {
            userPreferenceMapper.incrementPreference(userId, "tag_" + tag, tag, score);
        }
    }

    @Override
    public void refreshHotRanking(Integer rankingType) {
        log.info("刷新热门榜单，类型：{}", rankingType);
        
        try {
            // 根据榜单类型确定时间范围
            LocalDateTime startTime = getStartTimeByRankingType(rankingType);
            
            // 清空旧数据
            hotRankingMapper.clearRankingByType(rankingType);
            
            // 计算热度分数并生成新榜单
            List<HotRanking> rankings = calculateHotScores(startTime, rankingType);
            
            if (!rankings.isEmpty()) {
                // 使用MyBatis-Plus的批量插入
                for (HotRanking ranking : rankings) {
                    hotRankingMapper.insert(ranking);
                }
                log.info("刷新榜单类型 {} 成功，共 {} 条数据", rankingType, rankings.size());
            } else {
                log.warn("榜单类型 {} 没有数据", rankingType);
            }
        } catch (Exception e) {
            log.error("刷新热门榜单失败，类型：{}", rankingType, e);
        }
    }

    @Override
    public Map<String, Object> getRecommendationStats(Integer days) {
        String startTime = LocalDateTime.now().minusDays(days).toString();
        List<Map<String, Object>> stats = recommendationLogMapper.getRecommendationStats(startTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("period", days + "天");
        
        return result;
    }

    // ==================== 相似度计算算法 ====================

    /**
     * 计算两个集合的 Jaccard 相似度
     * Jaccard = |A ∩ B| / |A ∪ B|
     * 返回值范围: [0, 1]
     */
    private double calculateJaccardSimilarity(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() && setB.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 计算两个向量的余弦相似度
     * Cosine = (A · B) / (||A|| × ||B||)
     * 返回值范围: [0, 1]（假设所有值非负）
     */
    private double calculateCosineSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        if (vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        Set<Long> allKeys = new HashSet<>();
        allKeys.addAll(vectorA.keySet());
        allKeys.addAll(vectorB.keySet());
        
        for (Long key : allKeys) {
            double a = vectorA.getOrDefault(key, 0.0);
            double b = vectorB.getOrDefault(key, 0.0);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    /**
     * 计算剧本与用户偏好的匹配分数
     * 返回值范围: [0, 100]
     */
    private double calculateUserPreferenceScore(Script script, List<UserPreference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return 50.0; // 无偏好数据时返回中等分数
        }
        
        double totalScore = 0.0;
        double totalWeight = 0.0;
        
        // 获取剧本的标签
        List<String> scriptTags = scriptTagMapper.getScriptTags(script.getId());
        Set<String> scriptTagSet = new HashSet<>(scriptTags);
        
        for (UserPreference preference : preferences) {
            String prefType = preference.getPreferenceType();
            String prefValue = preference.getPreferenceValue();
            double prefScore = preference.getScore() != null ? preference.getScore().doubleValue() : 1.0;
            
            double matchScore = 0.0;
            
            // 匹配分类偏好
            if (prefType.startsWith("category_") && script.getCategoryId() != null) {
                if (prefValue.equals(String.valueOf(script.getCategoryId()))) {
                    matchScore = 1.0;
                }
            }
            // 匹配类型偏好
            else if (prefType.startsWith("type_") && script.getType() != null) {
                if (prefValue.equals(String.valueOf(script.getType()))) {
                    matchScore = 1.0;
                }
            }
            // 匹配难度偏好
            else if (prefType.startsWith("difficulty_") && script.getDifficulty() != null) {
                int prefDifficulty = Integer.parseInt(prefValue);
                int diff = Math.abs(script.getDifficulty() - prefDifficulty);
                matchScore = Math.max(0, 1.0 - diff * 0.25); // 难度差距越大分数越低
            }
            // 匹配标签偏好
            else if (prefType.startsWith("tag_")) {
                if (scriptTagSet.contains(prefValue)) {
                    matchScore = 1.0;
                }
            }
            
            totalScore += matchScore * prefScore;
            totalWeight += prefScore;
        }
        
        // 归一化到 [0, 100]
        double normalizedScore = totalWeight > 0 ? (totalScore / totalWeight) * 100 : 50.0;
        return Math.min(100.0, Math.max(0.0, normalizedScore));
    }

    /**
     * 计算基于内容的相似度分数（标签 + 属性）
     * 返回值范围: [0, 100]
     */
    private double calculateContentSimilarityScore(Script sourceScript, Script targetScript) {
        double score = 0.0;
        double weightSum = 0.0;
        
        // 1. 标签相似度 (权重: 40%)
        List<String> sourceTags = scriptTagMapper.getScriptTags(sourceScript.getId());
        List<String> targetTags = scriptTagMapper.getScriptTags(targetScript.getId());
        double tagSimilarity = calculateJaccardSimilarity(new HashSet<>(sourceTags), new HashSet<>(targetTags));
        score += tagSimilarity * 40;
        weightSum += 40;
        
        // 2. 分类匹配 (权重: 25%)
        if (sourceScript.getCategoryId() != null && targetScript.getCategoryId() != null) {
            if (sourceScript.getCategoryId().equals(targetScript.getCategoryId())) {
                score += 25;
            }
        }
        weightSum += 25;
        
        // 3. 类型匹配 (权重: 15%)
        if (sourceScript.getType() != null && targetScript.getType() != null) {
            if (sourceScript.getType().equals(targetScript.getType())) {
                score += 15;
            }
        }
        weightSum += 15;
        
        // 4. 难度相近度 (权重: 10%)
        if (sourceScript.getDifficulty() != null && targetScript.getDifficulty() != null) {
            int diffDelta = Math.abs(sourceScript.getDifficulty() - targetScript.getDifficulty());
            double diffScore = Math.max(0, 1.0 - diffDelta * 0.25) * 10;
            score += diffScore;
        }
        weightSum += 10;
        
        // 5. 人数相近度 (权重: 10%)
        if (sourceScript.getPlayerCount() != null && targetScript.getPlayerCount() != null) {
            int playerDelta = Math.abs(sourceScript.getPlayerCount() - targetScript.getPlayerCount());
            double playerScore = Math.max(0, 1.0 - playerDelta * 0.1) * 10;
            score += playerScore;
        }
        weightSum += 10;
        
        return weightSum > 0 ? score : 50.0;
    }

    /**
     * 计算协同过滤推荐分数
     * 基于浏览过相同剧本的用户数量和行为权重
     * 返回值范围: [0, 100]
     */
    private double calculateCollaborativeScore(Long scriptId, Map<Long, Integer> scriptViewCounts, int maxCount) {
        int viewCount = scriptViewCounts.getOrDefault(scriptId, 0);
        if (maxCount == 0) {
            return 50.0;
        }
        // 使用对数缩放避免热门剧本分数过高
        double normalizedScore = Math.log1p(viewCount) / Math.log1p(maxCount);
        return normalizedScore * 100;
    }

    /**
     * 计算热门推荐分数
     * 综合考虑：评分、预约量、收藏量、浏览量
     * 返回值范围: [0, 100]
     */
    private double calculateHotScore(Script script, HotRanking ranking) {
        double score = 0.0;
        
        // 1. 评分贡献 (权重: 40%)
        if (script.getRating() != null) {
            double ratingScore = script.getRating().doubleValue() / 5.0 * 40;
            score += ratingScore;
        }
        
        // 2. 榜单排名贡献 (权重: 30%)
        if (ranking != null && ranking.getRank() != null) {
            // 排名越高分数越高，假设最多50名
            double rankScore = Math.max(0, (51 - ranking.getRank()) / 50.0) * 30;
            score += rankScore;
        }
        
        // 3. 热度分数贡献 (权重: 30%)
        if (ranking != null && ranking.getScore() != null) {
            // 假设热度分数最高为100
            double hotScore = Math.min(ranking.getScore().doubleValue() / 100.0, 1.0) * 30;
            score += hotScore;
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * 计算新品推荐分数
     * 综合考虑：发布时间新鲜度、评分
     * 返回值范围: [0, 100]
     */
    private double calculateNewScriptScore(Script script) {
        double score = 0.0;
        
        // 1. 时间新鲜度 (权重: 50%)
        if (script.getCreateTime() != null) {
            long daysSinceCreation = ChronoUnit.DAYS.between(script.getCreateTime(), LocalDateTime.now());
            // 30天内的新品得分较高
            double freshnessScore = Math.max(0, 1.0 - daysSinceCreation / 30.0) * 50;
            score += freshnessScore;
        }
        
        // 2. 评分贡献 (权重: 30%)
        if (script.getRating() != null) {
            double ratingScore = script.getRating().doubleValue() / 5.0 * 30;
            score += ratingScore;
        }
        
        // 3. 独家加成 (权重: 20%)
        if (script.getIsExclusive() != null && script.getIsExclusive() == 1) {
            score += 20;
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * 计算综合推荐分数
     * 用于个性化推荐，结合多种因素
     * 返回值范围: [0, 100]
     */
    private double calculateComprehensiveScore(Script script, Long userId, Integer recommendationType, String reason) {
        double score = 60.0; // 基础分
        
        // 1. 评分贡献 (+0~20分)
        if (script.getRating() != null) {
            score += script.getRating().doubleValue() / 5.0 * 20;
        }
        
        // 2. 根据推荐类型调整
        switch (recommendationType) {
            case 1: // 协同过滤 - 社交验证加成
                score += 5;
                break;
            case 2: // 内容推荐 - 相关性加成
                score += 3;
                break;
            case 3: // 热门推荐 - 流行度加成
                score += 8;
                break;
            case 4: // 历史推荐 - 个性化加成
                score += 10;
                break;
        }
        
        // 3. 独家内容加成 (+5分)
        if (script.getIsExclusive() != null && script.getIsExclusive() == 1) {
            score += 5;
        }
        
        // 4. 如果有用户ID，计算偏好匹配度
        if (userId != null) {
            try {
                List<UserPreference> preferences = userPreferenceMapper.getUserTopPreferences(userId, 10);
                if (preferences != null && !preferences.isEmpty()) {
                    double prefScore = calculateUserPreferenceScore(script, preferences);
                    // 偏好匹配可贡献额外 0~15 分
                    score += prefScore * 0.15;
                }
            } catch (Exception e) {
                log.debug("计算用户偏好分数时出错: {}", e.getMessage());
            }
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取相似用户（浏览过相同剧本的用户）
     */
    private List<Long> getSimilarUsers(Long scriptId, Integer limit) {
        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getTargetType, 1)
               .eq(UserBrowseHistory::getTargetId, scriptId)
               .eq(UserBrowseHistory::getIsDeleted, 0)
               .orderByDesc(UserBrowseHistory::getBrowseTime)
               .last("LIMIT " + limit);
        
        List<UserBrowseHistory> histories = browseHistoryMapper.selectList(wrapper);
        return histories.stream()
                .map(UserBrowseHistory::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 批量查询分类名称 Map（scriptId -> categoryName）
     */
    private Map<Long, String> batchGetCategoryNames(List<Script> scripts) {
        Set<Long> categoryIds = scripts.stream()
                .map(Script::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return Collections.emptyMap();

        List<ScriptCategory> categories = scriptCategoryMapper.selectBatchIds(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(ScriptCategory::getId, ScriptCategory::getName, (a, b) -> a));
    }

    /**
     * 批量查询标签 Map（scriptId -> List<String>）
     */
    private Map<Long, List<String>> batchGetTags(List<Script> scripts) {
        List<Long> scriptIds = scripts.stream().map(Script::getId).collect(Collectors.toList());
        if (scriptIds.isEmpty()) return Collections.emptyMap();
        return scriptTagMapper.batchGetScriptTags(scriptIds);
    }

    /**
     * 批量查询收藏状态 Set（已收藏的 scriptId 集合）
     */
    private Set<Long> batchGetFavoriteIds(Long userId, List<Script> scripts) {
        if (userId == null || scripts.isEmpty()) return Collections.emptySet();
        List<Long> scriptIds = scripts.stream().map(Script::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId).in(ScriptFavorite::getScriptId, scriptIds);
        return scriptFavoriteMapper.selectList(wrapper).stream()
                .map(ScriptFavorite::getScriptId)
                .collect(Collectors.toSet());
    }

    /**
     * 构建推荐VO列表
     */
    private List<RecommendationVO> buildRecommendationVOs(List<Long> scriptIds, Long userId, 
                                                          Integer recommendationType, String reason) {
        if (scriptIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Script> scripts = scriptMapper.selectBatchIds(scriptIds);
        return convertToRecommendationVOs(scripts, userId, recommendationType, reason);
    }

    /**
     * 构建协同过滤推荐VO列表（批量查询，避免 N+1）
     */
    private List<RecommendationVO> buildCollaborativeRecommendationVOs(List<Long> scriptIds, Long userId,
                                                                        Map<Long, Integer> scriptViewCounts, int maxViewCount) {
        if (scriptIds.isEmpty()) return Collections.emptyList();

        List<Script> scripts = scriptMapper.selectBatchIds(scriptIds);
        Map<Long, String> categoryNameMap = batchGetCategoryNames(scripts);
        Map<Long, List<String>> tagsMap = batchGetTags(scripts);
        Set<Long> favoriteIds = batchGetFavoriteIds(userId, scripts);

        return scripts.stream().map(script -> {
            RecommendationVO vo = new RecommendationVO();
            BeanUtils.copyProperties(script, vo);
            vo.setCategoryName(categoryNameMap.get(script.getCategoryId()));
            vo.setTags(tagsMap.getOrDefault(script.getId(), Collections.emptyList()));
            vo.setRecommendationType(1);
            vo.setRecommendReason("看了这个的人还看了");
            vo.setIsFavorite(favoriteIds.contains(script.getId()));

            double cfScore = calculateCollaborativeScore(script.getId(), scriptViewCounts, maxViewCount);
            double ratingBonus = script.getRating() != null ? script.getRating().doubleValue() * 2 : 0;
            double finalScore = Math.min(100.0, cfScore * 0.7 + ratingBonus + 20);
            vo.setRecommendScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建热门推荐VO列表（批量查询，避免 N+1）
     */
    private List<RecommendationVO> buildHotRecommendationVOs(List<Long> scriptIds, Map<Long, HotRanking> rankingMap) {
        if (scriptIds.isEmpty()) return Collections.emptyList();

        List<Script> scripts = scriptMapper.selectBatchIds(scriptIds);
        Map<Long, String> categoryNameMap = batchGetCategoryNames(scripts);
        Map<Long, List<String>> tagsMap = batchGetTags(scripts);

        return scripts.stream().map(script -> {
            RecommendationVO vo = new RecommendationVO();
            BeanUtils.copyProperties(script, vo);
            vo.setCategoryName(categoryNameMap.get(script.getCategoryId()));
            vo.setTags(tagsMap.getOrDefault(script.getId(), Collections.emptyList()));
            vo.setRecommendationType(3);
            vo.setRecommendReason("热门推荐");
            vo.setIsFavorite(false);

            HotRanking ranking = rankingMap.get(script.getId());
            double hotScore = calculateHotScore(script, ranking);
            vo.setRecommendScore(BigDecimal.valueOf(hotScore).setScale(2, RoundingMode.HALF_UP));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建新品推荐VO列表（批量查询，避免 N+1）
     */
    private List<RecommendationVO> buildNewScriptRecommendationVOs(List<Script> scripts) {
        if (scripts.isEmpty()) return Collections.emptyList();

        Map<Long, String> categoryNameMap = batchGetCategoryNames(scripts);
        Map<Long, List<String>> tagsMap = batchGetTags(scripts);

        return scripts.stream().map(script -> {
            RecommendationVO vo = new RecommendationVO();
            BeanUtils.copyProperties(script, vo);
            vo.setCategoryName(categoryNameMap.get(script.getCategoryId()));
            vo.setTags(tagsMap.getOrDefault(script.getId(), Collections.emptyList()));
            vo.setRecommendationType(5); // 新品推荐独立类型
            vo.setRecommendReason("新品上架");
            vo.setIsFavorite(false);

            double newScore = calculateNewScriptScore(script);
            vo.setRecommendScore(BigDecimal.valueOf(newScore).setScale(2, RoundingMode.HALF_UP));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建基于内容的推荐VO列表（批量查询，避免 N+1）
     */
    private List<RecommendationVO> buildContentBasedRecommendationVOs(Script sourceScript, List<Script> targetScripts, Long userId) {
        if (targetScripts.isEmpty()) return Collections.emptyList();

        Map<Long, String> categoryNameMap = batchGetCategoryNames(targetScripts);
        Map<Long, List<String>> tagsMap = batchGetTags(targetScripts);
        Set<Long> favoriteIds = batchGetFavoriteIds(userId, targetScripts);

        return targetScripts.stream().map(script -> {
            RecommendationVO vo = new RecommendationVO();
            BeanUtils.copyProperties(script, vo);
            vo.setCategoryName(categoryNameMap.get(script.getCategoryId()));
            vo.setTags(tagsMap.getOrDefault(script.getId(), Collections.emptyList()));
            vo.setRecommendationType(2);
            vo.setRecommendReason("相似风格");
            vo.setIsFavorite(favoriteIds.contains(script.getId()));

            double contentScore = calculateContentSimilarityScore(sourceScript, script);
            vo.setRecommendScore(BigDecimal.valueOf(contentScore).setScale(2, RoundingMode.HALF_UP));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 转换为推荐VO（批量查询，避免 N+1）
     */
    private List<RecommendationVO> convertToRecommendationVOs(List<Script> scripts, Long userId,
                                                              Integer recommendationType, String reason) {
        if (scripts.isEmpty()) return Collections.emptyList();

        // 批量查询分类名、标签、收藏状态
        Map<Long, String> categoryNameMap = batchGetCategoryNames(scripts);
        Map<Long, List<String>> tagsMap = batchGetTags(scripts);
        Set<Long> favoriteIds = batchGetFavoriteIds(userId, scripts);

        // 批量计算用户偏好分（只查一次）
        List<UserPreference> preferences = Collections.emptyList();
        if (userId != null) {
            try {
                preferences = userPreferenceMapper.getUserTopPreferences(userId, 10);
            } catch (Exception e) {
                log.debug("获取用户偏好失败: {}", e.getMessage());
            }
        }
        final List<UserPreference> finalPreferences = preferences;

        return scripts.stream().map(script -> {
            RecommendationVO vo = new RecommendationVO();
            BeanUtils.copyProperties(script, vo);

            vo.setCategoryName(categoryNameMap.get(script.getCategoryId()));
            vo.setTags(tagsMap.getOrDefault(script.getId(), Collections.emptyList()));
            vo.setRecommendationType(recommendationType);
            vo.setRecommendReason(reason);
            vo.setIsFavorite(favoriteIds.contains(script.getId()));

            double realScore = calculateComprehensiveScoreWithPrefs(script, userId, recommendationType, finalPreferences);
            vo.setRecommendScore(BigDecimal.valueOf(realScore).setScale(2, RoundingMode.HALF_UP));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 综合分数计算（接受已查好的偏好列表，避免重复查 DB）
     */
    private double calculateComprehensiveScoreWithPrefs(Script script, Long userId,
                                                        Integer recommendationType,
                                                        List<UserPreference> preferences) {
        double score = 60.0;

        if (script.getRating() != null) {
            score += script.getRating().doubleValue() / 5.0 * 20;
        }

        switch (recommendationType != null ? recommendationType : 0) {
            case 1: score += 5; break;  // 协同过滤
            case 2: score += 3; break;  // 内容推荐
            case 3: score += 8; break;  // 热门推荐
            case 4: score += 10; break; // 历史推荐
        }

        if (script.getIsExclusive() != null && script.getIsExclusive() == 1) {
            score += 5;
        }

        if (userId != null && !preferences.isEmpty()) {
            double prefScore = calculateUserPreferenceScore(script, preferences);
            score += prefScore * 0.15;
        }

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * 去重
     */
    private List<RecommendationVO> removeDuplicates(List<RecommendationVO> recommendations) {
        Set<Long> seen = new HashSet<>();
        return recommendations.stream()
                .filter(vo -> seen.add(vo.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 保存推荐日志
     */
    private void saveRecommendationLogs(Long userId, List<RecommendationVO> recommendations) {
        for (RecommendationVO vo : recommendations) {
            RecommendationLog log = RecommendationLog.builder()
                    .userId(userId)
                    .scriptId(vo.getId())
                    .recommendationType(vo.getRecommendationType())
                    .score(vo.getRecommendScore())
                    .build();
            
            recommendationLogMapper.insert(log);
        }
    }

    /**
     * 实时获取热门推荐
     */
    private List<RecommendationVO> getRealTimeHotRecommendations(Integer limit) {
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getStatus, 1)
               .eq(Script::getIsDeleted, 0)
               .orderByDesc(Script::getRating)
               .last("LIMIT " + limit);
        
        List<Script> scripts = scriptMapper.selectList(wrapper);
        return convertToRecommendationVOs(scripts, null, 3, "热门推荐");
    }

    /**
     * 计算行为分数（含时间衰减）
     * 衰减公式：score * e^(-λ * daysSince)，λ = 0.02（约35天衰减到50%）
     */
    private double calculateActionScore(Integer actionType) {
        return calculateActionScore(actionType, LocalDateTime.now());
    }

    private double calculateActionScore(Integer actionType, LocalDateTime actionTime) {
        double baseScore;
        switch (actionType) {
            case 1: baseScore = 1.0; break;  // 浏览
            case 2: baseScore = 3.0; break;  // 收藏
            case 3: baseScore = 5.0; break;  // 预约
            case 4: baseScore = 2.0; break;  // 评价
            default: baseScore = 1.0;
        }
        // 时间衰减：距今越久权重越低，最低保留 10%
        long daysSince = ChronoUnit.DAYS.between(actionTime, LocalDateTime.now());
        double decayFactor = Math.max(0.1, Math.exp(-0.02 * daysSince));
        return baseScore * decayFactor;
    }

    /**
     * 根据榜单类型获取开始时间
     */
    private LocalDateTime getStartTimeByRankingType(Integer rankingType) {
        LocalDateTime now = LocalDateTime.now();
        switch (rankingType) {
            case 1: return now.minusDays(1);   // 今日
            case 2: return now.minusDays(7);   // 本周
            case 3: return now.minusDays(30);  // 本月
            case 4: return now.minusDays(90);  // 口碑榜（近3个月）
            default: return now.minusDays(7);
        }
    }

    /**
     * 计算热度分数
     */
    private List<HotRanking> calculateHotScores(LocalDateTime startTime, Integer rankingType) {
        log.info("计算热度分数，类型：{}，开始时间：{}", rankingType, startTime);
        
        List<HotRanking> rankings = new ArrayList<>();
        
        if (rankingType == 1 || rankingType == 2) {
            // 今日热门(1)和本周热门(2) - 基于预约数量
            rankings = calculateReservationBasedRankings(startTime, rankingType);
        } else if (rankingType == 4) {
            // 口碑榜(4) - 基于剧本评分 duration 字段
            rankings = calculateRatingBasedRankings(rankingType);
        } else {
            // 其他类型 - 默认使用评分排序
            rankings = calculateDefaultRankings(rankingType);
        }
        
        log.info("榜单类型 {} 计算完成，共 {} 条数据", rankingType, rankings.size());
        return rankings;
    }
    
    /**
     * 基于预约数量计算排名（今日热门、本周热门）
     */
    private List<HotRanking> calculateReservationBasedRankings(LocalDateTime startTime, Integer rankingType) {
        // 获取所有启用的剧本
        LambdaQueryWrapper<Script> scriptWrapper = new LambdaQueryWrapper<>();
        scriptWrapper.eq(Script::getStatus, 1)
                     .eq(Script::getIsDeleted, 0);
        List<Script> scripts = scriptMapper.selectList(scriptWrapper);
        
        if (scripts.isEmpty()) {
            log.warn("没有可用的剧本");
            return Collections.emptyList();
        }
        
        // 统计每个剧本在时间范围内的预约数量
        Map<Long, Long> scriptReservationCount = new HashMap<>();
        
        for (Script script : scripts) {
            // 查询该剧本在时间范围内的预约数量
            LambdaQueryWrapper<Reservation> reservationWrapper = new LambdaQueryWrapper<>();
            reservationWrapper.eq(Reservation::getScriptId, script.getId())
                             .ge(Reservation::getCreateTime, startTime)
                             .in(Reservation::getStatus, Arrays.asList(1, 2, 3, 4)); // 排除已取消的
            
            Long count = reservationMapper.selectCount(reservationWrapper);
            scriptReservationCount.put(script.getId(), count);
        }
        
        // 按预约数量排序
        List<Map.Entry<Long, Long>> sortedEntries = scriptReservationCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        // 生成榜单
        List<HotRanking> rankings = new ArrayList<>();
        int rank = 1;
        
        for (Map.Entry<Long, Long> entry : sortedEntries) {
            Long scriptId = entry.getKey();
            Long reservationCount = entry.getValue();
            
            // 只保留有预约的剧本
            if (reservationCount > 0) {
                Script script = scriptMapper.selectById(scriptId);
                if (script != null) {
                    // 计算热度分数：预约数量 * 10 + 评分
                    BigDecimal score = BigDecimal.valueOf(reservationCount * 10)
                            .add(script.getRating() != null ? script.getRating() : BigDecimal.ZERO);
                    
                    HotRanking ranking = HotRanking.builder()
                            .rankingType(rankingType)
                            .scriptId(scriptId)
                            .rank(rank++)
                            .score(score)
                            .rating(script.getRating())
                            .build();
                    
                    rankings.add(ranking);
                    
                    if (rank > 50) break; // 只保留前50名
                }
            }
        }
        
        log.info("基于预约数量的榜单生成完成，共 {} 条数据", rankings.size());
        return rankings;
    }
    
    /**
     * 基于评分计算排名（口碑榜）
     */
    private List<HotRanking> calculateRatingBasedRankings(Integer rankingType) {
        // 查询所有剧本，按照 rating 字段（评分）排序
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getStatus, 1)
               .eq(Script::getIsDeleted, 0)
               .isNotNull(Script::getRating) // 确保有评分数据
               .orderByDesc(Script::getRating) // 按照评分降序
               .last("LIMIT 50");
        
        List<Script> scripts = scriptMapper.selectList(wrapper);
        
        List<HotRanking> rankings = new ArrayList<>();
        int rank = 1;
        
        for (Script script : scripts) {
            // 使用 rating 字段作为评分依据
            BigDecimal rating = script.getRating() != null ? script.getRating() : BigDecimal.ZERO;
            
            HotRanking ranking = HotRanking.builder()
                    .rankingType(rankingType)
                    .scriptId(script.getId())
                    .rank(rank++)
                    .score(rating)
                    .rating(rating)
                    .build();
            
            rankings.add(ranking);
        }
        
        log.info("基于rating的口碑榜生成完成，共 {} 条数据", rankings.size());
        return rankings;
    }
    
    /**
     * 默认排名计算（基于评分）
     */
    private List<HotRanking> calculateDefaultRankings(Integer rankingType) {
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getStatus, 1)
               .eq(Script::getIsDeleted, 0)
               .orderByDesc(Script::getRating)
               .last("LIMIT 50");
        
        List<Script> scripts = scriptMapper.selectList(wrapper);
        
        List<HotRanking> rankings = new ArrayList<>();
        int rank = 1;
        
        for (Script script : scripts) {
            // 防止 rating 为 null 导致 NullPointerException
            BigDecimal rating = script.getRating() != null ? script.getRating() : BigDecimal.ZERO;
            BigDecimal score = rating.multiply(BigDecimal.TEN);
            
            HotRanking ranking = HotRanking.builder()
                    .rankingType(rankingType)
                    .scriptId(script.getId())
                    .rank(rank++)
                    .score(score)
                    .rating(rating)
                    .build();
            
            rankings.add(ranking);
        }
        
        log.info("默认榜单生成完成，共 {} 条数据", rankings.size());
        return rankings;
    }

    // ==================== AI增强推荐 ====================

    @Override
    public List<RecommendationVO> getAiEnhancedRecommendations(Long userId, Integer limit) {
        log.info("AI增强推荐 userId={}, limit={}", userId, limit);
        List<RecommendationVO> candidates = getPersonalizedRecommendations(userId, Math.min(limit * 2, 20));
        if (candidates == null || candidates.isEmpty()) candidates = getHotRecommendations(1, limit);
        if (candidates == null || candidates.isEmpty()) return new ArrayList<>();
        String userPrefDesc = buildUserPreferenceDesc(userId);
        return aiReRankAndAddReasons(candidates, userPrefDesc, limit);
    }

    @Override
    public Map<String, Object> getAiUserProfile(Long userId) {
        log.info("AI用户画像 userId={}", userId);
        Map<String, Object> profile = new HashMap<>();
        String behaviorDesc = buildUserPreferenceDesc(userId);
        // 提取tag类型的偏好值作为标签
        List<UserPreference> prefs = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .likeRight(UserPreference::getPreferenceType, "tag")
                .orderByDesc(UserPreference::getScore).last("LIMIT 20"));
        List<String> prefTags = new ArrayList<>();
        for (UserPreference p : prefs) {
            String val = p.getPreferenceValue();
            if (val != null && !val.trim().isEmpty() && !prefTags.contains(val.trim())) {
                prefTags.add(val.trim());
            }
            if (prefTags.size() >= 8) break;
        }
        profile.put("userId", userId);
        profile.put("preferenceTags", prefTags);
        profile.put("behaviorSummary", behaviorDesc);
        profile.put("aiProfile", callAiForUserProfile(behaviorDesc, prefTags));
        profile.put("generatedAt", java.time.LocalDateTime.now().toString());
        return profile;
    }

    private String buildUserPreferenceDesc(Long userId) {
        try {
            List<UserPreference> prefs = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                    .eq(UserPreference::getUserId, userId)
                    .orderByDesc(UserPreference::getScore).last("LIMIT 15"));
            if (prefs.isEmpty()) return "新用户，暂无历史偏好";
            // 按preferenceType分组提取偏好描述
            Set<String> tagValues = new LinkedHashSet<>();
            Set<String> categoryValues = new LinkedHashSet<>();
            int totalCount = 0;
            for (UserPreference p : prefs) {
                String type = p.getPreferenceType();
                String val  = p.getPreferenceValue();
                if (type != null && val != null) {
                    if (type.startsWith("tag"))      tagValues.add(val.trim());
                    if (type.startsWith("category")) categoryValues.add(val.trim());
                }
                if (p.getCount() != null) totalCount += p.getCount();
            }
            StringBuilder sb = new StringBuilder();
            if (!tagValues.isEmpty())      sb.append("偏好标签：").append(String.join("、", tagValues)).append("；");
            if (!categoryValues.isEmpty()) sb.append("偏好分类：").append(String.join("、", categoryValues)).append("；");
            sb.append("累计互动次数：").append(totalCount);
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建用户偏好描述失败: {}", e.getMessage());
            return "偏好数据获取失败";
        }
    }

    @SuppressWarnings("unchecked")
    private List<RecommendationVO> aiReRankAndAddReasons(List<RecommendationVO> candidates, String userPrefDesc, int limit) {
        String apiKey = environment.getProperty("ai.api-key", "");
        String apiUrl = environment.getProperty("ai.api-url", "https://api.deepseek.com/chat/completions");
        String model  = environment.getProperty("ai.model", "deepseek-chat");

        if (apiKey.isBlank()) {
            for (int i = 0; i < candidates.size() && i < limit; i++) {
                RecommendationVO vo = candidates.get(i);
                vo.setAiRank(i + 1);
                if (vo.getAiReason() == null) vo.setAiReason(buildRuleReason(vo));
            }
            return candidates.subList(0, Math.min(limit, candidates.size()));
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                RecommendationVO vo = candidates.get(i);
                sb.append(i+1).append(".《").append(vo.getName()).append("》分类:").append(vo.getCategoryName())
                  .append(",评分:").append(vo.getRating()).append(",标签:")
                  .append(vo.getTags()!=null?String.join("/",vo.getTags()):"无").append("\n");
            }
            String prompt = "你是剧本杀推荐助手。根据用户偏好从候选剧本选出最适合的" + limit + "个重新排序，" +
                "为每个生成15字以内的个性化推荐理由。\n用户偏好：" + userPrefDesc + "\n候选剧本：\n" + sb +
                "\n只返回JSON数组，格式：[{\"rank\":1,\"index\":1,\"reason\":\"推荐理由\"}...]";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> body = new HashMap<>();
            body.put("model", model); body.put("temperature", 0.7); body.put("max_tokens", 800);
            body.put("messages", List.of(Map.of("role","user","content",prompt)));
            ResponseEntity<Map> resp = restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<Map<String,Object>> choices = (List<Map<String,Object>>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String,Object> msg = (Map<String,Object>) choices.get(0).get("message");
                    String content = msg != null ? (String) msg.get("content") : null;
                    if (content != null) {
                        content = content.trim().replaceAll("```json","").replaceAll("```","").trim();
                        ObjectMapper mapper = new ObjectMapper();
                        List<Map<String,Object>> aiRes = mapper.readValue(content,
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                        List<RecommendationVO> reranked = new ArrayList<>();
                        for (Map<String,Object> item : aiRes) {
                            int idx = ((Number)item.get("index")).intValue()-1;
                            if (idx>=0 && idx<candidates.size()) {
                                RecommendationVO vo = candidates.get(idx);
                                vo.setAiRank(((Number)item.get("rank")).intValue());
                                vo.setAiReason((String)item.get("reason"));
                                reranked.add(vo);
                            }
                        }
                        reranked.sort(Comparator.comparingInt(v -> v.getAiRank()!=null?v.getAiRank():99));
                        return reranked.subList(0, Math.min(limit, reranked.size()));
                    }
                }
            }
        } catch (Exception e) { log.warn("AI重排序失败，降级: {}", e.getMessage()); }
        for (int i = 0; i < candidates.size() && i < limit; i++) {
            candidates.get(i).setAiRank(i+1);
            candidates.get(i).setAiReason(buildRuleReason(candidates.get(i)));
        }
        return candidates.subList(0, Math.min(limit, candidates.size()));
    }

    private String buildRuleReason(RecommendationVO vo) {
        if (vo.getRecommendationType() != null) {
            switch (vo.getRecommendationType()) {
                case 1: return "和你口味相似的玩家都爱这个";
                case 2: return "与你喜欢的剧本风格相近";
                case 3: return "近期最多玩家选择";
                case 4: return "根据你的游玩历史推荐";
            }
        }
        if (vo.getRating() != null && vo.getRating().doubleValue() >= 4.5) return "口碑极佳，强烈推荐";
        if (vo.getTags() != null && !vo.getTags().isEmpty()) return "符合你的" + vo.getTags().get(0) + "偏好";
        return "综合评分推荐";
    }

    @SuppressWarnings("unchecked")
    private String callAiForUserProfile(String behaviorDesc, List<String> prefTags) {
        String apiKey = environment.getProperty("ai.api-key", "");
        String apiUrl = environment.getProperty("ai.api-url", "https://api.deepseek.com/chat/completions");
        String model  = environment.getProperty("ai.model", "deepseek-chat");
        if (apiKey.isBlank()) {
            return prefTags.isEmpty() ? "探索中的新玩家" :
                "热衷" + String.join("、", prefTags.subList(0, Math.min(3, prefTags.size()))) + "风格的资深玩家";
        }
        try {
            String prompt = "你是剧本杀用户分析师。根据以下数据，用2-3句话生成有趣的玩家画像，要口语化有个性：\n" +
                "行为：" + behaviorDesc + "\n偏好标签：" + String.join("、", prefTags) + "\n只返回画像描述。";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String,Object> body = new HashMap<>();
            body.put("model", model); body.put("temperature", 0.8); body.put("max_tokens", 200);
            body.put("messages", List.of(Map.of("role","user","content",prompt)));
            ResponseEntity<Map> resp = restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<Map<String,Object>> choices = (List<Map<String,Object>>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String,Object> msg = (Map<String,Object>) choices.get(0).get("message");
                    if (msg != null) return (String) msg.get("content");
                }
            }
        } catch (Exception e) { log.warn("AI用户画像失败: {}", e.getMessage()); }
        return prefTags.isEmpty() ? "探索中的新玩家" :
            "热衷" + String.join("、", prefTags.subList(0, Math.min(3, prefTags.size()))) + "风格的资深玩家";
    }
}
