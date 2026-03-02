package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ArticleDTO;
import com.murder.dto.ArticleCommentDTO;
import com.murder.vo.ArticleVO;
import com.murder.vo.ArticleCommentVO;
import com.murder.service.ArticleService;
import com.murder.service.ArticleCommentService;
import com.murder.service.ArticleFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章控制�?
 */
@RestController
@RequestMapping("/api/article")
@Tag(name = "文章接口")
@Slf4j
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleCommentService commentService;

    @Autowired
    private ArticleFavoriteService favoriteService;

    /**
     * 分页查询文章列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询文章列表")
    public Result<PageResult<ArticleVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortBy) {
        log.info("分页查询文章列表: page={}, pageSize={}, category={}, keyword={}, status={}, sortBy={}", 
                page, pageSize, category, keyword, status, sortBy);
        PageResult<ArticleVO> pageResult = articleService.pageQuery(page, pageSize, category, keyword, status, sortBy);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询文章详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询文章详情")
    public Result<ArticleVO> getById(@PathVariable Long id) {
        log.info("查询文章详情: id={}", id);
        articleService.increaseViewCount(id);
        ArticleVO articleVO = articleService.getById(id);
        return Result.success(articleVO);
    }

    /**
     * 获取热门文章
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门文章")
    public Result<List<ArticleVO>> getHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取热门文章: limit={}", limit);
        List<ArticleVO> articles = articleService.getHotArticles(limit);
        return Result.success(articles);
    }

    /**
     * 获取推荐文章
     */
    @GetMapping("/recommended")
    @Operation(summary = "获取推荐文章")
    public Result<List<ArticleVO>> getRecommendedArticles(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取推荐文章: limit={}", limit);
        List<ArticleVO> articles = articleService.getRecommendedArticles(limit);
        return Result.success(articles);
    }

    /**
     * 新增文章
     */
    @PostMapping
    @Operation(summary = "新增文章")
    public Result<String> add(@RequestBody ArticleDTO articleDTO) {
        log.info("新增文章: {}", articleDTO);
        articleService.add(articleDTO);
        return Result.success("新增成功");
    }

    /**
     * 更新文章
     */
    @PutMapping
    @Operation(summary = "更新文章")
    public Result<String> update(@RequestBody ArticleDTO articleDTO) {
        log.info("更新文章: {}", articleDTO);
        articleService.update(articleDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除文章: id={}", id);
        articleService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 点赞文章
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "点赞文章")
    public Result<String> like(@PathVariable Long id) {
        log.info("点赞文章: id={}", id);
        articleService.likeArticle(id);
        return Result.success("点赞成功");
    }

    /**
     * 取消点赞文章
     */
    @DeleteMapping("/{id}/like")
    @Operation(summary = "取消点赞文章")
    public Result<String> unlike(@PathVariable Long id) {
        log.info("取消点赞文章: id={}", id);
        articleService.unlikeArticle(id);
        return Result.success("取消点赞成功");
    }

    /**
     * 收藏文章
     */
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏文章")
    public Result<String> favorite(@PathVariable Long id) {
        log.info("收藏文章: id={}", id);
        favoriteService.favoriteArticle(id);
        return Result.success("收藏成功");
    }

    /**
     * 取消收藏文章
     */
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏文章")
    public Result<String> unfavorite(@PathVariable Long id) {
        log.info("取消收藏文章: id={}", id);
        favoriteService.unfavoriteArticle(id);
        return Result.success("取消收藏成功");
    }

    /**
     * 检查是否已收藏文章
     */
    @GetMapping("/{id}/favorite/status")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavoriteStatus(@PathVariable Long id) {
        log.info("检查收藏状�? id={}", id);
        Long userId = com.murder.common.context.BaseContext.getCurrentId();
        if (userId == null) {
            return Result.success(false);
        }
        boolean isFavorited = favoriteService.isFavorited(id, userId);
        return Result.success(isFavorited);
    }

    /**
     * 获取文章评论列表
     */
    @GetMapping("/{id}/comments")
    @Operation(summary = "获取文章评论列表")
    public Result<List<ArticleCommentVO>> getComments(@PathVariable Long id) {
        log.info("获取文章评论列表: id={}", id);
        List<ArticleCommentVO> comments = commentService.getCommentsByArticleId(id);
        return Result.success(comments);
    }

    /**
     * 添加评论
     */
    @PostMapping("/{id}/comments")
    @Operation(summary = "添加评论")
    public Result<String> addComment(@PathVariable Long id, @RequestBody ArticleCommentDTO commentDTO) {
        log.info("添加评论: articleId={}, comment={}", id, commentDTO);
        commentDTO.setArticleId(id);
        commentService.addComment(commentDTO);
        return Result.success("评论成功");
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除评论")
    public Result<String> deleteComment(@PathVariable Long commentId) {
        log.info("删除评论: commentId={}", commentId);
        commentService.deleteComment(commentId);
        return Result.success("删除成功");
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "点赞评论")
    public Result<String> likeComment(@PathVariable Long commentId) {
        log.info("点赞评论: commentId={}", commentId);
        commentService.likeComment(commentId);
        return Result.success("点赞成功");
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "取消点赞评论")
    public Result<String> unlikeComment(@PathVariable Long commentId) {
        log.info("取消点赞评论: commentId={}", commentId);
        commentService.unlikeComment(commentId);
        return Result.success("取消点赞成功");
    }
}
