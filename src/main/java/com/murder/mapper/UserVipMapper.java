package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.UserVip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户VIP Mapper
 */
@Mapper
public interface UserVipMapper extends BaseMapper<UserVip> {
    
    /**
     * 获取用户当前有效的VIP记录
     */
    @Select("SELECT * FROM user_vip WHERE user_id = #{userId} AND status = 1 AND end_time > CURRENT_TIMESTAMP ORDER BY end_time DESC LIMIT 1")
    UserVip getCurrentVip(@Param("userId") Long userId);
}

