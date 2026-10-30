package com.moviebooking.modules.review.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.review.dto.request.ReviewRequest;
import com.moviebooking.modules.review.dto.response.MovieReviewsResponse;
import com.moviebooking.modules.review.dto.response.MyReviewResponse;
import com.moviebooking.modules.review.dto.response.ReviewResponse;

/** Đánh giá phim (FBK01): chỉ người đã xem phim tại rạp (vé đã soát) được viết. */
public interface ReviewService {

    /** Đánh giá hiển thị của phim, mới nhất trước, kèm điểm trung bình và phân bố sao. */
    MovieReviewsResponse forMovie(String slug, int page, int size);

    MyReviewResponse mine(Long userId, Long movieId);

    /** Viết mới hoặc sửa đánh giá của mình. */
    ReviewResponse save(Long userId, Long movieId, ReviewRequest request);

    void deleteMine(Long userId, Long movieId);

    // --- Quản trị ---

    /** {@code hidden} null = tất cả. */
    PageResponse<ReviewResponse> search(Boolean hidden, int page, int size);

    ReviewResponse setHidden(Long reviewId, boolean hidden);
}
