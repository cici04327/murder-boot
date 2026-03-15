package com.murder.service;

import com.murder.entity.User;
import com.murder.entity.UserPointsRecord;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserPointsRecordMapper;
import com.murder.service.impl.UserPointsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户积分服务测试类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserPointsServiceTest {

    @InjectMocks
    private UserPointsServiceImpl userPointsService;

    @Mock
    private UserPointsRecordMapper userPointsRecordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private VipService vipService;

    @Mock
    private CouponService couponService;

    private Long testUserId = 1L;
    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setPoints(100);
    }

    /**
     * 测试 getTotalPoints - 查询积分：正常情况
     */
    @Test
    public void testGetTotalPointsSuccess() {
        when(userMapper.selectById(testUserId)).thenReturn(testUser);

        Integer points = userPointsService.getTotalPoints(testUserId);

        assertEquals(100, points);
        verify(userMapper, times(1)).selectById(testUserId);
    }

    /**
     * 测试 getTotalPoints - 查询积分：用户不存在
     */
    @Test
    public void testGetTotalPointsUserNotExists() {
        when(userMapper.selectById(testUserId)).thenReturn(null);

        Integer points = userPointsService.getTotalPoints(testUserId);

        assertEquals(0, points);
        verify(userMapper, times(1)).selectById(testUserId);
    }

    /**
     * 测试 addPoints - 增加积分：正常增加
     */
    @Test
    public void testAddPointsSuccess() {
        when(userMapper.selectById(testUserId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(vipService.getPointMultiplier(testUserId)).thenReturn(BigDecimal.ONE);
        when(userPointsRecordMapper.insert(any(UserPointsRecord.class))).thenReturn(1);

        userPointsService.addPoints(testUserId, 50, "测试增加");

        verify(userMapper, times(1)).selectById(testUserId);
        verify(userMapper, times(1)).updateById(any(User.class));
        verify(userPointsRecordMapper, times(1)).insert(any(UserPointsRecord.class));
    }

    /**
     * 测试 addPoints - 增加积分：用户不存在抛异常
     */
    @Test
    public void testAddPointsUserNotExists() {
        when(userMapper.selectById(testUserId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userPointsService.addPoints(testUserId, 50, "测试增加");
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(userMapper, times(1)).selectById(testUserId);
    }

    /**
     * 测试 deductPoints - 扣减积分：正常扣减
     */
    @Test
    public void testDeductPointsSuccess() {
        when(userMapper.selectById(testUserId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userPointsRecordMapper.insert(any(UserPointsRecord.class))).thenReturn(1);

        userPointsService.deductPoints(testUserId, 30, "测试扣减");

        verify(userMapper, times(1)).selectById(testUserId);
        verify(userMapper, times(1)).updateById(any(User.class));
        verify(userPointsRecordMapper, times(1)).insert(any(UserPointsRecord.class));
    }

    /**
     * 测试 deductPoints - 扣减积分：积分不足抛异常
     */
    @Test
    public void testDeductPointsInsufficientPoints() {
        when(userMapper.selectById(testUserId)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userPointsService.deductPoints(testUserId, 150, "测试扣减");
        });

        assertEquals("积分不足", exception.getMessage());
        verify(userMapper, times(1)).selectById(testUserId);
        verify(userPointsRecordMapper, never()).insert(any(UserPointsRecord.class));
    }

    /**
     * 测试 signIn - 签到：正常签到
     */
    @Test
    public void testSignInSuccess() {
        when(userPointsRecordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(testUserId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(vipService.getPointMultiplier(testUserId)).thenReturn(BigDecimal.ONE);
        when(userPointsRecordMapper.insert(any(UserPointsRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> userPointsService.signIn(testUserId));

        verify(userPointsRecordMapper, atLeast(1)).selectCount(any());
        verify(userMapper, atLeast(1)).selectById(testUserId);
        verify(userPointsRecordMapper, atLeast(1)).insert(any(UserPointsRecord.class));
    }

    /**
     * 测试 signIn - 签到：重复签到抛异常
     */
    @Test
    public void testSignInDuplicate() {
        when(userPointsRecordMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userPointsService.signIn(testUserId);
        });

        assertEquals("今日已签到", exception.getMessage());
        verify(userPointsRecordMapper, times(1)).selectCount(any());
    }

    /**
     * 测试 rewardForReservation - 预约奖励：正常奖励
     */
    @Test
    public void testRewardForReservationSuccess() {
        Long reservationId = 100L;
        when(userPointsRecordMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(userMapper.selectById(testUserId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(vipService.getPointMultiplier(testUserId)).thenReturn(BigDecimal.ONE);
        when(userPointsRecordMapper.insert(any(UserPointsRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> userPointsService.rewardForReservation(testUserId, reservationId));

        verify(userPointsRecordMapper, atLeast(1)).selectList(any());
        verify(userMapper, atLeast(1)).selectById(testUserId);
        verify(userPointsRecordMapper, atLeast(1)).insert(any(UserPointsRecord.class));
    }
}
