package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.entity.Script;
import com.murder.service.ScriptFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 剧本收藏控制器?
 */
@RestController
@RequestMapping("/api/script/favorite")
@Tag(name = "剧本收藏接口")
@Slf4j
public class ScriptFavoriteController {
    
    @Autowired
    private ScriptFavoriteService scriptFavoriteService;
    
    /**
     * 收藏剧本
     */
    @PostMapping("/{scriptId}")
    @Operation(summary = "收藏剧本")
    public Result<String> favoriteScript(@PathVariable Long scriptId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        log.info("用户{}收藏剧本{}", userId, scriptId);
        
        try {
            scriptFavoriteService.favoriteScript(userId, scriptId);
            // 获取用户当前的收藏数?
            Integer favoriteCount = scriptFavoriteService.getFavoriteCount(userId);
            // 判断是否达到5的倍数，给出不同的提示
            if (favoriteCount % 5 == 0) {
                return Result.success("收藏成功！已收藏" + favoriteCount + "个剧本，获得20积分奖励");
            } else {
                int remaining = 5 - (favoriteCount % 5);
                return Result.success("收藏成功！再收藏" + remaining + "个剧本可获得积分奖励");
            }
        } catch (Exception e) {
            log.error("收藏失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 取消收藏剧本
     */
    @DeleteMapping("/{scriptId}")
    @Operation(summary = "取消收藏剧本")
    public Result<String> unfavoriteScript(@PathVariable Long scriptId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        log.info("用户{}取消收藏剧本{}", userId, scriptId);
        scriptFavoriteService.unfavoriteScript(userId, scriptId);
        return Result.success("取消收藏成功");
    }
    
    /**
     * 检查是否已收藏
     */
    @GetMapping("/{scriptId}/status")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavoriteStatus(@PathVariable Long scriptId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.success(false);
        }
        
        boolean favorited = scriptFavoriteService.isFavorited(userId, scriptId);
        return Result.success(favorited);
    }
    
    /**
     * 获取当前用户收藏的剧本列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取收藏的剧本列表")
    public Result<PageResult<Script>> getUserFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        log.info("查询用户{}的收藏列? page={}, pageSize={}", userId, page, pageSize);
        PageResult<Script> pageResult = scriptFavoriteService.getUserFavorites(userId, page, pageSize);
        return Result.success(pageResult);
    }
    
    /**
     * 获取收藏数量
     */
    @GetMapping("/count")
    @Operation(summary = "获取收藏数量")
    public Result<Integer> getFavoriteCount() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.success(0);
        }
        
        Integer count = scriptFavoriteService.getFavoriteCount(userId);
        return Result.success(count);
    }
}

