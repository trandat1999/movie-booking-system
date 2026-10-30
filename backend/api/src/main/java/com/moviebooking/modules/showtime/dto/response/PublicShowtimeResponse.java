package com.moviebooking.modules.showtime.dto.response;

import com.moviebooking.modules.cinema.enums.RoomType;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.Instant;

/**
 * Suất chiếu hiển thị cho khách (SCH03, SCH05).
 *
 * @param availableSeats số ghế còn trống (đã trừ ghế đang giữ, đã bán, ngừng bán)
 * @param minPrice giá thấp nhất trong các loại ghế của suất
 */
public record PublicShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String movieSlug,
        String moviePosterUrl,
        AgeRating ageRating,
        int durationMin,
        Long cinemaId,
        String cinemaName,
        String city,
        Long roomId,
        String roomName,
        RoomType roomType,
        Instant startAt,
        Instant endAt,
        ScreenFormat format,
        AudioType audioType,
        long availableSeats,
        long minPrice) {}
