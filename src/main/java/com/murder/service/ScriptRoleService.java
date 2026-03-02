package com.murder.service;

import com.murder.entity.ScriptRole;

import java.util.List;

/**
 * 剧本角色服务接口
 */
public interface ScriptRoleService {

    /**
     * 根据剧本ID查询角色列表
     */
    List<ScriptRole> listByScriptId(Long scriptId);

    /**
     * 根据ID查询角色详情
     */
    ScriptRole getById(Long id);

    /**
     * 新增角色
     */
    void add(ScriptRole scriptRole);

    /**
     * 更新角色
     */
    void update(ScriptRole scriptRole);

    /**
     * 删除角色
     */
    void delete(Long id);

    /**
     * 批量新增角色
     */
    void addBatch(List<ScriptRole> roles);
}
