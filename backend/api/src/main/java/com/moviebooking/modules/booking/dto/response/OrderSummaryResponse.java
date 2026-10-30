package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import java.time.Instant;

public record OrderSummaryResponse(
        String code, OrderStatus status, BookingShowtimeInfo showtime, int seatCount, long total, Instant createdAt) {}
