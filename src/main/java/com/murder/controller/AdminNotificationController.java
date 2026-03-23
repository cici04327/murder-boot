package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
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

        storeId = resolveScopedStoreId(storeId);

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
        storeId = resolveScopedStoreId(storeId);

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
        assertNotificationScope(id);
        log.info("标记通知为已读 id={}", id);
        adminNotificationService.markAsRead(id);
        return Result.success("标记成功");
    }

    /**
     * 标记所有通知为已�?
     */
    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已�?")
    public Result<String> markAllAsRead(@RequestParam(required = false) Long storeId) {
        storeId = resolveScopedStoreId(storeId);
        log.info("标记所有通知为已读 storeId={}", storeId);
        adminNotificationService.markAllAsRead(storeId);
        return Result.success("标记成功");
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public Result<String> deleteNotification(@PathVariable Long id) {
        assertNotificationScope(id);
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
        if (ids != null) {
            for (Long id : ids) {
                assertNotificationScope(id);
            }
        }
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
        assertNotificationScope(vo);
        return Result.success(vo);
    }

    /**
     * 获取通知统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取通知统计信息")
    public Result<Map<String, Object>> getStatistics(@RequestParam(required = false) Long storeId) {
        storeId = resolveScopedStoreId(storeId);
        log.info("获取通知统计信息: storeId={}", storeId);
        Map<String, Object> statistics = adminNotificationService.getStatistics(storeId);
        return Result.success(statistics);
    }

    private Long resolveScopedStoreId(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role)) {
            return requestedStoreId;
        }
        if ("STORE_ADMIN".equals(role)) {
            return BaseContext.getStoreId();
        }
        if ("STORE_STAFF".equals(role) && hasPermission("notification:view")) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前员工账号未绑定门店");
            }
            return storeId;
        }
        throw new BaseException("无权限访问通知中心");
    }

    private void assertNotificationScope(Long notificationId) {
        assertNotificationScope(adminNotificationService.getNotificationDetail(notificationId));
    }

    private void assertNotificationScope(AdminNotificationVO notification) {
        if (notification == null) {
            throw new BaseException("通知不存在");
        }
        Long scopedStoreId = resolveScopedStoreId(notification.getStoreId());
        if (scopedStoreId != null && notification.getStoreId() != null && !scopedStoreId.equals(notification.getStoreId())) {
            throw new BaseException("无权限操作该通知");
        }
    }

    private boolean hasPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (permissionCodes == null || permissionCode == null) {
            return false;
        }
        for (String code : permissionCodes.split(",")) {
            if (permissionCode.equals(code != null ? code.trim() : null)) {
                return true;
            }
        }
        return false;
    }
}
