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
 * 热门榜单缓存实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hot_ranking")
public class HotRanking implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 榜单类型：1今日热门，2本周热门，3本月热门，4口碑榜
     */
    private Integer rankingType;

    /**
     * 剧本ID
     */
    private Long scriptId;

    /**
     * 排名（rank 是 MySQL 8.0 保留字，必须用反引号转义）
     */
    @TableField("`rank`")
    private Integer rank;

    /**
     * 热度分数
     */
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    /**
     * 浏览量
     */
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 预约量
     */
    @Builder.Default
    private Integer reserveCount = 0;

    /**
     * 收藏量
     */
    @Builder.Default
    private Integer favoriteCount = 0;

    /**
     * 评分
     */
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
