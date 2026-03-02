package com.murder.service;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 */
public interface AIService {
    
    /**
     * AI对话
     * @param message 用户消息
     * @param sessionId 会话ID
     * @param history 对话历史
     * @param context 上下文信息
     * @return AI回复
     */
    Map<String, Object> chat(String message, String sessionId, List<Map<String, Object>> history, Map<String, Object> context);
    
    /**
     * 获取推荐
     * @param type 推荐类型
     * @return 推荐结果
     */
    Map<String, Object> getRecommendation(String type);
    
    /**
     * 记录对话
     * @param params 对话参数
     */
    void logConversation(Map<String, Object> params);
    
    /**
     * 获取常见问题
     * @return 常见问题列表
     */
    List<Map<String, Object>> getFrequentQuestions();
    
    /**
     * 提交反馈
     * @param params 反馈参数
     */
    void submitFeedback(Map<String, Object> params);
}
