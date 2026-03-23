package com.murder.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.config.AlipayConfig;
import com.murder.common.context.BaseContext;
import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.CouponService;
import com.murder.service.PaymentService;
import com.murder.service.ScriptScheduleService;
import com.murder.service.StoreEmployeeOperationLogService;
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

    @Autowired(required = false)
    private ScriptScheduleService scriptScheduleService;

    @Autowired(required = false)
    private StoreEmployeeOperationLogService storeEmployeeOperationLogService;

    @Override
    public String createPayment(Long reservationId, String paymentMethod) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertPayableReservation(reservation);

        String normalizedMethod = StringUtils.hasText(paymentMethod)
                ? paymentMethod.trim().toLowerCase()
                : "alipay";
        if (!"alipay".equals(normalizedMethod)) {
            throw new RuntimeException("当前仅支持支付宝支付");
        }
        return createAlipayPayment(reservation);
    }

    private void assertPayableReservation(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        if (Integer.valueOf(1).equals(reservation.getPayStatus())) {
            throw new RuntimeException("订单已支付");
        }
        if (Integer.valueOf(4).equals(reservation.getStatus())) {
            throw new RuntimeException("已取消订单不能支付");
        }
        if (Integer.valueOf(3).equals(reservation.getStatus())) {
            throw new RuntimeException("已完成订单不能支付");
        }
        if (Integer.valueOf(1).equals(reservation.getRefundStatus()) || Integer.valueOf(2).equals(reservation.getPayStatus())) {
            throw new RuntimeException("退款处理中订单不能支付");
        }
        if (Integer.valueOf(2).equals(reservation.getRefundStatus()) || Integer.valueOf(3).equals(reservation.getPayStatus())) {
            throw new RuntimeException("已退款订单不能支付");
        }
        Integer payStatus = reservation.getPayStatus();
        if (payStatus != null && !Integer.valueOf(0).equals(payStatus)) {
            throw new RuntimeException("当前订单状态不支持支付");
        }
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
            if (!isAlipayReturnSuccess(params, reservation)) {
                return buildResultRedirect(reservation, false, "支付宝支付结果确认中，请稍后查看订单状态");
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

    private boolean isAlipayReturnSuccess(Map<String, String> params, Reservation reservation) {
        if (isAlipayTradeSuccess(params.get("trade_status"))) {
            return true;
        }
        if (reservation != null && Integer.valueOf(1).equals(reservation.getPayStatus())) {
            return true;
        }
        return StringUtils.hasText(params.get("trade_no"));
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
        return builder.build().encode().toUriString();
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
        assertAdminRefundScope(reservation);
        if (!Integer.valueOf(1).equals(reservation.getRefundStatus())) {
            throw new RuntimeException("该订单未申请退款或已处理");
        }

        reservation.setRefundProcessTime(LocalDateTime.now());
        reservation.setAdminRemark(adminRemark);

        if (Integer.valueOf(1).equals(approved)) {
            executeRefund(reservation, reservation.getRefundReason(), adminRemark);
        } else {
            reservation.setRefundStatus(3);
            reservation.setPayStatus(1);
            reservationMapper.updateById(reservation);
        }
        recordRefundOperation(reservation, Integer.valueOf(1).equals(approved) ? "REFUND_APPROVE" : "REFUND_REJECT",
                Integer.valueOf(1).equals(approved) ? "同意退款" : "拒绝退款");

        try {
            sendRefundResultNotificationToUser(reservation, approved);
        } catch (Exception e) {
            log.warn("发送退款结果通知失败: reservationId={}, approved={}", reservationId, approved, e);
        }
    }

    private void assertAdminRefundScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role)) {
            return;
        }
        if ("STORE_ADMIN".equals(role)) {
            Long currentStoreId = BaseContext.getStoreId();
            if (currentStoreId == null) {
                throw new SecurityException("门店管理员未绑定门店");
            }
            if (reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
                throw new SecurityException("没有权限处理该门店的退款申请");
            }
            return;
        }
        if (!"STORE_STAFF".equals(role) || !hasPermission("refund:process")) {
            throw new SecurityException("没有权限处理该退款申请");
        }

        Long currentStoreId = BaseContext.getStoreId();
        if (currentStoreId == null) {
            throw new SecurityException("当前员工账号未绑定门店");
        }
        if (reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
            throw new SecurityException("没有权限处理该门店的退款申请");
        }
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
        executeRefund(reservation, reason, "系统自动退款");
        try {
            sendAutoRefundNotificationToUser(reservation, reason);
        } catch (Exception e) {
            log.warn("发送自动退款通知失败: reservationId={}", reservationId, e);
        }
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

        boolean groupFailed = StringUtils.hasText(reason) && reason.contains("拼单未成团");
        String title = groupFailed ? "拼单未成团自动退款通知" : "预约自动退款通知";
        String content = String.format(
                "您的预约订单已自动退款，订单号：%s，原因：%s，退款金额：%.2f。",
                reservation.getOrderNo(),
                reason,
                defaultAmount(reservation.getActualAmount())
        );

        notificationService.sendToUsers(
                title,
                content,
                5,
                "group",
                reservation.getGroupId() != null ? reservation.getGroupId() : reservation.getId(),
                reservation.getUserId()
        );
    }

    private boolean hasPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (!StringUtils.hasText(permissionCodes) || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        for (String permission : permissionCodes.split(",")) {
            if (permissionCode.equals(permission != null ? permission.trim() : null)) {
                return true;
            }
        }
        return false;
    }

    private void recordRefundOperation(Reservation reservation, String actionType, String detail) {
        if (storeEmployeeOperationLogService == null || reservation == null) {
            return;
        }
        try {
            storeEmployeeOperationLogService.record(
                    reservation.getStoreId(),
                    actionType,
                    "RESERVATION",
                    reservation.getId(),
                    reservation.getOrderNo(),
                    detail
            );
        } catch (Exception ignored) {
            // ignore
        }
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

    private void executeRefund(Reservation reservation, String refundReason, String adminRemark) {
        if (reservation != null && !StringUtils.hasText(reservation.getRemark()) && StringUtils.hasText(refundReason)) {
            reservation.setRemark(refundReason);
        }
        callAlipayRefundIfNecessary(reservation, refundReason);
        markRefundSuccess(reservation, adminRemark);
    }

    private void callAlipayRefundIfNecessary(Reservation reservation, String refundReason) {
        if (reservation == null || Boolean.TRUE.equals(alipayConfig.getMockPayment())) {
            return;
        }

        BigDecimal refundAmount = defaultAmount(reservation.getActualAmount());
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("退款金额为0，跳过支付宝退款调用: reservationId={}, orderNo={}",
                    reservation.getId(), reservation.getOrderNo());
            return;
        }
        if (!StringUtils.hasText(reservation.getOrderNo())) {
            throw new RuntimeException("缺少商户订单号，无法调用支付宝退款");
        }

        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(reservation.getOrderNo());
            model.setRefundAmount(refundAmount.toString());
            model.setRefundReason(StringUtils.hasText(refundReason) ? refundReason : "预约退款");
            request.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                String errorMessage = firstNonBlank(response.getSubMsg(), response.getMsg(), "支付宝退款失败");
                log.error("支付宝退款失败: reservationId={}, orderNo={}, code={}, subCode={}, message={}",
                        reservation.getId(), reservation.getOrderNo(),
                        response.getCode(), response.getSubCode(), errorMessage);
                throw new RuntimeException(errorMessage);
            }

            log.info("支付宝退款成功: reservationId={}, orderNo={}, refundAmount={}, fundChange={}",
                    reservation.getId(), reservation.getOrderNo(), refundAmount, response.getFundChange());
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款接口失败: reservationId={}, orderNo={}",
                    reservation.getId(), reservation.getOrderNo(), e);
            throw new RuntimeException("调用支付宝退款接口失败: " + e.getMessage(), e);
        }
    }

    private void markRefundSuccess(Reservation reservation, String adminRemark) {
        reservation.setRefundStatus(2);
        reservation.setPayStatus(3);
        reservation.setStatus(4);
        reservation.setAdminRemark(adminRemark);
        if (reservation.getRefundProcessTime() == null) {
            reservation.setRefundProcessTime(LocalDateTime.now());
        }
        reservationMapper.updateById(reservation);
        rollbackSchedulePlayersQuietly(reservation);
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

    private void rollbackSchedulePlayersQuietly(Reservation reservation) {
        if (reservation == null || reservation.getScheduleId() == null || scriptScheduleService == null) {
            return;
        }
        try {
            scriptScheduleService.decrementCurrentPlayers(
                    reservation.getScheduleId(),
                    resolvePlayerCount(reservation));
        } catch (Exception e) {
            log.warn("退款成功后回退排期人数失败: reservationId={}, scheduleId={}",
                    reservation.getId(), reservation.getScheduleId(), e);
        }
    }

    private int resolvePlayerCount(Reservation reservation) {
        return reservation != null && reservation.getPlayerCount() != null && reservation.getPlayerCount() > 0
                ? reservation.getPlayerCount()
                : 1;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
