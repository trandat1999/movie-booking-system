package com.moviebooking.modules.cinema.dto.response;

import com.moviebooking.modules.cinema.enums.RoomStatus;
import com.moviebooking.modules.cinema.enums.RoomType;

/**
 * @param seatCount số ghế
 * @param capacity tổng số chỗ ngồi (ghế đôi tính 2)
 */
public record RoomResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
        String name,
        RoomType roomType,
        RoomStatus status,
        int gridRows,
        int gridCols,
        boolean layoutLocked,
        long seatCount,
        long capacity) {}
