package com.moviebooking.modules.showtime.dto.response;

import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.Instant;

public record BasePriceResponse(ScreenFormat format, DayType dayType, long price, Instant updatedAt) {}
