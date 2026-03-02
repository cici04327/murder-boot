package com.murder.service;

import com.murder.dto.ArticleCommentDTO;
import com.murder.vo.ArticleCommentVO;

import java.util.List;

/**
 * 文章评论服务接口
 */
public interface ArticleCommentService {

    /**
     * 获取文章评论列表
     */
    List<ArticleCommentVO> getCommentsByArticleId(Long articleId);

    /**
     * 添加评论
     */
    void addComment(ArticleCommentDTO commentDTO);

    /**
     * 删除评论
     */
    void deleteComment(Long id);

    /**
     * 点赞评论
     */
    void likeComment(Long commentId);

    /**
     * 取消点赞评论
     */
    void unlikeComment(Long commentId);
}
