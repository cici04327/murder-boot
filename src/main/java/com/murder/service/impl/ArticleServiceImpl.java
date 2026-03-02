package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ArticleDTO;
import com.murder.entity.Article;
import com.murder.vo.ArticleVO;
import com.murder.mapper.ArticleMapper;
import com.murder.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 */
@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    private static final Map<Integer, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put(1, "新手攻略");
        CATEGORY_MAP.put(2, "选本技巧");
        CATEGORY_MAP.put(3, "榜单推荐");
        CATEGORY_MAP.put(4, "行业动态");
        CATEGORY_MAP.put(5, "玩家心得");
    }

    @Override
    public PageResult<ArticleVO> pageQuery(Integer page, Integer pageSize, Integer category, String keyword, Integer status, String sortBy) {
        log.info("分页查询文章: page={}, pageSize={}, category={}, keyword={}, status={}, sortBy={}", 
                page, pageSize, category, keyword, status, sortBy);

        Page<Article> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        if (status == null) {
            queryWrapper.eq(Article::getStatus, 1);
        } else {
            queryWrapper.eq(Article::getStatus, status);
        }

        if (category != null && category > 0) {
            queryWrapper.eq(Article::getCategory, category);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Article::getTitle, keyword)
                    .or()
                    .like(Article::getSummary, keyword)
            );
        }

        // 置顶文章始终在最前面
        queryWrapper.orderByDesc(Article::getIsTop);

        // 根据sortBy参数排序
        if ("view".equals(sortBy)) {
            // 最多浏�?
            queryWrapper.orderByDesc(Article::getViewCount);
        } else if ("like".equals(sortBy)) {
            // 最多点�?
            queryWrapper.orderByDesc(Article::getLikeCount);
        } else if ("comment".equals(sortBy)) {
            // 最多评�?
            queryWrapper.orderByDesc(Article::getCommentCount);
        } else {
            // 默认：最新发�?
            queryWrapper.orderByDesc(Article::getPublishTime);
        }

        Page<Article> result = articleMapper.selectPage(pageInfo, queryWrapper);

        List<ArticleVO> articleVOList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), articleVOList);
    }

    @Override
    public ArticleVO getById(Long id) {
        log.info("查询文章详情: id={}", id);
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return null;
        }
        return convertToVO(article);
    }

    @Override
    public List<ArticleVO> getHotArticles(Integer limit) {
        log.info("获取热门文章: limit={}", limit);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<Article> articles = articleMapper.selectList(queryWrapper);
        return articles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArticleVO> getRecommendedArticles(Integer limit) {
        log.info("获取推荐文章: limit={}", limit);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1)
                .eq(Article::getIsRecommended, 1)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<Article> articles = articleMapper.selectList(queryWrapper);
        return articles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void add(ArticleDTO articleDTO) {
        log.info("新增文章: {}", articleDTO);

        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);

        article.setCategoryName(CATEGORY_MAP.get(articleDTO.getCategory()));
        article.setViewCount(0);
        article.setLikeCount(0);

        if (articleDTO.getStatus() != null && articleDTO.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }

        articleMapper.insert(article);
    }

    @Override
    public void update(ArticleDTO articleDTO) {
        log.info("更新文章: {}", articleDTO);

        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);

        article.setCategoryName(CATEGORY_MAP.get(articleDTO.getCategory()));

        Article oldArticle = articleMapper.selectById(articleDTO.getId());
        if (oldArticle != null && oldArticle.getStatus() == 0 && articleDTO.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }

        articleMapper.updateById(article);
    }

    @Override
    public void delete(Long id) {
        log.info("删除文章: id={}", id);
        articleMapper.deleteById(id);
    }

    @Override
    public void increaseViewCount(Long id) {
        log.info("增加浏览次数: id={}", id);
        articleMapper.increaseViewCount(id);
    }

    @Override
    public void likeArticle(Long id) {
        log.info("点赞文章: id={}", id);
        articleMapper.increaseLikeCount(id);
    }

    @Override
    public void unlikeArticle(Long id) {
        log.info("取消点赞文章: id={}", id);
        articleMapper.decreaseLikeCount(id);
    }

    @Override
    public boolean isLiked(Long articleId, Long userId) {
        // TODO: 实现点赞状态检�?
        return false;
    }

    private ArticleVO convertToVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        vo.setCategoryName(CATEGORY_MAP.get(article.getCategory()));
        return vo;
    }
}
