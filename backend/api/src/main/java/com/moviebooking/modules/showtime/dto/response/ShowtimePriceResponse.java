package com.moviebooking.modules.showtime.dto.response;

/** Giá của một ghế thuộc loại này (ghế đôi: giá cho cả ghế 2 người). */
public record ShowtimePriceResponse(String seatTypeCode, String seatTypeName, long price) {}
