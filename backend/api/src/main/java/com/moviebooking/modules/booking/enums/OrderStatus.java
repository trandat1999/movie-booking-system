package com.moviebooking.modules.booking.enums;

/** Trạng thái đơn hàng (PLAN.md 6.2). */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    EXPIRED,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED,
    COMPLETED
}
