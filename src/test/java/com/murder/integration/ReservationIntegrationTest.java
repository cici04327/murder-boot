package com.murder.integration;

import com.murder.dto.ReservationDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预约模块集成测试
 */
@DisplayName("预约模块集成测试")
class ReservationIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("预约查询测试")
    class ReservationQueryTests {

        @Test
        @DisplayName("分页查询预约列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按用户ID查询预约")
        void pageQuery_ByUserId() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("按状态查询预约")
        void pageQuery_ByStatus() throws Exception {
            mockMvc.perform(get("/api/reservation/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("status", "1"))  // 已确认
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("根据ID查询预约详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.reservationNo").value("RSV202601230001"));
        }

        @Test
        @DisplayName("根据预约编号查询")
        void getByReservationNo_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/no/RSV202601230001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.reservationNo").value("RSV202601230001"));
        }

        @Test
        @DisplayName("查询不存在的预约")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/reservation/99999"))
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
            dto.setRoomId(1L);
            dto.setScriptId(1L);
            dto.setReservationTime(reservationTime);
            dto.setDuration(new BigDecimal("4.0"));
            dto.setPlayerCount(6);
            dto.setTotalPrice(new BigDecimal("198.00"));

            mockMvc.perform(post("/api/reservation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.reservationNo").exists());
        }
    }

    @Nested
    @DisplayName("预约状态变更测试")
    class ReservationStatusTests {

        @Test
        @DisplayName("确认预约")
        void confirm_Success() throws Exception {
            // 确认待处理的预约（ID=2）
            mockMvc.perform(put("/api/reservation/2/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("确认成功"));
        }

        @Test
        @DisplayName("取消预约")
        void cancel_Success() throws Exception {
            mockMvc.perform(put("/api/reservation/2/cancel")
                            .param("reason", "用户临时有事"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("取消成功"));
        }

        @Test
        @DisplayName("支付预约")
        void pay_Success() throws Exception {
            mockMvc.perform(put("/api/reservation/2/pay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("支付成功"));
        }

        @Test
        @DisplayName("完成预约")
        void complete_Success() throws Exception {
            // 完成已确认且已支付的预约（ID=1）
            mockMvc.perform(put("/api/reservation/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("完成成功"));
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
                            .param("duration", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isBoolean());
        }

        @Test
        @DisplayName("查询即将开始的预约")
        void getUpcoming_Success() throws Exception {
            mockMvc.perform(get("/api/reservation/upcoming")
                            .param("hours", "24"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
