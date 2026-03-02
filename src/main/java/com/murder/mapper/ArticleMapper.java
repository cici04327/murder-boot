package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 增加浏览次数
     */
    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    void increaseViewCount(@Param("id") Long id);

    /**
     * 增加点赞次数
     */
    @Update("UPDATE article SET like_count = like_count + 1 WHERE id = #{id}")
    void increaseLikeCount(@Param("id") Long id);

    /**
     * 减少点赞次数
     */
    @Update("UPDATE article SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    void decreaseLikeCount(@Param("id") Long id);

    /**
     * 增加评论�?
     */
    @Update("UPDATE article SET comment_count = comment_count + 1 WHERE id = #{id}")
    void increaseCommentCount(@Param("id") Long id);

    /**
     * 减少评论�?
     */
    @Update("UPDATE article SET comment_count = comment_count - 1 WHERE id = #{id} AND comment_count > 0")
    void decreaseCommentCount(@Param("id") Long id);

    /**
     * 增加收藏�?
     */
    @Update("UPDATE article SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    void increaseFavoriteCount(@Param("id") Long id);

    /**
     * 减少收藏�?
     */
    @Update("UPDATE article SET favorite_count = favorite_count - 1 WHERE id = #{id} AND favorite_count > 0")
    void decreaseFavoriteCount(@Param("id") Long id);
}
