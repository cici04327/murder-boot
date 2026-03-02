package com.murder.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.murder.entity.Reservation;
import com.murder.common.config.AlipayConfig;
import com.murder.mapper.ReservationMapper;
import com.murder.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 支付服务实现?
 */
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

    /**
     * 创建支付订单
     */
    @Override
    public String createPayment(Long reservationId, String paymentMethod) {
        // 查询预约信息
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        
        if (reservation.getPayStatus() == 1) {
            throw new RuntimeException("订单已支付");
        }
        
        // 根据支付方式处理
        if ("mock".equals(paymentMethod)) {
            // 模拟支付：直接标记为已支?
            return handleMockPayment(reservation);
        } else if ("alipay".equals(paymentMethod)) {
            // 支付宝支?
            if (alipayConfig.getMockPayment()) {
                // 开发环境：模拟支付宝支?
                return handleMockAlipayPayment(reservation);
            } else {
                // 生产环境：真实支付宝支付
                return createAlipayPayment(reservation);
            }
        } else if ("wechat".equals(paymentMethod)) {
            // 微信支付（暂未实现）
            return handleMockPayment(reservation);
        }
        
        throw new RuntimeException("不支持的支付方式");
    }

    /**
     * 模拟支付
     */
    private String handleMockPayment(Reservation reservation) {
        // 更新支付状?
        reservation.setPayStatus(1);
        reservation.setPayTime(LocalDateTime.now());
        reservation.setStatus(2); // 已确?
        reservationMapper.updateById(reservation);
        
        log.info("模拟支付成功: reservationId={}, orderNo={}", 
                reservation.getId(), reservation.getOrderNo());
        
        // 支付成功后奖励积?
        rewardPointsForPayment(reservation.getUserId(), reservation.getId());
        
        // 发送支付成功通知
        sendPaymentSuccessNotification(reservation);
        
        return "MOCK_PAY_SUCCESS";
    }
    
    /**
     * 模拟支付宝支?
     */
    private String handleMockAlipayPayment(Reservation reservation) {
        // 开发环境：直接返回成功，实际项目中可以返回一个模拟支付页?
        reservation.setPayStatus(1);
        reservation.setPayTime(LocalDateTime.now());
        reservation.setStatus(2); // 已确?
        reservationMapper.updateById(reservation);
        
        log.info("模拟支付宝支付成? reservationId={}, orderNo={}", 
                reservation.getId(), reservation.getOrderNo());
        
        // 支付成功后奖励积?
        rewardPointsForPayment(reservation.getUserId(), reservation.getId());
        
        // 发送支付成功通知
        sendPaymentSuccessNotification(reservation);
        
        return "ALIPAY_MOCK_SUCCESS";
    }

    /**
     * 创建真实支付宝支?
     */
    private String createAlipayPayment(Reservation reservation) {
        try {
            // 创建API请求对象
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            
            // 设置回调地址
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());
            
            // 设置业务参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(reservation.getOrderNo()); // 商户订单?
            model.setTotalAmount(reservation.getActualAmount().toString()); // 订单金额
            model.setSubject("剧本杀预约 - " + reservation.getOrderNo()); // 订单标题
            model.setBody("预约时间：" + reservation.getReservationTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))); // 订单描述
            model.setProductCode("FAST_INSTANT_TRADE_PAY"); // 产品码
            
            request.setBizModel(model);
            
            // 调用SDK生成支付表单
            String form = alipayClient.pageExecute(request).getBody();
            
            log.info("创建支付宝支付订单成? reservationId={}, orderNo={}", 
                    reservation.getId(), reservation.getOrderNo());
            
            return form;
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付订单失败", e);
            throw new RuntimeException("创建支付订单失败: " + e.getMessage());
        }
    }

    /**
     * 处理支付宝异步通知
     */
    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            // 验证支付宝签?
            log.info("支付宝回调通知参数: {}", params);
            
            try {
                boolean signVerified = com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
                );
                
                if (!signVerified) {
                    log.error("支付宝签名验证失败");
                    return "failure";
                }
                log.info("支付宝签名验证成功");
            } catch (AlipayApiException e) {
                log.error("支付宝签名验证异常", e);
                return "failure";
            }
            
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 查询订单
                Reservation reservation = reservationMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation>()
                                .eq(Reservation::getOrderNo, outTradeNo)
                );
                
                if (reservation != null && reservation.getPayStatus() == 0) {
                    // 更新支付状?
                    reservation.setPayStatus(1);
                    reservation.setPayTime(LocalDateTime.now());
                    reservation.setStatus(2); // 已确?
                    reservationMapper.updateById(reservation);
                    
                    log.info("支付宝支付成? orderNo={}", outTradeNo);
                    
                    // 支付成功后奖励积?
                    rewardPointsForPayment(reservation.getUserId(), reservation.getId());
                    
                    // 发送支付成功通知
                    sendPaymentSuccessNotification(reservation);
                }
            }
            
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "fail";
        }
    }

    /**
     * 查询支付状?
     */
    @Override
    public Integer queryPaymentStatus(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        return reservation != null ? reservation.getPayStatus() : null;
    }
    
    /**
     * 支付成功后奖励积?
     */
    private void rewardPointsForPayment(Long userId, Long reservationId) {
        if (restTemplate == null) {
            log.error("RestTemplate未注入，无法调用积分服务");
            return;
        }
        
        try {
            // 调用用户服务的积分接口（用户服务端口?082?
            if (userPointsService != null) {
                userPointsService.addPoints(userId, 100, "完成预约奖励");
                log.info("支付成功奖励积分成功: userId={}, reservationId={}", userId, reservationId);
            } else if (restTemplate != null) {
                // 单体版本：直接调用本地积分服务
                userPointsService.rewardForReservation(userId, reservationId);
                log.info("支付成功奖励积分成功(本地): userId={}, reservationId={}", userId, reservationId);
            }
        } catch (Exception e) {
            log.error("奖励积分失败，但不影响支付流? userId={}, reservationId={}, error={}", userId, reservationId, e.getMessage(), e);
        }
    }
    
    /**
     * 申请退款（用户端）
     */
    @Override
    public void applyRefund(Long reservationId, String reason) {
        // 查询预约信息
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        
        // 检查支付状态
        if (reservation.getPayStatus() != 1) {
            throw new RuntimeException("订单未支付或已退款，无法申请退款");
        }
        
        // 检查预约状态
        if (reservation.getStatus() == 3) {
            throw new RuntimeException("预约已完成，无法申请退款");
        }
        
        // 检查是否已经在退款中
        if (reservation.getRefundStatus() != null && reservation.getRefundStatus() > 0) {
            if (reservation.getRefundStatus() == 1) {
                throw new RuntimeException("退款申请处理中，请勿重复提交");
            } else if (reservation.getRefundStatus() == 2) {
                throw new RuntimeException("该订单已退款");
            }
        }
        
        // 更新退款信?
        reservation.setRefundReason(reason);
        reservation.setRefundApplyTime(LocalDateTime.now());
        reservation.setRefundStatus(1); // 退款中
        reservation.setPayStatus(2); // 退款中
        reservationMapper.updateById(reservation);
        
        log.info("用户申请退? reservationId={}, reason={}", reservationId, reason);
        
        // 发送通知给管理员
        try {
            sendRefundNotificationToAdmin(reservation, reason);
        } catch (Exception e) {
            log.error("发送退款通知给管理员失败", e);
        }
    }
    
    /**
     * 处理退款（管理端）
     */
    @Override
    public void processRefund(Long reservationId, Integer approved, String adminRemark) {
        // 查询预约信息
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        
        // 检查退款状态
        if (reservation.getRefundStatus() != 1) {
            throw new RuntimeException("该订单未申请退款或已处理");
        }
        
        reservation.setRefundProcessTime(LocalDateTime.now());
        reservation.setAdminRemark(adminRemark);
        
        if (approved == 1) {
            // 同意退?
            reservation.setRefundStatus(2); // 退款成?
            reservation.setPayStatus(3); // 已退?
            reservation.setStatus(4); // 已取?
            
            log.info("管理员同意退? reservationId={}", reservationId);
            
            // 扣除已奖励的积分
            deductPointsForRefund(reservation.getUserId(), reservationId);
        } else {
            // 拒绝退?
            reservation.setRefundStatus(3); // 退款失?
            reservation.setPayStatus(1); // 恢复为已支付
            
            log.info("管理员拒绝退? reservationId={}, reason={}", reservationId, adminRemark);
        }
        
        reservationMapper.updateById(reservation);
        
        // 发送通知给用?
        try {
            sendRefundResultNotificationToUser(reservation, approved);
        } catch (Exception e) {
            log.error("发送退款结果通知给用户失败", e);
        }
    }
    
    /**
     * 发送退款申请通知给管理员
     */
    private void sendRefundNotificationToAdmin(Reservation reservation, String reason) {
        try {
            String title = "退款申请提醒";
            String content = String.format("收到新的退款申请！预约编号?s，申请原因：%s", 
                    reservation.getOrderNo(), reason);
            
            // 单体版本：直接调用本地管理端通知服务
            if (adminNotificationService != null) {
                try {
                    adminNotificationService.sendNotification(
                            title,
                            content,
                            2,
                            "refund",
                            reservation.getId(),
                            reservation.getStoreId(),
                            3
                    );
                    log.info("发送退款申请通知给管理员(本地): reservationId={}", reservation.getId());
                } catch (Exception e) {
                    log.error("发送退款申请通知给管理员失败", e);
                }
            }
        } catch (Exception e) {
            log.error("发送管理端通知失败", e);
        }
    }
    
    /**
     * 发送退款结果通知给用?
     */
    private void sendRefundResultNotificationToUser(Reservation reservation, Integer approved) {
        if (notificationService == null) {
            log.warn("NotificationService未配置，无法发送通知");
            return;
        }
        
        try {
            String title = approved == 1 ? "退款成功通知" : "退款拒绝通知";
            String content = approved == 1 
                    ? String.format("您的退款申请已通过！预约编号：%s，退款金额将原路退回", reservation.getOrderNo())
                    : String.format("您的退款申请已被拒绝！预约编号：%s，拒绝原因：%s", 
                            reservation.getOrderNo(), 
                            reservation.getAdminRemark() != null ? reservation.getAdminRemark() : "未说明");
            
            Map<String, Object> notificationData = new java.util.HashMap<>();
            notificationData.put("title", title);
            notificationData.put("content", content);
            notificationData.put("type", approved == 1 ? 5 : 6); // 5:退款成功，6:退款拒?
            notificationData.put("bizType", "refund");
            notificationData.put("bizId", reservation.getId());
            notificationData.put("userIds", new Long[]{reservation.getUserId()});
            
            // 单体版本：直接调用本地通知服务
            notificationService.sendToUsers(
                    title,
                    content,
                    approved == 1 ? 5 : 6,
                    "refund",
                    reservation.getId(),
                    reservation.getUserId()
            );
            
            log.info("发送退款结果通知给用? userId={}, approved={}", reservation.getUserId(), approved);
        } catch (Exception e) {
            log.error("发送用户通知失败", e);
        }
    }
    
    /**
     * 发送支付成功通知给用?
     */
    private void sendPaymentSuccessNotification(Reservation reservation) {
        if (notificationService == null) {
            log.warn("NotificationService未配置，无法发送通知");
            return;
        }
        
        try {
            String title = "支付成功通知";
            String content = String.format("您已成功支付预约订单！预约编号：%s，支付金额：¥%.2f，预约时间：%s，请准时到场", 
                    reservation.getOrderNo(),
                    reservation.getActualAmount(),
                    reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            
            Map<String, Object> notificationData = new java.util.HashMap<>();
            notificationData.put("title", title);
            notificationData.put("content", content);
            notificationData.put("type", 3); // 3:支付成功
            notificationData.put("bizType", "payment");
            notificationData.put("bizId", reservation.getId());
            notificationData.put("userIds", new Long[]{reservation.getUserId()});
            
            // 单体版本：直接调用本地通知服务
            notificationService.sendToUsers(
                    title,
                    content,
                    3,
                    "payment",
                    reservation.getId(),
                    reservation.getUserId()
            );
            
            log.info("发送支付成功通知给用? userId={}, reservationId={}", reservation.getUserId(), reservation.getId());
        } catch (Exception e) {
            log.error("发送支付成功通知失败", e);
        }
    }
    
    /**
     * 退款时扣除积分
     */
    private void deductPointsForRefund(Long userId, Long reservationId) {
        if (restTemplate == null) {
            log.error("RestTemplate未注入，无法调用积分服务");
            return;
        }
        
        try {
            // 调用用户服务扣除积分接口（用户服务端口：8082?
            if (userPointsService != null) {
                userPointsService.deductPoints(userId, 100, "退款扣除积分");
                log.info("退款扣除积分成? userId={}, reservationId={}", userId, reservationId);
            } else if (restTemplate != null) {
                // 单体版本：直接调用本地积分服务
                userPointsService.deductForRefund(userId, reservationId);
                log.info("退款扣除积分成功(本地): userId={}, reservationId={}", userId, reservationId);
            }

        } catch (Exception e) {
            log.error("扣除积分失败，但不影响退款流? userId={}, reservationId={}, error={}", userId, reservationId, e.getMessage(), e);
        }
    }
}

