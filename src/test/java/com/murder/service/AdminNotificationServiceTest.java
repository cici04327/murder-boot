package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.AdminNotification;
import com.murder.mapper.AdminNotificationMapper;
import com.murder.service.impl.AdminNotificationServiceImpl;
import com.murder.vo.AdminNotificationVO;
import com.murder.websocket.AdminNotificationWebSocketHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 管理端通知服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("管理端通知服务测试")
class AdminNotificationServiceTest {

    @Mock
    private AdminNotificationMapper adminNotificationMapper;

    @Mock
    private AdminNotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private AdminNotificationServiceImpl adminNotificationService;

    private AdminNotification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new AdminNotification();
        testNotification.setId(1L);
        testNotification.setTitle("新预约通知");
        testNotification.setContent("用户张三提交了新的预约");
        testNotification.setType(1); // 新预约
        testNotification.setBizType("reservation");
        testNotification.setBizId(100L);
        testNotification.setStoreId(1L);
        testNotification.setPriority(2); // 中优先级
        testNotification.setIsRead(0); // 未读
        testNotification.setIsDeleted(0);
        testNotification.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("发送通知测试")
    class SendNotificationTests {

        @Test
        @DisplayName("发送管理端通知 - 成功")
        void testSendNotification_Success() {
            when(adminNotificationMapper.insert(any(AdminNotification.class))).thenAnswer(invocation -> {
                AdminNotification notification = invocation.getArgument(0);
                notification.setId(10L);
                return 1;
            });
            doNothing().when(webSocketHandler).pushNotificationToAll(anyMap());

            ArgumentCaptor<AdminNotification> notificationCaptor = ArgumentCaptor.forClass(AdminNotification.class);
            ArgumentCaptor<Map<String, Object>> wsCaptor = ArgumentCaptor.forClass(Map.class);

            adminNotificationService.sendNotification("新预约", "用户提交预约", 1, "reservation", 100L, 1L, 2);

            verify(adminNotificationMapper, times(1)).insert(notificationCaptor.capture());
            verify(webSocketHandler, times(1)).pushNotificationToAll(wsCaptor.capture());

            AdminNotification saved = notificationCaptor.getValue();
            assertEquals("新预约", saved.getTitle());
            assertEquals("用户提交预约", saved.getContent());
            assertEquals(1, saved.getType());
            assertEquals("reservation", saved.getBizType());
            assertEquals(100L, saved.getBizId());
            assertEquals(1L, saved.getStoreId());
            assertEquals(2, saved.getPriority());
            assertEquals(0, saved.getIsRead());
            assertEquals(1, saved.getTargetType());
            assertEquals(2, saved.getStatus());
            assertEquals(0, saved.getIsDeleted());
            assertNotNull(saved.getSendTime());

            Map<String, Object> wsMessage = wsCaptor.getValue();
            assertEquals(10L, wsMessage.get("id"));
            assertEquals("新预约", wsMessage.get("title"));
            assertEquals("用户提交预约", wsMessage.get("content"));
            assertEquals("reservation", wsMessage.get("bizType"));
            assertEquals(100L, wsMessage.get("bizId"));
            assertEquals(false, wsMessage.get("isRead"));
        }

        @Test
        @DisplayName("发送通知 - storeId为null时全平台通知")
        void testSendNotification_NullStoreId() {
            when(adminNotificationMapper.insert(any(AdminNotification.class))).thenAnswer(invocation -> {
                AdminNotification notification = invocation.getArgument(0);
                notification.setId(11L);
                return 1;
            });
            doNothing().when(webSocketHandler).pushNotificationToAll(anyMap());

            ArgumentCaptor<AdminNotification> notificationCaptor = ArgumentCaptor.forClass(AdminNotification.class);

            adminNotificationService.sendNotification("系统通知", "系统升级", 4, "system", null, null, 1);

            verify(adminNotificationMapper, times(1)).insert(notificationCaptor.capture());

            AdminNotification saved = notificationCaptor.getValue();
            assertNull(saved.getStoreId());
            assertNull(saved.getBizId());
            assertEquals("系统通知", saved.getTitle());
            assertEquals("system", saved.getBizType());
            assertEquals(1, saved.getPriority());
        }
    }

    @Nested
    @DisplayName("查询通知测试")
    class GetNotificationsTests {

        @Test
        @DisplayName("分页查询通知 - 按门店过滤")
        void testGetNotifications_ByStoreId() {
            when(adminNotificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(adminNotificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<AdminNotification> p = inv.getArgument(0);
                        p.setRecords(Collections.singletonList(testNotification));
                        return p;
                    });

            PageResult<AdminNotificationVO> result = adminNotificationService.getNotifications(
                    1, 10, 1L, null, false);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("分页查询通知 - 只查未读")
        void testGetNotifications_OnlyUnread() {
            when(adminNotificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(adminNotificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<AdminNotification> p = inv.getArgument(0);
                        p.setRecords(Collections.singletonList(testNotification));
                        return p;
                    });

            PageResult<AdminNotificationVO> result = adminNotificationService.getNotifications(
                    1, 10, null, null, true);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("获取通知详情 - 存在")
        void testGetNotificationDetail_Found() {
            when(adminNotificationMapper.selectById(1L)).thenReturn(testNotification);

            AdminNotificationVO result = adminNotificationService.getNotificationDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("新预约通知", result.getTitle());
        }

        @Test
        @DisplayName("获取通知详情 - 不存在")
        void testGetNotificationDetail_NotFound() {
            when(adminNotificationMapper.selectById(999L)).thenReturn(null);

            AdminNotificationVO result = adminNotificationService.getNotificationDetail(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("标记已读测试")
    class MarkReadTests {

        @Test
        @DisplayName("标记单条通知已读 - 成功")
        void testMarkAsRead_Success() {
            when(adminNotificationMapper.selectById(1L)).thenReturn(testNotification);
            when(adminNotificationMapper.updateById(any(AdminNotification.class))).thenReturn(1);

            ArgumentCaptor<AdminNotification> notificationCaptor = ArgumentCaptor.forClass(AdminNotification.class);

            adminNotificationService.markAsRead(1L);

            verify(adminNotificationMapper, times(1)).updateById(notificationCaptor.capture());

            AdminNotification updated = notificationCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals(1, updated.getIsRead());
            assertNotNull(updated.getReadTime());
        }

        @Test
        @DisplayName("标记所有通知已读 - 成功")
        void testMarkAllAsRead_Success() {
            AdminNotification anotherNotification = new AdminNotification();
            anotherNotification.setId(2L);
            anotherNotification.setIsRead(0);
            when(adminNotificationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(testNotification, anotherNotification));
            when(adminNotificationMapper.updateById(any(AdminNotification.class))).thenReturn(1);

            ArgumentCaptor<AdminNotification> notificationCaptor = ArgumentCaptor.forClass(AdminNotification.class);

            adminNotificationService.markAllAsRead(1L);

            verify(adminNotificationMapper, times(2)).updateById(notificationCaptor.capture());
            assertEquals(Arrays.asList(1L, 2L), notificationCaptor.getAllValues().stream()
                    .map(AdminNotification::getId)
                    .toList());
            assertTrue(notificationCaptor.getAllValues().stream().allMatch(notification ->
                    Integer.valueOf(1).equals(notification.getIsRead()) && notification.getReadTime() != null));
        }
    }

    @Nested
    @DisplayName("获取未读数量测试")
    class GetUnreadCountTests {

        @Test
        @DisplayName("获取未读数量 - 按门店")
        void testGetUnreadCount_ByStoreId() {
            when(adminNotificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            Long count = adminNotificationService.getUnreadCount(1L);

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("获取未读数量 - 全平台")
        void testGetUnreadCount_AllStores() {
            when(adminNotificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

            Long count = adminNotificationService.getUnreadCount(null);

            assertEquals(10L, count);
        }
    }

    @Nested
    @DisplayName("删除通知测试")
    class DeleteTests {

        @Test
        @DisplayName("删除单条通知 - 成功")
        void testDeleteNotification_Success() {
            when(adminNotificationMapper.deleteById(1L)).thenReturn(1);

            adminNotificationService.deleteNotification(1L);
            verify(adminNotificationMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("批量删除通知 - 成功")
        void testBatchDeleteNotifications_Success() {
            when(adminNotificationMapper.deleteBatchIds(anyList())).thenReturn(3);

            List<Long> ids = Arrays.asList(1L, 2L, 3L);
            adminNotificationService.batchDeleteNotifications(ids);
            verify(adminNotificationMapper, times(1)).deleteBatchIds(ids);
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取通知统计 - 成功")
        void testGetStatistics_Success() {
            when(adminNotificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 3L, 2L);
            when(adminNotificationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(testNotification));

            Map<String, Object> stats = adminNotificationService.getStatistics(1L);

            assertNotNull(stats);
        }
    }
}
