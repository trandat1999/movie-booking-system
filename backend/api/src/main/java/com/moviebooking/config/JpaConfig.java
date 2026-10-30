package com.moviebooking.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@code created_at}/{@code updated_at} lấy giờ từ {@link Clock} của ứng dụng — cùng nguồn với mọi mốc thời gian
 * nghiệp vụ khác (giữ ghế, thanh toán), để báo cáo theo ngày nhất quán và test điều khiển được thời gian.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
