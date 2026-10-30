package com.moviebooking.modules.showtime.event;

/**
 * Admin hủy suất chiếu (SCHADM03). Phát trong transaction; module đặt vé hủy đơn + hoàn tiền 100 %, module nhóm
 * đưa nhóm về bước chọn suất hoặc hủy nhóm đã đặt.
 */
public record ShowtimeCancelledEvent(Long showtimeId, Long actorId) {}
