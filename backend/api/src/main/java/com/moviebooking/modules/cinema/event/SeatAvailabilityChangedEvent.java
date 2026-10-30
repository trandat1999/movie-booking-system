package com.moviebooking.modules.cinema.event;

/**
 * Ghế được bật/tắt bán (CIN06). Module lịch chiếu lắng nghe đồng bộ để cập nhật trạng thái ghế của các suất
 * chiếu chưa diễn ra (AVAILABLE ↔ BLOCKED).
 */
public record SeatAvailabilityChangedEvent(Long roomId, Long seatId, boolean active) {}
