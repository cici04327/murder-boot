package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_conversation_log")
public class AiConversationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（匿名则为null） */
    private Long userId;

    /** 会话ID（前端生成的sessionId） */
    private String sessionId;

    /** 用户提问内容 */
    private String question;

    /** AI回复内容 */
    private String answer;

    /** 用户当前页面（context.page） */
    private String page;

    /** 是否触发转人工：0否，1是 */
    private Integer isTransferred;

    /** AI提供商（deepseek/openai/openrouter/mock等） */
    private String provider;

    /** 模型名称 */
    private String model;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
