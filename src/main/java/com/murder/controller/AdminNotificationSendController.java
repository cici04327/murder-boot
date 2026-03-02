package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端通知发送控制器（供内部服务调用）
 */
@RestController
@RequestMapping("/api/admin/notification")
@Tag(name = "管理端通知发送接口")
@Slf4j
public class AdminNotificationSendController {

    @Autowired
    private AdminNotificationService adminNotificationService;

    /**
     * 发送管理端通知（供内部服务调用）
     */
    @PostMapping("/send")
    @Operation(summary = "发送管理端通知")
    public Result<String> sendNotification(@RequestBody Map<String, Object> params) {
        try {
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            Integer type = (Integer) params.get("type");
            String bizType = (String) params.get("bizType");
            Long bizId = params.get("bizId") != null ? Long.valueOf(params.get("bizId").toString()) : null;
            Long storeId = params.get("storeId") != null ? Long.valueOf(params.get("storeId").toString()) : null;
            Integer priority = params.get("priority") != null ? (Integer) params.get("priority") : 2;
            
            log.info("接收到管理端通知发送请求: title={}, type={}, bizType={}, bizId={}", title, type, bizType, bizId);
            
            adminNotificationService.sendNotification(title, content, type, bizType, bizId, storeId, priority);
            
            return Result.success("通知发送成功");
        } catch (Exception e) {
            log.error("发送管理端通知失败", e);
            return Result.error("通知发送失败: " + e.getMessage());
        }
    }
}
