package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付订单")
    public Result<String> createPayment(
            @RequestParam Long reservationId,
            @RequestParam(defaultValue = "mock") String paymentMethod) {
        log.info("创建支付订单: reservationId={}, paymentMethod={}", reservationId, paymentMethod);
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
     * 查询支付状态
     */
    @GetMapping("/status/{reservationId}")
    @Operation(summary = "查询支付状态")
    public Result<Integer> queryPaymentStatus(@PathVariable Long reservationId) {
        log.info("查询支付状态: reservationId={}", reservationId);
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

