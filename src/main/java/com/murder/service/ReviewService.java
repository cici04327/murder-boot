package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.ReviewDTO;
import com.murder.entity.Review;
import com.murder.vo.ReviewStatisticsVO;
import com.murder.vo.ReviewVO;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 创建评价
     */
    void create(ReviewDTO reviewDTO);

    /**
     * 分页查询评价列表
     */
    PageResult<ReviewVO> pageQuery(Integer page, Integer pageSize, Long storeId, Long scriptId, Long userId, Integer status);

    /**
     * 根据ID查询评价详情
     */
    ReviewVO getById(Long id);

    /**
     * 根据预约ID查询评价
     */
    Review getByReservationId(Long reservationId);

    /**
     * 删除评价
     */
    void delete(Long id);

    /**
     * 审核评价
     */
    void audit(Long id, Integer status, String reason);

    /**
     * 回复评价
     */
    void reply(Long id, String replyContent);

    /**
     * 设置精选评�?
     */
    void setFeatured(Long id, Integer isFeatured);

    /**
     * 获取评价统计信息
     */
    ReviewStatisticsVO getStatistics(Long storeId, Long scriptId);
}
