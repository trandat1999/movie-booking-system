package com.moviebooking.modules.showtime.enums;

import java.time.Instant;

/** Trạng thái hiển thị (SCHADM08): sắp chiếu, đang chiếu, đã kết thúc, đã hủy. */
public enum ShowtimePhase {
    UPCOMING,
    ONGOING,
    ENDED,
    CANCELLED;

    public static ShowtimePhase of(ShowtimeStatus status, Instant startAt, Instant endAt, Instant now) {
        if (status == ShowtimeStatus.CANCELLED) {
            return CANCELLED;
        }
        if (now.isBefore(startAt)) {
            return UPCOMING;
        }
        return now.isBefore(endAt) ? ONGOING : ENDED;
    }
}
