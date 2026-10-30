package com.moviebooking.modules.showtime.service;

import com.moviebooking.modules.showtime.dto.response.PublicShowtimeDetailResponse;
import com.moviebooking.modules.showtime.dto.response.PublicShowtimeResponse;
import com.moviebooking.modules.showtime.dto.response.SeatMapResponse;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * Tra cứu lịch chiếu cho khách (SCH03–SCH07). Chỉ trả về suất còn bán vé: chưa hủy, phòng/rạp đang hoạt động,
 * và chưa quá {@code sale_close_after_start_min} phút sau giờ chiếu (BR16).
 */
public interface PublicShowtimeService {

    /** @param date ngày chiếu theo giờ Việt Nam; null = hôm nay */
    List<PublicShowtimeResponse> search(Long movieId, Long cinemaId, LocalDate date, ScreenFormat format);

    PublicShowtimeDetailResponse get(Long showtimeId);

    SeatMapResponse getSeatMap(Long showtimeId);
}
