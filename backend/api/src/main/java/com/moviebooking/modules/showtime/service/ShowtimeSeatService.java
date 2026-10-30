package com.moviebooking.modules.showtime.service;

import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import com.moviebooking.seatengine.SeatLayout;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Cổng duy nhất để module khác (đặt vé, nhóm) thay đổi trạng thái ghế của suất chiếu. Mọi thay đổi phát
 * {@link com.moviebooking.modules.showtime.event.SeatStatesChangedEvent} để đẩy realtime sau khi commit.
 */
public interface ShowtimeSeatService {

    /**
     * Suất chiếu còn bán vé (chưa hủy, phòng/rạp hoạt động, chưa quá thời gian cho phép bán sau giờ chiếu), kèm
     * phim, phòng, rạp, giá.
     *
     * @throws com.moviebooking.exception.BusinessException SHOWTIME_NOT_FOUND nếu không còn bán
     */
    Showtime getBookable(Long showtimeId);

    /** Suất chiếu kèm phim, phòng, rạp, giá — không kiểm tra còn bán (để hiển thị lịch sử). */
    Showtime getShowtime(Long showtimeId);

    /**
     * Sơ đồ ghế của suất còn bán vé ở dạng đầu vào của thuật toán cụm ghế ({@code seat-engine}): mỗi ghế kèm
     * sức chứa (ghế đôi = 2), giá đã chốt của suất và trạng thái trống hiệu lực.
     *
     * @throws com.moviebooking.exception.BusinessException SHOWTIME_NOT_FOUND nếu suất không còn bán
     */
    SeatLayout seatLayout(Long showtimeId);

    /** seatId → trạng thái hiệu lực (ghế giữ quá hạn coi là AVAILABLE). */
    Map<Long, ShowtimeSeatStatus> getStates(Long showtimeId);

    /** @return ghế đã giữ được; nơi gọi phải rollback nếu thiếu ghế */
    List<Long> hold(Long showtimeId, Collection<Long> seatIds, Long holdId, Instant expiresAt);

    /** Trả ghế của lượt giữ về trống. */
    List<Long> release(Long showtimeId, Long holdId);

    List<Long> seatIdsOfHold(Long holdId);

    /** Hủy vé: ghế đã bán của đơn trở lại trống. */
    List<Long> releaseSold(Long showtimeId, Long orderId);

    /**
     * Chuyển ghế sang SOLD nếu tất cả còn "nhận được": đang giữ bởi chính {@code holdId}, hoặc trống, hoặc giữ đã
     * quá hạn (BR17). Không đổi gì nếu có ghế đã thuộc người khác.
     *
     * @return true nếu đã chuyển toàn bộ
     */
    boolean claim(Long showtimeId, Collection<Long> seatIds, Long holdId, Long orderId);
}
