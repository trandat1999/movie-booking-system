package com.moviebooking.modules.booking.event;

import com.moviebooking.modules.booking.enums.HoldStatus;

/**
 * Lượt giữ ghế của nhóm kết thúc mà không thành đơn đã thanh toán (hết hạn, bỏ giữ, hủy đơn). Phát trong
 * transaction; module nhóm nghe đồng bộ để đưa nhóm từ HOLDING về LOCKED.
 */
public record HoldClosedEvent(Long holdId, Long groupId, HoldStatus status) {}
