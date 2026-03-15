package com.murder.service;

import com.murder.entity.ServiceMessage;
import com.murder.entity.ServiceSession;
import com.murder.mapper.ServiceMessageMapper;
import com.murder.mapper.ServiceSessionMapper;
import com.murder.service.impl.ServiceSessionServiceImpl;
import com.murder.websocket.ServiceWebSocketHandler;
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
 * 客服会话服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("客服会话服务测试")
class ServiceSessionServiceTest {

    @Mock
    private ServiceSessionMapper sessionMapper;

    @Mock
    private ServiceMessageMapper messageMapper;

    @Mock
    private ServiceWebSocketHandler webSocketHandler;

    @InjectMocks
    private ServiceSessionServiceImpl sessionService;

    private ServiceSession testSession;
    private ServiceMessage testMessage;

    @BeforeEach
    void setUp() {
        testSession = new ServiceSession();
        testSession.setId(1L);
        testSession.setUserId(1L);
        testSession.setUserName("测试用户");
        testSession.setStatus(0); // 0=等待中
        testSession.setCreateTime(LocalDateTime.now());
        testSession.setUpdateTime(LocalDateTime.now());

        testMessage = new ServiceMessage();
        testMessage.setId(1L);
        testMessage.setSessionId(1L);
        testMessage.setSenderType("user");
        testMessage.setSenderId(1L);
        testMessage.setContent("您好，我有问题需要咨询");
        testMessage.setMsgType("text");
        testMessage.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("创建会话测试")
    class CreateSessionTests {

        @Test
        @DisplayName("创建会话 - 成功")
        void testCreateSession_Success() {
            when(sessionMapper.insert(any(ServiceSession.class))).thenAnswer(inv -> {
                ServiceSession s = inv.getArgument(0);
                s.setId(1L);
                return 1;
            });
            when(messageMapper.insert(any(ServiceMessage.class))).thenReturn(1);
            doNothing().when(webSocketHandler).notifyAdmins(anyMap());

            Long sessionId = sessionService.createSession(1L, "测试用户", "我需要帮助");

            assertNotNull(sessionId);
            assertEquals(1L, sessionId);
            verify(sessionMapper, times(1)).insert(any(ServiceSession.class));
            verify(webSocketHandler, times(1)).notifyAdmins(anyMap());
        }

        @Test
        @DisplayName("创建会话 - 初始问题可以为空")
        void testCreateSession_NullQuestion() {
            when(sessionMapper.insert(any(ServiceSession.class))).thenAnswer(inv -> {
                ServiceSession s = inv.getArgument(0);
                s.setId(2L);
                return 1;
            });
            when(messageMapper.insert(any(ServiceMessage.class))).thenReturn(1);
            doNothing().when(webSocketHandler).notifyAdmins(anyMap());

            Long sessionId = sessionService.createSession(1L, "用户", null);

            assertNotNull(sessionId);
        }
    }

    @Nested
    @DisplayName("接入会话测试")
    class AcceptSessionTests {

        @Test
        @DisplayName("管理员接入会话 - 成功")
        void testAcceptSession_Success() {
            testSession.setStatus(0); // 等待中
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(ServiceSession.class))).thenReturn(1);
            when(messageMapper.insert(any(ServiceMessage.class))).thenReturn(1);
            doNothing().when(webSocketHandler).sendToUser(anyLong(), anyMap());

            assertDoesNotThrow(() -> sessionService.acceptSession(1L, 10L));
            verify(sessionMapper, times(1)).updateById(argThat(s ->
                    s.getId().equals(1L) && s.getStatus() == 1 // 1=进行中
            ));
        }

        @Test
        @DisplayName("接入会话 - 会话不存在时直接返回")
        void testAcceptSession_NotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> sessionService.acceptSession(999L, 10L));
            verify(sessionMapper, never()).updateById(any(ServiceSession.class));
        }
    }

    @Nested
    @DisplayName("发送消息测试")
    class SendMessageTests {

        @Test
        @DisplayName("用户发送消息 - 成功")
        void testSendMessage_UserSuccess() {
            testSession.setStatus(1); // 进行中
            testSession.setAdminId(10L);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(messageMapper.insert(any(ServiceMessage.class))).thenAnswer(inv -> {
                ServiceMessage m = inv.getArgument(0);
                m.setId(1L);
                return 1;
            });
            doNothing().when(webSocketHandler).sendToAdmin(anyLong(), anyMap());

            ServiceMessage result = sessionService.sendMessage(1L, "user", 1L, "你好", "text");

            assertNotNull(result);
            assertEquals("user", result.getSenderType());
            assertEquals("你好", result.getContent());
            verify(webSocketHandler, times(1)).sendToAdmin(eq(10L), anyMap());
        }

        @Test
        @DisplayName("管理员发送消息 - 成功")
        void testSendMessage_AdminSuccess() {
            testSession.setStatus(1);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(messageMapper.insert(any(ServiceMessage.class))).thenAnswer(inv -> {
                ServiceMessage m = inv.getArgument(0);
                m.setId(2L);
                return 1;
            });
            doNothing().when(webSocketHandler).sendToUser(anyLong(), anyMap());

            ServiceMessage result = sessionService.sendMessage(1L, "admin", 10L, "您好，请问有什么可以帮您？", "text");

            assertNotNull(result);
            assertEquals("admin", result.getSenderType());
            verify(webSocketHandler, times(1)).sendToUser(eq(1L), anyMap());
        }

        @Test
        @DisplayName("发送消息 - 会话不存在抛出异常")
        void testSendMessage_SessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class,
                    () -> sessionService.sendMessage(999L, "user", 1L, "消息", "text"));
        }
    }

    @Nested
    @DisplayName("关闭会话测试")
    class CloseSessionTests {

        @Test
        @DisplayName("关闭会话 - 用户关闭")
        void testCloseSession_ByUser() {
            testSession.setStatus(1);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(ServiceSession.class))).thenReturn(1);
            when(messageMapper.insert(any(ServiceMessage.class))).thenReturn(1);
            doNothing().when(webSocketHandler).sendToUser(anyLong(), anyMap());

            assertDoesNotThrow(() -> sessionService.closeSession(1L, "user"));
            verify(sessionMapper, times(1)).updateById(argThat(s ->
                    s.getId().equals(1L) && s.getStatus() == 2 // 2=已关闭
            ));
        }

        @Test
        @DisplayName("关闭会话 - 管理员关闭")
        void testCloseSession_ByAdmin() {
            testSession.setStatus(1);
            testSession.setAdminId(10L);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(ServiceSession.class))).thenReturn(1);
            when(messageMapper.insert(any(ServiceMessage.class))).thenReturn(1);
            doNothing().when(webSocketHandler).sendToUser(anyLong(), anyMap());
            doNothing().when(webSocketHandler).sendToAdmin(anyLong(), anyMap());

            assertDoesNotThrow(() -> sessionService.closeSession(1L, "admin"));
        }
    }

    @Nested
    @DisplayName("评价会话测试")
    class RateSessionTests {

        @Test
        @DisplayName("用户评价会话 - 成功")
        void testRateSession_Success() {
            testSession.setStatus(2); // 已关闭
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(ServiceSession.class))).thenReturn(1);

            assertDoesNotThrow(() -> sessionService.rateSession(1L, 5, "服务很好"));
            verify(sessionMapper, times(1)).updateById(any(ServiceSession.class));
        }
    }

    @Nested
    @DisplayName("查询消息列表测试")
    class GetMessagesTests {

        @Test
        @DisplayName("获取会话消息 - 有数据")
        void testGetMessages_WithData() {
            when(messageMapper.listBySessionId(1L)).thenReturn(Arrays.asList(testMessage));

            List<ServiceMessage> result = sessionService.getMessages(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("您好，我有问题需要咨询", result.get(0).getContent());
        }

        @Test
        @DisplayName("获取会话消息 - 无数据")
        void testGetMessages_Empty() {
            when(messageMapper.listBySessionId(1L)).thenReturn(Collections.emptyList());

            List<ServiceMessage> result = sessionService.getMessages(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("其他查询测试")
    class OtherQueryTests {

        @Test
        @DisplayName("获取等待中会话数量")
        void testCountWaiting() {
            when(sessionMapper.countWaiting()).thenReturn(3);

            int count = sessionService.countWaiting();

            assertEquals(3, count);
        }

        @Test
        @DisplayName("按用户ID获取进行中的会话")
        void testGetActiveSessionByUserId_Found() {
            when(sessionMapper.selectOne(any())).thenReturn(testSession);

            ServiceSession result = sessionService.getActiveSessionByUserId(1L);

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
        }

        @Test
        @DisplayName("按用户ID获取进行中的会话 - 无会话")
        void testGetActiveSessionByUserId_NotFound() {
            when(sessionMapper.selectOne(any())).thenReturn(null);

            ServiceSession result = sessionService.getActiveSessionByUserId(1L);

            assertNull(result);
        }

        @Test
        @DisplayName("分页查询会话列表")
        void testListSessions() {
            when(sessionMapper.pageWithUserInfo(any(), isNull())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<ServiceSession> p = inv.getArgument(0);
                p.setRecords(Collections.singletonList(testSession));
                p.setTotal(1);
                return p;
            });

            Map<String, Object> result = sessionService.listSessions(null, 1, 10);

            assertNotNull(result);
            assertTrue(result.containsKey("total") || result.containsKey("records") || !result.isEmpty());
        }
    }
}
