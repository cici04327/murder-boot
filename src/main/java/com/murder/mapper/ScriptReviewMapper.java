package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ScriptReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

/**
 * 剧本评价Mapper
 */
@Mapper
public interface ScriptReviewMapper extends BaseMapper<ScriptReview> {
    
    /**
     * 计算剧本平均评分
     */
    @Select("SELECT AVG(rating) FROM script_review WHERE script_id = #{scriptId} AND is_deleted = 0")
    BigDecimal calculateAverageRating(Long scriptId);
    
    /**
     * 统计剧本评价数量
     */
    @Select("SELECT COUNT(*) FROM script_review WHERE script_id = #{scriptId} AND is_deleted = 0")
    Integer countByScriptId(Long scriptId);
}
