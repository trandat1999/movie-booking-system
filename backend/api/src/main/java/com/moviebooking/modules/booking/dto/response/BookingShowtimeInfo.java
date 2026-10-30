package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.Instant;

/** Thông tin suất chiếu hiển thị trên đơn/vé. */
public record BookingShowtimeInfo(
        Long id,
        Long movieId,
        String movieTitle,
        String posterUrl,
        AgeRating ageRating,
        Instant startAt,
        Instant endAt,
        ScreenFormat format,
        AudioType audioType,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        String roomName) {}
