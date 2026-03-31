package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.StoreEmployeeOperationLog;

public interface StoreEmployeeOperationLogService {

    void record(Long storeId,
                String actionType,
                String targetType,
                Long targetId,
                String targetName,
                String detail);

    PageResult<StoreEmployeeOperationLog> pageQuery(Long storeId,
                                                    Long employeeId,
                                                    String actionType,
                                                    Integer page,
                                                    Integer pageSize);
}
