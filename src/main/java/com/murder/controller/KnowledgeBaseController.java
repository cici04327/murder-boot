package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.entity.KnowledgeBase;
import com.murder.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI知识库管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@Tag(name = "AI知识库管理")
@Slf4j
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 查询所有知识条目
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有知识条目")
    public Result<List<KnowledgeBase>> list() {
        return Result.success(knowledgeBaseService.listAll());
    }

    /**
     * 新增知识条目
     */
    @PostMapping
    @Operation(summary = "新增知识条目")
    public Result<String> add(@RequestBody KnowledgeBase item) {
        log.info("新增知识条目: category={}, title={}", item.getCategory(), item.getTitle());
        knowledgeBaseService.add(item);
        return Result.success("新增成功");
    }

    /**
     * 更新知识条目
     */
    @PutMapping
    @Operation(summary = "更新知识条目")
    public Result<String> update(@RequestBody KnowledgeBase item) {
        log.info("更新知识条目: id={}", item.getId());
        knowledgeBaseService.update(item);
        return Result.success("更新成功");
    }

    /**
     * 删除知识条目
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识条目")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除知识条目: id={}", id);
        knowledgeBaseService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 手动刷新知识库缓存
     */
    @PostMapping("/reload")
    @Operation(summary = "重新加载知识库缓存")
    public Result<String> reload() {
        log.info("手动重新加载知识库缓存");
        knowledgeBaseService.reloadCache();
        return Result.success("知识库缓存已刷新");
    }

    /**
     * 测试RAG检索
     */
    @GetMapping("/search")
    @Operation(summary = "测试RAG检索（输入问题，返回检索到的知识条目）")
    public Result<List<KnowledgeBase>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        log.info("测试RAG检索: query={}, topK={}", query, topK);
        return Result.success(knowledgeBaseService.search(query, topK));
    }
}
