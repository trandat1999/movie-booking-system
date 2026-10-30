package com.moviebooking.modules.showtime.repository;

import com.moviebooking.modules.showtime.entity.Showtime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {

    @Override
    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    Page<Showtime> findAll(Specification<Showtime> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "room", "room.cinema", "prices"})
    Optional<Showtime> findWithDetailsById(Long id);

    /**
     * Suất chưa hủy cùng phòng có khoảng [startAt, blockedUntil) giao với khoảng cần kiểm tra (SCHADM04, 05).
     * Bỏ qua chính suất đang sửa nếu {@code excludeId} khác null.
     */
    @Query("select s from Showtime s join fetch s.movie where s.room.id = :roomId "
            + "and s.status <> com.moviebooking.modules.showtime.enums.ShowtimeStatus.CANCELLED "
            + "and s.startAt < :until and s.blockedUntil > :from "
            + "and (:excludeId is null or s.id <> :excludeId) order by s.startAt")
    List<Showtime> findOverlapping(
            @Param("roomId") Long roomId,
            @Param("from") Instant from,
            @Param("until") Instant until,
            @Param("excludeId") Long excludeId);
}
