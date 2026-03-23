package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;
import com.murder.vo.ReservationVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService {

    Reservation create(ReservationDTO reservationDTO);

    PageResult<Reservation> pageQuery(Integer page, Integer pageSize, Long userId, Integer status);

    PageResult<ReservationVO> pageQueryWithDetails(
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
    );

    Reservation getById(Long id);

    ReservationVO getDetailById(Long id);

    Reservation getByReservationNo(String reservationNo);

    void confirm(Long id);

    void cancel(Long id, String reason);

    void complete(Long id);

    void checkIn(Long id, String checkInCode);

    List<Reservation> getCompletableReservations();

    List<Reservation> getUnpaidReservations(LocalDateTime timeoutTime);

    void pay(Long id);

    List<Reservation> getUpcomingReservations(Integer hours);

    boolean checkRoomAvailability(Long roomId, String reservationTime, Double duration);

    void reschedule(Long id, String newReservationTime);

    void assignDm(Long id, Long dmId);
}
