package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.AdminNotification;
import com.murder.vo.AdminNotificationVO;
import com.murder.mapper.AdminNotificationMapper;
import com.murder.service.AdminNotificationService;
import com.murder.websocket.AdminNotificationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端通知服务实现�?
 */
@Slf4j
@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    @Autowired
    private AdminNotificationMapper adminNotificationMapper;

    @Autowired
    private AdminNotificationWebSocketHandler adminWebSocketHandler;

    @Override
    @Transactional
    public void sendNotification(String title, String content, Integer type, String bizType, Long bizId, Long storeId, Integer priority) {
        AdminNotification notification = AdminNotification.builder()
                .title(title)
                .content(content)
                .type(type)
                .bizType(bizType)
                .bizId(bizId)
                .storeId(storeId)
                .priority(priority)
                .isRead(0)
                .targetType(1) // 1=全部管理员
                .status(2) // 2=已发送
                .sendTime(LocalDateTime.now())
                .isDeleted(0)
                .build();
        
        adminNotificationMapper.insert(notification);
        log.info("发送管理端通知成功: title={}, type={}", title, type);

        // WebSocket 实时推送给在线管理端
        try {
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
            adminWebSocketHandler.pushNotificationToAll(wsMessage);
        } catch (Exception e) {
            log.error("管理端WebSocket推送失败", e);
        }
    }

    @Override
    public PageResult<AdminNotificationVO> getNotifications(Integer page, Integer pageSize, Long storeId, Integer type, Boolean onlyUnread) {
        // 构建查询条件
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();

        if (storeId != null) {
            wrapper.eq(AdminNotification::getStoreId, storeId);
        }

        if (type != null) {
            wrapper.eq(AdminNotification::getType, type);
        }

        if (onlyUnread != null) {
            wrapper.eq(AdminNotification::getIsRead, onlyUnread ? 0 : 1);
        }

        // 2 表示已发送
        wrapper.eq(AdminNotification::getStatus, 2);
        
        wrapper.orderByDesc(AdminNotification::getCreateTime);
        
        // 查询总数
        Long total = adminNotificationMapper.selectCount(wrapper);
        
        // 分页查询
        Page<AdminNotification> pageInfo = new Page<>(page, pageSize);
        adminNotificationMapper.selectPage(pageInfo, wrapper);
        
        // 转换为VO
        List<AdminNotificationVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(total, voList);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        AdminNotification notification = adminNotificationMapper.selectById(notificationId);
        if (notification == null) {
            return;
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return;
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        adminNotificationMapper.updateById(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long storeId) {
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(AdminNotification::getStoreId, storeId);
        }
        wrapper.eq(AdminNotification::getIsRead, 0);

        List<AdminNotification> list = adminNotificationMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        for (AdminNotification n : list) {
            n.setIsRead(1);
            n.setReadTime(now);
            adminNotificationMapper.updateById(n);
        }
    }

    @Override
    public Long getUnreadCount(Long storeId) {
        // 使用 MyBatis-Plus 条件统计，避免因手写 SQL 与字段/方言不兼容导致偶发 500。
        // 同时做兜底：如果按门店统计失败，则退回到全局未读数，避免影响管理端使用。
        try {
            LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdminNotification::getIsRead, 0);
            wrapper.eq(AdminNotification::getIsDeleted, 0);
            if (storeId != null) {
                wrapper.eq(AdminNotification::getStoreId, storeId);
            }
            return adminNotificationMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("统计管理端未读通知失败: storeId={}, fallback to global count", storeId, e);
            // fallback：不按门店过滤，避免一直 500
            LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdminNotification::getIsRead, 0);
            wrapper.eq(AdminNotification::getIsDeleted, 0);
            return adminNotificationMapper.selectCount(wrapper);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        adminNotificationMapper.deleteById(notificationId);
        log.info("删除管理端通知: notificationId={}", notificationId);
    }

    @Override
    @Transactional
    public void batchDeleteNotifications(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        
        int count = adminNotificationMapper.deleteBatchIds(notificationIds);
        log.info("批量删除管理端通知: count={}", count);
    }

    @Override
    public AdminNotificationVO getNotificationDetail(Long notificationId) {
        AdminNotification notification = adminNotificationMapper.selectById(notificationId);
        if (notification == null) {
            return null;
        }
        
        return convertToVO(notification);
    }

    @Override
    public Map<String, Object> getStatistics(Long storeId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总通知数
        LambdaQueryWrapper<AdminNotification> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(AdminNotification::getStatus, 2); // 只统计已发送的
        Long totalCount = adminNotificationMapper.selectCount(allWrapper);
        
        // 未读数 (system_notification 没有此字段)
        Long unreadCount = 0L;
        
        // 已读数
        Long readCount = totalCount;
        
        // 按类型统计
        List<AdminNotification> allNotifications = adminNotificationMapper.selectList(allWrapper);
        Map<String, Long> typeCount = allNotifications.stream()
                .collect(Collectors.groupingBy(
                        n -> getTypeName(n.getType()),
                        Collectors.counting()
                ));
        
        // 今日新增
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<AdminNotification> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(AdminNotification::getStatus, 2);
        todayWrapper.ge(AdminNotification::getCreateTime, todayStart);
        Long todayCount = adminNotificationMapper.selectCount(todayWrapper);
        
        statistics.put("totalCount", totalCount);
        statistics.put("unreadCount", unreadCount);
        statistics.put("readCount", readCount);
        statistics.put("typeCount", typeCount);
        statistics.put("todayCount", todayCount);
        
        return statistics;
    }
    
    /**
     * 转换为VO
     */
    private AdminNotificationVO convertToVO(AdminNotification notification) {
        AdminNotificationVO vo = new AdminNotificationVO();
        BeanUtils.copyProperties(notification, vo);
        vo.setIsRead(false); // system_notification 没有已读状态,默认为未读
        vo.setTypeName(getTypeName(notification.getType()));
        
        return vo;
    }
    
    /**
     * 获取通知类型名称
     */
    private String getTypeName(Integer type) {
        switch (type) {
            case 1: return "预约成功";
            case 2: return "预约提醒";
            case 3: return "优惠券到期";
            case 4: return "系统公告";
            default: return "其他";
        }
    }
}
