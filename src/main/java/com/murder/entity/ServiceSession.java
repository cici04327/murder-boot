package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_session")
public class ServiceSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称（冗余，避免联查） */
    private String userName;

    /** 接待管理员ID（分配后填入） */
    private Long adminId;

    /** 会话状态：0-等待接入，1-进行中，2-已结束，3-超时关闭 */
    private Integer status;

    /** 用户发起时的问题描述（AI转人工时传入最近一条问题） */
    private String initialQuestion;

    /** 会话结束时间 */
    private LocalDateTime endTime;

    /** 用户评价：1-5 */
    private Integer rating;

    /** 用户评价内容 */
    private String ratingComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
