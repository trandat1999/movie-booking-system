package com.moviebooking.modules.payment.enums;

public enum RefundStatus {
    /** Chờ hoàn: cổng không hỗ trợ hoàn tự động hoặc gọi cổng thất bại — admin hoàn thủ công rồi xác nhận. */
    PENDING,
    COMPLETED,
    FAILED
}
