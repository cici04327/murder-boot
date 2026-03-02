package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.entity.StoreReview;
import com.murder.vo.StoreReviewVO;
import com.murder.service.StoreReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 门店评价控制�?
 */
@RestController
@RequestMapping("/api/store/review")
@Tag(name = "门店评价接口")
@Slf4j
public class StoreReviewController {

    @Autowired
    private StoreReviewService reviewService;

    /**
     * 分页查询门店评价列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询门店评价列表")
    public Result<PageResult<StoreReviewVO>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询门店评价列表: storeId={}, page={}, pageSize={}", storeId, page, pageSize);
        PageResult<StoreReviewVO> pageResult = reviewService.pageQuery(storeId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询评价详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询评价详情")
    public Result<StoreReviewVO> getById(@PathVariable Long id) {
        log.info("查询评价详情: {}", id);
        StoreReviewVO review = reviewService.getById(id);
        return Result.success(review);
    }

    /**
     * 新增评价
     */
    @PostMapping
    @Operation(summary = "新增评价")
    public Result<String> add(@RequestBody StoreReview review) {
        log.info("新增评价: {}", review);
        reviewService.add(review);
        return Result.success("评价成功");
    }

    /**
     * 商家回复评价
     */
    @PutMapping("/reply/{id}")
    @Operation(summary = "商家回复评价")
    public Result<String> reply(@PathVariable Long id, @RequestParam String reply) {
        log.info("商家回复评价: id={}, reply={}", id, reply);
        reviewService.reply(id, reply);
        return Result.success("回复成功");
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
}
