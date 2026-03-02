package com.murder.controller;

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
 * 门店房间控制�?
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
        return Result.success(room);
    }

    /**
     * 查询门店房间列表
     */
    @GetMapping("/list/{storeId}")
    @Operation(summary = "查询门店房间列表")
    public Result<List<StoreRoom>> list(@PathVariable Long storeId) {
        log.info("查询门店房间列表: {}", storeId);
        List<StoreRoom> roomList = storeRoomService.listByStoreId(storeId);
        return Result.success(roomList);
    }

    /**
     * 新增房间
     */
    @PostMapping
    @Operation(summary = "新增房间")
    public Result<String> add(@RequestBody StoreRoom room) {
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
        log.info("查询可用房间: {}", storeId);
        List<StoreRoom> roomList = storeRoomService.getAvailableRooms(storeId);
        return Result.success(roomList);
    }
}
