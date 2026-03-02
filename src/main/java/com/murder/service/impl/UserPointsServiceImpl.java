package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.User;
import com.murder.entity.UserPointsRecord;
import com.murder.vo.UserPointsRecordVO;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserPointsRecordMapper;
import com.murder.service.UserPointsService;
import com.murder.service.VipService;
import com.murder.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户积分服务实现?
 */
@Service
@Slf4j
public class UserPointsServiceImpl implements UserPointsService {

    @Autowired
    private UserPointsRecordMapper userPointsRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VipService vipService;

    @Autowired
    private CouponService couponService;

    /**
     * 获取用户积分信息（含统计数据?
     */
    @Override
    public Map<String, Object> getPointsInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        // 获取当前积分
        User user = userMapper.selectById(userId);
        Integer currentPoints = (user != null && user.getPoints() != null) ? user.getPoints() : 0;
        info.put("currentPoints", currentPoints);
        log.info("获取用户积分: userId={}, currentPoints={}", userId, currentPoints);
        
        // 统计累计获得积分（type=1?
        LambdaQueryWrapper<UserPointsRecord> earnedWrapper = new LambdaQueryWrapper<>();
        earnedWrapper.eq(UserPointsRecord::getUserId, userId);
        earnedWrapper.eq(UserPointsRecord::getType, 1);
        List<UserPointsRecord> earnedRecords = userPointsRecordMapper.selectList(earnedWrapper);
        Integer totalEarned = earnedRecords.stream()
                .mapToInt(UserPointsRecord::getPoints)
                .sum();
        info.put("totalEarned", totalEarned);
        
        // 统计累计使用积分（type=2?
        LambdaQueryWrapper<UserPointsRecord> usedWrapper = new LambdaQueryWrapper<>();
        usedWrapper.eq(UserPointsRecord::getUserId, userId);
        usedWrapper.eq(UserPointsRecord::getType, 2);
        List<UserPointsRecord> usedRecords = userPointsRecordMapper.selectList(usedWrapper);
        Integer totalUsed = usedRecords.stream()
                .mapToInt(UserPointsRecord::getPoints)
                .sum();
        info.put("totalUsed", totalUsed);
        
        // 即将过期积分（暂时设?，如果有过期机制再实现）
        info.put("expiringSoon", 0);
        
        return info;
    }

    /**
     * 分页查询用户积分记录
     */
    @Override
    public PageResult<UserPointsRecordVO> pageQuery(Integer page, Integer pageSize, Long userId, Integer type) {
        Page<UserPointsRecord> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId);
        if (type != null && type > 0) {
            wrapper.eq(UserPointsRecord::getType, type);
        }
        wrapper.orderByDesc(UserPointsRecord::getCreateTime);
        
        // 手动查询总数
        Long total = userPointsRecordMapper.selectCount(wrapper);
        
        userPointsRecordMapper.selectPage(pageInfo, wrapper);
        
        List<UserPointsRecordVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 使用手动查询?total ?
        return new PageResult<>(total, voList);
    }

    /**
     * 获取用户总积?
     */
    @Override
    public Integer getTotalPoints(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getPoints() : 0;
    }

    /**
     * 增加积分（含VIP加成?
     */
    @Override
    @Transactional
    public void addPoints(Long userId, Integer points, String reason) {
        log.info("增加积分开? userId={}, 原始积分={}, reason={}", userId, points, reason);
        
        // 更新用户积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("用户不存在, userId={}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 处理积分为null的情?
        Integer currentPoints = user.getPoints();
        if (currentPoints == null) {
            currentPoints = 0;
            log.warn("用户积分为null，初始化?: userId={}", userId);
        }
        
        // ========== VIP积分加成 ==========
        Integer originalPoints = points; // 原始积分
        Integer finalPoints = points;     // 最终积?
        BigDecimal multiplier = BigDecimal.ONE;
        String bonusInfo = "";
        
        try {
            // 获取VIP积分倍率
            multiplier = vipService.getPointMultiplier(userId);
            if (multiplier != null && multiplier.compareTo(BigDecimal.ONE) > 0) {
                // 应用VIP加成（原始积?* 倍率?
                finalPoints = BigDecimal.valueOf(originalPoints)
                        .multiply(multiplier)
                        .intValue();
                
                int bonusPoints = finalPoints - originalPoints;
                bonusInfo = String.format(" [VIP加成: %s? 额外获得%d积分]", multiplier, bonusPoints);
                log.info("应用VIP积分加成: userId={}, VIP倍率={}, 原始积分={}, 最终积?{}, 额外积分={}", 
                        userId, multiplier, originalPoints, finalPoints, bonusPoints);
            } else {
                log.info("用户非VIP或倍率?: userId={}, multiplier={}", userId, multiplier);
            }
        } catch (Exception e) {
            log.error("获取VIP积分倍率失败，使用原始积? userId={}", userId, e);
            finalPoints = originalPoints;
        }
        // ========== VIP积分加成结束 ==========
        
        Integer newPoints = currentPoints + finalPoints;
        log.info("积分更新: userId={}, 原积?{}, 新积?{}", userId, currentPoints, newPoints);
        
        user.setPoints(newPoints);
        int updateCount = userMapper.updateById(user);
        log.info("更新用户积分结果: updateCount={}", updateCount);
        
        // 根据原因确定来源
        Integer source = determineSource(reason);
        
        // 记录积分变动（记录最终积分）
        String description = reason != null ? reason : "积分增加";
        if (!bonusInfo.isEmpty()) {
            description += bonusInfo;
        }
        
        UserPointsRecord record = UserPointsRecord.builder()
                .userId(userId)
                .points(finalPoints) // 记录最终积分（含加成）
                .type(1) // 1-增加
                .source(source) // 积分来源
                .description(description)
                .build();
        userPointsRecordMapper.insert(record);
        log.info("增加积分完成: userId={}, 新积?{}, 最终积?{}, recordId={}", 
                userId, newPoints, finalPoints, record.getId());
    }

    /**
     * 扣减积分
     */
    @Override
    @Transactional
    public void deductPoints(Long userId, Integer points, String reason) {
        // 更新用户积分
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newPoints = user.getPoints() - points;
            if (newPoints < 0) {
                throw new RuntimeException("积分不足");
            }
            user.setPoints(newPoints);
            userMapper.updateById(user);
            
            // 根据原因确定来源
            Integer source = determineSource(reason);
            
            // 记录积分变动
            UserPointsRecord record = UserPointsRecord.builder()
                    .userId(userId)
                    .points(points)
                    .type(2) // 2-扣减
                    .source(source) // 积分来源
                    .description(reason != null ? reason : "积分扣减")
                    .build();
            userPointsRecordMapper.insert(record);
        }
    }

    /**
     * 每日签到
     */
    @Override
    @Transactional
    public void signIn(Long userId) {
        // 检查今天是否已签到
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId)
               .eq(UserPointsRecord::getDescription, "每日签到")
               .ge(UserPointsRecord::getCreateTime, startOfDay)
               .lt(UserPointsRecord::getCreateTime, endOfDay);
        
        Long count = userPointsRecordMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("今日已签到");
        }
        
        // 签到奖励10积分
        addPoints(userId, 10, "每日签到");
    }


    /**
     * 积分兑换优惠券
     */
    @Override
    @Transactional
    public void exchangeCoupon(Long userId, Long couponId, Integer points) {
        // 检查用户积分是否足够
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getPoints() < points) {
            throw new RuntimeException("积分不足，无法兑换");
        }
        
        // 先发放优惠券（如果发放失败会抛异常，不会扣积分）
        couponService.receiveCoupon(userId, couponId);
        
        // 扣减积分
        deductPoints(userId, points, "兑换优惠券");
        
        log.info("积分兑换优惠券成功: userId={}, couponId={}, points={}", userId, couponId, points);
    }

    /**
     * 实体转VO
     */
    private UserPointsRecordVO convertToVO(UserPointsRecord record) {
        if (record == null) {
            return null;
        }
        UserPointsRecordVO vo = new UserPointsRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
    
    /**
     * 根据描述确定积分来源
     * 1-注册赠送，2-消费获得?-签到奖励?-活动奖励?-管理员调?
     */
    private Integer determineSource(String reason) {
        if (reason == null) {
            return 5; // 默认为管理员调整
        }
        
        reason = reason.toLowerCase();
        
        if (reason.contains("注册") || reason.contains("register")) {
            return 1; // 注册赠?
        } else if (reason.contains("消费") || reason.contains("购买") || reason.contains("预约") || reason.contains("兑换")) {
            return 2; // 消费获得
        } else if (reason.contains("签到") || reason.contains("sign")) {
            return 3; // 签到奖励
        } else if (reason.contains("活动") || reason.contains("奖励") || reason.contains("评价") || reason.contains("收藏")) {
            return 4; // 活动奖励
        } else {
            return 5; // 管理员调?
        }
    }
    
    /**
     * 获取任务完成状?
     */
    @Override
    public Map<String, Object> getTasksStatus(Long userId) {
        Map<String, Object> tasks = new HashMap<>();
        
        // 查询所有积分记?
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId);
        wrapper.eq(UserPointsRecord::getType, 1); // 只查询增加积分的记录
        List<UserPointsRecord> records = userPointsRecordMapper.selectList(wrapper);
        
        // 任务1: 完成预约 - 检查是否有"完成预约"的积分记?
        boolean hasReservation = records.stream()
                .anyMatch(r -> r.getDescription() != null && r.getDescription().contains("完成预约"));
        tasks.put("hasReservation", hasReservation);
        
        // 任务2: 发表评价 - 检查是否有"发表评价"的积分记?
        boolean hasReview = records.stream()
                .anyMatch(r -> r.getDescription() != null && r.getDescription().contains("发表评价"));
        tasks.put("hasReview", hasReview);
        
        // 任务3: 完善资料 - 检查用户昵称和手机号是否都已填?
        User user = userMapper.selectById(userId);
        boolean hasCompleteProfile = user != null 
                && user.getNickname() != null && !user.getNickname().isEmpty()
                && user.getPhone() != null && !user.getPhone().isEmpty();
        tasks.put("hasCompleteProfile", hasCompleteProfile);
        
        // 任务4: 每日签到 - 检查今天是否有签到记录
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        LambdaQueryWrapper<UserPointsRecord> signInWrapper = new LambdaQueryWrapper<>();
        signInWrapper.eq(UserPointsRecord::getUserId, userId)
                .eq(UserPointsRecord::getType, 1)
                .like(UserPointsRecord::getDescription, "签到")
                .ge(UserPointsRecord::getCreateTime, startOfDay)
                .lt(UserPointsRecord::getCreateTime, endOfDay);
        Long signInCount = userPointsRecordMapper.selectCount(signInWrapper);
        tasks.put("hasSignedToday", signInCount > 0);
        
        // 任务5: 邀请好?- 暂时设为false
        tasks.put("hasInviteFriend", false);
        
        // 任务6: 收藏剧本 - 检查是否有"收藏剧本"的积分记录（需要达?个才奖励?
        boolean hasFavoriteScript = records.stream()
                .anyMatch(r -> r.getDescription() != null && r.getDescription().contains("收藏剧本"));
        tasks.put("hasFavoriteScript", hasFavoriteScript);
        
        log.info("获取用户{}任务完成状? {}", userId, tasks);
        return tasks;
    }
    
    /**
     * 收藏剧本奖励积分
     * 每收?个剧本奖?0积分
     */
    @Override
    @Transactional
    public void rewardForFavorite(Long userId) {
        log.info("收藏剧本奖励积分检? userId={}", userId);
        
        // 查询用户收藏数量（需要调用剧本服务或查询数据库）
        // 这里我们通过查询积分记录来判断是否已经奖励过
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId)
               .like(UserPointsRecord::getDescription, "收藏剧本");
        
        Long rewardCount = userPointsRecordMapper.selectCount(wrapper);
        log.info("用户{}已获得收藏奖励{}次", userId, rewardCount);
        
        // 计算应该获得的奖励次数（需要知道当前收藏总数?
        // 暂时假设外部已经检查过，直接奖?
        int currentRewards = rewardCount.intValue();
        int points = 20;
        String description = String.format("收藏剧本达到%d个", (currentRewards + 1) * 5);
        
        // 奖励积分
        addPoints(userId, points, description);
        log.info("用户{}收藏剧本奖励成功: +{}积分", userId, points);
    }
    
    /**
     * 完成预约奖励积分
     * 每完成一次预约奖?00积分
     */
    @Override
    @Transactional
    public void rewardForReservation(Long userId, Long reservationId) {
        log.info("完成预约奖励积分检? userId={}, reservationId={}", userId, reservationId);
        
        // 检查该预约是否已经奖励?
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId)
               .eq(UserPointsRecord::getSource, 2) // source=2表示预约来源
               .like(UserPointsRecord::getDescription, "完成预约");
        
        List<UserPointsRecord> records = userPointsRecordMapper.selectList(wrapper);
        
        // 检查是否有相同预约ID的记录（通过描述中是否包含预约ID来判断）
        boolean alreadyRewarded = records.stream()
                .anyMatch(r -> r.getDescription() != null && 
                               r.getDescription().contains("预约ID:" + reservationId));
        
        if (alreadyRewarded) {
            log.warn("预约{}已经奖励过积分，跳过", reservationId);
            return;
        }
        
        // 奖励积分
        int points = 100;
        String description = String.format("完成预约(预约ID:%d)", reservationId);
        addPoints(userId, points, description);
        log.info("用户{}完成预约{}奖励成功: +{}积分", userId, reservationId, points);
    }
    
    /**
     * 退款时扣除预约奖励的积?
     */
    @Override
    @Transactional
    public void deductForRefund(Long userId, Long reservationId) {
        log.info("退款扣除积分检? userId={}, reservationId={}", userId, reservationId);
        
        // 查找该预约的积分奖励记录
        LambdaQueryWrapper<UserPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPointsRecord::getUserId, userId)
               .eq(UserPointsRecord::getSource, 2) // source=2表示预约来源
               .eq(UserPointsRecord::getType, 1) // type=1表示增加
               .like(UserPointsRecord::getDescription, "完成预约")
               .like(UserPointsRecord::getDescription, "预约ID:" + reservationId);
        
        List<UserPointsRecord> records = userPointsRecordMapper.selectList(wrapper);
        
        if (records.isEmpty()) {
            log.warn("未找到预约{}的积分奖励记录，可能未奖励或已扣除", reservationId);
            return;
        }
        
        // 检查是否已经扣除过
        LambdaQueryWrapper<UserPointsRecord> deductWrapper = new LambdaQueryWrapper<>();
        deductWrapper.eq(UserPointsRecord::getUserId, userId)
                     .eq(UserPointsRecord::getType, 2) // type=2表示扣除
                     .like(UserPointsRecord::getDescription, "退款扣除")
                     .like(UserPointsRecord::getDescription, "预约ID:" + reservationId);
        
        List<UserPointsRecord> deductRecords = userPointsRecordMapper.selectList(deductWrapper);
        
        if (!deductRecords.isEmpty()) {
            log.warn("预约{}的积分已扣除过，跳过", reservationId);
            return;
        }
        
        // 扣除积分
        int points = 100; // 扣除之前奖励的积分
        String description = String.format("退款扣除(预约ID:%d)", reservationId);
        deductPoints(userId, points, description);
        log.info("用户{}预约{}退款，扣除{}积分", userId, reservationId, points);
    }
}
