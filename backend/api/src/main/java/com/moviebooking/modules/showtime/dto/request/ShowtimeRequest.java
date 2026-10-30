package com.moviebooking.modules.showtime.dto.request;

import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;

/**
 * Tạo suất chiếu (SCHADM01). Giờ kết thúc tính từ thời lượng phim. {@code prices} (tùy chọn) ghi đè giá tính từ
 * bảng giá cho từng mã loại ghế (SCHADM06).
 */
public record ShowtimeRequest(
        @NotNull(message = "Vui lòng chọn phim") Long movieId,
        @NotNull(message = "Vui lòng chọn phòng") Long roomId,
        @NotNull(message = "Vui lòng chọn giờ chiếu") Instant startAt,
        @NotNull(message = "Vui lòng chọn định dạng") ScreenFormat format,
        @NotNull(message = "Vui lòng chọn phụ đề/lồng tiếng") AudioType audioType,
        Map<String, @NotNull @Positive Long> prices) {}
