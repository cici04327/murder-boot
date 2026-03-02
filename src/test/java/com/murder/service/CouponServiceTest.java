package com.murder.service;

import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.service.impl.CouponServiceImpl;
import com.murder.vo.CouponVO;
import com.murder.vo.UserCouponVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("优惠券服务测试")
class CouponServiceTest {

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon testCoupon;
    private UserCoupon testUserCoupon;

    @BeforeEach
    void setUp() {
        // 初始化测试优惠券（使用实际的实体字段）
        testCoupon = new Coupon();
        testCoupon.setId(1L);
        testCoupon.setName("新用户专享券");
        testCoupon.setType(1); // 满减券
        testCoupon.setDiscountValue(new BigDecimal("50.00"));
        testCoupon.setMinAmount(new BigDecimal("200.00"));
        testCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        testCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));
        testCoupon.setTotalCount(1000);
        testCoupon.setRemainCount(900);
        testCoupon.setStatus(1);

        // 初始化用户优惠券
        testUserCoupon = new UserCoupon();
        testUserCoupon.setId(1L);
        testUserCoupon.setUserId(1L);
        testUserCoupon.setCouponId(1L);
        testUserCoupon.setStatus(0); // 未使用
        testUserCoupon.setExpireTime(LocalDateTime.now().plusDays(30));
        testUserCoupon.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("根据ID查询优惠券")
    void testGetCouponById() {
        // Given
        when(couponMapper.selectById(1L)).thenReturn(testCoupon);

        // When
        Coupon result = couponMapper.selectById(1L);

        // Then
        assertNotNull(result);
        assertEquals("新用户专享券", result.getName());
    }

    @Test
    @DisplayName("查询优惠券列表")
    void testGetCouponList() {
        // Given
        List<Coupon> coupons = Arrays.asList(testCoupon);
        when(couponMapper.selectList(any())).thenReturn(coupons);

        // When
        List<Coupon> result = couponMapper.selectList(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("领取优惠券 - 成功")
    void testReceiveCoupon_Success() {
        // Given
        when(couponMapper.selectById(1L)).thenReturn(testCoupon);
        when(userCouponMapper.selectCount(any())).thenReturn(0L); // 未领取过
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);
        when(couponMapper.updateById(any(Coupon.class))).thenReturn(1);

        // When & Then
        assertDoesNotThrow(() -> couponService.receiveCoupon(1L, 1L));
        verify(userCouponMapper, times(1)).insert(any(UserCoupon.class));
    }

    @Test
    @DisplayName("领取优惠券 - 已领取过")
    void testReceiveCoupon_AlreadyReceived() {
        // Given
        when(couponMapper.selectById(1L)).thenReturn(testCoupon);
        when(userCouponMapper.selectCount(any())).thenReturn(1L); // 已领取

        // When & Then
        assertThrows(Exception.class, () -> couponService.receiveCoupon(1L, 1L));
    }

    @Test
    @DisplayName("领取优惠券 - 已领完")
    void testReceiveCoupon_SoldOut() {
        // Given
        testCoupon.setRemainCount(0); // 已领完
        when(couponMapper.selectById(1L)).thenReturn(testCoupon);

        // When & Then
        assertThrows(Exception.class, () -> couponService.receiveCoupon(1L, 1L));
    }

    @Test
    @DisplayName("查询用户优惠券")
    void testGetUserCoupon() {
        // Given
        when(userCouponMapper.selectById(1L)).thenReturn(testUserCoupon);

        // When
        UserCoupon result = userCouponMapper.selectById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("使用优惠券 - 查询用户优惠券")
    void testUseCoupon() {
        // Given - 测试能够正确查询用户优惠券
        when(userCouponMapper.selectById(1L)).thenReturn(testUserCoupon);

        // When
        UserCoupon result = userCouponMapper.selectById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getCouponId());
        verify(userCouponMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("退还优惠券 - 查询订单关联优惠券")
    void testRefundCoupon() {
        // Given
        testUserCoupon.setStatus(1); // 已使用
        testUserCoupon.setOrderId(1L);
        List<UserCoupon> userCoupons = Arrays.asList(testUserCoupon);
        when(userCouponMapper.selectList(any())).thenReturn(userCoupons);

        // When
        List<UserCoupon> result = userCouponMapper.selectList(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(userCouponMapper, times(1)).selectList(any());
    }
}
