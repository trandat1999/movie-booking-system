package com.moviebooking.modules.payment.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;

/**
 * @param duplicate giao dịch đã được xử lý trước đó (callback lặp lại) — không có thay đổi nào
 * @param orderStatus trạng thái đơn sau khi xử lý
 */
public record PaymentCallbackResponse(String orderCode, boolean duplicate, OrderStatus orderStatus) {}
