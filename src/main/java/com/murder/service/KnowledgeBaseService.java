package com.murder.service;

import com.murder.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 * 负责知识条目的检索（RAG的R部分）
 */
public interface KnowledgeBaseService {

    /**
     * 根据用户问题检索最相关的知识条目
     * @param query 用户问题
     * @param topK  返回前K条
     * @return 相关知识列表
     */
    List<KnowledgeBase> search(String query, int topK);

    /**
     * 根据页面上下文推断分类，检索对应知识
     * @param page 当前页面路径
     * @param topK 返回前K条
     * @return 相关知识列表
     */
    List<KnowledgeBase> searchByPage(String page, int topK);

    /**
     * 将检索结果拼装为Prompt文本片段
     * @param items 检索到的知识条目
     * @return 可注入Prompt的文本
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
}
