package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预约模块集成测试
 */
@DisplayName("预约模块集成测试")
class ReservationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReservationMapper reservationMapper;

    @Nested
    @DisplayName("预约查询测试")
    class ReservationQueryTests {

        @Test
        @DisplayName("分页查询预约列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按用户ID查询预约")
        void pageQuery_ByUserId() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("userId", "1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3));
        }

        @Test
        @DisplayName("按状态查询预约")
        void pageQuery_ByStatus() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("status", "1")  // 已确认
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("根据ID查询预约详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.orderNo").value("RSV202601230001"));
        }

        @Test
        @DisplayName("根据预约编号查询")
        void getByReservationNo_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/no/RSV202601230001")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.orderNo").value("RSV202601230001"))
                    .andExpect(jsonPath("$.data.status").value(2));
        }

        @Test
        @DisplayName("查询不存在的预约")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/reservation/99999")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("创建预约测试")
    class CreateReservationTests {

        @Test
        @DisplayName("创建预约成功")
        void create_Success() throws Exception {
            // 预约时间设为3天后
            LocalDateTime reservationTime = LocalDateTime.now().plusDays(3);

            ReservationDTO dto = new ReservationDTO();
            dto.setUserId(1L);
            dto.setStoreId(1L);
            dto.setRoomId(2L);  // 使用房间2，避免与测试数据中的房间1冲突
            dto.setScriptId(1L);
            dto.setReservationTime(reservationTime);
            dto.setDuration(new BigDecimal("4.0"));
            dto.setPlayerCount(6);
            dto.setTotalPrice(new BigDecimal("198.00"));

            MvcResult result = mockMvc.perform(post("/api/reservation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(dto))
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.orderNo").exists())
                    .andReturn();

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            String orderNo = body.path("data").path("orderNo").asText();

            LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Reservation::getOrderNo, orderNo);
            Reservation created = reservationMapper.selectOne(wrapper);

            assertNotNull(created);
            assertEquals(1L, created.getUserId());
            assertEquals(1, created.getStatus());
            assertEquals(0, created.getPayStatus());
            assertEquals(2L, created.getRoomId());
            assertNotNull(created.getCheckInCode());
        }
    }

    @Nested
    @DisplayName("预约状态变更测试")
    class ReservationStatusTests {

        @Test
        @DisplayName("确认预约")
        void confirm_Success() throws Exception {
            // 确认待处理的预约（ID=2）
            mockMvc.perform(put("/api/reservation/2/confirm")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("确认成功"));

            Reservation updated = reservationMapper.selectById(2L);
            assertEquals(2, updated.getStatus());
            assertEquals(0, updated.getPayStatus());
        }

        @Test
        @DisplayName("取消预约")
        void cancel_Success() throws Exception {
            mockMvc.perform(put("/api/reservation/2/cancel")
                            .param("reason", "用户临时有事")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("取消成功"));

            Reservation updated = reservationMapper.selectById(2L);
            assertEquals(4, updated.getStatus());
            assertEquals("用户临时有事", updated.getRemark());
        }

        @Test
        @DisplayName("支付预约")
        void pay_Success() throws Exception {
            mockMvc.perform(put("/api/reservation/2/pay")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("支付成功"));

            Reservation updated = reservationMapper.selectById(2L);
            assertEquals(2, updated.getStatus());
            assertEquals(1, updated.getPayStatus());
            assertNotNull(updated.getPayTime());
        }

        @Test
        @DisplayName("完成预约")
        void complete_Success() throws Exception {
            // 完成已确认且已支付的预约（ID=1）
            mockMvc.perform(put("/api/reservation/1/complete")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("完成成功"));

            Reservation updated = reservationMapper.selectById(1L);
            assertEquals(3, updated.getStatus());
            assertEquals(1, updated.getPayStatus());
        }
    }

    @Nested
    @DisplayName("房间可用性检查测试")
    class AvailabilityTests {

        @Test
        @DisplayName("检查房间可用性 - 可用")
        void checkAvailability_Available() throws Exception {
            // 检查一个未来时间的可用性
            String futureTime = LocalDateTime.now().plusDays(7).toString();

            mockMvc.perform(get("/api/reservation/check-availability")
                            .param("roomId", "1")
                            .param("reservationTime", futureTime)
                            .param("duration", "3")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isBoolean());
        }

        @Test
        @DisplayName("查询即将开始的预约")
        void getUpcoming_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/upcoming")
                            .param("hours", "24")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
