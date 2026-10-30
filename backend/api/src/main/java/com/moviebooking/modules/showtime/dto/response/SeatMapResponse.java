package com.moviebooking.modules.showtime.dto.response;

import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import java.util.List;

/**
 * Sơ đồ ghế của một suất chiếu (SCH06, BKG01). Trạng thái là trạng thái hiệu lực: ghế giữ đã quá hạn được trả về
 * AVAILABLE.
 */
public record SeatMapResponse(
        Long showtimeId, int gridRows, int gridCols, List<SeatMapSeat> seats, List<SeatTypePrice> seatTypes) {

    public record SeatMapSeat(
            Long id,
            String rowLabel,
            int number,
            int gridRow,
            int gridCol,
            int colSpan,
            String seatTypeCode,
            ShowtimeSeatStatus status) {}

    /** Loại ghế có trong phòng kèm giá của suất này. */
    public record SeatTypePrice(String code, String name, int capacity, String color, long price) {}
}
