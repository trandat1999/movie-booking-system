package com.moviebooking.modules.booking.dto.response;

/** Một ghế kèm giá (giá của cả ghế — ghế đôi là giá cho 2 người). */
public record SeatLine(Long seatId, String label, String seatTypeCode, long price) {}
