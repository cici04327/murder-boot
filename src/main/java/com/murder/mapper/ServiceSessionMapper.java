package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.entity.ServiceSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ServiceSessionMapper extends BaseMapper<ServiceSession> {

    /**
     * 统计等待接入的会话数
     */
    @Select("SELECT COUNT(*) FROM service_session WHERE status = 0 AND is_deleted = 0")
    int countWaiting();

    /**
     * 分页查询会话列表（带用户信息）
     */
    @Select("<script>" +
            "SELECT ss.*, u.nickname as user_name " +
            "FROM service_session ss " +
            "LEFT JOIN user u ON ss.user_id = u.id " +
            "WHERE ss.is_deleted = 0 " +
            "<if test='status != null'> AND ss.status = #{status} </if>" +
            "ORDER BY ss.create_time DESC" +
            "</script>")
    IPage<ServiceSession> pageWithUserInfo(Page<ServiceSession> page,
                                           @Param("status") Integer status);
}
