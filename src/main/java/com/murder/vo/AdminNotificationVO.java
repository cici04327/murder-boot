package com.murder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端通知VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationVO implements Serializable {
    
    private Long id;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 门店名称
     */
    private String storeName;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 通知类型：1新预约，2退款申请，3用户评价，4系统通知
     */
    private Integer type;
    
    /**
     * 通知类型名称
     */
    private String typeName;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private Long bizId;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 优先级：1低，2中，3高
     */
    private Integer priority;
    
    /**
     * 优先级名称
     */
    private String priorityName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
