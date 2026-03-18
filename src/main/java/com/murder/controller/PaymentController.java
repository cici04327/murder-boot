package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.Result;
import com.murder.service.PaymentService;
import com.murder.service.ReservationService;
import com.murder.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/reservation/payment")
@Tag(name = "支付接口")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReservationService reservationService;

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付订单")
    public Result<String> createPayment(
            @RequestParam Long reservationId,
            @RequestParam(defaultValue = "mock") String paymentMethod) {
        log.info("创建支付订单: reservationId={}, paymentMethod={}", reservationId, paymentMethod);
        // 校验预约归属：只能为自己的预约付款
        Long currentUserId = BaseContext.getCurrentId();
        ReservationVO vo = reservationService.getDetailById(reservationId);
        if (vo == null || !Objects.equals(currentUserId, vo.getUserId())) {
            log.warn("用户{}尝试为不属于自己的预约{}创建支付", currentUserId, reservationId);
            return Result.error("无权操作该预约");
        }
        String result = paymentService.createPayment(reservationId, paymentMethod);
        return Result.success(result);
    }

    /**
     * 支付宝异步通知
     */
    @PostMapping("/notify")
    @Operation(summary = "支付宝异步通知")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("收到支付宝异步通知: {}", params);
        return paymentService.handleAlipayNotify(params);
    }

    /**
     * 支付宝同步回跳
     */
    @GetMapping("/return")
    @Operation(summary = "支付宝同步回跳")
    public void alipayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        log.info("收到支付宝同步回跳: {}", params);
        response.sendRedirect(paymentService.handleAlipayReturn(params));
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status/{reservationId}")
    @Operation(summary = "查询支付状态")
    public Result<Integer> queryPaymentStatus(@PathVariable Long reservationId) {
        log.info("查询支付状态: reservationId={}", reservationId);
        // 校验预约归属
        Long currentUserId = BaseContext.getCurrentId();
        ReservationVO vo = reservationService.getDetailById(reservationId);
        if (vo == null || !Objects.equals(currentUserId, vo.getUserId())) {
            log.warn("用户{}尝试查询不属于自己的预约{}支付状态", currentUserId, reservationId);
            return Result.error("无权查询该预约");
        }
        Integer status = paymentService.queryPaymentStatus(reservationId);
        return Result.success(status);
    }
    
    /**
     * 申请退款（用户端）
     */
    @PostMapping("/refund/apply")
    @Operation(summary = "申请退款")
    public Result<String> applyRefund(
            @RequestParam Long reservationId,
            @RequestParam String reason) {
        log.info("申请退款: reservationId={}, reason={}", reservationId, reason);
        // 校验预约归属：只能为自己的预约申请退款
        Long currentUserId = BaseContext.getCurrentId();
        ReservationVO vo = reservationService.getDetailById(reservationId);
        if (vo == null || !Objects.equals(currentUserId, vo.getUserId())) {
            log.warn("用户{}尝试为不属于自己的预约{}申请退款", currentUserId, reservationId);
            return Result.error("无权操作该预约");
        }
        try {
            paymentService.applyRefund(reservationId, reason);
            return Result.success("退款申请已提交，请等待管理员审核");
        } catch (Exception e) {
            log.error("申请退款失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 处理退款（管理端）
     */
    @PostMapping("/refund/process")
    @Operation(summary = "处理退款")
    public Result<String> processRefund(
            @RequestParam Long reservationId,
            @RequestParam Integer approved,
            @RequestParam(required = false) String adminRemark) {
        log.info("处理退款: reservationId={}, approved={}, adminRemark={}", 
                reservationId, approved, adminRemark);
        try {
            paymentService.processRefund(reservationId, approved, adminRemark);
            String message = approved == 1 ? "退款已同意并处理完成" : "退款已拒绝";
            return Result.success(message);
        } catch (Exception e) {
            log.error("处理退款失败", e);
            return Result.error(e.getMessage());
        }
    }
}

