package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.StoreReview;
import com.murder.vo.StoreReviewVO;

/**
 * 门店评价服务接口
 */
public interface StoreReviewService {

    /**
     * 分页查询门店评价列表
     */
    PageResult<StoreReviewVO> pageQuery(Long storeId, Integer page, Integer pageSize);

    /**
     * 根据ID查询评价详情
     */
    StoreReviewVO getById(Long id);

    /**
     * 新增评价
     */
    void add(StoreReview review);

    /**
     * 商家回复评价
     */
    void reply(Long id, String reply);

    /**
     * 删除评价
     */
    void delete(Long id);
}
