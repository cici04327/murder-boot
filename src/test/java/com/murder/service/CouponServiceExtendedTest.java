package com.murder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.murder.common.exception.CouponException;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.service.impl.CouponServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券服务扩展单元测试 - 重点覆盖 calculateDiscount、useCoupon、refundCoupon 等核心逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("优惠券服务扩展测试")
class CouponServiceExtendedTest {

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon fullReductionCoupon;   // 满减券 type=1
    private Coupon discountCoupon;        // 折扣券 type=2
    private Coupon cashCoupon;            // 代金券 type=3
    private UserCoupon userCoupon;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserCoupon.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserCoupon.class);
        }

        // 满减券：满200减50
        fullReductionCoupon = new Coupon();
        fullReductionCoupon.setId(1L);
        fullReductionCoupon.setName("满200减50");
        fullReductionCoupon.setType(1);
        fullReductionCoupon.setDiscountValue(new BigDecimal("50.00"));
        fullReductionCoupon.setMinAmount(new BigDecimal("200.00"));
        fullReductionCoupon.setStatus(1);
        fullReductionCoupon.setRemainCount(100);
        fullReductionCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        fullReductionCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));

        // 折扣券：8折
        discountCoupon = new Coupon();
        discountCoupon.setId(2L);
        discountCoupon.setName("8折优惠券");
        discountCoupon.setType(2);
        discountCoupon.setDiscountValue(new BigDecimal("0.20")); // 优惠20%，即8折
        discountCoupon.setMinAmount(new BigDecimal("100.00"));
        discountCoupon.setStatus(1);
        discountCoupon.setRemainCount(100);
        discountCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        discountCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));

        // 代金券：无门槛30元
        cashCoupon = new Coupon();
        cashCoupon.setId(3L);
        cashCoupon.setName("30元代金券");
        cashCoupon.setType(3);
        cashCoupon.setDiscountValue(new BigDecimal("30.00"));
        cashCoupon.setMinAmount(BigDecimal.ZERO);
        cashCoupon.setStatus(1);
        cashCoupon.setRemainCount(100);
        cashCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        cashCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));

        // 用户优惠券
        userCoupon = new UserCoupon();
        userCoupon.setId(10L);
        userCoupon.setUserId(1L);
        userCoupon.setCouponId(1L);
        userCoupon.setStatus(1); // 未使用
        userCoupon.setExpireTime(LocalDateTime.now().plusDays(30));
        userCoupon.setReceiveTime(LocalDateTime.now());
    }

    // ==================== calculateDiscount 测试 ====================

    @Nested
    @DisplayName("计算优惠金额测试")
    class CalculateDiscountTests {

        @Test
        @DisplayName("满减券 - 订单满足门槛正确计算")
        void calculateDiscount_FullReduction_Success() {
            userCoupon.setCouponId(1L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal("300.00"));

            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("满减券 - 订单未达门槛抛出异常")
        void calculateDiscount_FullReduction_BelowMinAmount() {
            userCoupon.setCouponId(1L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            assertThrows(CouponException.class,
                () -> couponService.calculateDiscount(10L, new BigDecimal("150.00")));
        }

        @Test
        @DisplayName("折扣券 - 正确计算折扣金额")
        void calculateDiscount_Discount_Success() {
            userCoupon.setCouponId(2L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(2L)).thenReturn(discountCoupon);

            // 200元 * 20% = 40元优惠
            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal("200.00"));

            assertEquals(new BigDecimal("40.00"), discount);
        }

        @Test
        @DisplayName("代金券 - 无门槛正确返回面值")
        void calculateDiscount_Cash_Success() {
            userCoupon.setCouponId(3L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(3L)).thenReturn(cashCoupon);

            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal("50.00"));

            assertEquals(new BigDecimal("30.00"), discount);
        }

        @Test
        @DisplayName("优惠金额不超过订单金额 - 代金券面值大于订单金额时返回订单金额")
        void calculateDiscount_DiscountNotExceedOrderAmount() {
            userCoupon.setCouponId(3L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(3L)).thenReturn(cashCoupon);

            // 订单20元，代金券30元 -> 优惠应为20元（不超过订单金额）
            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal("20.00"));

            assertEquals(new BigDecimal("20.00"), discount);
        }

        @Test
        @DisplayName("用户优惠券不存在 - 返回零")
        void calculateDiscount_UserCouponNotFound() {
            when(userCouponMapper.selectById(99L)).thenReturn(null);

            BigDecimal discount = couponService.calculateDiscount(99L, new BigDecimal("300.00"));

            assertEquals(BigDecimal.ZERO, discount);
        }

        @Test
        @DisplayName("用户优惠券已使用 - 返回零")
        void calculateDiscount_AlreadyUsed() {
            userCoupon.setStatus(2); // 已使用
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);

            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal("300.00"));

            assertEquals(BigDecimal.ZERO, discount);
        }

        @Test
        @DisplayName("订单金额为零 - 返回零")
        void calculateDiscount_ZeroOrderAmount() {
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            BigDecimal discount = couponService.calculateDiscount(10L, BigDecimal.ZERO);

            assertEquals(BigDecimal.ZERO, discount);
        }

        @ParameterizedTest
        @CsvSource({
            "200.00, 50.00",   // 恰好满足门槛
            "300.00, 50.00",   // 超出门槛
            "500.00, 50.00"    // 远超门槛，满减固定金额
        })
        @DisplayName("满减券 - 不同金额参数化测试")
        void calculateDiscount_FullReduction_Parameterized(String orderAmount, String expectedDiscount) {
            userCoupon.setCouponId(1L);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            BigDecimal discount = couponService.calculateDiscount(10L, new BigDecimal(orderAmount));

            assertEquals(new BigDecimal(expectedDiscount), discount);
        }
    }

    // ==================== useCoupon 测试 ====================

    @Nested
    @DisplayName("使用优惠券测试")
    class UseCouponTests {

        @Test
        @DisplayName("使用优惠券 - 成功")
        void useCoupon_Success() {
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);
            when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

            assertDoesNotThrow(() -> couponService.useCoupon(10L, 100L));

            verify(userCouponMapper, times(1)).updateById(any(UserCoupon.class));
        }

        @Test
        @DisplayName("使用优惠券 - 不存在")
        void useCoupon_NotFound() {
            when(userCouponMapper.selectById(99L)).thenReturn(null);

            assertThrows(CouponException.class, () -> couponService.useCoupon(99L, 100L));
        }

        @Test
        @DisplayName("使用优惠券 - 已使用")
        void useCoupon_AlreadyUsed() {
            userCoupon.setStatus(2);
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);

            assertThrows(CouponException.class, () -> couponService.useCoupon(10L, 100L));
        }

        @Test
        @DisplayName("使用优惠券 - 已过期")
        void useCoupon_Expired() {
            userCoupon.setExpireTime(LocalDateTime.now().minusDays(1));
            when(userCouponMapper.selectById(10L)).thenReturn(userCoupon);

            assertThrows(CouponException.class, () -> couponService.useCoupon(10L, 100L));
        }
    }

    // ==================== refundCoupon 测试 ====================

    @Nested
    @DisplayName("退还优惠券测试")
    class RefundCouponTests {

        @Test
        @DisplayName("退还优惠券 - 未过期恢复为可用")
        void refundCoupon_NotExpired_RestoreToUnused() {
            userCoupon.setStatus(2); // 已使用
            userCoupon.setOrderId(100L);
            userCoupon.setExpireTime(LocalDateTime.now().plusDays(10)); // 未过期

            when(userCouponMapper.selectList(any())).thenReturn(Collections.singletonList(userCoupon));
            when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

            assertDoesNotThrow(() -> couponService.refundCoupon(100L));

            verify(userCouponMapper, times(1)).updateById(any(UserCoupon.class));
        }

        @Test
        @DisplayName("退还优惠券 - 已过期设置为过期状态")
        void refundCoupon_Expired_SetToExpired() {
            userCoupon.setStatus(2); // 已使用
            userCoupon.setOrderId(100L);
            userCoupon.setExpireTime(LocalDateTime.now().minusDays(1)); // 已过期

            when(userCouponMapper.selectList(any())).thenReturn(Collections.singletonList(userCoupon));
            when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

            assertDoesNotThrow(() -> couponService.refundCoupon(100L));

            verify(userCouponMapper, times(1)).updateById(any(UserCoupon.class));
        }

        @Test
        @DisplayName("退还优惠券 - 无关联优惠券时无操作")
        void refundCoupon_NoCoupons() {
            when(userCouponMapper.selectList(any())).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> couponService.refundCoupon(999L));

            verify(userCouponMapper, never()).updateById(any());
        }
    }

    // ==================== receiveCoupon 扩展测试 ====================

    @Nested
    @DisplayName("领取优惠券扩展测试")
    class ReceiveCouponExtendedTests {

        @Test
        @DisplayName("领取优惠券 - 下架优惠券不可领取")
        void receiveCoupon_Offline() {
            fullReductionCoupon.setStatus(0); // 下架
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            assertThrows(CouponException.class, () -> couponService.receiveCoupon(1L, 1L));
        }

        @Test
        @DisplayName("领取优惠券 - 未到领取时间")
        void receiveCoupon_NotStarted() {
            fullReductionCoupon.setValidStartTime(LocalDateTime.now().plusDays(1)); // 明天才开始
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            assertThrows(CouponException.class, () -> couponService.receiveCoupon(1L, 1L));
        }

        @Test
        @DisplayName("领取优惠券 - 已过期不可领取")
        void receiveCoupon_Expired() {
            fullReductionCoupon.setValidEndTime(LocalDateTime.now().minusDays(1)); // 已过期
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            assertThrows(CouponException.class, () -> couponService.receiveCoupon(1L, 1L));
        }

        @Test
        @DisplayName("领取优惠券 - 库存不足")
        void receiveCoupon_OutOfStock() {
            fullReductionCoupon.setRemainCount(0);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);

            assertThrows(CouponException.class, () -> couponService.receiveCoupon(1L, 1L));
        }

        @Test
        @DisplayName("领取优惠券 - 不存在")
        void receiveCoupon_NotFound() {
            when(couponMapper.selectById(99L)).thenReturn(null);

            assertThrows(CouponException.class, () -> couponService.receiveCoupon(1L, 99L));
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("上下架优惠券测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("下架优惠券 - 成功")
        void updateStatus_Offline_Success() {
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);
            when(couponMapper.updateById(any(Coupon.class))).thenReturn(1);

            assertDoesNotThrow(() -> couponService.updateStatus(1L, 0));

            verify(couponMapper, times(1)).updateById(any(Coupon.class));
        }

        @Test
        @DisplayName("上架优惠券 - 成功")
        void updateStatus_Online_Success() {
            fullReductionCoupon.setStatus(0);
            when(couponMapper.selectById(1L)).thenReturn(fullReductionCoupon);
            when(couponMapper.updateById(any(Coupon.class))).thenReturn(1);

            assertDoesNotThrow(() -> couponService.updateStatus(1L, 1));

            verify(couponMapper, times(1)).updateById(any(Coupon.class));
        }

        @Test
        @DisplayName("更新状态 - 优惠券不存在")
        void updateStatus_NotFound() {
            when(couponMapper.selectById(99L)).thenReturn(null);

            assertThrows(CouponException.class, () -> couponService.updateStatus(99L, 0));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("删除优惠券测试")
    class DeleteCouponTests {

        @Test
        @DisplayName("删除优惠券 - 成功（无用户领取）")
        void delete_Success() {
            when(userCouponMapper.selectCount(any())).thenReturn(0L);
            when(couponMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> couponService.delete(1L));

            verify(couponMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("删除优惠券 - 已有用户领取不可删除")
        void delete_HasUserCoupons() {
            when(userCouponMapper.selectCount(any())).thenReturn(5L);

            assertThrows(CouponException.class, () -> couponService.delete(1L));

            verify(couponMapper, never()).deleteById(any());
        }
    }

    // ==================== expireCoupons 测试 ====================

    @Nested
    @DisplayName("批量过期优惠券测试")
    class ExpireCouponsTests {

        @Test
        @DisplayName("批量过期 - 成功执行")
        void expireCoupons_Success() {
            when(userCouponMapper.update(any(), any())).thenReturn(3);

            assertDoesNotThrow(() -> couponService.expireCoupons());

            verify(userCouponMapper, times(1)).update(any(), any());
        }
    }
}
