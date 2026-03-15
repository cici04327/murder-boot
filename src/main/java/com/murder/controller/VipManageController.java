package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.VipPackageDTO;
import com.murder.vo.VipPackageVO;
import com.murder.service.VipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * VIP管理控制器器（管理端）
 */
@RestController
@RequestMapping("/api/admin/vip")
@Tag(name = "VIP管理接口")
@Slf4j
public class VipManageController {

    @Autowired
    private VipService vipService;

    // ========== VIP套餐管理 ==========

    /**
     * 分页查询VIP套餐
     */
    @GetMapping("/packages")
    @Operation(summary = "分页查询VIP套餐")
    public Result<PageResult<VipPackageVO>> pageQueryPackages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询VIP套餐：page={}, pageSize={}, level={}, status={}", page, pageSize, level, status);
        PageResult<VipPackageVO> pageResult = vipService.pageQueryPackages(page, pageSize, level, status);
        return Result.success(pageResult);
    }

    /**
     * 创建VIP套餐
     */
    @PostMapping("/packages")
    @Operation(summary = "创建VIP套餐")
    public Result<String> createPackage(@RequestBody VipPackageDTO dto) {
        log.info("创建VIP套餐：{}", dto.getName());
        try {
            vipService.createPackage(dto);
            return Result.success("创建成功");
        } catch (Exception e) {
            log.error("创建VIP套餐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新VIP套餐
     */
    @PutMapping("/packages")
    @Operation(summary = "更新VIP套餐")
    public Result<String> updatePackage(@RequestBody VipPackageDTO dto) {
        log.info("更新VIP套餐：{}", dto.getId());
        try {
            vipService.updatePackage(dto);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新VIP套餐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除VIP套餐
     */
    @DeleteMapping("/packages/{id}")
    @Operation(summary = "删除VIP套餐")
    public Result<String> deletePackage(@PathVariable Long id) {
        log.info("删除VIP套餐：{}", id);
        try {
            vipService.deletePackage(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除VIP套餐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上下架VIP套餐
     */
    @PutMapping("/packages/{id}/status")
    @Operation(summary = "上下架VIP套餐")
    public Result<String> updatePackageStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("更新VIP套餐状态：id={}, status={}", id, status);
        try {
            vipService.updatePackageStatus(id, status);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新VIP套餐状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    // ========== VIP用户管理 ==========

    /**
     * 分页查询VIP用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "分页查询VIP用户列表")
    public Result<PageResult<Map<String, Object>>> pageQueryVipUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询VIP用户：page={}, pageSize={}, level={}, status={}", page, pageSize, level, status);
        PageResult<Map<String, Object>> pageResult = vipService.pageQueryVipUsers(page, pageSize, level, status);
        return Result.success(pageResult);
    }

    /**
     * 赠送VIP
     */
    @PostMapping("/grant")
    @Operation(summary = "赠送VIP")
    public Result<String> grantVip(
            @RequestParam Long userId,
            @RequestParam Integer days,
            @RequestParam Integer level,
            @RequestParam(required = false) String reason) {
        log.info("赠送VIP：userId={}, days={}, level={}, reason={}", userId, days, level, reason);
        try {
            vipService.grantVip(userId, days, level, reason);
            return Result.success("赠送成功");
        } catch (Exception e) {
            log.error("赠送VIP失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 延长VIP
     */
    @PostMapping("/extend")
    @Operation(summary = "延长VIP")
    public Result<String> extendVip(
            @RequestParam Long userId,
            @RequestParam Integer days) {
        log.info("延长VIP：userId={}, days={}", userId, days);
        try {
            vipService.extendVip(userId, days);
            return Result.success("延长成功");
        } catch (Exception e) {
            log.error("延长VIP失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动补发月度体验券
     */
    @PostMapping("/grant-monthly-coupons")
    @Operation(summary = "手动补发月度体验券")
    public Result<String> grantMonthlyCoupons(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(required = false) String reason) {
        // 默认补发当月
        if (year == 0 || month == 0) {
            java.time.LocalDate today = java.time.LocalDate.now();
            year = today.getYear();
            month = today.getMonthValue();
        }
        log.info("管理员补发月度体验券：userId={}, year={}, month={}, reason={}", userId, year, month, reason);
        try {
            vipService.adminGrantMonthlyCoupons(userId, year, month, reason);
            return Result.success("补发成功");
        } catch (Exception e) {
            log.error("补发月度体验券失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取VIP统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取VIP统计数据")
    public Result<Map<String, Object>> getVipStatistics() {
        log.info("获取VIP统计数据");
        Map<String, Object> statistics = vipService.getVipStatistics();
        return Result.success(statistics);
    }
}

