package com.moviebooking.modules.cinema.dto.request;

import jakarta.validation.constraints.NotNull;

/** Bật/tắt bán một ghế (ghế hỏng) — CIN06. */
public record SeatStatusRequest(@NotNull(message = "Vui lòng chọn trạng thái ghế") Boolean active) {}
