package com.moviebooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật {@code @Async} cho các listener chạy sau commit (email, thông báo, realtime). Kích thước thread pool cấu
 * hình qua {@code spring.task.execution.*}.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
