package com.murder.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服专用 WebSocket Handler
 * 用户连接：/api/ws/service?userId=xxx&role=user
 * 管理员连接：/api/ws/service?adminId=xxx&role=admin
 */
@Slf4j
@Component
public class ServiceWebSocketHandler extends TextWebSocketHandler {

    // userId -> session
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    // adminId -> session
    private static final Map<Long, WebSocketSession> ADMIN_SESSIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        java.net.URI uri = session.getUri();
        if (uri == null) {
            log.warn("客服WebSocket连接URI为null，尝试从attributes获取参数");
            session.close();
            return;
        }
        String query = uri.getQuery();
        // 兼容 Vite 代理：query 可能带在 path 里
        if (query == null || query.isEmpty()) {
            String path = uri.getPath();
            int idx = path != null ? path.indexOf('?') : -1;
            query = idx >= 0 ? path.substring(idx + 1) : null;
        }
        if (query == null || query.isEmpty()) {
            log.warn("客服WebSocket连接缺少查询参数: uri={}", uri);
            session.close();
            return;
        }

        String role = extractParam(query, "role");
        log.info("客服WebSocket连接: role={}, uri={}", role, uri);

        if ("admin".equals(role)) {
            String adminIdStr = extractParam(query, "adminId");
            if (adminIdStr == null || "null".equals(adminIdStr) || adminIdStr.isEmpty()) {
                log.warn("客服WebSocket管理员连接缺少adminId，拒绝连接");
                session.close();
                return;
            }
            try {
                Long adminId = Long.parseLong(adminIdStr);
                ADMIN_SESSIONS.put(adminId, session);
                log.info("客服WebSocket管理员连接成功: adminId={}, 当前在线管理员数={}", adminId, ADMIN_SESSIONS.size());
            } catch (NumberFormatException e) {
                log.warn("客服WebSocket管理员连接adminId格式错误: {}", adminIdStr);
                session.close();
            }
        } else {
            String userIdStr = extractParam(query, "userId");
            if (userIdStr == null || "null".equals(userIdStr) || userIdStr.isEmpty()) {
                log.warn("客服WebSocket用户连接缺少userId，拒绝连接");
                session.close();
                return;
            }
            try {
                Long userId = Long.parseLong(userIdStr);
                USER_SESSIONS.put(userId, session);
                log.info("客服WebSocket用户连接成功: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("客服WebSocket用户连接userId格式错误: {}", userIdStr);
                session.close();
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        USER_SESSIONS.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        ADMIN_SESSIONS.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        log.info("客服WebSocket连接关闭: sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (isConnectionClosed(exception)) {
            log.warn("客服WebSocket连接已断开: sessionId={}, msg={}", session.getId(), exception.getMessage());
        } else {
            log.error("客服WebSocket传输异常: sessionId={}", session.getId(), exception);
        }
        if (session.isOpen()) session.close();
        USER_SESSIONS.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        ADMIN_SESSIONS.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    /** 推送消息给指定用户 */
    public void sendToUser(Long userId, Map<String, Object> msg) {
        send(USER_SESSIONS.get(userId), msg);
    }

    /** 推送消息给指定管理员 */
    public void sendToAdmin(Long adminId, Map<String, Object> msg) {
        send(ADMIN_SESSIONS.get(adminId), msg);
    }

    /** 广播给所有在线管理员（新会话通知） */
    public void notifyAdmins(Map<String, Object> msg) {
        ADMIN_SESSIONS.values().forEach(s -> send(s, msg));
    }

    /** 获取在线管理员ID集合 */
    public Set<Long> getOnlineAdminIds() {
        return ADMIN_SESSIONS.keySet();
    }

    private void send(WebSocketSession session, Map<String, Object> msg) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        } catch (Exception e) {
            if (isConnectionClosed(e)) {
                log.warn("客服WebSocket发送时连接已断开: {}", e.getMessage());
            } else {
                log.error("客服WebSocket发送失败: {}", e.getMessage(), e);
            }
        }
    }

    private String extractParam(String query, String key) {
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private boolean isConnectionClosed(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.nio.channels.ClosedChannelException
                    || current instanceof java.io.EOFException) {
                return true;
            }
            if (current instanceof java.io.IOException && current.getMessage() != null
                    && (current.getMessage().contains("Broken pipe")
                    || current.getMessage().contains("Connection reset")
                    || current.getMessage().contains("ClosedChannelException"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
