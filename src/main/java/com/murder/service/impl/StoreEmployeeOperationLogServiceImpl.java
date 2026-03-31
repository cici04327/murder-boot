package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.entity.StoreEmployee;
import com.murder.entity.StoreEmployeeOperationLog;
import com.murder.entity.User;
import com.murder.mapper.StoreEmployeeMapper;
import com.murder.mapper.StoreEmployeeOperationLogMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.StoreEmployeeOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StoreEmployeeOperationLogServiceImpl implements StoreEmployeeOperationLogService {

    @Autowired
    private StoreEmployeeOperationLogMapper operationLogMapper;

    @Autowired(required = false)
    private StoreEmployeeMapper storeEmployeeMapper;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Override
    public void record(Long storeId,
                       String actionType,
                       String targetType,
                       Long targetId,
                       String targetName,
                       String detail) {
        StoreEmployeeOperationLog log = StoreEmployeeOperationLog.builder()
                .storeId(storeId != null ? storeId : BaseContext.getStoreId())
                .operatorId(resolveOperatorId())
                .employeeId(BaseContext.getEmployeeId())
                .operatorType(resolveOperatorType())
                .operatorRole(BaseContext.getRole())
                .operatorName(resolveOperatorName())
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .detail(detail)
                .build();
        operationLogMapper.insert(log);
    }

    @Override
    public PageResult<StoreEmployeeOperationLog> pageQuery(Long storeId,
                                                           Long employeeId,
                                                           String actionType,
                                                           Integer page,
                                                           Integer pageSize) {
        Page<StoreEmployeeOperationLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<StoreEmployeeOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(StoreEmployeeOperationLog::getStoreId, storeId);
        }
        if (employeeId != null) {
            wrapper.eq(StoreEmployeeOperationLog::getEmployeeId, employeeId);
        }
        if (StringUtils.hasText(actionType)) {
            wrapper.eq(StoreEmployeeOperationLog::getActionType, actionType);
        }
        wrapper.orderByDesc(StoreEmployeeOperationLog::getCreateTime, StoreEmployeeOperationLog::getId);

        Long total = operationLogMapper.selectCount(wrapper);
        List<StoreEmployeeOperationLog> records = operationLogMapper.selectPage(pageInfo, wrapper).getRecords();
        return new PageResult<>(total, records);
    }

    private Long resolveOperatorId() {
        if (BaseContext.getEmployeeId() != null) {
            return BaseContext.getEmployeeId();
        }
        return BaseContext.getCurrentId();
    }

    private String resolveOperatorType() {
        return BaseContext.getEmployeeId() != null ? "STORE_STAFF" : "ADMIN";
    }

    private String resolveOperatorName() {
        if (BaseContext.getEmployeeId() != null && storeEmployeeMapper != null) {
            StoreEmployee employee = storeEmployeeMapper.selectById(BaseContext.getEmployeeId());
            if (employee != null && StringUtils.hasText(employee.getName())) {
                return employee.getName();
            }
        }
        if (BaseContext.getCurrentId() != null && userMapper != null) {
            User user = userMapper.selectById(BaseContext.getCurrentId());
            if (user != null) {
                if (StringUtils.hasText(user.getNickname())) {
                    return user.getNickname();
                }
                if (StringUtils.hasText(user.getUsername())) {
                    return user.getUsername();
                }
            }
        }
        return "系统";
    }
}
