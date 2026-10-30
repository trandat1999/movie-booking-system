package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.Positive;
import java.util.Set;

/**
 * Tùy chọn đề xuất (GRP11).
 *
 * @param maxPricePerPerson null = dùng ngân sách thấp nhất của các thành viên "Có" (nếu có)
 * @param seatTypes rỗng/null = mọi loại ghế
 */
public record SeatSuggestionRequest(boolean allowSplit, @Positive Long maxPricePerPerson, Set<String> seatTypes) {}
