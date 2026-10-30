package com.moviebooking.modules.showtime.dto.response;

/** Thống kê ghế của suất chiếu (SCHADM09). */
public record SeatStats(long total, long available, long held, long sold, long blocked) {}
