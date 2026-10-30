package com.moviebooking.infrastructure.ratelimit;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Giới hạn tần suất bằng token bucket (Bucket4j) lưu trong bộ nhớ. Đủ cho một instance; khi chạy nhiều instance
 * thay bằng Bucket4j + Redis (PLAN.md mục 2).
 */
@Component
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key ví dụ {@code login:127.0.0.1}, {@code hold:42}
     * @throws BusinessException TOO_MANY_REQUESTS khi vượt giới hạn
     */
    public void check(String key, int perMinute) {
        Bucket bucket = buckets.computeIfAbsent(key + "@" + perMinute, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(perMinute)
                        .refillGreedy(perMinute, Duration.ofMinutes(1))
                        .build())
                .build());
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
