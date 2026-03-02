package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端通知实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_notification")
public class AdminNotification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 通知类型：1预约成功，2预约提醒，3优惠券到期，4系统公告
     */
    private Integer type;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private Long bizId;
    
    /**
     * 目标类型：1全体用户，2指定用户
     */
    private Integer targetType;
    
    /**
     * 目标用户ID列表（逗号分隔）
     */
    private String targetUsers;
    
    /**
     * 门店ID（可为空表示系统级通知）
     */
    private Long storeId;

    /**
     * 优先级：1低 2中 3高
     */
    private Integer priority;

    /**
     * 是否已读：0未读 1已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 状态：1待发送，2已发送，3已取消
     */
    private Integer status;
    
    /**
     * 逻辑删除：1删除，0未删除
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
