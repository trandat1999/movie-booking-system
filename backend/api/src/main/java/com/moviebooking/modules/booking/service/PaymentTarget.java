package com.moviebooking.modules.booking.service;

import com.moviebooking.modules.booking.enums.OrderStatus;

/** Đơn hàng đã khóa dòng để module thanh toán ghi nhận giao dịch. */
public record PaymentTarget(Long orderId, String code, Long userId, OrderStatus status, long total) {}
