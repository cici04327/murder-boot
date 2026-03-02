package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.StoreEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 门店员工Mapper
 */
@Mapper
public interface StoreEmployeeMapper extends BaseMapper<StoreEmployee> {
    
    /**
     * 统计门店员工�?
     */
    @Select("SELECT COUNT(*) FROM store_employee WHERE store_id = #{storeId} AND is_deleted = 0")
    Long countByStoreId(Long storeId);
    
    /**
     * 统计在职员工�?
     */
    @Select("SELECT COUNT(*) FROM store_employee WHERE store_id = #{storeId} AND status = 1 AND is_deleted = 0")
    Long countActiveByStoreId(Long storeId);
}
