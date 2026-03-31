package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.KnowledgeBase;

import java.util.List;
import java.util.Map;

/**
 * 知识库服务接口
 * 负责知识条目的检索（RAG的R部分）
 */
public interface KnowledgeBaseService {

    /**
     * 根据用户问题检索最相关的知识条目
     */
    List<KnowledgeBase> search(String query, int topK);

    /**
     * 根据页面上下文推断分类，检索对应知识
     */
    List<KnowledgeBase> searchByPage(String page, int topK);

    /**
     * 将检索结果拼装为Prompt文本片段
     */
    String buildContextBlock(List<KnowledgeBase> items);

    /**
     * 重新加载知识库缓存
     */
    void reloadCache();

    /**
     * 新增知识条目
     */
    void add(KnowledgeBase item);

    /**
     * 更新知识条目
     */
    void update(KnowledgeBase item);

    /**
     * 删除知识条目
     */
    void delete(Long id);

    /**
     * 查询所有启用的知识条目
     */
    List<KnowledgeBase> listAll();

    /**
     * 管理端分页查询知识条目
     */
    PageResult<KnowledgeBase> pageQuery(Integer page, Integer pageSize, String category, String keyword, Integer status);

    /**
     * 获取FAQ列表
     */
    List<Map<String, Object>> getFaqList(Integer limit);

    /**
     * 记录知识命中日志并更新命中统计
     */
    void recordHitLog(String sessionId, Long userId, String query, String page, List<KnowledgeBase> items);

    /**
     * 获取知识库统计数据
     */
    Map<String, Object> getStatistics();
}
