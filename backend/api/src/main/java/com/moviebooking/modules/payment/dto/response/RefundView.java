package com.moviebooking.modules.payment.dto.response;

import com.moviebooking.modules.payment.enums.RefundStatus;
import java.time.Instant;

public record RefundView(
        Long id,
        Long orderId,
        long amount,
        String reason,
        RefundStatus status,
        String provider,
        String note,
        Instant createdAt,
        Instant processedAt) {}
