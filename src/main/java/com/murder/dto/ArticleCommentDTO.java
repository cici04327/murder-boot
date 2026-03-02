package com.murder.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文章评论DTO
 */
@Data
public class ArticleCommentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID（用于回复）
     */
    private Long parentId;

    /**
     * 回复目标用户ID
     */
    private Long replyToUserId;
}
