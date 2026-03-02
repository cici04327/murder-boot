package com.murder.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 留言反馈DTO
 */
@Data
public class FeedbackDTO {
    
    /**
     * 留言人姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;
    
    /**
     * 联系方式
     */
    @NotBlank(message = "联系方式不能为空")
    private String contact;
    
    /**
     * 主题类型
     */
    @NotBlank(message = "请选择主题")
    private String subject;
    
    /**
     * 留言内容
     */
    @NotBlank(message = "留言内容不能为空")
    private String message;
}
