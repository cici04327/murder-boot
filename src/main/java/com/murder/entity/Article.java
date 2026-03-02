package com.murder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体�?
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
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
     * 文章内容（富文本�?
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 文章分类�?-新手攻略 2-选本技巧?3-榜单推荐 4-行业动�?5-玩家心得
     */
    private Integer category;

    /**
     * 分类名称（冗余字段）
     */
    private String categoryName;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 作者名称（冗余字段�?
     */
    private String authorName;

    /**
     * 浏览次数
     */
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 点赞次数
     */
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 评论�?
     */
    @Builder.Default
    private Integer commentCount = 0;

    /**
     * 收藏�?
     */
    @Builder.Default
    private Integer favoriteCount = 0;

    /**
     * 是否置顶�?-�?1-�?
     */
    @Builder.Default
    private Integer isTop = 0;

    /**
     * 是否推荐�?-�?1-�?
     */
    @Builder.Default
    private Integer isRecommended = 0;

    /**
     * 状态：0-草稿 1-已发�?2-已下�?
     */
    @Builder.Default
    private Integer status = 0;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

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

    /**
     * 创建�?
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 修改�?
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 是否删除�?-�?1-�?
     */
    @TableLogic
    @Builder.Default
    private Integer isDeleted = 0;
}
