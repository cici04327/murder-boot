package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.vo.AdminNotificationVO;
import com.murder.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端通知控制�?
 */
@RestController
@RequestMapping("/api/admin/notification")
@Tag(name = "管理端通知接口")
@Slf4j
public class AdminNotificationController {

    @Autowired
    private AdminNotificationService adminNotificationService;

    /**
     * 获取管理端通知列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取管理端通知列表")
    public Result<PageResult<AdminNotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Boolean onlyUnread) {

        // 门店管理员强制只能看自己的门店
        String role = com.murder.common.context.BaseContext.getRole();
        Long ctxStoreId = com.murder.common.context.BaseContext.getStoreId();
        if ("STORE_ADMIN".equals(role)) {
            storeId = ctxStoreId;
        }

        log.info("获取管理端通知列表: page={}, pageSize={}, storeId={}, type={}, onlyUnread={}",
                page, pageSize, storeId, type, onlyUnread);
        PageResult<AdminNotificationVO> pageResult = adminNotificationService.getNotifications(page, pageSize, storeId, type, onlyUnread);
        return Result.success(pageResult);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public Result<Long> getUnreadCount(@RequestParam(required = false) Long storeId) {
        // 门店管理员强制只能看自己的门店
        String role = com.murder.common.context.BaseContext.getRole();
        Long ctxStoreId = com.murder.common.context.BaseContext.getStoreId();
        if ("STORE_ADMIN".equals(role)) {
            storeId = ctxStoreId;
        }

        log.info("获取未读通知数量: storeId={}", storeId);
        Long count = adminNotificationService.getUnreadCount(storeId);
        return Result.success(count);
    }

    /**
     * 标记通知为已�?
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已�?")
    public Result<String> markAsRead(@PathVariable Long id) {
        log.info("标记通知为已�? id={}", id);
        adminNotificationService.markAsRead(id);
        return Result.success("标记成功");
    }

    /**
     * 标记所有通知为已�?
     */
    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已�?")
    public Result<String> markAllAsRead(@RequestParam(required = false) Long storeId) {
        log.info("标记所有通知为已�? storeId={}", storeId);
        adminNotificationService.markAllAsRead(storeId);
        return Result.success("标记成功");
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public Result<String> deleteNotification(@PathVariable Long id) {
        log.info("删除通知: id={}", id);
        adminNotificationService.deleteNotification(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除通知")
    public Result<String> batchDeleteNotifications(@RequestBody List<Long> ids) {
        log.info("批量删除通知: ids={}", ids);
        adminNotificationService.batchDeleteNotifications(ids);
        return Result.success("删除成功");
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取通知详情")
    public Result<AdminNotificationVO> getNotificationDetail(@PathVariable Long id) {
        log.info("获取通知详情: id={}", id);
        AdminNotificationVO vo = adminNotificationService.getNotificationDetail(id);
        return Result.success(vo);
    }

    /**
     * 获取通知统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取通知统计信息")
    public Result<Map<String, Object>> getStatistics(@RequestParam(required = false) Long storeId) {
        log.info("获取通知统计信息: storeId={}", storeId);
        Map<String, Object> statistics = adminNotificationService.getStatistics(storeId);
        return Result.success(statistics);
    }
}
