package com.murder.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文章DTO
 */
@Data
public class ArticleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID（编辑时需要）
     */
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章内容（富文本）
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 文章分类：1-新手攻略 2-选本技巧 3-榜单推荐 4-行业动态 5-玩家心得
     */
    private Integer category;

    /**
     * 是否置顶：0-否 1-是
     */
    private Integer isTop;

    /**
     * 是否推荐：0-否 1-是
     */
    private Integer isRecommended;

    /**
     * 状态：0-草稿 1-已发布 2-已下架
     */
    private Integer status;
}
