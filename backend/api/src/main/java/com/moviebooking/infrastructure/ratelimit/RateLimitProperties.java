package com.moviebooking.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Giới hạn số yêu cầu mỗi phút (BR15), tiền tố {@code app.rate-limit.*}.
 *
 * @param loginPerMinute số lần đăng nhập / phút / địa chỉ IP
 * @param holdPerMinute số lần giữ ghế / phút / người dùng
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(@DefaultValue("10") int loginPerMinute, @DefaultValue("20") int holdPerMinute) {}
