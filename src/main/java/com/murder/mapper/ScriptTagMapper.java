package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ScriptTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 剧本标签Mapper
 */
@Mapper
public interface ScriptTagMapper extends BaseMapper<ScriptTag> {

    /**
     * 批量获取多个剧本的标签（返回 scriptId -> tagNames 的 Map）
     */
    @Select("<script>" +
            "SELECT script_id, tag_name FROM script_tag " +
            "WHERE script_id IN " +
            "<foreach collection='scriptIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND is_deleted = 0 ORDER BY weight DESC" +
            "</script>")
    List<Map<String, Object>> batchGetScriptTagsRaw(@Param("scriptIds") List<Long> scriptIds);

    default Map<Long, List<String>> batchGetScriptTags(List<Long> scriptIds) {
        if (scriptIds == null || scriptIds.isEmpty()) return java.util.Collections.emptyMap();
        List<Map<String, Object>> rows = batchGetScriptTagsRaw(scriptIds);
        Map<Long, List<String>> result = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            Long scriptId = ((Number) row.get("script_id")).longValue();
            String tagName = (String) row.get("tag_name");
            result.computeIfAbsent(scriptId, k -> new java.util.ArrayList<>()).add(tagName);
        }
        return result;
    }

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
