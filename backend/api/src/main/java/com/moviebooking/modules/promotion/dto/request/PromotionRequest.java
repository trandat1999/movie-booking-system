package com.moviebooking.modules.promotion.dto.request;

import com.moviebooking.modules.promotion.enums.PromotionStatus;
import com.moviebooking.modules.promotion.enums.PromotionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * @param value PERCENT: 1–100; AMOUNT: số tiền giảm (VND)
 * @param minTickets số vé tối thiểu của đơn — &gt; 1 là ưu đãi đặt vé nhóm
 */
public record PromotionRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Chỉ gồm chữ, số, - và _") String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull PromotionType type,
        @Positive long value,
        @Positive Long maxDiscount,
        @PositiveOrZero long minOrder,
        @Min(1) int minTickets,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Positive Integer usageLimit,
        @Positive Integer perUserLimit,
        @NotNull PromotionStatus status) {}
