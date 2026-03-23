package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.dto.StoreEmployeeLoginDTO;
import com.murder.entity.StoreEmployee;
import com.murder.entity.StoreEmployeeOperationLog;
import com.murder.vo.StoreEmployeeVO;
import com.murder.vo.StoreEmployeeLoginVO;
import com.murder.service.StoreEmployeeOperationLogService;
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

    @Autowired(required = false)
    private StoreEmployeeOperationLogService operationLogService;

    /**
     * 分页查询门店员工列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询门店员工列表")
    public Result<PageResult<StoreEmployeeVO>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        requireEmployeeManageAccess();
        Long scopedStoreId = resolveStoreScope(storeId);
        log.info("分页查询门店员工列表: storeId={}, scopedStoreId={}, page={}, pageSize={}", storeId, scopedStoreId, page, pageSize);
        PageResult<StoreEmployeeVO> pageResult = employeeService.pageQuery(scopedStoreId, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据门店ID查询员工列表
     */
    @GetMapping("/list/{storeId}")
    @Operation(summary = "查询门店员工列表")
    public Result<List<StoreEmployee>> list(@PathVariable Long storeId) {
        requireEmployeeManageAccess();
        Long scopedStoreId = resolveStoreScope(storeId);
        log.info("查询门店员工列表: requestedStoreId={}, scopedStoreId={}", storeId, scopedStoreId);
        List<StoreEmployee> employeeList = employeeService.listByStoreId(scopedStoreId);
        return Result.success(employeeList);
    }

    /**
     * 根据ID查询员工详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询员工详情")
    public Result<StoreEmployee> getById(@PathVariable Long id) {
        requireEmployeeManageAccess();
        log.info("查询员工详情: {}", id);
        StoreEmployee employee = employeeService.getById(id);
        assertEmployeeStoreScope(employee);
        return Result.success(employee);
    }

    /**
     * 新增员工
     */
    @PostMapping
    @Operation(summary = "新增员工")
    public Result<String> add(@RequestBody StoreEmployeeDTO employeeDTO) {
        requireEmployeeManageAccess();
        rejectManagerTargetForStaff(employeeDTO);
        employeeDTO.setStoreId(resolveStoreScope(employeeDTO.getStoreId()));
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
        requireEmployeeManageAccess();
        rejectManagerTargetForStaff(employeeDTO);
        employeeDTO.setStoreId(resolveStoreScope(employeeDTO.getStoreId()));
        if (employeeDTO.getId() != null) {
            assertEmployeeStoreScope(employeeService.getById(employeeDTO.getId()));
        }
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
        requireEmployeeManageAccess();
        assertEmployeeStoreScope(employeeService.getById(id));
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
        requireEmployeeManageAccess();
        for (Long id : ids) {
            assertEmployeeStoreScope(employeeService.getById(id));
        }
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
        requireEmployeeManageAccess();
        assertEmployeeStoreScope(employeeService.getById(id));
        log.info("更新员工状态: id={}, status={}", id, status);
        employeeService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    @PutMapping("/reset-password/{id}")
    @Operation(summary = "重置员工密码")
    public Result<String> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        requireEmployeeManageAccess();
        assertEmployeeStoreScope(employeeService.getById(id));
        log.info("重置员工密码: id={}", id);
        employeeService.resetPassword(id, newPassword);
        return Result.success("密码重置成功");
    }

    @GetMapping("/log/page")
    @Operation(summary = "分页查询员工操作日志")
    public Result<PageResult<StoreEmployeeOperationLog>> logPage(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        requireEmployeeManageAccess();
        Long scopedStoreId = resolveStoreScope(storeId);
        log.info("分页查询员工操作日志: storeId={}, employeeId={}, actionType={}, page={}, pageSize={}",
                scopedStoreId, employeeId, actionType, page, pageSize);
        return Result.success(operationLogService.pageQuery(scopedStoreId, employeeId, actionType, page, pageSize));
    }

    @PostMapping("/login")
    @Operation(summary = "门店员工登录")
    public Result<StoreEmployeeLoginVO> login(@RequestBody StoreEmployeeLoginDTO loginDTO) {
        log.info("门店员工登录: {}", loginDTO.getLoginAccount());
        return Result.success(employeeService.login(loginDTO));
    }

    private void requireEmployeeManageAccess() {
        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role)) {
            return;
        }
        if ("STORE_STAFF".equals(role) && hasPermission("employee:manage")) {
            return;
        }
        throw new BaseException("无权限管理门店员工");
    }

    private boolean hasPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (permissionCodes == null || permissionCode == null) {
            return false;
        }
        for (String code : permissionCodes.split(",")) {
            if (permissionCode.equals(code != null ? code.trim() : null)) {
                return true;
            }
        }
        return false;
    }

    private Long resolveStoreScope(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if ("STORE_ADMIN".equals(role) || "STORE_STAFF".equals(role)) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前账号未绑定门店");
            }
            return storeId;
        }
        return requestedStoreId;
    }

    private void assertEmployeeStoreScope(StoreEmployee employee) {
        if (employee == null) {
            throw new BaseException("员工不存在");
        }
        String role = BaseContext.getRole();
        if (!"STORE_ADMIN".equals(role) && !"STORE_STAFF".equals(role)) {
            return;
        }
        Long storeId = BaseContext.getStoreId();
        if (storeId == null || employee.getStoreId() == null || !storeId.equals(employee.getStoreId())) {
            throw new BaseException("无权限操作该门店员工");
        }
        if ("STORE_STAFF".equals(role) && isManagerEmployee(employee)) {
            throw new BaseException("店长账号仅允许门店管理端查看和维护");
        }
    }

    private void rejectManagerTargetForStaff(StoreEmployeeDTO employeeDTO) {
        if (employeeDTO == null) {
            return;
        }
        if (!"STORE_STAFF".equals(BaseContext.getRole())) {
            return;
        }
        if ("MANAGER".equals(employeeDTO.getStaffRole()) || Integer.valueOf(1).equals(employeeDTO.getPosition()) || Integer.valueOf(2).equals(employeeDTO.getPosition())) {
            throw new BaseException("店长账号仅允许门店管理端维护");
        }
    }

    private boolean isManagerEmployee(StoreEmployee employee) {
        if (employee == null) {
            return false;
        }
        if ("MANAGER".equals(employee.getStaffRole())) {
            return true;
        }
        return Integer.valueOf(1).equals(employee.getPosition()) || Integer.valueOf(2).equals(employee.getPosition());
    }
}
