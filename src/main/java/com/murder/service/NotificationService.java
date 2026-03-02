package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.vo.NotificationVO;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    /**
     * 发送通知给指定用�?
     * @param title 标题
     * @param content 内容
     * @param type 通知类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param userIds 用户ID列表
     */
    void sendToUsers(String title, String content, Integer type, String bizType, Long bizId, Long... userIds);
    
    /**
     * 发送通知给所有用�?
     */
    void sendToAll(String title, String content, Integer type, String bizType, Long bizId);
    
    /**
     * 分页查询用户通知列表
     */
    PageResult<NotificationVO> getUserNotifications(Long userId, Integer page, Integer pageSize, Boolean onlyUnread);
    
    /**
     * 标记通知为已�?
     */
    void markAsRead(Long userId, Long notificationId);
    
    /**
     * 标记所有通知为已�?
     */
    void markAllAsRead(Long userId);
    
    /**
     * 获取未读通知数量
     */
    Long getUnreadCount(Long userId);
    
    /**
     * 删除通知
     */
    void deleteNotification(Long userId, Long notificationId);
    
    /**
     * 批量删除通知
     */
    void batchDeleteNotifications(Long userId, List<Long> notificationIds);
    
    /**
     * 批量标记通知为已�?
     */
    void batchMarkAsRead(Long userId, List<Long> notificationIds);
    
    /**
     * 获取通知详情
     */
    NotificationVO getNotificationDetail(Long userId, Long notificationId);
    
    /**
     * 搜索通知
     */
    PageResult<NotificationVO> searchNotifications(Long userId, String keyword, Integer page, Integer pageSize);
    
    /**
     * 获取通知统计信息
     */
    Map<String, Object> getNotificationStatistics(Long userId);
    
    /**
     * 清空已读通知
     */
    void clearReadNotifications(Long userId);
}
