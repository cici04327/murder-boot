package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.SystemNotification;
import com.murder.entity.UserNotification;
import com.murder.vo.NotificationVO;
import com.murder.mapper.SystemNotificationMapper;
import com.murder.mapper.UserNotificationMapper;
import com.murder.service.NotificationService;
import com.murder.websocket.NotificationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现�?
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private SystemNotificationMapper systemNotificationMapper;
    
    @Autowired
    private UserNotificationMapper userNotificationMapper;
    
    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public void sendToUsers(String title, String content, Integer type, String bizType, Long bizId, Long... userIds) {
        if (userIds == null || userIds.length == 0) {
            return;
        }
        
        // 创建系统通知
        SystemNotification notification = SystemNotification.builder()
                .title(title)
                .content(content)
                .type(type)
                .bizType(bizType)
                .bizId(bizId)
                .targetType(2) // 指定用户
                .targetUsers(Arrays.stream(userIds).map(String::valueOf).collect(Collectors.joining(",")))
                .sendTime(LocalDateTime.now())
                .status(2) // 已发送
                .isDeleted(0) // 逻辑删除默认未删除（否则MP默认查询 is_deleted=0 会查不到）
                .build();
        
        systemNotificationMapper.insert(notification);
        
        // 为每个用户创建通知记录
        for (Long userId : userIds) {
            UserNotification userNotification = UserNotification.builder()
                    .userId(userId)
                    .notificationId(notification.getId())
                    .isRead(0)
                    .isDeleted(0)
                    .build();
            userNotificationMapper.insert(userNotification);
            
            // 实时推送通知给用�?
            pushNotificationToUser(userId, notification, userNotification);
        }
        
        log.info("发送通知成功: title={}, userIds={}", title, Arrays.toString(userIds));
    }

    @Override
    @Transactional
    public void sendToAll(String title, String content, Integer type, String bizType, Long bizId) {
        // 创建系统通知
        SystemNotification notification = SystemNotification.builder()
                .title(title)
                .content(content)
                .type(type)
                .bizType(bizType)
                .bizId(bizId)
                .targetType(1) // 全体用户
                .sendTime(LocalDateTime.now())
                .status(2) // 已发送
                .isDeleted(0) // 逻辑删除默认未删除
                .build();
        
        systemNotificationMapper.insert(notification);
        
        // 实时推送通知给所有在线用�?
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("id", notification.getId());
        wsMessage.put("title", notification.getTitle());
        wsMessage.put("content", notification.getContent());
        wsMessage.put("type", notification.getType());
        wsMessage.put("typeName", getTypeName(notification.getType()));
        wsMessage.put("bizType", notification.getBizType());
        wsMessage.put("bizId", notification.getBizId());
        wsMessage.put("sendTime", notification.getSendTime());
        wsMessage.put("isRead", false);
        webSocketHandler.pushNotificationToAll(wsMessage);
        
        // TODO: 可以用定时任务为所有用户创建通知记录，或者在用户查询时动态创�?
        log.info("发送全体通知成功: title={}", title);
    }

    @Override
    public PageResult<NotificationVO> getUserNotifications(Long userId, Integer page, Integer pageSize, Boolean onlyUnread) {
        // 先查询总数
        LambdaQueryWrapper<UserNotification> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserNotification::getUserId, userId);
        if (onlyUnread != null && onlyUnread) {
            countWrapper.eq(UserNotification::getIsRead, 0);
        }
        Long total = userNotificationMapper.selectCount(countWrapper);
        
        // 查询分页数据
        Page<UserNotification> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId);
        if (onlyUnread != null && onlyUnread) {
            wrapper.eq(UserNotification::getIsRead, 0);
        }
        wrapper.orderByDesc(UserNotification::getCreateTime);
        
        userNotificationMapper.selectPage(pageInfo, wrapper);
        
        // 批量查询系统通知
        List<Long> notificationIds = pageInfo.getRecords().stream()
                .map(UserNotification::getNotificationId)
                .collect(Collectors.toList());
        
        if (notificationIds.isEmpty()) {
            return new PageResult<>(total, new ArrayList<>());
        }
        
        List<SystemNotification> notifications = systemNotificationMapper.selectBatchIds(notificationIds);
        Map<Long, SystemNotification> notificationMap = notifications.stream()
                .collect(Collectors.toMap(SystemNotification::getId, n -> n));
        
        // 转换为VO
        List<NotificationVO> voList = pageInfo.getRecords().stream()
                .map(un -> {
                    SystemNotification sn = notificationMap.get(un.getNotificationId());
                    if (sn == null) {
                        return null;
                    }
                    
                    return NotificationVO.builder()
                            .id(un.getId())
                            .title(sn.getTitle())
                            .content(sn.getContent())
                            .type(sn.getType())
                            .typeName(getTypeName(sn.getType()))
                            .bizType(sn.getBizType())
                            .bizId(sn.getBizId())
                            .isRead(un.getIsRead() == 1)
                            .readTime(un.getReadTime())
                            .sendTime(sn.getSendTime())
                            .createTime(un.getCreateTime())
                            .build();
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
        
        return new PageResult<>(total, voList);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getId, notificationId);
        
        UserNotification userNotification = userNotificationMapper.selectOne(wrapper);
        if (userNotification != null && userNotification.getIsRead() == 0) {
            userNotification.setIsRead(1);
            userNotification.setReadTime(LocalDateTime.now());
            userNotificationMapper.updateById(userNotification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getIsRead, 0);
        
        List<UserNotification> notifications = userNotificationMapper.selectList(wrapper);
        for (UserNotification notification : notifications) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            userNotificationMapper.updateById(notification);
        }
        
        log.info("标记所有通知为已�? userId={}, count={}", userId, notifications.size());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return userNotificationMapper.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getId, notificationId);
        
        userNotificationMapper.delete(wrapper);
        log.info("删除通知: userId={}, notificationId={}", userId, notificationId);
    }
    
    @Override
    @Transactional
    public void batchDeleteNotifications(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .in(UserNotification::getId, notificationIds);
        
        int count = userNotificationMapper.delete(wrapper);
        log.info("批量删除通知: userId={}, count={}", userId, count);
    }
    
    @Override
    @Transactional
    public void batchMarkAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .in(UserNotification::getId, notificationIds)
               .eq(UserNotification::getIsRead, 0);
        
        List<UserNotification> notifications = userNotificationMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        
        for (UserNotification notification : notifications) {
            notification.setIsRead(1);
            notification.setReadTime(now);
            userNotificationMapper.updateById(notification);
        }
        
        log.info("批量标记通知为已�? userId={}, count={}", userId, notifications.size());
    }
    
    @Override
    public NotificationVO getNotificationDetail(Long userId, Long notificationId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getId, notificationId);
        
        UserNotification userNotification = userNotificationMapper.selectOne(wrapper);
        if (userNotification == null) {
            return null;
        }
        
        SystemNotification systemNotification = systemNotificationMapper.selectById(userNotification.getNotificationId());
        if (systemNotification == null) {
            return null;
        }
        
        return NotificationVO.builder()
                .id(userNotification.getId())
                .title(systemNotification.getTitle())
                .content(systemNotification.getContent())
                .type(systemNotification.getType())
                .typeName(getTypeName(systemNotification.getType()))
                .bizType(systemNotification.getBizType())
                .bizId(systemNotification.getBizId())
                .isRead(userNotification.getIsRead() == 1)
                .readTime(userNotification.getReadTime())
                .sendTime(systemNotification.getSendTime())
                .createTime(userNotification.getCreateTime())
                .build();
    }
    
    @Override
    public PageResult<NotificationVO> searchNotifications(Long userId, String keyword, Integer page, Integer pageSize) {
        // 先查询用户的所有通知ID
        LambdaQueryWrapper<UserNotification> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserNotification::getUserId, userId);
        List<UserNotification> userNotifications = userNotificationMapper.selectList(userWrapper);
        
        if (userNotifications.isEmpty()) {
            return new PageResult<>(0L, new ArrayList<>());
        }
        
        List<Long> notificationIds = userNotifications.stream()
                .map(UserNotification::getNotificationId)
                .collect(Collectors.toList());
        
        // 在系统通知中搜�?
        LambdaQueryWrapper<SystemNotification> sysWrapper = new LambdaQueryWrapper<>();
        sysWrapper.in(SystemNotification::getId, notificationIds)
                  .and(w -> w.like(SystemNotification::getTitle, keyword)
                           .or()
                           .like(SystemNotification::getContent, keyword));
        
        List<SystemNotification> systemNotifications = systemNotificationMapper.selectList(sysWrapper);
        
        if (systemNotifications.isEmpty()) {
            return new PageResult<>(0L, new ArrayList<>());
        }
        
        // 创建通知ID到用户通知的映�?
        Map<Long, UserNotification> userNotificationMap = userNotifications.stream()
                .collect(Collectors.toMap(UserNotification::getNotificationId, un -> un));
        
        // 转换为VO并分�?
        List<NotificationVO> allVoList = systemNotifications.stream()
                .filter(sn -> userNotificationMap.containsKey(sn.getId()))
                .map(sn -> {
                    UserNotification un = userNotificationMap.get(sn.getId());
                    return NotificationVO.builder()
                            .id(un.getId())
                            .title(sn.getTitle())
                            .content(sn.getContent())
                            .type(sn.getType())
                            .typeName(getTypeName(sn.getType()))
                            .bizType(sn.getBizType())
                            .bizId(sn.getBizId())
                            .isRead(un.getIsRead() == 1)
                            .readTime(un.getReadTime())
                            .sendTime(sn.getSendTime())
                            .createTime(un.getCreateTime())
                            .build();
                })
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
        
        // 手动分页
        long total = allVoList.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allVoList.size());
        
        List<NotificationVO> pagedList = start < allVoList.size() ? 
                allVoList.subList(start, end) : new ArrayList<>();
        
        return new PageResult<>(total, pagedList);
    }
    
    @Override
    public Map<String, Object> getNotificationStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总通知�?
        LambdaQueryWrapper<UserNotification> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(UserNotification::getUserId, userId);
        Long totalCount = userNotificationMapper.selectCount(allWrapper);
        
        // 未读�?
        Long unreadCount = getUnreadCount(userId);
        
        // 已读�?
        Long readCount = totalCount - unreadCount;
        
        // 按类型统�?
        List<UserNotification> allNotifications = userNotificationMapper.selectList(allWrapper);
        List<Long> notificationIds = allNotifications.stream()
                .map(UserNotification::getNotificationId)
                .collect(Collectors.toList());
        
        Map<String, Long> typeCount = new HashMap<>();
        if (!notificationIds.isEmpty()) {
            List<SystemNotification> systemNotifications = systemNotificationMapper.selectBatchIds(notificationIds);
            typeCount = systemNotifications.stream()
                    .collect(Collectors.groupingBy(
                            sn -> getTypeName(sn.getType()),
                            Collectors.counting()
                    ));
        }
        
        // 今日新增
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<UserNotification> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(UserNotification::getUserId, userId)
                    .ge(UserNotification::getCreateTime, todayStart);
        Long todayCount = userNotificationMapper.selectCount(todayWrapper);
        
        statistics.put("totalCount", totalCount);
        statistics.put("unreadCount", unreadCount);
        statistics.put("readCount", readCount);
        statistics.put("typeCount", typeCount);
        statistics.put("todayCount", todayCount);
        
        return statistics;
    }
    
    @Override
    @Transactional
    public void clearReadNotifications(Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getIsRead, 1);
        
        int count = userNotificationMapper.delete(wrapper);
        log.info("清空已读通知: userId={}, count={}", userId, count);
    }
    
    /**
     * 推送通知给用户（WebSocket�?
     */
    private void pushNotificationToUser(Long userId, SystemNotification notification, UserNotification userNotification) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("id", userNotification.getId());
            wsMessage.put("title", notification.getTitle());
            wsMessage.put("content", notification.getContent());
            wsMessage.put("type", notification.getType());
            wsMessage.put("typeName", getTypeName(notification.getType()));
            wsMessage.put("bizType", notification.getBizType());
            wsMessage.put("bizId", notification.getBizId());
            wsMessage.put("sendTime", notification.getSendTime());
            wsMessage.put("createTime", userNotification.getCreateTime());
            wsMessage.put("isRead", false);
            
            webSocketHandler.pushNotification(userId, wsMessage);
        } catch (Exception e) {
            log.error("推送通知失败: userId={}", userId, e);
        }
    }
    
    private String getTypeName(Integer type) {
        switch (type) {
            case 1: return "预约成功";
            case 2: return "预约提醒";
            case 3: return "优惠券到期";
            case 4: return "系统公告";
            case 5: return "退款成功";
            case 6: return "退款拒绝";
            case 7: return "预约取消";
            case 8: return "预约确认";
            default: return "其他";
        }
    }
}
