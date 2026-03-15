package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserCouponMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 优惠券模块集成测试
 */
@DisplayName("优惠券模块集成测试")
class CouponIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Nested
    @DisplayName("优惠券查询测试")
    class QueryTests {

        @Test
        @DisplayName("分页查询优惠券列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/coupon/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按类型筛选优惠券")
        void pageQuery_ByType() throws Exception {
            mockMvc.perform(get("/api/coupon/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("type", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(2));
        }

        @Test
        @DisplayName("按状态筛选优惠券")
        void pageQuery_ByStatus() throws Exception {
            mockMvc.perform(get("/api/coupon/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3));
        }

        @Test
        @DisplayName("根据ID查询优惠券详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/coupon/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("新用户优惠券"))
                    .andExpect(jsonPath("$.data.remainCount").value(900));
        }

        @Test
        @DisplayName("查询不存在的优惠券")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/coupon/99999")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("用户领取优惠券测试")
    class ReceiveCouponTests {

        @Test
        @DisplayName("用户领取优惠券 - 成功")
        void receiveCoupon_Success() throws Exception {
            mockMvc.perform(post("/api/coupon/receive")
                            .header("token", testUserToken)
                            .param("couponId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, 1L)
                    .eq(UserCoupon::getCouponId, 1L)
                    .eq(UserCoupon::getStatus, 1);
            assertEquals(1L, userCouponMapper.selectCount(wrapper));

            Coupon coupon = couponMapper.selectById(1L);
            assertEquals(899, coupon.getRemainCount());
        }

        @Test
        @DisplayName("用户领取优惠券 - 未登录")
        void receiveCoupon_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/coupon/receive")
                            .param("couponId", "1"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("用户优惠券查询测试")
    class UserCouponTests {

        @Test
        @DisplayName("查询用户优惠券列表")
        void getUserCoupons_Success() throws Exception {
            mockMvc.perform(post("/api/coupon/receive")
                            .header("token", testUserToken)
                            .param("couponId", "1"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/coupon/user")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].couponId").value(1))
                    .andExpect(jsonPath("$.data.records[0].couponName").value("新用户优惠券"))
                    .andExpect(jsonPath("$.data.records[0].status").value(1));
        }

        @Test
        @DisplayName("查询用户未使用优惠券")
        void getUserCoupons_Unused() throws Exception {
            mockMvc.perform(post("/api/coupon/receive")
                            .header("token", testUserToken)
                            .param("couponId", "1"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/coupon/user")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @DisplayName("查询可用优惠券列表")
        void getAvailableCoupons_Success() throws Exception {
            mockMvc.perform(post("/api/coupon/receive")
                            .header("token", testUserToken)
                            .param("couponId", "1"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/coupon/user/1/available")
                            .header("token", testUserToken)
                            .param("orderAmount", "300.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].couponId").value(1))
                    .andExpect(jsonPath("$.data[0].canUse").value(true));
        }
    }

    @Nested
    @DisplayName("优惠券统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取优惠券统计信息")
        void getCouponStatistics_Success() throws Exception {
            mockMvc.perform(get("/api/coupon/1/statistics")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.receivedCount").value(0))
                    .andExpect(jsonPath("$.data.usedCount").value(0))
                    .andExpect(jsonPath("$.data.remainCount").value(900));
        }
    }
}
