package com.moviebooking.modules.showtime.dto.response;

import java.time.Instant;

/** Suất chiếu gây trùng lịch — trả trong {@code error.details} của SHOWTIME_OVERLAP. */
public record ConflictingShowtime(Long id, String movieTitle, Instant startAt, Instant endAt, Instant blockedUntil) {}
