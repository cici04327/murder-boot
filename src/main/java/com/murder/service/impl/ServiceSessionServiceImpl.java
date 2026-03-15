package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.entity.ServiceMessage;
import com.murder.entity.ServiceSession;
import com.murder.mapper.ServiceMessageMapper;
import com.murder.mapper.ServiceSessionMapper;
import com.murder.service.ServiceSessionService;
import com.murder.websocket.ServiceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceSessionServiceImpl implements ServiceSessionService {

    private final ServiceSessionMapper sessionMapper;
    private final ServiceMessageMapper messageMapper;
    private final ServiceWebSocketHandler serviceWebSocketHandler;

    @Override
    @Transactional
    public Long createSession(Long userId, String userName, String initialQuestion) {
        // 检查是否已有进行中的会话
        ServiceSession existing = getActiveSessionByUserId(userId);
        if (existing != null) {
            return existing.getId();
        }

        ServiceSession session = ServiceSession.builder()
                .userId(userId)
                .userName(userName)
                .status(0) // 等待接入
                .initialQuestion(initialQuestion)
                .build();
        sessionMapper.insert(session);

        // 插入系统消息
        insertSystemMessage(session.getId(), "您好！已为您转接人工客服，请稍等...");

        // 通知所有在线超级管理员有新会话
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "new_service_session");
        notify.put("sessionId", session.getId());
        notify.put("userName", userName);
        notify.put("question", initialQuestion);
        notify.put("time", LocalDateTime.now().toString());
        serviceWebSocketHandler.notifyAdmins(notify);

        log.info("创建客服会话: sessionId={}, userId={}", session.getId(), userId);
        return session.getId();
    }

    @Override
    @Transactional
    public void acceptSession(Long sessionId, Long adminId) {
        ServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() != 0) return;

        session.setAdminId(adminId);
        session.setStatus(1); // 进行中
        sessionMapper.updateById(session);

        // 插入系统消息通知用户
        insertSystemMessage(sessionId, "客服已接入，请开始咨询。");

        // 通知用户会话已接入
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "session_accepted");
        notify.put("sessionId", sessionId);
        notify.put("message", "客服已接入，请开始咨询。");
        serviceWebSocketHandler.sendToUser(session.getUserId(), notify);

        log.info("管理员接入会话: sessionId={}, adminId={}", sessionId, adminId);
    }

    @Override
    @Transactional
    public ServiceMessage sendMessage(Long sessionId, String senderType, Long senderId,
                                      String content, String msgType) {
        ServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() == 2) {
            throw new com.murder.common.exception.BaseException("会话已结束");
        }

        ServiceMessage msg = ServiceMessage.builder()
                .sessionId(sessionId)
                .senderType(senderType)
                .senderId(senderId)
                .content(content)
                .msgType(msgType != null ? msgType : "text")
                .isRead(0)
                .build();
        messageMapper.insert(msg);

        // 通过 WebSocket 实时推送消息
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "service_message");
        wsMsg.put("sessionId", sessionId);
        wsMsg.put("senderType", senderType);
        wsMsg.put("senderId", senderId);
        wsMsg.put("content", content);
        wsMsg.put("msgType", msg.getMsgType());
        wsMsg.put("createTime", msg.getCreateTime() != null ? msg.getCreateTime().toString() : LocalDateTime.now().toString());

        if ("user".equals(senderType)) {
            // 用户发的消息 → 推送给管理员
            if (session.getAdminId() != null) {
                serviceWebSocketHandler.sendToAdmin(session.getAdminId(), wsMsg);
            } else {
                serviceWebSocketHandler.notifyAdmins(wsMsg);
            }
        } else {
            // 管理员发的消息 → 推送给用户
            serviceWebSocketHandler.sendToUser(session.getUserId(), wsMsg);
        }

        return msg;
    }

    @Override
    @Transactional
    public void closeSession(Long sessionId, String closerType) {
        ServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) return;

        session.setStatus(2);
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        String closerLabel = "user".equals(closerType) ? "用户" : "客服";
        insertSystemMessage(sessionId, closerLabel + "已结束本次会话。");

        // 通知双方会话结束
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "session_closed");
        notify.put("sessionId", sessionId);
        notify.put("message", closerLabel + "已结束本次会话。");

        serviceWebSocketHandler.sendToUser(session.getUserId(), notify);
        if (session.getAdminId() != null) {
            serviceWebSocketHandler.sendToAdmin(session.getAdminId(), notify);
        }

        log.info("结束客服会话: sessionId={}, closerType={}", sessionId, closerType);
    }

    @Override
    public void rateSession(Long sessionId, Integer rating, String comment) {
        ServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) return;
        session.setRating(rating);
        session.setRatingComment(comment);
        sessionMapper.updateById(session);
    }

    @Override
    public List<ServiceMessage> getMessages(Long sessionId) {
        return messageMapper.listBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> listSessions(Integer status, Integer page, Integer pageSize) {
        Page<ServiceSession> pageObj = new Page<>(page, pageSize);
        sessionMapper.pageWithUserInfo(pageObj, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return result;
    }

    @Override
    public int countWaiting() {
        return sessionMapper.countWaiting();
    }

    @Override
    public ServiceSession getActiveSessionByUserId(Long userId) {
        LambdaQueryWrapper<ServiceSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceSession::getUserId, userId)
                .in(ServiceSession::getStatus, 0, 1)
                .eq(ServiceSession::getIsDeleted, 0)
                .orderByDesc(ServiceSession::getCreateTime)
                .last("LIMIT 1");
        return sessionMapper.selectOne(wrapper);
    }

    private void insertSystemMessage(Long sessionId, String content) {
        ServiceMessage sysMsg = ServiceMessage.builder()
                .sessionId(sessionId)
                .senderType("system")
                .content(content)
                .msgType("system")
                .isRead(1)
                .build();
        messageMapper.insert(sysMsg);
    }
}
