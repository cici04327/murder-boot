package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.DmDTO;
import com.murder.service.DmService;
import com.murder.vo.DmVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DM 控制器
 */
@RestController
@RequestMapping("/api/dm")
@Tag(name = "DM（主持人）接口")
@Slf4j
public class DmController {

    @Autowired
    private DmService dmService;

    /**
     * 分页查询（管理端）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询 DM 列表")
    public Result<PageResult<DmVO>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询 DM: storeId={}, status={}, page={}, pageSize={}", storeId, status, page, pageSize);
        return Result.success(dmService.pageQuery(storeId, status, page, pageSize));
    }

    /**
     * 查询门店在职 DM 列表（排期分配 / 用户端展示）
     */
    @GetMapping("/list")
    @Operation(summary = "查询门店 DM 列表")
    public Result<List<DmVO>> list(@RequestParam Long storeId) {
        log.info("查询门店 DM 列表: storeId={}", storeId);
        return Result.success(dmService.listByStoreId(storeId));
    }

    /**
     * 查询 DM 详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询 DM 详情")
    public Result<DmVO> getById(@PathVariable Long id) {
        log.info("查询 DM 详情: id={}", id);
        return Result.success(dmService.getById(id));
    }

    /**
     * 新增 DM
     */
    @PostMapping
    @Operation(summary = "新增 DM")
    public Result<String> add(@RequestBody DmDTO dmDTO) {
        log.info("新增 DM: {}", dmDTO);
        dmService.add(dmDTO);
        return Result.success("新增成功");
    }

    /**
     * 编辑 DM
     */
    @PutMapping
    @Operation(summary = "编辑 DM")
    public Result<String> update(@RequestBody DmDTO dmDTO) {
        log.info("编辑 DM: {}", dmDTO);
        dmService.update(dmDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除 DM
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除 DM")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除 DM: id={}", id);
        dmService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 更新状态（在职 / 离职）
     */
    @PutMapping("/status")
    @Operation(summary = "更新 DM 状态")
    public Result<String> updateStatus(
            @RequestParam Long id,
            @RequestParam Integer status) {
        log.info("更新 DM 状态: id={}, status={}", id, status);
        dmService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}
