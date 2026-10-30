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
