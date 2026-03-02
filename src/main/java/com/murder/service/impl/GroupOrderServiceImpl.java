package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.murder.entity.GroupOrder;
import com.murder.entity.GroupMember;
import com.murder.entity.User;
import com.murder.mapper.GroupOrderMapper;
import com.murder.mapper.GroupMemberMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.GroupOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupOrderServiceImpl extends ServiceImpl<GroupOrderMapper, GroupOrder> implements GroupOrderService {
    
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    
    @Override
    public Page<Map<String, Object>> pageQuery(Integer page, Integer pageSize, Long scriptId, Integer playerCount, Integer status) {
        Page<GroupOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (scriptId != null) {
            wrapper.eq(GroupOrder::getScriptId, scriptId);
        }
        if (playerCount != null) {
            wrapper.eq(GroupOrder::getPlayerCount, playerCount);
        }
        if (status != null) {
            wrapper.eq(GroupOrder::getStatus, status);
        } else {
            // 默认只查询拼团中和已成团的
            wrapper.in(GroupOrder::getStatus, Arrays.asList(1, 2));
        }
        
        wrapper.orderByDesc(GroupOrder::getCreateTime);
        
        Page<GroupOrder> result = this.page(pageParam, wrapper);
        
        // 转换为Map列表
        Page<Map<String, Object>> mapPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (GroupOrder group : result.getRecords()) {
            records.add(convertToMap(group));
        }
        mapPage.setRecords(records);
        return mapPage;
    }
    
    @Override
    public List<Map<String, Object>> getHotGroups(Integer limit) {
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1) // 拼团中
               .orderByDesc(GroupOrder::getCreateTime)
               .last("LIMIT " + limit);
        
        List<GroupOrder> groups = this.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupOrder group : groups) {
            result.add(convertToMap(group));
        }
        return result;
    }
    
    @Override
    public Map<String, Object> getDetailWithMembers(Long id) {
        GroupOrder group = this.getById(id);
        if (group == null) {
            return null;
        }
        
        Map<String, Object> result = convertToMapAnonymous(group);
        
        // 获取成员列表（匿名化处理）
        List<GroupMember> members = getGroupMembers(id);
        List<Map<String, Object>> anonymousMembers = new ArrayList<>();
        
        // 剧本杀角色代号列表
        String[] roleNames = {"神秘侦探", "暗夜行者", "迷雾旅人", "幽灵猎手", "黑袍法师", 
                              "白衣天使", "午夜骑士", "影子刺客", "星辰守望者", "命运编织者",
                              "时空旅者", "月光猎人", "风暴使者", "深渊行者", "光明守护"};
        
        int index = 0;
        for (GroupMember member : members) {
            Map<String, Object> anonymousMember = new HashMap<>();
            anonymousMember.put("id", member.getId());
            anonymousMember.put("isCreator", member.getIsCreator());
            anonymousMember.put("joinCount", member.getJoinCount());
            anonymousMember.put("createTime", member.getCreateTime());
            
            // 匿名化处理：使用角色代号
            String roleName = roleNames[index % roleNames.length];
            if (member.getIsCreator() == 1) {
                anonymousMember.put("anonymousName", "🎭 " + roleName + " (发起人)");
            } else {
                anonymousMember.put("anonymousName", "🎭 " + roleName);
            }
            // 不返回真实用户ID、昵称和头像
            anonymousMember.put("avatar", null);
            anonymousMember.put("nickname", roleName);
            
            anonymousMembers.add(anonymousMember);
            index++;
        }
        result.put("members", anonymousMembers);
        
        return result;
    }
    
    @Override
    @Transactional
    public GroupOrder createGroup(GroupOrder groupOrder, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        groupOrder.setCreatorId(userId);
        groupOrder.setCreatorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        groupOrder.setCreatorAvatar(user.getAvatar());
        groupOrder.setStatus(1); // 拼团中
        groupOrder.setCreateTime(LocalDateTime.now());
        groupOrder.setUpdateTime(LocalDateTime.now());
        
        // 如果当前人数未设置，默认为1（发起人自己）
        if (groupOrder.getCurrentCount() == null || groupOrder.getCurrentCount() < 1) {
            groupOrder.setCurrentCount(1);
        }
        
        this.save(groupOrder);
        
        // 添加发起人为成员
        GroupMember creator = new GroupMember();
        creator.setGroupId(groupOrder.getId());
        creator.setUserId(userId);
        creator.setNickname(groupOrder.getCreatorName());
        creator.setAvatar(groupOrder.getCreatorAvatar());
        creator.setIsCreator(1);
        creator.setJoinCount(groupOrder.getCurrentCount());
        creator.setStatus(1);
        creator.setCreateTime(LocalDateTime.now());
        groupMemberMapper.insert(creator);
        
        // 检查是否已成团
        checkAndUpdateGroupStatus(groupOrder);
        
        return groupOrder;
    }
    
    @Override
    @Transactional
    public boolean joinGroup(Long groupId, Long userId, Integer joinCount) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        if (group.getStatus() != 1) {
            throw new RuntimeException("该拼单已结束或已取消");
        }
        
        // 检查是否已加入
        LambdaQueryWrapper<GroupMember> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(GroupMember::getGroupId, groupId)
                   .eq(GroupMember::getUserId, userId)
                   .eq(GroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(checkWrapper) > 0) {
            throw new RuntimeException("您已经参加了该拼单");
        }
        
        // 检查人数
        if (joinCount == null || joinCount < 1) {
            joinCount = 1;
        }
        if (group.getCurrentCount() + joinCount > group.getNeedCount()) {
            throw new RuntimeException("剩余名额不足");
        }
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        
        // 添加成员
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setNickname(user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "用户" + userId);
        member.setAvatar(user != null ? user.getAvatar() : null);
        member.setIsCreator(0);
        member.setJoinCount(joinCount);
        member.setStatus(1);
        member.setCreateTime(LocalDateTime.now());
        groupMemberMapper.insert(member);
        
        // 更新当前人数
        group.setCurrentCount(group.getCurrentCount() + joinCount);
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        
        // 检查是否已成团
        checkAndUpdateGroupStatus(group);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean leaveGroup(Long groupId, Long userId) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        
        // 发起人不能退出
        if (group.getCreatorId().equals(userId)) {
            throw new RuntimeException("发起人不能退出，如需取消请使用取消功能");
        }
        
        // 查找成员记录
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
               .eq(GroupMember::getUserId, userId)
               .eq(GroupMember::getStatus, 1);
        GroupMember member = groupMemberMapper.selectOne(wrapper);
        
        if (member == null) {
            throw new RuntimeException("您未参加该拼单");
        }
        
        // 更新成员状态
        member.setStatus(0);
        groupMemberMapper.updateById(member);
        
        // 更新当前人数
        group.setCurrentCount(group.getCurrentCount() - member.getJoinCount());
        if (group.getStatus() == 2) {
            group.setStatus(1); // 重新变为拼团中
        }
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean cancelGroup(Long groupId, Long userId) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        
        // 只有发起人可以取消
        if (!group.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有发起人可以取消拼单");
        }
        
        if (group.getStatus() != 1) {
            throw new RuntimeException("只能取消拼团中的拼单");
        }
        
        group.setStatus(0); // 已取消
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        
        return true;
    }
    
    @Override
    public Page<Map<String, Object>> getMyGroups(Long userId, Integer page, Integer pageSize, Integer type) {
        Page<GroupOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (type == null || type == 0) {
            // 我发起的
            wrapper.eq(GroupOrder::getCreatorId, userId);
        } else {
            // 我参与的（需要从成员表查询）
            LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(GroupMember::getUserId, userId)
                        .eq(GroupMember::getStatus, 1)
                        .eq(GroupMember::getIsCreator, 0);
            List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
            if (members.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            List<Long> groupIds = new ArrayList<>();
            for (GroupMember m : members) {
                groupIds.add(m.getGroupId());
            }
            wrapper.in(GroupOrder::getId, groupIds);
        }
        
        wrapper.orderByDesc(GroupOrder::getCreateTime);
        
        Page<GroupOrder> result = this.page(pageParam, wrapper);
        
        Page<Map<String, Object>> mapPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (GroupOrder group : result.getRecords()) {
            records.add(convertToMap(group));
        }
        mapPage.setRecords(records);
        return mapPage;
    }
    
    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
               .eq(GroupMember::getStatus, 1)
               .orderByAsc(GroupMember::getCreateTime);
        return groupMemberMapper.selectList(wrapper);
    }
    
    /**
     * 检查并更新拼单状态
     */
    private void checkAndUpdateGroupStatus(GroupOrder group) {
        if (group.getCurrentCount() >= group.getNeedCount() && group.getStatus() == 1) {
            group.setStatus(2); // 已成团
            group.setUpdateTime(LocalDateTime.now());
            this.updateById(group);
        }
    }
    
    /**
     * 转换为Map（列表页使用，匿名发起人）
     */
    private Map<String, Object> convertToMap(GroupOrder group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        // 列表页不返回发起人真实信息，使用匿名展示
        map.put("creatorName", "神秘车主");
        map.put("creatorAvatar", null);
        map.put("scriptId", group.getScriptId());
        map.put("scriptName", group.getScriptName());
        map.put("storeId", group.getStoreId());
        map.put("storeName", group.getStoreName());
        map.put("playTime", group.getPlayTime());
        map.put("currentCount", group.getCurrentCount());
        map.put("needCount", group.getNeedCount());
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("status", group.getStatus());
        map.put("createTime", group.getCreateTime());
        return map;
    }
    
    /**
     * 转换为Map（详情页使用，匿名发起人）
     */
    private Map<String, Object> convertToMapAnonymous(GroupOrder group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        // 详情页也不返回发起人真实信息
        map.put("creatorName", "神秘车主");
        map.put("creatorAvatar", null);
        map.put("scriptId", group.getScriptId());
        map.put("scriptName", group.getScriptName());
        map.put("storeId", group.getStoreId());
        map.put("storeName", group.getStoreName());
        map.put("playTime", group.getPlayTime());
        map.put("currentCount", group.getCurrentCount());
        map.put("needCount", group.getNeedCount());
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("difficulty", 2); // 默认难度：进阶
        map.put("status", group.getStatus());
        map.put("createTime", group.getCreateTime());
        return map;
    }
}
