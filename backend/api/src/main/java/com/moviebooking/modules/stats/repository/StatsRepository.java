package com.moviebooking.modules.stats.repository;

import com.moviebooking.modules.stats.dto.response.GroupStats;
import com.moviebooking.modules.stats.dto.response.OverviewStats;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn báo cáo chỉ đọc (ADM01–ADM03). Module thống kê là "read model": đọc trực tiếp bảng của các module khác
 * bằng SQL tổng hợp, không dùng repository/entity của chúng và không ghi gì (PLAN.md 2A — ngoại lệ có chủ đích của
 * L4, xem quyết định M8).
 */
@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private static final String PAID = "o.status IN ('PAID', 'COMPLETED')";
    private static final String TICKETS_OF_ORDER =
            "(SELECT count(*) FROM tickets t WHERE t.order_id = o.id AND t.status IN ('VALID', 'USED'))";

    private final NamedParameterJdbcTemplate jdbc;

    /** Bộ lọc chung: khoảng thời gian [from, to) và tùy chọn rạp/phim. */
    public record Filter(Instant from, Instant to, Long cinemaId, Long movieId) {

        MapSqlParameterSource params() {
            return new MapSqlParameterSource()
                    .addValue("from", Timestamp.from(from))
                    .addValue("to", Timestamp.from(to))
                    .addValue("cinemaId", cinemaId)
                    .addValue("movieId", movieId);
        }

        /** Điều kiện rạp/phim trên bí danh {@code s} (showtimes) và {@code r} (rooms). */
        String scope() {
            return (cinemaId != null ? " AND r.cinema_id = :cinemaId" : "") + (movieId != null ? " AND s.movie_id = :movieId" : "");
        }
    }

    private static final String ORDER_JOIN =
            " FROM orders o JOIN showtimes s ON s.id = o.showtime_id JOIN rooms r ON r.id = s.room_id ";

    public long[] sales(Filter f) {
        return jdbc.queryForObject(
                "SELECT count(*), coalesce(sum(o.total), 0), coalesce(sum(o.discount), 0), coalesce(sum(" + TICKETS_OF_ORDER + "), 0)"
                        + ORDER_JOIN + "WHERE " + PAID + " AND o.paid_at >= :from AND o.paid_at < :to" + f.scope(),
                f.params(),
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)});
    }

    public long[] refunds(Filter f) {
        return jdbc.queryForObject(
                "SELECT count(*), coalesce(sum(fd.amount), 0) FROM refunds fd JOIN orders o ON o.id = fd.order_id "
                        + "JOIN showtimes s ON s.id = o.showtime_id JOIN rooms r ON r.id = s.room_id "
                        + "WHERE fd.status = 'COMPLETED' AND fd.processed_at >= :from AND fd.processed_at < :to" + f.scope(),
                f.params(),
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)});
    }

    public long[] payments(Filter f) {
        return jdbc.queryForObject(
                "SELECT count(*), count(*) FILTER (WHERE p.status = 'FAILED') FROM payments p "
                        + "JOIN orders o ON o.id = p.order_id JOIN showtimes s ON s.id = o.showtime_id "
                        + "JOIN rooms r ON r.id = s.room_id WHERE p.created_at >= :from AND p.created_at < :to" + f.scope(),
                f.params(),
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)});
    }

    public long expiredOrders(Filter f) {
        Long count = jdbc.queryForObject(
                "SELECT count(*)" + ORDER_JOIN + "WHERE o.status = 'EXPIRED' AND o.created_at >= :from AND o.created_at < :to"
                        + f.scope(),
                f.params(),
                Long.class);
        return count == null ? 0 : count;
    }

    /** Ghế đã bán / ghế bán được của các suất (chưa hủy) bắt đầu trong khoảng. */
    public long[] occupancy(Filter f) {
        return jdbc.queryForObject(
                "SELECT count(*) FILTER (WHERE ss.status = 'SOLD'), count(*) FILTER (WHERE ss.status <> 'BLOCKED') "
                        + "FROM showtime_seats ss JOIN showtimes s ON s.id = ss.showtime_id JOIN rooms r ON r.id = s.room_id "
                        + "WHERE s.status <> 'CANCELLED' AND s.start_at >= :from AND s.start_at < :to" + f.scope(),
                f.params(),
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)});
    }

    public List<OverviewStats.DailyPoint> daily(Filter f) {
        return jdbc.query(
                "SELECT (o.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS d, count(*), coalesce(sum(" + TICKETS_OF_ORDER
                        + "), 0), coalesce(sum(o.total), 0)" + ORDER_JOIN + "WHERE " + PAID
                        + " AND o.paid_at >= :from AND o.paid_at < :to" + f.scope() + " GROUP BY d ORDER BY d",
                f.params(),
                (rs, i) -> new OverviewStats.DailyPoint(
                        rs.getObject(1, LocalDate.class), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
    }

    public List<OverviewStats.Breakdown> byMovie(Filter f, int limit) {
        return jdbc.query(
                "SELECT m.id, m.title, count(*), coalesce(sum(" + TICKETS_OF_ORDER + "), 0), coalesce(sum(o.total), 0)"
                        + ORDER_JOIN + "JOIN movies m ON m.id = s.movie_id WHERE " + PAID
                        + " AND o.paid_at >= :from AND o.paid_at < :to" + f.scope()
                        + " GROUP BY m.id, m.title ORDER BY 5 DESC LIMIT " + limit,
                f.params(),
                (rs, i) -> new OverviewStats.Breakdown(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)));
    }

    public List<OverviewStats.Breakdown> byCinema(Filter f) {
        return jdbc.query(
                "SELECT c.id, c.name, count(*), coalesce(sum(" + TICKETS_OF_ORDER + "), 0), coalesce(sum(o.total), 0)"
                        + ORDER_JOIN + "JOIN cinemas c ON c.id = r.cinema_id WHERE " + PAID
                        + " AND o.paid_at >= :from AND o.paid_at < :to" + f.scope()
                        + " GROUP BY c.id, c.name ORDER BY 5 DESC",
                f.params(),
                (rs, i) -> new OverviewStats.Breakdown(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)));
    }

    // --- Nhóm (ADM02): lọc theo thời điểm tạo nhóm và phim ---

    private static String groupScope(Filter f) {
        return " g.created_at >= :from AND g.created_at < :to" + (f.movieId() != null ? " AND g.movie_id = :movieId" : "");
    }

    public Map<String, Long> groupsByStatus(Filter f) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT g.status, count(*) FROM booking_groups g WHERE" + groupScope(f) + " GROUP BY g.status",
                f.params(),
                rs -> {
                    result.put(rs.getString(1), rs.getLong(2));
                });
        return result;
    }

    /** [số vé TB của nhóm đã đặt, số thành viên TB, số giờ TB từ tạo tới thanh toán (null nếu chưa có)] */
    public Double[] groupAverages(Filter f) {
        Double avgTickets = jdbc.queryForObject(
                "SELECT avg(g.locked_ticket_count) FROM booking_groups g WHERE g.status IN ('BOOKED', 'COMPLETED') AND"
                        + groupScope(f),
                f.params(),
                Double.class);
        Double avgMembers = jdbc.queryForObject(
                "SELECT avg(cnt) FROM (SELECT count(m.id) AS cnt FROM booking_groups g JOIN group_members m "
                        + "ON m.group_id = g.id AND m.left_at IS NULL WHERE" + groupScope(f) + " GROUP BY g.id) x",
                f.params(),
                Double.class);
        Double avgHours = jdbc.queryForObject(
                "SELECT avg(extract(epoch FROM (e.created_at - g.created_at)) / 3600.0) FROM booking_groups g "
                        + "JOIN group_events e ON e.group_id = g.id AND e.type = 'PAID' WHERE" + groupScope(f),
                f.params(),
                Double.class);
        return new Double[] {avgTickets, avgMembers, avgHours};
    }

    public GroupStats.SuggestionStats suggestions(Filter f) {
        return jdbc.queryForObject(
                "SELECT count(*), count(*) FILTER (WHERE l.contiguous), count(*) FILTER (WHERE l.reason IS NOT NULL), "
                        + "coalesce(avg(l.duration_ms), 0), count(*) FILTER (WHERE l.chosen_rank IS NOT NULL), "
                        + "count(*) FILTER (WHERE l.chosen_rank = 1), count(*) FILTER (WHERE l.chosen_rank = 0) "
                        + "FROM seat_suggestion_logs l JOIN booking_groups g ON g.id = l.group_id "
                        + "WHERE l.created_at >= :from AND l.created_at < :to"
                        + (f.movieId() != null ? " AND g.movie_id = :movieId" : ""),
                f.params(),
                (rs, i) -> {
                    long count = rs.getLong(1);
                    long holds = rs.getLong(5);
                    return new GroupStats.SuggestionStats(
                            count,
                            ratio(rs.getLong(2), count),
                            rs.getLong(3),
                            rs.getDouble(4),
                            holds,
                            ratio(rs.getLong(6), holds),
                            ratio(rs.getLong(7), holds));
                });
    }

    public List<GroupStats.Bucket> sizeBuckets(Filter f) {
        return jdbc.query(
                "SELECT CASE WHEN g.locked_ticket_count <= 3 THEN '2–3 vé' WHEN g.locked_ticket_count <= 5 THEN '4–5 vé' "
                        + "WHEN g.locked_ticket_count <= 9 THEN '6–9 vé' ELSE '10+ vé' END AS b, count(*), "
                        + "min(g.locked_ticket_count) FROM booking_groups g WHERE g.status IN ('BOOKED', 'COMPLETED') AND"
                        + groupScope(f) + " GROUP BY b ORDER BY 3",
                f.params(),
                (rs, i) -> new GroupStats.Bucket(rs.getString(1), rs.getLong(2)));
    }

    /** Lượt giữ ghế nhóm hết hạn (không thanh toán kịp). */
    public long groupHoldsExpired(Filter f) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM group_events e JOIN booking_groups g ON g.id = e.group_id "
                        + "WHERE e.type = 'HOLD_EXPIRED' AND" + groupScope(f),
                f.params(),
                Long.class);
        return count == null ? 0 : count;
    }

    // --- Xuất CSV (ADM03) ---

    public record OrderExportRow(
            String code,
            Instant createdAt,
            Instant paidAt,
            String status,
            String customerEmail,
            String customerName,
            String movie,
            String cinema,
            Instant startAt,
            long tickets,
            long subtotal,
            long discount,
            long total,
            String promotionCode,
            boolean group) {}

    public List<OrderExportRow> exportOrders(Filter f) {
        return jdbc.query(
                "SELECT o.code, o.created_at, o.paid_at, o.status, u.email, u.full_name, m.title, c.name, s.start_at, "
                        + "(SELECT count(*) FROM order_items i WHERE i.order_id = o.id), o.subtotal, o.discount, o.total, "
                        + "o.promotion_code, o.group_id IS NOT NULL" + ORDER_JOIN
                        + "JOIN movies m ON m.id = s.movie_id JOIN cinemas c ON c.id = r.cinema_id "
                        + "JOIN users u ON u.id = o.user_id WHERE o.created_at >= :from AND o.created_at < :to" + f.scope()
                        + " ORDER BY o.created_at",
                f.params(),
                (rs, i) -> new OrderExportRow(
                        rs.getString(1),
                        rs.getTimestamp(2).toInstant(),
                        rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant(),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getTimestamp(9).toInstant(),
                        rs.getLong(10),
                        rs.getLong(11),
                        rs.getLong(12),
                        rs.getLong(13),
                        rs.getString(14),
                        rs.getBoolean(15)));
    }

    private static double ratio(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }
}
