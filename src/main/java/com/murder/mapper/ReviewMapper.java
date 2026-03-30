package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

/**
 * 评价Mapper
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    
    /**
     * 计算剧本平均评分（基于已通过的Review评价）
     */
    @Select("SELECT AVG(script_rating) FROM review WHERE script_id = #{scriptId} AND status = 2 AND is_deleted = 0 AND script_rating IS NOT NULL")
    BigDecimal calculateAvgScriptRating(Long scriptId);

    /**
     * 计算门店平均评分（基于已通过的Review评价）
     */
    @Select("SELECT AVG(store_rating) FROM review WHERE store_id = #{storeId} AND status = 2 AND is_deleted = 0 AND store_rating IS NOT NULL")
    BigDecimal calculateAvgStoreRating(Long storeId);

    /**
     * 计算DM平均评分（基于已通过的Review评价）
     */
    @Select("SELECT AVG(dm_rating) FROM review WHERE dm_id = #{dmId} AND status = 2 AND is_deleted = 0 AND dm_rating IS NOT NULL")
    BigDecimal calculateAvgDmRatingByDmId(Long dmId);

    /**
     * 统计DM已通过审核的评价数量
     */
    @Select("SELECT COUNT(*) FROM review WHERE dm_id = #{dmId} AND status = 2 AND is_deleted = 0 AND dm_rating IS NOT NULL")
    Integer countDmReviews(Long dmId);
}
