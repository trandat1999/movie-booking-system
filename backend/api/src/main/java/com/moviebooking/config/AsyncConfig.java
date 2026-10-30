package com.moviebooking.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bật {@code @Async} cho các listener chạy sau commit (email, thông báo, realtime). Kích thước thread pool cấu
 * hình qua {@code spring.task.execution.*}.
 *
 * <p>Khai báo executor tường minh: cấu hình STOMP tạo sẵn các executor riêng nên Spring Boot không tạo
 * {@code applicationTaskExecutor}, khi đó {@code @Async} sẽ rơi về {@code SimpleAsyncTaskExecutor} — mỗi tác vụ một
 * luồng mới, không giới hạn (phát hiện khi kiểm thử tải M9).
 *
 * <p>Môi trường test ({@code app.async.synchronous=true}) chạy {@code @Async} ngay trên luồng gọi để kiểm tra kết
 * quả (thông báo, email) một cách xác định.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final ThreadPoolTaskExecutorBuilder builder;
    private final boolean synchronous;

    public AsyncConfig(
            ThreadPoolTaskExecutorBuilder builder, @Value("${app.async.synchronous:false}") boolean synchronous) {
        this.builder = builder;
        this.synchronous = synchronous;
    }

    @Override
    public Executor getAsyncExecutor() {
        if (synchronous) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = builder.build();
        executor.initialize();
        return executor;
    }
}
