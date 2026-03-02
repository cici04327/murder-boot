package com.murder.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 留言反馈VO
 */
@Data
public class FeedbackVO {
    
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 留言人姓名
     */
    private String name;
    
    /**
     * 联系方式
     */
    private String contact;
    
    /**
     * 主题类型
     */
    private String subject;
    
    /**
     * 主题名称
     */
    private String subjectName;
    
    /**
     * 留言内容
     */
    private String message;
    
    /**
     * 状态：0待处理，1处理中，2已回复，3已关闭
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 管理员回复内容
     */
    private String replyContent;
    
    /**
     * 回复时间
     */
    private LocalDateTime replyTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
