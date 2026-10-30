package com.moviebooking.modules.review.dto.response;

/**
 * @param eligible đã xem phim (có vé đã soát) nên được viết đánh giá
 * @param review đánh giá hiện có của tôi, {@code null} nếu chưa viết
 */
public record MyReviewResponse(boolean eligible, ReviewResponse review) {}
