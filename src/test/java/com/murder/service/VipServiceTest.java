package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.entity.UserVip;
import com.murder.entity.VipPackage;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserVipMapper;
import com.murder.mapper.VipPackageMapper;
import com.murder.service.impl.VipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VIP月度体验券发放逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VIP月度体验券发放测试")
class VipServiceTest {

    @Mock
    private VipPackageMapper vipPackageMapper;

    @Mock
    private UserVipMapper userVipMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CouponService couponService;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VipServiceImpl vipService;

    private UserVip mockVip(int level) {
        UserVip vip = new UserVip();
        vip.setId((long) level);
        vip.setUserId(100L + level);
        vip.setPackageId((long) level);
        vip.setLevel(level);
        vip.setStatus(1);
        vip.setStartTime(LocalDateTime.now().minusDays(10));
        vip.setEndTime(LocalDateTime.now().plusDays(20));
        return vip;
    }

    private Coupon mockCouponTemplate(int level, int amount) {
        Coupon c = new Coupon();
        c.setId((long) (level * 10 + amount));
        c.setName("VIP" + levelName(level) + "月度体验券");
        c.setType(1);
        c.setDiscountValue(new BigDecimal(amount));
        c.setMinAmount(new BigDecimal(amount));
        c.setTotalCount(99999);
        c.setRemainCount(99999);
        c.setStatus(1);
        return c;
    }

    private String levelName(int level) {
        switch (level) {
            case 1: return "见习侦探";
            case 2: return "银章侦探";
            case 3: return "金章侦探";
            case 4: return "传奇侦探";
            default: return "未知";
        }
    }

    // =====================================================================
    // 核心测试：各等级张数和面额
    // =====================================================================

    @ParameterizedTest(name = "等级{0}：应发{1}张，面值{2}元")
    @CsvSource({
        "1, 2,  10",   // 见习侦探：2张×10元
        "2, 5,  20",   // 银章侦探：5张×20元
        "3, 10, 50",   // 金章侦探：10张×50元
        "4, 15, 100"   // 传奇侦探：15张×100元
    })
    @DisplayName("各VIP等级月度体验券张数和面额验证")
    void testGrantMonthlyCoupons_CountAndAmount(int level, int expectedCount, int expectedAmount) {
        // Given
        UserVip vip = mockVip(level);
        Coupon template = mockCouponTemplate(level, expectedAmount);

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);
        // 本月未发放过
        when(userCouponMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 返回已有模板（避免走insert分支）
        when(couponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then：验证 insert 被调用了正确的次数（即发放了正确张数）
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper, times(expectedCount)).insert(captor.capture());

        List<UserCoupon> issued = captor.getAllValues();
        assertEquals(expectedCount, issued.size(), "发放张数不正确");

        for (UserCoupon uc : issued) {
            assertEquals(vip.getUserId(), uc.getUserId(), "用户ID不正确");
            assertEquals(template.getId(), uc.getCouponId(), "优惠券模板ID不正确");
            assertEquals(1, uc.getStatus(), "优惠券初始状态应为未使用(1)");
            assertNotNull(uc.getReceiveTime(), "领取时间不能为空");
            assertNotNull(uc.getExpireTime(), "过期时间不能为空");
        }

        // 验证过期时间是当月最后一天（不是30天后）
        LocalDateTime expireTime = issued.get(0).getExpireTime();
        LocalDateTime monthEnd = LocalDateTime.now()
                .withDayOfMonth(1).plusMonths(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                .minusSeconds(1);
        // 允许1秒误差
        assertTrue(Math.abs(expireTime.getSecond() - monthEnd.getSecond()) <= 1
                && expireTime.getMonth() == monthEnd.getMonth()
                && expireTime.getDayOfMonth() == monthEnd.getDayOfMonth(),
                "体验券应在当月最后一天过期，实际过期时间：" + expireTime);
    }

    // =====================================================================
    // 防重复发放测试
    // =====================================================================

    @Test
    @DisplayName("本月已发放过 - 跳过重复发放")
    void testGrantMonthlyCoupons_SkipIfAlreadyGranted() {
        // Given
        UserVip vip = mockVip(2); // 银章
        Coupon template = mockCouponTemplate(2, 20);

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);
        when(couponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        // 本月已发放过5张
        when(userCouponMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then：不应再次insert
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    // =====================================================================
    // 非VIP用户测试
    // =====================================================================

    @Test
    @DisplayName("非VIP用户 - 不发放体验券")
    void testGrantMonthlyCoupons_NotVip() {
        // Given：用户没有VIP记录
        when(userVipMapper.getCurrentVip(999L)).thenReturn(null);

        // When
        vipService.grantMonthlyCoupons(999L);

        // Then
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    @DisplayName("VIP已过期 - 不发放体验券")
    void testGrantMonthlyCoupons_VipExpired() {
        // Given：VIP记录存在但已过期
        UserVip expiredVip = mockVip(1);
        expiredVip.setEndTime(LocalDateTime.now().minusDays(1)); // 已过期

        when(userVipMapper.getCurrentVip(expiredVip.getUserId())).thenReturn(expiredVip);

        // When
        vipService.grantMonthlyCoupons(expiredVip.getUserId());

        // Then
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    // =====================================================================
    // 未知等级测试
    // =====================================================================

    @Test
    @DisplayName("未知VIP等级 - 不发放体验券")
    void testGrantMonthlyCoupons_UnknownLevel() {
        // Given
        UserVip vip = mockVip(1);
        vip.setLevel(99); // 不存在的等级

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    // =====================================================================
    // 通知推送测试
    // =====================================================================

    @Test
    @DisplayName("发放成功后 - 应推送站内通知给用户")
    void testGrantMonthlyCoupons_SendsNotification() {
        // Given：金章侦探（10张×50元）
        UserVip vip = mockVip(3);
        Coupon template = mockCouponTemplate(3, 50);

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);
        when(userCouponMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(couponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then：通知服务应被调用一次
        verify(notificationService, times(1))
                .sendToUsers(anyString(), anyString(), anyInt(), anyString(), isNull(), eq(vip.getUserId()));
    }

    @Test
    @DisplayName("发放成功后 - 通知内容包含正确的张数和面额")
    void testGrantMonthlyCoupons_NotificationContent() {
        // Given：传奇侦探（15张×100元）
        UserVip vip = mockVip(4);
        Coupon template = mockCouponTemplate(4, 100);

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);
        when(userCouponMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(couponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then：捕获通知内容，验证包含15张和100元信息
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendToUsers(
                titleCaptor.capture(), contentCaptor.capture(),
                anyInt(), anyString(), isNull(), eq(vip.getUserId()));

        String title = titleCaptor.getValue();
        String content = contentCaptor.getValue();
        assertTrue(title.contains("体验券"), "通知标题应包含'体验券'，实际：" + title);
        assertTrue(content.contains("15"), "通知内容应包含张数15，实际：" + content);
        assertTrue(content.contains("100"), "通知内容应包含面额100，实际：" + content);
    }

    // =====================================================================
    // 模板自动创建测试
    // =====================================================================

    @Test
    @DisplayName("优惠券模板不存在时 - 自动创建模板再发放")
    void testGrantMonthlyCoupons_AutoCreateTemplate() {
        // Given：见习侦探，数据库中没有模板
        UserVip vip = mockVip(1);

        when(userVipMapper.getCurrentVip(vip.getUserId())).thenReturn(vip);
        when(userCouponMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 第一次查询返回null（不存在），insert后第二次调用selectOne也可能发生，用Answer模拟insert后返回
        when(couponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // 模拟insert后给coupon赋ID
        doAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            c.setId(999L);
            return 1;
        }).when(couponMapper).insert(any(Coupon.class));
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        // When
        vipService.grantMonthlyCoupons(vip.getUserId());

        // Then：应自动创建优惠券模板
        verify(couponMapper, times(1)).insert(any(Coupon.class));
        // 并发放2张（见习等级）
        verify(userCouponMapper, times(2)).insert(any(UserCoupon.class));
    }
}
