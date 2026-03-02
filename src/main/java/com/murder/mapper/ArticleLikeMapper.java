package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞Mapper
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
}
