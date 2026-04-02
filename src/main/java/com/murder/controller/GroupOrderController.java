package com.murder.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.Result;
import com.murder.entity.GroupOrder;
import com.murder.service.GroupOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 拼单Controller
 */
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupOrderController {
    
    private final GroupOrderService groupOrderService;
    
    /**
     * 分页查询拼单
     */
    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) Long scriptId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer playerCount,
            @RequestParam(required = false) Integer status) {
        Page<Map<String, Object>> result = groupOrderService.pageQuery(page, pageSize, scriptId, categoryId, playerCount, status);
        return Result.success(result);
    }
    
    /**
     * 获取热门拼单
     */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotGroups(@RequestParam(defaultValue = "10") Integer limit) {
        List<Map<String, Object>> result = groupOrderService.getHotGroups(limit);
        return Result.success(result);
    }
    
    /**
     * 获取拼单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId(); // 未登录时为null，不影响查询
        Map<String, Object> result = groupOrderService.getDetailWithMembers(id, userId);
        if (result == null) {
            return Result.error("拼单不存在");
        }
        return Result.success(result);
    }
    
    /**
     * 创建拼单
     */
    @PostMapping
    public Result<GroupOrder> create(@RequestBody GroupOrder groupOrder) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            GroupOrder result = groupOrderService.createGroup(groupOrder, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 加入拼单
     */
    @PostMapping("/{id}/join")
    public Result<String> join(@PathVariable Long id, 
                               @RequestParam(defaultValue = "1") Integer joinCount) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            groupOrderService.joinGroup(id, userId, joinCount);
            return Result.success("上车成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 退出拼单
     */
    @PostMapping("/{id}/leave")
    public Result<String> leave(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            groupOrderService.leaveGroup(id, userId);
            return Result.success("退出成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 取消拼单
     */
    @PostMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            groupOrderService.cancelGroup(id, userId);
            return Result.success("取消成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取我的拼单
     */
    @GetMapping("/my")
    public Result<Page<Map<String, Object>>> myGroups(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "0") Integer type) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Page<Map<String, Object>> result = groupOrderService.getMyGroups(userId, page, pageSize, type);
        return Result.success(result);
    }
}
