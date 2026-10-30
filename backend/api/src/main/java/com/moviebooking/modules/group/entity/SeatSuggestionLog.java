package com.moviebooking.modules.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một lần đề xuất cụm ghế cho nhóm và phương án được chọn — số liệu cho ADM02 và thực nghiệm. */
@Getter
@Setter
@Entity
@Table(name = "seat_suggestion_logs")
public class SeatSuggestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> options;

    /** Lý do không có phương án (NOT_ENOUGH_SEATS / NO_CLUSTER). */
    private String reason;

    /** Thứ hạng (1..K) phương án trưởng nhóm giữ; 0 = tự chọn tay; null = chưa giữ. */
    @Column(name = "chosen_rank")
    private Integer chosenRank;

    /** Phương án tốt nhất là một cụm liền nhau cùng hàng. */
    @Column(nullable = false)
    private boolean contiguous;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
