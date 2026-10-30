package com.moviebooking.modules.group.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.cinema.entity.Cinema;
import com.moviebooking.modules.group.enums.GroupStatus;
import com.moviebooking.modules.movie.entity.Movie;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Nhóm đặt vé (PLAN.md 6.3, 7). Trưởng nhóm là thành viên có vai trò LEADER và cũng là {@link #leaderId}. */
@Getter
@Setter
@Entity
@Table(name = "booking_groups")
public class BookingGroup extends BaseEntity {

    /** Mã mời 8 ký tự; tạo lại thì mã cũ hết hiệu lực. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    /** Hạn chốt: quá hạn mà nhóm chưa chốt → EXPIRED. */
    @Column(name = "rsvp_deadline", nullable = false)
    private Instant rsvpDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.OPEN;

    @Column(name = "locked_showtime_id")
    private Long lockedShowtimeId;

    @Column(name = "locked_ticket_count")
    private Integer lockedTicketCount;

    @Column(name = "hold_id")
    private Long holdId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;
}
