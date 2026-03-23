package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.properties.JwtProperties;
import com.murder.dto.StoreEmployeeLoginDTO;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.entity.Store;
import com.murder.entity.StoreEmployee;
import com.murder.entity.Dm;
import com.murder.vo.StoreEmployeeVO;
import com.murder.vo.StoreEmployeeLoginVO;
import com.murder.mapper.DmMapper;
import com.murder.mapper.StoreEmployeeMapper;
import com.murder.mapper.StoreMapper;
import com.murder.service.StoreEmployeeOperationLogService;
import com.murder.service.StoreEmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店员工服务实现?
 */
@Service
public class StoreEmployeeServiceImpl implements StoreEmployeeService {

    @Autowired
    private StoreEmployeeMapper employeeMapper;
    
    @Autowired
    private StoreMapper storeMapper;

    @Autowired(required = false)
    private DmMapper dmMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired(required = false)
    private StoreEmployeeOperationLogService operationLogService;

    @Override
    public PageResult<StoreEmployeeVO> pageQuery(Long storeId, Integer page, Integer pageSize) {
        Page<StoreEmployee> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<StoreEmployee> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(StoreEmployee::getStoreId, storeId);
        }
        applyManagerVisibleScope(wrapper);
        wrapper.orderByDesc(StoreEmployee::getCreateTime);
        
        // 手动查询总数
        Long total = employeeMapper.selectCount(wrapper);
        
        employeeMapper.selectPage(pageInfo, wrapper);
        
        // 批量查询门店信息
        List<Long> storeIds = pageInfo.getRecords().stream()
                .map(StoreEmployee::getStoreId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, String> storeNameMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            List<Store> stores = storeMapper.selectBatchIds(storeIds);
            stores.forEach(store -> storeNameMap.put(store.getId(), store.getName()));
        }
        
        // 转换为VO
        List<StoreEmployeeVO> voList = pageInfo.getRecords().stream()
                .map(employee -> convertToVO(employee, storeNameMap))
                .collect(Collectors.toList());
        
        // 使用手动查询?total ?
        return new PageResult<>(total, voList);
    }

    @Override
    public List<StoreEmployee> listByStoreId(Long storeId) {
        LambdaQueryWrapper<StoreEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreEmployee::getStoreId, storeId);
        applyManagerVisibleScope(wrapper);
        wrapper.orderByAsc(StoreEmployee::getPosition);
        return employeeMapper.selectList(wrapper);
    }

    @Override
    public StoreEmployee getById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public void add(StoreEmployeeDTO employeeDTO) {
        StoreEmployee employee = new StoreEmployee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setStatus(1); // 默认在职
        fillAccountFields(employee, employeeDTO, true);
        applyRolePermissionLimit(employee);
        validateLoginAccountUnique(employee.getLoginAccount(), null);
        employeeMapper.insert(employee);
        recordEmployeeLog(employee.getStoreId(), "EMPLOYEE_CREATE", employee.getId(), employee.getName(),
                buildEmployeeChangeDetail(null, employee, true));
    }

    @Override
    public void update(StoreEmployeeDTO employeeDTO) {
        StoreEmployee existing = employeeMapper.selectById(employeeDTO.getId());
        if (existing == null) {
            throw new RuntimeException("员工不存在");
        }
        StoreEmployee employee = new StoreEmployee();
        BeanUtils.copyProperties(employeeDTO, employee);
        fillAccountFields(employee, employeeDTO, false);
        if (!StringUtils.hasText(employee.getLoginPassword())) {
            employee.setLoginPassword(existing.getLoginPassword());
        }
        if (!StringUtils.hasText(employee.getLoginAccount())) {
            employee.setLoginAccount(existing.getLoginAccount());
        }
        if (!StringUtils.hasText(employee.getStaffRole())) {
            employee.setStaffRole(existing.getStaffRole());
        }
        if (!StringUtils.hasText(employee.getPermissionCodes())) {
            employee.setPermissionCodes(existing.getPermissionCodes());
        }
        applyRolePermissionLimit(employee);
        if (employee.getDmId() == null) {
            employee.setDmId(existing.getDmId());
        }
        validateLoginAccountUnique(employee.getLoginAccount(), employee.getId());
        employeeMapper.updateById(employee);
        recordEmployeeLog(employee.getStoreId(), "EMPLOYEE_UPDATE", employee.getId(), employee.getName(),
                buildEmployeeChangeDetail(existing, employee, false));
    }

    @Override
    public void delete(Long id) {
        employeeMapper.deleteById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        employeeMapper.deleteBatchIds(ids);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        StoreEmployee existing = employeeMapper.selectById(id);
        StoreEmployee employee = new StoreEmployee();
        employee.setId(id);
        employee.setStatus(status);
        employeeMapper.updateById(employee);
        if (existing != null) {
            existing.setStatus(status);
            recordEmployeeLog(existing.getStoreId(), "EMPLOYEE_STATUS", existing.getId(), existing.getName(),
                    "账号状态调整为：" + (Integer.valueOf(1).equals(status) ? "启用" : "禁用"));
        }
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        StoreEmployee existing = employeeMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("员工不存在");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new RuntimeException("新密码不能为空");
        }
        StoreEmployee employee = new StoreEmployee();
        employee.setId(id);
        employee.setLoginPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        employeeMapper.updateById(employee);
        recordEmployeeLog(existing.getStoreId(), "EMPLOYEE_RESET_PASSWORD", existing.getId(), existing.getName(), "手动重置员工登录密码");
    }

    @Override
    public StoreEmployeeLoginVO login(StoreEmployeeLoginDTO loginDTO) {
        LambdaQueryWrapper<StoreEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreEmployee::getLoginAccount, loginDTO.getLoginAccount())
                .eq(StoreEmployee::getIsDeleted, 0)
                .last("LIMIT 1");
        StoreEmployee employee = employeeMapper.selectOne(wrapper);

        if (employee == null) {
            throw new RuntimeException("员工账号不存在");
        }
        if (!Integer.valueOf(1).equals(employee.getStatus())) {
            throw new RuntimeException("该员工账号已停用");
        }
        if (!StringUtils.hasText(employee.getLoginPassword())) {
            throw new RuntimeException("员工账号未初始化密码");
        }

        String password = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!password.equals(employee.getLoginPassword())) {
            throw new RuntimeException("密码错误");
        }

        Store store = storeMapper.selectById(employee.getStoreId());
        if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
            throw new RuntimeException("所属门店不可用，无法登录");
        }

        String staffRole = StringUtils.hasText(employee.getStaffRole())
                ? employee.getStaffRole()
                : resolveStaffRole(employee.getPosition());
        String permissionCodes = StringUtils.hasText(employee.getPermissionCodes())
                ? employee.getPermissionCodes()
                : resolvePermissionCodes(staffRole);

        employee.setLastLoginTime(LocalDateTime.now());
        employee.setStaffRole(staffRole);
        employee.setPermissionCodes(permissionCodes);
        employeeMapper.updateById(employee);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, -employee.getId());
        claims.put(JwtClaimsConstant.EMPLOYEE_ID, employee.getId());
        claims.put(JwtClaimsConstant.STORE_ID, employee.getStoreId());
        claims.put(JwtClaimsConstant.ROLE, "STORE_STAFF");
        claims.put(JwtClaimsConstant.STAFF_ROLE, staffRole);
        claims.put(JwtClaimsConstant.PERMISSION_CODES, permissionCodes);
        if (employee.getDmId() != null) {
            claims.put(JwtClaimsConstant.DM_ID, employee.getDmId());
        }

        String token = com.murder.common.utils.JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        return StoreEmployeeLoginVO.builder()
                .employeeId(employee.getId())
                .name(employee.getName())
                .storeId(employee.getStoreId())
                .storeName(store.getName())
                .loginAccount(employee.getLoginAccount())
                .role("STORE_STAFF")
                .staffRole(staffRole)
                .permissionCodes(permissionCodes)
                .dmId(employee.getDmId())
                .token(token)
                .build();
    }
    
    /**
     * 转换为VO
     */
    private StoreEmployeeVO convertToVO(StoreEmployee employee, Map<Long, String> storeNameMap) {
        StoreEmployeeVO vo = new StoreEmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        
        // 设置门店名称
        if (employee.getStoreId() != null) {
            vo.setStoreName(storeNameMap.get(employee.getStoreId()));
        }
        
        // 设置职位名称
        if (employee.getPosition() != null) {
            String[] positions = {"", "店长", "副店长", "主持人", "服务员"};
            if (employee.getPosition() > 0 && employee.getPosition() < positions.length) {
                vo.setPositionName(positions[employee.getPosition()]);
            }
        }

        if (employee.getDmId() != null && dmMapper != null) {
            Dm dm = dmMapper.selectById(employee.getDmId());
            if (dm != null) {
                vo.setDmName(dm.getName());
            }
        }
        
        return vo;
    }

    private void fillAccountFields(StoreEmployee employee, StoreEmployeeDTO dto, boolean creating) {
        if (!StringUtils.hasText(employee.getLoginAccount()) && StringUtils.hasText(employee.getPhone())) {
            employee.setLoginAccount(employee.getPhone());
        }

        String staffRole = StringUtils.hasText(dto.getStaffRole()) ? dto.getStaffRole() : resolveStaffRole(employee.getPosition());
        employee.setStaffRole(staffRole);

        if (!StringUtils.hasText(dto.getPermissionCodes())) {
            employee.setPermissionCodes(resolvePermissionCodes(staffRole));
        }

        if (StringUtils.hasText(dto.getLoginPassword())) {
            employee.setLoginPassword(DigestUtils.md5DigestAsHex(dto.getLoginPassword().getBytes()));
        } else if (creating && StringUtils.hasText(employee.getLoginAccount())) {
            employee.setLoginPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        }
    }

    private String resolveStaffRole(Integer position) {
        if (position == null) {
            return "CLERK";
        }
        if (position == 1 || position == 2) {
            return "MANAGER";
        }
        if (position == 3) {
            return "DM";
        }
        return "CLERK";
    }

    private String resolvePermissionCodes(String staffRole) {
        if ("MANAGER".equals(staffRole)) {
            return "reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process,employee:manage,report:view,notification:view";
        }
        if ("DM".equals(staffRole)) {
            return "reservation:view,reservation:complete,notification:view";
        }
        return "reservation:view,reservation:checkin,notification:view";
    }

    private void applyRolePermissionLimit(StoreEmployee employee) {
        if (employee == null) {
            return;
        }
        String staffRole = StringUtils.hasText(employee.getStaffRole()) ? employee.getStaffRole() : resolveStaffRole(employee.getPosition());
        java.util.LinkedHashSet<String> allowedCodes = new java.util.LinkedHashSet<>();
        for (String code : resolvePermissionCodes(staffRole).split(",")) {
            if (StringUtils.hasText(code)) {
                allowedCodes.add(code.trim());
            }
        }

        if (!StringUtils.hasText(employee.getPermissionCodes())) {
            employee.setPermissionCodes(String.join(",", allowedCodes));
            return;
        }

        java.util.LinkedHashSet<String> selectedCodes = new java.util.LinkedHashSet<>();
        for (String code : employee.getPermissionCodes().split(",")) {
            if (StringUtils.hasText(code) && allowedCodes.contains(code.trim())) {
                selectedCodes.add(code.trim());
            }
        }
        employee.setPermissionCodes(String.join(",", selectedCodes));
    }

    private void applyManagerVisibleScope(LambdaQueryWrapper<StoreEmployee> wrapper) {
        if (wrapper == null) {
            return;
        }
        if (!"STORE_STAFF".equals(BaseContext.getRole()) || !"MANAGER".equals(BaseContext.getStaffRole())) {
            return;
        }
        wrapper.ne(StoreEmployee::getStaffRole, "MANAGER");
    }

    private void validateLoginAccountUnique(String loginAccount, Long currentEmployeeId) {
        if (!StringUtils.hasText(loginAccount)) {
            return;
        }
        LambdaQueryWrapper<StoreEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreEmployee::getLoginAccount, loginAccount)
                .eq(StoreEmployee::getIsDeleted, 0);
        if (currentEmployeeId != null) {
            wrapper.ne(StoreEmployee::getId, currentEmployeeId);
        }
        Long count = employeeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("该员工登录账号已被占用");
        }
    }

    private void recordEmployeeLog(Long storeId, String actionType, Long employeeId, String employeeName, String detail) {
        if (operationLogService == null) {
            return;
        }
        try {
            operationLogService.record(storeId, actionType, "STORE_EMPLOYEE", employeeId, employeeName, detail);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String buildEmployeeChangeDetail(StoreEmployee before, StoreEmployee after, boolean creating) {
        if (creating) {
            return String.format("新增员工账号：角色=%s，登录账号=%s，绑定DM=%s",
                    after.getStaffRole(),
                    after.getLoginAccount(),
                    after.getDmId() == null ? "未绑定" : after.getDmId());
        }
        StringBuilder builder = new StringBuilder("更新员工资料：");
        if (!safeEquals(before.getStaffRole(), after.getStaffRole())) {
            builder.append("角色 ").append(before.getStaffRole()).append(" -> ").append(after.getStaffRole()).append("；");
        }
        if (!safeEquals(before.getLoginAccount(), after.getLoginAccount())) {
            builder.append("登录账号 ").append(defaultText(before.getLoginAccount())).append(" -> ").append(defaultText(after.getLoginAccount())).append("；");
        }
        if (!safeEquals(before.getPermissionCodes(), after.getPermissionCodes())) {
            builder.append("权限已调整；");
        }
        if (!safeEquals(before.getDmId(), after.getDmId())) {
            builder.append("绑定DM ").append(before.getDmId() == null ? "未绑定" : before.getDmId())
                    .append(" -> ").append(after.getDmId() == null ? "未绑定" : after.getDmId()).append("；");
        }
        if (builder.length() == 8) {
            builder.append("基础信息更新");
        }
        return builder.toString();
    }

    private boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "未设置";
    }
}
