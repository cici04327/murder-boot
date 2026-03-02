package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.vo.NotificationVO;
import com.murder.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notification")
@Tag(name = "通知接口")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取通知详情")
    public Result<NotificationVO> getDetail(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("获取通知详情: userId={}, notificationId={}", userId, id);
        NotificationVO notification = notificationService.getNotificationDetail(userId, id);
        
        if (notification == null) {
            return Result.error("通知不存在");
        }
        
        return Result.success(notification);
    }
    
    /**
     * 搜索通知
     */
    @GetMapping("/search")
    @Operation(summary = "搜索通知")
    public Result<PageResult<NotificationVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("搜索通知: userId={}, keyword={}, page={}, pageSize={}", 
                userId, keyword, page, pageSize);
        
        PageResult<NotificationVO> pageResult = notificationService.searchNotifications(
                userId, keyword, page, pageSize);
        return Result.success(pageResult);
    }
    
    /**
     * 获取通知统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取通知统计信息")
    public Result<Map<String, Object>> getStatistics() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("获取通知统计: userId={}", userId);
        Map<String, Object> statistics = notificationService.getNotificationStatistics(userId);
        return Result.success(statistics);
    }
    
    /**
     * 清空已读通知
     */
    @DeleteMapping("/clear-read")
    @Operation(summary = "清空已读通知")
    public Result<String> clearRead() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("清空已读通知: userId={}", userId);
        notificationService.clearReadNotifications(userId);
        return Result.success("清空成功");
    }
    
    /**
     * 分页查询用户通知列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户通知列表")
    public Result<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Boolean onlyUnread) {
        
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("分页查询用户通知: userId={}, page={}, pageSize={}, onlyUnread={}", 
                userId, page, pageSize, onlyUnread);
        
        PageResult<NotificationVO> pageResult = notificationService.getUserNotifications(userId, page, pageSize, onlyUnread);
        return Result.success(pageResult);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public Result<Long> getUnreadCount() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("标记通知为已读: userId={}, notificationId={}", userId, id);
        notificationService.markAsRead(userId, id);
        return Result.success("标记成功");
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读")
    public Result<String> markAllAsRead() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("标记所有通知为已读: userId={}", userId);
        notificationService.markAllAsRead(userId);
        return Result.success("标记成功");
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("删除通知: userId={}, notificationId={}", userId, id);
        notificationService.deleteNotification(userId, id);
        return Result.success("删除成功");
    }
    
    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除通知")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("批量删除通知: userId={}, count={}", userId, ids.size());
        notificationService.batchDeleteNotifications(userId, ids);
        return Result.success("批量删除成功");
    }
    
    /**
     * 批量标记为已读
     */
    @PutMapping("/batch-read")
    @Operation(summary = "批量标记为已读")
    public Result<String> batchMarkAsRead(@RequestBody List<Long> ids) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("批量标记为已读: userId={}, count={}", userId, ids.size());
        notificationService.batchMarkAsRead(userId, ids);
        return Result.success("批量标记成功");
    }
    
}
