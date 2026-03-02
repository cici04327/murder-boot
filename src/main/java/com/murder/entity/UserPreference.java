package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户偏好实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_preference")
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 偏好类型：category_恐怖, difficulty_2, tag_推理
     */
    private String preferenceType;

    /**
     * 偏好值
     */
    private String preferenceValue;

    /**
     * 偏好分数（越高越喜欢）
     */
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    /**
     * 偏好次数
     */
    @Builder.Default
    private Integer count = 1;

    /**
     * 逻辑删除
     */
    @TableLogic
    @Builder.Default
    private Integer isDeleted = 0;

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
