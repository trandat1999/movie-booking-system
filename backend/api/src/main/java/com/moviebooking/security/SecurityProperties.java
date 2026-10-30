package com.moviebooking.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình xác thực, tiền tố {@code app.security.*}.
 *
 * @param jwt cấu hình access token
 * @param refreshTokenTtl thời hạn refresh token
 * @param refreshTokenReuseGrace khoảng thời gian một refresh token vừa được xoay vòng vẫn được chấp nhận (tránh
 *     đăng xuất nhầm khi nhiều tab gọi refresh cùng lúc); quá khoảng này mà dùng lại thì coi là bị đánh cắp
 * @param refreshCookieName tên cookie chứa refresh token
 * @param refreshCookieSecure bật cờ Secure cho cookie (bắt buộc khi chạy HTTPS)
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Jwt jwt,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue("30s") Duration refreshTokenReuseGrace,
        @DefaultValue("mb_refresh") String refreshCookieName,
        @DefaultValue("true") boolean refreshCookieSecure) {

    /**
     * @param secret khóa HMAC dạng Base64, tối thiểu 256 bit
     * @param accessTokenTtl thời hạn access token
     * @param issuer giá trị claim {@code iss}
     */
    public record Jwt(
            String secret,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("movie-booking") String issuer) {}
}
