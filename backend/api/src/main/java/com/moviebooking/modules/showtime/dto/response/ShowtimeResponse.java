package com.moviebooking.modules.showtime.dto.response;

import com.moviebooking.modules.cinema.enums.RoomType;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.enums.ShowtimePhase;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import java.time.Instant;
import java.util.List;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String moviePosterUrl,
        AgeRating ageRating,
        int durationMin,
        Long cinemaId,
        String cinemaName,
        Long roomId,
        String roomName,
        RoomType roomType,
        Instant startAt,
        Instant endAt,
        Instant blockedUntil,
        ScreenFormat format,
        AudioType audioType,
        DayType dayType,
        ShowtimeStatus status,
        ShowtimePhase phase,
        SeatStats seats,
        List<ShowtimePriceResponse> prices) {}
