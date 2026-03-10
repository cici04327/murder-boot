package com.murder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.murder.entity.GroupOrder;
import com.murder.entity.GroupMember;

import java.util.List;
import java.util.Map;

public interface GroupOrderService extends IService<GroupOrder> {
    
    /**
     * 分页查询拼单
     */
    Page<Map<String, Object>> pageQuery(Integer page, Integer pageSize, Long scriptId, Long categoryId, Integer playerCount, Integer status);
    
    /**
     * 获取热门拼单
     */
    List<Map<String, Object>> getHotGroups(Integer limit);
    
    /**
     * 获取拼单详情（包含成员列表）
     */
    Map<String, Object> getDetailWithMembers(Long id);
    
    /**
     * 创建拼单
     */
    GroupOrder createGroup(GroupOrder groupOrder, Long userId);
    
    /**
     * 加入拼单
     */
    boolean joinGroup(Long groupId, Long userId, Integer joinCount);
    
    /**
     * 退出拼单
     */
    boolean leaveGroup(Long groupId, Long userId);
    
    /**
     * 取消拼单
     */
    boolean cancelGroup(Long groupId, Long userId);
    
    /**
     * 获取我的拼单
     */
    Page<Map<String, Object>> getMyGroups(Long userId, Integer page, Integer pageSize, Integer type);
    
    /**
     * 获取拼单成员
     */
    List<GroupMember> getGroupMembers(Long groupId);

    /**
     * 处理超时未成团的拼单
     */
    void processExpiredGroups();
}
