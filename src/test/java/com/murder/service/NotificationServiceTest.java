package com.murder.service;

import com.murder.entity.SystemNotification;
import com.murder.entity.UserNotification;
import com.murder.mapper.SystemNotificationMapper;
import com.murder.mapper.UserNotificationMapper;
import com.murder.service.impl.NotificationServiceImpl;
import com.murder.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 通知服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("通知服务测试")
class NotificationServiceTest {

    @Mock
    private SystemNotificationMapper systemNotificationMapper;

    @Mock
    private UserNotificationMapper userNotificationMapper;

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private SystemNotification testSystemNotification;
    private UserNotification testUserNotification;

    @BeforeEach
    void setUp() {
        testSystemNotification = new SystemNotification();
        testSystemNotification.setId(1L);
        testSystemNotification.setTitle("测试通知");
        testSystemNotification.setContent("这是测试通知内容");
        testSystemNotification.setType(1);
        testSystemNotification.setBizType("test");
        testSystemNotification.setBizId(1L);
        testSystemNotification.setTargetType(2);
        testSystemNotification.setStatus(2);
        testSystemNotification.setSendTime(LocalDateTime.now());
        testSystemNotification.setIsDeleted(0);

        testUserNotification = new UserNotification();
        testUserNotification.setId(1L);
        testUserNotification.setUserId(1L);
        testUserNotification.setNotificationId(1L);
        testUserNotification.setIsRead(0);
        testUserNotification.setIsDeleted(0);
        testUserNotification.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("发送通知测试")
    class SendNotificationTests {

        @Test
        @DisplayName("发送给指定用户 - 成功")
        void sendToUsers_Success() {
            when(systemNotificationMapper.insert(any(SystemNotification.class))).thenAnswer(invocation -> {
                SystemNotification notification = invocation.getArgument(0);
                notification.setId(10L);
                return 1;
            });
            AtomicLong userNotificationId = new AtomicLong(100L);
            when(userNotificationMapper.insert(any(UserNotification.class))).thenAnswer(invocation -> {
                UserNotification userNotification = invocation.getArgument(0);
                userNotification.setId(userNotificationId.getAndIncrement());
                userNotification.setCreateTime(LocalDateTime.now());
                return 1;
            });
            doNothing().when(webSocketHandler).pushNotification(anyLong(), anyMap());

            ArgumentCaptor<SystemNotification> systemCaptor = ArgumentCaptor.forClass(SystemNotification.class);
            ArgumentCaptor<UserNotification> userCaptor = ArgumentCaptor.forClass(UserNotification.class);
            ArgumentCaptor<Map<String, Object>> wsCaptor = ArgumentCaptor.forClass(Map.class);

            notificationService.sendToUsers("标题", "内容", 1, "test", 1L, 1L, 2L);

            verify(systemNotificationMapper, times(1)).insert(systemCaptor.capture());
            verify(userNotificationMapper, times(2)).insert(userCaptor.capture());
            verify(webSocketHandler, times(2)).pushNotification(anyLong(), wsCaptor.capture());

            SystemNotification notification = systemCaptor.getValue();
            assertEquals("标题", notification.getTitle());
            assertEquals("内容", notification.getContent());
            assertEquals(1, notification.getType());
            assertEquals("test", notification.getBizType());
            assertEquals(1L, notification.getBizId());
            assertEquals(2, notification.getTargetType());
            assertEquals("1,2", notification.getTargetUsers());
            assertEquals(2, notification.getStatus());
            assertEquals(0, notification.getIsDeleted());
            assertNotNull(notification.getSendTime());

            List<UserNotification> userNotifications = userCaptor.getAllValues();
            assertEquals(Arrays.asList(1L, 2L), userNotifications.stream()
                    .map(UserNotification::getUserId)
                    .toList());
            assertTrue(userNotifications.stream().allMatch(userNotification ->
                    Long.valueOf(10L).equals(userNotification.getNotificationId())
                            && Integer.valueOf(0).equals(userNotification.getIsRead())
                            && Integer.valueOf(0).equals(userNotification.getIsDeleted())));

            assertEquals("标题", wsCaptor.getAllValues().get(0).get("title"));
            assertEquals("内容", wsCaptor.getAllValues().get(0).get("content"));
            assertEquals("test", wsCaptor.getAllValues().get(0).get("bizType"));
            assertEquals(1L, wsCaptor.getAllValues().get(0).get("bizId"));
            assertEquals(false, wsCaptor.getAllValues().get(0).get("isRead"));
        }

        @Test
        @DisplayName("发送给指定用户 - userIds为空时不发送")
        void sendToUsers_EmptyUserIds() {
            notificationService.sendToUsers("标题", "内容", 1, "test", 1L);

            verify(systemNotificationMapper, never()).insert(any());
        }

        @Test
        @DisplayName("发送给单个用户")
        void sendToUsers_SingleUser() {
            when(systemNotificationMapper.insert(any(SystemNotification.class))).thenAnswer(invocation -> {
                SystemNotification notification = invocation.getArgument(0);
                notification.setId(11L);
                return 1;
            });
            when(userNotificationMapper.insert(any(UserNotification.class))).thenAnswer(invocation -> {
                UserNotification userNotification = invocation.getArgument(0);
                userNotification.setId(101L);
                userNotification.setCreateTime(LocalDateTime.now());
                return 1;
            });
            doNothing().when(webSocketHandler).pushNotification(anyLong(), anyMap());

            ArgumentCaptor<UserNotification> userCaptor = ArgumentCaptor.forClass(UserNotification.class);
            ArgumentCaptor<Map<String, Object>> wsCaptor = ArgumentCaptor.forClass(Map.class);

            notificationService.sendToUsers("支付成功", "订单已支付", 3, "payment", 1L, 1L);

            verify(userNotificationMapper, times(1)).insert(userCaptor.capture());
            verify(webSocketHandler, times(1)).pushNotification(eq(1L), wsCaptor.capture());

            UserNotification userNotification = userCaptor.getValue();
            assertEquals(1L, userNotification.getUserId());
            assertEquals(11L, userNotification.getNotificationId());
            assertEquals(0, userNotification.getIsRead());
            assertEquals(0, userNotification.getIsDeleted());

            Map<String, Object> wsMessage = wsCaptor.getValue();
            assertEquals("支付成功", wsMessage.get("title"));
            assertEquals("订单已支付", wsMessage.get("content"));
            assertEquals("payment", wsMessage.get("bizType"));
            assertEquals(1L, wsMessage.get("bizId"));
            assertEquals(false, wsMessage.get("isRead"));
        }

        @Test
        @DisplayName("发送给全体用户")
        void sendToAll_Success() {
            when(systemNotificationMapper.insert(any(SystemNotification.class))).thenAnswer(invocation -> {
                SystemNotification notification = invocation.getArgument(0);
                notification.setId(12L);
                return 1;
            });
            doNothing().when(webSocketHandler).pushNotificationToAll(anyMap());

            ArgumentCaptor<SystemNotification> systemCaptor = ArgumentCaptor.forClass(SystemNotification.class);
            ArgumentCaptor<Map<String, Object>> wsCaptor = ArgumentCaptor.forClass(Map.class);

            notificationService.sendToAll("全体公告", "系统维护通知", 1, "system", null);

            verify(systemNotificationMapper, times(1)).insert(systemCaptor.capture());
            verify(webSocketHandler, times(1)).pushNotificationToAll(wsCaptor.capture());

            SystemNotification notification = systemCaptor.getValue();
            assertEquals("全体公告", notification.getTitle());
            assertEquals("系统维护通知", notification.getContent());
            assertEquals(1, notification.getTargetType());
            assertEquals(2, notification.getStatus());
            assertEquals(0, notification.getIsDeleted());
            assertNotNull(notification.getSendTime());

            Map<String, Object> wsMessage = wsCaptor.getValue();
            assertEquals(12L, wsMessage.get("id"));
            assertEquals("全体公告", wsMessage.get("title"));
            assertEquals("系统维护通知", wsMessage.get("content"));
            assertEquals("system", wsMessage.get("bizType"));
            assertNull(wsMessage.get("bizId"));
            assertEquals(false, wsMessage.get("isRead"));
        }
    }

    @Nested
    @DisplayName("标记已读测试")
    class MarkReadTests {

        @Test
        @DisplayName("标记单条通知为已读")
        void markAsRead_Success() {
            when(userNotificationMapper.selectOne(any())).thenReturn(testUserNotification);
            when(userNotificationMapper.updateById(any(UserNotification.class))).thenReturn(1);

            ArgumentCaptor<UserNotification> userCaptor = ArgumentCaptor.forClass(UserNotification.class);

            notificationService.markAsRead(1L, 1L);

            verify(userNotificationMapper, times(1)).updateById(userCaptor.capture());

            UserNotification updated = userCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals(1L, updated.getUserId());
            assertEquals(1, updated.getIsRead());
            assertNotNull(updated.getReadTime());
        }

        @Test
        @DisplayName("标记已读 - 通知不存在时不报错")
        void markAsRead_NotFound() {
            when(userNotificationMapper.selectOne(any())).thenReturn(null);

            notificationService.markAsRead(1L, 99L);

            verify(userNotificationMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("标记已读 - 已经是已读状态不重复更新")
        void markAsRead_AlreadyRead() {
            testUserNotification.setIsRead(1);
            when(userNotificationMapper.selectOne(any())).thenReturn(testUserNotification);

            notificationService.markAsRead(1L, 1L);

            verify(userNotificationMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("标记全部通知为已读")
        void markAllAsRead_Success() {
            UserNotification unread1 = new UserNotification();
            unread1.setId(1L);
            unread1.setUserId(1L);
            unread1.setIsRead(0);

            UserNotification unread2 = new UserNotification();
            unread2.setId(2L);
            unread2.setUserId(1L);
            unread2.setIsRead(0);

            when(userNotificationMapper.selectList(any())).thenReturn(Arrays.asList(unread1, unread2));
            when(userNotificationMapper.updateById(any(UserNotification.class))).thenReturn(1);

            ArgumentCaptor<UserNotification> userCaptor = ArgumentCaptor.forClass(UserNotification.class);

            notificationService.markAllAsRead(1L);

            verify(userNotificationMapper, times(2)).updateById(userCaptor.capture());

            assertEquals(Arrays.asList(1L, 2L), userCaptor.getAllValues().stream()
                    .map(UserNotification::getId)
                    .toList());
            assertTrue(userCaptor.getAllValues().stream().allMatch(notification ->
                    Integer.valueOf(1).equals(notification.getIsRead()) && notification.getReadTime() != null));
        }

        @Test
        @DisplayName("标记全部已读 - 无未读通知")
        void markAllAsRead_NoUnread() {
            when(userNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

            notificationService.markAllAsRead(1L);

            verify(userNotificationMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("获取未读数量测试")
    class UnreadCountTests {

        @Test
        @DisplayName("获取未读通知数量")
        void getUnreadCount_Success() {
            when(userNotificationMapper.countUnreadByUserId(1L)).thenReturn(5L);

            Long count = notificationService.getUnreadCount(1L);

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("获取未读数量 - 全部已读返回0")
        void getUnreadCount_Zero() {
            when(userNotificationMapper.countUnreadByUserId(1L)).thenReturn(0L);

            Long count = notificationService.getUnreadCount(1L);

            assertEquals(0L, count);
        }
    }

    @Nested
    @DisplayName("删除通知测试")
    class DeleteNotificationTests {

        @Test
        @DisplayName("删除单条通知")
        void deleteNotification_Success() {
            when(userNotificationMapper.delete(any())).thenReturn(1);

            notificationService.deleteNotification(1L, 1L);

            verify(userNotificationMapper, times(1)).delete(any());
        }

        @Test
        @DisplayName("批量删除通知")
        void batchDeleteNotifications_Success() {
            when(userNotificationMapper.delete(any())).thenReturn(2);

            List<Long> ids = Arrays.asList(1L, 2L, 3L);
            notificationService.batchDeleteNotifications(1L, ids);

            verify(userNotificationMapper, times(1)).delete(any());
        }
    }
}
