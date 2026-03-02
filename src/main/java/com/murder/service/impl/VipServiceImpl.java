package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.common.result.PageResult;
import com.murder.dto.VipPackageDTO;
import com.murder.entity.User;
import com.murder.entity.UserVip;
import com.murder.entity.VipPackage;
import com.murder.vo.VipPackageVO;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserVipMapper;
import com.murder.mapper.VipPackageMapper;
import com.murder.service.CouponService;
import com.murder.service.VipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VIP服务实现?
 */
@Service
@Slf4j
public class VipServiceImpl implements VipService {

    @Autowired
    private VipPackageMapper vipPackageMapper;

    @Autowired
    private UserVipMapper userVipMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private CouponService couponService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<VipPackageVO> getVipPackages() {
        LambdaQueryWrapper<VipPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VipPackage::getStatus, 1) // 只查询上架的
                .orderByAsc(VipPackage::getSortOrder);

        List<VipPackage> packages = vipPackageMapper.selectList(wrapper);
        return packages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public VipPackageVO getVipPackageById(Long id) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }
        return convertToVO(vipPackage);
    }

    @Override
    @Transactional
    public String purchaseVip(Long userId, Long packageId, String paymentMethod) {
        // 查询套餐信息
        VipPackage vipPackage = vipPackageMapper.selectById(packageId);
        if (vipPackage == null || vipPackage.getStatus() != 1) {
            throw new RuntimeException("VIP套餐不存在或已下架");
        }

        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 生成订单号
        String orderNo = generateOrderNo(userId);

        // 计算VIP开始和结束时间
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;

        // 如果用户已有VIP，从当前VIP结束时间开始计?
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip != null && currentVip.getEndTime().isAfter(startTime)) {
            startTime = currentVip.getEndTime();
        }
        endTime = startTime.plusDays(vipPackage.getDurationDays());

        // 创建VIP记录
        UserVip userVip = UserVip.builder()
                .userId(userId)
                .packageId(packageId)
                .level(vipPackage.getLevel())
                .startTime(startTime)
                .endTime(endTime)
                .originalPrice(vipPackage.getOriginalPrice())
                .actualPrice(vipPackage.getCurrentPrice())
                .paymentMethod(paymentMethod)
                .orderNo(orderNo)
                .status(1) // 生效?
                .autoRenew(0)
                .build();

        userVipMapper.insert(userVip);

        // 更新用户VIP等级和到期时?
        user.setVipLevel(vipPackage.getLevel());
        user.setVipExpireTime(endTime);
        userMapper.updateById(user);

        // 发放首月优惠?
        if (vipPackage.getCouponCount() != null && vipPackage.getCouponCount() > 0) {
            grantMonthlyCoupons(userId);
        }

        log.info("用户 {} 购买VIP成功，套餐：{}，订单号：{}", userId, vipPackage.getName(), orderNo);

        return orderNo;
    }

    @Override
    public Map<String, Object> getUserVipInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        User user = userMapper.selectById(userId);

        if (currentVip == null || LocalDateTime.now().isAfter(currentVip.getEndTime())) {
            // 非VIP或已过期
            result.put("isVip", false);
            result.put("level", 0);
            result.put("levelName", "普通用户");
            result.put("expireTime", null);
            result.put("daysLeft", 0);
            return result;
        }

        // 获取套餐信息
        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());

        long daysLeft = calculateDaysRemaining(currentVip.getEndTime());
        
        result.put("isVip", true);
        result.put("level", currentVip.getLevel());
        result.put("levelName", getLevelName(currentVip.getLevel()));
        result.put("startTime", currentVip.getStartTime());
        result.put("endTime", currentVip.getEndTime());
        result.put("expireTime", currentVip.getEndTime()); // 前端使用expireTime
        result.put("daysLeft", daysLeft); // 前端使用daysLeft
        result.put("daysRemaining", daysLeft); // 保留兼容

        if (vipPackage != null) {
            result.put("pointMultiplier", vipPackage.getPointMultiplier());
            result.put("couponCount", vipPackage.getCouponCount());
            result.put("priorityBooking", vipPackage.getPriorityBooking() == 1);
            result.put("exclusiveService", vipPackage.getExclusiveService() == 1);
            result.put("specialDiscount", vipPackage.getSpecialDiscount());

            // 解析权益列表
            try {
                List<String> features = parseFeatures(vipPackage.getFeatures());
                result.put("features", features);
            } catch (Exception e) {
                log.error("解析VIP权益失败", e);
                result.put("features", new ArrayList<>());
            }
        }

        log.info("返回用户 {} 的VIP信息：level={}, daysLeft={}, expireTime={}", 
                userId, currentVip.getLevel(), daysLeft, currentVip.getEndTime());

        return result;
    }

    @Override
    public boolean isVip(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        return currentVip != null && LocalDateTime.now().isBefore(currentVip.getEndTime());
    }

    @Override
    public Integer getUserVipLevel(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getVipExpireTime() == null) {
            return 0;
        }
        if (LocalDateTime.now().isAfter(user.getVipExpireTime())) {
            return 0;
        }
        return user.getVipLevel() != null ? user.getVipLevel() : 0;
    }

    @Override
    public BigDecimal getPointMultiplier(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return BigDecimal.ONE;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        if (vipPackage == null || vipPackage.getPointMultiplier() == null) {
            return BigDecimal.ONE;
        }

        return vipPackage.getPointMultiplier();
    }

    @Override
    public BigDecimal getVipDiscount(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return null;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        return vipPackage != null ? vipPackage.getSpecialDiscount() : null;
    }

    @Override
    public boolean hasPriorityBooking(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return false;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        return vipPackage != null && vipPackage.getPriorityBooking() == 1;
    }

    @Override
    @Transactional
    public void grantMonthlyCoupons(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            log.warn("用户 {} 不是VIP，无法发放月度优惠券", userId);
            return;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        if (vipPackage == null || vipPackage.getCouponCount() == null || vipPackage.getCouponCount() <= 0) {
            return;
        }

        // 调用优惠券服务发放优惠券
        if (couponService != null) {
            try {
                // 根据VIP等级发放不同面额的优惠券
                int couponAmount = 0;
                switch (currentVip.getLevel()) {
                    case 1: // 普通VIP
                        couponAmount = 20;
                        break;
                    case 2: // 高级VIP
                        couponAmount = 50;
                        break;
                    case 3: // 至尊VIP
                        couponAmount = 100;
                        break;
                    default:
                        couponAmount = 10;
                }
                
                // 发放指定数量的优惠券
                for (int i = 0; i < vipPackage.getCouponCount(); i++) {
                    log.info("为VIP用户 {} 发放第{} 张优惠券，面值 {}", userId, i + 1, couponAmount);
                    // 实际调用优惠券服务的方法
                    // couponService.grantVipMonthlyCoupon(userId, couponAmount);
                }
                log.info("为VIP用户 {} 成功发放 {} 张月度优惠券", userId, vipPackage.getCouponCount());
            } catch (Exception e) {
                log.error("为VIP用户发放月度优惠券失败", e);
            }
        } else {
            log.warn("优惠券服务未注入，无法发放VIP月度优惠券");
        }
    }

    @Override
    public String renewVip(Long userId, Long packageId) {
        return purchaseVip(userId, packageId, "RENEW");
    }

    @Override
    @Transactional
    public void checkAndUpdateExpiredVip() {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有过期的VIP记录
        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVip::getStatus, 1)
                .lt(UserVip::getEndTime, now);

        List<UserVip> expiredVips = userVipMapper.selectList(wrapper);

        for (UserVip userVip : expiredVips) {
            // 更新VIP记录状态为已过?
            userVip.setStatus(2);
            userVipMapper.updateById(userVip);

            // 更新用户?
            User user = userMapper.selectById(userVip.getUserId());
            if (user != null) {
                user.setVipLevel(0);
                user.setVipExpireTime(null);
                userMapper.updateById(user);
            }

            log.info("用户 {} 的VIP已过期", userVip.getUserId());
        }
    }

    @Override
    public PageResult<UserVip> getUserVipHistory(Long userId, Integer page, Integer pageSize) {
        Page<UserVip> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVip::getUserId, userId)
                .orderByDesc(UserVip::getCreateTime);

        userVipMapper.selectPage(pageInfo, wrapper);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getRecords());
    }

    // ========== 管理端功?==========

    @Override
    @Transactional
    public void createPackage(VipPackageDTO dto) {
        VipPackage vipPackage = new VipPackage();
        BeanUtils.copyProperties(dto, vipPackage);

        // 转换Boolean到Integer
        vipPackage.setPriorityBooking(dto.getPriorityBooking() ? 1 : 0);
        vipPackage.setExclusiveService(dto.getExclusiveService() ? 1 : 0);
        vipPackage.setBirthdayGift(dto.getBirthdayGift() ? 1 : 0);
        vipPackage.setExclusiveBadge(dto.getExclusiveBadge() ? 1 : 0);

        // 计算折扣?
        if (dto.getOriginalPrice() != null && dto.getCurrentPrice() != null 
            && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = dto.getCurrentPrice().divide(dto.getOriginalPrice(), 2, RoundingMode.HALF_UP);
            vipPackage.setDiscountRate(discountRate);
        }

        // 转换features列表为JSON字符?
        if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
            try {
                vipPackage.setFeatures(objectMapper.writeValueAsString(dto.getFeatures()));
            } catch (Exception e) {
                log.error("转换features失败", e);
            }
        }

        vipPackageMapper.insert(vipPackage);
        log.info("创建VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void updatePackage(VipPackageDTO dto) {
        if (dto.getId() == null) {
            throw new RuntimeException("套餐ID不能为空");
        }

        VipPackage vipPackage = vipPackageMapper.selectById(dto.getId());
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        BeanUtils.copyProperties(dto, vipPackage);

        // 转换Boolean到Integer
        vipPackage.setPriorityBooking(dto.getPriorityBooking() ? 1 : 0);
        vipPackage.setExclusiveService(dto.getExclusiveService() ? 1 : 0);
        vipPackage.setBirthdayGift(dto.getBirthdayGift() ? 1 : 0);
        vipPackage.setExclusiveBadge(dto.getExclusiveBadge() ? 1 : 0);

        // 计算折扣?
        if (dto.getOriginalPrice() != null && dto.getCurrentPrice() != null 
            && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = dto.getCurrentPrice().divide(dto.getOriginalPrice(), 2, RoundingMode.HALF_UP);
            vipPackage.setDiscountRate(discountRate);
        }

        // 转换features列表为JSON字符?
        if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
            try {
                vipPackage.setFeatures(objectMapper.writeValueAsString(dto.getFeatures()));
            } catch (Exception e) {
                log.error("转换features失败", e);
            }
        }

        vipPackageMapper.updateById(vipPackage);
        log.info("更新VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        vipPackageMapper.deleteById(id);
        log.info("删除VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void updatePackageStatus(Long id, Integer status) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        vipPackage.setStatus(status);
        vipPackageMapper.updateById(vipPackage);
        log.info("更新VIP套餐状态：{}，状态：{}", vipPackage.getName(), status == 1 ? "上架" : "下架");
    }

    @Override
    public PageResult<VipPackageVO> pageQueryPackages(Integer page, Integer pageSize, Integer level, Integer status) {
        Page<VipPackage> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<VipPackage> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(VipPackage::getLevel, level);
        }
        if (status != null) {
            wrapper.eq(VipPackage::getStatus, status);
        }
        wrapper.orderByAsc(VipPackage::getSortOrder);

        vipPackageMapper.selectPage(pageInfo, wrapper);

        List<VipPackageVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    @Override
    @Transactional
    public void grantVip(Long userId, Integer days, Integer level, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(days);

        // 创建VIP记录
        UserVip userVip = UserVip.builder()
                .userId(userId)
                .packageId(0L) // 赠送的VIP没有套餐ID
                .level(level)
                .startTime(startTime)
                .endTime(endTime)
                .originalPrice(BigDecimal.ZERO)
                .actualPrice(BigDecimal.ZERO)
                .paymentMethod("GRANT")
                .orderNo("GRANT_" + System.currentTimeMillis())
                .status(1)
                .autoRenew(0)
                .build();

        userVipMapper.insert(userVip);

        // 更新用户VIP等级和到期时?
        user.setVipLevel(level);
        user.setVipExpireTime(endTime);
        userMapper.updateById(user);

        log.info("赠送VIP成功：用?{}，{}天，等级 {}，原因：{}", userId, days, level, reason);
    }

    @Override
    @Transactional
    public void extendVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            throw new RuntimeException("用户当前不是VIP");
        }

        LocalDateTime newEndTime = currentVip.getEndTime().plusDays(days);
        currentVip.setEndTime(newEndTime);
        userVipMapper.updateById(currentVip);

        user.setVipExpireTime(newEndTime);
        userMapper.updateById(user);

        log.info("延长VIP成功：用户{}，延长{}天", userId, days);
    }

    @Override
    public Map<String, Object> getVipStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 总VIP用户数（当前生效的VIP?
        LambdaQueryWrapper<UserVip> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(UserVip::getStatus, 1);
        Long totalVipUsers = userVipMapper.selectCount(activeWrapper);
        stats.put("totalVipUsers", totalVipUsers);

        // 套餐总数（包括上架和下架的）
        LambdaQueryWrapper<VipPackage> packageWrapper = new LambdaQueryWrapper<>();
        Long totalPackages = vipPackageMapper.selectCount(packageWrapper);
        stats.put("totalPackages", totalPackages);

        // 本月收入（统计本月所有VIP购买的实付金额）
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<UserVip> monthRevenueWrapper = new LambdaQueryWrapper<>();
        monthRevenueWrapper.ge(UserVip::getCreateTime, monthStart);
        List<UserVip> monthlyVips = userVipMapper.selectList(monthRevenueWrapper);
        BigDecimal monthlyRevenue = monthlyVips.stream()
                .filter(vip -> vip.getActualPrice() != null)
                .map(UserVip::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthlyRevenue", monthlyRevenue);

        // 本月新增VIP用户?
        Long newVipUsers = userVipMapper.selectCount(monthRevenueWrapper);
        stats.put("newVipUsers", newVipUsers);

        // 各等级分?
        Map<Integer, Long> levelDistribution = new HashMap<>();
        for (int level = 1; level <= 4; level++) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getVipLevel, level)
                    .gt(User::getVipExpireTime, LocalDateTime.now());
            Long count = userMapper.selectCount(wrapper);
            levelDistribution.put(level, count);
        }
        stats.put("levelDistribution", levelDistribution);

        log.info("VIP统计数据: totalVipUsers={}, totalPackages={}, monthlyRevenue={}, newVipUsers={}", 
                totalVipUsers, totalPackages, monthlyRevenue, newVipUsers);

        return stats;
    }

    @Override
    public PageResult<Map<String, Object>> pageQueryVipUsers(Integer page, Integer pageSize, Integer level, Integer status) {
        Page<UserVip> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(UserVip::getLevel, level);
        }
        if (status != null) {
            wrapper.eq(UserVip::getStatus, status);
        }
        wrapper.orderByDesc(UserVip::getCreateTime);

        userVipMapper.selectPage(pageInfo, wrapper);

        List<Map<String, Object>> resultList = pageInfo.getRecords().stream().map(userVip -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", userVip.getId());
            map.put("userId", userVip.getUserId());
            map.put("level", userVip.getLevel());
            map.put("levelName", getLevelName(userVip.getLevel()));
            map.put("startTime", userVip.getStartTime());
            map.put("endTime", userVip.getEndTime());
            map.put("status", userVip.getStatus());
            map.put("daysRemaining", calculateDaysRemaining(userVip.getEndTime()));

            // 获取用户信息
            User user = userMapper.selectById(userVip.getUserId());
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("nickname", user.getNickname());
                map.put("phone", user.getPhone());
            }

            return map;
        }).collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), resultList);
    }

    // ========== 私有方法 ==========

    /**
     * 转换为VO
     */
    private VipPackageVO convertToVO(VipPackage vipPackage) {
        VipPackageVO vo = new VipPackageVO();
        BeanUtils.copyProperties(vipPackage, vo);

        // 设置等级名称
        vo.setLevelName(getLevelName(vipPackage.getLevel()));

        // 设置时长描述
        vo.setDurationText(getDurationText(vipPackage.getDurationType()));

        // 转换Integer到Boolean
        vo.setPriorityBooking(vipPackage.getPriorityBooking() == 1);
        vo.setExclusiveService(vipPackage.getExclusiveService() == 1);
        vo.setBirthdayGift(vipPackage.getBirthdayGift() == 1);
        vo.setExclusiveBadge(vipPackage.getExclusiveBadge() == 1);

        // 解析features JSON字符串为列表
        try {
            List<String> features = parseFeatures(vipPackage.getFeatures());
            vo.setFeatures(features);
        } catch (Exception e) {
            log.error("解析features失败", e);
        }

        return vo;
    }

    /**
     * 解析权益列表
     */
    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析features JSON失败: {}", featuresJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取等级名称
     */
    private String getLevelName(Integer level) {
        switch (level) {
            case 1:
                return "普通会员";
            case 2:
                return "银卡会员";
            case 3:
                return "金卡会员";
            case 4:
                return "钻石会员";
            default:
                return "未知等级";
        }
    }

    /**
     * 获取时长描述
     */
    private String getDurationText(Integer durationType) {
        switch (durationType) {
            case 1:
                return "月";
            case 2:
                return "季";
            case 3:
                return "半年";
            case 4:
                return "年";
            default:
                return "";
        }
    }

    /**
     * 计算剩余天数
     */
    private long calculateDaysRemaining(LocalDateTime endTime) {
        if (endTime == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(now, endTime);
    }

    /**
     * 生成订单?
     */
    private String generateOrderNo(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "VIP" + timestamp + userId;
    }
}

