package com.murder.integration;

import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
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
 * 支付模块集成测试
 */
@DisplayName("支付模块集成测试")
class PaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReservationMapper reservationMapper;

    @Nested
    @DisplayName("查询支付状态测试")
    class QueryPaymentStatusTests {

        @Test
        @DisplayName("查询支付状态 - 未支付订单")
        void queryPaymentStatus_Unpaid() throws Exception {
            mockMvc.perform(get("/api/reservation/payment/status/2")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(0));
        }

        @Test
        @DisplayName("查询支付状态 - 已支付订单")
        void queryPaymentStatus_Paid() throws Exception {
            mockMvc.perform(get("/api/reservation/payment/status/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("查询支付状态 - 未登录")
        void queryPaymentStatus_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/reservation/payment/status/1"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("创建支付测试")
    class CreatePaymentTests {

        @Test
        @DisplayName("创建支付 - 未登录")
        void createPayment_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/reservation/payment/create")
                            .param("reservationId", "2")
                            .param("paymentMethod", "alipay"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("创建支付 - 订单已支付")
        void createPayment_AlreadyPaid() throws Exception {
            mockMvc.perform(post("/api/reservation/payment/create")
                            .header("token", testUserToken)
                            .param("reservationId", "1")
                            .param("paymentMethod", "alipay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value(containsString("订单已支付")));

            Reservation unchanged = reservationMapper.selectById(1L);
            assertNotNull(unchanged);
            assertEquals(2, unchanged.getStatus());
            assertEquals(1, unchanged.getPayStatus());
            assertEquals(1, unchanged.getCheckInStatus());
            assertEquals("123456", unchanged.getCheckInCode());
        }
    }

    @Nested
    @DisplayName("退款申请测试")
    class RefundTests {

        @Test
        @DisplayName("申请退款 - 已支付订单成功申请")
        void applyRefund_Success() throws Exception {
            // 使用 reservation 3（已支付、未核销）申请退款
            mockMvc.perform(post("/api/reservation/payment/refund/apply")
                            .header("token", testUserToken)
                            .param("reservationId", "3")
                            .param("reason", "临时有事无法参加"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("退款申请已提交，请等待管理员审核"));

            Reservation updated = reservationMapper.selectById(3L);
            assertNotNull(updated);
            assertEquals(2, updated.getPayStatus());
            assertEquals(1, updated.getRefundStatus());
            assertEquals("临时有事无法参加", updated.getRefundReason());
            assertNotNull(updated.getRefundApplyTime());
        }

        @Test
        @DisplayName("申请退款 - 未支付订单不可退")
        void applyRefund_UnpaidOrder() throws Exception {
            mockMvc.perform(post("/api/reservation/payment/refund/apply")
                            .header("token", testUserToken)
                            .param("reservationId", "2")
                            .param("reason", "不想去了"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value(containsString("订单未支付或已退款")));

            Reservation unchanged = reservationMapper.selectById(2L);
            assertNotNull(unchanged);
            assertEquals(0, unchanged.getStatus());
            assertEquals(0, unchanged.getPayStatus());
            assertEquals(0, unchanged.getRefundStatus());
            assertNull(unchanged.getRefundApplyTime());
        }

        @Test
        @DisplayName("申请退款 - 未登录")
        void applyRefund_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/reservation/payment/refund/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reservationId\": 1, \"reason\": \"原因\"}"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
