package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.FeedbackDTO;
import com.murder.vo.FeedbackVO;

/**
 * 留言反馈服务接口
 */
public interface FeedbackService {
    
    /**
     * 提交留言
     * @param feedbackDTO 留言信息
     * @param userId 用户ID（可为空）
     * @param ipAddress IP地址
     */
    void submit(FeedbackDTO feedbackDTO, Long userId, String ipAddress);
    
    /**
     * 分页查询留言列表（管理端）
     */
    PageResult<FeedbackVO> pageQuery(Integer page, Integer pageSize, String subject, Integer status);
    
    /**
     * 查询用户的留言列表
     */
    PageResult<FeedbackVO> getUserFeedbacks(Long userId, Integer page, Integer pageSize);
    
    /**
     * 根据ID查询留言详情
     */
    FeedbackVO getById(Long id);
    
    /**
     * 回复留言（管理端）
     */
    void reply(Long id, String replyContent, Long replyUserId);
    
    /**
     * 更新留言状态（管理端）
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 删除留言（管理端）
     */
    void delete(Long id);
}
