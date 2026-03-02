package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章评论Mapper
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    /**
     * 增加评论点赞�?
     */
    @Update("UPDATE article_comment SET like_count = like_count + 1 WHERE id = #{id}")
    void increaseLikeCount(@Param("id") Long id);

    /**
     * 减少评论点赞�?
     */
    @Update("UPDATE article_comment SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    void decreaseLikeCount(@Param("id") Long id);
}
