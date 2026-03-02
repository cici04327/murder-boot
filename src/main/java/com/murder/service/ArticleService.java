package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.ArticleDTO;
import com.murder.entity.Article;
import com.murder.vo.ArticleVO;

import java.util.List;

/**
 * 文章服务
 */
public interface ArticleService {

    /**
     * 分页查询文章列表
     * @param sortBy 排序方式：time-最新发�? view-最多浏�? like-最多点�? comment-最多评�?
     */
    PageResult<ArticleVO> pageQuery(Integer page, Integer pageSize, Integer category, String keyword, Integer status, String sortBy);

    /**
     * 根据ID查询文章详情
     */
    ArticleVO getById(Long id);

    /**
     * 获取热门文章
     */
    List<ArticleVO> getHotArticles(Integer limit);

    /**
     * 获取推荐文章
     */
    List<ArticleVO> getRecommendedArticles(Integer limit);

    /**
     * 新增文章
     */
    void add(ArticleDTO articleDTO);

    /**
     * 更新文章
     */
    void update(ArticleDTO articleDTO);

    /**
     * 删除文章
     */
    void delete(Long id);

    /**
     * 增加浏览次数
     */
    void increaseViewCount(Long id);

    /**
     * 点赞文章
     */
    void likeArticle(Long id);

    /**
     * 取消点赞文章
     */
    void unlikeArticle(Long id);

    /**
     * 检查用户是否已点赞文章
     */
    boolean isLiked(Long articleId, Long userId);
}
