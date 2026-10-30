package com.moviebooking.modules.showtime.dto.request;

import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;

/** Sửa suất chiếu chưa có ghế được giữ/bán (SCHADM02). Muốn đổi phim/phòng thì hủy và tạo suất mới. */
public record ShowtimeUpdateRequest(
        @NotNull(message = "Vui lòng chọn giờ chiếu") Instant startAt,
        @NotNull(message = "Vui lòng chọn định dạng") ScreenFormat format,
        @NotNull(message = "Vui lòng chọn phụ đề/lồng tiếng") AudioType audioType,
        Map<String, @NotNull @Positive Long> prices) {}
