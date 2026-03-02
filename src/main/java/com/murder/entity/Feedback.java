package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户留言/反馈实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedback")
public class Feedback implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID（可为空，游客也可留言）
     */
    private Long userId;
    
    /**
     * 留言人姓名
     */
    private String name;
    
    /**
     * 联系方式（手机号或邮箱）
     */
    private String contact;
    
    /**
     * 主题类型：platform-平台使用问题, booking-预约相关, account-账号相关, 
     *          feedback-建议与反馈, business-商务合作, other-其他
     */
    private String subject;
    
    /**
     * 留言内容
     */
    private String message;
    
    /**
     * 状态：0待处理，1处理中，2已回复，3已关闭
     */
    private Integer status;
    
    /**
     * 管理员回复内容
     */
    private String replyContent;
    
    /**
     * 回复时间
     */
    private LocalDateTime replyTime;
    
    /**
     * 回复管理员ID
     */
    private Long replyUserId;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
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
