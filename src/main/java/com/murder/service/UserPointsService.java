package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.vo.UserPointsRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 用户积分服务接口
 */
public interface UserPointsService {

    /**
     * 获取用户积分信息（含统计数据�?
     */
    Map<String, Object> getPointsInfo(Long userId);

    /**
     * 分页查询用户积分记录
     */
    PageResult<UserPointsRecordVO> pageQuery(Integer page, Integer pageSize, Long userId, Integer type);

    /**
     * 获取用户总积�?
     */
    Integer getTotalPoints(Long userId);

    /**
     * 增加积分
     */
    void addPoints(Long userId, Integer points, String reason);

    /**
     * 扣减积分
     */
    void deductPoints(Long userId, Integer points, String reason);

    /**
     * 每日签到
     */
    void signIn(Long userId);


    /**
     * 积分兑换优惠�?
     */
    void exchangeCoupon(Long userId, Long couponId, Integer points);
    
    /**
     * 获取任务完成状�?
     */
    Map<String, Object> getTasksStatus(Long userId);
    
    /**
     * 收藏剧本奖励积分
     * 每收�?个剧本奖�?0积分
     */
    void rewardForFavorite(Long userId);
    
    /**
     * 完成预约奖励积分
     * 每完成一次预约奖�?00积分
     */
    void rewardForReservation(Long userId, Long reservationId);
    
    /**
     * 退款时扣除预约奖励的积�?
     */
    void deductForRefund(Long userId, Long reservationId);
}
