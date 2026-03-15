package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.ScriptRole;

import java.util.List;

/**
 * 剧本服务接口
 */
public interface ScriptService {

    /**
     * 分页查询剧本列表
     */
    PageResult<Script> pageQuery(Integer page, Integer pageSize, String name, Long categoryId, String difficulty, Integer playerCount, String sortBy, Integer type);
    
    /**
     * 获取热门剧本
     */
    List<Script> getHotScripts();
    
    /**
     * 获取推荐剧本
     */
    List<Script> getRecommendedScripts();

    /**
     * 根据ID查询剧本详情
     */
    Script getById(Long id);

    /**
     * 新增剧本
     */
    void add(Script script);

    /**
     * 更新剧本
     */
    void update(Script script);

    /**
     * 删除剧本
     */
    void delete(Long id);

    /**
     * 查询剧本分类列表
     */
    List<ScriptCategory> getCategories();

    /**
     * 新增剧本分类
     */
    void addCategory(ScriptCategory category);

    /**
     * 更新剧本分类
     */
    void updateCategory(Long id, ScriptCategory category);

    /**
     * 删除剧本分类
     */
    void deleteCategory(Long id);
    
    /**
     * 根据剧本ID获取角色列表
     */
    List<ScriptRole> getRolesByScriptId(Long scriptId);
}