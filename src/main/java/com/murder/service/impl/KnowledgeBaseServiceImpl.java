package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.AiKnowledgeHitLog;
import com.murder.entity.KnowledgeBase;
import com.murder.mapper.AiKnowledgeHitLogMapper;
import com.murder.mapper.KnowledgeBaseMapper;
import com.murder.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 使用关键词TF-IDF风格打分做轻量级语义检索，无需引入向量数据库
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private AiKnowledgeHitLogMapper aiKnowledgeHitLogMapper;

    // 内存缓存：启动时加载所有启用知识条目
    private final List<KnowledgeBase> knowledgeCache = Collections.synchronizedList(new ArrayList<>());

    // 页面 → 知识分类的映射
    private static final Map<String, List<String>> PAGE_CATEGORY_MAP = new LinkedHashMap<>();

    static {
        PAGE_CATEGORY_MAP.put("/vip", Arrays.asList("vip", "coupon"));
        PAGE_CATEGORY_MAP.put("/reservation", Arrays.asList("reservation", "refund", "payment"));
        PAGE_CATEGORY_MAP.put("/payment", Arrays.asList("payment", "refund"));
        PAGE_CATEGORY_MAP.put("/script", Arrays.asList("reservation", "group"));
        PAGE_CATEGORY_MAP.put("/user/points", Arrays.asList("points", "coupon"));
        PAGE_CATEGORY_MAP.put("/user/coupons", Arrays.asList("coupon", "vip"));
        PAGE_CATEGORY_MAP.put("/group", Arrays.asList("group", "reservation"));
        PAGE_CATEGORY_MAP.put("/store", Arrays.asList("store", "dm"));
        PAGE_CATEGORY_MAP.put("/user/reservations", Arrays.asList("reservation", "refund", "payment"));
    }

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
        if (!StringUtils.hasText(query) || knowledgeCache.isEmpty()) {
            return Collections.emptyList();
        }

        String[] queryTokens = tokenize(query);
        List<ScoredItem> scored = new ArrayList<>();
        for (KnowledgeBase item : knowledgeCache) {
            double score = calcScore(queryTokens, item);
            if (score > 0) {
                scored.add(new ScoredItem(item, score));
            }
        }

        return scored.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(s -> s.item)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBase> searchByPage(String page, int topK) {
        if (!StringUtils.hasText(page)) {
            return Collections.emptyList();
        }

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

        List<String> finalCategories = categories;
        return knowledgeCache.stream()
                .filter(item -> finalCategories.contains(item.getCategory()))
                .sorted((a, b) -> {
                    int pa = Optional.ofNullable(a.getPriority()).orElse(0);
                    int pb = Optional.ofNullable(b.getPriority()).orElse(0);
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
        normalizeItem(item, true);
        knowledgeBaseMapper.insert(item);
        reloadCache();
    }

    @Override
    public void update(KnowledgeBase item) {
        normalizeItem(item, false);
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

    @Override
    public PageResult<KnowledgeBase> pageQuery(Integer page, Integer pageSize, String category, String keyword, Integer status) {
        Page<KnowledgeBase> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getIsDeleted, 0)
                .eq(StringUtils.hasText(category), KnowledgeBase::getCategory, category)
                .eq(status != null, KnowledgeBase::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(KnowledgeBase::getTitle, keyword)
                        .or()
                        .like(KnowledgeBase::getContent, keyword)
                        .or()
                        .like(KnowledgeBase::getKeywords, keyword)
                        .or()
                        .like(KnowledgeBase::getFaqQuestion, keyword))
                .orderByDesc(KnowledgeBase::getPriority)
                .orderByDesc(KnowledgeBase::getUpdateTime)
                .orderByDesc(KnowledgeBase::getId);

        Page<KnowledgeBase> result = knowledgeBaseMapper.selectPage(pageInfo, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public List<Map<String, Object>> getFaqList(Integer limit) {
        int actualLimit = (limit == null || limit <= 0) ? 8 : Math.min(limit, 20);
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getIsDeleted, 0)
                .eq(KnowledgeBase::getStatus, 1)
                .eq(KnowledgeBase::getIsFaq, 1)
                .orderByDesc(KnowledgeBase::getPriority)
                .orderByDesc(KnowledgeBase::getHitCount)
                .orderByDesc(KnowledgeBase::getUpdateTime)
                .last("limit " + actualLimit);

        return knowledgeBaseMapper.selectList(wrapper).stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("question", StringUtils.hasText(item.getFaqQuestion()) ? item.getFaqQuestion() : item.getTitle());
                    map.put("category", item.getCategory());
                    map.put("title", item.getTitle());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void recordHitLog(String sessionId, Long userId, String query, String page, List<KnowledgeBase> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeBase item : items) {
            AiKnowledgeHitLog logItem = AiKnowledgeHitLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .query(query)
                    .knowledgeId(item.getId())
                    .knowledgeTitle(item.getTitle())
                    .category(item.getCategory())
                    .page(page)
                    .createTime(now)
                    .build();
            aiKnowledgeHitLogMapper.insert(logItem);

            KnowledgeBase updateItem = new KnowledgeBase();
            updateItem.setId(item.getId());
            updateItem.setHitCount(Optional.ofNullable(item.getHitCount()).orElse(0) + 1);
            updateItem.setLastHitTime(now);
            knowledgeBaseMapper.updateById(updateItem);
            item.setHitCount(updateItem.getHitCount());
            item.setLastHitTime(now);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        LambdaQueryWrapper<KnowledgeBase> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(KnowledgeBase::getIsDeleted, 0);
        List<KnowledgeBase> allItems = knowledgeBaseMapper.selectList(allWrapper);

        LambdaQueryWrapper<AiKnowledgeHitLog> allHitWrapper = new LambdaQueryWrapper<>();
        allHitWrapper.orderByDesc(AiKnowledgeHitLog::getCreateTime);
        List<AiKnowledgeHitLog> hitLogs = aiKnowledgeHitLogMapper.selectList(allHitWrapper);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayHitCount = hitLogs.stream()
                .filter(log -> log.getCreateTime() != null && !log.getCreateTime().isBefore(todayStart))
                .count();

        List<Map<String, Object>> categoryStats = allItems.stream()
                .collect(Collectors.groupingBy(KnowledgeBase::getCategory, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("category", entry.getKey());
                    map.put("count", entry.getValue().size());
                    map.put("activeCount", entry.getValue().stream().filter(item -> Objects.equals(item.getStatus(), 1)).count());
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());

        List<Map<String, Object>> topKnowledge = allItems.stream()
                .sorted((a, b) -> Integer.compare(Optional.ofNullable(b.getHitCount()).orElse(0), Optional.ofNullable(a.getHitCount()).orElse(0)))
                .limit(5)
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("title", item.getTitle());
                    map.put("category", item.getCategory());
                    map.put("hitCount", Optional.ofNullable(item.getHitCount()).orElse(0));
                    map.put("lastHitTime", item.getLastHitTime());
                    return map;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> recentQueries = hitLogs.stream()
                .filter(log -> StringUtils.hasText(log.getQuery()))
                .collect(Collectors.groupingBy(AiKnowledgeHitLog::getQuery, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("query", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", allItems.size());
        stats.put("activeCount", allItems.stream().filter(item -> Objects.equals(item.getStatus(), 1)).count());
        stats.put("faqCount", allItems.stream().filter(item -> Objects.equals(item.getIsFaq(), 1)).count());
        stats.put("totalHitCount", hitLogs.size());
        stats.put("todayHitCount", todayHitCount);
        stats.put("categoryStats", categoryStats);
        stats.put("topKnowledge", topKnowledge);
        stats.put("recentQueries", recentQueries);
        return stats;
    }

    private void normalizeItem(KnowledgeBase item, boolean isCreate) {
        if (isCreate) {
            item.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            item.setIsDeleted(0);
            item.setHitCount(item.getHitCount() == null ? 0 : item.getHitCount());
        }
        item.setPriority(item.getPriority() == null ? 0 : item.getPriority());
        item.setIsFaq(item.getIsFaq() == null ? 0 : item.getIsFaq());
        if (item.getIsFaq() == 1 && !StringUtils.hasText(item.getFaqQuestion())) {
            item.setFaqQuestion(item.getTitle());
        }
        if (item.getIsFaq() == 0) {
            item.setFaqQuestion(null);
        }
    }

    private String[] tokenize(String text) {
        if (text == null) return new String[0];
        String cleaned = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ");
        String[] rawTokens = cleaned.split("\\s+");

        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : rawTokens) {
            if (raw.length() < 1) continue;
            if (!raw.matches(".*[\\u4e00-\\u9fa5].*")) {
                if (raw.length() >= 2) tokens.add(raw.toLowerCase());
                continue;
            }
            for (int i = 0; i < raw.length(); i++) {
                if (i + 2 <= raw.length()) tokens.add(raw.substring(i, i + 2));
                if (i + 3 <= raw.length()) tokens.add(raw.substring(i, i + 3));
            }
            if (raw.length() >= 2) tokens.add(raw);
        }
        return tokens.toArray(new String[0]);
    }

    private double calcScore(String[] queryTokens, KnowledgeBase item) {
        double score = 0.0;

        String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
        String content = item.getContent() != null ? item.getContent().toLowerCase() : "";
        String keywords = item.getKeywords() != null ? item.getKeywords().toLowerCase() : "";
        String faqQuestion = item.getFaqQuestion() != null ? item.getFaqQuestion().toLowerCase() : "";

        for (String token : queryTokens) {
            String t = token.toLowerCase();
            if (keywords.contains(t)) score += 3.0;
            if (title.contains(t)) score += 2.0;
            if (faqQuestion.contains(t)) score += 2.0;
            if (content.contains(t)) score += 1.0;
        }

        if (item.getPriority() != null && item.getPriority() > 0) {
            score += item.getPriority() * 0.1;
        }
        if (item.getHitCount() != null && item.getHitCount() > 0) {
            score += Math.min(item.getHitCount(), 100) * 0.01;
        }

        return score;
    }

    private static class ScoredItem {
        final KnowledgeBase item;
        final double score;

        ScoredItem(KnowledgeBase item, double score) {
            this.item = item;
            this.score = score;
        }
    }
}
