package com.moviebooking.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình riêng của ứng dụng, tiền tố {@code app.*} trong application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors) {

    public record Cors(List<String> allowedOrigins) {}
}
