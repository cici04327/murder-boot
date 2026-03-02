package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.entity.StoreEmployee;
import com.murder.vo.StoreEmployeeVO;
import com.murder.service.StoreEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店员工控制�?
 */
@RestController
@RequestMapping("/api/store/employee")
@Tag(name = "门店员工接口")
@Slf4j
public class StoreEmployeeController {

    @Autowired
    private StoreEmployeeService employeeService;

    /**
     * 分页查询门店员工列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询门店员工列表")
    public Result<PageResult<StoreEmployeeVO>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询门店员工列表: storeId={}, page={}, pageSize={}", storeId, page, pageSize);
        PageResult<StoreEmployeeVO> pageResult = employeeService.pageQuery(storeId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据门店ID查询员工列表
     */
    @GetMapping("/list/{storeId}")
    @Operation(summary = "查询门店员工列表")
    public Result<List<StoreEmployee>> list(@PathVariable Long storeId) {
        log.info("查询门店员工列表: {}", storeId);
        List<StoreEmployee> employeeList = employeeService.listByStoreId(storeId);
        return Result.success(employeeList);
    }

    /**
     * 根据ID查询员工详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询员工详情")
    public Result<StoreEmployee> getById(@PathVariable Long id) {
        log.info("查询员工详情: {}", id);
        StoreEmployee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    /**
     * 新增员工
     */
    @PostMapping
    @Operation(summary = "新增员工")
    public Result<String> add(@RequestBody StoreEmployeeDTO employeeDTO) {
        log.info("新增员工: {}", employeeDTO);
        employeeService.add(employeeDTO);
        return Result.success("新增成功");
    }

    /**
     * 更新员工
     */
    @PutMapping
    @Operation(summary = "更新员工")
    public Result<String> update(@RequestBody StoreEmployeeDTO employeeDTO) {
        log.info("更新员工: {}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除员工")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除员工: {}", id);
        employeeService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除员工
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除员工")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        log.info("批量删除员工: {}", ids);
        employeeService.batchDelete(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 更新员工状态
     */
    @PutMapping("/status/{id}")
    @Operation(summary = "更新员工状态")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("更新员工状态: id={}, status={}", id, status);
        employeeService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}
