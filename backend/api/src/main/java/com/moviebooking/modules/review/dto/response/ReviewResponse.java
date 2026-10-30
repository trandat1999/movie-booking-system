package com.moviebooking.modules.review.dto.response;

import java.time.Instant;

/** @param movieTitle dùng ở trang quản trị (danh sách đánh giá của mọi phim) */
public record ReviewResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long userId,
        String userName,
        int rating,
        String comment,
        boolean hidden,
        Instant createdAt,
        Instant updatedAt) {}
