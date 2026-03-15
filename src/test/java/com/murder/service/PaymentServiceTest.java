package com.murder.service;

import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.impl.PaymentServiceImpl;
import com.murder.common.config.AlipayConfig;
import com.alipay.api.AlipayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * 支付服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("支付服务测试")
class PaymentServiceTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private AlipayClient alipayClient;

    @Mock
    private AlipayConfig alipayConfig;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private CouponService couponService;

    @Mock
    private UserPointsService userPointsService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setOrderNo("TEST202601010001");
        testReservation.setUserId(1L);
        testReservation.setStoreId(1L);
        testReservation.setActualAmount(new BigDecimal("198.00"));
        testReservation.setTotalPrice(new BigDecimal("198.00"));
        testReservation.setPayStatus(0); // 未支付
        testReservation.setStatus(0);
        testReservation.setCheckInStatus(0);
        testReservation.setReservationTime(LocalDateTime.now().plusDays(1));

        when(alipayConfig.getMockPayment()).thenReturn(true);
    }

    @Nested
    @DisplayName("创建支付测试")
    class CreatePaymentTests {

        @Test
        @DisplayName("mock支付 - 成功")
        void createPayment_Mock_Success() {
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            String result = paymentService.createPayment(1L, "mock");

            assertEquals("MOCK_PAY_SUCCESS", result);
            verify(reservationMapper, times(1)).updateById(any(Reservation.class));
        }

        @Test
        @DisplayName("alipay支付 - mock模式成功")
        void createPayment_Alipay_MockMode_Success() {
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);
            when(alipayConfig.getMockPayment()).thenReturn(true);

            String result = paymentService.createPayment(1L, "alipay");

            assertEquals("ALIPAY_MOCK_SUCCESS", result);
        }

        @Test
        @DisplayName("wechat支付 - mock模式成功")
        void createPayment_Wechat_Success() {
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            String result = paymentService.createPayment(1L, "wechat");

            assertEquals("MOCK_PAY_SUCCESS", result);
        }

        @Test
        @DisplayName("创建支付 - 预约不存在")
        void createPayment_ReservationNotFound() {
            when(reservationMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> paymentService.createPayment(99L, "mock"));
        }

        @Test
        @DisplayName("创建支付 - 订单已支付")
        void createPayment_AlreadyPaid() {
            testReservation.setPayStatus(1);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.createPayment(1L, "mock"));
        }

        @Test
        @DisplayName("创建支付 - 不支持的支付方式")
        void createPayment_UnsupportedMethod() {
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.createPayment(1L, "bitcoin"));
        }
    }

    @Nested
    @DisplayName("查询支付状态测试")
    class QueryPaymentStatusTests {

        @Test
        @DisplayName("查询支付状态 - 未支付")
        void queryPaymentStatus_Unpaid() {
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            Integer status = paymentService.queryPaymentStatus(1L);

            assertEquals(0, status);
        }

        @Test
        @DisplayName("查询支付状态 - 已支付")
        void queryPaymentStatus_Paid() {
            testReservation.setPayStatus(1);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            Integer status = paymentService.queryPaymentStatus(1L);

            assertEquals(1, status);
        }

        @Test
        @DisplayName("查询支付状态 - 预约不存在返回null")
        void queryPaymentStatus_NotFound() {
            when(reservationMapper.selectById(99L)).thenReturn(null);

            Integer status = paymentService.queryPaymentStatus(99L);

            assertNull(status);
        }
    }

    @Nested
    @DisplayName("申请退款测试")
    class ApplyRefundTests {

        @Test
        @DisplayName("申请退款 - 成功")
        void applyRefund_Success() {
            testReservation.setPayStatus(1);
            testReservation.setStatus(1);
            testReservation.setCheckInStatus(0);
            testReservation.setRefundStatus(0);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);

            paymentService.applyRefund(1L, "临时有事");

            verify(reservationMapper, times(1)).updateById(reservationCaptor.capture());
            verify(adminNotificationService, times(1)).sendNotification(
                    eq("退款申请提醒"),
                    contains("TEST202601010001"),
                    eq(2),
                    eq("refund"),
                    eq(1L),
                    eq(1L),
                    eq(3)
            );

            Reservation updated = reservationCaptor.getValue();
            assertEquals("临时有事", updated.getRefundReason());
            assertEquals(1, updated.getRefundStatus());
            assertEquals(2, updated.getPayStatus());
            assertNotNull(updated.getRefundApplyTime());
        }

        @Test
        @DisplayName("申请退款 - 预约不存在")
        void applyRefund_NotFound() {
            when(reservationMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> paymentService.applyRefund(99L, "原因"));
        }

        @Test
        @DisplayName("申请退款 - 订单未支付")
        void applyRefund_NotPaid() {
            testReservation.setPayStatus(0);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.applyRefund(1L, "原因"));
        }

        @Test
        @DisplayName("申请退款 - 已完成预约不可退")
        void applyRefund_Completed() {
            testReservation.setPayStatus(1);
            testReservation.setStatus(3); // 已完成
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.applyRefund(1L, "原因"));
        }

        @Test
        @DisplayName("申请退款 - 已核销不可退")
        void applyRefund_CheckedIn() {
            testReservation.setPayStatus(1);
            testReservation.setStatus(1);
            testReservation.setCheckInStatus(1); // 已核销
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.applyRefund(1L, "原因"));
        }

        @Test
        @DisplayName("申请退款 - 重复申请")
        void applyRefund_Duplicate() {
            testReservation.setPayStatus(1);
            testReservation.setStatus(1);
            testReservation.setCheckInStatus(0);
            testReservation.setRefundStatus(1); // 已申请
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.applyRefund(1L, "原因"));
        }
    }

    @Nested
    @DisplayName("处理退款测试")
    class ProcessRefundTests {

        @Test
        @DisplayName("审批退款 - 同意")
        void processRefund_Approved() {
            testReservation.setRefundStatus(1); // 待审批
            testReservation.setPayStatus(2);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);

            paymentService.processRefund(1L, 1, "同意退款");

            verify(reservationMapper, times(1)).updateById(reservationCaptor.capture());
            verify(couponService, times(1)).refundCoupon(1L);
            verify(userPointsService, times(1)).deductPoints(1L, 100, "退款扣减积分");
            verify(notificationService, times(1)).sendToUsers(
                    eq("退款成功通知"),
                    contains("TEST202601010001"),
                    eq(5),
                    eq("refund"),
                    eq(1L),
                    eq(1L)
            );

            Reservation updated = reservationCaptor.getValue();
            assertEquals(2, updated.getRefundStatus());
            assertEquals(3, updated.getPayStatus());
            assertEquals(4, updated.getStatus());
            assertEquals("同意退款", updated.getAdminRemark());
            assertNotNull(updated.getRefundProcessTime());
        }

        @Test
        @DisplayName("审批退款 - 拒绝")
        void processRefund_Rejected() {
            testReservation.setRefundStatus(1); // 待审批
            testReservation.setPayStatus(2);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);

            paymentService.processRefund(1L, 0, "不符合退款条件");

            verify(reservationMapper, times(1)).updateById(reservationCaptor.capture());
            verify(couponService, never()).refundCoupon(anyLong());
            verify(userPointsService, never()).deductPoints(anyLong(), anyInt(), anyString());
            verify(notificationService, times(1)).sendToUsers(
                    eq("退款拒绝通知"),
                    contains("不符合退款条件"),
                    eq(6),
                    eq("refund"),
                    eq(1L),
                    eq(1L)
            );

            Reservation updated = reservationCaptor.getValue();
            assertEquals(3, updated.getRefundStatus());
            assertEquals(1, updated.getPayStatus());
            assertEquals("不符合退款条件", updated.getAdminRemark());
            assertNotNull(updated.getRefundProcessTime());
        }

        @Test
        @DisplayName("处理退款 - 预约不存在")
        void processRefund_NotFound() {
            when(reservationMapper.selectById(99L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> paymentService.processRefund(99L, 1, ""));
        }

        @Test
        @DisplayName("处理退款 - 未申请退款")
        void processRefund_NotApplied() {
            testReservation.setRefundStatus(0);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.processRefund(1L, 1, ""));
        }
    }

    @Nested
    @DisplayName("自动退款测试")
    class AutoRefundTests {

        @Test
        @DisplayName("自动退款 - 成功")
        void autoRefund_Success() {
            testReservation.setPayStatus(1);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);

            paymentService.autoRefund(1L, "拼单未成团");

            verify(reservationMapper, times(1)).updateById(reservationCaptor.capture());
            verify(couponService, times(1)).refundCoupon(1L);
            verify(userPointsService, times(1)).deductPoints(1L, 100, "退款扣减积分");
            verify(notificationService, times(1)).sendToUsers(
                    eq("拼单未成团自动退款通知"),
                    contains("拼单未成团"),
                    eq(5),
                    eq("group"),
                    eq(1L),
                    eq(1L)
            );

            Reservation updated = reservationCaptor.getValue();
            assertEquals("拼单未成团", updated.getRefundReason());
            assertEquals(2, updated.getRefundStatus());
            assertEquals(3, updated.getPayStatus());
            assertEquals(4, updated.getStatus());
            assertEquals("系统自动退款", updated.getAdminRemark());
            assertNotNull(updated.getRefundApplyTime());
            assertNotNull(updated.getRefundProcessTime());
        }

        @Test
        @DisplayName("自动退款 - 未支付订单不可退")
        void autoRefund_NotPaid() {
            testReservation.setPayStatus(0);
            when(reservationMapper.selectById(1L)).thenReturn(testReservation);

            assertThrows(RuntimeException.class, () -> paymentService.autoRefund(1L, "原因"));
        }
    }
}
