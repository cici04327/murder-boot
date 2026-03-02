package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI客服控制器
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI客服接口")
@Slf4j
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * AI对话接口
     */
    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
        try {
            String message = (String) params.get("message");
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> history = (List<Map<String, Object>>) params.get("history");
            Map<String, Object> context = (Map<String, Object>) params.get("context");
            
            log.info("AI对话请求: sessionId={}, message={}", sessionId, message);
            
            Map<String, Object> response = aiService.chat(message, sessionId, history, context);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 获取AI推荐
     */
    @GetMapping("/recommend/{type}")
    @Operation(summary = "获取AI推荐")
    public Result<Map<String, Object>> recommend(@PathVariable String type) {
        try {
            log.info("获取AI推荐: type={}", type);
            Map<String, Object> recommendation = aiService.getRecommendation(type);
            return Result.success(recommendation);
        } catch (Exception e) {
            log.error("获取AI推荐失败", e);
            return Result.error("获取推荐失败");
        }
    }

    /**
     * 记录对话
     */
    @PostMapping("/log")
    @Operation(summary = "记录对话")
    public Result<String> logConversation(@RequestBody Map<String, Object> params) {
        try {
            aiService.logConversation(params);
            return Result.success("记录成功");
        } catch (Exception e) {
            log.error("记录对话失败", e);
            return Result.error("记录失败");
        }
    }

    /**
     * 获取常见问题
     */
    @GetMapping("/faq")
    @Operation(summary = "获取常见问题")
    public Result<List<Map<String, Object>>> getFAQ() {
        try {
            List<Map<String, Object>> faq = aiService.getFrequentQuestions();
            return Result.success(faq);
        } catch (Exception e) {
            log.error("获取常见问题失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    @Operation(summary = "提交反馈")
    public Result<String> submitFeedback(@RequestBody Map<String, Object> params) {
        try {
            aiService.submitFeedback(params);
            return Result.success("感谢您的反馈！");
        } catch (Exception e) {
            log.error("提交反馈失败", e);
            return Result.error("提交失败");
        }
    }
}
