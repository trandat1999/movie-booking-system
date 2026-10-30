package com.moviebooking.modules.showtime.dto.response;

import com.moviebooking.modules.showtime.enums.DayType;
import java.time.Instant;
import java.util.List;

/** Giá dự kiến khi tạo suất chiếu: loại ngày và giá từng loại ghế có trong phòng. */
public record PriceQuoteResponse(DayType dayType, Instant endAt, List<ShowtimePriceResponse> prices) {}
