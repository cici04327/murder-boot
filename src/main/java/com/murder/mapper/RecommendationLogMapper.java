package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.RecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 推荐记录Mapper
 */
@Mapper
public interface RecommendationLogMapper extends BaseMapper<RecommendationLog> {

    /**
     * 更新点击状态
     */
    @Update("UPDATE recommendation_log SET is_clicked = 1, click_time = NOW() " +
            "WHERE user_id = #{userId} AND script_id = #{scriptId} AND is_deleted = 0")
    int updateClickStatus(@Param("userId") Long userId, @Param("scriptId") Long scriptId);

    /**
     * 更新预约状态
     */
    @Update("UPDATE recommendation_log SET is_reserved = 1, reserve_time = NOW() " +
            "WHERE user_id = #{userId} AND script_id = #{scriptId} AND is_deleted = 0")
    int updateReserveStatus(@Param("userId") Long userId, @Param("scriptId") Long scriptId);

    /**
     * 获取推荐转化率统计
     */
    @Select("SELECT recommendation_type, " +
            "COUNT(*) as total_count, " +
            "SUM(is_clicked) as click_count, " +
            "SUM(is_reserved) as reserve_count, " +
            "ROUND(SUM(is_clicked) * 100.0 / COUNT(*), 2) as click_rate, " +
            "ROUND(SUM(is_reserved) * 100.0 / COUNT(*), 2) as reserve_rate " +
            "FROM recommendation_log " +
            "WHERE is_deleted = 0 AND create_time >= #{startTime} " +
            "GROUP BY recommendation_type")
    List<Map<String, Object>> getRecommendationStats(@Param("startTime") String startTime);

    /**
     * 获取用户最近未点击的推荐剧本ID（去重）
     */
    @Select("SELECT DISTINCT script_id FROM recommendation_log " +
            "WHERE user_id = #{userId} AND is_clicked = 0 AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Long> getRecentUnclickedScriptIds(@Param("userId") Long userId, @Param("limit") Integer limit);
}
