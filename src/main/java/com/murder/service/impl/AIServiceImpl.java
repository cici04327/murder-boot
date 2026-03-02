package com.murder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.service.AIService;
import lombok.extern.slf4j.Slf4j;
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

    public AIServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> chat(String message, String sessionId, List<Map<String, Object>> history, Map<String, Object> context) {
        log.info("AI对话: provider={}, message={}", aiProvider, message);
        
        try {
            // 根据不同的AI提供商调用不同的API
            switch (aiProvider.toLowerCase()) {
                case "openai":
                    return chatWithOpenAI(message, history, context);
                case "openrouter":
                    return chatWithOpenRouter(message, history, context);
                case "wenxin":
                    return chatWithWenxin(message, history, context);
                case "tongyi":
                    return chatWithTongyi(message, history, context);
                case "mock":
                default:
                    return chatWithMock(message, history, context);
            }
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return createErrorResponse("AI服务暂时不可用");
        }
    }

    /**
     * OpenAI API调用
     */
    private Map<String, Object> chatWithOpenAI(String message, List<Map<String, Object>> history, Map<String, Object> context) {
        try {
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(context);
            
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
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(context);
            
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
     * 构建系统提示词
     */
    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的剧本杀预约平台客服助手，你的名字是小剧。\n\n");
        prompt.append("你的职责：\n");
        prompt.append("1. 帮助用户了解剧本、预约流程、支付方式、退款政策等信息\n");
        prompt.append("2. 推荐合适的剧本和门店\n");
        prompt.append("3. 解答用户的疑问，态度友好、专业\n");
        prompt.append("4. 如果问题超出你的能力范围，建议用户联系人工客服\n\n");
        
        prompt.append("平台信息：\n");
        prompt.append("- 客服热线：400-123-4567\n");
        prompt.append("- 服务时间：9:00-21:00\n");
        prompt.append("- 邮箱：service@jubensha.com\n\n");
        
        prompt.append("退款政策：\n");
        prompt.append("- 预约前7天以上取消：全额退款\n");
        prompt.append("- 预约前3-7天取消：退款80%\n");
        prompt.append("- 预约前1-3天取消：退款50%\n");
        prompt.append("- 预约当天取消：不予退款\n\n");
        
        // 添加用户上下文
        if (context != null && context.containsKey("userInfo")) {
            Map<String, Object> userInfo = (Map<String, Object>) context.get("userInfo");
            if (userInfo != null) {
                prompt.append("当前用户信息：\n");
                if (userInfo.containsKey("nickname")) {
                    prompt.append("- 昵称：").append(userInfo.get("nickname")).append("\n");
                }
                if (userInfo.containsKey("vipLevel")) {
                    prompt.append("- VIP等级：").append(userInfo.get("vipLevel")).append("\n");
                }
                prompt.append("\n");
            }
        }
        
        prompt.append("回复要求：\n");
        prompt.append("1. 使用友好、亲切的语气\n");
        prompt.append("2. 回答简洁明了，重点突出\n");
        prompt.append("3. 适当使用emoji增加亲和力\n");
        prompt.append("4. 提供具体的操作步骤\n");
        prompt.append("5. 必要时提供快捷操作链接\n");
        
        return prompt.toString();
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
        // TODO: 记录对话到数据库，用于分析和优化
        log.info("记录对话: {}", params);
    }

    @Override
    public List<Map<String, Object>> getFrequentQuestions() {
        // TODO: 从数据库查询常见问题
        List<Map<String, Object>> faq = new ArrayList<>();
        faq.add(Map.of("question", "如何预约剧本？", "category", "预约"));
        faq.add(Map.of("question", "如何申请退款？", "category", "退款"));
        faq.add(Map.of("question", "支持哪些支付方式？", "category", "支付"));
        return faq;
    }

    @Override
    public void submitFeedback(Map<String, Object> params) {
        // TODO: 保存用户反馈到数据库
        log.info("收到反馈: {}", params);
    }
}
