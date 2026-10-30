package com.moviebooking.modules.review.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.review.dto.request.ReviewVisibilityRequest;
import com.moviebooking.modules.review.dto.response.ReviewResponse;
import com.moviebooking.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — đánh giá phim")
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasAuthority('movie:write')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Danh sách đánh giá mới nhất, lọc đã ẩn / đang hiện")
    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.search(hidden, page, size));
    }

    @Operation(summary = "Ẩn / hiện đánh giá vi phạm")
    @PutMapping("/{id}/visibility")
    @Audited(action = "REVIEW_VISIBILITY", resource = "review", resourceId = "#id", data = "#request")
    public ApiResponse<ReviewResponse> setHidden(@PathVariable Long id, @RequestBody ReviewVisibilityRequest request) {
        return ApiResponse.ok(reviewService.setHidden(id, request.hidden()));
    }
}
