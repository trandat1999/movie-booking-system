package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.TicketStatus;

public record MyTicketResponse(
        String code,
        String orderCode,
        String seatLabel,
        TicketStatus status,
        BookingShowtimeInfo showtime,
        String qrImage) {}
