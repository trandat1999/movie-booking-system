package com.moviebooking;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;

/**
 * Chạy backend ở máy dev KHÔNG cần Docker: khởi động PostgreSQL 16 nhúng (dữ liệu giữ lại ở
 * {@code backend/.dev-postgres}) rồi chạy ứng dụng với profile {@code dev}.
 *
 * <pre>
 * cd backend
 * ./mvnw -pl api -am spring-boot:test-run
 * </pre>
 *
 * <p>Redis không được khởi động; health check Redis bị tắt. Khi có Docker, dùng {@code docker compose up -d} và
 * {@code spring-boot:run} như bình thường.
 */
public final class TestMovieBookingApplication {

    private static final int PORT = 54329;

    private TestMovieBookingApplication() {}

    public static void main(String[] args) throws IOException {
        // spring-boot:test-run chạy với thư mục làm việc là backend/api → dữ liệu nằm ở backend/.dev-postgres
        Path dataDir = Path.of(System.getProperty("user.dir")).resolveSibling(".dev-postgres");
        Files.createDirectories(dataDir);

        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPort(PORT)
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (IOException ignored) {
                // đang tắt ứng dụng
            }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        System.setProperty("management.health.redis.enabled", "false");
        SpringApplication.run(MovieBookingApplication.class, args);
    }
}
