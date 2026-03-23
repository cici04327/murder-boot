package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.exception.CouponException;
import com.murder.common.result.PageResult;
import com.murder.dto.CouponDTO;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.vo.CouponVO;
import com.murder.vo.UserCouponVO;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现类
 */
@Service
@Slf4j
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    /**
     * 分页查询优惠券列?
     */
    @Override
    public PageResult<CouponVO> pageQuery(Integer page, Integer pageSize, String name, Integer type, Integer status) {
        Page<Coupon> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Coupon::getName, name);
        }
        if (type != null) {
            wrapper.eq(Coupon::getType, type);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        
        // 手动查询总数
        Long total = couponMapper.selectCount(wrapper);
        
        couponMapper.selectPage(pageInfo, wrapper);
        
        // 转换为VO
        List<CouponVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToCouponVO)
                .collect(Collectors.toList());
        
        // 使用手动查询?total ?
        return new PageResult<>(total, voList);
    }

    /**
     * 根据ID查询优惠券详?
     */
    @Override
    @org.springframework.cache.annotation.Cacheable(value = "coupon:detail", key = "#id", unless = "#result == null")
    public CouponVO getById(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CouponException("优惠券不存在");
        }
        return convertToCouponVO(coupon);
    }

    /**
     * 新增优惠?
     */
    @Override
    @Transactional
    public void add(CouponDTO couponDTO) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(couponDTO, coupon);
        coupon.setRemainCount(couponDTO.getTotalCount());
        if (coupon.getStatus() == null) {
            coupon.setStatus(1); // 默认上架
        }
        couponMapper.insert(coupon);
    }

    /**
     * 更新优惠?
     */
    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"coupon:detail", "coupon:available"}, allEntries = true)
    public void update(CouponDTO couponDTO) {
        Coupon coupon = couponMapper.selectById(couponDTO.getId());
        if (coupon == null) {
            throw new CouponException("优惠券不存在");
        }
        
        BeanUtils.copyProperties(couponDTO, coupon);
        couponMapper.updateById(coupon);
    }

    /**
     * 删除优惠?
     */
    @Override
    @Transactional
    public void delete(Long id) {
        // 检查是否有用户已领?
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getCouponId, id);
        Long count = userCouponMapper.selectCount(wrapper);
        
        if (count > 0) {
            throw new CouponException("该优惠券已被用户领取，无法删除");
        }
        
        couponMapper.deleteById(id);
    }
    
    /**
     * 上架/下架优惠?
     */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CouponException("优惠券不存在");
        }
        
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    /**
     * 用户领取优惠券（支持免费领取和积分兑换）
     */
    @Override
    @Transactional
    public void receiveCoupon(Long userId, Long couponId) {
        // 查询优惠?
        Coupon coupon = couponMapper.selectById(couponId);
        
        if (coupon == null) {
            throw new CouponException("优惠券不存在");
        }
        
        if (coupon.getStatus() != 1) {
            throw new CouponException("优惠券已下架");
        }
        
        // 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidStartTime() != null && now.isBefore(coupon.getValidStartTime())) {
            throw new CouponException("优惠券未到领取时间");
        }
        if (coupon.getValidEndTime() != null && now.isAfter(coupon.getValidEndTime())) {
            throw new CouponException("优惠券已过期");
        }
        
        if (coupon.getRemainCount() <= 0) {
            throw new CouponException("优惠券已领完");
        }
        
        // 检查是否已领取
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getCouponId, couponId);
        wrapper.in(UserCoupon::getStatus, 1, 2); // 未使用或已使?
        Long count = userCouponMapper.selectCount(wrapper);
        
        if (count > 0) {
            throw new CouponException("您已领取过该优惠券");
        }
        
        // 积分兑换逻辑：单体版本不在这里扣积分。
        // 统一由 UserPointsService.exchangeCoupon(...) 负责扣积分 + 发券，避免重复扣分且不引入任何外部服务调用。
        
        // 创建用户优惠券记?
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .status(1) // 未使?
                .receiveTime(now)
                .expireTime(coupon.getValidEndTime())
                .build();
        userCouponMapper.insert(userCoupon);
        
        // 减少优惠券剩余数?
        coupon.setRemainCount(coupon.getRemainCount() - 1);
        couponMapper.updateById(coupon);
        
        log.info("用户 {} 成功领取优惠?{}", userId, couponId);
    }

    /**
     * 查询用户的优惠券列表
     */
    @Override
    public PageResult<UserCouponVO> getUserCoupons(Long userId, Integer page, Integer pageSize, Integer status) {
        // 先更新已过期的优惠券状态
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getUserId, userId);
        updateWrapper.eq(UserCoupon::getStatus, 1); // 只更新未使用的
        updateWrapper.lt(UserCoupon::getExpireTime, now); // 过期时间小于当前时间
        updateWrapper.set(UserCoupon::getStatus, 3); // 设置为已过期
        
        int expiredCount = userCouponMapper.update(null, updateWrapper);
        if (expiredCount > 0) {
            log.info("用户 {} 有 {} 张优惠券已自动过期", userId, expiredCount);
        }
        
        // 同时更新已使用但过期的优惠券（矫正已使用优惠券的过期状态）
        LambdaUpdateWrapper<UserCoupon> updateUsedWrapper = new LambdaUpdateWrapper<>();
        updateUsedWrapper.eq(UserCoupon::getUserId, userId);
        updateUsedWrapper.eq(UserCoupon::getStatus, 2); // 已使用的
        updateUsedWrapper.lt(UserCoupon::getExpireTime, now); // 过期时间小于当前时间
        updateUsedWrapper.isNotNull(UserCoupon::getUseTime); // 确保有使用时间
        // 注意：已使用的优惠券保持状态为2，不需要改为3
        
        Page<UserCoupon> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getReceiveTime);
        
        userCouponMapper.selectPage(pageInfo, wrapper);
        
        // 调试日志
        log.info("分页查询结果 - 总记录数: {}, 当前页记录数: {}", pageInfo.getTotal(), pageInfo.getRecords().size());
        
        // 转换为VO
        List<UserCouponVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToUserCouponVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(pageInfo.getTotal(), voList);
    }
    
    /**
     * 查询用户可用的优惠券列表
     */
    @Override
    public List<UserCouponVO> getAvailableCoupons(Long userId, BigDecimal orderAmount) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getStatus, 1); // 未使?
        wrapper.orderByDesc(UserCoupon::getReceiveTime);
        
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        
        LocalDateTime now = LocalDateTime.now();
        return userCoupons.stream()
                .filter(uc -> {
                    // 检查是否过?
                    if (uc.getExpireTime() != null && now.isAfter(uc.getExpireTime())) {
                        return false;
                    }
                    
                    // 检查最低消费金额（如果没有传订单金额，则返回所有优惠券?
                    if (orderAmount != null) {
                        Coupon coupon = couponMapper.selectById(uc.getCouponId());
                        if (coupon == null) {
                            return false;
                        }
                        
                        // 如果优惠券有最低消费要求，检查订单金额是否满?
                        if (coupon.getMinAmount() != null && 
                            orderAmount.compareTo(coupon.getMinAmount()) < 0) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(this::convertToUserCouponVO)
                .collect(Collectors.toList());
    }

    /**
     * 使用优惠券（与订单关联）
     */
    @Override
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        useCoupon(userCouponId, orderId, null);
    }

    @Override
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId, Long userId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        
        if (userCoupon == null) {
            throw new CouponException("优惠券不存在");
        }

        if (userId != null && !userId.equals(userCoupon.getUserId())) {
            throw new CouponException("无权使用该优惠券");
        }
        
        if (userCoupon.getStatus() != 1) {
            throw new CouponException("优惠券不可用");
        }
        
        // 检查是否过?
        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getExpireTime() != null && now.isAfter(userCoupon.getExpireTime())) {
            throw new CouponException("优惠券已过期");
        }
        
        // 更新优惠券状态为已使?
        userCoupon.setStatus(2);
        userCoupon.setUseTime(now);
        userCoupon.setOrderId(orderId);
        userCouponMapper.updateById(userCoupon);
        
        log.info("用户优惠?{} 已使用，关联订单 {}", userCouponId, orderId);
    }
    
    /**
     * 计算优惠金额
     */
    @Override
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
        log.info("计算优惠金额: userCouponId={}, orderAmount={}", userCouponId, orderAmount);
        
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            log.warn("用户优惠券不存在: userCouponId={}", userCouponId);
            return BigDecimal.ZERO;
        }
        
        log.info("查询到用户优惠券: id={}, userId={}, couponId={}, status={}, expireTime={}", 
            userCoupon.getId(), userCoupon.getUserId(), userCoupon.getCouponId(), 
            userCoupon.getStatus(), userCoupon.getExpireTime());
        
        if (userCoupon.getStatus() != 1) {
            log.warn("用户优惠券状态不是未使用: userCouponId={}, status={}", userCouponId, userCoupon.getStatus());
            return BigDecimal.ZERO;
        }
        
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            log.warn("优惠券模板不存在: couponId={}", userCoupon.getCouponId());
            return BigDecimal.ZERO;
        }
        
        log.info("查询到优惠券模板: id={}, name={}, type={}, discountValue={}, minAmount={}", 
            coupon.getId(), coupon.getName(), coupon.getType(), coupon.getDiscountValue(), coupon.getMinAmount());
        
        // 检查订单金额是否为空或为零
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("订单金额为空或小于等于0: orderAmount={}", orderAmount);
            return BigDecimal.ZERO;
        }
        
        // 检查最低消费金额
        if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            log.warn("订单金额未达到优惠券使用门槛: orderAmount={}, minAmount={}", orderAmount, coupon.getMinAmount());
            throw new CouponException("订单金额未达到优惠券使用门槛");
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        // 根据优惠券类型计算优惠金额
        switch (coupon.getType()) {
            case 1: // 满减券
                discount = coupon.getDiscountValue();
                log.info("满减券，优惠金额: {}", discount);
                break;
            case 2: // 折扣券：discountValue 表示优惠比例（如0.20表示打8折优惠20%），优惠金额 = 订单金额 × 优惠比例
                discount = orderAmount.multiply(coupon.getDiscountValue())
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("折扣券，优惠比例: {}, 优惠金额: {}", coupon.getDiscountValue(), discount);
                break;
            case 3: // 代金券
                discount = coupon.getDiscountValue();
                log.info("代金券，优惠金额: {}", discount);
                break;
            default:
                log.warn("未知优惠券类型: {}", coupon.getType());
                break;
        }
        
        // 优惠金额不能超过订单金额
        if (discount.compareTo(orderAmount) > 0) {
            log.info("优惠金额超过订单金额，调整为订单金额: {} -> {}", discount, orderAmount);
            discount = orderAmount;
        }
        
        log.info("最终计算的优惠金额: userCouponId={}, discount={}", userCouponId, discount);
        return discount;
    }
    
    /**
     * 退还优惠券（订单取消时?
     */
    @Override
    @Transactional
    public void refundCoupon(Long orderId) {
        refundCoupon(orderId, null);
    }

    @Override
    @Transactional
    public void refundCoupon(Long orderId, Long userId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getOrderId, orderId);
        wrapper.eq(UserCoupon::getStatus, 2); // 已使?
        if (userId != null) {
            wrapper.eq(UserCoupon::getUserId, userId);
        }
        
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        if (userId != null && userCoupons.isEmpty()) {
            throw new CouponException("无权退还该优惠券");
        }
        
        for (UserCoupon userCoupon : userCoupons) {
            // 检查是否过?
            if (userCoupon.getExpireTime() != null && 
                LocalDateTime.now().isAfter(userCoupon.getExpireTime())) {
                userCoupon.setStatus(3); // 设置为已过期
            } else {
                userCoupon.setStatus(1); // 恢复为未使用
            }
            userCoupon.setOrderId(null);
            userCoupon.setUseTime(null);
            userCouponMapper.updateById(userCoupon);
            
            log.info("退还用户优惠券 {}，关联订?{}", userCoupon.getId(), orderId);
        }
    }
    
    /**
     * 批量过期优惠券（定时任务?
     */
    @Override
    @Transactional
    public void expireCoupons() {
        LambdaUpdateWrapper<UserCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserCoupon::getStatus, 1); // 未使?
        wrapper.lt(UserCoupon::getExpireTime, LocalDateTime.now()); // 已过?
        wrapper.set(UserCoupon::getStatus, 3); // 设置为已过期
        
        int count = userCouponMapper.update(null, wrapper);
        log.info("批量过期优惠券，共{} 张", count);
    }
    
    /**
     * 获取优惠券统计信?
     */
    @Override
    public CouponVO getCouponStatistics(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new CouponException("优惠券不存在");
        }
        
        // 统计已领取数?
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getCouponId, couponId);
        Long receivedCount = userCouponMapper.selectCount(wrapper);
        
        // 统计已使用数?
        wrapper.eq(UserCoupon::getStatus, 2);
        Long usedCount = userCouponMapper.selectCount(wrapper);
        
        CouponVO vo = convertToCouponVO(coupon);
        vo.setReceivedCount(receivedCount.intValue());
        vo.setUsedCount(usedCount.intValue());
        
        return vo;
    }
    
    /**
     * 转换为CouponVO
     */
    private CouponVO convertToCouponVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        BeanUtils.copyProperties(coupon, vo);
        
        // 设置类型名称
        vo.setTypeName(getCouponTypeName(coupon.getType()));
        
        // 设置状态名?
        vo.setStatusName(coupon.getStatus() == 1 ? "上架" : "下架");
        
        // 判断是否可领?
        LocalDateTime now = LocalDateTime.now();
        boolean canReceive = coupon.getStatus() == 1 
                && coupon.getRemainCount() > 0
                && (coupon.getValidStartTime() == null || now.isAfter(coupon.getValidStartTime()))
                && (coupon.getValidEndTime() == null || now.isBefore(coupon.getValidEndTime()));
        vo.setCanReceive(canReceive);
        
        // 确保exchangePoints字段被设置（如果为null设为0?
        if (vo.getExchangePoints() == null) {
            vo.setExchangePoints(0);
        }
        
        log.debug("转换优惠券VO - ID: {}, Name: {}, ExchangePoints: {}, RemainCount: {}, CanReceive: {}", 
                  coupon.getId(), coupon.getName(), vo.getExchangePoints(), coupon.getRemainCount(), canReceive);
        
        return vo;
    }
    
    /**
     * 转换为UserCouponVO
     */
    private UserCouponVO convertToUserCouponVO(UserCoupon userCoupon) {
        UserCouponVO vo = new UserCouponVO();
        BeanUtils.copyProperties(userCoupon, vo);
        
        // 查询优惠券详?
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon != null) {
            vo.setCouponName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setTypeName(getCouponTypeName(coupon.getType()));
            vo.setDiscountValue(coupon.getDiscountValue());
            vo.setMinAmount(coupon.getMinAmount());
        }
        
        // 实时检查过期状态并更新（双重保险）
        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getStatus() == 1 && userCoupon.getExpireTime() != null && now.isAfter(userCoupon.getExpireTime())) {
            // 如果发现未使用但已过期的优惠券，立即更新数据库
            userCoupon.setStatus(3);
            userCouponMapper.updateById(userCoupon);
            vo.setStatus(3);
            log.info("实时更新过期优惠券状态: userCouponId={}", userCoupon.getId());
        }
        
        // 设置状态名称（使用最新的状态）
        vo.setStatusName(getUserCouponStatusName(vo.getStatus() != null ? vo.getStatus() : userCoupon.getStatus()));
        
        // 判断是否可用：状态必须是未使用(1)且未过期
        boolean canUse = (vo.getStatus() != null ? vo.getStatus() : userCoupon.getStatus()) == 1
                && (userCoupon.getExpireTime() == null || now.isBefore(userCoupon.getExpireTime()));
        vo.setCanUse(canUse);
        
        return vo;
    }
    
    /**
     * 获取优惠券类型名?
     */
    private String getCouponTypeName(Integer type) {
        switch (type) {
            case 1:
                return "满减券";
            case 2:
                return "折扣券";
            case 3:
                return "代金券";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取用户优惠券状态名?
     */
    private String getUserCouponStatusName(Integer status) {
        switch (status) {
            case 1:
                return "未使用";
            case 2:
                return "已使用";
            case 3:
                return "已过期";
            default:
                return "未知";
        }
    }
}
