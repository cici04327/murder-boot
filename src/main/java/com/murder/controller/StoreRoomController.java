package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.common.result.Result;
import com.murder.entity.StoreRoom;
import com.murder.service.StoreRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店房间控制器
 */
@RestController
@RequestMapping("/api/store/room")
@Tag(name = "门店房间接口")
@Slf4j
public class StoreRoomController {

    @Autowired
    private StoreRoomService storeRoomService;

    /**
     * 根据ID查询房间详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询房间详情")
    public Result<StoreRoom> getById(@PathVariable Long id) {
        log.info("查询房间详情: {}", id);
        StoreRoom room = storeRoomService.getById(id);
        // 门店管理员只能查看本门店的房间
        requireStoreAccess(room != null ? room.getStoreId() : null);
        return Result.success(room);
    }

    /**
     * 查询门店房间列表
     */
    @GetMapping("/list/{storeId}")
    @Operation(summary = "查询门店房间列表")
    public Result<List<StoreRoom>> list(@PathVariable Long storeId) {
        // 门店管理员只能查看本门店的房间
        Long scopedStoreId = resolveStoreId(storeId);
        log.info("查询门店房间列表: storeId={}, scopedStoreId={}", storeId, scopedStoreId);
        List<StoreRoom> roomList = storeRoomService.listByStoreId(scopedStoreId);
        return Result.success(roomList);
    }

    /**
     * 新增房间
     */
    @PostMapping
    @Operation(summary = "新增房间")
    public Result<String> add(@RequestBody StoreRoom room) {
        // 门店管理员只能为本门店新增房间
        requireStoreAccess(room.getStoreId());
        log.info("新增房间: {}", room);
        storeRoomService.add(room);
        return Result.success("新增成功");
    }

    /**
     * 更新房间
     */
    @PutMapping
    @Operation(summary = "更新房间")
    public Result<String> update(@RequestBody StoreRoom room) {
        // 门店管理员只能更新本门店的房间
        requireStoreAccess(room.getStoreId());
        log.info("更新房间: {}", room);
        storeRoomService.update(room);
        return Result.success("更新成功");
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除房间")
    public Result<String> delete(@PathVariable Long id) {
        // 门店管理员只能删除本门店的房间
        StoreRoom room = storeRoomService.getById(id);
        if (room != null) {
            requireStoreAccess(room.getStoreId());
        }
        log.info("删除房间: {}", id);
        storeRoomService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询可用房间
     */
    @GetMapping("/available/{storeId}")
    @Operation(summary = "查询可用房间")
    public Result<List<StoreRoom>> getAvailableRooms(@PathVariable Long storeId) {
        Long scopedStoreId = resolveStoreId(storeId);
        log.info("查询可用房间: storeId={}, scopedStoreId={}", storeId, scopedStoreId);
        List<StoreRoom> roomList = storeRoomService.getAvailableRooms(scopedStoreId);
        return Result.success(roomList);
    }

    /**
     * 解析门店ID：门店管理员强制使用自己的门店ID
     */
    private Long resolveStoreId(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if ("STORE_ADMIN".equals(role)) {
            return BaseContext.getStoreId();
        }
        return requestedStoreId;
    }

    /**
     * 校验门店访问权限：门店管理员只能操作自己门店的数据
     */
    private void requireStoreAccess(Long targetStoreId) {
        String role = BaseContext.getRole();
        if ("STORE_ADMIN".equals(role)) {
            Long myStoreId = BaseContext.getStoreId();
            if (myStoreId == null || !myStoreId.equals(targetStoreId)) {
                throw new BaseException("无权限操作其他门店的房间数据");
            }
        }
    }
}
