package com.moviebooking.modules.showtime.repository;

import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * Trạng thái ghế theo suất chiếu ({@code showtime_seats}). Dùng SQL trực tiếp vì đây là bảng nóng nhất khi giữ
 * ghế/bán vé: cần câu lệnh UPDATE có điều kiện và thao tác hàng loạt (PLAN.md mục 8.1).
 */
@Repository
@RequiredArgsConstructor
public class ShowtimeSeatRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record SeatState(Long seatId, ShowtimeSeatStatus status) {}

    /** Dòng ghế đã khóa để chuyển sang SOLD. */
    public record LockedSeat(Long seatId, ShowtimeSeatStatus status, Long holdId, Instant heldUntil) {}

    /**
     * Giữ ghế nguyên tử (PLAN.md 8.1): chỉ cập nhật ghế còn trống hoặc đang giữ nhưng đã quá hạn. Hai giao dịch
     * tranh cùng ghế thì giao dịch sau chờ khóa dòng, đánh giá lại điều kiện và không cập nhật được. Dòng được
     * khóa theo thứ tự {@code seat_id} để hai lượt giữ chồng một phần ghế không deadlock.
     *
     * @return id các ghế đã giữ được — ít hơn số yêu cầu nghĩa là có ghế đã bị người khác giữ/bán; nơi gọi phải
     *     rollback để không giữ được một phần (BR03)
     */
    public List<Long> hold(Long showtimeId, Collection<Long> seatIds, Long holdId, Instant expiresAt, Instant now) {
        return jdbc.queryForList(
                """
                UPDATE showtime_seats ss
                SET status = 'HELD', hold_id = :holdId, held_until = :expiresAt, version = ss.version + 1
                FROM (SELECT seat_id FROM showtime_seats
                      WHERE showtime_id = :showtimeId AND seat_id IN (:seatIds)
                      ORDER BY seat_id FOR UPDATE) locked
                WHERE ss.showtime_id = :showtimeId AND ss.seat_id = locked.seat_id
                  AND (ss.status = 'AVAILABLE' OR (ss.status = 'HELD' AND ss.held_until < :now))
                RETURNING ss.seat_id
                """,
                new MapSqlParameterSource()
                        .addValue("showtimeId", showtimeId)
                        .addValue("seatIds", seatIds)
                        .addValue("holdId", holdId)
                        .addValue("expiresAt", Timestamp.from(expiresAt))
                        .addValue("now", Timestamp.from(now)),
                Long.class);
    }

    /** Trả các ghế còn đang giữ bởi {@code holdId} về trống. */
    public List<Long> release(Long holdId) {
        return jdbc.queryForList(
                """
                UPDATE showtime_seats
                SET status = 'AVAILABLE', hold_id = NULL, held_until = NULL, version = version + 1
                WHERE hold_id = :holdId AND status = 'HELD'
                RETURNING seat_id
                """,
                new MapSqlParameterSource("holdId", holdId),
                Long.class);
    }

    /** Trả các ghế đã bán của đơn về trống (hủy vé, BR10). */
    public List<Long> releaseSold(Long showtimeId, Long orderId) {
        return jdbc.queryForList(
                """
                UPDATE showtime_seats
                SET status = 'AVAILABLE', hold_id = NULL, held_until = NULL, order_id = NULL, version = version + 1
                WHERE showtime_id = :showtimeId AND order_id = :orderId AND status = 'SOLD'
                RETURNING seat_id
                """,
                new MapSqlParameterSource().addValue("showtimeId", showtimeId).addValue("orderId", orderId),
                Long.class);
    }

    /** Ghế đang thuộc lượt giữ (kể cả đã quá hạn nhưng chưa bị người khác lấy). */
    public List<Long> seatIdsOfHold(Long holdId) {
        return jdbc.queryForList(
                "SELECT seat_id FROM showtime_seats WHERE hold_id = :holdId AND status = 'HELD' ORDER BY seat_id",
                new MapSqlParameterSource("holdId", holdId),
                Long.class);
    }

    /** Khóa các dòng ghế (SELECT ... FOR UPDATE) trước khi chuyển sang SOLD. */
    public List<LockedSeat> lockSeats(Long showtimeId, Collection<Long> seatIds) {
        return jdbc.query(
                """
                SELECT seat_id, status, hold_id, held_until FROM showtime_seats
                WHERE showtime_id = :showtimeId AND seat_id IN (:seatIds)
                ORDER BY seat_id
                FOR UPDATE
                """,
                new MapSqlParameterSource().addValue("showtimeId", showtimeId).addValue("seatIds", seatIds),
                (rs, i) -> new LockedSeat(
                        rs.getLong("seat_id"),
                        ShowtimeSeatStatus.valueOf(rs.getString("status")),
                        (Long) rs.getObject("hold_id"),
                        rs.getTimestamp("held_until") == null ? null : rs.getTimestamp("held_until").toInstant()));
    }

    public int markSold(Long showtimeId, Collection<Long> seatIds, Long holdId, Long orderId) {
        return jdbc.update(
                """
                UPDATE showtime_seats
                SET status = 'SOLD', hold_id = :holdId, held_until = NULL, order_id = :orderId, version = version + 1
                WHERE showtime_id = :showtimeId AND seat_id IN (:seatIds)
                """,
                new MapSqlParameterSource()
                        .addValue("showtimeId", showtimeId)
                        .addValue("seatIds", seatIds)
                        .addValue("holdId", holdId)
                        .addValue("orderId", orderId));
    }

    /** Tạo trạng thái ban đầu cho mọi ghế của suất chiếu. */
    public void insertAll(Long showtimeId, List<SeatState> seats) {
        SqlParameterSource[] batch = seats.stream()
                .map(seat -> new MapSqlParameterSource()
                        .addValue("showtimeId", showtimeId)
                        .addValue("seatId", seat.seatId())
                        .addValue("status", seat.status().name()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(
                "INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES (:showtimeId, :seatId, :status)",
                batch);
    }

    /** Đếm ghế theo trạng thái cho nhiều suất; ghế HELD đã quá hạn được tính là AVAILABLE (BR02). */
    public Map<Long, Map<ShowtimeSeatStatus, Long>> countByStatus(Collection<Long> showtimeIds, Instant now) {
        Map<Long, Map<ShowtimeSeatStatus, Long>> result = new HashMap<>();
        if (showtimeIds.isEmpty()) {
            return result;
        }
        jdbc.query(
                """
                SELECT showtime_id,
                       CASE WHEN status = 'HELD' AND held_until < :now THEN 'AVAILABLE' ELSE status END AS effective,
                       count(*) AS total
                FROM showtime_seats
                WHERE showtime_id IN (:ids)
                GROUP BY showtime_id, effective
                """,
                new MapSqlParameterSource().addValue("ids", showtimeIds).addValue("now", Timestamp.from(now)),
                rs -> {
                    result.computeIfAbsent(rs.getLong("showtime_id"), id -> new EnumMap<>(ShowtimeSeatStatus.class))
                            .put(ShowtimeSeatStatus.valueOf(rs.getString("effective")), rs.getLong("total"));
                });
        return result;
    }

    /** Trạng thái hiệu lực của từng ghế trong suất: seatId → trạng thái (HELD quá hạn → AVAILABLE). */
    public Map<Long, ShowtimeSeatStatus> findStates(Long showtimeId, Instant now) {
        Map<Long, ShowtimeSeatStatus> states = new HashMap<>();
        jdbc.query(
                """
                SELECT seat_id,
                       CASE WHEN status = 'HELD' AND held_until < :now THEN 'AVAILABLE' ELSE status END AS effective
                FROM showtime_seats WHERE showtime_id = :id
                """,
                new MapSqlParameterSource().addValue("id", showtimeId).addValue("now", Timestamp.from(now)),
                rs -> {
                    states.put(rs.getLong("seat_id"), ShowtimeSeatStatus.valueOf(rs.getString("effective")));
                });
        return states;
    }

    /** Có ghế đang được giữ (chưa hết hạn) hoặc đã bán hay không. */
    public boolean hasHeldOrSoldSeats(Long showtimeId, Instant now) {
        Boolean exists = jdbc.queryForObject(
                """
                SELECT EXISTS (SELECT 1 FROM showtime_seats WHERE showtime_id = :id
                               AND (status = 'SOLD' OR (status = 'HELD' AND held_until >= :now)))
                """,
                new MapSqlParameterSource().addValue("id", showtimeId).addValue("now", Timestamp.from(now)),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Ghế bị tắt/bật bán → cập nhật các suất chưa bắt đầu của phòng. Chỉ đổi giữa AVAILABLE và BLOCKED; ghế đang
     * giữ/đã bán giữ nguyên.
     */
    public int setSeatBlocked(Long seatId, boolean blocked, Instant now) {
        return jdbc.update(
                """
                UPDATE showtime_seats ss SET status = :to, version = version + 1
                FROM showtimes s
                WHERE ss.showtime_id = s.id AND ss.seat_id = :seatId AND ss.status = :from
                  AND s.status = 'SCHEDULED' AND s.start_at > :now
                """,
                new MapSqlParameterSource()
                        .addValue("seatId", seatId)
                        .addValue("from", blocked ? "AVAILABLE" : "BLOCKED")
                        .addValue("to", blocked ? "BLOCKED" : "AVAILABLE")
                        .addValue("now", Timestamp.from(now)));
    }
}
