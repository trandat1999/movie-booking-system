package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String code,
        OrderStatus status,
        BookingShowtimeInfo showtime,
        List<SeatLine> items,
        long subtotal,
        long discount,
        long total,
        Instant expiresAt,
        Instant paidAt,
        Instant cancelledAt,
        Instant createdAt,
        List<TicketResponse> tickets,
        Instant serverTime) {}
