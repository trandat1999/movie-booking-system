package com.moviebooking.modules.showtime.repository;

import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class ShowtimeSpecifications {

    private ShowtimeSpecifications() {}

    public static Specification<Showtime> inCinema(Long cinemaId) {
        return cinemaId == null ? null : (root, query, cb) -> cb.equal(root.get("room").get("cinema").get("id"), cinemaId);
    }

    public static Specification<Showtime> inRoom(Long roomId) {
        return roomId == null ? null : (root, query, cb) -> cb.equal(root.get("room").get("id"), roomId);
    }

    public static Specification<Showtime> ofMovie(Long movieId) {
        return movieId == null ? null : (root, query, cb) -> cb.equal(root.get("movie").get("id"), movieId);
    }

    public static Specification<Showtime> hasStatus(ShowtimeStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Bắt đầu trong khoảng [from, to). */
    public static Specification<Showtime> startsBetween(Instant from, Instant to) {
        if (from == null || to == null) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("startAt"), from), cb.lessThan(root.get("startAt"), to));
    }
}
