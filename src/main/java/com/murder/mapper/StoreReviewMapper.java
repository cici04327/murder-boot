package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.StoreReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 门店评价Mapper
 */
@Mapper
public interface StoreReviewMapper extends BaseMapper<StoreReview> {
    
    /**
     * 统计门店评价�?
     */
    @Select("SELECT COUNT(*) FROM store_review WHERE store_id = #{storeId} AND is_deleted = 0")
    Long countByStoreId(Long storeId);
    
    /**
     * 计算门店平均评分
     */
    @Select("SELECT AVG(rating) FROM store_review WHERE store_id = #{storeId} AND is_deleted = 0")
    Double getAverageRating(Long storeId);
    
    /**
     * 统计好评数量（评分>=4为好评）
     */
    @Select("SELECT COUNT(*) FROM store_review WHERE rating >= 4 AND is_deleted = 0")
    Long countGoodReviews();
}
