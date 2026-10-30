package com.moviebooking.modules.review.dto.response;

import com.moviebooking.common.response.PageResponse;
import java.util.List;

/**
 * Đánh giá của một phim.
 *
 * @param average điểm trung bình (0 khi chưa có đánh giá), làm tròn 1 chữ số
 * @param distribution số đánh giá theo mức sao, phần tử 0 = 1 sao … phần tử 4 = 5 sao
 */
public record MovieReviewsResponse(double average, long count, List<Long> distribution, PageResponse<ReviewResponse> items) {}
