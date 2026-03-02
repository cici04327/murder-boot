package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ScriptTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 剧本标签Mapper
 */
@Mapper
public interface ScriptTagMapper extends BaseMapper<ScriptTag> {

    /**
     * 获取剧本的所有标签
     */
    @Select("SELECT tag_name FROM script_tag " +
            "WHERE script_id = #{scriptId} AND is_deleted = 0 " +
            "ORDER BY weight DESC")
    List<String> getScriptTags(@Param("scriptId") Long scriptId);

    /**
     * 根据标签获取相似剧本ID列表
     */
    @Select("SELECT DISTINCT script_id, SUM(weight) as total_weight " +
            "FROM script_tag " +
            "WHERE tag_name IN " +
            "<foreach collection='tags' item='tag' open='(' separator=',' close=')'>" +
            "#{tag}" +
            "</foreach> " +
            "AND script_id != #{excludeScriptId} AND is_deleted = 0 " +
            "GROUP BY script_id " +
            "ORDER BY total_weight DESC " +
            "LIMIT #{limit}")
    List<Long> getSimilarScriptIdsByTags(@Param("tags") List<String> tags, 
                                          @Param("excludeScriptId") Long excludeScriptId, 
                                          @Param("limit") Integer limit);

    /**
     * 批量插入标签
     */
    int batchInsert(@Param("tags") List<ScriptTag> tags);
}
