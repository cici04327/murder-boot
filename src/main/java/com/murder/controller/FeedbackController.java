package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.FeedbackDTO;
import com.murder.service.FeedbackService;
import com.murder.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 留言反馈控制器
 */
@RestController
@RequestMapping("/api/feedback")
@Tag(name = "留言反馈接口")
@Slf4j
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 提交留言（用户端）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交留言")
    public Result<String> submit(@Valid @RequestBody FeedbackDTO feedbackDTO, HttpServletRequest request) {
        // 尝试获取当前登录用户ID（可能为空，游客也可留言）
        Long userId = null;
        try {
            userId = BaseContext.getCurrentId();
        } catch (Exception e) {
            log.debug("游客提交留言");
        }
        
        // 获取IP地址
        String ipAddress = getClientIp(request);
        
        log.info("提交留言: name={}, subject={}, userId={}, ip={}", 
                feedbackDTO.getName(), feedbackDTO.getSubject(), userId, ipAddress);
        
        feedbackService.submit(feedbackDTO, userId, ipAddress);
        return Result.success("留言提交成功，我们会尽快处理！");
    }

    /**
     * 查询用户的留言列表
     */
    @GetMapping("/user")
    @Operation(summary = "查询用户的留言列表")
    public Result<PageResult<FeedbackVO>> getUserFeedbacks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("查询用户留言列表: userId={}, page={}, pageSize={}", userId, page, pageSize);
        
        PageResult<FeedbackVO> pageResult = feedbackService.getUserFeedbacks(userId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 分页查询留言列表（管理端）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询留言列表")
    public Result<PageResult<FeedbackVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询留言: page={}, pageSize={}, subject={}, status={}", page, pageSize, subject, status);
        
        PageResult<FeedbackVO> pageResult = feedbackService.pageQuery(page, pageSize, subject, status);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询留言详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询留言详情")
    public Result<FeedbackVO> getById(@PathVariable Long id) {
        log.info("查询留言详情: id={}", id);
        FeedbackVO feedback = feedbackService.getById(id);
        return Result.success(feedback);
    }

    /**
     * 回复留言（管理端）
     */
    @PutMapping("/{id}/reply")
    @Operation(summary = "回复留言")
    public Result<String> reply(
            @PathVariable Long id,
            @RequestParam String replyContent) {
        Long replyUserId = BaseContext.getCurrentId();
        log.info("回复留言: id={}, replyUserId={}", id, replyUserId);
        
        feedbackService.reply(id, replyContent, replyUserId);
        return Result.success("回复成功");
    }

    /**
     * 更新留言状态（管理端）
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新留言状态")
    public Result<String> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("更新留言状态: id={}, status={}", id, status);
        
        feedbackService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 删除留言（管理端）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除留言")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除留言: id={}", id);
        feedbackService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
