package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.ScriptRole;
import com.murder.service.ScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 剧本控制�?
 */
@RestController
@RequestMapping("/api/script")
@Tag(name = "剧本接口")
@Slf4j
public class ScriptController {

    @Autowired
    private ScriptService scriptService;

    /**
     * 分页查询剧本列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询剧本列表")
    public Result<PageResult<Script>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer playerCount,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer type) {
        log.info("分页查询剧本列表: page={}, pageSize={}, keyword={}, categoryId={}, difficulty={}, playerCount={}, sortBy={}", 
                page, pageSize, keyword, categoryId, difficulty, playerCount, sortBy);
        
        // keyword 优先级高�?name
        String searchName = keyword != null ? keyword : name;
        
        PageResult<Script> pageResult = scriptService.pageQuery(page, pageSize, searchName, categoryId, difficulty, playerCount, sortBy, type);
        return Result.success(pageResult);
    }
    
    /**
     * 根据ID查询剧本详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询剧本详情")
    public Result<Script> getById(@PathVariable Long id) {
        log.info("查询剧本详情: {}", id);
        Script script = scriptService.getById(id);
        return Result.success(script);
    }
    
    /**
     * 获取热门剧本
     */
    @GetMapping("/list/hot")
    @Operation(summary = "获取热门剧本")
    public Result<List<Script>> getHotScripts() {
        log.info("获取热门剧本");
        List<Script> hotScripts = scriptService.getHotScripts();
        return Result.success(hotScripts);
    }
    
    /**
     * 获取推荐剧本
     */
    @GetMapping("/list/recommended")
    @Operation(summary = "获取推荐剧本")
    public Result<List<Script>> getRecommendedScripts() {
        log.info("获取推荐剧本");
        List<Script> recommendedScripts = scriptService.getRecommendedScripts();
        return Result.success(recommendedScripts);
    }

    /**
     * 新增剧本
     */
    @PostMapping
    @Operation(summary = "新增剧本")
    public Result<String> add(@RequestBody Script script) {
        log.info("新增剧本: {}", script);
        scriptService.add(script);
        return Result.success("新增成功");
    }

    /**
     * 更新剧本
     */
    @PutMapping
    @Operation(summary = "更新剧本")
    public Result<String> update(@RequestBody Script script) {
        log.info("更新剧本: {}", script);
        scriptService.update(script);
        return Result.success("更新成功");
    }

    /**
     * 删除剧本
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除剧本")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除剧本: {}", id);
        scriptService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询剧本分类列表
     */
    @GetMapping("/category")
    @Operation(summary = "查询剧本分类列表")
    public Result<List<ScriptCategory>> getCategories() {
        log.info("查询剧本分类列表");
        List<ScriptCategory> categories = scriptService.getCategories();
        return Result.success(categories);
    }
    
    /**
     * 获取剧本角色列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "获取剧本角色列表")
    public Result<List<ScriptRole>> getRoles(@PathVariable Long id) {
        log.info("获取剧本角色列表: scriptId={}", id);
        List<ScriptRole> roles = scriptService.getRolesByScriptId(id);
        return Result.success(roles);
    }
}