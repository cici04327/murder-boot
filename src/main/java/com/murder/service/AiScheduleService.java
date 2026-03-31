package com.murder.service;

import com.murder.dto.AiDmAssignDTO;
import com.murder.dto.AiDmConfirmDTO;
import com.murder.vo.AiDmSuggestVO;

import java.util.List;

/**
 * AI智能排班服务接口（仅DM分配）
 */
public interface AiScheduleService {

    /**
     * AI为排期智能推荐DM（规则打分 + AI解释）
     */
    AiDmSuggestVO suggestDmAssignment(AiDmAssignDTO dto);

    /**
     * 确认并应用DM分配方案
     */
    void confirmDmAssignment(AiDmConfirmDTO dto);

    /**
     * 查询AI排班任务历史
     */
    List<?> listTaskHistory(Long storeId, int limit);

    /**
     * 定时任务：每周一自动为下周排期分配DM
     */
    void autoAssignDmForWeekSchedules();
}
