package com.murder.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
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
 * 閫氱煡 WebSocket 澶勭悊鍣?
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // 瀛樺偍鐢ㄦ埛ID鍜學ebSocket浼氳瘽鐨勬槧灏?
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initObjectMapper() {
        // 支持 LocalDateTime 等时间类型序列化，避免推送时因 JSON 序列化失败导致接口 500
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * 杩炴帴寤虹珛鍚?
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 浠嶶RL鍙傛暟涓幏鍙栫敤鎴稩D
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String userId = query.split("userId=")[1].split("&")[0];
            Long uid = Long.parseLong(userId);
            USER_SESSIONS.put(uid, session);
            log.info("WebSocket杩炴帴寤虹珛: userId={}, sessionId={}", uid, session.getId());
        } else {
            log.warn("WebSocket杩炴帴缂哄皯userId鍙傛暟: sessionId={}", session.getId());
            session.close();
        }
    }

    /**
     * 鎺ユ敹鍒版秷鎭?
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("鏀跺埌WebSocket娑堟伅: sessionId={}, message={}", session.getId(), message.getPayload());
        // 蹇冭烦妫€娴?
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    /**
     * 杩炴帴鍏抽棴鍚?
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 绉婚櫎浼氳瘽
        USER_SESSIONS.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        log.info("WebSocket杩炴帴鍏抽棴: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 鍙戦€佸紓甯?
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket浼犺緭寮傚父: sessionId={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        USER_SESSIONS.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

    /**
     * 鎺ㄩ€侀€氱煡缁欐寚瀹氱敤鎴?
     */
    public void pushNotification(Long userId, Map<String, Object> notification) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("鎺ㄩ€侀€氱煡鎴愬姛: userId={}, notification={}", userId, notification);
            } catch (Exception e) {
                log.error("推送通知失败: userId={}, err={}", userId, e.getMessage(), e);
            }
        } else {
            log.debug("鐢ㄦ埛涓嶅湪绾挎垨浼氳瘽宸插叧闂? userId={}", userId);
        }
    }

    /**
     * 鎺ㄩ€侀€氱煡缁欐墍鏈夊湪绾跨敤鎴?
     */
    public void pushNotificationToAll(Map<String, Object> notification) {
        USER_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    String jsonMessage = objectMapper.writeValueAsString(notification);
                    session.sendMessage(new TextMessage(jsonMessage));
                    log.debug("鎺ㄩ€侀€氱煡鎴愬姛: userId={}", userId);
                } catch (Exception e) {
                    log.error("推送通知失败: userId={}, err={}", userId, e.getMessage(), e);
                }
            }
        });
        log.info("鎺ㄩ€侀€氱煡缁欐墍鏈夊湪绾跨敤鎴峰畬鎴愶紝鍦ㄧ嚎鐢ㄦ埛鏁? {}", USER_SESSIONS.size());
    }

    /**
     * 鑾峰彇鍦ㄧ嚎鐢ㄦ埛鏁?
     */
    public int getOnlineUserCount() {
        return USER_SESSIONS.size();
    }
}
