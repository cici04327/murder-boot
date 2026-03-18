package com.murder.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.config.AlipayConfig;
import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.CouponService;
import com.murder.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired(required = false)
    private org.springframework.web.client.RestTemplate restTemplate;

    @Autowired(required = false)
    private com.murder.service.UserPointsService userPointsService;

    @Autowired(required = false)
    private com.murder.service.NotificationService notificationService;

    @Autowired(required = false)
    private com.murder.service.AdminNotificationService adminNotificationService;

    @Autowired(required = false)
    private CouponService couponService;

    @Override
    public String createPayment(Long reservationId, String paymentMethod) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (Integer.valueOf(1).equals(reservation.getPayStatus())) {
            throw new RuntimeException("订单已支付");
        }

        if ("mock".equals(paymentMethod)) {
            return handleMockPayment(reservation);
        }
        if ("alipay".equals(paymentMethod)) {
            return alipayConfig.getMockPayment()
                    ? handleMockAlipayPayment(reservation)
                    : createAlipayPayment(reservation);
        }
        if ("wechat".equals(paymentMethod)) {
            return handleMockPayment(reservation);
        }

        throw new RuntimeException("不支持的支付方式");
    }

    private String handleMockPayment(Reservation reservation) {
        markPaid(reservation);
        rewardPointsForPayment(reservation.getUserId(), reservation.getId());
        sendPaymentSuccessNotification(reservation);
        return "MOCK_PAY_SUCCESS";
    }

    private String handleMockAlipayPayment(Reservation reservation) {
        markPaid(reservation);
        rewardPointsForPayment(reservation.getUserId(), reservation.getId());
        sendPaymentSuccessNotification(reservation);
        return "ALIPAY_MOCK_SUCCESS";
    }

    private String createAlipayPayment(Reservation reservation) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(reservation.getOrderNo());
            model.setTotalAmount(defaultAmount(reservation.getActualAmount()).toString());
            model.setSubject("剧本杀预约 - " + reservation.getOrderNo());
            model.setBody("预约时间：" + reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            request.setBizModel(model);

            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付订单失败", e);
            throw new RuntimeException("创建支付订单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            Reservation reservation = verifyAndLoadAlipayReservation(params);
            if (!isAlipayTradeSuccess(params.get("trade_status"))) {
                return "success";
            }

            if (reservation != null && Integer.valueOf(0).equals(reservation.getPayStatus())) {
                markPaid(reservation);
                rewardPointsForPayment(reservation.getUserId(), reservation.getId());
                sendPaymentSuccessNotification(reservation);
            }
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "fail";
        }
    }

    @Override
    public String handleAlipayReturn(Map<String, String> params) {
        try {
            Reservation reservation = verifyAndLoadAlipayReservation(params);
            if (reservation == null) {
                return buildResultRedirect(null, false, "未找到对应预约订单");
            }
            if (!isAlipayTradeSuccess(params.get("trade_status"))) {
                return buildResultRedirect(reservation, false, "支付宝返回的交易状态未成功");
            }

            if (Integer.valueOf(0).equals(reservation.getPayStatus())) {
                markPaid(reservation);
                rewardPointsForPayment(reservation.getUserId(), reservation.getId());
                sendPaymentSuccessNotification(reservation);
            }
            return buildResultRedirect(reservation, true, null);
        } catch (Exception e) {
            log.error("处理支付宝同步回跳失败", e);
            return buildResultRedirect(null, false, e.getMessage());
        }
    }

    private Reservation verifyAndLoadAlipayReservation(Map<String, String> params) {
        try {
            boolean signVerified = com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                throw new RuntimeException("支付宝验签失败");
            }
            String outTradeNo = params.get("out_trade_no");
            if (!StringUtils.hasText(outTradeNo)) {
                throw new RuntimeException("支付宝回调缺少商户订单号");
            }

            Reservation reservation = reservationMapper.selectOne(
                    new LambdaQueryWrapper<Reservation>().eq(Reservation::getOrderNo, outTradeNo)
            );
            if (reservation == null) {
                throw new RuntimeException("未找到对应预约订单");
            }

            String callbackAppId = params.get("app_id");
            if (StringUtils.hasText(callbackAppId) && StringUtils.hasText(alipayConfig.getAppId())
                    && !callbackAppId.equals(alipayConfig.getAppId())) {
                throw new RuntimeException("支付宝回调应用ID不匹配");
            }

            String totalAmount = params.get("total_amount");
            if (StringUtils.hasText(totalAmount)) {
                BigDecimal callbackAmount = new BigDecimal(totalAmount);
                BigDecimal orderAmount = defaultAmount(reservation.getActualAmount());
                if (callbackAmount.compareTo(orderAmount) != 0) {
                    throw new RuntimeException("支付宝回调金额与订单金额不一致");
                }
            }

            return reservation;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("支付宝验签失败", e);
        }
    }

    private boolean isAlipayTradeSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private String buildResultRedirect(Reservation reservation, boolean success, String message) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(alipayConfig.getResultUrl())
                .queryParam("success", success);
        if (reservation != null) {
            builder.queryParam("reservationId", reservation.getId());
            builder.queryParam("orderNo", reservation.getOrderNo());
        }
        if (StringUtils.hasText(message)) {
            builder.queryParam("message", message);
        }
        return builder.build().toUriString();
    }

    @Override
    public Integer queryPaymentStatus(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        return reservation == null ? null : reservation.getPayStatus();
    }

    @Override
    public void applyRefund(Long reservationId, String reason) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!Integer.valueOf(1).equals(reservation.getPayStatus())) {
            throw new RuntimeException("订单未支付或已退款，无法申请退款");
        }
        if (Integer.valueOf(3).equals(reservation.getStatus())) {
            throw new RuntimeException("预约已完成，无法申请退款");
        }
        if (Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new RuntimeException("预约已核销，无法申请退款");
        }
        if (reservation.getRefundStatus() != null && reservation.getRefundStatus() > 0) {
            if (Integer.valueOf(1).equals(reservation.getRefundStatus())) {
                throw new RuntimeException("退款申请处理中，请勿重复提交");
            }
            if (Integer.valueOf(2).equals(reservation.getRefundStatus())) {
                throw new RuntimeException("该订单已退款");
            }
        }

        reservation.setRefundReason(reason);
        reservation.setRefundApplyTime(LocalDateTime.now());
        reservation.setRefundStatus(1);
        reservation.setPayStatus(2);
        reservationMapper.updateById(reservation);

        sendRefundNotificationToAdmin(reservation, reason);
    }

    @Override
    public void processRefund(Long reservationId, Integer approved, String adminRemark) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!Integer.valueOf(1).equals(reservation.getRefundStatus())) {
            throw new RuntimeException("该订单未申请退款或已处理");
        }

        reservation.setRefundProcessTime(LocalDateTime.now());
        reservation.setAdminRemark(adminRemark);

        if (Integer.valueOf(1).equals(approved)) {
            markRefundSuccess(reservation, adminRemark);
        } else {
            reservation.setRefundStatus(3);
            reservation.setPayStatus(1);
            reservationMapper.updateById(reservation);
        }

        sendRefundResultNotificationToUser(reservation, approved);
    }

    @Override
    public void autoRefund(Long reservationId, String reason) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!Integer.valueOf(1).equals(reservation.getPayStatus())) {
            throw new RuntimeException("当前订单未支付，无需自动退款");
        }

        reservation.setRefundReason(reason);
        reservation.setRefundApplyTime(LocalDateTime.now());
        reservation.setRefundProcessTime(LocalDateTime.now());
        markRefundSuccess(reservation, "系统自动退款");
        sendAutoRefundNotificationToUser(reservation, reason);
    }

    private void rewardPointsForPayment(Long userId, Long reservationId) {
        try {
            if (userPointsService != null) {
                userPointsService.addPoints(userId, 100, "完成预约奖励");
                return;
            }
            if (restTemplate != null && userPointsService != null) {
                userPointsService.rewardForReservation(userId, reservationId);
            }
        } catch (Exception e) {
            log.error("支付奖励积分失败: userId={}, reservationId={}", userId, reservationId, e);
        }
    }

    private void sendRefundNotificationToAdmin(Reservation reservation, String reason) {
        if (adminNotificationService == null) {
            return;
        }
        adminNotificationService.sendNotification(
                "退款申请提醒",
                String.format("收到新的退款申请，订单号：%s，原因：%s", reservation.getOrderNo(), reason),
                2,
                "refund",
                reservation.getId(),
                reservation.getStoreId(),
                3
        );
    }

    private void sendRefundResultNotificationToUser(Reservation reservation, Integer approved) {
        if (notificationService == null) {
            return;
        }

        String title = Integer.valueOf(1).equals(approved) ? "退款成功通知" : "退款拒绝通知";
        String content = Integer.valueOf(1).equals(approved)
                ? String.format("您的退款申请已通过，订单号：%s，退款金额将原路退回。", reservation.getOrderNo())
                : String.format(
                "您的退款申请已被拒绝，订单号：%s，原因：%s",
                reservation.getOrderNo(),
                StringUtils.hasText(reservation.getAdminRemark()) ? reservation.getAdminRemark() : "暂无"
        );

        notificationService.sendToUsers(
                title,
                content,
                Integer.valueOf(1).equals(approved) ? 5 : 6,
                "refund",
                reservation.getId(),
                reservation.getUserId()
        );
    }

    private void sendAutoRefundNotificationToUser(Reservation reservation, String reason) {
        if (notificationService == null) {
            return;
        }

        String content = String.format(
                "您的拼单预约已自动退款，订单号：%s，原因：%s，退款金额：%.2f。",
                reservation.getOrderNo(),
                reason,
                defaultAmount(reservation.getActualAmount())
        );

        notificationService.sendToUsers(
                "拼单未成团自动退款通知",
                content,
                5,
                "group",
                reservation.getGroupId() != null ? reservation.getGroupId() : reservation.getId(),
                reservation.getUserId()
        );
    }

    private void sendPaymentSuccessNotification(Reservation reservation) {
        if (notificationService == null) {
            return;
        }

        String reservationTime = reservation.getReservationTime() == null
                ? ""
                : reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String content = String.format(
                "您已成功支付预约订单，订单号：%s，金额：%.2f，预约时间：%s，到店请出示核销码：%s。",
                reservation.getOrderNo(),
                defaultAmount(reservation.getActualAmount()),
                reservationTime,
                reservation.getCheckInCode()
        );

        notificationService.sendToUsers(
                "支付成功通知",
                content,
                3,
                "payment",
                reservation.getId(),
                reservation.getUserId()
        );
    }

    private void deductPointsForRefund(Long userId, Long reservationId) {
        try {
            if (userPointsService != null) {
                userPointsService.deductPoints(userId, 100, "退款扣减积分");
                return;
            }
            if (restTemplate != null && userPointsService != null) {
                userPointsService.deductForRefund(userId, reservationId);
            }
        } catch (Exception e) {
            log.error("退款扣减积分失败: userId={}, reservationId={}", userId, reservationId, e);
        }
    }

    private void markRefundSuccess(Reservation reservation, String adminRemark) {
        reservation.setRefundStatus(2);
        reservation.setPayStatus(3);
        reservation.setStatus(4);
        reservation.setAdminRemark(adminRemark);
        reservationMapper.updateById(reservation);
        refundCouponQuietly(reservation.getId());
        deductPointsForRefund(reservation.getUserId(), reservation.getId());
    }

    private void markPaid(Reservation reservation) {
        reservation.setPayStatus(1);
        reservation.setPayTime(LocalDateTime.now());
        reservation.setStatus(2);
        if (reservation.getCheckInStatus() == null) {
            reservation.setCheckInStatus(0);
        }
        if (!StringUtils.hasText(reservation.getCheckInCode())) {
            reservation.setCheckInCode(generateCheckInCode());
        }
        reservationMapper.updateById(reservation);
    }

    private void refundCouponQuietly(Long reservationId) {
        if (couponService == null) {
            return;
        }
        try {
            couponService.refundCoupon(reservationId);
        } catch (Exception e) {
            log.warn("退款时返还优惠券失败: reservationId={}", reservationId, e);
        }
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
