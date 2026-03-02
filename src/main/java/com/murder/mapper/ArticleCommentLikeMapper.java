package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ArticleCommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞Mapper
 */
@Mapper
public interface ArticleCommentLikeMapper extends BaseMapper<ArticleCommentLike> {
}
