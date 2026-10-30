package com.moviebooking.modules.payment.dto.response;

/** @param redirectUrl trang của cổng thanh toán để chuyển người dùng tới */
public record PaymentSessionResponse(String provider, String redirectUrl) {}
