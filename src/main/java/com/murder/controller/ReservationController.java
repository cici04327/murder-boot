package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ReservationDTO;
import com.murder.dto.RescheduleDTO;
import com.murder.entity.Reservation;
import com.murder.service.ReservationService;
import com.murder.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Objects;

import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@Tag(name = "预约接口")
@Slf4j
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "创建预约")
    public Result<Reservation> create(@RequestBody ReservationDTO reservationDTO) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null) {
            reservationDTO.setUserId(currentUserId);
        }
        log.info("创建预约: userId={}, userCouponId={}, totalPrice={}",
                reservationDTO.getUserId(),
                reservationDTO.getUserCouponId(),
                reservationDTO.getTotalPrice());
        return Result.success(reservationService.create(reservationDTO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询预约列表")
    public Result<PageResult<ReservationVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reservationDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) Integer checkInStatus,
            @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) Boolean hasRefund
    ) {
        boolean operator = isBackofficeOperator();
        if (operator && !canViewReservationOperator()) {
            return Result.error(403, "没有查看预约权限");
        }

        if (!operator) {
            userId = BaseContext.getCurrentId();
        }

        if ("STORE_ADMIN".equals(BaseContext.getRole()) || "STORE_STAFF".equals(BaseContext.getRole())) {
            storeId = BaseContext.getStoreId();
        }

        log.info("分页查询预约列表: page={}, pageSize={}, userId={}, storeId={}, scheduleId={}, reservationDate={}, status={}, payStatus={}, checkInStatus={}, refundStatus={}, hasRefund={}, role={}",
                page, pageSize, userId, storeId, scheduleId, reservationDate, status, payStatus, checkInStatus, refundStatus, hasRefund, BaseContext.getRole());
        return Result.success(
                reservationService.pageQueryWithDetails(page, pageSize, userId, storeId, scheduleId, reservationDate, status, payStatus, checkInStatus, refundStatus, hasRefund)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预约详情")
    public Result<ReservationVO> getById(@PathVariable Long id) {
        log.info("查询预约详情: {}", id);
        ReservationVO vo = reservationService.getDetailById(id);
        // 用户端：校验预约归属
        if (!isBackofficeOperator() && vo != null) {
            Long currentUserId = BaseContext.getCurrentId();
            if (!Objects.equals(currentUserId, vo.getUserId())) {
                log.warn("用户{}尝试查看不属于自己的预约{}", currentUserId, id);
                return Result.error("无权查看该预约");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "查询预约详情")
    public Result<ReservationVO> getDetail(@PathVariable Long id) {
        log.info("查询预约详情: {}", id);
        ReservationVO vo = reservationService.getDetailById(id);
        // 用户端：校验预约归属
        if (!isBackofficeOperator() && vo != null) {
            Long currentUserId = BaseContext.getCurrentId();
            if (!Objects.equals(currentUserId, vo.getUserId())) {
                log.warn("用户{}尝试查看不属于自己的预约{}", currentUserId, id);
                return Result.error("无权查看该预约");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/no/{reservationNo}")
    @Operation(summary = "根据预约编号查询预约详情")
    public Result<Reservation> getByReservationNo(@PathVariable String reservationNo) {
        log.info("根据预约编号查询预约详情: {}", reservationNo);
        return Result.success(reservationService.getByReservationNo(reservationNo));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认预约")
    public Result<String> confirm(@PathVariable Long id) {
        if (!isAdminOperator()) {
            return Result.error(403, "没有管理端访问权限");
        }
        log.info("确认预约: {}", id);
        reservationService.confirm(id);
        return Result.success("确认成功");
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预约")
    public Result<String> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("取消预约: {}, reason={}", id, reason);
        reservationService.cancel(id, reason);
        return Result.success("取消成功");
    }

    @PutMapping("/{id}/check-in")
    @Operation(summary = "到店核销")
    public Result<String> checkIn(@PathVariable Long id, @RequestParam String checkInCode) {
        if (!canCheckInOperator()) {
            return Result.error(403, "没有管理端访问权限");
        }
        log.info("到店核销: id={}, checkInCode={}", id, checkInCode);
        reservationService.checkIn(id, checkInCode);
        return Result.success("核销成功");
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成预约")
    public Result<String> complete(@PathVariable Long id) {
        if (!canCompleteOperator()) {
            return Result.error(403, "没有管理端访问权限");
        }
        log.info("完成预约: {}", id);
        reservationService.complete(id);
        return Result.success("完成成功");
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "支付预约")
    public Result<String> pay(@PathVariable Long id) {
        if (!isAdminOperator()) {
            return Result.error(403, "没有管理端访问权限");
        }
        log.info("支付预约: {}", id);
        reservationService.pay(id);
        return Result.success("支付成功");
    }

    @GetMapping("/upcoming")
    @Operation(summary = "查询即将开始的预约")
    public Result<List<Reservation>> getUpcoming(@RequestParam(defaultValue = "2") Integer hours) {
        log.info("查询即将开始的预约: hours={}", hours);
        return Result.success(reservationService.getUpcomingReservations(hours));
    }

    @GetMapping("/check-availability")
    @Operation(summary = "检查房间可用性")
    public Result<Boolean> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam String reservationTime,
            @RequestParam(defaultValue = "3") Double duration
    ) {
        log.info("检查房间可用性: roomId={}, reservationTime={}, duration={}", roomId, reservationTime, duration);
        return Result.success(reservationService.checkRoomAvailability(roomId, reservationTime, duration));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "改期预约")
    public Result<String> reschedule(@PathVariable Long id, @RequestBody RescheduleDTO dto) {
        log.info("改期预约: id={}, newReservationTime={}", id, dto.getNewReservationTime());
        reservationService.reschedule(id, dto.getNewReservationTime());
        return Result.success("改期成功");
    }

    @PutMapping("/{id}/assign-dm")
    @Operation(summary = "分配预约主持 DM")
    public Result<String> assignDm(@PathVariable Long id, @RequestParam Long dmId) {
        if (!canAssignDmOperator()) {
            return Result.error(403, "没有管理端访问权限");
        }
        log.info("分配预约主持 DM: reservationId={}, dmId={}", id, dmId);
        reservationService.assignDm(id, dmId);
        return Result.success("分配成功");
    }

    private boolean isAdminOperator() {
        String role = BaseContext.getRole();
        return "SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role);
    }

    private boolean isBackofficeOperator() {
        String role = BaseContext.getRole();
        return "SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role) || "STORE_STAFF".equals(role);
    }

    private boolean canViewReservationOperator() {
        if (isAdminOperator()) {
            return true;
        }
        return "STORE_STAFF".equals(BaseContext.getRole()) && hasPermission("reservation:view");
    }

    private boolean canCheckInOperator() {
        if (isAdminOperator()) {
            return true;
        }
        return "STORE_STAFF".equals(BaseContext.getRole()) && hasPermission("reservation:checkin");
    }

    private boolean canCompleteOperator() {
        if (isAdminOperator()) {
            return true;
        }
        return "STORE_STAFF".equals(BaseContext.getRole()) && hasPermission("reservation:complete");
    }

    private boolean canAssignDmOperator() {
        if (isAdminOperator()) {
            return true;
        }
        return "STORE_STAFF".equals(BaseContext.getRole()) && hasPermission("reservation:assign_dm");
    }

    private boolean hasPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (permissionCodes == null || permissionCode == null) {
            return false;
        }
        for (String code : permissionCodes.split(",")) {
            if (permissionCode.equals(code != null ? code.trim() : null)) {
                return true;
            }
        }
        return false;
    }
}
