package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.entity.StoreEmployee;
import com.murder.vo.StoreEmployeeVO;

import java.util.List;

/**
 * 门店员工服务接口
 */
public interface StoreEmployeeService {

    /**
     * 分页查询门店员工列表
     */
    PageResult<StoreEmployeeVO> pageQuery(Long storeId, Integer page, Integer pageSize);

    /**
     * 根据门店ID查询员工列表
     */
    List<StoreEmployee> listByStoreId(Long storeId);

    /**
     * 根据ID查询员工详情
     */
    StoreEmployee getById(Long id);

    /**
     * 新增员工
     */
    void add(StoreEmployeeDTO employeeDTO);

    /**
     * 更新员工
     */
    void update(StoreEmployeeDTO employeeDTO);

    /**
     * 删除员工
     */
    void delete(Long id);
    
    /**
     * 批量删除员工
     */
    void batchDelete(List<Long> ids);
    
    /**
     * 更新员工状�?
     */
    void updateStatus(Long id, Integer status);
}
