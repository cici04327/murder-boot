package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.ScriptReviewDTO;
import com.murder.entity.ScriptReview;
import com.murder.vo.ScriptReviewVO;

/**
 * 剧本评价服务接口
 */
public interface ScriptReviewService {

    /**
     * 添加剧本评价
     */
    void add(ScriptReviewDTO reviewDTO);

    /**
     * 分页查询剧本评价
     */
    PageResult<ScriptReviewVO> pageQuery(Long scriptId, Integer page, Integer pageSize);

    /**
     * 根据ID查询评价详情
     */
    ScriptReviewVO getById(Long id);

    /**
     * 删除评价
     */
    void delete(Long id);
    
    /**
     * 根据预约ID查询评价
     */
    ScriptReview getByReservationId(Long reservationId);
}
