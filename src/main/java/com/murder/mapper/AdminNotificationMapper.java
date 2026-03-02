package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.AdminNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理端通知Mapper
 */
@Mapper
public interface AdminNotificationMapper extends BaseMapper<AdminNotification> {
    
    /**
     * 统计未读通知数量
     */
    @Select("SELECT COUNT(*) FROM admin_notification WHERE is_read = 0 AND is_deleted = 0")
    Long countUnread();
    
    /**
     * 统计指定门店未读通知数量
     */
    @Select("SELECT COUNT(*) FROM admin_notification WHERE store_id = #{storeId} AND is_read = 0 AND is_deleted = 0")
    Long countUnreadByStoreId(Long storeId);
}
