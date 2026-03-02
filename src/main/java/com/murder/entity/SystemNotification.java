package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_notification")
public class SystemNotification implements Serializable {

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
     * 通知类型�?预约成功�?预约提醒�?优惠券到期，4系统公告�?退款成功，6退款拒绝，7预约取消�?预约确认
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
     * 目标类型�?全体用户�?指定用户
     */
    private Integer targetType;
    
    /**
     * 目标用户ID列表（逗号分隔�?
     */
    private String targetUsers;
    
    /**
     * 发送时�?
     */
    private LocalDateTime sendTime;
    
    /**
     * 状态：1待发送，2已发送，3已取�?
     */
    private Integer status;
    
    /**
     * 逻辑删除�?删除�?未删�?
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
