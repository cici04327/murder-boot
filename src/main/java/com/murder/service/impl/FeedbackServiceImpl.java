package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.FeedbackDTO;
import com.murder.entity.Feedback;
import com.murder.mapper.FeedbackMapper;
import com.murder.service.FeedbackService;
import com.murder.vo.FeedbackVO;
import com.murder.websocket.NotificationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 留言反馈服务实现类
 */
@Service
@Slf4j
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;
    
    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    /**
     * 提交留言
     */
    @Override
    @Transactional
    public void submit(FeedbackDTO feedbackDTO, Long userId, String ipAddress) {
        Feedback feedback = new Feedback();
        BeanUtils.copyProperties(feedbackDTO, feedback);
        feedback.setUserId(userId);
        feedback.setIpAddress(ipAddress);
        feedback.setStatus(0); // 待处理
        
        feedbackMapper.insert(feedback);
        log.info("用户提交留言成功: name={}, subject={}", feedbackDTO.getName(), feedbackDTO.getSubject());
    }

    /**
     * 分页查询留言列表（管理端）
     */
    @Override
    public PageResult<FeedbackVO> pageQuery(Integer page, Integer pageSize, String subject, Integer status) {
        Page<Feedback> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(subject)) {
            wrapper.eq(Feedback::getSubject, subject);
        }
        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getCreateTime);
        
        feedbackMapper.selectPage(pageInfo, wrapper);
        
        List<FeedbackVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    /**
     * 查询用户的留言列表
     */
    @Override
    public PageResult<FeedbackVO> getUserFeedbacks(Long userId, Integer page, Integer pageSize) {
        Page<Feedback> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUserId, userId);
        wrapper.orderByDesc(Feedback::getCreateTime);
        
        feedbackMapper.selectPage(pageInfo, wrapper);
        
        List<FeedbackVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    /**
     * 根据ID查询留言详情
     */
    @Override
    public FeedbackVO getById(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("留言不存在");
        }
        return convertToVO(feedback);
    }

    /**
     * 回复留言（管理端）
     */
    @Override
    @Transactional
    public void reply(Long id, String replyContent, Long replyUserId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("留言不存在");
        }
        
        feedback.setReplyContent(replyContent);
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setReplyUserId(replyUserId);
        feedback.setStatus(2); // 已回复
        
        feedbackMapper.updateById(feedback);
        log.info("管理员回复留言: feedbackId={}, replyUserId={}", id, replyUserId);
        
        // 如果留言用户已登录（有userId），通过 WebSocket 推送通知
        if (feedback.getUserId() != null) {
            try {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "feedback_reply");
                notification.put("title", "您的留言已收到回复");
                notification.put("content", replyContent.length() > 50 ? replyContent.substring(0, 50) + "..." : replyContent);
                notification.put("feedbackId", id);
                notification.put("time", LocalDateTime.now().toString());
                
                webSocketHandler.pushNotification(feedback.getUserId(), notification);
                log.info("留言回复通知已推送: userId={}, feedbackId={}", feedback.getUserId(), id);
            } catch (Exception e) {
                log.error("推送留言回复通知失败: userId={}, feedbackId={}", feedback.getUserId(), id, e);
            }
        }
    }

    /**
     * 更新留言状态（管理端）
     */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("留言不存在");
        }
        
        feedback.setStatus(status);
        feedbackMapper.updateById(feedback);
        log.info("更新留言状态: feedbackId={}, status={}", id, status);
    }

    /**
     * 删除留言（管理端）
     */
    @Override
    @Transactional
    public void delete(Long id) {
        feedbackMapper.deleteById(id);
        log.info("删除留言: feedbackId={}", id);
    }

    /**
     * 转换为VO
     */
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        
        // 设置主题名称
        vo.setSubjectName(getSubjectName(feedback.getSubject()));
        
        // 设置状态名称
        vo.setStatusName(getStatusName(feedback.getStatus()));
        
        return vo;
    }

    /**
     * 获取主题名称
     */
    private String getSubjectName(String subject) {
        if (subject == null) return "未知";
        switch (subject) {
            // 原有类型
            case "platform": return "平台使用问题";
            case "booking": return "预约相关问题";
            case "account": return "账号相关问题";
            case "feedback": return "建议与反馈";
            case "business": return "商务合作";
            // 新增类型（来自 info/contact.vue）
            case "game": return "游戏咨询";
            case "store": return "门店合作";
            case "script": return "剧本投稿";
            case "suggestion": return "功能建议";
            case "bug": return "问题反馈";
            case "other": return "其他问题";
            default: return "未知";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "处理中";
            case 2: return "已回复";
            case 3: return "已关闭";
            default: return "未知";
        }
    }
}
