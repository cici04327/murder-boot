package com.murder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 推荐结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 剧本ID
     */
    private Long id;

    /**
     * 剧本名称
     */
    private String name;

    /**
     * 封面图
     */
    private String cover;

    /**
     * 剧本简介
     */
    private String description;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 难度
     */
    private Integer difficulty;

    /**
     * 玩家人数
     */
    private Integer playerCount;

    /**
     * 游戏时长（小时）
     */
    private BigDecimal duration;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 评分
     */
    private BigDecimal rating;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 推荐理由
     */
    private String recommendReason;

    /**
     * 推荐分数
     */
    private BigDecimal recommendScore;

    /**
     * 推荐类型：1协同过滤，2内容推荐，3热门推荐，4基于历史
     */
    private Integer recommendationType;

    /**
     * 是否收藏
     */
    private Boolean isFavorite;

    /**
     * 是否独家
     */
    private Integer isExclusive;
}
