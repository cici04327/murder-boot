package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.murder.service.NotificationService;
import com.murder.service.AdminNotificationService;
import com.murder.service.StoreRoomService;
import com.murder.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 预约服务实现
 */
@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;
    
    @Autowired
    private StoreRoomService storeRoomService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminNotificationService adminNotificationService;
    
    @Autowired(required = false)
    private com.murder.service.ReviewService reviewService;
    
    @Autowired(required = false)
    private com.murder.service.UserPointsService userPointsService;
    
    @Autowired(required = false)
    private com.murder.service.StoreService storeService;
    
    @Autowired(required = false)
    private com.murder.service.ScriptService scriptService;
    
    @Autowired(required = false)
    private com.murder.service.GroupOrderService groupOrderService;

    @Autowired(required = false)
    private com.murder.service.VipService vipService;

    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    /**
     * 创建预约
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Reservation create(ReservationDTO reservationDTO) {
        // 检查房间可用?
        if (reservationDTO.getRoomId() != null && reservationDTO.getReservationTime() != null) {
            String reservationTimeStr = reservationDTO.getReservationTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            boolean isAvailable = checkRoomAvailability(
                    reservationDTO.getRoomId(), 
                    reservationTimeStr, 
                    3.0 // 默认时长3小时
            );
            if (!isAvailable) {
                throw new RuntimeException("该时段房间已被预约，请选择其他时间");
            }
        }
        
        // 生成预约编号
        String reservationNo = generateReservationNo();
        
        // 构建预约对象
        Reservation reservation = new Reservation();
        BeanUtils.copyProperties(reservationDTO, reservation);
        reservation.setOrderNo(reservationNo);
        reservation.setStatus(1); // 待确?
        reservation.setPayStatus(0); // 未支?
        
        // 第一步：优先使用前端预计算的VIP折扣，后端再验证
        BigDecimal vipDiscountAmount = BigDecimal.ZERO;
        BigDecimal vipDiscountRate = null;
        BigDecimal basePrice = reservation.getTotalPrice();

        if (reservationDTO.getVipDiscountAmount() != null
                && reservationDTO.getVipDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && reservationDTO.getVipDiscount() != null) {
            // 前端传来了VIP折扣，后端验证是否合法
            BigDecimal expectedDiscount = null;
            if (vipService != null && reservationDTO.getUserId() != null) {
                expectedDiscount = vipService.getVipDiscount(reservationDTO.getUserId());
            }
            // 验证折扣率一致（允许0.01误差）
            if (expectedDiscount != null
                    && reservationDTO.getVipDiscount().subtract(expectedDiscount).abs().compareTo(new BigDecimal("0.01")) <= 0) {
                vipDiscountRate = expectedDiscount;
                vipDiscountAmount = reservation.getTotalPrice()
                        .multiply(BigDecimal.ONE.subtract(vipDiscountRate))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                basePrice = reservation.getTotalPrice().subtract(vipDiscountAmount);
                log.info("VIP折扣验证通过: userId={}, rate={}, amount={}", reservationDTO.getUserId(), vipDiscountRate, vipDiscountAmount);
            } else {
                log.warn("VIP折扣验证失败，不应用折扣: userId={}, 前端rate={}, 后端rate={}",
                        reservationDTO.getUserId(), reservationDTO.getVipDiscount(), expectedDiscount);
            }
        } else if (vipService != null && reservationDTO.getUserId() != null) {
            // 前端没传，后端自行查询计算（兜底）
            try {
                BigDecimal vipDiscount = vipService.getVipDiscount(reservationDTO.getUserId());
                log.info("后端查询VIP折扣: userId={}, vipDiscount={}", reservationDTO.getUserId(), vipDiscount);
                if (vipDiscount != null && vipDiscount.compareTo(BigDecimal.ONE) < 0) {
                    vipDiscountRate = vipDiscount;
                    vipDiscountAmount = basePrice.multiply(BigDecimal.ONE.subtract(vipDiscount))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    basePrice = basePrice.subtract(vipDiscountAmount);
                }
            } catch (Exception e) {
                log.warn("获取VIP折扣失败: userId={}", reservationDTO.getUserId(), e);
            }
        }

        // 第二步：处理优惠券（基于VIP折后价再折扣）
        boolean couponValid = false;
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        if (reservationDTO.getUserCouponId() != null) {
            try {
                BigDecimal discount = couponService.calculateDiscount(
                    reservationDTO.getUserCouponId(), basePrice);
                if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                    couponDiscountAmount = discount;
                    reservation.setCouponId(reservationDTO.getUserCouponId());
                    couponValid = true;
                    log.info("订单使用优惠券，userCouponId={}, 优惠金额: {}", reservationDTO.getUserCouponId(), discount);
                } else {
                    log.warn("优惠券折扣金额为0，不使用优惠券, userCouponId={}", reservationDTO.getUserCouponId());
                }
            } catch (Exception e) {
                log.error("计算优惠券折扣失败, userCouponId={}", reservationDTO.getUserCouponId(), e);
                reservation.setCouponId(null);
            }
        }

        // 第三步：汇总折扣，计算最终实付金额
        BigDecimal totalDiscount = vipDiscountAmount.add(couponDiscountAmount);
        BigDecimal actualAmount = reservation.getTotalPrice().subtract(totalDiscount);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) actualAmount = BigDecimal.ZERO;
        reservation.setDiscountAmount(totalDiscount);
        reservation.setActualAmount(actualAmount);
        reservation.setVipDiscountAmount(vipDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? vipDiscountAmount : null);
        reservation.setVipDiscount(vipDiscountRate);
        log.info("订单金额汇总: totalPrice={}, vipRate={}, vipAmt={}, couponAmt={}, actualAmount={}",
                reservation.getTotalPrice(), vipDiscountRate, vipDiscountAmount, couponDiscountAmount, actualAmount);
        
        // 保存预约
        reservationMapper.insert(reservation);
        
        // 使用优惠券（只有优惠券有效时才扣减，如果失败则回滚整个事务）
        if (couponValid && reservationDTO.getUserCouponId() != null) {
            log.info("开始扣减优惠券: userCouponId={}, reservationId={}", reservationDTO.getUserCouponId(), reservation.getId());
            try {
                couponService.useCoupon(reservationDTO.getUserCouponId(), reservation.getId());
                log.info("优惠券扣减成功: userCouponId={}, reservationId={}", reservationDTO.getUserCouponId(), reservation.getId());
            } catch (Exception e) {
                log.error("优惠券扣减失败: userCouponId={}, reservationId={}, error={}", 
                    reservationDTO.getUserCouponId(), reservation.getId(), e.getMessage(), e);
                throw new RuntimeException("使用优惠券失败: " + e.getMessage(), e);
            }
        } else {
            log.info("不使用优惠券: couponValid={}, userCouponId={}", couponValid, reservationDTO.getUserCouponId());
        }
        
        // 检查是否需要自动创建拼单（预约人数少于剧本所需人数）
        try {
            createGroupOrderIfNeeded(reservation, reservationDTO);
        } catch (Exception e) {
            log.error("自动创建拼单失败", e);
            // 拼单创建失败不影响预约
        }
        
        // 发送预约成功通知
        try {
            sendReservationSuccessNotification(reservation);
        } catch (Exception e) {
            log.error("发送预约成功通知失败", e);
        }
        
        return reservation;
    }
    
    /**
     * 检查并创建拼单（当预约人数少于剧本所需人数时）
     */
    private void createGroupOrderIfNeeded(Reservation reservation, ReservationDTO reservationDTO) {
        if (groupOrderService == null || scriptService == null) {
            log.warn("groupOrderService或scriptService未配置，无法自动创建拼单");
            return;
        }
        
        // 获取剧本信息
        com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
        if (script == null) {
            log.warn("剧本不存在，无法创建拼单: scriptId={}", reservation.getScriptId());
            return;
        }
        
        Integer scriptPlayerCount = script.getPlayerCount();
        Integer reservationPlayerCount = reservation.getPlayerCount();
        
        if (scriptPlayerCount == null || reservationPlayerCount == null) {
            return;
        }
        
        // 如果预约人数少于剧本所需人数，自动创建拼单
        if (reservationPlayerCount < scriptPlayerCount) {
            log.info("预约人数({})少于剧本所需人数({})，自动创建拼单", reservationPlayerCount, scriptPlayerCount);
            
            // 获取门店信息
            String storeName = "";
            if (storeService != null && reservation.getStoreId() != null) {
                try {
                    com.murder.vo.StoreVO store = storeService.getById(reservation.getStoreId());
                    if (store != null) {
                        storeName = store.getName();
                    }
                } catch (Exception e) {
                    log.warn("获取门店信息失败", e);
                }
            }
            
            // 创建拼单
            com.murder.entity.GroupOrder groupOrder = new com.murder.entity.GroupOrder();
            groupOrder.setScriptId(reservation.getScriptId());
            groupOrder.setScriptName(script.getName());
            groupOrder.setStoreId(reservation.getStoreId());
            groupOrder.setStoreName(storeName);
            groupOrder.setPlayTime(reservation.getReservationTime());
            groupOrder.setCurrentCount(reservationPlayerCount);
            groupOrder.setNeedCount(scriptPlayerCount);
            groupOrder.setPlayerCount(scriptPlayerCount);
            groupOrder.setPrice(script.getPrice());
            groupOrder.setNewbieWelcome(1); // 默认新手友好
            groupOrder.setDescription("预约自动发起的拼单，还差" + (scriptPlayerCount - reservationPlayerCount) + "位玩家！");
            groupOrder.setReservationId(reservation.getId());
            
            // 创建拼单
            groupOrderService.createGroup(groupOrder, reservation.getUserId());
            
            log.info("自动创建拼单成功: reservationId={}, scriptName={}, need={}", 
                    reservation.getId(), script.getName(), scriptPlayerCount - reservationPlayerCount);
        }
    }

    /**
     * 分页查询预约列表
     */
    @Override
    public PageResult<Reservation> pageQuery(Integer page, Integer pageSize, Long userId, Integer status) {
        Page<Reservation> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Reservation::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Reservation::getStatus, status);
        }
        wrapper.orderByDesc(Reservation::getCreateTime);
        
        // 手动查询总数
        Long total = reservationMapper.selectCount(wrapper);
        
        reservationMapper.selectPage(pageInfo, wrapper);
        
        // 使用手动查询?total ?
        return new PageResult<>(total, pageInfo.getRecords());
    }
    
    /**
     * 分页查询预约列表（包含关联信息）
     */
    @Override
    public PageResult<com.murder.vo.ReservationVO> pageQueryWithDetails(Integer page, Integer pageSize, Long userId, Long storeId, Integer status, Integer refundStatus, Boolean hasRefund) {
        Page<Reservation> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Reservation::getUserId, userId);
        }
        if (storeId != null) {
            wrapper.eq(Reservation::getStoreId, storeId);
        }
        if (status != null) {
            wrapper.eq(Reservation::getStatus, status);
        }
        if (refundStatus != null) {
            wrapper.eq(Reservation::getRefundStatus, refundStatus);
        }
        if (hasRefund != null && hasRefund) {
            // 查询有退款申请的订单（refundStatus > 0?
            wrapper.gt(Reservation::getRefundStatus, 0);
        }
        wrapper.orderByDesc(Reservation::getCreateTime);
        
        // 手动查询总数
        Long total = reservationMapper.selectCount(wrapper);
        
        reservationMapper.selectPage(pageInfo, wrapper);
        
        // 转换为VO列表
        java.util.List<com.murder.vo.ReservationVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList());
        
        return new PageResult<>(total, voList);
    }
    
    /**
     * 将Reservation转换为ReservationVO（不查询关联信息，仅基础转换?
     */
    private com.murder.vo.ReservationVO convertToVO(Reservation reservation) {
        com.murder.vo.ReservationVO vo = new com.murder.vo.ReservationVO();
        org.springframework.beans.BeanUtils.copyProperties(reservation, vo);

        // 查询关联信息
        try {
            // 查询剧本名称
            if (reservation.getScriptId() != null && scriptService != null) {
                try {
                    com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
                    if (script != null) {
                        vo.setScriptName(script.getName());
                    }
                } catch (Exception e) {
                    log.debug("查询剧本信息失败: {}", e.getMessage());
                }
            }

            // 查询门店名称
            if (reservation.getStoreId() != null && storeService != null) {
                try {
                    com.murder.vo.StoreVO store = storeService.getById(reservation.getStoreId());
                    if (store != null) {
                        vo.setStoreName(store.getName());
                    }
                } catch (Exception e) {
                    log.debug("查询门店信息失败: {}", e.getMessage());
                }
            }

            // 查询房间名称和容量（单体版本：直接查询本地 store_room 表）
            if (reservation.getRoomId() != null && storeRoomService != null) {
                try {
                    com.murder.entity.StoreRoom room = storeRoomService.getById(reservation.getRoomId());
                    if (room != null) {
                        vo.setRoomName(room.getName());
                        vo.setRoomCapacity(room.getCapacity());
                    }
                } catch (Exception e) {
                    log.debug("查询房间信息失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("查询关联信息时出现异常: {}", e.getMessage());
        }

        // 检查是否已评价
        try {
            if (reviewService != null && reservation.getId() != null) {
                com.murder.entity.Review review = reviewService.getByReservationId(reservation.getId());
                vo.setHasReviewed(review != null ? 1 : 0);
            } else {
                vo.setHasReviewed(0);
            }
        } catch (Exception e) {
            log.debug("检查评价状态失? {}", e.getMessage());
            vo.setHasReviewed(0);
        }
        
        return vo;
    }

    /**
     * 根据ID查询预约详情
     */
    @Override
    @org.springframework.cache.annotation.Cacheable(value = "reservation:detail", key = "#id", unless = "#result == null")
    public Reservation getById(Long id) {
        return reservationMapper.selectById(id);
    }
    
    /**
     * 根据ID查询预约详情VO（包含关联信息）
     */
    @Override
    public com.murder.vo.ReservationVO getDetailById(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return null;
        }

        // 管理端门店越权校验（用户端不限制）
        assertAdminStoreScope(reservation);
        
        com.murder.vo.ReservationVO vo = new com.murder.vo.ReservationVO();
        org.springframework.beans.BeanUtils.copyProperties(reservation, vo);
        
        // 查询门店信息
        if (reservation.getStoreId() != null && storeService != null) {
            try {
                com.murder.vo.StoreVO store = storeService.getById(reservation.getStoreId());
                if (store != null) {
                    vo.setStoreName(store.getName());
                    vo.setStoreAddress(store.getAddress());
                    vo.setStorePhone(store.getPhone());
                }
            } catch (Exception e) {
                log.error("查询门店信息失败: {}", e.getMessage());
            }
        }
        
        // 查询剧本信息
        if (reservation.getScriptId() != null && scriptService != null) {
            try {
                com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
                if (script != null) {
                    vo.setScriptName(script.getName());
                    vo.setScriptCover(script.getCover());
                }
            } catch (Exception e) {
                log.error("查询剧本信息失败: {}", e.getMessage());
            }
        }
        
        // 查询房间信息（单体版本：直接查询本地 store_room 表）
        if (reservation.getRoomId() != null && storeRoomService != null) {
            try {
                com.murder.entity.StoreRoom room = storeRoomService.getById(reservation.getRoomId());
                if (room != null) {
                    vo.setRoomName(room.getName());
                    vo.setRoomCapacity(room.getCapacity());
                }
            } catch (Exception e) {
                log.error("查询房间信息失败: {}", e.getMessage());
            }
        }
        
        // 检查是否已评价
        try {
            if (reviewService != null && reservation.getId() != null) {
                com.murder.entity.Review review = reviewService.getByReservationId(reservation.getId());
                vo.setHasReviewed(review != null ? 1 : 0);
            } else {
                vo.setHasReviewed(0);
            }
        } catch (Exception e) {
            log.debug("检查评价状态失? {}", e.getMessage());
            vo.setHasReviewed(0);
        }
        
        return vo;
    }

    /**
     * 根据预约编号查询预约详情
     */
    @Override
    public Reservation getByReservationNo(String reservationNo) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getOrderNo, reservationNo);
        return reservationMapper.selectOne(wrapper);
    }

    /**
     * 确认预约
     */
    @Override
    public void confirm(Long id) {
        Reservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(existing);

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(2); // 已确认
        reservationMapper.updateById(reservation);
    }

    /**
     * 取消预约
     */
    @Override
    public void cancel(Long id, String reason) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(reservation);
        
        // 退还优惠券
        if (reservation.getCouponId() != null) {
            try {
                refundCoupon(id);
                log.info("订单取消，退还优惠券");
            } catch (Exception e) {
                log.error("退还优惠券失败", e);
            }
        }
        
        reservation.setStatus(4); // 已取?
        reservation.setRemark(reason);
        reservationMapper.updateById(reservation);
    }

    /**
     * 完成预约
     */
    @Override
    public void complete(Long id) {
        // 查询预约信息
        Reservation existingReservation = reservationMapper.selectById(id);
        if (existingReservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(existingReservation);
        
        // 更新预约状态
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(3); // 已完?
        reservationMapper.updateById(reservation);
        
        log.info("预约订单已完? id={}, orderNo={}", id, existingReservation.getOrderNo());
        
        // 注意：支付时已经奖励?00积分，这里不再重复奖?
        // 如果需要完成时再奖励，取消下面的注?
        /*
        // 增加积分奖励（完成预约获?00积分?
        try {
            addPointsForReservation(existingReservation.getUserId(), id);
        } catch (Exception e) {
            log.error("完成预约增加积分失败", e);
            // 积分失败不影响预约完?
        }
        */
    }
    
    /**
     * 获取可以完成的预约列表（预约时间+时长已过的已确认订单?
     */
    @Override
    public List<Reservation> getCompletableReservations() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询已确认的预约
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, 2); // 已确?
        wrapper.le(Reservation::getReservationTime, now); // 预约时间小于等于当前时间
        
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        
        // 过滤出预约时?时长已过的订?
        List<Reservation> completableList = new java.util.ArrayList<>();
        for (Reservation reservation : reservations) {
            // 将BigDecimal转换为double，然后向上取?
            double durationHours = reservation.getDuration() != null 
                    ? reservation.getDuration().doubleValue() 
                    : 3.0;
            LocalDateTime endTime = reservation.getReservationTime()
                    .plusHours((long) Math.ceil(durationHours));
            if (endTime.isBefore(now)) {
                completableList.add(reservation);
            }
        }
        
        log.info("查询到{}个可完成的预约订单", completableList.size());
        return completableList;
    }
    
    /**
     * 获取超时未支付的预约列表
     */
    @Override
    public List<Reservation> getUnpaidReservations(LocalDateTime timeoutTime) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getPayStatus, 0); // 未支?
        wrapper.in(Reservation::getStatus, 0, 1); // 待支付或待确?
        wrapper.lt(Reservation::getCreateTime, timeoutTime); // 创建时间早于超时时间
        
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        log.info("查询到{}个超时未支付的预约订单", reservations.size());
        return reservations;
    }

    /**
     * 支付预约
     */
    @Override
    public void pay(Long id) {
        Reservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(existing);

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setPayStatus(1); // 已支付
        reservation.setPayTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);
    }
    
    /**
     * 查询即将开始的预约（指定小时数内）
     */
    @Override
    public List<Reservation> getUpcomingReservations(Integer hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(hours);
        return reservationMapper.selectUpcomingReservations(now, endTime);
    }
    
    /**
     * 检查房间可用?
     */
    @Override
    public boolean checkRoomAvailability(Long roomId, String reservationTime, Double duration) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(reservationTime, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            
            // 计算结束时间（向上取整小时数?
            long durationHours = (long) Math.ceil(duration);
            LocalDateTime endTime = startTime.plusHours(durationHours);
            
            // 查询是否有冲突的预约
            int conflictCount = reservationMapper.countConflictingReservations(roomId, startTime, endTime);
            
            return conflictCount == 0;
        } catch (Exception e) {
            log.error("检查房间可用性失败", e);
            return false;
        }
    }

    /**
     * 生成预约编号
     */
    private String generateReservationNo() {
        // 使用时间?随机数确保唯一?
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int)(Math.random() * 10000);
        String randomPart = String.format("%04d", random);
        return "R" + datePart + randomPart;
    }
    
    
    /**
     * 增加预约积分奖励
     */
    private void addPointsForReservation(Long userId, Long reservationId) {
        if (userPointsService == null) {
            log.warn("userPointsService未配置，无法调用积分服务");
            return;
        }
        
        try {
            userPointsService.rewardForReservation(userId, reservationId);
            log.info("用户{}完成预约{}，获?00积分", userId, reservationId);
        } catch (Exception e) {
            log.error("调用积分服务失败", e);
            throw e;
        }
    }
    
    
    /**
     * 退还优惠券
     */
    private void refundCoupon(Long orderId) {
        try {
            couponService.refundCoupon(orderId);
            log.info("优惠券退还成功, orderId={}", orderId);
        } catch (Exception e) {
            log.error("退还优惠券失败", e);
            throw new RuntimeException("退还优惠券失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送预约成功通知
     */
    private void sendReservationSuccessNotification(Reservation reservation) {
        try {
            // 1) 发送通知给用户（单体版本：直接调用本地 NotificationService，落库 + WebSocket 实时推送）
            String title = "预约成功通知";
            String content = String.format("您已成功预约，预约编号：%s，预约时间：%s，请准时到场",
                    reservation.getOrderNo(),
                    reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            notificationService.sendToUsers(
                    title,
                    content,
                    1, // 预约成功
                    "reservation",
                    reservation.getId(),
                    reservation.getUserId()
            );

            log.info("发送预约成功通知给用户(本地): userId={}, reservationId={}", reservation.getUserId(), reservation.getId());

            // 2) 发送管理端通知
            sendAdminNotification(reservation);
        } catch (Exception e) {
            log.error("发送预约成功通知失败", e);
        }
    }
    
    /**
     * 管理端门店数据范围校验：
     * - 仅对 X-Client-Type=admin 且 role=STORE_ADMIN 生效
     * - SUPER_ADMIN 不限制
     */
    private void assertAdminStoreScope(Reservation reservation) {
        try {
            org.springframework.web.context.request.RequestAttributes ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (!(ra instanceof org.springframework.web.context.request.ServletRequestAttributes attrs)) {
                return;
            }
            String clientType = attrs.getRequest().getHeader("X-Client-Type");
            if (!"admin".equals(clientType)) {
                return;
            }
            String role = com.murder.common.context.BaseContext.getRole();
            if (!"STORE_ADMIN".equals(role)) {
                return;
            }
            Long ctxStoreId = com.murder.common.context.BaseContext.getStoreId();
            if (ctxStoreId == null) {
                throw new RuntimeException("门店管理员未绑定门店(storeId)");
            }
            if (reservation.getStoreId() == null || !ctxStoreId.equals(reservation.getStoreId())) {
                throw new RuntimeException("没有权限操作该门店的预约");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // 出现异常时按安全策略拒绝
            throw new RuntimeException("权限校验失败");
        }
    }

    /**
     * 发送管理端通知
     */
    private void sendAdminNotification(Reservation reservation) {
        try {
            String adminTitle = "新预约提醒";
            String adminContent = String.format("收到新预约！预约编号：%s，预约时间：%s，人数：%d",
                    reservation.getOrderNo(),
                    reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    reservation.getPlayerCount() != null ? reservation.getPlayerCount() : 0);

            // 单体版本：直接调用本地 AdminNotificationService
            adminNotificationService.sendNotification(
                    adminTitle,
                    adminContent,
                    1,
                    "reservation",
                    reservation.getId(),
                    reservation.getStoreId(),
                    2
            );

            log.info("发送管理端通知成功(本地): reservationId={}", reservation.getId());
        } catch (Exception e) {
            log.error("发送管理端通知失败", e);
        }
    }

}
