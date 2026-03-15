package com.murder.service;

import com.murder.dto.FeedbackDTO;
import com.murder.entity.Feedback;
import com.murder.mapper.FeedbackMapper;
import com.murder.service.impl.FeedbackServiceImpl;
import com.murder.vo.FeedbackVO;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 留言反馈服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("留言反馈服务测试")
class FeedbackServiceTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private Feedback testFeedback;
    private FeedbackDTO testFeedbackDTO;

    @BeforeEach
    void setUp() {
        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setUserId(1L);
        testFeedback.setName("张三");
        testFeedback.setContact("13800138000");
        testFeedback.setSubject("platform");
        testFeedback.setMessage("这是测试留言内容");
        testFeedback.setStatus(0); // 待处理
        testFeedback.setCreateTime(LocalDateTime.now());

        testFeedbackDTO = new FeedbackDTO();
        testFeedbackDTO.setName("张三");
        testFeedbackDTO.setContact("13800138000");
        testFeedbackDTO.setSubject("platform");
        testFeedbackDTO.setMessage("这是测试留言内容");
    }

    @Nested
    @DisplayName("提交留言测试")
    class SubmitTests {

        @Test
        @DisplayName("提交留言 - 登录用户成功")
        void submit_LoggedUser_Success() {
            when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            feedbackService.submit(testFeedbackDTO, 1L, "127.0.0.1");

            verify(feedbackMapper, times(1)).insert(feedbackCaptor.capture());

            Feedback saved = feedbackCaptor.getValue();
            assertEquals(1L, saved.getUserId());
            assertEquals("张三", saved.getName());
            assertEquals("13800138000", saved.getContact());
            assertEquals("platform", saved.getSubject());
            assertEquals("这是测试留言内容", saved.getMessage());
            assertEquals("127.0.0.1", saved.getIpAddress());
            assertEquals(0, saved.getStatus());
        }

        @Test
        @DisplayName("提交留言 - 游客成功（userId为null）")
        void submit_Guest_Success() {
            when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            feedbackService.submit(testFeedbackDTO, null, "192.168.1.1");

            verify(feedbackMapper, times(1)).insert(feedbackCaptor.capture());

            Feedback saved = feedbackCaptor.getValue();
            assertNull(saved.getUserId());
            assertEquals("张三", saved.getName());
            assertEquals("platform", saved.getSubject());
            assertEquals("13800138000", saved.getContact());
            assertEquals("这是测试留言内容", saved.getMessage());
            assertEquals("192.168.1.1", saved.getIpAddress());
            assertEquals(0, saved.getStatus());
        }

        @Test
        @DisplayName("提交留言 - 不同主题类型")
        void submit_DifferentSubjects() {
            when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            String[] subjects = {"platform", "booking", "account", "feedback", "business",
                    "game", "store", "script", "suggestion", "bug", "other"};

            for (String subject : subjects) {
                testFeedbackDTO.setSubject(subject);
                feedbackService.submit(testFeedbackDTO, 1L, "127.0.0.1");
            }

            verify(feedbackMapper, times(subjects.length)).insert(feedbackCaptor.capture());

            List<String> actualSubjects = feedbackCaptor.getAllValues().stream()
                    .map(Feedback::getSubject)
                    .toList();
            assertIterableEquals(Arrays.asList(subjects), actualSubjects);
            assertTrue(feedbackCaptor.getAllValues().stream().allMatch(feedback ->
                    Long.valueOf(1L).equals(feedback.getUserId())
                            && "127.0.0.1".equals(feedback.getIpAddress())
                            && feedback.getStatus() == 0));
        }
    }

    @Nested
    @DisplayName("查询留言测试")
    class QueryTests {

        @Test
        @DisplayName("根据ID查询留言 - 成功")
        void getById_Success() {
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);

            FeedbackVO result = feedbackService.getById(1L);

            assertNotNull(result);
            assertEquals("张三", result.getName());
            assertEquals("平台使用问题", result.getSubjectName());
            assertEquals("待处理", result.getStatusName());
        }

        @Test
        @DisplayName("根据ID查询留言 - 不存在")
        void getById_NotFound() {
            when(feedbackMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> feedbackService.getById(99L));
        }
    }

    @Nested
    @DisplayName("回复留言测试")
    class ReplyTests {

        @Test
        @DisplayName("回复留言 - 成功并推送WebSocket通知")
        void reply_Success_WithWebSocket() {
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);
            doNothing().when(webSocketHandler).pushNotification(anyLong(), anyMap());

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);

            feedbackService.reply(1L, "感谢您的反馈，我们已记录处理", 2L);

            verify(feedbackMapper, times(1)).updateById(feedbackCaptor.capture());
            verify(webSocketHandler, times(1)).pushNotification(eq(1L), notificationCaptor.capture());

            Feedback updated = feedbackCaptor.getValue();
            assertEquals("感谢您的反馈，我们已记录处理", updated.getReplyContent());
            assertEquals(2L, updated.getReplyUserId());
            assertEquals(2, updated.getStatus());
            assertNotNull(updated.getReplyTime());

            Map<String, Object> notification = notificationCaptor.getValue();
            assertEquals("feedback_reply", notification.get("type"));
            assertEquals("您的留言已收到回复", notification.get("title"));
            assertEquals("感谢您的反馈，我们已记录处理", notification.get("content"));
            assertEquals(1L, notification.get("feedbackId"));
            assertNotNull(notification.get("time"));
        }

        @Test
        @DisplayName("回复留言 - 游客留言不推送WebSocket")
        void reply_GuestFeedback_NoWebSocket() {
            testFeedback.setUserId(null);
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            feedbackService.reply(1L, "感谢反馈", 2L);

            verify(feedbackMapper, times(1)).updateById(feedbackCaptor.capture());
            verify(webSocketHandler, never()).pushNotification(anyLong(), anyMap());

            Feedback updated = feedbackCaptor.getValue();
            assertEquals("感谢反馈", updated.getReplyContent());
            assertEquals(2L, updated.getReplyUserId());
            assertEquals(2, updated.getStatus());
            assertNotNull(updated.getReplyTime());
        }

        @Test
        @DisplayName("回复留言 - 留言不存在")
        void reply_NotFound() {
            when(feedbackMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> feedbackService.reply(99L, "回复内容", 2L));
        }

        @Test
        @DisplayName("回复留言 - 长内容截断推送")
        void reply_LongContent_Truncated() {
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);
            doNothing().when(webSocketHandler).pushNotification(anyLong(), anyMap());

            String longReply = "这是一段超过50个字符的回复内容，用于测试推送通知时是否会正确截断显示，避免通知内容过长影响用户体验";
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);

            feedbackService.reply(1L, longReply, 2L);

            verify(webSocketHandler, times(1)).pushNotification(eq(1L), notificationCaptor.capture());
            String expectedContent = longReply.length() > 50
                    ? longReply.substring(0, 50) + "..."
                    : longReply;
            assertEquals(expectedContent, notificationCaptor.getValue().get("content"));
        }
    }

    @Nested
    @DisplayName("更新状态测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新留言状态 - 处理中")
        void updateStatus_Processing() {
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            feedbackService.updateStatus(1L, 1);

            verify(feedbackMapper, times(1)).updateById(feedbackCaptor.capture());
            assertEquals(1, feedbackCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("更新留言状态 - 已关闭")
        void updateStatus_Closed() {
            when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

            ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);

            feedbackService.updateStatus(1L, 3);

            verify(feedbackMapper, times(1)).updateById(feedbackCaptor.capture());
            assertEquals(3, feedbackCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("更新状态 - 留言不存在")
        void updateStatus_NotFound() {
            when(feedbackMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> feedbackService.updateStatus(99L, 1));
        }
    }

    @Nested
    @DisplayName("删除留言测试")
    class DeleteTests {

        @Test
        @DisplayName("删除留言 - 成功")
        void delete_Success() {
            when(feedbackMapper.deleteById(1L)).thenReturn(1);

            feedbackService.delete(1L);

            verify(feedbackMapper, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("主题名称转换测试")
    class SubjectNameTests {

        @Test
        @DisplayName("各主题类型名称转换正确")
        void subjectName_Correct() {
            String[][] testCases = {
                {"platform", "平台使用问题"},
                {"booking", "预约相关问题"},
                {"account", "账号相关问题"},
                {"feedback", "建议与反馈"},
                {"business", "商务合作"},
                {"game", "游戏咨询"},
                {"store", "门店合作"},
                {"script", "剧本投稿"},
                {"suggestion", "功能建议"},
                {"bug", "问题反馈"},
                {"other", "其他问题"}
            };

            for (String[] tc : testCases) {
                testFeedback.setSubject(tc[0]);
                when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
                FeedbackVO vo = feedbackService.getById(1L);
                assertEquals(tc[1], vo.getSubjectName(), "主题 " + tc[0] + " 名称应为 " + tc[1]);
            }
        }

        @Test
        @DisplayName("状态名称转换正确")
        void statusName_Correct() {
            int[] statuses = {0, 1, 2, 3};
            String[] names = {"待处理", "处理中", "已回复", "已关闭"};

            for (int i = 0; i < statuses.length; i++) {
                testFeedback.setStatus(statuses[i]);
                when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
                FeedbackVO vo = feedbackService.getById(1L);
                assertEquals(names[i], vo.getStatusName(), "状态 " + statuses[i] + " 名称应为 " + names[i]);
            }
        }
    }
}
