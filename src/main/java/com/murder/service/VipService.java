package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.VipPackageDTO;
import com.murder.entity.UserVip;
import com.murder.vo.VipPackageVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * VIP服务接口
 */
public interface VipService {

    /**
     * 获取VIP套餐列表
     */
    List<VipPackageVO> getVipPackages();

    /**
     * 根据ID获取VIP套餐详情
     */
    VipPackageVO getVipPackageById(Long id);

    /**
     * 购买VIP
     */
    String purchaseVip(Long userId, Long packageId, String paymentMethod);

    /**
     * 获取用户VIP信息
     */
    Map<String, Object> getUserVipInfo(Long userId);

    /**
     * 检查用户是否是VIP
     */
    boolean isVip(Long userId);

    /**
     * 获取用户VIP等级
     */
    Integer getUserVipLevel(Long userId);

    /**
     * 获取用户积分倍率
     */
    BigDecimal getPointMultiplier(Long userId);

    /**
     * 获取用户VIP折扣
     */
    BigDecimal getVipDiscount(Long userId);

    /**
     * 检查用户是否有预约优先�?
     */
    boolean hasPriorityBooking(Long userId);

    /**
     * 发放月度优惠�?
     */
    void grantMonthlyCoupons(Long userId);

    /**
     * 续费VIP
     */
    String renewVip(Long userId, Long packageId);

    /**
     * 检查并更新过期VIP
     */
    void checkAndUpdateExpiredVip();

    /**
     * 获取用户VIP历史记录
     */
    PageResult<UserVip> getUserVipHistory(Long userId, Integer page, Integer pageSize);

    // ========== 管理端功�?==========

    /**
     * 创建VIP套餐
     */
    void createPackage(VipPackageDTO dto);

    /**
     * 更新VIP套餐
     */
    void updatePackage(VipPackageDTO dto);

    /**
     * 删除VIP套餐
     */
    void deletePackage(Long id);

    /**
     * 上下架VIP套餐
     */
    void updatePackageStatus(Long id, Integer status);

    /**
     * 分页查询VIP套餐（管理端�?
     */
    PageResult<VipPackageVO> pageQueryPackages(Integer page, Integer pageSize, Integer level, Integer status);

    /**
     * 赠送VIP
     */
    void grantVip(Long userId, Integer days, Integer level, String reason);

    /**
     * 延长VIP
     */
    void extendVip(Long userId, Integer days);

    /**
     * 获取VIP统计数据
     */
    Map<String, Object> getVipStatistics();

    /**
     * 分页查询VIP用户列表
     */
    PageResult<Map<String, Object>> pageQueryVipUsers(Integer page, Integer pageSize, Integer level, Integer status);
}

