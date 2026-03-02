package com.murder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浏览历史VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 浏览记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标类型：1剧本，2门店
     */
    private Integer targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标名称（剧本名称或门店名称）
     */
    private String name;

    /**
     * 封面图片
     */
    private String cover;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 玩家人数（剧本）
     */
    private String playerCount;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 评分
     */
    private BigDecimal rating;

    /**
     * 门店地址（门店类型时使用）
     */
    private String address;

    /**
     * 浏览时间
     */
    private LocalDateTime browseTime;

    /**
     * 浏览时间（格式化字符串）
     */
    private String browseTimeStr;

    /**
     * 浏览时长（秒）
     */
    private Integer duration;
}
