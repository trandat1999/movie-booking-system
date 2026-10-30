package com.moviebooking.modules.showtime.dto.request;

import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BasePriceItem(
        @NotNull ScreenFormat format,
        @NotNull DayType dayType,
        @NotNull @Min(value = 1000, message = "Giá tối thiểu 1.000đ") @Max(value = 10_000_000) Long price) {}
