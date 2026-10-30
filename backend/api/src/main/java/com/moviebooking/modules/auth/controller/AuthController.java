package com.moviebooking.modules.auth.controller;

import com.moviebooking.common.response.ApiError;
import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.auth.dto.request.LoginRequest;
import com.moviebooking.modules.auth.dto.request.RegisterRequest;
import com.moviebooking.modules.auth.dto.response.AuthResponse;
import com.moviebooking.modules.auth.service.AuthResult;
import com.moviebooking.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookies refreshTokenCookies;

    @Operation(summary = "Đăng ký tài khoản khách hàng và đăng nhập (USR01)")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResult result = authService.register(request, ClientInfo.from(httpRequest));
        return withSession(HttpStatus.CREATED, result);
    }

    @Operation(summary = "Đăng nhập bằng email hoặc số điện thoại (USR02)")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResult result = authService.login(request, ClientInfo.from(httpRequest));
        return withSession(HttpStatus.OK, result);
    }

    @Operation(summary = "Lấy access token mới bằng refresh token trong cookie")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest httpRequest) {
        String refreshToken = refreshTokenCookies
                .read(httpRequest)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        try {
            return withSession(HttpStatus.OK, authService.refresh(refreshToken, ClientInfo.from(httpRequest)));
        } catch (BusinessException ex) {
            // Phiên không còn hợp lệ: xóa cookie để client không gửi lại token hỏng
            return ResponseEntity.status(ex.getErrorCode().getStatus())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                    .body(ApiResponse.fail(new ApiError(
                            ex.getErrorCode().name(), ex.getMessage(), null)));
        }
    }

    @Operation(summary = "Đăng xuất (USR02)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        refreshTokenCookies.read(httpRequest).ifPresent(authService::logout);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .body(ApiResponse.ok());
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withSession(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(result.refreshToken()).toString())
                .body(ApiResponse.ok(result.response()));
    }
}
