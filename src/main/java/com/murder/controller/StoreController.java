package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.StoreLoginDTO;
import com.murder.dto.StoreQueryDTO;
import com.murder.entity.Store;
import com.murder.vo.StoreDetailVO;
import com.murder.vo.StoreLoginVO;
import com.murder.vo.StoreStatisticsVO;
import com.murder.vo.StoreVO;
import com.murder.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店控制器?
 */
@RestController
@RequestMapping("/api/store")
@Tag(name = "门店接口")
@Slf4j
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * 分页查询门店列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询门店列表")
    public Result<PageResult<StoreVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long storeId) {
        Long scopedStoreId = resolveStoreScope(storeId);
        log.info("分页查询门店列表: page={}, pageSize={}, name={}, storeId={}, scopedStoreId={}", page, pageSize, name, storeId, scopedStoreId);
        PageResult<StoreVO> pageResult = storeService.pageQuery(page, pageSize, name, scopedStoreId);
        return Result.success(pageResult);
    }

    /**
     * 多条件分页查询门店列表
     */
    @PostMapping("/page/advanced")
    @Operation(summary = "多条件分页查询门店列表")
    public Result<PageResult<StoreVO>> pageAdvanced(@RequestBody StoreQueryDTO queryDTO) {
        Long scopedStoreId = resolveStoreScope(queryDTO.getStoreId());
        queryDTO.setStoreId(scopedStoreId);
        log.info("多条件分页查询门店列表: {}", queryDTO);
        PageResult<StoreVO> pageResult = storeService.pageQueryAdvanced(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取所有门店列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有门店列表")
    public Result<List<StoreVO>> list(@RequestParam(required = false) Long storeId) {
        Long scopedStoreId = resolveStoreScope(storeId);
        log.info("获取所有门店列表: storeId={}, scopedStoreId={}", storeId, scopedStoreId);
        List<StoreVO> storeList = storeService.listAll(scopedStoreId);
        return Result.success(storeList);
    }

    /**
     * 获取门店统计信息（门店管理员传入storeId只看自己门店，超级管理员不传看全部）
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取门店统计信息")
    public Result<StoreStatisticsVO> getStatistics(
            @RequestParam(required = false) Long storeId) {
        String role = com.murder.common.context.BaseContext.getRole();
        // 门店管理员强制只看自己的门店
        if ("STORE_ADMIN".equals(role)) {
            Long contextStoreId = com.murder.common.context.BaseContext.getStoreId();
            storeId = contextStoreId;
        }
        log.info("获取门店统计信息: storeId={}, role={}", storeId, role);
        StoreStatisticsVO statistics = storeService.getStatistics(storeId);
        return Result.success(statistics);
    }

    /**
     * 根据ID查询门店详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询门店详情")
    public Result<StoreVO> getById(@PathVariable Long id) {
        log.info("查询门店详情: {}", id);
        StoreVO storeVO = storeService.getById(id);
        return Result.success(storeVO);
    }

    /**
     * 根据ID查询门店详细信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询门店详细信息")
    public Result<StoreDetailVO> getDetailById(@PathVariable Long id) {
        log.info("查询门店详细信息: {}", id);
        StoreDetailVO detailVO = storeService.getDetailById(id);
        return Result.success(detailVO);
    }

    /**
     * 新增门店（仅超级管理员）
     */
    @PostMapping
    @Operation(summary = "新增门店")
    public Result<String> add(@RequestBody Store store) {
        requireSuperAdmin();
        log.info("新增门店: {}", store);
        storeService.add(store);
        return Result.success("新增成功");
    }

    /**
     * 更新门店（仅超级管理员）
     */
    @PutMapping
    @Operation(summary = "更新门店")
    public Result<String> update(@RequestBody Store store) {
        requireSuperAdmin();
        log.info("更新门店: {}", store);
        storeService.update(store);
        return Result.success("更新成功");
    }

    /**
     * 删除门店（仅超级管理员）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除门店")
    public Result<String> delete(@PathVariable Long id) {
        requireSuperAdmin();
        log.info("删除门店: {}", id);
        storeService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除门店（仅超级管理员）
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除门店")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        requireSuperAdmin();
        log.info("批量删除门店: {}", ids);
        storeService.batchDelete(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 更新门店状态（仅超级管理员）
     */
    @PutMapping("/status/{id}")
    @Operation(summary = "更新门店状态")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireSuperAdmin();
        log.info("更新门店状态: id={}, status={}", id, status);
        storeService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 批量更新门店状态（仅超级管理员）
     */
    @PutMapping("/status/batch")
    @Operation(summary = "批量更新门店状态")
    public Result<String> batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        requireSuperAdmin();
        log.info("批量更新门店状态: ids={}, status={}", ids, status);
        storeService.batchUpdateStatus(ids, status);
        return Result.success("批量状态更新成功");
    }
    
    /**
     * 获取热门门店
     */
    @GetMapping("/list/hot")
    @Operation(summary = "获取热门门店")
    public Result<List<StoreVO>> getHotStores() {
        log.info("获取热门门店");
        List<StoreVO> hotStores = storeService.getHotStores();
        return Result.success(hotStores);
    }
    
    /**
     * 获取推荐门店
     */
    @GetMapping("/list/recommended")
    @Operation(summary = "获取推荐门店")
    public Result<List<StoreVO>> getRecommendedStores() {
        log.info("获取推荐门店");
        List<StoreVO> recommendedStores = storeService.getRecommendedStores();
        return Result.success(recommendedStores);
    }

    /**
     * 获取附近门店
     */
    @GetMapping("/nearby")
    @Operation(summary = "获取附近门店")
    public Result<List<StoreVO>> getNearbyStores(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取附近门店: latitude={}, longitude={}, limit={}", latitude, longitude, limit);
        
        // 如果提供了经纬度，实现基于地理位置的附近门店查询
        if (latitude != null && longitude != null) {
            List<StoreVO> nearbyStores = storeService.getNearbyStores(latitude, longitude, limit);
            return Result.success(nearbyStores);
        }
        
        // 否则返回热门门店作为替代
        List<StoreVO> nearbyStores = storeService.getHotStores();
        return Result.success(nearbyStores);
    }
    
    /**
     * 门店账号登录
     */
    @PostMapping("/login")
    @Operation(summary = "门店账号登录")
    public Result<StoreLoginVO> storeLogin(@RequestBody StoreLoginDTO storeLoginDTO) {
        log.info("门店账号登录: {}", storeLoginDTO.getLoginAccount());
        StoreLoginVO storeLoginVO = storeService.storeLogin(storeLoginDTO);
        return Result.success(storeLoginVO);
    }
    
    /**
     * 修改门店登录密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改门店登录密码")
    public Result<String> updatePassword(
            @RequestParam Long storeId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        log.info("修改门店登录密码: storeId={}", storeId);
        storeService.updateLoginPassword(storeId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }
    
    /**
     * 更新门店账号信息（仅超级管理员）
     */
    @PutMapping("/account")
    @Operation(summary = "更新门店账号信息")
    public Result<String> updateAccount(@RequestBody java.util.Map<String, Object> params) {
        requireSuperAdmin();
        Long storeId = Long.valueOf(params.get("id").toString());
        String loginAccount = (String) params.get("loginAccount");
        String loginPassword = (String) params.get("loginPassword");
        
        log.info("更新门店账号信息: storeId={}, loginAccount={}", storeId, loginAccount);
        storeService.updateStoreAccount(storeId, loginAccount, loginPassword);
        return Result.success("账号信息更新成功");
    }
    
    /**
     * 重置门店密码为默认值（仅超级管理员）
     */
    @PutMapping("/account/reset-password")
    @Operation(summary = "重置门店密码为默认值")
    public Result<String> resetPassword(@RequestBody java.util.Map<String, Object> params) {
        requireSuperAdmin();
        Long storeId = Long.valueOf(params.get("id").toString());
        
        log.info("重置门店密码: storeId={}", storeId);
        storeService.resetStorePassword(storeId);
        return Result.success("密码已重置为默认值: 123456");
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

    private Long resolveStoreScope(Long requestedStoreId) {
        String role = com.murder.common.context.BaseContext.getRole();
        if ("STORE_ADMIN".equals(role)) {
            return com.murder.common.context.BaseContext.getStoreId();
        }
        return requestedStoreId;
    }
}
