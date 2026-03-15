package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.entity.UserVip;
import com.murder.vo.VipPackageVO;
import com.murder.service.VipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * VIP控制器（用户端）
 */
@RestController
@RequestMapping("/api/vip")
@Tag(name = "VIP会员接口")
@Slf4j
public class VipController {

    @Autowired
    private VipService vipService;

    /**
     * 获取VIP套餐列表
     */
    @GetMapping("/packages")
    @Operation(summary = "获取VIP套餐列表")
    public Result<List<VipPackageVO>> getVipPackages() {
        log.info("获取VIP套餐列表");
        List<VipPackageVO> packages = vipService.getVipPackages();
        return Result.success(packages);
    }

    /**
     * 获取VIP套餐详情
     */
    @GetMapping("/packages/{id}")
    @Operation(summary = "获取VIP套餐详情")
    public Result<VipPackageVO> getVipPackageById(@PathVariable Long id) {
        log.info("获取VIP套餐详情：{}", id);
        VipPackageVO packageVO = vipService.getVipPackageById(id);
        return Result.success(packageVO);
    }

    /**
     * 购买VIP
     */
    @PostMapping("/purchase")
    @Operation(summary = "购买VIP")
    public Result<String> purchaseVip(
            @RequestParam Long packageId,
            @RequestParam(defaultValue = "ALIPAY") String paymentMethod) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        log.info("用户 {} 购买VIP，套餐ID：{}", userId, packageId);

        try {
            String orderNo = vipService.purchaseVip(userId, packageId, paymentMethod);
            return Result.success(orderNo);
        } catch (Exception e) {
            log.error("购买VIP失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户VIP信息（新接口，兼容前�?info路径�?
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户VIP信息")
    public Result<Map<String, Object>> getUserVipInfo() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        log.info("获取用户 {} 的VIP信息", userId);
        Map<String, Object> vipInfo = vipService.getUserVipInfo(userId);
        return Result.success(vipInfo);
    }

    /**
     * 获取我的VIP信息（旧接口，保留兼容性）
     */
    @GetMapping("/my-vip")
    @Operation(summary = "获取我的VIP信息")
    public Result<Map<String, Object>> getMyVipInfo() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        log.info("获取用户 {} 的VIP信息", userId);
        Map<String, Object> vipInfo = vipService.getUserVipInfo(userId);
        return Result.success(vipInfo);
    }

    /**
     * 续费VIP
     */
    @PostMapping("/renew")
    @Operation(summary = "续费VIP")
    public Result<String> renewVip(@RequestParam Long packageId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        log.info("用户 {} 续费VIP，套餐ID：{}", userId, packageId);

        try {
            String orderNo = vipService.renewVip(userId, packageId);
            return Result.success(orderNo);
        } catch (Exception e) {
            log.error("续费VIP失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取VIP历史记录
     */
    @GetMapping("/history")
    @Operation(summary = "获取VIP历史记录")
    public Result<PageResult<UserVip>> getVipHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        log.info("获取用户 {} 的VIP历史记录", userId);
        PageResult<UserVip> pageResult = vipService.getUserVipHistory(userId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询本月月度体验券发放状态（已发张数、下次发放倒计时）
     */
    @GetMapping("/monthly-coupon-status")
    @Operation(summary = "查询本月月度体验券发放状态")
    public Result<Map<String, Object>> getMonthlyCouponStatus() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Map<String, Object> status = vipService.getMonthlyCouponStatus(userId);
        return Result.success(status);
    }

    /**
     * 检查VIP状态
     */
    @GetMapping("/check-status")
    @Operation(summary = "检查VIP状态")
    public Result<Map<String, Object>> checkVipStatus() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        Map<String, Object> status = Map.of(
                "isVip", vipService.isVip(userId),
                "level", vipService.getUserVipLevel(userId),
                "pointMultiplier", vipService.getPointMultiplier(userId),
                "hasPriorityBooking", vipService.hasPriorityBooking(userId)
        );

        return Result.success(status);
    }
}

