package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.murder.entity.User;
import com.murder.entity.UserCoupon;
import com.murder.entity.UserVip;
import com.murder.mapper.UserCouponMapper;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserVipMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VIP模块集成测试
 */
@DisplayName("VIP模块集成测试")
class VipIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserVipMapper userVipMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Nested
    @DisplayName("VIP套餐查询测试")
    class PackageQueryTests {

        @Test
        @DisplayName("获取VIP套餐列表")
        void getVipPackages_Success() throws Exception {
            mockMvc.perform(get("/api/vip/packages")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("见习侦探月卡"))
                    .andExpect(jsonPath("$.data[0].level").value(1));
        }

        @Test
        @DisplayName("根据ID查询VIP套餐")
        void getVipPackageById_Success() throws Exception {
            mockMvc.perform(get("/api/vip/packages/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("见习侦探月卡"))
                    .andExpect(jsonPath("$.data.level").value(1))
                    .andExpect(jsonPath("$.data.couponCount").value(2));
        }
    }

    @Nested
    @DisplayName("用户VIP信息测试")
    class UserVipInfoTests {

        @Test
        @DisplayName("查询用户VIP信息 - 已登录")
        void getUserVipInfo_LoggedIn() throws Exception {
            mockMvc.perform(get("/api/vip/info")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.isVip").value(false))
                    .andExpect(jsonPath("$.data.level").value(0))
                    .andExpect(jsonPath("$.data.levelName").value("普通用户"))
                    .andExpect(jsonPath("$.data.daysLeft").value(0))
                    .andExpect(jsonPath("$.data.expireTime").value(nullValue()));
        }

        @Test
        @DisplayName("查询用户VIP信息 - 未登录")
        void getUserVipInfo_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/vip/info"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("查询月度体验券发放状态")
        void getMonthlyCouponStatus_Success() throws Exception {
            mockMvc.perform(get("/api/vip/monthly-coupon-status")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.isVip").value(false))
                    .andExpect(jsonPath("$.data.grantedCount").value(0))
                    .andExpect(jsonPath("$.data.totalCount").value(0))
                    .andExpect(jsonPath("$.data.couponAmount").value(0))
                    .andExpect(jsonPath("$.data.nextGrantTime").value(nullValue()));
        }

        @Test
        @DisplayName("查询VIP历史记录")
        void getUserVipHistory_Success() throws Exception {
            mockMvc.perform(get("/api/vip/history")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.records", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("购买VIP测试")
    class PurchaseVipTests {

        @Test
        @DisplayName("购买VIP - 成功")
        void purchaseVip_Success() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/vip/purchase")
                            .header("token", testUserToken)
                            .param("packageId", "1")
                            .param("paymentMethod", "ALIPAY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", startsWith("VIP")))
                    .andReturn();

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            String orderNo = body.path("data").asText();

            UserVip currentVip = userVipMapper.getCurrentVip(1L);
            assertNotNull(currentVip);
            assertEquals(orderNo, currentVip.getOrderNo());
            assertEquals(1, currentVip.getLevel());
            assertEquals("ALIPAY", currentVip.getPaymentMethod());
            assertEquals(1, currentVip.getStatus());
            assertTrue(currentVip.getEndTime().isAfter(currentVip.getStartTime()));

            User user = userMapper.selectById(1L);
            assertNotNull(user);
            assertEquals(1, user.getVipLevel());
            assertNotNull(user.getVipExpireTime());

            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, 1L);
            assertEquals(2L, userCouponMapper.selectCount(wrapper));

            mockMvc.perform(get("/api/vip/info")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.isVip").value(true))
                    .andExpect(jsonPath("$.data.level").value(1))
                    .andExpect(jsonPath("$.data.levelName").value("见习侦探"));
        }

        @Test
        @DisplayName("购买VIP - 未登录")
        void purchaseVip_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/vip/purchase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"packageId\": 1, \"paymentMethod\": \"mock\"}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("VIP等级检查测试")
    class VipLevelTests {

        @Test
        @DisplayName("检查是否是VIP - 非VIP用户")
        void isVip_NonVipUser() throws Exception {
            mockMvc.perform(get("/api/vip/check-status")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.isVip").value(false))
                    .andExpect(jsonPath("$.data.level").value(0))
                    .andExpect(jsonPath("$.data.pointMultiplier").value(1))
                    .andExpect(jsonPath("$.data.hasPriorityBooking").value(false));
        }
    }
}
