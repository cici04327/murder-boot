package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Feedback;
import com.murder.mapper.FeedbackMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 留言反馈模块集成测试
 */
@DisplayName("留言反馈模块集成测试")
class FeedbackIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Nested
    @DisplayName("提交留言测试")
    class SubmitTests {

        @Test
        @DisplayName("提交留言 - 登录用户成功")
        void submit_LoggedUser_Success() throws Exception {
            String message = "这是一条登录用户提交的测试留言，内容超过10个字符";
            String requestBody = "{"
                    + "\"name\": \"张三\","
                    + "\"contact\": \"13800138000\","
                    + "\"subject\": \"platform\","
                    + "\"message\": \"" + message + "\""
                    + "}";

            mockMvc.perform(post("/api/feedback/submit")
                            .header("token", testUserToken)
                            .header("X-Forwarded-For", "203.0.113.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("留言提交成功，我们会尽快处理！"));

            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Feedback::getUserId, 1L)
                    .eq(Feedback::getName, "张三")
                    .eq(Feedback::getSubject, "platform")
                    .eq(Feedback::getMessage, message);
            Feedback saved = feedbackMapper.selectOne(wrapper);

            assertNotNull(saved);
            assertEquals(0, saved.getStatus());
            assertEquals("203.0.113.10", saved.getIpAddress());
        }

        @Test
        @DisplayName("提交留言 - 游客成功")
        void submit_Guest_Success() throws Exception {
            String message = "这是来自游客的留言内容，超过10个字";
            String requestBody = "{"
                    + "\"name\": \"匿名用户\","
                    + "\"contact\": \"test@example.com\","
                    + "\"subject\": \"feedback\","
                    + "\"message\": \"" + message + "\""
                    + "}";

            mockMvc.perform(post("/api/feedback/submit")
                            .header("X-Forwarded-For", "203.0.113.11")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("留言提交成功，我们会尽快处理！"));

            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(Feedback::getUserId)
                    .eq(Feedback::getName, "匿名用户")
                    .eq(Feedback::getSubject, "feedback")
                    .eq(Feedback::getMessage, message);
            Feedback saved = feedbackMapper.selectOne(wrapper);

            assertNotNull(saved);
            assertNull(saved.getUserId());
            assertEquals(0, saved.getStatus());
            assertEquals("203.0.113.11", saved.getIpAddress());
        }

        @Test
        @DisplayName("提交留言 - 内容为空失败")
        void submit_EmptyMessage_Fail() throws Exception {
            String requestBody = "{"
                    + "\"name\": \"张三\","
                    + "\"contact\": \"13800138000\","
                    + "\"subject\": \"platform\","
                    + "\"message\": \"\""
                    + "}";

            mockMvc.perform(post("/api/feedback/submit")
                            .header("token", testUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)));
        }

        @Test
        @DisplayName("提交留言 - 不同主题类型")
        void submit_DifferentSubjects() throws Exception {
            String[] subjects = {"platform", "booking", "account", "game", "store", "script", "other"};

            for (String subject : subjects) {
                String requestBody = "{"
                        + "\"name\": \"测试用户\","
                        + "\"contact\": \"13800138000\","
                        + "\"subject\": \"" + subject + "\","
                        + "\"message\": \"这是测试留言，内容超过十个字符\""
                        + "}";

                mockMvc.perform(post("/api/feedback/submit")
                                .header("token", testUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
            }
        }
    }

    @Nested
    @DisplayName("查询留言测试")
    class QueryTests {

        @Test
        @DisplayName("管理端分页查询留言列表")
        void pageQuery_Admin_Success() throws Exception {
            mockMvc.perform(get("/api/feedback/page")
                            .header("token", adminToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("查询用户自己的留言")
        void getUserFeedbacks_Success() throws Exception {
            mockMvc.perform(get("/api/feedback/user")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(1))
                    .andExpect(jsonPath("$.data.records[0].subject").value("platform"))
                    .andExpect(jsonPath("$.data.records[0].subjectName").value("平台使用问题"));
        }

        @Test
        @DisplayName("查询留言详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/feedback/1")
                            .header("token", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("测试用户"))
                    .andExpect(jsonPath("$.data.subject").value("platform"))
                    .andExpect(jsonPath("$.data.subjectName").value("平台使用问题"))
                    .andExpect(jsonPath("$.data.status").value(0))
                    .andExpect(jsonPath("$.data.statusName").value("待处理"));
        }

        @Test
        @DisplayName("按状态筛选留言")
        void pageQuery_ByStatus() throws Exception {
            mockMvc.perform(get("/api/feedback/page")
                            .header("token", adminToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(2));
        }
    }

    @Nested
    @DisplayName("回复留言测试")
    class ReplyTests {

        @Test
        @DisplayName("管理员回复留言 - 成功")
        void reply_Success() throws Exception {
            mockMvc.perform(put("/api/feedback/1/reply")
                            .header("token", adminToken)
                            .param("replyContent", "感谢您的反馈，我们已经安排处理"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("回复成功"));

            Feedback updated = feedbackMapper.selectById(1L);
            assertNotNull(updated);
            assertEquals("感谢您的反馈，我们已经安排处理", updated.getReplyContent());
            assertEquals(2L, updated.getReplyUserId());
            assertEquals(2, updated.getStatus());
            assertNotNull(updated.getReplyTime());
        }

        @Test
        @DisplayName("管理员回复留言 - 未登录")
        void reply_Unauthorized() throws Exception {
            mockMvc.perform(put("/api/feedback/1/reply")
                            .param("replyContent", "感谢您的反馈"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("更新状态测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新留言状态 - 成功")
        void updateStatus_Success() throws Exception {
            mockMvc.perform(put("/api/feedback/1/status")
                            .header("token", adminToken)
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("状态更新成功"));

            Feedback updated = feedbackMapper.selectById(1L);
            assertNotNull(updated);
            assertEquals(1, updated.getStatus());
        }

        @Test
        @DisplayName("更新留言状态 - 需要管理员权限")
        void updateStatus_RequiresAdmin() throws Exception {
            // 无token时，拦截器返回401
            mockMvc.perform(put("/api/feedback/1/status")
                            .param("status", "1"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("删除留言测试")
    class DeleteTests {

        @Test
        @DisplayName("删除留言 - 超级管理员成功")
        void delete_SuperAdmin_Success() throws Exception {
            mockMvc.perform(delete("/api/feedback/2")
                            .header("token", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("删除成功"));

            assertNull(feedbackMapper.selectById(2L));
        }
    }
}
