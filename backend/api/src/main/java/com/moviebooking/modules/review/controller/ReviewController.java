package com.moviebooking.modules.review.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.review.dto.request.ReviewRequest;
import com.moviebooking.modules.review.dto.response.MovieReviewsResponse;
import com.moviebooking.modules.review.dto.response.MyReviewResponse;
import com.moviebooking.modules.review.dto.response.ReviewResponse;
import com.moviebooking.modules.review.service.ReviewService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Đánh giá phim")
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Đánh giá của phim, điểm trung bình (FBK01) — công khai")
    @GetMapping("/{slug}/reviews")
    public ApiResponse<MovieReviewsResponse> list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.forMovie(slug, page, size));
    }

    @Operation(summary = "Đánh giá của tôi cho phim + có được viết không")
    @GetMapping("/{movieId}/my-review")
    public ApiResponse<MyReviewResponse> mine(@CurrentUser AuthenticatedUser user, @PathVariable Long movieId) {
        return ApiResponse.ok(reviewService.mine(user.id(), movieId));
    }

    @Operation(summary = "Viết / sửa đánh giá (chỉ khi đã xem phim tại rạp)")
    @PutMapping("/{movieId}/my-review")
    public ApiResponse<ReviewResponse> save(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long movieId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.save(user.id(), movieId, request));
    }

    @Operation(summary = "Xóa đánh giá của tôi")
    @DeleteMapping("/{movieId}/my-review")
    public ApiResponse<Void> delete(@CurrentUser AuthenticatedUser user, @PathVariable Long movieId) {
        reviewService.deleteMine(user.id(), movieId);
        return ApiResponse.ok();
    }
}
