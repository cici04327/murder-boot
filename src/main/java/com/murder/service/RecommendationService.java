package com.murder.service;

import com.murder.vo.RecommendationVO;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口
 */
public interface RecommendationService {

    /**
     * 获取个性化推荐（综合推荐）
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getPersonalizedRecommendations(Long userId, Integer limit);

    /**
     * 基于协同过滤的推荐（看了这个还看了...）
     * @param userId 用户ID
     * @param scriptId 当前剧本ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getCollaborativeRecommendations(Long userId, Long scriptId, Integer limit);

    /**
     * 基于内容的推荐（相似剧本）
     * @param scriptId 剧本ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getContentBasedRecommendations(Long scriptId, Integer limit);

    /**
     * 基于用户历史的推荐
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getHistoryBasedRecommendations(Long userId, Integer limit);

    /**
     * 获取热门推荐
     * @param rankingType 榜单类型：1今日热门，2本周热门，3本月热门，4口碑榜
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getHotRecommendations(Integer rankingType, Integer limit);

    /**
     * 获取新品推荐
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<RecommendationVO> getNewScriptRecommendations(Integer limit);

    /**
     * 记录用户浏览行为
     * @param userId 用户ID
     * @param targetType 目标类型：1剧本，2门店
     * @param targetId 目标ID
     * @param duration 浏览时长（秒）
     */
    void recordBrowseHistory(Long userId, Integer targetType, Long targetId, Integer duration);

    /**
     * 记录推荐点击
     * @param userId 用户ID
     * @param scriptId 剧本ID
     */
    void recordRecommendationClick(Long userId, Long scriptId);

    /**
     * 记录推荐预约转化
     * @param userId 用户ID
     * @param scriptId 剧本ID
     */
    void recordRecommendationReserve(Long userId, Long scriptId);

    /**
     * 更新用户偏好（在用户预约、收藏、评价时调用）
     * @param userId 用户ID
     * @param scriptId 剧本ID
     * @param actionType 行为类型：1浏览，2收藏，3预约，4评价
     */
    void updateUserPreference(Long userId, Long scriptId, Integer actionType);

    /**
     * 刷新热门榜单（定时任务调用）
     * @param rankingType 榜单类型
     */
    void refreshHotRanking(Integer rankingType);

    /**
     * 获取推荐效果统计
     * @param days 统计天数
     * @return 统计数据
     */
    Map<String, Object> getRecommendationStats(Integer days);

    /**
     * AI增强推荐：规则引擎推荐 + AI重排序 + AI个性化推荐理由
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return AI增强推荐列表
     */
    List<RecommendationVO> getAiEnhancedRecommendations(Long userId, Integer limit);

    /**
     * AI用户画像分析：分析用户行为，生成偏好标签和画像描述
     * @param userId 用户ID
     * @return 用户画像Map（profile, tags, summary等）
     */
    Map<String, Object> getAiUserProfile(Long userId);
}
