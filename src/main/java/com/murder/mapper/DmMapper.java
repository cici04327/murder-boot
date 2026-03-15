package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.Dm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * DM Mapper
 */
@Mapper
public interface DmMapper extends BaseMapper<Dm> {

    /**
     * 更新 DM 评分和场次（由评价触发）
     */
    @Update("UPDATE dm SET rating = #{rating}, game_count = #{gameCount} WHERE id = #{dmId}")
    void updateRatingAndGameCount(@Param("dmId") Long dmId,
                                  @Param("rating") java.math.BigDecimal rating,
                                  @Param("gameCount") Integer gameCount);
}
