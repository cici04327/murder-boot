package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户通知记录 Mapper
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
    
    /**
     * 查询用户未读通知数量
     */
    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    Long countUnreadByUserId(@Param("userId") Long userId);
}
