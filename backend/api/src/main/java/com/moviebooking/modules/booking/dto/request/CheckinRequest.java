package com.moviebooking.modules.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param payload nội dung QR (mã vé hoặc mã đơn kèm chữ ký) — hoặc mã nhập tay kèm chữ ký */
public record CheckinRequest(@NotBlank @Size(max = 200) String payload) {}
