package com.murder.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理端通知 WebSocket Handler
 *
 * 连接方式：/api/ws/admin-notification?adminId=xxx
 */
@Slf4j
@Component
public class AdminNotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> ADMIN_SESSIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("adminId=")) {
            String adminId = query.split("adminId=")[1].split("&")[0];
            Long aid = Long.parseLong(adminId);
            ADMIN_SESSIONS.put(aid, session);
            log.info("Admin WebSocket连接建立: adminId={}, sessionId={}", aid, session.getId());
        } else {
            log.warn("Admin WebSocket连接缺少adminId参数: sessionId={}", session.getId());
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ADMIN_SESSIONS.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        log.info("Admin WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Admin WebSocket传输错误: sessionId={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        ADMIN_SESSIONS.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

    /**
     * 广播推送给所有在线管理端
     */
    public void pushNotificationToAll(Map<String, Object> notification) {
        ADMIN_SESSIONS.forEach((adminId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(notification);
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("推送管理端通知失败: adminId={}", adminId, e);
                }
            }
        });
        log.info("广播管理端通知完成, onlineAdmins={}", ADMIN_SESSIONS.size());
    }

    public int getOnlineAdminCount() {
        return ADMIN_SESSIONS.size();
    }
}
