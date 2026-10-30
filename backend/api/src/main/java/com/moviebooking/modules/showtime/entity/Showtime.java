package com.moviebooking.modules.showtime.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.cinema.entity.Room;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Suất chiếu. {@code blockedUntil = endAt + thời gian dọn phòng}; CSDL chặn hai suất chưa hủy cùng phòng có
 * khoảng {@code [startAt, blockedUntil)} chồng nhau.
 */
@Getter
@Setter
@Entity
@Table(name = "showtimes")
public class Showtime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "blocked_until", nullable = false)
    private Instant blockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreenFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_type", nullable = false)
    private AudioType audioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    /** Giá đã chốt theo mã loại ghế (giá của cả ghế — ghế đôi là giá cho 2 người). */
    @ElementCollection
    @CollectionTable(name = "showtime_prices", joinColumns = @JoinColumn(name = "showtime_id"))
    @MapKeyColumn(name = "seat_type_code")
    @Column(name = "price", nullable = false)
    private Map<String, Long> prices = new HashMap<>();
}
