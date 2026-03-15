package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.CouponDTO;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.vo.CouponVO;
import com.murder.vo.UserCouponVO;
import com.murder.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券控制器
 */
@RestController
@RequestMapping("/api/coupon")
@Tag(name = "优惠券接口")
@Slf4j
public class CouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 分页查询优惠券列�?
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询优惠券列�?")
    public Result<PageResult<CouponVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询优惠券列�? page={}, pageSize={}, name={}, type={}, status={}", 
                page, pageSize, name, type, status);
        PageResult<CouponVO> pageResult = couponService.pageQuery(page, pageSize, name, type, status);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询优惠券详�?
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询优惠券详�?")
    public Result<CouponVO> getById(@PathVariable Long id) {
        log.info("查询优惠券详�? {}", id);
        CouponVO coupon = couponService.getById(id);
        return Result.success(coupon);
    }
    
    /**
     * 获取优惠券统计信�?
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取优惠券统计信�?")
    public Result<CouponVO> getStatistics(@PathVariable Long id) {
        log.info("获取优惠券统计信�? {}", id);
        CouponVO statistics = couponService.getCouponStatistics(id);
        return Result.success(statistics);
    }

    /**
     * 新增优惠券（仅超级管理员）
     */
    @PostMapping
    @Operation(summary = "新增优惠券")
    public Result<String> add(@RequestBody CouponDTO couponDTO) {
        requireSuperAdmin();
        log.info("新增优惠券: {}", couponDTO);
        couponService.add(couponDTO);
        return Result.success("新增成功");
    }

    /**
     * 更新优惠券（仅超级管理员）
     */
    @PutMapping
    @Operation(summary = "更新优惠券")
    public Result<String> update(@RequestBody CouponDTO couponDTO) {
        requireSuperAdmin();
        log.info("更新优惠券: {}", couponDTO);
        couponService.update(couponDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除优惠券（仅超级管理员）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除优惠券")
    public Result<String> delete(@PathVariable Long id) {
        requireSuperAdmin();
        log.info("删除优惠券: {}", id);
        couponService.delete(id);
        return Result.success("删除成功");
    }
    
    /**
     * 上架/下架优惠券（仅超级管理员）
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "上架/下架优惠券")
    public Result<String> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        requireSuperAdmin();
        log.info("更新优惠券状态: id={}, status={}", id, status);
        couponService.updateStatus(id, status);
        return Result.success("更新成功");
    }

    /**
     * 用户领取优惠�?
     */
    @PostMapping("/receive")
    @Operation(summary = "用户领取优惠�?")
    public Result<String> receiveCoupon(@RequestParam Long couponId) {
        // 从上下文获取当前登录用户ID
        Long userId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("用户领取优惠�? userId={}, couponId={}", userId, couponId);
        couponService.receiveCoupon(userId, couponId);
        return Result.success("领取成功");
    }

    /**
     * 查询当前用户的优惠券列表
     */
    @GetMapping("/user")
    @Operation(summary = "查询当前用户的优惠券列表")
    public Result<PageResult<UserCouponVO>> getUserCoupons(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        // 从上下文获取当前登录用户ID
        Long userId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("查询用户的优惠券列表: userId={}, page={}, pageSize={}, status={}", 
                userId, page, pageSize, status);
        PageResult<UserCouponVO> pageResult = couponService.getUserCoupons(userId, page, pageSize, status);
        return Result.success(pageResult);
    }
    
    /**
     * 管理员查询指定用户的优惠券列表（仅管理端可用）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "管理员查询指定用户的优惠券列表")
    public Result<PageResult<UserCouponVO>> getUserCouponsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        // 安全性校验：仅管理端可以查询指定用户的优惠券
        String clientType = getClientType();
        Long queryUserId = userId;
        if (!"admin".equals(clientType)) {
            // 非管理端：强制使用当前登录用户的ID
            queryUserId = com.murder.common.context.BaseContext.getCurrentId();
            log.info("用户端查询优惠券列表: 请求userId={}, 实际使用userId={}", userId, queryUserId);
        } else {
            log.info("管理员查询用户的优惠券列表: userId={}, page={}, pageSize={}, status={}", 
                    userId, page, pageSize, status);
        }
        PageResult<UserCouponVO> pageResult = couponService.getUserCoupons(queryUserId, page, pageSize, status);
        return Result.success(pageResult);
    }
    
    /**
     * 查询用户可用的优惠券列表
     */
    @GetMapping("/user/{userId}/available")
    @Operation(summary = "查询用户可用的优惠券列表")
    public Result<List<UserCouponVO>> getAvailableCoupons(
            @PathVariable Long userId,
            @RequestParam(required = false) BigDecimal orderAmount) {
        // 安全性校验：仅管理端可以查询指定用户的优惠券
        String clientType = getClientType();
        Long queryUserId = userId;
        if (!"admin".equals(clientType)) {
            // 非管理端：强制使用当前登录用户的ID
            queryUserId = com.murder.common.context.BaseContext.getCurrentId();
            log.info("用户端查询可用优惠券: 请求userId={}, 实际使用userId={}", userId, queryUserId);
        } else {
            log.info("管理端查询用户可用的优惠券: userId={}, orderAmount={}", userId, orderAmount);
        }
        List<UserCouponVO> coupons = couponService.getAvailableCoupons(queryUserId, orderAmount);
        return Result.success(coupons);
    }
    
    /**
     * 获取客户端类型
     */
    private String getClientType() {
        try {
            org.springframework.web.context.request.RequestAttributes ra = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                return attrs.getRequest().getHeader("X-Client-Type");
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 计算优惠金额
     */
    @GetMapping("/calculate")
    @Operation(summary = "计算优惠金额")
    public Result<BigDecimal> calculateDiscount(
            @RequestParam Long userCouponId,
            @RequestParam BigDecimal orderAmount) {
        log.info("计算优惠金额: userCouponId={}, orderAmount={}", userCouponId, orderAmount);
        BigDecimal discount = couponService.calculateDiscount(userCouponId, orderAmount);
        return Result.success(discount);
    }

    /**
     * 使用优惠�?
     */
    @PutMapping("/use")
    @Operation(summary = "使用优惠�?")
    public Result<String> useCoupon(
            @RequestParam Long userCouponId,
            @RequestParam Long orderId) {
        log.info("使用优惠�? userCouponId={}, orderId={}", userCouponId, orderId);
        couponService.useCoupon(userCouponId, orderId);
        return Result.success("使用成功");
    }
    
    /**
     * 退还优惠券
     */
    @PutMapping("/refund")
    @Operation(summary = "退还优惠券")
    public Result<String> refundCoupon(@RequestParam Long orderId) {
        log.info("退还优惠券: orderId={}", orderId);
        couponService.refundCoupon(orderId);
        return Result.success("退还成�?");
    }
    
    /**
     * 批量过期优惠�?
     */
    @PostMapping("/expire")
    @Operation(summary = "批量过期优惠�?")
    public Result<String> expireCoupons() {
        log.info("批量过期优惠�?");
        couponService.expireCoupons();
        return Result.success("操作成功");
    }
    
    /**
     * 校验是否为超级管理员，否则抛出异常
     */
    private void requireSuperAdmin() {
        String role = com.murder.common.context.BaseContext.getRole();
        if (!"SUPER_ADMIN".equals(role)) {
            throw new com.murder.common.exception.BaseException("无权限，仅超级管理员可执行此操作");
        }
    }

    /**
     * 获取预约可用的优惠券列表
     */
    @GetMapping("/available/reservation/{reservationId}")
    @Operation(summary = "获取预约可用的优惠券列表")
    public Result<List<UserCouponVO>> getAvailableCouponsForReservation(
            @PathVariable Long reservationId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BigDecimal orderAmount) {
        log.info("获取预约可用的优惠券: reservationId={}, userId={}, orderAmount={}", 
                reservationId, userId, orderAmount);
        
        // 从当前登录用户上下文获取userId
        Long currentUserId = userId;
        if (currentUserId == null) {
            try {
                currentUserId = com.murder.common.context.BaseContext.getCurrentId();
            } catch (Exception e) {
                log.warn("无法获取当前登录用户ID，返回空优惠券列�?");
                return Result.success(new java.util.ArrayList<>());
            }
        }
        
        // 查询用户的可用优惠券
        List<UserCouponVO> coupons = couponService.getAvailableCoupons(currentUserId, orderAmount);
        return Result.success(coupons);
    }
}
