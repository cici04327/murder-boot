package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ServiceMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ServiceMessageMapper extends BaseMapper<ServiceMessage> {

    /**
     * 获取会话的所有消息（按时间排序）
     */
    @Select("SELECT * FROM service_message WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ServiceMessage> listBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 将指定会话的未读消息标为已读
     */
    @Update("UPDATE service_message SET is_read = 1 WHERE session_id = #{sessionId} AND sender_type = #{senderType} AND is_read = 0")
    void markAsRead(@Param("sessionId") Long sessionId, @Param("senderType") String senderType);

}
