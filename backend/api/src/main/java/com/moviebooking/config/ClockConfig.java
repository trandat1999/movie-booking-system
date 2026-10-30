package com.moviebooking.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nguồn thời gian duy nhất (UTC) — inject {@link Clock} thay vì gọi {@code Instant.now()} để dễ test. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
