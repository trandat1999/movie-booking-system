package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.payment.enums.RefundStatus;
import java.time.Instant;

/** Yêu cầu hoàn tiền cho trang quản trị (ORDADM-R). */
public record AdminRefundView(
        Long id,
        String orderCode,
        Long userId,
        long orderTotal,
        long amount,
        String reason,
        RefundStatus status,
        String provider,
        String note,
        Instant createdAt,
        Instant processedAt) {}
