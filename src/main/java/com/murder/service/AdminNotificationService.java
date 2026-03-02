package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.vo.AdminNotificationVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端通知服务接口
 */
public interface AdminNotificationService {
    
    /**
     * 发送管理端通知
     * @param title 标题
     * @param content 内容
     * @param type 通知类型�?新预约，2退款申请，3用户评价�?系统通知
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param storeId 门店ID（可选）
     * @param priority 优先级：1低，2中，3�?
     */
    void sendNotification(String title, String content, Integer type, String bizType, Long bizId, Long storeId, Integer priority);
    
    /**
     * 分页查询管理端通知列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param storeId 门店ID（可选，null表示查询所有）
     * @param type 通知类型（可选）
     * @param onlyUnread 是否只查询未�?
     */
    PageResult<AdminNotificationVO> getNotifications(Integer page, Integer pageSize, Long storeId, Integer type, Boolean onlyUnread);
    
    /**
     * 标记通知为已�?
     */
    void markAsRead(Long notificationId);
    
    /**
     * 标记所有通知为已�?
     */
    void markAllAsRead(Long storeId);
    
    /**
     * 获取未读通知数量
     */
    Long getUnreadCount(Long storeId);
    
    /**
     * 删除通知
     */
    void deleteNotification(Long notificationId);
    
    /**
     * 批量删除通知
     */
    void batchDeleteNotifications(List<Long> notificationIds);
    
    /**
     * 获取通知详情
     */
    AdminNotificationVO getNotificationDetail(Long notificationId);
    
    /**
     * 获取通知统计信息
     */
    Map<String, Object> getStatistics(Long storeId);
}
