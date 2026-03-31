package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.KnowledgeBase;
import com.murder.mapper.KnowledgeBaseMapper;
import com.murder.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 使用关键词TF-IDF打分做轻量级语义检索，无需引入向量数据库
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    // 内存缓存：启动时加载所有知识条目
    private List<KnowledgeBase> knowledgeCache = Collections.synchronizedList(new ArrayList<>());

    // 页面 → 知识分类的映射
    private static final Map<String, List<String>> PAGE_CATEGORY_MAP = new LinkedHashMap<>();

    static {
        PAGE_CATEGORY_MAP.put("/vip",          Arrays.asList("vip", "coupon"));
        PAGE_CATEGORY_MAP.put("/reservation",  Arrays.asList("reservation", "refund", "payment"));
        PAGE_CATEGORY_MAP.put("/payment",      Arrays.asList("payment", "refund"));
        PAGE_CATEGORY_MAP.put("/script",       Arrays.asList("reservation", "group"));
        PAGE_CATEGORY_MAP.put("/user/points",  Arrays.asList("points", "coupon"));
        PAGE_CATEGORY_MAP.put("/user/coupons", Arrays.asList("coupon", "vip"));
        PAGE_CATEGORY_MAP.put("/group",        Arrays.asList("group", "reservation"));
        PAGE_CATEGORY_MAP.put("/store",        Arrays.asList("store", "dm"));
        PAGE_CATEGORY_MAP.put("/user/reservations", Arrays.asList("reservation", "refund", "payment"));
    }

    /**
     * 启动时加载知识库缓存
     */
    @PostConstruct
    public void init() {
        try {
            reloadCache();
            log.info("知识库缓存初始化完成，共加载 {} 条知识", knowledgeCache.size());
        } catch (Exception e) {
            log.warn("知识库缓存初始化失败（可能是表不存在，等待初始化）: {}", e.getMessage());
        }
    }

    @Override
    public void reloadCache() {
        List<KnowledgeBase> items = knowledgeBaseMapper.selectAllActive();
        knowledgeCache.clear();
        knowledgeCache.addAll(items);
        log.info("知识库缓存已重新加载，共 {} 条", items.size());
    }

    @Override
    public List<KnowledgeBase> search(String query, int topK) {
        if (query == null || query.isBlank() || knowledgeCache.isEmpty()) {
            return Collections.emptyList();
        }

        // 分词：按空格、标点符号拆分，取有意义的词（长度≥2）
        String[] queryTokens = tokenize(query);

        // 对每条知识条目打分
        List<ScoredItem> scored = new ArrayList<>();
        for (KnowledgeBase item : knowledgeCache) {
            double score = calcScore(queryTokens, item);
            if (score > 0) {
                scored.add(new ScoredItem(item, score));
            }
        }

        // 按分数降序，取topK
        return scored.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(s -> s.item)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBase> searchByPage(String page, int topK) {
        if (page == null || page.isBlank()) {
            return Collections.emptyList();
        }

        // 找匹配的分类
        List<String> categories = null;
        for (Map.Entry<String, List<String>> entry : PAGE_CATEGORY_MAP.entrySet()) {
            if (page.contains(entry.getKey())) {
                categories = entry.getValue();
                break;
            }
        }

        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 按分类过滤，取优先级最高的topK条
        List<String> finalCategories = categories;
        return knowledgeCache.stream()
                .filter(item -> finalCategories.contains(item.getCategory()))
                .sorted((a, b) -> {
                    int pa = a.getPriority() != null ? a.getPriority() : 0;
                    int pb = b.getPriority() != null ? b.getPriority() : 0;
                    return Integer.compare(pb, pa);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public String buildContextBlock(List<KnowledgeBase> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n【相关知识库检索结果】\n");
        sb.append("以下是与用户问题最相关的平台知识，请优先基于这些内容回答：\n\n");

        for (KnowledgeBase item : items) {
            sb.append("▌ ").append(item.getTitle()).append("\n");
            sb.append(item.getContent()).append("\n\n");
        }

        sb.append("---\n");
        sb.append("请基于以上检索到的知识，结合平台背景，给出准确、友好的回答。\n");
        return sb.toString();
    }

    @Override
    public void add(KnowledgeBase item) {
        item.setStatus(1);
        item.setIsDeleted(0);
        knowledgeBaseMapper.insert(item);
        reloadCache();
    }

    @Override
    public void update(KnowledgeBase item) {
        knowledgeBaseMapper.updateById(item);
        reloadCache();
    }

    @Override
    public void delete(Long id) {
        KnowledgeBase item = new KnowledgeBase();
        item.setId(id);
        item.setIsDeleted(1);
        knowledgeBaseMapper.updateById(item);
        reloadCache();
    }

    @Override
    public List<KnowledgeBase> listAll() {
        return knowledgeBaseMapper.selectAllActive();
    }

    // ==================== 内部工具方法 ====================

    /**
     * 中文增强分词：按标点拆分词语后，对中文片段额外生成 2-gram / 3-gram 子词，
     * 提升中文短语的匹配精度（无需引入外部分词库）。
     * 例如 "如何申请退款" → ["如何", "何申", "申请", "请退", "退款", "如何申", "申请退", "请退款"]
     */
    private String[] tokenize(String text) {
        if (text == null) return new String[0];
        // 保留汉字、字母、数字，其余替换为空格
        String cleaned = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ");
        String[] rawTokens = cleaned.split("\\s+");

        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : rawTokens) {
            if (raw.length() < 1) continue;
            // 英文/数字整体保留（长度>=2）
            if (!raw.matches(".*[\\u4e00-\\u9fa5].*")) {
                if (raw.length() >= 2) tokens.add(raw.toLowerCase());
                continue;
            }
            // 中文：生成 2-gram 和 3-gram
            for (int i = 0; i < raw.length(); i++) {
                if (i + 2 <= raw.length()) tokens.add(raw.substring(i, i + 2));
                if (i + 3 <= raw.length()) tokens.add(raw.substring(i, i + 3));
            }
            // 整体片段也保留（用于较长关键词完整匹配）
            if (raw.length() >= 2) tokens.add(raw);
        }
        return tokens.toArray(new String[0]);
    }

    /**
     * 计算问题与知识条目的相关性分数
     * 匹配字段：keywords(权重3) > title(权重2) > content(权重1)
     */
    private double calcScore(String[] queryTokens, KnowledgeBase item) {
        double score = 0.0;

        String title    = item.getTitle()    != null ? item.getTitle().toLowerCase()    : "";
        String content  = item.getContent()  != null ? item.getContent().toLowerCase()  : "";
        String keywords = item.getKeywords() != null ? item.getKeywords().toLowerCase() : "";

        for (String token : queryTokens) {
            String t = token.toLowerCase();
            if (keywords.contains(t)) score += 3.0;
            if (title.contains(t))    score += 2.0;
            if (content.contains(t))  score += 1.0;
        }

        // 优先级加权（priority越高分数越高）
        if (item.getPriority() != null && item.getPriority() > 0) {
            score += item.getPriority() * 0.1;
        }

        return score;
    }

    /**
     * 带分数的知识条目包装
     */
    private static class ScoredItem {
        final KnowledgeBase item;
        final double score;
        ScoredItem(KnowledgeBase item, double score) {
            this.item = item;
            this.score = score;
        }
    }
}
