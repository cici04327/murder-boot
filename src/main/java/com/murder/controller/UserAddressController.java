package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.dto.UserAddressDTO;
import com.murder.entity.UserAddress;
import com.murder.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址控制器
 */
@RestController
@RequestMapping("/api/user/address")
@Tag(name = "用户地址接口")
@Slf4j
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 查询当前用户地址列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询当前用户地址列表")
    public Result<List<UserAddress>> list() {
        Long userId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("查询用户地址列表: {}", userId);
        List<UserAddress> addressList = userAddressService.listByUserId(userId);
        return Result.success(addressList);
    }
    
    /**
     * 查询用户地址列表（兼容旧接口，强制使用当前用户ID）
     */
    @GetMapping("/list/{userId}")
    @Operation(summary = "查询用户地址列表（兼容旧接口）")
    public Result<List<UserAddress>> listByUserId(@PathVariable Long userId) {
        // 安全性保障：忽略传入的userId，强制使用当前登录用户的ID
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("查询用户地址列表: 请求userId={}, 实际使用userId={}", userId, currentUserId);
        List<UserAddress> addressList = userAddressService.listByUserId(currentUserId);
        return Result.success(addressList);
    }

    /**
     * 根据ID查询地址详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询地址详情")
    public Result<UserAddress> getById(@PathVariable Long id) {
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("查询地址详情: id={}, userId={}", id, currentUserId);
        UserAddress address = userAddressService.getById(id);
        // 安全性校验：确保地址属于当前用户
        if (address != null && !currentUserId.equals(address.getUserId())) {
            log.warn("用户{}尝试访问不属于自己的地址{}", currentUserId, id);
            return Result.success(null);
        }
        return Result.success(address);
    }

    /**
     * 新增地址
     */
    @PostMapping
    @Operation(summary = "新增地址")
    public Result<String> add(@RequestBody UserAddressDTO addressDTO) {
        // 强制设置为当前用户ID
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        addressDTO.setUserId(currentUserId);
        log.info("新增地址: {}", addressDTO);
        userAddressService.add(addressDTO);
        return Result.success("新增成功");
    }

    /**
     * 更新地址
     */
    @PutMapping
    @Operation(summary = "更新地址")
    public Result<String> update(@RequestBody UserAddressDTO addressDTO) {
        // 强制设置为当前用户ID
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        addressDTO.setUserId(currentUserId);
        log.info("更新地址: {}", addressDTO);
        userAddressService.update(addressDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<String> delete(@PathVariable Long id) {
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("删除地址: id={}, userId={}", id, currentUserId);
        // 安全性校验：先查询地址确保属于当前用户
        UserAddress address = userAddressService.getById(id);
        if (address == null || !currentUserId.equals(address.getUserId())) {
            log.warn("用户{}尝试删除不属于自己的地址{}", currentUserId, id);
            return Result.error("无权删除该地址");
        }
        userAddressService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认地址")
    public Result<String> setDefault(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        // 忽略传入的userId，强制使用当前登录用户的ID
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("设置默认地址: id={}, userId={}", id, currentUserId);
        // 安全性校验：先查询地址确保属于当前用户
        UserAddress address = userAddressService.getById(id);
        if (address == null || !currentUserId.equals(address.getUserId())) {
            log.warn("用户{}尝试设置不属于自己的地址{}为默认", currentUserId, id);
            return Result.error("无权操作该地址");
        }
        userAddressService.setDefault(id, currentUserId);
        return Result.success("设置成功");
    }

    /**
     * 获取当前用户默认地址
     */
    @GetMapping("/default")
    @Operation(summary = "获取当前用户默认地址")
    public Result<UserAddress> getDefault() {
        Long userId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("获取默认地址: {}", userId);
        UserAddress address = userAddressService.getDefaultAddress(userId);
        return Result.success(address);
    }
    
    /**
     * 获取默认地址（兼容旧接口，强制使用当前用户ID）
     */
    @GetMapping("/default/{userId}")
    @Operation(summary = "获取默认地址（兼容旧接口）")
    public Result<UserAddress> getDefaultByUserId(@PathVariable Long userId) {
        // 安全性保障：忽略传入的userId，强制使用当前登录用户的ID
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        log.info("获取默认地址: 请求userId={}, 实际使用userId={}", userId, currentUserId);
        UserAddress address = userAddressService.getDefaultAddress(currentUserId);
        return Result.success(address);
    }
}
