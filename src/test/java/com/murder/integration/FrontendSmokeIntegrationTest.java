package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.murder.dto.ReservationDTO;
import com.murder.dto.UserLoginDTO;
import com.murder.entity.Feedback;
import com.murder.entity.Reservation;
import com.murder.mapper.FeedbackMapper;
import com.murder.mapper.ReservationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 面向前端关键用户路径的接口冒烟测试
 */
@DisplayName("前端关键链路冒烟测试")
class FrontendSmokeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Test
    @DisplayName("用户登录后可完成浏览-预约-支付-通知链路")
    void loginBrowseReservePayAndNotify_Success() throws Exception {
        String token = loginAsTestUser();
        long unreadBefore = getUnreadCount(token);

        mockMvc.perform(get("/api/script/list/hot").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("谜案追踪"));

        mockMvc.perform(get("/api/script/1").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("午夜惊魂"));

        ReservationDTO dto = new ReservationDTO();
        dto.setStoreId(1L);
        dto.setRoomId(2L);
        dto.setScriptId(1L);
        dto.setReservationTime(LocalDateTime.now().plusDays(5));
        dto.setDuration(new BigDecimal("4.0"));
        dto.setPlayerCount(6);
        dto.setTotalPrice(new BigDecimal("198.00"));

        MvcResult createResult = mockMvc.perform(post("/api/reservation")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.orderNo").exists())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long reservationId = createBody.path("data").path("id").asLong();
        String orderNo = createBody.path("data").path("orderNo").asText();
        long unreadAfterCreate = getUnreadCount(token);
        assertTrue(unreadAfterCreate > unreadBefore);

        mockMvc.perform(post("/api/reservation/payment/create")
                        .header("token", token)
                        .param("reservationId", String.valueOf(reservationId))
                        .param("paymentMethod", "mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("MOCK_PAY_SUCCESS"));

        mockMvc.perform(get("/api/reservation/payment/status/" + reservationId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));

        Reservation paidReservation = reservationMapper.selectById(reservationId);
        assertNotNull(paidReservation);
        assertEquals(1L, paidReservation.getUserId());
        assertEquals(2, paidReservation.getStatus());
        assertEquals(1, paidReservation.getPayStatus());
        assertNotNull(paidReservation.getPayTime());
        assertNotNull(paidReservation.getCheckInCode());

        long unreadAfter = getUnreadCount(token);
        assertEquals(unreadAfterCreate + 1, unreadAfter);

        MvcResult notificationsResult = mockMvc.perform(get("/api/notification/page")
                        .header("token", token)
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("onlyUnread", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode notificationsBody = objectMapper.readTree(notificationsResult.getResponse().getContentAsString());
        boolean foundPaymentNotification = StreamSupport.stream(
                        notificationsBody.path("data").path("records").spliterator(), false)
                .anyMatch(record ->
                        "支付成功通知".equals(record.path("title").asText())
                                && record.path("content").asText().contains(orderNo)
                                && !record.path("isRead").asBoolean());
        assertTrue(foundPaymentNotification);
    }

    @Test
    @DisplayName("用户浏览后可提交并查询自己的留言反馈")
    void browseAndSubmitFeedback_Success() throws Exception {
        String token = loginAsTestUser();
        String message = "前端冒烟测试留言-" + System.nanoTime();

        mockMvc.perform(get("/api/script/list/recommended").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("谜案追踪"));

        mockMvc.perform(get("/api/script/category").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("恐怖"));

        String requestBody = "{"
                + "\"name\": \"测试用户\","
                + "\"contact\": \"13800138000\","
                + "\"subject\": \"platform\","
                + "\"message\": \"" + message + "\""
                + "}";

        mockMvc.perform(post("/api/feedback/submit")
                        .header("token", token)
                        .header("X-Forwarded-For", "198.51.100.88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("留言提交成功，我们会尽快处理！"));

        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUserId, 1L).eq(Feedback::getMessage, message);
        Feedback saved = feedbackMapper.selectOne(wrapper);
        assertNotNull(saved);
        assertEquals("platform", saved.getSubject());
        assertEquals(0, saved.getStatus());
        assertEquals("198.51.100.88", saved.getIpAddress());

        MvcResult feedbackResult = mockMvc.perform(get("/api/feedback/user")
                        .header("token", token)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode feedbackBody = objectMapper.readTree(feedbackResult.getResponse().getContentAsString());
        boolean foundFeedback = StreamSupport.stream(
                        feedbackBody.path("data").path("records").spliterator(), false)
                .anyMatch(record ->
                        message.equals(record.path("message").asText())
                                && "platform".equals(record.path("subject").asText())
                                && "平台使用问题".equals(record.path("subjectName").asText()));
        assertTrue(foundFeedback);
    }

    private String loginAsTestUser() throws Exception {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("123456");

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").path("token").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private long getUnreadCount(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notification/unread-count")
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").asLong();
    }
}
