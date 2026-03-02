package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户浏览记录Mapper
 */
@Mapper
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory> {

    /**
     * 获取用户最近浏览的剧本ID列表
     */
    @Select("SELECT DISTINCT target_id FROM user_browse_history " +
            "WHERE user_id = #{userId} AND target_type = 1 AND is_deleted = 0 " +
            "ORDER BY browse_time DESC LIMIT #{limit}")
    List<Long> getRecentBrowseScriptIds(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 获取用户浏览过的剧本分类统计
     */
    @Select("SELECT s.category_id, COUNT(*) as count " +
            "FROM user_browse_history h " +
            "INNER JOIN script s ON h.target_id = s.id " +
            "WHERE h.user_id = #{userId} AND h.target_type = 1 AND h.is_deleted = 0 AND s.is_deleted = 0 " +
            "GROUP BY s.category_id " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getUserBrowseCategoryStats(@Param("userId") Long userId);

    /**
     * 获取用户浏览过的剧本类型统计
     */
    @Select("SELECT s.type, COUNT(*) as count " +
            "FROM user_browse_history h " +
            "INNER JOIN script s ON h.target_id = s.id " +
            "WHERE h.user_id = #{userId} AND h.target_type = 1 AND h.is_deleted = 0 AND s.is_deleted = 0 " +
            "GROUP BY s.type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getUserBrowseTypeStats(@Param("userId") Long userId);

    /**
     * 清理过期的浏览记录（超过90天）
     */
    @Delete("DELETE FROM user_browse_history WHERE browse_time < #{expireTime}")
    int cleanExpiredRecords(@Param("expireTime") LocalDateTime expireTime);
}
