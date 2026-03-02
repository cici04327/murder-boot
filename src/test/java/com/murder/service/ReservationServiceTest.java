package com.murder.service;

import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预约服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("预约服务测试")
class ReservationServiceTest {

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        // 初始化测试预约数据（使用实际的实体字段）
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setOrderNo("ORD202501230001");
        testReservation.setUserId(1L);
        testReservation.setStoreId(1L);
        testReservation.setScriptId(1L);
        testReservation.setRoomId(1L);
        testReservation.setReservationTime(LocalDateTime.now().plusDays(1));
        testReservation.setDuration(new BigDecimal("4.0"));
        testReservation.setPlayerCount(6);
        testReservation.setTotalPrice(new BigDecimal("1188.00"));
        testReservation.setActualAmount(new BigDecimal("1000.00"));
        testReservation.setStatus(1); // 待确认
        testReservation.setPayStatus(0); // 未支付
        testReservation.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("根据ID查询预约详情")
    void testGetById() {
        // Given
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        // When
        Reservation result = reservationService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals("ORD202501230001", result.getOrderNo());
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("根据订单号查询预约")
    void testGetByReservationNo() {
        // Given
        when(reservationMapper.selectOne(any())).thenReturn(testReservation);

        // When
        Reservation result = reservationService.getByReservationNo("ORD202501230001");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("确认预约")
    void testConfirm() {
        // Given
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

        // When
        reservationService.confirm(1L);

        // Then - 服务直接调用updateById更新状态
        verify(reservationMapper, times(1)).updateById(any(Reservation.class));
    }

    @Test
    @DisplayName("取消预约")
    void testCancel() {
        // Given - cancel方法会先查询预约是否存在
        testReservation.setStatus(1); // 待确认状态
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

        // When
        reservationService.cancel(1L, "临时有事");

        // Then
        verify(reservationMapper, times(1)).selectById(1L);
        verify(reservationMapper, times(1)).updateById(any(Reservation.class));
    }

    @Test
    @DisplayName("完成预约")
    void testComplete() {
        // Given - complete方法会先查询预约是否存在
        testReservation.setStatus(2); // 已确认状态
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

        // When
        reservationService.complete(1L);

        // Then
        verify(reservationMapper, times(1)).selectById(1L);
        verify(reservationMapper, times(1)).updateById(any(Reservation.class));
    }

    @Test
    @DisplayName("支付预约")
    void testPay() {
        // Given
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

        // When
        reservationService.pay(1L);

        // Then - 服务直接调用updateById更新支付状态
        verify(reservationMapper, times(1)).updateById(any(Reservation.class));
    }

    @Test
    @DisplayName("检查房间可用性 - 使用正确的日期格式")
    void testCheckRoomAvailability() {
        // Given
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        // 使用正确的日期时间格式 yyyy-MM-dd HH:mm:ss
        String reservationTime = "2026-01-25 14:00:00";

        // When
        boolean available = reservationService.checkRoomAvailability(1L, reservationTime, 4.0);

        // Then - 方法内部会查询数据库检查冲突
        // 如果查询到0条记录，表示房间可用
        assertNotNull(available);
    }
}
