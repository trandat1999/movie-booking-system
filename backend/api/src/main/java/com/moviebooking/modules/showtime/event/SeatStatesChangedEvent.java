package com.moviebooking.modules.showtime.event;

import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import java.util.Map;

/** Trạng thái một số ghế của suất chiếu đã đổi — đẩy realtime tới người đang xem sơ đồ ghế sau khi commit. */
public record SeatStatesChangedEvent(Long showtimeId, Map<Long, ShowtimeSeatStatus> seats) {}
