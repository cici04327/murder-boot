package com.murder.service;

import com.murder.entity.ServiceMessage;
import com.murder.entity.ServiceSession;

import java.util.List;
import java.util.Map;

public interface ServiceSessionService {

    /** 用户发起转人工请求，返回会话ID */
    Long createSession(Long userId, String userName, String initialQuestion);

    /** 管理员接入会话 */
    void acceptSession(Long sessionId, Long adminId);

    /** 发送消息（用户或管理员均可调用） */
    ServiceMessage sendMessage(Long sessionId, String senderType, Long senderId, String content, String msgType);

    /** 结束会话 */
    void closeSession(Long sessionId, String closerType);

    /** 用户提交评价 */
    void rateSession(Long sessionId, Integer rating, String comment);

    /** 获取会话消息列表 */
    List<ServiceMessage> getMessages(Long sessionId);

    /** 分页获取会话列表（管理端） */
    Map<String, Object> listSessions(Integer status, Integer page, Integer pageSize);

    /** 获取等待中的会话数量 */
    int countWaiting();

    /** 根据用户ID获取当前进行中的会话 */
    ServiceSession getActiveSessionByUserId(Long userId);
}
