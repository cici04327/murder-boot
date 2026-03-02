package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知发送控制器 - 供内部服务调用
 */
@RestController
@RequestMapping("/api/notification")
@Tag(name = "通知发送接口")
@Slf4j
public class NotificationSendController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 发送通知给指定用户（供其他服务调用）
     */
    @PostMapping("/send")
    @Operation(summary = "发送通知给指定用户")
    public Result<String> sendNotification(@RequestBody Map<String, Object> requestData) {
        try {
            String title = (String) requestData.get("title");
            String content = (String) requestData.get("content");
            Integer type = (Integer) requestData.get("type");
            String bizType = (String) requestData.get("bizType");
            Long bizId = requestData.get("bizId") != null ? 
                    Long.valueOf(requestData.get("bizId").toString()) : null;
            
            Object userIdsObj = requestData.get("userIds");
            Long[] userIds;
            
            if (userIdsObj instanceof Long[]) {
                userIds = (Long[]) userIdsObj;
            } else if (userIdsObj instanceof Object[]) {
                Object[] arr = (Object[]) userIdsObj;
                userIds = new Long[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    userIds[i] = Long.valueOf(arr[i].toString());
                }
            } else if (userIdsObj instanceof java.util.List) {
                // 支持 List 类型（JSON 反序列化通常得到 ArrayList）
                java.util.List<?> list = (java.util.List<?>) userIdsObj;
                userIds = new Long[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    userIds[i] = Long.valueOf(list.get(i).toString());
                }
            } else {
                log.error("userIds格式错误，实际类型: {}", userIdsObj != null ? userIdsObj.getClass().getName() : "null");
                return Result.error("userIds格式错误，期望数组类型");
            }
            
            log.info("接收到发送通知请求: title={}, type={}, userIds={}", title, type, userIds);
            
            notificationService.sendToUsers(title, content, type, bizType, bizId, userIds);
            
            return Result.success("发送成功");
        } catch (Exception e) {
            log.error("发送通知失败", e);
            return Result.error("发送通知失败: " + e.getMessage());
        }
    }

    /**
     * 发送通知给所有用户
     */
    @PostMapping("/send-all")
    @Operation(summary = "发送通知给所有用户")
    public Result<String> sendToAll(@RequestBody Map<String, Object> requestData) {
        try {
            String title = (String) requestData.get("title");
            String content = (String) requestData.get("content");
            Integer type = (Integer) requestData.get("type");
            String bizType = (String) requestData.get("bizType");
            Long bizId = requestData.get("bizId") != null ? 
                    Long.valueOf(requestData.get("bizId").toString()) : null;
            
            log.info("接收到发送全体通知请求: title={}, type={}", title, type);
            
            notificationService.sendToAll(title, content, type, bizType, bizId);
            
            return Result.success("发送成功");
        } catch (Exception e) {
            log.error("发送全体通知失败", e);
            return Result.error("发送通知失败: " + e.getMessage());
        }
    }
}
