package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ReservationDTO;
import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.entity.ScriptSchedule;
import com.murder.mapper.ReservationMapper;
import com.murder.service.AdminNotificationService;
import com.murder.service.CouponService;
import com.murder.service.NotificationService;
import com.murder.service.PaymentService;
import com.murder.service.ReservationService;
import com.murder.service.ScriptScheduleService;
import com.murder.service.StoreEmployeeOperationLogService;
import com.murder.service.StoreRoomService;
import com.murder.vo.ReservationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private static final DateTimeFormatter RESERVATION_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter RESERVATION_TIME_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    private ScriptScheduleService scriptScheduleService;

    @Autowired(required = false)
    private com.murder.service.VipService vipService;

    @Autowired(required = false)
    private com.murder.mapper.DmMapper dmMapper;

    @Autowired(required = false)
    private com.murder.mapper.GroupMemberMapper groupMemberMapper;

    @Autowired(required = false)
    private com.murder.mapper.ScriptScheduleMapper scriptScheduleMapper;

    @Autowired(required = false)
    private PaymentService paymentService;

    @Autowired(required = false)
    private StoreEmployeeOperationLogService storeEmployeeOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation create(ReservationDTO reservationDTO) {
        validateScheduleBeforeCreate(reservationDTO);

        if (reservationDTO.getRoomId() != null && reservationDTO.getReservationTime() != null) {
            String reservationTimeStr = reservationDTO.getReservationTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Double requestedDuration = reservationDTO.getDuration() != null
                    ? reservationDTO.getDuration().doubleValue()
                    : 3.0;
            boolean isAvailable = checkRoomAvailability(reservationDTO.getRoomId(), reservationTimeStr, requestedDuration);
            if (!isAvailable) {
                throw new RuntimeException("该时段房间已被预约，请选择其他时间");
            }
        }

        Reservation reservation = new Reservation();
        BeanUtils.copyProperties(reservationDTO, reservation);
        reservation.setOrderNo(generateReservationNo());
        reservation.setStatus(1);
        reservation.setPayStatus(0);
        reservation.setCheckInStatus(0);

        BigDecimal totalPrice = calculateReservationTotalPrice(reservation, reservationDTO.getTotalPrice());
        BigDecimal vipDiscountAmount = BigDecimal.ZERO;
        BigDecimal vipDiscountRate = null;
        BigDecimal basePrice = totalPrice;

        if (reservationDTO.getVipDiscountAmount() != null
                && reservationDTO.getVipDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && reservationDTO.getVipDiscount() != null) {
            BigDecimal expectedDiscount = null;
            if (vipService != null && reservationDTO.getUserId() != null) {
                expectedDiscount = vipService.getVipDiscount(reservationDTO.getUserId());
            }
            if (expectedDiscount != null
                    && reservationDTO.getVipDiscount().subtract(expectedDiscount).abs()
                    .compareTo(new BigDecimal("0.01")) <= 0) {
                vipDiscountRate = expectedDiscount;
                vipDiscountAmount = totalPrice
                        .multiply(BigDecimal.ONE.subtract(vipDiscountRate))
                        .setScale(2, RoundingMode.HALF_UP);
                basePrice = totalPrice.subtract(vipDiscountAmount);
            }
        } else if (vipService != null && reservationDTO.getUserId() != null) {
            try {
                BigDecimal vipDiscount = vipService.getVipDiscount(reservationDTO.getUserId());
                if (vipDiscount != null && vipDiscount.compareTo(BigDecimal.ONE) < 0) {
                    vipDiscountRate = vipDiscount;
                    vipDiscountAmount = totalPrice
                            .multiply(BigDecimal.ONE.subtract(vipDiscount))
                            .setScale(2, RoundingMode.HALF_UP);
                    basePrice = totalPrice.subtract(vipDiscountAmount);
                }
            } catch (Exception e) {
                log.warn("获取VIP折扣失败: userId={}", reservationDTO.getUserId(), e);
            }
        }

        boolean couponValid = false;
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        if (reservationDTO.getUserCouponId() != null) {
            try {
                BigDecimal discount = couponService.calculateDiscount(reservationDTO.getUserCouponId(), basePrice);
                if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                    couponDiscountAmount = discount;
                    reservation.setCouponId(reservationDTO.getUserCouponId());
                    couponValid = true;
                }
            } catch (Exception e) {
                log.error("计算优惠券失败: userCouponId={}", reservationDTO.getUserCouponId(), e);
                reservation.setCouponId(null);
            }
        }

        BigDecimal totalDiscount = vipDiscountAmount.add(couponDiscountAmount);
        BigDecimal actualAmount = totalPrice.subtract(totalDiscount);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualAmount = BigDecimal.ZERO;
        }

        reservation.setTotalPrice(totalPrice);
        reservation.setDiscountAmount(totalDiscount);
        reservation.setActualAmount(actualAmount);
        reservation.setVipDiscountAmount(vipDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? vipDiscountAmount : null);
        reservation.setVipDiscount(vipDiscountRate);

        reservationMapper.insert(reservation);

        // 同步更新排期的已预约人数（scheduleId 由前端传入）
        if (reservationDTO.getScheduleId() != null && scriptScheduleService != null) {
            try {
                scriptScheduleService.incrementCurrentPlayers(
                        reservationDTO.getScheduleId(),
                        reservation.getPlayerCount() == null ? 1 : reservation.getPlayerCount());
            } catch (Exception e) {
                log.warn("更新排期人数失败: scheduleId={}", reservationDTO.getScheduleId(), e);
            }
        }

        if (couponValid && reservationDTO.getUserCouponId() != null) {
            couponService.useCoupon(reservationDTO.getUserCouponId(), reservation.getId());
        }

        GroupOrder autoCreatedGroup = createGroupOrderIfNeeded(reservation);
        if (autoCreatedGroup != null) {
            reservation.setGroupId(autoCreatedGroup.getId());
        }

        try {
            sendReservationSuccessNotification(reservation);
        } catch (Exception e) {
            log.error("发送预约成功通知失败: reservationId={}", reservation.getId(), e);
        }

        return reservation;
    }

    private void validateScheduleBeforeCreate(ReservationDTO reservationDTO) {
        if (reservationDTO == null || reservationDTO.getScheduleId() == null || scriptScheduleService == null) {
            return;
        }

        ScriptSchedule schedule = scriptScheduleService.getById(reservationDTO.getScheduleId());
        if (schedule == null || Integer.valueOf(1).equals(schedule.getIsDeleted())) {
            throw new RuntimeException("所选场次不存在，请重新选择");
        }

        if (hasScheduleStarted(schedule)) {
            closeExpiredSchedule(schedule);
            throw new RuntimeException("该场次已开始，请选择其他场次");
        }

        if (Integer.valueOf(2).equals(schedule.getStatus())) {
            throw new RuntimeException("该场次已关闭，请选择其他场次");
        }

        int maxPlayers = schedule.getMaxPlayers() == null ? 0 : schedule.getMaxPlayers();
        int currentPlayers = schedule.getCurrentPlayers() == null ? 0 : schedule.getCurrentPlayers();
        int requestedPlayers = reservationDTO.getPlayerCount() == null ? 1 : reservationDTO.getPlayerCount();

        if (Integer.valueOf(0).equals(schedule.getStatus()) || (maxPlayers > 0 && currentPlayers >= maxPlayers)) {
            throw new RuntimeException("该场次已满，请选择其他场次");
        }

        if (maxPlayers > 0 && currentPlayers + requestedPlayers > maxPlayers) {
            throw new RuntimeException("该场次余位不足，请选择其他场次");
        }
    }

    private GroupOrder createGroupOrderIfNeeded(Reservation reservation) {
        if (groupOrderService == null || scriptService == null) {
            return null;
        }

        com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
        if (script == null || script.getPlayerCount() == null || reservation.getPlayerCount() == null) {
            return null;
        }

        if (reservation.getPlayerCount() >= script.getPlayerCount()) {
            return null;
        }

        if (reservation.getScheduleId() != null && scriptScheduleService != null) {
            ScriptSchedule schedule = scriptScheduleService.getById(reservation.getScheduleId());
            if (schedule != null) {
                int currentPlayers = schedule.getCurrentPlayers() == null ? 0 : schedule.getCurrentPlayers();
                if (currentPlayers >= script.getPlayerCount()) {
                    return null;
                }
            }
        }

        String storeName = "";
        if (storeService != null && reservation.getStoreId() != null) {
            try {
                com.murder.vo.StoreVO store = storeService.getById(reservation.getStoreId());
                if (store != null) {
                    storeName = store.getName();
                }
            } catch (Exception e) {
                log.warn("获取门店信息失败: storeId={}", reservation.getStoreId(), e);
            }
        }

        com.murder.entity.GroupOrder groupOrder = new com.murder.entity.GroupOrder();
        groupOrder.setScriptId(reservation.getScriptId());
        groupOrder.setScriptName(script.getName());
        groupOrder.setStoreId(reservation.getStoreId());
        groupOrder.setStoreName(storeName);
        groupOrder.setPlayTime(reservation.getReservationTime());
        groupOrder.setCurrentCount(reservation.getPlayerCount());
        groupOrder.setNeedCount(script.getPlayerCount());
        groupOrder.setPlayerCount(script.getPlayerCount());
        groupOrder.setPrice(script.getPrice());
        groupOrder.setNewbieWelcome(1);
        groupOrder.setDescription("预约自动发起的拼团，还差" + (script.getPlayerCount() - reservation.getPlayerCount()) + "位玩家");
        groupOrder.setReservationId(reservation.getId());
        return groupOrderService.createOrAttachAutoGroup(groupOrder, reservation, reservation.getUserId());
    }

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

        Long total = reservationMapper.selectCount(wrapper);
        reservationMapper.selectPage(pageInfo, wrapper);
        return new PageResult<>(total, pageInfo.getRecords());
    }

    @Override
    public PageResult<ReservationVO> pageQueryWithDetails(
            Integer page,
            Integer pageSize,
            Long userId,
            Long storeId,
            Long scheduleId,
            LocalDate reservationDate,
            Integer status,
            Integer payStatus,
            Integer checkInStatus,
            Integer refundStatus,
            Boolean hasRefund
    ) {
        Page<Reservation> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Reservation::getUserId, userId);
        }
        if (storeId != null) {
            wrapper.eq(Reservation::getStoreId, storeId);
        }
        if (scheduleId != null) {
            wrapper.eq(Reservation::getScheduleId, scheduleId);
        }
        applyStaffReservationScope(wrapper);
        if (reservationDate != null) {
            LocalDateTime startOfDay = reservationDate.atStartOfDay();
            LocalDateTime endOfDay = reservationDate.plusDays(1).atStartOfDay();
            wrapper.ge(Reservation::getReservationTime, startOfDay)
                    .lt(Reservation::getReservationTime, endOfDay);
        }
        if (status != null) {
            wrapper.eq(Reservation::getStatus, status);
        }
        if (payStatus != null) {
            wrapper.eq(Reservation::getPayStatus, payStatus);
        }
        if (checkInStatus != null) {
            wrapper.eq(Reservation::getCheckInStatus, checkInStatus);
        }
        if (refundStatus != null) {
            wrapper.eq(Reservation::getRefundStatus, refundStatus);
        }
        if (Boolean.TRUE.equals(hasRefund)) {
            wrapper.gt(Reservation::getRefundStatus, 0);
        }
        if (reservationDate != null) {
            wrapper.orderByAsc(Reservation::getReservationTime)
                    .orderByDesc(Reservation::getCreateTime);
        } else {
            wrapper.orderByDesc(Reservation::getCreateTime);
        }

        Long total = reservationMapper.selectCount(wrapper);
        reservationMapper.selectPage(pageInfo, wrapper);

        List<ReservationVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(total, voList);
    }

    private ReservationVO convertToVO(Reservation reservation) {
        ensureCheckInFields(reservation);

        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);

        populateStoreInfo(reservation, vo, false);
        populateScriptInfo(reservation, vo);
        populateRoomInfo(reservation, vo);
        populateReviewState(reservation, vo);
        populateDmInfo(reservation, vo);
        populateDmAssignmentState(reservation, vo);
        return vo;
    }

    @Override
    @Cacheable(value = "reservation:detail", key = "#id", unless = "#result == null")
    public Reservation getById(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        ensureCheckInFields(reservation);
        return reservation;
    }

    @Override
    public ReservationVO getDetailById(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return null;
        }

        assertOwnerOrViewScope(reservation);
        ensureCheckInFields(reservation);

        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        populateStoreInfo(reservation, vo, true);
        populateScriptInfo(reservation, vo);
        populateRoomInfo(reservation, vo);
        populateReviewState(reservation, vo);
        populateDmInfo(reservation, vo);
        populateDmAssignmentState(reservation, vo);
        return vo;
    }

    @Override
    public Reservation getByReservationNo(String reservationNo) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getOrderNo, reservationNo);
        Reservation reservation = reservationMapper.selectOne(wrapper);
        ensureCheckInFields(reservation);
        return reservation;
    }

    @Override
    public void confirm(Long id) {
        Reservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(existing);
        ensureCheckInFields(existing);

        if (existing.getStatus() != null && existing.getStatus() == 4) {
            throw new RuntimeException("已取消预约不能再次确认");
        }
        if (existing.getStatus() != null && existing.getStatus() == 3) {
            throw new RuntimeException("已完成预约不能再次确认");
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus(2);
        reservationMapper.updateById(update);
    }

    @Override
    public void cancel(Long id, String reason) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertOwnerOrAdminScope(reservation);
        ensureCheckInFields(reservation);

        if (Integer.valueOf(3).equals(reservation.getStatus())) {
            throw new RuntimeException("已完成预约不能取消");
        }
        if (Integer.valueOf(4).equals(reservation.getStatus())) {
            throw new RuntimeException("预约已取消");
        }
        if (Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new RuntimeException("预约已核销，不能取消");
        }
        if (Integer.valueOf(1).equals(reservation.getRefundStatus())) {
            throw new RuntimeException("订单退款处理中，请勿重复取消");
        }
        if (Integer.valueOf(2).equals(reservation.getRefundStatus())
                || Integer.valueOf(3).equals(reservation.getPayStatus())) {
            throw new RuntimeException("订单已退款，无需重复取消");
        }

        String cancelReason = StringUtils.hasText(reason) ? reason : "取消预约";

        if (Integer.valueOf(1).equals(reservation.getPayStatus())) {
            if (paymentService == null) {
                throw new RuntimeException("支付服务未启用，暂无法取消已支付预约");
            }
            paymentService.autoRefund(id, cancelReason);
            return;
        }

        if (reservation.getCouponId() != null) {
            try {
                refundCoupon(id);
            } catch (Exception e) {
                log.error("退回优惠券失败: reservationId={}", id, e);
            }
        }

        // 回退排期已预约人数
        if (reservation.getScheduleId() != null && scriptScheduleService != null) {
            try {
                scriptScheduleService.decrementCurrentPlayers(
                        reservation.getScheduleId(),
                        reservation.getPlayerCount() == null ? 1 : reservation.getPlayerCount());
            } catch (Exception e) {
                log.warn("回退排期人数失败: scheduleId={}", reservation.getScheduleId(), e);
            }
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus(4);
        update.setRemark(cancelReason);
        reservationMapper.updateById(update);
    }

    @Override
    public void complete(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertCompleteScope(reservation);
        ensureCheckInFields(reservation);

        if (!Integer.valueOf(2).equals(reservation.getStatus())) {
            throw new RuntimeException("只有已确认预约才能完成");
        }
        if (!Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new RuntimeException("预约尚未核销，不能直接完成");
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus(3);
        update.setCheckInStatus(1);
        update.setCheckInTime(reservation.getCheckInTime() != null ? reservation.getCheckInTime() : LocalDateTime.now());
        reservationMapper.updateById(update);
        recordReservationOperation("RESERVATION_COMPLETE", reservation, "完成预约");

        log.info("预约已完成: id={}, orderNo={}", reservation.getId(), reservation.getOrderNo());
    }

    @Override
    public void checkIn(Long id, String checkInCode) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertCheckInScope(reservation);
        ensureCheckInFields(reservation);

        if (!Integer.valueOf(2).equals(reservation.getStatus())) {
            throw new RuntimeException("只有已确认预约才能核销");
        }
        if (!Integer.valueOf(1).equals(reservation.getPayStatus())) {
            throw new RuntimeException("订单未支付，不能核销");
        }
        if (Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new RuntimeException("该预约已完成核销");
        }
        if (!StringUtils.hasText(checkInCode)) {
            throw new RuntimeException("请输入核销码");
        }
        if (!checkInCode.trim().equals(reservation.getCheckInCode())) {
            throw new RuntimeException("核销码不正确");
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setCheckInStatus(1);
        update.setCheckInTime(LocalDateTime.now());
        reservationMapper.updateById(update);
        recordReservationOperation("RESERVATION_CHECKIN", reservation, "到店核销");
    }

    @Override
    public List<Reservation> getCompletableReservations() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, 2);
        wrapper.eq(Reservation::getCheckInStatus, 1);
        wrapper.le(Reservation::getReservationTime, now);

        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        List<Reservation> completableList = new ArrayList<>();
        for (Reservation reservation : reservations) {
            LocalDateTime endTime = reservation.getReservationTime()
                    .plusMinutes(calculateDurationMinutes(reservation.getDuration()));
            if (endTime.isBefore(now)) {
                completableList.add(reservation);
            }
        }

        return completableList;
    }

    @Override
    public List<Reservation> getUnpaidReservations(LocalDateTime timeoutTime) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getPayStatus, 0);
        wrapper.in(Reservation::getStatus, 0, 1);
        wrapper.lt(Reservation::getCreateTime, timeoutTime);
        return reservationMapper.selectList(wrapper);
    }

    @Override
    public void pay(Long id) {
        Reservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(existing);
        ensureCheckInFields(existing);

        String checkInCode = StringUtils.hasText(existing.getCheckInCode())
                ? existing.getCheckInCode()
                : generateCheckInCode();

        Reservation update = new Reservation();
        update.setId(id);
        update.setPayStatus(1);
        update.setPayTime(LocalDateTime.now());
        update.setStatus(2);
        update.setCheckInStatus(existing.getCheckInStatus());
        update.setCheckInCode(checkInCode);
        reservationMapper.updateById(update);

        existing.setPayStatus(1);
        existing.setPayTime(update.getPayTime());
        existing.setStatus(2);
        existing.setCheckInStatus(update.getCheckInStatus());
        existing.setCheckInCode(checkInCode);

        try {
            sendPaymentSuccessNotification(existing);
        } catch (Exception e) {
            log.warn("发送支付成功通知失败: reservationId={}", id, e);
        }
    }

    @Override
    public List<Reservation> getUpcomingReservations(Integer hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(hours);
        return reservationMapper.selectUpcomingReservations(now, endTime);
    }

    @Override
    public boolean checkRoomAvailability(Long roomId, String reservationTime, Double duration) {
        try {
            LocalDateTime startTime = parseReservationTime(reservationTime);
            LocalDateTime endTime = startTime.plusMinutes(calculateDurationMinutes(duration));
            int conflictCount = reservationMapper.countConflictingReservations(roomId, startTime, endTime);
            return conflictCount == 0;
        } catch (Exception e) {
            log.error("检查房间可用性失败: roomId={}, reservationTime={}, duration={}", roomId, reservationTime, duration, e);
            throw new RuntimeException("检查房间可用性失败，请稍后重试", e);
        }
    }

    private void populateStoreInfo(Reservation reservation, ReservationVO vo, boolean detail) {
        if (reservation.getStoreId() == null || storeService == null) {
            return;
        }
        try {
            com.murder.vo.StoreVO store = storeService.getById(reservation.getStoreId());
            if (store == null) {
                return;
            }
            vo.setStoreName(store.getName());
            if (detail) {
                vo.setStoreAddress(store.getAddress());
                vo.setStorePhone(store.getPhone());
            }
        } catch (Exception e) {
            log.debug("查询门店信息失败: storeId={}", reservation.getStoreId(), e);
        }
    }

    private void populateScriptInfo(Reservation reservation, ReservationVO vo) {
        if (reservation.getScriptId() == null || scriptService == null) {
            return;
        }
        try {
            com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
            if (script == null) {
                return;
            }
            vo.setScriptName(script.getName());
            vo.setScriptCover(script.getCover());
        } catch (Exception e) {
            log.debug("查询剧本信息失败: scriptId={}", reservation.getScriptId(), e);
        }
    }

    private void populateRoomInfo(Reservation reservation, ReservationVO vo) {
        if (reservation.getRoomId() == null || storeRoomService == null) {
            return;
        }
        try {
            com.murder.entity.StoreRoom room = storeRoomService.getById(reservation.getRoomId());
            if (room == null) {
                return;
            }
            vo.setRoomName(room.getName());
            vo.setRoomCapacity(room.getCapacity());
        } catch (Exception e) {
            log.debug("查询房间信息失败: roomId={}", reservation.getRoomId(), e);
        }
    }

    private void populateDmInfo(Reservation reservation, ReservationVO vo) {
        if (dmMapper == null) return;
        Long dmId = reservation.getDmId();
        if (dmId == null) {
            ScriptSchedule schedule = getScheduleForReservation(reservation);
            dmId = schedule != null ? schedule.getDmId() : null;
        }
        if (dmId == null) return;
        try {
            com.murder.entity.Dm dm = dmMapper.selectById(dmId);
            if (dm != null) {
                vo.setDmId(dm.getId());
                vo.setDmName(dm.getName());
                vo.setDmAvatar(dm.getAvatar());
                vo.setDmStyleTags(dm.getStyleTags());
                vo.setDmRating(dm.getRating());
            }
        } catch (Exception e) {
            log.debug("查询 DM 信息失败: dmId={}", dmId, e);
        }
    }

    private void populateReviewState(Reservation reservation, ReservationVO vo) {
        try {
            if (reviewService != null && reservation.getId() != null) {
                com.murder.entity.Review review = reviewService.getByReservationId(reservation.getId());
                vo.setHasReviewed(review != null ? 1 : 0);
            } else {
                vo.setHasReviewed(0);
            }
        } catch (Exception e) {
            log.debug("查询评价状态失败: reservationId={}", reservation.getId(), e);
            vo.setHasReviewed(0);
        }
    }

    private void ensureCheckInFields(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return;
        }

        boolean needUpdate = false;
        if (reservation.getCheckInStatus() == null) {
            reservation.setCheckInStatus(0);
            needUpdate = true;
        }

        if (Integer.valueOf(3).equals(reservation.getStatus())
                && !Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            reservation.setCheckInStatus(1);
            if (reservation.getCheckInTime() == null) {
                reservation.setCheckInTime(
                        reservation.getUpdateTime() != null ? reservation.getUpdateTime() : LocalDateTime.now()
                );
            }
            needUpdate = true;
        }

        if (needUpdate) {
            Reservation update = new Reservation();
            update.setId(reservation.getId());
            update.setCheckInStatus(reservation.getCheckInStatus());
            update.setCheckInTime(reservation.getCheckInTime());
            reservationMapper.updateById(update);
        }
    }

    private String generateReservationNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "R" + datePart + random;
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal calculateReservationTotalPrice(Reservation reservation, BigDecimal clientTotalPrice) {
        if (reservation == null || reservation.getScriptId() == null) {
            throw new RuntimeException("剧本信息缺失，无法计算订单金额");
        }
        if (scriptService == null) {
            throw new RuntimeException("剧本服务未启用，无法计算订单金额");
        }

        com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
        if (script == null || Integer.valueOf(1).equals(script.getIsDeleted())) {
            throw new RuntimeException("剧本不存在，无法创建预约");
        }
        if (script.getPrice() == null || script.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("剧本价格异常，无法创建预约");
        }

        int playerCount = reservation.getPlayerCount() != null && reservation.getPlayerCount() > 0
                ? reservation.getPlayerCount()
                : 1;
        reservation.setPlayerCount(playerCount);

        if (reservation.getDuration() == null && script.getDuration() != null) {
            reservation.setDuration(script.getDuration());
        }

        BigDecimal totalPrice = script.getPrice()
                .multiply(BigDecimal.valueOf(playerCount))
                .setScale(2, RoundingMode.HALF_UP);

        if (clientTotalPrice != null && clientTotalPrice.compareTo(totalPrice) != 0) {
            log.warn("预约金额以服务端重算为准: scriptId={}, playerCount={}, clientTotalPrice={}, serverTotalPrice={}",
                    reservation.getScriptId(), playerCount, clientTotalPrice, totalPrice);
        }

        return totalPrice;
    }

    private long calculateDurationMinutes(BigDecimal duration) {
        return calculateDurationMinutes(duration != null ? duration.doubleValue() : null);
    }

    private long calculateDurationMinutes(Double duration) {
        double safeDuration = duration != null && duration > 0 ? duration : 3.0;
        return (long) Math.ceil(safeDuration * 60);
    }

    private LocalDateTime parseReservationTime(String reservationTime) {
        if (!StringUtils.hasText(reservationTime)) {
            throw new RuntimeException("预约时间不能为空");
        }

        String normalized = reservationTime.trim();
        if (normalized.contains("T")) {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        try {
            return LocalDateTime.parse(normalized, RESERVATION_TIME_SECOND_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(normalized, RESERVATION_TIME_MINUTE_FORMATTER);
        }
    }

    private void refundCoupon(Long orderId) {
        try {
            couponService.refundCoupon(orderId);
        } catch (Exception e) {
            throw new RuntimeException("退回优惠券失败: " + e.getMessage(), e);
        }
    }

    private void sendReservationSuccessNotification(Reservation reservation) {
        String reservationTime = reservation.getReservationTime() == null
                ? ""
                : reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        notificationService.sendToUsers(
                "预约成功通知",
                String.format("您已成功预约，预约编号：%s，预约时间：%s，请准时到场。", reservation.getOrderNo(), reservationTime),
                1,
                "reservation",
                reservation.getId(),
                reservation.getUserId()
        );
        sendAdminNotification(reservation);
    }

    private void sendPaymentSuccessNotification(Reservation reservation) {
        if (reservation == null || notificationService == null) {
            return;
        }

        String reservationTime = reservation.getReservationTime() == null
                ? ""
                : reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        notificationService.sendToUsers(
                "支付成功通知",
                String.format(
                        "您已成功支付预约订单，订单号：%s，金额：%.2f，预约时间：%s，到店请出示核销码：%s。",
                        reservation.getOrderNo(),
                        defaultAmount(reservation.getActualAmount()),
                        reservationTime,
                        reservation.getCheckInCode()
                ),
                3,
                "payment",
                reservation.getId(),
                reservation.getUserId()
        );
    }

    private void assertAdminStoreScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role)) {
            return;
        }
        if (!"STORE_ADMIN".equals(role)) {
            throw new SecurityException("没有权限操作该预约");
        }

        Long currentStoreId = BaseContext.getStoreId();
        if (currentStoreId == null) {
            throw new SecurityException("门店管理员未绑定门店");
        }
        if (reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
            throw new SecurityException("没有权限操作该门店的预约");
        }
    }

    private void assertCheckInScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role)) {
            assertAdminStoreScope(reservation);
            return;
        }

        if (!"STORE_STAFF".equals(role)) {
            throw new SecurityException("没有权限核销该预约");
        }

        Long currentStoreId = BaseContext.getStoreId();
        if (currentStoreId == null || reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
            throw new SecurityException("没有权限核销该门店的预约");
        }

        if (!hasReservationPermission("reservation:checkin")) {
            throw new SecurityException("当前员工账号没有核销权限");
        }
        assertDmAssignedScopeIfNeeded(reservation);
    }

    private void assertCompleteScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role)) {
            assertAdminStoreScope(reservation);
            return;
        }

        if (!"STORE_STAFF".equals(role)) {
            throw new SecurityException("没有权限完成该预约");
        }

        Long currentStoreId = BaseContext.getStoreId();
        if (currentStoreId == null || reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
            throw new SecurityException("没有权限操作该门店的预约");
        }

        if (!hasReservationPermission("reservation:complete")) {
            throw new SecurityException("当前员工账号没有完成预约权限");
        }
        assertDmAssignedScopeIfNeeded(reservation);
        if ("DM".equals(BaseContext.getStaffRole()) && !Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new SecurityException("DM 仅可完成自己已核销的场次");
        }
    }

    private void assertOwnerOrAdminScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null && currentUserId.equals(reservation.getUserId())) {
            return;
        }

        assertAdminStoreScope(reservation);
    }

    private void assertOwnerOrViewScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null && currentUserId.equals(reservation.getUserId())) {
            return;
        }

        String role = BaseContext.getRole();
        if ("STORE_STAFF".equals(role)) {
            Long currentStoreId = BaseContext.getStoreId();
            if (currentStoreId == null || reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
                throw new SecurityException("没有权限查看该门店的预约");
            }
            if (!hasReservationPermission("reservation:view")) {
                throw new SecurityException("当前员工账号没有查看预约权限");
            }
            assertDmAssignedScopeIfNeeded(reservation);
            return;
        }

        assertAdminStoreScope(reservation);
    }

    private boolean hasReservationPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (!StringUtils.hasText(permissionCodes) || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        String[] permissions = permissionCodes.split(",");
        for (String permission : permissions) {
            if (permissionCode.equals(permission != null ? permission.trim() : null)) {
                return true;
            }
        }
        return false;
    }

    private void applyStaffReservationScope(LambdaQueryWrapper<Reservation> wrapper) {
        if (wrapper == null || !"STORE_STAFF".equals(BaseContext.getRole())) {
            return;
        }
        if (!"DM".equals(BaseContext.getStaffRole())) {
            return;
        }

        Long currentDmId = BaseContext.getDmId();
        if (currentDmId == null) {
            wrapper.eq(Reservation::getId, -1L);
            return;
        }

        List<Long> scheduleIds = findScheduleIdsByDmId(currentDmId, BaseContext.getStoreId());
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            wrapper.and(w -> w.eq(Reservation::getDmId, currentDmId));
            return;
        }

        wrapper.and(w -> w.eq(Reservation::getDmId, currentDmId)
                .or()
                .in(Reservation::getScheduleId, scheduleIds));
    }

    private List<Long> findScheduleIdsByDmId(Long dmId, Long storeId) {
        if (dmId == null || scriptScheduleMapper == null) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getDmId, dmId)
                .eq(ScriptSchedule::getIsDeleted, 0);
        if (storeId != null) {
            wrapper.eq(ScriptSchedule::getStoreId, storeId);
        }
        List<ScriptSchedule> schedules = scriptScheduleMapper.selectList(wrapper);
        if (schedules == null || schedules.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return schedules.stream().map(ScriptSchedule::getId).collect(Collectors.toList());
    }

    private void assertDmAssignedScopeIfNeeded(Reservation reservation) {
        if (!"STORE_STAFF".equals(BaseContext.getRole()) || !"DM".equals(BaseContext.getStaffRole())) {
            return;
        }
        Long currentDmId = BaseContext.getDmId();
        if (currentDmId == null) {
            throw new SecurityException("当前 DM 账号未绑定主持人");
        }
        Long reservationDmId = resolveReservationDmId(reservation);
        if (reservationDmId == null || !currentDmId.equals(reservationDmId)) {
            throw new SecurityException("DM 只能操作分配给自己的预约");
        }
    }

    private Long resolveReservationDmId(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        if (reservation.getDmId() != null) {
            return reservation.getDmId();
        }
        ScriptSchedule schedule = getScheduleForReservation(reservation);
        return schedule != null ? schedule.getDmId() : null;
    }

    private void sendAdminNotification(Reservation reservation) {
        String reservationTime = reservation.getReservationTime() == null
                ? ""
                : reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        adminNotificationService.sendNotification(
                "新预约提醒",
                String.format(
                        "收到新预约，订单号：%s，预约时间：%s，人数：%d。",
                        reservation.getOrderNo(),
                        reservationTime,
                        reservation.getPlayerCount() == null ? 0 : reservation.getPlayerCount()
                ),
                1,
                "reservation",
                reservation.getId(),
                reservation.getStoreId(),
                2
        );
    }

    @Override
    public void reschedule(Long id, String newReservationTime) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertOwnerOrAdminScope(reservation);
        ensureCheckInFields(reservation);

        // 只允许待确认/已确认且未核销状态改期
        if (Integer.valueOf(4).equals(reservation.getStatus()) || Integer.valueOf(1).equals(reservation.getCheckInStatus())) {
            throw new RuntimeException("当前状态不支持改期");
        }

        // 新时间不能小于当前时间
        LocalDateTime newTime = LocalDateTime.parse(newReservationTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (newTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("改期时间不能小于当前时间");
        }

        ScriptSchedule targetSchedule = resolveRescheduleTargetSchedule(reservation, newTime);
        if (reservation.getScheduleId() != null && targetSchedule == null) {
            throw new RuntimeException("未找到对应排期，请重新选择可用场次");
        }

        if (targetSchedule != null) {
            validateRescheduleTargetSchedule(reservation, targetSchedule);
        } else {
            validateLegacyRescheduleAvailability(reservation, newTime);
        }

        GroupOrder linkedGroup = reservation.getGroupId() != null && groupOrderService != null
                ? groupOrderService.getById(reservation.getGroupId())
                : null;
        boolean moveCreatorGroup = linkedGroup != null
                && Integer.valueOf(1).equals(linkedGroup.getStatus())
                && reservation.getId().equals(linkedGroup.getReservationId());

        if (linkedGroup != null && !moveCreatorGroup) {
            detachReservationFromGroupForReschedule(reservation, linkedGroup);
        }

        syncSchedulePlayersOnReschedule(reservation, targetSchedule);

        Reservation update = new Reservation();
        update.setId(id);
        update.setReservationTime(newTime);
        if (targetSchedule != null) {
            update.setScheduleId(targetSchedule.getId());
            update.setRoomId(targetSchedule.getRoomId());
            update.setDmId(targetSchedule.getDmId());
        }
        if (linkedGroup != null && !moveCreatorGroup) {
            update.setGroupId(null);
        }
        reservationMapper.updateById(update);

        reservation.setReservationTime(newTime);
        if (targetSchedule != null) {
            reservation.setScheduleId(targetSchedule.getId());
            reservation.setRoomId(targetSchedule.getRoomId());
            reservation.setDmId(targetSchedule.getDmId());
        }
        if (linkedGroup != null && !moveCreatorGroup) {
            reservation.setGroupId(null);
        }

        if (moveCreatorGroup) {
            syncCreatorGroupAfterReschedule(linkedGroup, reservation);
        } else {
            GroupOrder autoCreatedGroup = createGroupOrderIfNeeded(reservation);
            if (autoCreatedGroup != null && !autoCreatedGroup.getId().equals(reservation.getGroupId())) {
                Reservation groupUpdate = new Reservation();
                groupUpdate.setId(reservation.getId());
                groupUpdate.setGroupId(autoCreatedGroup.getId());
                reservationMapper.updateById(groupUpdate);
                reservation.setGroupId(autoCreatedGroup.getId());
            }
        }

        // 发送改期通知
        try {
            String formattedTime = newTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            notificationService.sendToUsers(
                    "预约改期成功",
                    String.format("您的预约已成功改期至 %s，请按时到店", formattedTime),
                    1,
                    "reservation",
                    reservation.getId(),
                    reservation.getUserId()
            );
        } catch (Exception e) {
            log.warn("发送改期通知失败", e);
        }

        log.info("预约已改期: id={}, orderNo={}, newReservationTime={}", id, reservation.getOrderNo(), newReservationTime);
    }

    private ScriptSchedule resolveRescheduleTargetSchedule(Reservation reservation, LocalDateTime newTime) {
        if (reservation == null || scriptScheduleService == null
                || reservation.getStoreId() == null || reservation.getScriptId() == null || newTime == null) {
            return null;
        }

        List<ScriptSchedule> schedules = scriptScheduleService.listByStoreAndDate(
                reservation.getStoreId(),
                newTime.toLocalDate()
        );
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }

        ScriptSchedule preferred = null;
        for (ScriptSchedule schedule : schedules) {
            if (schedule == null
                    || Integer.valueOf(1).equals(schedule.getIsDeleted())
                    || !reservation.getScriptId().equals(schedule.getScriptId())
                    || schedule.getStartTime() == null
                    || !schedule.getStartTime().equals(newTime.toLocalTime())) {
                continue;
            }
            if (reservation.getScheduleId() != null && reservation.getScheduleId().equals(schedule.getId())) {
                return schedule;
            }
            if (preferred == null) {
                preferred = schedule;
            }
            if (reservation.getRoomId() != null && reservation.getRoomId().equals(schedule.getRoomId())) {
                preferred = schedule;
            }
        }
        return preferred;
    }

    private void validateRescheduleTargetSchedule(Reservation reservation, ScriptSchedule targetSchedule) {
        if (targetSchedule == null) {
            throw new RuntimeException("所选场次不存在，请重新选择");
        }
        if (Integer.valueOf(1).equals(targetSchedule.getIsDeleted())) {
            throw new RuntimeException("所选场次已失效，请重新选择");
        }
        if (hasScheduleStarted(targetSchedule)) {
            closeExpiredSchedule(targetSchedule);
            throw new RuntimeException("该场次已开始，请选择其他场次");
        }

        boolean sameSchedule = reservation.getScheduleId() != null && reservation.getScheduleId().equals(targetSchedule.getId());
        if (Integer.valueOf(2).equals(targetSchedule.getStatus())) {
            throw new RuntimeException("该场次已关闭，请选择其他场次");
        }
        if (Integer.valueOf(0).equals(targetSchedule.getStatus()) && !sameSchedule) {
            throw new RuntimeException("该场次已满，请选择其他场次");
        }

        int currentPlayers = targetSchedule.getCurrentPlayers() == null ? 0 : targetSchedule.getCurrentPlayers();
        if (sameSchedule) {
            currentPlayers = Math.max(0, currentPlayers - resolveReservationPlayerCount(reservation));
        }

        int maxPlayers = targetSchedule.getMaxPlayers() == null ? 0 : targetSchedule.getMaxPlayers();
        if (maxPlayers > 0 && currentPlayers + resolveReservationPlayerCount(reservation) > maxPlayers) {
            throw new RuntimeException("该场次余位不足，请选择其他场次");
        }
    }

    private void validateLegacyRescheduleAvailability(Reservation reservation, LocalDateTime newTime) {
        if (reservation == null || reservation.getRoomId() == null || newTime == null) {
            return;
        }
        LocalDateTime endTime = newTime.plusMinutes(calculateDurationMinutes(reservation.getDuration()));
        int conflictCount = reservationMapper.countConflictingReservationsExcludeSelf(
                reservation.getId(),
                reservation.getRoomId(),
                newTime,
                endTime
        );
        if (conflictCount > 0) {
            throw new RuntimeException("该时段房间已被预约，请选择其他时间");
        }
    }

    private void syncSchedulePlayersOnReschedule(Reservation reservation, ScriptSchedule targetSchedule) {
        if (scriptScheduleService == null || reservation == null) {
            return;
        }

        Long oldScheduleId = reservation.getScheduleId();
        Long newScheduleId = targetSchedule != null ? targetSchedule.getId() : null;
        if (oldScheduleId != null && !oldScheduleId.equals(newScheduleId)) {
            scriptScheduleService.decrementCurrentPlayers(oldScheduleId, resolveReservationPlayerCount(reservation));
        }
        if (newScheduleId != null && !newScheduleId.equals(oldScheduleId)) {
            scriptScheduleService.incrementCurrentPlayers(newScheduleId, resolveReservationPlayerCount(reservation));
        }
    }

    private void detachReservationFromGroupForReschedule(Reservation reservation, GroupOrder group) {
        if (reservation == null || group == null) {
            return;
        }

        if (!Integer.valueOf(1).equals(group.getStatus())) {
            reservation.setGroupId(null);
            return;
        }

        int playerCount = resolveReservationPlayerCount(reservation);
        if (groupMemberMapper != null && reservation.getUserId() != null) {
            LambdaQueryWrapper<com.murder.entity.GroupMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(com.murder.entity.GroupMember::getGroupId, group.getId())
                    .eq(com.murder.entity.GroupMember::getUserId, reservation.getUserId())
                    .eq(com.murder.entity.GroupMember::getStatus, 1);
            com.murder.entity.GroupMember member = groupMemberMapper.selectOne(wrapper);
            if (member != null) {
                int remainingCount = resolveJoinCount(member.getJoinCount()) - playerCount;
                if (remainingCount > 0) {
                    member.setJoinCount(remainingCount);
                } else {
                    member.setStatus(0);
                }
                groupMemberMapper.updateById(member);
            }
        }

        int currentCount = Math.max(0, resolveJoinCount(group.getCurrentCount()) - playerCount);
        GroupOrder update = new GroupOrder();
        update.setId(group.getId());
        update.setCurrentCount(currentCount);
        update.setDescription(buildAutoGroupDescription(currentCount, resolveJoinCount(group.getNeedCount())));
        groupOrderService.updateById(update);

        group.setCurrentCount(currentCount);
        group.setDescription(update.getDescription());
        reservation.setGroupId(null);
    }

    private void syncCreatorGroupAfterReschedule(GroupOrder group, Reservation reservation) {
        if (group == null || reservation == null || groupOrderService == null) {
            return;
        }

        int currentCount = resolveJoinCount(group.getCurrentCount());
        Integer requiredPlayers = group.getPlayerCount() != null && group.getPlayerCount() > 0
                ? group.getPlayerCount()
                : resolveRequiredPlayerCount(reservation.getScriptId());
        int needCount = requiredPlayers != null && requiredPlayers > 0
                ? resolveActiveGroupNeedCount(reservation, requiredPlayers, group.getId(), currentCount)
                : Math.max(currentCount, resolveJoinCount(group.getNeedCount()));

        GroupOrder update = new GroupOrder();
        update.setId(group.getId());
        update.setPlayTime(reservation.getReservationTime());
        update.setNeedCount(needCount);
        update.setDescription(buildAutoGroupDescription(currentCount, needCount));
        groupOrderService.updateById(update);

        group.setPlayTime(reservation.getReservationTime());
        group.setNeedCount(needCount);
        group.setDescription(update.getDescription());
        groupOrderService.refreshGroupStatus(group.getId());
    }

    private int resolveActiveGroupNeedCount(Reservation reservation, int requiredPlayers, Long groupId, int currentCount) {
        if (reservation == null || reservation.getScheduleId() == null) {
            return Math.max(currentCount, requiredPlayers);
        }
        int outsidePlayers = countActivePlayersOutsideGroupForSchedule(reservation.getScheduleId(), groupId);
        return Math.max(currentCount, requiredPlayers - outsidePlayers);
    }

    private int countActivePlayersOutsideGroupForSchedule(Long scheduleId, Long groupId) {
        if (scheduleId == null) {
            return 0;
        }
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getScheduleId, scheduleId)
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        int count = 0;
        for (Reservation item : reservations) {
            if (groupId != null && groupId.equals(item.getGroupId())) {
                continue;
            }
            count += resolveReservationPlayerCount(item);
        }
        return count;
    }

    private int resolveReservationPlayerCount(Reservation reservation) {
        return reservation != null && reservation.getPlayerCount() != null && reservation.getPlayerCount() > 0
                ? reservation.getPlayerCount()
                : 1;
    }

    private int resolveJoinCount(Integer count) {
        return count != null && count > 0 ? count : 1;
    }

    private String buildAutoGroupDescription(int currentCount, int needCount) {
        int remaining = Math.max(0, needCount - currentCount);
        return remaining > 0
                ? "预约自动发起的拼团，还差" + remaining + "位玩家"
                : "预约自动发起的拼团，已满足成团人数";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDm(Long id, Long dmId) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAssignDmScope(reservation);

        if (dmMapper == null) {
            throw new RuntimeException("DM 模块未启用");
        }

        com.murder.entity.Dm dm = dmMapper.selectById(dmId);
        if (dm == null || !Integer.valueOf(1).equals(dm.getStatus())) {
            throw new RuntimeException("指定 DM 不存在或已离职");
        }
        if (reservation.getStoreId() != null && dm.getStoreId() != null
                && !reservation.getStoreId().equals(dm.getStoreId())) {
            throw new RuntimeException("只能为当前门店预约分配本门店 DM");
        }

        DmAssignmentState assignmentState = resolveDmAssignmentState(reservation);
        if (!assignmentState.assignable()) {
            throw new RuntimeException(assignmentState.hint());
        }

        if (reservation.getScheduleId() != null && scriptScheduleService != null) {
            ScriptSchedule schedule = scriptScheduleService.getById(reservation.getScheduleId());
            if (schedule == null) {
                throw new RuntimeException("预约关联场次不存在，无法分配 DM");
            }

            ScriptSchedule updateSchedule = new ScriptSchedule();
            updateSchedule.setId(schedule.getId());
            updateSchedule.setDmId(dmId);
            scriptScheduleService.update(updateSchedule);

            syncDmToScheduleReservations(schedule.getId(), dmId);
        } else {
            updateReservationDm(reservation.getId(), dmId);
        }

        if (reservation.getGroupId() != null) {
            syncDmToGroupReservations(reservation.getGroupId(), dmId);
        }

        log.info("预约分配 DM 成功: reservationId={}, dmId={}", id, dmId);
        recordReservationOperation("RESERVATION_ASSIGN_DM", reservation, "分配主持DM为#" + dmId);
    }

    private void assertAssignDmScope(Reservation reservation) {
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role)) {
            assertAdminStoreScope(reservation);
            return;
        }
        if (!"STORE_STAFF".equals(role)) {
            throw new SecurityException("没有权限分配该预约的 DM");
        }
        Long currentStoreId = BaseContext.getStoreId();
        if (currentStoreId == null || reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
            throw new SecurityException("没有权限操作该门店的预约");
        }
        if (!hasReservationPermission("reservation:assign_dm")) {
            throw new SecurityException("当前员工账号没有分配 DM 权限");
        }
    }

    private void populateDmAssignmentState(Reservation reservation, ReservationVO vo) {
        DmAssignmentState state = resolveDmAssignmentState(reservation);
        vo.setDmAssignable(state.assignable());
        vo.setDmAssignHint(state.hint());
        vo.setGroupStatus(state.groupStatus());
    }

    private DmAssignmentState resolveDmAssignmentState(Reservation reservation) {
        if (reservation == null) {
            return new DmAssignmentState(false, "预约不存在", null);
        }
        if (Integer.valueOf(4).equals(reservation.getStatus())) {
            return new DmAssignmentState(false, "已取消预约不能分配 DM", null);
        }
        if (Integer.valueOf(3).equals(reservation.getStatus()) && reservation.getDmId() == null) {
            return new DmAssignmentState(false, "已完成预约不再支持补分配 DM", null);
        }

        ScriptSchedule schedule = getScheduleForReservation(reservation);
        Long resolvedDmId = reservation.getDmId() != null
                ? reservation.getDmId()
                : (schedule != null ? schedule.getDmId() : null);
        if (resolvedDmId != null) {
            return new DmAssignmentState(true, "当前场次已分配主持 DM，可直接改派", resolveGroupStatus(reservation));
        }

        Integer requiredPlayers = resolveRequiredPlayerCount(reservation.getScriptId());
        Integer currentPlayers = schedule != null && schedule.getCurrentPlayers() != null
                ? schedule.getCurrentPlayers()
                : reservation.getPlayerCount();
        Integer groupStatus = resolveGroupStatus(reservation);

        if (requiredPlayers == null || requiredPlayers <= 0) {
            return new DmAssignmentState(true, "当前场次可分配 DM", groupStatus);
        }
        if (currentPlayers != null && currentPlayers >= requiredPlayers) {
            return new DmAssignmentState(true, "当前人数已满足开场要求，可分配 DM", groupStatus);
        }
        if (Integer.valueOf(2).equals(groupStatus)) {
            return new DmAssignmentState(true, "拼团已成团，可分配 DM", groupStatus);
        }
        if (Integer.valueOf(1).equals(groupStatus)) {
            return new DmAssignmentState(false, "当前仍在拼团中，需成团后再分配 DM", groupStatus);
        }
        return new DmAssignmentState(false, "当前预约人数未达到剧本开场人数，暂不能分配 DM", groupStatus);
    }

    private Integer resolveRequiredPlayerCount(Long scriptId) {
        if (scriptId == null || scriptService == null) {
            return null;
        }
        try {
            com.murder.entity.Script script = scriptService.getById(scriptId);
            return script != null ? script.getPlayerCount() : null;
        } catch (Exception e) {
            log.debug("查询剧本开场人数失败: scriptId={}", scriptId, e);
            return null;
        }
    }

    private void recordReservationOperation(String actionType, Reservation reservation, String detail) {
        if (storeEmployeeOperationLogService == null || reservation == null) {
            return;
        }
        try {
            storeEmployeeOperationLogService.record(
                    reservation.getStoreId(),
                    actionType,
                    "RESERVATION",
                    reservation.getId(),
                    reservation.getOrderNo(),
                    detail
            );
        } catch (Exception ignored) {
            // ignore
        }
    }

    private boolean hasScheduleStarted(ScriptSchedule schedule) {
        if (schedule == null || schedule.getScheduleDate() == null || schedule.getStartTime() == null) {
            return false;
        }
        LocalDateTime startAt = LocalDateTime.of(schedule.getScheduleDate(), schedule.getStartTime());
        return !startAt.isAfter(LocalDateTime.now());
    }

    private void closeExpiredSchedule(ScriptSchedule schedule) {
        if (schedule == null || schedule.getId() == null || scriptScheduleService == null
                || Integer.valueOf(2).equals(schedule.getStatus())) {
            return;
        }

        ScriptSchedule update = new ScriptSchedule();
        update.setId(schedule.getId());
        update.setStatus(2);
        update.setRemark(buildExpiredScheduleRemark(schedule.getRemark()));
        try {
            scriptScheduleService.update(update);
            schedule.setStatus(2);
        } catch (Exception e) {
            log.warn("关闭已开场排期失败: scheduleId={}", schedule.getId(), e);
        }
    }

    private String buildExpiredScheduleRemark(String currentRemark) {
        String expiredRemark = "开场时间已过，排期已自动关闭";
        if (!StringUtils.hasText(currentRemark)) {
            return expiredRemark;
        }
        if (currentRemark.contains(expiredRemark)) {
            return currentRemark;
        }
        return currentRemark + "；" + expiredRemark;
    }

    private Integer resolveGroupStatus(Reservation reservation) {
        if (reservation == null || reservation.getGroupId() == null || groupOrderService == null) {
            return null;
        }
        try {
            GroupOrder groupOrder = groupOrderService.getById(reservation.getGroupId());
            return groupOrder != null ? groupOrder.getStatus() : null;
        } catch (Exception e) {
            log.debug("查询拼团状态失败: groupId={}", reservation.getGroupId(), e);
            return null;
        }
    }

    private ScriptSchedule getScheduleForReservation(Reservation reservation) {
        if (reservation == null || reservation.getScheduleId() == null || scriptScheduleService == null) {
            return null;
        }
        try {
            return scriptScheduleService.getById(reservation.getScheduleId());
        } catch (Exception e) {
            log.debug("查询预约关联场次失败: scheduleId={}", reservation.getScheduleId(), e);
            return null;
        }
    }

    private void syncDmToScheduleReservations(Long scheduleId, Long dmId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getScheduleId, scheduleId)
                .eq(Reservation::getIsDeleted, 0);
        syncDmToReservations(wrapper, dmId);
    }

    private void syncDmToGroupReservations(Long groupId, Long dmId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getGroupId, groupId)
                .eq(Reservation::getIsDeleted, 0);
        syncDmToReservations(wrapper, dmId);
    }

    private void syncDmToReservations(LambdaQueryWrapper<Reservation> wrapper, Long dmId) {
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        for (Reservation item : reservations) {
            updateReservationDm(item.getId(), dmId);
        }
    }

    private void updateReservationDm(Long reservationId, Long dmId) {
        Reservation update = new Reservation();
        update.setId(reservationId);
        update.setDmId(dmId);
        reservationMapper.updateById(update);
    }

    private record DmAssignmentState(boolean assignable, String hint, Integer groupStatus) {
    }
}
