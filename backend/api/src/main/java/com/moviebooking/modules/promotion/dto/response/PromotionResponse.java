package com.moviebooking.modules.promotion.dto.response;

import com.moviebooking.modules.promotion.enums.PromotionStatus;
import com.moviebooking.modules.promotion.enums.PromotionType;
import java.time.Instant;

public record PromotionResponse(
        Long id,
        String code,
        String name,
        String description,
        PromotionType type,
        long value,
        Long maxDiscount,
        long minOrder,
        int minTickets,
        Instant startsAt,
        Instant endsAt,
        Integer usageLimit,
        Integer perUserLimit,
        int usedCount,
        PromotionStatus status,
        Instant createdAt) {}
