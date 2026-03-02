package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.entity.Store;
import com.murder.entity.StoreEmployee;
import com.murder.vo.StoreEmployeeVO;
import com.murder.mapper.StoreEmployeeMapper;
import com.murder.mapper.StoreMapper;
import com.murder.service.StoreEmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public PageResult<StoreEmployeeVO> pageQuery(Long storeId, Integer page, Integer pageSize) {
        Page<StoreEmployee> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<StoreEmployee> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(StoreEmployee::getStoreId, storeId);
        }
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
        employeeMapper.insert(employee);
    }

    @Override
    public void update(StoreEmployeeDTO employeeDTO) {
        StoreEmployee employee = new StoreEmployee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.updateById(employee);
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
        StoreEmployee employee = new StoreEmployee();
        employee.setId(id);
        employee.setStatus(status);
        employeeMapper.updateById(employee);
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
        
        return vo;
    }
}
