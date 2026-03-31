package com.murder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.entity.AiConversationLog;
import com.murder.entity.KnowledgeBase;
import com.murder.mapper.AiConversationLogMapper;
import com.murder.service.AIService;
import com.murder.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI服务实现类
 * 支持多种AI服务提供商：OpenAI, 文心一言, 通义千问等
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // 配置项（从application.yml读取）
    @Value("${ai.provider:openai}")
    private String aiProvider; // openai, wenxin, tongyi, mock
    
    @Value("${ai.api-key:}")
    private String apiKey;
    
    @Value("${ai.api-url:}")
    private String apiUrl;
    
    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${ai.temperature:0.7}")
    private double temperature;
    
    @Value("${ai.max-tokens:1000}")
    private int maxTokens;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private AiConversationLogMapper aiConversationLogMapper;

    public AIServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // 转人工关键词列表
    private static final List<String> TRANSFER_KEYWORDS = Arrays.asList(
        "转人工", "人工客服", "转客服", "人工服务", "真人客服", "找客服", "联系客服"
    );

    @Override
    public Map<String, Object> chat(String message, String sessionId, List<Map<String, Object>> history, Map<String, Object> context) {
        log.info("AI对话: provider={}, message={}", aiProvider, message);

        Map<String, Object> runtimeContext = context == null ? new HashMap<>() : new HashMap<>(context);
        runtimeContext.put("_sessionId", sessionId);

        // 优先检测转人工意图，直接返回转接标记，不调用 AI 接口
        if (TRANSFER_KEYWORDS.stream().anyMatch(message::contains)) {
            log.info("检测到转人工意图，直接返回转接响应");
            Map<String, Object> response = new HashMap<>();
            response.put("reply", "好的，马上为您转接人工客服，请稍等...");
            response.put("suggestions", Collections.emptyList());
            response.put("actions", Arrays.asList(Map.of("label", "点击转接人工客服", "type", "transfer")));
            response.put("triggerTransfer", true);
            response.put("timestamp", System.currentTimeMillis());
            saveConversationLog(sessionId, message, (String) response.get("reply"), runtimeContext, 1);
            return response;
        }

        try {
            Map<String, Object> response;
            switch (aiProvider.toLowerCase()) {
                case "openai":
                    response = chatWithOpenAI(message, history, runtimeContext);
                    break;
                case "openrouter":
                    response = chatWithOpenRouter(message, history, runtimeContext);
                    break;
                case "deepseek":
                    response = chatWithDeepSeek(message, history, runtimeContext);
                    break;
                case "wenxin":
                    response = chatWithWenxin(message, history, runtimeContext);
                    break;
                case "tongyi":
                    response = chatWithTongyi(message, history, runtimeContext);
                    break;
                case "mock":
                default:
                    response = chatWithMock(message, history, runtimeContext);
                    break;
            }
            saveConversationLog(sessionId, message, (String) response.get("reply"), runtimeContext, 0);
            return response;
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return createErrorResponse("AI服务暂时不可用");
        }
    }

    /**
     * DeepSeek 官方 API 调用
     * DeepSeek 兼容 OpenAI Chat Completions 协议
     */
    private Map<String, Object> chatWithDeepSeek(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // RAG: 先检索相关知识，注入到系统提示词
            String knowledgeContext = retrieveKnowledge(message, context);
            String systemPrompt = buildSystemPrompt(context) + knowledgeContext;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null && !history.isEmpty()) {
                int startIdx = Math.max(0, history.size() - 10);
                for (int i = startIdx; i < history.size(); i++) {
                    Map<String, Object> msg = history.get(i);
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if (role != null && content != null) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }

            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = (apiUrl == null || apiUrl.isBlank())
                    ? "https://api.deepseek.com/chat/completions"
                    : apiUrl;
            log.info("调用DeepSeek API: url={}, model={}", url, model);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("DeepSeek返回未知错误");
                log.error("DeepSeek返回错误: {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            JsonNode choices = root.path("choices");
            String reply = "";
            if (choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).path("message");
                if (messageNode.has("content") && !messageNode.path("content").isNull()) {
                    reply = messageNode.path("content").asText("");
                }
                if (reply.isEmpty() && messageNode.has("reasoning_content")) {
                    reply = messageNode.path("reasoning_content").asText("");
                }
            }

            if (reply.isEmpty()) {
                log.warn("DeepSeek响应解析失败，原始响应: {}", response.getBody());
                throw new RuntimeException("无法解析DeepSeek响应");
            }

            reply = cleanReply(reply);
            return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
        } catch (Exception e) {
            log.error("DeepSeek调用失败", e);
            throw new RuntimeException("DeepSeek服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * OpenAI API调用
     */
    private Map<String, Object> chatWithOpenAI(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // RAG: 先检索相关知识，注入到系统提示词
            String knowledgeContext = retrieveKnowledge(message, context);
            String systemPrompt = buildSystemPrompt(context) + knowledgeContext;
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            
            // 添加历史对话（最近5轮）
            if (history != null && !history.isEmpty()) {
                int startIdx = Math.max(0, history.size() - 10);
                for (int i = startIdx; i < history.size(); i++) {
                    Map<String, Object> msg = history.get(i);
                    messages.add(Map.of(
                        "role", (String) msg.get("role"),
                        "content", (String) msg.get("content")
                    ));
                }
            }
            
            // 添加当前消息
            messages.add(Map.of("role", "user", "content", message));
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String url = apiUrl.isEmpty() ? "https://api.openai.com/v1/chat/completions" : apiUrl;
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            // 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            String reply = root.path("choices").get(0).path("message").path("content").asText();
            
            return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
            
        } catch (Exception e) {
            log.error("OpenAI调用失败", e);
            throw new RuntimeException("OpenAI服务调用失败", e);
        }
    }

    /**
     * OpenRouter API调用
     * OpenRouter使用与OpenAI兼容的API格式
     * 支持DeepSeek R1等推理模型（返回reasoning_content）
     */
    private Map<String, Object> chatWithOpenRouter(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // RAG: 先检索相关知识，注入到系统提示词
            String knowledgeContext = retrieveKnowledge(message, context);
            String systemPrompt = buildSystemPrompt(context) + knowledgeContext;
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            
            // 添加历史对话（最近5轮）
            if (history != null && !history.isEmpty()) {
                int startIdx = Math.max(0, history.size() - 10);
                for (int i = startIdx; i < history.size(); i++) {
                    Map<String, Object> msg = history.get(i);
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if (role != null && content != null) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }
            
            // 添加当前消息
            messages.add(Map.of("role", "user", "content", message));
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            // OpenRouter特定头部
            headers.set("HTTP-Referer", "https://jubensha.com");
            headers.set("X-Title", "剧本杀预约平台AI客服");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            log.info("调用OpenRouter API: url={}, model={}", apiUrl, model);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            // 解析响应 - 兼容多种模型返回格式
            JsonNode root = objectMapper.readTree(response.getBody());
            log.debug("OpenRouter原始响应: {}", response.getBody());
            
            String reply = "";
            
            // 检查是否有错误
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText();
                log.error("OpenRouter返回错误: {}", errorMsg);
                throw new RuntimeException("OpenRouter API错误: " + errorMsg);
            }
            
            // 获取choices数组
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice != null) {
                    JsonNode messageNode = firstChoice.path("message");
                    
                    // 优先获取content字段
                    if (messageNode.has("content") && !messageNode.path("content").isNull()) {
                        reply = messageNode.path("content").asText("");
                    }
                    
                    // 如果content为空，尝试获取reasoning_content（DeepSeek R1模型）
                    if (reply.isEmpty() && messageNode.has("reasoning_content")) {
                        reply = messageNode.path("reasoning_content").asText("");
                    }
                    
                    // 如果还是空，尝试text字段
                    if (reply.isEmpty() && firstChoice.has("text")) {
                        reply = firstChoice.path("text").asText("");
                    }
                }
            }
            
            // 如果仍然没有获取到回复
            if (reply.isEmpty()) {
                log.warn("OpenRouter响应解析失败，原始响应: {}", response.getBody());
                throw new RuntimeException("无法解析AI响应");
            }
            
            // 清理回复内容（去除可能的思考过程标记）
            reply = cleanReply(reply);
            
            log.info("OpenRouter响应成功，回复长度: {}", reply.length());
            return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
            
        } catch (Exception e) {
            log.error("OpenRouter调用失败", e);
            throw new RuntimeException("OpenRouter服务调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理AI回复内容
     * 移除思考过程标记、多余空白等
     */
    private String cleanReply(String reply) {
        if (reply == null) return "";
        
        // 移除<think>标签内容（DeepSeek R1的思考过程）
        reply = reply.replaceAll("<think>[\\s\\S]*?</think>", "");
        
        // 移除可能的Markdown代码块标记
        reply = reply.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "");
        
        // 清理多余的空行
        reply = reply.replaceAll("\\n{3,}", "\n\n");
        
        return reply.trim();
    }

    /**
     * 文心一言API调用
     */
    private Map<String, Object> chatWithWenxin(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // TODO: 实现文心一言API调用
            // 文档: https://cloud.baidu.com/doc/WENXINWORKSHOP/s/jlil56u11
            
            String systemPrompt = buildSystemPrompt(context);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", buildMessages(message, history, systemPrompt));
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String reply = root.path("result").asText();
            
            return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
            
        } catch (Exception e) {
            log.error("文心一言调用失败", e);
            throw new RuntimeException("文心一言服务调用失败", e);
        }
    }

    /**
     * 通义千问API调用
     */
    private Map<String, Object> chatWithTongyi(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // TODO: 实现通义千问API调用
            // 文档: https://help.aliyun.com/zh/dashscope/
            
            String systemPrompt = buildSystemPrompt(context);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("messages", buildMessages(message, history, systemPrompt));
            requestBody.put("model", model.isEmpty() ? "qwen-turbo" : model);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", temperature);
            requestBody.put("parameters", parameters);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String reply = root.path("output").path("text").asText();
            
            return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
            
        } catch (Exception e) {
            log.error("通义千问调用失败", e);
            throw new RuntimeException("通义千问服务调用失败", e);
        }
    }

    /**
     * Mock模式（用于测试）
     */
    private Map<String, Object> chatWithMock(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        log.info("使用Mock模式响应");
        
        // 简单的关键词匹配
        String reply = matchKeyword(message);
        
        return createSuccessResponse(reply, extractSuggestions(reply), extractActions(message));
    }

    /**
     * RAG检索：根据用户消息和上下文检索最相关的知识条目，拼装为Prompt片段
     */
    private String retrieveKnowledge(String message, Map<String, Object> context) {
        if (knowledgeBaseService == null) return "";
        try {
            List<KnowledgeBase> results = new ArrayList<>();

            List<KnowledgeBase> queryResults = knowledgeBaseService.search(message, 4);
            results.addAll(queryResults);

            String page = null;
            if (context != null) {
                page = (String) context.get("page");
                if (page != null && !page.isBlank()) {
                    List<KnowledgeBase> pageResults = knowledgeBaseService.searchByPage(page, 2);
                    Set<Long> existIds = results.stream()
                            .map(KnowledgeBase::getId)
                            .collect(java.util.stream.Collectors.toSet());
                    pageResults.stream()
                            .filter(k -> !existIds.contains(k.getId()))
                            .forEach(results::add);
                }
            }

            if (results.isEmpty()) return "";

            String sessionId = context != null ? (String) context.get("_sessionId") : null;
            Long userId = extractUserId(context);
            knowledgeBaseService.recordHitLog(sessionId, userId, message, page, results);
            return knowledgeBaseService.buildContextBlock(results);
        } catch (Exception e) {
            log.warn("RAG知识检索失败，降级为无检索模式: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 构建系统提示词 —— RAG增强版
     * 基础系统提示词保留平台角色定位，具体业务知识由RAG检索动态注入
     */
    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();

        // ========== 身份定位 ==========
        prompt.append("你是「剧本杀预约与门店管理平台」的专属AI客服，名字叫小剧。\n");
        prompt.append("你不是通用剧本杀顾问，而是这个平台的内部客服。\n");
        prompt.append("回答问题时，必须优先基于本平台已实现的真实功能，不能虚构或承诺平台没有的能力。\n\n");

        // ========== 平台整体结构 ==========
        prompt.append("【平台角色体系】\n");
        prompt.append("本平台分为用户端和管理端：\n");
        prompt.append("- 普通用户：通过用户端完成剧本浏览、预约、支付、会员、拼团等操作\n");
        prompt.append("- 系统管理员：负责平台级用户、门店、剧本、通知、反馈和数据统计管理\n");
        prompt.append("- 门店管理员：负责本门店信息维护、经营看板、员工管理、排期和预约处理\n");
        prompt.append("- 门店员工：按权限角色分为 MANAGER（店长/副店长）、DM（主持人）、CLERK（服务员）\n");
        prompt.append("  * MANAGER 可查看预约、核销、完成场次、分配DM、处理退款、管理员工、看报表\n");
        prompt.append("  * DM 可查看分配给自己的预约、完成场次、查看通知\n");
        prompt.append("  * CLERK 可查看预约、到店核销、查看通知\n\n");

        // ========== 预约核心业务 ==========
        prompt.append("【预约流程】\n");
        prompt.append("用户预约剧本杀的完整流程：\n");
        prompt.append("1. 浏览剧本列表，选择感兴趣的剧本\n");
        prompt.append("2. 选择门店和场次（排期）\n");
        prompt.append("3. 填写参与人数\n");
        prompt.append("4. 选择可用优惠券（如有）\n");
        prompt.append("5. 确认总价，完成支付宝支付\n");
        prompt.append("6. 支付成功后，系统自动下发核销码，并推送站内通知\n");
        prompt.append("7. 到店后出示核销码，由门店员工完成核销\n");
        prompt.append("8. 场次结束后，员工将预约标记为已完成\n");
        prompt.append("9. 完成后用户可提交评价，获得积分奖励\n\n");
        prompt.append("预约状态说明：\n");
        prompt.append("- 待确认（1）：已提交，等待门店确认\n");
        prompt.append("- 已确认（2）：门店已确认，等待游戏开始\n");
        prompt.append("- 已完成（3）：游戏结束，核销完成\n");
        prompt.append("- 已取消（4）：用户或管理员取消\n\n");

        // ========== 退款政策（来自真实业务逻辑）==========
        prompt.append("【退款规则】（本平台真实规则，请严格按此回答）\n");
        prompt.append("- 游戏开始前 24小时以上：全额退款 ✅\n");
        prompt.append("- 游戏开始前 12~24小时：退款80% ⚠️\n");
        prompt.append("- 游戏开始前 6~12小时：退款50% ⚠️\n");
        prompt.append("- 游戏开始前 6小时内：不支持退款 ❌\n");
        prompt.append("- 已核销（到店）的订单：不支持退款 ❌\n");
        prompt.append("退款申请步骤：进入我的预约 → 找到订单 → 点击申请退款 → 填写原因 → 等待审核\n");
        prompt.append("退款到账时间：支付宝 1-3个工作日\n\n");

        // ========== 改期规则 ==========
        prompt.append("【改期规则】\n");
        prompt.append("- 游戏开始前 24小时以上：可免费改期1次 ✅\n");
        prompt.append("- 游戏开始前 12~24小时：需联系门店确认\n");
        prompt.append("- 游戏开始前 12小时内：不支持改期 ❌\n");
        prompt.append("改期步骤：进入我的预约 → 选择订单 → 点击修改时间 → 选择新场次\n\n");

        // ========== VIP会员体系（来自真实代码）==========
        prompt.append("【VIP会员体系】（本平台真实实现，请严格按此回答）\n");
        prompt.append("VIP分4个等级：\n");
        prompt.append("- 等级1：见习侦探 —— 9.5折优惠，每月2张×10元体验券，生日礼券30元\n");
        prompt.append("- 等级2：银章侦探 —— 9折优惠，每月5张×20元体验券，生日礼券80元\n");
        prompt.append("- 等级3：金章侦探 —— 8.5折优惠，每月10张×50元体验券，生日礼券150元\n");
        prompt.append("- 等级4：传奇侦探 —— 8折优惠，每月15张×100元体验券，生日礼券200元\n");
        prompt.append("VIP通用权益：\n");
        prompt.append("- 积分倍率加成（按套餐配置，VIP等级越高倍率越高）\n");
        prompt.append("- 优先预约热门剧本\n");
        prompt.append("- 专属客服服务\n");
        prompt.append("- 每月发放月度体验券（当月有效，月底过期）\n");
        prompt.append("- 生日月自动发放生日专享礼券\n");
        prompt.append("VIP购买方式：仅支持支付宝支付，支持续费，续费天数自动叠加\n\n");

        // ========== 积分体系（来自真实代码）==========
        prompt.append("【积分体系】（本平台真实实现）\n");
        prompt.append("获取积分的方式：\n");
        prompt.append("- 每日签到：+10积分\n");
        prompt.append("- 完成预约游戏：+100积分\n");
        prompt.append("- 发表评价：另有积分奖励\n");
        prompt.append("- 收藏剧本达到里程碑：+20积分/次\n");
        prompt.append("- VIP用户享有积分倍率加成\n");
        prompt.append("积分用途：\n");
        prompt.append("- 积分兑换优惠券（按优惠券配置的兑换所需积分）\n");
        prompt.append("- 每种优惠券每天只能兑换一次\n\n");

        // ========== 优惠券体系 ==========
        prompt.append("【优惠券体系】\n");
        prompt.append("优惠券类型：满减券、折扣券、代金券\n");
        prompt.append("获取方式：\n");
        prompt.append("- VIP会员每月自动发放月度体验券\n");
        prompt.append("- 积分兑换（在积分页面操作）\n");
        prompt.append("- 平台活动发放\n");
        prompt.append("使用规则：\n");
        prompt.append("- 每笔订单限用一张优惠券\n");
        prompt.append("- 结合VIP折扣可叠加使用（先算VIP折扣，再减优惠券）\n");
        prompt.append("- 优惠券有有效期，过期自动失效\n");
        prompt.append("- 部分券有最低消费门槛\n\n");

        // ========== 拼团功能 ==========
        prompt.append("【拼团/拼单功能】\n");
        prompt.append("- 用户人数不足时，可发起拼团房间，等待其他玩家加入\n");
        prompt.append("- 也可以直接加入他人发起的拼团房间\n");
        prompt.append("- 当人数不足剧本要求时，系统会自动发起拼团\n");
        prompt.append("- 人齐后各自完成支付\n\n");

        // ========== 支付方式 ==========
        prompt.append("【支付方式】\n");
        prompt.append("- 本平台目前仅支持支付宝支付（预约和VIP均通过支付宝完成）\n\n");

        // ========== 通知体系 ==========
        prompt.append("【通知体系】\n");
        prompt.append("平台支持站内实时通知，场景包括：\n");
        prompt.append("- 预约成功通知\n");
        prompt.append("- 支付成功通知（含核销码）\n");
        prompt.append("- VIP月度体验券到账通知\n");
        prompt.append("- 系统公告通知\n\n");

        // ========== 人工客服 ==========
        prompt.append("【人工客服】\n");
        prompt.append("- 客服热线：400-123-4567\n");
        prompt.append("- 服务时间：9:00-22:00（全年无休）\n");
        prompt.append("- 邮箱：service@jubensha.com\n");
        prompt.append("- 也可在聊天窗口说转人工，直接发起在线人工客服\n\n");

        // ========== 动态用户上下文 ==========
        if (context != null) {
            String page = (String) context.get("page");
            if (page != null && !page.isBlank()) {
                prompt.append("【当前页面】用户正在浏览：").append(page).append("\n");
                if (page.contains("/vip")) {
                    prompt.append("→ 用户在VIP页面，优先解答VIP相关问题\n");
                } else if (page.contains("/reservation")) {
                    prompt.append("→ 用户在预约相关页面，优先解答预约、退款、改期问题\n");
                } else if (page.contains("/script")) {
                    prompt.append("→ 用户在剧本页面，优先解答剧本选择、推荐问题\n");
                } else if (page.contains("/user/points")) {
                    prompt.append("→ 用户在积分页面，优先解答积分获取和兑换问题\n");
                } else if (page.contains("/user/coupons")) {
                    prompt.append("→ 用户在优惠券页面，优先解答优惠券使用问题\n");
                }
                prompt.append("\n");
            }

            Map<String, Object> userInfo = context.containsKey("userInfo")
                    ? (Map<String, Object>) context.get("userInfo") : null;
            if (userInfo != null) {
                prompt.append("【当前用户信息】\n");
                if (userInfo.containsKey("nickname")) {
                    prompt.append("- 昵称：").append(userInfo.get("nickname")).append("\n");
                }
                if (userInfo.containsKey("vipLevel")) {
                    Integer vipLevel = (Integer) userInfo.get("vipLevel");
                    String levelName = vipLevel != null && vipLevel > 0
                            ? getVipLevelName(vipLevel) : "普通用户（非VIP）";
                    prompt.append("- VIP等级：").append(levelName).append("\n");
                }
                prompt.append("\n");
            }
        }

        // ========== 回答原则 ==========
        prompt.append("【回答原则】\n");
        prompt.append("1. 优先基于「相关知识库检索结果」中的内容回答，这些是平台的真实规则\n");
        prompt.append("2. 涉及退款金额、VIP折扣、积分数量等数字，请严格按知识库内容说明，不要自行推断\n");
        prompt.append("3. 语气友好、亲切，可适当使用emoji\n");
        prompt.append("4. 回答简洁清晰，给出具体操作步骤，不要泛泛而谈\n");
        prompt.append("5. 如果用户问的是后台操作（管理端/员工端），请根据平台角色体系说明对应权限\n");
        prompt.append("6. 知识库中没有的内容，不要凭空编造，建议用户联系人工客服：400-123-4567\n");
        prompt.append("7. 如果用户说转人工，请主动提示转接人工客服\n");

        return prompt.toString();
    }

    /**
     * 获取VIP等级名称（与 VipServiceImpl 保持一致）
     */
    private String getVipLevelName(int level) {
        switch (level) {
            case 1: return "见习侦探（Lv.1）";
            case 2: return "银章侦探（Lv.2）";
            case 3: return "金章侦探（Lv.3）";
            case 4: return "传奇侦探（Lv.4）";
            default: return "普通用户";
        }
    }

    /**
     * 构建消息列表
     */
    private List<Map<String, String>> buildMessages(String message, List<Map<String, Object>> history, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 10);
            for (int i = startIdx; i < history.size(); i++) {
                Map<String, Object> msg = history.get(i);
                messages.add(Map.of(
                    "role", (String) msg.get("role"),
                    "content", (String) msg.get("content")
                ));
            }
        }
        
        messages.add(Map.of("role", "user", "content", message));
        return messages;
    }

    /**
     * 简单的关键词匹配（Mock模式使用）
     */
    private String matchKeyword(String message) {
        message = message.toLowerCase();
        
        if (message.contains("预约") || message.contains("怎么订") || message.contains("如何订")) {
            return "您好！预约剧本很简单：\n\n1. 浏览剧本列表，选择喜欢的剧本\n2. 点击\"立即预约\"按钮\n3. 选择门店、日期和时间\n4. 填写联系信息和人数\n5. 选择优惠券并完成支付\n\n💡 建议提前3-7天预约，避免档期紧张哦！\n\n需要我帮您推荐剧本吗？";
        }
        
        if (message.contains("退款") || message.contains("取消") || message.contains("退订")) {
            return "关于退款政策：\n\n📅 预约前7天以上取消：全额退款 ✅\n📅 预约前3-7天取消：退款80% ⚠️\n📅 预约前1-3天取消：退款50% ⚠️\n📅 预约当天取消：不予退款 ❌\n\n申请步骤：\n1. 进入\"个人中心-我的预约\"\n2. 找到订单点击\"申请退款\"\n3. 选择退款原因提交\n4. 等待审核（1-2个工作日）\n\n需要帮您申请退款吗？";
        }
        
        if (message.contains("支付") || message.contains("付款") || message.contains("支付方式")) {
            return "我们支持多种支付方式：\n\n💰 支付宝支付\n💚 微信支付\n🏦 银行卡支付\n🎁 优惠券抵扣\n⭐ 积分兑换\n\n所有支付均采用SSL加密，安全可靠！\n\n您在支付时遇到问题了吗？";
        }
        
        if (message.contains("优惠券") || message.contains("折扣") || message.contains("优惠")) {
            return "获取优惠券的方式：\n\n🎉 新用户注册送50元券\n📅 每日签到赚积分兑换\n🎊 参与活动赢取专属券\n🎂 生日月自动发放生日礼券\n👥 邀请好友送30元券\n⭐ 完成评价获随机优惠券\n\n前往\"个人中心-我的优惠券\"查看哦！";
        }
        
        if (message.contains("人工") || message.contains("客服") || message.contains("联系")) {
            return "如需人工客服帮助：\n\n☎️ 客服热线：400-123-4567\n⏰ 服务时间：9:00-21:00\n📧 邮箱：service@jubensha.com\n💬 微信公众号：剧本杀预约平台\n\n您也可以继续向我提问，我会尽力帮助您！";
        }
        
        if (message.contains("推荐") || message.contains("什么好玩") || message.contains("热门")) {
            return "为您推荐热门剧本：\n\n🔥 推理类：《午夜凶铃》《寂静岭》\n😱 恐怖类：《鬼屋惊魂》《诡异旅馆》\n💔 情感类：《时光倒流》《再见爱人》\n⚔️ 机制类：《王朝争霸》《谍战风云》\n\n您更喜欢哪种类型呢？我可以给您更详细的推荐！";
        }
        
        // 默认回复
        return "您好！我是智能客服小剧，很高兴为您服务！😊\n\n我可以帮您：\n• 了解预约流程\n• 查询退款政策\n• 推荐热门剧本\n• 解答各类疑问\n\n请告诉我您需要什么帮助？";
    }

    /**
     * 从回复中提取建议问题
     */
    private List<String> extractSuggestions(String reply) {
        List<String> suggestions = new ArrayList<>();
        
        if (reply.contains("预约")) {
            suggestions.add("如何预约剧本？");
        }
        if (reply.contains("退款")) {
            suggestions.add("退款需要多久？");
        }
        if (reply.contains("支付")) {
            suggestions.add("支持哪些支付方式？");
        }
        if (reply.contains("优惠")) {
            suggestions.add("如何获取优惠券？");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("查看热门剧本");
            suggestions.add("联系人工客服");
        }
        
        return suggestions;
    }

    /**
     * 根据消息提取可执行操作
     */
    private List<Map<String, String>> extractActions(String message) {
        List<Map<String, String>> actions = new ArrayList<>();
        
        if (message.contains("预约") || message.contains("订")) {
            actions.add(Map.of("label", "立即预约", "route", "/script/list"));
        }
        if (message.contains("退款")) {
            actions.add(Map.of("label", "查看我的预约", "route", "/user/reservations"));
        }
        if (message.contains("优惠券")) {
            actions.add(Map.of("label", "我的优惠券", "route", "/user/coupons"));
        }
        if (message.contains("门店")) {
            actions.add(Map.of("label", "查看门店", "route", "/store/list"));
        }
        
        return actions;
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String reply, List<String> suggestions, List<Map<String, String>> actions) {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", reply);
        response.put("suggestions", suggestions);
        response.put("actions", actions);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", "抱歉，" + error + "。请稍后重试或联系人工客服：400-123-4567");
        response.put("suggestions", Arrays.asList("联系人工客服", "查看帮助中心"));
        response.put("actions", Arrays.asList(Map.of("label", "帮助中心", "route", "/help")));
        response.put("error", true);
        return response;
    }

    private Long extractUserId(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        Object userInfoObj = context.get("userInfo");
        if (!(userInfoObj instanceof Map)) {
            return null;
        }
        Object idObj = ((Map<?, ?>) userInfoObj).get("id");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        }
        if (idObj instanceof String) {
            try {
                return Long.parseLong((String) idObj);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void saveConversationLog(String sessionId, String question, String answer, Map<String, Object> context, Integer isTransferred) {
        if (aiConversationLogMapper == null || (question == null && answer == null)) {
            return;
        }
        try {
            String page = context != null ? (String) context.get("page") : null;
            AiConversationLog logItem = AiConversationLog.builder()
                    .userId(extractUserId(context))
                    .sessionId(sessionId)
                    .question(question)
                    .answer(answer)
                    .page(page)
                    .isTransferred(isTransferred == null ? 0 : isTransferred)
                    .provider(aiProvider)
                    .model(model)
                    .build();
            aiConversationLogMapper.insert(logItem);
        } catch (Exception e) {
            log.warn("记录AI对话日志失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRecommendation(String type) {
        // TODO: 根据用户偏好和历史记录生成个性化推荐
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("recommendations", new ArrayList<>());
        return result;
    }

    @Override
    public void logConversation(Map<String, Object> params) {
        if (params == null) {
            return;
        }
        Map<String, Object> context = params.get("context") instanceof Map ? (Map<String, Object>) params.get("context") : new HashMap<>();
        String sessionId = params.get("sessionId") instanceof String ? (String) params.get("sessionId") : null;
        String question = params.get("question") instanceof String ? (String) params.get("question") : (String) params.get("message");
        String answer = params.get("answer") instanceof String ? (String) params.get("answer") : null;
        Integer isTransferred = params.get("isTransferred") instanceof Number ? ((Number) params.get("isTransferred")).intValue() : 0;
        saveConversationLog(sessionId, question, answer, context, isTransferred);
    }

    @Override
    public List<Map<String, Object>> getFrequentQuestions() {
        if (knowledgeBaseService != null) {
            List<Map<String, Object>> faq = knowledgeBaseService.getFaqList(8);
            if (faq != null && !faq.isEmpty()) {
                return faq;
            }
        }
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(Map.of("question", "如何预约剧本？", "category", "reservation"));
        fallback.add(Map.of("question", "如何申请退款？", "category", "refund"));
        fallback.add(Map.of("question", "支持哪些支付方式？", "category", "payment"));
        return fallback;
    }

    @Override
    public void submitFeedback(Map<String, Object> params) {
        // TODO: 保存用户反馈到数据库
        log.info("收到反馈: {}", params);
    }
}
