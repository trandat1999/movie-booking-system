package com.moviebooking.modules.booking.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * @param warnings cảnh báo không chặn (ví dụ ghế lẻ khi quy tắc ghế lẻ = warn)
 * @param serverTime giờ máy chủ để client hiệu chỉnh đồng hồ đếm ngược
 */
public record HoldResponse(
        Long id,
        Long showtimeId,
        Instant expiresAt,
        List<SeatLine> seats,
        long total,
        List<String> warnings,
        Instant serverTime) {}
