package com.moviebooking.modules.auth.controller;

import com.moviebooking.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh token nằm trong cookie httpOnly (JavaScript không đọc được), SameSite=Strict (chống CSRF), chỉ gửi tới
 * {@code /api/v1/auth}.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookies {

    static final String COOKIE_PATH = "/api/v1/auth";

    private final SecurityProperties securityProperties;

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, securityProperties.refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> securityProperties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(securityProperties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(securityProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
