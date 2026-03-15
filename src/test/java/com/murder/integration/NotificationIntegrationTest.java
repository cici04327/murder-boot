package com.murder.integration;

import com.murder.entity.SystemNotification;
import com.murder.entity.UserNotification;
import com.murder.mapper.SystemNotificationMapper;
import com.murder.mapper.UserNotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通知模块集成测试
 */
@DisplayName("通知模块集成测试")
class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SystemNotificationMapper systemNotificationMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    private Long createUserNotification(String title, Integer type, Integer isRead) {
        LocalDateTime now = LocalDateTime.now();

        SystemNotification systemNotification = SystemNotification.builder()
                .title(title)
                .content(title + "内容")
                .type(type)
                .bizType("test")
                .bizId(1L)
                .targetType(2)
                .targetUsers("1")
                .sendTime(now)
                .status(2)
                .build();
        systemNotificationMapper.insert(systemNotification);

        UserNotification userNotification = UserNotification.builder()
                .userId(1L)
                .notificationId(systemNotification.getId())
                .isRead(isRead)
                .readTime(isRead == 1 ? now : null)
                .build();
        userNotificationMapper.insert(userNotification);
        return userNotification.getId();
    }

    @Nested
    @DisplayName("通知查询测试")
    class QueryTests {

        @Test
        @DisplayName("分页查询用户通知")
        void getUserNotifications_Success() throws Exception {
            createUserNotification("测试通知", 1, 0);

            mockMvc.perform(get("/api/notification/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].title").value("测试通知"))
                    .andExpect(jsonPath("$.data.records[0].isRead").value(false));
        }

        @Test
        @DisplayName("只查看未读通知")
        void getUserNotifications_OnlyUnread() throws Exception {
            createUserNotification("未读通知", 1, 0);
            createUserNotification("已读通知", 4, 1);

            mockMvc.perform(get("/api/notification/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("onlyUnread", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].title").value("未读通知"))
                    .andExpect(jsonPath("$.data.records[0].isRead").value(false));
        }

        @Test
        @DisplayName("查询通知 - 未登录")
        void getUserNotifications_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/notification/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("获取未读通知数量")
        void getUnreadCount_Success() throws Exception {
            createUserNotification("未读通知A", 1, 0);
            createUserNotification("未读通知B", 2, 0);
            createUserNotification("已读通知", 4, 1);

            mockMvc.perform(get("/api/notification/unread-count")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(2));
        }

        @Test
        @DisplayName("获取通知统计信息")
        void getNotificationStatistics_Success() throws Exception {
            createUserNotification("预约成功通知A", 1, 0);
            createUserNotification("预约成功通知B", 1, 0);
            createUserNotification("系统公告", 4, 1);

            mockMvc.perform(get("/api/notification/statistics")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.totalCount").value(3))
                    .andExpect(jsonPath("$.data.unreadCount").value(2))
                    .andExpect(jsonPath("$.data.readCount").value(1))
                    .andExpect(jsonPath("$.data.todayCount").value(3))
                    .andExpect(jsonPath("$.data.typeCount['预约成功']").value(2))
                    .andExpect(jsonPath("$.data.typeCount['系统公告']").value(1));
        }
    }

    @Nested
    @DisplayName("标记已读测试")
    class MarkReadTests {

        @Test
        @DisplayName("标记全部通知为已读")
        void markAllAsRead_Success() throws Exception {
            createUserNotification("未读通知", 1, 0);
            createUserNotification("已读通知", 4, 1);

            mockMvc.perform(put("/api/notification/read-all")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            mockMvc.perform(get("/api/notification/unread-count")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(0));

            mockMvc.perform(get("/api/notification/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("onlyUnread", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("标记全部已读 - 未登录")
        void markAllAsRead_Unauthorized() throws Exception {
            mockMvc.perform(put("/api/notification/read-all"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("批量标记已读")
        void batchMarkAsRead_Success() throws Exception {
            Long notificationId1 = createUserNotification("批量未读通知1", 1, 0);
            Long notificationId2 = createUserNotification("批量未读通知2", 2, 0);

            mockMvc.perform(put("/api/notification/batch-read")
                            .header("token", testUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[" + notificationId1 + ", " + notificationId2 + "]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            assertEquals(1, userNotificationMapper.selectById(notificationId1).getIsRead());
            assertEquals(1, userNotificationMapper.selectById(notificationId2).getIsRead());
        }
    }

    @Nested
    @DisplayName("删除通知测试")
    class DeleteTests {

        @Test
        @DisplayName("删除单条通知")
        void deleteNotification_Success() throws Exception {
            Long notificationId = createUserNotification("待删除通知", 1, 0);

            mockMvc.perform(delete("/api/notification/" + notificationId)
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            assertNull(userNotificationMapper.selectById(notificationId));

            mockMvc.perform(get("/api/notification/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("批量删除通知")
        void batchDeleteNotifications_Success() throws Exception {
            Long notificationId1 = createUserNotification("批量删除通知1", 1, 0);
            Long notificationId2 = createUserNotification("批量删除通知2", 2, 0);

            mockMvc.perform(delete("/api/notification/batch")
                            .header("token", testUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[" + notificationId1 + ", " + notificationId2 + "]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            assertNull(userNotificationMapper.selectById(notificationId1));
            assertNull(userNotificationMapper.selectById(notificationId2));
        }

        @Test
        @DisplayName("清空已读通知")
        void clearReadNotifications_Success() throws Exception {
            createUserNotification("已读通知", 4, 1);
            createUserNotification("未读通知", 1, 0);

            mockMvc.perform(delete("/api/notification/clear-read")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            mockMvc.perform(get("/api/notification/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].title").value("未读通知"));
        }
    }

    @Nested
    @DisplayName("搜索通知测试")
    class SearchTests {

        @Test
        @DisplayName("搜索通知")
        void searchNotifications_Success() throws Exception {
            createUserNotification("测试关键词通知", 1, 0);
            createUserNotification("普通通知", 4, 0);

            mockMvc.perform(get("/api/notification/search")
                            .header("token", testUserToken)
                            .param("keyword", "测试")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].title").value("测试关键词通知"));
        }
    }

    @Nested
    @DisplayName("管理端通知测试")
    class AdminNotificationTests {

        @Test
        @DisplayName("管理端获取未读通知数量")
        void getAdminUnreadCount_Success() throws Exception {
            mockMvc.perform(get("/api/admin/notification/unread-count")
                            .header("token", adminApiToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isNumber());
        }
    }
}
