package com.moviebooking.modules.booking.dto.request;

import jakarta.validation.constraints.Size;

/** @param note ghi chú, ví dụ mã giao dịch hoàn trên trang merchant */
public record CompleteRefundRequest(@Size(max = 500) String note) {}
