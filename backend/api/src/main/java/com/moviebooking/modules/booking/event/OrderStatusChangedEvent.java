package com.moviebooking.modules.booking.event;

import com.moviebooking.modules.booking.enums.OrderStatus;

/**
 * Phát khi đơn đổi trạng thái, trong transaction. Listener {@code AFTER_COMMIT} lo thông báo/email/realtime;
 * module nhóm nghe đồng bộ để cập nhật trạng thái nhóm trong cùng transaction.
 *
 * @param groupId khác null nếu là đơn của nhóm
 * @param reason loại sự kiện ghi trong {@code order_events}, ví dụ PAID, CANCELLED_BY_USER, SHOWTIME_CANCELLED
 */
public record OrderStatusChangedEvent(
        Long orderId, String orderCode, Long userId, Long groupId, OrderStatus from, OrderStatus to, String reason) {}
