package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.Result;
import com.murder.service.RecommendationService;
import com.murder.vo.RecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐系统Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendation")
@Tag(name = "推荐系统", description = "剧本推荐相关接口")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取个性化推荐
     */
    @GetMapping("/personalized")
    @Operation(summary = "获取个性化推荐", description = "根据用户历史行为生成个性化推荐")
    public Result<List<RecommendationVO>> getPersonalizedRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") Integer limit) {
        
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 请求个性化推荐，数量：{}", userId, limit);
        
        List<RecommendationVO> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
        return Result.success(recommendations);
    }

    /**
     * 获取协同过滤推荐（看了这个还看了...）
     */
    @GetMapping("/collaborative/{scriptId}")
    @Operation(summary = "协同过滤推荐", description = "根据其他用户的行为推荐相似剧本")
    public Result<List<RecommendationVO>> getCollaborativeRecommendations(
            @Parameter(description = "剧本ID") @PathVariable Long scriptId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "6") Integer limit) {
        
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 请求剧本 {} 的协同过滤推荐", userId, scriptId);
        
        List<RecommendationVO> recommendations = recommendationService.getCollaborativeRecommendations(userId, scriptId, limit);
        return Result.success(recommendations);
    }

    /**
     * 获取内容推荐（相似剧本）
     */
    @GetMapping("/similar/{scriptId}")
    @Operation(summary = "相似剧本推荐", description = "根据剧本内容推荐相似的剧本")
    public Result<List<RecommendationVO>> getContentBasedRecommendations(
            @Parameter(description = "剧本ID") @PathVariable Long scriptId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "6") Integer limit) {
        
        log.info("请求剧本 {} 的相似推荐", scriptId);
        
        List<RecommendationVO> recommendations = recommendationService.getContentBasedRecommendations(scriptId, limit);
        return Result.success(recommendations);
    }

    /**
     * 获取基于历史的推荐
     */
    @GetMapping("/history")
    @Operation(summary = "基于历史推荐", description = "根据用户历史偏好推荐剧本")
    public Result<List<RecommendationVO>> getHistoryBasedRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") Integer limit) {
        
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 请求基于历史的推荐", userId);
        
        List<RecommendationVO> recommendations = recommendationService.getHistoryBasedRecommendations(userId, limit);
        return Result.success(recommendations);
    }

    /**
     * 获取热门推荐
     */
    @GetMapping("/hot")
    @Operation(summary = "热门推荐", description = "获取热门剧本榜单")
    public Result<List<RecommendationVO>> getHotRecommendations(
            @Parameter(description = "榜单类型：1今日热门，2本周热门，3本月热门，4口碑榜") 
            @RequestParam(defaultValue = "1") Integer rankingType,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("请求热门推荐，类型：{}", rankingType);
        
        List<RecommendationVO> recommendations = recommendationService.getHotRecommendations(rankingType, limit);
        return Result.success(recommendations);
    }

    /**
     * 获取新品推荐
     */
    @GetMapping("/new")
    @Operation(summary = "新品推荐", description = "获取最新上架的剧本")
    public Result<List<RecommendationVO>> getNewScriptRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("请求新品推荐");
        
        List<RecommendationVO> recommendations = recommendationService.getNewScriptRecommendations(limit);
        return Result.success(recommendations);
    }

    /**
     * 记录浏览历史
     */
    @PostMapping("/browse")
    @Operation(summary = "记录浏览历史", description = "记录用户浏览剧本或门店的行为")
    public Result<String> recordBrowseHistory(
            @Parameter(description = "目标类型：1剧本，2门店") @RequestParam Integer targetType,
            @Parameter(description = "目标ID") @RequestParam Long targetId,
            @Parameter(description = "浏览时长（秒）") @RequestParam(defaultValue = "0") Integer duration) {
        
        Long userId = BaseContext.getCurrentId();
        log.info("记录用户 {} 的浏览行为，类型：{}，ID：{}", userId, targetType, targetId);
        
        recommendationService.recordBrowseHistory(userId, targetType, targetId, duration);
        return Result.success("记录成功");
    }

    /**
     * 记录推荐点击
     */
    @PostMapping("/click")
    @Operation(summary = "记录推荐点击", description = "记录用户点击推荐剧本的行为")
    public Result<String> recordClick(
            @Parameter(description = "剧本ID") @RequestParam Long scriptId) {
        
        Long userId = BaseContext.getCurrentId();
        log.info("记录用户 {} 点击推荐剧本 {}", userId, scriptId);
        
        recommendationService.recordRecommendationClick(userId, scriptId);
        return Result.success("记录成功");
    }

    /**
     * 获取推荐统计（管理端）
     */
    @GetMapping("/admin/stats")
    @Operation(summary = "推荐效果统计", description = "获取推荐系统的效果统计数据（管理端）")
    public Result<Map<String, Object>> getRecommendationStats(
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") Integer days) {
        
        log.info("查询推荐统计，天数：{}", days);
        
        Map<String, Object> stats = recommendationService.getRecommendationStats(days);
        return Result.success(stats);
    }

    /**
     * 刷新热门榜单（管理端）
     */
    @PostMapping("/admin/refresh-ranking")
    @Operation(summary = "刷新热门榜单", description = "手动刷新指定类型的热门榜单（管理端）")
    public Result<String> refreshHotRanking(
            @Parameter(description = "榜单类型：1今日热门，2本周热门，3本月热门，4口碑榜") 
            @RequestParam Integer rankingType) {
        
        log.info("刷新热门榜单，类型：{}", rankingType);
        
        recommendationService.refreshHotRanking(rankingType);
        return Result.success("刷新成功");
    }
}
