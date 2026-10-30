package com.moviebooking.modules.showtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayRequest(
        @NotNull(message = "Vui lòng chọn ngày") LocalDate date,
        @NotBlank(message = "Vui lòng nhập tên ngày lễ") @Size(max = 100) String name) {}
