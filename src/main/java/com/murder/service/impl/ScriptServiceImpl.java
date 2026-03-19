package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.ScriptRole;
import com.murder.mapper.ScriptCategoryMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptRoleMapper;
import com.murder.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 剧本服务实现?
 */
@Service
@Slf4j
public class ScriptServiceImpl implements ScriptService {

    @Autowired
    private ScriptMapper scriptMapper;

    @Autowired
    private ScriptCategoryMapper scriptCategoryMapper;
    
    @Autowired
    private ScriptRoleMapper scriptRoleMapper;

    /**
     * 分页查询剧本列表
     */
    @Override
    public PageResult<Script> pageQuery(Integer page, Integer pageSize, String name, Long categoryId, String difficulty, Integer playerCount, String sortBy, Integer type) {
        log.info("📄 分页查询剧本 - 页码: {}, 每页大小: {}, 关键? {}, 分类ID: {}, 难度: {}, 人数: {}, 排序: {}", 
                page, pageSize, name, categoryId, difficulty, playerCount, sortBy);
        
        Page<Script> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜?
        if (StringUtils.hasText(name)) {
            wrapper.like(Script::getName, name);
        }
        
        // 分类筛?
        if (categoryId != null) {
            wrapper.eq(Script::getCategoryId, categoryId);
        }
        
        // 难度筛?
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(Script::getDifficulty, difficulty);
        }
        
        // 人数筛?
        if (playerCount != null) {
            if (playerCount >= 8) {
                wrapper.ge(Script::getPlayerCount, 8);
            } else {
                wrapper.eq(Script::getPlayerCount, playerCount);
            }
        }
        
        if (type != null) {
            wrapper.eq(Script::getType, type);
        }
        
        wrapper.eq(Script::getStatus, 1);
        
        // 先查询总数
        Long total = scriptMapper.selectCount(wrapper);
        log.info("📊 查询到符合条件的剧本总数: {}", total);
        
        // 排序
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "hot":
                    // 按热度排序（按评?创建时间?
                    wrapper.orderByDesc(Script::getRating);
                    wrapper.orderByDesc(Script::getCreateTime);
                    break;
                case "rating":
                    // 按评分排?
                    wrapper.orderByDesc(Script::getRating);
                    wrapper.orderByDesc(Script::getCreateTime);
                    break;
                case "newest":
                    // 按最新排?
                    wrapper.orderByDesc(Script::getCreateTime);
                    break;
                case "price":
                    // 按价格排?
                    wrapper.orderByAsc(Script::getPrice);
                    wrapper.orderByDesc(Script::getCreateTime);
                    break;
                default:
                    wrapper.orderByDesc(Script::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(Script::getCreateTime);
        }
        
        // 执行分页查询
        Page<Script> result = scriptMapper.selectPage(pageInfo, wrapper);
        
        log.info("?分页查询完成 - 当前? {}, 每页大小: {}, 实际返回: {} ? 总记录数: {}, 总页? {}", 
                result.getCurrent(), result.getSize(), result.getRecords().size(), result.getTotal(), result.getPages());
        
        List<Script> records = result.getRecords();
        
        // ⚠️ 如果分页插件未生效，手动进行分页处理
        if (records.size() > pageSize) {
            log.warn("⚠️ 检测到分页插件未生效，手动进行分页处理");
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, records.size());
            records = records.subList(start, end);
            log.info("手动分页后：返回 {} 条记录", records.size());
        }
        
        // 返回分页结果
        return new PageResult<>(total, records);
    }
    
    /**
     * 获取热门剧本（去掉 @Cacheable 避免 Redis 序列化问题导致 500）
     */
    @Override
    public List<Script> getHotScripts() {
        log.info("获取热门剧本...");
        try {
            LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Script::getStatus, 1);
            wrapper.orderByDesc(Script::getRating);
            wrapper.orderByDesc(Script::getCreateTime);
            wrapper.last("LIMIT 10");
            List<Script> scripts = scriptMapper.selectList(wrapper);
            log.info("获取热门剧本成功，共{}条", scripts.size());
            return scripts;
        } catch (Exception e) {
            log.error("获取热门剧本失败", e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 获取推荐剧本（去掉 @Cacheable 避免 Redis 序列化问题导致 500）
     */
    @Override
    public List<Script> getRecommendedScripts() {
        log.info("获取推荐剧本...");
        try {
            LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Script::getStatus, 1);
            wrapper.orderByDesc(Script::getRating);
            wrapper.orderByDesc(Script::getCreateTime);
            wrapper.last("LIMIT 10");
            List<Script> scripts = scriptMapper.selectList(wrapper);
            log.info("获取推荐剧本成功，共{}条", scripts.size());
            return scripts;
        } catch (Exception e) {
            log.error("获取推荐剧本失败", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 根据ID查询剧本详情
     * 详情数据经常被后台直接调整价格/文案，去掉缓存避免用户端读到旧值
     */
    @Override
    public Script getById(Long id) {
        Script script = scriptMapper.selectById(id);
        if (script != null && script.getCategoryId() != null) {
            // 获取分类名称
            ScriptCategory category = scriptCategoryMapper.selectById(script.getCategoryId());
            if (category != null) {
                // 注意：Script实体没有categoryName字段，这里只能通过VO或者Map来扩?
                // 暂时返回原始数据，前端需要单独调用分类接口或使用VO
            }
        }
        return script;
    }

    /**
     * 新增剧本
     */
    @Override
    public void add(Script script) {
        scriptMapper.insert(script);
    }

    /**
     * 更新剧本（清除缓存）
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"script:detail", "hot:scripts"}, key = "#script.id", allEntries = false)
    public void update(Script script) {
        scriptMapper.updateById(script);
    }

    /**
     * 删除剧本（清除缓存）
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"script:detail", "hot:scripts", "recommended:scripts"}, allEntries = true)
    public void delete(Long id) {
        scriptMapper.deleteById(id);
    }

    /**
     * 查询剧本分类列表（添加缓存）
     */
    @Override
    @org.springframework.cache.annotation.Cacheable(value = "script:categories", unless = "#result == null || #result.isEmpty()")
    public List<ScriptCategory> getCategories() {
        LambdaQueryWrapper<ScriptCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptCategory::getStatus, 1);
        wrapper.orderByAsc(ScriptCategory::getSort);
        return scriptCategoryMapper.selectList(wrapper);
    }

    /**
     * 新增剧本分类
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "script:categories", allEntries = true)
    public void addCategory(ScriptCategory category) {
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        scriptCategoryMapper.insert(category);
    }

    /**
     * 更新剧本分类
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "script:categories", allEntries = true)
    public void updateCategory(Long id, ScriptCategory category) {
        category.setId(id);
        scriptCategoryMapper.updateById(category);
    }

    /**
     * 删除剧本分类
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "script:categories", allEntries = true)
    public void deleteCategory(Long id) {
        scriptCategoryMapper.deleteById(id);
    }
    
    /**
     * 根据剧本ID获取角色列表
     */
    @Override
    public List<ScriptRole> getRolesByScriptId(Long scriptId) {
        LambdaQueryWrapper<ScriptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptRole::getScriptId, scriptId);
        wrapper.orderByAsc(ScriptRole::getSort);
        return scriptRoleMapper.selectList(wrapper);
    }
}
