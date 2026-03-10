package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.mapper.ReservationMapper;
import com.murder.service.AdminNotificationService;
import com.murder.service.CouponService;
import com.murder.service.NotificationService;
import com.murder.service.ReservationService;
import com.murder.service.StoreRoomService;
import com.murder.vo.ReservationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation create(ReservationDTO reservationDTO) {
        if (reservationDTO.getRoomId() != null && reservationDTO.getReservationTime() != null) {
            String reservationTimeStr = reservationDTO.getReservationTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            boolean isAvailable = checkRoomAvailability(reservationDTO.getRoomId(), reservationTimeStr, 3.0);
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
        reservation.setCheckInCode(generateCheckInCode());

        BigDecimal totalPrice = defaultAmount(reservation.getTotalPrice());
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

        if (couponValid && reservationDTO.getUserCouponId() != null) {
            couponService.useCoupon(reservationDTO.getUserCouponId(), reservation.getId());
        }

        try {
            createGroupOrderIfNeeded(reservation, reservationDTO);
        } catch (Exception e) {
            log.error("自动创建拼团失败: reservationId={}", reservation.getId(), e);
        }

        try {
            sendReservationSuccessNotification(reservation);
        } catch (Exception e) {
            log.error("发送预约成功通知失败: reservationId={}", reservation.getId(), e);
        }

        return reservation;
    }

    private void createGroupOrderIfNeeded(Reservation reservation, ReservationDTO reservationDTO) {
        if (groupOrderService == null || scriptService == null) {
            return;
        }

        com.murder.entity.Script script = scriptService.getById(reservation.getScriptId());
        if (script == null || script.getPlayerCount() == null || reservation.getPlayerCount() == null) {
            return;
        }

        if (reservation.getPlayerCount() >= script.getPlayerCount()) {
            return;
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
        groupOrderService.createGroup(groupOrder, reservation.getUserId());
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
        wrapper.orderByDesc(Reservation::getCreateTime);

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

        assertAdminStoreScope(reservation);
        ensureCheckInFields(reservation);

        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        populateStoreInfo(reservation, vo, true);
        populateScriptInfo(reservation, vo);
        populateRoomInfo(reservation, vo);
        populateReviewState(reservation, vo);
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
        assertAdminStoreScope(reservation);
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

        if (reservation.getCouponId() != null) {
            try {
                refundCoupon(id);
            } catch (Exception e) {
                log.error("退回优惠券失败: reservationId={}", id, e);
            }
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus(4);
        update.setRemark(reason);
        reservationMapper.updateById(update);
    }

    @Override
    public void complete(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(reservation);
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
        reservationMapper.updateById(update);

        log.info("预约已完成: id={}, orderNo={}", reservation.getId(), reservation.getOrderNo());
    }

    @Override
    public void checkIn(Long id, String checkInCode) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        assertAdminStoreScope(reservation);
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
            double durationHours = reservation.getDuration() != null
                    ? reservation.getDuration().doubleValue()
                    : 3.0;
            LocalDateTime endTime = reservation.getReservationTime().plusHours((long) Math.ceil(durationHours));
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

        Reservation update = new Reservation();
        update.setId(id);
        update.setPayStatus(1);
        update.setPayTime(LocalDateTime.now());
        update.setStatus(2);
        update.setCheckInStatus(existing.getCheckInStatus());
        update.setCheckInCode(existing.getCheckInCode());
        reservationMapper.updateById(update);
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
            LocalDateTime startTime = LocalDateTime.parse(
                    reservationTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );
            long durationHours = (long) Math.ceil(duration);
            LocalDateTime endTime = startTime.plusHours(durationHours);
            int conflictCount = reservationMapper.countConflictingReservations(roomId, startTime, endTime);
            return conflictCount == 0;
        } catch (Exception e) {
            log.error("检查房间可用性失败", e);
            return false;
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
        if (!StringUtils.hasText(reservation.getCheckInCode())) {
            reservation.setCheckInCode(generateCheckInCode());
            needUpdate = true;
        }
        if (reservation.getCheckInStatus() == null) {
            reservation.setCheckInStatus(0);
            needUpdate = true;
        }

        if (needUpdate) {
            Reservation update = new Reservation();
            update.setId(reservation.getId());
            update.setCheckInCode(reservation.getCheckInCode());
            update.setCheckInStatus(reservation.getCheckInStatus());
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

    private void assertAdminStoreScope(Reservation reservation) {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
                return;
            }
            String clientType = servletRequestAttributes.getRequest().getHeader("X-Client-Type");
            if (!"admin".equals(clientType)) {
                return;
            }
            String role = com.murder.common.context.BaseContext.getRole();
            if (!"STORE_ADMIN".equals(role)) {
                return;
            }
            Long currentStoreId = com.murder.common.context.BaseContext.getStoreId();
            if (currentStoreId == null) {
                throw new RuntimeException("门店管理员未绑定门店");
            }
            if (reservation.getStoreId() == null || !currentStoreId.equals(reservation.getStoreId())) {
                throw new RuntimeException("没有权限操作该门店的预约");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("权限校验失败", e);
        }
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
}
