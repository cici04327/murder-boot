package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.AiScheduleTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI排班任务记录 Mapper
 */
@Mapper
public interface AiScheduleTaskMapper extends BaseMapper<AiScheduleTask> {

    /**
     * 查询门店任务历史（含门店名称）
     */
    @Select("SELECT t.*, s.name as store_name " +
            "FROM ai_schedule_task t " +
            "LEFT JOIN store s ON t.store_id = s.id " +
            "WHERE t.store_id = #{storeId} AND t.is_deleted = 0 " +
            "ORDER BY t.create_time DESC " +
            "LIMIT #{limit}")
    List<AiScheduleTask> listByStore(@Param("storeId") Long storeId, @Param("limit") int limit);
}
