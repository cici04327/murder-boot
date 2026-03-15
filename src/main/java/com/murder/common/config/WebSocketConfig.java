package com.murder.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.murder.websocket.NotificationWebSocketHandler;
import com.murder.websocket.AdminNotificationWebSocketHandler;
import com.murder.websocket.ServiceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Autowired
    private AdminNotificationWebSocketHandler adminNotificationWebSocketHandler;

    @Autowired
    private ServiceWebSocketHandler serviceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 用户端通知 WebSocket
        registry.addHandler(notificationWebSocketHandler, "/api/ws/notification")
                .setAllowedOrigins("*");

        // 管理端通知 WebSocket
        registry.addHandler(adminNotificationWebSocketHandler, "/api/ws/admin-notification")
                .setAllowedOrigins("*");

        // 客服专用双向通信 WebSocket
        registry.addHandler(serviceWebSocketHandler, "/api/ws/service")
                .setAllowedOrigins("*");
    }
}
