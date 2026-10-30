package com.moviebooking.modules.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/** @param provider mã cổng thanh toán, ví dụ {@code MOCK} */
public record PayOrderRequest(@NotBlank String provider) {}
