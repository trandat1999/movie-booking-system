package com.moviebooking.modules.promotion.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.promotion.dto.request.PromotionRequest;
import com.moviebooking.modules.promotion.dto.response.PromotionResponse;
import com.moviebooking.modules.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — khuyến mãi")
@RestController
@RequestMapping("/api/v1/admin/promotions")
@PreAuthorize("hasAuthority('promotion:write')")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;

    @Operation(summary = "Danh sách mã giảm giá (PROADM)")
    @GetMapping
    public ApiResponse<PageResponse<PromotionResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(promotionService.search(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(promotionService.get(id));
    }

    @Operation(summary = "Tạo mã giảm giá — min_tickets > 1 là ưu đãi đặt vé nhóm")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "PROMOTION_CREATE", resource = "promotion", data = "#request")
    public ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.create(request));
    }

    @PutMapping("/{id}")
    @Audited(action = "PROMOTION_UPDATE", resource = "promotion", resourceId = "#id", data = "#request")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.update(id, request));
    }
}
