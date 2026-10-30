package com.moviebooking.modules.showtime.dto.response;

import java.util.List;

/** Thông tin suất chiếu kèm địa chỉ rạp và giá từng loại ghế (SCH05, SCH07). */
public record PublicShowtimeDetailResponse(
        PublicShowtimeResponse showtime, String cinemaAddress, List<ShowtimePriceResponse> prices) {}
