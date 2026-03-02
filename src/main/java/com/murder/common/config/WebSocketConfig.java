package com.murder.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.murder.websocket.NotificationWebSocketHandler;
import com.murder.websocket.AdminNotificationWebSocketHandler;
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器，允许跨域
        // 注意：前端通过 /api/ws/notification 访问，Vite代理会转发到后端
        registry.addHandler(notificationWebSocketHandler, "/api/ws/notification")
                .setAllowedOrigins("*");

        // 管理端通知 WebSocket
        registry.addHandler(adminNotificationWebSocketHandler, "/api/ws/admin-notification")
                .setAllowedOrigins("*");
    }
}
