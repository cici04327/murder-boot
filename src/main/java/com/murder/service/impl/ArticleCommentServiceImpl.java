package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.context.BaseContext;
import com.murder.dto.ArticleCommentDTO;
import com.murder.entity.Article;
import com.murder.entity.ArticleComment;
import com.murder.entity.ArticleCommentLike;
import com.murder.entity.User;
import com.murder.vo.ArticleCommentVO;
import com.murder.mapper.ArticleCommentLikeMapper;
import com.murder.mapper.ArticleCommentMapper;
import com.murder.mapper.ArticleMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.ArticleCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章评论服务实现
 */
@Service
@Slf4j
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper commentMapper;

    @Autowired
    private ArticleCommentLikeMapper commentLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<ArticleCommentVO> getCommentsByArticleId(Long articleId) {
        log.info("获取文章评论列表: articleId={}", articleId);

        // 查询所有评?
        LambdaQueryWrapper<ArticleComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleComment::getArticleId, articleId)
                .eq(ArticleComment::getStatus, 1)
                .orderByDesc(ArticleComment::getCreateTime);

        List<ArticleComment> comments = commentMapper.selectList(queryWrapper);

        // 获取当前用户ID
        Long currentUserId = BaseContext.getCurrentId();

        // 查询当前用户点赞的评论ID列表
        List<Long> likedCommentIds = new ArrayList<>();
        if (currentUserId != null && !comments.isEmpty()) {
            List<Long> commentIds = comments.stream()
                    .map(ArticleComment::getId)
                    .collect(Collectors.toList());
            
            LambdaQueryWrapper<ArticleCommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(ArticleCommentLike::getUserId, currentUserId)
                    .in(ArticleCommentLike::getCommentId, commentIds);
            List<ArticleCommentLike> likes = commentLikeMapper.selectList(likeWrapper);
            likedCommentIds = likes.stream()
                    .map(ArticleCommentLike::getCommentId)
                    .collect(Collectors.toList());
        }

        // 分离顶级评论和回?
        List<ArticleComment> topComments = new ArrayList<>();
        Map<Long, List<ArticleComment>> repliesMap = comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(ArticleComment::getParentId));

        for (ArticleComment comment : comments) {
            if (comment.getParentId() == null) {
                topComments.add(comment);
            }
        }

        // 转换为VO
        List<Long> finalLikedCommentIds = likedCommentIds;
        return topComments.stream()
                .map(comment -> {
                    ArticleCommentVO vo = convertToVO(comment, finalLikedCommentIds.contains(comment.getId()));
                    // 添加回复
                    List<ArticleComment> replies = repliesMap.get(comment.getId());
                    if (replies != null && !replies.isEmpty()) {
                        vo.setReplies(replies.stream()
                                .map(reply -> convertToVO(reply, finalLikedCommentIds.contains(reply.getId())))
                                .collect(Collectors.toList()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addComment(ArticleCommentDTO commentDTO) {
        log.info("添加评论: {}", commentDTO);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登?");
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);

        // 构建评论对象
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(commentDTO.getArticleId());
        comment.setUserId(userId);
        comment.setUserName(user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "匿名用户");
        comment.setUserAvatar(user != null ? user.getAvatar() : null);
        comment.setContent(commentDTO.getContent());
        comment.setParentId(commentDTO.getParentId());
        comment.setLikeCount(0);
        comment.setStatus(1);

        // 如果是回复评论，获取被回复用户信?
        if (commentDTO.getReplyToUserId() != null) {
            User replyToUser = userMapper.selectById(commentDTO.getReplyToUserId());
            comment.setReplyToUserId(commentDTO.getReplyToUserId());
            comment.setReplyToUserName(replyToUser != null ? (replyToUser.getNickname() != null ? replyToUser.getNickname() : replyToUser.getUsername()) : "匿名用户");
        }

        commentMapper.insert(comment);

        // 更新文章评论?
        articleMapper.increaseCommentCount(commentDTO.getArticleId());
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        log.info("删除评论: id={}", id);

        ArticleComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 软删?
        comment.setStatus(0);
        commentMapper.updateById(comment);

        // 更新文章评论?
        articleMapper.decreaseCommentCount(comment.getArticleId());
    }

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        log.info("点赞评论: commentId={}", commentId);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<ArticleCommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleCommentLike::getCommentId, commentId)
                .eq(ArticleCommentLike::getUserId, userId);
        
        ArticleCommentLike existingLike = commentLikeMapper.selectOne(queryWrapper);
        if (existingLike != null) {
            throw new RuntimeException("已经点赞过了");
        }

        // 添加点赞记录
        ArticleCommentLike like = new ArticleCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeMapper.insert(like);

        // 增加点赞?
        commentMapper.increaseLikeCount(commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        log.info("取消点赞评论: commentId={}", commentId);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 删除点赞记录
        LambdaQueryWrapper<ArticleCommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleCommentLike::getCommentId, commentId)
                .eq(ArticleCommentLike::getUserId, userId);
        
        commentLikeMapper.delete(queryWrapper);

        // 减少点赞?
        commentMapper.decreaseLikeCount(commentId);
    }

    private ArticleCommentVO convertToVO(ArticleComment comment, boolean userLiked) {
        ArticleCommentVO vo = new ArticleCommentVO();
        BeanUtils.copyProperties(comment, vo);
        vo.setUserLiked(userLiked);
        vo.setReplies(new ArrayList<>());
        return vo;
    }
}
