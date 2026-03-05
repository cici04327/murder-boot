package com.murder.controller;

import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 预约控制�?
 */
@RestController
@RequestMapping("/api/reservation")
@Tag(name = "预约接口")
@Slf4j
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * 创建预约
     */
    @PostMapping
    @Operation(summary = "创建预约")
    public Result<Reservation> create(@RequestBody ReservationDTO reservationDTO) {
        // 优先从Token中获取当前登录用户ID，防止前端传入伪造userId
        Long currentUserId = com.murder.common.context.BaseContext.getCurrentId();
        if (currentUserId != null) {
            reservationDTO.setUserId(currentUserId);
        }
        log.info("创建预约: userId={}, userCouponId={}, totalPrice={}",
            reservationDTO.getUserId(), reservationDTO.getUserCouponId(), reservationDTO.getTotalPrice());
        Reservation reservation = reservationService.create(reservationDTO);
        return Result.success(reservation);
    }

    /**
     * 分页查询预约列表（包含关联信息）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询预约列表")
    public Result<PageResult<com.murder.vo.ReservationVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) Boolean hasRefund) {
        // 兼容性处理：某些情况下 RequestContextHolder 可能取不到请求上下文（会导致偶发 500）
        String clientType = null;
        try {
            org.springframework.web.context.request.RequestAttributes ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                clientType = attrs.getRequest().getHeader("X-Client-Type");
            }
        } catch (Exception ignore) {
            // ignore
        }

        // 用户端：强制只能查看自己的预约（安全性保障）
        if (!"admin".equals(clientType)) {
            userId = com.murder.common.context.BaseContext.getCurrentId();
        }

        // 管理端：门店管理员强制只能看自己的门店
        if ("admin".equals(clientType) && "STORE_ADMIN".equals(com.murder.common.context.BaseContext.getRole())) {
            storeId = com.murder.common.context.BaseContext.getStoreId();
        }

        log.info("分页查询预约列表: page={}, pageSize={}, userId={}, storeId={}, status={}, refundStatus={}, hasRefund={}, clientType={}",
                page, pageSize, userId, storeId, status, refundStatus, hasRefund, clientType);
        PageResult<com.murder.vo.ReservationVO> pageResult = reservationService.pageQueryWithDetails(page, pageSize, userId, storeId, status, refundStatus, hasRefund);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询预约详情（包含关联信息）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询预约详情")
    public Result<com.murder.vo.ReservationVO> getById(@PathVariable Long id) {
        log.info("查询预约详情: {}", id);
        com.murder.vo.ReservationVO vo = reservationService.getDetailById(id);
        return Result.success(vo);
    }
    
    /**
     * 根据ID查询预约详情（包含关联信息）
     */
    @GetMapping("/{id}/detail")
    @Operation(summary = "查询预约详情（包含关联信息）")
    public Result<com.murder.vo.ReservationVO> getDetail(@PathVariable Long id) {
        log.info("查询预约详情（包含关联信息）: {}", id);
        com.murder.vo.ReservationVO vo = reservationService.getDetailById(id);
        return Result.success(vo);
    }

    /**
     * 根据预约编号查询预约详情
     */
    @GetMapping("/no/{reservationNo}")
    @Operation(summary = "根据预约编号查询预约详情")
    public Result<Reservation> getByReservationNo(@PathVariable String reservationNo) {
        log.info("根据预约编号查询预约详情: {}", reservationNo);
        Reservation reservation = reservationService.getByReservationNo(reservationNo);
        return Result.success(reservation);
    }

    /**
     * 确认预约
     */
    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认预约")
    public Result<String> confirm(@PathVariable Long id) {
        log.info("确认预约: {}", id);
        reservationService.confirm(id);
        return Result.success("确认成功");
    }

    /**
     * 取消预约
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预约")
    public Result<String> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("取消预约: {}, 原因: {}", id, reason);
        reservationService.cancel(id, reason);
        return Result.success("取消成功");
    }

    /**
     * 完成预约
     */
    @PutMapping("/{id}/complete")
    @Operation(summary = "完成预约")
    public Result<String> complete(@PathVariable Long id) {
        log.info("完成预约: {}", id);
        reservationService.complete(id);
        return Result.success("完成成功");
    }

    /**
     * 支付预约
     */
    @PutMapping("/{id}/pay")
    @Operation(summary = "支付预约")
    public Result<String> pay(@PathVariable Long id) {
        log.info("支付预约: {}", id);
        reservationService.pay(id);
        return Result.success("支付成功");
    }
    
    /**
     * 查询即将开始的预约（供内部服务调用�?
     */
    @GetMapping("/upcoming")
    @Operation(summary = "查询即将开始的预约")
    public Result<java.util.List<Reservation>> getUpcoming(@RequestParam(defaultValue = "2") Integer hours) {
        log.info("查询即将开始的预约: hours={}", hours);
        java.util.List<Reservation> reservations = reservationService.getUpcomingReservations(hours);
        return Result.success(reservations);
    }
    
    /**
     * 检查房间可用性
     */
    @GetMapping("/check-availability")
    @Operation(summary = "检查房间可用性")
    public Result<Boolean> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam String reservationTime,
            @RequestParam(defaultValue = "3") Double duration) {
        log.info("检查房间可用�? roomId={}, reservationTime={}, duration={}", roomId, reservationTime, duration);
        boolean isAvailable = reservationService.checkRoomAvailability(roomId, reservationTime, duration);
        return Result.success(isAvailable);
    }
}