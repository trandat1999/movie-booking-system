package com.moviebooking.modules.auth.dto.response;

import com.moviebooking.modules.user.dto.response.UserProfileResponse;
import java.time.Instant;

/**
 * Kết quả đăng nhập/refresh. Refresh token KHÔNG nằm trong body mà trong cookie httpOnly.
 *
 * @param accessToken JWT gửi kèm header {@code Authorization: Bearer ...}
 * @param expiresAt thời điểm access token hết hạn
 */
public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserProfileResponse user) {

    public static AuthResponse bearer(String accessToken, Instant expiresAt, UserProfileResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresAt, user);
    }
}
