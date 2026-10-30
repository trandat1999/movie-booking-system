package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.user.dto.response.UserBrief;
import java.time.Instant;

/** Một dòng trong danh sách đơn của trang quản trị (ORDADM). */
public record AdminOrderRow(
        String code,
        OrderStatus status,
        BookingShowtimeInfo showtime,
        int seatCount,
        long total,
        long discount,
        String promotionCode,
        Long groupId,
        UserBrief customer,
        Instant createdAt,
        Instant paidAt) {}
