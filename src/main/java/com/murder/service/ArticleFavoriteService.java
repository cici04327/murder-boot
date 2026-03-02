package com.murder.service;

/**
 * 文章收藏服务接口
 */
public interface ArticleFavoriteService {

    /**
     * 收藏文章
     */
    void favoriteArticle(Long articleId);

    /**
     * 取消收藏
     */
    void unfavoriteArticle(Long articleId);

    /**
     * 检查用户是否已收藏文章
     */
    boolean isFavorited(Long articleId, Long userId);
}
