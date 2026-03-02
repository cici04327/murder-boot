package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.UserPreference;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户偏好Mapper
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {

    /**
     * 获取用户的偏好列表（按分数排序）
     */
    @Select("SELECT * FROM user_preference " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY score DESC LIMIT #{limit}")
    List<UserPreference> getUserTopPreferences(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 获取用户特定类型的偏好
     */
    @Select("SELECT preference_value, score FROM user_preference " +
            "WHERE user_id = #{userId} AND preference_type LIKE CONCAT(#{typePrefix}, '%') AND is_deleted = 0 " +
            "ORDER BY score DESC")
    List<Map<String, Object>> getUserPreferencesByType(@Param("userId") Long userId, 
                                                        @Param("typePrefix") String typePrefix);

    /**
     * 增加偏好计数和分数（使用INSERT ... ON DUPLICATE KEY UPDATE）
     * 注意：ON DUPLICATE KEY UPDATE 可能返回 1（插入）或 2（更新）
     */
    @Insert("INSERT INTO user_preference (user_id, preference_type, preference_value, score, count) " +
            "VALUES (#{userId}, #{preferenceType}, #{preferenceValue}, #{score}, 1) " +
            "ON DUPLICATE KEY UPDATE count = count + 1, score = score + #{score}, update_time = NOW()")
    @Options(useGeneratedKeys = false)
    void incrementPreference(@Param("userId") Long userId, 
                            @Param("preferenceType") String preferenceType,
                            @Param("preferenceValue") String preferenceValue,
                            @Param("score") Double score);
}
