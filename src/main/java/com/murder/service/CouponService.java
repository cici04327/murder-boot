package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.CouponDTO;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.vo.CouponVO;
import com.murder.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接�?
 */
public interface CouponService {

    /**
     * 分页查询优惠券列�?
     */
    PageResult<CouponVO> pageQuery(Integer page, Integer pageSize, String name, Integer type, Integer status);

    /**
     * 根据ID查询优惠券详�?
     */
    CouponVO getById(Long id);

    /**
     * 新增优惠�?
     */
    void add(CouponDTO couponDTO);

    /**
     * 更新优惠�?
     */
    void update(CouponDTO couponDTO);

    /**
     * 删除优惠�?
     */
    void delete(Long id);
    
    /**
     * 上架/下架优惠�?
     */
    void updateStatus(Long id, Integer status);

    /**
     * 用户领取优惠�?
     */
    void receiveCoupon(Long userId, Long couponId);

    /**
     * 查询用户的优惠券列表
     */
    PageResult<UserCouponVO> getUserCoupons(Long userId, Integer page, Integer pageSize, Integer status);
    
    /**
     * 查询用户可用的优惠券列表
     */
    List<UserCouponVO> getAvailableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 使用优惠券（与订单关联）
     */
    void useCoupon(Long userCouponId, Long orderId);

    /**
     * 用户使用自己的优惠券（校验归属）
     */
    void useCoupon(Long userCouponId, Long orderId, Long userId);
    
    /**
     * 计算优惠金额
     */
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);
    
    /**
     * 退还优惠券（订单取消时�?
     */
    void refundCoupon(Long orderId);

    /**
     * 用户退还自己的优惠券（校验归属）
     */
    void refundCoupon(Long orderId, Long userId);
    
    /**
     * 批量过期优惠券（定时任务�?
     */
    void expireCoupons();
    
    /**
     * 获取优惠券统计信�?
     */
    CouponVO getCouponStatistics(Long couponId);
}
