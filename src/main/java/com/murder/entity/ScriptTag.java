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
 * 剧本标签实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_tag")
public class ScriptTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 剧本ID
     */
    private Long scriptId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签类型：1风格，2难度，3主题，4特色
     */
    @Builder.Default
    private Integer tagType = 1;

    /**
     * 权重
     */
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

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
}
