package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ReviewDTO;
import com.murder.entity.Review;
import com.murder.vo.ReviewStatisticsVO;
import com.murder.vo.ReviewVO;
import com.murder.mapper.ReviewMapper;
import com.murder.service.ReviewService;
import com.murder.service.UserPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.murder.entity.User;
import com.murder.mapper.UserMapper;
import com.murder.service.StoreService;
import com.murder.service.ScriptService;
import com.murder.service.DmService;
import com.murder.mapper.DmMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价服务实现�?
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private StoreService storeService;

    @Autowired(required = false)
    private ScriptService scriptService;
    
    @Autowired(required = false)
    private UserPointsService userPointsService;

    @Autowired(required = false)
    private DmService dmService;

    @Autowired(required = false)
    private DmMapper dmMapper;

    /**
     * 创建评价
     */
    @Override
    @Transactional
    public void create(ReviewDTO reviewDTO) {
        // 1. 检查预约ID是否存在
        if (reviewDTO.getReservationId() == null) {
            throw new RuntimeException("预约ID不能为空");
        }
        
        // 2. 检查该预约是否已经评价�?
        LambdaQueryWrapper<Review> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Review::getReservationId, reviewDTO.getReservationId());
        Long existCount = reviewMapper.selectCount(checkWrapper);
        if (existCount > 0) {
            throw new RuntimeException("该订单已评价，不能重复评价");
        }
        
        // 3. 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            log.error("获取当前用户ID失败，BaseContext.getCurrentId() 返回 null");
            throw new RuntimeException("请先登录");
        }
        
        // 4. 防重复：同一个预约只能有一条有效评价（历史数据可能脏，这里至少保证新数据不再重复）
        if (reviewDTO.getReservationId() != null) {
            LambdaQueryWrapper<Review> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(Review::getReservationId, reviewDTO.getReservationId());
            existsWrapper.eq(Review::getIsDeleted, 0);
            Long existingCount = reviewMapper.selectCount(existsWrapper);
            if (existingCount != null && existingCount > 0) {
                throw new RuntimeException("该预约已评价，请勿重复提交");
            }
        }

        // 4. 构建评价对象
        Review review = new Review();
        BeanUtils.copyProperties(reviewDTO, review);
        
        // 设置用户ID（必填）
        review.setUserId(userId);
        
        // 默认状态：已通过（可以改为待审核�?�?
        review.setStatus(2); // 2-已通过，直接显�?
        
        // 4. 计算奖励积分
        int rewardPoints = 50; // 基础积分
        
        // 上传图片额外积分
        if (reviewDTO.getImages() != null && !reviewDTO.getImages().isEmpty()) {
            rewardPoints += 10;
        }
        
        // 内容详细额外积分（超�?0字）
        if (reviewDTO.getContent() != null && reviewDTO.getContent().length() > 50) {
            rewardPoints += 10;
        }
        
        // 三项都评分额外积�?
        if (reviewDTO.getStoreRating() != null && reviewDTO.getScriptRating() != null && reviewDTO.getServiceRating() != null) {
            rewardPoints += 5;
        }
        
        review.setRewardPoints(rewardPoints);

        // 自动计算综合评分（前端未传时自动补全，避免数据库 NOT NULL 报错）
        if (review.getOverallRating() == null) {
            int ratingSum = 0;
            int ratingCount = 0;
            if (reviewDTO.getStoreRating() != null) { ratingSum += reviewDTO.getStoreRating(); ratingCount++; }
            if (reviewDTO.getScriptRating() != null) { ratingSum += reviewDTO.getScriptRating(); ratingCount++; }
            if (reviewDTO.getServiceRating() != null) { ratingSum += reviewDTO.getServiceRating(); ratingCount++; }
            review.setOverallRating(ratingCount > 0 ? Math.round((float) ratingSum / ratingCount) : 5);
        }
        
        // 5. 保存评价
        reviewMapper.insert(review);
        log.info("用户{}创建评价成功: reviewId={}, reservationId={}", userId, review.getId(), reviewDTO.getReservationId());
        
        // 6. 发放积分到用户账�?
        if (userId != null) {
            try {
                addPointsForReview(userId, rewardPoints, review.getId());
                log.info("用户{}发表评价，获得{}积分", userId, rewardPoints);
            } catch (Exception e) {
                log.error("发放评价积分失败: userId={}, error={}", userId, e.getMessage(), e);
                // 积分发放失败不影响评价创�?
            }
        }
        
        // 7. 更新门店和剧本的平均评分
        try {
            updateStoreAndScriptRating(reviewDTO.getStoreId(), reviewDTO.getScriptId());
        } catch (Exception e) {
            log.error("更新评分失败", e);
        }

        // 8. 刷新 DM 评分（异步静默，失败不影响主流程）
        if (reviewDTO.getDmId() != null && dmService != null) {
            try {
                dmService.refreshRating(reviewDTO.getDmId());
            } catch (Exception e) {
                log.error("刷新 DM 评分失败: dmId={}", reviewDTO.getDmId(), e);
            }
        }
    }
    
    /**
     * 发放评价积分
     */
    private void addPointsForReview(Long userId, Integer points, Long reviewId) {
        if (userPointsService == null) {
            log.warn("UserPointsService未配置，跳过评价积分发放: userId={}, reviewId={}", userId, reviewId);
            return;
        }

        try {
            String description = "评价所得";
            userPointsService.addPoints(userId, points, description);
            log.info("发放评价积分成功(本地): userId={}, points={}, reviewId={}", userId, points, reviewId);
        } catch (Exception e) {
            log.error("发放评价积分失败: userId={}, points={}, error={}", userId, points, e.getMessage(), e);
            throw new RuntimeException("发放评价积分失败", e);
        }
    }
    
    /**
     * 更新门店和剧本的平均评分
     */
    private void updateStoreAndScriptRating(Long storeId, Long scriptId) {
        try {
            // 计算门店平均评分
            if (storeId != null) {
                LambdaQueryWrapper<Review> storeWrapper = new LambdaQueryWrapper<>();
                storeWrapper.eq(Review::getStoreId, storeId);
                storeWrapper.eq(Review::getStatus, 1);
                List<Review> storeReviews = reviewMapper.selectList(storeWrapper);
                
                if (!storeReviews.isEmpty()) {
                    double avgRating = storeReviews.stream()
                        .map(Review::getStoreRating)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                    BigDecimal avgStoreRating = BigDecimal.valueOf(avgRating)
                        .setScale(1, RoundingMode.HALF_UP);
                    
                    // 单体版本：不再调用外部门店服务更新评分。
                    // 如果需要将评分写回门店表，可在 StoreService 中补充更新接口后在此调用。
                    log.info("计算门店{}平均评分: {}", storeId, avgStoreRating);
                }
            }
            
            // 计算剧本平均评分
            if (scriptId != null) {
                LambdaQueryWrapper<Review> scriptWrapper = new LambdaQueryWrapper<>();
                scriptWrapper.eq(Review::getScriptId, scriptId);
                scriptWrapper.eq(Review::getStatus, 1);
                List<Review> scriptReviews = reviewMapper.selectList(scriptWrapper);
                
                if (!scriptReviews.isEmpty()) {
                    double avgRating = scriptReviews.stream()
                        .map(Review::getScriptRating)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                    BigDecimal avgScriptRating = BigDecimal.valueOf(avgRating)
                        .setScale(1, RoundingMode.HALF_UP);
                    
                    // 单体版本：不再调用外部剧本服务更新评分。
                    // 如果需要将评分写回剧本表，可在 ScriptService 中补充更新接口后在此调用。
                    log.info("计算剧本{}平均评分: {}", scriptId, avgScriptRating);
                }
            }
        } catch (Exception e) {
            log.error("更新平均评分异常", e);
        }
    }

    /**
     * 分页查询评价列表
     */
    @Override
    public PageResult<ReviewVO> pageQuery(Integer page, Integer pageSize, Long storeId, Long scriptId, Long userId, Integer status) {
        // 先查询总数
        LambdaQueryWrapper<Review> countWrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            countWrapper.eq(Review::getStoreId, storeId);
        }
        if (scriptId != null) {
            countWrapper.eq(Review::getScriptId, scriptId);
        }
        if (userId != null) {
            countWrapper.eq(Review::getUserId, userId);
        }
        if (status != null) {
            countWrapper.eq(Review::getStatus, status);
        }
        Long total = reviewMapper.selectCount(countWrapper);
        
        // 再查询分页数�?
        Page<Review> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(Review::getStoreId, storeId);
        }
        if (scriptId != null) {
            wrapper.eq(Review::getScriptId, scriptId);
        }
        if (userId != null) {
            wrapper.eq(Review::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Review::getStatus, status);
        }
        wrapper.orderByDesc(Review::getCreateTime);
        
        reviewMapper.selectPage(pageInfo, wrapper);
        
        List<ReviewVO> voList = new ArrayList<>();
        for (Review review : pageInfo.getRecords()) {
            try {
                ReviewVO vo = convertToVO(review);
                if (vo != null) {
                    voList.add(vo);
                }
            } catch (Exception e) {
                // 记录错误但不中断处理
                log.error("转换ReviewVO失败: reviewId={}, error={}", review.getId(), e.getMessage(), e);
            }
        }
        
        return new PageResult<>(total, voList);
    }

    /**
     * 根据ID查询评价详情
     */
    @Override
    public ReviewVO getById(Long id) {
        Review review = reviewMapper.selectById(id);
        return convertToVO(review);
    }

    /**
     * 根据预约ID查询评价
     */
    @Override
    public Review getByReservationId(Long reservationId) {
        // 历史数据可能存在同一 reservationId 多条评价，selectOne 会抛异常导致接口 500。
        // 这里按创建时间/ID 倒序取最新一条作为“该预约的评价”。
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getReservationId, reservationId);
        wrapper.orderByDesc(Review::getCreateTime);
        wrapper.orderByDesc(Review::getId);
        wrapper.last("LIMIT 1");
        return reviewMapper.selectOne(wrapper);
    }

    /**
     * 删除评价
     */
    @Override
    public void delete(Long id) {
        reviewMapper.deleteById(id);
    }

    /**
     * 审核评价
     */
    @Override
    @Transactional
    public void audit(Long id, Integer status, String reason) {
        Review review = new Review();
        review.setId(id);
        review.setStatus(status);
        review.setAuditReason(reason);
        review.setAuditTime(LocalDateTime.now());
        
        Long auditUserId = BaseContext.getCurrentId();
        if (auditUserId != null) {
            review.setAuditUserId(auditUserId);
        }
        
        reviewMapper.updateById(review);
    }

    /**
     * 回复评价
     */
    @Override
    public void reply(Long id, String replyContent) {
        Review review = new Review();
        review.setId(id);
        review.setReplyContent(replyContent);
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }

    /**
     * 设置精选评�?
     */
    @Override
    public void setFeatured(Long id, Integer isFeatured) {
        Review review = new Review();
        review.setId(id);
        review.setIsFeatured(isFeatured);
        reviewMapper.updateById(review);
        
        // 如果设置为精选，额外奖励
        if (isFeatured == 1) {
            Review existingReview = reviewMapper.selectById(id);
            if (existingReview != null) {
                review.setRewardPoints(existingReview.getRewardPoints() + 50);
                reviewMapper.updateById(review);
                // 发放精选奖励优惠券
                grantFeaturedRewardCoupon(existingReview.getUserId());
            }
        }
    }
    
    /**
     * 发放精选评价奖励优惠券
     */
    private void grantFeaturedRewardCoupon(Long userId) {
        try {
            // 单体版本：不再调用外部服务发券。
            // 如需实现“精选评价送券”，建议在本项目内新增固定优惠券ID并调用 CouponService.receiveCoupon 或 UserPointsService.exchangeCoupon。
            log.info("精选评价奖励触发: userId={}（当前未配置发券逻辑）", userId);
        } catch (Exception e) {
            log.error("发放精选评价奖励优惠券失败", e);
        }
    }

    /**
     * 获取评价统计信息
     */
    @Override
    public ReviewStatisticsVO getStatistics(Long storeId, Long scriptId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(Review::getStoreId, storeId);
        }
        if (scriptId != null) {
            wrapper.eq(Review::getScriptId, scriptId);
        }
        wrapper.eq(Review::getStatus, 2); // 只统计已通过的评�?
        
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        if (reviews.isEmpty()) {
            return ReviewStatisticsVO.builder()
                    .totalCount(0L)
                    .averageRating(BigDecimal.ZERO)
                    .goodReviews(0)
                    .mediumReviews(0)
                    .badReviews(0)
                    .goodRate(BigDecimal.ZERO)
                    .build();
        }
        
        int total = reviews.size();
        
        // 计算平均评分
        double avgRating = reviews.stream()
                .mapToInt(Review::getOverallRating)
                .average()
                .orElse(0.0);
        
        // 统计好评、中评、差�?
        int goodReviews = (int) reviews.stream().filter(r -> r.getOverallRating() >= 4).count();
        int mediumReviews = (int) reviews.stream().filter(r -> r.getOverallRating() == 3).count();
        int badReviews = (int) reviews.stream().filter(r -> r.getOverallRating() < 3).count();
        
        // 计算好评�?
        BigDecimal goodRate = total > 0 ? 
                BigDecimal.valueOf(goodReviews)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)) 
                : BigDecimal.ZERO;
        
        return ReviewStatisticsVO.builder()
                .totalCount((long) total)
                .averageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP))
                .goodReviews(goodReviews)
                .mediumReviews(mediumReviews)
                .badReviews(badReviews)
                .goodRate(goodRate)
                .build();
    }

    /**
     * 转换为VO
     */
    private ReviewVO convertToVO(Review review) {
        if (review == null) {
            return null;
        }
        
        ReviewVO vo = new ReviewVO();
        
        // 简单复制基本属�?
        org.springframework.beans.BeanUtils.copyProperties(review, vo);
        
        // 映射ID字段
        vo.setReservationId(review.getReservationId());
        vo.setIsFeatured(review.getIsFeatured());
        
        // 查询用户昵称、门店名称、剧本名称等
        try {
            if (review.getUserId() != null) {
                try {
                    User user = userMapper.selectById(review.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getNickname());
                        vo.setUserAvatar(user.getAvatar());
                    }
                } catch (Exception e) {
                    log.error("查询用户信息失败: userId={}", review.getUserId(), e);
                }
            }

            if (review.getStoreId() != null && storeService != null) {
                try {
                    com.murder.vo.StoreVO store = storeService.getById(review.getStoreId());
                    if (store != null) {
                        vo.setStoreName(store.getName());
                    }
                } catch (Exception e) {
                    log.error("查询门店信息失败: storeId={}", review.getStoreId(), e);
                }
            }

            if (review.getScriptId() != null && scriptService != null) {
                try {
                    com.murder.entity.Script script = scriptService.getById(review.getScriptId());
                    if (script != null) {
                        vo.setScriptName(script.getName());
                    }
                } catch (Exception e) {
                    log.error("查询剧本信息失败: scriptId={}", review.getScriptId(), e);
                }
            }

            // 填充 DM 信息
            if (review.getDmId() != null && dmMapper != null) {
                try {
                    com.murder.entity.Dm dm = dmMapper.selectById(review.getDmId());
                    if (dm != null) {
                        vo.setDmName(dm.getName());
                        vo.setDmAvatar(dm.getAvatar());
                    }
                } catch (Exception e) {
                    log.error("查询 DM 信息失败: dmId={}", review.getDmId(), e);
                }
            }
        } catch (Exception e) {
            log.error("查询关联信息异常", e);
        }
        
        return vo;
    }
}
