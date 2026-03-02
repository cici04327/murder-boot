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
 * 推荐记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recommendation_log")
public class RecommendationLog implements Serializable {

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
     * 剧本ID
     */
    private Long scriptId;

    /**
     * 推荐类型：1协同过滤，2内容推荐，3热门推荐，4基于历史
     */
    private Integer recommendationType;

    /**
     * 推荐分数
     */
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    /**
     * 是否点击：1是，0否
     */
    @Builder.Default
    private Integer isClicked = 0;

    /**
     * 点击时间
     */
    private LocalDateTime clickTime;

    /**
     * 是否预约：1是，0否
     */
    @Builder.Default
    private Integer isReserved = 0;

    /**
     * 预约时间
     */
    private LocalDateTime reserveTime;

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
