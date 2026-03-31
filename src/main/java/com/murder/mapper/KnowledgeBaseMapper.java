package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 查询所有启用的知识条目（用于内存检索）
     */
    @Select("SELECT * FROM ai_knowledge_base WHERE status = 1 AND is_deleted = 0 ORDER BY priority DESC")
    List<KnowledgeBase> selectAllActive();

    /**
     * 按分类查询
     */
    @Select("SELECT * FROM ai_knowledge_base WHERE category = #{category} AND status = 1 AND is_deleted = 0 ORDER BY priority DESC")
    List<KnowledgeBase> selectByCategory(String category);
}
