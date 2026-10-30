package com.moviebooking.modules.group.dto.response;

import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.Instant;

public record GroupShowtimeInfo(
        Long id,
        Instant startAt,
        Instant endAt,
        ScreenFormat format,
        AudioType audioType,
        Long cinemaId,
        String cinemaName,
        String roomName) {}
