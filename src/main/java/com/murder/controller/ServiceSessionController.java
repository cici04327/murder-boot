package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.common.result.Result;
import com.murder.entity.ServiceMessage;
import com.murder.entity.ServiceSession;
import com.murder.service.ServiceSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
@Tag(name = "客服会话接口")
public class ServiceSessionController {

    private final ServiceSessionService sessionService;

    /**
     * 用户发起转人工请求
     */
    @PostMapping("/session/create")
    @Operation(summary = "用户发起转人工")
    public Result<Long> createSession(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw new BaseException("请先登录");
        String userName = body.getOrDefault("userName", "用户");
        String question = body.getOrDefault("question", "");
        Long sessionId = sessionService.createSession(userId, userName, question);
        return Result.success(sessionId);
    }

    /**
     * 查询当前用户是否有进行中的会话
     */
    @GetMapping("/session/active")
    @Operation(summary = "查询当前活跃会话")
    public Result<ServiceSession> getActiveSession() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.success(null);
        ServiceSession session = sessionService.getActiveSessionByUserId(userId);
        return Result.success(session);
    }

    /**
     * 发送消息（用户端）
     */
    @PostMapping("/session/{sessionId}/message")
    @Operation(summary = "用户发送消息")
    public Result<ServiceMessage> sendUserMessage(@PathVariable Long sessionId,
                                                   @RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        String content = body.get("content");
        String msgType = body.getOrDefault("msgType", "text");
        ServiceMessage msg = sessionService.sendMessage(sessionId, "user", userId, content, msgType);
        return Result.success(msg);
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/session/{sessionId}/messages")
    @Operation(summary = "获取会话消息列表")
    public Result<List<ServiceMessage>> getMessages(@PathVariable Long sessionId) {
        return Result.success(sessionService.getMessages(sessionId));
    }

    /**
     * 用户结束会话
     */
    @PostMapping("/session/{sessionId}/close")
    @Operation(summary = "结束会话")
    public Result<String> closeSession(@PathVariable Long sessionId,
                                        @RequestParam(defaultValue = "user") String closerType) {
        sessionService.closeSession(sessionId, closerType);
        return Result.success("会话已结束");
    }

    /**
     * 用户评价会话
     */
    @PostMapping("/session/{sessionId}/rate")
    @Operation(summary = "评价会话")
    public Result<String> rateSession(@PathVariable Long sessionId,
                                       @RequestBody Map<String, Object> body) {
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String comment = (String) body.getOrDefault("comment", "");
        sessionService.rateSession(sessionId, rating, comment);
        return Result.success("评价成功");
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端：获取会话列表
     */
    @GetMapping("/admin/sessions")
    @Operation(summary = "管理端获取会话列表")
    public Result<Map<String, Object>> listSessions(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        requireSuperAdmin();
        return Result.success(sessionService.listSessions(status, page, pageSize));
    }

    /**
     * 管理端：接入会话
     */
    @PostMapping("/admin/session/{sessionId}/accept")
    @Operation(summary = "管理员接入会话")
    public Result<String> acceptSession(@PathVariable Long sessionId) {
        requireSuperAdmin();
        Long adminId = BaseContext.getCurrentId();
        sessionService.acceptSession(sessionId, adminId);
        return Result.success("已接入会话");
    }

    /**
     * 管理端：发送消息
     */
    @PostMapping("/admin/session/{sessionId}/message")
    @Operation(summary = "管理员发送消息")
    public Result<ServiceMessage> sendAdminMessage(@PathVariable Long sessionId,
                                                    @RequestBody Map<String, String> body) {
        requireSuperAdmin();
        Long adminId = BaseContext.getCurrentId();
        String content = body.get("content");
        String msgType = body.getOrDefault("msgType", "text");
        ServiceMessage msg = sessionService.sendMessage(sessionId, "admin", adminId, content, msgType);
        return Result.success(msg);
    }

    /**
     * 管理端：结束会话
     */
    @PostMapping("/admin/session/{sessionId}/close")
    @Operation(summary = "管理员结束会话")
    public Result<String> closeSessionByAdmin(@PathVariable Long sessionId) {
        requireSuperAdmin();
        sessionService.closeSession(sessionId, "admin");
        return Result.success("会话已结束");
    }

    /**
     * 管理端：等待接入的会话数量
     */
    @GetMapping("/admin/waiting-count")
    @Operation(summary = "获取等待中的会话数量")
    public Result<Integer> countWaiting() {
        requireSuperAdmin();
        return Result.success(sessionService.countWaiting());
    }

    private void requireSuperAdmin() {
        String role = BaseContext.getRole();
        if (!"SUPER_ADMIN".equals(role) && !"STORE_ADMIN".equals(role)) {
            throw new BaseException("无权限，仅管理员可操作");
        }
    }
}
