package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ScriptReviewDTO;
import com.murder.vo.ScriptReviewVO;
import com.murder.service.ScriptReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 剧本评价控制�?
 */
@RestController
@RequestMapping("/api/script/review")
@Tag(name = "剧本评价接口")
@Slf4j
public class ScriptReviewController {

    @Autowired
    private ScriptReviewService reviewService;

    /**
     * 添加剧本评价
     */
    @PostMapping
    @Operation(summary = "添加剧本评价")
    public Result<String> add(@RequestBody ScriptReviewDTO reviewDTO) {
        log.info("添加剧本评价: {}", reviewDTO);
        reviewService.add(reviewDTO);
        return Result.success("评价成功");
    }

    /**
     * 分页查询剧本评价列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询剧本评价列表")
    public Result<PageResult<ScriptReviewVO>> page(
            @RequestParam(required = false) Long scriptId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询剧本评价列表: scriptId={}, page={}, pageSize={}", scriptId, page, pageSize);
        PageResult<ScriptReviewVO> pageResult = reviewService.pageQuery(scriptId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询评价详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询评价详情")
    public Result<ScriptReviewVO> getById(@PathVariable Long id) {
        log.info("查询评价详情: {}", id);
        ScriptReviewVO review = reviewService.getById(id);
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
}
