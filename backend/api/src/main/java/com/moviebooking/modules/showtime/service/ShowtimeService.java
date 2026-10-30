package com.moviebooking.modules.showtime.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.showtime.dto.request.ShowtimeRequest;
import com.moviebooking.modules.showtime.dto.request.ShowtimeUpdateRequest;
import com.moviebooking.modules.showtime.dto.response.ShowtimeResponse;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import java.time.LocalDate;

/** Quản lý lịch chiếu (SCHADM01–SCHADM09). */
public interface ShowtimeService {

    /** @param date ngày chiếu theo giờ Việt Nam (tùy chọn) */
    PageResponse<ShowtimeResponse> search(
            Long cinemaId, Long roomId, Long movieId, LocalDate date, ShowtimeStatus status, int page, int size);

    ShowtimeResponse get(Long id);

    /**
     * Tạo suất chiếu: kiểm tra phim/phòng, trùng lịch (tính cả thời gian dọn phòng), chốt giá, sinh trạng thái
     * ghế và khóa sơ đồ phòng.
     */
    ShowtimeResponse create(ShowtimeRequest request);

    /** Chỉ sửa được suất chưa bắt đầu, chưa hủy và chưa có ghế được giữ/bán. */
    ShowtimeResponse update(Long id, ShowtimeUpdateRequest request);

    /** Hủy suất chưa bắt đầu và chưa có ghế được giữ/bán (hoàn tiền tự động được bổ sung ở M7). */
    ShowtimeResponse cancel(Long id);
}
