package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.context.BaseContext;
import com.murder.entity.ArticleFavorite;
import com.murder.mapper.ArticleFavoriteMapper;
import com.murder.mapper.ArticleMapper;
import com.murder.service.ArticleFavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章收藏服务实现
 */
@Service
@Slf4j
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    @Autowired
    private ArticleFavoriteMapper favoriteMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    @Transactional
    public void favoriteArticle(Long articleId) {
        log.info("收藏文章: articleId={}", articleId);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登?");
        }

        // 检查是否已收藏
        if (isFavorited(articleId, userId)) {
            throw new RuntimeException("已经收藏过了");
        }

        // 添加收藏记录
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setArticleId(articleId);
        favorite.setUserId(userId);
        favoriteMapper.insert(favorite);

        // 更新文章收藏?
        articleMapper.increaseFavoriteCount(articleId);
    }

    @Override
    @Transactional
    public void unfavoriteArticle(Long articleId) {
        log.info("取消收藏文章: articleId={}", articleId);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 删除收藏记录
        LambdaQueryWrapper<ArticleFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleFavorite::getArticleId, articleId)
                .eq(ArticleFavorite::getUserId, userId);
        
        favoriteMapper.delete(queryWrapper);

        // 更新文章收藏?
        articleMapper.decreaseFavoriteCount(articleId);
    }

    @Override
    public boolean isFavorited(Long articleId, Long userId) {
        LambdaQueryWrapper<ArticleFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleFavorite::getArticleId, articleId)
                .eq(ArticleFavorite::getUserId, userId);
        
        return favoriteMapper.selectCount(queryWrapper) > 0;
    }
}
