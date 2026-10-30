package com.moviebooking.modules.booking.event;

import com.moviebooking.modules.booking.enums.OrderStatus;

/** Phát khi đơn đổi trạng thái; listener xử lý sau commit (thông báo, email, realtime). */
public record OrderStatusChangedEvent(Long orderId, String orderCode, Long userId, OrderStatus from, OrderStatus to) {}
