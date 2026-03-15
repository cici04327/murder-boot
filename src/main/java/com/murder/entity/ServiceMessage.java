package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_message")
public class ServiceMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long sessionId;

    /** 发送方：user / admin / system */
    private String senderType;

    /** 发送方ID（system消息为null） */
    private Long senderId;

    /** 消息内容 */
    private String content;

    /** 消息类型：text / image / system */
    private String msgType;

    /** 是否已读：0未读，1已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
