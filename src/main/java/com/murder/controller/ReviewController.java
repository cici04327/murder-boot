package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ReviewDTO;
import com.murder.vo.ReviewStatisticsVO;
import com.murder.vo.ReviewVO;
import com.murder.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制�?
 */
@RestController
@RequestMapping("/api/reservation/review")
@Tag(name = "评价接口")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping
    @Operation(summary = "创建评价")
    public Result<String> create(@RequestBody ReviewDTO reviewDTO) {
        log.info("创建评价: {}", reviewDTO);
        reviewService.create(reviewDTO);
        return Result.success("评价成功");
    }

    /**
     * 分页查询评价列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询评价列表")
    public Result<PageResult<ReviewVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long scriptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询评价列表: page={}, pageSize={}, storeId={}, scriptId={}, userId={}, status={}", 
                page, pageSize, storeId, scriptId, userId, status);
        PageResult<ReviewVO> pageResult = reviewService.pageQuery(page, pageSize, storeId, scriptId, userId, status);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询评价详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询评价详情")
    public Result<ReviewVO> getById(@PathVariable Long id) {
        log.info("查询评价详情: {}", id);
        ReviewVO review = reviewService.getById(id);
        return Result.success(review);
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除评价: {}", id);
        reviewService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 审核评价
     */
    @PutMapping("/{id}/audit")
    @Operation(summary = "审核评价")
    public Result<String> audit(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String reason) {
        log.info("审核评价: id={}, status={}, reason={}", id, status, reason);
        reviewService.audit(id, status, reason);
        return Result.success("审核成功");
    }

    /**
     * 回复评价
     */
    @PutMapping("/{id}/reply")
    @Operation(summary = "回复评价")
    public Result<String> reply(
            @PathVariable Long id,
            @RequestParam String replyContent) {
        log.info("回复评价: id={}, replyContent={}", id, replyContent);
        reviewService.reply(id, replyContent);
        return Result.success("回复成功");
    }

    /**
     * 设置精选评价
     */
    @PutMapping("/{id}/featured")
    @Operation(summary = "设置精选评价")
    public Result<String> setFeatured(
            @PathVariable Long id,
            @RequestParam Integer isFeatured) {
        log.info("设置精选评价: id={}, isFeatured={}", id, isFeatured);
        reviewService.setFeatured(id, isFeatured);
        return Result.success("设置成功");
    }

    /**
     * 获取评价统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取评价统计")
    public Result<ReviewStatisticsVO> getStatistics(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long scriptId) {
        log.info("获取评价统计: storeId={}, scriptId={}", storeId, scriptId);
        ReviewStatisticsVO statistics = reviewService.getStatistics(storeId, scriptId);
        return Result.success(statistics);
    }
}
