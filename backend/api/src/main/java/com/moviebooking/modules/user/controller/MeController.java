package com.moviebooking.modules.user.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.user.dto.request.ChangePasswordRequest;
import com.moviebooking.modules.user.dto.request.UpdateProfileRequest;
import com.moviebooking.modules.user.dto.response.UserProfileResponse;
import com.moviebooking.modules.user.service.UserService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tài khoản của tôi")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @Operation(summary = "Xem hồ sơ (USR05)")
    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.ok(userService.getProfile(user.id()));
    }

    @Operation(summary = "Cập nhật hồ sơ (USR05)")
    @PatchMapping
    public ApiResponse<UserProfileResponse> updateProfile(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(user.id(), request));
    }

    @Operation(summary = "Đổi mật khẩu (USR06) — mọi phiên đăng nhập bị thu hồi, cần đăng nhập lại")
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
        return ApiResponse.ok();
    }
}
