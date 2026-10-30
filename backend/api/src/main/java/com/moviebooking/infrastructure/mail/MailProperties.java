package com.moviebooking.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình gửi email, tiền tố {@code app.mail.*}.
 *
 * @param enabled false: chỉ ghi email ra log (máy dev không có SMTP); true: gửi qua {@code spring.mail.*}
 * @param from địa chỉ người gửi
 * @param webBaseUrl địa chỉ trang web khách hàng, dùng cho liên kết trong email
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("MovieBooking <no-reply@moviebooking.local>") String from,
        @DefaultValue("http://localhost:4200") String webBaseUrl) {}
