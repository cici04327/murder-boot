package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.HotRanking;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 热门榜单Mapper
 */
@Mapper
public interface HotRankingMapper extends BaseMapper<HotRanking> {

    /**
     * 获取指定类型的热门榜单
     */
    @Select("SELECT hr.*, s.name as script_name, s.cover, s.rating, s.price " +
            "FROM hot_ranking hr " +
            "INNER JOIN script s ON hr.script_id = s.id " +
            "WHERE hr.ranking_type = #{rankingType} AND s.status = 1 AND s.is_deleted = 0 " +
            "ORDER BY hr.rank ASC " +
            "LIMIT #{limit}")
    List<HotRanking> getHotRankingList(@Param("rankingType") Integer rankingType, 
                                        @Param("limit") Integer limit);

    /**
     * 清空指定类型的榜单
     */
    @Delete("DELETE FROM hot_ranking WHERE ranking_type = #{rankingType}")
    int clearRankingByType(@Param("rankingType") Integer rankingType);
}
