package com.moviebooking.modules.booking.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.infrastructure.ratelimit.RateLimitProperties;
import com.moviebooking.infrastructure.ratelimit.RateLimiter;
import com.moviebooking.modules.booking.dto.request.HoldRequest;
import com.moviebooking.modules.booking.dto.response.HoldResponse;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Đặt vé")
@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    @Operation(summary = "Giữ ghế đã chọn (BKG02, BKG03) — giữ toàn bộ hoặc không giữ ghế nào")
    @PostMapping
    public ApiResponse<HoldResponse> hold(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody HoldRequest request) {
        rateLimiter.check("hold:" + user.id(), rateLimitProperties.holdPerMinute());
        return ApiResponse.ok(holdService.hold(user.id(), request));
    }

    @Operation(summary = "Bỏ giữ ghế (BKG04)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> release(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        holdService.release(user.id(), id);
        return ApiResponse.ok();
    }
}
