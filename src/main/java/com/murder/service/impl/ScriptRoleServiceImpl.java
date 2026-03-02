package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.ScriptRole;
import com.murder.mapper.ScriptRoleMapper;
import com.murder.service.ScriptRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 剧本角色服务实现�?
 */
@Service
@Slf4j
public class ScriptRoleServiceImpl implements ScriptRoleService {

    @Autowired
    private ScriptRoleMapper scriptRoleMapper;

    /**
     * 根据剧本ID查询角色列表
     */
    @Override
    public List<ScriptRole> listByScriptId(Long scriptId) {
        log.info("查询剧本角色列表: scriptId={}", scriptId);
        LambdaQueryWrapper<ScriptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptRole::getScriptId, scriptId);
        wrapper.orderByAsc(ScriptRole::getSort);
        return scriptRoleMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询角色详情
     */
    @Override
    public ScriptRole getById(Long id) {
        log.info("查询角色详情: id={}", id);
        return scriptRoleMapper.selectById(id);
    }

    /**
     * 新增角色
     */
    @Override
    public void add(ScriptRole scriptRole) {
        log.info("新增角色: {}", scriptRole);
        scriptRoleMapper.insert(scriptRole);
    }

    /**
     * 更新角色
     */
    @Override
    public void update(ScriptRole scriptRole) {
        log.info("更新角色: {}", scriptRole);
        scriptRoleMapper.updateById(scriptRole);
    }

    /**
     * 删除角色
     */
    @Override
    public void delete(Long id) {
        log.info("删除角色: id={}", id);
        scriptRoleMapper.deleteById(id);
    }

    /**
     * 批量新增角色
     */
    @Override
    public void addBatch(List<ScriptRole> roles) {
        log.info("批量新增角色，数�? {}", roles.size());
        for (ScriptRole role : roles) {
            scriptRoleMapper.insert(role);
        }
    }
}
