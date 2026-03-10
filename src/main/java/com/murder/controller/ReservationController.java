package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.service.ReservationService;
import com.murder.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) Integer checkInStatus,
            @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) Boolean hasRefund,
            HttpServletRequest request
    ) {
        String clientType = request.getHeader("X-Client-Type");

        if (!"admin".equals(clientType)) {
            userId = BaseContext.getCurrentId();
        }

        if ("admin".equals(clientType) && "STORE_ADMIN".equals(BaseContext.getRole())) {
            storeId = BaseContext.getStoreId();
        }

        log.info("分页查询预约列表: page={}, pageSize={}, userId={}, storeId={}, status={}, payStatus={}, checkInStatus={}, refundStatus={}, hasRefund={}, clientType={}",
                page, pageSize, userId, storeId, status, payStatus, checkInStatus, refundStatus, hasRefund, clientType);
        return Result.success(
                reservationService.pageQueryWithDetails(page, pageSize, userId, storeId, status, payStatus, checkInStatus, refundStatus, hasRefund)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预约详情")
    public Result<ReservationVO> getById(@PathVariable Long id) {
        log.info("查询预约详情: {}", id);
        return Result.success(reservationService.getDetailById(id));
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "查询预约详情")
    public Result<ReservationVO> getDetail(@PathVariable Long id) {
        log.info("查询预约详情: {}", id);
        return Result.success(reservationService.getDetailById(id));
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
        log.info("到店核销: id={}, checkInCode={}", id, checkInCode);
        reservationService.checkIn(id, checkInCode);
        return Result.success("核销成功");
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成预约")
    public Result<String> complete(@PathVariable Long id) {
        log.info("完成预约: {}", id);
        reservationService.complete(id);
        return Result.success("完成成功");
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "支付预约")
    public Result<String> pay(@PathVariable Long id) {
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
}
